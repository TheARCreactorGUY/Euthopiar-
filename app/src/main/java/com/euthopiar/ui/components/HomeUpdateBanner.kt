package com.euthopiar.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.update.AppUpdateManager
import com.euthopiar.data.update.ReleaseInfo
import com.euthopiar.data.update.UpdateCheckState
import com.euthopiar.ui.theme.*

/**
 * Obsidian frosted glass banner notifying the user that a new version of Euthopiar is available.
 * Designed identically to [OfflineBanner] to preserve visual cohesion.
 */
@Composable
fun HomeUpdateBanner(
    onOpenUpdatePopup: (ReleaseInfo, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateManager = remember(context) { AppUpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()
    val bannerDismissed by updateManager.bannerDismissedForSession.collectAsState()
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }
    val skippedVersion by preferencesRepository.skippedUpdateVersion.collectAsState(initial = "")

    val availableRelease = (updateState as? UpdateCheckState.UpdateAvailable)?.release
    val currentVersion = (updateState as? UpdateCheckState.UpdateAvailable)?.currentVersion.orEmpty()
    val isVisible = availableRelease != null && !bannerDismissed && availableRelease.versionName != skippedVersion

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(animationSpec = tween(Motion.DurationStandard)) + fadeIn(animationSpec = tween(Motion.DurationStandard)),
        exit = shrinkVertically(animationSpec = tween(Motion.DurationStandard)) + fadeOut(animationSpec = tween(Motion.DurationStandard)),
        modifier = modifier.fillMaxWidth()
    ) {
        if (availableRelease != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing4),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(ShapeFull)
                        .background(Color(0xFF14151F).copy(alpha = 0.92f))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), ShapeFull)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onOpenUpdatePopup(availableRelease, currentVersion)
                        }
                        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdateAlt,
                        contentDescription = "Update available",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Update v${availableRelease.versionName} available · Tap to view",
                        style = AppTypography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
