package com.euthopiar.core.model

import com.euthopiar.core.browser.BrowserResolver
import com.euthopiar.core.aniskip.AniSkipClient
import com.euthopiar.core.subtitle.external.SubDlClient
import com.euthopiar.core.util.TmdbBridge
import okhttp3.OkHttpClient

/**
 * Host Application API provided to Universal Plugins.
 * Bridges external service clients and headless browser capabilities
 * without giving plugins direct Android Context or Activity references.
 */
interface HostApi {
    val browserResolver: BrowserResolver
    val okHttpClient: OkHttpClient
    val defaultUserAgent: String
    val tmdbBridge: TmdbBridge
    val subDlClient: SubDlClient
    val aniSkipClient: AniSkipClient
}
