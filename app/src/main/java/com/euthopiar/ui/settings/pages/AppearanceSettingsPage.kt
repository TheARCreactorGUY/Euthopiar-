package com.euthopiar.ui.settings.pages

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.data.preferences.CardScaleMode
import com.euthopiar.data.preferences.SectionLabelStyle
import com.euthopiar.data.preferences.Top10Style
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.longpress.LongPressSheetStyle
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState

/**
 * Redesigned "Appearance" Settings Screen (formerly "Appearance & Interface").
 *
 * Strict High-Fidelity Design Specifications:
 * - Editorial Top Bar with 40x40dp glass back button & crossfading collapsed title.
 * - Bold 32sp Medium headline ("Appearance").
 * - Upgraded "LIVE INTERFACE STAGE" preview card:
 *   - Architectural glass frame (20dp corners, 5% white glass fill, 18dp blur).
 *   - Live section title preview reacting to Accent bar / Eyebrow / Rule line (pure white accent bar, never yellow).
 *   - 3-Poster miniature row with live width/height resizing matching Flagship / Balanced / Compact.
 *   - Live Top 10 rank reactivity ("01", "02", "03" tabular numerals with white glow or glass corner badge).
 * - Section 1: HOME CATALOG & SCALING with 3 Monochrome Segmented Pill Rows (no red borders or fills).
 * - Section 2: CARDS & MICRO-INTERACTIONS with Frosted Glass Floating Popover for Long-Press style,
 *   and Custom Glass Toggles (48x28dp) for Reduce Motion and AMOLED True Black.
 * - Bottom "Reset interface defaults" glass pill wired to shared Glass Confirmation Dialog.
 * - 100% monochrome design language, 98% press scale on all interactive elements.
 */
@Composable
fun AppearanceSettingsPage(
    onBack: () -> Unit,
    viewModel: AppearanceSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val cardScale by viewModel.cardScaleMode.collectAsState()
    val top10Style by viewModel.top10Style.collectAsState()
    val sectionLabelStyle by viewModel.sectionLabelStyle.collectAsState()
    val longPressStyle by viewModel.longPressStyle.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val amoledMode by viewModel.amoledMode.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    val longPressOptions = remember {
        LongPressSheetStyle.entries.map { style ->
            PopoverOption(
                value = style,
                title = style.displayName,
                description = style.description
            )
        }
    }

    SettingsScaffold(
        title = "Appearance",
        onBack = onBack,
        modifier = modifier
    ) { hazeState ->

        // Upgraded Top Preview Card — "LIVE INTERFACE STAGE"
        LiveInterfaceStageCard(
            cardScale = cardScale,
            top10Style = top10Style,
            sectionLabelStyle = sectionLabelStyle,
            hazeState = hazeState
        )

        // Section 1: Home Catalog & Scaling
        OpenSettingsSectionHeader(title = "Home Catalog & Scaling")

        // 1. Poster Card Scale Row
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
                        text = "Poster card scale",
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
                        text = cardScale.description,
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
                    text = "${cardScale.verticalFactor}x",
                    hazeState = hazeState
                )
            }

            MonochromeSegmentedPillRow(
                options = CardScaleMode.entries,
                selected = cardScale,
                onSelected = { viewModel.setCardScaleMode(it) },
                hazeState = hazeState,
                labelProvider = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
            )
        }

        // 2. Section Header Style Row
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Section header style",
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
                    text = "Ornamentation applied across catalog section rows",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = SettingsTokens.TextWhite54
                    )
                )
            }

            MonochromeSegmentedPillRow(
                options = SectionLabelStyle.entries,
                selected = sectionLabelStyle,
                onSelected = { viewModel.setSectionLabelStyle(it) },
                hazeState = hazeState,
                labelProvider = { it.label }
            )
        }

        // 3. Top 10 Row Styling Row
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Top 10 row styling",
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
                    text = top10Style.description,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = SettingsTokens.TextWhite54
                    )
                )
            }

            MonochromeSegmentedPillRow(
                options = Top10Style.entries,
                selected = top10Style,
                onSelected = { viewModel.setTop10Style(it) },
                hazeState = hazeState,
                labelProvider = { it.label }
            )
        }

        // Section 2: Cards & Micro-Interactions
        OpenSettingsSectionHeader(title = "Cards & Micro-Interactions")

        OpenSettingsRow(
            title = "Long-press card style",
            subtitle = "Preview sheet when holding down any media poster",
            trailingContent = {
                FrostedGlassDropdown(
                    options = longPressOptions,
                    selected = longPressStyle,
                    onSelected = { viewModel.setLongPressStyle(it) },
                    hazeState = hazeState,
                    popoverWidth = 290.dp
                )
            }
        )

        OpenSettingsToggleRow(
            title = "Reduce motion",
            subtitle = "Disables intense physics-based spring animations",
            checked = reduceMotion,
            onCheckedChange = { viewModel.setReduceMotion(it) },
            hazeState = hazeState
        )

        OpenSettingsToggleRow(
            title = "AMOLED true black",
            subtitle = "Pure #000000 pitch-black with zero color haze for OLED displays. Turn off for gentle atmospheric color haze on LCD displays.",
            checked = amoledMode,
            onCheckedChange = { viewModel.setAmoledMode(it) },
            hazeState = hazeState
        )

        // Bottom "Reset interface defaults" pill
        SettingsResetPill(
            text = "Reset interface defaults",
            onClick = { showResetDialog = true },
            hazeState = hazeState
        )

        // Shared Glass Confirmation Dialog
        if (showResetDialog) {
            EuthopiarGlassDialog(
                title = "Reset interface defaults?",
                message = "This will return all catalog scaling, header ornamentation, long-press preview styles, and motion preferences on this screen to their default values.",
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
 * Architectural Glass Stage Card — "LIVE INTERFACE STAGE"
 */
@Composable
private fun LiveInterfaceStageCard(
    cardScale: CardScaleMode,
    top10Style: Top10Style,
    sectionLabelStyle: SectionLabelStyle,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current

    // Animated poster dimensions based on scale mode
    val targetWidth = when (cardScale) {
        CardScaleMode.FLAGSHIP -> 64.dp
        CardScaleMode.BALANCED -> 56.dp
        CardScaleMode.COMPACT -> 48.dp
    }
    val targetHeight = when (cardScale) {
        CardScaleMode.FLAGSHIP -> 90.dp
        CardScaleMode.BALANCED -> 78.dp
        CardScaleMode.COMPACT -> 68.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(if (reduceMotion) 0 else 220),
        label = "posterWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(if (reduceMotion) 0 else 220),
        label = "posterHeight"
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
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: "LIVE INTERFACE STAGE" + Active Scale Preset Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE INTERFACE STAGE",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        letterSpacing = (10 * 0.08).sp,
                        color = SettingsTokens.TextWhite45
                    )
                )

                Box(
                    modifier = Modifier
                        .settingsGlass(
                            shape = RoundedCornerShape(6.dp),
                            hazeState = hazeState,
                            blurRadius = 18.dp,
                            customFill = Color.White.copy(alpha = 0.07f),
                            customBorderColor = Color.White.copy(alpha = 0.14f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${cardScale.name.lowercase().replaceFirstChar { it.uppercase() }} · ${cardScale.verticalFactor}x",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = SettingsTokens.TextWhite82
                        )
                    )
                }
            }

            // Live Section Title Preview ("Trending movies")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (sectionLabelStyle) {
                    SectionLabelStyle.AccentBar -> {
                        // 2dp wide by 12dp tall pure white vertical pill bar (#FFFFFF, never yellow)
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "Trending movies",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        )
                    }
                    SectionLabelStyle.Eyebrow -> {
                        Column {
                            Text(
                                text = "TRENDING MOVIES",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    letterSpacing = (10 * 0.09).sp,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            )
                        }
                    }
                    SectionLabelStyle.RuleLine -> {
                        Text(
                            text = "Trending movies",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        )
                        HorizontalDivider(
                            thickness = SettingsTokens.HairlineWidth,
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Live 3-Poster Miniature Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val rankAlphas = listOf(0.92f, 0.60f, 0.35f)
                val rankLabels = listOf("01", "02", "03")

                for (i in 0 until 3) {
                    Box(
                        modifier = Modifier
                            .width(animatedWidth)
                            .height(animatedHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    0.0f to Color.White.copy(alpha = 0.11f),
                                    1.0f to Color.White.copy(alpha = 0.03f),
                                    start = Offset(0f, 0f),
                                    end = Offset(animatedWidth.value, animatedHeight.value)
                                )
                            )
                            .border(SettingsTokens.HairlineWidth, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        if (top10Style == Top10Style.PosterTinted) {
                            // Radial white/silver ambient glow behind numeral at bottom-left
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                0.0f to Color.White.copy(alpha = 0.20f),
                                                1.0f to Color.Transparent,
                                                center = Offset(16f, size.height - 14f),
                                                radius = 36f
                                            )
                                        )
                                    }
                            )
                            Text(
                                text = rankLabels[i],
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    lineHeight = 20.sp,
                                    fontFeatureSettings = "tnum",
                                    color = Color.White.copy(alpha = rankAlphas[i])
                                ),
                                modifier = Modifier.padding(start = 5.dp, bottom = 3.dp)
                            )
                        } else {
                            // Frosted glass corner badge for Top 10 rank
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .settingsGlass(
                                        shape = RoundedCornerShape(6.dp),
                                        hazeState = hazeState,
                                        blurRadius = 18.dp,
                                        customFill = Color.White.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${rankLabels[i]}",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        fontFeatureSettings = "tnum",
                                        color = Color.White.copy(alpha = rankAlphas[i])
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
