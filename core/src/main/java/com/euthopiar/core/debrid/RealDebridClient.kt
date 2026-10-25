package com.euthopiar.core.debrid

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class RealDebridClient(
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserInfo(apiKey: String): Result<RealDebridUser> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        try {
            val request = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/user")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val user = json.decodeFromString<RealDebridUser>(raw)
                    Result.success(user)
                } else {
                    Result.failure(IOException("Failed to fetch Real-Debrid user: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unrestrictLink(apiKey: String, link: String): Result<RealDebridUnrestrictResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        try {
            val form = FormBody.Builder()
                .add("link", link)
                .build()

            val request = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/unrestrict/link")
                .header("Authorization", "Bearer $apiKey")
                .post(form)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val result = json.decodeFromString<RealDebridUnrestrictResponse>(raw)
                    Result.success(result)
                } else {
                    Result.failure(IOException("Real-Debrid unrestrict failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unrestrictMagnet(
        apiKey: String,
        magnet: String,
        fileIdx: Int? = null,
        season: Int? = null,
        episode: Int? = null
    ): Result<RealDebridUnrestrictResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        try {
            // 1. Add magnet to Real-Debrid
            val addForm = FormBody.Builder().add("magnet", magnet).build()
            val addReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/addMagnet")
                .header("Authorization", "Bearer $apiKey")
                .post(addForm)
                .build()

            val torrentId = httpClient.newCall(addReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Real-Debrid addMagnet failed: HTTP ${resp.code}"))
                val raw = resp.body?.string().orEmpty()
                json.decodeFromString<RealDebridAddMagnetResponse>(raw).id
            }

            // 2. Fetch torrent info to inspect files
            val preInfoReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val files = httpClient.newCall(preInfoReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val raw = resp.body?.string().orEmpty()
                    try {
                        json.decodeFromString<RealDebridTorrentInfo>(raw).files
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else emptyList()
            }

            // 3. Select target file by matching season/episode or fileIdx, or fallback to largest video
            val targetFileId = selectTargetFileId(files, fileIdx, season, episode)
            val filesParam = if (targetFileId != null) "$targetFileId" else "all"

            val selForm = FormBody.Builder().add("files", filesParam).build()
            val selReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId")
                .header("Authorization", "Bearer $apiKey")
                .post(selForm)
                .build()
            httpClient.newCall(selReq).execute().close()

            // 4. Get updated torrent info to extract download links
            val infoReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val linkToUnrestrict = httpClient.newCall(infoReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Real-Debrid torrents/info failed: HTTP ${resp.code}"))
                val raw = resp.body?.string().orEmpty()
                val info = json.decodeFromString<RealDebridTorrentInfo>(raw)
                info.links.firstOrNull() ?: return@withContext Result.failure(IOException("No download links generated for torrent"))
            }

            // 5. Unrestrict the direct link
            unrestrictLink(apiKey, linkToUnrestrict)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun selectTargetFileId(
        files: List<RealDebridTorrentFile>,
        fileIdx: Int?,
        season: Int?,
        episode: Int?
    ): Int? {
        if (files.isEmpty()) return null
        if (files.size == 1) return files[0].id

        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".webm", ".ts", ".m4v", ".mov")
        val videoFiles = files.filter { file ->
            val lower = file.path.lowercase()
            videoExtensions.any { ext -> lower.endsWith(ext) }
        }.ifEmpty { files }

        if (fileIdx != null && fileIdx >= 0) {
            val byId = files.firstOrNull { it.id == fileIdx + 1 || it.id == fileIdx }
            if (byId != null) return byId.id
            if (fileIdx in files.indices) return files[fileIdx].id
        }

        if (season != null && episode != null) {
            val patterns = listOf(
                Regex("""(?i)\bs0*${season}\s*e0*${episode}\b"""),
                Regex("""(?i)\b0*${season}x0*${episode}\b"""),
                Regex("""(?i)\b(?:season|s)[\s._-]*0*${season}[\s._-]+(?:episode|ep|e)[\s._-]*0*${episode}\b"""),
            )
            for (pattern in patterns) {
                val matched = videoFiles.filter { pattern.containsMatchIn(it.path) }
                if (matched.isNotEmpty()) return matched.maxByOrNull { it.bytes }?.id
            }
        } else if (episode != null) {
            val epPatterns = listOf(
                Regex("""(?i)(?:^|[^a-zA-Z]|[sS]\d+)[eE][pP]?\.?\s*0*${episode}(?:\D|$)"""),
                Regex("""(?i)\b(?:episode|ep)[\s._-]*0*${episode}(?:\D|$)"""),
                Regex("""(?i)(?:\bep?\.?\s*0*${episode}\b|-\s*0*${episode}\b|\b0*${episode}\s*(?:v\d)?\s*[\[\(])""")
            )
            for (pattern in epPatterns) {
                val matched = videoFiles.filter { pattern.containsMatchIn(it.path) }
                if (matched.isNotEmpty()) return matched.maxByOrNull { it.bytes }?.id
            }
        }

        return videoFiles.maxByOrNull { it.bytes }?.id
    }
}
