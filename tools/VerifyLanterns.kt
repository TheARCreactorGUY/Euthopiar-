import com.euthopiar.core.provider.CinejoyPlugin
import com.euthopiar.core.model.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

fun main() = runBlocking {
    println("=== Deep Verification of CinejoyPlugin for '95350-lanterns-2026' ===")
    val plugin = CinejoyPlugin()

    // 1. Details Test
    println("\n[1] Testing getDetails with ID '95350-lanterns-2026'...")
    val item = MediaItem(
        id = "95350-lanterns-2026",
        title = "Lanterns",
        url = "https://cinejoy.pk/tv/95350-lanterns-2026",
        posterUrl = null,
        type = MediaType.TV_SERIES
    )
    val details = try {
        plugin.getDetails(item)
    } catch (e: Throwable) {
        println("  getDetails error: ${e.message}")
        null
    }

    if (details != null) {
        println("  Title          : ${details.title}")
        println("  Clean ID       : ${details.id}")
        println("  Type           : ${details.type}")
        println("  Year           : ${details.year}")
        println("  Episodes Count : ${details.episodes.size}")
        for (ep in details.episodes.take(3)) {
            println("    * S${ep.seasonNumber}E${ep.episodeNumber}: '${ep.title}' (data: ${ep.data})")
        }
        println("  Recs Count     : ${details.recommendations.size}")
    }

    // 2. Streams Test with Raw Slug ID
    println("\n[2] Testing getStreamFlow with raw slug ID '95350-lanterns-2026'...")
    val rawEmissions = plugin.getStreamFlow("95350-lanterns-2026").toList()
    val rawSources = rawEmissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
    println("  Total Sources Discovered: ${rawSources.size}")
    for ((idx, s) in rawSources.withIndex()) {
        println("    #${idx + 1}: ${s.serverName} (${s.resolutionLabel}) -> ${s.url.take(65)}...")
    }

    // 3. Streams Test with Episode Data
    println("\n[3] Testing getStreamFlow with episode data '95350-lanterns-2026:1:1'...")
    val epEmissions = plugin.getStreamFlow("95350-lanterns-2026:1:1").toList()
    val epSources = epEmissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
    println("  Total Sources Discovered: ${epSources.size}")
    for ((idx, s) in epSources.withIndex()) {
        println("    #${idx + 1}: ${s.serverName} (${s.resolutionLabel}) -> ${s.url.take(65)}...")
    }

    // 4. Download Links Test
    println("\n[4] Testing getDownloadLinks with '95350-lanterns-2026'...")
    val downloads = plugin.getDownloadLinks("95350-lanterns-2026")
    println("  Total Download Options: ${downloads.size}")
    for ((idx, dl) in downloads.withIndex()) {
        println("    #${idx + 1}: ${dl.title} (${dl.quality}, ${dl.size}) -> ${dl.url.take(65)}...")
    }

    println("\n=== Verification Complete ===")
}
