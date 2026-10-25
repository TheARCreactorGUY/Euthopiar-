package com.euthopiar.core.addons

import com.euthopiar.core.model.CastMember
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AddonMetaParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(
        payload: String,
        id: String = "",
        type: String = "movie",
        addonName: String = "Stremio"
    ): MediaDetail? = parseMeta(payload, addonName)

    fun parseMeta(
        payload: String,
        addonName: String = "Stremio"
    ): MediaDetail? {
        val root = try {
            json.parseToJsonElement(payload).jsonObject
        } catch (_: Exception) {
            return null
        }

        val meta = root["meta"]?.jsonObject ?: return null

        val id = meta.string("id")?.trim() ?: return null
        val rawType = meta.string("type") ?: "movie"
        val name = meta.string("name") ?: meta.string("title") ?: id
        val description = meta.string("description") ?: meta.string("overview")

        val year = meta.string("year")?.take(4)?.toIntOrNull()
            ?: meta.string("releaseInfo")?.take(4)?.toIntOrNull()
            ?: meta.string("released")?.take(4)?.toIntOrNull()

        val imdbRating = meta.string("imdbRating") ?: meta.string("rating")
        val poster = meta.string("poster")
        val background = meta.string("background")
            ?: meta.string("banner")
            ?: meta.string("landscapePoster")
        val logo = meta.string("logo")

        val genres = (meta["genres"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull?.trim()
        }?.filter { it.isNotBlank() }
            ?: meta.string("genre")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()

        val runtime = meta.string("runtime")

        val directors = (meta["director"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull?.trim()
        }?.filter { it.isNotBlank() }
            ?: (meta["directors"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.contentOrNull?.trim()
            }?.filter { it.isNotBlank() }
            ?: meta.string("director")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()

        val castMembers = (meta["cast"] as? JsonArray)?.mapNotNull { elem ->
            when (elem) {
                is JsonObject -> {
                    val actorName = elem.string("name") ?: elem.string("actor") ?: return@mapNotNull null
                    val actorId = elem.string("id") ?: actorName
                    CastMember(
                        id = actorId,
                        name = actorName,
                        character = elem.string("character"),
                        profileUrl = elem.string("profileUrl") ?: elem.string("photo")
                    )
                }
                is JsonPrimitive -> {
                    val actorName = elem.contentOrNull?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    CastMember(
                        id = actorName,
                        name = actorName
                    )
                }
                else -> null
            }
        } ?: emptyList()

        val trailerUrl = (meta["trailers"] as? JsonArray)?.firstNotNullOfOrNull { elem ->
            when (elem) {
                is JsonObject -> {
                    val src = elem.string("source")
                    if (!src.isNullOrBlank()) {
                        if (src.startsWith("http://") || src.startsWith("https://")) src
                        else "https://www.youtube.com/watch?v=$src"
                    } else null
                }
                is JsonPrimitive -> {
                    val s = elem.contentOrNull?.trim()
                    if (!s.isNullOrBlank()) {
                        if (s.startsWith("http://") || s.startsWith("https://")) s
                        else "https://www.youtube.com/watch?v=$s"
                    } else null
                }
                else -> null
            }
        } ?: meta.string("trailer")?.let { t ->
            if (t.startsWith("http://") || t.startsWith("https://")) t
            else "https://www.youtube.com/watch?v=$t"
        }

        val hintsObj = meta["behaviorHints"] as? JsonObject
        val defaultVideoId = hintsObj?.string("defaultVideoId")

        val mediaType = when (rawType.lowercase()) {
            "series", "tv" -> MediaType.TV_SERIES
            "anime" -> MediaType.ANIME
            else -> MediaType.MOVIE
        }

        val videosArr = meta["videos"] as? JsonArray ?: JsonArray(emptyList())
        val parsedEpisodes = videosArr.mapIndexedNotNull { index, elem ->
            val vObj = elem as? JsonObject ?: return@mapIndexedNotNull null
            val videoId = vObj.string("id")?.trim() ?: return@mapIndexedNotNull null
            val epNum = vObj.int("episode") ?: vObj.int("number") ?: (index + 1)
            val sNum = vObj.int("season") ?: 1
            val epTitle = vObj.string("title") ?: vObj.string("name") ?: "Episode $epNum"
            val epThumbnail = vObj.string("thumbnail") ?: vObj.string("poster")
            val epOverview = vObj.string("overview") ?: vObj.string("description")
            EpisodeItem(
                id = videoId,
                title = epTitle,
                seasonNumber = sNum,
                episodeNumber = epNum,
                data = videoId,
                thumbnail = epThumbnail,
                description = epOverview
            )
        }

        val isLiveTv = rawType.equals("tv", ignoreCase = true) || rawType.equals("channel", ignoreCase = true)
        val finalEpisodes = if (parsedEpisodes.isEmpty() && (isLiveTv || !defaultVideoId.isNullOrBlank() || mediaType == MediaType.MOVIE)) {
            val targetId = defaultVideoId ?: id
            listOf(
                EpisodeItem(
                    id = targetId,
                    title = name,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    data = targetId,
                    thumbnail = poster ?: background,
                    description = description
                )
            )
        } else {
            parsedEpisodes
        }

        val imdbId = if (id.startsWith("tt")) id else meta.string("imdb_id") ?: meta.string("imdbId")

        return MediaDetail(
            id = id,
            title = name,
            url = "stremio_type:$rawType",
            posterUrl = poster,
            backdropUrl = background,
            type = mediaType,
            year = year,
            synopsis = description,
            genres = genres,
            duration = runtime,
            episodes = finalEpisodes,
            rating = imdbRating,
            provider = addonName,
            cast = castMembers,
            logoUrl = logo,
            imdbId = imdbId,
            trailerUrl = trailerUrl,
            directors = directors
        )
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull?.trim()

    private fun JsonObject.int(name: String): Int? =
        this[name]?.jsonPrimitive?.let { it.intOrNull ?: it.contentOrNull?.toIntOrNull() }
}
