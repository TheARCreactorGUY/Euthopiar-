package com.euthopiar.ui.library.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.euthopiar.data.repository.Playlist
import com.euthopiar.ui.navigation.LocalNavbarVisible
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Frosted Glass Bottom Sheet for creating a new playlist:
 * - 28dp top rounded frosted glass panel with 4x36dp handle
 * - In-window overlay supporting 24dp Haze blur
 * - 20sp Bold headline "New playlist"
 * - 48dp glass text field with 0.5dp hairline border
 * - 48dp solid white pill button "Create playlist"
 * - Vertical drag-to-dismiss gesture and animated black scrim
 */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    existingPlaylists: List<Playlist> = emptyList(),
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    var title by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val navbarVisible = LocalNavbarVisible.current

    DisposableEffect(Unit) {
        navbarVisible.value = false
        onDispose {
            navbarVisible.value = true
        }
    }

    val sheetProgress = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            sheetProgress.snapTo(1f)
        } else {
            sheetProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
    }

    val dismissWithAnim: (onComplete: (() -> Unit)?) -> Unit = { onComplete ->
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                if (reduceMotion) {
                    onComplete?.invoke()
                    onDismiss()
                } else {
                    sheetProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
                    )
                    onComplete?.invoke()
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim(null) }

    LaunchedEffect(Unit) {
        delay(120)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val shakeOffset = remember { Animatable(0f) }
    val trimmed = title.trim()
    val isDuplicate = remember(trimmed, existingPlaylists) {
        trimmed.isNotEmpty() && existingPlaylists.any { it.title.equals(trimmed, ignoreCase = true) }
    }

    val shakeAmplitudePx = with(density) { 8.dp.toPx() }

    fun triggerShake() {
        if (!reduceMotion) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
            } catch (_: Exception) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            scope.launch {
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(shakeAmplitudePx, tween(40, easing = LinearEasing))
                shakeOffset.animateTo(-shakeAmplitudePx, tween(40, easing = LinearEasing))
                shakeOffset.animateTo(shakeAmplitudePx * 0.6f, tween(35, easing = LinearEasing))
                shakeOffset.animateTo(-shakeAmplitudePx * 0.4f, tween(35, easing = LinearEasing))
                shakeOffset.animateTo(0f, tween(30, easing = LinearEasing))
            }
        }
    }

    val animValue = sheetProgress.value
    val maxSheetHeight = (configuration.screenHeightDp * 0.85f).dp
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    val sheetShape = RoundedCornerShape(topStart = EuthopiarGlassTokens.RadiusSheet, topEnd = EuthopiarGlassTokens.RadiusSheet)
    val sheetFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackFill = EuthopiarGlassTokens.fallbackFill(isAmoled)
    val scrimAlpha = if (reduceMotion) EuthopiarGlassTokens.ScrimAlpha else (EuthopiarGlassTokens.ScrimAlpha * (animValue / (250f / 340f))).coerceIn(0f, EuthopiarGlassTokens.ScrimAlpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissWithAnim(null) }
            )
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Sheet Container: slides up from bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .onGloballyPositioned { coordinates ->
                    sheetHeightPx = coordinates.size.height.toFloat()
                }
                .offset {
                    val slideDistance = if (sheetHeightPx > 0f) sheetHeightPx else with(density) { maxSheetHeight.toPx() }
                    val currentY = (1f - animValue) * slideDistance + dragOffsetY
                    IntOffset(0, currentY.roundToInt())
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through */ }
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || dragOffsetY > 0) {
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragOffsetY > 120f) {
                            dismissWithAnim(null)
                        } else {
                            scope.launch {
                                Animatable(dragOffsetY).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffsetY = value
                                }
                            }
                        }
                    }
                )
                .clip(sheetShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = EuthopiarGlassTokens.BlurRadius
                            tints = listOf(HazeTint(sheetFill))
                            fallbackTint = HazeTint(fallbackFill)
                        }
                    } else {
                        Modifier.background(fallbackFill)
                    }
                )
                .border(
                    width = EuthopiarGlassTokens.HairlineWidth,
                    color = EuthopiarGlassTokens.HairlineBorder,
                    shape = sheetShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Drag handle: 36 by 4 dp, white 28%
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                }

                Text(
                    text = "New playlist",
                    style = EuthopiarGlassTokens.HeaderTitle
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Glass Text Field (48dp tall, 16dp radius, 0.5dp hairline border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(
                            width = EuthopiarGlassTokens.HairlineWidth,
                            color = if (errorMessage != null) Color.White.copy(alpha = 0.50f) else EuthopiarGlassTokens.HairlineBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Playlist name",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.5.sp,
                                color = Color.White.copy(alpha = 0.40f)
                            )
                        )
                    }

                    BasicTextField(
                        value = title,
                        onValueChange = {
                            if (it.length <= 40) {
                                title = it
                                errorMessage = null
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.5.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                // Inline error message and live character counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B6B)
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = "${title.length}/40",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Button: 48dp Solid White Pill
                val isButtonActive = trimmed.isNotEmpty() && !isDuplicate
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (isButtonActive) Color.White else Color.White.copy(alpha = 0.35f))
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                if (trimmed.isEmpty()) {
                                    errorMessage = "Enter a name"
                                    triggerShake()
                                } else if (isDuplicate) {
                                    errorMessage = "You already have a playlist with that name"
                                    triggerShake()
                                } else {
                                    dismissWithAnim {
                                        onConfirm(trimmed)
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create playlist",
                        style = EuthopiarGlassTokens.PrimaryButtonText
                    )
                }
            }
        }
    }
}
