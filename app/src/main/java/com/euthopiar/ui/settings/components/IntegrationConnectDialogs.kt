package com.euthopiar.ui.settings.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.euthopiar.ui.settings.plugins.pluginsPressable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.euthopiarGlassCard
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * High-Fidelity Frosted-Glass Dialog for Trakt.tv PKCE Authentication.
 *
 * Implements the Apple Design & UI Triumvirate craft standards:
 * - Window blur behind (FLAG_BLUR_BEHIND on API 31+) with 45px blur radius.
 * - Physics-based entrance: scale from 0.94f to 1.0f + 160ms alpha fade-in.
 * - Tactile error state: horizontal spring shake animation on verification failure.
 * - Standardized Euthopiar Dialog Tokens (28dp radius, 0.5dp hairline border, 65% black scrim).
 * - Step-by-step guidance with instant clipboard auto-extraction and direct web handoff.
 */
@Composable
fun TraktConnectDialog(
    hazeState: HazeState? = null,
    isTraktValidating: Boolean = false,
    traktAuthError: String? = null,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onClearError: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()

    var traktCodeInput by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(reduceMotion) }

    // Error shake animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(traktAuthError) {
        if (traktAuthError != null && !reduceMotion) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-8f) at 50
                    8f at 100
                    (-6f) at 150
                    6f at 200
                    (-3f) at 250
                    3f at 300
                    0f at 350
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.80f, stiffness = 500f),
        label = "traktDialogCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 160, easing = LinearOutSlowInEasing),
        label = "traktDialogCardAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) EuthopiarGlassTokens.ScrimAlpha else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "traktDialogScrimAlpha"
    )

    Dialog(
        onDismissRequest = { if (!isTraktValidating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: generateSequence(view.parent) { it.parent }
                    .filterIsInstance<DialogWindowProvider>()
                    .firstOrNull()?.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setDimAmount(0f)
                val lp = window.attributes
                lp.blurBehindRadius = 45
                window.attributes = lp
            }
        }

        val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)

        // Animated Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isTraktValidating) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog Card
            Box(
                modifier = Modifier
                    .widthIn(max = 410.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .imePadding()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside card
                    )
                    .euthopiarGlassCard(
                        shape = cardShape,
                        hazeState = hazeState,
                        isAmoled = isAmoled
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row with Tile, Titles and Close Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44x44dp Glass Icon Tile
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    RoundedCornerShape(13.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect Trakt.tv",
                                style = EuthopiarGlassTokens.HeaderTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "OAuth 2.0 PKCE • No client secret",
                                style = EuthopiarGlassTokens.HeaderSubtitle
                            )
                        }

                        // Close "X" Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    CircleShape
                                )
                                .pluginsPressable(
                                    scaleDown = 0.94f,
                                    onClick = {
                                        if (!isTraktValidating) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                            onDismiss()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close dialog",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EuthopiarGlassTokens.HairlineWidth)
                            .background(EuthopiarGlassTokens.HairlineDivider)
                    )

                    // Explanatory Body Copy
                    Text(
                        text = "Authorize Euthopiar with Trakt to automatically sync your watched history, scrobble media, and keep watchlists aligned across all your devices.",
                        style = EuthopiarGlassTokens.BodyText
                    )

                    // Step 1: External Authorization Handshake
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepIndicatorBadge(stepNumber = 1, label = "Authorize in browser")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .pluginsPressable(
                                    scaleDown = 0.98f,
                                    onClick = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val authUrl = com.euthopiar.core.sync.trakt.TraktClient.getInstance().getAuthorizationUrl()
                                            val verifier = com.euthopiar.core.sync.trakt.TraktClient.getInstance().pendingCodeVerifier
                                            if (!verifier.isNullOrBlank()) {
                                                // Persist synchronously to SharedPreferences so it survives backgrounding or process death
                                                context.getSharedPreferences("trakt_auth_prefs", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putString("pending_code_verifier", verifier)
                                                    .commit()

                                                coroutineScope.launch {
                                                    com.euthopiar.data.preferences.PreferencesRepository.getInstance(context).setTraktCodeVerifier(verifier)
                                                }
                                            }
                                            uriHandler.openUri(authUrl)
                                        } catch (_: Exception) {}
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open Trakt.tv Authorization",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sign in & tap 'Yes' to grant read/write access",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.54f)
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Text(
                            text = "Note: After authorizing on Trakt, your browser will redirect to localhost and show 'Unable to connect'. That is normal! Copy the URL from the browser bar and paste it below.",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        )
                    }

                    // Step 2: Code Extraction Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepIndicatorBadge(stepNumber = 2, label = "Paste redirect link or code")

                        val inputBorderColor by animateColorAsState(
                            targetValue = when {
                                isInputFocused -> Color.White.copy(alpha = 0.40f)
                                traktCodeInput.isNotBlank() -> Color.White.copy(alpha = 0.24f)
                                else -> EuthopiarGlassTokens.HairlineBorder
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "traktInputBorder"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    inputBorderColor,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.size(19.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (traktCodeInput.isEmpty()) {
                                        Text(
                                            text = "Paste redirect URL or authorization code...",
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.38f)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = traktCodeInput,
                                        onValueChange = {
                                            val raw = it.trim()
                                            val extracted = Regex("""code=([^&]+)""").find(raw)?.groupValues?.get(1) ?: raw
                                            traktCodeInput = extracted
                                            onClearError()
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isInputFocused = it.isFocused }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Paste or Clear Button
                                if (traktCodeInput.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .pluginsPressable(
                                                scaleDown = 0.90f,
                                                onClick = {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                    traktCodeInput = ""
                                                    onClearError()
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear input",
                                            tint = Color.White.copy(alpha = 0.70f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.09f))
                                            .border(
                                                EuthopiarGlassTokens.HairlineWidth,
                                                EuthopiarGlassTokens.HairlineBorder,
                                                CircleShape
                                            )
                                            .pluginsPressable(
                                                scaleDown = 0.94f,
                                                onClick = {
                                                    val text = clipboardManager.getText()?.text
                                                    if (!text.isNullOrBlank()) {
                                                        val raw = text.trim()
                                                        val extracted = Regex("""code=([^&]+)""").find(raw)?.groupValues?.get(1) ?: raw
                                                        traktCodeInput = extracted
                                                        onClearError()
                                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 11.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPaste,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Paste",
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.5.sp,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Inline Error Banner with Spring Shake
                    AnimatedVisibility(
                        visible = traktAuthError != null,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        if (traktAuthError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { translationX = shakeOffset.value }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE57373).copy(alpha = 0.12f))
                                    .border(
                                        EuthopiarGlassTokens.HairlineWidth,
                                        Color(0xFFE57373).copy(alpha = 0.35f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = traktAuthError,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color.White.copy(alpha = 0.92f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dialog Actions Footer: Cancel on left, Connect on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Frosted Cancel Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    CircleShape
                                )
                                .pluginsPressable(
                                    enabled = !isTraktValidating,
                                    scaleDown = 0.97f,
                                    onClick = {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                style = EuthopiarGlassTokens.SecondaryButtonText.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        // Solid White Connect Pill
                        val canConnect = traktCodeInput.isNotBlank() && !isTraktValidating
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(if (canConnect) Color.White else Color.White.copy(alpha = 0.35f))
                                .pluginsPressable(
                                    enabled = canConnect,
                                    scaleDown = 0.97f,
                                    onClick = {
                                        val raw = traktCodeInput.trim()
                                        val extractedCode = Regex("""code=([^&]+)""").find(raw)?.groupValues?.get(1) ?: raw
                                        if (extractedCode.isNotBlank()) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                            onConnect(extractedCode)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTraktValidating) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = EuthopiarGlassTokens.TextDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "Connect",
                                    style = EuthopiarGlassTokens.PrimaryButtonText.copy(
                                        color = if (canConnect) EuthopiarGlassTokens.TextDark else EuthopiarGlassTokens.TextDark.copy(alpha = 0.50f)
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

/**
 * High-Fidelity Frosted-Glass Dialog for Simkl API Token Authentication.
 *
 * Implements the Apple Design & UI Triumvirate craft standards:
 * - Window blur behind (FLAG_BLUR_BEHIND on API 31+) with 45px blur radius.
 * - Physics-based entrance: scale from 0.94f to 1.0f + 160ms alpha fade-in.
 * - Tactile error state: horizontal spring shake animation on invalid token.
 * - Standardized Euthopiar Dialog Tokens (28dp radius, 0.5dp hairline border, 65% black scrim).
 * - Direct developer dashboard shortcut and one-tap clipboard token import.
 */
@Composable
fun SimklConnectDialog(
    hazeState: HazeState? = null,
    isSimklValidating: Boolean = false,
    simklAuthError: String? = null,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onClearError: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()

    var simklTokenInput by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(reduceMotion) }

    // Error shake animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(simklAuthError) {
        if (simklAuthError != null && !reduceMotion) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-8f) at 50
                    8f at 100
                    (-6f) at 150
                    6f at 200
                    (-3f) at 250
                    3f at 300
                    0f at 350
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.80f, stiffness = 500f),
        label = "simklDialogCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 160, easing = LinearOutSlowInEasing),
        label = "simklDialogCardAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) EuthopiarGlassTokens.ScrimAlpha else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "simklDialogScrimAlpha"
    )

    Dialog(
        onDismissRequest = { if (!isSimklValidating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: generateSequence(view.parent) { it.parent }
                    .filterIsInstance<DialogWindowProvider>()
                    .firstOrNull()?.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setDimAmount(0f)
                val lp = window.attributes
                lp.blurBehindRadius = 45
                window.attributes = lp
            }
        }

        val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)

        // Animated Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isSimklValidating) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog Card
            Box(
                modifier = Modifier
                    .widthIn(max = 410.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .imePadding()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside card
                    )
                    .euthopiarGlassCard(
                        shape = cardShape,
                        hazeState = hazeState,
                        isAmoled = isAmoled
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row with Tile, Titles and Close Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44x44dp Glass Icon Tile
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    RoundedCornerShape(13.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect Simkl",
                                style = EuthopiarGlassTokens.HeaderTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Personal API Access Token",
                                style = EuthopiarGlassTokens.HeaderSubtitle
                            )
                        }

                        // Close "X" Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    CircleShape
                                )
                                .pluginsPressable(
                                    scaleDown = 0.94f,
                                    onClick = {
                                        if (!isSimklValidating) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                            onDismiss()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close dialog",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EuthopiarGlassTokens.HairlineWidth)
                            .background(EuthopiarGlassTokens.HairlineDivider)
                    )

                    // Explanatory Body Copy
                    Text(
                        text = "Authorize Euthopiar with Simkl to synchronize your Plan to Watch list, Watching progress, and completed anime/TV/movie history across all your devices.",
                        style = EuthopiarGlassTokens.BodyText
                    )

                    // Step 1: OAuth 2.0 PKCE Browser Handshake
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepIndicatorBadge(stepNumber = 1, label = "Authorize in browser")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .pluginsPressable(
                                    scaleDown = 0.98f,
                                    onClick = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val authUrl = com.euthopiar.core.sync.simkl.SimklClient.getInstance().getAuthorizationUrl()
                                            val verifier = com.euthopiar.core.sync.simkl.SimklClient.getInstance().pendingCodeVerifier
                                            if (!verifier.isNullOrBlank()) {
                                                context.getSharedPreferences("simkl_auth_prefs", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putString("pending_code_verifier", verifier)
                                                    .commit()
                                                coroutineScope.launch {
                                                    com.euthopiar.data.preferences.PreferencesRepository.getInstance(context).setSimklCodeVerifier(verifier)
                                                }
                                            }
                                            uriHandler.openUri(authUrl)
                                        } catch (_: Exception) {}
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open Simkl Authorization",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sign in & tap 'Authorize' to link automatically",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.54f)
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Step 2: Code or Token Input Container
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepIndicatorBadge(stepNumber = 2, label = "Or paste redirect URL / token")
                        val inputBorderColor by animateColorAsState(
                            targetValue = when {
                                isInputFocused -> Color.White.copy(alpha = 0.40f)
                                simklTokenInput.isNotBlank() -> Color.White.copy(alpha = 0.24f)
                                else -> EuthopiarGlassTokens.HairlineBorder
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "simklInputBorder"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    inputBorderColor,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.size(19.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (simklTokenInput.isEmpty()) {
                                        Text(
                                            text = "Paste redirect URL, code, or token...",
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.38f)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = simklTokenInput,
                                        onValueChange = {
                                            val raw = it.trim()
                                            val extracted = Regex("""code=([^&]+)""").find(raw)?.groupValues?.get(1) ?: raw
                                            simklTokenInput = extracted
                                            onClearError()
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isInputFocused = it.isFocused }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Paste or Clear Button
                                if (simklTokenInput.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .pluginsPressable(
                                                scaleDown = 0.90f,
                                                onClick = {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                    simklTokenInput = ""
                                                    onClearError()
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear input",
                                            tint = Color.White.copy(alpha = 0.70f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.09f))
                                            .border(
                                                EuthopiarGlassTokens.HairlineWidth,
                                                EuthopiarGlassTokens.HairlineBorder,
                                                CircleShape
                                            )
                                            .pluginsPressable(
                                                scaleDown = 0.94f,
                                                onClick = {
                                                    val text = clipboardManager.getText()?.text
                                                    if (!text.isNullOrBlank()) {
                                                        val raw = text.trim()
                                                        val extracted = Regex("""code=([^&]+)""").find(raw)?.groupValues?.get(1) ?: raw
                                                        simklTokenInput = extracted
                                                        onClearError()
                                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 11.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPaste,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Paste",
                                                style = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.5.sp,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Inline Error Banner with Spring Shake
                    AnimatedVisibility(
                        visible = simklAuthError != null,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        if (simklAuthError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { translationX = shakeOffset.value }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE57373).copy(alpha = 0.12f))
                                    .border(
                                        EuthopiarGlassTokens.HairlineWidth,
                                        Color(0xFFE57373).copy(alpha = 0.35f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = simklAuthError,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color.White.copy(alpha = 0.92f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dialog Actions Footer: Cancel on left, Connect on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Frosted Cancel Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    EuthopiarGlassTokens.HairlineWidth,
                                    EuthopiarGlassTokens.HairlineBorder,
                                    CircleShape
                                )
                                .pluginsPressable(
                                    enabled = !isSimklValidating,
                                    scaleDown = 0.97f,
                                    onClick = {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                style = EuthopiarGlassTokens.SecondaryButtonText.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        // Solid White Connect Pill
                        val canConnect = simklTokenInput.isNotBlank() && !isSimklValidating
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(if (canConnect) Color.White else Color.White.copy(alpha = 0.35f))
                                .pluginsPressable(
                                    enabled = canConnect,
                                    scaleDown = 0.97f,
                                    onClick = {
                                        val token = simklTokenInput.trim()
                                        if (token.isNotBlank()) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                            onConnect(token)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSimklValidating) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = EuthopiarGlassTokens.TextDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "Connect",
                                    style = EuthopiarGlassTokens.PrimaryButtonText.copy(
                                        color = if (canConnect) EuthopiarGlassTokens.TextDark else EuthopiarGlassTokens.TextDark.copy(alpha = 0.50f)
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

/**
 * Compact Step Indicator Pill Badge ("STEP 1", "STEP 2").
 */
@Composable
private fun StepIndicatorBadge(
    stepNumber: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    EuthopiarGlassTokens.HairlineWidth,
                    EuthopiarGlassTokens.HairlineBorder,
                    CircleShape
                )
                .padding(horizontal = 7.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "STEP $stepNumber",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.90f)
                )
            )
        }

        Text(
            text = label,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        )
    }
}
