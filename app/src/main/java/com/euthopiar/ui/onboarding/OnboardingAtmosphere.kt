package com.euthopiar.ui.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.Background

/**
 * Clean monochrome calm obsidian atmosphere for onboarding.
 * Pure dark aesthetic (#0B0B0F) with a subtle top graphite luminance sheen.
 * Completely eliminates colored tints and harsh seam cutoffs.
 */
fun Modifier.onboardingTwilightBackground(): Modifier = drawWithCache {
    val linearBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF14151C), // Deep subtle graphite luminance at top
            Color(0xFF0E0F14),
            Background,        // 0xFF0B0B0F
            Background,
        ),
        startY = 0f,
        endY = size.height * 0.40f
    )

    onDrawBehind {
        drawRect(color = Background)
        drawRect(brush = linearBrush)
    }
}

/**
 * Directional vertical specular glass sheen border for floating cards.
 */
fun Modifier.onboardingCardBorder(
    cornerRadius: Dp = 16.dp,
    strokeWidth: Dp = 1.dp
): Modifier = border(
    width = strokeWidth,
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f)
        )
    ),
    shape = RoundedCornerShape(cornerRadius)
)
