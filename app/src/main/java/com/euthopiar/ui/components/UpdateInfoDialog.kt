package com.euthopiar.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.euthopiar.data.update.ApkDownloadState
import com.euthopiar.data.update.AppUpdateManager
import com.euthopiar.data.update.ReleaseInfo
import com.euthopiar.data.update.isRemoteVersionNewer
import com.euthopiar.ui.settings.components.SettingsMetadataChip
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.components.settingsGlass
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun UpdateInfoDialog(
    release: ReleaseInfo,
    currentVersion: String,
    downloadState: ApkDownloadState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: (ReleaseInfo) -> Unit,
    onTriggerInstall: (File, String) -> Unit,
    onSkipVersion: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val updateManager = remember(context) { AppUpdateManager.getInstance(context) }
    val scrollState = rememberScrollState()
    val isNewerUpdate = remember(currentVersion, release.tagName) {
        isRemoteVersionNewer(currentVersion, release.tagName)
    }

    val formattedSize = remember(release.apkSizeBytes) {
        if (release.apkSizeBytes > 0L) {
            String.format(Locale.US, "%.1f MB", release.apkSizeBytes / (1024.0 * 1024.0))
        } else null
    }

    val cleanChangelog = remember(release.changelog) {
        val lines = release.changelog.lines()
            .map { line ->
                var l = line.trim()
                if (l.startsWith("### ")) l = l.removePrefix("### ").trim()
                else if (l.startsWith("## ")) l = l.removePrefix("## ").trim()
                else if (l.startsWith("# ")) l = l.removePrefix("# ").trim()
                if (l.startsWith("* ")) l = "• " + l.removePrefix("* ").trim()
                else if (l.startsWith("- ")) l = "• " + l.removePrefix("- ").trim()
                l = l.replace("**", "").replace("`", "")
                l
            }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) "Bug fixes, performance improvements and stability updates."
        else lines.joinToString("\n")
    }

    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    val dialogProgress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            dialogProgress.snapTo(1f)
        } else {
            dialogProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    dialogProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = dialogProgress.value
    val scrimAlpha = if (reduceMotion) 0.60f else (0.60f * animValue).coerceIn(0f, 0.60f)
    val cardScale = if (reduceMotion) 1f else 0.92f + 0.08f * animValue
    val cardAlpha = if (reduceMotion) 1f else animValue

    val cardShape = RoundedCornerShape(SettingsTokens.RadiusDialog)
    val cardFill = if (isAmoled) Color.Black.copy(alpha = 0.88f) else Color(0xFF0E1118).copy(alpha = 0.74f)
    val fallbackColor = if (isAmoled) Color.Black.copy(alpha = 0.94f) else Color(0xFF0E1118).copy(alpha = 0.94f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            ),
        contentAlignment = Alignment.Center
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Dialog Card Container
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent dismiss when clicking dialog card */ }
                )
                .clip(cardShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 22.dp
                            tints = listOf(HazeTint(cardFill))
                            fallbackTint = HazeTint(fallbackColor)
                        }
                    } else {
                        Modifier.background(fallbackColor)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = cardShape
                )
                .padding(24.dp)
        ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 40x40dp Glass Icon Tile
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .settingsGlass(
                                    shape = RoundedCornerShape(SettingsTokens.RadiusControl),
                                    blurRadius = 14.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdateAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNewerUpdate) "Update Available" else "Release Information",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp,
                                    color = SettingsTokens.TextWhite96
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${release.releaseTitle} · ${release.publishedAt}",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = SettingsTokens.TextWhite54
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Close "X" button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable(onClick = dismissWithAnim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = SettingsTokens.TextWhite72,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Version Comparison Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isNewerUpdate) {
                            SettingsMetadataChip(
                                text = "Current: v$currentVersion",
                                hazeState = hazeState
                            )

                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = SettingsTokens.TextWhite42,
                                modifier = Modifier.size(13.dp)
                            )

                            SettingsMetadataChip(
                                text = "New: v${release.versionName}",
                                hazeState = hazeState
                            )
                        } else {
                            SettingsMetadataChip(
                                text = "Installed: v$currentVersion",
                                hazeState = hazeState
                            )
                            SettingsMetadataChip(
                                text = release.tagName,
                                hazeState = hazeState
                            )
                        }

                        if (formattedSize != null) {
                            SettingsMetadataChip(
                                text = formattedSize,
                                hazeState = hazeState
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section Label: WHAT'S NEW
                    Text(
                        text = "WHAT'S NEW",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            letterSpacing = 0.8.sp,
                            color = SettingsTokens.TextWhite45
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Changelog Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 200.dp)
                            .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineDivider, RoundedCornerShape(SettingsTokens.RadiusControl))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = cleanChangelog,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = SettingsTokens.TextWhite82
                                )
                            )
                        }
                    }

                    // Download Progress View (when Downloading)
                    AnimatedVisibility(
                        visible = downloadState is ApkDownloadState.Downloading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (downloadState is ApkDownloadState.Downloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val percent = (downloadState.progressFraction * 100).toInt()
                                    Text(
                                        text = "Downloading APK: $percent%",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = SettingsTokens.TextWhite82
                                        )
                                    )
                                    if (downloadState.totalBytes > 0L) {
                                        val curMb = downloadState.downloadedBytes / (1024.0 * 1024.0)
                                        val totMb = downloadState.totalBytes / (1024.0 * 1024.0)
                                        Text(
                                            text = String.format(Locale.US, "%.1f / %.1f MB", curMb, totMb),
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = SettingsTokens.TextWhite54
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { downloadState.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.14f)
                                )
                            }
                        }
                    }

                    // Download Error Notice (when Error)
                    if (downloadState is ApkDownloadState.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Download issue: ${downloadState.message}. You can update via browser.",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = SettingsTokens.TextWhite72
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button (Full-width 46dp Solid White Pill)
                    val isDownloading = downloadState is ApkDownloadState.Downloading
                    val isReadyToInstall = downloadState is ApkDownloadState.ReadyToInstall

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(CircleShape)
                            .background(if (isDownloading) Color.White.copy(alpha = 0.6f) else Color.White)
                            .settingsPressable(
                                enabled = !isDownloading,
                                onClick = {
                                    if (isReadyToInstall) {
                                        onTriggerInstall((downloadState as ApkDownloadState.ReadyToInstall).apkFile, release.htmlUrl)
                                    } else {
                                        onDownloadAndInstall(release)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SettingsTokens.TextDark,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Downloading Update...",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = SettingsTokens.TextDark
                                    )
                                )
                            } else if (isReadyToInstall) {
                                Text(
                                    text = "Install Update",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = SettingsTokens.TextDark
                                    )
                                )
                            } else {
                                val primaryLabel = when {
                                    release.apkDownloadUrl != null && isNewerUpdate -> "Update Now"
                                    release.apkDownloadUrl != null -> "Download APK"
                                    else -> "View on GitHub"
                                }
                                Text(
                                    text = primaryLabel,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = SettingsTokens.TextDark
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action Buttons Row (Later & Skip / GitHub)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Later" / "Close" Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .settingsGlass(
                                    shape = CircleShape,
                                    blurRadius = 14.dp
                                )
                                .settingsPressable(onClick = dismissWithAnim),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isNewerUpdate) "Later" else "Close",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = SettingsTokens.TextWhite88
                                )
                            )
                        }

                        // "Skip v..." or "View on GitHub" Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .settingsGlass(
                                    shape = CircleShape,
                                    blurRadius = 14.dp
                                )
                                .settingsPressable(
                                    onClick = {
                                        if (isNewerUpdate) {
                                            onSkipVersion(release.versionName)
                                        } else {
                                            updateManager.openInBrowser(release.htmlUrl)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isNewerUpdate) "Skip v${release.versionName}" else "View on GitHub",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = SettingsTokens.TextWhite88
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
