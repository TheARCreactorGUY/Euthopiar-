package com.euthopiar.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.app.plugin.PluginRepositoryService
import com.euthopiar.core.addons.AddonRepository
import com.euthopiar.core.addons.AddonsUiState
import com.euthopiar.core.addons.ManagedAddon
import com.euthopiar.core.js.JsUniversalPlugin
import com.euthopiar.core.js.network.httpRequestRaw
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginManagerViewModel(
    application: Application,
    private val pluginService: PluginRepositoryService,
    private val preferencesRepository: PreferencesRepository,
    private val registry: PluginRegistry
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        PluginRepositoryService.getInstance(application),
        PreferencesRepository.getInstance(application),
        PluginRegistry.Default
    )

    enum class Tab {
        INSTALLED,
        MARKETPLACE,
        REPOSITORIES
    }

    private val _selectedTab = MutableStateFlow(Tab.INSTALLED)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    data class ProviderRepoInfo(
        val url: String,
        val name: String,
        val description: String,
        val plugins: List<PluginManifest>
    )

    private val addonRepository = AddonRepository.getInstance(application)
    val addonsUiState: StateFlow<AddonsUiState> = addonRepository.uiState

    private val _installedPlugins = MutableStateFlow<List<PluginManifest>>(emptyList())
    val installedPlugins: StateFlow<List<PluginManifest>> = _installedPlugins.asStateFlow()

    private val _communityPlugins = MutableStateFlow<List<PluginManifest>>(emptyList())
    val communityPlugins: StateFlow<List<PluginManifest>> = _communityPlugins.asStateFlow()

    private val _providerRepos = MutableStateFlow<List<ProviderRepoInfo>>(emptyList())
    val providerRepos: StateFlow<List<ProviderRepoInfo>> = _providerRepos.asStateFlow()

    private val _selectedProviderRepo = MutableStateFlow<ProviderRepoInfo?>(null)
    val selectedProviderRepo: StateFlow<ProviderRepoInfo?> = _selectedProviderRepo.asStateFlow()

    fun selectProviderRepo(repo: ProviderRepoInfo?) {
        _selectedProviderRepo.value = repo
    }

    val repositoryUrls: StateFlow<Set<String>> = preferencesRepository.repositoryUrls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _installingPluginIds = MutableStateFlow<Set<String>>(emptySet())
    val installingPluginIds: StateFlow<Set<String>> = _installingPluginIds.asStateFlow()

    data class PluginHealth(
        val isWorking: Boolean,
        val message: String
    )

    private val _testingPluginIds = MutableStateFlow<Set<String>>(emptySet())
    val testingPluginIds: StateFlow<Set<String>> = _testingPluginIds.asStateFlow()

    private val _pluginHealthMap = MutableStateFlow<Map<String, PluginHealth>>(emptyMap())
    val pluginHealthMap: StateFlow<Map<String, PluginHealth>> = _pluginHealthMap.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val activePluginName: StateFlow<String> = registry.activePlugin.map { it.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), registry.activePlugin.value.name)

    fun testPlugin(manifest: PluginManifest, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _testingPluginIds.value = _testingPluginIds.value + manifest.id
            val startTime = System.currentTimeMillis()
            try {
                val cleanId = manifest.id.substringAfter(":")
                val registered = registry.plugins.value.find {
                    it.name.equals(manifest.name, ignoreCase = true) ||
                    (it is JsUniversalPlugin && (it.scraper.id == manifest.id || it.scraper.id.substringAfter(":") == cleanId))
                }
                val isHealthy: Boolean
                val message: String
                if (registered != null) {
                    kotlinx.coroutines.withTimeout(8000) {
                        registered.getStreams("603")
                    }
                    val latency = System.currentTimeMillis() - startTime
                    isHealthy = true
                    message = "Working · ${latency} ms"
                } else if (manifest.siteUrl.isNotBlank()) {
                    val response = kotlinx.coroutines.withTimeout(8000) {
                        httpRequestRaw("GET", manifest.siteUrl, emptyMap(), "", followRedirects = true)
                    }
                    val latency = System.currentTimeMillis() - startTime
                    if (response.status in 200..399) {
                        isHealthy = true
                        message = "Working · ${latency} ms"
                    } else {
                        isHealthy = false
                        message = "HTTP ${response.status}"
                    }
                } else {
                    isHealthy = false
                    message = "Not responding"
                }

                val health = PluginHealth(isWorking = isHealthy, message = message)
                val newMap = _pluginHealthMap.value.toMutableMap()
                newMap[manifest.id] = health
                newMap[cleanId] = health
                _pluginHealthMap.value = newMap
                withContext(Dispatchers.Main) {
                    onResult?.invoke(isHealthy, message)
                }
            } catch (e: Exception) {
                val cleanId = manifest.id.substringAfter(":")
                val err = e.message?.takeIf { it.isNotBlank() } ?: "Not responding"
                val health = PluginHealth(isWorking = false, message = err)
                val newMap = _pluginHealthMap.value.toMutableMap()
                newMap[manifest.id] = health
                newMap[cleanId] = health
                _pluginHealthMap.value = newMap
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, err)
                }
            } finally {
                _testingPluginIds.value = _testingPluginIds.value - manifest.id
            }
        }
    }

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun setActivePlugin(pluginName: String) {
        viewModelScope.launch {
            registry.setActivePlugin(pluginName)
            preferencesRepository.setActiveProvider(pluginName)
        }
    }

    fun syncCommunityRepository(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val existingUrls = preferencesRepository.repositoryUrls.first().filter { it.isNotBlank() }
                val urls = if (existingUrls.isEmpty()) {
                    val defaultUrl = PluginRepositoryService.DEFAULT_REPO_URL
                    preferencesRepository.addRepositoryUrl(defaultUrl)
                    listOf(defaultUrl)
                } else {
                    existingUrls
                }
                var total = 0
                for (url in urls) {
                    val count = pluginService.installAllFromRepo(url)
                    total += count
                }
                loadPlugins()
                _selectedTab.value = Tab.MARKETPLACE
                withContext(Dispatchers.Main) {
                    onResult(true, "Installed $total plugins from repositories")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Failed to sync repositories")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun fetchTestEupsRepository(onResult: (Boolean, String?) -> Unit) = syncCommunityRepository(onResult)

    init {
        loadPlugins()
    }

    fun loadPlugins() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                addonRepository.initialize()
                val installed = pluginService.getInstalledPlugins().distinctBy { it.id }
                _installedPlugins.value = installed.filterNot { it.isAdultPlugin }

                // Fetch community plugins from configured repos
                val urls = preferencesRepository.repositoryUrls.first()
                val activeUrls = urls.filter { it.isNotBlank() }
                val discoveredRepos = mutableListOf<ProviderRepoInfo>()
                val discovered = mutableListOf<PluginManifest>()

                for (url in activeUrls) {
                    try {
                        val repoIndex = pluginService.fetchRepository(url)
                        val repoPlugins = repoIndex.plugins.filterNot { it.isAdultPlugin }
                        if (repoPlugins.isNotEmpty()) {
                            discoveredRepos.add(
                                ProviderRepoInfo(
                                    url = url,
                                    name = repoIndex.name.ifBlank { url.substringAfterLast("/").substringBefore(".json") },
                                    description = repoIndex.description,
                                    plugins = repoPlugins
                                )
                            )
                            discovered.addAll(repoPlugins)
                        }
                    } catch (_: Exception) {}
                }

                _providerRepos.value = discoveredRepos
                _communityPlugins.value = discovered.distinctBy { it.id }

                val currentSelected = _selectedProviderRepo.value
                if (currentSelected != null) {
                    _selectedProviderRepo.value = discoveredRepos.find { it.url == currentSelected.url }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun togglePluginEnabled(manifest: PluginManifest) {
        viewModelScope.launch {
            val newEnabled = !manifest.isEnabled
            pluginService.setPluginEnabled(manifest, newEnabled)
            _installedPlugins.value = _installedPlugins.value.map {
                if (it.id == manifest.id) it.copy(isEnabled = newEnabled) else it
            }
        }
    }

    fun installPlugin(manifest: PluginManifest, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _installingPluginIds.value = _installingPluginIds.value + manifest.id
            val success = pluginService.downloadAndInstallPlugin(manifest)
            _installingPluginIds.value = _installingPluginIds.value - manifest.id

            if (success) {
                loadPlugins()
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun installFromUri(uri: Uri, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = pluginService.installFromUri(uri)
            _isRefreshing.value = false
            if (result.isSuccess) {
                loadPlugins()
                withContext(Dispatchers.Main) {
                    onComplete(true, result.getOrNull()?.name)
                }
            } else {
                val err = result.exceptionOrNull()
                android.util.Log.w("PluginManagerViewModel", "Failed to install plugin from URI", err)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Installation failed")
                }
            }
        }
    }

    fun uninstallPlugin(manifest: PluginManifest, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = pluginService.uninstallPlugin(manifest.id)
            if (success) {
                loadPlugins()
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun addRepository(url: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val trimmed = url.trim()
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Invalid URL: Must start with http:// or https://")
                }
                return@launch
            }

            try {
                val count = pluginService.installAllFromRepo(trimmed)
                preferencesRepository.addRepositoryUrl(trimmed)
                loadPlugins()
                withContext(Dispatchers.Main) {
                    onResult(true, "Installed $count plugins from repository")
                }
            } catch (e: Exception) {
                android.util.Log.w("PluginManagerViewModel", "Failed to load repository: $trimmed", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to load repository")
                }
            }
        }
    }

    fun installAllInRepo(repoUrl: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val count = pluginService.installAllFromRepo(repoUrl)
                loadPlugins()
                withContext(Dispatchers.Main) {
                    onResult(count)
                }
            } catch (e: Exception) {
                android.util.Log.e("PluginManagerViewModel", "Failed to install all plugins from repo: $repoUrl", e)
                withContext(Dispatchers.Main) {
                    onResult(0)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun removeRepository(url: String) {
        viewModelScope.launch {
            pluginService.removeRepository(url)
            loadPlugins()
        }
    }

    fun installAddon(url: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = addonRepository.installAddon(url)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    onResult(true, res.getOrNull()?.manifest?.name)
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Failed to install addon")
                }
            }
        }
    }

    fun removeAddon(manifestUrl: String) {
        viewModelScope.launch {
            addonRepository.removeAddon(manifestUrl)
        }
    }

    fun toggleAddon(manifestUrl: String, enabled: Boolean) {
        viewModelScope.launch {
            addonRepository.toggleAddon(manifestUrl, enabled)
        }
    }

    fun refreshAddon(manifestUrl: String) {
        viewModelScope.launch {
            addonRepository.refreshAddon(manifestUrl)
        }
    }


    /**
     * Updates and installs all plugins from all configured repositories and refreshes all Stremio addons.
     */
    fun updateAllReposAndAddons(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val userUrls = preferencesRepository.repositoryUrls.first()
                val allRepoUrls = userUrls
                    .distinct()
                    .filter { it.isNotBlank() }

                var totalScrapersUpdated = 0
                var reposUpdated = 0

                for (repoUrl in allRepoUrls) {
                    try {
                        val count = pluginService.installAllFromRepo(repoUrl)
                        totalScrapersUpdated += count
                        reposUpdated++
                    } catch (e: Exception) {
                        android.util.Log.w("PluginManagerViewModel", "Failed to update repository: $repoUrl", e)
                    }
                }

                val currentAddons = addonRepository.uiState.value.addons
                var addonsRefreshed = 0
                for (addon in currentAddons) {
                    try {
                        addonRepository.refreshAddon(addon.manifestUrl)
                        addonsRefreshed++
                    } catch (e: Exception) {
                        android.util.Log.w("PluginManagerViewModel", "Failed to refresh addon: ${addon.manifestUrl}", e)
                    }
                }

                loadPlugins()

                val summary = buildString {
                    append("Updated $reposUpdated repos ($totalScrapersUpdated scrapers installed/updated)")
                    if (addonsRefreshed > 0) {
                        append(" and refreshed $addonsRefreshed addons")
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, summary)
                }
            } catch (e: Exception) {
                android.util.Log.e("PluginManagerViewModel", "Failed to update all repos and addons", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, e.message ?: "Failed to update repositories and addons")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Updates a single repository and installs all plugins from its manifest.
     */
    fun updateRepositoryAndPlugins(repoUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val count = pluginService.installAllFromRepo(repoUrl)
                loadPlugins()
                withContext(Dispatchers.Main) {
                    onResult(true, "Updated repository · $count scrapers installed/updated")
                }
            } catch (e: Exception) {
                android.util.Log.e("PluginManagerViewModel", "Failed to update repo $repoUrl", e)
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Failed to update repository")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Refreshes all installed Stremio Addons.
     */
    fun updateAllAddons(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val currentAddons = addonRepository.uiState.value.addons
                var count = 0
                for (addon in currentAddons) {
                    try {
                        addonRepository.refreshAddon(addon.manifestUrl)
                        count++
                    } catch (e: Exception) {
                        android.util.Log.w("PluginManagerViewModel", "Failed to refresh addon ${addon.manifestUrl}", e)
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, "Refreshed $count addons")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Failed to refresh addons")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun fetchRepoDetails(url: String): com.euthopiar.core.model.PluginRepositoryIndex? =
        withContext(Dispatchers.IO) {
            runCatching { pluginService.fetchRepository(url) }.getOrNull()
        }
}
