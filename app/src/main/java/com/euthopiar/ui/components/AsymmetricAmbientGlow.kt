package com.euthopiar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.euthopiar.ui.util.PosterGlowColors

/**
 * Renders an organic, asymmetrical ambient backlight glow using overlapping,
 * off-center radial gradient blobs placed behind the card with extended range and vivid luminosity.
 *
 * Adheres strictly to the user's specification:
 * - No simple box-shadow or gradient div sized to the card.
 * - Blobs are significantly larger than the card (e.g. 310dp, 290dp, 250dp, 220dp) for extended range.
 * - 4 overlapping colored blobs of different sizes instead of one uniform shape.
 * - Blobs are positioned off-center so light spills out asymmetrically.
 * - Edges fade smoothly to transparent via radial gradients and high Gaussian blur (zero visible borders).
 * - Layered strictly behind the card (zIndex = 0f).
 */
@Composable
fun AsymmetricAmbientGlow(
    colors: PosterGlowColors,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1.0f
) {
    val animatedColor1 by animateColorAsState(colors.color1, tween(500), label = "glowC1")
    val animatedColor2 by animateColorAsState(colors.color2, tween(500), label = "glowC2")
    val animatedColor3 by animateColorAsState(colors.color3, tween(500), label = "glowC3")

    Box(
        modifier = modifier.zIndex(0f),
        contentAlignment = Alignment.Center
    ) {
        // Blob 1: Large Upper-Left Plume (Dominant top-left aura - 30% reduced range)
        RadialGlowBlob(
            color = animatedColor1,
            size = (217 * scaleFactor).dp,
            offsetX = (-25 * scaleFactor).dp,
            offsetY = (-35 * scaleFactor).dp,
            blurRadius = (28 * scaleFactor).coerceAtLeast(15f).dp,
            alpha = 1.0f
        )

        // Blob 2: Medium Lower-Right Plume (Dominant bottom-right aura - 30% reduced range)
        RadialGlowBlob(
            color = animatedColor2,
            size = (203 * scaleFactor).dp,
            offsetX = (27 * scaleFactor).dp,
            offsetY = (38 * scaleFactor).dp,
            blurRadius = (27 * scaleFactor).coerceAtLeast(14f).dp,
            alpha = 0.96f
        )

        // Blob 3: Accent Lower-Left / Center Plume (Rich bottom grounding aura - 30% reduced range)
        RadialGlowBlob(
            color = animatedColor3,
            size = (175 * scaleFactor).dp,
            offsetX = (-14 * scaleFactor).dp,
            offsetY = (42 * scaleFactor).dp,
            blurRadius = (24 * scaleFactor).coerceAtLeast(12f).dp,
            alpha = 0.92f
        )

        // Blob 4: Accent Upper-Right Plume (Completes asymmetric halo - 30% reduced range)
        RadialGlowBlob(
            color = animatedColor1,
            size = (154 * scaleFactor).dp,
            offsetX = (20 * scaleFactor).dp,
            offsetY = (-28 * scaleFactor).dp,
            blurRadius = (22 * scaleFactor).coerceAtLeast(11f).dp,
            alpha = 0.85f
        )
    }
}

@Composable
private fun RadialGlowBlob(
    color: Color,
    size: Dp,
    offsetX: Dp,
    offsetY: Dp,
    blurRadius: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .offset(x = offsetX, y = offsetY)
            .blur(blurRadius)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to color.copy(alpha = alpha),
                            0.48f to color.copy(alpha = alpha * 0.95f),
                            0.75f to color.copy(alpha = alpha * 0.78f),
                            0.90f to color.copy(alpha = alpha * 0.42f),
                            0.97f to color.copy(alpha = alpha * 0.14f),
                            1.0f to Color.Transparent
                        )
                    )
                )
        )
    }
}
