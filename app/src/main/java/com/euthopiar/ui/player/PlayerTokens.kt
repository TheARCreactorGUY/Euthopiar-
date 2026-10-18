package com.euthopiar.ui.player

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion

/**
 * Standard Design Tokens and Glass Recipes for the Euthopiar Video Player.
 * Defined once, referenced everywhere across player controls, overlays, panels and dialogs.
 */
object PlayerTokens {

    // =========================================================================
    // 1. COLOR & GLASS TOKENS
    // =========================================================================

    /** Near-black label color for text/icons on solid white elements */
    val LabelDark = Color(0xFF0B0B0F)

    /** Video-Overlay Glass: 36% black fill + 6% white lift, 0.5dp white 16% hairline, NO blur */
    val VideoOverlayGlassBase = Color.Black.copy(alpha = 0.36f)
    val VideoOverlayGlassLift = Color.White.copy(alpha = 0.06f)
    // Combined single composite color for fast rendering without multi-pass over video
    val VideoOverlayGlassFill = Color(0x6A121216)
    val VideoOverlayGlassBorder = Color.White.copy(alpha = 0.16f)

    /** Standard Glass (sheets, panels, pills over non-video surfaces) */
    val StandardGlassFill = Color.White.copy(alpha = 0.07f)
    val StandardGlassBorder = Color.White.copy(alpha = 0.16f)
    val StandardGlassBlur: Dp = 18.dp

    /** Sheet & Side Panel Glass (sitting over scrim, not direct video) */
    val SheetGlassFill = Color(0xFF0B0B0F).copy(alpha = 0.74f)
    val SheetGlassAmoledFill = Color(0xFF050507).copy(alpha = 0.88f)
    val LowEndFallbackFill = Color(0xFF0B0B0F).copy(alpha = 0.94f)
    val SheetGlassBorder = Color.White.copy(alpha = 0.16f)
    val SheetGlassBlur: Dp = 22.dp

    /** Failover notification pill glass fill (45% black) */
    val FailoverPillFill = Color.Black.copy(alpha = 0.45f)

    /** Track and Scrubber Colors */
    val TrackUnplayed = Color.White.copy(alpha = 0.18f)
    val TrackBuffered = Color.White.copy(alpha = 0.32f)
    val TrackPlayed = Color.White.copy(alpha = 0.90f)

    /** Scrim Gradients */
    // Top scrim: black 55% at top fading to 0 by 30% of screen height
    val TopScrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Black.copy(alpha = 0.55f),
            0.30f to Color.Transparent,
            1.00f to Color.Transparent
        )
    )

    // Bottom scrim: black fading from 0 at 50% height to 78% at bottom edge
    val BottomScrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.50f to Color.Transparent,
            1.00f to Color.Black.copy(alpha = 0.78f)
        )
    )

    /** Panel backdrop scrim: 30% black */
    val SidePanelScrim = Color.Black.copy(alpha = 0.30f)

    /** Portrait bottom sheet backdrop scrim: 60% black */
    val SheetScrim = Color.Black.copy(alpha = 0.60f)

    // =========================================================================
    // 2. CORNER RADII
    // =========================================================================
    val RadiusCircle = CircleShape
    val RadiusPill = RoundedCornerShape(16.dp)
    val RadiusSmall = RoundedCornerShape(12.dp)
    val RadiusMedium = RoundedCornerShape(16.dp)
    val RadiusSheet = RoundedCornerShape(28.dp)
    val RadiusSidePanel = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    val RadiusBottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    // =========================================================================
    // 3. ICON SIZES & TOUCH TARGETS
    // =========================================================================
    val TouchTargetMin: Dp = 44.dp
    val IconDock: Dp = 22.dp
    val IconHeader: Dp = 20.dp
    val IconBackArrow: Dp = 22.dp
    val IconSeekButton: Dp = 26.dp
    val IconPlayPause: Dp = 32.dp
    val IconPill: Dp = 16.dp
    val IconRow: Dp = 20.dp
    val IconCheck: Dp = 14.dp

    val SizeHeaderButton: Dp = 44.dp
    val SizeSeekButtonLandscape: Dp = 56.dp
    val SizeSeekButtonPortrait: Dp = 48.dp
    val SizePlayPauseLandscape: Dp = 76.dp
    val SizePlayPausePortrait: Dp = 68.dp
    val SizeExpanderCircle: Dp = 32.dp
    val SizeActiveDot: Dp = 4.dp
    val SizeCheckCircle: Dp = 20.dp
    val SizeLockCircle: Dp = 48.dp
    val PillMarginTop: Dp = 14.dp

    // =========================================================================
    // 4. MOTION & TIMING CONSTANTS (IN MILLISECONDS)
    // =========================================================================
    const val ControlsAutoHideMs: Long = 3500L
    const val ControlsShowDuration: Int = 200
    const val ControlsHideDuration: Int = 180
    val ControlsSlideOffset: Dp = 8.dp

    const val PressFeedbackInMs: Int = 100
    const val PressFeedbackOutMs: Int = 150
    const val PressScaleFactor: Float = 0.98f

    const val PlayPauseCrossfadeMs: Int = 160
    const val SeekRotateDurationMs: Int = 200
    const val SeekRotateDegrees: Float = 20f

    const val BufferingDelayMs: Long = 300L
    const val BufferingFadeMs: Int = 150
    const val BufferingCycleMs: Int = 1100

    const val DockChevronRotateMs: Int = 200
    const val DockExpandDurationMs: Int = 240
    const val DockCollapseDurationMs: Int = 180
    const val DockStaggerMs: Int = 40

    const val HudEnterMs: Int = 120
    const val HudExitMs: Int = 200
    const val HudIdleTimeoutMs: Long = 600L

    const val ScrubBadgeEnterMs: Int = 120
    const val ScrubBadgeExitMs: Int = 160

    const val DoubleTapRippleMs: Int = 300
    const val DoubleTapRippleFadeMs: Int = 200
    const val DoubleTapWindowMs: Long = 700L

    const val Pill2XEnterMs: Int = 160
    const val Pill2XExitMs: Int = 120

    const val AspectPillHoldMs: Long = 800L
    const val AspectPillCrossfadeMs: Int = 120

    const val FailoverEnterMs: Int = 280
    const val FailoverExitMs: Int = 200
    const val FailoverCountdownMs: Int = 2500

    const val SidePanelEnterMs: Int = 320
    const val SidePanelExitMs: Int = 240
    const val SidePanelScrimFadeMs: Int = 240
    const val SidePanelTabCrossfadeMs: Int = 180
    const val SidePanelRowStaggerMs: Int = 40

    const val SheetEnterMs: Int = 340
    const val SheetExitMs: Int = 240

    const val SourcesFooterWaveCycleMs: Long = 960L
    const val SourcesFooterWaveStaggerMs: Long = 160L
    const val SourcesFooterFadeMs: Int = 150
    const val SourcesFooterCollapseMs: Int = 220

    const val SourceRowArrivalPacingMs: Long = 400L
    const val SourceRowHeightExpandMs: Int = 280
    const val SourceRowFadeRiseMs: Int = 220

    const val ReduceMotionFadeInMs: Int = 120
    const val ReduceMotionFadeOutMs: Int = 100

    // Easings (Calm, premium, no overshoot, no bounce)
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EaseIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val StandardEasing = FastOutSlowInEasing

    // =========================================================================
    // 5. TYPOGRAPHY (Regular & Medium only, never bold)
    // =========================================================================
    val VideoTextShadow = Shadow(
        color = Color.Black.copy(alpha = 0.35f),
        offset = Offset(0f, 1f),
        blurRadius = 3f
    )

    val HeaderTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.White,
        shadow = VideoTextShadow
    )

    val HeaderSubtitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.White.copy(alpha = 0.70f),
        shadow = VideoTextShadow
    )

    val Timecode = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
        color = Color.White.copy(alpha = 0.80f),
        shadow = VideoTextShadow
    )

    val ScrubBadgeTime = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
        color = Color.White
    )

    val ScrubBadgeOffset = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
        color = Color.White.copy(alpha = 0.70f)
    )

    val PillText = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.sp,
        color = Color.White
    )

    val AspectPillText = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = Color.White
    )

    val SheetHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
        color = Color.White
    )

    val SheetSubtitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.White.copy(alpha = 0.70f)
    )

    val SectionLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = Color.White.copy(alpha = 0.45f)
    )

    val RowTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.White
    )

    val RowMeta = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.White.copy(alpha = 0.70f)
    )

    val TabLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = Color.White
    )

    val TooltipLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color.White
    )
}

/**
 * Checks if device is low-ram/low-end to provide zero-blur solid fallback.
 */
fun Context.isLowEndDevice(): Boolean {
    return try {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true
    } catch (_: Exception) {
        false
    }
}

/**
 * Video-Overlay Glass Modifier:
 * Applies black 36% fill + white 6% lift, 0.5dp hairline white 16%, NO blur.
 */
fun Modifier.videoOverlayGlass(
    shape: Shape = PlayerTokens.RadiusCircle
): Modifier = this
    .clip(shape)
    .background(PlayerTokens.VideoOverlayGlassFill, shape)
    .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, shape)

/**
 * Sheet / Panel Glass Modifier:
 * Over scrim. Translucent fill ~74% (or slightly darker base on AMOLED / solid on low-end), hairline on edge facing video, 28dp corners.
 */
fun Modifier.sheetGlass(
    shape: Shape = PlayerTokens.RadiusSheet,
    border: Boolean = true
): Modifier = composed {
    val isAmoled = LocalAmoledMode.current
    val context = LocalContext.current
    val isLowEnd = remember(context) { context.isLowEndDevice() }

    val bgColor = when {
        isLowEnd -> PlayerTokens.LowEndFallbackFill
        isAmoled -> PlayerTokens.SheetGlassAmoledFill
        else -> PlayerTokens.SheetGlassFill
    }

    this
        .clip(shape)
        .background(bgColor, shape)
        .then(
            if (border) Modifier.border(HairlineWidth, PlayerTokens.SheetGlassBorder, shape)
            else Modifier
        )
}

/**
 * Standardized Player Press Feedback Modifier:
 * Scales to 98% (never 94%) with 100ms in and 150ms out ease-out.
 * Adds faint white wash on glass (12%) or drops white fill to 88% on white elements.
 * Respects LocalReduceMotion by switching to opacity-only fade (120ms in, 100ms out).
 */
fun Modifier.playerPressable(
    enabled: Boolean = true,
    isWhiteElement: Boolean = false,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val actualSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) PlayerTokens.PressScaleFactor else 1f,
        animationSpec = if (isPressed) {
            tween(PlayerTokens.PressFeedbackInMs, easing = PlayerTokens.EaseOut)
        } else {
            tween(PlayerTokens.PressFeedbackOutMs, easing = PlayerTokens.EaseOut)
        },
        label = "playerPressScale"
    )

    val washAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) (if (isWhiteElement) 0.88f else 0.12f) else (if (isWhiteElement) 1f else 0f),
        animationSpec = if (isPressed) {
            tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else PlayerTokens.PressFeedbackInMs)
        } else {
            tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else PlayerTokens.PressFeedbackOutMs)
        },
        label = "playerPressWash"
    )

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = actualSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onLongClick = onLongClick,
            onClick = onClick
        )
    } else {
        Modifier.clickable(
            interactionSource = actualSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
    }

    this
        .scale(scale)
        .then(
            if (isWhiteElement) {
                Modifier.graphicsLayer { alpha = washAlpha }
            } else if (washAlpha > 0f) {
                Modifier.background(Color.White.copy(alpha = washAlpha))
            } else Modifier
        )
        .then(clickModifier)
}
