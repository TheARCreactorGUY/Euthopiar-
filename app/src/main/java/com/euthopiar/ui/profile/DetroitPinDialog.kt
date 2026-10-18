package com.euthopiar.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch

private enum class PinStep {
    ENTER_NEW,
    CONFIRM_NEW,
}

/**
 * 2-Step PIN Setup Dialog ported from Detroit with Euthopiar minimal glass design.
 */
@Composable
fun DetroitPinDialog(
    onDismissRequest: () -> Unit,
    onPinConfirmed: (String) -> Unit,
) {
    var step by remember { mutableStateOf(PinStep.ENTER_NEW) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current

    val dialogBg = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
    val currentEntered = if (step == PinStep.ENTER_NEW) firstPin else confirmPin

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

    fun onDigitPress(digit: String) {
        if (isError) {
            isError = false
            errorMessage = null
        }
        if (currentEntered.length < 4) {
            val updated = currentEntered + digit
            if (step == PinStep.ENTER_NEW) {
                firstPin = updated
                if (updated.length == 4) {
                    step = PinStep.CONFIRM_NEW
                }
            } else {
                confirmPin = updated
                if (updated.length == 4) {
                    if (updated == firstPin) {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}
                        animateDismiss { onPinConfirmed(updated) }
                    } else {
                        isError = true
                        errorMessage = "PINs do not match. Try again."
                        firstPin = ""
                        confirmPin = ""
                        step = PinStep.ENTER_NEW
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun onBackspacePress() {
        if (isError) {
            isError = false
            errorMessage = null
        }
        if (step == PinStep.ENTER_NEW) {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = PinStep.ENTER_NEW
            }
        }
    }

    Dialog(
        onDismissRequest = { animateDismiss(onDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f * dialogAnim.value))
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
                        onClick = {}
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                        label = "PinStepAnim",
                    ) { currentStep ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentStep == PinStep.ENTER_NEW) "Set 4-Digit PIN" else "Confirm PIN",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 26.sp,
                                    lineHeight = 28.sp,
                                    color = Color.White,
                                ),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentStep == PinStep.ENTER_NEW) {
                                    "Create a PIN to protect this profile"
                                } else {
                                    "Re-enter the 4-digit PIN to confirm"
                                },
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    color = ProfileGlassTokens.TextMuted50,
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PinDots(
                        pinLength = currentEntered.length,
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
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PinKeypad(
                        onDigitClick = ::onDigitPress,
                        onBackspaceClick = ::onBackspacePress
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileGlassPill(
                        text = "Cancel",
                        onClick = onDismissRequest,
                        visualHeight = 36.dp,
                        fontSize = 13
                    )
                }
            }
        }
    }
}
