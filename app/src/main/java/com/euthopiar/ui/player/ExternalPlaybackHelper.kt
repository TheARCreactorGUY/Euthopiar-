package com.euthopiar.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.euthopiar.core.model.StreamSource

data class ExternalPlayerOption(
    val id: String,
    val name: String,
    val packageName: String? = null
)

object ExternalPlaybackHelper {

    val POPULAR_PLAYERS = listOf(
        ExternalPlayerOption("vlc", "VLC for Android", "org.videolan.vlc"),
        ExternalPlayerOption("mx", "MX Player", "com.mxtech.videoplayer.ad"),
        ExternalPlayerOption("just", "Just Player", "com.brouken.player"),
        ExternalPlayerOption("mpv", "MPV-Android", "is.xyz.mpv"),
        ExternalPlayerOption("nova", "Nova Video Player", "org.courville.nova"),
        ExternalPlayerOption("system", "System App Chooser", null)
    )

    fun launchExternalPlayer(
        context: Context,
        streamSource: StreamSource,
        title: String,
        playerOption: ExternalPlayerOption = ExternalPlayerOption("system", "System App Chooser", null),
        onError: ((String) -> Unit)? = null
    ): Boolean {
        try {
            val uri = Uri.parse(streamSource.url)
            val mimeType = if (streamSource.isM3u8 || streamSource.url.contains(".m3u8")) {
                "application/x-mpegURL"
            } else if (streamSource.url.contains(".mpd")) {
                "application/dash+xml"
            } else {
                "video/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                putExtra("title", title)
                putExtra("video_title", title)

                // Inject HTTP headers for VLC / MX Player / Just Player
                if (streamSource.headers.isNotEmpty()) {
                    val headersArray = streamSource.headers.flatMap { listOf(it.key, it.value) }.toTypedArray()
                    putExtra("headers", headersArray)

                    // MX Player specific headers
                    val headersFormatted = streamSource.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                    putExtra("extra_headers", headersFormatted)

                    streamSource.headers["Referer"]?.let { putExtra("referer", it) }
                    streamSource.headers["User-Agent"]?.let { putExtra("user_agent", it) }
                }

                if (!playerOption.packageName.isNullOrBlank()) {
                    setPackage(playerOption.packageName)
                }
            }

            // Check if application is installed
            val packageManager = context.packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            if (activities.isEmpty() && !playerOption.packageName.isNullOrBlank()) {
                onError?.invoke("${playerOption.name} is not installed")
                return false
            }

            if (playerOption.packageName.isNullOrBlank()) {
                val chooser = Intent.createChooser(intent, "Play stream with...")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                context.startActivity(intent)
            }
            return true
        } catch (e: Exception) {
            android.util.Log.w("ExternalPlaybackHelper", "Failed to launch external player", e)
            onError?.invoke("Unable to launch external player")
            return false
        }
    }
}
