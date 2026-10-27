package com.euthopiar.ui.longpress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ThumbUp
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ThumbUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.util.PosterColorCache
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * Style C: Poster-Tinted Sheet with Resume
 *
 * Background samples the poster's dominant color via Palette,
 * darkened to ~35% lightness, fading to #141416 at 50% sheet height.
 * Features an 80x120dp poster, synopsis, thin progress bar with remaining time,
 * and a weighted button row (PrimaryPill 1.6x, CircleActions 1.0x).
 */
@Composable
fun TintedResumeSheet(
    item: SheetItem,
    actions: SheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: dev.chrisbanes.haze.HazeState? = LocalLongPressHazeState.current
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
    val maxHeight = (configuration.screenHeightDp * 0.85f).dp

    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    var resolvedSynopsis by remember(item.id, item.synopsis) { mutableStateOf(item.synopsis) }
    var resolvedGenres by remember(item.id, item.genres) { mutableStateOf(item.genres) }
    LaunchedEffect(item.id) {
        if (resolvedSynopsis.isNullOrBlank()) {
            try {
                val detail = ProviderRepository.Default.getDetails(item.id, item.type)
                resolvedSynopsis = detail.synopsis
                if (detail.genres != null && detail.genres.isNotEmpty()) {
                    resolvedGenres = detail.genres
                }
            } catch (_: Exception) {}
        }
    }

    val defaultFallbackColor = Color(0xFF3A2E22)
    var extractedColor by remember { mutableStateOf(defaultFallbackColor) }

    LaunchedEffect(item.posterUrl) {
        val cached = PosterColorCache.get(item.posterUrl)
        if (cached != null) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(
                android.graphics.Color.argb(
                    255,
                    (cached.color1.red * 255).toInt(),
                    (cached.color1.green * 255).toInt(),
                    (cached.color1.blue * 255).toInt()
                ),
                hsl
            )
            hsl[2] = hsl[2].coerceAtMost(0.30f)
            extractedColor = Color(ColorUtils.HSLToColor(hsl))
        } else if (!item.posterUrl.isNullOrBlank()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .size(64, 64)
                    .allowHardware(false)
                    .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
                        val swatch = palette?.dominantSwatch ?: palette?.darkVibrantSwatch
                        if (swatch != null) {
                            val hsl = FloatArray(3)
                            ColorUtils.colorToHSL(swatch.rgb, hsl)
                            hsl[2] = hsl[2].coerceAtMost(0.30f)
                            extractedColor = Color(ColorUtils.HSLToColor(hsl))
                        }
                    }
                }
            } catch (_: Throwable) {
                extractedColor = defaultFallbackColor
            }
        }
    }

    val animatedTopColor by animateColorAsState(
        targetValue = extractedColor,
        animationSpec = tween(durationMillis = 200),
        label = "TintedSheetTopColor"
    )

    val bottomBaseColor = Color(0xFF141416)

    Box(modifier = modifier.fillMaxSize()) {
        val animProgress = openAnim.value
        val slideOffset = (sheetHeightPx.takeIf { it > 0f } ?: with(density) { 450.dp.toPx() }) * (1f - animProgress)
        val totalOffsetY = slideOffset + dragOffsetY

        SharedScrim(
            onDismiss = dismissWithAnim,
            scrimAlpha = 0.76f,
            progress = animProgress,
            hazeState = hazeState
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
                .background(
                    Brush.verticalGradient(
                        0.0f to animatedTopColor,
                        0.60f to bottomBaseColor,
                        1.0f to bottomBaseColor
                    )
                )
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

                // 1. Poster Row: Poster 96 x 144 dp on the left, Meta + Genres + Synopsis on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 144.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = LongPressPrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Meta line: year, type, runtime
                        val runtimeStr = item.runtimeMinutes?.let { "${it}m" }
                        MetaRow(item = item, extraMeta = runtimeStr)

                        // Star rating in accent colour
                        val ratingText = item.rating
                        if (!ratingText.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rating",
                                    tint = Accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = ratingText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Accent
                                )
                            }
                        }

                        // Genres on one line (if available)
                        if (resolvedGenres.isNotEmpty()) {
                            Text(
                                text = resolvedGenres.take(3).joinToString(" • "),
                                fontSize = 11.sp,
                                color = LongPressSecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Synopsis (up to 3 lines if available)
                        if (!resolvedSynopsis.isNullOrBlank()) {
                            Text(
                                text = resolvedSynopsis!!,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Progress bar with remaining time caption (if progress > 0)
                if (item.progressPercent > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ThinProgress(progressPercent = item.progressPercent)
                    Spacer(modifier = Modifier.height(4.dp))

                    val caption = if (item.runtimeMinutes != null && item.runtimeMinutes > 0) {
                        val hours = item.runtimeMinutes / 60
                        val mins = item.runtimeMinutes % 60
                        if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
                    } else {
                        "${item.progressPercent}% watched"
                    }

                    Text(
                        text = caption,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Weighted Button Row: PrimaryPill (1.6 weight) + Watchlist (1.0 weight) + Like (1.0 weight)
                // Centered vertically on the 52 dp circles only (Alignment.Top so labels hang underneath without pushing Play pill)
                val isResume = item.progressPercent > 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1.6f)) {
                        PrimaryPill(
                            label = if (isResume) "Resume" else "Play",
                            onClick = {
                                dismissWithAnim()
                                if (isResume) actions.onResume() else actions.onPlay()
                            },
                            height = 52.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.TopCenter) {
                        CircleAction(
                            icon = if (item.inWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            label = "Watchlist",
                            isActive = item.inWatchlist,
                            onClick = actions.onToggleWatchlist
                        )
                    }

                    val isLiked = item.reaction == UserReaction.LIKE
                    Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.TopCenter) {
                        CircleAction(
                            icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                            label = "Like",
                            isActive = isLiked,
                            onClick = {
                                val next = if (isLiked) UserReaction.NONE else UserReaction.LIKE
                                actions.onReact(next)
                            }
                        )
                    }

                    if (actions.onAddToPlaylist != null) {
                        Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.TopCenter) {
                            CircleAction(
                                icon = Icons.Filled.PlaylistAdd,
                                label = "Playlist",
                                onClick = {
                                    dismissWithAnim()
                                    actions.onAddToPlaylist.invoke()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
