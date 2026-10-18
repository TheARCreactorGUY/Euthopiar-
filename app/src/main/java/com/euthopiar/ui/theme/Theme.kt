package com.euthopiar.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.flowOf

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = Background,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = TextPrimary,
    secondary = TextPrimary,
    onSecondary = Background,
    secondaryContainer = SurfaceLow,
    onSecondaryContainer = TextPrimary,
    tertiary = Accent,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceLow,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMid,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = Error
)

fun androidx.compose.material3.ColorScheme.toAmoled(): androidx.compose.material3.ColorScheme {
    return this.copy(
        background = androidx.compose.ui.graphics.Color.Black,
        surface = androidx.compose.ui.graphics.Color.Black,
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF0A0A0A)
    )
}

@Composable
fun rememberReduceMotion(): Boolean = LocalReduceMotion.current

@Composable
fun EuthopiarTheme(
    amoled: Boolean? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    val userReduceMotion by remember(context) {
        try {
            PreferencesRepository.getInstance(context.applicationContext).reduceMotion
        } catch (_: Exception) {
            flowOf(false)
        }
    }.collectAsState(initial = false)

    val systemReduceMotion = remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val reduceMotion = userReduceMotion || systemReduceMotion

    val preferenceAmoled by remember(context) {
        try {
            PreferencesRepository.getInstance(context.applicationContext).amoledMode
        } catch (_: Exception) {
            flowOf(true)
        }
    }.collectAsState(initial = true)

    val isAmoled = amoled ?: preferenceAmoled
    val colorScheme = if (isAmoled) DarkColorScheme.toAmoled() else DarkColorScheme

    val surfaces = remember(isAmoled) {
        if (isAmoled) {
            AppSurfaces(
                scrim = androidx.compose.ui.graphics.Color(0xCC000000),
                glassTint = androidx.compose.ui.graphics.Color(0xFA000000),
                outline = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                isAmoled = true
            )
        } else {
            AppSurfaces(
                scrim = OverlayScrim,
                glassTint = GlassSurfaceNavbar,
                outline = Outline,
                isAmoled = false
            )
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    val windowDimensions = rememberWindowDimensions()

    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalAmoledMode provides isAmoled,
        LocalAppSurfaces provides surfaces
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AdaptiveTypography.getTypography(windowDimensions.isTV),
            shapes = AppShapes,
            content = content
        )
    }
}
