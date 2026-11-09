package com.euthopiar.data.sync

import org.junit.Assert.*
import org.junit.Test

class SupabaseSyncOptimizationTest {

    private fun sanitizeCloudUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (trimmed.startsWith("data:", ignoreCase = true) || trimmed.length > 500) {
            return null
        }
        return trimmed
    }

    @Test
    fun testSanitizeCloudUrlStripsDataUrlsAndExcessiveStrings() {
        val validUrl = "https://image.tmdb.org/t/p/w500/sample.jpg"
        assertEquals(validUrl, sanitizeCloudUrl(validUrl))

        val dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA..."
        assertNull(sanitizeCloudUrl(dataUrl))

        val massiveUrl = "https://example.com/" + "a".repeat(501)
        assertNull(sanitizeCloudUrl(massiveUrl))

        assertNull(sanitizeCloudUrl(""))
        assertNull(sanitizeCloudUrl("   "))
        assertNull(sanitizeCloudUrl(null))
    }

    @Test
    fun testCollectionCapsEnforceFreeTierStorageBoundaries() {
        // Mock 120 history items
        val historyItems = (1..120).map { i ->
            Pair("item_$i", i * 1000L) // id, timestamp
        }
        val cappedHistory = historyItems
            .sortedByDescending { it.second }
            .take(50)
        assertEquals(50, cappedHistory.size)
        assertEquals("item_120", cappedHistory.first().first)
        assertEquals("item_71", cappedHistory.last().first)

        // Mock 250 watchlist items
        val watchlistItems = (1..250).map { i ->
            Pair("watch_$i", i * 500L)
        }
        val cappedWatchlist = watchlistItems
            .sortedByDescending { it.second }
            .take(100)
        assertEquals(100, cappedWatchlist.size)
        assertEquals("watch_250", cappedWatchlist.first().first)

        // Mock 25 playlists
        val playlists = (1..25).map { i ->
            Pair("playlist_$i", i * 100L)
        }
        val cappedPlaylists = playlists
            .sortedByDescending { it.second }
            .take(10)
        assertEquals(10, cappedPlaylists.size)
        assertEquals("playlist_25", cappedPlaylists.first().first)
    }

    @Test
    fun testPendingDeletedAddonsTombstoneManagement() {
        var pendingDeletions = setOf(
            "https://torrentio.strem.fun/manifest.json",
            "https://opensubtitles.strem.fun/manifest.json"
        )

        // Adding an item
        pendingDeletions = pendingDeletions + "https://cinemeta.strem.fun/manifest.json"
        assertEquals(3, pendingDeletions.size)

        // Simulating push of the first two
        val syncedDeletions = setOf(
            "https://torrentio.strem.fun/manifest.json",
            "https://opensubtitles.strem.fun/manifest.json"
        )
        pendingDeletions = pendingDeletions - syncedDeletions

        assertEquals(1, pendingDeletions.size)
        assertTrue(pendingDeletions.contains("https://cinemeta.strem.fun/manifest.json"))
    }
}
