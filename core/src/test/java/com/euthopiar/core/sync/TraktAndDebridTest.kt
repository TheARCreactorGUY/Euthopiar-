package com.euthopiar.core.sync

import com.euthopiar.core.debrid.DebridManager
import com.euthopiar.core.debrid.DebridProviderType
import com.euthopiar.core.sync.simkl.*
import com.euthopiar.core.sync.trakt.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TraktAndDebridTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testTraktClientDeviceCodeInitialization() = runBlocking {
        val client = TraktClient.getInstance()
        val result = client.generateDeviceCode()
        if (result.isSuccess) {
            val code = result.getOrNull()
            assertNotNull(code)
            assertTrue(code!!.deviceCode.isNotBlank())
            assertTrue(code.userCode.isNotBlank())
            assertTrue(code.verificationUrl.contains("trakt"))
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun testTraktScrobblePayloadConstruction() {
        val movie = TraktMovie(
            title = "Fight Club",
            year = 1999,
            ids = TraktIds(tmdb = 550, imdb = "tt0137523")
        )
        val body = TraktScrobbleBody(movie = movie, progress = 45.5f)
        assertEquals(45.5f, body.progress, 0.01f)
        assertEquals("Fight Club", body.movie?.title)
        assertEquals(550L, body.movie?.ids?.tmdb)
    }

    @Test
    fun testTraktAuthorizationUrlPkce() {
        val client = TraktClient.getInstance()
        val authUrl = client.getAuthorizationUrl()
        assertTrue(authUrl.startsWith("https://trakt.tv/oauth/authorize"))
        assertTrue(authUrl.contains("response_type=code"))
        assertTrue(authUrl.contains("code_challenge_method=S256"))
        assertTrue(authUrl.contains("code_challenge="))
        assertTrue(authUrl.contains("redirect_uri=http%3A%2F%2Flocalhost"))
        assertNotNull(client.pendingCodeVerifier)
        assertTrue(client.pendingCodeVerifier!!.length >= 32)
    }

    @Test
    fun testTraktSyncPayloadSerialization() {
        val payload = TraktSyncItemsPayload(
            movies = listOf(TraktMovie(title = "Inception", year = 2010, ids = TraktIds(tmdb = 27205, imdb = "tt1375666"))),
            shows = listOf(TraktShow(title = "Breaking Bad", year = 2008, ids = TraktIds(tmdb = 1396, imdb = "tt0903747")))
        )
        val encoded = json.encodeToString(payload)
        assertTrue(encoded.contains("Inception"))
        assertTrue(encoded.contains("27205"))
        assertTrue(encoded.contains("Breaking Bad"))
        assertTrue(encoded.contains("1396"))

        val decoded = json.decodeFromString<TraktSyncItemsPayload>(encoded)
        assertEquals(1, decoded.movies.size)
        assertEquals("Inception", decoded.movies[0].title)
        assertEquals(1, decoded.shows.size)
        assertEquals("Breaking Bad", decoded.shows[0].title)
    }

    @Test
    fun testTraktPlaybackProgressItemDeserialization() {
        val raw = """
            [
                {
                    "id": 1001,
                    "progress": 72.5,
                    "paused_at": "2026-11-04T12:00:00.000Z",
                    "type": "movie",
                    "movie": {
                        "title": "Interstellar",
                        "year": 2014,
                        "ids": { "tmdb": 157336, "imdb": "tt0816692" }
                    }
                }
            ]
        """.trimIndent()
        val items = json.decodeFromString<List<TraktPlaybackProgressItem>>(raw)
        assertEquals(1, items.size)
        assertEquals(1001L, items[0].id)
        assertEquals(72.5f, items[0].progress, 0.01f)
        assertEquals("Interstellar", items[0].movie?.title)
        assertEquals(157336L, items[0].movie?.ids?.tmdb)
    }

    @Test
    fun testSimklAuthorizationUrlPkce() {
        val client = SimklClient.getInstance()
        val authUrl = client.getAuthorizationUrl()
        assertTrue(authUrl.startsWith("https://simkl.com/oauth2/authorize"))
        assertTrue(authUrl.contains("response_type=code"))
        assertTrue(authUrl.contains("code_challenge_method=S256"))
        assertTrue(authUrl.contains("code_challenge="))
        assertNotNull(client.pendingCodeVerifier)
        assertTrue(client.pendingCodeVerifier!!.length >= 32)
    }

    @Test
    fun testSimklSyncPayloadSerialization() {
        val payload = SimklSyncPayload(
            movies = listOf(SimklSyncItem(title = "Dune", to = "plantowatch", ids = SimklIds(tmdb = 438631, imdb = "tt1160419"))),
            shows = listOf(SimklSyncItem(title = "Severance", to = "watching", ids = SimklIds(tmdb = 95396, imdb = "tt11280740")))
        )
        val encoded = json.encodeToString(payload)
        assertTrue(encoded.contains("Dune"))
        assertTrue(encoded.contains("plantowatch"))
        assertTrue(encoded.contains("Severance"))
        assertTrue(encoded.contains("watching"))

        val decoded = json.decodeFromString<SimklSyncPayload>(encoded)
        assertEquals(1, decoded.movies.size)
        assertEquals(1, decoded.shows.size)
        assertEquals(438631L, decoded.movies[0].ids?.tmdb)
    }

    @Test
    fun testDebridManagerValidationDisabled() = runBlocking {
        val manager = DebridManager.getInstance()
        val res = manager.verifyAccount(DebridProviderType.NONE, "")
        assertTrue(res.isFailure)
    }

    @Test
    fun testDebridEmptyKeyFailsSafely() = runBlocking {
        val manager = DebridManager.getInstance()
        val res = manager.verifyAccount(DebridProviderType.REAL_DEBRID, "")
        assertTrue(res.isFailure)
    }

    @Test
    fun testDebridResolveStreamBlankApiKeyReturnsFailure() = runBlocking {
        val manager = DebridManager.getInstance()
        val res = manager.resolveStream(
            providerType = DebridProviderType.REAL_DEBRID,
            apiKey = "",
            linkOrMagnet = "magnet:?xt=urn:btih:test",
            originalResolutionLabel = "4K UHD",
            originalQuality = "2160p"
        )
        assertTrue("Blank API key must return failure Result", res.isFailure)
    }
}
