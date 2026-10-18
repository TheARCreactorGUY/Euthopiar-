package tools

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val doh = DohDns.DEFAULT
    for (host in listOf("play.xpass.top", "core.vidzee.wtf", "player.vidzee.wtf", "api.viduki.net", "www.viduki.net")) {
        try {
            val ips = doh.lookup(host)
            println("DohDns for $host: $ips")
        } catch (e: Exception) {
            println("DohDns failed for $host: ${e.message}")
        }
    }
}
