package com.euthopiar.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.manifest.DashManifest
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.upstream.ParsingLoadable
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Custom DashManifestParser that intercepts incoming MPD XML text in-memory and escapes
 * raw, unescaped ampersands in query parameters and attribute values into '&amp;'
 * before parsing with Media3's underlying DashManifestParser.
 *
 * This completely prevents XmlPullParserException: "expected entity name after &"
 * caused by video CDNs emitting malformed MPD manifests with unescaped URL query parameters.
 */
@OptIn(UnstableApi::class)
class SanitizingDashManifestParser(
    delegate: ParsingLoadable.Parser<DashManifest>? = null
) : ParsingLoadable.Parser<DashManifest> {
    private val delegate: ParsingLoadable.Parser<DashManifest> by lazy { delegate ?: DashManifestParser() }

    companion object {
        private val UNESCAPED_AMP_REGEX = Regex("""&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)""")
    }

    override fun parse(uri: Uri, inputStream: InputStream): DashManifest {
        val rawXml = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val sanitizedXml = sanitizeXml(rawXml)
        val sanitizedStream = ByteArrayInputStream(sanitizedXml.toByteArray(StandardCharsets.UTF_8))
        return delegate.parse(uri, sanitizedStream)
    }

    fun sanitizeXml(xml: String): String {
        var sanitized = UNESCAPED_AMP_REGEX.replace(xml, "&amp;")
        // Also normalize hev1 to hvc1 so Android hardware video decoders bind reliably
        sanitized = sanitized.replace(Regex("""codecs="hev1\."""), """codecs="hvc1.""")
        return sanitized
    }
}
