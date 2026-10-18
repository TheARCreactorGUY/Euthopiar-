package com.euthopiar.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.R
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.SurfaceLow

data class ShowcasePosterItem(
    val title: String,
    val genre: String,
    val year: String,
    val posterUrl: String,
    val fallbackDrawableRes: Int,
)

internal val ShowcaseRowTop = listOf(
    ShowcasePosterItem(
        title = "Dune: Part Two",
        genre = "Sci-Fi",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
        fallbackDrawableRes = R.drawable.avatar_cyber_pilot,
    ),
    ShowcasePosterItem(
        title = "Arcane",
        genre = "Animation",
        year = "2024",
        posterUrl = "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn397FQ3CFOxc.jpg",
        fallbackDrawableRes = R.drawable.avatar_anime_hero,
    ),
    ShowcasePosterItem(
        title = "Oppenheimer",
        genre = "Drama",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
    ShowcasePosterItem(
        title = "Across Spider-Verse",
        genre = "Animation",
        year = "2023",
        posterUrl = "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
        fallbackDrawableRes = R.drawable.avatar_phantom_warrior,
    ),
)

internal val ShowcaseRowBottom = listOf(
    ShowcasePosterItem(
        title = "Interstellar",
        genre = "Sci-Fi",
        year = "2014",
        posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        fallbackDrawableRes = R.drawable.avatar_retro_cinephile,
    ),
    ShowcasePosterItem(
        title = "Blade Runner 2049",
        genre = "Sci-Fi",
        year = "2017",
        posterUrl = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
        fallbackDrawableRes = R.drawable.avatar_classic_director,
    ),
    ShowcasePosterItem(
        title = "Cyberpunk: Edgerunners",
        genre = "Anime",
        year = "2022",
        posterUrl = "https://image.tmdb.org/t/p/w500/7jswSQ119u1Ucu4fXQJ65mKjU3T.jpg",
        fallbackDrawableRes = R.drawable.avatar_cyber_pilot,
    ),
    ShowcasePosterItem(
        title = "The Dark Knight",
        genre = "Action",
        year = "2008",
        posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
        fallbackDrawableRes = R.drawable.avatar_noir,
    ),
)

/**
 * Dual-row tilted drifting poster marquee with enlarged cards and seamless DstIn alpha feathering.
 * Eliminates visible gradient seams and aligns strictly with calm monochrome luxury.
 */
@Composable
fun OnboardingPosterMarquee(
    modifier: Modifier = Modifier,
) {
    val cardWidth = 118.dp
    val cardHeight = 176.dp
    val cardSpacing = 14.dp

    val density = LocalDensity.current
    val itemFootprintPx = with(density) { (cardWidth + cardSpacing).toPx() }
    val cycleLengthPx = itemFootprintPx * ShowcaseRowTop.size

    val infiniteTransition = rememberInfiniteTransition(label = "marqueeTransition")
    val translationFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "translationFactor",
    )

    // Repeated 4x for unbounded coverage
    val topItems = remember { ShowcaseRowTop + ShowcaseRowTop + ShowcaseRowTop + ShowcaseRowTop }
    val bottomItems = remember { ShowcaseRowBottom + ShowcaseRowBottom + ShowcaseRowBottom + ShowcaseRowBottom }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(410.dp)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                // Mathematical DstIn alpha feathering: guaranteed zero visible edges or seams
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.06f to Color.White,
                        0.58f to Color.White,
                        0.84f to Color.White.copy(alpha = 0.40f),
                        1.00f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -5.5f
                    scaleX = 1.15f
                    scaleY = 1.15f
                    translationY = 24.dp.toPx()
                }
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
            // Row 1: Leftward continuous drift
            val topOffsetX = -(translationFactor * cycleLengthPx)
            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = topOffsetX
                    },
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
                topItems.forEach { item ->
                    MarqueeCard(
                        item = item,
                        width = cardWidth,
                        height = cardHeight,
                    )
                }
            }

            // Row 2: Rightward continuous drift
            val bottomOffsetX = -cycleLengthPx + (translationFactor * cycleLengthPx)
            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = bottomOffsetX
                    },
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
                bottomItems.forEach { item ->
                    MarqueeCard(
                        item = item,
                        width = cardWidth,
                        height = cardHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarqueeCard(
    item: ShowcasePosterItem,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(SurfaceLow)
            .onboardingCardBorder(cornerRadius = 16.dp),
    ) {
        // Fallback offline illustration
        Image(
            painter = painterResource(id = item.fallbackDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Dark obsidian atmosphere base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color(0xFF0B0B0F).copy(alpha = 0.70f),
                        ),
                    ),
                ),
        )

        // TMDB Poster image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Top specular glass sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height * 0.25f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Bottom label metadata in restrained monochrome
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0B0B0F).copy(alpha = 0.92f)),
                    ),
                )
                .padding(horizontal = 9.dp, vertical = 7.dp),
        ) {
            Text(
                text = item.title,
                style = AppTypography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.genre,
                    style = AppTypography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                    color = Color(0xFFD1D5DB),
                )
                Text(
                    text = "• ${item.year}",
                    style = AppTypography.labelSmall.copy(fontSize = 9.5.sp),
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}
