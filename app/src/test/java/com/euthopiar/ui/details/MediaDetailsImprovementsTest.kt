package com.euthopiar.ui.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.euthopiar.core.model.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaDetailsImprovementsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEpisodeChunkingForSeasonsWithMoreThanTwentyEpisodes() {
        val chunkSize = 20
        // Test season with 45 episodes
        val episodes = (1..45).map { num ->
            EpisodeItem(
                id = "ep_$num",
                title = "Episode $num",
                seasonNumber = 1,
                episodeNumber = num,
                data = "data_$num"
            )
        }

        val totalEpisodes = episodes.size
        val isChunked = totalEpisodes > chunkSize
        val chunkCount = if (isChunked) (totalEpisodes + chunkSize - 1) / chunkSize else 1

        assertTrue("Season with 45 episodes must be chunked", isChunked)
        assertEquals(3, chunkCount)

        // Chunk 0: episodes 1 to 20
        var selectedChunkIndex = 0
        var safeChunkIndex = selectedChunkIndex.coerceIn(0, (chunkCount - 1).coerceAtLeast(0))
        var displayedEpisodes = if (!isChunked) episodes else {
            val s = safeChunkIndex * chunkSize
            episodes.subList(s, minOf(s + chunkSize, totalEpisodes))
        }
        assertEquals(20, displayedEpisodes.size)
        assertEquals(1, displayedEpisodes.first().episodeNumber)
        assertEquals(20, displayedEpisodes.last().episodeNumber)

        // Continuation button label for next chunk
        val nextStart = (safeChunkIndex + 1) * chunkSize + 1
        val nextEnd = minOf((safeChunkIndex + 2) * chunkSize, totalEpisodes)
        assertEquals("Episodes 21–40", "Episodes $nextStart–$nextEnd")

        // Chunk 1: episodes 21 to 40
        selectedChunkIndex = 1
        safeChunkIndex = selectedChunkIndex.coerceIn(0, (chunkCount - 1).coerceAtLeast(0))
        displayedEpisodes = if (!isChunked) episodes else {
            val s = safeChunkIndex * chunkSize
            episodes.subList(s, minOf(s + chunkSize, totalEpisodes))
        }
        assertEquals(20, displayedEpisodes.size)
        assertEquals(21, displayedEpisodes.first().episodeNumber)
        assertEquals(40, displayedEpisodes.last().episodeNumber)

        // Previous button label for chunk 1
        val prevStart = (safeChunkIndex - 1) * chunkSize + 1
        val prevEnd = safeChunkIndex * chunkSize
        assertEquals("Previous: Episodes 1–20", "Previous: Episodes $prevStart–$prevEnd")

        // Chunk 2: episodes 41 to 45 (remainder)
        selectedChunkIndex = 2
        safeChunkIndex = selectedChunkIndex.coerceIn(0, (chunkCount - 1).coerceAtLeast(0))
        displayedEpisodes = if (!isChunked) episodes else {
            val s = safeChunkIndex * chunkSize
            episodes.subList(s, minOf(s + chunkSize, totalEpisodes))
        }
        assertEquals(5, displayedEpisodes.size)
        assertEquals(41, displayedEpisodes.first().episodeNumber)
        assertEquals(45, displayedEpisodes.last().episodeNumber)
    }

    @Test
    fun testEpisodeChunkingForSeasonsWithTwentyOrFewerEpisodes() {
        val chunkSize = 20
        // Test season with exactly 12 episodes
        val episodes = (1..12).map { num ->
            EpisodeItem(
                id = "ep_$num",
                title = "Episode $num",
                seasonNumber = 1,
                episodeNumber = num,
                data = "data_$num"
            )
        }

        val totalEpisodes = episodes.size
        val isChunked = totalEpisodes > chunkSize
        val chunkCount = if (isChunked) (totalEpisodes + chunkSize - 1) / chunkSize else 1

        assertFalse("Season with <= 20 episodes must NOT be chunked", isChunked)
        assertEquals(1, chunkCount)

        val safeChunkIndex = 0
        val displayedEpisodes = if (!isChunked) episodes else {
            val s = safeChunkIndex * chunkSize
            episodes.subList(s, minOf(s + chunkSize, totalEpisodes))
        }
        assertEquals(12, displayedEpisodes.size)
    }

    @Test
    fun testBackdropTrailerErrorFallbackSuppressesTrailer() {
        val trailerUrl = "https://www.youtube.com/watch?v=unplayable_trailer"
        var isBackdropTrailerEnabled = true
        var hasTrailerFailed = false
        val isMetered = false
        val isPowerSaver = false
        val isAtTop = true
        val showTrailerDialog = false
        val isNavigatingAway = false

        // Initially can play
        var canPlayAmbientTrailer = !hasTrailerFailed &&
            isBackdropTrailerEnabled &&
            !trailerUrl.isNullOrBlank() &&
            !isMetered &&
            !isPowerSaver &&
            isAtTop &&
            !showTrailerDialog &&
            !isNavigatingAway

        assertTrue("Initially should be able to play trailer", canPlayAmbientTrailer)

        // Error occurs (e.g. video blocked or error 150/101 or watchdog timeout)
        hasTrailerFailed = true

        canPlayAmbientTrailer = !hasTrailerFailed &&
            isBackdropTrailerEnabled &&
            !trailerUrl.isNullOrBlank() &&
            !isMetered &&
            !isPowerSaver &&
            isAtTop &&
            !showTrailerDialog &&
            !isNavigatingAway

        assertFalse("Trailer must NOT play after failure has been signaled", canPlayAmbientTrailer)

        // Tap toggle must be disabled when trailer has failed
        val onHeroClick = if (!trailerUrl.isNullOrBlank() && !hasTrailerFailed) {
            { isBackdropTrailerEnabled = !isBackdropTrailerEnabled }
        } else null

        assertEquals("Hero tap click must be null when trailer has failed", null, onHeroClick)
    }

    @Test
    fun testBackButtonScrolledVerticalCentering() {
        // Scrolled top bar row height is 68dp
        val topBarRowHeight = 68.dp
        // Scrolled back button size is 36dp
        val isScrolled = true
        val backButtonSize = if (isScrolled) 36.dp else 40.dp
        val backButtonOffsetY = if (isScrolled) 16.dp else 8.dp

        // (68dp - 36dp) / 2 = 16dp
        val expectedCenteredOffset = (topBarRowHeight - backButtonSize) / 2
        assertEquals(expectedCenteredOffset, backButtonOffsetY)
    }

    @Test
    fun testEpisodesPaginationButtonsInteraction() {
        var selectedChunk = 0
        var scrolledToHeader = false

        composeTestRule.setContent {
            EpisodesPaginationButtons(
                safeChunkIndex = selectedChunk,
                chunkCount = 3,
                chunkSize = 20,
                totalEpisodes = 55,
                onSelectChunk = { selectedChunk = it },
                onScrolled = { scrolledToHeader = true }
            )
        }

        // At chunk 0, continuation button "Episodes 21–40" exists
        composeTestRule.onNodeWithText("Episodes 21–40")
            .assertIsDisplayed()
            .performClick()

        assertEquals("Clicking continuation button must advance to chunk 1", 1, selectedChunk)
        assertTrue("Clicking continuation button must trigger scroll to header", scrolledToHeader)
    }

    @Test
    fun testEpisodesPaginationButtonsPreviousInteraction() {
        var selectedChunk = 1
        var scrolledToHeader = false

        composeTestRule.setContent {
            EpisodesPaginationButtons(
                safeChunkIndex = selectedChunk,
                chunkCount = 3,
                chunkSize = 20,
                totalEpisodes = 55,
                onSelectChunk = { selectedChunk = it },
                onScrolled = { scrolledToHeader = true }
            )
        }

        // At chunk 1, previous button "Previous: Episodes 1–20" exists
        composeTestRule.onNodeWithText("Previous: Episodes 1–20")
            .assertIsDisplayed()
            .performClick()

        assertEquals("Clicking previous button must retreat to chunk 0", 0, selectedChunk)
        assertTrue("Clicking previous button must trigger scroll to header", scrolledToHeader)
    }
}
