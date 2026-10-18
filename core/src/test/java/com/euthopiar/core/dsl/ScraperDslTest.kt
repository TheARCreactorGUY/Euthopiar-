package com.euthopiar.core.dsl

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.MediaType
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScraperDslTest {

    @Test
    fun testMediaItemDslBuilder() {
        val item = mediaItem {
            id = "movie_123"
            title = "Inception"
            url = "https://example.com/movie/123"
            posterUrl = "https://example.com/poster.jpg"
            backdropUrl = "https://example.com/backdrop.jpg"
            type = MediaType.MOVIE
            year = 2010
            rating = "8.8"
            quality = "1080p"
            provider = "Cinejoy"
        }

        assertEquals("movie_123", item.id)
        assertEquals("Inception", item.title)
        assertEquals(2010, item.year)
        assertEquals("8.8", item.rating)
        assertEquals("Cinejoy", item.provider)
    }

    @Test
    fun testCatalogRowDslBuilder() {
        val row = catalogRow("Trending Movies") {
            item {
                id = "1"
                title = "Item One"
                url = "https://example.com/1"
            }
            item {
                id = "2"
                title = "Item Two"
                url = "https://example.com/2"
            }
        }

        assertEquals("Trending Movies", row.title)
        assertEquals(2, row.items.size)
        assertEquals("Item One", row.items[0].title)
        assertEquals("Item Two", row.items[1].title)
    }

    @Test
    fun testStreamResultDslBuilder() {
        val result = streamResult {
            stream(
                url = "https://stream.example.com/master.m3u8",
                serverName = "Alpha CDN",
                resolutionLabel = "1080p",
                releaseType = AudioReleaseType.SUB
            )
            stream(
                url = "https://stream.example.com/720.mp4",
                serverName = "Beta Mirror",
                resolutionLabel = "720p",
                releaseType = AudioReleaseType.DUB
            )
            subtitle(url = "https://subs.example.com/en.vtt", language = "English")
            subtitle(url = "https://subs.example.com/es.srt", language = "Spanish")
        }

        assertEquals(2, result.streams.size)
        assertEquals("Alpha CDN", result.streams[0].serverName)
        assertEquals(AudioReleaseType.SUB, result.streams[0].releaseType)
        assertTrue(result.streams[0].isM3u8)

        assertEquals("Beta Mirror", result.streams[1].serverName)
        assertEquals(AudioReleaseType.DUB, result.streams[1].releaseType)

        assertEquals(2, result.subtitles.size)
        assertEquals("English", result.subtitles[0].language)
    }

    @Test
    fun testMediaDetailDslBuilder() {
        val baseItem = mediaItem {
            id = "tv_101"
            title = "Breaking Bad"
            url = "https://example.com/tv/101"
            type = MediaType.TV_SERIES
        }

        val detail = mediaDetail(baseItem) {
            synopsis = "A high school chemistry teacher..."
            genres = listOf("Crime", "Drama")
            rating = "9.5"
            contentRating = "TV-MA"
            imdbId = "tt0903747"
            availableAudioTypes = setOf(AudioReleaseType.SUB, AudioReleaseType.DUB)

            episode(
                id = "ep_1",
                title = "Pilot",
                seasonNumber = 1,
                episodeNumber = 1,
                data = "tv_101:1:1",
                duration = "58m"
            )
            castMember("Bryan Cranston", "Walter White", "https://img.example.com/bc.jpg")
        }

        assertEquals("Breaking Bad", detail.title)
        assertEquals("A high school chemistry teacher...", detail.synopsis)
        assertEquals(1, detail.episodes.size)
        assertEquals("Pilot", detail.episodes[0].title)
        assertEquals(1, detail.cast.size)
        assertEquals("Walter White", detail.cast[0].character)
    }

    @Test
    fun testJsonExtractionExtensions() {
        val jsonStr = """
            {
                "title": "Interstellar",
                "year": 2014,
                "score": 8.7,
                "is_active": true,
                "tags": ["sci-fi", "space"],
                "meta": { "director": "Christopher Nolan" }
            }
        """.trimIndent()

        val json = Json.parseToJsonElement(jsonStr)

        assertEquals("Interstellar", json.str("title"))
        assertEquals(2014, json.int("year"))
        assertEquals(8.7, json.double("score") ?: 0.0, 0.01)
        assertEquals(true, json.bool("is_active"))
        assertNotNull(json.arr("tags"))
        assertEquals(2, json.arr("tags")?.size)
        assertEquals("Christopher Nolan", json.obj("meta").str("director"))
        assertNull(json.str("non_existent"))
    }

    @Test
    fun testJsoupDomExtensions() {
        val html = """
            <div class="card">
                <a class="item-link" href="/movie/999">The Matrix</a>
                <img class="poster" src="https://img.com/matrix.jpg" alt="Matrix Poster"/>
                <span class="rating">8.7</span>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val card = doc.selectFirst(".card")!!

        assertEquals("The Matrix", card.textOrNull(".item-link"))
        assertEquals("/movie/999", card.href(".item-link"))
        assertEquals("https://img.com/matrix.jpg", card.src(".poster"))
        assertEquals("8.7", card.textOrNull(".rating"))
        assertNull(card.textOrNull(".non_existent"))
    }
}
