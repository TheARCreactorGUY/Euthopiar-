package com.euthopiar.core.sync.supabase

import com.euthopiar.core.BuildConfig
import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SupabaseAuthClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    var serverClockSkewMs: Long = 0L
        private set

    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String? = null): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val reqBody = SupabaseIdTokenRequest(
                provider = "google",
                idToken = idToken,
                nonce = nonce
            )
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=id_token")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(json.encodeToString(reqBody).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                updateClockSkew(response.header("Date"))
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Google Sign-In failed [HTTP ${response.code}]: $err"))
                }
                val raw = response.body?.string() ?: ""
                val tokenResp = json.decodeFromString<SupabaseAuthTokenResponse>(raw)
                val session = mapToSession(tokenResp)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(refreshToken: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val reqBody = SupabaseRefreshTokenRequest(refreshToken = refreshToken)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(json.encodeToString(reqBody).toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                updateClockSkew(response.header("Date"))
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Session refresh failed [HTTP ${response.code}]: $err"))
                }
                val raw = response.body?.string() ?: ""
                val tokenResp = json.decodeFromString<SupabaseAuthTokenResponse>(raw)
                val session = mapToSession(tokenResp)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/logout")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                updateClockSkew(response.header("Date"))
                if (!response.isSuccessful && response.code != 401 && response.code != 404) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Sign out failed [HTTP ${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToSession(resp: SupabaseAuthTokenResponse): SupabaseSession {
        val user = resp.user
        val meta = user?.userMetadata
        val displayName = meta?.get("full_name")?.jsonPrimitive?.content
            ?: meta?.get("name")?.jsonPrimitive?.content
            ?: user?.email?.substringBefore("@")
        val avatarUrl = meta?.get("avatar_url")?.jsonPrimitive?.content
            ?: meta?.get("picture")?.jsonPrimitive?.content
        val now = System.currentTimeMillis()
        val expiresAtMs = now + (resp.expiresIn * 1000L)

        return SupabaseSession(
            accessToken = resp.accessToken,
            refreshToken = resp.refreshToken,
            expiresIn = resp.expiresIn,
            expiresAtMs = expiresAtMs,
            userId = user?.id ?: "",
            email = user?.email,
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }

    private fun updateClockSkew(dateHeader: String?) {
        if (dateHeader.isNullOrBlank()) return
        try {
            val serverZdt = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME)
            val serverMillis = serverZdt.toInstant().toEpochMilli()
            serverClockSkewMs = serverMillis - System.currentTimeMillis()
        } catch (_: Exception) {
            // Ignore clock skew parse failure, fallback to 0
        }
    }
}
