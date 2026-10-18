package com.euthopiar.core.extractor

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.util.JsUnpacker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FilemoonExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExtractorApi {

    override val name: String = "Filemoon"
    override val mainUrl: String = "https://filemoon.sx"
    override val requiresReferer: Boolean = true

    override fun isHostFor(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("filemoon.sx") ||
                lower.contains("filemoon.to") ||
                lower.contains("filemoon.in") ||
                lower.contains("filemoon.top") ||
                lower.contains("filemoon.nl") ||
                lower.contains("moonmov.")
    }

    override suspend fun extract(
        embedUrl: String,
        referer: String?,
        headers: Map<String, String>
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            if (referer != null) {
                reqBuilder.header("Referer", referer)
            }
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }

            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val html = response.body?.string().orEmpty()
            extractFromHtml(html, embedUrl)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun extractFromHtml(html: String, targetUrl: String): List<StreamSource> {
        val scriptSource = if (JsUnpacker.isPacked(html)) {
            JsUnpacker.unpack(html).orEmpty()
        } else {
            html
        }

        val m3u8Url = parseM3u8Url(scriptSource) ?: parseM3u8Url(html) ?: return emptyList()

        return listOf(
            StreamSource(
                url = m3u8Url,
                serverName = "Filemoon",
                resolutionLabel = "Auto",
                quality = "Filemoon (Auto)",
                isM3u8 = true,
                audioTracks = emptyList(),
                releaseType = AudioReleaseType.ORIGINAL,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to targetUrl,
                    "Origin" to "https://${getHost(targetUrl)}"
                )
            )
        )
    }

    private fun parseM3u8Url(content: String): String? {
        // Pattern 1: {file: "https://...m3u8..."} or file:"..."
        val fileRegex = Regex("""(?:file|sources|src)\s*:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
        val fileMatch = fileRegex.find(content)
        if (fileMatch != null) return fileMatch.groupValues[1]

        // Pattern 2: direct quotes around any m3u8 url
        val directM3u8Regex = Regex("""["'](https?://[^"'\s]+\.m3u8[^"'\s]*)["']""")
        val directMatch = directM3u8Regex.find(content)
        if (directMatch != null) return directMatch.groupValues[1]

        return null
    }

    private fun getHost(url: String): String {
        return try {
            java.net.URI(url).host ?: "filemoon.sx"
        } catch (_: Exception) {
            "filemoon.sx"
        }
    }
}
