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

    @Test
    fun testStreamMovieYearMismatchRejected() {
        val movieHints = MatchHints(
            tmdbId = 438631,
            imdbId = "tt1160419",
            type = ContentType.MOVIE,
            titles = setOf("Dune"),
            year = 2021
        )
        val oldMovieStream = com.euthopiar.core.streams.StreamItem(
            name = "Dune 1984 1080p BluRay x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(movieHints, oldMovieStream)
        org.junit.Assert.assertFalse("Dune 1984 should be rejected for Dune 2021", result.isAccepted)
        assertEquals(0.0, result.score, 0.001)
    }

    @Test
    fun testStreamSequelMismatchRejected() {
        val movieHints = MatchHints(
            tmdbId = 438631,
            imdbId = "tt1160419",
            type = ContentType.MOVIE,
            titles = setOf("Dune"),
            year = 2021
        )
        val sequelStream = com.euthopiar.core.streams.StreamItem(
            name = "Dune.Part.Two.2024.2160p.WEB-DL",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(movieHints, sequelStream)
        org.junit.Assert.assertFalse("Dune Part Two should be rejected for Dune 2021", result.isAccepted)
    }

    @Test
    fun testStreamMovieWithTvMarkersRejected() {
        val movieHints = MatchHints(
            tmdbId = 27205,
            imdbId = "tt1375666",
            type = ContentType.MOVIE,
            titles = setOf("Inception"),
            year = 2010
        )
        val tvStream = com.euthopiar.core.streams.StreamItem(
            name = "Inception S01E01 1080p HDTV",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(movieHints, tvStream)
        org.junit.Assert.assertFalse("Movie stream with TV S01E01 marker must be rejected", result.isAccepted)
    }

    @Test
    fun testStreamTvEpisodeMatchAndMismatch() {
        val tvHints = MatchHints(
            tmdbId = 1399,
            imdbId = "tt0944947",
            type = ContentType.TV_SERIES,
            titles = setOf("Game of Thrones"),
            year = 2011,
            season = 2,
            episode = 3
        )
        val wrongEpStream = com.euthopiar.core.streams.StreamItem(
            name = "Game.of.Thrones.S02E01.1080p",
            addonName = "TestScraper",
            addonId = "test"
        )
        val wrongSeasonStream = com.euthopiar.core.streams.StreamItem(
            name = "Game.of.Thrones.S01E03.1080p",
            addonName = "TestScraper",
            addonId = "test"
        )
        val correctStream = com.euthopiar.core.streams.StreamItem(
            name = "Game.of.Thrones.S02E03.1080p.WEB-DL",
            addonName = "TestScraper",
            addonId = "test"
        )

        val wrongEpResult = MatchScorer.scoreStream(tvHints, wrongEpStream)
        val wrongSeasonResult = MatchScorer.scoreStream(tvHints, wrongSeasonStream)
        val correctResult = MatchScorer.scoreStream(tvHints, correctStream)

        org.junit.Assert.assertFalse("Wrong episode must be rejected", wrongEpResult.isAccepted)
        org.junit.Assert.assertFalse("Wrong season must be rejected", wrongSeasonResult.isAccepted)
        assertTrue("Correct episode must be accepted", correctResult.isAccepted)
        assertTrue("Exact match should have high confidence score >= 0.80, got ${correctResult.score}", correctResult.score >= 0.80)
    }

    @Test
    fun testStreamAnimeAbsoluteEpisodeMatch() {
        val animeHints = MatchHints(
            tmdbId = 85937,
            imdbId = "tt9335498",
            type = ContentType.ANIME,
            titles = setOf("Demon Slayer: Kimetsu no Yaiba", "Kimetsu no Yaiba"),
            year = 2019,
            season = 2,
            episode = 1,
            absoluteEpisode = 27
        )
        val absEpStream = com.euthopiar.core.streams.StreamItem(
            name = "Demon Slayer - 27 (1080p)",
            addonName = "AnimePahe",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(animeHints, absEpStream)
        assertTrue("Anime stream matching absolute episode must be accepted", result.isAccepted)
        assertTrue("Anime stream matching absolute episode should score >= 0.80, got ${result.score}", result.score >= 0.80)
    }

    @Test
    fun testStreamGenericServerNameAccepted() {
        val movieHints = MatchHints(
            tmdbId = 438631,
            imdbId = "tt1160419",
            type = ContentType.MOVIE,
            titles = setOf("Dune"),
            year = 2021
        )
        val genericStream1 = com.euthopiar.core.streams.StreamItem(
            name = "Server 1",
            quality = "1080p",
            addonName = "Vidmoly",
            addonId = "test"
        )
        val genericStream2 = com.euthopiar.core.streams.StreamItem(
            name = "Auto",
            addonName = "HLS Direct",
            addonId = "test"
        )

        val r1 = MatchScorer.scoreStream(movieHints, genericStream1)
        val r2 = MatchScorer.scoreStream(movieHints, genericStream2)

        assertTrue("Generic Server 1 should be accepted", r1.isAccepted)
        assertEquals(0.65, r1.score, 0.001)
        assertTrue("Generic Auto should be accepted", r2.isAccepted)
        assertEquals(0.65, r2.score, 0.001)
    }

    @Test
    fun testMovieWithYearInTitleAccepted() {
        val ww84Hints = MatchHints(
            tmdbId = 464052,
            imdbId = "tt7126958",
            type = ContentType.MOVIE,
            titles = setOf("Wonder Woman 1984"),
            year = 2020
        )
        val ww84Stream = com.euthopiar.core.streams.StreamItem(
            name = "Wonder.Woman.1984.2020.1080p.WEB-DL",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(ww84Hints, ww84Stream)
        assertTrue("Wonder Woman 1984 should be accepted and not treated as a year mismatch", result.isAccepted)
        assertTrue("Wonder Woman 1984 should score >= 0.80, got ${result.score}", result.score >= 0.80)

        val movie1917Hints = MatchHints(
            tmdbId = 530915,
            imdbId = "tt8579674",
            type = ContentType.MOVIE,
            titles = setOf("1917"),
            year = 2019
        )
        val stream1917 = com.euthopiar.core.streams.StreamItem(
            name = "1917.2019.1080p.BluRay.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result1917 = MatchScorer.scoreStream(movie1917Hints, stream1917)
        assertTrue("1917 should be accepted and not treated as a year mismatch", result1917.isAccepted)
        assertTrue("1917 should score >= 0.80, got ${result1917.score}", result1917.score >= 0.80)
    }

    @Test
    fun testGenericVideoHostEmbedsAccepted() {
        val hints = MatchHints(
            tmdbId = 693134,
            imdbId = "tt15239678",
            type = ContentType.MOVIE,
            titles = setOf("Dune: Part Two"),
            year = 2024
        )
        val vidmolyStream = com.euthopiar.core.streams.StreamItem(
            name = "Vidmoly - 1080p",
            addonName = "SuperStream",
            addonId = "superstream"
        )
        val streamtapeStream = com.euthopiar.core.streams.StreamItem(
            name = "Streamtape 720p",
            addonName = "VidSrc",
            addonId = "vidsrc"
        )
        val filemoonStream = com.euthopiar.core.streams.StreamItem(
            name = "Filemoon [1080p]",
            addonName = "FlixHQ",
            addonId = "flixhq"
        )

        val r1 = MatchScorer.scoreStream(hints, vidmolyStream)
        val r2 = MatchScorer.scoreStream(hints, streamtapeStream)
        val r3 = MatchScorer.scoreStream(hints, filemoonStream)

        assertTrue("Vidmoly - 1080p must pass through as generic stream", r1.isAccepted)
        assertEquals(0.65, r1.score, 0.001)
        assertTrue("Streamtape 720p must pass through as generic stream", r2.isAccepted)
        assertEquals(0.65, r2.score, 0.001)
        assertTrue("Filemoon [1080p] must pass through as generic stream", r3.isAccepted)
        assertEquals(0.65, r3.score, 0.001)
    }

    @Test
    fun testSquareBracketResolutionTagNotMisidentifiedAsEpisode() {
        val tvHints = MatchHints(
            tmdbId = 1399,
            imdbId = "tt0944947",
            type = ContentType.TV_SERIES,
            titles = setOf("Game of Thrones"),
            year = 2011,
            season = 1,
            episode = 1
        )
        val resInBracketsStream = com.euthopiar.core.streams.StreamItem(
            name = "Game.of.Thrones.S01E01.[720].HDTV",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(tvHints, resInBracketsStream)
        assertTrue("Square bracket [720] resolution tag must not be misidentified as episode 720", result.isAccepted)
        assertTrue("Exact S01E01 match should score >= 0.80, got ${result.score}", result.score >= 0.80)
    }

    @Test
    fun testSequelTargetRejectsPrequelStream() {
        val partTwoHints = MatchHints(
            tmdbId = 693134,
            imdbId = "tt15239678",
            type = ContentType.MOVIE,
            titles = setOf("Dune: Part Two", "Dune Part 2"),
            year = 2024
        )
        val prequelStream = com.euthopiar.core.streams.StreamItem(
            name = "Dune.2021.1080p.BluRay.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(partTwoHints, prequelStream)
        org.junit.Assert.assertFalse("Dune 2021 should be rejected when Dune Part Two is requested", result.isAccepted)
    }

    @Test
    fun testPollutedHostLabelsAccepted() {
        val hints = MatchHints(
            tmdbId = 12345,
            imdbId = "tt1234567",
            type = ContentType.MOVIE,
            titles = setOf("Deadpool & Wolverine"),
            year = 2024
        )
        val stream = com.euthopiar.core.streams.StreamItem(
            name = "HubCloud 10Gbps",
            description = "4KHDHub • 1080p • 2.4 GB",
            addonName = "4KHDHub",
            addonId = "hub"
        )
        val result = MatchScorer.scoreStream(hints, stream)
        org.junit.Assert.assertTrue("Polluted host label should be accepted", result.isAccepted)
        org.junit.Assert.assertTrue("Score should be >= 0.75, was ${result.score}", result.score >= 0.75)
    }

    @Test
    fun testTechnicalTokensDoNotTriggerSequelPenaltyOrTvMarkers() {
        val hints = MatchHints(
            tmdbId = 533535,
            imdbId = "tt6263850",
            type = ContentType.MOVIE,
            titles = setOf("Deadpool & Wolverine", "Deadpool 3"),
            year = 2024
        )
        val stream = com.euthopiar.core.streams.StreamItem(
            name = "Deadpool.and.Wolverine.2024.1080p.WEBRip.DDP5.1.Atmos.10bit.x265-FLUX",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(hints, stream)
        org.junit.Assert.assertTrue("Technical tokens like 5.1, 10bit, x265, 1080p should not reject stream", result.isAccepted)
        org.junit.Assert.assertTrue("Score should be >= 0.85, was ${result.score}", result.score >= 0.85)
    }

    @Test
    fun testMultiEpisodeSeasonPacksAcceptedForEpisodeTarget() {
        val hints = MatchHints(
            tmdbId = 94605,
            imdbId = "tt11126994",
            type = ContentType.TV_SERIES,
            titles = setOf("Arcane"),
            year = 2021,
            season = 1,
            episode = 5
        )
        val rangeStream = com.euthopiar.core.streams.StreamItem(
            name = "Arcane.S01E01-E10.1080p.NF.WEB-DL.DDP5.1.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val resultRange = MatchScorer.scoreStream(hints, rangeStream)
        org.junit.Assert.assertTrue("Multi-episode range S01E01-E10 should be accepted for Episode 5", resultRange.isAccepted)

        val batchStream = com.euthopiar.core.streams.StreamItem(
            name = "[Judas] Arcane [Batch] [01-09] [1080p][HEVC x265 10bit]",
            addonName = "TestScraper",
            addonId = "test"
        )
        val resultBatch = MatchScorer.scoreStream(hints, batchStream)
        org.junit.Assert.assertTrue("Batch range 01-09 should be accepted for Episode 5", resultBatch.isAccepted)

        val seasonCompleteStream = com.euthopiar.core.streams.StreamItem(
            name = "Arcane Season 1 Complete 1080p WEB-DL",
            addonName = "TestScraper",
            addonId = "test"
        )
        val resultComplete = MatchScorer.scoreStream(hints, seasonCompleteStream)
        org.junit.Assert.assertTrue("Season 1 Complete pack should be accepted for Episode 5", resultComplete.isAccepted)
    }

    @Test
    fun testAnimeRomajiEnglishAndSeasonSpecificSynonymsScoring() {
        val hints = MatchHints(
            tmdbId = 85937,
            imdbId = "tt9335498",
            type = ContentType.TV_SERIES,
            titles = setOf("Demon Slayer: Kimetsu no Yaiba"),
            year = 2019,
            season = 2,
            episode = 3,
            romajiTitle = "Kimetsu no Yaiba: Yuukaku-hen",
            englishTitle = "Demon Slayer: Kimetsu no Yaiba Entertainment District Arc",
            seasonSpecificTitles = mapOf(2 to listOf("Kimetsu no Yaiba: Yuukaku-hen", "Demon Slayer Season 2")),
            animeSynonyms = listOf("Kimetsu no Yaiba 2nd Season")
        )

        val romajiStream = com.euthopiar.core.streams.StreamItem(
            name = "[SubsPlease] Kimetsu no Yaiba: Yuukaku-hen - 03 (1080p) [98A7B2C1].mkv",
            addonName = "TestAnime",
            addonId = "anime"
        )
        val resultRomaji = MatchScorer.scoreStream(hints, romajiStream)
        org.junit.Assert.assertTrue("Romaji title stream should be accepted", resultRomaji.isAccepted)
        org.junit.Assert.assertTrue("Romaji title stream score should be >= 0.85, was ${resultRomaji.score}", resultRomaji.score >= 0.85)

        val synonymStream = com.euthopiar.core.streams.StreamItem(
            name = "[Erai-raws] Kimetsu no Yaiba 2nd Season - 03 [1080p][Multiple Subtitle].mkv",
            addonName = "TestAnime",
            addonId = "anime"
        )
        val resultSynonym = MatchScorer.scoreStream(hints, synonymStream)
        org.junit.Assert.assertTrue("Anime synonym stream should be accepted", resultSynonym.isAccepted)
        org.junit.Assert.assertTrue("Anime synonym stream score should be >= 0.85, was ${resultSynonym.score}", resultSynonym.score >= 0.85)
    }

    @Test
    fun testGenuineContradictionsRejected() {
        val tvHints = MatchHints(
            tmdbId = 94605,
            imdbId = "tt11126994",
            type = ContentType.TV_SERIES,
            titles = setOf("Arcane"),
            year = 2021,
            season = 1,
            episode = 5
        )
        val wrongEpStream = com.euthopiar.core.streams.StreamItem(
            name = "Arcane.S01E02.1080p.NF.WEB-DL.DDP5.1.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val resultEp = MatchScorer.scoreStream(tvHints, wrongEpStream)
        org.junit.Assert.assertFalse("S01E02 should be rejected when S01E05 is requested", resultEp.isAccepted)

        val movieHints = MatchHints(
            tmdbId = 533535,
            imdbId = "tt6263850",
            type = ContentType.MOVIE,
            titles = setOf("Deadpool & Wolverine"),
            year = 2024
        )
        val oldMovieStream = com.euthopiar.core.streams.StreamItem(
            name = "The.Martian.2015.1080p.BluRay.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val resultMovie = MatchScorer.scoreStream(movieHints, oldMovieStream)
        org.junit.Assert.assertFalse("Unrelated 2015 movie should be rejected for 2024 target", resultMovie.isAccepted)
    }

    @Test
    fun testUnrelatedSeasonPackMustBeRejected() {
        val hints = MatchHints(
            tmdbId = 94605,
            imdbId = "tt11126994",
            type = ContentType.TV_SERIES,
            titles = setOf("Arcane"),
            year = 2021,
            season = 1,
            episode = 5
        )
        val unrelatedPack = com.euthopiar.core.streams.StreamItem(
            name = "Friends Season 1 Complete 1080p WEB-DL",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(hints, unrelatedPack)
        org.junit.Assert.assertFalse("Unrelated show's season pack 'Friends Season 1 Complete' must be rejected for 'Arcane'", result.isAccepted)
    }

    @Test
    fun testOneYearDifferenceShouldNotBeFlaggedAsSequelMismatch() {
        val hints = MatchHints(
            tmdbId = 1726,
            imdbId = "tt0371746",
            type = ContentType.MOVIE,
            titles = setOf("Iron Man"),
            year = 2008
        )
        val stream = com.euthopiar.core.streams.StreamItem(
            name = "Iron.Man.2007.1080p.BluRay.x264",
            addonName = "TestScraper",
            addonId = "test"
        )
        val result = MatchScorer.scoreStream(hints, stream)
        org.junit.Assert.assertTrue("Iron Man with release year 2007 should be accepted for 2008 movie (+/- 1 year)", result.isAccepted)
        org.junit.Assert.assertTrue("Score should be > 0.60, was ${result.score}", result.score > 0.60)
    }

    @Test
    fun testPollutedDescriptionDoesNotBreakGenericHostLabel() {
        val hints = MatchHints(
            tmdbId = 27205,
            imdbId = "tt1375666",
            type = ContentType.MOVIE,
            titles = setOf("Inception"),
            year = 2010
        )
        val stream = com.euthopiar.core.streams.StreamItem(
            name = "HubCloud 10Gbps",
            description = "4KHDHub • 1080p • 2.4 GB • Ultra Speed High-Bandwidth Line",
            addonName = "4KHDHub",
            addonId = "hub"
        )
        val result = MatchScorer.scoreStream(hints, stream)
        org.junit.Assert.assertTrue("Generic host title with arbitrary description text should pass", result.isAccepted)
        org.junit.Assert.assertTrue("Score should be >= 0.75, was ${result.score}", result.score >= 0.75)
    }

    @Test
    fun testStreamMaskedTitleWithContradictoryRawTitleRejected() {
        val hints = MatchHints(
            tmdbId = 19995,
            type = ContentType.MOVIE,
            titles = setOf("Avatar"),
            year = 2009
        )
        val stream = com.euthopiar.core.streams.StreamItem(
            name = "Avatar (2009) [1080p] [Multi]",
            title = "Avatar (2009) [1080p] [Multi]",
            description = "VegaMovies • Google CDN",
            filename = "Avatar.The.Way.of.Water.2022.1080p.WEB-DL.DDP5.1.Atmos.H.264",
            addonName = "VegaMovies",
            addonId = "vegamovies"
        )
        val result = MatchScorer.scoreStream(hints, stream)
        org.junit.Assert.assertFalse("Stream with contradictory raw scraped title must be rejected", result.isAccepted)
        assertEquals(0.0, result.score, 0.001)
    }
}
