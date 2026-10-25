package com.euthopiar.ui.settings.pages

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.data.backup.BackupSummary
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper to split bytes into numeral + unit for smooth roll-down animations.
 */
private fun formatBytesSplit(bytes: Long): Pair<String, String> {
    return when {
        bytes <= 0L -> "0.00" to "MB"
        bytes < 1024L -> String.format(Locale.US, "%d", bytes) to "B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f", bytes / 1024f) to "KB"
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f", bytes / (1024f * 1024f)) to "MB"
        else -> String.format(Locale.US, "%.2f", bytes / (1024f * 1024f * 1024f)) to "GB"
    }
}

/**
 * Redesigned "Storage & cache" Settings Screen.
 *
 * Strict High-Fidelity Design Specifications:
 * - Editorial Top Bar with 40x40dp glass back button & crossfading collapsed title.
 * - Bold 32sp Medium headline ("Storage & cache").
 * - Hero Telemetry Block: Large 38sp Medium readout on baseline with tabular figures (`tnum`).
 * - Thin 2dp horizontal progress bar (white 90% cache + white 38% config) with 3dp gap.
 * - Legend with 6dp solid circle and 6dp hollow ring.
 * - Open hairline-divided sections (CACHE MAINTENANCE, DOWNLOADS & OFFLINE STORAGE, CONFIGURATION BACKUP).
 * - Custom Glass Toggle (48x28dp) for Download over Wi-Fi only.
 * - Shared Glass Confirmation Dialog for cache clearing (white Cancel pill on left, glass Clear cache pill on right).
 * - Signature Synchronized Cache-Drain Animation: 550ms numeric countdown + 550ms 2dp bar drain + 400ms track release pulse.
 * - Reduce-motion fallback support.
 */
@Composable
fun StorageSettingsPage(
    onBack: () -> Unit,
    viewModel: StorageSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()

    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsState()
    val downloadsSizeBytes by viewModel.downloadsSizeBytes.collectAsState()
    val configSizeBytes by viewModel.configSizeBytes.collectAsState()
    val customDownloadDir by viewModel.customDownloadDir.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()
    val wifiOnlyDownloads by viewModel.wifiOnlyDownloads.collectAsState()

    var showClearConfirmation by remember { mutableStateOf(false) }
    var isDrainAnimating by remember { mutableStateOf(false) }
    var startingDrainBytes by remember { mutableStateOf(0L) }

    val backupUiState by viewModel.backupUiState.collectAsState()
    val isExporting = backupUiState is BackupUiState.Exporting
    val isImporting = backupUiState is BackupUiState.Inspecting || backupUiState is BackupUiState.Restoring

    // Handle toast notification for export completion
    LaunchedEffect(backupUiState) {
        val state = backupUiState
        if (state is BackupUiState.ExportSuccess) {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            viewModel.dismissBackupState()
        }
    }

    // Animated float tracking the displayed byte size for the 550ms countdown roll-down
    val animatedByteSize = remember { Animatable(cacheSizeBytes.toFloat().coerceAtLeast(0f)) }

    // Pulse alpha sweep across the 2dp track upon drain completion
    val trackPulseAlpha = remember { Animatable(0f) }

    // Keep animatedByteSize in sync when not in the middle of clearing
    LaunchedEffect(cacheSizeBytes) {
        if (!isDrainAnimating && !isClearing) {
            animatedByteSize.snapTo(cacheSizeBytes.toFloat())
        }
    }

    val (currentNumeral, currentUnit) = remember(animatedByteSize.value, isDrainAnimating) {
        if (isDrainAnimating) {
            val v = animatedByteSize.value
            if (startingDrainBytes >= 1024L * 1024L * 1024L) {
                val gbs = maxOf(0f, v) / (1024f * 1024f * 1024f)
                val unit = if (v <= 0.001f) "MB" else "GB"
                String.format(Locale.US, "%.2f", gbs) to unit
            } else {
                val mbs = maxOf(0f, v) / (1024f * 1024f)
                String.format(Locale.US, "%.2f", mbs) to "MB"
            }
        } else {
            formatBytesSplit(animatedByteSize.value.toLong().coerceAtLeast(0L))
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            val resolvedPath = try {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                val split = docId.split(":")
                val type = split.getOrNull(0) ?: ""
                val relativePath = split.getOrNull(1)?.trim('/') ?: ""
                if (type.equals("primary", ignoreCase = true)) {
                    val base = android.os.Environment.getExternalStorageDirectory().absolutePath
                    if (relativePath.isNotEmpty()) "$base/$relativePath" else base
                } else if (type.isNotBlank()) {
                    if (relativePath.isNotEmpty()) "/storage/$type/$relativePath" else "/storage/$type"
                } else {
                    uri.path ?: uri.toString()
                }
            } catch (_: Exception) {
                uri.path ?: uri.toString()
            }
            viewModel.setCustomDownloadDir(resolvedPath)
            Toast.makeText(context, "Download directory updated", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConfiguration(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectBackup(uri)
        }
    }

    // Trigger the cache drain sequence
    fun triggerCacheClear() {
        showClearConfirmation = false
        isDrainAnimating = true
        startingDrainBytes = animatedByteSize.value.toLong()

        coroutineScope.launch {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}

            // Await actual IO file deletion
            viewModel.clearCacheSuspend()

            if (reduceMotion) {
                // Immediate update for reduced motion
                animatedByteSize.snapTo(0f)
                isDrainAnimating = false
            } else {
                // Snappy 220ms cubic-bezier roll-down & bar drain
                val drainEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

                launch {
                    animatedByteSize.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 220, easing = drainEasing)
                    )
                }

                delay(220L)

                // Track Release Pulse (200ms sweep shimmer)
                launch {
                    trackPulseAlpha.animateTo(
                        targetValue = 0.20f,
                        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                    )
                    trackPulseAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                    )
                }

                delay(200L)
                isDrainAnimating = false
            }

            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}

            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
        }
    }

    SettingsScaffold(
        title = "Storage & cache",
        onBack = onBack,
        modifier = modifier
    ) { hazeState ->

        // Hero Telemetry Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 20.dp)
                .drawBehind {
                    // 0.5dp white 14% hairline divider along bottom edge
                    drawLine(
                        color = SettingsTokens.HairlineDivider,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = SettingsTokens.HairlineWidth.toPx()
                    )
                }
                .padding(bottom = 20.dp)
        ) {
            // Eyebrow Label: "LOCAL DISK USAGE"
            Text(
                text = "LOCAL DISK USAGE",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = (11 * 0.08).sp,
                    color = SettingsTokens.TextWhite45
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Typographic Readout Row on Baseline
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isCalculating && cacheSizeBytes == 0L) "—" else "$currentNumeral $currentUnit",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-1.14).sp,
                        fontFeatureSettings = "tnum",
                        color = Color.White
                    )
                )

                Text(
                    text = if (isCalculating && cacheSizeBytes == 0L) "Calculating..." else "Cached",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = SettingsTokens.TextWhite54
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thin 2dp Horizontal Progress Bar
            val currentCache = animatedByteSize.value.toLong().coerceAtLeast(0L)
            val totalBytesSum = maxOf(currentCache + downloadsSizeBytes + configSizeBytes, 1024L * 1024L * 64L).toFloat()
            val cacheFraction = (currentCache.toFloat() / totalBytesSum).coerceIn(0f, 0.70f)
            val downloadsFraction = (downloadsSizeBytes.toFloat() / totalBytesSum).coerceIn(0f, 0.70f)
            val configFraction = (configSizeBytes.toFloat() / totalBytesSum).coerceIn(0.04f, 0.25f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (cacheFraction > 0.005f) {
                        // Cache Segment (Bright white at 90%)
                        Box(
                            modifier = Modifier
                                .weight(cacheFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.90f))
                        )

                        // 3dp transparent gap between segments
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    if (downloadsFraction > 0.005f) {
                        // Downloads Segment (Medium white at 60%)
                        Box(
                            modifier = Modifier
                                .weight(downloadsFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.60f))
                        )

                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    // Data & Config Segment (Muted white at 38%)
                    Box(
                        modifier = Modifier
                            .weight(configFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.38f))
                    )

                    // Remaining Free Space
                    val remainingFraction = (1f - cacheFraction - downloadsFraction - configFraction).coerceAtLeast(0f)
                    if (remainingFraction > 0.005f) {
                        Spacer(modifier = Modifier.weight(remainingFraction))
                    }
                }

                // Shimmer pulse highlight upon drain release
                if (trackPulseAlpha.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = trackPulseAlpha.value))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Row
            val (dlNumeral, dlUnit) = remember(downloadsSizeBytes) { formatBytesSplit(downloadsSizeBytes) }
            val (cfgNumeral, cfgUnit) = remember(configSizeBytes) { formatBytesSplit(configSizeBytes) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Cache
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.90f))
                    )
                    Text(
                        text = "Cache · $currentNumeral $currentUnit",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }

                // Item 2: Downloads
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.60f))
                    )
                    Text(
                        text = "Downloads · $dlNumeral $dlUnit",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }

                // Item 3: Data & Config
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    )
                    Text(
                        text = "Config · $cfgNumeral $cfgUnit",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }
            }
        }

        // Section 1: Cache Maintenance
        OpenSettingsSectionHeader(title = "Cache Maintenance")

        val isCacheEmpty = animatedByteSize.value <= 0f && !isDrainAnimating && !isCalculating
        OpenSettingsRow(
            title = "Clear image & HTTP cache",
            subtitle = "Frees thumbnail cache and memory buffers",
            trailingContent = {
                val pillText = when {
                    isDrainAnimating || isClearing || isCalculating -> ""
                    isCacheEmpty -> "Cleared"
                    else -> "Clear"
                }

                SettingsPillButton(
                    text = pillText,
                    onClick = {
                        if (!isCacheEmpty && !isDrainAnimating && !isCalculating) {
                            showClearConfirmation = true
                        }
                    },
                    isLoading = isDrainAnimating || isClearing || isCalculating,
                    enabled = !isCacheEmpty && !isDrainAnimating && !isCalculating,
                    hazeState = hazeState
                )
            }
        )

        // Section 2: Downloads & Offline Storage
        OpenSettingsSectionHeader(title = "Downloads & Offline Storage")

        OpenSettingsToggleRow(
            title = "Download over Wi-Fi only",
            subtitle = "Prevents downloading large media tracks over cellular data",
            checked = wifiOnlyDownloads,
            onCheckedChange = { viewModel.setWifiOnlyDownloads(it) },
            hazeState = hazeState
        )

        val isCustomDownloadDirSet = !customDownloadDir.isNullOrBlank()
        val displayDownloadDir = if (isCustomDownloadDirSet) customDownloadDir!! else viewModel.defaultDownloadDir

        OpenSettingsRow(
            title = "Download directory",
            subtitle = if (isCustomDownloadDirSet) "Custom: $displayDownloadDir" else "Default: $displayDownloadDir",
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCustomDownloadDirSet) {
                        SettingsPillButton(
                            text = "Reset",
                            onClick = {
                                viewModel.setCustomDownloadDir(null)
                                Toast.makeText(context, "Reset to default download location", Toast.LENGTH_SHORT).show()
                            },
                            hazeState = hazeState
                        )
                    }
                    SettingsPillButton(
                        text = "Change",
                        onClick = { folderPickerLauncher.launch(null) },
                        hazeState = hazeState
                    )
                }
            }
        )

        // Section 3: Configuration Backup
        OpenSettingsSectionHeader(title = "Configuration Backup")

        OpenSettingsRow(
            title = "Export configuration",
            subtitle = "Save preferences, user profiles, and repository configurations to JSON",
            enabled = !isExporting && !isImporting,
            onClick = {
                if (!isExporting && !isImporting) {
                    val timestamp = System.currentTimeMillis() / 1000
                    exportLauncher.launch("euthopiar_backup_$timestamp.json")
                }
            },
            trailingContent = {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 1.7.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SettingsTokens.TextWhite42,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        OpenSettingsRow(
            title = "Import configuration",
            subtitle = "Restore app layout, user profiles, and repository sources from backup file",
            enabled = !isImporting && !isExporting,
            onClick = {
                if (!isImporting && !isExporting) {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            },
            trailingContent = {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 1.7.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SettingsTokens.TextWhite42,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        // Shared Glass Confirmation Dialog for Cache Clear
        if (showClearConfirmation) {
            EuthopiarGlassDialog(
                title = "Clear image & HTTP cache?",
                message = "This will free $currentNumeral $currentUnit of cached thumbnails and memory buffers. Your downloads and settings stay intact.",
                confirmText = "Clear cache",
                dismissText = "Cancel",
                icon = Icons.Outlined.CleaningServices,
                onConfirm = { triggerCacheClear() },
                onDismiss = { showClearConfirmation = false }
            )
        }

        // Configuration Restore Confirmation Dialog with Glass Inspection Preview Card
        when (val state = backupUiState) {
            is BackupUiState.ReadyToRestore -> {
                EuthopiarGlassDialog(
                    title = "Restore configuration?",
                    message = "Restoring this backup will replace current settings, profiles, and repository configurations.",
                    confirmText = "Restore backup",
                    dismissText = "Cancel",
                    icon = Icons.Outlined.SettingsBackupRestore,
                    itemPreview = {
                        BackupInspectionPreviewCard(state.inspection.summary)
                    },
                    onConfirm = {
                        viewModel.confirmRestore(state.inspection)
                    },
                    onDismiss = {
                        viewModel.dismissBackupState()
                    }
                )
            }
            is BackupUiState.RestoreSuccess -> {
                EuthopiarGlassDialog(
                    title = "Backup restored",
                    message = "Configuration has been restored successfully. Restart Euthopiar now to apply all changes.",
                    confirmText = "Restart now",
                    dismissText = "Later",
                    icon = Icons.Outlined.CheckCircleOutline,
                    onConfirm = {
                        restartApp(context)
                    },
                    onDismiss = {
                        viewModel.dismissBackupState()
                    }
                )
            }
            is BackupUiState.RestoreError -> {
                EuthopiarGlassDialog(
                    title = "Restore failed",
                    message = state.message,
                    confirmText = "Dismiss",
                    dismissText = null,
                    icon = Icons.Outlined.ErrorOutline,
                    onConfirm = {
                        viewModel.dismissBackupState()
                    }
                )
            }
            is BackupUiState.ExportError -> {
                EuthopiarGlassDialog(
                    title = "Export failed",
                    message = state.message,
                    confirmText = "Dismiss",
                    dismissText = null,
                    icon = Icons.Outlined.ErrorOutline,
                    onConfirm = {
                        viewModel.dismissBackupState()
                    }
                )
            }
            else -> {}
        }
    }
}

/**
 * Editorial Frosted-Glass Card displaying metadata of a verified configuration snapshot before restore.
 */
@Composable
private fun BackupInspectionPreviewCard(summary: BackupSummary?) {
    if (summary == null) return

    val formattedDate = remember(summary.exportedAt) {
        if (summary.exportedAt > 0L) {
            val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
            sdf.format(Date(summary.exportedAt))
        } else {
            "Unknown date"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BACKUP SNAPSHOT",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.12.em,
                    color = Color.White.copy(alpha = 0.45f)
                )
            )
            Text(
                text = formattedDate,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.65f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BackupStatItem(label = "Settings", count = summary.preferencesCount)
            BackupStatItem(label = "Profiles", count = summary.profilesCount)
            BackupStatItem(label = "Sources", count = summary.repositoriesCount)
            BackupStatItem(label = "Addons", count = summary.addonsCount)
        }
    }
}

@Composable
private fun BackupStatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.50f)
            )
        )
    }
}

/**
 * Clean application restart trigger recreating the task cleanly.
 */
private fun restartApp(context: android.content.Context) {
    try {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val restartIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    } catch (e: Exception) {
        android.util.Log.e("StorageSettings", "Failed to restart app: ${e.message}", e)
    }
}
