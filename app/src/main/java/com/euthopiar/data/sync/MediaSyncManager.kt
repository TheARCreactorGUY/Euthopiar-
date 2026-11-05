package com.euthopiar.data.sync

import android.content.Context
import android.util.Log
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.sync.simkl.SimklClient
import com.euthopiar.core.sync.simkl.SimklEpisodeItem
import com.euthopiar.core.sync.simkl.SimklIds
import com.euthopiar.core.sync.simkl.SimklScrobbleItem
import com.euthopiar.core.sync.simkl.SimklScrobblePayload
import com.euthopiar.core.sync.simkl.SimklSyncItem
import com.euthopiar.core.sync.simkl.SimklSyncPayload
import com.euthopiar.core.sync.trakt.*
import com.euthopiar.core.util.TmdbBridge
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.local.WatchlistEntity
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.repository.LibraryRepository
import com.euthopiar.data.repository.PlaylistItem
import com.euthopiar.data.repository.PlaylistRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified MediaSyncManager orchestrating full two-way synchronization across Trakt.tv and Simkl.
 *
 * Capabilities:
 * - Two-way Watchlist & Favorites sync (fetch remote with TMDB poster/backdrop hydration & push local)
 * - Two-way Playback Resume points sync (pushes local Continue Watching to Trakt/Simkl & pulls remote with TMDB hydration)
 * - Two-way Full Watch History sync (pushes local completed items & pulls remote history into both playback_history and watchlist)
 * - Two-way Custom Lists & Playlists sync (creates & populates Trakt custom lists from local playlists and imports remote lists)
 * - Automatic OAuth token refresh on HTTP 401
 */
class MediaSyncManager private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getInstance(context)
    private val watchlistDao = db.watchlistDao()
    private val playbackHistoryDao = db.playbackHistoryDao()
    private val libraryRepository = LibraryRepository.getInstance(context)
    private val playlistRepository = PlaylistRepository.getInstance(context)
    private val preferencesRepository = PreferencesRepository.getInstance(context)
    private val profileManager = ProfileManager.getInstance(context)

    private val traktClient = TraktClient.getInstance()
    private val simklClient = SimklClient.getInstance()
    private val httpClient = DohDns.metadataClient.newBuilder()
        .dispatcher(
            okhttp3.Dispatcher().apply {
                maxRequests = 12
                maxRequestsPerHost = 4
            }
        )
        .build()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val hydrationSemaphore = Semaphore(4)
    private val hydratedCache = ConcurrentHashMap<String, HydratedMediaMeta>()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private data class HydratedMediaMeta(
        val mediaId: String,
        val tmdbId: Long?,
        val imdbId: String?,
        val title: String,
        val year: Int?,
        val posterUrl: String?,
        val backdropUrl: String?,
        val rating: Double?,
        val durationMs: Long?
    )

    private fun currentProfileId(): String =
        profileManager.activeProfile.value?.id ?: "default_user"

    /**
     * Fast, single-request lightweight TMDB metadata lookup specifically for sync & poster repair.
     * Avoids the 5-8 sub-requests (OMDb, Fanart, AniZip, AniList, Jikan, credits, videos, recommendations)
     * that [TmdbBridge.fetchEnrichedDetails] fires, preventing rate-limiting and connection pool starvation.
     */
    private suspend fun fetchLightweightTmdbMeta(
        rawId: String,
        fallbackTitle: String,
        fallbackYear: Int?,
        isTv: Boolean
    ): HydratedMediaMeta? = withContext(Dispatchers.IO) {
        val cleanId = rawId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (cleanId.isBlank()) return@withContext null

        val primaryEndpoint = if (isTv) "tv" else "movie"
        val altEndpoint = if (isTv) "movie" else "tv"

        for (attempt in 1..2) {
            for (endpoint in listOf(primaryEndpoint, altEndpoint)) {
                try {
                    val apiKey = TmdbBridge.DEFAULT_API_KEY
                    val sep = "?"
                    val url = "${TmdbBridge.BASE_URL}/$endpoint/$cleanId${sep}append_to_response=external_ids&api_key=$apiKey"
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", TmdbBridge.DEFAULT_USER_AGENT)
                        .build()

                    val (code, body) = httpClient.newCall(req).execute().use { resp ->
                        resp.code to (resp.body?.string() ?: "")
                    }

                    if (code == 429) {
                        delay(350L * attempt)
                        break
                    }
                    if (code == 404) {
                        continue
                    }
                    if (code !in 200..299 || body.isBlank()) {
                        break
                    }

                    val root = JSONObject(body)
                    val numericId = root.optLong("id", 0L).takeIf { it > 0L } ?: cleanId.toLongOrNull()
                    val resolvedTitle = root.optString("title").ifBlank { root.optString("name") }.ifBlank { fallbackTitle }
                    val posterPath = root.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
                    val backdropPath = root.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" }
                    val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: posterUrl

                    val dateStr = root.optString("release_date").ifBlank { root.optString("first_air_date") }
                    val yearVal = dateStr.take(4).toIntOrNull() ?: fallbackYear
                    val voteAvg = root.optDouble("vote_average", 0.0).takeIf { !it.isNaN() && it > 0.0 }

                    val runtimeMins = root.optInt("runtime", 0).takeIf { it > 0 }
                        ?: root.optJSONArray("episode_run_time")?.optInt(0, 0)?.takeIf { it > 0 }
                    val durationMs = runtimeMins?.let { it * 60_000L }

                    val extIds = root.optJSONObject("external_ids")
                    val imdbStr = extIds?.optString("imdb_id")?.takeIf { it.startsWith("tt") }
                        ?: root.optString("imdb_id").takeIf { it.startsWith("tt") }

                    val finalIdStr = numericId?.toString() ?: cleanId
                    if (imdbStr != null && numericId != null) {
                        TmdbBridge.recordIdMapping(numericId.toString(), imdbStr, endpoint == "tv")
                    }

                    return@withContext HydratedMediaMeta(
                        mediaId = finalIdStr,
                        tmdbId = numericId,
                        imdbId = imdbStr,
                        title = resolvedTitle,
                        year = yearVal,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        rating = voteAvg,
                        durationMs = durationMs
                    )
                } catch (_: Exception) {}
            }
        }
        null
    }

    /**
     * Resolves canonical TMDB/IMDb IDs and enriches poster/backdrop/runtime via lightweight TMDB lookup.
     */
    private suspend fun hydrateMediaMeta(
        rawMediaId: String?,
        tmdbId: Long?,
        imdbId: String?,
        title: String,
        year: Int?,
        isTv: Boolean,
        fetchRemoteIfUncached: Boolean = true
    ): HydratedMediaMeta {
        val cleanRaw = rawMediaId?.trim()?.removePrefix("tmdb:")?.removePrefix("tmdb/")
        val initialTmdb = tmdbId ?: cleanRaw?.toLongOrNull()
        val initialImdb = imdbId?.takeIf { it.startsWith("tt") } ?: cleanRaw?.takeIf { it.startsWith("tt") }
        val cacheKey = "${if (isTv) "tv" else "movie"}:${initialTmdb ?: initialImdb ?: "${title.lowercase()}_${year ?: 0}"}"

        hydratedCache[cacheKey]?.takeIf { !it.posterUrl.isNullOrBlank() }?.let { return it }

        var resolvedTmdbStr: String? = initialTmdb?.toString()
        var resolvedImdbStr: String? = initialImdb

        if (resolvedTmdbStr != null) {
            TmdbBridge.getCachedEnrichedDetails(resolvedTmdbStr, isTv)?.let { cached ->
                if (!cached.posterUrl.isNullOrBlank()) {
                    val meta = HydratedMediaMeta(
                        mediaId = resolvedTmdbStr,
                        tmdbId = resolvedTmdbStr.toLongOrNull(),
                        imdbId = cached.imdbId ?: resolvedImdbStr,
                        title = cached.title.ifBlank { title },
                        year = cached.year ?: year,
                        posterUrl = cached.posterUrl,
                        backdropUrl = cached.backdropUrl ?: cached.posterUrl,
                        rating = cached.rating?.toDoubleOrNull(),
                        durationMs = cached.runtimeMinutes?.takeIf { it > 0 }?.let { it * 60_000L }
                    )
                    hydratedCache[cacheKey] = meta
                    return meta
                }
            }
        }

        if (!fetchRemoteIfUncached) {
            val fallbackId = resolvedTmdbStr ?: resolvedImdbStr ?: cleanRaw ?: title
            return HydratedMediaMeta(
                mediaId = fallbackId,
                tmdbId = resolvedTmdbStr?.toLongOrNull(),
                imdbId = resolvedImdbStr,
                title = title,
                year = year,
                posterUrl = null,
                backdropUrl = null,
                rating = null,
                durationMs = null
            )
        }

        return hydrationSemaphore.withPermit {
            hydratedCache[cacheKey]?.takeIf { !it.posterUrl.isNullOrBlank() }?.let { return@withPermit it }
            try {
                if (resolvedTmdbStr == null && !resolvedImdbStr.isNullOrBlank()) {
                    resolvedTmdbStr = TmdbBridge.findTmdbIdByImdb(httpClient, resolvedImdbStr!!, isTv)
                }
                if (resolvedTmdbStr == null && title.isNotBlank()) {
                    resolvedTmdbStr = TmdbBridge.searchTmdbId(httpClient, title, year, isTv)
                }

                val lookupId = resolvedTmdbStr ?: resolvedImdbStr
                val cachedEnriched = if (!lookupId.isNullOrBlank()) {
                    TmdbBridge.getCachedEnrichedDetails(lookupId, isTv)
                } else null

                val meta = if (cachedEnriched != null && !cachedEnriched.posterUrl.isNullOrBlank()) {
                    val finalTmdbStr = cachedEnriched.tmdbId.takeIf { it.isNotBlank() } ?: resolvedTmdbStr
                    val finalImdbStr = cachedEnriched.imdbId?.takeIf { it.startsWith("tt") } ?: resolvedImdbStr
                    HydratedMediaMeta(
                        mediaId = finalTmdbStr ?: finalImdbStr ?: cleanRaw ?: title,
                        tmdbId = finalTmdbStr?.toLongOrNull(),
                        imdbId = finalImdbStr,
                        title = cachedEnriched.title.ifBlank { title },
                        year = cachedEnriched.year ?: year,
                        posterUrl = cachedEnriched.posterUrl,
                        backdropUrl = cachedEnriched.backdropUrl ?: cachedEnriched.posterUrl,
                        rating = cachedEnriched.rating?.toDoubleOrNull(),
                        durationMs = cachedEnriched.runtimeMinutes?.takeIf { it > 0 }?.let { it * 60_000L }
                    )
                } else if (!lookupId.isNullOrBlank()) {
                    fetchLightweightTmdbMeta(lookupId, title, year, isTv)
                } else null

                if (meta != null) {
                    val mergedImdb = meta.imdbId ?: resolvedImdbStr
                    val finalMeta = meta.copy(imdbId = mergedImdb)
                    if (!finalMeta.posterUrl.isNullOrBlank()) {
                        hydratedCache[cacheKey] = finalMeta
                    }
                    return@withPermit finalMeta
                }

                val fallbackId = resolvedTmdbStr ?: resolvedImdbStr ?: cleanRaw ?: title
                HydratedMediaMeta(
                    mediaId = fallbackId,
                    tmdbId = resolvedTmdbStr?.toLongOrNull(),
                    imdbId = resolvedImdbStr,
                    title = title,
                    year = year,
                    posterUrl = null,
                    backdropUrl = null,
                    rating = null,
                    durationMs = null
                )
            } catch (e: Exception) {
                Log.w(TAG, "Metadata hydration fallback for $title: ${e.message}")
                val fallbackId = resolvedTmdbStr ?: resolvedImdbStr ?: cleanRaw ?: title
                HydratedMediaMeta(
                    mediaId = fallbackId,
                    tmdbId = resolvedTmdbStr?.toLongOrNull(),
                    imdbId = resolvedImdbStr,
                    title = title,
                    year = year,
                    posterUrl = null,
                    backdropUrl = null,
                    rating = null,
                    durationMs = null
                )
            }
        }
    }

    /**
     * Background repair job that finds any local Watchlist, Continue Watching, History, or Playlist items
     * missing posterUrl/backdropUrl (e.g. from older Trakt/Simkl syncs) and backfills their artwork via TMDB.
     */
    fun repairMissingLocalPostersAsync() {
        scope.launch {
            try {
                repairMissingLocalPosters()
            } catch (e: Exception) {
                Log.w(TAG, "Error repairing missing local posters", e)
            }
        }
    }

    suspend fun repairMissingLocalPosters() = withContext(Dispatchers.IO) {
        val profileId = currentProfileId()

        // 1. Repair Watchlist items missing posters
        val watchlists = watchlistDao.getAllWatchlist().filter {
            it.profileId == profileId && (it.posterUrl.isNullOrBlank() || it.backdropUrl.isNullOrBlank())
        }
        for (w in watchlists) {
            val isTv = !w.mediaType.equals("MOVIE", ignoreCase = true)
            val hydrated = hydrateMediaMeta(
                rawMediaId = w.mediaId,
                tmdbId = w.mediaId.toLongOrNull(),
                imdbId = w.mediaId.takeIf { it.startsWith("tt") },
                title = w.title,
                year = w.year,
                isTv = isTv,
                fetchRemoteIfUncached = true
            )
            if (!hydrated.posterUrl.isNullOrBlank()) {
                watchlistDao.insert(
                    w.copy(
                        posterUrl = w.posterUrl?.takeIf { it.isNotBlank() } ?: hydrated.posterUrl,
                        backdropUrl = w.backdropUrl?.takeIf { it.isNotBlank() } ?: hydrated.backdropUrl ?: hydrated.posterUrl,
                        rating = w.rating ?: hydrated.rating,
                        year = w.year ?: hydrated.year
                    )
                )
            }
        }

        // 2. Repair PlaybackHistory (Continue Watching & History) items missing posters
        val histories = playbackHistoryDao.getAllHistory().filter {
            it.profileId == profileId && (it.posterUrl.isNullOrBlank() || it.backdropUrl.isNullOrBlank())
        }
        for (h in histories) {
            val isTv = h.seasonNumber != null || h.episodeNumber != null ||
                h.mediaType?.contains("TV", ignoreCase = true) == true ||
                h.mediaType?.contains("ANIME", ignoreCase = true) == true
            val hydrated = hydrateMediaMeta(
                rawMediaId = h.mediaId,
                tmdbId = h.mediaId.toLongOrNull(),
                imdbId = h.mediaId.takeIf { it.startsWith("tt") },
                title = h.title,
                year = null,
                isTv = isTv,
                fetchRemoteIfUncached = true
            )
            if (!hydrated.posterUrl.isNullOrBlank()) {
                playbackHistoryDao.upsert(
                    h.copy(
                        posterUrl = h.posterUrl?.takeIf { it.isNotBlank() } ?: hydrated.posterUrl,
                        backdropUrl = h.backdropUrl?.takeIf { it.isNotBlank() } ?: hydrated.backdropUrl ?: hydrated.posterUrl
                    )
                )
            }
        }

        // 3. Repair Custom Playlists missing posters
        val playlists = playlistRepository.awaitLoaded()
        for (pl in playlists) {
            if (pl.isSystem) continue
            for (item in pl.items) {
                if (item.posterUrl.isNullOrBlank() || item.backdropUrl.isNullOrBlank()) {
                    val hydrated = hydrateMediaMeta(
                        rawMediaId = item.mediaId,
                        tmdbId = item.mediaId.toLongOrNull(),
                        imdbId = item.mediaId.takeIf { it.startsWith("tt") },
                        title = item.title,
                        year = item.year,
                        isTv = item.isTvSeries,
                        fetchRemoteIfUncached = true
                    )
                    if (!hydrated.posterUrl.isNullOrBlank()) {
                        playlistRepository.backfillItemArtwork(
                            mediaId = item.mediaId,
                            title = item.title,
                            posterUrl = hydrated.posterUrl,
                            backdropUrl = hydrated.backdropUrl,
                            year = hydrated.year
                        )
                    }
                }
            }
        }
    }

    /**
     * Ensures the Trakt token is valid, automatically refreshing via refresh_token if HTTP 401 occurs.
     */
    private suspend fun ensureValidTraktToken(currentToken: String): String {
        if (currentToken.isBlank()) return ""
        val profileRes = traktClient.getUserProfile(currentToken)
        if (profileRes.isSuccess) return currentToken

        val errMsg = profileRes.exceptionOrNull()?.message.orEmpty()
        if (errMsg.contains("401")) {
            val refreshToken = preferencesRepository.traktRefreshToken.first()
            if (refreshToken.isNotBlank()) {
                val refreshed = traktClient.refreshToken(refreshToken).getOrNull()
                if (refreshed != null && refreshed.accessToken.isNotBlank()) {
                    val username = preferencesRepository.traktUsername.first().ifBlank { "Trakt User" }
                    val newRefresh = refreshed.refreshToken.ifBlank { refreshToken }
                    preferencesRepository.setTraktAuth(refreshed.accessToken, newRefresh, username)
                    Log.i(TAG, "Successfully refreshed Trakt OAuth access token")
                    return refreshed.accessToken
                }
            }
        }
        return currentToken
    }

    /**
     * Executes a full two-way synchronization of all enabled Trakt & Simkl features.
     */
    suspend fun syncAll(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        if (_isSyncing.value && !force) {
            return@withContext Result.success(Unit)
        }

        _isSyncing.value = true
        _syncError.value = null
        _syncStatusMessage.value = "Starting media synchronization..."
        Log.i(TAG, "Starting full media sync...")

        try {
            val rawTraktToken = preferencesRepository.traktAccessToken.first()
            val traktToken = if (rawTraktToken.isNotBlank()) ensureValidTraktToken(rawTraktToken) else ""
            val simklToken = preferencesRepository.simklAccessToken.first()

            if (traktToken.isBlank() && simklToken.isBlank()) {
                repairMissingLocalPosters()
                _syncStatusMessage.value = "No cloud tracking accounts connected"
                return@withContext Result.success(Unit)
            }

            val traktWatchlistEnabled = preferencesRepository.traktSyncWatchlist.first()
            val traktPlaybackEnabled = preferencesRepository.traktSyncPlayback.first()
            val traktHistoryEnabled = preferencesRepository.traktSyncHistory.first()
            val traktListsEnabled = preferencesRepository.traktSyncLists.first()

            val simklWatchlistEnabled = preferencesRepository.simklSyncWatchlist.first()
            val simklHistoryEnabled = preferencesRepository.simklSyncHistory.first()

            val errors = mutableListOf<String>()

            // 1. Trakt Watchlist Two-Way Sync
            if (traktToken.isNotBlank() && traktWatchlistEnabled) {
                _syncStatusMessage.value = "Syncing Trakt Watchlist..."
                syncTraktWatchlistInternal(traktToken)?.let { errors.add(it) }
            }

            // 2. Simkl Watchlist Two-Way Sync
            if (simklToken.isNotBlank() && simklWatchlistEnabled) {
                _syncStatusMessage.value = "Syncing Simkl Watchlist..."
                syncSimklWatchlistInternal(simklToken)?.let { errors.add(it) }
            }

            // 3. Trakt Playback Progress (Two-Way Continue Watching)
            if (traktToken.isNotBlank() && traktPlaybackEnabled) {
                _syncStatusMessage.value = "Syncing Continue Watching..."
                syncTraktPlaybackInternal(traktToken)?.let { errors.add(it) }
            }

            // 4. Simkl Playback / Watching Progress (Two-Way Continue Watching)
            if (simklToken.isNotBlank() && (simklWatchlistEnabled || simklHistoryEnabled)) {
                _syncStatusMessage.value = "Syncing Simkl Watching Progress..."
                syncSimklPlaybackInternal(simklToken)?.let { errors.add(it) }
            }

            // 5. Trakt Watch History (Two-Way Completed & History)
            if (traktToken.isNotBlank() && traktHistoryEnabled) {
                _syncStatusMessage.value = "Syncing Trakt Watch History..."
                syncTraktHistoryInternal(traktToken)?.let { errors.add(it) }
            }

            // 6. Simkl History (Two-Way Completed & History)
            if (simklToken.isNotBlank() && simklHistoryEnabled) {
                _syncStatusMessage.value = "Syncing Simkl Watch History..."
                syncSimklHistoryInternal(simklToken)?.let { errors.add(it) }
            }

            // 7. Trakt Custom Lists & Playlists (Two-Way Playlists & Favorites)
            if (traktToken.isNotBlank() && traktListsEnabled) {
                _syncStatusMessage.value = "Syncing Custom Playlists..."
                syncTraktListsInternal(traktToken)?.let { errors.add(it) }
            }

            // 8. Repair any remaining local items missing poster/backdrop artwork
            repairMissingLocalPosters()

            val timestamp = System.currentTimeMillis()
            preferencesRepository.setLastMediaSyncTimestamp(timestamp)

            if (errors.isNotEmpty()) {
                val combinedErr = errors.first()
                _syncError.value = combinedErr
                _syncStatusMessage.value = "Synced with warnings: $combinedErr"
                Log.w(TAG, "Media sync completed with warnings: $errors")
            } else {
                _syncStatusMessage.value = "Synchronization completed successfully"
                Log.i(TAG, "Media sync finished successfully at $timestamp")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Media sync failed", e)
            _syncError.value = e.message ?: "Sync encountered an unexpected error"
            _syncStatusMessage.value = "Sync failed: ${e.message}"
            Result.failure(e)
        } finally {
            _isSyncing.value = false
            scope.launch {
                delay(3500)
                if (_syncStatusMessage.value?.startsWith("Sync") == true) {
                    _syncStatusMessage.value = null
                }
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*                               TRAKT WATCHLIST                               */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncTraktWatchlistInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = traktClient.getWatchlist(token)
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Failed to fetch Trakt watchlist"
            Log.w(TAG, msg)
            return msg
        }

        val remoteItems = result.getOrNull() ?: emptyList()
        val localItems = watchlistDao.getAllWatchlist().filter { it.profileId == profileId }
        val localByMediaId = localItems.associateBy { it.mediaId }.toMutableMap()

        // 1. Remote -> Local (Pull & Hydrate with TMDB posters/backdrops)
        for (item in remoteItems) {
            val isMovie = item.type.equals("movie", ignoreCase = true)
            val movie = item.movie
            val show = item.show

            val tmdbId = movie?.ids?.tmdb ?: show?.ids?.tmdb
            val imdbId = movie?.ids?.imdb ?: show?.ids?.imdb
            val title = movie?.title ?: show?.title ?: continue
            val year = movie?.year ?: show?.year

            val hydrated = hydrateMediaMeta(
                rawMediaId = tmdbId?.toString() ?: imdbId,
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = title,
                year = year,
                isTv = !isMovie
            )

            val existing = localByMediaId[hydrated.mediaId]
                ?: imdbId?.let { localByMediaId[it] }
                ?: tmdbId?.toString()?.let { localByMediaId[it] }

            val addedEpoch = parseIsoTimestamp(item.listed_at) ?: System.currentTimeMillis()

            if (existing == null) {
                val entity = WatchlistEntity(
                    mediaId = hydrated.mediaId,
                    profileId = profileId,
                    title = hydrated.title,
                    posterUrl = hydrated.posterUrl,
                    backdropUrl = hydrated.backdropUrl,
                    mediaType = if (isMovie) "MOVIE" else "TV_SERIES",
                    rating = hydrated.rating,
                    year = hydrated.year,
                    status = "TO_BE_WATCHED",
                    addedTimestamp = addedEpoch
                )
                watchlistDao.insert(entity)
                localByMediaId[hydrated.mediaId] = entity
            } else if (existing.posterUrl.isNullOrBlank() && !hydrated.posterUrl.isNullOrBlank()) {
                val updated = existing.copy(
                    posterUrl = hydrated.posterUrl,
                    backdropUrl = existing.backdropUrl ?: hydrated.backdropUrl,
                    rating = existing.rating ?: hydrated.rating,
                    year = existing.year ?: hydrated.year
                )
                watchlistDao.insert(updated)
                localByMediaId[existing.mediaId] = updated
            }
        }

        // 2. Local -> Remote (Push unsynced local watchlist items to Trakt)
        val remoteTmdbIds = remoteItems.mapNotNull { it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb }.map { it.toString() }.toSet()
        val remoteImdbIds = remoteItems.mapNotNull { it.movie?.ids?.imdb ?: it.show?.ids?.imdb }.toSet()
        val remoteTitles = remoteItems.mapNotNull { (it.movie?.title ?: it.show?.title)?.lowercase()?.trim() }.toSet()

        val unsyncedMovies = mutableListOf<TraktMovie>()
        val unsyncedShows = mutableListOf<TraktShow>()

        for (local in localItems) {
            if (local.status.equals("COMPLETED", ignoreCase = true)) continue
            val isMovie = local.mediaType.equals("MOVIE", ignoreCase = true)
            val hydrated = hydrateMediaMeta(
                rawMediaId = local.mediaId,
                tmdbId = local.mediaId.toLongOrNull(),
                imdbId = local.mediaId.takeIf { it.startsWith("tt") },
                title = local.title,
                year = local.year,
                isTv = !isMovie,
                fetchRemoteIfUncached = local.mediaId.toLongOrNull() == null && !local.mediaId.startsWith("tt")
            )

            val tmdbId = hydrated.tmdbId
            val imdbId = hydrated.imdbId

            val alreadyRemote = (tmdbId != null && remoteTmdbIds.contains(tmdbId.toString())) ||
                (imdbId != null && remoteImdbIds.contains(imdbId)) ||
                remoteTitles.contains(local.title.lowercase().trim())

            if (!alreadyRemote) {
                val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                if (isMovie) {
                    unsyncedMovies.add(TraktMovie(title = local.title, year = local.year ?: hydrated.year, ids = ids))
                } else {
                    unsyncedShows.add(TraktShow(title = local.title, year = local.year ?: hydrated.year, ids = ids))
                }
            }
        }

        if (unsyncedMovies.isNotEmpty() || unsyncedShows.isNotEmpty()) {
            val payload = TraktSyncItemsPayload(movies = unsyncedMovies, shows = unsyncedShows)
            val pushRes = traktClient.addToWatchlist(token, payload)
            if (pushRes.isFailure) {
                Log.w(TAG, "Failed to push local watchlist to Trakt: ${pushRes.exceptionOrNull()?.message}")
            }
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                               SIMKL WATCHLIST                               */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncSimklWatchlistInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = simklClient.getAllItems(token, "plantowatch")
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Failed to fetch Simkl watchlist"
            Log.w(TAG, msg)
            return msg
        }

        val parsed = result.getOrNull() ?: return null
        val localItems = watchlistDao.getAllWatchlist().filter { it.profileId == profileId }
        val localByMediaId = localItems.associateBy { it.mediaId }.toMutableMap()

        // 1. Remote -> Local: Process Movies
        for (m in parsed.movies) {
            val movie = m.movie ?: continue
            val tmdbId = movie.ids?.tmdb
            val imdbId = movie.ids?.imdb
            val hydrated = hydrateMediaMeta(
                rawMediaId = tmdbId?.toString() ?: imdbId ?: movie.ids?.simkl?.toString(),
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = movie.title,
                year = movie.year,
                isTv = false
            )
            val existing = localByMediaId[hydrated.mediaId]
            if (existing == null) {
                val entity = WatchlistEntity(
                    mediaId = hydrated.mediaId,
                    profileId = profileId,
                    title = hydrated.title,
                    posterUrl = hydrated.posterUrl,
                    backdropUrl = hydrated.backdropUrl,
                    mediaType = "MOVIE",
                    rating = hydrated.rating,
                    year = hydrated.year,
                    status = "TO_BE_WATCHED",
                    addedTimestamp = System.currentTimeMillis()
                )
                watchlistDao.insert(entity)
                localByMediaId[hydrated.mediaId] = entity
            } else if (existing.posterUrl.isNullOrBlank() && !hydrated.posterUrl.isNullOrBlank()) {
                watchlistDao.insert(
                    existing.copy(
                        posterUrl = hydrated.posterUrl,
                        backdropUrl = existing.backdropUrl ?: hydrated.backdropUrl,
                        rating = existing.rating ?: hydrated.rating,
                        year = existing.year ?: hydrated.year
                    )
                )
            }
        }

        // Process Shows & Anime
        val allShows = parsed.shows.mapNotNull { it.show } + parsed.anime.mapNotNull { it.anime }
        for (show in allShows) {
            val tmdbId = show.ids?.tmdb
            val imdbId = show.ids?.imdb
            val hydrated = hydrateMediaMeta(
                rawMediaId = tmdbId?.toString() ?: imdbId ?: show.ids?.simkl?.toString(),
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = show.title,
                year = show.year,
                isTv = true
            )
            val existing = localByMediaId[hydrated.mediaId]
            if (existing == null) {
                val entity = WatchlistEntity(
                    mediaId = hydrated.mediaId,
                    profileId = profileId,
                    title = hydrated.title,
                    posterUrl = hydrated.posterUrl,
                    backdropUrl = hydrated.backdropUrl,
                    mediaType = "TV_SERIES",
                    rating = hydrated.rating,
                    year = hydrated.year,
                    status = "TO_BE_WATCHED",
                    addedTimestamp = System.currentTimeMillis()
                )
                watchlistDao.insert(entity)
                localByMediaId[hydrated.mediaId] = entity
            } else if (existing.posterUrl.isNullOrBlank() && !hydrated.posterUrl.isNullOrBlank()) {
                watchlistDao.insert(
                    existing.copy(
                        posterUrl = hydrated.posterUrl,
                        backdropUrl = existing.backdropUrl ?: hydrated.backdropUrl,
                        rating = existing.rating ?: hydrated.rating,
                        year = existing.year ?: hydrated.year
                    )
                )
            }
        }

        // 2. Local -> Remote: Push unsynced local watchlist items to Simkl
        val remoteTmdbIds = (
            parsed.movies.mapNotNull { it.movie?.ids?.tmdb } +
            parsed.shows.mapNotNull { it.show?.ids?.tmdb } +
            parsed.anime.mapNotNull { it.anime?.ids?.tmdb }
        ).map { it.toString() }.toSet()
        val remoteImdbIds = (
            parsed.movies.mapNotNull { it.movie?.ids?.imdb } +
            parsed.shows.mapNotNull { it.show?.ids?.imdb } +
            parsed.anime.mapNotNull { it.anime?.ids?.imdb }
        ).toSet()

        val pushMovies = mutableListOf<SimklSyncItem>()
        val pushShows = mutableListOf<SimklSyncItem>()

        for (local in localItems) {
            if (local.status.equals("COMPLETED", ignoreCase = true)) continue
            val isMovie = local.mediaType.equals("MOVIE", ignoreCase = true)
            val tmdbId = local.mediaId.toLongOrNull()
            val imdbId = if (local.mediaId.startsWith("tt")) local.mediaId else null

            val alreadyRemote = (tmdbId != null && remoteTmdbIds.contains(tmdbId.toString())) ||
                (imdbId != null && remoteImdbIds.contains(imdbId))

            if (!alreadyRemote) {
                val toStatus = if (local.status.equals("WATCHING", ignoreCase = true)) "watching" else "plantowatch"
                val ids = SimklIds(tmdb = tmdbId, imdb = imdbId)
                val item = SimklSyncItem(title = local.title, to = toStatus, ids = ids)
                if (isMovie) pushMovies.add(item) else pushShows.add(item)
            }
        }

        if (pushMovies.isNotEmpty() || pushShows.isNotEmpty()) {
            val payload = SimklSyncPayload(movies = pushMovies.take(50), shows = pushShows.take(50))
            simklClient.addToLibrary(token, payload)
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                  TRAKT PLAYBACK (TWO-WAY CONTINUE WATCHING)                 */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncTraktPlaybackInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = traktClient.getPlayback(token)
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Failed to fetch Trakt playback progress"
            Log.w(TAG, msg)
            return msg
        }

        val remoteProgress = result.getOrNull() ?: emptyList()
        val allLocalHistory = playbackHistoryDao.getAllHistory().filter { it.profileId == profileId }

        // 1. Remote -> Local (Pull paused items from Trakt into Continue Watching with TMDB hydration)
        val remoteMatchedKeys = mutableSetOf<String>()

        for (item in remoteProgress) {
            val isEpisode = item.type.equals("episode", ignoreCase = true)
            val progressPercent = item.progress.coerceIn(1f, 99f)
            val pausedEpoch = parseIsoTimestamp(item.paused_at) ?: System.currentTimeMillis()

            if (isEpisode) {
                val show = item.show ?: continue
                val episode = item.episode ?: continue
                val hydrated = hydrateMediaMeta(
                    rawMediaId = show.ids.tmdb?.toString() ?: show.ids.imdb,
                    tmdbId = show.ids.tmdb,
                    imdbId = show.ids.imdb,
                    title = show.title,
                    year = show.year,
                    isTv = true
                )
                val mediaId = hydrated.mediaId
                val seasonNum = episode.season.coerceAtLeast(1)
                val epNum = episode.number.coerceAtLeast(1)
                remoteMatchedKeys.add("${mediaId}_s${seasonNum}_e${epNum}")
                hydrated.tmdbId?.let { remoteMatchedKeys.add("${it}_s${seasonNum}_e${epNum}") }
                hydrated.imdbId?.let { remoteMatchedKeys.add("${it}_s${seasonNum}_e${epNum}") }

                val existing = allLocalHistory.firstOrNull {
                    (it.mediaId == mediaId || it.mediaId == hydrated.imdbId || it.mediaId == hydrated.tmdbId?.toString()) &&
                        it.seasonNumber == seasonNum && it.episodeNumber == epNum
                }

                val durationMs = existing?.durationMs?.takeIf { it > 60_000L }
                    ?: hydrated.durationMs?.takeIf { it > 60_000L }
                    ?: 2_700_000L
                val positionMs = ((progressPercent / 100f) * durationMs).toLong().coerceAtLeast(5_000L)

                val provider = existing?.provider ?: "tmdb"
                val entityId = existing?.id ?: PlaybackHistoryEntity.buildId(provider, mediaId, seasonNum, epNum, profileId)

                if (existing == null || existing.currentPositionMs < 1000L || existing.posterUrl.isNullOrBlank() || pausedEpoch > existing.lastWatchedTimestamp) {
                    val entity = PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = existing?.mediaId ?: mediaId,
                        title = hydrated.title.ifBlank { show.title },
                        episodeTitle = episode.title ?: existing?.episodeTitle,
                        seasonNumber = seasonNum,
                        episodeNumber = epNum,
                        posterUrl = existing?.posterUrl ?: hydrated.posterUrl,
                        backdropUrl = existing?.backdropUrl ?: hydrated.backdropUrl,
                        currentPositionMs = if (existing != null && existing.lastWatchedTimestamp >= pausedEpoch && existing.currentPositionMs > 1000L) existing.currentPositionMs else positionMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = maxOf(existing?.lastWatchedTimestamp ?: 0L, pausedEpoch),
                        isCompleted = false,
                        provider = provider,
                        mediaType = "TV_SERIES",
                        episodeData = existing?.episodeData,
                        isDismissedFromContinueWatching = false,
                        profileId = profileId
                    )
                    playbackHistoryDao.upsert(entity)
                }
            } else {
                val movie = item.movie ?: continue
                val hydrated = hydrateMediaMeta(
                    rawMediaId = movie.ids.tmdb?.toString() ?: movie.ids.imdb,
                    tmdbId = movie.ids.tmdb,
                    imdbId = movie.ids.imdb,
                    title = movie.title,
                    year = movie.year,
                    isTv = false
                )
                val mediaId = hydrated.mediaId
                remoteMatchedKeys.add(mediaId)
                hydrated.tmdbId?.let { remoteMatchedKeys.add(it.toString()) }
                hydrated.imdbId?.let { remoteMatchedKeys.add(it) }

                val existing = allLocalHistory.firstOrNull {
                    it.mediaId == mediaId || it.mediaId == hydrated.imdbId || it.mediaId == hydrated.tmdbId?.toString()
                }

                val durationMs = existing?.durationMs?.takeIf { it > 60_000L }
                    ?: hydrated.durationMs?.takeIf { it > 60_000L }
                    ?: 6_000_000L
                val positionMs = ((progressPercent / 100f) * durationMs).toLong().coerceAtLeast(5_000L)

                val provider = existing?.provider ?: "tmdb"
                val entityId = existing?.id ?: PlaybackHistoryEntity.buildId(provider, mediaId, null, null, profileId)

                if (existing == null || existing.currentPositionMs < 1000L || existing.posterUrl.isNullOrBlank() || pausedEpoch > existing.lastWatchedTimestamp) {
                    val entity = PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = existing?.mediaId ?: mediaId,
                        title = hydrated.title.ifBlank { movie.title },
                        episodeTitle = null,
                        seasonNumber = null,
                        episodeNumber = null,
                        posterUrl = existing?.posterUrl ?: hydrated.posterUrl,
                        backdropUrl = existing?.backdropUrl ?: hydrated.backdropUrl,
                        currentPositionMs = if (existing != null && existing.lastWatchedTimestamp >= pausedEpoch && existing.currentPositionMs > 1000L) existing.currentPositionMs else positionMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = maxOf(existing?.lastWatchedTimestamp ?: 0L, pausedEpoch),
                        isCompleted = false,
                        provider = provider,
                        mediaType = "MOVIE",
                        episodeData = existing?.episodeData,
                        isDismissedFromContinueWatching = false,
                        profileId = profileId
                    )
                    playbackHistoryDao.upsert(entity)
                }
            }
        }

        // 2. Local -> Remote (Push local Continue Watching items to Trakt via /scrobble/pause)
        val localContinueWatching = allLocalHistory
            .filter { !it.isCompleted && it.currentPositionMs > 1000L && !it.isDismissedFromContinueWatching }
            .groupBy { it.mediaId }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.lastWatchedTimestamp } }

        for (local in localContinueWatching.take(25)) {
            val isTv = local.seasonNumber != null || local.episodeNumber != null ||
                local.mediaType?.contains("TV", ignoreCase = true) == true ||
                local.mediaType?.contains("ANIME", ignoreCase = true) == true

            val key = if (isTv) {
                "${local.mediaId}_s${local.seasonNumber ?: 1}_e${local.episodeNumber ?: 1}"
            } else {
                local.mediaId
            }

            if (key in remoteMatchedKeys) continue

            val hydrated = hydrateMediaMeta(
                rawMediaId = local.mediaId,
                tmdbId = local.mediaId.toLongOrNull(),
                imdbId = local.mediaId.takeIf { it.startsWith("tt") },
                title = local.title,
                year = null,
                isTv = isTv,
                fetchRemoteIfUncached = local.mediaId.toLongOrNull() == null && !local.mediaId.startsWith("tt")
            )

            val tmdbId = hydrated.tmdbId
            val imdbId = hydrated.imdbId
            if (tmdbId == null && imdbId == null && local.title.isBlank()) continue

            val progressPercent = if (local.durationMs > 0L) {
                ((local.currentPositionMs.toFloat() / local.durationMs.toFloat()) * 100f).coerceIn(1f, 91f)
            } else {
                25f
            }

            val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
            val scrobbleBody = if (isTv) {
                TraktScrobbleBody(
                    show = TraktShow(title = local.title, year = hydrated.year, ids = ids),
                    episode = TraktEpisode(
                        season = local.seasonNumber ?: 1,
                        number = local.episodeNumber ?: 1,
                        title = local.episodeTitle
                    ),
                    progress = progressPercent
                )
            } else {
                TraktScrobbleBody(
                    movie = TraktMovie(title = local.title, year = hydrated.year, ids = ids),
                    progress = progressPercent
                )
            }
            traktClient.scrobble("pause", token, scrobbleBody)
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                  SIMKL PLAYBACK (TWO-WAY CONTINUE WATCHING)                 */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncSimklPlaybackInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = simklClient.getAllItems(token, "watching")
        if (result.isFailure) {
            return null
        }

        val parsed = result.getOrNull() ?: return null
        val allLocalHistory = playbackHistoryDao.getAllHistory().filter { it.profileId == profileId }
        val remoteIds = mutableSetOf<String>()

        // 1. Remote -> Local: Pull Simkl "watching" items into Continue Watching & Watchlist
        val watchingShows = parsed.shows.mapNotNull { it.show } + parsed.anime.mapNotNull { it.anime }
        for (show in watchingShows) {
            val tmdbId = show.ids?.tmdb
            val imdbId = show.ids?.imdb
            val hydrated = hydrateMediaMeta(
                rawMediaId = tmdbId?.toString() ?: imdbId ?: show.ids?.simkl?.toString(),
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = show.title,
                year = show.year,
                isTv = true
            )
            remoteIds.add(hydrated.mediaId)
            val existingHistory = allLocalHistory.firstOrNull { it.mediaId == hydrated.mediaId }
            if (existingHistory == null) {
                val durationMs = hydrated.durationMs?.takeIf { it > 60_000L } ?: 1_440_000L
                val entityId = PlaybackHistoryEntity.buildId("tmdb", hydrated.mediaId, 1, 1, profileId)
                playbackHistoryDao.upsert(
                    PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = hydrated.mediaId,
                        title = hydrated.title,
                        episodeTitle = "Episode 1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        posterUrl = hydrated.posterUrl,
                        backdropUrl = hydrated.backdropUrl,
                        currentPositionMs = (durationMs * 0.25f).toLong(),
                        durationMs = durationMs,
                        lastWatchedTimestamp = System.currentTimeMillis(),
                        isCompleted = false,
                        provider = "tmdb",
                        mediaType = "TV_SERIES",
                        profileId = profileId
                    )
                )
            }
        }

        for (m in parsed.movies) {
            val movie = m.movie ?: continue
            val tmdbId = movie.ids?.tmdb
            val imdbId = movie.ids?.imdb
            val hydrated = hydrateMediaMeta(
                rawMediaId = tmdbId?.toString() ?: imdbId ?: movie.ids?.simkl?.toString(),
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = movie.title,
                year = movie.year,
                isTv = false
            )
            remoteIds.add(hydrated.mediaId)
            val existingHistory = allLocalHistory.firstOrNull { it.mediaId == hydrated.mediaId }
            if (existingHistory == null) {
                val durationMs = hydrated.durationMs?.takeIf { it > 60_000L } ?: 6_000_000L
                val entityId = PlaybackHistoryEntity.buildId("tmdb", hydrated.mediaId, null, null, profileId)
                playbackHistoryDao.upsert(
                    PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = hydrated.mediaId,
                        title = hydrated.title,
                        episodeTitle = null,
                        seasonNumber = null,
                        episodeNumber = null,
                        posterUrl = hydrated.posterUrl,
                        backdropUrl = hydrated.backdropUrl,
                        currentPositionMs = (durationMs * 0.25f).toLong(),
                        durationMs = durationMs,
                        lastWatchedTimestamp = System.currentTimeMillis(),
                        isCompleted = false,
                        provider = "tmdb",
                        mediaType = "MOVIE",
                        profileId = profileId
                    )
                )
            }
        }

        // 2. Local -> Remote: Push local Continue Watching items to Simkl
        val localContinueWatching = allLocalHistory
            .filter { !it.isCompleted && it.currentPositionMs > 1000L && !it.isDismissedFromContinueWatching }
            .groupBy { it.mediaId }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.lastWatchedTimestamp } }

        val pushMovies = mutableListOf<SimklSyncItem>()
        val pushShows = mutableListOf<SimklSyncItem>()

        for (local in localContinueWatching.take(30)) {
            if (local.mediaId in remoteIds) continue
            val isTv = local.seasonNumber != null || local.episodeNumber != null ||
                local.mediaType?.contains("TV", ignoreCase = true) == true ||
                local.mediaType?.contains("ANIME", ignoreCase = true) == true

            val tmdbId = local.mediaId.toLongOrNull()
            val imdbId = local.mediaId.takeIf { it.startsWith("tt") }
            val ids = SimklIds(tmdb = tmdbId, imdb = imdbId)
            val syncItem = SimklSyncItem(title = local.title, to = "watching", ids = ids)
            if (isTv) pushShows.add(syncItem) else pushMovies.add(syncItem)
        }

        if (pushMovies.isNotEmpty() || pushShows.isNotEmpty()) {
            simklClient.addToLibrary(token, SimklSyncPayload(movies = pushMovies, shows = pushShows))
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                     TRAKT WATCH HISTORY (TWO-WAY COMPLETED)                 */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncTraktHistoryInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = traktClient.getHistory(token, page = 1, limit = 100)
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Failed to fetch Trakt history"
            Log.w(TAG, msg)
            return msg
        }

        val historyItems = result.getOrNull() ?: emptyList()
        val allLocalHistory = playbackHistoryDao.getAllHistory().filter { it.profileId == profileId }
        val localWatchlist = watchlistDao.getAllWatchlist().filter { it.profileId == profileId }

        val remoteCompletedMediaIds = mutableSetOf<String>()

        // 1. Remote -> Local: Populate both Watchlist status AND playback_history Completed tab
        // Hydrate up to 35 most recent unique items so sync remains fast and responsive
        var hydratedCount = 0
        val seenRemoteKeys = mutableSetOf<String>()

        for (item in historyItems) {
            val isMovie = item.type.equals("movie", ignoreCase = true)
            val movie = item.movie
            val show = item.show
            val episode = item.episode

            val rawTmdb = if (isMovie) movie?.ids?.tmdb else show?.ids?.tmdb
            val rawImdb = if (isMovie) movie?.ids?.imdb else show?.ids?.imdb
            val title = (if (isMovie) movie?.title else show?.title) ?: continue
            val year = if (isMovie) movie?.year else show?.year

            val baseId = rawTmdb?.toString() ?: rawImdb ?: continue
            remoteCompletedMediaIds.add(baseId)
            rawImdb?.let { remoteCompletedMediaIds.add(it) }

            val uniqueHistKey = if (!isMovie && episode != null) {
                "${baseId}_s${episode.season}_e${episode.number}"
            } else {
                baseId
            }
            if (!seenRemoteKeys.add(uniqueHistKey)) continue

            val shouldHydrateRemote = hydratedCount < 80
            val hydrated = hydrateMediaMeta(
                rawMediaId = baseId,
                tmdbId = rawTmdb,
                imdbId = rawImdb,
                title = title,
                year = year,
                isTv = !isMovie,
                fetchRemoteIfUncached = shouldHydrateRemote
            )
            if (shouldHydrateRemote) hydratedCount++

            val mediaId = hydrated.mediaId
            remoteCompletedMediaIds.add(mediaId)

            // Mark completed in watchlist if present
            if (watchlistDao.countWatchlist(mediaId, profileId) > 0) {
                watchlistDao.updateStatus(mediaId, "COMPLETED", profileId)
            }

            // Ensure completed entry exists in playback_history so History & Completed filters work after reset
            val seasonNum = if (!isMovie) episode?.season else null
            val epNum = if (!isMovie) episode?.number else null
            val existingHist = allLocalHistory.firstOrNull {
                (it.mediaId == mediaId || it.mediaId == baseId) &&
                    it.seasonNumber == seasonNum && it.episodeNumber == epNum
            }

            // Never insert new ghost history rows with null posters if unhydrated
            val resolvedPoster = existingHist?.posterUrl?.takeIf { it.isNotBlank() } ?: hydrated.posterUrl
            val resolvedBackdrop = existingHist?.backdropUrl?.takeIf { it.isNotBlank() } ?: hydrated.backdropUrl ?: resolvedPoster
            if (existingHist == null && resolvedPoster.isNullOrBlank()) continue

            if (existingHist == null || !existingHist.isCompleted || existingHist.posterUrl.isNullOrBlank()) {
                val watchedEpoch = parseIsoTimestamp(item.watched_at) ?: System.currentTimeMillis()
                val durationMs = existingHist?.durationMs?.takeIf { it > 60_000L }
                    ?: hydrated.durationMs?.takeIf { it > 60_000L }
                    ?: if (isMovie) 6_000_000L else 2_700_000L
                val provider = existingHist?.provider ?: "tmdb"
                val entityId = existingHist?.id ?: PlaybackHistoryEntity.buildId(provider, mediaId, seasonNum, epNum, profileId)

                playbackHistoryDao.upsert(
                    PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = existingHist?.mediaId ?: mediaId,
                        title = hydrated.title.ifBlank { title },
                        episodeTitle = episode?.title ?: existingHist?.episodeTitle,
                        seasonNumber = seasonNum,
                        episodeNumber = epNum,
                        posterUrl = resolvedPoster,
                        backdropUrl = resolvedBackdrop,
                        currentPositionMs = durationMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = existingHist?.lastWatchedTimestamp ?: watchedEpoch,
                        isCompleted = true,
                        provider = provider,
                        mediaType = if (isMovie) "MOVIE" else "TV_SERIES",
                        episodeData = existingHist?.episodeData,
                        isDismissedFromContinueWatching = true,
                        profileId = profileId
                    )
                )
            }
        }

        // 2. Local -> Remote: Push locally completed items to Trakt History
        val unsyncedMovies = mutableListOf<TraktMovie>()
        val unsyncedShows = mutableListOf<TraktShow>()
        val pushedLocalIds = mutableSetOf<String>()

        val completedLocalHistory = allLocalHistory.filter { it.isCompleted }
        for (local in completedLocalHistory) {
            if (local.mediaId in remoteCompletedMediaIds || !pushedLocalIds.add(local.mediaId)) continue
            val isTv = local.seasonNumber != null || local.episodeNumber != null ||
                local.mediaType?.contains("TV", ignoreCase = true) == true ||
                local.mediaType?.contains("ANIME", ignoreCase = true) == true
            val tmdbId = local.mediaId.toLongOrNull()
            val imdbId = local.mediaId.takeIf { it.startsWith("tt") }
            val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
            if (isTv) {
                unsyncedShows.add(TraktShow(title = local.title, ids = ids))
            } else {
                unsyncedMovies.add(TraktMovie(title = local.title, ids = ids))
            }
        }

        val completedWatchlist = localWatchlist.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        for (local in completedWatchlist) {
            if (local.mediaId in remoteCompletedMediaIds || !pushedLocalIds.add(local.mediaId)) continue
            val isMovie = local.mediaType.equals("MOVIE", ignoreCase = true)
            val tmdbId = local.mediaId.toLongOrNull()
            val imdbId = local.mediaId.takeIf { it.startsWith("tt") }
            val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
            if (isMovie) {
                unsyncedMovies.add(TraktMovie(title = local.title, year = local.year, ids = ids))
            } else {
                unsyncedShows.add(TraktShow(title = local.title, year = local.year, ids = ids))
            }
        }

        if (unsyncedMovies.isNotEmpty() || unsyncedShows.isNotEmpty()) {
            traktClient.addToHistory(token, TraktSyncItemsPayload(movies = unsyncedMovies, shows = unsyncedShows))
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                     SIMKL WATCH HISTORY (TWO-WAY COMPLETED)                 */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncSimklHistoryInternal(token: String): String? {
        val profileId = currentProfileId()
        val result = simklClient.getAllItems(token, "completed")
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Failed to fetch Simkl history"
            Log.w(TAG, msg)
            return msg
        }

        val parsed = result.getOrNull() ?: return null
        val allLocalHistory = playbackHistoryDao.getAllHistory().filter { it.profileId == profileId }
        val localWatchlist = watchlistDao.getAllWatchlist().filter { it.profileId == profileId }
        val remoteCompletedIds = mutableSetOf<String>()

        var hydratedCount = 0

        for (m in parsed.movies) {
            val movie = m.movie ?: continue
            val baseId = movie.ids?.tmdb?.toString() ?: movie.ids?.imdb ?: continue
            remoteCompletedIds.add(baseId)

            val shouldHydrate = hydratedCount < 80
            val hydrated = hydrateMediaMeta(
                rawMediaId = baseId,
                tmdbId = movie.ids?.tmdb,
                imdbId = movie.ids?.imdb,
                title = movie.title,
                year = movie.year,
                isTv = false,
                fetchRemoteIfUncached = shouldHydrate
            )
            if (shouldHydrate) hydratedCount++
            remoteCompletedIds.add(hydrated.mediaId)

            if (watchlistDao.countWatchlist(hydrated.mediaId, profileId) > 0) {
                watchlistDao.updateStatus(hydrated.mediaId, "COMPLETED", profileId)
            }

            val existingHist = allLocalHistory.firstOrNull { it.mediaId == hydrated.mediaId || it.mediaId == baseId }
            val resolvedPoster = existingHist?.posterUrl?.takeIf { it.isNotBlank() } ?: hydrated.posterUrl
            val resolvedBackdrop = existingHist?.backdropUrl?.takeIf { it.isNotBlank() } ?: hydrated.backdropUrl ?: resolvedPoster
            if (existingHist == null && resolvedPoster.isNullOrBlank()) continue

            if (existingHist == null || !existingHist.isCompleted || existingHist.posterUrl.isNullOrBlank()) {
                val durationMs = existingHist?.durationMs?.takeIf { it > 60_000L } ?: hydrated.durationMs ?: 6_000_000L
                val entityId = existingHist?.id ?: PlaybackHistoryEntity.buildId("tmdb", hydrated.mediaId, null, null, profileId)
                playbackHistoryDao.upsert(
                    PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = hydrated.mediaId,
                        title = hydrated.title,
                        episodeTitle = null,
                        seasonNumber = null,
                        episodeNumber = null,
                        posterUrl = resolvedPoster,
                        backdropUrl = resolvedBackdrop,
                        currentPositionMs = durationMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = existingHist?.lastWatchedTimestamp ?: System.currentTimeMillis(),
                        isCompleted = true,
                        provider = existingHist?.provider ?: "tmdb",
                        mediaType = "MOVIE",
                        isDismissedFromContinueWatching = true,
                        profileId = profileId
                    )
                )
            }
        }

        val allShows = parsed.shows.mapNotNull { it.show } + parsed.anime.mapNotNull { it.anime }
        for (show in allShows) {
            val baseId = show.ids?.tmdb?.toString() ?: show.ids?.imdb ?: continue
            remoteCompletedIds.add(baseId)

            val shouldHydrate = hydratedCount < 80
            val hydrated = hydrateMediaMeta(
                rawMediaId = baseId,
                tmdbId = show.ids?.tmdb,
                imdbId = show.ids?.imdb,
                title = show.title,
                year = show.year,
                isTv = true,
                fetchRemoteIfUncached = shouldHydrate
            )
            if (shouldHydrate) hydratedCount++
            remoteCompletedIds.add(hydrated.mediaId)

            if (watchlistDao.countWatchlist(hydrated.mediaId, profileId) > 0) {
                watchlistDao.updateStatus(hydrated.mediaId, "COMPLETED", profileId)
            }

            val existingHist = allLocalHistory.firstOrNull { it.mediaId == hydrated.mediaId || it.mediaId == baseId }
            val resolvedPoster = existingHist?.posterUrl?.takeIf { it.isNotBlank() } ?: hydrated.posterUrl
            val resolvedBackdrop = existingHist?.backdropUrl?.takeIf { it.isNotBlank() } ?: hydrated.backdropUrl ?: resolvedPoster
            if (existingHist == null && resolvedPoster.isNullOrBlank()) continue

            if (existingHist == null || !existingHist.isCompleted || existingHist.posterUrl.isNullOrBlank()) {
                val durationMs = existingHist?.durationMs?.takeIf { it > 60_000L } ?: hydrated.durationMs ?: 2_700_000L
                val entityId = existingHist?.id ?: PlaybackHistoryEntity.buildId("tmdb", hydrated.mediaId, 1, 1, profileId)
                playbackHistoryDao.upsert(
                    PlaybackHistoryEntity(
                        id = entityId,
                        mediaId = hydrated.mediaId,
                        title = hydrated.title,
                        episodeTitle = existingHist?.episodeTitle,
                        seasonNumber = existingHist?.seasonNumber ?: 1,
                        episodeNumber = existingHist?.episodeNumber ?: 1,
                        posterUrl = resolvedPoster,
                        backdropUrl = resolvedBackdrop,
                        currentPositionMs = durationMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = existingHist?.lastWatchedTimestamp ?: System.currentTimeMillis(),
                        isCompleted = true,
                        provider = existingHist?.provider ?: "tmdb",
                        mediaType = "TV_SERIES",
                        isDismissedFromContinueWatching = true,
                        profileId = profileId
                    )
                )
            }
        }

        // 2. Local -> Remote: Push locally completed items to Simkl
        val pushMovies = mutableListOf<SimklSyncItem>()
        val pushShows = mutableListOf<SimklSyncItem>()
        val pushedIds = mutableSetOf<String>()

        for (local in allLocalHistory.filter { it.isCompleted }) {
            if (local.mediaId in remoteCompletedIds || !pushedIds.add(local.mediaId)) continue
            val isTv = local.seasonNumber != null || local.episodeNumber != null ||
                local.mediaType?.contains("TV", ignoreCase = true) == true ||
                local.mediaType?.contains("ANIME", ignoreCase = true) == true
            val ids = SimklIds(tmdb = local.mediaId.toLongOrNull(), imdb = local.mediaId.takeIf { it.startsWith("tt") })
            val syncItem = SimklSyncItem(title = local.title, to = "completed", ids = ids)
            if (isTv) pushShows.add(syncItem) else pushMovies.add(syncItem)
        }

        for (local in localWatchlist.filter { it.status.equals("COMPLETED", ignoreCase = true) }) {
            if (local.mediaId in remoteCompletedIds || !pushedIds.add(local.mediaId)) continue
            val isMovie = local.mediaType.equals("MOVIE", ignoreCase = true)
            val ids = SimklIds(tmdb = local.mediaId.toLongOrNull(), imdb = local.mediaId.takeIf { it.startsWith("tt") })
            val syncItem = SimklSyncItem(title = local.title, to = "completed", ids = ids)
            if (isMovie) pushMovies.add(syncItem) else pushShows.add(syncItem)
        }

        if (pushMovies.isNotEmpty() || pushShows.isNotEmpty()) {
            simklClient.addToLibrary(token, SimklSyncPayload(movies = pushMovies.take(50), shows = pushShows.take(50)))
        }
        return null
    }

    /* -------------------------------------------------------------------------- */
    /*                  TRAKT CUSTOM LISTS & PLAYLISTS (TWO-WAY)                   */
    /* -------------------------------------------------------------------------- */

    private suspend fun syncTraktListsInternal(token: String): String? {
        val profileId = currentProfileId()
        val localPlaylists = playlistRepository.awaitLoaded()
        val listsResult = traktClient.getUserLists(token)
        if (listsResult.isFailure) {
            val msg = listsResult.exceptionOrNull()?.message ?: "Failed to fetch Trakt custom lists"
            Log.w(TAG, msg)
            return msg
        }

        val remoteLists = (listsResult.getOrNull() ?: emptyList()).toMutableList()

        // 1. Remote -> Local: Pull Trakt custom lists into PlaylistRepository with TMDB hydration
        for (list in remoteLists) {
            if (list.ids.slug.isBlank()) continue
            val itemsResult = traktClient.getListItems(token, list.ids.slug)
            if (itemsResult.isFailure) continue

            val items = itemsResult.getOrNull() ?: emptyList()
            val playlistItems = items.mapNotNull { traktItem ->
                val isMovie = traktItem.type.equals("movie", ignoreCase = true)
                val movie = traktItem.movie
                val show = traktItem.show

                val tmdbId = movie?.ids?.tmdb ?: show?.ids?.tmdb
                val imdbId = movie?.ids?.imdb ?: show?.ids?.imdb
                val title = movie?.title ?: show?.title ?: return@mapNotNull null
                val year = movie?.year ?: show?.year

                val hydrated = hydrateMediaMeta(
                    rawMediaId = tmdbId?.toString() ?: imdbId,
                    tmdbId = tmdbId,
                    imdbId = imdbId,
                    title = title,
                    year = year,
                    isTv = !isMovie
                )

                PlaylistItem(
                    mediaId = hydrated.mediaId,
                    title = hydrated.title,
                    posterUrl = hydrated.posterUrl,
                    backdropUrl = hydrated.backdropUrl,
                    isTvSeries = !isMovie,
                    year = hydrated.year,
                    provider = "tmdb"
                )
            }

            if (playlistItems.isNotEmpty()) {
                // If this is the "Favorites" list on Trakt, restore favoriteMediaIds as well
                if (list.name.equals("Favorites", ignoreCase = true)) {
                    val currentFavs = preferencesRepository.favoriteMediaIds.first()
                    for (pi in playlistItems) {
                        if (pi.mediaId !in currentFavs) {
                            preferencesRepository.toggleFavorite(pi.mediaId)
                        }
                        if (watchlistDao.countWatchlist(pi.mediaId, profileId) == 0) {
                            watchlistDao.insert(
                                WatchlistEntity(
                                    mediaId = pi.mediaId,
                                    profileId = profileId,
                                    title = pi.title,
                                    posterUrl = pi.posterUrl,
                                    backdropUrl = pi.backdropUrl,
                                    mediaType = if (pi.isTvSeries) "TV_SERIES" else "MOVIE",
                                    rating = null,
                                    year = pi.year,
                                    status = "TO_BE_WATCHED"
                                )
                            )
                        }
                    }
                }

                val currentPlaylists = playlistRepository.userPlaylists.value
                val existing = currentPlaylists.find { it.title.equals(list.name, ignoreCase = true) }
                if (existing == null) {
                    playlistRepository.createPlaylist(list.name, playlistItems, syncToRemote = false)
                } else {
                    for (pi in playlistItems) {
                        playlistRepository.addItemToPlaylist(existing.id, pi, syncToRemote = false)
                    }
                }
            }
        }

        // 2. Local -> Remote: Push local custom playlists (and Favorites) to Trakt custom lists!
        val updatedLocalPlaylists = playlistRepository.userPlaylists.value.toMutableList()

        // Also build a virtual playlist for Favorites if the user has local favorites
        val favoriteIds = preferencesRepository.favoriteMediaIds.first()
        if (favoriteIds.isNotEmpty() && updatedLocalPlaylists.none { it.title.equals("Favorites", ignoreCase = true) }) {
            val localWatchlist = watchlistDao.getAllWatchlist().filter { it.profileId == profileId }
            val favPlaylistItems = localWatchlist.filter { it.mediaId in favoriteIds }.map { w ->
                PlaylistItem(
                    mediaId = w.mediaId,
                    title = w.title,
                    posterUrl = w.posterUrl,
                    backdropUrl = w.backdropUrl,
                    isTvSeries = !w.mediaType.equals("MOVIE", ignoreCase = true),
                    year = w.year
                )
            }
            if (favPlaylistItems.isNotEmpty()) {
                updatedLocalPlaylists.add(
                    com.euthopiar.data.repository.Playlist(
                        id = "virtual_favorites",
                        title = "Favorites",
                        items = favPlaylistItems
                    )
                )
            }
        }

        for (localPl in updatedLocalPlaylists) {
            if (localPl.items.isEmpty() || localPl.isSystem) continue

            var remoteList = remoteLists.find { it.name.equals(localPl.title, ignoreCase = true) }
            if (remoteList == null) {
                remoteList = createTraktUserList(token, localPl.title)
                if (remoteList != null) {
                    remoteLists.add(remoteList)
                }
            }

            val slug = remoteList?.ids?.slug?.takeIf { it.isNotBlank() } ?: continue
            val existingRemoteItems = traktClient.getListItems(token, slug).getOrNull() ?: emptyList()
            val remoteTmdbIds = existingRemoteItems.mapNotNull { it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb }.map { it.toString() }.toSet()
            val remoteImdbIds = existingRemoteItems.mapNotNull { it.movie?.ids?.imdb ?: it.show?.ids?.imdb }.toSet()

            val moviesToPush = mutableListOf<TraktMovie>()
            val showsToPush = mutableListOf<TraktShow>()

            for (item in localPl.items) {
                val tmdbId = item.mediaId.toLongOrNull()
                val imdbId = item.mediaId.takeIf { it.startsWith("tt") }
                val alreadyInRemote = (tmdbId != null && remoteTmdbIds.contains(tmdbId.toString())) ||
                    (imdbId != null && remoteImdbIds.contains(imdbId))

                if (!alreadyInRemote) {
                    val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                    if (item.isTvSeries) {
                        showsToPush.add(TraktShow(title = item.title, year = item.year, ids = ids))
                    } else {
                        moviesToPush.add(TraktMovie(title = item.title, year = item.year, ids = ids))
                    }
                }
            }

            if (moviesToPush.isNotEmpty() || showsToPush.isNotEmpty()) {
                addItemsToTraktUserList(
                    token = token,
                    listSlug = slug,
                    payload = TraktSyncItemsPayload(movies = moviesToPush, shows = showsToPush)
                )
            }
        }
        return null
    }

    /**
     * Creates a new personal custom list on Trakt.tv (`POST /users/me/lists`).
     */
    private suspend fun createTraktUserList(token: String, name: String): TraktCustomList? = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("name", name)
                put("description", "Synced from Euthopiar")
                put("privacy", "private")
                put("display_numbers", false)
                put("allow_comments", false)
            }.toString()

            val req = Request.Builder()
                .url("${TraktClient.BASE_URL}/users/me/lists")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", TraktClient.DEFAULT_CLIENT_ID)
                .header("Authorization", "Bearer $token")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 201) {
                    val raw = resp.body?.string().orEmpty()
                    json.decodeFromString<TraktCustomList>(raw)
                } else {
                    Log.w(TAG, "Failed to create Trakt list '$name': HTTP ${resp.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error creating Trakt list '$name'", e)
            null
        }
    }

    /**
     * Adds movies/shows to a Trakt custom list (`POST /users/me/lists/{id}/items`).
     */
    private suspend fun addItemsToTraktUserList(
        token: String,
        listSlug: String,
        payload: TraktSyncItemsPayload
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${TraktClient.BASE_URL}/users/me/lists/$listSlug/items")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", TraktClient.DEFAULT_CLIENT_ID)
                .header("Authorization", "Bearer $token")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful || resp.code == 201
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pushing items to Trakt list '$listSlug'", e)
            false
        }
    }

    private fun parseIsoTimestamp(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    /* -------------------------------------------------------------------------- */
    /*                         REAL-TIME EVENT DISPATCH HOOKS                     */
    /* -------------------------------------------------------------------------- */

    /**
     * Dispatches an asynchronous push when custom playlists are created or updated locally.
     */
    fun notifyPlaylistsChanged() {
        scope.launch {
            try {
                val traktToken = preferencesRepository.traktAccessToken.first()
                val traktListsEnabled = preferencesRepository.traktSyncLists.first()
                if (traktToken.isNotBlank() && traktListsEnabled) {
                    val validToken = ensureValidTraktToken(traktToken)
                    if (validToken.isNotBlank()) {
                        syncTraktListsInternal(validToken)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed async playlist sync", e)
            }
        }
    }

    /**
     * Dispatches an asynchronous push when an item is added to the watchlist locally.
     */
    fun notifyWatchlistAdded(item: MediaItem) {
        scope.launch {
            try {
                val isTv = item.type.name.contains("TV", ignoreCase = true) || item.type.name.contains("ANIME", ignoreCase = true)
                val cleanId = item.id.trim().removePrefix("tmdb:").removePrefix("tmdb/")
                val tmdbId = cleanId.toLongOrNull()
                val imdbId = if (cleanId.startsWith("tt")) cleanId else null

                val traktToken = preferencesRepository.traktAccessToken.first()
                val traktEnabled = preferencesRepository.traktSyncWatchlist.first()
                if (traktToken.isNotBlank() && traktEnabled) {
                    val validToken = ensureValidTraktToken(traktToken)
                    val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                    val payload = if (isTv) {
                        TraktSyncItemsPayload(shows = listOf(TraktShow(title = item.title, year = item.year, ids = ids)))
                    } else {
                        TraktSyncItemsPayload(movies = listOf(TraktMovie(title = item.title, year = item.year, ids = ids)))
                    }
                    traktClient.addToWatchlist(validToken, payload)
                }

                val simklToken = preferencesRepository.simklAccessToken.first()
                val simklEnabled = preferencesRepository.simklSyncWatchlist.first()
                if (simklToken.isNotBlank() && simklEnabled) {
                    val syncItem = SimklSyncItem(title = item.title, to = "plantowatch", ids = SimklIds(tmdb = tmdbId, imdb = imdbId))
                    val payload = if (isTv) {
                        SimklSyncPayload(shows = listOf(syncItem))
                    } else {
                        SimklSyncPayload(movies = listOf(syncItem))
                    }
                    simklClient.addToLibrary(simklToken, payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed async watchlist push", e)
            }
        }
    }

    /**
     * Dispatches an asynchronous push when a MediaDetail item is added to the watchlist.
     */
    fun notifyWatchlistAdded(detail: MediaDetail) {
        scope.launch {
            try {
                val isTv = detail.type.name.contains("TV", ignoreCase = true) || detail.type.name.contains("ANIME", ignoreCase = true)
                val cleanId = detail.id.trim().removePrefix("tmdb:").removePrefix("tmdb/")
                val tmdbId = cleanId.toLongOrNull()
                val imdbId = detail.imdbId ?: if (cleanId.startsWith("tt")) cleanId else null

                val traktToken = preferencesRepository.traktAccessToken.first()
                val traktEnabled = preferencesRepository.traktSyncWatchlist.first()
                if (traktToken.isNotBlank() && traktEnabled) {
                    val validToken = ensureValidTraktToken(traktToken)
                    val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                    val payload = if (isTv) {
                        TraktSyncItemsPayload(shows = listOf(TraktShow(title = detail.title, year = detail.year, ids = ids)))
                    } else {
                        TraktSyncItemsPayload(movies = listOf(TraktMovie(title = detail.title, year = detail.year, ids = ids)))
                    }
                    traktClient.addToWatchlist(validToken, payload)
                }

                val simklToken = preferencesRepository.simklAccessToken.first()
                val simklEnabled = preferencesRepository.simklSyncWatchlist.first()
                if (simklToken.isNotBlank() && simklEnabled) {
                    val syncItem = SimklSyncItem(title = detail.title, to = "plantowatch", ids = SimklIds(tmdb = tmdbId, imdb = imdbId))
                    val payload = if (isTv) {
                        SimklSyncPayload(shows = listOf(syncItem))
                    } else {
                        SimklSyncPayload(movies = listOf(syncItem))
                    }
                    simklClient.addToLibrary(simklToken, payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed async watchlist detail push", e)
            }
        }
    }

    /**
     * Dispatches an asynchronous remove from Trakt & Simkl when un-bookmarked.
     */
    fun notifyWatchlistRemoved(mediaId: String, title: String? = null, isTv: Boolean = false) {
        scope.launch {
            try {
                val cleanId = mediaId.trim().removePrefix("tmdb:").removePrefix("tmdb/")
                val tmdbId = cleanId.toLongOrNull()
                val imdbId = if (cleanId.startsWith("tt")) cleanId else null

                val traktToken = preferencesRepository.traktAccessToken.first()
                val traktEnabled = preferencesRepository.traktSyncWatchlist.first()
                if (traktToken.isNotBlank() && traktEnabled) {
                    val validToken = ensureValidTraktToken(traktToken)
                    val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                    val payload = if (isTv) {
                        TraktSyncItemsPayload(shows = listOf(TraktShow(title = title ?: "", ids = ids)))
                    } else {
                        TraktSyncItemsPayload(movies = listOf(TraktMovie(title = title ?: "", ids = ids)))
                    }
                    traktClient.removeFromWatchlist(validToken, payload)
                }

                val simklToken = preferencesRepository.simklAccessToken.first()
                val simklEnabled = preferencesRepository.simklSyncWatchlist.first()
                if (simklToken.isNotBlank() && simklEnabled) {
                    val item = SimklSyncItem(title = title, ids = SimklIds(tmdb = tmdbId, imdb = imdbId))
                    val payload = if (isTv) {
                        SimklSyncPayload(shows = listOf(item))
                    } else {
                        SimklSyncPayload(movies = listOf(item))
                    }
                    simklClient.removeFromLibrary(simklToken, payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed async watchlist remove", e)
            }
        }
    }

    /**
     * Dispatches an asynchronous push when media playback is completed.
     */
    fun notifyMediaCompleted(mediaId: String, title: String, isTv: Boolean, season: Int? = null, episode: Int? = null) {
        scope.launch {
            try {
                val cleanId = mediaId.trim().removePrefix("tmdb:").removePrefix("tmdb/")
                val tmdbId = cleanId.toLongOrNull()
                val imdbId = if (cleanId.startsWith("tt")) cleanId else null

                val traktToken = preferencesRepository.traktAccessToken.first()
                val traktEnabled = preferencesRepository.traktSyncHistory.first()
                if (traktToken.isNotBlank() && traktEnabled) {
                    val validToken = ensureValidTraktToken(traktToken)
                    val ids = TraktIds(tmdb = tmdbId, imdb = imdbId)
                    val payload = if (isTv) {
                        TraktSyncItemsPayload(shows = listOf(TraktShow(title = title, ids = ids)))
                    } else {
                        TraktSyncItemsPayload(movies = listOf(TraktMovie(title = title, ids = ids)))
                    }
                    traktClient.addToHistory(validToken, payload)
                }

                val simklToken = preferencesRepository.simklAccessToken.first()
                val simklEnabled = preferencesRepository.simklSyncHistory.first()
                if (simklToken.isNotBlank() && simklEnabled) {
                    val ids = SimklIds(tmdb = tmdbId, imdb = imdbId)
                    val syncItem = SimklSyncItem(title = title, to = "completed", ids = ids)
                    val payload = if (isTv) {
                        SimklSyncPayload(shows = listOf(syncItem))
                    } else {
                        SimklSyncPayload(movies = listOf(syncItem))
                    }
                    simklClient.addToLibrary(simklToken, payload)
                    if (season != null && episode != null) {
                        simklClient.scrobble(
                            simklToken,
                            SimklScrobblePayload(
                                show = SimklScrobbleItem(title = title, ids = ids),
                                episode = SimklEpisodeItem(season = season, number = episode),
                                progress = 100f
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed async history push", e)
            }
        }
    }

    companion object {
        private const val TAG = "MediaSyncManager"

        @Volatile
        private var instance: MediaSyncManager? = null

        fun getInstance(context: Context): MediaSyncManager {
            return instance ?: synchronized(this) {
                instance ?: MediaSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
