package com.euthopiar.core.addons

import com.euthopiar.core.model.CastMember
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
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
        return try {
            val parsedElement = try {
                json.parseToJsonElement(payload)
            } catch (_: Exception) {
                return null
            }
            val root = parsedElement as? JsonObject ?: return null
            val meta = root["meta"] as? JsonObject ?: return null

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

            val genres = buildList {
                fun addFromElem(elem: kotlinx.serialization.json.JsonElement?) {
                    when (elem) {
                        is JsonArray -> {
                            for (it in elem) {
                                val p = it as? JsonPrimitive
                                val s = if (p != null && p !is kotlinx.serialization.json.JsonNull) p.contentOrNull?.trim() else null
                                if (!s.isNullOrBlank() && !s.equals("null", ignoreCase = true)) add(s)
                            }
                        }
                        is JsonPrimitive -> {
                            if (elem !is kotlinx.serialization.json.JsonNull) {
                                elem.contentOrNull?.split(",")?.forEach {
                                    val s = it.trim()
                                    if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) add(s)
                                }
                            }
                        }
                        else -> {}
                    }
                }
                addFromElem(meta["genres"])
                addFromElem(meta["genre"])
            }.distinct()

            val runtime = meta.string("runtime")

            val directors = (meta["director"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeUnless { p -> p is kotlinx.serialization.json.JsonNull }?.contentOrNull?.trim()
            }?.filter { it.isNotBlank() }
                ?: (meta["directors"] as? JsonArray)?.mapNotNull {
                    (it as? JsonPrimitive)?.takeUnless { p -> p is kotlinx.serialization.json.JsonNull }?.contentOrNull?.trim()
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
                        if (elem is kotlinx.serialization.json.JsonNull) return@mapNotNull null
                        val actorName = elem.contentOrNull?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: return@mapNotNull null
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
                        if (elem is kotlinx.serialization.json.JsonNull) null
                        else {
                            val s = elem.contentOrNull?.trim()
                            if (!s.isNullOrBlank() && !s.equals("null", ignoreCase = true)) {
                                if (s.startsWith("http://") || s.startsWith("https://")) s
                                else "https://www.youtube.com/watch?v=$s"
                            } else null
                        }
                    }
                    else -> null
                }
            } ?: meta.string("trailer")?.let { t ->
                if (t.startsWith("http://") || t.startsWith("https://")) t
                else "https://www.youtube.com/watch?v=$t"
            }

            val hintsObj = meta["behaviorHints"] as? JsonObject
            val defaultVideoId = hintsObj?.string("defaultVideoId")
            val videosArr = (meta["videos"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()

            val isVodId = id.startsWith("tt", ignoreCase = true) ||
                id.all { it.isDigit() } ||
                listOf("tmdb:", "imdb:", "kitsu:", "anilist:", "mal:", "jikan:", "tmdb/").any { id.startsWith(it, ignoreCase = true) }

            val hasLiveHint = hintsObj?.boolean("isLive") == true ||
                hintsObj?.string("type")?.equals("channel", ignoreCase = true) == true ||
                hintsObj?.string("type")?.equals("live", ignoreCase = true) == true

            val isLiveTv = hasLiveHint ||
                rawType.equals("channel", ignoreCase = true) ||
                rawType.equals("tv_channel", ignoreCase = true) ||
                rawType.equals("live", ignoreCase = true) ||
                rawType.equals("livestream", ignoreCase = true) ||
                (rawType.equals("tv", ignoreCase = true) && !isVodId && videosArr.isEmpty())

            val mediaType = when {
                isLiveTv -> MediaType.LIVE_TV
                rawType.equals("series", ignoreCase = true) || rawType.equals("tv", ignoreCase = true) -> MediaType.TV_SERIES
                rawType.equals("anime", ignoreCase = true) -> MediaType.ANIME
                else -> MediaType.MOVIE
            }

            val parsedEpisodes = videosArr.mapIndexedNotNull { index, vObj ->
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

            val finalEpisodes = if (parsedEpisodes.isEmpty() && (isLiveTv || !defaultVideoId.isNullOrBlank() || mediaType == MediaType.MOVIE || mediaType == MediaType.LIVE_TV)) {
                val targetId = defaultVideoId ?: id
                listOf(
                    EpisodeItem(
                        id = targetId,
                        title = name,
                        seasonNumber = if (mediaType == MediaType.LIVE_TV) 0 else 1,
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

            MediaDetail(
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
        } catch (e: Exception) {
            try {
                android.util.Log.w("AddonMetaParser", "Failed to parse meta: ${e.message}", e)
            } catch (_: Throwable) {}
            null
        }
    }

    private fun JsonObject.string(name: String): String? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        if (!primitive.isString && primitive.content.equals("null", ignoreCase = true)) return null
        val content = primitive.contentOrNull?.trim()
        return if (content.isNullOrBlank()) null else content
    }

    private fun JsonObject.int(name: String): Int? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.boolean(name: String): Boolean? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
    }

    private fun JsonObject.double(name: String): Double? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun JsonObject.stringList(name: String): List<String> =
        (this[name] as? JsonArray)?.mapNotNull {
            val p = it as? JsonPrimitive ?: return@mapNotNull null
            if (p is kotlinx.serialization.json.JsonNull || (!p.isString && p.content.equals("null", ignoreCase = true))) null
            else p.contentOrNull?.trim()?.takeIf { s -> s.isNotBlank() }
        } ?: emptyList()
}
