package com.euthopiar.ui.settings.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.settings.models.SettingsCategory
import com.euthopiar.ui.settings.models.SettingsPage
import com.euthopiar.ui.settings.models.SettingsSearchEntry
import com.euthopiar.ui.theme.*

fun getAllSettingsSearchEntries(): List<SettingsSearchEntry> = listOf(
    // Playback
    SettingsSearchEntry(
        id = "stream_quality",
        title = "Default Stream Quality",
        description = "Set preferred resolution (Auto, 1080p, 720p, 480p)",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("resolution", "quality", "1080p", "4k", "auto"),
        icon = Icons.Outlined.HighQuality
    ),
    SettingsSearchEntry(
        id = "buffer_size",
        title = "Video Buffer Size",
        description = "Adjust pre-loaded memory buffer (clamped to device RAM)",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("buffer", "ram", "memory", "preload", "lag", "stutter"),
        icon = Icons.Outlined.Storage
    ),
    SettingsSearchEntry(
        id = "seek_interval",
        title = "Seek Interval",
        description = "Double tap forward/backward skip duration (5s, 10s, 15s, 30s)",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("seek", "skip", "fast forward", "rewind", "double tap"),
        icon = Icons.Outlined.FastForward
    ),
    SettingsSearchEntry(
        id = "auto_play_next",
        title = "Auto-Play Next Episode",
        description = "Automatically queue next chapter when episode concludes",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("autoplay", "next", "binge", "episode"),
        icon = Icons.Outlined.PlayCircleOutline
    ),
    SettingsSearchEntry(
        id = "auto_skip_intro",
        title = "Auto-Skip Intro",
        description = "Skip title and opening sequence credits automatically",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("skip", "intro", "opening", "credits"),
        icon = Icons.Outlined.SkipNext
    ),
    SettingsSearchEntry(
        id = "subtitles",
        title = "Subtitles & Captions Styling",
        description = "Customize subtitle font size, color, background box and outline",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("subtitles", "captions", "font", "size", "color", "opensubtitles"),
        icon = Icons.Outlined.Subtitles
    ),
    SettingsSearchEntry(
        id = "hardware_accel",
        title = "Hardware Video Acceleration",
        description = "GPU decoding acceleration for HEVC and 4K streams (restart required)",
        category = SettingsCategory.General,
        page = SettingsPage.Playback,
        keywords = listOf("hardware", "gpu", "decoder", "tunneling", "acceleration", "hevc"),
        icon = Icons.Outlined.Speed
    ),

    // Appearance
    SettingsSearchEntry(
        id = "card_scale",
        title = "Card Scale Size",
        description = "Adjust home catalog poster card dimensions (Compact, Balanced, Large)",
        category = SettingsCategory.General,
        page = SettingsPage.Appearance,
        keywords = listOf("card", "scale", "poster", "size", "compact", "large"),
        icon = Icons.Outlined.AspectRatio
    ),

    SettingsSearchEntry(
        id = "top10_style",
        title = "Top 10 Row Style",
        description = "Switch between poster-tinted bleed and frosted acrylic glass tab",
        category = SettingsCategory.General,
        page = SettingsPage.Appearance,
        keywords = listOf("top 10", "ranking", "trending", "tab", "tint"),
        icon = Icons.Outlined.FormatListNumbered
    ),
    SettingsSearchEntry(
        id = "long_press_style",
        title = "Long-Press Sheet Style",
        description = "Select preview style: Hero peek, Glass menu, Tinted resume, Trailer card, Pill actions, Reaction list",
        category = SettingsCategory.General,
        page = SettingsPage.Appearance,
        keywords = listOf("long press", "dropdown", "sheet", "hero peek", "trailer", "reaction"),
        icon = Icons.Outlined.TouchApp
    ),
    SettingsSearchEntry(
        id = "reduce_motion",
        title = "Reduce Motion",
        description = "Disable fluid spring animations and transitions for performance",
        category = SettingsCategory.General,
        page = SettingsPage.Appearance,
        keywords = listOf("motion", "animation", "reduce", "speed", "smooth"),
        icon = Icons.Outlined.MotionPhotosOff
    ),

    // Integrations
    SettingsSearchEntry(
        id = "trakt",
        title = "Trakt.tv Scrobbling",
        description = "Sync watching history, progress and ratings with Trakt",
        category = SettingsCategory.Ecosystem,
        page = SettingsPage.Integrations,
        keywords = listOf("trakt", "scrobble", "sync", "history", "account"),
        icon = Icons.Outlined.CloudSync
    ),
    SettingsSearchEntry(
        id = "simkl",
        title = "Simkl Watchlist Sync",
        description = "Synchronize TV and movie watchlists with Simkl",
        category = SettingsCategory.Ecosystem,
        page = SettingsPage.Integrations,
        keywords = listOf("simkl", "sync", "watchlist", "tracking"),
        icon = Icons.Outlined.Sync
    ),
    SettingsSearchEntry(
        id = "debrid",
        title = "Debrid Cloud Services",
        description = "Configure Real-Debrid, AllDebrid and TorBox for high-speed streaming",
        category = SettingsCategory.Ecosystem,
        page = SettingsPage.Integrations,
        keywords = listOf("debrid", "real-debrid", "alldebrid", "torbox", "premium", "api key"),
        icon = Icons.Outlined.Bolt
    ),

    // Plugins & Scrapers
    SettingsSearchEntry(
        id = "plugins",
        title = "Plugins & Repositories",
        description = "Manage scraping repositories, community sources, live tester and priority",
        category = SettingsCategory.Ecosystem,
        page = SettingsPage.Plugins,
        keywords = listOf("plugins", "addons", "scrapers", "repositories", "sources", "manifest"),
        icon = Icons.Outlined.Extension
    ),

    // Storage & Cache
    SettingsSearchEntry(
        id = "storage_cache",
        title = "Clear Cache & Storage",
        description = "Clear image disk cache, HTTP network cache and manage downloaded videos",
        category = SettingsCategory.Advanced,
        page = SettingsPage.Storage,
        keywords = listOf("storage", "cache", "clear", "disk", "clean", "downloads"),
        icon = Icons.Outlined.DeleteSweep
    ),
    SettingsSearchEntry(
        id = "config_backup",
        title = "Export & Import Configuration",
        description = "Backup your app settings and plugin repositories safely",
        category = SettingsCategory.Advanced,
        page = SettingsPage.Storage,
        keywords = listOf("backup", "export", "import", "restore", "json"),
        icon = Icons.Outlined.FileDownload
    ),

    // About
    SettingsSearchEntry(
        id = "app_updates",
        title = "Application Updates",
        description = "Check for new GitHub releases, view changelog and install APK updates",
        category = SettingsCategory.About,
        page = SettingsPage.Updates,
        keywords = listOf("update", "upgrade", "version", "github", "release", "apk", "changelog"),
        icon = Icons.Outlined.SystemUpdateAlt
    ),
    SettingsSearchEntry(
        id = "about_euthopiar",
        title = "About Euthopiar",
        description = "App version, open-source licenses, GitHub repository and acknowledgments",
        category = SettingsCategory.About,
        page = SettingsPage.About,
        keywords = listOf("about", "version", "licenses", "github", "euthopiar"),
        icon = Icons.Outlined.Info
    )
)

@Composable
fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.16f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
        label = "searchFieldBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.size(19.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search settings",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.50f)
                        )
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Clear search",
                            onClick = { onQueryChange("") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}
