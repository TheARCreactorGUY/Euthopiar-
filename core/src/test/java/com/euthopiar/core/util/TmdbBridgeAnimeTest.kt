package com.euthopiar.core.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbBridgeAnimeTest {

    @Test
    fun testOfflineAnimeEpisodeSumExcludesSeasonZeroSpecials() = runBlocking {
        // Season 0 has 8 specials; Season 1 has 25; Season 2 has 12.
        // For Season 3 Episode 5: expected is 25 + 12 + 5 = 42.
        val seasonCounts = mapOf(
            0 to 8,  // Specials
            1 to 25,
            2 to 12,
            3 to 22
        )

        val absEp = TmdbBridge.resolveAnimeAbsoluteEpisode(
            tmdbId = "fake_anime_id_9999",
            season = 3,
            episode = 5,
            seasonEpisodeCounts = seasonCounts
        )

        assertEquals(42, absEp)
    }

    @Test
    fun testSeasonOneReturnsEpisodeDirectly() = runBlocking {
        val seasonCounts = mapOf(
            0 to 4,
            1 to 12
        )

        val absEp = TmdbBridge.resolveAnimeAbsoluteEpisode(
            tmdbId = "fake_anime_id_8888",
            season = 1,
            episode = 7,
            seasonEpisodeCounts = seasonCounts
        )

        assertEquals(7, absEp)
    }
}
