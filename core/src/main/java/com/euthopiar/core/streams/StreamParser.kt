package com.euthopiar.core.streams

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

            // Quality parsing from name, title, description
            val combinedLabel = "${nameStr.orEmpty()} ${titleStr.orEmpty()} ${descStr.orEmpty()}"
            val quality = when {
                combinedLabel.contains("2160", ignoreCase = true) || combinedLabel.contains("4k", ignoreCase = true) -> "4K 2160p"
                combinedLabel.contains("1440", ignoreCase = true) || combinedLabel.contains("2k", ignoreCase = true) -> "1440p 2K"
                combinedLabel.contains("1080", ignoreCase = true) || combinedLabel.contains("fhd", ignoreCase = true) -> "1080p FHD"
                combinedLabel.contains("720", ignoreCase = true) || combinedLabel.contains("hd", ignoreCase = true) -> "720p HD"
                combinedLabel.contains("480", ignoreCase = true) || combinedLabel.contains("sd", ignoreCase = true) -> "480p SD"
                effectiveInfoHash != null || url?.startsWith("magnet:", ignoreCase = true) == true || crMagnetUri != null -> "Torrent"
                else -> "Direct Stream"
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

            val computedMagnet = if (!effectiveInfoHash.isNullOrBlank()) {
                "magnet:?xt=urn:btih:$effectiveInfoHash"
            } else if (url?.startsWith("magnet:", ignoreCase = true) == true) {
                url
            } else if (!crMagnetUri.isNullOrBlank()) {
                crMagnetUri
            } else null

            val (cleanUrl, combinedHeaders) = if (!url.isNullOrBlank()) {
                com.euthopiar.core.network.PlayerPlaybackNetworking.extractUrlAndInlineHeaders(url, proxyHeaders)
            } else {
                Pair(url, proxyHeaders)
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
                streamType = obj.string("type"),
                headers = combinedHeaders,
                responseHeaders = responseHeaders,
                isCached = crIsCached,
                audioLanguages = parsedAudioLangs
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
