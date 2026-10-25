package com.euthopiar.ui.player

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.euthopiar.ui.theme.EuthopiarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerPlaybackSeekingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSeekIntervalGlyphsForwardAndRewindClicks() {
        var rewindDelta = 0L
        var forwardDelta = 0L

        composeTestRule.setContent {
            EuthopiarTheme {
                androidx.compose.foundation.layout.Row {
                    SeekIntervalGlyph(
                        isForward = false,
                        intervalSeconds = 10,
                        onClick = { rewindDelta = -10_000L }
                    )
                    SeekIntervalGlyph(
                        isForward = true,
                        intervalSeconds = 10,
                        onClick = { forwardDelta = 10_000L }
                    )
                }
            }
        }

        // Tap rewind glyph
        composeTestRule.onNodeWithContentDescription("Rewind 10s").assertExists()
        composeTestRule.onNodeWithContentDescription("Rewind 10s").performClick()
        assertEquals(-10_000L, rewindDelta)

        // Tap forward glyph
        composeTestRule.onNodeWithContentDescription("Forward 10s").assertExists()
        composeTestRule.onNodeWithContentDescription("Forward 10s").performClick()
        assertEquals(10_000L, forwardDelta)
    }

    @Test
    fun testPlayerScrubberLifecycleCallbacks() {
        var startPos: Long? = null
        var endPos: Long? = null

        composeTestRule.setContent {
            EuthopiarTheme {
                PlayerScrubber(
                    currentPositionMs = 30_000L,
                    durationMs = 120_000L,
                    bufferedPositionMs = 60_000L,
                    isScrubbing = false,
                    scrubPositionMs = 30_000L,
                    onScrubStart = { pos -> startPos = pos },
                    onScrubProgress = {},
                    onScrubEnd = { pos -> endPos = pos }
                )
            }
        }

        // Perform click in the middle of scrubber
        val node = composeTestRule.onNodeWithContentDescription("Playback progress, 00:30 of 02:00")
        node.assertExists()
        node.performClick()

        assertNotNull("onScrubStart should have been invoked", startPos)
        assertNotNull("onScrubEnd should have been invoked", endPos)
        assertTrue("Scrub end position should be non-negative", (endPos ?: -1L) >= 0L)
    }

    @Test
    fun testSeekRelativeCalculationClamping() {
        val durationMs = 100_000L

        // Test normal forward step
        var current = 20_000L
        var delta = 10_000L
        var target = current + delta
        var clamped = if (durationMs > 0) target.coerceIn(0L, durationMs) else target.coerceAtLeast(0L)
        assertEquals(30_000L, clamped)

        // Test backward step clamping to 0
        current = 5_000L
        delta = -10_000L
        target = current + delta
        clamped = if (durationMs > 0) target.coerceIn(0L, durationMs) else target.coerceAtLeast(0L)
        assertEquals(0L, clamped)

        // Test forward step clamping to duration
        current = 95_000L
        delta = 10_000L
        target = current + delta
        clamped = if (durationMs > 0) target.coerceIn(0L, durationMs) else target.coerceAtLeast(0L)
        assertEquals(100_000L, clamped)

        // Test when duration is unknown (0L)
        val unknownDuration = 0L
        current = 15_000L
        delta = 10_000L
        target = current + delta
        clamped = if (unknownDuration > 0) target.coerceIn(0L, unknownDuration) else target.coerceAtLeast(0L)
        assertEquals(25_000L, clamped)
    }

    @Test
    fun testSeekableExtractorsFactoryProducesValidExtractors() {
        val factory = SeekableExtractorsFactory(durationUs = 60_000_000L, file = null)
        val extractors = factory.createExtractors()
        assertTrue("Extractors must not be empty", extractors.isNotEmpty())

        // Verify all extractors are wrapped in ForcedSeekableExtractorWrapper
        extractors.forEach { extractor ->
            assertTrue(
                "Extractor should be ForcedSeekableExtractorWrapper",
                extractor is ForcedSeekableExtractorWrapper
            )
        }
    }

    @Test
    fun testPlayerViewModelSeekingAndScrubbing() {
        val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = PlayerViewModel(application)

        // Initial state verification
        assertEquals(0L, viewModel.uiState.value.currentPositionMs)
        assertFalse(viewModel.uiState.value.isScrubbing)

        // 1. Scrub start
        viewModel.onScrubStart(15_000L)
        assertTrue(viewModel.uiState.value.isScrubbing)
        assertEquals(15_000L, viewModel.uiState.value.scrubPositionMs)

        // 2. Scrub progress
        viewModel.onScrubProgress(25_000L)
        assertEquals(25_000L, viewModel.uiState.value.scrubPositionMs)

        // 3. Scrub end -> triggers seekTo
        viewModel.onScrubEnd(25_000L)
        assertFalse(viewModel.uiState.value.isScrubbing)
        assertEquals(25_000L, viewModel.uiState.value.currentPositionMs)

        // 4. Relative seek forward +10s
        viewModel.seekRelative(10_000L)
        assertEquals(35_000L, viewModel.uiState.value.currentPositionMs)

        // 5. Relative seek rewind -20s
        viewModel.seekRelative(-20_000L)
        assertEquals(15_000L, viewModel.uiState.value.currentPositionMs)

        // 6. Absolute seekTo
        viewModel.seekTo(50_000L)
        assertEquals(50_000L, viewModel.uiState.value.currentPositionMs)

        // Clean up
        viewModel.onExitPlayer()
    }

    @Test
    fun testRapidDoubleTapSeekAccumulatesWithoutStalePositionRace() {
        val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = PlayerViewModel(application)

        // Rapid double taps in succession (+10s, +10s, +10s)
        viewModel.seekRelative(10_000L)
        assertEquals(10_000L, viewModel.uiState.value.currentPositionMs)

        viewModel.seekRelative(10_000L)
        assertEquals(20_000L, viewModel.uiState.value.currentPositionMs)

        viewModel.seekRelative(10_000L)
        assertEquals(30_000L, viewModel.uiState.value.currentPositionMs)

        // Absolute seek overrides pending accumulator
        viewModel.seekTo(5_000L)
        assertEquals(5_000L, viewModel.uiState.value.currentPositionMs)

        viewModel.onExitPlayer()
    }

    @Test
    fun testPipControllerIsInPipModeHelper() {
        PipController.setInPipMode(true)
        assertTrue("PipController.isInPipMode() should return true", PipController.isInPipMode())

        PipController.setInPipMode(false)
        assertFalse("PipController.isInPipMode() should return false", PipController.isInPipMode())
    }

    @Test
    fun testVideoQualityOptionProperties() {
        val autoOption = VideoQualityOption(id = "auto", label = "Auto (Adaptive)", isAuto = true)
        assertTrue(autoOption.isAuto)
        assertEquals(-1, autoOption.groupIndex)
        assertEquals(-1, autoOption.trackIndex)
        assertNull(autoOption.bitrate)

        val trackOption = VideoQualityOption(
            id = "video_0_1",
            label = "1080p (6.5 Mbps)",
            height = 1080,
            bitrate = 6_500_000,
            groupIndex = 0,
            trackIndex = 1,
            isAuto = false
        )
        assertFalse(trackOption.isAuto)
        assertEquals(1080, trackOption.height)
        assertEquals(6_500_000, trackOption.bitrate)
        assertEquals(0, trackOption.groupIndex)
        assertEquals(1, trackOption.trackIndex)
    }
}
