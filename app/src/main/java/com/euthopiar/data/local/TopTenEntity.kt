package com.euthopiar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "top_ten_cache")
data class TopTenEntity(
    @PrimaryKey
    val cacheKey: String,
    val itemsJson: String,
    val fetchedAtEpochMs: Long
)
