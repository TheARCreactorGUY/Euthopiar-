package com.euthopiar.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.core.model.CastMember
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.euthopiar.ui.util.UserError
import com.euthopiar.ui.util.toUserError

sealed interface MediaDetailsUiState {
    data object Loading : MediaDetailsUiState
    data class Success(val detail: MediaDetail) : MediaDetailsUiState
    data class Error(
        val error: UserError = UserError.Unknown,
        val message: String = ""
    ) : MediaDetailsUiState
}

class MediaDetailsViewModel(
    application: Application,
    private val repository: ProviderRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, ProviderRepository.Default)

    private val libraryRepository = LibraryRepository.getInstance(application)
    private val playlistRepository = com.euthopiar.data.repository.PlaylistRepository.getInstance(application)
    private var watchlistObserverJob: Job? = null
    private var playlistObserverJob: Job? = null
    private var playbackObserverJob: Job? = null

    private val _uiState = MutableStateFlow<MediaDetailsUiState>(MediaDetailsUiState.Loading)
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _selectedAudioType = MutableStateFlow(com.euthopiar.core.model.AudioReleaseType.SUB)
    val selectedAudioType: StateFlow<com.euthopiar.core.model.AudioReleaseType> = _selectedAudioType.asStateFlow()

    fun selectAudioType(type: com.euthopiar.core.model.AudioReleaseType) {
        _selectedAudioType.value = type
    }

    private val _watchlistStatus = MutableStateFlow<String?>(null)
    val watchlistStatus: StateFlow<String?> = _watchlistStatus.asStateFlow()

    private val _isWatchlist = MutableStateFlow(false)
    val isWatchlist: StateFlow<Boolean> = _isWatchlist.asStateFlow()

    private val _isInPlaylist = MutableStateFlow(false)
    val isInPlaylist: StateFlow<Boolean> = _isInPlaylist.asStateFlow()

    private val _latestPlayback = MutableStateFlow<PlaybackHistoryEntity?>(null)
    val latestPlayback: StateFlow<PlaybackHistoryEntity?> = _latestPlayback.asStateFlow()

    private val _episodePlaybackMap = MutableStateFlow<Map<Pair<Int, Int>, PlaybackHistoryEntity>>(emptyMap())
    val episodePlaybackMap: StateFlow<Map<Pair<Int, Int>, PlaybackHistoryEntity>> = _episodePlaybackMap.asStateFlow()

    private val _cast = MutableStateFlow<List<CastMember>>(emptyList())
    val cast: StateFlow<List<CastMember>> = _cast.asStateFlow()

    private var currentMediaId: String? = null
    private var currentProvider: String? = null
    private var currentType: MediaType? = null
    private var hasUserManuallySelectedSeason = false

    fun loadDetails(mediaId: String, provider: String? = null, type: MediaType? = null) {
        hasUserManuallySelectedSeason = false
        watchlistObserverJob?.cancel()
        watchlistObserverJob = viewModelScope.launch {
            libraryRepository.getWatchlistEntity(mediaId).collect { entity ->
                _watchlistStatus.value = entity?.status
                _isWatchlist.value = entity != null
            }
        }

        playlistObserverJob?.cancel()
        playlistObserverJob = viewModelScope.launch {
            playlistRepository.userPlaylists.collect { playlists ->
                _isInPlaylist.value = playlists.any { pl -> pl.items.any { it.mediaId == mediaId } }
            }
        }

        val effectiveProvider = provider ?: currentProvider
        playbackObserverJob?.cancel()
        playbackObserverJob = viewModelScope.launch {
            launch {
                libraryRepository.getLatestPlaybackForMedia(mediaId, effectiveProvider).collect { entity ->
                    _latestPlayback.value = entity
                    if (!hasUserManuallySelectedSeason && entity != null && entity.seasonNumber != null && entity.seasonNumber > 0) {
                        val currentDetail = (_uiState.value as? MediaDetailsUiState.Success)?.detail
                        val availableSeasons = currentDetail?.episodes?.map { it.seasonNumber }?.distinct() ?: emptyList()
                        if (availableSeasons.isEmpty() || entity.seasonNumber in availableSeasons) {
                            _selectedSeason.value = entity.seasonNumber
                        }
                    }
                }
            }
            launch {
                libraryRepository.getPlaybackForMedia(mediaId, effectiveProvider).collect { list ->
                    _episodePlaybackMap.value = list.mapNotNull { item ->
                        val s = item.seasonNumber
                        val e = item.episodeNumber
                        if (s != null && e != null) (s to e) to item else null
                    }.toMap()
                }
            }
        }

        if (currentMediaId == mediaId && _uiState.value is MediaDetailsUiState.Success) return

        currentMediaId = mediaId
        currentProvider = provider
        currentType = type

        _uiState.value = MediaDetailsUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val detail = repository.getDetails(
                    mediaId = mediaId,
                    type = type ?: MediaType.MOVIE,
                    providerName = provider
                )

                if ((detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME) && detail.episodes.isNotEmpty()) {
                    val availableSeasons = detail.episodes.map { it.seasonNumber }.distinct().sorted()
                    val lastPlayed = _latestPlayback.value?.seasonNumber
                    val hintSeason = if (mediaId.contains(':')) mediaId.split(':').getOrNull(1)?.toIntOrNull() else null
                    val targetSeason = when {
                        !hasUserManuallySelectedSeason && hintSeason != null && hintSeason in availableSeasons -> hintSeason
                        !hasUserManuallySelectedSeason && lastPlayed != null && lastPlayed in availableSeasons -> lastPlayed
                        1 in availableSeasons -> 1
                        else -> availableSeasons.firstOrNull { it > 0 } ?: availableSeasons.firstOrNull() ?: 1
                    }
                    _selectedSeason.value = targetSeason
                    val seasonEps = detail.episodes.filter { it.seasonNumber == targetSeason }
                    val mostCommonAudioType = seasonEps
                        .groupBy { it.audioType }
                        .maxByOrNull { it.value.size }?.key
                        ?: detail.episodes.firstOrNull()?.audioType
                        ?: com.euthopiar.core.model.AudioReleaseType.ORIGINAL
                    _selectedAudioType.value = mostCommonAudioType
                }

                // 1. Immediately reveal the screen with core details and existing cast
                _cast.value = detail.cast
                _uiState.value = MediaDetailsUiState.Success(detail)

                // 2. Asynchronously enrich cast members in the background without gating the UI
                launch {
                    try {
                        val castList = repository.getCast(
                            mediaId = detail.id,
                            imdbId = detail.imdbId,
                            type = detail.type,
                            providerName = provider
                        )
                        if (castList.isNotEmpty()) {
                            _cast.value = castList
                        }
                    } catch (t: Throwable) {
                        android.util.Log.d("MediaDetailsViewModel", "Non-critical async cast load failed: ${t.message}")
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.w("MediaDetailsViewModel", "Failed to load media details for $mediaId", t)
                _uiState.value = MediaDetailsUiState.Error(
                    error = t.toUserError()
                )
            }
        }
    }

    fun selectSeason(season: Int) {
        hasUserManuallySelectedSeason = true
        _selectedSeason.value = season
    }

    fun setWatchlistCategory(status: String, onError: ((String) -> Unit)? = null) {
        val state = _uiState.value
        if (state is MediaDetailsUiState.Success) {
            val previousStatus = _watchlistStatus.value
            val previousIsWatchlist = _isWatchlist.value

            if (status == "REMOVE") {
                _watchlistStatus.value = null
                _isWatchlist.value = false
            } else {
                _watchlistStatus.value = status
                _isWatchlist.value = true
            }

            viewModelScope.launch {
                try {
                    if (status == "REMOVE") {
                        libraryRepository.removeFromWatchlist(state.detail.id)
                    } else {
                        libraryRepository.setWatchlistStatus(state.detail, status)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MediaDetailsViewModel", "Failed to update watchlist", e)
                    _watchlistStatus.value = previousStatus
                    _isWatchlist.value = previousIsWatchlist
                    onError?.invoke("Failed to update watchlist")
                }
            }
        }
    }

    fun toggleWatchlist() {
        val current = _watchlistStatus.value
        if (current == null) {
            setWatchlistCategory("TO_BE_WATCHED")
        } else {
            setWatchlistCategory("REMOVE")
        }
    }

    fun toggleEpisodeWatched(detail: MediaDetail, episode: EpisodeItem) {
        val key = episode.seasonNumber to episode.episodeNumber
        val current = _episodePlaybackMap.value[key]
        val isCurrentlyWatched = current?.isCompleted == true || (current != null && current.durationMs > 0 && current.currentPositionMs >= current.durationMs * 0.9)
        viewModelScope.launch {
            if (isCurrentlyWatched) {
                libraryRepository.updatePlaybackProgress(
                    mediaId = detail.id,
                    title = detail.title,
                    episodeTitle = episode.title,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    currentPositionMs = 0L,
                    durationMs = current?.durationMs ?: 0L,
                    backdropUrl = detail.backdropUrl ?: detail.posterUrl ?: episode.thumbnail,
                    posterUrl = detail.posterUrl ?: detail.backdropUrl,
                    provider = currentProvider,
                    mediaType = detail.type.name
                )
            } else {
                val duration = if (current != null && current.durationMs > 0) current.durationMs else 1000L
                libraryRepository.updatePlaybackProgress(
                    mediaId = detail.id,
                    title = detail.title,
                    episodeTitle = episode.title,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    currentPositionMs = duration,
                    durationMs = duration,
                    backdropUrl = detail.backdropUrl ?: detail.posterUrl ?: episode.thumbnail,
                    posterUrl = detail.posterUrl ?: detail.backdropUrl,
                    provider = currentProvider,
                    mediaType = detail.type.name
                )
            }
        }
    }

    fun retry() {
        currentMediaId?.let { loadDetails(it, currentProvider, currentType) }
    }
}
