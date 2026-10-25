package com.euthopiar.ui.player

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.StreamSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TieredStreamSelectionTest {

    private fun getViewModel(): PlayerViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return PlayerViewModel(app)
    }

    @Test
    fun testDeduplicateAndSortServersAggregatesAddonNamesAndMetadata() {
        com.euthopiar.core.debrid.DebridManager.getInstance().setActiveProviders(
            listOf(com.euthopiar.core.debrid.DebridManager.ActiveDebridProvider(com.euthopiar.core.debrid.DebridProviderType.REAL_DEBRID, true))
        )
        val vm = getViewModel()
        val infoHash = "abcdef1234567890abcdef1234567890abcdef12"

        val opt1 = ServerSourceOption(
            id = "s1",
            serverName = "Big Movie 2024 1080p Torrentio",
            resolutionLabel = "1080p",
            label = "Torrentio · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:$infoHash",
                quality = "1080p",
                infoHash = infoHash,
                seeders = 150,
                matchScore = 0.90,
                isCached = true
            ),
            size = "4.2 GB",
            seeders = 150,
            isCached = true,
            isTorrent = true,
            hdrType = "HDR",
            codec = "HEVC",
            addonNames = listOf("Torrentio")
        )

        val opt2 = ServerSourceOption(
            id = "s2",
            serverName = "Big Movie 2024 1080p Comet",
            resolutionLabel = "1080p",
            label = "Comet · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:$infoHash",
                quality = "1080p",
                infoHash = infoHash,
                seeders = 120,
                matchScore = 0.85,
                isCached = false
            ),
            size = null,
            seeders = 120,
            isCached = false,
            isTorrent = true,
            hdrType = null,
            codec = null,
            addonNames = listOf("Comet")
        )

        val deduped = vm.deduplicateAndSortServers(listOf(opt1, opt2), hasDebrid = true)

        assertEquals("Redundant streams with matching transport key must be merged into 1", 1, deduped.size)
        val merged = deduped.first()
        assertTrue("Aggregated addon names should contain Torrentio", merged.addonNames.contains("Torrentio"))
        assertTrue("Aggregated addon names should contain Comet", merged.addonNames.contains("Comet"))
        assertEquals("Merged seeders should be the maximum among copies", 150, merged.seeders)
        assertEquals("Merged size should be preserved", "4.2 GB", merged.size)
        assertTrue("Merged cache status should be true if any copy is cached", merged.isCached)
        assertEquals("Merged HDR type should be preserved", "HDR", merged.hdrType)
        assertEquals("Merged codec should be preserved", "HEVC", merged.codec)
    }

    @Test
    fun testDeduplicateAndSortServersPreservesDistinctVariantsAndSortsHierarchy() {
        val vm = getViewModel()

        val opt4k = ServerSourceOption(
            id = "4k",
            serverName = "Movie 4K UHD",
            resolutionLabel = "4K",
            label = "Movie · 4K",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:hash4k1111111111111111111111111111111111",
                quality = "4K",
                infoHash = "hash4k1111111111111111111111111111111111",
                seeders = 80,
                matchScore = 0.95
            )
        )

        val opt1080p = ServerSourceOption(
            id = "1080p",
            serverName = "Movie 1080p FHD",
            resolutionLabel = "1080p",
            label = "Movie · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:hash1080p222222222222222222222222222222",
                quality = "1080p",
                infoHash = "hash1080p222222222222222222222222222222",
                seeders = 200,
                matchScore = 0.95
            )
        )

        val sorted = vm.deduplicateAndSortServers(listOf(opt1080p, opt4k))
        assertEquals("Distinct torrents must both be preserved", 2, sorted.size)
        assertEquals("4K resolution tier must rank above 1080p in default tier sort", "4k", sorted.first().id)
    }

    @Test
    fun testCleanReleaseTitleLogic() {
        val raw1 = "Dune.Part.Two.2024.2160p.WEB-DL.DDP5.1.Atmos-FLUX[eztv].mkv"
        val cleaned1 = cleanReleaseTitle(raw1)
        assertTrue("Dots should be replaced with spaces", cleaned1.contains("Dune Part Two 2024 2160p WEB-DL"))
        assertTrue("Audio channel 5.1 should be preserved", cleaned1.contains("5.1"))
        assertFalse(".mkv extension should be stripped", cleaned1.endsWith(".mkv"))
        assertFalse("[eztv] tracker should be stripped", cleaned1.contains("[eztv]"))

        val raw2 = "Show_Name_S01E01_1080p_WEB-DL_DDP7.1[TGx]"
        val cleaned2 = cleanReleaseTitle(raw2)
        assertTrue("Underscores should be replaced with spaces", cleaned2.contains("Show Name S01E01 1080p WEB-DL"))
        assertTrue("7.1 audio should be preserved", cleaned2.contains("7.1"))
        assertFalse("[TGx] should be stripped", cleaned2.contains("[TGx]"))
    }

    @Test
    fun testParseSizeToBytes() {
        assertEquals(4509715660L, parseSizeToBytes("4.2 GB"))
        assertEquals(1073741824L, parseSizeToBytes("1024 MB"))
        assertEquals(1649267441664L, parseSizeToBytes("1.5 TB"))
        assertEquals(768000L, parseSizeToBytes("750 KB"))
        assertEquals(0L, parseSizeToBytes(null))
        assertEquals(0L, parseSizeToBytes(""))
        assertEquals(0L, parseSizeToBytes("Unknown"))
    }

    @Test
    fun testSortingOptionsNoInversion() {
        val s1 = ServerSourceOption(
            id = "s1",
            serverName = "Server 1",
            streamSource = StreamSource(url = "http://example.com/1", quality = "1080p", seeders = 10),
            size = "1 GB",
            seeders = 10,
            latencyMs = 120L
        )
        val s2 = ServerSourceOption(
            id = "s2",
            serverName = "Server 2",
            streamSource = StreamSource(url = "http://example.com/2", quality = "4K", seeders = 500),
            size = "10 GB",
            seeders = 500,
            latencyMs = 20L
        )
        val s3 = ServerSourceOption(
            id = "s3",
            serverName = "Server 3",
            streamSource = StreamSource(url = "http://example.com/3", quality = "720p", seeders = 20),
            size = "500 MB",
            seeders = 20,
            latencyMs = 450L
        )
        val s4 = ServerSourceOption(
            id = "s4",
            serverName = "Server 4",
            streamSource = StreamSource(url = "http://example.com/4", quality = "1080p", seeders = 100),
            size = "2 GB",
            seeders = 100,
            latencyMs = 50L
        )

        val list = listOf(s1, s2, s3, s4)

        // Size Descending: s2 (10 GB) > s4 (2 GB) > s1 (1 GB) > s3 (500 MB)
        val sizeDesc = list.sortedByDescending { parseSizeToBytes(it.size) }
        assertEquals(listOf("s2", "s4", "s1", "s3"), sizeDesc.map { it.id })

        // Size Ascending: s3 (500 MB) < s1 (1 GB) < s4 (2 GB) < s2 (10 GB)
        val sizeAsc = list.sortedWith(
            compareBy<ServerSourceOption> {
                val b = parseSizeToBytes(it.size)
                if (b <= 0L) Long.MAX_VALUE else b
            }
        )
        assertEquals(listOf("s3", "s1", "s4", "s2"), sizeAsc.map { it.id })

        // Seeders: s2 (500) > s4 (100) > s3 (20) > s1 (10)
        val seedersDesc = list.sortedByDescending { it.seeders ?: it.streamSource.seeders ?: 0 }
        assertEquals(listOf("s2", "s4", "s3", "s1"), seedersDesc.map { it.id })

        // Latency: s2 (20ms) < s4 (50ms) < s1 (120ms) < s3 (450ms)
        val latencyAsc = list.sortedWith(
            compareBy<ServerSourceOption> {
                val lat = it.latencyMs
                if (lat != null && lat > 0L) lat else Long.MAX_VALUE
            }.thenByDescending { it.seeders ?: it.streamSource.seeders ?: 0 }
        )
        assertEquals(listOf("s2", "s4", "s1", "s3"), latencyAsc.map { it.id })

        // Test unprobed (0ms or null) latencies are placed at the bottom, not the top
        val sUnprobed = ServerSourceOption(
            id = "s0",
            serverName = "Server Unprobed",
            streamSource = StreamSource(url = "http://example.com/0", quality = "1080p"),
            latencyMs = 0L
        )
        val listWithUnprobed = listOf(sUnprobed, s2, s4)
        val latencyWithUnprobed = listWithUnprobed.sortedWith(
            compareBy<ServerSourceOption> {
                val lat = it.latencyMs
                if (lat != null && lat > 0L) lat else Long.MAX_VALUE
            }
        )
        assertEquals("0ms unprobed server must sort at bottom", listOf("s2", "s4", "s0"), latencyWithUnprobed.map { it.id })
    }

    @Test
    fun testParseSizeToBytesWithGiBAndCommas() {
        assertEquals(4509715660L, parseSizeToBytes("4.2 GiB"))
        assertEquals(1073741824L, parseSizeToBytes("1024 MiB"))
        assertEquals(1572864000L, parseSizeToBytes("1,500 MB"))
        assertEquals(13421772800L, parseSizeToBytes("~12.5 GB"))
        assertTrue("15.8 GiB must sort much higher than 500 MB", parseSizeToBytes("15.8 GiB") > parseSizeToBytes("500 MB"))
    }

    @Test
    fun testCleanReleaseTitleStripsHashesAndProviderTags() {
        val rawWithHash = "Movie.Title.2024.1080p.[abcdef0123456789abcdef0123456789abcdef01].mkv"
        val cleaned = cleanReleaseTitle(rawWithHash)
        assertFalse("40-char hash must be stripped", cleaned.contains("abcdef0123456789abcdef0123456789abcdef01"))
        assertTrue("Title text must be preserved", cleaned.contains("Movie Title 2024 1080p"))

        val rawWithProviderMode = "Torrentio • [RD+] 4K | 15.2 GB | Dune.2024.2160p (Debrid/P2P)"
        val cleanedMode = cleanReleaseTitle(rawWithProviderMode)
        assertFalse("(Debrid/P2P) mode suffix should be stripped", cleanedMode.contains("(Debrid/P2P)"))
    }

    @Test
    fun testMatchesCategoryFilters() {
        val debridServer = ServerSourceOption(
            id = "d1",
            serverName = "Real-Debrid Torrent",
            streamSource = StreamSource(url = "http://debrid.com/file", quality = "1080p", isCached = true),
            isCached = true
        )
        assertTrue(matchesCategory(debridServer, StreamCategoryFilter.ALL))
        assertTrue(matchesCategory(debridServer, StreamCategoryFilter.DEBRID, hasActiveDebrid = true))
        assertFalse(matchesCategory(debridServer, StreamCategoryFilter.DEBRID, hasActiveDebrid = false))
        assertFalse(matchesCategory(debridServer, StreamCategoryFilter.P2P, hasActiveDebrid = true))

        val p2pServer = ServerSourceOption(
            id = "p1",
            serverName = "P2P Torrent (Debrid/P2P)",
            streamSource = StreamSource(url = "magnet:?xt=urn:btih:abc", quality = "4K", infoHash = "abc"),
            isTorrent = true,
            isCached = false,
            resolutionLabel = "4K"
        )
        assertTrue(matchesCategory(p2pServer, StreamCategoryFilter.ALL))
        assertFalse("Uncached P2P torrent with (Debrid/P2P) label must not match DEBRID category", matchesCategory(p2pServer, StreamCategoryFilter.DEBRID))
        assertTrue("Uncached torrent must match P2P category", matchesCategory(p2pServer, StreamCategoryFilter.P2P))
        assertTrue(matchesCategory(p2pServer, StreamCategoryFilter.Q_4K))
        assertFalse(matchesCategory(p2pServer, StreamCategoryFilter.Q_1080P))
    }

    @Test
    fun testMatchesCategoryQualityIsolation() {
        val server1080pFhd = ServerSourceOption(
            id = "fhd",
            serverName = "Movie 1080p FHD",
            resolutionLabel = "1080p FHD",
            streamSource = StreamSource(url = "http://example.com/1080", quality = "1080p FHD")
        )
        val server4kUhd = ServerSourceOption(
            id = "uhd",
            serverName = "Movie 4K UHD",
            resolutionLabel = "4K UHD",
            streamSource = StreamSource(url = "http://example.com/4k", quality = "4K UHD")
        )
        val server720pHd = ServerSourceOption(
            id = "hd720",
            serverName = "Movie 720p HD",
            resolutionLabel = "720p HD",
            streamSource = StreamSource(url = "http://example.com/720", quality = "720p HD")
        )

        assertTrue(matchesCategory(server1080pFhd, StreamCategoryFilter.Q_1080P))
        assertFalse("1080p FHD must NOT match 720p category despite containing 'HD'", matchesCategory(server1080pFhd, StreamCategoryFilter.Q_720P))

        assertTrue(matchesCategory(server4kUhd, StreamCategoryFilter.Q_4K))
        assertFalse("4K UHD must NOT match 720p category despite containing 'HD'", matchesCategory(server4kUhd, StreamCategoryFilter.Q_720P))

        assertTrue("720p HD must match 720p category", matchesCategory(server720pHd, StreamCategoryFilter.Q_720P))
        assertFalse("720p HD must not match 1080p", matchesCategory(server720pHd, StreamCategoryFilter.Q_1080P))
    }

    @Test
    fun testFormatResolutionBadge() {
        val s1 = ServerSourceOption(id = "1", serverName = "S1", resolutionLabel = "1080p FHD", streamSource = StreamSource(url = "u", quality = "1080p FHD"))
        val s2 = ServerSourceOption(id = "2", serverName = "S2", resolutionLabel = "4K 2160p", streamSource = StreamSource(url = "u", quality = "4K 2160p"))
        val s3 = ServerSourceOption(id = "3", serverName = "S3", resolutionLabel = "720p HD", streamSource = StreamSource(url = "u", quality = "720p HD"))

        assertEquals("1080p", formatResolutionBadge(s1))
        assertEquals("4K", formatResolutionBadge(s2))
        assertEquals("720p", formatResolutionBadge(s3))
    }

    @Test
    fun testIsDebridStreamGatedBehindUserDebridStatus() {
        val torrentioRd = ServerSourceOption(
            id = "rd1",
            serverName = "Torrentio • [RD+] Movie 1080p",
            resolutionLabel = "1080p",
            label = "Torrentio • [RD+] Movie · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567",
                quality = "1080p",
                infoHash = "0123456789abcdef0123456789abcdef01234567",
                seeders = 45
            ),
            isCached = false,
            isTorrent = true
        )

        assertFalse("Torrent with [RD+] badge must NOT be classified as Debrid stream if user has no Debrid",
            isDebridStream(torrentioRd, hasActiveDebrid = false))
        assertTrue("Torrent with [RD+] badge MUST be classified as Debrid stream if user has Debrid",
            isDebridStream(torrentioRd, hasActiveDebrid = true))

        // When user has no Debrid, it belongs to P2P filter
        assertTrue("Torrent stream must belong to P2P filter when user has no Debrid",
            matchesCategory(torrentioRd, StreamCategoryFilter.P2P, hasActiveDebrid = false))
        assertFalse("Torrent stream must not belong to DEBRID filter when user has no Debrid",
            matchesCategory(torrentioRd, StreamCategoryFilter.DEBRID, hasActiveDebrid = false))
    }

    @Test
    fun testIsPrimeCandidateAllowsViableTorrentsWhenNoDebrid() {
        val vm = getViewModel()
        val torrentOption = ServerSourceOption(
            id = "t1",
            serverName = "Movie 1080p P2P",
            resolutionLabel = "1080p",
            label = "Movie · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567",
                quality = "1080p",
                infoHash = "0123456789abcdef0123456789abcdef01234567",
                seeders = 25,
                matchScore = 0.95
            ),
            seeders = 25,
            isTorrent = true
        )

        val isPrime = vm.isPrimeCandidate(torrentOption, hasDebrid = false, prefHeight = 1080)
        assertTrue("Viable torrent with >= 3 seeders should qualify as prime candidate for non-Debrid users", isPrime)
    }

    @Test
    fun testTorrentSchemesDetection() {
        val torrentPrefix = "torrent:0123456789abcdef0123456789abcdef01234567"
        val torrentSlash = "torrent://0123456789abcdef0123456789abcdef01234567"
        val magnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"
        val base32Hash = "VAL7M35SOPZ2J6T42DFTRVFFXJ2QO36D"

        fun checkIsTorrent(cleanUrl: String): Boolean {
            val urlWithoutQuery = cleanUrl.substringBefore('?').trim()
            return cleanUrl.startsWith("magnet:", ignoreCase = true) ||
                cleanUrl.startsWith("torrent://", ignoreCase = true) ||
                cleanUrl.startsWith("torrent:", ignoreCase = true) ||
                ((urlWithoutQuery.length == 40 || urlWithoutQuery.length == 64) && urlWithoutQuery.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) ||
                (urlWithoutQuery.length == 32 && urlWithoutQuery.all { it.uppercaseChar() in 'A'..'Z' || it in '2'..'7' }) ||
                com.euthopiar.core.p2p.extractP2pStreamRequest(cleanUrl) != null
        }

        assertTrue("torrent: scheme must be recognized as torrent", checkIsTorrent(torrentPrefix))
        assertTrue("torrent:// scheme must be recognized as torrent", checkIsTorrent(torrentSlash))
        assertTrue("magnet: scheme must be recognized as torrent", checkIsTorrent(magnet))
        assertTrue("raw base32 hash must be recognized as torrent", checkIsTorrent(base32Hash))
        assertTrue("raw hex hash with query params must be recognized as torrent",
            checkIsTorrent("0123456789abcdef0123456789abcdef01234567?index=1&season=1&episode=2"))
        assertTrue("raw base32 hash with query params must be recognized as torrent",
            checkIsTorrent("$base32Hash?index=2"))
    }

    @Test
    fun testStreamModelsBase32AndTorrentWithIndex() {
        val base32 = "VAL7M35SOPZ2J6T42DFTRVFFXJ2QO36D"
        val streamItemBase32 = com.euthopiar.core.streams.StreamItem(
            name = "Test Movie",
            title = "Test Movie 1080p",
            url = base32,
            quality = "1080p",
            addonName = "Torrentio",
            addonId = "torrentio"
        )
        assertTrue("StreamItem with Base32 url must be torrent stream", streamItemBase32.isTorrentStream)
        val magnet = streamItemBase32.effectiveMagnetOrHash
        assertTrue("Magnet must be generated from Base32 url", magnet?.startsWith("magnet:?xt=urn:btih:$base32") == true)

        val streamItemTorrentScheme = com.euthopiar.core.streams.StreamItem(
            name = "Test Series",
            title = "Test S01E03",
            url = "torrent:0123456789abcdef0123456789abcdef01234567/3",
            quality = "1080p",
            addonName = "Torrentio",
            addonId = "torrentio"
        )
        assertTrue("torrent: scheme must be torrent stream", streamItemTorrentScheme.isTorrentStream)
        val magnetWithIdx = streamItemTorrentScheme.effectiveMagnetOrHash
        assertTrue("Effective magnet must contain index=3", magnetWithIdx?.contains("index=3") == true)
    }

    @Test
    fun testDeduplicateAndSortServersDoesNotMarkCachedWhenUserHasNoDebrid() {
        val vm = getViewModel()
        val infoHash = "1234567890abcdef1234567890abcdef12345678"

        // Ensure user has NO active Debrid provider
        com.euthopiar.core.debrid.DebridManager.getInstance().setActiveProviders(emptyList())

        val cachedFromAddon = ServerSourceOption(
            id = "c1",
            serverName = "Movie [RD+] 1080p",
            resolutionLabel = "1080p",
            label = "Movie · 1080p",
            streamSource = StreamSource(
                url = "magnet:?xt=urn:btih:$infoHash",
                quality = "1080p",
                infoHash = infoHash,
                seeders = 40,
                isCached = true
            ),
            isCached = false,
            isTorrent = true,
            seeders = 40
        )

        val deduped = vm.deduplicateAndSortServers(listOf(cachedFromAddon))
        assertEquals(1, deduped.size)
        assertFalse("Stream must NOT be marked isCached when user has no Debrid", deduped.first().isCached)
        assertTrue("Stream must remain isTorrent when user has no Debrid", deduped.first().isTorrent)
    }
}
