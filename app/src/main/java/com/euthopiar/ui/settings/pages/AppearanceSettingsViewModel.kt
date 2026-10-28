package com.euthopiar.ui.settings.pages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.euthopiar.core.model.ContentRegionPreset
import com.euthopiar.data.preferences.*
import com.euthopiar.ui.longpress.LongPressSheetStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository.getInstance(application)

    private val _contentRegionPreset = kotlinx.coroutines.flow.MutableStateFlow(ContentRegionPreset.GLOBAL)
    val contentRegionPreset: StateFlow<ContentRegionPreset> = _contentRegionPreset

    private val _cardScaleMode = kotlinx.coroutines.flow.MutableStateFlow(CardScaleMode.FLAGSHIP)
    val cardScaleMode: StateFlow<CardScaleMode> = _cardScaleMode

    private val _navbarStyle = kotlinx.coroutines.flow.MutableStateFlow(NavbarStyle.FLOATING)
    val navbarStyle: StateFlow<NavbarStyle> = _navbarStyle

    private val _top10Style = kotlinx.coroutines.flow.MutableStateFlow(Top10Style.PosterTinted)
    val top10Style: StateFlow<Top10Style> = _top10Style

    private val _sectionLabelStyle = kotlinx.coroutines.flow.MutableStateFlow(SectionLabelStyle.AccentBar)
    val sectionLabelStyle: StateFlow<SectionLabelStyle> = _sectionLabelStyle

    private val _longPressStyle = kotlinx.coroutines.flow.MutableStateFlow(LongPressSheetStyle.HERO_PEEK)
    val longPressStyle: StateFlow<LongPressSheetStyle> = _longPressStyle

    private val _reduceMotion = kotlinx.coroutines.flow.MutableStateFlow(false)
    val reduceMotion: StateFlow<Boolean> = _reduceMotion

    private val _amoledMode = kotlinx.coroutines.flow.MutableStateFlow(true)
    val amoledMode: StateFlow<Boolean> = _amoledMode

    init {
        viewModelScope.launch {
            prefs.contentRegionPreset.collect { _contentRegionPreset.value = it }
        }
        viewModelScope.launch {
            prefs.cardScaleMode.collect { _cardScaleMode.value = it }
        }
        viewModelScope.launch {
            prefs.navbarStyle.collect { _navbarStyle.value = it }
        }
        viewModelScope.launch {
            prefs.top10Style.collect { _top10Style.value = it }
        }
        viewModelScope.launch {
            prefs.sectionLabelStyle.collect { _sectionLabelStyle.value = it }
        }
        viewModelScope.launch {
            prefs.longPressStyle.collect { _longPressStyle.value = it }
        }
        viewModelScope.launch {
            prefs.reduceMotion.collect { _reduceMotion.value = it }
        }
        viewModelScope.launch {
            prefs.amoledMode.collect { _amoledMode.value = it }
        }
    }

    fun setContentRegionPreset(preset: ContentRegionPreset) {
        _contentRegionPreset.value = preset
        viewModelScope.launch { prefs.setContentRegionPreset(preset) }
    }

    fun setCardScaleMode(mode: CardScaleMode) {
        _cardScaleMode.value = mode
        viewModelScope.launch { prefs.setCardScaleMode(mode) }
    }

    fun setNavbarStyle(style: NavbarStyle) {
        _navbarStyle.value = style
        viewModelScope.launch { prefs.setNavbarStyle(style) }
    }

    fun setTop10Style(style: Top10Style) {
        _top10Style.value = style
        viewModelScope.launch { prefs.setTop10Style(style) }
    }

    fun setSectionLabelStyle(style: SectionLabelStyle) {
        _sectionLabelStyle.value = style
        viewModelScope.launch { prefs.setSectionLabelStyle(style) }
    }

    fun setLongPressStyle(style: LongPressSheetStyle) {
        _longPressStyle.value = style
        viewModelScope.launch { prefs.setLongPressStyle(style) }
    }

    fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
        viewModelScope.launch { prefs.setReduceMotion(enabled) }
    }

    fun setAmoledMode(enabled: Boolean) {
        _amoledMode.value = enabled
        viewModelScope.launch { prefs.setAmoledMode(enabled) }
    }

    fun resetDefaults() {
        setContentRegionPreset(ContentRegionPreset.GLOBAL)
        setNavbarStyle(NavbarStyle.FLOATING)
        setCardScaleMode(CardScaleMode.FLAGSHIP)
        setSectionLabelStyle(SectionLabelStyle.AccentBar)
        setTop10Style(Top10Style.PosterTinted)
        setLongPressStyle(LongPressSheetStyle.HERO_PEEK)
        setReduceMotion(false)
        setAmoledMode(true)
    }
}
