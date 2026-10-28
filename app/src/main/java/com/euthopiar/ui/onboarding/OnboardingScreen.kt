package com.euthopiar.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import com.euthopiar.core.model.ContentRegionPreset
import com.euthopiar.core.util.TmdbBridge
import com.euthopiar.data.preferences.CardScaleMode
import com.euthopiar.data.preferences.NavbarStyle
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.profile.CustomAvatarRepository
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.defaultAvatarPresets
import com.euthopiar.data.profile.hashPin
import com.euthopiar.ui.icons.EuthopiarIcons
import com.euthopiar.ui.profile.AvatarWithThinRing
import com.euthopiar.ui.profile.DashedCircleButton
import com.euthopiar.ui.profile.DetroitCircleCropDialog
import com.euthopiar.ui.profile.ImageCropUtils
import com.euthopiar.ui.profile.PinCreatorDialog
import com.euthopiar.ui.profile.ProfileGlassChip
import com.euthopiar.ui.profile.ProfileGlassPill
import com.euthopiar.ui.profile.ProfileGlassTokens
import com.euthopiar.ui.profile.ProfileGlassToggle
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.withContext

/**
 * Redesigned 3-Page First-Launch Onboarding Flow for Euthopiar.
 *
 * Page 1: Welcome (Edge-to-edge drifting poster collage + manifesto + "Get started" pill)
 * Page 2: Profile Creation (Full-screen Settings profile component)
 * Page 3: Customize (Live preview card + mini navbar + Posters & Appearance segmented controls + "Enter Euthopiar" pill)
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val postersState by viewModel.postersState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val isSystemAmoled = LocalAmoledMode.current

    val profileManager = remember { ProfileManager.getInstance(context) }
    val preferencesRepository = remember { PreferencesRepository.getInstance(context) }

    val initialCardScale by preferencesRepository.cardScaleMode.collectAsState(initial = CardScaleMode.FLAGSHIP)
    val preferenceAmoled by preferencesRepository.amoledMode.collectAsState(initial = isSystemAmoled)

    // Current page: 0 = Welcome, 1 = Profile, 2 = Customize
    var currentPage by remember { mutableIntStateOf(0) }
    var previousPage by remember { mutableIntStateOf(0) }

    // Page 2: Profile Creation State
    var profileName by remember { mutableStateOf("") }
    var profileAvatarId by remember { mutableStateOf("avatar_1") }
    var profileRole by remember { mutableStateOf(ProfileType.STANDARD) }
    var isKidsProfile by remember { mutableStateOf(false) }
    var profilePinHash by remember { mutableStateOf<String?>(null) }
    var profilePlainPin by remember { mutableStateOf<String?>(null) }

    val initialRegionPreset by preferencesRepository.contentRegionPreset.collectAsState(initial = ContentRegionPreset.GLOBAL)
    var selectedRegionPreset by remember(initialRegionPreset) { mutableStateOf(initialRegionPreset) }

    // Page 3: Customize State
    var selectedCardScale by remember(initialCardScale) { mutableStateOf(initialCardScale) }
    var isAmoledActive by remember(preferenceAmoled) { mutableStateOf(preferenceAmoled) }

    var isSubmitting by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    // Debounced action runner to prevent rapid double-clicks
    var isActionInProgress by remember { mutableStateOf(false) }
    fun executeDebounced(action: () -> Unit) {
        if (isActionInProgress || isSubmitting) return
        isActionInProgress = true
        action()
        coroutineScope.launch {
            kotlinx.coroutines.delay(350)
            isActionInProgress = false
        }
    }

    // Persist and finish onboarding
    fun finishOnboarding() {
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            // 1. Commit created profile as the primary profile
            profileManager.setInitialPrimaryProfile(
                name = profileName.trim().ifEmpty { "Cinephile" },
                avatarId = profileAvatarId,
                type = if (isKidsProfile) ProfileType.KIDS else profileRole,
                isLocked = !profilePinHash.isNullOrBlank() || !profilePlainPin.isNullOrBlank(),
                pin = profilePlainPin,
            )

            // 2. Persist chosen layout settings (Navbar style silently defaults to Floating)
            preferencesRepository.setNavbarStyle(NavbarStyle.FLOATING)
            preferencesRepository.setCardScaleMode(selectedCardScale)
            preferencesRepository.setAmoledMode(isAmoledActive)
            preferencesRepository.setContentRegionPreset(selectedRegionPreset)
            TmdbBridge.activeRegionPreset = selectedRegionPreset

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

    fun navigateToPage(page: Int) {
        if (page != currentPage) {
            previousPage = currentPage
            currentPage = page
        }
    }

    // System Back handling: go back one step if on page 1 or 2
    BackHandler(enabled = currentPage > 0) {
        if (currentPage > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            navigateToPage(currentPage - 1)
        }
    }

    // Base background with atmospheric haze blobs (or pure #000000 with haze OFF in AMOLED)
    Box(
        modifier = modifier
            .fillMaxSize()
            .onboardingBackground(isAmoled = isAmoledActive)
            .hazeSource(state = hazeState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // SHARED SCAFFOLD TOP: Progress Bar (8 dp below inset, 14 dp above top row)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.widthIn(max = 480.dp)) {
                    OnboardingProgressBar(
                        currentStep = currentPage + 1,
                        totalSteps = 3,
                    )
                }
            }

            // SHARED SCAFFOLD TOP: Top Row (40 dp tall: Back circle & Skip chip)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.widthIn(max = 480.dp)) {
                    OnboardingTopRow(
                        showBackButton = currentPage > 0,
                        onBack = {
                            executeDebounced {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navigateToPage(currentPage - 1)
                            }
                        },
                        onSkip = {
                            executeDebounced {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                finishOnboarding()
                            }
                        },
                        hazeState = hazeState,
                        isAmoled = isAmoledActive,
                    )
                }
            }

            // PAGE CONTENT WITH SLIDE & FADE TRANSITION
            val movingForward = currentPage >= previousPage
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(100))
                    } else {
                        val slideDistance = 24
                        if (movingForward) {
                            (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { slideDistance })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { -slideDistance }
                                )
                        } else {
                            (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -slideDistance })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { slideDistance }
                                )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboardingPageTransition",
            ) { targetPage ->
                when (targetPage) {
                    0 -> OnboardingPage1Welcome(
                        isActive = currentPage == 0,
                        topRowPosters = postersState.topRowPosters,
                        bottomRowPosters = postersState.bottomRowPosters,
                        isLoading = postersState.isLoading,
                        onGetStarted = {
                            executeDebounced {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navigateToPage(1)
                            }
                        },
                    )

                    1 -> OnboardingPage2Profile(
                        profileName = profileName,
                        onProfileNameChange = { profileName = it },
                        avatarId = profileAvatarId,
                        onAvatarIdChange = { profileAvatarId = it },
                        role = profileRole,
                        onRoleChange = { profileRole = it },
                        isKids = isKidsProfile,
                        onKidsChange = { isKidsProfile = it },
                        pinHash = profilePinHash,
                        plainPin = profilePlainPin,
                        onPinChange = { hash, plain ->
                            profilePinHash = hash
                            profilePlainPin = plain
                        },
                        onContinue = {
                            executeDebounced {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navigateToPage(2)
                            }
                        },
                        hazeState = hazeState,
                        isAmoled = isAmoledActive,
                    )

                    2 -> OnboardingPage3Customize(
                        selectedCardScale = selectedCardScale,
                        onCardScaleChanged = { selectedCardScale = it },
                        isAmoled = isAmoledActive,
                        onAmoledChanged = { enabled ->
                            isAmoledActive = enabled
                            coroutineScope.launch {
                                preferencesRepository.setAmoledMode(enabled)
                            }
                        },
                        selectedRegionPreset = selectedRegionPreset,
                        onRegionPresetChanged = { selectedRegionPreset = it },
                        isSubmitting = isSubmitting,
                        previewItems = postersState.previewPosters,
                        isLoadingPosters = postersState.isLoading,
                        onEnterSanctuary = {
                            executeDebounced {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                finishOnboarding()
                            }
                        },
                        hazeState = hazeState,
                    )
                }
            }
        }
    }
}

/**
 * PAGE 1: WELCOME
 * Full-bleed poster collage (304 dp) + text block overlapping by ~40 dp + 52 dp white pill "Get started".
 */
@Composable
private fun OnboardingPage1Welcome(
    isActive: Boolean,
    topRowPosters: List<ShowcasePosterItem> = ShowcaseRowTop,
    bottomRowPosters: List<ShowcasePosterItem> = ShowcaseRowBottom,
    isLoading: Boolean = false,
    onGetStarted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Full-bleed edge-to-edge poster collage
        OnboardingPosterMarquee(
            modifier = Modifier.fillMaxWidth(),
            topRowItems = topRowPosters,
            bottomRowItems = bottomRowPosters,
            isLoading = isLoading,
            isActive = isActive,
        )

        // Text block + CTA (centered with max width 480 dp on large displays)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                // Small label above headline
                Text(
                    text = "WELCOME",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.12.sp,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Big headline: 40 sp Medium, line height 1.0, letter spacing -0.03 em, 2 lines
                Text(
                    text = "Your private\ncinema.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.03).sp,
                        color = Color.White,
                    ),
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Truthful, neutral descriptive copy under 110 characters
                Text(
                    text = "Movies, series, and anime from all your sources, with automatic fallback when one goes down.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = OnboardingTokens.TextMuted55,
                    ),
                    modifier = Modifier.widthIn(max = 280.dp),
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Primary White Pill: 52 dp tall with outline right-arrow icon
                OnboardingWhitePillButton(
                    text = "Get started",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * PAGE 2: PROFILE CREATION
 * Full-screen profile creator reusing the exact fields, styling, validation, and crop/PIN dialogs
 * from the Settings Profile Studio experience.
 */
@Composable
private fun OnboardingPage2Profile(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    avatarId: String,
    onAvatarIdChange: (String) -> Unit,
    role: ProfileType,
    onRoleChange: (ProfileType) -> Unit,
    isKids: Boolean,
    onKidsChange: (Boolean) -> Unit,
    pinHash: String?,
    plainPin: String?,
    onPinChange: (String?, String?) -> Unit,
    onContinue: () -> Unit,
    hazeState: HazeState,
    isAmoled: Boolean,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val reduceMotion = LocalReduceMotion.current

    var validationError by remember { mutableStateOf<String?>(null) }
    var isNameFocused by remember { mutableStateOf(false) }
    var showPinCreator by remember { mutableStateOf(false) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    val isPinSet = !pinHash.isNullOrBlank() || !plainPin.isNullOrBlank()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = ImageCropUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                pendingCropBitmap = bitmap
            }
        }
    }

    fun validateAndProceed() {
        val trimmed = profileName.trim()
        if (trimmed.isEmpty()) {
            validationError = "Enter a name"
            return
        }
        validationError = null
        focusManager.clearFocus()
        keyboardController?.hide()
        onContinue()
    }

    val pageBgColor = if (isAmoled) OnboardingTokens.AmoledBackground else OnboardingTokens.DarkBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Small label above headline
                Text(
                    text = "STEP 2",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.12.sp,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Main Headline: 40 sp Medium
                Text(
                    text = "Your profile.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.03).sp,
                        color = Color.White,
                    ),
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pick an avatar and a name for your personal watchlists.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = OnboardingTokens.TextMuted55,
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Live Preview Row (Avatar with thin ring + live name + chip)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarWithThinRing(
                        avatarId = avatarId,
                        avatarSize = 62.dp,
                        ringGap = 5.dp,
                        ringBorderWidth = 0.5.dp,
                        ringColor = Color.White.copy(alpha = 0.16f),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (profileName.isBlank()) "Profile name" else profileName.trim(),
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp,
                                lineHeight = 24.sp,
                                color = if (profileName.isBlank()) Color.White.copy(alpha = 0.40f) else Color.White,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ProfileGlassChip(
                            text = if (isKids) "Kids" else role.displayName,
                            showLockIcon = isPinSet,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Avatar Picker Section
                Text(
                    text = "CHOOSE AVATAR",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.08.sp,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                Spacer(modifier = Modifier.height(14.dp))

                val allAvatarItems = defaultAvatarPresets
                val isCustomAvatarSelected = avatarId.startsWith("custom:")

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Row 1: First 5 Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until minOf(5, allAvatarItems.size)) {
                            val preset = allAvatarItems[i]
                            val isSelected = avatarId == preset.id
                            Box(
                                modifier = Modifier.size(52.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AvatarWithThinRing(
                                    avatarId = preset.id,
                                    avatarSize = 48.dp,
                                    ringGap = 2.dp,
                                    isSelected = isSelected,
                                    showSelectedCheck = true,
                                    isAvatarOption = true,
                                    onClick = {
                                        onAvatarIdChange(preset.id)
                                        validationError = null
                                    },
                                )
                            }
                        }
                    }

                    // Row 2: Remaining presets + custom photo tile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 5 until allAvatarItems.size) {
                            val preset = allAvatarItems[i]
                            val isSelected = avatarId == preset.id
                            Box(
                                modifier = Modifier.size(52.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AvatarWithThinRing(
                                    avatarId = preset.id,
                                    avatarSize = 48.dp,
                                    ringGap = 2.dp,
                                    isSelected = isSelected,
                                    showSelectedCheck = true,
                                    isAvatarOption = true,
                                    onClick = {
                                        onAvatarIdChange(preset.id)
                                        validationError = null
                                    },
                                )
                            }
                        }

                        // Custom Photo Upload Tile
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCustomAvatarSelected) {
                                AvatarWithThinRing(
                                    avatarId = avatarId,
                                    avatarSize = 48.dp,
                                    ringGap = 2.dp,
                                    isSelected = true,
                                    showSelectedCheck = true,
                                    isAvatarOption = true,
                                    onClick = { galleryLauncher.launch("image/*") },
                                )
                            } else {
                                DashedCircleButton(
                                    size = 48.dp,
                                    iconSize = 20.dp,
                                    contentDescription = "Upload custom photo",
                                    onClick = { galleryLauncher.launch("image/*") },
                                )
                            }
                        }

                        // 5th Slot spacer for alignment
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCustomAvatarSelected) {
                                DashedCircleButton(
                                    size = 48.dp,
                                    iconSize = 20.dp,
                                    contentDescription = "Upload new custom photo",
                                    onClick = { galleryLauncher.launch("image/*") },
                                )
                            } else {
                                Spacer(modifier = Modifier.size(52.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Profile Name Input Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "PROFILE NAME",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            letterSpacing = 0.08.sp,
                            color = OnboardingTokens.TextMuted45,
                        ),
                    )

                    Text(
                        text = "${profileName.length}/20",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = OnboardingTokens.TextMuted45,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val fieldBorderColor = when {
                    validationError != null -> Color.White.copy(alpha = 0.60f)
                    isNameFocused -> Color.White.copy(alpha = 0.30f)
                    else -> OnboardingTokens.GlassHairlineBorder
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OnboardingTokens.GlassFill)
                        .border(OnboardingTokens.HairlineWidth, fieldBorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (profileName.isEmpty()) {
                                Text(
                                    text = "Living Room",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.40f),
                                    ),
                                )
                            }
                            BasicTextField(
                                value = profileName,
                                onValueChange = {
                                    if (it.length <= 20) {
                                        onProfileNameChange(it)
                                        if (validationError != null) validationError = null
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isNameFocused = it.isFocused },
                            )
                        }

                        if (profileName.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Button,
                                        onClickLabel = "Clear text",
                                        onClick = {
                                            onProfileNameChange("")
                                            validationError = null
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                // Inline validation error in white 70% (NEVER RED)
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = validationError ?: "",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = OnboardingTokens.TextMuted70,
                        ),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Role Selection Field
                Text(
                    text = "PROFILE ROLE",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.08.sp,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(10.dp))

                val chevronRotation by animateFloatAsState(
                    targetValue = if (roleDropdownExpanded) 180f else 0f,
                    animationSpec = if (reduceMotion) tween(0) else tween(200, easing = FastOutSlowInEasing),
                    label = "chevronRot",
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OnboardingTokens.GlassFill)
                            .border(OnboardingTokens.HairlineWidth, OnboardingTokens.GlassHairlineBorder, RoundedCornerShape(12.dp))
                            .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = role.displayName,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    color = Color.White,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = role.description,
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = OnboardingTokens.TextMuted50,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Select role",
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation },
                        )
                    }

                    if (roleDropdownExpanded) {
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(0, with(LocalDensity.current) { 60.dp.roundToPx() }),
                            onDismissRequest = { roleDropdownExpanded = false },
                            properties = PopupProperties(focusable = true),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.SheetGlassFallback
                                    )
                                    .border(OnboardingTokens.HairlineWidth, OnboardingTokens.GlassHairlineBorder, RoundedCornerShape(12.dp))
                                    .padding(vertical = 4.dp),
                            ) {
                                Column {
                                    ProfileType.entries.forEachIndexed { index, option ->
                                        val isSelected = role == option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onRoleChange(option)
                                                    onKidsChange(option == ProfileType.KIDS)
                                                    roleDropdownExpanded = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = option.displayName,
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 15.sp,
                                                        color = Color.White,
                                                    ),
                                                )
                                                Text(
                                                    text = option.description,
                                                    style = TextStyle(
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 12.sp,
                                                        color = OnboardingTokens.TextMuted50,
                                                    ),
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = "Selected",
                                                        tint = OnboardingTokens.TextDarkButton,
                                                        modifier = Modifier.size(13.dp),
                                                    )
                                                }
                                            }
                                        }

                                        if (index < ProfileType.entries.size - 1) {
                                            HorizontalDivider(
                                                thickness = OnboardingTokens.HairlineWidth,
                                                color = OnboardingTokens.HairlineDivider,
                                                modifier = Modifier.padding(horizontal = 14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Hairline Divided Toggle Rows
                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                // Kids Profile Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kids profile",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                color = Color.White,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Curate content for younger viewers",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = OnboardingTokens.TextMuted50,
                            ),
                        )
                    }

                    ProfileGlassToggle(
                        checked = isKids,
                        onCheckedChange = { checked ->
                            onKidsChange(checked)
                            onRoleChange(if (checked) ProfileType.KIDS else ProfileType.STANDARD)
                        },
                    )
                }

                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                // PIN Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PIN lock",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                color = Color.White,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Require a 4-digit PIN to switch in",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = OnboardingTokens.TextMuted50,
                            ),
                        )
                    }

                    ProfileGlassToggle(
                        checked = isPinSet,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinCreator = true
                            } else {
                                onPinChange(null, null)
                            }
                        },
                    )
                }

                AnimatedVisibility(
                    visible = isPinSet,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ProfileGlassPill(
                            text = "Change PIN",
                            onClick = { showPinCreator = true },
                            visualHeight = 32.dp,
                            fontSize = 12,
                        )

                        ProfileGlassPill(
                            text = "Remove PIN",
                            onClick = { onPinChange(null, null) },
                            visualHeight = 32.dp,
                            fontSize = 12,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(90.dp)) // Extra space for pinned footer
            }
        }

        // Pinned Bottom White Pill Button with background fade scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to pageBgColor.copy(alpha = 0.85f),
                        1.0f to pageBgColor,
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                OnboardingWhitePillButton(
                    text = "Continue",
                    onClick = ::validateAndProceed,
                )
            }
        }
    }

    // PIN Creator Dialog
    if (showPinCreator) {
        PinCreatorDialog(
            onDismissRequest = { showPinCreator = false },
            onPinConfirmed = { confirmedPin ->
                onPinChange(hashPin(confirmedPin), confirmedPin)
                showPinCreator = false
            },
        )
    }

    // Custom Avatar Crop Dialog
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
 * PAGE 3: CUSTOMIZE ("Craft Your Canvas")
 * - Headline: "Make it" / "yours."
 * - Subtitle: "Change any of this later in Settings."
 * - Live Preview Card (fixed 160 dp area height, animated mini posters, static mini bottom nav bar)
 * - Posters choice block (CardScaleMode segmented control)
 * - Appearance choice block (Dark vs AMOLED segmented control, applies live)
 * - Removed Navigation block
 * - Pinned 52 dp white pill "Enter Euthopiar" with spinner on loading
 */
@Composable
private fun OnboardingPage3Customize(
    selectedCardScale: CardScaleMode,
    onCardScaleChanged: (CardScaleMode) -> Unit,
    isAmoled: Boolean,
    onAmoledChanged: (Boolean) -> Unit,
    selectedRegionPreset: ContentRegionPreset,
    onRegionPresetChanged: (ContentRegionPreset) -> Unit,
    isSubmitting: Boolean,
    onEnterSanctuary: () -> Unit,
    hazeState: HazeState,
    previewItems: List<ShowcasePosterItem> = ShowcaseRowTop.take(5),
    isLoadingPosters: Boolean = false,
) {
    val pageBgColor = if (isAmoled) OnboardingTokens.AmoledBackground else OnboardingTokens.DarkBackground

    val posterDescription = when (selectedCardScale) {
        CardScaleMode.FLAGSHIP -> "Large posters, two per row"
        CardScaleMode.BALANCED -> "Balanced layout with rich breathing room"
        CardScaleMode.COMPACT -> "Compact grid for maximum catalog density"
    }

    val appearanceDescription = if (isAmoled) {
        "Pure black. Best on OLED screens."
    } else {
        "Dark with a soft glow."
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Big Headline: 40 sp Medium, 2 lines
                Text(
                    text = "Make it\nyours.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.03).sp,
                        color = Color.White,
                    ),
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Change any of this later in Settings.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = OnboardingTokens.TextMuted50,
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // LIVE PREVIEW CARD (Fixed 160 dp height for poster stage, glass card)
                OnboardingLivePreviewCard(
                    cardScale = selectedCardScale,
                    isAmoled = isAmoled,
                    hazeState = hazeState,
                    previewItems = previewItems,
                    isLoading = isLoadingPosters,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // CHOICE BLOCK 1: POSTERS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                ) {
                    Text(
                        text = "Posters",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = posterDescription,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = OnboardingTokens.TextMuted50,
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OnboardingSegmentedControl(
                        options = listOf(CardScaleMode.FLAGSHIP, CardScaleMode.BALANCED, CardScaleMode.COMPACT),
                        selectedOption = selectedCardScale,
                        onOptionSelected = onCardScaleChanged,
                        labelProvider = { mode ->
                            when (mode) {
                                CardScaleMode.FLAGSHIP -> "Large"
                                CardScaleMode.BALANCED -> "Balanced"
                                CardScaleMode.COMPACT -> "Compact"
                            }
                        },
                        hazeState = hazeState,
                        isAmoled = isAmoled,
                    )
                }

                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                // CHOICE BLOCK 2: APPEARANCE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                ) {
                    Text(
                        text = "Appearance",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = appearanceDescription,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = OnboardingTokens.TextMuted50,
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OnboardingSegmentedControl(
                        options = listOf(false, true),
                        selectedOption = isAmoled,
                        onOptionSelected = onAmoledChanged,
                        labelProvider = { isAmoledSelected ->
                            if (isAmoledSelected) "AMOLED black" else "Dark"
                        },
                        hazeState = hazeState,
                        isAmoled = isAmoled,
                    )
                }

                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                // CHOICE BLOCK 3: CONTENT REGION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                ) {
                    Text(
                        text = "Content Region",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            color = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedRegionPreset.subtitle,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = OnboardingTokens.TextMuted50,
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ContentRegionPreset.entries.size) { idx ->
                            val preset = ContentRegionPreset.entries[idx]
                            val isSelected = preset == selectedRegionPreset
                            val chipBg = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f)
                            val chipTextColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(
                                        0.5.dp,
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onRegionPresetChanged(preset) }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = chipTextColor
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(88.dp)) // Extra space for pinned button
            }
        }

        // Pinned Bottom Button: "Enter Euthopiar"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to pageBgColor.copy(alpha = 0.85f),
                        1.0f to pageBgColor,
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                OnboardingWhitePillButton(
                    text = "Enter Euthopiar",
                    isLoading = isSubmitting,
                    onClick = onEnterSanctuary,
                )
            }
        }
    }
}

/**
 * LIVE PREVIEW CARD
 * - Glass card: 16 dp radius, 12 dp padding
 * - Header row: left "LIVE PREVIEW" 11 sp uppercase; right muted summary e.g. "Large · Dark"
 * - Fixed 160 dp poster area height, 5 mini posters, animated width/height
 * - Static mini bottom navigation bar with tinted glass lens on Home
 */
@Composable
private fun OnboardingLivePreviewCard(
    cardScale: CardScaleMode,
    isAmoled: Boolean,
    hazeState: HazeState,
    previewItems: List<ShowcasePosterItem> = ShowcaseRowTop.take(5),
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current

    val posterWidth by animateDpAsState(
        targetValue = when (cardScale) {
            CardScaleMode.FLAGSHIP -> 104.dp
            CardScaleMode.BALANCED -> 72.dp
            CardScaleMode.COMPACT -> 54.dp
        },
        animationSpec = if (reduceMotion) tween(0) else tween(240, easing = FastOutSlowInEasing),
        label = "miniPosterWidth",
    )

    val posterHeight by animateDpAsState(
        targetValue = when (cardScale) {
            CardScaleMode.FLAGSHIP -> 156.dp
            CardScaleMode.BALANCED -> 108.dp
            CardScaleMode.COMPACT -> 81.dp
        },
        animationSpec = if (reduceMotion) tween(0) else tween(240, easing = FastOutSlowInEasing),
        label = "miniPosterHeight",
    )

    val scaleLabel = when (cardScale) {
        CardScaleMode.FLAGSHIP -> "Large"
        CardScaleMode.BALANCED -> "Balanced"
        CardScaleMode.COMPACT -> "Compact"
    }

    val summaryText = "$scaleLabel · ${if (isAmoled) "AMOLED" else "Dark"}"
    val itemsToRender = if (previewItems.isNotEmpty()) previewItems else ShowcaseRowTop.take(5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onboardingGlassSurface(
                shape = RoundedCornerShape(16.dp),
                hazeState = hazeState,
                isAmoled = isAmoled,
            )
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = "Preview of your layout" },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            letterSpacing = 0.12.sp,
                            color = OnboardingTokens.TextMuted45,
                        ),
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 1.5.dp,
                            color = OnboardingTokens.TextMuted45,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }

                Text(
                    text = summaryText,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = OnboardingTokens.TextMuted55,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row of mini posters with FIXED 160 dp area height so card never jumps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    itemsToRender.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(width = posterWidth, height = posterHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OnboardingTokens.GlassFill)
                                .border(
                                    width = OnboardingTokens.HairlineWidth,
                                    color = OnboardingTokens.GlassHairlineBorder,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Fallback local illustration if offline or loading
                            Image(
                                painter = painterResource(id = item.fallbackDrawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )

                            // Dark subtle glass scrim over the fallback
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.20f)),
                            )

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.posterUrl)
                                    .crossfade(200)
                                    .error(item.fallbackDrawableRes)
                                    .fallback(item.fallbackDrawableRes)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Static Mini Bottom Navigation Bar
            // Glass pill, 20 dp radius, 4 dp inner padding, 4 tabs (Home, Search, Library, Profile)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(OnboardingTokens.GlassFill)
                    .border(
                        width = OnboardingTokens.HairlineWidth,
                        color = OnboardingTokens.GlassHairlineBorder,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Home Tab (Active with tinted glass lens: white 70% opacity, 14 dp radius, #0B0B0F text & icon)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.70f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = EuthopiarIcons.Home.outlined,
                                contentDescription = "Home",
                                tint = OnboardingTokens.TextDarkButton,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Home",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = OnboardingTokens.TextDarkButton,
                                ),
                            )
                        }
                    }

                    // Search Tab (Inactive)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = EuthopiarIcons.Search.outlined,
                                contentDescription = "Search",
                                tint = OnboardingTokens.TextMuted70,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Search",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = OnboardingTokens.TextMuted70,
                                ),
                            )
                        }
                    }

                    // Library Tab (Inactive)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = EuthopiarIcons.Library.outlined,
                                contentDescription = "Library",
                                tint = OnboardingTokens.TextMuted70,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Library",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = OnboardingTokens.TextMuted70,
                                ),
                            )
                        }
                    }

                    // Profile Tab (Inactive)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = EuthopiarIcons.Profile.outlined,
                                contentDescription = "Profile",
                                tint = OnboardingTokens.TextMuted70,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Profile",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = OnboardingTokens.TextMuted70,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
