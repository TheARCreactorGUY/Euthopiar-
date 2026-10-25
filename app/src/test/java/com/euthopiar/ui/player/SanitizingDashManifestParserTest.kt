package com.euthopiar.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SanitizingDashManifestParserTest {

    @Test
    fun sanitizeXml_escapesRawAmpersandsInUrls() {
        val parser = SanitizingDashManifestParser()
        val rawMpd = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011">
                <BaseURL>https://cdn.example.com/stream?token=abc&session=123&exp=9999</BaseURL>
                <Representation codecs="hev1.1.6.L93.B0" />
            </MPD>
        """.trimIndent()

        val sanitized = parser.sanitizeXml(rawMpd)

        val expected = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011">
                <BaseURL>https://cdn.example.com/stream?token=abc&amp;session=123&amp;exp=9999</BaseURL>
                <Representation codecs="hvc1.1.6.L93.B0" />
            </MPD>
        """.trimIndent()

        assertEquals(expected, sanitized)
    }

    @Test
    fun sanitizeXml_preservesExistingXmlEntities() {
        val parser = SanitizingDashManifestParser()
        val rawMpd = """<SegmentURL media="chunk.m4s?a=1&amp;b=2&lt;&gt;&quot;&apos;&#38;&#x26;&raw=1"/>"""
        val sanitized = parser.sanitizeXml(rawMpd)
        val expected = """<SegmentURL media="chunk.m4s?a=1&amp;b=2&lt;&gt;&quot;&apos;&#38;&#x26;&amp;raw=1"/>"""
        assertEquals(expected, sanitized)
    }
}
