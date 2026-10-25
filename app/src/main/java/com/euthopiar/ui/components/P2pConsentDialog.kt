package com.euthopiar.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

/**
 * Frosted-glass Peer-to-Peer network and battery awareness consent dialog.
 * Wraps [EuthopiarGlassDialog] to educate users on decentralized torrent swarm streaming.
 */
@Composable
fun P2pConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "P2P Swarm Notice",
    message: String = "Peer-to-peer torrent streaming exchanges video chunks directly with decentralized swarm peers. This utilizes network bandwidth and may increase battery consumption.",
    confirmText: String = "Proceed",
    dismissText: String = "Cancel",
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    EuthopiarGlassDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        icon = Icons.Outlined.Share,
        modifier = modifier,
        hazeState = hazeState,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
