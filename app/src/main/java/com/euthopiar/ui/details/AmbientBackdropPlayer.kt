package com.euthopiar.ui.details

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.euthopiar.ui.components.extractYouTubeVideoId

private class AmbientPlayerBridge(
    private val onPlaybackActive: (Boolean) -> Unit
) {
    @JavascriptInterface
    fun onPlaybackActive(active: Boolean) {
        onPlaybackActive(active)
    }
}


/**
 * Hardware-accelerated, zero-overhead ambient backdrop trailer player.
 *
 * Emulates TMDB origin to avoid YouTube embedding blocks, smoothly fades in
 * over 600ms once playback begins, scales by 1.28x to cleanly crop YouTube branding
 * and UI chrome, and respects playback/mute lifecycle state.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AmbientBackdropPlayer(
    trailerUrl: String,
    canPlay: Boolean,
    isMuted: Boolean = true,
    onPlaybackActive: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val videoId = remember(trailerUrl) { extractYouTubeVideoId(trailerUrl) }
    if (videoId == null) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCanPlay by rememberUpdatedState(canPlay)
    val currentOnPlaybackActive by rememberUpdatedState(onPlaybackActive)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    webViewRef?.let { wv ->
                        try {
                            wv.visibility = View.INVISIBLE
                            wv.onPause()
                            wv.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
                        } catch (_: Exception) {}
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (currentCanPlay) {
                        webViewRef?.let { wv ->
                            try {
                                wv.onResume()
                                wv.visibility = View.VISIBLE
                                wv.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
                            } catch (_: Exception) {}
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { wv ->
                try {
                    wv.visibility = View.INVISIBLE
                    wv.onPause()
                    wv.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
                    wv.stopLoading()
                } catch (_: Exception) {}
            }
            webViewRef = null
        }
    }

    LaunchedEffect(isMuted) {
        webViewRef?.evaluateJavascript("if (typeof setMuted === 'function') setMuted($isMuted);", null)
    }

    LaunchedEffect(canPlay) {
        webViewRef?.let { wv ->
            if (canPlay) {
                wv.visibility = View.VISIBLE
                wv.evaluateJavascript("if (typeof playVideo === 'function') playVideo(); if (typeof revealPlayer === 'function') revealPlayer();", null)
            } else {
                wv.evaluateJavascript("if (typeof hidePlayer === 'function') hidePlayer(); if (typeof pauseVideo === 'function') pauseVideo();", null)
            }
        }
    }

    Box(modifier = modifier) {
        key(videoId) {
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
                            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                        }
                        setBackgroundColor(AndroidColor.TRANSPARENT)

                        val bridge = AmbientPlayerBridge { active ->
                            post {
                                currentOnPlaybackActive(active)
                            }
                        }
                        addJavascriptInterface(bridge, "AndroidBridge")

                        val muteParam = if (isMuted) "1" else "0"
                        val autoPlayParam = if (canPlay) "1" else "0"

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
                                    background: transparent;
                                }
                                .video-container {
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    width: 100%;
                                    height: 100%;
                                    overflow: hidden;
                                    background: transparent;
                                    pointer-events: none;
                                }
                                #player {
                                    position: absolute;
                                    top: 50%;
                                    left: 50%;
                                    width: 177.78vh;
                                    height: 100vh;
                                    min-width: 100vw;
                                    min-height: 56.25vw;
                                    transform: translate(-50%, -50%) scale(1.28);
                                    border: 0;
                                    pointer-events: none;
                                    opacity: 0;
                                    background: transparent;
                                    transition: opacity 0.6s cubic-bezier(0.4, 0.0, 0.2, 1);
                                }
                                #player.active {
                                    opacity: 1;
                                }
                            </style>
                            </head>
                            <body>
                            <div class="video-container">
                                <iframe id="player"
                                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=$autoPlayParam&mute=$muteParam&controls=0&loop=1&playlist=$videoId&playsinline=1&rel=0&modestbranding=1&iv_load_policy=3&disablekb=1&fs=0&enablejsapi=1&cc_load_policy=0&origin=https://www.themoviedb.org&widget_referrer=https://www.themoviedb.org"
                                        allow="autoplay; encrypted-media; picture-in-picture; accelerometer; clipboard-write; gyroscope; fullscreen"
                                        referrerpolicy="strict-origin-when-cross-origin"
                                        frameborder="0"
                                        onload="onIframeLoad()">
                                </iframe>
                            </div>
                            <script>
                                function revealPlayer() {
                                    var p = document.getElementById('player');
                                    if (p) p.classList.add('active');
                                }
                                function hidePlayer() {
                                    var p = document.getElementById('player');
                                    if (p) p.classList.remove('active');
                                }
                                function disableCaptions() {
                                    var p = document.getElementById('player');
                                    if (p && p.contentWindow) {
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'unloadModule', args: ['captions']}), '*');
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'unloadModule', args: ['cc']}), '*');
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'setOption', args: ['captions', 'track', {}]}), '*');
                                    }
                                }
                                function playVideo() {
                                    var p = document.getElementById('player');
                                    if (p && p.contentWindow) {
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'playVideo', args: []}), '*');
                                    }
                                }
                                function pauseVideo() {
                                    var p = document.getElementById('player');
                                    if (p && p.contentWindow) {
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'pauseVideo', args: []}), '*');
                                    }
                                }
                                function setMuted(mute) {
                                    var p = document.getElementById('player');
                                    if (p && p.contentWindow) {
                                        var cmd = mute ? 'mute' : 'unMute';
                                        p.contentWindow.postMessage(JSON.stringify({event: 'command', func: cmd, args: []}), '*');
                                        if (!mute) {
                                            p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'setVolume', args: [100]}), '*');
                                        }
                                    }
                                }
                                function onIframeLoad() {
                                    var p = document.getElementById('player');
                                    if (p && p.contentWindow) {
                                        p.contentWindow.postMessage(JSON.stringify({event: 'listening'}), '*');
                                        if ($autoPlayParam === '1') {
                                            p.contentWindow.postMessage(JSON.stringify({event: 'command', func: 'playVideo', args: []}), '*');
                                        }
                                        disableCaptions();
                                    }
                                }
                                window.addEventListener('message', function(event) {
                                    try {
                                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                                        var state = (data && data.info && data.info.playerState !== undefined) ? data.info.playerState : (data && data.event === 'onStateChange' ? data.info : null);
                                        if (state === 1) {
                                            revealPlayer();
                                            disableCaptions();
                                            if (window.AndroidBridge) {
                                                window.AndroidBridge.onPlaybackActive(true);
                                            }
                                        } else if (state === 2) {
                                            if (window.AndroidBridge) {
                                                window.AndroidBridge.onPlaybackActive(false);
                                            }
                                        } else if (state === 0) {
                                            playVideo();
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
                update = { webView ->
                    webViewRef = webView
                    if (canPlay) {
                        if (webView.visibility != View.VISIBLE) {
                            webView.visibility = View.VISIBLE
                        }
                        webView.evaluateJavascript("if (typeof playVideo === 'function') playVideo(); if (typeof revealPlayer === 'function') revealPlayer();", null)
                    } else {
                        webView.evaluateJavascript("if (typeof hidePlayer === 'function') hidePlayer(); if (typeof pauseVideo === 'function') pauseVideo();", null)
                    }
                    webView.evaluateJavascript("if (typeof setMuted === 'function') setMuted($isMuted);", null)
                },
                onRelease = { webView ->
                    try {
                        webView.onPause()
                        webView.stopLoading()
                        webView.clearHistory()
                        webView.removeAllViews()
                        webView.post {
                            try {
                                webView.destroy()
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // HWUI stability layer shield
                        alpha = 0.99f
                    }
            )
        }
    }
}
