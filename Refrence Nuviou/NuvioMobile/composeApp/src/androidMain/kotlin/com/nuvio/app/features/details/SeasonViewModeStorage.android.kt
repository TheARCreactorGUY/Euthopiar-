package com.nuvio.app.features.details

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object SeasonViewModeStorage {
    private const val preferencesName = "nuvio_season_view_mode"
    private const val key = "season_view_mode"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(): SeasonViewMode? =
        preferences?.getString(ProfileScopedKey.of(key), null)?.let(SeasonViewMode::parse)

    actual fun save(mode: SeasonViewMode) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(key), SeasonViewMode.persist(mode))
            ?.apply()
    }
}
