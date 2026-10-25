package com.euthopiar.core.addons

import com.euthopiar.core.model.MediaType

data class HomeCatalogDefinition(
    val key: String,
    val defaultTitle: String,
    val catalogName: String,
    val addonName: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val supportsPagination: Boolean,
    val descriptorSignature: String = ""
) {
    val cacheKey: String
        get() = "$key|$descriptorSignature"

    fun buildCatalogUrl(
        skip: Int? = null,
        genre: String? = null,
        search: String? = null
    ): String {
        val extraParts = buildList {
            if (!search.isNullOrBlank()) add("search=${search.encodeCatalogExtra()}")
            if (!genre.isNullOrBlank()) add("genre=${genre.encodeCatalogExtra()}")
            if (skip != null && skip > 0) add("skip=$skip")
        }

        return buildAddonResourceUrl(
            manifestUrl = manifestUrl,
            resource = "catalog",
            type = type,
            id = catalogId,
            extraPathSegment = extraParts.joinToString("&").ifBlank { null }
        )
    }
}

fun AddonCatalog.supportsPagination(): Boolean =
    extra.any { property -> property.name.equals("skip", ignoreCase = true) }

fun buildHomeCatalogDefinitions(addons: List<ManagedAddon>): List<HomeCatalogDefinition> =
    addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        addon to manifest
    }.flatMap { (addon, manifest) ->
        manifest.catalogs
            .filter { catalog -> catalog.extra.none { it.isRequired } }
            .map { catalog ->
                HomeCatalogDefinition(
                    key = "${manifest.id}:${catalog.type}:${catalog.id}",
                    defaultTitle = formatHomeCatalogTitle(catalog.name, catalog.type),
                    catalogName = catalog.name,
                    addonName = addon.displayTitle,
                    manifestUrl = addon.manifestUrl,
                    type = catalog.type,
                    catalogId = catalog.id,
                    supportsPagination = catalog.supportsPagination(),
                    descriptorSignature = buildCatalogSignature(addon, manifest, catalog)
                )
            }
    }.distinctBy { it.key }

fun buildSearchCatalogDefinitions(addons: List<ManagedAddon>): List<HomeCatalogDefinition> =
    addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        addon to manifest
    }.flatMap { (addon, manifest) ->
        manifest.catalogs
            .filter { catalog -> catalog.extra.any { it.name.equals("search", ignoreCase = true) } }
            .map { catalog ->
                HomeCatalogDefinition(
                    key = "${manifest.id}:${catalog.type}:${catalog.id}",
                    defaultTitle = formatHomeCatalogTitle(catalog.name, catalog.type),
                    catalogName = catalog.name,
                    addonName = addon.displayTitle,
                    manifestUrl = addon.manifestUrl,
                    type = catalog.type,
                    catalogId = catalog.id,
                    supportsPagination = catalog.supportsPagination(),
                    descriptorSignature = buildCatalogSignature(addon, manifest, catalog)
                )
            }
    }.distinctBy { it.key }

fun formatHomeCatalogTitle(catalogName: String, type: String): String {
    val typeLabel = when (type.lowercase()) {
        "movie", "movies" -> "Movies"
        "series", "tv" -> "Series"
        "anime" -> "Anime"
        else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    return if (catalogName.contains(typeLabel, ignoreCase = true) ||
        (type.equals("movie", ignoreCase = true) && catalogName.contains("movie", ignoreCase = true)) ||
        (type.equals("series", ignoreCase = true) && (catalogName.contains("series", ignoreCase = true) || catalogName.contains("show", ignoreCase = true)))
    ) {
        catalogName
    } else {
        "$catalogName ($typeLabel)"
    }
}

private fun buildCatalogSignature(
    addon: ManagedAddon,
    manifest: AddonManifest,
    catalog: AddonCatalog
): String {
    var hash = 1125899906842597L
    fun mix(value: String?) {
        val s = value.orEmpty()
        for (c in s) {
            hash = 31L * hash + c.code
        }
    }
    mix(addon.manifestUrl)
    mix(manifest.id)
    mix(manifest.version)
    mix(catalog.type)
    mix(catalog.id)
    mix(catalog.name)
    return hash.toString(16)
}

private fun String.encodeCatalogExtra(): String =
    buildString {
        encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (
                char in 'a'..'z' ||
                char in 'A'..'Z' ||
                char in '0'..'9' ||
                char == '-' ||
                char == '_' ||
                char == '.' ||
                char == '~'
            ) {
                append(char)
            } else {
                append('%')
                append(HEX[value shr 4])
                append(HEX[value and 0x0F])
            }
        }
    }

private const val HEX = "0123456789ABCDEF"
