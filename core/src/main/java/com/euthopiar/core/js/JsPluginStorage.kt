package com.euthopiar.core.js

import android.content.Context
import com.euthopiar.core.js.network.httpRequestRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsPluginStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mutex = Mutex()

    private val pluginsRootDir: File
        get() = File(context.filesDir, "plugins").apply { if (!exists()) mkdirs() }

    private val legacyPluginsDir: File
        get() = File(context.filesDir, "js_plugins")

    private val scrapersMetadataFile: File
        get() = File(pluginsRootDir, "scrapers.json")

    private val reposFile: File
        get() = File(pluginsRootDir, "repositories.json")

    fun getRepoDir(repoId: String): File {
        return File(pluginsRootDir, repoId).apply { if (!exists()) mkdirs() }
    }

    fun getPluginScriptFile(repoId: String, pluginId: String): File {
        return File(getRepoDir(repoId), "$pluginId.js")
    }

    private val _scrapersFlow = MutableStateFlow<List<PluginScraper>>(emptyList())
    val scrapersFlow: StateFlow<List<PluginScraper>> = _scrapersFlow.asStateFlow()

    private val _repositoriesFlow = MutableStateFlow<List<String>>(emptyList())
    val repositoriesFlow: StateFlow<List<String>> = _repositoriesFlow.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        // Migration from legacy directory if needed
        val legacyMeta = File(legacyPluginsDir, "scrapers.json")
        if (!scrapersMetadataFile.exists() && legacyMeta.exists()) {
            try {
                scrapersMetadataFile.writeText(legacyMeta.readText())
            } catch (_: Exception) {}
        }
        val legacyRepos = File(legacyPluginsDir, "repositories.json")
        if (!reposFile.exists() && legacyRepos.exists()) {
            try {
                reposFile.writeText(legacyRepos.readText())
            } catch (_: Exception) {}
        }

        val scrapers = if (scrapersMetadataFile.exists()) {
            runCatching {
                json.decodeFromString<List<PluginScraper>>(scrapersMetadataFile.readText())
            }.getOrElse { emptyList() }
        } else emptyList()

        val populated = scrapers.map { scraper ->
            val rId = scraper.canonicalRepoId
            val namespacedFile = getPluginScriptFile(rId, scraper.id)
            val legacyFile = File(legacyPluginsDir, "${scraper.id}.js")
            val code = when {
                namespacedFile.exists() -> namespacedFile.readText()
                legacyFile.exists() -> {
                    val content = legacyFile.readText()
                    try { namespacedFile.writeText(content) } catch (_: Exception) {}
                    content
                }
                else -> scraper.code
            }
            val supported = if (scraper.supportedTypes.isEmpty()) listOf("movie", "tv") else scraper.supportedTypes
            scraper.copy(code = code, supportedTypes = supported, repoId = rId)
        }
        _scrapersFlow.value = populated

        for (scraper in populated) {
            com.euthopiar.core.registry.PluginRegistry.Default.registerPlugin(JsUniversalPlugin(scraper))
            com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(scraper.name, scraper.enabled)
        }

        val repos = if (reposFile.exists()) {
            runCatching {
                json.decodeFromString<List<String>>(reposFile.readText())
            }.getOrElse { emptyList() }
        } else emptyList()
        _repositoriesFlow.value = repos
    }

    suspend fun fetchRepositoryManifest(manifestUrl: String): Result<JsRepoManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val response = httpRequestRaw("GET", manifestUrl, emptyMap(), "", followRedirects = true)
            if (response.status !in 200..299 || response.body.isBlank()) {
                error("Failed to load manifest: HTTP ${response.status}")
            }
            json.decodeFromString<JsRepoManifest>(response.body)
        }
    }

    suspend fun addRepository(manifestUrl: String): Result<JsRepoManifest> = withContext(Dispatchers.IO) {
        val manifestResult = fetchRepositoryManifest(manifestUrl)
        if (manifestResult.isFailure) return@withContext manifestResult

        val manifest = manifestResult.getOrThrow()
        val baseUrl = manifestUrl.substringBeforeLast("/") + "/"
        val repoId = computeCanonicalRepoId(manifestUrl)

        val downloadedScrapers = coroutineScope {
            manifest.scrapers.map { scraperMeta ->
                async(Dispatchers.IO) {
                    val scriptUrl = if (scraperMeta.filename.startsWith("http")) {
                        scraperMeta.filename
                    } else {
                        baseUrl + scraperMeta.filename
                    }

                    try {
                        val scriptResponse = httpRequestRaw("GET", scriptUrl, emptyMap(), "", followRedirects = true)
                        if (scriptResponse.status in 200..299 && scriptResponse.body.isNotBlank()) {
                            Pair(scraperMeta, scriptResponse.body)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        mutex.withLock {
            val currentRepos = _repositoriesFlow.value.toMutableList()
            if (!currentRepos.contains(manifestUrl)) {
                currentRepos.add(manifestUrl)
                reposFile.writeText(json.encodeToString(currentRepos))
                _repositoriesFlow.value = currentRepos
            }

            // Install / update each scraper in the manifest under namespaced repo folder
            val updatedScrapers = _scrapersFlow.value.toMutableList()

            for ((scraperMeta, code) in downloadedScrapers) {
                val codeFile = getPluginScriptFile(repoId, scraperMeta.id)
                codeFile.writeText(code)

                val existingIndex = updatedScrapers.indexOfFirst { it.id == scraperMeta.id }
                val item = PluginScraper(
                    id = scraperMeta.id,
                    repositoryUrl = manifestUrl,
                    repoId = repoId,
                    name = scraperMeta.name,
                    description = scraperMeta.description ?: "",
                    version = scraperMeta.version,
                    filename = scraperMeta.filename,
                    supportedTypes = scraperMeta.supportedTypes,
                    enabled = scraperMeta.enabled,
                    hasSettings = scraperMeta.hasSettings,
                    logo = scraperMeta.logo,
                    formats = scraperMeta.formats,
                    code = code
                )

                if (existingIndex >= 0) {
                    updatedScrapers[existingIndex] = item
                } else {
                    updatedScrapers.add(item)
                }
            }

            scrapersMetadataFile.writeText(json.encodeToString(updatedScrapers))
            _scrapersFlow.value = updatedScrapers

            for (scraper in updatedScrapers) {
                com.euthopiar.core.registry.PluginRegistry.Default.registerPlugin(JsUniversalPlugin(scraper))
                com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(scraper.name, scraper.enabled)
            }
        }
        manifestResult
    }

    suspend fun removeRepository(manifestUrl: String) = withContext(Dispatchers.IO) {
        val targetRepoId = if (manifestUrl.startsWith("repo_")) manifestUrl else computeCanonicalRepoId(manifestUrl)
        PluginRuntime.cancelJobsForRepo(targetRepoId)

        mutex.withLock {
            val currentRepos = _repositoriesFlow.value.filter {
                computeCanonicalRepoId(it) != targetRepoId && it.trim() != manifestUrl.trim()
            }
            reposFile.writeText(json.encodeToString(currentRepos))
            _repositoriesFlow.value = currentRepos

            val currentScrapers = _scrapersFlow.value.filter { scraper ->
                val matches = scraper.canonicalRepoId == targetRepoId ||
                        scraper.repoId == targetRepoId ||
                        computeCanonicalRepoId(scraper.repositoryUrl) == targetRepoId ||
                        scraper.repositoryUrl.trim().equals(manifestUrl.trim(), ignoreCase = true)
                if (matches) {
                    com.euthopiar.core.registry.PluginRegistry.Default.unregisterPlugin(scraper.name)
                    false
                } else true
            }
            scrapersMetadataFile.writeText(json.encodeToString(currentScrapers))
            _scrapersFlow.value = currentScrapers

            // Cleanly delete the partitioned directory for this repo
            try {
                val repoDir = File(pluginsRootDir, targetRepoId)
                if (repoDir.exists()) {
                    repoDir.deleteRecursively()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun installScraper(
        id: String,
        name: String,
        version: String,
        repositoryUrl: String,
        description: String,
        filename: String,
        supportedTypes: List<String>,
        logo: String?,
        code: String
    ): PluginScraper = withContext(Dispatchers.IO) {
        val repoId = if (repositoryUrl.isNotBlank()) computeCanonicalRepoId(repositoryUrl) else "local"
        mutex.withLock {
            val codeFile = getPluginScriptFile(repoId, id)
            codeFile.writeText(code)

            val item = PluginScraper(
                id = id,
                repositoryUrl = repositoryUrl,
                repoId = repoId,
                name = name,
                description = description,
                version = version,
                filename = filename,
                supportedTypes = supportedTypes,
                enabled = true,
                hasSettings = false,
                logo = logo,
                code = code
            )

            val updated = _scrapersFlow.value.filter { it.id != id } + item
            scrapersMetadataFile.writeText(json.encodeToString(updated))
            _scrapersFlow.value = updated
            com.euthopiar.core.registry.PluginRegistry.Default.registerPlugin(JsUniversalPlugin(item))
            com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(item.name, true)
            item
        }
    }

    suspend fun installLocalScript(id: String, name: String, code: String): PluginScraper = withContext(Dispatchers.IO) {
        val repoId = "local"
        mutex.withLock {
            val codeFile = getPluginScriptFile(repoId, id)
            codeFile.writeText(code)

            val item = PluginScraper(
                id = id,
                repositoryUrl = "local",
                repoId = repoId,
                name = name,
                description = "Local provider script",
                version = "1.0.0",
                filename = "$id.js",
                supportedTypes = listOf("movie", "tv"),
                enabled = true,
                hasSettings = false,
                logo = null,
                code = code
            )

            val updated = _scrapersFlow.value.filter { it.id != id } + item
            scrapersMetadataFile.writeText(json.encodeToString(updated))
            _scrapersFlow.value = updated
            com.euthopiar.core.registry.PluginRegistry.Default.registerPlugin(JsUniversalPlugin(item))
            com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(item.name, true)
            item
        }
    }

    suspend fun toggleScraper(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _scrapersFlow.value.map {
                if (it.id == id) {
                    com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(it.name, enabled)
                    it.copy(enabled = enabled)
                } else it
            }
            scrapersMetadataFile.writeText(json.encodeToString(updated))
            _scrapersFlow.value = updated
        }
    }

    suspend fun deleteScraper(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = _scrapersFlow.value.find { it.id == id }
            if (target != null) {
                com.euthopiar.core.registry.PluginRegistry.Default.unregisterPlugin(target.name)
                val rId = target.canonicalRepoId
                getPluginScriptFile(rId, id).delete()
            }
            File(legacyPluginsDir, "$id.js").delete()
            val updated = _scrapersFlow.value.filter { it.id != id }
            scrapersMetadataFile.writeText(json.encodeToString(updated))
            _scrapersFlow.value = updated
        }
    }

    fun getEnabledScrapers(): List<PluginScraper> {
        return _scrapersFlow.value.filter { it.enabled && it.code.isNotBlank() }
    }

    fun getEnabledScrapersForType(type: String): List<PluginScraper> {
        val normType = normalizePluginType(type)
        return getEnabledScrapers().filter { scraper ->
            scraper.supportsType(normType)
        }
    }

    companion object {
        @Volatile
        private var instance: JsPluginStorage? = null

        fun getInstance(context: Context): JsPluginStorage {
            return instance ?: synchronized(this) {
                instance ?: JsPluginStorage(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): JsPluginStorage? = instance
    }
}
