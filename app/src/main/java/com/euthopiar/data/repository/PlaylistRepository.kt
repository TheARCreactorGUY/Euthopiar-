package com.euthopiar.data.repository

import android.content.Context
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.PlaybackHistoryDao
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.local.WatchlistDao
import com.euthopiar.data.local.WatchlistEntity
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class PlaylistItem(
    val mediaId: String,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val isTvSeries: Boolean = false,
    val year: Int? = null,
    val durationMinutes: Int? = null,
    val provider: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeData: String? = null
) {
    val uniqueKey: String
        get() = when {
            episodeData != null -> "${mediaId}_${episodeData}"
            seasonNumber != null && episodeNumber != null -> "${mediaId}_s${seasonNumber}e${episodeNumber}"
            else -> mediaId
        }
}

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val items: List<PlaylistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0L,
    val isSystem: Boolean = false
)

class PlaylistRepository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jsonStore = com.euthopiar.data.local.AtomicJsonStore(
        file = File(context.filesDir, "user_playlists.json"),
        serializer = kotlinx.serialization.builtins.ListSerializer(Playlist.serializer()),
        default = emptyList<Playlist>()
    )

    private val db = AppDatabase.getInstance(context)
    private val libraryRepository = LibraryRepository.getInstance(context)
    private val preferencesRepository = PreferencesRepository.getInstance(context)

    private val _userPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<Playlist>> = _userPlaylists.asStateFlow()

    @Volatile
    private var isDiskLoaded = false

    init {
        loadUserPlaylists()
    }

    private fun loadUserPlaylists() {
        scope.launch {
            awaitLoaded()
        }
    }

    suspend fun awaitLoaded(): List<Playlist> {
        if (isDiskLoaded && _userPlaylists.value.isNotEmpty()) return _userPlaylists.value
        try {
            val parsed = jsonStore.read()
            _userPlaylists.update { current ->
                if (current.isEmpty()) {
                    parsed
                } else {
                    val currentIds = current.map { it.id }.toSet()
                    val missingFromMemory = parsed.filterNot { it.id in currentIds }
                    current + missingFromMemory
                }
            }
            isDiskLoaded = true
        } catch (_: Exception) {
            isDiskLoaded = true
        }
        return _userPlaylists.value
    }

    /**
     * Unified stream of playlists combining built-in smart playlists (Watchlist, Continue Watching, Favorites)
     * with user-created custom playlists.
     */
    val allPlaylists: Flow<List<Playlist>> = combine(
        libraryRepository.watchlist,
        libraryRepository.continueWatching,
        preferencesRepository.favoriteMediaIds,
        _userPlaylists
    ) { watchlists, continueWatching, favoriteIds, userLists ->
        val result = mutableListOf<Playlist>()

        // 1. User playlists first if any exist
        result.addAll(userLists)

        // 2. Watchlist playlist (if items exist)
        if (watchlists.isNotEmpty()) {
            val items = watchlists.map { w ->
                val isTv = w.mediaType.equals("TV_SERIES", ignoreCase = true) || w.mediaType.equals("ANIME", ignoreCase = true)
                PlaylistItem(
                    mediaId = w.mediaId,
                    title = w.title.replace('+', ' ').trim(),
                    posterUrl = w.posterUrl,
                    backdropUrl = w.backdropUrl,
                    isTvSeries = isTv,
                    year = w.year
                )
            }
            result.add(
                Playlist(
                    id = "system_watchlist",
                    title = "Saved Watchlist",
                    items = items,
                    createdAt = watchlists.firstOrNull()?.addedTimestamp ?: 0L,
                    isSystem = true
                )
            )
        }

        // 3. Continue Watching playlist (if history exists)
        if (continueWatching.isNotEmpty()) {
            val items = continueWatching.distinctBy { it.mediaId }.map { h ->
                val isTv = h.seasonNumber != null || h.episodeNumber != null || h.mediaType?.contains("tv", ignoreCase = true) == true
                PlaylistItem(
                    mediaId = h.mediaId,
                    title = h.title.replace('+', ' ').trim(),
                    posterUrl = h.posterUrl,
                    backdropUrl = h.backdropUrl,
                    isTvSeries = isTv,
                    seasonNumber = h.seasonNumber,
                    episodeNumber = h.episodeNumber,
                    episodeTitle = h.episodeTitle?.replace('+', ' ')?.trim(),
                    provider = h.provider,
                    episodeData = h.episodeData
                )
            }
            result.add(
                Playlist(
                    id = "system_continue_watching",
                    title = "In Progress",
                    items = items,
                    createdAt = continueWatching.firstOrNull()?.lastWatchedTimestamp ?: 0L,
                    lastPlayedAt = continueWatching.firstOrNull()?.lastWatchedTimestamp ?: 0L,
                    isSystem = true
                )
            )
        }

        // 4. Favorites playlist (if favorites exist in watchlist)
        if (favoriteIds.isNotEmpty()) {
            val favItems = watchlists.filter { it.mediaId in favoriteIds }.map { w ->
                val isTv = w.mediaType.equals("TV_SERIES", ignoreCase = true) || w.mediaType.equals("ANIME", ignoreCase = true)
                PlaylistItem(
                    mediaId = w.mediaId,
                    title = w.title.replace('+', ' ').trim(),
                    posterUrl = w.posterUrl,
                    backdropUrl = w.backdropUrl,
                    isTvSeries = isTv,
                    year = w.year
                )
            }
            if (favItems.isNotEmpty()) {
                result.add(
                    Playlist(
                        id = "system_favorites",
                        title = "Favorites",
                        items = favItems,
                        isSystem = true
                    )
                )
            }
        }

        // Sort: most recently played first; if none played, most recently created
        result.sortedWith(
            compareByDescending<Playlist> { it.lastPlayedAt }
                .thenByDescending { it.createdAt }
        )
    }

    fun createPlaylist(title: String, initialItems: List<PlaylistItem> = emptyList(), syncToRemote: Boolean = true): Playlist {
        val cleanTitle = title.replace('+', ' ').trim().ifBlank { "New Playlist" }
        val cleanedInitialItems = initialItems.map {
            it.copy(title = it.title.replace('+', ' ').trim(), episodeTitle = it.episodeTitle?.replace('+', ' ')?.trim())
        }
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            items = cleanedInitialItems,
            createdAt = System.currentTimeMillis()
        )
        _userPlaylists.update { current ->
            if (current.any { it.id == newPlaylist.id }) current else listOf(newPlaylist) + current
        }
        scope.launch {
            try {
                jsonStore.update { diskList ->
                    if (diskList.any { it.id == newPlaylist.id }) diskList else listOf(newPlaylist) + diskList
                }
                if (syncToRemote) {
                    com.euthopiar.data.sync.MediaSyncManager.getInstance(context).notifyPlaylistsChanged()
                }
            } catch (_: Exception) {}
        }
        return newPlaylist
    }

    fun deletePlaylist(playlistId: String) {
        _userPlaylists.update { current ->
            current.filterNot { it.id == playlistId }
        }
        scope.launch {
            try {
                jsonStore.update { diskList ->
                    diskList.filterNot { it.id == playlistId }
                }
            } catch (_: Exception) {}
        }
    }

    fun addItemToPlaylist(playlistId: String, item: PlaylistItem, syncToRemote: Boolean = true) {
        val cleanItem = item.copy(
            title = item.title.replace('+', ' ').trim(),
            episodeTitle = item.episodeTitle?.replace('+', ' ')?.trim()
        )
        _userPlaylists.update { list ->
            list.map { pl ->
                if (pl.id == playlistId) {
                    val existing = pl.items.find { it.uniqueKey == cleanItem.uniqueKey }
                    val mergedItem = if (existing != null) {
                        cleanItem.copy(
                            posterUrl = cleanItem.posterUrl ?: existing.posterUrl,
                            backdropUrl = cleanItem.backdropUrl ?: existing.backdropUrl
                        )
                    } else cleanItem
                    val filtered = pl.items.filterNot { it.uniqueKey == cleanItem.uniqueKey }
                    pl.copy(items = listOf(mergedItem) + filtered)
                } else pl
            }
        }
        scope.launch {
            try {
                jsonStore.update { diskList ->
                    diskList.map { pl ->
                        if (pl.id == playlistId) {
                            val existing = pl.items.find { it.uniqueKey == cleanItem.uniqueKey }
                            val mergedItem = if (existing != null) {
                                cleanItem.copy(
                                    posterUrl = cleanItem.posterUrl ?: existing.posterUrl,
                                    backdropUrl = cleanItem.backdropUrl ?: existing.backdropUrl
                                )
                            } else cleanItem
                            val filtered = pl.items.filterNot { it.uniqueKey == cleanItem.uniqueKey }
                            pl.copy(items = listOf(mergedItem) + filtered)
                        } else pl
                    }
                }
                if (syncToRemote) {
                    com.euthopiar.data.sync.MediaSyncManager.getInstance(context).notifyPlaylistsChanged()
                }
            } catch (_: Exception) {}
        }
    }

    fun removeItemFromPlaylist(playlistId: String, itemKey: String) {
        val cleanKey = itemKey.trim()
        _userPlaylists.update { list ->
            list.map { pl ->
                if (pl.id == playlistId) {
                    pl.copy(items = pl.items.filterNot { it.uniqueKey == cleanKey || it.mediaId == cleanKey })
                } else pl
            }
        }
        scope.launch {
            try {
                jsonStore.update { diskList ->
                    diskList.map { pl ->
                        if (pl.id == playlistId) {
                            pl.copy(items = pl.items.filterNot { it.uniqueKey == cleanKey || it.mediaId == cleanKey })
                        } else pl
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateLastPlayed(playlistId: String) {
        val now = System.currentTimeMillis()
        _userPlaylists.update { list ->
            list.map { if (it.id == playlistId) it.copy(lastPlayedAt = now) else it }
        }
        scope.launch {
            try {
                jsonStore.update { diskList ->
                    diskList.map { if (it.id == playlistId) it.copy(lastPlayedAt = now) else it }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Returns all user-created custom playlists.
     */
    fun getUserPlaylists(): List<Playlist> = _userPlaylists.value

    /**
     * Backfills missing poster/backdrop artwork across all user playlists for items matching [mediaId] or [title].
     */
    suspend fun backfillItemArtwork(
        mediaId: String,
        title: String,
        posterUrl: String,
        backdropUrl: String?,
        year: Int? = null
    ) {
        if (posterUrl.isBlank()) return
        val normTitle = title.replace('+', ' ').trim().lowercase()
        var changed = false
        _userPlaylists.update { list ->
            list.map { pl ->
                var plChanged = false
                val updatedItems = pl.items.map { item ->
                    val matches = item.mediaId == mediaId || item.title.trim().lowercase() == normTitle
                    if (matches && (item.posterUrl.isNullOrBlank() || item.backdropUrl.isNullOrBlank())) {
                        plChanged = true
                        changed = true
                        item.copy(
                            posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: posterUrl,
                            backdropUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: backdropUrl ?: posterUrl,
                            year = item.year ?: year
                        )
                    } else {
                        item
                    }
                }
                if (plChanged) pl.copy(items = updatedItems) else pl
            }
        }
        if (changed) {
            try {
                jsonStore.update { diskList ->
                    diskList.map { pl ->
                        val updatedItems = pl.items.map { item ->
                            val matches = item.mediaId == mediaId || item.title.trim().lowercase() == normTitle
                            if (matches && (item.posterUrl.isNullOrBlank() || item.backdropUrl.isNullOrBlank())) {
                                item.copy(
                                    posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: posterUrl,
                                    backdropUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: backdropUrl ?: posterUrl,
                                    year = item.year ?: year
                                )
                            } else {
                                item
                            }
                        }
                        pl.copy(items = updatedItems)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Restores playlists from a configuration snapshot, preserving existing unique entries.
     */
    suspend fun restorePlaylists(playlists: List<Playlist>) {
        if (playlists.isEmpty()) return
        _userPlaylists.update { current ->
            val currentIds = current.map { it.id }.toSet()
            val newItems = playlists.filterNot { it.id in currentIds }
            current + newItems
        }
        try {
            jsonStore.update { diskList ->
                val diskIds = diskList.map { it.id }.toSet()
                val newItems = playlists.filterNot { it.id in diskIds }
                diskList + newItems
            }
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: PlaylistRepository? = null

        fun getInstance(context: Context): PlaylistRepository {
            return instance ?: synchronized(this) {
                instance ?: PlaylistRepository(context.applicationContext).also { instance = it }
            }
        }

        fun resetForTesting() {
            instance = null
        }
    }
}
