package com.euthopiar.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Environment
import android.os.StatFs
import androidx.media3.datasource.DataSource
import com.euthopiar.core.model.DownloadOption
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.streams.StreamItem
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class DownloadRepository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val metadataFile = File(context.filesDir, "download_metadata.json")

    private val preferencesRepository = PreferencesRepository.getInstance(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    @Volatile
    private var isWifiOnlyDownloads = false

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val engine = DownloadEngine(context)

    init {
        loadPersistedMetadata()
        observeWifiOnlyPreference()
        registerNetworkCallback()
    }

    private fun observeWifiOnlyPreference() {
        scope.launch {
            try {
                preferencesRepository.wifiOnlyDownloads.collect { wifiOnly ->
                    isWifiOnlyDownloads = wifiOnly
                    if (!wifiOnly) {
                        resumeWaitingDownloads()
                    } else if (isNetworkMeteredOrCellular()) {
                        pauseActiveDownloadsForWifi()
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    handleNetworkStateChange()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    handleNetworkStateChange()
                }

                override fun onLost(network: Network) {
                    handleNetworkStateChange()
                }
            })
        } catch (_: Throwable) {
            // Ignore in restricted sandbox or unit test
        }
    }

    internal fun isNetworkMeteredOrCellular(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return true

        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        if (hasVpn) {
            val directWifiOrEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            var underlyingWifiOrEthernet = false
            var underlyingCellular = false
            try {
                for (net in cm.allNetworks) {
                    val c = cm.getNetworkCapabilities(net) ?: continue
                    if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                    if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        underlyingWifiOrEthernet = true
                    }
                    if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        underlyingCellular = true
                    }
                }
            } catch (_: Throwable) {
                // Ignore sandbox / restricted access
            }

            if (directWifiOrEthernet || underlyingWifiOrEthernet) {
                return false
            }
            if (underlyingCellular) {
                return true
            }
            if (!cm.isActiveNetworkMetered) {
                return false
            }
            return true
        }

        val hasWifiOrEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val isMetered = cm.isActiveNetworkMetered ||
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        return isCellular || isMetered || !hasWifiOrEthernet
    }

    internal fun handleNetworkStateChange() {
        if (!isWifiOnlyEnabled()) return
        if (!isNetworkMeteredOrCellular()) {
            resumeWaitingDownloads()
        } else {
            pauseActiveDownloadsForWifi()
        }
    }

    internal fun isWifiOnlyEnabled(): Boolean {
        return if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            isWifiOnlyDownloads
        } else {
            try {
                kotlinx.coroutines.runBlocking {
                    preferencesRepository.wifiOnlyDownloads.first()
                }
            } catch (_: Throwable) {
                isWifiOnlyDownloads
            }
        }
    }

    private fun pauseActiveDownloadsForWifi() {
        val active = _downloads.value.filter { it.state == DownloadState.DOWNLOADING }
        for (item in active) {
            engine.pauseDownload(item.id)
            _downloads.update { list ->
                list.map {
                    if (it.id == item.id) it.copy(state = DownloadState.PAUSED, failureReason = "Waiting for Wi-Fi", speed = null)
                    else it
                }
            }
        }
        if (active.isNotEmpty()) {
            persistMetadata()
            checkStopService()
        }
    }

    private fun resumeWaitingDownloads() {
        val waiting = _downloads.value.filter {
            it.state == DownloadState.PAUSED && it.failureReason == "Waiting for Wi-Fi"
        }
        for (item in waiting) {
            resumeDownload(item.id)
        }
    }

    private fun loadPersistedMetadata() {
        scope.launch {
            try {
                if (metadataFile.exists()) {
                    val content = metadataFile.readText()
                    if (content.isNotBlank()) {
                        val items = json.decodeFromString<List<DownloadItem>>(content)
                        // Verify existing local files for completed items
                        val verifiedItems = items.map { item ->
                            if (item.state == DownloadState.COMPLETED && item.localFilePath != null) {
                                val f = File(item.localFilePath)
                                if (!f.exists()) {
                                    item.copy(state = DownloadState.FAILED, failureReason = "File removed from storage")
                                } else {
                                    item
                                }
                            } else {
                                item
                            }
                        }
                        _downloads.value = verifiedItems
                    }
                }
            } catch (e: Exception) {
                // Ignore metadata parse error
            }
        }
    }

    private fun persistMetadata() {
        scope.launch {
            try {
                val data = json.encodeToString(_downloads.value)
                metadataFile.writeText(data)
            } catch (e: Exception) {
                // Ignore persist error
            }
        }
    }

    fun enqueueFromStream(
        detail: MediaDetail,
        episode: EpisodeItem? = null,
        stream: StreamItem
    ): Boolean {
        var downloadUrl = stream.playableDirectUrl
            ?: (if (stream.isTorrentStream) stream.effectiveMagnetOrHash else null)
            ?: return false

        if (!stream.isTorrentStream && !stream.isSupportedDownloadUrl()) return false

        if (stream.isTorrentStream && !downloadUrl.startsWith("http", ignoreCase = true)) {
            val s = episode?.seasonNumber
            val epNum = episode?.episodeNumber
            val f = stream.fileIdx
            val extraParams = mutableListOf<String>()
            if (f != null && f >= 0 && !downloadUrl.contains("index=", ignoreCase = true) && !downloadUrl.contains("fileidx=", ignoreCase = true) && !downloadUrl.contains("so=", ignoreCase = true)) {
                extraParams.add("index=$f")
            }
            if (s != null && s > 0 && !downloadUrl.contains("season=", ignoreCase = true)) {
                extraParams.add("season=$s")
            }
            if (epNum != null && epNum > 0 && !downloadUrl.contains("episode=", ignoreCase = true)) {
                extraParams.add("episode=$epNum")
            }
            if (extraParams.isNotEmpty()) {
                val delim = if (downloadUrl.contains("?")) "&" else "?"
                downloadUrl = "$downloadUrl$delim${extraParams.joinToString("&")}"
            }
        }

        val option = DownloadOption(
            title = stream.name ?: detail.title,
            quality = stream.quality ?: if (stream.isTorrentStream) "Torrent P2P" else "Direct Stream",
            size = stream.size ?: "~2.0 GB",
            url = downloadUrl,
            source = stream.sourceName ?: stream.addonName,
            provider = stream.addonName,
            headers = stream.headers,
            isM3u8 = downloadUrl.contains(".m3u8", ignoreCase = true),
            container = stream.container,
            trackers = stream.trackers
        )

        startDownload(detail, episode, option)
        return true
    }

    fun updateDownloadUrl(id: String, newUrl: String, newHeaders: Map<String, String>) {
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(streamUrl = newUrl, headers = newHeaders) else it }
        }
        persistMetadata()
    }

    fun isDownloading(id: String): Boolean = engine.isDownloading(id)

    fun startDownload(
        detail: MediaDetail,
        episode: EpisodeItem? = null,
        option: DownloadOption
    ) {
        val id = if (episode != null) {
            "${detail.id}_s${episode.seasonNumber}e${episode.episodeNumber}"
        } else {
            detail.id
        }

        if (engine.isDownloading(id)) {
            return
        }

        val cleanDetailTitle = detail.title.replace('+', ' ').trim()
        val cleanEpisodeTitle = episode?.title?.replace('+', ' ')?.trim()
        val subtitle = if (episode != null) {
            "Season ${episode.seasonNumber} • Episode ${episode.episodeNumber}${if (!cleanEpisodeTitle.isNullOrBlank()) " - $cleanEpisodeTitle" else ""}"
        } else {
            detail.year?.let { "$it • ${detail.duration ?: "Movie"}" }
        }

        val qualityLabel = "${option.quality} (${option.size})"
        val estimatedTotal = parseEstimatedBytes(option.size)

        val waitingForWifi = isWifiOnlyEnabled() && isNetworkMeteredOrCellular()

        val item = DownloadItem(
            id = id,
            mediaId = detail.id,
            episodeId = episode?.data ?: episode?.id,
            title = if (episode != null && !cleanEpisodeTitle.isNullOrBlank()) "$cleanDetailTitle: $cleanEpisodeTitle" else cleanDetailTitle,
            subtitle = subtitle,
            backdropUrl = detail.backdropUrl ?: detail.posterUrl ?: episode?.thumbnail,
            posterUrl = detail.posterUrl ?: detail.backdropUrl,
            isTvSeries = detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME,
            qualityLabel = qualityLabel,
            state = if (waitingForWifi) DownloadState.PAUSED else DownloadState.DOWNLOADING,
            failureReason = if (waitingForWifi) "Waiting for Wi-Fi" else null,
            streamUrl = option.url,
            headers = option.headers,
            totalBytes = estimatedTotal,
            provider = option.provider,
            scraperId = option.provider,
            mediaType = if (detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME) "tv" else "movie",
            seasonNumber = episode?.seasonNumber,
            episodeNumber = episode?.episodeNumber,
            isM3u8 = option.isM3u8,
            container = option.container,
            trackers = option.trackers
        )

        _downloads.update { current ->
            val filtered = current.filterNot { it.id == id }
            listOf(item) + filtered
        }
        persistMetadata()

        scope.launch(Dispatchers.IO) {
            try {
                val db = com.euthopiar.data.local.AppDatabase.getInstance(context)
                val headersJson = json.encodeToString(item.headers)
                db.downloadAuthContextDao().insert(
                    com.euthopiar.data.local.DownloadAuthContextEntity(
                        downloadId = id,
                        headersJson = headersJson,
                        scraperId = item.scraperId,
                        mediaType = item.mediaType,
                        videoId = item.mediaId,
                        season = item.seasonNumber,
                        episode = item.episodeNumber
                    )
                )
            } catch (_: Exception) {}
        }

        if (waitingForWifi) {
            return
        }

        val wifiOnly = isWifiOnlyEnabled()
        EuthopiarDownloadService.start(context)
        EuthopiarDownloadJobService.scheduleUidtJob(context, id, estimatedTotal, wifiOnly)

        var lastNotifPercent = -1
        var lastNotifTimeMs = 0L

        engine.startOrResumeDownload(
            item = item,
            onProgress = { bytesDownloaded, totalBytes, percent, speed ->
                val resolvedTotal = if (totalBytes > 0L) totalBytes else estimatedTotal
                _downloads.update { list ->
                    list.map {
                        if (it.id == id) {
                            it.copy(
                                state = DownloadState.DOWNLOADING,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = resolvedTotal,
                                progressPercentage = percent,
                                speed = speed
                            )
                        } else it
                    }
                }
                val now = System.currentTimeMillis()
                val pctInt = percent.toInt()
                if (pctInt != lastNotifPercent && (now - lastNotifTimeMs >= 1000L || pctInt % 5 == 0)) {
                    lastNotifPercent = pctInt
                    lastNotifTimeMs = now
                    val subText = "$pctInt% • ${formatBytes(bytesDownloaded)} / ${formatBytes(resolvedTotal)} • $speed"
                    EuthopiarDownloadService.updateProgress(
                        context = context,
                        title = item.title,
                        subtitle = subText,
                        progress = pctInt,
                        downloadId = id
                    )
                }
            },
            onCompleted = { finalFilePath, totalBytes ->
                _downloads.update { list ->
                    list.map {
                        if (it.id == id) {
                            it.copy(
                                state = DownloadState.COMPLETED,
                                progressPercentage = 100f,
                                bytesDownloaded = totalBytes,
                                totalBytes = totalBytes,
                                localFilePath = finalFilePath,
                                speed = null
                            )
                        } else it
                    }
                }
                persistMetadata()
                EuthopiarDownloadJobService.cancelUidtJob(context, id)
                EuthopiarDownloadService.notifyComplete(context, item.title)
                checkStopService()
            },
            onError = { errorMessage ->
                _downloads.update { list ->
                    list.map {
                        if (it.id == id) {
                            it.copy(
                                state = DownloadState.FAILED,
                                failureReason = errorMessage,
                                speed = null
                            )
                        } else it
                    }
                }
                persistMetadata()
                EuthopiarDownloadJobService.cancelUidtJob(context, id)
                checkStopService()
            }
        )
    }

    fun startDownload(
        detail: MediaDetail,
        episode: EpisodeItem? = null,
        streamSource: StreamSource,
        qualityLabel: String
    ) {
        val option = DownloadOption(
            title = detail.title,
            quality = qualityLabel,
            size = streamSource.size ?: "~1.5 GB",
            url = streamSource.url,
            source = streamSource.serverName.ifBlank { "Stream Mirror" },
            headers = streamSource.headers,
            isM3u8 = streamSource.isM3u8,
            container = streamSource.container,
            trackers = streamSource.trackers
        )
        startDownload(detail, episode, option)
    }

    fun pauseDownload(id: String) {
        val currentItem = _downloads.value.firstOrNull { it.id == id }
        if (currentItem == null || currentItem.state == DownloadState.COMPLETED) {
            return
        }
        engine.pauseDownload(id)
        EuthopiarDownloadJobService.cancelUidtJob(context, id)
        _downloads.update { list ->
            list.map {
                if (it.id == id && it.state != DownloadState.COMPLETED) {
                    it.copy(state = DownloadState.PAUSED, failureReason = null, speed = null)
                } else it
            }
        }
        persistMetadata()
        checkStopService()
    }

    fun pauseAllDownloads() {
        val active = _downloads.value.filter { it.state == DownloadState.DOWNLOADING }
        for (item in active) {
            engine.pauseDownload(item.id)
            EuthopiarDownloadJobService.cancelUidtJob(context, item.id)
            _downloads.update { list ->
                list.map {
                    if (it.id == item.id && it.state != DownloadState.COMPLETED) {
                        it.copy(state = DownloadState.PAUSED, failureReason = null, speed = null)
                    } else it
                }
            }
        }
        if (active.isNotEmpty()) {
            persistMetadata()
            checkStopService()
        }
    }

    fun resumeDownload(id: String, fromJobService: Boolean = false) {
        val item = _downloads.value.firstOrNull { it.id == id } ?: return
        if (engine.isDownloading(id)) {
            return
        }
        if (item.state == DownloadState.COMPLETED && item.localFilePath != null && File(item.localFilePath).exists()) {
            return
        }

        val wifiOnly = isWifiOnlyEnabled()
        if (wifiOnly && isNetworkMeteredOrCellular()) {
            _downloads.update { list ->
                list.map {
                    if (it.id == id && it.state != DownloadState.COMPLETED) {
                        it.copy(state = DownloadState.PAUSED, failureReason = "Waiting for Wi-Fi", speed = null)
                    } else it
                }
            }
            persistMetadata()
            return
        }

        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(state = DownloadState.DOWNLOADING, failureReason = null) else it }
        }
        persistMetadata()

        EuthopiarDownloadService.start(context)
        if (!fromJobService) {
            EuthopiarDownloadJobService.scheduleUidtJob(context, id, item.totalBytes, wifiOnly)
        }

        scope.launch(Dispatchers.IO) {
            var effectiveItem = item
            if (effectiveItem.headers.isEmpty()) {
                try {
                    val db = com.euthopiar.data.local.AppDatabase.getInstance(context)
                    val authCtx = db.downloadAuthContextDao().getByDownloadId(id)
                    if (authCtx != null) {
                        val restored: Map<String, String> = json.decodeFromString(authCtx.headersJson)
                        if (restored.isNotEmpty()) {
                            effectiveItem = effectiveItem.copy(headers = restored)
                            updateDownloadUrl(id, effectiveItem.streamUrl, restored)
                        }
                    }
                } catch (_: Exception) {}
            }

            var lastNotifPercent = -1
            var lastNotifTimeMs = 0L

            engine.startOrResumeDownload(
                item = effectiveItem,
                onProgress = { bytesDownloaded, totalBytes, percent, speed ->
                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) {
                                it.copy(
                                    state = DownloadState.DOWNLOADING,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    progressPercentage = percent,
                                    speed = speed
                                )
                            } else it
                        }
                    }
                    val now = System.currentTimeMillis()
                    val pctInt = percent.toInt()
                    if (pctInt != lastNotifPercent && (now - lastNotifTimeMs >= 1000L || pctInt % 5 == 0)) {
                        lastNotifPercent = pctInt
                        lastNotifTimeMs = now
                        val subText = "$pctInt% • ${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)} • $speed"
                        EuthopiarDownloadService.updateProgress(
                            context = context,
                            title = effectiveItem.title,
                            subtitle = subText,
                            progress = pctInt,
                            downloadId = id
                        )
                    }
                },
                onCompleted = { finalFilePath, totalBytes ->
                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) {
                                it.copy(
                                    state = DownloadState.COMPLETED,
                                    progressPercentage = 100f,
                                    bytesDownloaded = totalBytes,
                                    totalBytes = totalBytes,
                                    localFilePath = finalFilePath,
                                    speed = null
                                )
                            } else it
                        }
                    }
                    persistMetadata()
                    EuthopiarDownloadJobService.cancelUidtJob(context, id)
                    EuthopiarDownloadService.notifyComplete(context, effectiveItem.title)
                    checkStopService()
                },
                onError = { errorMessage ->
                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) {
                                it.copy(
                                    state = DownloadState.FAILED,
                                    failureReason = errorMessage,
                                    speed = null
                                )
                            } else it
                        }
                    }
                    persistMetadata()
                    EuthopiarDownloadJobService.cancelUidtJob(context, id)
                    checkStopService()
                }
            )
        }
    }

    fun removeDownload(id: String) {
        val target = _downloads.value.firstOrNull { it.id == id }
        target?.localFilePath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) {
                    if (f.isDirectory) {
                        f.deleteRecursively()
                    } else if (f.name == "index.m3u8" || f.parentFile?.name == id) {
                        f.parentFile?.deleteRecursively()
                    } else {
                        f.delete()
                        val idxFile = File(f.parentFile, "${f.name}.idx")
                        if (idxFile.exists()) idxFile.delete()
                    }
                }
            } catch (_: Exception) {}
        }
        engine.removeDownload(id)
        EuthopiarDownloadJobService.cancelUidtJob(context, id)
        _downloads.update { list -> list.filterNot { it.id == id } }
        persistMetadata()
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.euthopiar.data.local.AppDatabase.getInstance(context)
                db.downloadAuthContextDao().deleteByDownloadId(id)
            } catch (_: Exception) {}
        }
        checkStopService()
    }

    private fun checkStopService() {
        val anyActive = _downloads.value.any { it.state == DownloadState.DOWNLOADING }
        if (!anyActive) {
            EuthopiarDownloadService.stop(context)
        }
    }

    fun matchesItem(item: DownloadItem, mediaId: String, episodeId: String? = null): Boolean {
        val matchesMedia = item.mediaId.equals(mediaId, ignoreCase = true) ||
                item.id.startsWith(mediaId, ignoreCase = true)
        if (!matchesMedia) return false

        if (episodeId == null) {
            return !item.isTvSeries
        }

        if (item.episodeId.equals(episodeId, ignoreCase = true) || item.id.equals(episodeId, ignoreCase = true)) {
            return true
        }

        if (item.id.contains(episodeId, ignoreCase = true) || episodeId.contains(item.id, ignoreCase = true)) {
            return true
        }

        // Parse season/episode tokens from episodeId (e.g. "mediaId:1:2" or "1:2")
        val colonTokens = episodeId.split(':')
        if (colonTokens.size >= 2) {
            val epNum = colonTokens.last().toIntOrNull()
            val seasonNum = colonTokens[colonTokens.size - 2].toIntOrNull()
            if (seasonNum != null && epNum != null) {
                if (item.seasonNumber == seasonNum && item.episodeNumber == epNum) {
                    return true
                }
                val tag = "_s${seasonNum}e${epNum}"
                if (item.id.contains(tag, ignoreCase = true)) {
                    return true
                }
            }
        }

        val seRegex = Regex("""(?:s|season)?\s*(\d+)\s*(?:e|ep|x|\.)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val match = seRegex.find(episodeId)
        if (match != null) {
            val seasonNum = match.groupValues[1].toIntOrNull()
            val epNum = match.groupValues[2].toIntOrNull()
            if (seasonNum != null && epNum != null) {
                if (item.seasonNumber == seasonNum && item.episodeNumber == epNum) {
                    return true
                }
                val tag = "_s${seasonNum}e${epNum}"
                if (item.id.contains(tag, ignoreCase = true)) {
                    return true
                }
            }
        }

        return false
    }

    private fun isDownloadFileValid(item: DownloadItem): Boolean {
        val path = item.localFilePath ?: return false
        val file = File(path)
        return if (file.isDirectory) {
            File(file, "index.m3u8").exists()
        } else {
            file.exists() && file.length() > 0L
        }
    }

    fun isMediaDownloaded(mediaId: String, episodeId: String? = null): Boolean {
        return _downloads.value.any { item ->
            matchesItem(item, mediaId, episodeId) &&
                    item.state == DownloadState.COMPLETED &&
                    isDownloadFileValid(item)
        }
    }

    fun getDownloadedItem(mediaId: String, episodeId: String? = null): DownloadItem? {
        return _downloads.value.firstOrNull { item ->
            matchesItem(item, mediaId, episodeId) &&
                    item.state == DownloadState.COMPLETED &&
                    isDownloadFileValid(item)
        }
    }

    fun getCacheDataSourceFactory(customHeaders: Map<String, String>? = null): DataSource.Factory {
        return DownloadManagerHelper.getCacheDataSourceFactory(context, customHeaders)
    }

    fun getOfflineCacheDataSourceFactory(): DataSource.Factory {
        return DownloadManagerHelper.getOfflineCacheDataSourceFactory(context)
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val downloadDir = engine.getDownloadDirectory()
        val appUsed = try {
            downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            0L
        }

        val statFs = try {
            StatFs(downloadDir.path)
        } catch (e: Exception) {
            StatFs(context.filesDir.path)
        }

        val freeBytes = statFs.availableBytes
        val totalBytes = statFs.totalBytes

        StorageStats(
            appUsedBytes = appUsed,
            freeBytes = freeBytes,
            totalBytes = totalBytes
        )
    }

    private fun parseEstimatedBytes(sizeStr: String?): Long {
        if (sizeStr.isNullOrBlank()) return 0L
        return try {
            val clean = sizeStr.replace("~", "").trim()
            val parts = clean.split(Regex("\\s+"))
            val num = parts.getOrNull(0)?.toDoubleOrNull() ?: return 0L
            val unit = parts.getOrNull(1)?.uppercase() ?: return 0L
            when {
                unit.startsWith("G") -> (num * 1024.0 * 1024.0 * 1024.0).toLong()
                unit.startsWith("M") -> (num * 1024.0 * 1024.0).toLong()
                unit.startsWith("K") -> (num * 1024.0).toLong()
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            if (mb < 10.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f MB", mb)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: DownloadRepository? = null

        fun getInstance(context: Context): DownloadRepository {
            return instance ?: synchronized(this) {
                instance ?: DownloadRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
