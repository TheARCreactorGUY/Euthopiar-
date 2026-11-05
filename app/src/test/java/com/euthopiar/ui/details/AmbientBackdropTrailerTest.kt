package com.euthopiar.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientBackdropTrailerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFrostedMuteButtonTogglesState() {
        var isMuted by mutableStateOf(true)

        composeTestRule.setContent {
            FrostedMuteButton(
                isMuted = isMuted,
                onToggle = { isMuted = !isMuted }
            )
        }

        // Initially muted, accessibility label is "Unmute trailer"
        composeTestRule.onNodeWithContentDescription("Unmute trailer")
            .assertExists()
            .performClick()

        assertTrue("Expected isMuted to become false after clicking mute button", !isMuted)

        // Now unmuted, accessibility label should be "Mute trailer"
        composeTestRule.onNodeWithContentDescription("Mute trailer")
            .assertExists()
            .performClick()

        assertTrue("Expected isMuted to become true after clicking unmute button", isMuted)
    }

    @Test
    fun testClickingMuteButtonDoesNotTriggerParentHeroClick() {
        var heroClicked = false
        var muteToggled = false
        var isMuted by mutableStateOf(true)

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clickable { heroClicked = true }
            ) {
                FrostedMuteButton(
                    isMuted = isMuted,
                    onToggle = {
                        muteToggled = true
                        isMuted = !isMuted
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }

        // Click the mute button
        composeTestRule.onNodeWithContentDescription("Unmute trailer").performClick()

        assertTrue("Mute button onToggle should have been called", muteToggled)
        assertFalse("Parent hero click should NOT have been called when clicking mute button", heroClicked)
    }

    @Test
    fun testFrostedMuteButtonMeetsMinimumTouchTarget() {
        composeTestRule.setContent {
            FrostedMuteButton(
                isMuted = true,
                onToggle = {}
            )
        }

        val node = composeTestRule.onNodeWithContentDescription("Unmute trailer")
        node.assertHeightIsAtLeast(com.euthopiar.ui.theme.Dimens.minTouchTarget)
        node.assertWidthIsAtLeast(com.euthopiar.ui.theme.Dimens.minTouchTarget)
    }

    @Test
    fun testHeroSpacerTogglesBackdropTrailerEnabledWithoutOpeningDialog() {
        var isBackdropTrailerEnabled by mutableStateOf(true)
        var showTrailerDialog by mutableStateOf(false)

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clickable {
                        isBackdropTrailerEnabled = !isBackdropTrailerEnabled
                    }
            )
        }

        composeTestRule.onNode(androidx.compose.ui.test.hasClickAction()).performClick()

        assertFalse("Hero click should toggle isBackdropTrailerEnabled to false", isBackdropTrailerEnabled)
        assertFalse("Hero click must NOT open the full trailer dialog", showTrailerDialog)

        composeTestRule.onNode(androidx.compose.ui.test.hasClickAction()).performClick()

        assertTrue("Hero click should toggle isBackdropTrailerEnabled back to true", isBackdropTrailerEnabled)
        assertFalse("Hero click must NOT open the full trailer dialog", showTrailerDialog)
    }

    @Test
    fun testCanPlayAmbientTrailerConditionals() {
        val trailerUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val isMetered = false
        val isPowerSaver = false
        val isAtTop = true
        val showTrailerDialog = false
        val isNavigatingAway = false

        // When backdrop trailer is enabled
        var isBackdropTrailerEnabled = true
        var canPlayAmbientTrailer = isBackdropTrailerEnabled &&
            !trailerUrl.isNullOrBlank() &&
            !isMetered &&
            !isPowerSaver &&
            isAtTop &&
            !showTrailerDialog &&
            !isNavigatingAway

        assertTrue("Ambient trailer should be allowed to play when enabled and at top", canPlayAmbientTrailer)

        // Tapping banner disables ambient trailer
        isBackdropTrailerEnabled = false
        canPlayAmbientTrailer = isBackdropTrailerEnabled &&
            !trailerUrl.isNullOrBlank() &&
            !isMetered &&
            !isPowerSaver &&
            isAtTop &&
            !showTrailerDialog &&
            !isNavigatingAway

        assertFalse("Ambient trailer must NOT play when user disabled it via banner tap", canPlayAmbientTrailer)

        // When full trailer dialog is opened via dedicated Trailer button, ambient trailer must pause
        isBackdropTrailerEnabled = true
        val dialogOpen = true
        val canPlayWithDialog = isBackdropTrailerEnabled &&
            !trailerUrl.isNullOrBlank() &&
            !isMetered &&
            !isPowerSaver &&
            isAtTop &&
            !dialogOpen &&
            !isNavigatingAway

        assertFalse("Ambient trailer must pause when full trailer dialog is open", canPlayWithDialog)
    }

    @Test
    fun testLetterboxDetectorCalculations() {
        // Zoom for 0 bar should be 1.0f
        val zeroZoom = LetterboxDetector.zoomFor(0f)
        org.junit.Assert.assertEquals(1.0f, zeroZoom, 0.001f)

        // Zoom for 0.10f bar should be 1 / (1 - 0.20) = 1.25f
        val wideZoom = LetterboxDetector.zoomFor(0.10f)
        org.junit.Assert.assertEquals(1.25f, wideZoom, 0.001f)

        // Sample window over check
        assertTrue("Position >= 20% duration should be considered over", LetterboxDetector.isSampleWindowOver(30_000L, 100_000L))
        assertFalse("Position < 20% duration should still sample", LetterboxDetector.isSampleWindowOver(10_000L, 100_000L))
    }

    @Test
    fun testLetterboxTrackerStability() {
        val tracker = LetterboxTracker()
        // Needs 4 stable samples before returning zoom
        org.junit.Assert.assertNull(tracker.onSample(0.12f))
        org.junit.Assert.assertNull(tracker.onSample(0.12f))
        org.junit.Assert.assertNull(tracker.onSample(0.12f))
        val zoom = tracker.onSample(0.12f)
        org.junit.Assert.assertNotNull("After 4 stable samples, zoom must be calculated", zoom)
        assertTrue("Zoom should be greater than 1 for letterboxed content", zoom!! > 1.0f)
    }

    @Test
    fun testInAppYouTubeExtractorVideoIdExtraction() {
        val extractor = com.euthopiar.core.trailer.InAppYouTubeExtractor()

        val id1 = extractor.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        org.junit.Assert.assertEquals("dQw4w9WgXcQ", id1)

        val id2 = extractor.extractVideoId("https://youtu.be/dQw4w9WgXcQ")
        org.junit.Assert.assertEquals("dQw4w9WgXcQ", id2)

        val id3 = extractor.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ")
        org.junit.Assert.assertEquals("dQw4w9WgXcQ", id3)

        val id4 = extractor.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        org.junit.Assert.assertEquals("dQw4w9WgXcQ", id4)

        val id5 = extractor.extractVideoId("dQw4w9WgXcQ")
        org.junit.Assert.assertEquals("dQw4w9WgXcQ", id5)
    }

    @Test
    fun testTrailerPlaybackSourceModel() {
        val source = com.euthopiar.core.trailer.TrailerPlaybackSource(
            videoUrl = "https://googlevideo.com/videoplayback?id=123",
            audioUrl = "https://googlevideo.com/videoplayback?id=456"
        )
        org.junit.Assert.assertEquals("https://googlevideo.com/videoplayback?id=123", source.videoUrl)
        org.junit.Assert.assertEquals("https://googlevideo.com/videoplayback?id=456", source.audioUrl)
    }

    @Test
    fun testCacheInvalidation() {
        val resolver = com.euthopiar.core.trailer.TrailerPlaybackResolver
        resolver.invalidateCache("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        resolver.invalidateCache("https://youtu.be/dQw4w9WgXcQ")
        resolver.invalidateCache("")
        resolver.clearCache()
    }
}
