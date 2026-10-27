package com.euthopiar.ui.settings.pages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository.getInstance(application)

    val maxSupportedBufferSizeMb: Int = PreferencesRepository.getMaxSupportedBufferSizeMb(application)

    private val _defaultQuality = MutableStateFlow("Auto")
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _autoPlayNext = MutableStateFlow(true)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    private val _autoSkipIntro = MutableStateFlow(false)
    val autoSkipIntro: StateFlow<Boolean> = _autoSkipIntro.asStateFlow()

    private val _skipIntroDuration = MutableStateFlow(85)
    val skipIntroDuration: StateFlow<Int> = _skipIntroDuration.asStateFlow()

    private val _skipOutroDuration = MutableStateFlow(90)
    val skipOutroDuration: StateFlow<Int> = _skipOutroDuration.asStateFlow()

    private val _countdownDuration = MutableStateFlow(10)
    val countdownDuration: StateFlow<Int> = _countdownDuration.asStateFlow()

    private val _seekInterval = MutableStateFlow(10)
    val seekInterval: StateFlow<Int> = _seekInterval.asStateFlow()

    private val _subtitleSize = MutableStateFlow(18)
    val subtitleSize: StateFlow<Int> = _subtitleSize.asStateFlow()

    private val _subtitleBgEnabled = MutableStateFlow(true)
    val subtitleBgEnabled: StateFlow<Boolean> = _subtitleBgEnabled.asStateFlow()

    private val _subtitleColor = MutableStateFlow("WHITE")
    val subtitleColor: StateFlow<String> = _subtitleColor.asStateFlow()

    private val _subtitleOutline = MutableStateFlow(true)
    val subtitleOutline: StateFlow<Boolean> = _subtitleOutline.asStateFlow()

    private val _externalSubtitlesEnabled = MutableStateFlow(true)
    val externalSubtitlesEnabled: StateFlow<Boolean> = _externalSubtitlesEnabled.asStateFlow()

    private val _bufferSizeMb = MutableStateFlow(50)
    val bufferSizeMb: StateFlow<Int> = _bufferSizeMb.asStateFlow()

    private val _hardwareAcceleration = MutableStateFlow(true)
    val hardwareAcceleration: StateFlow<Boolean> = _hardwareAcceleration.asStateFlow()

    private val _smartFailover = MutableStateFlow(false)
    val smartFailover: StateFlow<Boolean> = _smartFailover.asStateFlow()

    private val _audioPassthrough = MutableStateFlow(false)
    val audioPassthrough: StateFlow<Boolean> = _audioPassthrough.asStateFlow()

    init {
        viewModelScope.launch { prefs.defaultQuality.collect { _defaultQuality.value = it } }
        viewModelScope.launch { prefs.autoPlayNextEpisode.collect { _autoPlayNext.value = it } }
        viewModelScope.launch { prefs.autoSkipIntro.collect { _autoSkipIntro.value = it } }
        viewModelScope.launch { prefs.skipIntroDurationSeconds.collect { _skipIntroDuration.value = it } }
        viewModelScope.launch { prefs.skipOutroDurationSeconds.collect { _skipOutroDuration.value = it } }
        viewModelScope.launch { prefs.countdownNextEpisodeSeconds.collect { _countdownDuration.value = it } }
        viewModelScope.launch { prefs.seekIntervalSeconds.collect { _seekInterval.value = it } }
        viewModelScope.launch { prefs.subtitleSizeSp.collect { _subtitleSize.value = it } }
        viewModelScope.launch { prefs.subtitleBackgroundEnabled.collect { _subtitleBgEnabled.value = it } }
        viewModelScope.launch { prefs.subtitleColor.collect { _subtitleColor.value = it } }
        viewModelScope.launch { prefs.subtitleOutline.collect { _subtitleOutline.value = it } }
        viewModelScope.launch { prefs.externalSubtitlesEnabled.collect { _externalSubtitlesEnabled.value = it } }
        viewModelScope.launch { prefs.playbackBufferSizeMb.collect { _bufferSizeMb.value = it } }
        viewModelScope.launch { prefs.hardwareAccelerationEnabled.collect { _hardwareAcceleration.value = it } }
        viewModelScope.launch { prefs.smartFailoverEnabled.collect { _smartFailover.value = it } }
        viewModelScope.launch { prefs.audioPassthroughEnabled.collect { _audioPassthrough.value = it } }
    }

    fun setDefaultQuality(quality: String) {
        _defaultQuality.value = quality
        viewModelScope.launch { prefs.setDefaultQuality(quality) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _autoPlayNext.value = enabled
        viewModelScope.launch { prefs.setAutoPlayNextEpisode(enabled) }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        _autoSkipIntro.value = enabled
        viewModelScope.launch { prefs.setAutoSkipIntro(enabled) }
    }

    fun setSkipIntroDuration(seconds: Int) {
        _skipIntroDuration.value = seconds
        viewModelScope.launch { prefs.setSkipIntroDurationSeconds(seconds) }
    }

    fun setSkipOutroDuration(seconds: Int) {
        _skipOutroDuration.value = seconds
        viewModelScope.launch { prefs.setSkipOutroDurationSeconds(seconds) }
    }

    fun setCountdownDuration(seconds: Int) {
        _countdownDuration.value = seconds
        viewModelScope.launch { prefs.setCountdownNextEpisodeSeconds(seconds) }
    }

    fun setSeekInterval(seconds: Int) {
        _seekInterval.value = seconds
        viewModelScope.launch { prefs.setSeekIntervalSeconds(seconds) }
    }

    fun setSubtitleSize(sizeSp: Int) {
        _subtitleSize.value = sizeSp
        viewModelScope.launch { prefs.setSubtitleSizeSp(sizeSp) }
    }

    fun setSubtitleBgEnabled(enabled: Boolean) {
        _subtitleBgEnabled.value = enabled
        viewModelScope.launch { prefs.setSubtitleBackgroundEnabled(enabled) }
    }

    fun setSubtitleColor(color: String) {
        _subtitleColor.value = color
        viewModelScope.launch { prefs.setSubtitleColor(color) }
    }

    fun setSubtitleOutline(enabled: Boolean) {
        _subtitleOutline.value = enabled
        viewModelScope.launch { prefs.setSubtitleOutline(enabled) }
    }

    fun setExternalSubtitlesEnabled(enabled: Boolean) {
        _externalSubtitlesEnabled.value = enabled
        viewModelScope.launch { prefs.setExternalSubtitlesEnabled(enabled) }
    }

    fun setBufferSizeMb(sizeMb: Int) {
        _bufferSizeMb.value = sizeMb
        viewModelScope.launch { prefs.setPlaybackBufferSizeMb(sizeMb) }
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        _hardwareAcceleration.value = enabled
        viewModelScope.launch { prefs.setHardwareAccelerationEnabled(enabled) }
    }

    fun setSmartFailover(enabled: Boolean) {
        _smartFailover.value = enabled
        viewModelScope.launch { prefs.setSmartFailoverEnabled(enabled) }
    }

    fun setAudioPassthrough(enabled: Boolean) {
        _audioPassthrough.value = enabled
        viewModelScope.launch { prefs.setAudioPassthroughEnabled(enabled) }
    }

    fun resetDefaults() {
        setDefaultQuality("Auto")
        setBufferSizeMb(50)
        setHardwareAcceleration(true)
        setSmartFailover(false)
        setAudioPassthrough(false)
        setSeekInterval(10)
        setAutoPlayNext(true)
        setAutoSkipIntro(false)
        setSkipIntroDuration(85)
        setSkipOutroDuration(90)
        setCountdownDuration(10)
        setSubtitleSize(18)
        setSubtitleColor("WHITE")
        setSubtitleBgEnabled(true)
        setSubtitleOutline(true)
        setExternalSubtitlesEnabled(true)
    }
}
