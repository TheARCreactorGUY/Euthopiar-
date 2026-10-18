package com.euthopiar.core.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

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
    }

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun resolve(request: BrowserResolveRequest): BrowserResolveResult {
        return mutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val contextWrapper = MutableContextWrapper(appContext)
                var webView: WebView? = null
                val sniffedList = CopyOnWriteArrayList<String>()

                try {
                    val result = withTimeoutOrNull(request.timeoutMs) {
                        suspendCancellableCoroutine<BrowserResolveResult> { cont ->
                            val wv = WebView(contextWrapper)
                            webView = wv

                            cookieManager.setAcceptThirdPartyCookies(wv, true)

                            wv.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = request.userAgent ?: DEFAULT_USER_AGENT
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }

                            wv.webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    resourceRequest: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = resourceRequest?.url?.toString()
                                    if (reqUrl != null && request.urlSniffRegex != null) {
                                        if (request.urlSniffRegex.matches(reqUrl) || request.urlSniffRegex.containsMatchIn(reqUrl)) {
                                            sniffedList.add(reqUrl)
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, resourceRequest)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val currentUrl = url ?: request.url

                                    // Check if Cloudflare Turnstile challenge is still active
                                    view?.evaluateJavascript("document.title") { titleRaw ->
                                        val title = titleRaw?.replace("\"", "") ?: ""
                                        val isChallenged = title.contains("Just a moment", ignoreCase = true) ||
                                                title.contains("Attention Required", ignoreCase = true) ||
                                                title.contains("Cloudflare", ignoreCase = true)

                                        if (!isChallenged && cont.isActive) {
                                            // Page challenge resolved! Extract outer HTML and cookies
                                            view.evaluateJavascript("document.documentElement.outerHTML") { htmlRaw ->
                                                val unescapedHtml = unescapeJsString(htmlRaw)
                                                val rawCookies = cookieManager.getCookie(currentUrl) ?: ""
                                                val cookiesMap = parseCookies(rawCookies)

                                                if (request.evaluateJsAfterLoad != null) {
                                                    view.evaluateJavascript(request.evaluateJsAfterLoad) { jsRes ->
                                                        val finalRes = BrowserResolveResult(
                                                            finalUrl = currentUrl,
                                                            html = unescapedHtml,
                                                            cookies = cookiesMap,
                                                            cookieString = rawCookies,
                                                            userAgent = wv.settings.userAgentString,
                                                            sniffedUrls = sniffedList.toList(),
                                                            jsResult = jsRes?.trim('"')
                                                        )
                                                        if (cont.isActive) cont.resume(finalRes)
                                                    }
                                                } else {
                                                    val finalRes = BrowserResolveResult(
                                                        finalUrl = currentUrl,
                                                        html = unescapedHtml,
                                                        cookies = cookiesMap,
                                                        cookieString = rawCookies,
                                                        userAgent = wv.settings.userAgentString,
                                                        sniffedUrls = sniffedList.toList()
                                                    )
                                                    if (cont.isActive) cont.resume(finalRes)
                                                }
                                            }
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
                                    runOnMainThread {
                                        wv.stopLoading()
                                    }
                                }
                            }
                        }

                        result ?: BrowserResolveResult(
                            finalUrl = request.url,
                            html = "",
                            cookies = parseCookies(cookieManager.getCookie(request.url) ?: ""),
                            cookieString = cookieManager.getCookie(request.url) ?: "",
                            userAgent = request.userAgent ?: DEFAULT_USER_AGENT,
                            sniffedUrls = sniffedList.toList()
                        )
                    } finally {
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
                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                        view?.evaluateJavascript(script) { res ->
                                            if (cont.isActive) {
                                                cont.resume(res ?: "")
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
}
