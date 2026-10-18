package com.euthopiar.core.js.network

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import com.euthopiar.core.js.host.HostModule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class UrlBridge : HostModule {
    override fun register(runtime: QuickJs) {
        runtime.function("__parse_url") { args: Array<Any?> ->
            val urlString = args.getOrNull(0)?.toString() ?: ""
            parseUrl(urlString)
        }
    }

    private fun parseUrl(urlString: String): String {
        return try {
            val parsed = urlString.toHttpUrlOrNull()
            if (parsed != null) {
                JsonObject(
                    mapOf(
                        "protocol" to JsonPrimitive("${parsed.scheme}:"),
                        "host" to JsonPrimitive(
                            if (parsed.port != okhttp3.HttpUrl.defaultPort(parsed.scheme)) {
                                "${parsed.host}:${parsed.port}"
                            } else {
                                parsed.host
                            }
                        ),
                        "hostname" to JsonPrimitive(parsed.host),
                        "port" to JsonPrimitive(
                            if (parsed.port != okhttp3.HttpUrl.defaultPort(parsed.scheme)) parsed.port.toString() else ""
                        ),
                        "pathname" to JsonPrimitive(parsed.encodedPath.ifBlank { "/" }),
                        "search" to JsonPrimitive(parsed.encodedQuery?.let { "?$it" } ?: ""),
                        "hash" to JsonPrimitive(parsed.encodedFragment?.let { "#$it" } ?: "")
                    )
                ).toString()
            } else {
                fallbackUrlJson()
            }
        } catch (_: Exception) {
            fallbackUrlJson()
        }
    }

    private fun fallbackUrlJson(): String = JsonObject(
        mapOf(
            "protocol" to JsonPrimitive(""),
            "host" to JsonPrimitive(""),
            "hostname" to JsonPrimitive(""),
            "port" to JsonPrimitive(""),
            "pathname" to JsonPrimitive("/"),
            "search" to JsonPrimitive(""),
            "hash" to JsonPrimitive("")
        )
    ).toString()
}
