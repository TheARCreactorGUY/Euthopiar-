package com.euthopiar.core.extractor

import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.util.parallelMapIsolated

object ExtractorRegistry {
    private val extractors = mutableListOf<ExtractorApi>()

    init {
        registerDefaultExtractors()
    }

    private fun registerDefaultExtractors() {
        extractors.add(StreamTapeExtractor())
        extractors.add(FilemoonExtractor())
    }

    fun register(extractor: ExtractorApi) = synchronized(extractors) {
        if (extractors.none { it.name.equals(extractor.name, ignoreCase = true) }) {
            extractors.add(extractor)
        }
    }

    fun unregister(name: String) = synchronized(extractors) {
        extractors.removeAll { it.name.equals(name, ignoreCase = true) }
    }

    fun getAllExtractors(): List<ExtractorApi> = synchronized(extractors) {
        extractors.toList()
    }

    fun reset() = synchronized(extractors) {
        extractors.clear()
        registerDefaultExtractors()
    }

    suspend fun resolveUrl(url: String, referer: String? = null): List<StreamSource> {
        val matched = synchronized(extractors) {
            extractors.filter { it.isHostFor(url) }
        }
        if (matched.isEmpty()) return emptyList()

        return matched.parallelMapIsolated(timeoutMs = 4000L) { extractor ->
            try {
                extractor.extract(url, referer)
            } catch (_: Exception) {
                emptyList()
            }
        }.flatten()
    }
}
