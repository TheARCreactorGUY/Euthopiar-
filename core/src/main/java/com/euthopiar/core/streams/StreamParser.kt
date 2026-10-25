package com.euthopiar.core.streams

import com.euthopiar.core.model.StreamContainer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

object StreamParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        payload: String,
        addonName: String,
        addonId: String,
        addonLogo: String? = null,
        originalLanguage: String? = null
    ): List<StreamItem> {
        val root = try {
            json.parseToJsonElement(payload).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }

        val streamsArray = root["streams"] as? JsonArray ?: return emptyList()
        return streamsArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val url = obj.string("url")?.trim()
            val infoHash = obj.string("infoHash")?.trim()
            val externalUrl = obj.string("externalUrl")?.trim()
            val ytId = obj.string("ytId")?.trim()?.takeIf { it.isNotEmpty() }

            val hintsObj = obj["behaviorHints"] as? JsonObject

            // Unpack clientResolve from stream item or behaviorHints
            val clientResolveObj = (obj["clientResolve"] as? JsonObject)
                ?: (hintsObj?.get("clientResolve") as? JsonObject)
            val crInfoHash = clientResolveObj?.string("infoHash")?.trim()
            val crMagnetUri = (clientResolveObj?.string("magnetUri") ?: clientResolveObj?.string("magnet"))?.trim()
            val crFileIdx = clientResolveObj?.int("fileIdx")
            val crIsCached = clientResolveObj?.boolean("isCached") ?: obj.boolean("isCached") ?: false

            val effectiveInfoHash = infoHash ?: crInfoHash
            val effectiveFileIdx = obj.int("fileIdx") ?: crFileIdx

            if (url == null && effectiveInfoHash == null && externalUrl == null && ytId == null && crMagnetUri == null) {
                return@mapNotNull null
            }

            val titleStr = obj.string("title")
            val nameStr = obj.string("name")
            val descStr = obj.string("description") ?: titleStr

            // Hoist proxyHeaders from behaviorHints (handles both nested request map and flat map)
            val proxyHeaders = hintsObj?.get("proxyHeaders")?.let { ph ->
                when (ph) {
                    is JsonObject -> {
                        val req = (ph["request"] as? JsonObject) ?: ph
                        req.mapNotNull { (k, v) ->
                            val valStr = (v as? JsonPrimitive)?.contentOrNull
                            if (valStr != null) k to valStr else null
                        }.toMap()
                    }
                    else -> emptyMap()
                }
            } ?: emptyMap()

            val responseHeaders = hintsObj?.get("proxyHeaders")?.let { ph ->
                if (ph is JsonObject) {
                    (ph["response"] as? JsonObject)?.mapNotNull { (k, v) ->
                        val valStr = (v as? JsonPrimitive)?.contentOrNull
                        if (valStr != null) k to valStr else null
                    }?.toMap() ?: emptyMap()
                } else emptyMap()
            } ?: emptyMap()

            val filename = hintsObj?.string("filename") ?: obj.string("filename")

            val rawSources = (obj["sources"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() } ?: emptyList()
            val rawTrackers = (obj["trackers"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() } ?: emptyList()
            val combinedTrackers = (rawSources + rawTrackers)
                .map { it.removePrefix("tracker:").trim() }
                .filter { it.isNotBlank() }
                .distinct()

            val parsedSubtitles = (obj["subtitles"] as? JsonArray)?.mapNotNull { subElem ->
                val subObj = subElem as? JsonObject ?: return@mapNotNull null
                val subUrl = subObj.string("url")?.trim() ?: return@mapNotNull null
                val subLang = subObj.string("lang") ?: subObj.string("language") ?: "English"
                val subId = subObj.string("id") ?: subObj.string("name")
                StreamSubtitle(
                    url = subUrl,
                    language = subLang,
                    name = subId
                )
            } ?: emptyList()

            val (cleanUrl, combinedHeaders) = if (!url.isNullOrBlank()) {
                com.euthopiar.core.network.PlayerPlaybackNetworking.extractUrlAndInlineHeaders(url, proxyHeaders)
            } else {
                Pair(url, proxyHeaders)
            }

            val rawType = obj.string("type")
            val rawFormat = obj.string("format")
            val targetForExt = "${filename.orEmpty()} ${cleanUrl.orEmpty()}".lowercase()

            val (inferredStreamType, inferredContainer) = when {
                effectiveInfoHash != null || url?.startsWith("magnet:", ignoreCase = true) == true || crMagnetUri != null ||
                        rawType?.equals("torrent", ignoreCase = true) == true || rawType?.equals("p2p", ignoreCase = true) == true -> {
                    Pair("torrent", StreamContainer.P2P)
                }
                targetForExt.endsWith(".mkv") || rawType?.equals("mkv", ignoreCase = true) == true || rawFormat?.equals("mkv", ignoreCase = true) == true -> {
                    Pair("mkv", StreamContainer.MKV)
                }
                targetForExt.endsWith(".m3u8") || targetForExt.contains("/hls/") || targetForExt.contains("format=m3u8") ||
                        rawType?.equals("m3u8", ignoreCase = true) == true || rawType?.equals("hls", ignoreCase = true) == true -> {
                    Pair("m3u8", StreamContainer.HLS)
                }
                targetForExt.endsWith(".mpd") || targetForExt.contains("format=mpd") ||
                        rawType?.equals("mpd", ignoreCase = true) == true || rawType?.equals("dash", ignoreCase = true) == true -> {
                    Pair("mpd", StreamContainer.DASH)
                }
                targetForExt.endsWith(".webm") || rawType?.equals("webm", ignoreCase = true) == true -> {
                    Pair("webm", StreamContainer.WEBM)
                }
                targetForExt.endsWith(".ts") || rawType?.equals("ts", ignoreCase = true) == true -> {
                    Pair("ts", StreamContainer.TS)
                }
                targetForExt.endsWith(".mp4") || rawType?.equals("mp4", ignoreCase = true) == true -> {
                    Pair("mp4", StreamContainer.MP4)
                }
                else -> {
                    Pair(rawType ?: "mp4", StreamContainer.MP4)
                }
            }

            // Quality parsing from name, title, description with DS4K and HDR fix
            val combinedLabel = "${nameStr.orEmpty()} ${titleStr.orEmpty()} ${descStr.orEmpty()}"
            val quality = run {
                val lower = combinedLabel.lowercase()
                when {
                    lower.contains("2160p") -> "4K 2160p"
                    lower.contains("1440p") || Regex("""\b2k\b""", RegexOption.IGNORE_CASE).containsMatchIn(combinedLabel) -> "1440p 2K"
                    lower.contains("1080p") || Regex("""\bfhd\b""", RegexOption.IGNORE_CASE).containsMatchIn(combinedLabel) -> "1080p FHD"
                    lower.contains("720p") -> "720p HD"
                    lower.contains("480p") || lower.contains("576p") || lower.contains("360p") || Regex("""\bsd\b""", RegexOption.IGNORE_CASE).containsMatchIn(combinedLabel) -> "480p SD"
                    else -> {
                        val stripped = combinedLabel
                            .replace(Regex("""(?i)\b(?:ds4k|4k\s*remaster(?:ed)?|hdr10\+?|hdr|hdrip|hdts|hdtv)\b"""), " ")
                        when {
                            Regex("""(?i)\b(?:4k|uhd|2160)\b""").containsMatchIn(stripped) -> "4K 2160p"
                            Regex("""(?i)\b(?:1080)\b""").containsMatchIn(stripped) -> "1080p FHD"
                            Regex("""(?i)\b(?:720|hd)\b""").containsMatchIn(stripped) -> "720p HD"
                            effectiveInfoHash != null || url?.startsWith("magnet:", ignoreCase = true) == true || crMagnetUri != null -> "Torrent"
                            else -> "Direct Stream"
                        }
                    }
                }
            }

            val parsedSeeders = obj.int("seeders") ?: obj.int("seeds")
                ?: clientResolveObj?.int("seeders") ?: clientResolveObj?.int("seeds")
                ?: Regex("""(?:👤|👥|seeds?:?|seeders?:?)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    .find(combinedLabel)?.groupValues?.get(1)?.toIntOrNull()

            val parsedPeers = obj.int("peers") ?: obj.int("leechers")
                ?: clientResolveObj?.int("peers") ?: clientResolveObj?.int("leechers")
                ?: Regex("""(?:leechers?:?|peers?:?)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    .find(combinedLabel)?.groupValues?.get(1)?.toIntOrNull()

            val parsedSize = Regex("""(?:💾|size:?|\b)(\d+(?:\.\d+)?\s*(?:GB|MB|GiB|MiB))\b""", RegexOption.IGNORE_CASE)
                .find(combinedLabel)?.groupValues?.get(1)

            val baseMagnet = if (!effectiveInfoHash.isNullOrBlank()) {
                "magnet:?xt=urn:btih:$effectiveInfoHash"
            } else if (url?.startsWith("magnet:", ignoreCase = true) == true) {
                url
            } else if (!crMagnetUri.isNullOrBlank()) {
                crMagnetUri
            } else null

            val computedMagnet = if (!baseMagnet.isNullOrBlank() && combinedTrackers.isNotEmpty()) {
                val builder = StringBuilder(baseMagnet)
                for (tr in combinedTrackers) {
                    val encodedTr = try { URLEncoder.encode(tr, "UTF-8") } catch (_: Throwable) { tr }
                    if (!baseMagnet.contains(tr, ignoreCase = true) && !baseMagnet.contains(encodedTr, ignoreCase = true)) {
                        val delim = if (builder.contains("?")) "&" else "?"
                        builder.append("${delim}tr=$encodedTr")
                    }
                }
                builder.toString()
            } else {
                baseMagnet
            }

            val parsedAudioLangs = AudioLanguageDiscovery.parseAudioLanguagesFromMetadata(combinedLabel, originalLanguage)

            StreamItem(
                name = nameStr ?: titleStr ?: addonName,
                title = titleStr,
                description = descStr,
                url = cleanUrl,
                infoHash = effectiveInfoHash,
                fileIdx = effectiveFileIdx,
                magnetUrl = computedMagnet,
                quality = quality,
                size = parsedSize,
                seeders = parsedSeeders,
                peers = parsedPeers,
                sourceName = addonName,
                addonName = addonName,
                addonId = addonId,
                streamType = inferredStreamType,
                headers = combinedHeaders,
                responseHeaders = responseHeaders,
                externalSubtitles = parsedSubtitles,
                isCached = crIsCached,
                audioLanguages = parsedAudioLangs,
                filename = filename,
                container = inferredContainer,
                trackers = combinedTrackers,
                subtitles = parsedSubtitles
            )
        }
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(name: String): Int? =
        this[name]?.jsonPrimitive?.let { it.intOrNull ?: it.contentOrNull?.toIntOrNull() }

    private fun JsonObject.boolean(name: String): Boolean? =
        this[name]?.jsonPrimitive?.booleanOrNull
}
