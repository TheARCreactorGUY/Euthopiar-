package com.euthopiar.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Universal Frosted-Glass Alert & Confirmation Dialog for Euthopiar.
 *
 * Strictly adheres to the UpdateInfoDialog gold-standard formula:
 * - Scrim: Full-screen black at 65% opacity.
 * - Card: Max width 380dp, 16dp horizontal margin, 28dp corner radius.
 * - Surface: Dark frosted obsidian glass (blur 24dp Haze, 0.5dp hairline border @ 16% white).
 * - Inner padding: 24dp all around.
 * - Icon tile (optional): 44x44dp rounded square, 13dp radius, glass fill (white 7%) with 0.5dp hairline (white 16%).
 * - Headline: 20sp Bold, 96% white, line height 24sp.
 * - Body: 13.5sp Normal, 72% white, line height 19sp.
 * - Item row (optional): Bounded between two 0.5dp hairlines (white 10%) with 12dp vertical padding.
 * - Buttons: 48dp height pills (CircleShape), 12dp gap.
 *   - Cancel on the LEFT: Solid white pill, dark text (#0B0B0F), initial D-pad focus.
 *   - Confirm on the RIGHT: Frosted glass pill, 0.5dp hairline, white text. NEVER RED.
 *   - Single-button info mode: Full-width solid white pill when dismissText is null or blank.
 */
@Composable
fun EuthopiarGlassDialog(
    title: String,
    message: String = "",
    confirmText: String = "Confirm",
    dismissText: String? = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    itemPreview: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = onDismiss,
) {
    val haptic = LocalHapticFeedback.current
    val cancelFocusRequester = remember { FocusRequester() }
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    var isVisible by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(Unit) {
        isVisible = true
        try {
            cancelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 550f),
        label = "dialogCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 160, easing = LinearOutSlowInEasing),
        label = "dialogCardAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) EuthopiarGlassTokens.ScrimAlpha else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "dialogScrimAlpha"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
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
        val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
        val fallbackFill = EuthopiarGlassTokens.fallbackFill(isAmoled)

        // Scrim: Animated black overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog card: max 380dp, 16dp horizontal margin, 28dp radius, 24dp inner padding
            Box(
                modifier = modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                    .clip(cardShape)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurRadius = EuthopiarGlassTokens.BlurRadius
                                    tints = listOf(HazeTint(cardFill))
                                    fallbackTint = HazeTint(fallbackFill)
                                }
                            } else {
                                Modifier.background(cardFill)
                            }
                        } else {
                            Modifier.background(fallbackFill)
                        }
                    )
                    .border(
                        width = EuthopiarGlassTokens.HairlineWidth,
                        color = EuthopiarGlassTokens.HairlineBorder,
                        shape = cardShape
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Optional Icon Tile: 44x44dp, 13dp radius, 22dp icon
                    if (icon != null) {
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
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Headline: 20sp Bold, 96% white, line height 24sp
                    Text(
                        text = title,
                        style = EuthopiarGlassTokens.HeaderTitle,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Optional Body: 13.5sp Normal, 72% white, line height 19sp
                    if (message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = EuthopiarGlassTokens.BodyText
                        )
                    }

                    // Optional Item Row
                    if (itemPreview != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        itemPreview()
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Action Buttons: 48dp height pills, 12dp gap
                    val hasDismiss = !dismissText.isNullOrBlank()
                    if (hasDismiss) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Safe Solid White Cancel Pill on the LEFT (initial D-pad focus)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .focusRequester(cancelFocusRequester)
                                    .focusable()
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .pluginsPressable(
                                        scaleDown = 0.98f,
                                        onClick = {
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) {}
                                            onDismiss()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dismissText!!,
                                    style = EuthopiarGlassTokens.PrimaryButtonText
                                )
                            }

                            // Frosted Glass Action Pill on the RIGHT (strictly NEVER red)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .focusable()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(
                                        EuthopiarGlassTokens.HairlineWidth,
                                        EuthopiarGlassTokens.HairlineBorder,
                                        CircleShape
                                    )
                                    .pluginsPressable(
                                        scaleDown = 0.98f,
                                        onClick = {
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                            onConfirm()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmText,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Single-button information-only mode: full-width solid white pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusRequester(cancelFocusRequester)
                                .focusable()
                                .clip(CircleShape)
                                .background(Color.White)
                                .pluginsPressable(
                                    scaleDown = 0.98f,
                                    onClick = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        onConfirm()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                style = EuthopiarGlassTokens.PrimaryButtonText
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item row bounded by two 0.5dp hairlines (white 10%) with 12dp vertical padding.
 * Displays a single poster preview (36x52dp, 6dp radius), title, and metadata.
 */
@Composable
fun EuthopiarDialogSingleItemRow(
    title: String,
    metadata: String? = null,
    posterUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EuthopiarGlassTokens.HairlineWidth)
                .background(EuthopiarGlassTokens.HairlineDivider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AppAsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 36.dp, height = 52.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(EuthopiarGlassTokens.HairlineWidth, EuthopiarGlassTokens.HairlineBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!metadata.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metadata,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.54f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EuthopiarGlassTokens.HairlineWidth)
                .background(EuthopiarGlassTokens.HairlineDivider)
        )
    }
}

/**
 * Item row bounded by two 0.5dp hairlines (white 10%) with 12dp vertical padding.
 * Displays up to 3 overlapping posters (26x38dp, 6dp radius, 8dp overlap) and a summary label.
 */
@Composable
fun EuthopiarDialogMultiItemRow(
    posterUrls: List<String>,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EuthopiarGlassTokens.HairlineWidth)
                .background(EuthopiarGlassTokens.HairlineDivider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayPosters = posterUrls.take(3)
            val stackWidth = if (displayPosters.isNotEmpty()) {
                (26 + (displayPosters.size - 1) * 18).dp
            } else {
                26.dp
            }
            Box(
                modifier = Modifier.size(width = stackWidth, height = 38.dp)
            ) {
                displayPosters.forEachIndexed { index, url ->
                    Box(
                        modifier = Modifier
                            .padding(start = (index * 18).dp)
                            .size(width = 26.dp, height = 38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(EuthopiarGlassTokens.HairlineWidth, EuthopiarGlassTokens.HairlineBorder, RoundedCornerShape(6.dp))
                    ) {
                        AppAsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EuthopiarGlassTokens.HairlineWidth)
                .background(EuthopiarGlassTokens.HairlineDivider)
        )
    }
}
