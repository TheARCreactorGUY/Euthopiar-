package com.euthopiar.core.util

import com.euthopiar.core.js.host.HostFunctions
import com.euthopiar.core.model.MediaType
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TmdbBridgeConsolidationTest {

    @Before
    fun setUp() {
        TmdbBridge.clearCachesForTesting()
    }

    @Test
    fun testResolveImdbIdWithEndpointType() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """{"id":550,"imdb_id":"tt0137523","tvdb_id":null}"""
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val imdbId = TmdbBridge.resolveImdbId(
            type = "movie",
            tmdbId = "550",
            client = mockClient
        )

        assertEquals("tt0137523", imdbId)
        assertTrue(requestedUrl.startsWith(TmdbBridge.BASE_URL))
        assertTrue(requestedUrl.contains("/3/movie/550/external_ids"))
        assertTrue(!requestedUrl.contains("api_key="))
    }

    @Test
    fun testResolveImdbIdDirectImdbReturnsImmediately() = runBlocking {
        val imdbId = TmdbBridge.resolveImdbId(type = "movie", tmdbId = "tt0137523")
        assertEquals("tt0137523", imdbId)
    }

    @Test
    fun testResolveImdbIdBlankReturnsNull() = runBlocking {
        val imdbId = TmdbBridge.resolveImdbId(type = "movie", tmdbId = "   ")
        assertNull(imdbId)
    }

    @Test
    fun testFetchTrendingTopTenMovies() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 550,
                            "title": "Fight Club",
                            "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                            "backdrop_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
                            "vote_average": 8.433,
                            "release_date": "1999-10-15"
                        },
                        {
                            "id": 680,
                            "title": "Pulp Fiction",
                            "poster_path": "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
                            "backdrop_path": "/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg",
                            "vote_average": 8.489,
                            "release_date": "1994-09-10"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingTopTen(
            type = MediaType.MOVIE,
            language = "en-US",
            client = mockClient
        )

        assertEquals(2, items.size)
        assertEquals("550", items[0].id)
        assertEquals("Fight Club", items[0].title)
        assertEquals("https://image.tmdb.org/t/p/w780/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", items[0].posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w1280/hZkgoQYus5vegHoetLkCJzb17zJ.jpg", items[0].backdropUrl)
        assertEquals(1999, items[0].year)
        assertEquals("8.4", items[0].rating)
        assertEquals("1080p", items[0].quality)
        assertEquals("tmdb", items[0].provider)
        assertEquals(MediaType.MOVIE, items[0].type)

        assertTrue(requestedUrl.startsWith(TmdbBridge.BASE_URL))
        assertTrue(requestedUrl.contains("/3/trending/movie/day"))
        assertTrue(!requestedUrl.contains("api_key="))
        assertTrue(requestedUrl.contains("language=en-US"))
    }

    @Test
    fun testFetchTrendingTopTenTvSeriesUsesTvEndpoint() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 1399,
                            "name": "Game of Thrones",
                            "poster_path": "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                            "backdrop_path": "/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
                            "vote_average": 8.452,
                            "first_air_date": "2011-04-17"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingTopTen(
            type = MediaType.TV_SERIES,
            client = mockClient
        )

        assertEquals(1, items.size)
        assertEquals("1399", items[0].id)
        assertEquals("Game of Thrones", items[0].title)
        assertEquals(2011, items[0].year)
        assertTrue(requestedUrl.contains("/3/trending/tv/day"))
    }

    @Test
    fun testFetchTrendingTopTenHandlesHttpErrorGracefully() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Server Error")
                    .body("Internal Server Error".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingTopTen(
            type = MediaType.MOVIE,
            client = mockClient
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun testHostFunctionsUsesTmdbBridgeDefaultKey() {
        val hostFunctions = HostFunctions(scraperId = "test-scraper", onResult = {})
        // HostFunctions private field inspection via reflection to guarantee consistency
        val field = HostFunctions::class.java.getDeclaredField("tmdbApiKey").apply { isAccessible = true }
        val key = field.get(hostFunctions) as String
        assertEquals(TmdbBridge.DEFAULT_API_KEY, key)
    }

    @Test
    fun testResolveImdbIdWithMediaTypeOverload() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """{"id":1399,"imdb_id":"tt0944947","tvdb_id":121361}"""
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val imdbId = TmdbBridge.resolveImdbId(
            type = MediaType.TV_SERIES,
            tmdbId = "1399",
            client = mockClient
        )

        assertEquals("tt0944947", imdbId)
        assertTrue(requestedUrl.contains("/3/tv/1399/external_ids"))
    }

    @Test
    fun testResolveImdbIdWithTvSeriesString() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """{"id":1399,"imdb_id":"tt0944947","tvdb_id":121361}"""
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val imdbId = TmdbBridge.resolveImdbId(
            type = "tv_series",
            tmdbId = "1399",
            client = mockClient
        )

        assertEquals("tt0944947", imdbId)
        assertTrue(requestedUrl.contains("/3/tv/1399/external_ids"))
    }

    @Test
    fun testResolveImdbIdUsesCachedExternalIdsWithoutNetworkCall() = runBlocking {
        TmdbBridge.cacheTmdbExternalIdsJson("movie", "99999", """{"id":99999,"imdb_id":"tt9999999"}""")

        // Client that fails if called
        val failClient = OkHttpClient.Builder()
            .addInterceptor {
                throw AssertionError("Network call was made when cached external IDs should have been used")
            }
            .build()

        val imdbId = TmdbBridge.resolveImdbId(
            type = "movie",
            tmdbId = "99999",
            client = failClient
        )

        assertEquals("tt9999999", imdbId)
    }

    @Test
    fun testFetchTrendingTopTenAnimeUsesTvEndpoint() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 85937,
                            "name": "Demon Slayer",
                            "poster_path": "/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg",
                            "vote_average": 8.7,
                            "first_air_date": "2019-04-06"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingTopTen(
            type = MediaType.ANIME,
            client = mockClient
        )

        assertEquals(1, items.size)
        assertEquals("85937", items[0].id)
        assertEquals("Demon Slayer", items[0].title)
        assertTrue(requestedUrl.contains("/3/trending/tv/day"))
    }

    @Test
    fun testFetchTrendingAllUsesAllEndpointAndParsesCorrectly() = runBlocking {
        var requestedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrl = chain.request().url.toString()
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 1400940,
                            "media_type": "movie",
                            "title": "Clayface",
                            "poster_path": "/5jCpQnWPikggmQZoDp1eAi6BI6w.jpg",
                            "backdrop_path": "/clayface_back.jpg",
                            "vote_average": 7.9,
                            "release_date": "2026-10-19"
                        },
                        {
                            "id": 220542,
                            "media_type": "tv",
                            "name": "The Apothecary Diaries",
                            "poster_path": "/sJZcy8QsOPuRPiCfoCWVB3r9Ohg.jpg",
                            "backdrop_path": "/apothecary_back.jpg",
                            "vote_average": 8.8,
                            "first_air_date": "2023-10-22"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingAll(
            timeWindow = "day",
            client = mockClient
        )

        assertEquals(2, items.size)
        assertEquals("1400940", items[0].id)
        assertEquals("Clayface", items[0].title)
        assertEquals(MediaType.MOVIE, items[0].type)
        assertEquals("https://image.tmdb.org/t/p/w780/5jCpQnWPikggmQZoDp1eAi6BI6w.jpg", items[0].posterUrl)

        assertEquals("220542", items[1].id)
        assertEquals("The Apothecary Diaries", items[1].title)
        assertEquals(MediaType.TV_SERIES, items[1].type)

        assertTrue(requestedUrl.startsWith(TmdbBridge.BASE_URL))
        assertTrue(requestedUrl.contains("/3/trending/all/day"))
        assertTrue(!requestedUrl.contains("api_key="))
    }

    @Test
    fun testFetchOnboardingShowcasePostersReturnsCuratedMix() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val body = when {
                    url.contains("/trending/movie/day") -> """
                        {
                            "results": [
                                {
                                    "id": 101,
                                    "title": "Movie Alpha",
                                    "poster_path": "/alpha.jpg",
                                    "release_date": "2025-01-01"
                                }
                            ]
                        }
                    """.trimIndent()
                    url.contains("/trending/tv/day") -> """
                        {
                            "results": [
                                {
                                    "id": 201,
                                    "name": "Series Beta",
                                    "poster_path": "/beta.jpg",
                                    "first_air_date": "2025-02-01"
                                }
                            ]
                        }
                    """.trimIndent()
                    else -> """
                        {
                            "results": [
                                {
                                    "id": 301,
                                    "title": "Top Ten Item",
                                    "media_type": "movie",
                                    "poster_path": "/topten.jpg",
                                    "release_date": "2025-03-01"
                                }
                            ]
                        }
                    """.trimIndent()
                }

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val showcase = TmdbBridge.fetchOnboardingShowcasePosters(client = mockClient)

        assertEquals(1, showcase.trendingMovies.size)
        assertEquals("Movie Alpha", showcase.trendingMovies[0].title)

        assertEquals(1, showcase.trendingTv.size)
        assertEquals("Series Beta", showcase.trendingTv[0].title)

        assertEquals(1, showcase.topTen.size)
        assertEquals("Top Ten Item", showcase.topTen[0].title)

        // Mixed should contain all 3 items deduplicated
        assertEquals(3, showcase.mixed.size)
        assertEquals("Top Ten Item", showcase.mixed[0].title)
        assertTrue(showcase.mixed.any { it.title == "Movie Alpha" })
        assertTrue(showcase.mixed.any { it.title == "Series Beta" })
    }

    @Test
    fun testFetchTrendingAllHandlesHttpErrorGracefully() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingAll(client = mockClient)
        assertTrue(items.isEmpty())
    }

    @Test
    fun testFetchTrendingAllFiltersPersonsAndRequiresPosters() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 9901,
                            "media_type": "person",
                            "name": "Actor Person",
                            "profile_path": "/person.jpg"
                        },
                        {
                            "id": 9902,
                            "media_type": "movie",
                            "title": "Movie Without Poster",
                            "poster_path": null
                        },
                        {
                            "id": 9903,
                            "media_type": "tv",
                            "name": "TV With Poster",
                            "poster_path": "/tv_poster.jpg"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingAll(client = mockClient)
        assertEquals(1, items.size)
        assertEquals("9903", items[0].id)
        assertEquals("TV With Poster", items[0].title)
        assertEquals(MediaType.TV_SERIES, items[0].type)
    }

    @Test
    fun testFetchTrendingParsesJsonNullTitleAndDateGracefully() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val body = """
                {
                    "page": 1,
                    "results": [
                        {
                            "id": 8801,
                            "title": null,
                            "name": "Anime Series Name",
                            "release_date": null,
                            "first_air_date": "2024-04-01",
                            "poster_path": "/anime.jpg"
                        }
                    ]
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val items = TmdbBridge.fetchTrendingTopTen(type = MediaType.ANIME, client = mockClient)
        assertEquals(1, items.size)
        assertEquals("Anime Series Name", items[0].title)
        assertEquals(2024, items[0].year)
    }
}
