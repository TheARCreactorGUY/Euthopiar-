package com.euthopiar.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Decorating DataSource that normalizes masqueraded Content-Type headers for CDN streams.
 * Many streaming providers disguise video fragments as .html, .jpg, or .png
 * and upstream CDNs return Content-Type: text/html or image/jpeg to evade scrapers.
 * This wrapper intercepts getResponseHeaders() and reports Content-Type: video/mp4
 * for non-manifest media chunks so that Media3 binds the FragmentedMp4Extractor
 * rather than failing MIME sniffing.
 */
@OptIn(UnstableApi::class)
class MasqueradedMimeDataSource(
    private val upstream: DataSource,
    private val isMasqueradingEnabled: Boolean = true
) : DataSource {

    private var currentUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        val originalHeaders = upstream.responseHeaders
        if (!isMasqueradingEnabled) return originalHeaders

        val uri = currentUri ?: return originalHeaders
        val uriStr = uri.toString().lowercase()
        val path = uri.path?.lowercase() ?: ""

        val rawContentType = originalHeaders.entries
            .firstOrNull { it.key.equals("content-type", ignoreCase = true) }
            ?.value?.firstOrNull()?.lowercase() ?: ""

        // 1. If server explicitly returned valid streaming manifest or legitimate video/audio container, DO NOT TOUCH IT!
        val isAlreadyLegitMediaMime = rawContentType.contains("dash+xml") ||
                rawContentType.contains("apple.mpegurl") ||
                rawContentType.contains("x-mpegurl") ||
                rawContentType.startsWith("video/") ||
                rawContentType.startsWith("audio/") ||
                rawContentType == "application/octet-stream"

        if (isAlreadyLegitMediaMime) {
            return originalHeaders
        }

        // 2. Only intercept when upstream returned disguised Content-Type (image/* or text/html)
        val isDisguisedMediaChunk = path.endsWith(".html") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".png") ||
                path == "/a" ||
                path == "/b" ||
                rawContentType.startsWith("image/") ||
                rawContentType.startsWith("text/html")

        if (isDisguisedMediaChunk) {
            val modified = java.util.TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
            modified.putAll(originalHeaders)
            // Fragmented MP4 vs MPEG-TS
            // Path /b on workers or explicit .m4s / init-stream / chunk-stream -> video/mp4
            // Path /a on workers or .jpg/.html disguised chunks -> video/mp2t (MPEG-TS)
            val isMp4 = path.endsWith(".m4s") ||
                    path.endsWith(".mp4") ||
                    path == "/b" ||
                    uriStr.contains("init-stream") ||
                    uriStr.contains("chunk-stream") ||
                    uriStr.contains(".m4s")
            val mime = if (isMp4) {
                "video/mp4"
            } else {
                "video/mp2t"
            }
            modified["Content-Type"] = listOf(mime)
            modified["content-type"] = listOf(mime)
            return modified
        }

        return originalHeaders
    }

    override fun close() {
        currentUri = null
        upstream.close()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val isMasqueradingEnabled: Boolean = true
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return MasqueradedMimeDataSource(upstreamFactory.createDataSource(), isMasqueradingEnabled)
        }
    }
}
