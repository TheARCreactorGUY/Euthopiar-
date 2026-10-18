package com.euthopiar.ui.details

import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity

sealed interface PlaybackActionState {
    val label: String

    data class Play(override val label: String = "Play") : PlaybackActionState

    data class Resume(
        override val label: String,
        val progress: Float,
        val minutesLeft: Int,
        val history: PlaybackHistoryEntity
    ) : PlaybackActionState

    data class Completed(
        override val label: String = "Watch again",
        val history: PlaybackHistoryEntity
    ) : PlaybackActionState
}

object PlaybackTargetHelper {

    /**
     * Resolves the primary action state for a MediaItem or MediaDetail given its playback history.
     */
    fun resolveActionState(
        history: PlaybackHistoryEntity?,
        mediaType: MediaType? = null
    ): PlaybackActionState {
        if (history == null) {
            return PlaybackActionState.Play("Play")
        }
        if (history.isCompleted) {
            return PlaybackActionState.Completed("Watch again", history)
        }
        if (history.currentPositionMs > 5000L) {
            val isTv = mediaType == MediaType.TV_SERIES ||
                mediaType == MediaType.ANIME ||
                history.mediaType == "TV_SERIES" ||
                history.mediaType == "ANIME" ||
                (history.seasonNumber != null && history.seasonNumber > 0)

            val sNum = history.seasonNumber
            val eNum = history.episodeNumber
            val label = if (isTv && sNum != null && eNum != null && sNum > 0 && eNum > 0) {
                "Resume S$sNum E$eNum"
            } else {
                "Resume"
            }

            val progress = if (history.durationMs > 0) {
                (history.currentPositionMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val remainingMs = (history.durationMs - history.currentPositionMs).coerceAtLeast(0L)
            val minutesLeft = (remainingMs / 60000L).toInt().coerceAtLeast(1)

            return PlaybackActionState.Resume(
                label = label,
                progress = progress,
                minutesLeft = minutesLeft,
                history = history
            )
        }
        return PlaybackActionState.Play("Play")
    }

    /**
     * Target episode and resume point calculation for MediaDetailsScreen.
     */
    fun resolveDetailsPlaybackTarget(
        detail: MediaDetail,
        selectedSeason: Int,
        episodePlaybackMap: Map<Pair<Int, Int>, PlaybackHistoryEntity>,
        latestPlayback: PlaybackHistoryEntity?
    ): Pair<EpisodeItem?, PlaybackHistoryEntity?> {
        val isTv = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME
        return if (isTv) {
            val seasonEps = detail.episodes
                .filter { it.seasonNumber == selectedSeason }
                .sortedBy { it.episodeNumber }

            val inProgressEp = seasonEps.firstOrNull { ep ->
                val p = episodePlaybackMap[ep.seasonNumber to ep.episodeNumber]
                p != null && p.currentPositionMs > 5000L && !p.isCompleted
            }

            if (inProgressEp != null) {
                inProgressEp to episodePlaybackMap[inProgressEp.seasonNumber to inProgressEp.episodeNumber]
            } else {
                val nextUncompleted = seasonEps.firstOrNull { ep ->
                    val p = episodePlaybackMap[ep.seasonNumber to ep.episodeNumber]
                    p == null || !p.isCompleted
                } ?: seasonEps.firstOrNull()

                val pb = nextUncompleted?.let { episodePlaybackMap[it.seasonNumber to it.episodeNumber] }
                val activePb = if (pb != null && pb.currentPositionMs > 5000L && !pb.isCompleted) pb else null
                (nextUncompleted ?: detail.episodes.firstOrNull()) to activePb
            }
        } else {
            val curLatest = latestPlayback
            val pb = if (curLatest != null && curLatest.currentPositionMs > 5000L && !curLatest.isCompleted) curLatest else null
            detail.episodes.firstOrNull() to pb
        }
    }
}
