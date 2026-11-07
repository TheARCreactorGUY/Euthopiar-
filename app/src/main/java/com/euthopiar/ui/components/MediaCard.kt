package com.euthopiar.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.euthopiar.R
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.preferences.CardScaleMode
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.pressable
import java.util.Locale

/**
 * Formats a raw rating string (e.g. "8.4", "8.37", "7.9/10") into a normalized
 * single-decimal score out of 10 (e.g. "8.4"), or null if absent/zero.
 */
fun formatImdbScoreValue(rawRating: String?): String? {
    if (rawRating.isNullOrBlank()) return null
    val cleaned = rawRating.trim()
        .removeSuffix("/10")
        .removePrefix("★")
        .trim()
    val numeric = cleaned.toDoubleOrNull() ?: return null
    if (numeric <= 0.0 || numeric > 10.0) return null
    return String.format(Locale.US, "%.1f", numeric)
}

/**
 * High-craft single-line caption row rendered below content cards across all 3 card sizes:
 * `Title · [IMDb] x.x/10`
 *
 * Uses `Modifier.weight(1f, fill = false)` on the title and unweighted measurement on the
 * `· [IMDb] x.x/10` metadata cluster so Compose measures the IMDb badge and rating first,
 * guaranteeing that both the IMDb logo and `"x.x/10"` score remain 100% visible even on
 * long movie/series titles while keeping short titles snugly paired beside the dot.
 */
@Composable
fun MediaCardTitleAndRatingRow(
    title: String,
    rawRating: String?,
    scaleMode: CardScaleMode,
    modifier: Modifier = Modifier
) {
    val scoreValue = remember(rawRating) { formatImdbScoreValue(rawRating) }
    val titleFontSize = CardDimensions.titleFontSize(scaleMode)
    val titleLineHeight = CardDimensions.titleLineHeight(scaleMode)
    val ratingFontSize = CardDimensions.ratingFontSize(scaleMode)
    val imdbLogoHeight = CardDimensions.imdbLogoHeight(scaleMode)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                letterSpacing = (-0.01).em,
                color = TextPrimary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (scoreValue != null) {
            Text(
                text = " \u00B7 ",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    color = Color.White.copy(alpha = 0.46f),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                softWrap = false
            )

            Image(
                painter = painterResource(id = R.drawable.ic_imdb_logo_badge),
                contentDescription = "IMDb",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(imdbLogoHeight)
                    .aspectRatio(256f / 129f)
                    .clip(RoundedCornerShape(2.5.dp))
            )

            Spacer(modifier = Modifier.width(4.dp))

            val annotatedScore = remember(scoreValue) {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(scoreValue)
                    }
                    withStyle(
                        SpanStyle(
                            color = Color.White.copy(alpha = 0.58f),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("/10")
                    }
                }
            }

            Text(
                text = annotatedScore,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontSize = ratingFontSize,
                    lineHeight = titleLineHeight,
                    letterSpacing = (-0.01).em,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Modern artwork-first Media Card for streaming catalogs.
 * Scales corner radius, typography, and inline IMDb badge across COMPACT, BALANCED, and FLAGSHIP modes.
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
    val cardShape = RoundedCornerShape(CardDimensions.posterCornerRadius(scaleMode))
    val titleTopGap = CardDimensions.titleTopSpacing(scaleMode)

    val typeLabel = when (item.type) {
        MediaType.MOVIE -> "Movie"
        MediaType.TV_SERIES -> "Series"
        MediaType.ANIME -> "Anime"
        MediaType.LIVE_TV -> "Live TV"
    }
    val cleanTitle = remember(item.title) { sanitizeTitle(item.title) }
    val formattedScore = remember(item.rating) { formatImdbScoreValue(item.rating) }
    val a11yDescription = buildList {
        add(cleanTitle)
        formattedScore?.let { add("IMDb $it out of 10") }
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
            .tvFocusable(shape = cardShape)
    ) {
        // 2:3 Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.posterAspectRatio)
                .clip(cardShape)
                .background(SurfaceLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), cardShape)
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

        Spacer(modifier = Modifier.height(titleTopGap))

        // Dynamic Title · [IMDb] x.x/10 row below poster
        MediaCardTitleAndRatingRow(
            title = cleanTitle,
            rawRating = item.rating,
            scaleMode = scaleMode
        )
    }
}
