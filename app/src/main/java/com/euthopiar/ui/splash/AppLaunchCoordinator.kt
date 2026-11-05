package com.euthopiar.ui.splash

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LaunchDestinationMode { UNKNOWN, HOME, ONBOARDING, PIN_LOCK }

/**
 * Coordinates dynamic early release and visual handoff between HomeScreen catalog readiness
 * and the AppIgnitionSequence.
 */
object AppLaunchCoordinator {
    private val _isHomeDataReady = MutableStateFlow(false)
    val isHomeDataReady: StateFlow<Boolean> = _isHomeDataReady.asStateFlow()

    private val _isIgnitionActive = MutableStateFlow(true)
    val isIgnitionActive: StateFlow<Boolean> = _isIgnitionActive.asStateFlow()

    private val _destinationMode = MutableStateFlow(LaunchDestinationMode.UNKNOWN)
    val destinationMode: StateFlow<LaunchDestinationMode> = _destinationMode.asStateFlow()

    private val _wordmarkTargetOffset = MutableStateFlow<Offset?>(null)
    val wordmarkTargetOffset: StateFlow<Offset?> = _wordmarkTargetOffset.asStateFlow()

    var hasCompletedInitialLaunch: Boolean = false
        private set

    fun notifyHomeDataReady() {
        _isHomeDataReady.value = true
    }

    fun notifyDestinationMode(mode: LaunchDestinationMode) {
        _destinationMode.value = mode
    }

    fun reportWordmarkTarget(offset: Offset) {
        _wordmarkTargetOffset.value = offset
    }

    fun notifyIgnitionFinished() {
        _isIgnitionActive.value = false
        hasCompletedInitialLaunch = true
    }

    fun reset() {
        _isHomeDataReady.value = false
        _isIgnitionActive.value = true
        _destinationMode.value = LaunchDestinationMode.UNKNOWN
        _wordmarkTargetOffset.value = null
        hasCompletedInitialLaunch = false
    }
}
