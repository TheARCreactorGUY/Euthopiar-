package com.nuvio.app.features.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class TraktCommentReview(
    val id: Long,
    val authorDisplayName: String,
    val authorUsername: String? = null,
    val comment: String,
    val spoiler: Boolean = false,
    val containsInlineSpoilers: Boolean = false,
    val review: Boolean = false,
    val likes: Int = 0,
    val rating: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val hasSpoilerContent: Boolean get() = spoiler || containsInlineSpoilers
}

data class TraktCommentsPage(
    val items: List<TraktCommentReview>,
    val currentPage: Int,
    val pageCount: Int,
    val itemCount: Int,
)

internal enum class TraktCommentsType(val apiValue: String) {
    MOVIE("movie"),
    SHOW("show"),
}

@Serializable
internal data class TraktCommentDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("comment") val comment: String? = null,
    @SerialName("spoiler") val spoiler: Boolean? = null,
    @SerialName("review") val review: Boolean? = null,
    @SerialName("likes") val likes: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_stats") val userStats: TraktCommentUserStatsDto? = null,
    @SerialName("user") val user: TraktCommentUserDto? = null,
)

@Serializable
internal data class TraktCommentUserDto(
    @SerialName("username") val username: String? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
internal data class TraktCommentUserStatsDto(
    @SerialName("rating") val rating: Int? = null,
)

@Serializable
internal data class TraktCommentsSearchResultDto(
    @SerialName("type") val type: String? = null,
    @SerialName("score") val score: Double? = null,
    @SerialName("movie") val movie: TraktCommentsSearchItemDto? = null,
    @SerialName("show") val show: TraktCommentsSearchItemDto? = null,
)

@Serializable
internal data class TraktCommentsSearchItemDto(
    @SerialName("ids") val ids: TraktCommentsIdsDto? = null,
)

@Serializable
internal data class TraktCommentsIdsDto(
    @SerialName("trakt") val trakt: Long? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("imdb") val imdb: String? = null,
    @SerialName("tmdb") val tmdb: Int? = null,
)
