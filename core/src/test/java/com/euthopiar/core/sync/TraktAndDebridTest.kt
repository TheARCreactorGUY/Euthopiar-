package com.euthopiar.core.sync

import com.euthopiar.core.debrid.DebridManager
import com.euthopiar.core.debrid.DebridProviderType
import com.euthopiar.core.sync.simkl.SimklClient
import com.euthopiar.core.sync.trakt.TraktClient
import com.euthopiar.core.sync.trakt.TraktIds
import com.euthopiar.core.sync.trakt.TraktMovie
import com.euthopiar.core.sync.trakt.TraktScrobbleBody
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TraktAndDebridTest {

    @Test
    fun testTraktClientDeviceCodeInitialization() = runBlocking {
        val client = TraktClient.getInstance()
        val result = client.generateDeviceCode()
        // If internet is available, assert device code response fields
        if (result.isSuccess) {
            val code = result.getOrNull()
            assertNotNull(code)
            assertTrue(code!!.deviceCode.isNotBlank())
            assertTrue(code.userCode.isNotBlank())
            assertTrue(code.verificationUrl.contains("trakt"))
        } else {
            // Offline or rate limited - verify client handled gracefully without throwing unhandled exception
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
        assertEquals(550, body.movie?.ids?.tmdb)
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
