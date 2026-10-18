package com.euthopiar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadAuthContextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadAuthContextEntity)

    @Query("SELECT * FROM download_auth_context WHERE downloadId = :downloadId")
    suspend fun getByDownloadId(downloadId: String): DownloadAuthContextEntity?

    @Query("DELETE FROM download_auth_context WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: String)

    @Query("DELETE FROM download_auth_context")
    suspend fun clearAll()
}
