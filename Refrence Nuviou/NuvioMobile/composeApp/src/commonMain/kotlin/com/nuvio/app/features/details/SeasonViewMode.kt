package com.nuvio.app.features.details

enum class SeasonViewMode {
    Posters,
    Text,
    ;

    fun toggled(): SeasonViewMode = when (this) {
        Posters -> Text
        Text -> Posters
    }

    companion object {
        fun parse(raw: String?): SeasonViewMode? = when (raw?.lowercase()) {
            "posters" -> Posters
            "text" -> Text
            else -> null
        }

        fun persist(mode: SeasonViewMode): String = when (mode) {
            Posters -> "posters"
            Text -> "text"
        }
    }
}

internal expect object SeasonViewModeStorage {
    fun load(): SeasonViewMode?
    fun save(mode: SeasonViewMode)
}
