package com.euthopiar.core.streams

import com.euthopiar.core.addons.AddonManifest
import com.euthopiar.core.addons.AddonResource
import com.euthopiar.core.addons.supportsResource
import com.euthopiar.core.addons.supportsStream
import org.junit.Assert.*
import org.junit.Test

class StreamParserAndSortingTest {

    @Test
    fun testQualitySortingDescending() {
        val items = listOf(
            StreamItem(quality = "720p HD", addonName = "AddonA", addonId = "a"),
            StreamItem(quality = "480p SD", addonName = "AddonB", addonId = "b"),
            StreamItem(quality = "4K 2160p", addonName = "AddonC", addonId = "c"),
            StreamItem(quality = "Direct Stream", addonName = "AddonD", addonId = "d"),
            StreamItem(quality = "1080p FHD", addonName = "AddonE", addonId = "e")
        )

        val sorted = items.sortedForGroupedDisplay()
        assertEquals("4K 2160p", sorted[0].quality)
        assertEquals("1080p FHD", sorted[1].quality)
        assertEquals("720p HD", sorted[2].quality)
        assertEquals("480p SD", sorted[3].quality)
        assertEquals("Direct Stream", sorted[4].quality)
    }

    @Test
    fun testStreamParserExtractsResponseHeaders() {
        val payload = """
            {
                "streams": [
                    {
                        "title": "Stream 1",
                        "url": "https://example.com/video.m3u8",
                        "behaviorHints": {
                            "proxyHeaders": {
                                "request": {
                                    "User-Agent": "CustomUA"
                                },
                                "response": {
                                    "Content-Type": "application/vnd.apple.mpegurl",
                                    "Access-Control-Allow-Origin": "*"
                                }
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val parsed = StreamParser.parse(payload, "TestAddon", "test.addon")
        assertEquals(1, parsed.size)
        val stream = parsed[0]
        assertEquals("CustomUA", stream.headers["User-Agent"])
        assertEquals("application/vnd.apple.mpegurl", stream.responseHeaders["Content-Type"])
        assertEquals("*", stream.responseHeaders["Access-Control-Allow-Origin"])
    }

    @Test
    fun testEffectiveMagnetPreservesFileIdxAndTitle() {
        val item = StreamItem(
            name = "Test Movie",
            title = "Movie Title 2024",
            infoHash = "0123456789abcdef0123456789abcdef01234567",
            fileIdx = 3,
            addonName = "TorrentAddon",
            addonId = "torrent.addon"
        )

        val magnet = item.effectiveMagnetOrHash
        assertNotNull(magnet)
        assertTrue(magnet!!.startsWith("magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"))
        assertTrue(magnet.contains("index=3"))
        assertTrue(magnet.contains("dn=Movie"))
    }

    @Test
    fun testAddonManifestSupportsTvAndSeriesInterchangeably() {
        val manifest = AddonManifest(
            id = "torrentio",
            name = "Torrentio",
            resources = listOf(
                AddonResource(
                    name = "stream",
                    types = listOf("movie", "series"),
                    idPrefixes = listOf("tt")
                )
            ),
            types = listOf("movie", "series")
        )

        // When queried with "tv" (Euthopiar internal convention), it should match manifest having "series"
        assertTrue(manifest.supportsStream("tv", "tt0944947:1:1"))
        assertTrue(manifest.supportsStream("series", "tt0944947:1:1"))
        assertTrue(manifest.supportsStream("movie", "tt0137523"))
        // Numeric TMDB ID should also match because idPrefixes contains "tt"
        assertTrue(manifest.supportsStream("tv", "1399"))
        // Episodic numeric TMDB ID with colon should also match
        assertTrue(manifest.supportsStream("tv", "1399:1:1"))
        assertTrue(manifest.supportsStream("series", "1399:2:5"))
    }

    @Test
    fun testCinemetaRealManifestParsing() {
        val cinemetaJson = """
            {
                "id": "com.linvo.cinemeta",
                "version": "3.0.14",
                "description": "The official addon for movie and series catalogs",
                "name": "Cinemeta",
                "resources": ["catalog", "meta", "addon_catalog"],
                "types": ["movie", "series"],
                "idPrefixes": ["tt"],
                "catalogs": [
                    {
                        "type": "movie",
                        "id": "top",
                        "name": "Popular",
                        "genres": ["Action", "Comedy"],
                        "extra": [{"name": "genre"}, {"name": "skip"}]
                    },
                    {
                        "type": "series",
                        "id": "top",
                        "name": "Popular",
                        "genres": ["Action", "Drama"],
                        "extra": [{"name": "genre"}, {"name": "skip"}]
                    }
                ]
            }
        """.trimIndent()

        val manifest = com.euthopiar.core.addons.AddonManifestParser.parse(
            "https://v3-cinemeta.strem.io/manifest.json",
            cinemetaJson
        )

        assertEquals("com.linvo.cinemeta", manifest.id)
        assertEquals("Cinemeta", manifest.name)
        assertEquals(2, manifest.catalogs.size)
        assertEquals("top", manifest.catalogs[0].id)
        assertEquals("Popular", manifest.catalogs[0].name)
        // Cinemeta is a catalog/metadata addon, not a stream addon
        assertFalse(manifest.supportsStream("movie", "tt1254207"))
        assertTrue(manifest.supportsResource("catalog", "movie", "top"))
        assertTrue(manifest.supportsResource("meta", "series", "tt0944947"))
    }

    @Test
    fun testTorrentioRealManifestAndStreamParsing() {
        val torrentioJson = """
            {
                "id": "com.stremio.torrentio.addon",
                "version": "0.0.15",
                "name": "Torrentio",
                "description": "Provides torrent streams",
                "catalogs": [],
                "resources": [
                    {
                        "name": "stream",
                        "types": ["movie", "series", "anime"],
                        "idPrefixes": ["tt", "kitsu"]
                    }
                ],
                "types": ["movie", "series", "anime", "other"]
            }
        """.trimIndent()

        val manifest = com.euthopiar.core.addons.AddonManifestParser.parse(
            "https://torrentio.strem.fun/manifest.json",
            torrentioJson
        )

        assertEquals("com.stremio.torrentio.addon", manifest.id)
        assertEquals("Torrentio", manifest.name)
        assertTrue(manifest.catalogs.isEmpty())
        assertTrue(manifest.supportsStream("movie", "tt1254207"))
        assertTrue(manifest.supportsStream("series", "tt0944947:1:1"))
        assertTrue(manifest.supportsStream("tv", "1399:1:1"))

        // Real Torrentio stream payload
        val streamsJson = """
            {
                "streams": [
                    {
                        "name": "Torrentio\n4k",
                        "title": "Big Buck Bunny (2008) 4K 60FPS Blender Animated Short - x265\n👤 1 💾 7.1 GB ⚙️ EXT",
                        "infoHash": "f181e166e440b355ce73cc8b988f47ad7e8ea6ab",
                        "fileIdx": 0,
                        "behaviorHints": {
                            "bingeGroup": "torrentio|4k|x265",
                            "filename": "Big Buck Bunny (2008) 4K 60FPS Blender Animated Short - x265.mkv"
                        }
                    },
                    {
                        "name": "Torrentio\n1080p",
                        "title": "Big Buck Bunny 3D [2008 BDRemux 1080p]\n👤 3 💾 2.32 GB ⚙️ Rutracker",
                        "infoHash": "832de964bd23bc899bdb057795abc29eaa7d992a",
                        "sources": [
                            "tracker:http://bt.t-ru.org/ann?magnet",
                            "dht:832de964bd23bc899bdb057795abc29eaa7d992a"
                        ]
                    }
                ]
            }
        """.trimIndent()

        val parsedStreams = StreamParser.parse(streamsJson, "Torrentio", "com.stremio.torrentio.addon")
        assertEquals(2, parsedStreams.size)

        val first = parsedStreams[0]
        assertEquals("f181e166e440b355ce73cc8b988f47ad7e8ea6ab", first.infoHash)
        assertEquals(0, first.fileIdx)
        assertEquals("4K 2160p", first.quality)
        assertNotNull(first.effectiveMagnetOrHash)
        assertTrue(first.effectiveMagnetOrHash!!.startsWith("magnet:?xt=urn:btih:f181e166e440b355ce73cc8b988f47ad7e8ea6ab"))

        val second = parsedStreams[1]
        assertEquals("832de964bd23bc899bdb057795abc29eaa7d992a", second.infoHash)
        assertEquals("1080p FHD", second.quality)
    }

    @Test
    fun testOpenSubtitlesRealManifestParsing() {
        val openSubtitlesJson = """
            {
                "id": "org.stremio.opensubtitlesv3",
                "version": "1.0.0",
                "name": "OpenSubtitles v3",
                "description": "OpenSubtitles v3 Addon for Stremio",
                "catalogs": [],
                "resources": ["subtitles"],
                "types": ["movie", "series"],
                "idPrefixes": ["tt"]
            }
        """.trimIndent()

        val manifest = com.euthopiar.core.addons.AddonManifestParser.parse(
            "https://opensubtitles-v3.strem.io/manifest.json",
            openSubtitlesJson
        )

        assertEquals("org.stremio.opensubtitlesv3", manifest.id)
        assertEquals("OpenSubtitles v3", manifest.name)
        assertTrue(manifest.catalogs.isEmpty())
        assertFalse(manifest.supportsStream("movie", "tt1254207"))
        assertTrue(manifest.supportsResource("subtitles", "movie", "tt1254207"))
        assertTrue(manifest.supportsResource("subtitles", "series", "tt0944947:1:1"))
    }

    @Test
    fun testPluginRuntimeUntypedAstParserFaultTolerant() {
        val payload = """
            [
                {
                    "title": "Malformed Item With No URL Or InfoHash"
                },
                {
                    "name": "Nested Direct Stream",
                    "url": {
                        "url": "https://cdn.example.com/movie.mp4",
                        "headers": {
                            "Referer": "https://example.com"
                        },
                        "cookie": "session=abc123xyz"
                    },
                    "quality": "1080p FHD",
                    "seeders": "Seeds: 42",
                    "peers": 10
                },
                {
                    "name": "ClientResolve Stream",
                    "clientResolve": {
                        "infoHash": "aabbccddeeff00112233445566778899aabbccdd",
                        "magnetUri": "magnet:?xt=urn:btih:aabbccddeeff00112233445566778899aabbccdd",
                        "isCached": true
                    },
                    "description": "4K REMUX 👤 88"
                }
            ]
        """.trimIndent()

        val results = com.euthopiar.core.js.PluginRuntime.parseJsonResults(payload, "test-plugin")
        // Malformed item is skipped, leaving 2 valid items
        assertEquals(2, results.size)

        // Item 1: nested url hoisted with headers and cookie
        val directItem = results[0]
        assertEquals("Nested Direct Stream", directItem.title)
        assertEquals("https://cdn.example.com/movie.mp4", directItem.url)
        assertEquals("https://example.com", directItem.headers?.get("Referer"))
        assertEquals("session=abc123xyz", directItem.headers?.get("Cookie"))
        assertEquals(42, directItem.seeders)
        assertEquals(10, directItem.peers)
        assertEquals("1080p FHD", directItem.quality)

        // Item 2: clientResolve hoisted
        val crItem = results[1]
        assertEquals("aabbccddeeff00112233445566778899aabbccdd", crItem.infoHash)
        assertTrue(crItem.url.startsWith("magnet:?xt=urn:btih:aabbccddeeff00112233445566778899aabbccdd"))
        assertEquals(88, crItem.seeders)
        assertEquals("4K", crItem.quality)
    }

    @Test
    fun testStreamParserClientResolveAndProxyHeadersHoisting() {
        val payload = """
            {
                "streams": [
                    {
                        "title": "Stream With clientResolve in behaviorHints",
                        "behaviorHints": {
                            "clientResolve": {
                                "infoHash": "11223344556677889900aabbccddeeff11223344",
                                "magnetUri": "magnet:?xt=urn:btih:11223344556677889900aabbccddeeff11223344&dn=Test"
                            },
                            "proxyHeaders": {
                                "User-Agent": "NuvioPlayer/2.0",
                                "Referer": "https://stream.provider.tv"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val parsed = StreamParser.parse(payload, "TorrentAddon", "addon.torrent")
        assertEquals(1, parsed.size)
        val stream = parsed[0]
        assertEquals("11223344556677889900aabbccddeeff11223344", stream.infoHash)
        assertNotNull(stream.magnetUrl)
        assertTrue(stream.magnetUrl!!.contains("11223344556677889900aabbccddeeff11223344"))
        assertEquals("NuvioPlayer/2.0", stream.headers["User-Agent"])
        assertEquals("https://stream.provider.tv", stream.headers["Referer"])
    }

    @Test
    fun testAnimeAbsoluteEpisodeCalculation() = kotlinx.coroutines.runBlocking {
        // Season 1: 24 episodes, Season 2: 12 episodes.
        // For Season 2 Episode 5: offline fallback should calculate 24 + 5 = 29.
        val seasonCounts = mapOf(1 to 24, 2 to 12)
        val absolute = com.euthopiar.core.util.TmdbBridge.resolveAnimeAbsoluteEpisode(
            tmdbId = "fake_tmdb_id_offline_test",
            imdbId = null,
            season = 2,
            episode = 5,
            seasonEpisodeCounts = seasonCounts
        )
        assertEquals(29, absolute)

        // Season 1 Episode 12 should remain 12
        val s1Abs = com.euthopiar.core.util.TmdbBridge.resolveAnimeAbsoluteEpisode(
            tmdbId = "fake_tmdb_id_offline_test",
            imdbId = null,
            season = 1,
            episode = 12,
            seasonEpisodeCounts = seasonCounts
        )
        assertEquals(12, s1Abs)
    }

    @Test
    fun testBidirectionalIdMapping() {
        com.euthopiar.core.util.TmdbBridge.recordIdMapping(
            tmdbId = "1399",
            imdbId = "tt0944947",
            isTv = true
        )
        // Bidirectional query
        val cachedImdb = kotlinx.coroutines.runBlocking {
            com.euthopiar.core.util.TmdbBridge.resolveImdbId(tmdbId = "1399", isTv = true)
        }
        assertEquals("tt0944947", cachedImdb)

        val cachedTmdb = kotlinx.coroutines.runBlocking {
            com.euthopiar.core.util.TmdbBridge.findTmdbIdByImdb(
                client = com.euthopiar.core.network.DohDns.metadataClient,
                imdbId = "tt0944947",
                isTv = true
            )
        }
        assertEquals("1399", cachedTmdb)
    }

    @Test
    fun testStreamParserUnpacksClientResolveFileIdxAndIsCached() {
        val payload = """
            {
                "streams": [
                    {
                        "name": "Debrid Stream",
                        "title": "Episode 3",
                        "clientResolve": {
                            "infoHash": "fedcba9876543210fedcba9876543210fedcba98",
                            "fileIdx": 5,
                            "isCached": true,
                            "seeders": 45,
                            "peers": 12
                        }
                    }
                ]
            }
        """.trimIndent()

        val parsed = StreamParser.parse(payload, "DebridAddon", "debrid.addon")
        assertEquals(1, parsed.size)
        val stream = parsed[0]
        assertEquals("fedcba9876543210fedcba9876543210fedcba98", stream.infoHash)
        assertEquals(5, stream.fileIdx)
        assertTrue(stream.isCached)
        assertEquals(45, stream.seeders)
        assertEquals(12, stream.peers)
        assertTrue(stream.effectiveMagnetOrHash!!.contains("index=5"))
    }

    @Test
    fun testReversedIdMappingSucceeds() {
        // Swap arguments: first is IMDb ID, second is TMDB numeric ID
        com.euthopiar.core.util.TmdbBridge.recordIdMapping(
            tmdbId = "tt0133093",
            imdbId = "603",
            isTv = false
        )

        val resolvedImdb = kotlinx.coroutines.runBlocking {
            com.euthopiar.core.util.TmdbBridge.resolveImdbId(tmdbId = "603", isTv = false)
        }
        assertEquals("tt0133093", resolvedImdb)

        val resolvedTmdb = kotlinx.coroutines.runBlocking {
            com.euthopiar.core.util.TmdbBridge.findTmdbIdByImdb(
                client = com.euthopiar.core.network.DohDns.metadataClient,
                imdbId = "tt0133093",
                isTv = false
            )
        }
        assertEquals("603", resolvedTmdb)
    }

    @Test
    fun testAudioLanguageDiscoveryTokensAndDualExpansion() {
        // 1. Unicode Regional Flag Emojis
        val flagLangs = AudioLanguageDiscovery.parseAudioLanguagesFromMetadata("Anime Episode 01 🇯🇵 🇬🇧")
        assertTrue(flagLangs.contains("JPN"))
        assertTrue(flagLangs.contains("ENG"))

        // 2. Scene Tokens (MULTI, LAT, HIN, VOSTFR)
        val multiLangs = AudioLanguageDiscovery.parseAudioLanguagesFromMetadata("Movie 2024 1080p MULTI LAT HIN")
        assertTrue(multiLangs.contains("MULTI"))
        assertTrue(multiLangs.contains("LAT"))
        assertTrue(multiLangs.contains("HIN"))

        // 3. DUAL token expanded via TMDB original_language (e.g. ja for Anime)
        val animeDual = AudioLanguageDiscovery.parseAudioLanguagesFromMetadata(
            text = "Attack on Titan S04 DUAL 1080p",
            originalLanguage = "ja"
        )
        assertTrue(animeDual.contains("JPN"))
        assertTrue(animeDual.contains("ENG"))

        // 4. Badge formatting
        val badge = AudioLanguageDiscovery.formatLanguageBadges(listOf("ENG", "JPN"))
        assertEquals("[ENG · JPN]", badge)

        val multiBadge = AudioLanguageDiscovery.formatLanguageBadges(listOf("ENG", "MULTI"))
        assertEquals("[ENG] • [MULTI]", multiBadge)
    }

    @Test
    fun testBandwidthEstimatorQualityTiersAndDecider() {
        val estimator = com.euthopiar.core.network.BandwidthEstimator.Default

        // 1.40x VBR Headroom Factor thresholds:
        // <3 Mbps effective (<4.2 Mbps estimated) -> 480p
        assertEquals("480p", estimator.decideAutoQuality(3_000_000L))

        // 3-8 Mbps effective (4.2 - 11.2 Mbps estimated) -> 720p
        assertEquals("720p", estimator.decideAutoQuality(6_000_000L))

        // 8-22 Mbps effective (11.2 - 30.8 Mbps estimated) -> 1080p
        assertEquals("1080p", estimator.decideAutoQuality(15_000_000L))

        // >22 Mbps effective (>30.8 Mbps estimated) -> 4K
        assertEquals("4K", estimator.decideAutoQuality(40_000_000L))
    }

    @Test
    fun testComputeCanonicalRepoId() {
        val id1 = com.euthopiar.core.js.computeCanonicalRepoId("https://repo.example.com/manifest.json")
        val id2 = com.euthopiar.core.js.computeCanonicalRepoId("https://repo.example.com/manifest.json")
        val idDiff = com.euthopiar.core.js.computeCanonicalRepoId("https://other.example.com/manifest.json")

        assertTrue(id1.startsWith("repo_"))
        assertEquals(id1, id2)
        assertNotEquals(id1, idDiff)
        assertEquals("local", com.euthopiar.core.js.computeCanonicalRepoId("local"))
    }

    @Test
    fun testStreamParserExtractsContainerTrackersAndSubtitles() {
        val streamJson = """
            {
                "streams": [
                    {
                        "name": "Torrentio\n4K HDR",
                        "title": "Inception.2010.UHD.BluRay.2160p.TrueHD.Atmos.7.1.DV.HEVC-REMUX.mkv\n👤 45 💾 55.4 GB ⚙️ RD",
                        "url": "https://debrid.example.com/stream/file.mkv",
                        "behaviorHints": {
                            "filename": "Inception.2010.mkv"
                        },
                        "sources": [
                            "tracker:udp://tracker.opentrackr.org:1337/announce",
                            "tracker:udp://open.demonii.com:1337/announce"
                        ],
                        "subtitles": [
                            {
                                "id": "eng_1",
                                "lang": "eng",
                                "url": "https://example.com/subs/eng.srt"
                            }
                        ]
                    },
                    {
                        "name": "LiveStream",
                        "title": "Live 1080p Channel",
                        "url": "https://live.example.com/stream/index.m3u8"
                    }
                ]
            }
        """.trimIndent()

        val parsed = StreamParser.parse(streamJson, "torrentio", "Torrentio")
        assertEquals(2, parsed.size)

        val s1 = parsed[0]
        assertEquals("Inception.2010.mkv", s1.filename)
        assertEquals(com.euthopiar.core.model.StreamContainer.MKV, s1.container)
        assertEquals(2, s1.trackers.size)
        assertTrue(s1.trackers.contains("udp://tracker.opentrackr.org:1337/announce"))
        assertEquals(1, s1.subtitles.size)
        assertEquals("eng", s1.subtitles.first().language)
        assertEquals("https://example.com/subs/eng.srt", s1.subtitles.first().url)

        val s2 = parsed[1]
        assertEquals(com.euthopiar.core.model.StreamContainer.HLS, s2.container)
    }

    @Test
    fun testAnimeAndChannelSupportsResource() {
        val manifest = com.euthopiar.core.addons.AddonManifest(
            id = "torrentio",
            name = "Torrentio",
            resources = listOf(
                com.euthopiar.core.addons.AddonResource(
                    name = "stream",
                    types = listOf("movie", "series"),
                    idPrefixes = listOf("tt", "kitsu")
                )
            ),
            types = listOf("movie", "series")
        )

        // Addons declaring movie/series should support "anime" streams
        assertTrue(manifest.supportsStream("anime", "tt1234567"))
        assertTrue(manifest.supportsStream("anime", "kitsu:123:1"))

        // Channel / Live TV matching
        val iptvManifest = com.euthopiar.core.addons.AddonManifest(
            id = "usatv",
            name = "USA TV",
            resources = listOf(
                com.euthopiar.core.addons.AddonResource(
                    name = "stream",
                    types = listOf("channel"),
                    idPrefixes = listOf("ustv")
                )
            ),
            types = listOf("channel")
        )
        assertTrue(iptvManifest.supportsStream("channel", "ustv:fox"))
        assertTrue(iptvManifest.supportsStream("tv", "ustv:fox"))
    }

    @Test
    fun testBuildSearchCatalogDefinitionsFiltersSearchableCatalogs() {
        val nonSearchCatalog = com.euthopiar.core.addons.AddonCatalog(
            type = "movie",
            id = "top_movies",
            name = "Top Movies",
            extra = emptyList()
        )
        val searchCatalog = com.euthopiar.core.addons.AddonCatalog(
            type = "series",
            id = "search_series",
            name = "Series Search",
            extra = listOf(
                com.euthopiar.core.addons.AddonExtraProperty(
                    name = "search",
                    isRequired = true
                )
            )
        )
        val manifest = com.euthopiar.core.addons.AddonManifest(
            id = "test_addon",
            name = "Test Addon",
            catalogs = listOf(nonSearchCatalog, searchCatalog)
        )
        val managed = com.euthopiar.core.addons.ManagedAddon(
            manifestUrl = "https://example.com/manifest.json",
            manifest = manifest,
            enabled = true
        )

        val searchDefs = com.euthopiar.core.addons.buildSearchCatalogDefinitions(listOf(managed))
        assertEquals(1, searchDefs.size)
        assertEquals("search_series", searchDefs.first().catalogId)
        assertEquals("Test Addon", searchDefs.first().addonName)
    }
}
