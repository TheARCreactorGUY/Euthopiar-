package com.euthopiar.core.js

import com.euthopiar.core.model.PluginManifest
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.model.SubtitleTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JsRepoManifest(
    val name: String,
    val version: String = "1.0.0",
    val description: String? = null,
    val author: String? = null,
    val scrapers: List<JsScraperMeta> = emptyList()
)

@Serializable
data class JsScraperMeta(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: String = "1.0.0",
    val filename: String,
    @SerialName("supportedTypes") val supportedTypes: List<String> = listOf("movie", "tv"),
    val enabled: Boolean = true,
    val hasSettings: Boolean = false,
    val logo: String? = null,
    @SerialName("contentLanguage") val contentLanguage: List<String>? = null,
    val formats: List<String>? = null
)

fun computeCanonicalRepoId(repoUrl: String): String {
    val trimmed = repoUrl.trim()
    if (trimmed.isBlank() || trimmed.equals("local", ignoreCase = true)) return "local"
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(trimmed.toByteArray(Charsets.UTF_8))
    return "repo_" + digest.take(16).joinToString("") { "%02x".format(it) }
}

@Serializable
data class PluginScraper(
    val id: String,
    val repositoryUrl: String = "",
    val name: String,
    val description: String = "",
    val version: String = "1.0.0",
    val filename: String = "",
    val supportedTypes: List<String> = listOf("movie", "tv"),
    val enabled: Boolean = true,
    val hasSettings: Boolean = false,
    val logo: String? = null,
    val formats: List<String>? = null,
    val code: String = "",
    val repoId: String = ""
) {
    val canonicalRepoId: String
        get() = if (repoId.isNotBlank()) repoId else computeCanonicalRepoId(repositoryUrl)

    fun supportsType(type: String): Boolean {
        if (supportedTypes.isNullOrEmpty()) return true
        val normType = normalizePluginType(type)
        return supportedTypes.any { raw ->
            val norm = normalizePluginType(raw)
            norm == normType || norm == "all" ||
                (normType == "anime" && (norm == "tv" || norm == "movie" || norm == "anime"))
        }
    }
}

fun normalizePluginType(value: String): String =
    when (value.lowercase().trim()) {
        "series", "show", "other", "tv_series" -> "tv"
        "movies" -> "movie"
        "anime" -> "anime"
        else -> value.lowercase().trim()
    }

fun pluginContentId(
    videoId: String,
    season: Int? = null,
    episode: Int? = null,
): String {
    val trimmed = videoId.trim()
    if (trimmed.isBlank()) return videoId

    val withoutPrefix = when {
        trimmed.startsWith("tmdb:") -> trimmed.removePrefix("tmdb:")
        trimmed.startsWith("tmdb/") -> trimmed.removePrefix("tmdb/")
        else -> trimmed
    }

    val withoutEpisodeSuffix = if (season != null && episode != null) {
        withoutPrefix.removeSuffix(":$season:$episode")
    } else {
        withoutPrefix
    }

    return withoutEpisodeSuffix.substringBefore('/').ifBlank { trimmed }
}

fun PluginScraper.toPluginManifest(): PluginManifest {
    val isAdult = supportedTypes.any { it.equals("adult", ignoreCase = true) || it.equals("vault", ignoreCase = true) }
    return PluginManifest(
        id = id,
        name = name,
        version = 1,
        versionName = version,
        author = "Universal/Nuvio",
        siteUrl = repositoryUrl,
        description = description,
        iconUrl = logo,
        className = "js:$id",
        downloadUrl = filename,
        isBuiltIn = false,
        isEnabled = enabled,
        supportedTypes = supportedTypes,
        isAdult = isAdult,
        isVault = false
    )
}

fun JsScraperMeta.toPluginManifest(repoUrl: String): PluginManifest {
    val isAdult = supportedTypes.any { it.equals("adult", ignoreCase = true) || it.equals("vault", ignoreCase = true) }
    val fullDownloadUrl = if (filename.startsWith("http")) filename else {
        repoUrl.substringBeforeLast("/") + "/" + filename
    }
    return PluginManifest(
        id = id,
        name = name,
        version = 1,
        versionName = version,
        author = "Universal/Nuvio",
        siteUrl = repoUrl,
        description = description ?: "",
        iconUrl = logo,
        className = "js:$id",
        downloadUrl = fullDownloadUrl,
        isBuiltIn = false,
        isEnabled = enabled,
        supportedTypes = supportedTypes,
        isAdult = isAdult,
        isVault = false
    )
}

@Serializable
data class PluginSubtitleResult(
    val url: String,
    val language: String,
    val name: String? = null,
    val headers: Map<String, String>? = null
)

@Serializable
data class PluginRuntimeResult(
    val title: String,
    val name: String? = null,
    val url: String,
    val quality: String? = null,
    val size: String? = null,
    val language: String? = null,
    val provider: String? = null,
    val type: String? = null,
    val seeders: Int? = null,
    val peers: Int? = null,
    val infoHash: String? = null,
    val headers: Map<String, String>? = null,
    val subtitles: List<PluginSubtitleResult>? = null
) {
    fun toStreamSource(): StreamSource {
        val isM3u8 = type?.equals("m3u8", ignoreCase = true) == true ||
                url.contains(".m3u8", ignoreCase = true) ||
                url.contains("hls", ignoreCase = true)
        val cleanServerName = name ?: provider ?: "Server"
        val qualityLabel = quality ?: "Auto"
        return StreamSource(
            url = url,
            serverName = cleanServerName,
            resolutionLabel = qualityLabel,
            quality = qualityLabel,
            isM3u8 = isM3u8,
            headers = headers ?: emptyMap(),
            infoHash = infoHash,
            seeders = seeders
        )
    }

    fun toSubtitles(): List<SubtitleTrack> {
        return subtitles?.map { sub ->
            SubtitleTrack(
                url = sub.url,
                language = sub.language
            )
        } ?: emptyList()
    }
}
