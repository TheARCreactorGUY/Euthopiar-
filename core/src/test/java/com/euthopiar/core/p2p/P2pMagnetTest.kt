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
}
