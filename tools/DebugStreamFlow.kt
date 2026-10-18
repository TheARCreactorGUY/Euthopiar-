package tools

import com.euthopiar.core.provider.OneShowsPlugin
import com.euthopiar.core.model.StreamEmission
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("=== TESTING TV SHOW ONESHOWS STREAM FLOW (Loki S01E01 - 84958:1:1) ===")
    val plugin = OneShowsPlugin()
    var count = 0
    val start = System.currentTimeMillis()
    plugin.getStreamFlow("84958:1:1").collect { em ->
        val elapsed = System.currentTimeMillis() - start
        if (em is StreamEmission.SourceFound) {
            count++
            val s = em.source
            val langs = s.audioTracks.joinToString { it.languageName }
            println("[$elapsed ms] #$count: [${s.serverName}] -> ${s.quality} | Audio: [$langs] (${s.url.take(65)}...)")
        }
    }
    val totalTime = System.currentTimeMillis() - start
    println("=== FINISHED TV SHOW: Total $count sources in $totalTime ms ===")
}
