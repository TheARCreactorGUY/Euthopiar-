package com.euthopiar.core.util

import com.euthopiar.core.addons.AddonManifest
import com.euthopiar.core.addons.AddonResource
import com.euthopiar.core.addons.buildAddonResourceUrl
import com.euthopiar.core.addons.encodeAddonPathSegment
import com.euthopiar.core.addons.supportsResource
import com.euthopiar.core.addons.supportsStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentBridgeTest {

    @Test
    fun testColonPreservedInAddonPathSegment() {
        val seriesEpisodeId = "tt0944947:1:1"
        val encoded = seriesEpisodeId.encodeAddonPathSegment()
        assertEquals("tt0944947:1:1", encoded)

        val url = buildAddonResourceUrl(
            manifestUrl = "https://catalog.nuvio.tv/manifest.json",
            resource = "stream",
            type = "series",
            id = seriesEpisodeId
        )
        assertEquals("https://catalog.nuvio.tv/stream/series/tt0944947:1:1.json", url)
    }

    @Test
    fun testTvvooLiveTvUrlGeneration() {
        val channelId = "vavoo_SKY%201|group:uk"
        val encoded = channelId.encodeAddonPathSegment()
        assertEquals("vavoo_SKY%201%7Cgroup:uk", encoded)

        val metaUrl = buildAddonResourceUrl(
            manifestUrl = "https://tvvoo.com/manifest.json",
            resource = "meta",
            type = "tv",
            id = channelId
        )
        assertEquals("https://tvvoo.com/meta/tv/vavoo_SKY%201%7Cgroup:uk.json", metaUrl)

        val streamUrl = buildAddonResourceUrl(
            manifestUrl = "https://tvvoo.com/manifest.json",
            resource = "stream",
            type = "tv",
            id = channelId
        )
        assertEquals("https://tvvoo.com/stream/tv/vavoo_SKY%201%7Cgroup:uk.json", streamUrl)
    }

    @Test
    fun testIsCustomStremioAddonId() {
        assertTrue(com.euthopiar.core.addons.isCustomStremioAddonId("vavoo_SKY%201|group:uk"))
        assertTrue(com.euthopiar.core.addons.isCustomStremioAddonId("custom_channel:123"))
        assertFalse(com.euthopiar.core.addons.isCustomStremioAddonId("550"))
        assertFalse(com.euthopiar.core.addons.isCustomStremioAddonId("tt0944947"))
        assertFalse(com.euthopiar.core.addons.isCustomStremioAddonId("tmdb:12345"))
        assertFalse(com.euthopiar.core.addons.isCustomStremioAddonId("kitsu:456"))
        assertFalse(com.euthopiar.core.addons.isCustomStremioAddonId("   "))
    }

    @Test
    fun testAddonManifestEffectivePrefixesFallback() {
        // Manifest defines idPrefixes at root, but resource has empty idPrefixes
        val manifest = AddonManifest(
            id = "tv.nuvio.catalog",
            name = "Nuvio Catalog",
            idPrefixes = listOf("tt", "tmdb:"),
            resources = listOf(
                AddonResource(
                    name = "stream",
                    types = listOf("movie", "series"),
                    idPrefixes = emptyList() // should fall back to root idPrefixes
                )
            )
        )

        // BaseId starts with tt:
        assertTrue(manifest.supportsStream("series", "tt0944947:1:1"))
        assertTrue(manifest.supportsStream("movie", "tt1375666"))

        // Numeric TMDB ID should match because effectivePrefixes has "tt" and "tmdb:"
        assertTrue(manifest.supportsStream("series", "1399:1:1"))
        assertTrue(manifest.supportsStream("movie", "550"))

        // Unrelated prefix should not match
        assertFalse(manifest.supportsStream("movie", "kitsu:12345"))
    }

    @Test
    fun testAddonManifestResourcePrefixOverridesRoot() {
        val manifest = AddonManifest(
            id = "org.specific.addon",
            name = "Specific Addon",
            idPrefixes = listOf("tt"),
            resources = listOf(
                AddonResource(
                    name = "stream",
                    types = listOf("movie", "series"),
                    idPrefixes = listOf("kitsu:")
                )
            )
        )

        // Resource idPrefixes overrides root idPrefixes
        assertTrue(manifest.supportsStream("series", "kitsu:12345"))
        assertFalse(manifest.supportsStream("movie", "tt1375666"))
    }


    @Test
    fun testCachedEnrichedDetailsImdbLookup() {
        // Seed cache with mapping
        TmdbBridge.recordIdMapping(tmdbId = "1399", imdbId = "tt0944947", isTv = true)

        // Manually place mock enriched metadata into cache via reflection or ensure key resolution works
        val fakeMetadata = TmdbBridge.EnrichedMetadata(
            tmdbId = "1399",
            title = "Game of Thrones",
            posterUrl = null,
            backdropUrl = null,
            synopsis = "Winter is coming",
            rating = "9.2",
            rottenTomatoesRating = "89%",
            contentRating = "TV-MA",
            duration = "60m",
            year = 2011,
            imdbId = "tt0944947",
            genres = listOf("Drama", "Sci-Fi & Fantasy"),
            cast = emptyList(),
            directors = emptyList(),
            recommendations = emptyList(),
            trailerUrl = null,
            seasonNumbers = listOf(1, 2, 3),
            logoUrl = null
        )

        // Access private enrichedCache to verify getCachedEnrichedDetails resolves via IMDb ID
        val field = TmdbBridge::class.java.getDeclaredField("enrichedCache")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = field.get(null) as java.util.concurrent.ConcurrentHashMap<String, TmdbBridge.EnrichedMetadata>
        cache["tv:1399"] = fakeMetadata

        // Lookup by numeric TMDB ID
        val byTmdb = TmdbBridge.getCachedEnrichedDetails("1399", isTv = true)
        assertNotNull(byTmdb)
        assertEquals("Game of Thrones", byTmdb?.title)

        // Lookup by IMDb ID using getCachedEnrichedDetails
        val byImdb = TmdbBridge.getCachedEnrichedDetails("tt0944947", isTv = true)
        assertNotNull(byImdb)
        assertEquals("Game of Thrones", byImdb?.title)
        assertEquals("1399", byImdb?.tmdbId)

        // Edge case: Lookup with colon suffix or tmdb prefix
        val byEp = TmdbBridge.getCachedEnrichedDetails("1399:1:1", isTv = true)
        assertNotNull(byEp)
        assertEquals("Game of Thrones", byEp?.title)

        val byImdbEp = TmdbBridge.getCachedEnrichedDetails("tt0944947:1:1", isTv = true)
        assertNotNull(byImdbEp)
        assertEquals("Game of Thrones", byImdbEp?.title)

        // Edge case: Cross-type lookup fallback (cached as tv, queried as movie)
        val crossType = TmdbBridge.getCachedEnrichedDetails("1399", isTv = false)
        assertNotNull(crossType)
        assertEquals("Game of Thrones", crossType?.title)
    }

    @Test
    fun testAddonManifestSupportsTmdbPrefix() {
        val manifest = AddonManifest(
            id = "org.torrentio.addon",
            name = "Torrentio",
            idPrefixes = listOf("tt"),
            resources = listOf(
                AddonResource(
                    name = "stream",
                    types = listOf("movie", "series"),
                    idPrefixes = emptyList()
                )
            )
        )

        // Prefixed with tmdb: or tmdb/ should match because the underlying numeric ID is supported
        assertTrue(manifest.supportsStream("series", "tmdb:1399:1:1"))
        assertTrue(manifest.supportsStream("movie", "tmdb/550"))
        assertTrue(manifest.supportsStream("series", "1399:1:1"))
    }

    @Test
    fun testResolveImdbIdEdgeCases() = runBlocking {
        TmdbBridge.recordIdMapping(tmdbId = "1399", imdbId = "tt0944947", isTv = true)

        assertEquals("tt0944947", TmdbBridge.resolveImdbId(tmdbId = "1399", isTv = true))
        assertEquals("tt0944947", TmdbBridge.resolveImdbId(tmdbId = "1399:1:1", isTv = true))
        assertEquals("tt0944947", TmdbBridge.resolveImdbId(tmdbId = "tmdb:1399", isTv = true))
        assertEquals("tt0944947", TmdbBridge.resolveImdbId(tmdbId = "tt0944947:1:1", isTv = true))
    }

    @Test
    fun testDetectContainerMimeTypesAndQueryUrls() {
        val (ext1, c1) = com.euthopiar.core.streams.StreamParser.detectContainer(
            url = "https://example.com/proxy?url=https%3A%2F%2Ffeed.com%2Fstream.m3u8",
            filename = null,
            rawType = null,
            rawFormat = null
        )
        assertEquals(com.euthopiar.core.model.StreamContainer.HLS, c1)

        val (ext2, c2) = com.euthopiar.core.streams.StreamParser.detectContainer(
            url = "https://example.com/live/channel",
            filename = null,
            rawType = "application/vnd.apple.mpegurl",
            rawFormat = null
        )
        assertEquals(com.euthopiar.core.model.StreamContainer.HLS, c2)

        val (ext3, c3) = com.euthopiar.core.streams.StreamParser.detectContainer(
            url = "https://example.com/manifest.mpd?auth=123",
            filename = null,
            rawType = null,
            rawFormat = null
        )
        assertEquals(com.euthopiar.core.model.StreamContainer.DASH, c3)
    }

    @Test
    fun testLiveTvResourceMatchingCrossTypes() {
        val manifest = AddonManifest(
            id = "tv.tvvoo",
            name = "TvVoo",
            idPrefixes = listOf("vavoo"),
            resources = listOf(
                AddonResource(
                    name = "stream",
                    types = listOf("tv"),
                    idPrefixes = listOf("vavoo")
                )
            )
        )

        assertTrue(manifest.supportsStream("live", "vavoo_SKY%201|group:uk"))
        assertTrue(manifest.supportsStream("channel", "vavoo_SKY%201|group:uk"))
        assertTrue(manifest.supportsStream("live_tv", "vavoo_SKY%201|group:uk"))
        assertTrue(manifest.supportsStream("tv", "vavoo_SKY%201|group:uk"))
    }
}
