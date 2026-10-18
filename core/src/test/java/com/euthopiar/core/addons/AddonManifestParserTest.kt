package com.euthopiar.core.addons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonManifestParserTest {

    @Test
    fun testParseUniversalManifestWithMixedResources() {
        val payload = """
            {
              "id": "org.stremio.sample",
              "name": "Sample Universal Addon",
              "description": "Multi-resource universal addon",
              "version": "2.1.0",
              "icon": "//example.com/icon.png",
              "resources": [
                "catalog",
                {
                  "name": "stream",
                  "types": ["movie", "series"],
                  "idPrefixes": ["tt"]
                },
                {
                  "name": "subtitles",
                  "types": ["movie", "series"]
                }
              ],
              "types": ["movie", "series"],
              "catalogs": [
                {
                  "type": "movie",
                  "id": "top",
                  "name": "Top Movies",
                  "extraSupported": ["search", "skip"],
                  "extraRequired": []
                }
              ]
            }
        """.trimIndent()

        val manifest = AddonManifestParser.parse("https://example.com/addon/manifest.json", payload)
        assertEquals("org.stremio.sample", manifest.id)
        assertEquals("Sample Universal Addon", manifest.name)
        assertEquals("https://example.com/icon.png", manifest.logoUrl)
        assertEquals(3, manifest.resources.size)

        // Verify resource 0 (shorthand "catalog") inherited default types
        val catalogRes = manifest.resources[0]
        assertEquals("catalog", catalogRes.name)
        assertEquals(listOf("movie", "series"), catalogRes.types)

        // Verify resource 1 ("stream") has specific types and prefixes
        val streamRes = manifest.resources[1]
        assertEquals("stream", streamRes.name)
        assertEquals(listOf("tt"), streamRes.idPrefixes)

        // Verify catalog extraSupported parsing
        assertEquals(1, manifest.catalogs.size)
        val cat = manifest.catalogs[0]
        assertEquals("top", cat.id)
        assertEquals(2, cat.extra.size)
        assertEquals("search", cat.extra[0].name)
        assertEquals("skip", cat.extra[1].name)
        assertTrue(cat.supportsPagination())
    }

    @Test
    fun testParseDataUrlLogo() {
        val payload = """
            {
              "id": "org.data.logo",
              "name": "Data Logo Addon",
              "version": "1.0.0",
              "logo": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
              "resources": ["meta"],
              "types": ["movie"]
            }
        """.trimIndent()

        val manifest = AddonManifestParser.parse("https://addon.org/manifest.json", payload)
        assertTrue(manifest.logoUrl?.startsWith("data:image/png;base64,") == true)
    }
}
