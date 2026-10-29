package com.euthopiar.core.subtitle.external

import com.euthopiar.core.BuildConfig

import com.euthopiar.core.model.SubtitleTrack
import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class SubDlResponse(
    val status: Boolean = false,
    val subtitles: List<SubDlSubtitleItem> = emptyList()
)

@Serializable
data class SubDlSubtitleItem(
    val release_name: String = "",
    val name: String = "",
    val lang: String = "",
    val author: String = "",
    val url: String = "",
    val hearing_impaired: Boolean = false
)

class SubDlClient(
    private val apiKey: String = DEFAULT_API_KEY,
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchSubtitles(
        tmdbId: String? = null,
        imdbId: String? = null,
        type: String = "movie", // "movie" or "tv"
        season: Int? = null,
        episode: Int? = null,
        language: String = "en"
    ): Result<List<SubtitleTrack>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("https://api.subdl.com/api/v1/subtitles?api_key=$apiKey")
            if (!tmdbId.isNullOrBlank()) urlBuilder.append("&tmdb_id=$tmdbId")
            if (!imdbId.isNullOrBlank()) urlBuilder.append("&imdb_id=$imdbId")
            urlBuilder.append("&type=$type")
            if (season != null && season > 0) urlBuilder.append("&season_number=$season")
            if (episode != null && episode > 0) urlBuilder.append("&episode_number=$episode")
            urlBuilder.append("&languages=$language")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("SubDL HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val parsed = json.decodeFromString<SubDlResponse>(body)
                val tracks = parsed.subtitles.map { sub ->
                    val fullUrl = if (sub.url.startsWith("http")) sub.url else "https://dl.subdl.com${sub.url}"
                    val label = if (sub.release_name.isNotBlank()) {
                        "${sub.lang.uppercase()} - ${sub.release_name.take(24)}"
                    } else {
                        "${sub.lang.uppercase()} (SubDL)"
                    }
                    SubtitleTrack(
                        url = fullUrl,
                        language = label
                    )
                }
                Result.success(tracks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val DEFAULT_API_KEY: String = BuildConfig.SUBDL_API_KEY

        @Volatile
        private var instance: SubDlClient? = null

        fun getInstance(): SubDlClient {
            return instance ?: synchronized(this) {
                instance ?: SubDlClient().also { instance = it }
            }
        }
    }
}
