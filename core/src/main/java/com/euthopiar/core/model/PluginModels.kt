package com.euthopiar.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: Int,
    val versionName: String,
    val author: String,
    val siteUrl: String,
    val description: String,
    val iconUrl: String? = null,
    val className: String, // Full class path implementing UniversalPlugin
    val downloadUrl: String? = null,
    val isBuiltIn: Boolean = false,
    val isEnabled: Boolean = true,
    val sha256: String? = null,
    val isEup: Boolean = false,
    val supportedTypes: List<String> = listOf("MOVIE", "SERIES", "ANIME"),
    val isAdult: Boolean = false,
    val isVault: Boolean = false
) {
    val isAdultPlugin: Boolean
        get() = isAdult || isVault ||
                supportedTypes.any { it.equals("ADULT", ignoreCase = true) || it.equals("VAULT", ignoreCase = true) }
}

@Serializable
data class PluginRepositoryIndex(
    val name: String,
    val description: String,
    val manifestVersion: Int,
    val plugins: List<PluginManifest>
)
