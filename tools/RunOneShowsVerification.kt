package tools

import com.euthopiar.core.model.*
import com.euthopiar.core.provider.OneShowsPlugin
import com.euthopiar.eup.api.ContentType
import com.euthopiar.eup.api.MediaCard
import com.euthopiar.eup.api.PlayableTarget
import com.euthopiar.eup.api.StreamBundleEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    println("================================================================================")
    println("           EUP v2 VERIFICATION SUITE: 1SHOWS (Archetype A)")
    println("================================================================================")

    val plugin = OneShowsPlugin()

    var testsPassed = 0
    var testsFailed = 0

    fun assertTest(name: String, condition: Boolean, details: String = "") {
        if (condition) {
            println("  [PASS] $name $details")
            testsPassed++
        } else {
            println("  [FAIL] $name $details")
            testsFailed++
        }
    }

    // ─────────────────────── 1. Catalog Verification ───────────────────────
    println("\n[1/6] Auditing Home Catalog (getHomeCatalog)...")
    try {
        val rows = plugin.getHomeCatalog()
        assertTest("Catalog rows non-empty", rows.isNotEmpty(), "(${rows.size} rows)")
        for (row in rows.take(3)) {
            println("   Row: '${row.title}' -> ${row.items.size} items")
            assertTest("Row '${row.title}' has items", row.items.isNotEmpty())
        }
        val firstItem = rows.firstOrNull()?.items?.firstOrNull()
        if (firstItem != null) {
            println("   Sample item: '${firstItem.title}' (ID: ${firstItem.id}, Type: ${firstItem.type}, Poster: ${firstItem.posterUrl?.take(40)}...)")
            assertTest("Sample item has valid title", firstItem.title.isNotBlank())
            assertTest("Sample item has valid poster", !firstItem.posterUrl.isNullOrBlank())
        }
    } catch (e: Exception) {
        assertTest("Catalog fetch", false, "Exception: ${e.message}")
    }

    // ─────────────────────── 2. Search Verification ───────────────────────
    println("\n[2/6] Auditing Search (search)...")
    try {
        val movieResults = plugin.search("Fight Club")
        assertTest("Movie search ('Fight Club')", movieResults.isNotEmpty(), "(${movieResults.size} matches)")
        val fightClub = movieResults.firstOrNull()
        if (fightClub != null) {
            println("   Top movie result: '${fightClub.title}' (ID: ${fightClub.id}, Type: ${fightClub.type}, Year: ${fightClub.year})")
            assertTest("Fight Club title matches", fightClub.title.contains("Fight", ignoreCase = true))
        }

        val tvResults = plugin.search("Game of Thrones")
        assertTest("TV search ('Game of Thrones')", tvResults.isNotEmpty(), "(${tvResults.size} matches)")
        val got = tvResults.firstOrNull()
        if (got != null) {
            println("   Top TV result: '${got.title}' (ID: ${got.id}, Type: ${got.type}, Year: ${got.year})")
            assertTest("Game of Thrones type is TV_SERIES", got.type == MediaType.TV_SERIES)
        }
    } catch (e: Exception) {
        assertTest("Search query", false, "Exception: ${e.message}")
    }

    // ─────────────────────── 3. Media Details Verification ───────────────────────
    println("\n[3/6] Auditing Media Details (getDetails & details)...")
    try {
        // Movie details
        val movieItem = MediaItem(
            id = "550",
            title = "Fight Club",
            url = "https://www.1shows.org/movies/550",
            posterUrl = null,
            type = MediaType.MOVIE
        )
        val movieDetails = plugin.getDetails(movieItem)
        assertTest("Movie details ID matches", movieDetails.id == "550")
        assertTest("Movie synopsis populated", !movieDetails.synopsis.isNullOrBlank())
        assertTest("Movie cast populated", movieDetails.cast.isNotEmpty(), "(${movieDetails.cast.size} cast members)")
        assertTest("Movie logo resolved", !movieDetails.logoUrl.isNullOrBlank(), "Logo: ${movieDetails.logoUrl}")

        // TV details
        val tvItem = MediaItem(
            id = "1399",
            title = "Game of Thrones",
            url = "https://www.1shows.org/tv/1399",
            posterUrl = null,
            type = MediaType.TV_SERIES
        )
        val tvDetails = plugin.getDetails(tvItem)
        assertTest("TV details ID matches", tvDetails.id == "1399")
        assertTest("TV episodes populated", tvDetails.episodes.size > 1, "(${tvDetails.episodes.size} episodes)")
        assertTest("TV logo resolved", !tvDetails.logoUrl.isNullOrBlank(), "Logo: ${tvDetails.logoUrl}")
        val firstEp = tvDetails.episodes.firstOrNull()
        if (firstEp != null) {
            println("   Sample TV episode: S${firstEp.seasonNumber}E${firstEp.episodeNumber} - '${firstEp.title}' (data: ${firstEp.data})")
            assertTest("Episode data has eup URI", firstEp.data.startsWith("eup://1shows/"))
        }

        // :eup-api DetailsProvider contract
        val card = MediaCard(
            id = "1399",
            title = "Game of Thrones",
            type = ContentType.TV_SERIES
        )
        val eupDetails = plugin.details(card)
        assertTest("EUP Details seasons populated", eupDetails.seasons.isNotEmpty(), "(${eupDetails.seasons.size} seasons)")
    } catch (e: Exception) {
        assertTest("Media details", false, "Exception: ${e.message}")
    }

    // ─────────────────────── 4. Stream Resolution & Probing ───────────────────────
    println("\n[4/6] Auditing Stream Flow Resolution & Probing...")
    val discoveredStreams = mutableListOf<Pair<String, Map<String, String>>>()
    try {
        // Movie Stream Flow
        println("   Resolving Movie streams for Fight Club (550)...")
        val movieEmissions = plugin.getStreamFlow("550").toList()
        val movieSources = movieEmissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
        val movieSubs = movieEmissions.filterIsInstance<StreamEmission.SubtitleFound>().map { it.track }

        println("   Emitted ${movieSources.size} movie stream sources and ${movieSubs.size} subtitles")
        assertTest("Movie stream sources emitted", movieSources.isNotEmpty(), "(${movieSources.size} sources)")
        assertTest("Movie subtitles emitted", movieSubs.isNotEmpty(), "(${movieSubs.size} tracks)")

        for (s in movieSources) {
            println("     Stream: [${s.serverName}] ${s.quality} -> ${s.url.take(80)}...")
            discoveredStreams.add(s.url to s.headers)
        }

        // TV Stream Flow
        println("\n   Resolving TV Episode streams for Game of Thrones (1399:1:1)...")
        val tvEmissions = plugin.getStreamFlow("1399:1:1").toList()
        val tvSources = tvEmissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
        val tvSubs = tvEmissions.filterIsInstance<StreamEmission.SubtitleFound>().map { it.track }

        println("   Emitted ${tvSources.size} TV stream sources and ${tvSubs.size} subtitles")
        assertTest("TV stream sources emitted", tvSources.isNotEmpty(), "(${tvSources.size} sources)")
        assertTest("TV subtitles emitted", tvSubs.isNotEmpty(), "(${tvSubs.size} tracks)")

        for (s in tvSources) {
            println("     Stream: [${s.serverName}] ${s.quality} -> ${s.url.take(80)}...")
            discoveredStreams.add(s.url to s.headers)
        }

        // Modern :eup-api StreamResolver
        println("\n   Testing :eup-api StreamResolver.resolve()...")
        val eupEvents = plugin.resolve(PlayableTarget.Movie(tmdbId = 550, title = "Fight Club")).toList()
        val foundEvents = eupEvents.filterIsInstance<StreamBundleEvent.SourcesFound>()
        val totalEupSources = foundEvents.sumOf { it.sources.size }
        assertTest("EUP StreamResolver sources", totalEupSources > 0, "($totalEupSources sources)")
    } catch (e: Exception) {
        assertTest("Stream resolution", false, "Exception: ${e.message}")
    }

    // ─────────────────────── 5. Download Links Verification ───────────────────────
    println("\n[5/6] Auditing Download Links (getDownloadLinks)...")
    try {
        val downloads = plugin.getDownloadLinks("550")
        println("   Extracted ${downloads.size} download options for Fight Club")
        assertTest("Download options extracted", downloads.isNotEmpty(), "(${downloads.size} options)")
        for (d in downloads.take(5)) {
            println("     Download: [${d.source}] ${d.quality} (${d.size}) -> ${d.url.take(80)}...")
        }
    } catch (e: Exception) {
        assertTest("Download links", false, "Exception: ${e.message}")
    }

    // ─────────────────────── 6. Live Non-Destructive Stream Probing ───────────────────────
    println("\n[6/6] Non-Destructive Live Probing (Range: bytes=0-2048)...")
    var probeSuccessCount = 0
    val probeClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    for ((url, headers) in discoveredStreams.take(8)) {
        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-2048")
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            val req = reqBuilder.build()
            probeClient.newCall(req).execute().use { resp ->
                val code = resp.code
                val isLive = code in 200..299 || code == 206
                val ct = resp.header("Content-Type") ?: "unknown"
                println("   Probing: ${url.take(65)}... -> HTTP $code ($ct)")
                if (isLive) probeSuccessCount++
            }
        } catch (e: Exception) {
            println("   Probing failed for ${url.take(50)}: ${e.message}")
        }
    }
    assertTest("At least 1 live stream verified HTTP 200/206", probeSuccessCount > 0, "($probeSuccessCount successful)")

    // ─────────────────────── Summary ───────────────────────
    println("\n================================================================================")
    println("                VERIFICATION SUMMARY: $testsPassed PASSED, $testsFailed FAILED")
    println("================================================================================")
    if (testsFailed > 0) {
        System.exit(1)
    }
}
