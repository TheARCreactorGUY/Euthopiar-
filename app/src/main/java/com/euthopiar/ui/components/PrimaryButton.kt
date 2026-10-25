package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.playButtonSpringPress
import com.euthopiar.ui.theme.pressable

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Flagship Primary Action Button (e.g. "Play", "Watch Now").
 * High-contrast solid pill with 48dp height, bold typography, and spring press feedback.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isAccentRed: Boolean = false,
    enabled: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    val backgroundColor = if (isAccentRed) Accent else TextPrimary
    val contentColor = if (isAccentRed) TextPrimary else Background

    val interactionModifier = if (icon != null) {
        Modifier.playButtonSpringPress(enabled = enabled, onClickLabel = text, onClick = onClick)
    } else {
        Modifier.pressable(enabled = enabled, onClickLabel = text, onClick = onClick)
    }

    Row(
        modifier = modifier
            .then(interactionModifier)
            .height(Dimens.buttonHeight)
            .defaultMinSize(minWidth = 120.dp)
            .clip(ShapeFull)
            .background(backgroundColor)
            .tvFocusable(shape = ShapeFull)
            .padding(horizontal = Dimens.spacing24),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative - button label provided by Text
                tint = contentColor,
                modifier = Modifier
                    .padding(end = Dimens.spacing8)
                    .size(iconSize)
            )
        }
        Text(
            text = text,
            style = AppTypography.labelLarge.copy(fontSize = fontSize),
            fontWeight = fontWeight,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
