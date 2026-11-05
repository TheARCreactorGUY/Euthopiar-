package com.euthopiar.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.euthopiar.R
import com.euthopiar.data.common.DonationConfig
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 24-Hour Home Donation Reminder Popup.
 *
 * Designed with frosted glassmorphism strictly matching [UpdateInfoDialog]:
 * - Tonal depth: Obsidian 85% alpha (92% AMOLED) with 24dp Haze background blur
 * - Specular border: 0.5dp at 16% white alpha
 * - Corner geometry: 28dp radius
 * - Header block: 20sp Bold title, 4dp gap, 13sp Normal subtitle (54% white)
 * - Subtle hairline divider: 0.5dp at 10% white alpha
 * - Primary action: 48dp solid white pill button with dark text
 * - Secondary action: Quiet text button with tactile touch feedback
 * - Organic spring entrance & dismiss physics
 */
@Composable
fun DonationReminderDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current
    val configuration = LocalConfiguration.current
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

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
        delay(60L)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
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

    val maxDialogHeight = (configuration.screenHeightDp - 32).coerceAtLeast(200).dp
    val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)
    val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackColor = EuthopiarGlassTokens.fallbackFill(isAmoled)

    val openDonationLink = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DonationConfig.BUY_ME_A_COFFEE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Buy Me a Coffee Link", DonationConfig.BUY_ME_A_COFFEE_URL)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "No browser found. Link copied to clipboard", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
        dismissWithAnim()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
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
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent modal dismiss on card click */ }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Block matching UpdateInfoDialog
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enjoying Euthopiar?",
                        style = EuthopiarGlassTokens.HeaderTitle,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your contribution keeps the app free & open source.",
                        style = EuthopiarGlassTokens.HeaderSubtitle,
                        textAlign = TextAlign.Center
                    )
                }

                // Subtle Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EuthopiarGlassTokens.HairlineWidth)
                        .background(EuthopiarGlassTokens.HairlineDivider)
                )

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 160dp Rounded QR Card with Hairline
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.qr_donation),
                            contentDescription = "Buy Me a Coffee QR Code",
                            modifier = Modifier.size(144.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary Action: 48dp Solid White Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .focusRequester(focusRequester)
                            .settingsPressable(onClick = openDonationLink)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedBorderColor = Accent,
                                onClick = openDonationLink
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Donate",
                                style = EuthopiarGlassTokens.PrimaryButtonText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = EuthopiarGlassTokens.TextDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Action: Quiet Text Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .settingsPressable(onClick = dismissWithAnim)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = dismissWithAnim)
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
