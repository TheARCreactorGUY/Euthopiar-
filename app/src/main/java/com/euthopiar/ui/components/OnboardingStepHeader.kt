package com.euthopiar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.onboarding.OnboardingProgressBar
import com.euthopiar.ui.onboarding.OnboardingTopRow
import dev.chrisbanes.haze.HazeState

/**
 * Shared onboarding step header component matching the new design language:
 * 1. Step Progress Bar: 3 thin segments across the content width, 2 dp tall, 4 dp gap, fully rounded.
 *    Current and completed steps: white 90%. Upcoming: white 18%.
 * 2. Top row (40 dp tall, 14 dp below progress bar): Glass back button on left (if onBack != null)
 *    and Skip chip on right (if onSkip != null).
 */
@Composable
fun OnboardingStepHeader(
    step: Int,
    total: Int = 3,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        OnboardingProgressBar(
            currentStep = step,
            totalSteps = total,
        )

        Spacer(modifier = Modifier.height(14.dp))

        OnboardingTopRow(
            showBackButton = onBack != null,
            onBack = { onBack?.invoke() },
            onSkip = { onSkip?.invoke() },
            hazeState = hazeState,
            isAmoled = isAmoled,
        )
    }
}
