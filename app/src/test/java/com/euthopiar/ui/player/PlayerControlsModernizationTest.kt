package com.euthopiar.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.euthopiar.ui.theme.EuthopiarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControlsModernizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPlayerActionDockRendersAllAnchorsCompactly() {
        var episodesClicked = false
        var streamClicked = false
        var audioSubtitlesClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            EuthopiarTheme {
                PlayerActionDock(
                    isTvSeries = true,
                    activeSidePanel = PlayerSidePanelType.NONE,
                    onOpenEpisodes = { episodesClicked = true },
                    onOpenSources = { streamClicked = true },
                    onOpenAudioSubtitles = { audioSubtitlesClicked = true },
                    onOpenTuning = { settingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Episodes").assertExists()
        composeTestRule.onNodeWithContentDescription("Stream Sources").assertExists()
        composeTestRule.onNodeWithContentDescription("Audio Tracks").assertExists()
        composeTestRule.onNodeWithContentDescription("Subtitles").assertExists()
        composeTestRule.onNodeWithContentDescription("Player Settings").assertExists()

        composeTestRule.onNodeWithContentDescription("Episodes").performClick()
        assertTrue("Episodes clicked", episodesClicked)

        composeTestRule.onNodeWithContentDescription("Stream Sources").performClick()
        assertTrue("Stream clicked", streamClicked)

        composeTestRule.onNodeWithContentDescription("Audio Tracks").performClick()
        assertTrue("Audio & subtitles clicked", audioSubtitlesClicked)

        composeTestRule.onNodeWithContentDescription("Player Settings").performClick()
        assertTrue("Settings clicked", settingsClicked)
    }

    @Test
    fun testSpeedAccordionExpandsToLeftAndSelectsSpeed() {
        var selectedSpeed = 1.0f

        composeTestRule.setContent {
            EuthopiarTheme {
                PlayerBottomControls(
                    visible = true,
                    isScrubbing = false,
                    currentPositionMs = 10000L,
                    durationMs = 60000L,
                    bufferedPositionMs = 30000L,
                    scrubPositionMs = 10000L,
                    playbackSpeed = 1.0f,
                    isTvSeries = false,
                    hasNextEpisode = false,
                    onScrubStart = {},
                    onScrubProgress = {},
                    onScrubEnd = {},
                    onOpenSpeed = {},
                    onSelectSpeed = { speed -> selectedSpeed = speed },
                    onOpenAudioSubtitles = {},
                    onOpenSettings = {},
                    onNextEpisode = {}
                )
            }
        }

        // Initially compact shows "Speed"
        composeTestRule.onNodeWithText("Speed").assertExists()

        // Tap Speed to expand accordion
        composeTestRule.onNodeWithText("Speed").performClick()

        // After expansion, options 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x are exposed
        composeTestRule.onNodeWithText("1.5x").assertExists()
        composeTestRule.onNodeWithText("1.5x").performClick()

        assertEquals(1.5f, selectedSpeed, 0.001f)
    }

    @Test
    fun testAspectRatioAccordionExpandsAndSelectsMode() {
        var selectedMode: AspectRatioMode = AspectRatioMode.FIT

        composeTestRule.setContent {
            EuthopiarTheme {
                PlayerBottomControls(
                    visible = true,
                    isScrubbing = false,
                    currentPositionMs = 10000L,
                    durationMs = 60000L,
                    bufferedPositionMs = 30000L,
                    scrubPositionMs = 10000L,
                    playbackSpeed = 1.0f,
                    isTvSeries = false,
                    hasNextEpisode = false,
                    aspectRatioMode = AspectRatioMode.FIT,
                    onScrubStart = {},
                    onScrubProgress = {},
                    onScrubEnd = {},
                    onOpenSpeed = {},
                    onOpenAudioSubtitles = {},
                    onSelectAspectRatio = { mode -> selectedMode = mode },
                    onOpenSettings = {},
                    onNextEpisode = {}
                )
            }
        }

        // Initially compact shows "Fit"
        composeTestRule.onNodeWithText("Fit").assertExists()

        // Tap Fit to expand accordion
        composeTestRule.onNodeWithText("Fit").performClick()

        // After expansion, "Fill", "16:9", "Stretch" are exposed
        composeTestRule.onNodeWithText("Fill").assertExists()
        composeTestRule.onNodeWithText("Fill").performClick()

        assertEquals(AspectRatioMode.ZOOM, selectedMode)
    }

    @Test
    fun testQuickSourceSwitchPopoverQualityFilters() {
        val testServers = listOf(
            ServerSourceOption(
                id = "s1",
                label = "Server 1",
                serverName = "Vidcloud",
                resolutionLabel = "1080p",
                streamSource = com.euthopiar.core.model.StreamSource(
                    url = "https://example.com/1",
                    quality = "1080p"
                )
            ),
            ServerSourceOption(
                id = "s2",
                label = "Server 2",
                serverName = "UpCloud",
                resolutionLabel = "4K",
                streamSource = com.euthopiar.core.model.StreamSource(
                    url = "https://example.com/2",
                    quality = "4K"
                )
            )
        )

        composeTestRule.setContent {
            EuthopiarTheme {
                QuickSourceSwitchPopover(
                    visible = true,
                    servers = testServers,
                    selectedServer = testServers[0],
                    onSelectServer = {},
                    onOpenAllSources = {},
                    onDismiss = {}
                )
            }
        }

        // All 4 quality filter chips exist
        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("4K").assertExists()
        composeTestRule.onNode(hasText("1080p") and !hasText("Vidcloud")).assertExists()
        composeTestRule.onNodeWithText("720p").assertExists()

        // Tap 4K chip to filter
        composeTestRule.onNodeWithText("4K").performClick()
        composeTestRule.onNodeWithText("UpCloud").assertExists()
    }
}
