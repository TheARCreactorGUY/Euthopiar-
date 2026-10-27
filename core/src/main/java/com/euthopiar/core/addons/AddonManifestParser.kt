package com.euthopiar.core.addons

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AddonManifestParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(manifestUrl: String, payload: String): AddonManifest {
        val root = (json.parseToJsonElement(payload) as? JsonObject)
            ?: error("Invalid manifest payload: expected JsonObject")
        val defaultTypes = root.stringList("types")
        val defaultPrefixes = root.stringList("idPrefixes")

        return AddonManifest(
            id = root.requiredString("id"),
            name = root.requiredString("name"),
            description = root.optionalString("description").orEmpty(),
            version = root.optionalString("version") ?: "1.0.0",
            logoUrl = (root.optionalString("logo") ?: root.optionalString("icon"))?.resolveAgainstManifest(manifestUrl),
            resources = root.resources(defaultTypes, defaultPrefixes),
            types = defaultTypes,
            idPrefixes = defaultPrefixes,
            catalogs = root.catalogs(),
            behaviorHints = root.behaviorHints(),
            transportUrl = manifestUrl
        )
    }

    private fun JsonObject.resources(
        defaultTypes: List<String>,
        defaultPrefixes: List<String>
    ): List<AddonResource> =
        array("resources").mapNotNull { resource ->
            when (resource) {
                is JsonPrimitive -> {
                    if (resource is kotlinx.serialization.json.JsonNull) null
                    else {
                        val name = resource.contentOrNull?.trim()
                        if (!name.isNullOrBlank() && !name.equals("null", ignoreCase = true)) {
                            AddonResource(
                                name = name,
                                types = defaultTypes,
                                idPrefixes = defaultPrefixes
                            )
                        } else null
                    }
                }
                is JsonObject -> {
                    val name = resource.optionalString("name")
                    if (!name.isNullOrBlank()) {
                        AddonResource(
                            name = name,
                            types = resource.stringList("types").ifEmpty { defaultTypes },
                            idPrefixes = resource.stringList("idPrefixes").ifEmpty { defaultPrefixes }
                        )
                    } else null
                }
                else -> null
            }
        }.filter { it.name.isNotBlank() }

    private fun JsonObject.catalogs(): List<AddonCatalog> =
        array("catalogs").mapNotNull { catalogElement ->
            val catalog = catalogElement as? JsonObject ?: return@mapNotNull null
            val catalogId = catalog.optionalString("id") ?: return@mapNotNull null
            val catalogType = catalog.optionalString("type") ?: return@mapNotNull null
            val extras = catalog.array("extra").mapNotNull { extraElement ->
                val extraObj = extraElement as? JsonObject ?: return@mapNotNull null
                extraObj.optionalString("name")?.takeIf { it.isNotBlank() }?.let { name ->
                    AddonExtraProperty(
                        name = name,
                        isRequired = extraObj.boolean("isRequired"),
                        options = extraObj.stringList("options"),
                        optionsLimit = extraObj.int("optionsLimit")
                    )
                }
            }
            val extrasSupported = if (extras.isEmpty()) {
                val required = catalog.stringList("extraRequired").toSet()
                catalog.stringList("extraSupported").map { name ->
                    AddonExtraProperty(
                        name = name,
                        isRequired = name in required
                    )
                }
            } else extras

            AddonCatalog(
                type = catalogType,
                id = catalogId,
                name = catalog.optionalString("name")?.ifBlank { catalogId } ?: catalogId,
                extra = extrasSupported
            )
        }

    private fun JsonObject.behaviorHints(): AddonBehaviorHints {
        val hints = this["behaviorHints"] as? JsonObject ?: return AddonBehaviorHints()
        return AddonBehaviorHints(
            configurable = hints.boolean("configurable"),
            configurationRequired = hints.boolean("configurationRequired"),
            adult = hints.boolean("adult"),
            p2p = hints.boolean("p2p")
        )
    }

    private fun JsonObject.requiredString(name: String): String =
        optionalString(name)?.takeIf { it.isNotBlank() }
            ?: error("Addon manifest missing required field: $name")

    private fun JsonObject.optionalString(name: String): String? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        if (!primitive.isString && primitive.content.equals("null", ignoreCase = true)) return null
        val content = primitive.contentOrNull?.trim()
        return if (content.isNullOrBlank()) null else content
    }

    private fun JsonObject.boolean(name: String): Boolean {
        val primitive = this[name] as? JsonPrimitive ?: return false
        if (primitive is kotlinx.serialization.json.JsonNull) return false
        return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull() ?: false
    }

    private fun JsonObject.int(name: String): Int? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        if (primitive is kotlinx.serialization.json.JsonNull) return null
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.array(name: String): List<kotlinx.serialization.json.JsonElement> =
        (this[name] as? JsonArray)?.toList() ?: emptyList()

    private fun JsonObject.stringList(name: String): List<String> =
        array(name).mapNotNull {
            val p = it as? JsonPrimitive ?: return@mapNotNull null
            if (p is kotlinx.serialization.json.JsonNull || (!p.isString && p.content.equals("null", ignoreCase = true))) null
            else p.contentOrNull?.trim()?.takeIf { s -> s.isNotBlank() }
        }

    private fun String.resolveAgainstManifest(manifestUrl: String): String =
        when {
            startsWith("http://") || startsWith("https://") || startsWith("data:") -> this
            startsWith("//") -> "https:$this"
            else -> {
                val manifestBase = manifestUrl.substringBefore("?").substringBeforeLast('/', "")
                if (startsWith('/')) {
                    val origin = manifestBase.substringBefore("/", missingDelimiterValue = manifestBase)
                    val schemeAndHost = if (origin.contains("://")) {
                        val scheme = origin.substringBefore("://")
                        val host = manifestBase.substringAfter("://").substringBefore("/")
                        "$scheme://$host"
                    } else {
                        manifestBase
                    }
                    "$schemeAndHost$this"
                } else {
                    "$manifestBase/$this"
                }
            }
        }
}
