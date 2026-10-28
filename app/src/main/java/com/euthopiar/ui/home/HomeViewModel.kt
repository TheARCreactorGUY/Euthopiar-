package com.euthopiar.ui.home

import android.app.Application
import kotlin.coroutines.coroutineContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import coil3.request.ImageRequest
import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.core.util.AppSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.euthopiar.ui.util.UserError
import com.euthopiar.ui.util.toUserError

enum class HomeFilterTab(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV_SHOWS("TV Shows"),
    ANIME("Anime"),
    TRENDING("Trending")
}

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val heroItems: List<MediaItem>,
        val catalogRows: List<CatalogRow>,
        val availableTabs: List<HomeFilterTab> = listOf(HomeFilterTab.ALL),
        val filteredRows: List<CatalogRow> = catalogRows,
        val rowsByTab: Map<HomeFilterTab, List<CatalogRow>> = emptyMap()
    ) : HomeUiState
    data class Error(
        val error: UserError = UserError.Unknown,
        val message: String = ""
    ) : HomeUiState
}

class HomeViewModel(
    application: Application,
    private val repository: ProviderRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, ProviderRepository.Default)

    private val libraryRepository = LibraryRepository.getInstance(application)
    private val topTenRepository = com.euthopiar.data.repository.TopTenRepository.getInstance(application)
    private val preferencesRepository = com.euthopiar.data.preferences.PreferencesRepository.getInstance(application)
    private val _pendingRemovalContinueWatchingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingClearContinueWatching = MutableStateFlow(false)

    val top10Style: StateFlow<com.euthopiar.data.preferences.Top10Style> = preferencesRepository.top10Style
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.euthopiar.data.preferences.Top10Style.PosterTinted)

    val sectionLabelStyle: StateFlow<com.euthopiar.data.preferences.SectionLabelStyle> = preferencesRepository.sectionLabelStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.euthopiar.data.preferences.SectionLabelStyle.AccentBar)

    val longPressStyle: StateFlow<com.euthopiar.ui.longpress.LongPressSheetStyle> = preferencesRepository.longPressStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.euthopiar.ui.longpress.LongPressSheetStyle.HERO_PEEK)

    val mediaReactions: StateFlow<Map<String, String>> = preferencesRepository.mediaReactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setMediaReaction(mediaId: String, reaction: com.euthopiar.ui.longpress.UserReaction) {
        viewModelScope.launch {
            preferencesRepository.setMediaReaction(mediaId, if (reaction == com.euthopiar.ui.longpress.UserReaction.NONE) null else reaction.id)
        }
    }

    private val _selectedTab = MutableStateFlow(HomeFilterTab.ALL)
    val selectedTab: StateFlow<HomeFilterTab> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val universalTopTen: StateFlow<List<MediaItem>> =
        combine(_selectedTab, preferencesRepository.contentRegionPreset) { tab, preset ->
            tab to preset
        }.flatMapLatest { (tab, preset) ->
            val mediaType = when (tab) {
                HomeFilterTab.TV_SHOWS, HomeFilterTab.ANIME -> com.euthopiar.core.model.MediaType.TV_SERIES
                else -> com.euthopiar.core.model.MediaType.MOVIE
            }
            topTenRepository.observeTopTen(mediaType, preset = preset)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val watchlistItems: StateFlow<List<MediaItem>> =
        libraryRepository.watchlist
            .map { list ->
                list.map { entity ->
                    val type = try {
                        com.euthopiar.core.model.MediaType.valueOf(entity.mediaType)
                    } catch (_: Exception) {
                        com.euthopiar.core.model.MediaType.MOVIE
                    }
                    MediaItem(
                        id = entity.mediaId,
                        title = entity.title,
                        url = "",
                        posterUrl = entity.posterUrl,
                        backdropUrl = entity.backdropUrl,
                        type = type,
                        rating = entity.rating?.toString(),
                        year = entity.year
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val continueWatching: StateFlow<List<PlaybackHistoryEntity>> =
        combine(
            libraryRepository.continueWatching,
            _pendingRemovalContinueWatchingIds,
            _pendingClearContinueWatching
        ) { items, pendingIds, pendingClear ->
            if (pendingClear) {
                emptyList()
            } else {
                items
                    .filter { it.id !in pendingIds }
                    .distinctBy { it.mediaId }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val bookmarkedIds: StateFlow<Set<String>> =
        libraryRepository.watchlist
            .map { list -> list.map { it.mediaId }.toSet() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptySet()
            )

    val playbackHistoryMap: StateFlow<Map<String, PlaybackHistoryEntity>> =
        combine(
            continueWatching,
            libraryRepository.history
        ) { cwList, histList ->
            val map = mutableMapOf<String, PlaybackHistoryEntity>()
            histList.forEach { item ->
                map.putIfAbsent(item.mediaId, item)
            }
            cwList.forEach { item ->
                map.putIfAbsent(item.mediaId, item)
            }
            map
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyMap()
        )

    val watchlistStatusMap: StateFlow<Map<String, String>> =
        libraryRepository.watchlist
            .map { list -> list.associate { it.mediaId to it.status } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyMap()
            )

    fun toggleBookmark(item: MediaItem) {
        viewModelScope.launch {
            libraryRepository.toggleWatchlist(item)
        }
    }

    fun setWatchlistStatus(item: MediaItem, status: String) {
        viewModelScope.launch {
            if (status == "REMOVE") {
                libraryRepository.removeFromWatchlist(item.id)
            } else {
                libraryRepository.setWatchlistStatus(item, status)
            }
        }
    }

    fun removeFromWatchlist(mediaId: String) {
        viewModelScope.launch {
            libraryRepository.removeFromWatchlist(mediaId)
        }
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var rawCatalogRows: List<CatalogRow> = emptyList()
    private var rawHeroItems: List<MediaItem> = emptyList()
    private var computedTabs: List<HomeFilterTab> = listOf(HomeFilterTab.ALL)

    private fun deduplicateCatalogRows(rows: List<CatalogRow>): List<CatalogRow> {
        val seen = mutableSetOf<String>()
        return rows.filter { row ->
            val key = "${row.title.trim().lowercase()}|${row.subtitle?.trim()?.lowercase().orEmpty()}"
            seen.add(key)
        }
    }

    fun selectTab(tab: HomeFilterTab) {
        _selectedTab.value = tab
        updateSuccessState()
    }

    private fun updateSuccessState() {
        if (rawCatalogRows.isEmpty()) return
        val rowsMap = computedTabs.associateWith { tab -> filterRowsForTab(tab, rawCatalogRows) }
        val filtered = rowsMap[_selectedTab.value] ?: filterRowsForTab(_selectedTab.value, rawCatalogRows)
        _uiState.value = HomeUiState.Success(
            heroItems = rawHeroItems,
            catalogRows = rawCatalogRows,
            availableTabs = computedTabs,
            filteredRows = filtered,
            rowsByTab = rowsMap
        )
    }

    private fun filterRowsForTab(tab: HomeFilterTab, rows: List<CatalogRow>): List<CatalogRow> {
        return when (tab) {
            HomeFilterTab.ALL -> rows
            HomeFilterTab.TRENDING -> {
                val trendingRows = rows.filter { row ->
                    val t = row.title.lowercase()
                    t.contains("trend") || t.contains("popular") || t.contains("featured") || t.contains("top")
                }
                if (trendingRows.isNotEmpty()) trendingRows else rows.take(2)
            }
            HomeFilterTab.MOVIES -> {
                val filtered = rows.mapNotNull { row ->
                    val matching = row.items.filter { classifyItem(it, row.title) == HomeFilterTab.MOVIES }
                    if (matching.isNotEmpty()) row.copy(items = matching) else null
                }
                if (filtered.isNotEmpty()) filtered else rows
            }
            HomeFilterTab.TV_SHOWS -> {
                val filtered = rows.mapNotNull { row ->
                    val matching = row.items.filter { classifyItem(it, row.title) == HomeFilterTab.TV_SHOWS }
                    if (matching.isNotEmpty()) row.copy(items = matching) else null
                }
                if (filtered.isNotEmpty()) filtered else rows
            }
            HomeFilterTab.ANIME -> {
                val filtered = rows.mapNotNull { row ->
                    val matching = row.items.filter { classifyItem(it, row.title) == HomeFilterTab.ANIME }
                    if (matching.isNotEmpty()) row.copy(items = matching) else null
                }
                if (filtered.isNotEmpty()) filtered else rows
            }
        }
    }

    private fun classifyItem(item: MediaItem, rowTitle: String): HomeFilterTab {
        val rLower = rowTitle.lowercase()
        val tLower = item.title.lowercase()

        if (item.type == com.euthopiar.core.model.MediaType.ANIME || rLower.contains("anime") || (tLower.contains("season") && rLower.contains("anime"))) {
            return HomeFilterTab.ANIME
        }
        if (item.type == com.euthopiar.core.model.MediaType.TV_SERIES ||
            rLower.contains("series") || rLower.contains("show") || rLower.contains("season") ||
            tLower.contains("season") || tLower.contains("episode") || tLower.contains("s0") || tLower.contains("s1")
        ) {
            return HomeFilterTab.TV_SHOWS
        }
        return HomeFilterTab.MOVIES
    }

    init {
        var lastPluginName: String? = null
        viewModelScope.launch {
            combine(
                repository.activePlugin,
                preferencesRepository.contentRegionPreset
            ) { plugin, preset ->
                plugin.name to preset
            }.collect { (pluginName, preset) ->
                if (lastPluginName != pluginName) {
                    lastPluginName = pluginName
                    _selectedTab.value = HomeFilterTab.ALL
                }
                com.euthopiar.core.util.TmdbBridge.activeRegionPreset = preset
                loadCatalog()
            }
        }
    }

    fun removeFromContinueWatching(id: String) {
        viewModelScope.launch {
            libraryRepository.dismissFromContinueWatching(id)
        }
    }

    fun clearContinueWatching() {
        viewModelScope.launch {
            libraryRepository.clearContinueWatching(null)
        }
    }

    fun removeFromContinueWatchingWithUndo(
        id: String,
        onUndoAvailable: (undoAction: () -> Unit) -> Unit
    ) {
        _pendingRemovalContinueWatchingIds.update { it + id }
        val job = viewModelScope.launch {
            delay(5000L)
            libraryRepository.dismissFromContinueWatching(id)
            _pendingRemovalContinueWatchingIds.update { it - id }
        }
        onUndoAvailable {
            job.cancel()
            _pendingRemovalContinueWatchingIds.update { it - id }
        }
    }

    fun clearContinueWatchingWithUndo(
        onUndoAvailable: (undoAction: () -> Unit) -> Unit
    ) {
        _pendingClearContinueWatching.value = true
        val job = viewModelScope.launch {
            delay(5000L)
            val currentProvider = repository.activePlugin.value.name
            libraryRepository.clearContinueWatching(currentProvider)
            _pendingClearContinueWatching.value = false
        }
        onUndoAvailable {
            job.cancel()
            _pendingClearContinueWatching.value = false
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                AppSessionManager.increment()
                fetchCatalog(isPullToRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private var loadCatalogJob: kotlinx.coroutines.Job? = null

    fun loadCatalog() {
        val prevJob = loadCatalogJob
        loadCatalogJob = viewModelScope.launch {
            prevJob?.cancelAndJoin()
            fetchCatalog(isPullToRefresh = false)
        }
    }

    private var progressiveOffset: Int = 0
    private var hasMoreRails: Boolean = true
    private val _isProgressiveLoading = MutableStateFlow(false)
    val isProgressiveLoading: StateFlow<Boolean> = _isProgressiveLoading.asStateFlow()

    fun loadMoreRails() {
        if (_isProgressiveLoading.value || !hasMoreRails) return
        viewModelScope.launch {
            _isProgressiveLoading.value = true
            try {
                val limit = 4
                val newRails = repository.getProgressiveHomeRails(progressiveOffset, limit)
                if (newRails.isNotEmpty()) {
                    progressiveOffset += limit
                    rawCatalogRows = deduplicateCatalogRows(rawCatalogRows + newRails)
                    updateSuccessState()
                } else {
                    hasMoreRails = false
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "Failed to load progressive rails: ${e.message}")
            } finally {
                _isProgressiveLoading.value = false
            }
        }
    }

    private suspend fun fetchCatalog(isPullToRefresh: Boolean) {
        if (!isPullToRefresh && rawCatalogRows.isEmpty()) {
            _uiState.value = HomeUiState.Loading
        }
        progressiveOffset = 0
        hasMoreRails = true

        try {
            // 1. Fetch entire home catalog rows with graceful polling for startup plugin readiness
            var rows = repository.getHomeCatalog()
            var attempts = 0
            while (rows.isEmpty() && attempts < 6) {
                delay(500)
                if (!coroutineContext.isActive) return
                attempts++
                rows = repository.getHomeCatalog()
            }
            if (!coroutineContext.isActive) return

            if (rows.isEmpty()) {
                android.util.Log.w("HomeViewModel", "No content available for current provider after $attempts attempts")
                if (rawCatalogRows.isEmpty()) {
                    _uiState.value = HomeUiState.Error(
                        error = UserError.NoSources,
                        message = "No sources yet"
                    )
                }
                return
            }

                // 2. Deterministic provider + session seed ensures carousel & feed change per provider and launch
                val currentProvider = repository.activePlugin.value.name
                val providerHash = currentProvider.hashCode().toLong()
                val sessionIdx = AppSessionManager.getSessionIndex().toLong()
                val combinedSeed = (providerHash * 37L) xor (sessionIdx * 10007L)
                val providerRandom = kotlin.random.Random(combinedSeed)

                // 3. Select hero candidates from the top regional/core rows so regional presets never get diluted by global progressive rails
                val activePreset = com.euthopiar.core.util.TmdbBridge.activeRegionPreset
                val pinnedCoreCount = when (activePreset) {
                    com.euthopiar.core.model.ContentRegionPreset.GLOBAL -> 4
                    com.euthopiar.core.model.ContentRegionPreset.SOUTH_INDIA_ALL -> 9
                    else -> 5
                }.coerceAtMost(rows.size)

                val heroSourceRows = rows.take(pinnedCoreCount).ifEmpty { rows }
                val rawCandidates = heroSourceRows.flatMap { it.items }
                    .filter { !it.backdropUrl.isNullOrBlank() || !it.posterUrl.isNullOrBlank() }
                    .distinctBy { it.id }

                val candidates = if (activePreset.isIndianRegion) {
                    rawCandidates.filterNot { com.euthopiar.core.util.TmdbBridge.isIndianTvSerial(it) }
                } else {
                    rawCandidates
                }

                val candidatesWithLogos = candidates.filter { !it.logoUrl.isNullOrBlank() }.shuffled(providerRandom)
                val otherCandidates = candidates.filter { it.logoUrl.isNullOrBlank() }.shuffled(providerRandom)
                val heroCandidatesToResolve = (candidatesWithLogos + otherCandidates).take(8)

                // 4. Concurrently resolve logos for hero candidates
                val resolvedHeroes = coroutineScope {
                    heroCandidatesToResolve.map { item ->
                        async(Dispatchers.IO) {
                            if (!item.logoUrl.isNullOrBlank()) {
                                item
                            } else {
                                try {
                                    withTimeoutOrNull(2500L) {
                                        repository.resolveHeroItem(item)
                                    } ?: item
                                } catch (_: Throwable) {
                                    item
                                }
                            }
                        }
                    }.awaitAll()
                }

                // Prioritize items that have verified studio clear logos (up to 6 in carousel)
                val heroCandidatesPool = if (activePreset.isIndianRegion) {
                    resolvedHeroes.filterNot { com.euthopiar.core.util.TmdbBridge.isIndianTvSerial(it) }
                } else {
                    resolvedHeroes
                }
                val heroWithLogos = heroCandidatesPool.filter { !it.logoUrl.isNullOrBlank() }
                val heroItems = if (heroWithLogos.size >= 4) {
                    heroWithLogos.take(6)
                } else {
                    (heroWithLogos + heroCandidatesPool.filter { it.logoUrl.isNullOrBlank() }).take(6)
                }

                // 5. Keep regional/core rows pinned at the top; permute secondary discovery rows and rotate item offsets
                val processedRows = if (rows.size > pinnedCoreCount) {
                    val pinnedRows = rows.take(pinnedCoreCount)
                    val otherRows = rows.drop(pinnedCoreCount).shuffled(providerRandom)
                    pinnedRows + otherRows.map { row ->
                        if (row.items.size > 4) {
                            val offset = providerRandom.nextInt(row.items.size)
                            row.copy(items = row.items.drop(offset) + row.items.take(offset))
                        } else {
                            row
                        }
                    }
                } else {
                    rows
                }

                val enrichedRows = processedRows.map { row ->
                    val t = row.title.lowercase()
                    when {
                        t.contains("trend") -> row.copy(
                            subtitle = row.subtitle ?: "Based on recent views",
                            eyebrow = row.eyebrow ?: "RIGHT NOW"
                        )
                        t.contains("popular") -> row.copy(
                            subtitle = row.subtitle ?: "Most watched this week",
                            eyebrow = row.eyebrow ?: "FEATURED"
                        )
                        else -> row
                    }
                }

                rawCatalogRows = deduplicateCatalogRows(enrichedRows)
                rawHeroItems = heroItems

                // Compute bulletproof self-pruning tabs
                val allItems = enrichedRows.flatMap { it.items }
                val hasMovies = allItems.any { classifyItem(it, "") == HomeFilterTab.MOVIES }
                val hasTv = allItems.any { classifyItem(it, "") == HomeFilterTab.TV_SHOWS }
                val hasAnime = allItems.any { classifyItem(it, "") == HomeFilterTab.ANIME }
                val hasTrending = enrichedRows.any {
                    val t = it.title.lowercase()
                    t.contains("trend") || t.contains("popular") || t.contains("featured") || t.contains("top")
                }

                val tabs = mutableListOf(HomeFilterTab.ALL)
                if (hasMovies) tabs.add(HomeFilterTab.MOVIES)
                if (hasTv) tabs.add(HomeFilterTab.TV_SHOWS)
                if (hasAnime) tabs.add(HomeFilterTab.ANIME)
                if (hasTrending) tabs.add(HomeFilterTab.TRENDING)
                computedTabs = tabs

                if (_selectedTab.value !in tabs) {
                    _selectedTab.value = HomeFilterTab.ALL
                }

                val rowsMap = computedTabs.associateWith { tab -> filterRowsForTab(tab, enrichedRows) }
                val filtered = rowsMap[_selectedTab.value] ?: filterRowsForTab(_selectedTab.value, enrichedRows)

                if (!coroutineContext.isActive) return
                // 6. Emit Success IMMEDIATELY so the home feed appears instantaneously
                _uiState.value = HomeUiState.Success(
                    heroItems = heroItems,
                    catalogRows = enrichedRows,
                    availableTabs = computedTabs,
                    filteredRows = filtered,
                    rowsByTab = rowsMap
                )

                // 5. Preload images asynchronously in background without blocking UI
                viewModelScope.launch(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val imageLoader = app.imageLoader

                    val heroUrls = heroItems.flatMap { item ->
                        listOfNotNull(item.backdropUrl, item.logoUrl)
                    }
                    val visiblePosterUrls = rows.take(4).flatMap { it.items.take(8) }
                        .mapNotNull { it.posterUrl }

                    val allPreloadUrls = (heroUrls + visiblePosterUrls).distinct()
                    for (url in allPreloadUrls) {
                        try {
                            val req = ImageRequest.Builder(app)
                                .data(url)
                                .build()
                            imageLoader.enqueue(req)
                        } catch (_: Exception) {}
                    }
                }

            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                if (!coroutineContext.isActive) return
                android.util.Log.w("HomeViewModel", "Failed to load catalog", e)
                if (rawCatalogRows.isEmpty()) {
                    _uiState.value = HomeUiState.Error(
                        error = e.toUserError(),
                        message = ""
                    )
                }
            }
    }
}
