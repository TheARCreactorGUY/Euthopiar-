package com.euthopiar.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.TextTertiary

/**
 * Standard token-coloured Switch for settings and toggles across Euthopiar.
 * Uses Accent for active state, SurfaceHigh/Outline for inactive state, and TvFocus.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useWhiteStyle: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier
            .tvFocusable(shape = ShapeFull)
            .semantics {
                role = Role.Switch
            },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = if (useWhiteStyle) Color.White.copy(alpha = 0.35f) else Accent,
            checkedBorderColor = if (useWhiteStyle) Color.White.copy(alpha = 0.30f) else Color.Transparent,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = SurfaceHigh,
            uncheckedBorderColor = Outline,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
            disabledCheckedTrackColor = (if (useWhiteStyle) Color.White else Accent).copy(alpha = 0.2f),
            disabledUncheckedThumbColor = TextTertiary,
            disabledUncheckedTrackColor = SurfaceLow,
            disabledUncheckedBorderColor = Outline.copy(alpha = 0.5f)
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppSwitchPreview() {
    AppSwitch(checked = true, onCheckedChange = {})
}
