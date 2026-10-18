package com.nuvio.app.features.library

internal expect object LibraryClock {
    fun nowEpochMs(): Long
}
