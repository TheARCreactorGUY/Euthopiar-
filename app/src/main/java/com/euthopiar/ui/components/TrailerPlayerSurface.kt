package com.euthopiar.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.euthopiar.ui.longpress.ImageTopScrim
import com.euthopiar.ui.theme.Accent
import kotlinx.coroutines.delay

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

private class TrailerBridge(
    private val onState: (Int) -> Unit
) {
    @JavascriptInterface
    fun onStateChange(state: Int) {
        onState(state)
    }
}

/**
 * High-performance, zero-stall native trailer player.
 *
 * Employs direct hardware-accelerated WebView embedding with TMDB origin emulation,
 * eliminating the infinite buffering loops and Error 150 restrictions that plague
 * generic iframe wrappers.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerPlayerSurface(
    trailerUrl: String,
    modifier: Modifier = Modifier,
    startMuted: Boolean = true,
    autoPlay: Boolean = true,
    onCloseVideo: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoId = remember(trailerUrl) { extractYouTubeVideoId(trailerUrl) }

    if (videoId == null) return

    var isPlayerReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(startMuted) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Auto-hide play/pause indicator after 700ms
    LaunchedEffect(showPlayPauseIndicator) {
        if (showPlayPauseIndicator) {
            delay(700L)
            showPlayPauseIndicator = false
        }
    }

    // Safety timeout: dismiss buffering indicator after 4.5s
    LaunchedEffect(videoId) {
        delay(4500L)
        isBuffering = false
    }

    // Attach lifecycle observer to pause when screen moves to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                webViewRef?.evaluateJavascript("if (typeof sendCommand === 'function') sendCommand('pauseVideo');", null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { wv ->
                try {
                    wv.removeJavascriptInterface("AndroidBridge")
                    wv.loadUrl("about:blank")
                    wv.stopLoading()
                } catch (_: Exception) {}
            }
            webViewRef = null
        }
    }

    Box(modifier = modifier) {
        // 1. Ultra-fast hardware WebView with TMDB origin bypass
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean = false
                    }
                    setBackgroundColor(AndroidColor.TRANSPARENT)

                    val bridge = TrailerBridge { state ->
                        // YouTube Player States: -1=unstarted, 0=ended, 1=playing, 2=paused, 3=buffering, 5=cued
                        post {
                            when (state) {
                                1 -> {
                                    isPlaying = true
                                    isBuffering = false
                                    isPlayerReady = true
                                }
                                2 -> {
                                    isPlaying = false
                                    isBuffering = false
                                }
                                3 -> {
                                    // Buffering: do not fire commands, allow stream to fill
                                    isBuffering = true
                                }
                                0 -> {
                                    isPlaying = false
                                    isBuffering = false
                                }
                            }
                        }
                    }
                    addJavascriptInterface(bridge, "AndroidBridge")

                    val muteParam = if (startMuted) "1" else "0"
                    val autoPlayParam = if (autoPlay) "1" else "0"

                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <meta name="referrer" content="strict-origin-when-cross-origin">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            html, body {
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                                background: #000;
                            }
                            .video-container {
                                position: absolute;
                                top: 0;
                                left: 0;
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                                background: #000;
                            }
                            #player {
                                position: absolute;
                                top: 0;
                                left: 0;
                                width: 100%;
                                height: 100%;
                                border: 0;
                                background: transparent;
                            }
                        </style>
                        </head>
                        <body>
                        <div class="video-container">
                            <iframe id="player"
                                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=$autoPlayParam&mute=$muteParam&controls=0&playsinline=1&rel=0&modestbranding=1&iv_load_policy=3&disablekb=1&fs=0&enablejsapi=1&cc_load_policy=0&origin=https://www.themoviedb.org&widget_referrer=https://www.themoviedb.org"
                                    allow="autoplay; encrypted-media; picture-in-picture; accelerometer; clipboard-write; gyroscope; fullscreen"
                                    referrerpolicy="strict-origin-when-cross-origin"
                                    frameborder="0"
                                    onload="onIframeLoad()">
                            </iframe>
                        </div>
                        <script>
                            function sendCommand(func, args) {
                                var p = document.getElementById('player');
                                if (p && p.contentWindow) {
                                    p.contentWindow.postMessage(JSON.stringify({event: 'command', func: func, args: args || []}), '*');
                                }
                            }
                            function onIframeLoad() {
                                var p = document.getElementById('player');
                                if (p && p.contentWindow) {
                                    p.contentWindow.postMessage(JSON.stringify({event: 'listening'}), '*');
                                    if ($autoPlayParam === '1') {
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'playVideo', args: []}), '*');
                                    }
                                }
                            }
                            window.addEventListener('message', function(event) {
                                try {
                                    var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                                    var state = (data && data.info && data.info.playerState !== undefined) ? data.info.playerState : (data && data.event === 'onStateChange' ? data.info : null);
                                    if (state !== null && window.AndroidBridge) {
                                        window.AndroidBridge.onStateChange(state);
                                    }
                                } catch(e) {}
                            });
                        </script>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://www.themoviedb.org", html, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.08f
                    scaleY = 1.08f
                }
        )

        // 2. Transparent gesture overlay for tap-to-pause / tap-to-play
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        webViewRef?.let { wv ->
                            if (isPlaying) {
                                wv.evaluateJavascript("sendCommand('pauseVideo');", null)
                                isPlaying = false
                            } else {
                                wv.evaluateJavascript("sendCommand('playVideo');", null)
                                isPlaying = true
                            }
                            showPlayPauseIndicator = true
                        }
                    }
                )
        )

        // 3. Top gradient scrim so native overlay buttons remain legible
        ImageTopScrim(modifier = Modifier.align(Alignment.TopCenter))

        // 4. Center Loading spinner while initializing
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
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
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
                        webViewRef?.let { wv ->
                            if (isMuted) {
                                wv.evaluateJavascript("sendCommand('unMute');", null)
                                isMuted = false
                            } else {
                                wv.evaluateJavascript("sendCommand('mute');", null)
                                isMuted = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
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
                            webViewRef?.evaluateJavascript("sendCommand('pauseVideo');", null)
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
    }
}
