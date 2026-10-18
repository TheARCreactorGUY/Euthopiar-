package com.euthopiar.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface UniversalPlugin {
    val name: String
    val mainUrl: String
    val supportedTypes: Set<MediaType>
    val isSearchGlobalOnly: Boolean get() = false

    /**
     * Optional lifecycle initialization hook providing access to Host API capabilities
     * such as the headless BrowserResolver, TMDB bridge, and network clients.
     */
    fun init(host: HostApi) {}

    suspend fun getHomeCatalog(): List<CatalogRow>
    suspend fun search(query: String): List<MediaItem>
    suspend fun getDetails(mediaItem: MediaItem): MediaDetail

    // Legacy synchronous call (kept for existing plugins)
    suspend fun getStreamLinks(episodeData: String): StreamResult
    suspend fun getStreams(mediaId: String, episodeData: String? = null): StreamResult =
        getStreamLinks(episodeData ?: mediaId)

    suspend fun getDownloadLinks(episodeData: String): List<DownloadOption> = emptyList()
    suspend fun resolveLogo(mediaItem: MediaItem): String? = null
    suspend fun fetchCast(mediaId: String, imdbId: String? = null, type: MediaType = MediaType.MOVIE): List<CastMember> = emptyList()

    // Reactive progressive stream flow with default backward compatibility:
    fun getStreamFlow(episodeData: String): Flow<StreamEmission> = flow {
        try {
            val result = getStreamLinks(episodeData)
            result.streams.forEach { emit(StreamEmission.SourceFound(it)) }
            result.subtitles.forEach { emit(StreamEmission.SubtitleFound(it)) }
        } catch (e: Exception) {
            emit(StreamEmission.StatusUpdate(name, e.message ?: "Stream resolution failed"))
        }
    }

    fun getStreamFlow(mediaId: String, episodeData: String?): Flow<StreamEmission> =
        getStreamFlow(episodeData ?: mediaId)
}
