package com.euthopiar.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.euthopiar.ui.components.LocalShimmerOffset
import com.euthopiar.ui.components.ProvideShimmerScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MotionAdaptiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMotionTokenDurationsAndCurves() {
        assertEquals(120, Motion.DurationMicro)
        assertEquals(220, Motion.DurationStandard)
        assertEquals(360, Motion.DurationEmphasized)
        assertEquals(500, Motion.DurationSharedElement)
    }

    @Test
    fun testLocalReduceMotionDefaultAndProvided() {
        var observedDefault = true
        var observedReduced = false

        composeTestRule.setContent {
            observedDefault = LocalReduceMotion.current
            CompositionLocalProvider(LocalReduceMotion provides true) {
                observedReduced = LocalReduceMotion.current
            }
        }

        assertFalse("Default LocalReduceMotion should be false", observedDefault)
        assertTrue("Overridden LocalReduceMotion should be true", observedReduced)
    }

    @Test
    fun testProvideShimmerScopeRespectsReduceMotion() {
        var offsetWhenReduced: Float? = -999f

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                ProvideShimmerScope {
                    offsetWhenReduced = LocalShimmerOffset.current
                }
            }
        }

        assertNull("When reduce motion is enabled, ProvideShimmerScope must provide null offset", offsetWhenReduced)
    }

    @Test
    fun testProvideShimmerScopeProvidesSharedOffsetWhenActive() {
        var offsetWhenNormal: Float? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides false) {
                ProvideShimmerScope {
                    offsetWhenNormal = LocalShimmerOffset.current
                }
            }
        }

        assertNotNull("When reduce motion is disabled, ProvideShimmerScope must provide animated offset", offsetWhenNormal)
    }
}
