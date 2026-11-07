package com.euthopiar.ui.walkthrough

import android.content.Context
import androidx.compose.ui.geometry.Rect
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.data.update.AppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The 3 distinct interactive guided steps of the Euthopiar setup tour.
 */
enum class WalkthroughStep {
    NONE,
    STEP_1_PROFILE,
    STEP_2_PLUGINS_TILE,
    STEP_3_INSTALL_REPO
}

/**
 * Encapsulates the pixel-perfect highlight geometry for the spotlight cutout.
 */
data class WalkthroughHighlight(
    val bounds: Rect,
    val cornerRadiusPx: Float,
    val isInteractive: Boolean = false
)

/**
 * Global coordinator for the interactive first-install onboarding walkthrough.
 *
 * Responsibilities:
 * - Manages step state progression (Profile -> Plugins -> Add Repository) on new installations only.
 * - Directly links highlight clicks to navigation destinations.
 * - Handles back gestures safely to prevent app closures or broken navigation state.
 */
class WalkthroughCoordinator private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentStep = MutableStateFlow(WalkthroughStep.NONE)
    val currentStep: StateFlow<WalkthroughStep> = _currentStep.asStateFlow()

    private val _isSkippable = MutableStateFlow(false)
    val isSkippable: StateFlow<Boolean> = _isSkippable.asStateFlow()

    private val _targetHighlight = MutableStateFlow<WalkthroughHighlight?>(null)
    val targetHighlight: StateFlow<WalkthroughHighlight?> = _targetHighlight.asStateFlow()

    private val _targetBounds = MutableStateFlow<Rect?>(null)
    val targetBounds: StateFlow<Rect?> = _targetBounds.asStateFlow()

    /**
     * Direct navigation callback registered by the root navigation host.
     */
    var navigationHandler: ((route: String) -> Unit)? = null

    val isActive: Boolean
        get() = _currentStep.value != WalkthroughStep.NONE

    fun start(isFreshInstall: Boolean = true) {
        _isSkippable.value = !isFreshInstall
        _targetHighlight.value = null
        _targetBounds.value = null
        _currentStep.value = WalkthroughStep.STEP_1_PROFILE
    }

    fun reportTarget(
        step: WalkthroughStep,
        bounds: Rect,
        cornerRadiusPx: Float,
        isInteractive: Boolean = false
    ) {
        if (_currentStep.value == step) {
            val highlight = WalkthroughHighlight(bounds, cornerRadiusPx, isInteractive)
            _targetHighlight.value = highlight
            _targetBounds.value = bounds
        }
    }

    fun reportTargetBounds(step: WalkthroughStep, bounds: Rect) {
        reportTarget(step, bounds, cornerRadiusPx = 20f, isInteractive = false)
    }

    fun onTargetTapped(step: WalkthroughStep) {
        if (_currentStep.value != step) return

        when (step) {
            WalkthroughStep.STEP_1_PROFILE -> {
                _targetHighlight.value = null
                _targetBounds.value = null
                _currentStep.value = WalkthroughStep.STEP_2_PLUGINS_TILE
                navigationHandler?.invoke("profile")
            }
            WalkthroughStep.STEP_2_PLUGINS_TILE -> {
                _targetHighlight.value = null
                _targetBounds.value = null
                _currentStep.value = WalkthroughStep.STEP_3_INSTALL_REPO
                navigationHandler?.invoke("settings/plugins")
            }
            WalkthroughStep.STEP_3_INSTALL_REPO -> {
                // Completed via the button or repository action
            }
            WalkthroughStep.NONE -> {}
        }
    }

    fun stepBack() {
        when (_currentStep.value) {
            WalkthroughStep.STEP_3_INSTALL_REPO -> {
                _targetHighlight.value = null
                _targetBounds.value = null
                _currentStep.value = WalkthroughStep.STEP_2_PLUGINS_TILE
                navigationHandler?.invoke("profile")
            }
            WalkthroughStep.STEP_2_PLUGINS_TILE -> {
                _targetHighlight.value = null
                _targetBounds.value = null
                _currentStep.value = WalkthroughStep.STEP_1_PROFILE
                navigationHandler?.invoke("home")
            }
            WalkthroughStep.STEP_1_PROFILE, WalkthroughStep.NONE -> {}
        }
    }

    fun complete(context: Context) {
        _currentStep.value = WalkthroughStep.NONE
        _targetHighlight.value = null
        _targetBounds.value = null
        persistCompletion(context)
        navigationHandler?.invoke("home")
    }

    fun skip(context: Context) {
        if (_isSkippable.value) {
            _currentStep.value = WalkthroughStep.NONE
            _targetHighlight.value = null
            _targetBounds.value = null
            persistCompletion(context)
            navigationHandler?.invoke("home")
        }
    }

    private fun persistCompletion(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentVersion = AppUpdateManager.getInstance(context).getCurrentVersionName()
                PreferencesRepository.getInstance(context).setWalkthroughCompletedVersion(currentVersion)
            } catch (_: Exception) {}
        }
    }

    companion object {
        val instance by lazy { WalkthroughCoordinator() }
    }
}
