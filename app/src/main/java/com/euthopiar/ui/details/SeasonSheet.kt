package com.euthopiar.ui.details

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Approved Frosted Glass Season Sheet (Fix 2):
 * - Dark translucent fill (74% near-black with hint of blue-gray: Color(0xFF0E1118))
 * - Background blur about 22px
 * - 0.5px hairline on the top edge (white 18%)
 * - 28dp top corners, 36x4dp drag handle (white 28%)
 * - Dark scrim behind (62% black)
 * - Pinned header: "Select season" (26sp, medium, line height 1.05, slightly negative letter spacing)
 * - Subtitle: series name and season count (12sp muted)
 * - Rows: 48dp tall, NO boxes, NO borders around rows, separated by 0.5px hairlines
 * - "Season X" on left (16sp, medium if selected), episode count on right (11sp muted)
 * - Fixed 22dp slot: solid white circle (92%) with dark check for selected season; reserved empty for unselected
 * - Capped at 70% screen height, scrolls inside with centered scroll to selected season
 * - Bottom edge fade (70dp)
 * - Pop selection animation with 200ms close delay
 */
@Composable
fun SeasonSheet(
    seriesTitle: String,
    availableSeasons: List<Int>,
    selectedSeason: Int,
    episodes: List<EpisodeItem>,
    onSelectSeason: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val sheetProgress = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var locallySelectedSeason by remember { mutableIntStateOf(selectedSeason) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            sheetProgress.snapTo(1f)
        } else {
            sheetProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    sheetProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = sheetProgress.value
    val maxSheetHeight = (configuration.screenHeightDp * 0.70f).dp
    val listState = rememberLazyListState()
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    // Center selected season when sheet opens
    LaunchedEffect(availableSeasons, selectedSeason) {
        val selectedIdx = availableSeasons.indexOf(selectedSeason)
        if (selectedIdx >= 0) {
            val targetIdx = (selectedIdx - 2).coerceAtLeast(0)
            listState.scrollToItem(targetIdx)
        }
    }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheetFill = Color(0xFF0E1118).copy(alpha = 0.74f)
    val scrimAlpha = if (reduceMotion) 0.62f else (0.62f * (animValue / (250f / 340f))).coerceIn(0f, 0.62f)
    val headerAlpha = if (reduceMotion) 1f else animValue
    val headerOffsetY = if (reduceMotion) 0f else (1f - animValue) * with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            )
    ) {
        // Scrim: 62% black fade in ~250ms
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Sheet Container: slides up from bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .onGloballyPositioned { coordinates ->
                    sheetHeightPx = coordinates.size.height.toFloat()
                }
                .offset {
                    val slideDistance = if (sheetHeightPx > 0f) sheetHeightPx else with(density) { maxSheetHeight.toPx() }
                    val currentY = (1f - animValue) * slideDistance + dragOffsetY
                    IntOffset(0, currentY.roundToInt())
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through */ }
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || dragOffsetY > 0) {
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragOffsetY > 120f) {
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
                .clip(sheetShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 22.dp
                            tints = listOf(HazeTint(sheetFill))
                            fallbackTint = HazeTint(Color(0xFF0E1118).copy(alpha = 0.94f))
                        }
                    } else {
                        Modifier.background(Color(0xFF0E1118).copy(alpha = 0.94f))
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = sheetShape
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Drag handle: 36 by 4 dp, white 28%
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                }

                // Fixed Header with fade and 8dp rise motion
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = headerOffsetY
                        }
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Select season",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 27.sp,
                            letterSpacing = (-0.3).sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val seasonsCountText = when {
                        availableSeasons.isEmpty() -> "0 seasons"
                        availableSeasons.size == 1 -> "1 season"
                        else -> "${availableSeasons.size} seasons"
                    }
                    val displayTitle = seriesTitle.ifBlank { "Series" }
                    Text(
                        text = "$displayTitle · $seasonsCountText",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    )
                }

                // Hairline below header (0.5px white 14%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.14f))
                )

                // Scrollable Season Rows with bottom edge fade
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(availableSeasons, key = { _, s -> s }) { index, seasonNum ->
                            val isSelected = seasonNum == locallySelectedSeason
                            val epCount = episodes.count { it.seasonNumber == seasonNum }

                            SeasonRow(
                                seasonNumber = seasonNum,
                                episodeCount = epCount,
                                isSelected = isSelected,
                                staggerIndex = index,
                                animProgress = animValue,
                                reduceMotion = reduceMotion,
                                onSelect = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    locallySelectedSeason = seasonNum
                                    onSelectSeason(seasonNum)
                                    dismissWithAnim()
                                }
                            )

                            // 0.5px hairline between rows
                            if (index < availableSeasons.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.14f))
                                )
                            }
                        }
                    }

                    // Soft fade at bottom edge of list (70dp) showing more rows are below
                    if (listState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(70.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xFF0E1118).copy(alpha = 0.85f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonRow(
    seasonNumber: Int,
    episodeCount: Int,
    isSelected: Boolean,
    staggerIndex: Int,
    animProgress: Float,
    reduceMotion: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rowScale"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = 0.55f, // tiny overshoot pop
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    // Staggered enter: first 6 rows fade & rise 10dp with 40ms stagger
    val staggerDelay = (staggerIndex.coerceAtMost(6) * 0.06f)
    val rowProgress = ((animProgress - staggerDelay) / (1f - staggerDelay)).coerceIn(0f, 1f)
    val rowAlpha = if (reduceMotion) 1f else rowProgress
    val rowOffsetY = if (reduceMotion) 0f else (1f - rowProgress) * 10f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                alpha = rowAlpha
                translationY = rowOffsetY
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onSelect
            )
            .background(if (isPressed) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "Season X" on the left (16sp, medium if selected)
            Text(
                text = "Season $seasonNumber",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Episode count in 11sp muted
                Text(
                    text = "$episodeCount ${if (episodeCount == 1) "episode" else "episodes"}",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                )

                // Fixed 22dp slot: solid white circle (92%) with dark check for selected, empty reserved for unselected
                Box(
                    modifier = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF0E1118),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
