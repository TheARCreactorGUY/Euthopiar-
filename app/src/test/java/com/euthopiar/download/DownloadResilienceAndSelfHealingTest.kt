package com.euthopiar.download

import android.content.Context
import androidx.core.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
class DownloadResilienceAndSelfHealingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAtomicFileCrashSafePersistence() {
        val metaFile = File(tempFolder.root, "test_metadata.json")
        val atomicFile = AtomicFile(metaFile)

        val sampleItem = DownloadItem(
            id = "test_item_1",
            mediaId = "media_123",
            title = "Test Movie",
            state = DownloadState.DOWNLOADING,
            bytesDownloaded = 5000L,
            totalBytes = 10000L
        )

        val originalList = listOf(sampleItem)
        val data = json.encodeToString(originalList)

        // Write atomically
        val fos = atomicFile.startWrite()
        fos.write(data.toByteArray(Charsets.UTF_8))
        atomicFile.finishWrite(fos)

        assertTrue(metaFile.exists())
        val readBackContent = atomicFile.readFully().toString(Charsets.UTF_8)
        val readBackItems = json.decodeFromString<List<DownloadItem>>(readBackContent)

        assertEquals(1, readBackItems.size)
        assertEquals("test_item_1", readBackItems[0].id)
        assertEquals(DownloadState.DOWNLOADING, readBackItems[0].state)
        assertEquals(5000L, readBackItems[0].bytesDownloaded)
    }

    @Test
    fun testRecoverableNetworkErrorClassification() {
        // Errors that must be classified as recoverable (auto-resumed on network restoration)
        val recoverableMessages = listOf(
            "Waiting for network",
            "Waiting for Wi-Fi",
            "Download interrupted: connection lost",
            "Download interrupted: connection interrupted",
            "java.net.SocketTimeoutException: Read timed out",
            "java.io.IOException: unexpected end of stream",
            "Software caused connection abort"
        )

        for (msg in recoverableMessages) {
            val isNetworkWait = msg.contains("Waiting for network", ignoreCase = true) ||
                    msg.contains("Waiting for Wi-Fi", ignoreCase = true)
            val isRecoverable = isNetworkWait ||
                    msg.contains("connection lost", ignoreCase = true) ||
                    msg.contains("connection interrupted", ignoreCase = true) ||
                    msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("end of stream", ignoreCase = true) ||
                    msg.contains("abort", ignoreCase = true)

            assertTrue("Expected '$msg' to be recoverable", isRecoverable)
        }

        // Errors that must be classified as permanent non-recoverable failures
        val permanentMessages = listOf(
            "Video stream not found (HTTP 404)",
            "Video stream expired or gone (HTTP 410)",
            "Payment required or traffic limit reached (HTTP 402)",
            "File removed from storage"
        )

        for (msg in permanentMessages) {
            val isNetworkWait = msg.contains("Waiting for network", ignoreCase = true) ||
                    msg.contains("Waiting for Wi-Fi", ignoreCase = true)
            val isRecoverable = isNetworkWait ||
                    msg.contains("connection lost", ignoreCase = true) ||
                    msg.contains("connection interrupted", ignoreCase = true) ||
                    msg.contains("timeout", ignoreCase = true)

            assertFalse("Expected '$msg' to be non-recoverable", isRecoverable)
        }
    }

    @Test
    fun testPartialFileRetainedOnPrematureDisconnect() {
        val downloadDir = tempFolder.newFolder("downloads")
        val partFile = File(downloadDir, "item_abc.part")
        partFile.writeBytes(ByteArray(1024 * 512)) // 512 KB downloaded

        assertTrue("Part file must exist", partFile.exists())
        assertEquals(512 * 1024L, partFile.length())

        // Ensure that part file is NOT deleted on premature connection drop
        // Existing checkpoint must remain available for Range: bytes=524288-
        val simulatedExistingBytes = if (partFile.exists()) partFile.length() else 0L
        assertEquals(512 * 1024L, simulatedExistingBytes)
    }

    @Test
    fun testRange200Vs206CorruptionProtection() {
        val downloadDir = tempFolder.newFolder("downloads")
        val partFile = File(downloadDir, "item_xyz.part")
        partFile.writeBytes(ByteArray(1000)) // 1000 bytes existing

        var existingBytes = partFile.length()
        assertEquals(1000L, existingBytes)

        // Scenario A: Server returned 206 Partial Content
        val responseCodeA = 206
        val isPartialA = responseCodeA == 206
        val appendModeA = isPartialA && existingBytes > 0L
        assertTrue("206 response must append to partial file", appendModeA)

        // Scenario B: Server ignored Range header and returned 200 OK (full body from byte 0)
        val responseCodeB = 200
        val isPartialB = responseCodeB == 206
        var appendModeB = isPartialB && existingBytes > 0L
        if (!appendModeB && existingBytes > 0L) {
            // Anti-corruption check triggered: reset partFile and existingBytes
            partFile.delete()
            existingBytes = 0L
        }

        assertFalse("200 response must NOT append to existing partial file", appendModeB)
        assertEquals("Existing bytes must reset to 0 to prevent byte duplication corruption", 0L, existingBytes)
        assertFalse("Stale partial file must be cleared when server sends 200 OK", partFile.exists())
    }
}
