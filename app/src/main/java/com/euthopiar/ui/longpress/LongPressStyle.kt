package com.euthopiar.ui.longpress

/**
 * Visual and interactive styles for the media item long-press menu.
 */
enum class LongPressSheetStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    HERO_PEEK(
        id = "hero_peek",
        displayName = "Hero peek",
        description = "Cinematic backdrop header with primary play pill and circular actions"
    ),
    GLASS_MENU(
        id = "glass_menu",
        displayName = "Glass menu",
        description = "Floating poster fly-out with frosted acrylic context menu"
    ),
    TINTED_RESUME(
        id = "tinted_resume",
        displayName = "Tinted resume",
        description = "Dynamic poster palette color bleed with synopsis and resume progress"
    ),
    TRAILER_CARD(
        id = "trailer_card",
        displayName = "Trailer card",
        description = "Inline muted 16:9 trailer player card with quick actions"
    ),
    PILL_ACTIONS(
        id = "pill_actions",
        displayName = "Pill actions",
        description = "Centered hero poster with stacked frosted glass pills"
    ),
    REACTION_LIST(
        id = "reaction_list",
        displayName = "Reaction list",
        description = "Quick taste-rating row (dislike, like, love) with vertical action list"
    );

    companion object {
        fun fromId(id: String?): LongPressSheetStyle =
            entries.firstOrNull { it.id == id } ?: HERO_PEEK
    }
}
