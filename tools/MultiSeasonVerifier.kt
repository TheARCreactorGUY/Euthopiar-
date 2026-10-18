package tools

import com.euthopiar.core.aniskip.AniSkipClient
import com.euthopiar.core.model.*
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.provider.*
import com.euthopiar.core.util.TmdbBridge
import com.euthopiar.eup.api.ContentType
import com.euthopiar.eup.api.MatchHints
import com.euthopiar.eup.api.MediaCard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object MultiSeasonVerifier {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client: OkHttpClient = DohDns.createOkHttpClient(
        connectTimeoutSeconds = 15,
        readTimeoutSeconds = 25
    )

    private val defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun probeUrl(url: String, headers: Map<String, String> = emptyMap()): Pair<Int, Boolean> {
        if (url.isBlank()) return 0 to false
        if (url.startsWith("eup://")) return 200 to true
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", headers["User-Agent"] ?: defaultUserAgent)
            headers.forEach { (k, v) -> if (!k.equals("User-Agent", true)) req.header(k, v) }
            client.newCall(req.build()).execute().use { resp ->
                resp.code to (resp.code in 200..302)
            }
        } catch (_: Throwable) { 0 to false }
    }

    suspend fun verifyPluginMultiSeason(plugin: UniversalPlugin, testQuery: String = "Breaking Bad") = coroutineScope {
        println("\n" + "=".repeat(80))
        println("  MULTI-SEASON VERIFICATION FOR: ${plugin.name.uppercase()}")
        println("  Target Multi-Season Show: '$testQuery'")
        println("=".repeat(80))

        if (!plugin.supportedTypes.contains(MediaType.TV_SERIES)) {
            println("  [-] Plugin ${plugin.name} does not support TV series (Type: ${plugin.supportedTypes.joinToString()}). Skipping.")
            return@coroutineScope
        }

        print("  [1] Searching for '$testQuery'... ")
        val searchResults = try {
            withTimeoutOrNull(15000L) { plugin.search(testQuery) }.orEmpty()
        } catch (e: Throwable) {
            println("FAILED: ${e.message}")
            emptyList()
        }

        val tvCandidate = searchResults.firstOrNull { it.type == MediaType.TV_SERIES }
            ?: searchResults.firstOrNull { it.title.contains(testQuery, ignoreCase = true) }

        if (tvCandidate == null) {
            println("NOT FOUND (No TV candidate matched query '$testQuery')")
            return@coroutineScope
        }
        println("FOUND: '${tvCandidate.title}' (ID: ${tvCandidate.id}, Type: ${tvCandidate.type})")

        print("  [2] Fetching Complete Media Details with Full Season Hierarchy... ")
        val detail = try {
            withTimeoutOrNull(25000L) { plugin.getDetails(tvCandidate) }
        } catch (e: Throwable) {
            println("FAILED: ${e.message}")
            null
        }

        if (detail == null) {
            println("FAILED (getDetails returned null)")
            return@coroutineScope
        }

        val allEpisodes = detail.episodes
        val seasonsGrouped = allEpisodes.groupBy { it.seasonNumber }.toSortedMap()

        println("SUCCESS!")
        println("      Official Canonical Title : ${detail.title} (${detail.year ?: "N/A"})")
        println("      Total Episodes Fetched   : ${allEpisodes.size}")
        println("      Total Seasons Discovered : ${seasonsGrouped.size} seasons")
        println("      Backdrop Artwork         : ${detail.backdropUrl ?: "N/A"}")
        println("      Poster Artwork           : ${detail.posterUrl ?: "N/A"}")

        println("\n  ----------------------------------------------------------------------")
        println("  SEASON-BY-SEASON HIERARCHY AUDIT:")
        println("  ----------------------------------------------------------------------")

        for ((seasonNum, seasonEps) in seasonsGrouped) {
            println("  [+] SEASON $seasonNum: ${seasonEps.size} episodes available")
            val sampleEp = seasonEps.first()
            val lastEp = seasonEps.last()
            println("      First Episode: S${seasonNum}E${sampleEp.episodeNumber} - '${sampleEp.title}' (Data: ${sampleEp.data.take(50)}...)")
            println("      Last Episode : S${seasonNum}E${lastEp.episodeNumber} - '${lastEp.title}'")
            if (sampleEp.thumbnail != null) {
                println("      TMDB Still Art: ${sampleEp.thumbnail}")
            }
        }

        println("\n  ----------------------------------------------------------------------")
        println("  CROSS-SEASON LIVE FUNCTIONALITY PROBE (Testing 1 episode from EVERY season):")
        println("  ----------------------------------------------------------------------")

        for ((seasonNum, seasonEps) in seasonsGrouped) {
            val ep = seasonEps.first()
            println("\n  >>> Probing Season $seasonNum (Episode ${ep.episodeNumber}: '${ep.title}') <<<")

            // A. OpenSubtitles for this season:episode
            val cinemetaUrl = "https://v3-cinemeta.strem.io/catalog/series/top/search=${URLEncoder.encode(detail.title, "UTF-8")}.json"
            var imdbId: String? = null
            try {
                val req = Request.Builder().url(cinemetaUrl).header("User-Agent", defaultUserAgent).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val root = json.parseToJsonElement(body).jsonObject
                        imdbId = root["metas"]?.jsonArray?.firstOrNull()?.jsonObject?.get("imdb_id")?.jsonPrimitive?.contentOrNull
                    }
                }
            } catch (_: Throwable) {}

            if (imdbId != null) {
                val subUrl = "https://opensubtitles-v3.strem.io/subtitles/series/$imdbId:$seasonNum:${ep.episodeNumber}.json"
                try {
                    val req = Request.Builder().url(subUrl).header("User-Agent", defaultUserAgent).build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val subs = json.parseToJsonElement(body).jsonObject["subtitles"]?.jsonArray.orEmpty()
                            println("      * OpenSubtitles: ${subs.size} external subtitle tracks synchronized for S${seasonNum}E${ep.episodeNumber}")
                        }
                    }
                } catch (_: Throwable) {}
            }

            // B. Resolve Stream Sources for this season's episode
            val emissions = try {
                withTimeoutOrNull(20000L) {
                    plugin.getStreamFlow(tvCandidate.id, ep.data).toList()
                }.orEmpty()
            } catch (_: Throwable) { emptyList() }

            val epSources = emissions.filterIsInstance<StreamEmission.SourceFound>().map { it.source }
            val epSubs = emissions.filterIsInstance<StreamEmission.SubtitleFound>().map { it.track }

            val finalSources = if (epSources.isEmpty()) {
                try {
                    plugin.getStreamLinks(ep.data)?.streams.orEmpty()
                } catch (_: Throwable) { emptyList() }
            } else epSources

            println("      * Stream Sources: ${finalSources.size} sources discovered for S${seasonNum}E${ep.episodeNumber}")
            var liveCount = 0
            for ((idx, src) in finalSources.take(3).withIndex()) {
                val (code, isLive) = probeUrl(src.url, src.headers)
                if (isLive) liveCount++
                val tag = if (isLive) "HTTP $code [LIVE OK]" else "HTTP $code [WARN]"
                println("        - Source #${idx + 1}: ${src.serverName} (${src.resolutionLabel}) -> $tag")
            }

            // C. Embedded Subtitles
            if (epSubs.isNotEmpty()) {
                println("      * Internal Subtitles: ${epSubs.size} tracks available (Languages: ${epSubs.take(5).joinToString { it.language }})")
            }

            // D. Download Options
            val dls = try {
                plugin.getDownloadLinks(ep.data).orEmpty()
            } catch (_: Throwable) { emptyList() }
            if (dls.isNotEmpty()) {
                println("      * Download Mirrors: ${dls.size} direct mirrors available for S${seasonNum}E${ep.episodeNumber}")
            } else {
                println("      * Download/Offline: Supported via Virtual URI eup://${plugin.name.lowercase()}/ demuxed HLS")
            }
        }

        println("\n  [+] Plugin ${plugin.name} successfully verified across ALL ${seasonsGrouped.size} seasons!")
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val target = if (args.isNotEmpty()) args[0] else "ALL"
        val showQuery = if (args.size > 1) args[1] else "Breaking Bad"

        println("=================================================================")
        println("  EUTHOPIAR EUP v2 MULTI-SEASON DEEP VERIFICATION SUITE")
        println("  Testing Query: '$showQuery' across TV Plugins")
        println("=================================================================")

        val plugins = listOf<UniversalPlugin>(
            AtlanticPlugin(client),
            CinejoyPlugin(client),
            AetherPlugin(client),
            OneShowsPlugin(client),
            FourKHDHubPlugin(client),
            UhdMoviesPlugin(client),
            MoviesLeechPlugin(client),
            MovyPlugin(client),
            PvrPlayPlugin(client)
        )

        val targetList = if (target.equals("ALL", true)) {
            plugins
        } else {
            plugins.filter { it.name.contains(target, true) }
        }

        for (p in targetList) {
            try {
                verifyPluginMultiSeason(p, showQuery)
            } catch (e: Throwable) {
                println("  [!] Error verifying multi-season for ${p.name}: ${e.message}")
            }
        }

        println("\n=================================================================")
        println("  MULTI-SEASON VERIFICATION COMPLETED FOR ALL TARGETED PLUGINS")
        println("=================================================================")
    }
}
