package com.euthopiar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isCompleted = 0 AND currentPositionMs > 1000 AND isDismissedFromContinueWatching = 0 AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeContinueWatching(provider: String?, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isCompleted = 0 AND currentPositionMs > 1000 AND isDismissedFromContinueWatching = 0 ORDER BY lastWatchedTimestamp DESC")
    fun observeAllContinueWatching(profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeHistory(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isCompleted = 1 AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeCompleted(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE id = :id LIMIT 1")
    suspend fun getProgress(id: String): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND mediaId = :mediaId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC LIMIT 1")
    fun observeLatestPlaybackForMedia(mediaId: String, provider: String? = null, profileId: String = "default_user"): Flow<PlaybackHistoryEntity?>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND mediaId = :mediaId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider))")
    fun observePlaybackForMedia(mediaId: String, provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaybackHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(items: List<PlaybackHistoryEntity>)

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1 WHERE id = :id")
    suspend fun dismissFromContinueWatching(id: String)

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1 WHERE id IN (:ids)")
    suspend fun dismissBatchFromContinueWatching(ids: List<String>)

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1 WHERE profileId = :profileId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider))")
    suspend fun clearContinueWatching(provider: String? = null, profileId: String = "default_user")

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM playback_history WHERE id IN (:ids)")
    suspend fun deleteBatch(ids: List<String>)

    @Query("DELETE FROM playback_history WHERE profileId = :profileId AND mediaId IN (:mediaIds)")
    suspend fun deleteBatchByMediaId(mediaIds: List<String>, profileId: String = "default_user")

    @Query("DELETE FROM playback_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String = "default_user")
}
