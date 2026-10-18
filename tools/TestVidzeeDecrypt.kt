package tools

import com.euthopiar.core.provider.OneShowsWasmEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

fun main() = runBlocking {
    val client = OkHttpClient()
    val servers = listOf("dcloud", "ipcloud", "v4:Hindi", "v4:English", "v6:Hindi", "v6:English")
    println("Testing all Vidzee servers live decryption...")
    for (s in servers) {
        val enc = URLEncoder.encode(s, "UTF-8")
        val req = Request.Builder()
            .url("https://core.vidzee.wtf/streams/movie/634649?s=$enc&e=1")
            .header("Referer", "https://player.vidzee.wtf/")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        val body = client.newCall(req).execute().use { it.body?.string().orEmpty() }
        val cMatch = Regex("\"c\":\\s*\"([^\"]+)\"").find(body)
        if (cMatch != null) {
            val c = cMatch.groupValues[1]
            val dec = OneShowsWasmEngine.decryptVidzeePayload(client, c, "player.vidzee.wtf")
            println("Server [$s] -> Decrypted: $dec")
        } else {
            println("Server [$s] -> No ciphertext found (body: $body)")
        }
    }
}
