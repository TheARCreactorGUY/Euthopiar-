package com.euthopiar.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import com.euthopiar.MainActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.ui.player.subtitles.SubtitlePreset
import com.euthopiar.ui.components.PrimaryButton
import com.euthopiar.ui.components.SecondaryButton
import com.euthopiar.ui.components.VelvetAlertDialog
import com.euthopiar.ui.components.ErrorState
import com.euthopiar.ui.util.UserError
import androidx.compose.ui.res.stringResource
import com.euthopiar.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    mediaId: String,
    provider: String? = null,
    type: String? = null,
    episodeData: String? = null,
    mediaTitle: String? = null,
    episodeTitle: String? = null,
    initialSeason: Int? = null,
    initialEpisode: Int? = null,
    onBack: () -> Unit,
    onNavigateToPluginManager: (() -> Unit)? = null,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()
    val subtitleText by viewModel.currentSubtitleText.collectAsState()
    val currentCue by viewModel.currentSubtitleCue.collectAsState()
    val isInPip by PipController.isInPipMode.collectAsState()
    val isPipSupported by PipController.isPipSupported.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showUnlockButton by remember { mutableStateOf(false) }
    var unlockDismissJob by remember { mutableStateOf<Job?>(null) }
    var showCastPickerSheet by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(true) }
    var videoSurfaceScale by remember { mutableFloatStateOf(1.0f) }
    val targetAspectScale = 1.0f
    LaunchedEffect(targetAspectScale) {
        if (kotlin.math.abs(videoSurfaceScale - targetAspectScale) > 0.001f) {
            val anim = androidx.compose.animation.core.Animatable(videoSurfaceScale)
            anim.animateTo(
                targetValue = targetAspectScale,
                animationSpec = androidx.compose.animation.core.spring(
                    stiffness = PlayerMotionMath.SPRING_PINCH_K,
                    dampingRatio = PlayerMotionMath.SPRING_PINCH_ZETA
                )
            ) {
                videoSurfaceScale = this.value
            }
        }
    }
    val playerFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val windowDimensions = com.euthopiar.ui.theme.rememberWindowDimensions()

    fun triggerShowUnlockButton() {
        showUnlockButton = true
        unlockDismissJob?.cancel()
        unlockDismissJob = coroutineScope.launch {
            delay(3500L)
            showUnlockButton = false
        }
    }

    val handleBackNavigation: () -> Unit = {
        when {
            showCastPickerSheet -> showCastPickerSheet = false
            uiState.isEpisodeSelectorOpen -> viewModel.setEpisodeSelectorOpen(false)
            uiState.activeSidePanel != PlayerSidePanelType.NONE -> viewModel.closeSidePanel()
            uiState.isScreenLocked -> triggerShowUnlockButton()
            uiState.controlsVisible -> viewModel.hideControls()
            else -> onBack()
        }
    }

    LaunchedEffect(Unit) {
        playerFocusRequester.requestFocus()
    }

    LaunchedEffect(windowDimensions.isTV, uiState.controlsVisible) {
        if (windowDimensions.isTV && uiState.controlsVisible) {
            delay(100L)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showCastPickerSheet, uiState.activeSidePanel, uiState.isEpisodeSelectorOpen) {
        if (!showCastPickerSheet && uiState.activeSidePanel == PlayerSidePanelType.NONE && !uiState.isEpisodeSelectorOpen) {
            playerFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isInPip) {
        if (isInPip) {
            viewModel.closeSidePanel()
            viewModel.setEpisodeSelectorOpen(false)
        }
    }

    // Intercept hardware / gesture back button:
    // First Back hides open sheets, panels or controls; second exits.
    BackHandler {
        handleBackNavigation()
    }

    // Register PiP callbacks with PipController
    DisposableEffect(viewModel, uiState.isPlaying, uiState.seekIntervalSeconds) {
        PipController.onTogglePlayPause = { viewModel.togglePlayPause() }
        PipController.onSeekRelative = { deltaMs -> viewModel.seekRelative(deltaMs) }
        PipController.seekIntervalMs = uiState.seekIntervalSeconds * 1000L
        PipController.isPlaying = { uiState.isPlaying }
        (activity as? MainActivity)?.updatePipActions(uiState.isPlaying)
        onDispose {
            PipController.clearCallbacks()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!PipController.isInPipMode()) {
                    viewModel.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Forced Immersive Landscape Setup & Teardown
    DisposableEffect(activity) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Lock landscape & Keep screen on
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide status & navigation bars (immersive sticky)
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Restore original orientation
            activity?.requestedOrientation = originalOrientation
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Load stream on initial launch or key parameter changes
    LaunchedEffect(mediaId, episodeData) {
        val mediaType = try {
            if (type != null) MediaType.valueOf(type) else MediaType.MOVIE
        } catch (e: Exception) {
            MediaType.MOVIE
        }
        viewModel.loadStream(
            mediaId = mediaId,
            episodeData = episodeData,
            provider = provider,
            type = mediaType,
            title = mediaTitle,
            epTitle = episodeTitle,
            season = initialSeason,
            episode = initialEpisode
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onExitPlayer()
            (activity as? MainActivity)?.disableAutoPip()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                val isDown = event.type == KeyEventType.KeyDown
                val isUp = event.type == KeyEventType.KeyUp

                val isSeekLeft = event.key == Key.DirectionLeft || keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT
                val isSeekRight = event.key == Key.DirectionRight || keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                val isUpKey = event.key == Key.DirectionUp || keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val isDownKey = event.key == Key.DirectionDown || keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val isPlayPauseKey = event.key == Key.DirectionCenter ||
                        keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                        event.key == Key.Enter || keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                        event.key == Key.NumPadEnter || keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
                        event.key == Key.Spacebar || keyCode == AndroidKeyEvent.KEYCODE_SPACE ||
                        event.key == Key.MediaPlayPause || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == AndroidKeyEvent.KEYCODE_HEADSETHOOK
                val isMediaPlay = event.key == Key.MediaPlay || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_PLAY
                val isMediaPause = event.key == Key.MediaPause || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_PAUSE
                val isMediaStop = event.key == Key.MediaStop || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_STOP
                val isFastForward = event.key == Key.MediaFastForward ||
                        keyCode == AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                        keyCode == AndroidKeyEvent.KEYCODE_MEDIA_STEP_FORWARD
                val isRewind = event.key == Key.MediaRewind ||
                        keyCode == AndroidKeyEvent.KEYCODE_MEDIA_REWIND ||
                        keyCode == AndroidKeyEvent.KEYCODE_MEDIA_STEP_BACKWARD
                val isNextKey = event.key == Key.MediaNext || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_NEXT
                val isPrevKey = event.key == Key.MediaPrevious || keyCode == AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS
                val isBackOrEscape = event.key == Key.Escape || keyCode == AndroidKeyEvent.KEYCODE_ESCAPE ||
                        event.key == Key.Back || keyCode == AndroidKeyEvent.KEYCODE_BACK

                if (isUp) {
                    return@onKeyEvent isSeekLeft || isSeekRight || isUpKey || isDownKey ||
                            isPlayPauseKey || isMediaPlay || isMediaPause || isMediaStop ||
                            isFastForward || isRewind || isNextKey || isPrevKey || isBackOrEscape
                }

                if (!isDown) return@onKeyEvent false

                if (isBackOrEscape) {
                    handleBackNavigation()
                    return@onKeyEvent true
                }

                if (uiState.isScreenLocked) {
                    if (isSeekLeft || isSeekRight || isUpKey || isDownKey || isPlayPauseKey ||
                        isMediaPlay || isMediaPause || isMediaStop || isFastForward ||
                        isRewind || isNextKey || isPrevKey
                    ) {
                        triggerShowUnlockButton()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }

                val baseStepMs = (uiState.seekIntervalSeconds * 1000L).coerceAtLeast(5000L)
                val multiplier = when {
                    event.nativeKeyEvent.repeatCount >= 10 -> 4L
                    event.nativeKeyEvent.repeatCount >= 5 -> 2L
                    else -> 1L
                }
                val seekDeltaMs = baseStepMs * multiplier

                when {
                    isSeekLeft -> {
                        viewModel.seekRelative(-seekDeltaMs)
                        viewModel.showControls()
                        true
                    }
                    isSeekRight -> {
                        viewModel.seekRelative(seekDeltaMs)
                        viewModel.showControls()
                        true
                    }
                    isUpKey -> {
                        if (!uiState.controlsVisible) {
                            viewModel.showControls()
                        }
                        true
                    }
                    isDownKey -> {
                        if (uiState.controlsVisible) {
                            viewModel.hideControls()
                            true
                        } else {
                            false
                        }
                    }
                    isPlayPauseKey -> {
                        viewModel.togglePlayPause()
                        true
                    }
                    isMediaPlay -> {
                        viewModel.play()
                        true
                    }
                    isMediaPause -> {
                        viewModel.pause()
                        true
                    }
                    isMediaStop -> {
                        viewModel.pause()
                        true
                    }
                    isFastForward -> {
                        viewModel.seekRelative(baseStepMs)
                        viewModel.showControls()
                        true
                    }
                    isRewind -> {
                        viewModel.seekRelative(-baseStepMs)
                        viewModel.showControls()
                        true
                    }
                    isNextKey -> {
                        if (uiState.hasNextEpisode) {
                            viewModel.playNextEpisode()
                        }
                        true
                    }
                    isPrevKey -> {
                        if (uiState.hasPrevEpisode) {
                            viewModel.playPrevEpisode()
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Multi-episode check supporting both TV series and episodic anime
        val isMultiEpisode = uiState.mediaType == MediaType.TV_SERIES ||
            (uiState.mediaType == MediaType.ANIME && (uiState.seasonNumber != null || uiState.availableEpisodes.isNotEmpty()))

        // Layer 0: Startup Backdrop
        val showBackdrop = (!uiState.isPlaying && uiState.currentPositionMs == 0L) || uiState.isLoading
        if (showBackdrop && !uiState.backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = uiState.backdropUrl,
                contentDescription = null, // decorative
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.35f }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.50f))
            )
        }

        // Layer 1: ExoPlayer Video Surface
        val exoPlayer = viewModel.getPlayer()
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        subtitleView?.visibility = android.view.View.GONE
                        resizeMode = when (uiState.aspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    if (view.subtitleView?.visibility != android.view.View.GONE) {
                        view.subtitleView?.visibility = android.view.View.GONE
                    }
                    val targetResizeMode = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    if (view.resizeMode != targetResizeMode) {
                        view.resizeMode = targetResizeMode
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = videoSurfaceScale
                        scaleY = videoSurfaceScale
                    }
            )
        }

        // Slight 25% black dim when video is paused
        if (!uiState.isPlaying && !uiState.isLoading && !uiState.isBuffering && !isInPip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.25f))
            )
        }

        if (!isInPip) {
            // Layer 2: Gesture Detection Matrix (Taps, Pinch Zoom, Double-Tap Seek, Brightness & Volume Swipes, Hold to 2X)
            val isGesturesPaused = uiState.isEpisodeSelectorOpen || uiState.activeSidePanel != PlayerSidePanelType.NONE
            PlayerGestureOverlay(
                controlsVisible = uiState.controlsVisible,
                isHolding2x = uiState.isFastForwarding2x,
                isGesturesPaused = isGesturesPaused,
                isScreenLocked = uiState.isScreenLocked,
                currentAspectRatioMode = uiState.aspectRatioMode,
                onHold2xStart = { viewModel.onHold2xStart() },
                onHold2xEnd = { viewModel.onHold2xEnd() },
                onSingleTap = { viewModel.toggleControls() },
                onDoubleTapSeek = { deltaMs -> viewModel.seekRelative(deltaMs) },
                onPinchAspect = { mode, label -> viewModel.triggerPinchAspect(mode, label) },
                onPinchBadgeLabel = { label -> viewModel.showPinchBadge(label) },
                onPinchScaleChanged = { scale -> videoSurfaceScale = scale },
                onLockedScreenTap = { triggerShowUnlockButton() },
                seekIntervalSeconds = uiState.seekIntervalSeconds,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 3: Clean Subtitle Overlay
            SubtitleOverlay(
                text = currentCue?.text ?: subtitleText,
                alignment = currentCue?.alignment ?: Alignment.BottomCenter,
                controlsVisible = uiState.controlsVisible,
                fontSizeSp = uiState.subtitleSizeSp,
                backgroundEnabled = uiState.subtitleBackgroundEnabled,
                subtitleColor = uiState.subtitleColor,
                subtitleOutline = uiState.subtitleOutline,
                hasResumeNotice = uiState.resumeNotice != null,
                modifier = Modifier.align(currentCue?.alignment ?: Alignment.BottomCenter)
            )

            // Layer 4: Playback Error Screen (Section 5: safe/primary = solid white pill, secondary = glass pill, no red)
            val activeError = uiState.userError ?: uiState.errorMessage?.let { UserError.SourceUnavailable }
            if (activeError != null && !uiState.isLoading && !uiState.isBuffering && !uiState.isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (activeError is UserError.NoSources) "No Stream Providers Installed" else stringResource(R.string.error_source_failed_title),
                            style = PlayerTokens.SheetHeadline.copy(fontSize = 20.sp),
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeError is UserError.NoSources) "Install a provider plugin or Stremio addon in settings to stream content." else stringResource(R.string.error_source_failed_hint),
                            style = PlayerTokens.HeaderSubtitle,
                            color = Color.White.copy(alpha = 0.70f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Primary Action: Solid White Pill
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(PlayerTokens.RadiusPill)
                                    .background(Color.White)
                                    .playerPressable(
                                        isWhiteElement = true,
                                        role = Role.Button,
                                        onClick = {
                                            if (activeError is UserError.NoSources && onNavigateToPluginManager != null) {
                                                onNavigateToPluginManager()
                                            } else if (uiState.availableServers.isNotEmpty()) {
                                                viewModel.openSidePanel(PlayerSidePanelType.SOURCES)
                                            } else {
                                                viewModel.retry()
                                            }
                                        }
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (activeError is UserError.NoSources && onNavigateToPluginManager != null) "Open Addons / Plugins" else stringResource(R.string.action_try_another_source),
                                    style = PlayerTokens.RowTitle.copy(fontSize = 14.sp),
                                    color = PlayerTokens.LabelDark
                                )
                            }
                            // Secondary Action: Video-Overlay Glass Pill
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                                    .playerPressable(
                                        role = Role.Button,
                                        onClick = onBack
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.action_go_back),
                                    style = PlayerTokens.RowTitle.copy(fontSize = 14.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Layer 5: Top & Bottom Soft Gradients
            val showHud = uiState.controlsVisible && !uiState.isScreenLocked
            PlayerTopGradient(
                visible = showHud,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            PlayerBottomGradient(
                visible = showHud,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Layer 6: Top Bar
            PlayerTopBar(
                visible = showHud,
                isScrubbing = uiState.isScrubbing,
                title = uiState.mediaTitle,
                subtitle = uiState.episodeTitle,
                isTvSeries = isMultiEpisode,
                isLiveStream = uiState.isLiveStream,
                seasonNumber = uiState.seasonNumber,
                episodeNumber = uiState.episodeNumber,
                isEpisodeSelectorOpen = uiState.isEpisodeSelectorOpen,
                isOfflinePlayback = uiState.isOfflinePlayback,
                selectedServer = uiState.selectedServer,
                isScreenLocked = uiState.isScreenLocked,
                onToggleLock = { viewModel.setScreenLocked(!uiState.isScreenLocked) },
                onToggleEpisodeSelector = { viewModel.toggleEpisodeSelector() },
                onAudioSubtitlesClick = { viewModel.openSidePanel(PlayerSidePanelType.AUDIO_SUBTITLES) },
                onCastClick = { showCastPickerSheet = true },
                onOverflowClick = { viewModel.openSidePanel(PlayerSidePanelType.SETTINGS) },
                isPipSupported = isPipSupported,
                onEnterPip = {
                    val mainAct = activity as? MainActivity
                    if (mainAct != null) {
                        mainAct.enterPipMode()
                    } else {
                        PipController.requestPip()
                    }
                },
                onBack = {
                    when {
                        showCastPickerSheet -> showCastPickerSheet = false
                        uiState.isEpisodeSelectorOpen -> viewModel.setEpisodeSelectorOpen(false)
                        uiState.activeSidePanel != PlayerSidePanelType.NONE -> viewModel.closeSidePanel()
                        uiState.controlsVisible -> viewModel.hideControls()
                        else -> onBack()
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Layer 6.5: Mid-Top Format Badges (Dolby Vision, HDR10+, Dolby Audio / Atmos, 4K)
            var showFormatBadges by remember { mutableStateOf(false) }

            LaunchedEffect(showHud, uiState.mediaFormatInfo) {
                if (!showHud && !uiState.isScreenLocked && uiState.mediaFormatInfo.hasAnyBadge) {
                    showFormatBadges = true
                    delay(5000L)
                    showFormatBadges = false
                } else {
                    showFormatBadges = false
                }
            }

            PlayerFormatBadges(
                formatInfo = uiState.mediaFormatInfo,
                visible = showFormatBadges && !uiState.isFastForwarding2x,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 24.dp)
            )

            // Layer 7: Center Cluster
            PlayerCenterCluster(
                visible = showHud && !uiState.isScreenLocked,
                isScrubbing = uiState.isScrubbing,
                isPlaying = uiState.isPlaying,
                isLoading = uiState.isLoading,
                isBuffering = uiState.isBuffering,
                seekIntervalSeconds = uiState.seekIntervalSeconds,
                isLiveStream = uiState.isLiveStream,
                hasNextEpisode = isMultiEpisode && uiState.hasNextEpisode,
                onNextEpisode = { viewModel.playNextEpisode() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekRelative = { deltaMs -> viewModel.seekRelative(deltaMs) },
                onTryAnotherSource = { viewModel.openSidePanel(PlayerSidePanelType.SOURCES) },
                playPauseFocusRequester = playPauseFocusRequester,
                isP2p = uiState.isP2pStream,
                p2pPhaseLabel = uiState.p2pPhaseLabel,
                p2pPeers = uiState.p2pPeers,
                p2pSeeds = uiState.p2pSeeds,
                p2pDownloadSpeed = uiState.p2pDownloadSpeed,
                modifier = Modifier.align(Alignment.Center)
            )

            // Layer 7.5: Standalone Centered 56 dp Glass Buffering Spinner (Section 3.3)
            // When controls are hidden and video is buffering/loading
            if (!showHud && (uiState.isBuffering || uiState.isLoading) && !uiState.isScreenLocked) {
                BufferingSpinner(
                    isStartup = uiState.isLoading,
                    onTryAnotherSource = { viewModel.openSidePanel(PlayerSidePanelType.SOURCES) },
                    isP2p = uiState.isP2pStream,
                    p2pPhaseLabel = uiState.p2pPhaseLabel,
                    p2pPeers = uiState.p2pPeers,
                    p2pSeeds = uiState.p2pSeeds,
                    p2pDownloadSpeed = uiState.p2pDownloadSpeed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Layer 8: Bottom Controls
            PlayerBottomControls(
                visible = showHud,
                isScrubbing = uiState.isScrubbing,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                bufferedPositionMs = uiState.bufferedPositionMs,
                scrubPositionMs = uiState.scrubPositionMs,
                playbackSpeed = uiState.playbackSpeed,
                isTvSeries = isMultiEpisode,
                isLiveStream = uiState.isLiveStream,
                hasNextEpisode = uiState.hasNextEpisode,
                mediaTitle = uiState.mediaTitle,
                episodeTitle = uiState.episodeTitle,
                seasonNumber = uiState.seasonNumber,
                episodeNumber = uiState.episodeNumber,
                serverName = uiState.selectedServer?.serverName,
                showRemainingTime = showRemainingTime,
                aspectRatioMode = uiState.aspectRatioMode,
                selectedServer = uiState.selectedServer,
                selectedQuality = uiState.selectedQuality,
                availableServers = uiState.availableServers,
                activeSidePanel = uiState.activeSidePanel,
                sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
                onToggleRemainingTime = { showRemainingTime = !showRemainingTime },
                onScrubStart = { pos -> viewModel.onScrubStart(pos) },
                onScrubProgress = { pos -> viewModel.onScrubProgress(pos) },
                onScrubEnd = { pos -> viewModel.onScrubEnd(pos) },
                onOpenEpisodes = {
                    viewModel.closeSidePanel()
                    viewModel.setEpisodeSelectorOpen(true)
                },
                onOpenSources = { viewModel.openSidePanel(PlayerSidePanelType.SOURCES) },
                onSelectServer = { server -> viewModel.selectServer(server) },
                onOpenSpeed = { viewModel.openSidePanel(PlayerSidePanelType.SPEED) },
                onOpenAudioSubtitles = { viewModel.openSidePanel(PlayerSidePanelType.AUDIO) },
                onOpenAudio = { viewModel.openSidePanel(PlayerSidePanelType.AUDIO) },
                onOpenSubtitles = { viewModel.openSidePanel(PlayerSidePanelType.SUBTITLES) },
                onOpenQuality = { viewModel.openSidePanel(PlayerSidePanelType.VIDEO_QUALITY) },
                onEnterPip = {
                    if (isPipSupported) {
                        try {
                            PipController.onRequestPip?.invoke() ?: (activity as? MainActivity)?.enterPipMode()
                        } catch (_: Exception) {}
                    }
                },
                onOpenCast = { showCastPickerSheet = true },
                onOpenExternalPlayer = { showCastPickerSheet = true },
                onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                onSelectAspectRatio = { mode -> viewModel.setAspectRatio(mode) },
                onSelectSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                onLockScreen = { viewModel.setScreenLocked(true) },
                onOpenSettings = { viewModel.openSidePanel(PlayerSidePanelType.SETTINGS) },
                onNextEpisode = { viewModel.playNextEpisode() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Layer 9.5: Unlock Button
            PlayerUnlockCircle(
                visible = uiState.isScreenLocked && showUnlockButton,
                onUnlocked = {
                    viewModel.setScreenLocked(false)
                    showUnlockButton = false
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            )

            // Layer 10: Pinch-to-Zoom Aspect Ratio Badge
            PinchAspectBadge(
                label = if (!uiState.isFastForwarding2x) uiState.pinchAspectToast else null,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Layer 10.5: Series Episode Selector Dropdown Panel
            if (isMultiEpisode) {
                EpisodeSelectorPanel(
                    isOpen = showHud && uiState.isEpisodeSelectorOpen,
                    currentSeason = uiState.seasonNumber,
                    currentEpisode = uiState.episodeNumber,
                    selectedSeasonInPanel = uiState.selectedSeasonForSelector,
                    availableSeasons = uiState.availableSeasons,
                    episodes = uiState.availableEpisodes,
                    playbackMap = uiState.episodePlaybackMap,
                    isLoading = uiState.isEpisodesLoading,
                    onSeasonSelect = { season -> viewModel.selectSeasonInSelector(season) },
                    onEpisodeSelect = { ep -> viewModel.selectEpisode(ep) },
                    onDismiss = { viewModel.setEpisodeSelectorOpen(false) },
                    onInteraction = { viewModel.resetAutoHideTimer(8000L) },
                    modifier = Modifier.fillMaxSize().zIndex(15f)
                )
            }

            // Layer 11: Side Panel for Choices (Speed, Audio & Subtitles, Settings, Sleep Timer)
            PlayerSidePanel(
                activePanel = uiState.activeSidePanel,
                playbackSpeed = uiState.playbackSpeed,
                onSelectSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                audioTracks = uiState.availableAudioTracks,
                selectedAudioTrack = uiState.selectedAudioTrack,
                onSelectAudioTrack = { track -> viewModel.selectAudioTrack(track) },
                dialogueBoostLevel = uiState.dialogueBoostLevel,
                onSelectDialogueBoost = { boost -> viewModel.setDialogueBoostLevel(boost) },
                isNightModeDrcEnabled = uiState.isNightModeDrcEnabled,
                onToggleNightModeDrc = { enabled -> viewModel.setNightModeDrc(enabled) },
                selectedSleepTimer = uiState.selectedSleepTimer,
                sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
                onSelectSleepTimer = { opt -> viewModel.setSleepTimer(opt) },
                subtitles = uiState.availableSubtitles,
                selectedSubtitle = uiState.selectedSubtitle,
                onSelectSubtitle = { sub -> viewModel.selectSubtitle(sub) },
                subtitleOffsetMs = uiState.subtitleOffsetMs,
                onAdjustSubtitleOffset = { delta -> viewModel.adjustSubtitleOffset(delta) },
                onResetSubtitleOffset = { viewModel.resetSubtitleOffset() },
                isSearchingExternalSubtitles = uiState.isSearchingExternalSubtitles,
                onSearchExternalSubtitles = { viewModel.searchExternalSubtitles() },
                subtitleSizeSp = uiState.subtitleSizeSp,
                subtitleColor = uiState.subtitleColor,
                subtitleBackgroundEnabled = uiState.subtitleBackgroundEnabled,
                subtitleOutline = uiState.subtitleOutline,
                onApplySubtitlePreset = { preset -> viewModel.applySubtitlePreset(preset) },
                isSearchingStreams = uiState.isSearchingStreams,
                servers = uiState.availableServers,
                selectedServer = uiState.selectedServer,
                onSelectServer = { server ->
                    viewModel.selectServer(server)
                    viewModel.closeSidePanel()
                },
                qualities = uiState.availableQualities,
                selectedQuality = uiState.selectedQuality,
                onSelectQuality = { q -> viewModel.selectQuality(q) },
                aspectRatioMode = uiState.aspectRatioMode,
                onSelectAspectRatio = { ar -> viewModel.setAspectRatio(ar) },
                onOpenSleepTimer = { viewModel.openSidePanel(PlayerSidePanelType.SLEEP_TIMER) },
                isPipSupported = isPipSupported,
                onEnterPip = {
                    PipController.onRequestPip?.invoke() ?: (activity as? MainActivity)?.enterPipMode()
                },
                onLockScreen = { viewModel.setScreenLocked(true) },
                seekIntervalSeconds = uiState.seekIntervalSeconds,
                onSelectSeekInterval = { sec -> viewModel.setSeekInterval(sec) },
                smartFailoverEnabled = uiState.smartFailoverEnabled,
                onToggleSmartFailover = { enabled -> viewModel.setSmartFailoverEnabled(enabled) },
                episodes = uiState.availableEpisodes,
                availableSeasons = uiState.availableSeasons,
                selectedSeason = uiState.selectedSeasonForSelector,
                onSelectSeason = { season -> viewModel.selectSeasonInSelector(season) },
                currentEpisodeNumber = uiState.episodeNumber,
                onSelectEpisode = { ep ->
                    viewModel.selectEpisode(ep)
                    viewModel.closeSidePanel()
                },
                isEpisodesLoading = uiState.isEpisodesLoading,
                playbackMap = uiState.episodePlaybackMap,
                isTvSeries = isMultiEpisode,
                onDismiss = { viewModel.closeSidePanel() },
                onInteraction = { viewModel.resetAutoHideTimer() },
                modifier = Modifier.zIndex(20f)
            )

            val controlsVisible = uiState.controlsVisible
            val baseOverlayBottomPadding by animateDpAsState(
                targetValue = if (controlsVisible) 176.dp else 36.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "overlayBottomPadding"
            )

            // Layer 11.5: Up Next Episode Countdown Card
            UpNextCountdownCard(
                visible = uiState.showUpNextCard && !uiState.isLiveStream,
                nextEpisode = uiState.upNextEpisode,
                countdownSeconds = uiState.upNextCountdownSeconds,
                onPlayNow = { viewModel.playUpNextNow() },
                onDismiss = { viewModel.dismissUpNextCard() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = baseOverlayBottomPadding)
                    .zIndex(18f)
            )

            // Layer 12: Transient Frosted Glass Resume Notice Snackbar
            ResumeNoticeSnackbar(
                resumeNotice = uiState.resumeNotice,
                onStartOver = {
                    viewModel.seekTo(0L)
                    viewModel.dismissResumeNotice()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = baseOverlayBottomPadding)
            )

            // Layer 12.5: In-Player Automatic Failover Notification Pill
            FailoverPill(
                notice = if (!uiState.isFastForwarding2x) uiState.failoverNotice else null,
                controlsVisible = showHud,
                onDismiss = { viewModel.dismissFailoverNotice() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(19f)
            )

            // Layer 12.6: Pre-Play Quality Conflict Alert Dialog
            uiState.qualityConflictNotice?.let { conflict ->
                val reqMbps = (conflict.requiredBitrateBps / 1_000_000L).coerceAtLeast(1)
                val estMbps = (conflict.estimatedBitrateBps / 1_000_000L).coerceAtLeast(1)
                VelvetAlertDialog(
                    onDismissRequest = { viewModel.dismissQualityConflictNotice() },
                    title = "Bandwidth Warning",
                    message = "Streaming in ${conflict.requestedQuality} requires ~$reqMbps Mbps, but your current network bandwidth is estimated at ~$estMbps Mbps. This may cause frequent buffering stalls.",
                    confirmText = "Switch to ${conflict.recommendedQuality}",
                    dismissText = "Continue Anyway",
                    icon = Icons.Outlined.WifiOff,
                    onConfirm = { viewModel.confirmSwitchToRecommendedQuality() },
                    onDismiss = { viewModel.continueAnywayWithConflictQuality() }
                )
            }

            // Layer 13: Toast Message
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
                exit = fadeOut(tween(250)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                uiState.toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .clip(ShapeFull)
                            .background(SurfaceMid.copy(alpha = 0.85f))
                            .border(HairlineWidth, Outline, ShapeFull)
                            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing8)
                    ) {
                        Text(
                            text = msg,
                            style = AppTypography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Layer 13.5: Skip Intro / Skip Outro Floating Overlay (with 6s perimeter countdown)
            val isOutro = uiState.showSkipOutro
            val isIntroOrOutroVisible = (uiState.showSkipIntro || uiState.showSkipOutro) && !uiState.isScreenLocked && !uiState.isLiveStream
            val skipAlignment = if (isOutro && uiState.showUpNextCard) Alignment.BottomStart else Alignment.BottomEnd

            SkipIntroOverlay(
                visible = isIntroOrOutroVisible,
                isOutro = isOutro,
                skipLabel = if (isOutro) "Skip Outro" else uiState.skipIntroLabel,
                onSkip = {
                    if (isOutro) viewModel.skipOutro() else viewModel.skipIntro()
                },
                onDismiss = { viewModel.dismissSkipIntro() },
                modifier = Modifier
                    .align(skipAlignment)
                    .padding(
                        start = if (skipAlignment == Alignment.BottomStart) 24.dp else 0.dp,
                        end = if (skipAlignment == Alignment.BottomEnd) 24.dp else 0.dp,
                        bottom = if (skipAlignment == Alignment.BottomEnd && uiState.showUpNextCard) {
                            baseOverlayBottomPadding + 76.dp
                        } else {
                            baseOverlayBottomPadding
                        }
                    )
                    .zIndex(18f)
            )

            // Layer 14: Cast & External Playback Picker Sheet
            if (showCastPickerSheet) {
                CastPickerSheet(
                    streamSource = uiState.selectedServer?.streamSource,
                    mediaTitle = uiState.mediaTitle,
                    onDismiss = { showCastPickerSheet = false }
                )
            }
        }
    }
}

/**
 * Modern Subtitle Overlay with smooth gliding animation
 */
@Composable
private fun SubtitleOverlay(
    text: String?,
    alignment: Alignment = Alignment.BottomCenter,
    controlsVisible: Boolean,
    fontSizeSp: Int = 18,
    backgroundEnabled: Boolean = false,
    subtitleColor: String = "WHITE",
    subtitleOutline: Boolean = true,
    hasResumeNotice: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isTop = alignment == Alignment.TopCenter || alignment == Alignment.TopStart || alignment == Alignment.TopEnd
    val targetBottomPadding = when {
        controlsVisible && hasResumeNotice -> 140.dp
        controlsVisible -> 96.dp
        hasResumeNotice -> 72.dp
        else -> 28.dp
    }
    val reduceMotion = LocalReduceMotion.current
    val bottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = if (reduceMotion) snap() else tween(Motion.DurationStandard, easing = Motion.StandardEasing),
        label = "subBottomPadding"
    )
    val topPadding by animateDpAsState(
        targetValue = if (controlsVisible) 76.dp else 24.dp,
        animationSpec = if (reduceMotion) snap() else tween(Motion.DurationStandard, easing = Motion.StandardEasing),
        label = "subTopPadding"
    )

    // Subtitle colour presets (user content, not UI chrome)
    val textColor = when (subtitleColor.uppercase()) {
        "YELLOW" -> SubtitleColors.Yellow
        "CYAN" -> SubtitleColors.Cyan
        "GREEN" -> SubtitleColors.Green
        else -> SubtitleColors.White
    }

    val textShadow = if (subtitleOutline) {
        Shadow(
            color = Background.copy(alpha = 0.95f),
            offset = Offset(0f, 1.5f),
            blurRadius = if (backgroundEnabled) 3f else 6f
        )
    } else {
        Shadow(color = Color.Transparent, offset = Offset.Zero, blurRadius = 0f)
    }

    val cleanText = remember(text) {
        if (text.isNullOrBlank()) ""
        else SubtitleParser.sanitizeSubtitleText(text)
    }

    AnimatedVisibility(
        visible = cleanText.isNotBlank(),
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
        modifier = modifier
            .padding(
                start = 48.dp,
                end = 48.dp,
                top = if (isTop) topPadding else 0.dp,
                bottom = if (!isTop) bottomPadding else 0.dp
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
            val containerModifier = if (backgroundEnabled) {
                Modifier
                    .clip(ShapeS)
                    .background(Background.copy(alpha = 0.60f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            } else {
                Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            }

            Box(
                modifier = containerModifier
            ) {
                val effectiveSize = fontSizeSp.coerceIn(12, 28)
                Text(
                    text = cleanText,
                    color = textColor,
                    fontSize = effectiveSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    lineHeight = (effectiveSize * 1.36f).sp,
                    textAlign = when (alignment) {
                        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> TextAlign.Start
                        Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    style = TextStyle(
                        shadow = textShadow
                    )
                )
            }
        }
    }
}

@Composable
fun UpNextCountdownCard(
    visible: Boolean,
    nextEpisode: EpisodeItem?,
    countdownSeconds: Int,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && nextEpisode != null,
        enter = fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 2 },
        exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        if (nextEpisode == null) return@AnimatedVisibility

        Box(
            modifier = Modifier
                .width(320.dp)
                .videoOverlayGlass(PlayerTokens.RadiusMedium)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up next in ${countdownSeconds}s",
                        color = Color.White.copy(alpha = 0.70f),
                        style = PlayerTokens.SheetSubtitle
                    )
                    Box(
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = "Dismiss next episode overlay"
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .videoOverlayGlass(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 45.dp)
                            .clip(PlayerTokens.RadiusSmall)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(HairlineWidth, PlayerTokens.StandardGlassBorder, PlayerTokens.RadiusSmall)
                    ) {
                        AppAsyncImage(
                            model = nextEpisode.thumbnail,
                            contentDescription = null, // decorative - next episode title conveys content
                            contentScale = ContentScale.Crop,
                            titleForFallback = "E${nextEpisode.episodeNumber}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "S${nextEpisode.seasonNumber} · E${nextEpisode.episodeNumber}",
                            color = Color.White.copy(alpha = 0.70f),
                            style = PlayerTokens.RowMeta
                        )
                        Text(
                            text = nextEpisode.title.ifBlank { "Episode ${nextEpisode.episodeNumber}" },
                            color = Color.White,
                            style = PlayerTokens.RowTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Solid white primary pill with near-black label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .playerPressable(
                            isWhiteElement = true,
                            role = Role.Button,
                            onClickLabel = "Play now"
                        ) {
                            onPlayNow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = PlayerTokens.LabelDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play now",
                            color = PlayerTokens.LabelDark,
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}
