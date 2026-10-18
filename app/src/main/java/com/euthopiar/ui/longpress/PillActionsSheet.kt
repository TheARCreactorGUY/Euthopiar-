package com.euthopiar.ui.longpress

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.rounded.Share
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.theme.GlassDefaults
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Style E: Hero Poster with Pill Actions
 *
 * Minimalist layout:
 * - Scrim at 78% black with 16dp Haze background blur (Rule 1.1).
 * - Poster (140x210dp), title and meta block vertically centered in the free space
 *   between the status bar and the top of the action stack (at least 24dp gap).
 * - 52dp PrimaryPill in pure solid White with black play icon and text.
 * - Row of 2 frosted-glass secondary pills (Watchlist, Share) with background blur
 *   powered by Haze (identical to the navbar and "More Details" hero button).
 * - Centered "View details" link 16dp above the navigation bar inset.
 */
@Composable
fun PillActionsSheet(
    item: SheetItem,
    actions: SheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalLongPressHazeState.current
) {
    val coroutineScope = rememberCoroutineScope()
    val openAnim = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        openAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 320f
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

    val animProgress = openAnim.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = rememberDraggableState { delta ->
                    if (delta > 0 || dragOffsetY > 0) {
                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (dragOffsetY > 160f) {
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
    ) {
        // 1. Full-screen scrim at 78% opacity with 16 dp Haze background blur
        SharedScrim(
            onDismiss = dismissWithAnim,
            scrimAlpha = 0.78f,
            progress = animProgress,
            hazeState = hazeState
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 2. Poster + Title + Meta vertically centered in free space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .graphicsLayer {
                            scaleX = 0.88f + 0.12f * animProgress
                            scaleY = 0.88f + 0.12f * animProgress
                            alpha = animProgress.coerceIn(0f, 1f)
                            translationY = dragOffsetY
                        }
                ) {
                    val cleanTitle = remember(item.title) { sanitizeTitle(item.title) }
                    // Poster: 140 x 210 dp, 8 dp corners with depth shadow
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 210.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(8.dp),
                                ambientColor = Color.Black.copy(alpha = 0.40f),
                                spotColor = Color.Black.copy(alpha = 0.50f)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(LongPressPlaceholder)
                    ) {
                        AppAsyncImage(
                            model = item.posterUrl ?: item.backdropUrl,
                            contentDescription = cleanTitle,
                            contentScale = ContentScale.Crop,
                            titleForFallback = cleanTitle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Centered Title (20 sp medium)
                    Text(
                        text = cleanTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = LongPressPrimaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Centered MetaRow
                    MetaRow(item = item)
                }
            }

            // 3. Bottom Action Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - animProgress) * 60f + dragOffsetY
                        alpha = animProgress.coerceIn(0f, 1f)
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isResume = item.progressPercent > 0

                // A. Primary Play/Resume Pill: 52 dp tall, pure solid White with black label and icon
                PrimaryPill(
                    label = if (isResume) "Resume" else "Play",
                    onClick = {
                        dismissWithAnim()
                        if (isResume) actions.onResume() else actions.onPlay()
                    },
                    height = 52.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                // B. Row of two frosted-glass pills (Watchlist, Share), 48 dp tall
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Watchlist frosted glass pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .frostedGlassSurface(
                                shape = ShapeFull,
                                hazeState = hazeState,
                                fallbackColor = Color(0xFF2A2A30),
                                customTint = GlassDefaults.TintColor
                            )
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

                    // Share frosted glass pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .frostedGlassSurface(
                                shape = ShapeFull,
                                hazeState = hazeState,
                                fallbackColor = Color(0xFF2A2A30),
                                customTint = GlassDefaults.TintColor
                            )
                            .pressable(onClick = {
                                dismissWithAnim()
                                actions.onShare()
                            })
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LongPressPrimaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (actions.onAddToPlaylist != null) {
                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        dismissWithAnim()
                                        actions.onAddToPlaylist.invoke()
                                    }
                                )
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Playlist",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }

                    // C. Centered "View details" link (14 sp, white at 70%) with 12 dp chevron
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    dismissWithAnim()
                                    actions.onOpenDetails()
                                }
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "View details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Details",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
