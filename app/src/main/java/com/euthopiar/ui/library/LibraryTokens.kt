package com.euthopiar.ui.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily

object LibraryColors {
    val Background = Color(0xFF0B0B0F)
    val Surface = Color(0xFF14151F)
    val SurfaceAlt = Color(0xFF1B1C28)
    val Crimson = Color(0xFFE50914)
    val Gold = Color(0xFFD4AF37)
    val TextWhite = Color(0xFFFFFFFF)
    val TextMutedReadable = Color(0xFF9A9BA8)
    val TextMutedDecorative = Color(0xFF6B6C78)
    val Hairline = Color.White.copy(alpha = 0.14f)
    val ImageOverlay = Color(0xFF0B0B0F).copy(alpha = 0.55f)

    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFallback = Color(0xFF14151F).copy(alpha = 0.94f)
    val GlassBorder = Color.White.copy(alpha = 0.16f)
    val GlassBorderActive = Color.White.copy(alpha = 0.32f)
    val TextMuted55 = Color.White.copy(alpha = 0.55f)
    val TextMuted45 = Color.White.copy(alpha = 0.45f)
    val ProgressTrack = Color.White.copy(alpha = 0.14f)
    val ProgressFill = Color.White.copy(alpha = 0.85f)

    val HazeIndigo = Color(0xFF2A2859)
    val HazeRose = Color(0xFF5A2542)
    val HazeViolet = Color(0xFF3B2358)
    val HazeTeal = Color(0xFF1A454D)
}

object LibraryTypography {
    // Screen title "Library": 30sp, Medium, tight tracking
    val screenTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.04).em,
        color = LibraryColors.TextWhite
    )

    // Storage hero numeral: about 54sp, Medium, very tight tracking
    val storageHeroNumeral = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.05).em,
        color = LibraryColors.TextWhite
    )

    val storageHeroUnit = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        color = LibraryColors.TextMutedReadable
    )

    // Row file size numeral: 22sp, Medium, tight tracking
    val rowSizeNumeral = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.04).em,
        color = LibraryColors.TextWhite
    )

    val rowSizeUnit = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = LibraryColors.TextMutedReadable
    )

    // Row title: 15sp Medium
    val rowTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.02).em,
        color = LibraryColors.TextWhite
    )

    // Row sub-line: 12sp muted
    val rowSubline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = LibraryColors.TextMutedReadable
    )

    // Poster grid title: 13sp Medium
    val gridTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = (-0.02).em,
        color = LibraryColors.TextWhite
    )

    // Poster grid sub-line: 11sp muted
    val gridSubline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = LibraryColors.TextMutedReadable
    )

    // Section labels: 11sp muted, small positive tracking
    val sectionLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.em,
        color = LibraryColors.TextMutedReadable
    )

    // Tab labels: 13sp, Medium when selected, Normal when unselected
    val tabSelected = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.01).em,
        color = LibraryColors.TextWhite
    )

    val tabUnselected = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.01).em,
        color = LibraryColors.TextMutedReadable
    )

    // Continue card title: 28sp Medium tight tracking
    val continueCardTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.04).em,
        color = LibraryColors.TextWhite
    )

    // Playlist banner title: 20sp Medium tight tracking
    val playlistBannerTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.03).em,
        color = LibraryColors.TextWhite
    )

    // Redesigned Page headline ("Library"): 34 sp, medium, tight line height
    val pageHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.03).em,
        color = Color.White
    )

    // Storage hero numeral 64sp medium
    val storageHeroNumeral64 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.03).em,
        color = Color.White
    )

    val storageHeroUnit18 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        color = Color.White.copy(alpha = 0.50f)
    )

    // Section labels 11sp uppercase, wide tracking
    val sectionLabel11 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.em,
        color = Color.White.copy(alpha = 0.45f)
    )

    // Title 15sp Medium
    val title15 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.White
    )

    // Title 14sp
    val title14 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = Color.White
    )

    // Metadata 11-12sp, 55% white
    val metadata11 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        color = Color.White.copy(alpha = 0.55f)
    )

    // Tab labels 14sp
    val tab14Active = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = Color.White
    )

    val tab14Inactive = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = Color.White.copy(alpha = 0.50f)
    )

    // Continue card title 26sp
    val continueCardTitle26 = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.03).em,
        color = Color.White
    )
}

object LibraryDimens {
    val hairline = 0.5.dp
    val underlineHeight = 1.5.dp
    val minTouchTarget = 48.dp
    val progressBarHeight = 2.dp
    val toggleWidth = 34.dp
    val toggleHeight = 20.dp
    val toggleKnobSize = 16.dp
    val dropdownHeight = 32.dp
    val dropdownRadius = 12.dp
    val chipRadius = 10.dp
    val controlRadius = 14.dp
    val iconTileRadius = 14.dp
    val storageMeterHeight = 3.dp
    val progressRingSize = 30.dp
    val progressRingStroke = 2.dp
    val badgeDiscSize = 22.dp
    val selectCircleSize = 22.dp
    val rowThumbWidth = 44.dp
    val rowThumbHeight = 64.dp
    val rowThumbRadius = 6.dp
    val posterCornerRadius = 10.dp
    val continueCardHeight = 148.dp
    val continueCardRadius = 18.dp
    val bannerHeight = 210.dp
    val bannerRadius = 14.dp
    val detailMosaicHeight = 220.dp
    val detailThumbWidth = 36.dp
    val detailThumbHeight = 52.dp
    val detailThumbRadius = 5.dp
}

/**
 * Sanitizes titles across Library tabs by replacing '+' with space and trimming whitespace.
 */
fun sanitizeTitle(title: String?): String {
    if (title.isNullOrBlank()) return ""
    return title.replace('+', ' ').trim().replace(Regex("\\s+"), " ")
}
