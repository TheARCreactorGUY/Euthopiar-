package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.Request

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val url = "https://embed.vidrift.in/embed/movie/693134"
    val req = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .header("Referer", "https://bingr.one/")
        .build()

    val html = client.newCall(req).execute().use { resp ->
        resp.body?.string().orEmpty()
    }

    val metaIdx = html.indexOf("var embedMeta = ")
    println("metaIdx = $metaIdx")
    val startBrace = html.indexOf('{', metaIdx)
    var depth = 0
    var endBrace = -1
    for (i in startBrace until html.length) {
        val c = html[i]
        if (c == '{') depth++
        else if (c == '}') {
            depth--
            if (depth == 0) {
                endBrace = i + 1
                break
            }
        }
    }
    println("endBrace = $endBrace, length = ${endBrace - startBrace}")
    val rawJson = html.substring(startBrace, endBrace)
    try {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val root = json.parseToJsonElement(rawJson).jsonObject
        println("SUCCESS parsing JSON! Keys: ${root.keys}")
        val orion = root["orionStreams"]
        println("orionStreams: $orion")
    } catch (e: Throwable) {
        println("FAILED to parse JSON: ${e.message}")
        println("Last 100 chars of rawJson: ${rawJson.takeLast(100)}")
    }
}
