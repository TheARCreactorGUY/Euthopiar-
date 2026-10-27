package com.euthopiar.core.addons

fun addonTransportBaseUrl(manifestUrl: String): String =
    manifestUrl.substringBefore("?").removeSuffix("/manifest.json")

fun buildAddonResourceUrl(
    manifestUrl: String,
    resource: String,
    type: String,
    id: String,
    extraPathSegment: String? = null
): String {
    val encodedId = id.encodeAddonPathSegment()
    val baseUrl = addonTransportBaseUrl(manifestUrl)
    val query = manifestUrl.substringAfter("?", "").let { q ->
        if (q.isBlank()) "" else "?$q"
    }
    val resourceUrl = if (extraPathSegment.isNullOrEmpty()) {
        "$baseUrl/$resource/$type/$encodedId.json"
    } else {
        "$baseUrl/$resource/$type/$encodedId/$extraPathSegment.json"
    }
    return resourceUrl + query
}

fun String.encodeAddonPathSegment(): String =
    buildString {
        var i = 0
        while (i < this@encodeAddonPathSegment.length) {
            val char = this@encodeAddonPathSegment[i]
            if (char == '%' && i + 2 < this@encodeAddonPathSegment.length &&
                this@encodeAddonPathSegment[i + 1].isAddonHexDigit() &&
                this@encodeAddonPathSegment[i + 2].isAddonHexDigit()
            ) {
                append('%')
                append(this@encodeAddonPathSegment[i + 1])
                append(this@encodeAddonPathSegment[i + 2])
                i += 3
            } else if (
                char in 'a'..'z' ||
                char in 'A'..'Z' ||
                char in '0'..'9' ||
                char == '-' ||
                char == '_' ||
                char == '.' ||
                char == '~' ||
                char == ':'
            ) {
                append(char)
                i++
            } else {
                val bytes = char.toString().encodeToByteArray()
                for (byte in bytes) {
                    val value = byte.toInt() and 0xFF
                    append('%')
                    append(ADDON_URL_HEX[value shr 4])
                    append(ADDON_URL_HEX[value and 0x0F])
                }
                i++
            }
        }
    }

private fun Char.isAddonHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private const val ADDON_URL_HEX = "0123456789ABCDEF"
