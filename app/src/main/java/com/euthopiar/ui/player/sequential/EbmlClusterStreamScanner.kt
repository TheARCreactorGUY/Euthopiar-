package com.euthopiar.ui.player.sequential

import androidx.media3.common.util.UnstableApi

/**
 * Zero-allocation streaming byte scanner that detects Matroska Cluster (0x1F43B675)
 * followed by Timestamp (0xE7) as bytes flow from OkHttp to disk.
 *
 * Pattern inside Matroska Segment:
 *   [0x1F, 0x43, 0xB6, 0x75] (Cluster ID)
 *   [1..8 bytes VINT]        (Cluster Data Size)
 *   [0xE7]                   (Timestamp ID)
 *   [1 byte VINT: 0x81..0x88](Timestamp Size: 1..8 bytes)
 *   [1..8 bytes UInt]        (Timestamp Value)
 */
@UnstableApi
class EbmlClusterStreamScanner(
    private val index: MatroskaClusterIndex
) {
    private var matchState = 0
    private var clusterStartOffset = 0L
    private var vintBytesRemaining = 0
    private var timestampBytesRemaining = 0
    private var timestampAccumulator = 0L

    // For skipping optional small elements before 0xE7 (CRC-32, Position, PrevSize)
    private var clusterBytesInspected = 0
    private var skipVintBytesRemaining = 0
    private var skipVintValue = 0L
    private var skipPayloadBytesRemaining = 0

    // Initial EBML SegmentInfo header scanner for TimestampScale (0x2AD7B1) and Duration (0x4489)
    private var headerState = 0
    private var headerMode = 0 // 1 = TimestampScale, 2 = Duration
    private var headerVintRemaining = 0
    private var headerVintValue = 0L
    private var headerPayloadLength = 0
    private var headerPayloadRemaining = 0
    private var headerAccumulator = 0L

    fun onBytesReceived(buffer: ByteArray, offset: Int, length: Int, streamOffsetOfBufferStart: Long) {
        for (i in 0 until length) {
            val b = buffer[offset + i].toInt() and 0xFF
            val absPos = streamOffsetOfBufferStart + i

            // Scan initial EBML SegmentInfo header within first 1 MB of the stream
            if (absPos < 1024L * 1024L) {
                scanSegmentInfo(b)
            }

            when (matchState) {
                0 -> if (b == 0x1F) { clusterStartOffset = absPos; matchState = 1 }
                1 -> matchState = if (b == 0x43) 2 else if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                2 -> matchState = if (b == 0xB6) 3 else if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                3 -> matchState = if (b == 0x75) 4 else if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                4 -> {
                    // First byte of Cluster Size VINT; count leading zeros to find VINT length (1..8)
                    val vintLen = Integer.numberOfLeadingZeros(b) - 23
                    if (vintLen in 1..8) {
                        vintBytesRemaining = vintLen - 1
                        if (vintBytesRemaining == 0) {
                            clusterBytesInspected = 0
                            matchState = 6
                        } else {
                            matchState = 5
                        }
                    } else {
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    }
                }
                5 -> {
                    vintBytesRemaining--
                    if (vintBytesRemaining == 0) {
                        clusterBytesInspected = 0
                        matchState = 6
                    }
                }
                6 -> {
                    clusterBytesInspected++
                    if (clusterBytesInspected > 64) {
                        // Exceeded search window for Timestamp in this cluster
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    } else if (b == 0xE7) {
                        // Found Timestamp element ID
                        matchState = 7
                    } else if (b == 0xBF || b == 0xA7 || b == 0xAB) {
                        // Skippable element inside Cluster: CRC-32 (0xBF), Position (0xA7), or PrevSize (0xAB)
                        matchState = 9
                    } else if (b == 0xA3 || b == 0xA0) {
                        // Hit SimpleBlock (0xA3) or BlockGroup (0xA0) without Timestamp; abort
                        matchState = 0
                    } else {
                        // Unexpected byte inside cluster header; abort looking for Timestamp
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    }
                }
                7 -> {
                    clusterBytesInspected++
                    // Timestamp length VINT (1..8 bytes, encoded as 0x81..0x88)
                    val tsLen = b and 0x7F
                    if ((b and 0x80) != 0 && tsLen in 1..8) {
                        timestampBytesRemaining = tsLen
                        timestampAccumulator = 0L
                        matchState = 8
                    } else {
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    }
                }
                8 -> {
                    clusterBytesInspected++
                    timestampAccumulator = (timestampAccumulator shl 8) or b.toLong()
                    timestampBytesRemaining--
                    if (timestampBytesRemaining == 0) {
                        index.recordCluster(timestampAccumulator, clusterStartOffset)
                        matchState = 0
                    }
                }
                9 -> {
                    clusterBytesInspected++
                    if (clusterBytesInspected > 64) {
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    } else {
                        // First byte of skippable element size VINT
                        val vintLen = Integer.numberOfLeadingZeros(b) - 23
                        if (vintLen in 1..8) {
                            val mask = (1 shl (8 - vintLen)) - 1
                            skipVintValue = (b and mask).toLong()
                            if (vintLen == 1) {
                                skipPayloadBytesRemaining = skipVintValue.toInt()
                                if (skipPayloadBytesRemaining > 64) {
                                    matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                                } else {
                                    matchState = if (skipPayloadBytesRemaining == 0) 6 else 11
                                }
                            } else {
                                skipVintBytesRemaining = vintLen - 1
                                matchState = 10
                            }
                        } else {
                            matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                        }
                    }
                }
                10 -> {
                    clusterBytesInspected++
                    if (clusterBytesInspected > 64) {
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    } else {
                        skipVintValue = (skipVintValue shl 8) or b.toLong()
                        skipVintBytesRemaining--
                        if (skipVintBytesRemaining == 0) {
                            skipPayloadBytesRemaining = skipVintValue.toInt()
                            if (skipPayloadBytesRemaining > 64 || skipPayloadBytesRemaining < 0) {
                                matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                            } else {
                                matchState = if (skipPayloadBytesRemaining == 0) 6 else 11
                            }
                        }
                    }
                }
                11 -> {
                    clusterBytesInspected++
                    if (clusterBytesInspected > 64) {
                        matchState = if (b == 0x1F) { clusterStartOffset = absPos; 1 } else 0
                    } else {
                        skipPayloadBytesRemaining--
                        if (skipPayloadBytesRemaining <= 0) {
                            matchState = 6
                        }
                    }
                }
            }
        }
    }

    private var rawDurationVal: Double? = null

    private fun scanSegmentInfo(b: Int) {
        when (headerState) {
            0 -> {
                if (b == 0x2A) headerState = 1
                else if (b == 0x44) headerState = 10
            }
            1 -> {
                if (b == 0xD7) headerState = 2
                else if (b == 0x2A) headerState = 1
                else if (b == 0x44) headerState = 10
                else headerState = 0
            }
            2 -> {
                if (b == 0xB1) {
                    headerMode = 1 // TimestampScale
                    headerState = 20
                } else if (b == 0x2A) {
                    headerState = 1
                } else if (b == 0x44) {
                    headerState = 10
                } else {
                    headerState = 0
                }
            }
            10 -> {
                if (b == 0x89) {
                    headerMode = 2 // Duration
                    headerState = 20
                } else if (b == 0x44) {
                    headerState = 10
                } else if (b == 0x2A) {
                    headerState = 1
                } else {
                    headerState = 0
                }
            }
            20 -> {
                // First byte of element length VINT
                val vintLen = Integer.numberOfLeadingZeros(b) - 23
                if (vintLen in 1..8) {
                    val mask = (1 shl (8 - vintLen)) - 1
                    headerVintValue = (b and mask).toLong()
                    if (vintLen == 1) {
                        headerPayloadLength = headerVintValue.toInt()
                        headerPayloadRemaining = headerPayloadLength
                        headerAccumulator = 0L
                        headerState = if (headerPayloadRemaining in 1..8) 22 else 0
                    } else {
                        headerVintRemaining = vintLen - 1
                        headerState = 21
                    }
                } else {
                    headerState = 0
                }
            }
            21 -> {
                headerVintValue = (headerVintValue shl 8) or b.toLong()
                headerVintRemaining--
                if (headerVintRemaining == 0) {
                    headerPayloadLength = headerVintValue.toInt()
                    headerPayloadRemaining = headerPayloadLength
                    headerAccumulator = 0L
                    headerState = if (headerPayloadRemaining in 1..8) 22 else 0
                }
            }
            22 -> {
                headerAccumulator = (headerAccumulator shl 8) or b.toLong()
                headerPayloadRemaining--
                if (headerPayloadRemaining == 0) {
                    if (headerMode == 1) {
                        if (headerAccumulator > 0L) {
                            index.timestampScaleNs = headerAccumulator
                            rawDurationVal?.let { dur ->
                                val durUs = ((dur * headerAccumulator) / 1000.0).toLong()
                                if (durUs > 0L) {
                                    index.parsedDurationUs = durUs
                                }
                            }
                        }
                    } else if (headerMode == 2) {
                        val durationVal = when (headerPayloadLength) {
                            4 -> java.lang.Float.intBitsToFloat(headerAccumulator.toInt()).toDouble()
                            8 -> java.lang.Double.longBitsToDouble(headerAccumulator)
                            else -> headerAccumulator.toDouble()
                        }
                        if (durationVal > 0.0) {
                            rawDurationVal = durationVal
                            val durationUs = ((durationVal * index.timestampScaleNs) / 1000.0).toLong()
                            if (durationUs > 0L) {
                                index.parsedDurationUs = durationUs
                            }
                        }
                    }
                    headerState = 0
                }
            }
        }
    }
}
