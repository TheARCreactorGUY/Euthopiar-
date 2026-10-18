package com.euthopiar.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Passive 3-layer Bandwidth Estimator & Quality Decider.
 *
 * Layer 1: Android NetworkCapabilities.linkDownstreamBandwidthKbps (0.35x realistic discount factor as cold-start prior).
 * Layer 2: OkHttp EventListener tracking real throughput of TMDB image/metadata downloads with an Exponentially Weighted Moving Average (EWMA), filtering out <32KB payloads.
 * Layer 3: Tiny 96KB range probe fallback if fewer than 2 passive samples exist.
 */
class BandwidthEstimator private constructor() {

    private val sampleCount = AtomicInteger(0)
    @Volatile
    private var ewmaBps: Double = 0.0

    // Suppressed conflict quality tiers for the current app session
    private val suppressedTiers = ConcurrentHashMap.newKeySet<String>()

    /**
     * Layer 1: Read Android Link Downstream Bandwidth with 0.35x realistic discount factor.
     */
    fun estimateColdStartFromCapabilities(context: Context?): Long {
        if (context == null) return DEFAULT_BITRATE_BPS
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            val kbps = caps?.linkDownstreamBandwidthKbps ?: 0
            if (kbps > 0) {
                (kbps * 1000L * 0.35).toLong().coerceIn(1_000_000L, 100_000_000L)
            } else {
                DEFAULT_BITRATE_BPS
            }
        } catch (_: Exception) {
            DEFAULT_BITRATE_BPS
        }
    }

    /**
     * Records a throughput sample (bps) into the EWMA.
     * Filters out payloads < 32 KB.
     */
    fun recordThroughputSample(bytesTransferred: Long, durationMs: Long) {
        if (bytesTransferred < 32 * 1024L || durationMs <= 5L) {
            return
        }
        val sampleBps = (bytesTransferred * 8.0 * 1000.0) / durationMs
        synchronized(this) {
            val count = sampleCount.incrementAndGet()
            if (count == 1 || ewmaBps <= 0.0) {
                ewmaBps = sampleBps
            } else {
                val alpha = 0.25
                ewmaBps = alpha * sampleBps + (1.0 - alpha) * ewmaBps
            }
        }
    }

    /**
     * Layer 2: OkHttp EventListener for passive throughput tracking without extra byte overhead.
     */
    val eventListener: EventListener = object : EventListener() {
        private val startTimes = ConcurrentHashMap<Call, Long>()

        override fun responseHeadersEnd(call: Call, response: Response) {
            startTimes[call] = System.currentTimeMillis()
        }

        override fun responseBodyEnd(call: Call, byteCount: Long) {
            val start = startTimes.remove(call) ?: return
            val elapsed = System.currentTimeMillis() - start
            recordThroughputSample(byteCount, elapsed)
        }

        override fun callFailed(call: Call, ioe: IOException) {
            startTimes.remove(call)
        }
    }

    /**
     * Layer 3: Tiny 96KB Range probe fallback if fewer than 2 passive samples exist.
     */
    suspend fun probeTinyFallbackIfSparse(client: OkHttpClient, probeUrl: String): Long? = withContext(Dispatchers.IO) {
        if (sampleCount.get() >= 2) {
            return@withContext getEstimatedBandwidthBps(null)
        }
        try {
            val req = Request.Builder()
                .url(probeUrl)
                .header("Range", "bytes=0-98303") // 96 KB
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val start = System.currentTimeMillis()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 206) {
                    val bytes = resp.body?.bytes() ?: return@withContext null
                    val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                    recordThroughputSample(bytes.size.toLong(), elapsed)
                    return@withContext getEstimatedBandwidthBps(null)
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Current estimated bandwidth in bps.
     */
    fun getEstimatedBandwidthBps(context: Context?): Long {
        val currentEwma = ewmaBps
        if (sampleCount.get() > 0 && currentEwma > 0.0) {
            return currentEwma.toLong()
        }
        return estimateColdStartFromCapabilities(context)
    }

    /**
     * Maps estimated bandwidth to quality tiers with 1.40x VBR headroom factor:
     * - < 3 Mbps -> 480p
     * - 3-8 Mbps -> 720p
     * - 8-22 Mbps -> 1080p
     * - > 22 Mbps -> 4K
     */
    fun decideAutoQuality(estimatedBps: Long): String {
        val effectiveRate = estimatedBps / 1.40
        return when {
            effectiveRate < 3_000_000L -> "480p"
            effectiveRate < 8_000_000L -> "720p"
            effectiveRate <= 22_000_000L -> "1080p"
            else -> "4K"
        }
    }

    fun getRequiredBitrate(tier: String): Long {
        val lower = tier.lowercase()
        return when {
            lower.contains("4k") || lower.contains("2160") -> 22_000_000L
            lower.contains("1080") || lower.contains("fhd") -> 8_000_000L
            lower.contains("720") || lower.contains("hd") -> 3_000_000L
            else -> 1_500_000L
        }
    }

    /**
     * Checks if a manually chosen quality tier exceeds 1.25x estimated bandwidth.
     * Respects session anti-fatigue: returns null if tier is suppressed.
     */
    fun checkQualityConflict(requestedTier: String, estimatedBps: Long): QualityConflict? {
        val normTier = normalizeTier(requestedTier)
        if (normTier.equals("auto", ignoreCase = true) || isTierSuppressed(normTier)) {
            return null
        }
        val required = getRequiredBitrate(normTier)
        val threshold = (estimatedBps * 1.25).toLong()
        if (required > threshold) {
            val recommended = decideAutoQuality(estimatedBps)
            return QualityConflict(
                requestedQuality = normTier,
                recommendedQuality = recommended,
                requiredBitrateBps = required,
                estimatedBitrateBps = estimatedBps
            )
        }
        return null
    }

    fun suppressConflictForSession(tier: String) {
        suppressedTiers.add(normalizeTier(tier))
    }

    fun isTierSuppressed(tier: String): Boolean {
        return suppressedTiers.contains(normalizeTier(tier))
    }

    fun clearSuppressedTiers() {
        suppressedTiers.clear()
    }

    /**
     * Called when stream plays for 90s without buffering.
     * Raises bandwidth estimate so future playback of this tier doesn't trigger alerts.
     */
    fun onUnbufferedPlaybackAchieved(tier: String) {
        val req = getRequiredBitrate(normalizeTier(tier))
        val targetBps = (req * 1.40).coerceAtLeast(req.toDouble())
        synchronized(this) {
            if (ewmaBps < targetBps) {
                ewmaBps = targetBps
                sampleCount.incrementAndGet()
            }
        }
    }

    fun normalizeTier(tier: String): String {
        val lower = tier.lowercase().trim()
        return when {
            lower.contains("4k") || lower.contains("2160") -> "4K"
            lower.contains("1080") || lower.contains("fhd") -> "1080p"
            lower.contains("720") || lower.contains("hd") -> "720p"
            lower.contains("480") || lower.contains("sd") -> "480p"
            lower == "auto" -> "Auto"
            else -> tier.trim()
        }
    }

    fun setEstimatedBandwidthForTesting(bps: Long?) {
        if (bps != null) {
            ewmaBps = bps.toDouble()
            sampleCount.set(2)
        } else {
            ewmaBps = 0.0
            sampleCount.set(0)
        }
    }

    fun resetForTesting() {
        ewmaBps = 0.0
        sampleCount.set(0)
        suppressedTiers.clear()
    }

    companion object {
        const val DEFAULT_BITRATE_BPS = 12_000_000L // 12 Mbps default prior

        val Default: BandwidthEstimator by lazy { BandwidthEstimator() }
    }
}

data class QualityConflict(
    val requestedQuality: String,
    val recommendedQuality: String,
    val requiredBitrateBps: Long,
    val estimatedBitrateBps: Long
)
