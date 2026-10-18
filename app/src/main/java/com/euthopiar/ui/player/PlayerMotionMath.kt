package com.euthopiar.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Pure mathematical formulas for:
 * 1. Seek Interval Multi-Tap Radial Wave & Chevrons (Section 3.2)
 * 2. Pinch-to-Zoom Gesture Hysteresis & Rubber-Banding (Section 3.3)
 * 3. Volume Slider Fluid Interpolation, Rate Limiting & Icon Morphing (Section 3.4)
 * 4. Settings Panel Responsive Layout & 3-Tier Segmented Control (Section 3.5)
 */
object PlayerMotionMath {

    // =========================================================================
    // 1. SEEK INTERVAL MULTI-TAP RADIAL WAVE & CHEVRONS (Section 3.2)
    // =========================================================================

    const val T_WAVE_MS = 550L
    const val T_FADE_MS = 220L
    const val HOLD_WINDOW_MS = 650L
    const val T_CHEVRON_CYCLE_S = 0.65f

    // Spring parameters for text scale pop
    const val TEXT_BOUNCE_K = 1800f
    const val TEXT_BOUNCE_ZETA = 0.52f
    const val TEXT_BOUNCE_OMEGA_N = 42.43f
    const val TEXT_BOUNCE_OMEGA_D = 36.26f // omega_n * sqrt(1 - zeta^2)

    /**
     * Compute maximum Euclidean distance from tap position to corners of half-screen domain.
     */
    fun computeRMax(tapX: Float, tapY: Float, width: Float, height: Float, isForward: Boolean): Float {
        val (xMin, xMax) = if (isForward) (width / 2f to width) else (0f to width / 2f)
        val corners = listOf(
            Offset(xMin, 0f),
            Offset(xMin, height),
            Offset(xMax, 0f),
            Offset(xMax, height)
        )
        return corners.maxOf { corner ->
            val dx = corner.x - tapX
            val dy = corner.y - tapY
            sqrt(dx * dx + dy * dy)
        }
    }

    fun computeRMin(width: Float, height: Float): Float {
        return 0.12f * minOf(width / 2f, height)
    }

    /**
     * Radial Expansion Function R(tau):
     * R(tau) = R_min + (R_max - R_min) * (1 - (1 - tau)^3.4)
     */
    fun radialExpansion(tau: Float, rMin: Float, rMax: Float): Float {
        val clampedTau = tau.coerceIn(0f, 1f)
        val easeOut = 1.0f - (1.0f - clampedTau).pow(3.4f)
        return rMin + (rMax - rMin) * easeOut
    }

    /**
     * Alpha Decay Function A(tau):
     * Piecewise curve: ramp-in (0..0.12), sustain (0.12..0.60), smooth falloff (0.60..1.0).
     */
    fun alphaDecay(tau: Float, aPeak: Float): Float {
        val clampedTau = tau.coerceIn(0f, 1f)
        val curve = when {
            clampedTau < 0.12f -> clampedTau / 0.12f
            clampedTau < 0.60f -> 1.0f
            else -> 1.0f - ((clampedTau - 0.60f) / 0.40f).pow(2.2f)
        }
        return (aPeak * curve).coerceIn(0f, 1f)
    }

    /**
     * Phase Momentum Re-Anchoring on consecutive tap n >= 2:
     * tau_new = max(0.35, tau_curr - 0.28 * sqrt(1 - tau_curr))
     */
    fun reanchorWaveProgress(tauCurr: Float): Float {
        val clamped = tauCurr.coerceIn(0f, 1f)
        val pullBack = 0.28f * sqrt(1.0f - clamped)
        return maxOf(0.35f, clamped - pullBack)
    }

    /**
     * Anchor Center Exponential Smoothing:
     * P_anchor(n) = 0.65 * P_anchor(n-1) + 0.35 * P_tap(n)
     */
    fun smoothAnchorCenter(prevAnchor: Offset, newTap: Offset): Offset {
        return Offset(
            x = 0.65f * prevAnchor.x + 0.35f * newTap.x,
            y = 0.65f * prevAnchor.y + 0.35f * newTap.y
        )
    }

    /**
     * Propagation speed scale factor:
     * 1 + 0.18 * ln(min(n, 8))
     */
    fun wavePropagationSpeedFactor(tapCount: Int): Float {
        val n = tapCount.coerceIn(1, 8)
        return 1.0f + 0.18f * ln(n.toFloat())
    }

    /**
     * Text Scale Bounce S(n, Delta t):
     * S_amp(n) = min(0.32, 0.18 + 0.035 * ln(n))
     * S(n, Delta t) = 1.0 + S_amp(n) * e^(-zeta * omega_n * Delta t) * sin(omega_d * Delta t + arccos(zeta))
     */
    fun textScaleBounce(tapCount: Int, deltaSeconds: Float): Float {
        val n = tapCount.coerceAtLeast(1)
        val sAmp = minOf(0.32f, 0.18f + 0.035f * ln(n.toFloat()))
        val decay = exp(-TEXT_BOUNCE_ZETA * TEXT_BOUNCE_OMEGA_N * deltaSeconds)
        val phi = kotlin.math.acos(TEXT_BOUNCE_ZETA)
        val osc = sin(TEXT_BOUNCE_OMEGA_D * deltaSeconds + phi)
        return 1.0f + sAmp * decay * osc
    }

    /**
     * Chevron Opacity Continuous Traveling Wave A_i(t):
     * A_i(t) = 0.25 + 0.75 * ((1 + cos(phi(t) - sigma * i * 2*pi/3)) / 2)^1.6
     */
    fun chevronOpacity(chevronIndex: Int, timeSeconds: Float, isForward: Boolean): Float {
        val sigma = if (isForward) 1.0f else -1.0f
        val period = T_CHEVRON_CYCLE_S
        val phi = ((2.0 * Math.PI * (timeSeconds % period)) / period).toFloat()
        val spatialOffset = sigma * chevronIndex.toFloat() * ((2.0 * Math.PI) / 3.0).toFloat()
        val cosKernel = (1.0f + cos(phi - spatialOffset)) / 2.0f
        val shaped = cosKernel.pow(1.6f)
        return 0.25f + 0.75f * shaped
    }

    /**
     * Chevron Micro-Translation:
     * Delta x_i(t) = sigma * 3.5dp * ((A_i(t) - 0.25) / 0.75)
     */
    fun chevronMicroTranslationDp(alpha: Float, isForward: Boolean): Float {
        val sigma = if (isForward) 1.0f else -1.0f
        val norm = ((alpha - 0.25f) / 0.75f).coerceIn(0f, 1f)
        return sigma * 3.5f * norm
    }

    // =========================================================================
    // 2. PINCH-TO-ZOOM GESTURE HYSTERESIS & RUBBER-BANDING (Section 3.3)
    // =========================================================================

    const val DELTA_S_MAX = 0.12f
    const val SPRING_PINCH_K = 650f
    const val SPRING_PINCH_ZETA = 0.92f

    const val BADGE_ENTER_K = 900f
    const val BADGE_ENTER_ZETA = 0.72f
    const val BADGE_DEBOUNCE_HOLD_MS = 1100L
    const val BADGE_EXIT_DURATION_MS = 180

    /**
     * Hyperbolic Tangent Rubber-Banded Video Surface Scale:
     * Outside [S_fit, S_fill], applies resistance with max elastic excursion Delta S_max = 0.12.
     */
    fun rubberBandedVideoScale(
        pinchRatio: Float,
        committedScale: Float,
        sFit: Float = 1.0f,
        sFill: Float = 1.33f
    ): Float {
        val sRaw = committedScale * pinchRatio
        val excursionDivisor = DELTA_S_MAX / 0.35f

        return when {
            sRaw < sFit -> {
                sFit - DELTA_S_MAX * tanh((sFit - sRaw) / excursionDivisor)
            }
            sRaw > sFill -> {
                sFill + DELTA_S_MAX * tanh((sRaw - sFill) / excursionDivisor)
            }
            else -> sRaw
        }
    }

    /**
     * Directional Asymmetric Hysteresis Commitment Rule:
     * If start is FIT: commits to FILL when lambda >= 0.35 or r >= 1.12.
     * If start is FILL: commits to FIT when lambda <= 0.65 or r <= 0.88.
     */
    fun evaluatePinchHysteresis(
        startMode: AspectRatioMode,
        surfaceScale: Float,
        pinchRatio: Float,
        sFit: Float = 1.0f,
        sFill: Float = 1.33f
    ): AspectRatioMode {
        val lambda = if (sFill > sFit) {
            ((surfaceScale - sFit) / (sFill - sFit)).coerceIn(0f, 1f)
        } else 0f

        return when (startMode) {
            AspectRatioMode.FIT -> {
                if (lambda >= 0.35f || pinchRatio >= 1.12f) AspectRatioMode.ZOOM else AspectRatioMode.FIT
            }
            AspectRatioMode.ZOOM -> {
                if (lambda <= 0.65f || pinchRatio <= 0.88f) AspectRatioMode.FIT else AspectRatioMode.ZOOM
            }
            else -> startMode
        }
    }

    // =========================================================================
    // 3. VOLUME SLIDER IPC RATE LIMITING & ICON MORPHING (Section 3.4)
    // =========================================================================

    const val DELTA_T_IPC_MS = 48L
    const val SPRING_VOLUME_K = 1200f
    const val SPRING_VOLUME_ZETA = 1.00f

    /**
     * Binder IPC Throttling Decision:
     * Caps Binder IPC throughput to <= 20.8 calls/sec while guaranteeing immediate flush
     * on boundary hits (0 or V_max) or gesture release.
     */
    fun shouldDispatchVolumeIpc(
        targetVolume: Int,
        lastSentVolume: Int,
        currentTimeMs: Long,
        lastIpcTimeMs: Long,
        maxVolume: Int,
        isGestureRelease: Boolean
    ): Boolean {
        if (targetVolume == lastSentVolume) return false
        if (isGestureRelease) return true
        if (targetVolume == 0 || targetVolume == maxVolume) return true
        return (currentTimeMs - lastIpcTimeMs) >= DELTA_T_IPC_MS
    }

    /**
     * Cubic Hermite Smoothstep: S(u) = 3u^2 - 2u^3
     */
    fun hermiteSmoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val u = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return u * u * (3.0f - 2.0f * u)
    }

    data class IconCrossfadeWeights(
        val alphaMute: Float,
        val scaleMute: Float,
        val alphaDown: Float,
        val scaleDown: Float,
        val alphaUp: Float,
        val scaleUp: Float,
        val tintProgress: Float
    )

    /**
     * Icon Morphing and Tint Transitions across fill fraction f in [0, 1]:
     * Transition 1 (Mute <-> Down): [0.00, 0.08]
     * Transition 2 (Down <-> Up): [0.42, 0.58]
     * Tint Window: [0.09, 0.19]
     */
    fun computeVolumeIconWeights(fillFraction: Float): IconCrossfadeWeights {
        val f = fillFraction.coerceIn(0f, 1f)
        val uA = hermiteSmoothstep(0.00f, 0.08f, f)
        val uB = hermiteSmoothstep(0.42f, 0.58f, f)

        val alphaMute = 1.0f - uA
        val scaleMute = 0.85f + 0.15f * (1.0f - uA)

        val alphaDown = uA * (1.0f - uB)
        val scaleDown = 0.85f + 0.15f * uA * (1.0f - uB)

        val alphaUp = uB
        val scaleUp = 0.85f + 0.15f * uB

        val uTint = hermiteSmoothstep(0.09f, 0.19f, f)

        return IconCrossfadeWeights(
            alphaMute = alphaMute,
            scaleMute = scaleMute,
            alphaDown = alphaDown,
            scaleDown = scaleDown,
            alphaUp = alphaUp,
            scaleUp = scaleUp,
            tintProgress = uTint
        )
    }

    // =========================================================================
    // 4. SETTINGS PANEL RESPONSIVE LAYOUT & OVERFLOW ELIMINATION (Section 3.5)
    // =========================================================================

    const val DRAWER_PADDING_DP = 16f
    const val SEGMENTED_CONTAINER_PADDING_DP = 4f // 2 * P_seg = 8dp total (4dp each side)
    const val BUTTON_GAP_DP = 4f

    enum class SegmentedControlTier {
        TIER_1_TWO_LINE_STACK,
        TIER_2_COMPACT_MICRO,
        TIER_3_NUMERIC_BADGE
    }

    /**
     * Orientation-aware piecewise clamped width equation: W_drawer(W_screen)
     */
    fun calculateDrawerWidth(screenWidthDp: Float): Float {
        return when {
            screenWidthDp < 600f -> {
                (0.84f * screenWidthDp).coerceIn(300f, 360f)
            }
            screenWidthDp <= 960f -> {
                (0.42f * screenWidthDp).coerceIn(340f, 420f)
            }
            else -> {
                (0.34f * screenWidthDp).coerceIn(380f, 440f)
            }
        }
    }

    /**
     * Computes individual button width W_btn inside segmented control:
     * W_avail = W_drawer - 2*P_drawer - 2*P_seg = W_drawer - 32dp - 8dp = W_drawer - 40dp
     * W_btn = (W_avail - (N - 1) * G_btn) / N
     * Guarantees W_btn >= 62dp for W_drawer >= 300dp.
     */
    fun calculateButtonWidth(drawerWidthDp: Float, numButtons: Int): Float {
        val totalPadding = 2f * DRAWER_PADDING_DP + 2f * SEGMENTED_CONTAINER_PADDING_DP // 40dp
        val availableWidth = (drawerWidthDp - totalPadding).coerceAtLeast(0f)
        val totalGaps = (numButtons - 1) * BUTTON_GAP_DP
        return ((availableWidth - totalGaps) / numButtons.coerceAtLeast(1)).coerceAtLeast(0f)
    }

    /**
     * Landscape Cutout-safe Horizontal Badge Center Shift (Section 3.3):
     * X_badge_center = (W_viewport + I_cutout,left - I_cutout,right) / 2
     * Shifts center from W_viewport / 2 by (I_cutout,left - I_cutout,right) / 2
     */
    fun calculateBadgeCenterHorizontalShift(cutoutLeftPx: Float, cutoutRightPx: Float): Float {
        return (cutoutLeftPx - cutoutRightPx) / 2f
    }

    /**
     * Evaluates Effective Button Capacity Ratio W_eff = W_btn / fontScale
     * and maps to deterministic 3-tier layout mode.
     */
    fun determineSegmentedControlTier(buttonWidthDp: Float, fontScale: Float): SegmentedControlTier {
        val safeFontScale = fontScale.coerceIn(0.85f, 2.0f)
        val wEff = buttonWidthDp / safeFontScale

        return when {
            wEff >= 72f -> SegmentedControlTier.TIER_1_TWO_LINE_STACK
            wEff >= 52f -> SegmentedControlTier.TIER_2_COMPACT_MICRO
            else -> SegmentedControlTier.TIER_3_NUMERIC_BADGE
        }
    }
}
