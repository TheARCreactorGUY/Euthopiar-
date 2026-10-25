package com.euthopiar.ui.settings.plugins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.euthopiar.core.model.PluginManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginsRedesignLogicTest {

    @Test
    fun testPluginsGlassTokensIntegrity() {
        // Base dark background must be #0B0B0F
        assertEquals(Color(0xFF0B0B0F), PluginsGlassTokens.BaseDarkBackground)
        // Amoled must be pure #000000
        assertEquals(Color(0xFF000000), PluginsGlassTokens.AmoledBackground)
        // Text dark label on white must be #0B0B0F
        assertEquals(Color(0xFF0B0B0F), PluginsGlassTokens.TextDark)
        // Hairline width must be 0.5dp
        assertEquals(0.5.dp, PluginsGlassTokens.HairlineWidth)
        // Corner radii
        assertEquals(8.dp, PluginsGlassTokens.RadiusTiny)
        assertEquals(12.dp, PluginsGlassTokens.RadiusInput)
        assertEquals(13.dp, PluginsGlassTokens.RadiusIcon)
        assertEquals(16.dp, PluginsGlassTokens.RadiusProviderIcon)
        assertEquals(28.dp, PluginsGlassTokens.RadiusSheet)
        assertEquals(28.dp, PluginsGlassTokens.RadiusDialog)
    }

    @Test
    fun testSegmentedBarCalculations() {
        val totalAvailable = 100
        val installed = listOf(
            createManifest("1", "Provider 1", true),
            createManifest("2", "Provider 2", true),
            createManifest("3", "Provider 3", false)
        )

        val downloadedCount = installed.count { it.isEnabled }
        val disabledCount = installed.count { !it.isEnabled }
        val notDownloadedCount = (totalAvailable - installed.size).coerceAtLeast(0)

        assertEquals(2, downloadedCount)
        assertEquals(1, disabledCount)
        assertEquals(97, notDownloadedCount)
    }

    @Test
    fun testLiveScraperSearchFiltering() {
        val scrapers = listOf(
            createManifest("1", "FlixHQ", true, "Concurrent English movie scraper", listOf("movie", "series")),
            createManifest("2", "GogoAnime", true, "Fast anime scraper", listOf("anime")),
            createManifest("3", "DramaCool", false, "Asian drama scraper", listOf("drama"))
        )

        // Empty query returns all
        val all = filterScrapers(scrapers, "")
        assertEquals(3, all.size)

        // Name match
        val byName = filterScrapers(scrapers, "flix")
        assertEquals(1, byName.size)
        assertEquals("FlixHQ", byName[0].name)

        // Description match
        val byDesc = filterScrapers(scrapers, "fast")
        assertEquals(1, byDesc.size)
        assertEquals("GogoAnime", byDesc[0].name)

        // Type match
        val byType = filterScrapers(scrapers, "drama")
        assertEquals(1, byType.size)
        assertEquals("DramaCool", byType[0].name)

        // Case-insensitivity
        val upper = filterScrapers(scrapers, "GOGO")
        assertEquals(1, upper.size)
        assertEquals("GogoAnime", upper[0].name)

        // No match
        val none = filterScrapers(scrapers, "xyz123")
        assertTrue(none.isEmpty())
    }

    @Test
    fun testCategoryFilteringLogic() {
        val plugins = listOf(
            createManifest("1", "CinemaBox", true, "", listOf("movie")),
            createManifest("2", "SeriesHub", true, "", listOf("tv", "series")),
            createManifest("3", "AnimeWorld", true, "", listOf("anime")),
            createManifest("4", "K-DramaCentral", true, "", listOf("drama", "asian")),
            createManifest("5", "MultiProvider", true, "", listOf("movie", "series", "anime"))
        )

        val all = filterCategory(plugins, "All")
        assertEquals(5, all.size)

        val movies = filterCategory(plugins, "Movies")
        assertEquals(2, movies.size) // CinemaBox, MultiProvider

        val tvSeries = filterCategory(plugins, "TV Series")
        assertEquals(2, tvSeries.size) // SeriesHub, MultiProvider

        val anime = filterCategory(plugins, "Anime")
        assertEquals(2, anime.size) // AnimeWorld, MultiProvider

        val asian = filterCategory(plugins, "Asian Dramas")
        assertEquals(1, asian.size) // K-DramaCentral
    }

    @Test
    fun testUrlSanitization() {
        val url1 = "https://raw.githubusercontent.com/user/repo/main/manifest.json"
        val sanitized1 = url1.removePrefix("https://").removePrefix("http://")
        assertEquals("raw.githubusercontent.com/user/repo/main/manifest.json", sanitized1)

        val host1 = sanitized1.substringBefore("/")
        assertEquals("raw.githubusercontent.com", host1)
    }

    @Test
    fun testLiveScraperSearchWithLanguageMatching() {
        val scrapers = listOf(
            createManifest("1", "FlixHQ", true, "High quality streams", listOf("movie", "series")),
            createManifest("2", "GogoAnime", true, "Anime provider", listOf("anime")),
            createManifest("3", "DramaCool", true, "Asian dramas provider", listOf("asian"))
        )

        // Searching "english" matches all scrapers with English badge support
        val resultEnglish = filterScrapers(scrapers, "english")
        assertEquals(3, resultEnglish.size)

        // Searching "eng" matches "english"
        val resultEng = filterScrapers(scrapers, "eng")
        assertEquals(3, resultEng.size)

        // Searching specific name
        val resultFlix = filterScrapers(scrapers, "flix")
        assertEquals(1, resultFlix.size)
        assertEquals("FlixHQ", resultFlix[0].name)

        // Searching non-matching query
        val resultNone = filterScrapers(scrapers, "xyz123nonexistent")
        assertEquals(0, resultNone.size)
    }

    @Test
    fun testTabScrollRetentionStateIsolation() {
        val tabScrollPositions = mutableMapOf<String, Int>()
        var currentTab = "Repositories"
        var scrollValue = 450
        var isSwitchingTab = false

        // User scrolled in Repositories
        tabScrollPositions[currentTab] = scrollValue

        // User initiates switch to Scrapers
        val newTab = "Scrapers"
        tabScrollPositions[currentTab] = scrollValue
        isSwitchingTab = true
        currentTab = newTab
        val targetScroll = tabScrollPositions[newTab] ?: 0

        // Scroll state change fires while switching (e.g., intermediate frame at scroll 100)
        val intermediateScroll = 100
        if (!isSwitchingTab) {
            tabScrollPositions[currentTab] = intermediateScroll
        }

        // Target tab position was not clobbered by intermediate scroll
        assertEquals(null, tabScrollPositions["Scrapers"])
        assertEquals(450, tabScrollPositions["Repositories"])
        assertEquals(0, targetScroll)

        // Smooth scroll completes
        scrollValue = targetScroll
        isSwitchingTab = false

        // Now user scrolls on Scrapers tab
        scrollValue = 280
        if (!isSwitchingTab) {
            tabScrollPositions[currentTab] = scrollValue
        }

        assertEquals(280, tabScrollPositions["Scrapers"])
        assertEquals(450, tabScrollPositions["Repositories"])
    }

    @Test
    fun testBulkActionProgressString() {
        fun formatBulkAction(totalCount: Int, uninstalledCount: Int): String {
            val installedCount = totalCount - uninstalledCount
            return "Installing $installedCount of $totalCount"
        }

        assertEquals("Installing 0 of 5", formatBulkAction(5, 5))
        assertEquals("Installing 2 of 5", formatBulkAction(5, 3))
        assertEquals("Installing 5 of 5", formatBulkAction(5, 0))
    }

    @Test
    fun testRepositoryScraperNamespacedIdMatching() {
        val repoUrl = "https://example.com/repo/manifest.json"
        val canonicalRepoId = com.euthopiar.core.js.computeCanonicalRepoId(repoUrl)

        val installedPlugins = listOf(
            createManifest("$canonicalRepoId:showbox", "ShowBox", true, siteUrl = repoUrl),
            createManifest("local:custom_script", "CustomScript", true, siteUrl = "local"),
            createManifest("repo_other:foreign_scraper", "Foreign", true, siteUrl = "https://other.com/manifest.json"),
            createManifest("legacy_raw_id", "LegacyProvider", true, siteUrl = repoUrl)
        )

        fun isPluginInstalled(p: PluginManifest): Boolean {
            val cleanId = p.id.substringAfter(":")
            val namespacedId = "$canonicalRepoId:$cleanId"
            return installedPlugins.any { installed ->
                installed.id == namespacedId ||
                (installed.id.startsWith("$canonicalRepoId:") && installed.id.substringAfter(":") == cleanId) ||
                (installed.siteUrl.isNotBlank() && com.euthopiar.core.js.computeCanonicalRepoId(installed.siteUrl) == canonicalRepoId && installed.id.substringAfter(":") == cleanId) ||
                (installed.id == p.id && !installed.id.contains(":") && (installed.siteUrl == repoUrl || installed.siteUrl.isBlank()))
            }
        }

        // Raw manifest from repo with id = "showbox"
        val repoPlugin = createManifest("showbox", "ShowBox", false, siteUrl = repoUrl)
        assertTrue("Raw ID 'showbox' must match installed namespaced ID '$canonicalRepoId:showbox'", isPluginInstalled(repoPlugin))

        // Raw manifest from repo already with namespaced ID
        val namespacedPlugin = createManifest("$canonicalRepoId:showbox", "ShowBox", false, siteUrl = repoUrl)
        assertTrue("Namespaced ID must match installed", isPluginInstalled(namespacedPlugin))

        // Legacy raw ID from this repo
        val legacyPlugin = createManifest("legacy_raw_id", "LegacyProvider", false, siteUrl = repoUrl)
        assertTrue("Legacy raw ID must match installed legacy", isPluginInstalled(legacyPlugin))

        // Local script with same raw ID should NOT match this repo
        val localPlugin = createManifest("custom_script", "CustomScript", false, siteUrl = repoUrl)
        org.junit.Assert.assertFalse("Local script 'local:custom_script' should NOT match repo scraper with same raw ID", isPluginInstalled(localPlugin))

        // Foreign scraper from another repo should NOT match this repo
        val foreignPlugin = createManifest("foreign_scraper", "Foreign", false, siteUrl = repoUrl)
        org.junit.Assert.assertFalse("Foreign scraper from other repo should NOT match this repo", isPluginInstalled(foreignPlugin))

        // Uninstalled plugin
        val uninstalled = createManifest("unknown_provider", "Unknown", false, siteUrl = repoUrl)
        org.junit.Assert.assertFalse("Uninstalled plugin should not match", isPluginInstalled(uninstalled))
    }

    @Test
    fun testConfigurableAddonBehaviorHints() {
        val addonManifest = com.euthopiar.core.addons.AddonManifest(
            id = "org.stremio.torrentio",
            name = "Torrentio",
            behaviorHints = com.euthopiar.core.addons.AddonBehaviorHints(configurable = true)
        )
        val managedAddon = com.euthopiar.core.addons.ManagedAddon(
            manifestUrl = "https://torrentio.strem.fun/providers=yts%7Csort=quality/manifest.json?api_key=123",
            manifest = addonManifest
        )
        assertTrue("Addon should be marked configurable", managedAddon.manifest?.behaviorHints?.configurable == true)

        val baseUrl = com.euthopiar.core.addons.addonTransportBaseUrl(managedAddon.manifestUrl)
        assertEquals("https://torrentio.strem.fun/providers=yts%7Csort=quality", baseUrl)
        val query = managedAddon.manifestUrl.substringAfter("?", "")
        val configUrl = if (query.isNotBlank()) "$baseUrl/configure?$query" else "$baseUrl/configure"
        assertEquals("https://torrentio.strem.fun/providers=yts%7Csort=quality/configure?api_key=123", configUrl)
    }

    private fun filterScrapers(list: List<PluginManifest>, query: String): List<PluginManifest> {
        if (query.isBlank()) return list
        val q = query.trim().lowercase()
        return list.filter { p ->
            p.name.lowercase().contains(q) ||
            p.description.lowercase().contains(q) ||
            p.supportedTypes.any { it.lowercase().contains(q) } ||
            "english".contains(q)
        }
    }

    private fun filterCategory(list: List<PluginManifest>, category: String): List<PluginManifest> {
        return when (category) {
            "All" -> list
            "Movies" -> list.filter { p -> p.supportedTypes.any { it.contains("movie", ignoreCase = true) } }
            "TV Series" -> list.filter { p -> p.supportedTypes.any { it.contains("series", ignoreCase = true) || it.contains("tv", ignoreCase = true) } }
            "Anime" -> list.filter { p -> p.supportedTypes.any { it.contains("anime", ignoreCase = true) } }
            "Asian Dramas" -> list.filter { p -> p.supportedTypes.any { it.contains("drama", ignoreCase = true) || it.contains("asian", ignoreCase = true) } }
            else -> list
        }
    }

    private fun createManifest(
        id: String,
        name: String,
        isEnabled: Boolean,
        description: String = "",
        supportedTypes: List<String> = emptyList(),
        siteUrl: String = "https://example.com"
    ): PluginManifest {
        return PluginManifest(
            id = id,
            name = name,
            version = 1,
            versionName = "1.0.0",
            author = "Author",
            description = description,
            siteUrl = siteUrl,
            supportedTypes = supportedTypes,
            className = "com.euthopiar.plugin.$id",
            isEnabled = isEnabled
        )
    }
}
