package com.euthopiar.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.R
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.components.OfflineBanner
import com.euthopiar.ui.components.VelvetAlertDialog
import com.euthopiar.ui.navigation.LocalNavbarHazeState
import com.euthopiar.ui.navigation.LocalTabReselection
import com.euthopiar.ui.navigation.NavTab
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.rememberWindowDimensions
import com.euthopiar.ui.util.findActivity
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ALL_TYPE_OPTIONS = listOf("All types", "Movies", "Series", "Anime")
private val GENRE_OPTIONS = listOf(
    "All genres", "Action", "Comedy", "Drama", "Sci-Fi", "Animation",
    "Thriller", "Horror", "Fantasy", "Romance", "Crime", "Mystery",
    "Adventure", "Documentaries"
)
private val YEAR_OPTIONS = listOf(
    "Any year", "2025", "2024", "2023", "2022", "2021", "2020",
    "2010s", "2000s", "1990s", "Classic"
)

@Composable
fun SearchScreen(
    onMediaClick: (MediaItem) -> Unit,
    onPlayMedia: ((MediaItem) -> Unit)? = null,
    onNavigateToPluginManager: (() -> Unit)? = null,
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.currentQuery.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isResultsActive by viewModel.isResultsActive.collectAsState()
    val selectedType by viewModel.selectedTypeFilter.collectAsState()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsState()
    val selectedYear by viewModel.selectedYearFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val cachedTrending by viewModel.cachedTrendingItems.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarController = LocalSnackbarController.current
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current

    val searchHazeState = rememberHazeState()

    val preferencesRepository = remember { com.euthopiar.data.preferences.PreferencesRepository.getInstance(context) }
    val profileManager = remember { com.euthopiar.data.profile.ProfileManager.getInstance(context) }
    val activeProfile by profileManager.activeProfile.collectAsState()
    val windowDimensions = rememberWindowDimensions()
    val scope = rememberCoroutineScope()
    // Dropdown popover state (only one open at a time)
    var openDropdown by remember { mutableStateOf<String?>(null) } // "type", "genre", "year", "sort", null

    // Search input focus state for border highlight
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // First entrance animation state
    var hasPlayedEntrance by rememberSaveable { mutableStateOf(false) }
    val headlineAnim1 = remember { Animatable(if (reduceMotion || hasPlayedEntrance) 1f else 0f) }
    val headlineAnim2 = remember { Animatable(if (reduceMotion || hasPlayedEntrance) 1f else 0f) }
    val searchBarAnim = remember { Animatable(if (reduceMotion || hasPlayedEntrance) 1f else 0f) }
    val contentAnim = remember { Animatable(if (reduceMotion || hasPlayedEntrance) 1f else 0f) }

    var isClearingRecents by remember { mutableStateOf(false) }
    var removingRecentQuery by remember { mutableStateOf<String?>(null) }
    var isFilterUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reduceMotion && !hasPlayedEntrance) {
            hasPlayedEntrance = true
            headlineAnim1.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            delay(60)
            headlineAnim2.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            searchBarAnim.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            contentAnim.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        } else {
            headlineAnim1.snapTo(1f)
            headlineAnim2.snapTo(1f)
            searchBarAnim.snapTo(1f)
            contentAnim.snapTo(1f)
        }
    }

    // Toast state for undoing cleared recents
    var toastVisible by remember { mutableStateOf(false) }
    var previousRecentSearches by remember { mutableStateOf<List<String>>(emptyList()) }

    // Back handling: Intercept back gesture on Results screen to glide back to Search screen
    BackHandler(enabled = isResultsActive) {
        viewModel.backToSearch()
    }

    val searchListState = rememberLazyListState()
    val resultsListState = rememberLazyListState()

    // Animate floating navbar hide on scroll down, show on scroll up (matching Home & Library)
    if (isResultsActive) {
        com.euthopiar.ui.navigation.ObserveScrollForNavbar(resultsListState)
    } else {
        com.euthopiar.ui.navigation.ObserveScrollForNavbar(searchListState)
    }

    // Tab reselection: Tapping Search tab while on Results screen returns to root Search screen, or scrolls to top
    val tabReselectionEvents = LocalTabReselection.current
    LaunchedEffect(tabReselectionEvents) {
        tabReselectionEvents.collect { tab ->
            if (tab == NavTab.SEARCH) {
                if (isResultsActive) {
                    viewModel.backToSearch()
                } else {
                    if (searchListState.firstVisibleItemIndex > 0 || searchListState.firstVisibleItemScrollOffset > 0) {
                        searchListState.animateScrollToItem(0)
                    }
                }
            }
        }
    }

    // Dismiss keyboard on scrolling
    val isSearchScrolling by remember { derivedStateOf { searchListState.isScrollInProgress } }
    val isResultsScrolling by remember { derivedStateOf { resultsListState.isScrollInProgress } }
    LaunchedEffect(isSearchScrolling || isResultsScrolling) {
        if (isSearchScrolling || isResultsScrolling) {
            keyboardController?.hide()
            focusManager.clearFocus()
            openDropdown = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    SearchAmbientHazeBackground(
        hazeState = searchHazeState,
        modifier = modifier
            .hazeSource(state = searchHazeState)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    openDropdown = null
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 1. Collapsing Top Headline: "Find\nsomething."
                // Smoothly collapses height (115dp -> 0dp) and fades out over 320ms when query submitted / results active.
                val headlineHeight by animateDpAsState(
                    targetValue = if (isResultsActive) 0.dp else 115.dp,
                    animationSpec = if (reduceMotion) tween(0) else tween(320, easing = FastOutSlowInEasing),
                    label = "headlineCollapseHeight"
                )
                val headlineAlpha by animateFloatAsState(
                    targetValue = if (isResultsActive) 0f else 1f,
                    animationSpec = if (reduceMotion) tween(0) else tween(240, easing = FastOutSlowInEasing),
                    label = "headlineCollapseAlpha"
                )

                if (headlineHeight > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headlineHeight)
                            .graphicsLayer {
                                alpha = headlineAlpha
                                translationY = (1f - headlineAlpha) * (-16.dp.toPx())
                            }
                            .padding(top = 16.dp, bottom = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Find",
                                style = SearchTokens.LargeHeadline,
                                modifier = Modifier.graphicsLayer {
                                    alpha = headlineAnim1.value
                                    translationY = (1f - headlineAnim1.value) * 12.dp.toPx()
                                }
                            )
                            Text(
                                text = "something.",
                                style = SearchTokens.LargeHeadline,
                                modifier = Modifier.graphicsLayer {
                                    alpha = headlineAnim2.value
                                    translationY = (1f - headlineAnim2.value) * 12.dp.toPx()
                                }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 2. SEARCH BAR ROW (Shared element, height 48dp on search -> 40dp on results)
                val searchBarHeight by animateDpAsState(
                    targetValue = if (isResultsActive) 40.dp else 48.dp,
                    animationSpec = if (reduceMotion) tween(0) else tween(280, easing = FastOutSlowInEasing),
                    label = "searchBarHeight"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(searchBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button on Results screen (fades & scales from 90% to 100%)
                    AnimatedVisibility(
                        visible = isResultsActive,
                        enter = fadeIn(animationSpec = if (reduceMotion) tween(0) else tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.9f, animationSpec = if (reduceMotion) tween(0) else tween(220, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing)) +
                               scaleOut(targetScale = 0.9f, animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing))
                    ) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .searchGlassSurface(
                                        shape = RoundedCornerShape(SearchTokens.BackButtonRadius),
                                        hazeState = searchHazeState
                                    )
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Back to search"
                                    ) {
                                        viewModel.backToSearch()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }

                    // Glass Search Bar (Shared Element)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                if (!isResultsActive && !hasPlayedEntrance) {
                                    alpha = searchBarAnim.value
                                    translationY = (1f - searchBarAnim.value) * 12.dp.toPx()
                                }
                            }
                            .searchGlassSurface(
                                shape = RoundedCornerShape(SearchTokens.SearchBarRadius),
                                hazeState = searchHazeState,
                                isActive = isSearchFocused
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isResultsActive) {
                                    viewModel.backToSearch()
                                }
                                searchFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = if (isSearchFocused || query.isNotBlank()) Color.White else SearchTokens.TextMuted,
                                modifier = Modifier.size(19.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Movies, series, anime",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp,
                                            color = SearchTokens.TextMuted
                                        )
                                    )
                                }

                                BasicTextField(
                                    value = query,
                                    onValueChange = { newText ->
                                        viewModel.onQueryChange(newText)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(searchFocusRequester)
                                        .onFocusChanged { isSearchFocused = it.isFocused },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.submitImmediateSearch(query)
                                    })
                                )
                            }

                            if (query.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = "Clear search"
                                        ) {
                                            if (isResultsActive) {
                                                viewModel.clearQueryAndBack()
                                                searchFocusRequester.requestFocus()
                                                keyboardController?.show()
                                            } else {
                                                viewModel.clearQuery()
                                                searchFocusRequester.requestFocus()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = SearchTokens.TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. FILTER DROPDOWN ROW (Equal-width, persistent across Search and Results screens)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. All Types Dropdown
                    val currentTypeLabel = when (selectedType) {
                        MediaType.MOVIE -> "Movies"
                        MediaType.TV_SERIES -> "Series"
                        MediaType.ANIME -> "Anime"
                        null -> "All types"
                    }
                    SearchGlassDropdown(
                        label = "All types",
                        selectedValue = if (selectedType != null) currentTypeLabel else null,
                        options = ALL_TYPE_OPTIONS,
                        onSelect = { option ->
                            val newType = when (option) {
                                "Movies" -> MediaType.MOVIE
                                "Series" -> MediaType.TV_SERIES
                                "Anime" -> MediaType.ANIME
                                else -> null
                            }
                            if (isResultsActive) {
                                scope.launch {
                                    isFilterUpdating = true
                                    viewModel.setTypeFilter(newType)
                                    delay(200)
                                    isFilterUpdating = false
                                }
                            } else {
                                viewModel.setTypeFilter(newType)
                            }
                        },
                        isExpanded = openDropdown == "type",
                        onToggleExpand = { openDropdown = if (openDropdown == "type") null else "type" },
                        onDismiss = { if (openDropdown == "type") openDropdown = null },
                        hazeState = searchHazeState,
                        modifier = Modifier.weight(1f)
                    )

                    // 2. Genre Dropdown
                    SearchGlassDropdown(
                        label = "Genre",
                        selectedValue = selectedGenre,
                        options = GENRE_OPTIONS,
                        onSelect = { option ->
                            if (isResultsActive) {
                                scope.launch {
                                    isFilterUpdating = true
                                    viewModel.setGenreFilter(option)
                                    delay(200)
                                    isFilterUpdating = false
                                }
                            } else {
                                viewModel.setGenreFilter(option)
                            }
                        },
                        isExpanded = openDropdown == "genre",
                        onToggleExpand = { openDropdown = if (openDropdown == "genre") null else "genre" },
                        onDismiss = { if (openDropdown == "genre") openDropdown = null },
                        hazeState = searchHazeState,
                        modifier = Modifier.weight(1f)
                    )

                    // 3. Year Dropdown
                    SearchGlassDropdown(
                        label = "Year",
                        selectedValue = selectedYear,
                        options = YEAR_OPTIONS,
                        onSelect = { option ->
                            if (isResultsActive) {
                                scope.launch {
                                    isFilterUpdating = true
                                    viewModel.setYearFilter(option)
                                    delay(200)
                                    isFilterUpdating = false
                                }
                            } else {
                                viewModel.setYearFilter(option)
                            }
                        },
                        isExpanded = openDropdown == "year",
                        onToggleExpand = { openDropdown = if (openDropdown == "year") null else "year" },
                        onDismiss = { if (openDropdown == "year") openDropdown = null },
                        hazeState = searchHazeState,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Offline Awareness Banner
                OfflineBanner()

                // MAIN CONTENT CONTAINER: Smooth transition between Search screen and Results screen
                Crossfade(
                    targetState = isResultsActive,
                    animationSpec = if (reduceMotion) tween(0) else tween(300, easing = FastOutSlowInEasing),
                    label = "SearchToResultsCrossfade"
                ) { resultsActive ->
                    if (!resultsActive) {
                        // ----------------------------------------------------
                        // PART 2: SEARCH SCREEN (Before query submitted)
                        // ----------------------------------------------------
                        LazyColumn(
                            state = searchListState,
                            contentPadding = PaddingValues(bottom = 110.dp, top = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Recent Searches Section (hidden if empty, collapses on Clear All)
                            if (recentSearches.isNotEmpty() || isClearingRecents) {
                                item(key = "recent_section") {
                                    AnimatedVisibility(
                                        visible = !isClearingRecents && recentSearches.isNotEmpty(),
                                        enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                                        exit = fadeOut(tween(180)) + shrinkVertically(tween(240))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        alpha = contentAnim.value
                                                    }
                                                    .padding(top = 10.dp, bottom = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "RECENT",
                                                    style = SearchTokens.SectionLabel
                                                )

                                                Text(
                                                    text = "Clear all",
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 12.sp,
                                                        color = SearchTokens.TextMuted
                                                    ),
                                                    modifier = Modifier
                                                        .clickable(
                                                            role = Role.Button,
                                                            onClickLabel = "Clear all recent searches"
                                                        ) {
                                                            scope.launch {
                                                                isClearingRecents = true
                                                                delay(240)
                                                                previousRecentSearches = recentSearches
                                                                viewModel.clearRecentSearches()
                                                                isClearingRecents = false
                                                                toastVisible = true
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            SearchHairlineDivider()

                                            recentSearches.take(6).forEachIndexed { idx, recent ->
                                                val rowVisible = removingRecentQuery != recent
                                                AnimatedVisibility(
                                                    visible = rowVisible,
                                                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                                                ) {
                                                    RecentSearchRow(
                                                        query = recent,
                                                        onClick = {
                                                            keyboardController?.hide()
                                                            viewModel.submitImmediateSearch(recent)
                                                        },
                                                        onRemove = {
                                                            scope.launch {
                                                                removingRecentQuery = recent
                                                                delay(200)
                                                                viewModel.removeRecentSearch(recent)
                                                                removingRecentQuery = null
                                                            }
                                                        },
                                                        modifier = Modifier.graphicsLayer {
                                                            alpha = contentAnim.value
                                                        }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))
                                        }
                                    }
                                }
                            }

                            // 2. Trending Today Section
                            item(key = "trending_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = contentAnim.value
                                        }
                                        .padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "TRENDING TODAY",
                                        style = SearchTokens.SectionLabel
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SearchHairlineDivider()
                                }
                            }

                            val trendingList = (uiState as? SearchUiState.Trending)?.items ?: cachedTrending
                            if (trendingList.isEmpty() && (uiState as? SearchUiState.Trending)?.isLoading == true) {
                                items(5) {
                                    ResultSkeletonRow(isFirst = false)
                                }
                            } else {
                                itemsIndexed(
                                    items = trendingList.take(10),
                                    key = { index, item -> "trending_${item.id}_$index" }
                                ) { index, item ->
                                    val rowAnim = remember { Animatable(if (reduceMotion || hasPlayedEntrance) 1f else 0f) }
                                    LaunchedEffect(Unit) {
                                        if (!reduceMotion && !hasPlayedEntrance) {
                                            val staggerDelay = (index * 40).coerceAtMost(320)
                                            delay(staggerDelay.toLong())
                                            rowAnim.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                                        }
                                    }
                                    TrendingRankedRow(
                                        rank = index + 1,
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.graphicsLayer {
                                            val combinedAlpha = contentAnim.value * rowAnim.value
                                            alpha = combinedAlpha
                                            translationY = (1f - rowAnim.value) * 12.dp.toPx()
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // ----------------------------------------------------
                        // PART 3: RESULTS SCREEN (After query submitted)
                        // ----------------------------------------------------
                        val resultsHeadlineAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
                        LaunchedEffect(query) {
                            if (!reduceMotion) {
                                resultsHeadlineAnim.snapTo(0f)
                                resultsHeadlineAnim.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
                            } else {
                                resultsHeadlineAnim.snapTo(1f)
                            }
                        }

                        // Compute filtered and sorted results
                        val rawResults = (uiState as? SearchUiState.Results)?.items ?: emptyList()
                        val filteredResults = remember(rawResults, selectedType, selectedGenre, selectedYear, selectedSort, query) {
                            var list = rawResults

                            // Type filter
                            if (selectedType != null) {
                                list = list.filter { it.type == selectedType }
                            }

                            // Year filter
                            if (!selectedYear.isNullOrBlank() && selectedYear != "Any year") {
                                list = list.filter { item ->
                                    val y = item.year ?: return@filter false
                                    when (selectedYear) {
                                        "2025" -> y == 2025
                                        "2024" -> y == 2024
                                        "2023" -> y == 2023
                                        "2022" -> y == 2022
                                        "2021" -> y == 2021
                                        "2020" -> y == 2020
                                        "2010s" -> y in 2010..2019
                                        "2000s" -> y in 2000..2009
                                        "1990s" -> y in 1990..1999
                                        "Classic" -> y < 1990
                                        else -> selectedYear!!.toIntOrNull()?.let { y == it } ?: true
                                    }
                                }
                            }

                            // Genre filter
                            if (!selectedGenre.isNullOrBlank() && selectedGenre != "All genres") {
                                list = list.filter { item ->
                                    item.genres.any { it.equals(selectedGenre, ignoreCase = true) } ||
                                    item.title.contains(selectedGenre!!, ignoreCase = true) ||
                                    item.quality?.contains(selectedGenre!!, ignoreCase = true) == true ||
                                    (selectedGenre.equals("Anime", ignoreCase = true) && item.type == MediaType.ANIME)
                                }
                            }

                            // Sort
                            when (selectedSort) {
                                SearchSortOption.BEST_MATCH -> {
                                    val q = query.trim().lowercase()
                                    list.sortedWith(
                                        compareByDescending<MediaItem> { it.title.trim().lowercase() == q }
                                            .thenByDescending { it.title.trim().lowercase().startsWith(q) }
                                            .thenByDescending { it.rating?.toFloatOrNull() ?: 0f }
                                    )
                                }
                                SearchSortOption.RATING -> {
                                    list.sortedByDescending { it.rating?.toFloatOrNull() ?: 0f }
                                }
                                SearchSortOption.NEWEST -> {
                                    list.sortedByDescending { it.year ?: 0 }
                                }
                                SearchSortOption.A_TO_Z -> {
                                    list.sortedBy { it.title.lowercase() }
                                }
                            }
                        }

                        val listAlpha by animateFloatAsState(
                            targetValue = if (isFilterUpdating) 0.5f else 1f,
                            animationSpec = if (reduceMotion) tween(0) else tween(160, easing = FastOutSlowInEasing),
                            label = "resultsListDimming"
                        )

                        LazyColumn(
                            state = resultsListState,
                            contentPadding = PaddingValues(bottom = 110.dp, top = 8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = listAlpha }
                        ) {
                            // 1. Query Headline & Count / Sort Bar
                            item(key = "query_headline_section") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = resultsHeadlineAnim.value
                                            translationY = (1f - resultsHeadlineAnim.value) * 12.dp.toPx()
                                        }
                                        .padding(top = 10.dp, bottom = 12.dp)
                                ) {
                                    // Large Searched Query Headline
                                    Text(
                                        text = query.ifBlank { "Search" },
                                        style = SearchTokens.QueryHeadline,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Count on left, Sort Dropdown on right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val countText = when (uiState) {
                                            is SearchUiState.Searching -> "Searching…"
                                            is SearchUiState.Results -> "${filteredResults.size} result${if (filteredResults.size != 1) "s" else ""}"
                                            is SearchUiState.Empty -> "0 results"
                                            is SearchUiState.Error -> "Error"
                                            else -> ""
                                        }

                                        Text(
                                            text = countText,
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = SearchTokens.TextMuted
                                            )
                                        )

                                        // Small glass Sort dropdown (~120 px wide, 30 px tall)
                                        Box {
                                            var isSortPopupRendered by remember { mutableStateOf(false) }
                                            val sortPopupAnim = remember { Animatable(0f) }

                                            val sortChevronAngle by animateFloatAsState(
                                                targetValue = if (openDropdown == "sort") 180f else 0f,
                                                animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing),
                                                label = "sortChevronAngle"
                                            )

                                            LaunchedEffect(openDropdown == "sort") {
                                                if (openDropdown == "sort") {
                                                    isSortPopupRendered = true
                                                    if (reduceMotion) {
                                                        sortPopupAnim.snapTo(1f)
                                                    } else {
                                                        sortPopupAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                                                    }
                                                    } else if (isSortPopupRendered) {
                                                        if (reduceMotion) {
                                                            sortPopupAnim.snapTo(0f)
                                                            isSortPopupRendered = false
                                                        } else {
                                                            sortPopupAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                                                            isSortPopupRendered = false
                                                        }
                                                    }
                                                }

                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .height(30.dp)
                                                    .searchGlassSurface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        hazeState = searchHazeState,
                                                        isActive = false
                                                    )
                                                    .clickable(
                                                        role = Role.DropdownList,
                                                        onClickLabel = "Sort by ${selectedSort.displayName}"
                                                    ) {
                                                        openDropdown = if (openDropdown == "sort") null else "sort"
                                                    }
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = selectedSort.displayName,
                                                        style = TextStyle(
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 11.5.sp,
                                                            color = Color.White
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Icon(
                                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = SearchTokens.TextMuted,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .graphicsLayer { rotationZ = sortChevronAngle }
                                                    )
                                                }
                                            }

                                            if (isSortPopupRendered) {
                                                val density = LocalDensity.current
                                                val sortScale = 0.95f + (0.05f * sortPopupAnim.value)
                                                val sortAlpha = sortPopupAnim.value
                                                val sortOffsetY = (1f - sortPopupAnim.value) * (-6f) * density.density

                                                Popup(
                                                    alignment = Alignment.TopEnd,
                                                    offset = IntOffset(0, with(density) { 36.dp.roundToPx() }),
                                                    onDismissRequest = { openDropdown = null },
                                                    properties = PopupProperties(focusable = true)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(135.dp)
                                                            .graphicsLayer {
                                                                scaleX = sortScale
                                                                scaleY = sortScale
                                                                alpha = sortAlpha
                                                                translationY = sortOffsetY
                                                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                                                            }
                                                            .searchGlassSurface(
                                                                shape = RoundedCornerShape(12.dp),
                                                                hazeState = searchHazeState,
                                                                isActive = true
                                                            )
                                                            .background(Color(0xFF14151F).copy(alpha = 0.96f))
                                                            .padding(vertical = 4.dp)
                                                    ) {
                                                        Column {
                                                            SearchSortOption.entries.forEachIndexed { idx, opt ->
                                                                val isPicked = opt == selectedSort
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(38.dp)
                                                                        .clickable {
                                                                            scope.launch {
                                                                                isFilterUpdating = true
                                                                                viewModel.setSort(opt)
                                                                                openDropdown = null
                                                                                delay(200)
                                                                                isFilterUpdating = false
                                                                            }
                                                                        }
                                                                        .padding(horizontal = 12.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(
                                                                        text = opt.displayName,
                                                                        style = TextStyle(
                                                                            fontFamily = AppFontFamily,
                                                                            fontWeight = if (isPicked) FontWeight.Medium else FontWeight.Normal,
                                                                            fontSize = 12.sp,
                                                                            color = if (isPicked) Color.White else SearchTokens.TextMuted
                                                                        )
                                                                    )
                                                                    if (isPicked) {
                                                                        Icon(
                                                                            imageVector = Icons.Outlined.Check,
                                                                            contentDescription = "Selected",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    }
                                                                }

                                                                if (idx < SearchSortOption.entries.lastIndex) {
                                                                    SearchHairlineDivider(
                                                                        modifier = Modifier.padding(horizontal = 10.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    SearchHairlineDivider()
                                }
                            }

                            // 2. Body States (Loading, Results, No Results, Error)
                            when (uiState) {
                                is SearchUiState.Searching -> {
                                    items(6) { index ->
                                        ResultSkeletonRow(isFirst = index == 0)
                                    }
                                }

                                is SearchUiState.Empty -> {
                                    item(key = "no_results_card") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No results for \"$query\"",
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 18.sp,
                                                    color = Color.White
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = "Check the spelling or try fewer filters.",
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    color = SearchTokens.TextMuted
                                                )
                                            )

                                            val hasActiveFilter = selectedType != null || !selectedGenre.isNullOrBlank() || !selectedYear.isNullOrBlank()
                                            if (hasActiveFilter) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .searchGlassSurface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            hazeState = searchHazeState
                                                        )
                                                        .clickable(
                                                            role = Role.Button,
                                                            onClickLabel = "Clear filters"
                                                        ) {
                                                            viewModel.clearFilters()
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "Clear filters",
                                                        style = TextStyle(
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 13.sp,
                                                            color = Color.White
                                                        )
                                                    )
                                                }
                                            }

                                            // Trending suggestions below
                                            if (cachedTrending.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(32.dp))
                                                Text(
                                                    text = "TRENDING TODAY",
                                                    style = SearchTokens.SectionLabel,
                                                    modifier = Modifier.align(Alignment.Start)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                SearchHairlineDivider()
                                            }
                                        }
                                    }

                                    if (cachedTrending.isNotEmpty()) {
                                        itemsIndexed(
                                            items = cachedTrending.take(6),
                                            key = { index, item -> "empty_trending_${item.id}_$index" }
                                        ) { index, item ->
                                            TrendingRankedRow(
                                                rank = index + 1,
                                                item = item,
                                                onClick = { onMediaClick(item) }
                                            )
                                        }
                                    }
                                }

                                is SearchUiState.Error -> {
                                    item(key = "error_card") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Couldn't load results",
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 18.sp,
                                                    color = Color.White
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Box(
                                                modifier = Modifier
                                                    .searchGlassSurface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        hazeState = searchHazeState
                                                    )
                                                    .clickable(
                                                        role = Role.Button,
                                                        onClickLabel = "Try again"
                                                    ) {
                                                        viewModel.retrySearch()
                                                    }
                                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = "Try again",
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                is SearchUiState.Results -> {
                                    if (filteredResults.isEmpty()) {
                                        item(key = "no_filtered_results") {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 36.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "No matching results",
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 17.sp,
                                                        color = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Try clearing or relaxing your filters.",
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 13.sp,
                                                        color = SearchTokens.TextMuted
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .searchGlassSurface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            hazeState = searchHazeState
                                                        )
                                                        .clickable(
                                                            role = Role.Button,
                                                            onClickLabel = "Clear filters"
                                                        ) {
                                                            viewModel.clearFilters()
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "Clear filters",
                                                        style = TextStyle(
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 13.sp,
                                                            color = Color.White
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        itemsIndexed(
                                            items = filteredResults,
                                            key = { index, item -> "${item.id}_${item.provider}_$index" }
                                        ) { index, item ->
                                            val rowAnim = remember(item.id, query) { Animatable(if (reduceMotion) 1f else 0f) }
                                            LaunchedEffect(item.id, query) {
                                                if (!reduceMotion) {
                                                    val staggerDelay = (index * 40).coerceAtMost(320)
                                                    delay(staggerDelay.toLong())
                                                    rowAnim.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                                                }
                                            }
                                            val rowModifier = Modifier.graphicsLayer {
                                                alpha = rowAnim.value
                                                translationY = (1f - rowAnim.value) * 12.dp.toPx()
                                            }

                                            if (index == 0) {
                                                // 1. BEST MATCH Row (larger layout)
                                                BestMatchResultRow(
                                                    item = item,
                                                    onPlay = {
                                                        if (onPlayMedia != null) {
                                                            onPlayMedia(item)
                                                        } else {
                                                            onMediaClick(item)
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.commitSearch(query)
                                                        onMediaClick(item)
                                                    },
                                                    modifier = rowModifier
                                                )
                                            } else {
                                                // 2. Compact Result Row
                                                CompactResultRow(
                                                    item = item,
                                                    onClick = {
                                                        viewModel.commitSearch(query)
                                                        onMediaClick(item)
                                                    },
                                                    modifier = rowModifier
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

            // Glass Snackbar Toast for "Recent searches cleared" with Undo
            SearchGlassToast(
                message = "Recent searches cleared",
                actionLabel = "Undo",
                onActionClick = {
                    viewModel.undoClearRecentSearches(previousRecentSearches)
                    toastVisible = false
                },
                isVisible = toastVisible,
                hazeState = searchHazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )

            // Auto-dismiss toast after 4 seconds
            LaunchedEffect(toastVisible) {
                if (toastVisible) {
                    delay(4000)
                    toastVisible = false
                }
            }
        }

    }
}
