package com.euthopiar.core.js.host

import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.delay

private const val MAX_PLUGIN_TIMER_DELAY_MS = 60_000L

class HostFunctions(
    private val scraperId: String,
    private val scraperSettingsJson: String = "{}",
    private val callArgsJson: String = "{}",
    private val tmdbApiKey: String = "",
    private val onResult: (String) -> Unit
) : HostModule {

    override fun register(runtime: QuickJs) {
        runtime.asyncFunction("__plugin_sleep") { args: Array<Any?> ->
            val durationMs = (args.getOrNull(0) as? Number)
                ?.toLong()
                ?.coerceIn(0L, MAX_PLUGIN_TIMER_DELAY_MS)
                ?: 0L
            delay(durationMs)
            null
        }

        runtime.define("console") {
            function("log") { args ->
                Log.d("Plugin:$scraperId", args.joinToString(" ") { it?.toString() ?: "null" })
                null
            }
            function("error") { args ->
                Log.e("Plugin:$scraperId", args.joinToString(" ") { it?.toString() ?: "null" })
                null
            }
            function("warn") { args ->
                Log.w("Plugin:$scraperId", args.joinToString(" ") { it?.toString() ?: "null" })
                null
            }
            function("info") { args ->
                Log.i("Plugin:$scraperId", args.joinToString(" ") { it?.toString() ?: "null" })
                null
            }
            function("debug") { args ->
                Log.d("Plugin:$scraperId", args.joinToString(" ") { it?.toString() ?: "null" })
                null
            }
        }

        runtime.function("__get_scraper_id") { scraperId }
        runtime.function("__get_scraper_settings") { scraperSettingsJson }
        runtime.function("__get_tmdb_api_key") { tmdbApiKey }
        runtime.function("__get_call_args") { callArgsJson }
        runtime.function("__capture_result") { args ->
            onResult(args.getOrNull(0)?.toString() ?: "[]")
            null
        }

        runtime.asyncFunction("__host_browser_resolve") { args: Array<Any?> ->
            val resolver = com.euthopiar.core.browser.HeadlessWebResolver.getInstanceOrNull()
            if (resolver == null) {
                return@asyncFunction "{\"error\":\"HeadlessWebResolver not initialized\",\"sniffedUrls\":[],\"sniffedMedia\":[]}"
            }
            val firstArg = args.getOrNull(0)?.toString() ?: ""
            val request = try {
                if (firstArg.trim().startsWith("{")) {
                    val obj = org.json.JSONObject(firstArg)
                    val url = obj.optString("url", "")
                    val headersMap = mutableMapOf<String, String>()
                    val headersObj = obj.optJSONObject("headers")
                    if (headersObj != null) {
                        for (key in headersObj.keys()) {
                            headersMap[key] = headersObj.optString(key)
                        }
                    }
                    val userAgent = if (obj.has("userAgent") && !obj.isNull("userAgent")) obj.getString("userAgent") else null
                    val timeoutMs = obj.optLong("timeoutMs", 20000L)
                    val waitForSelector = if (obj.has("waitForSelector") && !obj.isNull("waitForSelector")) obj.getString("waitForSelector") else null
                    val waitForText = if (obj.has("waitForText") && !obj.isNull("waitForText")) obj.getString("waitForText") else null
                    val evaluateJs = if (obj.has("evaluateJsAfterLoad") && !obj.isNull("evaluateJsAfterLoad")) obj.getString("evaluateJsAfterLoad") else null
                    val sniffRegexStr = if (obj.has("urlSniffRegex") && !obj.isNull("urlSniffRegex")) obj.getString("urlSniffRegex") else null
                    val urlSniffRegex = sniffRegexStr?.let {
                        try { Regex(it, RegexOption.IGNORE_CASE) } catch (_: Exception) { null }
                    }
                    val stopOnFirstMediaMatch = obj.optBoolean("stopOnFirstMediaMatch", false)
                    val extraDelayAfterLoadMs = obj.optLong("extraDelayAfterLoadMs", 0L)
                    val autoClickPlaySelectors = obj.optBoolean("autoClickPlaySelectors", true)
                    val autoClickServers = obj.optBoolean("autoClickServers", false)
                    com.euthopiar.core.browser.BrowserResolveRequest(
                        url = url,
                        headers = headersMap,
                        userAgent = userAgent,
                        timeoutMs = timeoutMs,
                        waitForSelector = waitForSelector,
                        waitForText = waitForText,
                        evaluateJsAfterLoad = evaluateJs,
                        urlSniffRegex = urlSniffRegex,
                        stopOnFirstMediaMatch = stopOnFirstMediaMatch,
                        extraDelayAfterLoadMs = extraDelayAfterLoadMs,
                        autoClickPlaySelectors = autoClickPlaySelectors,
                        autoClickServers = autoClickServers
                    )
                } else {
                    com.euthopiar.core.browser.BrowserResolveRequest(url = firstArg)
                }
            } catch (e: Exception) {
                com.euthopiar.core.browser.BrowserResolveRequest(url = firstArg)
            }

            try {
                val res = resolver.resolve(request)
                val outJson = org.json.JSONObject().apply {
                    put("finalUrl", res.finalUrl)
                    put("html", res.html)
                    put("cookieString", res.cookieString)
                    val cookiesObj = org.json.JSONObject()
                    res.cookies.forEach { (k, v) -> cookiesObj.put(k, v) }
                    put("cookies", cookiesObj)
                    put("userAgent", res.userAgent)
                    val sniffedArr = org.json.JSONArray()
                    res.sniffedUrls.forEach { sniffedArr.put(it) }
                    put("sniffedUrls", sniffedArr)
                    val mediaArr = org.json.JSONArray()
                    res.sniffedMedia.forEach { m ->
                        val mObj = org.json.JSONObject().apply {
                            put("url", m.url)
                            val hObj = org.json.JSONObject()
                            m.headers.forEach { (hk, hv) -> hObj.put(hk, hv) }
                            put("headers", hObj)
                            if (m.mimeType != null) put("mimeType", m.mimeType) else put("mimeType", org.json.JSONObject.NULL)
                            put("isSubtitles", m.isSubtitles)
                        }
                        mediaArr.put(mObj)
                    }
                    put("sniffedMedia", mediaArr)
                    val subArr = org.json.JSONArray()
                    res.sniffedSubtitles.forEach { s ->
                        val sObj = org.json.JSONObject().apply {
                            put("url", s.url)
                            put("lang", s.lang)
                            put("name", s.name)
                        }
                        subArr.put(sObj)
                    }
                    put("sniffedSubtitles", subArr)
                    if (res.jsResult != null) put("jsResult", res.jsResult)
                }
                outJson.toString()
            } catch (e: Exception) {
                val errJson = org.json.JSONObject().apply {
                    put("error", e.message ?: "Browser resolve error")
                    put("finalUrl", request.url)
                    put("html", "")
                    put("cookieString", "")
                    put("cookies", org.json.JSONObject())
                    put("userAgent", "")
                    put("sniffedUrls", org.json.JSONArray())
                    put("sniffedMedia", org.json.JSONArray())
                    put("sniffedSubtitles", org.json.JSONArray())
                }
                errJson.toString()
            }
        }
    }
}
