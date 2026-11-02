package com.euthopiar.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseChangelogParserTest {

    @Test
    fun testExtractsPhotosAndFormatsHeadlines() {
        val markdown = """
            ![Hero Banner](https://github.com/TheARCreactorGUY/Euthopiar/assets/hero.png)
            <img src="https://github.com/TheARCreactorGUY/Euthopiar/assets/preview.jpg" />

            ## Top Features
            * **Player Quality HUD:** Live playback resolution now smoothly pops up when player controls fade away.
            * **Aspect Ratio Pill:** Refined spring physics with instant gesture feedback and zero ghost badges.

            ## What's New & Fixes
            * **Locker Gateway Bypasses:** Filtered defunct redirect servers to prevent playback stalls.
            * NonBold Feature: This is a colon separated feature title.
            * Just a normal bullet without a title
        """.trimIndent()

        val result = parseReleaseChangelog(markdown)

        // 1. Photos extracted
        assertEquals(2, result.photos.size)
        assertEquals("https://github.com/TheARCreactorGUY/Euthopiar/assets/hero.png", result.photos[0])
        assertEquals("https://github.com/TheARCreactorGUY/Euthopiar/assets/preview.jpg", result.photos[1])

        // 2. Nodes structured
        val nodes = result.nodes
        assertTrue(nodes.isNotEmpty())

        val header1 = nodes.first { it is ChangelogNode.Header && it.title == "Top Features" } as ChangelogNode.Header
        assertEquals("Top Features", header1.title)

        val bullet1 = nodes.first { it is ChangelogNode.FeatureBullet && it.title == "Player Quality HUD" } as ChangelogNode.FeatureBullet
        assertEquals("Player Quality HUD", bullet1.title)
        assertEquals("Live playback resolution now smoothly pops up when player controls fade away.", bullet1.description)

        val colonBullet = nodes.first { it is ChangelogNode.FeatureBullet && it.title == "NonBold Feature" } as ChangelogNode.FeatureBullet
        assertEquals("NonBold Feature", colonBullet.title)
        assertEquals("This is a colon separated feature title.", colonBullet.description)
    }

    @Test
    fun testFallbackForBlankChangelog() {
        val result = parseReleaseChangelog("")
        assertTrue(result.photos.isEmpty())
        assertEquals(1, result.nodes.size)
        assertTrue(result.nodes[0] is ChangelogNode.FeatureBullet)
    }
}
