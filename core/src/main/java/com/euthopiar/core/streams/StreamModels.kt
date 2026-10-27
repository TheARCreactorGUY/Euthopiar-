package com.euthopiar.core.streams

import com.euthopiar.core.model.StreamContainer
import kotlinx.serialization.Serializable

@Serializable
data class StreamSubtitle(
    val url: String,
    val language: String,
    val name: String? = null,
    val headers: Map<String, String>? = null
)

data class StreamItem(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val magnetUrl: String? = null,
    val quality: String? = null,
    val size: String? = null,
    val sourceName: String? = null,
    val addonName: String,
    val addonId: String,
    val streamType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val externalSubtitles: List<StreamSubtitle> = emptyList(),
    val seeders: Int? = null,
    val peers: Int? = null,
    val isCached: Boolean = false,
    val audioLanguages: List<String> = emptyList(),
    val matchScore: Double = 0.0,
    val filename: String? = null,
    val container: StreamContainer = StreamContainer.AUTO,
    val trackers: List<String> = emptyList(),
    val subtitles: List<StreamSubtitle> = emptyList(),
    val isLive: Boolean = false
) {
    val effectiveInfoHash: String?
        get() = infoHash ?: url?.let { u ->
            Regex("""xt=urn:btih:([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE).find(u)?.groupValues?.get(1)
        } ?: magnetUrl?.let { m ->
            Regex("""xt=urn:btih:([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE).find(m)?.groupValues?.get(1)
        }

    val streamLabel: String
        get() = name?.takeIf { it.isNotBlank() } ?: title?.takeIf { it.isNotBlank() } ?: "Stream"

    val streamSubtitle: String?
        get() = description

    val directPlaybackUrl: String?
        get() = url?.trim()?.takeIf { it.isNotEmpty() }

    val playableDirectUrl: String?
        get() = directPlaybackUrl?.takeIf { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }

    val isTorrentStream: Boolean
        get() {
            val clean = url?.trim()?.substringBefore('?')
            val isTorrentHash = clean != null && (
                ((clean.length == 40 || clean.length == 64) && clean.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) ||
                (clean.length == 32 && clean.all { it.uppercaseChar() in 'A'..'Z' || it in '2'..'7' })
            )
            return !infoHash.isNullOrBlank() || !magnetUrl.isNullOrBlank() ||
                    streamType?.equals("torrent", ignoreCase = true) == true ||
                    streamType?.equals("p2p", ignoreCase = true) == true ||
                    (url != null && (url.startsWith("magnet:", ignoreCase = true) ||
                            url.startsWith("torrent://", ignoreCase = true) ||
                            url.startsWith("torrent:", ignoreCase = true) ||
                            isTorrentHash ||
                            com.euthopiar.core.p2p.extractP2pStreamRequest(url, infoHash) != null))
        }

    val effectiveMagnetOrHash: String?
        get() {
            var rawTorrentUrl = url?.trim()
            if (rawTorrentUrl != null && rawTorrentUrl.startsWith("torrent://", ignoreCase = true)) {
                if (fileIdx != null && fileIdx >= 0 && rawTorrentUrl.count { it == '/' } < 3) {
                    rawTorrentUrl = "$rawTorrentUrl/$fileIdx"
                }
            }

            var base: String? = magnetUrl
            if (base == null && !infoHash.isNullOrBlank()) {
                val idxSuffix = if (fileIdx != null && fileIdx >= 0) "&index=$fileIdx" else ""
                base = "magnet:?xt=urn:btih:$infoHash$idxSuffix"
            }
            if (base == null && rawTorrentUrl != null && rawTorrentUrl.startsWith("magnet:", ignoreCase = true)) {
                base = rawTorrentUrl
            }

            if (base == null && rawTorrentUrl != null) {
                if (rawTorrentUrl.startsWith("torrent://", ignoreCase = true) || rawTorrentUrl.startsWith("torrent:", ignoreCase = true)) {
                    val raw = rawTorrentUrl.removePrefix("torrent://").removePrefix("TORRENT://")
                        .removePrefix("torrent:").removePrefix("TORRENT:")
                        .substringBefore('/').substringBefore('?').trim()
                    if (raw.isNotEmpty()) {
                        val idxSuffix = if (fileIdx != null && fileIdx >= 0) "&index=$fileIdx" else ""
                        base = "magnet:?xt=urn:btih:$raw$idxSuffix"
                    }
                } else {
                    val cleanHash = rawTorrentUrl.substringBefore('?').trim()
                    if (((cleanHash.length == 40 || cleanHash.length == 64) && cleanHash.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) ||
                        (cleanHash.length == 32 && cleanHash.all { it.uppercaseChar() in 'A'..'Z' || it in '2'..'7' })) {
                        val idxSuffix = if (fileIdx != null && fileIdx >= 0) "&index=$fileIdx" else ""
                        base = "magnet:?xt=urn:btih:$cleanHash$idxSuffix"
                    }
                }
            }

            if (base != null) {
                val effectiveIdx = (if (fileIdx != null && fileIdx >= 0) fileIdx else null) ?: if (rawTorrentUrl?.startsWith("torrent://", ignoreCase = true) == true || rawTorrentUrl?.startsWith("torrent:", ignoreCase = true) == true) {
                    val path = if (rawTorrentUrl.startsWith("torrent://", ignoreCase = true)) {
                        rawTorrentUrl.substring("torrent://".length).substringBefore('?')
                    } else {
                        rawTorrentUrl.substring("torrent:".length).substringBefore('?')
                    }
                    if ('/' in path) path.substringAfter('/').trim().toIntOrNull() else null
                } else null

                if (effectiveIdx != null &&
                    !base.contains("index=", ignoreCase = true) &&
                    !base.contains("indices=", ignoreCase = true) &&
                    !base.contains("fileidx=", ignoreCase = true) &&
                    !base.contains("so=", ignoreCase = true)
                ) {
                    val delim = if (base.contains("?")) "&" else "?"
                    base = "$base${delim}index=$effectiveIdx"
                }
                val nameHint = title ?: name
                if (!nameHint.isNullOrBlank() && !base.contains("dn=", ignoreCase = true)) {
                    val delim = if (base.contains("?")) "&" else "?"
                    val encodedDn = try { java.net.URLEncoder.encode(nameHint, "UTF-8") } catch (_: Throwable) { null }
                    if (encodedDn != null) {
                        base = "$base${delim}dn=$encodedDn"
                    }
                }
            }
            return base
        }

    fun isSupportedDownloadUrl(): Boolean {
        val u = playableDirectUrl?.lowercase() ?: return false
        if (u.contains("pixel.hubcloud", ignoreCase = true)) return false
        return true
    }
}

data class AddonStreamGroup(
    val addonName: String,
    val addonId: String,
    val streams: List<StreamItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class StreamsEmptyStateReason {
    NoAddonsInstalled,
    NoCompatibleAddons,
    NoStreamsFound,
    StreamFetchFailed
}

data class StreamsUiState(
    val requestToken: String? = null,
    val groups: List<AddonStreamGroup> = emptyList(),
    val isAnyLoading: Boolean = false,
    val emptyStateReason: StreamsEmptyStateReason? = null,
    val autoPlayStream: StreamItem? = null
) {
    val allStreams: List<StreamItem>
        get() = groups.flatMap { it.streams }

    val hasAnyStreams: Boolean
        get() = groups.any { it.streams.isNotEmpty() }
}

internal sealed interface StreamLoadCompletion {
    data class Addon(val group: AddonStreamGroup) : StreamLoadCompletion
    data class PluginScraper(
        val addonId: String,
        val addonName: String,
        val streams: List<StreamItem>,
        val error: String? = null
    ) : StreamLoadCompletion
}
