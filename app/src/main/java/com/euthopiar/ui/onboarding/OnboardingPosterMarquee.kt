package com.euthopiar.ui.onboarding

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import com.euthopiar.R
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.roundToInt

data class ShowcasePosterItem(
    val title: String,
    val genre: String = "",
    val year: String = "",
    val posterUrl: String,
    val fallbackDrawableRes: Int = R.drawable.avatar_classic_director,
)

fun com.euthopiar.core.model.MediaItem.toShowcasePosterItem(
    fallbackRes: Int? = null
): ShowcasePosterItem? {
    val url = posterUrl?.takeIf { it.isNotBlank() } ?: return null
    val displayGenre = when (type) {
        com.euthopiar.core.model.MediaType.TV_SERIES -> "TV Series"
        com.euthopiar.core.model.MediaType.ANIME -> "Anime"
        com.euthopiar.core.model.MediaType.LIVE_TV -> "Live TV"
        com.euthopiar.core.model.MediaType.MOVIE -> "Movie"
    }
    val defaultRes = when (type) {
        com.euthopiar.core.model.MediaType.ANIME -> R.drawable.avatar_anime_hero
        com.euthopiar.core.model.MediaType.TV_SERIES, com.euthopiar.core.model.MediaType.LIVE_TV -> R.drawable.avatar_retro_cinephile
        com.euthopiar.core.model.MediaType.MOVIE -> R.drawable.avatar_classic_director
    }
    return ShowcasePosterItem(
        title = title,
        genre = displayGenre,
        year = year?.toString().orEmpty(),
        posterUrl = url,
        fallbackDrawableRes = fallbackRes ?: defaultRes
    )
}

/**
 * 16 Real Posters (8 distinct per row) from the app's catalog.
 */
internal val ShowcaseRowTop = listOf(
    ShowcasePosterItem(
        title = "Dune: Part Two",
        genre = "Sci-Fi",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w342/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
        fallbackDrawableRes = R.drawable.avatar_cyber_pilot,
    ),
    ShowcasePosterItem(
        title = "Arcane",
        genre = "Animation",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w342/fqldf2t8ztc9aiwn397FQ3CFOxc.jpg",
        fallbackDrawableRes = R.drawable.avatar_anime_hero,
    ),
    ShowcasePosterItem(
        title = "Oppenheimer",
        genre = "Drama",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w342/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
    ShowcasePosterItem(
        title = "Across Spider-Verse",
        genre = "Animation",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w342/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
        fallbackDrawableRes = R.drawable.avatar_phantom_warrior,
    ),
    ShowcasePosterItem(
        title = "Spirited Away",
        genre = "Animation",
        year = "2001",
        posterUrl = "https://image.tmdb.org/t/p/w342/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg",
        fallbackDrawableRes = R.drawable.avatar_anime_hero,
    ),
    ShowcasePosterItem(
        title = "Inception",
        genre = "Sci-Fi",
        year = "2010",
        posterUrl = "https://image.tmdb.org/t/p/w342/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
        fallbackDrawableRes = R.drawable.avatar_classic_director,
    ),
    ShowcasePosterItem(
        title = "Shogun",
        genre = "Drama",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w342/7O4iVfOMQmdCSxhOg1WNzG1AgYT.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
    ShowcasePosterItem(
        title = "Succession",
        genre = "Drama",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w342/7hwVaTvd9k04Gqj6Jj8A21mH6nL.jpg",
        fallbackDrawableRes = R.drawable.avatar_retro_cinephile,
    ),
)

internal val ShowcaseRowBottom = listOf(
    ShowcasePosterItem(
        title = "Interstellar",
        genre = "Sci-Fi",
        year = "2014",
        posterUrl = "https://image.tmdb.org/t/p/w342/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        fallbackDrawableRes = R.drawable.avatar_retro_cinephile,
    ),
    ShowcasePosterItem(
        title = "Blade Runner 2049",
        genre = "Sci-Fi",
        year = "2017",
        posterUrl = "https://image.tmdb.org/t/p/w342/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
        fallbackDrawableRes = R.drawable.avatar_classic_director,
    ),
    ShowcasePosterItem(
        title = "Cyberpunk: Edgerunners",
        genre = "Anime",
        year = "2022",
        posterUrl = "https://image.tmdb.org/t/p/w342/7jswSQ119u1Ucu4fXQJ65mKjU3T.jpg",
        fallbackDrawableRes = R.drawable.avatar_cyber_pilot,
    ),
    ShowcasePosterItem(
        title = "The Dark Knight",
        genre = "Action",
        year = "2008",
        posterUrl = "https://image.tmdb.org/t/p/w342/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
    ShowcasePosterItem(
        title = "Attack on Titan",
        genre = "Anime",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w342/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg",
        fallbackDrawableRes = R.drawable.avatar_phantom_warrior,
    ),
    ShowcasePosterItem(
        title = "The Batman",
        genre = "Action",
        year = "2022",
        posterUrl = "https://image.tmdb.org/t/p/w342/74xTEgt7R36Fpooo50r9T25onhq.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
    ShowcasePosterItem(
        title = "Breaking Bad",
        genre = "Crime",
        year = "2013",
        posterUrl = "https://image.tmdb.org/t/p/w342/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
        fallbackDrawableRes = R.drawable.avatar_classic_director,
    ),
    ShowcasePosterItem(
        title = "Frieren",
        genre = "Anime",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w342/dqNJVb7Z3QO9G2Kz9pZfIePqK8k.jpg",
        fallbackDrawableRes = R.drawable.avatar_anime_hero,
    ),
)

/**
 * PAGE 1: POSTER COLLAGE
 * - Full-bleed (edge to edge), 18 dp below top row, height 304 dp, clipped.
 * - Two rows of poster tiles. Each tile: 96 dp wide by 144 dp tall (2:3), 12 dp corner radius,
 *   0.5 dp hairline border at white 16%, no shadow, no tilt, no rotation.
 * - 10 dp gap between tiles in a row, 12 dp gap between the two rows.
 * - Row 2 shifted left by half a tile (48 dp) so rows do not line up.
 * - Row 1 drifts left (24 s loop), Row 2 drifts right (28 s loop), mirrored in RTL.
 * - Pauses drift on: background, page inactive, battery saver, reduce motion, or low-end device.
 * - Offline fallback: glass tile with quiet outline film icon at white 30%.
 * - Bottom melt: 130 dp smootherstep scrim fading to exact page background color.
 */
@Composable
fun OnboardingPosterMarquee(
    modifier: Modifier = Modifier,
    topRowItems: List<ShowcasePosterItem> = ShowcaseRowTop,
    bottomRowItems: List<ShowcasePosterItem> = ShowcaseRowBottom,
    isLoading: Boolean = false,
    isActive: Boolean = true,
) {
    val context = LocalContext.current
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Lifecycle check for background state
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Battery saver check
    val isBatterySaver = remember(context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode == true
        } catch (_: Exception) {
            false
        }
    }

    // Low-end device detection
    val isLowEnd = remember(context) {
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            actManager?.isLowRamDevice == true || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        } catch (_: Exception) {
            false
        }
    }

    val shouldAnimate = isActive && isAppInForeground && !reduceMotion && !isBatterySaver && !isLowEnd

    val cardWidth = 96.dp
    val cardHeight = 144.dp
    val tileGap = 10.dp
    val rowGap = 12.dp
    val row2Shift = 48.dp

    val row1Items = remember(topRowItems) {
        val base = if (topRowItems.isNotEmpty()) topRowItems else ShowcaseRowTop
        var list = base
        while (list.size < 8) {
            list = list + base
        }
        list
    }
    val row2Items = remember(bottomRowItems) {
        val base = if (bottomRowItems.isNotEmpty()) bottomRowItems else ShowcaseRowBottom
        var list = base
        while (list.size < 8) {
            list = list + base
        }
        list
    }

    val density = LocalDensity.current
    val itemStridePx = with(density) { (cardWidth + tileGap).toPx() }
    val cycleLength1Px = itemStridePx * row1Items.size
    val cycleLength2Px = itemStridePx * row2Items.size

    val infiniteTransition = rememberInfiniteTransition(label = "posterDriftTransition")

    // Row 1: 24 second cycle
    val factorRow1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftRow1",
    )

    // Row 2: 28 second cycle with 0.35f initial phase offset
    val factorRow2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftRow2",
    )

    // Repeated 3 times for seamless wrapping
    val topItems = remember(row1Items) { row1Items + row1Items + row1Items }
    val bottomItems = remember(row2Items) { row2Items + row2Items + row2Items }

    val pageBgColor = if (isAmoled) OnboardingTokens.AmoledBackground else OnboardingTokens.DarkBackground

    // Pre-calculated Smootherstep Gradient for bottom 130 dp melt
    val meltBrush = remember(pageBgColor) {
        Brush.verticalGradient(
            0.00f to pageBgColor.copy(alpha = 0.000f),
            0.15f to pageBgColor.copy(alpha = 0.025f),
            0.30f to pageBgColor.copy(alpha = 0.163f),
            0.50f to pageBgColor.copy(alpha = 0.500f),
            0.70f to pageBgColor.copy(alpha = 0.837f),
            0.85f to pageBgColor.copy(alpha = 0.975f),
            0.95f to pageBgColor.copy(alpha = 1.000f),
            1.00f to pageBgColor.copy(alpha = 1.000f),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(304.dp)
            .clip(RoundedCornerShape(0.dp))
            .clearAndSetSemantics {}, // Decorative collage hidden from screen readers
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            // ROW 1: moves left (or right in RTL)
            val effectiveFactor1 = if (shouldAnimate) (factorRow1 % 1f) else 0f
            val rawOffset1 = -(effectiveFactor1 * cycleLength1Px)
            val finalOffset1 = if (isRtl) -rawOffset1 - cycleLength1Px else rawOffset1

            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = finalOffset1
                    },
                horizontalArrangement = Arrangement.spacedBy(tileGap),
            ) {
                val renderItems = if (!shouldAnimate) row1Items else topItems
                renderItems.forEach { item ->
                    OnboardingPosterTile(
                        item = item,
                        width = cardWidth,
                        height = cardHeight,
                    )
                }
            }

            // ROW 2: shifted by 48 dp, moves right (or left in RTL)
            val effectiveFactor2 = if (shouldAnimate) (factorRow2 % 1f) else 0f
            val shiftPx = with(density) { row2Shift.toPx() }
            val rawOffset2 = -cycleLength2Px - shiftPx + (effectiveFactor2 * cycleLength2Px)
            val finalOffset2 = if (isRtl) -rawOffset2 - cycleLength2Px else rawOffset2

            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = finalOffset2
                    },
                horizontalArrangement = Arrangement.spacedBy(tileGap),
            ) {
                val renderItems = if (!shouldAnimate) row2Items else bottomItems
                renderItems.forEach { item ->
                    OnboardingPosterTile(
                        item = item,
                        width = cardWidth,
                        height = cardHeight,
                    )
                }
            }
        }

        // BOTTOM MELT SCRIM (130 dp tall, with 2 dp overdraw past bottom edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, 2.dp.roundToPx()) }
                .drawBehind {
                    drawRect(brush = meltBrush)
                },
        )
    }
}

/**
 * Individual poster tile: 96x144 dp (2:3), 12 dp corner radius, 0.5 dp hairline border at white 16%.
 * Shows quiet outline film icon when loading / offline.
 */
@Composable
private fun OnboardingPosterTile(
    item: ShowcasePosterItem,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(OnboardingTokens.GlassFill)
            .border(
                width = OnboardingTokens.HairlineWidth,
                color = OnboardingTokens.GlassHairlineBorder,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Fallback local illustration if offline or loading
        Image(
            painter = painterResource(id = item.fallbackDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Dark subtle glass scrim over the fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f)),
        )

        // Real Poster Image (scaled ~2x display size, 200 ms crossfade)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.posterUrl)
                .crossfade(200)
                .error(item.fallbackDrawableRes)
                .fallback(item.fallbackDrawableRes)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
