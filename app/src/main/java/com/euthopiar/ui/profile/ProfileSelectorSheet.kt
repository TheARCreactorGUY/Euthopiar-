package com.euthopiar.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Legacy profile selector sheet forwarding to [ProfileManagementSheet].
 */
@Composable
fun ProfileSelectorSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileManagementSheet(
        onDismiss = onDismiss,
        modifier = modifier
    )
}
