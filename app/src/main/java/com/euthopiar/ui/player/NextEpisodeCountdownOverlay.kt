package com.euthopiar.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.HairlineWidth

@Composable
fun NextEpisodeCountdownOverlay(
    visible: Boolean,
    nextEpisodeTitle: String,
    countdownSeconds: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
        exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .videoOverlayGlass(PlayerTokens.RadiusMedium)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up next in ${countdownSeconds}s",
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.70f)
                    )

                    Box(
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = "Cancel countdown",
                                onClick = onCancel
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .videoOverlayGlass(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = nextEpisodeTitle,
                    style = PlayerTokens.RowTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Solid white primary pill with near-black label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .playerPressable(
                                isWhiteElement = true,
                                role = Role.Button,
                                onClickLabel = "Play now",
                                onClick = onPlayNow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = PlayerTokens.LabelDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Play now",
                                color = PlayerTokens.LabelDark,
                                style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    // Glass cancel pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .videoOverlayGlass(CircleShape)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = "Cancel",
                                onClick = onCancel
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            style = AppTypography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
