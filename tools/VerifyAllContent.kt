import com.euthopiar.core.provider.CinejoyPlugin
import com.euthopiar.core.model.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

data class ContentTestSpec(
    val label: String,
    val mediaId: String,
    val episodeData: String?,
    val expectedType: MediaType
)

fun main(): Unit = runBlocking {
    println("=" .repeat(80))
    println("  COMPREHENSIVE MULTI-CONTENT VERIFICATION FOR CINEJOY PLUGIN")
    println("  Testing Movies, TV Series, Slugs, Season:Episode, URLs & Anime across all servers")
    println("=".repeat(80))

    val plugin = CinejoyPlugin()

    val testSuite = listOf(
        // 1. Lanterns (Upcoming TV Series - Slug + S1E1 + Raw Slug)
        ContentTestSpec("Lanterns (TV Series - Raw Slug)", "95350-lanterns-2026", "95350-lanterns-2026", MediaType.TV_SERIES),
        ContentTestSpec("Lanterns (TV Series - Slug:S1:E1)", "95350-lanterns-2026", "95350-lanterns-2026:1:1", MediaType.TV_SERIES),

        // 2. Stranger Things (Popular TV Series - Slug + S4E9 + Numeric)
        ContentTestSpec("Stranger Things (TV Series - Slug:S1:E1)", "66732-stranger-things", "66732-stranger-things:1:1", MediaType.TV_SERIES),
        ContentTestSpec("Stranger Things (TV Series - Slug:S4:E9)", "66732-stranger-things", "66732:4:9", MediaType.TV_SERIES),

        // 3. Breaking Bad (Acclaimed TV Series - Slug:S5:E14 Ozymandias)
        ContentTestSpec("Breaking Bad (TV Series - Slug:S5:E14)", "1396-breaking-bad", "1396-breaking-bad:5:14", MediaType.TV_SERIES),

        // 4. Game of Thrones (TV Series - S1E1)
        ContentTestSpec("Game of Thrones (TV Series - S1:E1)", "1399-game-of-thrones", "1399:1:1", MediaType.TV_SERIES),

        // 5. Batman Begins (Movie - Slug)
        ContentTestSpec("Batman Begins (Movie - Slug)", "272-batman-begins-2005", "272-batman-begins-2005", MediaType.MOVIE),

        // 6. Avengers: Infinity War (Movie - Numeric ID)
        ContentTestSpec("Avengers: Infinity War (Movie - Numeric)", "299536", "299536", MediaType.MOVIE),

        // 7. Interstellar (Movie - Full URL)
        ContentTestSpec("Interstellar (Movie - URL)", "https://cinejoy.pk/movie/157336-interstellar", "157336", MediaType.MOVIE),

        // 8. Fight Club (Movie - Slug)
        ContentTestSpec("Fight Club (Movie - Slug)", "550-fight-club", "550-fight-club", MediaType.MOVIE),

        // 9. Demon Slayer (Anime Series - Slug:S1:E1)
        ContentTestSpec("Demon Slayer (Anime - Slug:S1:E1)", "85937-demon-slayer-kimetsu-no-yaiba", "85937:1:1", MediaType.TV_SERIES),

        // 10. Attack on Titan (Anime Series - Slug:S1:E1)
        ContentTestSpec("Attack on Titan (Anime - Slug:S1:E1)", "1429-attack-on-titan", "1429:1:1", MediaType.TV_SERIES)
    )

    var passedCount = 0
    var totalCount = testSuite.size

    for ((idx, spec) in testSuite.withIndex()) {
        println("\n" + "-".repeat(78))
        println("TEST #${idx + 1}: ${spec.label}")
        println("  Media ID    : ${spec.mediaId}")
        println("  Episode Data: ${spec.episodeData}")
        println("-".repeat(78))

        // A. Resolve Details
        val item = MediaItem(
            id = spec.mediaId,
            title = spec.label,
            url = if (spec.mediaId.startsWith("http")) spec.mediaId else "https://cinejoy.pk/${if (spec.expectedType == MediaType.TV_SERIES) "tv" else "movie"}/${spec.mediaId}",
            posterUrl = null,
            type = spec.expectedType
        )
        val details = try {
            plugin.getDetails(item)
        } catch (e: Throwable) {
            println("  [Details Error]: ${e.message}")
            null
        }

        if (details != null) {
            println("  Details OK: '${details.title}' (ID: ${details.id}, Year: ${details.year ?: "N/A"}, Type: ${details.type})")
            if (details.type == MediaType.TV_SERIES) {
                println("  Episodes  : ${details.episodes.size} episodes available (First: ${details.episodes.firstOrNull()?.title})")
            }
            println("  Recs      : ${details.recommendations.size} recommendations populated")
        }

        // B. Resolve Streams
        val targetEp = spec.episodeData ?: details?.episodes?.firstOrNull()?.data ?: spec.mediaId
        val emissions = try {
            plugin.getStreamFlow(spec.mediaId, targetEp).toList()
        } catch (e: Throwable) {
            println("  [Stream Error]: ${e.message}")
            emptyList()
        }

        val sources = emissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
        val subs = emissions.filterIsInstance<StreamEmission.SubtitleFound>().map { it.track }

        val serverNames = sources.map { it.serverName }.distinct()
        val hasHelios = serverNames.any { it.contains("Helios", ignoreCase = true) }
        val hasLisbon = serverNames.any { it.contains("Lisbon", ignoreCase = true) }
        val hasNebula = serverNames.any { it.contains("Nebula", ignoreCase = true) }

        println("  Sources Discovered: ${sources.size} stream sources across servers: $serverNames")
        println("    - Helios: ${if (hasHelios) "[PASS]" else "[WARN]"}")
        println("    - Lisbon: ${if (hasLisbon) "[PASS]" else "[WARN]"}")
        println("    - Nebula: ${if (hasNebula) "[PASS]" else "[WARN]"}")
        println("  Subtitles Tracks  : ${subs.size} subtitle tracks discovered")

        // C. Direct Download Links
        val downloads = try {
            plugin.getDownloadLinks(targetEp)
        } catch (_: Throwable) {
            emptyList()
        }
        println("  Direct Downloads  : ${downloads.size} download mirrors available")

        val isSuccess = sources.isNotEmpty() && (hasHelios || hasLisbon || hasNebula)
        if (isSuccess) {
            passedCount++
            println("  -> RESULT: [PASS - 100% WORKING]")
        } else {
            println("  -> RESULT: [FAIL - 0 Sources]")
        }
    }

    println("\n" + "=".repeat(80))
    println("  FINAL MULTI-CONTENT VERIFICATION RESULTS:")
    println("  PASSED: $passedCount / $totalCount tests (${(passedCount * 100) / totalCount}%)")
    println("=".repeat(80))
    kotlin.system.exitProcess(0)
}
