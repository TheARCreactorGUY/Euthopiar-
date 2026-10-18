package com.euthopiar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppSurfaces(
    val scrim: Color,
    val glassTint: Color,
    val outline: Color,
    val isAmoled: Boolean,
)

val LocalAppSurfaces = staticCompositionLocalOf<AppSurfaces> {
    AppSurfaces(
        scrim = Color(0x99000000),
        glassTint = Color(0xD90E1118),
        outline = Color(0x1AFFFFFF),
        isAmoled = false
    )
}

// ============================================================================
// Semantic Color Tokens (Calm Dark Design System)
// ============================================================================

val Background = Color(0xFF0B0B0F)
val SurfaceLow = Color(0xFF14141A)     // Cards, content rows, poster surfaces
val SurfaceMid = Color(0xFF1C1C24)     // Bottom sheets, dialogs, search inputs
val SurfaceHigh = Color(0xFF26262F)    // Selected pills, pressed surfaces
val OverlayScrim = Color(0x99000000)   // 60% black scrim
val Outline = Color(0x1AFFFFFF)        // 10% white hairline border
val BorderSubtle = Outline


val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3BD)  // WCAG AA on Background (>= 4.5:1)
val TextTertiary = Color(0xFF8A8A96)   // Captions, subtle metadata (>= 3:1)

val Accent = Color(0xFFE50914)         // Single vibrant accent for progress & active indicators
val Success = Color(0xFF34C759)
val Warning = Color(0xFFFF9F0A)
val Error = Color(0xFFFF453A)

// Signal Health & Player Modernization Tokens
val SignalGood = Color(0xFF3DDC97)
val SignalFair = Color(0xFFF5B83D)
val SignalPoor = Color(0xFFFF5A5F)
val GoldAccent = Color(0xFFD4AF37)

val SpecularBorderBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.04f)
    )
)

// Skeleton Loading Tokens
val SkeletonBase = Color(0xFF0F1318)
val SkeletonHighlight = Color(0xFF1B222B)

// Brand Rating Tokens
val ImdbGold = Color(0xFFF5C518)
val RottenTomatoesFresh = Color(0xFFFA320A)
val RottenTomatoesRotten = Color(0xFF00B020)


// Overlays (Glass/blur reserved only for floating navigation, player HUD, sheets over video)
val GlassSurfaceNavbar = Color(0xD90E1118)
val GlassBorder = Outline
val RefractiveGlassBorder = Outline

// ============================================================================
// Deprecated Legacy Aliases (Supported during incremental screen migrations)
// ============================================================================
@Deprecated("Use Background instead", ReplaceWith("Background"))
val VelvetBlack = Background

@Deprecated("Use SurfaceLow instead", ReplaceWith("SurfaceLow"))
val ObsidianDark = SurfaceLow

@Deprecated("Use SurfaceLow instead", ReplaceWith("SurfaceLow"))
val GlassSurface = SurfaceLow

@Deprecated("Use SurfaceHigh instead", ReplaceWith("SurfaceHigh"))
val GlassSurfacePill = SurfaceHigh

@Deprecated("Use TextPrimary instead", ReplaceWith("TextPrimary"))
val PureWhite = TextPrimary

@Deprecated("Use Background instead", ReplaceWith("Background"))
val PureBlack = Color(0xFF000000)

@Deprecated("Use TextTertiary instead", ReplaceWith("TextTertiary"))
val TextMuted = TextTertiary

@Deprecated("Use Warning or TextPrimary instead", ReplaceWith("Warning"))
val StarGold = Color(0xFFFFD700)

@Deprecated("Use Background instead", ReplaceWith("Background"))
val BackgroundDark = Background

@Deprecated("Use SurfaceLow instead", ReplaceWith("SurfaceLow"))
val SurfaceDark = SurfaceLow

@Deprecated("Use SurfaceMid instead", ReplaceWith("SurfaceMid"))
val SurfaceGlass = SurfaceMid

@Deprecated("Use Outline instead", ReplaceWith("Outline"))
val SurfaceGlassBorder = Outline

@Deprecated("Use Error instead", ReplaceWith("Error"))
val CrimsonAlert = Error

@Deprecated("Use Success instead", ReplaceWith("Success"))
val EmeraldGreen = Success
