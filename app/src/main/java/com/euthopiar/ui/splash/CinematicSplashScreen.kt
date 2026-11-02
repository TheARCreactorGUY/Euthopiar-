package com.euthopiar.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Legacy splash screen entry point.
 * Forwarded to [AppIgnitionSequence] for flagship shared-geometry wordmark ignition.
 */
@Deprecated("Use AppIgnitionSequence for fluid shared-geometry wordmark ignition")
@Composable
fun CinematicSplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppIgnitionSequence(
        onIgnitionFinished = onAnimationFinished,
        modifier = modifier
    )
}
