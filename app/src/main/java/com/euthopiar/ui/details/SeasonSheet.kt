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
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Approved Frosted Glass Season Sheet:
 * - Obsidian dark 85% alpha (92% AMOLED)
 * - 24dp Haze background blur
 * - 0.5dp hairline border @ 16% white alpha
 * - 28dp top corners, 36x4dp drag handle
 * - Full-screen black scrim @ 65% opacity
 * - Pinned header: "Select season" (20sp Bold, 96% white), series name & season count (13sp Normal, 54% white)
 * - Subtle hairline divider: 0.5dp @ 10% white alpha
 * - Rows: 48dp tall with 0.5dp hairline separators
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
    val isAmoled = LocalAmoledMode.current
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
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
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

    val sheetShape = RoundedCornerShape(topStart = EuthopiarGlassTokens.RadiusSheet, topEnd = EuthopiarGlassTokens.RadiusSheet)
    val sheetFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackFill = EuthopiarGlassTokens.fallbackFill(isAmoled)
    val scrimAlpha = if (reduceMotion) EuthopiarGlassTokens.ScrimAlpha else (EuthopiarGlassTokens.ScrimAlpha * (animValue / (250f / 340f))).coerceIn(0f, EuthopiarGlassTokens.ScrimAlpha)
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
        // Scrim
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
                        val next = dragOffsetY + if (dragOffsetY + delta < 0f) delta * 0.18f else delta
                        dragOffsetY = next.coerceAtLeast(-24f)
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        // Project momentum: fast downward flick (velocity > 900f px/s) or drag > 120f dismisses
                        val projected = dragOffsetY + (velocity * 0.15f)
                        if (dragOffsetY > 120f || (dragOffsetY > 24f && velocity > 900f) || projected > 220f) {
                            dismissWithAnim()
                        } else {
                            coroutineScope.launch {
                                Animatable(dragOffsetY).animateTo(
                                    targetValue = 0f,
                                    initialVelocity = velocity,
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
                            blurRadius = EuthopiarGlassTokens.BlurRadius
                            tints = listOf(HazeTint(sheetFill))
                            fallbackTint = HazeTint(fallbackFill)
                        }
                    } else {
                        Modifier.background(fallbackFill)
                    }
                )
                .border(
                    width = EuthopiarGlassTokens.HairlineWidth,
                    color = EuthopiarGlassTokens.HairlineBorder,
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

                // Fixed Header matching UpdateInfoDialog
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = headerOffsetY
                        }
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = "Select season",
                        style = EuthopiarGlassTokens.HeaderTitle
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
                        style = EuthopiarGlassTokens.HeaderSubtitle
                    )
                }

                // Subtle Hairline Divider below header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EuthopiarGlassTokens.HairlineWidth)
                        .background(EuthopiarGlassTokens.HairlineDivider)
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

                            // 0.5dp hairline divider between rows
                            if (index < availableSeasons.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(EuthopiarGlassTokens.HairlineWidth)
                                        .background(EuthopiarGlassTokens.HairlineDivider)
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
                                        listOf(Color.Transparent, sheetFill.copy(alpha = 0.85f))
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
        targetValue = if (isSelected) 1f else 0.92f,
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
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "Season X" on the left (15.5sp, semi-bold if selected)
            Text(
                text = "Season $seasonNumber",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else EuthopiarGlassTokens.TextWhite82
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Episode count in 12sp muted
                Text(
                    text = "$episodeCount ${if (episodeCount == 1) "episode" else "episodes"}",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = EuthopiarGlassTokens.TextWhite54
                    )
                )

                // Fixed 22dp slot: solid white circle with dark check for selected, empty reserved for unselected
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
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = EuthopiarGlassTokens.TextDark,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
