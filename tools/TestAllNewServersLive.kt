package tools

import com.euthopiar.core.model.StreamEmission
import com.euthopiar.core.provider.OneShowsPlugin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    println("=== TESTING ALL 1SHOWS STREAM SOURCES FOR SPIDER-MAN (634649) ===")
    val plugin = OneShowsPlugin()
    val emissions = plugin.getStreamFlow("634649").toList()
    val sources = emissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }

    println("Total sources emitted: ${sources.size}")

    val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    for (s in sources) {
        println("\n-------------------------------------------------------------")
        println("Server: [${s.serverName}]")
        println("Quality: ${s.quality}")
        println("isM3u8: ${s.isM3u8}")
        println("Audio: ${s.audioTracks.map { "${it.languageName} (${it.isoCode})" }}")
        println("URL: ${s.url}")
        println("Headers: ${s.headers}")

        try {
            val reqB = Request.Builder()
                .url(s.url)
                .header("Range", "bytes=0-2048")
            s.headers.forEach { (k, v) -> reqB.header(k, v) }
            val req = reqB.build()
            client.newCall(req).execute().use { resp ->
                val code = resp.code
                val ct = resp.header("Content-Type") ?: "none"
                val bodyBytes = resp.body?.byteStream()?.readNBytes(512) ?: ByteArray(0)
                val bodySnippet = String(bodyBytes).replace("\n", " ")
                println("PROBE RESULT: HTTP $code | Content-Type: $ct")
                println("Preview: ${bodySnippet.take(150)}")
                if (code in 200..299 && (ct.contains("mpegurl") || ct.contains("octet-stream") || bodySnippet.startsWith("#EXTM3U") || ct.contains("video") || s.url.endsWith(".mkv") || s.url.endsWith(".mp4"))) {
                    println("--> STATUS: [PLAYABLE MEDIA]")
                } else if (bodySnippet.contains("<!DOCTYPE") || bodySnippet.contains("<html")) {
                    println("--> STATUS: [FATAL: HTML WEBPAGE - NOT PLAYABLE IN EXOPLAYER!]")
                } else {
                    println("--> STATUS: [UNPLAYABLE / ERROR]")
                }
            }
        } catch (e: Exception) {
            println("PROBE EXCEPTION: ${e.message}")
        }
    }
}
