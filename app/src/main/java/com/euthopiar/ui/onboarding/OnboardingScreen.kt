package com.euthopiar.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.ui.icons.EuthopiarIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.preferences.CardScaleMode
import com.euthopiar.data.preferences.NavbarStyle
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.profile.CustomAvatarRepository
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.defaultAvatarPresets
import com.euthopiar.ui.components.DropdownOption
import com.euthopiar.ui.components.GlassDropdownRow
import com.euthopiar.ui.components.OnboardingStepHeader
import com.euthopiar.ui.components.PrimaryButton
import com.euthopiar.ui.profile.DetroitAvatarUploadDialog
import com.euthopiar.ui.profile.DetroitCircleCropDialog
import com.euthopiar.ui.profile.DetroitPinDialog
import com.euthopiar.ui.profile.ImageCropUtils
import com.euthopiar.ui.profile.ProfileAvatarImage
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Cinematic Sanctuary Gateway (Onboarding Flow).
 * Organized into a 3-Act interactive experience:
 * - Act I: The Sanctuary (Drifting Marquee with seamless DstIn feathering & manifesto)
 * - Act II: The Persona (Detroit Profile Creator — creates user's main profile)
 * - Act III: The Canvas (Live interactive canvas preview of navigation & card density)
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val profileManager = remember { ProfileManager.getInstance(context) }
    val preferencesRepository = remember { PreferencesRepository.getInstance(context) }

    val initialNavbarStyle by preferencesRepository.navbarStyle.collectAsState(initial = NavbarStyle.FLOATING)
    val initialCardScale by preferencesRepository.cardScaleMode.collectAsState(initial = CardScaleMode.FLAGSHIP)

    // Act 2 Profile Creator State
    var profileName by remember { mutableStateOf("") }
    var profileAvatarId by remember { mutableStateOf("avatar_1") }
    var profileType by remember { mutableStateOf(ProfileType.STANDARD) }
    var profileIsLocked by remember { mutableStateOf(false) }
    var profilePin by remember { mutableStateOf<String?>(null) }

    // Act 3 Canvas State
    var selectedNavbarStyle by remember(initialNavbarStyle) { mutableStateOf(initialNavbarStyle) }
    var selectedCardScale by remember(initialCardScale) { mutableStateOf(initialCardScale) }

    fun finishOnboarding() {
        coroutineScope.launch {
            // 1. Commit created profile as the main primary profile
            profileManager.setInitialPrimaryProfile(
                name = profileName.trim().ifEmpty { "Cinephile" },
                avatarId = profileAvatarId,
                type = profileType,
                isLocked = profileIsLocked,
                pin = profilePin,
            )

            // 2. Persist chosen layout settings
            preferencesRepository.setNavbarStyle(selectedNavbarStyle)
            preferencesRepository.setCardScaleMode(selectedCardScale)

            // 3. Mark first run completed in DataStore
            preferencesRepository.setFirstRunCompleted(true)

            // 4. Synchronous backup commit in SharedPreferences
            withContext(Dispatchers.IO) {
                context.getSharedPreferences("euthopiar_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_run_completed", true)
                    .commit()
            }

            onComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onboardingTwilightBackground()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Navigation Bar
        if (pagerState.currentPage < 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Back button (on pages > 0) or brand icon
                Box(
                    modifier = Modifier.sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (pagerState.currentPage > 0) {
                        Row(
                            modifier = Modifier
                                .clip(ShapeFull)
                                .pressable(
                                    onClickLabel = "Back",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Back",
                                style = AppTypography.labelLarge.copy(fontSize = 13.sp),
                                color = TextSecondary,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Center: Liquid Jelly Pager Indicator
                OnboardingJellyIndicator(
                    pagerState = pagerState,
                    pageCount = 3,
                )

                // Right: Skip button
                Box(
                    modifier = Modifier.sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(ShapeFull)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), ShapeFull)
                            .pressable(
                                onClickLabel = "Skip onboarding",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    finishOnboarding()
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Skip",
                            style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            when (pageIndex) {
                0 -> Act1TheSanctuary(
                    onBeginJourney = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                )

                1 -> Act2DetroitProfileCreator(
                    profileName = profileName,
                    onProfileNameChange = { profileName = it },
                    avatarId = profileAvatarId,
                    onAvatarIdChange = { profileAvatarId = it },
                    type = profileType,
                    onTypeChange = { profileType = it },
                    isLocked = profileIsLocked,
                    pin = profilePin,
                    onLockToggled = { locked, newPin ->
                        profileIsLocked = locked
                        profilePin = newPin
                    },
                    onContinue = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                )

                2 -> Act3TheCanvas(
                    selectedNavbarStyle = selectedNavbarStyle,
                    selectedCardScale = selectedCardScale,
                    onNavbarStyleChanged = { selectedNavbarStyle = it },
                    onCardScaleChanged = { selectedCardScale = it },
                    onBack = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onSkip = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        finishOnboarding()
                    },
                    onEnterSanctuary = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        finishOnboarding()
                    },
                )
            }
        }
    }
}

/**
 * Act 1: The Sanctuary — Drifting cinema poster marquee, high-impact wordmark, and manifesto.
 */
@Composable
private fun Act1TheSanctuary(
    onBeginJourney: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingPosterMarquee(
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        // Brand Narrative Lockup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(ShapeFull)
                    .background(Accent.copy(alpha = 0.14f))
                    .border(1.dp, Accent.copy(alpha = 0.35f), ShapeFull)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "CINEMATIC SANCTUARY",
                    style = AppTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Accent,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "EUTHOPIAR",
                style = AppTypography.displayHero.copy(
                    fontSize = 34.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your private sanctuary for cinema and series. Unified universal providers, seamless failover, and uncompromising presentation.",
                style = AppTypography.bodyMedium.copy(
                    lineHeight = 21.sp,
                    fontSize = 14.sp,
                ),
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Begin Journey",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                isAccentRed = true,
                onClick = onBeginJourney,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Act 2: Profile Creator ported from Detroit.
 * Creates the user's primary main profile rather than a prepackaged profile.
 */
@Composable
private fun Act2DetroitProfileCreator(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    avatarId: String,
    onAvatarIdChange: (String) -> Unit,
    type: ProfileType,
    onTypeChange: (ProfileType) -> Unit,
    isLocked: Boolean,
    pin: String?,
    onLockToggled: (Boolean, String?) -> Unit,
    onContinue: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = ImageCropUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                pendingCropBitmap = bitmap
            }
        }
    }

    val cardBgColor = Color(0xFF13141B)
    val inputBgColor = Color(0xFF1A1B24)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Section Badge
        Box(
            modifier = Modifier
                .clip(ShapeFull)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), ShapeFull)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "ACT II • YOUR PROFILE",
                style = AppTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Create Your Profile",
            style = AppTypography.titleLarge.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "This will be your primary account for personal watchlists and recommendations.",
            style = AppTypography.bodyMedium.copy(fontSize = 14.sp),
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Profile Creator Container Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBgColor)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top: Name field on left, Live Preview on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "PROFILE NAME",
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp,
                                color = TextSecondary,
                            ),
                        )

                        BasicTextField(
                            value = profileName,
                            onValueChange = onProfileNameChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(Accent),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(inputBgColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (profileName.isEmpty()) {
                                        Text(
                                            text = "Enter name (e.g. Alex)",
                                            style = androidx.compose.ui.text.TextStyle(
                                                color = TextSecondary.copy(alpha = 0.6f),
                                                fontSize = 15.sp,
                                            ),
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Live Preview
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ProfileAvatarImage(
                            avatarId = avatarId,
                            size = 56.dp,
                            borderColor = Accent,
                            borderWidth = 2.dp,
                        )

                        Text(
                            text = if (profileName.isBlank()) "Enter name" else profileName.trim(),
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (profileName.isBlank()) TextSecondary else TextPrimary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(72.dp),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isLocked) "Locked" else "Unlocked",
                                tint = if (isLocked) Accent else TextSecondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (isLocked) "Locked" else "Open",
                                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                color = TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Presets (8 Presets)
                Text(
                    text = "SELECT AVATAR (8 PRESETS)",
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary,
                    ),
                )

                Spacer(modifier = Modifier.height(10.dp))

                val avatarGrid = remember { defaultAvatarPresets.chunked(4) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    avatarGrid.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = preset.id == avatarId
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable { onAvatarIdChange(preset.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ProfileAvatarImage(
                                        avatarId = preset.id,
                                        size = 52.dp,
                                        borderColor = if (isSelected) Accent else Color.Transparent,
                                        borderWidth = 2.5.dp,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isCustomSelected = avatarId.startsWith("custom:")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable { showUploadDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCustomSelected) {
                                ProfileAvatarImage(
                                    avatarId = avatarId,
                                    size = 52.dp,
                                    borderColor = Accent,
                                    borderWidth = 2.5.dp,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(inputBgColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Upload Avatar",
                                        tint = Accent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Upload",
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isCustomSelected) Accent else TextSecondary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                // Role Selector (Chips)
                Text(
                    text = "PROFILE ROLE",
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileType.entries.forEach { pType ->
                        val isSelected = type == pType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Accent.copy(alpha = 0.18f) else inputBgColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Accent else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onTypeChange(pType) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = pType.displayName,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Accent else TextPrimary,
                                ),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBgColor)
                        .clickable {
                            if (!isLocked) {
                                showPinDialog = true
                            } else {
                                onLockToggled(false, null)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isLocked) Accent else TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Profile PIN Lock",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                ),
                            )
                            Text(
                                text = if (isLocked) "Protected with 4-digit PIN" else "Prompt for 4-digit PIN on opening",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                ),
                            )
                        }
                    }

                    Switch(
                        checked = isLocked,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPinDialog = true
                            } else {
                                onLockToggled(false, null)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF2A2B38),
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Continue to Canvas",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showPinDialog) {
        DetroitPinDialog(
            onDismissRequest = { showPinDialog = false },
            onPinConfirmed = { confirmedPin ->
                onLockToggled(true, confirmedPin)
                showPinDialog = false
            },
        )
    }

    if (showUploadDialog) {
        DetroitAvatarUploadDialog(
            currentAvatarId = avatarId,
            onDismissRequest = { showUploadDialog = false },
            onAvatarSelected = { selectedCustomId ->
                onAvatarIdChange(selectedCustomId)
                showUploadDialog = false
            },
            onChooseFromGallery = {
                showUploadDialog = false
                galleryPicker.launch("image/*")
            },
        )
    }

    pendingCropBitmap?.let { bmp ->
        DetroitCircleCropDialog(
            sourceBitmap = bmp,
            onDismissRequest = { pendingCropBitmap = null },
            onCropConfirmed = { cropped ->
                val savedId = CustomAvatarRepository.saveCroppedAvatar(context, cropped)
                onAvatarIdChange(savedId)
                pendingCropBitmap = null
            },
        )
    }
}

/**
 * Act 3: The Canvas — Interface navigation style & poster density tailoring
 * with Live Interactive Preview Canvas.
 */
@Composable
private fun Act3TheCanvas(
    selectedNavbarStyle: NavbarStyle,
    selectedCardScale: CardScaleMode,
    onNavbarStyleChanged: (NavbarStyle) -> Unit,
    onCardScaleChanged: (CardScaleMode) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onEnterSanctuary: () -> Unit,
) {
    val navbarOptions = remember {
        listOf(
            DropdownOption(
                value = NavbarStyle.FLOATING,
                title = "Floating Glass Pill",
                subtitle = "Frosted acrylic dock suspended above bottom with fluid motion",
            ),
            DropdownOption(
                value = NavbarStyle.DOCKED,
                title = "Docked Edge Bar",
                subtitle = "Classic edge-to-edge docked navigation bar",
            ),
        )
    }

    val posterOptions = remember {
        listOf(
            DropdownOption(
                value = CardScaleMode.FLAGSHIP,
                title = "Flagship Cinema",
                subtitle = "Cinematic 2.0x poster scale with rich breathing room",
                dividerBefore = false,
            ),
            DropdownOption(
                value = CardScaleMode.BALANCED,
                title = "Balanced",
                subtitle = "1.5x Vertical, 1.25x Horizontal cards",
                dividerBefore = false,
            ),
            DropdownOption(
                value = CardScaleMode.COMPACT,
                title = "Compact Grid",
                subtitle = "Balanced 1.0x layout for maximum catalog density",
                dividerBefore = false,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // 1. OnboardingStepHeader replacing old three-dot indicator and section chip
        OnboardingStepHeader(
            step = 3,
            total = 3,
            onBack = onBack,
            onSkip = onSkip,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Title & Subtitle
        Text(
            text = "Craft Your Canvas",
            style = AppTypography.titleLarge.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Personalize your layout and view live changes in the canvas preview.",
            style = AppTypography.bodyMedium.copy(fontSize = 14.sp),
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Pinned Live Interactive Canvas Preview
        LiveCanvasPreviewBox(
            navbarStyle = selectedNavbarStyle,
            cardScale = selectedCardScale,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Dropdown rows column (scrolls if needed, preview & header stay pinned)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            GlassDropdownRow(
                label = "Navigation",
                description = "How the bar sits",
                options = navbarOptions,
                selected = selectedNavbarStyle,
                onSelected = onNavbarStyleChanged,
                showTopDivider = false,
            )

            GlassDropdownRow(
                label = "Posters",
                description = "Size and density",
                options = posterOptions,
                selected = selectedCardScale,
                onSelected = onCardScaleChanged,
                showTopDivider = true,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Final CTA Button
        PrimaryButton(
            text = "Enter Euthopiar",
            icon = Icons.Rounded.Check,
            isAccentRed = true,
            onClick = onEnterSanctuary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * Legacy radio-list option composables preserved in-file to satisfy non-deletion requirements.
 */
@Suppress("unused")
@Composable
private fun LegacyAct3RadioChoiceOptions(
    selectedNavbarStyle: NavbarStyle,
    selectedCardScale: CardScaleMode,
    onNavbarStyleChanged: (NavbarStyle) -> Unit,
    onCardScaleChanged: (CardScaleMode) -> Unit,
) {
    OnboardingChoiceCard(
        title = "Floating Glass Pill",
        subtitle = "Frosted acrylic dock suspended above bottom with fluid motion",
        icon = Icons.Rounded.ViewStream,
        isSelected = selectedNavbarStyle == NavbarStyle.FLOATING,
        onClick = { onNavbarStyleChanged(NavbarStyle.FLOATING) },
    )
    OnboardingChoiceCard(
        title = "Docked Edge Bar",
        subtitle = "Classic edge-to-edge docked navigation bar",
        icon = Icons.Rounded.Dashboard,
        isSelected = selectedNavbarStyle == NavbarStyle.DOCKED,
        onClick = { onNavbarStyleChanged(NavbarStyle.DOCKED) },
    )
    OnboardingChoiceCard(
        title = "Flagship Cinema",
        subtitle = "Cinematic 2.0x poster scale with rich breathing room",
        icon = Icons.Rounded.ViewAgenda,
        isSelected = selectedCardScale == CardScaleMode.FLAGSHIP,
        onClick = { onCardScaleChanged(CardScaleMode.FLAGSHIP) },
    )
    OnboardingChoiceCard(
        title = "Compact Grid",
        subtitle = "Balanced 1.0x layout for maximum catalog density",
        icon = Icons.Rounded.GridView,
        isSelected = selectedCardScale == CardScaleMode.COMPACT,
        onClick = { onCardScaleChanged(CardScaleMode.COMPACT) },
    )
}

/**
 * Live interactive canvas preview mockup reflecting navbar style and poster density dynamically
 * with real poster cards in their real sizes and the exact real bottom navigation bars.
 */
@Composable
private fun LiveCanvasPreviewBox(
    navbarStyle: NavbarStyle,
    cardScale: CardScaleMode,
) {
    val context = LocalContext.current
    val isFlagship = cardScale == CardScaleMode.FLAGSHIP
    val isFloating = navbarStyle == NavbarStyle.FLOATING

    val animatedPosterWidth by animateDpAsState(
        targetValue = if (isFlagship) 140.dp else 100.dp,
        animationSpec = spring(),
        label = "previewPosterWidth",
    )
    val animatedPosterHeight by animateDpAsState(
        targetValue = if (isFlagship) 208.dp else 148.dp,
        animationSpec = spring(),
        label = "previewPosterHeight",
    )
    val animatedContainerHeight by animateDpAsState(
        targetValue = if (isFlagship) 330.dp else 272.dp,
        animationSpec = spring(),
        label = "previewContainerHeight",
    )

    val animatedNavbarRadius by animateDpAsState(
        targetValue = if (isFloating) 28.dp else 0.dp,
        animationSpec = spring(),
        label = "previewNavRadius",
    )
    val animatedNavbarMargin by animateDpAsState(
        targetValue = if (isFloating) 16.dp else 0.dp,
        animationSpec = spring(),
        label = "previewNavMargin",
    )
    val animatedNavbarBottomPadding by animateDpAsState(
        targetValue = if (isFloating) 8.dp else 0.dp,
        animationSpec = spring(),
        label = "previewNavBottom",
    )

    val previewItems = remember {
        (ShowcaseRowTop + ShowcaseRowBottom).distinctBy { it.title }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedContainerHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0C0D12))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Live Header with Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Accent),
                    )
                    Text(
                        text = "LIVE CANVAS PREVIEW",
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        ),
                    )
                }

                Text(
                    text = "${if (isFlagship) "2.0x Flagship" else "1.0x Compact"} • ${if (isFloating) "Floating Pill" else "Docked Bar"}",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Accent,
                    ),
                )
            }

            // Real Content Cards in Real Dimensions (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                previewItems.forEachIndexed { idx, item ->
                    Box(
                        modifier = Modifier
                            .size(width = animatedPosterWidth, height = animatedPosterHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14151E))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                    ) {
                        // Offline Fallback
                        Image(
                            painter = painterResource(id = item.fallbackDrawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // TMDB Real Poster
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.posterUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Top Specular Sheen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedPosterHeight * 0.30f)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        // 4K Ultra HD Badge (on first 2 items)
                        if (idx < 2) {
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Accent)
                                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    .align(Alignment.TopStart),
                            ) {
                                Text(
                                    text = "4K HDR",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        // Bottom Title & Metadata Gradient
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF0B0B0F).copy(alpha = 0.94f),
                                        ),
                                    ),
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = item.title,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = if (isFlagship) 11.sp else 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.genre} • ${item.year}",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = if (isFlagship) 9.sp else 8.sp,
                                    color = Color.White.copy(alpha = 0.55f),
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exact Real Navbar Mockup (Floating Glass Pill vs Docked Edge Bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = animatedNavbarMargin)
                    .padding(bottom = animatedNavbarBottomPadding)
                    .height(52.dp)
                    .clip(RoundedCornerShape(animatedNavbarRadius))
                    .background(
                        if (isFloating) Color(0xFF161822).copy(alpha = 0.92f) else Color(0xFF0F1017)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFloating) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(animatedNavbarRadius),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. Home (Selected)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = EuthopiarIcons.Home.filled,
                            contentDescription = "Home",
                            tint = Accent,
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = "Home",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent,
                            ),
                        )
                    }

                    // 2. Search
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = EuthopiarIcons.Search.outlined,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = "Search",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            ),
                        )
                    }

                    // 3. Library
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = EuthopiarIcons.Library.outlined,
                            contentDescription = "Library",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = "Library",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            ),
                        )
                    }

                    // 4. Profile
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = EuthopiarIcons.Profile.outlined,
                            contentDescription = "Profile",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = "Profile",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
