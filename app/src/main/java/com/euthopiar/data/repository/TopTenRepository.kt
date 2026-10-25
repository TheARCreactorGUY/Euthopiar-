package com.euthopiar.data.repository

import android.content.Context
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.util.TmdbBridge
import com.euthopiar.data.local.AppDatabase
import com.euthopiar.data.local.TopTenDao
import com.euthopiar.data.local.TopTenEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Repository providing Universal TMDB Top 10 items for Movies and TV Series.
 * Implements a 6-hour stale-while-revalidate Room database cache so Home paints instantly
 * with 0 network calls on warm starts.
 */
class TopTenRepository(
    private val dao: TopTenDao
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
                    val freshItems = TmdbBridge.fetchTrendingTopTen(type, language)
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
