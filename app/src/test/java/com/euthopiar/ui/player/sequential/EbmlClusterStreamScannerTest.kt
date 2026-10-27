package com.euthopiar.ui.player.sequential

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

@UnstableApi
class EbmlClusterStreamScannerTest {

    @Test
    fun scanner_detectsNormalClusterWithoutSkippables() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        val bytes = byteArrayOf(
            0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte(), // Cluster ID
            0x88.toByte(),                                              // VINT size = 8
            0xE7.toByte(),                                              // Timestamp ID
            0x82.toByte(),                                              // VINT length = 2
            0x03.toByte(), 0xE8.toByte()                                // Value = 1000 ms
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 0L)

        assertEquals(1, index.size)
        assertEquals(1_000_000L, index.getMaxIndexedTimeUs())
        assertEquals(0L, index.getMaxIndexedByteOffset())
    }

    @Test
    fun scanner_skipsCrc32BeforeTimestamp() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        val bytes = byteArrayOf(
            0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte(), // Cluster ID
            0x8E.toByte(),                                              // VINT size = 14
            0xBF.toByte(), 0x84.toByte(),                               // CRC-32 ID, len = 4
            0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), // CRC-32 4 bytes
            0xE7.toByte(),                                              // Timestamp ID
            0x82.toByte(),                                              // VINT length = 2
            0x07.toByte(), 0xD0.toByte()                                // Value = 2000 ms
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 1024L)

        assertEquals(1, index.size)
        assertEquals(2_000_000L, index.getMaxIndexedTimeUs())
        assertEquals(1024L, index.getMaxIndexedByteOffset())
    }

    @Test
    fun scanner_skipsMultipleElementsCrc32PrevSizePosition() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        val bytes = byteArrayOf(
            0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte(), // Cluster ID
            0x95.toByte(),                                              // VINT size
            0xBF.toByte(), 0x84.toByte(), 0x01, 0x02, 0x03, 0x04,       // CRC-32
            0xAB.toByte(), 0x82.toByte(), 0x05, 0x06,                   // PrevSize
            0xA7.toByte(), 0x81.toByte(), 0x07,                         // Position
            0xE7.toByte(), 0x82.toByte(), 0x0B.toByte(), 0xB8.toByte()  // Timestamp = 3000 ms
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 2048L)

        assertEquals(1, index.size)
        assertEquals(3_000_000L, index.getMaxIndexedTimeUs())
        assertEquals(2048L, index.getMaxIndexedByteOffset())
    }

    @Test
    fun scanner_parsesSegmentInfoTimestampScaleAndFloatDuration() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        // 120 seconds = 120,000 ms (TimestampScale ticks)
        val floatBits = java.lang.Float.floatToIntBits(120000.0f)
        val floatBytes = ByteBuffer.allocate(4).putInt(floatBits).array()

        val bytes = byteArrayOf(
            0x2A.toByte(), 0xD7.toByte(), 0xB1.toByte(),               // TimestampScale ID
            0x83.toByte(),                                              // Length = 3
            0x0F.toByte(), 0x42.toByte(), 0x40.toByte(),                // 1,000,000 ns = 1 ms
            0x44.toByte(), 0x89.toByte(),                               // Duration ID
            0x84.toByte(),                                              // Length = 4
            floatBytes[0], floatBytes[1], floatBytes[2], floatBytes[3]
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 0L)

        assertEquals(1_000_000L, index.timestampScaleNs)
        assertEquals(120_000_000L, index.parsedDurationUs)
    }

    @Test
    fun scanner_parsesSegmentInfoDoubleDurationFirstThenTimestampScale() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        // 60 seconds = 60,000 ms (TimestampScale ticks)
        val doubleBits = java.lang.Double.doubleToLongBits(60000.0)
        val doubleBytes = ByteBuffer.allocate(8).putLong(doubleBits).array()

        val bytes = byteArrayOf(
            0x44.toByte(), 0x89.toByte(),                               // Duration ID
            0x88.toByte(),                                              // Length = 8
            doubleBytes[0], doubleBytes[1], doubleBytes[2], doubleBytes[3],
            doubleBytes[4], doubleBytes[5], doubleBytes[6], doubleBytes[7],
            0x2A.toByte(), 0xD7.toByte(), 0xB1.toByte(),               // TimestampScale ID
            0x83.toByte(),                                              // Length = 3
            0x0F.toByte(), 0x42.toByte(), 0x40.toByte()                 // 1,000,000 ns
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 0L)

        assertEquals(1_000_000L, index.timestampScaleNs)
        assertEquals(60_000_000L, index.parsedDurationUs)
    }

    @Test
    fun scanner_abortsLookingForTimestampIfSimpleBlockEncountered() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        val bytes = byteArrayOf(
            0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte(), // Cluster ID
            0x88.toByte(),                                              // VINT size = 8
            0xA3.toByte(),                                              // SimpleBlock without Timestamp!
            0xE7.toByte(),                                              // Timestamp ID
            0x82.toByte(),
            0x03.toByte(), 0xE8.toByte()
        )

        scanner.onBytesReceived(bytes, 0, bytes.size, 0L)

        assertEquals(0, index.size)
    }

    @Test
    fun scanner_abortsLookingForTimestampIfSearchWindowExceeds64Bytes() {
        val index = MatroskaClusterIndex()
        val scanner = EbmlClusterStreamScanner(index)

        val list = mutableListOf<Byte>()
        list.addAll(listOf(0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte(), 0x88.toByte()))
        // Add 66 invalid padding bytes
        repeat(66) { list.add(0x00.toByte()) }
        // Now timestamp
        list.addAll(listOf(0xE7.toByte(), 0x82.toByte(), 0x03.toByte(), 0xE8.toByte()))

        val bytes = list.toByteArray()
        scanner.onBytesReceived(bytes, 0, bytes.size, 0L)

        assertEquals(0, index.size)
    }
}
