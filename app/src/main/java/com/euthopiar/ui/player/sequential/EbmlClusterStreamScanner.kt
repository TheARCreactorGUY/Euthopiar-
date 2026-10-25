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

    fun onBytesReceived(buffer: ByteArray, offset: Int, length: Int, streamOffsetOfBufferStart: Long) {
        for (i in 0 until length) {
            val b = buffer[offset + i].toInt() and 0xFF
            val absPos = streamOffsetOfBufferStart + i

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
                        matchState = if (vintBytesRemaining == 0) 6 else 5
                    } else {
                        matchState = 0
                    }
                }
                5 -> {
                    vintBytesRemaining--
                    if (vintBytesRemaining == 0) matchState = 6
                }
                6 -> {
                    // Expect Timestamp element ID (0xE7)
                    matchState = if (b == 0xE7) 7 else 0
                }
                7 -> {
                    // Timestamp length VINT (1..8 bytes, encoded as 0x81..0x88)
                    val tsLen = b and 0x7F
                    if ((b and 0x80) != 0 && tsLen in 1..8) {
                        timestampBytesRemaining = tsLen
                        timestampAccumulator = 0L
                        matchState = 8
                    } else {
                        matchState = 0
                    }
                }
                8 -> {
                    timestampAccumulator = (timestampAccumulator shl 8) or b.toLong()
                    timestampBytesRemaining--
                    if (timestampBytesRemaining == 0) {
                        index.recordCluster(timestampAccumulator, clusterStartOffset)
                        matchState = 0
                    }
                }
            }
        }
    }
}
