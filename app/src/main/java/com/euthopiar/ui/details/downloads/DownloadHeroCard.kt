package com.euthopiar.ui.details.downloads

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import java.util.Locale

/**
 * Editorial Hero Card displaying the "Best match" download source:
 * - White surface, 22dp corner radius.
 * - Numerals count up over 700ms with EaseOutCubic using tabular numbers.
 * - Black download pill (48dp tall) with spring press (0.97 scale, heroPressed background).
 * - Label cross-fades and slides up 8dp ("Download" -> "Starting…" -> "Added").
 * - Responsive typography scaling for 1.3x font scale to prevent overflow.
 */
@Composable
fun DownloadHeroCard(
    stream: StreamItem,
    downloadState: StreamDownloadState,
    enabled: Boolean,
    isInitialEntrance: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackEstimate: String = "2.00 GB"
) {
    val reduceMotion = isReduceMotion()
    val density = LocalDensity.current
    val fontScale = density.fontScale

    // Adaptive numeral sizes for 1.3x font scale resilience
    val sizeNumeralSp = if (fontScale > 1.15f) 42.sp else 58.sp
    val seedsNumeralSp = if (fontScale > 1.15f) 22.sp else 28.sp
    val pillSlideOffsetPx = with(density) { 8.dp.roundToPx() }

    val parsedSize = remember(stream.size, fallbackEstimate) {
        DownloadTokens.parseStreamSize(stream.size, fallbackEstimate)
    }

    // Entrance animation states
    val cardScale = remember { Animatable(if (reduceMotion || !isInitialEntrance) 1f else 0.96f) }
    val cardAlpha = remember { Animatable(if (reduceMotion || !isInitialEntrance) 1f else 0f) }
    val cardTranslateY = remember { Animatable(if (reduceMotion || !isInitialEntrance) 0f else 16f) }
    val countUpProgress = remember { Animatable(if (reduceMotion || !isInitialEntrance) 1f else 0f) }

    LaunchedEffect(isInitialEntrance) {
        if (!reduceMotion && isInitialEntrance) {
            delay(DownloadTokens.ENTRANCE_HERO_DELAY_MS.toLong())
            launch {
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                )
            }
            launch {
                cardAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                )
            }
            launch {
                cardTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                )
            }
            launch {
                countUpProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = DownloadTokens.COUNT_UP_DURATION_MS,
                        easing = EaseOutCubic
                    )
                )
            }
        } else {
            cardScale.snapTo(1f)
            cardAlpha.snapTo(1f)
            cardTranslateY.snapTo(0f)
            countUpProgress.snapTo(1f)
        }
    }

    val displaySize = if (countUpProgress.value >= 0.99f || reduceMotion) {
        parsedSize.formattedNumeral
    } else {
        val current = parsedSize.numericValue * countUpProgress.value
        String.format(Locale.US, "%.2f", current)
    }

    val displaySeeds = if (countUpProgress.value >= 0.99f || reduceMotion) {
        stream.seeders ?: 0
    } else {
        ((stream.seeders ?: 0) * countUpProgress.value).toInt()
    }

    val sourceLabel = stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName
    val accessibilityLabel = "Best match: " + DownloadTokens.getStreamAccessibilityDescription(
        stream = stream,
        displaySize = "${parsedSize.formattedNumeral} ${parsedSize.unit}"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
                translationY = cardTranslateY.value.dp.toPx()
            }
            .clip(RoundedCornerShape(DownloadTokens.heroCornerRadius))
            .background(DownloadTokens.heroSurface)
            .padding(
                horizontal = DownloadTokens.heroPaddingHorizontal,
                vertical = DownloadTokens.heroPaddingVertical
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: "Best match" & source name (for P2P; for Direct hero it's shown in the middle row right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Best match",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = DownloadTokens.onHero
                    )
                )
                if (stream.isTorrentStream) {
                    Text(
                        text = sourceLabel,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = DownloadTokens.textMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val res = DownloadTokens.extractResolution(stream.quality, stream.name ?: stream.title) ?: stream.quality
                    if (!res.isNullOrBlank()) {
                        Text(
                            text = res,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = DownloadTokens.textMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle row: Size numeral (58sp) + unit, and seeds count or "Direct"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Size numeral + unit (baseline aligned)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displaySize,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = sizeNumeralSp,
                            lineHeight = sizeNumeralSp,
                            letterSpacing = (-0.04).em,
                            fontFeatureSettings = "tnum",
                            color = DownloadTokens.onHero
                        ),
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = parsedSize.unit,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = DownloadTokens.textMuted
                        ),
                        modifier = Modifier
                            .padding(start = 3.dp)
                            .alignByBaseline()
                    )
                }

                // Right: Seeds block (P2P) or Direct block
                if (stream.isTorrentStream) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displaySeeds.toString(),
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = seedsNumeralSp,
                                lineHeight = seedsNumeralSp,
                                letterSpacing = (-0.04).em,
                                fontFeatureSettings = "tnum",
                                color = DownloadTokens.accentCrimson
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
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Direct",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = seedsNumeralSp,
                                lineHeight = seedsNumeralSp,
                                letterSpacing = (-0.04).em,
                                fontFeatureSettings = "tnum",
                                color = DownloadTokens.onHero
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Download Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DownloadTokens.pillHeight)
                    .clip(RoundedCornerShape(DownloadTokens.pillCornerRadius))
                    .editorialPress(
                        pressedScale = 0.97f,
                        pressedColor = DownloadTokens.heroPressed,
                        normalColor = DownloadTokens.onHero,
                        enabled = enabled && downloadState != StreamDownloadState.ADDED,
                        isReduceMotion = reduceMotion,
                        onClick = onDownloadClick
                    )
                    .semantics {
                        this.role = Role.Button
                        this.contentDescription = accessibilityLabel
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    EditorialDownloadIndicator(
                        state = downloadState,
                        tintColor = Color.White,
                        iconSize = 17.dp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier.widthIn(min = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = downloadState,
                            transitionSpec = {
                                if (reduceMotion) {
                                    fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                                } else {
                                    (slideInVertically(
                                        animationSpec = tween(200),
                                        initialOffsetY = { pillSlideOffsetPx }
                                    ) + fadeIn(animationSpec = tween(200))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(200),
                                        targetOffsetY = { -pillSlideOffsetPx }
                                    ) + fadeOut(animationSpec = tween(200)))
                                }
                            },
                            label = "heroPillLabel"
                        ) { state ->
                            val labelText = when (state) {
                                StreamDownloadState.IDLE -> "Download"
                                StreamDownloadState.STARTING -> "Starting…"
                                StreamDownloadState.ADDED -> "Added"
                            }
                            Text(
                                text = labelText,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
