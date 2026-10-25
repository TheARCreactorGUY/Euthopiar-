package com.euthopiar.ui.player

import android.app.Application
import androidx.media3.common.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AdaptiveStreamQualitySelectionTest {

    private lateinit var application: Application
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        viewModel = PlayerViewModel(application)
    }

    @Test
    fun testResolveTrackHeight_fromExplicitHeight() {
        val format = Format.Builder()
            .setHeight(1080)
            .setWidth(1920)
            .build()
        val resolved = viewModel.resolveTrackHeight(format, 0)
        assertEquals(1080, resolved)
    }

    @Test
    fun testResolveTrackHeight_fromStandardWidthsWhenHeightMissing() {
        val w4k = Format.Builder().setWidth(3840).build()
        assertEquals(2160, viewModel.resolveTrackHeight(w4k, 0))

        val w2k = Format.Builder().setWidth(2560).build()
        assertEquals(1440, viewModel.resolveTrackHeight(w2k, 0))

        val wFhd = Format.Builder().setWidth(1920).build()
        assertEquals(1080, viewModel.resolveTrackHeight(wFhd, 0))

        val wHd = Format.Builder().setWidth(1280).build()
        assertEquals(720, viewModel.resolveTrackHeight(wHd, 0))

        val wSd = Format.Builder().setWidth(854).build()
        assertEquals(480, viewModel.resolveTrackHeight(wSd, 0))
    }

    @Test
    fun testResolveTrackHeight_fromLabelOrId() {
        val formatWithLabel = Format.Builder()
            .setLabel("1080p High Bitrate")
            .build()
        assertEquals(1080, viewModel.resolveTrackHeight(formatWithLabel, 0))

        val formatWithId = Format.Builder()
            .setId("stream_720p")
            .build()
        assertEquals(720, viewModel.resolveTrackHeight(formatWithId, 0))
    }

    @Test
    fun testSelectServer_resetsSelectedQualityToAuto() {
        val targetQuality = VideoQualityOption(
            id = "video_0_2",
            label = "1080p (5.0 Mbps)",
            height = 1080,
            bitrate = 5_000_000,
            groupIndex = 0,
            trackIndex = 2
        )
        viewModel.selectQuality(targetQuality)
        assertEquals("video_0_2", viewModel.uiState.value.selectedQuality.id)

        val newServer = ServerSourceOption(
            id = "srv_2",
            serverName = "Server 2",
            resolutionLabel = "1080p",
            streamSource = com.euthopiar.core.model.StreamSource(url = "https://cdn.example.com/stream2.m3u8")
        )
        viewModel.selectServer(newServer, isUserExplicit = true)

        // Verifies selectedQuality was reset to Auto on server switch
        assertTrue(viewModel.uiState.value.selectedQuality.isAuto)
        assertEquals("auto", viewModel.uiState.value.selectedQuality.id)
    }
}
