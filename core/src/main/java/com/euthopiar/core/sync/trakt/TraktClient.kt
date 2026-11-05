package com.euthopiar.core.sync.trakt

import com.euthopiar.core.BuildConfig
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
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64
import java.net.URLEncoder

/**
 * High-performance, resilient Trakt.tv v2 REST API client.
 * Supports OAuth Device Code & PKCE authentication, scrobbling, and full two-way synchronization
 * (Watchlist, Playback Resume Points, History, and Custom Lists).
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

    /**
     * Fetches the user's unified watchlist (movies and shows).
     * Endpoint: GET /sync/watchlist
     */
    suspend fun getWatchlist(accessToken: String): Result<List<TraktWatchlistItem>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/watchlist")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val items = json.decodeFromString<List<TraktWatchlistItem>>(raw)
                    Result.success(items)
                } else {
                    Result.failure(IOException("Failed to fetch Trakt watchlist: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds movies or shows to the user's Trakt watchlist.
     * Endpoint: POST /sync/watchlist
     */
    suspend fun addToWatchlist(accessToken: String, payload: TraktSyncItemsPayload): Result<TraktSyncResponse> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/watchlist")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201) {
                    val raw = response.body?.string() ?: ""
                    val res = json.decodeFromString<TraktSyncResponse>(raw)
                    Result.success(res)
                } else {
                    Result.failure(IOException("Failed to add to Trakt watchlist: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes movies or shows from the user's Trakt watchlist.
     * Endpoint: POST /sync/watchlist/remove
     */
    suspend fun removeFromWatchlist(accessToken: String, payload: TraktSyncItemsPayload): Result<TraktSyncResponse> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/watchlist/remove")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    val raw = response.body?.string() ?: ""
                    val res = json.decodeFromString<TraktSyncResponse>(raw)
                    Result.success(res)
                } else {
                    Result.failure(IOException("Failed to remove from Trakt watchlist: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves paused playback progress (resume points / continue watching).
     * Endpoint: GET /sync/playback
     */
    suspend fun getPlayback(accessToken: String): Result<List<TraktPlaybackProgressItem>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/playback")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val items = json.decodeFromString<List<TraktPlaybackProgressItem>>(raw)
                    Result.success(items)
                } else {
                    Result.failure(IOException("Failed to fetch Trakt playback progress: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a paused playback item by ID.
     * Endpoint: DELETE /sync/playback/{id}
     */
    suspend fun removePlayback(accessToken: String, playbackId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/playback/$playbackId")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Failed to delete Trakt playback item: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves user watch history.
     * Endpoint: GET /sync/history?page={page}&limit={limit}
     */
    suspend fun getHistory(accessToken: String, page: Int = 1, limit: Int = 100): Result<List<TraktHistoryItem>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/history?page=$page&limit=$limit")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val items = json.decodeFromString<List<TraktHistoryItem>>(raw)
                    Result.success(items)
                } else {
                    Result.failure(IOException("Failed to fetch Trakt watch history: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds items to watched history.
     * Endpoint: POST /sync/history
     */
    suspend fun addToHistory(accessToken: String, payload: TraktSyncItemsPayload): Result<TraktSyncResponse> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/history")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201) {
                    val raw = response.body?.string() ?: ""
                    val res = json.decodeFromString<TraktSyncResponse>(raw)
                    Result.success(res)
                } else {
                    Result.failure(IOException("Failed to add to Trakt history: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes items from watched history.
     * Endpoint: POST /sync/history/remove
     */
    suspend fun removeFromHistory(accessToken: String, payload: TraktSyncItemsPayload): Result<TraktSyncResponse> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/sync/history/remove")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    val raw = response.body?.string() ?: ""
                    val res = json.decodeFromString<TraktSyncResponse>(raw)
                    Result.success(res)
                } else {
                    Result.failure(IOException("Failed to remove from Trakt history: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches user custom personal lists.
     * Endpoint: GET /users/me/lists
     */
    suspend fun getUserLists(accessToken: String): Result<List<TraktCustomList>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/users/me/lists")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val items = json.decodeFromString<List<TraktCustomList>>(raw)
                    Result.success(items)
                } else {
                    Result.failure(IOException("Failed to fetch Trakt custom lists: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches items inside a custom list.
     * Endpoint: GET /users/me/lists/{id}/items
     */
    suspend fun getListItems(accessToken: String, listSlug: String): Result<List<TraktListItem>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("No access token"))
        try {
            val request = Request.Builder()
                .url("$BASE_URL/users/me/lists/$listSlug/items")
                .header("Content-Type", "application/json")
                .header("trakt-api-version", "2")
                .header("trakt-api-key", clientId)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val items = json.decodeFromString<List<TraktListItem>>(raw)
                    Result.success(items)
                } else {
                    Result.failure(IOException("Failed to fetch Trakt list items: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Volatile
    var pendingCodeVerifier: String? = null
        private set

    @Volatile
    var pendingState: String? = null
        private set

    /**
     * Constructs the Trakt OAuth 2.0 authorization URL with mandatory PKCE (S256).
     * Endpoint: https://trakt.tv/oauth/authorize
     * Default redirect URI is "http://localhost".
     */
    fun getAuthorizationUrl(
        redirectUri: String = REDIRECT_URI,
        state: String? = null
    ): String {
        val randomBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        pendingCodeVerifier = verifier

        val actualState = state ?: run {
            val stateBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
        }
        pendingState = actualState

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedChallenge = URLEncoder.encode(challenge, "UTF-8")
        val encodedState = URLEncoder.encode(actualState, "UTF-8")

        return "$OAUTH_AUTHORIZE_URL?response_type=code&client_id=$clientId&redirect_uri=$encodedRedirect&code_challenge=$encodedChallenge&code_challenge_method=S256&state=$encodedState"
    }

    /**
     * Exchanges an OAuth 2.0 authorization code for an access token via PKCE code_verifier.
     * Does NOT require client_secret.
     */
    suspend fun exchangeCodeForToken(
        code: String,
        redirectUri: String = REDIRECT_URI,
        codeVerifier: String? = null
    ): Result<TraktTokenResponse> = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code cannot be blank"))
        try {
            val verifier = codeVerifier?.ifBlank { null } ?: pendingCodeVerifier
            val body = mutableMapOf<String, String>(
                "code" to code,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "grant_type" to "authorization_code"
            )
            if (!verifier.isNullOrBlank()) {
                body["code_verifier"] = verifier
                // For PKCE public clients, RFC 7636 and Trakt strictly prohibit sending client_secret
            } else if (clientSecret.isNotBlank()) {
                body["client_secret"] = clientSecret
            }

            var token: TraktTokenResponse? = null
            var lastError: String? = null

            val urisToTry = if (redirectUri.endsWith("/")) {
                listOf(redirectUri, redirectUri.removeSuffix("/"))
            } else {
                listOf(redirectUri, "$redirectUri/")
            }

            for (tryUri in urisToTry) {
                body["redirect_uri"] = tryUri
                val request = Request.Builder()
                    .url("$BASE_URL/oauth/token")
                    .header("Content-Type", "application/json")
                    .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                    .build()

                val callFailed = httpClient.newCall(request).execute().use { response ->
                    val raw = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        token = json.decodeFromString<TraktTokenResponse>(raw)
                        pendingCodeVerifier = null
                        pendingState = null
                        null
                    } else {
                        "HTTP ${response.code} ($raw)"
                    }
                }

                if (token != null) {
                    return@withContext Result.success(token!!)
                }
                lastError = callFailed
                if (lastError?.contains("redirect_uri") != true) {
                    break
                }
            }

            Result.failure(IOException("Trakt token exchange failed: $lastError"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes an expired Trakt access token.
     * Does NOT require client_secret when using PKCE.
     */
    suspend fun refreshToken(
        refreshToken: String,
        redirectUri: String = REDIRECT_URI
    ): Result<TraktTokenResponse> = withContext(Dispatchers.IO) {
        if (refreshToken.isBlank()) return@withContext Result.failure(IllegalArgumentException("Refresh token cannot be blank"))
        try {
            val body = mutableMapOf<String, String>(
                "refresh_token" to refreshToken,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "grant_type" to "refresh_token"
            )
            if (clientSecret.isNotBlank()) {
                body["client_secret"] = clientSecret
            }

            val request = Request.Builder()
                .url("$BASE_URL/oauth/token")
                .header("Content-Type", "application/json")
                .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val token = json.decodeFromString<TraktTokenResponse>(raw)
                    Result.success(token)
                } else {
                    Result.failure(IOException("Trakt token refresh failed: HTTP ${response.code} ($raw)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class PendingAuthException(message: String) : Exception(message)

    companion object {
        const val BASE_URL = "https://api.trakt.tv"
        const val OAUTH_AUTHORIZE_URL = "https://trakt.tv/oauth/authorize"
        const val REDIRECT_URI = "http://localhost"
        // Public open-source Trakt app client credentials
        val DEFAULT_CLIENT_ID: String = BuildConfig.TRAKT_CLIENT_ID
        val DEFAULT_CLIENT_SECRET: String = BuildConfig.TRAKT_CLIENT_SECRET

        @Volatile
        private var instance: TraktClient? = null

        fun getInstance(): TraktClient {
            return instance ?: synchronized(this) {
                instance ?: TraktClient().also { instance = it }
            }
        }
    }
}
