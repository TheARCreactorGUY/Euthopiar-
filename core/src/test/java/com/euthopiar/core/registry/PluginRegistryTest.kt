package com.euthopiar.core.registry

import com.euthopiar.core.model.*
import com.euthopiar.core.repository.ProviderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PluginRegistryTest {

    private lateinit var registry: PluginRegistry
    private lateinit var repository: ProviderRepository

    @Before
    fun setUp() {
        registry = PluginRegistry(listOf(createMockPlugin(name = "DefaultMock")))
        repository = ProviderRepository(registry)
    }

    @Test
    fun testDefaultPluginRegistration() {
        println("Testing default plugin registration...")
        val active = registry.activePlugin.value
        assertNotNull("Active plugin should not be null", active)
        assertEquals("Default active plugin should be DefaultMock", "DefaultMock", active.name)

        val plugins = registry.plugins.value
        assertEquals("Initially should have 1 registered plugin", 1, plugins.size)
        assertEquals("First plugin should be DefaultMock", "DefaultMock", plugins[0].name)
    }

    @Test
    fun testProviderSwitching() {
        println("Testing provider switching...")
        val mockPlugin = createMockPlugin(name = "FlixHQ")
        registry.registerPlugin(mockPlugin)

        assertEquals("Should now have 2 registered plugins", 2, registry.plugins.value.size)
        assertNotNull("Should be able to find registered plugin", registry.getPlugin("FlixHQ"))

        // Duplicate registration should be ignored
        registry.registerPlugin(mockPlugin)
        assertEquals("Duplicate registration should be ignored", 2, registry.plugins.value.size)

        // Switch to mock plugin
        val switched = registry.setActivePlugin("FlixHQ")
        assertTrue("Switching to valid plugin should return true", switched)
        assertEquals("Active plugin should now be FlixHQ", "FlixHQ", registry.activePlugin.value.name)

        // Switch to unknown plugin should fail
        val invalidSwitch = registry.setActivePlugin("NonExistent")
        assertFalse("Switching to unknown plugin should return false", invalidSwitch)
        assertEquals("Active plugin should remain FlixHQ", "FlixHQ", registry.activePlugin.value.name)

        // Unregister FlixHQ -> active plugin falls back to DefaultMock
        registry.unregisterPlugin("FlixHQ")
        assertEquals("Should have 1 plugin remaining", 1, registry.plugins.value.size)
        assertEquals("Active plugin should fallback to DefaultMock", "DefaultMock", registry.activePlugin.value.name)
    }

    @Test
    fun testDynamicPluginReplacement() {
        println("Testing dynamic plugin replacement...")
        val p1 = createMockPlugin(name = "TestProvider")
        registry.registerPlugin(p1)
        assertSame("Registered plugin matches", p1, registry.getPlugin("TestProvider"))

        val p2 = createMockPlugin(name = "TestProvider")
        registry.registerPlugin(p2)
        assertSame("Replaced plugin matches", p2, registry.getPlugin("TestProvider"))

        registry.unregisterPlugin("TestProvider")
        assertNull("Unregistered plugin is removed", registry.getPlugin("TestProvider"))
    }

    @Test
    fun testScopedVsGlobalSearch() = runBlocking {
        println("Testing scoped vs global search...")
        val secondaryPlugin = createMockPlugin(
            name = "AnimeWorld",
            searchResults = listOf(
                MediaItem(
                    id = "anime-1",
                    title = "Batman: Gotham Knight",
                    url = "https://example.com/anime-1",
                    posterUrl = null,
                    type = MediaType.ANIME
                )
            )
        )
        registry.registerPlugin(secondaryPlugin)

        val atlanticPlugin = createMockPlugin(
            name = "Atlantic",
            searchResults = listOf(
                MediaItem(
                    id = "atlantic-1",
                    title = "Batman Begins",
                    url = "https://example.com/batman",
                    posterUrl = null,
                    type = MediaType.MOVIE
                )
            )
        )
        registry.registerPlugin(atlanticPlugin)
        registry.setActivePlugin("Atlantic")
        val scopedResults = repository.search("batman", isGlobal = false)
        assertTrue("Scoped search should return results from Atlantic", scopedResults.isNotEmpty())
        assertTrue("All scoped items should be tagged with Atlantic", scopedResults.all { it.provider == "Atlantic" })
        assertFalse("Scoped search should not include AnimeWorld items", scopedResults.any { it.id == "anime-1" })

        // 2. Global Search querying both providers in parallel
        val globalResults = repository.search("batman", isGlobal = true)
        assertTrue("Global search should contain results from both plugins", globalResults.isNotEmpty())
        val providers = globalResults.mapNotNull { it.provider }.toSet()
        assertTrue("Global results should contain Atlantic", providers.contains("Atlantic"))
        assertTrue("Global results should contain AnimeWorld", providers.contains("AnimeWorld"))
        assertTrue("AnimeWorld item should be present in global search", globalResults.any { it.id == "anime-1" })

        // 3. Resilient Global Search with failing/timing-out plugin
        val faultyPlugin = object : UniversalPlugin {
            override val name: String = "FailingPlugin"
            override val mainUrl: String = "https://faulty.com"
            override val supportedTypes: Set<MediaType> = setOf(MediaType.MOVIE)
            override suspend fun getHomeCatalog(): List<CatalogRow> = throw RuntimeException("Network down")
            override suspend fun search(query: String): List<MediaItem> = throw RuntimeException("Search failed")
            override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = throw RuntimeException("Failed")
            override suspend fun getStreamLinks(episodeData: String): StreamResult = throw RuntimeException("Failed")
        }
        registry.registerPlugin(faultyPlugin)

        val resilientResults = repository.search("batman", isGlobal = true)
        assertTrue("Search should succeed even when one provider fails", resilientResults.isNotEmpty())
        assertTrue("Results should still contain AnimeWorld", resilientResults.any { it.provider == "AnimeWorld" })
    }

    private fun createMockPlugin(
        name: String,
        searchResults: List<MediaItem> = emptyList()
    ): UniversalPlugin {
        return object : UniversalPlugin {
            override val name: String = name
            override val mainUrl: String = "https://$name.local"
            override val supportedTypes: Set<MediaType> = setOf(MediaType.MOVIE, MediaType.TV_SERIES)

            override suspend fun getHomeCatalog(): List<CatalogRow> = listOf(
                CatalogRow(title = "Featured on $name", items = searchResults)
            )

            override suspend fun search(query: String): List<MediaItem> {
                delay(10) // Simulate slight network async latency
                return searchResults
            }

            override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = MediaDetail(
                id = mediaItem.id,
                title = mediaItem.title,
                url = mediaItem.url,
                posterUrl = mediaItem.posterUrl,
                backdropUrl = null,
                type = mediaItem.type,
                year = mediaItem.year,
                synopsis = "Mock details for ${mediaItem.title}"
            )

            override suspend fun getStreamLinks(episodeData: String): StreamResult = StreamResult(
                streams = listOf(
                    StreamSource(
                        url = "https://$name.local/stream.m3u8",
                        quality = "1080p",
                        isM3u8 = true
                    )
                )
            )
        }
    }
}
