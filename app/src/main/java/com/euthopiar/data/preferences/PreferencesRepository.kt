package com.euthopiar.data.preferences

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.euthopiar.core.model.ContentRegionPreset
import com.euthopiar.core.sync.supabase.SupabaseSession
import com.euthopiar.ui.longpress.LongPressSheetStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "euthopiar_settings")

enum class CardScaleMode(
    val id: String,
    val label: String,
    val description: String,
    val verticalFactor: Float,
    val horizontalFactor: Float
) {
    FLAGSHIP("flagship", "Large (Default)", "2.0x Vertical, 1.5x Horizontal cards", 2.0f, 1.5f),
    BALANCED("balanced", "Balanced", "1.5x Vertical, 1.25x Horizontal cards", 1.5f, 1.25f),
    COMPACT("compact", "Compact (Original)", "1.0x Vertical, 1.0x Horizontal cards", 1.0f, 1.0f);

    companion object {
        fun fromId(id: String?): CardScaleMode = entries.firstOrNull { it.id == id } ?: FLAGSHIP
    }
}

enum class NavbarStyle(
    val id: String,
    val label: String,
    val description: String
) {
    FLOATING("floating", "Floating glass pill", "Rounded floating pill above the navigation bar"),
    @Deprecated("Docked style is removed; silently falls back to FLOATING")
    DOCKED("docked", "Edge-to-edge bar", "Full width docked at the bottom of the screen");

    companion object {
        fun fromId(id: String?): NavbarStyle = FLOATING
    }
}

enum class Top10Style(
    val id: String,
    val label: String,
    val description: String = ""
) {
    PosterTinted("poster_tinted", "Poster-tinted", "Dynamic poster color bleed and glowing numerals"),
    GlassTab("glass_tab", "Glass tab", "Frosted acrylic tab container with bold numeral");

    companion object {
        fun fromId(id: String?): Top10Style = entries.firstOrNull { it.id == id } ?: PosterTinted
    }
}

enum class SectionLabelStyle(
    val id: String,
    val label: String,
    val description: String = ""
) {
    AccentBar("accent_bar", "Accent bar", "3dp accent colored bar beside header"),
    Eyebrow("eyebrow", "Eyebrow", "Uppercase tracking label above title"),
    RuleLine("rule_line", "Rule line", "Subtle horizontal divider extending right");

    companion object {
        fun fromId(id: String?): SectionLabelStyle = entries.firstOrNull { it.id == id } ?: AccentBar
    }
}

/**
 * Thread-safe preferences repository backed by Jetpack DataStore Preferences.
 */
class PreferencesRepository private constructor(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val AUTO_PLAY_NEXT_EPISODE = booleanPreferencesKey("auto_play_next_episode")
        val SEEK_INTERVAL_SECONDS = intPreferencesKey("seek_interval_seconds")
        val SUBTITLE_SIZE_SP = intPreferencesKey("subtitle_size_sp")
        val SUBTITLE_BACKGROUND_ENABLED = booleanPreferencesKey("subtitle_background_enabled")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_OUTLINE = booleanPreferencesKey("subtitle_outline")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val CUSTOM_DOWNLOAD_DIR = stringPreferencesKey("custom_download_dir")
        val FIRST_DOWNLOAD_ALERT_DISMISSED = booleanPreferencesKey("first_download_alert_dismissed")
        val REPOSITORY_URLS = stringSetPreferencesKey("repository_urls")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val DISABLED_PLUGINS = stringSetPreferencesKey("disabled_plugins")
        val EXTRA_CONTENT_ENABLED = booleanPreferencesKey("extra_content_enabled")
        val CONTENT_REGION_PRESET = stringPreferencesKey("content_region_preset")
        val CARD_SCALE_MODE = stringPreferencesKey("card_scale_mode")
        val NAVBAR_STYLE = stringPreferencesKey("navbar_style")
        val FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_csv")
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val FAVORITE_MEDIA_IDS = stringPreferencesKey("favorite_media_ids")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val LIBRARY_LAST_TAB = intPreferencesKey("library_last_tab")
        val LIBRARY_OFFLINE_ONLY = booleanPreferencesKey("library_offline_only")

        // 1.1 Ecosystem & External Sync
        val TRAKT_ACCESS_TOKEN = stringPreferencesKey("trakt_access_token")
        val TRAKT_REFRESH_TOKEN = stringPreferencesKey("trakt_refresh_token")
        val TRAKT_USERNAME = stringPreferencesKey("trakt_username")
        val TRAKT_SCROBBLE_ENABLED = booleanPreferencesKey("trakt_scrobble_enabled")
        val TRAKT_CODE_VERIFIER = stringPreferencesKey("trakt_code_verifier")
        val SIMKL_ACCESS_TOKEN = stringPreferencesKey("simkl_access_token")
        val SIMKL_SCROBBLE_ENABLED = booleanPreferencesKey("simkl_scrobble_enabled")
        val SIMKL_CODE_VERIFIER = stringPreferencesKey("simkl_code_verifier")
        val TRAKT_SYNC_WATCHLIST = booleanPreferencesKey("trakt_sync_watchlist")
        val TRAKT_SYNC_PLAYBACK = booleanPreferencesKey("trakt_sync_playback")
        val TRAKT_SYNC_HISTORY = booleanPreferencesKey("trakt_sync_history")
        val TRAKT_SYNC_LISTS = booleanPreferencesKey("trakt_sync_lists")
        val SIMKL_SYNC_WATCHLIST = booleanPreferencesKey("simkl_sync_watchlist")
        val SIMKL_SYNC_HISTORY = booleanPreferencesKey("simkl_sync_history")
        val LAST_MEDIA_SYNC_TIMESTAMP = longPreferencesKey("last_media_sync_timestamp")
        val REAL_DEBRID_API_KEY = stringPreferencesKey("real_debrid_api_key")
        val ALL_DEBRID_API_KEY = stringPreferencesKey("all_debrid_api_key")
        val TORBOX_API_KEY = stringPreferencesKey("torbox_api_key")
        val ACTIVE_DEBRID_PROVIDER = stringPreferencesKey("active_debrid_provider")
        val DEBRID_ENABLED = booleanPreferencesKey("debrid_enabled")

        // 1.3 Player Automations & External Subtitles
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val SKIP_INTRO_DURATION_SECONDS = intPreferencesKey("skip_intro_duration_seconds")
        val SKIP_OUTRO_DURATION_SECONDS = intPreferencesKey("skip_outro_duration_seconds")
        val COUNTDOWN_NEXT_EPISODE_SECONDS = intPreferencesKey("countdown_next_episode_seconds")
        val EXTERNAL_SUBTITLES_ENABLED = booleanPreferencesKey("external_subtitles_enabled")
        val TOP_10_STYLE = stringPreferencesKey("top_10_style")
        val SECTION_LABEL_STYLE = stringPreferencesKey("section_label_style")
        val LONG_PRESS_STYLE = stringPreferencesKey("long_press_style")
        val MEDIA_REACTIONS = stringPreferencesKey("media_reactions")
        val PLAYBACK_BUFFER_SIZE_MB = intPreferencesKey("playback_buffer_size_mb")
        val HARDWARE_ACCELERATION_ENABLED = booleanPreferencesKey("hardware_acceleration_enabled")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val SMART_FAILOVER_ENABLED = booleanPreferencesKey("smart_failover_enabled")
        val AUDIO_PASSTHROUGH_ENABLED = booleanPreferencesKey("audio_passthrough_enabled")

        // 1.4 Application Update Preferences
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val INCLUDE_PRERELEASES = booleanPreferencesKey("include_prereleases")
        val SKIPPED_UPDATE_VERSION = stringPreferencesKey("skipped_update_version")
        val LAST_UPDATE_CHECK_TIMESTAMP = longPreferencesKey("last_update_check_timestamp")

        // 1.5 Support & Donation Preferences
        val LAST_DONATION_PROMPT_TIMESTAMP = longPreferencesKey("last_donation_prompt_timestamp")
        val LAST_APP_VERSION = stringPreferencesKey("last_app_version")
        val LAST_APP_VERSION_CODE = intPreferencesKey("last_app_version_code")
        val WALKTHROUGH_COMPLETED_VERSION = stringPreferencesKey("walkthrough_completed_version")

        // 1.6 Supabase Cloud Sync
        val SUPABASE_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
        val SUPABASE_REFRESH_TOKEN = stringPreferencesKey("supabase_refresh_token")
        val SUPABASE_EXPIRES_IN = longPreferencesKey("supabase_expires_in")
        val SUPABASE_EXPIRES_AT = longPreferencesKey("supabase_expires_at")
        val SUPABASE_USER_ID = stringPreferencesKey("supabase_user_id")
        val SUPABASE_USER_EMAIL = stringPreferencesKey("supabase_user_email")
        val SUPABASE_USER_DISPLAY_NAME = stringPreferencesKey("supabase_user_display_name")
        val SUPABASE_USER_AVATAR = stringPreferencesKey("supabase_user_avatar")
        val SUPABASE_LAST_SYNC_TIMESTAMP = longPreferencesKey("supabase_last_sync_timestamp")
        val SUPABASE_LAST_SYNCED_USER_ID = stringPreferencesKey("supabase_last_synced_user_id")
        val SUPABASE_PLAYBACK_WATERMARK = stringPreferencesKey("supabase_playback_watermark")
        val SUPABASE_WATCHLIST_WATERMARK = stringPreferencesKey("supabase_watchlist_watermark")
        val SUPABASE_PLAYLISTS_WATERMARK = stringPreferencesKey("supabase_playlists_watermark")
        val SUPABASE_CLOUD_SYNC_ENABLED = booleanPreferencesKey("supabase_cloud_sync_enabled")
        val SUPABASE_SYNC_PLAYBACK = booleanPreferencesKey("supabase_sync_playback")
        val SUPABASE_SYNC_WATCHLIST = booleanPreferencesKey("supabase_sync_watchlist")
        val SUPABASE_SYNC_PLAYLISTS = booleanPreferencesKey("supabase_sync_playlists")
        val SUPABASE_SYNC_SETTINGS = booleanPreferencesKey("supabase_sync_settings")
    }
    val dohProvider: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DOH_PROVIDER] ?: "CLOUDFLARE"
        }
        .distinctUntilChanged()

    suspend fun setDohProvider(provider: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.DOH_PROVIDER] = provider
        }
    }

    val defaultQuality: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "Auto"
        }

    val autoPlayNextEpisode: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_PLAY_NEXT_EPISODE] ?: true
        }

    val seekIntervalSeconds: Flow<Int> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SEEK_INTERVAL_SECONDS] ?: 10
        }

    val subtitleSizeSp: Flow<Int> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUBTITLE_SIZE_SP] ?: 18
        }

    val subtitleBackgroundEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED] ?: true
        }

    val wifiOnlyDownloads: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] ?: false
        }

    val customDownloadDir: Flow<String?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CUSTOM_DOWNLOAD_DIR]
        }

    val firstDownloadAlertDismissed: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_DOWNLOAD_ALERT_DISMISSED] ?: false
        }

    val repositoryUrls: Flow<Set<String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.REPOSITORY_URLS] ?: emptySet()
        }

    val activeProvider: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROVIDER] ?: ""
        }

    val disabledPlugins: Flow<Set<String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DISABLED_PLUGINS] ?: emptySet()
        }

    val extraContentEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.EXTRA_CONTENT_ENABLED] ?: false
        }

    val contentRegionPreset: Flow<ContentRegionPreset> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            ContentRegionPreset.fromId(preferences[PreferencesKeys.CONTENT_REGION_PRESET])
        }
        .distinctUntilChanged()

    suspend fun setContentRegionPreset(preset: ContentRegionPreset) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTENT_REGION_PRESET] = preset.id
        }
    }

    val cardScaleMode: Flow<CardScaleMode> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            CardScaleMode.fromId(preferences[PreferencesKeys.CARD_SCALE_MODE])
        }

    val subtitleColor: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUBTITLE_COLOR] ?: "WHITE"
        }

    val subtitleOutline: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUBTITLE_OUTLINE] ?: true
        }

    val recentSearches: Flow<List<String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val raw = preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split("\u001F").filter { it.isNotBlank() }
        }

    suspend fun setCardScaleMode(mode: CardScaleMode) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CARD_SCALE_MODE] = mode.id
        }
    }

    suspend fun setSubtitleColor(colorName: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_COLOR] = colorName
        }
    }

    suspend fun setSubtitleOutline(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_OUTLINE] = enabled
        }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            val current = raw.split("\u001F").filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
            val updated = listOf(trimmed) + current.take(9)
            preferences[PreferencesKeys.RECENT_SEARCHES] = updated.joinToString("\u001F")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            val current = raw.split("\u001F").filter { it.isNotBlank() && !it.equals(query.trim(), ignoreCase = true) }
            preferences[PreferencesKeys.RECENT_SEARCHES] = current.joinToString("\u001F")
        }
    }

    suspend fun clearRecentSearches() {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENT_SEARCHES] = ""
        }
    }

    suspend fun restoreRecentSearches(items: List<String>) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENT_SEARCHES] = items.joinToString("\u001F")
        }
    }

    /**
     * Profile-aware recent searches stream.
     * Falls back to default recent searches if profile-specific recents do not exist yet.
     */
    fun recentSearchesForProfile(profileId: String): Flow<List<String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val profileKey = stringPreferencesKey("recent_searches_csv_$profileId")
            val raw = preferences[profileKey] ?: preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split("\u001F").filter { it.isNotBlank() }
        }

    suspend fun addRecentSearchForProfile(profileId: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.preferencesDataStore.edit { preferences ->
            val profileKey = stringPreferencesKey("recent_searches_csv_$profileId")
            val raw = preferences[profileKey] ?: preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            val current = raw.split("\u001F").filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
            val updated = listOf(trimmed) + current.take(9)
            val joined = updated.joinToString("\u001F")
            preferences[profileKey] = joined
            preferences[PreferencesKeys.RECENT_SEARCHES] = joined
        }
    }

    suspend fun removeRecentSearchForProfile(profileId: String, query: String) {
        context.preferencesDataStore.edit { preferences ->
            val profileKey = stringPreferencesKey("recent_searches_csv_$profileId")
            val raw = preferences[profileKey] ?: preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            val current = raw.split("\u001F").filter { it.isNotBlank() && !it.equals(query.trim(), ignoreCase = true) }
            val joined = current.joinToString("\u001F")
            preferences[profileKey] = joined
            preferences[PreferencesKeys.RECENT_SEARCHES] = joined
        }
    }

    suspend fun clearRecentSearchesForProfile(profileId: String) {
        context.preferencesDataStore.edit { preferences ->
            val profileKey = stringPreferencesKey("recent_searches_csv_$profileId")
            preferences[profileKey] = ""
            preferences[PreferencesKeys.RECENT_SEARCHES] = ""
        }
    }

    suspend fun restoreRecentSearchesForProfile(profileId: String, items: List<String>) {
        context.preferencesDataStore.edit { preferences ->
            val profileKey = stringPreferencesKey("recent_searches_csv_$profileId")
            val joined = items.joinToString("\u001F")
            preferences[profileKey] = joined
            preferences[PreferencesKeys.RECENT_SEARCHES] = joined
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] = quality
        }
    }

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_PLAY_NEXT_EPISODE] = enabled
        }
    }

    suspend fun setSeekIntervalSeconds(seconds: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SEEK_INTERVAL_SECONDS] = seconds
        }
    }

    suspend fun setSubtitleSizeSp(sizeSp: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_SIZE_SP] = sizeSp
        }
    }

    suspend fun setSubtitleBackgroundEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED] = enabled
        }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = enabled
        }
    }

    suspend fun setCustomDownloadDir(path: String?) {
        context.preferencesDataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.CUSTOM_DOWNLOAD_DIR)
            } else {
                preferences[PreferencesKeys.CUSTOM_DOWNLOAD_DIR] = path
            }
        }
    }

    suspend fun setFirstDownloadAlertDismissed(dismissed: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_DOWNLOAD_ALERT_DISMISSED] = dismissed
        }
    }

    suspend fun addRepositoryUrl(url: String) {
        context.preferencesDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.REPOSITORY_URLS] ?: emptySet()
            preferences[PreferencesKeys.REPOSITORY_URLS] = current + url
        }
    }

    suspend fun removeRepositoryUrl(url: String) {
        context.preferencesDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.REPOSITORY_URLS] ?: emptySet()
            preferences[PreferencesKeys.REPOSITORY_URLS] = current - url
        }
    }

    suspend fun setRepositoryUrls(urls: Set<String>) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.REPOSITORY_URLS] = urls
        }
    }

    suspend fun setActiveProvider(provider: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROVIDER] = provider
        }
    }

    suspend fun setPluginDisabled(pluginName: String, disabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.DISABLED_PLUGINS] ?: emptySet()).toMutableSet()
            if (disabled) {
                current.add(pluginName)
            } else {
                current.removeAll { it.equals(pluginName, ignoreCase = true) }
            }
            preferences[PreferencesKeys.DISABLED_PLUGINS] = current
        }
    }

    suspend fun setExtraContentEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTRA_CONTENT_ENABLED] = enabled
        }
    }

    // Trakt Preferences
    val traktAccessToken: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.TRAKT_ACCESS_TOKEN] ?: "" }.distinctUntilChanged()
    val traktRefreshToken: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.TRAKT_REFRESH_TOKEN] ?: "" }.distinctUntilChanged()
    val traktUsername: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.TRAKT_USERNAME] ?: "" }.distinctUntilChanged()
    val traktScrobbleEnabled: Flow<Boolean> = context.preferencesDataStore.data.map { it[PreferencesKeys.TRAKT_SCROBBLE_ENABLED] ?: true }.distinctUntilChanged()

    suspend fun setTraktAuth(accessToken: String, refreshToken: String, username: String) {
        context.preferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.TRAKT_ACCESS_TOKEN] = accessToken
            prefs[PreferencesKeys.TRAKT_REFRESH_TOKEN] = refreshToken
            prefs[PreferencesKeys.TRAKT_USERNAME] = username
        }
    }

    suspend fun clearTraktAuth() {
        context.preferencesDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.TRAKT_ACCESS_TOKEN)
            prefs.remove(PreferencesKeys.TRAKT_REFRESH_TOKEN)
            prefs.remove(PreferencesKeys.TRAKT_USERNAME)
        }
    }

    suspend fun setTraktScrobbleEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_SCROBBLE_ENABLED] = enabled }
    }


    val traktCodeVerifier: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.TRAKT_CODE_VERIFIER] ?: "" }.distinctUntilChanged()

    suspend fun setTraktCodeVerifier(verifier: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_CODE_VERIFIER] = verifier }
    }

    suspend fun clearTraktCodeVerifier() {
        context.preferencesDataStore.edit { it.remove(PreferencesKeys.TRAKT_CODE_VERIFIER) }
    }

    // Simkl Preferences
    val simklAccessToken: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.SIMKL_ACCESS_TOKEN] ?: "" }.distinctUntilChanged()
    val simklScrobbleEnabled: Flow<Boolean> = context.preferencesDataStore.data.map { it[PreferencesKeys.SIMKL_SCROBBLE_ENABLED] ?: true }.distinctUntilChanged()

    suspend fun setSimklAuth(token: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SIMKL_ACCESS_TOKEN] = token }
    }

    suspend fun clearSimklAuth() {
        context.preferencesDataStore.edit { it.remove(PreferencesKeys.SIMKL_ACCESS_TOKEN) }
    }

    suspend fun setSimklScrobbleEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SIMKL_SCROBBLE_ENABLED] = enabled }
    }

    val simklCodeVerifier: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.SIMKL_CODE_VERIFIER] ?: "" }.distinctUntilChanged()

    suspend fun setSimklCodeVerifier(verifier: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SIMKL_CODE_VERIFIER] = verifier }
    }

    suspend fun clearSimklCodeVerifier() {
        context.preferencesDataStore.edit { it.remove(PreferencesKeys.SIMKL_CODE_VERIFIER) }
    }

    // Media Sync Customization Preferences (Trakt & Simkl)
    val traktSyncWatchlist: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.TRAKT_SYNC_WATCHLIST] ?: true
    }.distinctUntilChanged()

    suspend fun setTraktSyncWatchlist(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_SYNC_WATCHLIST] = enabled }
    }

    val traktSyncPlayback: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.TRAKT_SYNC_PLAYBACK] ?: true
    }.distinctUntilChanged()

    suspend fun setTraktSyncPlayback(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_SYNC_PLAYBACK] = enabled }
    }

    val traktSyncHistory: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.TRAKT_SYNC_HISTORY] ?: true
    }.distinctUntilChanged()

    suspend fun setTraktSyncHistory(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_SYNC_HISTORY] = enabled }
    }

    val traktSyncLists: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.TRAKT_SYNC_LISTS] ?: true
    }.distinctUntilChanged()

    suspend fun setTraktSyncLists(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TRAKT_SYNC_LISTS] = enabled }
    }

    val simklSyncWatchlist: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.SIMKL_SYNC_WATCHLIST] ?: true
    }.distinctUntilChanged()

    suspend fun setSimklSyncWatchlist(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SIMKL_SYNC_WATCHLIST] = enabled }
    }

    val simklSyncHistory: Flow<Boolean> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.SIMKL_SYNC_HISTORY] ?: true
    }.distinctUntilChanged()

    suspend fun setSimklSyncHistory(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SIMKL_SYNC_HISTORY] = enabled }
    }

    val lastMediaSyncTimestamp: Flow<Long> = context.preferencesDataStore.data.map {
        it[PreferencesKeys.LAST_MEDIA_SYNC_TIMESTAMP] ?: 0L
    }.distinctUntilChanged()

    suspend fun setLastMediaSyncTimestamp(timestamp: Long) {
        context.preferencesDataStore.edit { it[PreferencesKeys.LAST_MEDIA_SYNC_TIMESTAMP] = timestamp }
    }

    // Debrid Preferences
    val realDebridApiKey: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.REAL_DEBRID_API_KEY] ?: "" }.distinctUntilChanged()
    val allDebridApiKey: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.ALL_DEBRID_API_KEY] ?: "" }.distinctUntilChanged()
    val torBoxApiKey: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.TORBOX_API_KEY] ?: "" }.distinctUntilChanged()
    val activeDebridProvider: Flow<String> = context.preferencesDataStore.data.map { it[PreferencesKeys.ACTIVE_DEBRID_PROVIDER] ?: "NONE" }.distinctUntilChanged()
    val debridEnabled: Flow<Boolean> = context.preferencesDataStore.data.map { it[PreferencesKeys.DEBRID_ENABLED] ?: false }.distinctUntilChanged()

    suspend fun setRealDebridApiKey(key: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.REAL_DEBRID_API_KEY] = key }
    }

    suspend fun setAllDebridApiKey(key: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.ALL_DEBRID_API_KEY] = key }
    }

    suspend fun setTorBoxApiKey(key: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.TORBOX_API_KEY] = key }
    }

    suspend fun setActiveDebridProvider(provider: String) {
        context.preferencesDataStore.edit { it[PreferencesKeys.ACTIVE_DEBRID_PROVIDER] = provider }
    }

    suspend fun setDebridEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.DEBRID_ENABLED] = enabled }
    }


    // Player Automations Preferences
    val autoSkipIntro: Flow<Boolean> = context.preferencesDataStore.data.map { it[PreferencesKeys.AUTO_SKIP_INTRO] ?: false }.distinctUntilChanged()
    val skipIntroDurationSeconds: Flow<Int> = context.preferencesDataStore.data.map { it[PreferencesKeys.SKIP_INTRO_DURATION_SECONDS] ?: 85 }.distinctUntilChanged()
    val skipOutroDurationSeconds: Flow<Int> = context.preferencesDataStore.data.map { it[PreferencesKeys.SKIP_OUTRO_DURATION_SECONDS] ?: 90 }.distinctUntilChanged()
    val countdownNextEpisodeSeconds: Flow<Int> = context.preferencesDataStore.data.map { it[PreferencesKeys.COUNTDOWN_NEXT_EPISODE_SECONDS] ?: 10 }.distinctUntilChanged()
    val externalSubtitlesEnabled: Flow<Boolean> = context.preferencesDataStore.data.map { it[PreferencesKeys.EXTERNAL_SUBTITLES_ENABLED] ?: true }.distinctUntilChanged()

    suspend fun setAutoSkipIntro(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.AUTO_SKIP_INTRO] = enabled }
    }

    suspend fun setSkipIntroDurationSeconds(seconds: Int) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SKIP_INTRO_DURATION_SECONDS] = seconds }
    }

    suspend fun setSkipOutroDurationSeconds(seconds: Int) {
        context.preferencesDataStore.edit { it[PreferencesKeys.SKIP_OUTRO_DURATION_SECONDS] = seconds }
    }

    suspend fun setCountdownNextEpisodeSeconds(seconds: Int) {
        context.preferencesDataStore.edit { it[PreferencesKeys.COUNTDOWN_NEXT_EPISODE_SECONDS] = seconds }
    }

    suspend fun setExternalSubtitlesEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[PreferencesKeys.EXTERNAL_SUBTITLES_ENABLED] = enabled }
    }

    val navbarStyle: Flow<NavbarStyle> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            NavbarStyle.fromId(preferences[PreferencesKeys.NAVBAR_STYLE])
        }
        .distinctUntilChanged()

    suspend fun setNavbarStyle(style: NavbarStyle) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.NAVBAR_STYLE] = style.id
        }
    }

    val top10Style: Flow<Top10Style> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            Top10Style.fromId(preferences[PreferencesKeys.TOP_10_STYLE])
        }
        .distinctUntilChanged()

    suspend fun setTop10Style(style: Top10Style) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TOP_10_STYLE] = style.id
        }
    }

    val sectionLabelStyle: Flow<SectionLabelStyle> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            SectionLabelStyle.fromId(preferences[PreferencesKeys.SECTION_LABEL_STYLE])
        }
        .distinctUntilChanged()

    suspend fun setSectionLabelStyle(style: SectionLabelStyle) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECTION_LABEL_STYLE] = style.id
        }
    }

    val longPressStyle: Flow<LongPressSheetStyle> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            LongPressSheetStyle.fromId(preferences[PreferencesKeys.LONG_PRESS_STYLE])
        }
        .distinctUntilChanged()

    suspend fun setLongPressStyle(style: LongPressSheetStyle) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LONG_PRESS_STYLE] = style.id
        }
    }

    val mediaReactions: Flow<Map<String, String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val raw = preferences[PreferencesKeys.MEDIA_REACTIONS] ?: ""
            if (raw.isBlank()) {
                emptyMap()
            } else {
                raw.split(";").mapNotNull { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
                }.toMap()
            }
        }
        .distinctUntilChanged()

    suspend fun setMediaReaction(mediaId: String, reaction: String?) {
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.MEDIA_REACTIONS] ?: ""
            val current = if (raw.isBlank()) {
                mutableMapOf()
            } else {
                raw.split(";").mapNotNull { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
                }.toMap().toMutableMap()
            }
            if (reaction != null && reaction != "none") {
                current[mediaId] = reaction
            } else {
                current.remove(mediaId)
            }
            preferences[PreferencesKeys.MEDIA_REACTIONS] = current.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
        com.euthopiar.data.sync.supabase.SupabaseSyncManager.getInstance(context).scheduleSync()
    }

    val reduceMotion: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.REDUCE_MOTION] ?: false
        }
        .distinctUntilChanged()

    suspend fun setReduceMotion(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.REDUCE_MOTION] = enabled
        }
    }

    val firstRunCompleted: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_RUN_COMPLETED] ?: false
        }
        .distinctUntilChanged()

    suspend fun setFirstRunCompleted(completed: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_RUN_COMPLETED] = completed
        }
    }

    val favoriteMediaIds: Flow<Set<String>> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val raw = preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
        }
        .distinctUntilChanged()

    suspend fun toggleFavorite(mediaId: String) {
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] ?: ""
            val current = if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
            val updated = if (mediaId in current) current - mediaId else current + mediaId
            preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] = updated.joinToString(",")
        }
        com.euthopiar.data.sync.supabase.SupabaseSyncManager.getInstance(context).scheduleSync()
    }

    suspend fun setFavorite(mediaId: String, isFavorite: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] ?: ""
            val current = if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
            val updated = if (isFavorite) current + mediaId else current - mediaId
            preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] = updated.joinToString(",")
        }
        com.euthopiar.data.sync.supabase.SupabaseSyncManager.getInstance(context).scheduleSync()
    }

    suspend fun setFavoritesBatch(mediaIds: Collection<String>, isFavorite: Boolean) {
        if (mediaIds.isEmpty()) return
        context.preferencesDataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] ?: ""
            val current = if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
            val updated = if (isFavorite) current + mediaIds else current - mediaIds
            preferences[PreferencesKeys.FAVORITE_MEDIA_IDS] = updated.joinToString(",")
        }
        com.euthopiar.data.sync.supabase.SupabaseSyncManager.getInstance(context).scheduleSync()
    }

    val playbackBufferSizeMb: Flow<Int> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.PLAYBACK_BUFFER_SIZE_MB] ?: 50
        }
        .distinctUntilChanged()

    suspend fun setPlaybackBufferSizeMb(sizeMb: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYBACK_BUFFER_SIZE_MB] = sizeMb
        }
    }

    val hardwareAccelerationEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.HARDWARE_ACCELERATION_ENABLED] ?: true
        }
        .distinctUntilChanged()

    suspend fun setHardwareAccelerationEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_ACCELERATION_ENABLED] = enabled
        }
    }

    val libraryLastTab: Flow<Int> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LIBRARY_LAST_TAB] ?: 1
        }
        .distinctUntilChanged()

    suspend fun setLibraryLastTab(tabIndex: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LIBRARY_LAST_TAB] = tabIndex.coerceIn(0, 2)
        }
    }

    val libraryOfflineOnly: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LIBRARY_OFFLINE_ONLY] ?: false
        }
        .distinctUntilChanged()

    suspend fun setLibraryOfflineOnly(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LIBRARY_OFFLINE_ONLY] = enabled
        }
    }

    val amoledMode: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.AMOLED_MODE] ?: true
        }
        .distinctUntilChanged()

    suspend fun setAmoledMode(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.AMOLED_MODE] = enabled
        }
    }

    val smartFailoverEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SMART_FAILOVER_ENABLED] ?: false
        }
        .distinctUntilChanged()

    suspend fun setSmartFailoverEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_FAILOVER_ENABLED] = enabled
        }
    }

    val audioPassthroughEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUDIO_PASSTHROUGH_ENABLED] ?: false
        }
        .distinctUntilChanged()

    suspend fun setAudioPassthroughEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_PASSTHROUGH_ENABLED] = enabled
        }
    }

    val autoCheckUpdates: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_CHECK_UPDATES] ?: true
        }
        .distinctUntilChanged()

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CHECK_UPDATES] = enabled
        }
    }

    val includePrereleases: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.INCLUDE_PRERELEASES] ?: false
        }
        .distinctUntilChanged()

    suspend fun setIncludePrereleases(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.INCLUDE_PRERELEASES] = enabled
        }
    }

    val skippedUpdateVersion: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SKIPPED_UPDATE_VERSION] ?: ""
        }
        .distinctUntilChanged()

    suspend fun setSkippedUpdateVersion(version: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIPPED_UPDATE_VERSION] = version
        }
    }

    val lastUpdateCheckTimestamp: Flow<Long> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIMESTAMP] ?: 0L
        }
        .distinctUntilChanged()

    suspend fun setLastUpdateCheckTimestamp(timestampMs: Long) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIMESTAMP] = timestampMs
        }
    }

    val lastDonationPromptTimestamp: Flow<Long> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_DONATION_PROMPT_TIMESTAMP] ?: 0L
        }
        .distinctUntilChanged()

    suspend fun setLastDonationPromptTimestamp(timestampMs: Long) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DONATION_PROMPT_TIMESTAMP] = timestampMs
        }
    }

    val lastAppVersion: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_APP_VERSION] ?: ""
        }
        .distinctUntilChanged()

    suspend fun setLastAppVersion(version: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APP_VERSION] = version
        }
    }

    val lastAppVersionCode: Flow<Int> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_APP_VERSION_CODE] ?: 0
        }
        .distinctUntilChanged()

    suspend fun setLastAppVersionCode(versionCode: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APP_VERSION_CODE] = versionCode
        }
    }

    val walkthroughCompletedVersion: Flow<String> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.WALKTHROUGH_COMPLETED_VERSION] ?: ""
        }
        .distinctUntilChanged()

    suspend fun setWalkthroughCompletedVersion(version: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.WALKTHROUGH_COMPLETED_VERSION] = version
        }
    }

    val supabaseSession: Flow<SupabaseSession?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val token = preferences[PreferencesKeys.SUPABASE_ACCESS_TOKEN]
            val refresh = preferences[PreferencesKeys.SUPABASE_REFRESH_TOKEN]
            val userId = preferences[PreferencesKeys.SUPABASE_USER_ID]
            if (token != null && refresh != null && userId != null) {
                SupabaseSession(
                    accessToken = token,
                    refreshToken = refresh,
                    expiresIn = preferences[PreferencesKeys.SUPABASE_EXPIRES_IN] ?: 3600L,
                    expiresAtMs = preferences[PreferencesKeys.SUPABASE_EXPIRES_AT] ?: (System.currentTimeMillis() + 3600_000L),
                    userId = userId,
                    email = preferences[PreferencesKeys.SUPABASE_USER_EMAIL],
                    displayName = preferences[PreferencesKeys.SUPABASE_USER_DISPLAY_NAME],
                    avatarUrl = preferences[PreferencesKeys.SUPABASE_USER_AVATAR]
                )
            } else null
        }
        .distinctUntilChanged()

    suspend fun setSupabaseSession(session: SupabaseSession?) {
        context.preferencesDataStore.edit { preferences ->
            if (session != null) {
                preferences[PreferencesKeys.SUPABASE_ACCESS_TOKEN] = session.accessToken
                preferences[PreferencesKeys.SUPABASE_REFRESH_TOKEN] = session.refreshToken
                preferences[PreferencesKeys.SUPABASE_EXPIRES_IN] = session.expiresIn
                preferences[PreferencesKeys.SUPABASE_EXPIRES_AT] = session.expiresAtMs
                preferences[PreferencesKeys.SUPABASE_USER_ID] = session.userId
                val email = session.email
                if (email != null) preferences[PreferencesKeys.SUPABASE_USER_EMAIL] = email else preferences.remove(PreferencesKeys.SUPABASE_USER_EMAIL)
                val displayName = session.displayName
                if (displayName != null) preferences[PreferencesKeys.SUPABASE_USER_DISPLAY_NAME] = displayName else preferences.remove(PreferencesKeys.SUPABASE_USER_DISPLAY_NAME)
                val avatarUrl = session.avatarUrl
                if (avatarUrl != null) preferences[PreferencesKeys.SUPABASE_USER_AVATAR] = avatarUrl else preferences.remove(PreferencesKeys.SUPABASE_USER_AVATAR)
                preferences[PreferencesKeys.SUPABASE_LAST_SYNCED_USER_ID] = session.userId
            } else {
                preferences.remove(PreferencesKeys.SUPABASE_ACCESS_TOKEN)
                preferences.remove(PreferencesKeys.SUPABASE_REFRESH_TOKEN)
                preferences.remove(PreferencesKeys.SUPABASE_EXPIRES_IN)
                preferences.remove(PreferencesKeys.SUPABASE_EXPIRES_AT)
                preferences.remove(PreferencesKeys.SUPABASE_USER_ID)
                preferences.remove(PreferencesKeys.SUPABASE_USER_EMAIL)
                preferences.remove(PreferencesKeys.SUPABASE_USER_DISPLAY_NAME)
                preferences.remove(PreferencesKeys.SUPABASE_USER_AVATAR)
            }
        }
    }

    suspend fun clearSupabaseSession() {
        setSupabaseSession(null)
    }

    val supabaseLastSyncTimestamp: Flow<Long> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_LAST_SYNC_TIMESTAMP] ?: 0L
        }
        .distinctUntilChanged()

    suspend fun setSupabaseLastSyncTimestamp(timestamp: Long) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    val supabaseCloudSyncEnabled: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_CLOUD_SYNC_ENABLED] ?: true
        }
        .distinctUntilChanged()

    suspend fun setSupabaseCloudSyncEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_CLOUD_SYNC_ENABLED] = enabled
        }
    }

    val supabaseLastSyncedUserId: Flow<String?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_LAST_SYNCED_USER_ID]
        }
        .distinctUntilChanged()

    suspend fun setSupabaseLastSyncedUserId(userId: String?) {
        context.preferencesDataStore.edit { preferences ->
            if (userId != null) preferences[PreferencesKeys.SUPABASE_LAST_SYNCED_USER_ID] = userId else preferences.remove(PreferencesKeys.SUPABASE_LAST_SYNCED_USER_ID)
        }
    }

    val supabasePlaybackWatermark: Flow<String?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_PLAYBACK_WATERMARK]
        }
        .distinctUntilChanged()

    suspend fun setSupabasePlaybackWatermark(watermark: String?) {
        context.preferencesDataStore.edit { preferences ->
            if (watermark != null) preferences[PreferencesKeys.SUPABASE_PLAYBACK_WATERMARK] = watermark else preferences.remove(PreferencesKeys.SUPABASE_PLAYBACK_WATERMARK)
        }
    }

    val supabaseWatchlistWatermark: Flow<String?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_WATCHLIST_WATERMARK]
        }
        .distinctUntilChanged()

    suspend fun setSupabaseWatchlistWatermark(watermark: String?) {
        context.preferencesDataStore.edit { preferences ->
            if (watermark != null) preferences[PreferencesKeys.SUPABASE_WATCHLIST_WATERMARK] = watermark else preferences.remove(PreferencesKeys.SUPABASE_WATCHLIST_WATERMARK)
        }
    }

    val supabasePlaylistsWatermark: Flow<String?> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_PLAYLISTS_WATERMARK]
        }
        .distinctUntilChanged()

    suspend fun setSupabasePlaylistsWatermark(watermark: String?) {
        context.preferencesDataStore.edit { preferences ->
            if (watermark != null) preferences[PreferencesKeys.SUPABASE_PLAYLISTS_WATERMARK] = watermark else preferences.remove(PreferencesKeys.SUPABASE_PLAYLISTS_WATERMARK)
        }
    }

    val supabaseSyncPlayback: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_PLAYBACK] ?: true
        }
        .distinctUntilChanged()

    suspend fun setSupabaseSyncPlayback(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_PLAYBACK] = enabled
        }
    }

    val supabaseSyncWatchlist: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_WATCHLIST] ?: true
        }
        .distinctUntilChanged()

    suspend fun setSupabaseSyncWatchlist(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_WATCHLIST] = enabled
        }
    }

    val supabaseSyncPlaylists: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_PLAYLISTS] ?: true
        }
        .distinctUntilChanged()

    suspend fun setSupabaseSyncPlaylists(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_PLAYLISTS] = enabled
        }
    }

    val supabaseSyncSettings: Flow<Boolean> = context.preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_SETTINGS] ?: true
        }
        .distinctUntilChanged()

    suspend fun setSupabaseSyncSettings(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPABASE_SYNC_SETTINGS] = enabled
        }
    }

    suspend fun getAllSyncableSettings(): Map<String, String> {
        val prefs = context.preferencesDataStore.data.first()
        val result = mutableMapOf<String, String>()
        prefs[PreferencesKeys.CARD_SCALE_MODE]?.let { result["card_scale_mode"] = it }
        prefs[PreferencesKeys.NAVBAR_STYLE]?.let { result["navbar_style"] = it }
        prefs[PreferencesKeys.TOP_10_STYLE]?.let { result["top_10_style"] = it }
        prefs[PreferencesKeys.SECTION_LABEL_STYLE]?.let { result["section_label_style"] = it }
        prefs[PreferencesKeys.LONG_PRESS_STYLE]?.let { result["long_press_style"] = it }
        prefs[PreferencesKeys.CONTENT_REGION_PRESET]?.let { result["content_region_preset"] = it }
        prefs[PreferencesKeys.AMOLED_MODE]?.let { result["amoled_mode"] = it.toString() }
        prefs[PreferencesKeys.DEFAULT_QUALITY]?.let { result["default_quality"] = it }
        prefs[PreferencesKeys.AUTO_PLAY_NEXT_EPISODE]?.let { result["auto_play_next_episode"] = it.toString() }
        prefs[PreferencesKeys.SEEK_INTERVAL_SECONDS]?.let { result["seek_interval_seconds"] = it.toString() }
        prefs[PreferencesKeys.AUTO_SKIP_INTRO]?.let { result["auto_skip_intro"] = it.toString() }
        prefs[PreferencesKeys.SKIP_INTRO_DURATION_SECONDS]?.let { result["skip_intro_duration_seconds"] = it.toString() }
        prefs[PreferencesKeys.SKIP_OUTRO_DURATION_SECONDS]?.let { result["skip_outro_duration_seconds"] = it.toString() }
        prefs[PreferencesKeys.COUNTDOWN_NEXT_EPISODE_SECONDS]?.let { result["countdown_next_episode_seconds"] = it.toString() }
        prefs[PreferencesKeys.SUBTITLE_SIZE_SP]?.let { result["subtitle_size_sp"] = it.toString() }
        prefs[PreferencesKeys.SUBTITLE_COLOR]?.let { result["subtitle_color"] = it }
        prefs[PreferencesKeys.SUBTITLE_OUTLINE]?.let { result["subtitle_outline"] = it.toString() }
        prefs[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED]?.let { result["subtitle_background_enabled"] = it.toString() }
        prefs[PreferencesKeys.EXTERNAL_SUBTITLES_ENABLED]?.let { result["external_subtitles_enabled"] = it.toString() }
        prefs[PreferencesKeys.HARDWARE_ACCELERATION_ENABLED]?.let { result["hardware_acceleration_enabled"] = it.toString() }
        prefs[PreferencesKeys.SMART_FAILOVER_ENABLED]?.let { result["smart_failover_enabled"] = it.toString() }
        prefs[PreferencesKeys.AUDIO_PASSTHROUGH_ENABLED]?.let { result["audio_passthrough_enabled"] = it.toString() }
        prefs[PreferencesKeys.WIFI_ONLY_DOWNLOADS]?.let { result["wifi_only_downloads"] = it.toString() }
        prefs[PreferencesKeys.EXTRA_CONTENT_ENABLED]?.let { result["extra_content_enabled"] = it.toString() }
        prefs[PreferencesKeys.REDUCE_MOTION]?.let { result["reduce_motion"] = it.toString() }
        prefs[PreferencesKeys.FAVORITE_MEDIA_IDS]?.let { if (it.isNotBlank()) result["favorite_media_ids"] = it }
        prefs[PreferencesKeys.MEDIA_REACTIONS]?.let { if (it.isNotBlank()) result["media_reactions"] = it }
        prefs[PreferencesKeys.ACTIVE_PROVIDER]?.let { if (it.isNotBlank()) result["active_provider"] = it }
        prefs[PreferencesKeys.REPOSITORY_URLS]?.let { urls ->
            if (urls.isNotEmpty()) result["repository_urls"] = urls.joinToString(",")
        }
        prefs[PreferencesKeys.DISABLED_PLUGINS]?.let { disabled ->
            if (disabled.isNotEmpty()) result["disabled_plugins"] = disabled.joinToString(",")
        }
        return result
    }

    suspend fun applySyncableSettings(settings: Map<String, String>) {
        context.preferencesDataStore.edit { prefs ->
            settings["card_scale_mode"]?.let { prefs[PreferencesKeys.CARD_SCALE_MODE] = it }
            settings["navbar_style"]?.let { prefs[PreferencesKeys.NAVBAR_STYLE] = it }
            settings["top_10_style"]?.let { prefs[PreferencesKeys.TOP_10_STYLE] = it }
            settings["section_label_style"]?.let { prefs[PreferencesKeys.SECTION_LABEL_STYLE] = it }
            settings["long_press_style"]?.let { prefs[PreferencesKeys.LONG_PRESS_STYLE] = it }
            settings["content_region_preset"]?.let { presetId ->
                prefs[PreferencesKeys.CONTENT_REGION_PRESET] = presetId
                com.euthopiar.core.util.TmdbBridge.activeRegionPreset = ContentRegionPreset.fromId(presetId)
            }
            settings["amoled_mode"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.AMOLED_MODE] = it }
            settings["default_quality"]?.let { prefs[PreferencesKeys.DEFAULT_QUALITY] = it }
            settings["auto_play_next_episode"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.AUTO_PLAY_NEXT_EPISODE] = it }
            settings["seek_interval_seconds"]?.toIntOrNull()?.let { prefs[PreferencesKeys.SEEK_INTERVAL_SECONDS] = it }
            settings["auto_skip_intro"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.AUTO_SKIP_INTRO] = it }
            settings["skip_intro_duration_seconds"]?.toIntOrNull()?.let { prefs[PreferencesKeys.SKIP_INTRO_DURATION_SECONDS] = it }
            settings["skip_outro_duration_seconds"]?.toIntOrNull()?.let { prefs[PreferencesKeys.SKIP_OUTRO_DURATION_SECONDS] = it }
            settings["countdown_next_episode_seconds"]?.toIntOrNull()?.let { prefs[PreferencesKeys.COUNTDOWN_NEXT_EPISODE_SECONDS] = it }
            settings["subtitle_size_sp"]?.toIntOrNull()?.let { prefs[PreferencesKeys.SUBTITLE_SIZE_SP] = it }
            settings["subtitle_color"]?.let { prefs[PreferencesKeys.SUBTITLE_COLOR] = it }
            settings["subtitle_outline"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.SUBTITLE_OUTLINE] = it }
            settings["subtitle_background_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED] = it }
            settings["external_subtitles_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.EXTERNAL_SUBTITLES_ENABLED] = it }
            settings["hardware_acceleration_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.HARDWARE_ACCELERATION_ENABLED] = it }
            settings["smart_failover_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.SMART_FAILOVER_ENABLED] = it }
            settings["audio_passthrough_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.AUDIO_PASSTHROUGH_ENABLED] = it }
            settings["wifi_only_downloads"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = it }
            settings["extra_content_enabled"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.EXTRA_CONTENT_ENABLED] = it }
            settings["reduce_motion"]?.toBooleanStrictOrNull()?.let { prefs[PreferencesKeys.REDUCE_MOTION] = it }

            // Merge favorite_media_ids (union of local + remote so favorites are never lost)
            settings["favorite_media_ids"]?.let { remoteFavsRaw ->
                val localFavsRaw = prefs[PreferencesKeys.FAVORITE_MEDIA_IDS] ?: ""
                val mergedFavs = (localFavsRaw.split(",") + remoteFavsRaw.split(","))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                if (mergedFavs.isNotEmpty()) {
                    prefs[PreferencesKeys.FAVORITE_MEDIA_IDS] = mergedFavs.joinToString(",")
                }
            }

            // Merge media_reactions
            settings["media_reactions"]?.let { remoteReactions ->
                if (remoteReactions.isNotBlank()) {
                    val localRaw = prefs[PreferencesKeys.MEDIA_REACTIONS] ?: ""
                    val mergedMap = mutableMapOf<String, String>()
                    localRaw.split(";").forEach { entry ->
                        val parts = entry.split("=")
                        if (parts.size == 2 && parts[0].isNotBlank()) mergedMap[parts[0]] = parts[1]
                    }
                    remoteReactions.split(";").forEach { entry ->
                        val parts = entry.split("=")
                        if (parts.size == 2 && parts[0].isNotBlank()) mergedMap[parts[0]] = parts[1]
                    }
                    prefs[PreferencesKeys.MEDIA_REACTIONS] = mergedMap.entries.joinToString(";") { "${it.key}=${it.value}" }
                }
            }

            settings["active_provider"]?.let { if (it.isNotBlank()) prefs[PreferencesKeys.ACTIVE_PROVIDER] = it }

            settings["repository_urls"]?.let { remoteUrlsCsv ->
                val remoteUrls = remoteUrlsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                if (remoteUrls.isNotEmpty()) {
                    val currentUrls = prefs[PreferencesKeys.REPOSITORY_URLS] ?: emptySet()
                    prefs[PreferencesKeys.REPOSITORY_URLS] = currentUrls + remoteUrls
                }
            }

            settings["disabled_plugins"]?.let { remoteDisabledCsv ->
                val remoteDisabled = remoteDisabledCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                prefs[PreferencesKeys.DISABLED_PLUGINS] = remoteDisabled
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
        }

        fun getMaxSupportedBufferSizeMb(context: Context): Int {
            val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actMan?.getMemoryInfo(memInfo)
            val totalRamGb = memInfo.totalMem / (1024L * 1024L * 1024L)
            return when {
                totalRamGb <= 2 -> 25
                totalRamGb <= 4 -> 50
                else -> 100
            }
        }
    }
}
