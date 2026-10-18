package com.euthopiar.data.repository

import android.content.Context
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.util.TmdbBridge
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.TopTenDao
import com.euthopiar.data.local.TopTenEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Repository providing Universal TMDB Top 10 items for Movies and TV Series.
 * Implements a 6-hour stale-while-revalidate Room database cache so Home paints instantly
 * with 0 network calls on warm starts.
 */
class TopTenRepository(
    private val dao: TopTenDao,
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient(connectTimeoutSeconds = 8, readTimeoutSeconds = 12),
    private val apiKey: String = TmdbBridge.DEFAULT_API_KEY
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val cacheTtlMs = TimeUnit.HOURS.toMillis(6)
    private val refreshMutex = Mutex()

    fun observeTopTen(type: MediaType, language: String = "en-US"): Flow<List<MediaItem>> = channelFlow {
        val cacheKey = "${type.name.lowercase()}_day_${language.lowercase()}"

        // 1. Emit cached data immediately from Room if available
        val cached = dao.getByKey(cacheKey)
        if (cached != null) {
            try {
                val cachedItems = json.decodeFromString<List<MediaItem>>(cached.itemsJson)
                if (cachedItems.isNotEmpty()) {
                    send(cachedItems)
                }
            } catch (_: Exception) {}
        }

        // 2. Stale-while-revalidate check
        val now = System.currentTimeMillis()
        val isStale = cached == null || (now - cached.fetchedAtEpochMs > cacheTtlMs)

        if (isStale) {
            refreshMutex.withLock {
                // Double check cache timestamp inside mutex
                val latestCached = dao.getByKey(cacheKey)
                if (latestCached == null || (System.currentTimeMillis() - latestCached.fetchedAtEpochMs > cacheTtlMs)) {
                    val freshItems = fetchTrendingFromTmdb(type, language)
                    if (freshItems.isNotEmpty()) {
                        val entity = TopTenEntity(
                            cacheKey = cacheKey,
                            itemsJson = json.encodeToString(freshItems),
                            fetchedAtEpochMs = System.currentTimeMillis()
                        )
                        dao.insertOrUpdate(entity)
                        send(freshItems)
                    }
                }
            }
        }
    }

    private suspend fun fetchTrendingFromTmdb(type: MediaType, language: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val endpointType = if (type == MediaType.TV_SERIES) "tv" else "movie"
            val url = "https://api.themoviedb.org/3/trending/$endpointType/day?api_key=$apiKey&language=$language"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.parseToJsonElement(body).jsonObject
                val results = parsed["results"]?.jsonArray ?: return@withContext emptyList()

                val items = mutableListOf<MediaItem>()
                for (elem in results.take(10)) {
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val title = (obj["title"] ?: obj["name"])?.jsonPrimitive?.contentOrNull ?: "Untitled"
                    val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                    val backdropPath = obj["backdrop_path"]?.jsonPrimitive?.contentOrNull
                    val voteAverage = obj["vote_average"]?.jsonPrimitive?.doubleOrNull
                    val releaseDate = (obj["release_date"] ?: obj["first_air_date"])?.jsonPrimitive?.contentOrNull
                    val year = releaseDate?.take(4)?.toIntOrNull()

                    val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                    val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
                    val ratingStr = voteAverage?.let { String.format(Locale.US, "%.1f", it) }

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

    companion object {
        @Volatile
        private var instance: TopTenRepository? = null

        fun getInstance(context: Context): TopTenRepository {
            return instance ?: synchronized(this) {
                instance ?: TopTenRepository(
                    dao = AppDatabase.getInstance(context).topTenDao()
                ).also { instance = it }
            }
        }
    }
}
