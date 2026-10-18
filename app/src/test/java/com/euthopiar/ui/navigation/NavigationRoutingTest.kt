package com.euthopiar.ui.navigation

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder

class NavigationRoutingTest {

    @Test
    fun testSafeDoubleEncodePreventsPathSplits() {
        val slashId = "stream/movie/tt1234567/part1"
        val encoded = safeDoubleEncode(slashId)

        // Must not contain raw slashes which would create extra path segments in AndroidX Nav
        assertFalse("Encoded mediaId must not contain raw /", encoded.contains('/'))
        // Must contain double-encoded %252F so single decode in NavDestination yields %2F (no segment split)
        assertTrue("Must contain %252F for slashes", encoded.contains("%252F"))

        // Simulating AndroidX Nav matching: first decode (done by NavController internally)
        val afterNavMatching = URLDecoder.decode(encoded, "UTF-8")
        assertFalse("After NavController matching, must not have raw / to prevent segment split", afterNavMatching.contains('/'))

        // Second decode in composable destination
        val restored = safeDecode(afterNavMatching)
        assertEquals("Full round-trip must restore original ID", slashId, restored)
    }

    @Test
    fun testSafeDecodeHandlesNormalAndEpisodicIds() {
        val tmdbId = "12345"
        val encodedTmdb = safeDoubleEncode(tmdbId)
        val navDecodedTmdb = URLDecoder.decode(encodedTmdb, "UTF-8")
        assertEquals(tmdbId, safeDecode(navDecodedTmdb))

        val imdbId = "tt0944947"
        val encodedImdb = safeDoubleEncode(imdbId)
        val navDecodedImdb = URLDecoder.decode(encodedImdb, "UTF-8")
        assertEquals(imdbId, safeDecode(navDecodedImdb))

        val episodeId = "1399:2:5"
        val encodedEp = safeDoubleEncode(episodeId)
        val navDecodedEp = URLDecoder.decode(encodedEp, "UTF-8")
        assertEquals(episodeId, safeDecode(navDecodedEp))
    }

    @Test
    fun testBuildDetailsRouteEncodesTitleWithPercentAndHandlesNullPoster() {
        val route = buildDetailsRoute(
            mediaId = "12345",
            provider = "tmdb",
            type = "MOVIE",
            depth = 1,
            title = "100% Wolf",
            posterUrl = null
        )

        assertTrue(route.startsWith("details/12345?provider=tmdb&type=MOVIE&depth=1&title="))
        assertFalse("Poster query parameter should not be present if posterUrl is null", route.contains("&posterUrl="))
        assertTrue("Title containing % must be URL-encoded in query string", route.contains("100%25+Wolf") || route.contains("100%25Wolf") || route.contains("100%25%20Wolf"))
    }

    @Test
    fun testBuildDetailsRouteIncludesPosterUrlWhenPresent() {
        val poster = "https://image.tmdb.org/t/p/w500/test.jpg"
        val route = buildDetailsRoute(
            mediaId = "12345",
            provider = "tmdb",
            type = "MOVIE",
            depth = 2,
            title = "Spider-Man",
            posterUrl = poster
        )

        assertTrue(route.contains("&depth=2"))
        assertTrue(route.contains("&posterUrl="))
        assertTrue(route.contains("https%3A%2F%2Fimage.tmdb.org"))
    }

    @Test
    fun testBuildPlayerRouteWithAnimeAndTvSeries() {
        val route = buildPlayerRoute(
            mediaId = "anime/54321",
            provider = "gogoanime",
            type = "ANIME",
            episodeData = "anime/54321:1:12",
            mediaTitle = "Attack on Titan",
            episodeTitle = "Episode 12",
            season = 1,
            episode = 12
        )

        assertTrue(route.startsWith("player/anime%252F54321?"))
        assertTrue(route.contains("provider=gogoanime"))
        assertTrue(route.contains("type=ANIME"))
        assertTrue(route.contains("season=1"))
        assertTrue(route.contains("episode=12"))
    }

    @Test
    fun testDeeplyNestedUrlsAndPluginUrisArePreservedWithoutSegmentSplits() {
        val complexId = "plugin://source/watch?v=123&cat=anime:1:1"
        val route = buildDetailsRoute(
            mediaId = complexId,
            provider = "anime-plugin",
            type = "ANIME",
            depth = 1,
            title = "Special Show"
        )

        // Route must start with details/ followed by query string; no unencoded slashes or question marks inside mediaId
        val pathPart = route.substringBefore("?").removePrefix("details/")
        assertFalse("Path argument must not contain unencoded slashes", pathPart.contains('/'))
        assertFalse("Path argument must not contain unencoded question marks", pathPart.contains('?'))
        assertFalse("Path argument must not contain unencoded ampersands", pathPart.contains('&'))

        // Verify round trip: simulate NavController matching (single decode) then destination (safeDecode)
        val afterNavMatching = URLDecoder.decode(pathPart, "UTF-8")
        assertFalse("Single decode must still not contain slashes or question marks", afterNavMatching.contains('/') || afterNavMatching.contains('?'))
        val fullyRestored = safeDecode(afterNavMatching)
        assertEquals(complexId, fullyRestored)
    }

    @Test
    fun testBlankMediaIdSafelyDefaultsToUnknown() {
        val route = buildDetailsRoute(
            mediaId = "   ",
            provider = "tmdb",
            type = "MOVIE"
        )
        assertTrue(route.startsWith("details/unknown?"))
    }

    @Test
    fun testBuildDetailsRouteFullParameterIntegrity() {
        val mediaId = "movie:123/456"
        val title = "The Matrix & Revolutions: 100% Action"
        val poster = "https://image.tmdb.org/t/p/w500/sample.jpg"
        val route = buildDetailsRoute(
            mediaId = mediaId,
            provider = "tmdb_custom",
            type = "MOVIE",
            depth = 3,
            title = title,
            posterUrl = poster
        )

        assertTrue(route.startsWith("details/"))
        assertTrue(route.contains("depth=3"))
        assertTrue(route.contains("provider=tmdb_custom"))
        assertTrue(route.contains("type=MOVIE"))

        // Extract query parameters and mediaId to ensure no parse corruption
        val path = route.substringBefore("?").removePrefix("details/")
        val decodedPath = safeDecode(URLDecoder.decode(path, "UTF-8"))
        assertEquals(mediaId, decodedPath)
    }

    @Test
    fun testDetailsBackFallbackLogicSimulation() {
        // Simulates the contract: onBack = { if (!popBackStack()) navigate("home") }
        var popSuccess = false
        var navigatedToHome = false

        val onBackAction: () -> Unit = {
            if (!popSuccess) {
                navigatedToHome = true
            }
        }

        // When popBackStack returns false (empty stack)
        popSuccess = false
        onBackAction()
        assertTrue("When backstack cannot be popped, must navigate to home", navigatedToHome)

        // When popBackStack returns true (normal history)
        popSuccess = true
        navigatedToHome = false
        onBackAction()
        assertFalse("When backstack is popped successfully, must not trigger home fallback", navigatedToHome)
    }

    @Test
    fun testNormalizeNavTabRouteStripsQueryParameters() {
        assertEquals("library", normalizeNavTabRoute("library?filter=watchlist"))
        assertEquals("search", normalizeNavTabRoute("search?q=demon+slayer&page=1"))
        assertEquals("home", normalizeNavTabRoute("home?tab=trending"))
        assertEquals("profile", normalizeNavTabRoute("profile?user=123"))
    }

    @Test
    fun testNormalizeNavTabRouteResolvesAliases() {
        assertEquals("profile", normalizeNavTabRoute("settings"))
        assertEquals("profile", normalizeNavTabRoute("settings?section=appearance"))
        assertEquals("library", normalizeNavTabRoute("downloads"))
        assertEquals("library", normalizeNavTabRoute("downloads?status=completed"))
    }

    @Test
    fun testNormalizeNavTabRouteEdgeCases() {
        assertEquals("home", normalizeNavTabRoute(null))
        assertEquals("home", normalizeNavTabRoute(""))
        assertEquals("home", normalizeNavTabRoute("   "))
        assertEquals("library", normalizeNavTabRoute("library/"))
        assertEquals("search", normalizeNavTabRoute("search/?q=test"))
    }

    @Test
    fun testNormalizeNavTabRouteLeadingSlashesAndSubpaths() {
        assertEquals("home", normalizeNavTabRoute("/home"))
        assertEquals("search", normalizeNavTabRoute("/search?q=demon"))
        assertEquals("profile", normalizeNavTabRoute("/settings"))
        assertEquals("profile", normalizeNavTabRoute("settings/appearance"))
        assertEquals("profile", normalizeNavTabRoute("/settings/playback?audio=track1"))
        assertEquals("library", normalizeNavTabRoute("/downloads"))
        assertEquals("library", normalizeNavTabRoute("downloads/history"))
        assertEquals("library", normalizeNavTabRoute("/library/saved"))
    }

    @Test
    fun testActiveTabHighlightMatchingWithQueryAndAliases() {
        val tabs = NavTab.entries

        val libraryQueryIdx = tabs.indexOfFirst { it.route == normalizeNavTabRoute("library?filter=watchlist") }
        assertEquals(tabs.indexOf(NavTab.LIBRARY), libraryQueryIdx)

        val searchQueryIdx = tabs.indexOfFirst { it.route == normalizeNavTabRoute("search?q=test") }
        assertEquals(tabs.indexOf(NavTab.SEARCH), searchQueryIdx)

        val settingsAliasIdx = tabs.indexOfFirst { it.route == normalizeNavTabRoute("settings") }
        assertEquals(tabs.indexOf(NavTab.PROFILE), settingsAliasIdx)

        val downloadsAliasIdx = tabs.indexOfFirst { it.route == normalizeNavTabRoute("downloads") }
        assertEquals(tabs.indexOf(NavTab.LIBRARY), downloadsAliasIdx)

        val homeIdx = tabs.indexOfFirst { it.route == normalizeNavTabRoute("home") }
        assertEquals(tabs.indexOf(NavTab.HOME), homeIdx)
    }
}

