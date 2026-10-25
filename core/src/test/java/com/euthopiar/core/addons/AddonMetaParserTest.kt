package com.euthopiar.core.addons

import com.euthopiar.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonMetaParserTest {

    @Test
    fun testParseMovieMeta() {
        val json = """
            {
                "meta": {
                    "id": "tt1375666",
                    "type": "movie",
                    "name": "Inception",
                    "poster": "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
                    "background": "https://image.tmdb.org/t/p/original/s3TBrRGB1iav7gFOCNx3H31MoES.jpg",
                    "logo": "https://image.tmdb.org/t/p/original/inception_logo.png",
                    "description": "A thief who steals corporate secrets through dream-sharing technology.",
                    "releaseInfo": "2010",
                    "imdbRating": "8.8",
                    "genres": ["Action", "Sci-Fi", "Adventure"],
                    "runtime": "148 min",
                    "cast": ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"],
                    "director": ["Christopher Nolan"],
                    "trailers": [
                        { "source": "YoHD9XEInc0", "type": "Trailer" }
                    ]
                }
            }
        """.trimIndent()

        val detail = AddonMetaParser.parse(json, "tt1375666", "movie", "Cinemeta")
        assertNotNull(detail)
        assertEquals("tt1375666", detail!!.id)
        assertEquals("Inception", detail.title)
        assertEquals(MediaType.MOVIE, detail.type)
        assertEquals(2010, detail.year)
        assertEquals("8.8", detail.rating)
        assertEquals(listOf("Action", "Sci-Fi", "Adventure"), detail.genres)
        assertEquals("148 min", detail.duration)
        assertEquals("https://www.youtube.com/watch?v=YoHD9XEInc0", detail.trailerUrl)
        assertEquals(listOf("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"), detail.cast.map { it.name })
        assertEquals(listOf("Christopher Nolan"), detail.directors)
        // Movie fallback single episode for instant playback
        assertEquals(1, detail.episodes.size)
        assertEquals("tt1375666", detail.episodes.first().data)
    }

    @Test
    fun testParseSeriesMetaWithVideos() {
        val json = """
            {
                "meta": {
                    "id": "tt0944947",
                    "type": "series",
                    "name": "Game of Thrones",
                    "poster": "https://example.com/got_poster.jpg",
                    "background": "https://example.com/got_bg.jpg",
                    "description": "Nine noble families fight for control over the lands of Westeros.",
                    "releaseInfo": "2011-2019",
                    "imdbRating": "9.2",
                    "genres": ["Action", "Adventure", "Drama"],
                    "videos": [
                        {
                            "id": "tt0944947:1:1",
                            "title": "Winter Is Coming",
                            "season": 1,
                            "episode": 1,
                            "released": "2011-04-17",
                            "overview": "Ned Stark is torn between his family and an old friend.",
                            "thumbnail": "https://example.com/s1e1.jpg"
                        },
                        {
                            "id": "tt0944947:1:2",
                            "title": "The Kingsroad",
                            "season": 1,
                            "episode": 2,
                            "released": "2011-04-24",
                            "overview": "Bran's fate remains in doubt.",
                            "thumbnail": "https://example.com/s1e2.jpg"
                        }
                    ]
                }
            }
        """.trimIndent()

        val detail = AddonMetaParser.parse(json, "tt0944947", "series", "Cinemeta")
        assertNotNull(detail)
        assertEquals("Game of Thrones", detail!!.title)
        assertEquals(MediaType.TV_SERIES, detail.type)
        assertEquals(2011, detail.year)
        assertEquals(2, detail.episodes.size)

        val ep1 = detail.episodes[0]
        assertEquals("tt0944947:1:1", ep1.id)
        assertEquals("tt0944947:1:1", ep1.data)
        assertEquals("Winter Is Coming", ep1.title)
        assertEquals(1, ep1.seasonNumber)
        assertEquals(1, ep1.episodeNumber)
        assertEquals("https://example.com/s1e1.jpg", ep1.thumbnail)

        val ep2 = detail.episodes[1]
        assertEquals("tt0944947:1:2", ep2.id)
        assertEquals(1, ep2.seasonNumber)
        assertEquals(2, ep2.episodeNumber)
    }

    @Test
    fun testParseLiveTvChannelMetaWithDefaultVideoId() {
        val json = """
            {
                "meta": {
                    "id": "ustv:hbo",
                    "type": "channel",
                    "name": "HBO East",
                    "poster": "https://example.com/hbo_logo.png",
                    "description": "HBO Live TV feed",
                    "behaviorHints": {
                        "defaultVideoId": "ustv:hbo:live"
                    }
                }
            }
        """.trimIndent()

        val detail = AddonMetaParser.parse(json, "ustv:hbo", "channel", "LiveAddon")
        assertNotNull(detail)
        assertEquals("HBO East", detail!!.title)
        // channel defaults to MOVIE in AddonMetaParser or single stream target
        assertEquals(1, detail.episodes.size)
        assertEquals("ustv:hbo:live", detail.episodes.first().data)
        assertEquals("ustv:hbo:live", detail.episodes.first().id)
    }
}
