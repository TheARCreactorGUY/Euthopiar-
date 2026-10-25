package com.euthopiar.download

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.dash.offline.DashDownloader
import androidx.media3.exoplayer.hls.offline.HlsDownloader
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.offline.DownloaderFactory
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class EuthopiarDownloaderFactory(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val executor: Executor = Executors.newFixedThreadPool(4)
) : DownloaderFactory {

    override fun createDownloader(request: DownloadRequest): Downloader {
        val uriStr = request.uri.toString().lowercase()
        val mimeType = request.mimeType?.lowercase() ?: ""

        val mediaItem = MediaItem.Builder()
            .setUri(request.uri)
            .setCustomCacheKey(request.customCacheKey)
            .setStreamKeys(request.streamKeys)
            .build()

        val isHls = mimeType.contains("mpegurl") || mimeType.contains("m3u8") ||
                uriStr.contains(".m3u8") || uriStr.contains("format=m3u8") || uriStr.contains("type=m3u8") || uriStr.contains("ext=m3u8")
        val isDash = mimeType.contains("dash") || mimeType.contains("mpd") ||
                uriStr.contains(".mpd") || uriStr.contains("format=mpd") || uriStr.contains("type=mpd") || uriStr.contains("ext=mpd")

        return when {
            isHls -> HlsDownloader(mediaItem, cacheDataSourceFactory, executor)
            isDash -> DashDownloader(mediaItem, cacheDataSourceFactory, executor)
            else -> ProgressiveDownloader(mediaItem, cacheDataSourceFactory, executor)
        }
    }
}
