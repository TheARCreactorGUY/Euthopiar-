package com.euthopiar.data.local

import android.util.Log
import androidx.core.util.AtomicFile
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class AtomicJsonStore<T>(
    file: File,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val atomicFile = AtomicFile(file)
    private val mutex = Mutex()

    suspend fun read(): T = mutex.withLock { withContext(io) { readLocked() } }

    suspend fun update(transform: (T) -> T): T = mutex.withLock {
        withContext(io) {
            val next = transform(readLocked())
            writeLocked(next)
            next
        }
    }

    suspend fun write(value: T) = mutex.withLock { withContext(io) { writeLocked(value) } }

    private fun readLocked(): T {
        if (!atomicFile.baseFile.exists()) return default
        return try {
            json.decodeFromString(serializer, atomicFile.readFully().decodeToString())
        } catch (e: Exception) {
            Log.e("AtomicJsonStore", "Corrupted file ${atomicFile.baseFile.name}, resetting to default", e)
            default
        }
    }

    private fun writeLocked(value: T) {
        val bytes = json.encodeToString(serializer, value).encodeToByteArray()
        atomicFile.baseFile.parentFile?.mkdirs()
        val out = atomicFile.startWrite()
        try {
            out.write(bytes)
            out.flush()
            // On Windows host JVMs (e.g. Robolectric tests), java.io.File.renameTo fails if destination exists.
            // On Android (Linux), File.renameTo uses atomic rename(2) which replaces destination atomically.
            if (System.getProperty("os.name")?.lowercase()?.contains("win") == true) {
                atomicFile.baseFile.delete()
            }
            atomicFile.finishWrite(out)
        } catch (t: Throwable) {
            atomicFile.failWrite(out)
            throw t
        }
    }
}
