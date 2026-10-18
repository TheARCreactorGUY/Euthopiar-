package com.nuvio.app.features.details

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object SeasonViewModeStorage {
    private const val key = "season_view_mode"

    actual fun load(): SeasonViewMode? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(key))
            ?.let(SeasonViewMode::parse)

    actual fun save(mode: SeasonViewMode) {
        NSUserDefaults.standardUserDefaults.setObject(
            SeasonViewMode.persist(mode),
            forKey = ProfileScopedKey.of(key),
        )
    }
}
