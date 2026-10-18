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

/**
 * Paced Sources List Content with Quality Dropdown Filter and breathing room.
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
    var displayedServers by remember { mutableStateOf(servers) }
    var selectedQualityFilter by remember { mutableStateOf(SourceQualityFilter.ALL) }

    LaunchedEffect(servers) {
        val currentIds = displayedServers.map { it.id }.toSet()
        val incoming = servers.filter { it.id !in currentIds }
        if (incoming.isEmpty()) {
            if (displayedServers.size > servers.size) {
                displayedServers = servers
            }
        } else {
            for (newSrv in incoming) {
                delay(PlayerTokens.SourceRowArrivalPacingMs)
                displayedServers = displayedServers + newSrv
            }
        }
    }

    val filteredServers = remember(displayedServers, selectedQualityFilter) {
        val kw = selectedQualityFilter.keyword
        if (kw == null) {
            displayedServers
        } else {
            displayedServers.filter { srv ->
                srv.serverName.contains(kw, ignoreCase = true) ||
                srv.resolutionLabel.contains(kw, ignoreCase = true) ||
                srv.label.contains(kw, ignoreCase = true) ||
                srv.streamSource.quality.contains(kw, ignoreCase = true) ||
                srv.streamSource.serverName.contains(kw, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Quality Filter Dropdown Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Sources",
                    style = PlayerTokens.SectionLabel,
                    color = Color.White.copy(alpha = 0.60f)
                )
                SourcesQualityDropdown(
                    selectedFilter = selectedQualityFilter,
                    onSelectFilter = { selectedQualityFilter = it }
                )
            }
        }

        if (filteredServers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sources matching ${selectedQualityFilter.label}",
                        style = PlayerTokens.SheetSubtitle,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
        } else {
            items(filteredServers, key = { it.id }) { server ->
                val isSelected = server.id == selectedServer?.id
                SourceItemRow(
                    server = server,
                    isSelected = isSelected,
                    onClick = { onSelectServer(server) }
                )
            }
        }

        // STILL SEARCHING Footer
        item {
            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(PlayerTokens.SourcesFooterFadeMs)) + shrinkVertically(tween(PlayerTokens.SourcesFooterCollapseMs, easing = PlayerTokens.EaseIn))
            ) {
                SearchingFooter(count = displayedServers.size)
            }
        }
    }
}

/**
 * Single Source Item Row (Section 4.2).
 * Unboxed, 0.5 dp hairline divider (white 14%).
 * Row height >= 56 dp, 16 dp horizontal padding.
 * 20 dp solid white circle with dark check on active source only.
 */
@Composable
fun SourceItemRow(
    server: ServerSourceOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .playerPressable(
                role = Role.Button,
                onClickLabel = server.serverName,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.serverName,
                    style = PlayerTokens.RowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val baseRes = if (server.resolutionLabel.isNotBlank()) server.resolutionLabel else "Auto"
                    val meta = if (!server.audioBadge.isNullOrBlank()) "$baseRes • ${server.audioBadge}" else baseRes
                    Text(
                        text = meta,
                        style = PlayerTokens.RowMeta
                    )
                    server.latencyMs?.let { latency ->
                        if (latency > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            ServerHealthBadge(latency, server.healthCategory)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Reserved 20 dp slot: 20 dp solid white circle with dark check if active
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

        // 0.5 dp hairline divider (white 14%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(HairlineWidth)
                .background(Color.White.copy(alpha = 0.14f))
        )
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
