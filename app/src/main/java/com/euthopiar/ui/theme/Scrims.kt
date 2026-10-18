package com.euthopiar.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Standard gradient and scrim overlays across the application.
 * All scrims and gradients derive from Background and OverlayScrim tokens.
 */
val HeroBottomGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Background.copy(alpha = 0.50f),
        0.25f to Color.Transparent,
        0.55f to Color.Transparent,
        0.75f to Background.copy(alpha = 0.60f),
        0.90f to Background.copy(alpha = 0.90f),
        1.00f to Background
    )
)

val PlayerTopScrim = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.Black.copy(alpha = 0.64f),
        0.40f to Color.Black.copy(alpha = 0.38f),
        0.70f to Color.Black.copy(alpha = 0.14f),
        1.00f to Color.Transparent
    )
)

val PlayerBottomScrim = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.Transparent,
        0.30f to Color.Black.copy(alpha = 0.20f),
        0.65f to Color.Black.copy(alpha = 0.60f),
        1.00f to Color.Black.copy(alpha = 0.88f)
    )
)

val DetailsBackdropGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Background.copy(alpha = 0.40f),
        0.3f to Color.Transparent,
        0.65f to Background.copy(alpha = 0.60f),
        1.0f to Background
    )
)
