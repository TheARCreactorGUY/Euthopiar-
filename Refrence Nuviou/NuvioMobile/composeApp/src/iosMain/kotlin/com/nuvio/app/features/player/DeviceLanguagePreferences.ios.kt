package com.nuvio.app.features.player

import platform.Foundation.NSUserDefaults

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> {
        val languages = mutableListOf<String>()

        val preferred = NSUserDefaults.standardUserDefaults
            .objectForKey("AppleLanguages") as? List<*>

        preferred.orEmpty().forEach { value ->
            val code = value as? String ?: return@forEach
            languages += code
            languages += code.substringBefore('-')
        }

        if (languages.isEmpty()) {
            languages += "en"
        }

        return languages
            .mapNotNull(::normalizeLanguageCode)
            .distinct()
    }
}
