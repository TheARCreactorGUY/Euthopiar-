package com.euthopiar.data.local

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.ui.details.sheets.MediaDetailsActionSheet
import com.euthopiar.ui.navigation.LocalNavbarVisible
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContinueWatchingDeduplicationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Application
    private lateinit var db: AppDatabase
    private lateinit var dao: PlaybackHistoryDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getInstance(context)
        dao = db.playbackHistoryDao()
        runBlocking {
            dao.clearHistory("test_profile")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dao.clearHistory("test_profile")
        }
    }

    @Test
    fun testObserveAllContinueWatching_deduplicatesEpisodesByMediaIdAndSelectsLatest() = runBlocking {
        val showId = "series_breaking_bad"

        // S1E1 watched at timestamp 1000
        val ep1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 1, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Breaking Bad",
            episodeTitle = "Pilot",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 15000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            mediaType = "TV_SERIES",
            lastWatchedTimestamp = 1000L
        )

        // S1E2 watched at timestamp 2000
        val ep2 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 2, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Breaking Bad",
            episodeTitle = "Cat's in the Bag...",
            seasonNumber = 1,
            episodeNumber = 2,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 25000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            mediaType = "TV_SERIES",
            lastWatchedTimestamp = 2000L
        )

        // S1E3 watched at timestamp 3000
        val ep3 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 3, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Breaking Bad",
            episodeTitle = "...And the Bag's in the River",
            seasonNumber = 1,
            episodeNumber = 3,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 30000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            mediaType = "TV_SERIES",
            lastWatchedTimestamp = 3000L
        )

        dao.upsertBatch(listOf(ep1, ep2, ep3))

        val cwList = dao.observeAllContinueWatching("test_profile").first()

        assertEquals("Only one episode card per mediaId should be returned", 1, cwList.size)
        val selectedEpisode = cwList.first()
        assertEquals(showId, selectedEpisode.mediaId)
        assertEquals(3, selectedEpisode.episodeNumber)
        assertEquals(3000L, selectedEpisode.lastWatchedTimestamp)
    }

    @Test
    fun testObserveContinueWatching_withProviderFilter_deduplicatesAndFiltersCorrectly() = runBlocking {
        val showA = "show_netflix_a"
        val showB = "show_hulu_b"

        val netflixEp1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showA, 1, 1, "test_profile"),
            mediaId = showA,
            profileId = "test_profile",
            title = "Show A",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 5000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 1000L
        )
        val netflixEp2 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showA, 1, 2, "test_profile"),
            mediaId = showA,
            profileId = "test_profile",
            title = "Show A",
            episodeTitle = "Ep 2",
            seasonNumber = 1,
            episodeNumber = 2,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 8000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 5000L
        )
        val huluEp1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("hulu", showB, 1, 1, "test_profile"),
            mediaId = showB,
            profileId = "test_profile",
            title = "Show B",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 12000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "hulu",
            lastWatchedTimestamp = 4000L
        )

        dao.upsertBatch(listOf(netflixEp1, netflixEp2, huluEp1))

        val netflixCw = dao.observeContinueWatching("netflix", "test_profile").first()
        assertEquals(1, netflixCw.size)
        assertEquals(showA, netflixCw.first().mediaId)
        assertEquals(2, netflixCw.first().episodeNumber)

        val allCw = dao.observeContinueWatching(null, "test_profile").first()
        assertEquals(2, allCw.size)
        assertEquals(showA, allCw[0].mediaId) // timestamp 5000
        assertEquals(showB, allCw[1].mediaId) // timestamp 4000
    }

    @Test
    fun testMediaDetailsActionSheet_controlsLocalNavbarVisibility() {
        val navbarVisibleState = mutableStateOf(true)
        val showSheet = mutableStateOf(true)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalNavbarVisible provides navbarVisibleState) {
                if (showSheet.value) {
                    MediaDetailsActionSheet(
                        mediaId = "test_media_1",
                        title = "Test Sheet",
                        onDismiss = { showSheet.value = false }
                    )
                }
            }
        }

        // When sheet is composed, navbarVisible must be false
        assertFalse("Navbar should be hidden while action sheet is open", navbarVisibleState.value)

        // Dismiss sheet
        showSheet.value = false
        composeTestRule.waitForIdle()

        // When sheet is disposed, navbarVisible must be restored to true
        assertTrue("Navbar should be visible when action sheet is dismissed", navbarVisibleState.value)
    }

    @Test
    fun testCompletedEpisode_notReturnedInContinueWatching() = runBlocking {
        val showId = "series_completed_show"

        // Ep 1 watched at 1000, uncompleted
        val ep1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 1, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Completed Show",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 15000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 1000L
        )
        // Ep 2 watched at 2000, completed!
        val ep2 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 2, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Completed Show",
            episodeTitle = "Ep 2",
            seasonNumber = 1,
            episodeNumber = 2,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 60000L,
            durationMs = 60000L,
            isCompleted = true,
            provider = "netflix",
            lastWatchedTimestamp = 2000L
        )

        dao.upsertBatch(listOf(ep1, ep2))

        val cwList = dao.observeAllContinueWatching("test_profile").first()
        assertTrue("If latest episode is completed, show should not appear in continue watching", cwList.isEmpty())
    }

    @Test
    fun testDismissedEpisode_notReturnedInContinueWatching() = runBlocking {
        val showId = "series_dismissed_show"

        val ep1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 1, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Dismissed Show",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 15000L,
            durationMs = 60000L,
            isCompleted = false,
            isDismissedFromContinueWatching = true,
            provider = "netflix",
            lastWatchedTimestamp = 1000L
        )

        dao.upsert(ep1)

        val cwList = dao.observeAllContinueWatching("test_profile").first()
        assertTrue("Dismissed episode should not appear in continue watching", cwList.isEmpty())
    }

    @Test
    fun testObserveContinueWatching_outOfOrderWatchTimestamps_selectsHighestTimestamp() = runBlocking {
        val showId = "series_out_of_order"

        // User watched Ep 1 at t=1000, Ep 3 at t=3000, Ep 2 at t=2000
        val ep1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 1, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Out of Order",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 5000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 1000L
        )
        val ep2 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 2, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Out of Order",
            episodeTitle = "Ep 2",
            seasonNumber = 1,
            episodeNumber = 2,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 5000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 2000L
        )
        val ep3 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("netflix", showId, 1, 3, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Out of Order",
            episodeTitle = "Ep 3",
            seasonNumber = 1,
            episodeNumber = 3,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 5000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "netflix",
            lastWatchedTimestamp = 3000L
        )

        dao.upsertBatch(listOf(ep1, ep3, ep2))

        val cwList = dao.observeAllContinueWatching("test_profile").first()
        assertEquals(1, cwList.size)
        assertEquals(3, cwList.first().episodeNumber)
        assertEquals(3000L, cwList.first().lastWatchedTimestamp)
    }

    @Test
    fun testObserveContinueWatching_thresholdAndProviderCaseInsensitivity() = runBlocking {
        val showId = "series_threshold_test"

        // Ep 1 watched <= 1000ms (e.g. 1000ms exact)
        val ep1 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("NetFLIX", showId, 1, 1, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Threshold Show",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 1000L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "NetFLIX",
            lastWatchedTimestamp = 1000L
        )
        dao.upsert(ep1)

        val cwEmpty = dao.observeContinueWatching("netflix", "test_profile").first()
        assertTrue("Position <= 1000ms must be excluded", cwEmpty.isEmpty())

        // Ep 2 watched > 1000ms
        val ep2 = PlaybackHistoryEntity(
            id = PlaybackHistoryEntity.buildId("NetFLIX", showId, 1, 2, "test_profile"),
            mediaId = showId,
            profileId = "test_profile",
            title = "Threshold Show",
            episodeTitle = "Ep 2",
            seasonNumber = 1,
            episodeNumber = 2,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 1001L,
            durationMs = 60000L,
            isCompleted = false,
            provider = "NetFLIX",
            lastWatchedTimestamp = 2000L
        )
        dao.upsert(ep2)

        val cwMatch = dao.observeContinueWatching("netflix", "test_profile").first()
        assertEquals(1, cwMatch.size)
        assertEquals(2, cwMatch.first().episodeNumber)
    }
}
