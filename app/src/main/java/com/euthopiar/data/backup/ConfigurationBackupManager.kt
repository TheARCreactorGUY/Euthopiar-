package com.euthopiar.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import com.euthopiar.app.plugin.PluginRepositoryService
import com.euthopiar.core.addons.AddonRepository
import com.euthopiar.core.addons.ManagedAddon
import com.euthopiar.data.preferences.preferencesDataStore
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Inspection summary returned before applying a configuration restore.
 */
data class BackupSummary(
    val exportedAt: Long,
    val preferencesCount: Int,
    val profilesCount: Int,
    val repositoriesCount: Int,
    val addonsCount: Int
)

/**
 * Result of validating and inspecting a configuration file.
 */
data class BackupInspectionResult(
    val isValid: Boolean,
    val summary: BackupSummary? = null,
    val errorMessage: String? = null,
    val rootJson: JSONObject? = null
)

/**
 * Enterprise-grade Configuration Backup & Restore Manager for Euthopiar.
 *
 * Architectural Guarantees:
 * 1. Type-Safe Preferences Coercion: Enforces strict data types matching PreferencesRepository keys
 *    to prevent DataStore ClassCastException.
 * 2. Complete State Coverage: Backs up Preferences, User Profiles (ProfileManager),
 *    Stremio Addons (AddonRepository), and Plugin Repositories.
 * 3. Sanitized Security: Omits raw tokens/passwords/API keys while sanitizing orphan states
 *    (disabling debrid/scrobble if underlying credentials are absent; stripping plaintext PINs).
 * 4. Stream Safety: Mode "wt" prevents trailing garbage overwrite corruption.
 * 5. Memory Boundedness: Enforces strict 5MB file cap to prevent OOM vulnerabilities.
 * 6. Module Synchronization: Coordinates DataStore, SharedPreferences fallback, ProfileManager,
 *    AddonRepository, and triggers automatic background repository plugin sync.
 */
class ConfigurationBackupManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    companion object {
        const val BACKUP_VERSION = 2
        const val MAX_BACKUP_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB ceiling

        private enum class KeyType { STRING, BOOLEAN, INT, STRING_SET }

        private val PREFERENCES_SCHEMA: Map<String, KeyType> = mapOf(
            // String preferences
            "default_quality" to KeyType.STRING,
            "subtitle_color" to KeyType.STRING,
            "custom_download_dir" to KeyType.STRING,
            "active_provider" to KeyType.STRING,
            "card_scale_mode" to KeyType.STRING,
            "navbar_style" to KeyType.STRING,
            "recent_searches_csv" to KeyType.STRING,
            "doh_provider" to KeyType.STRING,
            "favorite_media_ids" to KeyType.STRING,
            "top_10_style" to KeyType.STRING,
            "section_label_style" to KeyType.STRING,
            "long_press_style" to KeyType.STRING,
            "media_reactions" to KeyType.STRING,
            "active_debrid_provider" to KeyType.STRING,
            "trakt_username" to KeyType.STRING,

            // Boolean preferences
            "auto_play_next_episode" to KeyType.BOOLEAN,
            "subtitle_background_enabled" to KeyType.BOOLEAN,
            "subtitle_outline" to KeyType.BOOLEAN,
            "wifi_only_downloads" to KeyType.BOOLEAN,
            "first_download_alert_dismissed" to KeyType.BOOLEAN,
            "extra_content_enabled" to KeyType.BOOLEAN,
            "first_run_completed" to KeyType.BOOLEAN,
            "reduce_motion" to KeyType.BOOLEAN,
            "library_offline_only" to KeyType.BOOLEAN,
            "trakt_scrobble_enabled" to KeyType.BOOLEAN,
            "simkl_scrobble_enabled" to KeyType.BOOLEAN,
            "debrid_enabled" to KeyType.BOOLEAN,
            "auto_skip_intro" to KeyType.BOOLEAN,
            "external_subtitles_enabled" to KeyType.BOOLEAN,
            "hardware_acceleration_enabled" to KeyType.BOOLEAN,
            "amoled_mode" to KeyType.BOOLEAN,
            "smart_failover_enabled" to KeyType.BOOLEAN,
            "audio_passthrough_enabled" to KeyType.BOOLEAN,

            // Int preferences
            "seek_interval_seconds" to KeyType.INT,
            "subtitle_size_sp" to KeyType.INT,
            "library_last_tab" to KeyType.INT,
            "skip_intro_duration_seconds" to KeyType.INT,
            "skip_outro_duration_seconds" to KeyType.INT,
            "countdown_next_episode_seconds" to KeyType.INT,
            "playback_buffer_size_mb" to KeyType.INT,

            // StringSet preferences
            "repository_urls" to KeyType.STRING_SET,
            "disabled_plugins" to KeyType.STRING_SET
        )

        fun isSensitiveKey(key: String): Boolean {
            val lower = key.lowercase()
            return lower.contains("token") || lower.contains("secret") ||
                    lower.contains("api_key") || lower.contains("apikey") ||
                    lower.contains("pin") || lower.contains("password")
        }
    }

    /**
     * Serializes complete application configuration to a sanitized JSON structure and writes to [uri].
     */
    suspend fun exportConfiguration(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Gather & sanitize DataStore preferences
            val allPrefs = context.preferencesDataStore.data.first().asMap()
            val jsonPrefs = JSONObject()

            for ((key, value) in allPrefs) {
                val keyName = key.name
                if (isSensitiveKey(keyName)) continue

                // Check orphan credential states
                when (keyName) {
                    "debrid_enabled" -> {
                        // Debrid keys are excluded, so default debrid_enabled to false in export
                        jsonPrefs.put(keyName, false)
                        continue
                    }
                    "trakt_username" -> {
                        // Trakt tokens are excluded, do not export orphan username
                        continue
                    }
                    "trakt_scrobble_enabled" -> {
                        jsonPrefs.put(keyName, false)
                        continue
                    }
                }

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

            // 2. Gather User Profiles
            val profileManager = ProfileManager.getInstance(context)
            val rawProfiles = profileManager.profiles.value
            val sanitizedProfiles = rawProfiles.map { profile ->
                // Plaintext PIN is purged, cryptographic pinHash is securely retained
                profile.copy(pin = null)
            }
            val profilesJsonString = json.encodeToString(sanitizedProfiles)
            val jsonProfilesArray = JSONArray(profilesJsonString)

            // 3. Gather Repositories
            val rawRepos = allPrefs[stringSetPreferencesKey("repository_urls")] as? Set<*>
            val repoUrls: List<String> = rawRepos?.map { it.toString() } ?: emptyList()
            val jsonReposArray = JSONArray().apply {
                repoUrls.forEach { put(it) }
            }

            // 4. Gather Stremio Addons
            val addonRepository = AddonRepository.getInstance(context)
            val currentAddons = addonRepository.uiState.value.addons
            val addonsJsonString = json.encodeToString(currentAddons)
            val jsonAddonsArray = JSONArray(addonsJsonString)

            // 5. Construct Root Backup Envelope
            val root = JSONObject().apply {
                put("app", "Euthopiar")
                put("version", BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("preferences", jsonPrefs)
                put("profiles", jsonProfilesArray)
                put("activeProfileId", profileManager.activeProfileId.value)
                put("repositories", jsonReposArray)
                put("addons", jsonAddonsArray)
            }

            // 6. Write with "wt" mode (Write + Truncate) to prevent trailing garbage
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Unable to open output stream for export")

            outputStream.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            }

            Result.success("Configuration exported successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspects a selected backup file without modifying app state.
     * Enforces file size boundaries and validates the structural schema.
     */
    suspend fun inspectBackup(uri: Uri): BackupInspectionResult = withContext(Dispatchers.IO) {
        try {
            // Guard against OOM attacks via descriptor size check
            val declaredSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (_: Exception) { -1L }

            if (declaredSize > MAX_BACKUP_SIZE_BYTES) {
                return@withContext BackupInspectionResult(
                    isValid = false,
                    errorMessage = "Backup file exceeds maximum allowed size (5 MB)"
                )
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext BackupInspectionResult(
                    isValid = false,
                    errorMessage = "Unable to open input stream from file"
                )

            val rawContent = inputStream.use { stream ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_BACKUP_SIZE_BYTES) {
                        return@withContext BackupInspectionResult(
                            isValid = false,
                            errorMessage = "Backup file exceeds maximum allowed size (5 MB)"
                        )
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }

            if (rawContent.isBlank()) {
                return@withContext BackupInspectionResult(
                    isValid = false,
                    errorMessage = "Configuration file is empty"
                )
            }

            val root = try {
                JSONObject(rawContent)
            } catch (e: Exception) {
                return@withContext BackupInspectionResult(
                    isValid = false,
                    errorMessage = "Invalid JSON syntax: ${e.localizedMessage}"
                )
            }

            if (root.optString("app") != "Euthopiar") {
                return@withContext BackupInspectionResult(
                    isValid = false,
                    errorMessage = "Selected file is not a valid Euthopiar configuration backup"
                )
            }

            val jsonPrefs = root.optJSONObject("preferences")
            val profilesArr = root.optJSONArray("profiles")
            val reposArr = root.optJSONArray("repositories")
            val addonsArr = root.optJSONArray("addons")
            val exportedAt = root.optLong("exportedAt", 0L)

            val summary = BackupSummary(
                exportedAt = exportedAt,
                preferencesCount = jsonPrefs?.length() ?: 0,
                profilesCount = profilesArr?.length() ?: 0,
                repositoriesCount = reposArr?.length() ?: 0,
                addonsCount = addonsArr?.length() ?: 0
            )

            BackupInspectionResult(
                isValid = true,
                summary = summary,
                rootJson = root
            )
        } catch (e: Exception) {
            BackupInspectionResult(
                isValid = false,
                errorMessage = e.message ?: "Failed to inspect configuration file"
            )
        }
    }

    /**
     * Applies the validated backup payload:
     * - Coerces DataStore types safely matching [PREFERENCES_SCHEMA].
     * - Restores User Profiles to ProfileManager.
     * - Restores Stremio Addons to AddonRepository.
     * - Syncs SharedPreferences fallback.
     * - Triggers background repository plugin installation.
     */
    suspend fun applyBackup(root: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonPrefs = root.optJSONObject("preferences")
            val profilesArr = root.optJSONArray("profiles")
            val activeProfileId = root.optString("activeProfileId").takeIf { it.isNotBlank() }
            val reposArr = root.optJSONArray("repositories")
            val addonsArr = root.optJSONArray("addons")

            // 1. Restore DataStore Preferences with Type Coercion
            if (jsonPrefs != null) {
                context.preferencesDataStore.edit { prefsMap ->
                    val keys = jsonPrefs.keys()
                    while (keys.hasNext()) {
                        val keyName = keys.next()
                        if (isSensitiveKey(keyName)) continue

                        val rawValue = jsonPrefs.get(keyName)
                        val schemaType = PREFERENCES_SCHEMA[keyName]

                        if (schemaType != null) {
                            when (schemaType) {
                                KeyType.BOOLEAN -> {
                                    val coerced = when (rawValue) {
                                        is Boolean -> rawValue
                                        is Number -> rawValue.toInt() != 0
                                        is String -> rawValue.equals("true", ignoreCase = true)
                                        else -> false
                                    }
                                    prefsMap[booleanPreferencesKey(keyName)] = coerced
                                }
                                KeyType.INT -> {
                                    val coerced = when (rawValue) {
                                        is Number -> rawValue.toInt()
                                        is String -> rawValue.toIntOrNull() ?: 0
                                        else -> 0
                                    }
                                    prefsMap[intPreferencesKey(keyName)] = coerced
                                }
                                KeyType.STRING -> {
                                    prefsMap[stringPreferencesKey(keyName)] = rawValue.toString()
                                }
                                KeyType.STRING_SET -> {
                                    val set = mutableSetOf<String>()
                                    if (rawValue is JSONArray) {
                                        for (i in 0 until rawValue.length()) {
                                            set.add(rawValue.getString(i))
                                        }
                                    } else if (rawValue is String) {
                                        rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { set.add(it) }
                                    }
                                    prefsMap[stringSetPreferencesKey(keyName)] = set
                                }
                            }
                        } else {
                            // Dynamic fallback for any custom or new keys
                            when (rawValue) {
                                is Boolean -> prefsMap[booleanPreferencesKey(keyName)] = rawValue
                                is Int -> prefsMap[intPreferencesKey(keyName)] = rawValue
                                is Long -> prefsMap[longPreferencesKey(keyName)] = rawValue
                                is Double -> prefsMap[floatPreferencesKey(keyName)] = rawValue.toFloat()
                                is String -> prefsMap[stringPreferencesKey(keyName)] = rawValue
                                is JSONArray -> {
                                    val set = mutableSetOf<String>()
                                    for (i in 0 until rawValue.length()) {
                                        set.add(rawValue.getString(i))
                                    }
                                    prefsMap[stringSetPreferencesKey(keyName)] = set
                                }
                            }
                        }
                    }

                    // Enforce safe credentials consistency
                    prefsMap[booleanPreferencesKey("debrid_enabled")] = false
                    prefsMap.remove(stringPreferencesKey("trakt_username"))
                    prefsMap[booleanPreferencesKey("trakt_scrobble_enabled")] = false
                }
            }

            // 2. Synchronize SharedPreferences fallback for first_run_completed
            try {
                context.getSharedPreferences("euthopiar_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_run_completed", true)
                    .apply()
            } catch (_: Exception) {}

            // 3. Restore User Profiles
            if (profilesArr != null && profilesArr.length() > 0) {
                try {
                    val profilesList = json.decodeFromString<List<UserProfile>>(profilesArr.toString())
                    ProfileManager.getInstance(context).restoreProfiles(profilesList, activeProfileId)
                } catch (e: Exception) {
                    android.util.Log.e("ConfigBackupManager", "Failed to restore profiles: ${e.message}", e)
                }
            }

            // 4. Restore Stremio Addons
            if (addonsArr != null && addonsArr.length() > 0) {
                try {
                    val addonsList = json.decodeFromString<List<ManagedAddon>>(addonsArr.toString())
                    AddonRepository.getInstance(context).restoreAddons(addonsList)
                } catch (e: Exception) {
                    android.util.Log.e("ConfigBackupManager", "Failed to restore addons: ${e.message}", e)
                }
            }

            // 5. Sync and Download Repositories in Background
            val repoUrls = mutableSetOf<String>()
            if (reposArr != null) {
                for (i in 0 until reposArr.length()) {
                    val url = reposArr.optString(i)
                    if (url.isNotBlank()) repoUrls.add(url)
                }
            }
            val storedRepoUrls = context.preferencesDataStore.data.first()[stringSetPreferencesKey("repository_urls")]
            if (storedRepoUrls != null) {
                repoUrls.addAll(storedRepoUrls)
            }

            if (repoUrls.isNotEmpty()) {
                val pluginService = PluginRepositoryService(context)
                for (url in repoUrls) {
                    try {
                        pluginService.installAllFromRepo(url)
                    } catch (e: Exception) {
                        android.util.Log.w("ConfigBackupManager", "Could not sync repo $url: ${e.message}")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
