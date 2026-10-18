package com.euthopiar.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesKeyMappingTest {

    private lateinit var context: Context
    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = PreferencesRepository.getInstance(context)
    }

    @Test
    fun testPreferencesRepositorySingleton() {
        val repo2 = PreferencesRepository.getInstance(context)
        assertEquals(repository, repo2)
    }

    @Test
    fun testDefaultPlaybackAndGeneralPreferences() = runBlocking {
        // Quality
        val quality = repository.defaultQuality.first()
        assertEquals("Auto", quality)

        // Auto-play next episode
        val autoPlay = repository.autoPlayNextEpisode.first()
        assertTrue(autoPlay)

        // Seek interval
        val seekInterval = repository.seekIntervalSeconds.first()
        assertEquals(10, seekInterval)

        // Subtitle preferences
        val subSize = repository.subtitleSizeSp.first()
        assertEquals(18, subSize)
        val subBg = repository.subtitleBackgroundEnabled.first()
        assertTrue(subBg)
        val subColor = repository.subtitleColor.first()
        assertEquals("WHITE", subColor)
        val subOutline = repository.subtitleOutline.first()
        assertTrue(subOutline)

        // Playback automations
        val autoSkipIntro = repository.autoSkipIntro.first()
        assertFalse(autoSkipIntro)
        val skipIntroSecs = repository.skipIntroDurationSeconds.first()
        assertEquals(85, skipIntroSecs)
        val skipOutroSecs = repository.skipOutroDurationSeconds.first()
        assertEquals(90, skipOutroSecs)
        val countdownSecs = repository.countdownNextEpisodeSeconds.first()
        assertEquals(10, countdownSecs)

        // Buffer size & hardware acceleration
        val bufferSize = repository.playbackBufferSizeMb.first()
        assertEquals(50, bufferSize)
        val hwAccel = repository.hardwareAccelerationEnabled.first()
        assertTrue(hwAccel)
    }

    @Test
    fun testDefaultAppearanceAndInterfacePreferences() = runBlocking {
        val cardScale = repository.cardScaleMode.first()
        assertEquals(CardScaleMode.FLAGSHIP, cardScale)

        val navbarStyle = repository.navbarStyle.first()
        assertEquals(NavbarStyle.FLOATING, navbarStyle)

        val top10Style = repository.top10Style.first()
        assertEquals(Top10Style.PosterTinted, top10Style)

        val sectionLabel = repository.sectionLabelStyle.first()
        assertEquals(SectionLabelStyle.AccentBar, sectionLabel)

        val longPressStyle = repository.longPressStyle.first()
        assertEquals(com.euthopiar.ui.longpress.LongPressSheetStyle.HERO_PEEK, longPressStyle)

        val reduceMotion = repository.reduceMotion.first()
        assertFalse(reduceMotion)
    }

    @Test
    fun testNavbarStyleDockedSilentlyFallsBackToFloating() = runBlocking {
        repository.setNavbarStyle(NavbarStyle.DOCKED)
        val style = repository.navbarStyle.first()
        assertEquals(NavbarStyle.FLOATING, style)
    }


    @Test
    fun testMaxSupportedBufferSizeUtility() {
        val maxBuffer = PreferencesRepository.getMaxSupportedBufferSizeMb(context)
        assertTrue("Max buffer size must be one of 25, 50, 100", maxBuffer in listOf(25, 50, 100))
    }

    @Test
    fun testSmartFailoverAndAudioPassthroughPreferences() = runBlocking {
        val smartFailover = repository.smartFailoverEnabled.first()
        assertTrue(smartFailover)

        repository.setSmartFailoverEnabled(false)
        assertFalse(repository.smartFailoverEnabled.first())
        repository.setSmartFailoverEnabled(true)
        assertTrue(repository.smartFailoverEnabled.first())

        val audioPassthrough = repository.audioPassthroughEnabled.first()
        assertFalse(audioPassthrough)

        repository.setAudioPassthroughEnabled(true)
        assertTrue(repository.audioPassthroughEnabled.first())
        repository.setAudioPassthroughEnabled(false)
        assertFalse(repository.audioPassthroughEnabled.first())
    }

    @Test
    fun testCustomDownloadDirPreference() = runBlocking {
        repository.setCustomDownloadDir("/storage/emulated/0/MyMovies")
        val customDir = repository.customDownloadDir.first()
        assertEquals("/storage/emulated/0/MyMovies", customDir)

        repository.setCustomDownloadDir(null)
        val resetDir = repository.customDownloadDir.first()
        assertEquals(null, resetDir)
    }
}
