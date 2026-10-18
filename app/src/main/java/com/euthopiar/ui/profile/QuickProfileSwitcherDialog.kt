package com.euthopiar.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.profile.ProfileRepository
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch

/**
 * Quick Profile Switcher Dialog matching Euthopiar minimal glass design language.
 */
@Composable
fun QuickProfileSwitcherDialog(
    onDismissRequest: () -> Unit,
    onManageProfilesClick: () -> Unit,
) {
    val profiles by ProfileRepository.profiles.collectAsState()
    val activeProfileId by ProfileRepository.activeProfileId.collectAsState()

    var pendingUnlockProfile by remember { mutableStateOf<UserProfile?>(null) }
    val isAmoled = LocalAmoledMode.current
    val dialogBg = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val dialogAnim = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            dialogAnim.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        } else {
            dialogAnim.snapTo(1f)
        }
    }

    fun animateDismiss(onFinished: () -> Unit) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            if (!reduceMotion) {
                dialogAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
            onFinished()
        }
    }

    Dialog(
        onDismissRequest = { animateDismiss { onDismissRequest() } },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f * dialogAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateDismiss { onDismissRequest() } }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .graphicsLayer {
                        alpha = dialogAnim.value
                        val scale = 0.94f + (0.06f * dialogAnim.value)
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(dialogBg)
                    .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // block click through
                    )
                    .padding(22.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Who's watching?",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    color = Color.White,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Switch profile to personalize catalog",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = ProfileGlassTokens.TextMuted50,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(ProfileGlassTokens.GlassFill)
                                .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, CircleShape)
                                .clickable { animateDismiss { onDismissRequest() } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Profile Avatar Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(profiles, key = { it.id }) { item ->
                            val isActive = item.id == activeProfileId

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (item.id != activeProfileId) {
                                            if (item.isPinLocked) {
                                                pendingUnlockProfile = item
                                            } else {
                                                animateDismiss {
                                                    ProfileRepository.unlockProfile(item.id)
                                                    ProfileRepository.selectProfile(item.id)
                                                    onDismissRequest()
                                                }
                                            }
                                        } else {
                                            animateDismiss { onDismissRequest() }
                                        }
                                    }
                                    .padding(vertical = 6.dp)
                                    .semantics {
                                        this.role = Role.Tab
                                        this.selected = isActive
                                    }
                                    .tvFocusable(shape = RoundedCornerShape(12.dp)),
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AvatarWithThinRing(
                                        avatarId = item.avatarId,
                                        avatarSize = 56.dp,
                                        ringGap = 2.dp,
                                        isSelected = isActive,
                                        showSelectedCheck = false
                                    )
                                    if (item.isPinLocked) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(ProfileGlassTokens.BaseDarkBackground)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = "PIN",
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp),
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.name,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isActive) Color.White else ProfileGlassTokens.TextMuted50,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Manage Profiles Glass Pill Button
                    ProfileGlassPill(
                        text = "Manage Profiles",
                        icon = Icons.Outlined.Settings,
                        onClick = {
                            animateDismiss {
                                onDismissRequest()
                                onManageProfilesClick()
                            }
                        },
                        visualHeight = 44.dp,
                        horizontalPadding = 20.dp,
                        fontSize = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    pendingUnlockProfile?.let { target ->
        DetroitProfileUnlockDialog(
            profile = target,
            onDismissRequest = { pendingUnlockProfile = null },
            onUnlocked = {
                ProfileRepository.unlockProfile(target.id)
                ProfileRepository.selectProfile(target.id)
                pendingUnlockProfile = null
                onDismissRequest()
            },
        )
    }
}
