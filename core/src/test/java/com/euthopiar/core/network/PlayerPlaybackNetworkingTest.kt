package com.euthopiar.core.network

import okhttp3.Request
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
}
