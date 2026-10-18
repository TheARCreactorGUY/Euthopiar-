package com.euthopiar.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global Picture-in-Picture coordinator between Android Activity lifecycle
 * and Jetpack Compose Player UI.
 */
object PipController {
    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _isPipSupported = MutableStateFlow(true)
    val isPipSupported: StateFlow<Boolean> = _isPipSupported.asStateFlow()

    var onTogglePlayPause: (() -> Unit)? = null
    var onSeekRelative: ((Long) -> Unit)? = null
    var seekIntervalMs: Long = 10000L
    var isPlaying: () -> Boolean = { false }
    var onRequestPip: (() -> Unit)? = null

    fun setInPipMode(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    fun setPipSupported(supported: Boolean) {
        _isPipSupported.value = supported
    }

    fun requestPip() {
        onRequestPip?.invoke()
    }

    fun clearCallbacks() {
        onTogglePlayPause = null
        onSeekRelative = null
        seekIntervalMs = 10000L
        isPlaying = { false }
        onRequestPip = null
    }
}
