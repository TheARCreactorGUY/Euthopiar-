package com.euthopiar.download

import kotlinx.serialization.Serializable

enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Serializable
data class DownloadItem(
    val id: String,
    val mediaId: String,
    val episodeId: String? = null,
    val title: String,
    val subtitle: String? = null, // e.g. "Season 1 • Episode 1"
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val isTvSeries: Boolean = false,
    val qualityLabel: String = "1080p",
    val state: DownloadState = DownloadState.QUEUED,
    val progressPercentage: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val streamUrl: String = "",
    val failureReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val localFilePath: String? = null,
    val speed: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val provider: String? = null,
    val mediaType: String? = null,
    val scraperId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isM3u8: Boolean = false,
    val container: com.euthopiar.core.model.StreamContainer = if (isM3u8) com.euthopiar.core.model.StreamContainer.HLS else com.euthopiar.core.model.StreamContainer.AUTO,
    val trackers: List<String> = emptyList()
)

data class StorageStats(
    val appUsedBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long
)
