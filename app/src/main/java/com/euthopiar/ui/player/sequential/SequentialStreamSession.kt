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
    val context: Context,
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
    @Volatile var pendingTargetByteOffset: Long = -1L
    @Volatile var acceptsRanges: Boolean = false
    @Volatile var isEof: Boolean = false
    @Volatile var isClosed: Boolean = false
        private set
    @Volatile var streamError: IOException? = null
        private set

    @Volatile var detectedContainer: com.euthopiar.core.model.StreamContainer = com.euthopiar.core.model.StreamContainer.AUTO
        private set
    private val containerCompletable = kotlinx.coroutines.CompletableDeferred<com.euthopiar.core.model.StreamContainer>()

    suspend fun awaitDetectedContainer(timeoutMs: Long = 8000L): com.euthopiar.core.model.StreamContainer {
        return try {
            withTimeout(timeoutMs) {
                containerCompletable.await()
            }
        } catch (_: Throwable) {
            detectedContainer
        }
    }

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

    private fun inspectInitialBytesForContainer(bytes: ByteArray, length: Int) {
        if (containerCompletable.isCompleted || length < 4) return
        val detected = when {
            // EBML ID: 0x1A 0x45 0xDF 0xA3
            length >= 4 && (bytes[0].toInt() and 0xFF == 0x1A) &&
                    (bytes[1].toInt() and 0xFF == 0x45) &&
                    (bytes[2].toInt() and 0xFF == 0xDF) &&
                    (bytes[3].toInt() and 0xFF == 0xA3) -> {
                com.euthopiar.core.model.StreamContainer.MKV
            }
            // MP4 box type at bytes 4..7: "ftyp", "moov", "mdat", "free", "wide", "skip"
            length >= 8 && run {
                val boxType = String(bytes, 4, 4, Charsets.US_ASCII)
                boxType == "ftyp" || boxType == "moov" || boxType == "mdat" ||
                        boxType == "free" || boxType == "wide" || boxType == "skip"
            } -> {
                com.euthopiar.core.model.StreamContainer.MP4
            }
            // HLS playlist "#EXTM3U"
            length >= 7 && String(bytes, 0, min(length, 16), Charsets.UTF_8).startsWith("#EXTM3U") -> {
                com.euthopiar.core.model.StreamContainer.HLS
            }
            else -> com.euthopiar.core.model.StreamContainer.AUTO
        }
        detectedContainer = detected
        containerCompletable.complete(detected)
    }

    private fun startSinglePipeDownload() {
        scope.launch {
            var retryCount = 0
            val maxRetries = 3

            while (isActive && !isClosed) {
                val targetUrl = SequentialStreamManager.getPinnedUrl(url) ?: url
                val requestBuilder = Request.Builder().url(targetUrl)
                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                if (headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
                    requestBuilder.header("User-Agent", com.euthopiar.core.network.AppUserAgent.get(context))
                }

                val currentHwm = highWaterMark
                if (currentHwm > 0L) {
                    requestBuilder.header("Range", "bytes=$currentHwm-")
                } else if (headers.keys.none { it.equals("Range", ignoreCase = true) }) {
                    requestBuilder.header("Range", "bytes=0-")
                }

                var activeChunkIndex = -1L
                var activeRaf: RandomAccessFile? = null

                try {
                    okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                        val code = response.code
                        if (currentHwm > 0L) {
                            if (code != 206) {
                                throw IOException("Sequential stream resume failed: expected 206 Partial Content, got $code")
                            }
                        } else {
                            if (!response.isSuccessful && code != 206) {
                                throw IOException("Sequential stream HTTP $code: ${response.message}")
                            }
                        }

                        val finalUrl = response.request.url.toString()
                        resolvedUrl = finalUrl
                        SequentialStreamManager.pinResolvedUrl(url, finalUrl)

                        val acceptRangesHeader = response.header("Accept-Ranges")
                        acceptsRanges = (code == 206 || acceptRangesHeader?.equals("bytes", ignoreCase = true) == true)

                        if (currentHwm == 0L) {
                            var cl = response.body?.contentLength() ?: -1L
                            if (cl <= 0L) {
                                val crHeader = response.header("Content-Range")
                                if (crHeader != null) {
                                    val totalStr = crHeader.substringAfterLast('/', "")
                                    totalStr.toLongOrNull()?.let { cl = it }
                                }
                            }
                            totalContentLength = cl
                        } else {
                            val crHeader = response.header("Content-Range")
                            if (crHeader != null && totalContentLength <= 0L) {
                                val totalStr = crHeader.substringAfterLast('/', "")
                                totalStr.toLongOrNull()?.let { totalContentLength = it }
                            }
                        }

                        val inputStream = response.body?.byteStream()
                            ?: throw IOException("Empty response body from sequential stream")

                        val netBuffer = ByteArray(256 * 1024) // 256 KB network buffer
                        retryCount = 0 // Reset retries on successful connection
                        var firstChunkProcessed = (currentHwm > 0L)

                        while (isActive && !isClosed) {
                            // Backpressure control: pause high-speed pulling if buffered far ahead
                            lock.withLock {
                                while (!isClosed &&
                                    pendingTargetTimeUs == -1L &&
                                    pendingTargetByteOffset == -1L &&
                                    highWaterMark - currentReadOffset > maxReadAheadBytes
                                ) {
                                    spaceAvailable.await()
                                }
                            }

                            val read = inputStream.read(netBuffer)
                            if (read == -1) {
                                lock.withLock {
                                    isEof = true
                                    dataArrived.signalAll()
                                }
                                if (!containerCompletable.isCompleted) {
                                    containerCompletable.complete(detectedContainer)
                                }
                                return@launch
                            }

                            if (!firstChunkProcessed) {
                                inspectInitialBytesForContainer(netBuffer, read)
                                firstChunkProcessed = true
                            }

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
                    }
                } catch (e: IOException) {
                    try { activeRaf?.close() } catch (_: Throwable) {}
                    if (isClosed) return@launch

                    val isTransient = e is java.net.SocketTimeoutException ||
                            e is java.net.SocketException ||
                            e.message?.contains("unexpected end of stream", ignoreCase = true) == true ||
                            e.message?.contains("stream was reset", ignoreCase = true) == true ||
                            e.message?.contains("connection reset", ignoreCase = true) == true

                    if (highWaterMark > 0L && retryCount < maxRetries && isTransient) {
                        retryCount++
                        android.util.Log.w(TAG, "Transient error on sequential stream at hwm=$highWaterMark, retry $retryCount/$maxRetries: ${e.message}")
                        delay(500L * retryCount)
                        continue
                    }

                    if (!containerCompletable.isCompleted) {
                        containerCompletable.complete(detectedContainer)
                    }
                    lock.withLock {
                        if (!isClosed) {
                            streamError = e
                            dataArrived.signalAll()
                        }
                    }
                    return@launch
                } finally {
                    try { activeRaf?.close() } catch (_: Throwable) {}
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
            pendingTargetByteOffset = requestedOffset
            spaceAvailable.signalAll()

            try {
                while (!isClosed) {
                    streamError?.let { throw it }

                    // Ensure response headers and initial data have arrived before returning 0L
                    if (highWaterMark == 0L && !isEof) {
                        dataArrived.await()
                        continue
                    }

                    // Check indexed clusters if seeking by time
                    if (targetTime > 0 && clusterIndex.getMaxIndexedTimeUs() >= targetTime) {
                        _bridgeState.value = BridgeState.Idle
                        val floor = clusterIndex.findFloorCluster(targetTime)
                        if (floor != null) {
                            val resolved = floor.position
                            currentReadOffset = resolved
                            return resolved
                        }
                    }

                    // Check byte offset if target offset is within downloaded stream
                    if (targetTime <= 0 && highWaterMark >= requestedOffset && (highWaterMark > 0L || isEof)) {
                        _bridgeState.value = BridgeState.Idle
                        currentReadOffset = requestedOffset
                        return requestedOffset
                    }

                    // Check EOF
                    if (isEof || (totalContentLength in 1..requestedOffset)) {
                        _bridgeState.value = BridgeState.Idle
                        val resolved = if (totalContentLength in 1..requestedOffset) requestedOffset else highWaterMark
                        currentReadOffset = resolved
                        return resolved
                    }

                    _bridgeState.value = BridgeState.FastForwarding(
                        currentOffset = highWaterMark,
                        targetOffset = requestedOffset.coerceAtLeast(highWaterMark),
                        speedBytesPerSec = 0L
                    )

                    dataArrived.await()
                }
                throw IOException("Sequential stream session closed")
            } finally {
                pendingTargetTimeUs = -1L
                pendingTargetByteOffset = -1L
                spaceAvailable.signalAll()
            }
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
                if (isEof || (totalContentLength in 1..position)) return -1
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
        if (acceptsRanges && (totalContentLength <= 0L || offset <= totalContentLength)) return true
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
        val anchorOffset = if (pendingTargetByteOffset > 0L || pendingTargetTimeUs > 0L) {
            highWaterMark
        } else {
            currentReadOffset
        }
        val safeEvictBeforeOffset = anchorOffset - maxRewindWindowBytes
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
        if (!containerCompletable.isCompleted) {
            containerCompletable.complete(detectedContainer)
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
