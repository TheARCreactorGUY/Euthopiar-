package com.euthopiar.core.util

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.DownloadOption
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.StreamEmission
import com.euthopiar.core.model.StreamResult
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.model.SubtitleTrack
import com.euthopiar.core.model.UniversalPlugin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamEmissionFlowTest {

    private class MockLegacyPlugin : UniversalPlugin {
        override val name: String = "Mock Legacy"
        override val mainUrl: String = "https://example.com"
        override val supportedTypes: Set<MediaType> = setOf(MediaType.MOVIE)

        override suspend fun getHomeCatalog(): List<CatalogRow> = emptyList()
        override suspend fun search(query: String): List<MediaItem> = emptyList()
        override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = throw UnsupportedOperationException()

        override suspend fun getStreamLinks(episodeData: String): StreamResult {
            return StreamResult(
                streams = listOf(
                    StreamSource(
                        url = "https://example.com/hls/master.m3u8",
                        serverName = "Alpha Server",
                        resolutionLabel = "1080p",
                        quality = "1080p",
                        isM3u8 = true,
                        releaseType = AudioReleaseType.SUB
                    ),
                    StreamSource(
                        url = "https://example.com/direct/720.mp4",
                        serverName = "Beta Mirror",
                        resolutionLabel = "720p",
                        quality = "720p",
                        isM3u8 = false,
                        releaseType = AudioReleaseType.DUB
                    )
                ),
                subtitles = listOf(
                    SubtitleTrack(url = "https://example.com/subs/en.vtt", language = "English"),
                    SubtitleTrack(url = "https://example.com/subs/es.srt", language = "Spanish")
                )
            )
        }

        override suspend fun getDownloadLinks(episodeData: String): List<DownloadOption> = emptyList()
    }

    private class MockProgressivePlugin : UniversalPlugin {
        override val name: String = "Mock Progressive"
        override val mainUrl: String = "https://example.com"
        override val supportedTypes: Set<MediaType> = setOf(MediaType.MOVIE)

        override suspend fun getHomeCatalog(): List<CatalogRow> = emptyList()
        override suspend fun search(query: String): List<MediaItem> = emptyList()
        override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = throw UnsupportedOperationException()
        override suspend fun getStreamLinks(episodeData: String): StreamResult = StreamResult(emptyList(), emptyList())
        override suspend fun getDownloadLinks(episodeData: String): List<DownloadOption> = emptyList()

        override fun getStreamFlow(episodeData: String): Flow<StreamEmission> = flow {
            emit(StreamEmission.StatusUpdate("FastMirror", "Querying provider..."))
            emit(StreamEmission.SourceFound(
                StreamSource(
                    url = "https://fast.com/live.m3u8",
                    serverName = "FastMirror",
                    resolutionLabel = "1080p",
                    isM3u8 = true
                )
            ))
            emit(StreamEmission.SubtitleFound(
                SubtitleTrack(url = "https://fast.com/fr.vtt", language = "French")
            ))
            emit(StreamEmission.StatusUpdate("SlowBackup", "Querying backup mirror..."))
            emit(StreamEmission.SourceFound(
                StreamSource(
                    url = "https://backup.com/720.mp4",
                    serverName = "SlowBackup",
                    resolutionLabel = "720p",
                    isM3u8 = false
                )
            ))
        }
    }

    @Test
    fun testDefaultGetStreamFlowEmitsAllStreamsAndSubtitles() = runBlocking {
        val plugin = MockLegacyPlugin()
        val emissions = plugin.getStreamFlow("test_ep_data").toList()

        assertEquals(4, emissions.size)
        assertTrue(emissions[0] is StreamEmission.SourceFound)
        assertEquals("Alpha Server", (emissions[0] as StreamEmission.SourceFound).source.serverName)
        assertEquals(AudioReleaseType.SUB, (emissions[0] as StreamEmission.SourceFound).source.releaseType)

        assertTrue(emissions[1] is StreamEmission.SourceFound)
        assertEquals("Beta Mirror", (emissions[1] as StreamEmission.SourceFound).source.serverName)
        assertEquals(AudioReleaseType.DUB, (emissions[1] as StreamEmission.SourceFound).source.releaseType)

        assertTrue(emissions[2] is StreamEmission.SubtitleFound)
        assertEquals("English", (emissions[2] as StreamEmission.SubtitleFound).track.language)

        assertTrue(emissions[3] is StreamEmission.SubtitleFound)
        assertEquals("Spanish", (emissions[3] as StreamEmission.SubtitleFound).track.language)
    }

    @Test
    fun testProgressiveStreamEmissionContract() = runBlocking {
        val plugin = MockProgressivePlugin()
        val emissions = plugin.getStreamFlow("test_ep_data").toList()

        assertEquals(5, emissions.size)
        assertTrue(emissions[0] is StreamEmission.StatusUpdate)
        assertEquals("Querying provider...", (emissions[0] as StreamEmission.StatusUpdate).message)

        assertTrue(emissions[1] is StreamEmission.SourceFound)
        assertEquals("FastMirror", (emissions[1] as StreamEmission.SourceFound).source.serverName)

        assertTrue(emissions[2] is StreamEmission.SubtitleFound)
        assertEquals("French", (emissions[2] as StreamEmission.SubtitleFound).track.language)

        assertTrue(emissions[3] is StreamEmission.StatusUpdate)
        assertTrue(emissions[4] is StreamEmission.SourceFound)
        assertEquals("SlowBackup", (emissions[4] as StreamEmission.SourceFound).source.serverName)
    }
}
