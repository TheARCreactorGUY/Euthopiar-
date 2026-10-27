package com.euthopiar.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.width
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.euthopiar.ui.theme.rememberWindowDimensions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import com.euthopiar.ui.longpress.LocalLongPressHazeState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.registry.PluginRegistry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import com.euthopiar.ui.components.EmptyState
import com.euthopiar.ui.components.AppFilterChip
import com.euthopiar.ui.components.SnapWheelSelector
import com.euthopiar.ui.components.CompactErrorState
import com.euthopiar.ui.components.ContentRow
import com.euthopiar.ui.components.ContinueWatchingRow
import com.euthopiar.ui.components.ErrorState
import com.euthopiar.ui.components.HeroCarousel
import com.euthopiar.ui.components.HeroCarouselSkeleton
import com.euthopiar.ui.components.HomeScreenSkeleton
import com.euthopiar.ui.components.ProvideShimmerScope
import com.euthopiar.ui.details.sheets.MediaDetailsActionSheet
import com.euthopiar.ui.details.sheets.SheetScreen
import com.euthopiar.ui.components.rememberHeroStretchState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.components.MediaQuickActionsSheet
import com.euthopiar.ui.components.OfflineBanner
import com.euthopiar.ui.library.components.AddToPlaylistSheet
import com.euthopiar.ui.longpress.LongPressHost
import com.euthopiar.ui.longpress.SheetActions
import com.euthopiar.ui.longpress.UserReaction
import com.euthopiar.ui.longpress.toSheetItem
import com.euthopiar.ui.components.TopHeaderBar
import com.euthopiar.ui.components.TopTenRow
import com.euthopiar.ui.components.HomeUpdateBanner
import com.euthopiar.ui.components.UpdateInfoDialog
import com.euthopiar.ui.components.DonationReminderDialog
import com.euthopiar.data.common.DonationConfig
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.update.AppUpdateManager
import com.euthopiar.data.update.UpdateCheckState
import kotlinx.coroutines.flow.first
import com.euthopiar.ui.navigation.ObserveScrollForNavbar
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.SurfaceMid
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (MediaItem) -> Unit,
    onContinueWatchingClick: ((com.euthopiar.data.local.PlaybackHistoryEntity) -> Unit)? = null,
    onPlayHero: ((MediaItem, com.euthopiar.data.local.PlaybackHistoryEntity?) -> Unit)? = null,
    onNavigateToPluginManager: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel(),
    registry: PluginRegistry = PluginRegistry.Default,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val universalTopTen by viewModel.universalTopTen.collectAsState()
    val playbackHistoryMap by viewModel.playbackHistoryMap.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val watchlistItems by viewModel.watchlistItems.collectAsState()
    val watchlistStatusMap by viewModel.watchlistStatusMap.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val top10Style by viewModel.top10Style.collectAsState()
    val sectionLabelStyle by viewModel.sectionLabelStyle.collectAsState()
    val longPressStyle by viewModel.longPressStyle.collectAsState()
    val mediaReactions by viewModel.mediaReactions.collectAsState()
    val snackbarController = LocalSnackbarController.current
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isProgressiveLoading by viewModel.isProgressiveLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var quickActionItem by remember { mutableStateOf<MediaItem?>(null) }
    var addToPlaylistMedia by remember { mutableStateOf<MediaItem?>(null) }
    var watchlistSheetMedia by remember { mutableStateOf<MediaItem?>(null) }

    // GitHub Release Update Management
    val updateManager = remember(context) { AppUpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()
    val downloadState by updateManager.downloadState.collectAsState()
    val autoPopupShown by updateManager.autoPopupShownForSession.collectAsState()
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }
    val autoCheckUpdates by preferencesRepository.autoCheckUpdates.collectAsState(initial = true)
    val skippedUpdateVersion by preferencesRepository.skippedUpdateVersion.collectAsState(initial = "")
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }

    // Automatic update check on app launch
    LaunchedEffect(autoCheckUpdates) {
        if (autoCheckUpdates && updateState is UpdateCheckState.Idle) {
            updateManager.checkForUpdates(forceManual = false)
        }
    }

    // Auto-popup update dialog once per app session when update is available and not skipped
    LaunchedEffect(updateState, skippedUpdateVersion, autoPopupShown) {
        val available = updateState as? UpdateCheckState.UpdateAvailable
        if (available != null && !autoPopupShown && available.release.versionName != skippedUpdateVersion) {
            showUpdateDialog = true
            updateManager.markAutoPopupShownForSession()
        }
    }

    // Home Donation Reminder Evaluation (New Install first content load, Post-App Update, 24-Hour Cooldown)
    var donationEvaluatedThisSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (donationEvaluatedThisSession) return@LaunchedEffect
        // Must wait until home page gets loaded with content for the first time
        if (uiState !is HomeUiState.Success) return@LaunchedEffect

        // Smooth settling delay after home rails and hero render
        delay(1800L)

        val currentVersion = updateManager.getCurrentVersionName()
        val lastSavedVersion = preferencesRepository.lastAppVersion.first()
        val lastPrompt = preferencesRepository.lastDonationPromptTimestamp.first()
        val now = System.currentTimeMillis()

        // If an automatic update check is actively in-flight, give it up to 1.5s to settle
        var checksRemaining = 15
        while (updateState is UpdateCheckState.Checking && checksRemaining > 0) {
            delay(100L)
            checksRemaining--
        }

        // Suppress if an app update is checking or available, or an action sheet is active
        val isUpdateActive = updateState is UpdateCheckState.Checking ||
            updateState is UpdateCheckState.UpdateAvailable ||
            showUpdateDialog
        val isSheetActive = quickActionItem != null ||
            addToPlaylistMedia != null ||
            watchlistSheetMedia != null

        if (isUpdateActive || isSheetActive) {
            return@LaunchedEffect
        }

        // Trigger conditions:
        // 1. New installers after onboarding and content loaded for the first time (lastPrompt == 0L)
        // 2. After application update (lastSavedVersion is set and differs from currentVersion)
        // 3. 24-hour recurring cooldown expired
        // 4. System clock skew recovery
        val isNewInstallFirstContent = (lastPrompt == 0L)
        val isPostAppUpdate = (lastSavedVersion.isNotBlank() && lastSavedVersion != currentVersion)
        val isCooldownExpired = (lastPrompt > 0L && (now - lastPrompt >= DonationConfig.COOLDOWN_MS))
        val isClockSkew = (lastPrompt > now)

        if (isNewInstallFirstContent || isPostAppUpdate || isCooldownExpired || isClockSkew) {
            preferencesRepository.setLastDonationPromptTimestamp(now)
            preferencesRepository.setLastAppVersion(currentVersion)
            showDonationDialog = true
            donationEvaluatedThisSession = true
        } else {
            // Keep app version up-to-date even when donation popup is not triggered
            if (lastSavedVersion != currentVersion) {
                preferencesRepository.setLastAppVersion(currentVersion)
            }
            donationEvaluatedThisSession = true
        }
    }
    val lazyListState = rememberLazyListState()
    ObserveScrollForNavbar(lazyListState)
    val windowDimensions = rememberWindowDimensions()
    val heroPlayFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = lazyListState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreRails()
        }
    }

    LaunchedEffect(windowDimensions.isTV, uiState) {
        if (windowDimensions.isTV && uiState is HomeUiState.Success) {
            kotlinx.coroutines.delay(120)
            try {
                heroPlayFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val tabReselectionEvents = com.euthopiar.ui.navigation.LocalTabReselection.current
    LaunchedEffect(tabReselectionEvents) {
        tabReselectionEvents.collect { tab ->
            if (tab == com.euthopiar.ui.navigation.NavTab.HOME) {
                if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
                    lazyListState.animateScrollToItem(0)
                }
            }
        }
    }

    val isHeroOffScreen by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val heroScrollOffsetPx by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            } else {
                1000f
            }
        }
    }
    val headerScrollFraction by remember {
        derivedStateOf {
            val t = (heroScrollOffsetPx / with(density) { 140.dp.toPx() }).coerceIn(0f, 1f)
            // Smootherstep S(t) = 6t^5 - 15t^4 + 10t^3 ensures zero derivative at both ends (no start/end snap)
            t * t * t * (t * (t * 6f - 15f) + 10f)
        }
    }

    val homeHazeState = rememberHazeState()

    val isAmoled = LocalAmoledMode.current
    val screenBg = if (isAmoled) Color.Black else Background

    CompositionLocalProvider(LocalLongPressHazeState provides homeHazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            // Screen content behind the sheet (marked as hazeSource with screenBg for uniform non-patchy blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
                    .hazeSource(state = homeHazeState)
            ) {
        Crossfade(
            targetState = uiState,
            animationSpec = tween(durationMillis = Motion.DurationEmphasized),
            label = "HomeScreenTransition"
        ) { state ->
            when (state) {
                is HomeUiState.Loading -> {
                    ProvideShimmerScope {
                        HomeScreenSkeleton(modifier = Modifier.fillMaxSize())
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.error is com.euthopiar.ui.util.UserError.NoSources) {
                            EmptyState(
                                icon = Icons.Outlined.Extension,
                                title = "No sources yet",
                                message = "Add a source to start watching.",
                                actionLabel = if (onNavigateToPluginManager != null) "Browse Sources" else null,
                                onActionClick = onNavigateToPluginManager
                            )
                        } else {
                            ErrorState(
                                error = state.error,
                                onRetry = { viewModel.loadCatalog() }
                            )
                        }
                    }
                }

                is HomeUiState.Success -> {
                    if (state.filteredRows.isEmpty() && state.heroItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.Extension,
                                title = "No sources yet",
                                message = "Add a source to start watching.",
                                actionLabel = if (onNavigateToPluginManager != null) "Browse Sources" else null,
                                onActionClick = onNavigateToPluginManager
                            )
                        }
                    } else {
                        val heroItemCount = state.heroItems.size
                        val pagerState = rememberPagerState(
                            initialPage = remember(heroItemCount) {
                                if (heroItemCount > 1) (1000 / 2) * heroItemCount else 0
                            },
                            pageCount = { if (heroItemCount > 1) heroItemCount * 1000 else heroItemCount.coerceAtLeast(1) }
                        )
                    val rankedTop10Row = remember(state.filteredRows) {
                        state.filteredRows.firstOrNull { row ->
                            val t = row.title.trim().lowercase()
                            (t == "top 10" || t.startsWith("top 10") || t.startsWith("top ten") || t == "top 10 today") &&
                            row.items.size >= 5
                        }
                    }
                    val displayContentRows = remember(state.filteredRows, rankedTop10Row) {
                        if (rankedTop10Row != null) {
                            state.filteredRows.filter { it.title != rankedTop10Row.title }
                        } else {
                            state.filteredRows
                        }
                    }
                    val topTenDisplayItems = remember(universalTopTen, rankedTop10Row) {
                        if (universalTopTen.isNotEmpty()) universalTopTen.take(10) else rankedTop10Row?.items?.take(10) ?: emptyList()
                    }

                    val heroStretchState = rememberHeroStretchState(
                        listState = lazyListState,
                        onRefresh = null
                    )
                    val isFeedScrolling by remember {
                        derivedStateOf {
                            lazyListState.isScrollInProgress || heroStretchState.stretchPx > 0f
                        }
                    }
                    val heroStretchModifier = if (state.heroItems.isNotEmpty()) {
                        Modifier.nestedScroll(heroStretchState.nestedScrollConnection)
                    } else {
                        Modifier
                    }

                    val isReducedMotion = LocalReduceMotion.current
                    var isFirstHomeComposition by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        isFirstHomeComposition = false
                    }

                    val currentTabIndex = state.availableTabs.indexOf(selectedTab).coerceAtLeast(0)
                    val rowsForTab = state.rowsByTab[selectedTab] ?: displayContentRows
                    val rankedTop10 = remember(rowsForTab) {
                        rowsForTab.firstOrNull { row ->
                            val t = row.title.trim().lowercase()
                            (t == "top 10" || t.startsWith("top 10") || t.startsWith("top ten") || t == "top 10 today") &&
                            row.items.size >= 5
                        }
                    }
                    val contentRowsForTab = remember(rowsForTab, rankedTop10) {
                        val base = if (rankedTop10 != null) {
                            rowsForTab.filter { it.title != rankedTop10.title }
                        } else {
                            rowsForTab
                        }
                        base.distinctBy { "${it.title}_${it.subtitle.orEmpty()}_${it.eyebrow.orEmpty()}" }
                    }
                    val topTenForTab = remember(selectedTab, universalTopTen, rankedTop10) {
                        if (universalTopTen.isNotEmpty()) {
                            universalTopTen.take(10)
                        } else {
                            rankedTop10?.items?.take(10) ?: emptyList()
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        val lazyTopPadding = if (state.heroItems.isEmpty()) {
                            Dimens.spacing48 + 56.dp
                        } else {
                            0.dp
                        }

                        @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.foundation.LocalOverscrollConfiguration provides null
                        ) {
                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(top = lazyTopPadding),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(heroStretchModifier)
                            ) {
                            // 1. Apple TV Style Hero Carousel
                            item(key = "hero_carousel", contentType = "hero") {
                                HeroCarousel(
                                    items = state.heroItems,
                                    pagerState = pagerState,
                                    bookmarkedIds = bookmarkedIds,
                                    playbackMap = playbackHistoryMap,
                                    onPlayClick = { item, history ->
                                        if (onPlayHero != null) {
                                            onPlayHero(item, history)
                                        } else if (history != null && onContinueWatchingClick != null) {
                                            onContinueWatchingClick(history)
                                        } else {
                                            onMediaClick(item)
                                        }
                                    },
                                    onDetailsClick = { onMediaClick(it) },
                                    onBookmarkClick = { watchlistSheetMedia = it },
                                    isOffScreen = isHeroOffScreen,
                                    isFeedScrolling = { isFeedScrolling },
                                    playFocusRequester = heroPlayFocusRequester,
                                    stretchPx = { heroStretchState.stretchPx },
                                    scrollOffsetPx = { heroScrollOffsetPx }
                                )
                            }

                            // 2. Category Snap Wheel Selector
                            if (state.availableTabs.size > 1) {
                                item(key = "category_tabs", contentType = "snap_wheel") {
                                    val currentTabIndex = state.availableTabs.indexOf(selectedTab).coerceAtLeast(0)
                                    SnapWheelSelector(
                                        options = state.availableTabs.map { it.label },
                                        selectedIndex = currentTabIndex,
                                        onSelected = { index ->
                                            if (index in state.availableTabs.indices) {
                                                viewModel.selectTab(state.availableTabs[index])
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Dimens.spacing8)
                                    )
                                }
                            }

                            if (contentRowsForTab.isEmpty() && state.heroItems.isEmpty()) {
                                item(key = "empty_catalog_loading", contentType = "loader") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            trackColor = Color.White.copy(alpha = 0.2f),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            } else {
                                // 3. Continue Watching Row
                                if (continueWatching.isNotEmpty()) {
                                    item(key = "continue_watching", contentType = "continue_watching") {
                                        ContinueWatchingRow(
                                            items = continueWatching,
                                            sectionLabelStyle = sectionLabelStyle,
                                            onItemClick = { item ->
                                                if (onContinueWatchingClick != null) {
                                                    onContinueWatchingClick(item)
                                                } else {
                                                    val type = if (item.seasonNumber != null) {
                                                        com.euthopiar.core.model.MediaType.TV_SERIES
                                                    } else {
                                                        com.euthopiar.core.model.MediaType.MOVIE
                                                    }
                                                    onMediaClick(
                                                        MediaItem(
                                                            id = item.mediaId,
                                                            title = item.title,
                                                            url = "",
                                                            posterUrl = item.posterUrl,
                                                            backdropUrl = item.backdropUrl,
                                                            type = type
                                                        )
                                                    )
                                                }
                                            },
                                            onDeleteItem = { item ->
                                                viewModel.removeFromContinueWatchingWithUndo(item.id) { undo ->
                                                    snackbarController.show(
                                                        message = "Removed from Continue Watching",
                                                        actionLabel = "Undo",
                                                        durationMillis = 5000L
                                                    ) { undo() }
                                                }
                                            },
                                            onClearAll = {
                                                viewModel.clearContinueWatchingWithUndo { undo ->
                                                    snackbarController.show(
                                                        message = "Cleared Continue Watching",
                                                        actionLabel = "Undo",
                                                        durationMillis = 5000L
                                                    ) { undo() }
                                                }
                                            },
                                            modifier = Modifier.staggeredEntrance(
                                                tabKey = selectedTab,
                                                rowIndex = 0,
                                                isEntering = true,
                                                isFirstComposition = isFirstHomeComposition,
                                                reduceMotion = isReducedMotion
                                            )
                                        )
                                    }
                                }

                                // 3.5 My List / Watchlist Row (UX Guide §4)
                                if (watchlistItems.isNotEmpty()) {
                                    item(key = "watchlist_row", contentType = "watchlist") {
                                        ContentRow(
                                            row = com.euthopiar.core.model.CatalogRow(
                                                title = "My List",
                                                items = watchlistItems,
                                                subtitle = "Your saved movies & shows",
                                                eyebrow = "MY LIST"
                                            ),
                                            sectionLabelStyle = sectionLabelStyle,
                                            onItemClick = { onMediaClick(it) },
                                            onItemLongClick = { quickActionItem = it },
                                            modifier = Modifier.staggeredEntrance(
                                                tabKey = selectedTab,
                                                rowIndex = 1,
                                                isEntering = true,
                                                isFirstComposition = isFirstHomeComposition,
                                                reduceMotion = isReducedMotion
                                            )
                                        )
                                    }
                                }

                                // 4. Universal TMDB Top 10 today Row (Bridged across all plugins)
                                if (topTenForTab.isNotEmpty()) {
                                    item(key = "universal_top_10", contentType = "top_10") {
                                        TopTenRow(
                                            title = "Top 10 today",
                                            items = topTenForTab,
                                            style = top10Style,
                                            sectionLabelStyle = sectionLabelStyle,
                                            subtitle = "Updated daily",
                                            eyebrow = "THIS WEEK",
                                            onItemClick = { onMediaClick(it) },
                                            onItemLongClick = { quickActionItem = it },
                                            modifier = Modifier.staggeredEntrance(
                                                tabKey = selectedTab,
                                                rowIndex = 2,
                                                isEntering = true,
                                                isFirstComposition = isFirstHomeComposition,
                                                reduceMotion = isReducedMotion
                                            )
                                        )
                                    }
                                }

                                // 5. Multi-Category Content Rows (Progressively Loaded & Virtualized)
                                itemsIndexed(
                                    items = contentRowsForTab,
                                    key = { index, row -> "stremio_${row.title}_${row.subtitle.orEmpty()}_${row.eyebrow.orEmpty()}_$index" },
                                    contentType = { _, _ -> "catalog_row" }
                                ) { index, row ->
                                    val stagger = index + 3
                                    if (row.items.isEmpty()) {
                                        CompactErrorState(
                                            message = "Could not load ${row.title}",
                                            onRetry = { viewModel.refreshCatalog() },
                                            modifier = Modifier.staggeredEntrance(
                                                tabKey = selectedTab,
                                                rowIndex = stagger,
                                                isEntering = true,
                                                isFirstComposition = isFirstHomeComposition,
                                                reduceMotion = isReducedMotion
                                            )
                                        )
                                    } else {
                                        ContentRow(
                                            row = row,
                                            sectionLabelStyle = sectionLabelStyle,
                                            onItemClick = { onMediaClick(it) },
                                            onItemLongClick = { quickActionItem = it },
                                            modifier = Modifier.staggeredEntrance(
                                                tabKey = selectedTab,
                                                rowIndex = stagger,
                                                isEntering = true,
                                                isFirstComposition = isFirstHomeComposition,
                                                reduceMotion = isReducedMotion
                                            )
                                        )
                                    }
                                }

                                // Progressive rail loading indicator if more rails are fetching
                                if (isProgressiveLoading) {
                                    item(key = "progressive_rail_loader", contentType = "loader") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White.copy(alpha = 0.7f),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom Spacer for Navigation Bar clearance (phone only)
                            val bottomSpacerHeight = if (windowDimensions.isPhone) {
                                Dimens.navBarHeight + Dimens.spacing32
                            } else {
                                Dimens.spacing32
                            }
                            item(key = "home_bottom_spacer", contentType = "spacer") {
                                Spacer(modifier = Modifier.height(bottomSpacerHeight))
                            }
                        }
                    }
                }
            }
        }
    }
}
} // Close Box(hazeSource)

        val isPreviewActive = quickActionItem != null
        val headerAlpha by animateFloatAsState(
            targetValue = if (isPreviewActive) 0f else 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "HeaderPreviewAlpha"
        )

        // Pinned Top Header Bar, Offline Banner & Update Banner (outside hazeSource, samples homeHazeState)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerAlpha
                }
        ) {
            TopHeaderBar(
                registry = registry,
                scrollFraction = { headerScrollFraction },
                hazeState = if (isPreviewActive) null else homeHazeState,
                hasUpdateAvailable = updateState is UpdateCheckState.UpdateAvailable,
                onUpdateBadgeClick = { showUpdateDialog = true }
            )
            OfflineBanner()
            HomeUpdateBanner(
                hazeState = if (isPreviewActive) null else homeHazeState,
                onOpenUpdatePopup = { _, _ -> showUpdateDialog = true },
                onDismiss = { updateManager.dismissBannerForSession() }
            )
        }

    // Long-Press Quick Actions Host (sits on top of blurred hazeSource content)
    quickActionItem?.let { item ->
        val isBookmarked = item.id in bookmarkedIds
        val reactionStr = mediaReactions[item.id]
        val reaction = UserReaction.fromId(reactionStr)
        val history = playbackHistoryMap[item.id]
        val sheetItem = item.toSheetItem(
            history = history,
            inWatchlist = isBookmarked,
            reaction = reaction
        )

        val actions = SheetActions(
            onPlay = {
                quickActionItem = null
                if (onPlayHero != null) {
                    onPlayHero(item, history)
                } else if (history != null && onContinueWatchingClick != null) {
                    onContinueWatchingClick(history)
                } else {
                    onMediaClick(item)
                }
            },
            onResume = {
                quickActionItem = null
                if (onPlayHero != null) {
                    onPlayHero(item, history)
                } else if (history != null && onContinueWatchingClick != null) {
                    onContinueWatchingClick(history)
                } else {
                    onMediaClick(item)
                }
            },
            onToggleWatchlist = {
                viewModel.toggleBookmark(item)
                snackbarController.show(
                    message = if (isBookmarked) "Removed from watchlist" else "Added to watchlist",
                    actionLabel = "Undo",
                    durationMillis = 4000L
                ) {
                    viewModel.toggleBookmark(item)
                }
            },
            onReact = { nextReaction ->
                viewModel.setMediaReaction(item.id, nextReaction)
            },
            onShare = {
                quickActionItem = null
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, item.title)
                    putExtra(android.content.Intent.EXTRA_TEXT, "Watch ${item.title} on Euthopiar")
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share ${item.title}"))
            },
            onDownload = {
                quickActionItem = null
                onMediaClick(item)
            },
            onOpenDetails = {
                quickActionItem = null
                onMediaClick(item)
            },
            onNotInterested = {
                quickActionItem = null
                snackbarController.show("We'll show fewer recommendations like this")
            },
            onAddToPlaylist = {
                val target = item
                quickActionItem = null
                addToPlaylistMedia = target
            }
        )

        LongPressHost(
            item = sheetItem,
            style = longPressStyle,
            actions = actions,
            onDismiss = { quickActionItem = null }
        )
    }

    addToPlaylistMedia?.let { media ->
        AddToPlaylistSheet(
            mediaId = media.id,
            title = media.title,
            posterUrl = media.posterUrl ?: media.backdropUrl,
            backdropUrl = media.backdropUrl ?: media.posterUrl,
            isTvSeries = media.type == com.euthopiar.core.model.MediaType.TV_SERIES,
            year = media.year,
            provider = media.provider,
            hazeState = homeHazeState,
            onDismiss = { addToPlaylistMedia = null },
            onPlaylistAction = { msg -> snackbarController.show(msg) }
        )
    }

    watchlistSheetMedia?.let { media ->
        val currentStatus = watchlistStatusMap[media.id]
        MediaDetailsActionSheet(
            mediaId = media.id,
            title = media.title,
            posterUrl = media.posterUrl ?: media.backdropUrl,
            backdropUrl = media.backdropUrl ?: media.posterUrl,
            isTvSeries = media.type == com.euthopiar.core.model.MediaType.TV_SERIES || media.type == com.euthopiar.core.model.MediaType.ANIME,
            year = media.year,
            provider = media.provider,
            initialScreen = SheetScreen.WATCHLIST_CATEGORY,
            watchlistStatus = currentStatus,
            hazeState = homeHazeState,
            onSelectWatchlistCategory = { statusKey, label ->
                val isMove = !currentStatus.isNullOrBlank() && currentStatus != statusKey
                val toastMsg = if (isMove) "Moved to $label" else "Added to $label"
                viewModel.setWatchlistStatus(media, statusKey)
                snackbarController.show(toastMsg)
            },
            onRemoveFromWatchlist = {
                val previousStatus = currentStatus ?: "TO_BE_WATCHED"
                viewModel.removeFromWatchlist(media.id)
                snackbarController.show(
                    message = "Removed from watchlist",
                    actionLabel = "Undo",
                    durationMillis = 4000L
                ) {
                    viewModel.setWatchlistStatus(media, previousStatus)
                }
            },
            onPlaylistAction = { msg -> snackbarController.show(msg) },
            onDismiss = { watchlistSheetMedia = null }
        )
    }

    // Application Update Information Popup
    if (showUpdateDialog) {
        val available = updateState as? UpdateCheckState.UpdateAvailable
        if (available != null) {
            UpdateInfoDialog(
                release = available.release,
                currentVersion = available.currentVersion,
                downloadState = downloadState,
                hazeState = homeHazeState,
                onDismiss = { showUpdateDialog = false },
                onDownloadAndInstall = { rel -> updateManager.downloadAndInstallUpdate(rel) },
                onTriggerInstall = { file, fallbackUrl -> updateManager.triggerInstallApk(file, fallbackUrl) },
                onSkipVersion = { version ->
                    updateManager.skipVersion(version)
                    showUpdateDialog = false
                }
            )
        }
    }

    // Home Donation Reminder Popup
    if (showDonationDialog && !showUpdateDialog) {
        DonationReminderDialog(
            hazeState = homeHazeState,
            onDismiss = { showDonationDialog = false }
        )
    }
} // Close outer Box
} // Close CompositionLocalProvider
} // Close fun HomeScreen

@Composable
private fun Modifier.staggeredEntrance(
    tabKey: Any,
    rowIndex: Int,
    isEntering: Boolean,
    isFirstComposition: Boolean,
    reduceMotion: Boolean
): Modifier {
    if (isFirstComposition || reduceMotion || !isEntering || rowIndex >= 4) return this

    val animatable = remember(tabKey, rowIndex) { Animatable(0f) }
    LaunchedEffect(tabKey, rowIndex) {
        delay(rowIndex * 20L)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val translateYPx = with(density) { 8.dp.toPx() }

    return this.graphicsLayer {
        val progress = animatable.value
        alpha = 0.5f + 0.5f * progress
        translationY = (1f - progress) * translateYPx
    }
}

