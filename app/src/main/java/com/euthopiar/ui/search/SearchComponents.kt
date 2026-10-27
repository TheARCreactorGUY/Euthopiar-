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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.playButtonSpringPress
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.components.tvFocusable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

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

    val HazeBlue = Color(0xFF2E3D64)
    val HazeRose = Color(0xFF6B2F48)

    val DropdownRadius = 12.dp
    val SearchBarRadius = 16.dp
    val BackButtonRadius = 14.dp
    val PopoverRadius = 14.dp

    val LargeHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.03).sp,
        color = Color.White
    )

    val QueryHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.03).sp,
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
 */
@Composable
fun SearchAmbientHazeBackground(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
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

    val isAmoled = com.euthopiar.ui.theme.LocalAmoledMode.current
    val baseBg = if (isAmoled) Color.Black else SearchTokens.BaseBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val washAlphaScale = if (isAmoled) 0.35f else 1.0f
                val effectiveDrift1 = if (reduceMotion) 0f else driftOffset1.value
                val effectiveDrift2 = if (reduceMotion) 0f else driftOffset2.value
                val maxDim = maxOf(size.width, size.height)

                // Soft Indigo/Blue Haze atmospheric wash near Top-Right (no circular boundaries)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to SearchTokens.HazeBlue.copy(alpha = 0.24f * washAlphaScale),
                        0.25f to SearchTokens.HazeBlue.copy(alpha = 0.15f * washAlphaScale),
                        0.50f to SearchTokens.HazeBlue.copy(alpha = 0.06f * washAlphaScale),
                        0.75f to SearchTokens.HazeBlue.copy(alpha = 0.015f * washAlphaScale),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.95f + effectiveDrift1, size.height * 0.10f + effectiveDrift2),
                        radius = maxDim * 1.15f
                    )
                )

                // Soft Rose/Pink Haze atmospheric wash near Lower-Left (no circular boundaries)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to SearchTokens.HazeRose.copy(alpha = 0.20f * washAlphaScale),
                        0.25f to SearchTokens.HazeRose.copy(alpha = 0.12f * washAlphaScale),
                        0.50f to SearchTokens.HazeRose.copy(alpha = 0.05f * washAlphaScale),
                        0.75f to SearchTokens.HazeRose.copy(alpha = 0.01f * washAlphaScale),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.05f + effectiveDrift2, size.height * 0.85f + effectiveDrift1),
                        radius = maxDim * 1.05f
                    )
                )
            }
    ) {
        content()
    }
}

/**
 * Standard glass surface modifier matching Part 1 global design language.
 */
fun Modifier.searchGlassSurface(
    shape: Shape = RoundedCornerShape(SearchTokens.SearchBarRadius),
    hazeState: HazeState? = null,
    isActive: Boolean = false,
    hasGlow: Boolean = false
): Modifier {
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            } else Modifier
        )
        .border(
            width = SearchTokens.HairlineThickness,
            color = if (isActive) SearchTokens.GlassBorderActive else SearchTokens.GlassBorder,
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
 * Press feedback animation modifier using app standard pressable (0.96f scale, Confirm haptics).
 */
@Composable
fun searchPressModifier(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    onClickLabel: String? = null
): Modifier = Modifier.pressable(
    role = role,
    onClickLabel = onClickLabel,
    onLongClick = onLongClick,
    onClick = onClick
)

/**
 * Equal-width rounded glass dropdown box with anchored popover.
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
    isTrailing: Boolean = false
) {
    val isFiltered = !selectedValue.isNullOrBlank() && selectedValue != "All" && selectedValue != "All types" && selectedValue != "All genres" && selectedValue != "Any year"
    val displayLabel = if (isFiltered) selectedValue!! else label
    val reduceMotion = LocalReduceMotion.current

    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )

    // Outer box has 48dp height for accessible tap target per design guidelines
    Box(
        modifier = modifier
            .fillMaxWidth()
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
                            fadeIn(tween(160)) togetherWith fadeOut(tween(130))
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
                    popupAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                }
            } else if (isPopupRendered) {
                if (reduceMotion) {
                    popupAnim.snapTo(0f)
                    isPopupRendered = false
                } else {
                    popupAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                    isPopupRendered = false
                }
            }
        }

        if (isPopupRendered) {
            val density = LocalDensity.current
            val popoverScale = 0.95f + (0.05f * popupAnim.value)
            val popoverAlpha = popupAnim.value
            val popoverOffsetY = (1f - popupAnim.value) * (-6f) * density.density

            val popupAlignment = if (isTrailing) Alignment.TopEnd else Alignment.TopStart
            val origin = if (isTrailing) {
                androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            } else {
                androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }

            Popup(
                alignment = popupAlignment,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }), // anchored directly under the 48dp target
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true)
            ) {

                val isAmoled = com.euthopiar.ui.theme.LocalAmoledMode.current
                val popoverShape = RoundedCornerShape(SearchTokens.PopoverRadius)
                val glassFill = if (isAmoled) Color.Black.copy(alpha = 0.85f) else Color(0xFF0F101B).copy(alpha = 0.70f)
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
                                    Color.White.copy(alpha = 0.08f),
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

                            val rowAnimSpec: AnimationSpec<Float> = remember(index, reduceMotion) {
                                if (reduceMotion) tween<Float>(0)
                                else tween<Float>(durationMillis = 140, delayMillis = if (index < 5) index * 20 else 100)
                            }
                            val rowAlpha by animateFloatAsState(
                                targetValue = if (isExpanded) 1f else 0f,
                                animationSpec = rowAnimSpec,
                                label = "rowAlpha_$index"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .graphicsLayer { alpha = rowAlpha }
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
                                        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.5f, stiffness = 500f),
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
 * Recent search vertical list row.
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
                .pressable(
                    role = Role.Button,
                    onClickLabel = "Search for $query",
                    onClick = onClick
                )
                .padding(horizontal = 4.dp),
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
                    .size(44.dp)
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
 * Trending today ranked list row ("01", "02", "03"...).
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
                .height(64.dp)
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank numeral
            Text(
                text = rankStr,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.02).sp,
                    color = Color.White.copy(alpha = rankAlpha)
                ),
                modifier = Modifier.width(42.dp)
            )

            // Small poster thumbnail 36x50dp, 10dp radius with faint hairline
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                )
                val poster = item.posterUrl ?: item.backdropUrl
                if (!poster.isNullOrBlank()) {
                    AsyncImage(
                        model = poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
 * First result row: Best Match with poster 62x88, Play pill button, and rating.
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
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster 62 x 88 px, 10 px radius with faint hairline
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 88.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                )
                val poster = item.posterUrl ?: item.backdropUrl
                if (!poster.isNullOrBlank()) {
                    AsyncImage(
                        model = poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Small white pill "Play" button (white fill, dark text)
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .playButtonSpringPress(
                            role = Role.Button,
                            onClickLabel = "Play ${item.title}",
                            onClick = onPlay
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
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

            // Rating aligned to right
            if (ratingNum != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Rating $ratingNum",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = ratingNum,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Compact result row: poster 40x56, title, metadata, star rating on right.
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
    }

    val ratingNum = item.rating?.toFloatOrNull()?.let { String.format("%.1f", it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pressable(
                    onClickLabel = "Open ${item.title}",
                    onClick = onClick
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster 40 x 56 px, 8 px radius
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14151F))
                    .border(SearchTokens.HairlineThickness, SearchTokens.HairlineColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
                val poster = item.posterUrl ?: item.backdropUrl
                if (!poster.isNullOrBlank()) {
                    AsyncImage(
                        model = poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Rating $ratingNum",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = ratingNum,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Result skeleton row with 1.4s shimmer animation.
 */
@Composable
fun ResultSkeletonRow(
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = if (reduceMotion) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1B1C26), Color(0xFF1B1C26))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF181924),
                Color(0xFF27293A),
                Color(0xFF181924)
            ),
            start = Offset(translateAnim - 300f, 0f),
            end = Offset(translateAnim, 0f)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isFirst) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 62.dp, height = 88.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(width = 70.dp, height = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 90.dp, height = 11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }

        SearchHairlineDivider()
    }
}

/**
 * Glass snackbar toast for "Recent searches cleared" with Undo button.
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
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(130, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(130)),
        modifier = modifier
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
