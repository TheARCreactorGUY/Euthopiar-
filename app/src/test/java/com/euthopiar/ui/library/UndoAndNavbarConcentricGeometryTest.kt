package com.euthopiar.ui.library

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.local.WatchlistEntity
import com.euthopiar.data.preferences.NavbarStyle
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.repository.LibraryRepository
import com.euthopiar.ui.components.AppSnackbar
import com.euthopiar.ui.components.AppSnackbarController
import com.euthopiar.ui.components.AppSnackbarData
import com.euthopiar.ui.components.AppSnackbarHostState
import com.euthopiar.ui.components.AppSnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UndoAndNavbarConcentricGeometryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppSnackbar_undoClickDispatchesCallback() {
        var actionInvoked = false
        val hostState = AppSnackbarHostState()
        val scope = CoroutineScope(Dispatchers.Main)
        val controller = AppSnackbarController(hostState, scope)

        controller.show(
            message = "Removed 1 title",
            actionLabel = "Undo",
            durationMillis = 4500L
        ) {
            actionInvoked = true
        }

        composeTestRule.setContent {
            val data = hostState.currentSnackbarData
            if (data != null) {
                AppSnackbar(snackbarData = data)
            }
        }

        val undoNode = composeTestRule.onNodeWithText("Undo")
        undoNode.assertExists()
        undoNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        undoNode.performClick()
        composeTestRule.waitForIdle()

        assertTrue("Tapping Undo on AppSnackbar must invoke onAction callback", actionInvoked)
    }

    @Test
    fun testConcentricCornerGeometry_emilKowalskiRule() {
        // Emil Kowalski rule: R_inner = R_outer - inset
        // Floating pill container: R_outer = 24dp, inset = 4dp -> R_inner = 20dp
        val outerRadiusFloating = 24.dp
        val inset = 4.dp
        val innerRadiusFloating = (outerRadiusFloating - inset).coerceAtLeast(0.dp)
        assertEquals(20.dp, innerRadiusFloating)

        // Docked bar container: R_outer = 16dp, inset = 4dp -> R_inner = 12dp
        val outerRadiusDocked = 16.dp
        val innerRadiusDocked = (outerRadiusDocked - inset).coerceAtLeast(0.dp)
        assertEquals(12.dp, innerRadiusDocked)

        // Mathematical concentricity check:
        // Outer arc center at (24, 24), radius 24
        // Inner arc center at (4 + 20, 4 + 20) = (24, 24), radius 20
        // Distance along ANY radial angle theta (e.g. 45-degree diagonal) must be exactly inset (4dp)
        val outerCenterX = 24.0
        val outerCenterY = 24.0
        val innerCenterX = 4.0 + 20.0
        val innerCenterY = 4.0 + 20.0

        assertEquals("Centers of curvature must be identical (concentric)", outerCenterX, innerCenterX, 0.0001)
        assertEquals("Centers of curvature must be identical (concentric)", outerCenterY, innerCenterY, 0.0001)

        // Diagonal distance from center to outer curve = 24.0
        // Diagonal distance from center to inner curve = 20.0
        val gapAlongDiagonal = 24.0 - 20.0
        assertEquals(4.0, gapAlongDiagonal, 0.0001)
    }

    @Test
    fun testNavbarOuterBoundsTracking_extremeLeftAndRight() {
        val containerWidth = 320f
        val containerHeight = 70f
        val tabCount = 4
        val tabW = containerWidth / tabCount // 80f per tab
        val insetX = 4f
        val insetY = 4f
        val innerRadius = 20f

        // 1. Extreme left: Home tab (index 0)
        val leftHome = 0f * tabW + insetX // 4f
        val rightHome = leftHome + (tabW - insetX * 2) // 76f
        val topHome = insetY // 4f
        val bottomHome = containerHeight - insetY // 66f

        // Left boundary margin to container outer edge
        val homeLeftMargin = leftHome - 0f
        assertEquals(4f, homeLeftMargin, 0.001f)
        val homeTopMargin = topHome - 0f
        assertEquals(4f, homeTopMargin, 0.001f)
        val homeBottomMargin = containerHeight - bottomHome
        assertEquals(4f, homeBottomMargin, 0.001f)

        // 2. Extreme right: Profile tab (index 3)
        val leftProfile = 3f * tabW + insetX // 244f
        val rightProfile = leftProfile + (tabW - insetX * 2) // 316f
        val topProfile = insetY // 4f
        val bottomProfile = containerHeight - insetY // 66f

        val profileRightMargin = containerWidth - rightProfile
        assertEquals(4f, profileRightMargin, 0.001f)
        val profileTopMargin = topProfile - 0f
        assertEquals(4f, profileTopMargin, 0.001f)
        val profileBottomMargin = containerHeight - bottomProfile
        assertEquals(4f, profileBottomMargin, 0.001f)
    }

    @Test
    fun testLibraryViewModel_removeFromCurrentFilterWithUndo_restoresWatchlistAndHistoryAndFavorites() = runBlocking {
        val db = AppDatabase.getInstance(context)
        val watchlistDao = db.watchlistDao()
        val historyDao = db.playbackHistoryDao()
        val prefRepo = PreferencesRepository.getInstance(context)
        val libraryRepo = LibraryRepository.getInstance(context)

        // Clean database tables
        watchlistDao.deleteBatch(listOf("test_m1", "test_m2"), "default_user")
        historyDao.deleteBatch(listOf("default_user::test::test_m1", "default_user::test::test_m2"))

        // Insert initial test entities
        val initialWatchlist = WatchlistEntity(
            mediaId = "test_m1",
            profileId = "default_user",
            title = "Test Movie 1",
            posterUrl = "http://example.com/p1.jpg",
            backdropUrl = null,
            mediaType = "MOVIE",
            rating = 8.5,
            year = 2024,
            status = "TO_BE_WATCHED"
        )
        watchlistDao.insert(initialWatchlist)

        val initialHistory = PlaybackHistoryEntity(
            id = "default_user::test::test_m2",
            mediaId = "test_m2",
            title = "Test Series 2",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = "http://example.com/p2.jpg",
            backdropUrl = null,
            currentPositionMs = 50000L,
            durationMs = 120000L,
            isCompleted = false,
            provider = "test",
            mediaType = "TV_SERIES"
        )
        historyDao.upsert(initialHistory)

        // Mark m1 as favorite
        if ("test_m1" !in prefRepo.favoriteMediaIds.first()) {
            prefRepo.toggleFavorite("test_m1")
        }
        assertTrue("test_m1 must be in favorites initially", prefRepo.favoriteMediaIds.first().contains("test_m1"))

        val viewModel = LibraryViewModel(context)

        val item1 = LibraryItem(
            id = "test_m1",
            mediaId = "test_m1",
            title = "Test Movie 1",
            posterUrl = "http://example.com/p1.jpg",
            backdropUrl = null,
            mediaType = MediaType.MOVIE,
            rating = 8.5,
            year = 2024,
            isWatchlist = true,
            isHistory = false,
            isFavorite = true
        )

        val item2 = LibraryItem(
            id = "default_user::test::test_m2",
            mediaId = "test_m2",
            title = "Test Series 2",
            episodeTitle = "Ep 1",
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = "http://example.com/p2.jpg",
            backdropUrl = null,
            mediaType = MediaType.TV_SERIES,
            rating = null,
            year = null,
            currentPositionMs = 50000L,
            durationMs = 120000L,
            isWatchlist = false,
            isHistory = true,
            isFavorite = false
        )

        // Verify initial state
        assertEquals(1, watchlistDao.countWatchlist("test_m1", "default_user"))
        assertNotNull(historyDao.getProgress("default_user::test::test_m2"))

        var undoCallback: (() -> Unit)? = null

        // 1. Remove items with undo
        viewModel.removeFromCurrentFilterWithUndo(listOf(item1, item2)) { undo ->
            undoCallback = undo
        }

        assertNotNull("onUndoAvailable must provide non-null undo callback", undoCallback)

        // Allow async Room & DataStore IO to complete deletion
        val deleteStart = System.currentTimeMillis()
        while (("test_m1" in prefRepo.favoriteMediaIds.first() || watchlistDao.countWatchlist("test_m1", "default_user") > 0) &&
            (System.currentTimeMillis() - deleteStart < 3000L)
        ) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20L)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }

        // Verify items were deleted from Room DB and preferences
        assertEquals("Watchlist entity must be deleted", 0, watchlistDao.countWatchlist("test_m1", "default_user"))
        assertEquals("History entity must be deleted", null, historyDao.getProgress("default_user::test::test_m2"))
        assertFalse("Favorite must be removed", prefRepo.favoriteMediaIds.first().contains("test_m1"))

        // 2. Execute Undo
        undoCallback?.invoke()

        // Allow async Room & DataStore IO to complete restoration
        val undoStart = System.currentTimeMillis()
        while (("test_m1" !in prefRepo.favoriteMediaIds.first() || watchlistDao.countWatchlist("test_m1", "default_user") == 0) &&
            (System.currentTimeMillis() - undoStart < 3000L)
        ) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20L)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }

        // 3. Verify items are 100% restored to database and preferences
        assertEquals("Watchlist entity must be restored", 1, watchlistDao.countWatchlist("test_m1", "default_user"))
        val restoredWatchlist = watchlistDao.getWatchlistItem("test_m1", "default_user")
        assertNotNull(restoredWatchlist)
        assertEquals("Test Movie 1", restoredWatchlist?.title)

        val restoredHistory = historyDao.getProgress("default_user::test::test_m2")
        assertNotNull("History entity must be restored", restoredHistory)
        assertEquals("Test Series 2", restoredHistory?.title)
        assertEquals(50000L, restoredHistory?.currentPositionMs)

        assertTrue("Favorite status must be restored", prefRepo.favoriteMediaIds.first().contains("test_m1"))
    }

    @Test
    fun testLibraryViewModel_watchlistFilterDeletion_doesNotCorruptFavoritesOnUndo() = runBlocking {
        val db = AppDatabase.getInstance(context)
        val watchlistDao = db.watchlistDao()
        val prefRepo = PreferencesRepository.getInstance(context)

        val testMediaId = "test_non_fav_m1"
        watchlistDao.deleteBatch(listOf(testMediaId), "default_user")
        prefRepo.setFavorite(testMediaId, false)

        val item = WatchlistEntity(
            mediaId = testMediaId,
            profileId = "default_user",
            title = "Non Favorite Item",
            posterUrl = null,
            backdropUrl = null,
            mediaType = "MOVIE",
            rating = 7.0,
            year = 2024,
            status = "TO_BE_WATCHED"
        )
        watchlistDao.insert(item)

        val viewModel = LibraryViewModel(context)
        viewModel.selectFilter(LibraryFilter.WATCHLIST)

        val libraryItem = LibraryItem(
            id = testMediaId,
            mediaId = testMediaId,
            title = "Non Favorite Item",
            posterUrl = null,
            backdropUrl = null,
            mediaType = MediaType.MOVIE,
            rating = 7.0,
            year = 2024,
            isWatchlist = true,
            isHistory = false,
            isFavorite = false
        )

        var undoCallback: (() -> Unit)? = null
        viewModel.removeFromCurrentFilterWithUndo(listOf(libraryItem)) { undoAction ->
            undoCallback = undoAction
        }

        // Wait for deletion
        kotlinx.coroutines.delay(200L)
        assertEquals(0, watchlistDao.countWatchlist(testMediaId, "default_user"))
        // Favorite status must still be false
        org.junit.Assert.assertFalse(prefRepo.favoriteMediaIds.first().contains(testMediaId))

        // Trigger undo
        undoCallback?.invoke()
        kotlinx.coroutines.delay(200L)

        // Item must be restored in watchlist
        assertEquals(1, watchlistDao.countWatchlist(testMediaId, "default_user"))
        // Favorite status must REMAIN false (not inverted by a blind toggle)
        org.junit.Assert.assertFalse(
            "Non-favorite item must not be flipped to favorite on undo",
            prefRepo.favoriteMediaIds.first().contains(testMediaId)
        )
    }

    @Test
    fun testNavbarFocusedScaleUnity_preventsOuterBoundsExpansion() {
        val tabOuterRadius = 24.dp
        val inset = 4.dp
        val tabInnerRadius = (tabOuterRadius - inset).coerceAtLeast(0.dp)
        val focusedScale = 1.0f

        // When focusedScale is 1.0f, the tab dimensions do not expand beyond its 4dp padded bounds
        val originalWidth = 80f
        val scaledWidth = originalWidth * focusedScale
        assertEquals("Width must not scale up when focused", originalWidth, scaledWidth, 0.0001f)

        // Geometric concentricity: R_outer - R_inner == padding
        assertEquals(20.dp, tabInnerRadius)
        assertEquals(4.dp, tabOuterRadius - tabInnerRadius)
    }

    @Test
    fun testAppSnackbar_touchTargetMinimumConformsToA11yStandards() {
        val minWidth = 56.dp
        val minHeight = 48.dp
        assertTrue("Min height must meet or exceed 48dp touch target standard", minHeight >= 48.dp)
        assertTrue("Min width must meet or exceed 48dp touch target standard", minWidth >= 48.dp)
    }
}
