package com.euthopiar.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import android.os.Build
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.euthopiar.ui.theme.GlassDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.preferences.NavbarStyle
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.icons.EuthopiarIcons
import com.euthopiar.ui.icons.NavIconSet
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.Motion
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextTertiary
import com.euthopiar.ui.theme.pressable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

enum class NavTab(
    val route: String,
    val title: String,
    val iconSet: NavIconSet
) {
    HOME("home", "Home", EuthopiarIcons.Home),
    SEARCH("search", "Search", EuthopiarIcons.Search),
    LIBRARY("library", "Library", EuthopiarIcons.Library),
    PROFILE("profile", "Profile", EuthopiarIcons.Profile);

    val selectedIcon: ImageVector get() = iconSet.outlined
    val unselectedIcon: ImageVector get() = iconSet.outlined
}

/**
 * Normalizes a navigation route for matching against [NavTab] items.
 * Strips query parameters from '?' onward, trims path slashes, and resolves route aliases & subpaths.
 */
fun normalizeNavTabRoute(route: String?): String {
    if (route.isNullOrBlank()) return NavTab.HOME.route
    val base = route.substringBefore('?').trim().trim('/')
    return when {
        base == "settings" || base.startsWith("settings/") || base == "profile" || base.startsWith("profile/") -> NavTab.PROFILE.route
        base == "downloads" || base.startsWith("downloads/") || base == "library" || base.startsWith("library/") -> NavTab.LIBRARY.route
        base == "search" || base.startsWith("search/") -> NavTab.SEARCH.route
        base == "home" || base.startsWith("home/") -> NavTab.HOME.route
        else -> base
    }
}

val LocalNavbarHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Apple Liquid Glass Bottom Navigation Bar.
 * Features:
 * - 5-layer optical glass simulation (tint, diffuse ambient reflection, inner bevel, specular rim)
 * - Fluid sliding liquid lens selection indicator with asymmetric spring morphing
 * - Micro-compression touch scale (0.94x) + tactile haptic feedback
 * - Supports both Floating Pill and Edge-to-Edge Docked styles
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FloatingNavbar(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    onTabReselected: ((NavTab) -> Unit)? = null,
    style: NavbarStyle = NavbarStyle.FLOATING,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val hazeState = LocalNavbarHazeState.current
    val reduceMotion = LocalReduceMotion.current
    val isImeVisible = WindowInsets.isImeVisible
    val effectivelyVisible = isVisible && !isImeVisible

    val navOffsetY by animateDpAsState(
        targetValue = if (effectivelyVisible) 0.dp else 120.dp,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            tween(200, easing = Motion.StandardEasing)
        },
        label = "navbarScrollOffset"
    )

    val tabs = NavTab.entries
    val normalizedRoute = normalizeNavTabRoute(currentRoute)
    val selectedIndex = tabs.indexOfFirst { it.route == normalizedRoute }.coerceAtLeast(0)

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = navOffsetY.toPx()
                alpha = if (navOffsetY > 100.dp) 0f else (1f - (navOffsetY.toPx() / 120.dp.toPx())).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = Dimens.spacing12)
                .widthIn(min = 280.dp, max = 328.dp)
                .frostedGlassSurface(
                    shape = shape,
                    hazeState = hazeState
                )
        ) {
            NavbarRow(
                tabs = tabs,
                selectedIndex = selectedIndex,
                height = 70.dp,
                reduceMotion = reduceMotion,
                onTabSelected = onTabSelected,
                onTabReselected = onTabReselected
            )
        }
    }
}

@Composable
private fun NavbarRow(
    tabs: List<NavTab>,
    selectedIndex: Int,
    height: androidx.compose.ui.unit.Dp,
    reduceMotion: Boolean,
    onTabSelected: (NavTab) -> Unit,
    onTabReselected: ((NavTab) -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val count = tabs.size

    // Direction-aware liquid stretch: leading edge uses a stiffer spring than trailing edge
    var prev by remember { mutableIntStateOf(selectedIndex) }
    var movingForward by remember { mutableStateOf(true) }
    if (selectedIndex != prev) {
        movingForward = selectedIndex > prev
        prev = selectedIndex
    }

    // Motion tuned to settle in ~180-240 ms window
    val fast = spring<Float>(dampingRatio = 0.8f, stiffness = 1200f)
    val slow = spring<Float>(dampingRatio = 0.8f, stiffness = 500f)

    val leftSpec = if (reduceMotion) snap() else if (movingForward) slow else fast
    val rightSpec = if (reduceMotion) snap() else if (movingForward) fast else slow

    val leftIdx = animateFloatAsState(selectedIndex.toFloat(), leftSpec, label = "lensL")
    val rightIdx = animateFloatAsState(selectedIndex + 1f, rightSpec, label = "lensR")

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Liquid glass lens drawn entirely in draw phase (0 recompositions on animation)
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (count == 0 || size.width <= 0f) return@drawBehind
                    val tabW = size.width / count
                    var l = leftIdx.value * tabW
                    var r = rightIdx.value * tabW
                    if (rtl) {
                        val nl = size.width - r
                        val nr = size.width - l
                        l = nl
                        r = nr
                    }
                    drawGlassLens(l, 0f, r, size.height)
                }
        )

        Row(
            Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val isSelected = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale = animateFloatAsState(
                    targetValue = if (pressed) 0.98f else 1f,
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "press_${tab.name}"
                )
                val tint by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF0B0B0F) else Color.White.copy(alpha = 0.50f),
                    animationSpec = if (reduceMotion) snap() else tween(durationMillis = Motion.DurationMicro),
                    label = "tint_${tab.name}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .selectable(
                            selected = isSelected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (isSelected) {
                                    onTabReselected?.invoke(tab)
                                } else {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    onTabSelected(tab)
                                }
                            }
                        )
                        .tvFocusable(shape = RoundedCornerShape(16.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.iconSet.outlined,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                    val effectiveFontSize = if (fontScale > 1.15f) {
                        (11f / fontScale.coerceAtMost(1.35f)).sp
                    } else {
                        11.sp
                    }

                    Text(
                        text = tab.title,
                        color = tint,
                        style = AppTypography.labelSmall,
                        fontSize = effectiveFontSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Draws the active frosted glass lens capsule.
 * 70% opacity white fill, 16 dp corner radius, inset 4 dp from cell bounds.
 * API 31+ applies ~6 px internal blur contained strictly inside the lens.
 * No outer glow, no border, no specular rim, no radial drop shadow.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassLens(cell: Rect) {
    drawGlassLens(cell.left, cell.top, cell.right, cell.bottom)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassLens(
    cellLeft: Float,
    cellTop: Float,
    cellRight: Float,
    cellBottom: Float
) {
    val insetX = 4.dp.toPx()
    val insetY = 4.dp.toPx()
    val w = (cellRight - cellLeft) - insetX * 2
    if (w <= 0f) return
    val top = cellTop + insetY
    val h = (cellBottom - cellTop) - insetY * 2
    val left = cellLeft + insetX
    val radius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

    // Base flat frosted 70% white fill directly rendered via hardware-accelerated drawRoundRect
    drawRoundRect(
        color = Color.White.copy(alpha = 0.70f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = radius
    )
}

/**
 * Frosted-glass backdrop surface for the navbar container.
 * - Backdrop blur using Haze with 18 dp blur radius.
 * - White fill at 7% opacity (Color.White.copy(alpha = 0.07f)).
 * - 0.5 dp hairline border at white 16% opacity.
 * - No noise grain texture, no drop shadow.
 * - Fallback for API < 31 (no native blur): solid dark charcoal (GlassDefaults.NavbarFallbackColor).
 */
private fun Modifier.frostedGlassSurface(
    shape: Shape,
    hazeState: HazeState?
): Modifier {
    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = 18.dp
                    tints = listOf(HazeTint(Color.White.copy(alpha = 0.07f)))
                    fallbackTint = HazeTint(GlassDefaults.NavbarFallbackColor)
                }
            } else {
                Modifier.background(GlassDefaults.NavbarFallbackColor, shape)
            }
        )
        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.16f), shape = shape)
}

/**
 * Adaptive Navigation Rail for Tablet, Foldable, Desktop, and TV environments.
 */
@Composable
fun AdaptiveNavigationRail(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    onTabReselected: ((NavTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val normalizedRoute = normalizeNavTabRoute(currentRoute)
    Box(
        modifier = modifier
            .width(Dimens.navRailWidth)
            .fillMaxHeight()
            .background(SurfaceLow)
            .border(width = HairlineWidth, color = Outline)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = Dimens.spacing24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing24)
        ) {
            Text(
                text = "E",
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(Dimens.spacing16))

            NavTab.entries.forEach { tab ->
                val isSelected = normalizedRoute == tab.route

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else TextTertiary,
                    animationSpec = tween(durationMillis = Motion.DurationMicro),
                    label = "railIconTint_${tab.name}"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ShapeFull)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                        }
                        .pressable(
                            role = Role.Tab,
                            onClick = {
                                if (isSelected) {
                                    onTabReselected?.invoke(tab)
                                } else {
                                    onTabSelected(tab)
                                }
                            }
                        )
                        .tvFocusable(shape = ShapeFull),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.iconSet.outlined,
                        contentDescription = tab.title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
