package com.euthopiar.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.data.preferences.SectionLabelStyle
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions

private val SectionTitleGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFD4D4D8),
        Color(0xFF71717A)
    )
)

/**
 * Modern Multi-Style Section Header supporting AccentBar, Eyebrow, and RuleLine styles.
 * Guarantees zero layout shift across styles via identical padding and structure.
 */
@Composable
fun SectionHeader(
    title: String,
    style: SectionLabelStyle,
    subtitle: String? = null,
    eyebrow: String? = null,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all"
) {
    val windowDimensions = rememberWindowDimensions()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = windowDimensions.horizontalMargin)
            .padding(top = Dimens.spacing12, bottom = Dimens.spacing8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (style) {
            SectionLabelStyle.AccentBar -> {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical accent bar on the left (width 3dp, unrounded, height matches text block)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (!subtitle.isNullOrBlank()) 38.dp else 22.dp)
                            .background(Accent)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = title,
                            style = AppTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = AppTypography.bodySmall.copy(fontSize = 12.sp),
                                color = TextPrimary.copy(alpha = 0.50f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (onSeeAll != null) {
                    SectionHeaderSeeAllButton(actionLabel = actionLabel, title = title, onSeeAll = onSeeAll)
                }
            }

            SectionLabelStyle.Eyebrow -> {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!eyebrow.isNullOrBlank()) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = AppTypography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.16.em
                            ),
                            color = Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = title,
                        style = AppTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = AppTypography.bodySmall.copy(fontSize = 12.sp),
                            color = TextPrimary.copy(alpha = 0.50f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onSeeAll != null) {
                    SectionHeaderSeeAllButton(actionLabel = actionLabel, title = title, onSeeAll = onSeeAll)
                }
            }

            SectionLabelStyle.RuleLine -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = AppTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                        if (onSeeAll != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            SectionHeaderSeeAllButton(actionLabel = actionLabel, title = title, onSeeAll = onSeeAll)
                        }
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = AppTypography.bodySmall.copy(fontSize = 12.sp),
                            color = TextPrimary.copy(alpha = 0.50f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible SectionHeader overload.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all"
) {
    SectionHeader(
        title = title,
        style = SectionLabelStyle.AccentBar,
        subtitle = null,
        eyebrow = null,
        modifier = modifier,
        onSeeAll = onSeeAll,
        actionLabel = actionLabel
    )
}

@Composable
private fun SectionHeaderSeeAllButton(
    actionLabel: String,
    title: String,
    onSeeAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
            .clip(ShapeS)
            .tvFocusable(shape = ShapeS)
            .pressable(
                role = Role.Button,
                onClickLabel = "$actionLabel $title",
                onClick = onSeeAll
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = actionLabel,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

/**
 * Legacy Section Header preserved unused as requested.
 */
@Composable
@Suppress("unused")
fun LegacySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all",
    showBar: Boolean = true,
    showShine: Boolean = true
) {
    val windowDimensions = rememberWindowDimensions()
    val reduceMotion = LocalReduceMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "SectionHeaderSunray")
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunray_scale"
    )
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunray_alpha"
    )

    val currentScale = if (reduceMotion) 1f else animatedScale
    val currentAlpha = if (reduceMotion) 0.65f else animatedAlpha

    var lastCharRect by remember(title) { mutableStateOf<Rect?>(null) }

    val barHeight = if (windowDimensions.isTV) 17.dp else 13.dp
    val barWidth = if (windowDimensions.isTV) 4.5.dp else 3.5.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = windowDimensions.horizontalMargin, vertical = Dimens.spacing8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (showBar) {
                Box(
                    modifier = Modifier
                        .size(width = barWidth, height = barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush = SectionTitleGradient)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = title,
                style = AppTypography.titleLarge.copy(brush = SectionTitleGradient),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .semantics { heading() }
                    .drawWithContent {
                        drawContent()
                        if (showShine && title.isNotBlank()) {
                            val trimmed = title.trimEnd()
                            val lastChar = trimmed.lastOrNull()
                            val isUppercaseOrDigit = lastChar != null && (lastChar.isUpperCase() || lastChar.isDigit())
                            val hasAscender = lastChar in listOf('b', 'd', 'f', 'h', 'k', 'l', 't')
                            val topOffset = when {
                                isUppercaseOrDigit || hasAscender -> 4.5.dp.toPx()
                                else -> 8.5.dp.toPx()
                            }

                            val center = lastCharRect?.let { rect ->
                                Offset(
                                    x = (rect.right - 1.5.dp.toPx()).coerceAtLeast(0f),
                                    y = rect.top + topOffset
                                )
                            } ?: Offset(
                                x = (size.width - 3.dp.toPx()).coerceAtLeast(0f),
                                y = topOffset
                            )

                            drawSunrayShine(
                                center = center,
                                scale = currentScale,
                                alpha = currentAlpha
                            )
                        }
                    },
                onTextLayout = { layoutResult ->
                    val trimmed = title.trimEnd()
                    if (trimmed.isNotEmpty()) {
                        val lastCharIndex = (trimmed.length - 1).coerceAtLeast(0)
                        try {
                            lastCharRect = layoutResult.getBoundingBox(lastCharIndex)
                        } catch (_: Exception) {
                            lastCharRect = null
                        }
                    }
                }
            )
        }

        if (onSeeAll != null) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                    .clip(ShapeS)
                    .tvFocusable(shape = ShapeS)
                    .pressable(
                        role = Role.Button,
                        onClickLabel = "$actionLabel $title",
                        onClick = onSeeAll
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = actionLabel,
                    style = AppTypography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun DrawScope.drawSunrayShine(
    center: Offset,
    scale: Float = 1f,
    alpha: Float = 0.65f
) {
    val effectiveAlpha = alpha.coerceIn(0f, 0.65f)

    // 1. Soft ambient diamond bloom (diffuse light aura)
    val bloomRadius = 6.dp.toPx() * scale
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.45f * effectiveAlpha),
            0.35f to Color(0xFFE2E8F0).copy(alpha = 0.20f * effectiveAlpha),
            0.7f to Color.White.copy(alpha = 0.05f * effectiveAlpha),
            1.0f to Color.Transparent,
            center = center,
            radius = bloomRadius
        ),
        radius = bloomRadius,
        center = center,
        blendMode = BlendMode.Screen
    )

    // 2. Anamorphic horizontal flare streak (soft cinematic light beam)
    val hStreakLength = 6.dp.toPx() * scale
    drawLine(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = 0.65f * effectiveAlpha),
            1.0f to Color.Transparent,
            startX = center.x - hStreakLength,
            endX = center.x + hStreakLength
        ),
        start = Offset(center.x - hStreakLength, center.y),
        end = Offset(center.x + hStreakLength, center.y),
        strokeWidth = 1.1.dp.toPx() * scale,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Screen
    )

    // 3. Vertical soft sunbeam
    val vStreakLength = 4.5.dp.toPx() * scale
    drawLine(
        brush = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = 0.55f * effectiveAlpha),
            1.0f to Color.Transparent,
            startY = center.y - vStreakLength,
            endY = center.y + vStreakLength
        ),
        start = Offset(center.x, center.y - vStreakLength),
        end = Offset(center.x, center.y + vStreakLength),
        strokeWidth = 0.9.dp.toPx() * scale,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Screen
    )

    // 4. Subtle diagonal sunrays at 40° and 130°
    val diagLength = 3.5.dp.toPx() * scale
    rotate(degrees = 40f, pivot = center) {
        drawLine(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.40f * effectiveAlpha),
                1.0f to Color.Transparent,
                startX = center.x - diagLength,
                endX = center.x + diagLength
            ),
            start = Offset(center.x - diagLength, center.y),
            end = Offset(center.x + diagLength, center.y),
            strokeWidth = 0.7.dp.toPx() * scale,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen
        )
    }

    rotate(degrees = 130f, pivot = center) {
        drawLine(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.35f * effectiveAlpha),
                1.0f to Color.Transparent,
                startX = center.x - diagLength,
                endX = center.x + diagLength
            ),
            start = Offset(center.x - diagLength, center.y),
            end = Offset(center.x + diagLength, center.y),
            strokeWidth = 0.7.dp.toPx() * scale,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen
        )
    }

    // 5. Specular highlight core (feathered micro-glow + center pin)
    val coreGlowRadius = 1.8.dp.toPx() * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f * effectiveAlpha),
                Color.White.copy(alpha = 0.20f * effectiveAlpha),
                Color.Transparent
            ),
            center = center,
            radius = coreGlowRadius
        ),
        radius = coreGlowRadius,
        center = center,
        blendMode = BlendMode.Screen
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.65f * effectiveAlpha),
        radius = 0.55.dp.toPx() * scale,
        center = center,
        blendMode = BlendMode.Screen
    )
}

