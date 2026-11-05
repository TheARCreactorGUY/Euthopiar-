package com.euthopiar.ui.search

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.euthopiar.ui.icons.LucideIcons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.playButtonSpringPress
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

object SearchTokens {
    val BaseBackground = Color(0xFF0B0B0F)
    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFallbackFill = Color(0xFF14151F).copy(alpha = 0.94f)
    val GlassBorder = Color.White.copy(alpha = 0.16f)
    val GlassBorderActive = Color.White.copy(alpha = 0.32f)
    val HairlineColor = Color.White.copy(alpha = 0.14f)
    val HairlineThickness = 0.5.dp

    val TextWhite = Color.White
    val TextMuted = Color.White.copy(alpha = 0.55f)
    val TextLabel = Color.White.copy(alpha = 0.45f)
    val DarkText = Color(0xFF0B0B0F)
    val RatingGold = Color(0xFFFFB800)

    val HazeBlue = Color(0xFF2E3D64)
    val HazeRose = Color(0xFF6B2F48)

    val DropdownRadius = 10.dp
    val SearchBarRadius = 14.dp
    val BackButtonRadius = 14.dp
    val PopoverRadius = 14.dp

    val LargeHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.50).sp,
        color = Color.White
    )

    val QueryHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.40).sp,
        color = Color.White
    )

    val SectionLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.sp,
        color = TextLabel
    )

    val ItemTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.White
    )

    val BestMatchTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        color = Color.White
    )

    val ItemMetadata = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        color = TextMuted
    )
}

/**
 * Ambient Haze Background with 2-3 soft color blobs drifting slowly in the background.
 * Automatically bypassed in AMOLED mode to maintain 100% pitch black `#000000` pixels.
 */
@Composable
fun SearchAmbientHazeBackground(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = com.euthopiar.ui.theme.LocalAmoledMode.current

    val infiniteTransition = rememberInfiniteTransition(label = "ambientHaze")

    val driftOffset1 = infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )

    val driftOffset2 = infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    val baseBg = if (isAmoled) Color.Black else SearchTokens.BaseBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                if (!isAmoled) {
                    val effectiveDrift1 = if (reduceMotion) 0f else driftOffset1.value
                    val effectiveDrift2 = if (reduceMotion) 0f else driftOffset2.value
                    val maxDim = maxOf(size.width, size.height)

                    // Soft Indigo/Blue Haze atmospheric wash near Top-Right
                    drawRect(
                        brush = Brush.radialGradient(
                            0.00f to SearchTokens.HazeBlue.copy(alpha = 0.24f),
                            0.25f to SearchTokens.HazeBlue.copy(alpha = 0.15f),
                            0.50f to SearchTokens.HazeBlue.copy(alpha = 0.06f),
                            0.75f to SearchTokens.HazeBlue.copy(alpha = 0.015f),
                            1.00f to Color.Transparent,
                            center = Offset(size.width * 0.95f + effectiveDrift1, size.height * 0.10f + effectiveDrift2),
                            radius = maxDim * 1.15f
                        )
                    )

                    // Soft Rose/Pink Haze atmospheric wash near Lower-Left
                    drawRect(
                        brush = Brush.radialGradient(
                            0.00f to SearchTokens.HazeRose.copy(alpha = 0.20f),
                            0.25f to SearchTokens.HazeRose.copy(alpha = 0.12f),
                            0.50f to SearchTokens.HazeRose.copy(alpha = 0.05f),
                            0.75f to SearchTokens.HazeRose.copy(alpha = 0.01f),
                            1.00f to Color.Transparent,
                            center = Offset(size.width * 0.05f + effectiveDrift2, size.height * 0.85f + effectiveDrift1),
                            radius = maxDim * 1.05f
                        )
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Standard glass surface modifier with top-lit vertical gradient hairline border.
 */
fun Modifier.searchGlassSurface(
    shape: Shape = RoundedCornerShape(SearchTokens.SearchBarRadius),
    hazeState: HazeState? = null,
    isActive: Boolean = false,
    hasGlow: Boolean = false
): Modifier {
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isActive) 0.40f else 0.24f),
            Color.White.copy(alpha = if (isActive) 0.18f else 0.08f)
        )
    )

    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = 18.dp
                    tints = listOf(HazeTint(SearchTokens.GlassFill))
                    fallbackTint = HazeTint(SearchTokens.GlassFallbackFill)
                }
            } else {
                Modifier.background(SearchTokens.GlassFallbackFill, shape)
            }
        )
        .then(
            if (hasGlow) {
                Modifier.drawBehind {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            } else Modifier
        )
        .border(
            width = SearchTokens.HairlineThickness,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Hairline divider 0.5dp white 14% alpha across content width.
 */
@Composable
fun SearchHairlineDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = SearchTokens.HairlineThickness,
        color = SearchTokens.HairlineColor,
        modifier = modifier
    )
}

/**
 * Equal-width rounded glass dropdown box with anchored popover.
 * Supports start, center, and trailing inward anchors to prevent screen edge overflow.
 */
@Composable
fun SearchGlassDropdown(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    isTrailing: Boolean = false,
    isCenter: Boolean = false
) {
    val isFiltered = !selectedValue.isNullOrBlank() &&
            selectedValue != "All" &&
            selectedValue != "All types" &&
            selectedValue != "All genres" &&
            selectedValue != "Any year"
    val displayLabel = if (isFiltered) selectedValue!! else label
    val reduceMotion = LocalReduceMotion.current

    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "chevronRotation"
    )

    // Outer box has 48dp height for accessible tap target
    Box(
        modifier = modifier
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .searchGlassSurface(
                    shape = RoundedCornerShape(SearchTokens.DropdownRadius),
                    hazeState = hazeState,
                    isActive = isFiltered || isExpanded
                )
                .tvFocusable(shape = RoundedCornerShape(SearchTokens.DropdownRadius))
                .pressable(
                    role = Role.DropdownList,
                    onClickLabel = "$label filter, currently $displayLabel",
                    onClick = onToggleExpand
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = displayLabel,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "dropdownLabelCrossfade"
                ) { currentLabel ->
                    Text(
                        text = currentLabel,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = if (isFiltered) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isFiltered) Color.White else SearchTokens.TextMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isFiltered) Color.White else SearchTokens.TextMuted,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(chevronAngle)
                )
            }
        }

        var isPopupRendered by remember { mutableStateOf(false) }
        val popupAnim = remember { Animatable(0f) }

        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                isPopupRendered = true
                if (reduceMotion) {
                    popupAnim.snapTo(1f)
                } else {
                    popupAnim.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 460f))
                }
            } else if (isPopupRendered) {
                if (reduceMotion) {
                    popupAnim.snapTo(0f)
                    isPopupRendered = false
                } else {
                    popupAnim.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
                    isPopupRendered = false
                }
            }
        }

        if (isPopupRendered) {
            val density = LocalDensity.current
            val popoverScale = 0.94f + (0.06f * popupAnim.value)
            val popoverAlpha = popupAnim.value
            val popoverOffsetY = (1f - popupAnim.value) * (-6f) * density.density

            val popupAlignment = when {
                isTrailing -> Alignment.TopEnd
                isCenter -> Alignment.TopCenter
                else -> Alignment.TopStart
            }
            val origin = when {
                isTrailing -> TransformOrigin(1f, 0f)
                isCenter -> TransformOrigin(0.5f, 0f)
                else -> TransformOrigin(0f, 0f)
            }

            Popup(
                alignment = popupAlignment,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true)
            ) {
                val isAmoled = com.euthopiar.ui.theme.LocalAmoledMode.current
                val popoverShape = RoundedCornerShape(SearchTokens.PopoverRadius)
                val glassFill = if (isAmoled) Color(0xFF07070A) else Color(0xFF141522).copy(alpha = 0.96f)
                val borderBrush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )

                Box(
                    modifier = Modifier
                        .widthIn(min = 150.dp, max = 220.dp)
                        .heightIn(max = 280.dp)
                        .graphicsLayer {
                            scaleX = popoverScale
                            scaleY = popoverScale
                            alpha = popoverAlpha
                            translationY = popoverOffsetY
                            transformOrigin = origin
                        }
                        .clip(popoverShape)
                        .background(glassFill)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = SearchTokens.HairlineThickness,
                            brush = borderBrush,
                            shape = popoverShape
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        options.forEachIndexed { index, option ->
                            val isChosen = if (selectedValue == null) {
                                option == "All" || option == "All types" || option == "All genres" || option == "Any year"
                            } else {
                                option.equals(selectedValue, ignoreCase = true)
                            }

                            val rowAlpha by animateFloatAsState(
                                targetValue = if (isExpanded) 1f else 0f,
                                animationSpec = if (reduceMotion) tween(0) else tween(durationMillis = 140, delayMillis = if (index < 5) index * 15 else 80),
                                label = "rowAlpha_$index"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .graphicsLayer { alpha = rowAlpha }
                                    .tvFocusable(shape = RoundedCornerShape(8.dp))
                                    .pressable(
                                        role = Role.RadioButton,
                                        onClickLabel = "Select $option",
                                        onClick = {
                                            onSelect(option)
                                            onDismiss()
                                        }
                                    )
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = if (isChosen) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isChosen) Color.White else SearchTokens.TextMuted
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isChosen) {
                                    val checkScale by animateFloatAsState(
                                        targetValue = 1f,
                                        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.6f, stiffness = 500f),
                                        label = "checkScale"
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer {
                                                scaleX = checkScale
                                                scaleY = checkScale
                                            }
                                    )
                                }
                            }

                            if (index < options.lastIndex) {
                                SearchHairlineDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recent search vertical list row flush with the 20dp global rail.
 */
@Composable
fun RecentSearchRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .tvFocusable(shape = RoundedCornerShape(8.dp))
                .pressable(
                    role = Role.Button,
                    onClickLabel = "Search for $query",
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = SearchTokens.TextLabel,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = query,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .tvFocusable(shape = CircleShape)
                    .pressable(
                        role = Role.Button,
                        onClickLabel = "Remove $query from recent searches",
                        onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove $query",
                    tint = SearchTokens.TextLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Trending today ranked list row with 2:3 poster art and tabular numerals.
 */
@Composable
fun TrendingRankedRow(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankStr = String.format("%02d", rank)
    val rankAlpha = when (rank) {
        1 -> 0.90f
        2 -> 0.55f
        else -> 0.35f
    }

    val typeLabel = when (item.type) {
        MediaType.MOVIE -> "Movie"
        MediaType.TV_SERIES -> "Series"
        MediaType.ANIME -> "Anime"
        MediaType.LIVE_TV -> "Live TV"
    }

    val metadataText = buildString {
        append(typeLabel)
        item.year?.let { append(" · $it") }
        if (!item.quality.isNullOrBlank()) {
            append(" · ${item.quality}")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .tvFocusable(shape = RoundedCornerShape(8.dp))
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank numeral with tabular digits
            Text(
                text = rankStr,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.02).sp,
                    color = Color.White.copy(alpha = rankAlpha),
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.width(42.dp)
            )

            // Poster thumbnail 36x54dp (exact 2:3 cinematic ratio)
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val poster = item.posterUrl ?: item.backdropUrl
                AppAsyncImage(
                    model = poster,
                    contentDescription = item.title,
                    titleForFallback = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = sanitizeTitle(item.title),
                    style = SearchTokens.ItemTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = metadataText,
                    style = SearchTokens.ItemMetadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * First result row: Best Match with exact 2:3 poster (62x93dp), isolated Play button, and golden star rating.
 */
@Composable
fun BestMatchResultRow(
    item: MediaItem,
    onPlay: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = when (item.type) {
        MediaType.MOVIE -> "Movie"
        MediaType.TV_SERIES -> "Series"
        MediaType.ANIME -> "Anime"
        MediaType.LIVE_TV -> "Live TV"
    }

    val metadata = buildString {
        append(typeLabel)
        item.year?.let { append(" · $it") }
        if (!item.quality.isNullOrBlank()) {
            append(" · ${item.quality}")
        }
    }

    val ratingNum = item.rating?.toFloatOrNull()?.let { String.format("%.1f", it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(shape = RoundedCornerShape(10.dp))
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster 62 x 93 px (exact 2:3 cinematic ratio)
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 93.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val poster = item.posterUrl ?: item.backdropUrl
                AppAsyncImage(
                    model = poster,
                    contentDescription = item.title,
                    titleForFallback = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BEST MATCH",
                    style = SearchTokens.SectionLabel,
                    color = SearchTokens.TextLabel
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = sanitizeTitle(item.title),
                    style = SearchTokens.BestMatchTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = metadata,
                    style = SearchTokens.ItemMetadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Play pill button with 44dp hit area floor & isolated click consumption
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* absorb taps to prevent row click bubbling */ },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .tvFocusable(shape = RoundedCornerShape(8.dp))
                            .playButtonSpringPress(
                                role = Role.Button,
                                onClickLabel = "Play ${item.title}",
                                onClick = onPlay
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.Play,
                                contentDescription = null,
                                tint = SearchTokens.DarkText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Play",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = SearchTokens.DarkText
                                )
                            )
                        }
                    }
                }
            }

            // Rating aligned to right with filled star and tabular numbers
            if (ratingNum != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating $ratingNum",
                        tint = SearchTokens.RatingGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = ratingNum,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Compact result row: poster 40x60 (2:3 ratio), title, quality metadata, and filled star rating.
 */
@Composable
fun CompactResultRow(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = when (item.type) {
        MediaType.MOVIE -> "Movie"
        MediaType.TV_SERIES -> "Series"
        MediaType.ANIME -> "Anime"
        MediaType.LIVE_TV -> "Live TV"
    }

    val metadata = buildString {
        append(typeLabel)
        item.year?.let { append(" · $it") }
        if (!item.quality.isNullOrBlank()) {
            append(" · ${item.quality}")
        }
    }

    val ratingNum = item.rating?.toFloatOrNull()?.let { String.format("%.1f", it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .tvFocusable(shape = RoundedCornerShape(8.dp))
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster 40 x 60 px (exact 2:3 cinematic ratio)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val poster = item.posterUrl ?: item.backdropUrl
                AppAsyncImage(
                    model = poster,
                    contentDescription = item.title,
                    titleForFallback = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = sanitizeTitle(item.title),
                    style = SearchTokens.ItemTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = metadata,
                    style = SearchTokens.ItemMetadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (ratingNum != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating $ratingNum",
                        tint = SearchTokens.RatingGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = ratingNum,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = Color.White,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Calm, minimal placeholder row respecting v1.0.7 loading rules (no infinite shimmer loops).
 */
@Composable
fun ResultSkeletonRow(
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {
    val blockColor = Color(0xFF161722)

    Column(modifier = modifier.fillMaxWidth()) {
        if (isFirst) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 62.dp, height = 93.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(blockColor)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(width = 70.dp, height = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(blockColor)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(blockColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 90.dp, height = 11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(blockColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(blockColor)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(blockColor)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(blockColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(blockColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(blockColor)
                )
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Glass snackbar toast for "Recent searches cleared" with Undo button.
 * Includes imePadding to ride cleanly above the keyboard.
 */
@Composable
fun SearchGlassToast(
    message: String,
    actionLabel: String?,
    onActionClick: () -> Unit,
    isVisible: Boolean,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reduceMotion) fadeIn(tween(0)) else (
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
            ) + fadeIn(tween(180))
        ),
        exit = if (reduceMotion) fadeOut(tween(0)) else (
            slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(140))
        ),
        modifier = modifier.imePadding()
    ) {
        Box(
            modifier = Modifier
                .searchGlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    hazeState = hazeState,
                    isActive = false
                )
                .background(Color(0xFF14151F).copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = message,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                )

                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .tvFocusable(shape = RoundedCornerShape(6.dp))
                            .pressable(
                                role = Role.Button,
                                onClick = onActionClick
                            )
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
