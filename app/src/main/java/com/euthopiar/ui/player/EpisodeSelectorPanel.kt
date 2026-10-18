package com.euthopiar.ui.player

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import com.euthopiar.ui.theme.LocalReduceMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.outlined.Close
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.components.Dot
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.components.tvFocusable

/**
 * Top HUD Trigger Pill for Series Playback ("S1 · E3 ▾")
 * Styled with Video-Overlay Glass and standardized 98% press scale.
 */
@Composable
fun EpisodeSelectorPill(
    seasonNumber: Int?,
    episodeNumber: Int?,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = seasonNumber ?: 1
    val e = episodeNumber ?: 1

    val reduceMotion = LocalReduceMotion.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(PlayerTokens.DockChevronRotateMs, easing = PlayerTokens.EaseOut),
        label = "chevronRotation"
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .videoOverlayGlass(PlayerTokens.RadiusPill)
            .tvFocusable(shape = PlayerTokens.RadiusPill)
            .playerPressable(
                role = Role.Button,
                onClickLabel = if (isOpen) "Close Episode Selector" else "Open Episode Selector",
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "S$s · E$e",
                color = Color.White,
                style = PlayerTokens.PillText
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isOpen) "Close Episode Selector" else "Open Episode Selector",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}

/**
 * Top HUD Playback Speed Pill ("1x")
 * Styled with Video-Overlay Glass, 4dp solid white dot for active state, and no red accent.
 */
@Composable
fun TopBarSpeedPill(
    playbackSpeed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedLabel = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x"
    val isCustomSpeed = playbackSpeed != 1.0f

    Box(
        modifier = modifier
            .height(36.dp)
            .videoOverlayGlass(PlayerTokens.RadiusPill)
            .tvFocusable(shape = PlayerTokens.RadiusPill)
            .playerPressable(
                role = Role.Button,
                onClickLabel = "Playback Speed: $speedLabel",
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCustomSpeed) {
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeActiveDot)
                        .clip(PlayerTokens.RadiusCircle)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = speedLabel,
                color = Color.White,
                style = PlayerTokens.PillText
            )
        }
    }
}

/**
 * Dropdown Season and Episode Selector Panel styled with Sheet Glass and 16:9 thumbnails.
 */
@Composable
fun EpisodeSelectorPanel(
    isOpen: Boolean,
    currentSeason: Int?,
    currentEpisode: Int?,
    selectedSeasonInPanel: Int,
    availableSeasons: List<Int>,
    episodes: List<EpisodeItem>,
    playbackMap: Map<Pair<Int, Int>, PlaybackHistoryEntity>,
    isLoading: Boolean,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeSelect: (EpisodeItem) -> Unit,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(15f)
    ) {
        // Video dimming scrim (30% black per PlayerTokens.SidePanelScrim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerTokens.SidePanelScrim)
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismiss()
                    }
                }
        )

        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(PlayerTokens.SidePanelEnterMs, easing = PlayerTokens.EaseOut)) +
                    scaleIn(
                        initialScale = 0.98f,
                        animationSpec = tween(PlayerTokens.SidePanelEnterMs, easing = PlayerTokens.EaseOut)
                    ),
            exit = fadeOut(animationSpec = tween(PlayerTokens.SidePanelExitMs, easing = PlayerTokens.EaseIn)) +
                    scaleOut(
                        targetScale = 0.98f,
                        animationSpec = tween(PlayerTokens.SidePanelExitMs, easing = PlayerTokens.EaseIn)
                    ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 58.dp, end = 24.dp)
        ) {
            val maxHeight = (configuration.screenHeightDp * 0.65f).dp

            Box(
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            Modifier.width(400.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        }
                    )
                    .heightIn(max = maxHeight)
                    .sheetGlass(PlayerTokens.RadiusSheet)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onInteraction()
                        }
                    }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top: Season Selector Bar with Search-Style Glass Dropdown
                    val seasonsList = if (availableSeasons.isNotEmpty()) availableSeasons else listOf(1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (seasonsList.size <= 1) {
                            Text(
                                text = "Season ${seasonsList.firstOrNull() ?: 1}",
                                color = Color.White,
                                style = PlayerTokens.SheetHeadline.copy(fontSize = 18.sp, lineHeight = 22.sp)
                            )
                        } else {
                            SeasonGlassDropdown(
                                seasons = seasonsList,
                                selectedSeason = selectedSeasonInPanel,
                                onSeasonSelect = onSeasonSelect,
                                onInteraction = onInteraction,
                                modifier = Modifier.widthIn(min = 140.dp, max = 220.dp)
                            )
                        }

                        // Close button on top-right of panel
                        Box(
                            modifier = Modifier
                                .size(PlayerTokens.TouchTargetMin)
                                .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                                .tvFocusable(shape = PlayerTokens.RadiusCircle)
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = "Close Episode Selector",
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Episodes Content with cross-fade animation
                    AnimatedContent(
                        targetState = selectedSeasonInPanel,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "episodesListCrossfade",
                        modifier = Modifier.weight(1f, fill = false)
                    ) { targetSeason ->
                        val seasonEpisodes = remember(episodes, targetSeason) {
                            episodes.filter { it.seasonNumber == targetSeason }
                        }

                        if (isLoading && seasonEpisodes.isEmpty()) {
                            EpisodeListSkeleton()
                        } else if (seasonEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No episodes available",
                                    color = Color.White.copy(alpha = 0.50f),
                                    style = PlayerTokens.SheetSubtitle
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()

                            LaunchedEffect(targetSeason, currentSeason, currentEpisode) {
                                val activeIndex = seasonEpisodes.indexOfFirst {
                                    it.seasonNumber == (currentSeason ?: 1) && it.episodeNumber == (currentEpisode ?: 1)
                                }
                                if (activeIndex >= 0) {
                                    listState.scrollToItem(activeIndex.coerceAtLeast(0))
                                }
                            }

                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = seasonEpisodes,
                                    key = { it.id.ifBlank { "${it.seasonNumber}_${it.episodeNumber}" } }
                                ) { episodeItem ->
                                    val isNowPlaying = (currentSeason ?: 1) == episodeItem.seasonNumber &&
                                            (currentEpisode ?: 1) == episodeItem.episodeNumber

                                    val historyEntry = playbackMap[episodeItem.seasonNumber to episodeItem.episodeNumber]
                                    val isCompleted = historyEntry?.isCompleted == true
                                    val watchProgress = if (historyEntry != null && historyEntry.durationMs > 0L) {
                                        (historyEntry.currentPositionMs.toFloat() / historyEntry.durationMs.toFloat()).coerceIn(0f, 1f)
                                    } else 0f

                                    EpisodeSelectorRow(
                                        episode = episodeItem,
                                        isNowPlaying = isNowPlaying,
                                        isCompleted = isCompleted,
                                        watchProgress = watchProgress,
                                        onClick = {
                                            if (isNowPlaying) {
                                                onDismiss()
                                            } else {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onEpisodeSelect(episodeItem)
                                            }
                                        },
                                        onScrollInteraction = onInteraction
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

/**
 * 74dp tall episode row with 16:9 thumbnail, progress, and metadata.
 * Restyled with Sheet Glass, solid white check circle for completed, and no red accent.
 */
@Composable
private fun EpisodeSelectorRow(
    episode: EpisodeItem,
    isNowPlaying: Boolean,
    isCompleted: Boolean,
    watchProgress: Float,
    onClick: () -> Unit,
    onScrollInteraction: () -> Unit
) {
    val rowBg = if (isNowPlaying) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 74.dp)
            .clip(PlayerTokens.RadiusSmall)
            .background(rowBg)
            .tvFocusable(shape = PlayerTokens.RadiusSmall)
            .playerPressable(
                role = Role.Button,
                onClickLabel = episode.title.ifBlank { "Episode ${episode.episodeNumber}" }
            ) {
                onScrollInteraction()
                onClick()
            }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 16:9 Thumbnail (120dp wide, RadiusSmall corners)
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(67.5.dp)
                .clip(PlayerTokens.RadiusSmall)
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = if (isNowPlaying) 1.5.dp else HairlineWidth,
                    color = if (isNowPlaying) Color.White else PlayerTokens.StandardGlassBorder,
                    shape = PlayerTokens.RadiusSmall
                )
        ) {
            AppAsyncImage(
                model = episode.thumbnail,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                titleForFallback = "E${episode.episodeNumber}",
                modifier = Modifier.fillMaxSize()
            )

            // Completed badge: 20dp solid white circle with dark outline check
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(PlayerTokens.SizeCheckCircle)
                        .clip(PlayerTokens.RadiusCircle)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Watched",
                        tint = PlayerTokens.LabelDark,
                        modifier = Modifier.size(PlayerTokens.IconCheck)
                    )
                }
            }

            // Partially watched: white progress bar along bottom of thumbnail (no red Accent)
            if (!isCompleted && watchProgress > 0.03f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(watchProgress)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Metadata Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = episode.title.ifBlank { "Episode ${episode.episodeNumber}" },
                color = Color.White,
                style = PlayerTokens.RowTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (isNowPlaying) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(PlayerTokens.SizeActiveDot)
                            .clip(PlayerTokens.RadiusCircle)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOW PLAYING",
                        color = Color.White,
                        style = PlayerTokens.SectionLabel.copy(color = Color.White)
                    )
                }
            } else {
                val dur = episode.duration?.ifBlank { null } ?: "45m"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "E${episode.episodeNumber}",
                        color = Color.White.copy(alpha = 0.70f),
                        style = PlayerTokens.RowMeta
                    )
                    Dot()
                    Text(
                        text = dur,
                        color = Color.White.copy(alpha = 0.70f),
                        style = PlayerTokens.RowMeta
                    )
                }
            }

            val epDesc = episode.description
            if (!epDesc.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = epDesc.trim(),
                    color = Color.White.copy(alpha = 0.45f),
                    style = PlayerTokens.RowMeta.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Shimmer skeleton loading state for episode rows
 */
@Composable
private fun EpisodeListSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(PlayerTokens.RadiusSmall)
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(60.dp)
                        .clip(PlayerTokens.RadiusSmall)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(PlayerTokens.RadiusSmall)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(11.dp)
                            .clip(PlayerTokens.RadiusSmall)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}

/**
 * Frosted Glass Season Dropdown matching Search Page filter dropdown style.
 */
@Composable
private fun SeasonGlassDropdown(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelect: (Int) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(180, easing = PlayerTokens.EaseOut),
        label = "seasonChevronAngle"
    )

    Box(modifier = modifier) {
        // Dropdown Trigger Pill
        Box(
            modifier = Modifier
                .height(36.dp)
                .videoOverlayGlass(shape = PlayerTokens.RadiusSmall)
                .tvFocusable(shape = PlayerTokens.RadiusSmall)
                .playerPressable(
                    role = Role.DropdownList,
                    onClickLabel = "Season selector, currently Season $selectedSeason",
                    onClick = {
                        onInteraction()
                        isExpanded = !isExpanded
                    }
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Season $selectedSeason",
                    color = Color.White,
                    style = PlayerTokens.PillText.copy(fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevronAngle)
                )
            }
        }

        // Dropdown Menu Popover
        if (isExpanded) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 42.dp.roundToPx() }),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 220.dp)
                        .sheetGlass(PlayerTokens.RadiusMedium)
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(seasons, key = { it }) { seasonNum ->
                            val isSelected = seasonNum == selectedSeason
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(PlayerTokens.RadiusSmall)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .tvFocusable(shape = PlayerTokens.RadiusSmall)
                                    .playerPressable(
                                        role = Role.Button,
                                        onClickLabel = "Select Season $seasonNum"
                                    ) {
                                        onInteraction()
                                        onSeasonSelect(seasonNum)
                                        isExpanded = false
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Season $seasonNum",
                                    color = Color.White,
                                    style = PlayerTokens.RowTitle.copy(fontSize = 13.sp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(PlayerTokens.SizeCheckCircle)
                                            .clip(PlayerTokens.RadiusCircle)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = PlayerTokens.LabelDark,
                                            modifier = Modifier.size(PlayerTokens.IconCheck)
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
}
