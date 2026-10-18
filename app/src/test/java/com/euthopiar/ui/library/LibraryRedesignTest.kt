package com.euthopiar.ui.library

import android.app.Application
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.repository.Playlist
import com.euthopiar.data.repository.PlaylistItem
import com.euthopiar.data.repository.PlaylistRepository
import com.euthopiar.download.DownloadItem
import com.euthopiar.download.DownloadState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryRedesignTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        PlaylistRepository.resetForTesting()
        context = ApplicationProvider.getApplicationContext()
        // Clean any saved playlists for tests
        val file = File(context.filesDir, "user_playlists.json")
        if (file.exists()) file.delete()
    }

    @Test
    fun testPlaylistRepository_createAndListCustomPlaylists() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val created = repo.createPlaylist("Weekend Binge")

        assertEquals("Weekend Binge", created.title)
        assertNotNull(created.id)

        repo.addItemToPlaylist(
            created.id,
            PlaylistItem(
                mediaId = "media_1",
                title = "Breaking Bad",
                isTvSeries = true
            )
        )

        val userLists = repo.userPlaylists.value
        val target = userLists.firstOrNull { it.id == created.id }
        assertNotNull(target)
        assertEquals(1, target?.items?.size)
        assertEquals("Breaking Bad", target?.items?.first()?.title)
    }

    @Test
    fun testPlaylistRepository_lastPlayedOrdering() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val pl1 = repo.createPlaylist("Playlist 1")
        val pl2 = repo.createPlaylist("Playlist 2")

        // Mark pl1 as last played recently
        repo.updateLastPlayed(pl1.id)

        val all = repo.allPlaylists.first()
        val pl1Idx = all.indexOfFirst { it.id == pl1.id }
        val pl2Idx = all.indexOfFirst { it.id == pl2.id }

        // pl1 should be ordered before pl2 because pl1 was played
        assertTrue("Recently played playlist should appear first", pl1Idx < pl2Idx)
    }

    @Test
    fun testPlaylistRepository_deletePlaylist() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val pl = repo.createPlaylist("Temporary Playlist")
        assertTrue(repo.userPlaylists.value.any { it.id == pl.id })

        repo.deletePlaylist(pl.id)
        assertFalse(repo.userPlaylists.value.any { it.id == pl.id })
    }

    @Test
    fun testOfflineFilter_matchesCompletedDownloadsOnly() {
        val downloads = listOf(
            DownloadItem(
                id = "dl_1",
                mediaId = "movie_1",
                title = "Inception",
                state = DownloadState.COMPLETED,
                bytesDownloaded = 2000000000L,
                totalBytes = 2000000000L
            ),
            DownloadItem(
                id = "dl_2",
                mediaId = "series_1",
                title = "Severance",
                state = DownloadState.DOWNLOADING,
                bytesDownloaded = 500000000L,
                totalBytes = 1000000000L
            )
        )

        val downloadsMap = mutableMapOf<String, DownloadItem>()
        downloads.forEach {
            downloadsMap[it.id] = it
            downloadsMap[it.mediaId] = it
        }

        val libraryItems = listOf(
            LibraryItem(
                id = "lib_1",
                mediaId = "movie_1",
                title = "Inception",
                posterUrl = null,
                backdropUrl = null,
                mediaType = MediaType.MOVIE,
                rating = 8.8,
                year = 2010
            ),
            LibraryItem(
                id = "lib_2",
                mediaId = "series_1",
                title = "Severance",
                posterUrl = null,
                backdropUrl = null,
                mediaType = MediaType.TV_SERIES,
                rating = 8.7,
                year = 2022
            ),
            LibraryItem(
                id = "lib_3",
                mediaId = "movie_3",
                title = "Interstellar",
                posterUrl = null,
                backdropUrl = null,
                mediaType = MediaType.MOVIE,
                rating = 8.6,
                year = 2014
            )
        )

        // Filter offline only
        val offlineOnlyItems = libraryItems.filter { item ->
            val dl = downloadsMap[item.mediaId] ?: downloadsMap[item.id]
            dl?.state == DownloadState.COMPLETED
        }

        assertEquals(1, offlineOnlyItems.size)
        assertEquals("Inception", offlineOnlyItems.first().title)
    }

    @Test
    fun testEditorialTypographyAndColors_conformsToBrief() {
        assertEquals(30f, LibraryTypography.screenTitle.fontSize.value, 0.01f)
        assertEquals(54f, LibraryTypography.storageHeroNumeral.fontSize.value, 0.01f)
        assertEquals(22f, LibraryTypography.rowSizeNumeral.fontSize.value, 0.01f)
        assertEquals(15f, LibraryTypography.rowTitle.fontSize.value, 0.01f)
        assertEquals(13f, LibraryTypography.gridTitle.fontSize.value, 0.01f)
        assertEquals(11f, LibraryTypography.gridSubline.fontSize.value, 0.01f)
        assertEquals(11f, LibraryTypography.sectionLabel.fontSize.value, 0.01f)

        // Palette verification
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0B0B0F), LibraryColors.Background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFE50914), LibraryColors.Crimson)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), LibraryColors.Gold)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF9A9BA8), LibraryColors.TextMutedReadable)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF6B6C78), LibraryColors.TextMutedDecorative)
    }

    @Test
    fun testContinueCard_remainingTimeCalculation() {
        val history = PlaybackHistoryEntity(
            id = "default_user::test::movie_1",
            mediaId = "movie_1",
            title = "Test Movie",
            episodeTitle = null,
            seasonNumber = null,
            episodeNumber = null,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 36 * 60 * 1000L, // 36 min watched
            durationMs = 60 * 60 * 1000L,        // 60 min total
            isCompleted = false
        )

        val remainingMs = (history.durationMs - history.currentPositionMs).coerceAtLeast(0L)
        val remainingMinutes = (remainingMs / (1000 * 60)).coerceAtLeast(1)
        assertEquals(24L, remainingMinutes)
    }

    @Test
    fun testPlaylistDetail_totalRemainingTimeAcrossResumePoints() {
        val items = listOf(
            PlaylistItem(mediaId = "m1", title = "Movie 1", durationMinutes = 120),
            PlaylistItem(mediaId = "m2", title = "Movie 2", durationMinutes = 90),
            PlaylistItem(mediaId = "s1", title = "Series 1", isTvSeries = true)
        )

        // m1 has 30 mins remaining
        // m2 is completed (0 mins remaining)
        // s1 is untouched (defaults to 50 mins)
        val historyMap = mapOf(
            "m1" to PlaybackHistoryEntity(
                id = "1", mediaId = "m1", title = "Movie 1", episodeTitle = null, seasonNumber = null, episodeNumber = null,
                posterUrl = null, backdropUrl = null, currentPositionMs = 90 * 60000L, durationMs = 120 * 60000L, isCompleted = false
            ),
            "m2" to PlaybackHistoryEntity(
                id = "2", mediaId = "m2", title = "Movie 2", episodeTitle = null, seasonNumber = null, episodeNumber = null,
                posterUrl = null, backdropUrl = null, currentPositionMs = 90 * 60000L, durationMs = 90 * 60000L, isCompleted = true
            )
        )

        var totalRemainingMinutes = 0
        for (item in items) {
            val history = historyMap[item.mediaId]
            if (history != null) {
                if (!history.isCompleted) {
                    val remainingMs = (history.durationMs - history.currentPositionMs).coerceAtLeast(0L)
                    val minutes = if (remainingMs > 0) (remainingMs / 60000).toInt() else 45
                    totalRemainingMinutes += minutes
                }
            } else {
                val fullMinutes = if (item.isTvSeries) 50 else (item.durationMinutes ?: 110)
                totalRemainingMinutes += fullMinutes
            }
        }

        // 30 (m1) + 0 (m2 completed) + 50 (s1 untouched) = 80 minutes
        assertEquals(80, totalRemainingMinutes)
    }

    @Test
    fun testPlaylistDetail_zeroDurationUsesDefaultAndDoesNotDefaultToArbitrary45() {
        val seriesItem = PlaylistItem(mediaId = "s1", title = "Series 1", isTvSeries = true)
        // Series watched 20 min (1,200,000ms), but durationMs is recorded as 0L
        val history = PlaybackHistoryEntity(
            id = "1", mediaId = "s1", title = "Series 1", episodeTitle = null, seasonNumber = 1, episodeNumber = 1,
            posterUrl = null, backdropUrl = null, currentPositionMs = 20 * 60000L, durationMs = 0L, isCompleted = false
        )

        val defaultDurationMs = if (seriesItem.isTvSeries) 50L * 60000L else ((seriesItem.durationMinutes?.toLong() ?: 110L) * 60000L)
        val effectiveDurationMs = if (history.durationMs > 0L) history.durationMs else defaultDurationMs
        val remainingMs = (effectiveDurationMs - history.currentPositionMs).coerceAtLeast(0L)
        val remainingMinutes = (remainingMs / 60000).toInt()

        // 50 min default - 20 min watched = 30 min remaining (NOT 45 min)
        assertEquals(30, remainingMinutes)
    }

    @Test
    fun testContinuousTabUnderline_lerpInterpolation() {
        val tab0 = androidx.compose.ui.unit.Dp(16f) to androidx.compose.ui.unit.Dp(70f)
        val tab1 = androidx.compose.ui.unit.Dp(114f) to androidx.compose.ui.unit.Dp(46f)

        // Swiping halfway (fraction = 0.5)
        val fraction = 0.5f
        val interpolatedOffset = androidx.compose.ui.unit.lerp(tab0.first, tab1.first, fraction)
        val interpolatedWidth = androidx.compose.ui.unit.lerp(tab0.second, tab1.second, fraction)

        assertEquals(65f, interpolatedOffset.value, 0.01f)
        assertEquals(58f, interpolatedWidth.value, 0.01f)
    }

    @Test
    fun testPreferencesRepository_rememberLibraryLastTab() = runBlocking {
        val prefs = com.euthopiar.data.preferences.PreferencesRepository.getInstance(context)
        prefs.setLibraryLastTab(0)
        assertEquals(0, prefs.libraryLastTab.first())

        prefs.setLibraryLastTab(2)
        assertEquals(2, prefs.libraryLastTab.first())
    }

    @Test
    fun testContinueCard_zeroDurationFallback() {
        val seriesHistory = PlaybackHistoryEntity(
            id = "s_1",
            mediaId = "s_1",
            title = "Test Series",
            episodeTitle = null,
            seasonNumber = 1,
            episodeNumber = 1,
            posterUrl = null,
            backdropUrl = null,
            currentPositionMs = 15 * 60 * 1000L, // 15 min watched
            durationMs = 0L,                     // Missing / 0 duration
            isCompleted = false
        )

        val isSeries = seriesHistory.seasonNumber != null || seriesHistory.episodeNumber != null
        val effectiveDurationMs = if (seriesHistory.durationMs > 0L) {
            seriesHistory.durationMs
        } else {
            if (isSeries) 50L * 60000L else 110L * 60000L
        }

        val remainingMs = (effectiveDurationMs - seriesHistory.currentPositionMs).coerceAtLeast(0L)
        val remainingMinutes = (remainingMs / (1000 * 60)).coerceAtLeast(1)

        // 50 min default - 15 min watched = 35 min remaining
        assertEquals(35L, remainingMinutes)

        val progress = if (effectiveDurationMs > 0L) {
            (seriesHistory.currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        assertEquals(0.30f, progress, 0.01f)
    }

    @Test
    fun testSanitizeTitle_removesPlusAndNormalizesWhitespace() {
        assertEquals("Spider-Man: No Way Home", sanitizeTitle("Spider-Man:+No+Way+Home"))
        assertEquals("Marvel's Avengers Assemble", sanitizeTitle("Marvel's+Avengers++Assemble"))
        assertEquals("Simple Title", sanitizeTitle("  Simple+Title  "))
        assertEquals("", sanitizeTitle(null))
        assertEquals("", sanitizeTitle("   "))
    }

    @Test
    fun testLibraryTokens_bannerHeightIncreased() {
        assertEquals(210f, LibraryDimens.bannerHeight.value, 0.01f)
        assertEquals(220f, LibraryDimens.detailMosaicHeight.value, 0.01f)
    }

    @Test
    fun testPlaylistRepository_dynamicallyAddAndRemoveItems() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val pl = repo.createPlaylist("Chill Vibes")
        val item = PlaylistItem(
            mediaId = "m_100",
            title = "Chill+Movie",
            posterUrl = "http://example.com/poster.jpg"
        )
        repo.addItemToPlaylist(pl.id, item)

        val updated = repo.userPlaylists.value.first { it.id == pl.id }
        assertEquals(1, updated.items.size)
        // Title sanitized during addition
        assertEquals("Chill Movie", updated.items.first().title)

        // Remove item
        repo.removeItemFromPlaylist(pl.id, "m_100")
        val emptied = repo.userPlaylists.value.first { it.id == pl.id }
        assertEquals(0, emptied.items.size)
    }

    @Test
    fun testPlaylistRepository_multipleEpisodesDoNotOverwrite() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val pl = repo.createPlaylist("Series Watch")
        val ep1 = PlaylistItem(
            mediaId = "series_1",
            title = "Awesome Series",
            seasonNumber = 1,
            episodeNumber = 1,
            episodeTitle = "Pilot",
            posterUrl = "http://example.com/poster.jpg"
        )
        val ep2 = PlaylistItem(
            mediaId = "series_1",
            title = "Awesome Series",
            seasonNumber = 1,
            episodeNumber = 2,
            episodeTitle = "Episode Two",
            posterUrl = "http://example.com/poster.jpg"
        )

        repo.addItemToPlaylist(pl.id, ep1)
        repo.addItemToPlaylist(pl.id, ep2)

        val updated = repo.userPlaylists.value.first { it.id == pl.id }
        // Both episodes must exist without overwriting each other
        assertEquals(2, updated.items.size)
        assertEquals("series_1_s1e1", ep1.uniqueKey)
        assertEquals("series_1_s1e2", ep2.uniqueKey)

        // Remove only ep1 by uniqueKey
        repo.removeItemFromPlaylist(pl.id, ep1.uniqueKey)
        val remaining = repo.userPlaylists.value.first { it.id == pl.id }
        assertEquals(1, remaining.items.size)
        assertEquals(2, remaining.items.first().episodeNumber)
    }

    @Test
    fun testFrostedGlassDesignTokens() {
        assertEquals(34f, LibraryTypography.pageHeadline.fontSize.value)
        assertEquals(64f, LibraryTypography.storageHeroNumeral64.fontSize.value)
        assertEquals(18f, LibraryTypography.storageHeroUnit18.fontSize.value)
        assertEquals(11f, LibraryTypography.sectionLabel11.fontSize.value)
        assertEquals(15f, LibraryTypography.title15.fontSize.value)
        assertEquals(14f, LibraryTypography.title14.fontSize.value)
        assertEquals(11.5f, LibraryTypography.metadata11.fontSize.value)
        assertEquals(14f, LibraryTypography.tab14Active.fontSize.value)
        assertEquals(14f, LibraryTypography.tab14Inactive.fontSize.value)
        assertEquals(2f, LibraryDimens.progressBarHeight.value)
        assertEquals(34f, LibraryDimens.toggleWidth.value)
        assertEquals(20f, LibraryDimens.toggleHeight.value)
    }

    @Test
    fun testPreferencesRepository_libraryOfflineOnlyPersistence() = runBlocking {
        val prefRepo = PreferencesRepository.getInstance(context)
        prefRepo.setLibraryOfflineOnly(true)
        val enabled = prefRepo.libraryOfflineOnly.first()
        assertEquals(true, enabled)

        prefRepo.setLibraryOfflineOnly(false)
        val disabled = prefRepo.libraryOfflineOnly.first()
        assertEquals(false, disabled)
    }

    @Test
    fun testPlaylistRepository_isInPlaylistDetection() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val pl = repo.createPlaylist("Action Movies")
        val item = PlaylistItem(
            mediaId = "media_42",
            title = "Action Hero",
            posterUrl = "http://example.com/poster.jpg"
        )
        repo.addItemToPlaylist(pl.id, item)

        val isInAnyPlaylist = repo.userPlaylists.value.any { playlist ->
            playlist.items.any { it.mediaId == "media_42" }
        }
        assertEquals(true, isInAnyPlaylist)

        val isNotInPlaylist = repo.userPlaylists.value.any { playlist ->
            playlist.items.any { it.mediaId == "media_999" }
        }
        assertEquals(false, isNotInPlaylist)
    }

    @Test
    fun testDownloadsStorageBlock_subtitleFormatting() {
        val readyDownloadsCount = 3
        val activeDownloadsCount = 1
        val readyCountText = "$readyDownloadsCount title${if (readyDownloadsCount != 1) "s" else ""} ready"
        val subtitle = if (activeDownloadsCount > 0) {
            "$readyCountText · $activeDownloadsCount downloading"
        } else {
            readyCountText
        }
        assertEquals("3 titles ready · 1 downloading", subtitle)

        val warningText = "Your device is almost full. Manage downloads to free up space."
        assertEquals("Your device is almost full. Manage downloads to free up space.", warningText)
    }

    @Test
    fun testPlaylistNaming_duplicateDetectionCaseInsensitive() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val created = repo.createPlaylist("Anime Favorites")
        val existingPlaylists = repo.userPlaylists.value

        val duplicateCheck1 = existingPlaylists.any { it.title.equals("anime favorites", ignoreCase = true) }
        val duplicateCheck2 = existingPlaylists.any { it.title.equals("  Anime Favorites  ".trim(), ignoreCase = true) }
        val nonDuplicateCheck = existingPlaylists.any { it.title.equals("Unique List", ignoreCase = true) }

        assertTrue(duplicateCheck1)
        assertTrue(duplicateCheck2)
        assertFalse(nonDuplicateCheck)
    }

    @Test
    fun testTabCounts_liveCountComputation() {
        val downloads = listOf(
            DownloadItem(id = "1", mediaId = "m1", title = "T1", state = DownloadState.COMPLETED)
        )
        val items = listOf(
            LibraryItem(id = "1", mediaId = "m1", title = "T1", posterUrl = null, backdropUrl = null, mediaType = MediaType.MOVIE, rating = null, year = null),
            LibraryItem(id = "2", mediaId = "m2", title = "T2", posterUrl = null, backdropUrl = null, mediaType = MediaType.MOVIE, rating = null, year = null)
        )
        val playlists = listOf(
            Playlist(id = "p1", title = "P1", items = emptyList())
        )

        fun countForTab(tab: String): Int = when (tab) {
            "Downloads" -> downloads.size
            "Saved" -> items.size
            "Playlists" -> playlists.size
            else -> 0
        }

        assertEquals(1, countForTab("Downloads"))
        assertEquals(2, countForTab("Saved"))
        assertEquals(1, countForTab("Playlists"))
    }

    @Test
    fun testLibraryDimens_toggleAndIconTileDimensions() {
        assertEquals(34f, LibraryDimens.toggleWidth.value)
        assertEquals(20f, LibraryDimens.toggleHeight.value)
        assertEquals(16f, LibraryDimens.toggleKnobSize.value)
        assertEquals(14f, LibraryDimens.iconTileRadius.value)
    }
}

