package com.euthopiar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("""
        SELECT p1.* FROM playback_history p1
        INNER JOIN (
            SELECT mediaId, MAX(lastWatchedTimestamp) AS maxWatched
            FROM playback_history
            WHERE profileId = :profileId
              AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider))
            GROUP BY mediaId
        ) p2 ON p1.mediaId = p2.mediaId AND p1.lastWatchedTimestamp = p2.maxWatched
        WHERE p1.profileId = :profileId
          AND p1.isCompleted = 0
          AND p1.currentPositionMs > 1000
          AND p1.isDismissedFromContinueWatching = 0
          AND (:provider IS NULL OR LOWER(p1.provider) = LOWER(:provider))
        GROUP BY p1.mediaId
        ORDER BY p1.lastWatchedTimestamp DESC
    """)
    fun observeContinueWatching(provider: String?, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("""
        SELECT p1.* FROM playback_history p1
        INNER JOIN (
            SELECT mediaId, MAX(lastWatchedTimestamp) AS maxWatched
            FROM playback_history
            WHERE profileId = :profileId
            GROUP BY mediaId
        ) p2 ON p1.mediaId = p2.mediaId AND p1.lastWatchedTimestamp = p2.maxWatched
        WHERE p1.profileId = :profileId
          AND p1.isCompleted = 0
          AND p1.currentPositionMs > 1000
          AND p1.isDismissedFromContinueWatching = 0
        GROUP BY p1.mediaId
        ORDER BY p1.lastWatchedTimestamp DESC
    """)
    fun observeAllContinueWatching(profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeHistory(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isCompleted = 1 AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeCompleted(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY lastWatchedTimestamp DESC")
    suspend fun getAllHistory(): List<PlaybackHistoryEntity>

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
