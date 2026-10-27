package com.euthopiar.app.plugin

import android.content.Context
import android.net.Uri
import com.euthopiar.core.js.JsPluginStorage
import com.euthopiar.core.js.PluginScraper
import com.euthopiar.core.js.toPluginManifest
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.core.model.PluginRepositoryIndex
import com.euthopiar.core.model.UniversalPlugin
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Service managing JavaScript streaming provider plugins, repositories, and local script installations.
 */
class PluginRepositoryService(
    private val context: Context,
    private val registry: PluginRegistry = PluginRegistry.Default,
    private val preferencesRepository: PreferencesRepository = PreferencesRepository.getInstance(context)
) {
    private val storage = JsPluginStorage.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    val installedPluginsFlow: StateFlow<List<PluginManifest>> = storage.scrapersFlow.map { scrapers ->
        scrapers.distinctBy { it.id }.map { it.toPluginManifest() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val installedPublicPluginsFlow: Flow<List<PluginManifest>> = installedPluginsFlow.map { list ->
        list.filterNot { it.isAdultPlugin }
    }

    /**
     * Initializes installed JavaScript plugins from local storage on startup.
     */
    suspend fun initializePlugins() = withContext(Dispatchers.IO) {
        storage.initialize()
    }

    /**
     * Downloads and installs all plugins from a specific repository manifest URL.
     */
    suspend fun installAllFromRepo(repoUrl: String): Int = withContext(Dispatchers.IO) {
        try {
            val res = storage.addRepository(repoUrl)
            if (res.isSuccess) {
                res.getOrThrow().scrapers.size
            } else {
                -1
            }
        } catch (e: Exception) {
            android.util.Log.e("PluginRepositoryService", "Failed to install all from repo $repoUrl", e)
            -1
        }
    }

    /**
     * Checks all configured repositories for plugin updates and automatically installs newer versions.
     */
    suspend fun checkAndAutoUpdatePlugins(installNewFromRepos: Boolean = false): Int = withContext(Dispatchers.IO) {
        try {
            val userRepoUrls = preferencesRepository.repositoryUrls.first()
            val updatedCount = storage.checkAndAutoUpdatePlugins(
                extraRepoUrls = userRepoUrls,
                installNewFromRepos = installNewFromRepos
            )
            updatedCount
        } catch (e: Exception) {
            android.util.Log.w("PluginRepositoryService", "Auto-update check failed: ${e.message}")
            0
        }
    }

    /**
     * Downloads and installs all plugins from all configured repositories.
     */
    suspend fun installAllPluginsFromRepositories(): Int = withContext(Dispatchers.IO) {
        val repoUrls = storage.repositoriesFlow.value.toSet()
        var count = 0
        for (repoUrl in repoUrls) {
            val c = installAllFromRepo(repoUrl)
            if (c > 0) count += c
        }
        count
    }

    /**
     * Retrieves all installed JavaScript plugins.
     */
    suspend fun getInstalledPlugins(): List<PluginManifest> = withContext(Dispatchers.IO) {
        storage.scrapersFlow.value.distinctBy { it.id }.map { it.toPluginManifest() }
    }

    suspend fun getInstalledPublicPlugins(): List<PluginManifest> = withContext(Dispatchers.IO) {
        getInstalledPlugins().filterNot { it.isAdultPlugin }
    }

    /**
     * Fetches a remote plugin repository manifest (e.g. manifest.json).
     */
    suspend fun fetchRepository(url: String): PluginRepositoryIndex = withContext(Dispatchers.IO) {
        val result = storage.fetchRepositoryManifest(url)
        if (result.isSuccess) {
            val repo = result.getOrThrow()
            return@withContext PluginRepositoryIndex(
                name = repo.name,
                description = repo.description ?: "",
                manifestVersion = 1,
                plugins = repo.scrapers.map { it.toPluginManifest(url) }
            )
        }
        getDefaultRepository()
    }

    /**
     * Downloads and installs a provider plugin from a repository manifest.
     */
    suspend fun downloadAndInstallPlugin(manifest: PluginManifest): Boolean = withContext(Dispatchers.IO) {
        try {
            var dlUrl = manifest.downloadUrl
            if (!dlUrl.isNullOrBlank() && !dlUrl.startsWith("http") && manifest.siteUrl.isNotBlank()) {
                val baseUrl = manifest.siteUrl.substringBeforeLast("/") + "/"
                dlUrl = baseUrl + dlUrl
            } else if (dlUrl.isNullOrBlank() && manifest.siteUrl.isNotBlank()) {
                val baseUrl = manifest.siteUrl.substringBeforeLast("/") + "/"
                dlUrl = baseUrl + manifest.id + ".js"
            }
            if (!dlUrl.isNullOrBlank()) {
                val noCacheHeaders = mapOf("Cache-Control" to "no-cache, no-store, must-revalidate", "Pragma" to "no-cache")
                val res = com.euthopiar.core.js.network.httpRequestRaw("GET", dlUrl, noCacheHeaders, "", followRedirects = true)
                if (res.status in 200..299 && res.body.isNotBlank() && com.euthopiar.core.js.isValidJsPluginCode(res.body)) {
                    storage.installScraper(
                        id = manifest.id,
                        name = manifest.name,
                        version = manifest.versionName,
                        repositoryUrl = manifest.siteUrl,
                        description = manifest.description,
                        filename = manifest.downloadUrl ?: "${manifest.id}.js",
                        supportedTypes = manifest.supportedTypes,
                        logo = manifest.iconUrl,
                        code = res.body
                    )
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("PluginRepositoryService", "Error installing plugin ${manifest.id}: ${e.message}", e)
            false
        }
    }

    /**
     * Installs a local JavaScript scraper (.js) selected by the user via file picker.
     */
    suspend fun installFromUri(uri: Uri): Result<PluginManifest> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("Cannot open content stream from URI"))

            val fileName = uri.lastPathSegment ?: "plugin_${System.currentTimeMillis()}.js"
            val id = fileName.substringBeforeLast(".").replace(Regex("""[^a-zA-Z0-9_-]"""), "_").lowercase()
            val name = id.replaceFirstChar { it.uppercase() }

            val scraper = storage.installLocalScript(id, name, content)
            Result.success(scraper.toPluginManifest())
        } catch (e: Exception) {
            android.util.Log.e("PluginRepositoryService", "Failed to install from URI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uninstalls a JavaScript provider scraper by ID.
     */
    suspend fun uninstallPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            storage.deleteScraper(pluginId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Removes a repository, cascade-purging its installed plugins, files, and preferences atomically.
     */
    suspend fun removeRepository(repoUrl: String) = withContext(Dispatchers.IO) {
        try {
            storage.removeRepository(repoUrl)
            preferencesRepository.removeRepositoryUrl(repoUrl)
        } catch (e: Exception) {
            android.util.Log.e("PluginRepositoryService", "Failed to remove repository $repoUrl", e)
        }
    }

    /**
     * Toggles a plugin enabled/disabled state.
     */
    suspend fun setPluginEnabled(manifest: PluginManifest, enabled: Boolean) = withContext(Dispatchers.IO) {
        storage.toggleScraper(manifest.id, enabled)
        preferencesRepository.setPluginDisabled(manifest.name, !enabled)
    }

    /**
     * Lazily checks if a plugin is registered in [PluginRegistry].
     */
    fun ensurePluginLoaded(pluginId: String): UniversalPlugin? {
        return registry.getPlugin(pluginId)
    }

    fun getDefaultRepository(): PluginRepositoryIndex {
        return PluginRepositoryIndex(
            name = "Community Plugin Repository",
            description = "Nuvio-style JavaScript Provider Repository",
            manifestVersion = 1,
            plugins = emptyList()
        )
    }

    companion object {
        const val DEFAULT_REPO_URL: String = "https://raw.githubusercontent.com/D3adlyRocket/All-in-One-Nuvio/refs/heads/main/manifest.json"
        const val SECONDARY_REPO_URL: String = "https://raw.githubusercontent.com/yoruix/nuvio-providers/refs/heads/main/manifest.json"
        const val TEST_EUPS_REPO_URL: String = DEFAULT_REPO_URL
        val DEFAULT_REPOS: Set<String> = emptySet()

        @Volatile
        private var instance: PluginRepositoryService? = null

        fun getInstance(context: Context): PluginRepositoryService {
            return instance ?: synchronized(this) {
                instance ?: PluginRepositoryService(context.applicationContext).also { instance = it }
            }
        }
    }
}
