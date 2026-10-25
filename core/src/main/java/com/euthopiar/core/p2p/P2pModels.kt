package com.euthopiar.core.p2p

data class P2pSettingsUiState(
    val p2pEnabled: Boolean = true,
    val enableUpload: Boolean = true,
    val hideTorrentStats: Boolean = false,
    val torrentProfile: P2pTorrentProfile = P2pTorrentProfile.BALANCED,
    val cacheSize: P2pCacheSize = P2pCacheSize.GB_2,
)

enum class P2pTorrentProfile {
    SOFT,
    BALANCED,
    FAST,
}

enum class P2pCacheSize(val bytes: Long) {
    NONE(0L),
    GB_2(2L * 1024L * 1024L * 1024L),
    GB_5(5L * 1024L * 1024L * 1024L),
    GB_10(10L * 1024L * 1024L * 1024L),
}

data class P2pCacheUiState(
    val usedBytes: Long = 0L,
    val protectedBytes: Long = 0L,
    val isClearing: Boolean = false,
    val hasMeasurement: Boolean = false,
)

data class P2pCacheClearResult(
    val reclaimedBytes: Long,
    val remainingBytes: Long,
    val protectedBytes: Long,
)

data class P2pStreamRequest(
    val infoHash: String,
    val fileIdx: Int? = null,
    val filename: String? = null,
    val trackers: List<String> = emptyList(),
    val season: Int? = null,
    val episode: Int? = null,
)

fun decodeBase32ToHex(base32: String): String? {
    val s = base32.trim().uppercase()
    if (s.length != 32) return null
    var buffer = 0L
    var bitsLeft = 0
    val bytes = ByteArray(20)
    var byteIdx = 0

    for (c in s) {
        val v = when (c) {
            in 'A'..'Z' -> c - 'A'
            in '2'..'7' -> c - '2' + 26
            else -> return null
        }
        buffer = (buffer shl 5) or v.toLong()
        bitsLeft += 5
        while (bitsLeft >= 8) {
            bitsLeft -= 8
            if (byteIdx < 20) {
                bytes[byteIdx++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
    }
    if (byteIdx != 20 || bitsLeft != 0) return null
    return bytes.joinToString("") { "%02x".format(it) }
}

fun canonicalP2pInfoHash(infoHash: String): String {
    val trimmed = infoHash.trim()
    if (trimmed.length == 32) {
        val decoded = decodeBase32ToHex(trimmed)
        if (decoded != null) return decoded
    }
    val canonical = trimmed.lowercase()
    require((canonical.length == 40 || canonical.length == 64) &&
        canonical.all { it in '0'..'9' || it in 'a'..'f' }) {
        "Torrent info hash must be 40 or 64 hexadecimal characters, or 32 Base32 characters"
    }
    return canonical
}

fun buildP2pMagnetUri(infoHash: String, trackers: List<String>): String {
    val canonicalHash = canonicalP2pInfoHash(infoHash)
    val topic = if (canonicalHash.length == 40) {
        "urn:btih:$canonicalHash"
    } else {
        "urn:btmh:1220$canonicalHash"
    }
    val trackerParameters = trackers.filter(String::isNotBlank).distinct().joinToString("") { tracker ->
        "&tr=${tracker.encodeP2pQueryValue()}"
    }
    return "magnet:?xt=$topic$trackerParameters"
}

private fun String.encodeP2pQueryValue(): String = buildString {
    for (byte in this@encodeP2pQueryValue.encodeToByteArray()) {
        val value = byte.toInt() and 0xff
        if ((value in 'a'.code..'z'.code) ||
            (value in 'A'.code..'Z'.code) ||
            (value in '0'.code..'9'.code) ||
            value == '-'.code || value == '.'.code || value == '_'.code || value == '~'.code) {
            append(value.toChar())
        } else {
            append('%')
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }
}

private const val HEX_DIGITS = "0123456789ABCDEF"

sealed class P2pStreamingState {
    data object Idle : P2pStreamingState()

    data class Connecting(
        val phase: String = "starting_engine",
        val downloadSpeed: Long = 0L,
        val uploadSpeed: Long = 0L,
        val peers: Int = 0,
        val seeds: Int = 0,
    ) : P2pStreamingState()

    data class Streaming(
        val localUrl: String,
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val peers: Int,
        val seeds: Int,
        val bufferProgress: Float,
        val totalProgress: Float,
        val downloadedBytes: Long = 0L,
        val verifiedBytes: Long = 0L,
        val deliveredBytes: Long = 0L,
    ) : P2pStreamingState()

    data class Error(val message: String) : P2pStreamingState()
}

class P2pStreamingException(message: String) : Exception(message)

fun formatP2pSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_048_576 -> "${(bytesPerSec / 1_048_576.0).formatOneDecimal()} MB/s"
        bytesPerSec >= 1_024 -> "${(bytesPerSec / 1_024.0).formatNoDecimal()} KB/s"
        else -> "$bytesPerSec B/s"
    }
}

fun formatP2pMegabytes(bytes: Long): String =
    "${(bytes / 1_048_576.0).formatOneDecimal()} MB"

private fun Double.formatOneDecimal(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toLong()}.0"
    } else {
        "$rounded"
    }
}

private fun Double.formatNoDecimal(): String =
    "${kotlin.math.round(this).toLong()}"

fun extractP2pStreamRequest(
    rawUrl: String?,
    fallbackInfoHash: String? = null,
    defaultFileIdx: Int? = null,
    defaultSeason: Int? = null,
    defaultEpisode: Int? = null,
    trackers: List<String> = emptyList(),
): P2pStreamRequest? {
    val clean = rawUrl?.trim().orEmpty()
    val hash = when {
        clean.startsWith("magnet:", ignoreCase = true) -> {
            val marker = "btih:"
            val idx = clean.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                val start = idx + marker.length
                val end = clean.indexOf('&', start).takeIf { it >= 0 } ?: clean.length
                clean.substring(start, end).trim()
            } else {
                val btmhMarker = "btmh:1220"
                val btmhIdx = clean.indexOf(btmhMarker, ignoreCase = true)
                if (btmhIdx >= 0) {
                    val start = btmhIdx + btmhMarker.length
                    val end = clean.indexOf('&', start).takeIf { it >= 0 } ?: clean.length
                    clean.substring(start, end).trim()
                } else null
            }
        }
        clean.startsWith("torrent://", ignoreCase = true) || clean.startsWith("torrent:", ignoreCase = true) -> {
            val withoutPrefix = if (clean.startsWith("torrent://", ignoreCase = true)) {
                clean.substring("torrent://".length)
            } else {
                clean.substring("torrent:".length)
            }
            withoutPrefix
                .substringBefore('/')
                .substringBefore('?')
                .trim()
        }
        clean.length == 32 && clean.all { it.uppercaseChar() in 'A'..'Z' || it in '2'..'7' } -> clean
        (clean.length == 40 || clean.length == 64) && clean.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' } -> clean
        else -> fallbackInfoHash?.trim()?.takeIf { it.isNotBlank() }
    }?.takeIf { it.isNotBlank() } ?: return null

    val canonicalHash = try {
        canonicalP2pInfoHash(hash)
    } catch (_: Exception) {
        return null
    }

    val fileIdx = if (clean.startsWith("torrent://", ignoreCase = true) || clean.startsWith("torrent:", ignoreCase = true)) {
        val withoutPrefix = if (clean.startsWith("torrent://", ignoreCase = true)) {
            clean.substring("torrent://".length)
        } else {
            clean.substring("torrent:".length)
        }
        val path = withoutPrefix.substringBefore('?')
        if ('/' in path) {
            path.substringAfter('/').trim().toIntOrNull()
        } else null
    } else {
        Regex("""[?&](?:ind(?:ex|ices)|fileidx|so)=([0-9]+)""", RegexOption.IGNORE_CASE)
            .find(clean)?.groupValues?.get(1)?.toIntOrNull()
    } ?: defaultFileIdx

    val parsedSeason = Regex("""[?&]season=([0-9]+)""", RegexOption.IGNORE_CASE)
        .find(clean)?.groupValues?.get(1)?.toIntOrNull() ?: defaultSeason

    val parsedEpisode = Regex("""[?&]episode=([0-9]+)""", RegexOption.IGNORE_CASE)
        .find(clean)?.groupValues?.get(1)?.toIntOrNull() ?: defaultEpisode

    val filename = Regex("""[?&]dn=([^&]+)""").find(clean)?.groupValues?.get(1)?.let {
        try {
            java.net.URLDecoder.decode(it, "UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    val parsedTrackers = Regex("""[?&]tr=([^&]+)""").findAll(clean).mapNotNull {
        try {
            java.net.URLDecoder.decode(it.groupValues[1], "UTF-8")
        } catch (_: Exception) {
            null
        }
    }.toList()

    val combinedTrackers = (trackers + parsedTrackers).filter(String::isNotBlank).distinct()

    return P2pStreamRequest(
        infoHash = canonicalHash,
        fileIdx = fileIdx,
        filename = filename,
        trackers = combinedTrackers,
        season = parsedSeason,
        episode = parsedEpisode
    )
}

fun extractP2pStreamRequest(
    rawUrl: String?,
    defaultFileIdx: Int? = null,
    defaultSeason: Int? = null,
    defaultEpisode: Int? = null,
    trackers: List<String> = emptyList(),
): P2pStreamRequest? = extractP2pStreamRequest(
    rawUrl = rawUrl,
    fallbackInfoHash = null,
    defaultFileIdx = defaultFileIdx,
    defaultSeason = defaultSeason,
    defaultEpisode = defaultEpisode,
    trackers = trackers
)

