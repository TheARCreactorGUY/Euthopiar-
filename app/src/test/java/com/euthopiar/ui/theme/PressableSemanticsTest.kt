package com.euthopiar.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PressableSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPressableNodeIsClickableAndExposesRoleButton() {
        var clicked = false

        composeTestRule.setContent {
            Box(
                modifier = Modifier.pressable {
                    clicked = true
                }
            ) {
                Text(text = "Play Episode")
            }
        }

        val node = composeTestRule.onNodeWithText("Play Episode")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        node.performClick()
        assertTrue("Expected onClick to be invoked when performClick() is executed", clicked)
    }

    @Test
    fun testPlayButtonSpringPressExposesRoleButton() {
        var clicked = false

        composeTestRule.setContent {
            Box(
                modifier = Modifier.playButtonSpringPress {
                    clicked = true
                }
            ) {
                Text(text = "Spring Play")
            }
        }

        val node = composeTestRule.onNodeWithText("Spring Play")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        node.performClick()
        assertTrue("Expected onClick to be invoked on spring pressable", clicked)
    }
}
