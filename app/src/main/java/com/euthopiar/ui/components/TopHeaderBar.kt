package com.euthopiar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions

import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.GlassDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Redesigned Top Header Bar matching Part 2: Top Header (over the hero).
 *
 * - Left: "EUTHOPIAR" wordmark at 15sp medium with wide 0.2em letter spacing.
 * - Right: Profile avatar in a 38dp round glass ring with 0.5dp hairline border and 48dp touch target.
 * - Top scrim: Opacity fade protecting header legibility over bright posters.
 * - Scroll transition: Fades from transparent to frosted glass over scroll with 0.5dp bottom hairline.
 */
@Composable
fun TopHeaderBar(
    registry: PluginRegistry = PluginRegistry.Default,
    modifier: Modifier = Modifier,
    isScrolled: Boolean = false,
    scrollFraction: () -> Float = { if (isScrolled) 1f else 0f },
    hazeState: HazeState? = null,
    hasUpdateAvailable: Boolean = false,
    onUpdateBadgeClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val windowDimensions = rememberWindowDimensions()
    val profileManager = remember { com.euthopiar.data.profile.ProfileManager.getInstance(context) }
    val activeProfile by profileManager.activeProfile.collectAsState()
    var showProfileSheet by remember { mutableStateOf(false) }

    val isAmoled = LocalAmoledMode.current
    val barBg = if (isAmoled) Color.Black else Background

    val isUsingHaze = hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val currentFraction = scrollFraction().coerceIn(0f, 1f)
                // 1. Top status bar legibility scrim (75% black down to transparent ~120dp)
                val topScrimAlpha = (1f - currentFraction).coerceIn(0f, 1f) * 0.75f
                if (topScrimAlpha > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = topScrimAlpha),
                            0.5f to Color.Black.copy(alpha = topScrimAlpha * 0.40f),
                            1.0f to Color.Transparent,
                        )
                    )
                }
            }
    ) {
        // 1. Scrolled Frosted Glass Background Layer
        // Fades in/out continuously via graphicsLayer alpha with smootherstep easing
        // Eliminates rigid threshold pops and abrupt last-millisecond snaps
        val currentScrollFraction = scrollFraction().coerceIn(0f, 1f)
        val isHazeActive = isUsingHaze && currentScrollFraction > 0.01f

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = currentScrollFraction
                }
                .then(
                    if (isHazeActive) {
                        Modifier.hazeEffect(state = hazeState!!) {
                            blurRadius = GlassDefaults.BlurRadius
                            tints = listOf(
                                HazeTint(barBg.copy(alpha = 0.88f)),
                                HazeTint(Color.White.copy(alpha = 0.05f))
                            )
                            noiseFactor = GlassDefaults.NoiseFactor
                            fallbackTint = HazeTint(GlassDefaults.NavbarFallbackColor)
                        }
                    } else {
                        Modifier
                            .background(barBg.copy(alpha = 0.88f))
                            .background(Color.White.copy(alpha = 0.05f))
                    }
                )
                .drawBehind {
                    // Solid dark obsidian fill to ensure 100% opacity over scrolled catalog items
                    if (isHazeActive) {
                        drawRect(color = barBg.copy(alpha = 0.88f))
                        drawRect(color = Color.White.copy(alpha = 0.05f))
                    }
                    // Diffuse top-down ambient environmental light wash
                    val topLightBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                    drawRect(brush = topLightBrush)

                    // Hairline bottom border: specular gradient stroke matching FloatingNavbar
                    drawLine(
                        brush = GlassDefaults.BorderBrush,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = HairlineWidth.toPx()
                    )
                }
        )

        // 2. Bar Content Row (Wordmark + Avatar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Wordmark: 15 sp, medium, wide letter spacing (0.2 em), white
                Text(
                    text = "EUTHOPIAR",
                    style = AppTypography.titleMedium.copy(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.em
                    ),
                    color = Color.White
                )

                // Trailing Actions: Update notification badge (if available) + Profile avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasUpdateAvailable && onUpdateBadgeClick != null) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.minTouchTarget)
                                .aspectRatio(1f)
                                .pressable(
                                    scaleDown = 0.96f,
                                    onClickLabel = "Update available",
                                    onClick = onUpdateBadgeClick
                                )
                                .tvFocusable(shape = ShapeFull),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .border(HairlineWidth, Color.White.copy(alpha = 0.22f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SystemUpdateAlt,
                                    contentDescription = "Update available",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                // Subtle pure-white notification dot at top-right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .aspectRatio(1f)
                            .pressable(
                                scaleDown = 0.98f,
                                onClickLabel = "Profile: ${activeProfile.name}",
                                onClick = { showProfileSheet = true }
                            )
                            .tvFocusable(shape = ShapeFull),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(
                                    width = HairlineWidth,
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.euthopiar.ui.profile.ProfileAvatarImage(
                                avatarId = activeProfile.avatarId,
                                size = 34.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .aspectRatio(1f),
                                borderColor = Color.Transparent,
                                borderWidth = 0.dp,
                                contentDescription = "Profile: ${activeProfile.name}"
                            )
                        }
                    }
                }
            }
        }
    }

    var showProfileManagement by remember { mutableStateOf(false) }

    if (showProfileSheet) {
        com.euthopiar.ui.profile.QuickProfileSwitcherDialog(
            onDismissRequest = { showProfileSheet = false },
            onManageProfilesClick = {
                showProfileSheet = false
                showProfileManagement = true
            }
        )
    }

    if (showProfileManagement) {
        com.euthopiar.ui.profile.ProfileManagementSheet(
            onDismiss = { showProfileManagement = false }
        )
    }
}

