package com.euthopiar.ui.walkthrough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalkthroughCoordinatorTest {

    private lateinit var coordinator: WalkthroughCoordinator

    @Before
    fun setup() {
        coordinator = WalkthroughCoordinator.instance
    }

    @Test
    fun testFreshInstallWalkthroughNotSkippable() {
        coordinator.start(isFreshInstall = true)
        assertTrue(coordinator.isActive)
        assertEquals(WalkthroughStep.STEP_1_PROFILE, coordinator.currentStep.value)
        assertFalse("Fresh install walkthrough must not be skippable", coordinator.isSkippable.value)
    }

    @Test
    fun testAppUpdateWalkthroughIsSkippable() {
        coordinator.start(isFreshInstall = false)
        assertTrue(coordinator.isActive)
        assertEquals(WalkthroughStep.STEP_1_PROFILE, coordinator.currentStep.value)
        assertTrue("App update walkthrough must be skippable", coordinator.isSkippable.value)
    }

    @Test
    fun testStepProgressionFromProfileToPluginsToRepository() {
        coordinator.start(isFreshInstall = true)
        assertEquals(WalkthroughStep.STEP_1_PROFILE, coordinator.currentStep.value)

        // Step 1: User taps Profile on navbar
        coordinator.onTargetTapped(WalkthroughStep.STEP_1_PROFILE)
        assertEquals(WalkthroughStep.STEP_2_PLUGINS_TILE, coordinator.currentStep.value)

        // Step 2: User taps Plugins & Scrapers tile in Settings
        coordinator.onTargetTapped(WalkthroughStep.STEP_2_PLUGINS_TILE)
        assertEquals(WalkthroughStep.STEP_3_INSTALL_REPO, coordinator.currentStep.value)
    }

    @Test
    fun testStepBackProgression() {
        coordinator.start(isFreshInstall = true)
        coordinator.onTargetTapped(WalkthroughStep.STEP_1_PROFILE)
        coordinator.onTargetTapped(WalkthroughStep.STEP_2_PLUGINS_TILE)
        assertEquals(WalkthroughStep.STEP_3_INSTALL_REPO, coordinator.currentStep.value)

        // Step back from 3 -> 2
        coordinator.stepBack()
        assertEquals(WalkthroughStep.STEP_2_PLUGINS_TILE, coordinator.currentStep.value)

        // Step back from 2 -> 1
        coordinator.stepBack()
        assertEquals(WalkthroughStep.STEP_1_PROFILE, coordinator.currentStep.value)

        // Step back at root stays at 1
        coordinator.stepBack()
        assertEquals(WalkthroughStep.STEP_1_PROFILE, coordinator.currentStep.value)
    }
}
