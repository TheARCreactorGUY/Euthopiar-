package com.euthopiar.ui.player

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.network.AppUserAgent
import com.euthopiar.core.network.WebViewCookieJar
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import android.os.Handler
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import android.media.MediaMetadataRetriever
import com.euthopiar.core.model.EpisodeItem
import com.euthopiar.core.model.MediaDetail
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.StreamResult
import com.euthopiar.core.model.StreamSource
import com.euthopiar.core.model.SubtitleTrack
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.core.util.CharsetNormalizer
import com.euthopiar.ui.util.UserError
import com.euthopiar.ui.util.toUserError
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.player.subtitles.AssSubtitleParser
import com.euthopiar.data.preferences.PreferencesRepository
import com.euthopiar.ui.player.subtitles.SubtitleCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    ZOOM("Zoom (Fill)"),
    FIXED_16_9("16:9 Standard"),
    STRETCH("Stretch")
}

enum class DialogueBoostLevel(val label: String, val gainMb: Int) {
    OFF("Off", 0),
    LOW("Low (+3 dB)", 300),
    MEDIUM("Medium (+6 dB)", 600),
    HIGH("High (+9 dB)", 900)
}

enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    MIN_15(15, "15 Minutes"),
    MIN_30(30, "30 Minutes"),
    MIN_45(45, "45 Minutes"),
    MIN_60(60, "60 Minutes"),
    END_OF_EPISODE(-1, "End of Episode")
}

enum class ServerHealthCategory(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF4CAF50), // Green (smooth, instant buffer <= 150ms)
    AVERAGE("Average", 0xFFFFC107),     // Amber (smooth streaming 151-350ms)
    POOR("Poor", 0xFFFF9800),           // Orange (potential buffering 351-700ms)
    UNWATCHABLE("Unwatchable", 0xFFF44336) // Red (stalled or unplayable > 700ms)
}

data class ServerSourceOption(
    val id: String,
    val serverName: String,
    val resolutionLabel: String = "Auto",
    val label: String = if (serverName.isNotBlank() && resolutionLabel.isNotBlank()) "$serverName ($resolutionLabel)" else serverName,
    val streamSource: StreamSource,
    val latencyMs: Long? = null,
    val healthCategory: ServerHealthCategory? = null,
    val audioBadge: String? = null
)

data class VideoQualityOption(
    val id: String,
    val label: String,
    val height: Int? = null, // null indicates Auto (Adaptive)
    val latencyMs: Long? = null,
    val healthCategory: ServerHealthCategory? = null
)

data class SubtitleOption(
    val id: String,
    val label: String,
    val language: String? = null,
    val url: String? = null
)

data class AudioTrackOption(
    val id: String = "",
    val label: String = "",
    val language: String? = null,
    val isoCode: String = language ?: "en",
    val displayName: String = label.ifBlank { language ?: "Default" },
    val isSelected: Boolean = false,
    val trackIndex: Int = -1,
    val groupIndex: Int = -1,
    val isAuto: Boolean = false
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val userError: com.euthopiar.ui.util.UserError? = null,
    val mediaType: MediaType = MediaType.MOVIE,
    val mediaTitle: String = "",
    val episodeTitle: String? = null,
    val providerName: String = "Universal",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isPlaying: Boolean = false,
    val isOfflinePlayback: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val availableServers: List<ServerSourceOption> = emptyList(),
    val selectedServer: ServerSourceOption? = null,
    val availableQualities: List<VideoQualityOption> = listOf(VideoQualityOption("auto", "Auto (Adaptive)")),
    val selectedQuality: VideoQualityOption = VideoQualityOption("auto", "Auto (Adaptive)"),
    val availableSubtitles: List<SubtitleOption> = listOf(SubtitleOption("off", "Off")),
    val selectedSubtitle: SubtitleOption = SubtitleOption("off", "Off"),
    val availableAudioTracks: List<AudioTrackOption> = listOf(AudioTrackOption("auto", "Auto (Default)", isAuto = true)),
    val selectedAudioTrack: AudioTrackOption = AudioTrackOption("auto", "Auto (Default)", isAuto = true),
    val dialogueBoostLevel: DialogueBoostLevel = DialogueBoostLevel.OFF,
    val isNightModeDrcEnabled: Boolean = false,
    val selectedSleepTimer: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingSeconds: Int? = null,
    val playbackSpeed: Float = 1.0f,
    val isFastForwarding2x: Boolean = false,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val controlsVisible: Boolean = true,
    val hasPrevEpisode: Boolean = false,
    val hasNextEpisode: Boolean = false,
    val toastMessage: String? = null,
    val isScrubbing: Boolean = false,
    val scrubPositionMs: Long = 0L,
    val resumeNotice: String? = null,
    val subtitleOffsetMs: Long = 0L,
    val subtitleSizeSp: Int = 18,
    val subtitleBackgroundEnabled: Boolean = false,
    val subtitleColor: String = "WHITE",
    val subtitleOutline: Boolean = true,
    val seekIntervalSeconds: Int = 10,
    val availableEpisodes: List<EpisodeItem> = emptyList(),
    val availableSeasons: List<Int> = emptyList(),
    val isEpisodeSelectorOpen: Boolean = false,
    val selectedSeasonForSelector: Int = 1,
    val episodePlaybackMap: Map<Pair<Int, Int>, PlaybackHistoryEntity> = emptyMap(),
    val isEpisodesLoading: Boolean = false,
    val activeSidePanel: PlayerSidePanelType = PlayerSidePanelType.NONE,
    val smartFailoverEnabled: Boolean = true,
    val isScreenLocked: Boolean = false,
    val pinchAspectToast: String? = null,
    val showUpNextCard: Boolean = false,
    val upNextCountdownSeconds: Int = 10,
    val upNextEpisode: EpisodeItem? = null,
    val isSearchingExternalSubtitles: Boolean = false,
    val showSkipIntro: Boolean = false,
    val showSkipOutro: Boolean = false,
    val skipIntroSeconds: Int = 85,
    val skipIntroLabel: String = "Skip Intro",
    val backdropUrl: String? = null,
    val mediaFormatInfo: MediaFormatInfo = MediaFormatInfo(),
    val failoverNotice: FailoverNotice? = null,
    val qualityConflictNotice: com.euthopiar.core.network.QualityConflict? = null,
    val isP2pStream: Boolean = false,
    val p2pPeers: Int = 0,
    val p2pSeeds: Int = 0,
    val p2pDownloadSpeed: Long = 0L,
    val p2pBufferProgress: Float = 0f,
    val p2pPhaseLabel: String? = null
)

data class MediaFormatInfo(
    val resolutionBadge: String? = null, // "4K", "1080p", "720p"
    val dynamicRangeBadge: String? = null, // "DOLBY VISION", "HDR10+", "HDR10", "HDR"
    val audioBadge: String? = null, // "DOLBY ATMOS", "DOLBY AUDIO 5.1", "DOLBY AUDIO", "7.1 SURROUND", "5.1 SURROUND"
    val hasDolbyVision: Boolean = false,
    val hasDolbyAudio: Boolean = false,
    val hasAtmos: Boolean = false,
    val hasHdr: Boolean = false
) {
    val hasAnyBadge: Boolean
        get() = !resolutionBadge.isNullOrBlank() || !dynamicRangeBadge.isNullOrBlank() || !audioBadge.isNullOrBlank()
}

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ProviderRepository = ProviderRepository.Default
    private val downloadRepository: com.euthopiar.download.DownloadRepository =
        com.euthopiar.download.DownloadRepository.getInstance(application)
    private val libraryRepository: com.euthopiar.data.repository.LibraryRepository =
        com.euthopiar.data.repository.LibraryRepository.getInstance(application)
    private val preferencesRepository: PreferencesRepository =
        PreferencesRepository.getInstance(application)

    private val persistenceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var currentPosterUrl: String? = null
    private var currentBackdropUrl: String? = null
    private var lastProgressSaveTimestamp: Long = 0L
    private var resumeDismissJob: Job? = null
    private var currentCanonicalStream: com.euthopiar.core.model.CanonicalStream? = null
    fun getCurrentCanonicalStream(): com.euthopiar.core.model.CanonicalStream? = currentCanonicalStream

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var userDefaultQuality: String = "Auto"
    private var streamJob: Job? = null
    private var tickerJob: Job? = null
    private var autoHideJob: Job? = null
    private var toastJob: Job? = null
    private var subtitleJob: Job? = null
    private var p2pStateJob: Job? = null

    private var pre2xSpeed: Float = 1.0f
    private var videoFrameRendered: Boolean = false
    private var activeSubtitleCues: List<SubtitleCue> = emptyList()
    private val _currentSubtitleCue = MutableStateFlow<SubtitleCue?>(null)
    val currentSubtitleCue: StateFlow<SubtitleCue?> = _currentSubtitleCue.asStateFlow()
    private val _currentSubtitleText = MutableStateFlow<String?>(null)
    val currentSubtitleText: StateFlow<String?> = _currentSubtitleText.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val webViewCookieJar = WebViewCookieJar()

    private var currentMediaId: String? = null
    private var currentEpisodeData: String? = null
    private var currentImdbId: String? = null
    private var currentProvider: String? = null
    private var currentType: MediaType = MediaType.MOVIE
    private var loadedEpisodes: List<EpisodeItem> = emptyList()
    private var streamResult: StreamResult? = null

    private val streamingHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val failedServerIds = mutableSetOf<String>()
    private var activeRefreshableSource: com.euthopiar.core.streams.RefreshableSource? = null
    private var bufferingStallStartTime: Long? = null
    private var userExplicitServerSelection: Boolean = false
    private val audioDspManager by lazy { AudioDspManager(application) }
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var dynamicsProcessing: android.media.audiofx.DynamicsProcessing? = null
    private var sleepTimerJob: Job? = null
    private var upNextJob: Job? = null
    private var isUpNextDismissed: Boolean = false
    private var isAutoSkipIntroEnabled: Boolean = false

    private var totalBytesLoadedCumulative: Long = 0L
    private var lastVelocityCheckTimeMs: Long = 0L
    private var lastBytesLoaded: Long = 0L
    private var currentGracePeriodMs: Long = 5_000L
    private var lastBufferedPositionMs: Long = 0L
    private var lastP2pBufferProgress: Float = 0f
    private var lastPeerCount: Int = 0
    private var starvationConsecutiveSeconds: Int = 0
    private var continuousPlayTimeMs: Long = 0L
    private val boostedTiersForSession = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var pendingQualityOption: VideoQualityOption? = null
    private var pendingServerOption: ServerSourceOption? = null
    private var currentOriginalLanguage: String? = null

    init {
        initExoPlayer()
        observeUserPreferences()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            preferencesRepository.subtitleSizeSp.collect { size ->
                _uiState.update { it.copy(subtitleSizeSp = size) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.subtitleBackgroundEnabled.collect { bgEnabled ->
                _uiState.update { it.copy(subtitleBackgroundEnabled = bgEnabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.subtitleColor.collect { color ->
                _uiState.update { it.copy(subtitleColor = color) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.subtitleOutline.collect { outline ->
                _uiState.update { it.copy(subtitleOutline = outline) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.seekIntervalSeconds.collect { interval ->
                _uiState.update { it.copy(seekIntervalSeconds = interval) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.defaultQuality.collect { q ->
                userDefaultQuality = q
            }
        }
        viewModelScope.launch {
            preferencesRepository.countdownNextEpisodeSeconds.collect { sec ->
                _uiState.update { it.copy(upNextCountdownSeconds = sec) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.autoSkipIntro.collect { enabled ->
                isAutoSkipIntroEnabled = enabled
            }
        }
        viewModelScope.launch {
            preferencesRepository.skipIntroDurationSeconds.collect { durSec ->
                _uiState.update { it.copy(skipIntroSeconds = durSec) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.smartFailoverEnabled.collect { enabled ->
                _uiState.update { it.copy(smartFailoverEnabled = enabled) }
            }
        }
    }

    private fun initExoPlayer() {
        val context = getApplication<Application>()

        val bufferMb = kotlinx.coroutines.runBlocking {
            try {
                preferencesRepository.playbackBufferSizeMb.first()
            } catch (_: Throwable) {
                50
            }
        }
        val hwAccel = kotlinx.coroutines.runBlocking {
            try {
                preferencesRepository.hardwareAccelerationEnabled.first()
            } catch (_: Throwable) {
                true
            }
        }
        val seekSeconds = kotlinx.coroutines.runBlocking {
            try {
                preferencesRepository.seekIntervalSeconds.first()
            } catch (_: Throwable) {
                10
            }
        }

        // Clamp buffer size based on actual device memory to prevent OOM on 4K REMUX and lower-RAM devices
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val isHighRam = (actManager?.memoryClass ?: 192) >= 384 || totalRamMb > 4500
        val targetBytes = if (isHighRam) 128 * 1024 * 1024 else 64 * 1024 * 1024

        val loadControl = DefaultLoadControl.Builder()
            .setTargetBufferBytes(targetBytes)
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 35_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(15_000, true)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: Handler,
                eventListener: AudioRendererEventListener,
                out: ArrayList<Renderer>
            ) {
                // 1. Primary: Register standard platform MediaCodecAudioRenderer first.
                // Standard streams (AAC, Opus, MP3) use hardware decoding with native AudioTimestamp
                // anchors, maintaining sample-accurate synchronization with MediaCodecVideoRenderer.
                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    true,
                    audioSink,
                    eventHandler,
                    eventListener,
                    out
                )

                // 2. Fallback: Register FfmpegAudioRenderer as software fallback for codecs
                // not natively licensed or supported by device platform HAL (AC-3, E-AC-3, TrueHD, DTS).
                try {
                    val ffmpegRenderer = androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer(
                        eventHandler,
                        eventListener,
                        audioSink
                    )
                    out.add(ffmpegRenderer)
                } catch (t: Throwable) {
                    android.util.Log.w("PlayerViewModel", "Could not instantiate FfmpegAudioRenderer: ${t.message}")
                }
            }

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                // Pre-Sonic Channel Downmixing: Downmix multi-channel (5.1 / 7.1) audio to stereo
                // BEFORE reaching SonicAudioProcessor. This cuts Sonic processing overhead by ~70%
                // at 1.5x / 2.0x playback speeds and prevents AudioTrack buffer starvation.
                val coef51ToStereo = floatArrayOf(
                    1.0f, 0.0f, 0.7071f, 0.0f, 0.7071f, 0.0f,
                    0.0f, 1.0f, 0.7071f, 0.0f, 0.0f, 0.7071f
                )
                val coef71ToStereo = floatArrayOf(
                    1.0f, 0.0f, 0.7071f, 0.0f, 0.7071f, 0.0f, 0.5f, 0.0f,
                    0.0f, 1.0f, 0.7071f, 0.0f, 0.0f, 0.7071f, 0.0f, 0.5f
                )
                val channelMixingAudioProcessor = ChannelMixingAudioProcessor().apply {
                    putChannelMixingMatrix(ChannelMixingMatrix(6, 2, coef51ToStereo))
                    putChannelMixingMatrix(ChannelMixingMatrix(8, 2, coef71ToStereo))
                }

                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(false)
                    .setAudioProcessors(arrayOf(channelMixingAudioProcessor))
                    .build()
            }
        }.apply {
            if (hwAccel) {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            } else {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            }
            setEnableDecoderFallback(true)
            // Use high-precision software Sonic WSOLA audio time-stretching for 100% jitter-free speed changes at 1.25x, 1.5x, 2x
            setEnableAudioTrackPlaybackParams(false)
        }

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
            )
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(com.euthopiar.core.network.PlayerPlaybackNetworking.playbackHttpClient)
        val overridingDataSourceFactory = ResponseHeaderOverridingDataSource.Factory(okHttpDataSourceFactory)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(DefaultDataSource.Factory(context, overridingDataSourceFactory))

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val hasWakeLock = try {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WAKE_LOCK
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .apply {
                if (hasWakeLock) {
                    try {
                        setWakeMode(C.WAKE_MODE_NETWORK)
                    } catch (_: Throwable) {}
                }
            }
            .setSeekBackIncrementMs(seekSeconds * 1000L)
            .setSeekForwardIncrementMs(seekSeconds * 1000L)
            .build()
        player.setSeekParameters(SeekParameters.DEFAULT)
        val initSessionId = player.audioSessionId
        if (initSessionId != C.AUDIO_SESSION_ID_UNSET && initSessionId > 0) {
            audioDspManager.setAudioSessionId(initSessionId)
        }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    audioDspManager.setAudioSessionId(audioSessionId)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                updateSubtitleForPosition(pos)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (timeline.isEmpty) return
                val window = Timeline.Window()
                val windowIndex = player.currentMediaItemIndex.coerceIn(0, (timeline.windowCount - 1).coerceAtLeast(0))
                timeline.getWindow(windowIndex, window)
                val duration = window.durationMs.takeIf { it > 0 } ?: player.duration.takeIf { it > 0 }
                if (duration != null && duration > 0) {
                    _uiState.update { it.copy(durationMs = duration) }
                }
                android.util.Log.d("PlayerViewModel", "onTimelineChanged: isSeekable=${window.isSeekable}, durationMs=${window.durationMs}, dynamic=${window.isDynamic}, playerDuration=${player.duration}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _uiState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        starvationConsecutiveSeconds = 0
                        val duration = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs

                        // Honeypot Resilience: detect short warning/honeypot clips (< 95s) for full Movies/TV (only if auto-selected)
                        if (_uiState.value.smartFailoverEnabled && !userExplicitServerSelection && duration in 1000L..95000L && _uiState.value.mediaType in listOf(MediaType.MOVIE, MediaType.TV_SERIES)) {
                            val badServer = _uiState.value.selectedServer
                            if (badServer != null && badServer.id !in failedServerIds) {
                                failedServerIds.add(badServer.id)
                                val remaining = _uiState.value.availableServers.filterNot { it.id in failedServerIds }
                                if (remaining.isNotEmpty()) {
                                    val nextServer = remaining.first()
                                    showToast("Notice/warning clip detected (${duration / 1000}s). Switching to ${nextServer.serverName}...")
                                    selectServer(nextServer)
                                    return
                                }
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isBuffering = false,
                                durationMs = duration,
                                isPlaying = player.isPlaying
                            )
                        }
                        updateMediaFormatInfo()
                        resetAutoHideTimer()
                        applyDialogueBoost()
                        applyNightModeDrc()
                    }
                    Player.STATE_ENDED -> {
                        _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
                        if (_uiState.value.selectedSleepTimer == SleepTimerOption.END_OF_EPISODE) {
                            _uiState.update { it.copy(selectedSleepTimer = SleepTimerOption.OFF) }
                            showToast("Bedtime sleep timer: paused at end of episode")
                        } else if (_uiState.value.hasNextEpisode) {
                            playNextEpisode()
                        }
                    }
                    Player.STATE_IDLE -> {
                        _uiState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                android.util.Log.d("PlayerViewModel", "onRenderedFirstFrame: First video frame rendered successfully")
                videoFrameRendered = true
                updateMediaFormatInfo()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    resetAutoHideTimer()
                    applyDialogueBoost()
                    applyNightModeDrc()
                } else {
                    savePlaybackProgress()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PlayerViewModel", "ExoPlayer onPlayerError: ${error.errorCodeName} (${error.errorCode}) - ${error.message}", error)

                // 0. Subtitle / Text Renderer Recovery
                val isSubtitleError = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ||
                        (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED && (
                            error.message?.contains("TextRenderer", ignoreCase = true) == true ||
                            error.message?.contains("subtitle", ignoreCase = true) == true ||
                            error.cause?.javaClass?.name?.contains("subtitle", ignoreCase = true) == true ||
                            error.cause?.javaClass?.name?.contains("text", ignoreCase = true) == true
                        )) ||
                        error.message?.contains("subtitle", ignoreCase = true) == true ||
                        error.message?.contains("SubripDecoder", ignoreCase = true) == true ||
                        error.message?.contains("WebvttDecoder", ignoreCase = true) == true ||
                        error.message?.contains("SsaDecoder", ignoreCase = true) == true ||
                        error.message?.contains("TextRenderer", ignoreCase = true) == true ||
                        error.message?.contains("text/", ignoreCase = true) == true ||
                        error.cause?.message?.contains("subtitle", ignoreCase = true) == true

                if (isSubtitleError) {
                    try {
                        exoPlayer?.trackSelectionParameters = exoPlayer?.trackSelectionParameters?.buildUpon()
                            ?.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            ?.build() ?: return
                        _uiState.update { it.copy(selectedSubtitle = SubtitleOption("off", "Off")) }
                        showToast("Subtitle parsing error. Disabling subtitle track.")
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                        return
                    } catch (_: Throwable) {}
                }

                // 1. Audio Decoder / Audio Renderer Recovery
                val isAudioError = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                        error.message?.contains("MediaCodecAudioRenderer", ignoreCase = true) == true ||
                        error.message?.contains("AudioRenderer", ignoreCase = true) == true ||
                        error.message?.contains("audio/", ignoreCase = true) == true ||
                        error.cause?.message?.contains("Audio", ignoreCase = true) == true

                if (isAudioError) {
                    val currentTrack = _uiState.value.selectedAudioTrack
                    val fallback = _uiState.value.availableAudioTracks.firstOrNull { it.id != currentTrack.id && !it.isAuto }
                    if (fallback != null) {
                        showToast("Switching to compatible audio track: ${fallback.displayName}...")
                        selectAudioTrack(fallback)
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                        return
                    } else {
                        // Disable audio track to allow uninterrupted video playback rather than failing completely
                        exoPlayer?.trackSelectionParameters = exoPlayer?.trackSelectionParameters?.buildUpon()
                            ?.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                            ?.build() ?: return
                        showToast("Audio format unsupported on this device. Playing video.")
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                        return
                    }
                }

                // 2. Video Decoder / Capabilities Recovery
                val isVideoDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                        error.cause is androidx.media3.exoplayer.video.MediaCodecVideoDecoderException ||
                        error.message?.contains("MediaCodecVideoRenderer", ignoreCase = true) == true ||
                        error.message?.contains("NO_EXCEEDS_CAPABILITIES", ignoreCase = true) == true

                if (_uiState.value.smartFailoverEnabled && isVideoDecoderError) {
                    val currentServer = _uiState.value.selectedServer
                    if (currentServer != null) {
                        failedServerIds.add(currentServer.id)
                    }
                    val non4kFallback = _uiState.value.availableServers.firstOrNull {
                        it.id !in failedServerIds &&
                                !it.resolutionLabel.contains("2160") &&
                                !it.resolutionLabel.contains("4k", ignoreCase = true)
                    }
                    val fallback = non4kFallback ?: _uiState.value.availableServers.firstOrNull { it.id !in failedServerIds }
                    if (fallback != null) {
                        showToast("Stream exceeds hardware decoder limits. Switching to ${fallback.serverName} (${fallback.resolutionLabel})...")
                        selectServer(fallback, isUserExplicit = false)
                        return
                    }
                }

                // 3. HTTP 401/403 Token Auto-Refresh Prevention via RefreshableSource
                val currentServer = _uiState.value.selectedServer
                val isHttpAuthError = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.let {
                            it.responseCode == 401 || it.responseCode == 403
                        } == true ||
                        error.message?.contains("401") == true ||
                        error.message?.contains("403") == true ||
                        error.cause?.message?.contains("401") == true ||
                        error.cause?.message?.contains("403") == true

                val refreshable = activeRefreshableSource
                if (isHttpAuthError && refreshable != null && currentServer != null && !failedServerIds.contains(currentServer.id + "_refreshed")) {
                    failedServerIds.add(currentServer.id + "_refreshed")
                    val staleUrl = currentServer.streamSource.url
                    viewModelScope.launch(Dispatchers.IO) {
                        val refreshedUrl = refreshable.refreshOnExpiredToken(staleUrl)
                        if (!refreshedUrl.isNullOrBlank() && refreshedUrl != staleUrl) {
                            withContext(Dispatchers.Main) {
                                showToast("Token refreshed. Resuming playback...")
                                val updatedSource = currentServer.streamSource.copy(
                                    url = refreshedUrl,
                                    headers = refreshable.currentHeaders
                                )
                                val updatedServer = currentServer.copy(streamSource = updatedSource)
                                _uiState.update { it.copy(selectedServer = updatedServer) }
                                playStreamSource(updatedSource, streamResult?.subtitles ?: emptyList(), seekToPosition = exoPlayer?.currentPosition ?: _uiState.value.currentPositionMs)
                            }
                            return@launch
                        } else {
                            withContext(Dispatchers.Main) {
                                fallbackToNextServer(currentServer, error)
                            }
                        }
                    }
                    return
                }

                // 4. Server Source Fallback
                fallbackToNextServer(currentServer, error)
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractExoPlayerTracks(tracks)
            }

            override fun onCues(cueGroup: CueGroup) {
                if (activeSubtitleCues.isEmpty()) {
                    try {
                        val rawText = if (cueGroup.cues.isNotEmpty()) {
                            cueGroup.cues.joinToString("\n") { it.text?.toString() ?: "" }.trim()
                        } else null
                        val sanitized = if (!rawText.isNullOrBlank()) {
                            val stripped = rawText
                                .replace(Regex("<[^>]*>"), "")
                                .replace(Regex("\\{[^}]*\\}"), "")
                            SubtitleParser.sanitizeSubtitleText(stripped)
                        } else null
                        if (_currentSubtitleText.value != sanitized && _uiState.value.selectedSubtitle.id != "off") {
                            _currentSubtitleText.value = sanitized.takeIf { !it.isNullOrBlank() }
                        }
                    } catch (t: Throwable) {
                        android.util.Log.e("PlayerViewModel", "Error in onCues: ${t.message}", t)
                    }
                }
            }
        })

        // Diagnostic AnalyticsListener for format tracking, decoder initialization, and playback state monitoring
        player.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onDownstreamFormatChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                android.util.Log.d(
                    "PlayerAnalytics",
                    "onDownstreamFormatChanged: trackType=${mediaLoadData.trackType}, mime=${mediaLoadData.trackFormat?.sampleMimeType}, res=${mediaLoadData.trackFormat?.width}x${mediaLoadData.trackFormat?.height}, codecs=${mediaLoadData.trackFormat?.codecs}"
                )
            }

            override fun onLoadCompleted(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                val loaded = loadEventInfo.bytesLoaded.coerceAtLeast(0L)
                totalBytesLoadedCumulative += loaded
                android.util.Log.d(
                    "PlayerAnalytics",
                    "onLoadCompleted: trackType=${mediaLoadData.trackType}, uri=${loadEventInfo.uri}, bytes=${loadEventInfo.bytesLoaded}, durationMs=${loadEventInfo.loadDurationMs}"
                )
            }

            override fun onVideoDecoderInitialized(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                android.util.Log.d(
                    "PlayerAnalytics",
                    "onVideoDecoderInitialized: decoder=$decoderName, durationMs=$initializationDurationMs"
                )
            }

            override fun onAudioDecoderInitialized(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                android.util.Log.d(
                    "PlayerAnalytics",
                    "onAudioDecoderInitialized: decoder=$decoderName, durationMs=$initializationDurationMs"
                )
            }

            override fun onPlayerError(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                error: PlaybackException
            ) {
                android.util.Log.e(
                    "PlayerAnalytics",
                    "onPlayerError: errorCode=${error.errorCode}, message=${error.message}",
                    error
                )
            }

            override fun onPlaybackStateChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                state: Int
            ) {
                val stateStr = when (state) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "STATE_UNKNOWN($state)"
                }
                android.util.Log.d("PlayerAnalytics", "onPlaybackStateChanged: state=$stateStr")
            }
        })

        exoPlayer = player
        startPositionTicker()
    }

    private fun isInstantFailure(error: PlaybackException): Boolean {
        val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode
        if (httpCode != null && (httpCode == 401 || httpCode == 403 || httpCode == 404 || httpCode == 410 || httpCode in 500..599)) {
            return true
        }
        val msg = (error.message.orEmpty() + " " + error.cause?.message.orEmpty()).lowercase()
        if (msg.contains("eof") || msg.contains("end of file") || msg.contains("dead host") ||
            msg.contains("unknownhost") || msg.contains("connection refused") || msg.contains("connection reset") ||
            msg.contains("401") || msg.contains("403") || msg.contains("404") || msg.contains("410") ||
            msg.contains("500") || msg.contains("502") || msg.contains("503")
        ) {
            return true
        }
        if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.cause is androidx.media3.exoplayer.video.MediaCodecVideoDecoderException ||
            error.cause?.javaClass?.name?.contains("DecoderInitializationException") == true
        ) {
            return true
        }
        return false
    }

    private fun fallbackToNextServer(currentServer: ServerSourceOption?, error: PlaybackException) {
        val instant = isInstantFailure(error)
        if (instant) {
            android.util.Log.d("PlayerViewModel", "Instant failover triggered (0ms) for error: ${error.errorCodeName} - ${error.message}")
        }
        if (!_uiState.value.smartFailoverEnabled) {
            android.util.Log.w("PlayerViewModel", "Smart failover disabled; playback error on ${currentServer?.serverName}: ${error.localizedMessage}", error)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isBuffering = false,
                    userError = com.euthopiar.ui.util.UserError.SourceUnavailable,
                    errorMessage = null
                )
            }
            return
        }
        if (currentServer != null) {
            failedServerIds.add(currentServer.id)
        }
        val otherServers = _uiState.value.availableServers.filter { it.id !in failedServerIds }
        if (otherServers.isNotEmpty()) {
            val fallback = otherServers.first()
            val name = currentServer?.serverName ?: "Server"
            showToast("$name error. Switching to ${fallback.serverName}…")
            selectServer(fallback, isUserExplicit = false)
        } else {
            android.util.Log.w("PlayerViewModel", "Playback error: ${error.localizedMessage}", error)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isBuffering = false,
                    userError = com.euthopiar.ui.util.UserError.SourceUnavailable,
                    errorMessage = null
                )
            }
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun loadStream(
        mediaId: String,
        episodeData: String?,
        provider: String?,
        type: MediaType = MediaType.MOVIE,
        title: String? = null,
        epTitle: String? = null,
        season: Int? = null,
        episode: Int? = null
    ) {
        val targetEpisodeData = when {
            (type == MediaType.TV_SERIES || type == MediaType.ANIME) && season != null && episode != null -> {
                if (!episodeData.isNullOrBlank() && episodeData.contains(":")) episodeData else "$mediaId:$season:$episode"
            }
            !episodeData.isNullOrBlank() -> episodeData
            else -> mediaId
        }
        currentMediaId = mediaId
        currentEpisodeData = targetEpisodeData
        currentProvider = provider
        currentType = type
        failedServerIds.clear()
        activeRefreshableSource = null
        bufferingStallStartTime = null
        isUpNextDismissed = false
        upNextJob?.cancel()
        upNextJob = null
        hasPromptedSkipIntro = false
        currentAniSkipTimestamps = null
        currentImdbId = Regex("""tt\d+""").find("$mediaId $targetEpisodeData")?.value
        if (mediaId.all { it.isDigit() } && mediaId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val endpoint = if (type == MediaType.TV_SERIES || type == MediaType.ANIME) "tv" else "movie"
                    if (currentImdbId == null) {
                        val extUrl = "https://api.themoviedb.org/3/$endpoint/$mediaId/external_ids?api_key=${com.euthopiar.core.util.TmdbBridge.DEFAULT_API_KEY}"
                        val req = Request.Builder().url(extUrl).header("User-Agent", "Mozilla/5.0").build()
                        DohDns.metadataClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string().orEmpty()
                                val id = org.json.JSONObject(body).optString("imdb_id").takeIf { it.startsWith("tt") }
                                if (id != null) {
                                    currentImdbId = id
                                }
                            }
                        }
                    }
                    val detailsUrl = "https://api.themoviedb.org/3/$endpoint/$mediaId?api_key=${com.euthopiar.core.util.TmdbBridge.DEFAULT_API_KEY}"
                    val detReq = Request.Builder().url(detailsUrl).header("User-Agent", "Mozilla/5.0").build()
                    DohDns.metadataClient.newCall(detReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val lang = org.json.JSONObject(body).optString("original_language").takeIf { it.isNotBlank() }
                            if (lang != null) {
                                currentOriginalLanguage = lang
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        val isTv = type == MediaType.TV_SERIES || type == MediaType.ANIME
        if (isTv && episode != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val rawTitle = title ?: mediaId
                    val sTitle = cleanAnimeTitle(rawTitle)
                    if (sTitle.isNotBlank()) {
                        val res = com.euthopiar.core.aniskip.AniSkipClient.getInstance().getSkipTimesByTitle(sTitle, episode)
                        if (res.isSuccess) {
                            currentAniSkipTimestamps = res.getOrNull()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        android.util.Log.d("PlayerViewModel", "loadStream: mediaId=$mediaId, targetEpisodeData=$targetEpisodeData, provider=$provider")

        // 1. Cancel previous stream collection, clear player items, and reset cues
        try {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
        } catch (_: Throwable) {}
        p2pStateJob?.cancel()
        streamJob?.cancel()
        subtitleJob?.cancel()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        exoPlayer?.playbackParameters = PlaybackParameters(1.0f)
        pre2xSpeed = 1.0f
        activeSubtitleCues = emptyList()
        _currentSubtitleCue.value = null
        _uiState.update {
            it.copy(
                playbackSpeed = 1.0f,
                isLoading = true,
                errorMessage = null,
                mediaType = type,
                mediaTitle = title ?: mediaId,
                episodeTitle = if (isTv) epTitle else null,
                seasonNumber = if (isTv) season else null,
                episodeNumber = if (isTv) episode else null,
                providerName = provider ?: "Universal",
                controlsVisible = true,
                hasNextEpisode = false,
                hasPrevEpisode = false,
                showUpNextCard = false,
                upNextCountdownSeconds = 10,
                upNextEpisode = null,
                isEpisodeSelectorOpen = false,
                selectedSeasonForSelector = if (isTv) (season ?: 1) else 1,
                availableServers = emptyList(),
                selectedServer = null,
                availableSubtitles = listOf(SubtitleOption("off", "Off")),
                selectedSubtitle = SubtitleOption("off", "Off"),
                availableQualities = listOf(VideoQualityOption("auto", "Auto (Adaptive)")),
                selectedQuality = VideoQualityOption("auto", "Auto (Adaptive)"),
                availableAudioTracks = listOf(AudioTrackOption("auto", "Auto (Default)", isAuto = true)),
                selectedAudioTrack = AudioTrackOption("auto", "Auto (Default)", isAuto = true),
                currentPositionMs = 0L,
                bufferedPositionMs = 0L,
                durationMs = 0L,
                isOfflinePlayback = false
            )
        }

        streamJob = viewModelScope.launch(Dispatchers.IO) {
            val historyId = PlaybackHistoryEntity.buildId(provider, mediaId, season, episode)
            val legacyId = if (isTv && season != null && episode != null) "${mediaId}_s${season}_e${episode}" else mediaId
            val savedProgress = libraryRepository.getPlaybackProgress(historyId, provider, mediaId, season, episode)
                ?: libraryRepository.getPlaybackProgress(legacyId)
            val resumePosition = if (savedProgress != null && savedProgress.currentPositionMs > 5000L && !savedProgress.isCompleted) {
                savedProgress.currentPositionMs
            } else {
                null
            }

            if (isTv) {
                _uiState.update { it.copy(isEpisodesLoading = true) }
                launch {
                    try {
                        val detail = repository.getDetails(mediaId, type, provider)
                        loadedEpisodes = detail.episodes
                        val seasons = detail.episodes.map { it.seasonNumber }.distinct().sorted()
                        val curSeason = season ?: 1
                        val curEpisode = episode ?: 1
                        val currentEpItem = loadedEpisodes.firstOrNull {
                            it.seasonNumber == curSeason && it.episodeNumber == curEpisode
                        }
                        val nextEp = findNextEpisode(loadedEpisodes, curSeason, curEpisode)
                        val prevEp = findPrevEpisode(loadedEpisodes, curSeason, curEpisode)

                        currentPosterUrl = detail.posterUrl
                        currentBackdropUrl = detail.backdropUrl ?: detail.posterUrl ?: currentEpItem?.thumbnail

                        _uiState.update {
                            it.copy(
                                mediaTitle = detail.title.ifBlank { it.mediaTitle },
                                episodeTitle = currentEpItem?.title ?: it.episodeTitle,
                                hasNextEpisode = nextEp != null,
                                upNextEpisode = nextEp,
                                hasPrevEpisode = prevEp != null,
                                availableEpisodes = detail.episodes,
                                availableSeasons = seasons.ifEmpty { listOf(1) },
                                isEpisodesLoading = false,
                                backdropUrl = currentBackdropUrl
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isEpisodesLoading = false) }
                    }
                }
                launch {
                    libraryRepository.getPlaybackForMedia(mediaId).collect { list ->
                        val map = list.mapNotNull { item ->
                            val s = item.seasonNumber
                            val e = item.episodeNumber
                            if (s != null && e != null) (s to e) to item else null
                        }.toMap()
                        _uiState.update { it.copy(episodePlaybackMap = map) }
                    }
                }
            } else {
                launch {
                    try {
                        val detail = repository.getDetails(mediaId, type, provider)
                        currentPosterUrl = detail.posterUrl
                        currentBackdropUrl = detail.backdropUrl ?: detail.posterUrl
                        _uiState.update {
                            it.copy(
                                mediaTitle = detail.title.ifBlank { it.mediaTitle },
                                episodeTitle = null,
                                seasonNumber = null,
                                episodeNumber = null,
                                hasNextEpisode = false,
                                hasPrevEpisode = false,
                                availableEpisodes = emptyList(),
                                availableSeasons = emptyList(),
                                isEpisodesLoading = false,
                                backdropUrl = currentBackdropUrl
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            if (resumePosition != null) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(resumeNotice = "Resumed from ${formatTimecode(resumePosition)}") }
                    resumeDismissJob?.cancel()
                    resumeDismissJob = viewModelScope.launch {
                        delay(3500)
                        _uiState.update { it.copy(resumeNotice = null) }
                    }
                }
            }

            // Check if media is downloaded locally for seamless offline playback
            val downloadedItem = if (isTv) {
                downloadRepository.getDownloadedItem(mediaId, targetEpisodeData)
                    ?: downloadRepository.downloads.value.firstOrNull { item ->
                        downloadRepository.matchesItem(item, mediaId, targetEpisodeData) &&
                                item.state == com.euthopiar.download.DownloadState.COMPLETED
                    }
            } else {
                downloadRepository.getDownloadedItem(mediaId, null)
                    ?: downloadRepository.downloads.value.firstOrNull { item ->
                        downloadRepository.matchesItem(item, mediaId, null) &&
                                item.state == com.euthopiar.download.DownloadState.COMPLETED
                    }
            }
            if (downloadedItem != null) {
                currentPosterUrl = downloadedItem.posterUrl
                currentBackdropUrl = downloadedItem.backdropUrl
                _uiState.update {
                    it.copy(
                        isOfflinePlayback = true,
                        mediaTitle = downloadedItem.title.ifBlank { it.mediaTitle },
                        episodeTitle = downloadedItem.subtitle ?: it.episodeTitle,
                        backdropUrl = currentBackdropUrl
                    )
                }
                var localFile = downloadedItem.localFilePath?.let { java.io.File(it) }
                if (localFile != null && localFile.isDirectory) {
                    val indexFile = java.io.File(localFile, "index.m3u8")
                    if (indexFile.exists()) {
                        localFile = indexFile
                    }
                }
                if (localFile == null || !localFile.exists() || (!localFile.isDirectory && localFile.length() <= 0L)) {
                    try {
                        val dir = com.euthopiar.download.DownloadEngine(getApplication()).getDownloadDirectory()
                        val candidate = dir.listFiles()?.firstOrNull { f ->
                            f.name.contains(downloadedItem.id, ignoreCase = true) && !f.name.endsWith(".part") &&
                                    (if (f.isDirectory) java.io.File(f, "index.m3u8").exists() else f.length() > 0L)
                        }
                        if (candidate != null) {
                            localFile = if (candidate.isDirectory) java.io.File(candidate, "index.m3u8") else candidate
                        }
                    } catch (_: Exception) {}
                }
                val hasLocalFile = localFile != null && localFile.exists() &&
                        (if (localFile.isDirectory) java.io.File(localFile, "index.m3u8").exists() else localFile.length() > 0L)
                if (localFile != null && localFile.isDirectory) {
                    val idx = java.io.File(localFile, "index.m3u8")
                    if (idx.exists()) localFile = idx
                }
                var localDurationMs = 0L
                if (hasLocalFile && localFile != null) {
                    if (localFile.name.endsWith(".m3u8", ignoreCase = true) || localFile.name.contains("index.m3u8")) {
                        try {
                            var sumDuration = 0.0
                            localFile.forEachLine { line ->
                                if (line.startsWith("#EXTINF:")) {
                                    val sec = line.removePrefix("#EXTINF:").substringBefore(',').trim().toDoubleOrNull() ?: 0.0
                                    sumDuration += sec
                                }
                            }
                            localDurationMs = (sumDuration * 1000.0).toLong()
                        } catch (_: Throwable) {}
                    } else {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(localFile.absolutePath)
                            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            localDurationMs = durStr?.toLongOrNull() ?: 0L
                        } catch (e: Throwable) {
                            android.util.Log.w("PlayerViewModel", "MediaMetadataRetriever error: ${e.message}")
                        } finally {
                            try { retriever.release() } catch (_: Throwable) {}
                        }
                        if (localDurationMs <= 0L && (localFile.name.endsWith(".ts", ignoreCase = true) || sniffContainer(localFile) == ContainerType.TS)) {
                            localDurationMs = getTsFileDurationMs(localFile)
                        }
                    }
                }
                val targetUrl = if (hasLocalFile && localFile != null) {
                    android.net.Uri.fromFile(localFile).toString()
                } else {
                    downloadedItem.streamUrl
                }
                val offlineSource = StreamSource(
                    url = targetUrl,
                    quality = "${downloadedItem.qualityLabel} (Offline)",
                    isM3u8 = targetUrl.contains(".m3u8") || targetUrl.contains("/content?v="),
                    headers = downloadedItem.headers
                )
                val serverOption = ServerSourceOption(
                    id = "offline_cache",
                    serverName = "Offline Storage",
                    label = "Offline File (${downloadedItem.qualityLabel})",
                    streamSource = offlineSource,
                    latencyMs = 0L,
                    healthCategory = ServerHealthCategory.EXCELLENT
                )
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            availableServers = listOf(serverOption),
                            selectedServer = serverOption,
                            durationMs = if (localDurationMs > 0L) localDurationMs else it.durationMs
                        )
                    }
                    playStreamSource(offlineSource, emptyList(), seekToPosition = resumePosition, isOffline = true)
                }
                return@launch
            }

            // Resolve streaming links progressively via getStreamFlow
            var hasStartedPlayback = false
            try {
                android.util.Log.d("PlayerViewModel", "Calling getStreamFlow for $targetEpisodeData on provider $provider")
                val collectedStreams = mutableListOf<StreamSource>()
                val collectedSubtitles = mutableListOf<SubtitleTrack>()

                repository.getStreamFlow(targetEpisodeData, provider)
                    .collect { emission ->
                        when (emission) {
                            is com.euthopiar.core.model.StreamEmission.SourceFound -> {
                                val s = emission.source
                                collectedStreams.add(s)
                                streamResult = StreamResult(collectedStreams, collectedSubtitles)

                                val sIndex = collectedStreams.size
                                val rLabel = if (s.resolutionLabel.isNotBlank()) s.resolutionLabel else "Auto"
                                val cleanServerName = when {
                                    s.serverName.isNotBlank() && !s.serverName.equals("default", ignoreCase = true) && !s.serverName.startsWith("http") -> s.serverName
                                    s.quality.isNotBlank() && !s.quality.startsWith("http") -> s.quality
                                    else -> "Source $sIndex"
                                }
                                val parsedLangs = if (s.audioTracks.isNotEmpty()) {
                                    s.audioTracks.map { it.isoCode.ifBlank { it.languageName } }
                                } else {
                                    com.euthopiar.core.streams.AudioLanguageDiscovery.parseAudioLanguagesFromMetadata(
                                        cleanServerName + " " + s.url,
                                        currentOriginalLanguage ?: if (currentType == MediaType.ANIME) "ja" else null
                                    )
                                }
                                val aBadge = com.euthopiar.core.streams.AudioLanguageDiscovery.formatLanguageBadges(parsedLangs)
                                val option = ServerSourceOption(
                                    id = "server_${sIndex}_${s.url.hashCode()}",
                                    serverName = cleanServerName,
                                    resolutionLabel = rLabel,
                                    label = if (aBadge != null) "$cleanServerName · $rLabel • $aBadge" else if (cleanServerName.contains(rLabel, ignoreCase = true)) cleanServerName else "$cleanServerName · $rLabel",
                                    streamSource = s,
                                    audioBadge = aBadge
                                )

                                _uiState.update { state ->
                                    val updatedList = deduplicateAndSortServers(state.availableServers + option)
                                    android.util.Log.i("PlayerViewModel", "SourceFound: total=${updatedList.size}, torrents=${updatedList.count { it.streamSource.infoHash != null || it.streamSource.url.startsWith("magnet:") }}, latest=${option.label}")
                                    val updatedSelected = updatedList.find { it.id == state.selectedServer?.id }
                                        ?: updatedList.find { computeTransportKey(it.streamSource) == state.selectedServer?.let { s -> computeTransportKey(s.streamSource) } }
                                        ?: state.selectedServer
                                        ?: option
                                    state.copy(
                                        availableServers = updatedList,
                                        selectedServer = updatedSelected
                                    )
                                }
                                probeServerLatency(option)
                                if (aBadge == null) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        val discovered = if (s.isM3u8 || s.url.contains(".m3u8")) {
                                            com.euthopiar.core.streams.AudioLanguageDiscovery.probeHlsAudioTracks(s.url, s.headers)
                                        } else {
                                            com.euthopiar.core.streams.AudioLanguageDiscovery.probeContainerAudioTracks(s.url, s.headers)
                                        }
                                        val probedBadge = com.euthopiar.core.streams.AudioLanguageDiscovery.formatLanguageBadges(discovered)
                                        if (probedBadge != null) {
                                            _uiState.update { state ->
                                                val updated = state.availableServers.map { srv ->
                                                    if (srv.id == option.id) srv.copy(audioBadge = probedBadge) else srv
                                                }
                                                val cur = if (state.selectedServer?.id == option.id) state.selectedServer?.copy(audioBadge = probedBadge) else state.selectedServer
                                                state.copy(availableServers = updated, selectedServer = cur)
                                            }
                                        }
                                    }
                                }

                                // FAST PLAYBACK: Auto-start on the optimal source found or recover if previous errored or stalled!
                                val curSelected = _uiState.value.selectedServer
                                if (!hasStartedPlayback || _uiState.value.errorMessage != null || (curSelected != null && curSelected.id in failedServerIds)) {
                                    hasStartedPlayback = true
                                    withContext(Dispatchers.Main) {
                                        val targetServer = _uiState.value.availableServers.firstOrNull { it.id !in failedServerIds } ?: option
                                        _uiState.update { it.copy(isLoading = false, errorMessage = null, selectedServer = targetServer) }
                                        playStreamSource(targetServer.streamSource, collectedSubtitles, seekToPosition = resumePosition)
                                    }
                                }
                            }
                            is com.euthopiar.core.model.StreamEmission.SubtitleFound -> {
                                val rawLang = emission.track.language.trim()
                                val isCc = rawLang.contains("[CC]", ignoreCase = true)
                                val baseLang = rawLang.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()

                                val alreadyHasRegular = collectedSubtitles.any {
                                    val b = it.language.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()
                                    b == baseLang && !it.language.contains("[CC]", ignoreCase = true)
                                }
                                val alreadyHasCc = collectedSubtitles.any {
                                    val b = it.language.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()
                                    b == baseLang && it.language.contains("[CC]", ignoreCase = true)
                                }

                                val isDuplicateVariant = (isCc && alreadyHasCc) || (!isCc && alreadyHasRegular)
                                if (!isDuplicateVariant && collectedSubtitles.none { it.url == emission.track.url }) {
                                    collectedSubtitles.add(emission.track)
                                    streamResult = StreamResult(collectedStreams, collectedSubtitles)
                                    val opt = SubtitleOption(
                                        id = "sub_${collectedSubtitles.size}",
                                        label = emission.track.language,
                                        language = emission.track.language,
                                        url = emission.track.url
                                    )
                                    val isEng = emission.track.language.contains("eng", ignoreCase = true)
                                    val shouldAutoSelect = _uiState.value.selectedSubtitle.id == "off" && (isEng || collectedSubtitles.size == 1)
                                    _uiState.update { state ->
                                        state.copy(
                                            availableSubtitles = (state.availableSubtitles + opt).distinctBy { it.url ?: it.id },
                                            selectedSubtitle = if (shouldAutoSelect) opt else state.selectedSubtitle
                                        )
                                    }
                                    if (shouldAutoSelect && opt.url != null) {
                                        loadSubtitleCues(opt.url)
                                    }
                                }
                            }
                            is com.euthopiar.core.model.StreamEmission.StatusUpdate -> {
                                android.util.Log.d("PlayerViewModel", "StatusUpdate [${emission.serverName}]: ${emission.message}")
                            }
                        }
                    }

                if (!hasStartedPlayback && _uiState.value.availableServers.isEmpty()) {
                    if (isTv) {
                        val curSeason = season ?: 1
                        val curEpisode = episode ?: 1
                        val backupEp = loadedEpisodes.firstOrNull { it.seasonNumber == curSeason && it.episodeNumber == curEpisode }
                            ?: run {
                                try {
                                    val detail = repository.getDetails(mediaId, type, provider)
                                    loadedEpisodes = detail.episodes
                                    detail.episodes.firstOrNull { it.seasonNumber == curSeason && it.episodeNumber == curEpisode }
                                } catch (_: Exception) { null }
                            }

                        if (backupEp != null && backupEp.data.isNotBlank() && backupEp.data != targetEpisodeData) {
                            android.util.Log.i("PlayerViewModel", "Retrying getStreamFlow with fresh episode data for S${curSeason}E$curEpisode")
                            repository.getStreamFlow(backupEp.data, provider)
                                .collect { emission ->
                                    when (emission) {
                                        is com.euthopiar.core.model.StreamEmission.SourceFound -> {
                                            val s = emission.source
                                            collectedStreams.add(s)
                                            streamResult = StreamResult(collectedStreams, collectedSubtitles)
                                            val sIndex = collectedStreams.size
                                            val rLabel = if (s.resolutionLabel.isNotBlank()) s.resolutionLabel else "Auto"
                                            val cleanServerName = when {
                                                s.serverName.isNotBlank() && !s.serverName.equals("default", ignoreCase = true) && !s.serverName.startsWith("http") -> s.serverName
                                                s.quality.isNotBlank() && !s.quality.startsWith("http") -> s.quality
                                                else -> "Source $sIndex"
                                            }
                                            val parsedLangs = if (s.audioTracks.isNotEmpty()) {
                                                s.audioTracks.map { it.isoCode.ifBlank { it.languageName } }
                                            } else {
                                                com.euthopiar.core.streams.AudioLanguageDiscovery.parseAudioLanguagesFromMetadata(
                                                    cleanServerName + " " + s.url,
                                                    currentOriginalLanguage ?: if (currentType == MediaType.ANIME) "ja" else null
                                                )
                                            }
                                            val aBadge = com.euthopiar.core.streams.AudioLanguageDiscovery.formatLanguageBadges(parsedLangs)
                                            val option = ServerSourceOption(
                                                id = "server_${sIndex}_${s.url.hashCode()}",
                                                serverName = cleanServerName,
                                                resolutionLabel = rLabel,
                                                label = if (aBadge != null) "$cleanServerName · $rLabel • $aBadge" else if (cleanServerName.contains(rLabel, ignoreCase = true)) cleanServerName else "$cleanServerName · $rLabel",
                                                streamSource = s,
                                                audioBadge = aBadge
                                            )
                                            _uiState.update { state ->
                                                val updatedList = deduplicateAndSortServers(state.availableServers + option)
                                                val updatedSelected = updatedList.find { it.id == state.selectedServer?.id }
                                                    ?: updatedList.find { computeTransportKey(it.streamSource) == state.selectedServer?.let { s -> computeTransportKey(s.streamSource) } }
                                                    ?: state.selectedServer
                                                    ?: option
                                                state.copy(availableServers = updatedList, selectedServer = updatedSelected)
                                            }
                                            probeServerLatency(option)
                                            if (aBadge == null) {
                                                viewModelScope.launch(Dispatchers.IO) {
                                                    val discovered = if (s.isM3u8 || s.url.contains(".m3u8")) {
                                                        com.euthopiar.core.streams.AudioLanguageDiscovery.probeHlsAudioTracks(s.url, s.headers)
                                                    } else {
                                                        com.euthopiar.core.streams.AudioLanguageDiscovery.probeContainerAudioTracks(s.url, s.headers)
                                                    }
                                                    val probedBadge = com.euthopiar.core.streams.AudioLanguageDiscovery.formatLanguageBadges(discovered)
                                                    if (probedBadge != null) {
                                                        _uiState.update { state ->
                                                            val updated = state.availableServers.map { srv ->
                                                                if (srv.id == option.id) srv.copy(audioBadge = probedBadge) else srv
                                                            }
                                                            val cur = if (state.selectedServer?.id == option.id) state.selectedServer?.copy(audioBadge = probedBadge) else state.selectedServer
                                                            state.copy(availableServers = updated, selectedServer = cur)
                                                        }
                                                    }
                                                }
                                            }
                                            if (!hasStartedPlayback) {
                                                hasStartedPlayback = true
                                                withContext(Dispatchers.Main) {
                                                    val targetServer = _uiState.value.availableServers.firstOrNull { it.id !in failedServerIds } ?: option
                                                    _uiState.update { it.copy(isLoading = false, errorMessage = null, selectedServer = targetServer) }
                                                    playStreamSource(targetServer.streamSource, collectedSubtitles, seekToPosition = resumePosition)
                                                }
                                                if (collectedSubtitles.isEmpty()) {
                                                    launch(Dispatchers.IO) {
                                                        autoFetchExternalSubtitlesIfNeeded(title ?: mediaId, season, episode)
                                                    }
                                                }
                                            }
                                        }
                                        is com.euthopiar.core.model.StreamEmission.SubtitleFound -> {
                                            val rawLang = emission.track.language.trim()
                                            val isCc = rawLang.contains("[CC]", ignoreCase = true)
                                            val baseLang = rawLang.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()

                                            val alreadyHasRegular = collectedSubtitles.any {
                                                val b = it.language.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()
                                                b == baseLang && !it.language.contains("[CC]", ignoreCase = true)
                                            }
                                            val alreadyHasCc = collectedSubtitles.any {
                                                val b = it.language.replace(Regex("""\s*\[CC\]""", RegexOption.IGNORE_CASE), "").replace(Regex("""\s*#\d+"""), "").trim().lowercase()
                                                b == baseLang && it.language.contains("[CC]", ignoreCase = true)
                                            }

                                            val isDuplicateVariant = (isCc && alreadyHasCc) || (!isCc && alreadyHasRegular)
                                            if (!isDuplicateVariant && collectedSubtitles.none { it.url == emission.track.url }) {
                                                collectedSubtitles.add(emission.track)
                                                streamResult = StreamResult(collectedStreams, collectedSubtitles)
                                                val opt = SubtitleOption(id = "sub_${collectedSubtitles.size}", label = emission.track.language, language = emission.track.language, url = emission.track.url)
                                                val isEng = emission.track.language.contains("eng", ignoreCase = true)
                                                val shouldAutoSelect = _uiState.value.selectedSubtitle.id == "off" && (isEng || collectedSubtitles.size == 1)
                                                _uiState.update { state ->
                                                    state.copy(
                                                        availableSubtitles = (state.availableSubtitles + opt).distinctBy { it.url ?: it.id },
                                                        selectedSubtitle = if (shouldAutoSelect) opt else state.selectedSubtitle
                                                    )
                                                }
                                                if (shouldAutoSelect && opt.url != null) {
                                                    loadSubtitleCues(opt.url)
                                                }
                                            }
                                        }
                                        is com.euthopiar.core.model.StreamEmission.StatusUpdate -> {}
                                    }
                                }
                        }
                    }
                }

                if (!hasStartedPlayback && _uiState.value.availableServers.isEmpty()) {
                    android.util.Log.w("PlayerViewModel", "getStreamFlow yielded 0 streams for $targetEpisodeData")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userError = com.euthopiar.ui.util.UserError.NoSources,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                android.util.Log.w("PlayerViewModel", "Stream extraction failed for $targetEpisodeData", e)
                if (!hasStartedPlayback && _uiState.value.selectedServer == null && _uiState.value.availableServers.isEmpty() && exoPlayer?.isPlaying != true) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userError = e.toUserError(),
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun buildMediaSource(
        source: StreamSource,
        subtitles: List<SubtitleTrack> = emptyList(),
        isOffline: Boolean = false
    ): MediaSource {
        val context = getApplication<Application>()
        val uri = if (source.url.startsWith("/")) Uri.fromFile(java.io.File(source.url)) else Uri.parse(source.url)
        val urlLower = source.url.lowercase()
        val cleanUrlPath = source.url.substringBefore('?').substringBefore('#')

        val requestHeaders = com.euthopiar.core.network.PlayerPlaybackNetworking.sanitizeHeaders(source.headers)
        val userAgent = requestHeaders.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
            ?: AppUserAgent.get(context)

        val playbackHttpClient = com.euthopiar.core.network.PlayerPlaybackNetworking.createClient(webViewCookieJar)

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(playbackHttpClient)
            .setUserAgent(userAgent)
        okHttpDataSourceFactory.setDefaultRequestProperties(requestHeaders)

        val overridingDataSourceFactory = ResponseHeaderOverridingDataSource.Factory(
            upstreamFactory = okHttpDataSourceFactory,
            overriddenHeaders = source.responseHeaders
        )

        val isLocalFile = source.url.startsWith("file://") || source.url.startsWith("/")
        val isOfflinePlayback = isOffline || _uiState.value.isOfflinePlayback
        val isDownloaded = isOfflinePlayback ||
                downloadRepository.isMediaDownloaded(currentMediaId ?: "", currentEpisodeData)

        val dataSourceFactory: androidx.media3.datasource.DataSource.Factory = when {
            isLocalFile -> DefaultDataSource.Factory(context)
            isOfflinePlayback -> downloadRepository.getOfflineCacheDataSourceFactory()
            isDownloaded -> downloadRepository.getCacheDataSourceFactory(requestHeaders)
            else -> DefaultDataSource.Factory(context, overridingDataSourceFactory)
        }

        val isDashSource = urlLower.contains(".mpd") || urlLower.contains("dash")
        val cleanUrl = source.url.substringBefore('#')
        val playUri = if (source.url.startsWith("http://", ignoreCase = true) || source.url.startsWith("https://", ignoreCase = true)) {
            Uri.parse(cleanUrl)
        } else {
            uri
        }

        val subtitleConfigs = subtitles.map { sub ->
            val mimeType = if (sub.url.contains(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(mimeType)
                .setLanguage(sub.language)
                .setLabel(sub.language)
                .setSelectionFlags(0)
                .build()
        }

        val mediaItem = MediaItem.Builder()
            .setUri(playUri)
            .apply {
                if (isLocalFile) {
                    try {
                        val filePath = if (source.url.startsWith("file://")) {
                            Uri.parse(source.url).path ?: source.url.removePrefix("file://")
                        } else {
                            source.url
                        }
                        val localFileOnDisk = java.io.File(filePath)
                        if (localFileOnDisk.exists() && localFileOnDisk.length() >= 188) {
                            val headerBytes = ByteArray(188)
                            java.io.FileInputStream(localFileOnDisk).use { it.read(headerBytes) }
                            val headerStr = String(headerBytes, Charsets.ISO_8859_1)
                            if (headerStr.contains("ftyp") || headerStr.contains("moov") || headerStr.contains("moof")) {
                                setMimeType(MimeTypes.VIDEO_MP4)
                            } else if (headerBytes[0] == 0x47.toByte()) {
                                setMimeType(MimeTypes.VIDEO_MP2T)
                            } else if (headerBytes[0] == 0x1A.toByte() && headerBytes[1] == 0x45.toByte() && headerBytes[2] == 0xDF.toByte() && headerBytes[3] == 0xA3.toByte()) {
                                setMimeType(MimeTypes.VIDEO_MATROSKA)
                            }
                        }
                    } catch (_: Throwable) {}
                }
                val contentType = source.headers.entries
                    .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value?.lowercase()
                when {
                    source.isM3u8 || urlLower.contains(".m3u8") || urlLower.contains("hls") || urlLower.contains("/content?v=") || contentType?.contains("mpegurl") == true || contentType?.contains("m3u8") == true -> {
                        setMimeType(MimeTypes.APPLICATION_M3U8)
                    }
                    urlLower.contains(".mpd") || urlLower.contains("dash") || contentType?.contains("dash") == true || contentType?.contains("mpd") == true -> {
                        setMimeType(MimeTypes.APPLICATION_MPD)
                    }
                    cleanUrlPath.endsWith(".mp4", ignoreCase = true) || urlLower.contains(".mp4") || contentType?.contains("mp4") == true -> {
                        setMimeType(MimeTypes.VIDEO_MP4)
                    }
                    (cleanUrlPath.endsWith(".mkv", ignoreCase = true) || urlLower.contains(".mkv") || urlLower.contains("r2.dev") || urlLower.contains("r2.cloudflarestorage.com") || urlLower.contains("pixeldrain.com") || urlLower.contains("hubcloud") || urlLower.contains("gamerxyt") || source.quality.contains("MKV", ignoreCase = true) || urlLower.contains("googleusercontent.com") || urlLower.contains("video-downloads") || urlLower.contains("video-seed.dev") || contentType?.contains("matroska") == true || contentType?.contains("mkv") == true) && !isLocalFile -> {
                        setMimeType(MimeTypes.VIDEO_MATROSKA)
                    }
                    cleanUrlPath.endsWith(".webm", ignoreCase = true) || urlLower.contains(".webm") || contentType?.contains("webm") == true -> {
                        setMimeType(MimeTypes.VIDEO_WEBM)
                    }
                }
            }
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        android.util.Log.d("PlayerViewModel", "buildMediaSource: url=${source.url}, isM3u8=${source.isM3u8}")

        // 2. Format Sniffing & MediaSource Dispatch
        return when {
            // HLS (Adaptive Master Playlists & Media Playlists)
            source.isM3u8 || urlLower.contains(".m3u8") || urlLower.contains("hls") || urlLower.contains("/content?v=") ||
                    mediaItem.localConfiguration?.mimeType == MimeTypes.APPLICATION_M3U8 -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(false)
                    .createMediaSource(mediaItem)
            }
            // MPEG-DASH
            urlLower.contains(".mpd") || urlLower.contains("dash") -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            // Direct Progressive MP4 / MKV / WebM / Cloud Download Links
            else -> {
                val progressiveExtractorsFactory: ExtractorsFactory = if (isLocalFile) {
                    val filePath = if (source.url.startsWith("file://")) {
                        Uri.parse(source.url).path ?: source.url.removePrefix("file://")
                    } else {
                        source.url
                    }
                    val localFile = java.io.File(filePath)
                    val knownDurationUs = if (_uiState.value.durationMs > 0L) {
                        _uiState.value.durationMs * 1000L
                    } else {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(localFile.absolutePath)
                            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val parsedMs = durStr?.toLongOrNull() ?: 0L
                            retriever.release()
                            if (parsedMs > 0L) {
                                _uiState.update { it.copy(durationMs = parsedMs) }
                                parsedMs * 1000L
                            } else 0L
                        } catch (_: Exception) {
                            0L
                        }
                    }
                    val container = sniffContainer(localFile)
                    when (container) {
                        ContainerType.TS -> {
                            val tsIndex = getOrBuildTsIndex(localFile, knownDurationUs)
                            ExtractorsFactory {
                                val list = mutableListOf<Extractor>()
                                list.add(IndexedTsExtractor(tsIndex))
                                list.addAll(
                                    DefaultExtractorsFactory()
                                        .setConstantBitrateSeekingEnabled(true)
                                        .setConstantBitrateSeekingAlwaysEnabled(true)
                                        .setTsExtractorFlags(
                                            DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                                            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                                            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                                        )
                                        .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
                                        .createExtractors()
                                )
                                list.toTypedArray()
                            }
                        }
                        ContainerType.FMP4 -> {
                            SeekableExtractorsFactory(knownDurationUs, localFile)
                        }
                        else -> {
                            DefaultExtractorsFactory()
                                .setConstantBitrateSeekingEnabled(true)
                                .setConstantBitrateSeekingAlwaysEnabled(true)
                                .setTsExtractorFlags(
                                    DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                                )
                                .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
                        }
                    }
                } else {
                    DefaultExtractorsFactory()
                        .setConstantBitrateSeekingEnabled(true)
                        .setConstantBitrateSeekingAlwaysEnabled(true)
                        .setTsExtractorFlags(
                            DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                        )
                        .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
                }
                ProgressiveMediaSource.Factory(dataSourceFactory, progressiveExtractorsFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    private fun extractP2pStreamRequest(rawUrl: String): com.euthopiar.core.p2p.P2pStreamRequest? =
        com.euthopiar.core.p2p.extractP2pStreamRequest(rawUrl)

    private fun playStreamSource(
        source: StreamSource,
        subtitles: List<SubtitleTrack>,
        seekToPosition: Long? = null,
        isOffline: Boolean = false
    ) {

        val cleanUrl = source.url.trim()
        val isHttp = cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true)
        val isTorrent = !isHttp && (cleanUrl.startsWith("magnet:", ignoreCase = true) ||
                cleanUrl.startsWith("torrent://", ignoreCase = true) ||
                cleanUrl.startsWith("torrent:", ignoreCase = true) ||
                com.euthopiar.core.p2p.extractP2pStreamRequest(cleanUrl) != null)

        if (!isTorrent && !source.url.startsWith("http://127.0.0.1") && !source.url.startsWith("http://localhost") && !source.url.startsWith("http://[::1]")) {
            try {
                com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
            } catch (_: Throwable) {}
            p2pStateJob?.cancel()
        }

        val urlLower = source.url.lowercase()
        val isLoopback = urlLower.contains("://127.0.0.1") || urlLower.contains("://localhost") || urlLower.contains("://[::1]")
        val isAmbiguousHttp = isHttp && !isLoopback && !source.isM3u8 &&
                !urlLower.contains(".m3u8") &&
                !urlLower.contains(".mpd") &&
                !urlLower.contains(".mp4") &&
                !urlLower.contains(".mkv") &&
                !urlLower.contains(".webm") &&
                !urlLower.contains("/content?v=")

        if (isTorrent || isAmbiguousHttp) {
            viewModelScope.launch {
                var currentSource = source

                // 1. Debrid unrestrict for magnets or info hashes
                if (isTorrent) {
                    _uiState.update { it.copy(isLoading = true, isBuffering = true) }
                    val activeProviderStr = preferencesRepository.activeDebridProvider.firstOrNull() ?: "NONE"
                    val pType = com.euthopiar.core.debrid.DebridProviderType.fromId(activeProviderStr)
                    var apiKey = when (pType) {
                        com.euthopiar.core.debrid.DebridProviderType.REAL_DEBRID -> preferencesRepository.realDebridApiKey.firstOrNull() ?: ""
                        com.euthopiar.core.debrid.DebridProviderType.ALL_DEBRID -> preferencesRepository.allDebridApiKey.firstOrNull() ?: ""
                        com.euthopiar.core.debrid.DebridProviderType.TORBOX -> preferencesRepository.torBoxApiKey.firstOrNull() ?: ""
                        com.euthopiar.core.debrid.DebridProviderType.NONE -> ""
                    }
                    var effectiveProvider = pType
                    if (apiKey.isBlank()) {
                        val rdKey = preferencesRepository.realDebridApiKey.firstOrNull() ?: ""
                        if (rdKey.isNotBlank()) {
                            apiKey = rdKey
                            effectiveProvider = com.euthopiar.core.debrid.DebridProviderType.REAL_DEBRID
                        }
                    }

                    if (apiKey.isBlank()) {
                        // Route through P2P Streaming Engine when no Debrid key is present
                        val p2pRequest = extractP2pStreamRequest(currentSource.url)
                        if (p2pRequest == null) {
                            val err = "Real-Debrid API Key required or invalid torrent link"
                            showToast(err)
                            _uiState.update { it.copy(isLoading = false, isBuffering = false, errorMessage = err) }
                            return@launch
                        }

                        try {
                            _uiState.update { it.copy(isLoading = true, isBuffering = true) }
                            com.euthopiar.core.p2p.P2pStreamingEngine.initialize(getApplication())
                            val loopbackUrl = com.euthopiar.core.p2p.P2pStreamingEngine.startStream(p2pRequest)
                            observeP2pEngineState()
                            currentSource = currentSource.copy(url = loopbackUrl)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            val err = e.message ?: "Failed to start P2P stream"
                            showToast(err)
                            _uiState.update { it.copy(isLoading = false, isBuffering = false, errorMessage = err) }
                            return@launch
                        }
                    } else {
                        val resolveResult = withContext(Dispatchers.IO) {
                            com.euthopiar.core.debrid.DebridManager.getInstance().resolveStream(
                                effectiveProvider,
                                apiKey,
                                currentSource.url
                            )
                        }

                        if (resolveResult.isSuccess) {
                            currentSource = resolveResult.getOrThrow()
                        } else {
                            // If Debrid resolution fails, attempt fallback to native P2P engine
                            val p2pRequest = extractP2pStreamRequest(currentSource.url)
                            if (p2pRequest != null) {
                                try {
                                    com.euthopiar.core.p2p.P2pStreamingEngine.initialize(getApplication())
                                    val loopbackUrl = com.euthopiar.core.p2p.P2pStreamingEngine.startStream(p2pRequest)
                                    observeP2pEngineState()
                                    currentSource = currentSource.copy(url = loopbackUrl)
                                } catch (p2pErr: Exception) {
                                    if (p2pErr is kotlinx.coroutines.CancellationException) throw p2pErr
                                    val err = resolveResult.exceptionOrNull()?.message ?: "Debrid resolution failed"
                                    showToast("Debrid resolution failed: $err")
                                    _uiState.update { it.copy(isLoading = false, isBuffering = false, errorMessage = err) }
                                    return@launch
                                }
                            } else {
                                val err = resolveResult.exceptionOrNull()?.message ?: "Unknown Debrid error"
                                showToast("Debrid resolution failed: $err")
                                _uiState.update { it.copy(isLoading = false, isBuffering = false, errorMessage = err) }
                                return@launch
                            }
                        }
                    }
                }

                // 2. Active HEAD probe for ambiguous HTTP URLs to detect HLS playlists
                val effectiveUrlLower = currentSource.url.lowercase()
                val isEffectiveLoopback = effectiveUrlLower.contains("://127.0.0.1") ||
                        effectiveUrlLower.contains("://localhost") ||
                        effectiveUrlLower.contains("://[::1]")
                if (!isEffectiveLoopback && currentSource.url.startsWith("http", ignoreCase = true) && !currentSource.isM3u8 &&
                    !effectiveUrlLower.contains(".m3u8") && !effectiveUrlLower.contains(".mpd") &&
                    !effectiveUrlLower.contains(".mp4") && !effectiveUrlLower.contains(".mkv") &&
                    !effectiveUrlLower.contains(".webm") && !effectiveUrlLower.contains("/content?v=")) {
                    try {
                        val probeRequest = Request.Builder()
                            .url(currentSource.url)
                            .head()
                            .apply {
                                currentSource.headers.forEach { (k, v) -> addHeader(k, v) }
                            }
                            .build()
                        val response = withContext(Dispatchers.IO) {
                            com.euthopiar.core.network.PlayerPlaybackNetworking.playbackHttpClient.newCall(probeRequest).execute()
                        }
                        val ct = response.header("Content-Type")?.lowercase() ?: ""
                        val finalUrl = response.request.url.toString().lowercase()
                        response.close()
                        if (ct.contains("mpegurl") || ct.contains("m3u8") || finalUrl.contains(".m3u8")) {
                            currentSource = currentSource.copy(isM3u8 = true)
                        }
                    } catch (_: Throwable) {
                        // Keep source as-is on probe failure
                    }
                }

                playStreamSourceInternal(currentSource, subtitles, seekToPosition, isOffline)
            }
            return
        }

        playStreamSourceInternal(source, subtitles, seekToPosition, isOffline)
    }

    private fun playStreamSourceInternal(
        source: StreamSource,
        subtitles: List<SubtitleTrack>,
        seekToPosition: Long? = null,
        isOffline: Boolean = false
    ) {
        val player = exoPlayer ?: return
        videoFrameRendered = false
        player.stop()
        continuousPlayTimeMs = 0L

        val activeScraper = com.euthopiar.core.js.JsPluginStorage.getInstanceOrNull()?.scrapersFlow?.value?.find {
            it.name.equals(currentProvider, ignoreCase = true) || it.id.equals(currentProvider, ignoreCase = true)
        }
        currentCanonicalStream = com.euthopiar.core.model.CanonicalStream(
            mediaId = currentMediaId.orEmpty(),
            title = _uiState.value.mediaTitle,
            uri = source.url,
            headers = source.headers,
            responseHeaders = source.responseHeaders,
            quality = source.resolutionLabel.ifBlank { source.quality }.ifBlank { "Auto" },
            isM3u8 = source.isM3u8,
            repoId = activeScraper?.canonicalRepoId,
            pluginId = activeScraper?.id ?: currentProvider
        )

        val mId = currentMediaId
        if (mId != null && !isOffline) {
            val mType = if (currentType == MediaType.TV_SERIES) "tv" else "movie"
            activeRefreshableSource = com.euthopiar.core.streams.RefreshableSource(
                scraperId = currentProvider ?: "tmdb",
                mediaType = mType,
                videoId = mId,
                season = _uiState.value.seasonNumber,
                episode = _uiState.value.episodeNumber,
                quality = source.resolutionLabel.ifBlank { null },
                host = try { java.net.URI(source.url).host } catch (_: Exception) { null },
                name = source.serverName.ifBlank { null },
                initialUrl = source.url,
                initialHeaders = source.headers
            )
        } else {
            activeRefreshableSource = null
        }

        val mediaSource = buildMediaSource(source, subtitles, isOffline)

        // Initial default resolution options linked to active source
        val defaultQualities = listOf(
            VideoQualityOption("auto", "Auto (Adaptive)"),
            VideoQualityOption("height_1080", "1080p (Full HD)", 1080),
            VideoQualityOption("height_720", "720p (HD)", 720),
            VideoQualityOption("height_480", "480p (SD)", 480),
            VideoQualityOption("height_360", "360p (SD)", 360)
        )

        // Subtitles list
        val subtitleOptions = mutableListOf(SubtitleOption("off", "Off"))
        var defaultSubtitle: SubtitleOption? = null
        subtitles.forEachIndexed { idx, sub ->
            val opt = SubtitleOption(
                id = "sub_$idx",
                label = sub.language,
                language = sub.language,
                url = sub.url
            )
            subtitleOptions.add(opt)
            if (defaultSubtitle == null && (sub.language.contains("eng", ignoreCase = true) || idx == 0)) {
                defaultSubtitle = opt
            }
        }

        val initialSubtitle = defaultSubtitle ?: SubtitleOption("off", "Off")
        if (initialSubtitle.url != null) {
            loadSubtitleCues(initialSubtitle.url)
        } else {
            activeSubtitleCues = emptyList()
            _currentSubtitleText.value = null
        }

        val activeServer = _uiState.value.selectedServer
        val initialQualities = defaultQualities.map {
            it.copy(latencyMs = activeServer?.latencyMs, healthCategory = activeServer?.healthCategory)
        }
        _uiState.update {
            it.copy(
                availableQualities = initialQualities,
                selectedQuality = VideoQualityOption("auto", "Auto (Adaptive)", latencyMs = activeServer?.latencyMs, healthCategory = activeServer?.healthCategory),
                availableSubtitles = subtitleOptions,
                selectedSubtitle = initialSubtitle,
                availableAudioTracks = listOf(AudioTrackOption("auto", "Auto (Default)", isAuto = true)),
                selectedAudioTrack = AudioTrackOption("auto", "Auto (Default)", isAuto = true),
                errorMessage = null,
                isLoading = false,
                isBuffering = true
            )
        }

        val estimatedBps = com.euthopiar.core.network.BandwidthEstimator.Default.getEstimatedBandwidthBps(getApplication())
        val autoTier = com.euthopiar.core.network.BandwidthEstimator.Default.decideAutoQuality(estimatedBps)
        val maxH = when (autoTier) {
            "480p" -> 480
            "720p" -> 720
            "1080p" -> 1080
            else -> Int.MAX_VALUE
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearVideoSizeConstraints()
            .setMaxVideoSize(Int.MAX_VALUE, maxH)
            .setMinVideoSize(0, 0)
            .setPreferredVideoMimeType(null)
            .build()

        if (seekToPosition != null && seekToPosition > 0L) {
            player.setMediaSource(mediaSource, seekToPosition)
        } else {
            player.setMediaSource(mediaSource, /* resetPosition = */ true)
        }
        player.prepare()
        player.playWhenReady = true
    }

    private fun extractExoPlayerTracks(tracks: Tracks) {
        val detectedResolutions = mutableListOf<VideoQualityOption>()
        detectedResolutions.add(VideoQualityOption("auto", "Auto (Adaptive)"))

        val detectedAudio = mutableListOf<AudioTrackOption>()
        detectedAudio.add(AudioTrackOption("auto", "Auto (Default)", isAuto = true))
        var currentSelectedAudio = _uiState.value.selectedAudioTrack

        val detectedSubs = mutableListOf<SubtitleOption>()

        for ((groupIndex, group) in tracks.groups.withIndex()) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val h = format.height
                    if (h > 0) {
                        val label = when {
                            h >= 2160 -> "4K (2160p)"
                            h >= 1440 -> "2K (1440p)"
                            h >= 1080 -> "1080p (Full HD)"
                            h >= 720 -> "720p (HD)"
                            h >= 480 -> "480p (SD)"
                            else -> "${h}p"
                        }
                        if (detectedResolutions.none { it.height == h }) {
                            detectedResolutions.add(VideoQualityOption(id = "height_$h", label = label, height = h))
                        }
                    }
                }
            } else if (group.type == C.TRACK_TYPE_AUDIO) {
                var firstPlayableIndex: Int? = null
                var currentlySelectedIndex: Int? = null
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSupported = group.isTrackSupported(i)
                    val isSelected = group.isTrackSelected(i)
                    if (isSelected) currentlySelectedIndex = i
                    if (isSupported && firstPlayableIndex == null) firstPlayableIndex = i

                    val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
                    val labelBuilder = StringBuilder()
                    if (!format.label.isNullOrBlank()) {
                        labelBuilder.append(format.label)
                    } else if (lang != null) {
                        val displayLang = try {
                            java.util.Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }
                        } catch (_: Exception) {
                            lang.uppercase()
                        }
                        labelBuilder.append(displayLang)
                    } else {
                        labelBuilder.append("Audio Track ${detectedAudio.size}")
                    }

                    val codecName = when {
                        format.sampleMimeType?.contains("eac3-joc", ignoreCase = true) == true -> "DDP Atmos"
                        format.sampleMimeType?.contains("eac3", ignoreCase = true) == true -> "DDP 5.1"
                        format.sampleMimeType?.contains("ac3", ignoreCase = true) == true -> "AC3"
                        format.sampleMimeType?.contains("true-hd", ignoreCase = true) == true -> "TrueHD Atmos"
                        format.sampleMimeType?.contains("dts", ignoreCase = true) == true -> "DTS"
                        format.sampleMimeType?.contains("mp4a", ignoreCase = true) == true || format.sampleMimeType?.contains("aac", ignoreCase = true) == true -> "AAC"
                        format.sampleMimeType?.contains("opus", ignoreCase = true) == true -> "Opus"
                        else -> null
                    }

                    if (format.channelCount > 0) {
                        val channels = when (format.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1 Surround"
                            8 -> "7.1 Surround"
                            else -> "${format.channelCount} Ch"
                        }
                        if (codecName != null) {
                            labelBuilder.append(" ($channels • $codecName)")
                        } else {
                            labelBuilder.append(" ($channels)")
                        }
                    } else if (codecName != null) {
                        labelBuilder.append(" ($codecName)")
                    }

                    val optId = "audio_${groupIndex}_$i"
                    val audioOpt = AudioTrackOption(
                        id = optId,
                        label = labelBuilder.toString(),
                        language = lang,
                        isoCode = lang ?: "und",
                        displayName = labelBuilder.toString(),
                        isSelected = isSelected,
                        trackIndex = i,
                        groupIndex = groupIndex,
                        isAuto = false
                    )
                    detectedAudio.add(audioOpt)
                    if (isSelected && _uiState.value.selectedAudioTrack.isAuto) {
                        currentSelectedAudio = audioOpt
                    }
                }

                // Auto-fallback: if the current selected track is not supported (silent/TrueHD), switch to a playable track!
                if (currentlySelectedIndex != null && !group.isTrackSupported(currentlySelectedIndex) && firstPlayableIndex != null) {
                    val targetOpt = detectedAudio.firstOrNull { it.trackIndex == firstPlayableIndex && it.groupIndex == groupIndex }
                    if (targetOpt != null) {
                        selectAudioTrack(targetOpt)
                    }
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
                    val label = format.label ?: lang?.let {
                        try {
                            java.util.Locale(it).displayLanguage.replaceFirstChar { c -> c.uppercase() }
                        } catch (_: Exception) {
                            it.uppercase()
                        }
                    } ?: "Track ${detectedSubs.size + 1}"

                    val subOpt = SubtitleOption(
                        id = "embedded_${groupIndex}_$i",
                        label = "$label (Embedded)",
                        language = lang,
                        url = null
                    )
                    detectedSubs.add(subOpt)
                }
            }
        }

        if (detectedResolutions.size > 1) {
            val activeServer = _uiState.value.selectedServer
            val mappedResolutions = detectedResolutions.map {
                it.copy(latencyMs = activeServer?.latencyMs, healthCategory = activeServer?.healthCategory)
            }.sortedByDescending { it.height ?: 9999 }
            _uiState.update { it.copy(availableQualities = mappedResolutions) }

            // Auto-prefer highest available quality (e.g. 1080p Full HD) or honor user quality preference
            val currentQuality = _uiState.value.selectedQuality
            val targetHeight = when {
                userDefaultQuality.contains("2160") -> 2160
                userDefaultQuality.contains("1080") -> 1080
                userDefaultQuality.contains("720") -> 720
                userDefaultQuality.contains("480") -> 480
                currentQuality.height != null -> currentQuality.height
                else -> 1080 // Default to 1080p Full HD if available
            }

            val targetOption = mappedResolutions.firstOrNull { it.height == targetHeight }
                ?: mappedResolutions.filter { it.height != null }.maxByOrNull { it.height ?: 0 }

            if (targetOption != null && targetOption.height != null) {
                if (currentQuality.id != "auto" && currentQuality.height != null) {
                    _uiState.update { it.copy(selectedQuality = targetOption) }
                    exoPlayer?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoSize(Int.MAX_VALUE, targetOption.height)
                            .setMinVideoSize(0, 0)
                            .build()
                    }
                } else {
                    // Auto adaptive: allow player to seamlessly select best playable rendition without hard constraints
                    exoPlayer?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearVideoSizeConstraints()
                            .build()
                    }
                }
            }
        }

        if (detectedAudio.size > 1) {
            val activeSelected = _uiState.value.selectedAudioTrack
            _uiState.update {
                it.copy(
                    availableAudioTracks = detectedAudio.map { opt ->
                        opt.copy(isSelected = opt.id == activeSelected.id)
                    },
                    selectedAudioTrack = if (activeSelected.id in detectedAudio.map { a -> a.id }) activeSelected else detectedAudio.first()
                )
            }
        }

        if (detectedSubs.isNotEmpty()) {
            _uiState.update { state ->
                val existing = state.availableSubtitles
                val merged = (existing + detectedSubs).distinctBy { it.label }
                state.copy(availableSubtitles = merged)
            }
        }

        // Video track validation: ensure at least one video track is selected if video tracks exist
        val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        if (videoGroups.isNotEmpty()) {
            val hasSelectedVideo = videoGroups.any { group ->
                (0 until group.length).any { group.isTrackSelected(it) }
            }
            if (!hasSelectedVideo) {
                android.util.Log.w("PlayerViewModel", "No video track selected in video group! Attempting force-selection of playable track...")
                var forceSelected = false
                for (group in videoGroups) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSupported(i)) {
                            android.util.Log.i("PlayerViewModel", "Force-selecting supported video track at index $i")
                            exoPlayer?.trackSelectionParameters = exoPlayer?.trackSelectionParameters?.buildUpon()
                                ?.setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                                )
                                ?.build() ?: break
                            forceSelected = true
                            break
                        }
                    }
                    if (forceSelected) break
                }
                if (_uiState.value.smartFailoverEnabled && !forceSelected && !userExplicitServerSelection) {
                    // All video tracks in this source are unsupported by device decoder. Gracefully fallback!
                    val currentServer = _uiState.value.selectedServer
                    if (currentServer != null) {
                        failedServerIds.add(currentServer.id)
                    }
                    val non4kFallback = _uiState.value.availableServers.firstOrNull {
                        it.id !in failedServerIds &&
                                !it.resolutionLabel.contains("2160") &&
                                !it.resolutionLabel.contains("4k", ignoreCase = true)
                    }
                    val fallback = non4kFallback ?: _uiState.value.availableServers.firstOrNull { it.id !in failedServerIds }
                    if (fallback != null) {
                        showToast("Switching to compatible video stream (${fallback.resolutionLabel})...")
                        selectServer(fallback)
                    }
                }
            }
        }
        updateMediaFormatInfo()
    }

    fun updateMediaFormatInfo() {
        val player = exoPlayer
        val vFormat = player?.videoFormat
        val aFormat = player?.audioFormat
        val server = _uiState.value.selectedServer
        val serverName = server?.serverName ?: ""
        val resLabel = server?.resolutionLabel ?: ""
        val currentAudioLabel = _uiState.value.selectedAudioTrack.label
        val combinedMeta = "$serverName $resLabel $currentAudioLabel"

        // Resolution: e.g. "4K", "1440p", "1080p", "720p"
        val height = vFormat?.height?.takeIf { it > 0 } ?: _uiState.value.selectedQuality.height ?: 0
        val width = vFormat?.width?.takeIf { it > 0 } ?: 0
        val resolutionBadge = when {
            height >= 2160 || width >= 3800 || combinedMeta.contains("4K", ignoreCase = true) || combinedMeta.contains("2160") -> "4K"
            height >= 1440 || width >= 2500 || combinedMeta.contains("1440") -> "1440p"
            height >= 1080 || width >= 1900 || combinedMeta.contains("1080") -> "1080p"
            height >= 720 || width >= 1200 || combinedMeta.contains("720") -> "720p"
            height in 1..719 -> "${height}p"
            else -> null
        }

        // Dynamic Range: Dolby Vision, HDR10+, HDR10, HLG HDR, HDR
        val mime = vFormat?.sampleMimeType ?: ""
        val codecs = vFormat?.codecs ?: ""
        val isDv = mime.contains("dolby-vision", ignoreCase = true) ||
                codecs.contains("dvh1", ignoreCase = true) ||
                codecs.contains("dvhe", ignoreCase = true) ||
                codecs.contains("dva1", ignoreCase = true) ||
                codecs.contains("dav1", ignoreCase = true) ||
                combinedMeta.contains("Dolby Vision", ignoreCase = true) ||
                combinedMeta.matches(Regex(".*\\b(DV|DoVi)\\b.*", RegexOption.IGNORE_CASE))

        val colorTransfer = vFormat?.colorInfo?.colorTransfer ?: C.INDEX_UNSET
        val colorSpace = vFormat?.colorInfo?.colorSpace ?: C.INDEX_UNSET
        val isHdr10Plus = combinedMeta.contains("HDR10+", ignoreCase = true)
        val isHdr10 = colorTransfer == C.COLOR_TRANSFER_ST2084 || combinedMeta.contains("HDR10", ignoreCase = true)
        val isHlg = colorTransfer == C.COLOR_TRANSFER_HLG || combinedMeta.contains("HLG", ignoreCase = true)
        val isGenericHdr = colorSpace == C.COLOR_SPACE_BT2020 || combinedMeta.contains("HDR", ignoreCase = true)

        val dynamicRangeBadge = when {
            isDv -> "DOLBY VISION"
            isHdr10Plus -> "HDR10+"
            isHdr10 -> "HDR10"
            isHlg -> "HLG HDR"
            isGenericHdr -> "HDR"
            else -> null
        }

        // Audio: Atmos, Dolby Audio (7.1, 5.1, EAC3), Multichannel Surround
        val aMime = aFormat?.sampleMimeType ?: ""
        val aCodecs = aFormat?.codecs ?: ""
        val channelCount = aFormat?.channelCount?.takeIf { it > 0 } ?: when {
            combinedMeta.contains("7.1", ignoreCase = true) -> 8
            combinedMeta.contains("5.1", ignoreCase = true) -> 6
            else -> 2
        }

        val isAtmos = aMime.contains("eac3-joc", ignoreCase = true) ||
                aCodecs.contains("joc", ignoreCase = true) ||
                combinedMeta.contains("Atmos", ignoreCase = true)

        val isDolbyAudio = isAtmos ||
                aMime.contains("eac3", ignoreCase = true) ||
                aMime.contains("ac3", ignoreCase = true) ||
                aMime.contains("ac-3", ignoreCase = true) ||
                aMime.contains("true-hd", ignoreCase = true) ||
                combinedMeta.contains("Dolby", ignoreCase = true) ||
                combinedMeta.contains("DDP", ignoreCase = true) ||
                combinedMeta.contains("AC3", ignoreCase = true) ||
                combinedMeta.contains("EAC3", ignoreCase = true)

        val audioBadge = when {
            isAtmos -> "DOLBY ATMOS"
            isDolbyAudio && channelCount >= 8 -> "DOLBY AUDIO 7.1"
            isDolbyAudio && channelCount >= 6 -> "DOLBY AUDIO 5.1"
            isDolbyAudio -> "DOLBY AUDIO"
            channelCount >= 8 -> "7.1 SURROUND"
            channelCount >= 6 -> "5.1 SURROUND"
            else -> null
        }

        val info = MediaFormatInfo(
            resolutionBadge = resolutionBadge,
            dynamicRangeBadge = dynamicRangeBadge,
            audioBadge = audioBadge,
            hasDolbyVision = isDv,
            hasDolbyAudio = isDolbyAudio,
            hasAtmos = isAtmos,
            hasHdr = isDv || isHdr10Plus || isHdr10 || isHlg || isGenericHdr
        )

        _uiState.update { it.copy(mediaFormatInfo = info) }
    }

    private val probedLatencies = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, ServerHealthCategory>>()

    private fun probeServerLatency(option: ServerSourceOption) {
        val url = option.streamSource.url
        if (url.startsWith("file://") || url.startsWith("content://") || option.id == "offline_cache") {
            updateServerLatency(option.id, 0L, ServerHealthCategory.EXCELLENT)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cached = probedLatencies[url]
            if (cached != null) {
                updateServerLatency(option.id, cached.first, cached.second)
                return@launch
            }

            val result = measureUrlLatency(url, option.streamSource.headers)
            probedLatencies[url] = result
            updateServerLatency(option.id, result.first, result.second)
        }
    }

    private fun observeP2pEngineState() {
        p2pStateJob?.cancel()
        p2pStateJob = viewModelScope.launch {
            com.euthopiar.core.p2p.P2pStreamingEngine.state.collect { st ->
                when (st) {
                    is com.euthopiar.core.p2p.P2pStreamingState.Streaming -> {
                        val phase = when {
                            st.peers == 0 && st.seeds == 0 -> "Searching DHT & trackers for peers..."
                            st.bufferProgress < 0.05f && st.downloadSpeed <= 0L -> "Retrieving torrent metadata..."
                            else -> "Pre-buffering video pieces · ${(st.bufferProgress * 100).toInt()}%"
                        }
                        _uiState.update {
                            it.copy(
                                isP2pStream = true,
                                p2pPeers = st.peers,
                                p2pSeeds = st.seeds,
                                p2pDownloadSpeed = st.downloadSpeed,
                                p2pBufferProgress = st.bufferProgress,
                                p2pPhaseLabel = phase
                            )
                        }
                    }
                    is com.euthopiar.core.p2p.P2pStreamingState.Connecting -> {
                        val phase = when {
                            st.peers == 0 && st.seeds == 0 -> "Searching DHT & trackers for peers..."
                            else -> "Retrieving torrent metadata..."
                        }
                        _uiState.update {
                            it.copy(
                                isP2pStream = true,
                                p2pPeers = st.peers,
                                p2pSeeds = st.seeds,
                                p2pDownloadSpeed = st.downloadSpeed,
                                p2pBufferProgress = 0f,
                                p2pPhaseLabel = phase
                            )
                        }
                    }
                    is com.euthopiar.core.p2p.P2pStreamingState.Error -> {
                        val err = st.message.ifBlank { "P2P streaming session error" }
                        showToast(err)
                        _uiState.update { it.copy(isLoading = false, isBuffering = false, errorMessage = err) }
                    }
                    is com.euthopiar.core.p2p.P2pStreamingState.Idle -> {
                        _uiState.update { it.copy(isP2pStream = false, p2pPhaseLabel = null) }
                    }
                }
            }
        }
    }

    private suspend fun measureUrlLatency(url: String, headers: Map<String, String>): Pair<Long, ServerHealthCategory> = withContext(Dispatchers.IO) {
        val urlLower = url.lowercase()
        if (extractP2pStreamRequest(url) != null ||
            urlLower.startsWith("magnet:") ||
            urlLower.startsWith("torrent:") ||
            urlLower.contains("127.0.0.1") ||
            urlLower.contains("localhost") ||
            urlLower.contains("[::1]")
        ) {
            return@withContext 10L to ServerHealthCategory.EXCELLENT
        }
        val start = System.currentTimeMillis()
        try {
            val req = Request.Builder()
                .url(url)
                .head()
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()

            streamingHttpClient.newCall(req).execute().use { resp ->
                val duration = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                val cat = when {
                    duration <= 150L -> ServerHealthCategory.EXCELLENT
                    duration <= 350L -> ServerHealthCategory.AVERAGE
                    duration <= 700L -> ServerHealthCategory.POOR
                    else -> ServerHealthCategory.UNWATCHABLE
                }
                duration to cat
            }
        } catch (_: Exception) {
            try {
                val start2 = System.currentTimeMillis()
                val req2 = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-1024")
                    .apply { headers.forEach { (k, v) -> if (k != "Range") header(k, v) } }
                    .build()

                streamingHttpClient.newCall(req2).execute().use { resp2 ->
                    val duration = (System.currentTimeMillis() - start2).coerceAtLeast(1L)
                    val cat = when {
                        duration <= 150L -> ServerHealthCategory.EXCELLENT
                        duration <= 350L -> ServerHealthCategory.AVERAGE
                        duration <= 700L -> ServerHealthCategory.POOR
                        else -> ServerHealthCategory.UNWATCHABLE
                    }
                    duration to cat
                }
            } catch (_: Exception) {
                999L to ServerHealthCategory.UNWATCHABLE
            }
        }
    }

    private fun updateServerLatency(optionId: String, latencyMs: Long, category: ServerHealthCategory) {
        _uiState.update { state ->
            val updatedServers = state.availableServers.map { s ->
                if (s.id == optionId) s.copy(latencyMs = latencyMs, healthCategory = category) else s
            }
            val isSelectedMatch = state.selectedServer?.id == optionId
            val updatedSelected = if (isSelectedMatch) {
                state.selectedServer?.copy(latencyMs = latencyMs, healthCategory = category)
            } else state.selectedServer

            val updatedQualities = if (isSelectedMatch) {
                state.availableQualities.map { q ->
                    q.copy(latencyMs = latencyMs, healthCategory = category)
                }
            } else state.availableQualities

            val updatedSelectedQuality = if (isSelectedMatch) {
                state.selectedQuality.copy(latencyMs = latencyMs, healthCategory = category)
            } else state.selectedQuality

            state.copy(
                availableServers = updatedServers,
                selectedServer = updatedSelected,
                availableQualities = updatedQualities,
                selectedQuality = updatedSelectedQuality
            )
        }
    }

    fun selectServer(serverOption: ServerSourceOption, isUserExplicit: Boolean = true) {
        val player = exoPlayer ?: return
        bufferingStallStartTime = null
        userExplicitServerSelection = isUserExplicit
        failedServerIds.remove(serverOption.id)

        if (isUserExplicit && serverOption.resolutionLabel.isNotBlank() && !serverOption.resolutionLabel.contains("Auto", ignoreCase = true)) {
            val estimated = com.euthopiar.core.network.BandwidthEstimator.Default.getEstimatedBandwidthBps(getApplication())
            val conflict = com.euthopiar.core.network.BandwidthEstimator.Default.checkQualityConflict(serverOption.resolutionLabel, estimated)
            if (conflict != null) {
                pendingServerOption = serverOption
                _uiState.update { it.copy(qualityConflictNotice = conflict) }
                return
            }
        }

        val currentPos = player.currentPosition.coerceAtLeast(0L)
        _uiState.update { state ->
            val updatedQualities = state.availableQualities.map { q ->
                q.copy(latencyMs = serverOption.latencyMs, healthCategory = serverOption.healthCategory)
            }
            val updatedSelectedQuality = state.selectedQuality.copy(
                latencyMs = serverOption.latencyMs,
                healthCategory = serverOption.healthCategory
            )
            state.copy(
                selectedServer = serverOption,
                availableQualities = updatedQualities,
                selectedQuality = updatedSelectedQuality
            )
        }
        playStreamSource(
            source = serverOption.streamSource,
            subtitles = streamResult?.subtitles ?: emptyList(),
            seekToPosition = currentPos
        )
        updateMediaFormatInfo()
        if (!isUserExplicit) {
            _uiState.update {
                it.copy(
                    failoverNotice = FailoverNotice(
                        serverName = serverOption.serverName,
                        resolutionLabel = serverOption.resolutionLabel.ifBlank { "Auto" }
                    )
                )
            }
        } else {
            showToast("Switched to ${serverOption.serverName}")
        }
    }

    fun dismissFailoverNotice() {
        _uiState.update { it.copy(failoverNotice = null) }
    }

    fun selectQuality(option: VideoQualityOption) {
        if (option.id != "auto" && option.label.isNotBlank() && !option.label.contains("Auto", ignoreCase = true)) {
            val estimated = com.euthopiar.core.network.BandwidthEstimator.Default.getEstimatedBandwidthBps(getApplication())
            val conflict = com.euthopiar.core.network.BandwidthEstimator.Default.checkQualityConflict(option.label, estimated)
            if (conflict != null) {
                pendingQualityOption = option
                _uiState.update { it.copy(qualityConflictNotice = conflict) }
                return
            }
        }
        applyQuality(option)
    }

    fun confirmSwitchToRecommendedQuality() {
        val conflict = _uiState.value.qualityConflictNotice ?: return
        val rec = conflict.recommendedQuality
        _uiState.update { it.copy(qualityConflictNotice = null) }
        val targetServer = pendingServerOption
        val targetOpt = pendingQualityOption
        pendingServerOption = null
        pendingQualityOption = null

        if (targetServer != null) {
            val alternative = _uiState.value.availableServers.firstOrNull {
                it.resolutionLabel.contains(rec, ignoreCase = true)
            }
            if (alternative != null) {
                selectServer(alternative, isUserExplicit = false)
            } else {
                selectServer(targetServer, isUserExplicit = false)
                val targetQuality = _uiState.value.availableQualities.firstOrNull {
                    it.label.contains(rec, ignoreCase = true)
                } ?: VideoQualityOption(id = "auto", label = "Auto ($rec)")
                applyQuality(targetQuality)
            }
        } else {
            val finalTargetOpt = targetOpt ?: _uiState.value.availableQualities.firstOrNull {
                it.label.contains(rec, ignoreCase = true)
            } ?: _uiState.value.availableQualities.firstOrNull { it.id == "auto" }
              ?: VideoQualityOption(id = "auto", label = "Auto ($rec)")
            applyQuality(finalTargetOpt)
        }
    }

    fun continueAnywayWithConflictQuality() {
        val conflict = _uiState.value.qualityConflictNotice
        if (conflict != null) {
            com.euthopiar.core.network.BandwidthEstimator.Default.suppressConflictForSession(conflict.requestedQuality)
        }
        val targetServer = pendingServerOption
        val targetQuality = pendingQualityOption
        _uiState.update { it.copy(qualityConflictNotice = null) }
        pendingServerOption = null
        pendingQualityOption = null

        if (targetServer != null) {
            selectServer(targetServer, isUserExplicit = false)
        } else if (targetQuality != null) {
            applyQuality(targetQuality)
        }
    }

    fun dismissQualityConflictNotice() {
        _uiState.update { it.copy(qualityConflictNotice = null) }
        pendingQualityOption = null
        pendingServerOption = null
    }

    private fun applyQuality(option: VideoQualityOption) {
        _uiState.update { it.copy(selectedQuality = option) }
        val player = exoPlayer ?: return

        if (option.height != null) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, option.height)
                .setMinVideoSize(0, 0)
                .build()
            player.seekTo(player.currentPosition)
            updateMediaFormatInfo()
            showToast("Quality: ${option.label}")
        } else {
            // Auto: network-aware auto quality decider
            val estimatedBps = com.euthopiar.core.network.BandwidthEstimator.Default.getEstimatedBandwidthBps(getApplication())
            val autoTier = com.euthopiar.core.network.BandwidthEstimator.Default.decideAutoQuality(estimatedBps)
            val maxH = when (autoTier) {
                "480p" -> 480
                "720p" -> 720
                "1080p" -> 1080
                else -> Int.MAX_VALUE
            }
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .setMaxVideoSize(Int.MAX_VALUE, maxH)
                .setMinVideoSize(0, 0)
                .build()
            player.seekTo(player.currentPosition)
            updateMediaFormatInfo()
            showToast("Quality: Auto ($autoTier)")
        }
    }

    private fun stepDownQualityOnStarvation() {
        val curOpt = _uiState.value.selectedQuality
        val curHeight = curOpt.height ?: 1080
        val nextOption = _uiState.value.availableQualities.firstOrNull { opt ->
            val h = opt.height ?: 0
            h > 0 && h < curHeight
        }
        if (nextOption != null) {
            android.util.Log.w("PlayerViewModel", "Sub-bitrate starvation detected: stepping down from ${curOpt.label} to ${nextOption.label}")
            showToast("Slow connection: adjusting quality to ${nextOption.label}")
            applyQuality(nextOption)
        }
    }

    fun selectAudioLanguage(isoCode: String) {
        val player = exoPlayer ?: return
        if (isoCode.equals("auto", ignoreCase = true) || isoCode.isBlank()) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setPreferredAudioLanguage(null)
                .build()
            _uiState.update { state ->
                val updated = state.availableAudioTracks.map {
                    it.copy(isSelected = it.isAuto || it.isoCode.equals("auto", ignoreCase = true))
                }
                state.copy(
                    availableAudioTracks = updated,
                    selectedAudioTrack = updated.firstOrNull { it.isSelected } ?: state.selectedAudioTrack
                )
            }
            updateMediaFormatInfo()
            showToast("Audio: Auto (Default)")
            return
        }
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setPreferredAudioLanguage(isoCode)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
        _uiState.update { state ->
            val updated = state.availableAudioTracks.map {
                it.copy(isSelected = it.isoCode.equals(isoCode, ignoreCase = true) || it.language.equals(isoCode, ignoreCase = true))
            }
            state.copy(
                availableAudioTracks = updated,
                selectedAudioTrack = updated.firstOrNull { it.isSelected } ?: state.selectedAudioTrack
            )
        }
        updateMediaFormatInfo()
        val display = _uiState.value.availableAudioTracks.firstOrNull { it.isoCode.equals(isoCode, ignoreCase = true) }?.displayName ?: isoCode
        showToast("Audio: $display")
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        _uiState.update { state ->
            state.copy(
                selectedAudioTrack = option,
                availableAudioTracks = state.availableAudioTracks.map {
                    it.copy(isSelected = it.id == option.id)
                }
            )
        }
        updateMediaFormatInfo()
        val player = exoPlayer ?: return

        if (!option.isAuto && option.groupIndex >= 0 && option.trackIndex >= 0) {
            val tracks = player.currentTracks
            if (option.groupIndex < tracks.groups.size) {
                val group = tracks.groups[option.groupIndex]
                if (group.type == C.TRACK_TYPE_AUDIO && option.trackIndex < group.length) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .setOverrideForType(
                            TrackSelectionOverride(
                                group.mediaTrackGroup,
                                listOf(option.trackIndex)
                            )
                        )
                        .apply {
                            option.language?.let { setPreferredAudioLanguage(it) }
                        }
                        .build()
                    showToast("Audio: ${option.label}")
                    return
                }
            }
        }

        if (!option.isoCode.isBlank() && option.isoCode != "und" && option.isoCode != "auto") {
            selectAudioLanguage(option.isoCode)
            return
        }

        // Auto or reset
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setPreferredAudioLanguage(null)
            .build()
        showToast("Audio: Auto (Default)")
    }

    fun selectSubtitle(option: SubtitleOption) {
        try {
            _uiState.update { it.copy(selectedSubtitle = option) }
            val player = exoPlayer ?: return

            if (option.id == "off") {
                activeSubtitleCues = emptyList()
                _currentSubtitleCue.value = null
                _currentSubtitleText.value = null
                try {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                } catch (_: Throwable) {}
                showToast("Subtitles turned off")
            } else if (option.url.isNullOrBlank()) {
                // Embedded Track via ExoPlayer native text renderer
                activeSubtitleCues = emptyList()
                _currentSubtitleCue.value = null
                _currentSubtitleText.value = null

                val parts = option.id.split("_")
                val gIdx = parts.getOrNull(1)?.toIntOrNull()
                val tIdx = parts.getOrNull(2)?.toIntOrNull()
                val currentTracks = player.currentTracks

                if (gIdx != null && tIdx != null && gIdx < currentTracks.groups.size) {
                    val group = currentTracks.groups[gIdx]
                    if (group.type == C.TRACK_TYPE_TEXT && tIdx < group.length) {
                        val format = group.getTrackFormat(tIdx)
                        val mime = format.sampleMimeType ?: ""
                        if (!group.isTrackSupported(tIdx) || mime.contains("pgs", ignoreCase = true) || mime.contains("vobsub", ignoreCase = true)) {
                            showToast("Embedded subtitle format (${mime.substringAfterLast("/")}) not supported by player")
                            return
                        }
                        try {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(tIdx)))
                                .build()
                            showToast("Subtitles: ${option.label}")
                            return
                        } catch (t: Throwable) {
                            android.util.Log.e("PlayerViewModel", "Error applying track override: ${t.message}", t)
                        }
                    }
                }

                try {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .apply {
                            option.language?.let { setPreferredTextLanguage(it) }
                        }
                        .build()
                } catch (_: Throwable) {}
                showToast("Subtitles: ${option.label}")
            } else {
                // External sidecar track (.srt, .vtt, .ass)
                loadSubtitleCues(option.url)
                try {
                    // Disable ExoPlayer's internal text tracks so ExoPlayer does not crash on ASS/unsupported formats
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                } catch (_: Throwable) {}
                showToast("Subtitles: ${option.label}")
            }
        } catch (t: Throwable) {
            android.util.Log.e("PlayerViewModel", "selectSubtitle failed: ${t.message}", t)
            showToast("Failed to enable subtitle")
        }
    }

    private fun loadSubtitleCues(url: String) {
        subtitleJob?.cancel()
        subtitleJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                if (url.contains("vidzee", ignoreCase = true)) {
                    reqBuilder.header("Referer", "https://player.vidzee.wtf/")
                    reqBuilder.header("Origin", "https://player.vidzee.wtf")
                }
                val resp = httpClient.newCall(reqBuilder.build()).execute()
                if (resp.isSuccessful) {
                    val rawBytes = resp.body?.bytes() ?: ByteArray(0)
                    val bytes = if (rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte() && rawBytes[1] == 0x8b.toByte()) {
                        try {
                            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(rawBytes)).readBytes()
                        } catch (_: Exception) {
                            rawBytes
                        }
                    } else {
                        rawBytes
                    }
                    val langHint = _uiState.value.selectedSubtitle.language ?: _uiState.value.selectedSubtitle.label
                    val content = CharsetNormalizer.decodeBytes(bytes, langHint)
                    val cues = if (AssSubtitleParser.isAss(content)) {
                        AssSubtitleParser.parse(content)
                    } else {
                        SubtitleParser.parse(content)
                    }
                    withContext(Dispatchers.Main) {
                        activeSubtitleCues = cues
                        val currentPos = (exoPlayer?.currentPosition ?: 0L) + _uiState.value.subtitleOffsetMs
                        val active = cues.firstOrNull { currentPos in it.startMs..it.endMs }
                        _currentSubtitleCue.value = active
                        _currentSubtitleText.value = active?.text
                        android.util.Log.d("PlayerViewModel", "Loaded ${cues.size} subtitle cues for $url")
                        if (cues.isEmpty()) {
                            showToast("Selected subtitle track has no displayable cues")
                        }
                    }
                } else {
                    android.util.Log.w("PlayerViewModel", "Failed to load subtitles HTTP ${resp.code}: $url")
                    withContext(Dispatchers.Main) {
                        showToast("Subtitle source returned error ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                // Gracefully ignore network or parsing errors for subtitles
            }
        }
    }

    private fun updateSubtitleForPosition(positionMs: Long) {
        if (activeSubtitleCues.isEmpty()) {
            if (_currentSubtitleText.value != null && _uiState.value.selectedSubtitle.id == "off") {
                _currentSubtitleText.value = null
                _currentSubtitleCue.value = null
            }
            return
        }
        val effectivePosition = positionMs + _uiState.value.subtitleOffsetMs
        val matchingCue = activeSubtitleCues.firstOrNull { effectivePosition in it.startMs..it.endMs }
        if (_currentSubtitleCue.value != matchingCue) {
            _currentSubtitleCue.value = matchingCue
            _currentSubtitleText.value = matchingCue?.text
        }
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        _uiState.update { it.copy(subtitleOffsetMs = it.subtitleOffsetMs + deltaMs) }
        val player = exoPlayer ?: return
        updateSubtitleForPosition(player.currentPosition)
    }

    fun resetSubtitleOffset() {
        _uiState.update { it.copy(subtitleOffsetMs = 0L) }
        val player = exoPlayer ?: return
        updateSubtitleForPosition(player.currentPosition)
    }

    fun setSubtitleSize(sizeSp: Int) {
        _uiState.update { it.copy(subtitleSizeSp = sizeSp) }
        viewModelScope.launch { preferencesRepository.setSubtitleSizeSp(sizeSp) }
    }

    fun setSubtitleBackground(enabled: Boolean) {
        _uiState.update { it.copy(subtitleBackgroundEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.setSubtitleBackgroundEnabled(enabled) }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        resetAutoHideTimer()
    }

    fun play() {
        val player = exoPlayer ?: return
        if (!player.isPlaying) {
            player.play()
            resetAutoHideTimer()
        }
    }

    fun pause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            resetAutoHideTimer()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val effectiveDuration = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val clamped = if (effectiveDuration > 0) positionMs.coerceIn(0L, effectiveDuration) else positionMs.coerceAtLeast(0L)
        android.util.Log.d("PlayerViewModel", "seekTo: target=$positionMs, clamped=$clamped, duration=$effectiveDuration, isSeekable=${player.isCurrentMediaItemSeekable}")
        player.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped, isScrubbing = false) }
        updateSubtitleForPosition(clamped)
        resetAutoHideTimer()
        savePlaybackProgress()
    }

    fun seekRelative(deltaMs: Long) {
        val player = exoPlayer ?: return
        val playerPos = player.currentPosition.coerceAtLeast(0L)
        val uiPos = _uiState.value.currentPositionMs
        val current = if (Math.abs(uiPos - playerPos) > 1500L && uiPos > 0L) uiPos else playerPos
        val effectiveDuration = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val target = current + deltaMs
        val newPos = if (effectiveDuration > 0) target.coerceIn(0L, effectiveDuration) else target.coerceAtLeast(0L)
        android.util.Log.d("PlayerViewModel", "seekRelative: delta=$deltaMs, current=$current, target=$newPos, duration=$effectiveDuration, isSeekable=${player.isCurrentMediaItemSeekable}")
        player.seekTo(newPos)
        _uiState.update { it.copy(currentPositionMs = newPos) }
        updateSubtitleForPosition(newPos)
        resetAutoHideTimer()
    }

    fun onScrubStart(positionMs: Long) {
        _uiState.update { it.copy(isScrubbing = true, scrubPositionMs = positionMs) }
        autoHideJob?.cancel()
    }

    fun onScrubProgress(positionMs: Long) {
        _uiState.update { it.copy(scrubPositionMs = positionMs) }
        updateSubtitleForPosition(positionMs)
    }

    fun onScrubEnd(positionMs: Long) {
        seekTo(positionMs)
    }

    fun cycleAspectRatio() {
        val nextMode = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.FIXED_16_9
            AspectRatioMode.FIXED_16_9 -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
        }
        _uiState.update { it.copy(aspectRatioMode = nextMode) }
        showToast("Aspect Ratio: ${nextMode.label}")
        resetAutoHideTimer()
    }

    fun onHold2xStart() {
        val player = exoPlayer ?: return
        if (!_uiState.value.isFastForwarding2x) {
            pre2xSpeed = _uiState.value.playbackSpeed
            player.playbackParameters = PlaybackParameters(2.0f)
            _uiState.update { it.copy(isFastForwarding2x = true) }
        }
    }

    fun onHold2xEnd() {
        val player = exoPlayer ?: return
        if (_uiState.value.isFastForwarding2x) {
            val restoreSpeed = pre2xSpeed
            player.playbackParameters = PlaybackParameters(restoreSpeed)
            _uiState.update { it.copy(isFastForwarding2x = false) }
        }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val currentIndex = speeds.indexOf(_uiState.value.playbackSpeed)
        val nextSpeed = speeds[(currentIndex + 1) % speeds.size]

        exoPlayer?.playbackParameters = PlaybackParameters(nextSpeed)
        pre2xSpeed = nextSpeed
        _uiState.update { it.copy(playbackSpeed = nextSpeed) }
        showToast("Speed: ${nextSpeed}x")
        resetAutoHideTimer()
    }

    private fun findNextEpisode(episodes: List<EpisodeItem>, curSeason: Int, curEpisode: Int): EpisodeItem? {
        return episodes
            .filter { it.seasonNumber == curSeason && it.episodeNumber > curEpisode }
            .minByOrNull { it.episodeNumber }
            ?: episodes
                .filter { it.seasonNumber > curSeason }
                .minWithOrNull(compareBy({ it.seasonNumber }, { it.episodeNumber }))
    }

    private fun findPrevEpisode(episodes: List<EpisodeItem>, curSeason: Int, curEpisode: Int): EpisodeItem? {
        return episodes
            .filter { it.seasonNumber == curSeason && it.episodeNumber < curEpisode }
            .maxByOrNull { it.episodeNumber }
            ?: episodes
                .filter { it.seasonNumber < curSeason }
                .maxWithOrNull(compareBy({ it.seasonNumber }, { it.episodeNumber }))
    }

    fun playNextEpisode() {
        savePlaybackProgress()
        val curSeason = _uiState.value.seasonNumber ?: 1
        val curEpisode = _uiState.value.episodeNumber ?: 1
        val eps = loadedEpisodes.ifEmpty { _uiState.value.availableEpisodes }
        val nextEp = findNextEpisode(eps, curSeason, curEpisode) ?: return

        loadStream(
            mediaId = currentMediaId ?: "",
            episodeData = nextEp.data,
            provider = currentProvider,
            type = currentType,
            title = _uiState.value.mediaTitle,
            epTitle = nextEp.title,
            season = nextEp.seasonNumber,
            episode = nextEp.episodeNumber
        )
    }

    fun playPrevEpisode() {
        savePlaybackProgress()
        val curSeason = _uiState.value.seasonNumber ?: 1
        val curEpisode = _uiState.value.episodeNumber ?: 1
        val eps = loadedEpisodes.ifEmpty { _uiState.value.availableEpisodes }
        val prevEp = findPrevEpisode(eps, curSeason, curEpisode) ?: return

        loadStream(
            mediaId = currentMediaId ?: "",
            episodeData = prevEp.data,
            provider = currentProvider,
            type = currentType,
            title = _uiState.value.mediaTitle,
            epTitle = prevEp.title,
            season = prevEp.seasonNumber,
            episode = prevEp.episodeNumber
        )
    }

    fun toggleControls() {
        if (_uiState.value.isScreenLocked) return
        val nextVisible = !_uiState.value.controlsVisible
        _uiState.update {
            it.copy(
                controlsVisible = nextVisible,
                isEpisodeSelectorOpen = if (!nextVisible) false else it.isEpisodeSelectorOpen,
                activeSidePanel = if (!nextVisible) PlayerSidePanelType.NONE else it.activeSidePanel
            )
        }
        if (nextVisible) {
            resetAutoHideTimer()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun showControls() {
        if (_uiState.value.isScreenLocked) return
        _uiState.update { it.copy(controlsVisible = true) }
        resetAutoHideTimer()
    }

    fun hideControls() {
        _uiState.update {
            it.copy(
                controlsVisible = false,
                isEpisodeSelectorOpen = false,
                activeSidePanel = PlayerSidePanelType.NONE
            )
        }
        autoHideJob?.cancel()
    }

    fun resetAutoHideTimer(customDelayMs: Long? = null) {
        autoHideJob?.cancel()
        val isCurrentlyPlaying = exoPlayer?.isPlaying == true || _uiState.value.isPlaying
        val isBufferingOrLoading = _uiState.value.isLoading || _uiState.value.isBuffering
        val isScrubbing = _uiState.value.isScrubbing
        val isPanelOpen = _uiState.value.isEpisodeSelectorOpen || _uiState.value.activeSidePanel != PlayerSidePanelType.NONE
        val isLocked = _uiState.value.isScreenLocked

        // Controls hide after 3.5 seconds of idle, but never while paused, buffering, scrubbing, or panel open
        if (isCurrentlyPlaying && !isBufferingOrLoading && !isScrubbing && !isPanelOpen && !isLocked) {
            val timeout = customDelayMs ?: if (_uiState.value.isEpisodeSelectorOpen) 8000L else 3500L
            autoHideJob = viewModelScope.launch {
                delay(timeout)
                _uiState.update {
                    it.copy(
                        controlsVisible = false,
                        isEpisodeSelectorOpen = false,
                        activeSidePanel = PlayerSidePanelType.NONE
                    )
                }
            }
        }
    }

    fun openSidePanel(panel: PlayerSidePanelType) {
        _uiState.update {
            it.copy(
                activeSidePanel = panel,
                isEpisodeSelectorOpen = false,
                controlsVisible = true
            )
        }
        resetAutoHideTimer()
    }

    fun closeSidePanel() {
        _uiState.update { it.copy(activeSidePanel = PlayerSidePanelType.NONE) }
        resetAutoHideTimer()
    }

    fun setSmartFailoverEnabled(enabled: Boolean) {
        _uiState.update { it.copy(smartFailoverEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.setSmartFailoverEnabled(enabled) }
    }

    fun setScreenLocked(locked: Boolean) {
        _uiState.update {
            it.copy(
                isScreenLocked = locked,
                controlsVisible = if (locked) false else true,
                activeSidePanel = PlayerSidePanelType.NONE,
                isEpisodeSelectorOpen = false
            )
        }
        if (!locked) {
            resetAutoHideTimer()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun toggleScreenLock() {
        setScreenLocked(!_uiState.value.isScreenLocked)
    }

    fun showPinchBadge(label: String) {
        _uiState.update { it.copy(pinchAspectToast = label) }
    }

    fun triggerPinchAspect(mode: AspectRatioMode, label: String) {
        _uiState.update { it.copy(aspectRatioMode = mode, pinchAspectToast = label) }
        viewModelScope.launch {
            delay(1100L)
            _uiState.update {
                if (it.pinchAspectToast == label) it.copy(pinchAspectToast = null) else it
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        pre2xSpeed = speed
        _uiState.update { it.copy(playbackSpeed = speed) }
        resetAutoHideTimer()
    }

    fun setSeekInterval(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setSeekIntervalSeconds(seconds)
        }
        _uiState.update { it.copy(seekIntervalSeconds = seconds) }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatioMode = mode) }
        resetAutoHideTimer()
    }

    fun toggleEpisodeSelector() {
        val nextState = !_uiState.value.isEpisodeSelectorOpen
        _uiState.update {
            it.copy(
                isEpisodeSelectorOpen = nextState,
                activeSidePanel = PlayerSidePanelType.NONE
            )
        }
        resetAutoHideTimer(if (nextState) 8000L else 4000L)
    }

    fun setEpisodeSelectorOpen(isOpen: Boolean) {
        _uiState.update {
            it.copy(
                isEpisodeSelectorOpen = isOpen,
                activeSidePanel = if (isOpen) PlayerSidePanelType.NONE else it.activeSidePanel
            )
        }
        resetAutoHideTimer(if (isOpen) 8000L else 4000L)
    }

    fun selectSeasonInSelector(season: Int) {
        _uiState.update { it.copy(selectedSeasonForSelector = season) }
        resetAutoHideTimer(8000L)
    }

    fun selectEpisode(episode: EpisodeItem) {
        if (episode.seasonNumber == _uiState.value.seasonNumber &&
            episode.episodeNumber == _uiState.value.episodeNumber
        ) {
            setEpisodeSelectorOpen(false)
            return
        }
        savePlaybackProgress()
        setEpisodeSelectorOpen(false)
        val mediaId = currentMediaId ?: return
        loadStream(
            mediaId = mediaId,
            episodeData = episode.data,
            provider = currentProvider,
            type = MediaType.TV_SERIES,
            title = _uiState.value.mediaTitle,
            epTitle = episode.title,
            season = episode.seasonNumber,
            episode = episode.episodeNumber
        )
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun retry() {
        currentMediaId?.let { mediaId ->
            loadStream(
                mediaId = mediaId,
                episodeData = currentEpisodeData,
                provider = currentProvider,
                type = currentType,
                title = _uiState.value.mediaTitle,
                epTitle = _uiState.value.episodeTitle,
                season = _uiState.value.seasonNumber,
                episode = _uiState.value.episodeNumber
            )
        }
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val isBuffering = _uiState.value.isBuffering || player.playbackState == Player.STATE_BUFFERING
                    val nowMs = System.currentTimeMillis()

                    if (_uiState.value.smartFailoverEnabled && isBuffering && !_uiState.value.isLoading) {
                        continuousPlayTimeMs = 0L
                        val curServer = _uiState.value.selectedServer
                        val urlLower = curServer?.streamSource?.url?.lowercase().orEmpty()
                        val isP2p = _uiState.value.isP2pStream || urlLower.startsWith("magnet:") || urlLower.contains("127.0.0.1") || urlLower.contains("localhost") || com.euthopiar.core.p2p.extractP2pStreamRequest(urlLower) != null
                        val is4k = curServer?.resolutionLabel?.contains("2160") == true || curServer?.resolutionLabel?.contains("4k", ignoreCase = true) == true
                        val maxGraceMs = when {
                            isP2p -> 40_000L
                            is4k -> 20_000L
                            else -> 12_000L
                        }

                        val startTime = bufferingStallStartTime
                        if (startTime == null) {
                            bufferingStallStartTime = nowMs
                            lastVelocityCheckTimeMs = nowMs
                            lastBytesLoaded = totalBytesLoadedCumulative
                            lastBufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                            lastP2pBufferProgress = _uiState.value.p2pBufferProgress
                            lastPeerCount = _uiState.value.p2pPeers
                            currentGracePeriodMs = if (isP2p) 14_000L else 5000L
                        } else {
                            val elapsedMs = nowMs - startTime

                            // Fast-failover dead-swarm circuit breaker: If isP2p && elapsedMs >= 8_000L && peers == 0 && seeds == 0, trigger failover immediately at 8s
                            if (isP2p && elapsedMs >= 8_000L && _uiState.value.p2pPeers == 0 && _uiState.value.p2pSeeds == 0) {
                                android.util.Log.w("PlayerViewModel", "Dead swarm detected at ${elapsedMs}ms (0 peers, 0 seeds). Triggering circuit breaker failover.")
                                bufferingStallStartTime = null
                                triggerFailover("Dead torrent swarm (no seeds or peers)")
                                return@let
                            }

                            // Velocity / Swarm ramp-up check every 1s
                            if (nowMs - lastVelocityCheckTimeMs >= 1000L) {
                                val dt = (nowMs - lastVelocityCheckTimeMs).coerceAtLeast(1L)
                                val bytesDelta = (totalBytesLoadedCumulative - lastBytesLoaded).coerceAtLeast(0L)
                                val speedBps = (bytesDelta * 1000L) / dt
                                val curBuffered = player.bufferedPosition.coerceAtLeast(0L)
                                val bufferGrowing = curBuffered > lastBufferedPositionMs

                                lastVelocityCheckTimeMs = nowMs
                                lastBytesLoaded = totalBytesLoadedCumulative
                                lastBufferedPositionMs = curBuffered

                                if (isP2p) {
                                    // Healthy ramp-up rule: If p2pSpeed > 50 * 1024L || p2pBufferProgress > lastP2pBufferProgress || p2pPeers > lastPeerCount, extend grace lease by +3000L up to 40s
                                    val p2pSpeed = _uiState.value.p2pDownloadSpeed
                                    val p2pBuf = _uiState.value.p2pBufferProgress
                                    val p2pPeers = _uiState.value.p2pPeers
                                    if (p2pSpeed > 50 * 1024L || p2pBuf > lastP2pBufferProgress || p2pPeers > lastPeerCount) {
                                        currentGracePeriodMs = (currentGracePeriodMs + 3000L).coerceAtMost(maxGraceMs)
                                        android.util.Log.d("PlayerViewModel", "P2P ramp-up: speed=${p2pSpeed / 1024} KB/s, buf=$p2pBuf, peers=$p2pPeers. Extended grace to ${currentGracePeriodMs}ms (max ${maxGraceMs}ms)")
                                    }
                                    lastP2pBufferProgress = p2pBuf
                                    lastPeerCount = p2pPeers
                                } else {
                                    if (speedBps > 150 * 1024L && bufferGrowing) {
                                        currentGracePeriodMs = (currentGracePeriodMs + 3000L).coerceAtMost(maxGraceMs)
                                        android.util.Log.d("PlayerViewModel", "Velocity ramp-up: ${speedBps / 1024} KB/s, buffer growing. Extended grace to ${currentGracePeriodMs}ms (max ${maxGraceMs}ms)")
                                    }
                                }
                            }

                            if (elapsedMs > currentGracePeriodMs) {
                                bufferingStallStartTime = null
                                triggerFailover("Buffering stalled")
                            }
                        }
                    } else if (!_uiState.value.smartFailoverEnabled || player.playbackState == Player.STATE_READY || player.isPlaying) {
                        bufferingStallStartTime = null
                    }

                    // Continuous Playback 90s Unbuffered Booster & Sub-bitrate Starvation check
                    if (player.isPlaying) {
                        continuousPlayTimeMs += 500L
                        if (continuousPlayTimeMs >= 90_000L) {
                            val tier = _uiState.value.selectedServer?.resolutionLabel ?: _uiState.value.selectedQuality.label
                            val normTier = com.euthopiar.core.network.BandwidthEstimator.Default.normalizeTier(tier)
                            if (boostedTiersForSession.add(normTier)) {
                                com.euthopiar.core.network.BandwidthEstimator.Default.onUnbufferedPlaybackAchieved(normTier)
                                android.util.Log.d("PlayerViewModel", "90s continuous unbuffered playback achieved. Boosted bandwidth estimate for tier: $normTier")
                            }
                        }
                    }

                    // Sub-bitrate Starvation (<15% bitrate for 5s triggers step-down).
                    // Evaluated ONLY when player is buffering and actively trying to play (playWhenReady == true).
                    val isBufferingStarvationCandidate = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
                    if (isBufferingStarvationCandidate) {
                        if (nowMs - lastVelocityCheckTimeMs >= 1000L) {
                            val dt = (nowMs - lastVelocityCheckTimeMs).coerceAtLeast(1L)
                            lastVelocityCheckTimeMs = nowMs

                            val curServer = _uiState.value.selectedServer
                            val urlLower = curServer?.streamSource?.url?.lowercase().orEmpty()
                            val isP2p = _uiState.value.isP2pStream || urlLower.startsWith("magnet:") || urlLower.contains("127.0.0.1") || urlLower.contains("localhost") || com.euthopiar.core.p2p.extractP2pStreamRequest(urlLower) != null

                            val activeTier = _uiState.value.selectedQuality.label
                            val requiredBitrate = com.euthopiar.core.network.BandwidthEstimator.Default.getRequiredBitrate(activeTier)
                            val starvationThreshold = requiredBitrate * 0.15

                            // For P2P streams: evaluate download health using native P2P engine download speed / swarm ingress
                            // rather than local HTTP socket byte delta (since 16MB pieces take time to hash-verify before flushing to loopback)
                            val throughputBps = if (isP2p) {
                                _uiState.value.p2pDownloadSpeed * 8.0
                            } else {
                                val bytesDelta = (totalBytesLoadedCumulative - lastBytesLoaded).coerceAtLeast(0L)
                                (bytesDelta * 8.0 * 1000.0) / dt
                            }
                            lastBytesLoaded = totalBytesLoadedCumulative

                            if (throughputBps in 1.0..starvationThreshold) {
                                starvationConsecutiveSeconds++
                                if (starvationConsecutiveSeconds >= 5) {
                                    starvationConsecutiveSeconds = 0
                                    stepDownQualityOnStarvation()
                                }
                            } else if (throughputBps > starvationThreshold) {
                                starvationConsecutiveSeconds = 0
                            }
                        }
                    } else if (player.playbackState == Player.STATE_READY) {
                        starvationConsecutiveSeconds = 0
                    }

                    // Video Black Screen Recovery: if audio is playing for > 2.5s but 0 video frames rendered on screen, fallback!
                    if (_uiState.value.smartFailoverEnabled && player.isPlaying && pos > 2500L && !videoFrameRendered) {
                        val hasVideoTrack = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_VIDEO }
                        val currentServer = _uiState.value.selectedServer
                        if (hasVideoTrack && currentServer != null && currentServer.id !in failedServerIds) {
                            android.util.Log.w("PlayerViewModel", "Black screen detected: audio playing at ${pos}ms but 0 video frames rendered. Switching source...")
                            failedServerIds.add(currentServer.id)
                            val non4kFallback = _uiState.value.availableServers.firstOrNull {
                                it.id !in failedServerIds &&
                                        !it.resolutionLabel.contains("2160") &&
                                        !it.resolutionLabel.contains("4k", ignoreCase = true)
                            }
                            val fallback = non4kFallback ?: _uiState.value.availableServers.firstOrNull { it.id !in failedServerIds }
                            if (fallback != null) {
                                showToast("Video stream unsupported. Switching to ${fallback.serverName} (${fallback.resolutionLabel})...")
                                selectServer(fallback, isUserExplicit = false)
                            }
                        }
                    }

                    if (!_uiState.value.isScrubbing && (player.playbackState == Player.STATE_READY || player.isPlaying)) {
                        _uiState.update {
                            it.copy(
                                currentPositionMs = pos,
                                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                                durationMs = if (player.duration > 0) player.duration else it.durationMs
                            )
                        }
                        val effectiveDur = if (player.duration > 0) player.duration else _uiState.value.durationMs
                        checkAndTriggerUpNext(pos, effectiveDur)
                        checkSkipIntroAndOutro(pos, effectiveDur)
                    }
                    updateSubtitleForPosition(pos)

                    val now = System.currentTimeMillis()
                    if (now - lastProgressSaveTimestamp >= 5000L && player.isPlaying && pos > 0L) {
                        lastProgressSaveTimestamp = now
                        savePlaybackProgress()
                    }
                }
                val currentSpeed = exoPlayer?.playbackParameters?.speed ?: 1.0f
                val delayMs = when {
                    _uiState.value.isScrubbing -> 100L
                    activeSubtitleCues.isNotEmpty() || _uiState.value.selectedSubtitle.id != "off" -> {
                        (50L / currentSpeed.coerceAtLeast(0.25f)).toLong().coerceIn(20L, 100L)
                    }
                    _uiState.value.controlsVisible -> 250L
                    else -> 500L
                }
                delay(delayMs)
            }
        }
    }

    private fun triggerFailover(reason: String = "Buffering stalled") {
        val currentServer = _uiState.value.selectedServer
        if (currentServer != null) {
            failedServerIds.add(currentServer.id)
        }
        val otherServers = _uiState.value.availableServers.filter { it.id !in failedServerIds }
        if (otherServers.isNotEmpty()) {
            val fallback = otherServers.first()
            showToast("$reason. Switching to ${fallback.serverName}…")
            selectServer(fallback, isUserExplicit = false)
        } else {
            if (userExplicitServerSelection) {
                showToast("${currentServer?.serverName ?: "Server"} is taking longer to buffer.")
            } else {
                android.util.Log.w("PlayerViewModel", "Stream source unreachable for ${currentServer?.serverName} ($reason)")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isBuffering = false,
                        userError = com.euthopiar.ui.util.UserError.SourceUnavailable,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun setDialogueBoostLevel(level: DialogueBoostLevel) {
        _uiState.update { it.copy(dialogueBoostLevel = level) }
        applyDialogueBoost(level)
        showToast("Dialogue Boost: ${level.label}")
    }

    private fun applyDialogueBoost(level: DialogueBoostLevel = _uiState.value.dialogueBoostLevel) {
        val sessionId = exoPlayer?.audioSessionId ?: 0
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
            audioDspManager.setAudioSessionId(sessionId)
        }
        audioDspManager.updateParameters(
            boostLevel = level,
            nightModeDrc = _uiState.value.isNightModeDrcEnabled
        )
    }

    fun setNightModeDrc(enabled: Boolean) {
        _uiState.update { it.copy(isNightModeDrcEnabled = enabled) }
        applyNightModeDrc(enabled)
        showToast(if (enabled) "Night Mode DRC: On (Dialogue boosted, spikes limited)" else "Night Mode DRC: Off")
    }

    private fun applyNightModeDrc(enabled: Boolean = _uiState.value.isNightModeDrcEnabled) {
        val sessionId = exoPlayer?.audioSessionId ?: 0
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
            audioDspManager.setAudioSessionId(sessionId)
        }
        audioDspManager.updateParameters(
            boostLevel = _uiState.value.dialogueBoostLevel,
            nightModeDrc = enabled
        )
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(selectedSleepTimer = option) }

        if (option == SleepTimerOption.OFF) {
            _uiState.update { it.copy(sleepTimerRemainingSeconds = null) }
            exoPlayer?.volume = 1.0f
            showToast("Sleep timer turned off")
            return
        }

        if (option == SleepTimerOption.END_OF_EPISODE) {
            _uiState.update { it.copy(sleepTimerRemainingSeconds = null) }
            showToast("Playback will stop at end of episode")
            return
        }

        val totalSeconds = option.minutes * 60
        _uiState.update { it.copy(sleepTimerRemainingSeconds = totalSeconds) }
        showToast("Sleep timer set for ${option.label}")

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }

                // In last 10 seconds, smoothly fade audio volume
                if (remaining <= 10) {
                    val fadeVol = (remaining / 10f).coerceIn(0f, 1f)
                    exoPlayer?.volume = fadeVol
                }
            }

            if (isActive) {
                exoPlayer?.pause()
                exoPlayer?.volume = 1.0f
                _uiState.update {
                    it.copy(
                        selectedSleepTimer = SleepTimerOption.OFF,
                        sleepTimerRemainingSeconds = null
                    )
                }
                showToast("Bedtime sleep timer: playback paused")
            }
        }
    }

    fun setSubtitleColor(color: String) {
        _uiState.update { it.copy(subtitleColor = color) }
        viewModelScope.launch {
            preferencesRepository.setSubtitleColor(color)
        }
    }

    fun setSubtitleOutline(enabled: Boolean) {
        _uiState.update { it.copy(subtitleOutline = enabled) }
        viewModelScope.launch {
            preferencesRepository.setSubtitleOutline(enabled)
        }
    }

    fun playUpNextNow() {
        upNextJob?.cancel()
        upNextJob = null
        _uiState.update { it.copy(showUpNextCard = false) }
        playNextEpisode()
    }

    fun dismissUpNextCard() {
        upNextJob?.cancel()
        upNextJob = null
        isUpNextDismissed = true
        _uiState.update { it.copy(showUpNextCard = false) }
    }

    private fun getNextEpisode(): EpisodeItem? {
        val curSeason = _uiState.value.seasonNumber ?: 1
        val curEpisode = _uiState.value.episodeNumber ?: 1
        val eps = loadedEpisodes.ifEmpty { _uiState.value.availableEpisodes }
        return findNextEpisode(eps, curSeason, curEpisode)
    }

    private fun checkAndTriggerUpNext(pos: Long, duration: Long) {
        if (isUpNextDismissed || upNextJob != null || !_uiState.value.hasNextEpisode || duration <= 0L || _uiState.value.selectedSleepTimer == SleepTimerOption.END_OF_EPISODE) return
        val remaining = duration - pos
        if (remaining in 1L..35_000L) {
            val nextEp = _uiState.value.upNextEpisode ?: getNextEpisode() ?: return
            val countdownTotal = try {
                kotlinx.coroutines.runBlocking { preferencesRepository.countdownNextEpisodeSeconds.first() }
            } catch (_: Throwable) { 10 }
            _uiState.update {
                it.copy(
                    showUpNextCard = true,
                    upNextCountdownSeconds = countdownTotal,
                    upNextEpisode = nextEp
                )
            }
            upNextJob = viewModelScope.launch {
                for (sec in countdownTotal downTo 1) {
                    _uiState.update { it.copy(upNextCountdownSeconds = sec) }
                    delay(1000L)
                }
                _uiState.update { it.copy(showUpNextCard = false, upNextCountdownSeconds = 0) }
                upNextJob = null
                playNextEpisode()
            }
        }
    }

    private var hasPromptedSkipIntro = false
    private var currentAniSkipTimestamps: com.euthopiar.core.aniskip.SkipTimestamps? = null

    fun skipIntro() {
        hasPromptedSkipIntro = true
        _uiState.update { it.copy(showSkipIntro = false) }
        viewModelScope.launch {
            val aniEnd = currentAniSkipTimestamps?.introEndSeconds
            if (aniEnd != null && aniEnd > 0) {
                exoPlayer?.seekTo((aniEnd * 1000).toLong())
                showToast("Skipped intro (AniSkip)")
            } else {
                val introDur = _uiState.value.skipIntroSeconds
                seekRelative(introDur * 1000L)
                showToast("Skipped intro (+${introDur}s)")
            }
        }
    }

    fun dismissSkipIntro() {
        hasPromptedSkipIntro = true
        _uiState.update { it.copy(showSkipIntro = false) }
    }

    fun skipOutro() {
        if (_uiState.value.hasNextEpisode) {
            playNextEpisode()
        } else {
            val dur = _uiState.value.durationMs
            if (dur > 0L) {
                exoPlayer?.seekTo((dur - 2000L).coerceAtLeast(0L))
            }
        }
        _uiState.update { it.copy(showSkipOutro = false) }
    }

    private fun checkSkipIntroAndOutro(pos: Long, duration: Long) {
        if (duration <= 180_000L) return
        val ani = currentAniSkipTimestamps
        val introStart = ani?.introStartSeconds
        val introEnd = ani?.introEndSeconds
        val outroStart = ani?.outroStartSeconds

        val isIntroRange = if (introStart != null && introEnd != null) {
            val startMs = (introStart * 1000).toLong()
            val endMs = (introEnd * 1000).toLong()
            pos in (startMs - 1000L)..endMs
        } else {
            pos in 3_000L..120_000L
        }

        val isOutroRange = if (outroStart != null) {
            val startMs = (outroStart * 1000).toLong()
            pos >= startMs
        } else {
            (duration - pos) in 20_000L..150_000L
        }

        val dynamicIntroLabel = if (introStart != null && introStart > 20.0 && pos < (introStart * 1000).toLong()) {
            "Skip Recap"
        } else {
            "Skip Intro"
        }

        if (isIntroRange && !hasPromptedSkipIntro) {
            if (isAutoSkipIntroEnabled) {
                skipIntro()
            } else if (!_uiState.value.showSkipIntro) {
                _uiState.update { it.copy(showSkipIntro = true, skipIntroLabel = dynamicIntroLabel) }
            }
        } else if (!isIntroRange && _uiState.value.showSkipIntro) {
            _uiState.update { it.copy(showSkipIntro = false) }
        }

        _uiState.update { state ->
            state.copy(showSkipOutro = isOutroRange)
        }
    }

    fun applySubtitlePreset(preset: com.euthopiar.ui.player.subtitles.SubtitlePreset) {
        viewModelScope.launch {
            preferencesRepository.setSubtitleSizeSp(preset.sizeSp)
            preferencesRepository.setSubtitleColor(preset.color)
            preferencesRepository.setSubtitleBackgroundEnabled(preset.backgroundEnabled)
            preferencesRepository.setSubtitleOutline(preset.outline)
        }
    }

    private fun scrobbleToTrakt(action: String, progressPercent: Float) {
        persistenceScope.launch {
            try {
                val token = preferencesRepository.traktAccessToken.first()
                val enabled = preferencesRepository.traktScrobbleEnabled.first()
                if (!enabled || token.isBlank()) return@launch
                val isTv = currentType == MediaType.TV_SERIES
                val body = if (isTv) {
                    com.euthopiar.core.sync.trakt.TraktScrobbleBody(
                        show = com.euthopiar.core.sync.trakt.TraktShow(
                            title = _uiState.value.mediaTitle,
                            ids = com.euthopiar.core.sync.trakt.TraktIds(imdb = currentMediaId?.takeIf { it.startsWith("tt") })
                        ),
                        episode = com.euthopiar.core.sync.trakt.TraktEpisode(
                            season = _uiState.value.seasonNumber ?: 1,
                            number = _uiState.value.episodeNumber ?: 1,
                            title = _uiState.value.episodeTitle
                        ),
                        progress = progressPercent.coerceIn(0f, 100f)
                    )
                } else {
                    com.euthopiar.core.sync.trakt.TraktScrobbleBody(
                        movie = com.euthopiar.core.sync.trakt.TraktMovie(
                            title = _uiState.value.mediaTitle,
                            ids = com.euthopiar.core.sync.trakt.TraktIds(imdb = currentMediaId?.takeIf { it.startsWith("tt") })
                        ),
                        progress = progressPercent.coerceIn(0f, 100f)
                    )
                }
                com.euthopiar.core.sync.trakt.TraktClient.getInstance().scrobble(action, token, body)
            } catch (_: Exception) {}
        }
    }

    private fun scrobbleToSimkl(progressPercent: Float) {
        persistenceScope.launch {
            try {
                val token = preferencesRepository.simklAccessToken.first()
                val enabled = preferencesRepository.simklScrobbleEnabled.first()
                if (!enabled || token.isBlank()) return@launch
                val isTv = currentType == MediaType.TV_SERIES
                val payload = if (isTv) {
                    com.euthopiar.core.sync.simkl.SimklScrobblePayload(
                        show = com.euthopiar.core.sync.simkl.SimklScrobbleItem(
                            title = _uiState.value.mediaTitle
                        ),
                        episode = com.euthopiar.core.sync.simkl.SimklEpisodeItem(
                            season = _uiState.value.seasonNumber ?: 1,
                            number = _uiState.value.episodeNumber ?: 1
                        ),
                        progress = progressPercent
                    )
                } else {
                    com.euthopiar.core.sync.simkl.SimklScrobblePayload(
                        movie = com.euthopiar.core.sync.simkl.SimklScrobbleItem(
                            title = _uiState.value.mediaTitle
                        ),
                        progress = progressPercent
                    )
                }
                com.euthopiar.core.sync.simkl.SimklClient.getInstance().scrobble(token, payload)
            } catch (_: Exception) {}
        }
    }

    fun searchExternalSubtitles() {
        val title = _uiState.value.mediaTitle
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSearchingExternalSubtitles = true) }
                showToast("Searching subtitles online...")
            }
            val foundSubs = try {
                val imdb = currentImdbId ?: Regex("""tt\d+""").find("${currentMediaId.orEmpty()} ${currentEpisodeData.orEmpty()}")?.value
                val curUrl = _uiState.value.selectedServer?.streamSource?.url
                val fname = curUrl?.let { urlStr ->
                    if (urlStr.startsWith("file://") || urlStr.startsWith("/")) {
                        java.io.File(urlStr.removePrefix("file://")).name
                    } else {
                        try { android.net.Uri.parse(urlStr).lastPathSegment } catch (_: Exception) { null }
                    }
                }
                val vSize = curUrl?.let { urlStr ->
                    if (urlStr.startsWith("file://") || urlStr.startsWith("/")) {
                        val f = java.io.File(urlStr.removePrefix("file://"))
                        if (f.exists()) f.length() else null
                    } else null
                }
                ExternalSubtitleEngine.searchSubtitles(
                    title = title,
                    season = _uiState.value.seasonNumber,
                    episode = _uiState.value.episodeNumber,
                    imdbId = imdb,
                    videoSize = vSize,
                    filename = fname
                )
            } catch (_: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSearchingExternalSubtitles = false) }
                if (foundSubs.isEmpty()) {
                    showToast("No external subtitles found")
                } else {
                    val existingUrls = _uiState.value.availableSubtitles.mapNotNull { it.url }.toSet()
                    val newOptions = foundSubs.filter { it.url !in existingUrls }.mapIndexed { index, sub ->
                        SubtitleOption(
                            id = "ext_${System.currentTimeMillis()}_$index",
                            label = sub.language,
                            language = sub.language,
                            url = sub.url
                        )
                    }
                    if (newOptions.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(availableSubtitles = state.availableSubtitles + newOptions)
                        }
                        showToast("Added ${newOptions.size} external subtitles")
                    } else {
                        showToast("External subtitles already loaded")
                    }
                }
            }
        }
    }

    private suspend fun autoFetchExternalSubtitlesIfNeeded(title: String, season: Int?, episode: Int?) {
        if (_uiState.value.availableSubtitles.size > 1) return
        try {
            val imdb = currentImdbId ?: Regex("""tt\d+""").find("${currentMediaId.orEmpty()} ${currentEpisodeData.orEmpty()}")?.value
            val curUrl = _uiState.value.selectedServer?.streamSource?.url
            val fname = curUrl?.let { urlStr ->
                if (urlStr.startsWith("file://") || urlStr.startsWith("/")) {
                    java.io.File(urlStr.removePrefix("file://")).name
                } else {
                    try { android.net.Uri.parse(urlStr).lastPathSegment } catch (_: Exception) { null }
                }
            }
            val vSize = curUrl?.let { urlStr ->
                if (urlStr.startsWith("file://") || urlStr.startsWith("/")) {
                    val f = java.io.File(urlStr.removePrefix("file://"))
                    if (f.exists()) f.length() else null
                } else null
            }
            val foundSubs = ExternalSubtitleEngine.searchSubtitles(
                title = title,
                season = season,
                episode = episode,
                imdbId = imdb,
                videoSize = vSize,
                filename = fname
            )
            if (foundSubs.isNotEmpty()) {
                val existingUrls = _uiState.value.availableSubtitles.mapNotNull { it.url }.toSet()
                val newOptions = foundSubs.filter { it.url !in existingUrls }.mapIndexed { index, sub ->
                    SubtitleOption(
                        id = "ext_auto_${System.currentTimeMillis()}_$index",
                        label = sub.language,
                        language = sub.language,
                        url = sub.url
                    )
                }
                if (newOptions.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val shouldAutoSelect = _uiState.value.selectedSubtitle.id == "off"
                        val autoSub = if (shouldAutoSelect) {
                            newOptions.firstOrNull { it.language?.contains("eng", ignoreCase = true) == true } ?: newOptions.first()
                        } else null

                        _uiState.update { state ->
                            state.copy(
                                availableSubtitles = (state.availableSubtitles + newOptions).distinctBy { it.url ?: it.id },
                                selectedSubtitle = autoSub ?: state.selectedSubtitle
                            )
                        }
                        if (autoSub?.url != null) {
                            loadSubtitleCues(autoSub.url)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun savePlaybackProgress() {
        val mediaId = currentMediaId ?: return
        val pos = exoPlayer?.currentPosition ?: _uiState.value.currentPositionMs
        val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: _uiState.value.durationMs
        if (pos <= 0L) return

        val isTv = currentType == MediaType.TV_SERIES
        val s = if (isTv) _uiState.value.seasonNumber else null
        val e = if (isTv) _uiState.value.episodeNumber else null
        val epTitle = if (isTv) _uiState.value.episodeTitle else null

        persistenceScope.launch {
            libraryRepository.updatePlaybackProgress(
                mediaId = mediaId,
                title = _uiState.value.mediaTitle,
                episodeTitle = epTitle,
                seasonNumber = s,
                episodeNumber = e,
                currentPositionMs = pos,
                durationMs = duration,
                backdropUrl = currentBackdropUrl,
                posterUrl = currentPosterUrl,
                provider = currentProvider ?: _uiState.value.providerName,
                mediaType = currentType.name,
                episodeData = currentEpisodeData
            )
            val progressPercent = if (duration > 0L) (pos.toFloat() / duration.toFloat()) * 100f else 0f
            scrobbleToTrakt(if (pos >= duration * 0.9f) "stop" else "pause", progressPercent)
            scrobbleToSimkl(progressPercent)
        }
    }

    private fun cleanAnimeTitle(raw: String): String {
        var s = raw.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), " ")
        s = s.replace(Regex("""(?i)\b(?:4k|2160p|1080p|720p|480p|uhd|fhd|hd|bluray|blu-ray|web-dl|webrip|brrip|dvdrip|hdtv|remux|hevc|x265|x264|avc|10bit|hdr|dovi|sdr|aac|atmos|ddp|truehd|multi|dual\s*audio|hindi|english|season\s*\d+|s\d+e\d+|series)\b"""), " ")
        s = s.replace(Regex("[^a-zA-Z0-9 ]"), " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun dismissResumeNotice() {
        resumeDismissJob?.cancel()
        _uiState.update { it.copy(resumeNotice = null) }
    }

    fun onExitPlayer() {
        savePlaybackProgress()
        exoPlayer?.pause()
    }

    internal fun deduplicateAndSortServers(options: List<ServerSourceOption>): List<ServerSourceOption> {
        if (options.isEmpty()) return emptyList()

        // Tier 1: Exact Transport Deduplication
        // Group by exact transport identifier:
        // - Torrent: "torrent:<lower-infoHash>:<fileIdx>"
        // - Direct URL: "url:<normalized-playback-url>"
        // When transport keys match, keep the option with the highest seeders or richer metadata.
        val transportGroups = options.groupBy { opt ->
            computeTransportKey(opt.streamSource)
        }

        val tier1Deduped = transportGroups.values.map { group ->
            if (group.size == 1) {
                group.first()
            } else {
                group.maxWith(
                    compareBy<ServerSourceOption> { it.streamSource.seeders ?: 0 }
                        .thenBy { it.label.length }
                )
            }
        }

        // Tier 2: Preserve Distinct Release Variants
        // Distinct release variants (4K vs 1080p, REMUX vs WEB-DL, HDR/DV vs SDR, TrueHD vs AAC)
        // have different torrent hashes or direct stream URLs, and are therefore preserved.
        // Sort for optimal hierarchy: Resolution -> Release Type (REMUX/DV/HDR) -> Seeders -> Name
        return tier1Deduped.sortedWith(
            compareBy<ServerSourceOption> { opt ->
                val res = opt.resolutionLabel.lowercase()
                when {
                    res.contains("4k") || res.contains("2160") -> 0
                    res.contains("1440") || res.contains("2k") -> 1
                    res.contains("1080") || res.contains("fhd") -> 2
                    res.contains("720") || res.contains("hd") -> 3
                    res.contains("auto") -> 4
                    res.contains("480") || res.contains("sd") -> 5
                    else -> 6
                }
            }.thenBy { opt ->
                val combined = "${opt.serverName} ${opt.label}".lowercase()
                when {
                    combined.contains("remux") -> 0
                    combined.contains("dovi") || combined.contains("dolby vision") || combined.contains("dv") -> 1
                    combined.contains("hdr") -> 2
                    combined.contains("web-dl") || combined.contains("webrip") -> 3
                    combined.contains("bluray") || combined.contains("bdrip") -> 4
                    else -> 5
                }
            }.thenByDescending { opt ->
                opt.streamSource.seeders ?: 0
            }.thenBy { opt ->
                opt.serverName.lowercase()
            }
        )
    }

    private fun computeTransportKey(source: StreamSource): String {
        val hash = source.infoHash?.trim()?.lowercase()
        if (!hash.isNullOrBlank()) {
            val idx = extractFileIndex(source.url)
            return "torrent:$hash:$idx"
        }

        val url = source.url.trim()
        if (url.startsWith("magnet:", ignoreCase = true)) {
            val btih = Regex("""xt=urn:btih:([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.lowercase()
            val idx = extractFileIndex(url)
            return "torrent:${btih ?: url.lowercase()}:$idx"
        }

        if (url.startsWith("torrent://", ignoreCase = true) || url.startsWith("torrent:", ignoreCase = true)) {
            val raw = url.removePrefix("torrent://").removePrefix("TORRENT://")
                .removePrefix("torrent:").removePrefix("TORRENT:")
                .substringBefore('/').substringBefore('?').trim().lowercase()
            val idx = extractFileIndex(url)
            return "torrent:$raw:$idx"
        }

        if ((url.length == 40 || url.length == 64) && url.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) {
            return "torrent:${url.lowercase()}:0"
        }

        return "url:${normalizePlaybackUrl(url)}"
    }

    private fun extractFileIndex(url: String): Int {
        val match = Regex("""(?:index|fileIdx|indices)=(\d+)""", RegexOption.IGNORE_CASE).find(url)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun normalizePlaybackUrl(url: String): String {
        val clean = url.trim()
        val qIdx = clean.indexOf('?')
        if (qIdx == -1) return clean.lowercase().trimEnd('/')
        val base = clean.substring(0, qIdx).lowercase().trimEnd('/')
        val query = clean.substring(qIdx + 1)
        val transientParams = setOf(
            "token", "_t", "expires", "auth", "sign", "signature",
            "key", "timestamp", "session", "sig", "h", "ip"
        )
        val filteredQuery = query.split('&').filterNot { param ->
            val paramKey = param.substringBefore('=').lowercase().trim()
            paramKey in transientParams
        }.sorted().joinToString("&")
        return if (filteredQuery.isEmpty()) base else "$base?$filteredQuery"
    }

    override fun onCleared() {
        savePlaybackProgress()
        super.onCleared()
        try {
            com.euthopiar.core.p2p.P2pStreamingEngine.stopStream()
        } catch (_: Throwable) {}
        p2pStateJob?.cancel()
        tickerJob?.cancel()
        autoHideJob?.cancel()
        toastJob?.cancel()
        subtitleJob?.cancel()
        resumeDismissJob?.cancel()
        upNextJob?.cancel()
        sleepTimerJob?.cancel()
        try {
            audioDspManager.release()
        } catch (_: Exception) {}
        try {
            dynamicsProcessing?.release()
        } catch (_: Exception) {}
        dynamicsProcessing = null
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
