package com.euthopiar.ui.details.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.euthopiar.core.streams.StreamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadEditorialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val torrentHighSeed = StreamItem(
        name = "Movie.2024.2160p",
        quality = "4K 2160p",
        size = "12.5 GB",
        seeders = 350,
        sourceName = "Torrentio",
        addonName = "Torrentio",
        addonId = "torrentio",
        magnetUrl = "magnet:?xt=urn:btih:hash1"
    )

    private val torrentMediumSeed = StreamItem(
        name = "Movie.2024.1080p",
        quality = "1080p",
        size = "4.5 GB",
        seeders = 150,
        sourceName = "Comet",
        addonName = "Comet",
        addonId = "comet",
        magnetUrl = "magnet:?xt=urn:btih:hash2"
    )

    private val torrentLowSeed = StreamItem(
        name = "Movie.2024.720p",
        quality = "720p",
        size = "1.8 GB",
        seeders = 12,
        sourceName = "MediaFusion",
        addonName = "MediaFusion",
        addonId = "mediafusion",
        magnetUrl = "magnet:?xt=urn:btih:hash3"
    )

    private val torrentPoorSeed = StreamItem(
        name = "Movie.2024.480p",
        quality = "480p",
        size = "700 MB",
        seeders = 3,
        sourceName = "MediaFusion",
        addonName = "MediaFusion",
        addonId = "mediafusion",
        magnetUrl = "magnet:?xt=urn:btih:hash4"
    )

    private val directStream1 = StreamItem(
        name = "Movie.2024.Direct1",
        quality = "1080p",
        size = "2.8 GB",
        sourceName = "4KHDHub",
        addonName = "4KHDHub",
        addonId = "4khdhub",
        url = "https://example.com/stream1.mp4"
    )

    private val directStream2 = StreamItem(
        name = "Movie.2024.Direct2",
        quality = "720p",
        size = "1.2 GB",
        sourceName = "SuperStream",
        addonName = "SuperStream",
        addonId = "superstream",
        url = "https://example.com/stream2.mp4"
    )

    @Test
    fun testRankAndPartition_torrentsAndDirect_correctHeroAndSlotting() {
        val streams = listOf(
            torrentLowSeed,
            directStream1,
            torrentHighSeed,
            torrentPoorSeed,
            torrentMediumSeed,
            directStream2
        )

        val ranked = DownloadTokens.rankAndPartitionStreams(streams)

        // Hero must be highest seed torrent (torrentHighSeed: 350 seeds)
        assertEquals(torrentHighSeed, ranked.hero)

        // Remaining rows must have:
        // 1. Torrents with >= 100 seeds (torrentMediumSeed: 150)
        // 2. Direct sources (directStream1, directStream2)
        // 3. Torrents with < 100 seeds (torrentLowSeed: 12, torrentPoorSeed: 3)
        val remaining = ranked.remaining
        assertEquals(5, remaining.size)
        assertEquals(torrentMediumSeed, remaining[0])
        assertEquals(directStream1, remaining[1])
        assertEquals(directStream2, remaining[2])
        assertEquals(torrentLowSeed, remaining[3])
        assertEquals(torrentPoorSeed, remaining[4])
    }

    @Test
    fun testRankAndPartition_onlyDirectSources_picksFirstDirectAsHero() {
        val streams = listOf(directStream1, directStream2)
        val ranked = DownloadTokens.rankAndPartitionStreams(streams)

        assertEquals(directStream1, ranked.hero)
        assertEquals(listOf(directStream2), ranked.remaining)
    }

    @Test
    fun testRankAndPartition_emptyList() {
        val ranked = DownloadTokens.rankAndPartitionStreams(emptyList())
        assertNull(ranked.hero)
        assertTrue(ranked.remaining.isEmpty())
    }

    @Test
    fun testHealthTierColors() {
        // Direct sources always use textPrimary
        assertEquals(DownloadTokens.textPrimary, DownloadTokens.getHealthTierColor(seeds = 0, isDirect = true))
        assertEquals(DownloadTokens.textPrimary, DownloadTokens.getHealthTierColor(seeds = 500, isDirect = true))

        // P2P / Torrents:
        // >= 100: textPrimary
        assertEquals(DownloadTokens.textPrimary, DownloadTokens.getHealthTierColor(seeds = 100, isDirect = false))
        assertEquals(DownloadTokens.textPrimary, DownloadTokens.getHealthTierColor(seeds = 250, isDirect = false))

        // 10 to 99: textSecondary
        assertEquals(DownloadTokens.textSecondary, DownloadTokens.getHealthTierColor(seeds = 10, isDirect = false))
        assertEquals(DownloadTokens.textSecondary, DownloadTokens.getHealthTierColor(seeds = 99, isDirect = false))

        // Under 10: textMuted
        assertEquals(DownloadTokens.textMuted, DownloadTokens.getHealthTierColor(seeds = 9, isDirect = false))
        assertEquals(DownloadTokens.textMuted, DownloadTokens.getHealthTierColor(seeds = 0, isDirect = false))
        assertEquals(DownloadTokens.textMuted, DownloadTokens.getHealthTierColor(seeds = null, isDirect = false))
    }

    @Test
    fun testParseStreamSize() {
        val p1 = DownloadTokens.parseStreamSize("11.98 GB")
        assertEquals(11.98, p1.numericValue, 0.001)
        assertEquals("11.98", p1.formattedNumeral)
        assertEquals("GB", p1.unit)

        val p2 = DownloadTokens.parseStreamSize("~2.0 GB")
        assertEquals(2.0, p2.numericValue, 0.001)
        assertEquals("2.00", p2.formattedNumeral)
        assertEquals("GB", p2.unit)

        val p3 = DownloadTokens.parseStreamSize("850 MB")
        assertEquals(850.0, p3.numericValue, 0.001)
        assertEquals("850.00", p3.formattedNumeral)
        assertEquals("MB", p3.unit)

        val p4 = DownloadTokens.parseStreamSize(null, "3.5 GB")
        assertEquals(3.5, p4.numericValue, 0.001)
        assertEquals("3.50", p4.formattedNumeral)
        assertEquals("GB", p4.unit)

        // Thousands separator
        val p5 = DownloadTokens.parseStreamSize("1,250 MB")
        assertEquals(1250.0, p5.numericValue, 0.001)
        assertEquals("1250.00", p5.formattedNumeral)
        assertEquals("MB", p5.unit)
    }

    @Test
    fun testExtractResolution_uhdAndWordBoundaries() {
        // UHD must map to 4K 2160p, NOT 720p
        assertEquals("4K 2160p", DownloadTokens.extractResolution("UHD"))
        assertEquals("4K 2160p", DownloadTokens.extractResolution("4K UHD"))
        assertEquals("4K 2160p", DownloadTokens.extractResolution("2160p"))

        // FHD and 1080p
        assertEquals("1080p", DownloadTokens.extractResolution("1080p"))
        assertEquals("1080p", DownloadTokens.extractResolution("FHD"))

        // HD and 720p
        assertEquals("720p", DownloadTokens.extractResolution("720p"))
        assertEquals("720p", DownloadTokens.extractResolution("HD"))

        // Word boundary: HDR is high dynamic range, not 720p
        assertNull(DownloadTokens.extractResolution("HDR"))
        assertEquals("4K 2160p", DownloadTokens.extractResolution("4KHDR"))
    }

    @Test
    fun testCalculateEstimatedStreamSize() {
        val runtimeMin = 120 // 2 hours
        assertEquals("9.00 GB", DownloadTokens.calculateEstimatedStreamSize(runtimeMin, "4K 2160p"))
        assertEquals("4.40 GB", DownloadTokens.calculateEstimatedStreamSize(runtimeMin, "1080p"))
        assertEquals("2.40 GB", DownloadTokens.calculateEstimatedStreamSize(runtimeMin, "720p"))
        assertEquals("1.20 GB", DownloadTokens.calculateEstimatedStreamSize(runtimeMin, "480p"))

        val shortEpMin = 20
        assertEquals("204 MB", DownloadTokens.calculateEstimatedStreamSize(shortEpMin, "480p"))
    }

    @Test
    fun testCommonResolutionExtraction() {
        // Uniform resolutions
        val uniformStreams = listOf(
            torrentHighSeed.copy(quality = "1080p"),
            directStream1.copy(quality = "1080p")
        )
        assertEquals("1080p", DownloadTokens.parseCommonResolution(uniformStreams))

        // Mixed resolutions
        val mixedStreams = listOf(
            torrentHighSeed.copy(quality = "4K 2160p"),
            directStream1.copy(quality = "1080p")
        )
        assertNull(DownloadTokens.parseCommonResolution(mixedStreams))
    }

    @Test
    fun testAccessibilityDescription() {
        val torrentDesc = DownloadTokens.getStreamAccessibilityDescription(torrentHighSeed, "12.5 GB")
        assertTrue(torrentDesc.contains("4K 2160p"))
        assertTrue(torrentDesc.contains("12.5 gigabytes"))
        assertTrue(torrentDesc.contains("350 seeds"))
        assertTrue(torrentDesc.contains("Torrentio"))

        val directDesc = DownloadTokens.getStreamAccessibilityDescription(directStream1, "2.8 GB")
        assertTrue(directDesc.contains("1080p"))
        assertTrue(directDesc.contains("2.8 gigabytes"))
        assertTrue(directDesc.contains("direct via 4KHDHub"))
    }

    @Test
    fun testEditorialDownloadContent_rendersHeaderAndTabs() {
        val streams = listOf(torrentHighSeed, directStream1)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "The Substance",
                allStreams = streams,
                isLoading = false,
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                onDownloadClick = { _, _ -> }
            )
        }

        // Header
        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()

        // Tabs when mixed types exist
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("P2P").assertIsDisplayed()
        composeTestRule.onNodeWithText("Direct").assertIsDisplayed()
    }

    @Test
    fun testEditorialDownloadContent_singleTypeShowsAllFilterTabs() {
        val streams = listOf(torrentHighSeed, torrentMediumSeed)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Dune: Part Two",
                allStreams = streams,
                isLoading = false,
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                onDownloadClick = { _, _ -> }
            )
        }

        // Filter tabs MUST always be visible and selectable even if only 1 stream type has arrived
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("P2P").assertIsDisplayed()
        composeTestRule.onNodeWithText("Direct").assertIsDisplayed()
    }

    @Test
    fun testEditorialDownloadContent_errorStateShowsRetry() {
        var retried = false

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Gladiator II",
                allStreams = emptyList(),
                isLoading = false,
                isError = true,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                onDownloadClick = { _, _ -> },
                onRetry = { retried = true }
            )
        }

        composeTestRule.onNodeWithText("Couldn't load sources").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assertTrue("Expected retry callback to be executed", retried)
    }

    @Test
    fun testRankAndPartition_equalSeedCountsPreservesAll() {
        val torrent1 = torrentHighSeed.copy(name = "T1", magnetUrl = "magnet:?xt=urn:btih:h1")
        val torrent2 = torrentHighSeed.copy(name = "T2", magnetUrl = "magnet:?xt=urn:btih:h2")
        val streams = listOf(torrent1, torrent2)

        val ranked = DownloadTokens.rankAndPartitionStreams(streams)
        assertEquals(torrent1, ranked.hero)
        assertEquals(1, ranked.remaining.size)
        assertEquals(torrent2, ranked.remaining[0])
    }

    @Test
    fun testEditorialDownloadContent_dynamicLoadingShowsTabs() {
        // Even when only torrents have arrived so far, if isAnyLoading is true, tabs are visible
        val streams = listOf(torrentHighSeed, torrentMediumSeed)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Alien: Romulus",
                allStreams = streams,
                isLoading = false,
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                isAnyLoading = true,
                onDownloadClick = { _, _ -> }
            )
        }

        // Tabs MUST be displayed while streams are loading dynamically
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("P2P").assertIsDisplayed()
        composeTestRule.onNodeWithText("Direct").assertIsDisplayed()
    }

    @Test
    fun testEditorialDownloadContent_tabFilteringWorks() {
        val streams = listOf(torrentHighSeed, directStream1)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Wicked",
                allStreams = streams,
                isLoading = false,
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                onDownloadClick = { _, _ -> }
            )
        }

        // All tab selected by default: both hero (torrentHighSeed) and direct stream are present
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("P2P").assertIsDisplayed()
        composeTestRule.onNodeWithText("Direct").assertIsDisplayed()

        // Switch to Direct
        composeTestRule.onNodeWithText("Direct").performClick()
        // Direct stream now becomes the hero
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()

        // Switch to P2P
        composeTestRule.onNodeWithText("P2P").performClick()
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()
    }

    @Test
    fun testStableStreamKeys_strictlyUniqueAcrossIdenticalStreams() {
        // Two streams from the exact same addon with the exact same magnet/url and title
        val duplicate1 = torrentHighSeed.copy()
        val duplicate2 = torrentHighSeed.copy()
        val duplicate3 = torrentHighSeed.copy(magnetUrl = null, url = null, name = "Same", title = "Same")
        val duplicate4 = torrentHighSeed.copy(magnetUrl = null, url = null, name = "Same", title = "Same")

        val list = listOf(duplicate1, duplicate2, duplicate3, duplicate4)
        val keyed = list.toKeyedStreamItems()

        assertEquals(4, keyed.size)
        val keys = keyed.map { it.key }
        // All keys MUST be distinct to avoid Compose LazyColumn key collision crashes
        assertEquals(4, keys.distinct().size)
    }

    @Test
    fun testRankAndPartition_p2pCategoryOnlyContainsTorrents() {
        val mixedStreams = listOf(torrentHighSeed, directStream1, torrentMediumSeed, directStream2)
        val p2pOnly = mixedStreams.filter { it.isTorrentStream }
        val ranked = DownloadTokens.rankAndPartitionStreams(p2pOnly)

        assertEquals(torrentHighSeed, ranked.hero)
        assertEquals(listOf(torrentMediumSeed), ranked.remaining)
        assertTrue(ranked.remaining.all { it.isTorrentStream })
    }

    @Test
    fun testRankAndPartition_directCategoryOnlyContainsDirect() {
        val mixedStreams = listOf(torrentHighSeed, directStream1, torrentMediumSeed, directStream2)
        val directOnly = mixedStreams.filter { !it.isTorrentStream }
        val ranked = DownloadTokens.rankAndPartitionStreams(directOnly)

        assertEquals(directStream1, ranked.hero)
        assertEquals(listOf(directStream2), ranked.remaining)
        assertTrue(ranked.remaining.none { it.isTorrentStream })
    }

    @Test
    fun testEditorialDownloadContent_dynamicStreamArrivalDoesNotCrash() {
        val streamList = androidx.compose.runtime.mutableStateListOf<StreamItem>()
        var isAnyLoading by androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Dynamic Arrival",
                allStreams = streamList,
                isLoading = isAnyLoading && streamList.isEmpty(),
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                isAnyLoading = isAnyLoading,
                onDownloadClick = { _, _ -> }
            )
        }

        // Initial state: loading skeleton
        composeTestRule.waitForIdle()

        // 1. First low seed torrent arrives
        streamList.add(torrentLowSeed)
        composeTestRule.waitForIdle()

        // 2. High seed torrent arrives (displaces low seed torrent to remaining list!)
        streamList.add(torrentHighSeed)
        composeTestRule.waitForIdle()

        // 3. Direct stream arrives
        streamList.add(directStream1)
        composeTestRule.waitForIdle()

        // 4. Duplicate stream arrives (same URL/magnet)
        streamList.add(torrentHighSeed.copy())
        composeTestRule.waitForIdle()

        // 5. Another direct stream arrives
        streamList.add(directStream2)
        composeTestRule.waitForIdle()

        // 6. Switch to Direct tab while still loading
        composeTestRule.onNodeWithText("Direct").performClick()
        composeTestRule.waitForIdle()

        // 7. Another direct arrives
        streamList.add(directStream1.copy(name = "Direct3", url = "https://example.com/stream3.mp4"))
        composeTestRule.waitForIdle()

        // 8. Switch to P2P tab
        composeTestRule.onNodeWithText("P2P").performClick()
        composeTestRule.waitForIdle()

        // 9. Another P2P arrives with higher seeds
        streamList.add(torrentHighSeed.copy(name = "TorrentUltra", seeders = 1000, magnetUrl = "magnet:?xt=urn:btih:ultra"))
        composeTestRule.waitForIdle()

        // 10. Switch back to All
        composeTestRule.onNodeWithText("All").performClick()
        composeTestRule.waitForIdle()

        // 11. Loading finishes
        isAnyLoading = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()
    }

    @Test
    fun testEditorialDownloadContent_directTabShowsSearchingThenStreams() {
        val streamList = androidx.compose.runtime.mutableStateListOf<StreamItem>()
        var isAnyLoading by androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Searching Direct",
                allStreams = streamList,
                isLoading = isAnyLoading && streamList.isEmpty(),
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                isAnyLoading = isAnyLoading,
                onDownloadClick = { _, _ -> }
            )
        }

        // Add only a torrent stream
        streamList.add(torrentHighSeed)
        composeTestRule.waitForIdle()

        // Switch to Direct tab
        composeTestRule.onNodeWithText("Direct").performClick()
        composeTestRule.waitForIdle()

        // While scrapers are still running, searching message should be displayed
        composeTestRule.onNodeWithText("Searching for direct sources...").assertIsDisplayed()

        // Now a direct source arrives from a slower scraper
        streamList.add(directStream1)
        composeTestRule.waitForIdle()

        // Searching message should disappear, and Direct stream should now be displayed as Best match
        composeTestRule.onNodeWithText("Searching for direct sources...").assertDoesNotExist()
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Direct").assertCountEquals(2)
    }

    @Test
    fun testEditorialDownloadContent_emptyTabShowsViewAllSourcesButton() {
        val streamList = androidx.compose.runtime.mutableStateListOf<StreamItem>()
        var isAnyLoading by androidx.compose.runtime.mutableStateOf(false)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Empty Direct",
                allStreams = streamList,
                isLoading = false,
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                isAnyLoading = isAnyLoading,
                onDownloadClick = { _, _ -> }
            )
        }

        // Only torrents exist
        streamList.add(torrentHighSeed)
        composeTestRule.waitForIdle()

        // Switch to Direct tab
        composeTestRule.onNodeWithText("Direct").performClick()
        composeTestRule.waitForIdle()

        // Should show "No direct sources available" and "View all sources" button
        composeTestRule.onNodeWithText("No direct sources available").assertIsDisplayed()
        composeTestRule.onNodeWithText("View all sources").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Clicking "View all sources" switched back to All tab
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()
    }

    @Test
    fun testEditorialDownloadContent_rapidTwentyStreamAdditionsAndTabToggling() {
        val streamList = androidx.compose.runtime.mutableStateListOf<StreamItem>()
        var isAnyLoading by androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            EditorialDownloadContent(
                title = "Stress Test",
                allStreams = streamList,
                isLoading = isAnyLoading && streamList.isEmpty(),
                isError = false,
                activeDownloadStreamId = null,
                activeDownloadState = StreamDownloadState.IDLE,
                isAnyLoading = isAnyLoading,
                onDownloadClick = { _, _ -> }
            )
        }

        // Add 25 streams dynamically with interleaved tab toggling
        for (i in 1..25) {
            val isTorrent = i % 2 == 0
            val stream = if (isTorrent) {
                StreamItem(
                    name = "Torrent_$i",
                    addonId = "addon_${i % 3}",
                    addonName = "Addon",
                    seeders = i * 15,
                    magnetUrl = "magnet:?xt=urn:btih:hash_$i",
                    quality = if (i % 4 == 0) "4K 2160p" else "1080p",
                    size = "${i * 0.5} GB"
                )
            } else {
                StreamItem(
                    name = "Direct_$i",
                    addonId = "addon_${i % 3}",
                    addonName = "Addon",
                    url = "https://example.com/direct_$i.mp4",
                    quality = if (i % 3 == 0) "1080p" else "720p",
                    size = "${i * 0.3} GB"
                )
            }
            streamList.add(stream)

            if (i == 5) {
                composeTestRule.onNodeWithText("P2P").performClick()
            } else if (i == 10) {
                composeTestRule.onNodeWithText("Direct").performClick()
            } else if (i == 15) {
                composeTestRule.onNodeWithText("All").performClick()
            } else if (i == 20) {
                composeTestRule.onNodeWithText("P2P").performClick()
            }
            composeTestRule.waitForIdle()
        }

        isAnyLoading = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()
    }
}
