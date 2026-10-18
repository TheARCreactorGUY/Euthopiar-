package com.euthopiar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted authentication and scraper context for in-flight and resumable downloads.
 * Hoists custom headers (Referer, Origin, Authorization tokens) and stream metadata into Room
 * so downloads can survive process death and refresh expired tokens seamlessly.
 */
@Entity(tableName = "download_auth_context")
data class DownloadAuthContextEntity(
    @PrimaryKey
    val downloadId: String,
    val headersJson: String,
    val scraperId: String? = null,
    val mediaType: String? = null,
    val videoId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
