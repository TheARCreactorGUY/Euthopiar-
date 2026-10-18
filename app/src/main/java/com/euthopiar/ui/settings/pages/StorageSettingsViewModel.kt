package com.euthopiar.ui.settings.pages

import android.app.Application
import android.os.Environment
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.SingletonImageLoader
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.p2p.P2pStreamingEngine
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.preferences.preferencesDataStore
import com.euthopiar.download.DownloadEngine
import com.euthopiar.ui.player.StreamingPlaybackCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.util.Locale

class StorageSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository.getInstance(application)

    private val _cacheSizeFormatted = MutableStateFlow("Calculating...")
    val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _downloadsSizeBytes = MutableStateFlow(0L)
    val downloadsSizeBytes: StateFlow<Long> = _downloadsSizeBytes.asStateFlow()

    private val _configSizeBytes = MutableStateFlow(0L)
    val configSizeBytes: StateFlow<Long> = _configSizeBytes.asStateFlow()

    private val _isCalculating = MutableStateFlow(true)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val _wifiOnlyDownloads = MutableStateFlow(true)
    val wifiOnlyDownloads: StateFlow<Boolean> = _wifiOnlyDownloads.asStateFlow()

    private val _customDownloadDir = MutableStateFlow<String?>(null)
    val customDownloadDir: StateFlow<String?> = _customDownloadDir.asStateFlow()

    val defaultDownloadDir: String = try {
        (application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(application.filesDir, "downloads")).absolutePath
    } catch (_: Exception) { "" }

    init {
        viewModelScope.launch { prefs.wifiOnlyDownloads.collect { _wifiOnlyDownloads.value = it } }
        viewModelScope.launch { prefs.customDownloadDir.collect { _customDownloadDir.value = it } }
        refreshAllStorageTelemetry()
    }

    fun refreshAllStorageTelemetry() {
        calculateCacheSize()
        calculateStorageBreakdown()
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            _isCalculating.value = true
            val size = withContext(Dispatchers.IO) {
                calculateTotalCacheBytes()
            }
            _cacheSizeBytes.value = size
            _cacheSizeFormatted.value = formatSize(size)
            _isCalculating.value = false
        }
    }

    private fun calculateTotalCacheBytes(): Long {
        val context = getApplication<Application>()
        val downloadDir = try {
            DownloadEngine(context).getDownloadDirectory().canonicalPath
        } catch (_: Exception) { "" }
        val excludeSet = if (downloadDir.isNotBlank()) setOf(downloadDir) else emptySet()

        val visited = mutableSetOf<String>()
        var totalCache = 0L

        // 1. Internal application cache: context.cacheDir
        // (holds image_cache, stream_cache, http_cache, nuvio-engine cache)
        totalCache += getDirSizeSafely(context.cacheDir, visited, excludeSet)

        // 2. External application cache: context.externalCacheDirs
        try {
            context.externalCacheDirs?.filterNotNull()?.forEach { extDir ->
                totalCache += getDirSizeSafely(extDir, visited, excludeSet)
            }
        } catch (_: Exception) {}

        return totalCache
    }

    fun calculateStorageBreakdown() {
        viewModelScope.launch {
            val (downloadsBytes, configBytes) = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val downloadDir = try {
                    DownloadEngine(context).getDownloadDirectory()
                } catch (_: Exception) { null }

                val dSize = getDirSizeSafely(downloadDir, mutableSetOf(), emptySet())

                var cSize = 0L
                try {
                    val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
                    cSize += getDirSizeSafely(sharedPrefsDir, mutableSetOf(), emptySet())
                } catch (_: Exception) {}
                try {
                    val datastoreDir = File(context.filesDir, "datastore")
                    cSize += getDirSizeSafely(datastoreDir, mutableSetOf(), emptySet())
                } catch (_: Exception) {}
                try {
                    val stateDir = File(context.noBackupFilesDir, "nuvio-engine/state")
                    cSize += getDirSizeSafely(stateDir, mutableSetOf(), emptySet())
                } catch (_: Exception) {}
                try {
                    val metaFile = File(context.filesDir, "download_metadata.json")
                    if (metaFile.exists()) cSize += metaFile.length()
                } catch (_: Exception) {}

                dSize to cSize
            }
            _downloadsSizeBytes.value = downloadsBytes
            _configSizeBytes.value = configBytes
        }
    }

    suspend fun clearCacheSuspend(): Boolean {
        _isClearing.value = true
        val context = getApplication<Application>()
        val success = withContext(Dispatchers.IO) {
            // 1. Release ExoPlayer SimpleCache lock so chunk files can be deleted
            try {
                StreamingPlaybackCacheManager.release()
            } catch (_: Throwable) {}

            // 2. Evict OkHttp Disk Cache
            try {
                DohDns.sharedBaseClient.cache?.evictAll()
            } catch (_: Throwable) {}

            // 3. Clear Coil Image Cache (disk + memory)
            try {
                val imageLoader = SingletonImageLoader.get(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            } catch (_: Throwable) {}

            // 4. Reclaim P2P Engine Cache
            try {
                P2pStreamingEngine.clearCache()
            } catch (_: Throwable) {}

            // 5. Safely delete files in cache directories, strictly preserving offline downloads
            val downloadDir = try {
                DownloadEngine(context).getDownloadDirectory().canonicalPath
            } catch (_: Exception) { "" }
            val excludeSet = if (downloadDir.isNotBlank()) setOf(downloadDir) else emptySet()

            val visited = mutableSetOf<String>()
            val d1 = deleteDirSafely(context.cacheDir, visited, excludeSet, isRoot = true)
            var d2 = true
            try {
                context.externalCacheDirs?.filterNotNull()?.forEach { extDir ->
                    d2 = deleteDirSafely(extDir, visited, excludeSet, isRoot = true) && d2
                }
            } catch (_: Exception) {}

            // Re-create root cache directories
            try { context.cacheDir?.mkdirs() } catch (_: Exception) {}
            try { context.externalCacheDir?.mkdirs() } catch (_: Exception) {}

            d1 && d2
        }

        // Synchronously re-evaluate remaining cache size before clearing flag drops
        val remainingBytes = withContext(Dispatchers.IO) { calculateTotalCacheBytes() }
        _cacheSizeBytes.value = remainingBytes
        _cacheSizeFormatted.value = formatSize(remainingBytes)
        _isClearing.value = false
        calculateStorageBreakdown()
        return success
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            clearCacheSuspend()
            onComplete()
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { prefs.setWifiOnlyDownloads(enabled) }
    }

    fun setCustomDownloadDir(path: String?) {
        _customDownloadDir.value = path
        viewModelScope.launch {
            prefs.setCustomDownloadDir(path)
            refreshAllStorageTelemetry()
        }
    }

    fun exportConfiguration(uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val allPrefs = context.preferencesDataStore.data.first().asMap()
                val jsonPrefs = JSONObject()
                for ((key, value) in allPrefs) {
                    val keyName = key.name
                    if (isSensitiveKey(keyName)) continue
                    when (value) {
                        is Boolean -> jsonPrefs.put(keyName, value)
                        is Int -> jsonPrefs.put(keyName, value)
                        is Long -> jsonPrefs.put(keyName, value)
                        is Float -> jsonPrefs.put(keyName, value.toDouble())
                        is String -> jsonPrefs.put(keyName, value)
                        is Set<*> -> {
                            val arr = JSONArray()
                            value.forEach { arr.put(it.toString()) }
                            jsonPrefs.put(keyName, arr)
                        }
                    }
                }
                val root = JSONObject().apply {
                    put("app", "Euthopiar")
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("preferences", jsonPrefs)
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter().use { it.write(root.toString(2)) }
                    } ?: throw IllegalStateException("Could not open output stream")
                }
                onResult(true, "Configuration exported successfully")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.message}")
            }
        }
    }

    fun importConfiguration(uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    } ?: throw IllegalStateException("Could not open input stream")
                }
                val root = JSONObject(content)
                if (root.optString("app") != "Euthopiar") {
                    onResult(false, "Invalid configuration file (not a Euthopiar backup)")
                    return@launch
                }
                val jsonPrefs = root.optJSONObject("preferences")
                    ?: throw IllegalArgumentException("Missing preferences object")

                context.preferencesDataStore.edit { prefsMap ->
                    val keys = jsonPrefs.keys()
                    while (keys.hasNext()) {
                        val keyName = keys.next()
                        if (isSensitiveKey(keyName)) continue
                        val value = jsonPrefs.get(keyName)
                        when (value) {
                            is Boolean -> prefsMap[booleanPreferencesKey(keyName)] = value
                            is Int -> prefsMap[intPreferencesKey(keyName)] = value
                            is Long -> prefsMap[longPreferencesKey(keyName)] = value
                            is Double -> prefsMap[floatPreferencesKey(keyName)] = value.toFloat()
                            is String -> prefsMap[stringPreferencesKey(keyName)] = value
                            is JSONArray -> {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) {
                                    set.add(value.getString(i))
                                }
                                prefsMap[stringSetPreferencesKey(keyName)] = set
                            }
                        }
                    }
                }
                onResult(true, "Configuration imported successfully")
            } catch (e: Exception) {
                onResult(false, "Import failed: ${e.message}")
            }
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        val lower = key.lowercase()
        return lower.contains("token") || lower.contains("secret") ||
               lower.contains("api_key") || lower.contains("apikey") ||
               lower.contains("pin") || lower.contains("password")
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes <= 0L -> "0.00 MB"
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024f * 1024f * 1024f))
        }
    }

    /**
     * Traverses a directory tree safely:
     * - Skips symbolic links to avoid escaping or infinite cycles.
     * - Enforces that child files stay within canonical boundary of the root directory.
     * - Skips any paths inside [excludeCanonical] (e.g. offline downloaded media).
     * - Prevents duplicate visits via [visitedCanonical].
     */
    private fun getDirSizeSafely(
        dir: File?,
        visitedCanonical: MutableSet<String>,
        excludeCanonical: Set<String>
    ): Long {
        if (dir == null || !dir.exists()) return 0L
        val canonical = try { dir.canonicalFile } catch (_: Exception) { return 0L }
        val canonicalPath = canonical.absolutePath
        if (!visitedCanonical.add(canonicalPath)) return 0L
        if (excludeCanonical.any { canonicalPath.startsWith(it) }) return 0L

        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            try {
                if (Files.isSymbolicLink(file.toPath())) {
                    continue
                }
                val fileCanonical = file.canonicalFile
                val fileCanonicalPath = fileCanonical.absolutePath
                if (!fileCanonicalPath.startsWith(canonicalPath)) {
                    continue
                }
                if (excludeCanonical.any { fileCanonicalPath.startsWith(it) }) {
                    continue
                }
                if (file.isDirectory) {
                    size += getDirSizeSafely(file, visitedCanonical, excludeCanonical)
                } else {
                    size += file.length()
                }
            } catch (_: Exception) {}
        }
        return size
    }

    /**
     * Recursively and safely deletes directory contents:
     * - Strictly avoids recursing through symbolic links.
     * - Respects [excludeCanonical] to protect user downloads.
     * - Deletes empty child subdirectories (depth-first).
     */
    private fun deleteDirSafely(
        dir: File?,
        visitedCanonical: MutableSet<String>,
        excludeCanonical: Set<String>,
        isRoot: Boolean = true
    ): Boolean {
        if (dir == null || !dir.exists()) return true
        val canonical = try { dir.canonicalFile } catch (_: Exception) { return true }
        val canonicalPath = canonical.absolutePath
        if (!visitedCanonical.add(canonicalPath)) return true
        if (excludeCanonical.any { canonicalPath.startsWith(it) }) return true

        var allDeleted = true
        val files = dir.listFiles() ?: return true
        for (file in files) {
            try {
                if (Files.isSymbolicLink(file.toPath())) {
                    file.delete()
                    continue
                }
                val fileCanonical = file.canonicalFile
                val fileCanonicalPath = fileCanonical.absolutePath
                if (!fileCanonicalPath.startsWith(canonicalPath)) {
                    continue
                }
                if (excludeCanonical.any { fileCanonicalPath.startsWith(it) }) {
                    continue
                }
                if (file.isDirectory) {
                    val childSuccess = deleteDirSafely(file, visitedCanonical, excludeCanonical, isRoot = false)
                    allDeleted = allDeleted && childSuccess
                } else {
                    val deleted = file.delete()
                    if (!deleted) file.deleteOnExit()
                    allDeleted = allDeleted && deleted
                }
            } catch (_: Exception) {
                allDeleted = false
            }
        }
        if (!isRoot && dir.exists()) {
            val dirDeleted = dir.delete()
            if (!dirDeleted) dir.deleteOnExit()
            allDeleted = allDeleted && dirDeleted
        }
        return allDeleted
    }
}
