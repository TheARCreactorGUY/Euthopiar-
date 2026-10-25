package com.euthopiar.ui.library.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
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
import com.euthopiar.download.DownloadItem
import com.euthopiar.download.DownloadState
import com.euthopiar.download.StorageStats
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.library.components.LibraryGlassIconTile
import com.euthopiar.ui.library.components.LibraryGlassPill
import com.euthopiar.ui.library.components.LibraryHairlineDivider
import com.euthopiar.ui.library.components.LibraryProgressBar
import com.euthopiar.ui.library.components.SelectionIndicator
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun DownloadsLedgerTab(
    downloads: List<DownloadItem>,
    storageStats: StorageStats?,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onStartSelect: (String) -> Unit,
    onPlayDownload: (DownloadItem) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onExploreClick: (() -> Unit)?,
    listState: LazyListState,
    onMediaClick: ((mediaId: String, isTv: Boolean, provider: String?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val reduceMotion = LocalReduceMotion.current

    val activeDownloads = remember(downloads) {
        downloads.filter {
            it.state == DownloadState.DOWNLOADING ||
            it.state == DownloadState.QUEUED ||
            it.state == DownloadState.PAUSED ||
            it.state == DownloadState.FAILED
        }
    }

    val readyDownloads = remember(downloads) {
        downloads.filter { it.state == DownloadState.COMPLETED }
    }

    // Storage computations
    val completedBytes = readyDownloads.sumOf { it.bytesDownloaded.coerceAtLeast(it.totalBytes) }
    val activeBytes = activeDownloads.sumOf { it.bytesDownloaded }
    val totalAppBytes = maxOf(completedBytes, storageStats?.appUsedBytes ?: 0L)
    val hasReliableStorage = storageStats != null && storageStats.totalBytes > 0L
    val freeBytes = if (hasReliableStorage) storageStats!!.freeBytes else 0L
    val totalDeviceBytes = if (hasReliableStorage) storageStats!!.totalBytes else 0L

    val downloadedFraction = if (totalDeviceBytes > 0L && completedBytes > 0L) {
        (completedBytes.toFloat() / totalDeviceBytes.toFloat()).coerceIn(0.005f, 1f)
    } else {
        0f
    }
    val inProgressFraction = if (totalDeviceBytes > 0L && activeBytes > 0L) {
        (activeBytes.toFloat() / totalDeviceBytes.toFloat()).coerceIn(0.005f, 1f)
    } else {
        0f
    }

    val appGb = totalAppBytes / (1024.0 * 1024.0 * 1024.0)
    val isUnderOneGb = appGb < 1.0 && totalAppBytes > 0
    val targetHeroNumber = if (isUnderOneGb) {
        (totalAppBytes / (1024.0 * 1024.0)).toFloat()
    } else {
        appGb.toFloat()
    }
    val heroUnit = if (isUnderOneGb) "MB" else "GB"

    val freeGb = (freeBytes / (1024.0 * 1024.0 * 1024.0)).toInt()
    val isStorageAlmostFull = freeBytes in 1..(2L * 1024 * 1024 * 1024)

    // Storage numeral count-up entrance animation (700ms ease-out)
    var hasAnimatedHero by remember { mutableStateOf(false) }
    val heroNumberAnim = remember { Animatable(if (reduceMotion) targetHeroNumber else 0f) }

    LaunchedEffect(targetHeroNumber, reduceMotion) {
        if (reduceMotion) {
            heroNumberAnim.snapTo(targetHeroNumber)
        } else if (!hasAnimatedHero) {
            hasAnimatedHero = true
            heroNumberAnim.animateTo(
                targetValue = targetHeroNumber,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                )
            )
        } else {
            heroNumberAnim.snapTo(targetHeroNumber)
        }
    }

    val readyMovies = remember(readyDownloads) {
        readyDownloads.filter { !it.isTvSeries && !it.mediaType.equals("tv", ignoreCase = true) }
    }

        val readyMovieGroups = remember(readyMovies) {
            readyMovies
                .groupBy { it.mediaId.ifBlank { it.title.substringBefore(": ") } }
                .map { (mediaId, dls) ->
                    val first = dls.first()
                    val titlePart = if (first.title.contains(": ")) first.title.substringBefore(": ") else first.title
                    val poster = dls.firstNotNullOfOrNull { it.posterUrl } ?: dls.firstNotNullOfOrNull { it.backdropUrl }
                    val totalBytes = dls.sumOf { it.bytesDownloaded.coerceAtLeast(it.totalBytes) }
                    MovieGroup(
                        mediaId = mediaId,
                        movieTitle = sanitizeTitle(titlePart),
                        posterUrl = poster,
                        downloads = dls,
                        totalBytes = totalBytes,
                        provider = first.provider
                    )
                }
        }
        val readyTvGroups = remember(readyDownloads) {
            readyDownloads
                .filter { it.isTvSeries || it.mediaType.equals("tv", ignoreCase = true) }
                .groupBy { it.mediaId.ifBlank { it.title.substringBefore(": ") } }
                .map { (mediaId, eps) ->
                    val first = eps.first()
                    val seriesTitle = sanitizeTitle(if (first.title.contains(": ")) first.title.substringBefore(": ") else first.title)
                    val poster = eps.firstNotNullOfOrNull { it.posterUrl } ?: eps.firstNotNullOfOrNull { it.backdropUrl }
                    val sortedEps = eps.sortedWith(compareBy({ it.seasonNumber ?: 1 }, { it.episodeNumber ?: 1 }))
                    val totalBytes = eps.sumOf { it.bytesDownloaded.coerceAtLeast(it.totalBytes) }
                    TvGroup(
                        mediaId = mediaId,
                        seriesTitle = seriesTitle,
                        posterUrl = poster,
                        episodes = sortedEps,
                        totalBytes = totalBytes,
                        provider = first.provider
                    )
                }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 110.dp),
            modifier = modifier.fillMaxSize()
        ) {
            // 1. Storage Hero Section
            item(key = "storage_hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "ON THIS DEVICE",
                        style = LibraryTypography.sectionLabel11
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 64sp medium numeral + 18sp "GB" 50% white on baseline
                    val displayedNum = if (isUnderOneGb) {
                        heroNumberAnim.value.toInt().toString()
                    } else {
                        String.format("%.1f", heroNumberAnim.value)
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayedNum,
                            style = LibraryTypography.storageHeroNumeral64.copy(fontFeatureSettings = "tnum")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = heroUnit,
                            style = LibraryTypography.storageHeroUnit18,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dual-Tone Storage Meter: completed downloads (white) vs active in-progress (crimson)
                    val animDownloaded by animateFloatAsState(
                        targetValue = downloadedFraction.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 300, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                        label = "animDownloaded"
                    )
                    val animInProgress by animateFloatAsState(
                        targetValue = inProgressFraction.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 300, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                        label = "animInProgress"
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LibraryDimens.progressBarHeight)
                            .clip(CircleShape)
                            .background(LibraryColors.ProgressTrack)
                    ) {
                        val totalWidth = maxWidth
                        val rawReadyWidth = if (animDownloaded > 0f) (totalWidth * animDownloaded).coerceAtLeast(2.dp) else 0.dp
                        val rawInProgressWidth = if (animInProgress > 0f) (totalWidth * animInProgress).coerceAtLeast(2.dp) else 0.dp
                        val hasBoth = rawReadyWidth > 0.dp && rawInProgressWidth > 0.dp
                        val maxUsableWidth = (totalWidth - (if (hasBoth) 2.dp else 0.dp)).coerceAtLeast(0.dp)
                        val readyWidth = rawReadyWidth.coerceAtMost(maxUsableWidth)
                        val inProgressWidth = rawInProgressWidth.coerceAtMost((maxUsableWidth - readyWidth).coerceAtLeast(0.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (readyWidth > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .width(readyWidth)
                                        .height(LibraryDimens.progressBarHeight)
                                        .clip(CircleShape)
                                        .background(LibraryColors.ProgressFill)
                                )
                            }
                            if (inProgressWidth > 0.dp) {
                                if (readyWidth > 0.dp) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(inProgressWidth)
                                        .height(LibraryDimens.progressBarHeight)
                                        .clip(CircleShape)
                                        .background(LibraryColors.Crimson)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle lines: left "{X} titles ready · {Y} downloading", right "{Z} GB free"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val readyCountText = "${readyDownloads.size} title${if (readyDownloads.size != 1) "s" else ""} ready"
                        val leftLabel = if (activeDownloads.isNotEmpty()) {
                            "$readyCountText · ${activeDownloads.size} downloading"
                        } else {
                            readyCountText
                        }
                        Text(
                            text = leftLabel,
                            style = LibraryTypography.metadata11
                        )

                        if (hasReliableStorage) {
                            Text(
                                text = "$freeGb GB free",
                                style = LibraryTypography.metadata11
                            )
                        }
                    }

                    // Calm glass storage warning if almost full
                    if (isStorageAlmostFull) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your device is almost full. Manage downloads to free up space.",
                                style = LibraryTypography.metadata11,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // 2. Downloading Section (only if any active/downloading/paused/failed)
            if (activeDownloads.isNotEmpty()) {
                item(key = "section_downloading_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LibraryHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Text(
                            text = "DOWNLOADING",
                            style = LibraryTypography.sectionLabel11,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                itemsIndexed(
                    items = activeDownloads,
                    key = { _, item -> "active_${item.id}" }
                ) { index, item ->
                    StaggeredDownloadingRow(
                        index = index,
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedIds,
                        onToggleSelect = { onToggleSelect(item.id) },
                        onLongClick = { onStartSelect(item.id) },
                        onRowClick = {
                            if (isSelectionMode) {
                                onToggleSelect(item.id)
                            } else {
                                onMediaClick?.invoke(item.mediaId, item.isTvSeries, item.provider)
                            }
                        },
                        onTogglePauseResume = {
                            if (item.state == DownloadState.PAUSED || item.state == DownloadState.FAILED) {
                                onResumeDownload(item.id)
                            } else {
                                onPauseDownload(item.id)
                            }
                        },
                        hazeState = hazeState
                    )
                }
            }

            // 3. Ready to watch section
            item(key = "section_ready_to_watch_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LibraryHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = "READY TO WATCH",
                        style = LibraryTypography.sectionLabel11,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
            }

            if (readyDownloads.isEmpty()) {
                item(key = "ready_empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp, horizontal = 24.dp),
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
                            text = "Nothing downloaded yet",
                            style = LibraryTypography.title15,
                            color = LibraryColors.TextWhite
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Saved titles show up here for offline viewing.",
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
                if (readyMovieGroups.isNotEmpty()) {
                    readyMovieGroups.forEach { group ->
                        item(key = "movie_group_${group.mediaId}") {
                            MovieHeaderRow(
                                group = group,
                                isSelectionMode = isSelectionMode,
                                allSelected = group.downloads.all { it.id in selectedIds },
                                onToggleGroupSelect = {
                                    val allSel = group.downloads.all { it.id in selectedIds }
                                    group.downloads.forEach { dl ->
                                        if (allSel) {
                                            if (dl.id in selectedIds) onToggleSelect(dl.id)
                                        } else {
                                            if (dl.id !in selectedIds) onToggleSelect(dl.id)
                                        }
                                    }
                                },
                                onMovieClick = {
                                    onMediaClick?.invoke(group.mediaId, false, group.provider)
                                }
                            )
                        }

                        itemsIndexed(
                            items = group.downloads,
                            key = { _, dl -> "movie_dl_${dl.id}" }
                        ) { dlIndex, dl ->
                            NestedMovieDownloadRow(
                                index = dlIndex,
                                item = dl,
                                isSelectionMode = isSelectionMode,
                                isSelected = dl.id in selectedIds,
                                onToggleSelect = { onToggleSelect(dl.id) },
                                onLongClick = { onStartSelect(dl.id) },
                                onPlay = { onPlayDownload(dl) }
                            )
                        }
                    }
                }

                if (readyTvGroups.isNotEmpty()) {
                    readyTvGroups.forEach { group ->
                        item(key = "tv_series_group_${group.mediaId}") {
                            TvSeriesHeaderRow(
                                group = group,
                                isSelectionMode = isSelectionMode,
                                allSelected = group.episodes.all { it.id in selectedIds },
                                onToggleGroupSelect = {
                                    val allSel = group.episodes.all { it.id in selectedIds }
                                    group.episodes.forEach { ep ->
                                        if (allSel) {
                                            if (ep.id in selectedIds) onToggleSelect(ep.id)
                                        } else {
                                            if (ep.id !in selectedIds) onToggleSelect(ep.id)
                                        }
                                    }
                                },
                                onSeriesClick = {
                                    onMediaClick?.invoke(group.mediaId, true, group.provider)
                                }
                            )
                        }

                        itemsIndexed(
                            items = group.episodes,
                            key = { _, ep -> "tv_ep_${ep.id}" }
                        ) { epIndex, ep ->
                            NestedEpisodeRow(
                                index = epIndex,
                                item = ep,
                                isSelectionMode = isSelectionMode,
                                isSelected = ep.id in selectedIds,
                                onToggleSelect = { onToggleSelect(ep.id) },
                                onLongClick = { onStartSelect(ep.id) },
                                onPlay = { onPlayDownload(ep) }
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Clean Downloading Row:
 * - Hairlines before and after
 * - Poster 40x56dp (10dp radius, 0.5px hairline border)
 * - Title 15sp Medium white
 * - Status 11sp muted 55% white (NO CRIMSON)
 * - 2px thin white progress bar
 * - Single glass pill action on right ("Pause" / "Resume" / "Retry")
 */
@Composable
private fun StaggeredDownloadingRow(
    index: Int,
    item: DownloadItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onRowClick: () -> Unit,
    onTogglePauseResume: () -> Unit,
    hazeState: HazeState? = null
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

    val actionLabel = when (item.state) {
        DownloadState.PAUSED -> "Resume"
        DownloadState.FAILED -> "Retry"
        else -> "Pause"
    }

    val statusText = when (item.state) {
        DownloadState.DOWNLOADING -> {
            val pct = item.progressPercentage.toInt()
            val quality = item.qualityLabel.substringBefore(" ").ifBlank { "1080p" }
            "Downloading · $pct% · $quality"
        }
        DownloadState.QUEUED -> {
            val pct = item.progressPercentage.toInt()
            if (pct > 0) "Queued · $pct%" else "Queued"
        }
        DownloadState.PAUSED -> {
            val pct = item.progressPercentage.toInt()
            val reason = item.failureReason?.takeIf { it.isNotBlank() } ?: "Paused"
            "$reason · $pct%"
        }
        DownloadState.FAILED -> "Failed · Tap to retry"
        else -> "Ready"
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pressable(
                        scaleDown = 0.98f,
                        onClick = onRowClick,
                        onLongClick = onLongClick
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    Row {
                        SelectionIndicator(isSelected = isSelected)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }

                // Poster thumbnail 40x56dp, 10dp radius, 0.5px hairline border
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

                // Middle: Title, status, and 2px progress bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = sanitizeTitle(item.title),
                        style = LibraryTypography.title15,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = statusText,
                        style = LibraryTypography.metadata11,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LibraryProgressBar(
                        progress = (item.progressPercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Single glass pill action sibling on right with guaranteed minimum touch area
            Box(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                LibraryGlassPill(
                    text = actionLabel,
                    onClick = onTogglePauseResume,
                    hazeState = hazeState
                )
            }
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

private data class MovieGroup(
    val mediaId: String,
    val movieTitle: String,
    val posterUrl: String?,
    val downloads: List<DownloadItem>,
    val totalBytes: Long,
    val provider: String?
)

private data class TvGroup(
    val mediaId: String,
    val seriesTitle: String,
    val posterUrl: String?,
    val episodes: List<DownloadItem>,
    val totalBytes: Long,
    val provider: String?
)

@Composable
private fun TvSeriesHeaderRow(
    group: TvGroup,
    isSelectionMode: Boolean,
    allSelected: Boolean,
    onToggleGroupSelect: () -> Unit,
    onSeriesClick: () -> Unit
) {
    val totalGb = group.totalBytes / (1024.0 * 1024.0 * 1024.0)
    val totalSizeText = if (totalGb < 1.0) {
        val mb = (group.totalBytes / (1024 * 1024)).coerceAtLeast(1)
        "$mb MB"
    } else {
        String.format("%.1f GB", totalGb)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(scaleDown = 0.98f, onClick = onSeriesClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                SelectionIndicator(isSelected = allSelected)
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Series 40x56dp Poster (10dp radius, hairline border)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(10.dp))
                    .background(LibraryColors.Surface)
            ) {
                if (!group.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = group.posterUrl,
                        contentDescription = group.seriesTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.seriesTitle,
                    style = LibraryTypography.title15,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val epCountText = "${group.episodes.size} episode${if (group.episodes.size != 1) "s" else ""}"
                Text(
                    text = "$epCountText · Total $totalSizeText",
                    style = LibraryTypography.metadata11
                )
            }
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

@Composable
private fun NestedEpisodeRow(
    index: Int,
    item: DownloadItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val cleanEpisodeTitle = remember(item) {
        val s = item.seasonNumber ?: 1
        val e = item.episodeNumber ?: 1
        val titlePart = if (item.title.contains(": ")) {
            item.title.substringAfter(": ")
        } else {
            item.subtitle ?: "Episode $e"
        }
        "S$s:E$e · ${sanitizeTitle(titlePart)}"
    }

    val bytes = item.bytesDownloaded.coerceAtLeast(item.totalBytes)
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    val isMb = gb < 1.0
    val sizeText = if (isMb) {
        val mb = (bytes / (1024 * 1024)).coerceAtLeast(1)
        "$mb MB"
    } else {
        String.format("%.1f GB", gb)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(
                    scaleDown = 0.98f,
                    onClick = {
                        if (isSelectionMode) onToggleSelect() else onPlay()
                    },
                    onLongClick = onLongClick
                )
                .padding(start = 36.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Row {
                    SelectionIndicator(isSelected = isSelected)
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }

            // 32dp solid white play circle with dark play icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF0B0B0F),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanEpisodeTitle,
                    style = LibraryTypography.title14,
                    color = LibraryColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val quality = item.qualityLabel.substringBefore(" ").ifBlank { "1080p" }
                Text(
                    text = "$quality · Ready offline",
                    style = LibraryTypography.metadata11
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = sizeText,
                style = LibraryTypography.metadata11
            )
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 76.dp, end = 16.dp)
        )
    }
}

@Composable
private fun MovieHeaderRow(
    group: MovieGroup,
    isSelectionMode: Boolean,
    allSelected: Boolean,
    onToggleGroupSelect: () -> Unit,
    onMovieClick: () -> Unit
) {
    val totalGb = group.totalBytes / (1024.0 * 1024.0 * 1024.0)
    val totalSizeText = if (totalGb < 1.0) {
        val mb = (group.totalBytes / (1024 * 1024)).coerceAtLeast(1)
        "$mb MB"
    } else {
        String.format("%.1f GB", totalGb)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(
                    scaleDown = 0.98f,
                    role = Role.Button,
                    onClick = {
                        if (isSelectionMode) onToggleGroupSelect() else onMovieClick()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                SelectionIndicator(isSelected = allSelected)
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Movie 40x56dp Poster (10dp radius, hairline border)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(10.dp))
                    .background(LibraryColors.Surface)
            ) {
                if (!group.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = group.posterUrl,
                        contentDescription = group.movieTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.movieTitle,
                    style = LibraryTypography.title15,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val dlCountText = "${group.downloads.size} file${if (group.downloads.size != 1) "s" else ""}"
                Text(
                    text = "$dlCountText · Total $totalSizeText",
                    style = LibraryTypography.metadata11
                )
            }
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

@Composable
private fun NestedMovieDownloadRow(
    index: Int,
    item: DownloadItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val bytes = item.bytesDownloaded.coerceAtLeast(item.totalBytes)
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    val isMb = gb < 1.0
    val sizeText = if (isMb) {
        val mb = (bytes / (1024 * 1024)).coerceAtLeast(1)
        "$mb MB"
    } else {
        String.format("%.1f GB", gb)
    }

    val quality = item.qualityLabel.substringBefore(" ").ifBlank { "1080p" }
    val displaySubtitle = buildString {
        append(quality)
        if (!item.provider.isNullOrBlank()) {
            append(" · ${item.provider}")
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(
                    scaleDown = 0.98f,
                    onClick = {
                        if (isSelectionMode) onToggleSelect() else onPlay()
                    },
                    onLongClick = onLongClick
                )
                .padding(start = 36.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Row {
                    SelectionIndicator(isSelected = isSelected)
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }

            // 32dp solid white play circle with dark play icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF0B0B0F),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sanitizeTitle(item.title),
                    style = LibraryTypography.title14,
                    color = LibraryColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$displaySubtitle · Ready offline",
                    style = LibraryTypography.metadata11
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = sizeText,
                style = LibraryTypography.metadata11
            )
        }

        LibraryHairlineDivider(
            modifier = Modifier.padding(start = 76.dp, end = 16.dp)
        )
    }
}
