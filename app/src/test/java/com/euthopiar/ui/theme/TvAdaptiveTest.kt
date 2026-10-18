package com.euthopiar.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvAdaptiveTest {

    @Test
    fun testTvHorizontalSafeMarginCalculation() {
        // Safe area margin is 5% of screen width, minimum 48dp
        val margin1080p = Dimens.tvHorizontalMargin(1920.dp)
        assertEquals(96.dp, margin1080p)

        val margin720p = Dimens.tvHorizontalMargin(1280.dp)
        assertEquals(64.dp, margin720p)

        val marginSmallTv = Dimens.tvHorizontalMargin(800.dp)
        // 800 * 0.05 = 40dp, but minimum is 48dp
        assertEquals(48.dp, marginSmallTv)
    }

    @Test
    fun testTvTypographyScalesBodyText() {
        val tvTypography = AdaptiveTypography.getTypography(isTV = true)

        // TV bodyMedium must scale to >= 16sp
        assertTrue(
            "TV bodyMedium fontSize (${tvTypography.bodyMedium.fontSize}) must be at least 16sp",
            tvTypography.bodyMedium.fontSize >= 16.sp
        )

        // TV bodyLarge must scale to >= 18sp
        assertTrue(
            "TV bodyLarge fontSize (${tvTypography.bodyLarge.fontSize}) must be at least 18sp",
            tvTypography.bodyLarge.fontSize >= 18.sp
        )
    }

    @Test
    fun testWindowDimensionsTvSafeMarginIntegration() {
        val tvDimensions = WindowDimensions(
            widthClass = WindowWidthClass.Expanded,
            widthDp = 1920.dp,
            heightDp = 1080.dp,
            isLandscape = true,
            isTV = true
        )

        assertEquals(96.dp, tvDimensions.horizontalMargin)
    }
}
