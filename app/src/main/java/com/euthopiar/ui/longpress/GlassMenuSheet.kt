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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.AppAsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import com.euthopiar.ui.theme.GlassDefaults
import kotlinx.coroutines.launch

/**
 * Style B: Floating Poster with Glass Menu (iOS / Apple Context Menu style)
 *
 * Fix Pass:
 * - Scrim at 78% black with 16dp Haze background blur (Rule 1.1).
 * - Poster bottom edge positioned strictly 24 dp above the top of the menu card.
 * - Poster height 240 dp (shrinks keeping 2:3 ratio if less than 24 dp spare under status bar).
 * - 300 ms spring fly-in animation from pressed card bounds and reverse on dismiss.
 * - Menu card almost opaque (#26262C at 94%) with 16 dp Haze blur.
 * - 5 rows of 52 dp each with 8% white press state, including destructive "Not interested".
 */
@Composable
fun GlassMenuSheet(
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
                stiffness = 320f // ~300 ms spring
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
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Menu dimensions: 5 or 6 rows @ 52 dp + dividers @ 0.5 dp
    val menuHeight = if (actions.onAddToPlaylist != null) 314.dp else 262.dp
    val menuBottomMargin = 16.dp
    val gapBetweenPosterAndMenu = 24.dp
    val minTopStatusMargin = 32.dp

    // Poster size calculation: 240 dp default, shrinks preserving 2:3 ratio if tight
    val availablePosterHeight = screenHeight - minTopStatusMargin - gapBetweenPosterAndMenu - menuHeight - menuBottomMargin
    val posterHeight = if (availablePosterHeight < 240.dp) availablePosterHeight.coerceAtLeast(140.dp) else 240.dp
    val posterWidth = posterHeight * (2f / 3f)

    var isInWatchlist by remember(item.id, item.inWatchlist) { mutableStateOf(item.inWatchlist) }

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
                    if (dragOffsetY > 140f) {
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
        // 1. Full-screen scrim at 78% opacity with 16 dp Haze background blur (Rule 1.1)
        SharedScrim(
            onDismiss = dismissWithAnim,
            scrimAlpha = 0.78f,
            progress = animProgress,
            hazeState = hazeState
        )

        // 2. Poster + Menu positioned strictly as a single unified vertical column pinned to the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = menuBottomMargin)
                .graphicsLayer {
                    translationY = dragOffsetY
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- A. Floating Poster directly 24 dp above the menu ---
            val cardBounds = item.cardBounds
            val posterScale = if (cardBounds != null) {
                val initialScale = (cardBounds.width / with(density) { posterWidth.toPx() }).coerceIn(0.5f, 1.2f)
                initialScale + (1f - initialScale) * animProgress
            } else {
                0.85f + 0.15f * animProgress
            }

            val cleanTitle = remember(item.title) { sanitizeTitle(item.title) }
            Box(
                modifier = Modifier
                    .size(width = posterWidth, height = posterHeight)
                    .graphicsLayer {
                        scaleX = posterScale
                        scaleY = posterScale
                        alpha = animProgress.coerceIn(0f, 1f)
                    }
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
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

            Spacer(modifier = Modifier.height(gapBetweenPosterAndMenu))

            // --- B. Frosted Glass Menu Card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - animProgress) * 36f
                        alpha = animProgress.coerceIn(0f, 1f)
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.45f)
                    )
                    .frostedGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        hazeState = hazeState,
                        fallbackColor = GlassDefaults.NavbarFallbackColor,
                        customTint = GlassDefaults.TintColor
                    )
            ) {
                val isResume = item.progressPercent > 0
                val playLabel = if (isResume) "Resume" else "Play"

                // Row 1: Play / Resume
                GlassMenuRow(
                    label = playLabel,
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        dismissWithAnim()
                        if (isResume) actions.onResume() else actions.onPlay()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.5.dp)

                // Row 2: Add to watchlist / In watchlist (swaps icon and label; keeps menu open)
                GlassMenuRow(
                    label = if (isInWatchlist) "In watchlist" else "Add to watchlist",
                    icon = if (isInWatchlist) Icons.Outlined.Check else Icons.Rounded.Add,
                    onClick = {
                        isInWatchlist = !isInWatchlist
                        actions.onToggleWatchlist()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.5.dp)

                if (actions.onAddToPlaylist != null) {
                    GlassMenuRow(
                        label = "Add to playlist",
                        icon = Icons.Filled.PlaylistAdd,
                        onClick = {
                            dismissWithAnim()
                            actions.onAddToPlaylist.invoke()
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.5.dp)
                }

                // Row 3: Share
                GlassMenuRow(
                    label = "Share",
                    icon = Icons.Outlined.Share,
                    onClick = {
                        dismissWithAnim()
                        actions.onShare()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.5.dp)

                // Row 4: View details
                GlassMenuRow(
                    label = "View details",
                    icon = Icons.Outlined.Info,
                    onClick = {
                        dismissWithAnim()
                        actions.onOpenDetails()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.5.dp)

                // Row 5: Not interested (destructive red #FF7A7A)
                GlassMenuRow(
                    label = "Not interested",
                    icon = Icons.Outlined.Block,
                    tint = LongPressDestructive,
                    onClick = {
                        dismissWithAnim()
                        actions.onNotInterested()
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassMenuRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LongPressPrimaryText
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedBg = if (isPressed) Modifier.background(Color.White.copy(alpha = 0.08f)) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(pressedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
