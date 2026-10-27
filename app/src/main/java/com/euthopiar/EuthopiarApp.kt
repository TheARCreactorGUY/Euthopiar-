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
        com.euthopiar.ui.player.sequential.SequentialStreamManager.cleanOrphanedCache(this)

        // Synchronously initialize JsPluginStorage, AddonRepository, and P2P engine instances
        com.euthopiar.core.js.JsPluginStorage.getInstance(this)
        com.euthopiar.core.addons.AddonRepository.getInstance(this)
        com.euthopiar.core.p2p.P2pStreamingEngine.initialize(this)
        com.euthopiar.core.p2p.P2pSettingsRepository.initialize(this)
        com.euthopiar.core.browser.HeadlessWebResolver.getInstance(this)

        // Initialize plugins, prefetch DNS, and restore default selected provider across restarts
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.euthopiar.core.js.JsPluginStorage.getInstance(this@EuthopiarApp).initialize()
                com.euthopiar.core.addons.AddonRepository.getInstance(this@EuthopiarApp).initialize()
            } catch (_: Exception) {}

            // DNS pre-fetch for critical media & metadata hosts
            DohDns.DEFAULT.prefetch(
                "euthopiar-vault.sehgalvansh716.workers.dev",
                "api.themoviedb.org",
                "image.tmdb.org",
                "graphql.anilist.co"
            )

            try {
                val prefsRepo = PreferencesRepository.getInstance(this@EuthopiarApp)
                launch {
                    kotlinx.coroutines.flow.combine(
                        prefsRepo.debridEnabled,
                        prefsRepo.activeDebridProvider,
                        prefsRepo.realDebridApiKey,
                        prefsRepo.allDebridApiKey,
                        prefsRepo.torBoxApiKey
                    ) { enabled, provider, rdKey, adKey, tbKey ->
                        if (!enabled) {
                            com.euthopiar.core.debrid.DebridManager.getInstance().setActiveProviders(emptyList())
                            return@combine
                        }
                        val pType = com.euthopiar.core.debrid.DebridProviderType.fromId(provider)
                        val key = when (pType) {
                            com.euthopiar.core.debrid.DebridProviderType.REAL_DEBRID -> rdKey
                            com.euthopiar.core.debrid.DebridProviderType.ALL_DEBRID -> adKey
                            com.euthopiar.core.debrid.DebridProviderType.TORBOX -> tbKey
                            com.euthopiar.core.debrid.DebridProviderType.NONE -> ""
                        }
                        if (pType != com.euthopiar.core.debrid.DebridProviderType.NONE && key.isNotBlank()) {
                            com.euthopiar.core.debrid.DebridManager.getInstance().setActiveProviders(
                                listOf(com.euthopiar.core.debrid.DebridManager.ActiveDebridProvider(pType, true))
                            )
                        } else {
                            com.euthopiar.core.debrid.DebridManager.getInstance().setActiveProviders(emptyList())
                        }
                    }.collect { }
                }
            } catch (_: Exception) {}

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

            launch(Dispatchers.IO) {
                try {
                    val updated = PluginRepositoryService.getInstance(this@EuthopiarApp)
                        .checkAndAutoUpdatePlugins(installNewFromRepos = false)
                    if (updated > 0) {
                        android.util.Log.i("EuthopiarApp", "Auto-updated $updated provider plugin(s) on launch")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("EuthopiarApp", "Startup plugin auto-update error: ${e.message}")
                }
            }
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
