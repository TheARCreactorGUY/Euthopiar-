package com.euthopiar.ui.details.downloads

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Editorial loading skeleton screen:
 * - Shimmer sweeps left-to-right over 1.2s on repeat (white @ 8% shimmer).
 * - A hero block (22dp radius, 160dp tall).
 * - Three row skeletons made of two stacked bars each.
 */
@Composable
fun DownloadSkeleton(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberEditorialShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Hero block skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(DownloadTokens.heroCornerRadius))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Three row skeletons
        repeat(3) { index ->
            RowSkeleton(shimmerBrush = shimmerBrush)
            if (index < 2) {
                HorizontalDivider(
                    thickness = DownloadTokens.hairlineHeight,
                    color = DownloadTokens.hairline
                )
            }
        }
    }
}

@Composable
private fun RowSkeleton(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DownloadTokens.rowMinHeight)
            .padding(vertical = DownloadTokens.rowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left stacked bars
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(shimmerBrush)
            )
        }

        // Right stacked bars
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(shimmerBrush)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Icon placeholder
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(shimmerBrush)
        )
    }
}

@Composable
fun rememberEditorialShimmerBrush(): Brush {
    val reduceMotion = isReduceMotion()
    if (reduceMotion) {
        return Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val bandWidthPx = with(density) { 320.dp.toPx() }

    val transition = rememberInfiniteTransition(label = "editorialShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -bandWidthPx,
        targetValue = screenWidthPx + bandWidthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DownloadTokens.SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "editorialShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + bandWidthPx, 0f)
    )
}
