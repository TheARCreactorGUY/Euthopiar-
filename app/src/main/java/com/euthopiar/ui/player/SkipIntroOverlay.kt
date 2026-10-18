package com.euthopiar.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.roundToInt

/**
 * Modern floating Skip Intro / Skip Outro pill with a 6-second perimeter countdown line.
 * Refined, compact 34dp pill with mathematically aligned perimeter progressive stroke,
 * outline icons, medium typography, 98% press scale, and clean touch separation.
 */
@Composable
fun SkipIntroOverlay(
    visible: Boolean,
    isOutro: Boolean = false,
    skipLabel: String = if (isOutro) "Skip Outro" else "Skip Intro",
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(1f) }
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(visible) {
        if (visible) {
            animProgress.snapTo(1f)
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    val remainingSeconds = (animProgress.value * 6f).roundToInt().coerceIn(0, 6)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else 250)) +
                slideInHorizontally(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else 250)) { -it },
        exit = fadeOut(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else 200)) +
                slideOutHorizontally(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else 200)) { -it },
        modifier = modifier
    ) {
        val pillShape = CircleShape

        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(pillShape)
                .background(PlayerTokens.VideoOverlayGlassFill)
                .drawWithContent {
                    drawContent()

                    // Exact perimeter track & animated countdown stroke
                    val strokeWidth = 1.5.dp.toPx()
                    if (size.width > strokeWidth && size.height > strokeWidth) {
                        val halfStroke = strokeWidth / 2f
                        val arcW = size.width - strokeWidth
                        val arcH = size.height - strokeWidth
                        val cornerRadius = CornerRadius(arcH / 2f, arcH / 2f)

                        val perimeterPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(Offset(halfStroke, halfStroke), Size(arcW, arcH)),
                                    cornerRadius = cornerRadius
                                )
                            )
                        }

                        // Base perimeter track (subtle 16% white)
                        drawPath(
                            path = perimeterPath,
                            color = Color.White.copy(alpha = 0.16f),
                            style = Stroke(width = strokeWidth)
                        )

                        // Active animated progressive line
                        if (animProgress.value > 0.005f) {
                            val pathMeasure = PathMeasure().apply {
                                setPath(perimeterPath, forceClosed = true)
                            }
                            val activeLength = pathMeasure.length * animProgress.value
                            val activePath = Path()
                            pathMeasure.getSegment(0f, activeLength, activePath, startWithMoveTo = true)
                            drawPath(
                                path = activePath,
                                color = Color.White.copy(alpha = 0.90f),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                .playerPressable(role = Role.Button, onClickLabel = skipLabel) { onSkip() }
                .padding(start = 12.dp, end = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = skipLabel,
                    style = PlayerTokens.RowTitle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )

                // 6s countdown counter badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${remainingSeconds.coerceAtLeast(1)}s",
                        style = PlayerTokens.PillText.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }

                // Vertical hairline separator before close button
                Spacer(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                )

                // Fast dismiss (X) button with 26dp hit box
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = "Dismiss",
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
