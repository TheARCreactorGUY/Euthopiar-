package com.euthopiar.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.*

/**
 * Standard token-based Tab Row matching the approved visual language:
 * - Transparent background on the black page.
 * - Left-aligned labels spaced 20dp apart with 16dp start margin.
 * - 14sp font size, inactive 55% white (regular weight), active 100% white (medium weight, no bold).
 * - Smoothly sliding 1.5dp white underline under active tab.
 * - 0.5px hairline divider sitting below the underline across full width.
 */
@Composable
fun AppTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    indicatorColor: Color = Color.White
) {
    val density = LocalDensity.current
    val tabOffsets = remember { mutableStateMapOf<Int, Dp>() }
    val tabWidths = remember { mutableStateMapOf<Int, Dp>() }

    val activeOffset = tabOffsets[selectedTabIndex] ?: 0.dp
    val activeWidth = tabWidths[selectedTabIndex] ?: 0.dp
    var hasMeasured by remember { mutableStateOf(false) }

    LaunchedEffect(activeWidth) {
        if (activeWidth > 0.dp) {
            hasMeasured = true
        }
    }

    val reduceMotion = LocalReduceMotion.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val animatedOffset by animateDpAsState(
        targetValue = activeOffset,
        animationSpec = if (reduceMotion || !hasMeasured) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "tabIndicatorOffset"
    )
    val animatedWidth by animateDpAsState(
        targetValue = activeWidth,
        animationSpec = if (reduceMotion || !hasMeasured) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "tabIndicatorWidth"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInParent()
                            tabOffsets[index] = with(density) { pos.x.toDp() }
                            tabWidths[index] = with(density) { coordinates.size.width.toDp() }
                        }
                        .tvFocusable(shape = ShapeS)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                try {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                } catch (_: Throwable) {}
                                onTabSelected(index)
                            }
                        )
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                        }
                        .padding(top = 10.dp, bottom = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) contentColor else Color.White.copy(alpha = 0.55f)
                        )
                    )
                }
            }
        }

        // Active indicator line resting directly on top of the hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.5.dp)
        ) {
            if (activeWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(animatedWidth)
                        .height(1.5.dp)
                        .background(indicatorColor)
                )
            }
        }

        // Full-width 0.5px hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppTabRowPreview() {
    AppTabRow(
        tabs = listOf("Episodes", "More Like This", "Details"),
        selectedTabIndex = 0,
        onTabSelected = {}
    )
}
