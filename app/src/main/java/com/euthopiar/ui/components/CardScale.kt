package com.euthopiar.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.preferences.CardScaleMode

val LocalCardScaleMode = compositionLocalOf { CardScaleMode.FLAGSHIP }

object CardDimensions {
    val BaseVerticalCardWidth: Dp = 135.dp
    val BaseHorizontalCardWidth: Dp = 230.dp
    val BaseHorizontalCardHeight: Dp = 130.dp

    fun verticalWidth(mode: CardScaleMode): Dp = (135f * mode.verticalFactor).dp
    fun horizontalWidth(mode: CardScaleMode): Dp = (230f * mode.horizontalFactor).dp
    fun horizontalHeight(mode: CardScaleMode): Dp = (130f * mode.horizontalFactor).dp

    /**
     * Horizontal gap between vertical 2:3 content cards across the 3 card sizes.
     * Prevents adjacent "Title · [IMDb] x.x/10" captions from running together.
     */
    fun posterGap(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 10.dp
        CardScaleMode.BALANCED -> 12.dp
        CardScaleMode.FLAGSHIP -> 14.dp
    }

    /**
     * Horizontal gap between 16:9 widescreen Continue Watching cards across the 3 card sizes.
     */
    fun horizontalCardGap(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 12.dp
        CardScaleMode.BALANCED -> 14.dp
        CardScaleMode.FLAGSHIP -> 16.dp
    }

    /**
     * Horizontal gap between Top 10 rank cards across the 3 card sizes.
     */
    fun topTenCardGap(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 14.dp
        CardScaleMode.BALANCED -> 16.dp
        CardScaleMode.FLAGSHIP -> 18.dp
    }

    /**
     * Corner radius for vertical 2:3 posters scaled proportionally to card size.
     */
    fun posterCornerRadius(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 9.dp
        CardScaleMode.BALANCED -> 11.dp
        CardScaleMode.FLAGSHIP -> 12.dp
    }

    /**
     * Vertical spacing between the bottom edge of a card poster and its title/rating line.
     */
    fun titleTopSpacing(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 7.dp
        CardScaleMode.BALANCED -> 9.dp
        CardScaleMode.FLAGSHIP -> 10.dp
    }

    /**
     * Dynamic title font size below content cards across the 3 card sizes.
     */
    fun titleFontSize(mode: CardScaleMode): TextUnit = when (mode) {
        CardScaleMode.COMPACT -> 13.sp
        CardScaleMode.BALANCED -> 14.5.sp
        CardScaleMode.FLAGSHIP -> 16.sp
    }

    /**
     * Dynamic title line height below content cards across the 3 card sizes.
     */
    fun titleLineHeight(mode: CardScaleMode): TextUnit = when (mode) {
        CardScaleMode.COMPACT -> 17.sp
        CardScaleMode.BALANCED -> 19.sp
        CardScaleMode.FLAGSHIP -> 21.sp
    }

    /**
     * Dynamic font size for the "x.x/10" IMDb rating text beside the title.
     */
    fun ratingFontSize(mode: CardScaleMode): TextUnit = when (mode) {
        CardScaleMode.COMPACT -> 12.sp
        CardScaleMode.BALANCED -> 13.5.sp
        CardScaleMode.FLAGSHIP -> 14.5.sp
    }

    /**
     * Dynamic height for the inline IMDb logo badge beside the title.
     */
    fun imdbLogoHeight(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 12.5.dp
        CardScaleMode.BALANCED -> 14.dp
        CardScaleMode.FLAGSHIP -> 15.5.dp
    }

    /**
     * Top spacing above a catalogue section header (inter-rail visual separation).
     * Follows the craft-floor law of proximity: substantially more space above a header
     * than between the header and its own cards below.
     */
    fun railTopSpacing(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 16.dp
        CardScaleMode.BALANCED -> 20.dp
        CardScaleMode.FLAGSHIP -> 24.dp
    }

    /**
     * Bottom breathing room at the foot of each catalogue rail.
     */
    fun railBottomSpacing(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 6.dp
        CardScaleMode.BALANCED -> 8.dp
        CardScaleMode.FLAGSHIP -> 10.dp
    }

    /**
     * Tight grouping space between a catalogue SectionHeader and its cards immediately below.
     */
    fun headerToCardsSpacing(mode: CardScaleMode): Dp = when (mode) {
        CardScaleMode.COMPACT -> 9.dp
        CardScaleMode.BALANCED -> 11.dp
        CardScaleMode.FLAGSHIP -> 13.dp
    }

    /**
     * Dynamic catalogue section header title font size across the 3 card sizes.
     */
    fun sectionTitleFontSize(mode: CardScaleMode): TextUnit = when (mode) {
        CardScaleMode.COMPACT -> 19.sp
        CardScaleMode.BALANCED -> 20.5.sp
        CardScaleMode.FLAGSHIP -> 22.sp
    }

    /**
     * Dynamic catalogue section header subtitle font size across the 3 card sizes.
     */
    fun sectionSubtitleFontSize(mode: CardScaleMode): TextUnit = when (mode) {
        CardScaleMode.COMPACT -> 12.sp
        CardScaleMode.BALANCED -> 12.5.sp
        CardScaleMode.FLAGSHIP -> 13.sp
    }

    /**
     * Dynamic accent bar height on SectionHeader (AccentBar style) across the 3 card sizes.
     */
    fun sectionAccentBarHeight(mode: CardScaleMode, hasSubtitle: Boolean): Dp = when (mode) {
        CardScaleMode.COMPACT -> if (hasSubtitle) 36.dp else 20.dp
        CardScaleMode.BALANCED -> if (hasSubtitle) 39.dp else 22.dp
        CardScaleMode.FLAGSHIP -> if (hasSubtitle) 42.dp else 24.dp
    }
}
