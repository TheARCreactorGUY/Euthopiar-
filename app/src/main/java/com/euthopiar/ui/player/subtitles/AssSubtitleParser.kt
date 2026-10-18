package com.euthopiar.ui.player.subtitles

import androidx.compose.ui.Alignment
import com.euthopiar.ui.player.SubtitleParser

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val alignment: Alignment = Alignment.BottomCenter
)

object AssSubtitleParser {

    fun isAss(content: String): Boolean =
        content.contains("[Script Info]", ignoreCase = true) || content.contains("[Events]", ignoreCase = true)

    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()
        return try {
            val cues = mutableListOf<SubtitleCue>()
            var formatKeys = listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")
            var startIndex = 1
            var endIndex = 2
            var textIndex = 9

            content.lineSequence().forEach { rawLine ->
                try {
                    val line = rawLine.trim()
                    if (line.startsWith("Format:", ignoreCase = true)) {
                        val parsedKeys = line.substringAfter(":").split(",").map { it.trim().lowercase() }
                        if (parsedKeys.isNotEmpty()) {
                            formatKeys = parsedKeys
                            startIndex = formatKeys.indexOf("start").takeIf { it != -1 } ?: 1
                            endIndex = formatKeys.indexOf("end").takeIf { it != -1 } ?: 2
                            textIndex = formatKeys.indexOf("text").takeIf { it != -1 } ?: (formatKeys.size - 1)
                        }
                        return@forEach
                    }

                    if (line.startsWith("Dialogue:", ignoreCase = true)) {
                        val parts = line.substringAfter(":").split(",", limit = formatKeys.size)
                        if (parts.size > textIndex && parts.size > endIndex && parts.size > startIndex) {
                            val startMs = parseTimestamp(parts[startIndex].trim())
                            val endMs = parseTimestamp(parts[endIndex].trim())
                            val rawText = parts[textIndex]

                            // Detect ASS alignment tags (e.g. \an8 = TopCenter, \an7 = TopStart, \an9 = TopEnd)
                            val alignment = when {
                                rawText.contains("\\an8") -> Alignment.TopCenter
                                rawText.contains("\\an7") -> Alignment.TopStart
                                rawText.contains("\\an9") -> Alignment.TopEnd
                                rawText.contains("\\an4") -> Alignment.CenterStart
                                rawText.contains("\\an5") -> Alignment.Center
                                rawText.contains("\\an6") -> Alignment.CenterEnd
                                rawText.contains("\\an1") -> Alignment.BottomStart
                                rawText.contains("\\an2") -> Alignment.BottomCenter
                                rawText.contains("\\an3") -> Alignment.BottomEnd
                                rawText.contains("\\a5") || rawText.contains("\\a6") || rawText.contains("\\a7") -> Alignment.TopCenter
                                rawText.contains("\\a9") || rawText.contains("\\a10") || rawText.contains("\\a11") -> Alignment.Center
                                else -> Alignment.BottomCenter
                            }

                            // Strip style overrides like {\pos(x,y)}, {\c&H...&}, but convert \N to newlines
                            val stripped = rawText
                                .replace(Regex("""\{[^}]*\}"""), "")
                                .replace("\\N", "\n")
                                .replace("\\n", "\n")
                            val cleanText = SubtitleParser.sanitizeSubtitleText(stripped)

                            if (cleanText.isNotBlank() && endMs > startMs) {
                                cues.add(SubtitleCue(startMs, endMs, cleanText, alignment))
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Ignore single corrupt line and continue
                }
            }
            cues.sortedBy { it.startMs }
        } catch (t: Throwable) {
            android.util.Log.e("AssSubtitleParser", "Safe ASS parsing error: ${t.message}", t)
            emptyList()
        }
    }

    private fun parseTimestamp(time: String): Long {
        return try {
            val parts = time.trim().split(":")
            if (parts.size >= 3) {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val secondsWithMillis = parts[2].split(".")
                val seconds = secondsWithMillis[0].toLongOrNull() ?: 0L
                val centis = secondsWithMillis.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
                (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + centis
            } else if (parts.size == 2) {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val secondsWithMillis = parts[1].split(".")
                val seconds = secondsWithMillis[0].toLongOrNull() ?: 0L
                val centis = secondsWithMillis.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
                (minutes * 60000L) + (seconds * 1000L) + centis
            } else {
                0L
            }
        } catch (_: Throwable) {
            0L
        }
    }
}
