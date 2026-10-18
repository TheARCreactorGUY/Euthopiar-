package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.StreamEmission
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.model.SubtitleTrack
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

data class ContentTestTarget(
    val name: String,
    val type: String,
    val targetData: String
)

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = BingrPlugin(client)

    val targets = listOf(
        // Movies
        ContentTestTarget("Dune: Part Two", "Movie", "693134"),
        ContentTestTarget("Inception", "Movie", "27205"),
        ContentTestTarget("Avengers: Endgame", "Movie", "299534"),
        ContentTestTarget("Interstellar", "Movie", "157336"),
        ContentTestTarget("Jawan", "Movie", "872585"),
        
        // TV Series
        ContentTestTarget("Breaking Bad (S1E1)", "TV Series", "1396:1:1"),
        ContentTestTarget("Stranger Things (S1E1)", "TV Series", "66732:1:1"),
        ContentTestTarget("Game of Thrones (S1E1)", "TV Series", "1399:1:1"),
        ContentTestTarget("The Boys (S1E1)", "TV Series", "76479:1:1"),
        
        // Anime
        ContentTestTarget("Attack on Titan (S1E1)", "Anime", "1429:1:1")
    )

    println("======================================================================")
    println("           BINGR PLUGIN - 10 CONTENT SOURCE AUDIT                    ")
    println("======================================================================\n")

    for ((index, item) in targets.withIndex()) {
        println("----------------------------------------------------------------------")
        println("[${index + 1}/10] ${item.name} (${item.type}) - Target: ${item.targetData}")
        println("----------------------------------------------------------------------")

        val streams = mutableListOf<StreamSource>()
        val subtitles = mutableListOf<SubtitleTrack>()

        try {
            withTimeoutOrNull(20000L) {
                plugin.getStreamFlow(item.targetData).collect { emission ->
                    when (emission) {
                        is StreamEmission.SourceFound -> streams.add(emission.source)
                        is StreamEmission.SubtitleFound -> subtitles.add(emission.track)
                        else -> {}
                    }
                }
            }
        } catch (t: Throwable) {
            println("  ERROR during getStreamFlow: ${t.message}")
        }

        println("  Total Sources Found: ${streams.size}")
        println("  Total Subtitles Found: ${subtitles.size}")

        if (streams.isEmpty()) {
            println("  [!] NO STREAMS RETURNED")
        } else {
            println("  Sources Breakdown:")
            streams.forEachIndexed { sIdx, src ->
                val audioSummary = if (src.audioTracks.isNotEmpty()) {
                    src.audioTracks.joinToString(", ") { "${it.languageName} (${it.codec})" }
                } else {
                    "None specified (${src.releaseType})"
                }
                println("    ${sIdx + 1}. Server: \"${src.serverName}\" | Res: ${src.resolutionLabel} | Quality: \"${src.quality}\" | M3U8: ${src.isM3u8}")
                println("       Audio Tracks: $audioSummary")
                println("       URL: ${src.url.take(80)}...")
            }
        }
        println()
    }
}
