package com.euthopiar.core.js

import com.euthopiar.core.js.network.FetchBridge
import com.euthopiar.core.util.TmdbBridge
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FetchBridgeConsolidationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        TmdbBridge.clearCachesForTesting()
    }

    @Test
    fun testFetchBridgeRoutesToVaultAndOmitsEmptyApiKey() = runBlocking {
        val bridge = FetchBridge()
        // We will seed TmdbBridge external_ids cache for movie 12345
        TmdbBridge.cacheTmdbExternalIdsJson("movie", "12345", """{"id":12345,"imdb_id":"tt1234567"}""")

        // Fetch URL missing api_key
        val rawResult = bridge.performNativeFetch(
            url = "https://api.themoviedb.org/3/movie/12345/external_ids",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )

        val resultObj = json.parseToJsonElement(rawResult).jsonObject
        assertTrue(resultObj["ok"]?.jsonPrimitive?.booleanOrNull == true)
        val finalUrl = resultObj["url"]?.jsonPrimitive?.content ?: ""
        assertTrue(finalUrl.startsWith(TmdbBridge.BASE_URL))
        assertTrue(!finalUrl.contains("api_key="))
        val body = resultObj["body"]?.jsonPrimitive?.content ?: ""
        assertTrue(body.contains("tt1234567"))
    }

    @Test
    fun testFetchBridgeFixesUndefinedAndNullApiKey() = runBlocking {
        val bridge = FetchBridge()
        TmdbBridge.cacheTmdbExternalIdsJson("movie", "54321", """{"id":54321,"imdb_id":"tt7654321"}""")

        val rawResultUndefined = bridge.performNativeFetch(
            url = "https://api.themoviedb.org/3/movie/54321/external_ids?api_key=undefined",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )

        val resultObj = json.parseToJsonElement(rawResultUndefined).jsonObject
        val finalUrl = resultObj["url"]?.jsonPrimitive?.content ?: ""
        assertTrue(finalUrl.startsWith(TmdbBridge.BASE_URL))
        assertTrue(!finalUrl.contains("api_key="))
        assertTrue(!finalUrl.contains("undefined"))
    }

    @Test
    fun testFetchBridgeDoesNotReturnBasicDetailsCacheWhenAppendToResponsePresent() = runBlocking {
        val bridge = FetchBridge()
        // Cache a basic details response
        TmdbBridge.cacheTmdbDetailsJson("movie", "88888", """{"id":88888,"title":"Basic Movie"}""")

        // Request with append_to_response=credits. Since we don't mock the HTTP server, httpRequestRaw
        // will attempt network call and fail/timeout or return 0, but it MUST NOT return the cached basic details!
        val rawResult = bridge.performNativeFetch(
            url = "https://api.themoviedb.org/3/movie/88888?append_to_response=credits",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = false
        )

        val resultObj = json.parseToJsonElement(rawResult).jsonObject
        val body = resultObj["body"]?.jsonPrimitive?.content ?: ""
        // It must NOT short-circuit to the basic movie details
        assertTrue(body != """{"id":88888,"title":"Basic Movie"}""")
    }

    @Test
    fun testFetchBridgeHandlesSchemelessAndProtocolRelativeVaultUrls() = runBlocking {
        val bridge = FetchBridge()
        TmdbBridge.cacheTmdbExternalIdsJson("movie", "99901", """{"id":99901,"imdb_id":"tt9990101"}""")

        // Schemeless themoviedb.org
        val res1 = bridge.performNativeFetch(
            url = "api.themoviedb.org/3/movie/99901/external_ids",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )
        val obj1 = json.parseToJsonElement(res1).jsonObject
        assertEquals(true, obj1["ok"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(obj1["url"]?.jsonPrimitive?.content?.startsWith(TmdbBridge.BASE_URL) == true)

        // Protocol-relative vault
        val res2 = bridge.performNativeFetch(
            url = "//euthopiar-vault.sehgalvansh716.workers.dev/3/movie/99901/external_ids",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )
        val obj2 = json.parseToJsonElement(res2).jsonObject
        assertEquals(true, obj2["ok"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(obj2["url"]?.jsonPrimitive?.content?.startsWith(TmdbBridge.BASE_URL) == true)

        // Schemeless vault
        val res3 = bridge.performNativeFetch(
            url = "euthopiar-vault.sehgalvansh716.workers.dev/3/movie/99901/external_ids/",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )
        val obj3 = json.parseToJsonElement(res3).jsonObject
        assertEquals(true, obj3["ok"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(obj3["url"]?.jsonPrimitive?.content?.startsWith(TmdbBridge.BASE_URL) == true)
        assertTrue(obj3["body"]?.jsonPrimitive?.content?.contains("tt9990101") == true)
    }

    @Test
    fun testFetchBridgeCleansDanglingQueryParams() = runBlocking {
        val bridge = FetchBridge()
        TmdbBridge.cacheTmdbExternalIdsJson("movie", "99902", """{"id":99902,"imdb_id":"tt9990202"}""")

        val res = bridge.performNativeFetch(
            url = "https://api.themoviedb.org/3/movie/99902/external_ids?api_key=&",
            method = "GET",
            headersJson = "{}",
            bodyKind = "none",
            body = "",
            followRedirects = true
        )
        val obj = json.parseToJsonElement(res).jsonObject
        val finalUrl = obj["url"]?.jsonPrimitive?.content ?: ""
        assertTrue("URL must not end with ? or &: $finalUrl", !finalUrl.endsWith("?") && !finalUrl.endsWith("&"))
        assertTrue("URL must route to BASE_URL: $finalUrl", finalUrl.startsWith(TmdbBridge.BASE_URL))
    }
}
