package com.euthopiar.ui.player.subtitles

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssSubtitleParserTest {

    @Test
    fun testIsAssDetection() {
        val assContent = """
            [Script Info]
            Title: Sample Anime Episode
            ScriptType: v4.00+
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """.trimIndent()

        assertTrue(AssSubtitleParser.isAss(assContent))
    }

    @Test
    fun testParseAssDialogueAndAlignments() {
        val sample = """
            [Script Info]
            ScriptType: v4.00+
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:01:10.50,0:01:14.20,Default,,0,0,0,,{\an8}ATTACK ON TITAN: THE FINAL SEASON
            Dialogue: 0,0:01:15.00,0:01:18.45,Default,,0,0,0,,{\an7}Sign: District Hospital
            Dialogue: 0,0:01:20.10,0:01:23.80,Default,,0,0,0,,{\b1}Eren:{\b0} I have to keep moving forward.\NUntil my enemies are destroyed.
        """.trimIndent()

        val cues = AssSubtitleParser.parse(sample)
        assertEquals(3, cues.size)

        // Cue 1: TopCenter sign
        assertEquals(70500L, cues[0].startMs)
        assertEquals(74200L, cues[0].endMs)
        assertEquals("ATTACK ON TITAN: THE FINAL SEASON", cues[0].text)
        assertEquals(Alignment.TopCenter, cues[0].alignment)

        // Cue 2: TopStart sign
        assertEquals(75000L, cues[1].startMs)
        assertEquals(78450L, cues[1].endMs)
        assertEquals("Sign: District Hospital", cues[1].text)
        assertEquals(Alignment.TopStart, cues[1].alignment)

        // Cue 3: Standard dialogue with newline conversion and stripped bold tags
        assertEquals(80100L, cues[2].startMs)
        assertEquals(83800L, cues[2].endMs)
        assertEquals("Eren: I have to keep moving forward.\nUntil my enemies are destroyed.", cues[2].text)
        assertEquals(Alignment.BottomCenter, cues[2].alignment)
    }
}
