package com.euthopiar.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun testParseSrtContent() {
        val srtSample = """
            1
            00:00:11,000 --> 00:00:13,000
            Translated by: [ Team Sub ]

            2
            00:00:21,400 --> 00:00:23,765
            Wanted!
            Reward of $50!

            3
            00:00:24,560 --> 00:00:29,360
            <i>Alright folks! This is a robbery!</i>
            Nobody move!
        """.trimIndent()

        val cues = SubtitleParser.parse(srtSample)
        assertEquals(3, cues.size)

        assertEquals(11000L, cues[0].startMs)
        assertEquals(13000L, cues[0].endMs)
        assertEquals("Translated by: [ Team Sub ]", cues[0].text)

        assertEquals(21400L, cues[1].startMs)
        assertEquals(23765L, cues[1].endMs)
        assertEquals("Wanted!\nReward of $50!", cues[1].text)

        assertEquals(24560L, cues[2].startMs)
        assertEquals(29360L, cues[2].endMs)
        assertEquals("Alright folks! This is a robbery!\nNobody move!", cues[2].text)
    }

    @Test
    fun testParseVttContent() {
        val vttSample = """
            WEBVTT

            00:01:05.120 --> 00:01:08.500 line:80%
            To infinity...

            00:01:09.000 --> 00:01:12.000
            ...and beyond!
        """.trimIndent()

        val cues = SubtitleParser.parse(vttSample)
        assertEquals(2, cues.size)

        assertEquals(65120L, cues[0].startMs)
        assertEquals(68500L, cues[0].endMs)
        assertEquals("To infinity...", cues[0].text)

        assertEquals(69000L, cues[1].startMs)
        assertEquals(72000L, cues[1].endMs)
        assertEquals("...and beyond!", cues[1].text)
    }
}
