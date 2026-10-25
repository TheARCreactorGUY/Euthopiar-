package com.euthopiar.ui.player.sequential

import androidx.media3.common.util.UnstableApi
import com.euthopiar.core.model.StreamContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun isSequentialStream_recognizesRemoteProgressiveStreams() {
        val progressiveUrls = listOf(
            "https://cdn.example.com/video.mp4",
            "https://video-downloads.googleusercontent.com/video.mp4",
            "https://video-seed.dev/stream/file.mkv",
            "https://cdn.video-gen.xyz/stream.mp4",
            "https://pixel.hubcloud.club/file",
            "https://gpdl.hubcloud.club/download",
            "https://r2.cloudflarestorage.com/bucket/video.webm"
        )

        for (url in progressiveUrls) {
            val resultAuto = SequentialStreamManager.isSequentialStream(url, StreamContainer.AUTO)
            assertTrue("URL $url should be recognized as cloud progressive stream (AUTO)", resultAuto)

            val resultMp4 = SequentialStreamManager.isSequentialStream(url, StreamContainer.MP4)
            assertTrue("URL $url should be recognized as cloud progressive stream (MP4)", resultMp4)

            val resultMkv = SequentialStreamManager.isSequentialStream(url, StreamContainer.MKV)
            assertTrue("URL $url should be recognized as cloud progressive stream (MKV)", resultMkv)

            val resultWebm = SequentialStreamManager.isSequentialStream(url, StreamContainer.WEBM)
            assertTrue("URL $url should be recognized as cloud progressive stream (WEBM)", resultWebm)
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
            "https://cdn.example.com/videos/dash-in-the-snow.mp4",
            "https://seedbox.org/dashboard/movie.mkv",
            "https://storage.provider.com/the-incredibles-dash-scene.webm"
        )
        for (url in dashInFilenameUrls) {
            assertTrue("URL $url with MP4 should be progressive", SequentialStreamManager.isSequentialStream(url, StreamContainer.MP4))
            assertTrue("URL $url with AUTO should be progressive", SequentialStreamManager.isSequentialStream(url, StreamContainer.AUTO))
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
}
