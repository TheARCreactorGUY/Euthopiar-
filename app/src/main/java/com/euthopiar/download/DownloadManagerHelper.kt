package com.euthopiar.download

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import java.io.File
import java.util.concurrent.Executors

object DownloadManagerHelper {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "euthopiar_downloads"
    const val DOWNLOAD_NOTIFICATION_ID = 1001
    private const val DOWNLOAD_SUBDIR = "media_downloads"

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    @Volatile
    private var downloadCache: Cache? = null

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var notificationHelper: DownloadNotificationHelper? = null

    @Synchronized
    fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
            databaseProvider = it
        }
    }

    @Synchronized
    fun getDownloadDirectory(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val downloadDir = File(baseDir, DOWNLOAD_SUBDIR)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }

    @Synchronized
    fun getCache(context: Context): Cache {
        return downloadCache ?: run {
            val dbProvider = getDatabaseProvider(context)
            val downloadDir = getDownloadDirectory(context)
            SimpleCache(downloadDir, NoOpCacheEvictor(), dbProvider).also {
                downloadCache = it
            }
        }
    }

    fun getHttpDataSourceFactory(
        customHeaders: Map<String, String>? = null
    ): DefaultHttpDataSource.Factory {
        val headers = HashMap<String, String>().apply {
            put("User-Agent", com.euthopiar.core.network.AppUserAgent.get())
            if (customHeaders != null) {
                putAll(customHeaders)
            }
        }
        return DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setDefaultRequestProperties(headers)
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: run {
            val dbProvider = getDatabaseProvider(context)
            val cache = getCache(context)
            val upstreamFactory = getHttpDataSourceFactory()
            val executor = Executors.newFixedThreadPool(2)
            val manager = DownloadManager(
                context.applicationContext,
                dbProvider,
                cache,
                upstreamFactory,
                executor
            ).apply {
                maxParallelDownloads = 2
            }
            downloadManager = manager
            manager
        }
    }

    @Synchronized
    fun getNotificationHelper(context: Context): DownloadNotificationHelper {
        return notificationHelper ?: DownloadNotificationHelper(
            context.applicationContext,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID
        ).also {
            notificationHelper = it
        }
    }

    fun getCacheDataSourceFactory(
        context: Context,
        customHeaders: Map<String, String>? = null
    ): DataSource.Factory {
        val httpFactory = getHttpDataSourceFactory(customHeaders)
        val defaultFactory = DefaultDataSource.Factory(context, httpFactory)
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultFactory)
            .setCacheWriteDataSinkFactory(null) // Read-only during playback
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getOfflineCacheDataSourceFactory(
        context: Context
    ): DataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(null)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(0)
    }
}
