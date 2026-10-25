package com.euthopiar.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.ui.util.UserError
import com.euthopiar.ui.util.toUserError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchSortOption(val displayName: String) {
    BEST_MATCH("Best match"),
    RATING("Rating"),
    NEWEST("Newest"),
    A_TO_Z("A to Z")
}

sealed interface SearchUiState {
    data class Trending(val items: List<MediaItem>, val isLoading: Boolean = false) : SearchUiState
    object Searching : SearchUiState
    data class Results(val query: String, val items: List<MediaItem>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(
        val error: UserError = UserError.Unknown,
        val message: String = ""
    ) : SearchUiState
}

class SearchViewModel(
    application: Application,
    private val repository: ProviderRepository = ProviderRepository.Default
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, ProviderRepository.Default)

    private val preferencesRepository = PreferencesRepository.getInstance(application)
    val recentSearches: StateFlow<List<String>> = preferencesRepository.recentSearches.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val availableProviders: StateFlow<List<String>> = combine(
        repository.registry.plugins,
        repository.registry.disabledPlugins
    ) { plugins, disabled ->
        plugins
            .filter { plugin ->
                plugin.name.isNotBlank() &&
                plugin.name != "No Provider" &&
                !disabled.any { it.equals(plugin.name, ignoreCase = true) }
            }
            .map { it.name }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Trending(items = emptyList(), isLoading = true))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val currentQuery = MutableStateFlow("")

    // Screen navigation state: whether currently showing the Results screen
    val isResultsActive = MutableStateFlow(false)

    // Background refinement indicator for live debounce without flashing skeletons
    val isRefining = MutableStateFlow(false)

    // Three core filter dropdowns + Sort
    val selectedTypeFilter = MutableStateFlow<MediaType?>(null)
    val selectedGenreFilter = MutableStateFlow<String?>(null)
    val selectedYearFilter = MutableStateFlow<String?>(null)
    val selectedSort = MutableStateFlow(SearchSortOption.BEST_MATCH)

    // Kept for backward compatibility
    val selectedProviderFilter = MutableStateFlow<String?>(null)
    val selectedQualityFilter = MutableStateFlow<String?>(null)

    // Cached trending items for suggestions during No Results
    private val _cachedTrendingItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val cachedTrendingItems: StateFlow<List<MediaItem>> = _cachedTrendingItems.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTrending()
    }

    fun setTypeFilter(type: MediaType?) {
        selectedTypeFilter.value = type
    }

    fun setGenreFilter(genre: String?) {
        selectedGenreFilter.value = if (genre == "All genres" || genre == "All") null else genre
    }

    fun setYearFilter(year: String?) {
        selectedYearFilter.value = if (year == "Any year" || year == "All") null else year
    }

    fun setSort(sort: SearchSortOption) {
        selectedSort.value = sort
    }

    fun clearFilters() {
        selectedTypeFilter.value = null
        selectedGenreFilter.value = null
        selectedYearFilter.value = null
        selectedProviderFilter.value = null
        selectedQualityFilter.value = null
        selectedSort.value = SearchSortOption.BEST_MATCH
    }

    fun setProviderFilter(provider: String?) {
        selectedProviderFilter.value = provider
    }

    fun setQualityFilter(quality: String?) {
        selectedQualityFilter.value = quality
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) {
            viewModelScope.launch {
                preferencesRepository.addRecentSearch(clean)
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            preferencesRepository.removeRecentSearch(query)
        }
    }

    fun commitSearch(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) {
            addRecentSearch(clean)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
        }
    }

    fun undoClearRecentSearches(items: List<String>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            preferencesRepository.restoreRecentSearches(items)
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            if (currentQuery.value.isNotBlank() && isResultsActive.value) return@launch
            _uiState.value = SearchUiState.Trending(
                items = (_uiState.value as? SearchUiState.Trending)?.items ?: _cachedTrendingItems.value,
                isLoading = true
            )
            try {
                val catalog = repository.getHomeCatalog()
                val trendingItems = catalog.firstOrNull()?.items ?: emptyList()
                _cachedTrendingItems.value = trendingItems
                if (currentQuery.value.isBlank() || !isResultsActive.value) {
                    _uiState.value = SearchUiState.Trending(items = trendingItems, isLoading = false)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("SearchViewModel", "Failed to load trending items", e)
                if (currentQuery.value.isBlank() || !isResultsActive.value) {
                    val currentItems = _cachedTrendingItems.value
                    if (currentItems.isNotEmpty()) {
                        _uiState.value = SearchUiState.Trending(items = currentItems, isLoading = false)
                    } else {
                        _uiState.value = SearchUiState.Error(
                            error = e.toUserError(),
                            message = ""
                        )
                    }
                }
            }
        }
    }

    fun submitImmediateSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            clearQueryAndBack()
            return
        }
        currentQuery.value = trimmed
        isResultsActive.value = true
        searchJob?.cancel()
        commitSearch(trimmed)
        searchJob = viewModelScope.launch {
            val hasExistingResults = _uiState.value is SearchUiState.Results
            if (hasExistingResults) {
                isRefining.value = true
            } else {
                _uiState.value = SearchUiState.Searching
            }
            try {
                val results = repository.search(trimmed, isGlobal = true)
                isRefining.value = false
                if (results.isEmpty()) {
                    _uiState.value = SearchUiState.Empty(trimmed)
                } else {
                    _uiState.value = SearchUiState.Results(trimmed, results)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                isRefining.value = false
                android.util.Log.w("SearchViewModel", "Immediate search failed for query '$trimmed'", e)
                _uiState.value = SearchUiState.Error(
                    error = e.toUserError(),
                    message = ""
                )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        currentQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            isRefining.value = false
            if (!isResultsActive.value) {
                loadTrending()
            } else {
                _uiState.value = SearchUiState.Results("", emptyList())
            }
            return
        }

        // Live debounce (300ms) only when results screen is active
        if (isResultsActive.value) {
            val hasExistingResults = _uiState.value is SearchUiState.Results
            if (hasExistingResults) {
                isRefining.value = true
            }
            searchJob = viewModelScope.launch {
                delay(300)
                if (!hasExistingResults) {
                    _uiState.value = SearchUiState.Searching
                }
                try {
                    val results = repository.search(newQuery.trim(), isGlobal = true)
                    isRefining.value = false
                    if (results.isEmpty()) {
                        _uiState.value = SearchUiState.Empty(newQuery)
                    } else {
                        _uiState.value = SearchUiState.Results(newQuery, results)
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    isRefining.value = false
                    android.util.Log.w("SearchViewModel", "Search failed for query '$newQuery'", e)
                    _uiState.value = SearchUiState.Error(
                        error = e.toUserError(),
                        message = ""
                    )
                }
            }
        }
    }

    fun backToSearch() {
        searchJob?.cancel()
        isRefining.value = false
        isResultsActive.value = false
        if (currentQuery.value.isBlank()) {
            loadTrending()
        }
    }

    fun clearQueryAndBack() {
        searchJob?.cancel()
        isRefining.value = false
        currentQuery.value = ""
        isResultsActive.value = false
        loadTrending()
    }

    fun retrySearch() {
        val q = currentQuery.value.trim()
        if (q.isNotBlank()) {
            submitImmediateSearch(q)
        } else {
            loadTrending()
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        isRefining.value = false
        currentQuery.value = ""
        if (!isResultsActive.value) {
            loadTrending()
        } else {
            _uiState.value = SearchUiState.Results("", emptyList())
        }
    }
}
