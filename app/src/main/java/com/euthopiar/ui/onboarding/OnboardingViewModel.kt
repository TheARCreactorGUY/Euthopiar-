package com.euthopiar.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.core.util.TmdbBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class OnboardingPostersState(
    val topRowPosters: List<ShowcasePosterItem> = ShowcaseRowTop,
    val bottomRowPosters: List<ShowcasePosterItem> = ShowcaseRowBottom,
    val previewPosters: List<ShowcasePosterItem> = ShowcaseRowTop.take(5),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel @JvmOverloads constructor(
    application: Application,
    private val client: OkHttpClient? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _postersState = MutableStateFlow(OnboardingPostersState(isLoading = true))
    val postersState: StateFlow<OnboardingPostersState> = _postersState.asStateFlow()

    init {
        loadPosters()
    }

    fun loadPosters() {
        viewModelScope.launch(ioDispatcher) {
            _postersState.value = _postersState.value.copy(isLoading = true, errorMessage = null)
            try {
                val showcase = if (client != null) {
                    TmdbBridge.fetchOnboardingShowcasePosters(client = client, dispatcher = ioDispatcher)
                } else {
                    TmdbBridge.fetchOnboardingShowcasePosters(dispatcher = ioDispatcher)
                }

                val moviePosters = showcase.trendingMovies.mapNotNull { it.toShowcasePosterItem() }
                val tvPosters = showcase.trendingTv.mapNotNull { it.toShowcasePosterItem() }
                val topTenPosters = showcase.topTen.mapNotNull { it.toShowcasePosterItem() }
                val mixedPosters = showcase.mixed.mapNotNull { it.toShowcasePosterItem() }

                // Row 1 on Page 1: Dynamic mix of Trending Movies and Top 10 items
                val topRow = mutableListOf<ShowcasePosterItem>()
                val seenTop = mutableSetOf<String>()
                fun addTop(item: ShowcasePosterItem) {
                    if (seenTop.add(item.title)) topRow.add(item)
                }
                val topLimit = maxOf(moviePosters.size, topTenPosters.size)
                for (i in 0 until topLimit) {
                    if (i < moviePosters.size) addTop(moviePosters[i])
                    if (i < topTenPosters.size) addTop(topTenPosters[i])
                }

                // Row 2 on Page 1: Dynamic mix of Trending TV Shows and Top 10 items
                val bottomRow = mutableListOf<ShowcasePosterItem>()
                val seenBottom = mutableSetOf<String>()
                fun addBottom(item: ShowcasePosterItem) {
                    if (seenBottom.add(item.title)) bottomRow.add(item)
                }
                val bottomLimit = maxOf(tvPosters.size, topTenPosters.size)
                for (i in 0 until bottomLimit) {
                    if (i < tvPosters.size) addBottom(tvPosters[i])
                    if (i < topTenPosters.size) addBottom(topTenPosters[i])
                }

                val finalTop = if (topRow.isNotEmpty()) topRow else ShowcaseRowTop
                val finalBottom = if (bottomRow.isNotEmpty()) bottomRow else ShowcaseRowBottom
                val finalPreview = if (mixedPosters.isNotEmpty()) mixedPosters.take(8) else ShowcaseRowTop.take(5)

                _postersState.value = OnboardingPostersState(
                    topRowPosters = finalTop,
                    bottomRowPosters = finalBottom,
                    previewPosters = finalPreview,
                    isLoading = false,
                    isSuccess = moviePosters.isNotEmpty() || tvPosters.isNotEmpty() || topTenPosters.isNotEmpty(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _postersState.value = OnboardingPostersState(
                    topRowPosters = ShowcaseRowTop,
                    bottomRowPosters = ShowcaseRowBottom,
                    previewPosters = ShowcaseRowTop.take(5),
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
