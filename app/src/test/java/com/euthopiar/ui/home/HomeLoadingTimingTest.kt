package com.euthopiar.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.components.ErrorState
import com.euthopiar.ui.theme.EuthopiarTheme
import com.euthopiar.ui.util.UserError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeLoadingTimingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoadingStateNeverDisplaysSourceError() {
        val uiState = mutableStateOf<HomeUiState>(HomeUiState.Loading)

        composeTestRule.setContent {
            EuthopiarTheme {
                Crossfade(
                    targetState = uiState.value,
                    animationSpec = tween(durationMillis = 150),
                    label = "HomeScreenTransition"
                ) { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is HomeUiState.Error -> {
                            ErrorState(
                                error = state.error,
                                onRetry = {}
                            )
                        }
                        is HomeUiState.Success -> {
                            Text("Success Content")
                        }
                    }
                }
            }
        }

        // On initial loading, no error text must exist
        composeTestRule.onNodeWithText("This source isn't working").assertDoesNotExist()
        composeTestRule.onNodeWithText("Something went wrong").assertDoesNotExist()

        // Transition directly to Success
        val sampleItem = MediaItem(id = "1", title = "Movie", url = "", posterUrl = null, type = MediaType.MOVIE)
        val catalogRows = listOf(CatalogRow(title = "Trending", items = listOf(sampleItem)))
        uiState.value = HomeUiState.Success(
            heroItems = listOf(sampleItem),
            catalogRows = catalogRows,
            availableTabs = listOf(HomeFilterTab.ALL),
            filteredRows = catalogRows,
            rowsByTab = mapOf(HomeFilterTab.ALL to catalogRows)
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Success Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("This source isn't working").assertDoesNotExist()
        composeTestRule.onNodeWithText("Something went wrong").assertDoesNotExist()
    }

    @Test
    fun testHomeUiStateTransitionsPreserveCatalogAndTypeSafety() {
        val sampleItem = MediaItem(
            id = "test_item_1",
            title = "Test Movie",
            url = "",
            posterUrl = null,
            type = MediaType.MOVIE
        )
        val catalogRows = listOf(
            CatalogRow(title = "Trending Now", items = listOf(sampleItem))
        )

        val successState = HomeUiState.Success(
            heroItems = listOf(sampleItem),
            catalogRows = catalogRows,
            availableTabs = listOf(HomeFilterTab.ALL, HomeFilterTab.MOVIES),
            filteredRows = catalogRows,
            rowsByTab = mapOf(HomeFilterTab.ALL to catalogRows)
        )

        assertEquals(1, successState.catalogRows.size)
        assertEquals("Trending Now", successState.catalogRows.first().title)

        val errorState = HomeUiState.Error(
            error = UserError.SourceUnavailable,
            message = "Source error"
        )
        assertTrue(errorState.error is UserError.SourceUnavailable)
    }
}
