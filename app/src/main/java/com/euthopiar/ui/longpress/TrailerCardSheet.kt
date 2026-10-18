package com.euthopiar.ui.longpress

import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.ui.components.TrailerPlayerSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.euthopiar.ui.theme.Accent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.euthopiar.ui.components.AppAsyncImage
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
 * Style D: Trailer Preview Card
 *
 * Bottom sheet featuring an inline 16:9 trailer preview powered by Media3 ExoPlayer.
 * Starts muted after 400ms delay, includes mute toggle, video close button,
 * and quick actions below.
 */
@OptIn(UnstableApi::class)
@Composable
fun TrailerCardSheet(
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

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.88f).dp

    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isPowerSaver = powerManager?.isPowerSaveMode == true
    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val isDataSaver = connectivityManager?.isActiveNetworkMetered == true

    var resolvedTrailerUrl by remember(item.id, item.trailerUrl) { mutableStateOf(item.trailerUrl) }
    var isResolvingTrailer by remember(item.id) { mutableStateOf(item.trailerUrl.isNullOrBlank()) }

    LaunchedEffect(item.id) {
        if (resolvedTrailerUrl.isNullOrBlank()) {
            isResolvingTrailer = true
            try {
                val details = ProviderRepository.Default.getDetails(item.id, item.type)
                resolvedTrailerUrl = details.trailerUrl
            } catch (_: Exception) {
            } finally {
                isResolvingTrailer = false
            }
        }
    }

    val hasTrailer = !resolvedTrailerUrl.isNullOrBlank()
    val canAutoplay = hasTrailer && !isPowerSaver && !isDataSaver

    var isPlayerReady by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var isVideoStopped by remember { mutableStateOf(!canAutoplay) }

    LaunchedEffect(resolvedTrailerUrl) {
        if (!resolvedTrailerUrl.isNullOrBlank() && !isPowerSaver && !isDataSaver) {
            isVideoStopped = false
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
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Drag handle
                SheetDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

                // 1. 16:9 Video Card, inset 12 dp from sides, 16 dp corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(LongPressPlaceholder)
                ) {
                    val backdropUrl = item.backdropUrl ?: item.posterUrl

                    // Backdrop image as base / fallback
                    AppAsyncImage(
                        model = backdropUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        titleForFallback = item.title,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 40 dp top gradient scrim so controls and handle stay visible (Rule 1.3)
                    // Active clean trailer player if available
                    if (hasTrailer && !isVideoStopped && resolvedTrailerUrl != null) {
                        TrailerPlayerSurface(
                            trailerUrl = resolvedTrailerUrl!!,
                            modifier = Modifier.fillMaxSize(),
                            startMuted = isMuted,
                            autoPlay = true,
                            onCloseVideo = { isVideoStopped = true }
                        )

                        // Bottom-left "Trailer" pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(ShapeFull)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), ShapeFull)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Trailer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    } else if (hasTrailer && isVideoStopped) {
                        ImageTopScrim()
                        // Play button to manually start trailer
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                .clickable {
                                    isVideoStopped = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play trailer",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (isResolvingTrailer) {
                        ImageTopScrim()
                        // Loading indicator while resolving trailer URL
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.50f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        ImageTopScrim()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Title & Meta Row under video
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sanitizeTitle(item.title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = LongPressPrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val genreStr = item.genres.take(2).joinToString(", ").takeIf { it.isNotBlank() }
                    MetaRow(item = item, extraMeta = genreStr)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Row of two equal buttons, 48 dp tall: PrimaryPill and Soft Watchlist Pill
                val isResume = item.progressPercent > 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryPill(
                        label = if (isResume) "Resume" else "Play",
                        onClick = {
                            dismissWithAnim()
                            if (isResume) actions.onResume() else actions.onPlay()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Soft Watchlist Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(ShapeFull)
                            .background(LongPressSoftButtonFill)
                            .pressable(onClick = actions.onToggleWatchlist)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WatchlistIcon(inWatchlist = item.inWatchlist)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (item.inWatchlist) "In watchlist" else "Watchlist",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LongPressPrimaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Bottom row of three text actions: Share, Details, Download (icon + 12 sp label)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrailerTextAction(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        onClick = actions.onShare
                    )
                    if (actions.onAddToPlaylist != null) {
                        TrailerTextAction(
                            icon = Icons.Filled.PlaylistAdd,
                            label = "Playlist",
                            onClick = {
                                dismissWithAnim()
                                actions.onAddToPlaylist.invoke()
                            }
                        )
                    }
                    TrailerTextAction(
                        icon = Icons.Rounded.Info,
                        label = "Details",
                        onClick = {
                            dismissWithAnim()
                            actions.onOpenDetails()
                        }
                    )
                    TrailerTextAction(
                        icon = Icons.Rounded.FileDownload,
                        label = "Download",
                        onClick = actions.onDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailerTextAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LongPressSecondaryText,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = LongPressSecondaryText
        )
    }
}
