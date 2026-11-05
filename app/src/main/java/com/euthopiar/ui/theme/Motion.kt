package com.euthopiar.ui.theme

import android.os.SystemClock
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Standardized High-Fidelity Physics & Motion System for Euthopiar v1.0.8
 * Strict compliance with Apple Fluid Interfaces, Emil Kowalski standards, and Taste-Skill.
 */
object EuthopiarMotionTokens {
    // 1. Tactile Press Feedback (Immediate finger contact, subtle natural bounce on lift)
    const val PressScaleLarge = 0.96f
    const val PressScaleCompact = 0.94f
    val SpringPress = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = 520f
    )

    // 2. Spatial Navigation & Screen Pushes (Organic screen travel with exit parallax)
    val SpringSpatial = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )
    const val ExitParallaxFraction = 0.25f // Outgoing screen drifts by -it / 4

    // 3. Fluid Toggles & Stepper Snap (Apple iOS UISwitch standard)
    val SpringToggleKnob = spring<Dp>(
        dampingRatio = 0.70f,
        stiffness = 420f
    )

    // 4. Dialog, Sheet & Popover Emergence (Bloom from origin with subtle overshoot)
    val SpringDialogScale = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 450f
    )
    const val DialogInitialScale = 0.94f

    // 5. Expandable Content Accordions (Zero mechanical jerk at boundary)
    val SpringAccordion = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    // 6. Micro Interactions, Sheets & Snappy Sliders
    val SpringMicro = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 600f
    )
    val SpringSheet = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 450f
    )
    val SpringSnappy = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 500f
    )
}

object Motion {
    const val DurationMicro = 120
    const val DurationStandard = 220
    const val DurationEmphasized = 360
    const val DurationSharedElement = 500
    const val DurationHero = 500

    val StandardEasing = FastOutSlowInEasing
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
}

val AppleDecelCurve = CubicBezierEasing(0.12f, 0.98f, 0.24f, 1.0f)
val HudEnterEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
val HudExitEasing = CubicBezierEasing(0.40f, 0.0f, 1.0f, 1.0f)

/**
 * Tactile spring press scale with asymmetric physics (damped squeeze on press, lively overshoot on release).
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = EuthopiarMotionTokens.PressScaleLarge
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressedScale else 1f,
        animationSpec = if (reduceMotion) snap() else EuthopiarMotionTokens.SpringPress,
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

val LocalReduceMotion = compositionLocalOf { false }
val LocalAmoledMode = compositionLocalOf { true }

/**
 * Standard pressable modifier for consumer-grade streaming UI (recoil scale 0.96, alpha 1.0, tuned spring, haptics).
 * Exposes proper accessibility click semantics and Role.Button by default.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = EuthopiarMotionTokens.PressScaleLarge,
    pressedAlpha: Float = 1.0f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    debounceIntervalMs: Long = 300L,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) scaleDown else 1f,
        animationSpec = if (reduceMotion) snap() else EuthopiarMotionTokens.SpringPress,
        label = "pressScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) pressedAlpha else 1f,
        animationSpec = if (reduceMotion) snap() else EuthopiarMotionTokens.SpringPress,
        label = "pressAlpha"
    )

    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val lastClickTime = remember { longArrayOf(-100000L) }
    val debouncedOnClick = remember(onClick, debounceIntervalMs) {
        {
            val now = SystemClock.elapsedRealtime()
            if (debounceIntervalMs <= 0L || now - lastClickTime[0] >= debounceIntervalMs) {
                lastClickTime[0] = now
                onClick()
            }
        }
    }

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = actualInteractionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onLongClick = onLongClick,
            onClick = debouncedOnClick
        )
    } else {
        Modifier.clickable(
            interactionSource = actualInteractionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = debouncedOnClick
        )
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .then(clickModifier)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                } catch (_: Throwable) {}
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Tactile spring physics dedicated specifically to the Primary Play button.
 * Authentic 0.96f squeeze, release spring overshoot, graphicsLayer transform, and Confirm haptic.
 * Exposes proper accessibility click semantics and Role.Button by default.
 */
fun Modifier.playButtonSpringPress(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    debounceIntervalMs: Long = 300L,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) EuthopiarMotionTokens.PressScaleLarge else 1f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.78f, stiffness = 600f)
        } else {
            EuthopiarMotionTokens.SpringPress
        },
        label = "playSpring"
    )

    val lastClickTime = remember { longArrayOf(-100000L) }
    val debouncedOnClick = remember(onClick, debounceIntervalMs) {
        {
            val now = SystemClock.elapsedRealtime()
            if (debounceIntervalMs <= 0L || now - lastClickTime[0] >= debounceIntervalMs) {
                lastClickTime[0] = now
                onClick()
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = debouncedOnClick
        )
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                } catch (_: Throwable) {}
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}
