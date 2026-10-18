package com.euthopiar.core.streams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Pre-Playback Audio Language Discovery Engine (3 Tiers).
 *
 * Tier 1 (Zero-IO, 0ms): Parse Unicode flag emojis, scene tokens (DUAL, MULTI, HIN, LAT, TAM, TEL, VOSTFR),
 * and expand DUAL using TMDB original_language.
 * Tier 2 (HLS Master Playlist Probe): Fetch .m3u8 master playlist (<32KB text), parse #EXT-X-MEDIA:TYPE=AUDIO,
 * and cache in-memory for 60s.
 * Tier 3 (Range Probe): For direct progressive MP4/MKV streams, probe Range 0-65535 parsing MKV EBML tracks or MP4 moov/mdhd.
 */
object AudioLanguageDiscovery {

    private val hlsCache = ConcurrentHashMap<String, Pair<Long, List<String>>>()
    private const val HLS_CACHE_TTL_MS = 60_000L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val flagEmojiMap = mapOf(
        "🇬🇧" to "ENG", "🇺🇸" to "ENG",
        "🇯🇵" to "JPN",
        "🇪🇸" to "SPA", "🇲🇽" to "LAT", "🇦🇷" to "LAT",
        "🇮🇳" to "HIN",
        "🇫🇷" to "FRA",
        "🇩🇪" to "GER",
        "🇮🇹" to "ITA",
        "🇰🇷" to "KOR",
        "🇷🇺" to "RUS",
        "🇧🇷" to "POR", "🇵🇹" to "POR",
        "🇨🇳" to "CHI", "🇹🇼" to "CHI",
        "🇹🇷" to "TUR",
        "🇵🇱" to "POL",
        "🇳🇱" to "DUT",
        "🇸🇪" to "SWE",
        "🇩🇰" to "DAN",
        "🇳🇴" to "NOR",
        "🇫🇮" to "FIN",
        "🇹🇭" to "THA",
        "🇻🇳" to "VIE",
        "🇮🇩" to "IND",
        "🇦🇪" to "ARA", "🇸🇦" to "ARA"
    )

    private val iso2ToIso3 = mapOf(
        "en" to "ENG", "eng" to "ENG",
        "ja" to "JPN", "jpn" to "JPN",
        "es" to "SPA", "spa" to "SPA",
        "hi" to "HIN", "hin" to "HIN",
        "ta" to "TAM", "tam" to "TAM",
        "te" to "TEL", "tel" to "TEL",
        "fr" to "FRA", "fra" to "FRA", "fre" to "FRA",
        "de" to "GER", "ger" to "GER", "deu" to "GER",
        "it" to "ITA", "ita" to "ITA",
        "ko" to "KOR", "kor" to "KOR",
        "ru" to "RUS", "rus" to "RUS",
        "pt" to "POR", "por" to "POR",
        "zh" to "CHI", "chi" to "CHI", "zho" to "CHI",
        "tr" to "TUR", "tur" to "TUR",
        "pl" to "POL", "pol" to "POL",
        "nl" to "DUT", "dut" to "DUT", "nld" to "DUT",
        "ar" to "ARA", "ara" to "ARA",
        "lat" to "LAT"
    )

    /**
     * Tier 1 (Zero-IO, 0ms): Parses metadata (name, title, description) for language cues.
     */
    fun parseAudioLanguagesFromMetadata(
        text: String,
        originalLanguage: String? = null
    ): List<String> {
        if (text.isBlank()) return emptyList()
        val found = linkedSetOf<String>()

        // 1. Unicode Regional Flag Emojis
        flagEmojiMap.forEach { (emoji, lang) ->
            if (text.contains(emoji)) {
                found.add(lang)
            }
        }

        val upper = text.uppercase()

        // 2. Scene Tokens
        if (Regex("""\bMULTI\b""").containsMatchIn(upper)) {
            found.add("MULTI")
        }
        if (Regex("""\bVOSTFR\b""").containsMatchIn(upper)) {
            found.add("VOSTFR")
        }
        if (Regex("""\b(?:LAT|LATINO)\b""").containsMatchIn(upper)) {
            found.add("LAT")
        }
        if (Regex("""\b(?:HIN|HINDI)\b""").containsMatchIn(upper)) {
            found.add("HIN")
        }
        if (Regex("""\b(?:TAM|TAMIL)\b""").containsMatchIn(upper)) {
            found.add("TAM")
        }
        if (Regex("""\b(?:TEL|TELUGU)\b""").containsMatchIn(upper)) {
            found.add("TEL")
        }
        if (Regex("""\b(?:ENG|ENGLISH)\b""").containsMatchIn(upper)) {
            found.add("ENG")
        }
        if (Regex("""\b(?:JPN|JAPANESE)\b""").containsMatchIn(upper)) {
            found.add("JPN")
        }
        if (Regex("""\b(?:SPA|SPANISH|ESP)\b""").containsMatchIn(upper)) {
            found.add("SPA")
        }
        if (Regex("""\b(?:FRA|FRENCH)\b""").containsMatchIn(upper)) {
            found.add("FRA")
        }
        if (Regex("""\b(?:GER|GERMAN|DEU)\b""").containsMatchIn(upper)) {
            found.add("GER")
        }
        if (Regex("""\b(?:ITA|ITALIAN)\b""").containsMatchIn(upper)) {
            found.add("ITA")
        }
        if (Regex("""\b(?:KOR|KOREAN)\b""").containsMatchIn(upper)) {
            found.add("KOR")
        }
        if (Regex("""\b(?:RUS|RUSSIAN)\b""").containsMatchIn(upper)) {
            found.add("RUS")
        }
        if (Regex("""\b(?:POR|PORTUGUESE)\b""").containsMatchIn(upper)) {
            found.add("POR")
        }

        // 3. DUAL Token expansion using TMDB original_language
        if (Regex("""\bDUAL\b""").containsMatchIn(upper)) {
            val orig = originalLanguage?.lowercase()?.trim()
            val mappedOrig = orig?.let { iso2ToIso3[it] ?: it.uppercase() }
            if (mappedOrig != null && mappedOrig != "ENG") {
                found.add(mappedOrig)
                found.add("ENG")
            } else if (!found.contains("DUAL") && !found.contains("MULTI")) {
                found.add("DUAL")
            }
        }

        return found.toList()
    }

    /**
     * Tier 2 (HLS Master Playlist Probe): Fetches master playlist (<32KB text) in background,
     * parses #EXT-X-MEDIA:TYPE=AUDIO, and caches for 60s.
     */
    suspend fun probeHlsAudioTracks(
        m3u8Url: String,
        headers: Map<String, String> = emptyMap()
    ): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = hlsCache[m3u8Url]
        if (cached != null && now - cached.first < HLS_CACHE_TTL_MS) {
            return@withContext cached.second
        }

        try {
            val reqBuilder = Request.Builder().url(m3u8Url)
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            val req = reqBuilder.build()

            val response = httpClient.newCall(req).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext emptyList()
            }

            val body = response.body ?: return@withContext emptyList()
            val text = body.string()
            if (!text.contains("#EXTM3U")) {
                return@withContext emptyList()
            }

            val languages = linkedSetOf<String>()
            val regex = Regex("""#EXT-X-MEDIA:TYPE=AUDIO.*?LANGUAGE="([^"]+)"""", RegexOption.IGNORE_CASE)
            regex.findAll(text).forEach { match ->
                val rawLang = match.groupValues.getOrNull(1)?.lowercase()?.trim()
                if (!rawLang.isNullOrBlank()) {
                    val norm = iso2ToIso3[rawLang] ?: rawLang.uppercase()
                    languages.add(norm)
                }
            }

            val list = languages.toList()
            hlsCache[m3u8Url] = now to list
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Tier 3 (Range Probe): For direct progressive MP4/MKV streams.
     * Executes Range: bytes=0-65535 probe parsing MKV EBML tracks or MP4 moov -> trak -> mdhd.
     * Never probes unresolved torrent magnets or Debrid links.
     */
    suspend fun probeContainerAudioTracks(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): List<String> = withContext(Dispatchers.IO) {
        val urlLower = url.lowercase()
        // Safety guard: Never probe unresolved torrent magnets or Debrid unrestrict links
        if (urlLower.startsWith("magnet:") || urlLower.startsWith("torrent:") ||
            urlLower.contains("real-debrid.com") || urlLower.contains("alldebrid.com") ||
            urlLower.contains("torbox.app") || urlLower.contains("127.0.0.1") || urlLower.contains("localhost")
        ) {
            return@withContext emptyList()
        }

        val isMp4 = urlLower.endsWith(".mp4") || urlLower.contains(".mp4?")
        val isMkv = urlLower.endsWith(".mkv") || urlLower.contains(".mkv?") || urlLower.contains("matroska")
        if (!isMp4 && !isMkv) {
            return@withContext emptyList()
        }

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-65535")
            headers.forEach { (k, v) -> if (!k.equals("Range", ignoreCase = true)) reqBuilder.header(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 206) {
                    val bytes = resp.body?.bytes() ?: return@withContext emptyList()
                    if (isMkv) {
                        return@withContext parseMkvAudioLanguages(bytes)
                    } else if (isMp4) {
                        return@withContext parseMp4AudioLanguages(bytes)
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    private fun parseMkvAudioLanguages(data: ByteArray): List<String> {
        val found = linkedSetOf<String>()
        // Search for Matroska Track Language EBML tag 0x22 0xB5 0x9C
        for (i in 0 until data.size - 6) {
            if (data[i] == 0x22.toByte() && data[i + 1] == 0xB5.toByte() && data[i + 2] == 0x9C.toByte()) {
                val len = data[i + 3].toInt() and 0x7F
                if (len in 2..4 && i + 4 + len <= data.size) {
                    val langStr = String(data, i + 4, len, Charsets.US_ASCII).trim().lowercase()
                    val norm = iso2ToIso3[langStr] ?: langStr.uppercase()
                    if (norm.length == 3 && norm.all { it in 'A'..'Z' }) {
                        found.add(norm)
                    }
                }
            }
        }
        return found.toList()
    }

    private fun parseMp4AudioLanguages(data: ByteArray): List<String> {
        val found = linkedSetOf<String>()
        // Search for 'mdhd' box
        for (i in 0 until data.size - 32) {
            if (data[i] == 'm'.code.toByte() && data[i + 1] == 'd'.code.toByte() &&
                data[i + 2] == 'h'.code.toByte() && data[i + 3] == 'd'.code.toByte()
            ) {
                val version = data[i + 4].toInt() and 0xFF
                val langOffset = if (version == 1) i + 28 else i + 20
                if (langOffset + 2 <= data.size) {
                    val b1 = data[langOffset].toInt() and 0xFF
                    val b2 = data[langOffset + 1].toInt() and 0xFF
                    val packed = (b1 shl 8) or b2
                    val c1 = ((packed shr 10) and 0x1F) + 0x60
                    val c2 = ((packed shr 5) and 0x1F) + 0x60
                    val c3 = (packed and 0x1F) + 0x60
                    if (c1 in 'a'.code..'z'.code && c2 in 'a'.code..'z'.code && c3 in 'a'.code..'z'.code) {
                        val langCode = "${c1.toChar()}${c2.toChar()}${c3.toChar()}"
                        val norm = iso2ToIso3[langCode] ?: langCode.uppercase()
                        if (norm != "UND") {
                            found.add(norm)
                        }
                    }
                }
            }
        }
        return found.toList()
    }

    /**
     * Formats discovered languages into clean text badge notation:
     * e.g. [ENG · JPN] or [MULTI] or [ENG] • [MULTI]
     */
    fun formatLanguageBadges(languages: List<String>): String? {
        if (languages.isEmpty()) return null
        val distinct = languages.distinct()
        val hasMulti = distinct.contains("MULTI")
        val cleanLangs = distinct.filterNot { it == "MULTI" || it == "DUAL" }

        val parts = mutableListOf<String>()
        if (cleanLangs.isNotEmpty()) {
            parts.add("[${cleanLangs.joinToString(" · ")}]")
        }
        if (hasMulti || (distinct.contains("DUAL") && cleanLangs.isEmpty())) {
            parts.add(if (hasMulti) "[MULTI]" else "[DUAL]")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
    }

    fun buildFullBadge(resolutionLabel: String, languages: List<String>): String {
        val langBadge = formatLanguageBadges(languages)
        return if (langBadge != null) {
            "$resolutionLabel • $langBadge"
        } else {
            resolutionLabel
        }
    }
}
