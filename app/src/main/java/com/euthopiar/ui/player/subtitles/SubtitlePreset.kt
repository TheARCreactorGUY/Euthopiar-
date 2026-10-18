package com.euthopiar.ui.player.subtitles

/**
 * Subtitle styling presets conforming to the Euthopiar UX Guide.
 * Presets: Default (18sp, White, outline), Large (22sp, White, outline), High contrast (22sp, Yellow, background, outline).
 */
enum class SubtitlePreset(
    val id: String,
    val label: String,
    val sizeSp: Int,
    val color: String,
    val backgroundEnabled: Boolean,
    val outline: Boolean
) {
    DEFAULT("default", "Default", 18, "WHITE", false, true),
    LARGE("large", "Large", 22, "WHITE", false, true),
    HIGH_CONTRAST("high_contrast", "High contrast", 22, "YELLOW", true, true);

    companion object {
        fun matchingPreset(sizeSp: Int, color: String, backgroundEnabled: Boolean, outline: Boolean): SubtitlePreset? {
            return entries.firstOrNull {
                it.sizeSp == sizeSp &&
                it.color.equals(color, ignoreCase = true) &&
                it.backgroundEnabled == backgroundEnabled &&
                it.outline == outline
            }
        }
    }
}
