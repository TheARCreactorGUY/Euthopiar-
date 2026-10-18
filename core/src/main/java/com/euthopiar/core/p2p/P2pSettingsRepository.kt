package com.euthopiar.core.p2p

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object P2pSettingsRepository {
    private const val PREFS_NAME = "p2p_settings"
    private const val KEY_P2P_ENABLED = "p2p_enabled"
    private const val KEY_ENABLE_UPLOAD = "enable_upload"
    private const val KEY_HIDE_TORRENT_STATS = "hide_torrent_stats"
    private const val KEY_TORRENT_PROFILE = "torrent_profile"
    private const val KEY_CACHE_SIZE = "cache_size"

    private val _uiState = MutableStateFlow(P2pSettingsUiState())
    val uiState: StateFlow<P2pSettingsUiState> = _uiState.asStateFlow()

    private var preferences: SharedPreferences? = null
    private var hasLoaded = false

    private var p2pEnabled = true
    private var enableUpload = true
    private var hideTorrentStats = false
    private var torrentProfile = P2pTorrentProfile.BALANCED
    private var cacheSize = P2pCacheSize.GB_2

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun setP2pEnabled(enabled: Boolean) {
        ensureLoaded()
        if (p2pEnabled == enabled) return
        p2pEnabled = enabled
        preferences?.edit()?.putBoolean(KEY_P2P_ENABLED, enabled)?.apply()
        publish()
    }

    fun setEnableUpload(enabled: Boolean) {
        ensureLoaded()
        if (enableUpload == enabled) return
        enableUpload = enabled
        preferences?.edit()?.putBoolean(KEY_ENABLE_UPLOAD, enabled)?.apply()
        publish()
    }

    fun setHideTorrentStats(enabled: Boolean) {
        ensureLoaded()
        if (hideTorrentStats == enabled) return
        hideTorrentStats = enabled
        preferences?.edit()?.putBoolean(KEY_HIDE_TORRENT_STATS, enabled)?.apply()
        publish()
    }

    fun setTorrentProfile(profile: P2pTorrentProfile) {
        ensureLoaded()
        if (torrentProfile == profile) return
        torrentProfile = profile
        preferences?.edit()?.putString(KEY_TORRENT_PROFILE, profile.name)?.apply()
        publish()
    }

    fun setCacheSize(size: P2pCacheSize) {
        ensureLoaded()
        if (cacheSize == size) return
        cacheSize = size
        preferences?.edit()?.putString(KEY_CACHE_SIZE, size.name)?.apply()
        publish()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val prefs = preferences
        p2pEnabled = prefs?.getBoolean(KEY_P2P_ENABLED, true) ?: true
        enableUpload = prefs?.getBoolean(KEY_ENABLE_UPLOAD, true) ?: true
        hideTorrentStats = prefs?.getBoolean(KEY_HIDE_TORRENT_STATS, false) ?: false
        torrentProfile = prefs?.getString(KEY_TORRENT_PROFILE, null)
            ?.let { stored -> P2pTorrentProfile.entries.firstOrNull { it.name == stored } }
            ?: P2pTorrentProfile.BALANCED
        cacheSize = prefs?.getString(KEY_CACHE_SIZE, null)
            ?.let { stored -> P2pCacheSize.entries.firstOrNull { it.name == stored } }
            ?: P2pCacheSize.GB_2
        publish()
    }

    private fun publish() {
        _uiState.value = P2pSettingsUiState(
            p2pEnabled = p2pEnabled,
            enableUpload = enableUpload,
            hideTorrentStats = hideTorrentStats,
            torrentProfile = torrentProfile,
            cacheSize = cacheSize,
        )
    }
}
