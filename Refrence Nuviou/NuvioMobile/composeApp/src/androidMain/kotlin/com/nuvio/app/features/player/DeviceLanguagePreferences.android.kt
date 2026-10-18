package com.nuvio.app.features.player

import android.os.Build
import android.os.LocaleList
import java.util.Locale

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> {
        val languages = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList.getDefault()
            for (index in 0 until localeList.size()) {
                val locale = localeList[index] ?: continue
                appendLocaleCodes(languages, locale)
            }
        } else {
            appendLocaleCodes(languages, Locale.getDefault())
        }

        if (languages.isEmpty()) {
            appendLocaleCodes(languages, Locale.ENGLISH)
        }

        return languages
            .mapNotNull(::normalizeLanguageCode)
            .distinct()
    }

    private fun appendLocaleCodes(bucket: MutableList<String>, locale: Locale) {
        bucket += locale.toLanguageTag()
        bucket += locale.language
    }
}
