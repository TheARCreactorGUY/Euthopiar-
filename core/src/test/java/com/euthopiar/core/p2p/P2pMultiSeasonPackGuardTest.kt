package com.euthopiar.core.p2p

import com.nuvio.engine.NuvioTorrentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class P2pMultiSeasonPackGuardTest {

    private fun mockFile(index: Int, path: String, bytes: Long = 1_000_000_000L): NuvioTorrentFile {
        return NuvioTorrentFile(
            index = index,
            offset = 0L,
            size = bytes,
            path = path,
            pathTruncated = false
        )
    }

    @Test
    fun tier2_rejectsFilesFromDifferentSeasonInMultiSeasonPack() {
        val files = listOf(
            mockFile(0, "Complete Series/Season 02/Episode 03 - The Betrayal.mkv", bytes = 2_000_000_000L),
            mockFile(1, "Complete Series/Season 03/Episode 03 - The Return.mkv", bytes = 2_000_000_000L),
            mockFile(2, "Complete Series/Season 01/Episode 03 - The Beginning.mkv", bytes = 1_000_000_000L)
        )

        // Request Season 1 Episode 3
        val selectedIdx = P2pStreamingEngine.selectTargetFileIndex(files, season = 1, episode = 3)
        assertEquals("Should select Season 1 file, not Season 2 or Season 3 despite being smaller", 2, selectedIdx)
    }

    @Test
    fun tier2_rejectsFilesFromDifferentSeasonEvenIfLargerInMultiSeasonPack() {
        val files = listOf(
            mockFile(0, "Complete Series/Season 02/Ep 03 - The Betrayal.mkv", bytes = 2_000_000_000L),
            mockFile(1, "Complete Series/Season 01/Ep 03 - The Beginning.mkv", bytes = 1_000_000_000L)
        )

        // Request Season 1 Episode 3
        val selectedIdx = P2pStreamingEngine.selectTargetFileIndex(files, season = 1, episode = 3)
        assertEquals("Should select Season 1 file despite Season 2 being larger", 1, selectedIdx)
    }

    @Test
    fun tier2_acceptsFileWhenNoExplicitSeasonTokenPresent() {
        // Standalone anime or mini-series pack without explicit 'season' token in file path
        val files = listOf(
            mockFile(0, "Anime.Title.[01].mkv"),
            mockFile(1, "Anime.Title.[02].mkv"),
            mockFile(2, "Anime.Title.[03].mkv")
        )

        val selectedIdx = P2pStreamingEngine.selectTargetFileIndex(files, season = 1, episode = 2)
        assertEquals("Should match Episode 2 when no contradictory season is present", 1, selectedIdx)
    }

    @Test
    fun tier2_rejectsS01E05WhenSearchingForS02E05InEpisodeFallback() {
        val files = listOf(
            mockFile(0, "MultiSeasonPack/Show.S01E05.1080p.mkv", bytes = 2_000_000_000L),
            mockFile(1, "MultiSeasonPack/Show - Episode 05.mkv", bytes = 1_000_000_000L)
        )
        // Request Season 2 Episode 5
        val selectedIdx = P2pStreamingEngine.selectTargetFileIndex(files, season = 2, episode = 5)
        assertEquals("Must reject S01E05 even if larger when requesting Season 2", 1, selectedIdx)
    }

    @Test
    fun tier2_preservesFileInMultiSeasonFolderWhenTargetSeasonIsMentioned() {
        val files = listOf(
            mockFile(0, "Show S01-S02/Season 2/Ep 05.mkv", bytes = 1_000_000_000L),
            mockFile(1, "Show S01-S02/Season 1/Ep 05.mkv", bytes = 2_000_000_000L)
        )
        // Request Season 2 Episode 5
        val selectedIdx = P2pStreamingEngine.selectTargetFileIndex(files, season = 2, episode = 5)
        assertEquals("Must match Season 2 file inside S01-S02 multi-pack", 0, selectedIdx)
    }

    @Test
    fun extractP2pStreamRequest_mergesUrlAndExplicitTrackersDeduplicated() {
        val magnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337"
        val explicitTrackers = listOf(
            "udp://tracker.opentrackr.org:1337", // duplicate from url
            "udp://open.stealth.si:80/announce",
            "udp://tracker.torrent.eu.org:451/announce"
        )

        val request = extractP2pStreamRequest(
            rawUrl = magnet,
            trackers = explicitTrackers
        )

        assertNotNull(request)
        val trackers = request!!.trackers
        assertEquals("Should contain 4 unique trackers", 4, trackers.size)
        assertTrue(trackers.contains("udp://tracker.coppersurfer.tk:6969/announce"))
        assertTrue(trackers.contains("udp://tracker.opentrackr.org:1337"))
        assertTrue(trackers.contains("udp://open.stealth.si:80/announce"))
        assertTrue(trackers.contains("udp://tracker.torrent.eu.org:451/announce"))
    }
}
