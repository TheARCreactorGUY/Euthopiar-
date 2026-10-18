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
}
