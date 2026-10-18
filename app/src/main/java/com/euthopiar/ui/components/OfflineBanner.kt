package com.euthopiar.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*
import com.euthopiar.ui.util.ConnectivityObserver
import kotlinx.coroutines.delay

/**
 * Calm, consumer-grade Offline Awareness banner.
 * Shows an obsidian glass warning pill when offline, and a brief "Back online" confirmation when reconnected.
 */
@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    isNetworkConnected: Boolean? = null
) {
    val context = LocalContext.current
    val observedConnected by remember(context) {
        ConnectivityObserver.getInstance(context).isConnected
    }.collectAsState(initial = true)

    val isConnected = isNetworkConnected ?: observedConnected
    var wasOffline by remember { mutableStateOf(false) }
    var showBackOnline by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            wasOffline = true
            showBackOnline = false
        } else if (wasOffline) {
            showBackOnline = true
            delay(3000L)
            showBackOnline = false
            wasOffline = false
        }
    }

    AnimatedVisibility(
        visible = !isConnected || showBackOnline,
        enter = expandVertically(animationSpec = tween(Motion.DurationStandard)) + fadeIn(animationSpec = tween(Motion.DurationStandard)),
        exit = shrinkVertically(animationSpec = tween(Motion.DurationStandard)) + fadeOut(animationSpec = tween(Motion.DurationStandard)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing4),
            contentAlignment = Alignment.Center
        ) {
            if (!isConnected) {
                // Offline Warning Glass Pill
                Row(
                    modifier = Modifier
                        .clip(ShapeFull)
                        .background(Color(0xFF14151F).copy(alpha = 0.92f))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(HairlineWidth, Color.White.copy(alpha = 0.16f), ShapeFull)
                        .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = "Offline",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(Dimens.iconS)
                    )
                    Text(
                        text = "You're offline · Showing cached & downloaded content",
                        style = AppTypography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else if (showBackOnline) {
                // Reconnected Glass Pill
                Row(
                    modifier = Modifier
                        .clip(ShapeFull)
                        .background(Color(0xFF14151F).copy(alpha = 0.92f))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(HairlineWidth, Color.White.copy(alpha = 0.24f), ShapeFull)
                        .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = "Online",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(Dimens.iconS)
                    )
                    Text(
                        text = "Back online",
                        style = AppTypography.labelMedium,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
            }
        }
    }
}
