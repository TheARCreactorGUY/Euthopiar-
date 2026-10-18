package com.euthopiar.ui.details.downloads

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily
import kotlin.math.roundToInt

/**
 * Editorial text-only tabs row:
 * - Tabs: "All", "P2P", "Direct" (13sp Medium).
 * - Underline animates x-offset and width with spring(0.75f, 500f).
 * - Text color animates over 150ms.
 * - 0.5dp hairline under full tab row, 1.5dp white underline on top.
 * - Tab row is hidden entirely if tabs.size <= 1.
 */
@Composable
fun DownloadTabs(
    tabs: List<DownloadTab>,
    selectedTab: DownloadTab,
    onTabSelected: (DownloadTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = isReduceMotion()
    val density = LocalDensity.current

    var tabBounds by remember { mutableStateOf(mapOf<DownloadTab, Pair<Float, Float>>()) }

    val currentBounds = tabBounds[selectedTab] ?: Pair(0f, 0f)

    val animatedIndicatorOffset by animateFloatAsState(
        targetValue = currentBounds.first,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "tabIndicatorOffset"
    )

    val animatedIndicatorWidth by animateFloatAsState(
        targetValue = currentBounds.second,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "tabIndicatorWidth"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = DownloadTokens.tabGapAbove, bottom = DownloadTokens.tabGapBelow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DownloadTokens.tabSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) DownloadTokens.textPrimary else DownloadTokens.textMuted,
                    animationSpec = if (reduceMotion) snap() else tween(DownloadTokens.TAB_TEXT_COLOR_DURATION_MS),
                    label = "tabTextColor_${tab.name}"
                )

                Text(
                    text = tab.title,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = textColor
                    ),
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val parent = coordinates.parentLayoutCoordinates
                            if (parent != null) {
                                val pos = coordinates.positionInParent()
                                val w = coordinates.size.width.toFloat()
                                val current = tabBounds[tab]
                                if (current == null || current.first != pos.x || current.second != w) {
                                    tabBounds = tabBounds + (tab to Pair(pos.x, w))
                                }
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                        }
                        .padding(bottom = DownloadTokens.tabPaddingBottom)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DownloadTokens.tabUnderlineHeight)
        ) {
            // Full-width hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DownloadTokens.hairlineHeight)
                    .align(Alignment.BottomStart)
                    .background(DownloadTokens.hairline)
            )

            // Animated active underline indicator
            if (animatedIndicatorWidth > 0f) {
                val widthDp = with(density) { animatedIndicatorWidth.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = animatedIndicatorOffset.roundToInt(), y = 0) }
                        .width(widthDp)
                        .height(DownloadTokens.tabUnderlineHeight)
                        .align(Alignment.BottomStart)
                        .background(Color.White)
                )
            }
        }
    }
}
