package com.euthopiar.ui.player.sequential

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.euthopiar.core.model.StreamContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
object SequentialStreamManager {
    @Volatile
    private var activeSession: SequentialStreamSession? = null

    private val pinnedUrls = ConcurrentHashMap<String, String>()

    /**
     * Automatically recognizes any remote http/https progressive stream as a cloud progressive stream,
     * supporting containers in [MP4, MKV, WEBM, AUTO] that are not HLS, DASH, or local loopback 127.0.0.1.
     */
    fun isSequentialStream(
        url: String,
        container: StreamContainer? = null,
        responseHeaders: Map<String, String> = emptyMap()
    ): Boolean {
        val trimmedUrl = url.trim()
        val urlLower = trimmedUrl.lowercase()

        // 1. Must be a remote HTTP or HTTPS stream
        if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
            return false
        }

        // 2. Reject local loopback / localhost
        val uri = try { Uri.parse(trimmedUrl) } catch (_: Throwable) { null }
        val host = uri?.host?.lowercase()?.removePrefix("[")?.removeSuffix("]") ?: ""
        if (host == "127.0.0.1" || host == "localhost" || host == "::1" ||
            urlLower.contains("://127.0.0.1") || urlLower.contains("://localhost") || urlLower.contains("://[::1]")
        ) {
            return false
        }

        // 3. Reject HLS streams
        if (container == StreamContainer.HLS ||
            ((container == null || container == StreamContainer.AUTO) &&
                (urlLower.contains(".m3u8") || urlLower.contains("format=m3u8") ||
                    responseHeaders["Content-Type"]?.contains("mpegurl", ignoreCase = true) == true))
        ) {
            return false
        }

        // 4. Reject DASH streams
        if (container == StreamContainer.DASH ||
            ((container == null || container == StreamContainer.AUTO) &&
                (urlLower.contains(".mpd") || urlLower.contains("format=mpd") ||
                    responseHeaders["Content-Type"]?.contains("dash+xml", ignoreCase = true) == true))
        ) {
            return false
        }

        // 5. Reject TS and P2P containers
        if (container == StreamContainer.TS || container == StreamContainer.P2P) {
            return false
        }

        // 6. Check Accept-Ranges: none (always sequential)
        val acceptRanges = responseHeaders["Accept-Ranges"]
        if (acceptRanges != null && acceptRanges.equals("none", ignoreCase = true)) {
            return true
        }

        // 7. Progressive container check: container in [MP4, MKV, WEBM, AUTO] or null/unspecified
        val validContainers = setOf(
            StreamContainer.MP4,
            StreamContainer.MKV,
            StreamContainer.WEBM,
            StreamContainer.AUTO
        )

        return container == null || container in validContainers
    }

    /**
     * Backward-compatible overload for callers passing (url, responseHeaders).
     */
    fun isSequentialStream(url: String, responseHeaders: Map<String, String>): Boolean =
        isSequentialStream(url, null, responseHeaders)

    /**
     * Pins the resolved redirect URL for a session/stream so subsequent requests (Range, cues, scrub)
     * hit the resolved CDN URL directly rather than repeatedly hitting the gateway redirector.
     */
    fun pinResolvedUrl(originalUrl: String, resolvedUrl: String) {
        if (originalUrl.isNotBlank() && resolvedUrl.isNotBlank() && originalUrl != resolvedUrl) {
            pinnedUrls[originalUrl] = resolvedUrl
            val session = activeSession
            if (session != null && session.url == originalUrl) {
                session.resolvedUrl = resolvedUrl
            }
        }
    }

    fun getPinnedUrl(originalUrl: String): String? = pinnedUrls[originalUrl]

    fun clearPinnedUrls() {
        pinnedUrls.clear()
    }

    /**
     * Proactively resolves any 301/302/307/308 redirects for an addon or progressive cloud stream URL
     * and pins the target CDN URL so ExoPlayer range requests don't hit redirect gateways repeatedly.
     */
    suspend fun preResolveAndPinUrl(
        okHttpClient: OkHttpClient,
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val pinned = getPinnedUrl(url)
        if (pinned != null) return@withContext pinned

        if (url.startsWith("http://127.0.0.1") || url.startsWith("http://localhost") || url.startsWith("http://[::1]")) {
            return@withContext url
        }

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            val req = reqBuilder.build()

            okHttpClient.newCall(req).execute().use { resp ->
                val redirectLoc = if (resp.isRedirect || resp.code in 300..399) {
                    resp.header("Location")?.let { loc ->
                        resp.request.url.resolve(loc)?.toString() ?: loc
                    }
                } else null

                val finalUrl = redirectLoc ?: resp.request.url.toString()
                if (finalUrl.isNotBlank() && finalUrl != url) {
                    pinResolvedUrl(url, finalUrl)
                    return@withContext finalUrl
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SequentialStreamManager", "Failed to pre-resolve redirect for $url: ${e.message}")
        }
        url
    }

    @Synchronized
    fun getOrCreateSession(
        context: Context,
        url: String,
        headers: Map<String, String>,
        okHttpClient: OkHttpClient
    ): SequentialStreamSession {
        val current = activeSession
        if (current != null && current.url == url && !current.isClosed) {
            return current
        }
        releaseActiveSession()
        return SequentialStreamSession(
            context = context.applicationContext,
            url = url,
            headers = headers,
            okHttpClient = okHttpClient
        ).also {
            activeSession = it
        }
    }

    @Synchronized
    fun releaseActiveSession() {
        try {
            activeSession?.release()
        } catch (_: Throwable) {}
        activeSession = null
    }

    fun cleanOrphanedCache(context: Context) {
        try {
            val cacheDir = context.applicationContext.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("seq_stream_")) {
                    file.deleteRecursively()
                }
            }
        } catch (_: Throwable) {}
    }
}
