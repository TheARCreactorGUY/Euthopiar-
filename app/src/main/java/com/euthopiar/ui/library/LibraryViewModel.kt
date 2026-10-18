package com.euthopiar.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.repository.LibraryRepository
import com.euthopiar.data.repository.Playlist
import com.euthopiar.data.repository.PlaylistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySection(val label: String) {
    DOWNLOADS("Downloads"),
    SAVED("Saved"),
    PLAYLIST("Playlist")
}

enum class DownloadsSubFilter(val label: String) {
    ALL("All Downloads"),
    MOVIES("Movies"),
    TV_SERIES("TV Series"),
    ACTIVE("Active")
}

enum class LibraryFilter(val label: String) {
    ALL("All"),
    WATCHLIST("Watchlist"),
    HISTORY("History"),
    FAVORITES("Favorites"),
    DOWNLOADS("Downloads"),
    WATCHING("Watching"),
    TO_BE_WATCHED("To Be Watched"),
    COMPLETED("Completed")
}

enum class LibrarySortOrder(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    TITLE("Title (A-Z)"),
    RELEASE_DATE("Release Date")
}

data class LibraryItem(
    val id: String,
    val mediaId: String,
    val title: String,
    val episodeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val posterUrl: String?,
    val backdropUrl: String?,
    val mediaType: MediaType,
    val rating: Double?,
    val year: Int?,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val isWatchlist: Boolean = false,
    val isHistory: Boolean = false,
    val isFavorite: Boolean = false,
    val status: String = "TO_BE_WATCHED", // TO_BE_WATCHED, WATCHING, COMPLETED
    val provider: String? = null,
    val episodeData: String? = null,
    val timestamp: Long = 0L
) {
    val progress: Float
        get() = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository.getInstance(application)
    private val preferencesRepository = com.euthopiar.data.preferences.PreferencesRepository.getInstance(application)
    private val downloadRepository = com.euthopiar.download.DownloadRepository.getInstance(application)

    private val _selectedSection = MutableStateFlow(LibrarySection.SAVED)
    val selectedSection: StateFlow<LibrarySection> = _selectedSection.asStateFlow()

    private val _downloadsSubFilter = MutableStateFlow(DownloadsSubFilter.ALL)
    val downloadsSubFilter: StateFlow<DownloadsSubFilter> = _downloadsSubFilter.asStateFlow()

    fun setDownloadsSubFilter(filter: DownloadsSubFilter) {
        _downloadsSubFilter.value = filter
    }

    fun selectSection(section: LibrarySection) {
        exitSelectionMode()
        _selectedSection.value = section
        when (section) {
            LibrarySection.DOWNLOADS -> {
                _selectedFilter.value = LibraryFilter.DOWNLOADS
            }
            LibrarySection.SAVED -> {
                if (_selectedFilter.value !in listOf(LibraryFilter.ALL, LibraryFilter.WATCHLIST, LibraryFilter.FAVORITES, LibraryFilter.TO_BE_WATCHED, LibraryFilter.COMPLETED)) {
                    _selectedFilter.value = LibraryFilter.ALL
                }
            }
            LibrarySection.PLAYLIST -> {
                if (_selectedFilter.value !in listOf(LibraryFilter.HISTORY, LibraryFilter.WATCHING, LibraryFilter.COMPLETED)) {
                    _selectedFilter.value = LibraryFilter.HISTORY
                }
            }
        }
    }

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(LibrarySortOrder.RECENTLY_ADDED)
    val sortOrder: StateFlow<LibrarySortOrder> = _sortOrder.asStateFlow()

    fun setSortOrder(order: LibrarySortOrder) {
        _sortOrder.value = order
    }

    val favoriteMediaIds = preferencesRepository.favoriteMediaIds

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(mediaId)
        }
    }

    val watchlist = repository.watchlist
    val continueWatching = repository.continueWatching
    val history = repository.history
    val completed = repository.completed
    val downloads = downloadRepository.downloads

    fun pauseDownload(id: String) = downloadRepository.pauseDownload(id)
    fun resumeDownload(id: String) = downloadRepository.resumeDownload(id)
    fun removeDownload(id: String) = downloadRepository.removeDownload(id)
    suspend fun getStorageStats() = downloadRepository.getStorageStats()

    fun setInitialFilter(filter: LibraryFilter) {
        selectFilter(filter)
    }

    private val playlistRepository = PlaylistRepository.getInstance(application)

    val playlists: StateFlow<List<Playlist>> = playlistRepository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(title)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
        }
    }

    fun updatePlaylistLastPlayed(id: String) {
        viewModelScope.launch {
            playlistRepository.updateLastPlayed(id)
        }
    }

    fun removeItemFromPlaylist(playlistId: String, itemKey: String) {
        viewModelScope.launch {
            playlistRepository.removeItemFromPlaylist(playlistId, itemKey)
        }
    }

    val isOfflineOnly: StateFlow<Boolean> = preferencesRepository.libraryOfflineOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleOfflineOnly() {
        viewModelScope.launch {
            preferencesRepository.setLibraryOfflineOnly(!isOfflineOnly.value)
        }
    }

    val libraryLastTab: Flow<Int> = preferencesRepository.libraryLastTab

    fun saveLastTab(tabIndex: Int) {
        viewModelScope.launch {
            preferencesRepository.setLibraryLastTab(tabIndex)
        }
    }

    val downloadsMap: StateFlow<Map<String, com.euthopiar.download.DownloadItem>> = downloadRepository.downloads
        .map { list ->
            val map = mutableMapOf<String, com.euthopiar.download.DownloadItem>()
            for (item in list) {
                map[item.id] = item
            }
            // For mediaId, prioritize active in-progress states over completed
            val groupedByMedia = list.groupBy { it.mediaId }
            for ((mediaId, items) in groupedByMedia) {
                val prioritized = items.minByOrNull { item ->
                    when (item.state) {
                        com.euthopiar.download.DownloadState.DOWNLOADING -> 0
                        com.euthopiar.download.DownloadState.QUEUED -> 1
                        com.euthopiar.download.DownloadState.PAUSED -> 2
                        com.euthopiar.download.DownloadState.COMPLETED -> 3
                        com.euthopiar.download.DownloadState.FAILED -> 4
                    }
                }
                if (prioritized != null) {
                    map[mediaId] = prioritized
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val historyMap: StateFlow<Map<String, com.euthopiar.data.local.PlaybackHistoryEntity>> = repository.history
        .map { list ->
            val map = mutableMapOf<String, com.euthopiar.data.local.PlaybackHistoryEntity>()
            for (h in list) {
                map.putIfAbsent(h.mediaId, h)
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteSelectedDownloads() {
        val selected = _selectedItemIds.value
        viewModelScope.launch {
            for (id in selected) {
                downloadRepository.removeDownload(id)
            }
            exitSelectionMode()
        }
    }

    val filterCounts: StateFlow<Map<LibraryFilter, Int>> = combine(
        repository.watchlist,
        repository.history,
        repository.completed,
        preferencesRepository.favoriteMediaIds,
        downloadRepository.downloads
    ) { watchlists, histories, completeds, favoriteIds, downloadsList ->
        val historyMap = mutableMapOf<String, com.euthopiar.data.local.PlaybackHistoryEntity>()
        for (h in histories) {
            historyMap.putIfAbsent(h.mediaId, h)
        }
        val completedMediaIds = completeds.map { it.mediaId }.toSet()
        val watchlistMediaIds = watchlists.map { it.mediaId }.toSet()

        val fromWatchlist = watchlists.map { w ->
            val hist = historyMap[w.mediaId]
            val isDone = w.status == "COMPLETED" || (hist?.isCompleted == true) || w.mediaId in completedMediaIds
            val inProgress = !isDone && ((hist != null && hist.currentPositionMs > 5000L) || w.status == "WATCHING")
            when {
                isDone -> "COMPLETED"
                inProgress -> "WATCHING"
                else -> w.status
            }
        }
        val fromHistoryOnly = histories.filter { it.mediaId !in watchlistMediaIds }.distinctBy { it.mediaId }.map { h ->
            if (h.isCompleted) "COMPLETED" else "WATCHING"
        }
        val allStatuses = fromWatchlist + fromHistoryOnly

        val allMediaIds = watchlistMediaIds + histories.map { it.mediaId }.toSet()
        val favoritesCount = favoriteIds.count { it in allMediaIds }.coerceAtLeast(favoriteIds.size)

        mapOf(
            LibraryFilter.ALL to allStatuses.size,
            LibraryFilter.WATCHLIST to watchlists.size,
            LibraryFilter.HISTORY to histories.size,
            LibraryFilter.FAVORITES to favoritesCount,
            LibraryFilter.DOWNLOADS to downloadsList.size,
            LibraryFilter.WATCHING to allStatuses.count { it == "WATCHING" },
            LibraryFilter.TO_BE_WATCHED to fromWatchlist.count { it == "TO_BE_WATCHED" },
            LibraryFilter.COMPLETED to allStatuses.count { it == "COMPLETED" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _pendingRemovalIds = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingClearHistory = MutableStateFlow(false)

    private data class LibraryData(
        val watchlists: List<com.euthopiar.data.local.WatchlistEntity>,
        val histories: List<com.euthopiar.data.local.PlaybackHistoryEntity>,
        val completeds: List<com.euthopiar.data.local.PlaybackHistoryEntity>,
        val favoriteIds: Set<String>,
        val downloads: List<com.euthopiar.download.DownloadItem> = emptyList()
    )

    private val libraryDataFlow = combine(
        repository.watchlist,
        repository.history,
        repository.completed,
        preferencesRepository.favoriteMediaIds,
        downloadRepository.downloads
    ) { watchlists, histories, completeds, favoriteIds, downloads ->
        LibraryData(watchlists, histories, completeds, favoriteIds, downloads)
    }

    private val unfilteredItems: Flow<List<LibraryItem>> = combine(
        _selectedFilter,
        _sortOrder,
        libraryDataFlow
    ) { filter, sortOrder, data ->
        val (watchlists, histories, completeds, favoriteIds, downloads) = data
        val historyMap = mutableMapOf<String, com.euthopiar.data.local.PlaybackHistoryEntity>()
        for (h in histories) {
            historyMap.putIfAbsent(h.mediaId, h)
        }
        val completedMediaIds = completeds.map { it.mediaId }.toSet()
        val watchlistMediaIds = watchlists.map { it.mediaId }.toSet()
        val historyMediaIds = histories.map { it.mediaId }.toSet()

        // 1. Build unified models for watchlist items
        val fromWatchlist = watchlists.map { w ->
            val hist = historyMap[w.mediaId]
            val isDone = w.status == "COMPLETED" || (hist?.isCompleted == true) || w.mediaId in completedMediaIds
            val inProgress = !isDone && ((hist != null && hist.currentPositionMs > 5000L) || w.status == "WATCHING")
            val computedStatus = when {
                isDone -> "COMPLETED"
                inProgress -> "WATCHING"
                else -> w.status
            }
            val resolvedType = try { MediaType.valueOf(w.mediaType) } catch (_: Exception) { MediaType.MOVIE }
            val isMovie = resolvedType == MediaType.MOVIE

            LibraryItem(
                id = hist?.id ?: w.mediaId,
                mediaId = w.mediaId,
                title = sanitizeTitle(w.title),
                episodeTitle = if (isMovie) null else hist?.episodeTitle?.let { sanitizeTitle(it) },
                seasonNumber = if (isMovie) null else hist?.seasonNumber,
                episodeNumber = if (isMovie) null else hist?.episodeNumber,
                posterUrl = w.posterUrl ?: hist?.posterUrl,
                backdropUrl = w.backdropUrl ?: hist?.backdropUrl,
                mediaType = resolvedType,
                rating = w.rating,
                year = w.year,
                currentPositionMs = hist?.currentPositionMs ?: 0L,
                durationMs = hist?.durationMs ?: 0L,
                isCompleted = isDone,
                isWatchlist = true,
                isHistory = hist != null,
                isFavorite = w.mediaId in favoriteIds,
                status = computedStatus,
                provider = hist?.provider,
                episodeData = hist?.episodeData,
                timestamp = w.addedTimestamp
            )
        }

        // 2. Build items from history that aren't in watchlist
        val fromHistoryOnly = histories
            .filter { it.mediaId !in watchlistMediaIds }
            .distinctBy { it.mediaId }
            .map { h ->
                val computedStatus = if (h.isCompleted) "COMPLETED" else "WATCHING"
                val resolvedType = h.mediaType?.let {
                    try { MediaType.valueOf(it) } catch (_: Exception) { null }
                } ?: if (h.seasonNumber != null || h.episodeNumber != null) MediaType.TV_SERIES else MediaType.MOVIE
                val isMovie = resolvedType == MediaType.MOVIE

                LibraryItem(
                    id = h.id,
                    mediaId = h.mediaId,
                    title = sanitizeTitle(h.title),
                    episodeTitle = if (isMovie) null else h.episodeTitle?.let { sanitizeTitle(it) },
                    seasonNumber = if (isMovie) null else h.seasonNumber,
                    episodeNumber = if (isMovie) null else h.episodeNumber,
                    posterUrl = h.posterUrl,
                    backdropUrl = h.backdropUrl,
                    mediaType = resolvedType,
                    rating = null,
                    year = null,
                    currentPositionMs = h.currentPositionMs,
                    durationMs = h.durationMs,
                    isCompleted = h.isCompleted,
                    isWatchlist = false,
                    isHistory = true,
                    isFavorite = h.mediaId in favoriteIds,
                    status = computedStatus,
                    provider = h.provider,
                    episodeData = h.episodeData,
                    timestamp = h.lastWatchedTimestamp
                )
            }

        // 3. Build unified items for downloads that are not yet in watchlist or history
        val coveredMediaIds = watchlistMediaIds + historyMediaIds
        val fromDownloadsOnly = downloads
            .filter { it.mediaId !in coveredMediaIds }
            .groupBy { it.mediaId }
            .map { (mediaId, dlList) ->
                val primaryDl = dlList.first()
                val isTv = primaryDl.isTvSeries || primaryDl.mediaType.equals("tv", ignoreCase = true)
                val rawTitle = if (isTv && primaryDl.title.contains(": ")) {
                    primaryDl.title.substringBefore(": ")
                } else {
                    primaryDl.title
                }
                val cleanTitle = sanitizeTitle(rawTitle)
                val isAllCompleted = dlList.all { it.state == com.euthopiar.download.DownloadState.COMPLETED }
                val resolvedType = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE

                LibraryItem(
                    id = "download_$mediaId",
                    mediaId = mediaId,
                    title = cleanTitle,
                    episodeTitle = null,
                    seasonNumber = null,
                    episodeNumber = null,
                    posterUrl = primaryDl.posterUrl ?: primaryDl.backdropUrl,
                    backdropUrl = primaryDl.backdropUrl ?: primaryDl.posterUrl,
                    mediaType = resolvedType,
                    rating = null,
                    year = null,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    isCompleted = isAllCompleted,
                    isWatchlist = false,
                    isHistory = false,
                    isFavorite = mediaId in favoriteIds,
                    status = if (isAllCompleted) "COMPLETED" else "WATCHING",
                    provider = primaryDl.provider,
                    episodeData = null,
                    timestamp = dlList.maxOfOrNull { it.createdAt } ?: 0L
                )
            }

        val allCombined = (fromWatchlist + fromHistoryOnly + fromDownloadsOnly)

        val rawList = when (filter) {
            LibraryFilter.WATCHLIST -> fromWatchlist

            LibraryFilter.FAVORITES -> allCombined.filter { it.isFavorite }

            LibraryFilter.HISTORY -> {
                histories.map { h ->
                    val resolvedType = h.mediaType?.let {
                        try { MediaType.valueOf(it) } catch (_: Exception) { null }
                    } ?: if (h.seasonNumber != null || h.episodeNumber != null) MediaType.TV_SERIES else MediaType.MOVIE

                    LibraryItem(
                        id = h.id,
                        mediaId = h.mediaId,
                        title = sanitizeTitle(h.title),
                        episodeTitle = h.episodeTitle?.let { sanitizeTitle(it) },
                        seasonNumber = h.seasonNumber,
                        episodeNumber = h.episodeNumber,
                        posterUrl = h.posterUrl,
                        backdropUrl = h.backdropUrl,
                        mediaType = resolvedType,
                        rating = null,
                        year = null,
                        currentPositionMs = h.currentPositionMs,
                        durationMs = h.durationMs,
                        isCompleted = h.isCompleted,
                        isWatchlist = h.mediaId in watchlistMediaIds,
                        isHistory = true,
                        isFavorite = h.mediaId in favoriteIds,
                        status = if (h.isCompleted) "COMPLETED" else "WATCHING",
                        provider = h.provider,
                        episodeData = h.episodeData,
                        timestamp = h.lastWatchedTimestamp
                    )
                }
            }

            LibraryFilter.ALL -> allCombined

            LibraryFilter.WATCHING -> {
                allCombined.filter { it.status == "WATCHING" || (!it.isCompleted && it.currentPositionMs > 5000L) }
            }

            LibraryFilter.TO_BE_WATCHED -> {
                fromWatchlist.filter { it.status == "TO_BE_WATCHED" || (!it.isCompleted && it.currentPositionMs <= 5000L) }
            }

            LibraryFilter.COMPLETED -> {
                allCombined.filter { it.isCompleted || it.status == "COMPLETED" }
            }

            LibraryFilter.DOWNLOADS -> allCombined
        }

        when (sortOrder) {
            LibrarySortOrder.RECENTLY_ADDED -> rawList.sortedByDescending { it.timestamp.coerceAtLeast(0L) }
            LibrarySortOrder.TITLE -> rawList.sortedBy { it.title.lowercase().trim() }
            LibrarySortOrder.RELEASE_DATE -> rawList.sortedByDescending { it.year ?: 0 }
        }
    }

    val items: StateFlow<List<LibraryItem>> = combine(
        _selectedFilter,
        unfilteredItems,
        _pendingRemovalIds,
        _pendingClearHistory
    ) { filter, unfiltered, pendingRemovalIds, pendingClearHistory ->
        if (pendingClearHistory && filter == LibraryFilter.HISTORY) {
            emptyList()
        } else {
            unfiltered.filter { it.id !in pendingRemovalIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun selectFilter(filter: LibraryFilter) {
        exitSelectionMode()
        _selectedFilter.value = filter
        when (filter) {
            LibraryFilter.DOWNLOADS -> _selectedSection.value = LibrarySection.DOWNLOADS
            LibraryFilter.WATCHLIST, LibraryFilter.FAVORITES, LibraryFilter.TO_BE_WATCHED -> _selectedSection.value = LibrarySection.SAVED
            LibraryFilter.HISTORY, LibraryFilter.WATCHING -> _selectedSection.value = LibrarySection.PLAYLIST
            LibraryFilter.ALL -> {
                if (_selectedSection.value == LibrarySection.DOWNLOADS) {
                    _selectedSection.value = LibrarySection.SAVED
                }
            }
            LibraryFilter.COMPLETED -> {
                if (_selectedSection.value == LibrarySection.DOWNLOADS) {
                    _selectedSection.value = LibrarySection.SAVED
                }
            }
        }
    }

    fun startSelectionMode(initialItemId: String? = null) {
        _isSelectionMode.value = true
        _selectedItemIds.value = if (initialItemId != null) setOf(initialItemId) else emptySet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItemIds.value = emptySet()
    }

    fun toggleSelection(itemId: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (itemId in current) current - itemId else current + itemId
    }

    fun selectAll(allItems: List<LibraryItem>) {
        val current = _selectedItemIds.value
        val allIds = allItems.map { it.id }.toSet()
        if (current == allIds) {
            _selectedItemIds.value = emptySet()
        } else {
            _selectedItemIds.value = allIds
        }
    }

    fun deleteSelected(allItems: List<LibraryItem>) {
        val selectedIds = _selectedItemIds.value
        val toRemove = allItems.filter { it.id in selectedIds }
        removeGroupFromCurrentFilter(toRemove)
        exitSelectionMode()
    }

    fun moveToWatching(item: LibraryItem) {
        viewModelScope.launch {
            if (item.isWatchlist) {
                repository.updateWatchlistStatus(item.mediaId, "WATCHING")
            } else {
                repository.addToWatchlist(
                    mediaId = item.mediaId,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    backdropUrl = item.backdropUrl,
                    mediaType = item.mediaType.name,
                    rating = item.rating,
                    year = item.year,
                    status = "WATCHING"
                )
            }
        }
    }

    fun moveToToBeWatched(item: LibraryItem) {
        viewModelScope.launch {
            if (item.isWatchlist) {
                repository.updateWatchlistStatus(item.mediaId, "TO_BE_WATCHED")
            } else {
                repository.addToWatchlist(
                    mediaId = item.mediaId,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    backdropUrl = item.backdropUrl,
                    mediaType = item.mediaType.name,
                    rating = item.rating,
                    year = item.year,
                    status = "TO_BE_WATCHED"
                )
            }
        }
    }

    fun moveToBeWatched(item: LibraryItem) = moveToToBeWatched(item)

    fun markAsCompleted(item: LibraryItem) {
        viewModelScope.launch {
            repository.markAsCompleted(item.id)
            repository.updateWatchlistStatus(item.mediaId, "COMPLETED")
        }
    }

    fun removeFromCurrentFilter(item: LibraryItem) {
        removeGroupFromCurrentFilter(listOf(item))
    }

    fun removeGroupFromCurrentFilter(itemsToRemove: List<LibraryItem>) {
        if (itemsToRemove.isEmpty()) return
        viewModelScope.launch {
            val filter = _selectedFilter.value
            val mediaIds = itemsToRemove.map { it.mediaId }.distinct()
            val historyIds = itemsToRemove.map { it.id }.distinct()

            when (filter) {
                LibraryFilter.WATCHLIST, LibraryFilter.TO_BE_WATCHED -> {
                    repository.removeFromWatchlistBatch(mediaIds)
                }
                LibraryFilter.FAVORITES -> {
                    for (item in itemsToRemove) {
                        preferencesRepository.toggleFavorite(item.mediaId)
                    }
                }
                LibraryFilter.WATCHING -> {
                    for (item in itemsToRemove) {
                        if (item.isWatchlist) repository.updateWatchlistStatus(item.mediaId, "TO_BE_WATCHED")
                    }
                    repository.removeFromHistoryByMediaIdsBatch(mediaIds)
                    repository.removeFromHistoryBatch(historyIds)
                }
                LibraryFilter.COMPLETED -> {
                    repository.removeFromHistoryByMediaIdsBatch(mediaIds)
                    repository.removeFromHistoryBatch(historyIds)
                    repository.removeFromWatchlistBatch(mediaIds)
                }
                LibraryFilter.HISTORY -> {
                    repository.removeFromHistoryBatch(historyIds)
                    repository.removeFromHistoryByMediaIdsBatch(mediaIds)
                }
                LibraryFilter.DOWNLOADS -> {
                    for (item in itemsToRemove) {
                        downloadRepository.removeDownload(item.id)
                    }
                }
                LibraryFilter.ALL -> {
                    repository.removeFromWatchlistBatch(mediaIds)
                    repository.removeFromHistoryByMediaIdsBatch(mediaIds)
                    repository.removeFromHistoryBatch(historyIds)
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun removeFromCurrentFilterWithUndo(
        itemsToRemove: List<LibraryItem>,
        onUndoAvailable: (undoAction: () -> Unit) -> Unit
    ) {
        if (itemsToRemove.isEmpty()) return

        val watchlistSnapshots = itemsToRemove.filter { it.isWatchlist }.map { item ->
            com.euthopiar.data.local.WatchlistEntity(
                mediaId = item.mediaId,
                profileId = "default_user",
                title = item.title,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                mediaType = item.mediaType.name,
                rating = item.rating,
                year = item.year,
                status = item.status,
                addedTimestamp = item.timestamp
            )
        }

        // Execute deletion immediately so it is permanently applied in DB
        removeGroupFromCurrentFilter(itemsToRemove)

        onUndoAvailable {
            viewModelScope.launch {
                if (watchlistSnapshots.isNotEmpty()) {
                    repository.insertWatchlistBatch(watchlistSnapshots)
                }
                if (_selectedFilter.value == LibraryFilter.FAVORITES) {
                    for (item in itemsToRemove) {
                        preferencesRepository.toggleFavorite(item.mediaId)
                    }
                }
            }
        }
    }

    fun clearHistoryWithUndo(
        onUndoAvailable: (undoAction: () -> Unit) -> Unit
    ) {
        viewModelScope.launch {
            repository.clearHistory()
        }
        onUndoAvailable {
            // Watch history cleared
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500L)
            _isRefreshing.value = false
        }
    }
}
