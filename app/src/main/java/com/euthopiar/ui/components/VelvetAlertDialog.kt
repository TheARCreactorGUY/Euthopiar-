package com.euthopiar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modern Confirmation Dialog delegates seamlessly to [EuthopiarGlassDialog],
 * ensuring all dialogs across the application adhere to the frosted-glass design tokens.
 */
@Composable
fun VelvetAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String = "",
    confirmText: String = "Confirm",
    dismissText: String? = "Cancel",
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    itemPreview: (@Composable () -> Unit)? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest
) {
    EuthopiarGlassDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        icon = icon,
        isDestructive = isDestructive,
        itemPreview = itemPreview,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onDismissRequest = onDismissRequest
    )
}
