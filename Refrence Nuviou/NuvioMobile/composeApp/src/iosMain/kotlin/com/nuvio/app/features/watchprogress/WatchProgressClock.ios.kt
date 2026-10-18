package com.nuvio.app.features.watchprogress

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

actual object WatchProgressClock {
    @OptIn(ExperimentalForeignApi::class)
    actual fun nowEpochMs(): Long = time(null) * 1000L
}
