package com.euthopiar.core.js

import com.euthopiar.core.js.crypto.CryptoBridge
import com.euthopiar.core.js.dom.DomBridge
import com.euthopiar.core.js.host.HostApiRegistry
import com.euthopiar.core.js.host.HostFunctions
import com.euthopiar.core.js.network.FetchBridge
import com.euthopiar.core.js.network.UrlBridge
import com.euthopiar.core.js.wasm.WasmBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PluginRuntime {
    private val json = Json { ignoreUnknownKeys = true }
    private val scraperSemaphore = Semaphore(MAX_CONCURRENT_PLUGINS)
    private val activeRepoJobs = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<Job>>()

    fun registerRepoJob(repoId: String, job: Job) {
        if (repoId.isBlank()) return
        val list = activeRepoJobs.computeIfAbsent(repoId) { java.util.concurrent.CopyOnWriteArrayList() }
        list.add(job)
        job.invokeOnCompletion { list.remove(job) }
    }

    fun cancelJobsForRepo(repoId: String) {
        if (repoId.isBlank()) return
        activeRepoJobs.remove(repoId)?.forEach { job ->
            if (job.isActive) {
                job.cancel(CancellationException("Repository $repoId was deleted"))
            }
        }
    }

    suspend fun executePlugin(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null,
        scraperId: String = "plugin",
        scraperSettingsJson: String = "{}",
        imdbId: String? = null,
        absoluteEpisode: Int? = null,
        title: String? = null,
        year: Int? = null,
        repoId: String = ""
    ): List<PluginRuntimeResult> {
        return scraperSemaphore.withPermit {
            withContext(pluginDispatcher) {
                coroutineContext[Job]?.let { job ->
                    if (repoId.isNotBlank()) registerRepoJob(repoId, job)
                }
                withTimeout(PLUGIN_TIMEOUT_MS) {
                    executePluginInternal(
                        code = code,
                        tmdbId = tmdbId,
                        mediaType = mediaType,
                        season = season,
                        episode = episode,
                        scraperId = scraperId,
                        scraperSettingsJson = scraperSettingsJson,
                        imdbId = imdbId,
                        absoluteEpisode = absoluteEpisode,
                        title = title,
                        year = year,
                        repoId = repoId
                    )
                }
            }
        }
    }

    private suspend fun executePluginInternal(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        scraperId: String,
        scraperSettingsJson: String,
        imdbId: String? = null,
        absoluteEpisode: Int? = null,
        title: String? = null,
        year: Int? = null,
        repoId: String = ""
    ): List<PluginRuntimeResult> {
        val jsRuntime = JsRuntime()
        val deferred = CompletableDeferred<String>()
        val callArgsJson = JsonObject(
            mapOf(
                "tmdbId" to JsonPrimitive(tmdbId),
                "mediaType" to JsonPrimitive(mediaType),
                "season" to (season?.let(::JsonPrimitive) ?: JsonNull),
                "episode" to (episode?.let(::JsonPrimitive) ?: JsonNull),
                "imdbId" to (imdbId?.let(::JsonPrimitive) ?: JsonNull),
                "absoluteEpisode" to (absoluteEpisode?.let(::JsonPrimitive) ?: JsonNull),
                "title" to (title?.let(::JsonPrimitive) ?: JsonNull),
                "year" to (year?.let(::JsonPrimitive) ?: JsonNull),
                "id" to JsonPrimitive(tmdbId)
            )
        ).toString()

        val domBridge = DomBridge()
        val hostRegistry = HostApiRegistry().apply {
            addModule(
                HostFunctions(
                    scraperId = scraperId,
                    scraperSettingsJson = scraperSettingsJson,
                    callArgsJson = callArgsJson,
                    onResult = { deferred.complete(it) }
                )
            )
            addModule(FetchBridge())
            addModule(UrlBridge())
            addModule(CryptoBridge())
            addModule(WasmBridge())
            addModule(domBridge)
        }

        try {
            jsRuntime.use {
                hostRegistry.registerAll(this)
                evaluateCached({ JsRuntime.polyfillBytecode(this) }, JsBindings.staticPolyfillCode)
                evaluate<Any?>(wrapPluginModule(code))
                evaluateCached({ JsRuntime.callBytecode(this) }, JsBindings.staticCallCode)
                deferred.await()
            }
            return parseJsonResults(deferred.await(), scraperId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            android.util.Log.e("PluginRuntime", "Error executing plugin $scraperId: ${e.message}", e)
            return emptyList()
        } finally {
            domBridge.clear()
        }
    }

    private fun wrapPluginModule(code: String): String = """
        var module = { exports: {} };
        var exports = module.exports;
        (function() {
            $code
        })();
    """.trimIndent()

    private suspend fun com.dokar.quickjs.QuickJs.evaluateCached(
        bytecode: com.dokar.quickjs.QuickJs.() -> ByteArray,
        source: String
    ) {
        val compiled = runCatching { bytecode() }.getOrNull()
        if (compiled != null) {
            evaluate<Any?>(compiled)
        } else {
            evaluate<Any?>(source)
        }
    }

    fun parseJsonResults(rawJson: String, scraperId: String = "plugin"): List<PluginRuntimeResult> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val parsedElement = json.parseToJsonElement(rawJson)
            val jsonArray = when (parsedElement) {
                is JsonArray -> parsedElement
                is JsonObject -> {
                    (parsedElement["streams"] as? JsonArray)
                        ?: (parsedElement["results"] as? JsonArray)
                        ?: (parsedElement["data"] as? JsonArray)
                        ?: JsonArray(listOf(parsedElement))
                }
                else -> return emptyList()
            }

            jsonArray.mapNotNull { itemElement ->
                try {
                    val obj = itemElement as? JsonObject ?: return@mapNotNull null

                    // 1. Title Coalescing
                    val title = obj.string("title")
                        ?: obj.string("name")
                        ?: obj.string("label")
                        ?: obj.string("description")
                        ?: obj.string("filename")
                        ?: "$scraperId Stream"

                    val cleanTitle = title.trim()
                    val rawName = obj.string("name")?.trim()

                    // 2. Mutable headers & URL / infoHash extraction
                    val headersMap = mutableMapOf<String, String>()

                    // Top-level headers
                    (obj["headers"] as? JsonObject)?.forEach { (k, v) ->
                        (v as? JsonPrimitive)?.contentOrNull?.let { headersMap[k.trim()] = it.trim() }
                    }

                    // Top-level cookie
                    obj.string("cookie")?.let { cookieStr ->
                        if (cookieStr.isNotBlank()) headersMap["Cookie"] = cookieStr.trim()
                    }

                    // Check URL candidates: url, streamUrl, link, file, externalUrl, magnet, stream
                    var rawUrl: String? = null
                    val candidateFields = listOf("url", "streamUrl", "link", "file", "externalUrl", "magnet", "stream")
                    for (field in candidateFields) {
                        val valueElement = obj[field] ?: continue
                        when (valueElement) {
                            is JsonPrimitive -> {
                                val str = valueElement.contentOrNull?.trim()
                                if (!str.isNullOrBlank()) {
                                    rawUrl = str
                                    break
                                }
                            }
                            is JsonObject -> {
                                val nestedUrl = valueElement.string("url")
                                    ?: valueElement.string("link")
                                    ?: valueElement.string("file")
                                    ?: valueElement.string("streamUrl")
                                if (!nestedUrl.isNullOrBlank()) {
                                    rawUrl = nestedUrl.trim()
                                    (valueElement["headers"] as? JsonObject)?.forEach { (hk, hv) ->
                                        (hv as? JsonPrimitive)?.contentOrNull?.let { headersMap[hk.trim()] = it.trim() }
                                    }
                                    valueElement.string("cookie")?.let { cStr ->
                                        if (cStr.isNotBlank()) headersMap["Cookie"] = cStr.trim()
                                    }
                                    break
                                }
                            }
                            else -> {}
                        }
                    }

                    // Check infoHash
                    var infoHash = obj.string("infoHash")?.trim()

                    // Check clientResolve
                    val crObj = (obj["clientResolve"] as? JsonObject)
                        ?: ((obj["behaviorHints"] as? JsonObject)?.get("clientResolve") as? JsonObject)
                    if (crObj != null) {
                        val crInfoHash = crObj.string("infoHash")?.trim()
                        val crMagnet = (crObj.string("magnetUri") ?: crObj.string("magnet"))?.trim()
                        if (infoHash.isNullOrBlank() && !crInfoHash.isNullOrBlank()) {
                            infoHash = crInfoHash
                        }
                        if (rawUrl.isNullOrBlank() && !crMagnet.isNullOrBlank()) {
                            rawUrl = crMagnet
                        }
                    }

                    // Hoist behaviorHints proxyHeaders if present
                    val hintsObj = obj["behaviorHints"] as? JsonObject
                    val proxyHeaders = hintsObj?.get("proxyHeaders")
                    if (proxyHeaders is JsonObject) {
                        val reqHeaders = (proxyHeaders["request"] as? JsonObject) ?: proxyHeaders
                        reqHeaders.forEach { (k, v) ->
                            (v as? JsonPrimitive)?.contentOrNull?.let { headersMap[k.trim()] = it.trim() }
                        }
                    }

                    // If rawUrl is missing/blank, but infoHash is present: synthesize magnet
                    if (rawUrl.isNullOrBlank() && !infoHash.isNullOrBlank()) {
                        rawUrl = "magnet:?xt=urn:btih:$infoHash"
                    }

                    // Drop item ONLY if both URL and infoHash are missing
                    if (rawUrl.isNullOrBlank() && infoHash.isNullOrBlank()) {
                        return@mapNotNull null
                    }
                    val finalUrl = rawUrl ?: "magnet:?xt=urn:btih:$infoHash"

                    // 3. Seeders & Peers regex coercion
                    val descStr = obj.string("description").orEmpty()
                    val seeders = parseIntegerMetric(obj, "seeders", "seeds")
                        ?: extractRegexMetric(cleanTitle, """(?:👤|👥|seeds?:?|seeders?:?)\s*(\d+)""")
                        ?: extractRegexMetric(rawName.orEmpty(), """(?:👤|👥|seeds?:?|seeders?:?)\s*(\d+)""")
                        ?: extractRegexMetric(descStr, """(?:👤|👥|seeds?:?|seeders?:?)\s*(\d+)""")

                    val peers = parseIntegerMetric(obj, "peers", "leechers")
                        ?: extractRegexMetric(cleanTitle, """(?:leechers?:?|peers?:?)\s*(\d+)""")
                        ?: extractRegexMetric(rawName.orEmpty(), """(?:leechers?:?|peers?:?)\s*(\d+)""")
                        ?: extractRegexMetric(descStr, """(?:leechers?:?|peers?:?)\s*(\d+)""")

                    // 4. Resolution parsing & fallback
                    val explicitQuality = obj.string("quality") ?: obj.string("resolution") ?: obj.string("res")
                    val combinedText = "$cleanTitle ${rawName.orEmpty()} $descStr ${explicitQuality.orEmpty()} $finalUrl"
                    val inferredQuality = when {
                        Regex("""(?i)\b(4k|2160p?)\b""").containsMatchIn(combinedText) -> "4K"
                        Regex("""(?i)\b(1440p?|2k)\b""").containsMatchIn(combinedText) -> "1440p"
                        Regex("""(?i)\b(1080p?|fhd)\b""").containsMatchIn(combinedText) -> "1080p"
                        Regex("""(?i)\b(720p?|hd)\b""").containsMatchIn(combinedText) -> "720p"
                        Regex("""(?i)\b(480p?|sd)\b""").containsMatchIn(combinedText) -> "480p"
                        else -> null
                    }
                    val finalQuality = explicitQuality?.takeIf { it.isNotBlank() && !it.equals("Auto", ignoreCase = true) }
                        ?: inferredQuality
                        ?: "Auto"

                    // Size
                    val size = obj.string("size") ?: obj.string("fileSize")

                    // Language, provider, type
                    val language = obj.string("language") ?: obj.string("lang")
                    val provider = obj.string("provider") ?: scraperId
                    val type = obj.string("type")

                    // 5. Tolerant Subtitle Parsing
                    val subtitlesList = mutableListOf<PluginSubtitleResult>()
                    val rawSubs = obj["subtitles"] as? JsonArray
                    rawSubs?.forEach { subElem ->
                        try {
                            val subObj = subElem as? JsonObject ?: return@forEach
                            val subUrl = subObj.string("url") ?: subObj.string("file") ?: subObj.string("link") ?: return@forEach
                            if (subUrl.isBlank()) return@forEach
                            val subLang = subObj.string("language") ?: subObj.string("lang") ?: subObj.string("label") ?: "und"
                            val subName = subObj.string("name") ?: subObj.string("title")
                            val subHeaders = (subObj["headers"] as? JsonObject)?.mapNotNull { (sk, sv) ->
                                (sv as? JsonPrimitive)?.contentOrNull?.let { sk.trim() to it.trim() }
                            }?.toMap()
                            subtitlesList.add(
                                PluginSubtitleResult(
                                    url = subUrl.trim(),
                                    language = subLang.trim(),
                                    name = subName?.trim(),
                                    headers = subHeaders
                                )
                            )
                        } catch (_: Exception) {}
                    }

                    PluginRuntimeResult(
                        title = cleanTitle,
                        name = rawName,
                        url = finalUrl,
                        quality = finalQuality,
                        size = size?.trim(),
                        language = language?.trim(),
                        provider = provider,
                        type = type?.trim(),
                        seeders = seeders,
                        peers = peers,
                        infoHash = infoHash,
                        headers = if (headersMap.isNotEmpty()) headersMap else null,
                        subtitles = if (subtitlesList.isNotEmpty()) subtitlesList else null
                    )
                } catch (e: Exception) {
                    android.util.Log.w("PluginRuntime", "Skipping malformed stream item from $scraperId: ${e.message}")
                    null
                }
            }
        }.getOrElse { e ->
            android.util.Log.w("PluginRuntime", "Failed to parse JSON results from $scraperId: ${e.message}")
            emptyList()
        }
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun parseIntegerMetric(obj: JsonObject, vararg keys: String): Int? {
        for (k in keys) {
            val elem = obj[k] ?: continue
            if (elem is JsonPrimitive) {
                elem.intOrNull?.let { return it }
                val str = elem.contentOrNull ?: continue
                val num = Regex("""\b(\d+)\b""").find(str)?.groupValues?.get(1)?.toIntOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    private fun extractRegexMetric(text: String, regexPattern: String): Int? {
        if (text.isBlank()) return null
        return Regex(regexPattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
    }
}
