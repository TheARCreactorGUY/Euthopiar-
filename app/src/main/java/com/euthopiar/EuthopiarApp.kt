package com.euthopiar

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.euthopiar.app.plugin.PluginRepositoryService
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.core.util.AppSessionManager
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

class EuthopiarApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("euthopiar_session", Context.MODE_PRIVATE)
        val lastIndex = prefs.getInt("launch_session_index", 0)
        val nextIndex = lastIndex + 1
        prefs.edit().putInt("launch_session_index", nextIndex).apply()
        AppSessionManager.initialize(nextIndex)

        // Initialize 50MB HTTP disk cache and connection pool
        DohDns.initHttpCache(cacheDir)

        // Synchronously initialize JsPluginStorage, AddonRepository, and P2P engine instances
        com.euthopiar.core.js.JsPluginStorage.getInstance(this)
        com.euthopiar.core.addons.AddonRepository.getInstance(this)
        com.euthopiar.core.p2p.P2pStreamingEngine.initialize(this)
        com.euthopiar.core.p2p.P2pSettingsRepository.initialize(this)

        // Initialize plugins, prefetch DNS, and restore default selected provider across restarts
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.euthopiar.core.js.JsPluginStorage.getInstance(this@EuthopiarApp).initialize()
                com.euthopiar.core.addons.AddonRepository.getInstance(this@EuthopiarApp).initialize()
            } catch (_: Exception) {}

            // DNS pre-fetch for critical media & metadata hosts
            DohDns.DEFAULT.prefetch(
                "api.themoviedb.org",
                "image.tmdb.org",
                "graphql.anilist.co"
            )

            try {
                val savedProvider = PreferencesRepository.getInstance(this@EuthopiarApp).activeProvider.first()
                if (savedProvider.isNotBlank()) {
                    PluginRegistry.Default.setActivePlugin(savedProvider)
                }
            } catch (_: Exception) {}

            try {
                PluginRepositoryService.getInstance(this@EuthopiarApp).initializePlugins()
            } catch (_: Exception) {}

            try {
                val prefsRepo = PreferencesRepository.getInstance(this@EuthopiarApp)
                val savedProvider = prefsRepo.activeProvider.first()
                if (savedProvider.isNotBlank() && PluginRegistry.Default.getPlugin(savedProvider) != null) {
                    PluginRegistry.Default.setActivePlugin(savedProvider)
                } else {
                    val firstPublic = PluginRepositoryService.getInstance(this@EuthopiarApp)
                        .getInstalledPublicPlugins()
                        .firstOrNull { it.isEnabled }
                    if (firstPublic != null) {
                        PluginRegistry.Default.setActivePlugin(firstPublic.name)
                        prefsRepo.setActiveProvider(firstPublic.name)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { DohDns.sharedBaseClient }
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.HARDWARE)
            .crossfade(220)
            .build()
    }
}
