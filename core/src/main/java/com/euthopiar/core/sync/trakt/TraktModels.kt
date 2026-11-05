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
    val title: String = "",
    val year: Int? = null,
    val ids: TraktIds = TraktIds()
)

@Serializable
data class TraktShow(
    val title: String = "",
    val year: Int? = null,
    val ids: TraktIds = TraktIds()
)

@Serializable
data class TraktEpisode(
    val season: Int = 1,
    val number: Int = 1,
    val title: String? = null,
    val ids: TraktIds? = null
)

@Serializable
data class TraktIds(
    val trakt: Long? = null,
    val slug: String? = null,
    val imdb: String? = null,
    val tmdb: Long? = null
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

@Serializable
data class TraktWatchlistItem(
    val id: Long? = null,
    val rank: Int? = null,
    val listed_at: String? = null,
    val type: String = "", // "movie" or "show"
    val movie: TraktMovie? = null,
    val show: TraktShow? = null
)

@Serializable
data class TraktPlaybackProgressItem(
    val id: Long = 0L,
    val progress: Float = 0f,
    val paused_at: String? = null,
    val type: String = "", // "movie" or "episode"
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null
)

@Serializable
data class TraktHistoryItem(
    val id: Long = 0L,
    val watched_at: String? = null,
    val action: String? = null, // "scrobble", "checkin", "watch"
    val type: String = "", // "movie" or "episode"
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null
)

@Serializable
data class TraktCustomList(
    val name: String = "",
    val description: String? = null,
    val privacy: String = "private",
    val item_count: Int = 0,
    val ids: TraktListIds = TraktListIds()
)

@Serializable
data class TraktListIds(
    val trakt: Long = 0L,
    val slug: String = ""
)

@Serializable
data class TraktListItem(
    val id: Long? = null,
    val rank: Int? = null,
    val listed_at: String? = null,
    val type: String = "",
    val movie: TraktMovie? = null,
    val show: TraktShow? = null
)

@Serializable
data class TraktSyncItemsPayload(
    val movies: List<TraktMovie> = emptyList(),
    val shows: List<TraktShow> = emptyList()
)

@Serializable
data class TraktSyncResponse(
    val added: TraktSyncCount? = null,
    val deleted: TraktSyncCount? = null,
    val existing: TraktSyncCount? = null,
    val not_found: TraktSyncNotFound? = null
)

@Serializable
data class TraktSyncCount(
    val movies: Int = 0,
    val shows: Int = 0,
    val episodes: Int = 0
)

@Serializable
data class TraktSyncNotFound(
    val movies: List<TraktMovie> = emptyList(),
    val shows: List<TraktShow> = emptyList()
)
