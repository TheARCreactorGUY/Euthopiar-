package com.euthopiar.core.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SupabaseAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("refresh_token") val refreshToken: String,
    val user: SupabaseAuthUserResponse? = null
)

@Serializable
data class SupabaseAuthUserResponse(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, JsonElement>? = null
)

@Serializable
data class SupabaseIdTokenRequest(
    val provider: String = "google",
    @SerialName("id_token") val idToken: String,
    val nonce: String? = null
)

@Serializable
data class SupabaseRefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val expiresAtMs: Long,
    val userId: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?
)

@Serializable
data class CloudPlaybackItem(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String = "default_user",
    val provider: String = "default",
    @SerialName("media_id") val mediaId: String,
    val title: String,
    @SerialName("episode_title") val episodeTitle: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    @SerialName("current_position_ms") val currentPositionMs: Long = 0L,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("is_dismissed") val isDismissed: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("media_type") val mediaType: String = "MOVIE",
    @SerialName("episode_data") val episodeData: String? = null,
    @SerialName("last_watched_timestamp") val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudWatchlistItem(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String = "default_user",
    @SerialName("media_id") val mediaId: String,
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    @SerialName("media_type") val mediaType: String = "MOVIE",
    val rating: Double? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    val status: String = "TO_BE_WATCHED",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("added_timestamp") val addedTimestamp: Long = System.currentTimeMillis(),
    @SerialName("client_updated_at_ms") val clientUpdatedAtMs: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudPlaylist(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String = "default_user",
    @SerialName("playlist_id") val playlistId: String,
    val title: String,
    @SerialName("is_system") val isSystem: Boolean = false,
    @SerialName("items_json") val itemsJson: String = "[]",
    @SerialName("created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @SerialName("last_played_at_ms") val lastPlayedAtMs: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("client_updated_at_ms") val clientUpdatedAtMs: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudSettingEntry(
    @SerialName("val") val value: String,
    val ts: Long
)

@Serializable
data class CloudUserSettings(
    @SerialName("user_id") val userId: String,
    @SerialName("settings_json") val settingsJson: Map<String, CloudSettingEntry> = emptyMap(),
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudUserProfile(
    @SerialName("profile_id") val profileId: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("profile_type") val profileType: String = "STANDARD",
    @SerialName("avatar_id") val avatarId: String = "avatar_1",
    @SerialName("custom_avatar_url") val customAvatarUrl: String? = null,
    @SerialName("pin_hash") val pinHash: String? = null,
    @SerialName("pin_salt") val pinSalt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("client_updated_at_ms") val clientUpdatedAtMs: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudUserAccount(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("active_profile_id") val activeProfileId: String = "default_user",
    @SerialName("client_updated_at_ms") val clientUpdatedAtMs: Long = System.currentTimeMillis()
)
