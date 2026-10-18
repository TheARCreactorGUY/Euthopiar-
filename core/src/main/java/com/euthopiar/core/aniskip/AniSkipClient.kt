package com.euthopiar.core.aniskip

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
data class AniSkipResponse(
    val found: Boolean = false,
    val results: List<AniSkipResult> = emptyList()
)

@Serializable
data class AniSkipResult(
    @SerialName("skipType") val skipType: String = "", // "op" or "ed"
    val interval: AniSkipInterval? = null,
    @SerialName("episodeLength") val episodeLength: Double = 0.0
)

@Serializable
data class AniSkipInterval(
    val startTime: Double = 0.0,
    val endTime: Double = 0.0
)

data class SkipTimestamps(
    val introStartSeconds: Double? = null,
    val introEndSeconds: Double? = null,
    val outroStartSeconds: Double? = null,
    val outroEndSeconds: Double? = null
)

class AniSkipClient(
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getSkipTimes(malId: Long, episodeNumber: Int): Result<SkipTimestamps> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types=op&types=ed&episodeLength=0"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("AniSkip HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val parsed = json.decodeFromString<AniSkipResponse>(body)
                if (!parsed.found || parsed.results.isEmpty()) {
                    return@withContext Result.failure(IOException("No skip times found"))
                }

                var introStart: Double? = null
                var introEnd: Double? = null
                var outroStart: Double? = null
                var outroEnd: Double? = null

                for (r in parsed.results) {
                    if (r.skipType == "op" && r.interval != null) {
                        introStart = r.interval.startTime
                        introEnd = r.interval.endTime
                    } else if (r.skipType == "ed" && r.interval != null) {
                        outroStart = r.interval.startTime
                        outroEnd = r.interval.endTime
                    }
                }

                Result.success(
                    SkipTimestamps(
                        introStartSeconds = introStart,
                        introEndSeconds = introEnd,
                        outroStartSeconds = outroStart,
                        outroEndSeconds = outroEnd
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val malIdCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun resolveMalId(title: String): Long? = withContext(Dispatchers.IO) {
        var clean = title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), " ")
        clean = clean.replace(Regex("""(?i)\b(?:season\s*\d+|s\d+|1080p|720p|hevc|dual\s*audio|batch|part\s*\d+)\b"""), " ")
        clean = clean.replace(Regex("[^a-zA-Z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        val cleanKey = clean.lowercase()
        malIdCache[cleanKey]?.let { return@withContext it }
        try {
            val encoded = java.net.URLEncoder.encode(clean, "UTF-8")
            val url = "https://api.jikan.moe/v4/anime?q=$encoded&limit=1"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                val malRegex = Regex(""""mal_id"\s*:\s*(\d+)""")
                val found = malRegex.find(body)?.groupValues?.get(1)?.toLongOrNull()
                if (found != null && found > 0) {
                    malIdCache[cleanKey] = found
                    return@withContext found
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun getSkipTimesByTitle(title: String, episodeNumber: Int): Result<SkipTimestamps> = withContext(Dispatchers.IO) {
        val malId = resolveMalId(title) ?: return@withContext Result.failure(IOException("Could not resolve MAL ID for $title"))
        getSkipTimes(malId, episodeNumber)
    }

    companion object {
        @Volatile
        private var instance: AniSkipClient? = null

        fun getInstance(): AniSkipClient {
            return instance ?: synchronized(this) {
                instance ?: AniSkipClient().also { instance = it }
            }
        }
    }
}
