package com.euthopiar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE profileId = :profileId AND isDeleted = 0 ORDER BY addedTimestamp DESC")
    fun observeWatchlist(profileId: String = "default_user"): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId AND status = :status AND isDeleted = 0 ORDER BY addedTimestamp DESC")
    fun observeWatchlistByStatus(profileId: String = "default_user", status: String): Flow<List<WatchlistEntity>>

    @Query("SELECT COUNT(*) > 0 FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId AND isDeleted = 0")
    fun isWatchlisted(mediaId: String, profileId: String = "default_user"): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId AND isDeleted = 0")
    suspend fun countWatchlist(mediaId: String, profileId: String = "default_user"): Int

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId AND isDeleted = 0 LIMIT 1")
    fun observeItem(mediaId: String, profileId: String = "default_user"): Flow<WatchlistEntity?>

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId AND isDeleted = 0 LIMIT 1")
    suspend fun getWatchlistItem(mediaId: String, profileId: String = "default_user"): WatchlistEntity?

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId LIMIT 1")
    suspend fun getRawItem(mediaId: String, profileId: String = "default_user"): WatchlistEntity?

    @Query("UPDATE watchlist SET status = :status, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun updateStatus(mediaId: String, status: String, profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM watchlist WHERE isDeleted = 0 ORDER BY addedTimestamp DESC")
    suspend fun getAllWatchlist(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE isDirty = 1")
    suspend fun getDirtyRows(): List<WatchlistEntity>

    @Query("UPDATE watchlist SET isDirty = 0 WHERE profileId = :profileId AND mediaId IN (:mediaIds) AND clientUpdatedAtMs <= :upToTimestamp")
    suspend fun markClean(mediaIds: List<String>, profileId: String = "default_user", upToTimestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(items: List<WatchlistEntity>)

    @Query("UPDATE watchlist SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun softDelete(mediaId: String, profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("UPDATE watchlist SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId AND mediaId IN (:mediaIds)")
    suspend fun softDeleteBatch(mediaIds: List<String>, profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("UPDATE watchlist SET isDeleted = 1, isDirty = 1, clientUpdatedAtMs = :now WHERE profileId = :profileId")
    suspend fun softClearWatchlist(profileId: String = "default_user", now: Long = System.currentTimeMillis())

    @Query("DELETE FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun delete(mediaId: String, profileId: String = "default_user")

    @Query("DELETE FROM watchlist WHERE profileId = :profileId AND mediaId IN (:mediaIds)")
    suspend fun deleteBatch(mediaIds: List<String>, profileId: String = "default_user")
}
