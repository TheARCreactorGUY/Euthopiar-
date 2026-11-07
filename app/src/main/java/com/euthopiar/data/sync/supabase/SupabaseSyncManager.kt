package com.euthopiar.data.sync.supabase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.euthopiar.app.plugin.PluginRepositoryService
import com.euthopiar.core.BuildConfig
import com.euthopiar.core.sync.supabase.*
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.local.WatchlistEntity
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.repository.PlaylistRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.abs

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class SupabaseSyncManager private constructor(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val TAG = "SupabaseSyncManager"
    private val preferencesRepository = PreferencesRepository.getInstance(context)
    private val playlistRepository = PlaylistRepository.getInstance(context)
    private val profileManager = ProfileManager.getInstance(context)
    private val db = AppDatabase.getInstance(context)
    private val playbackHistoryDao = db.playbackHistoryDao()
    private val watchlistDao = db.watchlistDao()
    private val json = Json { ignoreUnknownKeys = true }

    val authClient = SupabaseAuthClient()

    private var currentSessionCache: SupabaseSession? = null
    private var scheduledSyncJob: Job? = null
    private val syncMutex = Mutex()
    private val isInitialRestoreInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    val tokenAuthenticator = SupabaseTokenAuthenticator(
        authClient = authClient,
        sessionProvider = { currentSessionCache },
        onSessionRefreshed = { refreshed ->
            currentSessionCache = refreshed
            scope.launch {
                preferencesRepository.setSupabaseSession(refreshed)
            }
        },
        onSessionExpired = {
            currentSessionCache = null
            scope.launch {
                preferencesRepository.clearSupabaseSession()
            }
        }
    )

    val restClient = SupabaseRestClient(
        tokenAuthenticator = tokenAuthenticator
    )

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    private val isSyncingActive = java.util.concurrent.atomic.AtomicBoolean(false)

    val sessionFlow: StateFlow<SupabaseSession?> = preferencesRepository.supabaseSession
        .stateIn(scope, SharingStarted.Eagerly, null)

    val lastSyncTimestamp: StateFlow<Long> = preferencesRepository.supabaseLastSyncTimestamp
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    init {
        scope.launch {
            sessionFlow.collect { session ->
                currentSessionCache = session
            }
        }
    }

    /**
     * Schedules a debounced background sync. Call whenever local data is added, deleted, or updated.
     */
    fun scheduleSync(delayMs: Long = 1000L) {
        scheduledSyncJob?.cancel()
        scheduledSyncJob = scope.launch {
            delay(delayMs)
            // Wait if an initial sign-in restore is currently pulling cloud backup
            while (isInitialRestoreInProgress.get()) {
                delay(250L)
            }
            val session = currentSessionCache ?: preferencesRepository.supabaseSession.first() ?: return@launch
            currentSessionCache = session
            syncAll(forceFull = false)
        }
    }

    /**
     * Authenticates with Google One-Tap / Credential Manager and exchanges ID token with Supabase.
     * Enforces a strict "Restore-First, Push-Second" sequence so signing into a fresh app
     * never overwrites existing cloud backups.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (serverClientId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Google Web Client ID is not configured."))
            }

            val rawNonce = generateRawNonce()
            val hashedNonce = sha256(rawNonce)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(activity)
            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = activity
                )
            } catch (e: GetCredentialCancellationException) {
                return@withContext Result.failure(Exception("Sign in cancelled by user"))
            } catch (e: GetCredentialException) {
                return@withContext Result.failure(Exception("Credential Manager error: ${e.message}"))
            }

            val credential = result.credential
            val idToken = when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                }
                else -> {
                    return@withContext Result.failure(IllegalStateException("Unrecognized credential format returned."))
                }
            }

            // Read previous synced user ID BEFORE setSupabaseSession overwrites it
            val lastUserId = preferencesRepository.supabaseLastSyncedUserId.first()

            val authResult = authClient.signInWithGoogleIdToken(idToken, rawNonce)
            if (authResult.isFailure) {
                return@withContext Result.failure(authResult.exceptionOrNull() ?: Exception("Supabase Auth failed"))
            }

            val session = authResult.getOrThrow()
            isInitialRestoreInProgress.set(true)
            try {
                currentSessionCache = session
                preferencesRepository.setSupabaseSession(session)

                // Always reset watermarks on fresh sign-in so we pull the complete cloud state
                preferencesRepository.setSupabasePlaybackWatermark(null)
                preferencesRepository.setSupabaseWatchlistWatermark(null)
                preferencesRepository.setSupabasePlaylistsWatermark(null)
                preferencesRepository.setSupabaseLastSyncedUserId(session.userId)

                val account = CloudUserAccount(
                    userId = session.userId,
                    email = session.email,
                    displayName = session.displayName,
                    avatarUrl = session.avatarUrl,
                    activeProfileId = profileManager.activeProfileId.value
                )
                restClient.upsertAccount(account)

                val isSameOrGuestUser = (lastUserId == null || lastUserId == session.userId)
                triggerSafeMergeAndSync(
                    session = session,
                    mergePreExistingLocal = isSameOrGuestUser
                )
            } finally {
                isInitialRestoreInProgress.set(false)
            }

            Result.success(session)
        } catch (e: Exception) {
            isInitialRestoreInProgress.set(false)
            Log.e(TAG, "Sign in with Google failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduledSyncJob?.cancel()
            val session = currentSessionCache
            if (session != null) {
                authClient.signOut(session.accessToken)
            }
            currentSessionCache = null
            preferencesRepository.clearSupabaseSession()
            _syncStatus.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            currentSessionCache = null
            preferencesRepository.clearSupabaseSession()
            Result.failure(e)
        }
    }

    private suspend fun triggerSafeMergeAndSync(
        session: SupabaseSession,
        mergePreExistingLocal: Boolean
    ) {
        try {
            if (mergePreExistingLocal) {
                // Preserve any guest/local items created before signing in, using their existing timestamps
                // (or a slight fallback timestamp so newer cloud backups win if there is a conflict).
                val fallbackTs = System.currentTimeMillis() - 60_000L
                val localPlayback = playbackHistoryDao.getAllHistory()
                if (localPlayback.isNotEmpty()) {
                    val dirtyPlayback = localPlayback.map {
                        it.copy(
                            isDirty = true,
                            clientUpdatedAtMs = if (it.clientUpdatedAtMs > 0L) it.clientUpdatedAtMs else fallbackTs
                        )
                    }
                    playbackHistoryDao.upsertBatch(dirtyPlayback)
                }

                val localWatchlist = watchlistDao.getAllWatchlist()
                if (localWatchlist.isNotEmpty()) {
                    val dirtyWatchlist = localWatchlist.map {
                        it.copy(
                            isDirty = true,
                            clientUpdatedAtMs = if (it.clientUpdatedAtMs > 0L) it.clientUpdatedAtMs else fallbackTs
                        )
                    }
                    watchlistDao.insertBatch(dirtyWatchlist)
                }
            }

            // Execute full sync with restoreFirst=true so all cloud data is pulled BEFORE local dirty items are pushed
            syncAll(forceFull = true, isInitialSignInRestore = true)
        } catch (e: Exception) {
            Log.e(TAG, "Safe merge failed", e)
        }
    }

    suspend fun syncPlaybackDirty(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialRestoreInProgress.get()) return@withContext Result.success(Unit)
        val session = currentSessionCache ?: preferencesRepository.supabaseSession.first() ?: return@withContext Result.success(Unit)
        currentSessionCache = session
        val cloudSyncEnabled = preferencesRepository.supabaseCloudSyncEnabled.first()
        val syncPlaybackEnabled = preferencesRepository.supabaseSyncPlayback.first()
        if (!cloudSyncEnabled || !syncPlaybackEnabled) return@withContext Result.success(Unit)

        try {
            val dirtyRows = playbackHistoryDao.getDirtyRows()
            if (dirtyRows.isEmpty()) return@withContext Result.success(Unit)

            // Ensure profiles exist in cloud before pushing foreign-keyed playback rows
            syncProfilesUpstream(session)

            val now = System.currentTimeMillis()
            val cloudItems = dirtyRows.map { entity ->
                val cleanProvider = (entity.provider?.trim()?.ifBlank { null } ?: "default").lowercase()
                val cloudMediaId = encodeCloudPlaybackMediaId(entity.mediaId, entity.seasonNumber, entity.episodeNumber)
                CloudPlaybackItem(
                    userId = session.userId,
                    profileId = entity.profileId,
                    provider = cleanProvider,
                    mediaId = cloudMediaId,
                    title = entity.title,
                    episodeTitle = entity.episodeTitle,
                    seasonNumber = entity.seasonNumber,
                    episodeNumber = entity.episodeNumber,
                    posterUrl = entity.posterUrl,
                    backdropUrl = entity.backdropUrl,
                    currentPositionMs = entity.currentPositionMs,
                    durationMs = entity.durationMs,
                    isCompleted = entity.isCompleted,
                    isDismissed = entity.isDismissedFromContinueWatching,
                    isDeleted = entity.isDeleted,
                    mediaType = entity.mediaType ?: "MOVIE",
                    episodeData = entity.episodeData,
                    lastWatchedTimestamp = entity.clientUpdatedAtMs.takeIf { it > 0L } ?: entity.lastWatchedTimestamp
                )
            }

            val pushRes = restClient.upsertPlaybackBatch(cloudItems)
            if (pushRes.isSuccess) {
                val ids = dirtyRows.map { it.id }
                playbackHistoryDao.markClean(ids, now)
                Result.success(Unit)
            } else {
                Result.failure(pushRes.exceptionOrNull() ?: IOException("Push playback failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncProfilesDownstream(userId: String) {
        val pullRes = restClient.pullProfiles(userId)
        if (pullRes.isSuccess) {
            val remoteProfiles = pullRes.getOrThrow()
            if (remoteProfiles.isNotEmpty()) {
                profileManager.mergeRemoteProfiles(remoteProfiles)
            }
        }
    }

    private suspend fun syncProfilesUpstream(session: SupabaseSession) {
        val localProfiles = profileManager.profiles.value
        val now = System.currentTimeMillis()
        val cloudProfiles = if (localProfiles.isNotEmpty()) {
            localProfiles.map { prof ->
                val normId = if (prof.id == "primary_profile") "default_user" else prof.id
                CloudUserProfile(
                    profileId = normId,
                    userId = session.userId,
                    name = prof.name.ifBlank { if (normId == "default_user") (session.displayName ?: "Primary") else "Profile" },
                    profileType = prof.type.name,
                    avatarId = prof.avatarId.ifBlank { "avatar_1" },
                    customAvatarUrl = if (normId == "default_user") session.avatarUrl else null,
                    pinHash = prof.pinHash,
                    isDeleted = false,
                    clientUpdatedAtMs = now
                )
            }
        } else {
            listOf(
                CloudUserProfile(
                    profileId = "default_user",
                    userId = session.userId,
                    name = session.displayName ?: "Primary",
                    profileType = "ADMIN",
                    avatarId = "avatar_1",
                    customAvatarUrl = session.avatarUrl,
                    clientUpdatedAtMs = now
                )
            )
        }
        restClient.upsertProfilesBatch(cloudProfiles)
    }

    suspend fun syncAll(
        forceFull: Boolean = false,
        isInitialSignInRestore: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // If an initial sign-in restore is running and this is a routine background sync, let the initial restore handle it
        if (!isInitialSignInRestore && isInitialRestoreInProgress.get()) {
            return@withContext Result.success(Unit)
        }

        val session = currentSessionCache ?: preferencesRepository.supabaseSession.first() ?: return@withContext Result.success(Unit)
        currentSessionCache = session
        val cloudSyncEnabled = preferencesRepository.supabaseCloudSyncEnabled.first()
        if (!cloudSyncEnabled) return@withContext Result.success(Unit)

        // If a non-forced background sync arrives while syncMutex is already locked, skip duplicate work;
        // if forceFull or isInitialSignInRestore is true, wait for the mutex so forced syncs are never dropped!
        if (!forceFull && !isInitialSignInRestore && syncMutex.isLocked) {
            return@withContext Result.success(Unit)
        }

        syncMutex.withLock {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                _syncErrorMessage.value = null

                val now = System.currentTimeMillis()
                val userId = session.userId

                val syncPlayback = preferencesRepository.supabaseSyncPlayback.first()
                val syncWatchlist = preferencesRepository.supabaseSyncWatchlist.first()
                val syncPlaylists = preferencesRepository.supabaseSyncPlaylists.first()
                val syncSettings = preferencesRepository.supabaseSyncSettings.first()

                // =========================================================================
                // PHASE 1: PROFILE SYNCHRONIZATION (Prevents HTTP 409 Foreign Key Errors)
                // =========================================================================
                syncProfilesDownstream(userId)
                syncProfilesUpstream(session)

                // =========================================================================
                // PHASE 2: DOWNSTREAM PULL FIRST (Guarantees Cloud Backups Are Never Clobbered)
                // =========================================================================

                // 1. Pull Playback History Delta
                if (syncPlayback) {
                    val playbackWatermark = if (forceFull) null else preferencesRepository.supabasePlaybackWatermark.first()
                    val pullPlaybackRes = restClient.pullPlaybackDelta(userId, playbackWatermark)
                    if (pullPlaybackRes.isSuccess) {
                        val remoteItems = pullPlaybackRes.getOrThrow()
                        Log.i(TAG, "Pulled ${remoteItems.size} playback items from cloud (watermark: $playbackWatermark)")
                        var maxWatermark = playbackWatermark
                        for (remote in remoteItems) {
                            val (cleanMediaId, parsedSeason, parsedEpisode) = decodeCloudPlaybackMediaId(
                                remote.mediaId,
                                remote.seasonNumber,
                                remote.episodeNumber
                            )
                            val cleanProvider = remote.provider.trim().ifBlank { "default" }.lowercase()
                            val localId = PlaybackHistoryEntity.buildId(
                                cleanProvider,
                                cleanMediaId,
                                parsedSeason,
                                parsedEpisode,
                                remote.profileId
                            )
                            val local = playbackHistoryDao.getProgress(localId) ?: playbackHistoryDao.getRawById(localId)
                            val shouldUpdate = local == null ||
                                (!local.isDirty) ||
                                remote.lastWatchedTimestamp >= local.clientUpdatedAtMs ||
                                (abs(remote.lastWatchedTimestamp - local.clientUpdatedAtMs) < 60_000L && remote.currentPositionMs > local.currentPositionMs)

                            if (shouldUpdate) {
                                val merged = PlaybackHistoryEntity(
                                    id = localId,
                                    mediaId = cleanMediaId,
                                    profileId = remote.profileId,
                                    title = remote.title,
                                    posterUrl = remote.posterUrl ?: local?.posterUrl,
                                    backdropUrl = remote.backdropUrl ?: local?.backdropUrl,
                                    mediaType = remote.mediaType,
                                    seasonNumber = parsedSeason,
                                    episodeNumber = parsedEpisode,
                                    episodeTitle = remote.episodeTitle ?: local?.episodeTitle,
                                    durationMs = maxOf(remote.durationMs, local?.durationMs ?: 0L),
                                    currentPositionMs = remote.currentPositionMs,
                                    isCompleted = remote.isCompleted || (local?.isCompleted == true),
                                    lastWatchedTimestamp = remote.lastWatchedTimestamp,
                                    provider = cleanProvider,
                                    episodeData = remote.episodeData ?: local?.episodeData,
                                    isDismissedFromContinueWatching = remote.isDismissed,
                                    isDirty = false,
                                    isDeleted = remote.isDeleted,
                                    clientUpdatedAtMs = remote.lastWatchedTimestamp
                                )
                                playbackHistoryDao.upsert(merged)
                            }

                            val serverUpdatedAt = remote.updatedAt
                            if (serverUpdatedAt != null) {
                                if (maxWatermark == null || serverUpdatedAt > maxWatermark) {
                                    maxWatermark = serverUpdatedAt
                                }
                            }
                        }
                        if (maxWatermark != null) {
                            preferencesRepository.setSupabasePlaybackWatermark(maxWatermark)
                        }
                    }
                }

                // 2. Pull Watchlist Delta
                if (syncWatchlist) {
                    val watchlistWatermark = if (forceFull) null else preferencesRepository.supabaseWatchlistWatermark.first()
                    val pullWatchlistRes = restClient.pullWatchlistDelta(userId, watchlistWatermark)
                    if (pullWatchlistRes.isSuccess) {
                        val remoteWatchlist = pullWatchlistRes.getOrThrow()
                        Log.i(TAG, "Pulled ${remoteWatchlist.size} watchlist items from cloud (watermark: $watchlistWatermark)")
                        var maxWatermark = watchlistWatermark
                        for (remote in remoteWatchlist) {
                            val local = watchlistDao.getWatchlistItem(remote.mediaId, remote.profileId) ?: watchlistDao.getRawItem(remote.mediaId, remote.profileId)
                            val shouldUpdate = local == null || (!local.isDirty) || remote.clientUpdatedAtMs >= local.clientUpdatedAtMs

                            if (shouldUpdate) {
                                val merged = WatchlistEntity(
                                    mediaId = remote.mediaId,
                                    profileId = remote.profileId,
                                    title = remote.title,
                                    posterUrl = remote.posterUrl ?: local?.posterUrl,
                                    backdropUrl = remote.backdropUrl ?: local?.backdropUrl,
                                    mediaType = remote.mediaType,
                                    rating = remote.rating ?: local?.rating,
                                    year = remote.releaseYear ?: local?.year,
                                    status = remote.status,
                                    addedTimestamp = remote.addedTimestamp,
                                    isDirty = false,
                                    isDeleted = remote.isDeleted,
                                    clientUpdatedAtMs = remote.clientUpdatedAtMs
                                )
                                watchlistDao.insert(merged)
                            }

                            val serverUpdatedAt = remote.updatedAt
                            if (serverUpdatedAt != null) {
                                if (maxWatermark == null || serverUpdatedAt > maxWatermark) {
                                    maxWatermark = serverUpdatedAt
                                }
                            }
                        }
                        if (maxWatermark != null) {
                            preferencesRepository.setSupabaseWatchlistWatermark(maxWatermark)
                        }
                    }
                }

                // 3. Pull Playlists Delta
                if (syncPlaylists) {
                    val playlistsWatermark = if (forceFull) null else preferencesRepository.supabasePlaylistsWatermark.first()
                    val pullPlaylistsRes = restClient.pullPlaylistsDelta(userId, playlistsWatermark)
                    if (pullPlaylistsRes.isSuccess) {
                        val remotePlaylists = pullPlaylistsRes.getOrThrow()
                        if (remotePlaylists.isNotEmpty()) {
                            playlistRepository.mergeRemotePlaylists(remotePlaylists)
                            val maxWatermark = remotePlaylists.mapNotNull { it.updatedAt }.maxOrNull()
                            if (maxWatermark != null) {
                                preferencesRepository.setSupabasePlaylistsWatermark(maxWatermark)
                            }
                        }
                    }
                }

                // 4. Pull & Reconcile User Settings (Cloud wins on initial sign-in restore!)
                if (syncSettings) {
                    val pullSettingsRes = restClient.pullUserSettings(userId)
                    val cloudSettings = pullSettingsRes.getOrNull()
                    val remoteEntries = cloudSettings?.settingsJson ?: emptyMap()

                    if (isInitialSignInRestore && remoteEntries.isNotEmpty()) {
                        // On fresh sign-in / restore, apply cloud settings first so fresh install defaults don't clobber cloud backup
                        val remoteValuesMap = remoteEntries.mapValues { it.value.value }
                        preferencesRepository.applySyncableSettings(remoteValuesMap)
                        installRestoredPluginReposIfNeeded(remoteValuesMap["repository_urls"])
                    }

                    // Push local settings (after applying remote if initial restore, or with newer timestamp on normal sync)
                    val localSettings = preferencesRepository.getAllSyncableSettings()
                    if (localSettings.isNotEmpty()) {
                        if (isInitialSignInRestore && remoteEntries.isNotEmpty()) {
                            // Only push keys that were not present in remote backup or merged collections (like favorite_media_ids)
                            val mergedToPush = localSettings.mapValues { (key, value) ->
                                val existingRemote = remoteEntries[key]
                                if (existingRemote != null && key != "favorite_media_ids" && key != "repository_urls") {
                                    existingRemote
                                } else {
                                    CloudSettingEntry(value = value, ts = now)
                                }
                            }
                            restClient.mergeUserSettingsEntries(mergedToPush)
                        } else {
                            restClient.mergeUserSettings(localSettings, now)
                        }
                    }

                    if (!isInitialSignInRestore && remoteEntries.isNotEmpty()) {
                        val settingsMap = remoteEntries.mapValues { it.value.value }
                        preferencesRepository.applySyncableSettings(settingsMap)
                    }
                }

                // =========================================================================
                // PHASE 3: UPSTREAM PUSH (Pushes Remaining Dirty Local Changes to Cloud)
                // =========================================================================

                // 5. Push Dirty Playback History
                if (syncPlayback) {
                    val dirtyPlayback = playbackHistoryDao.getDirtyRows()
                    if (dirtyPlayback.isNotEmpty()) {
                        val cloudPlayback = dirtyPlayback.map { entity ->
                            val cleanProvider = (entity.provider?.trim()?.ifBlank { null } ?: "default").lowercase()
                            val cloudMediaId = encodeCloudPlaybackMediaId(entity.mediaId, entity.seasonNumber, entity.episodeNumber)
                            CloudPlaybackItem(
                                userId = userId,
                                profileId = entity.profileId,
                                provider = cleanProvider,
                                mediaId = cloudMediaId,
                                title = entity.title,
                                episodeTitle = entity.episodeTitle,
                                seasonNumber = entity.seasonNumber,
                                episodeNumber = entity.episodeNumber,
                                posterUrl = entity.posterUrl,
                                backdropUrl = entity.backdropUrl,
                                currentPositionMs = entity.currentPositionMs,
                                durationMs = entity.durationMs,
                                isCompleted = entity.isCompleted,
                                isDismissed = entity.isDismissedFromContinueWatching,
                                isDeleted = entity.isDeleted,
                                mediaType = entity.mediaType ?: "MOVIE",
                                episodeData = entity.episodeData,
                                lastWatchedTimestamp = entity.clientUpdatedAtMs.takeIf { it > 0L } ?: entity.lastWatchedTimestamp
                            )
                        }
                        val pushPlaybackResult = restClient.upsertPlaybackBatch(cloudPlayback)
                        if (pushPlaybackResult.isSuccess) {
                            Log.i(TAG, "Successfully pushed ${dirtyPlayback.size} playback items to cloud")
                            playbackHistoryDao.markClean(dirtyPlayback.map { it.id }, now)
                        } else {
                            Log.e(TAG, "Failed to push playback batch: ${pushPlaybackResult.exceptionOrNull()?.message}")
                        }
                    }
                }

                // 6. Push Dirty Watchlist (Grouped by profileId so all profiles are marked clean)
                if (syncWatchlist) {
                    val dirtyWatchlist = watchlistDao.getDirtyRows()
                    if (dirtyWatchlist.isNotEmpty()) {
                        val cloudWatchlist = dirtyWatchlist.map { entity ->
                            CloudWatchlistItem(
                                userId = userId,
                                profileId = entity.profileId,
                                mediaId = entity.mediaId,
                                title = entity.title,
                                posterUrl = entity.posterUrl,
                                backdropUrl = entity.backdropUrl,
                                mediaType = entity.mediaType,
                                rating = entity.rating,
                                releaseYear = entity.year,
                                status = entity.status,
                                isDeleted = entity.isDeleted,
                                addedTimestamp = entity.addedTimestamp,
                                clientUpdatedAtMs = entity.clientUpdatedAtMs
                            )
                        }
                        val pushWatchlistResult = restClient.upsertWatchlistBatch(cloudWatchlist)
                        if (pushWatchlistResult.isSuccess) {
                            Log.i(TAG, "Successfully pushed ${dirtyWatchlist.size} watchlist items to cloud")
                            val byProfile = dirtyWatchlist.groupBy { it.profileId }
                            for ((profId, rows) in byProfile) {
                                watchlistDao.markClean(rows.map { it.mediaId }, profId, now)
                            }
                        } else {
                            Log.e(TAG, "Failed to push watchlist batch: ${pushWatchlistResult.exceptionOrNull()?.message}")
                        }
                    }
                }

                // 7. Push User Playlists (After awaiting disk load & merging remote)
                if (syncPlaylists) {
                    val localPlaylists = playlistRepository.getAllPlaylistsIncludingDeleted()
                    if (localPlaylists.isNotEmpty()) {
                        val cloudPlaylists = localPlaylists.map { pl ->
                            CloudPlaylist(
                                userId = userId,
                                profileId = "default_user",
                                playlistId = pl.id,
                                title = pl.title,
                                isSystem = pl.isSystem,
                                itemsJson = json.encodeToString(pl.items),
                                createdAtMs = pl.createdAt,
                                lastPlayedAtMs = pl.lastPlayedAt,
                                isDeleted = pl.isDeleted,
                                clientUpdatedAtMs = pl.clientUpdatedAtMs
                            )
                        }
                        restClient.upsertPlaylistsBatch(cloudPlaylists)
                    }
                }

                preferencesRepository.setSupabaseLastSyncTimestamp(System.currentTimeMillis())
                _syncStatus.value = SyncStatus.SUCCESS
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync all failed", e)
                _syncStatus.value = SyncStatus.ERROR
                _syncErrorMessage.value = e.message
                Result.failure(e)
            }
        }
    }

    private fun installRestoredPluginReposIfNeeded(repoUrlsCsv: String?) {
        if (repoUrlsCsv.isNullOrBlank()) return
        val urls = repoUrlsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (urls.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val pluginService = PluginRepositoryService(context)
            for (url in urls) {
                try {
                    pluginService.installAllFromRepo(url)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not auto-install restored repo $url: ${e.message}")
                }
            }
        }
    }

    /**
     * Encodes season & episode numbers into the cloud `media_id` for TV episodes so that
     * `PRIMARY KEY (user_id, profile_id, provider, media_id)` stores each episode as its own row
     * without overwriting previous episodes of the same series.
     */
    private fun encodeCloudPlaybackMediaId(
        baseMediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): String {
        val cleanBase = baseMediaId.substringBefore("::s")
        return if (seasonNumber != null && episodeNumber != null) {
            "${cleanBase}::s${seasonNumber}_e${episodeNumber}"
        } else {
            cleanBase
        }
    }

    /**
     * Decodes a cloud `media_id` back into `(baseMediaId, seasonNumber, episodeNumber)`.
     */
    private fun decodeCloudPlaybackMediaId(
        cloudMediaId: String,
        explicitSeason: Int?,
        explicitEpisode: Int?
    ): Triple<String, Int?, Int?> {
        if (cloudMediaId.contains("::s")) {
            val base = cloudMediaId.substringBefore("::s")
            val suffix = cloudMediaId.substringAfter("::s")
            val s = suffix.substringBefore("_e").toIntOrNull() ?: explicitSeason
            val e = suffix.substringAfter("_e").toIntOrNull() ?: explicitEpisode
            return Triple(base, s, e)
        }
        return Triple(cloudMediaId, explicitSeason, explicitEpisode)
    }

    private fun generateRawNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        @Volatile
        private var instance: SupabaseSyncManager? = null

        fun getInstance(context: Context): SupabaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
