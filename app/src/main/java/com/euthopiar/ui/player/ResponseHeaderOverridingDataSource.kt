package com.euthopiar.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.TreeMap

/**
 * Decorating DataSource that intercepts HTTP response headers from streaming CDNs and Debrid proxies.
 *
 * Provides two critical fixes:
 * 1. Injects explicit overridden response headers (e.g. from Stremio behaviorHints.proxyHeaders.response).
 * 2. Normalizes generic or masqueraded Content-Type headers (such as application/octet-stream, text/plain,
 *    text/html, or image/jpeg) into valid media MIME types (application/vnd.apple.mpegurl, video/mp4, video/mp2t)
 *    so Media3/ExoPlayer correctly binds the appropriate extractor rather than throwing UnrecognizedInputFormatException.
 */
@OptIn(UnstableApi::class)
class ResponseHeaderOverridingDataSource(
    private val upstream: DataSource,
    private val overriddenHeaders: Map<String, String> = emptyMap()
) : DataSource {

    private var currentDataSpec: DataSpec? = null

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        val originalHeaders = upstream.responseHeaders
        val headers = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
        headers.putAll(originalHeaders)

        // 1. Apply explicitly configured response header overrides
        for ((key, value) in overriddenHeaders) {
            headers[key] = listOf(value)
        }

        // 2. MIME type inference & normalization for streaming CDNs
        val uri = currentDataSpec?.uri ?: upstream.uri
        val uriStr = uri?.toString()?.lowercase() ?: ""
        val path = uri?.path?.lowercase() ?: ""

        val rawContentType = headers["Content-Type"]?.firstOrNull()?.lowercase() ?: ""

        // Case A: Stream is HLS (.m3u8, format=m3u8, or content?v=) but server sent generic or octet-stream MIME
        val isHlsStream = uriStr.contains(".m3u8") ||
                uriStr.contains("format=m3u8") ||
                uriStr.contains("/content?v=") ||
                path.endsWith(".m3u8")

        if (isHlsStream && (rawContentType.isBlank() ||
                    rawContentType == "application/octet-stream" ||
                    rawContentType.startsWith("text/plain") ||
                    rawContentType.startsWith("text/html"))) {
            headers["Content-Type"] = listOf("application/vnd.apple.mpegurl")
            headers["content-type"] = listOf("application/vnd.apple.mpegurl")
            return headers
        }

        // Case B: Stream is MPEG-TS segment (.ts) disguised or sent as octet-stream/image/text
        val isTsSegment = path.endsWith(".ts") || path == "/a" || (path.contains("segment") && !path.endsWith(".m4s"))
        if (isTsSegment && (rawContentType.isBlank() ||
                    rawContentType == "application/octet-stream" ||
                    rawContentType.startsWith("text/plain") ||
                    rawContentType.startsWith("text/html") ||
                    rawContentType.startsWith("image/"))) {
            headers["Content-Type"] = listOf("video/mp2t")
            headers["content-type"] = listOf("video/mp2t")
            return headers
        }

        // Case C: Fragmented MP4 segment (.m4s, .mp4, /b, or init/chunk streams)
        val isMp4Segment = path.endsWith(".m4s") ||
                path.endsWith(".mp4") ||
                path == "/b" ||
                uriStr.contains("init-stream") ||
                uriStr.contains("chunk-stream")
        if (isMp4Segment && (rawContentType.isBlank() ||
                    rawContentType == "application/octet-stream" ||
                    rawContentType.startsWith("text/plain") ||
                    rawContentType.startsWith("text/html") ||
                    rawContentType.startsWith("image/"))) {
            headers["Content-Type"] = listOf("video/mp4")
            headers["content-type"] = listOf("video/mp4")
            return headers
        }

        return headers
    }

    override fun close() {
        currentDataSpec = null
        upstream.close()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val overriddenHeaders: Map<String, String> = emptyMap()
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return ResponseHeaderOverridingDataSource(
                upstream = upstreamFactory.createDataSource(),
                overriddenHeaders = overriddenHeaders
            )
        }
    }
}
