package com.euthopiar.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
        var completed = false
        composeTestRule.setContent {
            EuthopiarTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        // Page 0: Get started should be displayed
        composeTestRule.onNodeWithText("Get started").assertIsDisplayed()

        // Click Get started to go to Page 1
        composeTestRule.onNodeWithText("Get started").performClick()
        composeTestRule.waitForIdle()

        // Page 1: Check Back and Continue
        val backNode = composeTestRule.onNodeWithContentDescription("Back")
        backNode.assertIsDisplayed()

        // Enter profile name to satisfy validation
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Alex")
        composeTestRule.waitForIdle()

        val continueNode = composeTestRule.onNodeWithText("Continue")
        continueNode.assertIsDisplayed()

        // Click Continue to go to Page 2
        continueNode.performClick()
        composeTestRule.waitForIdle()

        // Page 2: Check Enter Euthopiar
        val enterNode = composeTestRule.onNodeWithText("Enter Euthopiar")
        enterNode.assertIsDisplayed()
    }
}
