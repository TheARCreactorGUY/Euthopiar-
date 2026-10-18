package com.euthopiar.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Universal media identity envelope bridging TMDB, IMDb, and AniZip identifiers.
 *
 * Scrapers and streaming engines receive this canonical envelope to resolve
 * torrent or direct streams without relying on fragile string matching.
 */
@Serializable
data class MediaIdentity(
    val tmdbId: String,
    val imdbId: String? = null,
    val mediaType: String = "movie",
    val season: Int? = null,
    val episode: Int? = null,
    val absoluteEpisode: Int? = null,
    val title: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val originalLanguage: String? = null,
    val originCountry: List<String> = emptyList()
) {
    val isTv: Boolean
        get() = mediaType.equals("tv", ignoreCase = true) ||
                mediaType.equals("series", ignoreCase = true) ||
                mediaType.equals("anime", ignoreCase = true)

    val isAnime: Boolean
        get() = mediaType.equals("anime", ignoreCase = true) ||
                (genres.any { it.equals("Animation", ignoreCase = true) || it.equals("Anime", ignoreCase = true) } &&
                        (originalLanguage.equals("ja", ignoreCase = true) || originCountry.any { it.equals("JP", ignoreCase = true) }))

    fun toCallArgsJson(extraArgs: Map<String, String> = emptyMap()): String {
        val map = mutableMapOf<String, JsonElement>(
            "tmdbId" to JsonPrimitive(tmdbId),
            "mediaType" to JsonPrimitive(mediaType),
            "season" to (season?.let(::JsonPrimitive) ?: JsonNull),
            "episode" to (episode?.let(::JsonPrimitive) ?: JsonNull),
            "imdbId" to (imdbId?.let(::JsonPrimitive) ?: JsonNull),
            "absoluteEpisode" to (absoluteEpisode?.let(::JsonPrimitive) ?: JsonNull),
            "title" to (title?.let(::JsonPrimitive) ?: JsonNull),
            "year" to (year?.let(::JsonPrimitive) ?: JsonNull),
            "id" to JsonPrimitive(tmdbId)
        )
        extraArgs.forEach { (k, v) ->
            map[k] = JsonPrimitive(v)
        }
        return JsonObject(map).toString()
    }
}
