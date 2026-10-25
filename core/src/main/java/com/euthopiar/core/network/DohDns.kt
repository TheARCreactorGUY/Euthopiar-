package com.euthopiar.core.network

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Cache
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * High-performance DNS-over-HTTPS (DoH) resolver for OkHttp.
 *
 * Automatically bypasses ISP DNS poisoning, censorship, and DNS resolution failures
 * by querying Cloudflare (1.1.1.1) and Google (8.8.8.8) DoH endpoints over TLS.
 *
 * Implements in-memory TTL caching and graceful fallback to system DNS.
 */
class DohDns(
    private val timeoutSeconds: Long = 5
) : Dns {

    private val mapper = jacksonObjectMapper()

    // Isolated bootstrap client connecting directly by raw IP
    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private data class CacheEntry(
        val addresses: List<InetAddress>,
        val expiresAtMs: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private val ipv4Pattern = Pattern.compile("""^(\d{1,3}\.){3}\d{1,3}$""")

    override fun lookup(hostname: String): List<InetAddress> {
        val trimmed = hostname.trim()

        // 0. Loopback / localhost bypass
        if (trimmed.equals("localhost", ignoreCase = true) || trimmed == "127.0.0.1" || trimmed == "::1" || trimmed == "[::1]") {
            val host = trimmed.removeSurrounding("[", "]")
            return listOf(InetAddress.getByName(host))
        }

        // 1. Literal IP addresses need no DNS lookup
        if (ipv4Pattern.matcher(trimmed).matches()) {
            return listOf(InetAddress.getByName(trimmed))
        }

        // 2. DoH bootstrap endpoints
        if (trimmed == "1.1.1.1" || trimmed == "cloudflare-dns.com") {
            return listOf(InetAddress.getByAddress(trimmed, byteArrayOf(1, 1, 1, 1)))
        }
        if (trimmed == "1.0.0.1") {
            return listOf(InetAddress.getByAddress(trimmed, byteArrayOf(1, 0, 0, 1)))
        }
        if (trimmed == "8.8.8.8" || trimmed == "dns.google") {
            return listOf(InetAddress.getByAddress(trimmed, byteArrayOf(8, 8, 8, 8)))
        }
        if (trimmed == "8.8.4.4") {
            return listOf(InetAddress.getByAddress(trimmed, byteArrayOf(8, 8, 4, 4)))
        }

        // 3. In-memory TTL Cache
        val cached = cache[trimmed]
        val now = System.currentTimeMillis()
        if (cached != null && cached.expiresAtMs > now) {
            return cached.addresses
        }

        // 4. Concurrently race DoH endpoints: Cloudflare (1.1.1.1), Google (8.8.8.8), and Cloudflare Secondary (1.0.0.1)
        var addresses = raceDoh(trimmed, timeoutSeconds)

        // 5. Fallback: System DNS
        if (addresses.isEmpty()) {
            try {
                addresses = Dns.SYSTEM.lookup(trimmed)
            } catch (e: Exception) {
                // If system DNS also fails and we had an expired cache, return the expired entry as fallback
                if (cached != null && cached.addresses.isNotEmpty()) {
                    return cached.addresses
                }
                throw UnknownHostException("Unable to resolve host '$trimmed' via DoH or System DNS: ${e.message}")
            }
        }

        // Cache valid results for at least 60 seconds (up to 30 minutes)
        if (addresses.isNotEmpty()) {
            cache[trimmed] = CacheEntry(
                addresses = addresses,
                expiresAtMs = now + TimeUnit.MINUTES.toMillis(5)
            )
        }

        return addresses
    }

    private fun raceDoh(hostname: String, timeoutSec: Long): List<InetAddress> {
        val calls = ConcurrentLinkedQueue<Call>()
        val tasks = listOf<Callable<List<InetAddress>>>(
            Callable { queryCloudflare(hostname, calls) },
            Callable { queryGoogle(hostname, calls) },
            Callable { queryCloudflareSecondary(hostname, calls) }
        )
        val ecs = ExecutorCompletionService<List<InetAddress>>(dohExecutor)
        val futures = tasks.map { task -> ecs.submit(task) }
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)

        var completedCount = 0
        while (completedCount < tasks.size) {
            val remainingMs = (deadline - System.currentTimeMillis()).coerceAtLeast(0)
            if (remainingMs <= 0) break
            try {
                val future = ecs.poll(remainingMs, TimeUnit.MILLISECONDS) ?: break
                completedCount++
                val result = try { future.get() } catch (_: Exception) { emptyList() }
                if (result.isNotEmpty()) {
                    calls.forEach { runCatching { it.cancel() } }
                    futures.forEach { it.cancel(true) }
                    return result
                }
            } catch (_: Exception) {
                break
            }
        }
        calls.forEach { runCatching { it.cancel() } }
        futures.forEach { it.cancel(true) }
        return emptyList()
    }

    private fun queryCloudflare(hostname: String, calls: ConcurrentLinkedQueue<Call>): List<InetAddress> {
        val url = "https://1.1.1.1/dns-query?name=$hostname&type=A"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-json")
            .build()
        return executeDohRequest(request, hostname, calls)
    }

    private fun queryCloudflareSecondary(hostname: String, calls: ConcurrentLinkedQueue<Call>): List<InetAddress> {
        val url = "https://1.0.0.1/dns-query?name=$hostname&type=A"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-json")
            .build()
        return executeDohRequest(request, hostname, calls)
    }

    private fun queryGoogle(hostname: String, calls: ConcurrentLinkedQueue<Call>): List<InetAddress> {
        val url = "https://8.8.8.8/resolve?name=$hostname&type=A"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        return executeDohRequest(request, hostname, calls)
    }

    private fun executeDohRequest(
        request: Request,
        hostname: String,
        calls: ConcurrentLinkedQueue<Call>
    ): List<InetAddress> {
        val call = bootstrapClient.newCall(request)
        calls.add(call)
        return try {
            call.execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: return emptyList()
                parseDnsJson(bodyStr, hostname)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDnsJson(json: String, hostname: String): List<InetAddress> {
        return try {
            val root = mapper.readTree(json)
            val status = root.path("Status").asInt(-1)
            if (status != 0) return emptyList()

            val answerNode = root.path("Answer")
            if (!answerNode.isArray) return emptyList()

            val list = mutableListOf<InetAddress>()
            for (node in answerNode) {
                val type = node.path("type").asInt()
                // type 1 is DNS Type A (IPv4)
                if (type == 1) {
                    val ipStr = node.path("data").asText()
                    if (ipv4Pattern.matcher(ipStr).matches()) {
                        try {
                            val raw = InetAddress.getByName(ipStr).address
                            list.add(InetAddress.getByAddress(hostname, raw))
                        } catch (_: Exception) {}
                    }
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Prefetches DNS records in the background so cold network hops have zero DNS delay.
     */
    fun prefetch(vararg hosts: String) {
        Thread {
            for (h in hosts) {
                try {
                    lookup(h)
                } catch (_: Throwable) {}
            }
        }.start()
    }

    companion object {
        private val dohExecutor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "doh-dns-worker").apply { isDaemon = true }
        }

        val DEFAULT = DohDns()

        val defaultPool = ConnectionPool(maxIdleConnections = 10, keepAliveDuration = 5, timeUnit = TimeUnit.MINUTES)
        val defaultDispatcher = Dispatcher().apply { maxRequests = 128; maxRequestsPerHost = 32 }

        /** Root shared client. All derived clients share this connection pool, dispatcher, and SSL session cache. */
        val sharedBaseClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .dns(DEFAULT)
                .eventListener(BandwidthEstimator.Default.eventListener)
                .connectionPool(defaultPool)
                .dispatcher(defaultDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        @Volatile
        private var cachedClient: OkHttpClient? = null

        /** Optional disk-cache initializer for the shared client */
        fun initHttpCache(cacheDirectory: File, maxSizeBytes: Long = 50L * 1024 * 1024) {
            if (cachedClient != null) return
            synchronized(this) {
                if (cachedClient == null) {
                    try {
                        val cacheDir = File(cacheDirectory, "http_cache")
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val httpCache = Cache(cacheDir, maxSizeBytes)
                        cachedClient = sharedBaseClient.newBuilder()
                            .cache(httpCache)
                            .addNetworkInterceptor(ForceCacheInterceptor)
                            .build()
                    } catch (_: Throwable) {}
                }
            }
        }

        /** Client optimized for metadata (TMDB, AniList) with disk cache if initialized */
        val metadataClient: OkHttpClient
            get() = cachedClient ?: sharedBaseClient

        /**
         * Creates an OkHttpClient configured with automatic DNS-over-HTTPS.
         * Keeps clean isolation for scrapers and streaming CDNs.
         */
        fun createOkHttpClient(
            connectTimeoutSeconds: Long = 30,
            readTimeoutSeconds: Long = 45,
            dns: Dns = DEFAULT
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .dns(dns)
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}

private object ForceCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val resp = chain.proceed(req)
        val host = req.url.host
        if (req.method != "GET" || (host != "api.themoviedb.org" && host != "euthopiar-vault.sehgalvansh716.workers.dev") || !resp.isSuccessful) return resp
        return resp.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=600, stale-while-revalidate=86400")
            .build()
    }
}
