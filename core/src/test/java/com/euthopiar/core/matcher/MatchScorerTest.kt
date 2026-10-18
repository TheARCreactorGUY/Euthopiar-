package com.euthopiar.core.matcher

import com.euthopiar.core.model.ContentType
import com.euthopiar.core.model.MatchHints
import com.euthopiar.core.model.MediaCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchScorerTest {

    @Test
    fun testExactMatchAutoAccepted() {
        val hints = MatchHints(
            tmdbId = 693134,
            imdbId = "tt15239678",
            type = ContentType.MOVIE,
            titles = setOf("Dune: Part Two", "Dune Part 2"),
            year = 2024,
            runtimeMin = 166
        )

        val candidate = MediaCard(
            id = "dune-2",
            title = "Dune: Part Two (2024)",
            posterUrl = "",
            type = ContentType.MOVIE,
            releaseYear = 2024
        )

        val score = MatchScorer.score(hints, candidate)
        val decision = MatchScorer.evaluate(hints, candidate)

        assertTrue("Expected score >= 0.92 for exact match, got $score", score >= 0.92)
        assertEquals(MatchScorer.Decision.AUTO_ACCEPT, decision)
    }

    @Test
    fun testSequelMismatchPenalized() {
        val hints = MatchHints(
            tmdbId = 438631,
            imdbId = "tt1160419",
            type = ContentType.MOVIE,
            titles = setOf("Dune"),
            year = 2021,
            runtimeMin = 155
        )

        val candidatePartTwo = MediaCard(
            id = "dune-part-2",
            title = "Dune: Part Two",
            posterUrl = "",
            type = ContentType.MOVIE,
            releaseYear = 2024
        )

        val score = MatchScorer.score(hints, candidatePartTwo)
        val decision = MatchScorer.evaluate(hints, candidatePartTwo)

        assertTrue("Expected sequel mismatch score to be low (< 0.75), got $score", score < 0.75)
        assertEquals(MatchScorer.Decision.REJECT, decision)
    }

    @Test
    fun testMediaTypeMismatchRejected() {
        val hints = MatchHints(
            tmdbId = 1234,
            imdbId = null,
            type = ContentType.TV_SERIES,
            titles = setOf("Halo"),
            year = 2022
        )

        val candidateMovie = MediaCard(
            id = "halo-movie",
            title = "Halo: The Fall of Reach",
            posterUrl = "",
            type = ContentType.MOVIE,
            releaseYear = 2022
        )

        val score = MatchScorer.score(hints, candidateMovie)
        val decision = MatchScorer.evaluate(hints, candidateMovie)

        assertTrue("Type mismatch must reject candidate, got $score", score < 0.75)
        assertEquals(MatchScorer.Decision.REJECT, decision)
    }

    @Test
    fun testNormalizationStripsPunctuationAndLeadingArticle() {
        val normalized = MatchScorer.normalize("The Lord of the Rings: The Fellowship of the Ring (1080p WEB-DL)")
        assertEquals("lord of rings fellowship of ring", normalized)
    }
}
