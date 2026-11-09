package com.euthopiar.core.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class P2pHardenedDownloaderTest {

    @Test
    fun diskCacheCapacity_isBoundedAndAvoids32BitBlockOverflow() {
        // Must NOT be Long.MAX_VALUE to prevent arithmetic overflow in native Rust/C++
        assertNotEquals(Long.MAX_VALUE, UNLIMITED_P2P_DISK_CACHE_BYTES)
        // Must be at least 2 GiB to provide ample buffer for multi-gigabyte media
        assertTrue(UNLIMITED_P2P_DISK_CACHE_BYTES >= 2L * 1024L * 1024L * 1024L)
        // Must not exceed 32 GiB so that block count in 16 KiB blocks never overflows a 32-bit signed integer
        val blockCount = UNLIMITED_P2P_DISK_CACHE_BYTES / (16 * 1024)
        assertTrue(blockCount <= Int.MAX_VALUE)
        // Must fit cleanly in positive 64-bit signed integer without wrapping
        assertTrue(UNLIMITED_P2P_DISK_CACHE_BYTES > 0L)
    }

    @Test
    fun workDir_canonicalNamingPreventsNameCollisionAndLeaks() {
        val hash = "0123456789ABCDEF0123456789ABCDEF01234567"
        val canonical = canonicalP2pInfoHash(hash)
        val parentDir = File("/storage/emulated/0/Download")
        val workDir = File(parentDir, ".p2p_work_$canonical")

        assertEquals(
            "/storage/emulated/0/Download/.p2p_work_0123456789abcdef0123456789abcdef01234567",
            workDir.path.replace('\\', '/')
        )
    }

    @Test
    fun buildNuvioEngineConfig_assignsBoundedCapacity() {
        val stateDir = File("/tmp/state")
        val cacheDir = File("/tmp/cache")
        val config = buildNuvioEngineConfig(
            stateDirectory = stateDir,
            cacheDirectory = cacheDir,
            uploadEnabled = false,
            torrentProfile = P2pTorrentProfile.FAST,
            diskCacheCapacityBytes = UNLIMITED_P2P_DISK_CACHE_BYTES
        )

        assertEquals(UNLIMITED_P2P_DISK_CACHE_BYTES, config.diskCacheCapacityBytes)
        assertEquals(stateDir, config.dataDirectory)
        assertEquals(cacheDir, config.cacheDirectory)
    }
}
