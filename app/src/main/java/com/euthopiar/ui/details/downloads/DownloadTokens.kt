package com.euthopiar.ui.details.downloads

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.core.streams.StreamItem
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.TextPrimary
import java.util.Locale

/**
 * Editorial design tokens and pure domain helpers for the redesigned Download Sources Sheet.
 */
object DownloadTokens {
    // Colors
    val sheetBackground: Color = Background // #0B0B0F
    val heroSurface: Color = Color(0xFFFFFFFF)
    val onHero: Color = Color(0xFF0B0B0F)
    val textPrimary: Color = TextPrimary // #FFFFFF
    val textSecondary: Color = Color(0xFF9A9BA8)
    val textMuted: Color = Color(0xFF6B6C78)
    val accentCrimson: Color = Accent // #E50914
    val hairline: Color = Color.White.copy(alpha = 0.10f)
    val pressedSurface: Color = Color.White.copy(alpha = 0.06f)
    val heroPressed: Color = Color(0xFF1B1C28)
    val scrimColor: Color = Color.Black.copy(alpha = 0.60f)
    val dragHandleColor: Color = Color.White.copy(alpha = 0.20f)

    // Dimensions
    val hairlineHeight = 0.5.dp
    val sheetCornerRadius = 28.dp
    val dragHandleWidth = 36.dp
    val dragHandleHeight = 4.dp
    val contentPaddingHorizontal = 18.dp
    val contentPaddingTop = 22.dp
    val contentPaddingBottom = 18.dp
    const val sheetMaxHeightRatio = 0.85f

    val heroCornerRadius = 22.dp
    val heroPaddingHorizontal = 20.dp
    val heroPaddingVertical = 18.dp
    val heroGapBottom = 2.dp

    val pillHeight = 48.dp
    val pillCornerRadius = 999.dp

    val rowMinHeight = 64.dp
    val rowPaddingVertical = 15.dp

    val tabSpacing = 18.dp
    val tabPaddingBottom = 6.dp
    val tabGapAbove = 16.dp
    val tabGapBelow = 14.dp
    val tabUnderlineHeight = 1.5.dp

    // Durations & Tuning Constants
    const val DIRECT_SOURCE_SEED_THRESHOLD = 100
    const val DISMISS_DELAY_MS = 700L
    const val SHIMMER_DURATION_MS = 1200
    const val COUNT_UP_DURATION_MS = 700
    const val ENTRANCE_HEADER_FADE_MS = 250
    const val ENTRANCE_TABS_DELAY_MS = 40
    const val ENTRANCE_HERO_DELAY_MS = 80
    const val ENTRANCE_ROW_STAGGER_MS = 45
    const val ENTRANCE_ROW_MAX_STAGGER_INDEX = 7
    const val ENTRANCE_ROW_DURATION_MS = 320
    const val TAB_TEXT_COLOR_DURATION_MS = 150
    const val TAB_CHANGE_OUT_DURATION_MS = 120
    const val TAB_CHANGE_IN_DURATION_MS = 180

    /**
     * Seed health tiers:
     * - 100 or more seeds: textPrimary (#FFFFFF)
     * - 10 to 99 seeds: textSecondary (#9A9BA8)
     * - Under 10 seeds: textMuted (#6B6C78)
     * - Direct sources always use textPrimary.
     */
    fun getHealthTierColor(seeds: Int?, isDirect: Boolean): Color {
        if (isDirect) return textPrimary
        val s = seeds ?: 0
        return when {
            s >= 100 -> textPrimary
            s >= 10 -> textSecondary
            else -> textMuted
        }
    }

    /**
     * Best-match ranking rule:
     * - Among torrent sources, pick the highest seed count.
     * - If there are no torrents, use the first direct source.
     * - Sort remaining rows by seeds descending, and slot direct sources after any
     *   torrent with 100 or more seeds and before the rest.
     */
    fun rankAndPartitionStreams(
        streams: List<StreamItem>,
        directSeedThreshold: Int = DIRECT_SOURCE_SEED_THRESHOLD
    ): RankedStreams {
        if (streams.isEmpty()) {
            return RankedStreams(hero = null, remaining = emptyList())
        }

        val torrents = streams.filter { it.isTorrentStream }
        val directs = streams.filter { !it.isTorrentStream }

        val hero: StreamItem
        val remainingPool: List<StreamItem>

        if (torrents.isNotEmpty()) {
            val heroCandidate = torrents.maxByOrNull { it.seeders ?: 0 } ?: torrents.first()
            val heroIndex = torrents.indexOfFirst { it === heroCandidate }.takeIf { it >= 0 } ?: 0
            hero = torrents[heroIndex]
            val remainingTorrents = torrents.filterIndexed { index, _ -> index != heroIndex }
            val (highSeedTorrents, lowerSeedTorrents) = remainingTorrents.partition {
                (it.seeders ?: 0) >= directSeedThreshold
            }
            val sortedHigh = highSeedTorrents.sortedByDescending { it.seeders ?: 0 }
            val sortedLow = lowerSeedTorrents.sortedByDescending { it.seeders ?: 0 }
            remainingPool = sortedHigh + directs + sortedLow
        } else {
            hero = directs.first()
            remainingPool = directs.drop(1)
        }

        return RankedStreams(hero = hero, remaining = remainingPool)
    }

    /**
     * Extracts resolution from quality or stream title/name (e.g., "4K 2160p", "1080p", "720p", "480p").
     */
    fun extractResolution(quality: String?, fallbackName: String? = null): String? {
        val target = quality?.takeIf { it.isNotBlank() } ?: fallbackName ?: return null
        val upper = target.uppercase(Locale.US)
        return when {
            upper.contains("4K") || upper.contains("2160") || upper.contains("UHD") -> "4K 2160p"
            upper.contains("1440P") || upper.contains("1440") || upper.contains("2K") -> "1440p"
            upper.contains("1080P") || upper.contains("1080") || upper.contains("FHD") -> "1080p"
            upper.contains("720P") || upper.contains("720") || Regex("""\bHD\b""").containsMatchIn(upper) -> "720p"
            upper.contains("480P") || upper.contains("480") || Regex("""\bSD\b""").containsMatchIn(upper) -> "480p"
            upper.contains("360P") || upper.contains("360") -> "360p"
            quality != null && Regex("""\b\d{3,4}[pP]\b""").containsMatchIn(quality) -> quality.trim()
            else -> null
        }
    }

    /**
     * Parses duration string (e.g., "2h 15m", "45m", "120 min") into total minutes.
     */
    fun parseRuntimeMinutes(duration: String?): Int {
        if (duration == null) return 120
        val cleaned = duration.lowercase(Locale.US)
        val hourMatch = Regex("""(\d+)\s*h""").find(cleaned)
        val minMatch = Regex("""(\d+)\s*m""").find(cleaned)
        val hours = hourMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = hours * 60 + mins
        return if (total > 0) total else 120
    }

    /**
     * Estimates stream size based on media runtime and stream resolution (preserves old UI logic).
     */
    fun calculateEstimatedStreamSize(runtimeMin: Int, quality: String?): String {
        val q = quality ?: ""
        val gbPerHour = when {
            q.contains("4K", ignoreCase = true) || q.contains("2160") || q.contains("UHD", ignoreCase = true) -> 4.5
            q.contains("1080") || q.contains("FHD", ignoreCase = true) -> 2.2
            q.contains("720") || q.contains("HD", ignoreCase = true) -> 1.2
            q.contains("480") || q.contains("SD", ignoreCase = true) -> 0.6
            else -> 1.5
        }
        val hours = runtimeMin / 60.0
        val totalGb = hours * gbPerHour
        return if (totalGb >= 1.0) {
            String.format(Locale.US, "%.2f GB", totalGb)
        } else {
            val mb = (totalGb * 1024).toInt()
            "$mb MB"
        }
    }

    /**
     * Returns common resolution if every source shares the exact same resolution; otherwise null.
     */
    fun parseCommonResolution(streams: List<StreamItem>): String? {
        if (streams.isEmpty()) return null
        val resolutions = streams.mapNotNull { extractResolution(it.quality, it.name ?: it.title) }
        return if (resolutions.size == streams.size && resolutions.distinct().size == 1) {
            resolutions.first()
        } else {
            null
        }
    }

    /**
     * Parsed size components for typography layout and count-up animation.
     */
    fun parseStreamSize(raw: String?, fallbackEstimate: String = "2.00 GB"): ParsedStreamSize {
        val input = raw?.takeIf { it.isNotBlank() } ?: fallbackEstimate
        val cleaned = input.replace(",", "").trim().removePrefix("~").trim()
        val regex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*([a-zA-Z]+)?""")
        val match = regex.find(cleaned)

        if (match != null) {
            val numStr = match.groupValues[1]
            val numeric = numStr.toDoubleOrNull() ?: 0.0
            val unitStr = match.groupValues[2].ifEmpty { "GB" }.uppercase(Locale.US)
            val formatted = String.format(Locale.US, "%.2f", numeric)
            return ParsedStreamSize(
                numericValue = numeric,
                formattedNumeral = formatted,
                unit = unitStr
            )
        }

        return ParsedStreamSize(numericValue = 0.0, formattedNumeral = "0.00", unit = "GB")
    }

    /**
     * Accessibility contentDescription generator for a stream source.
     */
    fun getStreamAccessibilityDescription(stream: StreamItem, displaySize: String): String {
        val quality = extractResolution(stream.quality, stream.name) ?: stream.quality ?: if (stream.isTorrentStream) "Torrent" else "Direct"
        val sizeText = displaySize.replace("GB", "gigabytes", ignoreCase = true)
            .replace("MB", "megabytes", ignoreCase = true)
        val source = stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName

        return if (stream.isTorrentStream) {
            val seeds = stream.seeders ?: 0
            "$quality, $sizeText, $seeds seeds, torrent via $source. Download"
        } else {
            "$quality, $sizeText, direct via $source. Download"
        }
    }

    /**
     * Generates a stable, collision-free key for a stream item.
     * Sanitizes special characters to avoid illegal Compose key issues.
     */
    fun getStableStreamKey(stream: StreamItem, disambiguationIndex: Int = 0): String {
        val rawId = stream.playableDirectUrl
            ?: stream.effectiveMagnetOrHash
            ?: stream.url
            ?: "${stream.name.orEmpty()}_${stream.title.orEmpty()}_${stream.quality.orEmpty()}_${stream.size.orEmpty()}"
        val sanitized = rawId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_").take(120).ifBlank { "stream" }
        val addon = stream.addonId.ifBlank { "unknown" }
        return if (disambiguationIndex == 0) {
            "${addon}_$sanitized"
        } else {
            "${addon}_${sanitized}_$disambiguationIndex"
        }
    }
}

data class KeyedStreamItem(
    val key: String,
    val stream: StreamItem
)

/**
 * Maps a list of StreamItem to KeyedStreamItem, strictly enforcing unique keys
 * via an occurrence disambiguation counter to prevent Compose LazyColumn key collision crashes.
 */
fun List<StreamItem>.toKeyedStreamItems(): List<KeyedStreamItem> {
    val seenCounts = mutableMapOf<String, Int>()
    return map { stream ->
        val rawKey = DownloadTokens.getStableStreamKey(stream, 0)
        val count = seenCounts.getOrDefault(rawKey, 0)
        seenCounts[rawKey] = count + 1
        val uniqueKey = "row_${if (count == 0) rawKey else "${rawKey}_$count"}"
        KeyedStreamItem(key = uniqueKey, stream = stream)
    }
}

data class RankedStreams(
    val hero: StreamItem?,
    val remaining: List<StreamItem>
)

data class ParsedStreamSize(
    val numericValue: Double,
    val formattedNumeral: String,
    val unit: String
)

enum class DownloadTab(val title: String) {
    ALL("All"),
    P2P("P2P"),
    DIRECT("Direct")
}

enum class StreamDownloadState {
    IDLE,
    STARTING,
    ADDED
}

@Composable
fun isReduceMotion(): Boolean {
    val localReduce = LocalReduceMotion.current
    if (localReduce) return true
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Editorial press modifier with spring physics and overshoot on release.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.editorialPress(
    pressedScale: Float = 0.98f,
    pressedColor: Color = DownloadTokens.pressedSurface,
    normalColor: Color = Color.Transparent,
    enabled: Boolean = true,
    isReduceMotion: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isReduceMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "editorialPressScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && enabled && !isReduceMotion) pressedColor else normalColor,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "editorialPressBg"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .background(backgroundColor)
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
            } else {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            }
        )
}

/**
 * Download indicator handling transitions between Arrow, Progress Ring, and Animated Check Mark.
 */
@Composable
fun EditorialDownloadIndicator(
    state: StreamDownloadState,
    tintColor: Color = Color.White,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val reduceMotion = isReduceMotion()

    val arrowAlpha by animateFloatAsState(
        targetValue = if (state == StreamDownloadState.IDLE) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "arrowAlpha"
    )
    val arrowTranslationY by animateFloatAsState(
        targetValue = if (state == StreamDownloadState.IDLE) 0f else 6f,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "arrowTranslationY"
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (state == StreamDownloadState.STARTING) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(
            durationMillis = 150,
            delayMillis = if (state == StreamDownloadState.STARTING) 50 else 0
        ),
        label = "progressAlpha"
    )

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        if (arrowAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = arrowAlpha
                        translationY = arrowTranslationY.dp.toPx()
                    }
            )
        }
        if (progressAlpha > 0.01f) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer { alpha = progressAlpha },
                strokeWidth = 2.dp,
                color = Color.White
            )
        }
        if (state == StreamDownloadState.ADDED) {
            AnimatedCheckMark(
                size = iconSize,
                color = Color.White,
                reduceMotion = reduceMotion
            )
        }
    }
}

/**
 * Animated CheckMark drawn with PathMeasure path trim over 250ms.
 */
@Composable
fun AnimatedCheckMark(
    size: Dp,
    color: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.30f)
        }

        if (reduceMotion || progress.value >= 0.99f) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else if (progress.value > 0.01f) {
            val measure = PathMeasure()
            measure.setPath(path, false)
            val trimPath = Path()
            measure.getSegment(0f, measure.length * progress.value, trimPath, true)
            drawPath(
                path = trimPath,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
