package com.euthopiar.ui.details

import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTargetLiveTvTest {

    @Test
    fun resolveActionState_returnsWatchLiveForLiveTv() {
        val history = PlaybackHistoryEntity(
            id = "default_user::default::live:stream1",
            mediaId = "live:stream1",
            title = "Sky News Live",
            episodeTitle = null,
            seasonNumber = null,
            episodeNumber = null,
            posterUrl = null,
            backdropUrl = null,
            mediaType = "LIVE_TV",
            currentPositionMs = 120_000L,
            durationMs = 0L,
            lastWatchedTimestamp = 1000L,
            isCompleted = false
        )

        val actionState = PlaybackTargetHelper.resolveActionState(
            history = history,
            mediaType = MediaType.LIVE_TV
        )

        assertTrue(actionState is PlaybackActionState.Play)
        assertEquals("Watch Live", actionState.label)
    }

    @Test
    fun resolveDetailsPlaybackTarget_returnsSingleLiveTargetWithoutResumePoint() {
        val liveDetail = MediaDetail(
            id = "live:espn",
            title = "ESPN Live",
            url = "stremio_type:channel",
            posterUrl = "https://example.com/espn.png",
            backdropUrl = null,
            type = MediaType.LIVE_TV,
            year = null,
            synopsis = "24/7 Live Sports",
            episodes = listOf(
                EpisodeItem(
                    id = "live:espn:stream",
                    title = "ESPN Live",
                    seasonNumber = 1,
                    episodeNumber = 1,
                    data = "live:espn:stream"
                )
            )
        )

        val (targetEp, resumeHistory) = PlaybackTargetHelper.resolveDetailsPlaybackTarget(
            detail = liveDetail,
            selectedSeason = 1,
            episodePlaybackMap = emptyMap(),
            latestPlayback = null
        )

        assertEquals("live:espn:stream", targetEp?.id)
        assertNull("Live TV channels must never have a resume history point", resumeHistory)
    }
}
