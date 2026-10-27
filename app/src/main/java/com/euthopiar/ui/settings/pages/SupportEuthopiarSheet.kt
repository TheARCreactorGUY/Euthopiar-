package com.euthopiar.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.euthopiar.R
import com.euthopiar.data.common.DonationConfig
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Settings "Support Euthopiar" Bottom Sheet.
 *
 * Implements fluid SeasonSheet-style drag-to-dismiss gesture physics:
 * - Direct drag on drag handle and header
 * - Nested scroll integration with content so dragging down from the top seamlessly drags the sheet down
 * - Velocity-aware flick to dismiss
 * - Dynamic scrim opacity responsive to drag offset
 * - Calibrated Emil Kowalski spring entrance and 160ms decelerating exit
 * - Tactile 0.96f button presses with Confirm haptic feedback
 */
@Composable
fun SupportEuthopiarSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    val sheetProgress = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            sheetProgress.snapTo(1f)
        } else {
            sheetProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f
                )
            )
        }
        delay(80L)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    sheetProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = sheetProgress.value
    val maxSheetHeight = (configuration.screenHeightDp * 0.90f).dp
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheetFill = if (isAmoled) Color.Black.copy(alpha = 0.88f) else Color(0xFF0E1118).copy(alpha = 0.74f)
    val fallbackColor = if (isAmoled) Color.Black.copy(alpha = 0.94f) else Color(0xFF0E1118).copy(alpha = 0.94f)

    // Dynamic scrim fade proportional to drag offset
    val scrimDragFactor = if (sheetHeightPx > 0f) (1f - (dragOffsetY / sheetHeightPx)).coerceIn(0f, 1f) else 1f
    val scrimAlpha = if (reduceMotion) 0.62f else (0.62f * animValue * scrimDragFactor).coerceIn(0f, 0.62f)

    // Content rise animation matching SeasonSheet
    val contentAlpha = if (reduceMotion) 1f else animValue
    val contentOffsetY = if (reduceMotion) 0f else (1f - animValue) * with(density) { 8.dp.toPx() }

    val openDonationLink = {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DonationConfig.BUY_ME_A_COFFEE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Buy Me a Coffee Link", DonationConfig.BUY_ME_A_COFFEE_URL)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "No browser found. Link copied to clipboard", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val copyDonationLink = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Buy Me a Coffee Link", DonationConfig.BUY_ME_A_COFFEE_URL)
            clipboard.setPrimaryClip(clip)
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    // Direct draggable state for drag handle & header
    val sheetDraggableState = rememberDraggableState { delta ->
        if (delta > 0 || dragOffsetY > 0) {
            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
        }
    }

    val onDragEnd = { velocity: Float ->
        if (dragOffsetY > 120f || velocity > 600f) {
            dismissWithAnim()
        } else {
            coroutineScope.launch {
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

    // Seamless NestedScrollConnection to allow pulling down the sheet from the scrollable body
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && dragOffsetY > 0f) {
                    val consumed = delta.coerceAtLeast(-dragOffsetY)
                    dragOffsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0 && available.y > 0 && scrollState.value == 0) {
                    dragOffsetY += delta
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY > 120f || available.y > 600f) {
                    dismissWithAnim()
                    return available
                } else if (dragOffsetY > 0f) {
                    coroutineScope.launch {
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
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            )
    ) {
        // Scrim with dynamic opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Bottom Sheet Container with SeasonSheet physics
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
                    onClick = { /* prevent dismiss on sheet body click */ }
                )
                .clip(sheetShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 22.dp
                            tints = listOf(HazeTint(sheetFill))
                            fallbackTint = HazeTint(fallbackColor)
                        }
                    } else {
                        Modifier.background(fallbackColor)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = sheetShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffsetY
                    }
            ) {
                // Drag Handle area with generous 40dp hit-box for direct dragging
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .draggable(
                            state = sheetDraggableState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> onDragEnd(velocity) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }

                // Scrollable Body with NestedScrollConnection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    // Eyebrow
                    Text(
                        text = "SUPPORT",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            letterSpacing = (11 * 0.08).sp,
                            color = SettingsTokens.TextWhite45
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = "Support Euthopiar",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            color = SettingsTokens.TextWhite96
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = "Free and open source. If it is part of your evenings, a coffee helps keep it going.",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = SettingsTokens.TextWhite72
                        )
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary Button: "Buy me a coffee" with tactile 0.96f scale
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .focusRequester(focusRequester)
                            .settingsPressable(scaleDown = 0.96f, onClick = openDonationLink)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedBorderColor = Accent,
                                onClick = openDonationLink
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Buy me a coffee",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = SettingsTokens.TextDark
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = SettingsTokens.TextDark,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Button: "Copy link" with tactile 0.96f scale
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                            .settingsPressable(scaleDown = 0.96f, onClick = copyDonationLink)
                            .tvFocusable(shape = CircleShape, onClick = copyDonationLink),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Copy link",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Divider Row: "OR SCAN FROM ANOTHER DEVICE"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = SettingsTokens.HairlineWidth,
                            color = SettingsTokens.HairlineDivider
                        )
                        Text(
                            text = "OR SCAN FROM ANOTHER DEVICE",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                letterSpacing = (11 * 0.08).sp,
                                color = SettingsTokens.TextWhite45,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = SettingsTokens.HairlineWidth,
                            color = SettingsTokens.HairlineDivider
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // QR Code Card (180dp white card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.qr_donation),
                                contentDescription = "Buy Me a Coffee QR Code",
                                modifier = Modifier.size(160.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
