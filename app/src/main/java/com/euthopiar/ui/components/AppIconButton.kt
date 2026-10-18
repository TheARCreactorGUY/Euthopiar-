package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.pressable

/**
 * Standard accessible icon button with minimum 48dp touch target,
 * non-nullable contentDescription for screen readers, and TV focus support.
 */
@Composable
fun AppIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TextPrimary,
    backgroundColor: Color = Color.Transparent,
    shape: Shape = ShapeFull,
    iconSize: Dp = Dimens.iconL
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(Dimens.minTouchTarget)
            .clip(shape)
            .background(backgroundColor)
            .tvFocusable(shape = shape)
            .pressable(enabled = enabled, onClick = onClick)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null, // Set on outer container for accurate accessibility tree
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppIconButtonPreview() {
    AppIconButton(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = "Play",
        onClick = {}
    )
}
