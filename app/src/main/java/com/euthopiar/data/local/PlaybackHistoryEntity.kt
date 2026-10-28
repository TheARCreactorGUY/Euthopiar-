package com.euthopiar.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["profileId", "mediaId", "provider"]),
        Index(value = ["profileId", "lastWatchedTimestamp"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey val id: String, // "${profileId}::${cleanProvider}::${mediaId}" or with season/episode
    val mediaId: String,
    val title: String,
    val episodeTitle: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val currentPositionMs: Long,
    val durationMs: Long,
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val provider: String? = null,
    val mediaType: String? = null,
    val episodeData: String? = null,
    val isDismissedFromContinueWatching: Boolean = false,
    val profileId: String = "default_user"
) {
    companion object {
        fun buildId(
            provider: String?,
            mediaId: String,
            seasonNumber: Int? = null,
            episodeNumber: Int? = null,
            profileId: String = "default_user"
        ): String {
            val cleanProv = (provider?.trim()?.ifBlank { null } ?: "default").lowercase()
            return if (seasonNumber != null && episodeNumber != null) {
                "${profileId}::${cleanProv}::${mediaId}_s${seasonNumber}_e${episodeNumber}"
            } else {
                "${profileId}::${cleanProv}::${mediaId}"
            }
        }
    }
}
