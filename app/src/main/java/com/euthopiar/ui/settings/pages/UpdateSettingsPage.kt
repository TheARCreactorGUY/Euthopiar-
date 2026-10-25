package com.euthopiar.ui.settings.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.update.AppUpdateConfig
import com.euthopiar.data.update.AppUpdateManager
import com.euthopiar.data.update.ReleaseInfo
import com.euthopiar.data.update.UpdateCheckState
import com.euthopiar.ui.components.UpdateInfoDialog
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdateSettingsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember(context) { AppUpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()
    val downloadState by updateManager.downloadState.collectAsState()

    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }
    val autoCheckUpdates by preferencesRepository.autoCheckUpdates.collectAsState(initial = true)
    val includePrereleases by preferencesRepository.includePrereleases.collectAsState(initial = false)
    val lastUpdateCheckTimestamp by preferencesRepository.lastUpdateCheckTimestamp.collectAsState(initial = 0L)

    val currentVersion = remember(context) { updateManager.getCurrentVersionName() }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var activeDialogRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    val isChecking = updateState is UpdateCheckState.Checking

    val formattedLastCheckTime = remember(lastUpdateCheckTimestamp) {
        if (lastUpdateCheckTimestamp <= 0L) {
            "Never"
        } else {
            try {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                sdf.format(Date(lastUpdateCheckTimestamp))
            } catch (_: Exception) {
                "Recent"
            }
        }
    }

    val availableRelease = (updateState as? UpdateCheckState.UpdateAvailable)?.release
    val upToDateRelease = (updateState as? UpdateCheckState.UpToDate)?.latestRelease
    val displayRelease = availableRelease ?: upToDateRelease

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    val dialogRelease = activeDialogRelease ?: availableRelease ?: upToDateRelease

    SettingsScaffold(
        title = "Application Updates",
        onBack = onBack,
        modifier = modifier,
        overlay = { hazeState ->
            if (showUpdateDialog && dialogRelease != null) {
                UpdateInfoDialog(
                    release = dialogRelease,
                    currentVersion = currentVersion,
                    downloadState = downloadState,
                    hazeState = hazeState,
                    onDismiss = { showUpdateDialog = false },
                    onDownloadAndInstall = { rel -> updateManager.downloadAndInstallUpdate(rel) },
                    onTriggerInstall = { file, fallbackUrl -> updateManager.triggerInstallApk(file, fallbackUrl) },
                    onSkipVersion = { ver ->
                        updateManager.skipVersion(ver)
                        showUpdateDialog = false
                    }
                )
            }
        }
    ) { hazeState ->

        // Hero Status Block (Hairline-divided at bottom matching AboutSettingsPage)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 22.dp)
                .drawBehind {
                    drawLine(
                        color = SettingsTokens.HairlineDivider,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = SettingsTokens.HairlineWidth.toPx()
                    )
                }
                .padding(bottom = 20.dp)
        ) {
            // Glass Emblem Tile + Headline / Subtitle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 52x52dp Glass Emblem Tile
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusTileEmblem),
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdateAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Dynamic Status Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val statusHeadline = when (val state = updateState) {
                        is UpdateCheckState.Checking -> "Checking for updates..."
                        is UpdateCheckState.UpdateAvailable -> "Update v${state.release.versionName} Available"
                        is UpdateCheckState.UpToDate -> "Euthopiar is up to date"
                        is UpdateCheckState.Error -> "Update check failed"
                        is UpdateCheckState.Idle -> "Current Version: v$currentVersion"
                    }

                    Text(
                        text = statusHeadline,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.sp,
                            color = Color.White
                        )
                    )

                    Text(
                        text = "Installed: v$currentVersion · Last checked: $formattedLastCheckTime",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsMetadataChip(
                    text = "v$currentVersion",
                    hazeState = hazeState
                )
                SettingsMetadataChip(
                    text = if (includePrereleases) "Pre-release Channel" else "Stable Channel",
                    hazeState = hazeState
                )
                SettingsMetadataChip(
                    text = "GitHub Releases",
                    hazeState = hazeState
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row inside the Hero Block
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsPillButton(
                    text = if (isChecking) "Checking..." else "Check for Updates",
                    onClick = {
                        scope.launch {
                            updateManager.checkForUpdates(forceManual = true)
                        }
                    },
                    isSolidWhite = updateState !is UpdateCheckState.UpdateAvailable,
                    isLoading = isChecking,
                    enabled = !isChecking,
                    hazeState = hazeState,
                    modifier = Modifier.weight(1f)
                )

                if (availableRelease != null) {
                    SettingsPillButton(
                        text = "View & Install v${availableRelease.versionName}",
                        onClick = {
                            activeDialogRelease = availableRelease
                            showUpdateDialog = true
                        },
                        isSolidWhite = true,
                        hazeState = hazeState,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 1: Update Preferences
        OpenSettingsSectionHeader(title = "Update Preferences")

        OpenSettingsToggleRow(
            title = "Automatic Update Checks",
            subtitle = "Check GitHub Releases automatically when launching Euthopiar",
            checked = autoCheckUpdates,
            onCheckedChange = { enabled ->
                scope.launch {
                    preferencesRepository.setAutoCheckUpdates(enabled)
                }
            },
            hazeState = hazeState
        )

        OpenSettingsToggleRow(
            title = "Receive Pre-Release Updates",
            subtitle = "Include beta and release-candidate builds from GitHub",
            checked = includePrereleases,
            onCheckedChange = { enabled ->
                scope.launch {
                    preferencesRepository.setIncludePrereleases(enabled)
                    updateManager.checkForUpdates(forceManual = true)
                }
            },
            hazeState = hazeState
        )

        // Section 2: Latest Release Information
        OpenSettingsSectionHeader(title = "Latest Release Information")

        if (displayRelease != null) {
            OpenSettingsRow(
                title = "Latest GitHub Release",
                subtitle = "${displayRelease.releaseTitle} (${displayRelease.tagName}) · Published ${displayRelease.publishedAt}",
                onClick = {
                    activeDialogRelease = displayRelease
                    showUpdateDialog = true
                },
                trailingContent = {
                    SettingsMetadataChip(
                        text = displayRelease.tagName,
                        hazeState = hazeState
                    )
                }
            )

            val changelogFirstLine = displayRelease.changelog.lines().firstOrNull { it.isNotBlank() }
                ?: "Tap to read full release notes"

            OpenSettingsRow(
                title = "Release Notes & Changelog",
                subtitle = changelogFirstLine,
                onClick = {
                    activeDialogRelease = displayRelease
                    showUpdateDialog = true
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SettingsTokens.TextWhite42,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            OpenSettingsRow(
                title = "Direct APK Asset",
                subtitle = if (displayRelease.apkDownloadUrl != null) {
                    "Universal Release APK available for in-app install"
                } else {
                    "Open release page on GitHub"
                },
                onClick = {
                    activeDialogRelease = displayRelease
                    showUpdateDialog = true
                },
                trailingContent = {
                    SettingsPillButton(
                        text = if (displayRelease.apkDownloadUrl != null) "Download" else "Open",
                        isSolidWhite = false,
                        onClick = {
                            activeDialogRelease = displayRelease
                            showUpdateDialog = true
                        },
                        hazeState = hazeState
                    )
                }
            )
        } else {
            OpenSettingsRow(
                title = "Release Feed",
                subtitle = "Tap 'Check for Updates' above to query GitHub Releases",
                trailingContent = {
                    SettingsMetadataChip(
                        text = "GitHub API",
                        hazeState = hazeState
                    )
                }
            )
        }

        // Section 3: Source & Releases
        OpenSettingsSectionHeader(title = "Source & Releases")

        OpenSettingsRow(
            title = "GitHub Releases Page",
            subtitle = "Browse all historical versions and APKs on GitHub",
            onClick = { openUrl(AppUpdateConfig.GITHUB_RELEASES_URL) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SettingsTokens.TextWhite42,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}
