package com.euthopiar.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.Accent
import kotlin.math.abs

/**
 * Liquid Jelly-inspired page indicator for the onboarding flow.
 * Uses real-time pager offset fraction to provide fluid elastic expansion and morphing.
 */
@Composable
fun OnboardingJellyIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isCurrent = pagerState.currentPage == index
            val offsetFraction = pagerState.currentPageOffsetFraction

            val targetWidth by remember(pagerState.currentPage, offsetFraction) {
                derivedStateOf {
                    val distance = abs((pagerState.currentPage + offsetFraction) - index)
                    when {
                        distance < 0.25f -> 26.dp
                        distance < 0.65f -> 18.dp
                        else -> 7.dp
                    }
                }
            }

            val animatedWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "jellyDotWidth_$index"
            )

            val dotColor = if (isCurrent) Accent else Color.White.copy(alpha = 0.22f)

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
