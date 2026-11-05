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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LinkOff
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

    // Trakt PKCE Connect states
    var showTraktConnectDialog by remember { mutableStateOf(false) }
    var isTraktValidating by remember { mutableStateOf(false) }
    var traktAuthError by remember { mutableStateOf<String?>(null) }

    val openTraktConnect: () -> Unit = {
        traktAuthError = null
        isTraktValidating = false
        showTraktConnectDialog = true
    }

    LaunchedEffect(traktUsername) {
        if (traktUsername.isNotBlank()) {
            showTraktConnectDialog = false
        }
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

        // Section 2: Trakt.tv Tracking
        OpenSettingsSectionHeader(title = "Trakt.tv Tracking")

        val isTraktConnected = traktUsername.isNotBlank()
        OpenSettingsRow(
            title = "Account status",
            subtitle = if (isTraktConnected) "Connected as @$traktUsername" else "Not linked to any account",
            trailingContent = {
                if (isTraktConnected) {
                    SettingsPillButton(
                        text = "Disconnect",
                        onClick = { showDisconnectTraktDialog = true },
                        hazeState = hazeState
                    )
                } else {
                    SettingsPillButton(
                        text = "Sign in",
                        isSolidWhite = true,
                        isLoading = isTraktValidating,
                        onClick = openTraktConnect
                    )
                }
            }
        )

        if (isTraktConnected) {
            OpenSettingsToggleRow(
                title = "Background scrobbling",
                subtitle = "Send playback progress & mark watched automatically",
                checked = traktScrobble,
                onCheckedChange = { viewModel.setTraktScrobble(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Watchlist two-way sync",
                subtitle = "Keep bookmarks synchronized between Euthopiar and Trakt",
                checked = traktSyncWatchlist,
                onCheckedChange = { viewModel.setTraktSyncWatchlist(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Resume points sync",
                subtitle = "Synchronize Continue Watching progress with Trakt playback",
                checked = traktSyncPlayback,
                onCheckedChange = { viewModel.setTraktSyncPlayback(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Watch history sync",
                subtitle = "Sync completed watch history and badges with your profile",
                checked = traktSyncHistory,
                onCheckedChange = { viewModel.setTraktSyncHistory(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Custom lists & playlists",
                subtitle = "Sync personal Trakt lists as in-app playlists",
                checked = traktSyncLists,
                onCheckedChange = { viewModel.setTraktSyncLists(it) },
                hazeState = hazeState
            )
        }

        // Section 3: Simkl Sync
        OpenSettingsSectionHeader(title = "Simkl Sync")

        val isSimklConnected = simklToken.isNotBlank()
        OpenSettingsRow(
            title = "Simkl account",
            subtitle = if (isSimklConnected) "Authorized & linked" else "Not linked",
            trailingContent = {
                if (isSimklConnected) {
                    SettingsPillButton(
                        text = "Disconnect",
                        onClick = { showDisconnectSimklDialog = true },
                        hazeState = hazeState
                    )
                } else {
                    SettingsPillButton(
                        text = "Connect",
                        isSolidWhite = true,
                        isLoading = isSimklValidating,
                        onClick = openSimklConnect
                    )
                }
            }
        )

        if (isSimklConnected) {
            OpenSettingsToggleRow(
                title = "Simkl scrobbling",
                subtitle = "Update watchlist and history on playback events",
                checked = simklScrobble,
                onCheckedChange = { viewModel.setSimklScrobble(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Simkl Plan to Watch sync",
                subtitle = "Keep Plan to Watch list in sync with your local watchlist",
                checked = simklSyncWatchlist,
                onCheckedChange = { viewModel.setSimklSyncWatchlist(it) },
                hazeState = hazeState
            )

            OpenSettingsToggleRow(
                title = "Simkl history sync",
                subtitle = "Sync completed watching records with Simkl profile",
                checked = simklSyncHistory,
                onCheckedChange = { viewModel.setSimklSyncHistory(it) },
                hazeState = hazeState
            )
        }

        // Section 4: Cloud Media Synchronization
        if (isTraktConnected || isSimklConnected) {
            OpenSettingsSectionHeader(title = "Cloud Media Synchronization")

            val syncSubtitle = when {
                isSyncing -> syncStatusMessage ?: "Synchronizing media library with cloud accounts..."
                lastSyncTimestamp > 0L -> {
                    val diff = System.currentTimeMillis() - lastSyncTimestamp
                    val mins = (diff / 60000L).coerceAtLeast(0L)
                    when {
                        mins < 1L -> "Last synchronized just now"
                        mins < 60L -> "Last synchronized $mins min${if (mins == 1L) "" else "s"} ago"
                        else -> {
                            val hours = mins / 60L
                            "Last synchronized $hours hr${if (hours == 1L) "" else "s"} ago"
                        }
                    }
                }
                else -> "Never synchronized"
            }

            OpenSettingsRow(
                title = "Synchronize now",
                subtitle = syncSubtitle,
                trailingContent = {
                    SettingsPillButton(
                        text = if (isSyncing) "Syncing..." else "Sync now",
                        isSolidWhite = true,
                        isLoading = isSyncing,
                        onClick = {
                            viewModel.triggerSyncNow()
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                        },
                        hazeState = hazeState
                    )
                }
            )
        }

        // Trakt PKCE Connect Glass Dialog
        if (showTraktConnectDialog) {
            TraktConnectDialog(
                hazeState = hazeState,
                isTraktValidating = isTraktValidating,
                traktAuthError = traktAuthError,
                onDismiss = { showTraktConnectDialog = false },
                onClearError = { traktAuthError = null },
                onConnect = { code ->
                    if (!isTraktValidating) {
                        isTraktValidating = true
                        traktAuthError = null
                        coroutineScope.launch {
                            val syncVerifier = context.getSharedPreferences("trakt_auth_prefs", android.content.Context.MODE_PRIVATE)
                                .getString("pending_code_verifier", null)
                            val prefs = com.euthopiar.data.preferences.PreferencesRepository.getInstance(context)
                            val storedVerifier = prefs.traktCodeVerifier.firstOrNull()?.ifBlank { null }
                            val verifier = syncVerifier ?: storedVerifier ?: com.euthopiar.core.sync.trakt.TraktClient.getInstance().pendingCodeVerifier
                            val res = com.euthopiar.core.sync.trakt.TraktClient.getInstance().exchangeCodeForToken(code, codeVerifier = verifier)
                            isTraktValidating = false
                            if (res.isSuccess) {
                                val tokenResp = res.getOrThrow()
                                val profile = com.euthopiar.core.sync.trakt.TraktClient.getInstance().getUserProfile(tokenResp.accessToken).getOrNull()
                                val username = profile?.username?.ifBlank { "Trakt User" } ?: "Trakt User"
                                viewModel.setTraktAuth(tokenResp.accessToken, tokenResp.refreshToken, username)
                                prefs.clearTraktCodeVerifier()
                                context.getSharedPreferences("trakt_auth_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                showTraktConnectDialog = false
                            } else {
                                traktAuthError = res.exceptionOrNull()?.message ?: "Invalid authorization code or connection failure."
                            }
                        }
                    }
                }
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
