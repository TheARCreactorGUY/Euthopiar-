package com.euthopiar.ui.player.sequential

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException

@UnstableApi
class SequentialStreamDataSource(
    private val session: SequentialStreamSession
) : BaseDataSource(/* isNetwork = */ true) {

    private var uri: Uri? = null
    private var readPosition: Long = 0L
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    private var useRangeRequest: Boolean = false
    private var rangeResponse: Response? = null
    private var rangeInputStream: InputStream? = null

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        this.uri = dataSpec.uri

        val targetOffset = dataSpec.position
        val isAvailableInSession = (targetOffset == 0L) || session.isOffsetBuffered(targetOffset)

        if (isAvailableInSession) {
            useRangeRequest = false
            cleanupRangeStream()
            try {
                this.readPosition = session.awaitOffsetOrCluster(targetOffset)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Interrupted while bridging sequential seek")
            }

            val totalLen = session.totalContentLength
            this.bytesRemaining = when {
                dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
                totalLen != -1L -> (totalLen - readPosition).coerceAtLeast(0L)
                else -> C.LENGTH_UNSET.toLong()
            }
        } else {
            // Offset is outside local sequential buffer (e.g. EOF Cues fetch or forward scrubbing).
            // Hit the pinned CDN URL directly with an HTTP Range request.
            val targetUrl = SequentialStreamManager.getPinnedUrl(session.url) ?: session.resolvedUrl
            val reqBuilder = Request.Builder().url(targetUrl)
            session.headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val rangeHeader = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                "bytes=$targetOffset-${targetOffset + dataSpec.length - 1}"
            } else {
                "bytes=$targetOffset-"
            }
            reqBuilder.header("Range", rangeHeader)

            var rangeOpened = false
            try {
                val response = session.okHttpClient.newCall(reqBuilder.build()).execute()
                if (response.code == 206 || (response.code == 200 && targetOffset == 0L)) {
                    val finalUrl = response.request.url.toString()
                    session.resolvedUrl = finalUrl
                    SequentialStreamManager.pinResolvedUrl(session.url, finalUrl)

                    val body = response.body
                    if (body != null) {
                        this.rangeResponse = response
                        this.rangeInputStream = body.byteStream()
                        this.readPosition = targetOffset
                        this.bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                            dataSpec.length
                        } else {
                            val cl = body.contentLength()
                            if (cl >= 0L) cl else C.LENGTH_UNSET.toLong()
                        }
                        this.useRangeRequest = true
                        rangeOpened = true
                    } else {
                        response.close()
                    }
                } else {
                    response.close()
                }
            } catch (_: Exception) {}

            if (!rangeOpened) {
                // Server doesn't support Range requests; fallback to sequential session await
                useRangeRequest = false
                cleanupRangeStream()
                try {
                    this.readPosition = session.awaitOffsetOrCluster(targetOffset)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("Interrupted while bridging sequential seek")
                }

                val totalLen = session.totalContentLength
                this.bytesRemaining = when {
                    dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
                    totalLen != -1L -> (totalLen - readPosition).coerceAtLeast(0L)
                    else -> C.LENGTH_UNSET.toLong()
                }
            }
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }

        val bytesRead = if (useRangeRequest) {
            val stream = rangeInputStream ?: return C.RESULT_END_OF_INPUT
            try {
                val read = stream.read(buffer, offset, toRead)
                if (read == -1) C.RESULT_END_OF_INPUT else read
            } catch (e: InterruptedIOException) {
                Thread.currentThread().interrupt()
                throw e
            } catch (e: IOException) {
                throw e
            }
        } else {
            try {
                session.readAt(readPosition, buffer, offset, toRead)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Read interrupted")
            }
        }

        if (bytesRead == -1 || bytesRead == C.RESULT_END_OF_INPUT) return C.RESULT_END_OF_INPUT

        readPosition += bytesRead
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        cleanupRangeStream()
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun cleanupRangeStream() {
        try { rangeInputStream?.close() } catch (_: Throwable) {}
        try { rangeResponse?.close() } catch (_: Throwable) {}
        rangeInputStream = null
        rangeResponse = null
        useRangeRequest = false
    }

    class Factory(private val session: SequentialStreamSession) : DataSource.Factory {
        override fun createDataSource(): DataSource = SequentialStreamDataSource(session)
    }
}
