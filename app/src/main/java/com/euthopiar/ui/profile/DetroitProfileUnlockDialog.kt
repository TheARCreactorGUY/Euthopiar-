package com.euthopiar.ui.profile

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.data.profile.hashPin
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Profile Unlock PIN Dialog matching Euthopiar frosted glass design language.
 */
@Composable
fun DetroitProfileUnlockDialog(
    profile: UserProfile,
    onDismissRequest: () -> Unit,
    onUnlocked: () -> Unit,
    onSwitchProfileClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current

    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val dialogAnim = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            dialogAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 550f)
            )
        } else {
            dialogAnim.snapTo(1f)
        }
    }

    fun animateDismiss(action: () -> Unit) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            if (!reduceMotion) {
                dialogAnim.animateTo(0f, tween(180))
            }
            action()
        }
    }

    fun onDigitPress(digit: String) {
        if (isError) {
            isError = false
            errorMessage = null
        }
        if (enteredPin.length < 4) {
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == 4) {
                val isValid = if (!profile.pinHash.isNullOrBlank()) {
                    hashPin(updated) == profile.pinHash
                } else if (!profile.pin.isNullOrBlank()) {
                    updated == profile.pin
                } else {
                    false
                }
                if (isValid) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    animateDismiss { onUnlocked() }
                } else {
                    isError = true
                    errorMessage = "Wrong PIN. Try again."
                    enteredPin = ""
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun onBackspacePress() {
        if (isError) {
            isError = false
            errorMessage = null
        }
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)
    val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackColor = EuthopiarGlassTokens.fallbackFill(isAmoled)
    val animVal = dialogAnim.value
    val scrimAlpha = if (reduceMotion) EuthopiarGlassTokens.ScrimAlpha else (EuthopiarGlassTokens.ScrimAlpha * animVal).coerceIn(0f, EuthopiarGlassTokens.ScrimAlpha)
    val cardScale = if (reduceMotion) 1f else 0.94f + 0.06f * animVal
    val cardTranslationY = if (reduceMotion) 0f else (1f - animVal) * 30f

    Dialog(
        onDismissRequest = { animateDismiss(onDismissRequest) },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateDismiss(onDismissRequest) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .graphicsLayer {
                        alpha = animVal
                        scaleX = cardScale
                        scaleY = cardScale
                        translationY = cardTranslationY
                    }
                    .clip(cardShape)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurRadius = EuthopiarGlassTokens.BlurRadius
                                tints = listOf(HazeTint(cardFill))
                                fallbackTint = HazeTint(fallbackColor)
                            }
                        } else {
                            Modifier.background(fallbackColor)
                        }
                    )
                    .border(
                        width = EuthopiarGlassTokens.HairlineWidth,
                        color = EuthopiarGlassTokens.HairlineBorder,
                        shape = cardShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // block click through
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Profile Avatar with thin white hairline ring
                    AvatarWithThinRing(
                        avatarId = profile.avatarId,
                        avatarSize = 64.dp,
                        ringGap = 4.dp,
                        ringBorderWidth = 0.5.dp,
                        ringColor = Color.White.copy(alpha = 0.20f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = profile.name,
                        style = EuthopiarGlassTokens.HeaderTitle,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter 4-digit PIN to access",
                        style = EuthopiarGlassTokens.HeaderSubtitle,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PinDots(
                        pinLength = enteredPin.length,
                        isError = isError
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PinKeypad(
                        onDigitClick = ::onDigitPress,
                        onBackspaceClick = ::onBackspacePress
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onSwitchProfileClick != null) {
                            ProfileGlassPill(
                                text = "Switch Profile",
                                onClick = onSwitchProfileClick,
                                visualHeight = 40.dp,
                                fontSize = 13
                            )
                        }
                        ProfileGlassPill(
                            text = "Cancel",
                            onClick = onDismissRequest,
                            visualHeight = 40.dp,
                            fontSize = 13
                        )
                    }
                }
            }
        }
    }
}
