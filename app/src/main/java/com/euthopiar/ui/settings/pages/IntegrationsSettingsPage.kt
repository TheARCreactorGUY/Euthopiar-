package com.euthopiar.ui.settings.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.euthopiar.ui.theme.EuthopiarMotionTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.core.sync.trakt.TraktDeviceCodeResponse
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * Redesigned "Integrations" Settings Screen (formerly "External Integrations").
 *
 * Strict High-Fidelity Design Specifications:
 * - Editorial Top Bar with 40x40dp glass back button & crossfading collapsed title.
 * - Bold 32sp Medium headline ("Integrations").
 * - Removed top "CLOUD & SYNC RADAR" summary box completely; services appear in dedicated sections below.
 * - Section 1: DEBRID CLOUD SERVICES with Custom Glass Toggle (48x28dp).
 *   When ON: smooth 220ms expand of Active Provider popover dropdown and masked API key rows.
 *   Glass inline key editor with Paste, Visibility toggle, and white Save Key pill.
 * - Section 2: TRAKT.TV TRACKING with solid white "Sign in" pill / frosted glass "Disconnect" pill.
 *   Disconnecting confirms via shared Glass Confirmation Dialog. Background scrobbling toggle when linked.
 * - Section 3: SIMKL SYNC with solid white "Connect" pill / frosted glass "Disconnect" pill.
 *   Disconnecting confirms via shared Glass Confirmation Dialog. Simkl scrobbling toggle when linked.
 * - 100% monochrome design language, 98% press scale on all interactive elements.
 */
@Composable
fun IntegrationsSettingsPage(
    onBack: () -> Unit,
    viewModel: IntegrationsSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val reduceMotion = LocalReduceMotion.current

    val traktUsername by viewModel.traktUsername.collectAsState()
    val traktScrobble by viewModel.traktScrobbleEnabled.collectAsState()
    val traktSyncWatchlist by viewModel.traktSyncWatchlist.collectAsState()
    val traktSyncPlayback by viewModel.traktSyncPlayback.collectAsState()
    val traktSyncHistory by viewModel.traktSyncHistory.collectAsState()
    val traktSyncLists by viewModel.traktSyncLists.collectAsState()

    val simklToken by viewModel.simklAccessToken.collectAsState()
    val simklScrobble by viewModel.simklScrobbleEnabled.collectAsState()
    val simklSyncWatchlist by viewModel.simklSyncWatchlist.collectAsState()
    val simklSyncHistory by viewModel.simklSyncHistory.collectAsState()

    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val debridEnabled by viewModel.debridEnabled.collectAsState()
    val activeDebridProvider by viewModel.activeDebridProvider.collectAsState()
    val rdKey by viewModel.realDebridApiKey.collectAsState()
    val adKey by viewModel.allDebridApiKey.collectAsState()
    val tbKey by viewModel.torBoxApiKey.collectAsState()

    var activeKeyEditor by remember { mutableStateOf<String?>(null) }
    var tempKeyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    var showDisconnectTraktDialog by remember { mutableStateOf(false) }
    var showDisconnectSimklDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Trakt Maintenance gate state (production Client ID / Secret pending)
    var showTraktMaintenanceDialog by remember { mutableStateOf(false) }

    val openTraktConnect: () -> Unit = {
        showTraktMaintenanceDialog = true
    }

    // Simkl Connect states
    var showSimklConnectDialog by remember { mutableStateOf(false) }
    var isSimklValidating by remember { mutableStateOf(false) }
    var simklAuthError by remember { mutableStateOf<String?>(null) }

    val openSimklConnect: () -> Unit = {
        simklAuthError = null
        isSimklValidating = false
        showSimklConnectDialog = true
    }

    LaunchedEffect(simklToken) {
        if (simklToken.isNotBlank()) {
            showSimklConnectDialog = false
        }
    }

    val debridOptions = remember {
        listOf(
            PopoverOption("realdebrid", "Real-Debrid", "High-speed multi-hoster & cloud cache"),
            PopoverOption("alldebrid", "AllDebrid", "European premium cloud multi-downloader"),
            PopoverOption("torbox", "TorBox", "Direct cloud seeding & Usenet caching")
        )
    }

    SettingsScaffold(
        title = "Integrations",
        onBack = onBack,
        modifier = modifier
    ) { hazeState ->

        // Section 1: Debrid Cloud Services
        OpenSettingsSectionHeader(title = "Debrid Cloud Services")

        OpenSettingsToggleRow(
            title = "Enable Debrid services",
            subtitle = "Stream high-bitrate torrents directly from cloud caches",
            checked = debridEnabled,
            onCheckedChange = { viewModel.setDebridEnabled(it) },
            hazeState = hazeState
        )

        // Conditional Child Rows when Debrid is enabled (smooth spring expand/collapse)
        AnimatedVisibility(
            visible = debridEnabled,
            enter = if (reduceMotion) fadeIn(snap()) else expandVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeIn(tween(180)),
            exit = if (reduceMotion) fadeOut(snap()) else shrinkVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeOut(tween(180))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Active Debrid Provider Selector
                OpenSettingsRow(
                    title = "Active Debrid provider",
                    subtitle = "Primary engine used to resolve cloud torrent streams",
                    trailingContent = {
                        FrostedGlassDropdown(
                            options = debridOptions,
                            selected = activeDebridProvider,
                            onSelected = { viewModel.setActiveDebridProvider(it) },
                            hazeState = hazeState,
                            popoverWidth = 270.dp
                        )
                    }
                )

                // Real-Debrid API Key Row
                OpenSettingsRow(
                    title = "Real-Debrid API key",
                    subtitle = if (rdKey.isNotBlank()) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022${rdKey.takeLast(4)}" else "Not configured",
                    trailingContent = {
                        SettingsPillButton(
                            text = if (activeKeyEditor == "Real-Debrid") "Close" else if (rdKey.isNotBlank()) "Edit" else "Set key",
                            onClick = {
                                if (activeKeyEditor == "Real-Debrid") {
                                    activeKeyEditor = null
                                } else {
                                    activeKeyEditor = "Real-Debrid"
                                    tempKeyInput = rdKey
                                    keyVisible = false
                                }
                            },
                            hazeState = hazeState
                        )
                    }
                )

                // AllDebrid API Key Row
                OpenSettingsRow(
                    title = "AllDebrid API key",
                    subtitle = if (adKey.isNotBlank()) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022${adKey.takeLast(4)}" else "Not configured",
                    trailingContent = {
                        SettingsPillButton(
                            text = if (activeKeyEditor == "AllDebrid") "Close" else if (adKey.isNotBlank()) "Edit" else "Set key",
                            onClick = {
                                if (activeKeyEditor == "AllDebrid") {
                                    activeKeyEditor = null
                                } else {
                                    activeKeyEditor = "AllDebrid"
                                    tempKeyInput = adKey
                                    keyVisible = false
                                }
                            },
                            hazeState = hazeState
                        )
                    }
                )

                // TorBox API Key Row
                OpenSettingsRow(
                    title = "TorBox API key",
                    subtitle = if (tbKey.isNotBlank()) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022${tbKey.takeLast(4)}" else "Not configured",
                    trailingContent = {
                        SettingsPillButton(
                            text = if (activeKeyEditor == "TorBox") "Close" else if (tbKey.isNotBlank()) "Edit" else "Set key",
                            onClick = {
                                if (activeKeyEditor == "TorBox") {
                                    activeKeyEditor = null
                                } else {
                                    activeKeyEditor = "TorBox"
                                    tempKeyInput = tbKey
                                    keyVisible = false
                                }
                            },
                            hazeState = hazeState
                        )
                    }
                )
            }
        }

        // Inline Key Editor Card
        activeKeyEditor?.let { provider ->
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsGlass(
                        shape = RoundedCornerShape(SettingsTokens.RadiusPopover),
                        hazeState = hazeState,
                        blurRadius = 18.dp
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Configure $provider token",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    color = SettingsTokens.TextWhite96
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paste Button
                            IconButton(
                                onClick = {
                                    val text = clipboardManager.getText()?.text
                                    if (!text.isNullOrBlank()) {
                                        tempKeyInput = text.trim()
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = SettingsTokens.TextWhite82,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Visibility Toggle
                            IconButton(
                                onClick = { keyVisible = !keyVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = SettingsTokens.TextWhite82,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Paste your secret token from your $provider account settings. It will be stored on-device in encrypted DataStore.",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )

                    // Text Input Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (tempKeyInput.isEmpty()) {
                            Text(
                                text = "Paste secret API key here...",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = SettingsTokens.TextWhite40
                                )
                            )
                        }
                        BasicTextField(
                            value = tempKeyInput,
                            onValueChange = { tempKeyInput = it },
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsPillButton(
                            text = "Cancel",
                            onClick = { activeKeyEditor = null },
                            hazeState = hazeState
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        SettingsPillButton(
                            text = "Save key",
                            isSolidWhite = true,
                            onClick = {
                                val clean = tempKeyInput.trim()
                                when (provider) {
                                    "Real-Debrid" -> viewModel.setRealDebridKey(clean)
                                    "AllDebrid" -> viewModel.setAllDebridKey(clean)
                                    "TorBox" -> viewModel.setTorBoxKey(clean)
                                }
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                activeKeyEditor = null
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Section 2: Simkl Watchlist & History (Primary Tracker)
        OpenSettingsSectionHeader(title = "Simkl Watchlist & History")

        val isSimklConnected = simklToken.isNotBlank()
        val simklSyncTelemetry = remember(isSimklConnected, isSyncing, syncStatusMessage, lastSyncTimestamp) {
            when {
                !isSimklConnected -> "Automatic episode scrobbling, watchlist sync, and watch history for anime, series, and movies."
                isSyncing -> syncStatusMessage ?: "Syncing watchlist and episode history with Simkl..."
                lastSyncTimestamp > 0L -> {
                    val diff = System.currentTimeMillis() - lastSyncTimestamp
                    val mins = (diff / 60000L).coerceAtLeast(0L)
                    val timeLabel = when {
                        mins < 1L -> "just now"
                        mins < 60L -> "$mins min${if (mins == 1L) "" else "s"} ago"
                        else -> {
                            val hours = mins / 60L
                            "$hours hr${if (hours == 1L) "" else "s"} ago"
                        }
                    }
                    "Last synced $timeLabel • Automatic scrobbling and two-way library sync active."
                }
                else -> "Account linked • Ready to sync your watchlist and playback history."
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SimklSyncSectionCard(
            isConnected = isSimklConnected,
            isValidating = isSimklValidating,
            isSyncing = isSyncing,
            telemetryText = simklSyncTelemetry,
            hazeState = hazeState,
            onConnectClick = openSimklConnect,
            onSyncNowClick = {
                viewModel.triggerSyncNow()
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
            },
            onDisconnectClick = { showDisconnectSimklDialog = true }
        )

        AnimatedVisibility(
            visible = isSimklConnected,
            enter = if (reduceMotion) fadeIn(snap()) else expandVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeIn(tween(180)),
            exit = if (reduceMotion) fadeOut(snap()) else shrinkVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeOut(tween(180))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OpenSettingsToggleRow(
                    title = "Live scrobbling",
                    subtitle = "Mark episodes and movies watched automatically at 80% playback",
                    checked = simklScrobble,
                    onCheckedChange = { viewModel.setSimklScrobble(it) },
                    hazeState = hazeState
                )

                OpenSettingsToggleRow(
                    title = "Plan to Watch",
                    subtitle = "Two-way sync between your Euthopiar watchlist and Simkl",
                    checked = simklSyncWatchlist,
                    onCheckedChange = { viewModel.setSimklSyncWatchlist(it) },
                    hazeState = hazeState
                )

                OpenSettingsToggleRow(
                    title = "Watch history",
                    subtitle = "Import and export completed series, anime, and movie records",
                    checked = simklSyncHistory,
                    onCheckedChange = { viewModel.setSimklSyncHistory(it) },
                    hazeState = hazeState
                )
            }
        }

        // Section 3: Trakt.tv Tracking (Currently Under Maintenance)
        OpenSettingsSectionHeader(title = "Trakt.tv Tracking")

        OpenSettingsRow(
            title = "Trakt.tv account",
            subtitle = "Temporarily unavailable while under maintenance",
            onClick = openTraktConnect,
            trailingContent = {
                SettingsPillButton(
                    text = "Maintenance",
                    onClick = openTraktConnect,
                    hazeState = hazeState
                )
            }
        )

        // Trakt.tv Under Maintenance Glass Dialog
        if (showTraktMaintenanceDialog) {
            EuthopiarGlassDialog(
                title = "Currently Under Maintenance",
                message = "Trakt.tv sign-in and synchronization are temporarily offline while production API credentials are being finalized. Use Simkl above to sync your watchlist and playback history in the meantime.",
                confirmText = "Got it",
                dismissText = null,
                icon = Icons.Outlined.Build,
                hazeState = hazeState,
                onConfirm = { showTraktMaintenanceDialog = false },
                onDismiss = { showTraktMaintenanceDialog = false }
            )
        }

        // Simkl Connect Glass Dialog
        if (showSimklConnectDialog) {
            SimklConnectDialog(
                hazeState = hazeState,
                isSimklValidating = isSimklValidating,
                simklAuthError = simklAuthError,
                onDismiss = { showSimklConnectDialog = false },
                onClearError = { simklAuthError = null },
                onConnect = { codeOrToken ->
                    if (!isSimklValidating) {
                        isSimklValidating = true
                        simklAuthError = null
                        coroutineScope.launch {
                            val simklClient = com.euthopiar.core.sync.simkl.SimklClient.getInstance()
                            val prefs = com.euthopiar.data.preferences.PreferencesRepository.getInstance(context)
                            val syncVerifier = context.getSharedPreferences("simkl_auth_prefs", android.content.Context.MODE_PRIVATE)
                                .getString("pending_code_verifier", null)
                            val storedVerifier = prefs.simklCodeVerifier.firstOrNull()?.ifBlank { null }
                            val verifier = syncVerifier ?: storedVerifier ?: simklClient.pendingCodeVerifier

                            val extractedCode = Regex("""code=([^&]+)""").find(codeOrToken)?.groupValues?.get(1) ?: codeOrToken
                            val exchangeRes = simklClient.exchangeCodeForToken(extractedCode, codeVerifier = verifier)
                            val resolvedToken = if (exchangeRes.isSuccess && exchangeRes.getOrThrow().accessToken.isNotBlank()) {
                                exchangeRes.getOrThrow().accessToken
                            } else {
                                extractedCode
                            }

                            val profileRes = simklClient.getUserProfile(resolvedToken)
                            isSimklValidating = false
                            if (profileRes.isSuccess) {
                                viewModel.setSimklAuth(resolvedToken)
                                prefs.clearSimklCodeVerifier()
                                context.getSharedPreferences("simkl_auth_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                showSimklConnectDialog = false
                            } else {
                                simklAuthError = exchangeRes.exceptionOrNull()?.message
                                    ?: profileRes.exceptionOrNull()?.message
                                    ?: "Invalid Simkl authorization code or token."
                            }
                        }
                    }
                }
            )
        }

        // Confirmation Dialog for Trakt Disconnect
        if (showDisconnectTraktDialog) {
            EuthopiarGlassDialog(
                title = "Disconnect Trakt.tv?",
                message = "Your scrobbling and playback history syncing with @$traktUsername will be paused until you sign in again.",
                confirmText = "Disconnect",
                dismissText = "Cancel",
                icon = Icons.Outlined.LinkOff,
                isDestructive = true,
                hazeState = hazeState,
                onConfirm = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                    viewModel.disconnectTrakt()
                    showDisconnectTraktDialog = false
                },
                onDismiss = { showDisconnectTraktDialog = false }
            )
        }

        // Confirmation Dialog for Simkl Disconnect
        if (showDisconnectSimklDialog) {
            EuthopiarGlassDialog(
                title = "Disconnect Simkl?",
                message = "Your automated watchlist and episode progress sync with Simkl will be unlinked.",
                confirmText = "Disconnect",
                dismissText = "Cancel",
                icon = Icons.Outlined.LinkOff,
                isDestructive = true,
                hazeState = hazeState,
                onConfirm = {
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                    viewModel.disconnectSimkl()
                    showDisconnectSimklDialog = false
                },
                onDismiss = { showDisconnectSimklDialog = false }
            )
        }
    }
}

/**
 * Architectural Frosted-Glass Surface for the Simkl Integration Section.
 *
 * Crafted following Apple Design, Impeccable, Emil Kowalski, Taste-Skill, and Unslop principles:
 * - Single non-nested glass surface (20dp radius) with directional ice-blue/white 0.5dp specular border.
 * - Features the Simkl wordmark (`SIMKL™`) in a crisp white-to-electric-blue gradient.
 * - Secondary copy is tinted from the surface's sky-blue hue (#D8F3FF @ 72%) for >5:1 contrast without dead gray.
 * - Tabular numerals (`tnum`) on sync timestamps and physics-based press feedback (0.97f scale).
 */
@Composable
private fun SimklSyncSectionCard(
    isConnected: Boolean,
    isValidating: Boolean,
    isSyncing: Boolean,
    telemetryText: String,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onConnectClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAmoled = LocalAmoledMode.current
    val haptic = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(SettingsTokens.RadiusPreviewCard)

    val baseFill = if (isAmoled) {
        Color(0xFF03070D).copy(alpha = 0.94f)
    } else {
        Color(0xFF0A121E).copy(alpha = 0.78f)
    }

    val specularBorderBrush = remember(isConnected) {
        Brush.linearGradient(
            0.00f to Color(0xFF7DD3FC).copy(alpha = if (isConnected) 0.44f else 0.32f),
            0.48f to Color.White.copy(alpha = 0.15f),
            1.00f to Color(0xFF38BDF8).copy(alpha = if (isConnected) 0.24f else 0.12f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .settingsGlass(
                shape = cardShape,
                hazeState = hazeState,
                blurRadius = 22.dp,
                borderWidth = 0.dp,
                customFill = baseFill
            )
            .drawBehind {
                // Subtle top-left atmospheric sky-blue refraction behind the SIMKL™ wordmark
                val glowRadius = size.width * 0.72f
                drawRect(
                    brush = Brush.radialGradient(
                        0.00f to Color(0xFF38BDF8).copy(alpha = if (isConnected) 0.16f else 0.11f),
                        0.55f to Color(0xFF0284C7).copy(alpha = 0.04f),
                        1.00f to Color.Transparent,
                        center = Offset(size.width * 0.18f, size.height * 0.15f),
                        radius = glowRadius.coerceAtLeast(1f)
                    )
                )
            }
            .border(
                width = SettingsTokens.HairlineWidth,
                brush = specularBorderBrush,
                shape = cardShape
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Simkl Blue-White Gradient Wordmark + Live Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimklGradientWordmark(height = 21.dp)

                // Status Pill
                val statusDotColor = when {
                    isSyncing -> Color(0xFF7DD3FC)
                    isConnected -> Color(0xFF38BDF8)
                    else -> Color.White.copy(alpha = 0.34f)
                }
                val statusLabel = when {
                    isSyncing -> "Syncing"
                    isConnected -> "Linked"
                    else -> "Not linked"
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isConnected) Color(0xFF38BDF8).copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            SettingsTokens.HairlineWidth,
                            if (isConnected) Color(0xFF7DD3FC).copy(alpha = 0.32f)
                            else SettingsTokens.HairlineBorder,
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                color = if (isConnected) Color(0xFFE0F2FE) else Color(0xFFD8F3FF).copy(alpha = 0.68f)
                            )
                        )
                    }
                }
            }

            // Telemetry / Description Copy (Hue-tinted secondary text + tabular numerals)
            Text(
                text = telemetryText,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp,
                    fontFeatureSettings = "tnum",
                    color = Color(0xFFD8F3FF).copy(alpha = 0.74f)
                )
            )

            // Hairline Divider inside surface before action bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SettingsTokens.HairlineWidth)
                    .background(Color(0xFFBAE6FD).copy(alpha = 0.12f))
            )

            // Action Bar: Connect CTA when unlinked, or Sync Now + Disconnect when linked
            val ctaGradientBrush = remember {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFDDF4FF),
                        Color(0xFF7DD3FC)
                    )
                )
            }

            if (!isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .settingsPressable(
                            enabled = !isValidating,
                            scaleDown = 0.97f,
                            onClick = {
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                onConnectClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(ctaGradientBrush, CircleShape)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                color = Color(0xFF060E17),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Connect Simkl",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF060E17)
                                )
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary: Sync Now Pill (Blue-White Gradient)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .settingsPressable(
                                enabled = !isSyncing,
                                scaleDown = 0.97f,
                                onClick = onSyncNowClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(ctaGradientBrush, CircleShape)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        color = Color(0xFF060E17),
                                        strokeWidth = 1.8.dp
                                    )
                                    Text(
                                        text = "Syncing...",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF060E17)
                                        )
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF060E17),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Sync now",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF060E17)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Secondary: Disconnect Frosted Glass Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .settingsPressable(
                                scaleDown = 0.97f,
                                onClick = {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    onDisconnectClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                .border(
                                    SettingsTokens.HairlineWidth,
                                    SettingsTokens.HairlineBorder,
                                    CircleShape
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Disconnect",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = SettingsTokens.TextWhite88
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
