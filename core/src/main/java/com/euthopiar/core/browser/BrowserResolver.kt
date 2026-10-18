package com.euthopiar.core.browser

/**
 * Request payload for resolving dynamic or protected web pages via the Host Browser Engine.
 */
data class BrowserResolveRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val timeoutMs: Long = 20000L,
    val waitForSelector: String? = null,
    val waitForText: String? = null,
    val urlSniffRegex: Regex? = null,
    val evaluateJsAfterLoad: String? = null
)

/**
 * Result returned after headless or interactive browser resolution.
 */
data class BrowserResolveResult(
    val finalUrl: String,
    val html: String,
    val cookies: Map<String, String>,
    val cookieString: String,
    val userAgent: String,
    val sniffedUrls: List<String> = emptyList(),
    val jsResult: String? = null
)

/**
 * Host Browser Resolver interface.
 * Exposes controlled browser capabilities (Cloudflare Turnstile bypass, JS execution,
 * background request sniffing) to Universal Plugins without leaking Android Context or UI widgets.
 */
interface BrowserResolver {
    /**
     * Resolves a URL in a headless sandboxed browser context.
     * Automatically waits for Cloudflare / Turnstile challenges to clear,
     * extracts outer HTML, captures cookies, and sniffs matching network requests.
     */
    suspend fun resolve(request: BrowserResolveRequest): BrowserResolveResult

    /**
     * Convenience method to resolve a URL with default parameters.
     */
    suspend fun resolve(url: String, timeoutMs: Long = 20000L): BrowserResolveResult =
        resolve(BrowserResolveRequest(url = url, timeoutMs = timeoutMs))

    /**
     * Evaluates a custom JavaScript snippet on a webpage and returns the string result.
     */
    suspend fun evaluateJavascript(url: String, script: String, timeoutMs: Long = 15000L): String
}
