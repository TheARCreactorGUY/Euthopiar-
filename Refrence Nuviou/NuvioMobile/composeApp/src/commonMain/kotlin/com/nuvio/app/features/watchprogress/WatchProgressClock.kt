package com.nuvio.app.features.watchprogress

internal expect object WatchProgressClock {
    fun nowEpochMs(): Long
}
