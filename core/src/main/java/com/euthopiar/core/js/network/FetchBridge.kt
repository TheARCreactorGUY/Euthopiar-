package com.euthopiar.core.js.network

import android.util.Base64
import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.euthopiar.core.js.host.HostModule
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val MAX_FETCH_HEADER_VALUE_CHARS = 8 * 1024

class FetchBridge : HostModule {
    private val json = Json { ignoreUnknownKeys = true }

    override fun register(runtime: QuickJs) {
        runtime.asyncFunction("__native_fetch") { args: Array<Any?> ->
            val url = args.getOrNull(0)?.toString() ?: ""
            val method = args.getOrNull(1)?.toString() ?: "GET"
            val headersJson = args.getOrNull(2)?.toString() ?: "{}"
            val bodyKind = args.getOrNull(3)?.toString() ?: "none"
            val body = args.getOrNull(4)?.toString() ?: ""
            val followRedirects = args.getOrNull(5) as? Boolean ?: true
            try {
                performNativeFetch(url, method, headersJson, bodyKind, body, followRedirects)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e("FetchBridge", "Fetch bridge error for $method $url", t)
                JsonObject(
                    mapOf(
                        "ok" to JsonPrimitive(false),
                        "status" to JsonPrimitive(0),
                        "statusText" to JsonPrimitive(t.message ?: "Fetch failed"),
                        "url" to JsonPrimitive(url),
                        "body" to JsonPrimitive(""),
                        "bodyBase64" to JsonPrimitive(""),
                        "headers" to JsonObject(emptyMap())
                    )
                ).toString()
            }
        }
    }

    private suspend fun performNativeFetch(
        url: String,
        method: String,
        headersJson: String,
        bodyKind: String,
        body: String,
        followRedirects: Boolean
    ): String {
        val headers = parseHeaders(headersJson).toMutableMap()
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        val response = httpRequestRaw(
            method = method,
            url = url,
            headers = headers,
            body = if (bodyKind == "text") body else "",
            followRedirects = followRedirects,
            bodyBytes = decodeBinaryBody(bodyKind, body)
        )

        val responseHeaders = response.headers.mapKeys { (key, _) -> key.lowercase() }
            .mapValues { (_, value) -> truncateString(value, MAX_FETCH_HEADER_VALUE_CHARS) }
        val result = JsonObject(
            mapOf(
                "ok" to JsonPrimitive(response.status in 200..299),
                "status" to JsonPrimitive(response.status),
                "url" to JsonPrimitive(response.url),
                "statusText" to JsonPrimitive(response.statusText),
                "body" to JsonPrimitive(response.body),
                "bodyBase64" to JsonPrimitive(encodeBase64(response.bodyBytes)),
                "headers" to JsonObject(responseHeaders.mapValues { JsonPrimitive(it.value) })
            )
        )
        return result.toString()
    }

    private fun decodeBinaryBody(bodyKind: String, body: String): ByteArray? = when (bodyKind) {
        "base64" -> Base64.decode(body, Base64.DEFAULT)
        "none", "text" -> null
        else -> null
    }

    private fun encodeBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun parseHeaders(headersJson: String): Map<String, String> {
        return runCatching {
            val element = json.parseToJsonElement(headersJson) as? JsonObject ?: return emptyMap()
            element.mapNotNull { (key, value) ->
                val str = value.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                key to str
            }.toMap()
        }.getOrElse { emptyMap() }
    }

    private fun truncateString(value: String, maxChars: Int): String {
        return if (value.length <= maxChars) value else value.take(maxChars) + "\n...[truncated]"
    }
}
