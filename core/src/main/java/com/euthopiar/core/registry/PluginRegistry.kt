package com.euthopiar.core.registry

import com.euthopiar.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

object EmptyPlaceholderPlugin : UniversalPlugin {
    override val name: String = "No Provider"
    override val mainUrl: String = ""
    override val supportedTypes: Set<MediaType> = emptySet()
    override suspend fun getHomeCatalog(): List<CatalogRow> = emptyList()
    override suspend fun search(query: String): List<MediaItem> = emptyList()
    override suspend fun getDetails(mediaItem: MediaItem): MediaDetail =
        MediaDetail(
            id = mediaItem.id,
            title = mediaItem.title.ifEmpty { "No Provider Selected" },
            url = mediaItem.url,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            type = mediaItem.type,
            year = mediaItem.year,
            synopsis = null
        )
    override suspend fun getStreamLinks(episodeData: String): StreamResult =
        StreamResult(emptyList(), emptyList())
}

/**
 * Thread-safe registry managing installed, enabled, and active streaming provider plugins.
 * Streaming provider plugins are dynamically loaded and managed through JavaScript runtime modules.
 */
open class PluginRegistry(
    initialPlugins: List<UniversalPlugin> = emptyList()
) {
    private val lock = Any()

    private val _plugins = MutableStateFlow(initialPlugins)
    val plugins: StateFlow<List<UniversalPlugin>> = _plugins.asStateFlow()

    private val _activePlugin = MutableStateFlow<UniversalPlugin>(
        initialPlugins.firstOrNull() ?: EmptyPlaceholderPlugin
    )
    val activePlugin: StateFlow<UniversalPlugin> = _activePlugin.asStateFlow()

    private val _disabledPlugins = MutableStateFlow<Set<String>>(emptySet())
    val disabledPlugins: StateFlow<Set<String>> = _disabledPlugins.asStateFlow()

    /**
     * Registers a new provider plugin. If a plugin with the same name is already registered,
     * it replaces it (enabling dynamic EUP updates).
     */
    fun registerPlugin(plugin: UniversalPlugin) {
        synchronized(lock) {
            val current = _plugins.value
            val existingIndex = current.indexOfFirst { it.name.equals(plugin.name, ignoreCase = true) }
            val updated = if (existingIndex >= 0) {
                current.toMutableList().apply { set(existingIndex, plugin) }
            } else {
                current + plugin
            }
            _plugins.value = updated
            if (_activePlugin.value === EmptyPlaceholderPlugin ||
                _activePlugin.value.name.equals(plugin.name, ignoreCase = true) ||
                current.isEmpty()
            ) {
                _activePlugin.value = plugin
            }
        }
    }

    /**
     * Unregisters a provider plugin by name.
     */
    fun unregisterPlugin(pluginName: String) {
        synchronized(lock) {
            val current = _plugins.value
            val updated = current.filterNot { it.name.equals(pluginName, ignoreCase = true) }
            _plugins.value = updated
            val disabled = _disabledPlugins.value
            if (_activePlugin.value.name.equals(pluginName, ignoreCase = true)) {
                val fallback = updated.firstOrNull { p -> !disabled.any { it.equals(p.name, ignoreCase = true) } }
                    ?: updated.firstOrNull() ?: EmptyPlaceholderPlugin
                _activePlugin.value = fallback
            }
        }
    }

    /**
     * Checks if a plugin is currently enabled.
     */
    fun isPluginEnabled(pluginName: String): Boolean {
        synchronized(lock) {
            return !_disabledPlugins.value.any { it.equals(pluginName, ignoreCase = true) }
        }
    }

    /**
     * Enables or disables a plugin by name.
     */
    fun setPluginEnabled(pluginName: String, enabled: Boolean) {
        synchronized(lock) {
            val currentDisabled = _disabledPlugins.value.toMutableSet()
            if (enabled) {
                currentDisabled.removeAll { it.equals(pluginName, ignoreCase = true) }
            } else {
                currentDisabled.add(pluginName)
            }
            _disabledPlugins.value = currentDisabled

            if (!enabled && _activePlugin.value.name.equals(pluginName, ignoreCase = true)) {
                val nextEnabled = _plugins.value.firstOrNull { plugin ->
                    !currentDisabled.any { it.equals(plugin.name, ignoreCase = true) }
                } ?: EmptyPlaceholderPlugin
                _activePlugin.value = nextEnabled
            }
        }
    }

    /**
     * Returns all registered plugins that are currently enabled.
     */
    fun getEnabledPlugins(): List<UniversalPlugin> {
        val disabled = _disabledPlugins.value
        return _plugins.value.filter { plugin ->
            !disabled.any { it.equals(plugin.name, ignoreCase = true) }
        }
    }

    /**
     * Switches the active provider to the plugin matching [pluginName].
     * Returns true if successfully switched, false if no matching plugin is registered.
     */
    fun setActivePlugin(pluginName: String): Boolean {
        synchronized(lock) {
            val target = _plugins.value.find { it.name.equals(pluginName, ignoreCase = true) }
            return if (target != null) {
                setPluginEnabled(pluginName, true)
                _activePlugin.value = target
                true
            } else {
                false
            }
        }
    }

    /**
     * Retrieves a registered plugin by name.
     */
    fun getPlugin(pluginName: String): UniversalPlugin? {
        return _plugins.value.find { it.name.equals(pluginName, ignoreCase = true) }
    }

    /**
     * Resets registry state back to initial state (useful for test resets).
     */
    fun reset(plugins: List<UniversalPlugin> = emptyList()) {
        synchronized(lock) {
            _plugins.value = plugins
            _activePlugin.value = plugins.firstOrNull() ?: EmptyPlaceholderPlugin
            _disabledPlugins.value = emptySet()
        }
    }

    companion object Default : PluginRegistry(emptyList()) {
        fun defaultPlugins(): List<UniversalPlugin> = emptyList()
    }
}
