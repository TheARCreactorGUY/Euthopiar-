package com.euthopiar.ui.details

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Frosted Glass Guide Modal displayed when tapping the faded Play button
 * without any streaming provider plugins installed.
 *
 * Designed with the exact same visual luxury, glassmorphism, and Emil Kowalski
 * spring physics as [com.euthopiar.ui.components.UpdateInfoDialog].
 */
@Composable
fun NoStreamingEngineDialog(
    onDismiss: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    val dialogProgress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            dialogProgress.snapTo(1f)
        } else {
            dialogProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 550f)
            )
        }
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    dialogProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = dialogProgress.value
    val scrimAlpha = if (reduceMotion) EuthopiarGlassTokens.ScrimAlpha else (EuthopiarGlassTokens.ScrimAlpha * animValue).coerceIn(0f, EuthopiarGlassTokens.ScrimAlpha)
    val cardScale = if (reduceMotion) 1f else 0.94f + 0.06f * animValue
    val cardTranslationY = if (reduceMotion) 0f else (1f - animValue) * 30f
    val cardAlpha = if (reduceMotion) 1f else animValue.coerceIn(0f, 1f)

    val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)
    val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackColor = EuthopiarGlassTokens.fallbackFill(isAmoled)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(200f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            ),
        contentAlignment = Alignment.Center
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Modal Dialog Card
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    translationY = cardTranslationY
                    alpha = cardAlpha
                }
                .widthIn(max = 390.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent dismiss on card click */ }
                )
                .clip(cardShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = EuthopiarGlassTokens.BlurRadius
                            tints = listOf(HazeTint(cardFill))
                            fallbackTint = HazeTint(fallbackColor)
                        }
                    } else {
                        Modifier.background(fallbackColor)
                    }
                )
                .border(
                    width = EuthopiarGlassTokens.HairlineWidth,
                    color = EuthopiarGlassTokens.HairlineBorder,
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Frosted Glass Icon Tile
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = EuthopiarGlassTokens.HairlineWidth,
                            color = EuthopiarGlassTokens.HairlineBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Streaming Engine Needed",
                    style = EuthopiarGlassTokens.HeaderTitle,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Description
                Text(
                    text = "Movies and series require a streaming provider plugin from the repository section to play. Let's add your first repository so you can stream seamlessly!",
                    style = EuthopiarGlassTokens.BodyText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pinned Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary 48dp Solid White Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .settingsPressable(
                                onClick = {
                                    dismissWithAnim()
                                    onNavigateToPlugins()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Set Up Plugins & Scrapers",
                            style = EuthopiarGlassTokens.PrimaryButtonText
                        )
                    }

                    // Secondary Quiet Text Action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .settingsPressable(onClick = dismissWithAnim)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Maybe Later",
                            style = EuthopiarGlassTokens.SecondaryButtonText
                        )
                    }
                }
            }
        }
    }
}
