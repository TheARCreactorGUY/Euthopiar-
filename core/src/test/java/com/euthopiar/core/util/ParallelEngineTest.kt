package com.euthopiar.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class ParallelEngineTest {

    @Test
    fun testParallelMapIsolatedWithHangingAndThrowingTasks() = runBlocking {
        val tasks = listOf(
            "task_fast_1",
            "task_hang",
            "task_fast_2",
            "task_throw",
            "task_fast_3"
        )

        val duration = measureTimeMillis {
            val results = tasks.parallelMapIsolated(timeoutMs = 500L, concurrencyLimit = 8) { item ->
                when (item) {
                    "task_hang" -> {
                        delay(10_000L) // 10s hang
                        "hung"
                    }
                    "task_throw" -> {
                        throw RuntimeException("Fatal error in task")
                    }
                    else -> {
                        delay(50L)
                        "completed_$item"
                    }
                }
            }

            assertEquals("Should return exactly the 3 successful results", 3, results.size)
            assertTrue(results.contains("completed_task_fast_1"))
            assertTrue(results.contains("completed_task_fast_2"))
            assertTrue(results.contains("completed_task_fast_3"))
        }

        println("Execution completed in ${duration}ms")
        assertTrue("Total duration should be less than 2000ms (timeout is 500ms)", duration < 2000L)
    }

    @Test
    fun testEmptyCollectionReturnsEmpty() = runBlocking {
        val empty = emptyList<String>()
        val results = empty.parallelMapIsolated { it }
        assertTrue(results.isEmpty())
    }

    @Test
    fun testConcurrencyThrottle() = runBlocking {
        val tasks = (1..10).toList()
        var maxConcurrent = 0
        var currentActive = 0
        val lock = Any()

        val results = tasks.parallelMapIsolated(timeoutMs = 1000L, concurrencyLimit = 3) { num ->
            synchronized(lock) {
                currentActive++
                if (currentActive > maxConcurrent) maxConcurrent = currentActive
            }
            delay(100L)
            synchronized(lock) {
                currentActive--
            }
            num * 2
        }

        assertEquals(10, results.size)
        assertTrue("Max concurrent tasks ($maxConcurrent) should not exceed limit (3)", maxConcurrent <= 3)
    }
}
