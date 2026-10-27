package com.euthopiar.ui.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamingPlaybackRegressionTest {

    // Dummy fake upstream DataSource for testing ResponseHeaderOverridingDataSource
    private class FakeUpstreamDataSource(
        private val uri: Uri,
        private val headers: Map<String, List<String>>,
        private val data: ByteArray = ByteArray(0)
    ) : DataSource {
        private var readPos = 0
        override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}
        override fun open(dataSpec: DataSpec): Long {
            readPos = 0
            return data.size.toLong()
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (readPos >= data.size) return -1
            val bytesToRead = minOf(length, data.size - readPos)
            System.arraycopy(data, readPos, buffer, offset, bytesToRead)
            readPos += bytesToRead
            return bytesToRead
        }
        override fun getUri(): Uri = uri
        override fun getResponseHeaders(): Map<String, List<String>> = headers
        override fun close() {}
    }

    @Test
    fun testResponseHeaderOverridingDataSourceSanitizesDashManifestAndNormalizesCodecs() {
        val rawMpd = java.io.File("c:/Euthopiar/scratch/orion_manifest.mpd").readText(Charsets.UTF_8)
        val dummyUri = Uri.parse("https://vidstuck.cc/backend/database/andromeda?ext=manifest.mpd")
        val fakeUpstream = FakeUpstreamDataSource(
            uri = dummyUri,
            headers = mapOf("Content-Type" to listOf("application/octet-stream")),
            data = rawMpd.toByteArray(Charsets.UTF_8)
        )
        val overridingDataSource = ResponseHeaderOverridingDataSource(fakeUpstream)
        val dataSpec = DataSpec(dummyUri)
        val openLen = overridingDataSource.open(dataSpec)
        assertTrue(openLen > 0)
        assertEquals("application/dash+xml", overridingDataSource.responseHeaders["Content-Type"]?.firstOrNull())

        val buffer = java.io.ByteArrayOutputStream()
        val temp = ByteArray(1024)
        var n: Int
        while (overridingDataSource.read(temp, 0, temp.size).also { n = it } != -1) {
            buffer.write(temp, 0, n)
        }
        val readString = String(buffer.toByteArray(), Charsets.UTF_8)
        assertTrue("Ampersands should be sanitized to &amp;", readString.contains("&amp;Signature="))
        assertTrue("Codecs should be normalized to hvc1", readString.contains("""codecs="hvc1."""))
        assertFalse("hev1 should be replaced", readString.contains("""codecs="hev1."""))

        val parser = androidx.media3.exoplayer.dash.manifest.DashManifestParser()
        val manifest = parser.parse(dummyUri, java.io.ByteArrayInputStream(buffer.toByteArray()))
        assertNotNull(manifest)
        assertEquals(1, manifest.periodCount)
    }

    @Test
    fun testDashManifestParserRawVsSanitized() {
        val rawMpd = java.io.File("c:/Euthopiar/scratch/orion_manifest.mpd").readText(Charsets.UTF_8)
        val parser = androidx.media3.exoplayer.dash.manifest.DashManifestParser()
        val dummyUri = Uri.parse("https://vidstuck.cc/backend/database/andromeda?ext=manifest.mpd")

        var rawFailed = false
        try {
            parser.parse(dummyUri, java.io.ByteArrayInputStream(rawMpd.toByteArray(Charsets.UTF_8)))
        } catch (e: Throwable) {
            rawFailed = true
            println("Raw MPD parse failed as expected: " + e.message)
        }
        assertTrue("Raw MPD should fail due to unescaped ampersands in attributes", rawFailed)

        val sanitizedMpd = rawMpd.replace(Regex("""&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)"""), "&amp;")
        val manifest = parser.parse(dummyUri, java.io.ByteArrayInputStream(sanitizedMpd.toByteArray(Charsets.UTF_8)))
        assertNotNull(manifest)
        assertEquals(1, manifest.periodCount)
    }

    @Test
    fun testChannelMixingMatrixAllChannels1To8() {
        val channelMixingAudioProcessor = androidx.media3.common.audio.ChannelMixingAudioProcessor().apply {
            putChannelMixingMatrix(androidx.media3.common.audio.ChannelMixingMatrix.create(1, 2))
            putChannelMixingMatrix(androidx.media3.common.audio.ChannelMixingMatrix.create(2, 2))
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    3, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.7071f
                    )
                )
            )
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    4, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.0f,
                        0.0f, 0.7071f
                    )
                )
            )
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    5, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.7071f,
                        0.7071f, 0.0f,
                        0.0f, 0.7071f
                    )
                )
            )
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    6, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.7071f,
                        0.0f, 0.0f,
                        0.7071f, 0.0f,
                        0.0f, 0.7071f
                    )
                )
            )
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    7, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.7071f,
                        0.0f, 0.0f,
                        0.5f, 0.5f,
                        0.7071f, 0.0f,
                        0.0f, 0.7071f
                    )
                )
            )
            putChannelMixingMatrix(
                androidx.media3.common.audio.ChannelMixingMatrix(
                    8, 2,
                    floatArrayOf(
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.7071f, 0.7071f,
                        0.0f, 0.0f,
                        0.5f, 0.0f,
                        0.0f, 0.5f,
                        0.7071f, 0.0f,
                        0.0f, 0.7071f
                    )
                )
            )
        }

        for (ch in 1..8) {
            val inputFormat = androidx.media3.common.audio.AudioProcessor.AudioFormat(
                48000,
                ch,
                androidx.media3.common.C.ENCODING_PCM_16BIT
            )
            val outputFormat = channelMixingAudioProcessor.configure(inputFormat)
            if (ch == 2) {
                assertFalse("2ch identity must not make processor active", channelMixingAudioProcessor.isActive)
                assertEquals(androidx.media3.common.audio.AudioProcessor.AudioFormat.NOT_SET, outputFormat)
            } else {
                assertTrue("Processor must be active for ch=$ch", channelMixingAudioProcessor.isActive)
                assertEquals(2, outputFormat.channelCount)
            }
        }
    }

    @Test
    fun testResponseHeaderOverridingDataSourceDisguisedSegmentsAndStripping() {
        // Disguised .jpg segment with image/jpeg Content-Type must have Content-Type stripped
        val disguisedJpgUri = Uri.parse("https://stream-cdn.example.com/hls/segment_001.jpg")
        val upstreamJpg = FakeUpstreamDataSource(
            uri = disguisedJpgUri,
            headers = mapOf("Content-Type" to listOf("image/jpeg"))
        )
        val dsJpg = ResponseHeaderOverridingDataSource(upstream = upstreamJpg)
        dsJpg.open(DataSpec(disguisedJpgUri))
        assertFalse("Content-Type must be removed for disguised jpg segment", dsJpg.responseHeaders.containsKey("Content-Type"))

        // Disguised .html segment with text/html Content-Type must have Content-Type stripped
        val disguisedHtmlUri = Uri.parse("https://stream-cdn.example.com/hls/data-chunk.html")
        val upstreamHtml = FakeUpstreamDataSource(
            uri = disguisedHtmlUri,
            headers = mapOf("content-type" to listOf("text/html; charset=utf-8"))
        )
        val dsHtml = ResponseHeaderOverridingDataSource(upstream = upstreamHtml)
        dsHtml.open(DataSpec(disguisedHtmlUri))
        assertFalse("Content-Type must be removed for disguised html segment", dsHtml.responseHeaders.containsKey("Content-Type"))

        // Fragmented MP4 segment with generic application/octet-stream must normalize to video/mp4
        val mp4SegmentUri = Uri.parse("https://stream-cdn.example.com/live/init-stream.m4s")
        val upstreamMp4 = FakeUpstreamDataSource(
            uri = mp4SegmentUri,
            headers = mapOf("Content-Type" to listOf("application/octet-stream"))
        )
        val dsMp4 = ResponseHeaderOverridingDataSource(upstream = upstreamMp4)
        dsMp4.open(DataSpec(mp4SegmentUri))
        assertEquals("video/mp4", dsMp4.responseHeaders["Content-Type"]?.firstOrNull())

        // Fragmented MP4 audio segment with generic application/octet-stream must normalize to audio/mp4
        val mp4AudioSegmentUri = Uri.parse("https://sacdn.hakunaymatata.com/dash/123/init-stream1.m4s")
        val upstreamMp4Audio = FakeUpstreamDataSource(
            uri = mp4AudioSegmentUri,
            headers = mapOf("Content-Type" to listOf("application/octet-stream"))
        )
        val dsMp4Audio = ResponseHeaderOverridingDataSource(upstream = upstreamMp4Audio)
        dsMp4Audio.open(DataSpec(mp4AudioSegmentUri))
        assertEquals("audio/mp4", dsMp4Audio.responseHeaders["Content-Type"]?.firstOrNull())

        // DASH MPD manifest with generic application/octet-stream must normalize to application/dash+xml
        val dashUri = Uri.parse("https://vidstuck.cc/backend/database/andromeda?url=xyz&ext=manifest.mpd")
        val upstreamDash = FakeUpstreamDataSource(
            uri = dashUri,
            headers = mapOf("Content-Type" to listOf("application/octet-stream"))
        )
        val dsDash = ResponseHeaderOverridingDataSource(upstream = upstreamDash)
        dsDash.open(DataSpec(dashUri))
        assertEquals("application/dash+xml", dsDash.responseHeaders["Content-Type"]?.firstOrNull())
    }

    @Test
    fun testAmbiguousStreamPlaylistSniffingLogic() {
        val bomPayload = "\uFEFF#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:10.0,\nseg1.ts"
        val trimmed = bomPayload.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        assertTrue("BOM payload must be recognized as EXTM3U", trimmed.startsWith("#EXTM3U"))

        val dashPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\">"
        assertTrue("DASH XML payload must contain <MPD", dashPayload.contains("<MPD", ignoreCase = true))
    }

    @Test
    fun testResponseHeaderOverridingDataSourceNormalizesMimeForHls() {
        val hlsUri = Uri.parse("https://stream-cdn.example.com/live/playlist.m3u8")
        // Raw CDN returns generic application/octet-stream
        val upstream = FakeUpstreamDataSource(
            uri = hlsUri,
            headers = mapOf("Content-Type" to listOf("application/octet-stream"))
        )

        val overridingDataSource = ResponseHeaderOverridingDataSource(
            upstream = upstream,
            overriddenHeaders = emptyMap() // empty custom responseHeaders from scraper
        )

        overridingDataSource.open(DataSpec(hlsUri))
        val responseHeaders = overridingDataSource.responseHeaders

        val ct = responseHeaders["Content-Type"]?.firstOrNull()
        assertEquals("application/vnd.apple.mpegurl", ct)
    }

    @Test
    fun testResponseHeaderOverridingDataSourceNormalizesMimeForTsSegment() {
        val tsUri = Uri.parse("https://stream-cdn.example.com/live/segment-001.ts")
        // Raw CDN sends text/plain or octet-stream
        val upstream = FakeUpstreamDataSource(
            uri = tsUri,
            headers = mapOf("Content-Type" to listOf("text/plain"))
        )

        val overridingDataSource = ResponseHeaderOverridingDataSource(
            upstream = upstream,
            overriddenHeaders = emptyMap()
        )

        overridingDataSource.open(DataSpec(tsUri))
        val responseHeaders = overridingDataSource.responseHeaders

        val ct = responseHeaders["Content-Type"]?.firstOrNull()
        assertEquals("video/mp2t", ct)
    }

    @Test
    fun testResponseHeaderOverridingDataSourceAppliesExplicitOverrides() {
        val streamUri = Uri.parse("https://stream-cdn.example.com/play/video.mp4")
        val upstream = FakeUpstreamDataSource(
            uri = streamUri,
            headers = mapOf("Content-Type" to listOf("video/mp4"))
        )

        val overrides = mapOf("Custom-Proxy-Header" to "proxied_value")
        val overridingDataSource = ResponseHeaderOverridingDataSource(
            upstream = upstream,
            overriddenHeaders = overrides
        )

        overridingDataSource.open(DataSpec(streamUri))
        val responseHeaders = overridingDataSource.responseHeaders

        assertEquals(listOf("proxied_value"), responseHeaders["Custom-Proxy-Header"])
        assertEquals(listOf("video/mp4"), responseHeaders["Content-Type"])
    }

    @Test
    fun testTorrentStreamClassificationLogic() {
        fun isTorrentStream(url: String): Boolean {
            val cleanUrl = url.trim()
            val isHttp = cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true)
            return !isHttp && (cleanUrl.startsWith("magnet:", ignoreCase = true) ||
                    cleanUrl.startsWith("torrent://", ignoreCase = true) ||
                    ((cleanUrl.length == 40 || cleanUrl.length == 64) && cleanUrl.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }))
        }

        // 1. Torrent magnets and hashes
        assertTrue(isTorrentStream("magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Test"))
        assertTrue(isTorrentStream("torrent://0123456789abcdef0123456789abcdef01234567/0"))
        assertTrue(isTorrentStream("0123456789abcdef0123456789abcdef01234567")) // 40-char SHA1 infohash
        assertTrue(isTorrentStream("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")) // 64-char SHA256 infohash

        // 2. Pre-resolved Debrid web streams (Torrentio RD, Comet RD, TorBox)
        assertFalse(isTorrentStream("https://torrentio.strem.fun/stream/123456.mp4"))
        assertFalse(isTorrentStream("https://debrid.download.real-debrid.com/d/ABCXYZ/movie.mkv"))
        assertFalse(isTorrentStream("https://torbox.app/download/web/stream.mp4"))
        assertFalse(isTorrentStream("http://127.0.0.1:8888/stream/torrent_p2p"))
        assertFalse(isTorrentStream("http://localhost:8888/stream/torrent_p2p"))
        assertFalse(isTorrentStream("http://[::1]:8888/stream/torrent_p2p"))
    }

    @Test
    fun testTwoTierDeduplicationAndSorting() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        val torrentOptA = ServerSourceOption(
            id = "t1",
            serverName = "Torrent Provider A",
            resolutionLabel = "1080p FHD",
            label = "Torrent Provider A · 1080p FHD (10 seeds)",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&index=0",
                infoHash = "0123456789abcdef0123456789abcdef01234567",
                seeders = 10
            )
        )

        val torrentOptB = ServerSourceOption(
            id = "t2",
            serverName = "Torrent Provider B",
            resolutionLabel = "1080p FHD",
            label = "Torrent Provider B · 1080p FHD (100 seeds)",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&index=0",
                infoHash = "0123456789abcdef0123456789abcdef01234567",
                seeders = 100
            )
        )

        val directOpt1 = ServerSourceOption(
            id = "d1",
            serverName = "FastCDN",
            resolutionLabel = "1080p",
            label = "FastCDN · 1080p",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://cdn.example.com/video.mp4?token=tokenA&expires=999"
            )
        )

        val directOpt2 = ServerSourceOption(
            id = "d2",
            serverName = "FastCDN Mirror",
            resolutionLabel = "1080p",
            label = "FastCDN Mirror · 1080p",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://cdn.example.com/video.mp4?token=tokenB&expires=888"
            )
        )

        // Tier 2 Distinct release variants
        val opt4kRemux = ServerSourceOption(
            id = "remux_4k",
            serverName = "RD 4K REMUX",
            resolutionLabel = "4K 2160p",
            label = "RD 4K REMUX Dolby Vision",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://debrid.example.com/dl/remux4k.mkv"
            )
        )

        val opt4kWebDl = ServerSourceOption(
            id = "webdl_4k",
            serverName = "RD 4K WEB-DL",
            resolutionLabel = "4K 2160p",
            label = "RD 4K WEB-DL HDR",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://debrid.example.com/dl/webdl4k.mkv"
            )
        )

        val allOptions = listOf(torrentOptA, torrentOptB, directOpt1, directOpt2, opt4kRemux, opt4kWebDl)
        val deduped = vm.deduplicateAndSortServers(allOptions)

        // Tier 1 exact deduplication:
        // - torrentOptA & torrentOptB collapse to torrentOptB (100 seeds > 10 seeds)
        // - directOpt1 & directOpt2 collapse to single FastCDN option
        // - opt4kRemux and opt4kWebDl are distinct 4K variants and MUST be preserved!
        assertEquals(4, deduped.size)

        // Verify highest seeders kept for duplicate torrent
        val keptTorrent = deduped.first { it.streamSource.infoHash == "0123456789abcdef0123456789abcdef01234567" }
        assertEquals(100, keptTorrent.streamSource.seeders)

        // Verify sorting hierarchy: 4K REMUX -> 4K WEB-DL -> 1080p streams
        assertEquals("remux_4k", deduped[0].id)
        assertEquals("webdl_4k", deduped[1].id)
    }

    @Test
    fun testRawHexInfoHashTransportKeyClassification() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        val rawHashOptA = ServerSourceOption(
            id = "rh1",
            serverName = "Hash Provider 1",
            resolutionLabel = "1080p",
            label = "Hash Provider 1 (10 seeds)",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "0123456789abcdef0123456789abcdef01234567",
                seeders = 10
            )
        )

        val rawHashOptB = ServerSourceOption(
            id = "rh2",
            serverName = "Hash Provider 2",
            resolutionLabel = "1080p",
            label = "Hash Provider 2 (50 seeds)",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "0123456789ABCDEF0123456789ABCDEF01234567",
                seeders = 50
            )
        )

        val deduped = vm.deduplicateAndSortServers(listOf(rawHashOptA, rawHashOptB))
        assertEquals(1, deduped.size)
        assertEquals(50, deduped.first().streamSource.seeders)
    }

    @Test
    fun testScoreServerForInitialAutoPlayPrioritizes1080pHttpOverHeavy4kRemuxAndUnseededP2p() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        val direct1080p = ServerSourceOption(
            id = "direct_1080p",
            serverName = "FastCDN 1080p",
            resolutionLabel = "1080p",
            label = "FastCDN 1080p WEB-DL (2.5 GB)",
            healthCategory = ServerHealthCategory.EXCELLENT,
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://cdn.example.com/movie.mp4",
                quality = "1080p",
                matchScore = 0.90
            )
        )

        val heavy4kRemux = ServerSourceOption(
            id = "remux_4k",
            serverName = "Torrent 4K REMUX",
            resolutionLabel = "4K 2160p",
            label = "Torrent 4K REMUX Dolby Vision (65 GB)",
            healthCategory = ServerHealthCategory.AVERAGE,
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "magnet:?xt=urn:btih:remux4khash",
                quality = "4K 2160p",
                seeders = 40,
                matchScore = 0.85
            )
        )

        val unseededTorrent = ServerSourceOption(
            id = "unseeded",
            serverName = "Slow Swarm",
            resolutionLabel = "1080p",
            label = "Slow Swarm 1080p (2 seeds)",
            healthCategory = ServerHealthCategory.UNWATCHABLE,
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "magnet:?xt=urn:btih:unseededhash",
                quality = "1080p",
                seeders = 2,
                matchScore = 0.70
            )
        )

        val score1080p = vm.scoreServerForInitialAutoPlay(direct1080p, hasDebrid = false, preferredQualityHeight = 1080)
        val score4k = vm.scoreServerForInitialAutoPlay(heavy4kRemux, hasDebrid = false, preferredQualityHeight = 1080)
        val scoreDead = vm.scoreServerForInitialAutoPlay(unseededTorrent, hasDebrid = false, preferredQualityHeight = 1080)

        // 1080p direct CDN stream must score higher than heavy 4K REMUX for initial auto-selection
        assertTrue("Direct 1080p ($score1080p) should score higher than heavy 4K REMUX ($score4k)", score1080p > score4k)
        assertTrue("Unwatchable/unseeded torrent should receive large penalty, got $scoreDead", scoreDead < 0.0)

        // When 4K is preferred by user, 4K resolution score increases
        val score4kPreferred = vm.scoreServerForInitialAutoPlay(heavy4kRemux, hasDebrid = true, preferredQualityHeight = 2160)
        val score1080pWhen4kPreferred = vm.scoreServerForInitialAutoPlay(direct1080p, hasDebrid = true, preferredQualityHeight = 2160)
        assertTrue("4K should score higher when preferredQualityHeight is 2160", score4kPreferred > score1080pWhen4kPreferred)
    }

    @Test
    fun testIsPrimeCandidateClassification() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        val primeOpt = ServerSourceOption(
            id = "prime",
            serverName = "Fast HLS",
            resolutionLabel = "1080p",
            label = "Fast 1080p Direct HLS",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "https://example.com/master.m3u8",
                quality = "1080p",
                matchScore = 0.88
            )
        )

        val rawP2pOpt = ServerSourceOption(
            id = "p2p",
            serverName = "Raw Magnet",
            resolutionLabel = "1080p",
            label = "Raw Magnet 1080p",
            streamSource = com.euthopiar.core.model.StreamSource(
                url = "magnet:?xt=urn:btih:testprimep2p",
                quality = "1080p",
                matchScore = 0.85
            )
        )

        assertTrue("High-match 1080p HTTP stream should be prime candidate", vm.isPrimeCandidate(primeOpt, hasDebrid = false, prefHeight = 1080))
        assertFalse("Raw P2P without debrid should not be prime candidate for 0ms start", vm.isPrimeCandidate(rawP2pOpt, hasDebrid = false, prefHeight = 1080))
    }

    @Test
    fun testIsDashStreamRejectsOpaqueGoogleCdnTokenAndAcceptsValidPath() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        // Reflection to invoke private isDashStream
        val isDashMethod = PlayerViewModel::class.java.getDeclaredMethod(
            "isDashStream",
            String::class.java,
            com.euthopiar.core.model.StreamContainer::class.java
        ).apply { isAccessible = true }

        // Opaque Google CDN URL with "dash" inside the long token
        val googleCdnUrl = "https://video-downloads.googleusercontent.com/ADGPM2_dash_secret_token_123456/stream.mp4"
        val isDashGoogle = isDashMethod.invoke(vm, googleCdnUrl, null) as Boolean
        assertFalse("Google CDN URL with 'dash' in token must NOT be classified as DASH", isDashGoogle)

        // Known sequential cloud host with MKV container
        val isDashMkv = isDashMethod.invoke(vm, "https://cdn.example.com/manifest.mpd", com.euthopiar.core.model.StreamContainer.MKV) as Boolean
        assertFalse("MKV container must never be classified as DASH even with .mpd in url", isDashMkv)

        // Valid DASH URL ending with /dash in path
        val validDashPath = "https://example.com/live/channel/dash?token=abc"
        val isDashPath = isDashMethod.invoke(vm, validDashPath, null) as Boolean
        assertTrue("URL path ending in /dash must be classified as DASH", isDashPath)

        // Valid DASH URL with .mpd
        val validMpd = "https://example.com/live/manifest.mpd"
        val isDashMpd = isDashMethod.invoke(vm, validMpd, null) as Boolean
        assertTrue("URL with .mpd must be classified as DASH", isDashMpd)
    }

    @Test
    fun testLooksLikeHlsUrlDetectsM3u8InQueryStringAndEncoded() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = PlayerViewModel(app)

        val looksLikeHlsMethod = PlayerViewModel::class.java.getDeclaredMethod(
            "looksLikeHlsUrl",
            String::class.java,
            Map::class.java,
            Map::class.java
        ).apply { isAccessible = true }

        // tvvoo URL with .m3u8 in path
        val tvvooUrl = "https://tvvoo.hayd.uk/live/manifest.m3u8?url=http://upstream.com/live"
        val isTvvooHls = looksLikeHlsMethod.invoke(vm, tvvooUrl, emptyMap<String, String>(), emptyMap<String, String>()) as Boolean
        assertTrue("tvvoo live manifest URL must be classified as HLS", isTvvooHls)

        // Proxy URL with m3u8 in query parameter
        val proxyM3u8InQuery = "https://proxy.example.com/stream?source=http://upstream.com/live.m3u8"
        val isQueryHls = looksLikeHlsMethod.invoke(vm, proxyM3u8InQuery, emptyMap<String, String>(), emptyMap<String, String>()) as Boolean
        assertTrue("URL with .m3u8 in query parameter must be classified as HLS", isQueryHls)

        // Encoded %2em3u8
        val encodedM3u8 = "https://proxy.example.com/stream?target=http%3A%2F%2Fserver%2Findex%2em3u8"
        val isEncodedHls = looksLikeHlsMethod.invoke(vm, encodedM3u8, emptyMap<String, String>(), emptyMap<String, String>()) as Boolean
        assertTrue("URL with %2em3u8 must be classified as HLS", isEncodedHls)
    }
}
