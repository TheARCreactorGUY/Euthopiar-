package com.euthopiar.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.R
import com.euthopiar.ui.components.SecondaryButton
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressScale
import com.euthopiar.ui.theme.pressable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Formats milliseconds to "MM:SS" or "H:MM:SS" timecode.
 */
fun formatTimecode(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Top Scrim Gradient (Section 1):
 * Black ~55% at top edge fading to 0 by 30% of screen height.
 */
@Composable
fun PlayerTopGradient(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut)),
        exit = fadeOut(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(PlayerTokens.TopScrim)
        )
    }
}

/**
 * Bottom Scrim Gradient (Section 1):
 * Black fading from 0 at ~50% height to ~78% at bottom edge.
 */
@Composable
fun PlayerBottomGradient(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut)),
        exit = fadeOut(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(PlayerTokens.BottomScrim)
        )
    }
}

/**
 * Top Header (Section 3.1):
 * Left: 44 dp circle in Video-Overlay Glass, back arrow outline 22 dp.
 * 12 dp gap. Title block: title (15 sp medium), subtitle (12 sp).
 * Right: row of 44 dp circular Video-Overlay Glass buttons (10 dp gap):
 * extra button (group/watch-together), PiP, Cast, Screen Lock.
 * Outline icons only at 20 dp.
 * Motion: 200 ms ease-out (fade in + 8 dp slide down), 180 ms ease-in (fade out + 8 dp slide up).
 */
@Composable
fun PlayerTopBar(
    visible: Boolean,
    isScrubbing: Boolean,
    title: String,
    subtitle: String?,
    isTvSeries: Boolean,
    seasonNumber: Int?,
    episodeNumber: Int?,
    isEpisodeSelectorOpen: Boolean = false,
    isOfflinePlayback: Boolean = false,
    selectedServer: ServerSourceOption? = null,
    isScreenLocked: Boolean = false,
    onToggleLock: (() -> Unit)? = null,
    onServerClick: () -> Unit = {},
    onToggleEpisodeSelector: () -> Unit = {},
    onAudioSubtitlesClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
    onOverflowClick: () -> Unit = {},
    isPipSupported: Boolean = false,
    onEnterPip: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val scrubAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 0.04f else 1f,
        animationSpec = tween(150),
        label = "topBarScrubAlpha"
    )

    val enterTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeIn(tween(PlayerTokens.ReduceMotionFadeInMs))
        } else {
            val slidePx = with(density) { PlayerTokens.ControlsSlideOffset.roundToPx() }
            slideInVertically(
                initialOffsetY = { -slidePx },
                animationSpec = tween(PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut)
            ) + fadeIn(tween(PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut))
        }
    }

    val exitTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeOut(tween(PlayerTokens.ReduceMotionFadeOutMs))
        } else {
            val slidePx = with(density) { PlayerTokens.ControlsSlideOffset.roundToPx() }
            slideOutVertically(
                targetOffsetY = { -slidePx },
                animationSpec = tween(PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn)
            ) + fadeOut(tween(PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .graphicsLayer { alpha = scrubAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button (44 dp circle in Video-Overlay Glass, back arrow outline 22 dp)
            Box(
                modifier = Modifier
                    .size(PlayerTokens.SizeHeaderButton)
                    .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                    .tvFocusable(shape = PlayerTokens.RadiusCircle)
                    .playerPressable(
                        role = Role.Button,
                        onClickLabel = "Back",
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(PlayerTokens.IconBackArrow)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title block (15 sp medium, 12 sp subtitle)
            Column(
                modifier = Modifier.weight(1f, fill = true)
            ) {
                Text(
                    text = title,
                    style = PlayerTokens.HeaderTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle: year / S1 · E4 + server + quality
                val metaParts = mutableListOf<String>()
                if (isTvSeries) {
                    val s = seasonNumber ?: 1
                    val e = episodeNumber ?: 1
                    metaParts.add("S$s · E$e")
                }
                if (!subtitle.isNullOrBlank() && !metaParts.contains(subtitle)) {
                    metaParts.add(subtitle)
                }
                selectedServer?.serverName?.let { sName ->
                    if (sName.isNotBlank() && !metaParts.contains(sName)) {
                        metaParts.add("Via $sName")
                    }
                }
                selectedServer?.resolutionLabel?.let { res ->
                    if (res.isNotBlank()) metaParts.add(res)
                }

                val subtitleLine = metaParts.joinToString(" · ")
                if (subtitleLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subtitleLine,
                            style = PlayerTokens.HeaderSubtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOfflinePlayback) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DownloadDone,
                                        contentDescription = "Offline playback",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Offline",
                                        style = PlayerTokens.TooltipLabel,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Row of 44 dp circular Video-Overlay Glass buttons (10 dp gaps)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Picture-in-Picture button
                if (isPipSupported && onEnterPip != null) {
                    Box(
                        modifier = Modifier
                            .size(PlayerTokens.SizeHeaderButton)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                            .tvFocusable(shape = PlayerTokens.RadiusCircle)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = "Picture in Picture",
                                onClick = onEnterPip
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_player_pip),
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                            modifier = Modifier.size(PlayerTokens.IconHeader)
                        )
                    }
                }

                // Cast button
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeHeaderButton)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                        .tvFocusable(shape = PlayerTokens.RadiusCircle)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = "Cast and External Playback",
                            onClick = onCastClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_player_cast),
                        contentDescription = "Cast and External Playback",
                        tint = Color.White,
                        modifier = Modifier.size(PlayerTokens.IconHeader)
                    )
                }

                // Screen Lock button
                if (onToggleLock != null) {
                    Box(
                        modifier = Modifier
                            .size(PlayerTokens.SizeHeaderButton)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                            .tvFocusable(shape = PlayerTokens.RadiusCircle)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = if (isScreenLocked) "Unlock Player" else "Lock Player",
                                onClick = onToggleLock
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isScreenLocked) R.drawable.ic_player_unlock else R.drawable.ic_player_lock
                            ),
                            contentDescription = if (isScreenLocked) "Unlock Player" else "Lock Player",
                            tint = Color.White,
                            modifier = Modifier.size(PlayerTokens.IconHeader)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Format Badges (4K, Dolby Vision, Atmos, HDR).
 */
@Composable
fun PlayerFormatBadges(
    formatInfo: MediaFormatInfo,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!formatInfo.hasAnyBadge) return
    val reduceMotion = LocalReduceMotion.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else 300)),
        exit = fadeOut(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else 300)),
        modifier = modifier
    ) {
        val badges = remember(formatInfo) {
            val list = mutableListOf<String>()
            formatInfo.resolutionBadge?.let { list.add(it) }
            formatInfo.dynamicRangeBadge?.let { list.add(it) }
            formatInfo.audioBadge?.let { list.add(it) }
            list
        }

        if (badges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEachIndexed { index, badge ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    }
                    Text(
                        text = badge,
                        style = PlayerTokens.PillText,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Center Playback Cluster (Section 3.2 & 3.3):
 * Horizontally centered at ~43% height.
 * Three elements: Rewind 10s (56 dp glass), Play/Pause (76 dp solid white circle), Forward 10s (56 dp glass).
 * Portrait: Play 68 dp, side buttons 48 dp, gaps 32 dp.
 * Buffering: thin spinner (2.5 dp stroke, 36 dp arc, 1.1s linear), 76 dp faint outer ring. Rewind/forward dim to 40%.
 */
@Composable
fun PlayerCenterCluster(
    visible: Boolean,
    isScrubbing: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    isBuffering: Boolean,
    seekIntervalSeconds: Int,
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    onPreviousEpisode: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (deltaMs: Long) -> Unit,
    onTryAnotherSource: (() -> Unit)? = null,
    playPauseFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    isP2p: Boolean = false,
    p2pPhaseLabel: String? = null,
    p2pPeers: Int = 0,
    p2pSeeds: Int = 0,
    p2pDownloadSpeed: Long = 0L,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp
    val isSpinning = isLoading || isBuffering
    val reduceMotion = LocalReduceMotion.current

    val playSize = if (isPortrait) PlayerTokens.SizePlayPausePortrait else PlayerTokens.SizePlayPauseLandscape
    val sideSize = if (isPortrait) PlayerTokens.SizeSeekButtonPortrait else PlayerTokens.SizeSeekButtonLandscape
    val gap = if (isPortrait) 32.dp else 44.dp

    val scrubAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 0.04f else 1f,
        animationSpec = tween(150),
        label = "centerScrubAlpha"
    )

    // Delay showing buffering spinner by 300 ms to prevent flashing on sub-second stalls
    var showBufferingSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            delay(PlayerTokens.BufferingDelayMs)
            showBufferingSpinner = true
        } else {
            showBufferingSpinner = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeInMs else PlayerTokens.ControlsShowDuration)),
        exit = fadeOut(tween(if (reduceMotion) PlayerTokens.ReduceMotionFadeOutMs else PlayerTokens.ControlsHideDuration)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer { alpha = scrubAlpha },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                // Optional Previous Episode (if TV series)
                if (hasPreviousEpisode) {
                    Box(
                        modifier = Modifier
                            .size(sideSize)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                            .tvFocusable(shape = PlayerTokens.RadiusCircle)
                            .playerPressable(
                                enabled = !isSpinning,
                                role = Role.Button,
                                onClickLabel = "Previous Episode",
                                onClick = onPreviousEpisode
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipPrevious,
                            contentDescription = "Previous Episode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Rewind 10 seconds button (56 dp circle, rotates 20 deg on tap)
                SeekIntervalButton(
                    isForward = false,
                    size = sideSize,
                    intervalSeconds = seekIntervalSeconds,
                    dimmed = isSpinning,
                    onClick = { onSeekRelative(-seekIntervalSeconds * 1000L) }
                )

                // Play / Pause / Buffering Primary Button
                Box(
                    modifier = Modifier.size(playSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Faint outer ring while buffering (76 dp, white 14%, 1 dp)
                    if (showBufferingSpinner) {
                        Box(
                            modifier = Modifier
                                .size(playSize)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        )
                    }

                    // Solid White Play/Pause Circle (THE ONLY solid white element)
                    Box(
                        modifier = Modifier
                            .size(playSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .tvFocusable(shape = CircleShape)
                            .then(if (playPauseFocusRequester != null) Modifier.focusRequester(playPauseFocusRequester) else Modifier)
                            .playerPressable(
                                enabled = !showBufferingSpinner,
                                isWhiteElement = true,
                                role = Role.Button,
                                onClickLabel = if (isPlaying) "Pause" else "Play",
                                onClick = onTogglePlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showBufferingSpinner) {
                            // Thin spinner: 2.5 dp stroke, 36 dp arc, linear 1.1s rotation
                            BufferingSpinnerArc()
                        } else {
                            // Play/Pause crossfade (160 ms with 90%->100% scale on incoming icon)
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (fadeIn(tween(PlayerTokens.PlayPauseCrossfadeMs)) + scaleIn(tween(PlayerTokens.PlayPauseCrossfadeMs), initialScale = 0.90f))
                                        .togetherWith(fadeOut(tween(PlayerTokens.PlayPauseCrossfadeMs)))
                                },
                                label = "PlayPauseMorph"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = PlayerTokens.LabelDark,
                                    modifier = Modifier.size(PlayerTokens.IconPlayPause)
                                )
                            }
                        }
                    }
                }

                // Forward 10 seconds button (56 dp circle, rotates 20 deg on tap)
                SeekIntervalButton(
                    isForward = true,
                    size = sideSize,
                    intervalSeconds = seekIntervalSeconds,
                    dimmed = isSpinning,
                    onClick = { onSeekRelative(seekIntervalSeconds * 1000L) }
                )

                // Optional Next Episode (if TV series)
                if (hasNextEpisode) {
                    Box(
                        modifier = Modifier
                            .size(sideSize)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                            .tvFocusable(shape = PlayerTokens.RadiusCircle)
                            .playerPressable(
                                enabled = !isSpinning,
                                role = Role.Button,
                                onClickLabel = "Next Episode",
                                onClick = onNextEpisode
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = "Next Episode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (showBufferingSpinner && isP2p) {
                    Spacer(modifier = Modifier.height(16.dp))
                    P2pSwarmTelemetryBadge(
                        phaseLabel = p2pPhaseLabel,
                        peers = p2pPeers,
                        seeds = p2pSeeds,
                        downloadSpeed = p2pDownloadSpeed
                    )
                }
            }
        }
    }
}

/**
 * 20-degree feedback rotation on seek button taps.
 */
@Composable
private fun SeekIntervalButton(
    isForward: Boolean,
    size: androidx.compose.ui.unit.Dp,
    intervalSeconds: Int,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    val reduceMotion = LocalReduceMotion.current

    val icon = when (intervalSeconds) {
        5 -> if (isForward) Icons.Filled.Forward5 else Icons.Filled.Replay5
        30 -> if (isForward) Icons.Filled.Forward30 else Icons.Filled.Replay30
        else -> if (isForward) Icons.Outlined.Forward10 else Icons.Outlined.Replay10
    }

    val alpha by animateFloatAsState(
        targetValue = if (dimmed) 0.40f else 1f,
        animationSpec = tween(PlayerTokens.BufferingFadeMs),
        label = "seekDimmedAlpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
            .tvFocusable(shape = PlayerTokens.RadiusCircle)
            .playerPressable(
                enabled = !dimmed,
                role = Role.Button,
                onClickLabel = if (isForward) "Forward 10 seconds" else "Rewind 10 seconds",
                onClick = {
                    if (!reduceMotion) {
                        scope.launch {
                            val targetDeg = if (isForward) PlayerTokens.SeekRotateDegrees else -PlayerTokens.SeekRotateDegrees
                            rotation.animateTo(targetDeg, tween(100, easing = LinearEasing))
                            rotation.animateTo(0f, tween(100, easing = LinearEasing))
                        }
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isForward) "Forward 10s" else "Rewind 10s",
            tint = Color.White,
            modifier = Modifier
                .size(PlayerTokens.IconSeekButton)
                .rotate(rotation.value)
        )
    }
}

/**
 * Thin buffering spinner (2.5 dp stroke, 36 dp arc, 1.1s linear rotation).
 */
@Composable
fun BufferingSpinnerArc(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bufferingSpinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(PlayerTokens.BufferingCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerAngle"
    )

    Canvas(modifier = modifier.size(36.dp)) {
        drawArc(
            color = PlayerTokens.LabelDark,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Centered 56 dp Video-Overlay Glass Spinner for hidden controls buffering and startup.
 */
@Composable
fun BufferingSpinner(
    isStartup: Boolean = false,
    onTryAnotherSource: (() -> Unit)? = null,
    isP2p: Boolean = false,
    p2pPhaseLabel: String? = null,
    p2pPeers: Int = 0,
    p2pSeeds: Int = 0,
    p2pDownloadSpeed: Long = 0L,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .videoOverlayGlass(shape = PlayerTokens.RadiusCircle),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "glassSpinner")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(PlayerTokens.BufferingCycleMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "glassSpinnerAngle"
            )

            Canvas(modifier = Modifier.size(36.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.90f),
                    startAngle = angle,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        if (isP2p) {
            Spacer(modifier = Modifier.height(14.dp))
            P2pSwarmTelemetryBadge(
                phaseLabel = p2pPhaseLabel,
                peers = p2pPeers,
                seeds = p2pSeeds,
                downloadSpeed = p2pDownloadSpeed
            )
        }
    }
}

@Composable
fun P2pSwarmTelemetryBadge(
    phaseLabel: String?,
    peers: Int,
    seeds: Int,
    downloadSpeed: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0B0B0F))
            .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val phase = phaseLabel ?: "Searching DHT & trackers for peers..."
            Text(
                text = phase,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            val speedStr = com.euthopiar.core.p2p.formatP2pSpeed(downloadSpeed)
            Text(
                text = "$peers Peers · $seeds Seeds · ↓ $speedStr",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compatibility alias for SeekIntervalGlyph.
 */
@Composable
fun SeekIntervalGlyph(
    isForward: Boolean,
    intervalSeconds: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SeekIntervalButton(
        isForward = isForward,
        size = 48.dp,
        intervalSeconds = intervalSeconds,
        dimmed = false,
        onClick = onClick
    )
}

/**
 * Liquid Scrubber / Timeline (Section 3.4).
 * Full width between 24 dp side margins (plus insets).
 * Track: 3 dp (grows to 5 dp when touching).
 * Knob: 12 dp white circle (grows to 16 dp when touching).
 * Touch area: 44 dp tall.
 * Tap jumps, drag scrubs and commits on release.
 */
@Composable
fun PlayerScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    showRemainingTime: Boolean = true,
    onToggleRemainingTime: () -> Unit = {},
    onScrubStart: (positionMs: Long) -> Unit,
    onScrubProgress: (positionMs: Long) -> Unit,
    onScrubEnd: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val effectiveDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentEffectiveDuration by rememberUpdatedState(effectiveDuration)
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubProgress by rememberUpdatedState(onScrubProgress)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)

    val activeFraction = if (isDragging) dragFraction else (currentPositionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
    val displayPositionMs = if (isDragging) (dragFraction * effectiveDuration).toLong() else (if (isScrubbing) scrubPositionMs else currentPositionMs)
    val bufferedFraction = (bufferedPositionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)

    val reduceMotion = LocalReduceMotion.current
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 3.dp,
        animationSpec = if (reduceMotion) snap() else tween(120),
        label = "timelineTrackHeight"
    )
    val knobSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        animationSpec = if (reduceMotion) snap() else tween(120),
        label = "timelineKnobSize"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerTokens.TouchTargetMin),
        contentAlignment = Alignment.CenterStart
    ) {
        var componentWidthPx by remember { mutableFloatStateOf(0f) }

        // Horizontal Scrub Badge (Section 3.7b) centered a bit above vertical middle
        if (isDragging && componentWidthPx > 0f) {
            val deltaMs = displayPositionMs - currentPositionMs
            val deltaSign = if (deltaMs >= 0) "+" else "-"
            val deltaFormatted = "$deltaSign${formatTimecode(Math.abs(deltaMs))}"

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-56).dp)
                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTimecode(displayPositionMs),
                        style = PlayerTokens.ScrubBadgeTime
                    )
                    Text(
                        text = deltaFormatted,
                        style = PlayerTokens.ScrubBadgeOffset
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerTokens.TouchTargetMin)
                .onSizeChanged { componentWidthPx = it.width.toFloat() }
                .semantics {
                    this.progressBarRangeInfo = ProgressBarRangeInfo(activeFraction, 0f..1f)
                    this.contentDescription = "Playback progress, ${formatTimecode(displayPositionMs)} of ${formatTimecode(effectiveDuration)}"
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        val w = size.width.toFloat()
                        dragFraction = if (w > 0f) (down.position.x / w).coerceIn(0f, 1f) else 0f
                        val dur = currentEffectiveDuration
                        val initialTarget = (dragFraction * dur).toLong()
                        currentOnScrubStart(initialTarget)
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            val currentW = size.width.toFloat()
                            dragFraction = if (currentW > 0f) (change.position.x / currentW).coerceIn(0f, 1f) else 0f
                            val currentTarget = (dragFraction * currentEffectiveDuration).toLong()
                            currentOnScrubProgress(currentTarget)
                        }

                        isDragging = false
                        val finalTarget = (dragFraction * currentEffectiveDuration).toLong()
                        currentOnScrubEnd(finalTarget)
                    }
                }
        ) {
            val width = size.width
            val centerY = size.height / 2f
            val h = trackHeight.toPx()
            val radius = CornerRadius(h / 2f, h / 2f)

            // Unplayed track (white 18%)
            drawRoundRect(
                color = PlayerTokens.TrackUnplayed,
                topLeft = Offset(0f, centerY - h / 2f),
                size = Size(width, h),
                cornerRadius = radius
            )

            // Buffered range (white 32%)
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = PlayerTokens.TrackBuffered,
                    topLeft = Offset(0f, centerY - h / 2f),
                    size = Size(width * bufferedFraction, h),
                    cornerRadius = radius
                )
            }

            // Played range (white 90%)
            val playedWidth = (width * activeFraction).coerceIn(0f, width)
            if (playedWidth > 0f) {
                drawRoundRect(
                    color = PlayerTokens.TrackPlayed,
                    topLeft = Offset(0f, centerY - h / 2f),
                    size = Size(playedWidth, h),
                    cornerRadius = radius
                )
            }

            // Knob: 12 dp white circle (grows to 16 dp)
            val knobR = knobSize.toPx() / 2f
            val knobX = playedWidth.coerceIn(knobR, width - knobR)
            drawCircle(
                color = Color.White,
                radius = knobR,
                center = Offset(knobX, centerY)
            )
        }
    }
}

/**
 * Expandable Action Dock (Section 3.5 & 3.6).
 * Beneath timeline (8 dp gap), 44 dp tall, bare outline icons at 22 dp in 40 dp targets on bottom scrim.
 * Dedicated separate buttons for Audio and Subtitles. Speed & Fit removed (available above timeline).
 * Expander chevron in 32 dp glass circle. Timecode pinned at far right.
 */
@Composable
fun PlayerActionDock(
    isTvSeries: Boolean,
    activeSidePanel: PlayerSidePanelType,
    onOpenEpisodes: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenAudio: () -> Unit = {},
    onOpenSubtitles: () -> Unit = {},
    onOpenAudioSubtitles: (() -> Unit)? = null,
    onOpenQuality: (() -> Unit)? = null,
    onOpenTuning: () -> Unit,
    onOpenPip: (() -> Unit)? = null,
    onOpenCast: (() -> Unit)? = null,
    onOpenExternalPlayer: (() -> Unit)? = null,
    onOpenInfo: (() -> Unit)? = null,
    sleepTimerRemainingSeconds: Int? = null,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    subtitlesActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current

    // Chevron rotation 180 degrees over 200 ms
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(PlayerTokens.DockChevronRotateMs, easing = PlayerTokens.EaseOut),
        label = "dockChevronRotate"
    )

    // Timecode format: elapsed / total or elapsed / -remaining
    val effectiveDuration = durationMs.coerceAtLeast(1L)
    val remainingMs = (effectiveDuration - currentPositionMs).coerceAtLeast(0L)
    val timecodeString = if (showRemainingTime) {
        "${formatTimecode(currentPositionMs)} / -${formatTimecode(remainingMs)}"
    } else {
        "${formatTimecode(currentPositionMs)} / ${formatTimecode(effectiveDuration)}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerTokens.TouchTargetMin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontally scrollable icons area with closer spacing (2 dp gap)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 1. Audio Button
            DockBareIcon(
                iconRes = R.drawable.ic_player_audio,
                contentDescription = "Audio Tracks",
                hasDot = activeSidePanel == PlayerSidePanelType.AUDIO,
                onClick = {
                    isExpanded = false
                    if (onOpenAudioSubtitles != null) onOpenAudioSubtitles() else onOpenAudio()
                }
            )

            // 2. Subtitles Button
            DockBareIcon(
                iconRes = R.drawable.ic_player_subtitles,
                contentDescription = "Subtitles",
                hasDot = activeSidePanel == PlayerSidePanelType.SUBTITLES || subtitlesActive,
                onClick = {
                    isExpanded = false
                    if (onOpenAudioSubtitles != null) onOpenAudioSubtitles() else onOpenSubtitles()
                }
            )

            // Video Quality Button
            if (onOpenQuality != null) {
                DockBareIcon(
                    iconRes = R.drawable.ic_quality_fhd,
                    contentDescription = "Video Quality",
                    hasDot = activeSidePanel == PlayerSidePanelType.VIDEO_QUALITY,
                    onClick = {
                        isExpanded = false
                        onOpenQuality()
                    }
                )
            }

            // 3. Sources ("Stream") Button
            DockBareIcon(
                iconRes = R.drawable.ic_player_sources,
                contentDescription = "Stream Sources",
                hasDot = activeSidePanel == PlayerSidePanelType.SOURCES,
                onClick = {
                    isExpanded = false
                    onOpenSources()
                }
            )

            // 4. Episodes (TV Series only, hidden for movies)
            if (isTvSeries) {
                DockBareIcon(
                    iconRes = R.drawable.ic_player_episodes,
                    contentDescription = "Episodes",
                    hasDot = activeSidePanel == PlayerSidePanelType.EPISODES,
                    onClick = {
                        isExpanded = false
                        onOpenEpisodes()
                    }
                )
            }

            // 5. Settings / Tuning
            DockBareIcon(
                iconRes = R.drawable.ic_player_tuning,
                contentDescription = "Player Settings",
                hasDot = activeSidePanel == PlayerSidePanelType.SETTINGS ||
                        activeSidePanel == PlayerSidePanelType.SLEEP_TIMER,
                onClick = {
                    isExpanded = false
                    onOpenTuning()
                }
            )

            // Expanded items with ease-in-ease-out animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = if (reduceMotion) {
                    fadeIn(tween(PlayerTokens.ReduceMotionFadeInMs))
                } else {
                    expandHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(240, easing = FastOutSlowInEasing))
                },
                exit = if (reduceMotion) {
                    fadeOut(tween(PlayerTokens.ReduceMotionFadeOutMs))
                } else {
                    shrinkHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(180, easing = FastOutSlowInEasing))
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Hairline divider: 1 dp by 20 dp, white 14%, 4 dp margin
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )

                    // Picture-in-Picture
                    if (onOpenPip != null) {
                        DockBareIcon(
                            iconRes = R.drawable.ic_player_pip,
                            contentDescription = "Picture in Picture",
                            hasDot = false,
                            onClick = onOpenPip
                        )
                    }

                    // Open in External Player
                    if (onOpenExternalPlayer != null) {
                        DockBareIcon(
                            iconRes = R.drawable.ic_player_external,
                            contentDescription = "Open in external player",
                            hasDot = false,
                            onClick = onOpenExternalPlayer
                        )
                    }

                    // Cast
                    if (onOpenCast != null) {
                        DockBareIcon(
                            iconRes = R.drawable.ic_player_cast,
                            contentDescription = "Cast to Device",
                            hasDot = false,
                            onClick = onOpenCast
                        )
                    }

                    // Info (if available)
                    if (onOpenInfo != null) {
                        DockBareIcon(
                            icon = Icons.Outlined.Info,
                            contentDescription = "Info",
                            hasDot = false,
                            onClick = onOpenInfo
                        )
                    }
                }
            }

            // Expander Chevron inside 32 dp Video-Overlay Glass circle in 44 dp touch target
            Box(
                modifier = Modifier
                    .size(PlayerTokens.TouchTargetMin)
                    .playerPressable(
                        role = Role.Button,
                        onClickLabel = if (isExpanded) "Collapse dock" else "Expand dock",
                        onClick = { isExpanded = !isExpanded }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeExpanderCircle)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronRotation)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pinned Timecode at right end of the dock row (13 sp regular tabular, white 80%)
        Box(
            modifier = Modifier
                .widthIn(min = 110.dp)
                .playerPressable(
                    role = Role.Button,
                    onClickLabel = "Toggle elapsed / remaining time",
                    onClick = { showRemainingTime = !showRemainingTime }
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = timecodeString,
                style = PlayerTokens.Timecode,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

/**
 * Bare Outline Dock Icon (40 dp touch target, 22 dp outline icon, NO box container, NO visible text label).
 * Shows a 4 dp white dot underneath for active states.
 * Long press shows a tiny 11 sp Video-Overlay Glass tooltip pill for 1.5s.
 */
@Composable
private fun DockBareIcon(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    contentDescription: String,
    hasDot: Boolean = false,
    onClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = PlayerTokens.TouchTargetMin)
            .semantics {
                this.contentDescription = contentDescription
            }
            .playerPressable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onLongClick = {
                    showTooltip = true
                    scope.launch {
                        delay(1500L)
                        showTooltip = false
                    }
                },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(PlayerTokens.IconDock)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(PlayerTokens.IconDock)
                )
            }

            // 4 dp white dot for active state (directly underneath icon)
            if (hasDot) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeActiveDot)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Tooltip pill on long press
        if (showTooltip) {
            Box(
                modifier = Modifier
                    .offset(y = (-32).dp)
                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = contentDescription,
                    style = PlayerTokens.TooltipLabel
                )
            }
        }
    }
}

/**
 * Modern Bottom Controls (Section 3.4 & 3.5).
 */
@Composable
fun PlayerBottomControls(
    visible: Boolean,
    isScrubbing: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    scrubPositionMs: Long,
    playbackSpeed: Float,
    isTvSeries: Boolean,
    hasNextEpisode: Boolean,
    mediaTitle: String = "",
    episodeTitle: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    serverName: String? = null,
    showRemainingTime: Boolean = true,
    aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    selectedServer: ServerSourceOption? = null,
    selectedQuality: VideoQualityOption? = null,
    availableServers: List<ServerSourceOption> = emptyList(),
    activeSidePanel: PlayerSidePanelType = PlayerSidePanelType.NONE,
    sleepTimerRemainingSeconds: Int? = null,
    onToggleRemainingTime: () -> Unit = {},
    onScrubStart: (positionMs: Long) -> Unit,
    onScrubProgress: (positionMs: Long) -> Unit,
    onScrubEnd: (positionMs: Long) -> Unit,
    onOpenEpisodes: (() -> Unit)? = null,
    onOpenSources: (() -> Unit)? = null,
    onSelectServer: ((ServerSourceOption) -> Unit)? = null,
    onOpenSpeed: () -> Unit,
    onOpenAudioSubtitles: () -> Unit,
    onOpenQuality: (() -> Unit)? = null,
    onOpenAudio: (() -> Unit)? = null,
    onOpenSubtitles: (() -> Unit)? = null,
    onOpenExternalPlayer: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    onOpenCast: (() -> Unit)? = null,
    onCycleAspectRatio: (() -> Unit)? = null,
    onSelectAspectRatio: ((AspectRatioMode) -> Unit)? = null,
    onSelectSpeed: ((Float) -> Unit)? = null,
    onLockScreen: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp

    var isSpeedExpanded by remember { mutableStateOf(false) }
    var isAspectExpanded by remember { mutableStateOf(false) }

    val enterTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeIn(tween(PlayerTokens.ReduceMotionFadeInMs))
        } else {
            val slidePx = with(density) { PlayerTokens.ControlsSlideOffset.roundToPx() }
            slideInVertically(
                initialOffsetY = { slidePx },
                animationSpec = tween(PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut)
            ) + fadeIn(tween(PlayerTokens.ControlsShowDuration, easing = PlayerTokens.EaseOut))
        }
    }

    val exitTransition = remember(reduceMotion, density) {
        if (reduceMotion) {
            fadeOut(tween(PlayerTokens.ReduceMotionFadeOutMs))
        } else {
            val slidePx = with(density) { PlayerTokens.ControlsSlideOffset.roundToPx() }
            slideOutVertically(
                targetOffsetY = { slidePx },
                animationSpec = tween(PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn)
            ) + fadeOut(tween(PlayerTokens.ControlsHideDuration, easing = PlayerTokens.EaseIn))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Quick Accordion row (Quality, Aspect Ratio & Speed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Quick Video Quality Pill
                if (onOpenQuality != null) {
                    val qBadge = when {
                        selectedQuality == null || selectedQuality.isAuto || selectedQuality.id == "auto" -> "Auto"
                        selectedQuality.height != null -> "${selectedQuality.height}p"
                        else -> selectedQuality.label
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = "Video Quality: $qBadge",
                                onClick = onOpenQuality
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_quality_fhd),
                                contentDescription = "Video Quality",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = qBadge,
                                color = Color.White,
                                style = PlayerTokens.PillText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Quick Aspect Ratio Expandable Accordion
                val aspectLabel = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> "Fit"
                    AspectRatioMode.ZOOM -> "Fill"
                    AspectRatioMode.FIXED_16_9 -> "16:9"
                    AspectRatioMode.STRETCH -> "Stretch"
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                        .animateContentSize(tween(durationMillis = 200, easing = PlayerTokens.EaseOut)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAspectExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .playerPressable(role = Role.Button, onClickLabel = "Collapse Aspect Ratio") {
                                        isAspectExpanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            val modes = listOf(
                                AspectRatioMode.FIT to "Fit",
                                AspectRatioMode.ZOOM to "Fill",
                                AspectRatioMode.FIXED_16_9 to "16:9",
                                AspectRatioMode.STRETCH to "Stretch"
                            )
                            modes.forEach { (mode, label) ->
                                val isSelected = aspectRatioMode == mode
                                Box(
                                    modifier = Modifier
                                        .height(26.dp)
                                        .clip(PlayerTokens.RadiusPill)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                                        .playerPressable(role = Role.Button, onClickLabel = label) {
                                            onSelectAspectRatio?.invoke(mode) ?: onCycleAspectRatio?.invoke()
                                            isAspectExpanded = false
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        style = PlayerTokens.PillText
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = "Aspect Ratio: $aspectLabel",
                                    onClick = {
                                        if (onSelectAspectRatio != null) {
                                            isAspectExpanded = true
                                        } else {
                                            onCycleAspectRatio?.invoke()
                                        }
                                    }
                                )
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_player_aspect),
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = aspectLabel,
                                color = Color.White,
                                style = PlayerTokens.PillText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Quick Speed Expandable Accordion
                val speedLabel = if (playbackSpeed == 1.0f) "Speed" else "${playbackSpeed}x"
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                        .animateContentSize(tween(durationMillis = 200, easing = PlayerTokens.EaseOut)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSpeedExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .playerPressable(role = Role.Button, onClickLabel = "Collapse Speed") {
                                        isSpeedExpanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_player_speed),
                                    contentDescription = "Speed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            speeds.forEach { spd ->
                                val isSelected = playbackSpeed == spd
                                val label = if (spd == 1.0f) "1x" else "${spd}x"
                                Box(
                                    modifier = Modifier
                                        .height(26.dp)
                                        .clip(PlayerTokens.RadiusPill)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                                        .playerPressable(role = Role.Button, onClickLabel = label) {
                                            onSelectSpeed?.invoke(spd) ?: onOpenSpeed()
                                            isSpeedExpanded = false
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        style = PlayerTokens.PillText
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = "Speed",
                                    onClick = {
                                        if (onSelectSpeed != null) {
                                            isSpeedExpanded = true
                                        } else {
                                            onOpenSpeed()
                                        }
                                    }
                                )
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_player_speed),
                                contentDescription = "Speed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = speedLabel,
                                color = Color.White,
                                style = PlayerTokens.PillText
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Timeline (Section 3.4)
            PlayerScrubber(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                isScrubbing = isScrubbing,
                scrubPositionMs = scrubPositionMs,
                showRemainingTime = showRemainingTime,
                onToggleRemainingTime = onToggleRemainingTime,
                onScrubStart = onScrubStart,
                onScrubProgress = onScrubProgress,
                onScrubEnd = onScrubEnd,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Dock (Section 3.5 & 3.6)
            PlayerActionDock(
                isTvSeries = isTvSeries,
                activeSidePanel = activeSidePanel,
                onOpenEpisodes = { onOpenEpisodes?.invoke() },
                onOpenSources = { onOpenSources?.invoke() },
                onOpenQuality = onOpenQuality,
                onOpenAudio = { onOpenAudio?.invoke() ?: onOpenAudioSubtitles() },
                onOpenSubtitles = { onOpenSubtitles?.invoke() ?: onOpenAudioSubtitles() },
                onOpenTuning = onOpenSettings,
                onOpenPip = onEnterPip,
                onOpenCast = onOpenCast,
                onOpenExternalPlayer = onOpenExternalPlayer,
                sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                subtitlesActive = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Screen Unlock Circle (Section 3.7f):
 * 48 dp Video-Overlay Glass circle with outline lock icon at left edge (24 dp from edge).
 * Tapping it unlocks (switches to open lock outline for 150 ms then controls appear).
 */
@Composable
fun PlayerUnlockCircle(
    visible: Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isUnlockedState by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(PlayerTokens.ControlsShowDuration)),
        exit = fadeOut(tween(PlayerTokens.ControlsHideDuration)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(PlayerTokens.SizeLockCircle)
                .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                .tvFocusable(shape = PlayerTokens.RadiusCircle)
                .playerPressable(
                    role = Role.Button,
                    onClickLabel = "Unlock Player",
                    onClick = {
                        isUnlockedState = true
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                        scope.launch {
                            delay(150L) // 150 ms open-lock feedback before showing controls
                            onUnlocked()
                            isUnlockedState = false
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isUnlockedState) R.drawable.ic_player_unlock else R.drawable.ic_player_lock
                ),
                contentDescription = "Unlock",
                tint = Color.White,
                modifier = Modifier.size(PlayerTokens.IconHeader)
            )
        }
    }
}

/**
 * Aspect Ratio Pill Badge (Section 3.7e).
 * Centered 36 dp tall Video-Overlay Glass pill, 13 sp medium uppercase.
 * Crossfades labels over 120 ms.
 */
/**
 * Aspect Ratio Pill Badge (Section 2.1, 3.3, 4.2).
 * Centered 36 dp tall Video-Overlay Glass pill.
 * Safe margin: max(I_cutout, top, I_status, top, 16dp) + 16dp.
 * Enter: Slightly underdamped spring k = 900 N/m, zeta = 0.72 (scale 0.85 -> 1.0, Y: -12dp -> 0dp, alpha -> 1.0).
 * Mode pulse on label flip: scale 1.08 -> 1.0.
 * Debounce hold: 1100ms.
 * Exit: Cubic-Bezier(0.40, 0.0, 1.0, 1.0), T = 180ms (alpha -> 0.0, scale -> 0.90).
 */
@Composable
fun PinchAspectBadge(
    label: String?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var currentDisplayLabel by remember { mutableStateOf<String?>(null) }
    val badgeAlpha = remember { Animatable(0.0f) }
    val badgeScale = remember { Animatable(0.85f) }
    val badgeOffsetY = remember { Animatable(-12f) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(label) {
        if (!label.isNullOrBlank()) {
            dismissJob?.cancel()
            val isFirstEnter = currentDisplayLabel == null
            currentDisplayLabel = label

            if (isFirstEnter) {
                badgeAlpha.snapTo(0.0f)
                badgeScale.snapTo(0.85f)
                badgeOffsetY.snapTo(-12f)
                launch {
                    badgeAlpha.animateTo(1.0f, tween(120))
                }
                launch {
                    badgeOffsetY.animateTo(
                        targetValue = 0.0f,
                        animationSpec = spring(dampingRatio = PlayerMotionMath.BADGE_ENTER_ZETA, stiffness = PlayerMotionMath.BADGE_ENTER_K)
                    )
                }
                launch {
                    badgeScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(dampingRatio = PlayerMotionMath.BADGE_ENTER_ZETA, stiffness = PlayerMotionMath.BADGE_ENTER_K)
                    )
                }
            } else {
                // Tactile badge pulse on mode change
                badgeScale.snapTo(1.08f)
                launch {
                    badgeScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(dampingRatio = PlayerMotionMath.BADGE_ENTER_ZETA, stiffness = PlayerMotionMath.BADGE_ENTER_K)
                    )
                }
            }

            // Start 1100ms debounce hold timer before exit
            dismissJob = launch {
                delay(PlayerMotionMath.BADGE_DEBOUNCE_HOLD_MS)
                launch {
                    badgeScale.animateTo(
                        targetValue = 0.90f,
                        animationSpec = tween(
                            durationMillis = PlayerMotionMath.BADGE_EXIT_DURATION_MS,
                            easing = CubicBezierEasing(0.40f, 0.0f, 1.0f, 1.0f)
                        )
                    )
                }
                badgeAlpha.animateTo(
                    targetValue = 0.0f,
                    animationSpec = tween(
                        durationMillis = PlayerMotionMath.BADGE_EXIT_DURATION_MS,
                        easing = CubicBezierEasing(0.40f, 0.0f, 1.0f, 1.0f)
                    )
                )
                currentDisplayLabel = null
            }
        }
    }

    if (badgeAlpha.value > 0.001f && !currentDisplayLabel.isNullOrBlank()) {
        val windowInsets = WindowInsets.displayCutout
        val statusInsets = WindowInsets.statusBars
        val topInsetDp = with(density) {
            maxOf(windowInsets.getTop(density), statusInsets.getTop(density), (16.dp).roundToPx()).toDp() + 16.dp
        }
        val horizontalShiftDp = with(density) {
            PlayerMotionMath.calculateBadgeCenterHorizontalShift(
                windowInsets.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr).toFloat(),
                windowInsets.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr).toFloat()
            ).toDp()
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = topInsetDp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .offset(x = horizontalShiftDp)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                        translationY = with(density) { badgeOffsetY.value.dp.toPx() }
                        alpha = badgeAlpha.value
                    }
                    .height(36.dp)
                    .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (currentDisplayLabel ?: "").uppercase(),
                    style = PlayerTokens.AspectPillText
                )
            }
        }
    }
}

/**
 * Resume Notice Snackbar restyled with Video-Overlay Glass and white pill action (no red).
 */
@Composable
fun ResumeNoticeSnackbar(
    resumeNotice: String?,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = resumeNotice != null,
        enter = fadeIn(tween(PlayerTokens.ControlsShowDuration)) + slideInVertically(tween(PlayerTokens.ControlsShowDuration)) { it / 2 },
        exit = fadeOut(tween(PlayerTokens.ControlsHideDuration)) + slideOutVertically(tween(PlayerTokens.ControlsHideDuration)) { it / 2 },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = resumeNotice ?: "",
                style = PlayerTokens.RowTitle.copy(fontSize = 13.sp),
                color = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .playerPressable(
                        isWhiteElement = true,
                        role = Role.Button,
                        onClickLabel = "Start Over",
                        onClick = onStartOver
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Start Over",
                    style = PlayerTokens.PillText,
                    color = PlayerTokens.LabelDark
                )
            }
        }
    }
}

/**
 * QuickSourceSwitchPopover for quick server switching.
 */
@Composable
fun QuickSourceSwitchPopover(
    visible: Boolean,
    servers: List<ServerSourceOption>,
    selectedServer: ServerSourceOption?,
    onSelectServer: (ServerSourceOption) -> Unit,
    onOpenAllSources: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filteredServers = remember(servers, selectedFilter) {
        when (selectedFilter) {
            "4K" -> servers.filter { it.resolutionLabel.contains("4k", ignoreCase = true) || it.resolutionLabel.contains("2160", ignoreCase = true) }
            "1080p" -> servers.filter { it.resolutionLabel.contains("1080", ignoreCase = true) }
            "720p" -> servers.filter { it.resolutionLabel.contains("720", ignoreCase = true) }
            else -> servers
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.95f),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .sheetGlass(shape = PlayerTokens.RadiusSheet)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sources",
                        style = PlayerTokens.RowTitle
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                            .playerPressable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quality Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "4K", "1080p", "720p").forEach { chip ->
                        val isSelected = selectedFilter == chip
                        Box(
                            modifier = Modifier
                                .clip(PlayerTokens.RadiusPill)
                                .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                                .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                                .playerPressable(
                                    isWhiteElement = isSelected,
                                    role = Role.Button,
                                    onClickLabel = chip,
                                    onClick = { selectedFilter = chip }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                style = PlayerTokens.PillText,
                                color = if (isSelected) PlayerTokens.LabelDark else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Server list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredServers.take(4).forEach { srv ->
                        val isSelected = srv.id == selectedServer?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .videoOverlayGlass(shape = PlayerTokens.RadiusSmall)
                                .playerPressable(
                                    role = Role.Button,
                                    onClickLabel = srv.serverName,
                                    onClick = { onSelectServer(srv) }
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = srv.serverName, style = PlayerTokens.RowTitle)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(PlayerTokens.SizeCheckCircle)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Selected",
                                        tint = PlayerTokens.LabelDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
