package com.euthopiar.data.repository

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.TopTenDao
import com.euthopiar.data.local.TopTenEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TopTenRepositoryTest {

    private class FakeTopTenDao : TopTenDao {
        val map = mutableMapOf<String, TopTenEntity>()

        override suspend fun getByKey(key: String): TopTenEntity? = map[key]

        override suspend fun insertOrUpdate(entity: TopTenEntity) {
            map[entity.cacheKey] = entity
        }

        override suspend fun deleteByKey(key: String) {
            map.remove(key)
        }
    }

    @Test
    fun testObserveTopTenEmitsCachedItemsImmediately() = runBlocking {
        val fakeDao = FakeTopTenDao()
        val cachedMedia = listOf(
            MediaItem(
                id = "550",
                title = "Fight Club",
                url = "",
                posterUrl = "https://image.tmdb.org/t/p/w780/test.jpg",
                type = MediaType.MOVIE,
                year = 1999
            )
        )
        val json = Json { ignoreUnknownKeys = true }
        val now = System.currentTimeMillis()
        fakeDao.insertOrUpdate(
            TopTenEntity(
                cacheKey = "movie_day_en-us_global",
                itemsJson = json.encodeToString(cachedMedia),
                fetchedAtEpochMs = now // fresh cache
            )
        )

        val repository = TopTenRepository(fakeDao)
        val emitted = repository.observeTopTen(MediaType.MOVIE, "en-US").first()

        assertNotNull(emitted)
        assertEquals(1, emitted.size)
        assertEquals("Fight Club", emitted[0].title)
        assertEquals("550", emitted[0].id)
    }
}
