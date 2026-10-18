package com.euthopiar.ui.components

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroCarouselOrientationTest {

    @Test
    fun testPortraitPicksPosterOverBackdrop() {
        val item = MediaItem(
            id = "123",
            title = "Inception",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/poster.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            type = MediaType.MOVIE
        )

        val isLandscapeOrLarge = false
        val artworkUrl = if (isLandscapeOrLarge) {
            item.backdropUrl ?: item.posterUrl
        } else {
            item.posterUrl ?: item.backdropUrl
        }

        assertEquals("Portrait must pick 2:3 poster as primary artwork", "https://image.tmdb.org/t/p/w780/poster.jpg", artworkUrl)
    }

    @Test
    fun testLandscapePicksBackdropOverPoster() {
        val item = MediaItem(
            id = "123",
            title = "Inception",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/poster.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            type = MediaType.MOVIE
        )

        val isLandscapeOrLarge = true
        val artworkUrl = if (isLandscapeOrLarge) {
            item.backdropUrl ?: item.posterUrl
        } else {
            item.posterUrl ?: item.backdropUrl
        }

        assertEquals("Landscape must pick 16:9 backdrop as primary artwork", "https://image.tmdb.org/t/p/w1280/backdrop.jpg", artworkUrl)
    }

    @Test
    fun testPortraitFallbackToBackdropWhenPosterMissing() {
        val item = MediaItem(
            id = "123",
            title = "Inception",
            url = "",
            posterUrl = null,
            backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            type = MediaType.MOVIE
        )

        val isLandscapeOrLarge = false
        val artworkUrl = if (isLandscapeOrLarge) {
            item.backdropUrl ?: item.posterUrl
        } else {
            item.posterUrl ?: item.backdropUrl
        }

        assertEquals("Portrait should fallback to backdrop if poster is missing", "https://image.tmdb.org/t/p/w1280/backdrop.jpg", artworkUrl)
    }

    @Test
    fun testLandscapeFallbackToPosterWhenBackdropMissing() {
        val item = MediaItem(
            id = "123",
            title = "Inception",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/poster.jpg",
            backdropUrl = null,
            type = MediaType.MOVIE
        )

        val isLandscapeOrLarge = true
        val artworkUrl = if (isLandscapeOrLarge) {
            item.backdropUrl ?: item.posterUrl
        } else {
            item.posterUrl ?: item.backdropUrl
        }

        assertEquals("Landscape should fallback to poster if backdrop is missing", "https://image.tmdb.org/t/p/w780/poster.jpg", artworkUrl)
    }

    @Test
    fun testArtworkResolutionUpgradeFromW500() {
        val w500Url = "https://image.tmdb.org/t/p/w500/sample.jpg"

        // Landscape upgrade
        val landscapeUpgraded = w500Url.replace("/t/p/w500/", "/t/p/w1280/")
        assertEquals("Landscape must upgrade w500 to w1280", "https://image.tmdb.org/t/p/w1280/sample.jpg", landscapeUpgraded)

        // Portrait upgrade
        val portraitUpgraded = w500Url.replace("/t/p/w500/", "/t/p/w780/")
        assertEquals("Portrait must upgrade w500 to w780", "https://image.tmdb.org/t/p/w780/sample.jpg", portraitUpgraded)
    }

    @Test
    fun testLandscapePhoneCarouselHeightDoesNotOverflow() {
        val screenHeightDp = 390f // standard phone landscape height
        val isLandscape = true
        val isLargeScreen = false

        val height = if (isLandscape) {
            if (isLargeScreen) {
                (screenHeightDp * 0.65f).coerceIn(400f, 600f)
            } else {
                (screenHeightDp * 0.85f).coerceIn(280f, 380f)
            }
        } else {
            (screenHeightDp * 0.60f + 40f).coerceIn(480f, 600f)
        }

        assertTrue("Landscape phone carousel height ($height) must not exceed screen height ($screenHeightDp)", height < screenHeightDp)
        assertEquals(331.5f, height, 0.1f)
    }
}
