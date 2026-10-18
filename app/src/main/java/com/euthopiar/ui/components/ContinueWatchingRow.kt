package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.data.local.PlaybackHistoryEntity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeM
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions

/**
 * Modern Netflix / Apple TV style Continue Watching Row.
 * Uses 16:9 widescreen thumbnails with a crisp Accent red bottom progress line,
 * single clean title/episode label below, and zero duplicate badges.
 */
@Composable
fun ContinueWatchingRow(
    items: List<PlaybackHistoryEntity>,
    onItemClick: (PlaybackHistoryEntity) -> Unit,
    onDeleteItem: ((PlaybackHistoryEntity) -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
    sectionLabelStyle: com.euthopiar.data.preferences.SectionLabelStyle = com.euthopiar.data.preferences.SectionLabelStyle.AccentBar,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var itemToDelete by remember { mutableStateOf<PlaybackHistoryEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val windowDimensions = rememberWindowDimensions()
    val continueWatchingState = androidx.compose.runtime.saveable.rememberSaveable(
        "continue_watching_row",
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacing8)
    ) {
        SectionHeader(
            title = "Continue Watching",
            style = sectionLabelStyle,
            actionLabel = if (onClearAll != null) "Clear" else "See all",
            onSeeAll = if (onClearAll != null) { { showClearAllDialog = true } } else null
        )

        LazyRow(
            state = continueWatchingState,
            contentPadding = PaddingValues(horizontal = windowDimensions.horizontalMargin),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongClick = { itemToDelete = item }
                )
            }
        }
    }

    itemToDelete?.let { item ->
        val metadata = if (item.seasonNumber != null && item.episodeNumber != null) {
            "Season ${item.seasonNumber}, Episode ${item.episodeNumber}"
        } else {
            item.mediaType?.replaceFirstChar { it.uppercase() } ?: "Movie"
        }
        EuthopiarGlassDialog(
            onDismissRequest = { itemToDelete = null },
            title = "Remove from Continue Watching?",
            message = "Remove this title from your continue watching list?",
            confirmText = "Remove",
            dismissText = "Cancel",
            icon = Icons.Outlined.VisibilityOff,
            isDestructive = true,
            itemPreview = {
                EuthopiarDialogSingleItemRow(
                    title = item.title,
                    metadata = metadata,
                    posterUrl = item.posterUrl
                )
            },
            onConfirm = {
                onDeleteItem?.invoke(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (showClearAllDialog) {
        EuthopiarGlassDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = "Clear Continue Watching?",
            message = "This will remove all items from your continue watching list.",
            confirmText = "Clear All",
            dismissText = "Cancel",
            icon = Icons.Outlined.VisibilityOff,
            isDestructive = true,
            onConfirm = {
                onClearAll?.invoke()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
fun ContinueWatchingCard(
    item: PlaybackHistoryEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scaleMode = LocalCardScaleMode.current
    val cardWidth = CardDimensions.horizontalWidth(scaleMode)
    val context = LocalContext.current

    val progress = if (item.durationMs > 0) {
        (item.currentPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isAnime = item.mediaType?.contains("anime", ignoreCase = true) == true
    val isTv = item.seasonNumber != null || item.episodeNumber != null ||
        item.mediaType?.contains("tv", ignoreCase = true) == true ||
        item.mediaType?.contains("series", ignoreCase = true) == true ||
        isAnime
    val isSeries = isTv && (item.seasonNumber != null || item.episodeNumber != null || !item.mediaType.equals("movie", ignoreCase = true))

    val tagText = when {
        item.seasonNumber != null && item.episodeNumber != null -> "S${item.seasonNumber} · E${item.episodeNumber}"
        item.seasonNumber != null -> "Season ${item.seasonNumber}"
        item.episodeNumber != null -> "EP ${item.episodeNumber}"
        isAnime -> "Anime"
        isSeries -> "Series"
        else -> "Movie"
    }
    val tagIcon = if (tagText == "Movie") Icons.Outlined.Movie else Icons.Outlined.Tv

    Column(
        modifier = modifier
            .width(cardWidth)
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = ShapeM)
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.backdropAspectRatio)
                .clip(ShapeM)
                .background(SurfaceLow)
        ) {
            AppAsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                titleForFallback = item.title,
                modifier = Modifier.fillMaxSize()
            )

            // Top Season/Episode Tag with cool icon matching library page style
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0F).copy(alpha = 0.85f))
                    .border(
                        0.5.dp,
                        Color.White.copy(alpha = 0.16f),
                        CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 3.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = tagText,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    )
                }
            }

            // Bottom Accent Red Progress Bar
            if (progress > 0f) {
                ProgressBarThin(
                    progress = progress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacing4))

        // Single clean text line below: Title
        Text(
            text = item.title,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
