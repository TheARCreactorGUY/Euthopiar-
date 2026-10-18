package com.euthopiar.ui.settings.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState

/**
 * Subtitle color options helper.
 */
private fun resolveSubtitleColor(colorName: String): Color {
    return when (colorName.uppercase()) {
        "YELLOW" -> Color(0xFFFFEB3B)
        "CYAN" -> Color(0xFF00E5FF)
        "GREEN" -> Color(0xFF76FF03)
        else -> Color.White
    }
}

/**
 * Redesigned "Playback" Settings Screen (formerly "Playback & Subtitles").
 *
 * Strict High-Fidelity Design Specifications:
 * - Editorial Top Bar with 40x40dp glass back button & crossfading collapsed title.
 * - Bold 32sp Medium headline ("Playback").
 * - Upgraded "LIVE CINEMA & CAPTION STAGE" preview card:
 *   - Architectural glass container with resolution badge and GPU HW ACCEL indicator (monochrome glass chips).
 *   - Live caption viewport centered with sample dialogue reacting live to size, color, background box, and shadow outline.
 *   - Bottom telemetry with 2dp animated buffer meter and live seek readout chip.
 * - Section 1: STREAMING & QUALITY with Frosted Glass Dropdowns, Monochrome Segmented Buffer Row, and HW Acceleration toggle.
 * - Section 2: CONTROLS & AUTOMATIONS with Seek Interval popover, Auto-play toggle, Custom Glass Steppers, and smooth auto-skip sub-rows.
 * - Section 3: SUBTITLES & CAPTIONS with font size stepper, color dropdown with swatches, and high-contrast toggles.
 * - Bottom "Reset playback defaults" glass pill wired to shared Glass Confirmation Dialog.
 * - 100% monochrome design language, 98% press scale on all interactive elements.
 */
@Composable
fun PlaybackSettingsPage(
    onBack: () -> Unit,
    viewModel: PlaybackSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current

    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val autoSkipIntro by viewModel.autoSkipIntro.collectAsState()
    val skipIntroDuration by viewModel.skipIntroDuration.collectAsState()
    val skipOutroDuration by viewModel.skipOutroDuration.collectAsState()
    val countdownDuration by viewModel.countdownDuration.collectAsState()
    val seekInterval by viewModel.seekInterval.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val subtitleBgEnabled by viewModel.subtitleBgEnabled.collectAsState()
    val subtitleColor by viewModel.subtitleColor.collectAsState()
    val subtitleOutline by viewModel.subtitleOutline.collectAsState()
    val externalSubtitlesEnabled by viewModel.externalSubtitlesEnabled.collectAsState()
    val bufferSizeMb by viewModel.bufferSizeMb.collectAsState()
    val hardwareAccel by viewModel.hardwareAcceleration.collectAsState()
    val smartFailover by viewModel.smartFailover.collectAsState()
    val audioPassthrough by viewModel.audioPassthrough.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    val qualityOptions = remember {
        listOf(
            PopoverOption("Auto", "Auto", "Adaptive resolution based on connection"),
            PopoverOption("1080p", "1080p FHD", "Crisp high-definition · Optimal for Wi-Fi"),
            PopoverOption("720p", "720p HD", "Balanced resolution · Data saver"),
            PopoverOption("480p", "480p SD", "Low bandwidth · Mobile friendly")
        )
    }

    val seekOptions = remember {
        listOf(
            PopoverOption(5, "5 Seconds", "Precise scrub for short jumps"),
            PopoverOption(10, "10 Seconds", "Default double-tap jump"),
            PopoverOption(15, "15 Seconds", "Quick scene skip"),
            PopoverOption(30, "30 Seconds", "Extended jump")
        )
    }

    val colorOptions = remember {
        listOf(
            PopoverOption("WHITE", "White", "Classic clean subtitle", Color.White),
            PopoverOption("YELLOW", "Yellow", "High-contrast cinema standard", Color(0xFFFFEB3B)),
            PopoverOption("CYAN", "Cyan", "Modern high-visibility blue", Color(0xFF00E5FF)),
            PopoverOption("GREEN", "Green", "Retro high-contrast tint", Color(0xFF76FF03))
        )
    }

    val bufferOptions = remember { listOf(25, 50, 75, 100) }

    SettingsScaffold(
        title = "Playback",
        onBack = onBack,
        modifier = modifier
    ) { hazeState ->

        // Upgraded Top Preview Card — "LIVE CINEMA & CAPTION STAGE"
        LiveCinemaCaptionStageCard(
            quality = defaultQuality,
            hardwareAcceleration = hardwareAccel,
            subtitleSize = subtitleSize,
            subtitleColorName = subtitleColor,
            subtitleBgEnabled = subtitleBgEnabled,
            subtitleOutline = subtitleOutline,
            bufferSizeMb = bufferSizeMb,
            seekInterval = seekInterval,
            hazeState = hazeState
        )

        // Section 1: Streaming & Quality
        OpenSettingsSectionHeader(title = "Streaming & Quality")

        // 1. Default Stream Quality Row
        OpenSettingsRow(
            title = "Default stream quality",
            subtitle = "Preferred video resolution when playback begins",
            trailingContent = {
                FrostedGlassDropdown(
                    options = qualityOptions,
                    selected = defaultQuality,
                    onSelected = { viewModel.setDefaultQuality(it) },
                    hazeState = hazeState,
                    popoverWidth = 240.dp
                )
            }
        )

        // 2. Video Buffer Size Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = SettingsTokens.HairlineDivider,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = SettingsTokens.HairlineWidth.toPx()
                    )
                }
                .padding(vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Video buffer size",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = SettingsTokens.TextWhite96
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pre-loads forward video stream into memory",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }

                SettingsMetadataChip(
                    text = "${bufferSizeMb} MB",
                    hazeState = hazeState
                )
            }

            MonochromeSegmentedPillRow(
                options = bufferOptions,
                selected = bufferSizeMb,
                onSelected = { viewModel.setBufferSizeMb(it) },
                hazeState = hazeState,
                labelProvider = { "${it}M" },
                isAllowed = { it <= viewModel.maxSupportedBufferSizeMb }
            )
        }

        // 3. Hardware Video Acceleration Row
        OpenSettingsToggleRow(
            title = "Hardware video acceleration",
            titleBadge = {
                SettingsMetadataChip(
                    text = "Restart required",
                    hazeState = hazeState
                )
            },
            subtitle = "Enables GPU surface decoding for 4K & HEVC",
            checked = hardwareAccel,
            onCheckedChange = { viewModel.setHardwareAcceleration(it) },
            hazeState = hazeState
        )

        // 4. Smart Stream Failover Row
        OpenSettingsToggleRow(
            title = "Smart stream failover",
            subtitle = "Automatically switches to backup streams on playback stalls, decoder failures, or dead links",
            checked = smartFailover,
            onCheckedChange = { viewModel.setSmartFailover(it) },
            hazeState = hazeState
        )

        // 5. Audio Passthrough Row
        OpenSettingsToggleRow(
            title = "Audio passthrough",
            subtitle = "Bitstream Dolby & DTS surround audio directly to external AV receiver or soundbar without software conversion",
            checked = audioPassthrough,
            onCheckedChange = { viewModel.setAudioPassthrough(it) },
            hazeState = hazeState
        )

        // Section 2: Controls & Automations
        OpenSettingsSectionHeader(title = "Controls & Automations")

        // 1. Seek Interval Row
        OpenSettingsRow(
            title = "Seek interval",
            subtitle = "Double tap to forward / rewind interval",
            trailingContent = {
                val currentSeekOption = seekOptions.firstOrNull { it.value == seekInterval }
                FrostedGlassDropdown(
                    options = seekOptions,
                    selected = seekInterval,
                    onSelected = { viewModel.setSeekInterval(it) },
                    hazeState = hazeState,
                    popoverWidth = 230.dp
                )
            }
        )

        // 2. Auto-Play Next Episode Row
        OpenSettingsToggleRow(
            title = "Auto-play next episode",
            subtitle = "Plays the subsequent chapter automatically",
            checked = autoPlayNext,
            onCheckedChange = { viewModel.setAutoPlayNext(it) },
            hazeState = hazeState
        )

        // 3. Next Episode Countdown Row
        OpenSettingsRow(
            title = "Next episode countdown",
            subtitle = "Seconds before next episode automatically starts",
            enabled = autoPlayNext,
            modifier = Modifier.alpha(if (autoPlayNext) 1f else 0.45f),
            trailingContent = {
                CustomGlassStepper(
                    value = countdownDuration,
                    unit = "s",
                    onValueChange = { viewModel.setCountdownDuration(it) },
                    min = 3,
                    max = 30,
                    step = 1,
                    enabled = autoPlayNext,
                    hazeState = hazeState
                )
            }
        )

        // 4. Auto-Skip Intro Row
        OpenSettingsToggleRow(
            title = "Auto-skip intro",
            subtitle = "Jumps past opening sequence automatically",
            checked = autoSkipIntro,
            onCheckedChange = { viewModel.setAutoSkipIntro(it) },
            hazeState = hazeState
        )

        // Sub-settings for Auto-Skip Intro (smooth 220ms expand/collapse)
        AnimatedVisibility(
            visible = autoSkipIntro,
            enter = if (reduceMotion) fadeIn(tween(120)) else expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = if (reduceMotion) fadeOut(tween(120)) else shrinkVertically(tween(220)) + fadeOut(tween(180))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OpenSettingsRow(
                    title = "Intro duration",
                    subtitle = "Seconds skipped on opening theme",
                    trailingContent = {
                        CustomGlassStepper(
                            value = skipIntroDuration,
                            unit = "s",
                            onValueChange = { viewModel.setSkipIntroDuration(it) },
                            min = 10,
                            max = 180,
                            step = 5,
                            hazeState = hazeState
                        )
                    }
                )

                OpenSettingsRow(
                    title = "Outro duration",
                    subtitle = "Seconds skipped at end credits",
                    trailingContent = {
                        CustomGlassStepper(
                            value = skipOutroDuration,
                            unit = "s",
                            onValueChange = { viewModel.setSkipOutroDuration(it) },
                            min = 10,
                            max = 180,
                            step = 5,
                            hazeState = hazeState
                        )
                    }
                )
            }
        }

        // Section 3: Subtitles & Captions
        OpenSettingsSectionHeader(title = "Subtitles & Captions")

        // 1. Caption Font Size Row
        OpenSettingsRow(
            title = "Caption font size",
            subtitle = "Display scale for dialogue text",
            trailingContent = {
                CustomGlassStepper(
                    value = subtitleSize,
                    unit = "sp",
                    onValueChange = { viewModel.setSubtitleSize(it) },
                    min = 12,
                    max = 34,
                    step = 2,
                    hazeState = hazeState
                )
            }
        )

        // 2. Subtitle Color Row
        OpenSettingsRow(
            title = "Subtitle color",
            subtitle = "Text foreground hue for closed captions",
            trailingContent = {
                FrostedGlassDropdown(
                    options = colorOptions,
                    selected = subtitleColor.uppercase(),
                    onSelected = { viewModel.setSubtitleColor(it) },
                    hazeState = hazeState,
                    popoverWidth = 230.dp
                )
            }
        )

        // 3. Dark Background Box Row
        OpenSettingsToggleRow(
            title = "Dark background box",
            subtitle = "Draws high-contrast pill behind caption lines",
            checked = subtitleBgEnabled,
            onCheckedChange = { viewModel.setSubtitleBgEnabled(it) },
            hazeState = hazeState
        )

        // 4. Text Edge Outline Row
        OpenSettingsToggleRow(
            title = "Text edge outline",
            subtitle = "Adds drop shadow and outline around caption text",
            checked = subtitleOutline,
            onCheckedChange = { viewModel.setSubtitleOutline(it) },
            hazeState = hazeState
        )

        // 5. External Online Subtitles Row
        OpenSettingsToggleRow(
            title = "External online subtitles",
            subtitle = "Auto-downloads OpenSubtitles & SubDL matching tracks",
            checked = externalSubtitlesEnabled,
            onCheckedChange = { viewModel.setExternalSubtitlesEnabled(it) },
            hazeState = hazeState
        )

        // Bottom "Reset playback defaults" pill
        SettingsResetPill(
            text = "Reset playback defaults",
            onClick = { showResetDialog = true },
            hazeState = hazeState
        )

        // Shared Glass Confirmation Dialog
        if (showResetDialog) {
            EuthopiarGlassDialog(
                title = "Reset playback defaults?",
                message = "This will return video stream quality, buffer memory allocation, seek jump intervals, countdown automations, and closed-caption styles to their original defaults.",
                confirmText = "Reset defaults",
                dismissText = "Cancel",
                onConfirm = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                    viewModel.resetDefaults()
                    showResetDialog = false
                },
                onDismiss = { showResetDialog = false }
            )
        }
    }
}

/**
 * Cinema Viewport Glass Card — "LIVE CINEMA & CAPTION STAGE"
 */
@Composable
private fun LiveCinemaCaptionStageCard(
    quality: String,
    hardwareAcceleration: Boolean,
    subtitleSize: Int,
    subtitleColorName: String,
    subtitleBgEnabled: Boolean,
    subtitleOutline: Boolean,
    bufferSizeMb: Int,
    seekInterval: Int,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val activeColor = remember(subtitleColorName) { resolveSubtitleColor(subtitleColorName) }

    // Smooth buffer progress fraction
    val targetBufferProgress = (bufferSizeMb.coerceIn(0, 100) / 100f)
    val animatedBufferProgress by animateFloatAsState(
        targetValue = targetBufferProgress,
        animationSpec = tween(if (reduceMotion) 0 else 220),
        label = "bufferProgress"
    )

    // Hardware acceleration chip opacity
    val hwChipAlpha by animateFloatAsState(
        targetValue = if (hardwareAcceleration) 1f else 0.38f,
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "hwChipAlpha"
    )

    // Caption background box alpha
    val captionBgAlpha by animateFloatAsState(
        targetValue = if (subtitleBgEnabled) 0.72f else 0.0f,
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "captionBgAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .settingsGlass(
                shape = RoundedCornerShape(SettingsTokens.RadiusPreviewCard),
                hazeState = hazeState,
                blurRadius = 18.dp,
                customFill = Color.White.copy(alpha = 0.05f)
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Telemetry Row: Resolution Badge + GPU HW ACCEL Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution Badge
                Box(
                    modifier = Modifier
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusChip),
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = if (quality == "Auto") "Auto (1080p)" else "$quality HD",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = SettingsTokens.TextWhite88
                            )
                        )
                    }
                }

                // GPU HW ACCEL Chip (no green, smoothly dimmed to 38% if OFF)
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = hwChipAlpha }
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusChip),
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Memory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "GPU HW ACCEL",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = SettingsTokens.TextWhite82
                            )
                        )
                    }
                }
            }

            // Center Live Caption Viewport (80dp height, 12dp radius, dark cinema frame)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .border(
                        SettingsTokens.HairlineWidth,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(SettingsTokens.RadiusControl)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Caption Container Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = captionBgAlpha))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"I'm going to make him an offer he can't refuse.\"",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = subtitleSize.coerceIn(12, 34).sp,
                            lineHeight = (subtitleSize + 4).coerceIn(16, 38).sp,
                            textAlign = TextAlign.Center,
                            color = activeColor,
                            shadow = if (subtitleOutline) {
                                Shadow(
                                    color = Color.Black.copy(alpha = 0.90f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            } else null
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Telemetry Row: Buffer Meter + Seek Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Buffer Readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BUFFER",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            letterSpacing = (10 * 0.06).sp,
                            color = SettingsTokens.TextWhite45
                        )
                    )

                    // 2dp Monochrome Progress Bar
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedBufferProgress)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.90f))
                        )
                    }

                    Text(
                        text = "$bufferSizeMb MB",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontFeatureSettings = "tnum",
                            color = SettingsTokens.TextWhite88
                        )
                    )
                }

                // Seek Interval Chip
                Box(
                    modifier = Modifier
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusChip),
                            hazeState = hazeState,
                            blurRadius = 18.dp,
                            customBorderColor = Color.White.copy(alpha = 0.14f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = SettingsTokens.TextWhite72,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SEEK ±${seekInterval}s",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontFeatureSettings = "tnum",
                                color = SettingsTokens.TextWhite82
                            )
                        )
                    }
                }
            }
        }
    }
}
