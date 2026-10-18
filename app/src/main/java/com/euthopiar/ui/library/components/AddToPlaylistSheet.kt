package com.euthopiar.ui.library.components

import androidx.compose.runtime.Composable
import com.euthopiar.ui.details.sheets.MediaDetailsActionSheet
import com.euthopiar.ui.details.sheets.SheetScreen

/**
 * Bottom sheet allowing users to add movies and shows to their custom playlists,
 * or create a new playlist on the fly.
 * Delegates to the unified frosted glass MediaDetailsActionSheet.
 */
@Composable
fun AddToPlaylistSheet(
    mediaId: String,
    title: String,
    posterUrl: String? = null,
    backdropUrl: String? = null,
    isTvSeries: Boolean = false,
    year: Int? = null,
    durationMinutes: Int? = null,
    provider: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    episodeData: String? = null,
    onDismiss: () -> Unit,
    onPlaylistAction: ((message: String) -> Unit)? = null
) {
    MediaDetailsActionSheet(
        mediaId = mediaId,
        title = title,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        isTvSeries = isTvSeries,
        year = year,
        provider = provider,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        episodeData = episodeData,
        initialScreen = SheetScreen.ADD_TO_PLAYLIST,
        onPlaylistAction = onPlaylistAction,
        onDismiss = onDismiss
    )
}
