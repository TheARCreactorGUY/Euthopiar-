package com.euthopiar.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared styling constants for frosted-glass surfaces (FloatingNavbar and Hero "More Details" pill).
 */
object GlassDefaults {
    val BlurRadius: Dp = 24.dp
    const val TintAlpha: Float = 0.08f
    val TintColor: Color = Color.White.copy(alpha = TintAlpha)
    const val NoiseFactor: Float = 0.045f

    val BorderBrush: Brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.06f)
        )
    )

    val NavbarFallbackColor: Color = Color(0xCC1A1A1F)
    val PillFallbackColor: Color = Color.White.copy(alpha = 0.16f)
}
