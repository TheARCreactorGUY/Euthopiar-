package com.euthopiar.core.util

/**
 * Manages the active session index across app launches to ensure session consistency
 * while guaranteeing that each new app start reshuffles content from the previous start.
 */
object AppSessionManager {
    @Volatile
    private var sessionIndex: Int = 1

    fun initialize(index: Int) {
        sessionIndex = index
    }

    fun increment(): Int {
        sessionIndex += 1
        return sessionIndex
    }

    fun getSessionIndex(): Int = sessionIndex
}
