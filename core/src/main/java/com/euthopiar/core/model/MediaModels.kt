package com.euthopiar.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class MediaType { MOVIE, TV_SERIES, ANIME }

@Immutable
@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val posterUrl: String?,
    val backdropUrl: String? = null,
    val type: MediaType,
    val year: Int? = null,
    val rating: String? = null,
    val quality: String? = null,
    val provider: String? = null,
    val logoUrl: String? = null,
    val contentRating: String? = null,
    val rottenTomatoesRating: String? = null,
    val genres: List<String> = emptyList(),
) {
    fun withEnrichment(
        logoUrl: String? = this.logoUrl,
        backdropUrl: String? = this.backdropUrl,
        posterUrl: String? = this.posterUrl,
        rating: String? = this.rating,
        year: Int? = this.year,
        contentRating: String? = this.contentRating,
        rottenTomatoesRating: String? = this.rottenTomatoesRating,
        genres: List<String> = this.genres
    ): MediaItem {
        return this.copy(
            logoUrl = logoUrl ?: this.logoUrl,
            backdropUrl = backdropUrl ?: this.backdropUrl,
            posterUrl = posterUrl ?: this.posterUrl,
            rating = rating ?: this.rating,
            year = year ?: this.year,
            contentRating = contentRating ?: this.contentRating,
            rottenTomatoesRating = rottenTomatoesRating ?: this.rottenTomatoesRating,
            genres = if (genres.isNotEmpty()) genres else this.genres
        )
    }
}

@Immutable
@Serializable
data class CatalogRow(
    val title: String,
    val items: List<MediaItem>,
    val subtitle: String? = null,
    val eyebrow: String? = null
)

@Immutable
@Serializable
data class CastMember(
    val id: String,
    val name: String,
    val character: String? = null,
    val profileUrl: String? = null
)

@Serializable
enum class AudioReleaseType {
    SUB,
    DUB,
    DUAL_AUDIO,
    ORIGINAL
}

@Immutable
@Serializable
data class AudioTrackDescriptor(
    val languageName: String, // e.g. "English", "Japanese", "Hindi"
    val isoCode: String,      // e.g. "en", "ja", "hi"
    val channels: Int = 2,    // 2 = Stereo, 6 = 5.1
    val codec: String? = null // "aac", "ac3", etc.
)

@Immutable
@Serializable
data class MediaDetail(
    val id: String,
    val title: String,
    val url: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: MediaType,
    val year: Int?,
    val synopsis: String?,
    val genres: List<String> = emptyList(),
    val duration: String? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val rating: String? = null,
    val contentRating: String? = null,
    val provider: String? = null,
    val cast: List<CastMember> = emptyList(),
    val logoUrl: String? = null,
    val imdbId: String? = null,
    val availableAudioTypes: Set<AudioReleaseType> = setOf(AudioReleaseType.SUB),
    val rottenTomatoesRating: String? = null,
    val recommendations: List<MediaItem> = emptyList(),
    val trailerUrl: String? = null,
    val directors: List<String> = emptyList()
)

@Immutable
@Serializable
data class EpisodeItem(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val data: String, // Payload/URL needed to extract the stream
    val thumbnail: String? = null,
    val description: String? = null,
    val duration: String? = null,
    val audioType: AudioReleaseType = AudioReleaseType.SUB,
    val availableLanguages: List<String> = emptyList()
)

enum class StreamContainer {
    AUTO,
    HLS,
    DASH,
    MP4,
    MKV,
    WEBM,
    TS,
    P2P
}

@Serializable
data class DrmConfig(
    val scheme: String = "clearkey",
    val licenseUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val clearKeys: Map<String, String> = emptyMap()
)

@Immutable
@Serializable
data class StreamSource(
    val url: String,
    val serverName: String = "Default",
    val resolutionLabel: String = "Auto",
    val quality: String = resolutionLabel, // Kept for 100% backward compatibility
    val isM3u8: Boolean = false,
    val audioTracks: List<AudioTrackDescriptor> = emptyList(),
    val releaseType: AudioReleaseType = AudioReleaseType.ORIGINAL,
    val headers: Map<String, String> = emptyMap(), // Referer, Origin, User-Agent
    val responseHeaders: Map<String, String> = emptyMap(),
    val infoHash: String? = null,
    val seeders: Int? = null,
    val fileIdx: Int? = null,
    val addonId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val matchScore: Double = 0.0,
    val size: String? = null,
    val isCached: Boolean = false,
    val hdrType: String? = null,
    val codec: String? = null,
    val addonName: String? = null,
    val container: StreamContainer = if (isM3u8) StreamContainer.HLS else StreamContainer.AUTO,
    val drm: DrmConfig? = null,
    val trackers: List<String> = emptyList(),
    val manifestContent: String? = null
) {
    val server: String get() = serverName
    val isM3M8: Boolean get() = isM3u8

    // Secondary constructor to ensure positional arguments StreamSource(url, quality, isM3u8, headers) compile identically
    constructor(
        url: String,
        quality: String,
        isM3u8: Boolean,
        headers: Map<String, String> = emptyMap()
    ) : this(
        url = url,
        serverName = "Default",
        resolutionLabel = quality,
        quality = quality,
        isM3u8 = isM3u8,
        audioTracks = emptyList(),
        releaseType = AudioReleaseType.ORIGINAL,
        headers = headers,
        responseHeaders = emptyMap(),
        infoHash = null,
        seeders = null,
        fileIdx = null,
        addonId = null,
        season = null,
        episode = null,
        subtitles = emptyList(),
        matchScore = 0.0,
        size = null,
        isCached = false,
        hdrType = null,
        codec = null,
        addonName = null,
        container = if (isM3u8) StreamContainer.HLS else StreamContainer.AUTO,
        drm = null,
        trackers = emptyList(),
        manifestContent = null
    )

    constructor(
        server: String,
        url: String,
        quality: String = "Auto",
        isM3M8: Boolean = false,
        headers: Map<String, String> = emptyMap(),
        subtitles: List<SubtitleTrack> = emptyList(),
        infoHash: String? = null,
        seeders: Int? = null,
        fileIdx: Int? = null,
        addonId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        audioTracks: List<AudioTrackDescriptor> = emptyList(),
        releaseType: AudioReleaseType = AudioReleaseType.ORIGINAL,
        responseHeaders: Map<String, String> = emptyMap(),
        resolutionLabel: String = quality,
        matchScore: Double = 0.0,
        size: String? = null,
        isCached: Boolean = false,
        hdrType: String? = null,
        codec: String? = null,
        addonName: String? = null
    ) : this(
        url = url,
        serverName = server,
        resolutionLabel = resolutionLabel,
        quality = quality,
        isM3u8 = isM3M8,
        audioTracks = audioTracks,
        releaseType = releaseType,
        headers = headers,
        responseHeaders = responseHeaders,
        infoHash = infoHash,
        seeders = seeders,
        fileIdx = fileIdx,
        addonId = addonId,
        season = season,
        episode = episode,
        subtitles = subtitles,
        matchScore = matchScore,
        size = size,
        isCached = isCached,
        hdrType = hdrType,
        codec = codec,
        addonName = addonName,
        container = if (isM3M8) StreamContainer.HLS else StreamContainer.AUTO,
        drm = null,
        trackers = emptyList(),
        manifestContent = null
    )
}

@Immutable
@Serializable
data class SubtitleTrack(
    val url: String,
    val language: String,
    val headers: Map<String, String> = emptyMap(),
    val name: String = language,
    val mimeType: String? = null
) {
    val lang: String get() = language
}

@Immutable
data class StreamResult(
    val streams: List<StreamSource>,
    val subtitles: List<SubtitleTrack> = emptyList()
)

@Immutable
@Serializable
data class DownloadOption(
    val title: String,
    val quality: String,
    val size: String,
    val url: String,
    val source: String,
    val provider: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val isM3u8: Boolean = false,
    val container: StreamContainer = if (isM3u8) StreamContainer.HLS else StreamContainer.AUTO,
    val trackers: List<String> = emptyList()
)

@Immutable
@Serializable
data class CanonicalStream(
    val mediaId: String = "",
    val title: String = "",
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val quality: String = "Auto",
    val isM3u8: Boolean = false,
    val repoId: String? = null,
    val pluginId: String? = null
)


