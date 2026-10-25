package com.euthopiar.ui.onboarding

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Onboarding Design Tokens & Atmosphere.
 * Bold, minimal, modern, cinematic design language.
 */
object OnboardingTokens {
    val DarkBackground = Color(0xFF0B0B0F)
    val AmoledBackground = Color(0xFF000000)

    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFillPressed = Color.White.copy(alpha = 0.10f)
    val GlassFallbackFill = Color.White.copy(alpha = 0.12f)
    val GlassHairlineBorder = Color.White.copy(alpha = 0.16f)
    val HairlineDivider = Color.White.copy(alpha = 0.14f)

    // Soft Haze Atmosphere Blobs (muted indigo, rose, teal at 20-30% opacity)
    val HazeIndigo = Color(0xFF262D4A)
    val HazeRose = Color(0xFF4A2635)
    val HazeTeal = Color(0xFF1E3A3A)

    val TextDarkButton = Color(0xFF0B0B0F)
    val TextMuted55 = Color.White.copy(alpha = 0.55f)
    val TextMuted50 = Color.White.copy(alpha = 0.50f)
    val TextMuted45 = Color.White.copy(alpha = 0.45f)
    val TextMuted70 = Color.White.copy(alpha = 0.70f)
    val TextWhite90 = Color.White.copy(alpha = 0.90f)
    val TextWhite18 = Color.White.copy(alpha = 0.18f)

    val HairlineWidth = 0.5.dp
}

/**
 * Draws the persistent background behind onboarding content.
 * Normal dark mode: near-black page background plus soft blurred haze blobs
 * (muted indigo, rose, teal at 20-30% opacity) that never scroll with content.
 * AMOLED mode: pure #000000 and haze is completely OFF.
 */
@Composable
fun Modifier.onboardingBackground(isAmoled: Boolean = LocalAmoledMode.current): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingAmbientHaze")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hazeDrift1",
    )

    val drift2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hazeDrift2",
    )

    val baseColor = if (isAmoled) OnboardingTokens.AmoledBackground else OnboardingTokens.DarkBackground

    return this
        .background(baseColor)
        .drawBehind {
            if (!isAmoled) {
                val effectiveDrift1 = if (reduceMotion) 0f else drift1
                val effectiveDrift2 = if (reduceMotion) 0f else drift2
                val maxDim = maxOf(size.width, size.height)

                // 1. Soft Muted Indigo Haze Blob near Upper-Right (24% opacity)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to OnboardingTokens.HazeIndigo.copy(alpha = 0.26f),
                        0.25f to OnboardingTokens.HazeIndigo.copy(alpha = 0.16f),
                        0.55f to OnboardingTokens.HazeIndigo.copy(alpha = 0.06f),
                        0.80f to OnboardingTokens.HazeIndigo.copy(alpha = 0.015f),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.90f + effectiveDrift1, size.height * 0.15f + effectiveDrift2),
                        radius = maxDim * 1.10f,
                    ),
                )

                // 2. Soft Muted Rose Haze Blob near Lower-Left (22% opacity)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to OnboardingTokens.HazeRose.copy(alpha = 0.22f),
                        0.28f to OnboardingTokens.HazeRose.copy(alpha = 0.13f),
                        0.55f to OnboardingTokens.HazeRose.copy(alpha = 0.05f),
                        0.80f to OnboardingTokens.HazeRose.copy(alpha = 0.01f),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.10f + effectiveDrift2, size.height * 0.80f + effectiveDrift1),
                        radius = maxDim * 1.05f,
                    ),
                )

                // 3. Subtle Muted Teal Haze Blob near Center-Left (20% opacity)
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to OnboardingTokens.HazeTeal.copy(alpha = 0.20f),
                        0.30f to OnboardingTokens.HazeTeal.copy(alpha = 0.10f),
                        0.60f to OnboardingTokens.HazeTeal.copy(alpha = 0.04f),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.20f + effectiveDrift1, size.height * 0.40f - effectiveDrift2),
                        radius = maxDim * 0.90f,
                    ),
                )
            }
        }
}

/**
 * Standard frosted glass surface helper for onboarding controls.
 * - White at ~7% fill
 * - Background blur ~18 dp via Haze (API 31+)
 * - 0.5 dp hairline border at white 16%
 * - Low-end fallback: opaque fill (white 12%), no blur
 */
fun Modifier.onboardingGlassSurface(
    shape: Shape,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
    customFill: Color = OnboardingTokens.GlassFill,
    fallbackFill: Color = OnboardingTokens.GlassFallbackFill,
    borderAlpha: Float = 0.16f,
): Modifier {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null && !isAmoled

    return this
        .clip(shape)
        .then(
            if (canBlur) {
                Modifier.hazeEffect(state = hazeState!!) {
                    blurRadius = 18.dp
                    tints = listOf(HazeTint(customFill))
                    fallbackTint = HazeTint(fallbackFill)
                }
            } else {
                Modifier.background(fallbackFill, shape)
            },
        )
        .border(
            width = OnboardingTokens.HairlineWidth,
            color = Color.White.copy(alpha = borderAlpha),
            shape = shape,
        )
}

/**
 * Backward compatibility extension for existing callers.
 */
fun Modifier.onboardingTwilightBackground(): Modifier = this.drawBehind {
    drawRect(color = OnboardingTokens.DarkBackground)
}

/**
 * Directional vertical specular glass sheen border for floating cards.
 */
fun Modifier.onboardingCardBorder(
    cornerRadius: Dp = 16.dp,
    strokeWidth: Dp = OnboardingTokens.HairlineWidth,
): Modifier = border(
    width = strokeWidth,
    color = OnboardingTokens.GlassHairlineBorder,
    shape = RoundedCornerShape(cornerRadius),
)
