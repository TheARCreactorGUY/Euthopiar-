package com.euthopiar.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Fluid Decelerated Easing Curves (Emil Kowalski / Apple Deceleration)
private val SharedDockEasing = CubicBezierEasing(0.12f, 0.98f, 0.24f, 1.0f)
private val BreathingEasing = CubicBezierEasing(0.40f, 0.0f, 0.60f, 1.0f)

/**
 * Wordmark-Centric App Ignition & Shared Coordinate Docking Unfurl.
 *
 * Implements:
 * - Emil Kowalski: Shared geometry docking, velocity continuity, zero artificial latency, zero recomposition.
 * - Leonxlnx Taste-Skill v2: Luxury of calm, typography dignity, elimination of strikethrough/neon gimmicks.
 * - Peter Bakaus Impeccable: Zero CLS, RenderThread hardware acceleration, display color science.
 */
@Composable
fun AppIgnitionSequence(
    onIgnitionFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current
    val density = LocalDensity.current

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarTopPx = with(density) { statusBarTopPadding.toPx() }
    val targetLeftX = with(density) { 16.dp.toPx() }
    val headerHeightPx = with(density) { 56.dp.toPx() }

    // Reduced motion fast path
    if (reduceMotion) {
        LaunchedEffect(Unit) {
            delay(120L)
            AppLaunchCoordinator.notifyIgnitionFinished()
            onIgnitionFinished()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isAmoled) Color.Black else Background)
        )
        return
    }

    // Animation clocks (driven on frame pulse without causing recomposition)
    val breathingClock = remember { Animatable(0f) }
    val dockClock = remember { Animatable(0f) }

    // Dynamic Early Release Orchestrator
    LaunchedEffect(Unit) {
        val breathingJob = launch {
            breathingClock.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 950, easing = BreathingEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Minimum fluid settling duration to establish visual anchor
        val minDelayJob = launch { delay(300L) }

        // Wait directly for Home catalog data to be 100% ready (safety ceiling of 8000ms for dead network)
        withTimeoutOrNull(8000L) {
            AppLaunchCoordinator.isHomeDataReady.first { it }
        }

        // Ensure minimum visual anchor presence
        minDelayJob.join()

        breathingJob.cancel()

        // Phase 2: Shared Coordinate Docking (Wordmark glides smoothly into TopHeaderBar)
        dockClock.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 440,
                easing = SharedDockEasing
            )
        )

        AppLaunchCoordinator.notifyIgnitionFinished()
        onIgnitionFinished()
    }

    var textWidthPx by remember { mutableFloatStateOf(0f) }
    var textHeightPx by remember { mutableFloatStateOf(0f) }

    val baseBg = if (isAmoled) Color.Black else Background

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Overlay fades out strictly during the second half of docking
                val dock = dockClock.value
                alpha = if (dock <= 0.3f) 1f else (1f - ((dock - 0.3f) / 0.7f)).coerceIn(0f, 1f)
            }
            .background(baseBg)
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = 220.dp.toPx()

                // Display Color Science: AMOLED non-linear cubic stops vs LCD velvet obsidian
                val amoledColors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color(0xFFEDEFEF).copy(alpha = 0.10f),
                    Color(0xFFCCD4E2).copy(alpha = 0.045f),
                    Color(0xFF1B222E).copy(alpha = 0.01f),
                    Color.Transparent
                )
                val lcdColors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color(0xFFB0C0D8).copy(alpha = 0.12f),
                    Color(0xFF2A3444).copy(alpha = 0.05f),
                    Color(0xFF12151B).copy(alpha = 0.012f),
                    Color.Transparent
                )
                val gradientColors = if (isAmoled) amoledColors else lcdColors

                onDrawBehind {
                    val breath = breathingClock.value
                    val dock = dockClock.value
                    val currentRadius = (baseRadius + 28.dp.toPx() * breath) * (1f - dock * 0.4f)
                    val currentAlpha = (1f - dock).coerceIn(0f, 1f)

                    if (currentAlpha > 0.01f) {
                        val brush = Brush.radialGradient(
                            colors = gradientColors,
                            center = center,
                            radius = currentRadius.coerceAtLeast(10f)
                        )
                        drawCircle(brush = brush, center = center, radius = currentRadius, alpha = currentAlpha)
                    }
                }
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // Shared Docking Wordmark ("EUTHOPIAR")
        // Starts centered with hero presence (1.35x scale) and glides smoothly into TopHeaderBar (1.0x scale)
        // 100% Hardware-accelerated inside graphicsLayer (Zero Recomposition during 60/120fps motion)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    val dock = dockClock.value
                    val breath = breathingClock.value

                    val startScale = 1.35f
                    val targetScale = 1.00f
                    val currentScale = (startScale + (0.03f * breath * (1f - dock))) + (targetScale - startScale) * dock

                    // Center coordinates when scaled by startScale
                    val startLeftX = (screenWidthPx - (textWidthPx * startScale)) / 2f
                    val startTopY = (screenHeightPx - textHeightPx) / 2f

                    // Target header coordinates
                    val targetTopY = statusBarTopPx + ((headerHeightPx - textHeightPx) / 2f).coerceAtLeast(0f)

                    val currentX = startLeftX + (targetLeftX - startLeftX) * dock
                    val currentY = startTopY + (targetTopY - startTopY) * dock

                    translationX = currentX
                    translationY = currentY
                    scaleX = currentScale
                    scaleY = currentScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .onSizeChanged { size ->
                    if (size.width > 0 && textWidthPx == 0f) {
                        textWidthPx = size.width.toFloat()
                        textHeightPx = size.height.toFloat()
                    }
                }
        ) {
            Text(
                text = "EUTHOPIAR",
                style = AppTypography.titleMedium.copy(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.20.em,
                    color = Color.White
                )
            )
        }
    }
}
