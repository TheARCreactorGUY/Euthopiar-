package com.euthopiar.core.js.network

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.cancellation.CancellationException

data class RawHttpResponse(
    val status: Int,
    val statusText: String,
    val url: String,
    val body: String,
    val bodyBytes: ByteArray,
    val headers: Map<String, String>
)

private val baseRawClient: okhttp3.OkHttpClient by lazy {
    DohDns.sharedBaseClient.newBuilder()
        .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
}

private val noRedirectClient: okhttp3.OkHttpClient by lazy {
    baseRawClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
}

suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
    maxResponseBodyBytes: Int = 10 * 1024 * 1024,
    bodyBytes: ByteArray? = null
): RawHttpResponse = withContext(Dispatchers.IO) {
    val normalizedMethod = method.uppercase()
    val builder = Request.Builder().url(url)
    headers.forEach { (key, value) ->
        if (!key.equals("Accept-Encoding", ignoreCase = true)) {
            builder.header(key, value)
        }
    }
    builder.removeHeader("Accept-Encoding")

    val request = if (normalizedMethod in listOf("POST", "PUT", "PATCH", "DELETE")) {
        val contentType = headers.entries.find { it.key.equals("Content-Type", ignoreCase = true) }?.value
            ?: if (normalizedMethod == "POST") "application/x-www-form-urlencoded" else "application/json"
        val requestBody = (bodyBytes ?: body.toByteArray(Charsets.UTF_8))
            .toRequestBody(contentType.toMediaType())
        builder.method(normalizedMethod, requestBody)
    } else {
        builder.method(normalizedMethod, null)
    }.build()

    val client = if (followRedirects) {
        baseRawClient
    } else {
        noRedirectClient
    }

    val call = client.newCall(request)
    val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
        if (cause is CancellationException) {
            call.cancel()
        }
    }
    try {
        call.execute().use { response ->
            val headersMap = response.headers.toMultimap().mapValues { (_, values) ->
                values.joinToString(",")
            }.mapKeys { (name, _) -> name.lowercase() }

            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val isMpegUrl = contentType.contains("mpegurl") || contentType.contains("application/x-mpegurl")
            val isBinaryVideo = (contentType.startsWith("video/") && !isMpegUrl) || (contentLength > 15 * 1024 * 1024L)

            if (isBinaryVideo) {
                // Binary video stream guard: close body without reading huge payload, return synthetic link
                val finalUrl = response.request.url.toString()
                val syntheticJson = "{\"url\":\"$finalUrl\",\"streamUrl\":\"$finalUrl\",\"synthetic\":true}"
                return@withContext RawHttpResponse(
                    status = response.code,
                    statusText = response.message,
                    url = finalUrl,
                    body = syntheticJson,
                    bodyBytes = syntheticJson.toByteArray(Charsets.UTF_8),
                    headers = headersMap
                )
            }

            val bodyStream = response.body?.byteStream()
            val responseBytes = if (bodyStream != null) {
                val maxBytes = maxResponseBodyBytes.coerceAtLeast(0)
                val out = java.io.ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
                val buffer = ByteArray(8 * 1024)
                var remaining = maxBytes
                while (remaining > 0) {
                    val read = bodyStream.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    remaining -= read
                }
                out.toByteArray()
            } else ByteArray(0)

            val decoded = String(responseBytes, Charsets.UTF_8)

            RawHttpResponse(
                status = response.code,
                statusText = response.message,
                url = response.request.url.toString(),
                body = decoded,
                bodyBytes = responseBytes,
                headers = headersMap
            )
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        RawHttpResponse(
            status = 0,
            statusText = e.message ?: "Request failed",
            url = url,
            body = "",
            bodyBytes = ByteArray(0),
            headers = emptyMap()
        )
    } finally {
        cancelHandle?.dispose()
    }
}
