package com.euthopiar.download

import android.content.Context
import android.os.Environment
import com.euthopiar.core.network.DohDns
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resumable download engine supporting both direct progressive video files (MKV, MP4)
 * and HLS segmented video streams (M3U8).
 */
class DownloadEngine(private val context: Context) {

    private val client = com.euthopiar.core.network.PlayerPlaybackNetworking.playbackHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request()
            val b = req.newBuilder()
            if (req.header("User-Agent").isNullOrBlank()) {
                b.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            }
            if (req.header("Accept-Encoding").isNullOrBlank()) {
                b.header("Accept-Encoding", "identity")
            }
            chain.proceed(b.build())
        }
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun getDownloadDirectory(): File {
        val customPath = runBlocking {
            try {
                PreferencesRepository.getInstance(context).customDownloadDir.first()
            } catch (_: Exception) { null }
        }
        if (!customPath.isNullOrBlank()) {
            val customDir = File(customPath)
            if (customDir.exists() || customDir.mkdirs()) {
                return customDir
            }
        }
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun startOrResumeDownload(
        item: DownloadItem,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Float, speed: String) -> Unit,
        onCompleted: (finalFilePath: String, totalBytes: Long) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val existing = activeJobs[item.id]
        if (existing != null && existing.isActive) {
            return
        }
        activeJobs[item.id]?.cancel()

        val job = scope.launch {
            try {
                val cleanUrl = sanitizeUrl(item.streamUrl)
                val isTorrent = cleanUrl.startsWith("magnet:", ignoreCase = true) ||
                        cleanUrl.startsWith("torrent://", ignoreCase = true) ||
                        ((cleanUrl.length == 40 || cleanUrl.length == 64) && cleanUrl.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' })
                val isHls = !isTorrent && isHlsStream(cleanUrl, item.headers)

                when {
                    isTorrent -> downloadTorrentViaP2p(item, cleanUrl, onProgress, onCompleted, onError)
                    isHls -> downloadHlsStream(item, cleanUrl, onProgress, onCompleted, onError)
                    else -> downloadDirectFile(item, cleanUrl, onProgress, onCompleted, onError)
                }
            } catch (e: CancellationException) {
                // User paused or canceled
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Download failed: connection interrupted")
            } finally {
                activeJobs.remove(item.id)
            }
        }

        activeJobs[item.id] = job
    }

    private suspend fun downloadDirectFile(
        item: DownloadItem,
        cleanUrl: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Float, speed: String) -> Unit,
        onCompleted: (finalFilePath: String, totalBytes: Long) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
        val dir = getDownloadDirectory()
        val ext = inferExtension(cleanUrl)
        val safeFilename = sanitizeFilename(item.title)
        val partFile = File(dir, "${item.id}.part")
        val finalFile = File(dir, "$safeFilename-${item.id}.$ext")

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(cleanUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept-Encoding", "identity")

        // Apply item headers if matching or required
        applyHeaders(requestBuilder, cleanUrl, item.headers)

        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        var response = client.newCall(requestBuilder.build()).execute()
        var isPartial = response.code == 206
        var isOk = response.isSuccessful

        // Resiliency: If server rejected Range with 403 or 416, retry without Range from start
        if ((response.code == 403 || response.code == 416) && existingBytes > 0L) {
            response.close()
            partFile.delete()
            existingBytes = 0L
            val retryBuilder = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Encoding", "identity")
            applyHeaders(retryBuilder, cleanUrl, item.headers)
            response = client.newCall(retryBuilder.build()).execute()
            isPartial = response.code == 206
            isOk = response.isSuccessful
        }

        if (!isOk && !isPartial) {
            val code = response.code
            response.close()
            if (code == 401 || code == 403) {
                val refreshedUrl = tryRefreshToken(item, cleanUrl)
                if (refreshedUrl != null) {
                    downloadDirectFile(item.copy(streamUrl = refreshedUrl), refreshedUrl, onProgress, onCompleted, onError)
                    return@withContext
                }
            }
            if (code == 416) {
                if (partFile.exists() && partFile.length() > 0) {
                    partFile.renameTo(finalFile)
                    onCompleted(finalFile.absolutePath, finalFile.length())
                    return@withContext
                }
            }
            val errorMsg = when (code) {
                402 -> "Payment required or traffic limit reached (HTTP 402)"
                403 -> "Stream access forbidden or signature expired (HTTP 403)"
                401 -> "Stream unauthorized (HTTP 401)"
                else -> "HTTP Error $code: ${response.message}"
            }
            onError(errorMsg)
            return@withContext
        }

        val body = response.body ?: run {
            response.close()
            onError("Empty response body from server")
            return@withContext
        }

        // Safety check: If server responded with an HLS playlist instead of direct media
        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        val isPlaylistResponse = contentType.contains("mpegurl") ||
                contentType.contains("application/x-mpegurl") ||
                try {
                    response.peekBody(512).string().contains("#EXTM3U")
                } catch (_: Exception) { false }

        if (isPlaylistResponse) {
            response.close()
            downloadHlsStream(item, cleanUrl, onProgress, onCompleted, onError)
            return@withContext
        }

        // Safety check: Reject web pages (HTML) masquerading as direct video files
        if (contentType.contains("text/html") || contentType.contains("text/plain")) {
            response.close()
            partFile.delete()
            onError("Source returned a web page instead of a direct video stream.")
            return@withContext
        }

        val contentLength = body.contentLength()
        val totalBytes = if (isPartial && contentLength > 0) {
            existingBytes + contentLength
        } else if (contentLength > 0) {
            contentLength
        } else {
            item.totalBytes.takeIf { it > 0 } ?: 0L
        }

        val appendMode = isPartial && existingBytes > 0L
        if (!appendMode) {
            existingBytes = 0L
        }

        val inputStream = body.byteStream()
        val outputStream = java.io.BufferedOutputStream(FileOutputStream(partFile, appendMode), 256 * 1024)

        val buffer = ByteArray(128 * 1024)
        var bytesReadTotal = existingBytes
        var lastProgressUpdateMs = System.currentTimeMillis()
        var bytesReadSinceLastUpdate = 0L

        inputStream.use { input ->
            outputStream.use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    if (!coroutineContext.isActive) {
                        output.flush()
                        return@withContext
                    }

                    output.write(buffer, 0, read)
                    bytesReadTotal += read
                    bytesReadSinceLastUpdate += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastProgressUpdateMs

                    if (elapsed >= 500L) {
                        val speedBytesPerSec = if (elapsed > 0) {
                            (bytesReadSinceLastUpdate * 1000L) / elapsed
                        } else 0L

                        val speedStr = formatSpeed(speedBytesPerSec)
                        val percent = if (totalBytes > 0L) {
                            (bytesReadTotal.toFloat() / totalBytes * 100f).coerceIn(0f, 100f)
                        } else 0f

                        onProgress(bytesReadTotal, totalBytes, percent, speedStr)

                        lastProgressUpdateMs = now
                        bytesReadSinceLastUpdate = 0L
                    }
                }
                output.flush()
            }
        }

        if (finalFile.exists()) {
            finalFile.delete()
        }
        val renameSuccess = partFile.renameTo(finalFile)
        val targetFile = if (renameSuccess) finalFile else partFile

        // Safety check: Ensure downloaded file is valid and non-empty
        if (targetFile.length() < 100 * 1024L) {
            targetFile.delete()
            onError("Download failed: incomplete data received (${targetFile.length()} bytes)")
            return@withContext
        }

        if (totalBytes > 0L && targetFile.length() < (totalBytes * 0.95)) {
            targetFile.delete()
            onError("Download interrupted: received ${targetFile.length()} of $totalBytes bytes")
            return@withContext
        }

        onCompleted(targetFile.absolutePath, targetFile.length())
        }
    }

    private suspend fun isHlsStream(cleanUrl: String, headers: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        val lower = cleanUrl.lowercase()
        if (lower.contains(".m3u8") ||
            lower.contains("/content?v=") ||
            lower.contains("totallyacdn.org") ||
            lower.contains("hls.lol") ||
            lower.contains("vdrk.site") ||
            (lower.contains("workers.dev") && lower.contains("payload=")) ||
            lower.contains("playlist")
        ) {
            return@withContext true
        }

        // Fast probe via range GET to detect #EXTM3U
        try {
            val req = Request.Builder().url(cleanUrl)
            applyHeaders(req, cleanUrl, headers)
            req.header("Range", "bytes=0-511")
            client.newCall(req.build()).execute().use { resp ->
                val contentType = resp.header("Content-Type")?.lowercase() ?: ""
                if (contentType.contains("mpegurl") || contentType.contains("application/x-mpegurl")) {
                    return@withContext true
                }
                val peek = resp.peekBody(512).string()
                if (peek.contains("#EXTM3U")) {
                    return@withContext true
                }
            }
        } catch (_: Exception) {}
        false
    }

    private suspend fun downloadHlsStream(
        item: DownloadItem,
        cleanUrl: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Float, speed: String) -> Unit,
        onCompleted: (finalFilePath: String, totalBytes: Long) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val dir = getDownloadDirectory()
        val safeFilename = sanitizeFilename(item.title)
        val partFile = File(dir, "${item.id}.part")

        // Initial progress notification
        val initialTotal = if (item.totalBytes > 0L) item.totalBytes else 0L
        onProgress(0L, initialTotal, 0f, "Connecting...")

        // 1. Fetch playlist
        val reqBuilder = Request.Builder().url(cleanUrl)
        applyHeaders(reqBuilder, cleanUrl, item.headers)

        val resp = client.newCall(reqBuilder.build()).execute()
        if (!resp.isSuccessful) {
            val code = resp.code
            resp.close()
            onError("HTTP Error $code fetching HLS stream")
            return@withContext
        }

        val playlistText = resp.body?.string() ?: ""
        resp.close()
        if (playlistText.isBlank() || !playlistText.contains("#EXTM3U")) {
            onError("Invalid HLS playlist response")
            return@withContext
        }

        val baseUri = try { URI(cleanUrl) } catch (_: Exception) { null }

        // 2. If master playlist, resolve sub-playlist based on requested quality
        val (mediaPlaylistUrl, subPlaylistText) = if (playlistText.contains("#EXT-X-STREAM-INF")) {
            val lines = playlistText.lines()
            var selectedSubUrl: String? = null
            var bestBandwidth = -1L
            val target1080 = item.qualityLabel.contains("1080", ignoreCase = true)
            val target720 = item.qualityLabel.contains("720", ignoreCase = true)
            val target480 = item.qualityLabel.contains("480", ignoreCase = true)

            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF:")) {
                    val bw = Regex("""BANDWIDTH=(\d+)""").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    val res = Regex("""RESOLUTION=(\d+x\d+)""").find(line)?.groupValues?.get(1) ?: ""
                    val is1080 = res.contains("1080") || res.contains("800")
                    val is720 = res.contains("720") || res.contains("534")

                    var subLineUrl: String? = null
                    for (j in (i + 1) until lines.size) {
                        val subLine = lines[j].trim()
                        if (subLine.isNotBlank() && !subLine.startsWith("#")) {
                            subLineUrl = try { baseUri?.resolve(subLine)?.toString() ?: subLine } catch (_: Exception) { subLine }
                            break
                        }
                    }

                    if (subLineUrl != null) {
                        if ((target1080 && is1080) || (target720 && is720) || selectedSubUrl == null || (bw > bestBandwidth && !target480)) {
                            selectedSubUrl = subLineUrl
                            bestBandwidth = bw
                            if ((target1080 && is1080) || (target720 && is720)) break
                        }
                    }
                }
            }
            val targetSubUrl = selectedSubUrl ?: cleanUrl
            val subReq = Request.Builder().url(targetSubUrl)
            applyHeaders(subReq, targetSubUrl, item.headers)
            val subResp = client.newCall(subReq.build()).execute()
            if (!subResp.isSuccessful) {
                val code = subResp.code
                subResp.close()
                onError("HTTP Error $code fetching media segments")
                return@withContext
            }
            val text = subResp.body?.string() ?: ""
            subResp.close()
            targetSubUrl to text
        } else {
            cleanUrl to playlistText
        }

        val itemDir = File(dir, item.id)
        itemDir.mkdirs()

        val isFmp4 = subPlaylistText.contains("#EXT-X-MAP:") || subPlaylistText.contains(".m4s") || subPlaylistText.contains(".mp4")
        val segExt = if (isFmp4) "m4s" else "ts"

        // 3. Build offline playlist and extract media assets
        val subUri = try { URI(mediaPlaylistUrl) } catch (_: Exception) { null }
        val outPlaylist = StringBuilder()
        val segmentEntries = mutableListOf<Pair<String, String>>()
        var initMapUrl: String? = null
        val keyEntries = mutableListOf<Pair<String, String>>()
        val uriAttr = Regex("""URI=[\"']([^\"']+)[\"']""")

        var segCounter = 0
        var keyCounter = 0
        var mapCounter = 0

        for (line in subPlaylistText.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXT-X-KEY") && !trimmed.contains("METHOD=NONE") -> {
                    val match = uriAttr.find(trimmed)
                    if (match != null) {
                        val rawKeyUri = match.groupValues[1]
                        val resolvedKeyUrl = try { subUri?.resolve(rawKeyUri)?.toString() ?: rawKeyUri } catch (_: Exception) { rawKeyUri }
                        val localKeyName = "key_$keyCounter.bin"
                        keyCounter++
                        keyEntries.add(resolvedKeyUrl to localKeyName)
                        outPlaylist.appendLine(trimmed.replace(rawKeyUri, localKeyName))
                    } else {
                        outPlaylist.appendLine(trimmed)
                    }
                }
                trimmed.startsWith("#EXT-X-MAP:") -> {
                    val match = uriAttr.find(trimmed)
                    if (match != null) {
                        val rawMapUri = match.groupValues[1]
                        val resolvedMapUrl = try { subUri?.resolve(rawMapUri)?.toString() ?: rawMapUri } catch (_: Exception) { rawMapUri }
                        initMapUrl = resolvedMapUrl
                        val localMapName = "init_${mapCounter++}.mp4"
                        outPlaylist.appendLine(trimmed.replace(rawMapUri, localMapName))
                    } else {
                        outPlaylist.appendLine(trimmed)
                    }
                }
                trimmed.startsWith("#EXT-X-PLAYLIST-TYPE") || trimmed.startsWith("#EXT-X-ENDLIST") -> {
                    // Handled at finalize
                }
                trimmed.isBlank() -> {
                    // skip blank lines
                }
                trimmed.startsWith("#") -> {
                    outPlaylist.appendLine(trimmed)
                }
                else -> {
                    val resolvedSegUrl = try { subUri?.resolve(trimmed)?.toString() ?: trimmed } catch (_: Exception) { trimmed }
                    val localSegName = "seg_%05d.$segExt".format(segCounter++)
                    segmentEntries.add(resolvedSegUrl to localSegName)
                    outPlaylist.appendLine(localSegName)
                }
            }
        }

        if (segmentEntries.isEmpty()) {
            onError("No video segments found in HLS stream")
            return@withContext
        }

        // Download init map if present
        initMapUrl?.let { mapUrl ->
            val initFile = File(itemDir, "init_0.mp4")
            if (!initFile.exists() || initFile.length() == 0L) {
                try {
                    val initReq = Request.Builder().url(mapUrl)
                    applyHeaders(initReq, mapUrl, item.headers)
                    client.newCall(initReq.build()).execute().use { initResp ->
                        if (initResp.isSuccessful) {
                            initResp.body?.bytes()?.let { initFile.writeBytes(it) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Download keys if present
        for ((keyUrl, keyName) in keyEntries) {
            val keyFile = File(itemDir, keyName)
            if (!keyFile.exists() || keyFile.length() == 0L) {
                try {
                    val keyReq = Request.Builder().url(keyUrl)
                    applyHeaders(keyReq, keyUrl, item.headers)
                    client.newCall(keyReq.build()).execute().use { keyResp ->
                        if (keyResp.isSuccessful) {
                            keyResp.body?.bytes()?.let { keyFile.writeBytes(it) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        val totalSegments = segmentEntries.size
        var totalBytesDownloaded = 0L

        // Pre-count already downloaded segments for resume
        for ((_, segName) in segmentEntries) {
            val segFile = File(itemDir, segName)
            if (segFile.exists()) {
                totalBytesDownloaded += segFile.length()
            }
        }

        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val downloadedBytesAtomic = java.util.concurrent.atomic.AtomicLong(totalBytesDownloaded)
        val bytesSinceUpdateAtomic = java.util.concurrent.atomic.AtomicLong(0L)
        val progressLock = Any()
        var lastProgressMs = System.currentTimeMillis()
        val errorSignal = java.util.concurrent.atomic.AtomicReference<String?>(null)

        val semaphore = kotlinx.coroutines.sync.Semaphore(5)

        coroutineScope {
            val jobs = segmentEntries.mapIndexed { idx, (rawSegUrl, localSegName) ->
                launch(Dispatchers.IO) {
                    if (!isActive || errorSignal.get() != null) return@launch

                    val segFile = File(itemDir, localSegName)
                    if (segFile.exists() && segFile.length() > 0L) {
                        completedCount.incrementAndGet()
                        return@launch
                    }

                    semaphore.withPermit {
                        if (!isActive || errorSignal.get() != null) return@withPermit

                        val segUrl = sanitizeUrl(rawSegUrl)
                        var attempts = 0
                        var success = false

                        while (attempts < 3 && !success && isActive && errorSignal.get() == null) {
                            attempts++
                            try {
                                val segReq = Request.Builder().url(segUrl)
                                applyHeaders(segReq, segUrl, item.headers)

                                client.newCall(segReq.build()).execute().use { segResp ->
                                    if (segResp.isSuccessful) {
                                        val body = segResp.body ?: throw java.io.IOException("Empty segment body")
                                        val partSeg = File(itemDir, "$localSegName.part")
                                        partSeg.outputStream().use { fos ->
                                            body.byteStream().copyTo(fos)
                                        }
                                        if (partSeg.length() > 0L) {
                                            partSeg.renameTo(segFile)
                                            val downloadedSize = segFile.length()
                                            downloadedBytesAtomic.addAndGet(downloadedSize)
                                            bytesSinceUpdateAtomic.addAndGet(downloadedSize)
                                            success = true
                                        } else {
                                            partSeg.delete()
                                            throw java.io.IOException("Empty segment file")
                                        }
                                    } else {
                                        throw java.io.IOException("HTTP ${segResp.code} fetching segment $idx from $segUrl")
                                    }
                                }
                            } catch (e: Exception) {
                                if (attempts >= 3) {
                                    errorSignal.compareAndSet(null, "Failed to download segment ${idx + 1}/$totalSegments: ${e.localizedMessage}")
                                    return@withPermit
                                }
                                delay(400L * attempts)
                            }
                        }

                        val done = completedCount.incrementAndGet()
                        val now = System.currentTimeMillis()
                        synchronized(progressLock) {
                            val elapsed = now - lastProgressMs
                            if (elapsed >= 300L || done == totalSegments) {
                                val bytesSince = bytesSinceUpdateAtomic.getAndSet(0L)
                                val speedBytesPerSec = if (elapsed > 0) (bytesSince * 1000L) / elapsed else 0L
                                val speedStr = formatSpeed(speedBytesPerSec)
                                val currentTotal = downloadedBytesAtomic.get()
                                val percent = ((done.toFloat() / totalSegments) * 100f).coerceIn(0f, 100f)
                                val estimatedTotal = if (done > 0) {
                                    (currentTotal / done) * totalSegments
                                } else if (item.totalBytes > 0L) {
                                    item.totalBytes
                                } else {
                                    currentTotal
                                }

                                onProgress(currentTotal, estimatedTotal, percent, speedStr)
                                lastProgressMs = now
                            }
                        }
                    }
                }
            }
            jobs.joinAll()
        }

        val finalError = errorSignal.get()
        if (finalError != null) {
            onError(finalError)
            return@withContext
        }
        totalBytesDownloaded = downloadedBytesAtomic.get()

        // Finalize index.m3u8 playlist
        if (!outPlaylist.contains("#EXT-X-ENDLIST")) {
            outPlaylist.appendLine("#EXT-X-ENDLIST")
        }
        val rawContent = outPlaylist.toString()
        val finalPlaylistContent = if (!rawContent.contains("#EXT-X-PLAYLIST-TYPE")) {
            rawContent.replaceFirst(Regex("#EXTM3U\\r?\\n"), "#EXTM3U\n#EXT-X-PLAYLIST-TYPE:VOD\n")
        } else {
            rawContent
        }

        val playlistFile = File(itemDir, "index.m3u8")
        val tmpPlaylist = File(itemDir, "index.m3u8.tmp")
        tmpPlaylist.writeText(finalPlaylistContent)
        tmpPlaylist.renameTo(playlistFile)

        onCompleted(playlistFile.absolutePath, totalBytesDownloaded)
    }

    private fun applyHeaders(builder: Request.Builder, url: String, itemHeaders: Map<String, String>) {
        val lower = url.lowercase()
        val isPreSignedCloud = lower.contains("r2.cloudflarestorage.com") ||
                lower.contains(".r2.dev") ||
                lower.contains("googleusercontent.com") ||
                lower.contains("storage.googleapis.com") ||
                lower.contains(".amazonaws.com")
        val stripAuth = com.euthopiar.core.network.PlayerPlaybackNetworking.shouldStripAuthorization(url)

        // Ensure default User-Agent
        builder.header("User-Agent", itemHeaders["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        if (isPreSignedCloud || stripAuth) {
            // Cloudflare R2, AWS S3, and GCS pre-signed URLs break with HTTP 403/400 when unsolicited Referer, Origin, or Authorization headers are attached
            itemHeaders.forEach { (k, v) ->
                val isExcluded = k.equals("Referer", ignoreCase = true) ||
                        k.equals("Origin", ignoreCase = true) ||
                        (stripAuth && k.equals("Authorization", ignoreCase = true))
                if (!isExcluded) {
                    builder.header(k, v)
                }
            }
            return
        }

        // Apply all headers passed from the plugin/stream source
        itemHeaders.forEach { (k, v) -> builder.header(k, v) }

        // Fallback: if Referer is not provided, derive default only for hosts that strictly require it
        if (!itemHeaders.keys.any { it.equals("Referer", ignoreCase = true) } && url.startsWith("http")) {
            val referer = when {
                lower.contains("valentine.guru") || lower.contains("cocktail.beer") || lower.contains("pixel.hubcdn") || lower.contains("gamerxyt") -> "https://gamerxyt.com/"
                lower.contains("zenoak") || lower.contains("movy") || lower.contains("cinejoy") || lower.contains("wing.st") || lower.contains("cheaptruckrepairs") || lower.contains("bright67") -> "https://cinejoy.pk/"
                lower.contains("driveseed.org") -> "https://driveseed.org/"
                lower.contains("pixeldrain.com") -> "https://pixeldrain.com/"
                lower.contains("hls.lol") -> "https://stream.hls.lol/"
                else -> null
            }
            if (referer != null) {
                builder.header("Referer", referer)
            }
        }
    }

    private suspend fun tryRefreshToken(item: DownloadItem, staleUrl: String): String? {
        val scraperId = item.scraperId ?: item.provider ?: return null
        return try {
            val refreshable = com.euthopiar.core.streams.RefreshableSource(
                scraperId = scraperId,
                mediaType = item.mediaType ?: if (item.isTvSeries) "tv" else "movie",
                videoId = item.mediaId,
                season = item.seasonNumber,
                episode = item.episodeNumber,
                quality = item.qualityLabel,
                initialUrl = staleUrl,
                initialHeaders = item.headers
            )
            val newUrl = refreshable.refreshOnExpiredToken(staleUrl)
            if (newUrl != null && newUrl != staleUrl) {
                DownloadRepository.getInstance(context).updateDownloadUrl(item.id, newUrl, refreshable.currentHeaders)
                newUrl
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("DownloadEngine", "Token refresh error: ${e.message}")
            null
        }
    }

    private suspend fun downloadTorrentViaP2p(
        item: DownloadItem,
        cleanUrl: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Float, speed: String) -> Unit,
        onCompleted: (finalFilePath: String, totalBytes: Long) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val p2pRequest = com.euthopiar.core.p2p.extractP2pStreamRequest(cleanUrl)
            ?: run {
                onError("Invalid torrent magnet or info hash")
                return@withContext
            }

        com.euthopiar.core.p2p.P2pStreamingEngine.initialize(context)

        val loopbackUrl = try {
            com.euthopiar.core.p2p.P2pStreamingEngine.startStream(p2pRequest)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            onError("Failed to start P2P engine: ${e.message}")
            return@withContext
        }

        val dir = getDownloadDirectory()
        val safeFilename = sanitizeFilename(item.title)
        val ext = inferExtension(p2pRequest.filename ?: item.title)
        val finalFile = File(dir, "$safeFilename-${item.id}.$ext")
        val partFile = File(dir, "${item.id}.part")

        try {
            val reqBuilder = Request.Builder()
                .url(loopbackUrl)
                .header("User-Agent", "Euthopiar/1.0")

            var existingBytes = if (partFile.exists()) partFile.length() else 0L
            if (existingBytes > 0L) {
                reqBuilder.header("Range", "bytes=$existingBytes-")
            }

            var resp = client.newCall(reqBuilder.build()).execute()
            if ((resp.code == 403 || resp.code == 416) && existingBytes > 0L) {
                resp.close()
                partFile.delete()
                existingBytes = 0L
                val retryReq = Request.Builder().url(loopbackUrl).header("User-Agent", "Euthopiar/1.0").build()
                resp = client.newCall(retryReq).execute()
            }

            if (!resp.isSuccessful && resp.code != 206) {
                val code = resp.code
                resp.close()
                com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
                onError("P2P Engine error HTTP $code")
                return@withContext
            }

            val body = resp.body ?: run {
                resp.close()
                com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
                onError("Empty stream from P2P engine")
                return@withContext
            }

            val isPartial = resp.code == 206
            val contentLength = body.contentLength()
            val totalBytes = if (isPartial && contentLength > 0) {
                existingBytes + contentLength
            } else if (contentLength > 0) {
                contentLength
            } else {
                item.totalBytes.takeIf { it > 0 } ?: 0L
            }

            val appendMode = isPartial && existingBytes > 0L
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(partFile, appendMode)

            val buffer = ByteArray(64 * 1024)
            var bytesReadTotal = existingBytes
            var lastUpdateMs = System.currentTimeMillis()
            var bytesReadSinceUpdate = 0L

            outputStream.use { out ->
                inputStream.use { inp ->
                    while (currentCoroutineContext().isActive) {
                        val read = inp.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        bytesReadTotal += read
                        bytesReadSinceUpdate += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateMs
                        if (elapsed >= 500) {
                            val speedBytesPerSec = if (elapsed > 0) (bytesReadSinceUpdate * 1000L) / elapsed else 0L
                            val percent = if (totalBytes > 0) {
                                ((bytesReadTotal.toDouble() / totalBytes) * 100.0).toFloat().coerceIn(0f, 99.9f)
                            } else 0f

                            val p2pState = com.euthopiar.core.p2p.P2pStreamingEngine.state.value
                            val speedStr = if (p2pState is com.euthopiar.core.p2p.P2pStreamingState.Streaming && p2pState.downloadSpeed > 0L) {
                                "${com.euthopiar.core.p2p.formatP2pSpeed(p2pState.downloadSpeed)} • ${p2pState.peers} peers"
                            } else {
                                formatSpeed(speedBytesPerSec)
                            }

                            onProgress(bytesReadTotal, totalBytes, percent, speedStr)
                            lastUpdateMs = now
                            bytesReadSinceUpdate = 0L
                        }
                    }
                    out.flush()
                }
            }

            // Deduplicate with engine payload directory to prevent double writes
            val payloadDir = File(context.cacheDir, "nuvio-engine/payload")
            val reqFilename = p2pRequest.filename
            val matchingPayload = try {
                payloadDir.listFiles()?.firstOrNull { file ->
                    file.name.contains(p2pRequest.infoHash, ignoreCase = true) ||
                    (!reqFilename.isNullOrBlank() && file.name.contains(reqFilename, ignoreCase = true)) ||
                    (totalBytes > 0L && file.length() == totalBytes)
                }
            } catch (_: Exception) { null }

            if (finalFile.exists()) finalFile.delete()

            val resolvedFile = if (matchingPayload != null && matchingPayload.exists() && matchingPayload.length() >= totalBytes * 0.95) {
                // Atomically move from payload directory to avoid double writes
                try {
                    java.nio.file.Files.move(
                        matchingPayload.toPath(),
                        finalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                    if (partFile.exists()) partFile.delete()
                    finalFile
                } catch (_: Exception) {
                    if (partFile.exists() && partFile.length() > 0L) {
                        partFile.renameTo(finalFile)
                        try { matchingPayload.delete() } catch (_: Exception) {}
                        finalFile
                    } else matchingPayload
                }
            } else if (partFile.exists() && partFile.length() > 0L) {
                partFile.renameTo(finalFile)
                try { matchingPayload?.delete() } catch (_: Exception) {}
                finalFile
            } else null

            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()

            if (resolvedFile != null && resolvedFile.exists() && resolvedFile.length() > 0L) {
                onCompleted(resolvedFile.absolutePath, resolvedFile.length())
            } else {
                onError("P2P download incomplete or empty")
            }
        } catch (e: CancellationException) {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
            throw e
        } catch (e: Exception) {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
            onError("P2P download interrupted: ${e.message}")
        }
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        try {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
        } catch (_: Exception) {}
    }

    fun removeDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        try {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
        } catch (_: Exception) {}

        try {
            val dir = getDownloadDirectory()
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.name.contains(id)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore file deletion errors
        }
    }

    fun isDownloading(id: String): Boolean {
        return activeJobs[id]?.isActive == true
    }

    private fun sanitizeUrl(url: String): String {
        if (!url.contains(" ") && !url.contains("[") && !url.contains("]") && !url.contains("|") && !url.contains("^") && !url.contains("`") && !url.contains("{") && !url.contains("}")) {
            return url
        }
        return try {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd != -1) {
                val scheme = url.substring(0, schemeEnd + 3)
                val rest = url.substring(schemeEnd + 3)
                val slashIdx = rest.indexOf('/')
                if (slashIdx != -1) {
                    val host = rest.substring(0, slashIdx)
                    val pathAndQuery = rest.substring(slashIdx)
                    val cleanPathAndQuery = pathAndQuery
                        .replace(" ", "%20")
                        .replace("[", "%5B")
                        .replace("]", "%5D")
                        .replace("{", "%7B")
                        .replace("}", "%7D")
                        .replace("|", "%7C")
                        .replace("^", "%5E")
                        .replace("`", "%60")
                    scheme + host + cleanPathAndQuery
                } else {
                    url.replace(" ", "%20")
                }
            } else {
                url.replace(" ", "%20")
            }
        } catch (_: Exception) {
            url.replace(" ", "%20")
        }
    }

    private fun inferExtension(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".mkv") -> "mkv"
            lower.contains(".mp4") -> "mp4"
            lower.contains(".webm") -> "webm"
            lower.contains(".ts") -> "ts"
            else -> "mkv"
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(40)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 KB/s"
        val mb = bytesPerSec / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format("%.1f MB/s", mb)
        } else {
            val kb = bytesPerSec / 1024.0
            String.format("%.0f KB/s", kb)
        }
    }
}
