package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object StreamLinkCacheStorage {
    actual fun loadEntry(hashedKey: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(hashedKey))

    actual fun saveEntry(hashedKey: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(hashedKey))
    }

    actual fun removeEntry(hashedKey: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(hashedKey))
    }
}
