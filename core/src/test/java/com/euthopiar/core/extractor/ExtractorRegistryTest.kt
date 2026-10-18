package com.euthopiar.core.extractor

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.StreamSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExtractorRegistryTest {

    @Before
    fun setUp() {
        ExtractorRegistry.reset()
    }

    @Test
    fun testDefaultExtractorsRegistered() {
        val extractors = ExtractorRegistry.getAllExtractors()
        assertTrue("Default extractors should include StreamTape", extractors.any { it.name == "StreamTape" })
        assertTrue("Default extractors should include Filemoon", extractors.any { it.name == "Filemoon" })
    }

    @Test
    fun testRegisterAndResolveMockExtractor() = runBlocking {
        val mockExtractor = object : ExtractorApi {
            override val name: String = "MockHost"
            override val mainUrl: String = "https://mockhost.com"

            override fun isHostFor(url: String): Boolean = url.contains("mockhost.com")

            override suspend fun extract(
                embedUrl: String,
                referer: String?,
                headers: Map<String, String>
            ): List<StreamSource> {
                return listOf(
                    StreamSource(
                        url = "https://cdn.mockhost.com/video.mp4",
                        serverName = "MockHost",
                        resolutionLabel = "1080p",
                        quality = "MockHost (1080p)",
                        isM3u8 = false
                    )
                )
            }
        }

        ExtractorRegistry.register(mockExtractor)
        assertTrue(ExtractorRegistry.getAllExtractors().any { it.name == "MockHost" })

        val sources = ExtractorRegistry.resolveUrl("https://mockhost.com/e/12345")
        assertEquals(1, sources.size)
        assertEquals("https://cdn.mockhost.com/video.mp4", sources[0].url)
        assertEquals("MockHost", sources[0].serverName)
    }

    @Test
    fun testStreamTapeExtractionFromHtml() {
        val extractor = StreamTapeExtractor()
        assertTrue(extractor.isHostFor("https://streamtape.com/e/abc123xyz"))
        assertTrue(extractor.isHostFor("https://streamtape.to/v/abc123xyz"))

        val sampleHtml = """
            <html>
            <body>
            <script>
            document.getElementById('robotlink').innerHTML = '//streamtape.com/get_video?id=abc123xyz&token=' + ('xmySecretToken123').substring(1);
            </script>
            </body>
            </html>
        """.trimIndent()

        val sources = extractor.extractFromHtml(sampleHtml, "https://streamtape.com/e/abc123xyz")
        assertEquals(1, sources.size)
        val source = sources[0]
        assertEquals("https://streamtape.com/get_video?id=abc123xyz&token=mySecretToken123", source.url)
        assertEquals("StreamTape", source.serverName)
        assertEquals("1080p", source.resolutionLabel)
        assertFalse(source.isM3u8)
    }

    @Test
    fun testFilemoonExtractionFromPackedHtml() {
        val extractor = FilemoonExtractor()
        assertTrue(extractor.isHostFor("https://filemoon.sx/e/def456uvw"))
        assertTrue(extractor.isHostFor("https://filemoon.to/d/def456uvw"))

        // Packed JS simulating Filemoon m3u8 source
        val sampleHtml = """
            <html>
            <body>
            <script>
            eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c]);return p;}('{0: "1"}',2,2,'file|https://delivery.filemoon.sx/hls/master.m3u8'.split('|')))
            </script>
            </body>
            </html>
        """.trimIndent()

        val sources = extractor.extractFromHtml(sampleHtml, "https://filemoon.sx/e/def456uvw")
        assertEquals(1, sources.size)
        val source = sources[0]
        assertEquals("https://delivery.filemoon.sx/hls/master.m3u8", source.url)
        assertEquals("Filemoon", source.serverName)
        assertTrue(source.isM3u8)
    }

    @Test
    fun testUnrecognizedHostReturnsEmpty() = runBlocking {
        val sources = ExtractorRegistry.resolveUrl("https://unsupported-unknown-host.com/video")
        assertTrue("Unrecognized host should return empty list", sources.isEmpty())
    }
}
