package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.pressable

/**
 * Modern artwork-first 2:3 poster card inspired by Apple TV and Netflix.
 * High-performance, clean geometry with dynamic corner radius and optional inline IMDb rating.
 */
@Composable
fun PosterCard(
    posterUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Dimens.posterWidthCompact,
    showTitleBelow: Boolean = true,
    rating: String? = null,
    progress: Float? = null,
    year: Int? = null,
    type: String? = null,
    id: String? = null,
    releaseStatus: com.euthopiar.core.model.ReleaseStatus? = null,
    releaseDate: String? = null,
    onLongClick: (() -> Unit)? = null
) {
    val scaleMode = LocalCardScaleMode.current
    val cardShape = RoundedCornerShape(CardDimensions.posterCornerRadius(scaleMode))
    val titleTopGap = CardDimensions.titleTopSpacing(scaleMode)

    val widthModifier = if (width != Dp.Unspecified) Modifier.width(width) else Modifier.fillMaxWidth()

    val cleanTitle = remember(title) { sanitizeTitle(title) }
    val formattedScore = remember(rating) { formatImdbScoreValue(rating) }
    val a11yDescription = buildString {
        append(cleanTitle)
        if (formattedScore != null) append(", IMDb $formattedScore out of 10")
        if (year != null) append(", $year")
        if (!type.isNullOrBlank()) append(", $type")
        if (progress != null && progress in 0f..1f) {
            val pct = (progress * 100).toInt()
            append(", $pct percent watched")
        }
    }

    Column(
        modifier = modifier
            .then(widthModifier)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
            }
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .tvFocusable(shape = cardShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.posterAspectRatio)
                .clip(cardShape)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), cardShape)
        ) {
            AppAsyncImage(
                model = posterUrl,
                contentDescription = null, // decorative - merged in parent semantics
                contentScale = ContentScale.Crop,
                titleForFallback = cleanTitle,
                modifier = Modifier.fillMaxSize()
            )

            ReleaseStatusBadge(
                status = releaseStatus,
                releaseDate = releaseDate,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp)
            )

            // Optional bottom progress bar for continue-watching items
            if (progress != null && progress in 0f..1f) {
                ProgressBarThin(
                    progress = progress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showTitleBelow) {
            Spacer(modifier = Modifier.height(titleTopGap))
            MediaCardTitleAndRatingRow(
                title = cleanTitle,
                rawRating = rating,
                scaleMode = scaleMode
            )
        }
    }
}
