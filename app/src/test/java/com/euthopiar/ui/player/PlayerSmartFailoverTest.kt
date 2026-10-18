package com.euthopiar.ui.player

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.repository.ProviderRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerSmartFailoverTest {

    private lateinit var application: Application
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        com.euthopiar.core.network.BandwidthEstimator.Default.setEstimatedBandwidthForTesting(20_000_000L)
        viewModel = PlayerViewModel(application)
    }

    @org.junit.After
    fun tearDown() {
        com.euthopiar.core.network.BandwidthEstimator.Default.resetForTesting()
    }

    @Test
    fun testSmartFailoverInitialStateAndToggle() {
        val state = viewModel.uiState.value
        assertTrue("Smart Failover should default to enabled", state.smartFailoverEnabled)

        viewModel.setSmartFailoverEnabled(false)
        assertFalse("Smart Failover should reflect disabled state", viewModel.uiState.value.smartFailoverEnabled)

        viewModel.setSmartFailoverEnabled(true)
        assertTrue("Smart Failover should reflect re-enabled state", viewModel.uiState.value.smartFailoverEnabled)
    }

    @Test
    fun testSelectServerUserExplicitFlag() {
        val server1 = ServerSourceOption(
            id = "server_1",
            label = "Server 1",
            serverName = "Alpha",
            resolutionLabel = "1080p",
            streamSource = StreamSource(url = "https://example.com/s1", quality = "1080p")
        )
        val server2 = ServerSourceOption(
            id = "server_2",
            label = "Server 2",
            serverName = "Beta",
            resolutionLabel = "720p",
            streamSource = StreamSource(url = "https://example.com/s2", quality = "720p")
        )

        // Manual user selection sets explicit selection
        viewModel.selectServer(server1, isUserExplicit = true)
        assertEquals("Alpha", viewModel.uiState.value.selectedServer?.serverName)

        // Automatic fallback sets isUserExplicit = false
        viewModel.selectServer(server2, isUserExplicit = false)
        assertEquals("Beta", viewModel.uiState.value.selectedServer?.serverName)
    }

    @Test
    fun testPrePlayQualityConflictAlertOn4kStreamSelection() {
        val server4k = ServerSourceOption(
            id = "server_4k",
            label = "Server 4K",
            serverName = "UltraHD",
            resolutionLabel = "4K 2160p",
            streamSource = StreamSource(url = "https://example.com/4k.mkv", quality = "4K")
        )

        // When user explicitly selects 4K requiring 22 Mbps on 12 Mbps connection, pre-play conflict alert is displayed
        com.euthopiar.core.network.BandwidthEstimator.Default.setEstimatedBandwidthForTesting(12_000_000L)
        viewModel.selectServer(server4k, isUserExplicit = true)
        val conflict = viewModel.uiState.value.qualityConflictNotice
        assertNotNull("Quality conflict alert must be raised for 4K on default bandwidth prior", conflict)
        assertEquals("4K", conflict?.requestedQuality)
        assertEquals("1080p", conflict?.recommendedQuality)

        // User chooses to Continue Anyway -> warning is dismissed and tier is suppressed for session
        viewModel.continueAnywayWithConflictQuality()
        assertEquals(null, viewModel.uiState.value.qualityConflictNotice)
        assertTrue(com.euthopiar.core.network.BandwidthEstimator.Default.isTierSuppressed("4K"))

        // Selecting 4K again in same session should NOT trigger alert due to anti-fatigue suppression
        viewModel.selectServer(server4k, isUserExplicit = true)
        assertEquals(null, viewModel.uiState.value.qualityConflictNotice)

        // Clean up
        com.euthopiar.core.network.BandwidthEstimator.Default.clearSuppressedTiers()
    }

    @Test
    fun testCanonicalStreamSnapshotHoldsResolvedStream() {
        val server = ServerSourceOption(
            id = "server_direct",
            label = "Server Direct",
            serverName = "DirectServer",
            resolutionLabel = "1080p",
            streamSource = StreamSource(
                url = "https://cdn.example.com/video.mp4",
                quality = "1080p",
                headers = mapOf("Authorization" to "Bearer token123", "User-Agent" to "CustomPlayer")
            )
        )
        viewModel.selectServer(server, isUserExplicit = true)

        val canonical = viewModel.getCurrentCanonicalStream()
        assertNotNull("CanonicalStream must be created upon stream start", canonical)
        assertEquals("https://cdn.example.com/video.mp4", canonical?.uri)
        assertEquals("Bearer token123", canonical?.headers?.get("Authorization"))
        assertEquals("CustomPlayer", canonical?.headers?.get("User-Agent"))
    }
}
