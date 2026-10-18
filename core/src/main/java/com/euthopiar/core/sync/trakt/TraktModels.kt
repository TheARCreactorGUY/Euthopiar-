package com.euthopiar.core.sync.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int
)

@Serializable
data class TraktTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("scope") val scope: String = "",
    @SerialName("created_at") val createdAt: Long = 0L
)

@Serializable
data class TraktUser(
    val username: String = "",
    val name: String = "",
    val vip: Boolean = false
)

@Serializable
data class TraktMovie(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktShow(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktEpisode(
    val season: Int,
    val number: Int,
    val title: String? = null,
    val ids: TraktIds? = null
)

@Serializable
data class TraktIds(
    val trakt: Int? = null,
    val slug: String? = null,
    val imdb: String? = null,
    val tmdb: Int? = null
)

@Serializable
data class TraktScrobbleBody(
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null,
    val progress: Float = 0f, // 0.0 to 100.0
    val app_version: String = "1.0.0"
)

@Serializable
data class TraktScrobbleResponse(
    val id: Long? = null,
    val action: String? = null,
    val progress: Float? = null
)
