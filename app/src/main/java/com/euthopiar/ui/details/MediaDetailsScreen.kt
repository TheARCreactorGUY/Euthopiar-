package com.euthopiar.ui.details

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import android.net.ConnectivityManager
import android.os.PowerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import com.euthopiar.download.DownloadItem
import com.euthopiar.download.DownloadRepository
import com.euthopiar.download.DownloadState
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.details.sheets.MediaDetailsActionSheet
import com.euthopiar.ui.details.sheets.SheetScreen
import com.euthopiar.ui.library.components.AddToPlaylistSheet
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.VelvetAlertDialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.euthopiar.core.model.CastMember
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.components.AppTabRow
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.components.ContentRatingBadge
import com.euthopiar.ui.components.ErrorState
import com.euthopiar.ui.components.MetaLine
import com.euthopiar.ui.components.PosterCard
import com.euthopiar.ui.components.PrimaryButton
import com.euthopiar.ui.components.ProgressBarThin
import com.euthopiar.ui.components.QualityBadge
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Error
import com.euthopiar.ui.theme.GlassDefaults
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.Motion
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.ShapeM
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.Success
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.SurfaceMid
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.TextTertiary
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions
import com.euthopiar.ui.util.TrimTransparentTransformation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.euthopiar.ui.longpress.LocalLongPressHazeState
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    mediaId: String,
    provider: String? = null,
    type: String? = null,
    initialTitle: String? = null,
    initialPosterUrl: String? = null,
    depth: Int = 1,
    onBack: () -> Unit,
    onCloseToRoot: (() -> Unit)? = null,
    onPlayMedia: (detail: MediaDetail, episode: EpisodeItem?) -> Unit = { _, _ -> },
    onNavigateToMedia: ((MediaItem) -> Unit)? = null,
    onNavigateToPluginManager: (() -> Unit)? = null,
    viewModel: MediaDetailsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pluginService = remember { com.euthopiar.app.plugin.PluginRepositoryService.getInstance(context) }
    val installedPlugins by pluginService.installedPluginsFlow.collectAsState()
    val addonRepo = remember { com.euthopiar.core.addons.AddonRepository.getInstance(context) }
    val addonState by addonRepo.uiState.collectAsState()
    var showNoPluginsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.euthopiar.core.p2p.P2pStreamingEngine.prewarmEngine(context)
    }

    val hasAnyStreamProviders = remember(installedPlugins, addonState.addons) {
        installedPlugins.any { it.isEnabled } || addonState.addons.any { it.enabled }
    }

    var isNavigatingAway by remember { mutableStateOf(false) }
    val safeNavigate: (() -> Unit) -> Unit = remember {
        { action ->
            isNavigatingAway = true
            action()
        }
    }

    val handlePlayRequest: (detail: MediaDetail, episode: EpisodeItem?) -> Unit = remember(hasAnyStreamProviders, onPlayMedia) {
        { d, ep ->
            if (!hasAnyStreamProviders) {
                showNoPluginsDialog = true
            } else {
                safeNavigate { onPlayMedia(d, ep) }
            }
        }
    }
    BackHandler(onBack = { safeNavigate(onBack) })
    val windowDims = rememberWindowDimensions()
    val parsedType = remember(type) {
        when (type?.uppercase()) {
            "TV_SERIES" -> MediaType.TV_SERIES
            "ANIME" -> MediaType.ANIME
            else -> MediaType.MOVIE
        }
    }

    LaunchedEffect(mediaId, provider, parsedType) {
        viewModel.loadDetails(mediaId = mediaId, provider = provider, type = parsedType)
    }

    val uiState by viewModel.uiState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val isWatchlist by viewModel.isWatchlist.collectAsState()
    val watchlistStatus by viewModel.watchlistStatus.collectAsState()
    val latestPlayback by viewModel.latestPlayback.collectAsState()
    val episodePlaybackMap by viewModel.episodePlaybackMap.collectAsState()
    val castMembers by viewModel.cast.collectAsState()
    val selectedAudioType by viewModel.selectedAudioType.collectAsState()
    val isInPlaylist by viewModel.isInPlaylist.collectAsState()
    val snackbarController = LocalSnackbarController.current
    val downloadRepository = remember { DownloadRepository.getInstance(context) }
    val downloads by downloadRepository.downloads.collectAsState()

    val detailsHazeState = rememberHazeState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var showSeasonSheet by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var showWatchlistSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedEpisodeForPlaylist by remember { mutableStateOf<EpisodeItem?>(null) }
    var selectedRelatedMediaForPlaylist by remember { mutableStateOf<com.euthopiar.core.model.MediaItem?>(null) }
    var showTrailerDialog by remember { mutableStateOf(false) }
    var episodeToDownload by remember { mutableStateOf<EpisodeItem?>(null) }

    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val isMetered = try { connectivityManager?.isActiveNetworkMetered == true } catch (_: Exception) { false }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isPowerSaver = try { powerManager?.isPowerSaveMode == true } catch (_: Exception) { false }

    var isAmbientMuted by remember { mutableStateOf(true) }
    var isTrailerPlaybackActive by remember { mutableStateOf(false) }
    var isBackdropTrailerEnabled by remember(mediaId) { mutableStateOf(true) }
    var hasTrailerFailed by remember(mediaId) { mutableStateOf(false) }

    LaunchedEffect(mediaId) {
        isTrailerPlaybackActive = false
        isBackdropTrailerEnabled = true
        hasTrailerFailed = false
    }

    val coroutineScope = rememberCoroutineScope()

    val isAmoled = LocalAmoledMode.current
    val screenBg = if (isAmoled) Color.Black else Background

    CompositionLocalProvider(LocalLongPressHazeState provides detailsHazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            Crossfade(
                targetState = uiState,
                animationSpec = tween(durationMillis = Motion.DurationEmphasized),
                label = "MediaDetailsTransition"
            ) { state ->
            when (state) {
                is MediaDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                    }
                }

                is MediaDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            error = state.error,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }

                is MediaDetailsUiState.Success -> {
                    val detail = state.detail
                    val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME

                    // Target episode and resume point calculation
                    val (targetEpisodeForPlay, resumePlaybackForPlay) = remember(detail, selectedSeason, episodePlaybackMap, latestPlayback) {
                        PlaybackTargetHelper.resolveDetailsPlaybackTarget(
                            detail = detail,
                            selectedSeason = selectedSeason,
                            episodePlaybackMap = episodePlaybackMap,
                            latestPlayback = latestPlayback
                        )
                    }

                    // Episode pagination / chunking (max 20 episodes displayed at a time)
                    val lazySeasonEpisodes = remember(detail.episodes, selectedSeason) {
                        detail.episodes.filter { it.seasonNumber == (selectedSeason ?: 1) }
                    }
                    val chunkSize = 20
                    val totalEpisodes = lazySeasonEpisodes.size
                    val isChunked = totalEpisodes > chunkSize
                    val chunkCount = if (isChunked) (totalEpisodes + chunkSize - 1) / chunkSize else 1
                    var selectedChunkIndex by remember(detail.id, selectedSeason) { mutableIntStateOf(0) }
                    val safeChunkIndex = selectedChunkIndex.coerceIn(0, (chunkCount - 1).coerceAtLeast(0))
                    val displayedEpisodes = remember(lazySeasonEpisodes, safeChunkIndex, isChunked) {
                        if (!isChunked) lazySeasonEpisodes
                        else {
                            val s = safeChunkIndex * chunkSize
                            lazySeasonEpisodes.subList(s, minOf(s + chunkSize, totalEpisodes))
                        }
                    }

                    if (windowDims.isTablet) {
                        // Tablet / TV: Adaptive Two-Column Layout
                        val canPlayAmbientTrailer = !hasTrailerFailed &&
                            isBackdropTrailerEnabled &&
                            !detail.trailerUrl.isNullOrBlank() &&
                            !isMetered &&
                            !isPowerSaver &&
                            !showTrailerDialog &&
                            !isNavigatingAway

                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(state = detailsHazeState)
                            ) {
                                // Left Column: Artwork, Header, Play, Overview
                                val leftScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight()
                                        .verticalScroll(leftScrollState)
                                        .padding(Dimens.spacing24)
                                ) {
                                    DetailsBackdropHero(
                                        backdropUrl = detail.backdropUrl ?: detail.posterUrl,
                                        logoUrl = detail.logoUrl,
                                        title = detail.title,
                                        trailerUrl = detail.trailerUrl,
                                        canPlayTrailer = canPlayAmbientTrailer,
                                        isMuted = isAmbientMuted,
                                        onPlaybackActive = { isTrailerPlaybackActive = it },
                                        onPlaybackError = {
                                            hasTrailerFailed = true
                                            isTrailerPlaybackActive = false
                                        },
                                        mediaId = detail.id,
                                        onHeroClick = if (!detail.trailerUrl.isNullOrBlank() && !hasTrailerFailed) {
                                            { isBackdropTrailerEnabled = !isBackdropTrailerEnabled }
                                        } else null,
                                        showMuteButton = !hasTrailerFailed && isBackdropTrailerEnabled && isTrailerPlaybackActive,
                                        onToggleMute = { isAmbientMuted = !isAmbientMuted },
                                        hasFailed = hasTrailerFailed
                                    )

                                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                                    DetailsTitleHeader(
                                        title = detail.title,
                                        logoUrl = detail.logoUrl
                                    )

                                    Spacer(modifier = Modifier.height(Dimens.spacing8))

                                    DetailsMetaBlock(detail = detail)

                                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                                    DetailsActionBlock(
                                        detail = detail,
                                        targetEpisode = targetEpisodeForPlay,
                                        latestPlayback = resumePlaybackForPlay,
                                        isInWatchlist = isWatchlist,
                                        watchlistStatus = watchlistStatus,
                                        isInPlaylist = isInPlaylist,
                                        downloads = downloads,
                                        onPlay = { handlePlayRequest(detail, targetEpisodeForPlay) },
                                        onWatchlist = { showWatchlistSheet = true },
                                        onDownload = {
                                            episodeToDownload = if (isTv) {
                                                targetEpisodeForPlay ?: selectedSeason?.let { s ->
                                                    detail.episodes.firstOrNull { it.seasonNumber == s }
                                                } ?: detail.episodes.firstOrNull() ?: EpisodeItem(
                                                    id = "${detail.id}:1:1",
                                                    title = "Episode 1",
                                                    seasonNumber = 1,
                                                    episodeNumber = 1,
                                                    data = "${detail.id}:1:1"
                                                )
                                            } else null
                                            showDownloadSheet = true
                                        },
                                        onShare = {
                                            val shareYear = if (detail.year != null && detail.year != 0) " (${detail.year})" else ""
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                setType("text/plain")
                                                putExtra(Intent.EXTRA_SUBJECT, detail.title)
                                                putExtra(Intent.EXTRA_TEXT, "Watch ${detail.title}$shareYear on Euthopiar")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share ${detail.title}"))
                                        },
                                        onTrailer = if (!detail.trailerUrl.isNullOrBlank()) { { showTrailerDialog = true } } else null,
                                        onPlaylist = { showAddToPlaylistSheet = true }
                                    )

                                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                                    ExpandableOverview(overview = detail.synopsis)
                                }

                                // Right Column: Prime Video Style Tabs
                                val rightScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight()
                                        .verticalScroll(rightScrollState)
                                        .padding(top = Dimens.spacing24, end = Dimens.spacing24, bottom = Dimens.spacing24)
                                ) {
                                    DetailsTabsContent(
                                        detail = detail,
                                        isTv = isTv,
                                        selectedSeason = selectedSeason ?: 1,
                                        castMembers = castMembers,
                                        episodePlaybackMap = episodePlaybackMap,
                                        downloads = downloads,
                                        onSelectSeason = { showSeasonSheet = true },
                                        onPlayEpisode = { ep -> handlePlayRequest(detail, ep) },
                                        onDownloadEpisode = { ep ->
                                            episodeToDownload = ep
                                            showDownloadSheet = true
                                        },
                                        onToggleWatched = { ep -> viewModel.toggleEpisodeWatched(detail, ep) },
                                        onNavigateToMedia = onNavigateToMedia?.let { nav -> { item -> safeNavigate { nav(item) } } },
                                        onAddToPlaylistEpisode = { selectedEpisodeForPlaylist = it },
                                        onAddToPlaylistMedia = { selectedRelatedMediaForPlaylist = it },
                                        selectedTabIndex = selectedTabIndex,
                                        onTabSelected = { selectedTabIndex = it },
                                        hazeState = detailsHazeState,
                                        isTabRowVisible = true,
                                        displayedEpisodes = displayedEpisodes,
                                        totalEpisodesCount = totalEpisodes,
                                        chunkCount = chunkCount,
                                        selectedChunkIndex = safeChunkIndex,
                                        chunkSize = chunkSize,
                                        onSelectChunk = { selectedChunkIndex = it },
                                        onScrollToEpisodesHeader = {
                                            coroutineScope.launch {
                                                rightScrollState.animateScrollTo(0)
                                            }
                                        },
                                        renderEpisodes = true
                                    )
                                }
                            }

                            // Tablet Floating Glass Back and Close Buttons (Fix for tablet navigation)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(start = Dimens.spacing24, top = Dimens.spacing8)
                                    .zIndex(10f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MediaDetailsGlassIconButton(
                                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        onClick = { safeNavigate(onBack) },
                                        size = 40.dp,
                                        radius = 14.dp,
                                        iconSize = 20.dp,
                                        hazeState = detailsHazeState,
                                        isOverArtwork = true
                                    )
                                    if (depth >= 3 && onCloseToRoot != null) {
                                        MediaDetailsGlassIconButton(
                                            icon = Icons.Filled.Close,
                                            contentDescription = "Close to tab root",
                                            onClick = { safeNavigate { onCloseToRoot() } },
                                            size = 40.dp,
                                            radius = 14.dp,
                                            iconSize = 20.dp,
                                            hazeState = detailsHazeState,
                                            isOverArtwork = true
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Phone: Single-Column Layout
                        val lazyListState = rememberLazyListState()
                        val configuration = LocalConfiguration.current
                        val density = LocalDensity.current
                        val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        val statusBarInsetPx = with(density) { statusBarInset.toPx() }
                        val heroHeight = (configuration.screenHeightDp * 0.38f).coerceIn(240f, 340f).dp
                        val heroHeightPx = with(density) { heroHeight.toPx() }
                        val topBarHeight = statusBarInset + 68.dp
                        val topBarHeightPx = with(density) { topBarHeight.toPx() }

                        var metaActionsHeightPx by remember { mutableFloatStateOf(0f) }

                        val isAtTop by remember {
                            derivedStateOf {
                                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 70
                            }
                        }

                        val heroScrollAlpha by remember {
                            derivedStateOf {
                                if (lazyListState.firstVisibleItemIndex > 0) {
                                    0f
                                } else {
                                    val offset = lazyListState.firstVisibleItemScrollOffset.toFloat()
                                    (1f - (offset / 320f)).coerceIn(0f, 1f)
                                }
                            }
                        }

                        val totalScrollPx by remember {
                            derivedStateOf {
                                val firstIndex = lazyListState.firstVisibleItemIndex
                                val firstOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
                                when {
                                    firstIndex == 0 -> firstOffset
                                    firstIndex == 1 -> heroHeightPx + firstOffset
                                    else -> heroHeightPx + metaActionsHeightPx + firstOffset
                                }
                            }
                        }

                        val playButtonThresholdPx = heroHeightPx + with(density) { 90.dp.toPx() }
                        val fadeDistancePx = with(density) { 60.dp.toPx() }
                        val topBarProgress by remember {
                            derivedStateOf {
                                if (lazyListState.firstVisibleItemIndex >= 2) 1f
                                else ((totalScrollPx - playButtonThresholdPx) / fadeDistancePx).coerceIn(0f, 1f)
                            }
                        }
                        val isCompactTopBarVisible by remember {
                            derivedStateOf { topBarProgress > 0.01f }
                        }

                        val reduceMotion = LocalReduceMotion.current
                        val compactBarAlpha = if (reduceMotion) (if (isCompactTopBarVisible) 1f else 0f) else topBarProgress
                        val isScrolled by remember {
                            derivedStateOf { topBarProgress > 0.5f }
                        }
                        val backButtonSize by animateDpAsState(
                            targetValue = if (isScrolled) 36.dp else 40.dp,
                            animationSpec = if (reduceMotion) tween(120) else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                            label = "backButtonSize"
                        )
                        val backButtonRadius by animateDpAsState(
                            targetValue = if (isScrolled) 12.dp else 14.dp,
                            animationSpec = if (reduceMotion) tween(120) else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                            label = "backButtonRadius"
                        )
                        val backButtonIconSize by animateDpAsState(
                            targetValue = if (isScrolled) 18.dp else 20.dp,
                            animationSpec = if (reduceMotion) tween(120) else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                            label = "backButtonIconSize"
                        )
                        val backButtonOffsetY by animateDpAsState(
                            targetValue = if (isScrolled) 16.dp else 8.dp,
                            animationSpec = if (reduceMotion) tween(120) else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                            label = "backButtonOffsetY"
                        )
                        val bigLogoAlpha by remember {
                            derivedStateOf {
                                if (lazyListState.firstVisibleItemIndex >= 2) 0f
                                else if (totalScrollPx <= heroHeightPx) 1f
                                else (1f - (totalScrollPx - heroHeightPx) / with(density) { 50.dp.toPx() }).coerceIn(0f, 1f)
                            }
                        }
                        val isTabPinned by remember {
                            derivedStateOf {
                                lazyListState.firstVisibleItemIndex >= 2 ||
                                    (metaActionsHeightPx > 0f && totalScrollPx >= (heroHeightPx + metaActionsHeightPx - topBarHeightPx))
                            }
                        }

                        val canPlayAmbientTrailer = !hasTrailerFailed &&
                            isBackdropTrailerEnabled &&
                            !detail.trailerUrl.isNullOrBlank() &&
                            !isMetered &&
                            !isPowerSaver &&
                            isAtTop &&
                            !showTrailerDialog &&
                            !isNavigatingAway

                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(state = detailsHazeState)
                            ) {
                                // Persistent pinned backdrop hero container layered behind LazyColumn
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(heroHeight)
                                        .graphicsLayer {
                                            alpha = heroScrollAlpha
                                            val offset = if (lazyListState.firstVisibleItemIndex == 0) {
                                                lazyListState.firstVisibleItemScrollOffset.toFloat()
                                            } else {
                                                1000f
                                            }
                                            translationY = -offset * 0.4f
                                        }
                                ) {
                                    DetailsBackdropHero(
                                        backdropUrl = detail.backdropUrl ?: detail.posterUrl,
                                        logoUrl = detail.logoUrl,
                                        title = detail.title,
                                        trailerUrl = detail.trailerUrl,
                                        canPlayTrailer = canPlayAmbientTrailer,
                                        isMuted = isAmbientMuted,
                                        onPlaybackActive = { isTrailerPlaybackActive = it },
                                        onPlaybackError = {
                                            hasTrailerFailed = true
                                            isTrailerPlaybackActive = false
                                        },
                                        mediaId = detail.id,
                                        onHeroClick = null,
                                        hasFailed = hasTrailerFailed
                                    )
                                }

                                var expandedEpisodeId by remember(selectedSeason, safeChunkIndex) { mutableStateOf<String?>(null) }
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Transparent Hero Spacer (tapping toggles ambient trailer between video and poster; mute button overlay)
                                    item(key = "hero_spacer") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(heroHeight)
                                                .then(
                                                    if (!detail.trailerUrl.isNullOrBlank() && !hasTrailerFailed) {
                                                        Modifier.clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            isBackdropTrailerEnabled = !isBackdropTrailerEnabled
                                                        }
                                                    } else Modifier
                                                )
                                        ) {
                                            AnimatedVisibility(
                                                visible = !hasTrailerFailed && isBackdropTrailerEnabled && isTrailerPlaybackActive && isAtTop && heroScrollAlpha > 0.5f,
                                                enter = fadeIn(tween(300)),
                                                exit = fadeOut(tween(300)),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = Dimens.spacing16, bottom = Dimens.spacing16)
                                            ) {
                                                FrostedMuteButton(
                                                    isMuted = isAmbientMuted,
                                                    onToggle = { isAmbientMuted = !isAmbientMuted }
                                                )
                                            }
                                        }
                                    }

                                // 2. Metadata & Actions Block
                                item(key = "meta_actions") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                metaActionsHeightPx = coordinates.size.height.toFloat()
                                            }
                                            .background(screenBg)
                                            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing12)
                                    ) {
                                        DetailsTitleHeader(
                                            title = detail.title,
                                            logoUrl = detail.logoUrl,
                                            alpha = bigLogoAlpha
                                        )

                                        Spacer(modifier = Modifier.height(Dimens.spacing8))

                                        DetailsMetaBlock(
                                            detail = detail,
                                            hazeState = detailsHazeState
                                        )

                                        Spacer(modifier = Modifier.height(Dimens.spacing16))

                                        DetailsActionBlock(
                                            detail = detail,
                                            targetEpisode = targetEpisodeForPlay,
                                            latestPlayback = resumePlaybackForPlay,
                                            isInWatchlist = isWatchlist,
                                            watchlistStatus = watchlistStatus,
                                            isInPlaylist = isInPlaylist,
                                            downloads = downloads,
                                            onPlay = { handlePlayRequest(detail, targetEpisodeForPlay) },
                                            onWatchlist = { showWatchlistSheet = true },
                                            onDownload = {
                                                episodeToDownload = if (isTv) {
                                                    targetEpisodeForPlay ?: selectedSeason?.let { s ->
                                                        detail.episodes.firstOrNull { it.seasonNumber == s }
                                                    } ?: detail.episodes.firstOrNull() ?: EpisodeItem(
                                                        id = "${detail.id}:1:1",
                                                        title = "Episode 1",
                                                        seasonNumber = 1,
                                                        episodeNumber = 1,
                                                        data = "${detail.id}:1:1"
                                                    )
                                                } else null
                                                showDownloadSheet = true
                                            },
                                            onShare = {
                                                val shareYear = if (detail.year != null && detail.year != 0) " (${detail.year})" else ""
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    setType("text/plain")
                                                    putExtra(Intent.EXTRA_SUBJECT, detail.title)
                                                    putExtra(Intent.EXTRA_TEXT, "Watch ${detail.title}$shareYear on Euthopiar")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share ${detail.title}"))
                                            },
                                            onTrailer = if (!detail.trailerUrl.isNullOrBlank()) { { showTrailerDialog = true } } else null,
                                            onPlaylist = { showAddToPlaylistSheet = true },
                                            hazeState = detailsHazeState
                                        )

                                        Spacer(modifier = Modifier.height(Dimens.spacing16))

                                        ExpandableOverview(overview = detail.synopsis)
                                    }
                                }

                                // 3. Prime Video Style Tabs
                                item(key = "tabs_content") {
                                    Box(modifier = Modifier.background(screenBg)) {
                                        DetailsTabsContent(
                                            detail = detail,
                                            isTv = isTv,
                                            selectedSeason = selectedSeason ?: 1,
                                            castMembers = castMembers,
                                            episodePlaybackMap = episodePlaybackMap,
                                            downloads = downloads,
                                            onSelectSeason = { showSeasonSheet = true },
                                            onPlayEpisode = { ep -> handlePlayRequest(detail, ep) },
                                            onDownloadEpisode = { ep ->
                                                episodeToDownload = ep
                                                showDownloadSheet = true
                                            },
                                            onToggleWatched = { ep -> viewModel.toggleEpisodeWatched(detail, ep) },
                                            onNavigateToMedia = onNavigateToMedia?.let { nav -> { item -> safeNavigate { nav(item) } } },
                                            onAddToPlaylistEpisode = { selectedEpisodeForPlaylist = it },
                                            onAddToPlaylistMedia = { selectedRelatedMediaForPlaylist = it },
                                            selectedTabIndex = selectedTabIndex,
                                            onTabSelected = { selectedTabIndex = it },
                                            hazeState = detailsHazeState,
                                            isTabRowVisible = !isTabPinned
                                        )
                                    }
                                }

                                // 3b. Episodes: virtualized as real LazyColumn items so that series with
                                // hundreds/thousands of episodes only compose the visible rows.
                                if (isTv && selectedTabIndex == 0) {
                                    item(key = "episodes_header") {
                                        Box(modifier = Modifier.background(screenBg)) {
                                            EpisodesTabContent(
                                                selectedSeason = selectedSeason ?: 1,
                                                episodeCount = totalEpisodes,
                                                onSelectSeason = { showSeasonSheet = true },
                                                hazeState = detailsHazeState,
                                                chunkCount = chunkCount,
                                                selectedChunkIndex = safeChunkIndex,
                                                chunkSize = chunkSize,
                                                onSelectChunk = { idx ->
                                                    selectedChunkIndex = idx
                                                }
                                            )
                                        }
                                    }
                                    if (displayedEpisodes.isEmpty()) {
                                        item(key = "episodes_empty") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(screenBg)
                                                    .padding(vertical = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No episodes available for this season",
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color.White.copy(alpha = 0.50f)
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        itemsIndexed(
                                            items = displayedEpisodes,
                                            key = { index, ep -> "${ep.id}#${safeChunkIndex * chunkSize + index}" },
                                            contentType = { _, _ -> "episode_row" }
                                        ) { index, episode ->
                                            val playback = episodePlaybackMap[episode.seasonNumber to episode.episodeNumber]
                                            val progress = if (playback != null && playback.durationMs > 0) {
                                                (playback.currentPositionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
                                            } else 0f
                                            val isWatched = playback?.isCompleted == true || (playback != null && playback.durationMs > 0 && playback.currentPositionMs >= (playback.durationMs * 0.9f).toLong())
                                            val isExpanded = expandedEpisodeId == episode.id
                                            val epDownload = downloads.firstOrNull {
                                                (it.episodeId != null && it.episodeId == episode.id) ||
                                                (it.seasonNumber == episode.seasonNumber && it.episodeNumber == episode.episodeNumber &&
                                                    (it.mediaId == detail.id || it.title.contains(detail.title, ignoreCase = true)))
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(screenBg)
                                                    .padding(horizontal = 16.dp)
                                            ) {
                                                EpisodeRowItem(
                                                    episode = episode,
                                                    detail = detail,
                                                    progress = progress,
                                                    isWatched = isWatched,
                                                    isExpanded = isExpanded,
                                                    downloadItem = epDownload,
                                                    staggerIndex = index,
                                                    hazeState = detailsHazeState,
                                                    onRowClick = { expandedEpisodeId = if (isExpanded) null else episode.id },
                                                    onPlayClick = { handlePlayRequest(detail, episode) },
                                                    onDownloadClick = {
                                                        episodeToDownload = episode
                                                        showDownloadSheet = true
                                                    },
                                                    onToggleWatched = { viewModel.toggleEpisodeWatched(detail, episode) },
                                                    onAddToPlaylist = { selectedEpisodeForPlaylist = episode }
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(0.5.dp)
                                                        .background(Color.White.copy(alpha = 0.14f))
                                                )
                                            }
                                        }

                                        if (isChunked) {
                                            item(key = "episodes_chunk_pagination") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(screenBg)
                                                        .padding(horizontal = 16.dp)
                                                ) {
                                                    EpisodesPaginationButtons(
                                                        safeChunkIndex = safeChunkIndex,
                                                        chunkCount = chunkCount,
                                                        chunkSize = chunkSize,
                                                        totalEpisodes = totalEpisodes,
                                                        hazeState = detailsHazeState,
                                                        onSelectChunk = { selectedChunkIndex = it },
                                                        onScrolled = {
                                                            coroutineScope.launch {
                                                                lazyListState.animateScrollToItem(3)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom spacer
                                item(key = "bottom_spacer") {
                                    Spacer(modifier = Modifier.height(80.dp).background(screenBg))
                                }
                            }
                        }

                            // Pinned Tab Row (docked immediately under compact top bar when scrolled)
                            // Pinned Tab Row (docked immediately under compact top bar when scrolled)
                            if (isTabPinned) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = topBarHeight)
                                        .fillMaxWidth()
                                        .zIndex(14f)
                                        .background(screenBg)
                                ) {
                                    val tabTitles = remember(isTv) {
                                        if (isTv) listOf("Episodes", "Details & Cast", "More like this")
                                        else listOf("Details & Cast", "More like this")
                                    }
                                    AppTabRow(
                                        tabs = tabTitles,
                                        selectedTabIndex = selectedTabIndex,
                                        onTabSelected = { selectedTabIndex = it }
                                    )
                                }
                            }

                            // Compact Top Bar (Fix 1)
                            if (compactBarAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(topBarHeight)
                                        .zIndex(15f)
                                        .graphicsLayer {
                                            alpha = compactBarAlpha
                                        }
                                        .then(
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                Modifier.hazeEffect(state = detailsHazeState) {
                                                    blurRadius = GlassDefaults.BlurRadius
                                                    tints = listOf(
                                                        HazeTint(screenBg.copy(alpha = 0.88f)),
                                                        HazeTint(Color.White.copy(alpha = 0.05f))
                                                    )
                                                    noiseFactor = GlassDefaults.NoiseFactor
                                                    fallbackTint = HazeTint(GlassDefaults.NavbarFallbackColor)
                                                }
                                            } else {
                                                Modifier
                                                    .background(screenBg.copy(alpha = 0.88f))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            }
                                        )
                                        .drawBehind {
                                            // Solid dark obsidian fill to ensure 100% opacity over scrolled catalog items
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                drawRect(color = screenBg.copy(alpha = 0.88f))
                                                drawRect(color = Color.White.copy(alpha = 0.05f))
                                            }
                                            // Ambient vertical gradient light wash
                                            val topLightBrush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.02f),
                                                    Color.Transparent
                                                ),
                                                startY = 0f,
                                                endY = size.height * 0.45f
                                            )
                                            drawRect(brush = topLightBrush)

                                            // Hairline bottom border: specular gradient stroke matching FloatingNavbar
                                            drawLine(
                                                brush = GlassDefaults.BorderBrush,
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = HairlineWidth.toPx()
                                            )
                                        }
                                ) {
                                    // Aligned to bottom of bar (below status bar inset, height 68dp)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(68.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Reserved space for the single persistent back button (48dp touch target)
                                        Spacer(modifier = Modifier.width(48.dp))

                                        Spacer(modifier = Modifier.width(12.dp))

                                        val smallLogoAlpha = (1f - bigLogoAlpha).coerceIn(0f, 1f)

                                        // Title logo art scaled down to 28-30dp tall (fallback title 15sp medium wide letter spacing)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .graphicsLayer { alpha = smallLogoAlpha }
                                        ) {
                                            if (!detail.logoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(detail.logoUrl)
                                                        .crossfade(200)
                                                        .transformations(TrimTransparentTransformation())
                                                        .build(),
                                                    contentDescription = detail.title,
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .widthIn(max = 200.dp),
                                                    contentScale = ContentScale.Fit,
                                                    alignment = Alignment.CenterStart
                                                )
                                            } else {
                                                Text(
                                                    text = sanitizeTitle(detail.title),
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        letterSpacing = 1.2.sp,
                                                        color = Color.White
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (depth >= 3 && onCloseToRoot != null) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            // Reserved space for the single persistent close button (48dp touch target)
                                            Spacer(modifier = Modifier.width(48.dp))
                                        }
                                    }
                                }
                            }

                            // Single Unified Persistent Back Button (Seamless Docking into Top Bar)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .offset(y = backButtonOffsetY)
                                    .padding(start = 16.dp)
                                    .zIndex(25f)
                            ) {
                                MediaDetailsGlassIconButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    onClick = { safeNavigate(onBack) },
                                    size = backButtonSize,
                                    radius = backButtonRadius,
                                    iconSize = backButtonIconSize,
                                    hazeState = detailsHazeState,
                                    isOverArtwork = !isScrolled
                                )
                            }

                            // Single Unified Persistent Close to Tab Root Button (when depth >= 3)
                            if (depth >= 3 && onCloseToRoot != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .statusBarsPadding()
                                        .offset(y = backButtonOffsetY)
                                        .padding(end = 16.dp)
                                        .zIndex(25f)
                                ) {
                                    MediaDetailsGlassIconButton(
                                        icon = Icons.Filled.Close,
                                        contentDescription = "Close to tab root",
                                        onClick = { safeNavigate { onCloseToRoot() } },
                                        size = backButtonSize,
                                        radius = backButtonRadius,
                                        iconSize = backButtonIconSize,
                                        hazeState = detailsHazeState,
                                        isOverArtwork = !isScrolled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val successDetail = (uiState as? MediaDetailsUiState.Success)?.detail

        if (showNoPluginsDialog) {
            VelvetAlertDialog(
                onDismissRequest = { showNoPluginsDialog = false },
                title = "No Stream Providers Installed",
                message = "Euthopiar is a modular player and does not provide or host media streams. Please install a provider plugin or Stremio addon in the manager to stream content.",
                confirmText = "Open Manager",
                dismissText = "Cancel",
                hazeState = detailsHazeState,
                onConfirm = {
                    showNoPluginsDialog = false
                    onNavigateToPluginManager?.invoke()
                }
            )
        }

        // Season Selector Sheet (Frosted Glass, Fix 2)
        if (showSeasonSheet && successDetail != null) {
            val detail = successDetail
            val availableSeasons = remember(detail.episodes) {
                detail.episodes.map { it.seasonNumber }.distinct().sorted()
            }
            SeasonSheet(
                seriesTitle = detail.title,
                availableSeasons = availableSeasons,
                selectedSeason = selectedSeason ?: availableSeasons.firstOrNull() ?: 1,
                episodes = detail.episodes,
                onSelectSeason = { seasonNum ->
                    viewModel.selectSeason(seasonNum)
                },
                onDismiss = {
                    showSeasonSheet = false
                },
                hazeState = detailsHazeState
            )
        }

        // Download Quality Bottom Sheet
        if (showDownloadSheet && successDetail != null) {
            val detail = successDetail
            DownloadQualitySheet(
                detail = detail,
                episode = episodeToDownload,
                provider = provider,
                onDismiss = {
                    showDownloadSheet = false
                    episodeToDownload = null
                },
                onDownloadStarted = { msg -> snackbarController.show(msg) }
            )
        }

        // Watchlist Category Bottom Sheet
        if (showWatchlistSheet && successDetail != null) {
            val detail = successDetail
            val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME
            val currentEp = if (isTv) {
                selectedSeason?.let { s ->
                    detail.episodes.firstOrNull { it.seasonNumber == s }
                } ?: detail.episodes.firstOrNull()
            } else null

            MediaDetailsActionSheet(
                mediaId = detail.id,
                title = detail.title,
                posterUrl = detail.posterUrl ?: detail.backdropUrl,
                backdropUrl = detail.backdropUrl ?: detail.posterUrl,
                isTvSeries = isTv,
                year = detail.year,
                provider = provider,
                seasonNumber = currentEp?.seasonNumber,
                episodeNumber = currentEp?.episodeNumber,
                episodeTitle = currentEp?.title,
                episodeData = currentEp?.data,
                initialScreen = SheetScreen.WATCHLIST_CATEGORY,
                watchlistStatus = watchlistStatus,
                hazeState = detailsHazeState,
                onSelectWatchlistCategory = { statusKey, label ->
                    val isMove = !watchlistStatus.isNullOrBlank() && watchlistStatus != statusKey
                    val toastMsg = if (isMove) "Moved to $label" else "Added to $label"
                    viewModel.setWatchlistCategory(statusKey) { err -> snackbarController.show(err) }
                    snackbarController.show(toastMsg)
                },
                onRemoveFromWatchlist = {
                    val previousStatus = watchlistStatus ?: "TO_BE_WATCHED"
                    viewModel.setWatchlistCategory("REMOVE") { err -> snackbarController.show(err) }
                    snackbarController.show(
                        message = "Removed from watchlist",
                        actionLabel = "Undo",
                        durationMillis = 4000L
                    ) {
                        viewModel.setWatchlistCategory(previousStatus) { err -> snackbarController.show(err) }
                    }
                },
                onPlaylistAction = { msg -> snackbarController.show(msg) },
                onDismiss = { showWatchlistSheet = false }
            )
        }

        // Playlist Bottom Sheet
        if (showAddToPlaylistSheet && successDetail != null) {
            val detail = successDetail
            val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME
            val currentEp = if (isTv) {
                selectedSeason?.let { s ->
                    detail.episodes.firstOrNull { it.seasonNumber == s }
                } ?: detail.episodes.firstOrNull()
            } else null

            AddToPlaylistSheet(
                mediaId = detail.id,
                title = detail.title,
                posterUrl = detail.posterUrl ?: detail.backdropUrl,
                backdropUrl = detail.backdropUrl ?: detail.posterUrl,
                isTvSeries = isTv,
                year = detail.year,
                provider = provider,
                seasonNumber = currentEp?.seasonNumber,
                episodeNumber = currentEp?.episodeNumber,
                episodeTitle = currentEp?.title,
                episodeData = currentEp?.data,
                hazeState = detailsHazeState,
                onDismiss = { showAddToPlaylistSheet = false },
                onPlaylistAction = { msg -> snackbarController.show(msg) }
            )
        }

        if (selectedEpisodeForPlaylist != null && successDetail != null) {
            val ep = selectedEpisodeForPlaylist!!
            val detail = successDetail
            AddToPlaylistSheet(
                mediaId = detail.id,
                title = detail.title,
                posterUrl = detail.posterUrl ?: detail.backdropUrl,
                backdropUrl = detail.backdropUrl ?: detail.posterUrl,
                isTvSeries = true,
                year = detail.year,
                provider = provider,
                seasonNumber = ep.seasonNumber,
                episodeNumber = ep.episodeNumber,
                episodeTitle = ep.title,
                episodeData = ep.data ?: "${detail.id}:${ep.seasonNumber}:${ep.episodeNumber}",
                hazeState = detailsHazeState,
                onDismiss = { selectedEpisodeForPlaylist = null },
                onPlaylistAction = { msg -> snackbarController.show(msg) }
            )
        }

        if (selectedRelatedMediaForPlaylist != null) {
            val related = selectedRelatedMediaForPlaylist!!
            AddToPlaylistSheet(
                mediaId = related.id,
                title = related.title,
                posterUrl = related.posterUrl ?: related.backdropUrl,
                backdropUrl = related.backdropUrl ?: related.posterUrl,
                isTvSeries = related.type == MediaType.TV_SERIES,
                year = related.year,
                provider = related.provider,
                hazeState = detailsHazeState,
                onDismiss = { selectedRelatedMediaForPlaylist = null },
                onPlaylistAction = { msg -> snackbarController.show(msg) }
            )
        }

        // Full Trailer Interactive In-Window Overlay
        if (showTrailerDialog && successDetail != null) {
            val trailerDetail = successDetail
            val tUrl = trailerDetail.trailerUrl
            if (!tUrl.isNullOrBlank()) {
                val context = LocalContext.current
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                BackHandler { showTrailerDialog = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .zIndex(100f)
                ) {
                    // Centered 16:9 widescreen video player with letterboxing
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isLandscape) PaddingValues(0.dp) else PaddingValues(horizontal = Dimens.spacing8)),
                        contentAlignment = Alignment.Center
                    ) {
                        val playerModifier = if (isLandscape) {
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(16f / 9f)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(ShapeM)
                        }
                        TrailerBackdropPlayer(
                            trailerUrl = tUrl,
                            isInteractive = true,
                            modifier = playerModifier
                        )
                    }

                    // Top Bar with Media Title, Watch on YouTube, and Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(Dimens.spacing16)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trailerDetail.title,
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Dimens.spacing16)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(tUrl)
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.background(SurfaceMid.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Watch on YouTube",
                                    tint = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { showTrailerDialog = false },
                                modifier = Modifier.background(SurfaceMid.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close trailer",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun TrailerBackdropPlayer(
    trailerUrl: String,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false
) {
    val youtubeKey = remember(trailerUrl) {
        val clean = trailerUrl.trim()
        when {
            clean.contains("v=") -> clean.substringAfter("v=").substringBefore("&")
            clean.contains("youtu.be/") -> clean.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            clean.contains("embed/") -> clean.substringAfter("embed/").substringBefore("?").substringBefore("&")
            clean.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> clean
            else -> null
        }
    }

    if (youtubeKey != null) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        userAgentString = if (isInteractive) {
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        } else {
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                        }
                    }
                    webChromeClient = android.webkit.WebChromeClient()
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            return false
                        }
                    }
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    val muteParam = if (isInteractive) "0" else "1"
                    val controlsParam = if (isInteractive) "1" else "0"
                    val loopParam = if (isInteractive) "0" else "1"
                    val pointerEvents = if (isInteractive) "auto" else "none"

                    val playerCss = if (isInteractive) {
                        """
                        #player {
                            position: absolute;
                            top: 0;
                            left: 0;
                            width: 100%;
                            height: 100%;
                            border: 0;
                            pointer-events: auto;
                            opacity: 1;
                            background: transparent;
                        }
                        """.trimIndent()
                    } else {
                        """
                        #player {
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            width: 177.78vh;
                            height: 100vh;
                            min-width: 100vw;
                            min-height: 56.25vw;
                            transform: translate(-50%, -50%) scale(1.25);
                            border: 0;
                            pointer-events: none;
                            opacity: 0;
                            background: transparent;
                            transition: opacity 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
                        }
                        #player.active {
                            opacity: 1;
                        }
                        """.trimIndent()
                    }

                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <meta name="referrer" content="strict-origin-when-cross-origin">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            html, body {
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                                background: transparent;
                            }
                            .video-container {
                                position: absolute;
                                top: 0;
                                left: 0;
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                                background: transparent;
                                pointer-events: $pointerEvents;
                            }
                            $playerCss
                        </style>
                        </head>
                        <body>
                        <div class="video-container">
                            <iframe id="player"
                                    src="https://www.youtube-nocookie.com/embed/$youtubeKey?autoplay=1&mute=$muteParam&controls=$controlsParam&loop=$loopParam&playlist=$youtubeKey&playsinline=1&rel=0&modestbranding=1&iv_load_policy=3&disablekb=1&fs=0&enablejsapi=1&cc_load_policy=0&origin=https://www.themoviedb.org&widget_referrer=https://www.themoviedb.org"
                                    allow="autoplay; encrypted-media; picture-in-picture; accelerometer; clipboard-write; gyroscope; fullscreen"
                                    referrerpolicy="strict-origin-when-cross-origin"
                                    frameborder="0"
                                    onload="onIframeLoad()">
                            </iframe>
                        </div>
                        <script>
                            function revealPlayer() {
                                var p = document.getElementById('player');
                                if (p) p.classList.add('active');
                            }
                            function disableCaptions() {
                                var p = document.getElementById('player');
                                if (p && p.contentWindow) {
                                    p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'unloadModule', args: ['captions']}), '*');
                                    p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'unloadModule', args: ['cc']}), '*');
                                    p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'setOption', args: ['captions', 'track', {}]}), '*');
                                }
                            }
                            function onIframeLoad() {
                                var p = document.getElementById('player');
                                if (p && p.contentWindow) {
                                    p.contentWindow.postMessage(JSON.stringify({event: 'listening'}), '*');
                                    p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'playVideo', args: []}), '*');
                                    disableCaptions();
                                }
                            }
                            window.addEventListener('message', function(event) {
                                try {
                                    var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                                    var state = (data && data.info && data.info.playerState !== undefined) ? data.info.playerState : (data && data.event === 'onStateChange' ? data.info : null);
                                    if (state === 1) {
                                        revealPlayer();
                                        disableCaptions();
                                    }
                                } catch(e) {}
                            });
                        </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://www.themoviedb.org", html, "text/html", "UTF-8", null)
                }
            },
            onRelease = { webView ->
                try {
                    webView.onPause()
                    webView.stopLoading()
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                } catch (_: Exception) {}
            },
            modifier = modifier
        )
    }
}

/**
 * Small frosted glass mute/unmute button for the ambient backdrop trailer.
 */
@Composable
fun FrostedMuteButton(
    isMuted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
            .pressable(
                role = Role.Button,
                onClickLabel = if (isMuted) "Unmute trailer" else "Mute trailer",
                onClick = onToggle
            )
            .tvFocusable(shape = ShapeFull),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(ShapeFull)
                .background(Background.copy(alpha = 0.65f))
                .border(HairlineWidth, Color.White.copy(alpha = 0.25f), ShapeFull),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute trailer" else "Mute trailer",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Glass surface modifier matching Fix 3:
 * - Small controls: translucent fill (white ~7%), backdrop blur ~18px, 0.5px hairline border (white ~16%).
 * - Over hero artwork: backdrop blur 16-20px, black ~36% luminance anchor + white ~6%, 1dp hairline with vertical gradient (white 32% top, 10% 1/3, 6% 2/3, 16% bottom).
 * - On plain black page: very subtle faint surface (white ~7%) with 0.5px hairline.
 */
fun Modifier.mediaDetailsGlassSurface(
    shape: Shape = RoundedCornerShape(12.dp),
    hazeState: HazeState? = null,
    isOverArtwork: Boolean = false,
    alphaFill: Float = 0.07f
): Modifier {
    val hairlineGradient = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.32f),
        0.33f to Color.White.copy(alpha = 0.10f),
        0.66f to Color.White.copy(alpha = 0.06f),
        1.0f to Color.White.copy(alpha = 0.16f)
    )
    val standardHairline = Color.White.copy(alpha = 0.16f)

    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isOverArtwork && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = 18.dp
                    tints = listOf(
                        HazeTint(Color(0xFF0B0D13).copy(alpha = 0.36f)),
                        HazeTint(Color.White.copy(alpha = 0.06f))
                    )
                    fallbackTint = HazeTint(Color(0xFF0F101A).copy(alpha = 0.70f))
                }
            } else {
                Modifier.background(
                    if (isOverArtwork) Color(0xFF0B0D13).copy(alpha = 0.50f)
                    else Color.White.copy(alpha = alphaFill),
                    shape
                )
            }
        )
        .then(
            if (isOverArtwork) {
                Modifier.border(1.dp, hairlineGradient, shape)
            } else {
                Modifier.border(0.5.dp, standardHairline, shape)
            }
        )
}

/**
 * Monochromatic Glass Chip (Fix 4):
 * 24dp tall, 8dp radius (not fully rounded), 8dp horizontal padding, 12sp medium white text.
 * No yellow badge, no red tomato, no logos, no colors.
 */
@Composable
fun MediaDetailsGlassChip(
    text: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .mediaDetailsGlassSurface(
                shape = RoundedCornerShape(8.dp),
                hazeState = hazeState,
                alphaFill = 0.07f
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            maxLines = 1
        )
    }
}

/**
 * Glass Square Back / Close Button (Fix 1, Fix 3):
 * 36x36 dp (12 dp radius) when scrolled in top bar, or 40x40 dp (14 dp radius) when floating unscrolled.
 */
@Composable
fun MediaDetailsGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    radius: Dp = 12.dp,
    iconSize: Dp = 18.dp,
    hazeState: HazeState? = null,
    isOverArtwork: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "glassBtnScale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .mediaDetailsGlassSurface(
                    shape = RoundedCornerShape(radius),
                    hazeState = hazeState,
                    isOverArtwork = isOverArtwork
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * 16:9 Backdrop Hero with clean bottom dissolve and ambient trailer player.
 * Dark bottom fade and dark HUD scrim are drawn INSIDE the layer the glass blurs (Fix 3).
 */
@Composable
private fun DetailsBackdropHero(
    backdropUrl: String?,
    logoUrl: String?,
    title: String,
    trailerUrl: String? = null,
    canPlayTrailer: Boolean = false,
    isMuted: Boolean = true,
    onPlaybackActive: (Boolean) -> Unit = {},
    onPlaybackError: () -> Unit = {},
    mediaId: String? = null,
    onHeroClick: (() -> Unit)? = null,
    showMuteButton: Boolean = false,
    onToggleMute: () -> Unit = {},
    hasFailed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val heroHeight = (configuration.screenHeightDp * 0.38f).coerceIn(240f, 340f).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clipToBounds()
            .background(Color.Black)
            .then(
                if (onHeroClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onHeroClick() }
                } else Modifier
            )
    ) {
        // Static backdrop image (always present at base to ensure seamless backing)
        AppAsyncImage(
            model = backdropUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            titleForFallback = title,
            modifier = Modifier.fillMaxSize()
        )

        // Ambient Backdrop Trailer Player
        if (!trailerUrl.isNullOrBlank() && !hasFailed) {
            AmbientBackdropPlayer(
                trailerUrl = trailerUrl,
                canPlay = canPlayTrailer,
                isMuted = isMuted,
                onPlaybackActive = onPlaybackActive,
                onPlaybackError = onPlaybackError,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient dissolve into Black + top HUD scrim inside the blurred layer (Fix 3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.25f to Color.Transparent,
                            0.60f to Color.Black.copy(alpha = 0.65f),
                            1.0f to Color.Black
                        )
                    )
                )
        )

        // Frosted Mute Button overlay (if visible)
        AnimatedVisibility(
            visible = showMuteButton,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.spacing16, bottom = Dimens.spacing16)
        ) {
            FrostedMuteButton(
                isMuted = isMuted,
                onToggle = onToggleMute
            )
        }
    }
}

/**
 * Title Header: Clear PNG logo or styled heading title (medium weight, no bold).
 */
@Composable
private fun DetailsTitleHeader(
    title: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val context = LocalContext.current
    val cleanTitle = remember(title) { sanitizeTitle(title) }
    Box(
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(220)
                    .transformations(TrimTransparentTransformation())
                    .build(),
                contentDescription = cleanTitle,
                modifier = Modifier
                    .heightIn(min = 40.dp, max = 64.dp)
                    .widthIn(max = 260.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart
            )
        } else {
            Text(
                text = cleanTitle,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.semantics { heading() }
            )
        }
    }
}

/**
 * Metadata Block (Fix 4):
 * Line 1: Meta text items (year · seasons/duration · genres)
 * Line 2: One wrapping row directly under the metadata line and above the Play button,
 * containing four plain glass chips: content rating (TV-14), quality (4K), IMDb 7.0, RT 72%.
 * Chips: 24dp tall, 8dp radius, 8dp horizontal padding, 12sp medium white text, 6dp gaps.
 * No yellow badge, no red tomato, no logos, no colors. Hide any chip whose data is missing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsMetaBlock(
    detail: MediaDetail,
    hazeState: HazeState? = null
) {
    val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME
    val metaTextItems = remember(detail) {
        buildList {
            detail.year?.let { if (it > 0) add("$it") }
            val durationOrSeasons = if (isTv && detail.episodes.isNotEmpty()) {
                val totalSeasons = detail.episodes.map { it.seasonNumber }.distinct().size
                if (totalSeasons > 1) "$totalSeasons Seasons" else "1 Season"
            } else {
                detail.duration
            }
            durationOrSeasons?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (detail.genres.isNotEmpty()) {
                add(detail.genres.take(2).joinToString(", "))
            }
        }
    }

    val contentRating = detail.contentRating?.takeIf { it.isNotBlank() }
    val qualityLabel = when {
        detail.episodes.any { it.title.contains("4K", ignoreCase = true) } || detail.title.contains("4K", ignoreCase = true) -> "4K"
        detail.episodes.any { it.title.contains("1080", ignoreCase = true) } || detail.title.contains("1080", ignoreCase = true) -> "1080p"
        detail.episodes.isNotEmpty() || detail.type == MediaType.MOVIE -> "HD"
        else -> null
    }

    val imdbRating = detail.rating?.toDoubleOrNull()?.let { imdbVal ->
        if (imdbVal > 0.0) String.format(java.util.Locale.US, "%.1f", imdbVal) else null
    }
    val rtScore = detail.rottenTomatoesRating?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Line 1: Metadata line
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            metaTextItems.forEachIndexed { index, item ->
                if (index > 0) {
                    Text(
                        text = "·",
                        style = AppTypography.labelSmall,
                        color = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                Text(
                    text = item,
                    style = AppTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        // Line 2: One wrapping row directly under the metadata line and above the Play button
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            if (!contentRating.isNullOrBlank()) {
                MediaDetailsGlassChip(
                    text = contentRating,
                    hazeState = hazeState
                )
            }
            if (!qualityLabel.isNullOrBlank()) {
                MediaDetailsGlassChip(
                    text = qualityLabel,
                    hazeState = hazeState
                )
            }
            if (!imdbRating.isNullOrBlank()) {
                MediaDetailsGlassChip(
                    text = "IMDb $imdbRating",
                    hazeState = hazeState
                )
            }
            if (!rtScore.isNullOrBlank()) {
                val formattedRt = if (rtScore.endsWith("%")) rtScore else "$rtScore%"
                MediaDetailsGlassChip(
                    text = "RT $formattedRt",
                    hazeState = hazeState
                )
            }
        }
    }
}

/**
 * Action Block (Fix 4, Fix 5):
 * - Primary Play Pill (white).
 * - Thin progress bar if resume.
 * - Old separate rating row below Play button is REMOVED.
 * - Five equal columns. Each is a glass rounded square: 46x46dp, 15dp radius, outline icon 20dp in white.
 * - Label under it: 11sp, white 70%, regular weight. Full white at medium weight for active states (no bold).
 * - State indicators: small white dot on Playlist tile, checked bookmark on Watchlist tile.
 */
@Composable
private fun DetailsActionBlock(
    detail: MediaDetail,
    targetEpisode: EpisodeItem?,
    latestPlayback: PlaybackHistoryEntity?,
    isInWatchlist: Boolean,
    watchlistStatus: String? = null,
    isInPlaylist: Boolean = false,
    downloads: List<DownloadItem> = emptyList(),
    onPlay: () -> Unit,
    onWatchlist: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onTrailer: (() -> Unit)? = null,
    onPlaylist: (() -> Unit)? = null,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME || detail.episodes.isNotEmpty()
    val actionState = remember(latestPlayback, detail.type) {
        PlaybackTargetHelper.resolveActionState(latestPlayback, detail.type)
    }

    val playFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val windowDimensions = rememberWindowDimensions()

    LaunchedEffect(windowDimensions.isTV, detail.id) {
        if (windowDimensions.isTV) {
            kotlinx.coroutines.delay(120)
            try {
                playFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val mediaDownload = remember(downloads, detail.id, detail.title) {
        downloads.firstOrNull {
            (it.mediaId == detail.id || it.title.equals(detail.title, ignoreCase = true)) && it.episodeId == null
        }
    }

    val seriesDownloads = remember(downloads, detail.id, detail.title, detail.episodes) {
        if (!isTv) emptyList()
        else downloads.filter { item ->
            item.mediaId == detail.id ||
            item.title.contains(detail.title, ignoreCase = true) ||
            detail.episodes.any { ep -> ep.id == item.episodeId }
        }
    }

    val activeDownloadingEp = remember(seriesDownloads) {
        seriesDownloads.firstOrNull { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.QUEUED }
    }
    val activePausedEp = remember(seriesDownloads) {
        seriesDownloads.firstOrNull { it.state == DownloadState.PAUSED }
    }
    val activeFailedEp = remember(seriesDownloads) {
        seriesDownloads.firstOrNull { it.state == DownloadState.FAILED }
    }
    val targetEpDownload = remember(seriesDownloads, targetEpisode) {
        if (targetEpisode == null) null
        else seriesDownloads.firstOrNull {
            it.episodeId == targetEpisode.id ||
            (it.seasonNumber == targetEpisode.seasonNumber && it.episodeNumber == targetEpisode.episodeNumber)
        }
    }

    val (resolvedDownloadState, resolvedProgressFraction) = remember(
        isTv, mediaDownload, seriesDownloads, activeDownloadingEp, activePausedEp, activeFailedEp, targetEpDownload
    ) {
        if (isTv) {
            when {
                activeDownloadingEp != null -> {
                    val p = (activeDownloadingEp.progressPercentage / 100f).coerceIn(0f, 1f)
                    DownloadState.DOWNLOADING to p
                }
                activePausedEp != null -> {
                    val p = (activePausedEp.progressPercentage / 100f).coerceIn(0f, 1f)
                    DownloadState.PAUSED to p
                }
                targetEpDownload != null -> {
                    val p = (targetEpDownload.progressPercentage / 100f).coerceIn(0f, 1f)
                    targetEpDownload.state to p
                }
                activeFailedEp != null -> {
                    DownloadState.FAILED to 0f
                }
                seriesDownloads.isNotEmpty() && seriesDownloads.all { it.state == DownloadState.COMPLETED } -> {
                    DownloadState.COMPLETED to 1f
                }
                else -> null to 0f
            }
        } else {
            if (mediaDownload != null) {
                val p = (mediaDownload.progressPercentage / 100f).coerceIn(0f, 1f)
                mediaDownload.state to p
            } else {
                null to 0f
            }
        }
    }

    val isDownloading = resolvedDownloadState == DownloadState.DOWNLOADING || resolvedDownloadState == DownloadState.QUEUED
    val isPaused = resolvedDownloadState == DownloadState.PAUSED
    val isDownloaded = resolvedDownloadState == DownloadState.COMPLETED
    val isDownloadFailed = resolvedDownloadState == DownloadState.FAILED

    val downloadLabel = when {
        isDownloaded -> "Downloaded"
        isDownloading -> "Downloading"
        isPaused -> "Paused"
        isDownloadFailed -> "Retry"
        else -> "Download"
    }
    val downloadIcon = when {
        isDownloaded -> Icons.Outlined.Check
        isDownloading -> Icons.Filled.Pause
        isPaused -> Icons.Filled.PlayArrow
        isDownloadFailed -> Icons.Outlined.Refresh
        else -> Icons.Outlined.Download
    }

    val handleDownloadClick: () -> Unit = {
        val downloadRepo = DownloadRepository.getInstance(context)
        when {
            isDownloading -> {
                val activeItem = if (isTv) activeDownloadingEp else mediaDownload
                if (activeItem != null) downloadRepo.pauseDownload(activeItem.id)
            }
            isPaused -> {
                val pausedItem = if (isTv) (activePausedEp ?: targetEpDownload) else mediaDownload
                if (pausedItem != null) downloadRepo.resumeDownload(pausedItem.id)
            }
            isDownloadFailed -> {
                val failedItem = if (isTv) (activeFailedEp ?: targetEpDownload) else mediaDownload
                if (failedItem != null) downloadRepo.resumeDownload(failedItem.id) else onDownload()
            }
            isDownloaded -> {
                android.widget.Toast.makeText(context, "Media is already downloaded for offline playback", android.widget.Toast.LENGTH_SHORT).show()
            }
            else -> onDownload()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Primary Play Pill (White pill)
        PrimaryButton(
            text = actionState.label,
            icon = Icons.Filled.PlayArrow,
            onClick = onPlay,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(playFocusRequester)
        )

        if (actionState is PlaybackActionState.Resume) {
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressBarThin(
                    progress = actionState.progress,
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.85f),
                    trackColor = Color.White.copy(alpha = 0.14f)
                )
                Text(
                    text = "${actionState.minutesLeft} min left",
                    style = AppTypography.timeLabel.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val watchlistLabel = when {
            !isInWatchlist -> "Watchlist"
            watchlistStatus == "WATCHING" -> "Watching"
            watchlistStatus == "COMPLETED" -> "Completed"
            watchlistStatus == "TO_BE_WATCHED" -> "To Watch"
            else -> "Saved"
        }
        val watchlistIcon = if (isInWatchlist) Icons.Outlined.Check else Icons.Outlined.BookmarkBorder

        // Action Buttons Row: 5 equal columns, glass rounded squares (46x46dp, 15dp radius)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Watchlist Action
            ActionButtonItem(
                icon = watchlistIcon,
                label = watchlistLabel,
                onClick = onWatchlist,
                isActive = isInWatchlist,
                hazeState = hazeState,
                modifier = Modifier.weight(1f)
            )

            // Playlist Action
            if (onPlaylist != null) {
                ActionButtonItem(
                    icon = Icons.Outlined.PlaylistAdd,
                    label = "Playlist",
                    onClick = onPlaylist,
                    showDot = isInPlaylist,
                    isActive = isInPlaylist,
                    hazeState = hazeState,
                    modifier = Modifier.weight(1f)
                )
            }

            // Download Action
            ActionButtonItem(
                icon = downloadIcon,
                label = downloadLabel,
                onClick = handleDownloadClick,
                isActive = isDownloaded || isDownloading || isPaused,
                isDownloading = isDownloading,
                isPaused = isPaused,
                downloadProgress = resolvedProgressFraction,
                hazeState = hazeState,
                modifier = Modifier.weight(1f)
            )

            // Share Action
            ActionButtonItem(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = onShare,
                hazeState = hazeState,
                modifier = Modifier.weight(1f)
            )

            // Trailer Action
            if (onTrailer != null) {
                ActionButtonItem(
                    icon = Icons.Outlined.Movie,
                    label = "Trailer",
                    onClick = onTrailer,
                    hazeState = hazeState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isDownloading: Boolean = false,
    isPaused: Boolean = false,
    downloadProgress: Float = 0f,
    showDot: Boolean = false,
    hazeState: HazeState? = null
) {
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionBtnScale"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) tween(0) else tween(300),
        label = "actionDownloadProgress"
    )

    val tileShape = RoundedCornerShape(15.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    } catch (_: Throwable) {}
                    onClick()
                }
            )
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .mediaDetailsGlassSurface(
                    shape = tileShape,
                    hazeState = hazeState,
                    alphaFill = if (isActive) 0.14f else 0.07f
                )
                .tvFocusable(shape = tileShape),
            contentAlignment = Alignment.Center
        ) {
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(tileShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
            }

            if (isDownloading) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceAtLeast(0.04f) },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            } else if (isPaused) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceAtLeast(0.04f) },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            (scaleIn(tween(160), initialScale = 0.95f) + fadeIn(tween(160)))
                                .togetherWith(scaleOut(tween(140), targetScale = 0.95f) + fadeOut(tween(140)))
                        }
                    },
                    label = "actionIconAnim"
                ) { targetIcon ->
                    Icon(
                        imageVector = targetIcon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.70f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Expandable Synopsis Block (medium font weight, no bold).
 */
@Composable
private fun ExpandableOverview(overview: String?) {
    if (overview.isNullOrBlank()) return
    var expanded by remember(overview) { mutableStateOf(false) }
    var canExpand by remember(overview) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = overview,
            style = AppTypography.bodyLarge,
            color = Color.White.copy(alpha = 0.70f),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!expanded) {
                    canExpand = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 3
                }
            },
            modifier = Modifier.animateContentSize(tween(240, easing = FastOutSlowInEasing))
        )
        if (canExpand) {
            Spacer(modifier = Modifier.height(Dimens.spacing4))
            Text(
                text = if (expanded) "Less" else "More",
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .pressable { expanded = !expanded }
                    .padding(vertical = Dimens.spacing4)
            )
        }
    }
}

/**
 * Details Tabs: Episodes, Details & Cast, More Like This (Fix 7).
 * Transparent background on black page, 0.5px hairline underneath.
 */
@Composable
private fun DetailsTabsContent(
    detail: MediaDetail,
    isTv: Boolean,
    selectedSeason: Int,
    castMembers: List<CastMember>,
    episodePlaybackMap: Map<Pair<Int, Int>, PlaybackHistoryEntity>,
    downloads: List<DownloadItem> = emptyList(),
    onSelectSeason: () -> Unit,
    onPlayEpisode: (EpisodeItem) -> Unit,
    onDownloadEpisode: (EpisodeItem) -> Unit,
    onToggleWatched: (EpisodeItem) -> Unit,
    onNavigateToMedia: ((MediaItem) -> Unit)?,
    onAddToPlaylistEpisode: (EpisodeItem) -> Unit = {},
    onAddToPlaylistMedia: ((MediaItem) -> Unit)? = null,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState? = null,
    isTabRowVisible: Boolean = true,
    displayedEpisodes: List<EpisodeItem> = emptyList(),
    totalEpisodesCount: Int = 0,
    chunkCount: Int = 1,
    selectedChunkIndex: Int = 0,
    chunkSize: Int = 20,
    onSelectChunk: (Int) -> Unit = {},
    onScrollToEpisodesHeader: (() -> Unit)? = null,
    renderEpisodes: Boolean = false
) {
    val tabTitles = remember(isTv) {
        if (isTv) listOf("Episodes", "Details & Cast", "More like this")
        else listOf("Details & Cast", "More like this")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isTabRowVisible) {
            AppTabRow(
                tabs = tabTitles,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected
            )
        } else {
            // Keep identical height space when tab row is pinned outside to avoid jump
            Spacer(modifier = Modifier.height(44.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentTitle = tabTitles.getOrNull(selectedTabIndex) ?: ""
        when (currentTitle) {
            "Episodes" -> {
                // Rendered here for tablet two-column layout. On phone, LazyColumn virtualizes below tabs_content.
                if (renderEpisodes || displayedEpisodes.isNotEmpty() || totalEpisodesCount > 0) {
                    var expandedEpId by remember(selectedSeason, selectedChunkIndex) { mutableStateOf<String?>(null) }
                    EpisodesTabContent(
                        selectedSeason = selectedSeason,
                        episodeCount = totalEpisodesCount,
                        onSelectSeason = onSelectSeason,
                        hazeState = hazeState,
                        chunkCount = chunkCount,
                        selectedChunkIndex = selectedChunkIndex,
                        chunkSize = chunkSize,
                        onSelectChunk = onSelectChunk
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (displayedEpisodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes available for this season",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            )
                        }
                    } else {
                        displayedEpisodes.forEachIndexed { index, episode ->
                            val playback = episodePlaybackMap[episode.seasonNumber to episode.episodeNumber]
                            val progress = if (playback != null && playback.durationMs > 0) {
                                (playback.currentPositionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val isWatched = playback?.isCompleted == true || (playback != null && playback.durationMs > 0 && playback.currentPositionMs >= (playback.durationMs * 0.9f).toLong())
                            val isExpanded = expandedEpId == episode.id
                            val epDownload = downloads.firstOrNull {
                                (it.episodeId != null && it.episodeId == episode.id) ||
                                (it.seasonNumber == episode.seasonNumber && it.episodeNumber == episode.episodeNumber &&
                                    (it.mediaId == detail.id || it.title.contains(detail.title, ignoreCase = true)))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                EpisodeRowItem(
                                    episode = episode,
                                    detail = detail,
                                    progress = progress,
                                    isWatched = isWatched,
                                    isExpanded = isExpanded,
                                    downloadItem = epDownload,
                                    staggerIndex = index,
                                    hazeState = hazeState,
                                    onRowClick = {
                                        expandedEpId = if (isExpanded) null else episode.id
                                    },
                                    onPlayClick = { onPlayEpisode(episode) },
                                    onDownloadClick = { onDownloadEpisode(episode) },
                                    onToggleWatched = { onToggleWatched(episode) },
                                    onAddToPlaylist = { onAddToPlaylistEpisode(episode) }
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.14f))
                                )
                            }
                        }

                        if (chunkCount > 1) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EpisodesPaginationButtons(
                                    safeChunkIndex = selectedChunkIndex,
                                    chunkCount = chunkCount,
                                    chunkSize = chunkSize,
                                    totalEpisodes = totalEpisodesCount,
                                    hazeState = hazeState,
                                    onSelectChunk = onSelectChunk,
                                    onScrolled = {
                                        onScrollToEpisodesHeader?.invoke()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            "Details & Cast", "Details" -> {
                DetailsMetaTabContent(
                    detail = detail,
                    castMembers = castMembers
                )
            }
            "More like this" -> {
                RecommendationsTabContent(
                    recommendations = detail.recommendations,
                    onNavigateToMedia = onNavigateToMedia,
                    onAddToPlaylistMedia = onAddToPlaylistMedia
                )
            }
        }
    }
}

/**
 * Episodes Tab Content (Fix 6):
 * - Season dropdown: 34dp tall visually (48dp tap target), 12dp radius, 13sp text, outline chevron icon.
 * - Right side: episode count for selected season in 11sp muted.
 * - Hairline under season control row.
 * - Episode rows: 104x60dp thumbnail (10dp radius, 0.5px hairline border, 2dp progress bar, tiny outline check when watched).
 * - Title: 15sp, Medium weight (not bold), single line with ellipsis.
 * - Description: 12sp, white 62%, line height 1.4, clamped to 2 lines, smooth expand/collapse.
 * - Glass download tile (32x32dp, 11dp radius, 16dp icon) + 40dp overflow menu.
 * - Hairline between every row and after the last one. Vertical padding 12dp.
 */
@Composable
private fun EpisodesTabContent(
    selectedSeason: Int,
    episodeCount: Int,
    onSelectSeason: () -> Unit,
    hazeState: HazeState? = null,
    chunkCount: Int = 1,
    selectedChunkIndex: Int = 0,
    chunkSize: Int = 20,
    onSelectChunk: (Int) -> Unit = {}
) {
    val reduceMotion = LocalReduceMotion.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Season control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Glass dropdown
            val dropdownInteractionSource = remember { MutableInteractionSource() }
            val isDropdownPressed by dropdownInteractionSource.collectIsPressedAsState()
            val dropdownHaptic = LocalHapticFeedback.current
            val dropdownScale by animateFloatAsState(
                targetValue = if (isDropdownPressed && !reduceMotion) 0.96f else 1f,
                animationSpec = if (reduceMotion) snap() else spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dropdownScale"
            )

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .graphicsLayer {
                        scaleX = dropdownScale
                        scaleY = dropdownScale
                    }
                    .clickable(
                        interactionSource = dropdownInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Select season, currently Season $selectedSeason",
                        onClick = {
                            try {
                                dropdownHaptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            } catch (_: Throwable) {}
                            onSelectSeason()
                        }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .mediaDetailsGlassSurface(
                            shape = RoundedCornerShape(12.dp),
                            hazeState = hazeState,
                            alphaFill = 0.08f
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedSeason,
                            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                            label = "seasonLabelCrossfade"
                        ) { s ->
                            Text(
                                text = "Season $s",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Right: episode count for selected season in 11sp muted
            AnimatedContent(
                targetState = episodeCount,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                label = "epCountCrossfade"
            ) { count ->
                Text(
                    text = "$count ${if (count == 1) "episode" else "episodes"}",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                )
            }
        }

        // Frosted range pills for seasons with > 20 episodes
        if (chunkCount > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chunkCount) { chunkIdx ->
                    val startEp = chunkIdx * chunkSize + 1
                    val endEp = minOf((chunkIdx + 1) * chunkSize, episodeCount)
                    val isSelected = chunkIdx == selectedChunkIndex
                    val pillLabel = "Episodes $startEp–$endEp"

                    Box(
                        modifier = Modifier
                            .mediaDetailsGlassSurface(
                                shape = RoundedCornerShape(12.dp),
                                hazeState = hazeState,
                                alphaFill = if (isSelected) 0.22f else 0.08f
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.60f),
                                                Color.White.copy(alpha = 0.20f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .clickable {
                                onSelectChunk(chunkIdx)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pillLabel,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f)
                            )
                        )
                    }
                }
            }
        }

        // Hairline under season control row (0.5px, white 14%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
        )

        Spacer(modifier = Modifier.height(4.dp))

    }
}

@Composable
internal fun EpisodesPaginationButtons(
    safeChunkIndex: Int,
    chunkCount: Int,
    chunkSize: Int,
    totalEpisodes: Int,
    hazeState: HazeState? = null,
    onSelectChunk: (Int) -> Unit,
    onScrolled: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (chunkCount <= 1) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (safeChunkIndex > 0) {
            val prevStart = (safeChunkIndex - 1) * chunkSize + 1
            val prevEnd = safeChunkIndex * chunkSize
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .mediaDetailsGlassSurface(
                        shape = RoundedCornerShape(12.dp),
                        hazeState = hazeState,
                        alphaFill = 0.08f
                    )
                    .clickable {
                        onSelectChunk(safeChunkIndex - 1)
                        onScrolled()
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Previous: Episodes $prevStart–$prevEnd",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (safeChunkIndex < chunkCount - 1) {
            val nextStart = (safeChunkIndex + 1) * chunkSize + 1
            val nextEnd = minOf((safeChunkIndex + 2) * chunkSize, totalEpisodes)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .mediaDetailsGlassSurface(
                        shape = RoundedCornerShape(12.dp),
                        hazeState = hazeState,
                        alphaFill = 0.12f
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onSelectChunk(safeChunkIndex + 1)
                        onScrolled()
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Episodes $nextStart–$nextEnd",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: EpisodeItem,
    detail: MediaDetail,
    progress: Float,
    isWatched: Boolean,
    isExpanded: Boolean,
    downloadItem: DownloadItem? = null,
    staggerIndex: Int,
    hazeState: HazeState?,
    onRowClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowPressed by rowInteractionSource.collectIsPressedAsState()

    val rowScale by animateFloatAsState(
        targetValue = if (isRowPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(120),
        label = "epRowScale"
    )

    var showOptions by remember { mutableStateOf(false) }

    // Stagger animation for first 8 rows on enter
    val enterAnimProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(episode.id) {
        if (!reduceMotion && staggerIndex < 8) {
            delay(staggerIndex * 40L)
            enterAnimProgress.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        } else {
            enterAnimProgress.snapTo(1f)
        }
    }

    val staggerOffsetY = (1f - enterAnimProgress.value) * 10f
    val staggerAlpha = enterAnimProgress.value

    // 80ms delayed fade for expanded description
    val descFadeAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.85f,
        animationSpec = tween(
            durationMillis = if (isExpanded) 160 else 140,
            delayMillis = if (isExpanded && !reduceMotion) 80 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "descFadeAnim"
    )

    // Episode download state
    val epDownloadState = downloadItem?.state
    val epDownloadProgress = if (downloadItem != null && downloadItem.progressPercentage > 0f) {
        (downloadItem.progressPercentage / 100f).coerceIn(0f, 1f)
    } else 0f
    val isEpDownloading = epDownloadState == DownloadState.DOWNLOADING || epDownloadState == DownloadState.QUEUED
    val isEpPaused = epDownloadState == DownloadState.PAUSED
    val isEpDownloaded = epDownloadState == DownloadState.COMPLETED
    val isEpDownloadFailed = epDownloadState == DownloadState.FAILED

    val animatedEpProgress by animateFloatAsState(
        targetValue = epDownloadProgress,
        animationSpec = if (reduceMotion) tween(0) else tween(300),
        label = "epProgress"
    )

    val epDownloadIcon = when {
        isEpDownloaded -> Icons.Outlined.Check
        isEpDownloadFailed -> Icons.Outlined.Refresh
        else -> Icons.Outlined.Download
    }

    // Clean title to avoid "1. 1. Title" or duplicate numbering
    val rawTitle = episode.title.trim()
    val displayTitle = remember(episode.episodeNumber, rawTitle) {
        if (rawTitle.matches(Regex("""^(?:Episode\s+)?${episode.episodeNumber}[:.\s\-]+.*""", RegexOption.IGNORE_CASE))) {
            rawTitle
        } else if (rawTitle.isBlank()) {
            "Episode ${episode.episodeNumber}"
        } else {
            "${episode.episodeNumber}. $rawTitle"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
                translationY = staggerOffsetY
                alpha = staggerAlpha
            }
            .background(if (isRowPressed) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = onRowClick
            )
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail: about 104 by 60 dp, 10 dp radius, with a 0.5 px hairline border
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F101A))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Play episode ${episode.episodeNumber}",
                            onClick = onPlayClick
                        )
                ) {
                    AppAsyncImage(
                        model = episode.thumbnail ?: detail.backdropUrl ?: detail.posterUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        titleForFallback = "E${episode.episodeNumber}",
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2 dp white progress line runs along bottom edge
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(progress)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.90f))
                        )
                    }

                    // Watched indicator: tiny outline check on a glass chip at thumbnail corner
                    if (isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .mediaDetailsGlassSurface(
                                    shape = RoundedCornerShape(6.dp),
                                    hazeState = hazeState,
                                    alphaFill = 0.20f
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Watched",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayTitle,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isWatched) Color.White.copy(alpha = 0.70f) else Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val epDuration = episode.duration
                    if (!epDuration.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = epDuration,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = if (isWatched) 0.50f else 0.60f)
                            )
                        )
                    }

                    val epDescription = episode.description
                    if (!epDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = epDescription,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.8.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = (if (isWatched) 0.50f else 0.62f) * if (isExpanded) descFadeAlpha else 1f)
                            ),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(
                                    durationMillis = if (isExpanded) 240 else 180,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: glass download tile (32 by 32 dp, 11 dp radius, outline icon at 16 dp)
                val handleEpDownloadClick: () -> Unit = {
                    val downloadRepo = DownloadRepository.getInstance(context)
                    when {
                        isEpDownloading && downloadItem != null -> downloadRepo.pauseDownload(downloadItem.id)
                        isEpPaused && downloadItem != null -> downloadRepo.resumeDownload(downloadItem.id)
                        isEpDownloadFailed && downloadItem != null -> downloadRepo.resumeDownload(downloadItem.id)
                        isEpDownloaded -> {
                            android.widget.Toast.makeText(context, "Episode is downloaded", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        else -> onDownloadClick()
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = when {
                                isEpDownloaded -> "Episode downloaded"
                                isEpDownloading -> "Downloading episode ${(animatedEpProgress * 100).toInt()}%"
                                isEpPaused -> "Paused at ${(animatedEpProgress * 100).toInt()}%"
                                isEpDownloadFailed -> "Retry episode download"
                                else -> "Download episode"
                            },
                            onClick = handleEpDownloadClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .mediaDetailsGlassSurface(
                                shape = RoundedCornerShape(11.dp),
                                hazeState = hazeState,
                                alphaFill = if (isEpDownloaded) 0.16f else 0.08f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEpDownloading) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { animatedEpProgress.coerceAtLeast(0.04f) },
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 1.75.dp,
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.20f),
                                    strokeCap = StrokeCap.Round
                                )
                                Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                        } else if (isEpPaused) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { animatedEpProgress.coerceAtLeast(0.04f) },
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 1.75.dp,
                                    color = Color.White.copy(alpha = 0.50f),
                                    trackColor = Color.White.copy(alpha = 0.20f),
                                    strokeCap = StrokeCap.Round
                                )
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        } else {
                            AnimatedContent(
                                targetState = epDownloadIcon,
                                transitionSpec = {
                                    if (reduceMotion) {
                                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                    } else {
                                        (scaleIn(tween(160), initialScale = 0.7f) + fadeIn(tween(160)))
                                            .togetherWith(scaleOut(tween(140), targetScale = 0.7f) + fadeOut(tween(140)))
                                    }
                                },
                                label = "epDownloadIconAnim"
                            ) { targetIcon ->
                                Icon(
                                    imageVector = targetIcon,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = if (isEpDownloaded) 1f else 0.90f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Overflow menu icon as a small outline three-dot button with 40 dp tap area
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Episode options",
                            onClick = { showOptions = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Episode options",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(18.dp)
                    )

                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isWatched) "Mark as unwatched" else "Mark as watched",
                                    style = AppTypography.bodyMedium,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isWatched) Icons.Filled.CheckCircleOutline else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isWatched) Color.White.copy(alpha = 0.70f) else Color.White
                                )
                            },
                            onClick = {
                                showOptions = false
                                onToggleWatched()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Add to playlist…",
                                    style = AppTypography.bodyMedium,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                showOptions = false
                                onAddToPlaylist()
                            }
                        )
                    }
                }
            }

            // Play action button inside expanded area (Fix 6 requirement)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 116.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .mediaDetailsGlassSurface(
                                shape = RoundedCornerShape(10.dp),
                                hazeState = hazeState,
                                alphaFill = 0.14f
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = onPlayClick
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Play Episode",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsTabContent(
    recommendations: List<MediaItem>,
    onNavigateToMedia: ((MediaItem) -> Unit)?,
    onAddToPlaylistMedia: ((MediaItem) -> Unit)? = null
) {
    if (recommendations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacing32),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No related titles found",
                style = AppTypography.bodyMedium,
                color = TextTertiary
            )
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(Dimens.posterGap),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(recommendations, key = { index, item -> "${item.id}_$index" }) { _, item ->
            PosterCard(
                posterUrl = item.posterUrl ?: item.backdropUrl,
                title = item.title,
                onClick = { onNavigateToMedia?.invoke(item) },
                onLongClick = onAddToPlaylistMedia?.let { cb -> { cb(item) } }
            )
        }
    }
}

@Composable
private fun DetailsMetaTabContent(
    detail: MediaDetail,
    castMembers: List<CastMember>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacing16)
    ) {
        // Cast Gallery
        if (castMembers.isNotEmpty()) {
            Text(
                text = "Cast & Crew",
                style = AppTypography.titleMedium,
                color = TextPrimary,
                modifier = Modifier
                    .padding(bottom = Dimens.spacing8)
                    .semantics { heading() }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(castMembers, key = { index, it -> "${it.name}_${it.character}_$index" }) { _, cast ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        AppAsyncImage(
                            model = cast.profileUrl,
                            contentDescription = cast.name,
                            contentScale = ContentScale.Crop,
                            titleForFallback = cast.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacing4))
                        Text(
                            text = cast.name,
                            style = AppTypography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))
        }

        // Additional Metadata
        Text(
            text = "Information",
            style = AppTypography.titleMedium,
            color = TextPrimary,
            modifier = Modifier
                .padding(bottom = Dimens.spacing8)
                .semantics { heading() }
        )

        detail.genres.takeIf { it.isNotEmpty() }?.let { genres ->
            MetaRow(label = "Genres", value = genres.joinToString(", "))
        }

        detail.contentRating?.let { rating ->
            MetaRow(label = "Content Rating", value = rating)
        }

        MetaRow(label = "Source Provider", value = detail.provider ?: "Default")
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacing4),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = AppTypography.bodyMedium, color = TextTertiary)
        Text(text = value, style = AppTypography.bodyMedium, color = TextPrimary)
    }
}


private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
