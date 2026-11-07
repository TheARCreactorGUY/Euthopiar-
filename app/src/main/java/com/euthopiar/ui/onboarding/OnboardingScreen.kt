package com.euthopiar.ui.onboarding

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.euthopiar.data.sync.supabase.SupabaseSyncManager
import com.euthopiar.R
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.unit.em
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import com.euthopiar.ui.theme.pressable
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalView
import com.euthopiar.ui.theme.EuthopiarMotionTokens
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
    val syncManager = remember { SupabaseSyncManager.getInstance(context) }
    var isSigningInWithGoogle by remember { mutableStateOf(false) }

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
            // 1. Commit created profile as the primary profile (preserving any PIN hash or restored sub-profiles)
            profileManager.setInitialPrimaryProfile(
                name = profileName.trim().ifEmpty { "Cinephile" },
                avatarId = profileAvatarId,
                type = if (isKidsProfile) ProfileType.KIDS else profileRole,
                isLocked = !profilePinHash.isNullOrBlank() || !profilePlainPin.isNullOrBlank(),
                pin = profilePlainPin,
                pinHash = profilePinHash
            )

            // 2. Persist chosen layout settings (Navbar style silently defaults to Floating)
            preferencesRepository.setNavbarStyle(NavbarStyle.FLOATING)
            preferencesRepository.setCardScaleMode(selectedCardScale)
            preferencesRepository.setAmoledMode(isAmoledActive)
            preferencesRepository.setContentRegionPreset(selectedRegionPreset)
            TmdbBridge.activeRegionPreset = selectedRegionPreset

            // 3. Replicate final onboarding profile & settings to cloud if signed in
            syncManager.scheduleSync(500L)

            // 4. Mark first run completed in DataStore
            onComplete(); kotlinx.coroutines.delay(260L); preferencesRepository.setFirstRunCompleted(true)

            // 5. Synchronous backup commit in SharedPreferences
            withContext(Dispatchers.IO) {
                context.getSharedPreferences("euthopiar_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_run_completed", true)
                    .commit()
            }
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
                        if (movingForward) {
                            (fadeIn(animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) +
                                slideInHorizontally(animationSpec = EuthopiarMotionTokens.SpringSpatial) { it / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(animationSpec = EuthopiarMotionTokens.SpringSpatial) { -it / 3 }
                                )
                        } else {
                            (fadeIn(animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) +
                                slideInHorizontally(animationSpec = EuthopiarMotionTokens.SpringSpatial) { -it / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(animationSpec = EuthopiarMotionTokens.SpringSpatial) { it / 3 }
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
                        isSigningIn = isSigningInWithGoogle,
                        onGoogleSignIn = {
                            val act = context as? Activity
                            if (act != null) {
                                isSigningInWithGoogle = true
                                coroutineScope.launch {
                                    val res = syncManager.signInWithGoogle(act)
                                    isSigningInWithGoogle = false
                                    if (res.isSuccess) {
                                        val session = res.getOrThrow()
                                        val restoredPrimary = profileManager.profiles.value.firstOrNull { it.id == "default_user" }
                                        val restoredName = restoredPrimary?.name?.takeIf { it.isNotBlank() && it != "Primary" }
                                            ?: session.displayName
                                            ?: session.email?.substringBefore("@")
                                            ?: "Primary"
                                        profileName = restoredName
                                        if (restoredPrimary != null) {
                                            profileAvatarId = restoredPrimary.avatarId
                                            profileRole = restoredPrimary.type
                                            isKidsProfile = restoredPrimary.isKidsProfile
                                            profilePinHash = restoredPrimary.pinHash
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navigateToPage(1)
                                    } else {
                                        val err = res.exceptionOrNull()?.message ?: "Google Sign-In failed"
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
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
    isSigningIn: Boolean = false,
    onGoogleSignIn: () -> Unit = {},
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
                        letterSpacing = 0.10.em,
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
                        letterSpacing = (-0.03).em,
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

                // Google Sign-In Button (Frosted Obsidian Glass with Google Logo)
                OnboardingGoogleSignInButton(
                    isLoading = isSigningIn,
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Guest Fallback Button
                OnboardingWhitePillButton(
                    text = "Continue as Guest",
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

    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun validateAndProceed() {
        val trimmed = profileName.trim()
        if (trimmed.isEmpty()) {
            validationError = "Enter a name"
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.32f, stiffness = 620f),
                    initialVelocity = 1600f
                )
            }
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
                        letterSpacing = 0.10.em,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Main Headline: 40 sp Medium with Apple/Emil negative optical tracking
                Text(
                    text = "Your profile.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.03).em,
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
                        letterSpacing = 0.08.em,
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
                                    showSelectedCheck = false,
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
                                    showSelectedCheck = false,
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
                                    showSelectedCheck = false,
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
                            letterSpacing = 0.08.em,
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

                val fieldBorderColor by animateColorAsState(
                    targetValue = when {
                        validationError != null -> Color.White.copy(alpha = 0.70f)
                        isNameFocused -> Color.White.copy(alpha = 0.40f)
                        else -> OnboardingTokens.GlassHairlineBorder
                    },
                    animationSpec = tween(150),
                    label = "nameFieldBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeOffset.value }
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

                        AnimatedVisibility(
                            visible = profileName.isNotEmpty(),
                            enter = scaleIn(initialScale = 0.92f, animationSpec = EuthopiarMotionTokens.SpringMicro) + fadeIn(tween(150)),
                            exit = scaleOut(targetScale = 0.92f) + fadeOut(tween(120))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .pressable(
                                        scaleDown = 0.92f,
                                        onClickLabel = "Clear text",
                                        onClick = {
                                            onProfileNameChange("")
                                            validationError = null
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
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
                        letterSpacing = 0.08.em,
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
                            .pressable(
                                scaleDown = 0.98f,
                                onClick = {
                                    focusManager.clearFocus()
                                    roleDropdownExpanded = !roleDropdownExpanded
                                }
                            )
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
                        val dropdownAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(roleDropdownExpanded) {
                            dropdownAnim.snapTo(0f)
                            dropdownAnim.animateTo(1f, animationSpec = EuthopiarMotionTokens.SpringSheet)
                        }

                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(0, with(LocalDensity.current) { 60.dp.roundToPx() }),
                            onDismissRequest = { roleDropdownExpanded = false },
                            properties = PopupProperties(focusable = true),
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 440.dp)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        val p = dropdownAnim.value
                                        scaleX = 0.95f + 0.05f * p
                                        scaleY = 0.95f + 0.05f * p
                                        alpha = p
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    }
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
                                                .pressable(
                                                    scaleDown = 0.98f,
                                                    onClick = {
                                                        onRoleChange(option)
                                                        onKidsChange(option == ProfileType.KIDS)
                                                        roleDropdownExpanded = false
                                                    }
                                                )
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

                val toggleView = LocalView.current

                // Kids Profile Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .pressable(
                            scaleDown = 0.98f,
                            onClick = {
                                toggleView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                val next = !isKids
                                onKidsChange(next)
                                onRoleChange(if (next) ProfileType.KIDS else ProfileType.STANDARD)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
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
                            toggleView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
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
                        .clip(RoundedCornerShape(14.dp))
                        .pressable(
                            scaleDown = 0.98f,
                            onClick = {
                                toggleView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                if (!isPinSet) {
                                    showPinCreator = true
                                } else {
                                    onPinChange(null, null)
                                }
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
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
                            toggleView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
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
private fun getRegionFlag(preset: ContentRegionPreset): String = getRegionCode(preset)

private fun getRegionCode(preset: ContentRegionPreset): String = when (preset) {
    ContentRegionPreset.GLOBAL -> "GL"
    ContentRegionPreset.INDIA_ALL -> "IN"
    ContentRegionPreset.SOUTH_INDIA_ALL -> "IN"
    ContentRegionPreset.SOUTH_INDIA_TELUGU -> "TE"
    ContentRegionPreset.SOUTH_INDIA_TAMIL -> "TA"
    ContentRegionPreset.SOUTH_INDIA_MALAYALAM -> "ML"
    ContentRegionPreset.SOUTH_INDIA_KANNADA -> "KN"
    ContentRegionPreset.NORTH_INDIA_HINDI -> "HI"
    ContentRegionPreset.UNITED_STATES -> "US"
    ContentRegionPreset.UNITED_KINGDOM -> "UK"
    ContentRegionPreset.SOUTH_KOREA -> "KR"
    ContentRegionPreset.JAPAN -> "JP"
}

private fun getRegionCode(id: String): String = when (id.lowercase()) {
    "global" -> "GL"
    "india_all" -> "IN"
    "south_india_all" -> "IN"
    "south_india_telugu", "telugu" -> "TE"
    "south_india_tamil", "tamil" -> "TA"
    "south_india_malayalam", "malayalam" -> "ML"
    "south_india_kannada", "kannada" -> "KN"
    "north_india_hindi", "hindi" -> "HI"
    "united_states", "us" -> "US"
    "united_kingdom", "uk" -> "UK"
    "south_korea", "korea" -> "KR"
    "japan" -> "JP"
    else -> id.take(2).uppercase()
}

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
                Spacer(modifier = Modifier.height(14.dp))

                // Small label above headline
                Text(
                    text = "STEP 3",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.10.em,
                        color = OnboardingTokens.TextMuted45,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Big Headline: 40 sp Medium, 2 lines
                Text(
                    text = "Make it\nyours.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-0.03).em,
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
                    selectedRegionPreset = selectedRegionPreset,
                    previewItems = previewItems,
                    isLoading = isLoadingPosters,
                )

                Spacer(modifier = Modifier.height(22.dp))

                // HERO CHOICE BLOCK 1: CONTENT REGION (Primary Home Screen Curation Decision)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CONTENT REGION",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                letterSpacing = 0.10.em,
                                color = OnboardingTokens.TextMuted45,
                            ),
                        )

                        // Active Region Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "${getRegionCode(selectedRegionPreset)} · ${selectedRegionPreset.cleanName}",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Shapes what movies, series, and regional cinema appear on your home feed.",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = OnboardingTokens.TextMuted55,
                        ),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Region Category Filter Chips
                    var regionFilter by remember { mutableStateOf("Popular") }
                    val filterTabs = listOf("Popular", "India & Regional", "International", "All")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filterTabs.forEach { tab ->
                            val isTabSelected = regionFilter == tab
                            val tabBg by animateColorAsState(
                                targetValue = if (isTabSelected) Color.White else Color.White.copy(alpha = 0.06f),
                                animationSpec = tween(120),
                                label = "regionTabBg_$tab"
                            )
                            val tabTextColor by animateColorAsState(
                                targetValue = if (isTabSelected) OnboardingTokens.TextDarkButton else OnboardingTokens.TextMuted70,
                                animationSpec = tween(120),
                                label = "regionTabText_$tab"
                            )

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tabBg)
                                    .border(
                                        0.5.dp,
                                        if (isTabSelected) Color.White else Color.White.copy(alpha = 0.10f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .pressable(
                                        scaleDown = 0.96f,
                                        onClick = { regionFilter = tab }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = tab,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = if (isTabSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = tabTextColor,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val displayedPresets = remember(regionFilter) {
                        when (regionFilter) {
                            "Popular" -> listOf(
                                ContentRegionPreset.GLOBAL,
                                ContentRegionPreset.INDIA_ALL,
                                ContentRegionPreset.NORTH_INDIA_HINDI,
                                ContentRegionPreset.SOUTH_INDIA_TELUGU,
                                ContentRegionPreset.UNITED_STATES,
                                ContentRegionPreset.SOUTH_KOREA,
                                ContentRegionPreset.JAPAN
                            )
                            "India & Regional" -> listOf(
                                ContentRegionPreset.INDIA_ALL,
                                ContentRegionPreset.NORTH_INDIA_HINDI,
                                ContentRegionPreset.SOUTH_INDIA_ALL,
                                ContentRegionPreset.SOUTH_INDIA_TELUGU,
                                ContentRegionPreset.SOUTH_INDIA_TAMIL,
                                ContentRegionPreset.SOUTH_INDIA_MALAYALAM,
                                ContentRegionPreset.SOUTH_INDIA_KANNADA
                            )
                            "International" -> listOf(
                                ContentRegionPreset.GLOBAL,
                                ContentRegionPreset.UNITED_STATES,
                                ContentRegionPreset.UNITED_KINGDOM,
                                ContentRegionPreset.SOUTH_KOREA,
                                ContentRegionPreset.JAPAN
                            )
                            else -> ContentRegionPreset.entries.toList()
                        }
                    }

                    // High-contrast, tactile region cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)),
                    ) {
                        displayedPresets.forEach { preset ->
                            val isSelected = preset == selectedRegionPreset
                            val cardBg by animateColorAsState(
                                targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else OnboardingTokens.GlassFill,
                                animationSpec = tween(150),
                                label = "regionCardBg_${preset.id}"
                            )
                            val cardBorder by animateColorAsState(
                                targetValue = if (isSelected) Color.White.copy(alpha = 0.50f) else OnboardingTokens.GlassHairlineBorder,
                                animationSpec = tween(150),
                                label = "regionCardBorder_${preset.id}"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .border(0.5.dp, cardBorder, RoundedCornerShape(14.dp))
                                    .pressable(
                                        scaleDown = 0.98f,
                                        onClick = { onRegionPresetChanged(preset) },
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color.White.copy(alpha = if (isSelected) 0.14f else 0.06f),
                                            CircleShape
                                        )
                                        .border(
                                            0.5.dp,
                                            Color.White.copy(alpha = if (isSelected) 0.28f else 0.12f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = getRegionCode(preset),
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.6.sp,
                                            color = Color.White,
                                        ),
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            color = Color.White,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.subtitle,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.75f) else OnboardingTokens.TextMuted50,
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.30f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isSelected,
                                        enter = scaleIn(initialScale = 0.90f, animationSpec = EuthopiarMotionTokens.SpringMicro) + fadeIn(tween(150)),
                                        exit = scaleOut(targetScale = 0.90f) + fadeOut(tween(120)),
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
                        }
                    }
                }

                HorizontalDivider(
                    thickness = OnboardingTokens.HairlineWidth,
                    color = OnboardingTokens.HairlineDivider,
                )

                // CHOICE BLOCK 2: POSTERS
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

                // CHOICE BLOCK 3: APPEARANCE
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
    selectedRegionPreset: ContentRegionPreset = ContentRegionPreset.GLOBAL,
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
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "miniPosterWidth",
    )

    val posterHeight by animateDpAsState(
        targetValue = when (cardScale) {
            CardScaleMode.FLAGSHIP -> 156.dp
            CardScaleMode.BALANCED -> 108.dp
            CardScaleMode.COMPACT -> 81.dp
        },
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "miniPosterHeight",
    )

    val scaleLabel = when (cardScale) {
        CardScaleMode.FLAGSHIP -> "Large"
        CardScaleMode.BALANCED -> "Balanced"
        CardScaleMode.COMPACT -> "Compact"
    }

    val allPosters = if (previewItems.isNotEmpty()) previewItems else (ShowcaseRowTop + ShowcaseRowBottom)
    val offsetIndex = selectedRegionPreset.hashCode().and(0x7FFFFFFF) % allPosters.size.coerceAtLeast(1)
    val itemsToRender = remember(allPosters, offsetIndex) {
        if (allPosters.isEmpty()) emptyList()
        else (allPosters.drop(offsetIndex) + allPosters.take(offsetIndex)).take(5)
    }

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${getRegionCode(selectedRegionPreset)} · ${selectedRegionPreset.displayName}",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        )
                    }
                    Text(
                        text = "$scaleLabel · ${if (isAmoled) "AMOLED" else "Dark"}",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = OnboardingTokens.TextMuted55,
                        ),
                    )
                }
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

            // Interactive Mini Bottom Navigation Bar
            // Glass pill, 20 dp radius, 4 dp inner padding, 4 tabs (Home, Search, Library, Profile)
            var activePreviewTab by remember { mutableIntStateOf(0) }
            val miniNavView = LocalView.current
            val previewTabs = listOf(
                EuthopiarIcons.Home.outlined to "Home",
                EuthopiarIcons.Search.outlined to "Search",
                EuthopiarIcons.Library.outlined to "Library",
                EuthopiarIcons.Profile.outlined to "Profile",
            )

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
                    previewTabs.forEachIndexed { index, (icon, label) ->
                        val isActive = activePreviewTab == index
                        val tabBg by animateColorAsState(
                            targetValue = if (isActive) Color.White.copy(alpha = 0.70f) else Color.Transparent,
                            animationSpec = tween(150),
                            label = "miniTabBg_$index"
                        )
                        val tabTextColor by animateColorAsState(
                            targetValue = if (isActive) OnboardingTokens.TextDarkButton else OnboardingTokens.TextMuted70,
                            animationSpec = tween(150),
                            label = "miniTabTextColor_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tabBg)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    miniNavView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                    activePreviewTab = index
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tabTextColor,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = tabTextColor,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun OnboardingGoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "googleBtnScale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                role = Role.Button
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign in with Google",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}
