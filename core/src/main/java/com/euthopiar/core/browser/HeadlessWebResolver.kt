package com.euthopiar.core.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

private val BUILTIN_MEDIA_SNIFF_REGEX = Regex(
    """\.(?:m3u8|mpd|mp4|mkv|webm|vtt|srt|ass)(?:\?|$|#)|(?:master|playlist|index)\.(?:m3u8|txt)|/hls/|/backend/database/|/database/|mime=video""",
    RegexOption.IGNORE_CASE
)

private val SUBTITLE_SNIFF_REGEX = Regex(
    """\.(?:vtt|srt|ass)(?:\?|$|#)""",
    RegexOption.IGNORE_CASE
)

private val AD_FILTER_REGEX = Regex(
    """google-analytics|doubleclick|popads|adsco|histats|yandex|\.(?:gif|png|jpg|jpeg|webp|woff|woff2|css)(?:\?|$|#)""",
    RegexOption.IGNORE_CASE
)

private val BLOCKED_SCHEMES = setOf(
    "intent", "market", "mailto", "tel", "whatsapp", "tg"
)

private val BLOCKED_AD_HOSTS_REGEX = Regex(
    """popads|adsco|histats|doubleclick|google-analytics|adkeeper|exoclick|propellerads|juicyads|onclickads|bet365|1xbet""",
    RegexOption.IGNORE_CASE
)

internal const val AUTO_CLICK_PLAY_JS = """
    (function() {
        try {
            var selectors = [
                '.jw-display-icon-container', '.jw-icon-display', '.vjs-big-play-button',
                '.plyr__control--overlaid', 'button[aria-label*="Play" i]', '#play',
                '.play-button', '.play-btn', '#play-button', '.play_button',
                'div[class*="play" i]', 'button[class*="play" i]', 'video'
            ];
            for (var i = 0; i < selectors.length; i++) {
                var el = document.querySelector(selectors[i]);
                if (el) {
                    try { el.click(); } catch(e) {}
                }
            }
            var videos = document.querySelectorAll('video');
            for (var j = 0; j < videos.length; j++) {
                try {
                    videos[j].muted = true;
                    var p = videos[j].play();
                    if (p && typeof p.catch === 'function') p.catch(function(){});
                } catch(e) {}
            }
        } catch(e) {}
    })();
"""

internal const val AUTO_CLICK_SERVERS_JS = """
    (function() {
        try {
            var selectors = [
                '[data-server]',
                '.server-item',
                'button[class*="server" i]',
                'div[class*="server-item" i]',
                'div[class*="server-tab" i]',
                'li[class*="server" i]',
                'a[class*="server" i]',
                '.server'
            ];
            function isVisible(el) {
                if (!el) return false;
                var style = window.getComputedStyle ? window.getComputedStyle(el) : null;
                if (style && (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0')) return false;
                var rect = el.getBoundingClientRect();
                return !!(rect && (rect.width > 0 || rect.height > 0));
            }
            var elements = [];
            for (var s = 0; s < selectors.length; s++) {
                var found = document.querySelectorAll(selectors[s]);
                for (var i = 0; i < found.length; i++) {
                    var el = found[i];
                    if (isVisible(el) && elements.indexOf(el) === -1) {
                        elements.push(el);
                    }
                }
            }
            // Filter out container elements if their child element is also in the list
            elements = elements.filter(function(el) {
                return !elements.some(function(other) {
                    return other !== el && el.contains(other);
                });
            });
            for (var j = 0; j < elements.length; j++) {
                (function(el, d) {
                    setTimeout(function() {
                        try {
                            el.scrollIntoView({ behavior: 'instant', block: 'center' });
                            el.click();
                            el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                            var playBtn = document.querySelector('.jw-display-icon-container, .vjs-big-play-button, .play-button, button[aria-label*="Play" i]');
                            if (playBtn) playBtn.click();
                        } catch(e) {}
                    }, d);
                })(elements[j], j * 250);
            }
            return elements.length;
        } catch(e) {
            return 0;
        }
    })();
"""

internal const val UNIVERSAL_SNIFFER_INTERCEPTOR_JS = """
    (function() {
        if (window.__snifferHooksInstalled) return;
        window.__snifferHooksInstalled = true;

        function toAbsUrl(url) {
            if (!url || typeof url !== 'string') return '';
            var trimmed = url.trim();
            if (trimmed.startsWith('blob:') || trimmed.startsWith('data:') || trimmed.startsWith('javascript:')) return '';
            if (trimmed.startsWith('//')) {
                return (typeof window !== 'undefined' && window.location ? window.location.protocol : 'https:') + trimmed;
            }
            if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed;
            if (trimmed.startsWith('/') || trimmed.startsWith('./') || trimmed.startsWith('../')) {
                try {
                    if (typeof window !== 'undefined' && window.location) {
                        return new URL(trimmed, window.location.href).href;
                    }
                } catch(e) {}
            }
            return '';
        }

        function isSubtitleUrl(url) {
            if (!url || typeof url !== 'string') return false;
            var clean = url.split(/[?#]/)[0].toLowerCase();
            return clean.endsWith('.vtt') || clean.endsWith('.srt') || clean.endsWith('.ass');
        }

        function isMediaUrl(url) {
            if (!url || typeof url !== 'string') return false;
            var lower = url.toLowerCase();
            return lower.indexOf('.m3u8') >= 0 || lower.indexOf('.mpd') >= 0 || lower.indexOf('.mp4') >= 0 ||
                   lower.indexOf('.mkv') >= 0 || lower.indexOf('.webm') >= 0 || lower.indexOf('/hls/') >= 0 ||
                   lower.indexOf('/backend/database/') >= 0 || lower.indexOf('/database/') >= 0 ||
                   lower.indexOf('mime=video') >= 0;
        }

        function notifyMedia(rawUrl, explicitMime) {
            if (!rawUrl || typeof rawUrl !== 'string') return;
            var abs = toAbsUrl(rawUrl);
            if (!abs) return;

            var isSub = isSubtitleUrl(abs);
            var isMedia = isMediaUrl(abs);

            if (!isSub && !isMedia && !explicitMime) {
                return;
            }

            var mime = explicitMime || null;
            if (isSub && !mime) {
                mime = abs.indexOf('.vtt') >= 0 ? 'text/vtt' : (abs.indexOf('.ass') >= 0 ? 'text/x-ssa' : 'application/x-subrip');
            } else if (!mime) {
                if (abs.indexOf('.m3u8') >= 0 || abs.indexOf('/database/atlas') >= 0 || abs.indexOf('/database/ursa') >= 0 || abs.indexOf('/database/daedalus') >= 0) {
                    mime = 'application/x-mpegURL';
                } else if (abs.indexOf('.mpd') >= 0 || abs.indexOf('/database/andromeda') >= 0 || abs.indexOf('/database/orion') >= 0) {
                    mime = 'application/dash+xml';
                } else if (abs.indexOf('.mp4') >= 0) {
                    mime = 'video/mp4';
                } else if (abs.indexOf('.webm') >= 0) {
                    mime = 'video/webm';
                } else if (abs.indexOf('.mkv') >= 0) {
                    mime = 'video/x-matroska';
                }
            }
            if (window.__hostResolverBridge && typeof window.__hostResolverBridge.postMedia === 'function') {
                try {
                    window.__hostResolverBridge.postMedia(abs, mime, isSub);
                } catch(e) {}
            }
        }

        function notifySubtitle(rawUrl, lang, name) {
            if (!rawUrl || typeof rawUrl !== 'string') return;
            var abs = toAbsUrl(rawUrl);
            if (!abs) return;
            if (window.__hostResolverBridge && typeof window.__hostResolverBridge.postSubtitle === 'function') {
                try {
                    window.__hostResolverBridge.postSubtitle(abs, lang || 'Unknown', name || (lang || 'Unknown'));
                } catch(e) {}
            }
        }

        function inspectParsedObject(obj, depth) {
            if (!obj || typeof obj !== 'object' || depth > 5) return;

            if (Array.isArray(obj)) {
                for (var i = 0; i < obj.length; i++) {
                    inspectParsedObject(obj[i], depth + 1);
                }
                return;
            }

            // Subtitles / Captions / Tracks
            var subCollections = [obj.captions, obj.subtitles, obj.tracks];
            for (var sc = 0; sc < subCollections.length; sc++) {
                var col = subCollections[sc];
                if (Array.isArray(col)) {
                    for (var s = 0; s < col.length; s++) {
                        var sub = col[s];
                        if (sub && typeof sub === 'object') {
                            var sUrl = sub.file || sub.url || sub.src || sub.link;
                            var sLang = sub.display || sub.label || sub.lang || sub.language || sub.name || 'Unknown';
                            var sName = sub.display || sub.name || sub.label || sLang;
                            if (sUrl && typeof sUrl === 'string') {
                                notifySubtitle(sUrl, sLang, sName);
                            }
                        } else if (typeof sub === 'string') {
                            notifySubtitle(sub, 'Unknown', 'Unknown');
                        }
                    }
                } else if (col && typeof col === 'object') {
                    inspectParsedObject(col, depth + 1);
                }
            }

            if (obj.kind === 'captions' || obj.kind === 'subtitles') {
                var trackUrl = obj.file || obj.url || obj.src || obj.link;
                if (trackUrl && typeof trackUrl === 'string') {
                    notifySubtitle(trackUrl, obj.label || obj.lang || obj.display || 'Unknown', obj.label || obj.display || 'Unknown');
                }
            }

            // Sources / Links / Dubs
            var mediaCollections = [obj.sources, obj.links, obj.dubs];
            for (var mc = 0; mc < mediaCollections.length; mc++) {
                var mCol = mediaCollections[mc];
                if (Array.isArray(mCol)) {
                    for (var m = 0; m < mCol.length; m++) {
                        var mItem = mCol[m];
                        if (mItem && typeof mItem === 'object') {
                            var mUrl = mItem.file || mItem.url || mItem.link || mItem.src;
                            if (mUrl && typeof mUrl === 'string') {
                                notifyMedia(mUrl, 'video/mp4');
                            }
                            inspectParsedObject(mItem, depth + 1);
                        } else if (typeof mItem === 'string') {
                            notifyMedia(mItem, 'video/mp4');
                        }
                    }
                } else if (mCol && typeof mCol === 'object') {
                    inspectParsedObject(mCol, depth + 1);
                }
            }

            if (obj.file || obj.url || obj.link) {
                var directLink = obj.file || obj.url || obj.link;
                if (typeof directLink === 'string') {
                    notifyMedia(directLink);
                }
            }

            var keys = Object.keys(obj);
            for (var k = 0; k < keys.length; k++) {
                var key = keys[k];
                if (key !== 'captions' && key !== 'subtitles' && key !== 'tracks' &&
                    key !== 'sources' && key !== 'links' && key !== 'dubs') {
                    var child = obj[key];
                    if (child && typeof child === 'object') {
                        inspectParsedObject(child, depth + 1);
                    }
                }
            }
        }

        function checkDecodedString(str) {
            if (!str || typeof str !== 'string') return;
            var trimmed = str.trim();
            if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
                try {
                    var parsed = JSON.parse(trimmed);
                    inspectParsedObject(parsed, 0);
                } catch(e) {}
            }
            if ((trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('/backend/database/') || trimmed.startsWith('/database/')) &&
                trimmed.indexOf('\n') < 0 && trimmed.indexOf('\r') < 0 && trimmed.indexOf(' ') < 0 && trimmed.indexOf('"') < 0 && trimmed.indexOf("'") < 0) {
                if (!/\.(?:js|css|html|png|jpg|jpeg|gif|webp|svg|woff2?|json)(?:\?|$|#)/i.test(trimmed)) {
                    notifyMedia(trimmed, isMediaUrl(trimmed) ? null : 'application/x-mpegURL');
                    return;
                }
            }
            try {
                var mediaRegex = /(?:https?:\/\/[^\s"'<>\\]+?(?:\.m3u8|\.mpd|\.mp4|\.mkv|\.webm|\/hls\/|\/backend\/database\/|\/database\/)[^\s"'<>\\]*|\/(?:backend\/database\/|database\/)[^\s"'<>\\]+|\/[^\s"'<>\\]+?\.(?:m3u8|mpd|mp4|webm)[^\s"'<>\\]*)/gi;
                var match;
                while ((match = mediaRegex.exec(str)) !== null) {
                    var cleaned = match[0].replace(/[)\]},;.]+$/, '');
                    notifyMedia(cleaned);
                }
                var subRegex = /(?:https?:\/\/[^\s"'<>\\]+?\.(?:vtt|srt|ass)[^\s"'<>\\]*|\/[^\s"'<>\\]+?\.(?:vtt|srt|ass)[^\s"'<>\\]*)/gi;
                while ((match = subRegex.exec(str)) !== null) {
                    var cleanSub = match[0].replace(/[)\]},;.]+$/, '');
                    notifySubtitle(cleanSub, 'Unknown', 'Unknown');
                }
            } catch(e) {}
        }

        if (typeof TextDecoder !== 'undefined' && TextDecoder.prototype && !TextDecoder.prototype._origDecode) {
            TextDecoder.prototype._origDecode = TextDecoder.prototype.decode;
            TextDecoder.prototype.decode = function() {
                var res = TextDecoder.prototype._origDecode.apply(this, arguments);
                try {
                    if (typeof res === 'string' && (res.indexOf('http://') >= 0 || res.indexOf('https://') >= 0 || res.indexOf('.m3u8') >= 0 || res.indexOf('.mpd') >= 0 || res.indexOf('/backend/database/') >= 0 || res.indexOf('/database/') >= 0)) {
                        checkDecodedString(res);
                    }
                } catch(e) {}
                return res;
            };
        }

        if (typeof window !== 'undefined' && window.atob && !window._origAtob) {
            window._origAtob = window.atob;
            window.atob = function(str) {
                var res = window._origAtob.call(this, str);
                try {
                    if (typeof res === 'string' && (res.indexOf('http://') >= 0 || res.indexOf('https://') >= 0 || res.indexOf('.m3u8') >= 0 || res.indexOf('.mpd') >= 0 || res.indexOf('/backend/database/') >= 0 || res.indexOf('/database/') >= 0)) {
                        checkDecodedString(res);
                    }
                } catch(e) {}
                return res;
            };
        }

        if (typeof JSON !== 'undefined' && JSON.parse && !JSON._origParse) {
            JSON._origParse = JSON.parse;
            JSON.parse = function() {
                var obj = JSON._origParse.apply(this, arguments);
                try {
                    if (obj && typeof obj === 'object') {
                        inspectParsedObject(obj, 0);
                    }
                } catch(e) {}
                return obj;
            };
        }

        try {
            if (typeof HTMLMediaElement !== 'undefined' && HTMLMediaElement.prototype && !HTMLMediaElement.prototype._sniffMediaHooked) {
                var srcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                if (srcDesc && srcDesc.set) {
                    var origSet = srcDesc.set;
                    Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                        set: function(val) {
                            try {
                                if (val && typeof val === 'string' && !val.startsWith('blob:') && !val.startsWith('data:')) {
                                    notifyMedia(val, 'video/mp4');
                                }
                            } catch(e) {}
                            return origSet.call(this, val);
                        },
                        get: srcDesc.get,
                        enumerable: srcDesc.enumerable,
                        configurable: srcDesc.configurable
                    });
                    HTMLMediaElement.prototype._sniffMediaHooked = true;
                }
            }
        } catch(e) {}

        try {
            if (typeof HTMLSourceElement !== 'undefined' && HTMLSourceElement.prototype && !HTMLSourceElement.prototype._sniffSourceHooked) {
                var sourceDesc = Object.getOwnPropertyDescriptor(HTMLSourceElement.prototype, 'src');
                if (sourceDesc && sourceDesc.set) {
                    var origSourceSet = sourceDesc.set;
                    Object.defineProperty(HTMLSourceElement.prototype, 'src', {
                        set: function(val) {
                            try {
                                if (val && typeof val === 'string' && !val.startsWith('blob:') && !val.startsWith('data:')) {
                                    notifyMedia(val, 'video/mp4');
                                }
                            } catch(e) {}
                            return origSourceSet.call(this, val);
                        },
                        get: sourceDesc.get,
                        enumerable: sourceDesc.enumerable,
                        configurable: sourceDesc.configurable
                    });
                    HTMLSourceElement.prototype._sniffSourceHooked = true;
                }
            }
        } catch(e) {}

        try {
            if (typeof MediaSource !== 'undefined' && MediaSource.prototype) {
                var _mediaSourceProto = MediaSource.prototype;
                ['src', 'url', '_url', '_mediaUrl', '_sourceUrl', 'sourceURL'].forEach(function(prop) {
                    var desc = Object.getOwnPropertyDescriptor(_mediaSourceProto, prop);
                    if (!desc || desc.configurable) {
                        var _val = undefined;
                        Object.defineProperty(_mediaSourceProto, prop, {
                            configurable: true,
                            enumerable: true,
                            get: function() { return _val; },
                            set: function(val) {
                                _val = val;
                                try {
                                    if (val && typeof val === 'string' && (val.indexOf('http') >= 0 || val.indexOf('/') === 0)) {
                                        notifyMedia(val, 'application/x-mpegURL');
                                    }
                                } catch(e) {}
                            }
                        });
                    }
                });
            }
        } catch(e) {}

        if (typeof URL !== 'undefined' && URL.createObjectURL && !URL._origCreateObjectURL) {
            URL._origCreateObjectURL = URL.createObjectURL;
            URL.createObjectURL = function(obj) {
                try {
                    if (obj && typeof obj === 'object') {
                        var streamUrl = obj._mediaUrl || obj.url || obj.src || obj._src || obj._sourceUrl || obj.sourceURL || obj._manifestUri || obj.manifestUri;
                        if (streamUrl && typeof streamUrl === 'string') {
                            notifyMedia(streamUrl, 'application/x-mpegURL');
                        }
                    }
                } catch(e) {}
                try {
                    return URL._origCreateObjectURL.apply(this, arguments);
                } catch(e) {
                    return 'blob:headless-stream/' + Math.random().toString(36).substring(2);
                }
            };
        }

        function hookHls(hlsCtor) {
            if (!hlsCtor || !hlsCtor.prototype) return;
            if (hlsCtor.prototype.loadSource && !hlsCtor.prototype._origLoadSource) {
                hlsCtor.prototype._origLoadSource = hlsCtor.prototype.loadSource;
                hlsCtor.prototype.loadSource = function(src) {
                    try {
                        if (src && typeof src === 'string') {
                            notifyMedia(src, 'application/x-mpegURL');
                        }
                    } catch(e) {}
                    return hlsCtor.prototype._origLoadSource.apply(this, arguments);
                };
            } else {
                var _origLs = hlsCtor.prototype.loadSource;
                Object.defineProperty(hlsCtor.prototype, 'loadSource', {
                    configurable: true,
                    enumerable: true,
                    get: function() { return _origLs; },
                    set: function(fn) {
                        _origLs = function(src) {
                            try {
                                if (src && typeof src === 'string') {
                                    notifyMedia(src, 'application/x-mpegURL');
                                }
                            } catch(e) {}
                            return fn.apply(this, arguments);
                        };
                    }
                });
            }
        }

        try {
            if (typeof window !== 'undefined') {
                if (window.Hls) {
                    hookHls(window.Hls);
                } else {
                    var _hlsHolder = undefined;
                    Object.defineProperty(window, 'Hls', {
                        configurable: true,
                        enumerable: true,
                        get: function() { return _hlsHolder; },
                        set: function(val) {
                            _hlsHolder = val;
                            hookHls(val);
                        }
                    });
                }
            }
        } catch(e) {}
    })();
"""

internal class SnifferBridge(
    private val onMedia: (url: String, mime: String?, isSub: Boolean) -> Unit,
    private val onSubtitle: (url: String, lang: String, name: String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun postMedia(url: String?, mime: String?, isSub: Boolean) {
        if (!url.isNullOrBlank()) {
            onMedia(url, mime, isSub)
        }
    }

    @android.webkit.JavascriptInterface
    fun postSubtitle(url: String?, lang: String?, name: String?) {
        if (!url.isNullOrBlank()) {
            onSubtitle(url, lang ?: "Unknown", name ?: (lang ?: "Unknown"))
        }
    }
}

/**
 * Headless, sandboxed WebView resolver.
 * Handles Cloudflare Turnstile challenges, dynamic JavaScript execution,
 * cookie collection, and network traffic sniffing without UI interaction.
 */
class HeadlessWebResolver(
    private val appContext: Context
) : BrowserResolver {

    private val mutex = Mutex()

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        @Volatile
        private var instance: HeadlessWebResolver? = null

        fun getInstance(context: Context): HeadlessWebResolver {
            return instance ?: synchronized(this) {
                instance ?: HeadlessWebResolver(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): HeadlessWebResolver? = instance
    }

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun resolve(request: BrowserResolveRequest): BrowserResolveResult {
        return mutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val contextWrapper = MutableContextWrapper(appContext)
                var webView: WebView? = null
                val sniffedUrlsList = CopyOnWriteArrayList<String>()
                val sniffedMediaList = CopyOnWriteArrayList<SniffedMediaResource>()
                val sniffedSubtitlesList = CopyOnWriteArrayList<SniffedSubtitleResource>()
                var currentUrl: String = request.url
                var pollJob: Job? = null

                try {
                    val result = withTimeoutOrNull(request.timeoutMs) {
                        suspendCancellableCoroutine<BrowserResolveResult> { cont ->
                            val wv = WebView(contextWrapper)
                            webView = wv

                            cookieManager.setAcceptThirdPartyCookies(wv, true)

                            val effectiveUa = request.userAgent ?: DEFAULT_USER_AGENT
                            wv.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = effectiveUa
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = false
                            }

                            wv.webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    // Silently block popup traps from embed players
                                    return false
                                }
                            }

                            fun finishWithResult(html: String, jsRes: String?) {
                                if (!cont.isActive) return
                                pollJob?.cancel()
                                val rawCookies = cookieManager.getCookie(currentUrl) ?: ""
                                val cookiesMap = parseCookies(rawCookies)
                                val finalRes = BrowserResolveResult(
                                    finalUrl = currentUrl,
                                    html = html,
                                    cookies = cookiesMap,
                                    cookieString = rawCookies,
                                    userAgent = wv.settings.userAgentString,
                                    sniffedUrls = sniffedUrlsList.toList(),
                                    jsResult = jsRes,
                                    sniffedMedia = sniffedMediaList.toList(),
                                    sniffedSubtitles = sniffedSubtitlesList.toList()
                                )
                                cont.resume(finalRes)
                            }

                            fun completeResolution() {
                                if (!cont.isActive) return
                                wv.evaluateJavascript("document.documentElement.outerHTML") { htmlRaw ->
                                    val unescapedHtml = unescapeJsString(htmlRaw)
                                    if (request.evaluateJsAfterLoad != null) {
                                        wv.evaluateJavascript(request.evaluateJsAfterLoad) { jsRes ->
                                            finishWithResult(unescapedHtml, jsRes?.trim('"'))
                                        }
                                    } else {
                                        finishWithResult(unescapedHtml, null)
                                    }
                                }
                            }

                            val hasTriggeredAutoClickServers = java.util.concurrent.atomic.AtomicBoolean(false)

                            fun triggerAutoClickServersIfNeeded() {
                                if (!request.autoClickServers) return
                                if (!hasTriggeredAutoClickServers.compareAndSet(false, true)) return
                                runOnMainThread {
                                    if (!cont.isActive) return@runOnMainThread
                                    wv.evaluateJavascript(AUTO_CLICK_SERVERS_JS) { countStr ->
                                        val count = countStr?.trim('"', ' ')?.toIntOrNull() ?: 0
                                        val staggerWaitMs = (count * 250L) + 2500L
                                        val serverWaitMs = maxOf(staggerWaitMs, request.extraDelayAfterLoadMs.coerceAtLeast(3500L))
                                        CoroutineScope(Dispatchers.Main.immediate).launch {
                                            delay(serverWaitMs)
                                            if (cont.isActive) {
                                                completeResolution()
                                            }
                                        }
                                    }
                                }
                            }

                            fun handleSniffedMedia(rawUrl: String, mime: String?, isSub: Boolean) {
                                val url = if (rawUrl.startsWith("/")) {
                                    try {
                                        java.net.URI(currentUrl).resolve(rawUrl).toString()
                                    } catch (_: Exception) {
                                        rawUrl
                                    }
                                } else rawUrl

                                if (!url.startsWith("http://") && !url.startsWith("https://")) return
                                val isSubtitle = isSub || SUBTITLE_SNIFF_REGEX.containsMatchIn(url)
                                val matchesUserRegex = request.urlSniffRegex != null &&
                                        (request.urlSniffRegex.matches(url) || request.urlSniffRegex.containsMatchIn(url))
                                val isMedia = isSubtitle || BUILTIN_MEDIA_SNIFF_REGEX.containsMatchIn(url) || mime != null || matchesUserRegex
                                if (!isMedia) return

                                if (isSubtitle && sniffedSubtitlesList.none { it.url == url }) {
                                    sniffedSubtitlesList.add(SniffedSubtitleResource(url = url))
                                }

                                val effectiveMime = mime ?: inferMimeType(url, isSubtitle)
                                val headers = mutableMapOf<String, String>()
                                headers["Referer"] = currentUrl
                                headers["User-Agent"] = effectiveUa
                                val pageCookie = cookieManager.getCookie(currentUrl) ?: ""
                                if (pageCookie.isNotBlank()) {
                                    headers["Cookie"] = pageCookie
                                }

                                val resource = SniffedMediaResource(
                                    url = url,
                                    headers = headers,
                                    mimeType = effectiveMime,
                                    isSubtitles = isSubtitle
                                )

                                if (sniffedMediaList.none { it.url == url }) {
                                    sniffedMediaList.add(resource)
                                    sniffedUrlsList.add(url)
                                }

                                if (request.autoClickServers && !isSubtitle) {
                                    triggerAutoClickServersIfNeeded()
                                } else if (request.stopOnFirstMediaMatch && !request.autoClickServers && !isSubtitle && cont.isActive) {
                                    runOnMainThread {
                                        completeResolution()
                                    }
                                }
                            }

                            fun handleSniffedSubtitle(rawUrl: String, lang: String, name: String) {
                                val url = if (rawUrl.startsWith("/")) {
                                    try {
                                        java.net.URI(currentUrl).resolve(rawUrl).toString()
                                    } catch (_: Exception) {
                                        rawUrl
                                    }
                                } else rawUrl

                                if (!url.startsWith("http://") && !url.startsWith("https://")) return

                                val cleanLang = lang.ifBlank { "Unknown" }
                                val cleanName = name.ifBlank { cleanLang }
                                val sub = SniffedSubtitleResource(
                                    url = url,
                                    lang = cleanLang,
                                    name = cleanName
                                )
                                val existingIdx = sniffedSubtitlesList.indexOfFirst { it.url == url }
                                if (existingIdx >= 0) {
                                    val old = sniffedSubtitlesList[existingIdx]
                                    if (old.lang == "Unknown" && cleanLang != "Unknown") {
                                        sniffedSubtitlesList[existingIdx] = sub
                                    }
                                } else {
                                    sniffedSubtitlesList.add(sub)
                                }

                                val headers = mutableMapOf<String, String>()
                                headers["Referer"] = currentUrl
                                headers["User-Agent"] = effectiveUa
                                val pageCookie = cookieManager.getCookie(currentUrl) ?: ""
                                if (pageCookie.isNotBlank()) {
                                    headers["Cookie"] = pageCookie
                                }

                                val subMedia = SniffedMediaResource(
                                    url = url,
                                    headers = headers,
                                    mimeType = inferMimeType(url, true),
                                    isSubtitles = true
                                )
                                if (sniffedMediaList.none { it.url == url }) {
                                    sniffedMediaList.add(subMedia)
                                    sniffedUrlsList.add(url)
                                }
                            }

                            wv.addJavascriptInterface(
                                SnifferBridge(
                                    onMedia = { u, m, s -> handleSniffedMedia(u, m, s) },
                                    onSubtitle = { u, l, n -> handleSniffedSubtitle(u, l, n) }
                                ),
                                "__hostResolverBridge"
                            )

                            wv.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    resourceRequest: WebResourceRequest?
                                ): Boolean {
                                    val reqUri = resourceRequest?.url ?: return false
                                    val scheme = reqUri.scheme?.lowercase() ?: ""
                                    if (scheme in BLOCKED_SCHEMES || (scheme != "http" && scheme != "https")) {
                                        return true // block popup / ad schemes
                                    }
                                    val host = reqUri.host?.lowercase() ?: ""
                                    if (BLOCKED_AD_HOSTS_REGEX.containsMatchIn(host)) {
                                        return true
                                    }
                                    return false
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url == null) return false
                                    val lower = url.lowercase()
                                    for (scheme in BLOCKED_SCHEMES) {
                                        if (lower.startsWith("${'$'}scheme:")) return true
                                    }
                                    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return true
                                    if (BLOCKED_AD_HOSTS_REGEX.containsMatchIn(lower)) return true
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (url != null) currentUrl = url
                                    view?.evaluateJavascript(UNIVERSAL_SNIFFER_INTERCEPTOR_JS, null)
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    super.onPageCommitVisible(view, url)
                                    view?.evaluateJavascript(UNIVERSAL_SNIFFER_INTERCEPTOR_JS, null)
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    resourceRequest: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = resourceRequest?.url?.toString()
                                    if (reqUrl != null && !AD_FILTER_REGEX.containsMatchIn(reqUrl)) {
                                        val matchesUserRegex = request.urlSniffRegex != null &&
                                                (request.urlSniffRegex.matches(reqUrl) || request.urlSniffRegex.containsMatchIn(reqUrl))
                                        val matchesBuiltin = request.stopOnFirstMediaMatch && BUILTIN_MEDIA_SNIFF_REGEX.containsMatchIn(reqUrl)

                                        if (matchesUserRegex || matchesBuiltin) {
                                            val headers = resourceRequest.requestHeaders?.toMutableMap() ?: mutableMapOf()
                                            if (!headers.containsKey("Referer") && !headers.containsKey("referer")) {
                                                headers["Referer"] = currentUrl
                                            }
                                            if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
                                                headers["User-Agent"] = effectiveUa
                                            }
                                            val pageCookie = cookieManager.getCookie(currentUrl) ?: ""
                                            val mediaCookie = cookieManager.getCookie(reqUrl) ?: ""
                                            val cookieStr = when {
                                                pageCookie.isNotBlank() && mediaCookie.isNotBlank() && pageCookie != mediaCookie -> "${'$'}pageCookie; ${'$'}mediaCookie"
                                                pageCookie.isNotBlank() -> pageCookie
                                                else -> mediaCookie
                                            }
                                            if (cookieStr.isNotBlank() && !headers.containsKey("Cookie") && !headers.containsKey("cookie")) {
                                                headers["Cookie"] = cookieStr
                                            }

                                            val isSub = SUBTITLE_SNIFF_REGEX.containsMatchIn(reqUrl)
                                            val mime = inferMimeType(reqUrl, isSub)
                                            val resource = SniffedMediaResource(
                                                url = reqUrl,
                                                headers = headers,
                                                mimeType = mime,
                                                isSubtitles = isSub
                                            )

                                            if (sniffedMediaList.none { it.url == reqUrl }) {
                                                sniffedMediaList.add(resource)
                                                sniffedUrlsList.add(reqUrl)
                                            }

                                            if (isSub && sniffedSubtitlesList.none { it.url == reqUrl }) {
                                                sniffedSubtitlesList.add(SniffedSubtitleResource(url = reqUrl))
                                            }

                                            // Fast Early Exit or Trigger Server Clicking
                                            if (request.autoClickServers && !isSub) {
                                                triggerAutoClickServersIfNeeded()
                                            } else if (request.stopOnFirstMediaMatch && !request.autoClickServers && !isSub && cont.isActive) {
                                                runOnMainThread {
                                                    completeResolution()
                                                }
                                            }
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, resourceRequest)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url != null) currentUrl = url
                                    view?.evaluateJavascript(UNIVERSAL_SNIFFER_INTERCEPTOR_JS, null)

                                    pollJob?.cancel()
                                    pollJob = CoroutineScope(Dispatchers.Main.immediate).launch {
                                        // 1. Wait for Cloudflare Turnstile challenge to clear
                                        var isChallenged = true
                                        while (isChallenged && cont.isActive) {
                                            val title = suspendCancellableCoroutine<String> { titleCont ->
                                                view?.evaluateJavascript("document.title") { tRaw ->
                                                    if (titleCont.isActive) {
                                                        titleCont.resume(tRaw?.trim('"') ?: "")
                                                    }
                                                }
                                            }
                                            isChallenged = title.contains("Just a moment", ignoreCase = true) ||
                                                    title.contains("Attention Required", ignoreCase = true) ||
                                                    title.contains("Cloudflare", ignoreCase = true)
                                            if (isChallenged) {
                                                delay(300L)
                                            }
                                        }

                                        if (!cont.isActive) return@launch

                                        // 2. Play-Button Auto-Clicker
                                        if ((request.urlSniffRegex != null || request.stopOnFirstMediaMatch || request.autoClickServers) && request.autoClickPlaySelectors) {
                                            view?.evaluateJavascript(AUTO_CLICK_PLAY_JS, null)
                                        }

                                        // 3. Wait for selector or text
                                        if (request.waitForSelector != null) {
                                            val sel = request.waitForSelector
                                            while (cont.isActive) {
                                                val found = suspendCancellableCoroutine<Boolean> { selCont ->
                                                    view?.evaluateJavascript("!!document.querySelector(${'$'}{escapeJsString(sel)})") { res ->
                                                        if (selCont.isActive) selCont.resume(res == "true")
                                                    }
                                                }
                                                if (found || sniffedMediaList.isNotEmpty()) break
                                                delay(250L)
                                            }
                                        } else if (request.waitForText != null) {
                                            val txt = request.waitForText
                                            while (cont.isActive) {
                                                val found = suspendCancellableCoroutine<Boolean> { txtCont ->
                                                    view?.evaluateJavascript("(document.body ? document.body.innerText : '').includes(${'$'}{escapeJsString(txt)})") { res ->
                                                        if (txtCont.isActive) txtCont.resume(res == "true")
                                                    }
                                                }
                                                if (found || sniffedMediaList.isNotEmpty()) break
                                                delay(250L)
                                            }
                                        }

                                        // 4. Auto-Click Server Tabs if autoClickServers is true
                                        if (request.autoClickServers) {
                                            val initialWaitStart = System.currentTimeMillis()
                                            val maxInitialWait = (request.timeoutMs - 4000L).coerceIn(3000L, 7000L)
                                            while (cont.isActive && sniffedMediaList.none { !it.isSubtitles } && System.currentTimeMillis() - initialWaitStart < maxInitialWait) {
                                                delay(200L)
                                            }
                                            // If initial stream was already sniffed, triggerAutoClickServersIfNeeded() was already called.
                                            // Otherwise trigger now as a fallback.
                                            triggerAutoClickServersIfNeeded()
                                        } else if ((request.urlSniffRegex != null || request.stopOnFirstMediaMatch) && sniffedMediaList.isEmpty()) {
                                            // Smart waiting if media sniffing is active and sniffedMedia is still empty
                                            val extraWait = request.extraDelayAfterLoadMs.coerceAtLeast(3500L)
                                            val waitStart = System.currentTimeMillis()
                                            while (cont.isActive && sniffedMediaList.isEmpty()) {
                                                if (System.currentTimeMillis() - waitStart >= extraWait) break
                                                delay(200L)
                                            }
                                        }

                                        // 5. Complete resolution (for non-autoClickServers flows)
                                        if (!request.autoClickServers && cont.isActive) {
                                            completeResolution()
                                        }
                                    }
                                }
                            }

                            if (request.headers.isNotEmpty()) {
                                wv.loadUrl(request.url, request.headers)
                            } else {
                                wv.loadUrl(request.url)
                            }

                            cont.invokeOnCancellation {
                                pollJob?.cancel()
                                runOnMainThread {
                                    wv.stopLoading()
                                }
                            }
                        }
                    }

                    result ?: BrowserResolveResult(
                        finalUrl = currentUrl,
                        html = "",
                        cookies = parseCookies(cookieManager.getCookie(currentUrl) ?: ""),
                        cookieString = cookieManager.getCookie(currentUrl) ?: "",
                        userAgent = request.userAgent ?: DEFAULT_USER_AGENT,
                        sniffedUrls = sniffedUrlsList.toList(),
                        sniffedMedia = sniffedMediaList.toList(),
                        sniffedSubtitles = sniffedSubtitlesList.toList()
                    )
                } finally {
                    pollJob?.cancel()
                    runOnMainThread {
                        webView?.stopLoading()
                        webView?.clearHistory()
                        webView?.clearCache(true)
                        webView?.destroy()
                    }
                }
            }
        }
    }

        @SuppressLint("SetJavaScriptEnabled")
        override suspend fun evaluateJavascript(url: String, script: String, timeoutMs: Long): String {
            return mutex.withLock {
                withContext(Dispatchers.Main.immediate) {
                    var webView: WebView? = null
                    try {
                        val result = withTimeoutOrNull(timeoutMs) {
                            suspendCancellableCoroutine<String> { cont ->
                                val wv = WebView(MutableContextWrapper(appContext))
                                webView = wv
                                wv.settings.javaScriptEnabled = true
                                wv.settings.userAgentString = DEFAULT_USER_AGENT

                                wv.webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        view?.evaluateJavascript(UNIVERSAL_SNIFFER_INTERCEPTOR_JS, null)
                                    }

                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                        view?.evaluateJavascript(UNIVERSAL_SNIFFER_INTERCEPTOR_JS) {
                                            view.evaluateJavascript(script) { res ->
                                                if (cont.isActive) {
                                                    cont.resume(res ?: "")
                                                }
                                            }
                                        }
                                    }
                                }

                                wv.loadUrl(url)
                                cont.invokeOnCancellation {
                                    runOnMainThread {
                                        wv.stopLoading()
                                    }
                                }
                            }
                        }
                        result ?: ""
                    } finally {
                        runOnMainThread {
                            webView?.stopLoading()
                            webView?.destroy()
                        }
                    }
                }
            }
        }

        private fun runOnMainThread(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                try {
                    block()
                } catch (_: Throwable) {}
            } else {
                Handler(Looper.getMainLooper()).post {
                    try {
                        block()
                    } catch (_: Throwable) {}
                }
            }
        }

    private fun parseCookies(rawCookieHeader: String): Map<String, String> {
        if (rawCookieHeader.isBlank()) return emptyMap()
        return rawCookieHeader.split(";")
            .mapNotNull {
                val parts = it.trim().split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    }

    private fun unescapeJsString(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        var s = raw
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1)
        }
        return s.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun inferMimeType(url: String, isSubtitles: Boolean): String? {
        if (isSubtitles) {
            return when {
                url.contains(".vtt", ignoreCase = true) -> "text/vtt"
                url.contains(".ass", ignoreCase = true) -> "text/x-ssa"
                else -> "application/x-subrip"
            }
        }
        return when {
            url.contains(".m3u8", ignoreCase = true) ||
                    url.contains("/master.", ignoreCase = true) ||
                    url.contains("/playlist", ignoreCase = true) ||
                    url.contains("/hls/", ignoreCase = true) ||
                    url.contains("/database/atlas", ignoreCase = true) ||
                    url.contains("/database/ursa", ignoreCase = true) ||
                    url.contains("/database/daedalus", ignoreCase = true) -> "application/x-mpegURL"
            url.contains(".mpd", ignoreCase = true) ||
                    url.contains("/database/andromeda", ignoreCase = true) ||
                    url.contains("/database/orion", ignoreCase = true) -> "application/dash+xml"
            url.contains(".mp4", ignoreCase = true) -> "video/mp4"
            url.contains(".mkv", ignoreCase = true) -> "video/x-matroska"
            url.contains(".webm", ignoreCase = true) -> "video/webm"
            else -> null
        }
    }

    private fun escapeJsString(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
    }
}
