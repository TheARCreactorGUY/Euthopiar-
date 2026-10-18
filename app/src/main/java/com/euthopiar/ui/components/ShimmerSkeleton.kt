package com.euthopiar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*

val LocalShimmerOffset = compositionLocalOf<Float?> { null }

/**
 * Shared shimmer animation clock provider.
 * When multiple skeleton placeholders are on screen, this drives a single
 * unified specular sheen wave across all components, avoiding dozens of concurrent transitions.
 * If reduce motion is active, disables animation completely.
 */
@Composable
fun ProvideShimmerScope(
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) {
        CompositionLocalProvider(LocalShimmerOffset provides null) {
            content()
        }
    } else {
        val transition = rememberInfiniteTransition(label = "sharedShimmer")
        val translateAnim by transition.animateFloat(
            initialValue = -500f,
            targetValue = 1500f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sharedShimmerTranslation"
        )
        CompositionLocalProvider(LocalShimmerOffset provides translateAnim) {
            content()
        }
    }
}

/**
 * High-performance, ultra-subtle obsidian glass shimmering sweep effect for luxury skeleton loaders.
 * Uses a single shared shimmer clock when inside a ProvideShimmerScope, or falls back to a standalone
 * clock. Renders a calm static base surface when reduce motion is enabled.
 */
fun Modifier.shimmerEffect(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this

    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) {
        return@composed background(SkeletonBase)
    }

    val sharedOffset = LocalShimmerOffset.current
    val translateAnim = if (sharedOffset != null) {
        sharedOffset
    } else {
        val transition = rememberInfiniteTransition(label = "standaloneShimmer")
        val localOffset by transition.animateFloat(
            initialValue = -500f,
            targetValue = 1500f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "standaloneShimmerTranslation"
        )
        localOffset
    }

    // Clean, horizontal specular sheen matching the startup screen aesthetic
    val shimmerColors = listOf(
        SkeletonBase,
        SkeletonHighlight,
        SkeletonHighlight.copy(alpha = 0.8f),
        SkeletonHighlight,
        SkeletonBase
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 450f, 0f),
        end = Offset(translateAnim, 0f)
    )

    background(brush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = ShapeS
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(HairlineWidth, Outline.copy(alpha = 0.15f), shape)
            .shimmerEffect()
    )
}

@Composable
fun MediaCardSkeleton(
    modifier: Modifier = Modifier
) {
    val scaleMode = LocalCardScaleMode.current
    val cardWidth = CardDimensions.verticalWidth(scaleMode)

    Column(
        modifier = modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(ShapeM)
                .border(HairlineWidth, Outline, ShapeM)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(14.dp),
            shape = ShapeS
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(
                modifier = Modifier.size(width = 38.dp, height = 12.dp),
                shape = ShapeS
            )
            ShimmerBox(
                modifier = Modifier.size(width = 30.dp, height = 12.dp),
                shape = ShapeS
            )
        }
    }
}

@Composable
fun ContinueWatchingCardSkeleton(
    modifier: Modifier = Modifier
) {
    val scaleMode = LocalCardScaleMode.current
    val cardWidth = CardDimensions.horizontalWidth(scaleMode)
    val cardHeight = CardDimensions.horizontalHeight(scaleMode)

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(ShapeM)
            .border(HairlineWidth, Outline, ShapeM)
            .shimmerEffect()
    )
}

/**
 * Full-screen luxury obsidian skeleton wireframe for Home Screen.
 * Exact 1:1 match to the loaded HomeScreen layout.
 */
@Composable
fun HomeScreenSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideShimmerScope {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val carouselHeight = if (isLandscape) {
            (configuration.screenHeightDp * 0.85f).coerceIn(280f, 560f).dp
        } else {
            (configuration.screenHeightDp * 0.60f).coerceIn(440f, 560f).dp
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
        ) {
        // 1. Cinematic Hero Carousel Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight)
                .shimmerEffect()
        ) {
            // Dark vignette bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Background.copy(alpha = 0.6f),
                                Background
                            ),
                            startY = 200f
                        )
                    )
            )

            // Hero content placeholder - Left-aligned 1:1 match to HeroCarousel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Dimens.spacing16)
                    .padding(bottom = Dimens.spacing24),
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                ShimmerBox(
                    modifier = Modifier
                        .width(220.dp)
                        .height(34.dp),
                    shape = ShapeS
                )
                Spacer(modifier = Modifier.height(Dimens.spacing8))
                // Metadata pills
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                    ShimmerBox(
                        modifier = Modifier.size(width = 54.dp, height = 18.dp),
                        shape = ShapeS
                    )
                    ShimmerBox(
                        modifier = Modifier.size(width = 46.dp, height = 18.dp),
                        shape = ShapeS
                    )
                    ShimmerBox(
                        modifier = Modifier.size(width = 48.dp, height = 18.dp),
                        shape = ShapeS
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spacing16))
                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(Dimens.buttonHeight),
                        shape = ShapeFull
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(110.dp)
                            .height(Dimens.buttonHeight),
                        shape = ShapeFull
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacing8))

        // 1.5 Category Filter Tabs Skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing8),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
        ) {
            listOf(52.dp, 70.dp, 82.dp, 66.dp, 78.dp).forEach { tabWidth ->
                ShimmerBox(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(Dimens.chipHeight),
                    shape = ShapeFull
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacing8))

        // 1.8 Continue Watching Row Skeleton
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(160.dp)
                    .height(20.dp),
                shape = ShapeS
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(2) {
                    ContinueWatchingCardSkeleton()
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Curated Rows Skeletons
        repeat(3) { rowIndex ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // Row title skeleton
                val titleWidth = when (rowIndex % 3) {
                    0 -> 180.dp
                    1 -> 220.dp
                    else -> 150.dp
                }
                ShimmerBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(titleWidth)
                        .height(20.dp),
                    shape = ShapeS
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal card row skeleton
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        MediaCardSkeleton()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
}

/**
 * Cinematic obsidian skeleton wireframe for Media Details Screen.
 * 1:1 match with loaded layout to eliminate CLS (Cumulative Layout Shift).
 */
@Composable
fun MediaDetailsSkeleton(
    modifier: Modifier = Modifier,
    backdropUrl: String? = null,
    title: String? = null,
    mediaId: String? = null
) {
    ProvideShimmerScope {
        val windowDims = rememberWindowDimensions()
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val heroHeight = (configuration.screenHeightDp * 0.38f).coerceIn(240f, 340f).dp

        if (windowDims.isTablet) {
            // Adaptive 2-Column Skeleton for Tablet / Large Foldables
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                // Left Column: Artwork, Header, Play, Overview
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .padding(Dimens.spacing24)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Backdrop Shimmer Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                            .clip(ShapeM)
                            .background(SurfaceLow)
                            .border(HairlineWidth, Outline, ShapeM)
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    // Title Skeleton (above metadata and play button)
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = AppTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    } else {
                        ShimmerBox(
                            modifier = Modifier
                                .width(220.dp)
                                .height(30.dp),
                            shape = ShapeS
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing8))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 18.dp), shape = ShapeS)
                        ShimmerBox(modifier = Modifier.size(width = 44.dp, height = 20.dp), shape = ShapeS)
                        ShimmerBox(modifier = Modifier.size(width = 34.dp, height = 20.dp), shape = ShapeS)
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.buttonHeight),
                        shape = ShapeFull
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacing8))

                    // Dual Ratings Skeleton (24dp pills)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .size(width = 68.dp, height = Dimens.spacing24),
                            shape = ShapeFull
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .size(width = 64.dp, height = Dimens.spacing24),
                            shape = ShapeFull
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing12))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ShimmerBox(
                                    modifier = Modifier.size(Dimens.minTouchTarget),
                                    shape = ShapeFull
                                )
                                Spacer(modifier = Modifier.height(Dimens.spacing4))
                                ShimmerBox(
                                    modifier = Modifier.size(width = 46.dp, height = 12.dp),
                                    shape = ShapeS
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp), shape = ShapeS)
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.92f).height(16.dp), shape = ShapeS)
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.68f).height(16.dp), shape = ShapeS)
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(modifier = Modifier.size(width = 42.dp, height = 16.dp), shape = ShapeS)
                }

                // Right Column: Prime Video Style Tabs Content
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .padding(top = Dimens.spacing24, end = Dimens.spacing24, bottom = Dimens.spacing24)
                ) {
                    MediaDetailsTabContentSkeleton()
                }
            }
        } else {
            // Single-Column Layout for Phone
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Backdrop Shimmer Box (slightly off the edges with subtle rounded corners)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Dimens.spacing16, top = Dimens.spacing12, end = Dimens.spacing16, bottom = Dimens.spacing4)
                        .height(heroHeight)
                        .clip(ShapeM)
                        .background(SurfaceLow)
                        .border(HairlineWidth, Outline, ShapeM)
                        .shimmerEffect()
                )

                    // 2. Metadata, Actions, and Overview Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing12)
                    ) {
                        // Title Skeleton (above metadata and play button)
                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                style = AppTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        } else {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(30.dp),
                                shape = ShapeS
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.spacing8))

                        // Metadata chips (MetaLine + Content Rating + Quality)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 18.dp), shape = ShapeS)
                            ShimmerBox(modifier = Modifier.size(width = 44.dp, height = 20.dp), shape = ShapeS)
                            ShimmerBox(modifier = Modifier.size(width = 34.dp, height = 20.dp), shape = ShapeS)
                        }

                        Spacer(modifier = Modifier.height(Dimens.spacing16))

                        // Primary Play Button (Full Width, 48dp)
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.buttonHeight),
                            shape = ShapeFull
                        )

                        Spacer(modifier = Modifier.height(Dimens.spacing8))

                        // Dual Ratings Skeleton (24dp pills)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .size(width = 68.dp, height = Dimens.spacing24),
                                shape = ShapeFull
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .size(width = 64.dp, height = Dimens.spacing24),
                                shape = ShapeFull
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.spacing12))

                    // Action Buttons Row: 4 circular 48dp icon buttons with labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ShimmerBox(
                                    modifier = Modifier.size(Dimens.minTouchTarget),
                                    shape = ShapeFull
                                )
                                Spacer(modifier = Modifier.height(Dimens.spacing4))
                                ShimmerBox(
                                    modifier = Modifier.size(width = 46.dp, height = 12.dp),
                                    shape = ShapeS
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    // Synopsis 3 lines + "More"
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                        shape = ShapeS
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(16.dp),
                        shape = ShapeS
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(16.dp),
                        shape = ShapeS
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    ShimmerBox(
                        modifier = Modifier.size(width = 42.dp, height = 16.dp),
                        shape = ShapeS
                    )
                }

                // 3. Tab row and episode content
                MediaDetailsTabContentSkeleton()

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MediaDetailsTabContentSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Tab Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing16),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Dimens.spacing12)
            ) {
                ShimmerBox(modifier = Modifier.size(width = 64.dp, height = 18.dp), shape = ShapeS)
                Spacer(modifier = Modifier.height(Dimens.spacing12))
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Outline))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Dimens.spacing12)
            ) {
                ShimmerBox(modifier = Modifier.size(width = 84.dp, height = 18.dp), shape = ShapeS)
                Spacer(modifier = Modifier.height(Dimens.spacing12))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Dimens.spacing12)
            ) {
                ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 18.dp), shape = ShapeS)
                Spacer(modifier = Modifier.height(Dimens.spacing12))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HairlineWidth)
                .background(Outline)
        )

        Spacer(modifier = Modifier.height(Dimens.spacing16))

        // Season selector pill skeleton
        Box(modifier = Modifier.padding(horizontal = Dimens.spacing16)) {
            ShimmerBox(
                modifier = Modifier.size(width = 104.dp, height = 36.dp),
                shape = ShapeFull
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacing16))

        // 3 Episode item skeletons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(Dimens.backdropAspectRatio),
                        shape = ShapeM
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing12))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp),
                            shape = ShapeS
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacing4))
                        ShimmerBox(
                            modifier = Modifier
                                .width(50.dp)
                                .height(12.dp),
                            shape = ShapeS
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacing4))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(12.dp),
                            shape = ShapeS
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    ShimmerBox(
                        modifier = Modifier.size(24.dp),
                        shape = ShapeFull
                    )
                }
            }
        }
    }
}

/**
 * Grid Skeleton for Search Screen matching card scale 1:1.
 */
@Composable
fun SearchGridSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideShimmerScope {
        val windowDimensions = rememberWindowDimensions()
        val gridColumns = when (windowDimensions.widthClass) {
            WindowWidthClass.Expanded -> 6
            WindowWidthClass.Medium -> 5
            WindowWidthClass.Compact -> 3
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.posterGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12),
            modifier = modifier.fillMaxSize()
        ) {
            items(gridColumns * 4, key = { it }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(ShapeS)
                            .background(SurfaceLow)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(13.dp),
                        shape = ShapeS
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4)) {
                        ShimmerBox(
                            modifier = Modifier.size(width = 34.dp, height = 11.dp),
                            shape = ShapeS
                        )
                        ShimmerBox(
                            modifier = Modifier.size(width = 28.dp, height = 11.dp),
                            shape = ShapeS
                        )
                    }
                }
            }
        }
    }
}

/**
 * Option cards skeleton for Download Quality Bottom Sheet.
 */
@Composable
fun DownloadSheetSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideShimmerScope {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(ShapeM)
                        .border(HairlineWidth, Outline.copy(alpha = 0.18f), ShapeM)
                        .shimmerEffect()
                )
            }
        }
    }
}
