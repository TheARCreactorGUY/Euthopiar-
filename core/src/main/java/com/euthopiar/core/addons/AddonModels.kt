package com.euthopiar.core.addons

import kotlinx.serialization.Serializable

@Serializable
data class AddonManifest(
    val id: String,
    val name: String,
    val description: String = "",
    val version: String = "1.0.0",
    val logoUrl: String? = null,
    val resources: List<AddonResource> = emptyList(),
    val types: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
    val catalogs: List<AddonCatalog> = emptyList(),
    val behaviorHints: AddonBehaviorHints = AddonBehaviorHints(),
    val transportUrl: String = ""
)

fun AddonManifest.supportsResource(name: String, type: String, id: String): Boolean =
    resources.any { resource ->
        val canonical = if (type.equals("tv", ignoreCase = true)) "series" else type.lowercase()
        val typeMatches = resource.types.isEmpty() ||
            resource.types.any {
                it.equals(canonical, ignoreCase = true) ||
                (canonical == "series" && it.equals("tv", ignoreCase = true)) ||
                (canonical == "tv" && it.equals("series", ignoreCase = true)) ||
                (canonical == "movie" && it.equals("movies", ignoreCase = true)) ||
                (canonical == "movies" && it.equals("movie", ignoreCase = true))
            }
        val cleanId = id.trim().removePrefix("tmdb:").removePrefix("tmdb/")
        val baseId = cleanId.substringBefore(':').trim()
        val resNameMatches = resource.name.equals(name, ignoreCase = true) ||
            (name.equals("subtitles", ignoreCase = true) && resource.name.equals("subtitle", ignoreCase = true)) ||
            (name.equals("subtitle", ignoreCase = true) && resource.name.equals("subtitles", ignoreCase = true))
        val effectivePrefixes = if (resource.idPrefixes.isNotEmpty()) resource.idPrefixes else this.idPrefixes
        val prefixMatches = name.equals("catalog", ignoreCase = true) ||
            id.isEmpty() ||
            effectivePrefixes.isEmpty() ||
            effectivePrefixes.any { prefix ->
                id.startsWith(prefix, ignoreCase = true) ||
                (prefix.endsWith(":") && id.startsWith(prefix.removeSuffix(":"), ignoreCase = true)) ||
                (id.startsWith("tmdb:", ignoreCase = true) && prefix.equals("tmdb", ignoreCase = true)) ||
                (prefix.startsWith("tmdb", ignoreCase = true) && (id.startsWith("tmdb:", ignoreCase = true) || id.startsWith("tmdb/", ignoreCase = true)))
            } ||
            (baseId.startsWith("tt") && effectivePrefixes.any { it.startsWith("tt", ignoreCase = true) }) ||
            (baseId.isNotEmpty() && baseId.all { it.isDigit() } && effectivePrefixes.any { it.startsWith("tt", ignoreCase = true) || it.startsWith("tmdb", ignoreCase = true) })

        resNameMatches && typeMatches && prefixMatches
    }

fun AddonManifest.supportsStream(type: String, id: String): Boolean =
    supportsResource("stream", type, id)

@Serializable
data class AddonResource(
    val name: String,
    val types: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList()
)

@Serializable
data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<AddonExtraProperty> = emptyList()
)

@Serializable
data class AddonExtraProperty(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
    val optionsLimit: Int? = null
)

@Serializable
data class AddonBehaviorHints(
    val configurable: Boolean = false,
    val configurationRequired: Boolean = false,
    val adult: Boolean = false,
    val p2p: Boolean = false
)

@Serializable
data class ManagedAddon(
    val manifestUrl: String,
    val manifest: AddonManifest? = null,
    val userSetName: String? = null,
    val enabled: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    val isActive: Boolean
        get() = enabled && manifest != null

    val displayTitle: String
        get() = userSetName?.takeIf { it.isNotBlank() && it != manifest?.name }
            ?: manifest?.name
            ?: manifestUrl.substringBefore("?").substringAfterLast("/").ifBlank { "Addon" }

    fun streamAddonInstanceId(manifestId: String): String =
        "addon:$manifestId"
}

data class AddonsUiState(
    val addons: List<ManagedAddon> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

fun List<ManagedAddon>.enabledAddons(): List<ManagedAddon> =
    filter { it.enabled && it.manifest != null }
