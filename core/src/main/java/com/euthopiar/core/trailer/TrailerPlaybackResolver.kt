package com.euthopiar.core.trailer

object TrailerPlaybackResolver {
    private val extractor = InAppYouTubeExtractor()

    suspend fun resolveFromYouTubeUrl(youtubeUrl: String): TrailerPlaybackSource? =
        extractor.extractPlaybackSource(youtubeUrl)

    suspend fun resolveSingleUrlFromYouTubeUrl(youtubeUrl: String): String? =
        extractor.extractSingleUrl(youtubeUrl)

    fun clearCache() {
        extractor.clearCache()
    }

    fun invalidateCache(youtubeUrl: String) {
        extractor.invalidateCache(youtubeUrl)
    }
}
