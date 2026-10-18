import com.euthopiar.core.provider.CinejoyWasmEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

fun main() = runBlocking {
    val client = OkHttpClient()
    val rawId = "95350-lanterns-2026"
    val cleanId = "95350"
    val servers = listOf("Lisbon", "Nebula")

    println("=== Testing rawId '$rawId' as TV S1E1 ===")
    for (s in servers) {
        val res = try {
            CinejoyWasmEngine.requestStream(client, s, "tv", rawId, season = 1, episode = 1)
        } catch (e: Throwable) { e.toString() }
        println("  [$s]: $res")
    }

    println("\n=== Testing cleanId '$cleanId' as TV S1E1 ===")
    for (s in servers) {
        val res = try {
            CinejoyWasmEngine.requestStream(client, s, "tv", cleanId, season = 1, episode = 1)
        } catch (e: Throwable) { e.toString() }
        println("  [$s]: $res")
    }
}

