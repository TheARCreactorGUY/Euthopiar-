package com.euthopiar.core.sync.supabase

import com.euthopiar.core.BuildConfig
import com.euthopiar.core.network.DohDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

class SupabaseRestClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val tokenAuthenticator: SupabaseTokenAuthenticator,
    baseClient: OkHttpClient = DohDns.createOkHttpClient()
) {
    private val httpClient: OkHttpClient = baseClient.newBuilder()
        .authenticator(tokenAuthenticator)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun buildAuthorizedRequest(builder: Request.Builder): Request.Builder {
        val token = tokenAuthenticator.getValidAccessToken()
        return builder
            .header("apikey", anonKey)
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
    }

    suspend fun upsertAccount(account: CloudUserAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(account)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/user_accounts?on_conflict=user_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert account failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert account failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert account exception", e)
            Result.failure(e)
        }
    }

    suspend fun upsertProfile(profile: CloudUserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(profile)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?on_conflict=user_id,profile_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert profile failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert profile failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert profile exception", e)
            Result.failure(e)
        }
    }

    suspend fun upsertProfilesBatch(profiles: List<CloudUserProfile>): Result<Unit> = withContext(Dispatchers.IO) {
        if (profiles.isEmpty()) return@withContext Result.success(Unit)
        try {
            val deduped = profiles.associateBy { "${it.userId}::${it.profileId}" }.values.toList()
            val body = json.encodeToString(deduped)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?on_conflict=user_id,profile_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert profiles batch failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert profiles batch failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert profiles batch exception", e)
            Result.failure(e)
        }
    }

    suspend fun pullProfiles(userId: String): Result<List<CloudUserProfile>> = withContext(Dispatchers.IO) {
        try {
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?user_id=eq.$encodedUserId&is_deleted=eq.false&order=client_updated_at_ms.asc")
                    .get()
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Pull profiles failed [${response.code}]: $err"))
                }
                val raw = response.body?.string() ?: "[]"
                val profiles = json.decodeFromString<List<CloudUserProfile>>(raw)
                Result.success(profiles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergeUserSettings(
        settingsMap: Map<String, String>,
        clientUpdatedAtMs: Long
    ): Result<Unit> {
        val wrappedMap = settingsMap.mapValues { (_, value) ->
            CloudSettingEntry(value = value, ts = clientUpdatedAtMs)
        }
        return mergeUserSettingsEntries(wrappedMap)
    }

    suspend fun mergeUserSettingsEntries(
        entriesMap: Map<String, CloudSettingEntry>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (entriesMap.isEmpty()) return@withContext Result.success(Unit)
        try {
            val payload = buildJsonObject {
                put("p_incoming_settings", json.encodeToJsonElement(entriesMap))
            }
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/merge_user_settings")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Merge settings failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullUserSettings(userId: String): Result<CloudUserSettings?> = withContext(Dispatchers.IO) {
        try {
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/user_settings?user_id=eq.$encodedUserId&limit=1")
                    .get()
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Pull settings failed [${response.code}]: $err"))
                }
                val raw = response.body?.string() ?: "[]"
                val list = json.decodeFromString<List<CloudUserSettings>>(raw)
                Result.success(list.firstOrNull())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertPlaybackBatch(items: List<CloudPlaybackItem>): Result<Unit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(Unit)
        try {
            // Deduplicate by composite primary key (user_id, profile_id, provider, media_id), keeping latest row
            val deduped = items
                .groupBy { "${it.userId}::${it.profileId}::${it.provider}::${it.mediaId}" }
                .mapValues { (_, group) ->
                    group.maxWithOrNull(
                        compareBy<CloudPlaybackItem> { it.lastWatchedTimestamp }
                            .thenBy { it.currentPositionMs }
                    )!!
                }
                .values
                .toList()

            // First try conflict-safe server RPC (handles clock skew & GREATEST position)
            val rpcPayload = buildJsonObject {
                put("p_rows", json.encodeToJsonElement(deduped))
            }
            val rpcRequest = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/upsert_playback_history_batch")
                    .header("Content-Type", "application/json")
                    .post(rpcPayload.toString().toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(rpcRequest).execute().use { rpcResponse ->
                if (rpcResponse.isSuccessful) {
                    return@withContext Result.success(Unit)
                }
            }

            // Fallback to direct REST upsert if RPC is unavailable
            val body = json.encodeToString(deduped)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/playback_history?on_conflict=user_id,profile_id,provider,media_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert playback batch failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert playback batch failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert playback batch exception", e)
            Result.failure(e)
        }
    }

    suspend fun pullPlaybackDelta(
        userId: String,
        sinceWatermark: String? = null
    ): Result<List<CloudPlaybackItem>> = withContext(Dispatchers.IO) {
        try {
            val allResults = mutableListOf<CloudPlaybackItem>()
            var currentWatermark = sinceWatermark
            val pageSize = 500
            var pageCount = 0
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val selectProjection = "user_id,profile_id,provider,media_id,title,episode_title,season_number,episode_number,poster_url,backdrop_url,current_position_ms,duration_ms,is_completed,is_dismissed,is_deleted,media_type,episode_data,last_watched_timestamp,updated_at"

            while (pageCount < 10) {
                pageCount++
                var url = "$supabaseUrl/rest/v1/playback_history?user_id=eq.$encodedUserId&select=$selectProjection"
                if (!currentWatermark.isNullOrBlank()) {
                    val encodedWatermark = URLEncoder.encode(currentWatermark, Charsets.UTF_8.name())
                    url += "&updated_at=gt.$encodedWatermark"
                }
                url += "&order=updated_at.asc&limit=$pageSize"

                val request = buildAuthorizedRequest(
                    Request.Builder().url(url).get()
                ).build()

                val pageList = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: ""
                        android.util.Log.e("SupabaseRestClient", "Pull playback failed [${response.code}]: $err")
                        return@withContext Result.failure(IOException("Pull playback failed [${response.code}]: $err"))
                    }
                    val raw = response.body?.string() ?: "[]"
                    json.decodeFromString<List<CloudPlaybackItem>>(raw)
                }

                allResults.addAll(pageList)
                val nextWatermark = pageList.lastOrNull()?.updatedAt
                if (pageList.size < pageSize || nextWatermark.isNullOrBlank() || nextWatermark == currentWatermark) {
                    break
                }
                currentWatermark = nextWatermark
            }

            Result.success(allResults)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Pull playback exception", e)
            Result.failure(e)
        }
    }

    suspend fun upsertWatchlistBatch(items: List<CloudWatchlistItem>): Result<Unit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(Unit)
        try {
            val deduped = items
                .groupBy { "${it.userId}::${it.profileId}::${it.mediaId}" }
                .mapValues { (_, group) ->
                    group.maxByOrNull { it.clientUpdatedAtMs }!!
                }
                .values
                .toList()
            val body = json.encodeToString(deduped)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/watchlist?on_conflict=user_id,profile_id,media_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert watchlist batch failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert watchlist batch failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert watchlist batch exception", e)
            Result.failure(e)
        }
    }

    suspend fun pullWatchlistDelta(
        userId: String,
        sinceWatermark: String? = null
    ): Result<List<CloudWatchlistItem>> = withContext(Dispatchers.IO) {
        try {
            val allResults = mutableListOf<CloudWatchlistItem>()
            var currentWatermark = sinceWatermark
            val pageSize = 500
            var pageCount = 0
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val selectProjection = "user_id,profile_id,media_id,title,poster_url,backdrop_url,media_type,rating,release_year,status,is_deleted,added_timestamp,client_updated_at_ms,updated_at"

            while (pageCount < 10) {
                pageCount++
                var url = "$supabaseUrl/rest/v1/watchlist?user_id=eq.$encodedUserId&select=$selectProjection"
                if (!currentWatermark.isNullOrBlank()) {
                    val encodedWatermark = URLEncoder.encode(currentWatermark, Charsets.UTF_8.name())
                    url += "&updated_at=gt.$encodedWatermark"
                }
                url += "&order=updated_at.asc&limit=$pageSize"

                val request = buildAuthorizedRequest(
                    Request.Builder().url(url).get()
                ).build()

                val pageList = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: ""
                        android.util.Log.e("SupabaseRestClient", "Pull watchlist failed [${response.code}]: $err")
                        return@withContext Result.failure(IOException("Pull watchlist failed [${response.code}]: $err"))
                    }
                    val raw = response.body?.string() ?: "[]"
                    json.decodeFromString<List<CloudWatchlistItem>>(raw)
                }

                allResults.addAll(pageList)
                val nextWatermark = pageList.lastOrNull()?.updatedAt
                if (pageList.size < pageSize || nextWatermark.isNullOrBlank() || nextWatermark == currentWatermark) {
                    break
                }
                currentWatermark = nextWatermark
            }

            Result.success(allResults)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Pull watchlist exception", e)
            Result.failure(e)
        }
    }

    suspend fun upsertPlaylistsBatch(items: List<CloudPlaylist>): Result<Unit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(Unit)
        try {
            val deduped = items
                .groupBy { "${it.userId}::${it.profileId}::${it.playlistId}" }
                .mapValues { (_, group) ->
                    group.maxByOrNull { it.clientUpdatedAtMs }!!
                }
                .values
                .toList()
            val body = json.encodeToString(deduped)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/user_playlists?on_conflict=user_id,profile_id,playlist_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Upsert playlists batch failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullPlaylistsDelta(
        userId: String,
        sinceWatermark: String? = null
    ): Result<List<CloudPlaylist>> = withContext(Dispatchers.IO) {
        try {
            val allResults = mutableListOf<CloudPlaylist>()
            var currentWatermark = sinceWatermark
            val pageSize = 200
            var pageCount = 0
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val selectProjection = "user_id,profile_id,playlist_id,title,is_system,items_json,created_at_ms,last_played_at_ms,is_deleted,client_updated_at_ms,updated_at"

            while (pageCount < 10) {
                pageCount++
                var url = "$supabaseUrl/rest/v1/user_playlists?user_id=eq.$encodedUserId&select=$selectProjection"
                if (!currentWatermark.isNullOrBlank()) {
                    val encodedWatermark = URLEncoder.encode(currentWatermark, Charsets.UTF_8.name())
                    url += "&updated_at=gt.$encodedWatermark"
                }
                url += "&order=updated_at.asc&limit=$pageSize"

                val request = buildAuthorizedRequest(
                    Request.Builder().url(url).get()
                ).build()

                val pageList = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: ""
                        return@withContext Result.failure(IOException("Pull playlists failed [${response.code}]: $err"))
                    }
                    val raw = response.body?.string() ?: "[]"
                    json.decodeFromString<List<CloudPlaylist>>(raw)
                }

                allResults.addAll(pageList)
                val nextWatermark = pageList.lastOrNull()?.updatedAt
                if (pageList.size < pageSize || nextWatermark.isNullOrBlank() || nextWatermark == currentWatermark) {
                    break
                }
                currentWatermark = nextWatermark
            }

            Result.success(allResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertAddonsBatch(items: List<CloudUserAddon>): Result<Unit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(Unit)
        try {
            val deduped = items
                .groupBy { "${it.userId}::${it.manifestUrl}" }
                .mapValues { (_, group) ->
                    group.maxByOrNull { it.clientUpdatedAtMs }!!
                }
                .values
                .toList()

            // First try conflict-safe server RPC (enforces 60-second cooldown & deletion protection)
            val rpcPayload = buildJsonObject {
                put("p_rows", json.encodeToJsonElement(deduped))
            }
            val rpcRequest = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/upsert_user_addons_batch")
                    .header("Content-Type", "application/json")
                    .post(rpcPayload.toString().toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(rpcRequest).execute().use { rpcResponse ->
                if (rpcResponse.isSuccessful) {
                    return@withContext Result.success(Unit)
                }
            }

            // Fallback to direct REST upsert if RPC is unavailable
            val body = json.encodeToString(deduped)
            val request = buildAuthorizedRequest(
                Request.Builder()
                    .url("$supabaseUrl/rest/v1/user_addons?on_conflict=user_id,manifest_url")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toRequestBody(jsonMediaType))
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    android.util.Log.e("SupabaseRestClient", "Upsert addons batch failed [${response.code}]: $err")
                    return@withContext Result.failure(IOException("Upsert addons batch failed [${response.code}]: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Upsert addons batch exception", e)
            Result.failure(e)
        }
    }

    suspend fun pullAddonsDelta(
        userId: String,
        sinceWatermark: String? = null
    ): Result<List<CloudUserAddon>> = withContext(Dispatchers.IO) {
        try {
            val allResults = mutableListOf<CloudUserAddon>()
            var currentWatermark = sinceWatermark
            val pageSize = 100
            var pageCount = 0
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val selectProjection = "user_id,manifest_url,addon_type,name,is_deleted,client_updated_at_ms,updated_at"

            while (pageCount < 10) {
                pageCount++
                var url = "$supabaseUrl/rest/v1/user_addons?user_id=eq.$encodedUserId&select=$selectProjection"
                if (!currentWatermark.isNullOrBlank()) {
                    val encodedWatermark = URLEncoder.encode(currentWatermark, Charsets.UTF_8.name())
                    url += "&updated_at=gt.$encodedWatermark"
                }
                url += "&order=updated_at.asc&limit=$pageSize"

                val request = buildAuthorizedRequest(
                    Request.Builder().url(url).get()
                ).build()

                val pageList = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: ""
                        android.util.Log.e("SupabaseRestClient", "Pull addons failed [${response.code}]: $err")
                        return@withContext Result.failure(IOException("Pull addons failed [${response.code}]: $err"))
                    }
                    val raw = response.body?.string() ?: "[]"
                    json.decodeFromString<List<CloudUserAddon>>(raw)
                }

                allResults.addAll(pageList)
                val nextWatermark = pageList.lastOrNull()?.updatedAt
                if (pageList.size < pageSize || nextWatermark.isNullOrBlank() || nextWatermark == currentWatermark) {
                    break
                }
                currentWatermark = nextWatermark
            }

            Result.success(allResults)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseRestClient", "Pull addons exception", e)
            Result.failure(e)
        }
    }
}
