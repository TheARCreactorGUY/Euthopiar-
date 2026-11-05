package com.euthopiar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppStatusModal(
    title: String,
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    destructive: Boolean = false,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!isVisible) return

    EuthopiarGlassDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        isDestructive = destructive,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
