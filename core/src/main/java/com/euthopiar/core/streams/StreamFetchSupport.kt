package com.euthopiar.core.streams

import com.euthopiar.core.js.PluginRuntimeResult
import com.euthopiar.core.js.PluginScraper

fun PluginRuntimeResult.toStreamItem(
    scraper: PluginScraper,
    addonName: String = scraper.name,
    addonId: String = "plugin:${scraper.id}",
    originalLanguage: String? = null
): StreamItem {
    val combinedText = listOfNotNull(title, name, quality, size).joinToString(" ")

    val detectedQuality = when {
        combinedText.contains("2160", ignoreCase = true) || combinedText.contains("4K", ignoreCase = true) -> "4K 2160p"
        combinedText.contains("1080", ignoreCase = true) || combinedText.contains("FHD", ignoreCase = true) -> "1080p FHD"
        combinedText.contains("720", ignoreCase = true) || combinedText.contains("HD", ignoreCase = true) -> "720p HD"
        combinedText.contains("480", ignoreCase = true) || combinedText.contains("SD", ignoreCase = true) -> "480p SD"
        combinedText.contains("Auto", ignoreCase = true) -> "Adaptive Stream"
        else -> null
    }

    val isTorrent = url.startsWith("magnet:", ignoreCase = true) ||
            url.startsWith("torrent://", ignoreCase = true) ||
            url.startsWith("torrent:", ignoreCase = true) ||
            !infoHash.isNullOrBlank() ||
            type?.equals("torrent", ignoreCase = true) == true ||
            type?.equals("p2p", ignoreCase = true) == true

    val cleanQuality = detectedQuality
        ?: quality?.ifBlank { null }
        ?: if (isTorrent) "Torrent" else "Direct Stream"

    // Extract clean file size (avoid multiline strings from torrent descriptions)
    val sizeRegex = Regex("""(?:💾|size:?|\b)(\d+(?:\.\d+)?\s*(?:GB|MB|GiB|MiB|KB|KiB))\b""", RegexOption.IGNORE_CASE)
    val cleanSize = if (!size.isNullOrBlank() && size.length <= 25 && !size.contains("\n")) {
        size.trim()
    } else {
        val match = sizeRegex.find(listOfNotNull(size, title, name).joinToString(" "))
        match?.groupValues?.get(1)?.trim() ?: size?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
    }

    // Extract seeders count if not directly provided
    val seedersRegex = Regex("""(?:👤|👥|seeds?:?|seeders?:?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    val parsedSeeders = seeders?.takeIf { it > 0 } ?: run {
        val match = seedersRegex.find(listOfNotNull(title, name, size).joinToString(" "))
        match?.groupValues?.get(1)?.toIntOrNull()
    }

    val subtitleParts = listOfNotNull(
        scraper.name.takeIf { it.isNotBlank() },
        cleanQuality.takeIf { it.isNotBlank() },
        cleanSize?.takeIf { it.isNotBlank() },
        parsedSeeders?.takeIf { it > 0 }?.let { "$it seeds" },
        language?.takeIf { it.isNotBlank() }
    )

    val cleanHeaders = headers
        .orEmpty()
        .mapNotNull { (key, value) ->
            val headerName = key.trim()
            val headerValue = value.trim()
            if (headerName.isBlank() || headerValue.isBlank() || headerName.equals("Range", ignoreCase = true)) {
                null
            } else {
                headerName to headerValue
            }
        }
        .toMap()

    val extSubs = subtitles?.map {
        StreamSubtitle(
            url = it.url,
            language = it.language,
            name = it.name,
            headers = it.headers
        )
    } ?: emptyList()

    val magnet = if (url.startsWith("magnet:", ignoreCase = true)) {
        url
    } else if (!infoHash.isNullOrBlank()) {
        "magnet:?xt=urn:btih:$infoHash"
    } else if (url.startsWith("torrent://", ignoreCase = true) || url.startsWith("torrent:", ignoreCase = true)) {
        val raw = url.removePrefix("torrent://").removePrefix("TORRENT://")
            .removePrefix("torrent:").removePrefix("TORRENT:")
            .substringBefore('/').substringBefore('?').trim()
        if (raw.isNotEmpty()) "magnet:?xt=urn:btih:$raw" else null
    } else null

    val audioLangs = AudioLanguageDiscovery.parseAudioLanguagesFromMetadata(
        listOfNotNull(title, name, language, size).joinToString(" "),
        originalLanguage
    )

    return StreamItem(
        name = name ?: title,
        title = title,
        description = subtitleParts.joinToString(" • ").ifBlank { null },
        url = url,
        infoHash = infoHash,
        magnetUrl = magnet,
        seeders = parsedSeeders,
        peers = peers,
        quality = cleanQuality,
        size = cleanSize,
        sourceName = scraper.name,
        addonName = addonName,
        addonId = addonId,
        streamType = type,
        headers = cleanHeaders,
        externalSubtitles = extSubs,
        audioLanguages = audioLangs
    )
}

fun List<StreamItem>.sortedForGroupedDisplay(): List<StreamItem> =
    sortedWith(
        compareBy<StreamItem> { item ->
            val q = item.quality?.lowercase() ?: ""
            when {
                q.contains("4k") || q.contains("2160") -> 0
                q.contains("1080") || q.contains("fhd") -> 1
                q.contains("720") || q.contains("hd") -> 2
                q.contains("480") || q.contains("sd") -> 3
                else -> 4
            }
        }.thenByDescending { it.seeders ?: 0 }
         .thenBy { it.sourceName.orEmpty().lowercase() }
    )
