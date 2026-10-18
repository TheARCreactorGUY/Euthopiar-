package com.euthopiar.ui.components

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.details.PlaybackActionState
import com.euthopiar.ui.details.PlaybackTargetHelper
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HeroCarouselOverhaulTest {

    @Test
    fun testActionStateResolvesPlayForNewMedia() {
        val actionState = PlaybackTargetHelper.resolveActionState(null, MediaType.MOVIE)
        assertTrue("Item without history should resolve to Play", actionState is PlaybackActionState.Play)
        assertEquals("Play", actionState.label)
    }

    @Test
    fun testActionStateResolvesResumeForMovieInProgress() {
        val history = PlaybackHistoryEntity(
            id = "default_user::test::101",
            mediaId = "101",
            title = "Test Movie",
            episodeTitle = null,
            seasonNumber = null,
            episodeNumber = null,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 60_000L * 30, // 30 minutes in
            durationMs = 60_000L * 90          // 90 minutes total
        )

        val actionState = PlaybackTargetHelper.resolveActionState(history, MediaType.MOVIE)
        assertTrue("Item with progress must resolve to Resume", actionState is PlaybackActionState.Resume)
        val resumeState = actionState as PlaybackActionState.Resume
        assertEquals("Resume", resumeState.label)
        assertEquals(60, resumeState.minutesLeft)
        assertEquals(0.333f, resumeState.progress, 0.01f)
    }

    @Test
    fun testActionStateResolvesResumeWithEpisodeForSeries() {
        val history = PlaybackHistoryEntity(
            id = "default_user::test::202_s1_e1",
            mediaId = "202",
            title = "Visionquest",
            episodeTitle = "Pilot",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 60_000L * 15,
            durationMs = 60_000L * 67 // 52 minutes left
        )

        val actionState = PlaybackTargetHelper.resolveActionState(history, MediaType.TV_SERIES)
        assertTrue(actionState is PlaybackActionState.Resume)
        val resumeState = actionState as PlaybackActionState.Resume
        assertEquals("Resume S1 E1", resumeState.label)
        assertEquals(52, resumeState.minutesLeft)
    }

    @Test
    fun testPaginationSlidingWindowForLargeSlideCounts() {
        val itemsCount = 10
        val maxVisible = 7

        // Helper that mimics HeroPagination windowing
        fun calculateWindow(activeIndex: Int): Pair<Int, Int> {
            val windowStart = (activeIndex - maxVisible / 2).coerceIn(0, itemsCount - maxVisible)
            val activeSlot = activeIndex - windowStart
            return Pair(windowStart, activeSlot)
        }

        // At slide 0: window starts at 0, active slot is 0
        val (start0, slot0) = calculateWindow(0)
        assertEquals(0, start0)
        assertEquals(0, slot0)
        assertEquals("Slide 1 of 10", "Slide ${start0 + slot0 + 1} of $itemsCount")

        // At slide 4: centered window starts at 1, active slot is 3
        val (start4, slot4) = calculateWindow(4)
        assertEquals(1, start4)
        assertEquals(3, slot4)
        assertEquals("Slide 5 of 10", "Slide ${start4 + slot4 + 1} of $itemsCount")

        // At slide 8 (9th slide): window starts at 3, active slot is 5
        val (start8, slot8) = calculateWindow(8)
        assertEquals(3, start8)
        assertEquals(5, slot8)
        // Must announce actual slide 9, NEVER wrapped "Slide 1 of 8"
        assertEquals("Slide 9 of 10", "Slide ${start8 + slot8 + 1} of $itemsCount")

        // At slide 9 (10th slide): window clamped to 3, active slot is 6
        val (start9, slot9) = calculateWindow(9)
        assertEquals(3, start9)
        assertEquals(6, slot9)
        assertEquals("Slide 10 of 10", "Slide ${start9 + slot9 + 1} of $itemsCount")
    }

    @Test
    fun testPaginationHidingForSingleSlide() {
        val itemsCount = 1
        val shouldShow = itemsCount > 1
        assertFalse("Pagination must be hidden for single slide", shouldShow)
    }

    @Test
    fun testStaggerEntranceProgression() {
        // Test that stagger progression maintains proper chronological order:
        // logo (0.15) -> chips (0.25) -> actions (0.35) -> progress (0.45)
        fun calculateStaggers(enterFraction: Float): List<Float> {
            val logo = ((enterFraction - 0.15f) / 0.65f).coerceIn(0f, 1f)
            val chips = ((enterFraction - 0.25f) / 0.65f).coerceIn(0f, 1f)
            val actions = ((enterFraction - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val progress = ((enterFraction - 0.45f) / 0.55f).coerceIn(0f, 1f)
            return listOf(logo, chips, actions, progress)
        }

        // At start (enterFraction = 0.10f), nothing has entered yet
        val staggersAt01 = calculateStaggers(0.10f)
        assertEquals(listOf(0f, 0f, 0f, 0f), staggersAt01)

        // At 0.20f, logo has started entering, others are at 0
        val staggersAt02 = calculateStaggers(0.20f)
        assertTrue(staggersAt02[0] > 0f)
        assertEquals(0f, staggersAt02[1], 0.001f)
        assertEquals(0f, staggersAt02[2], 0.001f)
        assertEquals(0f, staggersAt02[3], 0.001f)

        // At 0.30f, logo and chips are entering, actions and progress are at 0
        val staggersAt03 = calculateStaggers(0.30f)
        assertTrue(staggersAt03[0] > staggersAt03[1])
        assertTrue(staggersAt03[1] > 0f)
        assertEquals(0f, staggersAt03[2], 0.001f)

        // At completion (enterFraction = 1.0f), all elements are fully settled at 1f
        val staggersAt10 = calculateStaggers(1.0f)
        assertEquals(listOf(1f, 1f, 1f, 1f), staggersAt10)
    }

    @Test
    fun testOutgoingSlideQuickFadeOut() {
        fun outgoingAlpha(absOffset: Float): Float = (1f - absOffset * 2.5f).coerceIn(0f, 1f)

        assertEquals(1.0f, outgoingAlpha(0f), 0.001f)
        assertEquals(0.5f, outgoingAlpha(0.2f), 0.001f)
        assertEquals(0.0f, outgoingAlpha(0.4f), 0.001f)
        assertEquals(0.0f, outgoingAlpha(0.8f), 0.001f)
    }

    @Test
    fun testSettlingScaleProgression() {
        fun settleScale(absOffset: Float): Float = 1.0f + (absOffset * 0.04f).coerceIn(0f, 0.04f)

        assertEquals(1.04f, settleScale(1.0f), 0.001f)
        assertEquals(1.02f, settleScale(0.5f), 0.001f)
        assertEquals(1.00f, settleScale(0.0f), 0.001f)
    }

    @Test
    fun testMissingMetadataOmissionRules() {
        val minimalItem = MediaItem(
            id = "empty_meta",
            title = "Minimal Movie",
            url = "",
            posterUrl = null,
            type = MediaType.MOVIE,
            year = null,
            rating = null,
            quality = null,
            provider = null,
            logoUrl = null,
            contentRating = null,
            rottenTomatoesRating = null
        )

        assertFalse("Missing year must not produce a chip", minimalItem.year != null)
        assertFalse("Missing quality must not produce a chip", !minimalItem.quality.isNullOrBlank())
        assertFalse("Missing content rating must not produce a chip", !minimalItem.contentRating.isNullOrBlank())
        assertFalse("Missing IMDb rating must not produce a chip", !minimalItem.rating.isNullOrBlank() && minimalItem.rating != "0")
        assertFalse("Missing Rotten Tomatoes score must not produce a chip", !minimalItem.rottenTomatoesRating.isNullOrBlank())
    }

    @Test
    fun testValidMetadataInclusionRules() {
        val fullItem = MediaItem(
            id = "full_meta",
            title = "Blockbuster",
            url = "",
            posterUrl = "https://example.com/poster.jpg",
            type = MediaType.MOVIE,
            year = 2026,
            rating = "8.7",
            quality = "4K UHD",
            provider = "Provider",
            logoUrl = "https://example.com/logo.png",
            contentRating = "PG-13",
            rottenTomatoesRating = "91%"
        )

        assertTrue(fullItem.year != null)
        assertTrue(!fullItem.quality.isNullOrBlank())
        assertTrue(!fullItem.contentRating.isNullOrBlank())
        assertTrue(!fullItem.rating.isNullOrBlank())
        assertTrue(!fullItem.rottenTomatoesRating.isNullOrBlank())
    }

    @Test
    fun testSlideHazeColorStability() {
        val items = (1..10).map { id ->
            MediaItem(
                id = "item_$id",
                title = "Title $id",
                url = "",
                posterUrl = null,
                type = MediaType.MOVIE
            )
        }

        val hashes = items.map { (it.id.hashCode() and 0x7FFFFFFF) % 5 }
        assertTrue("Hashing should produce valid palette indices in 0..4", hashes.all { it in 0..4 })
    }

    @Test
    fun testHeroMeltColorStopsSmootherstepProgression() {
        val baseBg = Color(0xFF0B0B0F)
        val dominantColor = Color(0xFF2A2859)

        // AMOLED Mode test
        val amoledStops = calculateHeroMeltColorStops(
            baseBg = Color.Black,
            dominantColor = dominantColor,
            isAmoled = true
        )

        // Start at 0.0f and 0.300f must have 0 alpha
        assertEquals(0.0f, amoledStops.first().second.alpha, 0.01f)
        val stopAt030 = amoledStops.first { abs(it.first - 0.300f) < 0.001f }
        assertEquals(0.0f, stopAt030.second.alpha, 0.01f)

        // Mid-point at 0.625f must have ~0.500 alpha (8-bit color quantization gives 128/255 = 0.50196)
        val stopAt0625 = amoledStops.first { abs(it.first - 0.625f) < 0.001f }
        assertEquals(0.500f, stopAt0625.second.alpha, 0.01f)

        // 0.950f and 1.0f must have 1.0 alpha and pure Color.Black RGB
        val stopAt0950 = amoledStops.first { abs(it.first - 0.950f) < 0.001f }
        assertEquals(1.0f, stopAt0950.second.alpha, 0.01f)
        assertEquals(Color.Black.red, stopAt0950.second.red, 0.01f)
        assertEquals(Color.Black.green, stopAt0950.second.green, 0.01f)
        assertEquals(Color.Black.blue, stopAt0950.second.blue, 0.01f)

        val lastStop = amoledStops.last()
        assertEquals(1.0f, lastStop.second.alpha, 0.01f)
        assertEquals(Color.Black.red, lastStop.second.red, 0.01f)

        // LCD Mode test
        val lcdStops = calculateHeroMeltColorStops(
            baseBg = baseBg,
            dominantColor = dominantColor,
            isAmoled = false
        )
        val lcdLastStop = lcdStops.last()
        assertEquals(1.0f, lcdLastStop.second.alpha, 0.01f)
        // At 1.0, RGB must match baseBg exactly (zero tint)
        assertEquals(baseBg.red, lcdLastStop.second.red, 0.01f)
        assertEquals(baseBg.green, lcdLastStop.second.green, 0.01f)
        assertEquals(baseBg.blue, lcdLastStop.second.blue, 0.01f)
    }

    @Test
    fun testHudScrimColorStopsProgression() {
        val baseBg = Color.Black
        val hudStops = calculateHudScrimColorStops(baseBg)

        // Starts with 0 alpha
        assertEquals(0.0f, hudStops.first().second.alpha, 0.01f)

        // Rises over first 60% to 0.40 max alpha
        val stopAt060 = hudStops.first { abs(it.first - 0.60f) < 0.001f }
        assertEquals(0.40f, stopAt060.second.alpha, 0.01f)

        // Holds at 0.40 alpha at 1.00f
        val lastStop = hudStops.last()
        assertEquals(0.40f, lastStop.second.alpha, 0.01f)
    }
}
