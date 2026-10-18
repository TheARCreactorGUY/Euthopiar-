package tools

import com.euthopiar.core.provider.OneShowsWasmEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    println("Testing with OkHttpClient with 4s timeout (similar to PluginHttp)...")
    val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    println("1. Dexter:")
    try {
        val dx = OneShowsWasmEngine.resolveDexterStreams(client, false, "634649", 1, 1)
        println("   Dexter count: ${dx.size}")
    } catch (e: Exception) {
        println("   Dexter error: ${e.message}")
        e.printStackTrace()
    }

    println("2. Vidzee:")
    try {
        val vz = OneShowsWasmEngine.decryptVidzeePayload(client, "JHd/O54eLA5vxsCtWd93Lje/bCkNsUmiXpP5HAUxlG1BGMmDoAioZO/ymBkQVwa4E7Q5vha1rXgWGo0OgPeLgpiLCZDwmI36lq/nYPeAcKSMTBq5TZnuqxhsP+1kRIQB5RaZn7+G/vzDT699carZ7/5Ut1dG/f1GlCpCPjTchVQ/CKKCqGG1xCm8eg==", "player.vidzee.wtf")
        println("   Vidzee dec: $vz")
    } catch (e: Exception) {
        println("   Vidzee error: ${e.message}")
        e.printStackTrace()
    }
}
