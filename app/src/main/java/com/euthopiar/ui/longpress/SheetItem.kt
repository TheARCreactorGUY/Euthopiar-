package com.euthopiar.ui.longpress

import androidx.compose.ui.geometry.Rect
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity

enum class UserReaction(val id: String) {
    NONE("none"),
    DISLIKE("dislike"),
    LIKE("like"),
    LOVE("love");

    companion object {
        fun fromId(id: String?): UserReaction =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

/**
 * Unified data model passed into all long-press sheet styles.
 */
data class SheetItem(
    val id: String,
    val title: String,
    val year: Int?,
    val type: MediaType,
    val certificate: String? = null,
    val isHd: Boolean = true,
    val rating: String? = null,
    val runtimeMinutes: Int? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val posterUrl: String?,
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val progressPercent: Int = 0,
    val inWatchlist: Boolean = false,
    val reaction: UserReaction = UserReaction.NONE,
    val cardBounds: Rect? = null,
)

/**
 * Factory extension to map app domain MediaItem into SheetItem.
 */
fun MediaItem.toSheetItem(
    history: PlaybackHistoryEntity? = null,
    inWatchlist: Boolean = false,
    reaction: UserReaction = UserReaction.NONE,
    cardBounds: Rect? = null,
    trailerUrl: String? = null
): SheetItem {
    val progressPercent = if (history != null && history.durationMs > 0L) {
        ((history.currentPositionMs.toDouble() / history.durationMs.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val remainingMinutes = if (history != null && history.durationMs > 0L) {
        val remMs = (history.durationMs - history.currentPositionMs).coerceAtLeast(0L)
        (remMs / 60000L).toInt()
    } else null

    return SheetItem(
        id = id,
        title = title,
        year = year,
        type = type,
        certificate = contentRating ?: "16+",
        isHd = quality?.contains("4k", ignoreCase = true) == true || quality?.contains("hd", ignoreCase = true) == true || true,
        rating = rating ?: rottenTomatoesRating,
        runtimeMinutes = remainingMinutes,
        synopsis = null,
        genres = emptyList(),
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        trailerUrl = trailerUrl,
        progressPercent = progressPercent,
        inWatchlist = inWatchlist,
        reaction = reaction,
        cardBounds = cardBounds
    )
}
