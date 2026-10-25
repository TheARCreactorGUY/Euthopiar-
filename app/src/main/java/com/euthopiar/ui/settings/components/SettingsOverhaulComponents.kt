package com.euthopiar.ui.settings.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Global Design Tokens for the Euthopiar Settings Overhaul.
 * Strict monochrome, white-on-dark, and pure architectural frosted-glass design language.
 */
object SettingsTokens {
    val LcdBackground = Color(0xFF09090D)
    val AmoledBackground = Color(0xFF000000)

    val GlassFillLcd = Color.White.copy(alpha = 0.07f)
    val GlassFillAmoled = Color.White.copy(alpha = 0.06f)
    val GlassFillPressed = Color.White.copy(alpha = 0.10f)
    val GlassFillDisabled = Color.White.copy(alpha = 0.04f)

    val HairlineBorder = Color.White.copy(alpha = 0.16f)
    val HairlineBorderBright = Color.White.copy(alpha = 0.32f)
    val HairlineDivider = Color.White.copy(alpha = 0.14f)
    val HairlineDividerSubtle = Color.White.copy(alpha = 0.12f)

    val HairlineWidth = 0.5.dp

    val LowEndFallback = Color(0xEB16161B) // #16161B at ~92%
    val PopoverFallback = Color(0xF516161B) // #16161B at ~96%

    val PopoverFillLcd = Color(0xC7101016) // #101016 at 78%
    val PopoverFillAmoled = Color(0xD60A0A0E) // #0A0A0E at 84%

    val TextWhite96 = Color.White.copy(alpha = 0.96f)
    val TextWhite88 = Color.White.copy(alpha = 0.88f)
    val TextWhite82 = Color.White.copy(alpha = 0.82f)
    val TextWhite72 = Color.White.copy(alpha = 0.72f)
    val TextWhite54 = Color.White.copy(alpha = 0.54f)
    val TextWhite45 = Color.White.copy(alpha = 0.45f)
    val TextWhite42 = Color.White.copy(alpha = 0.42f)
    val TextWhite40 = Color.White.copy(alpha = 0.40f)
    val TextWhite30 = Color.White.copy(alpha = 0.30f)
    val TextDark = Color(0xFF0B0B0F)

    // Corner Radii
    val RadiusChip = 8.dp
    val RadiusControl = 12.dp
    val RadiusTile = 13.dp
    val RadiusTileEmblem = 15.dp
    val RadiusPopover = 18.dp
    val RadiusPreviewCard = 20.dp
    val RadiusDialog = 28.dp
    val RadiusPill = 999.dp

    // Ambient Haze blobbing colors for LCD dark mode
    val HazeIndigoBlob = Color(0xFF232845)
    val HazeVioletBlob = Color(0xFF2C2245)
    val HazeTealBlob = Color(0xFF1B2F3B)
}

/**
 * Checks if the device is low RAM to disable heavy GPU RenderEffects.
 */
fun isDeviceLowEnd(context: Context): Boolean {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true
    } catch (_: Exception) {
        false
    }
}

/**
 * Universal frosted glass modifier with Haze blur, AMOLED true black, and low-end fallback.
 */
fun Modifier.settingsGlass(
    shape: Shape = RoundedCornerShape(SettingsTokens.RadiusControl),
    hazeState: HazeState? = null,
    blurRadius: Dp = 18.dp,
    customBorderColor: Color = SettingsTokens.HairlineBorder,
    borderWidth: Dp = SettingsTokens.HairlineWidth,
    customFill: Color? = null
): Modifier = composed {
    val isAmoled = LocalAmoledMode.current
    val context = LocalContext.current
    val isLowEnd = remember(context) { isDeviceLowEnd(context) }

    val baseFill = customFill ?: when {
        isLowEnd -> SettingsTokens.LowEndFallback
        isAmoled -> SettingsTokens.GlassFillAmoled
        else -> SettingsTokens.GlassFillLcd
    }

    val canBlur = !isAmoled && !isLowEnd && hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val blurModifier = if (canBlur) {
        Modifier.hazeEffect(state = hazeState!!) {
            this.blurRadius = blurRadius
        }
    } else {
        Modifier
    }

    this
        .clip(shape)
        .then(blurModifier)
        .background(baseFill, shape)
        .border(borderWidth, customBorderColor, shape)
}

/**
 * Tactile 98% (0.98x) press scale modifier for interactive rows, buttons, toggles.
 */
fun Modifier.settingsPressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.98f,
    pressedAlpha: Float = 0.88f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) scaleDown else 1f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "settingsPressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "settingsPressAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}

/**
 * Architectural Settings Page Scaffold with:
 * - Dynamic AMOLED True Black (#000000) vs LCD Dark (#09090D) Canvas transition (280ms)
 * - Muted ambient Haze blobs behind scrolling content in LCD mode (completely disabled in AMOLED)
 * - Pinned frosted glass top bar with crossfading title on scroll
 * - 40x40dp rounded-square glass back button (13dp radius)
 * - Large 32sp Medium headline with letter spacing -0.025em
 * - 20dp horizontal screen margins and generous bottom clearance
 */
@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState = rememberHazeState(),
    overlay: @Composable (BoxScope.(HazeState) -> Unit)? = null,
    content: @Composable ColumnScope.(HazeState) -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Smooth canvas background transition
    val animatedBgColor by animateColorAsState(
        targetValue = if (isAmoled) SettingsTokens.AmoledBackground else SettingsTokens.LcdBackground,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "scaffoldBg"
    )

    // Collapsing header threshold: when user scrolls past the top headline (~80dp)
    val isScrolledPastHeadline by remember {
        derivedStateOf { scrollState.value > 160 }
    }

    val compactTopBarAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHeadline) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 180, easing = FastOutSlowInEasing),
        label = "compactTopBarAlpha"
    )

    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .then(
                if (!isAmoled) {
                    Modifier.drawBehind {
                        val maxDim = maxOf(size.width, size.height)
                        // Soft Indigo wash top-right
                        drawRect(
                            brush = Brush.radialGradient(
                                0.00f to SettingsTokens.HazeIndigoBlob.copy(alpha = 0.24f),
                                0.40f to SettingsTokens.HazeIndigoBlob.copy(alpha = 0.12f),
                                0.70f to SettingsTokens.HazeIndigoBlob.copy(alpha = 0.03f),
                                1.00f to Color.Transparent,
                                center = Offset(size.width * 0.88f, size.height * 0.12f),
                                radius = maxDim * 0.80f
                            )
                        )
                        // Soft Violet / Teal wash mid-left
                        drawRect(
                            brush = Brush.radialGradient(
                                0.00f to SettingsTokens.HazeTealBlob.copy(alpha = 0.20f),
                                0.45f to SettingsTokens.HazeVioletBlob.copy(alpha = 0.09f),
                                0.80f to Color.Transparent,
                                center = Offset(size.width * 0.12f, size.height * 0.52f),
                                radius = maxDim * 0.72f
                            )
                        )
                    }
                } else Modifier
            )
    ) {
        // Main Scrolling Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(
                    top = statusBarTopInset + 8.dp,
                    bottom = navBarBottomInset + 32.dp
                )
        ) {
            // Expanded Header Area
            // Top Row: 40x40dp Glass Back Button (with 48x48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .settingsPressable(
                        onClickLabel = "Navigate back",
                        onClick = {
                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            onBack()
                        }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusTile),
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = SettingsTokens.TextWhite96,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large 32sp Medium Headline
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    lineHeight = 33.sp,
                    letterSpacing = (-0.8).sp,
                    color = SettingsTokens.TextWhite96
                ),
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Screen Specific Body Content
            content(hazeState)
        }

        // Pinned Compact Frosted Glass Top Bar (56dp + status bar inset)
        if (compactTopBarAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp + statusBarTopInset)
                    .graphicsLayer { alpha = compactTopBarAlpha }
                    .background(
                        if (isAmoled) SettingsTokens.AmoledBackground.copy(alpha = 0.88f)
                        else SettingsTokens.LcdBackground.copy(alpha = 0.82f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks to prevent touch leak to underlying items */ }
                    .settingsGlass(
                        shape = RoundedCornerShape(0.dp),
                        hazeState = hazeState,
                        blurRadius = 18.dp,
                        borderWidth = 0.dp
                    )
                    .drawBehind {
                        // 0.5dp bottom hairline divider
                        drawLine(
                            color = SettingsTokens.HairlineDivider,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = SettingsTokens.HairlineWidth.toPx()
                        )
                    }
                    .padding(top = statusBarTopInset, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pinned 40x40dp Glass Back Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .settingsPressable(
                                onClickLabel = "Navigate back",
                                onClick = {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onBack()
                                }
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .settingsGlass(
                                    shape = RoundedCornerShape(SettingsTokens.RadiusTile),
                                    hazeState = hazeState,
                                    blurRadius = 18.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = SettingsTokens.TextWhite96,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = SettingsTokens.TextWhite96
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Overlay slot (in-window sheets and dialogs)
        overlay?.invoke(this, hazeState)
    }
}

/**
 * Open Section Header:
 * - 11sp Medium ALL CAPS, letter spacing +0.08em, white 45%
 * - 26dp top margin, 10dp bottom padding
 * - 0.5dp white 14% hairline divider directly below the label
 */
@Composable
fun OpenSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = (11 * 0.08).sp,
                color = SettingsTokens.TextWhite45
            ),
            modifier = Modifier
                .padding(bottom = 10.dp)
                .semantics { heading() }
        )

        HorizontalDivider(
            thickness = SettingsTokens.HairlineWidth,
            color = SettingsTokens.HairlineDivider
        )
    }
}

/**
 * Open Settings Row:
 * - Minimum height 56dp, vertical padding 15dp
 * - Bottom border 0.5dp white 14% hairline divider
 * - Left column (weight 1f, min 16dp gap before right accessory)
 * - Title: 15sp Medium white 96%
 * - Subtitle: 12sp Regular white 54%
 * - Right accessory slot (wrap content)
 */
@Composable
fun OpenSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isClickable = onClick != null && enabled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = SettingsTokens.HairlineDivider,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = SettingsTokens.HairlineWidth.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .then(
                    if (isClickable) {
                        Modifier
                            .tvFocusable(shape = RoundedCornerShape(SettingsTokens.RadiusControl))
                            .settingsPressable(
                                enabled = enabled,
                                onClick = {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onClick?.invoke()
                                }
                            )
                    } else Modifier
                )
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = if (enabled) SettingsTokens.TextWhite96 else SettingsTokens.TextWhite40
                        )
                    )

                    if (titleBadge != null) {
                        titleBadge()
                    }
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (enabled) SettingsTokens.TextWhite54 else SettingsTokens.TextWhite30
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Accessory Column
            if (trailingContent != null) {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailingContent()
                }
            }
        }
    }
}

/**
 * Open Settings Toggle Row:
 * - Minimum height 56dp, vertical padding 15dp
 * - Bottom border 0.5dp white 14% hairline divider
 * - Left column (weight 1f, min 16dp gap before toggle)
 * - Title: 15sp Medium white 96%
 * - Subtitle: 12sp Regular white 54%
 * - Tapping anywhere on the row or on the switch toggles the state with 98% press scale
 * - Merged semantics with Role.Switch for screen readers
 */
@Composable
fun OpenSettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current
    val toggleAction = {
        if (enabled) {
            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
            onCheckedChange(!checked)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = SettingsTokens.HairlineDivider,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = SettingsTokens.HairlineWidth.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics(mergeDescendants = true) {
                    this.role = Role.Switch
                }
                .tvFocusable(shape = RoundedCornerShape(SettingsTokens.RadiusControl))
                .settingsPressable(
                    enabled = enabled,
                    role = Role.Switch,
                    onClick = toggleAction
                )
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = if (enabled) SettingsTokens.TextWhite96 else SettingsTokens.TextWhite40
                        )
                    )

                    if (titleBadge != null) {
                        titleBadge()
                    }
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (enabled) SettingsTokens.TextWhite54 else SettingsTokens.TextWhite30
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Toggle Column
            CustomGlassToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                hazeState = hazeState
            )
        }
    }
}

/**
 * Custom Frosted Glass Toggle (Switch):
 * - Dimensions: 48dp wide by 28dp tall, full pill radius (999dp), 3dp internal padding
 * - OFF State: White 7% fill, 18dp blur, 0.5dp white 16% hairline border, 20dp thumb on left in white 48%
 * - ON State: White 72% tinted glass lens fill, 0.5dp white 35% border, 20dp thumb translated 20dp right in solid #0B0B0F
 * - 200ms smooth spring/ease translation, 180ms track opacity crossfade, 98% press scale
 */
@Composable
fun CustomGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.80f, stiffness = 550f),
        label = "glassToggleThumb"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) Color.White.copy(alpha = 0.35f) else SettingsTokens.HairlineBorder,
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "trackBorder"
    )

    val trackBgColor by animateColorAsState(
        targetValue = when {
            checked -> Color.White.copy(alpha = 0.72f)
            isAmoled -> SettingsTokens.GlassFillAmoled
            else -> SettingsTokens.GlassFillLcd
        },
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "trackBg"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) SettingsTokens.TextDark else Color.White.copy(alpha = 0.48f),
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "thumbColor"
    )

    Box(
        modifier = modifier
            .size(width = 54.dp, height = 48.dp) // Touch target >= 48dp
            .settingsPressable(
                enabled = enabled,
                role = Role.Switch,
                onClick = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                    onCheckedChange(!checked)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .then(
                    if (!checked && !isAmoled && hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.hazeEffect(state = hazeState) { blurRadius = 18.dp }
                    } else Modifier
                )
                .background(trackBgColor, CircleShape)
                .border(SettingsTokens.HairlineWidth, trackBorderColor, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

/**
 * Monochrome Segmented Pill Row:
 * - Equal-width horizontal row (`Modifier.weight(1f)`)
 * - 8dp horizontal gap, 12dp top gap below row's title
 * - Unselected: 36dp tall (48dp touch target), 12dp radius, white 7% fill, 18dp blur, 0.5dp white 16% border, 12sp Medium white 72%
 * - Selected: 36dp tall, 12dp radius, solid white (#FFFFFF) fill, 0.5dp white 100% border, 12sp Medium near-black #0B0B0F
 * - Selection transition: 160ms crossfade, 98% press scale
 */
@Composable
fun <T> MonochromeSegmentedPillRow(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    labelProvider: (T) -> String = { it.toString() },
    isAllowed: (T) -> Boolean = { true }
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isCurrentSelected = option == selected
            val enabled = isAllowed(option)

            val bgColor by animateColorAsState(
                targetValue = when {
                    !enabled -> Color.White.copy(alpha = 0.03f)
                    isCurrentSelected -> Color.White
                    isAmoled -> SettingsTokens.GlassFillAmoled
                    else -> SettingsTokens.GlassFillLcd
                },
                animationSpec = tween(if (reduceMotion) 0 else 160),
                label = "segBg"
            )

            val borderColor by animateColorAsState(
                targetValue = when {
                    !enabled -> Color.White.copy(alpha = 0.05f)
                    isCurrentSelected -> Color.White
                    else -> SettingsTokens.HairlineBorder
                },
                animationSpec = tween(if (reduceMotion) 0 else 160),
                label = "segBorder"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    !enabled -> SettingsTokens.TextWhite30
                    isCurrentSelected -> SettingsTokens.TextDark
                    else -> SettingsTokens.TextWhite72
                },
                animationSpec = tween(if (reduceMotion) 0 else 160),
                label = "segText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp) // Touch target >= 48dp
                    .settingsPressable(
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            if (enabled && !isCurrentSelected) {
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                onSelected(option)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                        .then(
                            if (!isCurrentSelected && !isAmoled && hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.hazeEffect(state = hazeState) { blurRadius = 18.dp }
                            } else Modifier
                        )
                        .background(bgColor, RoundedCornerShape(SettingsTokens.RadiusControl))
                        .border(
                            SettingsTokens.HairlineWidth,
                            borderColor,
                            RoundedCornerShape(SettingsTokens.RadiusControl)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(option),
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            color = textColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Option item model for the Frosted Glass Floating Popover Menu.
 */
data class PopoverOption<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val colorSwatch: Color? = null
)

/**
 * Frosted Glass Dropdown Trigger Pill with Floating Popover Menu:
 * - Trigger: 36dp tall, 12dp radius, white 7% glass fill, 18dp blur, 0.5dp white 16% border (brightens to 32% when open).
 * - Label in 12sp Medium white 96% + 7dp gap + 14dp outline chevron-down (rotates 180° when expanded).
 * - Optional color swatch (10x10dp with 3dp radius).
 * - Floating Popover: 18dp corner radius, dark frosted glass base (#101016 at 78% / #0A0A0E at 84%) with 22dp blur, 0.5dp white 18% border.
 * - Backdrop: black at 28% opacity dismiss layer.
 * - Rows: 0.5dp white 12% hairlines. Title 14sp Medium white 96%, description 11.5sp Regular white 54%.
 * - Selected Indicator: 20dp solid white circle with 12dp outline checkmark in #0B0B0F.
 */
@Composable
fun <T> FrostedGlassDropdown(
    options: List<PopoverOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    popoverWidth: Dp = 240.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    val currentOption = remember(options, selected) {
        options.firstOrNull { it.value == selected }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 180, easing = FastOutSlowInEasing),
        label = "chevronRot"
    )

    val triggerBorderColor by animateColorAsState(
        targetValue = if (expanded) SettingsTokens.HairlineBorderBright else SettingsTokens.HairlineBorder,
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "triggerBorder"
    )

    // Trigger Pill
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp) // 48dp touch target
            .settingsPressable(
                onClickLabel = "Change selection",
                onClick = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                    expanded = !expanded
                }
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .settingsGlass(
                    shape = RoundedCornerShape(SettingsTokens.RadiusControl),
                    hazeState = hazeState,
                    blurRadius = 18.dp,
                    customBorderColor = triggerBorderColor
                )
                .padding(start = 14.dp, end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (currentOption?.colorSwatch != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(currentOption.colorSwatch)
                            .border(SettingsTokens.HairlineWidth, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = currentOption?.title ?: selected.toString(),
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = SettingsTokens.TextWhite96
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(7.dp))

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }
    }

    // Floating Popover Menu Dialog
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            // Dismiss Backdrop (black at 28% opacity)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Floating Popover Card
                Box(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = popoverWidth.coerceAtLeast(240.dp))
                        .heightIn(max = 390.dp)
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Intercept clicks inside card
                        )
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusPopover),
                            hazeState = hazeState,
                            blurRadius = 22.dp,
                            customBorderColor = Color.White.copy(alpha = 0.18f),
                            customFill = if (isAmoled) SettingsTokens.PopoverFillAmoled else SettingsTokens.PopoverFillLcd
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        options.forEachIndexed { index, option ->
                            val isSelected = option.value == selected

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .settingsPressable(
                                        onClick = {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                            onSelected(option.value)
                                            expanded = false
                                        }
                                    )
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Column: Title + Optional Description
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (option.colorSwatch != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(option.colorSwatch)
                                                        .border(SettingsTokens.HairlineWidth, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                                )
                                            }

                                            Text(
                                                text = option.title,
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp,
                                                    color = SettingsTokens.TextWhite96
                                                )
                                            )
                                        }

                                        if (!option.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = option.description,
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp,
                                                    color = SettingsTokens.TextWhite54
                                                )
                                            )
                                        }
                                    }

                                    // Right Column: 20x20dp Reservation Slot
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Selected",
                                                    tint = SettingsTokens.TextDark,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 0.5dp divider between items
                            if (index < options.lastIndex) {
                                HorizontalDivider(
                                    thickness = SettingsTokens.HairlineWidth,
                                    color = SettingsTokens.HairlineDividerSubtle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Frosted Glass Stepper (`− 10s +` and `− 18sp +`):
 * - Container: 36dp tall, 12dp radius, white 7% glass fill, 18dp blur, 0.5dp white 16% border, 4dp internal padding
 * - Minus & Plus: 28x28dp hitbox (8dp corner radius) with 14dp outline icon in white 85% (dims to white 30% when disabled)
 * - Press feedback: 95% scale + white 12% highlight
 * - Center readout: min width 44dp, centered, 12sp Medium white 100%, tabular numerals (`tnum`)
 */
@Composable
fun CustomGlassStepper(
    value: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int = 1,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    val canDecrease = enabled && value > min
    val canIncrease = enabled && value < max

    Box(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .settingsGlass(
                    shape = RoundedCornerShape(SettingsTokens.RadiusControl),
                    hazeState = hazeState,
                    blurRadius = 18.dp
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Minus Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .settingsPressable(
                            enabled = canDecrease,
                            scaleDown = 0.95f,
                            onClickLabel = "Decrease",
                            onClick = {
                                if (canDecrease) {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onValueChange(maxOf(min, value - step))
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = "Decrease",
                        tint = if (canDecrease) Color.White.copy(alpha = 0.85f) else SettingsTokens.TextWhite30,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Center Readout
                Box(
                    modifier = Modifier.widthIn(min = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$value$unit",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFeatureSettings = "tnum",
                            color = if (enabled) Color.White else SettingsTokens.TextWhite40
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Plus Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .settingsPressable(
                            enabled = canIncrease,
                            scaleDown = 0.95f,
                            onClickLabel = "Increase",
                            onClick = {
                                if (canIncrease) {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onValueChange(minOf(max, value + step))
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Increase",
                        tint = if (canIncrease) Color.White.copy(alpha = 0.85f) else SettingsTokens.TextWhite30,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard Settings Pill Button:
 * - Solid White Pill: #FFFFFF background with #0B0B0F dark 13sp Medium label
 * - Frosted Glass Pill: white 7% fill, 18dp blur, 0.5dp white 16% border with white 96% 13sp Medium label
 * - 36dp tall with 48dp minimum touch target
 */
@Composable
fun SettingsPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSolidWhite: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .settingsPressable(
                enabled = enabled && !isLoading,
                onClick = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .then(
                    if (isSolidWhite) {
                        Modifier.background(if (enabled) Color.White else Color.White.copy(alpha = 0.5f), CircleShape)
                    } else {
                        Modifier.settingsGlass(
                            shape = CircleShape,
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        )
                    }
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = if (isSolidWhite) SettingsTokens.TextDark else Color.White,
                    strokeWidth = 1.7.dp
                )
            } else {
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = when {
                            !enabled -> SettingsTokens.TextWhite40
                            isSolidWhite -> SettingsTokens.TextDark
                            else -> SettingsTokens.TextWhite96
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Bottom "Reset Defaults" Glass Pill:
 * - 42dp tall, full pill (999dp), white 7% fill, 18dp blur, 0.5dp white 16% border, 20dp horizontal padding
 * - 15dp outline reset arrow + 8dp gap + 13sp Medium white 88% label
 */
@Composable
fun SettingsResetPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .settingsPressable(
                    onClick = {
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .settingsGlass(
                        shape = CircleShape,
                        hazeState = hazeState,
                        blurRadius = 18.dp
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )

                    Text(
                        text = text,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = SettingsTokens.TextWhite88
                        )
                    )
                }
            }
        }
    }
}

/**
 * Compact Glass Metadata Chip (e.g., "Media3", "Compose", "Multi-device", "2.0x"):
 * - 8dp corner radius, white 7% fill, 0.5dp white 16% border
 * - 5dp vertical / 10dp horizontal padding, 11sp Medium white 82%
 */
@Composable
fun SettingsMetadataChip(
    text: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    Box(
        modifier = modifier
            .settingsGlass(
                shape = RoundedCornerShape(SettingsTokens.RadiusChip),
                hazeState = hazeState,
                blurRadius = 18.dp
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = SettingsTokens.TextWhite82
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
