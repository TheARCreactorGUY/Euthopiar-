package com.euthopiar.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.euthopiar.ui.theme.EuthopiarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOnboardingPagingButtonVisibility() {
        composeTestRule.setContent {
            EuthopiarTheme {
                OnboardingScreen(onComplete = {})
            }
        }

        // Page 0: Begin Journey should be displayed
        composeTestRule.onNodeWithText("Begin Journey").assertIsDisplayed()

        // Click Begin Journey to go to Page 1
        composeTestRule.onNodeWithText("Begin Journey").performClick()
        composeTestRule.waitForIdle()

        // Page 1: Check Back and Continue
        val backNode = composeTestRule.onNodeWithText("Back")
        backNode.assertIsDisplayed()

        val continueNode = composeTestRule.onNodeWithText("Continue to Canvas")
        continueNode.performScrollTo().assertIsDisplayed()

        // Click Continue to Canvas to go to Page 2
        continueNode.performClick()
        composeTestRule.waitForIdle()

        // Page 2: Check Enter Euthopiar
        val enterNode = composeTestRule.onNodeWithText("Enter Euthopiar")
        enterNode.assertIsDisplayed()
    }
}
