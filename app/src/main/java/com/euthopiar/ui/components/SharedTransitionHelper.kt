package com.euthopiar.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * No-op extension. Shared element poster transitions are intentionally disabled
 * in favor of standard, resilient crossfade and slide navigation transitions to prevent
 * visual artifacts with TMDB metadata bridging and layout clipping across screens.
 */
fun Modifier.sharedPosterElement(
    key: String? = null
): Modifier = this
