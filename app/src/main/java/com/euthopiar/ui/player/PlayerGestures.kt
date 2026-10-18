package com.euthopiar.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

private enum class DragMode { NONE, BRIGHTNESS, VOLUME }

/**
 * PlayerGestureOverlay: Master gesture matrix implementing:
 * 1. Double-Tap Seek Accumulation & Exit Sequencing State Machine (Section 4.1 & 3.2).
 * 2. Pinch-to-Zoom Gesture Hysteresis, Rubber-Banded Scale & Spring Handoff (Section 4.2 & 3.3).
 * 3. Volume Slider IPC Rate Limiting (48ms window), Fluid Spring & Icon Morphing (Section 3.4).
 * 4. Hold to 2x Speed with video overlay pill.
 */
@Composable
fun PlayerGestureOverlay(
    controlsVisible: Boolean,
    isHolding2x: Boolean,
    isGesturesPaused: Boolean = false,
    isScreenLocked: Boolean = false,
    currentAspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    onHold2xStart: () -> Unit,
    onHold2xEnd: () -> Unit,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (deltaMs: Long) -> Unit,
    onPinchAspect: (mode: AspectRatioMode, label: String) -> Unit = { _, _ -> },
    onPinchBadgeLabel: (label: String) -> Unit = {},
    onPinchScaleChanged: (scale: Float) -> Unit = {},
    onLockedScreenTap: () -> Unit = {},
    seekIntervalSeconds: Int = 10,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // HUD State
    var hudVisible by remember { mutableStateOf(false) }
    var isVolumeHud by remember { mutableStateOf(false) }
    var hudPercentage by remember { mutableIntStateOf(50) }
    var hudDismissJob by remember { mutableStateOf<Job?>(null) }
    var lastHapticThreshold by remember { mutableIntStateOf(-1) }

    // Double-Tap Seek State Machine Registers (Section 4.1)
    var internalAccumSeconds by remember { mutableIntStateOf(0) }
    var displayFrozenSeconds by remember { mutableIntStateOf(0) }
    var currentSign by remember { mutableIntStateOf(0) }
    var tapCount by remember { mutableIntStateOf(0) }
    var rippleAnchor by remember { mutableStateOf(Offset.Zero) }
    val waveProgress = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(1.0f) }
    var holdTimerJob by remember { mutableStateOf<Job?>(null) }
    var exitAnimJob by remember { mutableStateOf<Job?>(null) }
    var textBounceJob by remember { mutableStateOf<Job?>(null) }

    val displayString = remember(displayFrozenSeconds) {
        if (displayFrozenSeconds == 0) ""
        else "${if (displayFrozenSeconds > 0) "+" else "-"}${abs(displayFrozenSeconds)}s"
    }

    // Tap coordination state
    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnDoubleTapSeek by rememberUpdatedState(onDoubleTapSeek)
    val currentOnHold2xStart by rememberUpdatedState(onHold2xStart)
    val currentOnHold2xEnd by rememberUpdatedState(onHold2xEnd)
    val currentOnPinchAspect by rememberUpdatedState(onPinchAspect)
    val currentOnPinchBadgeLabel by rememberUpdatedState(onPinchBadgeLabel)
    val currentOnPinchScaleChanged by rememberUpdatedState(onPinchScaleChanged)
    val currentOnLockedScreenTap by rememberUpdatedState(onLockedScreenTap)

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapOffset by remember { mutableStateOf(Offset.Zero) }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }

    // Volume IPC Rate Limiter State (Section 3.4)
    var lastSentVolume by remember { mutableIntStateOf(-1) }
    var lastIpcTimeMs by remember { mutableLongStateOf(0L) }

    // Drag tracking
    var currentDragMode by remember { mutableStateOf(DragMode.NONE) }
    var dragVolumeAcc by remember { mutableFloatStateOf(0f) }
    var dragBrightnessAcc by remember { mutableFloatStateOf(0.5f) }

    // Pinch-to-zoom interactive state (Section 4.2)
    var initialPinchDist by remember { mutableFloatStateOf(-1f) }
    var activePinchStartMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var pendingPinchMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var isPinchActive by remember { mutableStateOf(false) }
    var interactiveSurfaceScale by remember { mutableFloatStateOf(1.0f) }

    fun triggerHud(isVol: Boolean, percent: Int) {
        isVolumeHud = isVol
        val clamped = percent.coerceIn(0, 100)
        hudPercentage = clamped
        hudVisible = true
        hudDismissJob?.cancel()
        hudDismissJob = scope.launch {
            delay(PlayerTokens.HudIdleTimeoutMs)
            hudVisible = false
        }
        val threshold = when {
            clamped == 0 -> 0
            clamped == 100 -> 100
            else -> -1
        }
        if (threshold != -1 && threshold != lastHapticThreshold) {
            lastHapticThreshold = threshold
            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
        } else if (threshold == -1) {
            lastHapticThreshold = -1
        }
    }

    fun handleDoubleTapSeek(sigma: Int, tapPos: Offset) {
        holdTimerJob?.cancel()
        exitAnimJob?.cancel()

        if (currentSign != 0 && currentSign != sigma) {
            internalAccumSeconds = 0
            tapCount = 1
        } else {
            tapCount += 1
        }
        currentSign = sigma
        internalAccumSeconds += (sigma * seekIntervalSeconds)
        displayFrozenSeconds = internalAccumSeconds

        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
        currentOnDoubleTapSeek(sigma * seekIntervalSeconds * 1000L)

        if (tapCount == 1) {
            rippleAnchor = tapPos
            scope.launch {
                overlayAlpha.snapTo(1.0f)
                waveProgress.snapTo(0.0f)
                waveProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(
                        durationMillis = PlayerMotionMath.T_WAVE_MS.toInt(),
                        easing = PlayerTokens.EaseOut
                    )
                )
            }
        } else {
            val tauCurr = waveProgress.value
            val tauNew = PlayerMotionMath.reanchorWaveProgress(tauCurr)
            rippleAnchor = PlayerMotionMath.smoothAnchorCenter(rippleAnchor, tapPos)
            val speedFactor = PlayerMotionMath.wavePropagationSpeedFactor(tapCount)
            val remainingMs = ((PlayerMotionMath.T_WAVE_MS * (1.0f - tauNew)) / speedFactor).toLong().coerceIn(120L, 550L)

            scope.launch {
                overlayAlpha.snapTo(1.0f)
                waveProgress.snapTo(tauNew)
                waveProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(
                        durationMillis = remainingMs.toInt(),
                        easing = PlayerTokens.EaseOut
                    )
                )
            }
        }

        // Text Scale Pop Impulse S(n)
        textBounceJob?.cancel()
        textBounceJob = scope.launch {
            val sAmp = minOf(0.32f, 0.18f + 0.035f * ln(tapCount.toFloat()))
            textScale.snapTo(1.0f + sAmp)
            textScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = PlayerMotionMath.TEXT_BOUNCE_ZETA, stiffness = PlayerMotionMath.TEXT_BOUNCE_K)
            )
        }

        // Hold timer: 650ms before entering exit fade
        holdTimerJob = scope.launch {
            delay(PlayerMotionMath.HOLD_WINDOW_MS)
            // Transition to EXITING_FADE: DO NOT mutate displayFrozenSeconds!
            exitAnimJob = launch {
                overlayAlpha.animateTo(
                    targetValue = 0.0f,
                    animationSpec = tween(
                        durationMillis = PlayerMotionMath.T_FADE_MS.toInt(),
                        easing = CubicBezierEasing(0.40f, 0.0f, 0.20f, 1.0f)
                    )
                )
                // Transition to CLEANUP_RESET
                internalAccumSeconds = 0
                displayFrozenSeconds = 0
                tapCount = 0
                currentSign = 0
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(screenWidthPx, screenHeightPx, isGesturesPaused, isScreenLocked) {
                if (isGesturesPaused) return@pointerInput

                if (isScreenLocked) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                currentOnLockedScreenTap()
                                break
                            }
                        }
                    }
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val edgeGuardPx = with(density) { 24.dp.toPx() }
                    if (startPos.x < edgeGuardPx || startPos.x > (screenWidthPx - edgeGuardPx)) {
                        return@awaitEachGesture
                    }

                    val isLeftThird = startPos.x < (screenWidthPx / 3f)
                    val isRightThird = startPos.x > (screenWidthPx * 2f / 3f)
                    val isBrightnessDragZone = isLeftThird
                    val isVolumeDragZone = isRightThird

                    val touchSlop = viewConfiguration.touchSlop
                    val minVerticalDragSlop = touchSlop * 2.5f
                    var isDragging = false
                    var isHold2xActive = false
                    var lastY = startPos.y

                    var holdJob: Job? = null
                    if (!currentControlsVisible) {
                        holdJob = scope.launch {
                            delay(300)
                            if (!isDragging && !isPinchActive) {
                                isHold2xActive = true
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                currentOnHold2xStart()
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()

                        // 1. Two-Finger Pinch Gesture (Section 4.2)
                        val pressedPointers = event.changes.filter { it.pressed }
                        if (pressedPointers.size >= 2) {
                            holdJob?.cancel()
                            if (isHold2xActive) {
                                isHold2xActive = false
                                currentOnHold2xEnd()
                            }
                            val p0 = pressedPointers[0].position
                            val p1 = pressedPointers[1].position
                            val dist = (p0 - p1).getDistance()

                            pressedPointers[0].consume()
                            pressedPointers[1].consume()

                            if (!isPinchActive) {
                                if (abs(dist - initialPinchDist) > touchSlop || initialPinchDist <= 0f) {
                                    initialPinchDist = dist
                                    activePinchStartMode = currentAspectRatioMode
                                    pendingPinchMode = activePinchStartMode
                                    isPinchActive = true
                                    interactiveSurfaceScale = if (activePinchStartMode == AspectRatioMode.ZOOM) 1.33f else 1.0f
                                    currentOnPinchBadgeLabel(if (pendingPinchMode == AspectRatioMode.ZOOM) "FILL" else "FIT")
                                }
                            } else {
                                val r = if (initialPinchDist > 0f) dist / initialPinchDist else 1.0f
                                val committedScale = if (activePinchStartMode == AspectRatioMode.ZOOM) 1.33f else 1.0f
                                val surfaceScale = PlayerMotionMath.rubberBandedVideoScale(r, committedScale, sFit = 1.0f, sFill = 1.33f)
                                interactiveSurfaceScale = surfaceScale
                                currentOnPinchScaleChanged(surfaceScale)

                                val evaluatedMode = PlayerMotionMath.evaluatePinchHysteresis(pendingPinchMode, surfaceScale, r, sFit = 1.0f, sFill = 1.33f)
                                if (evaluatedMode != pendingPinchMode) {
                                    pendingPinchMode = evaluatedMode
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    currentOnPinchBadgeLabel(if (evaluatedMode == AspectRatioMode.ZOOM) "FILL" else "FIT")
                                }
                            }
                        } else if (isPinchActive) {
                            // Pointer release during pinch: trigger settling spring handoff immediately
                            isPinchActive = false
                            initialPinchDist = -1f
                            val targetScale = if (pendingPinchMode == AspectRatioMode.ZOOM) 1.33f else 1.0f
                            scope.launch {
                                val anim = Animatable(interactiveSurfaceScale)
                                anim.animateTo(
                                    targetValue = targetScale,
                                    animationSpec = spring(stiffness = PlayerMotionMath.SPRING_PINCH_K, dampingRatio = PlayerMotionMath.SPRING_PINCH_ZETA)
                                ) {
                                    interactiveSurfaceScale = this.value
                                    currentOnPinchScaleChanged(this.value)
                                }
                                currentOnPinchAspect(pendingPinchMode, if (pendingPinchMode == AspectRatioMode.ZOOM) "FILL" else "FIT")
                                currentOnPinchScaleChanged(targetScale)
                            }
                            break
                        }

                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        // Pointer Release
                        if (!change.pressed) {
                            holdJob?.cancel()

                            if (isHold2xActive) {
                                isHold2xActive = false
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                currentOnHold2xEnd()
                                change.consume()
                                break
                            }

                            // Pinch Release Handoff (Section 4.2 Settling Spring)
                            if (isPinchActive) {
                                isPinchActive = false
                                initialPinchDist = -1f
                                val targetScale = if (pendingPinchMode == AspectRatioMode.ZOOM) 1.33f else 1.0f
                                scope.launch {
                                    val anim = Animatable(interactiveSurfaceScale)
                                    anim.animateTo(
                                        targetValue = targetScale,
                                        animationSpec = spring(stiffness = PlayerMotionMath.SPRING_PINCH_K, dampingRatio = PlayerMotionMath.SPRING_PINCH_ZETA)
                                    ) {
                                        interactiveSurfaceScale = this.value
                                        currentOnPinchScaleChanged(this.value)
                                    }
                                    currentOnPinchAspect(pendingPinchMode, if (pendingPinchMode == AspectRatioMode.ZOOM) "FILL" else "FIT")
                                    currentOnPinchScaleChanged(targetScale)
                                }
                                break
                            }

                            if (!isDragging) {
                                val now = System.currentTimeMillis()
                                val tapX = change.position.x
                                val isLeftHalf = tapX < (screenWidthPx * 0.5f)

                                val isOngoingSeek = overlayAlpha.value > 0.05f || internalAccumSeconds != 0
                                if (isOngoingSeek) {
                                    pendingSingleTapJob?.cancel()
                                    pendingSingleTapJob = null
                                    val sigma = if (isLeftHalf) -1 else 1
                                    handleDoubleTapSeek(sigma, change.position)
                                } else if (!currentControlsVisible) {
                                    val isDoubleTap = (now - lastTapTime < 300L) &&
                                            ((change.position - lastTapOffset).getDistance() < touchSlop * 2)

                                    if (isDoubleTap) {
                                        pendingSingleTapJob?.cancel()
                                        pendingSingleTapJob = null
                                        lastTapTime = 0L
                                        val sigma = if (isLeftHalf) -1 else 1
                                        handleDoubleTapSeek(sigma, change.position)
                                    } else {
                                        lastTapTime = now
                                        lastTapOffset = change.position
                                        pendingSingleTapJob?.cancel()
                                        pendingSingleTapJob = scope.launch {
                                            delay(300L)
                                            currentOnSingleTap()
                                        }
                                    }
                                } else {
                                    pendingSingleTapJob?.cancel()
                                    pendingSingleTapJob = null
                                    lastTapTime = 0L
                                    currentOnSingleTap()
                                }
                            } else {
                                // Terminal volume flush on drag gesture release (Section 3.4)
                                if (currentDragMode == DragMode.VOLUME) {
                                    val finalVol = dragVolumeAcc.roundToInt().coerceIn(0, maxVolume)
                                    val now = System.currentTimeMillis()
                                    if (PlayerMotionMath.shouldDispatchVolumeIpc(finalVol, lastSentVolume, now, lastIpcTimeMs, maxVolume, isGestureRelease = true)) {
                                        lastSentVolume = finalVol
                                        lastIpcTimeMs = now
                                        scope.launch(Dispatchers.Default) {
                                            try { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, finalVol, 0) } catch (_: Exception) {}
                                        }
                                    }
                                }
                                currentDragMode = DragMode.NONE
                                isDragging = false
                            }
                            break
                        }

                        if (!isDragging && (change.position - startPos).getDistance() > touchSlop * 2) {
                            holdJob?.cancel()
                            if (isHold2xActive) {
                                isHold2xActive = false
                                currentOnHold2xEnd()
                            }
                        }

                        val totalDeltaX = abs(change.position.x - startPos.x)
                        val totalDeltaY = abs(change.position.y - startPos.y)
                        val canInitiateDrag = !currentControlsVisible && !isPinchActive && (isBrightnessDragZone || isVolumeDragZone)

                        if (!isDragging && canInitiateDrag && totalDeltaY > minVerticalDragSlop && totalDeltaY > (totalDeltaX * 1.8f)) {
                            holdJob?.cancel()
                            if (isHold2xActive) {
                                isHold2xActive = false
                                currentOnHold2xEnd()
                            }
                            isDragging = true
                            currentDragMode = if (isBrightnessDragZone) DragMode.BRIGHTNESS else DragMode.VOLUME

                            if (currentDragMode == DragMode.BRIGHTNESS) {
                                val win = activity?.window
                                val current = win?.attributes?.screenBrightness ?: -1f
                                dragBrightnessAcc = if (current < 0f) {
                                    try {
                                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                                    } catch (_: Exception) {
                                        0.5f
                                    }
                                } else current
                                dragBrightnessAcc = dragBrightnessAcc.coerceIn(0.0f, 1.0f)
                                triggerHud(false, (dragBrightnessAcc * 100).roundToInt())
                            } else {
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                dragVolumeAcc = curVol.toFloat()
                                lastSentVolume = curVol
                                lastIpcTimeMs = System.currentTimeMillis()
                                val pct = ((dragVolumeAcc / maxVolume) * 100).toInt()
                                triggerHud(true, pct)
                            }
                            lastY = change.position.y
                        }

                        if (isDragging) {
                            change.consume()
                            val dragAmount = change.position.y - lastY
                            lastY = change.position.y
                            val deltaFraction = -dragAmount / (screenHeightPx * 0.9f)

                            if (currentDragMode == DragMode.BRIGHTNESS) {
                                dragBrightnessAcc = (dragBrightnessAcc + deltaFraction).coerceIn(0.0f, 1.0f)
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = if (dragBrightnessAcc <= 0.001f) 0.001f else dragBrightnessAcc
                                    act.window.attributes = lp
                                }
                                triggerHud(false, (dragBrightnessAcc * 100).roundToInt())
                            } else if (currentDragMode == DragMode.VOLUME) {
                                val deltaVol = deltaFraction * maxVolume
                                dragVolumeAcc = (dragVolumeAcc + deltaVol).coerceIn(0f, maxVolume.toFloat())
                                val targetVolInt = dragVolumeAcc.roundToInt().coerceIn(0, maxVolume)
                                val now = System.currentTimeMillis()

                                // Binder IPC Rate Limiting (Section 3.4)
                                if (PlayerMotionMath.shouldDispatchVolumeIpc(targetVolInt, lastSentVolume, now, lastIpcTimeMs, maxVolume, isGestureRelease = false)) {
                                    lastSentVolume = targetVolInt
                                    lastIpcTimeMs = now
                                    scope.launch(Dispatchers.Default) {
                                        try { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolInt, 0) } catch (_: Exception) {}
                                    }
                                }

                                val pct = ((dragVolumeAcc / maxVolume) * 100).toInt()
                                triggerHud(true, pct)
                            }
                        }
                    }
                }
            }
    ) {
        // Double-Tap Seek Ripple Overlay (Left half)
        if (overlayAlpha.value > 0.001f && currentSign < 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
            ) {
                SeekRippleOverlay(
                    isForward = false,
                    rippleAnchor = rippleAnchor,
                    waveProgress = waveProgress.value,
                    overlayAlpha = overlayAlpha.value,
                    textScale = textScale.value,
                    displayString = displayString
                )
            }
        }

        // Double-Tap Seek Ripple Overlay (Right half)
        if (overlayAlpha.value > 0.001f && currentSign > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
            ) {
                SeekRippleOverlay(
                    isForward = true,
                    rippleAnchor = rippleAnchor,
                    waveProgress = waveProgress.value,
                    overlayAlpha = overlayAlpha.value,
                    textScale = textScale.value,
                    displayString = displayString
                )
            }
        }

        // Vertical Sliders (Brightness left, Volume right)
        AnimatedVisibility(
            visible = hudVisible,
            enter = fadeIn(tween(PlayerTokens.HudEnterMs)),
            exit = fadeOut(tween(PlayerTokens.HudExitMs)),
            modifier = Modifier
                .align(if (isVolumeHud) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 20.dp)
        ) {
            VerticalSliderCapsule(
                isVolume = isVolumeHud,
                percentage = hudPercentage
            )
        }

        // Top-center 2X Speed Pill
        AnimatedVisibility(
            visible = isHolding2x,
            enter = if (reduceMotion) {
                fadeIn(tween(PlayerTokens.ReduceMotionFadeInMs))
            } else {
                fadeIn(tween(PlayerTokens.Pill2XEnterMs)) + slideInVertically(
                    initialOffsetY = { -with(density) { 4.dp.roundToPx() } },
                    animationSpec = tween(PlayerTokens.Pill2XEnterMs, easing = PlayerTokens.EaseOut)
                )
            },
            exit = fadeOut(tween(PlayerTokens.Pill2XExitMs)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (controlsVisible) 68.dp else PlayerTokens.PillMarginTop)
        ) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(PlayerTokens.IconPill)
                    )
                    Text(
                        text = "2X SPEED",
                        style = PlayerTokens.PillText,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Vertical Slider Capsule (Section 3.4).
 * Tracks volume/brightness through critically damped second-order spring (k = 1200, zeta = 1.0).
 * Continuous Hermite icon crossfading and tint interpolation over window [0.09, 0.19].
 */
@Composable
fun VerticalSliderCapsule(
    isVolume: Boolean,
    percentage: Int
) {
    val targetFill = (percentage / 100f).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = spring(dampingRatio = PlayerMotionMath.SPRING_VOLUME_ZETA, stiffness = PlayerMotionMath.SPRING_VOLUME_K),
        label = "capsuleFill"
    )

    val weights = PlayerMotionMath.computeVolumeIconWeights(animatedFill)
    val iconTint = androidx.compose.ui.graphics.lerp(Color.White, PlayerTokens.LabelDark, weights.tintProgress)

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(120.dp)
            .videoOverlayGlass(shape = RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // White 85% fill rising from the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFill)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.85f))
        )

        // Outline icon inside at bottom (18 dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isVolume) {
                Box(contentAlignment = Alignment.Center) {
                    if (weights.alphaMute > 0.01f) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeMute,
                            contentDescription = "Mute",
                            tint = iconTint,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    alpha = weights.alphaMute
                                    scaleX = weights.scaleMute
                                    scaleY = weights.scaleMute
                                }
                        )
                    }
                    if (weights.alphaDown > 0.01f) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeDown,
                            contentDescription = "Volume Down",
                            tint = iconTint,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    alpha = weights.alphaDown
                                    scaleX = weights.scaleDown
                                    scaleY = weights.scaleDown
                                }
                        )
                    }
                    if (weights.alphaUp > 0.01f) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = iconTint,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    alpha = weights.alphaUp
                                    scaleX = weights.scaleUp
                                    scaleY = weights.scaleUp
                                }
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = "Brightness",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * SeekRippleOverlay (Section 3.2 & 4.1).
 * Continuous radial shockwave expansion, phase-coherent chevrons, underdamped text bounce S(n).
 */
@Composable
private fun SeekRippleOverlay(
    isForward: Boolean,
    @Suppress("UNUSED_PARAMETER") rippleAnchor: Offset,
    @Suppress("UNUSED_PARAMETER") waveProgress: Float,
    overlayAlpha: Float,
    textScale: Float,
    displayString: String
) {
    if (overlayAlpha <= 0.001f || displayString.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "chevrons")
    val chevronCycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = PlayerMotionMath.T_CHEVRON_CYCLE_S,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chevronTime"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // Chevrons and Text Scale Bounce (Pure monochrome floating icons + typography, zero background tint)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                alpha = overlayAlpha
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                for (idx in 0 until 3) {
                    val rawChevronAlpha = PlayerMotionMath.chevronOpacity(idx, chevronCycleProgress, isForward)
                    val chevronAlpha = rawChevronAlpha * overlayAlpha
                    val chevronOffsetX = PlayerMotionMath.chevronMicroTranslationDp(rawChevronAlpha, isForward)
                    Icon(
                        imageVector = if (isForward) Icons.AutoMirrored.Outlined.NavigateNext else Icons.AutoMirrored.Outlined.NavigateBefore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = chevronAlpha),
                        modifier = Modifier
                            .offset(x = chevronOffsetX.dp)
                            .size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = displayString,
                style = PlayerTokens.Timecode.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                }
            )
        }
    }
}
