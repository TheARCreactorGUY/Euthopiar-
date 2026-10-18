package com.euthopiar.ui.library.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.data.repository.Playlist
import com.euthopiar.data.repository.PlaylistItem
import com.euthopiar.ui.components.VelvetAlertDialog
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.library.components.LibraryGlassBackButton
import com.euthopiar.ui.library.components.LibraryGlassOverflowButton
import com.euthopiar.ui.library.components.LibraryGlassPill
import com.euthopiar.ui.library.components.LibraryHairlineDivider
import com.euthopiar.ui.library.components.LibraryProgressBar
import com.euthopiar.ui.library.components.PlaylistMosaic
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    historyMap: Map<String, PlaybackHistoryEntity>,
    onBack: () -> Unit,
    onResumeAll: () -> Unit,
    onDownloadAll: () -> Unit,
    onPlayItem: (PlaylistItem, PlaybackHistoryEntity?) -> Unit,
    onDeletePlaylist: ((String) -> Unit)? = null,
    onRemoveItem: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    BackHandler(onBack = onBack)

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<PlaylistItem?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        VelvetAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Delete playlist?",
            message = "Are you sure you want to delete \"${com.euthopiar.ui.library.sanitizeTitle(playlist.title)}\"? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDeletePlaylist?.invoke(playlist.id)
                onBack()
            }
        )
    }

    if (itemToRemove != null) {
        val targetItem = itemToRemove!!
        VelvetAlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = "Remove from playlist?",
            message = "Remove \"${com.euthopiar.ui.library.sanitizeTitle(targetItem.title)}\" from this playlist?",
            confirmText = "Remove",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                val key = targetItem.uniqueKey
                itemToRemove = null
                onRemoveItem?.invoke(key)
            }
        )
    }

    val posters = remember(playlist.items) {
        playlist.items.mapNotNull { it.backdropUrl ?: it.posterUrl }.take(4)
    }

    // Calculate total remaining time across the playlist's current resume points
    val totalRemainingMinutes = remember(playlist.items, historyMap) {
        var totalMinutes = 0
        for (item in playlist.items) {
            val history = historyMap[item.mediaId]
            if (history != null) {
                if (!history.isCompleted) {
                    val defaultDurationMs = if (item.isTvSeries) 50L * 60000L else ((item.durationMinutes?.toLong() ?: 110L) * 60000L)
                    val effectiveDurationMs = if (history.durationMs > 0L) history.durationMs else defaultDurationMs
                    val remainingMs = (effectiveDurationMs - history.currentPositionMs).coerceAtLeast(0L)
                    val minutes = (remainingMs / 60000).toInt()
                    totalMinutes += minutes
                }
            } else {
                val fullMinutes = if (item.isTvSeries) 50 else (item.durationMinutes ?: 110)
                totalMinutes += fullMinutes
            }
        }
        totalMinutes
    }

    val formattedRemainingTime = remember(totalRemainingMinutes) {
        if (totalRemainingMinutes >= 60) {
            val h = totalRemainingMinutes / 60
            val m = totalRemainingMinutes % 60
            if (m > 0) "${h}h ${m}m left" else "${h}h left"
        } else {
            "${totalRemainingMinutes}m left"
        }
    }

    val isAmoled = com.euthopiar.ui.theme.LocalAmoledMode.current
    val pageBg = if (isAmoled) Color.Black else LibraryColors.Background

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. Full-width Hero Collage header (~150dp tall, dimmed ~75%, soft bottom fade)
        item(key = "playlist_hero_mosaic") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
            ) {
                PlaylistMosaic(
                    posters = posters,
                    cornerRadius = 0.dp,
                    overlayAlpha = 0.75f,
                    modifier = Modifier.fillMaxSize()
                )

                // Soft opacity fade to background at bottom edge (no colored gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    pageBg
                                )
                            )
                        )
                )

                // Top navigation bar: back button left, overflow button right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryGlassBackButton(
                        onClick = onBack,
                        hazeState = hazeState
                    )

                    if (!playlist.isSystem && onDeletePlaylist != null) {
                        Box {
                            LibraryGlassOverflowButton(
                                onClick = { showOverflowMenu = true },
                                hazeState = hazeState
                            )

                            val menuBg = if (isAmoled) Color.Black.copy(alpha = 0.88f) else Color(0xFF0F101B).copy(alpha = 0.72f)
                            val borderBrush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(menuBg)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(LibraryDimens.hairline, borderBrush, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete playlist",
                                            style = LibraryTypography.metadata11.copy(
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.70f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Headline Block & Action Row
        item(key = "playlist_caption") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Title: 34sp medium tight line height
                Text(
                    text = com.euthopiar.ui.library.sanitizeTitle(playlist.title),
                    style = LibraryTypography.pageHeadline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Meta line: "{N} titles · {X}h {Y}m left"
                val countText = "${playlist.items.size} title${if (playlist.items.size != 1) "s" else ""}"
                Text(
                    text = "$countText · $formattedRemainingTime",
                    style = LibraryTypography.metadata11
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Row: Solid white pill "Resume all", Glass pill "Download all"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryGlassPill(
                        text = "Resume all",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = onResumeAll,
                        hazeState = hazeState
                    )

                    LibraryGlassPill(
                        text = "Download all",
                        icon = Icons.Outlined.Download,
                        isPrimary = false,
                        onClick = onDownloadAll,
                        hazeState = hazeState
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                LibraryHairlineDivider()
            }
        }

        // 3. Continuity Rows
        itemsIndexed(
            items = playlist.items,
            key = { _, item -> "detail_item_${item.uniqueKey}" }
        ) { index, item ->
            val history = historyMap[item.mediaId]
            StaggeredPlaylistRow(
                index = index,
                item = item,
                history = history,
                onClick = { onPlayItem(item, history) },
                onLongClick = if (!playlist.isSystem && onRemoveItem != null) {
                    { itemToRemove = item }
                } else null
            )
        }
    }
}

/**
 * Editorial playlist row:
 * - 48dp+ touch target (64dp height)
 * - 0.5dp hairline divider
 * - Thumbnail: 40x56dp, 10dp radius, hairline border
 * - Title: 15sp Medium white
 * - Subline: 11sp muted 55% white (NO YELLOW/AMBER)
 * - 2px thin white progress bar under title when has progress
 * - Right side: remaining time ("14m left" or "52m" or "Finished") in 11.5sp muted
 */
@Composable
private fun StaggeredPlaylistRow(
    index: Int,
    item: PlaylistItem,
    history: PlaybackHistoryEntity?,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
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

    val defaultDurationMs = if (item.isTvSeries) 50L * 60000L else ((item.durationMinutes?.toLong() ?: 110L) * 60000L)
    val effectiveDurationMs = if (history != null && history.durationMs > 0L) history.durationMs else defaultDurationMs

    val progress = if (history != null && effectiveDurationMs > 0L) {
        (history.currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val hasProgress = history != null && !history.isCompleted && (history.currentPositionMs > 5000L || progress > 0.02f)

    // Remaining time computation
    val remainingString = if (history != null && !history.isCompleted) {
        val remMs = (effectiveDurationMs - history.currentPositionMs).coerceAtLeast(0L)
        val remM = (remMs / 60000).toInt().coerceAtLeast(1)
        if (hasProgress) "${remM}m left" else "${remM}m"
    } else if (history?.isCompleted == true) {
        "Finished"
    } else {
        val fullM = if (item.isTvSeries) 50 else (item.durationMinutes ?: 110)
        "${fullM}m"
    }

    val sublineText = when {
        hasProgress && item.isTvSeries -> {
            val s = history?.seasonNumber ?: 1
            val e = history?.episodeNumber ?: 1
            "Continue · S$s E$e"
        }
        hasProgress -> "Continue"
        history?.isCompleted == true -> "Completed"
        else -> "Not started"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = (1f - animProgress.value) * 12.dp.value
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .pressable(
                    scaleDown = 0.98f,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail: 40x56dp, 10dp radius, hairline border
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(10.dp))
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
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title and subline
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = com.euthopiar.ui.library.sanitizeTitle(item.title),
                    style = LibraryTypography.title15,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = sublineText,
                    style = LibraryTypography.metadata11,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))
                LibraryProgressBar(
                    progress = if (hasProgress) progress else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Remaining time in muted metadata
            Text(
                text = remainingString,
                style = LibraryTypography.metadata11
            )
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}
