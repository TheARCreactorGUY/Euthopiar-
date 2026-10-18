package com.euthopiar.ui.settings.pages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IntegrationsSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository.getInstance(application)

    private val _traktUsername = MutableStateFlow("")
    val traktUsername: StateFlow<String> = _traktUsername.asStateFlow()

    private val _traktScrobbleEnabled = MutableStateFlow(false)
    val traktScrobbleEnabled: StateFlow<Boolean> = _traktScrobbleEnabled.asStateFlow()

    private val _simklAccessToken = MutableStateFlow("")
    val simklAccessToken: StateFlow<String> = _simklAccessToken.asStateFlow()

    private val _simklScrobbleEnabled = MutableStateFlow(false)
    val simklScrobbleEnabled: StateFlow<Boolean> = _simklScrobbleEnabled.asStateFlow()

    private val _realDebridApiKey = MutableStateFlow("")
    val realDebridApiKey: StateFlow<String> = _realDebridApiKey.asStateFlow()

    private val _allDebridApiKey = MutableStateFlow("")
    val allDebridApiKey: StateFlow<String> = _allDebridApiKey.asStateFlow()

    private val _torBoxApiKey = MutableStateFlow("")
    val torBoxApiKey: StateFlow<String> = _torBoxApiKey.asStateFlow()

    private val _activeDebridProvider = MutableStateFlow("realdebrid")
    val activeDebridProvider: StateFlow<String> = _activeDebridProvider.asStateFlow()

    private val _debridEnabled = MutableStateFlow(false)
    val debridEnabled: StateFlow<Boolean> = _debridEnabled.asStateFlow()

    init {
        viewModelScope.launch { prefs.traktUsername.collect { _traktUsername.value = it } }
        viewModelScope.launch { prefs.traktScrobbleEnabled.collect { _traktScrobbleEnabled.value = it } }
        viewModelScope.launch { prefs.simklAccessToken.collect { _simklAccessToken.value = it } }
        viewModelScope.launch { prefs.simklScrobbleEnabled.collect { _simklScrobbleEnabled.value = it } }
        viewModelScope.launch { prefs.realDebridApiKey.collect { _realDebridApiKey.value = it } }
        viewModelScope.launch { prefs.allDebridApiKey.collect { _allDebridApiKey.value = it } }
        viewModelScope.launch { prefs.torBoxApiKey.collect { _torBoxApiKey.value = it } }
        viewModelScope.launch { prefs.activeDebridProvider.collect { _activeDebridProvider.value = it } }
        viewModelScope.launch { prefs.debridEnabled.collect { _debridEnabled.value = it } }
    }

    fun setTraktScrobble(enabled: Boolean) {
        _traktScrobbleEnabled.value = enabled
        viewModelScope.launch { prefs.setTraktScrobbleEnabled(enabled) }
    }

    fun setSimklScrobble(enabled: Boolean) {
        _simklScrobbleEnabled.value = enabled
        viewModelScope.launch { prefs.setSimklScrobbleEnabled(enabled) }
    }

    fun setDebridEnabled(enabled: Boolean) {
        _debridEnabled.value = enabled
        viewModelScope.launch { prefs.setDebridEnabled(enabled) }
    }

    fun setActiveDebridProvider(provider: String) {
        _activeDebridProvider.value = provider
        viewModelScope.launch { prefs.setActiveDebridProvider(provider) }
    }

    fun setRealDebridKey(key: String) {
        _realDebridApiKey.value = key
        viewModelScope.launch { prefs.setRealDebridApiKey(key) }
    }

    fun setAllDebridKey(key: String) {
        _allDebridApiKey.value = key
        viewModelScope.launch { prefs.setAllDebridApiKey(key) }
    }

    fun setTorBoxKey(key: String) {
        _torBoxApiKey.value = key
        viewModelScope.launch { prefs.setTorBoxApiKey(key) }
    }

    fun setTraktAuth(accessToken: String, refreshToken: String, username: String) {
        _traktUsername.value = username
        _traktScrobbleEnabled.value = true
        viewModelScope.launch {
            prefs.setTraktAuth(accessToken, refreshToken, username)
            prefs.setTraktScrobbleEnabled(true)
        }
    }

    fun setSimklAuth(token: String) {
        _simklAccessToken.value = token
        _simklScrobbleEnabled.value = true
        viewModelScope.launch {
            prefs.setSimklAuth(token)
            prefs.setSimklScrobbleEnabled(true)
        }
    }

    fun disconnectTrakt() {
        _traktUsername.value = ""
        _traktScrobbleEnabled.value = false
        viewModelScope.launch {
            prefs.clearTraktAuth()
            prefs.setTraktScrobbleEnabled(false)
        }
    }

    fun disconnectSimkl() {
        _simklAccessToken.value = ""
        _simklScrobbleEnabled.value = false
        viewModelScope.launch {
            prefs.clearSimklAuth()
            prefs.setSimklScrobbleEnabled(false)
        }
    }
}
