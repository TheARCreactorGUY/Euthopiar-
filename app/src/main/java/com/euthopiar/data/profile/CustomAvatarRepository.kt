package com.euthopiar.data.profile

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Manages user-uploaded custom profile avatar images for Euthopiar.
 * Keeps up to 3 recent custom uploads for quick reuse and safely scraps older unused ones.
 */
object CustomAvatarRepository {
    private const val PREFS_NAME = "euthopiar_custom_avatars"
    private const val KEY_RECENT_AVATARS = "key_recent_avatars"
    private const val MAX_RECENT_AVATARS = 3
    private const val AVATARS_DIR_NAME = "custom_avatars"

    private var preferences: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _recentAvatars = MutableStateFlow<List<String>>(emptyList())
    val recentAvatars: StateFlow<List<String>> = _recentAvatars.asStateFlow()

    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs

        val dir = getAvatarsDirectory(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val savedJson = prefs.getString(KEY_RECENT_AVATARS, null)
        val loadedList = if (!savedJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<String>>(savedJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val existingFiles = loadedList.filter { fileName ->
            File(dir, fileName).exists()
        }
        _recentAvatars.value = existingFiles
    }

    fun getAvatarsDirectory(context: Context): File {
        return File(context.applicationContext.filesDir, AVATARS_DIR_NAME)
    }

    fun getAvatarFile(context: Context, fileName: String): File {
        return File(getAvatarsDirectory(context), fileName)
    }

    /**
     * Saves a newly cropped circular bitmap to disk and returns "custom:<fileName>".
     */
    fun saveCroppedAvatar(context: Context, bitmap: Bitmap): String {
        initialize(context)
        val dir = getAvatarsDirectory(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val fileName = "avatar_custom_${System.currentTimeMillis()}.png"
        val targetFile = File(dir, fileName)

        try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
        } catch (e: Exception) {
            return "avatar_1"
        }

        val current = _recentAvatars.value.toMutableList()
        current.removeAll { it == fileName }
        current.add(0, fileName)

        val kept = current.take(MAX_RECENT_AVATARS)
        val excess = current.drop(MAX_RECENT_AVATARS)

        val inUseAvatarFiles = ProfileRepository.profiles.value.mapNotNull { profile ->
            if (profile.avatarId.startsWith("custom:")) {
                profile.avatarId.removePrefix("custom:")
            } else null
        }.toSet()

        excess.forEach { excessFile ->
            if (!inUseAvatarFiles.contains(excessFile)) {
                try {
                    val fileToDelete = File(dir, excessFile)
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                } catch (_: Exception) {}
            }
        }

        _recentAvatars.value = kept
        persist()

        return "custom:$fileName"
    }

    private fun persist() {
        val prefs = preferences ?: return
        try {
            val encoded = json.encodeToString(_recentAvatars.value)
            prefs.edit()
                .putString(KEY_RECENT_AVATARS, encoded)
                .apply()
        } catch (_: Exception) {}
    }
}
