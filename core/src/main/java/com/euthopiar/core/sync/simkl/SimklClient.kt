package com.euthopiar.core.sync.simkl

import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SimklClient(
    private val clientId: String = DEFAULT_CLIENT_ID,
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getUserProfile(token: String): Result<SimklUserDetails> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalArgumentException("No token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/users/settings")
                .header("simkl-api-key", clientId)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val parsed = json.decodeFromString<SimklUser>(raw)
                    val user = parsed.user ?: SimklUserDetails(name = "Simkl User")
                    Result.success(user)
                } else {
                    Result.failure(IOException("Failed to fetch Simkl profile: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scrobble(
        token: String,
        payload: SimklScrobblePayload
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalArgumentException("No token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/history")
                .header("Content-Type", "application/json")
                .header("simkl-api-key", clientId)
                .header("Authorization", "Bearer $token")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Simkl scrobble failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BASE_URL = "https://api.simkl.com"
        const val DEFAULT_CLIENT_ID = "YOUR_API_KEY_HERE"

        @Volatile
        private var instance: SimklClient? = null

        fun getInstance(): SimklClient {
            return instance ?: synchronized(this) {
                instance ?: SimklClient().also { instance = it }
            }
        }
    }
}
