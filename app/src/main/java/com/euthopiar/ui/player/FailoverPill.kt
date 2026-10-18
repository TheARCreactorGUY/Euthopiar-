package com.euthopiar.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * Transient notification model for in-player stream failover.
 */
data class FailoverNotice(
    val id: Long = System.currentTimeMillis(),
    val title: String = "Switching source",
    val serverName: String = "",
    val resolutionLabel: String = "",
    val provider: String = serverName,
    val onAction: (() -> Unit)? = null,
    val actionLabel: String? = null
)

/**
 * In-Player Sleek 34dp Failover Notification Pill (Section 3.8 / Architectural Challenge 3).
 *
 * - Located at Top-Center (below header/cutouts).
 * - Strictly 34 dp height, full radius (17 dp).
 * - Dark frosted glass (Color(0xCC121212), 0.5 dp white 16% hairline).
 * - 14 dp spinner indicator on left.
 * - Brief text (`Switching · [Provider] · [1080p]`).
 * - Compact action button (e.g. "Cancel" / "Dismiss").
 */
@Composable
fun FailoverPill(
    notice: FailoverNotice?,
    controlsVisible: Boolean = false,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current

    var currentNotice by remember { mutableStateOf<FailoverNotice?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(notice?.id) {
        if (notice != null) {
            currentNotice = notice
            visible = true
            delay(PlayerTokens.FailoverCountdownMs.toLong())
            visible = false
            delay(PlayerTokens.FailoverExitMs.toLong() + 50L)
            onDismiss()
        } else {
            visible = false
        }
    }

    val enterTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeIn(animationSpec = tween(PlayerTokens.ReduceMotionFadeInMs))
        } else {
            val slidePx = with(density) { 14.dp.roundToPx() }
            slideInVertically(
                initialOffsetY = { -slidePx },
                animationSpec = tween(PlayerTokens.FailoverEnterMs, easing = PlayerTokens.EaseOut)
            ) + fadeIn(animationSpec = tween(PlayerTokens.FailoverEnterMs, easing = PlayerTokens.EaseOut))
        }
    }

    val exitTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeOut(animationSpec = tween(PlayerTokens.ReduceMotionFadeOutMs))
        } else {
            val slidePx = with(density) { 10.dp.roundToPx() }
            slideOutVertically(
                targetOffsetY = { -slidePx },
                animationSpec = tween(PlayerTokens.FailoverExitMs, easing = PlayerTokens.EaseIn)
            ) + fadeOut(animationSpec = tween(PlayerTokens.FailoverExitMs, easing = PlayerTokens.EaseIn))
        }
    }

    val topOffset by animateDpAsState(
        targetValue = if (controlsVisible) 84.dp else 14.dp,
        animationSpec = tween(PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut),
        label = "failoverTopOffset"
    )

    AnimatedVisibility(
        visible = visible && currentNotice != null,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = topOffset)
    ) {
        val activeNotice = currentNotice ?: return@AnimatedVisibility
        val providerName = activeNotice.provider.ifBlank { activeNotice.serverName }.ifBlank { "Stream" }
        val qualityName = activeNotice.resolutionLabel.ifBlank { "Auto" }
        val displayText = "Switching · $providerName · $qualityName"

        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0xDD121212))
                .border(
                    width = HairlineWidth,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(17.dp)
                )
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = displayText
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 14 dp spinner indicator
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Brief text
                Text(
                    text = displayText,
                    style = AppTypography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val actionText = activeNotice.actionLabel ?: "Dismiss"
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = actionText
                        ) {
                            activeNotice.onAction?.invoke()
                            visible = false
                            onDismiss()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionText,
                        style = AppTypography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
