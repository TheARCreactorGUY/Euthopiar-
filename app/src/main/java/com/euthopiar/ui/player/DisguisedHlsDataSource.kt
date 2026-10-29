package com.euthopiar.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.TreeMap

/**
 * Decorating DataSource for HLS media playback that inspects incoming segment data and:
 * 1. Strips prepended fake image bytes (e.g. PNG/JPEG/GIF decoy headers) before the 0x47 MPEG-TS sync byte.
 * 2. Normalizes disguised Content-Type headers (image, text, .png, .jpg, .txt, .js) to video/mp2t.
 */
@OptIn(UnstableApi::class)
class DisguisedHlsDataSource(
    private val upstream: DataSource,
    private val factory: Factory? = null,
    initialMasterQuery: String? = null,
    initialMasterHeaders: Map<String, String> = emptyMap()
) : DataSource {

    var masterQuery: String? = initialMasterQuery
    var masterHeaders: Map<String, String> = initialMasterHeaders

    private var currentDataSpec: DataSpec? = null
    private var headerInspected = false
    private var prefixBuffer: ByteArray? = null
    private var prefixOffset = 0
    private var prefixLimit = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        headerInspected = false
        prefixBuffer = null
        prefixOffset = 0
        prefixLimit = 0

        // Inspect and record master playlist query and headers
        val uri = dataSpec.uri
        val path = uri.path?.lowercase() ?: ""
        val uriStr = uri.toString().lowercase()
        val isPlaylist = path.endsWith(".m3u8") || path.endsWith(".m3u") || uriStr.contains("format=m3u8")
        val currentEncodedQuery = uri.encodedQuery
        if (isPlaylist && !currentEncodedQuery.isNullOrBlank()) {
            val existingShared = factory?.sharedMasterQuery ?: this.masterQuery
            if (existingShared.isNullOrBlank() || (!isTokenQuery(existingShared) && isTokenQuery(currentEncodedQuery))) {
                factory?.sharedMasterQuery = currentEncodedQuery
                this.masterQuery = currentEncodedQuery
            }
            if (dataSpec.httpRequestHeaders.isNotEmpty()) {
                val existingHeaders = factory?.sharedMasterHeaders ?: this.masterHeaders
                if (existingHeaders.isEmpty() || !existingShared.isNullOrBlank()) {
                    factory?.sharedMasterHeaders = dataSpec.httpRequestHeaders
                    this.masterHeaders = dataSpec.httpRequestHeaders
                }
            }
        }

        val openResult = try {
            upstream.open(dataSpec)
        } catch (e: Throwable) {
            val queryToInherit = factory?.sharedMasterQuery ?: this.masterQuery
            val headersToInherit = factory?.sharedMasterHeaders ?: this.masterHeaders
            if (isHttp403(e) && !queryToInherit.isNullOrBlank()) {
                try { upstream.close() } catch (_: Throwable) {}
                val retriedUri = appendQueryParameters(dataSpec.uri, queryToInherit)
                if (retriedUri != dataSpec.uri) {
                    val mergedHeaders = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    mergedHeaders.putAll(headersToInherit)
                    mergedHeaders.putAll(dataSpec.httpRequestHeaders)
                    val retryDataSpec = dataSpec.buildUpon()
                        .setUri(retriedUri)
                        .setHttpRequestHeaders(mergedHeaders)
                        .build()
                    currentDataSpec = retryDataSpec
                    upstream.open(retryDataSpec)
                } else {
                    throw e
                }
            } else {
                throw e
            }
        }

        // If master playlist was redirected to a CDN URL with query tokens, record the resolved query
        val resolvedUri = upstream.uri
        if (isPlaylist && resolvedUri != null && !resolvedUri.encodedQuery.isNullOrBlank()) {
            val resEncodedQuery = resolvedUri.encodedQuery
            val existingShared = factory?.sharedMasterQuery ?: this.masterQuery
            if (existingShared.isNullOrBlank() || (!isTokenQuery(existingShared) && isTokenQuery(resEncodedQuery))) {
                factory?.sharedMasterQuery = resEncodedQuery
                this.masterQuery = resEncodedQuery
            }
        }

        return openResult
    }

    private fun isTokenQuery(query: String?): Boolean {
        if (query.isNullOrBlank()) return false
        val tokenKeys = setOf(
            "token", "sig", "signature", "hdnts", "policy",
            "key-pair-id", "auth", "expires", "h", "wmsauthsign"
        )
        return query.split('&').any { param ->
            val key = param.substringBefore('=', "").trim().lowercase()
            key in tokenKeys
        }
    }

    private fun isHttp403(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                if (current.responseCode == 403 || current.responseCode == 401) return true
            }
            val msg = current.message?.lowercase() ?: ""
            if (msg.contains("403") || msg.contains("forbidden") || msg.contains("response code: 403") ||
                msg.contains("401") || msg.contains("unauthorized") || msg.contains("response code: 401")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun appendQueryParameters(targetUri: Uri, masterQuery: String): Uri {
        val targetEncodedQuery = targetUri.encodedQuery ?: ""
        val existingKeys = targetEncodedQuery.split('&').mapNotNull {
            val idx = it.indexOf('=')
            val key = if (idx > 0) it.substring(0, idx).trim() else it.trim()
            key.ifEmpty { null }
        }.toSet()

        val paramsToAppend = mutableListOf<String>()
        masterQuery.split('&').forEach { param ->
            val trimmedParam = param.trim()
            if (trimmedParam.isNotEmpty()) {
                val idx = trimmedParam.indexOf('=')
                val key = if (idx > 0) trimmedParam.substring(0, idx).trim() else trimmedParam
                if (key.isNotEmpty() && !existingKeys.contains(key)) {
                    paramsToAppend.add(trimmedParam)
                }
            }
        }

        if (paramsToAppend.isEmpty()) {
            return targetUri
        }

        val combinedQuery = if (targetEncodedQuery.isEmpty()) {
            paramsToAppend.joinToString("&")
        } else {
            targetEncodedQuery + "&" + paramsToAppend.joinToString("&")
        }

        return targetUri.buildUpon().encodedQuery(combinedQuery).build()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!headerInspected) {
            headerInspected = true
            val uri = currentDataSpec?.uri ?: upstream.uri
            val uriStr = uri?.toString()?.lowercase() ?: ""
            val path = uri?.path?.lowercase() ?: ""
            val isExplicitFmp4 = path.endsWith(".m4s") || path.endsWith(".mp4") || uriStr.contains(".m4s?") || uriStr.contains(".mp4?")

            // Read initial probe chunk to inspect for decoy header bytes
            val probeBuf = ByteArray(2048)
            val bytesRead = upstream.read(probeBuf, 0, probeBuf.size)
            if (bytesRead > 0) {
                val fourcc = if (bytesRead >= 8) String(probeBuf, 4, 4, Charsets.US_ASCII) else ""
                // If it starts with 0x47, it's already standard MPEG-TS
                // If it starts with #EXTM3U or fMP4 (ftyp/styp/moof/moov/sidx), leave untouched
                val isStandardTs = probeBuf[0] == 0x47.toByte()
                val isPlaylistOrFmp4 = isExplicitFmp4 ||
                        (bytesRead >= 7 && String(probeBuf, 0, 7, Charsets.US_ASCII).startsWith("#EXTM3U")) ||
                        (fourcc in setOf("ftyp", "styp", "moof", "moov", "sidx", "emsg", "prft"))

                var syncOffset = 0
                if (!isStandardTs && !isPlaylistOrFmp4 && bytesRead >= 188 * 2) {
                    for (i in 0 until (bytesRead - 188)) {
                        if (probeBuf[i] == 0x47.toByte() && probeBuf[i + 188] == 0x47.toByte()) {
                            syncOffset = i
                            break
                        }
                    }
                }

                prefixBuffer = probeBuf
                prefixOffset = syncOffset
                prefixLimit = bytesRead
            }
        }

        // Deliver from prefix buffer if available
        val pBuf = prefixBuffer
        if (pBuf != null && prefixOffset < prefixLimit) {
            val available = prefixLimit - prefixOffset
            val bytesToCopy = minOf(length, available)
            System.arraycopy(pBuf, prefixOffset, buffer, offset, bytesToCopy)
            prefixOffset += bytesToCopy
            if (prefixOffset >= prefixLimit) {
                prefixBuffer = null
            }
            return bytesToCopy
        }

        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri ?: currentDataSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        val original = upstream.responseHeaders
        val headers = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
        headers.putAll(original)

        val uri = currentDataSpec?.uri ?: upstream.uri
        val uriStr = uri?.toString()?.lowercase() ?: ""
        val path = uri?.path?.lowercase() ?: ""

        val isPlaylist = path.endsWith(".m3u8") || path.endsWith(".m3u") || uriStr.contains("format=m3u8")
        val isFmp4 = path.endsWith(".m4s") || path.endsWith(".mp4") || uriStr.contains(".m4s?") || uriStr.contains(".mp4?")
        if (!isPlaylist && !isFmp4) {
            val rawContentType = headers["Content-Type"]?.firstOrNull()?.lowercase() ?: ""
            val isDisguisedExt = path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                    path.endsWith(".txt") || path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".webp") ||
                    uriStr.contains(".png?") || uriStr.contains(".jpg?") || uriStr.contains(".jpeg?") ||
                    uriStr.contains(".txt?") || uriStr.contains(".js?")
            val isDisguisedMime = rawContentType.startsWith("image/") ||
                    rawContentType.startsWith("text/plain") ||
                    rawContentType.startsWith("text/javascript") ||
                    rawContentType.startsWith("application/javascript")

            if (isDisguisedExt || isDisguisedMime) {
                headers["Content-Type"] = listOf("video/mp2t")
            }
        }

        return headers
    }

    override fun close() {
        prefixBuffer = null
        currentDataSpec = null
        upstream.close()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private var initialMasterUrl: String? = null,
        private var initialMasterHeaders: Map<String, String> = emptyMap()
    ) : DataSource.Factory {
        @Volatile
        var sharedMasterQuery: String? = initialMasterUrl?.let { try { Uri.parse(it).encodedQuery } catch (_: Throwable) { null } }
        @Volatile
        var sharedMasterHeaders: Map<String, String> = initialMasterHeaders

        override fun createDataSource(): DataSource {
            return DisguisedHlsDataSource(
                upstream = upstreamFactory.createDataSource(),
                factory = this,
                initialMasterQuery = sharedMasterQuery,
                initialMasterHeaders = sharedMasterHeaders
            )
        }
    }
}
