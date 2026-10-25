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
        val cleanPluginId = pluginId.substringAfter(":")
        val namespacedFile = File(getRepoDir(repoId), "$cleanPluginId.js")
        if (namespacedFile.exists()) return namespacedFile
        val legacyFile = File(legacyPluginsDir, "$cleanPluginId.js")
        if (legacyFile.exists()) return legacyFile
        return namespacedFile
    }

    fun getStorageKey(repoId: String, scraperId: String): String {
        val cleanRepo = repoId.ifBlank { "local" }
        val cleanId = scraperId.substringAfter(":")
        return "$cleanRepo:$cleanId"
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
            val cleanId = scraper.id.substringAfter(":")
            val namespacedId = if (scraper.id.contains(":")) scraper.id else getStorageKey(rId, cleanId)
            val namespacedFile = getPluginScriptFile(rId, cleanId)
            val legacyFile = File(legacyPluginsDir, "$cleanId.js")
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
            scraper.copy(id = namespacedId, code = code, supportedTypes = supported, repoId = rId)
        }.distinctBy { it.id }

        if (populated.size != scrapers.size) {
            try { scrapersMetadataFile.writeText(json.encodeToString(populated)) } catch (_: Exception) {}
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

        val distinctScrapers = manifest.scrapers.distinctBy { it.id.lowercase().trim() }
        val downloadedScrapers = coroutineScope {
            distinctScrapers.map { scraperMeta ->
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
                val cleanScraperId = scraperMeta.id.substringAfter(":")
                val namespacedId = getStorageKey(repoId, cleanScraperId)
                val codeFile = getPluginScriptFile(repoId, cleanScraperId)
                codeFile.writeText(code)

                val existingIndex = updatedScrapers.indexOfFirst {
                    it.id == namespacedId || (it.canonicalRepoId == repoId && (it.id == cleanScraperId || it.id.substringAfter(":") == cleanScraperId))
                }
                val item = PluginScraper(
                    id = namespacedId,
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

            val distinctUpdated = updatedScrapers.distinctBy { it.id }
            scrapersMetadataFile.writeText(json.encodeToString(distinctUpdated))
            _scrapersFlow.value = distinctUpdated

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
            val cleanId = id.substringAfter(":")
            val namespacedId = getStorageKey(repoId, cleanId)
            val codeFile = getPluginScriptFile(repoId, cleanId)
            codeFile.writeText(code)

            val item = PluginScraper(
                id = namespacedId,
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

            val updated = (_scrapersFlow.value.filterNot {
                it.id == namespacedId || (it.canonicalRepoId == repoId && (it.id == id || it.id.substringAfter(":") == cleanId))
            } + item).distinctBy { it.id }
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
            val cleanId = id.substringAfter(":")
            val canonicalKey = getStorageKey(repoId, cleanId)
            val codeFile = getPluginScriptFile(repoId, cleanId)
            codeFile.writeText(code)

            val item = PluginScraper(
                id = canonicalKey,
                repositoryUrl = "local",
                repoId = repoId,
                name = name,
                description = "Local provider script",
                version = "1.0.0",
                filename = "$cleanId.js",
                supportedTypes = listOf("movie", "tv"),
                enabled = true,
                hasSettings = false,
                logo = null,
                code = code
            )

            val updated = (_scrapersFlow.value.filterNot {
                it.id == canonicalKey || (it.canonicalRepoId == repoId && (it.id == id || it.id.substringAfter(":") == cleanId))
            } + item).distinctBy { it.id }
            scrapersMetadataFile.writeText(json.encodeToString(updated))
            _scrapersFlow.value = updated
            com.euthopiar.core.registry.PluginRegistry.Default.registerPlugin(JsUniversalPlugin(item))
            com.euthopiar.core.registry.PluginRegistry.Default.setPluginEnabled(item.name, true)
            item
        }
    }

    suspend fun toggleScraper(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val isNamespaced = id.contains(":")
            val updated = _scrapersFlow.value.map {
                val matches = if (isNamespaced) {
                    it.id == id
                } else {
                    it.id == id || it.id.substringAfter(":") == id
                }
                if (matches) {
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
            val isNamespaced = id.contains(":")
            val target = _scrapersFlow.value.find {
                if (isNamespaced) {
                    it.id == id
                } else {
                    it.id == id || it.id.substringAfter(":") == id
                }
            }
            val targetId = target?.id ?: id
            val cleanId = targetId.substringAfter(":")
            if (target != null) {
                com.euthopiar.core.registry.PluginRegistry.Default.unregisterPlugin(target.name)
                val rId = target.canonicalRepoId
                getPluginScriptFile(rId, target.id).delete()
                getPluginScriptFile(rId, cleanId).delete()
            }
            File(legacyPluginsDir, "$targetId.js").delete()
            File(legacyPluginsDir, "$cleanId.js").delete()
            val updated = _scrapersFlow.value.filterNot {
                it.id == targetId || it.id == id
            }
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

        fun getStorageKey(repoId: String, scraperId: String): String {
            val cleanRepo = repoId.ifBlank { "local" }
            val cleanId = scraperId.substringAfter(":")
            return "$cleanRepo:$cleanId"
        }
    }
}
