package com.euthopiar.core.sync.simkl

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

    @Volatile
    var pendingCodeVerifier: String? = null
        private set

    @Volatile
    var pendingState: String? = null
        private set

    /**
     * Constructs the Simkl OAuth 2.0 (Auth V2) authorization URL with mandatory PKCE (S256).
     * Endpoint: https://simkl.com/oauth2/authorize
     * Default redirect URI is "Euthopiar://auth/simkl".
     */
    fun getAuthorizationUrl(
        redirectUri: String = REDIRECT_URI,
        state: String? = null
    ): String {
        val randomBytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val verifier = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        pendingCodeVerifier = verifier

        val actualState = state ?: run {
            val stateBytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
        }
        pendingState = actualState

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

        val encodedRedirect = java.net.URLEncoder.encode(redirectUri, "UTF-8")
        val encodedChallenge = java.net.URLEncoder.encode(challenge, "UTF-8")
        val encodedState = java.net.URLEncoder.encode(actualState, "UTF-8")

        return "$OAUTH_AUTHORIZE_URL?response_type=code&client_id=$clientId&redirect_uri=$encodedRedirect&code_challenge=$encodedChallenge&code_challenge_method=S256&state=$encodedState"
    }

    /**
     * Exchanges an OAuth 2.0 authorization code for an access token via Auth V2 (/oauth2/token) using PKCE code_verifier.
     */
    suspend fun exchangeCodeForToken(
        code: String,
        redirectUri: String = REDIRECT_URI,
        codeVerifier: String? = null
    ): Result<SimklTokenResponse> = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code cannot be blank"))
        try {
            val verifier = codeVerifier ?: pendingCodeVerifier
            val body = mutableMapOf<String, String>(
                "grant_type" to "authorization_code",
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "code" to code
            )
            if (!verifier.isNullOrBlank()) {
                body["code_verifier"] = verifier
            }

            val request = Request.Builder()
                .url("$BASE_URL/oauth2/token")
                .header("Content-Type", "application/json")
                .header("simkl-api-key", clientId)
                .post(json.encodeToString(body).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val token = json.decodeFromString<SimklTokenResponse>(raw)
                    pendingCodeVerifier = null
                    pendingState = null
                    Result.success(token)
                } else {
                    Result.failure(IOException("Simkl token exchange failed: HTTP ${response.code} ($raw)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BASE_URL = "https://api.simkl.com"
        const val OAUTH_AUTHORIZE_URL = "https://simkl.com/oauth2/authorize"
        const val REDIRECT_URI = "Euthopiar://auth/simkl"
        val DEFAULT_CLIENT_ID: String = BuildConfig.SIMKL_CLIENT_ID

        @Volatile
        private var instance: SimklClient? = null

        fun getInstance(): SimklClient {
            return instance ?: synchronized(this) {
                instance ?: SimklClient().also { instance = it }
            }
        }
    }
}
