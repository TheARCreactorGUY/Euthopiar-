package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.StreamEmission
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = BingrPlugin(client)

    println("Testing getStreamFlow('693134') for Dune: Part Two...")
    var count = 0
    withTimeoutOrNull(15000L) {
        plugin.getStreamFlow("693134").collect { emission ->
            if (emission is StreamEmission.SourceFound) {
                count++
                val src = emission.source
                println("  Source $count: ${src.serverName} | ${src.quality} | ${src.url}")
            }
        }
    }
    println("Total sources emitted: $count")
}
