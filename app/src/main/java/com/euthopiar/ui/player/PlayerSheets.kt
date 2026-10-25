package com.euthopiar.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Subtitles
import com.euthopiar.ui.theme.LocalReduceMotion
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.core.animateFloatAsState
import com.euthopiar.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalAmoledMode
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.euthopiar.ui.details.downloads.DownloadTokens
import java.util.Locale

/**
 * Visual indicator of server latency across health categories.
 */
@Composable
fun ServerHealthBadge(
    latencyMs: Long?,
    healthCategory: ServerHealthCategory?,
    modifier: Modifier = Modifier
) {
    val displayText = when {
        latencyMs != null && latencyMs > 0L -> "${latencyMs}ms"
        healthCategory != null -> healthCategory.label
        else -> "Good"
    }

    Box(
        modifier = modifier
            .videoOverlayGlass(shape = PlayerTokens.RadiusPill)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = displayText,
            style = PlayerTokens.TooltipLabel,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

enum class SourceQualityFilter(
    val label: String,
    val keyword: String?,
    val iconRes: Int
) {
    ALL("All Qualities", null, R.drawable.ic_quality_all),
    Q_4K("4K (2160p)", "2160", R.drawable.ic_quality_4k),
    Q_2K("2K (1440p)", "1440", R.drawable.ic_quality_2k),
    Q_FHD("FHD (1080p)", "1080", R.drawable.ic_quality_fhd),
    Q_HD("HD (720p)", "720", R.drawable.ic_quality_hd),
    Q_SD("SD (480p)", "480", R.drawable.ic_quality_sd)
}

/**
 * Frosted Glass Dropdown for filtering sources by resolution keyword.
 */
@Composable
fun SourcesQualityDropdown(
    selectedFilter: SourceQualityFilter,
    onSelectFilter: (SourceQualityFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180, easing = PlayerTokens.EaseOut),
        label = "qualityChevronAngle"
    )

    Box(modifier = modifier.wrapContentSize()) {
        // Dropdown Trigger Pill (Compact)
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(30.dp)
                .sheetGlass(shape = PlayerTokens.RadiusSmall)
                .tvFocusable(shape = PlayerTokens.RadiusSmall)
                .playerPressable(
                    role = Role.DropdownList,
                    onClickLabel = "Filter quality: ${selectedFilter.label}",
                    onClick = { isExpanded = !isExpanded }
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = selectedFilter.iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = selectedFilter.label,
                        color = Color.White,
                        style = PlayerTokens.PillText.copy(fontSize = 12.sp)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(chevronAngle)
                )
            }
        }

        // Dropdown Menu Popover
        if (isExpanded) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(density) { 34.dp.roundToPx() }),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 220.dp)
                        .sheetGlass(PlayerTokens.RadiusMedium)
                        .padding(4.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SourceQualityFilter.values().forEach { filter ->
                            val isSelected = filter == selectedFilter
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(PlayerTokens.RadiusSmall)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .tvFocusable(shape = PlayerTokens.RadiusSmall)
                                    .playerPressable(
                                        role = Role.Button,
                                        onClickLabel = filter.label
                                    ) {
                                        onSelectFilter(filter)
                                        isExpanded = false
                                    }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = filter.iconRes),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = filter.label,
                                        color = Color.White,
                                        style = PlayerTokens.RowTitle.copy(fontSize = 12.sp)
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(PlayerTokens.RadiusCircle)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = PlayerTokens.LabelDark,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class SourceSortOption(val label: String) {
    BEST_MATCH("Best Match"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)"),
    SEEDERS("Seeders"),
    LATENCY("Latency")
}

enum class StreamCategoryFilter(val label: String) {
    ALL("All"),
    DEBRID("⚡ Debrid"),
    P2P("👥 P2P"),
    Q_4K("4K"),
    Q_1080P("1080p"),
    Q_720P("720p")
}

fun cleanReleaseTitle(title: String): String {
    if (title.isBlank()) return title
    var cleaned = title
    // Strip trailing container extensions
    cleaned = cleaned.replace(Regex("""\.(mkv|mp4|avi|webm|ts)$""", RegexOption.IGNORE_CASE), "")
    // Strip tracker tags
    cleaned = cleaned.replace(Regex("""\[(?:eztv|rarbg|tgx|yts[.\w]*|torrentgalaxy|psa|qxr)\]""", RegexOption.IGNORE_CASE), "")
    // Strip 32..64 char hex infohashes
    cleaned = cleaned.replace(Regex("""\[[a-f0-9]{32,64}\]""", RegexOption.IGNORE_CASE), "")
    cleaned = cleaned.replace(Regex("""\b[a-f0-9]{40}\b""", RegexOption.IGNORE_CASE), "")
    // Strip trailing provider mode indicators like (Debrid/P2P) or (P2P)
    cleaned = cleaned.replace(Regex("""\((?:Debrid/P2P|P2P)\)""", RegexOption.IGNORE_CASE), "")
    // Preserve audio channels like 5.1, 7.1, 2.0 before replacing dots
    cleaned = cleaned
        .replace("5.1", "AUDIO5X1")
        .replace("7.1", "AUDIO7X1")
        .replace("2.0", "AUDIO2X0")
    cleaned = cleaned
        .replace('.', ' ')
        .replace('_', ' ')
        .replace("AUDIO5X1", "5.1")
        .replace("AUDIO7X1", "7.1")
        .replace("AUDIO2X0", "2.0")
    cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
    return cleaned.ifBlank { title }
}

fun parseSizeToBytes(sizeStr: String?): Long {
    if (sizeStr.isNullOrBlank()) return 0L
    val cleaned = sizeStr.replace(",", "").replace("~", "").trim().uppercase(Locale.US)
        .replace("GIB", "GB").replace("MIB", "MB").replace("TIB", "TB").replace("KIB", "KB")
    val regex = Regex("""([\d.]+)\s*(TB|GB|MB|KB|BYTES|B)?""")
    val match = regex.find(cleaned) ?: return 0L
    val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
    val unit = match.groupValues[2]
    return when (unit) {
        "TB" -> (value * 1024.0 * 1024.0 * 1024.0 * 1024.0).toLong()
        "GB" -> (value * 1024.0 * 1024.0 * 1024.0).toLong()
        "MB" -> (value * 1024.0 * 1024.0).toLong()
        "KB" -> (value * 1024.0).toLong()
        else -> value.toLong()
    }
}

fun isDebridStream(
    server: ServerSourceOption,
    hasActiveDebrid: Boolean = com.euthopiar.core.debrid.DebridManager.hasActiveDebrid()
): Boolean {
    if (!hasActiveDebrid) return false
    if (server.isCached || server.streamSource.isCached) return true
    val combined = "${server.serverName} ${server.streamSource.serverName} ${server.streamSource.url} ${server.label}".uppercase(Locale.US)
    if (combined.contains("[RD+]") || combined.contains("RD+") ||
        combined.contains("[TB+]") || combined.contains("TB+") ||
        combined.contains("[AD+]") || combined.contains("AD+") ||
        combined.contains("[PM+]") || combined.contains("PM+") ||
        combined.contains("REAL-DEBRID") || combined.contains("REALDEBRID") ||
        combined.contains("TORBOX") || combined.contains("ALLDEBRID") ||
        combined.contains("PREMIUMIZE")
    ) {
        return true
    }
    return !server.isTorrent && (combined.contains("DEBRID") || server.addonNames.any { it.contains("debrid", ignoreCase = true) })
}

fun getDebridStatusLabel(server: ServerSourceOption): String {
    val text = "${server.serverName} ${server.streamSource.serverName} ${server.streamSource.url} ${server.addonNames.joinToString(" ")}"
    val upper = text.uppercase(Locale.US)
    return when {
        upper.contains("TORBOX") || upper.contains("TB+") -> "⚡ TB+"
        upper.contains("ALLDEBRID") || upper.contains("AD+") -> "⚡ AD+"
        upper.contains("PREMIUMIZE") || upper.contains("PM+") -> "⚡ PM+"
        else -> "⚡ RD+"
    }
}

fun formatResolutionBadge(server: ServerSourceOption): String? {
    val r = server.resolutionLabel.lowercase(Locale.US)
    val q = server.streamSource.quality.lowercase(Locale.US)
    return when {
        r.contains("4k") || r.contains("2160") || q.contains("4k") || q.contains("2160") -> "4K"
        r.contains("1440") || r.contains("2k") || q.contains("1440") || q.contains("2k") -> "1440p"
        r.contains("1080") || r.contains("fhd") || q.contains("1080") || q.contains("fhd") -> "1080p"
        r.contains("720") || r.contains("hd") || q.contains("720") || q.contains("hd") -> "720p"
        r.contains("480") || r.contains("sd") || q.contains("480") || q.contains("sd") -> "480p"
        else -> server.resolutionLabel.takeIf { it.isNotBlank() && !it.equals("Auto", ignoreCase = true) }
    }
}

fun matchesCategory(
    server: ServerSourceOption,
    filter: StreamCategoryFilter,
    hasActiveDebrid: Boolean = com.euthopiar.core.debrid.DebridManager.hasActiveDebrid()
): Boolean {
    val isDebrid = isDebridStream(server, hasActiveDebrid)
    val isTorrentStream = server.isTorrent ||
            server.streamSource.infoHash != null ||
            server.streamSource.url.startsWith("magnet:", ignoreCase = true) ||
            server.streamSource.url.startsWith("torrent:", ignoreCase = true) ||
            server.streamSource.url.startsWith("torrent://", ignoreCase = true) ||
            (server.seeders ?: server.streamSource.seeders ?: 0) > 0 ||
            com.euthopiar.core.p2p.extractP2pStreamRequest(server.streamSource.url, server.streamSource.infoHash) != null

    return when (filter) {
        StreamCategoryFilter.ALL -> true
        StreamCategoryFilter.DEBRID -> isDebrid
        StreamCategoryFilter.P2P -> (!isDebrid || !hasActiveDebrid) && isTorrentStream
        StreamCategoryFilter.Q_4K -> {
            val r = server.resolutionLabel.lowercase(Locale.US)
            val s = server.serverName.lowercase(Locale.US)
            val q = server.streamSource.quality.lowercase(Locale.US)
            r.contains("4k") || s.contains("4k") || q.contains("4k") ||
            r.contains("2160") || s.contains("2160") || q.contains("2160") ||
            r.contains("uhd") || s.contains("uhd") || q.contains("uhd")
        }
        StreamCategoryFilter.Q_1080P -> {
            val r = server.resolutionLabel.lowercase(Locale.US)
            val s = server.serverName.lowercase(Locale.US)
            val q = server.streamSource.quality.lowercase(Locale.US)
            r.contains("1080") || s.contains("1080") || q.contains("1080") ||
            r.contains("fhd") || s.contains("fhd") || q.contains("fhd")
        }
        StreamCategoryFilter.Q_720P -> {
            val r = server.resolutionLabel.lowercase(Locale.US)
            val s = server.serverName.lowercase(Locale.US)
            val q = server.streamSource.quality.lowercase(Locale.US)
            (r.contains("720") || s.contains("720") || q.contains("720")) ||
            ((r.contains("hd") || s.contains("hd") || q.contains("hd")) &&
             !r.contains("fhd") && !s.contains("fhd") && !q.contains("fhd") &&
             !r.contains("uhd") && !s.contains("uhd") && !q.contains("uhd") &&
             !r.contains("1080") && !s.contains("1080") && !q.contains("1080") &&
             !r.contains("4k") && !s.contains("4k") && !q.contains("4k"))
        }
    }
}

@Composable
fun StreamBadgePill(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White.copy(alpha = 0.85f),
    backgroundColor: Color = PlayerTokens.VideoOverlayGlassBase,
    borderColor: Color = PlayerTokens.VideoOverlayGlassBorder
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(HairlineWidth, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = PlayerTokens.TooltipLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun SourceSortDropdown(
    selectedSort: SourceSortOption,
    onSelectSort: (SourceSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180, easing = PlayerTokens.EaseOut),
        label = "sortChevronAngle"
    )

    Box(modifier = modifier.wrapContentSize()) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(30.dp)
                .sheetGlass(shape = PlayerTokens.RadiusSmall)
                .tvFocusable(shape = PlayerTokens.RadiusSmall)
                .playerPressable(
                    role = Role.DropdownList,
                    onClickLabel = "Sort sources: ${selectedSort.label}",
                    onClick = { isExpanded = !isExpanded }
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "Sort: ${selectedSort.label}",
                    color = Color.White,
                    style = PlayerTokens.PillText.copy(fontSize = 12.sp)
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(chevronAngle)
                )
            }
        }

        if (isExpanded) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(density) { 34.dp.roundToPx() }),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 220.dp)
                        .sheetGlass(PlayerTokens.RadiusMedium)
                        .padding(4.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SourceSortOption.values().forEach { sortOpt ->
                            val isSelected = sortOpt == selectedSort
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(PlayerTokens.RadiusSmall)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .tvFocusable(shape = PlayerTokens.RadiusSmall)
                                    .playerPressable(
                                        role = Role.Button,
                                        onClickLabel = sortOpt.label
                                    ) {
                                        onSelectSort(sortOpt)
                                        isExpanded = false
                                    }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sortOpt.label,
                                    color = Color.White,
                                    style = PlayerTokens.RowTitle.copy(fontSize = 12.sp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(PlayerTokens.RadiusCircle)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = PlayerTokens.LabelDark,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hero Card for the highest-ranked stream recommendation.
 */
@Composable
fun StreamHeroCard(
    server: ServerSourceOption,
    isSelected: Boolean,
    badgeLabel: String = "RECOMMENDED",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isSelected) Color.White.copy(alpha = 0.10f) else PlayerTokens.SheetGlassFill
    val containerBorder = if (isSelected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.16f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PlayerTokens.RadiusMedium)
            .background(containerBg)
            .border(HairlineWidth, containerBorder, PlayerTokens.RadiusMedium)
            .tvFocusable(shape = PlayerTokens.RadiusMedium, onClick = onClick)
            .playerPressable(
                role = Role.Button,
                onClickLabel = "Select recommended source: ${server.serverName}",
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val badgeBrush = if (isSelected) {
                    SolidColor(Color.White)
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF00E5FF).copy(alpha = 0.25f)))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBrush)
                        .border(
                            HairlineWidth,
                            if (isSelected) Color.White
                            else Color(0xFF00E5FF).copy(alpha = 0.40f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = PlayerTokens.TooltipLabel.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isSelected) PlayerTokens.LabelDark else Color(0xFF00E5FF)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(HairlineWidth, Color.White.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            style = PlayerTokens.TooltipLabel.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeCheckCircle)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = PlayerTokens.LabelDark,
                        modifier = Modifier.size(PlayerTokens.IconCheck)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = cleanReleaseTitle(server.serverName),
            style = PlayerTokens.RowTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            formatResolutionBadge(server)?.let { res ->
                StreamBadgePill(
                    text = res,
                    textColor = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.16f),
                    borderColor = Color.White.copy(alpha = 0.30f)
                )
            }
            if (!server.hdrType.isNullOrBlank()) {
                StreamBadgePill(
                    text = server.hdrType,
                    textColor = Color(0xFFFFD54F),
                    borderColor = Color(0xFFFFD54F).copy(alpha = 0.35f)
                )
            }
            if (!server.codec.isNullOrBlank()) {
                StreamBadgePill(text = server.codec)
            }
            if (!server.audioBadge.isNullOrBlank()) {
                StreamBadgePill(text = server.audioBadge)
            }
            if (!server.size.isNullOrBlank()) {
                StreamBadgePill(text = server.size)
            }
            if (isDebridStream(server)) {
                StreamBadgePill(
                    text = getDebridStatusLabel(server),
                    textColor = Color(0xFF00E5FF),
                    backgroundColor = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    borderColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
                )
            } else if (server.isTorrent || server.streamSource.infoHash != null || server.streamSource.url.startsWith("magnet:", ignoreCase = true) || server.streamSource.url.startsWith("torrent:", ignoreCase = true) || server.streamSource.url.startsWith("torrent://", ignoreCase = true) || (server.seeders ?: 0) > 0 || (server.streamSource.seeders ?: 0) > 0 || com.euthopiar.core.p2p.extractP2pStreamRequest(server.streamSource.url, server.streamSource.infoHash) != null) {
                val seeds = server.seeders ?: server.streamSource.seeders ?: 0
                val seedColor = DownloadTokens.getHealthTierColor(seeds, isDirect = false)
                StreamBadgePill(
                    text = "👤 $seeds",
                    textColor = seedColor,
                    borderColor = seedColor.copy(alpha = 0.30f)
                )
            }
            server.addonNames.forEach { addon ->
                StreamBadgePill(
                    text = addon,
                    textColor = Color(0xFFA78BFA),
                    backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.10f),
                    borderColor = Color(0xFF8B5CF6).copy(alpha = 0.30f)
                )
            }
            server.latencyMs?.let { latency ->
                if (latency > 0) {
                    val dot = when {
                        latency < 100L -> "🟢"
                        latency < 300L -> "🟡"
                        else -> "🔴"
                    }
                    StreamBadgePill(
                        text = "$dot ${latency}ms",
                        textColor = Color.White.copy(alpha = 0.90f),
                        backgroundColor = PlayerTokens.VideoOverlayGlassBase,
                        borderColor = PlayerTokens.VideoOverlayGlassBorder
                    )
                }
            }
        }
    }
}

/**
 * Single Source Item Row.
 * Unboxed, discrete badges, solid white check circle on active, TV focusable.
 */
@Composable
fun SourceItemRow(
    server: ServerSourceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = PlayerTokens.RadiusSmall
    val rowBg = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent
    val rowBorder = if (isSelected) Color.White.copy(alpha = 0.30f) else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(rowShape)
            .background(rowBg)
            .border(if (isSelected) HairlineWidth else 0.dp, rowBorder, rowShape)
            .tvFocusable(shape = rowShape, onClick = onClick)
            .playerPressable(
                role = Role.Button,
                onClickLabel = server.serverName,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = cleanReleaseTitle(server.serverName),
                        style = PlayerTokens.RowTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NOW PLAYING",
                                style = PlayerTokens.TooltipLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    formatResolutionBadge(server)?.let { res ->
                        StreamBadgePill(
                            text = res,
                            textColor = Color.White,
                            backgroundColor = Color.White.copy(alpha = 0.12f),
                            borderColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                    if (!server.hdrType.isNullOrBlank()) {
                        StreamBadgePill(
                            text = server.hdrType,
                            textColor = Color(0xFFFFD54F),
                            borderColor = Color(0xFFFFD54F).copy(alpha = 0.35f)
                        )
                    }
                    if (!server.codec.isNullOrBlank()) {
                        StreamBadgePill(text = server.codec)
                    }
                    if (!server.audioBadge.isNullOrBlank()) {
                        StreamBadgePill(text = server.audioBadge)
                    }
                    if (!server.size.isNullOrBlank()) {
                        StreamBadgePill(text = server.size)
                    }
                    if (isDebridStream(server)) {
                        StreamBadgePill(
                            text = getDebridStatusLabel(server),
                            textColor = Color(0xFF00E5FF),
                            backgroundColor = Color(0xFF00E5FF).copy(alpha = 0.12f),
                            borderColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
                        )
                    } else if (server.isTorrent || server.streamSource.infoHash != null || server.streamSource.url.startsWith("magnet:", ignoreCase = true) || server.streamSource.url.startsWith("torrent:", ignoreCase = true) || server.streamSource.url.startsWith("torrent://", ignoreCase = true) || (server.seeders ?: 0) > 0 || (server.streamSource.seeders ?: 0) > 0 || com.euthopiar.core.p2p.extractP2pStreamRequest(server.streamSource.url, server.streamSource.infoHash) != null) {
                        val seeds = server.seeders ?: server.streamSource.seeders ?: 0
                        val seedColor = DownloadTokens.getHealthTierColor(seeds, isDirect = false)
                        StreamBadgePill(
                            text = "👤 $seeds",
                            textColor = seedColor,
                            borderColor = seedColor.copy(alpha = 0.30f)
                        )
                    }
                    server.addonNames.take(2).forEach { addon ->
                        StreamBadgePill(
                            text = addon,
                            textColor = Color(0xFFA78BFA),
                            backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.10f),
                            borderColor = Color(0xFF8B5CF6).copy(alpha = 0.30f)
                        )
                    }
                    server.latencyMs?.let { latency ->
                        if (latency > 0) {
                            val dot = when {
                                latency < 100L -> "🟢"
                                latency < 300L -> "🟡"
                                else -> "🔴"
                            }
                            StreamBadgePill(
                                text = "$dot ${latency}ms",
                                textColor = Color.White.copy(alpha = 0.90f),
                                backgroundColor = PlayerTokens.VideoOverlayGlassBase,
                                borderColor = PlayerTokens.VideoOverlayGlassBorder
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.size(PlayerTokens.SizeCheckCircle),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(PlayerTokens.SizeCheckCircle)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = PlayerTokens.LabelDark,
                            modifier = Modifier.size(PlayerTokens.IconCheck)
                        )
                    }
                }
            }
        }

        if (!isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(HairlineWidth)
                    .background(Color.White.copy(alpha = 0.10f))
            )
        }
    }
}

/**
 * Tiered Sources List Content with Category Filter Pills, Sort Dropdown, and Hero Card.
 */
@Composable
fun SourcesListContent(
    servers: List<ServerSourceOption>,
    selectedServer: ServerSourceOption?,
    isSearching: Boolean = false,
    contentTitle: String = "",
    onSelectServer: (ServerSourceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(StreamCategoryFilter.ALL) }
    var selectedSort by remember { mutableStateOf(SourceSortOption.BEST_MATCH) }

    val hasActiveDebrid = remember { com.euthopiar.core.debrid.DebridManager.hasActiveDebrid() }
    val categoryCounts = remember(servers, hasActiveDebrid) {
        StreamCategoryFilter.values().associateWith { filter ->
            servers.count { matchesCategory(it, filter, hasActiveDebrid) }
        }
    }

    val filteredServers = remember(servers, selectedCategory, hasActiveDebrid) {
        if (selectedCategory == StreamCategoryFilter.ALL) {
            servers
        } else {
            servers.filter { matchesCategory(it, selectedCategory, hasActiveDebrid) }
        }
    }

    val sortedServers = remember(filteredServers, selectedSort) {
        when (selectedSort) {
            SourceSortOption.BEST_MATCH -> filteredServers
            SourceSortOption.SIZE_DESC -> filteredServers.sortedByDescending { parseSizeToBytes(it.size) }
            SourceSortOption.SIZE_ASC -> filteredServers.sortedWith(
                compareBy<ServerSourceOption> {
                    val b = parseSizeToBytes(it.size)
                    if (b <= 0L) Long.MAX_VALUE else b
                }
            )
            SourceSortOption.SEEDERS -> filteredServers.sortedWith(
                compareByDescending<ServerSourceOption> { it.seeders ?: it.streamSource.seeders ?: 0 }
                    .thenByDescending { parseSizeToBytes(it.size) }
            )
            SourceSortOption.LATENCY -> filteredServers.sortedWith(
                compareBy<ServerSourceOption> {
                    val lat = it.latencyMs
                    if (lat != null && lat > 0L) lat else Long.MAX_VALUE
                }.thenByDescending { it.seeders ?: it.streamSource.seeders ?: 0 }
            )
        }
    }

    val heroServer = sortedServers.firstOrNull()
    val remainingServers = remember(sortedServers) {
        if (sortedServers.size > 1) sortedServers.drop(1).distinctBy { it.id } else emptyList()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Category Pills (All, ⚡ Debrid, 👥 P2P, 4K, 1080p, 720p) with live counts
        item(key = "category_filter_row") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StreamCategoryFilter.values()) { filter ->
                    val isSelected = filter == selectedCategory
                    val count = categoryCounts[filter] ?: 0
                    Box(
                        modifier = Modifier
                            .clip(PlayerTokens.RadiusPill)
                            .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                            .border(HairlineWidth, if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                            .tvFocusable(shape = PlayerTokens.RadiusPill)
                            .playerPressable(
                                isWhiteElement = isSelected,
                                onClick = { selectedCategory = filter }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            style = PlayerTokens.PillText.copy(fontSize = 11.sp),
                            color = if (isSelected) PlayerTokens.LabelDark else Color.White
                        )
                    }
                }
            }
        }

        // Header with count & Sort Dropdown
        item(key = "sort_header_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countText = if (sortedServers.size == 1) "1 source" else "${sortedServers.size} sources"
                Text(
                    text = countText.uppercase(Locale.US),
                    style = PlayerTokens.SectionLabel,
                    color = Color.White.copy(alpha = 0.60f)
                )

                SourceSortDropdown(
                    selectedSort = selectedSort,
                    onSelectSort = { selectedSort = it }
                )
            }
        }

        // Empty state
        if (sortedServers.isEmpty()) {
            item(key = "empty_sources_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sources found for ${selectedCategory.label}",
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
        } else {
            // Hero / Recommended Stream Card at top
            if (heroServer != null) {
                item(key = "hero_stream_${heroServer.id}") {
                    val isSelected = heroServer.id == selectedServer?.id
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        StreamHeroCard(
                            server = heroServer,
                            isSelected = isSelected,
                            badgeLabel = if (selectedSort == SourceSortOption.BEST_MATCH) "RECOMMENDED" else "TOP MATCH",
                            onClick = { onSelectServer(heroServer) }
                        )
                    }
                }
            }

            // Remaining Streams
            items(remainingServers, key = { it.id }) { server ->
                val isSelected = server.id == selectedServer?.id
                SourceItemRow(
                    server = server,
                    isSelected = isSelected,
                    onClick = { onSelectServer(server) }
                )
            }
        }

        // STILL SEARCHING Footer
        item(key = "searching_footer") {
            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(PlayerTokens.SourcesFooterFadeMs)) + shrinkVertically(tween(PlayerTokens.SourcesFooterCollapseMs, easing = PlayerTokens.EaseIn))
            ) {
                SearchingFooter(count = servers.size)
            }
        }
    }
}


/**
 * "STILL SEARCHING" Footer (Section 4.2).
 * Three 4 dp dots (white 60%) pulsing in a wave (each dot 160 ms behind previous, cycle 960 ms, opacity 0.3 to 1.0).
 * "Finding more sources" (12 sp muted) and tabular count ("4 found").
 */
@Composable
fun SearchingFooter(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "searchWave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PlayerTokens.SourcesFooterWaveCycleMs.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { idx ->
                val offset = (waveProgress - (idx * 0.166f)).let { if (it < 0f) it + 1f else it }
                val alpha = 0.30f + 0.70f * (0.5f + 0.5f * kotlin.math.sin(offset * 2 * Math.PI.toFloat()))
                Box(
                    modifier = Modifier
                        .size(PlayerTokens.SizeActiveDot)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = alpha.coerceIn(0.3f, 1f)))
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Finding more sources",
                style = PlayerTokens.SheetSubtitle
            )
        }

        Text(
            text = "$count found",
            style = PlayerTokens.Timecode.copy(fontSize = 12.sp)
        )
    }
}

/**
 * Dedicated Portrait Sources Sheet (Section 4.2).
 * 28 dp top corners, hairline on top edge, 36x4 dp handle, 60% scrim, 70% height cap.
 * Headline "Sources" (26 sp medium) and 12 sp muted subtitle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerBottomSheet(
    availableServers: List<ServerSourceOption>,
    selectedServer: ServerSourceOption?,
    isSearching: Boolean = false,
    contentName: String = "",
    onServerSelected: (ServerSourceOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val containerBg = if (isAmoled) PlayerTokens.SheetGlassAmoledFill else PlayerTokens.SheetGlassFill
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        com.euthopiar.core.p2p.P2pStreamingEngine.prewarmEngine(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = PlayerTokens.RadiusBottomSheet,
        containerColor = containerBg,
        scrimColor = PlayerTokens.SheetScrim,
        dragHandle = {
            // 36 by 4 dp drag handle
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.40f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .navigationBarsPadding()
        ) {
            // Header: 26 sp medium headline + 12 sp subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sources",
                        style = PlayerTokens.SheetHeadline
                    )
                    if (contentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = contentName,
                            style = PlayerTokens.SheetSubtitle
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .videoOverlayGlass(shape = PlayerTokens.RadiusCircle)
                        .playerPressable(
                            role = Role.Button,
                            onClickLabel = "Close",
                            onClick = onDismissRequest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hairline divider under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HairlineWidth)
                    .background(Color.White.copy(alpha = 0.14f))
            )

            // Sources List Content
            SourcesListContent(
                servers = availableServers,
                selectedServer = selectedServer,
                isSearching = isSearching,
                contentTitle = contentName,
                onSelectServer = { srv ->
                    onServerSelected(srv)
                    onDismissRequest()
                },
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/**
 * Stream & Audio Sheet restyled with Sheet Glass and solid white check circles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamAndAudioSheet(
    servers: List<ServerSourceOption>,
    selectedServer: ServerSourceOption?,
    audioTracks: List<AudioTrackOption>,
    onServerSelected: (ServerSourceOption) -> Unit,
    onAudioTrackSelected: (AudioTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val containerBg = if (isAmoled) PlayerTokens.SheetGlassAmoledFill else PlayerTokens.SheetGlassFill

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = PlayerTokens.RadiusBottomSheet,
        containerColor = containerBg,
        scrimColor = PlayerTokens.SheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.40f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Audio & Streaming Sources",
                style = PlayerTokens.SheetHeadline
            )
            Spacer(Modifier.height(16.dp))

            // Audio Language section
            val displayTracks = audioTracks.filter { it.displayName.isNotBlank() && !it.displayName.contains("Track -1", ignoreCase = true) }
            if (displayTracks.isNotEmpty()) {
                Text(
                    text = "AUDIO LANGUAGE",
                    style = PlayerTokens.SectionLabel
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayTracks, key = { it.id }) { track ->
                        val isSelected = track.isSelected
                        Box(
                            modifier = Modifier
                                .clip(PlayerTokens.RadiusPill)
                                .background(if (isSelected) Color.White else PlayerTokens.VideoOverlayGlassBase)
                                .border(HairlineWidth, PlayerTokens.VideoOverlayGlassBorder, PlayerTokens.RadiusPill)
                                .tvFocusable(shape = PlayerTokens.RadiusPill)
                                .playerPressable(
                                    isWhiteElement = isSelected,
                                    onClick = { onAudioTrackSelected(track) }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = track.displayName,
                                style = PlayerTokens.PillText,
                                color = if (isSelected) PlayerTokens.LabelDark else Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Sources section
            Text(
                text = "STREAMING SOURCES",
                style = PlayerTokens.SectionLabel
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    val isSelected = server.id == selectedServer?.id
                    SourceItemRow(
                        server = server,
                        isSelected = isSelected,
                        onClick = { onServerSelected(server) }
                    )
                }
            }
        }
    }
}

/**
 * Quality Selection Bottom Sheet restyled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    qualities: List<VideoQualityOption>,
    selectedQuality: VideoQualityOption,
    onQualitySelected: (VideoQualityOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val containerBg = if (isAmoled) PlayerTokens.SheetGlassAmoledFill else PlayerTokens.SheetGlassFill

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = PlayerTokens.RadiusBottomSheet,
        containerColor = containerBg,
        scrimColor = PlayerTokens.SheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.40f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Video Quality",
                style = PlayerTokens.SheetHeadline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(qualities, key = { it.id }) { q ->
                    val isSelected = q.id == selectedQuality.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = q.label,
                                onClick = {
                                    onQualitySelected(q)
                                    onDismissRequest()
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = q.label, style = PlayerTokens.RowTitle)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(PlayerTokens.SizeCheckCircle)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = PlayerTokens.LabelDark,
                                        modifier = Modifier.size(PlayerTokens.IconCheck)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(HairlineWidth)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subtitles Selection Bottom Sheet restyled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlesBottomSheet(
    subtitles: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption,
    onSubtitleSelected: (SubtitleOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val containerBg = if (isAmoled) PlayerTokens.SheetGlassAmoledFill else PlayerTokens.SheetGlassFill

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = PlayerTokens.RadiusBottomSheet,
        containerColor = containerBg,
        scrimColor = PlayerTokens.SheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.40f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Subtitles",
                style = PlayerTokens.SheetHeadline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(subtitles, key = { it.id }) { sub ->
                    val isSelected = sub.id == selectedSubtitle.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = sub.label,
                                onClick = {
                                    onSubtitleSelected(sub)
                                    onDismissRequest()
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = sub.label, style = PlayerTokens.RowTitle)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(PlayerTokens.SizeCheckCircle)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = PlayerTokens.LabelDark,
                                        modifier = Modifier.size(PlayerTokens.IconCheck)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(HairlineWidth)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Audio Track Selection Bottom Sheet restyled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBottomSheet(
    audioTracks: List<AudioTrackOption>,
    selectedAudioTrack: AudioTrackOption,
    onAudioTrackSelected: (AudioTrackOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val containerBg = if (isAmoled) PlayerTokens.SheetGlassAmoledFill else PlayerTokens.SheetGlassFill

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = PlayerTokens.RadiusBottomSheet,
        containerColor = containerBg,
        scrimColor = PlayerTokens.SheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.40f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Audio Track",
                style = PlayerTokens.SheetHeadline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(audioTracks, key = { it.id }) { track ->
                    val isSelected = track.id == selectedAudioTrack.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .playerPressable(
                                role = Role.Button,
                                onClickLabel = track.displayName,
                                onClick = {
                                    onAudioTrackSelected(track)
                                    onDismissRequest()
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = track.displayName, style = PlayerTokens.RowTitle)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(PlayerTokens.SizeCheckCircle)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = PlayerTokens.LabelDark,
                                        modifier = Modifier.size(PlayerTokens.IconCheck)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(HairlineWidth)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                    }
                }
            }
        }
    }
}
