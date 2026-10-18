package com.euthopiar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppTypography

/**
 * Data model for an option in [GlassDropdownRow].
 */
data class DropdownOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val dividerBefore: Boolean = false,
    val colorBadge: Color? = null,
)

/**
 * Shared reusable frosted glass dropdown row.
 * Displays a label + optional description on the start, and a glass pill trigger
 * that opens an aligned DropdownMenu on the end.
 */
@Composable
fun <T> GlassDropdownRow(
    label: String,
    description: String? = null,
    options: List<DropdownOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    showTopDivider: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerWidthDp by remember { mutableStateOf(0.dp) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val selectedOption = remember(options, selected) {
        options.firstOrNull { it.value == selected }
    }
    val selectedTitle = selectedOption?.title.orEmpty()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "chevronRotation",
    )

    // Remembered shapes, brushes and tokens to prevent per-frame allocations
    val triggerShape = remember { RoundedCornerShape(20.dp) }
    val triggerBorderBrush = remember { SolidColor(Color.White.copy(alpha = 0.12f)) }
    val triggerFillColor = remember { Color.White.copy(alpha = 0.08f) }
    val menuShape = remember { RoundedCornerShape(16.dp) }
    val menuBorderStroke = remember { BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) }
    val menuContainerColor = remember { Color(0xFF1F1F25) }
    val itemShape = remember { RoundedCornerShape(10.dp) }
    val dividerColor = remember { Color.White.copy(alpha = 0.08f) }
    val menuDividerColor = remember { Color.White.copy(alpha = 0.10f) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showTopDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Start: Label & optional description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = label,
                    style = AppTypography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White,
                )

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = AppTypography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color.White.copy(alpha = 0.50f),
                    )
                }
            }

            // End: Trigger and DropdownMenu anchor
            Box(
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .onGloballyPositioned { coordinates ->
                        triggerWidthDp = with(density) { coordinates.size.width.toDp() }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier
                        .semantics {
                            role = Role.DropdownList
                            contentDescription = if (description != null) {
                                "$label, $description, $selectedTitle"
                            } else {
                                "$label, $selectedTitle"
                            }
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        }
                        .clip(triggerShape)
                        .background(triggerFillColor)
                        .border(1.dp, triggerBorderBrush, triggerShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = if (expanded) "Collapse $label menu" else "Expand $label menu",
                            onClick = {
                                expanded = !expanded
                            },
                        )
                        .padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (selectedOption?.colorBadge != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(selectedOption.colorBadge)
                                .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = selectedTitle,
                        style = AppTypography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                rotationZ = chevronRotation
                            },
                    )
                }

                // DropdownMenu aligned to trigger's end edge, opening below it
                val menuWidth = 236.dp
                val xOffset = if (triggerWidthDp > 0.dp) triggerWidthDp - menuWidth else 0.dp

                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = menuContainerColor,
                    ),
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(x = xOffset, y = 4.dp),
                        shape = menuShape,
                        containerColor = menuContainerColor,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        border = menuBorderStroke,
                        modifier = Modifier.width(menuWidth),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                        ) {
                            options.forEach { option ->
                                if (option.dividerBefore) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = menuDividerColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }

                                val isSelected = option.value == selected
                                val itemInteractionSource = remember { MutableInteractionSource() }
                                val isPressed by itemInteractionSource.collectIsPressedAsState()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(itemShape)
                                        .background(
                                            if (isPressed) Color.White.copy(alpha = 0.06f) else Color.Transparent,
                                        )
                                        .selectable(
                                            selected = isSelected,
                                            interactionSource = itemInteractionSource,
                                            indication = null,
                                            role = Role.RadioButton,
                                            onClick = {
                                                try {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } catch (_: Exception) {
                                                    try {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } catch (_: Exception) {}
                                                }
                                                onSelected(option.value)
                                                expanded = false
                                            },
                                        )
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Fixed 18dp leading slot
                                    Box(
                                        modifier = Modifier.size(18.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (option.colorBadge != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(option.colorBadge)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = option.title,
                                            style = AppTypography.titleSmall.copy(
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                        if (!option.subtitle.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.subtitle,
                                                style = AppTypography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                ),
                                                color = Color.White.copy(alpha = 0.50f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun GlassDropdownRowPreview() {
    var selectedNav by remember { mutableStateOf("floating") }
    var selectedPosters by remember { mutableStateOf("flagship") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0F))
            .padding(16.dp),
    ) {
        GlassDropdownRow(
            label = "Navigation",
            description = "How the bar sits",
            options = listOf(
                DropdownOption("floating", "Floating Glass Pill", "Frosted acrylic dock suspended above bottom with fluid motion"),
                DropdownOption("docked", "Docked Edge Bar", "Classic edge-to-edge docked navigation bar"),
            ),
            selected = selectedNav,
            onSelected = { selectedNav = it },
            showTopDivider = false,
        )

        GlassDropdownRow(
            label = "Posters",
            description = "Size and density",
            options = listOf(
                DropdownOption("flagship", "Flagship Cinema", "Cinematic 2.0x poster scale with rich breathing room"),
                DropdownOption("compact", "Compact Grid", "Balanced 1.0x layout for maximum catalog density"),
            ),
            selected = selectedPosters,
            onSelected = { selectedPosters = it },
            showTopDivider = true,
        )
    }
}
