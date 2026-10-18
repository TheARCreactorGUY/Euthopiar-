package com.euthopiar.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import com.euthopiar.ui.library.sanitizeTitle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.Outline
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable

/**
 * Consumer-grade quick action bottom sheet for media items.
 * Triggered on long-press (or TV context menu) from PosterCard and MediaCard.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MediaQuickActionsSheet(
    item: MediaItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDetailsClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cleanTitle = remember(item.title) { sanitizeTitle(item.title) }

    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing24)
                .padding(bottom = Dimens.spacing32)
        ) {
            // Header: Thumbnail + Title + Meta info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .aspectRatio(Dimens.posterAspectRatio)
                        .clip(ShapeS)
                        .background(SurfaceLow)
                ) {
                    AppAsyncImage(
                        model = item.posterUrl ?: item.backdropUrl,
                        contentDescription = cleanTitle,
                        contentScale = ContentScale.Crop,
                        titleForFallback = cleanTitle,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.spacing16))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanTitle,
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val metaString = buildString {
                        if (item.year != null && item.year != 0) {
                            append(item.year)
                            append(" • ")
                        }
                        append(if (item.type == MediaType.TV_SERIES) "Series" else "Movie")
                    }
                    Text(
                        text = metaString,
                        style = AppTypography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))
            HorizontalDivider(color = Outline, thickness = HairlineWidth)
            Spacer(modifier = Modifier.height(Dimens.spacing8))

            // Action 1: Play / Resume (if provided)
            if (onPlayClick != null) {
                QuickActionRow(
                    icon = Icons.Outlined.PlayArrow,
                    label = "Play",
                    onClick = {
                        onDismissRequest()
                        onPlayClick()
                    }
                )
            }

            // Action 2: Add / Remove Watchlist
            QuickActionRow(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = if (isBookmarked) "In watchlist" else "Add to watchlist",
                iconTint = if (isBookmarked) Accent else TextPrimary,
                onClick = {
                    onToggleBookmark()
                    onDismissRequest()
                }
            )

            // Action 2.5: Add to Playlist
            if (onAddToPlaylist != null) {
                QuickActionRow(
                    icon = Icons.Filled.PlaylistAdd,
                    label = "Add to playlist",
                    onClick = {
                        onDismissRequest()
                        onAddToPlaylist()
                    }
                )
            }

            // Action 3: Details
            QuickActionRow(
                icon = Icons.Outlined.Info,
                label = "View details",
                onClick = {
                    onDismissRequest()
                    onDetailsClick()
                }
            )

            // Action 4: Share
            QuickActionRow(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = {
                    onDismissRequest()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, item.title)
                        putExtra(Intent.EXTRA_TEXT, "Watch ${item.title} on Euthopiar")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${item.title}"))
                }
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.minTouchTarget)
            .clip(ShapeS)
            .tvFocusable(shape = ShapeS)
            .pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(Dimens.iconL)
        )
        Spacer(modifier = Modifier.width(Dimens.spacing16))
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Open action",
            tint = TextSecondary,
            modifier = Modifier.size(Dimens.iconS)
        )
    }
}
