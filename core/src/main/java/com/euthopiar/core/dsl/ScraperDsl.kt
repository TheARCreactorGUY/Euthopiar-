package com.euthopiar.core.dsl

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.AudioTrackDescriptor
import com.euthopiar.core.model.CastMember
import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.DownloadOption
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.StreamEmission
import com.euthopiar.core.model.StreamResult
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.model.SubtitleTrack
import com.euthopiar.core.model.UniversalPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

val DefaultScraperJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// ============================================================================
// 1. HTTP Networking DSL Extensions
// ============================================================================

suspend fun OkHttpClient.httpGet(
    url: String,
    headers: Map<String, String> = emptyMap()
): Response = withContext(Dispatchers.IO) {
    val reqBuilder = Request.Builder().url(url)
    headers.forEach { (k, v) -> reqBuilder.header(k, v) }
    newCall(reqBuilder.build()).execute()
}

suspend fun OkHttpClient.httpPost(
    url: String,
    body: RequestBody,
    headers: Map<String, String> = emptyMap()
): Response = withContext(Dispatchers.IO) {
    val reqBuilder = Request.Builder().url(url).post(body)
    headers.forEach { (k, v) -> reqBuilder.header(k, v) }
    newCall(reqBuilder.build()).execute()
}

suspend fun OkHttpClient.getString(
    url: String,
    headers: Map<String, String> = emptyMap()
): String = withContext(Dispatchers.IO) {
    httpGet(url, headers).use { resp ->
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code} for $url")
        resp.body?.string() ?: ""
    }
}

suspend fun OkHttpClient.getJson(
    url: String,
    headers: Map<String, String> = emptyMap(),
    json: Json = DefaultScraperJson
): JsonElement = withContext(Dispatchers.IO) {
    val text = getString(url, headers)
    json.parseToJsonElement(text)
}

suspend fun OkHttpClient.getHtml(
    url: String,
    headers: Map<String, String> = emptyMap()
): Document = withContext(Dispatchers.IO) {
    val html = getString(url, headers)
    Jsoup.parse(html, url)
}

// ============================================================================
// 2. Safe JSON Extraction Extensions
// ============================================================================

fun JsonElement?.str(key: String): String? =
    this?.jsonObject?.get(key)?.jsonPrimitive?.contentOrNull

fun JsonElement?.int(key: String): Int? =
    this?.jsonObject?.get(key)?.jsonPrimitive?.intOrNull

fun JsonElement?.long(key: String): Long? =
    this?.jsonObject?.get(key)?.jsonPrimitive?.longOrNull

fun JsonElement?.double(key: String): Double? =
    this?.jsonObject?.get(key)?.jsonPrimitive?.doubleOrNull

fun JsonElement?.bool(key: String): Boolean? =
    this?.jsonObject?.get(key)?.jsonPrimitive?.booleanOrNull

fun JsonElement?.arr(key: String): JsonArray? =
    this?.jsonObject?.get(key)?.jsonArray

fun JsonElement?.obj(key: String): JsonObject? =
    this?.jsonObject?.get(key)?.jsonObject

// ============================================================================
// 3. Jsoup DOM Query Extensions
// ============================================================================

fun Element.textOrNull(selector: String): String? =
    selectFirst(selector)?.text()?.trim()?.ifBlank { null }

fun Element.attrOrNull(selector: String, attribute: String): String? =
    selectFirst(selector)?.attr(attribute)?.trim()?.ifBlank { null }

fun Element.href(selector: String = "a"): String? =
    attrOrNull(selector, "href")

fun Element.src(selector: String = "img"): String? =
    attrOrNull(selector, "src")

// ============================================================================
// 4. Declarative Domain Model Builders
// ============================================================================

class MediaItemBuilder {
    var id: String = ""
    var title: String = ""
    var url: String = ""
    var posterUrl: String? = null
    var backdropUrl: String? = null
    var type: MediaType = MediaType.MOVIE
    var year: Int? = null
    var rating: String? = null
    var quality: String? = null
    var provider: String? = null
    var logoUrl: String? = null
    var contentRating: String? = null
    var rottenTomatoesRating: String? = null
    var genres: List<String> = emptyList()

    fun build(): MediaItem {
        return MediaItem(
            id = id,
            title = title,
            url = url,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            type = type,
            year = year,
            rating = rating,
            quality = quality,
            provider = provider,
            logoUrl = logoUrl,
            contentRating = contentRating,
            rottenTomatoesRating = rottenTomatoesRating,
            genres = genres
        )
    }
}

fun mediaItem(block: MediaItemBuilder.() -> Unit): MediaItem =
    MediaItemBuilder().apply(block).build()

class CatalogRowBuilder(val title: String) {
    private val items = mutableListOf<MediaItem>()

    fun item(block: MediaItemBuilder.() -> Unit) {
        items.add(MediaItemBuilder().apply(block).build())
    }

    fun addAll(newItems: Collection<MediaItem>) {
        items.addAll(newItems)
    }

    fun build(): CatalogRow = CatalogRow(title = title, items = items)
}

fun catalogRow(title: String, block: CatalogRowBuilder.() -> Unit): CatalogRow =
    CatalogRowBuilder(title).apply(block).build()

class StreamResultBuilder {
    private val streams = mutableListOf<StreamSource>()
    private val subtitles = mutableListOf<SubtitleTrack>()

    fun stream(
        url: String,
        serverName: String = "Default",
        resolutionLabel: String = "Auto",
        quality: String = resolutionLabel,
        isM3u8: Boolean = url.contains(".m3u8", ignoreCase = true),
        releaseType: AudioReleaseType = AudioReleaseType.ORIGINAL,
        audioTracks: List<AudioTrackDescriptor> = emptyList(),
        headers: Map<String, String> = emptyMap()
    ) {
        streams.add(
            StreamSource(
                url = url,
                serverName = serverName,
                resolutionLabel = resolutionLabel,
                quality = quality,
                isM3u8 = isM3u8,
                releaseType = releaseType,
                audioTracks = audioTracks,
                headers = headers
            )
        )
    }

    fun subtitle(url: String, language: String) {
        subtitles.add(SubtitleTrack(url = url, language = language))
    }

    fun build(): StreamResult = StreamResult(streams = streams, subtitles = subtitles)
}

fun streamResult(block: StreamResultBuilder.() -> Unit): StreamResult =
    StreamResultBuilder().apply(block).build()

class MediaDetailBuilder(val mediaItem: MediaItem) {
    var synopsis: String? = null
    var genres: List<String> = emptyList()
    var duration: String? = null
    var rating: String? = null
    var contentRating: String? = null
    var logoUrl: String? = null
    var imdbId: String? = null
    var availableAudioTypes: Set<AudioReleaseType> = setOf(AudioReleaseType.SUB)

    private val episodes = mutableListOf<EpisodeItem>()
    private val cast = mutableListOf<CastMember>()

    fun episode(
        id: String,
        title: String,
        seasonNumber: Int,
        episodeNumber: Int,
        data: String,
        thumbnail: String? = null,
        description: String? = null,
        duration: String? = null,
        audioType: AudioReleaseType = AudioReleaseType.SUB,
        availableLanguages: List<String> = emptyList()
    ) {
        episodes.add(
            EpisodeItem(
                id = id,
                title = title,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                data = data,
                thumbnail = thumbnail,
                description = description,
                duration = duration,
                audioType = audioType,
                availableLanguages = availableLanguages
            )
        )
    }

    fun addEpisodes(items: Collection<EpisodeItem>) {
        episodes.addAll(items)
    }

    fun castMember(name: String, character: String? = null, profileUrl: String? = null, id: String = name) {
        cast.add(CastMember(id = id, name = name, character = character, profileUrl = profileUrl))
    }

    fun addCast(members: Collection<CastMember>) {
        cast.addAll(members)
    }

    fun build(): MediaDetail = MediaDetail(
        id = mediaItem.id,
        title = mediaItem.title,
        url = mediaItem.url,
        posterUrl = mediaItem.posterUrl,
        backdropUrl = mediaItem.backdropUrl,
        type = mediaItem.type,
        year = mediaItem.year,
        synopsis = synopsis,
        genres = genres,
        duration = duration,
        episodes = episodes,
        rating = rating,
        contentRating = contentRating,
        provider = mediaItem.provider,
        cast = cast,
        logoUrl = logoUrl,
        imdbId = imdbId,
        availableAudioTypes = availableAudioTypes
    )
}

fun mediaDetail(mediaItem: MediaItem, block: MediaDetailBuilder.() -> Unit): MediaDetail =
    MediaDetailBuilder(mediaItem).apply(block).build()

// ============================================================================
// 5. Declarative Scraper Base Class
// ============================================================================

abstract class DeclarativePlugin(
    override val name: String,
    override val mainUrl: String,
    protected val client: OkHttpClient
) : UniversalPlugin {

    protected suspend fun fetchString(url: String, headers: Map<String, String> = emptyMap()): String =
        client.getString(url, headers)

    protected suspend fun fetchJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement =
        client.getJson(url, headers)

    protected suspend fun fetchHtml(url: String, headers: Map<String, String> = emptyMap()): Document =
        client.getHtml(url, headers)
}
