package com.euthopiar.ui.settings.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.R
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.sync.supabase.SupabaseSyncManager
import com.euthopiar.data.sync.supabase.SyncStatus
import com.euthopiar.ui.components.AppSwitch
import com.euthopiar.ui.components.EuthopiarGlassDialog
import com.euthopiar.ui.profile.ProfileGlassTokens
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun GoogleCloudAccountCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current

    val prefs = remember { PreferencesRepository.getInstance(context) }
    val syncManager = remember { SupabaseSyncManager.getInstance(context) }

    val session by syncManager.sessionFlow.collectAsState()
    val syncStatus by syncManager.syncStatus.collectAsState()
    val lastSyncTs by syncManager.lastSyncTimestamp.collectAsState()

    val syncPlayback by prefs.supabaseSyncPlayback.collectAsState(initial = true)
    val syncWatchlist by prefs.supabaseSyncWatchlist.collectAsState(initial = true)
    val syncPlaylists by prefs.supabaseSyncPlaylists.collectAsState(initial = true)
    val syncSettings by prefs.supabaseSyncSettings.collectAsState(initial = true)

    var isConnecting by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    // Manual sync rotating icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseSync")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val syncRotation by animateFloatAsState(
        targetValue = if (syncStatus == SyncStatus.SYNCING) 360f else 0f,
        animationSpec = if (syncStatus == SyncStatus.SYNCING) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "syncRotation"
    )

    val surfaceBg = if (isAmoled) {
        Color(0xFF070709)
    } else {
        Color(0xFF13141A).copy(alpha = 0.75f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceBg)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        if (session == null) {
            // UNLINKED STATE: Connect Google Account Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tile: 46x46dp frosted glass tile with Google 'G' icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Middle: Title and description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Google Cloud Sync",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Sync continue watching, watchlist, playlists & settings across all devices",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = ProfileGlassTokens.TextMuted50
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Solid White Pill Button (tactile press scale 0.96f)
                val btnInteraction = remember { MutableInteractionSource() }
                val btnPressed by btnInteraction.collectIsPressedAsState()
                val btnScale by animateFloatAsState(
                    targetValue = if (btnPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                    label = "btnScale"
                )

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .graphicsLayer {
                            scaleX = btnScale
                            scaleY = btnScale
                        }
                        .clip(CircleShape)
                        .background(if (isConnecting) Color.White.copy(alpha = 0.70f) else Color.White)
                        .clickable(
                            interactionSource = btnInteraction,
                            indication = null,
                            enabled = !isConnecting,
                            role = Role.Button
                        ) {
                            if (activity == null) return@clickable
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isConnecting = true
                            scope.launch {
                                val res = syncManager.signInWithGoogle(activity)
                                isConnecting = false
                                if (res.isFailure) {
                                    val err = res.exceptionOrNull()?.message ?: "Sign in failed"
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF0B0B0F),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Connect",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0B0B0F)
                            )
                        )
                    }
                }
            }
        } else {
            // LINKED STATE: Connected Google Account Card + Granular Sync Controls
            val currentSession = session!!
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with subtle glow ring
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.20f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentSession.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentSession.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = currentSession.displayName ?: "User Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val initials = (currentSession.displayName ?: currentSession.email ?: "U")
                                .take(1).uppercase()
                            Text(
                                text = initials,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Account info & live sync status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentSession.displayName ?: currentSession.email?.substringBefore("@") ?: "Google Account",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Google small badge icon
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = currentSession.email ?: "Cloud sync active",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = ProfileGlassTokens.TextMuted50
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Pulse indicator + Live sync status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pulseColor = when (syncStatus) {
                                SyncStatus.SYNCING -> Color(0xFFFFB300) // Amber
                                SyncStatus.ERROR -> Color(0xFFFF5252)   // Red
                                else -> Color(0xFF00E676)              // Emerald Green
                            }

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .graphicsLayer {
                                        alpha = if (syncStatus == SyncStatus.SYNCING) pulseAlpha else 1f
                                    }
                                    .clip(CircleShape)
                                    .background(pulseColor)
                            )

                            val statusText = when (syncStatus) {
                                SyncStatus.SYNCING -> "Syncing library..."
                                SyncStatus.ERROR -> "Sync error — retry"
                                else -> formatRelativeTime(lastSyncTs)
                            }

                            Text(
                                text = statusText,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = if (syncStatus == SyncStatus.ERROR) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.65f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Actions: Manual Sync Button & Manage Accordion Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Manual Sync Icon Pill
                        val syncBtnInteraction = remember { MutableInteractionSource() }
                        val syncBtnPressed by syncBtnInteraction.collectIsPressedAsState()
                        val syncBtnScale by animateFloatAsState(
                            targetValue = if (syncBtnPressed) 0.92f else 1f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                            label = "syncBtnScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = syncBtnScale
                                    scaleY = syncBtnScale
                                }
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = syncBtnInteraction,
                                    indication = null,
                                    enabled = syncStatus != SyncStatus.SYNCING
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        syncManager.syncAll(forceFull = true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = "Sync now",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        rotationZ = syncRotation
                                    }
                            )
                        }

                        // Manage Accordion Pill
                        val manageInteraction = remember { MutableInteractionSource() }
                        val managePressed by manageInteraction.collectIsPressedAsState()
                        val manageScale by animateFloatAsState(
                            targetValue = if (managePressed) 0.94f else 1f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                            label = "manageScale"
                        )

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .graphicsLayer {
                                    scaleX = manageScale
                                    scaleY = manageScale
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isExpanded) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = 0.5.dp,
                                    color = if (isExpanded) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(
                                    interactionSource = manageInteraction,
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isExpanded = !isExpanded
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Close" else "Manage",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // EXPANDABLE GRANULAR SYNC PREFERENCES
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                    ) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "SYNC CATEGORIES",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = ProfileGlassTokens.TextMuted50
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Playback & Continue Watching Toggle
                        SyncPreferenceRow(
                            title = "Playback & Progress",
                            subtitle = "Sync continue watching position and watch history",
                            checked = syncPlayback,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setSupabaseSyncPlayback(checked)
                                    if (checked) syncManager.scheduleSync(300L)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Watchlist & Bookmarks Toggle
                        SyncPreferenceRow(
                            title = "Watchlist & Bookmarks",
                            subtitle = "Sync saved titles and custom status across devices",
                            checked = syncWatchlist,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setSupabaseSyncWatchlist(checked)
                                    if (checked) syncManager.scheduleSync(300L)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Custom Playlists Toggle
                        SyncPreferenceRow(
                            title = "Playlists & Collections",
                            subtitle = "Sync user-created custom playlists and collections",
                            checked = syncPlaylists,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setSupabaseSyncPlaylists(checked)
                                    if (checked) syncManager.scheduleSync(300L)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. App Preferences Toggle
                        SyncPreferenceRow(
                            title = "Settings & Preferences",
                            subtitle = "Sync subtitle styling, player gestures & theme settings",
                            checked = syncSettings,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setSupabaseSyncSettings(checked)
                                    if (checked) syncManager.scheduleSync(300L)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Disconnect Button Row
                        val disconnectInteraction = remember { MutableInteractionSource() }
                        val disconnectPressed by disconnectInteraction.collectIsPressedAsState()
                        val disconnectScale by animateFloatAsState(
                            targetValue = if (disconnectPressed) 0.97f else 1f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                            label = "disconnectScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = disconnectScale
                                    scaleY = disconnectScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = disconnectInteraction,
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDisconnectDialog = true
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Disconnect Google Account",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF5252)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Disconnecting / Managing Account
    if (showDisconnectDialog) {
        val email = session?.email ?: "this Google account"
        EuthopiarGlassDialog(
            title = "Google Cloud Account",
            message = "Signed in as $email.\n\nDisconnecting will stop cloud synchronization across devices. Your local watch history, watchlist, and playlists will remain safely on this device.",
            confirmText = "Disconnect",
            dismissText = "Keep Connected",
            icon = Icons.Outlined.CloudOff,
            onConfirm = {
                showDisconnectDialog = false
                scope.launch {
                    syncManager.signOut()
                    Toast.makeText(context, "Google account disconnected", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                showDisconnectDialog = false
            }
        )
    }
}

@Composable
private fun SyncPreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = ProfileGlassTokens.TextMuted50
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            useWhiteStyle = true
        )
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Not synced yet"
    val diffMs = System.currentTimeMillis() - timestampMs
    if (diffMs < 45_000L) return "Synced just now"
    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    if (mins < 60L) return "Synced ${mins}m ago"
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    if (hours < 24L) return "Synced ${hours}h ago"
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return "Synced ${days}d ago"
}
