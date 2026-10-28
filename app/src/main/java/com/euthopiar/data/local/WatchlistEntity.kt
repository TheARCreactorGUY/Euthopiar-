package com.euthopiar.data.local

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "watchlist",
    primaryKeys = ["mediaId", "profileId"],
    indices = [
        Index(value = ["profileId", "addedTimestamp"])
    ]
)
data class WatchlistEntity(
    val mediaId: String,
    val profileId: String = "default_user",
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val mediaType: String, // MOVIE, TV_SERIES, ANIME
    val rating: Double?,
    val year: Int?,
    val status: String = "TO_BE_WATCHED", // TO_BE_WATCHED, WATCHING, COMPLETED
    val addedTimestamp: Long = System.currentTimeMillis()
)
