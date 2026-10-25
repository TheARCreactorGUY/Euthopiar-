package com.euthopiar.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.ui.details.MediaDetailsScreen
import com.euthopiar.ui.home.HomeScreen
import com.euthopiar.ui.player.PlayerScreen
import com.euthopiar.ui.search.SearchScreen
import com.euthopiar.ui.components.AppSnackbarController
import com.euthopiar.ui.components.AppSnackbarHost
import com.euthopiar.ui.components.AppSnackbarHostState
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.profile.DetroitProfileUnlockDialog
import com.euthopiar.ui.profile.QuickProfileSwitcherDialog
import com.euthopiar.ui.theme.*
import com.euthopiar.ui.util.findActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource

val LocalNavbarVisible = compositionLocalOf { mutableStateOf(true) }

/**
 * Computes directional scroll delta handling item boundary transitions and heterogeneous item sizes.
 * If currentIndex > previousIndex, user scrolled DOWN (content moved UP) past at least one item boundary,
 * so the delta is guaranteed positive and clamped to at least [thresholdPx].
 * If currentIndex < previousIndex, user scrolled UP (content moved DOWN) past at least one item boundary,
 * so the delta is guaranteed negative and clamped to at most -[thresholdPx].
 */
internal fun computeScrollDelta(
    currentIndex: Int,
    currentOffset: Int,
    previousIndex: Int,
    previousOffset: Int,
    itemDimension: Int,
    thresholdPx: Float
): Float {
    return if (currentIndex > previousIndex) {
        val estimatedPassed = (currentIndex - previousIndex) * itemDimension - previousOffset + currentOffset
        maxOf(thresholdPx, estimatedPassed.toFloat())
    } else if (currentIndex < previousIndex) {
        val estimatedPassed = (currentIndex - previousIndex) * itemDimension - previousOffset + currentOffset
        minOf(-thresholdPx, estimatedPassed.toFloat())
    } else {
        (currentOffset - previousOffset).toFloat()
    }
}

/**
 * Accumulates directional scroll deltas against a threshold before requesting navbar visibility changes.
 * Direction reversals immediately reset opposing accumulation, preventing flutter and jitter.
 */
internal class ScrollDirectionAccumulator(val thresholdPx: Float) {
    var accumulatedScroll = 0f
        private set

    /**
     * Updates the accumulator with [delta].
     * Returns true if navbar should show, false if navbar should hide, or null if threshold not reached.
     */
    fun onDelta(delta: Float): Boolean? {
        if (delta > 0f) {
            if (accumulatedScroll < 0f) {
                accumulatedScroll = 0f
            }
            accumulatedScroll += delta
            if (accumulatedScroll >= thresholdPx) {
                accumulatedScroll = 0f
                return false
            }
        } else if (delta < 0f) {
            if (accumulatedScroll > 0f) {
                accumulatedScroll = 0f
            }
            accumulatedScroll += delta
            if (accumulatedScroll <= -thresholdPx) {
                accumulatedScroll = 0f
                return true
            }
        }
        return null
    }

    fun reset() {
        accumulatedScroll = 0f
    }
}

@Composable
fun ObserveScrollForNavbar(lazyListState: androidx.compose.foundation.lazy.LazyListState) {
    val navbarVisible = LocalNavbarVisible.current
    val thresholdPx = with(LocalDensity.current) { 28.dp.toPx() }

    LaunchedEffect(lazyListState, thresholdPx) {
        var previousIndex = lazyListState.firstVisibleItemIndex
        var previousOffset = lazyListState.firstVisibleItemScrollOffset
        val accumulator = ScrollDirectionAccumulator(thresholdPx)

        snapshotFlow {
            Pair(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
        }
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                val isNearTop = currentIndex == 0 && currentOffset < thresholdPx
                if (isNearTop) {
                    navbarVisible.value = true
                    accumulator.reset()
                    previousIndex = currentIndex
                    previousOffset = currentOffset
                    return@collect
                }

                val itemSize = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: thresholdPx.toInt()
                val delta = computeScrollDelta(
                    currentIndex = currentIndex,
                    currentOffset = currentOffset,
                    previousIndex = previousIndex,
                    previousOffset = previousOffset,
                    itemDimension = itemSize,
                    thresholdPx = thresholdPx
                )

                previousIndex = currentIndex
                previousOffset = currentOffset

                val visibilityChange = accumulator.onDelta(delta)
                if (visibilityChange != null) {
                    navbarVisible.value = visibilityChange
                }
            }
    }
}

@Composable
fun ObserveScrollForNavbar(gridState: androidx.compose.foundation.lazy.grid.LazyGridState) {
    val navbarVisible = LocalNavbarVisible.current
    val thresholdPx = with(LocalDensity.current) { 28.dp.toPx() }

    LaunchedEffect(gridState, thresholdPx) {
        var previousIndex = gridState.firstVisibleItemIndex
        var previousOffset = gridState.firstVisibleItemScrollOffset
        val accumulator = ScrollDirectionAccumulator(thresholdPx)

        snapshotFlow {
            Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                val isNearTop = currentIndex == 0 && currentOffset < thresholdPx
                if (isNearTop) {
                    navbarVisible.value = true
                    accumulator.reset()
                    previousIndex = currentIndex
                    previousOffset = currentOffset
                    return@collect
                }

                val itemHeight = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: thresholdPx.toInt()
                val delta = computeScrollDelta(
                    currentIndex = currentIndex,
                    currentOffset = currentOffset,
                    previousIndex = previousIndex,
                    previousOffset = previousOffset,
                    itemDimension = itemHeight,
                    thresholdPx = thresholdPx
                )

                previousIndex = currentIndex
                previousOffset = currentOffset

                val visibilityChange = accumulator.onDelta(delta)
                if (visibilityChange != null) {
                    navbarVisible.value = visibilityChange
                }
            }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    registry: PluginRegistry = PluginRegistry.Default
) {
    val reduceMotion = LocalReduceMotion.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavTab.HOME.route
    val isFullscreen = currentRoute.startsWith("details") || currentRoute.startsWith("player") || currentRoute.startsWith("plugin_manager") || currentRoute.startsWith("settings/")

    val navbarVisible = remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val preferencesRepository = remember { com.euthopiar.data.preferences.PreferencesRepository.getInstance(context) }
    val cardScaleMode by preferencesRepository.cardScaleMode.collectAsState(initial = com.euthopiar.data.preferences.CardScaleMode.FLAGSHIP)
    val navbarStyle by preferencesRepository.navbarStyle.collectAsState(initial = com.euthopiar.data.preferences.NavbarStyle.FLOATING)
    val firstRunCompleted by preferencesRepository.firstRunCompleted.collectAsState(initial = null)
    val isAmoledMode by preferencesRepository.amoledMode.collectAsState(initial = true)

    val windowDimensions = com.euthopiar.ui.theme.rememberWindowDimensions()

    val snackbarHostState = remember { AppSnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarController = remember(snackbarHostState, coroutineScope) {
        AppSnackbarController(snackbarHostState, coroutineScope)
    }

    val tabReselectionEvents = remember { kotlinx.coroutines.flow.MutableSharedFlow<NavTab>(extraBufferCapacity = 1) }
    val navbarHazeState = rememberHazeState()

    CompositionLocalProvider(
        LocalNavbarVisible provides navbarVisible,
        LocalNavbarHazeState provides navbarHazeState,
        com.euthopiar.ui.components.LocalCardScaleMode provides cardScaleMode,
        LocalSnackbarController provides snackbarController,
        LocalTabReselection provides tabReselectionEvents,
        com.euthopiar.ui.theme.LocalAmoledMode provides isAmoledMode
    ) {
        val profileManager = remember { com.euthopiar.data.profile.ProfileManager.getInstance(context) }
        val activeProfile by profileManager.activeProfile.collectAsState()

        if (firstRunCompleted == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return@CompositionLocalProvider
        }

        if (firstRunCompleted == false) {
            com.euthopiar.ui.onboarding.OnboardingScreen(
                onComplete = {
                    coroutineScope.launch {
                        preferencesRepository.setFirstRunCompleted(true)
                    }
                }
            )
            return@CompositionLocalProvider
        }

        val unlockedProfileIds by profileManager.unlockedProfileIds.collectAsState()
        val isCurrentProfileUnlocked = !activeProfile.isPinLocked || unlockedProfileIds.contains(activeProfile.id)
        var showColdStartSwitcher by remember { mutableStateOf(false) }

        if (!isCurrentProfileUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                DetroitProfileUnlockDialog(
                    profile = activeProfile,
                    onDismissRequest = {
                        activity?.finish()
                    },
                    onUnlocked = {
                        profileManager.unlockProfile(activeProfile.id)
                    },
                    onSwitchProfileClick = {
                        showColdStartSwitcher = true
                    }
                )

                if (showColdStartSwitcher) {
                    QuickProfileSwitcherDialog(
                        onDismissRequest = { showColdStartSwitcher = false },
                        onManageProfilesClick = {
                            showColdStartSwitcher = false
                        }
                    )
                }
            }
            return@CompositionLocalProvider
        }

        val showBottomNav = windowDimensions.isPhone && !isFullscreen
        val showNavRail = !windowDimensions.isPhone && !isFullscreen

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                        if (showNavRail) {
                            AdaptiveNavigationRail(
                                currentRoute = currentRoute,
                                onTabSelected = { tab ->
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onTabReselected = { tab ->
                                    val currentDest = navController.currentDestination?.route
                                    if (currentDest != null && currentDest != tab.route) {
                                        navController.popBackStack(tab.route, inclusive = false)
                                    } else {
                                        coroutineScope.launch {
                                            tabReselectionEvents.emit(tab)
                                        }
                                    }
                                }
                            )
                        }

                    NavHost(
                        navController = navController,
                        startDestination = NavTab.HOME.route,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn(animationSpec = tween(150)) },
                        exitTransition = { fadeOut(animationSpec = tween(150)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                        popExitTransition = { fadeOut(animationSpec = tween(150)) }
                    ) {
            composable(NavTab.HOME.route) {
                HomeScreen(
                    registry = registry,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onMediaClick = { item: MediaItem ->
                        navController.navigate(
                            buildDetailsRoute(
                                mediaId = item.id,
                                provider = item.provider,
                                type = item.type.name,
                                title = item.title,
                                posterUrl = item.posterUrl ?: item.backdropUrl
                            )
                        )
                    },
                    onContinueWatchingClick = { item ->
                        val isTv = item.mediaType == "TV_SERIES" || item.mediaType == "ANIME" || (item.mediaType == null && item.seasonNumber != null && item.episodeNumber != null)
                        val typeStr = if (isTv) (item.mediaType ?: "TV_SERIES") else "MOVIE"
                        val epData = item.episodeData ?: if (isTv) "${item.mediaId}:${item.seasonNumber ?: 1}:${item.episodeNumber ?: 1}" else item.mediaId
                        val season = if (isTv) (item.seasonNumber ?: 1) else 0
                        val episode = if (isTv) (item.episodeNumber ?: 1) else 0
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = item.mediaId,
                                provider = item.provider,
                                type = typeStr,
                                episodeData = epData,
                                mediaTitle = item.title,
                                episodeTitle = if (isTv) item.episodeTitle else null,
                                season = season,
                                episode = episode
                            )
                        )
                    },
                    onPlayHero = { item, history ->
                        val isTv = item.type == com.euthopiar.core.model.MediaType.TV_SERIES ||
                            item.type == com.euthopiar.core.model.MediaType.ANIME ||
                            history?.mediaType == "TV_SERIES" ||
                            history?.mediaType == "ANIME" ||
                            (history?.seasonNumber != null && history.seasonNumber > 0)
                        val typeStr = if (isTv) (history?.mediaType ?: item.type.name) else "MOVIE"
                        val epData = history?.episodeData ?: if (isTv) "${item.id}:${history?.seasonNumber ?: 1}:${history?.episodeNumber ?: 1}" else item.id
                        val season = if (isTv) (history?.seasonNumber ?: 1) else 0
                        val episode = if (isTv) (history?.episodeNumber ?: 1) else 0
                        val title = history?.title?.ifBlank { item.title } ?: item.title
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = item.id,
                                provider = history?.provider ?: item.provider,
                                type = typeStr,
                                episodeData = epData,
                                mediaTitle = title,
                                episodeTitle = if (isTv) history?.episodeTitle else null,
                                season = season,
                                episode = episode
                            )
                        )
                    },
                    onNavigateToPluginManager = {
                        navController.navigate("plugin_manager")
                    }
                )
            }

            composable(NavTab.SEARCH.route) {
                SearchScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onMediaClick = { item: MediaItem ->
                        navController.navigate(
                            buildDetailsRoute(
                                mediaId = item.id,
                                provider = item.provider,
                                type = item.type.name,
                                title = item.title,
                                posterUrl = item.posterUrl ?: item.backdropUrl
                            )
                        )
                    },
                    onPlayMedia = { item: MediaItem ->
                        val isTv = item.type == com.euthopiar.core.model.MediaType.TV_SERIES || item.type == com.euthopiar.core.model.MediaType.ANIME
                        val typeStr = item.type.name
                        val epData = if (isTv) "${item.id}:1:1" else item.id
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = item.id,
                                provider = item.provider,
                                type = typeStr,
                                episodeData = epData,
                                mediaTitle = item.title,
                                episodeTitle = null,
                                season = if (isTv) 1 else 0,
                                episode = if (isTv) 1 else 0
                            )
                        )
                    },
                    onNavigateToPluginManager = {
                        navController.navigate("plugin_manager")
                    }
                )
            }

            composable(
                route = "details/{mediaId}?provider={provider}&type={type}&depth={depth}&title={title}&posterUrl={posterUrl}",
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("provider") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("depth") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("posterUrl") { type = NavType.StringType; nullable = true; defaultValue = null }
                ),
                enterTransition = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(180))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200))
                    }
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) {
                        fadeOut(animationSpec = tween(180))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    }
                }
            ) { backStackEntry ->
                val rawMediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                val mediaId = safeDecode(rawMediaId)
                val provider = backStackEntry.arguments?.getString("provider")
                val type = backStackEntry.arguments?.getString("type")
                val depth = backStackEntry.arguments?.getInt("depth") ?: 1
                val rawTitle = backStackEntry.arguments?.getString("title")
                val initialTitle = rawTitle?.takeIf { it.isNotBlank() }
                val rawPosterUrl = backStackEntry.arguments?.getString("posterUrl")
                val initialPosterUrl = rawPosterUrl?.takeIf { it.isNotBlank() }
                MediaDetailsScreen(
                    mediaId = mediaId,
                    provider = provider,
                    type = type,
                    initialTitle = initialTitle,
                    initialPosterUrl = initialPosterUrl,
                    depth = depth,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCloseToRoot = {
                        navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
                    },
                    onNavigateToMedia = { rec ->
                        val p = rec.provider ?: provider ?: "tmdb"
                        navController.navigate(
                            buildDetailsRoute(
                                mediaId = rec.id,
                                provider = p,
                                type = rec.type.name,
                                depth = depth + 1,
                                title = rec.title,
                                posterUrl = rec.posterUrl ?: rec.backdropUrl
                            )
                        )
                    },
                    onPlayMedia = { detail, episode ->
                        val prov = detail.provider ?: provider ?: "tmdb"
                        val isTv = detail.type == com.euthopiar.core.model.MediaType.TV_SERIES || detail.type == com.euthopiar.core.model.MediaType.ANIME
                        val season = if (isTv) (episode?.seasonNumber ?: 1) else 0
                        val epNum = if (isTv) (episode?.episodeNumber ?: 1) else 0
                        val rawEpData = if (isTv) episode?.data else (detail.episodes.firstOrNull()?.data ?: episode?.data)
                        val epData = if (isTv) {
                            if (!rawEpData.isNullOrBlank() && rawEpData.contains(":")) rawEpData else "${detail.id}:$season:$epNum"
                        } else {
                            rawEpData ?: detail.id
                        }
                        val epTitle = if (isTv) (episode?.title ?: "") else ""
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = detail.id,
                                provider = prov,
                                type = detail.type.name,
                                episodeData = epData,
                                mediaTitle = detail.title,
                                episodeTitle = epTitle,
                                season = season,
                                episode = epNum
                            )
                        )
                    },
                    onNavigateToPluginManager = {
                        navController.navigate("plugin_manager")
                    }
                )
            }

            composable(
                route = "player/{mediaId}?provider={provider}&type={type}&episodeData={episodeData}&mediaTitle={mediaTitle}&episodeTitle={episodeTitle}&season={season}&episode={episode}",
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("provider") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("episodeData") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("mediaTitle") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("episodeTitle") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("season") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 0 }
                ),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
                popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                popExitTransition = { fadeOut(animationSpec = tween(200)) }
            ) { backStackEntry ->
                val rawMediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                val mediaId = safeDecode(rawMediaId)
                val provider = backStackEntry.arguments?.getString("provider")
                val type = backStackEntry.arguments?.getString("type")
                val rawEpData = backStackEntry.arguments?.getString("episodeData")
                val episodeData = rawEpData?.takeIf { it.isNotBlank() }
                val rawTitle = backStackEntry.arguments?.getString("mediaTitle")
                val mediaTitle = rawTitle?.takeIf { it.isNotBlank() }
                val isTv = type == "TV_SERIES" || type == "ANIME"
                val rawEpTitle = backStackEntry.arguments?.getString("episodeTitle")
                val episodeTitle = if (isTv) rawEpTitle?.takeIf { it.isNotBlank() } else null
                val rawSeason = backStackEntry.arguments?.getInt("season") ?: 0
                val rawEpisode = backStackEntry.arguments?.getInt("episode") ?: 0
                val season = if (isTv && rawSeason > 0) rawSeason else null
                val episode = if (isTv && rawEpisode > 0) rawEpisode else null

                PlayerScreen(
                    mediaId = mediaId,
                    provider = provider,
                    type = type,
                    episodeData = episodeData,
                    mediaTitle = mediaTitle,
                    episodeTitle = episodeTitle,
                    initialSeason = season,
                    initialEpisode = episode,
                    onBack = { navController.popBackStack() },
                    onNavigateToPluginManager = { navController.navigate("plugin_manager") }
                )
            }

            composable(NavTab.LIBRARY.route) {
                com.euthopiar.ui.library.LibraryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onMediaClick = { mediaId, type, prov ->
                        val providerName = prov ?: registry.activePlugin.value.name
                        navController.navigate(
                            buildDetailsRoute(
                                mediaId = mediaId,
                                provider = providerName,
                                type = type.name
                            )
                        )
                    },
                    onPlayMedia = { mediaId, title, epTitle, season, episode, type, prov, epData ->
                        val providerName = prov ?: registry.activePlugin.value.name
                        val isTv = type == com.euthopiar.core.model.MediaType.TV_SERIES || type == com.euthopiar.core.model.MediaType.ANIME
                        val streamEpData = if (isTv) {
                            if (!epData.isNullOrBlank() && epData.contains(":")) epData else "$mediaId:${season ?: 1}:${episode ?: 1}"
                        } else {
                            epData ?: mediaId
                        }
                        val s = if (isTv) (season ?: 1) else 0
                        val ep = if (isTv) (episode ?: 1) else 0
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = mediaId,
                                provider = providerName,
                                type = type.name,
                                episodeData = streamEpData,
                                mediaTitle = title,
                                episodeTitle = if (isTv) epTitle else null,
                                season = s,
                                episode = ep
                            )
                        )
                    },
                    onPlayDownload = { downloadItem ->
                        val epData = downloadItem.episodeId ?: downloadItem.id
                        val typeStr = if (downloadItem.isTvSeries) "TV_SERIES" else "MOVIE"
                        val prov = downloadItem.provider ?: "Offline"
                        val s = if (downloadItem.isTvSeries) (downloadItem.seasonNumber ?: 1) else 0
                        val ep = if (downloadItem.isTvSeries) (downloadItem.episodeNumber ?: 1) else 0
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = downloadItem.mediaId,
                                provider = prov,
                                type = typeStr,
                                episodeData = epData,
                                mediaTitle = downloadItem.title,
                                episodeTitle = downloadItem.subtitle,
                                season = s,
                                episode = ep
                            )
                        )
                    },
                    onExploreClick = {
                        navController.navigate(NavTab.HOME.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(
                route = "${NavTab.LIBRARY.route}?filter={filter}",
                arguments = listOf(
                    navArgument("filter") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val filterArg = backStackEntry.arguments?.getString("filter")
                val initialFilter = when (filterArg?.lowercase()) {
                    "downloads" -> com.euthopiar.ui.library.LibraryFilter.DOWNLOADS
                    "watchlist" -> com.euthopiar.ui.library.LibraryFilter.WATCHLIST
                    "history" -> com.euthopiar.ui.library.LibraryFilter.HISTORY
                    "favorites" -> com.euthopiar.ui.library.LibraryFilter.FAVORITES
                    else -> null
                }
                com.euthopiar.ui.library.LibraryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onMediaClick = { mediaId, type, prov ->
                        val providerName = prov ?: registry.activePlugin.value.name
                        navController.navigate(
                            buildDetailsRoute(
                                mediaId = mediaId,
                                provider = providerName,
                                type = type.name
                            )
                        )
                    },
                    onPlayMedia = { mediaId, title, epTitle, season, episode, type, prov, epData ->
                        val providerName = prov ?: registry.activePlugin.value.name
                        val isTv = type == com.euthopiar.core.model.MediaType.TV_SERIES || type == com.euthopiar.core.model.MediaType.ANIME
                        val streamEpData = if (isTv) {
                            if (!epData.isNullOrBlank() && epData.contains(":")) epData else "$mediaId:${season ?: 1}:${episode ?: 1}"
                        } else {
                            epData ?: mediaId
                        }
                        val s = if (isTv) (season ?: 1) else 0
                        val ep = if (isTv) (episode ?: 1) else 0
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = mediaId,
                                provider = providerName,
                                type = type.name,
                                episodeData = streamEpData,
                                mediaTitle = title,
                                episodeTitle = if (isTv) epTitle else null,
                                season = s,
                                episode = ep
                            )
                        )
                    },
                    onPlayDownload = { downloadItem ->
                        val epData = downloadItem.episodeId ?: downloadItem.id
                        val typeStr = if (downloadItem.isTvSeries) "TV_SERIES" else "MOVIE"
                        val prov = downloadItem.provider ?: "Offline"
                        val s = if (downloadItem.isTvSeries) (downloadItem.seasonNumber ?: 1) else 0
                        val ep = if (downloadItem.isTvSeries) (downloadItem.episodeNumber ?: 1) else 0
                        navController.navigate(
                            buildPlayerRoute(
                                mediaId = downloadItem.mediaId,
                                provider = prov,
                                type = typeStr,
                                episodeData = epData,
                                mediaTitle = downloadItem.title,
                                episodeTitle = downloadItem.subtitle,
                                season = s,
                                episode = ep
                            )
                        )
                    },
                    onExploreClick = {
                        navController.navigate(NavTab.HOME.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    initialFilter = initialFilter
                )
            }

            composable("downloads") {
                LaunchedEffect(Unit) {
                    navController.navigate("${NavTab.LIBRARY.route}?filter=downloads") {
                        popUpTo("downloads") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            composable(NavTab.PROFILE.route) {
                com.euthopiar.ui.profile.ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onNavigateToPage = { route -> navController.navigate(route) }
                )
            }

            composable("settings") {
                com.euthopiar.ui.profile.ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = navbarHazeState),
                    onNavigateToPage = { route -> navController.navigate(route) }
                )
            }

            composable(
                route = "settings/playback",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.PlaybackSettingsPage(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/appearance",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.AppearanceSettingsPage(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/integrations",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.IntegrationsSettingsPage(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/plugins",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.PluginsSettingsPage(
                    onBack = { navController.popBackStack() },
                    onNavigateToRepo = { repoRoute -> navController.navigate(repoRoute) }
                )
            }

            composable(
                route = "settings/plugins/repo/{encodedRepoUrl}",
                arguments = listOf(
                    androidx.navigation.navArgument("encodedRepoUrl") {
                        type = androidx.navigation.NavType.StringType
                    }
                ),
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedRepoUrl") ?: ""
                val decodedUrl = try {
                    java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                } catch (_: Exception) {
                    encodedUrl
                }
                com.euthopiar.ui.settings.pages.RepoPluginsScreen(
                    repoUrl = decodedUrl,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/storage",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.StorageSettingsPage(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/updates",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.UpdateSettingsPage(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "settings/about",
                enterTransition = {
                    if (reduceMotion) fadeIn(animationSpec = tween(180)) else slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) fadeOut(animationSpec = tween(140)) else slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(160, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(140))
                }
            ) {
                com.euthopiar.ui.settings.pages.AboutSettingsPage(
                    onBack = { navController.popBackStack() },
                    onNavigateToUpdates = { navController.navigate("settings/updates") }
                )
            }

            composable(
                route = "plugin_manager",
                enterTransition = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(180))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200))
                    }
                },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = {
                    if (reduceMotion) {
                        fadeOut(animationSpec = tween(180))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    }
                }
            ) {
                com.euthopiar.ui.settings.PluginManagerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

            if (showBottomNav) {
                FloatingNavbar(
                    currentRoute = currentRoute,
                    style = navbarStyle,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTabReselected = { tab ->
                        val currentDest = navController.currentDestination?.route
                        if (currentDest != null && currentDest != tab.route) {
                            navController.popBackStack(tab.route, inclusive = false)
                        } else {
                            coroutineScope.launch {
                                tabReselectionEvents.emit(tab)
                            }
                        }
                    },
                    isVisible = navbarVisible.value,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            val targetSnackbarPadding = if (showBottomNav && navbarVisible.value) 94.dp else Dimens.spacing16
            val snackbarBottomPadding by animateDpAsState(
                targetValue = targetSnackbarPadding,
                animationSpec = if (reduceMotion) {
                    snap()
                } else if (targetSnackbarPadding > Dimens.spacing16) {
                    tween(durationMillis = 220, easing = AppleDecelCurve)
                } else {
                    tween(durationMillis = 140, easing = HudExitEasing)
                },
                label = "snackbarBottomPadding"
            )
            AppSnackbarHost(
                hostState = snackbarHostState,
                bottomPadding = snackbarBottomPadding
            )
        }
    }
}
}

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Dimens.spacing24, vertical = Dimens.spacing24)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Text(
                text = subtitle,
                style = AppTypography.bodyMedium,
                color = TextTertiary
            )
        }
    }
}

internal fun safeDoubleEncode(value: String): String {
    return URLEncoder.encode(URLEncoder.encode(value, "UTF-8"), "UTF-8")
}

internal fun safeDecode(value: String): String {
    return try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }
}

internal fun buildDetailsRoute(
    mediaId: String,
    provider: String? = null,
    type: String? = null,
    depth: Int = 1,
    title: String? = null,
    posterUrl: String? = null
): String {
    val safeId = mediaId.trim().ifBlank { "unknown" }
    val encodedId = safeDoubleEncode(safeId)
    val prov = if (provider.isNullOrBlank()) "tmdb" else provider
    val encodedProv = URLEncoder.encode(prov, "UTF-8")
    val typeStr = type ?: "MOVIE"
    val encodedTitle = title?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
    val posterQuery = if (!posterUrl.isNullOrBlank()) "&posterUrl=${URLEncoder.encode(posterUrl, "UTF-8")}" else ""
    return "details/$encodedId?provider=$encodedProv&type=$typeStr&depth=$depth&title=$encodedTitle$posterQuery"
}

internal fun buildPlayerRoute(
    mediaId: String,
    provider: String? = null,
    type: String? = null,
    episodeData: String? = null,
    mediaTitle: String? = null,
    episodeTitle: String? = null,
    season: Int = 0,
    episode: Int = 0
): String {
    val safeId = mediaId.trim().ifBlank { "unknown" }
    val encodedMediaId = safeDoubleEncode(safeId)
    val prov = if (provider.isNullOrBlank()) "tmdb" else provider
    val encodedProv = URLEncoder.encode(prov, "UTF-8")
    val typeStr = type ?: "MOVIE"
    val encodedEpData = episodeData?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
    val encodedTitle = mediaTitle?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
    val encodedEpTitle = episodeTitle?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
    return "player/$encodedMediaId?provider=$encodedProv&type=$typeStr&episodeData=$encodedEpData&mediaTitle=$encodedTitle&episodeTitle=$encodedEpTitle&season=$season&episode=$episode"
}
