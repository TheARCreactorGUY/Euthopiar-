package com.euthopiar.core.sync.trakt

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

/**
 * High-performance, resilient Trakt.tv v2 REST API client.
 * Supports OAuth Device Code authentication, real-time playback scrobbling, and user watchlist synchronization.
 */
class TraktClient(
    private val clientId: String = DEFAULT_CLIENT_ID,
    private val clientSecret: String = DEFAULT_CLIENT_SECRET,
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Initiates OAuth 2.0 Device Flow. User visits verificationUrl and enters userCode.
     */
    suspend fun generateDeviceCode(): Result<TraktDeviceCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("client_id" to clientId)
            val request = Request.Builder()
                .url("$BASE_URL/oauth/device/code")
                .header("Content-Type", "application/json")
                .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to get device code: HTTP ${response.code}"))
                }
                val raw = response.body?.string() ?: ""
                val parsed = json.decodeFromString<TraktDeviceCodeResponse>(raw)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Polls Trakt endpoint to check if user has approved the device code.
     * Returns:
     * - Success with token if approved
     * - Failure with PendingException if still waiting
     * - Failure with other exception on expiration/error
     */
    suspend fun pollAccessToken(deviceCode: String): Result<TraktTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf(
                "code" to deviceCode,
                "client_id" to clientId,
                "client_secret" to clientSecret
            )
            val request = Request.Builder()
                .url("$BASE_URL/oauth/device/token")
                .header("Content-Type", "application/json")
                .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val raw = response.body?.string() ?: ""
                        val token = json.decodeFromString<TraktTokenResponse>(raw)
                        Result.success(token)
                    }
                    400 -> Result.failure(PendingAuthException("Waiting for user authorization..."))
                    404 -> Result.failure(IOException("Invalid device code"))
                    409 -> Result.failure(IOException("Device code expired"))
                    410 -> Result.failure(IOException("Token already retrieved"))
                    418 -> Result.failure(IOException("User denied authorization"))
                    429 -> Result.failure(PendingAuthException("Polling too fast, slow down"))
                    else -> Result.failure(IOException("HTTP error ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scrobble playback status to Trakt.
     * action: "start", "pause", "stop"
     */
    suspend fun scrobble(
        action: String,
        accessToken: String,
        body: TraktScrobbleBody
    ): Result<TraktScrobbleResponse> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/scrobble/$action")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 201 || response.code == 200) {
                    val raw = response.body?.string() ?: ""
                    val parsed = json.decodeFromString<TraktScrobbleResponse>(raw)
                    Result.success(parsed)
                } else {
                    Result.failure(IOException("Scrobble $action failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves current authorized Trakt user profile.
     */
    suspend fun getUserProfile(accessToken: String): Result<TraktUser> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/users/me")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val user = json.decodeFromString<TraktUser>(raw)
                    Result.success(user)
                } else {
                    Result.failure(IOException("Failed to fetch user profile: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class PendingAuthException(message: String) : Exception(message)

    companion object {
        const val BASE_URL = "https://api.trakt.tv"
        // Public open-source Trakt app client credentials
        const val DEFAULT_CLIENT_ID = "YOUR_API_KEY_HERE"
        const val DEFAULT_CLIENT_SECRET = "YOUR_API_KEY_HERE"

        @Volatile
        private var instance: TraktClient? = null

        fun getInstance(): TraktClient {
            return instance ?: synchronized(this) {
                instance ?: TraktClient().also { instance = it }
            }
        }
    }
}
