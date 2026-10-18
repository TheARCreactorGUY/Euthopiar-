package com.euthopiar.ui.player

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import kotlin.math.roundToInt

/**
 * Pure mathematical parameters and calculations for Audio DSP Gain-Staging,
 * Headroom Allocation, Dialogue Clarity, and Multi-band Dynamic Range Compression.
 * Implements Section 1 and Section 3.1 of DSP Specification.
 */
object AudioDspMath {

    const val T_LIM = -1.5f       // Safety limiter threshold (dBFS)
    const val D_MAX = 6.0f        // Max allowed limiter drive (dB)
    const val ALPHA_CREST = 0.65f // Spectral crest weighting factor
    val BETA = floatArrayOf(0.35f, 1.00f, 0.50f) // MBC band boost weighting

    // 6-band HAL Upper Cutoff Frequencies for PreEQ (Hz)
    val PRE_EQ_CUTOFFS_HZ = floatArrayOf(110.0f, 360.0f, 1450.0f, 3500.0f, 6200.0f, 20000.0f)

    // PreEQ Gains for Dialogue Clarity (dB)
    val PRE_EQ_GAINS_OFF = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    val PRE_EQ_GAINS_LOW = floatArrayOf(-6.0f, -1.5f, 1.5f, 3.0f, 1.5f, -1.5f)
    val PRE_EQ_GAINS_MED = floatArrayOf(-9.0f, -3.0f, 3.0f, 5.5f, 2.5f, -3.0f)
    val PRE_EQ_GAINS_HIGH = floatArrayOf(-12.0f, -4.5f, 4.5f, 8.0f, 3.5f, -5.0f)

    // 3-band Multi-Band Compressor Parameters
    val MBC_CUTOFFS_HZ = floatArrayOf(220.0f, 4500.0f, 20000.0f)
    val MBC_THRESHOLDS_DBFS = floatArrayOf(-26.0f, -30.0f, -24.0f)
    val MBC_RATIOS = floatArrayOf(6.0f, 3.2f, 4.5f)
    val MBC_KNEE_WIDTHS_DB = floatArrayOf(8.0f, 12.0f, 6.0f)
    val MBC_ATTACK_TIMES_MS = floatArrayOf(12.0f, 25.0f, 2.5f)
    val MBC_RELEASE_TIMES_MS = floatArrayOf(280.0f, 400.0f, 140.0f)
    val MBC_GATE_THRESHOLDS_DBFS = floatArrayOf(-60.0f, -52.0f, -56.0f)
    val MBC_EXPANDER_RATIOS = floatArrayOf(1.5f, 2.0f, 2.5f)
    val MBC_PRE_GAINS_DB = floatArrayOf(0.0f, 0.0f, 0.0f)
    val MBC_BASE_POST_GAINS_DB = floatArrayOf(1.5f, 7.5f, 2.0f)

    // Limiter Parameters
    const val LIMITER_ATTACK_MS = 1.0f
    const val LIMITER_RELEASE_MS = 60.0f
    const val LIMITER_RATIO = 20.0f
    const val LIMITER_POST_GAIN_DB = 0.0f

    // Legacy Equalizer (API < 28) 5-band Gains (dB)
    val LEGACY_EQ_GAINS_OFF = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    val LEGACY_EQ_GAINS_LOW = floatArrayOf(-5.0f, -1.5f, 2.0f, 3.0f, -1.5f)
    val LEGACY_EQ_GAINS_MED = floatArrayOf(-8.0f, -3.0f, 3.5f, 5.5f, -3.0f)
    val LEGACY_EQ_GAINS_HIGH = floatArrayOf(-11.0f, -4.5f, 5.0f, 7.5f, -4.5f)

    fun getPreEqGains(level: DialogueBoostLevel): FloatArray {
        return when (level) {
            DialogueBoostLevel.OFF -> PRE_EQ_GAINS_OFF
            DialogueBoostLevel.LOW -> PRE_EQ_GAINS_LOW
            DialogueBoostLevel.MEDIUM -> PRE_EQ_GAINS_MED
            DialogueBoostLevel.HIGH -> PRE_EQ_GAINS_HIGH
        }
    }

    fun getLegacyEqGains(level: DialogueBoostLevel): FloatArray {
        return when (level) {
            DialogueBoostLevel.OFF -> LEGACY_EQ_GAINS_OFF
            DialogueBoostLevel.LOW -> LEGACY_EQ_GAINS_LOW
            DialogueBoostLevel.MEDIUM -> LEGACY_EQ_GAINS_MED
            DialogueBoostLevel.HIGH -> LEGACY_EQ_GAINS_HIGH
        }
    }

    /**
     * Calculates input gain for DynamicsProcessing when MBC is disabled.
     * G_input_applied = min(G_boost, T_lim + D_max - alpha_crest * max_k(G_PreEQ,k))
     */
    fun calculateInputGainMbcDisabled(gBoost: Float, preEqGains: FloatArray): Float {
        val maxPreEq = preEqGains.maxOrNull()?.coerceAtLeast(0.0f) ?: 0.0f
        val maxAllowed = T_LIM + D_MAX - ALPHA_CREST * maxPreEq
        return minOf(gBoost, maxAllowed)
    }

    /**
     * Calculates dynamic scaled makeup gains G*_MBC_post,m for each MBC band when MBC is enabled.
     * Prevents Limiter overdrive: L_peak,m <= T_lim = -1.5 dBFS.
     */
    fun calculateMbcPostGains(gBoost: Float, preEqGains: FloatArray): FloatArray {
        // Map PreEQ bands to MBC bands:
        // Band 0 (20-220 Hz): PreEQ band 0 (cutoff 110 Hz)
        val maxPreEq0 = preEqGains[0].coerceAtLeast(0.0f)
        // Band 1 (220-4500 Hz): PreEQ bands 1, 2, 3 (cutoffs 360, 1450, 3500 Hz)
        val maxPreEq1 = maxOf(preEqGains[1], preEqGains[2], preEqGains[3]).coerceAtLeast(0.0f)
        // Band 2 (4500-20000 Hz): PreEQ bands 4, 5 (cutoffs 6200, 20000 Hz)
        val maxPreEq2 = maxOf(preEqGains[4], preEqGains[5]).coerceAtLeast(0.0f)

        val maxPreEqPerMbc = floatArrayOf(maxPreEq0, maxPreEq1, maxPreEq2)
        val result = FloatArray(3)

        for (m in 0 until 3) {
            val tComp = MBC_THRESHOLDS_DBFS[m]
            val r = MBC_RATIOS[m]
            val maxPreEq = maxPreEqPerMbc[m]

            // L_max_comp,m = T_comp,m + (0 dBFS + max_{k in m}(G_PreEQ,k) - T_comp,m) / R_m
            val lMaxComp = tComp + (0.0f + maxPreEq - tComp) / r
            // H_avail,m = T_lim - L_max_comp,m
            val hAvail = T_LIM - lMaxComp
            // G*_MBC_post,m = min(G_MBC_post,m + beta_m * G_boost, H_avail,m)
            val gPostDemand = MBC_BASE_POST_GAINS_DB[m] + BETA[m] * gBoost
            result[m] = minOf(gPostDemand, hAvail)
        }
        return result
    }

    data class LegacyGains(
        val normalizedBandGainsMb: ShortArray,
        val loudnessEnhancerMb: Int
    )

    /**
     * Legacy API < 28 Headroom Clamping (Equalizer + LoudnessEnhancer).
     * Shifts Equalizer bands downward so peak is 0 dB, and transfers net positive gain
     * to LoudnessEnhancer targeting look-ahead safe ceiling.
     */
    fun calculateLegacyGains(gBoost: Float, eqGainsDb: FloatArray): LegacyGains {
        val gEqMax = eqGainsDb.maxOrNull()?.coerceAtLeast(0.0f) ?: 0.0f
        val normalizedMb = ShortArray(eqGainsDb.size) { i ->
            val normDb = eqGainsDb[i] - gEqMax
            (normDb * 100f).roundToInt().toShort()
        }
        val leGainDb = minOf(14.0f, gBoost + 0.75f * gEqMax)
        val leMb = (100f * leGainDb).roundToInt()
        return LegacyGains(normalizedMb, leMb)
    }
}

/**
 * Manages the Android AudioFX DSP lifecycle, Session attachments, and State Machine transitions.
 * Supports API >= 28 DynamicsProcessing with automatic fallback to Equalizer + LoudnessEnhancer.
 * Implements Section 4.3 of Specification.
 */
class AudioDspManager(private val context: Context) {

    private val TAG = "AudioDspManager"

    private var currentSessionId: Int = 0
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var dpAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    private var activeBoostLevel = DialogueBoostLevel.OFF
    private var activeNightMode = false
    private var activeVolumeBoostDb = 0.0f

    /**
     * Attaches audio session ID or cleans up and reinitializes when session changes.
     */
    fun setAudioSessionId(sessionId: Int) {
        if (sessionId == currentSessionId && (dynamicsProcessing != null || equalizer != null || loudnessEnhancer != null)) {
            return
        }
        release()
        currentSessionId = sessionId
        if (sessionId <= 0) return

        // Fresh probe for DynamicsProcessing on API 28+ for newly attached sessions
        dpAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        initAudioFx(sessionId)
        applyCurrentParameters()
    }

    private fun initAudioFx(sessionId: Int) {
        if (dpAvailable) {
            try {
                initDynamicsProcessing(sessionId)
                return
            } catch (t: Throwable) {
                Log.w(TAG, "DynamicsProcessing initialization failed on session $sessionId; falling back to legacy AudioFX: ${t.message}")
                dpAvailable = false
                releaseDynamicsProcessing()
            }
        }
        initLegacyAudioFx(sessionId)
    }

    private fun initDynamicsProcessing(sessionId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
            2,     // 2 channels (Stereo)
            true,  // PreEQ in use
            6,     // 6 PreEQ bands
            true,  // MBC in use
            3,     // 3 MBC bands
            false, // PostEQ disabled
            0,
            true   // Limiter in use
        ).setPreferredFrameDuration(5.0f)
         .build()

        dynamicsProcessing = DynamicsProcessing(0, sessionId, config)

        // Pre-configure static limiter parameters
        val limiter = DynamicsProcessing.Limiter(
            true, // inUse
            true, // enabled
            0,    // linkGroup (stereo linked)
            AudioDspMath.LIMITER_ATTACK_MS,
            AudioDspMath.LIMITER_RELEASE_MS,
            AudioDspMath.LIMITER_RATIO,
            AudioDspMath.T_LIM,
            AudioDspMath.LIMITER_POST_GAIN_DB
        )
        dynamicsProcessing?.setLimiterAllChannelsTo(limiter)
    }

    private fun initLegacyAudioFx(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = false
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Legacy Equalizer init failed: ${t.message}")
            equalizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = false
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Legacy LoudnessEnhancer init failed: ${t.message}")
            loudnessEnhancer = null
        }
    }

    fun updateParameters(
        boostLevel: DialogueBoostLevel = activeBoostLevel,
        nightModeDrc: Boolean = activeNightMode,
        volumeBoostDb: Float = activeVolumeBoostDb
    ) {
        activeBoostLevel = boostLevel
        activeNightMode = nightModeDrc
        activeVolumeBoostDb = volumeBoostDb.coerceIn(0.0f, 12.0f)

        applyCurrentParameters()
    }

    private fun applyCurrentParameters() {
        if (currentSessionId <= 0) return

        if (dpAvailable && dynamicsProcessing != null) {
            applyDynamicsProcessing()
        } else {
            applyLegacyAudioFx()
        }
    }

    private fun applyDynamicsProcessing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dp = dynamicsProcessing ?: return

        try {
            val preEqGains = AudioDspMath.getPreEqGains(activeBoostLevel)

            // 1. Configure 6-Band PreEQ atomically
            val isPreEqNeeded = activeBoostLevel != DialogueBoostLevel.OFF
            val eq = DynamicsProcessing.Eq(true, isPreEqNeeded, 6)
            for (k in 0 until 6) {
                val band = DynamicsProcessing.EqBand(
                    isPreEqNeeded,
                    AudioDspMath.PRE_EQ_CUTOFFS_HZ[k],
                    if (isPreEqNeeded) preEqGains[k] else 0.0f
                )
                eq.setBand(k, band)
            }
            dp.setPreEqAllChannelsTo(eq)

            // 2. Configure 3-Band MBC & Headroom Input Gain atomically
            if (activeNightMode) {
                val dynamicPostGains = AudioDspMath.calculateMbcPostGains(activeVolumeBoostDb, preEqGains)
                val mbc = DynamicsProcessing.Mbc(true, true, 3)
                for (m in 0 until 3) {
                    val mbcBand = DynamicsProcessing.MbcBand(
                        true,
                        AudioDspMath.MBC_CUTOFFS_HZ[m],
                        AudioDspMath.MBC_ATTACK_TIMES_MS[m],
                        AudioDspMath.MBC_RELEASE_TIMES_MS[m],
                        AudioDspMath.MBC_RATIOS[m],
                        AudioDspMath.MBC_THRESHOLDS_DBFS[m],
                        AudioDspMath.MBC_KNEE_WIDTHS_DB[m],
                        AudioDspMath.MBC_GATE_THRESHOLDS_DBFS[m],
                        AudioDspMath.MBC_EXPANDER_RATIOS[m],
                        AudioDspMath.MBC_PRE_GAINS_DB[m],
                        dynamicPostGains[m]
                    )
                    mbc.setBand(m, mbcBand)
                }
                dp.setMbcAllChannelsTo(mbc)
                // Unity input when MBC handles dynamic headroom
                dp.setInputGainAllChannelsTo(0.0f)
            } else {
                dp.setMbcAllChannelsTo(DynamicsProcessing.Mbc(false, false, 3))
                // Apply input gain scaling to protect limiter
                val inputGain = AudioDspMath.calculateInputGainMbcDisabled(activeVolumeBoostDb, preEqGains)
                dp.setInputGainAllChannelsTo(inputGain)
            }

            // Master enable: active if Dialogue Clarity, Night Mode DRC, or Volume Boost are non-zero
            val needsDsp = activeBoostLevel != DialogueBoostLevel.OFF || activeNightMode || activeVolumeBoostDb > 0.0f
            dp.enabled = needsDsp
        } catch (t: Throwable) {
            Log.w(TAG, "Error applying DynamicsProcessing: ${t.message}; falling back to legacy AudioFX")
            dpAvailable = false
            releaseDynamicsProcessing()
            initLegacyAudioFx(currentSessionId)
            applyLegacyAudioFx()
        }
    }

    private fun applyLegacyAudioFx() {
        try {
            val legacyEqGains = AudioDspMath.getLegacyEqGains(activeBoostLevel)
            val legacySettings = AudioDspMath.calculateLegacyGains(activeVolumeBoostDb, legacyEqGains)

            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
                for (i in 0 until numBands) {
                    val level = legacySettings.normalizedBandGainsMb.getOrElse(i) { 0 }
                    eq.setBandLevel(i.toShort(), level)
                }
                eq.enabled = activeBoostLevel != DialogueBoostLevel.OFF
            }

            loudnessEnhancer?.let { le ->
                val targetGain = if (activeNightMode && legacySettings.loudnessEnhancerMb == 0) {
                    350 // Night mode speech boost fallback
                } else {
                    legacySettings.loudnessEnhancerMb
                }

                if (targetGain > 0) {
                    le.setTargetGain(targetGain)
                    le.enabled = true
                } else {
                    le.enabled = false
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error applying legacy AudioFX: ${t.message}")
        }
    }

    private fun releaseDynamicsProcessing() {
        try {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
        } catch (_: Throwable) {}
        dynamicsProcessing = null
    }

    private fun releaseLegacyAudioFx() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        loudnessEnhancer = null
    }

    fun release() {
        releaseDynamicsProcessing()
        releaseLegacyAudioFx()
        currentSessionId = 0
    }
}
