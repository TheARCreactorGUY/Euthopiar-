package com.euthopiar.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

/**
 * Offline status badge overlaid on posters:
 * Restyled as a small glass chip at the poster corner with outline icons and no off-palette accents.
 */
@Composable
fun OfflinePosterBadge(
    downloadItem: DownloadItem?,
    modifier: Modifier = Modifier
) {
    if (downloadItem == null) return

    val reduceMotion = LocalReduceMotion.current
    val progressTarget = (downloadItem.progressPercentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = if (reduceMotion) tween(0) else tween(300),
        label = "offlineBadgeProgress"
    )

    val a11yDesc = when (downloadItem.state) {
        DownloadState.QUEUED -> "Download queued"
        DownloadState.DOWNLOADING -> "Downloading, ${downloadItem.progressPercentage.toInt()} percent"
        DownloadState.PAUSED -> "Download paused, ${downloadItem.progressPercentage.toInt()} percent"
        DownloadState.COMPLETED -> "Downloaded and ready offline"
        DownloadState.FAILED -> "Download failed"
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0E0F17).copy(alpha = 0.85f))
            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
            .semantics { contentDescription = a11yDesc },
        contentAlignment = Alignment.Center
    ) {
        when (downloadItem.state) {
            DownloadState.QUEUED -> {
                // Faint hollow ring
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                )
            }
            DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.14f),
                    strokeWidth = 1.5.dp,
                    strokeCap = StrokeCap.Round
                )
                // Inner white indicator dot
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            DownloadState.PAUSED -> {
                Icon(
                    imageVector = Icons.Outlined.Pause,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(13.dp)
                )
            }
            DownloadState.COMPLETED -> {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            DownloadState.FAILED -> {
                Text(
                    text = "!",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
