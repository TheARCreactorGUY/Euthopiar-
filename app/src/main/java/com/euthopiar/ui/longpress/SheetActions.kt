package com.euthopiar.ui.longpress

/**
 * Standard callback action set available across all long-press sheet styles.
 */
data class SheetActions(
    val onPlay: () -> Unit,
    val onResume: () -> Unit = onPlay,
    val onToggleWatchlist: () -> Unit,
    val onReact: (UserReaction) -> Unit = {},
    val onShare: () -> Unit,
    val onDownload: () -> Unit = {},
    val onOpenDetails: () -> Unit,
    val onNotInterested: () -> Unit = {},
    val onAddToPlaylist: (() -> Unit)? = null
)
