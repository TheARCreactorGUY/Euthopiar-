package com.euthopiar.ui.details.downloads

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.core.streams.StreamItem
import com.euthopiar.ui.theme.AppFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Editorial source row:
 * - 64dp min height, 15dp vertical padding, flat tappable surface.
 * - Left: size numeral 28sp Medium + "GB" unit (13sp textMuted, baseline aligned).
 * - Right: seeds count (22sp Medium) + "seeds" (11sp textMuted) or "Direct" + source name.
 * - Seed health tiers applied together to size numeral, seeds numeral, and arrow icon.
 * - Staggered entrance animation (45ms per row up to 8th row, 320ms FastOutSlowInEasing).
 * - Row press: scale to 0.98 and background to pressedSurface with spring overshoot.
 */
@Composable
fun DownloadSourceRow(
    stream: StreamItem,
    index: Int,
    showResolution: Boolean,
    downloadState: StreamDownloadState,
    enabled: Boolean,
    isInitialEntrance: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    fallbackEstimate: String = "2.00 GB"
) {
    val reduceMotion = isReduceMotion()
    val isDirect = !stream.isTorrentStream
    val tierColor = remember(stream.seeders, isDirect) {
        DownloadTokens.getHealthTierColor(stream.seeders, isDirect)
    }

    val parsedSize = remember(stream.size, fallbackEstimate) {
        DownloadTokens.parseStreamSize(stream.size, fallbackEstimate)
    }

    val resolutionText = remember(stream.quality, stream.name, stream.title) {
        DownloadTokens.extractResolution(stream.quality, stream.name ?: stream.title)
    }

    val sourceLabel = stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName
    val accessibilityLabel = DownloadTokens.getStreamAccessibilityDescription(
        stream = stream,
        displaySize = "${parsedSize.formattedNumeral} ${parsedSize.unit}"
    )

    // Staggered entrance animation
    val rowAlpha = remember { Animatable(if (reduceMotion || !isInitialEntrance) 1f else 0f) }
    val rowTranslateY = remember { Animatable(if (reduceMotion || !isInitialEntrance) 0f else 12f) }

    LaunchedEffect(isInitialEntrance) {
        if (!reduceMotion && isInitialEntrance) {
            val staggerIndex = minOf(index, DownloadTokens.ENTRANCE_ROW_MAX_STAGGER_INDEX)
            val delayMs = staggerIndex * DownloadTokens.ENTRANCE_ROW_STAGGER_MS.toLong()
            delay(delayMs)
            launch {
                rowAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = DownloadTokens.ENTRANCE_ROW_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            launch {
                rowTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = DownloadTokens.ENTRANCE_ROW_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        } else {
            rowAlpha.snapTo(1f)
            rowTranslateY.snapTo(0f)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DownloadTokens.rowMinHeight)
            .graphicsLayer {
                alpha = rowAlpha.value
                translationY = rowTranslateY.value.dp.toPx()
            }
            .editorialPress(
                pressedScale = 0.98f,
                pressedColor = DownloadTokens.pressedSurface,
                enabled = enabled && downloadState != StreamDownloadState.ADDED,
                isReduceMotion = reduceMotion,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = accessibilityLabel
            }
            .padding(vertical = DownloadTokens.rowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: Size numeral + unit (and resolution if mixed)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = parsedSize.formattedNumeral,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 28.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.04).em,
                        fontFeatureSettings = "tnum",
                        color = tierColor
                    ),
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = parsedSize.unit,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = DownloadTokens.textMuted
                    ),
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .alignByBaseline()
                )
            }

            if (showResolution && !resolutionText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolutionText,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = DownloadTokens.textMuted
                    )
                )
            }
        }

        // Right stacked column: Seeds count or "Direct" + source name
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            if (stream.isTorrentStream) {
                Text(
                    text = (stream.seeders ?: 0).toString(),
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.04).em,
                        fontFeatureSettings = "tnum",
                        color = tierColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "seeds",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = DownloadTokens.textMuted
                    )
                )
            } else {
                Text(
                    text = "Direct",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.04).em,
                        fontFeatureSettings = "tnum",
                        color = tierColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceLabel,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = DownloadTokens.textMuted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Arrow down or download state indicator
        EditorialDownloadIndicator(
            state = downloadState,
            tintColor = tierColor,
            iconSize = 20.dp
        )
    }
}
