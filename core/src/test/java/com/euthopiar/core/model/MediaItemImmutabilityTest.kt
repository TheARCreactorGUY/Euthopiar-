package com.euthopiar.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaItemImmutabilityTest {

    @Test
    fun testCopyPreservesRatingsAndGenres() {
        val item = MediaItem(
            id = "test_1",
            title = "Inception",
            url = "https://example.com/inception",
            posterUrl = "https://example.com/poster.jpg",
            type = MediaType.MOVIE,
            year = 2010,
            rating = "8.8",
            contentRating = "PG-13",
            rottenTomatoesRating = "87%",
            genres = listOf("Action", "Sci-Fi", "Adventure")
        )

        // Calling standard data class .copy() must preserve all fields
        val copied = item.copy(title = "Inception (2010)")

        assertEquals("Inception (2010)", copied.title)
        assertEquals("PG-13", copied.contentRating)
        assertEquals("87%", copied.rottenTomatoesRating)
        assertEquals(listOf("Action", "Sci-Fi", "Adventure"), copied.genres)
    }

    @Test
    fun testWithEnrichmentPreservesAndUpdatesGenres() {
        val base = MediaItem(
            id = "test_2",
            title = "Dune",
            url = "",
            posterUrl = null,
            type = MediaType.MOVIE,
            contentRating = "PG-13",
            rottenTomatoesRating = "83%",
            genres = listOf("Sci-Fi")
        )

        val enriched = base.withEnrichment(
            rating = "8.1",
            genres = listOf("Sci-Fi", "Adventure")
        )

        assertEquals("8.1", enriched.rating)
        assertEquals("PG-13", enriched.contentRating)
        assertEquals("83%", enriched.rottenTomatoesRating)
        assertEquals(listOf("Sci-Fi", "Adventure"), enriched.genres)
    }
}
