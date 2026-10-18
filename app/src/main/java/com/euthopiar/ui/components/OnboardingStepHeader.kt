package com.euthopiar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography

/**
 * Shared onboarding step header component.
 * Displays navigation controls (Back, Skip) and a step counter with animated progress line.
 */
@Composable
fun OnboardingStepHeader(
    step: Int,
    total: Int,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val trackColor = remember { Color.White.copy(alpha = 0.12f) }
    val trackShape = remember { RoundedCornerShape(1.dp) }
    val textColor = remember { Color.White.copy(alpha = 0.70f) }
    val counterColor = remember { Color.White.copy(alpha = 0.50f) }

    val targetFraction = (step.toFloat() / total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "onboardingHeaderProgress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Navigation Buttons Row (Back & Skip)
        if (onBack != null || onSkip != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "Back",
                                onClick = onBack,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "Back",
                            style = AppTypography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = textColor,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                if (onSkip != null) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "Skip",
                                onClick = onSkip,
                            ),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "Skip",
                            style = AppTypography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = textColor,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Progress row: "0N / 0M" on left, thin animated progress line filling remainder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val stepText = String.format("%02d / %02d", step, total)
            Text(
                text = stepText,
                style = AppTypography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.14.em,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = counterColor,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(trackShape)
                    .background(trackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .fillMaxHeight()
                        .background(Accent),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun OnboardingStepHeaderPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0F)),
    ) {
        OnboardingStepHeader(
            step = 3,
            total = 3,
            onBack = {},
            onSkip = {},
        )
    }
}
