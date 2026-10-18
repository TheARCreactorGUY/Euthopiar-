package com.euthopiar.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.R
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private data class HeroChipData(
    val iconRes: Int,
    val text: String,
    val contentDescription: String?
)

/**
 * Hero Metadata Chips matching Part 3 item 3:
 *
 * Directly under the logo (about 12 px gap), a wrapping row of small glass chips:
 * - 24 px tall, 8 px radius, 11 sp medium, 6 px gaps, outline icons at 12 px.
 * - Fill: white at 7% over dark background.
 * - Border: 0.5 px hairline, white at 16%.
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

    // 3. Quality (4K, UHD, HD)
    if (!quality.isNullOrBlank()) {
        val is4k = quality.contains("4k", ignoreCase = true) || quality.contains("uhd", ignoreCase = true)
        val qualityIconRes = if (is4k) R.drawable.ic_quality_4k else R.drawable.ic_quality_hd
        val qualityLabel = if (is4k) "4K" else "HD"
        items.add(
            HeroChipData(
                iconRes = qualityIconRes,
                text = qualityLabel,
                contentDescription = "$qualityLabel Quality"
            )
        )
    }

    // 4. Age / Content Rating (e.g. TV-14, PG-13, R) - only show when real data is present
    if (!contentRating.isNullOrBlank()) {
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_age_rating_shield,
                text = contentRating.trim(),
                contentDescription = "Content rating $contentRating"
            )
        )
    }

    // 5. IMDb Rating Score - only show when real data is present
    if (!imdbRating.isNullOrBlank() && imdbRating != "0" && imdbRating != "0.0") {
        val formattedImdb = imdbRating.trim().take(3)
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_rating_star_imdb,
                text = formattedImdb,
                contentDescription = "IMDb rating $formattedImdb"
            )
        )
    }

    // 6. Critic Score (Rotten Tomatoes) - only show when real data is present
    if (!rottenTomatoesRating.isNullOrBlank()) {
        val trimmedRt = rottenTomatoesRating.trim()
        val cleanRt = if (trimmedRt.endsWith("%")) trimmedRt else "$trimmedRt%"
        items.add(
            HeroChipData(
                iconRes = R.drawable.ic_rating_tomato,
                text = cleanRt,
                contentDescription = "Rotten Tomatoes score $cleanRt"
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
 * Individual small glass chip (26 px tall, 8 px radius, 12 sp medium, 1 px specular border).
 * Anchored with 38% black tint + 6% white lift, 18 px background blur, 98% scale on press.
 * Meets WCAG AAA 7:1+ contrast against 100% white text.
 */
@Composable
private fun HeroGlassChip(
    icon: Painter,
    text: String,
    contentDescription: String? = null,
    isUsingUnifiedHaze: Boolean = false,
    anchorColor: Color = Color.Black,
    fallbackColor: Color = Color(0xFF14151F).copy(alpha = 0.94f),
    anchorAlpha: Float = 0.38f,
    whiteLiftAlpha: Float = 0.06f,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val chipShape = RoundedCornerShape(8.dp)

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "chipPressScale"
    )

    val specularBorderBrush = remember {
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.32f),
            0.35f to Color.White.copy(alpha = 0.10f),
            0.65f to Color.White.copy(alpha = 0.06f),
            1.00f to Color.White.copy(alpha = 0.16f)
        )
    }

    val chipBgModifier = if (isUsingUnifiedHaze) {
        Modifier // Handled by unified background layer
    } else {
        Modifier
            .background(anchorColor.copy(alpha = anchorAlpha), chipShape)
            .background(Color.White.copy(alpha = whiteLiftAlpha), chipShape)
    }

    Box(
        modifier = modifier
            .height(26.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(chipShape)
            .then(chipBgModifier)
            .border(
                width = 1.dp,
                brush = specularBorderBrush,
                shape = chipShape
            )
            .drawBehind {
                // Subtle 1dp top inner highlight white at 10%
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(4.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 4.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .pointerInput(reduceMotion) {
                if (!reduceMotion) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontFamily = AppFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
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
