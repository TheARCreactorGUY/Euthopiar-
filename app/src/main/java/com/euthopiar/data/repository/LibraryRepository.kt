package com.euthopiar.data.repository

import android.content.Context
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.PlaybackHistoryDao
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.local.WatchlistDao
import com.euthopiar.data.local.WatchlistEntity
import com.euthopiar.data.profile.ProfileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepository(
    private val watchlistDao: WatchlistDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val profileManager: ProfileManager? = null,
    private val syncManagerProvider: (() -> com.euthopiar.data.sync.MediaSyncManager)? = null,
    private val supabaseSyncManagerProvider: (() -> com.euthopiar.data.sync.supabase.SupabaseSyncManager)? = null
) {
    private fun notifyCloudSync() {
        supabaseSyncManagerProvider?.invoke()?.scheduleSync()
    }
    private fun currentProfileId(): String = profileManager?.activeProfile?.value?.id ?: "default_user"

    val watchlist: Flow<List<WatchlistEntity>> =
        profileManager?.activeProfile?.flatMapLatest { profile ->
            watchlistDao.observeWatchlist(profile.id)
        } ?: watchlistDao.observeWatchlist("default_user")

    val continueWatching: Flow<List<PlaybackHistoryEntity>> =
        profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observeAllContinueWatching(profile.id)
        } ?: playbackHistoryDao.observeAllContinueWatching("default_user")

    val history: Flow<List<PlaybackHistoryEntity>> =
        profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observeHistory(profileId = profile.id)
        } ?: playbackHistoryDao.observeHistory(profileId = "default_user")

    val completed: Flow<List<PlaybackHistoryEntity>> =
        profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observeCompleted(profileId = profile.id)
        } ?: playbackHistoryDao.observeCompleted(profileId = "default_user")

    fun getContinueWatching(provider: String? = null): Flow<List<PlaybackHistoryEntity>> {
        return profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observeContinueWatching(provider, profile.id)
        } ?: playbackHistoryDao.observeContinueWatching(provider, "default_user")
    }

    fun isWatchlisted(mediaId: String): Flow<Boolean> {
        return profileManager?.activeProfile?.flatMapLatest { profile ->
            watchlistDao.isWatchlisted(mediaId, profile.id)
        } ?: watchlistDao.isWatchlisted(mediaId, "default_user")
    }

    fun getWatchlistEntity(mediaId: String): Flow<WatchlistEntity?> {
        return profileManager?.activeProfile?.flatMapLatest { profile ->
            watchlistDao.observeItem(mediaId, profile.id)
        } ?: watchlistDao.observeItem(mediaId, "default_user")
    }

    fun getLatestPlaybackForMedia(mediaId: String, provider: String? = null): Flow<PlaybackHistoryEntity?> {
        return profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observeLatestPlaybackForMedia(mediaId, provider, profile.id)
        } ?: playbackHistoryDao.observeLatestPlaybackForMedia(mediaId, provider, "default_user")
    }

    fun getPlaybackForMedia(mediaId: String, provider: String? = null): Flow<List<PlaybackHistoryEntity>> {
        return profileManager?.activeProfile?.flatMapLatest { profile ->
            playbackHistoryDao.observePlaybackForMedia(mediaId, provider, profile.id)
        } ?: playbackHistoryDao.observePlaybackForMedia(mediaId, provider, "default_user")
    }

    suspend fun setWatchlistStatus(mediaDetail: MediaDetail, status: String) {
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val existing = watchlistDao.getWatchlistItem(mediaDetail.id, profileId)
        if (existing != null) {
            watchlistDao.updateStatus(mediaDetail.id, status, profileId, now)
        } else {
            val entity = WatchlistEntity(
                mediaId = mediaDetail.id,
                profileId = profileId,
                title = mediaDetail.title,
                posterUrl = mediaDetail.posterUrl,
                backdropUrl = mediaDetail.backdropUrl,
                mediaType = mediaDetail.type.name,
                rating = mediaDetail.rating?.toDoubleOrNull(),
                year = mediaDetail.year,
                status = status,
                addedTimestamp = now,
                isDirty = true,
                isDeleted = false,
                clientUpdatedAtMs = now
            )
            watchlistDao.insert(entity)
        }
        if (status == "TO_BE_WATCHED" || status == "WATCHING") {
            syncManagerProvider?.invoke()?.notifyWatchlistAdded(mediaDetail)
        } else if (status == "COMPLETED") {
            val isTv = mediaDetail.type.name.contains("TV", ignoreCase = true) || mediaDetail.type.name.contains("ANIME", ignoreCase = true)
            syncManagerProvider?.invoke()?.notifyMediaCompleted(mediaDetail.id, mediaDetail.title, isTv)
        }
        notifyCloudSync()
    }

    suspend fun setWatchlistStatus(item: MediaItem, status: String) {
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val existing = watchlistDao.getWatchlistItem(item.id, profileId)
        if (existing != null) {
            watchlistDao.updateStatus(item.id, status, profileId, now)
        } else {
            val entity = WatchlistEntity(
                mediaId = item.id,
                profileId = profileId,
                title = item.title,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                mediaType = item.type.name,
                rating = item.rating?.toDoubleOrNull(),
                year = item.year,
                status = status,
                addedTimestamp = now,
                isDirty = true,
                isDeleted = false,
                clientUpdatedAtMs = now
            )
            watchlistDao.insert(entity)
        }
        if (status == "TO_BE_WATCHED" || status == "WATCHING") {
            syncManagerProvider?.invoke()?.notifyWatchlistAdded(item)
        } else if (status == "COMPLETED") {
            val isTv = item.type.name.contains("TV", ignoreCase = true) || item.type.name.contains("ANIME", ignoreCase = true)
            syncManagerProvider?.invoke()?.notifyMediaCompleted(item.id, item.title, isTv)
        }
        notifyCloudSync()
    }

    suspend fun toggleWatchlist(item: MediaItem): Boolean {
        val profileId = currentProfileId()
        val exists = watchlistDao.countWatchlist(item.id, profileId) > 0
        val now = System.currentTimeMillis()
        return if (exists) {
            watchlistDao.softDelete(item.id, profileId, now)
            syncManagerProvider?.invoke()?.notifyWatchlistRemoved(item.id, item.title, item.type.name.contains("TV", ignoreCase = true))
            notifyCloudSync()
            false
        } else {
            val entity = WatchlistEntity(
                mediaId = item.id,
                profileId = profileId,
                title = item.title,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                mediaType = item.type.name,
                rating = item.rating?.toDoubleOrNull(),
                year = item.year,
                status = "TO_BE_WATCHED",
                addedTimestamp = now,
                isDirty = true,
                isDeleted = false,
                clientUpdatedAtMs = now
            )
            watchlistDao.insert(entity)
            syncManagerProvider?.invoke()?.notifyWatchlistAdded(item)
            notifyCloudSync()
            true
        }
    }

    suspend fun toggleWatchlist(mediaDetail: MediaDetail): Boolean {
        val profileId = currentProfileId()
        val exists = watchlistDao.countWatchlist(mediaDetail.id, profileId) > 0
        val now = System.currentTimeMillis()
        return if (exists) {
            watchlistDao.softDelete(mediaDetail.id, profileId, now)
            syncManagerProvider?.invoke()?.notifyWatchlistRemoved(mediaDetail.id, mediaDetail.title, mediaDetail.type.name.contains("TV", ignoreCase = true))
            notifyCloudSync()
            false
        } else {
            val entity = WatchlistEntity(
                mediaId = mediaDetail.id,
                profileId = profileId,
                title = mediaDetail.title,
                posterUrl = mediaDetail.posterUrl,
                backdropUrl = mediaDetail.backdropUrl,
                mediaType = mediaDetail.type.name,
                rating = mediaDetail.rating?.toDoubleOrNull(),
                year = mediaDetail.year,
                status = "TO_BE_WATCHED",
                addedTimestamp = now,
                isDirty = true,
                isDeleted = false,
                clientUpdatedAtMs = now
            )
            watchlistDao.insert(entity)
            syncManagerProvider?.invoke()?.notifyWatchlistAdded(mediaDetail)
            notifyCloudSync()
            true
        }
    }

    suspend fun removeFromWatchlist(mediaId: String) {
        val now = System.currentTimeMillis()
        watchlistDao.softDelete(mediaId, currentProfileId(), now)
        syncManagerProvider?.invoke()?.notifyWatchlistRemoved(mediaId)
        notifyCloudSync()
    }

    suspend fun removeFromWatchlistBatch(mediaIds: List<String>) {
        if (mediaIds.isNotEmpty()) {
            val now = System.currentTimeMillis()
            watchlistDao.softDeleteBatch(mediaIds, currentProfileId(), now)
            notifyCloudSync()
        }
    }

    suspend fun insertWatchlistBatch(items: List<WatchlistEntity>) {
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        for (item in items) {
            watchlistDao.insert(item.copy(profileId = profileId, isDirty = true, clientUpdatedAtMs = now))
        }
        notifyCloudSync()
    }

    suspend fun insertPlaybackHistoryBatch(items: List<PlaybackHistoryEntity>) {
        if (items.isEmpty()) return
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val scoped = items.map { entity ->
            val properId = if (entity.id.contains("::")) {
                entity.id
            } else {
                PlaybackHistoryEntity.buildId(entity.provider, entity.mediaId, entity.seasonNumber, entity.episodeNumber, profileId)
            }
            entity.copy(id = properId, profileId = profileId, isDirty = true, clientUpdatedAtMs = now)
        }
        playbackHistoryDao.upsertBatch(scoped)
        notifyCloudSync()
    }

    suspend fun addToWatchlist(
        mediaId: String,
        title: String,
        posterUrl: String?,
        backdropUrl: String?,
        mediaType: String,
        rating: Double?,
        year: Int?,
        status: String = "TO_BE_WATCHED"
    ) {
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val entity = WatchlistEntity(
            mediaId = mediaId,
            profileId = profileId,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            mediaType = mediaType,
            rating = rating,
            year = year,
            status = status,
            addedTimestamp = now,
            isDirty = true,
            isDeleted = false,
            clientUpdatedAtMs = now
        )
        watchlistDao.insert(entity)
        notifyCloudSync()
    }

    suspend fun updatePlaybackProgress(
        mediaId: String,
        title: String,
        episodeItem: EpisodeItem? = null,
        currentPositionMs: Long,
        durationMs: Long,
        backdropUrl: String?,
        posterUrl: String?,
        provider: String? = null,
        mediaType: String? = null,
        episodeData: String? = null
    ) {
        val season = episodeItem?.seasonNumber
        val episode = episodeItem?.episodeNumber
        val epTitle = episodeItem?.title

        updatePlaybackProgressInternal(
            mediaId = mediaId,
            title = title,
            episodeTitle = epTitle,
            seasonNumber = season,
            episodeNumber = episode,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            backdropUrl = backdropUrl,
            posterUrl = posterUrl,
            provider = provider,
            mediaType = mediaType,
            episodeData = episodeData
        )
    }

    suspend fun updatePlaybackProgress(
        mediaId: String,
        title: String,
        episodeTitle: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        currentPositionMs: Long,
        durationMs: Long,
        backdropUrl: String?,
        posterUrl: String?,
        provider: String? = null,
        mediaType: String? = null,
        episodeData: String? = null
    ) {
        updatePlaybackProgressInternal(
            mediaId = mediaId,
            title = title,
            episodeTitle = episodeTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            backdropUrl = backdropUrl,
            posterUrl = posterUrl,
            provider = provider,
            mediaType = mediaType,
            episodeData = episodeData
        )
    }

    private suspend fun updatePlaybackProgressInternal(
        mediaId: String,
        title: String,
        episodeTitle: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        currentPositionMs: Long,
        durationMs: Long,
        backdropUrl: String?,
        posterUrl: String?,
        provider: String? = null,
        mediaType: String? = null,
        episodeData: String? = null
    ) {
        val profileId = currentProfileId()
        val id = PlaybackHistoryEntity.buildId(provider, mediaId, seasonNumber, episodeNumber, profileId)

        val isCompleted = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()) >= 0.92f
        } else {
            false
        }

        val existing = playbackHistoryDao.getProgress(id)
        val watchlistItem = if (posterUrl.isNullOrBlank() || backdropUrl.isNullOrBlank()) {
            watchlistDao.getWatchlistItem(mediaId, profileId)
        } else null
        val isTv = seasonNumber != null || episodeNumber != null ||
            mediaType?.contains("TV", ignoreCase = true) == true ||
            mediaType?.contains("ANIME", ignoreCase = true) == true
        val cachedTmdb = if (posterUrl.isNullOrBlank() || backdropUrl.isNullOrBlank()) {
            com.euthopiar.core.util.TmdbBridge.getCachedEnrichedDetails(mediaId, isTv)
        } else null

        val resolvedPoster = posterUrl?.takeIf { it.isNotBlank() }
            ?: existing?.posterUrl?.takeIf { it.isNotBlank() }
            ?: watchlistItem?.posterUrl?.takeIf { it.isNotBlank() }
            ?: cachedTmdb?.posterUrl?.takeIf { it.isNotBlank() }

        val resolvedBackdrop = backdropUrl?.takeIf { it.isNotBlank() }
            ?: existing?.backdropUrl?.takeIf { it.isNotBlank() }
            ?: watchlistItem?.backdropUrl?.takeIf { it.isNotBlank() }
            ?: cachedTmdb?.backdropUrl?.takeIf { it.isNotBlank() }
            ?: resolvedPoster

        val entity = PlaybackHistoryEntity(
            id = id,
            mediaId = mediaId,
            title = title,
            episodeTitle = episodeTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            posterUrl = resolvedPoster,
            backdropUrl = resolvedBackdrop,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            lastWatchedTimestamp = System.currentTimeMillis(),
            isCompleted = isCompleted,
            provider = provider,
            mediaType = mediaType,
            episodeData = episodeData,
            isDismissedFromContinueWatching = false,
            profileId = profileId
        )

        playbackHistoryDao.upsert(entity)
        notifyCloudSync()

        // Synchronize Watchlist status if this media is bookmarked
        if (watchlistDao.countWatchlist(mediaId, profileId) > 0) {
            val watchlistStatus = if (isCompleted) "COMPLETED" else "WATCHING"
            watchlistDao.updateStatus(mediaId, watchlistStatus, profileId)
        }
    }

    suspend fun updateWatchlistStatus(mediaId: String, status: String) {
        val now = System.currentTimeMillis()
        watchlistDao.updateStatus(mediaId, status, currentProfileId(), now)
        notifyCloudSync()
    }

    suspend fun getPlaybackProgress(
        id: String,
        provider: String? = null,
        mediaId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): PlaybackHistoryEntity? {
        val direct = playbackHistoryDao.getProgress(id)
        if (direct != null) return direct

        val profileId = currentProfileId()
        if (mediaId != null) {
            val scopedId = PlaybackHistoryEntity.buildId(provider, mediaId, seasonNumber, episodeNumber, profileId)
            val scoped = playbackHistoryDao.getProgress(scopedId)
            if (scoped != null) return scoped

            val fallbackAnyProvider = playbackHistoryDao.getProgressForMediaAnyProvider(
                mediaId = mediaId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                profileId = profileId
            )
            if (fallbackAnyProvider != null) return fallbackAnyProvider
        }
        return null
    }

    suspend fun dismissFromContinueWatching(id: String) {
        val now = System.currentTimeMillis()
        playbackHistoryDao.dismissFromContinueWatching(id, now)
        notifyCloudSync()
    }

    suspend fun dismissBatchFromContinueWatching(ids: List<String>) {
        if (ids.isNotEmpty()) {
            val now = System.currentTimeMillis()
            playbackHistoryDao.dismissBatchFromContinueWatching(ids, now)
            notifyCloudSync()
        }
    }

    suspend fun clearContinueWatching(provider: String? = null) {
        val now = System.currentTimeMillis()
        playbackHistoryDao.clearContinueWatching(provider, currentProfileId(), now)
        notifyCloudSync()
    }

    suspend fun removeFromHistory(id: String) {
        val now = System.currentTimeMillis()
        playbackHistoryDao.softDelete(id, now)
        notifyCloudSync()
    }

    suspend fun removeFromHistoryBatch(ids: List<String>) {
        if (ids.isNotEmpty()) {
            val now = System.currentTimeMillis()
            playbackHistoryDao.softDeleteBatch(ids, now)
            notifyCloudSync()
        }
    }

    suspend fun removeFromHistoryByMediaIdsBatch(mediaIds: List<String>) {
        if (mediaIds.isNotEmpty()) {
            val now = System.currentTimeMillis()
            playbackHistoryDao.softDeleteBatchByMediaId(mediaIds, currentProfileId(), now)
            notifyCloudSync()
        }
    }

    suspend fun markAsCompleted(id: String) {
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val existing = playbackHistoryDao.getProgress(id)
        if (existing != null) {
            val updated = existing.copy(
                isCompleted = true,
                currentPositionMs = existing.durationMs.coerceAtLeast(existing.currentPositionMs),
                lastWatchedTimestamp = now,
                isDirty = true,
                clientUpdatedAtMs = now
            )
            playbackHistoryDao.upsert(updated)
            if (watchlistDao.countWatchlist(existing.mediaId, profileId) > 0) {
                watchlistDao.updateStatus(existing.mediaId, "COMPLETED", profileId, now)
            }
            syncManagerProvider?.invoke()?.notifyMediaCompleted(existing.mediaId, existing.title, existing.seasonNumber != null, existing.seasonNumber, existing.episodeNumber)
            notifyCloudSync()
        } else {
            // Check if it's a mediaId in watchlist directly
            if (watchlistDao.countWatchlist(id, profileId) > 0) {
                watchlistDao.updateStatus(id, "COMPLETED", profileId, now)
            }
            syncManagerProvider?.invoke()?.notifyMediaCompleted(id, "", false)
            notifyCloudSync()
        }
    }

    suspend fun clearHistory() {
        val now = System.currentTimeMillis()
        playbackHistoryDao.softClearHistory(currentProfileId(), now)
        notifyCloudSync()
    }

    companion object {
        @Volatile
        private var instance: LibraryRepository? = null

        fun getInstance(context: Context): LibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = AppDatabase.getInstance(context)
                    val profileMgr = ProfileManager.getInstance(context)
                    val appContext = context.applicationContext
                    LibraryRepository(
                        db.watchlistDao(),
                        db.playbackHistoryDao(),
                        profileMgr,
                        syncManagerProvider = { com.euthopiar.data.sync.MediaSyncManager.getInstance(appContext) },
                        supabaseSyncManagerProvider = { com.euthopiar.data.sync.supabase.SupabaseSyncManager.getInstance(appContext) }
                    ).also { instance = it }
                }
            }
        }
    }
}
