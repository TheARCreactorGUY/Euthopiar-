package com.euthopiar.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.euthopiar.data.preferences.CardScaleMode

val LocalCardScaleMode = compositionLocalOf { CardScaleMode.FLAGSHIP }

object CardDimensions {
    val BaseVerticalCardWidth: Dp = 135.dp
    val BaseHorizontalCardWidth: Dp = 230.dp
    val BaseHorizontalCardHeight: Dp = 130.dp

    fun verticalWidth(mode: CardScaleMode): Dp = (135f * mode.verticalFactor).dp
    fun horizontalWidth(mode: CardScaleMode): Dp = (230f * mode.horizontalFactor).dp
    fun horizontalHeight(mode: CardScaleMode): Dp = (130f * mode.horizontalFactor).dp
}
