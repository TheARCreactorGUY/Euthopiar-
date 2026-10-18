package com.euthopiar.core.debrid

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AllDebridClient(
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserInfo(apiKey: String): Result<AllDebridUserInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        try {
            val request = Request.Builder()
                .url("https://api.alldebrid.com/v4/user?agent=Euthopiar&apikey=$apiKey")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val parsed = json.decodeFromString<AllDebridUserResponse>(raw)
                    val user = parsed.data?.user ?: return@withContext Result.failure(IOException("No user data"))
                    Result.success(user)
                } else {
                    Result.failure(IOException("Failed to fetch AllDebrid user: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class TorBoxClient(
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserInfo(apiKey: String): Result<TorBoxUserData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        try {
            val request = Request.Builder()
                .url("https://api.torbox.app/v1/api/user/me")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val parsed = json.decodeFromString<TorBoxUserResponse>(raw)
                    val user = parsed.data ?: return@withContext Result.failure(IOException("No TorBox user data"))
                    Result.success(user)
                } else {
                    Result.failure(IOException("Failed to fetch TorBox user: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
