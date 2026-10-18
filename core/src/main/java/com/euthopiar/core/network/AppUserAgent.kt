package com.euthopiar.core.network

import android.content.Context
import android.webkit.WebSettings

/**
 * Provides a single, unified User-Agent across WebView, OkHttp, and Media3.
 * Essential because cf_clearance tokens are cryptographically bound to the User-Agent.
 */
object AppUserAgent {

    @Volatile
    private var cachedUserAgent: String? = null

    private const val FALLBACK_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun get(context: Context? = null): String {
        cachedUserAgent?.let { return it }

        if (context != null) {
            try {
                val ua = WebSettings.getDefaultUserAgent(context.applicationContext)
                if (!ua.isNullOrBlank()) {
                    cachedUserAgent = ua
                    return ua
                }
            } catch (_: Exception) {}
        }

        return cachedUserAgent ?: FALLBACK_USER_AGENT
    }

    fun initialize(context: Context) {
        try {
            cachedUserAgent = WebSettings.getDefaultUserAgent(context.applicationContext)
        } catch (_: Exception) {
            cachedUserAgent = FALLBACK_USER_AGENT
        }
    }
}
