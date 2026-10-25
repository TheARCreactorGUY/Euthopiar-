package com.euthopiar.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.repository.Playlist
import com.euthopiar.download.DownloadItem
import com.euthopiar.download.DownloadState
import com.euthopiar.download.StorageStats
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.components.EuthopiarDialogSingleItemRow
import com.euthopiar.ui.components.EuthopiarDialogMultiItemRow
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.components.VelvetAlertDialog
import com.euthopiar.ui.library.components.CreatePlaylistDialog
import com.euthopiar.ui.library.components.LibraryAmbientHazeBackground
import com.euthopiar.ui.library.components.LibraryGlassPill
import com.euthopiar.ui.library.components.LibraryHairlineDivider
import com.euthopiar.ui.library.components.libraryGlassSurface
import com.euthopiar.ui.library.tabs.DownloadsLedgerTab
import com.euthopiar.ui.library.tabs.PlaylistDetailView
import com.euthopiar.ui.library.tabs.PlaylistsTab
import com.euthopiar.ui.library.tabs.SavedWallTab
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onMediaClick: (mediaId: String, type: MediaType, provider: String?) -> Unit,
    onPlayMedia: (mediaId: String, title: String, epTitle: String?, season: Int?, episode: Int?, type: MediaType) -> Unit,
    onExploreClick: (() -> Unit)? = null,
    initialFilter: LibraryFilter? = null,
    viewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    LibraryScreen(
        onMediaClick = onMediaClick,
        onPlayMedia = { mId, title, epTitle, season, episode, type, prov, epData ->
            onPlayMedia(mId, title, epTitle, season, episode, type)
        },
        onPlayDownload = null,
        onExploreClick = onExploreClick,
        initialFilter = initialFilter,
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
fun LibraryScreen(
    onMediaClick: (mediaId: String, type: MediaType, provider: String?) -> Unit,
    onPlayMedia: (mediaId: String, title: String, epTitle: String?, season: Int?, episode: Int?, type: MediaType, provider: String?, episodeData: String?) -> Unit,
    onPlayDownload: ((DownloadItem) -> Unit)? = null,
    onExploreClick: (() -> Unit)? = null,
    initialFilter: LibraryFilter? = null,
    viewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val snackbarController = LocalSnackbarController.current
    val navbarVisible = com.euthopiar.ui.navigation.LocalNavbarVisible.current

    val hazeState = remember { HazeState() }

    // State collections
    val downloads by viewModel.downloads.collectAsState()
    val downloadsMap by viewModel.downloadsMap.collectAsState()
    val items by viewModel.items.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val historyMap by viewModel.historyMap.collectAsState()
    val continueWatchingList by viewModel.continueWatching.collectAsState(initial = emptyList())
    val isOfflineOnly by viewModel.isOfflineOnly.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val persistedLastTab by viewModel.libraryLastTab.collectAsState(initial = 1)

    // Hide floating navbar in selection mode to swap cleanly with action pill
    androidx.compose.runtime.DisposableEffect(isSelectionMode) {
        navbarVisible.value = !isSelectionMode
        onDispose {
            navbarVisible.value = true
        }
    }

    val storageStats by produceState<StorageStats?>(initialValue = null, downloads) {
        value = viewModel.getStorageStats()
    }

    // Determine initial tab from initialFilter if provided, else persisted last tab
    var initialTabApplied by rememberSaveable { mutableStateOf(false) }
    val initialPage = when (initialFilter) {
        LibraryFilter.DOWNLOADS -> 0
        LibraryFilter.HISTORY, LibraryFilter.WATCHING -> 1
        else -> 1
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })

    LaunchedEffect(persistedLastTab) {
        if (!initialTabApplied && initialFilter == null) {
            initialTabApplied = true
            if (persistedLastTab in 0..2 && pagerState.currentPage != persistedLastTab) {
                pagerState.scrollToPage(persistedLastTab)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.saveLastTab(pagerState.currentPage)
        if (isSelectionMode) {
            viewModel.exitSelectionMode()
        }
    }

    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            val target = when (initialFilter) {
                LibraryFilter.DOWNLOADS -> 0
                LibraryFilter.HISTORY, LibraryFilter.WATCHING -> 1
                else -> 1
            }
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }
    }

    // Preserve scroll positions per tab
    val downloadsListState = rememberLazyListState()
    val savedGridState = rememberLazyGridState()
    val playlistsGridState = rememberLazyGridState()

    // Navbar scroll observation
    when (pagerState.currentPage) {
        0 -> com.euthopiar.ui.navigation.ObserveScrollForNavbar(downloadsListState)
        1 -> com.euthopiar.ui.navigation.ObserveScrollForNavbar(savedGridState)
        2 -> com.euthopiar.ui.navigation.ObserveScrollForNavbar(playlistsGridState)
    }

    // Active download indicator (subtle 5dp white dot next to Downloads tab)
    val hasActiveDownloads = remember(downloads) {
        downloads.any { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.QUEUED }
    }

    // Playlist detail view state
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    // First in-progress item for Continue Card
    val continueWatchingItem = remember(continueWatchingList) {
        continueWatchingList.firstOrNull { !it.isCompleted && it.currentPositionMs > 1000L }
    }

    // Back handling
    BackHandler(enabled = isSelectionMode || selectedPlaylistForDetail != null) {
        if (selectedPlaylistForDetail != null) {
            selectedPlaylistForDetail = null
        } else if (isSelectionMode) {
            viewModel.exitSelectionMode()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LibraryAmbientHazeBackground(
            tabIndex = pagerState.currentPage,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize()
        ) {
        AnimatedContent(
            targetState = selectedPlaylistForDetail,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        animationSpec = tween(320, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(tween(260))).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(320, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                            targetOffsetX = { fullWidth -> -fullWidth / 4 }
                        ) + fadeOut(tween(200))
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(300, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                        initialOffsetX = { fullWidth -> -fullWidth / 4 }
                    ) + fadeIn(tween(240))).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                            targetOffsetX = { fullWidth -> fullWidth }
                        ) + fadeOut(tween(220))
                    )
                }
            },
            label = "playlistTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentPlaylist ->
            if (currentPlaylist != null) {
                // Full-screen Playlist Detail Screen
                PlaylistDetailView(
                    playlist = currentPlaylist,
                    historyMap = historyMap,
                    onBack = { selectedPlaylistForDetail = null },
                    onResumeAll = {
                        val firstUnfinished = currentPlaylist.items.firstOrNull { item ->
                            val h = historyMap[item.mediaId]
                            h == null || !h.isCompleted
                        } ?: currentPlaylist.items.firstOrNull()

                        if (firstUnfinished != null) {
                            val h = historyMap[firstUnfinished.mediaId]
                            val s = h?.seasonNumber ?: (if (firstUnfinished.isTvSeries) 1 else null)
                            val ep = h?.episodeNumber ?: (if (firstUnfinished.isTvSeries) 1 else null)
                            val epTitle = h?.episodeTitle ?: firstUnfinished.episodeTitle
                            val epData = h?.episodeData ?: firstUnfinished.episodeData ?: (if (firstUnfinished.isTvSeries) "${firstUnfinished.mediaId}:$s:$ep" else firstUnfinished.mediaId)
                            val type = if (firstUnfinished.isTvSeries) MediaType.TV_SERIES else MediaType.MOVIE

                            viewModel.updatePlaylistLastPlayed(currentPlaylist.id)
                            onPlayMedia(firstUnfinished.mediaId, firstUnfinished.title, epTitle, s, ep, type, firstUnfinished.provider, epData)
                        }
                    },
                    onDownloadAll = {
                        snackbarController.show("Downloading all available titles in ${currentPlaylist.title}")
                    },
                    onPlayItem = { item, history ->
                        val s = history?.seasonNumber ?: (if (item.isTvSeries) 1 else null)
                        val ep = history?.episodeNumber ?: (if (item.isTvSeries) 1 else null)
                        val epTitle = history?.episodeTitle ?: item.episodeTitle
                        val epData = history?.episodeData ?: item.episodeData ?: (if (item.isTvSeries) "${item.mediaId}:$s:$ep" else item.mediaId)
                        val type = if (item.isTvSeries) MediaType.TV_SERIES else MediaType.MOVIE

                        viewModel.updatePlaylistLastPlayed(currentPlaylist.id)
                        onPlayMedia(item.mediaId, item.title, epTitle, s, ep, type, item.provider, epData)
                    },
                    onDeletePlaylist = { id ->
                        viewModel.deletePlaylist(id)
                        selectedPlaylistForDetail = null
                    },
                    onRemoveItem = { itemKey ->
                        viewModel.removeItemFromPlaylist(currentPlaylist.id, itemKey)
                    },
                    hazeState = hazeState
                )
            } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Pinned Header: "Library" left, "Select" right (or "{N} selected" / "Done")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val isDownloadsTab = pagerState.currentPage == 0
                    val selectableIds = remember(isDownloadsTab, downloads, items, isOfflineOnly, downloadsMap) {
                        if (isDownloadsTab) {
                            downloads.map { it.id }.toSet()
                        } else if (isOfflineOnly) {
                            items.filter { item ->
                                val dl = downloadsMap[item.mediaId] ?: downloadsMap[item.id]
                                dl?.state == com.euthopiar.download.DownloadState.COMPLETED
                            }.map { it.id }.toSet()
                        } else {
                            items.map { it.id }.toSet()
                        }
                    }
                    val canSelect = (pagerState.currentPage == 0 || pagerState.currentPage == 1) && selectableIds.isNotEmpty()

                    Crossfade(targetState = isSelectionMode, label = "headerCrossfade") { selectionModeActive ->
                        if (!selectionModeActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Library",
                                    style = LibraryTypography.pageHeadline,
                                    modifier = Modifier.semantics { heading() }
                                )

                                // Select action pill (Downloads and Saved only)
                                if (canSelect) {
                                    LibraryGlassPill(
                                        text = "Select",
                                        onClick = {
                                            if (!reduceMotion) {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            viewModel.startSelectionMode()
                                        },
                                        hazeState = hazeState
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val count = selectedItemIds.size
                                val allSelected = selectableIds.isNotEmpty() && selectedItemIds.containsAll(selectableIds)

                                AnimatedContent(
                                    targetState = count,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                                                slideOutVertically { -it / 2 } + fadeOut()
                                            )
                                        } else {
                                            (slideInVertically { -it / 2 } + fadeIn()).togetherWith(
                                                slideOutVertically { it / 2 } + fadeOut()
                                            )
                                        }
                                    },
                                    label = "selectedCountAnim"
                                ) { currentCount ->
                                    Text(
                                        text = "$currentCount selected",
                                        style = LibraryTypography.pageHeadline.copy(fontSize = 22.sp),
                                        color = Color.White
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectableIds.isNotEmpty()) {
                                        LibraryGlassPill(
                                            text = if (allSelected) "Deselect all" else "Select all",
                                            onClick = {
                                                if (!reduceMotion) {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                                if (isDownloadsTab || isOfflineOnly) {
                                                    viewModel.selectAllIds(selectableIds)
                                                } else {
                                                    viewModel.selectAll(items)
                                                }
                                            },
                                            hazeState = hazeState
                                        )
                                    }

                                    LibraryGlassPill(
                                        text = "Done",
                                        onClick = { viewModel.exitSelectionMode() },
                                        hazeState = hazeState
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Tabs Row: Downloads / Saved / Playlists (14sp text with 1.5dp sliding white underline)
                val tabTitles = listOf("Downloads", "Saved", "Playlists")
                val tabCoordinates = remember { mutableStateMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>() }
                var tabContainerCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { tabContainerCoordinates = it }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                val isSelected = pagerState.currentPage == index
                                val pageOffset = (pagerState.currentPage + pagerState.currentPageOffsetFraction) - index
                                val selectionFraction = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                                val textColor = androidx.compose.ui.graphics.lerp(
                                    Color.White.copy(alpha = 0.50f),
                                    Color.White,
                                    selectionFraction
                                )
                                val countColor = androidx.compose.ui.graphics.lerp(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.70f),
                                    selectionFraction
                                )

                                Row(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            tabCoordinates[index] = coordinates
                                        }
                                        .pressable(
                                            scaleDown = 0.98f,
                                            role = Role.Tab,
                                            onClick = {
                                                if (!isSelected) {
                                                    if (!reduceMotion) {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(index)
                                                    }
                                                }
                                            }
                                        )
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = (if (isSelected) LibraryTypography.tab14Active else LibraryTypography.tab14Inactive).copy(color = textColor)
                                    )

                                    // Tab counts styled small (11 sp)
                                    val countForTab = when (title) {
                                        "Downloads" -> downloads.size
                                        "Saved" -> items.size
                                        "Playlists" -> playlists.size
                                        else -> 0
                                    }

                                    if (countForTab > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$countForTab",
                                            style = LibraryTypography.metadata11.copy(
                                                fontSize = 11.sp,
                                                color = countColor
                                            )
                                        )
                                    }

                                    // Downloads tab: subtle 5dp white dot if any active download
                                    if (title == "Downloads") {
                                        AnimatedVisibility(
                                            visible = hasActiveDownloads,
                                            enter = scaleIn() + fadeIn(),
                                            exit = scaleOut() + fadeOut()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.85f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Full-width 0.5px hairline divider under the entire tab row
                        LibraryHairlineDivider(modifier = Modifier.align(Alignment.BottomStart))

                        // Sliding & Resizing 1.5dp White Underline (GPU compositor graphicsLayer, zero layout reflow)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .height(LibraryDimens.underlineHeight)
                                .graphicsLayer {
                                    val continuousPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 2f)
                                    val baseIndex = continuousPosition.toInt().coerceIn(0, 2)
                                    val nextIndex = (baseIndex + 1).coerceIn(0, 2)
                                    val fraction = (continuousPosition - baseIndex).coerceIn(0f, 1f)

                                    val parent = tabContainerCoordinates
                                    val baseCoords = tabCoordinates[baseIndex]
                                    val nextCoords = tabCoordinates[nextIndex]

                                    if (parent != null && parent.isAttached && baseCoords != null && baseCoords.isAttached) {
                                        val basePos = parent.localPositionOf(baseCoords, androidx.compose.ui.geometry.Offset.Zero)
                                        val baseW = baseCoords.size.width.toFloat()

                                        val nextPos = if (nextCoords != null && nextCoords.isAttached) {
                                            parent.localPositionOf(nextCoords, androidx.compose.ui.geometry.Offset.Zero)
                                        } else {
                                            basePos
                                        }
                                        val nextW = if (nextCoords != null && nextCoords.isAttached) {
                                            nextCoords.size.width.toFloat()
                                        } else {
                                            baseW
                                        }

                                        val startX = androidx.compose.ui.util.lerp(basePos.x, nextPos.x, fraction)
                                        val targetW = androidx.compose.ui.util.lerp(baseW, nextW, fraction)
                                        translationX = startX
                                        scaleX = targetW / 1.dp.toPx()
                                        alpha = 1f
                                    } else {
                                        alpha = 0f
                                    }
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .width(1.dp)
                                .background(Color.White)
                        )
                    }
                }

                // 3. Horizontal Pager (Downloads, Saved, Playlists)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> DownloadsLedgerTab(
                            downloads = downloads,
                            storageStats = storageStats,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedItemIds,
                            onToggleSelect = { id ->
                                if (!reduceMotion) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleSelection(id)
                            },
                            onStartSelect = { id ->
                                if (!reduceMotion) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startSelectionMode(id)
                            },
                            onPlayDownload = { item ->
                                if (onPlayDownload != null) {
                                    onPlayDownload(item)
                                } else {
                                    val isTv = item.isTvSeries
                                    val epData = item.episodeId ?: item.id
                                    onPlayMedia(
                                        item.mediaId,
                                        item.title,
                                        item.subtitle,
                                        if (isTv) item.seasonNumber ?: 1 else null,
                                        if (isTv) item.episodeNumber ?: 1 else null,
                                        if (isTv) MediaType.TV_SERIES else MediaType.MOVIE,
                                        item.provider ?: "Offline",
                                        epData
                                    )
                                }
                            },
                            onPauseDownload = { viewModel.pauseDownload(it) },
                            onResumeDownload = { viewModel.resumeDownload(it) },
                            onExploreClick = onExploreClick,
                            listState = downloadsListState,
                            onMediaClick = { mediaId, isTv, provider ->
                                onMediaClick(mediaId, if (isTv) MediaType.TV_SERIES else MediaType.MOVIE, provider)
                            },
                            hazeState = hazeState
                        )

                        1 -> SavedWallTab(
                            items = items,
                            downloadsMap = downloadsMap,
                            continueWatchingItem = continueWatchingItem,
                            isOfflineOnly = isOfflineOnly,
                            onToggleOfflineOnly = { viewModel.toggleOfflineOnly() },
                            sortOrder = sortOrder,
                            onSelectSortOrder = { viewModel.setSortOrder(it) },
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedItemIds,
                            onToggleSelect = { id ->
                                if (!reduceMotion) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleSelection(id)
                            },
                            onStartSelect = { id ->
                                if (!reduceMotion) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startSelectionMode(id)
                            },
                            onItemClick = { item ->
                                onMediaClick(item.mediaId, item.mediaType, item.provider)
                            },
                            onPlayHistoryItem = { h ->
                                val isTv = h.seasonNumber != null || h.episodeNumber != null || h.mediaType?.contains("tv", ignoreCase = true) == true
                                val s = h.seasonNumber ?: (if (isTv) 1 else null)
                                val ep = h.episodeNumber ?: (if (isTv) 1 else null)
                                val epData = h.episodeData ?: (if (isTv) "${h.mediaId}:$s:$ep" else h.mediaId)
                                val type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE

                                onPlayMedia(h.mediaId, h.title, h.episodeTitle, s, ep, type, h.provider, epData)
                            },
                            onHistoryItemClick = { h ->
                                val isTv = h.seasonNumber != null || h.episodeNumber != null || h.mediaType?.contains("tv", ignoreCase = true) == true
                                val type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE
                                onMediaClick(h.mediaId, type, h.provider)
                            },
                            onExploreClick = onExploreClick,
                            gridState = savedGridState,
                            hazeState = hazeState
                        )

                        2 -> PlaylistsTab(
                            playlists = playlists,
                            onSelectPlaylist = { pl ->
                                selectedPlaylistForDetail = pl
                            },
                            onCreatePlaylistClick = {
                                showCreatePlaylistDialog = true
                            },
                            gridState = playlistsGridState,
                            onDeletePlaylist = { id ->
                                viewModel.deletePlaylist(id)
                            },
                            hazeState = hazeState
                        )
                    }
                }
            }
        }
    }

        // 4. Floating Glass Action Bar in Selection Mode (Replaces floating navbar)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(
                animationSpec = tween(220, easing = LinearOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(140, easing = FastOutLinearInEasing),
                targetOffsetY = { it }
            ) + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            val count = selectedItemIds.size
            val actionLabel = if (pagerState.currentPage == 0) {
                if (count == 0) "Delete downloads" else "Delete $count download${if (count != 1) "s" else ""}"
            } else {
                if (count == 0) "Remove from saved" else "Remove $count title${if (count != 1) "s" else ""}"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .libraryGlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        hazeState = hazeState,
                        isActive = true
                    )
                    .background(Color(0xFF14151F).copy(alpha = 0.94f))
                    .pressable(
                        scaleDown = 0.98f,
                        role = Role.Button,
                        enabled = count > 0,
                        onClick = {
                            if (count > 0) {
                                showConfirmDeleteDialog = true
                            }
                        }
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = if (count > 0) Color.White else Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = LibraryTypography.title14,
                        color = if (count > 0) Color.White else Color.White.copy(alpha = 0.40f)
                    )
                }
            }
        }
    }

    // 5. Destructive Action Confirmation Dialog
    if (showConfirmDeleteDialog) {
        val isDownloadsTab = pagerState.currentPage == 0
        val count = selectedItemIds.size
        val previewItems: List<Triple<String, String, String?>> = if (isDownloadsTab) {
            downloads.filter { it.id in selectedItemIds }.map { dl ->
                val meta = if (!dl.subtitle.isNullOrBlank()) {
                    dl.subtitle
                } else if (dl.seasonNumber != null && dl.episodeNumber != null) {
                    "Season ${dl.seasonNumber}, Episode ${dl.episodeNumber}"
                } else if (dl.isTvSeries) {
                    "TV Series"
                } else {
                    "Movie"
                }
                Triple(dl.title, meta, dl.posterUrl)
            }
        } else {
            items.filter { it.id in selectedItemIds }.map { item ->
                val meta = if (item.seasonNumber != null && item.episodeNumber != null) {
                    "Season ${item.seasonNumber}, Episode ${item.episodeNumber}"
                } else if (item.mediaType == MediaType.TV_SERIES) {
                    "TV Series"
                } else {
                    "Movie"
                }
                Triple(item.title, meta, item.posterUrl)
            }
        }

        EuthopiarGlassDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = if (isDownloadsTab) "Delete Selected Downloads?" else "Remove from Saved?",
            message = if (isDownloadsTab) {
                "Are you sure you want to permanently delete $count downloaded item${if (count != 1) "s" else ""} from local device storage?"
            } else {
                "Are you sure you want to remove $count selected title${if (count != 1) "s" else ""} from your saved library?"
            },
            confirmText = if (isDownloadsTab) "Delete" else "Remove",
            dismissText = "Cancel",
            icon = Icons.Outlined.DeleteOutline,
            isDestructive = true,
            hazeState = hazeState,
            itemPreview = if (previewItems.isNotEmpty()) {
                {
                    if (previewItems.size == 1) {
                        val (itemTitle, itemMeta, itemPoster) = previewItems.first()
                        EuthopiarDialogSingleItemRow(
                            title = itemTitle,
                            metadata = itemMeta,
                            posterUrl = itemPoster
                        )
                    } else {
                        val posters = previewItems.mapNotNull { it.third }
                        val label = "${previewItems.first().first}, +${previewItems.size - 1} more"
                        EuthopiarDialogMultiItemRow(
                            posterUrls = posters,
                            label = label
                        )
                    }
                }
            } else null,
            onConfirm = {
                showConfirmDeleteDialog = false
                if (isDownloadsTab) {
                    viewModel.deleteSelectedDownloads()
                    snackbarController.show("Deleted $count download${if (count != 1) "s" else ""}")
                } else {
                    val toRemove = items.filter { it.id in selectedItemIds }
                    viewModel.removeFromCurrentFilterWithUndo(toRemove) { undoAction ->
                        snackbarController.show(
                            message = "Removed $count title${if (count != 1) "s" else ""}",
                            actionLabel = "Undo",
                            durationMillis = 4500L
                        ) {
                            undoAction()
                        }
                    }
                    viewModel.exitSelectionMode()
                }
            },
            onDismiss = { showConfirmDeleteDialog = false }
        )
    }

    // 6. Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                snackbarController.show("Created playlist \"$name\"")
            },
            existingPlaylists = playlists,
            hazeState = hazeState
        )
    }
}
}
