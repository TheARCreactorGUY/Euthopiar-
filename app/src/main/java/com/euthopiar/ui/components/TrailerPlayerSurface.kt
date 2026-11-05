package com.euthopiar.ui.components

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import com.euthopiar.ui.icons.LucideIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import com.euthopiar.core.trailer.TrailerExtractionPlatform
import com.euthopiar.core.trailer.TrailerPlaybackResolver
import com.euthopiar.core.trailer.TrailerPlaybackSource
import com.euthopiar.ui.details.YoutubeChunkedDataSourceFactory
import com.euthopiar.ui.longpress.ImageTopScrim
import com.euthopiar.ui.theme.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Extracts a clean 11-character YouTube video ID from various URL formats.
 */
fun extractYouTubeVideoId(url: String): String? {
    val clean = url.trim()
    return when {
        clean.contains("v=") -> clean.substringAfter("v=").substringBefore("&")
        clean.contains("youtu.be/") -> clean.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        clean.contains("embed/") -> clean.substringAfter("embed/").substringBefore("?").substringBefore("&")
        clean.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> clean
        else -> null
    }
}

/**
 * Native hardware-accelerated Media3 ExoPlayer trailer player surface.
 *
 * Uses InnerTube stream extraction and YoutubeChunkedDataSourceFactory to stream raw
 * video and audio directly with zero iframe chrome, zero webview overhead, and instant response.
 */
@OptIn(UnstableApi::class)
@Composable
fun TrailerPlayerSurface(
    trailerUrl: String,
    modifier: Modifier = Modifier,
    startMuted: Boolean = true,
    autoPlay: Boolean = true,
    posterUrl: String? = null,
    showBuiltInTopChrome: Boolean = true,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onControlsVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackFailure: (() -> Unit)? = null,
    onCloseVideo: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val latestOnControlsVisibilityChanged = rememberUpdatedState(onControlsVisibilityChanged)
    val latestOnPlaybackFailure = rememberUpdatedState(onPlaybackFailure)

    var playbackSource by remember(trailerUrl) { mutableStateOf<TrailerPlaybackSource?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(startMuted) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(trailerUrl) { mutableStateOf(false) }
    var playerContainer by remember { mutableStateOf<TrailerTextureContainer?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    // Notify controls visibility changes
    LaunchedEffect(controlsVisible, isPlaying) {
        latestOnControlsVisibilityChanged.value?.invoke(controlsVisible || !isPlaying)
    }

    // Auto-hide controls when playing (2600ms inactivity timer)
    LaunchedEffect(controlsVisible, isPlaying, interactionTick) {
        if (!isPlaying) {
            controlsVisible = true
        } else if (controlsVisible) {
            delay(2600L)
            controlsVisible = false
        }
    }

    // Resolve InnerTube stream sources
    LaunchedEffect(trailerUrl) {
        if (trailerUrl.isBlank()) {
            isBuffering = false
            latestOnPlaybackFailure.value?.invoke()
            return@LaunchedEffect
        }
        isBuffering = true
        val resolved = withContext(Dispatchers.IO) {
            TrailerPlaybackResolver.resolveFromYouTubeUrl(trailerUrl)
        }
        if (resolved == null) {
            isBuffering = false
            latestOnPlaybackFailure.value?.invoke()
        } else {
            playbackSource = resolved
        }
    }

    val source = playbackSource

    // Build ExoPlayer instance when playbackSource is resolved
    val exoPlayer = remember(source) {
        if (source == null) null
        else {
            val dataSourceFactory = YoutubeChunkedDataSourceFactory(TrailerExtractionPlatform.defaultHeaders)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    val audioUrl = source.audioUrl
                    if (!audioUrl.isNullOrBlank()) {
                        val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(source.videoUrl))
                        val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                        setMediaSource(MergingMediaSource(videoSource, audioSource))
                    } else {
                        setMediaItem(MediaItem.fromUri(source.videoUrl))
                    }
                    repeatMode = Player.REPEAT_MODE_OFF
                    volume = if (isMuted) 0f else 1f
                    playWhenReady = autoPlay
                    prepare()
                }
        }
    }

    // Auto-hide play/pause indicator after 700ms (built-in top chrome mode)
    LaunchedEffect(showPlayPauseIndicator) {
        if (showPlayPauseIndicator) {
            delay(700L)
            showPlayPauseIndicator = false
        }
    }

    // Attach ExoPlayer listeners and lifecycle observers
    DisposableEffect(exoPlayer, lifecycleOwner) {
        fun detachVideoSurface() {
            exoPlayer?.let { playerContainer?.detachPlayer(it) }
        }

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                playerContainer?.setVideoSize(videoSize)
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                isBuffering = false
                isPlayerReady = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        isPlayerReady = true
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        exoPlayer?.seekTo(0)
                        if (autoPlay) {
                            exoPlayer?.play()
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isBuffering = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                detachVideoSurface()
                TrailerPlaybackResolver.invalidateCache(trailerUrl)
                isBuffering = false
                isPlaying = false
                latestOnPlaybackFailure.value?.invoke()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.pause()
                    detachVideoSurface()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.let { player ->
                        playerContainer?.attachPlayer(player)
                        if (autoPlay && isPlaying) {
                            player.play()
                        }
                    }
                }
                else -> Unit
            }
        }

        exoPlayer?.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.removeListener(listener)
            detachVideoSurface()
            exoPlayer?.stop()
            exoPlayer?.release()
            playerContainer = null
        }
    }

    // React to mute toggle
    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    val surfaceAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "trailerSurfaceAlpha"
    )

    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds()
    ) {
        // 0. Backdrop / Poster placeholder before first frame
        if (!posterUrl.isNullOrBlank() && !hasRenderedFirstFrame) {
            AppAsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f }
            )
        }

        // 1. Native TextureView Video Surface
        if (exoPlayer != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = surfaceAlpha },
                factory = { viewContext ->
                    TrailerTextureContainer(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        attachPlayer(exoPlayer)
                        playerContainer = this
                    }
                },
                update = { container ->
                    playerContainer = container
                    container.attachPlayer(exoPlayer)
                }
            )
        }

        // 2. Gesture overlay for interaction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (showBuiltInTopChrome) {
                            exoPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                                showPlayPauseIndicator = true
                            }
                        } else {
                            controlsVisible = !controlsVisible
                            interactionTick++
                        }
                    }
                )
        )

        if (showBuiltInTopChrome) {
            // 3. Top gradient scrim so native overlay buttons remain legible
            ImageTopScrim(modifier = Modifier.align(Alignment.TopCenter))

            // 4. Center Loading spinner while initializing/buffering
            AnimatedVisibility(
                visible = isBuffering && !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 5. Center Play / Pause momentary flash indicator
            AnimatedVisibility(
                visible = showPlayPauseIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) LucideIcons.Play else LucideIcons.Pause,
                        contentDescription = if (isPlaying) "Playing" else "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // 6. Top-Right Native Controls: Mute toggle & Close button
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable {
                            isMuted = !isMuted
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) LucideIcons.VolumeMute else LucideIcons.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close button (if provided)
                if (onCloseVideo != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable {
                            exoPlayer?.pause()
                            onCloseVideo()
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close trailer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Clean Cinema mode controls overlay
            AnimatedVisibility(
                visible = controlsVisible || !isPlaying || isBuffering,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle bottom gradient scrim (height 64.dp, aligned BottomCenter)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )

                    // Center Play/Pause Glass Pill (56.dp circle)
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering && !hasRenderedFirstFrame) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0E1017).copy(alpha = 0.72f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.2.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0E1017).copy(alpha = 0.72f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                    .pressable(
                                        role = Role.Button,
                                        onClick = {
                                            exoPlayer?.let { player ->
                                                if (player.isPlaying) {
                                                    player.pause()
                                                } else {
                                                    player.play()
                                                }
                                            }
                                            interactionTick++
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) LucideIcons.Pause else LucideIcons.Play,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Bottom-Right Control Pill Row
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute / Unmute Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0E1017).copy(alpha = 0.72f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                .pressable(
                                    role = Role.Button,
                                    onClick = {
                                        isMuted = !isMuted
                                        interactionTick++
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) LucideIcons.VolumeMute else LucideIcons.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen / Orientation Toggle Button
                        if (onToggleFullscreen != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E1017).copy(alpha = 0.72f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                    .pressable(
                                        role = Role.Button,
                                        onClick = {
                                            onToggleFullscreen()
                                            interactionTick++
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) LucideIcons.FullscreenExit else LucideIcons.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private class TrailerTextureContainer(
    context: Context,
) : FrameLayout(context) {
    val textureView = TextureView(context)
    private val textureTransform = Matrix()
    private var videoAspectRatio = 16f / 9f
    private var attachedPlayer: ExoPlayer? = null

    init {
        clipChildren = true
        clipToPadding = true
        isFocusable = false
        addView(
            textureView,
            LayoutParams(MATCH_PARENT, MATCH_PARENT),
        )
        textureView.isFocusable = false
        textureView.isClickable = false
    }

    fun attachPlayer(player: ExoPlayer) {
        if (attachedPlayer === player) return
        attachedPlayer?.clearVideoTextureView(textureView)
        attachedPlayer = player
        player.setVideoTextureView(textureView)
    }

    fun detachPlayer(player: ExoPlayer) {
        if (attachedPlayer === player) {
            player.clearVideoTextureView(textureView)
            attachedPlayer = null
        }
    }

    fun setVideoSize(videoSize: VideoSize) {
        if (videoSize.width <= 0 || videoSize.height <= 0) return
        videoAspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        updateTextureTransform()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateTextureTransform()
    }

    private fun updateTextureTransform() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || videoAspectRatio <= 0f) return

        val viewAspectRatio = viewWidth / viewHeight
        textureTransform.reset()
        if (viewAspectRatio > videoAspectRatio) {
            val scaleY = viewAspectRatio / videoAspectRatio
            textureTransform.setScale(1f, scaleY, viewWidth / 2f, viewHeight / 2f)
        } else {
            val scaleX = videoAspectRatio / viewAspectRatio
            textureTransform.setScale(scaleX, 1f, viewWidth / 2f, viewHeight / 2f)
        }
        textureView.setTransform(textureTransform)
    }
}
