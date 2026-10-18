package com.euthopiar.ui.settings.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
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
    val simklToken by viewModel.simklAccessToken.collectAsState()
    val simklScrobble by viewModel.simklScrobbleEnabled.collectAsState()
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

    // Trakt Device Flow states
    var showTraktAuthDialog by remember { mutableStateOf(false) }
    var isStartingTraktAuth by remember { mutableStateOf(false) }
    var traktDeviceCodeResponse by remember { mutableStateOf<TraktDeviceCodeResponse?>(null) }
    var isPollingTrakt by remember { mutableStateOf(false) }
    var traktAuthError by remember { mutableStateOf<String?>(null) }
    var traktPollingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val startTraktDeviceAuth: () -> Unit = {
        if (!isStartingTraktAuth) {
            isStartingTraktAuth = true
            traktAuthError = null
            traktDeviceCodeResponse = null
            showTraktAuthDialog = true
            coroutineScope.launch {
                val result = com.euthopiar.core.sync.trakt.TraktClient.getInstance().generateDeviceCode()
                isStartingTraktAuth = false
                result.fold(
                    onSuccess = { response ->
                        traktDeviceCodeResponse = response
                        isPollingTrakt = true
                        traktPollingJob?.cancel()
                        traktPollingJob = coroutineScope.launch {
                            val intervalMs = (response.interval.coerceAtLeast(2) * 1000L)
                            val startTime = System.currentTimeMillis()
                            val timeoutMs = response.expiresIn * 1000L
                            while (isActive && (System.currentTimeMillis() - startTime) < timeoutMs) {
                                delay(intervalMs)
                                val pollRes = com.euthopiar.core.sync.trakt.TraktClient.getInstance().pollAccessToken(response.deviceCode)
                                if (pollRes.isSuccess) {
                                    val token = pollRes.getOrThrow()
                                    val profileRes = com.euthopiar.core.sync.trakt.TraktClient.getInstance().getUserProfile(token.accessToken)
                                    val username = profileRes.getOrNull()?.username?.ifBlank { "Trakt User" } ?: "Trakt User"
                                    viewModel.setTraktAuth(token.accessToken, token.refreshToken, username)
                                    isPollingTrakt = false
                                    showTraktAuthDialog = false
                                    break
                                } else {
                                    val exception = pollRes.exceptionOrNull()
                                    if (exception !is com.euthopiar.core.sync.trakt.TraktClient.PendingAuthException) {
                                        traktAuthError = exception?.message ?: "Authorization failed"
                                        isPollingTrakt = false
                                        break
                                    }
                                }
                            }
                            if (isPollingTrakt) {
                                isPollingTrakt = false
                                traktAuthError = "Authorization timed out. Please try again."
                            }
                        }
                    },
                    onFailure = { err ->
                        traktAuthError = err.message ?: "Failed to generate Trakt device code"
                    }
                )
            }
        }
    }

    val dismissTraktAuth: () -> Unit = {
        traktPollingJob?.cancel()
        traktPollingJob = null
        isPollingTrakt = false
        isStartingTraktAuth = false
        showTraktAuthDialog = false
    }

    // Simkl Connect states
    var showSimklConnectDialog by remember { mutableStateOf(false) }
    var simklTokenInput by remember { mutableStateOf("") }
    var isSimklValidating by remember { mutableStateOf(false) }
    var simklAuthError by remember { mutableStateOf<String?>(null) }

    val openSimklConnect: () -> Unit = {
        simklTokenInput = ""
        simklAuthError = null
        isSimklValidating = false
        showSimklConnectDialog = true
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

        // Conditional Child Rows when Debrid is enabled (220ms smooth expand/collapse)
        AnimatedVisibility(
            visible = debridEnabled,
            enter = if (reduceMotion) fadeIn(tween(120)) else expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = if (reduceMotion) fadeOut(tween(120)) else shrinkVertically(tween(220)) + fadeOut(tween(180))
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
                    subtitle = if (rdKey.isNotBlank()) "••••••••${rdKey.takeLast(4)}" else "Not configured",
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
                    subtitle = if (adKey.isNotBlank()) "••••••••${adKey.takeLast(4)}" else "Not configured",
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
                    subtitle = if (tbKey.isNotBlank()) "••••••••${tbKey.takeLast(4)}" else "Not configured",
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                )
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
                        isLoading = isStartingTraktAuth,
                        onClick = startTraktDeviceAuth
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
        }

        // Trakt Device Code Authorization Glass Dialog
        if (showTraktAuthDialog) {
            Dialog(
                onDismissRequest = dismissTraktAuth,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .imePadding()
                        .clip(RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusDialog),
                            hazeState = hazeState,
                            blurRadius = 22.dp
                        )
                        .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with icon and title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Authorize Trakt.tv",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = SettingsTokens.TextWhite96
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "OAuth Device Code Flow",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = SettingsTokens.TextWhite54
                                    )
                                )
                            }
                        }

                        if (isStartingTraktAuth) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Generating authorization code...",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = SettingsTokens.TextWhite72
                                    )
                                )
                            }
                        } else if (traktDeviceCodeResponse != null) {
                            val response = traktDeviceCodeResponse!!
                            Text(
                                text = "Visit the activation page on your browser and enter this code to connect:",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = SettingsTokens.TextWhite72
                                ),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 24sp Medium glass chip with userCode and Copy button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = response.userCode,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 24.sp,
                                            letterSpacing = 0.1.em,
                                            color = Color.White
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(response.userCode))
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentPaste,
                                            contentDescription = "Copy code",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Polling status indicator
                            if (isPollingTrakt) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 1.5.dp
                                    )
                                    Text(
                                        text = "Waiting for authorization on trakt.tv/activate...",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = SettingsTokens.TextWhite54
                                        )
                                    )
                                }
                            }

                            // Action button to open URL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                SettingsPillButton(
                                    text = "Open trakt.tv/activate",
                                    isSolidWhite = false,
                                    onClick = {
                                        try {
                                            uriHandler.openUri(response.verificationUrl)
                                        } catch (_: Exception) {}
                                    },
                                    hazeState = hazeState
                                )
                            }
                        }

                        if (traktAuthError != null) {
                            Text(
                                text = traktAuthError!!,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Dialog actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsPillButton(
                                text = "Cancel",
                                onClick = dismissTraktAuth,
                                hazeState = hazeState
                            )
                        }
                    }
                }
            }
        }

        // Simkl Connect Glass Dialog
        if (showSimklConnectDialog) {
            Dialog(
                onDismissRequest = { if (!isSimklValidating) showSimklConnectDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .imePadding()
                        .clip(RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusDialog),
                            hazeState = hazeState,
                            blurRadius = 22.dp
                        )
                        .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with icon and title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect Simkl",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = SettingsTokens.TextWhite96
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Personal API Access Token",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = SettingsTokens.TextWhite54
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Paste your Simkl client access token from your developer dashboard to link your profile and sync history.",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = SettingsTokens.TextWhite72
                            )
                        )

                        // Text Input Field with Paste button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (simklTokenInput.isEmpty()) {
                                        Text(
                                            text = "Paste Simkl token...",
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = SettingsTokens.TextWhite40
                                            )
                                        )
                                    }
                                    BasicTextField(
                                        value = simklTokenInput,
                                        onValueChange = {
                                            simklTokenInput = it
                                            simklAuthError = null
                                        },
                                        singleLine = true,
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

                                IconButton(
                                    onClick = {
                                        val text = clipboardManager.getText()?.text
                                        if (!text.isNullOrBlank()) {
                                            simklTokenInput = text.trim()
                                            simklAuthError = null
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
                            }
                        }

                        // Direct link to Simkl developer dashboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            SettingsPillButton(
                                text = "Get token from simkl.com",
                                isSolidWhite = false,
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://simkl.com/settings/developer/")
                                    } catch (_: Exception) {}
                                },
                                hazeState = hazeState
                            )
                        }

                        if (simklAuthError != null) {
                            Text(
                                text = simklAuthError!!,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }

                        // Dialog actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsPillButton(
                                text = "Cancel",
                                enabled = !isSimklValidating,
                                onClick = { showSimklConnectDialog = false },
                                hazeState = hazeState
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            SettingsPillButton(
                                text = "Connect",
                                isSolidWhite = true,
                                isLoading = isSimklValidating,
                                enabled = simklTokenInput.isNotBlank(),
                                onClick = {
                                    val token = simklTokenInput.trim()
                                    if (token.isNotBlank() && !isSimklValidating) {
                                        isSimklValidating = true
                                        simklAuthError = null
                                        coroutineScope.launch {
                                            val res = com.euthopiar.core.sync.simkl.SimklClient.getInstance().getUserProfile(token)
                                            isSimklValidating = false
                                            if (res.isSuccess) {
                                                viewModel.setSimklAuth(token)
                                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                showSimklConnectDialog = false
                                            } else {
                                                simklAuthError = res.exceptionOrNull()?.message ?: "Invalid Simkl token or connection failure."
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Confirmation Dialog for Trakt Disconnect
        if (showDisconnectTraktDialog) {
            EuthopiarGlassDialog(
                title = "Disconnect Trakt.tv?",
                message = "Your scrobbling and playback history syncing with @$traktUsername will be paused until you sign in again.",
                confirmText = "Disconnect",
                dismissText = "Cancel",
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
