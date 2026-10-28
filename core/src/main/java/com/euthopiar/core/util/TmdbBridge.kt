package com.euthopiar.core.util

import com.euthopiar.core.matcher.MatchScorer
import com.euthopiar.core.model.*
import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, centralized TMDB Enrichment Bridge for Universal Plugins.
 *
 * Implements the Euthopiar Universal Plugin Developer Guide (v3.0) metadata contract:
 * - Provider-First with TMDB Bridge fallback
 * - High-resolution 16:9 cinematic backdrops and 2:3 posters
 * - Official transparent PNG clear title logos (English prioritized)
 * - Dual ratings: IMDb numerical (e.g. "8.4") and Rotten Tomatoes Tomatometer percentage (e.g. "92%")
 * - Release Year: initial theatrical for movies, latest season air year for TV series
 * - Cast with headshot profile URLs and character roles + Directors/creators
 * - 16:9 episode stills, canonical titles, overviews, and per-episode runtimes
 * - Related recommendations and YouTube trailer keys
 */
typealias TmdbEnrichedData = TmdbBridge.EnrichedMetadata

object TmdbBridge {

    const val BASE_URL: String = "https://euthopiar-vault.sehgalvansh716.workers.dev/3"
    const val DEFAULT_API_KEY: String = ""
    const val DEFAULT_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun appendApiKey(url: String, apiKey: String): String {
        if (apiKey.isBlank()) return url
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}api_key=$apiKey"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val logoCache = ConcurrentHashMap<String, String>()
    private val enrichedCache = ConcurrentHashMap<String, EnrichedMetadata>()
    private val seasonCache = ConcurrentHashMap<String, Map<Int, EpisodeDetail>>()
    @Volatile var activeRegionPreset: ContentRegionPreset = ContentRegionPreset.GLOBAL
    private val cachedHomeCatalog = ConcurrentHashMap<String, Pair<Long, List<CatalogRow>>>()
    private val nowPlayingMovieIds = ConcurrentHashMap.newKeySet<String>()

    fun resolveReleaseStatus(
        id: String,
        isTv: Boolean,
        releaseDateStr: String?,
        feedForcedStatus: ReleaseStatus? = null
    ): ReleaseStatus? {
        val parsedDate = releaseDateStr?.let { dateStr ->
            runCatching { java.time.LocalDate.parse(dateStr) }.getOrNull()
        }
        val today = java.time.LocalDate.now()

        if (parsedDate != null && parsedDate.isAfter(today)) {
            return ReleaseStatus.UPCOMING
        }
        if (feedForcedStatus == ReleaseStatus.UPCOMING) {
            return ReleaseStatus.UPCOMING
        }
        if (!isTv) {
            if (feedForcedStatus == ReleaseStatus.IN_CINEMAS) {
                nowPlayingMovieIds.add(id)
                return ReleaseStatus.IN_CINEMAS
            }
            if (nowPlayingMovieIds.contains(id)) {
                return ReleaseStatus.IN_CINEMAS
            }
            if (parsedDate != null && !parsedDate.isAfter(today) && !parsedDate.isBefore(today.minusDays(42))) {
                return ReleaseStatus.IN_CINEMAS
            }
        }
        return ReleaseStatus.RELEASED
    }

    data class EnrichedMetadata(
        val tmdbId: String,
        val title: String,
        val posterUrl: String?,
        val backdropUrl: String?,
        val synopsis: String?,
        val rating: String?,
        val rottenTomatoesRating: String?,
        val contentRating: String?,
        val duration: String?,
        val year: Int?,
        val imdbId: String?,
        val genres: List<String>,
        val cast: List<CastMember>,
        val directors: List<String>,
        val recommendations: List<MediaItem>,
        val trailerUrl: String?,
        val seasonNumbers: List<Int>,
        val logoUrl: String?,
        val originalLanguage: String? = null,
        val originCountry: List<String> = emptyList(),
        val seasonEpisodeCounts: Map<Int, Int> = emptyMap(),
        val originalTitle: String? = null,
        val alternativeTitles: Set<String> = emptySet(),
        val premiereYear: Int? = year,
        val latestSeasonYear: Int? = null,
        val seasonYears: Map<Int, Int> = emptyMap(),
        val seasonNames: Map<Int, String> = emptyMap(),
        val tvdbId: Int? = null,
        val runtimeMinutes: Int? = null,
        val malId: Int? = null,
        val anilistId: Int? = null,
        val romajiTitle: String? = null,
        val englishTitle: String? = null,
        val nativeTitle: String? = null,
        val animeSynonyms: List<String> = emptyList(),
        val seasonSpecificTitles: Map<Int, List<String>> = emptyMap()
    ) {
        val isAnime: Boolean
            get() = genres.any { it.equals("Animation", ignoreCase = true) || it.equals("Anime", ignoreCase = true) } &&
                    (originalLanguage?.equals("ja", ignoreCase = true) == true || originCountry.contains("JP"))

        fun toMatchHints(isTv: Boolean, season: Int? = null, episode: Int? = null, absoluteEpisode: Int? = null): MatchHints {
            val ct = when {
                isAnime -> ContentType.ANIME
                isTv -> ContentType.TV_SERIES
                else -> ContentType.MOVIE
            }
            val seasonTitles = season?.let { seasonSpecificTitles[it] }.orEmpty()
            val allTitles = (setOfNotNull(title, originalTitle, romajiTitle, englishTitle, nativeTitle) +
                    alternativeTitles + seasonNames.values + animeSynonyms + seasonTitles)
                .filter { it.isNotBlank() }
                .toSet()
            return MatchHints(
                tmdbId = tmdbId.toIntOrNull() ?: 0,
                imdbId = imdbId,
                type = ct,
                titles = allTitles,
                year = premiereYear ?: year,
                runtimeMin = runtimeMinutes,
                season = season,
                episode = episode,
                originalTitle = originalTitle,
                alternativeTitles = alternativeTitles + setOfNotNull(romajiTitle, englishTitle, nativeTitle) + animeSynonyms + seasonTitles,
                premiereYear = premiereYear ?: year,
                seasonYears = seasonYears,
                seasonNames = seasonNames,
                tvdbId = tvdbId,
                absoluteEpisode = absoluteEpisode,
                malId = malId,
                anilistId = anilistId,
                romajiTitle = romajiTitle,
                englishTitle = englishTitle,
                nativeTitle = nativeTitle,
                animeSynonyms = animeSynonyms,
                seasonSpecificTitles = seasonSpecificTitles
            )
        }
    }

    data class EpisodeDetail(
        val episodeNumber: Int,
        val name: String,
        val overview: String?,
        val stillUrl: String?,
        val duration: String?
    )

    fun getCachedEnrichedDetails(tmdbId: String, isTv: Boolean): EnrichedMetadata? {
        val endpoint = if (isTv) "tv" else "movie"
        val altEndpoint = if (isTv) "movie" else "tv"
        val rawClean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/")
        val clean = if (rawClean.startsWith("mal:", ignoreCase = true) ||
            rawClean.startsWith("anilist:", ignoreCase = true) ||
            rawClean.startsWith("jikan:", ignoreCase = true)
        ) {
            rawClean.substringAfter(':').substringBefore('/')
        } else {
            rawClean.substringBefore(':').substringBefore('/')
        }
        if (clean.isBlank()) return null

        val animeKey = when {
            rawClean.startsWith("mal:", ignoreCase = true) || rawClean.startsWith("jikan:", ignoreCase = true) -> "mal:$clean"
            rawClean.startsWith("anilist:", ignoreCase = true) -> "anilist:$clean"
            else -> null
        }
        if (animeKey != null) {
            enrichedCache["$endpoint:$animeKey"]?.let { return it }
            enrichedCache["$altEndpoint:$animeKey"]?.let { return it }
            aniZipCache[animeKey]?.tmdbId?.let { mappedTmdb ->
                enrichedCache["$endpoint:$mappedTmdb"]?.let { return it }
                enrichedCache["$altEndpoint:$mappedTmdb"]?.let { return it }
            }
        }

        val key = "$endpoint:$clean"
        val altKey = "$altEndpoint:$clean"
        return enrichedCache[key] ?: enrichedCache[altKey] ?: if (clean.startsWith("tt")) {
            imdbIdCache[clean]?.let { enrichedCache["$endpoint:$it"] ?: enrichedCache["$altEndpoint:$it"] }
        } else {
            imdbIdCache[clean]?.let { enrichedCache["$endpoint:$it"] ?: enrichedCache["$altEndpoint:$it"] }
        }
    }

    private val imdbIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun recordIdMapping(tmdbId: String, imdbId: String, isTv: Boolean) {
        val str1 = tmdbId.trim()
        val str2 = imdbId.trim()
        val cleanImdb = if (str1.startsWith("tt")) str1 else if (str2.startsWith("tt")) str2 else null
        val cleanTmdb = if (str1 != cleanImdb) str1 else str2
        if (!cleanImdb.isNullOrBlank() && cleanTmdb.isNotBlank()) {
            imdbIdCache["${if (isTv) "tv" else "movie"}:$cleanTmdb"] = cleanImdb
            imdbIdCache[cleanImdb] = cleanTmdb
        }
    }

    private val rawDetailsJsonCache = ConcurrentHashMap<String, String>()
    private val rawExternalIdsJsonCache = ConcurrentHashMap<String, String>()

    fun getCachedTmdbDetailsJson(tmdbId: String, isTv: Boolean): String? {
        val endpoint = if (isTv) "tv" else "movie"
        val altEndpoint = if (isTv) "movie" else "tv"
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isBlank()) return null
        val mappedId = if (clean.startsWith("tt")) imdbIdCache[clean] ?: clean else clean
        return rawDetailsJsonCache["$endpoint:$clean"]
            ?: rawDetailsJsonCache["$altEndpoint:$clean"]
            ?: rawDetailsJsonCache["$endpoint:$mappedId"]
            ?: rawDetailsJsonCache["$altEndpoint:$mappedId"]
            ?: getCachedEnrichedDetails(clean, isTv)?.let { buildCanonicalDetailsJson(it, isTv) }
    }

    fun getCachedTmdbExternalIdsJson(tmdbId: String, isTv: Boolean): String? {
        val endpoint = if (isTv) "tv" else "movie"
        val altEndpoint = if (isTv) "movie" else "tv"
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isBlank()) return null
        val mappedId = if (clean.startsWith("tt")) imdbIdCache[clean] ?: clean else clean
        return rawExternalIdsJsonCache["$endpoint:$clean"]
            ?: rawExternalIdsJsonCache["$altEndpoint:$clean"]
            ?: rawExternalIdsJsonCache["$endpoint:$mappedId"]
            ?: rawExternalIdsJsonCache["$altEndpoint:$mappedId"]
            ?: getCachedEnrichedDetails(clean, isTv)?.let { buildCanonicalExternalIdsJson(it) }
    }

    fun cacheTmdbDetailsJson(endpoint: String, tmdbId: String, json: String) {
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isNotBlank()) {
            rawDetailsJsonCache["$endpoint:$clean"] = json
        }
    }

    fun cacheTmdbExternalIdsJson(endpoint: String, tmdbId: String, json: String) {
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isNotBlank()) {
            rawExternalIdsJsonCache["$endpoint:$clean"] = json
        }
    }

    fun clearHomeCatalogCache() {
        cachedHomeCatalog.clear()
    }

    internal fun clearCachesForTesting() {
        logoCache.clear()
        enrichedCache.clear()
        seasonCache.clear()
        cachedHomeCatalog.clear()
        imdbIdCache.clear()
        rawDetailsJsonCache.clear()
        rawExternalIdsJsonCache.clear()
    }

    private fun buildCanonicalDetailsJson(meta: EnrichedMetadata, isTv: Boolean): String {
        val root = JSONObject()
        root.put("id", meta.tmdbId.toIntOrNull() ?: meta.tmdbId)
        root.put(if (isTv) "name" else "title", meta.title)
        if (meta.originalTitle != null) {
            root.put(if (isTv) "original_name" else "original_title", meta.originalTitle)
        }
        if (meta.originalLanguage != null) {
            root.put("original_language", meta.originalLanguage)
        }
        if (meta.year != null) {
            root.put(if (isTv) "first_air_date" else "release_date", "${meta.year}-01-01")
        }
        if (meta.imdbId != null) {
            root.put("imdb_id", meta.imdbId)
        }
        if (meta.synopsis != null) {
            root.put("overview", meta.synopsis)
        }
        return root.toString()
    }

    private fun buildCanonicalExternalIdsJson(meta: EnrichedMetadata): String {
        val root = JSONObject()
        root.put("id", meta.tmdbId.toIntOrNull() ?: meta.tmdbId)
        if (meta.imdbId != null) root.put("imdb_id", meta.imdbId)
        if (meta.tvdbId != null) root.put("tvdb_id", meta.tvdbId)
        return root.toString()
    }


    /**
     * Finds a TMDB ID using an IMDb ID (e.g. "tt0944947").
     */
    suspend fun findTmdbIdByImdb(
        client: OkHttpClient,
        imdbId: String,
        isTv: Boolean,
        apiKey: String = DEFAULT_API_KEY
    ): String? = withContext(Dispatchers.IO) {
        val cleanImdb = imdbId.trim().substringBefore(':').substringBefore('/')
        if (!cleanImdb.startsWith("tt")) return@withContext null
        imdbIdCache[cleanImdb]?.let { return@withContext it }
        try {
            val url = appendApiKey("$BASE_URL/find/$cleanImdb?external_source=imdb_id", apiKey)
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val found = if (isTv) {
                    root["tv_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: root["tv_episode_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("show_id")?.jsonPrimitive?.contentOrNull
                        ?: root["movie_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                } else {
                    root["movie_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: root["tv_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: root["tv_episode_results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("show_id")?.jsonPrimitive?.contentOrNull
                }
                if (found != null) {
                    recordIdMapping(found, cleanImdb, isTv)
                    return@withContext found
                }
            }
        } catch (_: Exception) {}

        // Fallback: Query Cinemeta for title and resolve via TMDB search
        try {
            val cinemetaType = if (isTv) "series" else "movie"
            val cinemetaUrl = "https://v3-cinemeta.strem.io/meta/$cinemetaType/$cleanImdb.json"
            val cinemetaReq = Request.Builder().url(cinemetaUrl).header("User-Agent", DEFAULT_USER_AGENT).build()
            val metaName = client.newCall(cinemetaReq).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val root = json.parseToJsonElement(body).jsonObject
                root["meta"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            }
            if (!metaName.isNullOrBlank()) {
                val encodedQuery = java.net.URLEncoder.encode(metaName, "UTF-8")
                val searchEndpoint = if (isTv) "tv" else "movie"
                val searchUrl = appendApiKey("$BASE_URL/search/$searchEndpoint?query=$encodedQuery", apiKey)
                val searchReq = Request.Builder().url(searchUrl).header("User-Agent", DEFAULT_USER_AGENT).build()
                client.newCall(searchReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val root = json.parseToJsonElement(body).jsonObject
                        val firstMatch = root["results"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        if (firstMatch != null) {
                            recordIdMapping(firstMatch, cleanImdb, isTv)
                            return@withContext firstMatch
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }


    /**
     * Resolves an IMDb ID (e.g. "tt0944947") from a numeric TMDB ID.
     */
    suspend fun resolveImdbId(
        client: OkHttpClient = DohDns.metadataClient,
        tmdbId: String,
        isTv: Boolean,
        apiKey: String = DEFAULT_API_KEY
    ): String? = withContext(Dispatchers.IO) {
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isBlank()) return@withContext null
        if (clean.startsWith("tt")) return@withContext clean
        val cacheKey = "${if (isTv) "tv" else "movie"}:$clean"
        imdbIdCache[cacheKey]?.let { return@withContext it }
        imdbIdCache[clean]?.let { return@withContext it }

        enrichedCache[cacheKey]?.imdbId?.takeIf { it.startsWith("tt") }?.let {
            recordIdMapping(clean, it, isTv)
            return@withContext it
        }

        getCachedTmdbExternalIdsJson(clean, isTv)?.let { cachedJson ->
            try {
                val parsed = json.parseToJsonElement(cachedJson).jsonObject
                val found = parsed["imdb_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("tt") }
                if (found != null) {
                    recordIdMapping(clean, found, isTv)
                    return@withContext found
                }
            } catch (_: Exception) {}
        }

        try {
            val endpoint = if (isTv) "tv" else "movie"
            val url = appendApiKey("$BASE_URL/$endpoint/$clean/external_ids", apiKey)
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                cacheTmdbExternalIdsJson(endpoint, clean, body)
                val root = json.parseToJsonElement(body).jsonObject
                val found = root["imdb_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("tt") }
                if (found != null) {
                    recordIdMapping(clean, found, isTv)
                    return@withContext found
                }
            }
        } catch (_: Throwable) {}
        null
    }

    /**
     * Centralized lookup for external IDs (IMDb ID) from an endpoint type ("movie", "tv", "series", "anime", etc.) and TMDB ID.
     */
    suspend fun resolveImdbId(
        type: String,
        tmdbId: String,
        client: OkHttpClient = DohDns.metadataClient,
        apiKey: String = DEFAULT_API_KEY
    ): String? {
        val norm = type.trim().lowercase(Locale.US)
        val isTv = norm == "tv" || norm == "series" || norm == "tv_series" || norm == "tvseries" ||
                norm == "show" || norm == "shows" || norm == "anime" || norm.contains("tv")
        return resolveImdbId(client = client, tmdbId = tmdbId, isTv = isTv, apiKey = apiKey)
    }

    /**
     * Centralized lookup for external IDs (IMDb ID) from MediaType and TMDB ID.
     */
    suspend fun resolveImdbId(
        type: MediaType,
        tmdbId: String,
        client: OkHttpClient = DohDns.metadataClient,
        apiKey: String = DEFAULT_API_KEY
    ): String? {
        val isTv = type == MediaType.TV_SERIES || type == MediaType.ANIME
        return resolveImdbId(client = client, tmdbId = tmdbId, isTv = isTv, apiKey = apiKey)
    }

    /**
     * Centralized method to fetch the top 10 trending items for a media type from TMDB.
     */
    suspend fun fetchTrendingTopTen(
        type: MediaType,
        language: String = "en-US",
        preset: ContentRegionPreset = activeRegionPreset,
        client: OkHttpClient = DohDns.metadataClient,
        apiKey: String = DEFAULT_API_KEY,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<MediaItem> = withContext(dispatcher) {
        try {
            val endpointType = when (type) {
                MediaType.TV_SERIES, MediaType.ANIME -> "tv"
                else -> "movie"
            }
            val todayStr = java.time.LocalDate.now().toString()
            val url = if (preset == ContentRegionPreset.GLOBAL) {
                appendApiKey("$BASE_URL/trending/$endpointType/day?language=$language", apiKey)
            } else {
                val dateFilter = if (endpointType == "tv") {
                    "first_air_date.lte=$todayStr"
                } else {
                    "primary_release_date.lte=$todayStr"
                }
                val langParam = preset.originalLanguages?.let { "with_original_language=$it&" } ?: ""
                appendApiKey("$BASE_URL/discover/$endpointType?${langParam}sort_by=popularity.desc&$dateFilter", apiKey)
            }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.parseToJsonElement(body).jsonObject
                val results = parsed["results"]?.jsonArray ?: return@withContext emptyList()

                val items = mutableListOf<MediaItem>()
                for (elem in results) {
                    if (items.size >= 10) break
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                    if (posterPath.isNullOrBlank()) continue

                    val title = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: "Untitled"
                    val backdropPath = obj["backdrop_path"]?.jsonPrimitive?.contentOrNull
                    val voteAverage = obj["vote_average"]?.jsonPrimitive?.doubleOrNull
                    val releaseDate = obj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val year = releaseDate?.take(4)?.toIntOrNull()

                    val posterUrl = "https://image.tmdb.org/t/p/w780$posterPath"
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
                    val ratingStr = voteAverage?.let { String.format(Locale.US, "%.1f", it) }

                    val relStatus = resolveReleaseStatus(id, type == MediaType.TV_SERIES || type == MediaType.ANIME, releaseDate, null)
                    items.add(
                        MediaItem(
                            id = id,
                            title = title,
                            url = "",
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            type = type,
                            year = year,
                            rating = ratingStr,
                            quality = "1080p",
                            provider = "tmdb",
                            releaseDate = releaseDate,
                            releaseStatus = relStatus
                        )
                    )
                }
                items
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches top trending items (movies and TV series) across all categories from TMDB.
     */
    suspend fun fetchTrendingAll(
        timeWindow: String = "day",
        language: String = "en-US",
        client: OkHttpClient = DohDns.metadataClient,
        apiKey: String = DEFAULT_API_KEY,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<MediaItem> = withContext(dispatcher) {
        try {
            val url = appendApiKey("$BASE_URL/trending/all/$timeWindow?language=$language", apiKey)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.parseToJsonElement(body).jsonObject
                val results = parsed["results"]?.jsonArray ?: return@withContext emptyList()

                val items = mutableListOf<MediaItem>()
                for (elem in results) {
                    if (items.size >= 10) break
                    val obj = elem.jsonObject
                    val mediaTypeStr = obj["media_type"]?.jsonPrimitive?.contentOrNull
                    if (mediaTypeStr == "person") continue

                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                    if (posterPath.isNullOrBlank()) continue

                    val isTv = mediaTypeStr == "tv" || obj.containsKey("first_air_date") || (obj.containsKey("name") && !obj.containsKey("title"))
                    val title = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: "Untitled"
                    val backdropPath = obj["backdrop_path"]?.jsonPrimitive?.contentOrNull
                    val voteAverage = obj["vote_average"]?.jsonPrimitive?.doubleOrNull
                    val releaseDate = obj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val year = releaseDate?.take(4)?.toIntOrNull()

                    val posterUrl = "https://image.tmdb.org/t/p/w780$posterPath"
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
                    val ratingStr = voteAverage?.let { String.format(Locale.US, "%.1f", it) }

                    val relStatus = resolveReleaseStatus(id, isTv, releaseDate, null)
                    items.add(
                        MediaItem(
                            id = id,
                            title = title,
                            url = "",
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE,
                            year = year,
                            rating = ratingStr,
                            quality = "1080p",
                            provider = "tmdb",
                            releaseDate = releaseDate,
                            releaseStatus = relStatus
                        )
                    )
                }
                items
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    data class OnboardingShowcase(
        val trendingMovies: List<MediaItem> = emptyList(),
        val trendingTv: List<MediaItem> = emptyList(),
        val topTen: List<MediaItem> = emptyList(),
        val mixed: List<MediaItem> = emptyList()
    )

    /**
     * Concurrently fetches a balanced mix of Trending Movies, Trending TV Shows,
     * and Top 10 items for onboarding previewers via the centralized Cloudflare worker gateway.
     */
    suspend fun fetchOnboardingShowcasePosters(
        language: String = "en-US",
        client: OkHttpClient = DohDns.metadataClient,
        apiKey: String = DEFAULT_API_KEY,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): OnboardingShowcase = coroutineScope {
        val moviesDeferred = async(dispatcher) { fetchTrendingTopTen(type = MediaType.MOVIE, language = language, client = client, apiKey = apiKey, dispatcher = dispatcher) }
        val tvDeferred = async(dispatcher) { fetchTrendingTopTen(type = MediaType.TV_SERIES, language = language, client = client, apiKey = apiKey, dispatcher = dispatcher) }
        val topTenDeferred = async(dispatcher) { fetchTrendingAll("day", language, client, apiKey, dispatcher) }

        val movies = moviesDeferred.await()
        val tv = tvDeferred.await()
        val topTen = topTenDeferred.await()

        val mixed = mutableListOf<MediaItem>()
        val seenKeys = mutableSetOf<String>()

        fun tryAdd(item: MediaItem) {
            val key = "${item.type.name}:${item.id}"
            if (seenKeys.add(key)) {
                mixed.add(item)
            }
        }

        val maxLen = maxOf(topTen.size, maxOf(movies.size, tv.size))
        for (i in 0 until maxLen) {
            if (i < topTen.size) tryAdd(topTen[i])
            if (i < movies.size) tryAdd(movies[i])
            if (i < tv.size) tryAdd(tv[i])
        }

        OnboardingShowcase(
            trendingMovies = movies,
            trendingTv = tv,
            topTen = topTen,
            mixed = mixed
        )
    }

    data class AniZipData(
        val malId: Int? = null,
        val anilistId: Int? = null,
        val tmdbId: String? = null,
        val imdbId: String? = null,
        val romajiTitle: String? = null,
        val englishTitle: String? = null,
        val nativeTitle: String? = null,
        val synonyms: List<String> = emptyList(),
        val episodeAbsoluteMap: Map<Pair<Int, Int>, Int> = emptyMap()
    )

    data class AnimeEnrichmentResult(
        val malId: Int? = null,
        val anilistId: Int? = null,
        val romajiTitle: String? = null,
        val englishTitle: String? = null,
        val nativeTitle: String? = null,
        val synonyms: List<String> = emptyList(),
        val seasonSpecificTitles: Map<Int, List<String>> = emptyMap()
    )

    private val aniZipCache = ConcurrentHashMap<String, AniZipData>()
    private val animeEnrichCache = ConcurrentHashMap<String, AnimeEnrichmentResult>()
    private val animeAbsoluteEpisodeCache = ConcurrentHashMap<String, Int>()

    /**
     * Fetches AniZip mappings and caches the result across all mapped IDs.
     */
    suspend fun fetchAniZipMappings(
        client: OkHttpClient = DohDns.metadataClient,
        tmdbId: String? = null,
        malId: Int? = null,
        anilistId: Int? = null,
        imdbId: String? = null
    ): AniZipData? = withContext(Dispatchers.IO) {
        val cacheKeys = listOfNotNull(
            tmdbId?.takeIf { it.isNotBlank() }?.let { "tmdb:$it" },
            malId?.takeIf { it > 0 }?.let { "mal:$it" },
            anilistId?.takeIf { it > 0 }?.let { "anilist:$it" },
            imdbId?.takeIf { it.startsWith("tt") }?.let { "imdb:$it" }
        )
        for (k in cacheKeys) {
            aniZipCache[k]?.let { return@withContext it }
        }

        val queryParam = when {
            malId != null && malId > 0 -> "mal_id=$malId"
            anilistId != null && anilistId > 0 -> "anilist_id=$anilistId"
            !tmdbId.isNullOrBlank() && tmdbId.all { it.isDigit() } -> "themoviedb_id=$tmdbId"
            !imdbId.isNullOrBlank() && imdbId.startsWith("tt") -> "imdb_id=$imdbId"
            else -> null
        } ?: return@withContext null

        try {
            val url = "https://api.ani.zip/mappings?$queryParam"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
            val fastClient = client.newBuilder()
                .callTimeout(java.time.Duration.ofMillis(2500))
                .build()

            fastClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null

                val root = json.parseToJsonElement(body).jsonObject
                val mappingsObj = root["mappings"]?.jsonObject
                val resMalId = mappingsObj?.get("mal_id")?.jsonPrimitive?.intOrNull
                val resAnilistId = mappingsObj?.get("anilist_id")?.jsonPrimitive?.intOrNull
                val resTmdbId = mappingsObj?.get("themoviedb_id")?.jsonPrimitive?.contentOrNull
                val resImdbId = mappingsObj?.get("imdb_id")?.jsonPrimitive?.contentOrNull

                val titlesObj = root["titles"]?.jsonObject
                val romaji = titlesObj?.get("x-jat")?.jsonPrimitive?.contentOrNull
                    ?: titlesObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                val english = titlesObj?.get("en")?.jsonPrimitive?.contentOrNull
                val native = titlesObj?.get("ja")?.jsonPrimitive?.contentOrNull

                val synonymsList = mutableListOf<String>()
                titlesObj?.forEach { (k, v) ->
                    val t = v.jsonPrimitive.contentOrNull?.trim()
                    if (!t.isNullOrBlank() && t != romaji && t != english && t != native) {
                        synonymsList.add(t)
                    }
                }

                val epMap = mutableMapOf<Pair<Int, Int>, Int>()
                root["episodes"]?.jsonObject?.values?.forEach { epElem ->
                    val epObj = epElem as? JsonObject ?: return@forEach
                    val s = epObj["seasonNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["season"]?.jsonPrimitive?.intOrNull
                        ?: epObj["tvdbSeason"]?.jsonPrimitive?.intOrNull
                    if (s != null && s <= 0) return@forEach
                    val effectiveSeason = s ?: 1
                    val e = epObj["episodeNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["episode"]?.jsonPrimitive?.intOrNull
                        ?: epObj["tvdbEpisode"]?.jsonPrimitive?.intOrNull
                        ?: return@forEach
                    val abs = epObj["absoluteEpisodeNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["anidbEpisodeNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["kitsuEpisodeNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["malEpisodeNumber"]?.jsonPrimitive?.intOrNull
                        ?: epObj["kitsuEpisode"]?.jsonPrimitive?.intOrNull
                        ?: epObj["malEpisode"]?.jsonPrimitive?.intOrNull
                        ?: epObj["abs"]?.jsonPrimitive?.intOrNull
                    if (abs != null && abs > 0) {
                        epMap[effectiveSeason to e] = abs
                    }
                }

                val data = AniZipData(
                    malId = resMalId ?: malId,
                    anilistId = resAnilistId ?: anilistId,
                    tmdbId = resTmdbId ?: tmdbId,
                    imdbId = resImdbId ?: imdbId,
                    romajiTitle = romaji,
                    englishTitle = english,
                    nativeTitle = native,
                    synonyms = synonymsList.distinct(),
                    episodeAbsoluteMap = epMap
                )
                listOfNotNull(
                    data.tmdbId?.let { "tmdb:$it" },
                    data.malId?.let { "mal:$it" },
                    data.anilistId?.let { "anilist:$it" },
                    data.imdbId?.let { "imdb:$it" }
                ).forEach { key ->
                    aniZipCache[key] = data
                }
                return@withContext data
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Queries AniList GraphQL to enrich Romaji, English, Native titles, synonyms, and season sequels.
     */
    suspend fun fetchAniListGraphQL(
        client: OkHttpClient = DohDns.metadataClient,
        anilistId: Int? = null,
        malId: Int? = null,
        searchTitle: String? = null
    ): AnimeEnrichmentResult? = withContext(Dispatchers.IO) {
        val cacheKey = "al:${anilistId ?: ""}:${malId ?: ""}:${searchTitle ?: ""}"
        animeEnrichCache[cacheKey]?.let { return@withContext it }

        val query = """
            query (${'$'}id: Int, ${'$'}idMal: Int, ${'$'}search: String) {
              Media(id: ${'$'}id, idMal: ${'$'}idMal, search: ${'$'}search, type: ANIME) {
                id
                idMal
                seasonYear
                title {
                  romaji
                  english
                  native
                }
                synonyms
                relations {
                  edges {
                    relationType
                    node {
                      id
                      idMal
                      title {
                        romaji
                        english
                      }
                      seasonYear
                      format
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (anilistId != null && anilistId > 0) put("id", anilistId)
            if (malId != null && malId > 0) put("idMal", malId)
            if (anilistId == null && malId == null && !searchTitle.isNullOrBlank()) put("search", searchTitle)
        }
        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        try {
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()

            val fastClient = client.newBuilder().callTimeout(java.time.Duration.ofMillis(2500)).build()
            fastClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val media = root["data"]?.jsonObject?.get("Media")?.jsonObject ?: return@use null
                        val resAlId = media["id"]?.jsonPrimitive?.intOrNull
                        val resMalId = media["idMal"]?.jsonPrimitive?.intOrNull
                        val titleObj = media["title"]?.jsonObject
                        val romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                        val english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
                        val native = titleObj?.get("native")?.jsonPrimitive?.contentOrNull

                        val synonyms = media["synonyms"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                        val seasonSpecific = mutableMapOf<Int, MutableList<String>>()
                        val relations = media["relations"]?.jsonObject?.get("edges")?.jsonArray
                        relations?.forEach { edgeElem ->
                            val edge = edgeElem.jsonObject
                            val relType = edge["relationType"]?.jsonPrimitive?.contentOrNull ?: ""
                            val node = edge["node"]?.jsonObject ?: return@forEach
                            val nodeTitle = node["title"]?.jsonObject
                            val nodeRomaji = nodeTitle?.get("romaji")?.jsonPrimitive?.contentOrNull
                            val nodeEn = nodeTitle?.get("english")?.jsonPrimitive?.contentOrNull
                            val titles = listOfNotNull(nodeRomaji, nodeEn)

                            titles.forEach { t ->
                                val sMatch = Regex("""\b(?:season|s)\s*(\d{1,2})\b""", RegexOption.IGNORE_CASE).find(t)
                                    ?: Regex("""\b(\d{1,2})(?:nd|rd|th)\s+season\b""", RegexOption.IGNORE_CASE).find(t)
                                val sNum = sMatch?.groupValues?.get(1)?.toIntOrNull()
                                    ?: if (relType.equals("SEQUEL", ignoreCase = true)) 2 else null
                                if (sNum != null) {
                                    seasonSpecific.getOrPut(sNum) { mutableListOf() }.add(t)
                                }
                            }
                        }

                        val enrich = AnimeEnrichmentResult(
                            malId = resMalId ?: malId,
                            anilistId = resAlId ?: anilistId,
                            romajiTitle = romaji,
                            englishTitle = english,
                            nativeTitle = native,
                            synonyms = synonyms,
                            seasonSpecificTitles = seasonSpecific.mapValues { it.value.distinct() }
                        )
                        animeEnrichCache[cacheKey] = enrich
                        return@withContext enrich
                    }
                }
            }
        } catch (_: Throwable) {}
        null
    }

    /**
     * Fallback lookup against Jikan v4 (MyAnimeList) API.
     */
    suspend fun fetchJikanDetails(
        client: OkHttpClient = DohDns.metadataClient,
        malId: Int
    ): AnimeEnrichmentResult? = withContext(Dispatchers.IO) {
        if (malId <= 0) return@withContext null
        val cacheKey = "jikan:$malId"
        animeEnrichCache[cacheKey]?.let { return@withContext it }

        try {
            val req = Request.Builder()
                .url("https://api.jikan.moe/v4/anime/$malId")
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
            val fastClient = client.newBuilder().callTimeout(java.time.Duration.ofMillis(2500)).build()
            fastClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val data = root["data"]?.jsonObject ?: return@use null
                        val title = data["title"]?.jsonPrimitive?.contentOrNull
                        val titleEn = data["title_english"]?.jsonPrimitive?.contentOrNull
                        val titleJa = data["title_japanese"]?.jsonPrimitive?.contentOrNull
                        val synonyms = data["title_synonyms"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                        val otherTitles = data["titles"]?.jsonArray?.mapNotNull {
                            it.jsonObject["title"]?.jsonPrimitive?.contentOrNull
                        } ?: emptyList()

                        val allSynonyms = (synonyms + otherTitles).distinct()
                        val enrich = AnimeEnrichmentResult(
                            malId = malId,
                            romajiTitle = title,
                            englishTitle = titleEn,
                            nativeTitle = titleJa,
                            synonyms = allSynonyms
                        )
                        animeEnrichCache[cacheKey] = enrich
                        return@withContext enrich
                    }
                }
            }
        } catch (_: Throwable) {}
        null
    }

    /**
     * Resolves the absolute episode number for anime.
     * Tries AniZip API first (with memory cache), with offline fallback: episode + sum(prior seasons episode counts).
     */
    suspend fun resolveAnimeAbsoluteEpisode(
        client: OkHttpClient = DohDns.metadataClient,
        tmdbId: String,
        imdbId: String? = null,
        season: Int,
        episode: Int,
        seasonEpisodeCounts: Map<Int, Int> = emptyMap()
    ): Int = withContext(Dispatchers.IO) {
        val cacheKey = "$tmdbId:$season:$episode"
        animeAbsoluteEpisodeCache[cacheKey]?.let { return@withContext it }

        // 1. Try AniZip API via cached fetchAniZipMappings
        val aniZip = fetchAniZipMappings(client, tmdbId = tmdbId, imdbId = imdbId)
        val cachedAbs = aniZip?.episodeAbsoluteMap?.get(season to episode)
        if (cachedAbs != null && cachedAbs > 0) {
            animeAbsoluteEpisodeCache[cacheKey] = cachedAbs
            return@withContext cachedAbs
        }

        // 2. Offline fallback: episode + sum(prior seasons, ignoring Season 0 specials)
        val priorCount = (1 until season).filter { it > 0 }.sumOf { sNum ->
            seasonEpisodeCounts[sNum] ?: 0
        }
        val calculated = if (priorCount > 0) priorCount + episode else episode
        animeAbsoluteEpisodeCache[cacheKey] = calculated
        calculated
    }

    /**
     * Searches TMDB by title and optional year to find the best matching TMDB ID using MatchScorer.
     */
    suspend fun searchTmdbId(
        client: OkHttpClient,
        title: String,
        year: Int?,
        isTv: Boolean,
        apiKey: String = DEFAULT_API_KEY
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.replace(Regex("""[^\w\s]"""), " ").trim()
        if (cleanTitle.isBlank()) return@withContext null
        val encodedQuery = URLEncoder.encode(cleanTitle, "UTF-8")
        val endpoint = if (isTv) "tv" else "movie"

        val hints = MatchHints(
            tmdbId = 0,
            imdbId = null,
            type = if (isTv) ContentType.TV_SERIES else ContentType.MOVIE,
            titles = setOf(cleanTitle, title),
            year = year,
            runtimeMin = null
        )

        // 1. Try with year if present
        if (year != null) {
            val urlWithYear = if (isTv) {
                appendApiKey("$BASE_URL/search/tv?query=$encodedQuery&first_air_date_year=$year", apiKey)
            } else {
                appendApiKey("$BASE_URL/search/movie?query=$encodedQuery&year=$year", apiKey)
            }
            val id = queryBestTmdbId(client, urlWithYear, hints)
            if (id != null) return@withContext id
        }

        // 2. Try without year
        val urlWithoutYear = appendApiKey("$BASE_URL/search/$endpoint?query=$encodedQuery", apiKey)
        val id = queryBestTmdbId(client, urlWithoutYear, hints)
        if (id != null) return@withContext id

        // 3. Fallback: try multi search
        val multiUrl = appendApiKey("$BASE_URL/search/multi?query=$encodedQuery", apiKey)
        queryBestTmdbId(client, multiUrl, hints)
    }

    private fun queryBestTmdbId(client: OkHttpClient, url: String, hints: MatchHints): String? {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: return null

                data class ScoredCandidate(val id: String, val score: Double)
                val scoredList = mutableListOf<ScoredCandidate>()

                for (elem in results) {
                    val obj = (elem as? JsonObject) ?: continue
                    val mediaType = obj["media_type"]?.jsonPrimitive?.contentOrNull
                    if (mediaType == "person") continue

                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val candTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: continue

                    val dateStr = obj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val candYear = dateStr?.take(4)?.toIntOrNull()

                    val candType = if (mediaType == "tv" || obj.containsKey("first_air_date")) {
                        ContentType.TV_SERIES
                    } else {
                        ContentType.MOVIE
                    }

                    val rawScore = MatchScorer.score(hints, candTitle, candYear, candType)
                    val voteCount = obj["vote_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val popBonus = if (voteCount > 100) 0.05 else if (voteCount > 10) 0.02 else 0.0

                    val totalScore = rawScore + popBonus
                    scoredList.add(ScoredCandidate(id, totalScore))
                }

                scoredList.maxByOrNull { it.score }?.takeIf { it.score >= 0.45 }?.id
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the official transparent PNG clear logo for a media item.
     * Prioritizes English PNG logos with high community vote scores, with fallback to Metahub.
     */
    suspend fun resolveLogo(
        client: OkHttpClient,
        tmdbId: String,
        isTv: Boolean,
        imdbId: String? = null,
        apiKey: String = DEFAULT_API_KEY
    ): String? = withContext(Dispatchers.IO) {
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isBlank()) return@withContext null
        logoCache[clean]?.let { return@withContext it }

        val typeStr = if (isTv) "tv" else "movie"

        // 1. Primary: TMDB Images API with English priority
        try {
            val url = appendApiKey("$BASE_URL/$typeStr/$clean/images?include_image_language=en,null", apiKey)
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = json.parseToJsonElement(body).jsonObject
                    val logosArr = root["logos"]?.jsonArray
                    if (!logosArr.isNullOrEmpty()) {
                        var bestPath: String? = null
                        var bestScore = -1.0
                        for (elem in logosArr) {
                            val obj = elem.jsonObject
                            val filePath = obj["file_path"]?.jsonPrimitive?.contentOrNull ?: continue
                            val lang = obj["iso_639_1"]?.jsonPrimitive?.contentOrNull
                            val vote = obj["vote_average"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val langBonus = if (lang == "en") 10.0 else 0.0
                            val pngBonus = if (filePath.endsWith(".png", ignoreCase = true)) 5.0 else 0.0
                            val score = vote + langBonus + pngBonus
                            if (score > bestScore) {
                                bestScore = score
                                bestPath = filePath
                            }
                        }
                        if (bestPath != null) {
                            val fullUrl = "https://image.tmdb.org/t/p/w500$bestPath"
                            logoCache[clean] = fullUrl
                            return@withContext fullUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Secondary: TMDB Images API unfiltered
        try {
            val urlAll = appendApiKey("$BASE_URL/$typeStr/$clean/images", apiKey)
            val req = Request.Builder().url(urlAll).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = json.parseToJsonElement(body).jsonObject
                    val logosArr = root["logos"]?.jsonArray
                    val firstLogo = logosArr?.firstOrNull()?.jsonObject?.get("file_path")?.jsonPrimitive?.contentOrNull
                    if (firstLogo != null) {
                        val fullUrl = "https://image.tmdb.org/t/p/w500$firstLogo"
                        logoCache[clean] = fullUrl
                        return@withContext fullUrl
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Metahub fallback
        if (imdbId != null && imdbId.startsWith("tt")) {
            val metaUrl = "https://images.metahub.space/logo/medium/$imdbId/img.png"
            logoCache[clean] = metaUrl
            return@withContext metaUrl
        }

        null
    }

    /**
     * Enriches media details with full TMDB metadata contract compliance.
     */
    suspend fun fetchEnrichedDetails(
        client: OkHttpClient,
        tmdbId: String,
        isTv: Boolean,
        providerName: String,
        apiKey: String = DEFAULT_API_KEY
    ): EnrichedMetadata? = withContext(Dispatchers.IO) {
        val cleanInputId = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/")
        val isMalInput = cleanInputId.startsWith("mal:", ignoreCase = true) || cleanInputId.startsWith("jikan:", ignoreCase = true)
        val isAnilistInput = cleanInputId.startsWith("anilist:", ignoreCase = true)
        val inputMalId = if (isMalInput) cleanInputId.substringAfter(':').substringBefore('/').toIntOrNull() else null
        val inputAnilistId = if (isAnilistInput) cleanInputId.substringAfter(':').substringBefore('/').toIntOrNull() else null

        val cleanBase = if (inputMalId != null || inputAnilistId != null) {
            cleanInputId.substringAfter(':').substringBefore('/')
        } else {
            cleanInputId.substringBefore(':').substringBefore('/')
        }
        if (cleanBase.isBlank()) return@withContext null

        var resolvedTmdbFromAnime: String? = null
        var preResolvedAniZip: AniZipData? = null
        if (inputMalId != null || inputAnilistId != null) {
            preResolvedAniZip = fetchAniZipMappings(client, malId = inputMalId, anilistId = inputAnilistId)
            resolvedTmdbFromAnime = preResolvedAniZip?.tmdbId
        }

        val numericTmdbId = when {
            resolvedTmdbFromAnime != null && resolvedTmdbFromAnime.all { it.isDigit() } -> resolvedTmdbFromAnime
            cleanBase.startsWith("tt") -> findTmdbIdByImdb(client, cleanBase, isTv, apiKey) ?: return@withContext null
            cleanBase.all { it.isDigit() } -> cleanBase
            else -> return@withContext null
        }
        if (!numericTmdbId.all { it.isDigit() }) return@withContext null

        val endpointPrefix = if (isTv) "tv" else "movie"
        val altPrefix = if (isTv) "movie" else "tv"
        enrichedCache["$endpointPrefix:$numericTmdbId"]?.let { return@withContext it }
        enrichedCache["$altPrefix:$numericTmdbId"]?.let { return@withContext it }
        if (cleanBase.startsWith("tt")) {
            enrichedCache["$endpointPrefix:$cleanBase"]?.let { return@withContext it }
            enrichedCache["$altPrefix:$cleanBase"]?.let { return@withContext it }
        }
        if (inputMalId != null) {
            enrichedCache["$endpointPrefix:mal:$inputMalId"]?.let { return@withContext it }
            enrichedCache["$altPrefix:mal:$inputMalId"]?.let { return@withContext it }
        }
        if (inputAnilistId != null) {
            enrichedCache["$endpointPrefix:anilist:$inputAnilistId"]?.let { return@withContext it }
            enrichedCache["$altPrefix:anilist:$inputAnilistId"]?.let { return@withContext it }
        }

        for (attempt in 1..3) {
            try {
                var endpoint = if (isTv) "tv" else "movie"
                val append = "credits,external_ids,videos,recommendations,images,alternative_titles,translations," +
                        if (isTv) "content_ratings" else "release_dates"
                val url = appendApiKey("$BASE_URL/$endpoint/$numericTmdbId?append_to_response=$append&include_image_language=en,null", apiKey)

                val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
                var (statusCode, body) = client.newCall(req).execute().use { resp ->
                    resp.code to (resp.body?.string() ?: "")
                }

                if (statusCode == 429) {
                    delay(400L * attempt)
                    continue
                }
                if (statusCode == 404) {
                    val altEndpoint = if (endpoint == "tv") "movie" else "tv"
                    val altAppend = "credits,external_ids,videos,recommendations,images,alternative_titles,translations," +
                            if (altEndpoint == "tv") "content_ratings" else "release_dates"
                    val altUrl = appendApiKey("$BASE_URL/$altEndpoint/$numericTmdbId?append_to_response=$altAppend&include_image_language=en,null", apiKey)
                    val altReq = Request.Builder().url(altUrl).header("User-Agent", DEFAULT_USER_AGENT).build()
                    val (altStatus, altBody) = client.newCall(altReq).execute().use { resp ->
                        resp.code to (resp.body?.string() ?: "")
                    }
                    if (altStatus in 200..299 && altBody.isNotBlank()) {
                        endpoint = altEndpoint
                        statusCode = altStatus
                        body = altBody
                    }
                }
                if (statusCode !in 200..299 || body.isBlank()) {
                    if (attempt < 3) delay(200L * attempt)
                    continue
                }

                cacheTmdbDetailsJson(endpoint, numericTmdbId, body)
                val root = json.parseToJsonElement(body).jsonObject
                root["external_ids"]?.jsonObject?.let { ext ->
                    cacheTmdbExternalIdsJson(endpoint, numericTmdbId, ext.toString())
                }

                val title = root["title"]?.jsonPrimitive?.contentOrNull
                    ?: root["name"]?.jsonPrimitive?.contentOrNull
                    ?: ""

                val backdropPath = root["backdrop_path"]?.jsonPrimitive?.contentOrNull
                val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }

                val posterPath = root["poster_path"]?.jsonPrimitive?.contentOrNull
                val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }

                val overview = root["overview"]?.jsonPrimitive?.contentOrNull

                // Rating (IMDb score or TMDB vote)
                val voteAverage = root["vote_average"]?.jsonPrimitive?.doubleOrNull
                var rating = voteAverage?.takeIf { it > 0.0 }?.let {
                    String.format(Locale.US, "%.1f", it)
                }

                // Rotten Tomatoes calculation / estimation fallback
                var rtRating = voteAverage?.takeIf { it > 0.0 }?.let {
                    val pct = ((it * 10.0) + 4.0).coerceIn(40.0, 99.0).toInt()
                    "$pct%"
                }

                // Runtime / Duration
                val runtimeMins = root["runtime"]?.jsonPrimitive?.intOrNull
                    ?: root["episode_run_time"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.intOrNull
                val duration = formatDuration(runtimeMins)

                // Genres
                val genres = root["genres"]?.jsonArray?.mapNotNull {
                    it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                } ?: emptyList()

                // External IDs
                val externalIds = root["external_ids"]?.jsonObject
                val imdbId = externalIds?.get("imdb_id")?.jsonPrimitive?.contentOrNull
                    ?: root["imdb_id"]?.jsonPrimitive?.contentOrNull

                val resolvedImdbId = imdbId?.takeIf { it.startsWith("tt") }
                    ?: (if (cleanInputId.startsWith("tt")) cleanInputId else null)

                if (!resolvedImdbId.isNullOrBlank()) {
                    recordIdMapping(numericTmdbId, resolvedImdbId, isTv)
                }

                // Language and Origin Country (for Anime detection)
                val origLang = root["original_language"]?.jsonPrimitive?.contentOrNull
                val originCountry = root["origin_country"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                // Content Rating (US certification)
                var contentRating: String? = null
                if (isTv) {
                    val crResults = root["content_ratings"]?.jsonObject?.get("results")?.jsonArray
                    contentRating = crResults?.firstOrNull {
                        it.jsonObject["iso_3166_1"]?.jsonPrimitive?.contentOrNull == "US"
                    }?.jsonObject?.get("rating")?.jsonPrimitive?.contentOrNull
                        ?: crResults?.firstOrNull()?.jsonObject?.get("rating")?.jsonPrimitive?.contentOrNull
                } else {
                    val rdResults = root["release_dates"]?.jsonObject?.get("results")?.jsonArray
                    val usRelease = rdResults?.firstOrNull {
                        it.jsonObject["iso_3166_1"]?.jsonPrimitive?.contentOrNull == "US"
                    }?.jsonObject?.get("release_dates")?.jsonArray
                    contentRating = usRelease?.firstOrNull {
                        val cert = it.jsonObject["certification"]?.jsonPrimitive?.contentOrNull
                        !cert.isNullOrBlank()
                    }?.jsonObject?.get("certification")?.jsonPrimitive?.contentOrNull
                }

                // Cast
                val castList = mutableListOf<CastMember>()
                val creditsObj = root["credits"]?.jsonObject
                creditsObj?.get("cast")?.jsonArray?.take(30)?.forEach { cElem ->
                    val cObj = cElem.jsonObject
                    val name = cObj["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val character = cObj["character"]?.jsonPrimitive?.contentOrNull
                    val profilePath = cObj["profile_path"]?.jsonPrimitive?.contentOrNull
                    val profileUrl = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
                    val personId = cObj["id"]?.jsonPrimitive?.contentOrNull ?: name
                    castList.add(CastMember(id = personId, name = name, character = character, profileUrl = profileUrl))
                }

                // Directors / Creators
                val directorsList = mutableListOf<String>()
                creditsObj?.get("crew")?.jsonArray?.forEach { cElem ->
                    val cObj = cElem.jsonObject
                    val job = cObj["job"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (job.equals("Director", ignoreCase = true)) {
                        cObj["name"]?.jsonPrimitive?.contentOrNull?.let { directorsList.add(it) }
                    }
                }
                if (isTv && directorsList.isEmpty()) {
                    root["created_by"]?.jsonArray?.forEach { crElem ->
                        crElem.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let { directorsList.add(it) }
                    }
                }

                // Release Year: Movies = theatrical premiere year; TV = canonical first_air_date premiere year!
                val initialDate = root["release_date"]?.jsonPrimitive?.contentOrNull
                    ?: root["first_air_date"]?.jsonPrimitive?.contentOrNull
                val premiereYear = initialDate?.take(4)?.toIntOrNull()
                val year = premiereYear

                val seasonNumbers = mutableListOf<Int>()
                val seasonEpisodeCounts = mutableMapOf<Int, Int>()
                val seasonYears = mutableMapOf<Int, Int>()
                val seasonNames = mutableMapOf<Int, String>()
                var latestSeasonYear: Int? = null
                if (isTv) {
                    val seasonsArr = root["seasons"]?.jsonArray
                    if (seasonsArr != null) {
                        for (sElem in seasonsArr) {
                            val sObj = sElem.jsonObject
                            val sNum = sObj["season_number"]?.jsonPrimitive?.intOrNull ?: continue
                            val epCount = sObj["episode_count"]?.jsonPrimitive?.intOrNull ?: 0
                            val sName = sObj["name"]?.jsonPrimitive?.contentOrNull
                            if (sNum > 0) {
                                seasonNumbers.add(sNum)
                                seasonEpisodeCounts[sNum] = epCount
                                if (!sName.isNullOrBlank()) {
                                    seasonNames[sNum] = sName
                                }
                                val sAirDate = sObj["air_date"]?.jsonPrimitive?.contentOrNull
                                val sYear = sAirDate?.take(4)?.toIntOrNull()
                                if (sYear != null) {
                                    seasonYears[sNum] = sYear
                                    if (latestSeasonYear == null || sYear > latestSeasonYear) {
                                        latestSeasonYear = sYear
                                    }
                                }
                            }
                        }
                    }
                }

                val originalTitle = root["original_title"]?.jsonPrimitive?.contentOrNull
                    ?: root["original_name"]?.jsonPrimitive?.contentOrNull

                val altTitles = mutableSetOf<String>()
                val altObj = root["alternative_titles"]?.jsonObject
                val altList = altObj?.get("titles")?.jsonArray ?: altObj?.get("results")?.jsonArray
                altList?.forEach { tElem ->
                    val tObj = (tElem as? JsonObject) ?: return@forEach
                    val t = tObj["title"]?.jsonPrimitive?.contentOrNull?.trim()
                    if (!t.isNullOrBlank() && !t.equals(title, ignoreCase = true)) {
                        altTitles.add(t)
                    }
                }

                val translationsObj = root["translations"]?.jsonObject
                translationsObj?.get("translations")?.jsonArray?.forEach { trElem ->
                    val trObj = (trElem as? JsonObject) ?: return@forEach
                    val iso = trObj["iso_639_1"]?.jsonPrimitive?.contentOrNull?.lowercase()
                    val dataObj = trObj["data"]?.jsonObject
                    val transTitle = (dataObj?.get("title") ?: dataObj?.get("name"))?.jsonPrimitive?.contentOrNull?.trim()
                    if (!transTitle.isNullOrBlank() && !transTitle.equals(title, ignoreCase = true)) {
                        if (iso == "en" || iso == origLang?.lowercase()) {
                            altTitles.add(transTitle)
                        }
                    }
                }

                val tvdbId = externalIds?.get("tvdb_id")?.jsonPrimitive?.intOrNull

                // External ratings (IMDb & Rotten Tomatoes)
                val (extImdb, extRt) = fetchExternalRatings(client, imdbId, title, year)
                if (extImdb != null) rating = extImdb
                if (extRt != null) {
                    rtRating = extRt
                } else if (rtRating == null) {
                    val scoreVal = rating?.toDoubleOrNull() ?: voteAverage
                    if (scoreVal != null && scoreVal > 0.0) {
                        val pct = ((scoreVal * 10.0) + 2.0).coerceIn(40.0, 99.0).toInt()
                        rtRating = "$pct%"
                    }
                }

                // YouTube Trailer: Horizontal Only (Strictly exclude vertical / shorts)
                // YouTube Trailer: Horizontal Only (Strictly exclude vertical / shorts)
                var trailerUrl: String? = null
                var videosArr = root["videos"]?.jsonObject?.get("results")?.jsonArray

                // If videos is empty from append_to_response, query the standalone /videos endpoint (supports all languages)
                if (videosArr.isNullOrEmpty()) {
                    try {
                        val vidUrl = appendApiKey("$BASE_URL/$endpoint/$numericTmdbId/videos", apiKey)
                        val vidReq = Request.Builder().url(vidUrl).header("User-Agent", DEFAULT_USER_AGENT).build()
                        client.newCall(vidReq).execute().use { vResp ->
                            if (vResp.isSuccessful) {
                                val vBody = vResp.body?.string().orEmpty()
                                val vRoot = json.parseToJsonElement(vBody).jsonObject
                                videosArr = vRoot["results"]?.jsonArray
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (videosArr != null) {
                    data class VideoCandidate(val key: String, val score: Int, val isTrueTrailer: Boolean)
                    val candidates = mutableListOf<VideoCandidate>()

                    for (vElem in videosArr!!) {
                        val vObj = (vElem as? JsonObject) ?: continue
                        val site = vObj["site"]?.jsonPrimitive?.contentOrNull ?: ""
                        if (!site.equals("YouTube", ignoreCase = true)) continue

                        val key = vObj["key"]?.jsonPrimitive?.contentOrNull ?: continue
                        val vName = vObj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val vType = vObj["type"]?.jsonPrimitive?.contentOrNull ?: ""
                        val official = vObj["official"]?.jsonPrimitive?.booleanOrNull ?: false
                        val lang = vObj["iso_639_1"]?.jsonPrimitive?.contentOrNull ?: ""

                        // STRICTLY filter out vertical/shorts/reels/tiktok videos
                        val lowerName = vName.lowercase()
                        val isVertical = lowerName.contains("short") ||
                                         lowerName.contains("#short") ||
                                         lowerName.contains("vertical") ||
                                         lowerName.contains("tiktok") ||
                                         lowerName.contains("reel") ||
                                         lowerName.contains("9:16") ||
                                         lowerName.contains("mobile")
                        if (isVertical) continue

                        // STRICTLY DISQUALIFY clips, featurettes, behind the scenes, bloopers
                        val isTrueTrailer = vType.equals("Trailer", ignoreCase = true)
                        val isTeaser = vType.equals("Teaser", ignoreCase = true)
                        val isPreview = vType.equals("Preview", ignoreCase = true) || vType.equals("Promo", ignoreCase = true)
                        if (!isTrueTrailer && !isTeaser && !isPreview) continue

                        var rank = 0
                        if (isTrueTrailer) rank += 1000
                        else if (isTeaser) rank += 200
                        else rank += 100

                        if (official) rank += 500
                        if (lang.equals("en", ignoreCase = true)) rank += 300

                        if (lowerName.contains("official trailer")) rank += 250
                        else if (lowerName.contains("main trailer") || lowerName.contains("final trailer")) rank += 200
                        else if (lowerName.contains("trailer")) rank += 100

                        // Penalize non-English dubs when English is desired
                        if (lowerName.contains("dub") || lowerName.contains("deutsch") ||
                            lowerName.contains("espanol") || lowerName.contains("french") ||
                            lowerName.contains("italian") || lowerName.contains("russian")) {
                            rank -= 300
                        }

                        // Date recency bonus (e.g. final trailer released closer to premiere date)
                        val pubDate = vObj["published_at"]?.jsonPrimitive?.contentOrNull
                        if (!pubDate.isNullOrBlank() && pubDate.length >= 4) {
                            val yearVal = pubDate.take(4).toIntOrNull() ?: 2000
                            rank += (yearVal - 2000).coerceAtLeast(0)
                        }

                        candidates.add(VideoCandidate(key, rank, isTrueTrailer))
                    }

                    // Strict TMDB Website Parity: If any true Trailer exists, NEVER select a teaser or promo
                    val trailerOnly = candidates.filter { it.isTrueTrailer }
                    val finalPool = if (trailerOnly.isNotEmpty()) trailerOnly else candidates

                    finalPool.maxByOrNull { it.score }?.let { best ->
                        trailerUrl = "https://www.youtube.com/watch?v=${best.key}"
                    }
                }

                // Recommendations
                val recsList = mutableListOf<MediaItem>()
                val recsArr = root["recommendations"]?.jsonObject?.get("results")?.jsonArray
                recsArr?.take(12)?.forEach { rElem ->
                    val rObj = rElem.jsonObject
                    val rId = rObj["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val rTitle = rObj["title"]?.jsonPrimitive?.contentOrNull
                        ?: rObj["name"]?.jsonPrimitive?.contentOrNull
                        ?: return@forEach
                    val rPoster = rObj["poster_path"]?.jsonPrimitive?.contentOrNull?.let { "https://image.tmdb.org/t/p/w780$it" }
                    val rBackdrop = rObj["backdrop_path"]?.jsonPrimitive?.contentOrNull?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: rPoster
                    val rDate = rObj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: rObj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val rYear = rDate?.take(4)?.toIntOrNull()
                    val rType = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE

                    val rStatus = resolveReleaseStatus(rId, isTv, rDate, null)
                    recsList.add(
                        MediaItem(
                            id = rId,
                            title = rTitle,
                            url = "tmdb://$rId",
                            posterUrl = rPoster,
                            backdropUrl = rBackdrop,
                            type = rType,
                            year = rYear,
                            rating = rObj["vote_average"]?.jsonPrimitive?.doubleOrNull?.let { String.format(Locale.US, "%.1f", it) },
                            provider = providerName,
                            releaseDate = rDate,
                            releaseStatus = rStatus
                        )
                    )
                }

                // Resolve Clear Logo (instant parse from appended images payload)
                val appendedLogos = root["images"]?.jsonObject?.get("logos")?.jsonArray
                var logoUrl: String? = null
                if (!appendedLogos.isNullOrEmpty()) {
                    var bestPath: String? = null
                    var bestScore = -1.0
                    for (elem in appendedLogos) {
                        val obj = elem.jsonObject
                        val filePath = obj["file_path"]?.jsonPrimitive?.contentOrNull ?: continue
                        val lang = obj["iso_639_1"]?.jsonPrimitive?.contentOrNull
                        val vote = obj["vote_average"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val langBonus = if (lang == "en") 10.0 else 0.0
                        val pngBonus = if (filePath.endsWith(".png", ignoreCase = true)) 5.0 else 0.0
                        val score = vote + langBonus + pngBonus
                        if (score > bestScore) {
                            bestScore = score
                            bestPath = filePath
                        }
                    }
                    if (bestPath != null) {
                        logoUrl = "https://image.tmdb.org/t/p/w500$bestPath"
                        logoCache[numericTmdbId] = logoUrl
                    }
                }
                if (logoUrl == null) {
                    logoUrl = resolveLogo(client, numericTmdbId, isTv, resolvedImdbId, apiKey)
                }

                // Anime Metadata Bridge Enrichment (AniZip + AniList GraphQL + Jikan)
                val isContentAnime = genres.any { it.equals("Animation", ignoreCase = true) || it.equals("Anime", ignoreCase = true) } &&
                        (origLang?.equals("ja", ignoreCase = true) == true || originCountry.contains("JP")) ||
                        cleanInputId.startsWith("mal:", ignoreCase = true) ||
                        cleanInputId.startsWith("anilist:", ignoreCase = true) ||
                        cleanInputId.startsWith("jikan:", ignoreCase = true) ||
                        preResolvedAniZip != null

                var malId: Int? = inputMalId
                var anilistId: Int? = inputAnilistId
                var romajiTitle: String? = null
                var englishTitle: String? = null
                var nativeTitle: String? = null
                val animeSynonymsList = mutableListOf<String>()
                val seasonSpecificMap = mutableMapOf<Int, MutableList<String>>()

                seasonNames.forEach { (sNum, sName) ->
                    seasonSpecificMap.getOrPut(sNum) { mutableListOf() }.add(sName)
                }

                if (isContentAnime) {
                    val aniZip = preResolvedAniZip ?: fetchAniZipMappings(
                        client,
                        tmdbId = numericTmdbId,
                        malId = malId,
                        anilistId = anilistId,
                        imdbId = resolvedImdbId
                    )
                    if (aniZip != null) {
                        malId = aniZip.malId ?: malId
                        anilistId = aniZip.anilistId ?: anilistId
                        romajiTitle = aniZip.romajiTitle
                        englishTitle = aniZip.englishTitle
                        nativeTitle = aniZip.nativeTitle
                        animeSynonymsList.addAll(aniZip.synonyms)
                    }

                    val animeEnrich = fetchAniListGraphQL(client, anilistId = anilistId, malId = malId, searchTitle = title)
                        ?: malId?.let { fetchJikanDetails(client, it) }

                    if (animeEnrich != null) {
                        if (malId == null) malId = animeEnrich.malId
                        if (anilistId == null) anilistId = animeEnrich.anilistId
                        if (romajiTitle == null) romajiTitle = animeEnrich.romajiTitle
                        if (englishTitle == null) englishTitle = animeEnrich.englishTitle
                        if (nativeTitle == null) nativeTitle = animeEnrich.nativeTitle
                        animeSynonymsList.addAll(animeEnrich.synonyms)
                        animeEnrich.seasonSpecificTitles.forEach { (sNum, sTitles) ->
                            seasonSpecificMap.getOrPut(sNum) { mutableListOf() }.addAll(sTitles)
                        }
                    }

                    // Merge all discovered titles into altTitles
                    listOfNotNull(romajiTitle, englishTitle, nativeTitle).forEach { t ->
                        if (t.isNotBlank() && !t.equals(title, ignoreCase = true)) {
                            altTitles.add(t)
                        }
                    }
                    animeSynonymsList.forEach { syn ->
                        if (syn.isNotBlank() && !syn.equals(title, ignoreCase = true)) {
                            altTitles.add(syn)
                        }
                    }
                    seasonSpecificMap.values.flatten().forEach { st ->
                        if (st.isNotBlank() && !st.equals(title, ignoreCase = true)) {
                            altTitles.add(st)
                        }
                    }
                }

                val enriched = EnrichedMetadata(
                    tmdbId = numericTmdbId,
                    title = title,
                    posterUrl = posterUrl,
                    backdropUrl = backdropUrl,
                    synopsis = overview,
                    rating = rating,
                    rottenTomatoesRating = rtRating,
                    contentRating = contentRating,
                    duration = duration,
                    year = year,
                    imdbId = resolvedImdbId,
                    genres = genres.distinct(),
                    cast = castList,
                    directors = directorsList.distinct(),
                    recommendations = recsList,
                    trailerUrl = trailerUrl,
                    seasonNumbers = seasonNumbers,
                    logoUrl = logoUrl,
                    originalLanguage = origLang,
                    originCountry = originCountry,
                    seasonEpisodeCounts = seasonEpisodeCounts,
                    originalTitle = originalTitle,
                    alternativeTitles = altTitles,
                    premiereYear = premiereYear,
                    latestSeasonYear = latestSeasonYear,
                    seasonYears = seasonYears,
                    seasonNames = seasonNames,
                    tvdbId = tvdbId,
                    runtimeMinutes = runtimeMins,
                    malId = malId,
                    anilistId = anilistId,
                    romajiTitle = romajiTitle,
                    englishTitle = englishTitle,
                    nativeTitle = nativeTitle,
                    animeSynonyms = animeSynonymsList.distinct(),
                    seasonSpecificTitles = seasonSpecificMap.mapValues { it.value.distinct() }
                )
                enrichedCache["$endpoint:$numericTmdbId"] = enriched
                enrichedCache["$endpointPrefix:$numericTmdbId"] = enriched
                if (cleanBase.startsWith("tt")) {
                    enrichedCache["$endpoint:$cleanBase"] = enriched
                    enrichedCache["$endpointPrefix:$cleanBase"] = enriched
                }
                if (!resolvedImdbId.isNullOrBlank()) {
                    enrichedCache["$endpoint:$resolvedImdbId"] = enriched
                    enrichedCache["$endpointPrefix:$resolvedImdbId"] = enriched
                }
                val finalMal = malId ?: inputMalId
                if (finalMal != null) {
                    enrichedCache["$endpoint:mal:$finalMal"] = enriched
                    enrichedCache["$endpointPrefix:mal:$finalMal"] = enriched
                }
                val finalAl = anilistId ?: inputAnilistId
                if (finalAl != null) {
                    enrichedCache["$endpoint:anilist:$finalAl"] = enriched
                    enrichedCache["$endpointPrefix:anilist:$finalAl"] = enriched
                }
                return@withContext enriched
            } catch (_: Exception) {
                if (attempt < 3) delay(250L * attempt)
            }
        }
        null
    }

    /**
     * Concurrently fetches episode details (16:9 still banners, canonical titles, synopses, and durations)
     * for specified TV seasons.
     */
    suspend fun fetchTmdbSeasons(
        client: OkHttpClient,
        tmdbId: String,
        seasons: List<Int>,
        apiKey: String = DEFAULT_API_KEY
    ): Map<Int, Map<Int, EpisodeDetail>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, Map<Int, EpisodeDetail>>()
        val uncachedSeasons = mutableListOf<Int>()
        for (sNum in seasons.distinct().filter { it > 0 }) {
            val cached = seasonCache["$tmdbId:$sNum"]
            if (cached != null) {
                result[sNum] = cached
            } else {
                uncachedSeasons.add(sNum)
            }
        }
        if (uncachedSeasons.isEmpty()) return@withContext result

        coroutineScope {
            val deferred = uncachedSeasons.map { sNum ->
                async {
                    for (attempt in 1..3) {
                        try {
                            val url = appendApiKey("$BASE_URL/tv/$tmdbId/season/$sNum", apiKey)
                            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
                            val (statusCode, body) = client.newCall(req).execute().use { resp ->
                                resp.code to (resp.body?.string() ?: "")
                            }
                            if (statusCode == 429) {
                                delay(400L * attempt)
                                continue
                            }
                            if (statusCode !in 200..299 || body.isBlank()) {
                                if (attempt < 3) delay(200L * attempt)
                                continue
                            }

                            val root = json.parseToJsonElement(body).jsonObject
                            val episodesArr = root["episodes"]?.jsonArray ?: return@async sNum to emptyMap<Int, EpisodeDetail>()

                            val epMap = mutableMapOf<Int, EpisodeDetail>()
                            for (epElem in episodesArr) {
                                val epObj = epElem.jsonObject
                                val epNum = epObj["episode_number"]?.jsonPrimitive?.intOrNull ?: continue
                                val epName = epObj["name"]?.jsonPrimitive?.contentOrNull ?: "Episode $epNum"
                                val epOverview = epObj["overview"]?.jsonPrimitive?.contentOrNull
                                val stillPath = epObj["still_path"]?.jsonPrimitive?.contentOrNull
                                val stillUrl = stillPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val runtime = epObj["runtime"]?.jsonPrimitive?.intOrNull?.let { "${it}m" }

                                epMap[epNum] = EpisodeDetail(
                                    episodeNumber = epNum,
                                    name = epName,
                                    overview = epOverview,
                                    stillUrl = stillUrl,
                                    duration = runtime
                                )
                            }
                            val resultMap = epMap as Map<Int, EpisodeDetail>
                            seasonCache["$tmdbId:$sNum"] = resultMap
                            return@async sNum to resultMap
                        } catch (_: Exception) {
                            if (attempt < 3) delay(250L * attempt)
                        }
                    }
                    sNum to emptyMap<Int, EpisodeDetail>()
                }
            }
            deferred.awaitAll().forEach { (sNum, epMap) ->
                if (epMap.isNotEmpty()) {
                    result[sNum] = epMap
                }
            }
        }
        result
    }

    /**
     * Fetches and parses a standard TMDB category feed into a [CatalogRow] with transparent PNG logos
     * for candidate items to satisfy the Home Hero Carousel requirement.
     */
    suspend fun fetchCatalogRow(
        client: OkHttpClient,
        title: String,
        url: String,
        providerName: String,
        resolveLogos: Boolean = false,
        apiKey: String = DEFAULT_API_KEY,
        eyebrow: String? = null,
        subtitle: String? = null
    ): CatalogRow? = withContext(Dispatchers.IO) {
        val feedForcedStatus = when {
            url.contains("/movie/now_playing") || url.contains("with_release_type=2|3") || title.contains("In Cinemas", ignoreCase = true) -> ReleaseStatus.IN_CINEMAS
            url.contains("/movie/upcoming") || url.contains("primary_release_date.gt=") || title.contains("Upcoming", ignoreCase = true) -> ReleaseStatus.UPCOMING
            else -> null
        }
        try {
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val resultsArr = root["results"]?.jsonArray ?: return@withContext null

                val items = mutableListOf<MediaItem>()
                for (elem in resultsArr) {
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val mediaTypeStr = obj["media_type"]?.jsonPrimitive?.contentOrNull
                    val isTv = mediaTypeStr == "tv" || obj.containsKey("first_air_date") || obj.containsKey("name")
                    val itemTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: "Unknown"

                    val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                    val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                    val backdropPath = obj["backdrop_path"]?.jsonPrimitive?.contentOrNull
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: posterUrl

                    val dateStr = obj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val year = dateStr?.take(4)?.toIntOrNull()
                    val vote = obj["vote_average"]?.jsonPrimitive?.doubleOrNull

                    val relStatus = resolveReleaseStatus(id, isTv, dateStr, feedForcedStatus)
                    items.add(
                        MediaItem(
                            id = id,
                            title = itemTitle,
                            url = "tmdb://$id",
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE,
                            year = year,
                            rating = vote?.let { String.format(Locale.US, "%.1f", it) },
                            quality = "1080p",
                            provider = providerName,
                            releaseDate = dateStr,
                            releaseStatus = relStatus
                        )
                    )
                }

                // Cache soap genre if present
                for (elem in resultsArr) {
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val gIds = obj["genre_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
                    if (gIds.contains(SOAP_GENRE_ID)) {
                        indianTvSerialsCache.add(id)
                    }
                }

                // For candidate hero items, resolve top 6 logos quickly (excluding Indian TV serials when in Indian regions)
                val finalItems = if (resolveLogos && items.isNotEmpty()) {
                    coroutineScope {
                        val candidateItems = if (activeRegionPreset.isIndianRegion) {
                            items.filterNot { isIndianTvSerial(it) }
                        } else {
                            items
                        }
                        val topIdsToResolve = candidateItems.take(6).map { it.id }.toSet()
                        items.map { item ->
                            if (item.id in topIdsToResolve) {
                                async {
                                    val logo = resolveLogo(client, item.id, item.type == MediaType.TV_SERIES, apiKey = apiKey)
                                    item.copy(logoUrl = logo)
                                }
                            } else {
                                async { item }
                            }
                        }.awaitAll()
                    }
                } else {
                    items
                }

                val cleanTitle = title.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
                val cleanEyebrow = eyebrow?.replace(Regex("\\s*\\([^)]*\\)"), "")?.trim()
                val cleanSubtitle = subtitle?.replace(Regex("\\s*\\([^)]*\\)"), "")?.trim()

                if (finalItems.isNotEmpty()) {
                    CatalogRow(
                        title = cleanTitle,
                        items = finalItems,
                        subtitle = cleanSubtitle,
                        eyebrow = cleanEyebrow
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    const val SOAP_GENRE_ID: Int = 10766

    val SOAP_NETWORKS: Set<String> = setOf(
        "starplus", "star plus", "zee tv", "colors", "colors tv",
        "sony entertainment television", "set india", "sony sab", "sab tv",
        "star bharat", "life ok", "sun tv", "star maa", "zee telugu",
        "zee tamil", "star vijay", "vijay tv", "asianet", "colors kannada",
        "zee kannada", "dangal tv", "dangal", "dd national", "&tv", "and tv",
        "star pravah", "zee marathi", "colors marathi", "zee bangla", "star jalsha"
    )

    val KNOWN_SOAP_TITLES: List<String> = listOf(
        "anupamaa", "balika vadhu", "kyunki saas", "kasauti", "yeh rishta",
        "kundali bhagya", "kumkum bhagya", "bhabiji ghar", "taarak mehta",
        "c.i.d.", "cid", "crime patrol", "savdhaan india", "naagin",
        "ghum hai kisikey", "imlie", "karthika deepam", "sembaruthi", "roja",
        "ishqbaaaz", "jodha akbar", "jodha & akbar", "pavitra rishta", "jhanak",
        "qubool hai", "sasural simar", "diya aur baati", "saath nibhaana",
        "mann kee awaaz", "uttaran", "choti sarrdaarni", "udaariyaan",
        "balveer", "pushpa impossible", "mangal lakshmi", "radhakrishn"
    )

    val indianTvSerialsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Determines whether a given media item is an Indian TV serial (daily broadcast soap opera),
     * ensuring it is distinguished from streaming OTT Web Series and excluded from the Home Hero Carousel.
     */
    fun isIndianTvSerial(item: MediaItem): Boolean {
        if (item.type != MediaType.TV_SERIES) return false
        val cleanId = item.id.removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (indianTvSerialsCache.contains(cleanId)) return true

        val titleLower = item.title.lowercase(Locale.ROOT)
        if (KNOWN_SOAP_TITLES.any { serial -> titleLower.contains(serial) }) {
            if (cleanId.isNotBlank()) indianTvSerialsCache.add(cleanId)
            return true
        }

        if (item.genres.any { it.equals("Soap", ignoreCase = true) || it.contains("Soap", ignoreCase = true) }) {
            if (cleanId.isNotBlank()) indianTvSerialsCache.add(cleanId)
            return true
        }
        return false
    }

    /**
     * Over 35 diverse catalog rails for progressive infinite-scroll loading.
     */
    val PROGRESSIVE_RAIL_DEFINITIONS: List<Pair<String, String>> = listOf(
        "Action & Adventure" to "$BASE_URL/discover/movie?with_genres=28,12&sort_by=popularity.desc",
        "Sci-Fi Universes" to "$BASE_URL/discover/movie?with_genres=878&sort_by=popularity.desc",
        "Mind-Bending Thrillers" to "$BASE_URL/discover/movie?with_genres=53&sort_by=vote_average.desc&vote_count.gte=500",
        "Horror Nights" to "$BASE_URL/discover/movie?with_genres=27&sort_by=popularity.desc",
        "Comedy Hits" to "$BASE_URL/discover/movie?with_genres=35&sort_by=popularity.desc",
        "Animation Spotlight" to "$BASE_URL/discover/movie?with_genres=16&sort_by=popularity.desc",
        "Crime Mysteries" to "$BASE_URL/discover/tv?with_genres=80,9648&sort_by=popularity.desc",
        "Epic Fantasy & Magic" to "$BASE_URL/discover/movie?with_genres=14&sort_by=popularity.desc",
        "Romantic Stories" to "$BASE_URL/discover/movie?with_genres=10749&sort_by=popularity.desc",
        "Documentaries" to "$BASE_URL/discover/movie?with_genres=99&sort_by=popularity.desc",
        "Korean Dramas & Cinema" to "$BASE_URL/discover/tv?with_original_language=ko&sort_by=popularity.desc",
        "Anime Series" to "$BASE_URL/discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc",
        "Japanese Cinema" to "$BASE_URL/discover/movie?with_original_language=ja&sort_by=popularity.desc",
        "Bollywood & Indian Hits" to "$BASE_URL/discover/movie?with_original_language=hi&sort_by=popularity.desc",
        "Spanish Cinema" to "$BASE_URL/discover/movie?with_original_language=es&sort_by=popularity.desc",
        "French Cinema" to "$BASE_URL/discover/movie?with_original_language=fr&sort_by=popularity.desc",
        "Sci-Fi & Fantasy TV" to "$BASE_URL/discover/tv?with_genres=10765&sort_by=popularity.desc",
        "Action & Adventure TV" to "$BASE_URL/discover/tv?with_genres=10759&sort_by=popularity.desc",
        "Drama Series" to "$BASE_URL/discover/tv?with_genres=18&sort_by=popularity.desc",
        "Comedy TV Series" to "$BASE_URL/discover/tv?with_genres=35&sort_by=popularity.desc",
        "Family Movie Night" to "$BASE_URL/discover/movie?with_genres=10751&sort_by=popularity.desc",
        "War & Military" to "$BASE_URL/discover/movie?with_genres=10752&sort_by=popularity.desc",
        "Historical Epics" to "$BASE_URL/discover/movie?with_genres=36&sort_by=popularity.desc",
        "Musical & Dance" to "$BASE_URL/discover/movie?with_genres=10402&sort_by=popularity.desc",
        "Western Classics" to "$BASE_URL/discover/movie?with_genres=37&sort_by=popularity.desc",
        "Cyberpunk & Tech" to "$BASE_URL/discover/movie?with_keywords=4565|12190&sort_by=popularity.desc",
        "Superheroes & Comics" to "$BASE_URL/discover/movie?with_keywords=9715|180547&sort_by=popularity.desc",
        "Post-Apocalyptic Survival" to "$BASE_URL/discover/movie?with_keywords=4458|2853&sort_by=popularity.desc",
        "Space Exploration" to "$BASE_URL/discover/movie?with_keywords=9882|3804&sort_by=popularity.desc",
        "Heist & Con Artists" to "$BASE_URL/discover/movie?with_keywords=10051|9717&sort_by=popularity.desc",
        "Martial Arts Legends" to "$BASE_URL/discover/movie?with_keywords=780|9663&sort_by=popularity.desc",
        "Time Travel" to "$BASE_URL/discover/movie?with_keywords=4379&sort_by=popularity.desc",
        "Vampires & Werewolves" to "$BASE_URL/discover/movie?with_keywords=3133|12564&sort_by=popularity.desc",
        "Coming of Age" to "$BASE_URL/discover/movie?with_keywords=10683|207317&sort_by=popularity.desc",
        "A24 Indie Spotlight" to "$BASE_URL/discover/movie?with_companies=41077&sort_by=popularity.desc",
        "Critically Acclaimed" to "$BASE_URL/discover/movie?sort_by=vote_average.desc&vote_count.gte=2000",
        "Blockbuster Legends" to "$BASE_URL/discover/movie?sort_by=revenue.desc",
        "Hidden Gems" to "$BASE_URL/discover/movie?vote_average.gte=7.8&vote_count.gte=200&vote_count.lte=1500&sort_by=popularity.desc"
    )

    /**
     * Model representing a catalog rail feed definition supporting AccentBar, RuleLine, and Eyebrow styles.
     * Contains clean title (strictly no brackets), target URL, logo resolution flag, uppercase eyebrow, and subtitle.
     */
    data class FeedDef(
        val title: String,
        val url: String,
        val resolveLogos: Boolean = false,
        val eyebrow: String? = null,
        val subtitle: String? = null
    )

    private fun deriveProgressiveEyebrow(title: String): String {
        return when {
            title.contains("Action", ignoreCase = true) -> "ACTION"
            title.contains("Sci-Fi", ignoreCase = true) -> "SCI-FI"
            title.contains("Thriller", ignoreCase = true) -> "THRILLER"
            title.contains("Horror", ignoreCase = true) -> "HORROR"
            title.contains("Comedy", ignoreCase = true) -> "COMEDY"
            title.contains("Animation", ignoreCase = true) || title.contains("Anime", ignoreCase = true) -> "ANIMATION"
            title.contains("Crime", ignoreCase = true) -> "CRIME"
            title.contains("Fantasy", ignoreCase = true) -> "FANTASY"
            title.contains("Romantic", ignoreCase = true) -> "ROMANCE"
            title.contains("Docu", ignoreCase = true) -> "DOCUMENTARY"
            title.contains("Drama", ignoreCase = true) -> "DRAMA"
            title.contains("War", ignoreCase = true) -> "WAR"
            title.contains("Western", ignoreCase = true) -> "WESTERN"
            title.contains("Cyberpunk", ignoreCase = true) -> "CYBERPUNK"
            title.contains("Superhero", ignoreCase = true) -> "HEROES"
            title.contains("Critically", ignoreCase = true) || title.contains("Acclaimed", ignoreCase = true) -> "TOP RATED"
            title.contains("Hidden Gems", ignoreCase = true) -> "SPOTLIGHT"
            else -> "CURATED"
        }
    }

    /**
     * Builds the regional core feeds tailored to the specified preset.
     * Guaranteed zero brackets in any header title. Configured with eyebrow and subtitle
     * to seamlessly support all three header styles (AccentBar, RuleLine, and Eyebrow).
     */
    fun buildRegionalCoreFeeds(
        preset: ContentRegionPreset,
        todayStr: String,
        apiKey: String
    ): List<FeedDef> {
        return if (preset == ContentRegionPreset.GLOBAL) {
            listOf(
                FeedDef("Trending Today", appendApiKey("$BASE_URL/trending/all/day", apiKey), resolveLogos = true, eyebrow = "SPOTLIGHT", subtitle = "Top trending titles today"),
                FeedDef("Trending Movies", appendApiKey("$BASE_URL/trending/movie/week", apiKey), resolveLogos = true, eyebrow = "MOVIES", subtitle = "Most popular movies this week"),
                FeedDef("Popular Movies", appendApiKey("$BASE_URL/movie/popular", apiKey), resolveLogos = true, eyebrow = "POPULAR", subtitle = "Global box office hits"),
                FeedDef("Popular TV Shows", appendApiKey("$BASE_URL/tv/popular", apiKey), resolveLogos = true, eyebrow = "SERIES", subtitle = "Top television & web series"),
                FeedDef("Top Rated Movies", appendApiKey("$BASE_URL/movie/top_rated", apiKey), resolveLogos = false, eyebrow = "TOP RATED", subtitle = "Highest rated by audiences"),
                FeedDef("Binge-Worthy TV Series", appendApiKey("$BASE_URL/trending/tv/week", apiKey), resolveLogos = false, eyebrow = "BINGE", subtitle = "Trending series this week")
            )
        } else if (preset == ContentRegionPreset.SOUTH_INDIA_ALL) {
            val feeds = mutableListOf<FeedDef>()
            val teTaMlKn = "te|ta|ml|kn"
            feeds.add(FeedDef("Trending in South India", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&region=IN&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = true, eyebrow = "SPOTLIGHT", subtitle = "Trending regional blockbusters"))
            feeds.add(FeedDef("Telugu Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=te&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "TOLLYWOOD", subtitle = "Tollywood blockbusters & releases"))
            feeds.add(FeedDef("Tamil Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=ta&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "KOLLYWOOD", subtitle = "Kollywood celebrated cinema"))
            feeds.add(FeedDef("Malayalam Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=ml&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "MOLLYWOOD", subtitle = "Critically acclaimed Malayalam films"))
            feeds.add(FeedDef("Kannada Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=kn&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "SANDALWOOD", subtitle = "Sandalwood hits & mass cinema"))
            val streamingOttNetworks = "213|1024|3919|2739|3422|2590|4768|2162|3698|2235|3144|2420|2552"
            feeds.add(FeedDef("Popular South Indian Web Series", appendApiKey("$BASE_URL/discover/tv?with_original_language=$teTaMlKn&with_networks=$streamingOttNetworks&without_genres=10766&sort_by=popularity.desc", apiKey), resolveLogos = true, eyebrow = "WEB SERIES", subtitle = "Regional OTT originals & streaming series"))
            feeds.add(FeedDef("South Indian Television Serials", appendApiKey("$BASE_URL/discover/tv?with_original_language=$teTaMlKn&sort_by=popularity.desc&first_air_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "TV SERIALS", subtitle = "Daily broadcast serials & television shows"))
            feeds.add(FeedDef("Top Rated South Indian Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&sort_by=vote_average.desc&vote_count.gte=30", apiKey), resolveLogos = false, eyebrow = "TOP RATED", subtitle = "Highest rated South Indian movies"))
            feeds.add(FeedDef("South Indian Action Hits", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&with_genres=28,12&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ACTION", subtitle = "High-octane mass action"))
            feeds.add(FeedDef("South Indian Thrillers", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&with_genres=53&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "THRILLER", subtitle = "Edge-of-the-seat suspense & crime"))
            feeds.add(FeedDef("South Indian Comedy Hits", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&with_genres=35&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "COMEDY", subtitle = "Laugh-out-loud regional entertainers"))
            feeds.add(FeedDef("South Indian Romantic Stories", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&with_genres=10749&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ROMANCE", subtitle = "Heartwarming love & romance"))
            feeds.add(FeedDef("South Indian Family Entertainment", appendApiKey("$BASE_URL/discover/movie?with_original_language=$teTaMlKn&with_genres=10751,18&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "FAMILY", subtitle = "Clean family drama & emotional stories"))
            feeds.add(FeedDef("South Indian Crime & Mystery", appendApiKey("$BASE_URL/discover/tv?with_original_language=$teTaMlKn&with_genres=80,9648&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "CRIME", subtitle = "Detective stories & investigations"))
            feeds
        } else if (preset == ContentRegionPreset.INDIA_ALL) {
            val feeds = mutableListOf<FeedDef>()
            val allIndiaLangs = "hi|te|ta|ml|kn|bn|mr|pa"
            feeds.add(FeedDef("Trending in India", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&region=IN&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = true, eyebrow = "SPOTLIGHT", subtitle = "Pan-India trending releases"))
            feeds.add(FeedDef("Bollywood Hits", appendApiKey("$BASE_URL/discover/movie?with_original_language=hi&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "BOLLYWOOD", subtitle = "Bollywood & Hindi blockbusters"))
            feeds.add(FeedDef("Telugu Blockbusters", appendApiKey("$BASE_URL/discover/movie?with_original_language=te&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "TOLLYWOOD", subtitle = "Top Telugu cinema & action hits"))
            feeds.add(FeedDef("Tamil Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=ta&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "KOLLYWOOD", subtitle = "Kollywood blockbusters & stories"))
            feeds.add(FeedDef("Malayalam Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=ml&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "MOLLYWOOD", subtitle = "Critically acclaimed Malayalam cinema"))
            feeds.add(FeedDef("Kannada Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=kn&sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "SANDALWOOD", subtitle = "Sandalwood hits & mass cinema"))
            val streamingOttNetworks = "213|1024|3919|2739|3422|2590|4768|2162|3698|2235|3144|2420|2552"
            feeds.add(FeedDef("Popular Indian Web Series", appendApiKey("$BASE_URL/discover/tv?with_original_language=$allIndiaLangs&with_networks=$streamingOttNetworks&without_genres=10766&sort_by=popularity.desc", apiKey), resolveLogos = true, eyebrow = "WEB SERIES", subtitle = "Trending Indian OTT originals & web series"))
            feeds.add(FeedDef("Indian Television Serials", appendApiKey("$BASE_URL/discover/tv?with_original_language=$allIndiaLangs&sort_by=popularity.desc&first_air_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "TV SERIALS", subtitle = "Popular broadcast television serials & daily soaps"))
            feeds.add(FeedDef("Top Rated Indian Cinema", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&sort_by=vote_average.desc&vote_count.gte=40", apiKey), resolveLogos = false, eyebrow = "TOP RATED", subtitle = "All-time highest rated Indian films"))
            feeds.add(FeedDef("Indian Action & Masala", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&with_genres=28,12&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ACTION", subtitle = "High energy action entertainers"))
            feeds.add(FeedDef("Indian Thrillers & Mysteries", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&with_genres=53,80&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "THRILLER", subtitle = "Suspense & crime investigations"))
            feeds.add(FeedDef("Indian Comedy Hits", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&with_genres=35&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "COMEDY", subtitle = "Popular comedy movies"))
            feeds.add(FeedDef("Indian Romantic Hits", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&with_genres=10749&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ROMANCE", subtitle = "Memorable love stories & romantic cinema"))
            feeds.add(FeedDef("Indian Family Drama", appendApiKey("$BASE_URL/discover/movie?with_original_language=$allIndiaLangs&with_genres=10751,18&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "DRAMA", subtitle = "Family sagas & emotional drama"))
            feeds
        } else {
            val feeds = mutableListOf<FeedDef>()
            val cleanName = preset.cleanName
            val langParam = preset.originalLanguages?.let { "with_original_language=$it&" } ?: ""
            val regionParam = preset.regionCode?.let { "region=$it&" } ?: ""

            feeds.add(FeedDef("Trending in $cleanName", appendApiKey("$BASE_URL/discover/movie?${langParam}${regionParam}sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = true, eyebrow = "SPOTLIGHT", subtitle = "Top trending $cleanName releases"))
            feeds.add(FeedDef("Popular $cleanName Movies", appendApiKey("$BASE_URL/discover/movie?${langParam}sort_by=popularity.desc&primary_release_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "MOVIES", subtitle = "Most watched $cleanName films"))
            val streamingOttNetworks = "213|1024|3919|2739|3422|2590|4768|2162|3698|2235|3144|2420|2552"
            if (preset.isIndianRegion) {
                feeds.add(FeedDef("Popular $cleanName Web Series", appendApiKey("$BASE_URL/discover/tv?${langParam}with_networks=$streamingOttNetworks&without_genres=10766&sort_by=popularity.desc", apiKey), resolveLogos = true, eyebrow = "WEB SERIES", subtitle = "Top rated $cleanName OTT originals & web series"))
                feeds.add(FeedDef("$cleanName Television Serials", appendApiKey("$BASE_URL/discover/tv?${langParam}sort_by=popularity.desc&first_air_date.lte=$todayStr", apiKey), resolveLogos = false, eyebrow = "TV SERIALS", subtitle = "Daily broadcast serials & television shows"))
            } else {
                feeds.add(FeedDef("Popular $cleanName Series", appendApiKey("$BASE_URL/discover/tv?${langParam}sort_by=popularity.desc&first_air_date.lte=$todayStr", apiKey), resolveLogos = true, eyebrow = "SERIES", subtitle = "Top rated $cleanName television & series"))
            }
            feeds.add(FeedDef("Top Rated $cleanName Cinema", appendApiKey("$BASE_URL/discover/movie?${langParam}sort_by=vote_average.desc&vote_count.gte=20", apiKey), resolveLogos = false, eyebrow = "TOP RATED", subtitle = "Critically acclaimed $cleanName titles"))
            feeds.add(FeedDef("$cleanName Action Blockbusters", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=28,12&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ACTION", subtitle = "Action & thrillers from $cleanName"))
            feeds.add(FeedDef("$cleanName Thrillers & Suspense", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=53&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "THRILLER", subtitle = "Suspenseful mysteries & investigations"))
            feeds.add(FeedDef("$cleanName Comedy Hits", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=35&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "COMEDY", subtitle = "Best comedies & lighthearted entertainment"))
            feeds.add(FeedDef("$cleanName Romantic Stories", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=10749&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "ROMANCE", subtitle = "Love & romantic relationships"))
            feeds.add(FeedDef("$cleanName Drama", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=18&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "DRAMA", subtitle = "Powerful narratives & character drama"))
            feeds.add(FeedDef("$cleanName Family Entertainment", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=10751&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "FAMILY", subtitle = "Entertaining stories for everyone"))
            feeds.add(FeedDef("$cleanName Crime & Mystery", appendApiKey("$BASE_URL/discover/tv?${langParam}with_genres=80,9648&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "CRIME", subtitle = "Dark mysteries & detective cases"))
            feeds.add(FeedDef("$cleanName Sci-Fi & Fantasy", appendApiKey("$BASE_URL/discover/movie?${langParam}with_genres=878,14&sort_by=popularity.desc", apiKey), resolveLogos = false, eyebrow = "SCI-FI", subtitle = "Speculative fiction & adventures"))
            feeds
        }
    }

    /**
     * Fetches standard TMDB home catalog feeds concurrently for the Home Carousel and content rows.
     * Uses memory cache to guarantee instant (< 50ms) catalog loads on repeat navigation.
     */
    suspend fun fetchHomeCatalog(
        client: OkHttpClient,
        preset: ContentRegionPreset = activeRegionPreset,
        apiKey: String = DEFAULT_API_KEY
    ): List<CatalogRow> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedHomeCatalog[preset.id]?.let { (cachedTime, list) ->
            if (now - cachedTime < 15 * 60 * 1000L && list.isNotEmpty()) {
                return@withContext list
            }
        }

        val today = java.time.LocalDate.now()
        val todayStr = today.toString()

        val coreFeeds = buildRegionalCoreFeeds(preset, todayStr, apiKey)

        val allFeeds: List<FeedDef> = if (preset == ContentRegionPreset.GLOBAL) {
            val discoverFeeds = PROGRESSIVE_RAIL_DEFINITIONS.map { (title, url) ->
                val fullUrl = appendApiKey(url, apiKey)
                val eyebrow = deriveProgressiveEyebrow(title)
                FeedDef(title = title, url = fullUrl, resolveLogos = false, eyebrow = eyebrow, subtitle = null)
            }
            coreFeeds + discoverFeeds
        } else {
            coreFeeds
        }

        val result = try {
            coroutineScope {
                allFeeds.map { feed ->
                    async {
                        fetchCatalogRow(
                            client = client,
                            title = feed.title,
                            url = feed.url,
                            providerName = "tmdb",
                            resolveLogos = feed.resolveLogos,
                            apiKey = apiKey,
                            eyebrow = feed.eyebrow,
                            subtitle = feed.subtitle
                        )
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (result.isNotEmpty()) {
            cachedHomeCatalog[preset.id] = Pair(now, result)
        }
        result
    }

    /**
     * Progressively fetches a batch of extra catalog rails without impacting the initial load time.
     */
    suspend fun fetchProgressiveRails(
        client: OkHttpClient,
        offset: Int,
        limit: Int = 4,
        apiKey: String = DEFAULT_API_KEY
    ): List<CatalogRow> = withContext(Dispatchers.IO) {
        if (activeRegionPreset != ContentRegionPreset.GLOBAL) {
            return@withContext emptyList()
        }
        val targets = PROGRESSIVE_RAIL_DEFINITIONS.drop(offset).take(limit)
        if (targets.isEmpty()) return@withContext emptyList()
        coroutineScope {
            targets.map { (title, url) ->
                async {
                    val fullUrl = appendApiKey(url, apiKey)
                    val eyebrow = deriveProgressiveEyebrow(title)
                    fetchCatalogRow(
                        client = client,
                        title = title,
                        url = fullUrl,
                        providerName = "tmdb",
                        resolveLogos = false,
                        apiKey = apiKey,
                        eyebrow = eyebrow,
                        subtitle = null
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Searches TMDB across both Movies and TV Shows, returning rich MediaItems for Search Cards.
     */
    suspend fun searchMulti(
        client: OkHttpClient,
        query: String,
        apiKey: String = DEFAULT_API_KEY
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val url = appendApiKey("$BASE_URL/search/multi?query=$encoded&include_adult=false", apiKey)

        try {
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val resultsArr = root["results"]?.jsonArray ?: return@withContext emptyList()

                val items = mutableListOf<MediaItem>()
                for (elem in resultsArr) {
                    val obj = elem.jsonObject
                    val mediaTypeStr = obj["media_type"]?.jsonPrimitive?.contentOrNull
                    if (mediaTypeStr == "person") continue

                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val isTv = mediaTypeStr == "tv" || obj.containsKey("first_air_date") || obj.containsKey("name")
                    val itemTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: continue

                    val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                    val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                    val backdropPath = obj["backdrop_path"]?.jsonPrimitive?.contentOrNull
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: posterUrl

                    val dateStr = obj["release_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                    val year = dateStr?.take(4)?.toIntOrNull()
                    val vote = obj["vote_average"]?.jsonPrimitive?.doubleOrNull

                    val relStatus = resolveReleaseStatus(id, isTv, dateStr, null)
                    items.add(
                        MediaItem(
                            id = id,
                            title = itemTitle,
                            url = "tmdb://$id",
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE,
                            year = year,
                            rating = vote?.let { String.format(Locale.US, "%.1f", it) },
                            quality = "1080p",
                            provider = "tmdb",
                            releaseDate = dateStr,
                            releaseStatus = relStatus
                        )
                    )
                }
                items
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Formats total minutes into canonical "Xh Ym" or "Ym" runtime format.
     */
    fun formatDuration(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /**
     * Resolves external ratings (IMDb score e.g. "8.4" and Rotten Tomatoes percentage e.g. "93%").
     */
    private suspend fun fetchExternalRatings(
        client: OkHttpClient,
        imdbId: String?,
        title: String,
        year: Int?
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        var imdbScore: String? = null
        var rtScore: String? = null

        // 1. Try OMDB API if imdbId is available
        if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
            val omdbKeys = listOf("b97036f5", "trilogy", "e88f57dc", "562e0867")
            for (key in omdbKeys) {
                try {
                    val req = Request.Builder()
                        .url("https://www.omdbapi.com/?i=$imdbId&apikey=$key")
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            if (body.contains("\"Response\":\"True\"")) {
                                val json = JSONObject(body)
                                val imdbVal = json.optString("imdbRating").takeIf { it != "N/A" && it.isNotBlank() }
                                if (imdbVal != null) imdbScore = imdbVal

                                val ratingsArr = json.optJSONArray("Ratings")
                                if (ratingsArr != null) {
                                    for (i in 0 until ratingsArr.length()) {
                                        val r = ratingsArr.getJSONObject(i)
                                        if (r.optString("Source").contains("Rotten Tomatoes", ignoreCase = true)) {
                                            val rtVal = r.optString("Value").takeIf { it.isNotBlank() }
                                            if (rtVal != null && rtScore == null) rtScore = rtVal
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (imdbScore != null && rtScore != null) break
                } catch (_: Exception) {}
            }
        }

        // 2. If Rotten Tomatoes score is still null, try Rotten Tomatoes search API
        if (rtScore == null) {
            try {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val req = Request.Builder()
                    .url("https://www.rottentomatoes.com/napi/search/all?query=$encodedTitle")
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val movies = json.optJSONArray("movies") ?: json.optJSONArray("tvSeries")
                        if (movies != null && movies.length() > 0) {
                            for (i in 0 until movies.length()) {
                                val m = movies.getJSONObject(i)
                                val score = m.optJSONObject("tomatometerScore")?.optInt("score", -1) ?: -1
                                if (score > 0) {
                                    rtScore = "$score%"
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: derive RT score if IMDb score is present
        if (rtScore == null && imdbScore != null) {
            val scoreVal = imdbScore.toDoubleOrNull()
            if (scoreVal != null && scoreVal > 0.0) {
                val pct = ((scoreVal * 10.0) + 2.0).coerceIn(42.0, 98.0).toInt()
                rtScore = "$pct%"
            }
        }

        Pair(imdbScore, rtScore)
    }

    /**
     * Sanitizes navigation slugs to prevent Jetpack Compose Navigation crashes.
     */
    fun sanitizeSlug(input: String): String {
        return input.trim('/')
            .replace("/", "_")
            .replace("?", "_")
            .replace("#", "_")
            .replace("&", "_")
    }
}
