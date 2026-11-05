package com.euthopiar.ui.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.theme.LocalReduceMotion

/**
 * 22dp circle selection indicator:
 * - Fluid state morphing: background color and check alpha blend seamlessly.
 * - Unselected: hairline circle (35% white border over obsidian depth).
 * - Selected: solid white disc with high-contrast dark check (#0B0B0F).
 */
@Composable
fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.92f,
        animationSpec = if (reduceMotion) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "selectSpring"
    )

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF0B0B0F).copy(alpha = 0.65f),
        animationSpec = if (reduceMotion) tween(0) else tween(160),
        label = "selectBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.35f),
        animationSpec = if (reduceMotion) tween(0) else tween(160),
        label = "selectBorder"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(120),
        label = "checkAlpha"
    )

    Box(
        modifier = modifier
            .size(LibraryDimens.selectCircleSize)
            .scale(scale)
            .clip(CircleShape)
            .background(animatedBg)
            .border(1.dp, animatedBorderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checkAlpha > 0f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF0B0B0F),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { alpha = checkAlpha }
            )
        }
    }
}
