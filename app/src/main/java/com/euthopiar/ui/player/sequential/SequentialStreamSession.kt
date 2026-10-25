package com.euthopiar.ui.player.sequential

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

sealed interface BridgeState {
    data object Idle : BridgeState
    data class FastForwarding(
        val currentOffset: Long,
        val targetOffset: Long,
        val speedBytesPerSec: Long
    ) : BridgeState
    data class SeekClamped(val maxAvailableTimeUs: Long, val reason: String) : BridgeState
}

@UnstableApi
class SequentialStreamSession(
    private val context: Context,
    val url: String,
    val headers: Map<String, String>,
    val okHttpClient: OkHttpClient,
    val sessionId: String = UUID.randomUUID().toString()
) {
    @Volatile var resolvedUrl: String = url
    companion object {
        const val CHUNK_SIZE = 32L * 1024L * 1024L // 32 MB per chunk file
        private const val TAG = "SequentialStreamSession"

        fun calculateDynamicCacheBudget(context: Context): Long {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val storageManager = context.getSystemService(StorageManager::class.java)
                    val uuid = storageManager?.getUuidForPath(context.cacheDir)
                    if (storageManager != null && uuid != null) {
                        val availableBytes = storageManager.getAllocatableBytes(uuid)
                        (availableBytes / 4L).coerceIn(128L * 1024L * 1024L, 1024L * 1024L * 1024L)
                    } else {
                        (context.cacheDir.usableSpace / 4L).coerceIn(128L * 1024L * 1024L, 1024L * 1024L * 1024L)
                    }
                } else {
                    (context.cacheDir.usableSpace / 4L).coerceIn(128L * 1024L * 1024L, 1024L * 1024L * 1024L)
                }
            } catch (_: Throwable) {
                512L * 1024L * 1024L // 512 MB safe fallback
            }
        }
    }

    private val maxDiskCacheBytes: Long = calculateDynamicCacheBudget(context)
    private val maxForwardBridgeBytes: Long = (maxDiskCacheBytes * 0.50).toLong().coerceAtLeast(256L * 1024L * 1024L)

    private val sessionDir = File(context.cacheDir, "seq_stream_$sessionId")

    val clusterIndex = MatroskaClusterIndex()
    private val scanner = EbmlClusterStreamScanner(clusterIndex)

    private val lock = ReentrantLock()
    private val dataArrived = lock.newCondition()
    private val spaceAvailable = lock.newCondition()

    @Volatile var totalContentLength: Long = -1L
        private set
    @Volatile var highWaterMark: Long = 0L
        private set
    @Volatile var evictionFloorOffset: Long = 0L
        private set
    @Volatile var currentReadOffset: Long = 0L
    @Volatile var pendingTargetTimeUs: Long = -1L
    @Volatile var isClosed: Boolean = false
        private set
    @Volatile var streamError: IOException? = null
        private set

    private val _bridgeState = MutableStateFlow<BridgeState>(BridgeState.Idle)
    val bridgeState: StateFlow<BridgeState> = _bridgeState

    private val chunkFiles = ConcurrentHashMap<Long, File>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val maxRewindWindowBytes = (maxDiskCacheBytes * 0.25).toLong()
    private val maxReadAheadBytes = (maxDiskCacheBytes * 0.70).toLong()

    init {
        sessionDir.mkdirs()
        startSinglePipeDownload()
    }

    private fun startSinglePipeDownload() {
        scope.launch {
            val targetUrl = SequentialStreamManager.getPinnedUrl(url) ?: url
            val requestBuilder = Request.Builder().url(targetUrl)
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            if (headers.keys.none { it.equals("Range", ignoreCase = true) }) {
                requestBuilder.header("Range", "bytes=0-")
            }

            try {
                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Sequential stream HTTP ${response.code}: ${response.message}")
                    }
                    val finalUrl = response.request.url.toString()
                    resolvedUrl = finalUrl
                    SequentialStreamManager.pinResolvedUrl(url, finalUrl)
                    totalContentLength = response.body?.contentLength() ?: -1L
                    val inputStream = response.body?.byteStream()
                        ?: throw IOException("Empty response body from sequential stream")

                    val netBuffer = ByteArray(256 * 1024) // 256 KB network buffer
                    var activeChunkIndex = -1L
                    var activeRaf: RandomAccessFile? = null

                    try {
                        while (isActive && !isClosed) {
                            // Backpressure control: pause high-speed pulling if buffered far ahead
                            lock.withLock {
                                while (!isClosed &&
                                    pendingTargetTimeUs == -1L &&
                                    highWaterMark - currentReadOffset > maxReadAheadBytes
                                ) {
                                    spaceAvailable.await()
                                }
                            }

                            val read = inputStream.read(netBuffer)
                            if (read == -1) break

                            // 1. Scan for Matroska Clusters in-flight
                            scanner.onBytesReceived(netBuffer, 0, read, highWaterMark)

                            // 2. Write across fixed chunk boundaries
                            var bytesWritten = 0
                            while (bytesWritten < read) {
                                val writePos = highWaterMark + bytesWritten
                                val chunkIdx = writePos / CHUNK_SIZE
                                val chunkOffset = writePos % CHUNK_SIZE
                                val toWrite = min(
                                    (read - bytesWritten).toLong(),
                                    CHUNK_SIZE - chunkOffset
                                ).toInt()

                                if (chunkIdx != activeChunkIndex) {
                                    activeRaf?.close()
                                    val file = getOrCreateChunkFile(chunkIdx)
                                    activeRaf = RandomAccessFile(file, "rw")
                                    activeChunkIndex = chunkIdx
                                }

                                activeRaf!!.seek(chunkOffset)
                                activeRaf.write(netBuffer, bytesWritten, toWrite)
                                bytesWritten += toWrite
                            }

                            // 3. Advance High-Water Mark & wake readers
                            lock.withLock {
                                highWaterMark += read
                                evictOldChunksIfNeededLocked()
                                dataArrived.signalAll()
                            }
                        }
                    } finally {
                        try { activeRaf?.close() } catch (_: Throwable) {}
                    }
                }
            } catch (e: IOException) {
                lock.withLock {
                    if (!isClosed) {
                        streamError = e
                        dataArrived.signalAll()
                    }
                }
            }
        }
    }

    /**
     * Blocks until [requestedOffset] is downloaded or until a Cluster covering [pendingTargetTimeUs]
     * is indexed. Returns the aligned byte offset to read from.
     */
    @Throws(IOException::class, InterruptedException::class)
    fun awaitOffsetOrCluster(requestedOffset: Long): Long {
        lock.withLock {
            val targetTime = pendingTargetTimeUs
            pendingTargetTimeUs = -1L
            spaceAvailable.signalAll()

            while (!isClosed) {
                streamError?.let { throw it }

                // Check indexed clusters if seeking by time
                if (targetTime > 0 && clusterIndex.getMaxIndexedTimeUs() >= targetTime) {
                    _bridgeState.value = BridgeState.Idle
                    val floor = clusterIndex.findFloorCluster(targetTime)
                    if (floor != null) return floor.position
                }

                // Check byte offset if target offset is within downloaded stream
                if (targetTime <= 0 && highWaterMark >= requestedOffset) {
                    _bridgeState.value = BridgeState.Idle
                    return requestedOffset
                }

                // Check EOF
                if (totalContentLength in 1..requestedOffset) {
                    return requestedOffset
                }

                _bridgeState.value = BridgeState.FastForwarding(
                    currentOffset = highWaterMark,
                    targetOffset = requestedOffset.coerceAtLeast(highWaterMark),
                    speedBytesPerSec = 0L
                )

                dataArrived.await()
            }
            throw IOException("Sequential stream session closed")
        }
    }

    private val readLock = Any()
    private var cachedChunkIdx: Long = -1L
    private var cachedRaf: RandomAccessFile? = null

    /**
     * Reads bytes from the chunked disk store, waiting if reader hits [highWaterMark].
     */
    @Throws(IOException::class, InterruptedException::class)
    fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        lock.withLock {
            currentReadOffset = position
            spaceAvailable.signalAll()

            while (highWaterMark <= position && !isClosed) {
                streamError?.let { throw it }
                if (totalContentLength in 1..position) return -1 // EOF
                dataArrived.await()
            }
            if (isClosed) throw IOException("Sequential stream session closed")
        }

        val chunkIdx = position / CHUNK_SIZE
        val chunkOffset = position % CHUNK_SIZE
        val chunkFile = chunkFiles[chunkIdx]
            ?: throw IOException("Requested offset $position was evicted (floor=$evictionFloorOffset)")

        val availableInStream = highWaterMark - position
        val availableInChunk = CHUNK_SIZE - chunkOffset
        val toRead = minOf(length.toLong(), availableInStream, availableInChunk).toInt()

        // Reuse cached RandomAccessFile across sequential block reads to eliminate kernel open/close thrashing
        synchronized(readLock) {
            if (isClosed) throw IOException("Sequential stream session closed")
            val raf = if (cachedChunkIdx == chunkIdx && cachedRaf != null) {
                cachedRaf!!
            } else {
                try { cachedRaf?.close() } catch (_: Throwable) {}
                val newRaf = RandomAccessFile(chunkFile, "r")
                cachedRaf = newRaf
                cachedChunkIdx = chunkIdx
                newRaf
            }
            raf.seek(chunkOffset)
            return raf.read(buffer, offset, toRead)
        }
    }

    fun isOffsetAvailable(offset: Long): Boolean {
        if (offset < 0L) return false
        if (offset == 0L) return true
        val minValidOffset = if (offset < CHUNK_SIZE) 0L else evictionFloorOffset
        return offset in minValidOffset..(highWaterMark + maxForwardBridgeBytes)
    }

    fun isOffsetBuffered(offset: Long): Boolean {
        if (offset < 0L) return false
        val minValidOffset = if (offset < CHUNK_SIZE) 0L else evictionFloorOffset
        return offset in minValidOffset..highWaterMark
    }

    private fun getOrCreateChunkFile(chunkIdx: Long): File {
        return chunkFiles.getOrPut(chunkIdx) {
            File(sessionDir, "chunk_%05d.bin".format(chunkIdx))
        }
    }

    private fun evictOldChunksIfNeededLocked() {
        // Never evict Chunk 0 (EBML header, tracks, codec config)
        val safeEvictBeforeOffset = currentReadOffset - maxRewindWindowBytes
        val maxChunkIdxToEvict = (safeEvictBeforeOffset / CHUNK_SIZE) - 1
        if (maxChunkIdxToEvict <= 0) return

        val startChunk = (evictionFloorOffset / CHUNK_SIZE).coerceAtLeast(1L)
        for (idx in startChunk..maxChunkIdxToEvict) {
            val f = chunkFiles.remove(idx)
            try { f?.delete() } catch (_: Throwable) {}
            evictionFloorOffset = (idx + 1) * CHUNK_SIZE
        }

        synchronized(readLock) {
            if (cachedChunkIdx in startChunk..maxChunkIdxToEvict) {
                try { cachedRaf?.close() } catch (_: Throwable) {}
                cachedRaf = null
                cachedChunkIdx = -1L
            }
        }
    }

    fun release() {
        lock.withLock {
            isClosed = true
            dataArrived.signalAll()
            spaceAvailable.signalAll()
        }
        scope.cancel()
        synchronized(readLock) {
            try { cachedRaf?.close() } catch (_: Throwable) {}
            cachedRaf = null
            cachedChunkIdx = -1L
        }
        chunkFiles.values.forEach {
            try { it.delete() } catch (_: Throwable) {}
        }
        chunkFiles.clear()
        try {
            sessionDir.deleteRecursively()
        } catch (_: Throwable) {}
    }
}
