package com.euthopiar.ui.longpress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ThumbDown
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.pressable
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * Style F: Reaction Row Plus Quick List
 *
 * Compact sheet with a 3-pill taste rating row (Dislike, Like, Love),
 * animated confirmation feedback text, and a clean list of 52dp action rows with dividers.
 */
@Composable
fun ReactionListSheet(
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
    val maxHeight = (configuration.screenHeightDp * 0.85f).dp

    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    var confirmationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(confirmationMessage) {
        if (confirmationMessage != null) {
            delay(1500L)
            confirmationMessage = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val animProgress = openAnim.value
        val slideOffset = (sheetHeightPx.takeIf { it > 0f } ?: with(density) { 450.dp.toPx() }) * (1f - animProgress)
        val totalOffsetY = slideOffset + dragOffsetY

        SharedScrim(
            onDismiss = dismissWithAnim,
            scrimAlpha = 0.70f,
            progress = animProgress
        )

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                // Drag handle
                SheetDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

                // 1. Header row: poster 48 x 72 dp, title (16 sp medium), meta line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LongPressPlaceholder)
                    ) {
                        AppAsyncImage(
                            model = item.posterUrl ?: item.backdropUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            titleForFallback = item.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sanitizeTitle(item.title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = LongPressPrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        MetaRow(item = item)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Reaction row: three equal pills, 40 dp tall, soft fill, 20 dp icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dislike pill
                    val isDisliked = item.reaction == UserReaction.DISLIKE
                    ReactionPill(
                        icon = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        label = "Dislike",
                        isSelected = isDisliked,
                        onClick = {
                            val next = if (isDisliked) UserReaction.NONE else UserReaction.DISLIKE
                            actions.onReact(next)
                            confirmationMessage = if (next == UserReaction.DISLIKE) "We'll show you less like this" else "Reaction removed"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Like pill
                    val isLiked = item.reaction == UserReaction.LIKE
                    ReactionPill(
                        icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        label = "Like",
                        isSelected = isLiked,
                        onClick = {
                            val next = if (isLiked) UserReaction.NONE else UserReaction.LIKE
                            actions.onReact(next)
                            confirmationMessage = if (next == UserReaction.LIKE) "We'll show you more like this" else "Reaction removed"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Love pill
                    val isLoved = item.reaction == UserReaction.LOVE
                    ReactionPill(
                        icon = if (isLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label = "Love",
                        isSelected = isLoved,
                        onClick = {
                            val next = if (isLoved) UserReaction.NONE else UserReaction.LOVE
                            actions.onReact(next)
                            confirmationMessage = if (next == UserReaction.LOVE) "Added to your top taste profile" else "Reaction removed"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Confirmation text banner (1.5 seconds)
                AnimatedVisibility(
                    visible = confirmationMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = confirmationMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)

                // 4. Action List: 52 dp rows with 24 dp icon on the left and 15 sp label
                val isResume = item.progressPercent > 0

                // Row 1: Play / Resume
                ReactionListActionRow(
                    icon = Icons.Outlined.PlayArrow,
                    label = if (isResume) "Resume" else "Play",
                    onClick = {
                        dismissWithAnim()
                        if (isResume) actions.onResume() else actions.onPlay()
                    }
                )
                HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)

                // Row 2: Add to watchlist / In watchlist
                ReactionListActionRow(
                    icon = if (item.inWatchlist) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    label = if (item.inWatchlist) "In watchlist" else "Add to watchlist",
                    iconTint = if (item.inWatchlist) Accent else Color.White,
                    onClick = actions.onToggleWatchlist
                )
                HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)

                if (actions.onAddToPlaylist != null) {
                    ReactionListActionRow(
                        icon = Icons.Filled.PlaylistAdd,
                        label = "Add to playlist",
                        onClick = {
                            dismissWithAnim()
                            actions.onAddToPlaylist.invoke()
                        }
                    )
                    HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)
                }

                // Row 3: Download
                ReactionListActionRow(
                    icon = Icons.Outlined.FileDownload,
                    label = "Download",
                    onClick = {
                        dismissWithAnim()
                        actions.onDownload()
                    }
                )
                HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)

                // Row 4: Share
                ReactionListActionRow(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    onClick = {
                        dismissWithAnim()
                        actions.onShare()
                    }
                )
                HorizontalDivider(color = LongPressDivider, thickness = 0.5.dp)

                // Row 5: Details
                ReactionListActionRow(
                    icon = Icons.Outlined.Info,
                    label = "Details",
                    onClick = {
                        dismissWithAnim()
                        actions.onOpenDetails()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Accent else LongPressSoftButtonFill
    val tint = if (isSelected) getOnAccentColor(Accent) else Color.White

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(ShapeFull)
            .background(bg)
            .pressable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun ReactionListActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = LongPressPrimaryText
        )
    }
}
