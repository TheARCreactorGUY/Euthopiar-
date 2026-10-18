package com.euthopiar.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.euthopiar.data.profile.CustomAvatarRepository
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.data.profile.defaultAvatarPresets
import com.euthopiar.data.profile.hashPin
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlin.math.roundToInt

/**
 * Screen 3: New Profile / Edit Profile Frosted Glass Bottom Sheet.
 * Shared sheet layout for both modes.
 * - Frosted glass sheet: dark translucent fill (72-74%), blur ~22px, 0.5px hairline on top edge, 28dp top corners, drag handle 36x4dp.
 * - Pinned header: headline 26sp medium, subtitle 12sp.
 * - Live preview row: 62dp avatar inside thin ring (0.5px white 16%, outer 72dp), name at 20sp medium, neutral glass chip. Live updating!
 * - Avatar picker: 5 per row, 48dp each, selected solid 2dp white ring with 2dp gap + outline check badge. Dashed circle add tile.
 * - Name input: counter "X/20" on right, 48dp glass field, 12dp radius, outline clear button, "Living Room" placeholder.
 * - Role dropdown: 56dp glass field with outline chevron, glass popover with check circle.
 * - Hairline divided toggle rows: Kids profile & PIN lock (46x28dp glass toggles).
 * - PINNED FOOTER: White pill button (52dp tall) + quiet "Delete profile" button (in Edit mode). Fully visible with keyboard open.
 * - Unsaved changes prompt on drag down / scrim tap.
 */
@Composable
fun ProfileStudioSheet(
    existingProfile: UserProfile?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarId: String, role: ProfileType, isKids: Boolean, pin: String?, pinHash: String?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current

    val profileManager = remember { ProfileManager.getInstance(context) }
    val existingProfiles by profileManager.profiles.collectAsState()

    // Form State
    val initialName = remember { existingProfile?.name ?: "" }
    val initialAvatarId = remember { existingProfile?.avatarId ?: "avatar_1" }
    val initialRole = remember { existingProfile?.type ?: ProfileType.STANDARD }
    val initialIsKids = remember { existingProfile?.isKidsProfile ?: false }
    val initialPinHash = remember { existingProfile?.pinHash }
    val initialPlainPin = remember { existingProfile?.pin }

    var name by remember { mutableStateOf(initialName) }
    var selectedAvatarId by remember { mutableStateOf(initialAvatarId) }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var isKids by remember { mutableStateOf(initialIsKids) }
    var pinHash by remember { mutableStateOf(initialPinHash) }
    var plainPin by remember { mutableStateOf(initialPlainPin) }

    val isPinSet = !pinHash.isNullOrBlank() || !plainPin.isNullOrBlank()

    var showPinCreator by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isNameFocused by remember { mutableStateOf(false) }

    // Custom avatar picking & cropping
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = ImageCropUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                pendingCropBitmap = bitmap
            }
        }
    }

    // Has unsaved changes check
    val hasUnsavedChanges = name.trim() != initialName.trim() ||
            selectedAvatarId != initialAvatarId ||
            selectedRole != initialRole ||
            isKids != initialIsKids ||
            pinHash != initialPinHash ||
            plainPin != initialPlainPin

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val initialOffsetPx = (screenHeightPx * 1.15f).coerceAtLeast(2400f)

    val sheetAnimOffsetY = remember { Animatable(initialOffsetPx) }
    val scrimAnimAlpha = remember { Animatable(0f) }
    var isClosingSheet by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    LaunchedEffect(Unit) {
        if (!reduceMotion && !isClosingSheet) {
            val entranceDuration = 320
            val entranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            launch {
                scrimAnimAlpha.animateTo(
                    targetValue = 0.72f,
                    animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
                )
            }
            sheetAnimOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = entranceDuration, easing = entranceEasing)
            )
        } else {
            scrimAnimAlpha.snapTo(0.72f)
            sheetAnimOffsetY.snapTo(0f)
        }
    }

    fun animateDismiss(onComplete: () -> Unit) {
        if (isClosingSheet) return
        isClosingSheet = true
        focusManager.clearFocus()
        keyboardController?.hide()
        coroutineScope.launch {
            if (!reduceMotion) {
                val exitDuration = 240
                val exitTarget = (screenHeightPx * 1.15f).coerceAtLeast(2400f)
                val scrimJob = launch {
                    scrimAnimAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                    )
                }
                val sheetJob = launch {
                    sheetAnimOffsetY.animateTo(
                        targetValue = exitTarget,
                        animationSpec = tween(durationMillis = exitDuration, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))
                    )
                }
                scrimJob.join()
                sheetJob.join()
            }
            onComplete()
        }
    }

    fun attemptDismiss() {
        if (isClosingSheet) return
        if (hasUnsavedChanges) {
            showDiscardConfirm = true
        } else {
            animateDismiss { onDismiss() }
        }
    }

    BackHandler {
        attemptDismiss()
    }

    fun handleSave() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            validationError = "Enter a name"
            return
        }

        // Check for duplicate name if changed
        val isDuplicate = existingProfiles.any {
            it.id != existingProfile?.id && it.name.trim().equals(trimmedName, ignoreCase = true)
        }
        if (isDuplicate) {
            validationError = "That name is already taken"
            return
        }

        validationError = null
        isSaving = true
        animateDismiss {
            onSave(trimmedName, selectedAvatarId, selectedRole, isKids, plainPin, pinHash)
        }
    }

    Dialog(
        onDismissRequest = { attemptDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val fullHeightPx = constraints.maxHeight.toFloat()
            val effectiveTargetExit = (fullHeightPx * 1.15f).coerceAtLeast(screenHeightPx)

            // Scrim background (Rendered exclusively in Draw phase to eliminate composition churn)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = scrimAnimAlpha.value))
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { attemptDismiss() }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Sheet container (up to ~90% height, translated via GPU RenderNode matrix)
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.90f)
                        .graphicsLayer {
                            translationY = sheetAnimOffsetY.value
                        }
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(
                            if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.SheetGlassFallback
                        )
                        .border(
                            width = ProfileGlassTokens.HairlineWidthDp,
                            color = ProfileGlassTokens.SheetHairlineBorder,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // block clicking through to scrim
                        )
                        .statusBarsPadding()
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Drag Handle & Header Area (Pinned at top, handles drag-to-dismiss without fighting content scroll)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            velocityTracker.resetTracking()
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (isClosingSheet) return@detectVerticalDragGestures
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            val newY = (sheetAnimOffsetY.value + dragAmount).coerceAtLeast(0f)
                                            coroutineScope.launch {
                                                sheetAnimOffsetY.snapTo(newY)
                                                val dragProgress = (newY / (effectiveTargetExit * 0.40f)).coerceIn(0f, 1f)
                                                scrimAnimAlpha.snapTo((1f - dragProgress) * 0.72f)
                                            }
                                        },
                                        onDragEnd = {
                                            if (isClosingSheet) return@detectVerticalDragGestures
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            val dismissThresholdPx = with(density) { 110.dp.toPx() }
                                            if (sheetAnimOffsetY.value > dismissThresholdPx || velocityY > 1200f) {
                                                attemptDismiss()
                                            } else {
                                                coroutineScope.launch {
                                                    launch {
                                                        sheetAnimOffsetY.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                    launch {
                                                        scrimAnimAlpha.animateTo(
                                                            targetValue = 0.72f,
                                                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            if (isClosingSheet) return@detectVerticalDragGestures
                                            coroutineScope.launch {
                                                launch {
                                                    sheetAnimOffsetY.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    scrimAnimAlpha.animateTo(
                                                        targetValue = 0.72f,
                                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                                .padding(horizontal = 22.dp)
                        ) {
                        // Drag Handle: 36 x 4 dp, white ~30%, centered
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = ProfileGlassTokens.DragHandleWidth, height = ProfileGlassTokens.DragHandleHeight)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.30f))
                            )
                        }

                        // Headline & Subtitle
                        Text(
                            text = if (existingProfile != null) "Edit profile" else "New profile",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 26.sp,
                                lineHeight = 28.sp,
                                letterSpacing = (-0.02).sp,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (existingProfile != null) existingProfile.name else "Pick an avatar and a name",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = ProfileGlassTokens.TextMuted50
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // 2. Scrollable Middle Body (weight 1f so footer is never pushed out)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp)
                    ) {
                        // Live Preview Row (Avatar 62dp inside thin ring, name 20sp medium, neutral glass chip)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarWithThinRing(
                                avatarId = selectedAvatarId,
                                avatarSize = 62.dp,
                                ringGap = 5.dp,
                                ringBorderWidth = 0.5.dp,
                                ringColor = Color.White.copy(alpha = 0.16f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (name.isBlank()) "Profile name" else name.trim(),
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp,
                                        lineHeight = 24.sp,
                                        color = if (name.isBlank()) Color.White.copy(alpha = 0.40f) else Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                ProfileGlassChip(
                                    text = if (isKids) "Kids" else selectedRole.displayName,
                                    showLockIcon = isPinSet
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. "Choose avatar" Section
                        Text(
                            text = "CHOOSE AVATAR",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                letterSpacing = 0.08.sp,
                                color = ProfileGlassTokens.TextLabel45
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        HorizontalDivider(
                            thickness = ProfileGlassTokens.HairlineWidthDp,
                            color = ProfileGlassTokens.HairlineDivider
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid of avatars: 5 per row, 48dp each, even spacing
                        val allAvatarItems = defaultAvatarPresets
                        val isCustomAvatarSelected = selectedAvatarId.startsWith("custom:")

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Row 1: Presets 0..4
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until minOf(5, allAvatarItems.size)) {
                                    val preset = allAvatarItems[i]
                                    val isSelected = selectedAvatarId == preset.id
                                    Box(
                                        modifier = Modifier.size(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AvatarWithThinRing(
                                            avatarId = preset.id,
                                            avatarSize = 48.dp,
                                            ringGap = 2.dp,
                                            isSelected = isSelected,
                                            showSelectedCheck = true,
                                            isAvatarOption = true,
                                            onClick = {
                                                selectedAvatarId = preset.id
                                                validationError = null
                                            }
                                        )
                                    }
                                }
                            }

                            // Row 2: Remaining presets 5..7 + custom upload tile + aligned slots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 5 until allAvatarItems.size) {
                                    val preset = allAvatarItems[i]
                                    val isSelected = selectedAvatarId == preset.id
                                    Box(
                                        modifier = Modifier.size(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AvatarWithThinRing(
                                            avatarId = preset.id,
                                            avatarSize = 48.dp,
                                            ringGap = 2.dp,
                                            isSelected = isSelected,
                                            showSelectedCheck = true,
                                            isAvatarOption = true,
                                            onClick = {
                                                selectedAvatarId = preset.id
                                                validationError = null
                                            }
                                        )
                                    }
                                }

                                // Custom Photo Upload Tile (or show cropped custom avatar with check)
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCustomAvatarSelected) {
                                        AvatarWithThinRing(
                                            avatarId = selectedAvatarId,
                                            avatarSize = 48.dp,
                                            ringGap = 2.dp,
                                            isSelected = true,
                                            showSelectedCheck = true,
                                            isAvatarOption = true,
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                            }
                                        )
                                    } else {
                                        DashedCircleButton(
                                            size = 48.dp,
                                            iconSize = 20.dp,
                                            contentDescription = "Upload custom photo",
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                            }
                                        )
                                    }
                                }

                                // 5th Slot: Upload new photo if 4th slot has custom avatar, else equal size spacer for exact alignment
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCustomAvatarSelected) {
                                        DashedCircleButton(
                                            size = 48.dp,
                                            iconSize = 20.dp,
                                            contentDescription = "Upload new custom photo",
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(52.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. "Profile name" Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROFILE NAME",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.08.sp,
                                    color = ProfileGlassTokens.TextLabel45
                                )
                            )

                            Text(
                                text = "${name.length}/20",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ProfileGlassTokens.TextLabel45
                                ),
                                modifier = Modifier.semantics {
                                    if (name.length >= 18) {
                                        this.contentDescription = "${name.length} of 20 characters, approaching limit"
                                    } else {
                                        this.contentDescription = "${name.length} of 20 characters"
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Glass Text Field (48 dp tall, 12 dp radius, 15 sp text, outline X clear button)
                        val fieldBorderColor = when {
                            validationError != null -> Color.White.copy(alpha = 0.60f)
                            isNameFocused -> ProfileGlassTokens.HairlineBorderActive
                            else -> ProfileGlassTokens.HairlineBorder
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ProfileGlassTokens.GlassFill)
                                .border(ProfileGlassTokens.HairlineWidthDp, fieldBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (name.isEmpty()) {
                                        Text(
                                            text = "Living Room",
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp,
                                                color = Color.White.copy(alpha = 0.40f)
                                            )
                                        )
                                    }
                                    BasicTextField(
                                        value = name,
                                        onValueChange = {
                                            if (it.length <= 20) {
                                                name = it
                                                if (validationError != null) validationError = null
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isNameFocused = it.isFocused }
                                    )
                                }

                                if (name.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                role = Role.Button,
                                                onClickLabel = "Clear text",
                                                onClick = {
                                                    name = ""
                                                    validationError = null
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.60f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Validation message
                        if (validationError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = validationError ?: "",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 5. "Profile role" Section
                        Text(
                            text = "PROFILE ROLE",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                letterSpacing = 0.08.sp,
                                color = ProfileGlassTokens.TextLabel45
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var fieldWidthPx by remember { mutableStateOf(0) }
                        var isRolePopupRendered by remember { mutableStateOf(false) }
                        val rolePopupProgress = remember { Animatable(0f) }

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (roleDropdownExpanded) 180f else 0f,
                            animationSpec = if (reduceMotion) tween(0) else tween(200, easing = FastOutSlowInEasing),
                            label = "chevronRotation"
                        )

                        LaunchedEffect(roleDropdownExpanded) {
                            if (roleDropdownExpanded) {
                                isRolePopupRendered = true
                                if (reduceMotion) {
                                    rolePopupProgress.snapTo(1f)
                                } else {
                                    rolePopupProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    )
                                }
                            } else if (isRolePopupRendered) {
                                if (reduceMotion) {
                                    rolePopupProgress.snapTo(0f)
                                    isRolePopupRendered = false
                                } else {
                                    rolePopupProgress.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                    )
                                    isRolePopupRendered = false
                                }
                            }
                        }

                        // Glass Dropdown Field (56 dp tall, 12 dp radius, role name + one line description)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .onGloballyPositioned { fieldWidthPx = it.size.width }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ProfileGlassTokens.GlassFill)
                                    .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(12.dp))
                                    .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedRole.displayName,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                            lineHeight = 18.sp,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedRole.description,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            color = ProfileGlassTokens.TextMuted50
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Select role",
                                    tint = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }

                            // Glass Dropdown Menu with start-to-end ease-in / ease-out animation
                            if (isRolePopupRendered) {
                                val density = LocalDensity.current
                                val progress = rolePopupProgress.value
                                val menuAlpha = progress
                                val menuScale = 0.95f + (0.05f * progress)
                                val menuTranslationY = (1f - progress) * (-6f) * density.density

                                Popup(
                                    alignment = Alignment.TopStart,
                                    offset = IntOffset(0, with(density) { 60.dp.roundToPx() }),
                                    onDismissRequest = { roleDropdownExpanded = false },
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .then(if (fieldWidthPx > 0) Modifier.width(with(density) { fieldWidthPx.toDp() }) else Modifier.fillMaxWidth())
                                            .graphicsLayer {
                                                alpha = menuAlpha
                                                scaleX = menuScale
                                                scaleY = menuScale
                                                translationY = menuTranslationY
                                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                                            }
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.SheetGlassFallback
                                            )
                                            .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(12.dp))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column {
                                            ProfileType.entries.forEachIndexed { index, roleOption ->
                                                val isSelected = selectedRole == roleOption
                                                val itemInteractionSource = remember { MutableInteractionSource() }
                                                val isItemPressed by itemInteractionSource.collectIsPressedAsState()
                                                val itemScale by animateFloatAsState(
                                                    targetValue = if (isItemPressed && !reduceMotion) 0.98f else 1f,
                                                    animationSpec = tween(120),
                                                    label = "roleItemScale_${roleOption.name}"
                                                )

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .graphicsLayer {
                                                            scaleX = itemScale
                                                            scaleY = itemScale
                                                        }
                                                        .clickable(
                                                            interactionSource = itemInteractionSource,
                                                            indication = null,
                                                            onClick = {
                                                                selectedRole = roleOption
                                                                isKids = (roleOption == ProfileType.KIDS)
                                                                roleDropdownExpanded = false
                                                            }
                                                        )
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = roleOption.displayName,
                                                            style = TextStyle(
                                                                fontFamily = AppFontFamily,
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 15.sp,
                                                                color = Color.White
                                                            )
                                                        )
                                                        Text(
                                                            text = roleOption.description,
                                                            style = TextStyle(
                                                                fontFamily = AppFontFamily,
                                                                fontWeight = FontWeight.Normal,
                                                                fontSize = 12.sp,
                                                                color = ProfileGlassTokens.TextMuted50
                                                            )
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .aspectRatio(1f)
                                                                .clip(CircleShape)
                                                                .background(Color.White),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Check,
                                                                contentDescription = "Selected",
                                                                tint = ProfileGlassTokens.TextDarkButton,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (index < ProfileType.entries.size - 1) {
                                                    HorizontalDivider(
                                                        thickness = ProfileGlassTokens.HairlineWidthDp,
                                                        color = ProfileGlassTokens.HairlineDivider,
                                                        modifier = Modifier.padding(horizontal = 14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 6. Hairline-divided Toggle Rows (NOT boxed)
                        HorizontalDivider(
                            thickness = ProfileGlassTokens.HairlineWidthDp,
                            color = ProfileGlassTokens.HairlineDivider
                        )

                        // Kids Profile Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kids profile",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        lineHeight = 18.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Curate content for younger viewers",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = ProfileGlassTokens.TextMuted50
                                    )
                                )
                            }

                            ProfileGlassToggle(
                                checked = isKids,
                                onCheckedChange = { checked ->
                                    isKids = checked
                                    selectedRole = if (checked) ProfileType.KIDS else ProfileType.STANDARD
                                }
                            )
                        }

                        HorizontalDivider(
                            thickness = ProfileGlassTokens.HairlineWidthDp,
                            color = ProfileGlassTokens.HairlineDivider
                        )

                        // PIN Lock Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PIN lock",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        lineHeight = 18.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Require a 4-digit PIN to switch in",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = ProfileGlassTokens.TextMuted50
                                    )
                                )
                            }

                            ProfileGlassToggle(
                                checked = isPinSet,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showPinCreator = true
                                    } else {
                                        pinHash = null
                                        plainPin = null
                                    }
                                }
                            )
                        }

                        // If PIN is set: show Change PIN and Remove PIN actions
                        AnimatedVisibility(
                            visible = isPinSet,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProfileGlassPill(
                                    text = "Change PIN",
                                    onClick = { showPinCreator = true },
                                    visualHeight = 32.dp,
                                    fontSize = 12
                                )

                                ProfileGlassPill(
                                    text = "Remove PIN",
                                    onClick = {
                                        pinHash = null
                                        plainPin = null
                                    },
                                    visualHeight = 32.dp,
                                    fontSize = 12
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 7. Pinned Footer (Always visible, never clipped)
                    // Hairline on top, 12 dp padding top, 20 dp bottom (+ gesture inset)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp)
                    ) {
                        HorizontalDivider(
                            thickness = ProfileGlassTokens.HairlineWidthDp,
                            color = ProfileGlassTokens.HairlineDivider
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full width solid white pill button
                        ProfileWhitePillButton(
                            text = if (existingProfile != null) "Save changes" else "Create profile",
                            onClick = ::handleSave,
                            isLoading = isSaving
                        )

                        // In Edit mode only: quiet centered text button "Delete profile" in white 55%
                        if (canDelete) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Button,
                                        onClickLabel = "Delete profile",
                                        onClick = { showDeleteConfirm = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Delete profile",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

    // PIN Creator Dialog
    if (showPinCreator) {
        PinCreatorDialog(
            onDismissRequest = { showPinCreator = false },
            onPinConfirmed = { confirmedPin ->
                plainPin = null
                pinHash = hashPin(confirmedPin)
                showPinCreator = false
            }
        )
    }

    // Circular Image Crop Dialog for Custom Photos
    pendingCropBitmap?.let { bitmap ->
        DetroitCircleCropDialog(
            sourceBitmap = bitmap,
            onDismissRequest = { pendingCropBitmap = null },
            onCropConfirmed = { cropped ->
                val savedId = CustomAvatarRepository.saveCroppedAvatar(context, cropped)
                selectedAvatarId = savedId
                pendingCropBitmap = null
            }
        )
    }

    // Delete Confirmation Sheet
    if (showDeleteConfirm) {
        GlassConfirmDialog(
            title = "Delete this profile?",
            message = "Watch history and personal settings will be permanently removed. This cannot be undone.",
            confirmText = "Delete",
            cancelText = "Cancel",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                animateDismiss { onDelete() }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Unsaved Changes Confirmation Sheet
    if (showDiscardConfirm) {
        GlassConfirmDialog(
            title = "Discard changes?",
            message = "You have unsaved changes that will be lost.",
            confirmText = "Keep editing",
            cancelText = "Discard",
            onConfirm = { showDiscardConfirm = false },
            onDismiss = { showDiscardConfirm = false },
            onCancelAction = {
                showDiscardConfirm = false
                animateDismiss { onDismiss() }
            }
        )
    }
}
