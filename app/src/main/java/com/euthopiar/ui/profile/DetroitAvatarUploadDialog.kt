package com.euthopiar.ui.profile

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.profile.CustomAvatarRepository
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.EuthopiarGlassTokens
import com.euthopiar.ui.theme.LocalAmoledMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Avatar Upload Dialog matching Euthopiar frosted glass design language.
 */
@Composable
fun DetroitAvatarUploadDialog(
    currentAvatarId: String,
    onDismissRequest: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onChooseFromGallery: () -> Unit,
    hazeState: HazeState? = null,
) {
    val recentAvatars by CustomAvatarRepository.recentAvatars.collectAsState()
    val isAmoled = LocalAmoledMode.current

    val cardShape = RoundedCornerShape(EuthopiarGlassTokens.RadiusDialog)
    val cardFill = EuthopiarGlassTokens.cardFill(isAmoled)
    val fallbackColor = EuthopiarGlassTokens.fallbackFill(isAmoled)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = EuthopiarGlassTokens.ScrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Avatar",
                                style = EuthopiarGlassTokens.HeaderTitle,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload image or reuse recent photos",
                                style = EuthopiarGlassTokens.HeaderSubtitle,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(EuthopiarGlassTokens.HairlineWidth, EuthopiarGlassTokens.HairlineBorder, CircleShape)
                                .clickable { onDismissRequest() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Subtle Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EuthopiarGlassTokens.HairlineWidth)
                            .background(EuthopiarGlassTokens.HairlineDivider)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Choose from Gallery: 48dp solid white primary pill
                        ProfileWhitePillButton(
                            text = "Choose from Gallery",
                            onClick = onChooseFromGallery
                        )

                        if (recentAvatars.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "RECENT CUSTOM UPLOADS",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.08.sp,
                                    color = ProfileGlassTokens.TextLabel45,
                                ),
                                modifier = Modifier.align(Alignment.Start),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                recentAvatars.forEach { fileName ->
                                    val fullId = "custom:$fileName"
                                    val isSelected = currentAvatarId == fullId

                                    AvatarWithThinRing(
                                        avatarId = fullId,
                                        avatarSize = 52.dp,
                                        ringGap = 2.dp,
                                        isSelected = isSelected,
                                        showSelectedCheck = true,
                                        onClick = {
                                            onAvatarSelected(fullId)
                                            onDismissRequest()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
