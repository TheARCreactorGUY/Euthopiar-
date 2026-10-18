package com.euthopiar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val HERO_STRETCH_MAX = 180.dp
private const val HERO_STRETCH_DRAG_RESISTANCE = 0.65f
private const val HERO_STRETCH_RELEASE_ABSORB = 0.35f
private const val HERO_STRETCH_FRAME_TO_VELOCITY = 60f

private val HeroStretchReleaseSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 320f,
)
private val HeroStretchBounceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 110f,
)

/**
 * High-performance elastic overscroll controller for the Home screen hero carousel.
 * Captures pull-down overscroll gestures at the top of the feed, stretching the hero poster
 * with organic logarithmic drag resistance, compressing content subtly, and cleanly triggering
 * catalogue refresh when pulled past the spring threshold.
 */
@Stable
class HeroStretchState internal constructor(
    private val scope: CoroutineScope,
    val maxStretchPx: Float,
    private val hapticFeedback: HapticFeedback? = null,
    private val isAtTop: () -> Boolean,
    private val onRefresh: (() -> Unit)? = null,
) {
    private val stretchAnim = Animatable(0f)
    private var settleJob: Job? = null
    private var flingAbsorbed = false
    private var hasTriggeredThresholdHaptic = false

    val stretchPx: Float get() = stretchAnim.value
    val stretchFraction: Float get() = (stretchAnim.value / maxStretchPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val refreshThresholdPx: Float = maxStretchPx * 0.55f
    val isThresholdReached: Boolean get() = onRefresh != null && stretchAnim.value >= refreshThresholdPx

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) {
                flingAbsorbed = false
            }
            val stretch = stretchAnim.value
            // If scrolling up while the hero is stretched, consume the scroll to collapse the stretch first
            if (available.y < 0f && stretch > 0f) {
                val consumedY = available.y.coerceAtLeast(-stretch)
                snapTo(stretch + consumedY)
                if (stretch + consumedY < refreshThresholdPx) {
                    hasTriggeredThresholdHaptic = false
                }
                return Offset(0f, consumedY)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (available.y <= 0f || !isAtTop()) return Offset.Zero

            if (source == NestedScrollSource.UserInput) {
                val stretch = stretchAnim.value
                val fraction = (stretch / maxStretchPx).coerceIn(0f, 1f)
                val progressiveFactor = (1f - fraction).pow(1.75f)
                val resistance = HERO_STRETCH_DRAG_RESISTANCE * progressiveFactor
                val newStretch = (stretch + available.y * resistance).coerceIn(0f, maxStretchPx)
                snapTo(newStretch)

                if (onRefresh != null && newStretch >= refreshThresholdPx && !hasTriggeredThresholdHaptic) {
                    hasTriggeredThresholdHaptic = true
                    hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
                } else if (newStretch < refreshThresholdPx) {
                    hasTriggeredThresholdHaptic = false
                }
            } else if (!flingAbsorbed) {
                flingAbsorbed = true
                if (settleJob?.isActive != true) {
                    settle(available.y * HERO_STRETCH_FRAME_TO_VELOCITY, HeroStretchBounceSpring)
                }
            }
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (stretchAnim.value <= 0.5f) return Velocity.Zero
            val shouldRefresh = onRefresh != null && stretchAnim.value >= refreshThresholdPx
            if (shouldRefresh) {
                onRefresh.invoke()
            }
            hasTriggeredThresholdHaptic = false
            if (settleJob?.isActive != true) {
                settle(available.y * HERO_STRETCH_RELEASE_ABSORB, HeroStretchReleaseSpring)
            }
            return Velocity(0f, available.y)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            flingAbsorbed = false
            if (stretchAnim.value > 0.5f) {
                val shouldRefresh = onRefresh != null && stretchAnim.value >= refreshThresholdPx
                if (shouldRefresh) {
                    onRefresh.invoke()
                }
                hasTriggeredThresholdHaptic = false
                if (settleJob?.isActive != true) {
                    settle(0f, HeroStretchReleaseSpring)
                }
            }
            return if (available.y > 0f && isAtTop()) {
                Velocity(0f, available.y)
            } else {
                Velocity.Zero
            }
        }
    }

    private fun snapTo(value: Float) {
        scope.launch { stretchAnim.snapTo(value.coerceIn(0f, maxStretchPx)) }
    }

    private fun settle(initialVelocity: Float, spec: SpringSpec<Float>) {
        settleJob = scope.launch {
            stretchAnim.animateTo(
                targetValue = 0f,
                animationSpec = spec,
                initialVelocity = initialVelocity,
            )
        }
    }
}

@Composable
fun rememberHeroStretchState(
    listState: LazyListState,
    maxStretch: Dp = HERO_STRETCH_MAX,
    onRefresh: (() -> Unit)? = null,
): HeroStretchState {
    val scope = rememberCoroutineScope()
    val maxStretchPx = with(LocalDensity.current) { maxStretch.toPx() }
    val hapticFeedback = LocalHapticFeedback.current
    return remember(listState, maxStretchPx, onRefresh) {
        HeroStretchState(
            scope = scope,
            maxStretchPx = maxStretchPx,
            hapticFeedback = hapticFeedback,
            isAtTop = { !listState.canScrollBackward },
            onRefresh = onRefresh,
        )
    }
}

/**
 * Expands the measurable height of the Hero carousel container dynamically with stretch delta.
 */
fun Modifier.heroStretchHeight(baseHeight: Dp, stretchPx: () -> Float): Modifier =
    layout { measurable, constraints ->
        val height = baseHeight.roundToPx() + stretchPx().coerceAtLeast(0f).roundToInt()
        val placeable = measurable.measure(
            constraints.copy(minHeight = height, maxHeight = height),
        )
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

/**
 * Expands the artwork zoom from the top center anchor, matching natural spring elasticity.
 */
fun Modifier.heroStretchZoom(stretchPx: () -> Float): Modifier = graphicsLayer {
    val zoom = 1f + stretchPx().coerceAtLeast(0f) / size.height.coerceAtLeast(1f)
    transformOrigin = TransformOrigin(0.5f, 0f)
    scaleX = zoom
    scaleY = zoom
}

/**
 * Subtly compresses and scales down the hero content (logo & metadata) towards the bottom-left anchor
 * as the user stretches the canvas down, providing physical rubber-band feedback.
 */
fun Modifier.heroStretchContentCompress(stretchPx: () -> Float, maxStretchPx: Float): Modifier = graphicsLayer {
    val fraction = (stretchPx() / maxStretchPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val scale = 1f - (fraction * 0.10f)
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(0f, 1f)
}
