package com.euthopiar.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Gold-standard frosted glassmorphism tokens matching the Home Page Update Dialog (UpdateInfoDialog).
 *
 * Characteristics:
 * 1. Blur Radius: 24dp Haze blur sampling the underlying screen.
 * 2. Tonal Depth: Obsidian dark 85% alpha (#0F1219) in Dark mode, 92% alpha (#000000) in AMOLED mode.
 * 3. Hairline Specular Border: 0.5dp at 16% white alpha.
 * 4. Hairline Divider: 0.5dp at 10% white alpha.
 * 5. Corner Geometry: 28dp radius for dialogs and sheets.
 * 6. Typography: 20sp Bold (96% white) title, 13sp Normal (54% white) subtitle, 13.5sp Normal (72% white) body.
 * 7. Action Button: 48dp solid white pill (#FFFFFF) with dark near-black text (#0B0B0F).
 */
object EuthopiarGlassTokens {
    val BlurRadius: Dp = 24.dp
    val RadiusDialog: Dp = 28.dp
    val RadiusSheet: Dp = 28.dp
    val RadiusPill: Dp = 24.dp

    val HairlineWidth: Dp = 0.5.dp
    val HairlineBorder: Color = Color.White.copy(alpha = 0.16f)
    val HairlineDivider: Color = Color.White.copy(alpha = 0.10f)

    const val ScrimAlpha: Float = 0.65f

    // Tonal fills
    val BaseDark: Color = Color(0xFF0F1219)
    val TextDark: Color = Color(0xFF0B0B0F)

    fun cardFill(isAmoled: Boolean): Color =
        if (isAmoled) Color.Black.copy(alpha = 0.92f) else BaseDark.copy(alpha = 0.85f)

    fun fallbackFill(isAmoled: Boolean): Color =
        if (isAmoled) Color.Black.copy(alpha = 0.96f) else BaseDark.copy(alpha = 0.96f)

    // Text colors
    val TextWhite96: Color = Color.White.copy(alpha = 0.96f)
    val TextWhite82: Color = Color.White.copy(alpha = 0.82f)
    val TextWhite72: Color = Color.White.copy(alpha = 0.72f)
    val TextWhite54: Color = Color.White.copy(alpha = 0.54f)
    val TextWhite42: Color = Color.White.copy(alpha = 0.42f)

    // Typography styles
    val HeaderTitle: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        color = TextWhite96
    )

    val HeaderSubtitle: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = TextWhite54
    )

    val SectionHeader: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 21.sp,
        color = TextWhite96
    )

    val BodyText: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        color = TextWhite72
    )

    val PrimaryButtonText: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        color = TextDark
    )

    val SecondaryButtonText: TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        color = TextWhite72
    )
}

/**
 * Standard modifier for modal glass cards (Dialogs & Sheets) adhering to the UpdateInfoDialog formula.
 */
fun Modifier.euthopiarGlassCard(
    shape: Shape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog),
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
    borderWidth: Dp = EuthopiarGlassTokens.HairlineWidth,
    borderColor: Color = EuthopiarGlassTokens.HairlineBorder
): Modifier {
    val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallback = EuthopiarGlassTokens.fallbackFill(isAmoled)

    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = EuthopiarGlassTokens.BlurRadius
                    tints = listOf(HazeTint(cardFill))
                    fallbackTint = HazeTint(fallback)
                }
            } else {
                Modifier.background(fallback)
            }
        )
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape
        )
}
