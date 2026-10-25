package com.euthopiar.ui.details.downloads

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.euthopiar.ui.components.VelvetAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.euthopiar.core.debrid.DebridManager
import com.euthopiar.core.debrid.DebridProviderType
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.streams.StreamItem
import com.euthopiar.core.streams.StreamsEmptyStateReason
import com.euthopiar.core.streams.StreamsRepository
import com.euthopiar.core.streams.sortedForGroupedDisplay
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.download.DownloadRepository
import com.euthopiar.ui.theme.AppFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Editorial Download Sources Sheet:
 * - Restyles the download sources list into the editorial typography and motion design.
 * - Header pinned (media title + resolution, "Downloads" 30sp Medium).
 * - Tabs pinned ("All", "P2P", "Direct" with spring underline).
 * - Best match Hero card with 58sp numerals and black download pill.
 * - Rows with seed health tiers and hairline dividers.
 * - Zero feature loss: preserves Debrid unrestrict, native P2P fallback, direct download.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSourcesSheet(
    detail: MediaDetail,
    episode: EpisodeItem? = null,
    provider: String? = null,
    onDismiss: () -> Unit,
    onDownloadStarted: (message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val downloadRepository = remember { DownloadRepository.getInstance(context) }
    val preferencesRepository = remember { PreferencesRepository.getInstance(context) }

    val isDebridEnabled by preferencesRepository.debridEnabled.collectAsState(initial = false)
    val activeDebridProvider by preferencesRepository.activeDebridProvider.collectAsState(initial = "NONE")
    val realDebridApiKey by preferencesRepository.realDebridApiKey.collectAsState(initial = "")
    val allDebridApiKey by preferencesRepository.allDebridApiKey.collectAsState(initial = "")
    val torBoxApiKey by preferencesRepository.torBoxApiKey.collectAsState(initial = "")
    val firstDownloadAlertDismissed by preferencesRepository.firstDownloadAlertDismissed.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        com.euthopiar.core.p2p.P2pStreamingEngine.prewarmEngine(context)
    }

    var pendingStreamToDownload by remember { mutableStateOf<Pair<StreamItem, String>?>(null) }
    var showFirstDownloadDialog by remember { mutableStateOf(false) }

    val effectiveEpisode = remember(detail, episode) {
        if (detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME) {
            episode ?: detail.episodes.firstOrNull() ?: EpisodeItem(
                id = "${detail.id}:1:1",
                title = "Episode 1",
                seasonNumber = 1,
                episodeNumber = 1,
                data = "${detail.id}:1:1"
            )
        } else {
            episode
        }
    }

    val s = effectiveEpisode?.seasonNumber
    val e = effectiveEpisode?.episodeNumber
    val mediaType = if (detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME) "tv" else "movie"

    LaunchedEffect(detail.id, s, e) {
        val detailImdb = detail.imdbId
        val resolvedImdb = if (!detailImdb.isNullOrBlank() && detailImdb.startsWith("tt")) {
            detailImdb
        } else {
            withContext(Dispatchers.IO) {
                com.euthopiar.core.util.TmdbBridge.resolveImdbId(
                    type = detail.type,
                    tmdbId = detail.id
                )
            }
        }
        StreamsRepository.load(
            type = mediaType,
            videoId = detail.id,
            season = s,
            episode = e,
            title = detail.title,
            year = detail.year,
            forceRefresh = false,
            imdbId = resolvedImdb ?: detail.imdbId
        )
    }

    val streamsUiState by StreamsRepository.uiState.collectAsState()
    val availableStreams = remember(streamsUiState.groups) {
        val valid = streamsUiState.allStreams.filter { stream ->
            stream.isSupportedDownloadUrl() || (stream.isTorrentStream && !stream.effectiveMagnetOrHash.isNullOrBlank())
        }.sortedForGroupedDisplay()
        val seen = HashSet<String>()
        valid.filter { stream ->
            val id = stream.playableDirectUrl ?: stream.effectiveMagnetOrHash ?: stream.url ?: "${stream.name}_${stream.title}_${stream.size}"
            val dedupeKey = "${stream.addonId}_${id}_${stream.quality.orEmpty()}"
            seen.add(dedupeKey)
        }
    }

    val subtitleText = remember(detail, effectiveEpisode) {
        if (detail.type == MediaType.TV_SERIES || detail.type == MediaType.ANIME) {
            val ep = effectiveEpisode
            if (ep != null) {
                "${detail.title} · S${ep.seasonNumber ?: 1}E${ep.episodeNumber ?: 1}"
            } else {
                detail.title
            }
        } else {
            detail.title
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeDownloadStreamId by remember { mutableStateOf<String?>(null) }
    var activeDownloadState by remember { mutableStateOf(StreamDownloadState.IDLE) }

    val executeStreamDownload: (StreamItem, String) -> Unit = { stream, streamKey ->
        val cleanKey = DownloadTokens.getStableStreamKey(stream)
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        activeDownloadStreamId = cleanKey
        activeDownloadState = StreamDownloadState.STARTING

        coroutineScope.launch {
            try {
                val cleanQuality = stream.quality ?: if (stream.isTorrentStream) "Torrent" else "Direct Stream"

                if (stream.isTorrentStream) {
                    val magnet = stream.effectiveMagnetOrHash
                    if (magnet.isNullOrBlank()) {
                        activeDownloadState = StreamDownloadState.IDLE
                        activeDownloadStreamId = null
                        onDownloadStarted("Invalid torrent magnet")
                        return@launch
                    }

                    val pType = DebridProviderType.fromId(activeDebridProvider)
                    var apiKey = when (pType) {
                        DebridProviderType.REAL_DEBRID -> realDebridApiKey
                        DebridProviderType.ALL_DEBRID -> allDebridApiKey
                        DebridProviderType.TORBOX -> torBoxApiKey
                        DebridProviderType.NONE -> ""
                    }
                    var effectiveProvider = pType
                    if (apiKey.isBlank()) {
                        if (realDebridApiKey.isNotBlank()) {
                            apiKey = realDebridApiKey
                            effectiveProvider = DebridProviderType.REAL_DEBRID
                        } else if (allDebridApiKey.isNotBlank()) {
                            apiKey = allDebridApiKey
                            effectiveProvider = DebridProviderType.ALL_DEBRID
                        } else if (torBoxApiKey.isNotBlank()) {
                            apiKey = torBoxApiKey
                            effectiveProvider = DebridProviderType.TORBOX
                        }
                    }

                    val canUseDebrid = isDebridEnabled && apiKey.isNotBlank() && effectiveProvider != DebridProviderType.NONE

                    if (canUseDebrid) {
                        onDownloadStarted("Unrestricting with ${effectiveProvider.displayName}...")
                        val resolveResult = withContext(Dispatchers.IO) {
                            DebridManager.getInstance().resolveStream(
                                providerType = effectiveProvider,
                                apiKey = apiKey,
                                linkOrMagnet = magnet,
                                fileIdx = stream.fileIdx,
                                season = effectiveEpisode?.seasonNumber,
                                episode = effectiveEpisode?.episodeNumber,
                                originalResolutionLabel = stream.quality
                            )
                        }

                        if (resolveResult.isSuccess) {
                            val resolvedSource = resolveResult.getOrThrow()
                            if (resolvedSource.url.isBlank() || !resolvedSource.url.startsWith("http")) {
                                throw IllegalStateException("Debrid returned an invalid unrestrict URL")
                            }
                            val directStream = stream.copy(
                                url = resolvedSource.url,
                                headers = resolvedSource.headers
                            )
                            val enqueued = downloadRepository.enqueueFromStream(
                                detail = detail,
                                episode = effectiveEpisode,
                                stream = directStream
                            )
                            if (enqueued) {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                } catch (_: Throwable) {}
                                activeDownloadState = StreamDownloadState.ADDED
                                delay(DownloadTokens.DISMISS_DELAY_MS)
                                onDownloadStarted("Downloading $cleanQuality...")
                                try {
                                    sheetState.hide()
                                } catch (_: Throwable) {}
                                onDismiss()
                            } else {
                                activeDownloadState = StreamDownloadState.IDLE
                                activeDownloadStreamId = null
                                onDownloadStarted("Failed to enqueue stream")
                            }
                        } else {
                            val err = resolveResult.exceptionOrNull()?.message ?: "Debrid resolution failed"
                            onDownloadStarted("Debrid fallback to P2P: $err")
                            val torrentStream = stream.copy(url = magnet)
                            val enqueued = downloadRepository.enqueueFromStream(
                                detail = detail,
                                episode = effectiveEpisode,
                                stream = torrentStream
                            )
                            if (enqueued) {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                } catch (_: Throwable) {}
                                activeDownloadState = StreamDownloadState.ADDED
                                delay(DownloadTokens.DISMISS_DELAY_MS)
                                onDownloadStarted("Starting Native P2P Download...")
                                try {
                                    sheetState.hide()
                                } catch (_: Throwable) {}
                                onDismiss()
                            } else {
                                activeDownloadState = StreamDownloadState.IDLE
                                activeDownloadStreamId = null
                                onDownloadStarted("Failed to start torrent download")
                            }
                        }
                    } else {
                        // Direct Native P2P torrent download
                        val torrentStream = stream.copy(url = magnet)
                        val enqueued = downloadRepository.enqueueFromStream(
                            detail = detail,
                            episode = effectiveEpisode,
                            stream = torrentStream
                        )
                        if (enqueued) {
                            activeDownloadState = StreamDownloadState.ADDED
                            delay(DownloadTokens.DISMISS_DELAY_MS)
                            onDownloadStarted("Starting Native P2P Torrent Download...")
                            try {
                                sheetState.hide()
                            } catch (_: Throwable) {}
                            onDismiss()
                        } else {
                            activeDownloadState = StreamDownloadState.IDLE
                            activeDownloadStreamId = null
                            onDownloadStarted("Failed to start torrent download")
                        }
                    }
                } else {
                    // Direct stream download
                    val enqueued = downloadRepository.enqueueFromStream(
                        detail = detail,
                        episode = effectiveEpisode,
                        stream = stream
                    )
                    if (enqueued) {
                        activeDownloadState = StreamDownloadState.ADDED
                        delay(DownloadTokens.DISMISS_DELAY_MS)
                        onDownloadStarted("Downloading $cleanQuality...")
                        try {
                            sheetState.hide()
                        } catch (_: Throwable) {}
                        onDismiss()
                    } else {
                        activeDownloadState = StreamDownloadState.IDLE
                        activeDownloadStreamId = null
                        onDownloadStarted("Failed to enqueue stream")
                    }
                }
            } catch (t: Throwable) {
                activeDownloadState = StreamDownloadState.IDLE
                activeDownloadStreamId = null
                onDownloadStarted("Download error: ${t.message}")
            }
        }
    }

    val handleStreamDownload: (StreamItem, String) -> Unit = { stream, streamKey ->
        if (!firstDownloadAlertDismissed) {
            pendingStreamToDownload = stream to streamKey
            showFirstDownloadDialog = true
        } else {
            executeStreamDownload(stream, streamKey)
        }
    }

    val onLongClickCopy: (StreamItem) -> Unit = { stream ->
        val link = stream.effectiveMagnetOrHash ?: stream.playableDirectUrl ?: stream.url
        if (!link.isNullOrBlank()) {
            clipboardManager.setText(AnnotatedString(link))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    val onRetry = {
        StreamsRepository.load(
            type = mediaType,
            videoId = detail.id,
            season = s,
            episode = e,
            title = detail.title,
            year = detail.year,
            forceRefresh = true,
            imdbId = detail.imdbId
        )
    }

    val runtimeMin = remember(detail, effectiveEpisode) {
        DownloadTokens.parseRuntimeMinutes(effectiveEpisode?.duration ?: detail.duration)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = DownloadTokens.sheetCornerRadius,
            topEnd = DownloadTokens.sheetCornerRadius
        ),
        containerColor = DownloadTokens.sheetBackground,
        scrimColor = DownloadTokens.scrimColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(
                            width = DownloadTokens.dragHandleWidth,
                            height = DownloadTokens.dragHandleHeight
                        )
                        .background(
                            color = DownloadTokens.dragHandleColor,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        },
        modifier = modifier
    ) {
        EditorialDownloadContent(
            title = subtitleText,
            allStreams = availableStreams,
            isLoading = streamsUiState.isAnyLoading && availableStreams.isEmpty(),
            isError = streamsUiState.emptyStateReason == StreamsEmptyStateReason.StreamFetchFailed && availableStreams.isEmpty(),
            activeDownloadStreamId = activeDownloadStreamId,
            activeDownloadState = activeDownloadState,
            runtimeMinutes = runtimeMin,
            isAnyLoading = streamsUiState.isAnyLoading,
            onDownloadClick = handleStreamDownload,
            onLongClick = onLongClickCopy,
            onRetry = onRetry
        )
    }

    if (showFirstDownloadDialog) {
        VelvetAlertDialog(
            onDismissRequest = {
                showFirstDownloadDialog = false
                pendingStreamToDownload = null
            },
            title = "Download Storage Notice",
            message = "Downloaded media files will be saved to your device's external downloads directory for offline playback. You can configure a custom download path anytime in Settings.",
            confirmText = "Proceed",
            dismissText = "Cancel",
            icon = Icons.Outlined.Download,
            onConfirm = {
                showFirstDownloadDialog = false
                coroutineScope.launch {
                    preferencesRepository.setFirstDownloadAlertDismissed(true)
                }
                pendingStreamToDownload?.let { (s, k) ->
                    executeStreamDownload(s, k)
                }
                pendingStreamToDownload = null
            },
            onDismiss = {
                showFirstDownloadDialog = false
                pendingStreamToDownload = null
            }
        )
    }
}

/**
 * Editorial sheet body containing pinned header, pinned tabs, and scrollable content list.
 */
@Composable
fun EditorialDownloadContent(
    title: String,
    allStreams: List<StreamItem>,
    isLoading: Boolean,
    isError: Boolean,
    activeDownloadStreamId: String?,
    activeDownloadState: StreamDownloadState,
    onDownloadClick: (StreamItem, String) -> Unit,
    modifier: Modifier = Modifier,
    runtimeMinutes: Int = 120,
    isAnyLoading: Boolean = false,
    onLongClick: ((StreamItem) -> Unit)? = null,
    onRetry: () -> Unit = {}
) {
    val reduceMotion = isReduceMotion()
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * DownloadTokens.sheetMaxHeightRatio).dp

    // Tabs: Always show "All", "P2P", and "Direct" filter tabs.
    // Retain tabs visibility stably across all loading states so they never jump or disappear.
    val availableTabs = remember {
        listOf(DownloadTab.ALL, DownloadTab.P2P, DownloadTab.DIRECT)
    }

    var selectedTab by remember { mutableStateOf(DownloadTab.ALL) }

    // Filter streams by active tab
    val filteredStreams = remember(allStreams.toList(), selectedTab) {
        when (selectedTab) {
            DownloadTab.ALL -> allStreams.toList()
            DownloadTab.P2P -> allStreams.filter { it.isTorrentStream }
            DownloadTab.DIRECT -> allStreams.filter { !it.isTorrentStream }
        }
    }

    // Rank best match and remaining rows
    val ranked = remember(filteredStreams) {
        DownloadTokens.rankAndPartitionStreams(filteredStreams)
    }
    val heroStream = ranked.hero
    val remainingStreams = ranked.remaining
    val keyedRemaining = remember(remainingStreams) {
        remainingStreams.toKeyedStreamItems()
    }

    // Common resolution check across filtered streams
    val commonResolution = remember(filteredStreams) {
        DownloadTokens.parseCommonResolution(filteredStreams)
    }

    // Track entrance for stagger & count-up animations after content has loaded
    var hasContentAppeared by remember { mutableStateOf(false) }
    var isInitialEntrance by remember { mutableStateOf(true) }

    LaunchedEffect(isLoading, allStreams.isEmpty()) {
        if (!isLoading && allStreams.isNotEmpty() && !hasContentAppeared) {
            hasContentAppeared = true
            delay(DownloadTokens.COUNT_UP_DURATION_MS + 250L)
            isInitialEntrance = false
        }
    }

    // Entrance fades for pinned elements
    val headerAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val tabsAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            launch {
                headerAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(DownloadTokens.ENTRANCE_HEADER_FADE_MS)
                )
            }
            launch {
                delay(DownloadTokens.ENTRANCE_TABS_DELAY_MS.toLong())
                tabsAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(DownloadTokens.ENTRANCE_HEADER_FADE_MS)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .padding(
                start = DownloadTokens.contentPaddingHorizontal,
                end = DownloadTokens.contentPaddingHorizontal,
                top = DownloadTokens.contentPaddingTop,
                bottom = DownloadTokens.contentPaddingBottom
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Pinned Header: Line 1 (title · common resolution), Line 2 ("Downloads" 30sp Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = headerAlpha.value }
        ) {
            val line1 = if (!commonResolution.isNullOrBlank()) {
                "$title · $commonResolution"
            } else {
                title
            }

            Text(
                text = line1,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = DownloadTokens.textMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Downloads",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.04).em,
                    fontFeatureSettings = "tnum",
                    color = DownloadTokens.textPrimary
                ),
                modifier = Modifier.semantics { heading() }
            )
        }

        // Pinned Tabs: animated underline & text color
        Box(modifier = Modifier.graphicsLayer { alpha = tabsAlpha.value }) {
            DownloadTabs(
                tabs = availableTabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // Scrollable content area
        when {
            isLoading -> {
                DownloadSkeleton()
            }
            isError -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Couldn't load sources",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = DownloadTokens.textMuted
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Retry",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = DownloadTokens.textPrimary
                            ),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onRetry
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
            filteredStreams.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnyLoading || isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = DownloadTokens.textMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedTab) {
                                    DownloadTab.P2P -> "Searching for P2P sources..."
                                    DownloadTab.DIRECT -> "Searching for direct sources..."
                                    DownloadTab.ALL -> "Searching for sources..."
                                },
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = DownloadTokens.textMuted
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    DownloadTab.P2P -> "No P2P sources available"
                                    DownloadTab.DIRECT -> "No direct sources available"
                                    DownloadTab.ALL -> "No sources for this filter"
                                },
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = DownloadTokens.textMuted
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (selectedTab != DownloadTab.ALL && allStreams.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "View all sources",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = DownloadTokens.textPrimary
                                    ),
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { selectedTab = DownloadTab.ALL }
                                        )
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Best Match Hero Card with stable slot key
                    if (heroStream != null) {
                        item(key = "editorial_hero_slot") {
                            val heroFallback = DownloadTokens.calculateEstimatedStreamSize(runtimeMinutes, heroStream.quality)

                            AnimatedContent(
                                targetState = heroStream,
                                transitionSpec = {
                                    if (reduceMotion) {
                                        fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                                    } else {
                                        fadeIn(animationSpec = tween(DownloadTokens.TAB_CHANGE_IN_DURATION_MS)) togetherWith
                                                fadeOut(animationSpec = tween(DownloadTokens.TAB_CHANGE_OUT_DURATION_MS))
                                    }
                                },
                                label = "heroCrossFade"
                            ) { targetHero ->
                                val cleanKey = DownloadTokens.getStableStreamKey(targetHero)
                                val itemState = if (activeDownloadStreamId == cleanKey || activeDownloadStreamId == "hero_$cleanKey") activeDownloadState else StreamDownloadState.IDLE

                                DownloadHeroCard(
                                    stream = targetHero,
                                    downloadState = itemState,
                                    enabled = activeDownloadStreamId == null,
                                    isInitialEntrance = isInitialEntrance,
                                    fallbackEstimate = heroFallback,
                                    onDownloadClick = { onDownloadClick(targetHero, cleanKey) }
                                )
                            }
                            Spacer(modifier = Modifier.height(DownloadTokens.heroGapBottom))
                        }
                    }

                    // Remaining source rows with strictly unique keys and race-condition-free dynamic updates
                    itemsIndexed(
                        items = keyedRemaining,
                        key = { _, item -> item.key }
                    ) { index, item ->
                        val stream = item.stream
                        val cleanKey = DownloadTokens.getStableStreamKey(stream)
                        val itemState = if (activeDownloadStreamId == cleanKey || activeDownloadStreamId == item.key) activeDownloadState else StreamDownloadState.IDLE
                        val rowFallback = DownloadTokens.calculateEstimatedStreamSize(runtimeMinutes, stream.quality)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DownloadSourceRow(
                                stream = stream,
                                index = index,
                                showResolution = commonResolution == null,
                                downloadState = itemState,
                                enabled = activeDownloadStreamId == null,
                                isInitialEntrance = isInitialEntrance,
                                fallbackEstimate = rowFallback,
                                onClick = { onDownloadClick(stream, cleanKey) },
                                onLongClick = onLongClick?.let { { it(stream) } }
                            )
                            if (index < keyedRemaining.lastIndex) {
                                HorizontalDivider(
                                    thickness = DownloadTokens.hairlineHeight,
                                    color = DownloadTokens.hairline
                                )
                            }
                        }
                    }

                    // Subtle loading indicator at bottom of list while background scrapers are still running
                    if (isAnyLoading) {
                        item(key = "editorial_loading_footer") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = DownloadTokens.textMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Searching for more sources...",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = DownloadTokens.textMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Section 9: Previews
// ============================================================================

private val sampleStreams = listOf(
    StreamItem(
        name = "Love of Your Life.2024.2160p.HDR.DDP5.1",
        quality = "4K 2160p",
        size = "11.98 GB",
        seeders = 180,
        sourceName = "Torrentio",
        addonName = "Torrentio",
        addonId = "torrentio",
        magnetUrl = "magnet:?xt=urn:btih:samplehash1"
    ),
    StreamItem(
        name = "Love of Your Life.2024.1080p.Web-DL",
        quality = "1080p",
        size = "4.20 GB",
        seeders = 65,
        sourceName = "Comet",
        addonName = "Comet",
        addonId = "comet",
        magnetUrl = "magnet:?xt=urn:btih:samplehash2"
    ),
    StreamItem(
        name = "Love of Your Life.2024.1080p.Direct",
        quality = "1080p",
        size = "2.50 GB",
        sourceName = "4KHDHub",
        addonName = "4KHDHub",
        addonId = "4khdhub",
        url = "https://example.com/stream1.mp4"
    ),
    StreamItem(
        name = "Love of Your Life.2024.720p.Web-DL",
        quality = "720p",
        size = "1.80 GB",
        seeders = 8,
        sourceName = "MediaFusion",
        addonName = "MediaFusion",
        addonId = "mediafusion",
        magnetUrl = "magnet:?xt=urn:btih:samplehash3"
    ),
    StreamItem(
        name = "Love of Your Life.2024.720p.Direct",
        quality = "720p",
        size = "1.20 GB",
        sourceName = "SuperStream",
        addonName = "SuperStream",
        addonId = "superstream",
        url = "https://example.com/stream2.mp4"
    )
)

@Preview(name = "1. Loaded (Mixed P2P and Direct)", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsLoaded() {
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = sampleStreams,
        isLoading = false,
        isError = false,
        activeDownloadStreamId = null,
        activeDownloadState = StreamDownloadState.IDLE,
        onDownloadClick = { _, _ -> }
    )
}

@Preview(name = "2. Single Type (No Tabs)", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsSingleType() {
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = sampleStreams.filter { it.isTorrentStream },
        isLoading = false,
        isError = false,
        activeDownloadStreamId = null,
        activeDownloadState = StreamDownloadState.IDLE,
        onDownloadClick = { _, _ -> }
    )
}

@Preview(name = "3. Loading State", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsLoading() {
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = emptyList(),
        isLoading = true,
        isError = false,
        activeDownloadStreamId = null,
        activeDownloadState = StreamDownloadState.IDLE,
        onDownloadClick = { _, _ -> }
    )
}

@Preview(name = "4. Empty Filter State", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsEmpty() {
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = emptyList(),
        isLoading = false,
        isError = false,
        activeDownloadStreamId = null,
        activeDownloadState = StreamDownloadState.IDLE,
        onDownloadClick = { _, _ -> }
    )
}

@Preview(name = "5. Error State", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsError() {
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = emptyList(),
        isLoading = false,
        isError = true,
        activeDownloadStreamId = null,
        activeDownloadState = StreamDownloadState.IDLE,
        onDownloadClick = { _, _ -> }
    )
}

@Preview(name = "6. Downloading State", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun PreviewEditorialDownloadsDownloading() {
    val firstKey = "${sampleStreams.first().addonId}_${sampleStreams.first().magnetUrl}"
    EditorialDownloadContent(
        title = "Love of Your Life",
        allStreams = sampleStreams,
        isLoading = false,
        isError = false,
        activeDownloadStreamId = firstKey,
        activeDownloadState = StreamDownloadState.STARTING,
        onDownloadClick = { _, _ -> }
    )
}
