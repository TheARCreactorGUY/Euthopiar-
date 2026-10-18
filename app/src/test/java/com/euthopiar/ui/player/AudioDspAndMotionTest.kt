package com.euthopiar.ui.player

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioDspAndMotionTest {

    private val EPSILON = 0.001f

    // =========================================================================
    // 1. AUDIO DSP GAIN STAGING & HEADROOM TESTS
    // =========================================================================

    @Test
    fun testPreEqFilterProfiles() {
        // Off profile: all 0 dB
        val offGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.OFF)
        assertEquals(6, offGains.size)
        assertTrue(offGains.all { it == 0.0f })

        // Low profile
        val lowGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.LOW)
        assertEquals(6, lowGains.size)
        assertEquals(-6.0f, lowGains[0], EPSILON)
        assertEquals(3.0f, lowGains[3], EPSILON) // 3500 Hz speech resonance peak

        // High profile: +8 dB speech formant boost, -12 dB bass reduction
        val highGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.HIGH)
        assertEquals(6, highGains.size)
        assertEquals(-12.0f, highGains[0], EPSILON)
        assertEquals(8.0f, highGains[3], EPSILON)
        assertEquals(-5.0f, highGains[5], EPSILON)
    }

    @Test
    fun testHeadroomAllocationMbcDisabled() {
        val offGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.OFF)
        val highGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.HIGH)

        // With Dialogue OFF (maxPreEq = 0):
        // maxAllowed = T_lim (-1.5) + D_max (6.0) - alpha_crest (0.65) * 0 = 4.5 dB
        val gain0BoostOff = AudioDspMath.calculateInputGainMbcDisabled(0.0f, offGains)
        assertEquals(0.0f, gain0BoostOff, EPSILON)

        val gain12BoostOff = AudioDspMath.calculateInputGainMbcDisabled(12.0f, offGains)
        assertEquals(4.5f, gain12BoostOff, EPSILON) // Capped at headroom ceiling 4.5 dB

        // With Dialogue HIGH (maxPreEq = 8.0 dB at band 3):
        // maxAllowed = -1.5 + 6.0 - 0.65 * 8.0 = 4.5 - 5.2 = -0.7 dB
        val gain0BoostHigh = AudioDspMath.calculateInputGainMbcDisabled(0.0f, highGains)
        assertEquals(-0.7f, gain0BoostHigh, EPSILON) // Compelled attenuation for safety

        val gain12BoostHigh = AudioDspMath.calculateInputGainMbcDisabled(12.0f, highGains)
        assertEquals(-0.7f, gain12BoostHigh, EPSILON) // Headroom clamped at -0.7 dB
    }

    @Test
    fun testMbcDynamicMakeupGainAllocation() {
        val highGains = AudioDspMath.getPreEqGains(DialogueBoostLevel.HIGH)

        // Band 0 (20-220Hz): maxPreEq = 0 (since band 0 gain is -12 dB, coerced to 0)
        // L_max_comp = -26 + (0 - (-26))/6 = -26 + 4.333 = -21.667 dBFS
        // H_avail = -1.5 - (-21.667) = +20.167 dB
        // Base post gain = 1.5 dB, beta = 0.35
        val postGains0Boost = AudioDspMath.calculateMbcPostGains(0.0f, highGains)
        assertEquals(1.5f, postGains0Boost[0], EPSILON)

        // Band 1 (220-4500Hz): PreEQ bands 1,2,3 -> max is 8.0 dB
        // L_max_comp = -30 + (0 + 8.0 - (-30))/3.2 = -30 + 38/3.2 = -30 + 11.875 = -18.125 dBFS
        // H_avail = -1.5 - (-18.125) = +16.625 dB
        // Base post gain = 7.5 dB, beta = 1.00
        assertEquals(7.5f, postGains0Boost[1], EPSILON)

        // Test with 12 dB Volume Boost:
        // Band 1 demand: 7.5 + 1.0 * 12.0 = 19.5 dB, but H_avail is 16.625 dB -> clamped to 16.625 dB!
        val postGains12Boost = AudioDspMath.calculateMbcPostGains(12.0f, highGains)
        assertTrue("MBC Band 1 must be clamped to H_avail to prevent limiter clipping", postGains12Boost[1] <= 16.626f)
        assertEquals(16.625f, postGains12Boost[1], 0.01f)

        // Band 0 demand: 1.5 + 0.35 * 12.0 = 5.7 dB, H_avail is 20.167 dB -> 5.7 dB fits within headroom
        assertEquals(5.7f, postGains12Boost[0], 0.01f)
    }

    @Test
    fun testLegacyEqualizerDownwardNormalization() {
        val highGains = AudioDspMath.getLegacyEqGains(DialogueBoostLevel.HIGH)
        // High profile: [-11, -4.5, 5, 7.5, -4.5] -> max is 7.5 dB
        val legacyGains = AudioDspMath.calculateLegacyGains(0.0f, highGains)

        // Check normalization: all bands shifted downward by max (7.5 dB)
        // Band 3 was 7.5 -> becomes 0 mB
        // Band 0 was -11.0 -> becomes (-11.0 - 7.5) * 100 = -1850 mB
        assertEquals(0.toShort(), legacyGains.normalizedBandGainsMb[3])
        assertEquals((-1850).toShort(), legacyGains.normalizedBandGainsMb[0])
        assertTrue("Normalized EQ bands must not exceed 0 mB", legacyGains.normalizedBandGainsMb.all { it <= 0 })

        // LoudnessEnhancer target: min(14.0, gBoost + 0.75 * gEqMax)
        // With 0 dB boost: 0 + 0.75 * 7.5 = 5.625 dB -> 563 mB (or 562 mB rounded)
        assertEquals(563, legacyGains.loudnessEnhancerMb)

        // With 12 dB boost: 12 + 5.625 = 17.625 dB, clamped to 14.0 dB max (1400 mB)
        val legacyGainsBoosted = AudioDspMath.calculateLegacyGains(12.0f, highGains)
        assertEquals(1400, legacyGainsBoosted.loudnessEnhancerMb)
    }

    // =========================================================================
    // 2. SEEK INTERVAL RADIAL EXPANSION & MOTION PHYSICS TESTS
    // =========================================================================

    @Test
    fun testRadialExpansionCurve() {
        val rMin = 50f
        val rMax = 400f

        // Boundaries
        assertEquals(rMin, PlayerMotionMath.radialExpansion(0.0f, rMin, rMax), EPSILON)
        assertEquals(rMax, PlayerMotionMath.radialExpansion(1.0f, rMin, rMax), EPSILON)

        // Monotonic ease-out progression
        var prevR = rMin
        for (i in 1..10) {
            val tau = i / 10f
            val r = PlayerMotionMath.radialExpansion(tau, rMin, rMax)
            assertTrue("Expansion must be strictly monotonic", r > prevR)
            prevR = r
        }
    }

    @Test
    fun testAlphaDecayCurve() {
        val aPeak = 0.85f

        // Initial ramp-in
        assertEquals(0.0f, PlayerMotionMath.alphaDecay(0.0f, aPeak), EPSILON)
        assertEquals(aPeak, PlayerMotionMath.alphaDecay(0.12f, aPeak), EPSILON)

        // Sustain window [0.12, 0.60]
        assertEquals(aPeak, PlayerMotionMath.alphaDecay(0.35f, aPeak), EPSILON)
        assertEquals(aPeak, PlayerMotionMath.alphaDecay(0.60f, aPeak), EPSILON)

        // Exit smooth falloff
        assertTrue(PlayerMotionMath.alphaDecay(0.80f, aPeak) < aPeak)
        assertEquals(0.0f, PlayerMotionMath.alphaDecay(1.0f, aPeak), EPSILON)
    }

    @Test
    fun testWaveProgressReanchoring() {
        // If current progress is 0.80, re-anchoring pulls back
        val reanchored = PlayerMotionMath.reanchorWaveProgress(0.80f)
        val expected = maxOf(0.35f, 0.80f - 0.28f * kotlin.math.sqrt(1.0f - 0.80f))
        assertEquals(expected, reanchored, EPSILON)

        // Minimum floor of 0.35 is respected
        val nearComplete = PlayerMotionMath.reanchorWaveProgress(0.99f)
        assertTrue(nearComplete >= 0.35f)
    }

    @Test
    fun testAnchorCenterSmoothing() {
        val pAnchor = Offset(100f, 200f)
        val pTap = Offset(150f, 250f)
        val smoothed = PlayerMotionMath.smoothAnchorCenter(pAnchor, pTap)

        // 0.65 * 100 + 0.35 * 150 = 65 + 52.5 = 117.5
        // 0.65 * 200 + 0.35 * 250 = 130 + 87.5 = 217.5
        assertEquals(117.5f, smoothed.x, EPSILON)
        assertEquals(217.5f, smoothed.y, EPSILON)
    }

    @Test
    fun testTextScaleBounceSpring() {
        // At deltaT = 0, scale bounce is 1.0 + sAmp * sin(phi)
        val scale0 = PlayerMotionMath.textScaleBounce(2, 0.0f)
        assertTrue("Scale bounce must start above 1.0 on tap impulse", scale0 > 1.0f)

        // As time passes (e.g. 1.0 second), underdamped spring decays toward 1.0
        val scaleLate = PlayerMotionMath.textScaleBounce(2, 1.0f)
        assertEquals(1.0f, scaleLate, 0.05f)
    }

    // =========================================================================
    // 3. PINCH-TO-ZOOM RUBBER-BANDING & HYSTERESIS TESTS
    // =========================================================================

    @Test
    fun testPinchRubberBandingWithinBounds() {
        // Inside [1.0, 1.33], scale is 1:1 linear
        val sInside = PlayerMotionMath.rubberBandedVideoScale(
            pinchRatio = 1.15f,
            committedScale = 1.0f,
            sFit = 1.0f,
            sFill = 1.33f
        )
        assertEquals(1.15f, sInside, EPSILON)
    }

    @Test
    fun testPinchRubberBandingOutsideBounds() {
        val sFit = 1.0f
        val sFill = 1.33f

        // Pinching below FIT: resistance applied, excursion strictly bounded by DELTA_S_MAX (0.12)
        val sBelow = PlayerMotionMath.rubberBandedVideoScale(
            pinchRatio = 0.50f,
            committedScale = 1.0f,
            sFit = sFit,
            sFill = sFill
        )
        assertTrue(sBelow < sFit)
        assertTrue("Scale must not exceed Delta S max excursion", sBelow >= sFit - PlayerMotionMath.DELTA_S_MAX)

        // Pinching above FILL: resistance applied, excursion strictly bounded by DELTA_S_MAX (0.12)
        val sAbove = PlayerMotionMath.rubberBandedVideoScale(
            pinchRatio = 2.0f,
            committedScale = 1.0f,
            sFit = sFit,
            sFill = sFill
        )
        assertTrue(sAbove > sFill)
        assertTrue("Scale must not exceed Delta S max excursion", sAbove <= sFill + PlayerMotionMath.DELTA_S_MAX)
    }

    @Test
    fun testPinchHysteresisDirectionalAsymmetry() {
        val sFit = 1.0f
        val sFill = 1.33f

        // Case A: Starting in FIT mode
        // Small zoom (lambda = 0.20, pinch = 1.05) -> remains FIT
        val decisionFitSmall = PlayerMotionMath.evaluatePinchHysteresis(
            startMode = AspectRatioMode.FIT,
            surfaceScale = 1.066f, // lambda = 0.20
            pinchRatio = 1.05f,
            sFit = sFit,
            sFill = sFill
        )
        assertEquals(AspectRatioMode.FIT, decisionFitSmall)

        // Strong zoom (lambda = 0.40 >= 0.35) -> commits to ZOOM
        val decisionFitZoom = PlayerMotionMath.evaluatePinchHysteresis(
            startMode = AspectRatioMode.FIT,
            surfaceScale = 1.132f, // lambda = 0.40
            pinchRatio = 1.13f,
            sFit = sFit,
            sFill = sFill
        )
        assertEquals(AspectRatioMode.ZOOM, decisionFitZoom)

        // Case B: Starting in ZOOM mode
        // Small shrink (lambda = 0.80, pinch = 0.95) -> remains ZOOM
        val decisionZoomSmall = PlayerMotionMath.evaluatePinchHysteresis(
            startMode = AspectRatioMode.ZOOM,
            surfaceScale = 1.264f, // lambda = 0.80
            pinchRatio = 0.95f,
            sFit = sFit,
            sFill = sFill
        )
        assertEquals(AspectRatioMode.ZOOM, decisionZoomSmall)

        // Strong shrink (lambda = 0.50 <= 0.65) -> commits to FIT
        val decisionZoomFit = PlayerMotionMath.evaluatePinchHysteresis(
            startMode = AspectRatioMode.ZOOM,
            surfaceScale = 1.165f, // lambda = 0.50
            pinchRatio = 0.85f,
            sFit = sFit,
            sFill = sFill
        )
        assertEquals(AspectRatioMode.FIT, decisionZoomFit)
    }

    // =========================================================================
    // 4. VOLUME IPC RATE LIMITING & ICON MORPHING TESTS
    // =========================================================================

    @Test
    fun testVolumeIpcRateLimiterDecision() {
        val maxVol = 15

        // Same volume -> never dispatch
        assertFalse(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 5,
                lastSentVolume = 5,
                currentTimeMs = 1000L,
                lastIpcTimeMs = 900L,
                maxVolume = maxVol,
                isGestureRelease = false
            )
        )

        // Intermediate change throttled if < 48ms
        assertFalse(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 6,
                lastSentVolume = 5,
                currentTimeMs = 1030L,
                lastIpcTimeMs = 1000L, // 30ms elapsed (< 48ms)
                maxVolume = maxVol,
                isGestureRelease = false
            )
        )

        // Intermediate change permitted if >= 48ms
        assertTrue(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 6,
                lastSentVolume = 5,
                currentTimeMs = 1050L,
                lastIpcTimeMs = 1000L, // 50ms elapsed (>= 48ms)
                maxVolume = maxVol,
                isGestureRelease = false
            )
        )

        // Boundary 0 (mute) flushed IMMEDIATELY even if < 48ms
        assertTrue(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 0,
                lastSentVolume = 1,
                currentTimeMs = 1010L,
                lastIpcTimeMs = 1000L, // only 10ms elapsed
                maxVolume = maxVol,
                isGestureRelease = false
            )
        )

        // Boundary maxVolume flushed IMMEDIATELY even if < 48ms
        assertTrue(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 15,
                lastSentVolume = 14,
                currentTimeMs = 1010L,
                lastIpcTimeMs = 1000L, // only 10ms elapsed
                maxVolume = maxVol,
                isGestureRelease = false
            )
        )

        // Gesture release flushed IMMEDIATELY even if < 48ms
        assertTrue(
            PlayerMotionMath.shouldDispatchVolumeIpc(
                targetVolume = 8,
                lastSentVolume = 7,
                currentTimeMs = 1005L,
                lastIpcTimeMs = 1000L, // only 5ms elapsed
                maxVolume = maxVol,
                isGestureRelease = true
            )
        )
    }

    @Test
    fun testVolumeIconCrossfadeAndTint() {
        // At 0% (mute): mute icon fully visible
        val weights0 = PlayerMotionMath.computeVolumeIconWeights(0.0f)
        assertEquals(1.0f, weights0.alphaMute, EPSILON)
        assertEquals(0.0f, weights0.alphaDown, EPSILON)
        assertEquals(0.0f, weights0.alphaUp, EPSILON)
        assertEquals(0.0f, weights0.tintProgress, EPSILON)

        // At 25% (low volume): volume down icon active, tint complete
        val weights25 = PlayerMotionMath.computeVolumeIconWeights(0.25f)
        assertEquals(0.0f, weights25.alphaMute, EPSILON)
        assertEquals(1.0f, weights25.alphaDown, EPSILON)
        assertEquals(0.0f, weights25.alphaUp, EPSILON)
        assertEquals(1.0f, weights25.tintProgress, EPSILON)

        // At 100% (high volume): volume up icon active
        val weights100 = PlayerMotionMath.computeVolumeIconWeights(1.0f)
        assertEquals(0.0f, weights100.alphaMute, EPSILON)
        assertEquals(0.0f, weights100.alphaDown, EPSILON)
        assertEquals(1.0f, weights100.alphaUp, EPSILON)
        assertEquals(1.0f, weights100.tintProgress, EPSILON)
    }

    // =========================================================================
    // 5. RESPONSIVE DRAWER & SEGMENTED CONTROL TESTS
    // =========================================================================

    @Test
    fun testDrawerPiecewiseClamping() {
        // Phone portrait (< 600dp)
        // 360dp screen * 0.84 = 302.4dp -> inside [300, 360]
        val phoneDrawer = PlayerMotionMath.calculateDrawerWidth(360f)
        assertEquals(302.4f, phoneDrawer, EPSILON)

        // Very small screen: clamped to 300dp
        val smallPhoneDrawer = PlayerMotionMath.calculateDrawerWidth(320f)
        assertEquals(300.0f, smallPhoneDrawer, EPSILON)

        // Tablet portrait (600..960dp)
        // 800dp screen * 0.42 = 336dp -> clamped to min 340dp
        val tabletDrawer = PlayerMotionMath.calculateDrawerWidth(800f)
        assertEquals(340.0f, tabletDrawer, EPSILON)

        // Landscape (> 960dp)
        // 1200dp screen * 0.34 = 408dp -> inside [380, 440]
        val landscapeDrawer = PlayerMotionMath.calculateDrawerWidth(1200f)
        assertEquals(408.0f, landscapeDrawer, EPSILON)
    }

    @Test
    fun testSegmentedControlTierResolution() {
        // Minimum Drawer width 300dp with 4 buttons:
        // W_avail = 300 - 40 = 260dp
        // Total gaps = 3 * 4 = 12dp
        // W_btn = (260 - 12) / 4 = 62dp (Guaranteeing W_btn >= 62dp per Section 3.5)
        val minBtnWidth = PlayerMotionMath.calculateButtonWidth(300f, 4)
        assertEquals(62.0f, minBtnWidth, EPSILON)

        // Drawer width 360dp with 4 buttons:
        // W_avail = 360 - 40 = 320dp
        // Total gaps = 3 * 4 = 12dp
        // W_btn = (320 - 12) / 4 = 77dp
        val btnWidth = PlayerMotionMath.calculateButtonWidth(360f, 4)
        assertEquals(77.0f, btnWidth, EPSILON)

        // At standard 1.0 font scale: W_eff = 77 >= 72 -> Tier 1 (Two-line stack)
        assertEquals(
            PlayerMotionMath.SegmentedControlTier.TIER_1_TWO_LINE_STACK,
            PlayerMotionMath.determineSegmentedControlTier(btnWidth, 1.0f)
        )

        // At 1.25x accessibility font scale: W_eff = 77 / 1.25 = 61.6dp -> Tier 2 (Compact micro)
        assertEquals(
            PlayerMotionMath.SegmentedControlTier.TIER_2_COMPACT_MICRO,
            PlayerMotionMath.determineSegmentedControlTier(btnWidth, 1.25f)
        )

        // At 1.6x large font scale: W_eff = 77 / 1.6 = 48.125dp (< 52) -> Tier 3 (Numeric badge)
        assertEquals(
            PlayerMotionMath.SegmentedControlTier.TIER_3_NUMERIC_BADGE,
            PlayerMotionMath.determineSegmentedControlTier(btnWidth, 1.6f)
        )
    }

    @Test
    fun testUltraNarrowScreenExtremeAccessibilityFontScale() {
        // Ultra-narrow 280dp phone screen with 2.0x system font scale
        val drawerWidth = PlayerMotionMath.calculateDrawerWidth(280f)
        assertEquals(300.0f, drawerWidth, EPSILON) // Clamped to min 300dp

        val btnWidth = PlayerMotionMath.calculateButtonWidth(drawerWidth, 4)
        assertEquals(62.0f, btnWidth, EPSILON)

        // At extreme font scale 2.0: W_eff = 62 / 2.0 = 31dp (< 52) -> Tier 3 (Numeric badge)
        val tier = PlayerMotionMath.determineSegmentedControlTier(btnWidth, 2.0f)
        assertEquals(PlayerMotionMath.SegmentedControlTier.TIER_3_NUMERIC_BADGE, tier)
    }

    @Test
    fun testChevronOpacityAndMicroTranslationBounds() {
        // Section 3.2: Opacity A_i(t) in [0.25, 1.0] for all chevrons and cycle times
        for (chevronIdx in 0..2) {
            for (step in 0..65) {
                val t = step / 100f
                val forwardAlpha = PlayerMotionMath.chevronOpacity(chevronIdx, t, isForward = true)
                assertTrue("Forward chevron opacity must be >= 0.25", forwardAlpha >= 0.25f - EPSILON)
                assertTrue("Forward chevron opacity must be <= 1.0", forwardAlpha <= 1.0f + EPSILON)

                val backwardAlpha = PlayerMotionMath.chevronOpacity(chevronIdx, t, isForward = false)
                assertTrue("Backward chevron opacity must be >= 0.25", backwardAlpha >= 0.25f - EPSILON)
                assertTrue("Backward chevron opacity must be <= 1.0", backwardAlpha <= 1.0f + EPSILON)

                // Micro-translation must strictly be in [-3.5, 3.5] dp
                val forwardX = PlayerMotionMath.chevronMicroTranslationDp(forwardAlpha, isForward = true)
                assertTrue("Forward micro-translation must be >= 0", forwardX >= -EPSILON)
                assertTrue("Forward micro-translation must be <= 3.5dp", forwardX <= 3.5f + EPSILON)

                val backwardX = PlayerMotionMath.chevronMicroTranslationDp(backwardAlpha, isForward = false)
                assertTrue("Backward micro-translation must be <= 0", backwardX <= EPSILON)
                assertTrue("Backward micro-translation must be >= -3.5dp", backwardX >= -3.5f - EPSILON)
            }
        }
    }

    @Test
    fun testCutoutSafeBadgeCenterShift() {
        // Asymmetric camera cutout on left (48px) and no cutout on right (0px)
        val asymmetricShift = PlayerMotionMath.calculateBadgeCenterHorizontalShift(cutoutLeftPx = 48f, cutoutRightPx = 0f)
        assertEquals(24.0f, asymmetricShift, EPSILON)

        // Symmetric cutouts (48px both sides)
        val symmetricShift = PlayerMotionMath.calculateBadgeCenterHorizontalShift(cutoutLeftPx = 48f, cutoutRightPx = 48f)
        assertEquals(0.0f, symmetricShift, EPSILON)
    }
}
