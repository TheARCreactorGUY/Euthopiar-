package com.euthopiar.core.addons

import org.junit.Assert.assertEquals
import org.junit.Test

class AddonRepositoryUrlNormalizationTest {

    @Test
    fun testTrailingSlashDoesNotDuplicateManifestJson() {
        val input = "https://torrentio.strem.fun/manifest.json/"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }

    @Test
    fun testTrailingSlashOnBaseUrlAppendsManifestJsonOnce() {
        val input = "https://torrentio.strem.fun/"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }

    @Test
    fun testStremioSchemeConvertedToHttps() {
        val input = "stremio://torrentio.strem.fun/manifest.json/"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }

    @Test
    fun testQueryParametersPreservedWithManifestJson() {
        val input = "https://torrentio.strem.fun/manifest.json?providers=yts&sort=quality"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json?providers=yts&sort=quality", normalized)
    }

    @Test
    fun testBaseUrlWithQueryParametersAppendsManifestJsonBeforeQuery() {
        val input = "https://torrentio.strem.fun/?providers=yts"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json?providers=yts", normalized)
    }

    @Test
    fun testMultipleTrailingSlashes() {
        val input = "https://addon.example.com/manifest.json///"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://addon.example.com/manifest.json", normalized)
    }

    @Test
    fun testSchemelessUrlGetsHttpsScheme() {
        val input = "torrentio.strem.fun/manifest.json"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }

    @Test
    fun testUppercaseStremioSchemeConvertedToHttps() {
        val input = "STREMIO://torrentio.strem.fun/manifest.json"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }

    @Test
    fun testBaseUrlWithoutTrailingSlashAppendsManifestJson() {
        val input = "https://torrentio.strem.fun"
        val normalized = AddonRepository.normalizeAddonUrl(input)
        assertEquals("https://torrentio.strem.fun/manifest.json", normalized)
    }
}
