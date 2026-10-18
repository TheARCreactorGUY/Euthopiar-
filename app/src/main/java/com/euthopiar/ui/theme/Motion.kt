package com.euthopiar.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

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
    pressedScale: Float = 0.98f
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressedScale else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 100, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 150, easing = FastOutSlowInEasing)
        },
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
 * Standard pressable modifier for consumer-grade streaming UI (alpha 0.85 + scale 0.98, no wobble).
 * Exposes proper accessibility click semantics and Role.Button by default.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.98f,
    pressedAlpha: Float = 0.85f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) scaleDown else 1f,
        animationSpec = tween(Motion.DurationMicro, easing = Motion.StandardEasing),
        label = "pressScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) pressedAlpha else 1f,
        animationSpec = tween(Motion.DurationMicro, easing = Motion.StandardEasing),
        label = "pressAlpha"
    )

    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = actualInteractionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onLongClick = onLongClick,
            onClick = onClick
        )
    } else {
        Modifier.clickable(
            interactionSource = actualInteractionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
    }

    this
        .scale(scale)
        .alpha(alpha)
        .then(clickModifier)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Subtle spring physics dedicated specifically to the Primary Play button.
 * Exposes proper accessibility click semantics and Role.Button by default.
 */
fun Modifier.playButtonSpringPress(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) 0.98f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 100, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 150, easing = FastOutSlowInEasing)
        },
        label = "playSpring"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}
