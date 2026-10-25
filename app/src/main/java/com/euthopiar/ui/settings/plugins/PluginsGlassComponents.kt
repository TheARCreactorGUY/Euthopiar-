package com.euthopiar.ui.settings.plugins

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Standard Design Tokens for the Plugins & Addons subsystem.
 * Strictly adheres to white-on-dark, frosted glass, and pure monochrome status semantics.
 */
object PluginsGlassTokens {
    val BaseDarkBackground = Color(0xFF0B0B0F)
    val AmoledBackground = Color(0xFF000000)

    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFillPressed = Color.White.copy(alpha = 0.10f)
    val GlassFillDisabled = Color.White.copy(alpha = 0.04f)

    val HairlineBorder = Color.White.copy(alpha = 0.16f)
    val HairlineDivider = Color.White.copy(alpha = 0.14f)

    val SheetGlassFill = Color(0xBA0B0B0F)
    val SheetGlassFallback = Color(0xF50B0B0F)
    val SheetGlassAmoledFill = Color(0xD8000000)

    val DialogGlassFill = Color(0xBA0B0B0F)
    val DialogGlassFallback = Color(0xF50B0B0F)
    val DialogGlassAmoledFill = Color(0xD8000000)

    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.55f)
    val TextTertiary = Color.White.copy(alpha = 0.45f)
    val TextSheetBody = Color.White.copy(alpha = 0.62f)
    val TextDark = Color(0xFF0B0B0F)

    val HairlineWidth = 0.5.dp

    val RadiusTiny = 8.dp
    val RadiusInput = 12.dp
    val RadiusIcon = 13.dp
    val RadiusProviderIcon = 16.dp
    val RadiusSheet = 28.dp
    val RadiusDialog = 28.dp
    val RadiusPill = 999.dp
}

/**
 * Checks if the device has low RAM to eliminate background blurs cleanly.
 */
fun Context.isLowEndDevice(): Boolean {
    return try {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true
    } catch (_: Exception) {
        false
    }
}

/**
 * Large-surface glass modifier (compact top bar, bottom sheets, dialogs).
 * Supports Haze background blur where supported, with AMOLED and low-end fallbacks.
 */
fun Modifier.pluginsGlassSurface(
    shape: Shape = RoundedCornerShape(PluginsGlassTokens.RadiusSheet),
    border: Boolean = true,
    hazeState: HazeState? = null,
    isSheetOrDialog: Boolean = false
): Modifier = composed {
    val isAmoled = LocalAmoledMode.current
    val context = LocalContext.current
    val isLowEnd = remember(context) { context.isLowEndDevice() }

    val baseColor = when {
        isLowEnd -> if (isSheetOrDialog) PluginsGlassTokens.SheetGlassFallback else Color(0xF0101118)
        isAmoled -> if (isSheetOrDialog) PluginsGlassTokens.SheetGlassAmoledFill else PluginsGlassTokens.AmoledBackground
        isSheetOrDialog -> PluginsGlassTokens.SheetGlassFill
        else -> PluginsGlassTokens.GlassFill
    }

    val blurModifier = if (!isAmoled && !isLowEnd && hazeState != null) {
        Modifier.hazeEffect(state = hazeState) {
            blurRadius = if (isSheetOrDialog) 22.dp else 18.dp
        }
    } else {
        Modifier
    }

    this
        .clip(shape)
        .then(blurModifier)
        .background(baseColor, shape)
        .then(
            if (border) Modifier.border(PluginsGlassTokens.HairlineWidth, PluginsGlassTokens.HairlineBorder, shape)
            else Modifier
        )
}

/**
 * Standard glass row modifier without blur (for list rows, row tiles, chips, toggles).
 * Strictly adheres to 7% white fill and 0.5dp hairline border at 16% white.
 */
fun Modifier.pluginsRowGlass(
    shape: Shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon),
    border: Boolean = true
): Modifier = this
    .clip(shape)
    .background(PluginsGlassTokens.GlassFill, shape)
    .then(
        if (border) Modifier.border(PluginsGlassTokens.HairlineWidth, PluginsGlassTokens.HairlineBorder, shape)
        else Modifier
    )

/**
 * Subtle 98% scale press feedback.
 * Uses collectIsPressedAsState for zero-latency scroll/swipe gesture compatibility.
 * Respects Reduce Motion by falling back to quick opacity fades (120ms in, 100ms out).
 */
fun Modifier.pluginsPressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.98f,
    pressedAlpha: Float = 0.88f,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) scaleDown else 1f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "pluginsPressScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = if (reduceMotion) 120 else 80, easing = FastOutSlowInEasing),
        label = "pluginsPressAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * 0.5dp Hairline Divider.
 */
@Composable
fun PluginsHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = PluginsGlassTokens.HairlineDivider
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = PluginsGlassTokens.HairlineWidth,
        color = color
    )
}

/**
 * 44x26dp Custom Glass Toggle Switch with 48dp minimum touch target.
 * - When ON: solid white track with near-black 20dp knob.
 * - When OFF: glass track (7% fill, 0.5dp hairline border) with 60% white knob.
 * - Smooth 140ms ease-out synchronized knob travel and track crossfade.
 */
@Composable
fun PluginsGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current

    val trackBg by animateColorAsState(
        targetValue = if (checked) Color.White else PluginsGlassTokens.GlassFill,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 140, easing = FastOutSlowInEasing),
        label = "pluginsToggleTrackBg"
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) PluginsGlassTokens.TextDark else Color.White.copy(alpha = 0.60f),
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 140, easing = FastOutSlowInEasing),
        label = "pluginsToggleKnobColor"
    )

    // Travel from 3.dp (left) to 21.dp (right) within 44.dp track
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 140, easing = FastOutSlowInEasing),
        label = "toggleKnobOffset"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .pluginsPressable(enabled = enabled, scaleDown = 0.98f) {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                .background(trackBg)
                .then(
                    if (!checked) Modifier.border(
                        PluginsGlassTokens.HairlineWidth,
                        PluginsGlassTokens.HairlineBorder,
                        RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                    ) else Modifier
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
 * Primary Solid White Pill Button.
 * Used for primary affirmative actions ("Install repository", "Install all", etc.).
 */
@Composable
fun PluginsWhitePillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
            .background(if (enabled) Color.White else Color.White.copy(alpha = 0.40f))
            .pluginsPressable(enabled = enabled && !isLoading, scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = PluginsGlassTokens.TextDark
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PluginsGlassTokens.TextDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                color = PluginsGlassTokens.TextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Secondary Glass Pill Button.
 * Used for secondary or destructive actions (white text on glass fill).
 */
@Composable
fun PluginsGlassPillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
            .background(PluginsGlassTokens.GlassFill)
            .border(
                PluginsGlassTokens.HairlineWidth,
                PluginsGlassTokens.HairlineBorder,
                RoundedCornerShape(PluginsGlassTokens.RadiusPill)
            )
            .pluginsPressable(enabled = enabled && !isLoading, scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 40dp Circle Button (Back, Refresh, Trailing actions).
 */
@Composable
fun PluginsGlassCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isWhite: Boolean = false,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val bg = if (isWhite) Color.White else PluginsGlassTokens.GlassFill
    val iconTint = if (isWhite) PluginsGlassTokens.TextDark else Color.White

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (!isWhite) Modifier.border(
                    PluginsGlassTokens.HairlineWidth,
                    PluginsGlassTokens.HairlineBorder,
                    CircleShape
                ) else Modifier
            )
            .pluginsPressable(enabled = enabled && !isLoading, scaleDown = 0.98f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = iconTint
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 48dp Glass Input Field with 12dp radius, Paste glass pill, and Clear button.
 */
@Composable
fun PluginsGlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onPaste: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusInput))
            .background(PluginsGlassTokens.GlassFill)
            .border(
                PluginsGlassTokens.HairlineWidth,
                PluginsGlassTokens.HairlineBorder,
                RoundedCornerShape(PluginsGlassTokens.RadiusInput)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = PluginsGlassTokens.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (value.isNotEmpty() && onClear != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .pluginsPressable(scaleDown = 0.98f, onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = PluginsGlassTokens.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (value.isEmpty() && onPaste != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                        .background(PluginsGlassTokens.GlassFill)
                        .border(
                            PluginsGlassTokens.HairlineWidth,
                            PluginsGlassTokens.HairlineBorder,
                            RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                        )
                        .pluginsPressable(scaleDown = 0.98f, onClick = onPaste)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Paste",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

enum class InstallButtonState {
    EMPTY,
    READY,
    BUSY
}

/**
 * "Install repository" or "Install addon" button.
 * - EMPTY: 40% glass fill with 4dp horizontal nudge animation on tap.
 * - READY: solid white pill.
 * - BUSY: spinner + "Installing...".
 */
@Composable
fun PluginsInstallButton(
    state: InstallButtonState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val nudgeOffset = remember { Animatable(0f) }

    val isReady = state == InstallButtonState.READY
    val isBusy = state == InstallButtonState.BUSY

    val bg = when (state) {
        InstallButtonState.READY -> Color.White
        InstallButtonState.BUSY -> Color.White
        InstallButtonState.EMPTY -> PluginsGlassTokens.GlassFillDisabled
    }

    val textColor = when (state) {
        InstallButtonState.READY, InstallButtonState.BUSY -> PluginsGlassTokens.TextDark
        InstallButtonState.EMPTY -> PluginsGlassTokens.TextTertiary
    }

    Box(
        modifier = modifier
            .offset(x = nudgeOffset.value.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
            .background(bg)
            .then(
                if (!isReady && !isBusy) Modifier.border(
                    PluginsGlassTokens.HairlineWidth,
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                ) else Modifier
            )
            .pluginsPressable(
                enabled = true,
                scaleDown = 0.98f,
                onClick = {
                    if (state == InstallButtonState.EMPTY) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                        scope.launch {
                            nudgeOffset.animateTo(4f, tween(50, easing = LinearEasing))
                            nudgeOffset.animateTo(-4f, tween(70, easing = LinearEasing))
                            nudgeOffset.animateTo(3f, tween(50, easing = LinearEasing))
                            nudgeOffset.animateTo(0f, tween(40, easing = LinearEasing))
                        }
                    } else if (isReady) {
                        onClick()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = PluginsGlassTokens.TextDark
                )
                Text(
                    text = "Installing...",
                    color = PluginsGlassTokens.TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Unified Glass Confirmation Dialog (centered at max 360dp, 28dp corners, 60% scrim).
 * White Cancel pill on the left, glass action pill on the right (never red).
 */
@Composable
fun PluginsSharedGlassDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemPreview: (@Composable () -> Unit)? = null
) {
    com.euthopiar.ui.components.EuthopiarGlassDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        itemPreview = itemPreview,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

sealed class StatusDialogState {
    object Installing : StatusDialogState()
    data class Success(val message: String) : StatusDialogState()
    data class Error(val message: String, val onRetry: (() -> Unit)? = null) : StatusDialogState()
}

/**
 * In-place Installation Status Dialog.
 * Smoothly updates state: Installing spinner -> Success "Done" -> Error "Retry" + "Close".
 */
@Composable
fun PluginsInstallStatusDialog(
    state: StatusDialogState,
    onDismiss: () -> Unit
) {
    val isBusy = state is StatusDialogState.Installing

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = !isBusy)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .pluginsGlassSurface(
                    shape = RoundedCornerShape(PluginsGlassTokens.RadiusDialog),
                    isSheetOrDialog = true
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    StatusDialogState.Installing -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                                .background(PluginsGlassTokens.GlassFill)
                                .border(
                                    PluginsGlassTokens.HairlineWidth,
                                    PluginsGlassTokens.HairlineBorder,
                                    RoundedCornerShape(PluginsGlassTokens.RadiusIcon)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Installing...",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Fetching manifest and resolving sources.",
                            style = TextStyle(
                                color = PluginsGlassTokens.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(PluginsGlassTokens.GlassFill)
                                .border(
                                    PluginsGlassTokens.HairlineWidth,
                                    PluginsGlassTokens.HairlineBorder,
                                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                )
                                .pluginsPressable(scaleDown = 0.98f, onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is StatusDialogState.Success -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                                .background(PluginsGlassTokens.GlassFill)
                                .border(
                                    PluginsGlassTokens.HairlineWidth,
                                    PluginsGlassTokens.HairlineBorder,
                                    RoundedCornerShape(PluginsGlassTokens.RadiusIcon)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Text(
                            text = "Installation Complete",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = state.message,
                            style = TextStyle(
                                color = PluginsGlassTokens.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(Color.White)
                                .pluginsPressable(scaleDown = 0.98f, onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Done",
                                color = PluginsGlassTokens.TextDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is StatusDialogState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                                .background(PluginsGlassTokens.GlassFill)
                                .border(
                                    PluginsGlassTokens.HairlineWidth,
                                    PluginsGlassTokens.HairlineBorder,
                                    RoundedCornerShape(PluginsGlassTokens.RadiusIcon)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Text(
                            text = "Installation Failed",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = state.message,
                            style = TextStyle(
                                color = PluginsGlassTokens.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.onRetry != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                        .background(Color.White)
                                        .pluginsPressable(scaleDown = 0.98f, onClick = state.onRetry),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Retry",
                                        color = PluginsGlassTokens.TextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                    .background(PluginsGlassTokens.GlassFill)
                                    .border(
                                        PluginsGlassTokens.HairlineWidth,
                                        PluginsGlassTokens.HairlineBorder,
                                        RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                    )
                                    .pluginsPressable(scaleDown = 0.98f, onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
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
 * 1.6s Shimmer skeleton animation modifier.
 */
fun Modifier.pluginsShimmer(): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) {
        return@composed this.background(Color.White.copy(alpha = 0.06f))
    }

    val transition = rememberInfiniteTransition(label = "pluginsShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pluginsShimmerAnim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 300f, 300f)
    )

    this.background(brush)
}

/**
 * Glass Toast Notification Pill.
 * Bottom-anchored frosted glass pill with slide-up entrance, outline check icon,
 * and 2.5s auto-dismiss. Replaces Android OS native Toasts across the subsystem.
 */
@Composable
fun PluginsGlassToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.CheckCircle
) {
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            kotlinx.coroutines.delay(2500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = if (reduceMotion) fadeIn(tween(120)) else slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(180)),
        exit = if (reduceMotion) fadeOut(tween(100)) else slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                .background(Color(0xD80B0B0F))
                .border(
                    PluginsGlassTokens.HairlineWidth,
                    PluginsGlassTokens.HairlineBorder,
                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                )
                .pluginsPressable(scaleDown = 0.98f, onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

