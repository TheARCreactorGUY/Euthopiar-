package com.euthopiar.core.addons

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ParsedCatalogResponse(
    val items: List<MediaItem>,
    val rawItemCount: Int,
    val nextSkip: Int? = null
)

object HomeCatalogParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseCatalog(
        payload: String,
        addonName: String = "Stremio",
        maxItems: Int? = null
    ): List<MediaItem> {
        return parseCatalogResponse(
            payload = payload,
            addonName = addonName,
            maxItems = maxItems
        ).items
    }

    fun parseCatalogResponse(
        payload: String,
        addonName: String = "Stremio",
        currentSkip: Int? = null,
        maxItems: Int? = null
    ): ParsedCatalogResponse {
        val root = try {
            json.parseToJsonElement(payload) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return ParsedCatalogResponse(emptyList(), 0, null)

        val metas = root.array("metas")
        val parsedItems = buildList {
            val seenKeys = mutableSetOf<String>()
            for (element in metas) {
                if (maxItems != null && size >= maxItems) break

                val meta = element as? JsonObject ?: continue
                val id = meta.string("id")
                val typeStr = meta.string("type") ?: "movie"
                val name = meta.string("name") ?: meta.string("title")

                if (id.isNullOrBlank() || name.isNullOrBlank()) {
                    continue
                }

                val hintsObj = meta["behaviorHints"] as? JsonObject
                val isLiveHint = hintsObj?.boolean("isLive") == true ||
                    hintsObj?.string("type")?.equals("channel", ignoreCase = true) == true ||
                    hintsObj?.string("type")?.equals("live", ignoreCase = true) == true

                val isVodId = id.startsWith("tt", ignoreCase = true) ||
                    id.all { it.isDigit() } ||
                    listOf("tmdb:", "imdb:", "kitsu:", "anilist:", "mal:", "jikan:", "tmdb/").any { id.startsWith(it, ignoreCase = true) }

                val isLiveTv = typeStr.equals("channel", ignoreCase = true) ||
                    typeStr.equals("live", ignoreCase = true) ||
                    typeStr.equals("tv_channel", ignoreCase = true) ||
                    isLiveHint ||
                    (typeStr.equals("tv", ignoreCase = true) && !isVodId)

                val mediaType = when {
                    isLiveTv -> MediaType.LIVE_TV
                    typeStr.equals("series", ignoreCase = true) || typeStr.equals("tv", ignoreCase = true) -> MediaType.TV_SERIES
                    typeStr.equals("anime", ignoreCase = true) -> MediaType.ANIME
                    else -> MediaType.MOVIE
                }

                val landscapePoster = meta.string("landscapePoster")?.takeIf { it.isNotBlank() }
                val poster = meta.string("poster")
                val backdrop = meta.string("banner") ?: meta.string("background") ?: landscapePoster
                val logo = meta.string("logo")
                val year = meta.string("releaseInfo")?.take(4)?.toIntOrNull()
                    ?: meta.string("year")?.take(4)?.toIntOrNull()
                    ?: meta.string("released")?.take(4)?.toIntOrNull()
                val rating = meta.string("imdbRating") ?: meta.string("rating")

                val stableKey = "$typeStr:$id"
                if (seenKeys.add(stableKey)) {
                    add(
                        MediaItem(
                            id = id,
                            title = name,
                            url = "stremio_type:$typeStr",
                            posterUrl = poster,
                            backdropUrl = backdrop,
                            type = mediaType,
                            year = year,
                            rating = rating,
                            provider = addonName,
                            logoUrl = logo
                        )
                    )
                }
            }
        }

        val nextSkip = if (metas.size > 0) {
            (currentSkip ?: 0) + metas.size
        } else {
            null
        }

        return ParsedCatalogResponse(
            items = parsedItems,
            rawItemCount = metas.size,
            nextSkip = nextSkip
        )
    }

    private fun JsonObject.string(name: String): String? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        if (!primitive.isString && primitive.content.equals("null", ignoreCase = true)) return null
        val content = primitive.contentOrNull?.trim()
        return if (content.isNullOrBlank()) null else content
    }

    private fun JsonObject.boolean(name: String): Boolean? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
    }

    private fun JsonObject.int(name: String): Int? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())
}
