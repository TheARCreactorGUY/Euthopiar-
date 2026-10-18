package com.nuvio.app.features.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

internal class ResponseHeaderOverridingDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val defaultResponseHeaders: Map<String, String>,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        ResponseHeaderOverridingDataSource(
            upstream = upstreamFactory.createDataSource(),
            defaultResponseHeaders = defaultResponseHeaders,
        )
}

private class ResponseHeaderOverridingDataSource(
    private val upstream: DataSource,
    private val defaultResponseHeaders: Map<String, String>,
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long = upstream.open(dataSpec)

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        val upstreamHeaders = upstream.responseHeaders
        if (defaultResponseHeaders.isEmpty()) return upstreamHeaders

        val merged = LinkedHashMap<String, List<String>>(upstreamHeaders.size + defaultResponseHeaders.size)
        merged.putAll(upstreamHeaders)
        defaultResponseHeaders.forEach { (key, value) ->
            merged[key] = listOf(value)
        }
        return merged
    }

    override fun close() {
        upstream.close()
    }
}
