package com.euthopiar.ui.player.sequential

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mkv.MatroskaExtractor

@UnstableApi
class SequentialMatroskaExtractorsFactory(
    private val session: SequentialStreamSession
) : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        val extractors = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
            .setMatroskaExtractorFlags(MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)
            .createExtractors()

        return extractors.map { delegate ->
            object : Extractor {
                override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

                override fun init(output: ExtractorOutput) {
                    delegate.init(object : ExtractorOutput {
                        override fun track(id: Int, type: Int): TrackOutput = output.track(id, type)

                        override fun endTracks() = output.endTracks()

                        override fun seekMap(seekMap: SeekMap) {
                            output.seekMap(SafeSeekMap(seekMap, session))
                        }
                    })
                }

                override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int =
                    delegate.read(input, seekPosition)

                override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)

                override fun release() = delegate.release()
            }
        }.toTypedArray()
    }
}

@UnstableApi
class SafeSeekMap(
    private val delegate: SeekMap,
    private val session: SequentialStreamSession
) : SeekMap {

    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long {
        if (delegate.durationUs != C.TIME_UNSET && delegate.durationUs > 0) {
            return delegate.durationUs
        }
        val parsedDuration = session.clusterIndex.parsedDurationUs
        if (parsedDuration != C.TIME_UNSET && parsedDuration > 0) {
            return parsedDuration
        }
        val maxIndexedTime = session.clusterIndex.getMaxIndexedTimeUs()
        if (maxIndexedTime > 0) {
            return maxIndexedTime
        }
        return C.TIME_UNSET
    }

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        // User requested seek to 0:00 -> SeekPoint.START is standard and expected
        if (timeUs <= 0L) {
            return delegate.getSeekPoints(0L)
        }

        // Check if underlying delegate returns a valid non-zero seek point
        val delegatePoints = delegate.getSeekPoints(timeUs)
        val p1 = delegatePoints.first

        // If delegate parsed Cues and returned a valid non-zero seek position/time, use it!
        if (delegate.isSeekable && (p1.timeUs > 0L || p1.position > 0L)) {
            return delegatePoints
        }

        // Delegate returned SeekPoint.START (time = 0, pos = 0) or is unseekable.
        // This occurs when Cues are still loading or absent.
        // We MUST NEVER return SeekPoint.START (00:00) for non-zero seek timestamps!
        val durationUs = getDurationUs()
        val dynamicSeekMap = DynamicClusterSeekMap(
            if (durationUs != C.TIME_UNSET) durationUs else 0L,
            session
        )
        val dynPoints = dynamicSeekMap.getSeekPoints(timeUs)
        val dp1 = dynPoints.first

        // If DynamicClusterSeekMap returned a non-zero seek point, use it
        if (dp1.timeUs > 0L || dp1.position > 0L) {
            return dynPoints
        }

        // Fallback: Synthesize an estimated non-zero seek point so ExoPlayer never resets to 00:00
        val totalBytes = session.totalContentLength
        val estimatedOffset = if (totalBytes > 0 && durationUs > 0) {
            ((timeUs.toDouble() / durationUs.toDouble()) * totalBytes).toLong().coerceIn(1L, totalBytes)
        } else {
            session.highWaterMark.coerceAtLeast(1L)
        }
        session.pendingTargetTimeUs = timeUs
        return SeekMap.SeekPoints(SeekPoint(timeUs, estimatedOffset))
    }
}
