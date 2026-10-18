package com.euthopiar.core.matcher

import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.core.model.ContentType
import com.euthopiar.core.model.MatchHints
import com.euthopiar.core.model.MediaCard
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Universal candidate matcher and scoring engine for TMDB bridging.
 * Scores provider candidates against canonical TMDB metadata hints without false positives.
 */
object MatchScorer {

    const val THRESHOLD_AUTO_ACCEPT = 0.92
    const val THRESHOLD_VERIFY = 0.75

    /**
     * Normalizes a title for robust comparison:
     * - Lowercases and trims
     * - Maps Roman numerals to Arabic digits
     * - Removes common release/quality tags (e.g. 1080p, WEB-DL, BluRay)
     * - Strips standalone years and punctuation
     * - Maps roman numerals and number words to digits
     * - Strips the word "the"
     */
    fun normalize(title: String): String {
        var s = title.lowercase().trim()

        // Remove brackets and parentheticals containing release tags or years
        s = s.replace(Regex("""\((?:19|20)\d{2}\)"""), " ")
        s = s.replace(Regex("""\[(?:19|20)\d{2}\]"""), " ")

        // Strip quality & codec tags
        s = s.replace(Regex("""\b(?:4k|2160p|1080p|720p|480p|uhd|fhd|hd|bluray|blu-ray|web-dl|webrip|brrip|dvdrip|hdtv|remux|hevc|x265|x264|avc|10bit|hdr|sdr|aac|atmos)\b"""), " ")

        // Strip roman numerals and number words in part/chapter/standalone contexts
        s = s.replace(Regex("""\b(part|chapter|vol|volume)\s+(?:i{1,3}|iv|v|vi{1,3}|ix|x|one|two|three|four|five|six|seven|eight|nine|ten)\b""")) { mr ->
            val suffix = mr.value.substringAfterLast(" ")
            val num = wordOrRomanToArab(suffix)
            "${mr.value.substringBeforeLast(" ")} ${num ?: suffix}"
        }
        s = s.replace(Regex("""\b(?:i{1,3}|iv|v|vi{1,3}|ix|x|one|two|three|four|five|six|seven|eight|nine|ten)\b""")) { mr ->
            val arab = wordOrRomanToArab(mr.value)
            arab?.toString() ?: mr.value
        }

        // Remove punctuation and special symbols
        s = s.replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")

        // Remove the word "the"
        s = s.replace(Regex("""\bthe\b"""), " ")

        // Collapse whitespace
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    private fun wordOrRomanToArab(token: String): Int? {
        return when (token.lowercase()) {
            "i", "one" -> 1
            "ii", "two" -> 2
            "iii", "three" -> 3
            "iv", "four" -> 4
            "v", "five" -> 5
            "vi", "six" -> 6
            "vii", "seven" -> 7
            "viii", "eight" -> 8
            "ix", "nine" -> 9
            "x", "ten" -> 10
            else -> null
        }
    }

    /**
     * Detects numeral or sequel subtitle mismatches between the hint titles and candidate title.
     * Prevents "Dune (1984)" or "Dune (2021)" from matching "Dune: Part Two",
     * while allowing valid matches across alternative titles.
     */
    fun hasNumeralOrSubtitleMismatch(hintTitles: Set<String>, candidateTitle: String): Boolean {
        val normCandidate = normalize(candidateTitle)
        val candidateDigits = extractTrailingOrStandaloneDigits(normCandidate)

        val targetDigitsList = hintTitles.map { extractTrailingOrStandaloneDigits(normalize(it)) }
        val hintsWithDigits = targetDigitsList.filter { it.isNotEmpty() }

        return if (hintsWithDigits.isNotEmpty()) {
            // Target has sequel/part numerals (e.g. [2]).
            // Candidate must match one of the declared target numeral sets.
            candidateDigits.isEmpty() || candidateDigits !in hintsWithDigits
        } else {
            // Target has no sequel/part numerals (e.g. "Dune", "Avatar", "Spider-Man").
            // Candidate must NOT have sequel/part numerals (e.g. "Dune 2", "Avatar 2").
            candidateDigits.isNotEmpty()
        }
    }

    private fun extractTrailingOrStandaloneDigits(s: String): List<Int> {
        return Regex("""\b\d+\b""").findAll(s).mapNotNull { it.value.toIntOrNull() }.toList()
    }

    /**
     * Standard Jaro-Winkler string distance algorithm (returns value between 0.0 and 1.0).
     */
    fun jaroWinkler(s1: CharSequence, s2: CharSequence): Double {
        val jaro = jaroDistance(s1, s2)
        if (jaro < 0.7) return jaro

        var prefix = 0
        val maxPrefix = min(4, min(s1.length, s2.length))
        for (i in 0 until maxPrefix) {
            if (s1[i] == s2[i]) prefix++ else break
        }
        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private fun jaroDistance(s1: CharSequence, s2: CharSequence): Double {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 && len2 == 0) return 1.0
        if (len1 == 0 || len2 == 0) return 0.0

        val matchDistance = max(len1, len2) / 2 - 1
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)

        var matches = 0
        for (i in 0 until len1) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, len2)
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        return (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0
    }

    /**
     * Computes candidate match score between hints and candidate metadata.
     * Weights:
     * - Title similarity: 70%
     * - Year similarity: +0.25 (exact), +0.10 (+/- 1 year), -0.50 (mismatch > 1)
     * - Media type: +0.05 (match), -1.0 (mismatch)
     * - Sequel penalty: -0.30 (if numeral or sequel mismatch)
     */
    fun score(
        hints: MatchHints,
        candidateTitle: String,
        candidateYear: Int?,
        candidateType: ContentType?
    ): Double {
        val normCandidate = normalize(candidateTitle)
        val allHintTitles = (hints.titles + setOfNotNull(hints.titles.firstOrNull())).ifEmpty { setOf(candidateTitle) }
        val titleSim = allHintTitles.maxOfOrNull { jaroWinkler(normalize(it), normCandidate) } ?: 0.0

        val hintYear = hints.year
        val yearScore = when {
            hintYear == null || candidateYear == null -> 0.0
            hintYear == candidateYear -> 0.25
            abs(hintYear - candidateYear) == 1 -> 0.10
            else -> -0.50
        }

        val typeScore = when {
            candidateType == null -> 0.0
            candidateType == hints.type -> 0.05
            else -> -1.0
        }

        val sequelPenalty = if (hasNumeralOrSubtitleMismatch(allHintTitles, candidateTitle)) -0.30 else 0.0

        return (titleSim * 0.70) + yearScore + typeScore + sequelPenalty
    }

    fun score(hints: MatchHints, item: MediaItem): Double {
        val ct = when (item.type) {
            MediaType.MOVIE -> ContentType.MOVIE
            MediaType.TV_SERIES -> ContentType.TV_SERIES
            MediaType.ANIME -> ContentType.ANIME
        }
        return score(hints, item.title, item.year, ct)
    }

    fun score(hints: MatchHints, card: MediaCard): Double {
        return score(hints, card.title, card.releaseYear, card.type)
    }

    enum class Decision {
        AUTO_ACCEPT,
        VERIFY_OR_CHOOSE,
        REJECT
    }

    fun evaluate(hints: MatchHints, item: MediaItem): Decision {
        val sc = score(hints, item)
        val yearMatches = hints.year != null && item.year != null && hints.year == item.year
        return when {
            sc >= THRESHOLD_AUTO_ACCEPT && yearMatches -> Decision.AUTO_ACCEPT
            sc >= THRESHOLD_VERIFY -> Decision.VERIFY_OR_CHOOSE
            else -> Decision.REJECT
        }
    }

    fun evaluate(hints: MatchHints, card: MediaCard): Decision {
        val sc = score(hints, card)
        val yearMatches = hints.year != null && card.releaseYear != null && hints.year == card.releaseYear
        return when {
            sc >= THRESHOLD_AUTO_ACCEPT && yearMatches -> Decision.AUTO_ACCEPT
            sc >= THRESHOLD_VERIFY -> Decision.VERIFY_OR_CHOOSE
            else -> Decision.REJECT
        }
    }
}
