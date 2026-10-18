package com.euthopiar.core.extractor

import com.euthopiar.core.model.StreamSource

interface ExtractorApi {
    val name: String
    val mainUrl: String
    val requiresReferer: Boolean get() = true

    fun isHostFor(url: String): Boolean

    suspend fun extract(
        embedUrl: String,
        referer: String? = null,
        headers: Map<String, String> = emptyMap()
    ): List<StreamSource>
}
