package com.euthopiar.data.sync.supabase

import android.content.Context
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Two-Tier Playback Sync Coordinator.
 *
 * Tier 1 (Local): Ultra high-frequency (2-second) Room persistence to guarantee zero progress loss locally.
 * Tier 2 (Cloud): Event-driven replication to Supabase:
 *  - 15-minute coalesced emergency heartbeat during uninterrupted active playback (prevents Postgres MVCC table bloat)
 *  - 3-second debounce on seek or pause
 *  - 0-ms immediate flush on player exit or completion
 *
 * Runs completely asynchronously off the UI thread and never blocks ExoPlayer or frame rendering.
 */
class PlaybackSyncCoordinator(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val syncManager by lazy { SupabaseSyncManager.getInstance(context) }
    private var heartbeatJob: Job? = null
    private var seekDebounceJob: Job? = null
    private val isSessionActive = AtomicBoolean(false)

    fun onPlaybackStarted() {
        isSessionActive.set(true)
        startHeartbeat()
    }

    fun onPlaybackPaused() {
        scheduleSeekOrPauseFlush()
    }

    fun onPlaybackResumed() {
        isSessionActive.set(true)
        seekDebounceJob?.cancel()
        if (heartbeatJob == null || heartbeatJob?.isActive == false) {
            startHeartbeat()
        }
    }

    fun onPlaybackSeeked() {
        scheduleSeekOrPauseFlush()
    }

    fun onPlaybackEndedOrExited(preFlushJob: Job? = null) {
        isSessionActive.set(false)
        heartbeatJob?.cancel()
        heartbeatJob = null
        seekDebounceJob?.cancel()
        seekDebounceJob = null

        // Immediate flush on exit or completion after local Room save completes
        scope.launch {
            try {
                preFlushJob?.join()
                syncManager.syncPlaybackDirty()
            } catch (_: Exception) {
                // Non-blocking fail-soft
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isSessionActive.get()) {
                delay(15 * 60 * 1000L) // 15-minute coalesced heartbeat to prevent DB update bloat
                try {
                    syncManager.syncPlaybackDirty()
                } catch (_: Exception) {
                    // Non-blocking fail-soft
                }
            }
        }
    }

    private fun scheduleSeekOrPauseFlush() {
        seekDebounceJob?.cancel()
        seekDebounceJob = scope.launch {
            delay(3000L) // 3-second debounce
            try {
                syncManager.syncPlaybackDirty()
            } catch (_: Exception) {
                // Non-blocking fail-soft
            }
        }
    }
}
