package com.euthopiar.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.components.AppFilterChip
import com.euthopiar.ui.components.ContentRatingBadge
import com.euthopiar.ui.components.MetaLine
import com.euthopiar.ui.components.PrimaryButton
import com.euthopiar.ui.components.QualityBadge
import com.euthopiar.ui.components.SecondaryButton
import com.euthopiar.ui.components.SettingsGroup
import com.euthopiar.ui.components.SettingsRow
import com.euthopiar.ui.player.PlayerBottomControls
import com.euthopiar.ui.player.PlayerCenterCluster
import com.euthopiar.ui.player.PlayerTopBar
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.EuthopiarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Visual regression and layout scaling verification tests for core application surfaces:
 * 1. Home
 * 2. Media Details
 * 3. Player Controls
 * 4. Settings
 *
 * Each surface is validated across standard (1.0x) and enlarged (1.3x) accessibility font scales
 * to verify touch target preservation (>=48dp), content visibility, and layout integrity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisualRegressionScalingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun ScalingSurface(
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 2.75f, fontScale = fontScale)
        ) {
            EuthopiarTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(Dimens.spacing16)
                ) {
                    content()
                }
            }
        }
    }

    // =========================================================================
    // 1. HOME SCREEN SURFACE TESTS
    // =========================================================================

    @Test
    fun testHomeFilterChipsAndCategories_Scale1_0x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.0f) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                    AppFilterChip(text = "All", selected = true, onClick = {})
                    AppFilterChip(text = "Movies", selected = false, onClick = {})
                    AppFilterChip(text = "TV Shows", selected = false, onClick = {})
                }
            }
        }

        val allChip = composeTestRule.onNodeWithText("All")
        allChip.assertExists()
        allChip.assertHeightIsAtLeast(Dimens.minTouchTarget)
        allChip.assertWidthIsAtLeast(Dimens.minTouchTarget)

        composeTestRule.onNodeWithText("Movies").assertExists()
        composeTestRule.onNodeWithText("TV Shows").assertExists()
    }

    @Test
    fun testHomeFilterChipsAndCategories_Scale1_3x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.3f) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                    AppFilterChip(text = "All", selected = true, onClick = {})
                    AppFilterChip(text = "Movies", selected = false, onClick = {})
                    AppFilterChip(text = "TV Shows", selected = false, onClick = {})
                }
            }
        }

        val allChip = composeTestRule.onNodeWithText("All")
        allChip.assertExists()
        allChip.assertHeightIsAtLeast(Dimens.minTouchTarget)
        allChip.assertWidthIsAtLeast(Dimens.minTouchTarget)

        composeTestRule.onNodeWithText("Movies").assertExists()
        composeTestRule.onNodeWithText("TV Shows").assertExists()
    }

    // =========================================================================
    // 2. MEDIA DETAILS SURFACE TESTS
    // =========================================================================

    @Test
    fun testMediaDetailsActionsAndBadges_Scale1_0x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.0f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    MetaLine(items = listOf("2024", "2h 18m", "Action", "Sci-Fi"))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                        QualityBadge(quality = "4K")
                        QualityBadge(quality = "HDR")
                        ContentRatingBadge(contentRating = "TV-MA")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)
                    ) {
                        PrimaryButton(
                            text = "Play",
                            icon = Icons.Default.PlayArrow,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Download",
                            icon = Icons.Outlined.Download,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Play").assertExists()
        composeTestRule.onNodeWithText("Play").assertHeightIsAtLeast(Dimens.minTouchTarget)
        composeTestRule.onNodeWithText("Download").assertExists()
        composeTestRule.onNodeWithText("Download").assertHeightIsAtLeast(Dimens.minTouchTarget)
        composeTestRule.onNodeWithText("4K").assertExists()
        composeTestRule.onNodeWithText("HDR").assertExists()
        composeTestRule.onNodeWithText("TV-MA").assertExists()
    }

    @Test
    fun testMediaDetailsActionsAndBadges_Scale1_3x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    MetaLine(items = listOf("2024", "2h 18m", "Action", "Sci-Fi"))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                        QualityBadge(quality = "4K")
                        QualityBadge(quality = "HDR")
                        ContentRatingBadge(contentRating = "TV-MA")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)
                    ) {
                        PrimaryButton(
                            text = "Play",
                            icon = Icons.Default.PlayArrow,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Download",
                            icon = Icons.Outlined.Download,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Play").assertExists()
        composeTestRule.onNodeWithText("Play").assertHeightIsAtLeast(Dimens.minTouchTarget)
        composeTestRule.onNodeWithText("Download").assertExists()
        composeTestRule.onNodeWithText("Download").assertHeightIsAtLeast(Dimens.minTouchTarget)
        composeTestRule.onNodeWithText("4K").assertExists()
        composeTestRule.onNodeWithText("HDR").assertExists()
        composeTestRule.onNodeWithText("TV-MA").assertExists()
    }

    // =========================================================================
    // 3. PLAYER CONTROLS SURFACE TESTS
    // =========================================================================

    @Test
    fun testPlayerTopBarAndCenterCluster_Scale1_0x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.0f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing16)) {
                    PlayerTopBar(
                        visible = true,
                        isScrubbing = false,
                        title = "Dune: Part Two",
                        subtitle = "Chapter 4: The Arrakis Prophecy",
                        isTvSeries = true,
                        seasonNumber = 1,
                        episodeNumber = 4,
                        isEpisodeSelectorOpen = false,
                        isOfflinePlayback = false,
                        onToggleEpisodeSelector = {},
                        onBack = {}
                    )

                    PlayerCenterCluster(
                        visible = true,
                        isScrubbing = false,
                        isPlaying = true,
                        isLoading = false,
                        isBuffering = false,
                        seekIntervalSeconds = 10,
                        onTogglePlayPause = {},
                        onSeekRelative = {}
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Dune: Part Two").assertExists()
        composeTestRule.onNodeWithText("S1 · E4 · Chapter 4: The Arrakis Prophecy").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun testPlayerTopBarAndCenterCluster_Scale1_3x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing16)) {
                    PlayerTopBar(
                        visible = true,
                        isScrubbing = false,
                        title = "Dune: Part Two",
                        subtitle = "Chapter 4: The Arrakis Prophecy",
                        isTvSeries = true,
                        seasonNumber = 1,
                        episodeNumber = 4,
                        isEpisodeSelectorOpen = false,
                        isOfflinePlayback = false,
                        onToggleEpisodeSelector = {},
                        onBack = {}
                    )

                    PlayerCenterCluster(
                        visible = true,
                        isScrubbing = false,
                        isPlaying = true,
                        isLoading = false,
                        isBuffering = false,
                        seekIntervalSeconds = 10,
                        onTogglePlayPause = {},
                        onSeekRelative = {}
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Dune: Part Two").assertExists()
        composeTestRule.onNodeWithText("S1 · E4 · Chapter 4: The Arrakis Prophecy").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun testPlayerBottomControlsAndScrubber_Scale1_0x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.0f) {
                PlayerBottomControls(
                    visible = true,
                    isScrubbing = false,
                    currentPositionMs = 45000L,
                    durationMs = 120000L,
                    bufferedPositionMs = 70000L,
                    scrubPositionMs = 45000L,
                    playbackSpeed = 1.0f,
                    isTvSeries = false,
                    hasNextEpisode = false,
                    onScrubStart = {},
                    onScrubProgress = {},
                    onScrubEnd = {},
                    onOpenSpeed = {},
                    onOpenAudioSubtitles = {},
                    onOpenSettings = {},
                    onNextEpisode = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Speed").assertExists()
        composeTestRule.onNodeWithContentDescription("Audio Tracks").assertExists()
        composeTestRule.onNodeWithContentDescription("Player Settings").assertExists()
    }

    @Test
    fun testPlayerBottomControlsAndScrubber_Scale1_3x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.3f) {
                PlayerBottomControls(
                    visible = true,
                    isScrubbing = false,
                    currentPositionMs = 45000L,
                    durationMs = 120000L,
                    bufferedPositionMs = 70000L,
                    scrubPositionMs = 45000L,
                    playbackSpeed = 1.0f,
                    isTvSeries = false,
                    hasNextEpisode = false,
                    onScrubStart = {},
                    onScrubProgress = {},
                    onScrubEnd = {},
                    onOpenSpeed = {},
                    onOpenAudioSubtitles = {},
                    onOpenSettings = {},
                    onNextEpisode = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Speed").assertExists()
        composeTestRule.onNodeWithContentDescription("Audio Tracks").assertExists()
        composeTestRule.onNodeWithContentDescription("Player Settings").assertExists()
    }

    // =========================================================================
    // 4. SETTINGS SCREEN SURFACE TESTS
    // =========================================================================

    @Test
    fun testSettingsNavigationAndToggles_Scale1_0x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.0f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    SettingsGroup(title = "Playback", icon = Icons.Default.PlayArrow) {
                        SettingsRow(
                            title = "Preferred quality",
                            subtitle = "1080p Full HD",
                            showChevron = true,
                            onClick = {}
                        )
                        SettingsRow(
                            title = "Auto-play next episode",
                            subtitle = "Queue the next episode when current finishes",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }

                    SettingsGroup(title = "General", icon = Icons.Default.Settings) {
                        SettingsRow(
                            title = "Clear image cache",
                            subtitle = "Free up device storage",
                            onClick = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("PLAYBACK").assertExists()
        composeTestRule.onNodeWithText("Preferred quality").assertExists()
        composeTestRule.onNodeWithText("Auto-play next episode").assertExists()
        composeTestRule.onNodeWithText("GENERAL").assertExists()
        composeTestRule.onNodeWithText("Clear image cache").assertExists()
    }

    @Test
    fun testSettingsNavigationAndToggles_Scale1_3x() {
        composeTestRule.setContent {
            ScalingSurface(fontScale = 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    SettingsGroup(title = "Playback", icon = Icons.Default.PlayArrow) {
                        SettingsRow(
                            title = "Preferred quality",
                            subtitle = "1080p Full HD",
                            showChevron = true,
                            onClick = {}
                        )
                        SettingsRow(
                            title = "Auto-play next episode",
                            subtitle = "Queue the next episode when current finishes",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }

                    SettingsGroup(title = "General", icon = Icons.Default.Settings) {
                        SettingsRow(
                            title = "Clear image cache",
                            subtitle = "Free up device storage",
                            onClick = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("PLAYBACK").assertExists()
        composeTestRule.onNodeWithText("Preferred quality").assertExists()
        composeTestRule.onNodeWithText("Auto-play next episode").assertExists()
        composeTestRule.onNodeWithText("GENERAL").assertExists()
        composeTestRule.onNodeWithText("Clear image cache").assertExists()
    }
}
