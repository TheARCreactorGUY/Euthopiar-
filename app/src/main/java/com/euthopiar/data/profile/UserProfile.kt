package com.euthopiar.data.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.euthopiar.R
import com.euthopiar.ui.theme.PureWhite
import com.euthopiar.ui.theme.StarGold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Profile role types available in Euthopiar.
 */
@Serializable
enum class ProfileType(val displayName: String, val description: String) {
    ADMIN("Master", "Full administrative access & settings"),
    STANDARD("Standard", "Personal watchlists & recommendations"),
    KIDS("Kids", "Curated family & age-filtered catalog"),
    GUEST("Guest", "Temporary session without history"),
}

/**
 * Avatar preset definition for 4-column 2-row avatar selector.
 */
data class ProfileAvatarPreset(
    val id: String,
    val label: String,
    val assetPath: String = "profiles/$id.png",
    val drawableRes: Int = R.drawable.avatar_retro_cinephile,
)

val defaultAvatarPresets = listOf(
    ProfileAvatarPreset("avatar_1", "Avatar 1"),
    ProfileAvatarPreset("avatar_2", "Avatar 2"),
    ProfileAvatarPreset("avatar_3", "Avatar 3"),
    ProfileAvatarPreset("avatar_4", "Avatar 4"),
    ProfileAvatarPreset("avatar_5", "Avatar 5"),
    ProfileAvatarPreset("avatar_6", "Avatar 6"),
    ProfileAvatarPreset("avatar_7", "Avatar 7"),
    ProfileAvatarPreset("avatar_8", "Avatar 8"),
)

/**
 * Legacy avatar presets for backwards compatibility.
 */
enum class AvatarPreset(val id: String, val title: String, val drawableRes: Int) {
    NOIR("noir", "Noir Detective", R.drawable.avatar_noir),
    CYBER_PILOT("pilot", "Cyber Pilot", R.drawable.avatar_cyber_pilot),
    ANIME_HERO("anime", "Anime Hero", R.drawable.avatar_anime_hero),
    DIRECTOR("director", "Film Director", R.drawable.avatar_classic_director),
    PHANTOM("phantom", "Masked Phantom", R.drawable.avatar_phantom_warrior),
    CINEPHILE("cinephile", "3D Cinephile", R.drawable.avatar_retro_cinephile);

    companion object {
        fun fromId(id: String?): AvatarPreset {
            return entries.firstOrNull { it.id == id } ?: NOIR
        }
    }
}

/**
 * Avatar glow color helper mapping avatar preset IDs to vibrant ambient colors.
 */
fun getAvatarGlowColor(avatarId: String): Color {
    return when (avatarId) {
        "avatar_1" -> Color(0xFFE50914) // Crimson
        "avatar_2" -> Color(0xFF00E5FF) // Cyan
        "avatar_3" -> Color(0xFFFF9100) // Amber
        "avatar_4" -> Color(0xFFE040FB) // Neon Purple
        "avatar_5" -> Color(0xFF00E676) // Emerald Green
        "avatar_6" -> Color(0xFFFFD700) // Gold
        "avatar_7" -> Color(0xFF3D5AFE) // Electric Blue
        "avatar_8" -> Color(0xFFFF4081) // Hot Pink
        else -> Color(0xFFD4AF37) // Custom photo ambient gold
    }
}

/**
 * SHA-256 PIN hashing helper for secure PIN storage.
 */
fun hashPin(pin: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

/**
 * Unified User Profile model for Euthopiar.
 */
@Serializable
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ProfileType = ProfileType.STANDARD,
    val isLocked: Boolean = false,
    val pin: String? = null,
    val pinHash: String? = null,
    val avatarId: String = "avatar_1",
    val avatarPresetId: String = "avatar_1",
    val avatarColorIndex: Int = 0,
    val isKids: Boolean = false,
) {
    val isKidsProfile: Boolean get() = isKids || type == ProfileType.KIDS
    val isPinLocked: Boolean get() = isLocked && (!pin.isNullOrBlank() || !pinHash.isNullOrBlank())

    val avatarPreset: ProfileAvatarPreset
        get() = defaultAvatarPresets.find { it.id == avatarId } ?: defaultAvatarPresets.first()

    val legacyAvatarPreset: AvatarPreset
        get() = AvatarPreset.fromId(avatarPresetId)

    fun getAvatarColor(): Color = getAvatarGlowColor(avatarId)

    val initials: String
        get() = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
}

/**
 * Core ProfileManager managing profile state and persistence.
 */
class ProfileManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("euthopiar_profiles", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(
        UserProfile(id = "default_user", name = "Primary", avatarId = "avatar_1", avatarPresetId = "avatar_1", type = ProfileType.ADMIN)
    )
    val activeProfile: StateFlow<UserProfile> = _activeProfile.asStateFlow()

    private val _activeProfileId = MutableStateFlow("default_user")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _unlockedProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedProfileIds: StateFlow<Set<String>> = _unlockedProfileIds.asStateFlow()

    fun isProfileUnlocked(profileId: String): Boolean {
        val profile = _profiles.value.firstOrNull { it.id == profileId } ?: return true
        if (!profile.isPinLocked) return true
        return _unlockedProfileIds.value.contains(profileId)
    }

    fun unlockProfile(profileId: String) {
        _unlockedProfileIds.value = _unlockedProfileIds.value + profileId
    }

    fun lockProfile(profileId: String) {
        _unlockedProfileIds.value = _unlockedProfileIds.value - profileId
    }

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val storedJson = prefs.getString(KEY_PROFILES, null)
        val list = if (!storedJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<UserProfile>>(storedJson)
            } catch (_: Exception) {
                defaultProfiles()
            }
        } else {
            defaultProfiles()
        }
        _profiles.value = list

        val activeId = prefs.getString(KEY_ACTIVE_ID, list.firstOrNull()?.id ?: "default_user") ?: "default_user"
        _activeProfileId.value = activeId
        _activeProfile.value = list.firstOrNull { it.id == activeId } ?: list.firstOrNull() ?: defaultProfiles().first()
    }

    private fun defaultProfiles(): List<UserProfile> {
        val defaults = listOf(
            UserProfile(id = "default_user", name = "Primary", avatarId = "avatar_1", avatarPresetId = "avatar_1", type = ProfileType.ADMIN),
            UserProfile(id = "cinema_user", name = "Cinema", avatarId = "avatar_2", avatarPresetId = "avatar_2", type = ProfileType.STANDARD)
        )
        saveProfiles(defaults)
        return defaults
    }

    private fun saveProfiles(list: List<UserProfile>) {
        prefs.edit().putString(KEY_PROFILES, json.encodeToString(list)).apply()
        _profiles.value = list
    }

    fun switchProfile(profileId: String): Boolean {
        val target = _profiles.value.firstOrNull { it.id == profileId } ?: return false
        val previousId = _activeProfileId.value
        if (previousId != profileId) {
            _unlockedProfileIds.value = _unlockedProfileIds.value - previousId
        }
        _activeProfileId.value = profileId
        _activeProfile.value = target
        prefs.edit().putString(KEY_ACTIVE_ID, profileId).apply()
        return true
    }

    fun selectProfile(profileId: String): Boolean = switchProfile(profileId)

    fun updateProfile(updated: UserProfile): Boolean {
        val currentList = _profiles.value
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index == -1) return false
        val finalProfile = if (!updated.pin.isNullOrBlank()) {
            updated.copy(
                pin = null,
                pinHash = hashPin(updated.pin),
                isLocked = true
            )
        } else if (!updated.isLocked) {
            updated.copy(
                pin = null,
                pinHash = null,
                isLocked = false
            )
        } else {
            updated
        }
        val newList = currentList.toMutableList()
        newList[index] = finalProfile
        saveProfiles(newList)
        if (_activeProfile.value.id == finalProfile.id) {
            _activeProfile.value = finalProfile
            if (finalProfile.isPinLocked) {
                unlockProfile(finalProfile.id)
            }
        }
        return true
    }

    fun addProfile(
        name: String,
        avatarPresetId: String,
        avatarColorIndex: Int = 0,
        pin: String? = null,
        pinHash: String? = null,
        isKids: Boolean = false,
        type: ProfileType = if (isKids) ProfileType.KIDS else ProfileType.STANDARD
    ): UserProfile {
        val finalPinHash = if (!pin.isNullOrBlank()) hashPin(pin) else pinHash
        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Profile" },
            avatarId = avatarPresetId,
            avatarPresetId = avatarPresetId,
            avatarColorIndex = avatarColorIndex,
            pin = null,
            pinHash = finalPinHash,
            isLocked = !finalPinHash.isNullOrBlank(),
            type = type,
            isKids = isKids || type == ProfileType.KIDS
        )
        val updated = _profiles.value + newProfile
        saveProfiles(updated)
        return newProfile
    }

    fun addProfile(profile: UserProfile): UserProfile {
        val finalProfile = if (!profile.pin.isNullOrBlank()) {
            profile.copy(
                pin = null,
                pinHash = hashPin(profile.pin),
                isLocked = true
            )
        } else {
            profile
        }
        val previousId = _activeProfileId.value
        if (previousId != finalProfile.id) {
            _unlockedProfileIds.value = _unlockedProfileIds.value - previousId
        }
        val updated = _profiles.value + finalProfile
        saveProfiles(updated)
        _activeProfileId.value = finalProfile.id
        _activeProfile.value = finalProfile
        if (finalProfile.isPinLocked) {
            unlockProfile(finalProfile.id)
        }
        prefs.edit().putString(KEY_ACTIVE_ID, finalProfile.id).apply()
        return finalProfile
    }

    /**
     * Sets the user-created profile during Act 2 onboarding as the initial main profile.
     */
    fun setInitialPrimaryProfile(
        name: String,
        avatarId: String,
        type: ProfileType,
        isLocked: Boolean,
        pin: String?
    ): UserProfile {
        val finalPinHash = if (!pin.isNullOrBlank()) hashPin(pin) else null
        val mainProfile = UserProfile(
            id = "primary_profile",
            name = name.trim().ifEmpty { "Cinephile" },
            type = type,
            isLocked = isLocked,
            pin = null,
            pinHash = finalPinHash,
            avatarId = avatarId,
            avatarPresetId = avatarId,
            isKids = (type == ProfileType.KIDS)
        )
        // Store as the singular initial profile
        saveProfiles(listOf(mainProfile))
        _activeProfileId.value = mainProfile.id
        _activeProfile.value = mainProfile
        prefs.edit().putString(KEY_ACTIVE_ID, mainProfile.id).apply()
        return mainProfile
    }

    fun deleteProfile(profileId: String): Boolean {
        if (_profiles.value.size <= 1) return false
        val updated = _profiles.value.filter { it.id != profileId }
        saveProfiles(updated)
        if (_activeProfile.value.id == profileId) {
            switchProfile(updated.first().id)
        }
        return true
    }

    fun verifyPin(profile: UserProfile, enteredPin: String): Boolean {
        if (!profile.isPinLocked) return true
        if (!profile.pinHash.isNullOrBlank()) {
            return hashPin(enteredPin) == profile.pinHash
        }
        if (!profile.pin.isNullOrBlank()) {
            return profile.pin == enteredPin
        }
        return false
    }

    companion object {
        private const val KEY_PROFILES = "profiles_list"
        private const val KEY_ACTIVE_ID = "active_profile_id"

        @Volatile
        private var instance: ProfileManager? = null

        fun getInstance(context: Context): ProfileManager {
            return instance ?: synchronized(this) {
                instance ?: ProfileManager(context.applicationContext).also {
                    instance = it
                    ProfileRepository.bindManager(it)
                }
            }
        }
    }
}

/**
 * Singleton repository matching Detroit's ProfileRepository API.
 */
object ProfileRepository {
    private var manager: ProfileManager? = null

    private val fallbackProfiles = listOf(
        UserProfile(id = "default_user", name = "Primary", avatarId = "avatar_1", avatarPresetId = "avatar_1")
    )
    private val _profiles = MutableStateFlow(fallbackProfiles)
    val profiles: StateFlow<List<UserProfile>>
        get() = manager?.profiles ?: _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow("default_user")
    val activeProfileId: StateFlow<String>
        get() = manager?.activeProfileId ?: _activeProfileId.asStateFlow()

    fun bindManager(profileManager: ProfileManager) {
        manager = profileManager
    }

    fun initialize(context: Context) {
        ProfileManager.getInstance(context)
    }

    fun selectProfile(id: String) {
        manager?.switchProfile(id)
    }

    fun addProfile(profile: UserProfile) {
        manager?.addProfile(profile)
    }

    fun updateProfile(updated: UserProfile) {
        manager?.updateProfile(updated)
    }

    fun deleteProfile(id: String) {
        manager?.deleteProfile(id)
    }

    fun getActiveProfile(): UserProfile {
        return manager?.activeProfile?.value ?: fallbackProfiles.first()
    }

    fun getProfile(id: String): UserProfile? {
        return profiles.value.find { it.id == id }
    }

    val unlockedProfileIds: StateFlow<Set<String>>
        get() = manager?.unlockedProfileIds ?: MutableStateFlow(emptySet<String>()).asStateFlow()

    fun isProfileUnlocked(id: String): Boolean {
        return manager?.isProfileUnlocked(id) ?: true
    }

    fun unlockProfile(id: String) {
        manager?.unlockProfile(id)
    }

    fun lockProfile(id: String) {
        manager?.lockProfile(id)
    }

    fun verifyPin(profileId: String, pin: String): Boolean {
        val p = getProfile(profileId) ?: return false
        return manager?.verifyPin(p, pin) ?: (!p.isPinLocked || (!p.pinHash.isNullOrBlank() && hashPin(pin) == p.pinHash) || (!p.pin.isNullOrBlank() && p.pin == pin))
    }
}
