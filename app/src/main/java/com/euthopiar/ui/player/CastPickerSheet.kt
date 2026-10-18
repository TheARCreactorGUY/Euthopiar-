package com.euthopiar.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.R
import com.euthopiar.core.model.StreamSource
import com.euthopiar.ui.components.AppBottomSheet
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.theme.HairlineWidth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastPickerSheet(
    streamSource: StreamSource?,
    mediaTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbarController = LocalSnackbarController.current
    val coroutineScope = rememberCoroutineScope()
    val isScanning by CastController.isScanning.collectAsState()
    val discoveredDevices by CastController.discoveredDevices.collectAsState()
    val activeDevice by CastController.activeCastDevice.collectAsState()

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.cast_title),
                        style = PlayerTokens.SheetHeadline.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.cast_subtitle),
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(PlayerTokens.TouchTargetMin)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = "Scan for devices",
                            onClick = {
                                coroutineScope.launch {
                                    CastController.scanDevices(context)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        BufferingSpinnerArc()
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Scan for devices",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Cast Banner if connected (no red or green, Video-Overlay Glass)
            if (activeDevice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .videoOverlayGlass(PlayerTokens.RadiusMedium)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CastConnected,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Casting to ${activeDevice?.name}",
                                color = Color.White,
                                style = PlayerTokens.RowTitle.copy(fontSize = 14.sp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = "Disconnect",
                                    onClick = { CastController.disconnect() }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Disconnect",
                                color = Color.White.copy(alpha = 0.85f),
                                style = PlayerTokens.PillText
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.cast_external_player_header).uppercase(),
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // External player app list
            ExternalPlaybackHelper.POPULAR_PLAYERS.forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusMedium)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = player.name,
                            onClick = {
                                if (streamSource != null) {
                                    val launched = ExternalPlaybackHelper.launchExternalPlayer(
                                        context = context,
                                        streamSource = streamSource,
                                        title = mediaTitle,
                                        playerOption = player,
                                        onError = { msg -> snackbarController.show(msg) }
                                    )
                                    if (launched) onDismiss()
                                } else {
                                    snackbarController.show("Video source not loaded yet")
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (player.id) {
                                "vlc" -> Icons.Outlined.PlayCircle
                                "mx" -> Icons.Outlined.SmartDisplay
                                "system" -> Icons.Outlined.Share
                                else -> Icons.Outlined.PlayArrow
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name,
                            style = PlayerTokens.RowTitle.copy(fontSize = 14.sp),
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Smart TV Devices
            Text(
                text = stringResource(R.string.cast_network_header).uppercase(),
                style = PlayerTokens.SectionLabel,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (discoveredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusMedium)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isScanning) stringResource(R.string.cast_searching_devices) else stringResource(R.string.cast_no_devices_found),
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
            } else {
                discoveredDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusMedium)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = device.name,
                                onClick = {
                                    CastController.selectDevice(device)
                                    snackbarController.show("Selected ${device.name}")
                                    onDismiss()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = device.name,
                                style = PlayerTokens.RowTitle.copy(fontSize = 14.sp),
                                color = Color.White
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.Cast,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Copy Stream Link Button
        val copyLinkCopiedMsg = stringResource(R.string.cast_stream_link_copied)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                .playerPressable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.cast_copy_stream_link),
                    onClick = {
                        if (streamSource != null) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Stream URL", streamSource.url)
                            clipboard.setPrimaryClip(clip)
                            snackbarController.show(copyLinkCopiedMsg)
                            onDismiss()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.cast_copy_stream_link),
                    style = PlayerTokens.RowTitle.copy(fontSize = 14.sp),
                    color = Color.White
                )
            }
        }
    }
}
}
