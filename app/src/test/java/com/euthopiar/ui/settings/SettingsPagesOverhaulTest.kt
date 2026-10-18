package com.euthopiar.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.models.SettingsCategory
import com.euthopiar.ui.settings.models.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPagesOverhaulTest {

    @Test
    fun testSettingsTokensMonochromeIntegrity() {
        // LCD and AMOLED backgrounds
        assertEquals(Color(0xFF09090D), SettingsTokens.LcdBackground)
        assertEquals(Color(0xFF000000), SettingsTokens.AmoledBackground)

        // TextDark for white pills must be #0B0B0F
        assertEquals(Color(0xFF0B0B0F), SettingsTokens.TextDark)

        // Hairline divider and border widths must be 0.5dp
        assertEquals(0.5.dp, SettingsTokens.HairlineWidth)

        // Glass radii tokens
        assertEquals(8.dp, SettingsTokens.RadiusChip)
        assertEquals(12.dp, SettingsTokens.RadiusControl)
        assertEquals(13.dp, SettingsTokens.RadiusTile)
        assertEquals(15.dp, SettingsTokens.RadiusTileEmblem)
        assertEquals(18.dp, SettingsTokens.RadiusPopover)
        assertEquals(20.dp, SettingsTokens.RadiusPreviewCard)
        assertEquals(28.dp, SettingsTokens.RadiusDialog)
        assertEquals(999.dp, SettingsTokens.RadiusPill)
    }

    @Test
    fun testSettingsPageTitlesReflectConciseRedesign() {
        assertEquals("Playback", SettingsPage.Playback.title)
        assertEquals("Appearance", SettingsPage.Appearance.title)
        assertEquals("Integrations", SettingsPage.Integrations.title)
        assertEquals("Storage & cache", SettingsPage.Storage.title)
        assertEquals("About Euthopiar", SettingsPage.About.title)
    }

    @Test
    fun testSettingsPageCategories() {
        assertEquals(SettingsCategory.General, SettingsPage.Playback.category)
        assertEquals(SettingsCategory.General, SettingsPage.Appearance.category)
        assertEquals(SettingsCategory.Ecosystem, SettingsPage.Integrations.category)
        assertEquals(SettingsCategory.Advanced, SettingsPage.Storage.category)
        assertEquals(SettingsCategory.About, SettingsPage.About.category)
    }

    @Test
    fun testSettingsTokensColorTransparencies() {
        // High-contrast white opacities
        assertEquals(0.96f, SettingsTokens.TextWhite96.alpha, 0.01f)
        assertEquals(0.88f, SettingsTokens.TextWhite88.alpha, 0.01f)
        assertEquals(0.82f, SettingsTokens.TextWhite82.alpha, 0.01f)
        assertEquals(0.72f, SettingsTokens.TextWhite72.alpha, 0.01f)
        assertEquals(0.54f, SettingsTokens.TextWhite54.alpha, 0.01f)
        assertEquals(0.45f, SettingsTokens.TextWhite45.alpha, 0.01f)
        assertEquals(0.42f, SettingsTokens.TextWhite42.alpha, 0.01f)
        assertEquals(0.40f, SettingsTokens.TextWhite40.alpha, 0.01f)
        assertEquals(0.30f, SettingsTokens.TextWhite30.alpha, 0.01f)

        // Borders & hairlines
        assertEquals(0.16f, SettingsTokens.HairlineBorder.alpha, 0.01f)
        assertEquals(0.32f, SettingsTokens.HairlineBorderBright.alpha, 0.01f)
        assertEquals(0.14f, SettingsTokens.HairlineDivider.alpha, 0.01f)
    }

    @Test
    fun testTraktAndSimklClientEndpoints() {
        assertEquals("https://api.trakt.tv", com.euthopiar.core.sync.trakt.TraktClient.BASE_URL)
        assertEquals("https://api.simkl.com", com.euthopiar.core.sync.simkl.SimklClient.BASE_URL)
    }
}
