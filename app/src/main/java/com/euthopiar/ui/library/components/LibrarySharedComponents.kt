package com.euthopiar.ui.library.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Standard glass surface modifier for Library matching Global Design Language.
 * - Fill: white at about 7%
 * - Blur: 18dp blur behind surface
 * - Border: 0.5dp hairline, white at 16%
 * - Low-end fallback: solid opaque fill Color(0xFF14151F).copy(alpha = 0.94f)
 */
fun Modifier.libraryGlassSurface(
    shape: Shape = RoundedCornerShape(LibraryDimens.controlRadius),
    hazeState: HazeState? = null,
    isActive: Boolean = false,
    alphaFill: Float = 0.07f
): Modifier {
    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = 18.dp
                    tints = listOf(HazeTint(Color.White.copy(alpha = alphaFill)))
                    fallbackTint = HazeTint(LibraryColors.GlassFallback)
                }
            } else {
                Modifier.background(LibraryColors.GlassFallback, shape)
            }
        )
        .border(
            width = LibraryDimens.hairline,
            color = if (isActive) LibraryColors.GlassBorderActive else LibraryColors.GlassBorder,
            shape = shape
        )
}

/**
 * 0.5px hairline divider in white 14% opacity.
 */
@Composable
fun LibraryHairlineDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = LibraryDimens.hairline,
        color = LibraryColors.Hairline,
        modifier = modifier
    )
}

/**
 * Thin 2px progress bar:
 * - 2px tall, fully rounded.
 * - Track: white 14%.
 * - Fill: white 85%.
 * - Smooth transition when value changes.
 */
@Composable
fun LibraryProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) tween(0) else tween(300, easing = FastOutSlowInEasing),
        label = "libraryProgressBar"
    )

    Box(
        modifier = modifier
            .height(LibraryDimens.progressBarHeight)
            .clip(CircleShape)
            .background(LibraryColors.ProgressTrack)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(LibraryDimens.progressBarHeight)
                    .clip(CircleShape)
                    .background(LibraryColors.ProgressFill)
            )
        }
    }
}

/**
 * Ambient Haze Background with 1 to 2 soft color haze blobs drifting extremely slowly.
 * Varied subtle hues per tab:
 * 0: Teal & Indigo (Downloads)
 * 1: Indigo & Rose (Saved)
 * 2: Violet & Indigo (Playlists)
 */
@Composable
fun LibraryAmbientHazeBackground(
    tabIndex: Int,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "libraryAmbientHaze")

    val drift1 = infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )

    val drift2 = infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    val isAmoled = LocalAmoledMode.current
    val baseBg = if (isAmoled) Color.Black else LibraryColors.Background

    val (topBlobColor, bottomBlobColor) = when (tabIndex) {
        0 -> LibraryColors.HazeTeal to LibraryColors.HazeIndigo
        2 -> LibraryColors.HazeViolet to LibraryColors.HazeIndigo
        else -> LibraryColors.HazeIndigo to LibraryColors.HazeRose
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .hazeSource(state = hazeState)
            .drawBehind {
                if (!isAmoled) {
                    val effectiveDrift1 = if (reduceMotion) 0f else drift1.value
                    val effectiveDrift2 = if (reduceMotion) 0f else drift2.value
                    val maxDim = maxOf(size.width, size.height)

                    // Top corner haze atmospheric wash (no circular boundaries)
                    drawRect(
                        brush = Brush.radialGradient(
                            0.00f to topBlobColor.copy(alpha = 0.22f),
                            0.25f to topBlobColor.copy(alpha = 0.13f),
                            0.50f to topBlobColor.copy(alpha = 0.05f),
                            0.75f to topBlobColor.copy(alpha = 0.01f),
                            1.00f to Color.Transparent,
                            center = Offset(size.width * 0.95f + effectiveDrift1, size.height * 0.10f + effectiveDrift2),
                            radius = maxDim * 1.15f
                        )
                    )

                    // Lower side haze atmospheric wash (no circular boundaries)
                    drawRect(
                        brush = Brush.radialGradient(
                            0.00f to bottomBlobColor.copy(alpha = 0.18f),
                            0.25f to bottomBlobColor.copy(alpha = 0.10f),
                            0.50f to bottomBlobColor.copy(alpha = 0.04f),
                            0.75f to bottomBlobColor.copy(alpha = 0.01f),
                            1.00f to Color.Transparent,
                            center = Offset(size.width * 0.05f + effectiveDrift2, size.height * 0.85f + effectiveDrift1),
                            radius = maxDim * 1.05f
                        )
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Glass toggle switch for "Offline only":
 * - 34 x 20 px, fully rounded.
 * - Off: knob on left in white 50%, track white 7%.
 * - On: knob on right in solid white, track fill white 18%, brighter border.
 */
@Composable
fun LibraryGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val reduceMotion = LocalReduceMotion.current
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 16.dp else 2.dp,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "toggleKnob"
    )

    val trackAlpha by animateFloatAsState(
        targetValue = if (checked) 0.18f else 0.07f,
        animationSpec = if (reduceMotion) tween(0) else tween(180),
        label = "trackAlpha"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (checked) 0.32f else 0.16f,
        animationSpec = if (reduceMotion) tween(0) else tween(180),
        label = "borderAlpha"
    )

    val knobColor = if (checked) Color.White else Color.White.copy(alpha = 0.50f)

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 48.dp) // 48dp touch target
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                enabled = enabled,
                onClickLabel = "Offline only, switch, ${if (checked) "on" else "off"}"
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = LibraryDimens.toggleWidth, height = LibraryDimens.toggleHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = trackAlpha))
                .border(LibraryDimens.hairline, Color.White.copy(alpha = borderAlpha), CircleShape)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(LibraryDimens.toggleKnobSize)
                    .clip(CircleShape)
                    .background(knobColor)
            )
        }
    }
}

/**
 * Rounded glass dropdown (32 px tall visually, 48 px tap target, 12 px radius)
 * with anchored popover separated by hairlines and check mark icon.
 */
@Composable
fun LibraryGlassDropdown(
    selectedLabel: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing),
        label = "chevronAngle"
    )

    Box(
        modifier = modifier
            .height(48.dp), // 48dp minimum accessible touch target
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(LibraryDimens.dropdownHeight)
                .libraryGlassSurface(
                    shape = RoundedCornerShape(LibraryDimens.dropdownRadius),
                    hazeState = hazeState,
                    isActive = isExpanded
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.DropdownList,
                    onClickLabel = "Sort by, currently $selectedLabel"
                ) {
                    onToggleExpand()
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedLabel,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(chevronAngle)
                )
            }
        }

        var isPopupRendered by remember { mutableStateOf(false) }
        val popupAnim = remember { Animatable(0f) }

        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                isPopupRendered = true
                if (reduceMotion) {
                    popupAnim.snapTo(1f)
                } else {
                    popupAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                }
            } else if (isPopupRendered) {
                if (reduceMotion) {
                    popupAnim.snapTo(0f)
                    isPopupRendered = false
                } else {
                    popupAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                    isPopupRendered = false
                }
            }
        }

        if (isPopupRendered) {
            val density = LocalDensity.current
            val popoverScale = 0.95f + (0.05f * popupAnim.value)
            val popoverAlpha = popupAnim.value
            val popoverOffsetY = (1f - popupAnim.value) * (-6f) * density.density

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 44.dp.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true)
            ) {

                val isAmoled = LocalAmoledMode.current
                val popoverShape = RoundedCornerShape(14.dp)
                val glassFill = if (isAmoled) Color.Black.copy(alpha = 0.85f) else Color(0xFF0F101B).copy(alpha = 0.70f)
                val borderBrush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )

                Box(
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 220.dp)
                        .graphicsLayer {
                            scaleX = popoverScale
                            scaleY = popoverScale
                            alpha = popoverAlpha
                            translationY = popoverOffsetY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        }
                        .clip(popoverShape)
                        .background(glassFill)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = LibraryDimens.hairline,
                            brush = borderBrush,
                            shape = popoverShape
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        options.forEachIndexed { index, option ->
                            val isChosen = option.equals(selectedLabel, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable {
                                        onSelect(option)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = if (isChosen) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isChosen) Color.White else LibraryColors.TextMuted55
                                    )
                                )

                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (index < options.lastIndex) {
                                LibraryHairlineDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small glass pill (about 12px radius, 7px by 14px padding) used for "Select" / "Done" action.
 */
@Composable
fun LibraryGlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    hazeState: HazeState? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "pillScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isPrimary) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                } else {
                    Modifier.libraryGlassSurface(
                        shape = RoundedCornerShape(12.dp),
                        hazeState = hazeState
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color(0xFF0B0B0F) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = if (isPrimary) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 12.5.sp,
                    color = if (isPrimary) Color(0xFF0B0B0F) else Color.White
                )
            )
        }
    }
}

/**
 * Glass rounded square icon tile (36x36 or 40x40 or 48x48) with outline icon inside.
 */
@Composable
fun LibraryGlassIconTile(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    radius: Dp = 12.dp,
    iconSize: Dp = 18.dp,
    hazeState: HazeState? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .libraryGlassSurface(
                shape = RoundedCornerShape(radius),
                hazeState = hazeState
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Glass square back button (40 by 40 px, 14 px radius, arrow-left icon).
 */
@Composable
fun LibraryGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "backBtnScale"
    )

    Box(
        modifier = modifier
            .size(48.dp) // 48dp minimum accessible touch target
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Go back",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .libraryGlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    hazeState = hazeState
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Glass square overflow button (40 by 40 px, 14 px radius, more-vert icon).
 */
@Composable
fun LibraryGlassOverflowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "overflowBtnScale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "More options",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .libraryGlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    hazeState = hazeState
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
