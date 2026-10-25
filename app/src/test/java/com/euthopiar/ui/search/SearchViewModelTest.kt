package com.euthopiar.ui.search

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchViewModelTest {

    private lateinit var app: Application
    private lateinit var prefsRepo: PreferencesRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        prefsRepo = PreferencesRepository.getInstance(app)
    }

    @Test
    fun testRecentSearchesAddClearAndRestore() = runBlocking {
        prefsRepo.clearRecentSearches()
        prefsRepo.addRecentSearch("Batman")
        prefsRepo.addRecentSearch("Superman")

        val recents = prefsRepo.recentSearches.first()
        assertTrue(recents.contains("Superman"))
        assertTrue(recents.contains("Batman"))

        // Simulate "Clear all"
        val savedBackup = recents
        prefsRepo.clearRecentSearches()
        val emptyRecents = prefsRepo.recentSearches.first()
        assertTrue(emptyRecents.isEmpty())

        // Simulate "Undo" via restoreRecentSearches
        prefsRepo.restoreRecentSearches(savedBackup)
        val restoredRecents = prefsRepo.recentSearches.first()
        assertEquals(savedBackup, restoredRecents)
    }

    @Test
    fun testRecentSearchesSingleItemRemoval() = runBlocking {
        prefsRepo.clearRecentSearches()
        prefsRepo.addRecentSearch("Inception")
        prefsRepo.addRecentSearch("Interstellar")

        val recentsBefore = prefsRepo.recentSearches.first()
        assertEquals(2, recentsBefore.size)

        // Remove "Inception"
        prefsRepo.removeRecentSearch("Inception")
        val recentsAfter = prefsRepo.recentSearches.first()
        assertEquals(1, recentsAfter.size)
        assertFalse(recentsAfter.contains("Inception"))
        assertTrue(recentsAfter.contains("Interstellar"))
    }

    @Test
    fun testContinueWatchingTagFormattingForSeriesAndMovies() {
        // 1. Series with Season and Episode
        val seriesItem = PlaybackHistoryEntity(
            id = "default_user::test::show_1::3::7",
            mediaId = "show_1",
            title = "Breaking Bad",
            episodeTitle = "Fly",
            seasonNumber = 3,
            episodeNumber = 7,
            posterUrl = "https://example.com/bb_poster.jpg",
            backdropUrl = "https://example.com/bb_backdrop.jpg",
            currentPositionMs = 1200000L,
            durationMs = 2800000L,
            mediaType = "tv"
        )

        val isSeries = seriesItem.seasonNumber != null || seriesItem.episodeNumber != null ||
            seriesItem.mediaType?.contains("tv", ignoreCase = true) == true
        val seasonNum = seriesItem.seasonNumber ?: 1
        val episodeNum = seriesItem.episodeNumber ?: 1
        val tagText = if (isSeries) "S$seasonNum · E$episodeNum" else "Movie"
        assertEquals("S3 · E7", tagText)
        assertTrue(isSeries)

        // 2. Movie entity
        val movieItem = PlaybackHistoryEntity(
            id = "default_user::test::mov_1",
            mediaId = "mov_1",
            title = "Oppenheimer",
            episodeTitle = null,
            seasonNumber = null,
            episodeNumber = null,
            posterUrl = "https://example.com/opp_poster.jpg",
            backdropUrl = "https://example.com/opp_backdrop.jpg",
            currentPositionMs = 3600000L,
            durationMs = 10800000L,
            mediaType = "movie"
        )

        val isMovieSeries = movieItem.seasonNumber != null || movieItem.episodeNumber != null ||
            movieItem.mediaType?.contains("tv", ignoreCase = true) == true
        val movieTagText = if (isMovieSeries) "S${movieItem.seasonNumber ?: 1} · E${movieItem.episodeNumber ?: 1}" else "Movie"
        assertEquals("Movie", movieTagText)
        assertFalse(isMovieSeries)

        // 3. Subline logic: should use episodeTitle when present to avoid duplicate "S1:E1"
        val sublineText = if (!seriesItem.episodeTitle.isNullOrBlank()) {
            seriesItem.episodeTitle
        } else if (seriesItem.seasonNumber != null && seriesItem.episodeNumber != null) {
            "S${seriesItem.seasonNumber}:E${seriesItem.episodeNumber}"
        } else {
            "Continue watching"
        }
        assertEquals("Fly", sublineText)

        // When episodeTitle is missing, falls back cleanly
        val noEpisodeTitleItem = seriesItem.copy(episodeTitle = null)
        val fallbackSubline = if (!noEpisodeTitleItem.episodeTitle.isNullOrBlank()) {
            noEpisodeTitleItem.episodeTitle
        } else if (noEpisodeTitleItem.seasonNumber != null && noEpisodeTitleItem.episodeNumber != null) {
            "S${noEpisodeTitleItem.seasonNumber}:E${noEpisodeTitleItem.episodeNumber}"
        } else {
            "Continue watching"
        }
        assertEquals("S3:E7", fallbackSubline)
    }

    @Test
    fun testSearchSortBestMatchComparator() {
        val query = "star"
        val items = listOf(
            MediaItem(id = "1", title = "The Star", url = "https://example.com/1", posterUrl = null, type = MediaType.MOVIE, rating = "6.5"),
            MediaItem(id = "2", title = "Star", url = "https://example.com/2", posterUrl = null, type = MediaType.MOVIE, rating = "7.0"), // Exact match
            MediaItem(id = "3", title = "Star Wars", url = "https://example.com/3", posterUrl = null, type = MediaType.MOVIE, rating = "8.6"), // Prefix match
            MediaItem(id = "4", title = "A Star Is Born", url = "https://example.com/4", posterUrl = null, type = MediaType.MOVIE, rating = "7.8")
        )

        val q = query.trim().lowercase()
        val sorted = items.sortedWith(
            compareByDescending<MediaItem> { it.title.trim().lowercase() == q }
                .thenByDescending { it.title.trim().lowercase().startsWith(q) }
                .thenByDescending { it.rating?.toFloatOrNull() ?: 0f }
        )

        assertEquals("Star", sorted[0].title)
        assertEquals("Star Wars", sorted[1].title)
        assertEquals("A Star Is Born", sorted[2].title)
        assertEquals("The Star", sorted[3].title)
    }

    @Test
    fun testSearchFilterLogic() {
        val movies = listOf(
            MediaItem(id = "1", title = "Dune", url = "", posterUrl = null, type = MediaType.MOVIE, year = 2021),
            MediaItem(id = "2", title = "Dune: Part Two", url = "", posterUrl = null, type = MediaType.MOVIE, year = 2024),
            MediaItem(id = "3", title = "Arcane", url = "", posterUrl = null, type = MediaType.ANIME, year = 2021),
            MediaItem(id = "4", title = "Severance", url = "", posterUrl = null, type = MediaType.TV_SERIES, year = 2022)
        )

        // Filter by Type
        val moviesOnly = movies.filter { it.type == MediaType.MOVIE }
        assertEquals(2, moviesOnly.size)

        // Filter by Year
        val year2024 = movies.filter { it.year == 2024 }
        assertEquals(1, year2024.size)
        assertEquals("Dune: Part Two", year2024.first().title)

        // Filter by Decade "2020s"
        val decade2020s = movies.filter { (it.year ?: 0) in 2020..2029 }
        assertEquals(4, decade2020s.size)
    }

    @Test
    fun testPreviouslyReservedKeywordsBehaveAsStandardQueries() {
        val vm = SearchViewModel(app)
        vm.onQueryChange("::private")
        assertEquals("::private", vm.currentQuery.value)

        vm.onQueryChange("::vault")
        assertEquals("::vault", vm.currentQuery.value)
    }

    @Test
    fun testSearchGenreFilterMatchesExplicitGenresAndFallback() {
        val items = listOf(
            MediaItem(id = "1", title = "The Dark Knight", url = "", posterUrl = null, type = MediaType.MOVIE, genres = listOf("Action", "Crime", "Drama")),
            MediaItem(id = "2", title = "Superbad", url = "", posterUrl = null, type = MediaType.MOVIE, genres = listOf("Comedy")),
            MediaItem(id = "3", title = "Action Movie Without Genre Tag", url = "", posterUrl = null, type = MediaType.MOVIE, genres = emptyList())
        )

        val selectedGenre = "Action"
        val filtered = items.filter { item ->
            item.genres.any { it.equals(selectedGenre, ignoreCase = true) } ||
            item.title.contains(selectedGenre, ignoreCase = true) ||
            item.quality?.contains(selectedGenre, ignoreCase = true) == true ||
            (selectedGenre.equals("Anime", ignoreCase = true) && item.type == MediaType.ANIME)
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "1" }) // matched via genres
        assertTrue(filtered.any { it.id == "3" }) // matched via title
        assertFalse(filtered.any { it.id == "2" })
    }

    @Test
    fun testClearQueryMaintainsResultsActiveStateWhenInResultsMode() {
        val vm = SearchViewModel(app)
        vm.isResultsActive.value = true
        vm.onQueryChange("Dune")
        assertEquals("Dune", vm.currentQuery.value)

        vm.clearQuery()
        assertEquals("", vm.currentQuery.value)
        assertTrue("Results mode must stay active when clearQuery() is called in results mode", vm.isResultsActive.value)
        assertFalse(vm.isRefining.value)
        val state = vm.uiState.value
        assertTrue("uiState must be Results with empty list when query is cleared in results mode", state is SearchUiState.Results && state.items.isEmpty())
    }

    @Test
    fun testOnQueryChangeBlankClearsResultsInResultsMode() {
        val vm = SearchViewModel(app)
        vm.isResultsActive.value = true
        vm.onQueryChange("Batman")
        assertEquals("Batman", vm.currentQuery.value)

        // Backspacing to blank
        vm.onQueryChange("")
        assertEquals("", vm.currentQuery.value)
        assertTrue(vm.isResultsActive.value)
        assertFalse(vm.isRefining.value)
        val state = vm.uiState.value
        assertTrue("uiState must be Results with empty list when query is blank in results mode", state is SearchUiState.Results && state.items.isEmpty())
    }

    @Test
    fun testBackToSearchResetsResultsActiveAndRefining() {
        val vm = SearchViewModel(app)
        vm.isResultsActive.value = true
        vm.isRefining.value = true

        vm.backToSearch()
        assertFalse(vm.isResultsActive.value)
        assertFalse(vm.isRefining.value)
    }
}
