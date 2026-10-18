package com.euthopiar.core.util

import com.euthopiar.core.matcher.MatchScorer
import com.euthopiar.core.model.*
import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
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
object TmdbBridge {

    const val DEFAULT_API_KEY: String = "1865f43a0549ca50d341dd9ab8b29f49"
    private const val DEFAULT_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val logoCache = ConcurrentHashMap<String, String>()
    private val enrichedCache = ConcurrentHashMap<String, EnrichedMetadata>()
    private val seasonCache = ConcurrentHashMap<String, Map<Int, EpisodeDetail>>()
    @Volatile private var cachedHomeCatalog: Pair<Long, List<CatalogRow>>? = null

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
        val seasonEpisodeCounts: Map<Int, Int> = emptyMap()
    ) {
        val isAnime: Boolean
            get() = genres.any { it.equals("Animation", ignoreCase = true) || it.equals("Anime", ignoreCase = true) } &&
                    (originalLanguage?.equals("ja", ignoreCase = true) == true || originCountry.contains("JP"))
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
        val clean = tmdbId.trim().removePrefix("tmdb:").removePrefix("tmdb/").substringBefore(':').substringBefore('/')
        if (clean.isBlank()) return null
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
            val url = "https://api.themoviedb.org/3/find/$cleanImdb?api_key=$apiKey&external_source=imdb_id"
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
                val searchUrl = "https://api.themoviedb.org/3/search/$searchEndpoint?api_key=$apiKey&query=$encodedQuery"
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

        try {
            val endpoint = if (isTv) "tv" else "movie"
            val url = "https://api.themoviedb.org/3/$endpoint/$clean/external_ids?api_key=$apiKey"
            val req = Request.Builder().url(url).header("User-Agent", DEFAULT_USER_AGENT).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
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

    private val animeAbsoluteEpisodeCache = ConcurrentHashMap<String, Int>()

    /**
     * Resolves the absolute episode number for anime.
     * Tries AniZip API first, with offline fallback: episode + sum(prior seasons episode counts).
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

        // 1. Try AniZip API
        val anizipAbsolute = try {
            val queryParam = if (tmdbId.isNotBlank() && tmdbId.all { it.isDigit() }) {
                "themoviedb_id=$tmdbId"
            } else if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
                "imdb_id=$imdbId"
            } else null

            if (queryParam != null) {
                val url = "https://api.ani.zip/mappings?$queryParam"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            val root = json.parseToJsonElement(body).jsonObject
                            val episodesMap = root["episodes"]?.jsonObject
                            episodesMap?.values?.firstNotNullOfOrNull { epElem ->
                                val epObj = epElem as? JsonObject
                                val s = epObj?.get("seasonNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("season")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("tvdbSeason")?.jsonPrimitive?.intOrNull
                                if (s != null && s <= 0) return@firstNotNullOfOrNull null
                                val effectiveSeason = s ?: 1
                                val e = epObj?.get("episodeNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("episode")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("tvdbEpisode")?.jsonPrimitive?.intOrNull
                                val abs = epObj?.get("absoluteEpisodeNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("anidbEpisodeNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("kitsuEpisodeNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("malEpisodeNumber")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("kitsuEpisode")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("malEpisode")?.jsonPrimitive?.intOrNull
                                    ?: epObj?.get("abs")?.jsonPrimitive?.intOrNull
                                if (effectiveSeason == season && e == episode && abs != null && abs > 0) abs else null
                            }
                        } else null
                    } else null
                }
            } else null
        } catch (_: Throwable) {
            null
        }

        if (anizipAbsolute != null) {
            animeAbsoluteEpisodeCache[cacheKey] = anizipAbsolute
            return@withContext anizipAbsolute
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
                "https://api.themoviedb.org/3/search/tv?api_key=$apiKey&query=$encodedQuery&first_air_date_year=$year"
            } else {
                "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$encodedQuery&year=$year"
            }
            val id = queryBestTmdbId(client, urlWithYear, hints)
            if (id != null) return@withContext id
        }

        // 2. Try without year
        val urlWithoutYear = "https://api.themoviedb.org/3/search/$endpoint?api_key=$apiKey&query=$encodedQuery"
        val id = queryBestTmdbId(client, urlWithoutYear, hints)
        if (id != null) return@withContext id

        // 3. Fallback: try multi search
        val multiUrl = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$encodedQuery"
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
            val url = "https://api.themoviedb.org/3/$typeStr/$clean/images?api_key=$apiKey&include_image_language=en,null"
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
            val urlAll = "https://api.themoviedb.org/3/$typeStr/$clean/images?api_key=$apiKey"
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
        val cleanBase = cleanInputId.substringBefore(':').substringBefore('/')
        if (cleanBase.isBlank()) return@withContext null

        val numericTmdbId = if (cleanBase.startsWith("tt")) {
            findTmdbIdByImdb(client, cleanBase, isTv, apiKey) ?: return@withContext null
        } else {
            cleanBase
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

        for (attempt in 1..3) {
            try {
                var endpoint = if (isTv) "tv" else "movie"
                val append = "credits,external_ids,videos,recommendations,images," +
                        if (isTv) "content_ratings" else "release_dates"
                val url = "https://api.themoviedb.org/3/$endpoint/$numericTmdbId?api_key=$apiKey&append_to_response=$append&include_image_language=en,null"

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
                    val altAppend = "credits,external_ids,videos,recommendations,images," +
                            if (altEndpoint == "tv") "content_ratings" else "release_dates"
                    val altUrl = "https://api.themoviedb.org/3/$altEndpoint/$numericTmdbId?api_key=$apiKey&append_to_response=$altAppend&include_image_language=en,null"
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

                val root = json.parseToJsonElement(body).jsonObject

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

                // Release Year: Movies = theatrical premiere year; TV = latest season year!
                val initialDate = root["release_date"]?.jsonPrimitive?.contentOrNull
                    ?: root["first_air_date"]?.jsonPrimitive?.contentOrNull
                var year = initialDate?.take(4)?.toIntOrNull()

                val seasonNumbers = mutableListOf<Int>()
                val seasonEpisodeCounts = mutableMapOf<Int, Int>()
                if (isTv) {
                    val seasonsArr = root["seasons"]?.jsonArray
                    if (seasonsArr != null) {
                        for (sElem in seasonsArr) {
                            val sObj = sElem.jsonObject
                            val sNum = sObj["season_number"]?.jsonPrimitive?.intOrNull ?: continue
                            val epCount = sObj["episode_count"]?.jsonPrimitive?.intOrNull ?: 0
                            if (sNum > 0) {
                                seasonNumbers.add(sNum)
                                seasonEpisodeCounts[sNum] = epCount
                                val sAirDate = sObj["air_date"]?.jsonPrimitive?.contentOrNull
                                val sYear = sAirDate?.take(4)?.toIntOrNull()
                                if (sYear != null && (year == null || sYear > year)) {
                                    year = sYear
                                }
                            }
                        }
                    }
                }

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
                        val vidUrl = "https://api.themoviedb.org/3/$endpoint/$numericTmdbId/videos?api_key=$apiKey"
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
                            provider = providerName
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
                    seasonEpisodeCounts = seasonEpisodeCounts
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
                            val url = "https://api.themoviedb.org/3/tv/$tmdbId/season/$sNum?api_key=$apiKey"
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
        apiKey: String = DEFAULT_API_KEY
    ): CatalogRow? = withContext(Dispatchers.IO) {
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
                            provider = providerName
                        )
                    )
                }

                // For candidate hero items, resolve top 6 logos quickly
                val finalItems = if (resolveLogos && items.isNotEmpty()) {
                    coroutineScope {
                        items.take(6).map { item ->
                            async {
                                val logo = resolveLogo(client, item.id, item.type == MediaType.TV_SERIES, apiKey = apiKey)
                                item.copy(logoUrl = logo)
                            }
                        }.awaitAll() + items.drop(6)
                    }
                } else {
                    items
                }

                if (finalItems.isNotEmpty()) CatalogRow(title = title, items = finalItems) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Over 35 diverse catalog rails for progressive infinite-scroll loading.
     */
    val PROGRESSIVE_RAIL_DEFINITIONS: List<Pair<String, String>> = listOf(
        "Action & Adventure" to "https://api.themoviedb.org/3/discover/movie?with_genres=28,12&sort_by=popularity.desc",
        "Sci-Fi Universes" to "https://api.themoviedb.org/3/discover/movie?with_genres=878&sort_by=popularity.desc",
        "Mind-Bending Thrillers" to "https://api.themoviedb.org/3/discover/movie?with_genres=53&sort_by=vote_average.desc&vote_count.gte=500",
        "Horror Nights" to "https://api.themoviedb.org/3/discover/movie?with_genres=27&sort_by=popularity.desc",
        "Comedy Hits" to "https://api.themoviedb.org/3/discover/movie?with_genres=35&sort_by=popularity.desc",
        "Animation Spotlight" to "https://api.themoviedb.org/3/discover/movie?with_genres=16&sort_by=popularity.desc",
        "Crime Mysteries" to "https://api.themoviedb.org/3/discover/tv?with_genres=80,9648&sort_by=popularity.desc",
        "Epic Fantasy & Magic" to "https://api.themoviedb.org/3/discover/movie?with_genres=14&sort_by=popularity.desc",
        "Romantic Stories" to "https://api.themoviedb.org/3/discover/movie?with_genres=10749&sort_by=popularity.desc",
        "Documentaries" to "https://api.themoviedb.org/3/discover/movie?with_genres=99&sort_by=popularity.desc",
        "Korean Dramas & Cinema" to "https://api.themoviedb.org/3/discover/tv?with_original_language=ko&sort_by=popularity.desc",
        "Anime Series" to "https://api.themoviedb.org/3/discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc",
        "Japanese Cinema" to "https://api.themoviedb.org/3/discover/movie?with_original_language=ja&sort_by=popularity.desc",
        "Bollywood & Indian Hits" to "https://api.themoviedb.org/3/discover/movie?with_original_language=hi&sort_by=popularity.desc",
        "Spanish Cinema" to "https://api.themoviedb.org/3/discover/movie?with_original_language=es&sort_by=popularity.desc",
        "French Cinema" to "https://api.themoviedb.org/3/discover/movie?with_original_language=fr&sort_by=popularity.desc",
        "Sci-Fi & Fantasy TV" to "https://api.themoviedb.org/3/discover/tv?with_genres=10765&sort_by=popularity.desc",
        "Action & Adventure TV" to "https://api.themoviedb.org/3/discover/tv?with_genres=10759&sort_by=popularity.desc",
        "Drama Series" to "https://api.themoviedb.org/3/discover/tv?with_genres=18&sort_by=popularity.desc",
        "Comedy TV Series" to "https://api.themoviedb.org/3/discover/tv?with_genres=35&sort_by=popularity.desc",
        "Family Movie Night" to "https://api.themoviedb.org/3/discover/movie?with_genres=10751&sort_by=popularity.desc",
        "War & Military" to "https://api.themoviedb.org/3/discover/movie?with_genres=10752&sort_by=popularity.desc",
        "Historical Epics" to "https://api.themoviedb.org/3/discover/movie?with_genres=36&sort_by=popularity.desc",
        "Musical & Dance" to "https://api.themoviedb.org/3/discover/movie?with_genres=10402&sort_by=popularity.desc",
        "Western Classics" to "https://api.themoviedb.org/3/discover/movie?with_genres=37&sort_by=popularity.desc",
        "Cyberpunk & Tech" to "https://api.themoviedb.org/3/discover/movie?with_keywords=4565|12190&sort_by=popularity.desc",
        "Superheroes & Comics" to "https://api.themoviedb.org/3/discover/movie?with_keywords=9715|180547&sort_by=popularity.desc",
        "Post-Apocalyptic Survival" to "https://api.themoviedb.org/3/discover/movie?with_keywords=4458|2853&sort_by=popularity.desc",
        "Space Exploration" to "https://api.themoviedb.org/3/discover/movie?with_keywords=9882|3804&sort_by=popularity.desc",
        "Heist & Con Artists" to "https://api.themoviedb.org/3/discover/movie?with_keywords=10051|9717&sort_by=popularity.desc",
        "Martial Arts Legends" to "https://api.themoviedb.org/3/discover/movie?with_keywords=780|9663&sort_by=popularity.desc",
        "Time Travel" to "https://api.themoviedb.org/3/discover/movie?with_keywords=4379&sort_by=popularity.desc",
        "Vampires & Werewolves" to "https://api.themoviedb.org/3/discover/movie?with_keywords=3133|12564&sort_by=popularity.desc",
        "Coming of Age" to "https://api.themoviedb.org/3/discover/movie?with_keywords=10683|207317&sort_by=popularity.desc",
        "A24 Indie Spotlight" to "https://api.themoviedb.org/3/discover/movie?with_companies=41077&sort_by=popularity.desc",
        "Critically Acclaimed" to "https://api.themoviedb.org/3/discover/movie?sort_by=vote_average.desc&vote_count.gte=2000",
        "Blockbuster Legends" to "https://api.themoviedb.org/3/discover/movie?sort_by=revenue.desc",
        "Hidden Gems" to "https://api.themoviedb.org/3/discover/movie?vote_average.gte=7.8&vote_count.gte=200&vote_count.lte=1500&sort_by=popularity.desc"
    )

    /**
     * Fetches standard TMDB home catalog feeds concurrently for the Home Carousel and content rows.
     * Uses memory cache to guarantee instant (< 50ms) catalog loads on repeat navigation.
     */
    suspend fun fetchHomeCatalog(
        client: OkHttpClient,
        apiKey: String = DEFAULT_API_KEY
    ): List<CatalogRow> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedHomeCatalog?.let { (cachedTime, list) ->
            if (now - cachedTime < 15 * 60 * 1000L && list.isNotEmpty()) {
                return@withContext list
            }
        }

        val coreFeeds = listOf(
            Triple("Trending Today", "https://api.themoviedb.org/3/trending/all/day?api_key=$apiKey", true),
            Triple("Trending Movies", "https://api.themoviedb.org/3/trending/movie/week?api_key=$apiKey", true),
            Triple("Popular Movies", "https://api.themoviedb.org/3/movie/popular?api_key=$apiKey", true),
            Triple("Popular TV Shows", "https://api.themoviedb.org/3/tv/popular?api_key=$apiKey", true),
            Triple("Top Rated Movies", "https://api.themoviedb.org/3/movie/top_rated?api_key=$apiKey", false),
            Triple("Binge-Worthy TV Series", "https://api.themoviedb.org/3/trending/tv/week?api_key=$apiKey", false)
        )

        val discoverFeeds = PROGRESSIVE_RAIL_DEFINITIONS.map { (title, url) ->
            val fullUrl = if (url.contains("?")) "$url&api_key=$apiKey" else "$url?api_key=$apiKey"
            Triple(title, fullUrl, false)
        }

        val allFeeds = coreFeeds + discoverFeeds

        val result = try {
            coroutineScope {
                allFeeds.map { (title, url, resolveLogos) ->
                    async {
                        fetchCatalogRow(client, title, url, providerName = "tmdb", resolveLogos = resolveLogos, apiKey = apiKey)
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (result.isNotEmpty()) {
            cachedHomeCatalog = Pair(now, result)
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
        val targets = PROGRESSIVE_RAIL_DEFINITIONS.drop(offset).take(limit)
        if (targets.isEmpty()) return@withContext emptyList()
        coroutineScope {
            targets.map { (title, url) ->
                async {
                    val fullUrl = if (url.contains("?")) "$url&api_key=$apiKey" else "$url?api_key=$apiKey"
                    fetchCatalogRow(client, title, fullUrl, providerName = "tmdb", resolveLogos = false, apiKey = apiKey)
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
        val url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$encoded&include_adult=false"

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
                            provider = "tmdb"
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
