package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*

/**
 * Standard token-driven Segmented Control (pills in SurfaceLow track with SurfaceHigh selected indicator).
 * Replaces ad-hoc custom pill controls across Search and filter screens.
 */
@Composable
fun AppSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(ShapeFull)
            .background(SurfaceLow)
            .border(HairlineWidth, Outline, ShapeFull)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeFull)
                    .background(if (isSelected) SurfaceHigh else Color.Transparent)
                    .tvFocusable(shape = ShapeFull)
                    .pressable { onItemSelected(index) }
                    .clickable(
                        role = Role.Tab,
                        onClick = { onItemSelected(index) }
                    )
                    .semantics {
                        this.role = Role.Tab
                        this.selected = isSelected
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTypography.labelMedium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppSegmentedControlPreview() {
    AppSegmentedControl(
        items = listOf("All", "Movies", "Series", "Anime"),
        selectedIndex = 1,
        onItemSelected = {}
    )
}
