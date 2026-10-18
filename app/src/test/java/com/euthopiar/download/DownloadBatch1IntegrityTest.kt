package com.euthopiar.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.core.model.DownloadOption
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.network.PlayerPlaybackNetworking
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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
import java.security.cert.X509Certificate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadBatch1IntegrityTest {

    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferencesRepository = PreferencesRepository.getInstance(context)
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Test
    fun testJobServiceRequiredNetworkTypeConfiguration() {
        val scheduler = context.getSystemService(JobScheduler::class.java)

        // Schedule with wifiOnly = true
        EuthopiarDownloadJobService.scheduleUidtJob(
            context = context,
            downloadId = "test_wifi_only_item",
            totalBytes = 5000000L,
            wifiOnly = true
        )

        val wifiJobId = EuthopiarDownloadJobService.JOB_ID_BASE + ("test_wifi_only_item".hashCode() and 0x7FFF)
        val wifiJob = scheduler.getPendingJob(wifiJobId)
        assertNotNull("Job should be scheduled in JobScheduler", wifiJob)
        assertEquals("Network type must be UNMETERED for wifiOnly", JobInfo.NETWORK_TYPE_UNMETERED, wifiJob?.networkType)

        // Schedule with wifiOnly = false
        EuthopiarDownloadJobService.scheduleUidtJob(
            context = context,
            downloadId = "test_any_network_item",
            totalBytes = 5000000L,
            wifiOnly = false
        )

        val anyJobId = EuthopiarDownloadJobService.JOB_ID_BASE + ("test_any_network_item".hashCode() and 0x7FFF)
        val anyJob = scheduler.getPendingJob(anyJobId)
        assertNotNull("Job should be scheduled in JobScheduler", anyJob)
        assertEquals("Network type must be ANY for regular download", JobInfo.NETWORK_TYPE_ANY, anyJob?.networkType)
    }

    @Test
    fun testPermissiveDownloadSslPreservedForThirdPartyCdns() {
        val engine = DownloadEngine(context)
        val clientField = DownloadEngine::class.java.getDeclaredField("client").apply {
            isAccessible = true
        }
        val client = clientField.get(engine) as OkHttpClient

        // Permissive HostnameVerifier must return true for any third-party CDN or scraper hostname
        val hostnameVerifier = client.hostnameVerifier
        assertTrue(
            "Hostname verifier must accept arbitrary scraper/CDN hosts",
            hostnameVerifier.verify("cdn.unverified-media-stream.cc", null)
        )

        // Trust manager must be permissive (PlayerPlaybackNetworking.trustAllManager)
        val trustManager = client.x509TrustManager
        assertNotNull(trustManager)
        // TrustManager should accept without throwing SSLHandshakeException
        trustManager?.checkServerTrusted(emptyArray<X509Certificate>(), "RSA")
    }

    @Test
    fun testDownloadRepositoryWifiOnlyEnforcement() = runBlocking {
        val repository = DownloadRepository.getInstance(context)

        // Set wifi-only to true
        preferencesRepository.setWifiOnlyDownloads(true)
        assertTrue(preferencesRepository.wifiOnlyDownloads.first())

        // In Robolectric test environment with no active unmetered Wi-Fi, isNetworkMeteredOrCellular should return true
        val isMetered = repository.isNetworkMeteredOrCellular()
        assertTrue("Without unmetered Wi-Fi active, should report metered or non-wifi", isMetered)

        val sampleDetail = MediaDetail(
            id = "test_media_wifi_1",
            title = "Sample Wi-Fi Movie",
            url = "https://example.com/movie/test_media_wifi_1",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            type = MediaType.MOVIE,
            year = 2024,
            synopsis = "A test movie"
        )
        val sampleOption = DownloadOption(
            title = "Sample Wi-Fi Movie",
            quality = "1080p",
            size = "1.5 GB",
            url = "https://example.com/stream.mp4",
            source = "Direct",
            provider = "test"
        )

        repository.startDownload(sampleDetail, null, sampleOption)

        val item = repository.downloads.value.firstOrNull { it.id == "test_media_wifi_1" }
        assertNotNull("Item should be enqueued in downloads", item)
        assertEquals("Item must be paused when Wi-Fi only is enforced on metered network", DownloadState.PAUSED, item?.state)
        assertEquals("Failure reason must state 'Waiting for Wi-Fi'", "Waiting for Wi-Fi", item?.failureReason)
        assertFalse("Engine should not be actively downloading", repository.isDownloading("test_media_wifi_1"))

        // Cleanup
        repository.removeDownload("test_media_wifi_1")
        preferencesRepository.setWifiOnlyDownloads(false)
    }

    @Test
    fun testManualPauseClearsFailureReason() = runBlocking {
        val repository = DownloadRepository.getInstance(context)

        preferencesRepository.setWifiOnlyDownloads(true)

        val sampleDetail = MediaDetail(
            id = "test_media_manual_pause",
            title = "Manual Pause Movie",
            url = "https://example.com/movie/test_media_manual_pause",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            type = MediaType.MOVIE,
            year = 2024,
            synopsis = "A test movie"
        )
        val sampleOption = DownloadOption(
            title = "Manual Pause Movie",
            quality = "1080p",
            size = "1.5 GB",
            url = "https://example.com/stream.mp4",
            source = "Direct",
            provider = "test"
        )

        repository.startDownload(sampleDetail, null, sampleOption)

        var item = repository.downloads.value.firstOrNull { it.id == "test_media_manual_pause" }
        assertEquals("Waiting for Wi-Fi", item?.failureReason)

        // Manually pause the download
        repository.pauseDownload("test_media_manual_pause")
        item = repository.downloads.value.firstOrNull { it.id == "test_media_manual_pause" }
        assertEquals(DownloadState.PAUSED, item?.state)
        assertEquals("Manual pause must clear failureReason so it is not treated as waiting for Wi-Fi", null, item?.failureReason)

        // Cleanup
        repository.removeDownload("test_media_manual_pause")
        preferencesRepository.setWifiOnlyDownloads(false)
    }

    @Test
    fun testDisablingWifiOnlyResumesWaitingDownloads() = runBlocking {
        val repository = DownloadRepository.getInstance(context)

        // Start with wifiOnly = true
        preferencesRepository.setWifiOnlyDownloads(true)

        val sampleDetail = MediaDetail(
            id = "test_media_resume_flow",
            title = "Resume Flow Movie",
            url = "https://example.com/movie/test_media_resume_flow",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            type = MediaType.MOVIE,
            year = 2024,
            synopsis = "A test movie"
        )
        val sampleOption = DownloadOption(
            title = "Resume Flow Movie",
            quality = "1080p",
            size = "1.5 GB",
            url = "https://example.com/stream.mp4",
            source = "Direct",
            provider = "test"
        )

        repository.startDownload(sampleDetail, null, sampleOption)

        var item = repository.downloads.value.firstOrNull { it.id == "test_media_resume_flow" }
        assertEquals("Waiting for Wi-Fi", item?.failureReason)

        // Disabling wifi-only downloads triggers auto-resume
        preferencesRepository.setWifiOnlyDownloads(false)
        kotlinx.coroutines.delay(300)

        item = repository.downloads.value.firstOrNull { it.id == "test_media_resume_flow" }
        // When resumed, it leaves PAUSED waiting state
        assertNotEquals("Failure reason 'Waiting for Wi-Fi' must be cleared upon resume", "Waiting for Wi-Fi", item?.failureReason)

        // Cleanup
        repository.removeDownload("test_media_resume_flow")
    }

    @Test
    fun testIsMediaDownloadedRequiresLocalFileOnDisk() = runBlocking {
        val repository = DownloadRepository.getInstance(context)

        val nonExistentPath = java.io.File(context.cacheDir, "non_existent_movie.mp4").absolutePath
        val sampleDetail = MediaDetail(
            id = "test_media_missing_file",
            title = "Missing File Movie",
            url = "https://example.com/movie/test_media_missing_file",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            type = MediaType.MOVIE,
            year = 2024,
            synopsis = "A test movie"
        )
        val sampleOption = DownloadOption(
            title = "Missing File Movie",
            quality = "1080p",
            size = "1.5 GB",
            url = "https://example.com/stream.mp4",
            source = "Direct",
            provider = "test"
        )

        repository.startDownload(sampleDetail, null, sampleOption)

        // Force item state to COMPLETED with non-existent file
        val downloadsField = DownloadRepository::class.java.getDeclaredField("_downloads").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val flow = downloadsField.get(repository) as kotlinx.coroutines.flow.MutableStateFlow<List<DownloadItem>>
        flow.value = flow.value.map {
            if (it.id == "test_media_missing_file") {
                it.copy(state = DownloadState.COMPLETED, localFilePath = nonExistentPath)
            } else it
        }

        // Even though state is COMPLETED, because the file is missing on disk, isMediaDownloaded must return false
        assertFalse(
            "isMediaDownloaded must return false when localFilePath does not exist on disk",
            repository.isMediaDownloaded("test_media_missing_file", null)
        )
        assertEquals(
            "getDownloadedItem must return null when file does not exist on disk",
            null,
            repository.getDownloadedItem("test_media_missing_file", null)
        )

        // Cleanup
        repository.removeDownload("test_media_missing_file")
    }

    @Test
    fun testSanitizeHeadersStripsRangeAndEmptyEntries() {
        val inputHeaders = mapOf(
            "User-Agent" to " CustomAgent/1.0 ",
            "Range" to "bytes=0-1024",
            "range" to "bytes=500-",
            " " to "blankKey",
            "EmptyValue" to "  ",
            "Authorization" to "Bearer abc123xyz"
        )

        val sanitized = PlayerPlaybackNetworking.sanitizeHeaders(inputHeaders)

        assertEquals("CustomAgent/1.0", sanitized["User-Agent"])
        assertEquals("Bearer abc123xyz", sanitized["Authorization"])
        assertFalse("Must strip Range header to prevent ExoPlayer conflicts", sanitized.containsKey("Range"))
        assertFalse("Must strip case-insensitive range header", sanitized.containsKey("range"))
        assertFalse("Must strip empty values", sanitized.containsKey("EmptyValue"))
        assertEquals(2, sanitized.size)
    }

    @Test
    fun testDownloadEngineApplyHeadersStripsAuthorizationOnPresignedUrls() {
        val engine = DownloadEngine(context)
        val applyHeadersMethod = DownloadEngine::class.java.getDeclaredMethod(
            "applyHeaders",
            okhttp3.Request.Builder::class.java,
            String::class.java,
            Map::class.java
        ).apply { isAccessible = true }

        val testHeaders = mapOf(
            "Authorization" to "Bearer secret_api_key",
            "Referer" to "https://forbidden-referer.com",
            "Custom-Track" to "valid_val"
        )

        // Presigned S3 URL
        val s3Builder = okhttp3.Request.Builder().url("https://my-bucket.s3.amazonaws.com/video.mp4?X-Amz-Signature=sig123")
        applyHeadersMethod.invoke(engine, s3Builder, "https://my-bucket.s3.amazonaws.com/video.mp4?X-Amz-Signature=sig123", testHeaders)
        val s3Req = s3Builder.build()
        assertFalse("Authorization must be stripped on S3 presigned URLs", s3Req.headers.names().contains("Authorization"))
        assertFalse("Referer must be stripped on S3 presigned URLs", s3Req.headers.names().contains("Referer"))
        assertEquals("valid_val", s3Req.header("Custom-Track"))

        // Presigned R2 URL
        val r2Builder = okhttp3.Request.Builder().url("https://pub-id.r2.cloudflarestorage.com/file.mkv")
        applyHeadersMethod.invoke(engine, r2Builder, "https://pub-id.r2.cloudflarestorage.com/file.mkv", testHeaders)
        val r2Req = r2Builder.build()
        assertFalse("Authorization must be stripped on R2 URLs", r2Req.headers.names().contains("Authorization"))
        assertFalse("Referer must be stripped on R2 URLs", r2Req.headers.names().contains("Referer"))
        assertEquals("valid_val", r2Req.header("Custom-Track"))

        // Regular CDN URL: headers must be preserved
        val regularBuilder = okhttp3.Request.Builder().url("https://cdn.example.com/video.mp4")
        applyHeadersMethod.invoke(engine, regularBuilder, "https://cdn.example.com/video.mp4", testHeaders)
        val regularReq = regularBuilder.build()
        assertEquals("Bearer secret_api_key", regularReq.header("Authorization"))
        assertEquals("valid_val", regularReq.header("Custom-Track"))
    }

    @Test
    fun testResumeDownloadFromJobServiceDoesNotRescheduleUidtJob() = runBlocking {
        val repository = DownloadRepository.getInstance(context)
        val scheduler = context.getSystemService(JobScheduler::class.java)

        val sampleDetail = MediaDetail(
            id = "test_job_loop_id",
            title = "Job Loop Test",
            url = "https://example.com/movie/test_job_loop_id",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            type = MediaType.MOVIE,
            year = 2024,
            synopsis = "A test movie"
        )
        val sampleOption = DownloadOption(
            title = "Job Loop Test",
            quality = "1080p",
            size = "1.5 GB",
            url = "https://example.com/stream.mp4",
            source = "Direct",
            provider = "test"
        )

        repository.startDownload(sampleDetail, null, sampleOption)
        val jobId = EuthopiarDownloadJobService.JOB_ID_BASE + ("test_job_loop_id".hashCode() and 0x7FFF)

        // Cancel job in scheduler to simulate running state or test resumption
        scheduler.cancel(jobId)
        assertEquals(null, scheduler.getPendingJob(jobId))

        // When resuming from job service, fromJobService = true must NOT re-schedule the UIDT job
        repository.resumeDownload("test_job_loop_id", fromJobService = true)
        assertEquals(
            "UIDT job must not be re-scheduled when triggered by onStartJob (fromJobService = true)",
            null,
            scheduler.getPendingJob(jobId)
        )

        // Cleanup
        repository.removeDownload("test_job_loop_id")
    }

    @Test
    fun testDownloadRepositoryEpisodeMatching() {
        val repository = DownloadRepository.getInstance(context)

        val tvItem = DownloadItem(
            id = "show123_s2e5",
            mediaId = "show123",
            title = "Test Show",
            subtitle = "Episode 5",
            isTvSeries = true,
            seasonNumber = 2,
            episodeNumber = 5,
            episodeId = "ep_unique_99",
            streamUrl = "https://example.com/stream.mp4",
            qualityLabel = "1080p",
            state = DownloadState.COMPLETED
        )

        val movieItem = DownloadItem(
            id = "movie456",
            mediaId = "movie456",
            title = "Test Movie",
            isTvSeries = false,
            streamUrl = "https://example.com/movie.mp4",
            qualityLabel = "4K",
            state = DownloadState.COMPLETED
        )

        // Movie matching
        assertTrue(repository.matchesItem(movieItem, "movie456", null))
        assertFalse(repository.matchesItem(movieItem, "movie999", null))
        assertFalse(repository.matchesItem(movieItem, "movie456", "ep1"))

        // TV matching via colon format "show123:2:5"
        assertTrue(repository.matchesItem(tvItem, "show123", "show123:2:5"))
        // TV matching via short colon format "2:5"
        assertTrue(repository.matchesItem(tvItem, "show123", "2:5"))
        // TV matching via regex "s2e5"
        assertTrue(repository.matchesItem(tvItem, "show123", "s2e5"))
        // TV matching via exact episodeId "ep_unique_99"
        assertTrue(repository.matchesItem(tvItem, "show123", "ep_unique_99"))
        // TV matching via item.id "show123_s2e5"
        assertTrue(repository.matchesItem(tvItem, "show123", "show123_s2e5"))

        // Mismatched episode numbers
        assertFalse(repository.matchesItem(tvItem, "show123", "show123:2:6"))
        assertFalse(repository.matchesItem(tvItem, "show123", "s1e5"))
        assertFalse(repository.matchesItem(tvItem, "wrongShow", "s2e5"))
    }
}

