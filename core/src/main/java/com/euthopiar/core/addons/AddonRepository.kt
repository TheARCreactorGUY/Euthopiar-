package com.euthopiar.core.addons

import android.content.Context
import com.euthopiar.core.js.network.httpRequestRaw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AddonRepository private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mutex = Mutex()

    private val addonsDir: File
        get() = File(context.filesDir, "addons").apply { if (!exists()) mkdirs() }

    private val addonsFile: File
        get() = File(addonsDir, "addons.json")

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    private var initialized = false

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        mutex.withLock {
            loadFromDisk()
            initialized = true
        }
    }

    private fun loadFromDisk() {
        val fileExists = addonsFile.exists()
        val saved = if (fileExists) {
            runCatching {
                json.decodeFromString<List<ManagedAddon>>(addonsFile.readText())
            }.getOrNull()
        } else null

        val initial = if (saved == null) {
            listOf(
                ManagedAddon(
                    manifestUrl = DEFAULT_CATALOG_MANIFEST_URL,
                    userSetName = "Nuvio Catalog",
                    enabled = true
                ),
                ManagedAddon(
                    manifestUrl = DEFAULT_OPENSUBTITLES_MANIFEST_URL,
                    userSetName = "OpenSubtitles v3",
                    enabled = true
                )
            )
        } else saved

        _uiState.value = AddonsUiState(addons = initial)
        if (!fileExists) {
            persist()
        }

        // Refresh manifests for any enabled addons in background
        initial.filter { it.enabled }.forEach { addon ->
            scope.launch {
                refreshAddonInternal(addon.manifestUrl)
            }
        }
    }

    private fun persist() {
        runCatching {
            addonsFile.writeText(json.encodeToString(_uiState.value.addons))
        }
    }

    suspend fun installAddon(rawUrl: String): Result<ManagedAddon> = withContext(Dispatchers.IO) {
        val manifestUrl = normalizeAddonUrl(rawUrl)

        runCatching {
            val response = httpRequestRaw("GET", manifestUrl, emptyMap(), "", followRedirects = true)
            if (response.status !in 200..299 || response.body.isBlank()) {
                error("Failed to fetch addon manifest: HTTP ${response.status}")
            }

            val manifest = AddonManifestParser.parse(manifestUrl, response.body)
            val managed = ManagedAddon(
                manifestUrl = manifestUrl,
                manifest = manifest,
                userSetName = manifest.name,
                enabled = true
            )

            mutex.withLock {
                _uiState.update { state ->
                    val existingIndex = state.addons.indexOfFirst {
                        (manifest.id.isNotBlank() && it.manifest?.id == manifest.id) ||
                        it.manifestUrl == manifestUrl
                    }
                    if (existingIndex >= 0) {
                        val updatedList = state.addons.toMutableList()
                        val old = updatedList[existingIndex]
                        val preservedUserSetName = if (!old.userSetName.isNullOrBlank() && old.userSetName != old.manifest?.name) {
                            old.userSetName
                        } else {
                            manifest.name
                        }
                        updatedList[existingIndex] = managed.copy(
                            userSetName = preservedUserSetName,
                            enabled = old.enabled
                        )
                        state.copy(addons = updatedList)
                    } else {
                        state.copy(addons = state.addons + managed)
                    }
                }
                persist()
            }
            managed
        }
    }

    suspend fun removeAddon(manifestUrl: String) = withContext(Dispatchers.IO) {
        val norm = normalizeAddonUrl(manifestUrl)
        mutex.withLock {
            _uiState.update { state ->
                state.copy(addons = state.addons.filterNot { it.manifestUrl == manifestUrl || it.manifestUrl == norm })
            }
            persist()
        }
    }

    suspend fun toggleAddon(manifestUrl: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val norm = normalizeAddonUrl(manifestUrl)
        mutex.withLock {
            _uiState.update { state ->
                state.copy(
                    addons = state.addons.map {
                        if (it.manifestUrl == manifestUrl || it.manifestUrl == norm) it.copy(enabled = enabled) else it
                    }
                )
            }
            persist()
        }
    }

    suspend fun restoreAddons(newAddons: List<ManagedAddon>) = withContext(Dispatchers.IO) {
        if (newAddons.isEmpty()) return@withContext
        mutex.withLock {
            _uiState.update { state ->
                state.copy(addons = newAddons)
            }
            persist()
        }
        newAddons.filter { it.enabled }.forEach { addon ->
            scope.launch {
                refreshAddonInternal(addon.manifestUrl)
            }
        }
    }

    suspend fun refreshAddon(manifestUrl: String) = withContext(Dispatchers.IO) {
        refreshAddonInternal(manifestUrl)
    }

    private suspend fun refreshAddonInternal(manifestUrl: String) {
        runCatching {
            val response = httpRequestRaw("GET", manifestUrl, emptyMap(), "", followRedirects = true)
            if (response.status in 200..299 && response.body.isNotBlank()) {
                val manifest = AddonManifestParser.parse(manifestUrl, response.body)
                mutex.withLock {
                    _uiState.update { state ->
                        state.copy(
                            addons = state.addons.map {
                                if (it.manifestUrl == manifestUrl) {
                                    it.copy(manifest = manifest, isRefreshing = false, errorMessage = null)
                                } else it
                            }
                        )
                    }
                    persist()
                }
            }
        }.onFailure { err ->
            mutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        addons = state.addons.map {
                            if (it.manifestUrl == manifestUrl) {
                                it.copy(isRefreshing = false, errorMessage = err.message)
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun getEnabledAddonsForType(type: String, id: String = ""): List<ManagedAddon> {
        return _uiState.value.addons.filter { addon ->
            addon.enabled && addon.manifest?.supportsStream(type, id) == true
        }
    }

    fun getEnabledAddonsForResource(resource: String, type: String, id: String = ""): List<ManagedAddon> {
        val canonicalType = type.lowercase()
        return _uiState.value.addons.filter { addon ->
            addon.enabled && addon.manifest?.supportsResource(resource, canonicalType, id) == true
        }
    }

    companion object {
        const val DEFAULT_CATALOG_MANIFEST_URL = "https://catalog.nuvio.tv/manifest.json"
        const val DEFAULT_OPENSUBTITLES_MANIFEST_URL = "https://opensubtitles-v3.strem.io/manifest.json"

        fun normalizeAddonUrl(url: String): String {
            var clean = url.trim()
            if (clean.startsWith("stremio://", ignoreCase = true)) {
                clean = "https://" + clean.substring("stremio://".length)
            } else if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
                clean = "https://$clean"
            }
            val query = clean.substringAfter("?", "")
            var path = clean.substringBefore("?").trimEnd('/')
            if (!path.endsWith("/manifest.json") && path != "manifest.json") {
                path = "$path/manifest.json"
            }
            return if (query.isNotBlank()) "$path?$query" else path
        }

        @Volatile
        private var instance: AddonRepository? = null

        fun getInstance(context: Context): AddonRepository {
            return instance ?: synchronized(this) {
                instance ?: AddonRepository(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): AddonRepository? = instance
    }
}
