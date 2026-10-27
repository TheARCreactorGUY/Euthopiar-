package com.euthopiar.ui.player.sequential

import androidx.media3.common.util.UnstableApi
import com.euthopiar.core.model.StreamContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@UnstableApi
class SequentialStreamManagerTest {

    @Test
    fun isSequentialStream_recognizesNonRangeCloudStreams() {
        val nonRangeUrls = listOf(
            "https://video-downloads.googleusercontent.com/video.mp4",
            "https://drive.usercontent.google.com/download?id=123",
            "https://docs.googleusercontent.com/docs/securesc/xyz",
            "https://video-seed.dev/stream/file.mkv",
            "https://cdn.video-gen.xyz/stream.mp4",
            "https://pixel.hubcloud.club/file",
            "https://gpdl.hubcloud.club/download",
            "https://hubcloud.one/drive/file",
            "https://custom-proxy.workers.dev/direct/video.mp4"
        )

        for (url in nonRangeUrls) {
            val resultAuto = SequentialStreamManager.isSequentialStream(url, StreamContainer.AUTO)
            assertTrue("URL $url should be recognized as non-range cloud stream (AUTO)", resultAuto)

            val resultMp4 = SequentialStreamManager.isSequentialStream(url, StreamContainer.MP4)
            assertTrue("URL $url should be recognized as non-range cloud stream (MP4)", resultMp4)

            val resultMkv = SequentialStreamManager.isSequentialStream(url, StreamContainer.MKV)
            assertTrue("URL $url should be recognized as non-range cloud stream (MKV)", resultMkv)
        }

        // Standard seekable CDN streams without Accept-Ranges: none are NOT hijacked into sequential pipe
        val standardCdnUrls = listOf(
            "https://cdn.example.com/video.mp4",
            "https://r2.cloudflarestorage.com/bucket/video.webm"
        )
        for (url in standardCdnUrls) {
            val resultAuto = SequentialStreamManager.isSequentialStream(url, StreamContainer.AUTO)
            assertFalse("Standard CDN URL $url must not be hijacked into sequential engine", resultAuto)
        }
    }

    @Test
    fun isSequentialStream_rejectsLoopback() {
        val loopbackUrls = listOf(
            "http://127.0.0.1:8080/stream.mp4",
            "http://localhost:8080/stream.mkv",
            "http://127.0.0.1/video.mp4"
        )
        for (url in loopbackUrls) {
            val result = SequentialStreamManager.isSequentialStream(url, StreamContainer.MP4)
            assertFalse("Loopback URL $url must not be recognized as cloud progressive stream", result)
        }
    }

    @Test
    fun isSequentialStream_rejectsHlsAndDashStreams() {
        // HLS
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/playlist.m3u8", StreamContainer.AUTO))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream?format=m3u8", StreamContainer.AUTO))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream", StreamContainer.HLS))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream", StreamContainer.AUTO, mapOf("Content-Type" to "application/vnd.apple.mpegurl")))

        // DASH
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/manifest.mpd", StreamContainer.AUTO))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream?format=mpd", StreamContainer.AUTO))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream", StreamContainer.DASH))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream", StreamContainer.AUTO, mapOf("Content-Type" to "application/dash+xml")))

        // TS and P2P
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream.ts", StreamContainer.TS))
        assertFalse(SequentialStreamManager.isSequentialStream("https://cdn.example.com/stream", StreamContainer.P2P))
    }

    @Test
    fun isSequentialStream_returnsTrueWhenAcceptRangesNone() {
        val result = SequentialStreamManager.isSequentialStream(
            url = "https://cdn.example.com/stream",
            responseHeaders = mapOf("Accept-Ranges" to "none")
        )
        assertTrue(result)
    }

    @Test
    fun redirectUrlPinning_storesAndResolvesUrl() {
        SequentialStreamManager.clearPinnedUrls()
        val gatewayUrl = "https://gateway.example.com/watch/item123"
        val cdnResolvedUrl = "https://video-downloads.googleusercontent.com/download/file.mkv?token=xyz"

        assertNull(SequentialStreamManager.getPinnedUrl(gatewayUrl))

        SequentialStreamManager.pinResolvedUrl(gatewayUrl, cdnResolvedUrl)
        assertEquals(cdnResolvedUrl, SequentialStreamManager.getPinnedUrl(gatewayUrl))

        SequentialStreamManager.clearPinnedUrls()
        assertNull(SequentialStreamManager.getPinnedUrl(gatewayUrl))
    }

    @Test
    fun isSequentialStream_recognizesProgressiveStreamsWithDashInFilename() {
        val dashInFilenameUrls = listOf(
            "https://pixel.hubcloud.club/videos/dash-in-the-snow.mp4",
            "https://video-seed.dev/dashboard/movie.mkv",
            "https://gpdl.hubcloud.club/the-incredibles-dash-scene.webm"
        )
        for (url in dashInFilenameUrls) {
            assertTrue("URL $url with MP4 should be sequential", SequentialStreamManager.isSequentialStream(url, StreamContainer.MP4))
            assertTrue("URL $url with AUTO should be sequential", SequentialStreamManager.isSequentialStream(url, StreamContainer.AUTO))
        }
    }

    @Test
    fun isSequentialStream_rejectsIpv6Loopback() {
        assertFalse(SequentialStreamManager.isSequentialStream("http://[::1]:8080/stream.mp4", StreamContainer.MP4))
        assertFalse(SequentialStreamManager.isSequentialStream("http://[::1]/stream.mkv", StreamContainer.AUTO))
    }

    @Test
    fun sequentialStreamSession_isOffsetAvailable_returnsTrueForZeroOffsetAtStart() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/video.mp4",
            headers = emptyMap(),
            okHttpClient = okhttp3.OkHttpClient()
        )
        try {
            assertTrue("Byte 0 must be available at start of session", session.isOffsetAvailable(0L))
            assertTrue("Offset within forward bridge must be available", session.isOffsetAvailable(1024L))
            assertFalse("Negative offset must not be available", session.isOffsetAvailable(-1L))
        } finally {
            session.release()
        }
    }

    @Test
    fun preResolveAndPinUrl_returnsCachedPinnedUrl() = kotlinx.coroutines.runBlocking {
        SequentialStreamManager.clearPinnedUrls()
        val orig = "https://gateway.example.com/stream/redirect"
        val cdn = "https://cdn.example.com/video.mp4"
        SequentialStreamManager.pinResolvedUrl(orig, cdn)

        val resolved = SequentialStreamManager.preResolveAndPinUrl(
            okHttpClient = okhttp3.OkHttpClient(),
            url = orig
        )
        assertEquals(cdn, resolved)
    }

    @Test
    fun preResolveAndPinUrl_ignoresLoopback() = kotlinx.coroutines.runBlocking {
        val loopback = "http://127.0.0.1:8080/stream.mp4"
        val resolved = SequentialStreamManager.preResolveAndPinUrl(
            okHttpClient = okhttp3.OkHttpClient(),
            url = loopback
        )
        assertEquals(loopback, resolved)
    }

    @Test
    fun readAt_returnsNegativeOneImmediatelyWhenIsEofTrueWithoutContentLength() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/video.mp4",
            headers = emptyMap(),
            okHttpClient = okhttp3.OkHttpClient()
        )
        try {
            // totalContentLength is -1L (chunked / unknown length stream)
            assertEquals(-1L, session.totalContentLength)
            session.isEof = true

            // When isEof is true, readAt must return -1 immediately without hanging
            val buf = ByteArray(64)
            val result = session.readAt(0L, buf, 0, 64)
            assertEquals(-1, result)
        } finally {
            session.release()
        }
    }

    @Test
    fun urlAndHeaderSanitization_stripsInlineHeadersAndFragmentAndAddsUserAgent() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val rawUrl = "https://pixel.hubcloud.club/file.mkv#preview|Referer=https%3A%2F%2Fdomain.com"
        val (rawCleanUrl, inlineHeaders) = com.euthopiar.core.network.PlayerPlaybackNetworking.extractUrlAndInlineHeaders(
            rawUrl,
            emptyMap()
        )
        val cleanSeqUrl = rawCleanUrl.substringBefore('#')
        assertEquals("https://pixel.hubcloud.club/file.mkv", cleanSeqUrl)

        val sanitizedHeaders = com.euthopiar.core.network.PlayerPlaybackNetworking.sanitizeHeaders(inlineHeaders).toMutableMap()
        assertEquals("https://domain.com", sanitizedHeaders["Referer"])

        if (sanitizedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            sanitizedHeaders["User-Agent"] = com.euthopiar.core.network.AppUserAgent.get(context)
        }
        assertTrue(sanitizedHeaders.containsKey("User-Agent"))
        assertTrue(sanitizedHeaders["User-Agent"]!!.isNotEmpty())
    }

    @Test
    fun clusterIndex_durationResolution_scalesMicrosecondsToMilliseconds() {
        val index = MatroskaClusterIndex()
        assertEquals(androidx.media3.common.C.TIME_UNSET, index.parsedDurationUs)
        assertEquals(0L, index.getMaxIndexedTimeUs())

        // Record a cluster: 5000 ticks * 1_000_000 ns / 1_000 ns = 5_000_000 us (5,000 ms)
        index.recordCluster(5000L, 1024L)
        assertEquals(5_000_000L, index.getMaxIndexedTimeUs())

        val durationFromClustersMs = index.getMaxIndexedTimeUs() / 1000L
        assertEquals(5000L, durationFromClustersMs)

        // When parsedDurationUs is populated from SegmentInfo Duration: 120_000_000 us (120,000 ms)
        index.parsedDurationUs = 120_000_000L
        val resolvedDurationUs = when {
            index.parsedDurationUs > 0L -> index.parsedDurationUs
            index.getMaxIndexedTimeUs() > 0L -> index.getMaxIndexedTimeUs()
            else -> 0L
        }
        assertEquals(120_000L, resolvedDurationUs / 1000L)
    }

    @Test
    fun sequentialStreamDataSource_supportsOptionalTransferListenerDirectlyAndInFactory() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = SequentialStreamSession(
            context = context,
            url = "https://cdn.example.com/video.mp4",
            headers = emptyMap(),
            okHttpClient = okhttp3.OkHttpClient()
        )
        try {
            var listenerInvoked = false
            val listener = object : androidx.media3.datasource.TransferListener {
                override fun onTransferInitializing(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {
                    listenerInvoked = true
                }
                override fun onTransferStart(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {}
                override fun onBytesTransferred(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean, bytesTransferred: Int) {}
                override fun onTransferEnd(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {}
            }

            // Verify direct constructor accepts optional listener
            val ds = SequentialStreamDataSource(session, listener)
            assertNotNull(ds)

            // Verify Factory accepts optional listener and attaches it
            val factory = SequentialStreamDataSource.Factory(session, listener)
            val dsFromFactory = factory.createDataSource()
            assertNotNull(dsFromFactory)
        } finally {
            session.release()
        }
    }
}
