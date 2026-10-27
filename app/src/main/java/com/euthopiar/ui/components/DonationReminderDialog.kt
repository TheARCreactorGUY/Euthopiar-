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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.euthopiar.R
import com.euthopiar.data.common.DonationConfig
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.components.settingsGlass
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppFontFamily
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
 * Designed with frosted glassmorphism matching [UpdateInfoDialog] and [SeasonSheet]:
 * - Eyebrow: "SUPPORT EUTHOPIAR"
 * - Headline: "Enjoying Euthopiar?"
 * - Body: "Your smallest contribution helps keep the application free."
 * - Centered 170dp rounded QR card displaying the Buy Me a Coffee donation QR.
 * - Solid white pill "Donate" button with external link icon.
 * - Frosted pill "Later" button.
 * - Responsive scrolling for compact screens and TV D-pad focus resilience.
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
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
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
                        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = dialogProgress.value
    val scrimAlpha = if (reduceMotion) 0.60f else (0.60f * animValue).coerceIn(0f, 0.60f)
    val cardScale = if (reduceMotion) 1f else 0.92f + 0.08f * animValue
    val cardAlpha = if (reduceMotion) 1f else animValue

    val maxDialogHeight = (configuration.screenHeightDp - 32).coerceAtLeast(200).dp
    val cardShape = RoundedCornerShape(SettingsTokens.RadiusDialog)
    val cardFill = if (isAmoled) Color.Black.copy(alpha = 0.88f) else Color(0xFF0E1118).copy(alpha = 0.74f)
    val fallbackColor = if (isAmoled) Color.Black.copy(alpha = 0.94f) else Color(0xFF0E1118).copy(alpha = 0.94f)

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
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Dialog Card Container
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent dismiss on card click */ }
                )
                .clip(cardShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 22.dp
                            tints = listOf(HazeTint(cardFill))
                            fallbackTint = HazeTint(fallbackColor)
                        }
                    } else {
                        Modifier.background(fallbackColor)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = cardShape
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Eyebrow
                Text(
                    text = "SUPPORT EUTHOPIAR",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = (11 * 0.08).sp,
                        color = SettingsTokens.TextWhite45,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Headline
                Text(
                    text = "Enjoying Euthopiar?",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        color = SettingsTokens.TextWhite96,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Body
                Text(
                    text = "Your smallest contribution helps keep the application free.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = SettingsTokens.TextWhite72,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 170dp Rounded QR Code Card
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qr_donation),
                        contentDescription = "Buy Me a Coffee QR Code",
                        modifier = Modifier.size(154.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary "Donate" Button (46dp solid white pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
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
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = SettingsTokens.TextDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = SettingsTokens.TextDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary "Later" Button (40dp frosted glass pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .settingsGlass(
                            shape = CircleShape,
                            blurRadius = 14.dp
                        )
                        .settingsPressable(onClick = dismissWithAnim)
                        .tvFocusable(shape = CircleShape, onClick = dismissWithAnim),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Later",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SettingsTokens.TextWhite88
                        )
                    )
                }
            }
        }
    }
}
