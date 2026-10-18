package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.StreamEmission
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = BingrPlugin(client)

    val tmdbId = 693134
    val url = "https://embed.vidrift.in/embed/movie/$tmdbId"
    println("Calling $url...")
    val req = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .header("Referer", "https://bingr.one/")
        .build()

    client.newCall(req).execute().use { resp ->
        val html = resp.body?.string().orEmpty()
        println("Fetched html len: ${html.length}")
        val emitted = mutableListOf<String>()
        val emittedKeys = mutableSetOf<String>()
        // invoke parseVidriftHtml via reflection or by checking what it does
        val method = BingrPlugin::class.java.getDeclaredMethod(
            "parseVidriftHtml",
            String::class.java,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            java.lang.Integer::class.java,
            java.lang.Integer::class.java,
            Long::class.javaPrimitiveType,
            List::class.java,
            MutableSet::class.java,
            kotlin.jvm.functions.Function3::class.java,
            kotlin.coroutines.Continuation::class.java
        )
        println("parseVidriftHtml method found: $method")
    }
}
