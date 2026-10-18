package com.euthopiar.ui.library.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.download.DownloadItem
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable

/**
 * Editorial Continue Watching card:
 * - Wide card with rounded corners (14 px), backdrop image filling it, dark overlay for readability
 * - Top-left: small glass chip "Continue · 55 min left" (11 sp, 10 px radius)
 * - Bottom-left: title (26 sp, medium, tight line height) with episode info (e.g. "S1:E1") under it in muted text
 * - Bottom-right: solid white circle (40 px) with dark play icon
 * - Along bottom edge: thin 2 px progress bar showing watched amount
 */
@Composable
fun ContinueWatchingCard(
    historyItem: PlaybackHistoryEntity,
    downloadItem: DownloadItem?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = onPlay
) {
    val reduceMotion = LocalReduceMotion.current
    val isAnime = historyItem.mediaType?.contains("anime", ignoreCase = true) == true
    val isTv = historyItem.seasonNumber != null || historyItem.episodeNumber != null ||
            historyItem.mediaType?.contains("tv", ignoreCase = true) == true ||
            historyItem.mediaType?.contains("series", ignoreCase = true) == true ||
            isAnime
    val isSeries = isTv && (historyItem.seasonNumber != null || historyItem.episodeNumber != null || !historyItem.mediaType.equals("movie", ignoreCase = true))

    val effectiveDurationMs = if (historyItem.durationMs > 0L) {
        historyItem.durationMs
    } else {
        if (isSeries) 50L * 60000L else 110L * 60000L
    }

    val progress = if (effectiveDurationMs > 0L) {
        (historyItem.currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reduceMotion) tween(0) else tween(300, easing = FastOutSlowInEasing),
        label = "continueCardProgress"
    )

    val remainingMs = (effectiveDurationMs - historyItem.currentPositionMs).coerceAtLeast(0L)
    val remainingMinutes = (remainingMs / (1000 * 60)).coerceAtLeast(1)
    val remainingText = if (remainingMinutes >= 60) {
        val h = remainingMinutes / 60
        val m = remainingMinutes % 60
        "${h}h ${m}m left"
    } else {
        "${remainingMinutes} min left"
    }

    val epInfo = when {
        historyItem.seasonNumber != null && historyItem.episodeNumber != null -> "S${historyItem.seasonNumber}:E${historyItem.episodeNumber}"
        historyItem.seasonNumber != null -> "Season ${historyItem.seasonNumber}"
        historyItem.episodeNumber != null -> "EP ${historyItem.episodeNumber}"
        else -> if (isSeries) "Series" else "Movie"
    }

    val displaySubtitle = buildString {
        append(epInfo)
        historyItem.episodeTitle?.takeIf { it.isNotBlank() }?.let {
            append(" · ${sanitizeTitle(it)}")
        }
    }

    val cardA11y = "Continue watching ${historyItem.title}, $remainingText, $displaySubtitle"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(120),
        label = "cardPressScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14151F))
            .border(LibraryDimens.hairline, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "View ${historyItem.title} details",
                onClick = onCardClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = cardA11y
            }
    ) {
        // Backdrop image
        val imageUrl = historyItem.backdropUrl ?: historyItem.posterUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient overlay at bottom for strong readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0B0F).copy(alpha = 0.35f),
                            Color(0xFF0B0B0F).copy(alpha = 0.60f),
                            Color(0xFF0B0B0F).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Top-left: small glass chip "Continue · 55 min left" (11 sp, 10 px radius)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Continue · $remainingText",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color.White
                )
            )
        }

        // Top-right download status badge if downloaded
        if (downloadItem != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp)
            ) {
                OfflinePosterBadge(downloadItem = downloadItem)
            }
        }

        // Bottom section with Title, Episode Info, and Solid White Play Circle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = sanitizeTitle(historyItem.title),
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.03).sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = displaySubtitle,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom-right: 40px solid white circle with dark play icon
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (isPlayPressed && !reduceMotion) 0.92f else 1f,
                animationSpec = tween(100),
                label = "playScale"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Play",
                        onClick = onPlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF0B0B0F),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Along very bottom edge: thin 2 px progress bar (track 14%, fill 85%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }
    }
}
