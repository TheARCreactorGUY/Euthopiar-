package com.euthopiar.ui.player

import android.net.Uri
import com.euthopiar.core.addons.AddonRepository
import com.euthopiar.core.addons.ManagedAddon
import com.euthopiar.core.addons.buildAddonResourceUrl
import com.euthopiar.core.model.SubtitleTrack
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.util.CharsetNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * ExternalSubtitleEngine: Dynamic Stremio & OpenSubtitles provider broker.
 *
 * Concurrently queries all installed subtitle addons (e.g. OpenSubtitles v3, SubDL, etc.)
 * via their manifest endpoints using buildAddonResourceUrl with videoHash, videoSize,
 * and filename support, with BCP-47 language normalization and charset auto-transcoding.
 */
object ExternalSubtitleEngine {

    private val httpClient: OkHttpClient by lazy {
        DohDns.createOkHttpClient(connectTimeoutSeconds = 8, readTimeoutSeconds = 12)
    }

    /**
     * Search installed subtitle addons and external providers for subtitles matching metadata.
     * Returns a curated, deduplicated list of [SubtitleTrack].
     */
    suspend fun searchSubtitles(
        title: String,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        imdbId: String? = null,
        videoHash: String? = null,
        videoSize: Long? = null,
        filename: String? = null
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SubtitleTrack>()

        // 1. Resolve IMDb ID if not already provided or if title contains it
        val resolvedImdb = resolveImdbId(title, imdbId, season, episode)

        // 2. Concurrently query all installed subtitle addons
        if (!resolvedImdb.isNullOrBlank()) {
            val addonTracks = queryInstalledSubtitleAddons(
                imdbId = resolvedImdb,
                season = season,
                episode = episode,
                videoHash = videoHash,
                videoSize = videoSize,
                filename = filename
            )
            results.addAll(addonTracks)
        }

        // 3. Fallback to OpenSubtitles REST API if fewer than 2 subtitles found
        if (results.size < 2) {
            try {
                val openSubsRest = queryOpenSubtitlesRest(title, season, episode, year, resolvedImdb)
                results.addAll(openSubsRest)
            } catch (t: Throwable) {
                android.util.Log.d("ExternalSubtitleEngine", "OpenSubtitles REST failed: ${t.message}")
            }
        }

        // 4. Fallback to SubDL API if still needed
        if (results.size < 2) {
            try {
                val subDlResult = com.euthopiar.core.subtitle.external.SubDlClient.getInstance()
                    .searchSubtitles(
                        imdbId = resolvedImdb,
                        season = season,
                        episode = episode,
                        type = if (season != null && episode != null) "tv" else "movie"
                    )
                subDlResult.getOrNull()?.let { results.addAll(it) }
            } catch (t: Throwable) {
                android.util.Log.d("ExternalSubtitleEngine", "SubDL search failed: ${t.message}")
            }
        }

        // Final deduplication by URL and cap at most 2 per language
        val finalTracks = mutableListOf<SubtitleTrack>()
        val langCounts = mutableMapOf<String, Int>()

        for (track in results.distinctBy { it.url }) {
            val baseLang = track.language.replace(Regex("""\s*\(Alt\)""", RegexOption.IGNORE_CASE), "").trim()
            val key = baseLang.lowercase()
            val count = langCounts.getOrDefault(key, 0)
            if (count < 2) {
                langCounts[key] = count + 1
                val label = if (count == 0) baseLang else "$baseLang (Alt)"
                finalTracks.add(track.copy(language = label))
            }
        }

        finalTracks.sortedWith(
            compareByDescending<SubtitleTrack> { it.language.contains("english", ignoreCase = true) }
                .thenBy { it.language }
        )
    }

    private suspend fun queryInstalledSubtitleAddons(
        imdbId: String,
        season: Int?,
        episode: Int?,
        videoHash: String?,
        videoSize: Long?,
        filename: String?
    ): List<SubtitleTrack> = coroutineScope {
        val targetType = if (season != null && episode != null) "series" else "movie"
        val targetId = if (season != null && episode != null) "$imdbId:$season:$episode" else imdbId

        val addonRepo = AddonRepository.getInstanceOrNull()
        val installedAddons = addonRepo?.getEnabledAddonsForResource("subtitles", targetType, targetId).orEmpty()

        val activeAddons = installedAddons
        if (activeAddons.isEmpty()) return@coroutineScope emptyList()

        val extraParts = mutableListOf<String>()
        if (!videoHash.isNullOrBlank()) extraParts.add("videoHash=$videoHash")
        if (videoSize != null && videoSize > 0) extraParts.add("videoSize=$videoSize")
        if (!filename.isNullOrBlank()) {
            try {
                extraParts.add("filename=${URLEncoder.encode(filename, "UTF-8")}")
            } catch (_: Exception) {}
        }
        val extraPathSegment = if (extraParts.isNotEmpty()) extraParts.joinToString("&") else null

        val deferred = activeAddons.map { addon ->
            async(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(8000L) {
                        val url = buildAddonResourceUrl(
                            manifestUrl = addon.manifestUrl,
                            resource = "subtitles",
                            type = targetType,
                            id = targetId,
                            extraPathSegment = extraPathSegment
                        )

                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .header("Accept", "application/json")
                            .build()

                        httpClient.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@withTimeoutOrNull emptyList<SubtitleTrack>()
                            val body = response.body?.string() ?: return@withTimeoutOrNull emptyList<SubtitleTrack>()
                            val root = JSONObject(body)
                            val subsArray = root.optJSONArray("subtitles") ?: return@withTimeoutOrNull emptyList<SubtitleTrack>()

                            val tracks = mutableListOf<SubtitleTrack>()
                            val langCounts = mutableMapOf<String, Int>()

                            for (i in 0 until subsArray.length()) {
                                val obj = subsArray.optJSONObject(i) ?: continue
                                val subUrl = obj.optString("url").trim()
                                if (subUrl.isBlank() || (!subUrl.startsWith("http://") && !subUrl.startsWith("https://"))) {
                                    continue
                                }

                                val rawLang = listOf("lang", "language", "languageCode", "locale")
                                    .firstNotNullOfOrNull { obj.optString(it).takeIf { s -> s.isNotBlank() } } ?: "eng"

                                val bcp47 = iso6392ToBcp47(rawLang)
                                val langName = bcp47ToLanguageName(bcp47)

                                val count = langCounts.getOrDefault(langName.lowercase(), 0)
                                if (count < 2) {
                                    langCounts[langName.lowercase()] = count + 1
                                    val label = if (count == 0) langName else "$langName (Alt)"
                                    tracks.add(SubtitleTrack(url = subUrl, language = label))
                                }
                            }
                            tracks
                        }
                    } ?: emptyList()
                } catch (t: Throwable) {
                    android.util.Log.d("ExternalSubtitleEngine", "Subtitle addon query failed for ${addon.displayTitle}: ${t.message}")
                    emptyList()
                }
            }
        }

        deferred.awaitAll().flatten()
    }

    private suspend fun resolveImdbId(
        title: String,
        imdbId: String?,
        season: Int?,
        episode: Int?
    ): String? {
        val directImdb = imdbId?.trim()?.takeIf { it.startsWith("tt") }
            ?: Regex("""tt\d+""").find(title)?.value
        if (directImdb != null) return directImdb

        val cleanTitle = cleanSearchTitle(title)
        if (cleanTitle.isBlank()) return null

        val isTv = season != null && episode != null
        val cat = if (isTv) "series" else "movie"
        val queryUrl = "https://v3-cinemeta.strem.io/catalog/$cat/top/search=${URLEncoder.encode(cleanTitle, "UTF-8")}.json"

        try {
            val request = Request.Builder()
                .url(queryUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JSONObject(body)
                    val metas = root.optJSONArray("metas")
                    if (metas != null && metas.length() > 0) {
                        val found = metas.getJSONObject(0).optString("imdb_id").takeIf { it.startsWith("tt") }
                        if (found != null) return found
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: search TMDB to get canonical imdb_id
        try {
            val tmdbId = com.euthopiar.core.util.TmdbBridge.searchTmdbId(
                client = httpClient,
                title = cleanTitle,
                year = null,
                isTv = isTv
            )
            if (!tmdbId.isNullOrBlank()) {
                val endpoint = if (isTv) "tv" else "movie"
                val extUrl = "https://api.themoviedb.org/3/$endpoint/$tmdbId/external_ids?api_key=${com.euthopiar.core.util.TmdbBridge.DEFAULT_API_KEY}"
                val req = Request.Builder().url(extUrl).header("User-Agent", "Mozilla/5.0").build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val found = JSONObject(body).optString("imdb_id").takeIf { it.startsWith("tt") }
                        if (found != null) return found
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun queryOpenSubtitlesRest(
        title: String,
        season: Int?,
        episode: Int?,
        year: Int?,
        imdbId: String?
    ): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()
        val cleanTitle = cleanSearchTitle(title)
        val encodedQuery = URLEncoder.encode(cleanTitle, "UTF-8")
        val cleanImdb = imdbId?.replace("tt", "")?.trim()?.takeIf { it.isNotBlank() && it.all { c -> c.isDigit() } }

        val path = when {
            cleanImdb != null && season != null && episode != null ->
                "imdbid-$cleanImdb/season-$season/episode-$episode/sublanguageid-all"
            cleanImdb != null ->
                "imdbid-$cleanImdb/sublanguageid-all"
            season != null && episode != null ->
                "query-$encodedQuery/season-$season/episode-$episode/sublanguageid-all"
            year != null ->
                "query-$encodedQuery/year-$year/sublanguageid-all"
            else ->
                "query-$encodedQuery/sublanguageid-all"
        }

        val request = Request.Builder()
            .url("https://rest.opensubtitles.org/search/$path")
            .header("User-Agent", "TemporaryUserAgent")
            .header("X-User-Agent", "TemporaryUserAgent")
            .header("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = JSONArray(body)
            val langCounts = mutableMapOf<String, Int>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val downloadLink = obj.optString("SubDownloadLink").trim()
                val rawLang = obj.optString("SubLanguageID", "eng").ifBlank { obj.optString("LanguageName", "English") }
                val bcp47 = iso6392ToBcp47(rawLang)
                val language = bcp47ToLanguageName(bcp47)

                val currentCount = langCounts.getOrDefault(language.lowercase(), 0)
                if (currentCount >= 2) continue

                if (downloadLink.isNotBlank() && downloadLink.startsWith("http")) {
                    langCounts[language.lowercase()] = currentCount + 1
                    val label = if (currentCount == 0) language else "$language (Alt)"
                    tracks.add(SubtitleTrack(url = downloadLink, language = label))
                }
            }
        }
        return tracks
    }

    private fun cleanSearchTitle(raw: String): String {
        var s = raw.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), " ")
        s = s.replace(Regex("""(?i)\b(?:4k|2160p|1080p|720p|480p|uhd|fhd|hd|bluray|blu-ray|web-dl|webrip|brrip|dvdrip|hdtv|remux|hevc|x265|x264|avc|10bit|hdr|dovi|sdr|aac|atmos|ddp|truehd|multi|dual\s*audio|hindi|english|season\s*\d+|s\d+e\d+|series)\b"""), " ")
        s = s.replace(Regex("[^a-zA-Z0-9 ]"), " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Maps ISO 639-2 three-letter language codes to standard BCP-47 two-letter codes.
     */
    fun iso6392ToBcp47(code: String): String {
        val clean = code.lowercase().trim().replace('_', '-')
        return when (clean) {
            "eng", "en" -> "en"
            "spa", "es" -> "es"
            "spl", "es-419", "es-la", "es-lat" -> "es-419"
            "fre", "fra", "fr" -> "fr"
            "ger", "deu", "de" -> "de"
            "ita", "it" -> "it"
            "por", "pt" -> "pt"
            "pob", "pt-br", "pt_br" -> "pt-br"
            "rus", "ru" -> "ru"
            "ara", "ar" -> "ar"
            "hin", "hi" -> "hi"
            "chi", "zho", "zh" -> "zh"
            "zht", "zh-tw" -> "zh-tw"
            "zhs", "zh-cn" -> "zh-cn"
            "jpn", "ja" -> "ja"
            "kor", "ko" -> "ko"
            "tur", "tr" -> "tr"
            "ind", "id" -> "id"
            "vie", "vi" -> "vi"
            "pol", "pl" -> "pl"
            "dut", "nld", "nl" -> "nl"
            "swe", "sv" -> "sv"
            "dan", "da" -> "da"
            "fin", "fi" -> "fi"
            "nor", "no" -> "no"
            "ell", "gre", "el" -> "el"
            "heb", "he" -> "he"
            "tha", "th" -> "th"
            "ces", "cze", "cs" -> "cs"
            "hun", "hu" -> "hu"
            "ron", "rum", "ro" -> "ro"
            "ukr", "uk" -> "uk"
            "tam", "ta" -> "ta"
            "tel", "te" -> "te"
            "kan", "kn" -> "kn"
            "mal", "ml" -> "ml"
            "mar", "mr" -> "mr"
            "ben", "bn" -> "bn"
            "urd", "ur" -> "ur"
            "fas", "per", "fa" -> "fa"
            else -> if (clean.length == 2 || clean.contains("-")) clean else clean.take(2)
        }
    }

    /**
     * Converts BCP-47 or ISO code to friendly English language display name.
     */
    fun bcp47ToLanguageName(code: String): String {
        val bcp47 = iso6392ToBcp47(code)
        return when (bcp47) {
            "en" -> "English"
            "es" -> "Spanish"
            "es-419" -> "Spanish (Latin America)"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "pt-br" -> "Portuguese (BR)"
            "ru" -> "Russian"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            "zh" -> "Chinese"
            "zh-tw" -> "Chinese (Traditional)"
            "zh-cn" -> "Chinese (Simplified)"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "tr" -> "Turkish"
            "id" -> "Indonesian"
            "vi" -> "Vietnamese"
            "pl" -> "Polish"
            "nl" -> "Dutch"
            "sv" -> "Swedish"
            "da" -> "Danish"
            "fi" -> "Finnish"
            "no" -> "Norwegian"
            "el" -> "Greek"
            "he" -> "Hebrew"
            "th" -> "Thai"
            "cs" -> "Czech"
            "hu" -> "Hungarian"
            "ro" -> "Romanian"
            "uk" -> "Ukrainian"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "kn" -> "Kannada"
            "ml" -> "Malayalam"
            "mr" -> "Marathi"
            "bn" -> "Bengali"
            "ur" -> "Urdu"
            "fa" -> "Persian"
            else -> code.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
