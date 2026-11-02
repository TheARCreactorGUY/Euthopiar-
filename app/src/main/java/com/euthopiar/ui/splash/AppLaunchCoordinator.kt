package com.euthopiar.ui.splash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates dynamic early release and visual handoff between HomeScreen catalog readiness
 * and the AppIgnitionSequence.
 */
object AppLaunchCoordinator {
    private val _isHomeDataReady = MutableStateFlow(false)
    val isHomeDataReady: StateFlow<Boolean> = _isHomeDataReady.asStateFlow()

    private val _isIgnitionActive = MutableStateFlow(true)
    val isIgnitionActive: StateFlow<Boolean> = _isIgnitionActive.asStateFlow()

    var hasCompletedInitialLaunch: Boolean = false
        private set

    fun notifyHomeDataReady() {
        _isHomeDataReady.value = true
    }

    fun notifyIgnitionFinished() {
        _isIgnitionActive.value = false
        hasCompletedInitialLaunch = true
    }

    fun reset() {
        _isHomeDataReady.value = false
        _isIgnitionActive.value = true
        hasCompletedInitialLaunch = false
    }
}
