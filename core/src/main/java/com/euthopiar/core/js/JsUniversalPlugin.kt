package com.euthopiar.core.js

import com.euthopiar.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Bridges a JavaScript plugin scraper ([PluginScraper]) to the [UniversalPlugin] interface,
 * allowing it to seamlessly integrate with [com.euthopiar.core.registry.PluginRegistry].
 */
class JsUniversalPlugin(
    val scraper: PluginScraper
) : UniversalPlugin {
    override val name: String get() = scraper.name
    override val mainUrl: String get() = ""
    override val supportedTypes: Set<MediaType> get() = scraper.supportedTypes.mapNotNull {
        when (it.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv", "series" -> MediaType.TV_SERIES
            "anime" -> MediaType.ANIME
            else -> null
        }
    }.toSet().ifEmpty { setOf(MediaType.MOVIE, MediaType.TV_SERIES) }

    override suspend fun getHomeCatalog(): List<CatalogRow> = emptyList()

    override suspend fun search(query: String): List<MediaItem> = emptyList()

    override suspend fun getDetails(mediaItem: MediaItem): MediaDetail {
        return MediaDetail(
            id = mediaItem.id,
            title = mediaItem.title,
            url = mediaItem.url,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            type = mediaItem.type,
            year = mediaItem.year,
            synopsis = null,
            provider = scraper.name
        )
    }

    override suspend fun getStreamLinks(episodeData: String): StreamResult {
        val streams = mutableListOf<StreamSource>()
        val subtitles = mutableListOf<SubtitleTrack>()
        getStreamFlow(episodeData).collect { emission ->
            when (emission) {
                is StreamEmission.SourceFound -> streams.add(emission.source)
                is StreamEmission.SubtitleFound -> subtitles.add(emission.track)
                else -> {}
            }
        }
        return StreamResult(streams.distinctBy { it.url }, subtitles.distinctBy { it.url })
    }

    override fun getStreamFlow(episodeData: String): Flow<StreamEmission> = flow {
        val parts = episodeData.split(":")
        val tmdbId = parts.getOrNull(0) ?: episodeData
        val season = parts.getOrNull(1)?.toIntOrNull()
        val episode = parts.getOrNull(2)?.toIntOrNull()
        val isTv = season != null && episode != null

        emit(StreamEmission.StatusUpdate(scraper.name, "Resolving streams..."))
        try {
            val results = PluginRuntime.executePlugin(
                code = scraper.code,
                tmdbId = tmdbId,
                mediaType = if (isTv) "tv" else "movie",
                season = season,
                episode = episode,
                scraperId = scraper.id,
                repoId = scraper.canonicalRepoId
            )

            for (res in results) {
                val serverName = if (res.name != null) "${scraper.name} (${res.name})" else scraper.name
                val source = res.toStreamSource().copy(
                    serverName = serverName
                )
                emit(StreamEmission.SourceFound(source))
                for (sub in res.toSubtitles()) {
                    emit(StreamEmission.SubtitleFound(sub))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("JsUniversalPlugin", "Error executing scraper ${scraper.name}: ${e.message}")
        }
    }
}
