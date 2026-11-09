package com.euthopiar.core.sync

import com.euthopiar.core.sync.supabase.CloudUserAddon
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SupabaseAddonModelAndSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun testCloudUserAddonSerializationRoundTrip() {
        val addon = CloudUserAddon(
            userId = "user_abc_123",
            manifestUrl = "https://torrentio.strem.fun/manifest.json",
            addonType = "stremio",
            name = "Torrentio",
            isDeleted = false,
            clientUpdatedAtMs = 1700000000000L,
            updatedAt = "2026-11-09T12:00:00Z"
        )

        val serialized = json.encodeToString(addon)
        assertTrue(serialized.contains("\"user_id\":\"user_abc_123\""))
        assertTrue(serialized.contains("\"manifest_url\":\"https://torrentio.strem.fun/manifest.json\""))
        assertTrue(serialized.contains("\"addon_type\":\"stremio\""))
        assertTrue(serialized.contains("\"name\":\"Torrentio\""))
        assertTrue(serialized.contains("\"is_deleted\":false"))

        val deserialized = json.decodeFromString<CloudUserAddon>(serialized)
        assertEquals(addon.userId, deserialized.userId)
        assertEquals(addon.manifestUrl, deserialized.manifestUrl)
        assertEquals(addon.addonType, deserialized.addonType)
        assertEquals(addon.name, deserialized.name)
        assertFalse(deserialized.isDeleted)
        assertEquals(1700000000000L, deserialized.clientUpdatedAtMs)
        assertEquals("2026-11-09T12:00:00Z", deserialized.updatedAt)
    }

    @Test
    fun testCloudUserAddonDeduplicationKeepsLatest() {
        val oldItem = CloudUserAddon(
            userId = "user_1",
            manifestUrl = "https://example.com/manifest.json",
            addonType = "stremio",
            name = "Old Name",
            isDeleted = false,
            clientUpdatedAtMs = 1000L
        )
        val newItem = CloudUserAddon(
            userId = "user_1",
            manifestUrl = "https://example.com/manifest.json",
            addonType = "stremio",
            name = "New Name",
            isDeleted = false,
            clientUpdatedAtMs = 5000L
        )
        val deletedItem = CloudUserAddon(
            userId = "user_1",
            manifestUrl = "https://example.com/manifest.json",
            addonType = "stremio",
            name = "New Name",
            isDeleted = true,
            clientUpdatedAtMs = 10000L
        )

        val list = listOf(oldItem, newItem, deletedItem)
        val deduped = list
            .groupBy { "${it.userId}::${it.manifestUrl}" }
            .mapValues { (_, group) ->
                group.maxByOrNull { it.clientUpdatedAtMs }!!
            }
            .values
            .toList()

        assertEquals(1, deduped.size)
        assertEquals(10000L, deduped[0].clientUpdatedAtMs)
        assertTrue(deduped[0].isDeleted)
    }

    @Test
    fun testJsRepoTypeSerialization() {
        val repo = CloudUserAddon(
            userId = "user_999",
            manifestUrl = "https://raw.githubusercontent.com/org/repo/main/manifest.json",
            addonType = "js_repo",
            name = null,
            isDeleted = false,
            clientUpdatedAtMs = 2000L
        )

        val serialized = json.encodeToString(repo)
        assertTrue(serialized.contains("\"addon_type\":\"js_repo\""))

        val deserialized = json.decodeFromString<CloudUserAddon>(serialized)
        assertEquals("js_repo", deserialized.addonType)
        assertNull(deserialized.name)
    }
}
