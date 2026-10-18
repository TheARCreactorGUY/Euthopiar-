package com.euthopiar.ui.profile

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Shared design tokens for the Settings & Profile redesign.
 * Bold, minimal, modern, cinematic design language.
 */
object ProfileGlassTokens {
    val BaseDarkBackground = Color(0xFF0B0B0F)
    val AmoledBackground = Color(0xFF000000)

    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFillPressed = Color.White.copy(alpha = 0.10f)
    val GlassFallbackFill = Color(0xFF14151F).copy(alpha = 0.94f)
    val GlassFallbackFillOpaque = Color.White.copy(alpha = 0.14f)

    val HairlineBorder = Color.White.copy(alpha = 0.16f)
    val HairlineBorderActive = Color.White.copy(alpha = 0.30f)
    val HairlineDivider = Color.White.copy(alpha = 0.14f)

    val SheetGlassFill = Color(0xBA0B0B0F)
    val SheetGlassFallback = Color(0xF0101118)
    val SheetHairlineBorder = Color.White.copy(alpha = 0.18f)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted50 = Color.White.copy(alpha = 0.50f)
    val TextLabel45 = Color.White.copy(alpha = 0.45f)
    val TextDarkButton = Color(0xFF0B0B0F)

    // Soft Indigo haze for normal dark mode (disabled in AMOLED)
    val HazeIndigoBlob = Color(0xFF262D4A)

    val HairlineWidthDp = 0.5.dp
    val DragHandleWidth = 36.dp
    val DragHandleHeight = 4.dp
    val MinTouchTarget = 48.dp

    val CachedDashedPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
}

/**
 * Frosted-glass backdrop surface.
 * Fallback when Haze is unavailable, API < 31, or in AMOLED mode:
 * opaque translucent fill without blur.
 */
fun Modifier.profileGlassSurface(
    shape: Shape,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
    isActive: Boolean = false,
    customFill: Color = ProfileGlassTokens.GlassFill,
    fallbackFill: Color = ProfileGlassTokens.GlassFallbackFill
): Modifier {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null && !isAmoled
    val borderColor = if (isActive) ProfileGlassTokens.HairlineBorderActive else ProfileGlassTokens.HairlineBorder

    return this
        .clip(shape)
        .then(
            if (canBlur) {
                Modifier.hazeEffect(state = hazeState!!) {
                    blurRadius = 18.dp
                    tints = listOf(HazeTint(customFill))
                    fallbackTint = HazeTint(fallbackFill)
                }
            } else {
                Modifier.background(fallbackFill, shape)
            }
        )
        .border(width = ProfileGlassTokens.HairlineWidthDp, color = borderColor, shape = shape)
}

/**
 * Hairline divider 0.5 px white at 14% opacity.
 */
@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = ProfileGlassTokens.HairlineWidthDp,
        color = ProfileGlassTokens.HairlineDivider,
        modifier = modifier
    )
}

/**
 * Neutral glass chip (e.g. "Master", "Standard", "Kids").
 * 22 dp visual height, 8 dp radius, 11 sp medium white text.
 */
@Composable
fun ProfileGlassChip(
    text: String,
    modifier: Modifier = Modifier,
    showLockIcon: Boolean = false
) {
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ProfileGlassTokens.GlassFill)
            .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showLockIcon) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "PIN Protected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Glass pill button (e.g. "Switch", "Edit profile", "Done").
 * Height 32-44 dp, touch target at least 44-48 dp, 13-14 sp medium text.
 * Press feedback: scale to 98 percent on press, slight lift of glass fill to ~10%.
 */
@Composable
fun ProfileGlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    visualHeight: Dp = 36.dp,
    horizontalPadding: Dp = 16.dp,
    enabled: Boolean = true,
    fontSize: Int = 13
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pillScale"
    )

    val currentFill = if (isPressed) ProfileGlassTokens.GlassFillPressed else ProfileGlassTokens.GlassFill

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = ProfileGlassTokens.MinTouchTarget, minHeight = ProfileGlassTokens.MinTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(visualHeight)
                .clip(CircleShape)
                .background(currentFill)
                .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, CircleShape)
                .padding(horizontal = horizontalPadding),
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
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize + 4).sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Full width solid white pill button for primary actions ("Save changes", "Create profile").
 * Height 52 dp, 15 sp medium near-black text (#0B0B0F).
 * Press scale 98%, progress indicator on loading.
 */
@Composable
fun ProfileWhitePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "whitePillScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = ProfileGlassTokens.TextDarkButton,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = ProfileGlassTokens.TextDarkButton
                )
            )
        }
    }
}

/**
 * Glass toggle switch (Screen 3: 46 by 28 dp).
 * OFF state: glass track (7% white fill with 0.5 px hairline) and 20 dp knob in white 45%.
 * ON state: solid white track with near-black (#0B0B0F) knob.
 */
@Composable
fun ProfileGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val reduceMotion = LocalReduceMotion.current

    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 4.dp,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "glassToggleKnob"
    )

    val trackFillAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0.07f,
        animationSpec = if (reduceMotion) tween(0) else tween(180),
        label = "trackFillAlpha"
    )

    val knobColor = if (checked) ProfileGlassTokens.TextDarkButton else Color.White.copy(alpha = 0.45f)
    val trackBgColor = if (checked) Color.White else Color.White.copy(alpha = trackFillAlpha)

    Box(
        modifier = modifier
            .size(width = 54.dp, height = ProfileGlassTokens.MinTouchTarget) // at least 48 dp touch target
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                enabled = enabled,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(CircleShape)
                .background(trackBgColor)
                .then(
                    if (!checked) {
                        Modifier.border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(knobColor)
            )
        }
    }
}

/**
 * Hairline-divided settings row with glass icon tile on the left.
 * - Icon tile: 38 dp square, 12 dp radius, glass recipe, containing 19 dp outline icon in white.
 * - Title: 15 sp medium, white.
 * - Subtitle: 12 sp regular, white 50%, wraps up to 2 lines if font is large.
 * - Right: thin outline chevron (18 dp, white 40%).
 * - Row min height: 64 dp, vertical padding 14 dp.
 * - Press state: faint glass lift (white 5 to 7%) on row background, 98% scale on icon tile.
 */
@Composable
fun SettingsHairlineRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val iconTileScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "iconTileScale"
    )

    val rowBgColor = if (isPressed && enabled) Color.White.copy(alpha = 0.06f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .background(rowBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .tvFocusable(shape = RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 38 dp square glass icon tile
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = iconTileScale
                    scaleY = iconTileScale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(ProfileGlassTokens.GlassFill)
                .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center: Title + Subtitle Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = ProfileGlassTokens.TextMuted50
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right: Thin outline chevron
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.40f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Circular add / upload button with dashed hairline border.
 * Matches the Library dashed "New playlist" tile.
 */
@Composable
fun DashedCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 22.dp,
    contentDescription: String = "Add",
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "dashedScale"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = ProfileGlassTokens.MinTouchTarget, minHeight = ProfileGlassTokens.MinTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick,
                onClickLabel = contentDescription
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(ProfileGlassTokens.GlassFill)
                .drawBehind {
                    val strokeWidth = ProfileGlassTokens.HairlineWidthDp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = ProfileGlassTokens.CachedDashedPathEffect
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Avatar with thin outer ring and optional check indicator.
 */
@Composable
fun AvatarWithThinRing(
    avatarId: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    ringGap: Dp = 4.dp,
    ringBorderWidth: Dp = 0.5.dp,
    ringColor: Color = Color.White.copy(alpha = 0.30f),
    isSelected: Boolean = false,
    showSelectedCheck: Boolean = false,
    isAvatarOption: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(120),
        label = "avatarScale"
    )

    val outerSize = avatarSize + (ringGap * 2)

    val targetRingWidth = if (isSelected) 2.dp else ringBorderWidth
    val targetRingColor = if (isSelected) Color.White else ringColor
    val animatedRingWidth by animateDpAsState(
        targetValue = targetRingWidth,
        animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing),
        label = "ringWidth"
    )
    val animatedRingColor by animateColorAsState(
        targetValue = targetRingColor,
        animationSpec = if (reduceMotion) tween(0) else tween(180, easing = FastOutSlowInEasing),
        label = "ringColor"
    )

    Box(
        modifier = modifier
            .size(outerSize)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isAvatarOption) {
                    Modifier.semantics {
                        this.role = Role.RadioButton
                        this.selected = isSelected
                        this.contentDescription = if (isSelected) "Avatar option, selected" else "Avatar option, not selected"
                    }
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = if (isAvatarOption) Role.RadioButton else Role.Button,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer thin ring
        Box(
            modifier = Modifier
                .size(outerSize)
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(
                    width = animatedRingWidth,
                    color = animatedRingColor,
                    shape = CircleShape
                )
        )

        // Avatar Image
        ProfileAvatarImage(
            avatarId = avatarId,
            size = avatarSize,
            modifier = Modifier
                .size(avatarSize)
                .aspectRatio(1f),
            borderColor = Color.Transparent,
            borderWidth = 0.dp
        )

        // Selected check circle badge at bottom right corner
        AnimatedVisibility(
            visible = showSelectedCheck && isSelected,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = if (reduceMotion) fadeIn(tween(0)) else (scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = 0.6f) + fadeIn(tween(180))),
            exit = if (reduceMotion) fadeOut(tween(0)) else (scaleOut(tween(140, easing = FastOutSlowInEasing), targetScale = 0.6f) + fadeOut(tween(140)))
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, ProfileGlassTokens.TextDarkButton, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = ProfileGlassTokens.TextDarkButton,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Reusable glass confirmation dialog / bottom sheet.
 * Headline 20-22 sp medium, muted message, white pill primary action, white cancel action. No red!
 */
@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelText: String = "Cancel",
    isDestructive: Boolean = false,
    onCancelAction: (() -> Unit)? = null
) {
    val isAmoled = LocalAmoledMode.current
    val dialogBg = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val dialogAnim = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            dialogAnim.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        } else {
            dialogAnim.snapTo(1f)
        }
    }

    fun animateDismiss(action: () -> Unit) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            if (!reduceMotion) {
                dialogAnim.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
            }
            action()
        }
    }

    Dialog(
        onDismissRequest = { animateDismiss(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f * dialogAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateDismiss(onDismiss) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        alpha = dialogAnim.value
                        val scale = 0.94f + (0.06f * dialogAnim.value)
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(dialogBg)
                    .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // block clicking through
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = ProfileGlassTokens.TextMuted50
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action: White Pill Button
                    ProfileWhitePillButton(
                        text = confirmText,
                        onClick = { animateDismiss(onConfirm) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cancel Text Action
                    ProfileGlassPill(
                        text = cancelText,
                        onClick = { animateDismiss(onCancelAction ?: onDismiss) },
                        visualHeight = 40.dp,
                        fontSize = 14
                    )
                }
            }
        }
    }
}
