package com.euthopiar.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion

/**
 * Screen 2: "Who's watching?" Profile Screen.
 * Bold, minimal, modern, cinematic design language.
 * - Headline "Who's watching?" on two lines.
 * - Center focused avatar with thin white hairline ring (150 dp avatar, 12 dp gap, 0.5 px border at 16%).
 * - Soft muted indigo haze blob behind avatar (disabled in AMOLED mode).
 * - Name 30 sp medium, neutral glass chip, "Edit profile" pill.
 * - Bottom floating glass dock (76 dp tall, 38 dp radius, 40 dp from bottom edge) with 46 dp avatars inside 56 dp slots.
 * - Dashed add button slot.
 */
@Composable
fun ProfileManagementSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    val profiles by profileManager.profiles.collectAsState()
    val activeProfile by profileManager.activeProfile.collectAsState()
    val haptic = LocalHapticFeedback.current

    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current

    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var pendingUnlockProfile by remember { mutableStateOf<UserProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }

    // Screen enter animation
    var enterTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        enterTriggered = true
    }

    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    fun animateDismiss() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            if (!reduceMotion) {
                delay(180)
            }
            onDismiss()
        }
    }

    BackHandler {
        animateDismiss()
    }

    val exitAlpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "sheetExitAlpha"
    )

    val headlineAlpha by animateFloatAsState(
        targetValue = if (enterTriggered || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "headlineAlpha"
    )

    val heroScale by animateFloatAsState(
        targetValue = if (enterTriggered || reduceMotion) 1f else 0.96f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "heroScale"
    )

    val heroAlpha by animateFloatAsState(
        targetValue = if (enterTriggered || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )

    val dockOffsetY by animateDpAsState(
        targetValue = if (enterTriggered || reduceMotion) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 320, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "dockOffsetY"
    )

    val dockAlpha by animateFloatAsState(
        targetValue = if (enterTriggered || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 320, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "dockAlpha"
    )

    // Subtle ambient indigo haze drift (20s) for normal dark mode only
    val infiniteTransition = rememberInfiniteTransition(label = "hazeBlobDrift")
    val hazeDrift = infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazeBlobDrift"
    )

    val backgroundColor = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground

    Dialog(
        onDismissRequest = ::animateDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = exitAlpha }
                .background(backgroundColor)
                .then(
                    if (!isAmoled) {
                        Modifier.drawBehind {
                            val effectiveDrift = if (reduceMotion) 0f else hazeDrift.value
                            val centerX = size.width * 0.50f + effectiveDrift
                            val centerY = size.height * 0.40f - (effectiveDrift * 0.5f)
                            // Soft muted indigo haze blob (20% opacity, fully faded edges, no banding)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.00f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.22f),
                                    0.30f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.14f),
                                    0.60f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.05f),
                                    1.00f to Color.Transparent,
                                    center = Offset(centerX, centerY),
                                    radius = size.width * 0.75f
                                )
                            )
                        }
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Top Area: Headline "Who's watching?" on two lines + "Done" glass pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = headlineAlpha }
                    ) {
                        Text(
                            text = "Who's",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 40.sp,
                                lineHeight = 40.sp,
                                letterSpacing = (-0.03).sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "watching?",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 40.sp,
                                lineHeight = 40.sp,
                                letterSpacing = (-0.03).sp,
                                color = Color.White
                            )
                        )
                    }

                    ProfileGlassPill(
                        text = "Done",
                        onClick = ::animateDismiss,
                        visualHeight = 36.dp,
                        horizontalPadding = 18.dp,
                        fontSize = 13
                    )
                }

                // 2. Center Focus Area (Vertically centered between headline and dock)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activeProfile,
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(animationSpec = tween(120)).togetherWith(fadeOut(animationSpec = tween(120)))
                            } else {
                                (fadeIn(animationSpec = tween(240)) + scaleIn(animationSpec = tween(240, easing = FastOutSlowInEasing), initialScale = 0.96f))
                                    .togetherWith(fadeOut(animationSpec = tween(120)))
                            }
                        },
                        label = "HeroProfileCrossfade"
                    ) { currentProfile ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = heroAlpha
                                scaleX = heroScale
                                scaleY = heroScale
                            }
                        ) {
                            // Large avatar: 150 dp, thin ring: 0.5 px white at 16%, 12 dp gap (outer size 174 dp)
                            AvatarWithThinRing(
                                avatarId = currentProfile.avatarId,
                                avatarSize = 150.dp,
                                ringGap = 12.dp,
                                ringBorderWidth = 0.5.dp,
                                ringColor = Color.White.copy(alpha = 0.16f),
                                onClick = {
                                    editingProfile = currentProfile
                                    isCreatingNew = false
                                }
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Profile Name: 30 sp medium, slightly negative letter spacing
                            Text(
                                text = currentProfile.name,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 30.sp,
                                    lineHeight = 32.sp,
                                    letterSpacing = (-0.02).sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Neutral Role Chip: 22 dp tall, 8 dp radius, 11 sp text
                            ProfileGlassChip(
                                text = if (currentProfile.isKids) "Kids" else currentProfile.type.displayName,
                                showLockIcon = currentProfile.isPinLocked
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // "Edit profile" Glass Pill: 44 dp tall, 24 dp horizontal padding, outline pencil icon + text
                            ProfileGlassPill(
                                text = "Edit profile",
                                icon = Icons.Outlined.Edit,
                                visualHeight = 44.dp,
                                horizontalPadding = 24.dp,
                                fontSize = 14,
                                onClick = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    editingProfile = currentProfile
                                    isCreatingNew = false
                                }
                            )
                        }
                    }
                }

                // 3. Bottom Switcher Dock
                // Glass pill container: 76 dp tall, 38 dp radius, centered horizontally, 40 dp from bottom edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                        .graphicsLayer {
                            translationY = dockOffsetY.value
                            alpha = dockAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val dockScrollState = rememberScrollState()

                    // Auto-scroll selected avatar into view
                    LaunchedEffect(activeProfile.id, profiles) {
                        val selectedIndex = profiles.indexOfFirst { it.id == activeProfile.id }
                        if (selectedIndex >= 0 && dockScrollState.maxValue > 0) {
                            val itemPx = 70.dp.value * 2.5f // approximate item width + spacing
                            val targetScroll = (selectedIndex * itemPx - 100f).coerceIn(0f, dockScrollState.maxValue.toFloat()).toInt()
                            if (reduceMotion) {
                                dockScrollState.scrollTo(targetScroll)
                            } else {
                                dockScrollState.animateScrollTo(targetScroll)
                            }
                        }
                    }

                    // Horizontal dock container with soft fade at edges when scrolling
                    val fadeBgColor = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
                    Box(
                        modifier = Modifier
                            .height(76.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(ProfileGlassTokens.GlassFill)
                            .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(38.dp))
                            .drawWithContent {
                                drawContent()
                                if (dockScrollState.maxValue > 0) {
                                    val fadeW = 20.dp.toPx()
                                    if (dockScrollState.value > 0) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to fadeBgColor.copy(alpha = 0.80f),
                                                1f to Color.Transparent,
                                                startX = 0f,
                                                endX = fadeW
                                            )
                                        )
                                    }
                                    if (dockScrollState.value < dockScrollState.maxValue) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                1f to fadeBgColor.copy(alpha = 0.80f),
                                                startX = size.width - fadeW,
                                                endX = size.width
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(dockScrollState),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            profiles.forEach { profile ->
                                val isSelected = profile.id == activeProfile.id

                                val avatarOpacity by animateFloatAsState(
                                    targetValue = if (isSelected) 1.0f else 0.70f,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "dockAvatarOpacity_${profile.id}"
                                )

                                // Each profile avatar: 46 dp inside 56 dp ring slot
                                // Selected has solid 2 dp white ring with 2 dp gap (outer size 54-56 dp)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .semantics {
                                            this.role = Role.Tab
                                            this.selected = isSelected
                                            this.contentDescription = "${profile.name}, ${if (isSelected) "selected" else "not selected"}"
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            role = Role.Tab,
                                            onClick = {
                                                if (!isSelected) {
                                                    try {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } catch (_: Exception) {}
                                                    if (profile.isPinLocked) {
                                                        pendingUnlockProfile = profile
                                                    } else {
                                                        profileManager.unlockProfile(profile.id)
                                                        profileManager.switchProfile(profile.id)
                                                    }
                                                }
                                            }
                                        )
                                        .tvFocusable(shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier.alpha(avatarOpacity),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AvatarWithThinRing(
                                            avatarId = profile.avatarId,
                                            avatarSize = 46.dp,
                                            ringGap = 2.dp,
                                            ringBorderWidth = 0.5.dp,
                                            ringColor = Color.Transparent,
                                            isSelected = isSelected
                                        )

                                        // Outline lock indicator for locked profiles
                                        if (profile.isPinLocked) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(ProfileGlassTokens.BaseDarkBackground)
                                                    .border(ProfileGlassTokens.HairlineWidthDp, Color.White.copy(alpha = 0.25f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Lock,
                                                    contentDescription = "Locked",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add Profile Button: 56 dp circle with 1 dp dashed hairline border (white ~30%) and outline plus icon
                            if (profiles.size < 5) {
                                DashedCircleButton(
                                    size = 56.dp,
                                    iconSize = 22.dp,
                                    contentDescription = "Add Profile",
                                    onClick = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {}
                                        editingProfile = null
                                        isCreatingNew = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Screen 3: New Profile / Edit Profile Sheet
    if (isCreatingNew || editingProfile != null) {
        ProfileStudioSheet(
            existingProfile = editingProfile,
            canDelete = editingProfile != null && profiles.size > 1 &&
                    editingProfile?.type != ProfileType.ADMIN &&
                    editingProfile?.id != "default_user" &&
                    editingProfile?.id != "primary_profile",
            onDismiss = {
                editingProfile = null
                isCreatingNew = false
            },
            onSave = { name, avatarId, role, isKids, plainPin, pinHash ->
                if (editingProfile != null) {
                    val updated = editingProfile!!.copy(
                        name = name,
                        avatarId = avatarId,
                        avatarPresetId = avatarId,
                        type = role,
                        isKids = isKids,
                        pin = plainPin,
                        pinHash = pinHash,
                        isLocked = !pinHash.isNullOrBlank() || !plainPin.isNullOrBlank()
                    )
                    profileManager.updateProfile(updated)
                } else {
                    profileManager.addProfile(
                        name = name,
                        avatarPresetId = avatarId,
                        pin = plainPin,
                        pinHash = pinHash,
                        isKids = isKids,
                        type = role
                    )
                }
                editingProfile = null
                isCreatingNew = false
            },
            onDelete = {
                editingProfile?.let { prof ->
                    profileManager.deleteProfile(prof.id)
                }
                editingProfile = null
                isCreatingNew = false
            }
        )
    }

    // PIN Challenge Dialog for switching into a locked profile
    pendingUnlockProfile?.let { targetProfile ->
        PinChallengeDialog(
            profile = targetProfile,
            onDismissRequest = { pendingUnlockProfile = null },
            onSuccess = {
                profileManager.unlockProfile(targetProfile.id)
                profileManager.switchProfile(targetProfile.id)
                pendingUnlockProfile = null
            }
        )
    }

    // Delete Confirmation Dialog
    profileToDelete?.let { profile ->
        GlassConfirmDialog(
            title = "Delete this profile?",
            message = "Are you sure you want to delete profile \"${profile.name}\"? All history and watchlists associated with this profile will be permanently removed.",
            confirmText = "Delete",
            cancelText = "Keep",
            isDestructive = true,
            onConfirm = {
                profileManager.deleteProfile(profile.id)
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null }
        )
    }
}
