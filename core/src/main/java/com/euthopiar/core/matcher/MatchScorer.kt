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
     * Strips technical numbers and metadata tokens (audio channels, bitrates, codecs, resolutions)
     * before punctuation replacement to prevent them from becoming false sequel digits.
     */
    fun stripTechnicalTokens(input: String): String {
        var s = input
        // Strip leading release/fansub group tag in brackets e.g. [SubsPlease], [Erai-raws], [Judas]
        s = s.replace(Regex("""^\[[^\]]+\]\s*"""), "")
        // Strip CRC32 checksum in brackets e.g. [98A7B2C1]
        s = s.replace(Regex("""\[[0-9a-fA-F]{8}\]"""), " ")
        // 1. Audio channels: 5.1, 2.0, 7.1, 1.0, 2.1, 5.0, 7.0
        s = s.replace(Regex("""\b[1257]\.[01]\b""", RegexOption.IGNORE_CASE), " ")
        // 2. Audio/codec composite: ddp5.1, ddp5 1, ac3 5.1, aac2.0
        s = s.replace(Regex("""\b(?:ddp?|eac3|ac3|aac|dts)[\s._-]?[1257][\s._-]?[01]\b""", RegexOption.IGNORE_CASE), " ")
        // 3. Bitrates, speeds, file sizes, bit depths, framerates, channels: 10bit, 8bit, 10gbps, 1gbps, 2.4 gb, 60fps, 24fps, 5.1ch, 6ch
        s = s.replace(Regex("""\b\d+(?:\.\d+)?\s*(?:gbps|mbps|kbps|gb|mb|kb|gib|mib|kib|bit|fps|hz|ch)\b""", RegexOption.IGNORE_CASE), " ")
        // 4. Codecs and formats: h.264, h.265, h264, h265, x264, x265, av1, mpeg2, mpeg4, hevc, avc
        s = s.replace(Regex("""\b(?:h\.?26[45]|x26[45]|av1|mpeg[24]|hevc|avc)\b""", RegexOption.IGNORE_CASE), " ")
        // 5. Video resolutions: 2160p, 1440p, 1080p, 720p, 576p, 480p, 360p, 240p, or standalone resolution tags
        s = s.replace(Regex("""\b(?:2160|1440|1080|720|576|480|360)p?\b""", RegexOption.IGNORE_CASE), " ")
        return s
    }

    /**
     * Normalizes a title for robust comparison:
     * - Strips technical tokens (5.1, 10bit, x265, 1080p) before punctuation
     * - Lowercases and trims
     * - Maps Roman numerals to Arabic digits
     * - Removes common release/quality tags (e.g. 1080p, WEB-DL, BluRay)
     * - Strips standalone years and punctuation
     * - Maps roman numerals and number words to digits
     * - Strips the word "the"
     */
    fun normalize(title: String): String {
        var s = title.lowercase().trim()

        // Replace ampersand with 'and' for consistency between releases and canonical titles
        s = s.replace("&", " and ")

        // Strip technical specifications first to prevent decimal points turning into standalone digits
        s = stripTechnicalTokens(s)

        // Remove brackets and parentheticals containing release tags or years
        s = s.replace(Regex("""\((?:19|20)\d{2}\)"""), " ")
        s = s.replace(Regex("""\[(?:19|20)\d{2}\]"""), " ")

        // Strip quality & codec tags
        s = s.replace(Regex("""\b(?:4k|uhd|fhd|hd|bluray|blu-ray|web-dl|webrip|brrip|dvdrip|hdtv|remux|hdr|sdr|aac|atmos|truehd|dovi|dv)\b"""), " ")

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
     *
     * Only flags a sequel mismatch when a standalone sequel number (2, 3, 4, II, III, Part 2, Chapter 2)
     * appears immediately adjacent to the matched title tokens, never from trailing technical metadata or release years.
     */
    fun hasNumeralOrSubtitleMismatch(hintTitles: Set<String>, candidateTitle: String): Boolean {
        val normCandidate = normalize(candidateTitle)
        if (normCandidate.isBlank() || hintTitles.isEmpty()) return false

        val targetSequels = hintTitles.mapNotNull { ht ->
            val normHt = normalize(ht)
            Regex("""\b(?:part\s+|chapter\s+|vol\s+|volume\s+)?([2-9]|1[0-9]|ii|iii|iv|v|vi|vii|viii|ix|x)$""", RegexOption.IGNORE_CASE)
                .find(normHt)?.groupValues?.get(1)?.lowercase()
                ?.let { wordOrRomanToArab(it)?.toString() ?: it }
        }.toSet()

        for (ht in hintTitles) {
            val normHt = normalize(ht)
            val baseTarget = normHt.replace(
                Regex("""\s+(?:part\s+|chapter\s+|vol\s+|volume\s+)?([2-9]|1[0-9]|ii|iii|iv|v|vi|vii|viii|ix|x)$""", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (baseTarget.isBlank()) continue

            val candidateSequelRaw = extractAdjacentSequelToken(baseTarget, normCandidate)
            if (candidateSequelRaw != null) {
                val candidateSequel = wordOrRomanToArab(candidateSequelRaw)?.toString() ?: candidateSequelRaw
                if (targetSequels.isEmpty()) {
                    return true
                } else if (candidateSequel !in targetSequels) {
                    return true
                }
            }
        }
        return false
    }

    private fun extractAdjacentSequelToken(baseTarget: String, normCandidate: String): String? {
        val cleanCandidate = normCandidate.trim()
        val cleanTitle = baseTarget.trim()
        if (cleanCandidate == cleanTitle) return null

        val remainder = if (cleanCandidate.startsWith("$cleanTitle ")) {
            cleanCandidate.substring(cleanTitle.length).trim()
        } else {
            val titleWords = cleanTitle.split(' ').filter { it.isNotBlank() }
            val candWords = cleanCandidate.split(' ').filter { it.isNotBlank() }
            if (candWords.size > titleWords.size && candWords.take(titleWords.size) == titleWords) {
                candWords.drop(titleWords.size).joinToString(" ")
            } else null
        } ?: return null

        val sequelMatch = Regex(
            """^(?:part\s+|chapter\s+|vol\s+|volume\s+)?([2-9]|1[0-9]|ii|iii|iv|v|vi|vii|viii|ix|x)\b""",
            RegexOption.IGNORE_CASE
        ).find(remainder)
        return sequelMatch?.groupValues?.get(1)?.lowercase()
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

    data class StreamVerificationResult(
        val score: Double,
        val isAccepted: Boolean,
        val reason: String? = null
    )

    fun scoreStream(
        hints: MatchHints,
        item: com.euthopiar.core.streams.StreamItem
    ): StreamVerificationResult {
        return scoreStream(
            hints = hints,
            streamTitle = item.title ?: item.name,
            streamDescription = item.description,
            streamUrl = item.url,
            magnetUrl = item.magnetUrl,
            scraperName = item.addonName.ifBlank { item.sourceName }
        )
    }

    fun scoreStream(
        hints: MatchHints,
        streamTitle: String?,
        streamDescription: String? = null,
        streamUrl: String? = null,
        magnetUrl: String? = null,
        scraperName: String? = null
    ): StreamVerificationResult {
        // 1. Extract magnet dn parameter if present
        var magnetDn: String? = null
        val rawMagnet = magnetUrl ?: streamUrl?.takeIf { it.startsWith("magnet:", ignoreCase = true) }
        if (rawMagnet != null) {
            val dnParam = Regex("""[?&]dn=([^&]+)""", RegexOption.IGNORE_CASE).find(rawMagnet)?.groupValues?.get(1)
            if (dnParam != null) {
                magnetDn = try {
                    java.net.URLDecoder.decode(dnParam, "UTF-8")
                } catch (_: Exception) {
                    dnParam.replace('+', ' ')
                }
            }
        }

        // Identify any 4-digit numbers that are part of canonical titles (e.g. "Wonder Woman 1984", "1917", "Blade Runner 2049")
        val rawHintTitles = (hints.titles + setOfNotNull(hints.originalTitle) + hints.alternativeTitles +
                hints.seasonNames.values + hints.animeSynonyms +
                listOfNotNull(hints.romajiTitle, hints.englishTitle, hints.nativeTitle) +
                (hints.season?.let { hints.seasonSpecificTitles[it] } ?: emptyList()))
            .filter { it.isNotBlank() }
        val titleYearsOrNumbers = rawHintTitles.flatMap { t ->
            Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)""").findAll(t).mapNotNull { it.value.toIntOrNull() }
        }.toSet()

        // 1b. Separate filename / release candidate from UI metadata description
        var filenameHint: String? = magnetDn?.takeIf { it.isNotBlank() }
        if (filenameHint == null) {
            val descLines = listOfNotNull(streamTitle, streamDescription).flatMap { it.lines() }
            val mediaLine = descLines.firstOrNull { line ->
                val l = line.trim()
                val isGeneric = isGenericStreamLabel(l, scraperName, titleYearsOrNumbers)
                !isGeneric && (
                    l.endsWith(".mkv", ignoreCase = true) || l.endsWith(".mp4", ignoreCase = true) ||
                            l.endsWith(".avi", ignoreCase = true) || l.endsWith(".ts", ignoreCase = true) ||
                            Regex("""\bS\d{1,2}[\s._-]*E\d{1,3}\b""", RegexOption.IGNORE_CASE).containsMatchIn(l) ||
                            (Regex("""\b(?:2160p|1080p|720p|bluray|web-?dl|remux)\b""", RegexOption.IGNORE_CASE).containsMatchIn(l) &&
                             l.split(' ', '.', '_').filter { it.isNotBlank() }.size >= 3)
                )
            }
            filenameHint = mediaLine?.trim()
        }
        if (filenameHint == null && streamUrl != null && !streamUrl.startsWith("magnet:", ignoreCase = true)) {
            try {
                val path = java.net.URI(streamUrl).path
                val lastSeg = path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                if (lastSeg != null && (lastSeg.endsWith(".mkv", ignoreCase = true) || lastSeg.endsWith(".mp4", ignoreCase = true) ||
                        lastSeg.contains("1080p", ignoreCase = true) || lastSeg.contains("2160p", ignoreCase = true))) {
                    filenameHint = lastSeg
                }
            } catch (_: Exception) {}
        }

        val rawTitleOnly = streamTitle?.trim().orEmpty()
        val candidateText = listOfNotNull(filenameHint, streamTitle, streamDescription)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        if (candidateText.isBlank()) {
            return StreamVerificationResult(score = 0.65, isAccepted = true, reason = "Empty stream label")
        }

        // 2. Check for host/server labels against streamTitle ALONE (before description pollution)
        val isTitleGeneric = rawTitleOnly.isNotBlank() && isGenericStreamLabel(rawTitleOnly, scraperName, titleYearsOrNumbers)
        val isDnGeneric = magnetDn == null || isGenericStreamLabel(magnetDn, scraperName, titleYearsOrNumbers)
        val isGenericHostLabel = isTitleGeneric && isDnGeneric

        if (isGenericHostLabel) {
            // Check for explicit contradiction on generic host labels before accepting
            val combinedTextForSeCheck = listOfNotNull(streamTitle, streamDescription, filenameHint).joinToString(" ")
            val explicitWrongSeMatch = Regex("""\bS(\d{1,2})[\s._-]*E(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(combinedTextForSeCheck)

            if (hints.type == ContentType.MOVIE && explicitWrongSeMatch != null) {
                return StreamVerificationResult(
                    score = 0.0,
                    isAccepted = false,
                    reason = "TV season/episode markers found on movie stream"
                )
            }
            if (hints.type != ContentType.MOVIE && explicitWrongSeMatch != null) {
                val sNum = explicitWrongSeMatch.groupValues[1].toIntOrNull()
                val eNum = explicitWrongSeMatch.groupValues[2].toIntOrNull()
                val reqS = hints.season
                val reqE = hints.episode
                val reqAbs = hints.absoluteEpisode
                val seasonMismatch = reqS != null && sNum != null && sNum != reqS
                val epMismatch = reqE != null && eNum != null && eNum != reqE && (reqAbs == null || eNum != reqAbs)
                if (seasonMismatch || epMismatch) {
                    return StreamVerificationResult(
                        score = 0.0,
                        isAccepted = false,
                        reason = "Explicit SxxExx mismatch on stream: S${sNum}E${eNum} vs requested S${reqS}E${reqE}"
                    )
                }
            }

            val lowerTitle = rawTitleOnly.lowercase()
            val isPureEmbed = lowerTitle.contains("vidmoly") || lowerTitle.contains("streamtape") ||
                    lowerTitle.contains("filemoon") || lowerTitle.contains("doodstream") ||
                    lowerTitle.contains("server 1") || lowerTitle.contains("server 2") ||
                    lowerTitle == "auto" || lowerTitle == "hls" || lowerTitle == "default"
            val score = if (isPureEmbed) 0.65 else 0.78
            return StreamVerificationResult(
                score = score,
                isAccepted = true,
                reason = if (isPureEmbed) "Generic server name pass-through" else "Host/Server label trusted pass"
            )
        }

        // Use filenameHint (or streamTitle) for release parsing so description metadata never pollutes title comparison!
        val releaseCandidate = filenameHint ?: streamTitle ?: candidateText

        // 3. Multi-Episode Ranges & Season Packs Detection
        val rangeMatch = Regex("""\bS(\d{1,2})[\s._-]*E(\d{1,3})\s*[-~]\s*E?(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)
            ?: Regex("""\bE(?:pisode)?[\s._-]*(\d{1,3})\s*[-~]\s*E?(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)
            ?: Regex("""(?:\b|\[)(\d{1,3})\s*[-~]\s*(\d{1,3})(?:\b|\])""").find(releaseCandidate)

        var isRangeMatched = false
        if (rangeMatch != null && hints.type != ContentType.MOVIE) {
            val startEp: Int?
            val endEp: Int?
            val rangeSeason: Int?
            if (rangeMatch.groupValues.size >= 4) {
                rangeSeason = rangeMatch.groupValues[1].toIntOrNull()
                startEp = rangeMatch.groupValues[2].toIntOrNull()
                endEp = rangeMatch.groupValues[3].toIntOrNull()
            } else {
                rangeSeason = null
                startEp = rangeMatch.groupValues[1].toIntOrNull()
                endEp = rangeMatch.groupValues[2].toIntOrNull()
            }
            if (startEp != null && endEp != null && startEp <= endEp) {
                val reqEp = hints.episode
                val reqAbs = hints.absoluteEpisode
                val inRange = (reqEp != null && reqEp in startEp..endEp) || (reqAbs != null && reqAbs in startEp..endEp)
                val seasonOk = rangeSeason == null || hints.season == null || rangeSeason == hints.season
                if (inRange && seasonOk) {
                    isRangeMatched = true
                }
            }
        }

        val isCompletePack = releaseCandidate.contains("Complete Series", ignoreCase = true) ||
                releaseCandidate.contains("Complete Season", ignoreCase = true) ||
                releaseCandidate.contains("Season Complete", ignoreCase = true) ||
                Regex("""\b(?:complete|batch|full\s*season|all\s*episodes)\b""", RegexOption.IGNORE_CASE).containsMatchIn(releaseCandidate)

        val streamSeasonOnly = Regex("""\bS(?:eason)?\s*(\d{1,2})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)?.groupValues?.get(1)?.toIntOrNull()
        val hasExplicitEpisode = Regex("""\bE(\d{1,3})\b""", RegexOption.IGNORE_CASE).containsMatchIn(releaseCandidate) ||
                Regex("""\b(?:ep|episode)\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE).containsMatchIn(releaseCandidate)

        val isSeasonPack = hints.type != ContentType.MOVIE &&
                (isCompletePack || (streamSeasonOnly != null && !hasExplicitEpisode)) &&
                (hints.season == null || streamSeasonOnly == null || streamSeasonOnly == hints.season)

        // 4. Parse single Season & Episode markers
        val seMatch = Regex("""\bS(\d{1,2})[\s._-]*E(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)
            ?: Regex("""\bSeason\s*(\d{1,2})\s*Episode\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)
            ?: Regex("""\b(\d{1,2})x(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)

        val streamSeason = seMatch?.groupValues?.get(1)?.toIntOrNull() ?: streamSeasonOnly

        val resolutionCodecNumbers = setOf(240, 360, 480, 576, 720, 1080, 1440, 2160, 264, 265)
        val bracketNum = Regex("""\[(\d{1,3})\]""").find(releaseCandidate)?.groupValues?.get(1)?.toIntOrNull()
        val safeBracketEpisode = if (bracketNum != null && bracketNum !in resolutionCodecNumbers) bracketNum else null

        val streamEpisode = seMatch?.groupValues?.get(2)?.toIntOrNull()
            ?: Regex("""\b(?:ep|episode|e)[\s._-]*(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(releaseCandidate)?.groupValues?.get(1)?.toIntOrNull()
            ?: safeBracketEpisode
            ?: Regex("""\s-\s+(\d{1,3})\b""").find(releaseCandidate)?.groupValues?.get(1)?.toIntOrNull()

        // 5. Hard Contradiction Rejection
        // 5a. Movie requested but explicit TV markers found
        if (hints.type == ContentType.MOVIE) {
            val explicitTvOnMovie = Regex("""\bS\d{1,2}[\s._-]*E\d{1,3}\b""", RegexOption.IGNORE_CASE).containsMatchIn(releaseCandidate) ||
                    Regex("""\bSeason\s*\d+\b""", RegexOption.IGNORE_CASE).containsMatchIn(releaseCandidate) ||
                    isCompletePack
            if (explicitTvOnMovie) {
                return StreamVerificationResult(
                    score = 0.0,
                    isAccepted = false,
                    reason = "TV season/episode markers found on movie stream"
                )
            }
        }

        // 5b. TV / Anime requested: Season & Episode verification
        var episodeBonus = 0.0
        if (hints.type != ContentType.MOVIE) {
            val reqSeason = hints.season
            val reqEpisode = hints.episode
            val reqAbs = hints.absoluteEpisode

            if (isRangeMatched) {
                episodeBonus = 0.25
            } else if (isSeasonPack) {
                episodeBonus = 0.20
            } else {
                // Hard Season Mismatch (single episode release)
                if (streamSeason != null && reqSeason != null && streamSeason != reqSeason) {
                    val animeAbsMatches = hints.type == ContentType.ANIME && streamEpisode != null && reqAbs != null && streamEpisode == reqAbs
                    if (!animeAbsMatches) {
                        return StreamVerificationResult(
                            score = 0.0,
                            isAccepted = false,
                            reason = "Season mismatch: requested S$reqSeason but found S$streamSeason"
                        )
                    }
                }

                // Hard Episode Mismatch (single episode release)
                if (streamEpisode != null && reqEpisode != null) {
                    val epMatches = streamEpisode == reqEpisode
                    val absMatches = reqAbs != null && streamEpisode == reqAbs
                    if (!epMatches && !absMatches) {
                        return StreamVerificationResult(
                            score = 0.0,
                            isAccepted = false,
                            reason = "Episode mismatch: requested E$reqEpisode (abs: $reqAbs) but found E$streamEpisode"
                        )
                    }
                }

                // Exact Episode Match Bonus (+0.25)
                if (streamEpisode != null && (streamEpisode == reqEpisode || (reqAbs != null && streamEpisode == reqAbs))) {
                    episodeBonus = 0.25
                } else if (streamSeason != null && streamSeason == reqSeason && reqEpisode == null) {
                    episodeBonus = 0.15
                }
            }
        }

        // 6. Extract Candidate Release Title
        var rawReleaseTitle = releaseCandidate
        val epMatch = seMatch
            ?: Regex("""\b(?:ep|episode|e)[\s._-]*\d{1,3}\b""", RegexOption.IGNORE_CASE).find(rawReleaseTitle)
            ?: Regex("""\s-\s+\d{1,3}\b""").find(rawReleaseTitle)
            ?: Regex("""\[\d{1,3}\]""").find(rawReleaseTitle)

        if (epMatch != null && epMatch.range.first > 1) {
            rawReleaseTitle = rawReleaseTitle.substring(0, epMatch.range.first)
        } else {
            val allYearMatches = Regex("""(?<!\d)(?:19|20)\d{2}(?!p|\d)""").findAll(rawReleaseTitle).toList()
            val yearTagMatch = allYearMatches.firstOrNull { m ->
                m.value.toIntOrNull() !in titleYearsOrNumbers
            }
            if (yearTagMatch != null && yearTagMatch.range.first > 2) {
                rawReleaseTitle = rawReleaseTitle.substring(0, yearTagMatch.range.first)
            }
        }
        rawReleaseTitle = rawReleaseTitle.replace('.', ' ').replace('_', ' ').trim()
        val normCandidateTitle = normalize(rawReleaseTitle).ifBlank { normalize(releaseCandidate) }

        // Compare against canonical titles: title, originalTitle, alternativeTitles, seasonNames, anime synonyms, season-specific titles
        val allHintTitles = rawHintTitles
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(normCandidateTitle) }

        val maxTitleSim = allHintTitles.maxOfOrNull { jaroWinkler(it, normCandidateTitle) } ?: 0.0

        // 7. Year Verification
        val allCandidateYears = Regex("""(?<!\d)((?:19|20)\d{2})(?!p|\d)""").findAll(releaseCandidate)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toList()

        val streamYear = allCandidateYears.firstOrNull { it !in titleYearsOrNumbers }
            ?: (if (titleYearsOrNumbers.isEmpty() || allCandidateYears.size > 1) allCandidateYears.firstOrNull() else null)

        val candidateTargetYears = listOfNotNull(
            hints.premiereYear,
            hints.year,
            hints.season?.let { hints.seasonYears[it] }
        ).distinct()

        var yearBonus = 0.0
        if (streamYear != null && candidateTargetYears.isNotEmpty()) {
            val minYearDiff = candidateTargetYears.minOf { abs(streamYear - it) }
            // Hard contradiction on release year:
            // Year difference > 2 for movies (e.g. 1984 vs 2021) or year difference > 1 with low title similarity (< 0.75)
            val isMovie = hints.type == ContentType.MOVIE
            if ((isMovie && minYearDiff > 2) || (minYearDiff > 1 && maxTitleSim < 0.75)) {
                return StreamVerificationResult(
                    score = 0.0,
                    isAccepted = false,
                    reason = "Year mismatch: release year $streamYear is off by $minYearDiff years from $candidateTargetYears"
                )
            }
            yearBonus = when (minYearDiff) {
                0 -> 0.20
                1 -> 0.10
                else -> -0.15
            }
        }

        // 8. Numeral or Sequel Mismatch
        val hasMismatch = hasNumeralOrSubtitleMismatch(rawHintTitles.toSet(), rawReleaseTitle)
        if (hasMismatch) {
            val yearMismatches = streamYear != null && candidateTargetYears.isNotEmpty() &&
                    candidateTargetYears.none { it == streamYear }
            if (yearMismatches || maxTitleSim < 0.45) {
                return StreamVerificationResult(
                    score = 0.0,
                    isAccepted = false,
                    reason = "Numeral or sequel mismatch with different year or low title similarity ($maxTitleSim)"
                )
            }
        }
        val sequelPenalty = if (hasMismatch) -0.30 else 0.0

        // 9. Final Score Calculation with Soft-Ranking
        val finalScore = ((maxTitleSim * 0.70) + yearBonus + episodeBonus + sequelPenalty).coerceIn(0.0, 1.0)
        // Soft-ranking: accept unless title similarity is critically non-matching (< 0.35)
        val isAccepted = if (isRangeMatched || isSeasonPack) {
            maxTitleSim >= 0.50
        } else {
            maxTitleSim >= 0.35 && finalScore >= 0.35
        }
        val effectiveScore = if (isRangeMatched || isSeasonPack) {
            if (isAccepted) finalScore.coerceAtLeast(0.80) else finalScore
        } else finalScore

        return StreamVerificationResult(
            score = effectiveScore,
            isAccepted = isAccepted,
            reason = if (isAccepted) "Verified (score: $effectiveScore)" else "Title similarity too low ($maxTitleSim)"
        )
    }

    private fun isGenericStreamLabel(text: String, scraperName: String?, titleYearsOrNumbers: Set<Int> = emptySet()): Boolean {
        val lower = text.lowercase().trim()
        val genericExact = setOf(
            "server", "stream", "default", "embed", "direct", "auto", "hls", "fast server",
            "vip server", "source", "vidmoly", "streamtape", "filemoon", "doodstream", "mixdrop",
            "superstream", "upcloud", "megacloud", "rabbitstream", "adaptive stream", "torrent",
            "direct stream", "voe", "streamwish", "luluvdo", "vidsrc", "player", "hubcloud",
            "gdflix", "pixeldrain", "gofile", "fsl", "vcloud", "gdrive", "drive", "fastcloud",
            "worker", "cf", "cloudflare", "r2", "desiupload", "indiashare", "filepress",
            "drop", "mirror", "instant", "download", "10gbps", "1gbps", "backup", "main",
            "vip", "alpha", "beta", "fast", "line", "speed", "ultra", "high", "bandwidth", "cdn", "cloud"
        )
        if (lower in genericExact) return true

        val scraperTokens = setOf(
            "4khdhub", "moviesdrive", "uhdmovies", "moviesmod", "hdhub4u", "katmoviehd",
            "vegamovies", "multimovies", "streamflix", "vidlink", "vixsrc", "vidzee",
            "primesrc", "consumet", "aniwave", "hianime", "gogoanime", "animepahe",
            "torrentio", "mediafusion", "comet", "annatar", "knightcrawler", "jackettio",
            "stremthru", "nuvio"
        )
        if (lower in scraperTokens) return true

        var stripped = lower
            .replace(Regex("""\b(?:server|source|stream|link|mirror|worker|drop|direct|download|backup|main|alpha|beta)\s*\d*\b"""), " ")
            .replace(Regex("""\b(?:hubcloud|gdflix|pixeldrain|gofile|fsl|vcloud|gdrive|drive|fastcloud|cf|cloudflare|r2|desiupload|indiashare|filepress|10gbps|1gbps|line|speed|ultra|high|bandwidth|cdn|cloud)\b"""), " ")
            .replace(Regex("""\b(?:4k|2160p?|1440p?|1080p?|720p?|480p?|360p?|uhd|fhd|hd|sd|auto|hls|m3u8|embed|direct|fast|vip|normal|default|bluray|web-?dl|webrip|brrip|dvdrip|remux|hevc|x265|x264|avc|10bit|hdr|dovi|sdr|aac|atmos|truehd|ddp|multi|dual\s*audio|english|hindi|tamil|telugu|sub|dub|uncensored|seeds?|peers?)\b"""), " ")
            .replace(Regex("""💾|👤|👥|size:?|\d+(?:\.\d+)?\s*(?:gbps|mbps|kbps|gb|mb|gib|mib|kb|kib|bit|fps|hz|ch)"""), " ")

        // Strip scraper names
        scraperTokens.forEach { token ->
            stripped = stripped.replace(Regex("""\b$token\b"""), " ")
        }

        // Strip standalone release years that are not part of the title
        stripped = stripped.replace(Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)""")) { m ->
            if (m.value.toIntOrNull() in titleYearsOrNumbers) m.value else " "
        }

        stripped = stripped
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (stripped.isEmpty() || stripped in genericExact || stripped in scraperTokens) return true

        val words = stripped.split(' ').filter { it.isNotBlank() }
        if (words.isNotEmpty() && words.all { it in genericExact || it in scraperTokens }) return true

        if (scraperName != null) {
            val cleanScraper = scraperName.lowercase().trim()
            if (stripped.equals(cleanScraper, ignoreCase = true) || stripped.replace(cleanScraper, "").trim() in genericExact) {
                return true
            }
        }
        return stripped.length < 3
    }

    fun filterAndRankStreams(
        hints: MatchHints,
        streams: List<com.euthopiar.core.streams.StreamItem>
    ): List<com.euthopiar.core.streams.StreamItem> {
        return streams.mapNotNull { stream ->
            val result = scoreStream(hints, stream)
            if (result.isAccepted) {
                stream.copy(matchScore = result.score)
            } else {
                null
            }
        }.sortedByDescending { it.matchScore }
    }
}
