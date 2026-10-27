package com.euthopiar.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.euthopiar.R
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.player.subtitles.SubtitlePreset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.ui.components.tvFocusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalReduceMotion

enum class PlayerSidePanelType {
    NONE,
    SPEED,
    AUDIO_SUBTITLES,
    AUDIO,
    SUBTITLES,
    SETTINGS,
    SLEEP_TIMER,
    EPISODES,
    SOURCES,
    VIDEO_QUALITY
}

enum class SidePanelTab(val label: String) {
    EPISODES("Episodes"),
    AUDIO_SUBTITLES("Audio & Subtitles"),
    VIDEO_QUALITY("Video Quality"),
    STREAM_SOURCES("Stream Sources"),
    SPEED("Speed"),
    SLEEP_TIMER("Sleep Timer")
}

/**
 * Landscape Side Panel (Section 4.1).
 *
 * - Right-edge drawer: 360 dp width (or 45% screen width if narrow), full height, respecting insets.
 * - Sheet glass from tokens, hairline on left edge, 28 dp corners on the two left corners.
 * - Video remains visible to the left with a 30% black scrim over it (tap scrim or back closes).
 * - Header: scrollable tab row (13 sp medium) with 2 dp white underline for selected tab.
 * - Content: hairline-divided rows (>= 56 dp height, 16 dp side padding), no boxed rows.
 * - Selected row shows a 20 dp solid white circle with dark check on the right.
 * - Section labels: 11 sp uppercase, wide spacing, white 45%.
 * - Motion: enter 40 dp slide in + fade (320 ms ease-out), scrim in 240 ms; exit 240 ms ease-in.
 * - Tab crossfade: 180 ms. Row stagger: 40 ms for first 6 rows.
 */
@Composable
fun PlayerSidePanel(
    activePanel: PlayerSidePanelType,
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    audioTracks: List<AudioTrackOption>,
    selectedAudioTrack: AudioTrackOption,
    onSelectAudioTrack: (AudioTrackOption) -> Unit,
    dialogueBoostLevel: DialogueBoostLevel = DialogueBoostLevel.OFF,
    onSelectDialogueBoost: (DialogueBoostLevel) -> Unit = {},
    isNightModeDrcEnabled: Boolean = false,
    onToggleNightModeDrc: (Boolean) -> Unit = {},
    selectedSleepTimer: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingSeconds: Int? = null,
    onSelectSleepTimer: (SleepTimerOption) -> Unit = {},
    subtitles: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption,
    onSelectSubtitle: (SubtitleOption) -> Unit,
    subtitleOffsetMs: Long = 0L,
    onAdjustSubtitleOffset: (Long) -> Unit = {},
    onResetSubtitleOffset: () -> Unit = {},
    isSearchingExternalSubtitles: Boolean = false,
    onSearchExternalSubtitles: () -> Unit = {},
    subtitleSizeSp: Int = 18,
    subtitleColor: String = "WHITE",
    subtitleBackgroundEnabled: Boolean = false,
    subtitleOutline: Boolean = true,
    onApplySubtitlePreset: (SubtitlePreset) -> Unit = {},
    isSearchingStreams: Boolean = false,
    servers: List<ServerSourceOption>,
    selectedServer: ServerSourceOption?,
    onSelectServer: (ServerSourceOption) -> Unit,
    qualities: List<VideoQualityOption>,
    selectedQuality: VideoQualityOption? = null,
    onSelectQuality: (VideoQualityOption) -> Unit,
    aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    onSelectAspectRatio: (AspectRatioMode) -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    isPipSupported: Boolean = false,
    onEnterPip: () -> Unit = {},
    onLockScreen: () -> Unit = {},
    seekIntervalSeconds: Int = 10,
    onSelectSeekInterval: (Int) -> Unit = {},
    smartFailoverEnabled: Boolean = false,
    onToggleSmartFailover: (Boolean) -> Unit = {},
    episodes: List<EpisodeItem> = emptyList(),
    availableSeasons: List<Int> = emptyList(),
    selectedSeason: Int = 1,
    onSelectSeason: (Int) -> Unit = {},
    currentEpisodeNumber: Int? = null,
    onSelectEpisode: (EpisodeItem) -> Unit = {},
    isEpisodesLoading: Boolean = false,
    playbackMap: Map<Pair<Int, Int>, PlaybackHistoryEntity> = emptyMap(),
    isTvSeries: Boolean = false,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (activePanel == PlayerSidePanelType.NONE) return

    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val drawerWidth = PlayerMotionMath.calculateDrawerWidth(screenWidthDp).dp

    val activeTitle = when (activePanel) {
        PlayerSidePanelType.AUDIO -> "Audio Tracks"
        PlayerSidePanelType.SUBTITLES -> "Subtitles"
        PlayerSidePanelType.AUDIO_SUBTITLES -> "Audio & Subtitles"
        PlayerSidePanelType.SOURCES -> "Sources"
        PlayerSidePanelType.SPEED -> "Playback Speed"
        PlayerSidePanelType.SETTINGS -> "Player Settings"
        PlayerSidePanelType.SLEEP_TIMER -> "Sleep Timer"
        PlayerSidePanelType.EPISODES -> "Episodes"
        PlayerSidePanelType.VIDEO_QUALITY -> "Video Quality"
        else -> "Options"
    }

    val activeIconRes = when (activePanel) {
        PlayerSidePanelType.AUDIO -> R.drawable.ic_player_audio
        PlayerSidePanelType.SUBTITLES -> R.drawable.ic_player_subtitles
        PlayerSidePanelType.AUDIO_SUBTITLES -> R.drawable.ic_player_audio
        PlayerSidePanelType.SOURCES -> R.drawable.ic_player_sources
        PlayerSidePanelType.SPEED -> R.drawable.ic_player_speed
        PlayerSidePanelType.SETTINGS -> R.drawable.ic_player_tuning
        PlayerSidePanelType.SLEEP_TIMER -> R.drawable.ic_player_tuning
        PlayerSidePanelType.EPISODES -> R.drawable.ic_player_episodes
        PlayerSidePanelType.VIDEO_QUALITY -> R.drawable.ic_quality_fhd
        else -> R.drawable.ic_player_tuning
    }

    BackHandler { onDismiss() }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim over video (30% black). Tapping scrim dismisses panel.
        AnimatedVisibility(
            visible = activePanel != PlayerSidePanelType.NONE,
            enter = fadeIn(tween(PlayerTokens.SidePanelScrimFadeMs)),
            exit = fadeOut(tween(PlayerTokens.SidePanelScrimFadeMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerTokens.SidePanelScrim)
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            )
        }

        // Right-Edge Drawer
        AnimatedVisibility(
            visible = activePanel != PlayerSidePanelType.NONE,
            enter = if (reduceMotion) {
                fadeIn(tween(PlayerTokens.ReduceMotionFadeInMs))
            } else {
                val slidePx = with(density) { 40.dp.roundToPx() }
                slideInHorizontally(
                    initialOffsetX = { slidePx },
                    animationSpec = spring(dampingRatio = 0.98f, stiffness = 700f)
                ) + fadeIn(spring(dampingRatio = 0.98f, stiffness = 700f))
            },
            exit = if (reduceMotion) {
                fadeOut(tween(PlayerTokens.ReduceMotionFadeOutMs))
            } else {
                val slidePx = with(density) { 40.dp.roundToPx() }
                slideOutHorizontally(
                    targetOffsetX = { slidePx },
                    animationSpec = spring(dampingRatio = 0.98f, stiffness = 700f)
                ) + fadeOut(spring(dampingRatio = 0.98f, stiffness = 700f))
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .sheetGlass(shape = PlayerTokens.RadiusSidePanel)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: Clean dedicated panel title with styled Icon and Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 18.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = activeIconRes),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = activeTitle,
                                style = PlayerTokens.SheetHeadline.copy(fontSize = 20.sp),
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                                .tvFocusable(shape = PlayerTokens.RadiusCircle)
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = "Close",
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Hairline under header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HairlineWidth)
                            .background(Color.White.copy(alpha = 0.14f))
                    )

                    // Dedicated panel content (NO global multi-tab strip!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activePanel) {
                            PlayerSidePanelType.AUDIO -> {
                                AudioOnlyTabContent(
                                    audioTracks = audioTracks,
                                    selectedAudioTrack = selectedAudioTrack,
                                    onSelectAudioTrack = onSelectAudioTrack,
                                    dialogueBoostLevel = dialogueBoostLevel,
                                    onSelectDialogueBoost = onSelectDialogueBoost,
                                    isNightModeDrcEnabled = isNightModeDrcEnabled,
                                    onToggleNightModeDrc = onToggleNightModeDrc
                                )
                            }
                            PlayerSidePanelType.SUBTITLES -> {
                                SubtitlesOnlyTabContent(
                                    subtitles = subtitles,
                                    selectedSubtitle = selectedSubtitle,
                                    onSelectSubtitle = onSelectSubtitle,
                                    subtitleOffsetMs = subtitleOffsetMs,
                                    onAdjustSubtitleOffset = onAdjustSubtitleOffset,
                                    onResetSubtitleOffset = onResetSubtitleOffset,
                                    isSearchingExternalSubtitles = isSearchingExternalSubtitles,
                                    onSearchExternalSubtitles = onSearchExternalSubtitles
                                )
                            }
                            PlayerSidePanelType.AUDIO_SUBTITLES -> {
                                AudioSubtitlesTabContent(
                                    audioTracks = audioTracks,
                                    selectedAudioTrack = selectedAudioTrack,
                                    onSelectAudioTrack = onSelectAudioTrack,
                                    subtitles = subtitles,
                                    selectedSubtitle = selectedSubtitle,
                                    onSelectSubtitle = onSelectSubtitle,
                                    subtitleOffsetMs = subtitleOffsetMs,
                                    onAdjustSubtitleOffset = onAdjustSubtitleOffset,
                                    onResetSubtitleOffset = onResetSubtitleOffset
                                )
                            }
                            PlayerSidePanelType.SOURCES -> {
                                SourcesListContent(
                                    servers = servers,
                                    selectedServer = selectedServer,
                                    isSearching = isSearchingStreams,
                                    onSelectServer = { server ->
                                        onSelectServer(server)
                                        onDismiss()
                                    }
                                )
                            }
                            PlayerSidePanelType.SPEED -> {
                                SpeedTabContent(
                                    playbackSpeed = playbackSpeed,
                                    onSelectSpeed = onSelectSpeed
                                )
                            }
                            PlayerSidePanelType.SETTINGS -> {
                                PlayerSettingsPanelContent(
                                    aspectRatioMode = aspectRatioMode,
                                    onSelectAspectRatio = onSelectAspectRatio,
                                    onOpenSleepTimer = onOpenSleepTimer,
                                    isPipSupported = isPipSupported,
                                    onEnterPip = onEnterPip,
                                    onLockScreen = onLockScreen,
                                    seekIntervalSeconds = seekIntervalSeconds,
                                    onSelectSeekInterval = onSelectSeekInterval,
                                    smartFailoverEnabled = smartFailoverEnabled,
                                    onToggleSmartFailover = onToggleSmartFailover,
                                    dialogueBoostLevel = dialogueBoostLevel,
                                    onSelectDialogueBoost = onSelectDialogueBoost,
                                    isNightModeDrcEnabled = isNightModeDrcEnabled,
                                    onToggleNightModeDrc = onToggleNightModeDrc
                                )
                            }
                            PlayerSidePanelType.SLEEP_TIMER -> {
                                SleepTimerTabContent(
                                    selectedSleepTimer = selectedSleepTimer,
                                    sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                                    onSelectSleepTimer = onSelectSleepTimer
                                )
                            }
                            PlayerSidePanelType.VIDEO_QUALITY -> {
                                QualityTabContent(
                                    qualities = qualities,
                                    selectedQuality = selectedQuality ?: VideoQualityOption("auto", "Auto (Adaptive)", isAuto = true),
                                    onSelectQuality = onSelectQuality
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Audio Only Tab Content with generous breathing room.
 * Minimum row height 62dp, 20dp padding, Dialogue Boost & Night Mode DRC sections.
 */
@Composable
private fun AudioOnlyTabContent(
    audioTracks: List<AudioTrackOption>,
    selectedAudioTrack: AudioTrackOption,
    onSelectAudioTrack: (AudioTrackOption) -> Unit,
    dialogueBoostLevel: DialogueBoostLevel,
    onSelectDialogueBoost: (DialogueBoostLevel) -> Unit,
    isNightModeDrcEnabled: Boolean,
    onToggleNightModeDrc: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Section: AUDIO TRACKS
        item {
            Text(
                text = "AVAILABLE AUDIO TRACKS",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
            )
        }
        val displayTracks = audioTracks.filter { it.displayName.isNotBlank() && !it.displayName.contains("Track -1", ignoreCase = true) }
        if (displayTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No additional audio tracks found",
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
        } else {
            itemsIndexed(displayTracks) { idx, track ->
                val isSelected = track.id == selectedAudioTrack.id
                DrawerSelectionRow(
                    title = track.displayName,
                    meta = if (track.isAuto) "Default Track" else null,
                    isSelected = isSelected,
                    onClick = { onSelectAudioTrack(track) }
                )
            }
        }

        // Section: ENHANCEMENTS
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AUDIO ENHANCEMENTS",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 10.dp)
            )
        }

        // Dialogue Boost Selection Row
        // Dialogue Boost Selection Row (Section 3.5)
        item {
            ResponsiveDialogueBoostControl(
                selectedLevel = dialogueBoostLevel,
                onSelectLevel = onSelectDialogueBoost
            )
        }

        // Night Mode DRC Row
        item {
            DrawerActionToggleRow(
                title = "Night Mode DRC",
                subtitle = "Compresses dynamic range so loud sounds are tempered",
                checked = isNightModeDrcEnabled,
                onCheckedChange = onToggleNightModeDrc
            )
        }
    }
}

/**
 * Dedicated Subtitles Only Tab Content with generous breathing room.
 * Minimum row height 62dp, 20dp padding, Online Search button, and fine-tuning sync.
 */
@Composable
private fun SubtitlesOnlyTabContent(
    subtitles: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption,
    onSelectSubtitle: (SubtitleOption) -> Unit,
    subtitleOffsetMs: Long,
    onAdjustSubtitleOffset: (Long) -> Unit,
    onResetSubtitleOffset: () -> Unit,
    isSearchingExternalSubtitles: Boolean,
    onSearchExternalSubtitles: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Section: SUBTITLES
        item {
            Text(
                text = "SUBTITLE TRACKS",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
            )
        }

        // First row of subtitles: "Off"
        val isOffSelected = selectedSubtitle.id == "off" || selectedSubtitle.label.equals("Off", ignoreCase = true)
        item {
            DrawerSelectionRow(
                title = "Off",
                meta = "Disable subtitles",
                isSelected = isOffSelected,
                onClick = { onSelectSubtitle(SubtitleOption("off", "Off")) }
            )
        }

        val displaySubs = subtitles.filter { it.id != "off" && !it.label.equals("Off", ignoreCase = true) }
        itemsIndexed(displaySubs) { idx, sub ->
            val isSelected = sub.id == selectedSubtitle.id
            DrawerSelectionRow(
                title = sub.label,
                meta = sub.language ?: "Subtitles",
                isSelected = isSelected,
                onClick = { onSelectSubtitle(sub) }
            )
        }

        // Online Subtitle Search Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                        .tvFocusable(shape = PlayerTokens.RadiusPill)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = "Search Online Subtitles",
                            onClick = onSearchExternalSubtitles
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearchingExternalSubtitles) {
                        BufferingSpinnerArc(modifier = Modifier.size(20.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_player_subtitles),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Search Online Subtitles",
                                style = PlayerTokens.PillText,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Subtitle Sync / Delay Adjustment
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUBTITLE SYNCHRONIZATION",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Offset",
                        style = PlayerTokens.RowTitle.copy(fontSize = 15.sp)
                    )
                    Text(
                        text = if (subtitleOffsetMs == 0L) "0 ms" else "${subtitleOffsetMs} ms",
                        style = PlayerTokens.Timecode,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "-250ms", onClick = { onAdjustSubtitleOffset(-250L) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-250ms", style = PlayerTokens.PillText)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "-50ms", onClick = { onAdjustSubtitleOffset(-50L) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-50ms", style = PlayerTokens.PillText)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "Reset 0ms", onClick = onResetSubtitleOffset),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Reset", style = PlayerTokens.PillText)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "+50ms", onClick = { onAdjustSubtitleOffset(50L) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+50ms", style = PlayerTokens.PillText)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "+250ms", onClick = { onAdjustSubtitleOffset(250L) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+250ms", style = PlayerTokens.PillText)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(HairlineWidth)
                    .background(Color.White.copy(alpha = 0.14f))
            )
        }
    }
}

/**
 * Audio & Subtitles Tab Content.
 * Section labels: AUDIO and SUBTITLES (11 sp uppercase wide spacing, white 45%).
 * "Off" is the first row of subtitles.
 * Hairline-divided rows, 20 dp solid white check circle on active.
 */
@Composable
private fun AudioSubtitlesTabContent(
    audioTracks: List<AudioTrackOption>,
    selectedAudioTrack: AudioTrackOption,
    onSelectAudioTrack: (AudioTrackOption) -> Unit,
    subtitles: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption,
    onSelectSubtitle: (SubtitleOption) -> Unit,
    subtitleOffsetMs: Long,
    onAdjustSubtitleOffset: (Long) -> Unit,
    onResetSubtitleOffset: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Section: AUDIO
        item {
            Text(
                text = "AUDIO",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        val displayTracks = audioTracks.filter { it.displayName.isNotBlank() && !it.displayName.contains("Track -1", ignoreCase = true) }
        itemsIndexed(displayTracks) { idx, track ->
            val isSelected = track.id == selectedAudioTrack.id
            DrawerSelectionRow(
                title = track.displayName,
                meta = if (track.isAuto) "Default" else null,
                isSelected = isSelected,
                onClick = { onSelectAudioTrack(track) }
            )
        }

        // Section: SUBTITLES
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUBTITLES",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        // First row of subtitles: "Off"
        val isOffSelected = selectedSubtitle.id == "off" || selectedSubtitle.label.equals("Off", ignoreCase = true)
        item {
            DrawerSelectionRow(
                title = "Off",
                meta = null,
                isSelected = isOffSelected,
                onClick = { onSelectSubtitle(SubtitleOption("off", "Off")) }
            )
        }

        val displaySubs = subtitles.filter { it.id != "off" && !it.label.equals("Off", ignoreCase = true) }
        itemsIndexed(displaySubs) { idx, sub ->
            val isSelected = sub.id == selectedSubtitle.id
            DrawerSelectionRow(
                title = sub.label,
                meta = sub.language,
                isSelected = isSelected,
                onClick = { onSelectSubtitle(sub) }
            )
        }

        // Subtitle Sync / Delay Adjustment
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUBTITLE TIMING",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delay: ${subtitleOffsetMs}ms",
                    style = PlayerTokens.RowTitle
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "-50ms", onClick = { onAdjustSubtitleOffset(-50L) })
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "-50ms", style = PlayerTokens.PillText)
                    }
                    Box(
                        modifier = Modifier
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(role = Role.Button, onClickLabel = "+50ms", onClick = { onAdjustSubtitleOffset(50L) })
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "+50ms", style = PlayerTokens.PillText)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(HairlineWidth)
                    .background(Color.White.copy(alpha = 0.14f))
            )
        }
    }
}

/**
 * Video Quality Tab Content.
 */
@Composable
private fun QualityTabContent(
    qualities: List<VideoQualityOption>,
    selectedQuality: VideoQualityOption,
    onSelectQuality: (VideoQualityOption) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "RESOLUTION",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        itemsIndexed(qualities) { idx, q ->
            val isSelected = q.id == selectedQuality.id
            DrawerSelectionRow(
                title = q.label,
                meta = if (q.id == "auto" || q.isAuto) "Adaptive" else null,
                isSelected = isSelected,
                onClick = { onSelectQuality(q) }
            )
        }
    }
}

/**
 * Playback Speed Tab Content.
 * 0.5x, 0.75x, 1.0x (Normal), 1.25x, 1.5x, 2.0x.
 */
@Composable
private fun SpeedTabContent(
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit
) {
    val speeds = listOf(
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "1.0x (Normal)",
        1.25f to "1.25x",
        1.5f to "1.5x",
        2.0f to "2.0x"
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "PLAYBACK SPEED",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        itemsIndexed(speeds) { idx, (spd, label) ->
            val isSelected = playbackSpeed == spd
            DrawerSelectionRow(
                title = label,
                meta = null,
                isSelected = isSelected,
                onClick = { onSelectSpeed(spd) }
            )
        }
    }
}

/**
 * Sleep Timer Tab Content.
 * Off, 15, 30, 45, 60 minutes, End of episode.
 */
@Composable
private fun SleepTimerTabContent(
    selectedSleepTimer: SleepTimerOption,
    sleepTimerRemainingSeconds: Int?,
    onSelectSleepTimer: (SleepTimerOption) -> Unit
) {
    val options: List<Pair<SleepTimerOption, String>> = listOf(
        SleepTimerOption.OFF to "Off",
        SleepTimerOption.MIN_15 to "15 minutes",
        SleepTimerOption.MIN_30 to "30 minutes",
        SleepTimerOption.MIN_45 to "45 minutes",
        SleepTimerOption.MIN_60 to "60 minutes",
        SleepTimerOption.END_OF_EPISODE to "End of episode"
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "SLEEP TIMER",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        itemsIndexed(options) { idx, (opt, label) ->
            val isSelected = selectedSleepTimer == opt
            val meta = if (isSelected && sleepTimerRemainingSeconds != null && sleepTimerRemainingSeconds > 0) {
                "${sleepTimerRemainingSeconds / 60}m remaining"
            } else null

            DrawerSelectionRow(
                title = label,
                meta = meta,
                isSelected = isSelected,
                onClick = { onSelectSleepTimer(opt) }
            )
        }
    }
}

/**
 * TV Series Episodes Tab Content.
 */
@Composable
private fun EpisodesTabContent(
    episodes: List<EpisodeItem>,
    availableSeasons: List<Int>,
    selectedSeason: Int,
    currentEpisodeNumber: Int?,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (EpisodeItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (availableSeasons.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSeasons) { season ->
                    val isSelected = season == selectedSeason
                    Box(
                        modifier = Modifier
                            .clip(PlayerTokens.RadiusPill)
                            .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                            .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                            .playerPressable(
                                isWhiteElement = isSelected,
                                onClick = { onSelectSeason(season) }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Season $season",
                            style = PlayerTokens.PillText,
                            color = if (isSelected) PlayerTokens.LabelDark else Color.White
                        )
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(episodes, key = { ep -> "${ep.seasonNumber}_${ep.episodeNumber}" }) { ep ->
                val isSelected = ep.episodeNumber == currentEpisodeNumber
                DrawerSelectionRow(
                    title = "Episode ${ep.episodeNumber}",
                    meta = ep.title.ifBlank { null },
                    isSelected = isSelected,
                    onClick = { onSelectEpisode(ep) }
                )
            }
        }
    }
}

/**
 * Player Settings Panel Content with generous breathing room.
 * Double tap seek interval, smart failover, aspect ratio, screen lock, sleep timer, dialogue boost, night mode DRC.
 */
@Composable
private fun PlayerSettingsPanelContent(
    aspectRatioMode: AspectRatioMode,
    onSelectAspectRatio: (AspectRatioMode) -> Unit,
    onOpenSleepTimer: () -> Unit,
    isPipSupported: Boolean,
    onEnterPip: () -> Unit,
    onLockScreen: () -> Unit,
    seekIntervalSeconds: Int,
    onSelectSeekInterval: (Int) -> Unit,
    smartFailoverEnabled: Boolean,
    onToggleSmartFailover: (Boolean) -> Unit,
    dialogueBoostLevel: DialogueBoostLevel,
    onSelectDialogueBoost: (DialogueBoostLevel) -> Unit,
    isNightModeDrcEnabled: Boolean,
    onToggleNightModeDrc: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Section: PLAYBACK PREFERENCES
        item {
            Text(
                text = "PLAYBACK PREFERENCES",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
            )
        }

        // Seek Interval
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Double Tap Seek Interval",
                    style = PlayerTokens.RowTitle.copy(fontSize = 15.sp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Number of seconds to skip on double tap",
                    style = PlayerTokens.RowMeta.copy(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PlayerMotionMath.BUTTON_GAP_DP.dp)
                ) {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        val isSelected = seekIntervalSeconds == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(PlayerTokens.RadiusPill)
                                .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                                .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                                .tvFocusable(shape = PlayerTokens.RadiusPill)
                                .playerPressable(
                                    isWhiteElement = isSelected,
                                    onClick = { onSelectSeekInterval(sec) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s",
                                style = PlayerTokens.PillText,
                                color = if (isSelected) PlayerTokens.LabelDark else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Smart Failover Toggle
        item {
            DrawerActionToggleRow(
                title = "Smart Source Failover",
                subtitle = "Automatically switches to backup server if current stream stalls",
                checked = smartFailoverEnabled,
                onCheckedChange = onToggleSmartFailover
            )
        }

        // Section: CONTROLS & SECURITY
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "CONTROLS & DISPLAY",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 10.dp)
            )
        }

        // Screen Lock Row
        item {
            DrawerSelectionRow(
                title = "Lock Screen Gestures",
                meta = "Disables on-screen taps and gestures",
                isSelected = false,
                onClick = onLockScreen
            )
        }

        // Sleep Timer Row
        item {
            DrawerSelectionRow(
                title = "Sleep Timer",
                meta = "Schedule playback shutdown",
                isSelected = false,
                onClick = onOpenSleepTimer
            )
        }

        // PiP Row
        if (isPipSupported) {
            item {
                DrawerSelectionRow(
                    title = "Picture-in-Picture",
                    meta = "Continue watching in a floating window",
                    isSelected = false,
                    onClick = onEnterPip
                )
            }
        }

        // Section: AUDIO ENHANCEMENTS
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AUDIO ENHANCEMENTS",
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 10.dp)
            )
        }

        // Dialogue Boost (Section 3.5)
        item {
            ResponsiveDialogueBoostControl(
                selectedLevel = dialogueBoostLevel,
                onSelectLevel = onSelectDialogueBoost
            )
        }

        // Night Mode DRC
        item {
            DrawerActionToggleRow(
                title = "Night Mode DRC",
                subtitle = "Normalizes sudden loud sound effects",
                checked = isNightModeDrcEnabled,
                onCheckedChange = onToggleNightModeDrc
            )
        }
    }
}

/**
 * Toggle Row for Side Panel settings.
 */
@Composable
private fun DrawerActionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .playerPressable(
                    role = Role.Switch,
                    onClickLabel = title,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PlayerTokens.RowTitle.copy(fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = PlayerTokens.RowMeta.copy(fontSize = 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PlayerTokens.LabelDark,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                    uncheckedTrackColor = PlayerTokens.VideoOverlayGlassBase,
                    uncheckedBorderColor = Color.White.copy(alpha = 0.20f)
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(HairlineWidth)
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}

/**
 * Hairline-divided row with 20 dp solid white circle with dark check for active items.
 * Generous breathing space: 62 dp minimum height, 20 dp padding.
 */
@Composable
private fun DrawerSelectionRow(
    title: String,
    meta: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .playerPressable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PlayerTokens.RowTitle.copy(fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!meta.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = meta,
                        style = PlayerTokens.RowMeta.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Reserved 20 dp slot
            Box(
                modifier = Modifier.size(PlayerTokens.SizeCheckCircle),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(PlayerTokens.SizeCheckCircle)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = PlayerTokens.LabelDark,
                            modifier = Modifier.size(PlayerTokens.IconCheck)
                        )
                    }
                }
            }
        }

        // 0.5 dp hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(HairlineWidth)
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}

/**
 * Responsive 3-Tier Segmented Control for Dialogue Clarity Boost (Section 3.5).
 * Tier 1 (W_eff >= 72dp): Full Two-Line Stack ("Off", "Low", "Medium", "High" / "0 dB", "+3 dB", "+6 dB", "+8 dB")
 * Tier 2 (52dp <= W_eff < 72dp): Compact Two-Tier Micro ("Off", "Low", "Med", "High" / "—", "+3dB", "+6dB", "+8dB")
 * Tier 3 (W_eff < 52dp): Numeric Badge ("Off", "1", "2", "3") with dynamic active value echoed in header.
 */
@Composable
fun ResponsiveDialogueBoostControl(
    selectedLevel: DialogueBoostLevel,
    onSelectLevel: (DialogueBoostLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val drawerWidthDp = PlayerMotionMath.calculateDrawerWidth(screenWidthDp)
    val fontScale = density.fontScale.coerceIn(0.85f, 2.0f)
    val buttonWidthDp = PlayerMotionMath.calculateButtonWidth(drawerWidthDp, numButtons = 4)
    val tier = PlayerMotionMath.determineSegmentedControlTier(buttonWidthDp, fontScale)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dialogue Clarity Boost",
                style = PlayerTokens.RowTitle.copy(fontSize = 15.sp)
            )
            if (tier == PlayerMotionMath.SegmentedControlTier.TIER_3_NUMERIC_BADGE) {
                Text(
                    text = selectedLevel.label,
                    style = PlayerTokens.RowMeta.copy(
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Enhances vocal frequencies for clearer speech",
            style = PlayerTokens.RowMeta.copy(fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PlayerMotionMath.BUTTON_GAP_DP.dp)
        ) {
            val levels = DialogueBoostLevel.values()
            levels.forEach { level ->
                val isSelected = selectedLevel == level
                val (line1, line2) = when (tier) {
                    PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK -> when (level) {
                        DialogueBoostLevel.OFF -> "Off" to "0 dB"
                        DialogueBoostLevel.LOW -> "Low" to "+3 dB"
                        DialogueBoostLevel.MEDIUM -> "Medium" to "+6 dB"
                        DialogueBoostLevel.HIGH -> "High" to "+8 dB"
                    }
                    PlayerMotionMath.SegmentedControlTier.TIER_2_COMPACT_MICRO -> when (level) {
                        DialogueBoostLevel.OFF -> "Off" to "—"
                        DialogueBoostLevel.LOW -> "Low" to "+3dB"
                        DialogueBoostLevel.MEDIUM -> "Med" to "+6dB"
                        DialogueBoostLevel.HIGH -> "High" to "+8dB"
                    }
                    PlayerMotionMath.SegmentedControlTier.TIER_3_NUMERIC_BADGE -> when (level) {
                        DialogueBoostLevel.OFF -> "Off" to null
                        DialogueBoostLevel.LOW -> "1" to null
                        DialogueBoostLevel.MEDIUM -> "2" to null
                        DialogueBoostLevel.HIGH -> "3" to null
                    }
                }

                val buttonHeight = if (tier == PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK) 44.dp else 40.dp

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .clip(PlayerTokens.RadiusPill)
                        .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                        .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                        .tvFocusable(shape = PlayerTokens.RadiusPill)
                        .playerPressable(
                            isWhiteElement = isSelected,
                            onClick = { onSelectLevel(level) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (line2 != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = line1,
                                style = TextStyle(
                                    fontFamily = com.euthopiar.ui.theme.AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (tier == PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK) 12.sp else 10.5.sp,
                                    lineHeight = if (tier == PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK) 14.sp else 12.sp,
                                    color = if (isSelected) PlayerTokens.LabelDark else Color.White
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = line2,
                                style = TextStyle(
                                    fontFamily = com.euthopiar.ui.theme.AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = if (tier == PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK) 10.sp else 9.5.sp,
                                    lineHeight = if (tier == PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK) 12.sp else 11.sp,
                                    color = if (isSelected) PlayerTokens.LabelDark.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = line1,
                            style = TextStyle(
                                fontFamily = com.euthopiar.ui.theme.AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) PlayerTokens.LabelDark else Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
