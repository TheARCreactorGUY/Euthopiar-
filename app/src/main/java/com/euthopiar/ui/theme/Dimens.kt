package com.euthopiar.ui.theme

import androidx.compose.ui.unit.dp

// ============================================================================
// Spacing & Layout Dimension Tokens
// ============================================================================
object Dimens {
    // Spacing scale (4, 8, 12, 16, 24, 32, 48)
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing48 = 48.dp

    // Screen horizontal margins
    val screenHorizontalMarginPhone = 16.dp
    val screenHorizontalMarginTablet = 24.dp
    val screenHorizontalMarginTV = 48.dp
    fun tvHorizontalMargin(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
        maxOf(48.dp, screenWidth * 0.05f)

    // Poster & Grid geometry
    val posterGap = 8.dp
    val posterWidthCompact = 125.dp
    val posterAspectRatio = 2f / 3f
    val backdropAspectRatio = 16f / 9f

    // Min touch target & component heights
    val minTouchTarget = 48.dp
    val buttonHeight = 48.dp
    val chipHeight = 36.dp
    val navBarHeight = 64.dp
    val navRailWidth = 72.dp

    // Hairline & Border constants
    val HairlineWidth = 1.dp

    // Icon size scale
    val iconS = 16.dp
    val iconM = 20.dp
    val iconL = 24.dp
    val iconXL = 32.dp
}

// Top-level aliases for convenient direct access
val HairlineWidth = Dimens.HairlineWidth
val iconS = Dimens.iconS
val iconM = Dimens.iconM
val iconL = Dimens.iconL
val iconXL = Dimens.iconXL

