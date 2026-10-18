package com.euthopiar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TopTenDao {
    @Query("SELECT * FROM top_ten_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): TopTenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TopTenEntity)

    @Query("DELETE FROM top_ten_cache WHERE cacheKey = :key")
    suspend fun deleteByKey(key: String)
}
