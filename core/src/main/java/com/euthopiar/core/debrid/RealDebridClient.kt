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

    suspend fun unrestrictMagnet(apiKey: String, magnet: String): Result<RealDebridUnrestrictResponse> = withContext(Dispatchers.IO) {
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

            // 2. Select files (all)
            val selForm = FormBody.Builder().add("files", "all").build()
            val selReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId")
                .header("Authorization", "Bearer $apiKey")
                .post(selForm)
                .build()
            httpClient.newCall(selReq).execute().close()

            // 3. Get torrent info to extract download links
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

            // 4. Unrestrict the direct link
            unrestrictLink(apiKey, linkToUnrestrict)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
