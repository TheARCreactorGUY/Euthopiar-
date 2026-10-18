package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeM
import com.euthopiar.ui.theme.SurfaceHigh
import com.euthopiar.ui.theme.SurfaceLow
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable

/**
 * Modern 16:9 Backdrop Card for episode items and continue-watching rows.
 */
@Composable
fun BackdropCard(
    backdropUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp = 240.dp,
    progress: Float? = null
) {
    val context = LocalContext.current

    val a11yDescription = buildString {
        append(title)
        if (subtitle != null) append(", $subtitle")
        if (progress != null && progress in 0f..1f) {
            val pct = (progress * 100).toInt()
            append(", $pct percent watched")
        }
    }

    Column(
        modifier = modifier
            .width(width)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
            }
            .pressable(onClick = onClick)
            .tvFocusable(shape = ShapeM)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.backdropAspectRatio)
                .clip(ShapeM)
                .background(SurfaceLow)
        ) {
            AppAsyncImage(
                model = backdropUrl,
                contentDescription = null, // decorative - merged in parent semantics
                contentScale = ContentScale.Crop,
                titleForFallback = title,
                modifier = Modifier.fillMaxSize()
            )

            if (progress != null && progress in 0f..1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Accent,
                    trackColor = SurfaceHigh
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacing4))
        Text(
            text = title,
            style = AppTypography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppTypography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
