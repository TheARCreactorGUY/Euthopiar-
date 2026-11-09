package com.euthopiar.ui.player.sequential

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@UnstableApi
class LoopbackMatroskaExtractorsFactoryTest {

    @Test
    fun loopbackSafeSeekMap_neverReturnsStartForNonZeroSeekWhenCuesAbsent() {
        val durationUs = 7_200_000_000L // 2 hours in microseconds
        val totalBytes = 35_000_000_000L // 35 GB REMUX file
        val unseekableDelegate = SeekMap.Unseekable(durationUs)

        // Baseline Media3 behavior: Unseekable returns (0, 0) for ANY seek!
        val media3Points = unseekableDelegate.getSeekPoints(1_800_000_000L) // seek to 30 mins
        assertEquals(0L, media3Points.first.timeUs)
        assertEquals(0L, media3Points.first.position)

        // With LoopbackSafeSeekMap:
        val loopbackSeekMap = LoopbackSafeSeekMap(
            delegate = unseekableDelegate,
            totalContentLengthProvider = { totalBytes },
            durationMsProvider = { 7_200_000L }
        )

        assertTrue("isSeekable must be true", loopbackSeekMap.isSeekable)
        assertEquals(durationUs, loopbackSeekMap.durationUs)

        // Seek to 30 mins (1,800,000,000 us = 25% of movie)
        val seekTargetUs = 1_800_000_000L
        val points = loopbackSeekMap.getSeekPoints(seekTargetUs)
        val p1 = points.first

        // MUST NOT reset to 00:00!
        assertNotEquals("Must not return SeekPoint.START", SeekPoint.START, p1)
        assertEquals(seekTargetUs, p1.timeUs)

        // Offset should be approximately 25% of 35GB (8.75 GB)
        val expectedOffset = ((seekTargetUs.toDouble() / durationUs.toDouble()) * totalBytes).toLong()
        assertEquals(expectedOffset, p1.position)
        assertTrue("Estimated position must be non-zero", p1.position > 1_000_000_000L)
    }

    @Test
    fun loopbackSafeSeekMap_seekToZeroReturnsStart() {
        val unseekableDelegate = SeekMap.Unseekable(60_000_000L)
        val loopbackSeekMap = LoopbackSafeSeekMap(
            delegate = unseekableDelegate,
            totalContentLengthProvider = { 1_000_000_000L },
            durationMsProvider = { 60_000L }
        )

        val points = loopbackSeekMap.getSeekPoints(0L)
        assertEquals(0L, points.first.timeUs)
        assertEquals(0L, points.first.position)
    }

    @Test
    fun loopbackSafeSeekMap_delegatesValidSeekPointsWhenPresent() {
        val validSeekPoint = SeekPoint(10_000_000L, 50_000_000L)
        val validDelegate = object : SeekMap {
            override fun isSeekable(): Boolean = true
            override fun getDurationUs(): Long = 60_000_000L
            override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints =
                SeekMap.SeekPoints(validSeekPoint)
        }

        val loopbackSeekMap = LoopbackSafeSeekMap(
            delegate = validDelegate,
            totalContentLengthProvider = { 1_000_000_000L },
            durationMsProvider = { 60_000L }
        )

        val points = loopbackSeekMap.getSeekPoints(10_000_000L)
        assertEquals(validSeekPoint, points.first)
    }

    @Test
    fun loopbackMatroskaExtractorsFactory_createsExtractors() {
        val factory = LoopbackMatroskaExtractorsFactory(
            totalContentLengthProvider = { 20_000_000_000L },
            durationMsProvider = { 3_600_000L }
        )

        val extractors = factory.createExtractors()
        assertTrue("Extractors array should not be empty", extractors.isNotEmpty())
    }
}
