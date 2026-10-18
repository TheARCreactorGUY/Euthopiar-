package com.euthopiar.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes a collection of asynchronous tasks in parallel with:
 * 1. Individual per-task timeout (capped at 3,500ms default)
 * 2. Supervisor isolation (one dead or stalled server never cancels siblings)
 * 3. Concurrency throttle via Semaphore to prevent network saturation
 */
suspend fun <T, R> Collection<T>.parallelMapIsolated(
    timeoutMs: Long = 3500L,
    concurrencyLimit: Int = 8,
    transform: suspend (T) -> R?
): List<R> = supervisorScope {
    val semaphore = Semaphore(concurrencyLimit)
    val deferreds = this@parallelMapIsolated.map { item ->
        async(Dispatchers.IO) {
            semaphore.withPermit {
                try {
                    withTimeoutOrNull(timeoutMs) {
                        transform(item)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    // Log failure without crashing or aborting siblings
                    null
                }
            }
        }
    }
    deferreds.awaitAll().filterNotNull()
}
