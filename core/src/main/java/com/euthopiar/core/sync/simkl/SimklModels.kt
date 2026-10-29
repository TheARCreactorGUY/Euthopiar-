package com.euthopiar.core.sync.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimklUser(
    val user: SimklUserDetails? = null
)

@Serializable
data class SimklTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    val scope: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String = ""
)

@Serializable
data class SimklUserDetails(
    val name: String = "",
    val id: Long = 0L,
    val joined_at: String? = null
)

@Serializable
data class SimklScrobbleItem(
    val title: String,
    val year: Int? = null,
    val ids: SimklIds? = null
)

@Serializable
data class SimklIds(
    val simkl: Long? = null,
    val imdb: String? = null,
    val tmdb: Long? = null
)

@Serializable
data class SimklScrobblePayload(
    val movie: SimklScrobbleItem? = null,
    val show: SimklScrobbleItem? = null,
    val episode: SimklEpisodeItem? = null,
    val progress: Float = 0f
)

@Serializable
data class SimklEpisodeItem(
    val season: Int,
    val number: Int
)

@Serializable
data class SimklResponse(
    val result: String = "ok",
    val message: String? = null
)
