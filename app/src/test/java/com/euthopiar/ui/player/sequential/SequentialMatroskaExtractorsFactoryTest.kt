package com.euthopiar.ui.player.sequential

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@UnstableApi
class SequentialMatroskaExtractorsFactoryTest {

    @Test
    fun safeSeekMap_neverReturnsStartSeekPointForNonZeroTimestampsWhenCuesAbsent() {
        val context = RuntimeEnvironment.getApplication()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/movie.mkv",
            headers = emptyMap(),
            okHttpClient = OkHttpClient()
        )

        // Mock delegate SeekMap.Unseekable(durationUs)
        val durationUs = 60_000_000L // 60 seconds
        val unseekableDelegate = SeekMap.Unseekable(durationUs)

        // Verifying standard Media3 behavior: Unseekable returns START (00:00) on every seek!
        val standardPoints = unseekableDelegate.getSeekPoints(15_000_000L)
        assertEquals(0L, standardPoints.first.timeUs)
        assertEquals(0L, standardPoints.first.position)

        // Now wrap with SafeSeekMap
        val safeSeekMap = SafeSeekMap(unseekableDelegate, session)

        // For non-zero seek timestamp (e.g. 15s forward seek):
        val seekPoints = safeSeekMap.getSeekPoints(15_000_000L)
        val p1 = seekPoints.first

        // MUST NOT reset to 00:00
        assertNotEquals("SafeSeekMap must never return SeekPoint.START for non-zero timeUs", SeekPoint.START, p1)
        assertTrue("SafeSeekMap must preserve non-zero seek time or non-zero offset", p1.timeUs > 0L || p1.position > 0L)
        assertEquals(15_000_000L, p1.timeUs)

        // For 00:00 rewind, START is expected
        val zeroPoints = safeSeekMap.getSeekPoints(0L)
        assertEquals(0L, zeroPoints.first.timeUs)
        assertEquals(0L, zeroPoints.first.position)

        session.release()
    }

    @Test
    fun safeSeekMap_preservesDelegateSeekPointsWhenCuesAvailable() {
        val context = RuntimeEnvironment.getApplication()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/movie.mkv",
            headers = emptyMap(),
            okHttpClient = OkHttpClient()
        )

        val durationUs = 60_000_000L
        val cueSeekPoint = SeekPoint(30_000_000L, 50_000_000L)
        val seekableDelegate = object : SeekMap {
            override fun isSeekable(): Boolean = true
            override fun getDurationUs(): Long = durationUs
            override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints = SeekMap.SeekPoints(cueSeekPoint)
        }

        val safeSeekMap = SafeSeekMap(seekableDelegate, session)
        val result = safeSeekMap.getSeekPoints(30_000_000L)

        assertEquals(cueSeekPoint, result.first)

        session.release()
    }

    @Test
    fun dynamicClusterSeekMap_neverReturnsStart_whenDurationUnsetOrCorrupt() {
        val context = RuntimeEnvironment.getApplication()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/movie.mkv",
            headers = emptyMap(),
            okHttpClient = OkHttpClient()
        )

        try {
            val corruptSeekMap = DynamicClusterSeekMap(durationUs = C.TIME_UNSET, session = session)
            val seekPoints = corruptSeekMap.getSeekPoints(10_000_000L)
            assertNotEquals("Must never return SeekPoint.START", SeekPoint.START, seekPoints.first)
            assertTrue("Must preserve non-zero seek position/time", seekPoints.first.timeUs > 0L || seekPoints.first.position > 0L)
        } finally {
            session.release()
        }
    }

    @Test
    fun safeSeekMap_withUnsetDuration_neverReturnsStartSeekPoint() {
        val context = RuntimeEnvironment.getApplication()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/movie.mkv",
            headers = emptyMap(),
            okHttpClient = OkHttpClient()
        )

        try {
            val unseekable = SeekMap.Unseekable(C.TIME_UNSET)
            val safe = SafeSeekMap(unseekable, session)
            val points = safe.getSeekPoints(5_000_000L)
            assertNotEquals("SafeSeekMap must not return START for non-zero timeUs", SeekPoint.START, points.first)
            assertEquals(5_000_000L, points.first.timeUs)
            assertTrue(points.first.position > 0L)
        } finally {
            session.release()
        }
    }
}
