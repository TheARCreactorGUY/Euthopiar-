package com.euthopiar.core.addons

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
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
            json.parseToJsonElement(payload).jsonObject
        } catch (_: Exception) {
            return ParsedCatalogResponse(emptyList(), 0, null)
        }

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

                val mediaType = when (typeStr.lowercase()) {
                    "series", "tv" -> MediaType.TV_SERIES
                    "anime" -> MediaType.ANIME
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
                            url = "",
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

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull?.trim()

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())
}
