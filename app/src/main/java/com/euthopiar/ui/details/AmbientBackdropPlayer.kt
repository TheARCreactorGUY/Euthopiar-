package com.euthopiar.ui.details

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Native Media3 ExoPlayer TextureView ambient backdrop trailer player.
 *
 * Plays raw YouTube video/audio streams natively extracted via InnerTube with
 * zero YouTube overlays, automatic letterbox black-bar zoom compensation,
 * chunked range anti-throttling, and seamless crossfade only after onRenderedFirstFrame().
 */
@Composable
fun AmbientBackdropPlayer(
    trailerUrl: String,
    canPlay: Boolean,
    isMuted: Boolean = true,
    onPlaybackActive: (Boolean) -> Unit = {},
    onPlaybackError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (trailerUrl.isBlank()) return

    val currentOnPlaybackError by rememberUpdatedState(onPlaybackError)
    var playbackSource by remember(trailerUrl) { mutableStateOf<TrailerPlaybackSource?>(null) }

    LaunchedEffect(trailerUrl) {
        val resolved = withContext(Dispatchers.IO) {
            TrailerPlaybackResolver.resolveFromYouTubeUrl(trailerUrl)
        }
        if (resolved == null) {
            currentOnPlaybackError()
        } else {
            playbackSource = resolved
        }
    }

    val source = playbackSource
    if (source != null) {
        AmbientBackdropPlayerSurface(
            source = source,
            trailerUrl = trailerUrl,
            canPlay = canPlay,
            isMuted = isMuted,
            onPlaybackActive = onPlaybackActive,
            onPlaybackError = onPlaybackError,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AmbientBackdropPlayerSurface(
    source: TrailerPlaybackSource,
    trailerUrl: String,
    canPlay: Boolean,
    isMuted: Boolean,
    onPlaybackActive: (Boolean) -> Unit,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestCanPlay = rememberUpdatedState(canPlay)
    val latestOnPlaybackActive = rememberUpdatedState(onPlaybackActive)
    val latestOnPlaybackError = rememberUpdatedState(onPlaybackError)

    var playerContainer by remember { mutableStateOf<HeroTrailerTextureContainer?>(null) }
    var hasRenderedFirstFrame by remember(source) { mutableStateOf(false) }

    val exoPlayer = remember(source) {
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
                playWhenReady = canPlay
                prepare()
            }
    }

    var letterboxZoom by remember(exoPlayer) { mutableFloatStateOf(1f) }
    val letterboxZoomState = animateFloatAsState(
        targetValue = letterboxZoom,
        animationSpec = tween(durationMillis = 400),
        label = "ambientLetterboxZoom",
    )

    // Smooth Fade-In without Layout Shift: 0f until first frame renders AND canPlay == true
    val playerAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame && canPlay) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "ambientPlayerAlpha",
    )

    DisposableEffect(exoPlayer, lifecycleOwner) {
        fun detachVideoSurface() {
            playerContainer?.detachPlayer(exoPlayer)
        }

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                playerContainer?.setVideoSize(videoSize)
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                if (exoPlayer.isPlaying && latestCanPlay.value) {
                    latestOnPlaybackActive.value(true)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                latestOnPlaybackActive.value(isPlaying && hasRenderedFirstFrame && latestCanPlay.value)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    if (latestCanPlay.value) {
                        exoPlayer.play()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                detachVideoSurface()
                TrailerPlaybackResolver.invalidateCache(trailerUrl)
                latestOnPlaybackActive.value(false)
                latestOnPlaybackError.value()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (latestCanPlay.value) {
                        playerContainer?.attachPlayer(exoPlayer)
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                    detachVideoSurface()
                    latestOnPlaybackActive.value(false)
                }
                else -> Unit
            }
        }

        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            detachVideoSurface()
            latestOnPlaybackActive.value(false)
            exoPlayer.stop()
            exoPlayer.release()
            playerContainer = null
        }
    }

    LaunchedEffect(exoPlayer, canPlay) {
        exoPlayer.playWhenReady = canPlay
        if (canPlay) {
            playerContainer?.attachPlayer(exoPlayer)
            exoPlayer.play()
            if (hasRenderedFirstFrame && exoPlayer.isPlaying) {
                latestOnPlaybackActive.value(true)
            }
        } else {
            exoPlayer.pause()
            latestOnPlaybackActive.value(false)
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer, hasRenderedFirstFrame) {
        letterboxZoom = 1f
        if (!hasRenderedFirstFrame) return@LaunchedEffect
        val tracker = LetterboxTracker()
        val sampler = LetterboxSampler()
        try {
            while (true) {
                delay(LetterboxDetector.SAMPLE_INTERVAL_MS)
                if (LetterboxDetector.isSampleWindowOver(exoPlayer.currentPosition, exoPlayer.duration)) break
                if (!exoPlayer.isPlaying) continue
                val textureView = playerContainer?.textureView ?: continue
                val bar = sampler.sample(textureView) ?: continue
                letterboxZoom = tracker.onSample(bar) ?: continue
                break
            }
        } finally {
            sampler.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = letterboxZoomState.value
                    scaleY = letterboxZoomState.value
                    alpha = playerAlpha
                },
            factory = { viewContext ->
                HeroTrailerTextureContainer(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    attachPlayer(exoPlayer)
                    playerContainer = this
                }
            },
            update = { container ->
                playerContainer = container
                if (canPlay) {
                    container.attachPlayer(exoPlayer)
                }
            }
        )
    }
}

private class HeroTrailerTextureContainer(
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
