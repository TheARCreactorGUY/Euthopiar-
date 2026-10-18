package com.euthopiar.core.extractor

import com.euthopiar.core.model.AudioReleaseType
import com.euthopiar.core.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class StreamTapeExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExtractorApi {

    override val name: String = "StreamTape"
    override val mainUrl: String = "https://streamtape.com"
    override val requiresReferer: Boolean = true

    override fun isHostFor(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("streamtape.com") ||
                lower.contains("streamtape.to") ||
                lower.contains("streamtape.net") ||
                lower.contains("streamta.pe") ||
                lower.contains("streamtape.xyz")
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
        val streamUrl = parseStreamUrl(html) ?: return emptyList()

        return listOf(
            StreamSource(
                url = streamUrl,
                serverName = "StreamTape",
                resolutionLabel = "1080p",
                quality = "StreamTape (1080p)",
                isM3u8 = streamUrl.contains(".m3u8", ignoreCase = true),
                audioTracks = emptyList(),
                releaseType = AudioReleaseType.ORIGINAL,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to targetUrl
                )
            )
        )
    }

    private fun parseStreamUrl(html: String): String? {
        // Pattern 1: document.getElementById('robotlink').innerHTML = '...' + ('...').substring(...)
        val innerHtmlRegex = Regex(
            """document\.getElementById\(['"](?:robotlink|videolink|cairo)['"]\)\.innerHTML\s*=\s*(['"][^;]+);"""
        )
        val match = innerHtmlRegex.find(html)
        if (match != null) {
            val expr = match.groupValues[1]
            val resolved = evaluateConcatenation(expr)
            if (resolved.isNotBlank()) {
                return normalizeUrl(resolved)
            }
        }

        // Pattern 2: direct token & id pattern /get_video?id=...&token=...
        val directVideoRegex = Regex("""['"](//[^'"]*streamtape\.com/get_video\?[^'"]+)['"]""")
        val directMatch = directVideoRegex.find(html)
        if (directMatch != null) {
            return normalizeUrl(directMatch.groupValues[1])
        }

        // Pattern 3: <video id="video-stream" src="...">
        val videoTagRegex = Regex("""<video[^>]+src=["']([^"']+)["']""")
        val videoMatch = videoTagRegex.find(html)
        if (videoMatch != null) {
            return normalizeUrl(videoMatch.groupValues[1])
        }

        return null
    }

    private fun evaluateConcatenation(expr: String): String {
        val sb = StringBuilder()
        // Match string literals and possible (optional closing parenthesis) .substring(N) calls
        val tokenRegex = Regex("""['"]([^'"]*)['"](?:\s*\))?\s*(?:\.substring\((\d+)\))?""")
        for (m in tokenRegex.findAll(expr)) {
            val text = m.groupValues[1]
            val subStart = m.groupValues[2].toIntOrNull()
            if (subStart != null && subStart in 0..text.length) {
                sb.append(text.substring(subStart))
            } else {
                sb.append(text)
            }
        }
        return sb.toString()
    }

    private fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            !url.startsWith("http") -> "https://$url"
            else -> url
        }
    }
}
