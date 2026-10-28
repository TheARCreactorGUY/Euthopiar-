package com.euthopiar.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.euthopiar.core.model.MediaItem
import com.euthopiar.data.preferences.SectionLabelStyle
import com.euthopiar.data.preferences.Top10Style
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val posterTintCache = ConcurrentHashMap<String, Color>()

@Composable
fun rememberPosterTint(
    itemId: String,
    imageUrl: String?,
    fallbackColor: Color = Accent
): Color {
    val context = LocalContext.current
    var tintColor by remember(itemId) {
        mutableStateOf(posterTintCache[itemId] ?: fallbackColor)
    }

    LaunchedEffect(itemId, imageUrl) {
        if (posterTintCache.containsKey(itemId)) {
            tintColor = posterTintCache[itemId] ?: fallbackColor
            return@LaunchedEffect
        }
        if (imageUrl.isNullOrBlank()) {
            tintColor = fallbackColor
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(100, 150)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val rawBitmap = result.image.toBitmap()
                    val bitmap = if (rawBitmap.config == Bitmap.Config.HARDWARE) {
                        rawBitmap.copy(Bitmap.Config.ARGB_8888, false)
                    } else {
                        rawBitmap
                    }
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.lightMutedSwatch
                        ?: palette.darkMutedSwatch
                        ?: palette.dominantSwatch

                    val extracted = if (swatch != null) {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(swatch.rgb, hsl)
                        // Guarantee sufficient lightness on dark OLED background
                        if (hsl[2] < 0.45f) {
                            hsl[2] = 0.55f
                        }
                        // Guarantee minimum saturation so the numeral looks intentionally colored
                        if (hsl[1] < 0.35f) {
                            hsl[1] = (hsl[1] + 0.35f).coerceAtMost(0.95f)
                        }
                        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                    } else {
                        fallbackColor
                    }
                    posterTintCache[itemId] = extracted
                    withContext(Dispatchers.Main) {
                        tintColor = extracted
                    }
                }
            } catch (_: Throwable) {
                posterTintCache[itemId] = fallbackColor
                withContext(Dispatchers.Main) {
                    tintColor = fallbackColor
                }
            }
        }
    }

    val animatedTint by animateColorAsState(
        targetValue = tintColor,
        animationSpec = tween(durationMillis = 240, easing = com.euthopiar.ui.theme.AppleDecelCurve),
        label = "posterTint_$itemId"
    )

    return animatedTint
}

/**
 * Modern Netflix / Apple TV style "Top 10 Today" Row.
 * Supports PosterTinted and GlassTab treatments with zero layout shift.
 */
@Composable
fun TopTenRow(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Top 10 today",
    style: Top10Style = Top10Style.PosterTinted,
    sectionLabelStyle: SectionLabelStyle = SectionLabelStyle.AccentBar,
    subtitle: String? = null,
    eyebrow: String? = null,
    onItemLongClick: ((MediaItem) -> Unit)? = null
) {
    if (items.isEmpty()) return
    val topItems = items.take(10)
    val windowDimensions = rememberWindowDimensions()
    val topTenRowState = androidx.compose.runtime.saveable.rememberSaveable(
        "top_10_row_$title",
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacing8)
    ) {
        SectionHeader(
            title = title,
            style = sectionLabelStyle,
            subtitle = subtitle,
            eyebrow = eyebrow
        )

        LazyRow(
            state = topTenRowState,
            contentPadding = PaddingValues(horizontal = windowDimensions.horizontalMargin),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing16),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = topItems,
                key = { index, item -> "top10_${index}_${item.id}_${item.provider ?: ""}" }
            ) { index, item ->
                when (style) {
                    Top10Style.PosterTinted -> {
                        TopTenRankCardPosterTinted(
                            rank = index + 1,
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = onItemLongClick?.let { { it(item) } }
                        )
                    }
                    Top10Style.GlassTab -> {
                        TopTenRankCardGlassTab(
                            rank = index + 1,
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = onItemLongClick?.let { { it(item) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTenRankCardPosterTinted(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val scaleMode = LocalCardScaleMode.current
    val posterWidth = CardDimensions.verticalWidth(scaleMode)
    val posterHeight = posterWidth / Dimens.posterAspectRatio

    val isTwoDigit = rank >= 10
    val tabPeekWidth = if (isTwoDigit) 90.dp else 58.dp
    val totalItemWidth = posterWidth + tabPeekWidth

    val tintColor = rememberPosterTint(
        itemId = item.id,
        imageUrl = item.posterUrl ?: item.backdropUrl,
        fallbackColor = Accent
    )

    val glowBrush = remember(tintColor) {
        Brush.radialGradient(
            colors = listOf(
                tintColor.copy(alpha = 0.28f),
                Color.Transparent
            )
        )
    }

    val numberBrush = remember(tintColor) {
        Brush.verticalGradient(
            colors = listOf(
                tintColor.copy(alpha = 0.95f),
                tintColor.copy(alpha = 0.04f)
            )
        )
    }

    Box(
        modifier = Modifier
            .width(totalItemWidth)
            .height(posterHeight)
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = "Rank $rank, ${item.title}"
            }
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = ShapeS)
    ) {
        // LAYER 1: GLOW (Radial gradient centered behind numeral, radius ~130dp, 28% alpha -> 0%, BlendMode.Plus)
        val glowXOffset = if (isTwoDigit) (-18).dp else (-30).dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(260.dp)
                .offset(x = glowXOffset)
                .zIndex(0f)
                .drawBehind {
                    drawCircle(
                        brush = glowBrush,
                        radius = size.minDimension / 2f,
                        center = center,
                        blendMode = BlendMode.Plus
                    )
                }
        )

        // LAYER 2: RANK NUMBER (150sp full flagship size, bold/black font weight, vertical brush 95% -> 4%, right 25-30% covered by card)
        val rankFontSize = 150.sp
        val rankLetterSpacing = if (isTwoDigit) (-0.06).em else (-0.04).em
        val rankXOffset = if (isTwoDigit) (-8).dp else (-4).dp

        Text(
            text = rank.toString(),
            style = AppTypography.numeralDisplay.copy(
                fontSize = rankFontSize,
                lineHeight = rankFontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = rankLetterSpacing,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                brush = numberBrush
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = rankXOffset)
                .clearAndSetSemantics { }
                .zIndex(1f)
        )

        // LAYER 3: POSTER CARD (Overlaps the right side of the numeral cleanly)
        Box(
            modifier = Modifier
                .width(posterWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .zIndex(2f)
                .clip(ShapeS)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), ShapeS)
        ) {
            AppAsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                titleForFallback = item.title,
                modifier = Modifier.fillMaxSize()
            )

            ReleaseStatusBadge(
                status = item.releaseStatus,
                releaseDate = item.releaseDate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
            )
        }
    }
}

@Composable
private fun TopTenRankCardGlassTab(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val scaleMode = LocalCardScaleMode.current
    val posterWidth = CardDimensions.verticalWidth(scaleMode)
    val posterHeight = posterWidth / Dimens.posterAspectRatio

    val isTwoDigit = rank >= 10
    val tabPeekWidth = if (isTwoDigit) 82.dp else 58.dp
    val totalItemWidth = posterWidth + tabPeekWidth
    val glassTabWidth = if (isTwoDigit) 108.dp else 84.dp

    val glassGlowBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Accent.copy(alpha = 0.55f),
                Color.Transparent
            )
        )
    }

    val glassBorderBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    Box(
        modifier = Modifier
            .width(totalItemWidth)
            .height(posterHeight)
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = "Rank $rank, ${item.title}"
            }
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = ShapeS)
    ) {
        // LAYER 1: GLOW (Subtle radial gradient behind tab using Accent, radius ~90dp, alpha ~55% fading to 0%)
        val glowXOffset = if (isTwoDigit) 0.dp else (-10).dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(180.dp)
                .offset(x = glowXOffset)
                .zIndex(0f)
                .drawBehind {
                    drawCircle(
                        brush = glassGlowBrush,
                        radius = size.minDimension / 2f,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                }
        )

        // LAYER 2: GLASS TAB + RANK NUMBER (84dp/108dp width, 14dp radius, white 10% fill, 1dp 25% -> 8% border, 72sp bold 92% white)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(glassTabWidth)
                .fillMaxHeight()
                .zIndex(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    brush = glassBorderBrush,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val glassFontSize = 72.sp
            val glassLetterSpacing = if (isTwoDigit) (-0.04).em else 0.em
            val glassXOffset = if (isTwoDigit) (-14).dp else (-12).dp

            Text(
                text = rank.toString(),
                style = AppTypography.numeralDisplay.copy(
                    fontSize = glassFontSize,
                    lineHeight = glassFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = glassLetterSpacing,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    color = Color.White.copy(alpha = 0.92f)
                ),
                modifier = Modifier
                    .offset(x = glassXOffset)
                    .clearAndSetSemantics { }
            )
        }

        // LAYER 3: POSTER CARD (Overlaps the right 26dp of the 84dp tab)
        Box(
            modifier = Modifier
                .width(posterWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .zIndex(2f)
                .clip(ShapeS)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), ShapeS)
        ) {
            AppAsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                titleForFallback = item.title,
                modifier = Modifier.fillMaxSize()
            )

            ReleaseStatusBadge(
                status = item.releaseStatus,
                releaseDate = item.releaseDate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
            )
        }
    }
}

/**
 * Legacy Top 10 Rank Card preserved unused as requested.
 */
@Composable
@Suppress("unused")
private fun LegacyTopTenRankCard(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val scaleMode = LocalCardScaleMode.current
    val posterWidth = CardDimensions.verticalWidth(scaleMode)
    val posterHeight = posterWidth / Dimens.posterAspectRatio

    // Numeral height enlarged by 50% (from 40% to 60% of poster height) across all card settings
    val numeralHeight = posterHeight * 0.60f
    val numeralFontSize = (posterHeight.value * 0.60f).sp

    // Proportional visible width peeking out from behind the cards (~75% visible, ~25% tucked behind)
    val numeralVisibleWidth = when {
        rank >= 10 -> numeralHeight * 0.82f
        rank == 1 -> numeralHeight * 0.35f
        else -> numeralHeight * 0.48f
    }
    val totalCardWidth = posterWidth + numeralVisibleWidth

    val strokeWidthPx = (1.8f * scaleMode.verticalFactor).coerceIn(1.5f, 3.0f)
    val shadowStrokeWidthPx = strokeWidthPx * 2.2f

    // 5-degree forward italic slant: tan(5°) ≈ 0.08748866f
    val italicSkewFactor = -0.08749f

    Box(
        modifier = Modifier
            .width(totalCardWidth)
            .height(posterHeight)
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = "Rank $rank, ${item.title}"
            }
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = ShapeS)
    ) {
        // LAYER 1: NUMERAL (Rendered behind the poster, anchored to bottom-left with 5° italic slant)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(0f)
                .drawWithContent {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        nativeCanvas.save()
                        nativeCanvas.translate(0f, size.height)
                        nativeCanvas.skew(italicSkewFactor, 0f)
                        nativeCanvas.translate(0f, -size.height)
                        drawContent()
                        nativeCanvas.restore()
                    }
                }
        ) {
            // Shadow / dark outer stroke for high contrast against any backdrop
            Text(
                text = rank.toString(),
                style = AppTypography.numeralDisplay.copy(
                    fontSize = numeralFontSize,
                    lineHeight = numeralFontSize,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    drawStyle = Stroke(width = shadowStrokeWidthPx, join = StrokeJoin.Round),
                    color = Background
                )
            )

            // Crisp silver / metallic hairline outline
            Text(
                text = rank.toString(),
                style = AppTypography.numeralDisplay.copy(
                    fontSize = numeralFontSize,
                    lineHeight = numeralFontSize,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    drawStyle = Stroke(width = strokeWidthPx, join = StrokeJoin.Round),
                    color = TextPrimary.copy(alpha = 0.85f)
                )
            )

            // Velvet dark charcoal interior fill
            Text(
                text = rank.toString(),
                style = AppTypography.numeralDisplay.copy(
                    fontSize = numeralFontSize,
                    lineHeight = numeralFontSize,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    color = SurfaceLow
                )
            )
        }

        // LAYER 2: 2:3 POSTER CARD (Rendered on top of the numeral's right side)
        Box(
            modifier = Modifier
                .width(posterWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .zIndex(1f)
                .clip(ShapeS)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), ShapeS)
        ) {
            AppAsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = null, // decorative - merged in parent semantics
                contentScale = ContentScale.Crop,
                titleForFallback = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
