package com.euthopiar.ui.longpress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.PlaylistAdd
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.AppAsyncImage
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * Style A: Hero Peek Sheet (Default)
 *
 * Cinematic bottom sheet with 180dp backdrop header, vertical fade,
 * primary Play/Resume pill, 4 circular actions, and "View full details" row.
 */
@Composable
fun HeroPeekSheet(
    item: SheetItem,
    actions: SheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val openAnim = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        openAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 280,
                easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
            )
        )
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                openAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                onDismiss()
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.78f).dp
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    var isDownloaded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        SharedScrim(
            onDismiss = dismissWithAnim,
            scrimAlpha = 0.70f,
            progress = openAnim.value
        )

        val animProgress = openAnim.value
        val slideOffset = (sheetHeightPx.takeIf { it > 0f } ?: with(density) { 450.dp.toPx() }) * (1f - animProgress)
        val totalOffsetY = slideOffset + dragOffsetY

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .offset { IntOffset(0, totalOffsetY.roundToInt()) }
                .onGloballyPositioned { coordinates ->
                    sheetHeightPx = coordinates.size.height.toFloat()
                }
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || dragOffsetY > 0) {
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (sheetHeightPx > 0 && dragOffsetY > sheetHeightPx * 0.30f) {
                            dismissWithAnim()
                        } else {
                            coroutineScope.launch {
                                Animatable(dragOffsetY).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffsetY = value
                                }
                            }
                        }
                    }
                )
                .clip(LongPressSheetShape)
                .then(LongPressSheetBorder)
                .background(LongPressSheetBackground)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Backdrop Header (180 dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val backdropUrl = item.backdropUrl ?: item.posterUrl
                    AppAsyncImage(
                        model = backdropUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        titleForFallback = item.title,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 40 dp gradient scrim at the very top so handle never vanishes (Rule 1.3)
                    ImageTopScrim()

                    // Vertical gradient from transparent to #18181C covering lower 70%
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    0.70f to LongPressSheetBackground.copy(alpha = 0.85f),
                                    1.0f to LongPressSheetBackground
                                )
                            )
                    )

                    // Drag handle
                    SheetDragHandle(modifier = Modifier.align(Alignment.TopCenter))

                    // Title & MetaRow at bottom-left of backdrop
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 8.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sanitizeTitle(item.title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = LongPressPrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        MetaRow(item = item)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Primary Pill (Full Width)
                val isResume = item.progressPercent > 0
                val primaryLabel = if (isResume) "Resume" else "Play"
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PrimaryPill(
                        label = primaryLabel,
                        onClick = {
                            dismissWithAnim()
                            if (isResume) actions.onResume() else actions.onPlay()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Row of four CircleActions: Watchlist, Like, Share, Download
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleAction(
                        icon = if (item.inWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = "Watchlist",
                        isActive = item.inWatchlist,
                        onClick = actions.onToggleWatchlist
                    )

                    val isLiked = item.reaction == UserReaction.LIKE
                    CircleAction(
                        icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                        label = "Like",
                        isActive = isLiked,
                        onClick = {
                            val next = if (isLiked) UserReaction.NONE else UserReaction.LIKE
                            actions.onReact(next)
                        }
                    )

                    CircleAction(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        onClick = actions.onShare
                    )

                    CircleAction(
                        icon = if (isDownloaded) Icons.Rounded.Check else Icons.Rounded.FileDownload,
                        label = if (isDownloaded) "Saved" else "Download",
                        isActive = isDownloaded,
                        onClick = {
                            isDownloaded = !isDownloaded
                            actions.onDownload()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (actions.onAddToPlaylist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    dismissWithAnim()
                                    actions.onAddToPlaylist.invoke()
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = LongPressPrimaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Add to playlist",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LongPressPrimaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = "Add to playlist",
                            tint = LongPressSecondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    HorizontalDivider(
                        color = LongPressDivider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 4. "View full details" row (48 dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                dismissWithAnim()
                                actions.onOpenDetails()
                            }
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "View full details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LongPressPrimaryText
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Details",
                        tint = LongPressSecondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
