package com.euthopiar.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Profile management subpage screen that delegates directly to the redesigned [ProfileManagementSheet]
 * ("Who's watching?" screen) in compliance with the monochrome frosted glass design language.
 */
@Composable
fun ProfileSubpageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    ProfileManagementSheet(
        onDismiss = onBackClick,
        modifier = modifier
    )
}
