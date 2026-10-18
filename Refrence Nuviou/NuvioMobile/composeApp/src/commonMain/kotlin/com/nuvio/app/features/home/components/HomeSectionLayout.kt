package com.nuvio.app.features.home.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun homeSectionHorizontalPaddingForWidth(maxWidthDp: Float): Dp =
    when {
        maxWidthDp >= 1440f -> 32.dp
        maxWidthDp >= 1024f -> 28.dp
        maxWidthDp >= 768f -> 24.dp
        else -> 16.dp
    }