package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Snapping horizontal wheel selector of text labels.
 *
 * Visual & Interactive Spec:
 * - Centered label: ~22-24sp, SemiBold, alpha 1.0, with a tiny 4dp white indicator dot.
 * - Neighbors: smooth scale (0.68x) and alpha fade (0.40x) interpolated continuously via graphicsLayer.
 * - Snaps smoothly using rememberSnapFlingBehavior with SnapPosition.Center.
 * - Content padding enables first and last items to reach exact center.
 * - Haptic feedback: one soft tick per centered index change during scrolling.
 * - Settled callback: onSelected is invoked only once a user-initiated scroll has settled.
 */
@Composable
fun SnapWheelSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val itemWidth = 104.dp
    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current

    val initialIndex = remember { selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)) }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(
        lazyListState = lazyListState,
        snapPosition = SnapPosition.Center
    )

    // Compute the centered item index from lazyListState.layoutInfo inside derivedStateOf
    val centeredIndex by remember(lazyListState, options.size) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf selectedIndex.coerceIn(0, options.size - 1)

            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            visibleItems.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2f
                abs(itemCenter - viewportCenter)
            }?.index ?: selectedIndex.coerceIn(0, options.size - 1)
        }
    }

    // Haptics: trigger one soft tick per index change during scrolling (no repeats)
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(centeredIndex, lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && centeredIndex != lastHapticIndex) {
            lastHapticIndex = centeredIndex
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_TICK)
                } catch (_: Throwable) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    // Call onSelected only once a USER-INITIATED scroll has settled on a new index
    var hasUserScrolled by remember { mutableStateOf(false) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            if (!isProgrammaticScroll) {
                hasUserScrolled = true
            }
        } else if (hasUserScrolled) {
            hasUserScrolled = false
            val settled = centeredIndex
            if (settled in options.indices && settled != selectedIndex) {
                onSelected(settled)
            }
        }
    }

    // External selectedIndex changes (sync without thrashing)
    LaunchedEffect(selectedIndex, options.size) {
        if (selectedIndex !in options.indices) return@LaunchedEffect
        if (centeredIndex == selectedIndex) return@LaunchedEffect
        if (lazyListState.isScrollInProgress) return@LaunchedEffect

        isProgrammaticScroll = true
        try {
            val layoutInfo = lazyListState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
            val delta = if (itemInfo != null) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                itemCenter - viewportCenter
            } else {
                (selectedIndex - centeredIndex) * itemWidthPx
            }

            if (abs(delta) > 1f) {
                if (reduceMotion) {
                    lazyListState.scrollBy(delta)
                } else {
                    lazyListState.animateScrollBy(delta)
                }
            }
        } finally {
            isProgrammaticScroll = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        val halfViewport = maxWidth / 2f
        val halfItem = itemWidth / 2f
        val horizontalPadding = (halfViewport - halfItem).coerceAtLeast(0.dp)

        LazyRow(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = options,
                key = { _, option -> option }
            ) { index, option ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            this.role = Role.Tab
                            this.selected = (index == selectedIndex)
                            this.contentDescription = option
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (index != selectedIndex) {
                                onSelected(index)
                                coroutineScope.launch {
                                    isProgrammaticScroll = true
                                    try {
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportCenter = (lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportEndOffset) / 2f
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                            val delta = itemCenter - viewportCenter
                                            if (reduceMotion) {
                                                lazyListState.scrollBy(delta)
                                            } else {
                                                lazyListState.animateScrollBy(delta)
                                            }
                                        } else {
                                            lazyListState.animateScrollToItem(index)
                                        }
                                    } finally {
                                        isProgrammaticScroll = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                val layoutInfo = lazyListState.layoutInfo
                                val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                if (itemInfo != null) {
                                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                    val dist = abs(itemCenter - viewportCenter)
                                    val maxDist = itemInfo.size.toFloat().coerceAtLeast(1f)
                                    val frac = (dist / maxDist).coerceIn(0f, 1f)

                                    val scale = 1.0f - (1.0f - 0.68f) * frac
                                    val computedAlpha = 1.0f - (1.0f - 0.40f) * frac

                                    scaleX = scale
                                    scaleY = scale
                                    alpha = computedAlpha
                                } else {
                                    scaleX = 0.68f
                                    scaleY = 0.68f
                                    alpha = 0.40f
                                }
                            }
                    ) {
                        Text(
                            text = option,
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .graphicsLayer {
                                    val layoutInfo = lazyListState.layoutInfo
                                    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                    if (itemInfo != null) {
                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                        val dist = abs(itemCenter - viewportCenter)
                                        val maxDist = itemInfo.size.toFloat().coerceAtLeast(1f)
                                        val dotFrac = (dist / (maxDist * 0.35f)).coerceIn(0f, 1f)
                                        alpha = 1.0f - dotFrac
                                    } else {
                                        alpha = 0f
                                    }
                                }
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
