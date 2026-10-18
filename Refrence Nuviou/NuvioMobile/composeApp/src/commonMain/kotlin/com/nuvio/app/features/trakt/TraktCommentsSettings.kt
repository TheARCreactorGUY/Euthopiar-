package com.nuvio.app.features.trakt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TraktCommentsSettings {
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (_enabled.value == value) return
        _enabled.value = value
        TraktCommentsStorage.saveEnabled(value)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        _enabled.value = TraktCommentsStorage.loadEnabled() ?: true
    }
}
