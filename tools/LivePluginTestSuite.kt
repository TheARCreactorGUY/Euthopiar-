package tools

import com.euthopiar.core.model.*
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.provider.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class PluginTestSummary(
    val pluginName: String,
    val isVault: Boolean,
    val catalogRowsCount: Int,
    val totalItemsTested: Int,
    val detailsSuccessCount: Int,
    val streamResolvedCount: Int,
    val totalStreamsExtracted: Int,
    val totalSubtitlesExtracted: Int,
    val downloadsExtractedCount: Int,
    val failures: List<String>
)

object LivePluginTestSuite {

    private val client: OkHttpClient = DohDns.createOkHttpClient(
        connectTimeoutSeconds = 15,
        readTimeoutSeconds = 25
    )

    private val searchQueries = listOf(
        "Batman", "Deadpool", "Inception", "Spider", "Avengers",
        "Breaking Bad", "Game of Thrones", "Stranger Things", "Dune", "Avatar"
    )

    private val vaultQueries = listOf(
        "massage", "amateur", "beach", "office", "party",
        "vacation", "secret", "model", "fitness", "college"
    )

    suspend fun testPlugin(plugin: UniversalPlugin, isVault: Boolean): PluginTestSummary = coroutineScope {
        println("\n=======================================================")
        println(">>> TESTING PLUGIN: ${plugin.name} (Vault: $isVault)")
        println("=======================================================")

        val failures = mutableListOf<String>()
        val candidateItems = mutableListOf<MediaItem>()

        // 1. Test getHomeCatalog()
        var catalogRowsCount = 0
        try {
            print("  [1/4] Fetching getHomeCatalog()... ")
            val rows = withTimeoutOrNull(25000L) { plugin.getHomeCatalog() }.orEmpty()
            catalogRowsCount = rows.size
            println("OK ($catalogRowsCount rows found)")
            for (row in rows) {
                candidateItems.addAll(row.items)
            }
        } catch (e: Throwable) {
            println("FAILED: ${e.message}")
            failures.add("getHomeCatalog error: ${e.message}")
        }

        // 2. Test search() to gather additional items if catalog had < 20
        print("  [2/4] Executing searches to collect up to 20 candidate items... ")
        val queryList = if (isVault) vaultQueries else searchQueries
        for (q in queryList) {
            if (candidateItems.distinctBy { it.id }.size >= 25) break
            try {
                val results = withTimeoutOrNull(15000L) { plugin.search(q) }.orEmpty()
                candidateItems.addAll(results)
            } catch (e: Throwable) {
                // search error recorded as minor note
            }
        }

        val testItems = candidateItems.distinctBy { it.id }.take(20)
        println("OK (Collected ${testItems.size} distinct test items)")

        // 3. Test getDetails() and Streams for each of the 20 items
        println("  [3/4] Testing getDetails() and Streams across ${testItems.size} items (concurrency = 4)...")
        val detailsSuccess = AtomicInteger(0)
        val streamResolved = AtomicInteger(0)
        val totalStreams = AtomicInteger(0)
        val totalSubs = AtomicInteger(0)
        val downloadsResolved = AtomicInteger(0)

        val semaphore = kotlinx.coroutines.sync.Semaphore(4)

        val jobs = testItems.mapIndexed { idx, item ->
            async(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    // A. getDetails
                    var detail: MediaDetail? = null
                    try {
                        detail = withTimeoutOrNull(15000L) { plugin.getDetails(item) }
                        if (detail != null && detail.title.isNotBlank()) {
                            detailsSuccess.incrementAndGet()
                        }
                    } catch (e: Throwable) {
                        failures.add("Item #${idx + 1} (${item.title}) details failed: ${e.message}")
                    }

                    // B. getStreamFlow / getStreamLinks
                    val episodeData = detail?.episodes?.firstOrNull()?.data ?: item.url.ifBlank { item.id }
                    try {
                        val emissions = withTimeoutOrNull(20000L) {
                            plugin.getStreamFlow(item.id, episodeData).toList()
                        }.orEmpty()

                        var hasStream = false
                        for (em in emissions) {
                            when (em) {
                                is StreamEmission.SourceFound -> {
                                    if (em.source.url.isNotBlank()) {
                                        hasStream = true
                                        totalStreams.incrementAndGet()
                                    }
                                }
                                is StreamEmission.SubtitleFound -> {
                                    if (em.track.url.isNotBlank()) {
                                        totalSubs.incrementAndGet()
                                    }
                                }
                                else -> {}
                            }
                        }

                        if (!hasStream) {
                            // Try fallback getStreamLinks
                            val directResult = withTimeoutOrNull(10000L) { plugin.getStreamLinks(episodeData) }
                            if (directResult != null && directResult.streams.isNotEmpty()) {
                                hasStream = true
                                totalStreams.addAndGet(directResult.streams.size)
                                totalSubs.addAndGet(directResult.subtitles.size)
                            }
                        }

                        if (hasStream) {
                            streamResolved.incrementAndGet()
                        }
                    } catch (e: Throwable) {
                        failures.add("Item #${idx + 1} (${item.title}) stream failed: ${e.message}")
                    }

                    // C. getDownloadLinks
                    try {
                        val downloads = withTimeoutOrNull(8000L) { plugin.getDownloadLinks(episodeData) }.orEmpty()
                        if (downloads.isNotEmpty()) {
                            downloadsResolved.addAndGet(downloads.size)
                        }
                    } catch (_: Throwable) {}

                } finally {
                    semaphore.release()
                }
            }
        }

        jobs.awaitAll()

        println("  [4/4] Results for ${plugin.name}:")
        println("        - Details: ${detailsSuccess.get()}/${testItems.size} verified")
        println("        - Streams: ${streamResolved.get()}/${testItems.size} items resolved (${totalStreams.get()} total sources, ${totalSubs.get()} subtitles)")
        if (downloadsResolved.get() > 0) {
            println("        - Downloads: ${downloadsResolved.get()} direct download options extracted")
        }

        PluginTestSummary(
            pluginName = plugin.name,
            isVault = isVault,
            catalogRowsCount = catalogRowsCount,
            totalItemsTested = testItems.size,
            detailsSuccessCount = detailsSuccess.get(),
            streamResolvedCount = streamResolved.get(),
            totalStreamsExtracted = totalStreams.get(),
            totalSubtitlesExtracted = totalSubs.get(),
            downloadsExtractedCount = downloadsResolved.get(),
            failures = failures.take(5)
        )
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("=================================================================")
        println("  EUTHOPIAR UNIVERSAL PLUGIN (EUP v2) FULL SUITE VERIFICATION")
        println("  Testing 15 Plugins (9 Main + 6 Vault) across 20 items each")
        println("=================================================================")

        val mainPlugins = listOf(
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

        val vaultPlugins = listOf(
            HQPornerPlugin(client),
            XvideosPlugin(client),
            EpornerPlugin(client),
            Rule34VideoPlugin(client),
            BeegPlugin(client),
            FaphousePlugin(client)
        )

        val results = mutableListOf<PluginTestSummary>()

        // Test Main Plugins
        for (p in mainPlugins) {
            val summary = testPlugin(p, isVault = false)
            results.add(summary)
        }

        // Test Vault Plugins
        for (p in vaultPlugins) {
            val summary = testPlugin(p, isVault = true)
            results.add(summary)
        }

        println("\n\n=================================================================")
        println("                     FINAL SUMMARY SCORECARD                    ")
        println("=================================================================")
        println(String.format("%-16s | %-5s | %-7s | %-12s | %-12s | %-10s | %-6s", "Plugin", "Type", "Catalog", "Details (20)", "Streams (20)", "Total Src", "Subs"))
        println("---------------------------------------------------------------------------------------------------")
        for (r in results) {
            val typeStr = if (r.isVault) "Vault" else "Main"
            println(String.format(
                "%-16s | %-5s | %-7d | %-12s | %-12s | %-10d | %-6d",
                r.pluginName,
                typeStr,
                r.catalogRowsCount,
                "${r.detailsSuccessCount}/${r.totalItemsTested}",
                "${r.streamResolvedCount}/${r.totalItemsTested}",
                r.totalStreamsExtracted,
                r.totalSubtitlesExtracted
            ))
        }
        println("=================================================================")
    }
}
