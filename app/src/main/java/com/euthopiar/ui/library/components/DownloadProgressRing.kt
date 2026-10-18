package com.euthopiar.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.download.DownloadItem
import com.euthopiar.download.DownloadState
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable

/**
 * 30dp progress ring with 2dp stroke, 10% white track, crimson arc, round caps.
 * Tap ring to pause/resume/retry.
 */
@Composable
fun DownloadProgressRing(
    item: DownloadItem,
    onTogglePauseResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val progressTarget = (item.progressPercentage / 100f).coerceIn(0.02f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = if (reduceMotion) tween(0) else tween(300),
        label = "ringProgress"
    )

    val a11yDesc = when (item.state) {
        DownloadState.PAUSED -> "Paused at ${item.progressPercentage.toInt()}%. Tap to resume."
        DownloadState.FAILED -> "Failed. Tap to retry."
        else -> "Downloading at ${item.progressPercentage.toInt()}%. Tap to pause."
    }

    Box(
        modifier = modifier
            .size(LibraryDimens.progressRingSize)
            .clip(CircleShape)
            .pressable(
                scaleDown = 0.98f,
                role = Role.Button,
                onClickLabel = a11yDesc,
                onClick = onTogglePauseResume
            )
            .semantics { contentDescription = a11yDesc },
        contentAlignment = Alignment.Center
    ) {
        when (item.state) {
            DownloadState.FAILED -> {
                // Crimson '!' in a ring
                Box(
                    modifier = Modifier
                        .size(LibraryDimens.progressRingSize)
                        .clip(CircleShape)
                        .background(LibraryColors.Crimson.copy(alpha = 0.15f))
                        .border(LibraryDimens.progressRingStroke, LibraryColors.Crimson, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = LibraryColors.Crimson,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
            DownloadState.PAUSED -> {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(LibraryDimens.progressRingSize),
                    color = LibraryColors.TextMutedReadable,
                    trackColor = LibraryColors.Hairline,
                    strokeWidth = LibraryDimens.progressRingStroke,
                    strokeCap = StrokeCap.Round
                )
                // Resume icon (Play arrow)
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = LibraryColors.TextWhite,
                    modifier = Modifier.size(15.dp)
                )
            }
            else -> {
                // DOWNLOADING / QUEUED
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(LibraryDimens.progressRingSize),
                    color = LibraryColors.Crimson,
                    trackColor = LibraryColors.Hairline,
                    strokeWidth = LibraryDimens.progressRingStroke,
                    strokeCap = StrokeCap.Round
                )
                // Explicit Pause icon inside the ring
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = LibraryColors.Crimson,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
