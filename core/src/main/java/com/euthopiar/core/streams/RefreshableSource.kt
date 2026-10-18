package com.euthopiar.core.streams

import com.euthopiar.core.js.JsPluginStorage
import com.euthopiar.core.js.PluginRuntime
import com.euthopiar.core.js.normalizePluginType
import com.euthopiar.core.js.pluginContentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI

/**
 * Single-flight token auto-refresh for media stream sources.
 *
 * Wraps token renewal in a Mutex so concurrent worker 403s only trigger
 * a single QuickJS re-scrape matching on (scraperId, quality, host, name).
 */
class RefreshableSource(
    val scraperId: String,
    val mediaType: String,
    val videoId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val quality: String? = null,
    val host: String? = null,
    val name: String? = null,
    initialUrl: String,
    initialHeaders: Map<String, String> = emptyMap()
) {
    private val mutex = Mutex()

    @Volatile
    var currentUrl: String = initialUrl
        private set

    @Volatile
    var currentHeaders: Map<String, String> = initialHeaders
        private set

    /**
     * Called when a download or playback worker receives HTTP 403 / 401.
     * Guarantees that only the first failing worker triggers a QuickJS re-scrape;
     * other concurrent workers await the mutex and immediately receive the renewed URL.
     */
    suspend fun refreshOnExpiredToken(staleUrl: String): String? = mutex.withLock {
        // If another worker already renewed the URL, return it immediately without re-scraping
        if (currentUrl != staleUrl && currentUrl.isNotBlank()) {
            return currentUrl
        }

        withContext(Dispatchers.IO) {
            try {
                val storage = JsPluginStorage.getInstanceOrNull() ?: return@withContext null
                val normType = normalizePluginType(mediaType)
                val scraper = storage.getEnabledScrapersForType(normType).firstOrNull { it.id == scraperId }
                    ?: return@withContext null

                val cleanTmdbId = pluginContentId(videoId, season, episode)
                val results = withTimeoutOrNull(45_000L) {
                    PluginRuntime.executePlugin(
                        code = scraper.code,
                        tmdbId = cleanTmdbId,
                        mediaType = normType,
                        season = season,
                        episode = episode,
                        scraperId = scraper.id,
                        repoId = scraper.canonicalRepoId
                    )
                } ?: emptyList()

                val targetHost = host ?: try { URI(staleUrl).host } catch (_: Exception) { null }

                // Match candidate stream by (scraperId, quality, host, name)
                val matching = results.firstOrNull { res ->
                    val resHost = try { URI(res.url).host } catch (_: Exception) { null }
                    val hostMatches = targetHost == null || resHost.equals(targetHost, ignoreCase = true)
                    val qualityMatches = quality == null || res.quality.equals(quality, ignoreCase = true)
                    val nameMatches = name == null || res.name.equals(name, ignoreCase = true)
                    hostMatches && qualityMatches && nameMatches
                } ?: results.firstOrNull { res ->
                    val resHost = try { URI(res.url).host } catch (_: Exception) { null }
                    targetHost != null && resHost.equals(targetHost, ignoreCase = true)
                } ?: results.firstOrNull()

                if (matching != null && matching.url.isNotBlank()) {
                    currentUrl = matching.url
                    currentHeaders = matching.headers ?: emptyMap()
                    currentUrl
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.w("RefreshableSource", "Token auto-refresh failed: ${e.message}")
                null
            }
        }
    }
}
