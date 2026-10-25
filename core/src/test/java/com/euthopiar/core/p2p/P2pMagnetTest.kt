package com.euthopiar.core.p2p

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class P2pMagnetTest {

    @Test
    fun v1HashAndTrackersAreCanonicalEncodedAndDeduplicated() {
        val hash = "ABCDEF0123456789ABCDEF0123456789ABCDEF01"
        val tracker = "udp://tracker.example:80/announce?key=hello world"
        val magnet = buildP2pMagnetUri(hash, listOf("", tracker, " ", tracker))

        assertTrue(magnet.startsWith("magnet:?xt=urn:btih:${hash.lowercase()}"))
        assertEquals(1, magnet.windowed("&tr=".length).count { it == "&tr=" })
        assertTrue(magnet.endsWith("udp%3A%2F%2Ftracker.example%3A80%2Fannounce%3Fkey%3Dhello%20world"))
    }

    @Test
    fun v2HashUsesMultihashTopic() {
        val hash = "a".repeat(64)
        val magnet = buildP2pMagnetUri(hash, emptyList())
        assertTrue(magnet.contains("xt=urn:btmh:1220$hash"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHashIsRejectedBeforeNativeWork() {
        buildP2pMagnetUri("not-a-hash", emptyList())
    }

    @Test
    fun matchingUnsolicitedStopBecomesTerminalError() {
        val error = unexpectedStreamStopError(
            requestId = 0L,
            eventStreamId = "stream",
            currentStreamId = "stream",
            message = "stream expired after inactivity",
            fallbackMessage = "unknown",
        )

        assertEquals(
            P2pStreamingState.Error("stream expired after inactivity"),
            error,
        )
    }

    @Test
    fun explicitAndStaleStopsAreIgnored() {
        assertNull(
            unexpectedStreamStopError(
                requestId = 7L,
                eventStreamId = "stream",
                currentStreamId = "stream",
                message = "stopped",
                fallbackMessage = "unknown",
            )
        )
        assertNull(
            unexpectedStreamStopError(
                requestId = 0L,
                eventStreamId = "old-stream",
                currentStreamId = "stream",
                message = "stopped",
                fallbackMessage = "unknown",
            )
        )
    }

    @Test
    fun matchingUnsolicitedTorrentFailureBecomesTerminalError() {
        val error = unexpectedTorrentError(
            requestId = 0L,
            eventTorrentId = "torrent",
            currentTorrentId = "torrent",
            message = "file write failed",
            fallbackMessage = "unknown",
        )
        assertEquals(
            P2pStreamingState.Error("file write failed"),
            error,
        )
    }

    @Test
    fun nestedPayloadMovesToTheEnginePayloadDirectory() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val torrentId = "0123456789abcdef0123456789abcdef01234567"
            val legacy = File(cacheDirectory, "payload/payload/$torrentId").apply { mkdirs() }
            File(legacy, "video.mkv").writeText("cached")

            migrateNestedPayloadDirectory(cacheDirectory)

            assertEquals("cached", File(cacheDirectory, "payload/$torrentId/video.mkv").readText())
            assertFalse(File(cacheDirectory, "payload/payload").exists())
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun base32InfoHashDecodesCorrectlyToHex() {
        // 32 all-A's = 20 zero bytes = 40 zeros in hex
        val base32Zeros = "A".repeat(32)
        assertEquals("0".repeat(40), decodeBase32ToHex(base32Zeros))
        assertEquals("0".repeat(40), canonicalP2pInfoHash(base32Zeros))

        // Known Base32: JBSWY3DPEBLW64TMMQ====== (20 chars) -> "hello world"
        // 32 char Base32: "JBSWY3DPEBLW64TMMQQQ2222AAAA4444"
        val sample = "JBSWY3DPEBLW64TMMQQQ2222AAAA4444"
        val hex = decodeBase32ToHex(sample)
        assertNotNull(hex)
        assertEquals(40, hex!!.length)
        assertEquals(hex, canonicalP2pInfoHash(sample))
    }

    @Test
    fun extractP2pStreamRequestSupportsTorrentSchemeAndBase32() {
        val base32Hash = "A".repeat(32)
        val expectedHex = "0".repeat(40)

        // magnet with base32
        val magnetReq = extractP2pStreamRequest("magnet:?xt=urn:btih:$base32Hash&dn=Test+Movie&tr=udp://tracker.co")
        assertNotNull(magnetReq)
        assertEquals(expectedHex, magnetReq!!.infoHash)
        assertEquals("Test Movie", magnetReq.filename)
        assertEquals(listOf("udp://tracker.co"), magnetReq.trackers)

        // torrent: prefix without double slash
        val torrentReq1 = extractP2pStreamRequest("torrent:$expectedHex")
        assertNotNull(torrentReq1)
        assertEquals(expectedHex, torrentReq1!!.infoHash)

        // torrent:// prefix with file index
        val torrentReq2 = extractP2pStreamRequest("torrent://$expectedHex/2")
        assertNotNull(torrentReq2)
        assertEquals(expectedHex, torrentReq2!!.infoHash)
        assertEquals(2, torrentReq2.fileIdx)
    }

    @Test
    fun splitHorizonDnsResolvesLoopbackDirectly() {
        val dns = com.euthopiar.core.network.PlayerPlaybackNetworking.SplitHorizonDns()
        val addrs127 = dns.lookup("127.0.0.1")
        assertTrue(addrs127.isNotEmpty())
        assertTrue(addrs127.any { it.isLoopbackAddress })

        val addrsLocalhost = dns.lookup("localhost")
        assertTrue(addrsLocalhost.isNotEmpty())
        assertTrue(addrsLocalhost.any { it.isLoopbackAddress })
    }

    @Test
    fun extractP2pStreamRequestExtractsQueryParamsAndFallbacks() {
        val hex = "0".repeat(40)
        val req1 = extractP2pStreamRequest("magnet:?xt=urn:btih:$hex&fileidx=3&season=2&episode=5")
        assertNotNull(req1)
        assertEquals(3, req1!!.fileIdx)
        assertEquals(2, req1.season)
        assertEquals(5, req1.episode)

        val req2 = extractP2pStreamRequest("magnet:?xt=urn:btih:$hex&so=4")
        assertNotNull(req2)
        assertEquals(4, req2!!.fileIdx)

        val req3 = extractP2pStreamRequest(
            rawUrl = "magnet:?xt=urn:btih:$hex",
            defaultFileIdx = 1,
            defaultSeason = 3,
            defaultEpisode = 7
        )
        assertNotNull(req3)
        assertEquals(1, req3!!.fileIdx)
        assertEquals(3, req3.season)
        assertEquals(7, req3.episode)
    }

    @Test
    fun selectTargetFileIndexExcludesSampleAndPicksLargestMatchingEpisode() {
        val files = listOf(
            com.nuvio.engine.NuvioTorrentFile(0, 0L, 20_000_000L, "Show.S01E01.sample.mkv", false),
            com.nuvio.engine.NuvioTorrentFile(1, 0L, 1_500_000_000L, "Show.S01E01.1080p.mkv", false),
        )

        // Must pick index 1 (the 1.5GB episode), NOT index 0 (the 20MB sample)
        val selected = P2pStreamingEngine.selectTargetFileIndex(files, season = 1, episode = 1)
        assertEquals(1, selected)
    }

    @Test
    fun selectTargetFileIndexMatchesAnimeBatch3DigitEpisode() {
        val files = listOf(
            com.nuvio.engine.NuvioTorrentFile(0, 0L, 400_000_000L, "[Sub] Series - 011 [1080p].mkv", false),
            com.nuvio.engine.NuvioTorrentFile(1, 0L, 420_000_000L, "[Sub] Series - 012 [1080p].mkv", false),
            com.nuvio.engine.NuvioTorrentFile(2, 0L, 410_000_000L, "[Sub] Series - 013 [1080p].mkv", false),
        )

        val selected = P2pStreamingEngine.selectTargetFileIndex(files, season = null, episode = 12)
        assertEquals(1, selected)
    }

    @Test
    fun extractUrlAndInlineHeadersDeduplicatesCaseInsensitively() {
        val raw = "https://example.com/master.m3u8|Referer=https://foo.com&user-agent=InlineAgent&origin=https://origin.com"
        val existing = mapOf("User-Agent" to "DefaultAgent", "cookie" to "sid=123")
        val (cleanUrl, headers) = com.euthopiar.core.network.PlayerPlaybackNetworking.extractUrlAndInlineHeaders(raw, existing)

        assertEquals("https://example.com/master.m3u8", cleanUrl)
        assertEquals("InlineAgent", headers["User-Agent"])
        assertNull(headers["user-agent"])
        assertEquals("https://foo.com", headers["Referer"])
        assertEquals("https://origin.com", headers["Origin"])
        assertEquals("sid=123", headers["Cookie"])
    }

    @Test
    fun extractP2pStreamRequestSupportsFallbackInfoHash() {
        val hex = "a".repeat(40)
        val reqFromNull = extractP2pStreamRequest(
            rawUrl = null,
            fallbackInfoHash = hex,
            defaultFileIdx = 2,
            defaultSeason = 1,
            defaultEpisode = 4
        )
        assertNotNull(reqFromNull)
        assertEquals(hex, reqFromNull!!.infoHash)
        assertEquals(2, reqFromNull.fileIdx)
        assertEquals(1, reqFromNull.season)
        assertEquals(4, reqFromNull.episode)

        val reqFromInvalidUrl = extractP2pStreamRequest(
            rawUrl = "not-a-magnet-url",
            fallbackInfoHash = hex
        )
        assertNotNull(reqFromInvalidUrl)
        assertEquals(hex, reqFromInvalidUrl!!.infoHash)
    }
}

