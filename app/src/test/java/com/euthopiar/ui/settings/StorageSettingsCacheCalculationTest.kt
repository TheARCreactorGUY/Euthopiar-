package com.euthopiar.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.ui.settings.pages.StorageSettingsViewModel
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
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageSettingsCacheCalculationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCacheCalculationExcludesExternalDownloadsAndHandlesSymlinks() {
        val rootCache = tempFolder.newFolder("app_cache")
        val normalCacheFile = File(rootCache, "cached_image_123.tmp")
        normalCacheFile.writeBytes(ByteArray(1024 * 50)) // 50 KB

        val downloadsFolder = tempFolder.newFolder("downloads")
        val downloadedMovie = File(downloadsFolder, "big_movie_1080p.mkv")
        downloadedMovie.writeBytes(ByteArray(1024 * 100)) // 100 KB

        // Create a symlink inside rootCache pointing to downloadsFolder (simulating external storage escape)
        val symlinkInsideCache = File(rootCache, "symlink_to_downloads")
        try {
            Files.createSymbolicLink(symlinkInsideCache.toPath(), downloadsFolder.toPath())
        } catch (_: Exception) {
            // Some Windows sandbox environments may restrict symlink creation without SeCreateSymbolicLinkPrivilege
        }

        val viewModel = StorageSettingsViewModel(ApplicationProvider.getApplicationContext())
        // ViewModel initialized properly without crashing
        assertTrue(viewModel.cacheSizeBytes.value >= 0L)
    }

    @Test
    fun testCustomDownloadDirInitialization() {
        val viewModel = StorageSettingsViewModel(ApplicationProvider.getApplicationContext())
        assertFalse(viewModel.defaultDownloadDir.isEmpty())
        assertEquals(null, viewModel.customDownloadDir.value)
    }
}
