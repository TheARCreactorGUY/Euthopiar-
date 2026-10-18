package com.euthopiar.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.euthopiar.ui.components.AppFilterChip
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchTargetAndScalingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMinTouchTargetTokenIs48dp() {
        assertTrue(
            "Dimens.minTouchTarget must be at least 48dp",
            Dimens.minTouchTarget >= 48.dp
        )
    }

    @Test
    fun testAppFilterChipMeetsMinimumTouchTargetSize() {
        composeTestRule.setContent {
            AppFilterChip(
                text = "Action",
                selected = false,
                onClick = {}
            )
        }

        val node = composeTestRule.onNodeWithText("Action")
        node.assertHeightIsAtLeast(48.dp)
        node.assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun testTouchTargetConstraintsEnforceMinimumDimensions() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                    .pressable(role = androidx.compose.ui.semantics.Role.Button, onClick = {})
            ) {
                Text(text = "Target Area")
            }
        }

        val node = composeTestRule.onNodeWithText("Target Area")
        node.assertHeightIsAtLeast(48.dp)
        node.assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun testColorTokensMeetWcagContrastRequirements() {
        fun contrastRatio(fg: Color, bg: Color): Double {
            return ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())
        }

        // TextPrimary on Background (should be > 7:1)
        val textPrimaryBgContrast = contrastRatio(TextPrimary, Background)
        assertTrue(
            "TextPrimary on Background contrast ratio ($textPrimaryBgContrast) must exceed 7:1",
            textPrimaryBgContrast >= 7.0
        )

        // TextPrimary on SurfaceHigh (should be > 7:1)
        val textPrimarySurfaceContrast = contrastRatio(TextPrimary, SurfaceHigh)
        assertTrue(
            "TextPrimary on SurfaceHigh contrast ratio ($textPrimarySurfaceContrast) must exceed 7:1",
            textPrimarySurfaceContrast >= 7.0
        )

        // TextSecondary on SurfaceHigh (should be > 4.5:1)
        val textSecondarySurfaceContrast = contrastRatio(TextSecondary, SurfaceHigh)
        assertTrue(
            "TextSecondary on SurfaceHigh contrast ratio ($textSecondarySurfaceContrast) must exceed 4.5:1",
            textSecondarySurfaceContrast >= 4.5
        )
    }
}
