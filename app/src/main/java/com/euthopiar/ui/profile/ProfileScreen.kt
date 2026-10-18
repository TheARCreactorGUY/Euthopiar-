package com.euthopiar.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.euthopiar.ui.settings.SettingsRootScreen

@Composable
fun ProfileScreen(
    onNavigateToPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsRootScreen(
        onNavigateToPage = onNavigateToPage,
        modifier = modifier
    )
}
