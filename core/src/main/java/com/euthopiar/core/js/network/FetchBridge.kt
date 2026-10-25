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

    internal suspend fun performNativeFetch(
        url: String,
        method: String,
        headersJson: String,
        bodyKind: String,
        body: String,
        followRedirects: Boolean
    ): String {
        val isTmdb = url.contains("api.themoviedb.org/3", ignoreCase = true) ||
                url.contains("euthopiar-vault.sehgalvansh716.workers.dev/3", ignoreCase = true) ||
                url.contains("api.themoviedb.org", ignoreCase = true)

        val routedUrl = when {
            url.contains("api.themoviedb.org/3", ignoreCase = true) -> {
                url.replace(Regex("""^(?:https?:)?(?://)?api\.themoviedb\.org/3""", RegexOption.IGNORE_CASE), com.euthopiar.core.util.TmdbBridge.BASE_URL)
            }
            url.contains("euthopiar-vault.sehgalvansh716.workers.dev/3", ignoreCase = true) -> {
                url.replace(Regex("""^(?:https?:)?(?://)?euthopiar-vault\.sehgalvansh716\.workers\.dev/3""", RegexOption.IGNORE_CASE), com.euthopiar.core.util.TmdbBridge.BASE_URL)
            }
            else -> url
        }

        val effectiveUrl = if (isTmdb) {
            val key = com.euthopiar.core.util.TmdbBridge.DEFAULT_API_KEY
            if (key.isNotBlank()) {
                when {
                    !routedUrl.contains("api_key=", ignoreCase = true) -> {
                        val sep = if (routedUrl.contains("?")) "&" else "?"
                        "$routedUrl${sep}api_key=$key"
                    }
                    Regex("""api_key=(?:undefined|null|)(?:&|$)""", RegexOption.IGNORE_CASE).containsMatchIn(routedUrl) -> {
                        routedUrl.replace(Regex("""api_key=(?:undefined|null|)(?:&|$)""", RegexOption.IGNORE_CASE), "api_key=$key&").trimEnd('&')
                    }
                    else -> routedUrl
                }
            } else {
                if (Regex("""[?&]api_key=(?:undefined|null|)(?:&|$)""", RegexOption.IGNORE_CASE).containsMatchIn(routedUrl)) {
                    routedUrl.replace(Regex("""([?&])api_key=(?:undefined|null|)(?:&|$)""", RegexOption.IGNORE_CASE)) { mr ->
                        if (mr.groupValues[1] == "?") {
                            if (mr.value.endsWith("&")) "?" else ""
                        } else {
                            if (mr.value.endsWith("&")) "&" else ""
                        }
                    }.trimEnd('?', '&')
                } else {
                    routedUrl
                }
            }
        } else {
            routedUrl
        }

        val tmdbMatch = if (method.equals("GET", ignoreCase = true)) {
            Regex("""^https?://(?:api\.themoviedb\.org|euthopiar-vault\.sehgalvansh716\.workers\.dev)/3/(movie|tv)/(\d+)(?:/(external_ids))?/?(?:\?.*)?$""", RegexOption.IGNORE_CASE).find(effectiveUrl)
        } else null

        val tmdbFindMatch = if (method.equals("GET", ignoreCase = true)) {
            Regex("""^https?://(?:api\.themoviedb\.org|euthopiar-vault\.sehgalvansh716\.workers\.dev)/3/find/([^/?]+)/?(?:\?.*)?$""", RegexOption.IGNORE_CASE).find(effectiveUrl)
        } else null

        val isExternalIds = tmdbMatch?.groupValues?.getOrNull(3).equals("external_ids", ignoreCase = true)
        val hasAppendToResponse = effectiveUrl.contains("append_to_response=", ignoreCase = true)

        if (tmdbMatch != null && (isExternalIds || !hasAppendToResponse)) {
            val endpointType = tmdbMatch.groupValues[1].lowercase()
            val tmdbId = tmdbMatch.groupValues[2]
            val isTv = endpointType == "tv"
            val cachedJson = if (isExternalIds) {
                com.euthopiar.core.util.TmdbBridge.getCachedTmdbExternalIdsJson(tmdbId, isTv)
            } else {
                com.euthopiar.core.util.TmdbBridge.getCachedTmdbDetailsJson(tmdbId, isTv)
            }
            if (cachedJson != null) {
                val responseHeaders = mapOf(
                    "content-type" to JsonPrimitive("application/json;charset=utf-8"),
                    "cache-control" to JsonPrimitive("public, max-age=86400")
                )
                return JsonObject(
                    mapOf(
                        "ok" to JsonPrimitive(true),
                        "status" to JsonPrimitive(200),
                        "url" to JsonPrimitive(effectiveUrl),
                        "statusText" to JsonPrimitive("OK"),
                        "body" to JsonPrimitive(cachedJson),
                        "bodyBase64" to JsonPrimitive(encodeBase64(cachedJson.toByteArray(Charsets.UTF_8))),
                        "headers" to JsonObject(responseHeaders)
                    )
                ).toString()
            }
        }

        val headers = parseHeaders(headersJson).toMutableMap()
        if (headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            headers["User-Agent"] = com.euthopiar.core.util.TmdbBridge.DEFAULT_USER_AGENT
        }

        val response = httpRequestRaw(
            method = method,
            url = effectiveUrl,
            headers = headers,
            body = if (bodyKind == "text") body else "",
            followRedirects = followRedirects,
            bodyBytes = decodeBinaryBody(bodyKind, body)
        )

        if (isTmdb && response.status in 200..299 && response.body.isNotBlank()) {
            if (tmdbMatch != null) {
                val endpointType = tmdbMatch.groupValues[1].lowercase()
                val tmdbId = tmdbMatch.groupValues[2]
                val isTv = endpointType == "tv"
                if (isExternalIds) {
                    com.euthopiar.core.util.TmdbBridge.cacheTmdbExternalIdsJson(endpointType, tmdbId, response.body)
                } else {
                    com.euthopiar.core.util.TmdbBridge.cacheTmdbDetailsJson(endpointType, tmdbId, response.body)
                }
                try {
                    val root = org.json.JSONObject(response.body)
                    val imdbId = root.optString("imdb_id").takeIf { it.startsWith("tt") }
                        ?: root.optJSONObject("external_ids")?.optString("imdb_id")?.takeIf { it.startsWith("tt") }
                    if (imdbId != null) {
                        com.euthopiar.core.util.TmdbBridge.recordIdMapping(tmdbId, imdbId, isTv)
                    }
                    val extObj = root.optJSONObject("external_ids")
                    if (extObj != null) {
                        com.euthopiar.core.util.TmdbBridge.cacheTmdbExternalIdsJson(endpointType, tmdbId, extObj.toString())
                    }
                } catch (_: Exception) {}
            } else if (tmdbFindMatch != null) {
                try {
                    val findTarget = tmdbFindMatch.groupValues[1]
                    val root = org.json.JSONObject(response.body)
                    val movieResults = root.optJSONArray("movie_results")
                    val tvResults = root.optJSONArray("tv_results")
                    val firstMovieId = movieResults?.optJSONObject(0)?.optInt("id")?.toString()?.takeIf { it != "0" }
                    val firstTvId = tvResults?.optJSONObject(0)?.optInt("id")?.toString()?.takeIf { it != "0" }
                    if (firstMovieId != null && findTarget.startsWith("tt")) {
                        com.euthopiar.core.util.TmdbBridge.recordIdMapping(firstMovieId, findTarget, false)
                    }
                    if (firstTvId != null && findTarget.startsWith("tt")) {
                        com.euthopiar.core.util.TmdbBridge.recordIdMapping(firstTvId, findTarget, true)
                    }
                } catch (_: Exception) {}
            }
        }

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
        "base64" -> try {
            Base64.decode(body, Base64.DEFAULT)
        } catch (_: Throwable) {
            runCatching { java.util.Base64.getDecoder().decode(body) }.getOrNull()
        }
        "none", "text" -> null
        else -> null
    }

    private fun encodeBase64(bytes: ByteArray): String = try {
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Throwable) {
        java.util.Base64.getEncoder().encodeToString(bytes)
    }

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
