package com.euthopiar.core.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Bridges Android WebView's CookieManager to OkHttp.
 * CookieManager is thread-safe; WebView itself is not.
 * Ensures cookies set during Cloudflare challenges (cf_clearance, __cf_bm)
 * are seamlessly loaded by OkHttp and Media3.
 */
class WebViewCookieJar(
    private val cookieManager: CookieManager = CookieManager.getInstance()
) : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieString = try {
            cookieManager.getCookie(url.toString())
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        return cookieString.split("; ")
            .mapNotNull { Cookie.parse(url, it) }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        try {
            cookies.forEach { cookie ->
                cookieManager.setCookie(url.toString(), cookie.toString())
            }
            cookieManager.flush()
        } catch (_: Exception) {
            // Non-critical if background flush fails
        }
    }
}
