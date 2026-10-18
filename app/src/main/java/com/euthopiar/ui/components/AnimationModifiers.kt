package com.euthopiar.ui.components

import androidx.compose.ui.Modifier

/**
 * Obsidian animated shimmer extension, delegating directly to the token-compliant,
 * reduce-motion-aware, shared-clock [shimmerEffect].
 */
fun Modifier.obsidianShimmer(visible: Boolean = true): Modifier = shimmerEffect(enabled = visible)
