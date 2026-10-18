package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object ResumePromptStorage {
    private const val wasInPlayerKey = "nuvio_resume_prompt_was_in_player"
    private const val lastPlayerVideoIdKey = "nuvio_resume_prompt_last_player_video_id"

    actual fun loadWasInPlayer(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(ProfileScopedKey.of(wasInPlayerKey))

    actual fun saveWasInPlayer(value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = ProfileScopedKey.of(wasInPlayerKey))
    }

    actual fun loadLastPlayerVideoId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(lastPlayerVideoIdKey))

    actual fun saveLastPlayerVideoId(videoId: String?) {
        if (videoId != null) {
            NSUserDefaults.standardUserDefaults.setObject(videoId, forKey = ProfileScopedKey.of(lastPlayerVideoIdKey))
        } else {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(lastPlayerVideoIdKey))
        }
    }
}
