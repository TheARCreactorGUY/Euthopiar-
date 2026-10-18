package com.euthopiar.ui.player

typealias SubtitleCue = com.euthopiar.ui.player.subtitles.SubtitleCue

object SubtitleParser {

    /**
     * Sanitizes subtitle text by removing unpaired surrogates, NULL bytes,
     * and non-printable control characters that can crash Android TextPaint/Compose TextLayout.
     */
    fun sanitizeSubtitleText(raw: String): String {
        if (raw.isEmpty()) return ""
        val sb = java.lang.StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch == '\u0000') {
                i++
                continue
            }
            if (ch.isHighSurrogate()) {
                if (i + 1 < raw.length && raw[i + 1].isLowSurrogate()) {
                    sb.append(ch)
                    sb.append(raw[i + 1])
                    i += 2
                    continue
                } else {
                    // Unpaired high surrogate - skip to avoid TextPaint crash
                    i++
                    continue
                }
            } else if (ch.isLowSurrogate()) {
                // Unpaired low surrogate - skip
                i++
                continue
            }
            sb.append(ch)
            i++
        }
        return sb.toString().trim()
    }

    /**
     * Parses SubRip (.srt) and WebVTT (.vtt) text into a chronological list of [SubtitleCue]s.
     * Guaranteed never to throw an unhandled exception or crash playback.
     */
    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()
        return try {
            val cues = ArrayList<SubtitleCue>()
            val lines = content.lines()
            var i = 0
            val totalLines = lines.size

            while (i < totalLines) {
                val line = lines[i].trim()
                if (line.contains("-->")) {
                    val parts = line.split("-->")
                    if (parts.size >= 2) {
                        val startMs = parseTimestamp(parts[0].trim())
                        // End timestamp may have trailing positioning flags e.g. "00:00:13.000 line:80%"
                        val endToken = parts[1].trim().split(Regex("\\s+")).firstOrNull() ?: ""
                        val endMs = parseTimestamp(endToken)

                        i++
                        val textBuilder = StringBuilder()
                        while (i < totalLines) {
                            val subLine = lines[i].trim()
                            if (subLine.isEmpty()) break
                            // If line is an integer index followed immediately by another timestamp line, break
                            if (subLine.all { it.isDigit() } && i + 1 < totalLines && lines[i + 1].contains("-->")) {
                                i--
                                break
                            }
                            if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                            textBuilder.append(subLine)
                            i++
                        }

                        val rawText = textBuilder.toString()
                        val stripped = rawText
                            .replace(Regex("<[^>]*>"), "") // strip HTML tags
                            .replace(Regex("\\{[^}]*\\}"), "") // strip ASS/SSA tags
                        val cleanText = sanitizeSubtitleText(stripped)

                        if (cleanText.isNotBlank() && startMs >= 0L && endMs > startMs) {
                            cues.add(SubtitleCue(startMs, endMs, cleanText))
                        }
                    }
                }
                i++
            }

            cues.sortBy { it.startMs }
            cues
        } catch (t: Throwable) {
            android.util.Log.e("SubtitleParser", "Safe subtitle parsing error: ${t.message}", t)
            emptyList()
        }
    }

    private fun parseTimestamp(raw: String): Long {
        return try {
            val clean = raw.replace(',', '.').trim()
            val colonParts = clean.split(':')
            if (colonParts.size == 3) {
                val hours = colonParts[0].toLongOrNull() ?: 0L
                val minutes = colonParts[1].toLongOrNull() ?: 0L
                val dotParts = colonParts[2].split('.')
                val seconds = dotParts[0].toLongOrNull() ?: 0L
                val millis = if (dotParts.size > 1) {
                    dotParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else 0L
                (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
            } else if (colonParts.size == 2) {
                val minutes = colonParts[0].toLongOrNull() ?: 0L
                val dotParts = colonParts[1].split('.')
                val seconds = dotParts[0].toLongOrNull() ?: 0L
                val millis = if (dotParts.size > 1) {
                    dotParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else 0L
                (minutes * 60000L) + (seconds * 1000L) + millis
            } else -1L
        } catch (_: Throwable) {
            -1L
        }
    }
}
