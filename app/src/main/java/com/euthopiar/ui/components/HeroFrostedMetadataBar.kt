package com.euthopiar.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.R
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.RottenTomatoesFresh
import com.euthopiar.ui.theme.RottenTomatoesRotten
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private data class HeroChipData(
    val iconRes: Int,
    val text: String,
    val contentDescription: String?,
    val iconTint: Color = Color.White.copy(alpha = 0.90f),
    val textWeight: FontWeight = FontWeight.Medium,
    val isFullColorBadge: Boolean = false
)

/**
 * Hero Metadata Chips matching Part 3 item 3:
 *
 * Directly under the logo (about 12 px gap), a wrapping row of small glass chips:
 * - 26 px tall, 7 px radius, 12 sp, 6 px gaps, outline icons at 13 px.
 * - Fill: white at 7% over dark background.
 * - Specular border: 0.5 px gradient, white at 30% -> 6%.
 * - Low-end fallback: 0xFF14151F at 94% opacity.
 * - Chips only appear when real data is present (missing fields are hidden, no blanks or fake defaults).
 * - Background layer shares a unified Haze blur pass to eliminate multi-node sampling overhead.
 */
@Composable
fun HeroMetadataChips(
    year: Int?,
    type: MediaType?,
    quality: String?,
    contentRating: String?,
    imdbRating: String?,
    rottenTomatoesRating: String?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val items = mutableListOf<HeroChipData>()

    // 1. Release Year
    year?.let { y ->
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_year_calendar,
                text = "$y",
                contentDescription = "Release year $y"
            )
        )
    }

    // 2. Content Type
    type?.let { t ->
        val (iconRes, label) = when (t) {
            MediaType.MOVIE -> Pair(R.drawable.ic_type_movie_clapperboard, "Movie")
            MediaType.TV_SERIES -> Pair(R.drawable.ic_type_tv_monitor, "Series")
            MediaType.ANIME -> Pair(R.drawable.ic_type_tv_monitor, "Anime")
            else -> Pair(R.drawable.ic_type_movie_clapperboard, t.name.replace("_", " "))
        }
        items.add(
            HeroChipData(
                iconRes = iconRes,
                text = label,
                contentDescription = label
            )
        )
    }

    // 3. Age / Content Rating (e.g. TV-14, PG-13, R) - only show when real data is present
    if (!contentRating.isNullOrBlank()) {
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_age_rating_shield,
                text = contentRating.trim(),
                contentDescription = "Content rating $contentRating",
                textWeight = FontWeight.SemiBold
            )
        )
    }

    // 4. IMDb Rating Score ("x.x/10" with official IMDb badge) - only show when real data is present
    val formattedImdbScore = formatImdbScoreValue(imdbRating)
    if (formattedImdbScore != null) {
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_imdb_logo_badge,
                text = "$formattedImdbScore/10",
                contentDescription = "IMDb rating $formattedImdbScore out of 10",
                textWeight = FontWeight.SemiBold,
                isFullColorBadge = true
            )
        )
    }

    // 5. Critic Score (Rotten Tomatoes) - only show when real data is present
    if (!rottenTomatoesRating.isNullOrBlank()) {
        val trimmedRt = rottenTomatoesRating.trim()
        val cleanRt = if (trimmedRt.endsWith("%")) trimmedRt else "$trimmedRt%"
        val scoreInt = trimmedRt.replace("%", "").toIntOrNull() ?: 0
        val rtColor = if (scoreInt >= 60) RottenTomatoesFresh else RottenTomatoesRotten
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_rating_tomato,
                text = cleanRt,
                contentDescription = "Rotten Tomatoes score $cleanRt",
                iconTint = rtColor,
                textWeight = FontWeight.SemiBold
            )
        )
    }

    if (items.isEmpty()) return

    HeroMetadataChipsLayout(
        chips = items,
        hazeState = hazeState,
        modifier = modifier
    )
}

@Composable
private fun HeroMetadataChipsLayout(
    chips: List<HeroChipData>,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val isAmoled = LocalAmoledMode.current
    val anchorColor = if (isAmoled) Color.Black else Color(0xFF000000)
    val fallbackColor = if (isAmoled) Color.Black else Color(0xFF14151F).copy(alpha = 0.94f)
    val anchorAlpha = 0.38f
    val whiteLiftAlpha = 0.06f

    var chipRects by remember { mutableStateOf<List<Rect>>(emptyList()) }
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 8.dp.toPx() }

    val chipMaskShape = remember(chipRects, cornerRadiusPx) {
        if (chipRects.isEmpty()) {
            RectangleShape
        } else {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val path = Path()
                    for (r in chipRects) {
                        path.addRoundRect(
                            RoundRect(
                                left = r.left,
                                top = r.top,
                                right = r.right,
                                bottom = r.bottom,
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }
                    return Outline.Generic(path)
                }
            }
        }
    }

    val isUsingHaze = hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier) {
        // Unified Frosted Glass Blur Background Layer
        // Shares a SINGLE Haze blur pass across all chips, eliminating multi-node sampling overhead!
        if (isUsingHaze && chipRects.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(chipMaskShape)
                    .hazeEffect(state = hazeState!!) {
                        blurRadius = 18.dp
                        tints = listOf(
                            HazeTint(anchorColor.copy(alpha = anchorAlpha)),
                            HazeTint(Color.White.copy(alpha = whiteLiftAlpha))
                        )
                        fallbackTint = HazeTint(fallbackColor)
                    }
            )
        }

        // Chip Items Layout (Flow-style wrapping)
        Layout(
            content = {
                chips.forEach { chip ->
                    HeroGlassChip(
                        icon = painterResource(chip.iconRes),
                        text = chip.text,
                        contentDescription = chip.contentDescription,
                        iconTint = chip.iconTint,
                        textWeight = chip.textWeight,
                        isFullColorBadge = chip.isFullColorBadge,
                        isUsingUnifiedHaze = isUsingHaze && chipRects.isNotEmpty(),
                        anchorColor = anchorColor,
                        fallbackColor = fallbackColor,
                        anchorAlpha = anchorAlpha,
                        whiteLiftAlpha = whiteLiftAlpha
                    )
                }
            }
        ) { measurables, constraints ->
            val spacingPx = with(density) { 6.dp.roundToPx() }
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

            val rects = ArrayList<Rect>(placeables.size)
            var currentX = 0
            var currentY = 0
            var rowHeight = 0
            var maxWidth = 0

            val placements = ArrayList<Triple<Placeable, Int, Int>>(placeables.size)

            for (placeable in placeables) {
                if (currentX + placeable.width > constraints.maxWidth && currentX > 0) {
                    maxWidth = maxOf(maxWidth, currentX - spacingPx)
                    currentX = 0
                    currentY += rowHeight + spacingPx
                    rowHeight = 0
                }
                placements.add(Triple(placeable, currentX, currentY))
                rects.add(
                    Rect(
                        currentX.toFloat(),
                        currentY.toFloat(),
                        (currentX + placeable.width).toFloat(),
                        (currentY + placeable.height).toFloat()
                    )
                )
                rowHeight = maxOf(rowHeight, placeable.height)
                currentX += placeable.width + spacingPx
            }
            maxWidth = maxOf(maxWidth, if (currentX > 0) currentX - spacingPx else 0)
            val totalHeight = if (placeables.isNotEmpty()) currentY + rowHeight else 0

            if (rects != chipRects) {
                chipRects = rects
            }

            layout(maxWidth, totalHeight) {
                placements.forEach { (placeable, x, y) ->
                    placeable.placeRelative(x, y)
                }
            }
        }
    }
}

/**
 * Individual small glass chip (26 px tall, 7 px radius, 12 sp, 0.5 px specular border).
 * Anchored with 38% black tint + 6% white lift, 18 px background blur.
 * Meets WCAG AAA 7:1+ contrast against 100% white text.
 */
@Composable
private fun HeroGlassChip(
    icon: Painter,
    text: String,
    contentDescription: String? = null,
    iconTint: Color = Color.White.copy(alpha = 0.90f),
    textWeight: FontWeight = FontWeight.Medium,
    isFullColorBadge: Boolean = false,
    isUsingUnifiedHaze: Boolean = false,
    anchorColor: Color = Color.Black,
    fallbackColor: Color = Color(0xFF14151F).copy(alpha = 0.94f),
    anchorAlpha: Float = 0.38f,
    whiteLiftAlpha: Float = 0.06f,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(7.dp)

    val specularBorderBrush = remember {
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.30f),
            0.35f to Color.White.copy(alpha = 0.12f),
            0.65f to Color.White.copy(alpha = 0.06f),
            1.00f to Color.White.copy(alpha = 0.14f)
        )
    }

    val chipBgModifier = if (isUsingUnifiedHaze) {
        Modifier // Handled by unified background layer
    } else {
        Modifier
            .background(fallbackColor, chipShape)
            .background(Color.White.copy(alpha = whiteLiftAlpha), chipShape)
    }

    Box(
        modifier = modifier
            .heightIn(min = 26.dp)
            .clip(chipShape)
            .then(chipBgModifier)
            .border(
                width = 0.5.dp,
                brush = specularBorderBrush,
                shape = chipShape
            )
            .drawBehind {
                // Subtle 0.5dp top inner highlight white at 12%
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(4.dp.toPx(), 0.5.dp.toPx()),
                    end = Offset(size.width - 4.dp.toPx(), 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .padding(horizontal = 8.5.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isFullColorBadge) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(13.dp)
                        .aspectRatio(256f / 129f)
                        .clip(RoundedCornerShape(2.dp))
                )
            } else {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                fontFamily = AppFontFamily,
                fontSize = 12.sp,
                fontWeight = textWeight,
                letterSpacing = (-0.01).em,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Backward-compatible entrypoint for HeroFrostedMetadataBar.
 * Renders the wrapping row of small glass chips.
 */
@Composable
fun HeroFrostedMetadataBar(
    year: Int?,
    type: MediaType?,
    quality: String?,
    contentRating: String?,
    imdbRating: String?,
    rottenTomatoesRating: String?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    HeroMetadataChips(
        year = year,
        type = type,
        quality = quality,
        contentRating = contentRating,
        imdbRating = imdbRating,
        rottenTomatoesRating = rottenTomatoesRating,
        modifier = modifier,
        hazeState = hazeState
    )
}
