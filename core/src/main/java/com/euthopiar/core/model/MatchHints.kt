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
    val episode: Int? = null,
    val originalTitle: String? = null,
    val alternativeTitles: Set<String> = emptySet(),
    val premiereYear: Int? = year,
    val seasonYears: Map<Int, Int> = emptyMap(),
    val seasonNames: Map<Int, String> = emptyMap(),
    val tvdbId: Int? = null,
    val absoluteEpisode: Int? = null,
    val malId: Int? = null,
    val anilistId: Int? = null,
    val romajiTitle: String? = null,
    val englishTitle: String? = null,
    val nativeTitle: String? = null,
    val animeSynonyms: List<String> = emptyList(),
    val seasonSpecificTitles: Map<Int, List<String>> = emptyMap()
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
