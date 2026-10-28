package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.pressable

/**
 * Modern artwork-first Media Card for streaming catalogs.
 * Clean 2:3 aspect ratio, 8dp corner radius, zero drop shadows or border chrome,
 * with pure artwork focus and subtle title below.
 */
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val scaleMode = LocalCardScaleMode.current
    val cardWidth = CardDimensions.verticalWidth(scaleMode)
    val context = LocalContext.current

    val typeLabel = when (item.type) {
        MediaType.MOVIE -> "Movie"
        MediaType.TV_SERIES -> "Series"
        MediaType.ANIME -> "Anime"
        MediaType.LIVE_TV -> "Live TV"
    }
    val cleanTitle = androidx.compose.runtime.remember(item.title) { sanitizeTitle(item.title) }
    val a11yDescription = buildList {
        add(cleanTitle)
        item.year?.let { add(it.toString()) }
        add(typeLabel)
    }.joinToString(", ")

    Column(
        modifier = modifier
            .width(cardWidth)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
            }
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = ShapeS)
    ) {
        // 2:3 Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.posterAspectRatio)
                .clip(ShapeS)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), ShapeS)
        ) {
            AppAsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = null, // decorative - merged in parent semantics
                contentScale = ContentScale.Crop,
                titleForFallback = cleanTitle,
                modifier = Modifier.fillMaxSize()
            )

            ReleaseStatusBadge(
                status = item.releaseStatus,
                releaseDate = item.releaseDate,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacing4))

        // Title below poster
        Text(
            text = cleanTitle,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
