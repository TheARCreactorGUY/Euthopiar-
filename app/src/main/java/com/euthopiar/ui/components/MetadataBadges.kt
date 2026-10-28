package com.euthopiar.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.ReleaseStatus
import com.euthopiar.download.DownloadState
import com.euthopiar.ui.theme.*

/**
 * Calm, token-driven badge for metadata tags across Euthopiar.
 * Follows the design system rule: ShapeS, SurfaceHigh background, and AppTypography.badge in TextSecondary.
 * No custom borders, no gold/4K glow.
 */
@Composable
fun FrostedBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary,
    textColor: Color = TextSecondary,
    backgroundColor: Color = SurfaceHigh,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .clip(ShapeS)
            .background(backgroundColor)
            .padding(horizontal = Dimens.spacing8, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // decorative - badge text conveys meaning
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.spacing12)
                )
            }
            Text(
                text = text,
                style = AppTypography.badge,
                color = textColor
            )
        }
    }
}

/**
 * Calm Media Type Badge (e.g. "MOVIE", "SERIES", "ANIME").
 */
@Composable
fun MediaTypeBadge(
    type: MediaType,
    modifier: Modifier = Modifier,
    isTvSerial: Boolean = false
) {
    val activePreset = com.euthopiar.core.util.TmdbBridge.activeRegionPreset
    val label = when (type) {
        MediaType.MOVIE -> "MOVIE"
        MediaType.TV_SERIES -> {
            if (activePreset.isIndianRegion) {
                if (isTvSerial) "TV SERIAL" else "WEB SERIES"
            } else {
                "SERIES"
            }
        }
        MediaType.ANIME -> "ANIME"
        MediaType.LIVE_TV -> "LIVE"
        else -> type.name.replace("_", " ")
    }
    FrostedBadge(
        text = label,
        modifier = modifier
    )
}

/**
 * Calm Star Rating Badge using standard text and TextSecondary icon.
 */
@Composable
fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier
) {
    val cleanRating = rating.take(3)
    FrostedBadge(
        text = cleanRating,
        icon = Icons.Filled.Star,
        iconTint = TextSecondary,
        modifier = modifier
    )
}

/**
 * Quality Badge (e.g. "4K", "HD", "HDR").
 * Strictly text-only, ShapeS, SurfaceHigh, TextSecondary, no gold/glow.
 */
@Composable
fun QualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    FrostedBadge(
        text = quality,
        modifier = modifier
    )
}

/**
 * Status Badge for Downloads (Ready / Downloading / Queued / Paused / Failed).
 * Always presents an icon + text, never colour alone.
 */
@Composable
fun StatusBadge(
    state: DownloadState,
    modifier: Modifier = Modifier
) {
    val (label, icon) = when (state) {
        DownloadState.COMPLETED -> Pair("Ready", Icons.Filled.Check)
        DownloadState.DOWNLOADING -> Pair("Downloading", Icons.Filled.Download)
        DownloadState.QUEUED -> Pair("Queued", Icons.Filled.Schedule)
        DownloadState.PAUSED -> Pair("Paused", Icons.Filled.Pause)
        DownloadState.FAILED -> Pair("Failed", Icons.Filled.Close)
    }
    FrostedBadge(
        text = label,
        icon = icon,
        iconTint = TextSecondary,
        textColor = TextSecondary,
        modifier = modifier
    )
}

/**
 * Generic Status Badge with icon + text.
 */
@Composable
fun StatusBadge(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    FrostedBadge(
        text = label,
        icon = icon,
        iconTint = TextSecondary,
        textColor = TextSecondary,
        modifier = modifier
    )
}

/**
 * Content Rating Badge (e.g. "TV-MA", "PG-13", "18+").
 */
@Composable
fun ContentRatingBadge(
    contentRating: String,
    modifier: Modifier = Modifier
) {
    FrostedBadge(
        text = contentRating,
        modifier = modifier
    )
}

/**
 * Year Pill with token typography.
 */
@Composable
fun YearBadge(
    year: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$year",
        style = AppTypography.badge,
        color = TextSecondary,
        modifier = modifier
    )
}

/**
 * Below-card metadata line using tokens.
 */
@Composable
fun MediaCardMetadataLine(
    year: Int?,
    rating: String?,
    type: MediaType? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4),
        modifier = modifier.padding(top = Dimens.spacing4)
    ) {
        year?.let { y ->
            YearBadge(year = y)
        }

        if (year != null && rating != null) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(ShapeFull)
                    .background(TextTertiary)
            )
        }

        rating?.let { r ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Rating",
                    tint = TextSecondary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = r.take(3),
                    style = AppTypography.badge,
                    color = TextSecondary
                )
            }
        }

        if (type != null) {
            if (year != null || rating != null) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(ShapeFull)
                        .background(TextTertiary)
                )
            }
            Text(
                text = if (type == MediaType.TV_SERIES) "Series" else "Movie",
                style = AppTypography.badge,
                color = TextTertiary
            )
        }
    }
}

/**
 * High-contrast, compact IMDb rating badge pill.
 */
@Composable
fun ImdbRatingPill(
    rating: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4),
        modifier = modifier
            .height(Dimens.spacing24)
            .clip(ShapeFull)
            .background(SurfaceLow)
            .border(HairlineWidth, Outline, ShapeFull)
            .padding(horizontal = Dimens.spacing8)
    ) {
        Box(
            modifier = Modifier
                .clip(ShapeS)
                .background(ImdbGold)
                .padding(horizontal = Dimens.spacing4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IMDb",
                style = AppTypography.badge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Background
            )
        }
        Text(
            text = rating.take(3),
            style = AppTypography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = TextPrimary
        )
    }
}

/**
 * Compact Rotten Tomatoes score badge pill with custom painted tomato icon.
 */
@Composable
fun RottenTomatoesRatingPill(
    rating: String,
    modifier: Modifier = Modifier
) {
    val cleanScore = rating.replace("%", "").trim()
    val scoreInt = cleanScore.toIntOrNull() ?: 0
    val isFresh = scoreInt >= 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4),
        modifier = modifier
            .height(Dimens.spacing24)
            .clip(ShapeFull)
            .background(SurfaceLow)
            .border(HairlineWidth, Outline, ShapeFull)
            .padding(horizontal = Dimens.spacing8)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(Dimens.spacing12)
        ) {
            val tomatoColor = if (isFresh) RottenTomatoesFresh else RottenTomatoesRotten
            drawCircle(
                color = tomatoColor,
                radius = size.width * 0.42f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.56f)
            )
            drawCircle(
                color = Success,
                radius = size.width * 0.16f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.18f)
            )
        }
        Text(
            text = "$cleanScore%",
            style = AppTypography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = TextPrimary
        )
    }
}


/**
 * Helper to format release dates into short uppercase format (e.g. "OCT 31").
 */
fun formatShortReleaseDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "SOON"
    val clean = rawDate.trim()
    return try {
        val parsed = java.time.LocalDate.parse(clean)
        val month = parsed.month.name.take(3)
        val day = parsed.dayOfMonth
        "$month $day"
    } catch (_: Exception) {
        val parts = clean.split("-")
        if (parts.size >= 3) {
            val mNum = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1
            val months = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            val mStr = months.getOrElse(mNum - 1) { "SOON" }
            "$mStr $day"
        } else {
            clean.uppercase(Locale.ROOT)
        }
    }
}

/**
 * Neutral frosted-glass release status pill ("IN CINEMAS" or "UPCOMING").
 * Quiet, premium chrome with backdrop blur matching the UI design specification:
 * - Small pill, 5dp uniform corner radius.
 * - Backdrop blur using Haze when available, with fallback to translucent dark scrim rgba(20,20,22, 0.55).
 * - Border: 0.5dp, white at 22% opacity.
 * - Padding: 4dp vertical, 8dp horizontal.
 * - IN CINEMAS: 5dp solid red (#FF5C4D) dot gently pulsing opacity between 1.0f and 0.30f on a ~1.6s loop + "IN CINEMAS" text.
 * - UPCOMING: 9dp outline chevron-right icon + short release date (e.g. "OCT 31"), white, no pulsing dot.
 * - Typography: 9sp, weight 600, letter spacing 0.5sp, pure white.
 */
@Composable
fun ReleaseStatusBadge(
    status: ReleaseStatus?,
    modifier: Modifier = Modifier,
    releaseDate: String? = null,
    posterUrl: String? = null,
    itemId: String? = null,
    hazeState: HazeState? = null
) {
    if (status != ReleaseStatus.IN_CINEMAS && status != ReleaseStatus.UPCOMING) return

    val isCinemas = status == ReleaseStatus.IN_CINEMAS
    val pillShape = RoundedCornerShape(5.dp)
    val fallbackScrim = Color(0x8C141416) // rgba(20, 20, 22, 0.55)

    // IN CINEMAS gently pulsing live red dot (1.6s loop: 800ms tween reversed)
    val dotAlpha = if (isCinemas) {
        val infiniteTransition = rememberInfiniteTransition(label = "in_cinemas_dot_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        )
        alpha
    } else {
        1.0f
    }

    val formattedDate = remember(releaseDate) {
        formatShortReleaseDate(releaseDate)
    }

    val isUsingHaze = hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val surfaceModifier = modifier
        .clip(pillShape)
        .then(
            if (isUsingHaze) {
                Modifier.hazeEffect(state = hazeState!!) {
                    blurRadius = 14.dp
                    tints = listOf(
                        HazeTint(fallbackScrim)
                    )
                    fallbackTint = HazeTint(fallbackScrim)
                }
            } else {
                Modifier.background(fallbackScrim)
            }
        )
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = 0.22f),
            shape = pillShape
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)

    Box(
        modifier = surfaceModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isCinemas) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5C4D).copy(alpha = dotAlpha))
                )
                Text(
                    text = "IN CINEMAS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    maxLines = 1
                )
            } else {
                // UPCOMING: small outline chevron-right arrow (~9dp)
                androidx.compose.foundation.Canvas(modifier = Modifier.size(9.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.15f)
                        lineTo(size.width * 0.75f, size.height * 0.50f)
                        lineTo(size.width * 0.25f, size.height * 0.85f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.25.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
