package com.euthopiar.core.model

import kotlinx.serialization.Serializable

enum class ContentType { MOVIE, TV_SERIES, ANIME }

/**
 * Metadata hints supplied to match TMDB media with scrapers.
 */
@Serializable
data class MatchHints(
    val tmdbId: Int,
    val imdbId: String? = null,
    val type: ContentType,
    val titles: Set<String>,
    val year: Int?,
    val runtimeMin: Int? = null,
    val season: Int? = null,
    val episode: Int? = null
)

/**
 * Lightweight card representation for search/catalog matches.
 */
@Serializable
data class MediaCard(
    val id: String,
    val title: String,
    val year: Int? = null,
    val type: ContentType = ContentType.MOVIE,
    val posterUrl: String? = null,
    val releaseYear: Int? = year
)
