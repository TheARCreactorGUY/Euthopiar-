package com.euthopiar.ui.player.sequential

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekPoint
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe sparse index mapping Matroska Cluster timestamps (in microseconds)
 * to their exact starting byte offset (the 0x1F byte of 0x1F43B675).
 */
@UnstableApi
class MatroskaClusterIndex {
    private val lock = ReentrantReadWriteLock()
    private var timesUs = LongArray(2048)
    private var byteOffsets = LongArray(2048)
    var size: Int = 0
        private set

    // Default Matroska TimestampScale is 1,000,000 ns (1 ms = 1,000 us)
    @Volatile var timestampScaleNs: Long = 1_000_000L
    @Volatile var parsedDurationUs: Long = androidx.media3.common.C.TIME_UNSET

    fun recordCluster(clusterTimecodeTicks: Long, clusterStartByteOffset: Long) {
        val timeUs = (clusterTimecodeTicks * timestampScaleNs) / 1_000L
        lock.write {
            if (size > 0 && byteOffsets[size - 1] == clusterStartByteOffset) return
            if (size > 0 && timeUs <= timesUs[size - 1]) return // Enforce monotonic order
            if (size == timesUs.size) {
                timesUs = timesUs.copyOf(timesUs.size * 2)
                byteOffsets = byteOffsets.copyOf(byteOffsets.size * 2)
            }
            timesUs[size] = timeUs
            byteOffsets[size] = clusterStartByteOffset
            size++
        }
    }

    fun getMaxIndexedTimeUs(): Long = lock.read {
        if (size == 0) 0L else timesUs[size - 1]
    }

    fun getMaxIndexedByteOffset(): Long = lock.read {
        if (size == 0) 0L else byteOffsets[size - 1]
    }

    /**
     * Finds the closest indexed Cluster at or before [targetTimeUs].
     */
    fun findFloorCluster(targetTimeUs: Long): SeekPoint? = lock.read {
        if (size == 0) return null
        val idx = timesUs.binarySearch(targetTimeUs, 0, size)
        val resolvedIdx = if (idx >= 0) idx else (-idx - 2).coerceAtLeast(0)
        SeekPoint(timesUs[resolvedIdx], byteOffsets[resolvedIdx])
    }
}
