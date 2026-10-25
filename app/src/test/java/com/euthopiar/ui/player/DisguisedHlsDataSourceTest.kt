package com.euthopiar.ui.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@UnstableApi
class DisguisedHlsDataSourceTest {

    @Test
    fun stripsDecoyBytesPrecedingMpegTsSyncByte() {
        // Construct fake payload: 32 bytes of decoy PNG header + MPEG-TS packets starting with 0x47
        val decoyHeader = ByteArray(32) { 0x89.toByte() }
        val tsPacket1 = ByteArray(188) { 0 }
        tsPacket1[0] = 0x47.toByte()
        val tsPacket2 = ByteArray(188) { 0 }
        tsPacket2[0] = 0x47.toByte()

        val fullPayload = decoyHeader + tsPacket1 + tsPacket2
        val upstream = ByteArrayDataSource(fullPayload)
        val disguisedSource = DisguisedHlsDataSource(upstream)

        val uri = Uri.parse("https://cdn.example.com/segment_001.ts")
        disguisedSource.open(DataSpec(uri))

        val readBuffer = ByteArray(512)
        val bytesRead = disguisedSource.read(readBuffer, 0, readBuffer.size)

        assertEquals(tsPacket1.size + tsPacket2.size, bytesRead)
        assertEquals("First byte must be MPEG-TS sync byte 0x47", 0x47.toByte(), readBuffer[0])
        assertEquals("Byte at offset 188 must be MPEG-TS sync byte 0x47", 0x47.toByte(), readBuffer[188])
    }

    @Test
    fun leavesStandardTsStreamUntouched() {
        val tsPacket1 = ByteArray(188) { 0 }
        tsPacket1[0] = 0x47.toByte()
        val tsPacket2 = ByteArray(188) { 0 }
        tsPacket2[0] = 0x47.toByte()

        val fullPayload = tsPacket1 + tsPacket2
        val upstream = ByteArrayDataSource(fullPayload)
        val disguisedSource = DisguisedHlsDataSource(upstream)

        disguisedSource.open(DataSpec(Uri.parse("https://cdn.example.com/clean.ts")))

        val readBuffer = ByteArray(512)
        val bytesRead = disguisedSource.read(readBuffer, 0, readBuffer.size)

        assertEquals(fullPayload.size, bytesRead)
        assertEquals(0x47.toByte(), readBuffer[0])
    }

    @Test
    fun retriesWithMasterPlaylistQueryTokensOnHttp403() {
        var requestedUri: Uri? = null
        var requestedHeaders: Map<String, String> = emptyMap()
        var openCallCount = 0

        val fakeUpstream = object : BaseDataSource(true) {
            override fun open(dataSpec: DataSpec): Long {
                openCallCount++
                requestedUri = dataSpec.uri
                requestedHeaders = dataSpec.httpRequestHeaders

                val token = dataSpec.uri.getQueryParameter("token")
                val sig = dataSpec.uri.getQueryParameter("sig")

                // If token and sig are absent, reject with HTTP 403
                if (token.isNullOrBlank() || sig.isNullOrBlank()) {
                    throw HttpDataSource.InvalidResponseCodeException(
                        403,
                        "Forbidden: signed query token required",
                        null,
                        emptyMap(),
                        dataSpec,
                        ByteArray(0)
                    )
                }

                return 188L
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (length > 0) {
                    buffer[offset] = 0x47.toByte()
                    return 1
                }
                return C.RESULT_END_OF_INPUT
            }

            override fun getUri(): Uri? = requestedUri

            override fun close() {}
        }

        val factory = DisguisedHlsDataSource.Factory(
            upstreamFactory = { fakeUpstream },
            initialMasterUrl = "https://cdn.example.com/stream/master.m3u8?token=SECRET_AUTH_TOKEN&sig=ABC123XYZ&hdnts=exp=99999",
            initialMasterHeaders = mapOf("Authorization" to "Bearer test-jwt")
        )

        val dataSource = factory.createDataSource()

        // Child segment URL that omits the query string parameters
        val childUri = Uri.parse("https://cdn.example.com/stream/variant_1080p/segment_001.ts")
        val dataSpec = DataSpec.Builder().setUri(childUri).build()

        val length = dataSource.open(dataSpec)
        assertEquals(188L, length)
        assertEquals(2, openCallCount) // 1st try failed with 403, 2nd try retried and succeeded

        val finalUri = dataSource.getUri()
        assertEquals("SECRET_AUTH_TOKEN", finalUri?.getQueryParameter("token"))
        assertEquals("ABC123XYZ", finalUri?.getQueryParameter("sig"))
        assertEquals("exp=99999", finalUri?.getQueryParameter("hdnts"))
        assertEquals("Bearer test-jwt", requestedHeaders["Authorization"])
    }

    @Test
    fun preservesEncodedDelimitersAndHmacInSignedTokensOn403Retry() {
        var requestedUri: Uri? = null
        val fakeUpstream = object : BaseDataSource(true) {
            override fun open(dataSpec: DataSpec): Long {
                requestedUri = dataSpec.uri
                val encodedQuery = dataSpec.uri.encodedQuery ?: ""
                if (!encodedQuery.contains("sig=abc%2Bdef%2F123%3D%3D")) {
                    throw HttpDataSource.InvalidResponseCodeException(
                        403,
                        "Forbidden",
                        null,
                        emptyMap(),
                        dataSpec,
                        ByteArray(0)
                    )
                }
                return 188L
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = C.RESULT_END_OF_INPUT
            override fun getUri(): Uri? = requestedUri
            override fun close() {}
        }

        val rawMasterQuery = "token=st%3D12345%7Eexp%3D67890%7Eacl%3D%2F*&sig=abc%2Bdef%2F123%3D%3D"
        val factory = DisguisedHlsDataSource.Factory(
            upstreamFactory = { fakeUpstream },
            initialMasterUrl = "https://cdn.example.com/stream/master.m3u8?$rawMasterQuery"
        )
        val dataSource = factory.createDataSource()

        val childUri = Uri.parse("https://cdn.example.com/stream/key.php?key_id=99")
        val dataSpec = DataSpec.Builder().setUri(childUri).build()

        dataSource.open(dataSpec)
        val finalUri = dataSource.getUri()
        val finalEncodedQuery = finalUri?.encodedQuery ?: ""

        assertTrue("Final query must contain original uncorrupted key_id", finalEncodedQuery.contains("key_id=99"))
        assertTrue("Final query must preserve encoded %2B and %2F without mutation", finalEncodedQuery.contains("sig=abc%2Bdef%2F123%3D%3D"))
        assertTrue("Final query must preserve encoded %3D and %7E without mutation", finalEncodedQuery.contains("token=st%3D12345%7Eexp%3D67890%7Eacl%3D%2F*"))
    }

    @Test
    fun childVariantQueryDoesNotOverwriteMasterQueryTokens() {
        var requestedUri: Uri? = null
        val fakeUpstream = object : BaseDataSource(true) {
            override fun open(dataSpec: DataSpec): Long {
                requestedUri = dataSpec.uri
                val path = dataSpec.uri.path ?: ""
                if (path.endsWith(".php")) {
                    val token = dataSpec.uri.getQueryParameter("token")
                    if (token != "MASTER_SECRET_TOKEN") {
                        throw HttpDataSource.InvalidResponseCodeException(403, "Forbidden", null, emptyMap(), dataSpec, ByteArray(0))
                    }
                }
                return 100L
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = C.RESULT_END_OF_INPUT
            override fun getUri(): Uri? = requestedUri
            override fun close() {}
        }

        val factory = DisguisedHlsDataSource.Factory(
            upstreamFactory = { fakeUpstream },
            initialMasterUrl = "https://cdn.example.com/master.m3u8?token=MASTER_SECRET_TOKEN"
        )
        val dataSource = factory.createDataSource()

        // 1. Child variant playlist with its own bandwidth query parameter
        val variantUri = Uri.parse("https://cdn.example.com/variant_1080p.m3u8?bandwidth=5000000")
        dataSource.open(DataSpec.Builder().setUri(variantUri).build())

        // 2. Subsequent AES-128 key request omitting query tokens
        val keyUri = Uri.parse("https://cdn.example.com/hls/aes128.php")
        dataSource.open(DataSpec.Builder().setUri(keyUri).build())

        val finalKeyUri = dataSource.getUri()
        assertEquals("MASTER_SECRET_TOKEN", finalKeyUri?.getQueryParameter("token"))
    }

    @Test
    fun fmp4SegmentDoesNotHaveVideoMp2tForced() {
        val fakeUpstream = object : BaseDataSource(true) {
            override fun open(dataSpec: DataSpec): Long = 100L
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = C.RESULT_END_OF_INPUT
            override fun getUri(): Uri? = Uri.parse("https://cdn.example.com/stream/segment_001.m4s")
            override fun getResponseHeaders(): Map<String, List<String>> = mapOf("Content-Type" to listOf("image/jpeg"))
            override fun close() {}
        }

        val source = DisguisedHlsDataSource(fakeUpstream)
        source.open(DataSpec.Builder().setUri(Uri.parse("https://cdn.example.com/stream/segment_001.m4s")).build())

        val headers = source.responseHeaders
        val contentType = headers["Content-Type"]?.firstOrNull()
        org.junit.Assert.assertNotEquals("fMP4 segment (.m4s) must NOT be forced to video/mp2t", "video/mp2t", contentType)
    }
}
