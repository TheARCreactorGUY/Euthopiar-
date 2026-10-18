package com.euthopiar.ui.library.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.download.DownloadItem
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryItem
import com.euthopiar.ui.library.LibrarySortOrder
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.library.components.ContinueWatchingCard
import com.euthopiar.ui.library.components.LibraryGlassDropdown
import com.euthopiar.ui.library.components.LibraryGlassIconTile
import com.euthopiar.ui.library.components.LibraryGlassPill
import com.euthopiar.ui.library.components.LibraryGlassToggle
import com.euthopiar.ui.library.components.LibraryProgressBar
import com.euthopiar.ui.library.components.OfflinePosterBadge
import com.euthopiar.ui.library.components.SelectionIndicator
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun SavedWallTab(
    items: List<LibraryItem>,
    downloadsMap: Map<String, DownloadItem>,
    continueWatchingItem: PlaybackHistoryEntity?,
    isOfflineOnly: Boolean,
    onToggleOfflineOnly: () -> Unit,
    sortOrder: LibrarySortOrder,
    onSelectSortOrder: (LibrarySortOrder) -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onStartSelect: (String) -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    onPlayHistoryItem: (PlaybackHistoryEntity) -> Unit,
    onHistoryItemClick: ((PlaybackHistoryEntity) -> Unit)? = null,
    onExploreClick: (() -> Unit)?,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter by offline only if toggled
    val displayedItems = remember(items, isOfflineOnly, downloadsMap) {
        if (!isOfflineOnly) {
            items
        } else {
            items.filter { item ->
                val dl = downloadsMap[item.mediaId] ?: downloadsMap[item.id]
                dl?.state == com.euthopiar.download.DownloadState.COMPLETED
            }
        }
    }

    if (items.isEmpty()) {
        // Frosted Glass Typographic Empty State
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LibraryGlassIconTile(
                    icon = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    size = 48.dp,
                    radius = 16.dp,
                    iconSize = 24.dp,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nothing saved yet",
                    style = LibraryTypography.title15,
                    color = LibraryColors.TextWhite
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Titles you save show up here.",
                    style = LibraryTypography.metadata11,
                    color = LibraryColors.TextMuted55,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (onExploreClick != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    LibraryGlassPill(
                        text = "Browse titles",
                        onClick = onExploreClick,
                        hazeState = hazeState
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            modifier = modifier.fillMaxSize()
        ) {
            // 1. Filter Row: Sort dropdown left, Offline-only toggle right
            item(
                key = "filter_row",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Glass sort dropdown
                    LibraryGlassDropdown(
                        selectedLabel = sortOrder.label,
                        options = LibrarySortOrder.entries.map { it.label },
                        onSelect = { label ->
                            LibrarySortOrder.entries.firstOrNull { it.label == label }?.let(onSelectSortOrder)
                        },
                        isExpanded = showSortMenu,
                        onToggleExpand = { showSortMenu = !showSortMenu },
                        onDismiss = { showSortMenu = false },
                        hazeState = hazeState
                    )

                    // Right: Glass switch "Offline only"
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Switch,
                                onClick = onToggleOfflineOnly
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline only",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                color = if (isOfflineOnly) LibraryColors.TextWhite else LibraryColors.TextMuted55
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LibraryGlassToggle(
                            checked = isOfflineOnly,
                            onCheckedChange = { onToggleOfflineOnly() }
                        )
                    }
                }
            }

            // 2. Continue Watching Card
            if (continueWatchingItem != null && (!isOfflineOnly || downloadsMap[continueWatchingItem.mediaId]?.state == com.euthopiar.download.DownloadState.COMPLETED)) {
                item(
                    key = "continue_watching_card",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    val matchingDl = downloadsMap[continueWatchingItem.mediaId]
                    ContinueWatchingCard(
                        historyItem = continueWatchingItem,
                        downloadItem = matchingDl,
                        onPlay = { onPlayHistoryItem(continueWatchingItem) },
                        onCardClick = {
                            if (onHistoryItemClick != null) {
                                onHistoryItemClick(continueWatchingItem)
                            } else {
                                val matchingItem = items.firstOrNull { it.mediaId == continueWatchingItem.mediaId }
                                if (matchingItem != null) {
                                    onItemClick(matchingItem)
                                } else {
                                    onPlayHistoryItem(continueWatchingItem)
                                }
                            }
                        }
                    )
                }
            }

            // 3. Offline-only empty state
            if (isOfflineOnly && displayedItems.isEmpty()) {
                item(
                    key = "empty_offline_state",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 54.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LibraryGlassIconTile(
                            icon = Icons.Outlined.Download,
                            contentDescription = null,
                            size = 48.dp,
                            radius = 16.dp,
                            iconSize = 24.dp,
                            hazeState = hazeState
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No saved titles are downloaded",
                            style = LibraryTypography.title15,
                            color = LibraryColors.TextWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Turn off Offline only to see everything.",
                            style = LibraryTypography.metadata11,
                            color = LibraryColors.TextMuted55,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 4. Poster Grid Items
            itemsIndexed(
                items = displayedItems,
                key = { _, item -> item.id }
            ) { index, item ->
                val matchingDownload = downloadsMap[item.mediaId] ?: downloadsMap[item.id]
                StaggeredPosterItem(
                    index = index,
                    item = item,
                    downloadItem = matchingDownload,
                    isSelectionMode = isSelectionMode,
                    isSelected = item.id in selectedIds,
                    onToggleSelect = { onToggleSelect(item.id) },
                    onStartSelect = { onStartSelect(item.id) },
                    onItemClick = { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Editorial poster item for the Saved Wall grid:
 * - 2:3 poster with 10dp radius and 0.5px hairline border
 * - Offline badge at top-right
 * - Multi-select indicator at top-left
 * - Title 14sp Medium under poster
 * - Sub-line 11.5sp muted 55% white
 * - Thin 2px white progress bar if watch progress exists
 * - Staggered entrance rise + fade
 */
@Composable
private fun StaggeredPosterItem(
    index: Int,
    item: LibraryItem,
    downloadItem: DownloadItem?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onStartSelect: () -> Unit,
    onItemClick: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val animProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion && index < 8) {
            delay((index * 35L).coerceAtMost(300L))
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val itemAlpha = animProgress.value
    val itemTranslationY = (1f - animProgress.value) * 12.dp.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemTranslationY
            }
            .pressable(
                scaleDown = 0.98f,
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onItemClick()
                },
                onLongClick = {
                    if (!isSelectionMode) onStartSelect() else onToggleSelect()
                }
            )
    ) {
        // 2:3 Poster Box with 0.5px hairline border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(LibraryDimens.posterCornerRadius))
                .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(LibraryDimens.posterCornerRadius))
                .background(LibraryColors.Surface)
        ) {
            val posterUrl = item.posterUrl ?: item.backdropUrl
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top-right: Offline status badge (8dp inset)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                OfflinePosterBadge(downloadItem = downloadItem)
            }

            // Top-left: Selection circle (in select mode)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    SelectionIndicator(isSelected = isSelected)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title: 14sp Medium
        Text(
            text = com.euthopiar.ui.library.sanitizeTitle(item.title),
            style = LibraryTypography.title14,
            color = LibraryColors.TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Sub-line: 11.5sp muted 55% white
        val subline = buildString {
            if (item.mediaType == MediaType.TV_SERIES) {
                val s = item.seasonNumber ?: 1
                val e = item.episodeNumber ?: 1
                append("S$s:E$e")
                if (item.progress > 0f) {
                    val pct = (item.progress * 100).toInt()
                    append(" · $pct%")
                }
            } else {
                append("Movie")
                if (item.year != null) {
                    append(" · ${item.year}")
                }
            }
            if (downloadItem != null && downloadItem.state == com.euthopiar.download.DownloadState.COMPLETED) {
                val gb = downloadItem.bytesDownloaded / (1024.0 * 1024.0 * 1024.0)
                if (gb >= 1.0) {
                    append(" · ${String.format("%.1f GB", gb)}")
                } else if (downloadItem.bytesDownloaded > 0) {
                    append(" · ${downloadItem.bytesDownloaded / (1024 * 1024)} MB")
                }
            }
        }

        Text(
            text = subline,
            style = LibraryTypography.metadata11,
            color = LibraryColors.TextMuted55,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Thin 2px white progress bar under the metadata (empty for unwatched)
        Spacer(modifier = Modifier.height(4.dp))
        LibraryProgressBar(
            progress = item.progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
