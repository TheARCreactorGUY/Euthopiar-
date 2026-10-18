package com.euthopiar.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.data.profile.hashPin
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.roundToInt

/**
 * Reusable 4-dot PIN indicator with gentle shake animation (8 dp, 3 oscillations, 240 ms).
 * In reduced-motion mode: brief opacity pulse instead of shake.
 * Dots are outline circles (16 dp) that fill solid white when entered. No red!
 */
@Composable
fun PinDots(
    pinLength: Int,
    maxDigits: Int = 4,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val shakeOffset = remember { Animatable(0f) }
    val errorAlphaPulse = remember { Animatable(1f) }

    LaunchedEffect(isError) {
        if (isError) {
            if (reduceMotion) {
                errorAlphaPulse.animateTo(0.3f, tween(80))
                errorAlphaPulse.animateTo(1f, tween(80))
            } else {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 240
                        0f at 0
                        -8f at 40
                        8f at 80
                        -6f at 120
                        6f at 160
                        -3f at 200
                        0f at 240
                    }
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .graphicsLayer { alpha = errorAlphaPulse.value }
    ) {
        repeat(maxDigits) { index ->
            val isFilled = index < pinLength
            val dotColor = if (isFilled) Color.White else Color.Transparent
            val borderColor = if (isFilled) Color.White else Color.White.copy(alpha = 0.35f)

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(ProfileGlassTokens.HairlineWidthDp, borderColor, CircleShape)
            )
        }
    }
}

/**
 * Glass numeric keypad with large 22 sp regular numerals.
 * 64 dp circular glass buttons with 98% press scale.
 */
@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val keypadRows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        keypadRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { digit ->
                    when (digit) {
                        "" -> {
                            Box(modifier = Modifier.size(64.dp))
                        }
                        "DEL" -> {
                            KeypadButton(
                                onClick = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    onBackspaceClick()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                onClick = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    onDigitClick(digit)
                                }
                            ) {
                                Text(
                                    text = digit,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 22.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "keypadPress"
    )

    val currentFill = if (isPressed) ProfileGlassTokens.GlassFillPressed else ProfileGlassTokens.GlassFill

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(currentFill)
            .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * PIN Challenge Dialog shown when switching into a locked profile.
 * Restyled with frosted glass recipe, large 26 sp headline, white text, no red.
 */
@Composable
fun PinChallengeDialog(
    profile: UserProfile,
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
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
                    profile.pin == updated
                } else {
                    false
                }

                if (isValid) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    animateDismiss { onSuccess() }
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

    Dialog(
        onDismissRequest = { animateDismiss(onDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        onClick = {} // block click through
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter 4-digit PIN to switch in",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = ProfileGlassTokens.TextMuted50
                        )
                    )

                    Spacer(modifier = Modifier.height(22.dp))

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

private enum class SetupPinStep {
    ENTER_NEW,
    CONFIRM_NEW
}

/**
 * 2-Step PIN Creator Dialog: Enter PIN -> Confirm PIN -> Verified.
 * Restyled with frosted glass recipe, large 26 sp headline, white text, no red.
 */
@Composable
fun PinCreatorDialog(
    onDismissRequest: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    var step by remember { mutableStateOf(SetupPinStep.ENTER_NEW) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current

    val dialogBg = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
    val currentEntered = if (step == SetupPinStep.ENTER_NEW) firstPin else confirmPin

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
            if (step == SetupPinStep.ENTER_NEW) {
                firstPin = updated
                if (updated.length == 4) {
                    step = SetupPinStep.CONFIRM_NEW
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
                        step = SetupPinStep.ENTER_NEW
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
        if (step == SetupPinStep.ENTER_NEW) {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = SetupPinStep.ENTER_NEW
            }
        }
    }

    Dialog(
        onDismissRequest = { animateDismiss(onDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                        label = "PinSetupStepAnim"
                    ) { currentStep ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentStep == SetupPinStep.ENTER_NEW) "Set 4-Digit PIN" else "Confirm PIN",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 26.sp,
                                    lineHeight = 28.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentStep == SetupPinStep.ENTER_NEW) {
                                    "Create a PIN to protect this profile"
                                } else {
                                    "Re-enter the 4-digit PIN to confirm"
                                },
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    color = ProfileGlassTokens.TextMuted50
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

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
