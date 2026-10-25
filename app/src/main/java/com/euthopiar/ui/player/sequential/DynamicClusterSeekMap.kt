package com.euthopiar.ui.player.sequential

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint

@UnstableApi
class DynamicClusterSeekMap(
    private val durationUs: Long,
    private val session: SequentialStreamSession
) : SeekMap {

    override fun isSeekable(): Boolean = durationUs != C.TIME_UNSET && durationUs > 0

    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        if (timeUs <= 0L) {
            return SeekMap.SeekPoints(SeekPoint.START)
        }

        val index = session.clusterIndex
        val maxIndexedTimeUs = index.getMaxIndexedTimeUs()

        // Case 1: Target time is already downloaded & indexed -> Exact Cluster hit
        if (timeUs <= maxIndexedTimeUs) {
            val floor = index.findFloorCluster(timeUs)
            if (floor != null && (floor.timeUs > 0L || floor.position > 0L)) {
                return SeekMap.SeekPoints(floor)
            }
        }

        // Case 2: Target time is ahead of highWaterMark (forward seek)
        val totalBytes = session.totalContentLength
        if (totalBytes <= 0 || durationUs <= 0) {
            val latest = index.findFloorCluster(maxIndexedTimeUs)
            if (latest != null && (latest.timeUs > 0L || latest.position > 0L)) {
                return SeekMap.SeekPoints(latest)
            }
            session.pendingTargetTimeUs = timeUs
            val fallbackPos = session.highWaterMark.coerceAtLeast(1L)
            return SeekMap.SeekPoints(SeekPoint(timeUs, fallbackPos))
        }

        val estimatedTargetOffset = ((timeUs.toDouble() / durationUs.toDouble()) * totalBytes).toLong().coerceIn(1L, totalBytes)

        // If the jump exceeds allowed forward bridge threshold, clamp to buffered edge or latest cluster
        if (!session.isOffsetAvailable(estimatedTargetOffset)) {
            val latest = index.findFloorCluster(maxIndexedTimeUs)
            if (latest != null && (latest.timeUs > 0L || latest.position > 0L)) {
                return SeekMap.SeekPoints(latest)
            }
            val fallbackOffset = session.highWaterMark.coerceAtLeast(1L)
            val fallbackTimeUs = if (totalBytes > 0 && durationUs > 0) {
                ((fallbackOffset.toDouble() / totalBytes.toDouble()) * durationUs).toLong().coerceAtLeast(1L)
            } else {
                timeUs
            }
            session.pendingTargetTimeUs = fallbackTimeUs
            return SeekMap.SeekPoints(SeekPoint(fallbackTimeUs, fallbackOffset))
        }

        // Bridge to [timeUs]
        session.pendingTargetTimeUs = timeUs
        return SeekMap.SeekPoints(SeekPoint(timeUs, estimatedTargetOffset))
    }
}
