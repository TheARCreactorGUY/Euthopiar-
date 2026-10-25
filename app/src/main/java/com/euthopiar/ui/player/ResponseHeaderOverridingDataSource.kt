package com.euthopiar.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.TreeMap

/**
 * Decorating DataSource that intercepts HTTP response headers and manifest payloads from streaming CDNs and Debrid proxies.
 *
 * Provides three critical fixes:
 * 1. Injects explicit overridden response headers (e.g. from Stremio behaviorHints.proxyHeaders.response).
 * 2. Normalizes generic or masqueraded Content-Type headers (such as application/octet-stream, text/plain,
 *    text/html, or image/jpeg) into valid media MIME types (application/vnd.apple.mpegurl, application/dash+xml, video/mp4, video/mp2t).
 * 3. Intercepts and sanitizes MPEG-DASH XML manifests with unescaped ampersands in query parameters (e.g. Vidstuck / Hakunaymatata CDNs)
 *    and normalizes hev1 to hvc1 so Android MediaCodec hardware video decoders bind reliably.
 */
@OptIn(UnstableApi::class)
class ResponseHeaderOverridingDataSource(
    private val upstream: DataSource,
    private val overriddenHeaders: Map<String, String> = emptyMap()
) : DataSource {

    private var currentDataSpec: DataSpec? = null
    private var interceptedStream: InputStream? = null

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val originalUriStr = dataSpec.uri.toString()
        val pinnedUrl = com.euthopiar.ui.player.sequential.SequentialStreamManager.getPinnedUrl(originalUriStr)
        val effectiveDataSpec = if (pinnedUrl != null && pinnedUrl != originalUriStr) {
            dataSpec.buildUpon().setUri(Uri.parse(pinnedUrl)).build()
        } else {
            dataSpec
        }

        currentDataSpec = effectiveDataSpec
        interceptedStream = null

        val uriStr = effectiveDataSpec.uri.toString().lowercase()
        val path = effectiveDataSpec.uri.path?.lowercase() ?: ""
        val isDashManifestRequest = (uriStr.contains(".mpd") ||
                uriStr.contains("format=mpd") ||
                path.endsWith(".mpd")) && !path.endsWith(".m4s")

        val openResult = if (isDashManifestRequest) {
            upstream.open(effectiveDataSpec)
            val bytes = try {
                val buffer = ByteArrayOutputStream()
                val temp = ByteArray(8192)
                var bytesRead: Int
                while (upstream.read(temp, 0, temp.size).also { bytesRead = it } != -1) {
                    buffer.write(temp, 0, bytesRead)
                }
                buffer.toByteArray()
            } finally {
                upstream.close()
            }

            val text = String(bytes, Charsets.UTF_8)
            if (text.contains("<MPD", ignoreCase = true)) {
                // 1. Sanitize raw unescaped ampersands in attributes (e.g. &Signature= and &Key-Pair-Id=)
                // Standard XML parsers (XmlPullParser) reject bare '&' as malformed entities
                var sanitized = text.replace(Regex("""&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)"""), "&amp;")

                // 2. Normalize hev1 to hvc1 so Android hardware decoders recognize out-of-band parameter sets (hvcC box)
                sanitized = sanitized.replace(Regex("""codecs="hev1\."""), """codecs="hvc1.""")

                val sanitizedBytes = sanitized.toByteArray(Charsets.UTF_8)
                interceptedStream = ByteArrayInputStream(sanitizedBytes)
                sanitizedBytes.size.toLong()
            } else {
                interceptedStream = ByteArrayInputStream(bytes)
                bytes.size.toLong()
            }
        } else {
            upstream.open(effectiveDataSpec)
        }

        val resolvedUri = upstream.uri?.toString()
        if (!resolvedUri.isNullOrBlank() && resolvedUri != originalUriStr) {
            com.euthopiar.ui.player.sequential.SequentialStreamManager.pinResolvedUrl(originalUriStr, resolvedUri)
        }

        return openResult
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val stream = interceptedStream
        if (stream != null) {
            val readBytes = stream.read(buffer, offset, length)
            return if (readBytes == 0) -1 else readBytes
        }
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

        val isSubtitle = path.endsWith(".vtt") || path.endsWith(".srt") ||
                path.endsWith(".ass") || path.endsWith(".ssa") ||
                uriStr.contains("/subtitles") || uriStr.contains(".vtt?") || uriStr.contains(".srt?")

        if (isSubtitle) {
            return headers
        }

        val rawContentType = headers["Content-Type"]?.firstOrNull()?.lowercase() ?: ""

        val isGenericOrDisguised = rawContentType.isBlank() ||
                rawContentType.startsWith("image/") ||
                rawContentType.startsWith("text/html") ||
                rawContentType.startsWith("text/plain") ||
                rawContentType.startsWith("text/css") ||
                rawContentType.startsWith("application/octet-stream") ||
                rawContentType.startsWith("binary/octet-stream") ||
                rawContentType.startsWith("application/javascript") ||
                rawContentType.startsWith("text/javascript")

        val hasDisguisedExtension = path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") ||
                path.endsWith(".webp") || path.endsWith(".ico") || path.endsWith(".html") ||
                path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".txt") ||
                uriStr.contains(".jpg?") || uriStr.contains(".jpeg?") || uriStr.contains(".png?") ||
                uriStr.contains(".webp?") || uriStr.contains(".ico?") || uriStr.contains(".html?") ||
                uriStr.contains(".js?") || uriStr.contains(".css?") || uriStr.contains(".txt?")

        if (isGenericOrDisguised || hasDisguisedExtension) {
            // Case A: Stream is HLS playlist (.m3u8, .m3u, format=m3u8, or content?v=) but server sent generic/text/html/image MIME
            val isHlsStream = (uriStr.contains(".m3u8") ||
                    uriStr.contains(".m3u") ||
                    uriStr.contains("format=m3u8") ||
                    path.endsWith(".m3u8") ||
                    path.endsWith(".m3u")) && !path.endsWith(".ts") && !path.endsWith(".m4s")

            if (isHlsStream) {
                headers["Content-Type"] = listOf("application/vnd.apple.mpegurl")
                return headers
            }

            // Case A2: Stream is DASH MPD manifest (.mpd, format=mpd, manifest.mpd)
            val isDashManifest = (uriStr.contains(".mpd") ||
                    uriStr.contains("format=mpd") ||
                    path.endsWith(".mpd")) && !path.endsWith(".m4s")

            if (isDashManifest) {
                headers["Content-Type"] = listOf("application/dash+xml")
                return headers
            }

            // Case B: Stream is explicit MPEG-TS segment (.ts, /a, etc.)
            val isTsSegment = path.endsWith(".ts") || uriStr.contains(".ts?") ||
                    path == "/a" || ((path.contains("segment") || uriStr.contains("segment") || path.contains("seg-") || path.contains("seg_")) &&
                    !path.endsWith(".m4s") && !path.endsWith(".mp4") && !hasDisguisedExtension)
            if (isTsSegment) {
                headers["Content-Type"] = listOf("video/mp2t")
                return headers
            }

            // Case C: Fragmented MP4 segment (.m4s, .mp4, /b, or init/chunk streams)
            val isMp4Segment = path.endsWith(".m4s") || uriStr.contains(".m4s?") ||
                    path.endsWith(".mp4") || uriStr.contains(".mp4?") ||
                    path == "/b" ||
                    uriStr.contains("init-stream") ||
                    uriStr.contains("chunk-stream")
            if (isMp4Segment) {
                if (rawContentType.startsWith("audio/")) {
                    headers["Content-Type"] = listOf("audio/mp4")
                    return headers
                }
                if (rawContentType.startsWith("video/")) {
                    headers["Content-Type"] = listOf("video/mp4")
                    return headers
                }
                val isAudioSegment = uriStr.contains("audio") ||
                        uriStr.contains("sound") ||
                        uriStr.contains("voice") ||
                        uriStr.contains("/a/") ||
                        uriStr.contains("_a.") ||
                        uriStr.contains("-a.") ||
                        Regex("""(?:init|chunk)[-_]stream[1-9]""").containsMatchIn(uriStr) ||
                        Regex("""[-_/]a(?:udio)?[-_\d.]""").containsMatchIn(uriStr) ||
                        Regex("""track[-_]?(?:a|audio|[1-9])""").containsMatchIn(uriStr)

                if (isAudioSegment) {
                    headers["Content-Type"] = listOf("audio/mp4")
                    return headers
                }

                val isVideoSegment = uriStr.contains("video") ||
                        uriStr.contains("/v/") ||
                        uriStr.contains("_v.") ||
                        uriStr.contains("-v.") ||
                        path.endsWith(".mp4") ||
                        uriStr.contains(".mp4?") ||
                        uriStr.contains("init-stream") ||
                        uriStr.contains("chunk-stream") ||
                        Regex("""track[-_]?(?:v|video)""").containsMatchIn(uriStr)

                if (isVideoSegment) {
                    headers["Content-Type"] = listOf("video/mp4")
                    return headers
                }

                // Do not blindly force video/mp4 on ambiguous .m4s segments; remove Content-Type to let Media3 sniff moov/moof boxes
                headers.remove("Content-Type")
                return headers
            }

            // Case D: Disguised HLS segment (.jpg, .jpeg, .png, .webp, .ico, .html, .js, .css, .txt) or deceptive/generic MIME
            // Strip the bogus Content-Type header so Media3's extractor sniffs the real 0x47 (TS) or ftyp/moof (fMP4) magic bytes
            val isDeceptiveOrGenericMime = rawContentType.startsWith("image/") ||
                    rawContentType.startsWith("text/html") ||
                    rawContentType.startsWith("text/plain") ||
                    rawContentType.startsWith("text/css") ||
                    rawContentType.startsWith("application/javascript") ||
                    rawContentType.startsWith("text/javascript") ||
                    rawContentType.startsWith("application/octet-stream") ||
                    rawContentType.startsWith("binary/octet-stream")

            if (isDeceptiveOrGenericMime || hasDisguisedExtension) {
                headers.remove("Content-Type")
                return headers
            }
        }

        return headers
    }

    override fun close() {
        try {
            interceptedStream?.close()
        } catch (_: Throwable) {}
        interceptedStream = null
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
