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
    private val profileManager: ProfileManager? = null
) {
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
        val existing = watchlistDao.getWatchlistItem(mediaDetail.id, profileId)
        if (existing != null) {
            watchlistDao.updateStatus(mediaDetail.id, status, profileId)
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
                addedTimestamp = System.currentTimeMillis()
            )
            watchlistDao.insert(entity)
        }
    }

    suspend fun setWatchlistStatus(item: MediaItem, status: String) {
        val profileId = currentProfileId()
        val existing = watchlistDao.getWatchlistItem(item.id, profileId)
        if (existing != null) {
            watchlistDao.updateStatus(item.id, status, profileId)
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
                addedTimestamp = System.currentTimeMillis()
            )
            watchlistDao.insert(entity)
        }
    }

    suspend fun toggleWatchlist(item: MediaItem): Boolean {
        val profileId = currentProfileId()
        val exists = watchlistDao.countWatchlist(item.id, profileId) > 0
        return if (exists) {
            watchlistDao.delete(item.id, profileId)
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
                addedTimestamp = System.currentTimeMillis()
            )
            watchlistDao.insert(entity)
            true
        }
    }

    suspend fun toggleWatchlist(mediaDetail: MediaDetail): Boolean {
        val profileId = currentProfileId()
        val exists = watchlistDao.countWatchlist(mediaDetail.id, profileId) > 0
        return if (exists) {
            watchlistDao.delete(mediaDetail.id, profileId)
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
                addedTimestamp = System.currentTimeMillis()
            )
            watchlistDao.insert(entity)
            true
        }
    }

    suspend fun removeFromWatchlist(mediaId: String) {
        watchlistDao.delete(mediaId, currentProfileId())
    }

    suspend fun removeFromWatchlistBatch(mediaIds: List<String>) {
        if (mediaIds.isNotEmpty()) {
            watchlistDao.deleteBatch(mediaIds, currentProfileId())
        }
    }

    suspend fun insertWatchlistBatch(items: List<WatchlistEntity>) {
        val profileId = currentProfileId()
        for (item in items) {
            watchlistDao.insert(item.copy(profileId = profileId))
        }
    }

    suspend fun insertPlaybackHistoryBatch(items: List<PlaybackHistoryEntity>) {
        if (items.isEmpty()) return
        val profileId = currentProfileId()
        val scoped = items.map { entity ->
            val properId = if (entity.id.contains("::")) {
                entity.id
            } else {
                PlaybackHistoryEntity.buildId(entity.provider, entity.mediaId, entity.seasonNumber, entity.episodeNumber, profileId)
            }
            entity.copy(id = properId, profileId = profileId)
        }
        playbackHistoryDao.upsertBatch(scoped)
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
            addedTimestamp = System.currentTimeMillis()
        )
        watchlistDao.insert(entity)
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

        val entity = PlaybackHistoryEntity(
            id = id,
            mediaId = mediaId,
            title = title,
            episodeTitle = episodeTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
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

        // Synchronize Watchlist status if this media is bookmarked
        if (watchlistDao.countWatchlist(mediaId, profileId) > 0) {
            val watchlistStatus = if (isCompleted) "COMPLETED" else "WATCHING"
            watchlistDao.updateStatus(mediaId, watchlistStatus, profileId)
        }
    }

    suspend fun updateWatchlistStatus(mediaId: String, status: String) {
        watchlistDao.updateStatus(mediaId, status, currentProfileId())
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

        if (mediaId != null) {
            val scopedId = PlaybackHistoryEntity.buildId(provider, mediaId, seasonNumber, episodeNumber, currentProfileId())
            val scoped = playbackHistoryDao.getProgress(scopedId)
            if (scoped != null) return scoped
        }
        return null
    }

    suspend fun dismissFromContinueWatching(id: String) {
        playbackHistoryDao.dismissFromContinueWatching(id)
    }

    suspend fun dismissBatchFromContinueWatching(ids: List<String>) {
        if (ids.isNotEmpty()) {
            playbackHistoryDao.dismissBatchFromContinueWatching(ids)
        }
    }

    suspend fun clearContinueWatching(provider: String? = null) {
        playbackHistoryDao.clearContinueWatching(provider, currentProfileId())
    }

    suspend fun removeFromHistory(id: String) {
        playbackHistoryDao.delete(id)
    }

    suspend fun removeFromHistoryBatch(ids: List<String>) {
        if (ids.isNotEmpty()) {
            playbackHistoryDao.deleteBatch(ids)
        }
    }

    suspend fun removeFromHistoryByMediaIdsBatch(mediaIds: List<String>) {
        if (mediaIds.isNotEmpty()) {
            playbackHistoryDao.deleteBatchByMediaId(mediaIds, currentProfileId())
        }
    }

    suspend fun markAsCompleted(id: String) {
        val profileId = currentProfileId()
        val existing = playbackHistoryDao.getProgress(id)
        if (existing != null) {
            val updated = existing.copy(
                isCompleted = true,
                currentPositionMs = existing.durationMs.coerceAtLeast(existing.currentPositionMs),
                lastWatchedTimestamp = System.currentTimeMillis()
            )
            playbackHistoryDao.upsert(updated)
            if (watchlistDao.countWatchlist(existing.mediaId, profileId) > 0) {
                watchlistDao.updateStatus(existing.mediaId, "COMPLETED", profileId)
            }
        } else {
            // Check if it's a mediaId in watchlist directly
            if (watchlistDao.countWatchlist(id, profileId) > 0) {
                watchlistDao.updateStatus(id, "COMPLETED", profileId)
            }
        }
    }

    suspend fun clearHistory() {
        playbackHistoryDao.clearHistory(currentProfileId())
    }

    companion object {
        @Volatile
        private var instance: LibraryRepository? = null

        fun getInstance(context: Context): LibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = AppDatabase.getInstance(context)
                    val profileMgr = ProfileManager.getInstance(context)
                    LibraryRepository(db.watchlistDao(), db.playbackHistoryDao(), profileMgr).also { instance = it }
                }
            }
        }
    }
}
