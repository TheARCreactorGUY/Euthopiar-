package com.euthopiar.core.streams

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
    val audioLanguages: List<String> = emptyList()
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
        get() = !infoHash.isNullOrBlank() || !magnetUrl.isNullOrBlank() ||
                streamType?.equals("torrent", ignoreCase = true) == true ||
                streamType?.equals("p2p", ignoreCase = true) == true ||
                (url != null && (url.startsWith("magnet:", ignoreCase = true) ||
                        url.startsWith("torrent://", ignoreCase = true) ||
                        url.startsWith("torrent:", ignoreCase = true) ||
                        ((url.length == 40 || url.length == 64) && url.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' })))

    val effectiveMagnetOrHash: String?
        get() {
            var base = magnetUrl ?: if (!infoHash.isNullOrBlank()) "magnet:?xt=urn:btih:$infoHash" else url?.takeIf { it.startsWith("magnet:", ignoreCase = true) }
            if (base == null && url != null) {
                val cleanUrl = url.trim()
                if (cleanUrl.startsWith("torrent://", ignoreCase = true) || cleanUrl.startsWith("torrent:", ignoreCase = true)) {
                    val raw = cleanUrl.removePrefix("torrent://").removePrefix("TORRENT://")
                        .removePrefix("torrent:").removePrefix("TORRENT:")
                        .substringBefore('/').substringBefore('?').trim()
                    if (raw.isNotEmpty()) {
                        base = "magnet:?xt=urn:btih:$raw"
                    }
                } else if ((cleanUrl.length == 40 || cleanUrl.length == 64) && cleanUrl.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) {
                    base = "magnet:?xt=urn:btih:$cleanUrl"
                }
            }
            if (base != null) {
                val effectiveIdx = fileIdx ?: if (url?.startsWith("torrent://", ignoreCase = true) == true) {
                    val path = url.removePrefix("torrent://").removePrefix("TORRENT://").substringBefore('?')
                    if ('/' in path) path.substringAfter('/').trim().toIntOrNull() else null
                } else null

                if (effectiveIdx != null && !base.contains("index=", ignoreCase = true) && !base.contains("indices=", ignoreCase = true)) {
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
