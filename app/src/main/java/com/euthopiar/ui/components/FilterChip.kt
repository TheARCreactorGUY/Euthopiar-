package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceMid
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable

/**
 * Standard streaming category filter chip (36dp height, 48dp minimum interactive touch target).
 */
@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
            .semantics {
                this.role = Role.Checkbox
                this.selected = selected
            }
            .pressable(role = Role.Checkbox, onClick = onClick)
            .tvFocusable(shape = ShapeFull),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(Dimens.chipHeight)
                .clip(ShapeFull)
                .background(if (selected) TextPrimary else SurfaceMid)
                .then(
                    if (!selected) Modifier.border(1.dp, Outline, ShapeFull) else Modifier
                )
                .padding(horizontal = Dimens.spacing16),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = AppTypography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Background else TextSecondary
            )
        }
    }
}
