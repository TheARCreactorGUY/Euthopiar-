package com.euthopiar.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.settings.plugins.pluginsGlassSurface
import com.euthopiar.ui.settings.plugins.pluginsPressable

/**
 * Universal Frosted-Glass Alert & Confirmation Dialog for Euthopiar.
 *
 * Strict Design Specifications:
 * - Scrim: Full-screen black at 60% opacity (Color.Black.copy(alpha = 0.6f)).
 * - Card: Max width 360dp, 16dp horizontal margin, 28dp corner radius.
 * - Surface: Dark frosted glass acrylic (blur ~22px / Haze or AMOLED fallback, 0.5dp hairline border).
 * - Inner padding: 24dp all around.
 * - Icon tile (optional): 40x40dp rounded square, 13dp radius, glass fill (white 7%) with 0.5dp hairline (white 16%), 20dp outline icon.
 * - Headline: 22sp Medium, white 100%, line height 24sp, letter spacing -0.01em, max 3 lines with ellipsis.
 * - Body: 14sp Regular, white 60%, line height 20sp.
 * - Item row (optional): Bounded between two 0.5dp white 14% hairlines with 12dp vertical padding, 20dp space below before buttons.
 * - Buttons: 48dp height pills (radius 24dp), 12dp gap, 15sp Medium centered text, 98% press scale.
 *   - Cancel (Safe choice) on the LEFT: Solid white fill, near-black text (#0B0B0F), initial D-pad focus.
 *   - Confirm (Destructive/Action) on the RIGHT: Frosted glass fill (white 7%), 0.5dp hairline (white 16%), white text. NEVER RED.
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = onDismiss,
) {
    val haptic = LocalHapticFeedback.current
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            cancelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Scrim: 60% full-screen black overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog card: max 360dp, 16dp horizontal margin, 28dp radius, 24dp inner padding
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside card
                    )
                    .pluginsGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        isSheetOrDialog = true
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Optional Icon Tile: 40x40dp, 13dp radius, 20dp icon
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(
                                    0.5.dp,
                                    Color.White.copy(alpha = 0.16f),
                                    RoundedCornerShape(13.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Headline: 22sp Medium, white 100%, line height 24sp, letter spacing -0.01em
                    Text(
                        text = title,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                            letterSpacing = (-0.01).em
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Optional Body: 14sp Regular, white 60%, line height 20sp
                    if (message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            )
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
                                    .clip(RoundedCornerShape(24.dp))
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
                                    color = Color(0xFF0B0B0F),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Frosted Glass Action Pill on the RIGHT (strictly NEVER red)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .focusable()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(
                                        0.5.dp,
                                        Color.White.copy(alpha = 0.16f),
                                        RoundedCornerShape(24.dp)
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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
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
                                .clip(RoundedCornerShape(24.dp))
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
                                color = Color(0xFF0B0B0F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item row bounded by two 0.5dp hairlines (white 14%) with 12dp vertical padding.
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
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
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
                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(6.dp)),
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
                            color = Color.White.copy(alpha = 0.50f),
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
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}

/**
 * Item row bounded by two 0.5dp hairlines (white 14%) with 12dp vertical padding.
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
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
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
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
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
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}
