package com.euthopiar.core.sync

import com.euthopiar.core.aniskip.AniSkipClient
import com.euthopiar.core.aniskip.SkipTimestamps
import com.euthopiar.core.subtitle.external.SubDlClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AniSkipAndSubDlTest {

    @Test
    fun testAniSkipClientSingleton() {
        val client1 = AniSkipClient.getInstance()
        val client2 = AniSkipClient.getInstance()
        assertSame(client1, client2)
    }

    @Test
    fun testSubDlClientSingleton() {
        val client1 = SubDlClient.getInstance()
        val client2 = SubDlClient.getInstance()
        assertSame(client1, client2)
    }

    @Test
    fun testSkipTimestampsDataClass() {
        val stamps = SkipTimestamps(
            introStartSeconds = 12.5,
            introEndSeconds = 97.5,
            outroStartSeconds = 1350.0,
            outroEndSeconds = 1440.0
        )
        assertEquals(12.5, stamps.introStartSeconds!!, 0.01)
        assertEquals(97.5, stamps.introEndSeconds!!, 0.01)
        assertEquals(1350.0, stamps.outroStartSeconds!!, 0.01)
        assertEquals(1440.0, stamps.outroEndSeconds!!, 0.01)
    }

    @Test
    fun testSubDlSearchSubtitlesGracefulOnEmpty() = runBlocking {
        val client = SubDlClient.getInstance()
        // Querying with non-existent IDs shouldn't crash
        val res = client.searchSubtitles(tmdbId = "00000000", imdbId = "tt0000000")
        assertNotNull(res)
    }
}
