package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.SurfaceMid
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.pressable

import android.os.Build
import com.euthopiar.ui.theme.GlassDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Secondary Action Button (e.g. "Details", "Add to Watchlist", "Download").
 * Calm SurfaceMid background with hairline outline and 48dp height, or frosted glass when hazeState is provided.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    hazeState: HazeState? = null,
) {
    val backgroundModifier = if (hazeState != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.hazeEffect(state = hazeState) {
                blurRadius = GlassDefaults.BlurRadius
                tints = listOf(HazeTint(GlassDefaults.TintColor))
                fallbackTint = HazeTint(GlassDefaults.PillFallbackColor)
                noiseFactor = GlassDefaults.NoiseFactor
            }
        } else {
            Modifier.background(GlassDefaults.PillFallbackColor, ShapeFull)
        }
            .border(width = 1.dp, brush = GlassDefaults.BorderBrush, shape = ShapeFull)
    } else {
        Modifier
            .background(SurfaceMid)
            .border(1.dp, Outline, ShapeFull)
    }

    Row(
        modifier = modifier
            .height(Dimens.buttonHeight)
            .defaultMinSize(minWidth = 80.dp)
            .clip(ShapeFull)
            .then(backgroundModifier)
            .pressable(enabled = enabled, onClickLabel = text, onClick = onClick)
            .tvFocusable(shape = ShapeFull)
            .padding(horizontal = Dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative - button label provided by Text
                tint = TextPrimary,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(iconSize)
            )
        }
        Text(
            text = text,
            style = AppTypography.labelLarge.copy(fontSize = fontSize),
            fontWeight = fontWeight,
            color = TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
