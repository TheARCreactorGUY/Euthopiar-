package com.euthopiar.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.data.local.AtomicJsonStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistRepositoryIntegrityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        PlaylistRepository.resetForTesting()
        context = ApplicationProvider.getApplicationContext()
        val storageFile = File(context.filesDir, "user_playlists.json")
        if (storageFile.exists()) {
            storageFile.delete()
        }
    }

    @Test
    fun testStartupDoesNotOverwriteExistingDiskPlaylists() = runBlocking {
        // Pre-populate disk file with existing playlists
        val storageFile = File(context.filesDir, "user_playlists.json")
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val diskPlaylists = listOf(
            Playlist(id = "disk_pl_1", title = "Favorites 2023", items = emptyList()),
            Playlist(id = "disk_pl_2", title = "Anime Watchlist", items = emptyList())
        )
        val initialStore = AtomicJsonStore(
            file = storageFile,
            serializer = ListSerializer(Playlist.serializer()),
            default = emptyList<Playlist>()
        )
        initialStore.write(diskPlaylists)

        // Instantiate repository (simulates app startup)
        val repo = PlaylistRepository.getInstance(context)

        // Immediately create a new playlist before waiting
        val created = repo.createPlaylist("Startup Added Playlist")
        assertNotNull(created)

        // Give coroutines a brief moment to complete serialization
        kotlinx.coroutines.delay(200)

        // Verify in-memory state
        val inMemory = repo.userPlaylists.value
        assertTrue("Must contain newly created playlist", inMemory.any { it.id == created.id })
        assertTrue("Must retain disk playlist 1", inMemory.any { it.id == "disk_pl_1" })
        assertTrue("Must retain disk playlist 2", inMemory.any { it.id == "disk_pl_2" })
        assertEquals(3, inMemory.size)

        // Verify disk state
        val onDisk = initialStore.read()
        assertTrue("Disk must contain newly created playlist", onDisk.any { it.id == created.id })
        assertTrue("Disk must retain disk playlist 1", onDisk.any { it.id == "disk_pl_1" })
        assertTrue("Disk must retain disk playlist 2", onDisk.any { it.id == "disk_pl_2" })
        assertEquals(3, onDisk.size)
    }

    @Test
    fun testAddItemAndRemoveItemSerialization() = runBlocking {
        val repo = PlaylistRepository.getInstance(context)
        val playlist = repo.createPlaylist("Test Items Playlist")

        val item1 = PlaylistItem(
            mediaId = "movie_101",
            title = "Movie 101",
            posterUrl = "https://example.com/p1.jpg"
        )
        val item2 = PlaylistItem(
            mediaId = "tv_202",
            title = "TV Show 202",
            seasonNumber = 1,
            episodeNumber = 1,
            episodeTitle = "Pilot"
        )

        repo.addItemToPlaylist(playlist.id, item1)
        repo.addItemToPlaylist(playlist.id, item2)
        kotlinx.coroutines.delay(200)

        val updated = repo.userPlaylists.value.first { it.id == playlist.id }
        assertEquals(2, updated.items.size)
        assertTrue(updated.items.any { it.mediaId == "movie_101" })
        assertTrue(updated.items.any { it.mediaId == "tv_202" })

        // Remove item 1
        repo.removeItemFromPlaylist(playlist.id, item1.uniqueKey)
        kotlinx.coroutines.delay(200)

        val afterRemove = repo.userPlaylists.value.first { it.id == playlist.id }
        assertEquals(1, afterRemove.items.size)
        assertEquals("tv_202", afterRemove.items[0].mediaId)

        // Delete playlist
        repo.deletePlaylist(playlist.id)
        kotlinx.coroutines.delay(200)

        val afterDelete = repo.userPlaylists.value
        assertTrue(afterDelete.none { it.id == playlist.id })
    }
}
