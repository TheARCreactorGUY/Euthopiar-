package tools

import com.euthopiar.core.provider.OneShowsWasmEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

fun main() = runBlocking {
    val client = OkHttpClient()
    println("--- TESTING DEXTER DIRECTLY ---")
    try {
        val dxList = OneShowsWasmEngine.resolveDexterStreams(client, false, "634649", 1, 1)
        println("Dexter streams found: ${dxList.size}")
        for (dx in dxList) {
            println("  Dexter: [${dx.serverName}] -> ${dx.masterHlsUrl}")
        }
    } catch (e: Exception) {
        println("Dexter failed: ${e.message}")
        e.printStackTrace()
    }

    println("\n--- TESTING VIDZEE DIRECTLY ---")
    try {
        val vzBase = "https://core.vidzee.wtf"
        val vzHeaders = mapOf(
            "Referer" to "https://player.vidzee.wtf/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        )
        val langPath = "/streams/languages/movie/634649"
        val langReqBuilder = Request.Builder().url("$vzBase$langPath")
        vzHeaders.forEach { (k, v) -> langReqBuilder.header(k, v) }
        val langJson = client.newCall(langReqBuilder.build()).execute().use { it.body?.string().orEmpty() }
        println("Vidzee langJson: $langJson")

        val srvKey = "v4:Hindi"
        val encSrv = URLEncoder.encode(srvKey, "UTF-8")
        val streamPath = "/streams/movie/634649?s=$encSrv&e=1"
        val stReqBuilder = Request.Builder().url("$vzBase$streamPath")
        vzHeaders.forEach { (k, v) -> stReqBuilder.header(k, v) }
        val stResp = client.newCall(stReqBuilder.build()).execute().use { it.body?.string().orEmpty() }
        println("Vidzee stResp: $stResp")
        val cMatch = Regex("\"c\":\\s*\"([^\"]+)\"").find(stResp)
        if (cMatch != null) {
            val dec = OneShowsWasmEngine.decryptVidzeePayload(client, cMatch.groupValues[1], "player.vidzee.wtf")
            println("Vidzee decrypted: $dec")
        }
    } catch (e: Exception) {
        println("Vidzee failed: ${e.message}")
        e.printStackTrace()
    }
}
