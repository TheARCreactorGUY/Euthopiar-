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
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor

/**
 * LoopbackMatroskaExtractorsFactory provides high-resilience extraction for local loopback (127.0.0.1)
 * P2P torrent streams.
 *
 * It allows MatroskaExtractor to naturally fetch and parse tail Cues with the 65-second loopback
 * read timeout, building an exact cluster-aligned seek table for smooth scrubbing.
 *
 * If a stream has missing or corrupted Cues, LoopbackSafeSeekMap provides a safety fallback so
 * that seeking does not snap back to 00:00.
 */
@UnstableApi
class LoopbackMatroskaExtractorsFactory(
    private val totalContentLengthProvider: () -> Long,
    private val durationMsProvider: () -> Long
) : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        val extractors = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
            .createExtractors()

        return extractors.map { delegate ->
            object : Extractor {
                override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

                override fun init(output: ExtractorOutput) {
                    delegate.init(object : ExtractorOutput {
                        override fun track(id: Int, type: Int): TrackOutput = output.track(id, type)

                        override fun endTracks() = output.endTracks()

                        override fun seekMap(seekMap: SeekMap) {
                            output.seekMap(
                                LoopbackSafeSeekMap(
                                    delegate = seekMap,
                                    totalContentLengthProvider = totalContentLengthProvider,
                                    durationMsProvider = durationMsProvider
                                )
                            )
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

/**
 * LoopbackSafeSeekMap prevents ExoPlayer from snapping seeks to SeekPoint.START (00:00) when
 * Matroska Cues are omitted.
 */
@UnstableApi
class LoopbackSafeSeekMap(
    private val delegate: SeekMap,
    private val totalContentLengthProvider: () -> Long,
    private val durationMsProvider: () -> Long
) : SeekMap {

    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long {
        if (delegate.durationUs != C.TIME_UNSET && delegate.durationUs > 0) {
            return delegate.durationUs
        }
        val durMs = durationMsProvider()
        if (durMs > 0L) {
            return durMs * 1000L
        }
        return C.TIME_UNSET
    }

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        if (timeUs <= 0L) {
            return delegate.getSeekPoints(0L)
        }

        // If underlying delegate parsed cues or is seekable with a non-zero seek position, use it
        val delegatePoints = delegate.getSeekPoints(timeUs)
        val p1 = delegatePoints.first
        if (delegate.isSeekable && (p1.timeUs > 0L || p1.position > 0L)) {
            return delegatePoints
        }

        // Delegate returned SeekPoint.START or is unseekable.
        // Synthesize an estimated non-zero seek point based on totalContentLength and durationUs
        // so ExoPlayer seeks to the corresponding byte offset rather than snapping back to 00:00!
        val durationUs = getDurationUs()
        val totalBytes = totalContentLengthProvider()
        val estimatedOffset = if (totalBytes > 0L && durationUs > 0L && durationUs != C.TIME_UNSET) {
            ((timeUs.toDouble() / durationUs.toDouble()) * totalBytes).toLong().coerceIn(1L, totalBytes)
        } else {
            1L
        }
        return SeekMap.SeekPoints(SeekPoint(timeUs, estimatedOffset))
    }
}
