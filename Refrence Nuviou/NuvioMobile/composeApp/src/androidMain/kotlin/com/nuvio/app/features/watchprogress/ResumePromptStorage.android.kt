package com.nuvio.app.features.watchprogress

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object ResumePromptStorage {
    private const val preferencesName = "nuvio_resume_prompt"
    private const val wasInPlayerKey = "was_in_player"
    private const val lastPlayerVideoIdKey = "last_player_video_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadWasInPlayer(): Boolean =
        preferences?.getBoolean(ProfileScopedKey.of(wasInPlayerKey), false) ?: false

    actual fun saveWasInPlayer(value: Boolean) {
        preferences?.edit()?.putBoolean(ProfileScopedKey.of(wasInPlayerKey), value)?.apply()
    }

    actual fun loadLastPlayerVideoId(): String? =
        preferences?.getString(ProfileScopedKey.of(lastPlayerVideoIdKey), null)

    actual fun saveLastPlayerVideoId(videoId: String?) {
        preferences?.edit()?.apply {
            if (videoId != null) {
                putString(ProfileScopedKey.of(lastPlayerVideoIdKey), videoId)
            } else {
                remove(ProfileScopedKey.of(lastPlayerVideoIdKey))
            }
            apply()
        }
    }
}
