package com.euthopiar.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.euthopiar.R
import com.euthopiar.ui.theme.*

/**
 * Clean, fluid, cinematic launch sequence inspired by Apple TV and Netflix.
 * Focuses on artwork-first minimalism: subtle scale + alpha reveal, smooth crossfade into Home.
 */
@Composable
fun CinematicSplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val masterClock = remember { Animatable(0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            onAnimationFinished()
        } else {
            masterClock.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1100,
                    easing = CubicBezierEasing(0.12f, 0.98f, 0.24f, 1.0f)
                )
            )
            onAnimationFinished()
        }
    }

    val progress = masterClock.value

    // Subtle scale (0.92 -> 1.0) and alpha (0 -> 1) over first 60% of duration
    val revealProgress = (progress / 0.65f).coerceIn(0f, 1f)
    val emblemAlpha = revealProgress
    val emblemScale = 0.92f + (0.08f * revealProgress)

    // Dissolve into Home over last 25%
    val dissolveProgress = ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
    val masterAlpha = 1f - dissolveProgress

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = masterAlpha
            }
            .background(Background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onAnimationFinished() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clean Logo Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = emblemScale
                        scaleY = emblemScale
                        alpha = emblemAlpha
                    }
                    .clip(ShapeL)
                    .background(SurfaceMid)
                    .border(HairlineWidth, Outline, ShapeL)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Euthopiar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ShapeM)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing24))

            // Minimalist Brand Typography
            Text(
                text = "EUTHOPIAR",
                style = AppTypography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.graphicsLayer {
                    alpha = emblemAlpha
                }
            )
        }
    }
}
