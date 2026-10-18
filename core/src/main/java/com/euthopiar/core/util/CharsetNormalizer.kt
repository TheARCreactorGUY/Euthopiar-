package com.euthopiar.core.util

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object CharsetNormalizer {

    private val adPatterns = listOf(
        Regex("""(?i)\bopensubtitles(?:\.org|\.com)?\b"""),
        Regex("""(?i)\bsupport us and become (?:a )?vip\b"""),
        Regex("""(?i)\badvertise your product or brand\b"""),
        Regex("""(?i)\bcontact www\.opensubtitles\b"""),
        Regex("""(?i)\bplease rate this subtitle\b"""),
        Regex("""(?i)\bsubtitles? by \b"""),
        Regex("""(?i)\bsynchronized by \b"""),
        Regex("""(?i)\btranscoded by \b"""),
        Regex("""(?i)\bdownloaded from (?:www\.)?\w+\.\w+\b""")
    )

    /**
     * Inspects raw bytes for Byte Order Marks (BOM), probes UTF-8 validity,
     * applies language priors (especially Arabic CP1256, Cyrillic CP1251, Hebrew CP1255),
     * and transcodes cleanly to UTF-8 while stripping OpenSubtitles ads.
     */
    fun decodeBytes(bytes: ByteArray, languageHint: String? = null): String {
        if (bytes.isEmpty()) return ""

        // 1. UTF-8 BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            val text = String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
            return stripSubtitleAds(text)
        }
        // 2. UTF-16 Little Endian BOM
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            val text = String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
            return stripSubtitleAds(text)
        }
        // 3. UTF-16 Big Endian BOM
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            val text = String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
            return stripSubtitleAds(text)
        }

        // 4. Strict UTF-8 Probe
        try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return stripSubtitleAds(text)
        } catch (_: Exception) {}

        // 5. Try ICU CharsetDetector via reflection if present on runtime classpath
        try {
            val detectorClass = try {
                Class.forName("com.ibm.icu.text.CharsetDetector")
            } catch (_: Throwable) {
                try { Class.forName("android.icu.text.CharsetDetector") } catch (_: Throwable) { null }
            }
            if (detectorClass != null) {
                val detector = detectorClass.getDeclaredConstructor().newInstance()
                detectorClass.getMethod("setText", ByteArray::class.java).invoke(detector, bytes)
                val isArabic = languageHint != null && (
                    languageHint.startsWith("ar", ignoreCase = true) ||
                    languageHint.contains("arabic", ignoreCase = true) ||
                    languageHint.equals("cp1256", ignoreCase = true)
                )
                if (isArabic) {
                    try {
                        detectorClass.getMethod("setDeclaredEncoding", String::class.java).invoke(detector, "windows-1256")
                    } catch (_: Throwable) {}
                }
                val match = detectorClass.getMethod("detect").invoke(detector)
                if (match != null) {
                    val name = match.javaClass.getMethod("getName").invoke(match) as? String
                    val confidence = (match.javaClass.getMethod("getConfidence").invoke(match) as? Number)?.toInt() ?: 0
                    val encoding = if (isArabic && confidence < 70) "windows-1256" else name ?: "windows-1252"
                    val charset = Charset.forName(encoding)
                    val text = String(bytes, charset)
                    return stripSubtitleAds(text)
                }
            }
        } catch (_: Throwable) {}

        // 6. Language-prior heuristics for legacy encodings
        val charset = when {
            languageHint != null && (languageHint.startsWith("ar", ignoreCase = true) || languageHint.contains("arabic", ignoreCase = true) || languageHint.contains("1256")) -> {
                try { Charset.forName("windows-1256") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
            languageHint != null && (languageHint.startsWith("he", ignoreCase = true) || languageHint.contains("hebrew", ignoreCase = true) || languageHint.contains("1255")) -> {
                try { Charset.forName("windows-1255") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
            languageHint != null && (languageHint.startsWith("ru", ignoreCase = true) || languageHint.startsWith("uk", ignoreCase = true) || languageHint.contains("1251")) -> {
                try { Charset.forName("windows-1251") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
            languageHint != null && (languageHint.startsWith("el", ignoreCase = true) || languageHint.contains("greek", ignoreCase = true) || languageHint.contains("1253")) -> {
                try { Charset.forName("windows-1253") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
            languageHint != null && (languageHint.startsWith("tr", ignoreCase = true) || languageHint.contains("turkish", ignoreCase = true) || languageHint.contains("1254")) -> {
                try { Charset.forName("windows-1254") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
            else -> {
                try { Charset.forName("windows-1252") } catch (_: Exception) { StandardCharsets.ISO_8859_1 }
            }
        }
        val text = String(bytes, charset)
        return stripSubtitleAds(text)
    }

    /**
     * Strips promotional lines and ads inserted by OpenSubtitles or uploaders.
     */
    fun stripSubtitleAds(content: String): String {
        if (content.isBlank()) return content
        if (!content.contains("-->") && !content.contains("Dialogue:")) {
            return content
        }

        // If SRT or WebVTT format, process cues separated by blank lines
        if (content.contains("-->")) {
            val blocks = content.split(Regex("""\r?\n\s*\r?\n"""))
            val cleanedBlocks = mutableListOf<String>()
            var cueIndex = 1

            for (block in blocks) {
                val lines = block.lines().map { it.trimEnd() }
                if (lines.isEmpty()) continue

                // Check if any dialogue line in this cue matches an ad pattern
                val hasAd = lines.any { line ->
                    !line.contains("-->") && !line.all { it.isDigit() } && adPatterns.any { it.containsMatchIn(line) }
                }

                if (!hasAd) {
                    // Normalize cue numbering if the block begins with a number
                    if (lines.firstOrNull()?.trim()?.all { it.isDigit() } == true) {
                        val renumbered = listOf("$cueIndex") + lines.drop(1)
                        cleanedBlocks.add(renumbered.joinToString("\n"))
                        cueIndex++
                    } else {
                        cleanedBlocks.add(block)
                    }
                }
            }
            return cleanedBlocks.joinToString("\n\n")
        }

        // If ASS format, filter dialogue lines matching ads
        if (content.contains("Dialogue:")) {
            return content.lines().filterNot { line ->
                line.startsWith("Dialogue:", ignoreCase = true) && adPatterns.any { it.containsMatchIn(line) }
            }.joinToString("\n")
        }

        return content
    }
}
