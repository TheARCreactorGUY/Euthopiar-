package com.euthopiar.ui.settings.plugins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.ui.settings.PluginManagerViewModel
import com.euthopiar.ui.theme.LocalAmoledMode

/**
 * Screen 3: Provider Detail Bottom Sheet.
 * High-performance frosted glass sheet with spring physics, full description with expander,
 * supported types chips, provider health latency probe, and pinned monochrome action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailSheet(
    plugin: PluginManifest,
    isInstalled: Boolean,
    isInstalling: Boolean,
    isTesting: Boolean,
    health: PluginManagerViewModel.PluginHealth?,
    onTest: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current
    val context = LocalContext.current
    val isLowEnd = remember(context) { context.isLowEndDevice() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }

    val containerBg = when {
        isLowEnd -> PluginsGlassTokens.SheetGlassFallback
        isAmoled -> PluginsGlassTokens.SheetGlassAmoledFill
        else -> PluginsGlassTokens.SheetGlassFill
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        containerColor = containerBg,
        scrimColor = Color.Black.copy(alpha = 0.60f),
        dragHandle = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PluginsHairlineDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header: 52dp glass tile with 26dp outline puzzle icon, Name 26sp medium, Meta line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusProviderIcon)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = plugin.name,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val authorStr = if (plugin.author.isNotBlank()) " · By ${plugin.author}" else ""
                        val versionStr = "v${plugin.versionName.ifBlank { plugin.version.toString() }}$authorStr"
                        Text(
                            text = versionStr,
                            style = TextStyle(
                                color = PluginsGlassTokens.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Description (Clamped to 6 lines with More/Less expander)
                if (plugin.description.isNotBlank()) {
                    Column(
                        modifier = Modifier.animateContentSize(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = plugin.description,
                            style = TextStyle(
                                color = PluginsGlassTokens.TextSheetBody,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 6,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (plugin.description.length > 200) {
                            Text(
                                text = if (isDescriptionExpanded) "Show less" else "More",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .pluginsPressable(scaleDown = 0.98f) {
                                        isDescriptionExpanded = !isDescriptionExpanded
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Supports Section
                if (plugin.supportedTypes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SUPPORTS",
                            style = TextStyle(
                                color = PluginsGlassTokens.TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.1.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            plugin.supportedTypes.forEach { type ->
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(PluginsGlassTokens.RadiusTiny))
                                        .background(PluginsGlassTokens.GlassFill)
                                        .border(
                                            PluginsGlassTokens.HairlineWidth,
                                            PluginsGlassTokens.HairlineBorder,
                                            RoundedCornerShape(PluginsGlassTokens.RadiusTiny)
                                        )
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Provider Health Latency Probe Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PluginsHairlineDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(18.dp)
                            )

                            if (health != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (health.isWorking) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = health.message,
                                            style = TextStyle(
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.60f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Not responding",
                                            style = TextStyle(
                                                color = Color.White.copy(alpha = 0.60f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Diagnostic probe",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }

                        // Test / Retest Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(PluginsGlassTokens.GlassFill)
                                .border(
                                    PluginsGlassTokens.HairlineWidth,
                                    PluginsGlassTokens.HairlineBorder,
                                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                )
                                .pluginsPressable(enabled = !isTesting, scaleDown = 0.98f) {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onTest()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (health != null) "Retest" else "Test",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    PluginsHairlineDivider()
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Pinned Bottom Action Button (Crossfades smoothly, stays open after action)
            Crossfade(
                targetState = isInstalled,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "providerActionCrossfade",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) { installed ->
                if (installed) {
                    // Installed: glass "Uninstall provider" (NEVER red)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                            .background(PluginsGlassTokens.GlassFill)
                            .border(
                                PluginsGlassTokens.HairlineWidth,
                                PluginsGlassTokens.HairlineBorder,
                                RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                            )
                            .pluginsPressable(scaleDown = 0.98f, onClick = { showUninstallConfirm = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Uninstall provider",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Not installed: Solid white "Install provider"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                            .background(Color.White)
                            .pluginsPressable(enabled = !isInstalling, scaleDown = 0.98f, onClick = onInstall),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isInstalling) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = PluginsGlassTokens.TextDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Install provider",
                                    color = PluginsGlassTokens.TextDark,
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

    if (showUninstallConfirm) {
        PluginsSharedGlassDialog(
            title = "Uninstall ${plugin.name}?",
            message = "This provider will be removed from your stream aggregator and will no longer query for links.",
            confirmText = "Uninstall",
            dismissText = "Cancel",
            onConfirm = {
                showUninstallConfirm = false
                onUninstall()
            },
            onDismiss = { showUninstallConfirm = false },
            itemPreview = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = plugin.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "v${plugin.versionName.ifBlank { plugin.version.toString() }}",
                            color = PluginsGlassTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        )
    }
}
