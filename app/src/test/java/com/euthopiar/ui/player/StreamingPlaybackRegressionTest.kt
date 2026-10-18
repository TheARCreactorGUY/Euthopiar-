package com.euthopiar.ui.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        private val headers: Map<String, List<String>>
    ) : DataSource {
        override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}
        override fun open(dataSpec: DataSpec): Long = 100L
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
        override fun getUri(): Uri = uri
        override fun getResponseHeaders(): Map<String, List<String>> = headers
        override fun close() {}
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
}
