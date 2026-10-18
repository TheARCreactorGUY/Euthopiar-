package com.euthopiar.app.plugin

import android.content.Context
import com.euthopiar.core.aniskip.AniSkipClient
import com.euthopiar.core.browser.BrowserResolver
import com.euthopiar.core.browser.HeadlessWebResolver
import com.euthopiar.core.model.HostApi
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.subtitle.external.SubDlClient
import com.euthopiar.core.util.TmdbBridge
import okhttp3.OkHttpClient

/**
 * Standard implementation of [HostApi] supplied by the host application to Universal Plugins.
 */
class DefaultHostApi(
    context: Context
) : HostApi {
    private val appContext = context.applicationContext

    override val browserResolver: BrowserResolver by lazy {
        HeadlessWebResolver.getInstance(appContext)
    }

    override val okHttpClient: OkHttpClient by lazy {
        DohDns.createOkHttpClient()
    }

    override val defaultUserAgent: String = HeadlessWebResolver.DEFAULT_USER_AGENT

    override val tmdbBridge: TmdbBridge = TmdbBridge

    override val subDlClient: SubDlClient by lazy {
        SubDlClient()
    }

    override val aniSkipClient: AniSkipClient by lazy {
        AniSkipClient()
    }

    companion object {
        @Volatile
        private var instance: DefaultHostApi? = null

        fun getInstance(context: Context): DefaultHostApi {
            return instance ?: synchronized(this) {
                instance ?: DefaultHostApi(context.applicationContext).also { instance = it }
            }
        }
    }
}
