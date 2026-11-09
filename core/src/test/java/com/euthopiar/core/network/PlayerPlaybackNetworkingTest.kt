package com.euthopiar.core.network

import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.concurrent.thread

class PlayerPlaybackNetworkingTest {

    private var serverSocket: ServerSocket? = null
    private var port: Int = 0

    @Before
    fun setUp() {
        PlayerPlaybackNetworking.resetPreferredHostsForTesting()
        serverSocket = ServerSocket(0)
        port = serverSocket!!.localPort
    }

    @After
    fun tearDown() {
        serverSocket?.close()
    }

    @Test
    fun testLoopbackPlaybackHttpClientRequest() {
        thread {
            try {
                val client = serverSocket?.accept() ?: return@thread
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val line = reader.readLine()
                if (line != null) {
                    val out = client.getOutputStream()
                    val body = "ok"
                    val resp = "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                    out.write(resp.toByteArray())
                    out.flush()
                }
                client.close()
            } catch (_: Exception) {}
        }

        val client = PlayerPlaybackNetworking.playbackHttpClient
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/stream/test")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            assertEquals("ok", response.body?.string())
        }
    }

    @Test
    fun testAuthorizationHeaderPreservedAcrossRedirect() {
        thread {
            try {
                // Request 1: returns 302 to /target
                val client1 = serverSocket?.accept() ?: return@thread
                val reader1 = BufferedReader(InputStreamReader(client1.getInputStream()))
                while (reader1.readLine().isNotEmpty()) {}
                val out1 = client1.getOutputStream()
                val resp1 = "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:$port/target\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                out1.write(resp1.toByteArray())
                out1.flush()
                client1.close()

                // Request 2: follow-up to /target
                val client2 = serverSocket?.accept() ?: return@thread
                val reader2 = BufferedReader(InputStreamReader(client2.getInputStream()))
                var authHeader: String? = null
                var headerLine = reader2.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    if (headerLine.startsWith("Authorization:", ignoreCase = true)) {
                        authHeader = headerLine.substringAfter(":").trim()
                    }
                    headerLine = reader2.readLine()
                }
                val out2 = client2.getOutputStream()
                val body2 = authHeader ?: "missing"
                val resp2 = "HTTP/1.1 200 OK\r\nContent-Length: ${body2.length}\r\nConnection: close\r\n\r\n$body2"
                out2.write(resp2.toByteArray())
                out2.flush()
                client2.close()
            } catch (_: Exception) {}
        }

        val client = PlayerPlaybackNetworking.playbackHttpClient
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/redirect")
            .header("Authorization", "Bearer secret_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            assertEquals("Bearer secret_token", response.body?.string())
        }
    }

    @Test
    fun testShouldStripAuthorization() {
        assertTrue(PlayerPlaybackNetworking.shouldStripAuthorization("https://bucket.r2.cloudflarestorage.com/video.mp4"))
        assertTrue(PlayerPlaybackNetworking.shouldStripAuthorization("https://storage.googleapis.com/bucket/stream.m3u8"))
        assertTrue(PlayerPlaybackNetworking.shouldStripAuthorization("https://s3.amazonaws.com/bucket/file?X-Amz-Signature=abc"))
        assertTrue(PlayerPlaybackNetworking.shouldStripAuthorization("https://example.com/stream?Signature=xyz&other=1"))
        assertFalse(PlayerPlaybackNetworking.shouldStripAuthorization("https://example.com/regular/video.mp4"))
        assertFalse(PlayerPlaybackNetworking.shouldStripAuthorization("https://torrentio.strem.fun/stream/123.mkv"))
    }

    @Test
    fun testPresignedUrlStripsAuthorizationHeader() {
        thread {
            try {
                val clientSock = serverSocket?.accept() ?: return@thread
                val reader = BufferedReader(InputStreamReader(clientSock.getInputStream()))
                var authHeader: String? = null
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    if (headerLine.startsWith("Authorization:", ignoreCase = true)) {
                        authHeader = headerLine.substringAfter(":").trim()
                    }
                    headerLine = reader.readLine()
                }
                val out = clientSock.getOutputStream()
                val body = authHeader ?: "stripped"
                val resp = "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                out.write(resp.toByteArray())
                out.flush()
                clientSock.close()
            } catch (_: Exception) {}
        }

        val client = PlayerPlaybackNetworking.playbackHttpClient
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/stream?X-Amz-Signature=dummy_sig")
            .header("Authorization", "Bearer should_be_stripped")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            assertEquals("Authorization must be stripped on presigned URLs", "stripped", response.body?.string())
        }
    }

    @Test
    fun testCrossHostRedirectStripsAuthorization() {
        var secondarySocket: ServerSocket? = null
        try {
            secondarySocket = ServerSocket(0)
            val port2 = secondarySocket.localPort

            // Server 1 (127.0.0.1:$port) redirects to Server 2 (localhost:$port2)
            thread {
                try {
                    val client1 = serverSocket?.accept() ?: return@thread
                    val reader1 = BufferedReader(InputStreamReader(client1.getInputStream()))
                    while (reader1.readLine().isNotEmpty()) {}
                    val out1 = client1.getOutputStream()
                    val resp1 = "HTTP/1.1 302 Found\r\nLocation: http://localhost:$port2/target\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    out1.write(resp1.toByteArray())
                    out1.flush()
                    client1.close()
                } catch (_: Exception) {}
            }

            thread {
                try {
                    val client2 = secondarySocket.accept() ?: return@thread
                    val reader2 = BufferedReader(InputStreamReader(client2.getInputStream()))
                    var authHeader: String? = null
                    var headerLine = reader2.readLine()
                    while (!headerLine.isNullOrEmpty()) {
                        if (headerLine.startsWith("Authorization:", ignoreCase = true)) {
                            authHeader = headerLine.substringAfter(":").trim()
                        }
                        headerLine = reader2.readLine()
                    }
                    val out2 = client2.getOutputStream()
                    val body2 = authHeader ?: "stripped_on_cross_domain"
                    val resp2 = "HTTP/1.1 200 OK\r\nContent-Length: ${body2.length}\r\nConnection: close\r\n\r\n$body2"
                    out2.write(resp2.toByteArray())
                    out2.flush()
                    client2.close()
                } catch (_: Exception) {}
            }

            val client = PlayerPlaybackNetworking.playbackHttpClient
            val request = Request.Builder()
                .url("http://127.0.0.1:$port/initial")
                .header("Authorization", "Bearer initial_token")
                .build()

            client.newCall(request).execute().use { response ->
                assertTrue(response.isSuccessful)
                assertEquals("stripped_on_cross_domain", response.body?.string())
            }
        } finally {
            secondarySocket?.close()
        }
    }

    @Test
    fun test403WithMozillaUserAgentRetriesWithLibmpvWithoutCrashing() {
        thread {
            try {
                // Request 1: Client sends Mozilla/5.0 UA, server rejects with 403 Forbidden
                val client1 = serverSocket?.accept() ?: return@thread
                val reader1 = BufferedReader(InputStreamReader(client1.getInputStream()))
                while (reader1.readLine().isNotEmpty()) {}
                val out1 = client1.getOutputStream()
                val body1 = "Forbidden"
                val resp1 = "HTTP/1.1 403 Forbidden\r\nContent-Length: ${body1.length}\r\nConnection: close\r\n\r\n$body1"
                out1.write(resp1.toByteArray())
                out1.flush()
                client1.close()

                // Request 2: Retry with libmpv
                val client2 = serverSocket?.accept() ?: return@thread
                val reader2 = BufferedReader(InputStreamReader(client2.getInputStream()))
                var receivedUa: String? = null
                var headerLine = reader2.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    if (headerLine.startsWith("User-Agent:", ignoreCase = true)) {
                        receivedUa = headerLine.substringAfter(":").trim()
                    }
                    headerLine = reader2.readLine()
                }
                val out2 = client2.getOutputStream()
                val body2 = if (receivedUa == "libmpv") "libmpv_ok" else "wrong_ua"
                val resp2 = "HTTP/1.1 200 OK\r\nContent-Length: ${body2.length}\r\nConnection: close\r\n\r\n$body2"
                out2.write(resp2.toByteArray())
                out2.flush()
                client2.close()
            } catch (_: Exception) {}
        }

        val client = PlayerPlaybackNetworking.playbackHttpClient
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/stream/protected")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro)")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue("Expected retry to succeed with 200 OK without crashing", response.isSuccessful)
            assertEquals("libmpv_ok", response.body?.string())
        }
    }

    @Test
    fun testDualFallbackAccumulatesHeadersWithoutReverting() {
        val initialRequest = Request.Builder()
            .url("https://bucket.r2.cloudflarestorage.com/video.mp4?X-Amz-Signature=abc")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .header("Referer", "https://locker.com/view")
            .header("Authorization", "Bearer token123")
            .build()

        // 1. First failure: 403 with browser UA triggers UA switch to libmpv
        val dummyResp403 = Response.Builder()
            .request(initialRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .build()

        val next1 = PlayerPlaybackNetworking.nextFallbackRequest(initialRequest, dummyResp403)
        assertNotNull("Should produce fallback request for 403 + browser UA", next1)
        assertEquals(PlayerPlaybackNetworking.NATIVE_PLAYER_USER_AGENT, next1?.header("User-Agent"))
        assertNull("Referer should be stripped", next1?.header("Referer"))
        assertEquals("Bearer token123", next1?.header("Authorization"))

        // 2. Second failure: 403 on S3 presigned URL with lingering Authorization
        val dummyRespS3 = Response.Builder()
            .request(next1!!)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .build()

        val next2 = PlayerPlaybackNetworking.nextFallbackRequest(next1, dummyRespS3)
        assertNotNull("Should produce second fallback request for lingering auth on presigned storage", next2)
        assertEquals("User-Agent must NOT revert to Mozilla/5.0!", PlayerPlaybackNetworking.NATIVE_PLAYER_USER_AGENT, next2?.header("User-Agent"))
        assertNull("Authorization must be stripped on presigned URL", next2?.header("Authorization"))
        assertNull("Referer must remain stripped", next2?.header("Referer"))
    }

    @Test
    fun testPresignedStorageStripsHeadersAcross302Redirect() {
        var secondarySocket: ServerSocket? = null
        try {
            secondarySocket = ServerSocket(0)
            val port2 = secondarySocket.localPort

            // Server 1 (127.0.0.1:$port) redirects to Server 2 presigned URL
            thread {
                try {
                    val client1 = serverSocket?.accept() ?: return@thread
                    val reader1 = BufferedReader(InputStreamReader(client1.getInputStream()))
                    while (reader1.readLine().isNotEmpty()) {}
                    val out1 = client1.getOutputStream()
                    val resp1 = "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:$port2/video.mp4?X-Amz-Signature=sig123\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    out1.write(resp1.toByteArray())
                    out1.flush()
                    client1.close()
                } catch (_: Exception) {}
            }

            // Server 2 (presigned URL destination)
            thread {
                try {
                    val client2 = secondarySocket.accept() ?: return@thread
                    val reader2 = BufferedReader(InputStreamReader(client2.getInputStream()))
                    var hasReferer = false
                    var hasOrigin = false
                    var hasAuth = false
                    var headerLine = reader2.readLine()
                    while (!headerLine.isNullOrEmpty()) {
                        if (headerLine.startsWith("Referer:", ignoreCase = true)) hasReferer = true
                        if (headerLine.startsWith("Origin:", ignoreCase = true)) hasOrigin = true
                        if (headerLine.startsWith("Authorization:", ignoreCase = true)) hasAuth = true
                        headerLine = reader2.readLine()
                    }
                    val out2 = client2.getOutputStream()
                    val body2 = if (!hasReferer && !hasOrigin && !hasAuth) "clean_presigned_redirect" else "polluted"
                    val resp2 = "HTTP/1.1 200 OK\r\nContent-Length: ${body2.length}\r\nConnection: close\r\n\r\n$body2"
                    out2.write(resp2.toByteArray())
                    out2.flush()
                    client2.close()
                } catch (_: Exception) {}
            }

            val client = PlayerPlaybackNetworking.playbackHttpClient
            val request = Request.Builder()
                .url("http://127.0.0.1:$port/gateway/resolve")
                .header("Referer", "https://locker.example.com")
                .header("Origin", "https://locker.example.com")
                .header("Authorization", "Bearer gateway_token")
                .build()

            client.newCall(request).execute().use { response ->
                assertTrue(response.isSuccessful)
                assertEquals("clean_presigned_redirect", response.body?.string())
            }
        } finally {
            secondarySocket?.close()
        }
    }

    @Test
    fun testCancelLoopbackPlaybackCalls() {
        val countDownLatch = java.util.concurrent.CountDownLatch(1)
        var callCanceled = false

        val call = PlayerPlaybackNetworking.baseClient.newCall(
            Request.Builder().url("http://127.0.0.1:$port/blocking/stream").build()
        )

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (call.isCanceled()) {
                    callCanceled = true
                }
                countDownLatch.countDown()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.close()
                countDownLatch.countDown()
            }
        })

        Thread.sleep(100)
        PlayerPlaybackNetworking.cancelLoopbackPlaybackCalls()
        countDownLatch.await(2, java.util.concurrent.TimeUnit.SECONDS)

        assertTrue("Loopback call should be canceled by cancelLoopbackPlaybackCalls()", callCanceled)
    }

    @Test
    fun testFallback403Retries() {
        val countDownLatch = java.util.concurrent.CountDownLatch(1)
        var finalCode = 0
        var finalBody = ""
        var caughtException: Throwable? = null

        val secondarySocket = java.net.ServerSocket(0)
        val port2 = secondarySocket.localPort

        try {
            kotlin.concurrent.thread {
                try {
                    // Server 1: 302 Found redirecting to Server 2 (loop to serve initial + retries)
                    while (!secondarySocket.isClosed) {
                        val client1 = serverSocket?.accept() ?: break
                        val reader1 = java.io.BufferedReader(java.io.InputStreamReader(client1.getInputStream()))
                        var line = reader1.readLine()
                        while (!line.isNullOrEmpty()) {
                            line = reader1.readLine()
                        }
                        val out1 = client1.getOutputStream()
                        val resp1 = "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:$port2/stream/dest.mp4\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                        out1.write(resp1.toByteArray())
                        out1.flush()
                        client1.close()
                    }
                } catch (_: Exception) {}
            }

            kotlin.concurrent.thread {
                try {
                    var requestCount = 0
                    while (!secondarySocket.isClosed) {
                        val client = secondarySocket.accept() ?: break
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(client.getInputStream()))
                        var line = reader.readLine()
                        while (!line.isNullOrEmpty()) {
                            line = reader.readLine()
                        }
                        val out = client.getOutputStream()
                        if (requestCount == 0) {
                            requestCount++
                            val body = "Forbidden initial"
                            val resp = "HTTP/1.1 403 Forbidden\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                            out.write(resp.toByteArray())
                            out.flush()
                            client.close()
                        } else {
                            val body = "Success fallback"
                            val resp = "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                            out.write(resp.toByteArray())
                            out.flush()
                            client.close()
                        }
                    }
                } catch (_: Exception) {}
            }

            val client = PlayerPlaybackNetworking.playbackHttpClient
            val request = Request.Builder()
                .url("http://127.0.0.1:$port/stream/initial")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    caughtException = e
                    countDownLatch.countDown()
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    finalCode = response.code
                    finalBody = response.body?.string() ?: ""
                    countDownLatch.countDown()
                }
            })

            countDownLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            caughtException?.let { throw it }
            assertEquals(200, finalCode)
            assertEquals("Success fallback", finalBody)
        } finally {
            secondarySocket.close()
        }
    }
}
