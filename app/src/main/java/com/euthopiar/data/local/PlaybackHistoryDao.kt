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
              AND isDeleted = 0
              AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider))
            GROUP BY mediaId
        ) p2 ON p1.mediaId = p2.mediaId AND p1.lastWatchedTimestamp = p2.maxWatched
        WHERE p1.profileId = :profileId
          AND p1.isCompleted = 0
          AND p1.currentPositionMs > 1000
          AND p1.isDismissedFromContinueWatching = 0
          AND p1.isDeleted = 0
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
              AND isDeleted = 0
            GROUP BY mediaId
        ) p2 ON p1.mediaId = p2.mediaId AND p1.lastWatchedTimestamp = p2.maxWatched
        WHERE p1.profileId = :profileId
          AND p1.isCompleted = 0
          AND p1.currentPositionMs > 1000
          AND p1.isDismissedFromContinueWatching = 0
          AND p1.isDeleted = 0
        GROUP BY p1.mediaId
        ORDER BY p1.lastWatchedTimestamp DESC
    """)
    fun observeAllContinueWatching(profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isDeleted = 0 AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeHistory(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND isCompleted = 1 AND isDeleted = 0 AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider)) ORDER BY lastWatchedTimestamp DESC")
    fun observeCompleted(provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE isDeleted = 0 ORDER BY lastWatchedTimestamp DESC")
    suspend fun getAllHistory(): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getProgress(id: String): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history WHERE id = :id LIMIT 1")
    suspend fun getRawById(id: String): PlaybackHistoryEntity?

    @Query("""
        SELECT * FROM playback_history
        WHERE profileId = :profileId
          AND mediaId = :mediaId
          AND isDeleted = 0
        ORDER BY
          CASE WHEN :provider IS NOT NULL AND LOWER(provider) = LOWER(:provider) THEN 0 ELSE 1 END ASC,
          lastWatchedTimestamp DESC
        LIMIT 1
    """)
    fun observeLatestPlaybackForMedia(mediaId: String, provider: String? = null, profileId: String = "default_user"): Flow<PlaybackHistoryEntity?>

    @Query("""
        SELECT * FROM playback_history
        WHERE profileId = :profileId
          AND mediaId = :mediaId
          AND isDeleted = 0
        ORDER BY
          CASE WHEN :provider IS NOT NULL AND LOWER(provider) = LOWER(:provider) THEN 0 ELSE 1 END ASC,
          lastWatchedTimestamp ASC
    """)
    fun observePlaybackForMedia(mediaId: String, provider: String? = null, profileId: String = "default_user"): Flow<List<PlaybackHistoryEntity>>

    @Query("""
        SELECT * FROM playback_history
        WHERE profileId = :profileId
          AND mediaId = :mediaId
          AND (:seasonNumber IS NULL OR seasonNumber = :seasonNumber)
          AND (:episodeNumber IS NULL OR episodeNumber = :episodeNumber)
          AND isDeleted = 0
        ORDER BY lastWatchedTimestamp DESC
        LIMIT 1
    """)
    suspend fun getProgressForMediaAnyProvider(
        mediaId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        profileId: String = "default_user"
    ): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history WHERE isDirty = 1")
    suspend fun getDirtyRows(): List<PlaybackHistoryEntity>

    @Query("UPDATE playback_history SET isDirty = 0 WHERE id IN (:ids) AND clientUpdatedAtMs <= :upToTimestamp")
    suspend fun markClean(ids: List<String>, upToTimestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaybackHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(items: List<PlaybackHistoryEntity>)

    @Query("""
        UPDATE playback_history 
        SET currentPositionMs = :positionMs, 
            durationMs = :durationMs, 
            isCompleted = :isCompleted, 
            lastWatchedTimestamp = :lastWatchedTs, 
            clientUpdatedAtMs = :clientUpdatedTs, 
            isDirty = 1 
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: String,
        positionMs: Long,
        durationMs: Long,
        isCompleted: Boolean,
        lastWatchedTs: Long,
        clientUpdatedTs: Long
    )

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE id = :id")
    suspend fun dismissFromContinueWatching(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE id IN (:ids)")
    suspend fun dismissBatchFromContinueWatching(ids: List<String>, now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDismissedFromContinueWatching = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId AND (:provider IS NULL OR LOWER(provider) = LOWER(:provider))")
    suspend fun clearContinueWatching(provider: String? = null, profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE id IN (:ids)")
    suspend fun softDeleteBatch(ids: List<String>, now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId AND mediaId IN (:mediaIds)")
    suspend fun softDeleteBatchByMediaId(mediaIds: List<String>, profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("UPDATE playback_history SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId")
    suspend fun softClearHistory(profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM playback_history WHERE id IN (:ids)")
    suspend fun deleteBatch(ids: List<String>)

    @Query("DELETE FROM playback_history WHERE profileId = :profileId AND mediaId IN (:mediaIds)")
    suspend fun deleteBatchByMediaId(mediaIds: List<String>, profileId: String = "default_user")

    @Query("DELETE FROM playback_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String = "default_user")
}
