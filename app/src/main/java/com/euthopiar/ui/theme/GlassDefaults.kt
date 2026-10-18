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
    const val TintAlpha: Float = 0.10f
    val TintColor: Color = Color.White.copy(alpha = TintAlpha)
    const val NoiseFactor: Float = 0.03f

    val BorderBrush: Brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val NavbarFallbackColor: Color = Color(0xCC1A1A1F)
    val PillFallbackColor: Color = Color.White.copy(alpha = 0.16f)
}
