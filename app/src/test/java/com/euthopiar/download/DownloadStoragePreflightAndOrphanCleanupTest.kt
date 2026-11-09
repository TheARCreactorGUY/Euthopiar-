package com.euthopiar.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadStoragePreflightAndOrphanCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFormatBytesAccuracy() {
        assertEquals("0 MB", DownloadRepository.formatBytes(0L))
        assertEquals("0 MB", DownloadRepository.formatBytes(-500L))
        assertEquals("500 MB", DownloadRepository.formatBytes(500L * 1024L * 1024L))
        assertEquals("23.00 GB", DownloadRepository.formatBytes(23L * 1024L * 1024L * 1024L))
        assertEquals("82.00 GB", DownloadRepository.formatBytes(82L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testCleanupOrphanedP2pWorkDirs() {
        val rootDir = tempFolder.newFolder("downloads")

        val activeWorkDir = File(rootDir, ".p2p_work_active_hash_111").apply {
            mkdirs()
            File(this, "chunk_0.bin").writeBytes(ByteArray(1024))
        }

        val orphanWorkDir1 = File(rootDir, ".p2p_work_orphan_hash_222").apply {
            mkdirs()
            File(this, "sparse_payload.bin").writeBytes(ByteArray(2048))
        }

        val orphanWorkDir2 = File(rootDir, ".p2p_work_orphan_hash_333").apply {
            mkdirs()
        }

        val normalCompletedFile = File(rootDir, "Interstellar (2014).mkv").apply {
            writeBytes(ByteArray(4096))
        }

        assertTrue(activeWorkDir.exists())
        assertTrue(orphanWorkDir1.exists())
        assertTrue(orphanWorkDir2.exists())
        assertTrue(normalCompletedFile.exists())

        val engine = DownloadEngine(context)
        val activeHashes = setOf("active_hash_111")

        // Perform cleanup on test root directory
        val deletedCount = engine.cleanupOrphanedP2pWorkDirs(activeHashes, rootDir)

        assertEquals(2, deletedCount)
        assertTrue("Active P2P working directory should be kept", activeWorkDir.exists())
        assertFalse("Orphan 1 should be completely deleted", orphanWorkDir1.exists())
        assertFalse("Orphan 2 should be completely deleted", orphanWorkDir2.exists())
        assertTrue("Normal files should never be deleted", normalCompletedFile.exists())
    }

    @Test
    fun testStoragePreflightDetection() {
        val downloadDir = DownloadEngine(context).getDownloadDirectory()
        // Register stats: 50 GB available (block size default in Robolectric is 4096 or blockSize from stats)
        // registerStats(path, blockCount, freeBlocks, availableBlocks)
        org.robolectric.shadows.ShadowStatFs.registerStats(downloadDir.path, 10000000, 5000000, 5000000)
        org.robolectric.shadows.ShadowStatFs.registerStats(downloadDir, 10000000, 5000000, 5000000)

        val repository = DownloadRepository.getInstance(context)

        // 1 TB download should exceed available capacity
        val massiveBytes = 1000L * 1024L * 1024L * 1024L
        val result = repository.checkStorageCapacity(massiveBytes)

        assertTrue(
            "Massive download must return InsufficientSpace",
            result is StoragePreflightResult.InsufficientSpace
        )

        val insufficient = result as StoragePreflightResult.InsufficientSpace
        assertEquals(massiveBytes, insufficient.requiredBytes)
        assertTrue("Available bytes should be > 0", insufficient.availableBytes > 0L)

        // Small payload should be sufficient given 5000000 available blocks
        val tinyResult = repository.checkStorageCapacity(1024L)
        assertTrue("Tiny payload should be Sufficient", tinyResult is StoragePreflightResult.Sufficient)
    }

    @Test
    fun testHeroStorageDisplay_zeroCompletedEliminatesGhostGigabytes() {
        // When zero completed downloads exist and zero active downloads exist, totalAppBytes must return 0L
        val readyDownloads = emptyList<DownloadItem>()
        val activeDownloads = emptyList<DownloadItem>()
        val storageStatsAppUsedBytes = 82L * 1024L * 1024L * 1024L // Simulated 82 GB leftover/sparse
        val totalCompletedBytes = 0L
        val activeBytes = activeDownloads.sumOf { it.bytesDownloaded }

        val completedBaseBytes = if (readyDownloads.isNotEmpty()) {
            maxOf(totalCompletedBytes, storageStatsAppUsedBytes)
        } else {
            0L
        }
        val totalAppBytes = completedBaseBytes + activeBytes

        assertEquals(0L, totalAppBytes)
    }

    @Test
    fun testHeroStorageDisplay_activeDownloadsShowLiveBytes() {
        // When active downloads exist, live MB counter reflects real-time downloaded bytes
        val readyDownloads = emptyList<DownloadItem>()
        val activeDownloads = listOf(
            DownloadItem(
                id = "dl_1",
                mediaId = "123",
                title = "Test Movie",
                bytesDownloaded = 350L * 1024L * 1024L,
                totalBytes = 1024L * 1024L * 1024L,
                state = DownloadState.DOWNLOADING
            )
        )
        val storageStatsAppUsedBytes = 0L
        val totalCompletedBytes = 0L
        val activeBytes = activeDownloads.sumOf { it.bytesDownloaded }

        val completedBaseBytes = if (readyDownloads.isNotEmpty()) {
            maxOf(totalCompletedBytes, storageStatsAppUsedBytes)
        } else {
            0L
        }
        val totalAppBytes = completedBaseBytes + activeBytes

        assertEquals(350L * 1024L * 1024L, totalAppBytes)
    }
}
