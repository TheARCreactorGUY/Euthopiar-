package com.euthopiar.ui.onboarding

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateHasFallbackPosters() = runTest(testDispatcher) {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"results": []}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val viewModel = OnboardingViewModel(application, mockClient, testDispatcher)
        // Before coroutines advance, initial state has fallback posters and isLoading = true
        val state = viewModel.postersState.value
        assertEquals(ShowcaseRowTop.size, state.topRowPosters.size)
        assertEquals(ShowcaseRowBottom.size, state.bottomRowPosters.size)
        assertTrue(state.isLoading)
    }

    @Test
    fun testSuccessfulPosterLoadPopulatesDynamicPosters() = runTest(testDispatcher) {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val body = when {
                    url.contains("/trending/movie/day") -> """
                        {
                            "results": [
                                {
                                    "id": 1001,
                                    "title": "Dynamic Movie 1",
                                    "poster_path": "/dyn_movie_1.jpg",
                                    "release_date": "2026-01-01"
                                }
                            ]
                        }
                    """.trimIndent()
                    url.contains("/trending/tv/day") -> """
                        {
                            "results": [
                                {
                                    "id": 2001,
                                    "name": "Dynamic TV 1",
                                    "poster_path": "/dyn_tv_1.jpg",
                                    "first_air_date": "2026-02-01"
                                }
                            ]
                        }
                    """.trimIndent()
                    else -> """
                        {
                            "results": [
                                {
                                    "id": 3001,
                                    "title": "Top Ten All 1",
                                    "media_type": "movie",
                                    "poster_path": "/dyn_top_1.jpg",
                                    "release_date": "2026-03-01"
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

        val viewModel = OnboardingViewModel(application, mockClient, testDispatcher)
        advanceUntilIdle()

        val state = viewModel.postersState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)

        // Top row contains dynamic movie poster & top ten item
        assertTrue(state.topRowPosters.any { it.title == "Dynamic Movie 1" })
        assertTrue(state.topRowPosters.any { it.title == "Top Ten All 1" })
        assertTrue(state.topRowPosters[0].posterUrl.contains("/dyn_movie_1.jpg"))

        // Bottom row contains dynamic TV poster & top ten item
        assertTrue(state.bottomRowPosters.any { it.title == "Dynamic TV 1" })
        assertTrue(state.bottomRowPosters.any { it.title == "Top Ten All 1" })
        assertTrue(state.bottomRowPosters[0].posterUrl.contains("/dyn_tv_1.jpg"))

        // Preview posters contain mixed items
        assertTrue(state.previewPosters.isNotEmpty())
        assertTrue(state.previewPosters.any { it.title == "Top Ten All 1" })
        assertTrue(state.previewPosters.any { it.title == "Dynamic Movie 1" })
    }

    @Test
    fun testNetworkErrorGracefullyFallsBackToDefaults() = runTest(testDispatcher) {
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

        val viewModel = OnboardingViewModel(application, mockClient, testDispatcher)
        advanceUntilIdle()

        val state = viewModel.postersState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        // Fallback to default Showcase rows
        assertEquals(ShowcaseRowTop.size, state.topRowPosters.size)
        assertEquals(ShowcaseRowBottom.size, state.bottomRowPosters.size)
        assertEquals(5, state.previewPosters.size)
    }

    @Test
    fun testToShowcasePosterItemMapping() {
        val itemWithPoster = com.euthopiar.core.model.MediaItem(
            id = "123",
            title = "Test Movie",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/test.jpg",
            type = MediaType.MOVIE,
            year = 2026
        )

        val showcaseItem = itemWithPoster.toShowcasePosterItem()
        assertNotNull(showcaseItem)
        assertEquals("Test Movie", showcaseItem?.title)
        assertEquals("Movie", showcaseItem?.genre)
        assertEquals("2026", showcaseItem?.year)
        assertEquals("https://image.tmdb.org/t/p/w780/test.jpg", showcaseItem?.posterUrl)
        assertEquals(com.euthopiar.R.drawable.avatar_classic_director, showcaseItem?.fallbackDrawableRes)

        val tvItemWithPoster = com.euthopiar.core.model.MediaItem(
            id = "125",
            title = "Test TV",
            url = "",
            posterUrl = "https://image.tmdb.org/t/p/w780/tv.jpg",
            type = MediaType.TV_SERIES,
            year = 2026
        )
        val tvShowcase = tvItemWithPoster.toShowcasePosterItem()
        assertEquals(com.euthopiar.R.drawable.avatar_retro_cinephile, tvShowcase?.fallbackDrawableRes)

        val itemWithoutPoster = com.euthopiar.core.model.MediaItem(
            id = "124",
            title = "No Poster",
            url = "",
            posterUrl = null,
            type = MediaType.TV_SERIES,
            year = 2025
        )
        assertNull(itemWithoutPoster.toShowcasePosterItem())
    }
}
