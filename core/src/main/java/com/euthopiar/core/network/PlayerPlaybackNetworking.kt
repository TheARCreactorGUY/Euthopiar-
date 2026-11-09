package com.euthopiar.core.network

import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * High-performance, fault-tolerant networking client designed specifically for media playback.
 *
 * Implements the approved Fix 3 networking architecture:
 * 1. Split-Horizon DNS: Default to native Android System DNS (Dns.SYSTEM) for media CDNs/segments
 *    to preserve carrier-local edge routing and ECS, with in-memory TTL caching (180s floor).
 *    Fallback to DoH (1.1.1.1 / 8.8.8.8) on UnknownHostException or sinkhole IPs.
 * 2. Unified ConnectionPool (20 idle, 5 min keep-alive) shared across probe and ExoPlayer clients.
 * 3. Snappy timeouts: 8s connectTimeout, 10s readTimeout (inactivity between bytes).
 * 4. IPv4-First Interleaving + fastFallback(true) (Happy Eyeballs) to defeat mobile carrier IPv6 blackholes.
 */
object PlayerPlaybackNetworking {

    val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    val sslSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllManager), SecureRandom())
    }.socketFactory

    /**
     * Unified connection pool shared across validation probes and ExoPlayer media sockets.
     * Permits true zero-handshake socket reuse.
     */
    val sharedConnectionPool = ConnectionPool(20, 5, TimeUnit.MINUTES)

    private data class CachedDnsEntry(val addresses: List<InetAddress>, val timestampMs: Long)
    private val dnsCache = ConcurrentHashMap<String, CachedDnsEntry>()
    private const val DNS_CACHE_TTL_MS = 180_000L // 3 minutes floor TTL

    /**
     * Domain-Tiered Split-Horizon DNS with IPv4-first interleaving and in-memory TTL cache.
     */
    class SplitHorizonDns(
        private val fallbackDns: Dns = DohDns.DEFAULT
    ) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val now = System.currentTimeMillis()
            dnsCache[hostname]?.let { cached ->
                if (now - cached.timestampMs < DNS_CACHE_TTL_MS && cached.addresses.isNotEmpty()) {
                    return cached.addresses
                }
            }

            if (isLoopbackHost(hostname)) {
                val loopbackAddrs = Dns.SYSTEM.lookup(hostname)
                dnsCache[hostname] = CachedDnsEntry(loopbackAddrs, now)
                return loopbackAddrs
            }

            val resolved: List<InetAddress> = try {
                val systemResult = Dns.SYSTEM.lookup(hostname)
                if (systemResult.isEmpty() || isSinkhole(systemResult)) {
                    fallbackDns.lookup(hostname)
                } else {
                    systemResult
                }
            } catch (e: UnknownHostException) {
                fallbackDns.lookup(hostname)
            } catch (t: Throwable) {
                try { fallbackDns.lookup(hostname) } catch (_: Throwable) { throw t }
            }

            val ordered = interleaveIpv4First(resolved)
            dnsCache[hostname] = CachedDnsEntry(ordered, now)
            return ordered
        }

        private fun isSinkhole(addresses: List<InetAddress>): Boolean =
            addresses.all { it.isLoopbackAddress || it.isAnyLocalAddress }

        private fun interleaveIpv4First(addresses: List<InetAddress>): List<InetAddress> {
            if (addresses.size <= 1) return addresses
            val v4 = ArrayList<InetAddress>(addresses.size)
            val v6 = ArrayList<InetAddress>(addresses.size)
            for (addr in addresses) {
                if (addr is Inet4Address) v4.add(addr) else v6.add(addr)
            }
            return if (v4.isNotEmpty()) v4 + v6 else addresses
        }
    }

    val playbackDns: Dns = SplitHorizonDns()

    data class OriginRequestMetadata(
        val originalHost: String,
        val referer: String?,
        val origin: String?,
        val authorization: String?,
        val cookie: String?
    )

    fun isLoopbackHost(host: String): Boolean = when (host.lowercase()) {
        "127.0.0.1", "localhost", "::1" -> true
        else -> false
    }

    fun isPreSignedStorageUrl(url: okhttp3.HttpUrl): Boolean {
        val host = url.host.lowercase()
        val fullUrl = url.toString().lowercase()
        val isStorageHost = host.contains("r2.cloudflarestorage.com") ||
                host.contains(".r2.dev") ||
                host.contains("amazonaws.com") ||
                host.contains("storage.googleapis.com") ||
                host.contains("blob.core.windows.net") ||
                host.contains("digitaloceanspaces.com") ||
                host.contains("backblazeb2.com")
        val hasSigParams = url.queryParameterNames.any { name ->
            val lowerName = name.lowercase()
            lowerName.startsWith("x-amz-") ||
                    lowerName == "sig" ||
                    lowerName == "signature" ||
                    lowerName.contains("signature") ||
                    lowerName == "googleaccessid"
        } || fullUrl.contains("x-amz-signature") || fullUrl.contains("signature=")
        return isStorageHost || hasSigParams
    }

    fun shouldStripAuthorization(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull()
        return if (parsed != null) isPreSignedStorageUrl(parsed) else {
            val lower = url.lowercase()
            lower.contains("x-amz-signature") ||
                    lower.contains("signature=") ||
                    lower.contains("r2.cloudflarestorage.com") ||
                    lower.contains("storage.googleapis.com")
        }
    }

    const val NATIVE_PLAYER_USER_AGENT = "libmpv"
    private val nativeUaPreferredHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    @androidx.annotation.VisibleForTesting
    fun resetPreferredHostsForTesting() {
        nativeUaPreferredHosts.clear()
    }

    fun requiresNativePlayerUserAgent(host: String): Boolean {
        val h = host.lowercase()
        return h in nativeUaPreferredHosts ||
            h.contains("hakunaymatata.com") ||
            h.contains("aoneroom.com") ||
            h.contains("inmoviebox.com")
    }

    /**
     * Determines the next request in the adaptive fallback chain, or null if no further retry applies.
     * Pure function ensuring monotonic request transformation (accumulates fixes without clobbering).
     */
    fun nextFallbackRequest(request: Request, response: Response): Request? {
        val isUaBlocked = (response.code == 428 || response.code == 403)
        val hasBrowserUa = (request.header("User-Agent") ?: "").startsWith("Mozilla/5.0")
        if (isUaBlocked && hasBrowserUa) {
            return request.newBuilder()
                .header("User-Agent", NATIVE_PLAYER_USER_AGENT)
                .removeHeader("Referer")
                .removeHeader("Origin")
                .build()
        }

        if (response.code == 403) {
            val isPreSigned = isPreSignedStorageUrl(request.url)
            val hasReferer = !request.header("Referer").isNullOrBlank()
            val hasOrigin = !request.header("Origin").isNullOrBlank()
            val hasAuth = !request.header("Authorization").isNullOrBlank()
            val hasCookie = !request.header("Cookie").isNullOrBlank()
            if (isPreSigned && (hasReferer || hasOrigin || hasAuth || hasCookie)) {
                return request.newBuilder()
                    .removeHeader("Referer")
                    .removeHeader("Origin")
                    .removeHeader("Authorization")
                    .removeHeader("Cookie")
                    .build()
            }
        }

        return null
    }

    /**
     * Root foundational OkHttpClient owning the shared ConnectionPool, DNS router, and TLS settings.
     */
    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .sslSocketFactory(sslSocketFactory, trustAllManager)
            .hostnameVerifier { _, _ -> true }
            .dns(playbackDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val isLoopback = isLoopbackHost(originalRequest.url.host)
                val requestChain = if (isLoopback) {
                    chain.withConnectTimeout(65, TimeUnit.SECONDS)
                        .withReadTimeout(65, TimeUnit.SECONDS)
                        .withWriteTimeout(65, TimeUnit.SECONDS)
                } else {
                    chain
                }

                val baseRequest = if (originalRequest.tag(OriginRequestMetadata::class.java) == null) {
                    originalRequest.newBuilder().tag(
                        OriginRequestMetadata::class.java,
                        OriginRequestMetadata(
                            originalHost = originalRequest.url.host,
                            referer = originalRequest.header("Referer"),
                            origin = originalRequest.header("Origin"),
                            authorization = originalRequest.header("Authorization"),
                            cookie = originalRequest.header("Cookie")
                        )
                    ).build()
                } else {
                    originalRequest
                }

                // Pre-request normalization: if the target host is known to require native player UA, apply upfront
                val targetHost = baseRequest.url.host.lowercase()
                var currentRequest = if (requiresNativePlayerUserAgent(targetHost)) {
                    val currentUa = baseRequest.header("User-Agent")
                    if (currentUa != null && currentUa.startsWith("Mozilla/5.0")) {
                        baseRequest.newBuilder()
                            .header("User-Agent", NATIVE_PLAYER_USER_AGENT)
                            .removeHeader("Referer")
                            .removeHeader("Origin")
                            .build()
                    } else {
                        baseRequest
                    }
                } else {
                    baseRequest
                }

                var resp = requestChain.proceed(currentRequest)
                var attempts = 0
                val maxAttempts = 2

                while (attempts < maxAttempts && !requestChain.call().isCanceled()) {
                    val nextRequest = nextFallbackRequest(currentRequest, resp) ?: break
                    resp.close()
                    currentRequest = nextRequest
                    resp = requestChain.proceed(currentRequest)
                    attempts++

                    // Commit host to verified cache ONLY after verified success
                    if (resp.isSuccessful) {
                        val successfulHost = resp.request.url.host.lowercase()
                        val usedNativeUa = (currentRequest.header("User-Agent") == NATIVE_PLAYER_USER_AGENT)
                        if (usedNativeUa && !isLoopbackHost(successfulHost)) {
                            nativeUaPreferredHosts.add(successfulHost)
                        }
                    }
                }

                resp
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val meta = request.tag(OriginRequestMetadata::class.java)
                val isPreSigned = isPreSignedStorageUrl(request.url)
                val isSameHost = meta != null && request.url.host.equals(meta.originalHost, ignoreCase = true)

                val builder = request.newBuilder()
                var modified = false

                if (requiresNativePlayerUserAgent(request.url.host)) {
                    val currentUa = request.header("User-Agent")
                    if (currentUa != null && currentUa.startsWith("Mozilla/5.0")) {
                        builder.header("User-Agent", NATIVE_PLAYER_USER_AGENT)
                        modified = true
                    }
                }

                if (isPreSigned) {
                    if (request.header("Authorization") != null) {
                        builder.removeHeader("Authorization")
                        modified = true
                    }
                    if (request.header("Cookie") != null) {
                        builder.removeHeader("Cookie")
                        modified = true
                    }
                    if (request.header("Referer") != null) {
                        builder.removeHeader("Referer")
                        modified = true
                    }
                    if (request.header("Origin") != null) {
                        builder.removeHeader("Origin")
                        modified = true
                    }
                } else if (isSameHost) {
                    if (request.header("Authorization") == null && meta.authorization != null) {
                        builder.header("Authorization", meta.authorization)
                        modified = true
                    }
                    if (request.header("Cookie") == null && meta.cookie != null) {
                        builder.header("Cookie", meta.cookie)
                        modified = true
                    }
                }

                if (modified) {
                    chain.proceed(builder.build())
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }

    val playbackHttpClient: OkHttpClient get() = baseClient

    /**
     * Specialized lightweight probe client derived from baseClient sharing the exact same
     * ConnectionPool and DNS router for zero-handshake socket handoff to ExoPlayer.
     */
    val probeHttpClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapNotNull { (rawKey, rawValue) ->
            val key = rawKey.trim()
            val value = rawValue.trim()
            if (key.isBlank() || value.isBlank() || key.equals("Range", ignoreCase = true)) {
                null
            } else {
                key to value
            }
        }.toMap()

    private fun canonicalHeaderName(name: String): String {
        return when (name.lowercase()) {
            "user-agent" -> "User-Agent"
            "referer" -> "Referer"
            "origin" -> "Origin"
            "cookie" -> "Cookie"
            "authorization" -> "Authorization"
            "accept" -> "Accept"
            "range" -> "Range"
            "content-type" -> "Content-Type"
            else -> name.trim()
        }
    }

    fun extractUrlAndInlineHeaders(
        rawUrl: String,
        existingHeaders: Map<String, String> = emptyMap()
    ): Pair<String, Map<String, String>> {
        val cleanUrl = if (rawUrl.contains('|')) rawUrl.substringBefore('|').trim() else rawUrl.trim()
        val inlineHeadersString = if (rawUrl.contains('|')) rawUrl.substringAfter('|').trim() else ""

        val normalized = LinkedHashMap<String, Pair<String, String>>()
        for ((k, v) in existingHeaders) {
            val trimmedKey = k.trim()
            if (trimmedKey.isNotBlank() && v.isNotBlank()) {
                val canonicalKey = canonicalHeaderName(trimmedKey)
                normalized[canonicalKey.lowercase()] = canonicalKey to v.trim()
            }
        }

        if (inlineHeadersString.isNotEmpty()) {
            inlineHeadersString.split('&').forEach { param ->
                val idx = param.indexOf('=')
                if (idx > 0) {
                    val rawK = param.substring(0, idx).trim()
                    val rawV = param.substring(idx + 1).trim()
                    val k = try { java.net.URLDecoder.decode(rawK, "UTF-8") } catch (_: Exception) { rawK }
                    val v = try { java.net.URLDecoder.decode(rawV, "UTF-8") } catch (_: Exception) { rawV }
                    if (k.isNotBlank() && v.isNotBlank()) {
                        val canonicalKey = canonicalHeaderName(k)
                        normalized[canonicalKey.lowercase()] = canonicalKey to v
                    }
                }
            }
        }

        val resultHeaders = normalized.values.associate { it.first to it.second }
        return Pair(cleanUrl, resultHeaders)
    }

    fun createClient(cookieJar: CookieJar? = null): OkHttpClient {
        val builder = baseClient.newBuilder()
        if (cookieJar != null) {
            builder.cookieJar(cookieJar)
        }
        return builder.build()
    }

    /**
     * Forcefully cancels all active and queued in-flight calls to loopback hosts (127.0.0.1, localhost).
     * This immediately aborts the active ExoPlayer streaming socket via TCP RST/FIN without touching
     * any external network calls (such as TMDB, Simkl/Trakt, SubDL subtitles, or poster image loading).
     * The local native P2P HTTP daemon detects EOF/RST in <5ms, unblocking stream route teardown instantly.
     */
    fun cancelLoopbackPlaybackCalls() {
        try {
            baseClient.dispatcher.runningCalls()
                .filter { isLoopbackHost(it.request().url.host) }
                .forEach { runCatching { it.cancel() } }
            baseClient.dispatcher.queuedCalls()
                .filter { isLoopbackHost(it.request().url.host) }
                .forEach { runCatching { it.cancel() } }
        } catch (_: Throwable) {}
    }
}
