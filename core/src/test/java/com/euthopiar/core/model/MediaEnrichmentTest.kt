package com.euthopiar.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaEnrichmentTest {

    @Test
    fun testWithEnrichmentPreservesAndUpdatesArtwork() {
        val original = MediaItem(
            id = "100",
            title = "Dune",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w500/old_poster.jpg",
            backdropUrl = null,
            type = MediaType.MOVIE
        )

        val upgradedPoster = original.posterUrl?.replace("/w500/", "/w780/")
        val enrichedBackdrop = "https://image.tmdb.org/t/p/w1280/new_backdrop.jpg"

        val enriched = original.withEnrichment(
            posterUrl = upgradedPoster,
            backdropUrl = enrichedBackdrop,
            rating = "8.2",
            year = 2024
        )

        assertEquals("https://image.tmdb.org/t/p/w780/old_poster.jpg", enriched.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w1280/new_backdrop.jpg", enriched.backdropUrl)
        assertEquals("8.2", enriched.rating)
        assertEquals(2024, enriched.year)
    }

    @Test
    fun testWithEnrichmentDefaultsFallbackToExisting() {
        val original = MediaItem(
            id = "200",
            title = "Breaking Bad",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/bb_poster.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/bb_backdrop.jpg",
            type = MediaType.TV_SERIES,
            year = 2008
        )

        // Enriching only rating without overriding artwork
        val enriched = original.withEnrichment(rating = "9.5")

        assertEquals(original.posterUrl, enriched.posterUrl)
        assertEquals(original.backdropUrl, enriched.backdropUrl)
        assertEquals(2008, enriched.year)
        assertEquals("9.5", enriched.rating)
    }
}
