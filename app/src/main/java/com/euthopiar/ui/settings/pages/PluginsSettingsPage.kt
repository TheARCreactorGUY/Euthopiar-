package com.euthopiar.ui.settings.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import com.euthopiar.core.addons.ManagedAddon
import com.euthopiar.core.addons.addonTransportBaseUrl
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.ui.settings.PluginManagerViewModel
import com.euthopiar.ui.settings.plugins.*
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class PluginsTab(val label: String) {
    REPOSITORIES("Repositories"),
    ADDONS("Addons"),
    SCRAPERS("Scrapers")
}

/**
 * Screen 1: The Hub ("Plugins & Addons").
 * High-performance virtualized architecture with:
 * - Fixed Top Bar (Back & spinning Refresh button + compact title fade)
 * - Collapsible Headline & 3-Column Metric Summary Row with 2dp segmented bar
 * - HorizontalPager supporting fluid left/right swiping between tabs
 * - Dynamic sliding & resizing white underline tracking pager touch drag
 * - Fully virtualized LazyColumns per tab for 120 FPS buttery smooth scrolling
 * - 100% white-on-dark monochrome status language (NO red, green, orange, yellow, gold)
 */
@Composable
fun PluginsSettingsPage(
    onBack: () -> Unit,
    onNavigateToRepo: ((String) -> Unit)? = null,
    viewModel: PluginManagerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val providerRepos by viewModel.providerRepos.collectAsState()
    val repositoryUrls by viewModel.repositoryUrls.collectAsState()
    val addonsUiState by viewModel.addonsUiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val testingIds by viewModel.testingPluginIds.collectAsState()
    val healthMap by viewModel.pluginHealthMap.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { PluginsTab.entries.size })

    var repoUrlInput by rememberSaveable { mutableStateOf("") }
    var addonUrlInput by rememberSaveable { mutableStateOf("") }
    var scraperSearchQuery by rememberSaveable { mutableStateOf("") }

    var statusDialogState by remember { mutableStateOf<StatusDialogState?>(null) }
    var alreadyInstalledAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    var repoToDelete by remember { mutableStateOf<String?>(null) }
    var pluginToDelete by remember { mutableStateOf<PluginManifest?>(null) }
    var addonToDelete by remember { mutableStateOf<ManagedAddon?>(null) }
    var isSyncingOfficial by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Segmented progress numbers
    val totalAvailableScrapers = remember(providerRepos, installedPlugins) {
        val repoScraperIds = providerRepos.flatMap { it.plugins }.map { it.id }.toSet()
        val installedIds = installedPlugins.map { it.id }.toSet()
        (repoScraperIds + installedIds).size
    }
    val downloadedCount = installedPlugins.count { it.isEnabled }
    val disabledCount = installedPlugins.count { !it.isEnabled }
    val notDownloadedCount = (totalAvailableScrapers - installedPlugins.size).coerceAtLeast(0)

    val pageBg = if (isAmoled) PluginsGlassTokens.AmoledBackground else PluginsGlassTokens.BaseDarkBackground

    // Collapsible header math
    val maxCollapseHeight = 175.dp
    val maxCollapsePx = with(density) { maxCollapseHeight.toPx() }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) { // Scrolling up -> collapse header
                    val prev = collapseOffsetPx
                    collapseOffsetPx = (collapseOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                    val consumed = collapseOffsetPx - prev
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f) { // Scrolling down -> expand header
                    val prev = collapseOffsetPx
                    collapseOffsetPx = (collapseOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                    val consumed = collapseOffsetPx - prev
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    val collapseFraction = (-collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
    val currentCollapsibleHeight = maxCollapseHeight * (1f - collapseFraction)

    // Live filtering for scrapers tab (matching name, description, types, and language)
    val filteredScrapers = remember(installedPlugins, scraperSearchQuery) {
        val base = installedPlugins.distinctBy { it.id }
        if (scraperSearchQuery.isBlank()) {
            base
        } else {
            val q = scraperSearchQuery.trim().lowercase()
            base.filter { p ->
                p.name.lowercase().contains(q) ||
                p.description.lowercase().contains(q) ||
                p.supportedTypes.any { it.lowercase().contains(q) } ||
                p.id.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // 1. Fixed Top Bar (Back & Refresh Buttons + Animated Compact Title)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PluginsGlassCircleButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = onBack,
                        contentDescription = "Back",
                        size = 40.dp
                    )

                    Text(
                        text = "Plugins & addons",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.alpha(collapseFraction)
                    )

                    PluginsGlassCircleButton(
                        icon = Icons.Outlined.Sync,
                        onClick = {
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                            statusDialogState = StatusDialogState.Installing
                            viewModel.updateAllReposAndAddons { success, msg ->
                                statusDialogState = if (success) {
                                    StatusDialogState.Success(msg)
                                } else {
                                    StatusDialogState.Error(msg)
                                }
                            }
                        },
                        contentDescription = "Update all repositories and addons",
                        isLoading = isRefreshing,
                        size = 40.dp
                    )
                }
            }

            // 2. Collapsible Header Area (Headline + 3-Column Summary + Segmented Bar)
            if (collapseFraction < 0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentCollapsibleHeight)
                        .clipToBounds()
                        .alpha(1f - collapseFraction)
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Headline: "Plugins" and "& addons" on two lines, 32sp medium
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                text = "Plugins",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 36.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Text(
                                text = "& addons",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 36.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        }

                        // Summary Row: 3 equal columns with large 28sp numerals, 2dp segmented bar, and subtitle
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Scrapers Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${downloadedCount + disabledCount}",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "SCRAPERS",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )
                                }

                                // Repositories Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${repositoryUrls.size}",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "REPOSITORIES",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )
                                }

                                // Addons Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${addonsUiState.addons.size}",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "ADDONS",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )
                                }
                            }

                            // Full-width 2dp segmented bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(PluginsGlassTokens.HairlineDivider)
                            ) {
                                val total = (downloadedCount + disabledCount + notDownloadedCount).coerceAtLeast(1).toFloat()
                                if (downloadedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(downloadedCount / total)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(alpha = 0.88f))
                                    )
                                }
                                if (disabledCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(disabledCount / total)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(alpha = 0.30f))
                                    )
                                }
                                if (notDownloadedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(notDownloadedCount / total)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(alpha = 0.14f))
                                    )
                                }
                            }

                            // Subtitle
                            Text(
                                text = "$downloadedCount active · $disabledCount disabled · $notDownloadedCount available to install",
                                style = TextStyle(
                                    color = PluginsGlassTokens.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // 3. Tab Row: Repositories / Addons / Scrapers with dynamic sliding white underline
            HubTabRow(
                pagerState = pagerState,
                onSelectTab = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                },
                reduceMotion = reduceMotion
            )

            // 4. Horizontal Pager (Virtualized per-page LazyColumns)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // TAB 1: REPOSITORIES
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: ADD REPOSITORY
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "ADD REPOSITORY",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )

                                    PluginsGlassInputField(
                                        value = repoUrlInput,
                                        onValueChange = { repoUrlInput = it },
                                        placeholder = "https://example.com/repo.json",
                                        onPaste = {
                                            val text = clipboardManager.getText()?.text
                                            if (!text.isNullOrBlank()) {
                                                repoUrlInput = text.trim()
                                            }
                                        },
                                        onClear = { repoUrlInput = "" },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val installButtonState = when {
                                        statusDialogState is StatusDialogState.Installing -> InstallButtonState.BUSY
                                        repoUrlInput.isNotBlank() -> InstallButtonState.READY
                                        else -> InstallButtonState.EMPTY
                                    }

                                    PluginsInstallButton(
                                        state = installButtonState,
                                        label = "Install repository",
                                        onClick = {
                                            val url = repoUrlInput.trim()
                                            if (url.isNotBlank()) {
                                                if (repositoryUrls.contains(url)) {
                                                    alreadyInstalledAlert = "Repository Already Installed" to "The repository \"$url\" is already installed on your device."
                                                } else {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                    statusDialogState = StatusDialogState.Installing
                                                    viewModel.addRepository(url) { success, msg ->
                                                        statusDialogState = if (success) {
                                                            repoUrlInput = ""
                                                            StatusDialogState.Success(msg ?: url)
                                                        } else {
                                                            StatusDialogState.Error(
                                                                message = msg ?: "Failed to add repository",
                                                                onRetry = {
                                                                    statusDialogState = StatusDialogState.Installing
                                                                    viewModel.addRepository(url) { s, m ->
                                                                        statusDialogState = if (s) {
                                                                            repoUrlInput = ""
                                                                            StatusDialogState.Success(m ?: url)
                                                                        } else {
                                                                            StatusDialogState.Error(m ?: "Failed to add repository")
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    // Sync official repositories glass button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                            .background(PluginsGlassTokens.GlassFill)
                                            .border(
                                                PluginsGlassTokens.HairlineWidth,
                                                PluginsGlassTokens.HairlineBorder,
                                                RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                            )
                                            .pluginsPressable(enabled = !isSyncingOfficial, scaleDown = 0.98f) {
                                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                isSyncingOfficial = true
                                                statusDialogState = StatusDialogState.Installing
                                                viewModel.syncCommunityRepository { success, msg ->
                                                    isSyncingOfficial = false
                                                    statusDialogState = if (success) {
                                                        StatusDialogState.Success(msg ?: "Official repositories installed")
                                                    } else {
                                                        StatusDialogState.Error(msg ?: "Failed to sync repositories")
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isSyncingOfficial) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Syncing...",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Sync official repositories",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: INSTALLED · X with Update all action
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "INSTALLED · ${repositoryUrls.size}",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )

                                    if (repositoryUrls.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                                .background(PluginsGlassTokens.GlassFill)
                                                .border(
                                                    PluginsGlassTokens.HairlineWidth,
                                                    PluginsGlassTokens.HairlineBorder,
                                                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                                )
                                                .pluginsPressable(enabled = !isRefreshing, scaleDown = 0.98f) {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                    statusDialogState = StatusDialogState.Installing
                                                    viewModel.updateAllReposAndAddons { success, msg ->
                                                        statusDialogState = if (success) StatusDialogState.Success(msg) else StatusDialogState.Error(msg)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Sync,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Update all",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (repositoryUrls.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusProviderIcon)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Public,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Text(
                                                text = "No repositories yet",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Add a repository URL above to discover scrapers.",
                                                color = PluginsGlassTokens.TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            } else {
                                val repoList = repositoryUrls.toList()
                                items(repoList.distinct(), key = { it }) { url ->
                                    val repoInfo = providerRepos.find { it.url == url }
                                    val repoName = repoInfo?.name ?: url.substringAfterLast("/").substringBefore(".json").ifBlank { url }
                                    val host = remember(url) {
                                        url.removePrefix("https://").removePrefix("http://").substringBefore("/")
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pluginsPressable(scaleDown = 0.98f) {
                                                val encoded = URLEncoder.encode(url, "UTF-8")
                                                onNavigateToRepo?.invoke("settings/plugins/repo/$encoded")
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // 40dp glass tile with outline globe icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Public,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = repoName,
                                                style = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${repoInfo?.plugins?.size ?: 0} scrapers · $host",
                                                style = TextStyle(
                                                    color = PluginsGlassTokens.TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Outline update / sync
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .pluginsPressable(scaleDown = 0.98f) {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                    statusDialogState = StatusDialogState.Installing
                                                    viewModel.updateRepositoryAndPlugins(url) { success, msg ->
                                                        statusDialogState = if (success) StatusDialogState.Success(msg) else StatusDialogState.Error(msg)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Sync,
                                                contentDescription = "Update repository plugins",
                                                tint = Color.White.copy(alpha = 0.70f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Outline trash
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .pluginsPressable(scaleDown = 0.98f) {
                                                    repoToDelete = url
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = PluginsGlassTokens.TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Outline chevron
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                            contentDescription = "Open",
                                            tint = Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    if (url != repoList.lastOrNull()) {
                                        PluginsHairlineDivider()
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }

                    1 -> {
                        // TAB 2: STREMIO ADDONS
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: ADD STREMIO ADDON
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "ADD STREMIO ADDON",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )

                                    PluginsGlassInputField(
                                        value = addonUrlInput,
                                        onValueChange = { addonUrlInput = it },
                                        placeholder = "https://addon.example.com/manifest.json",
                                        onPaste = {
                                            val text = clipboardManager.getText()?.text
                                            if (!text.isNullOrBlank()) {
                                                addonUrlInput = text.trim()
                                            }
                                        },
                                        onClear = { addonUrlInput = "" },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val installButtonState = when {
                                        statusDialogState is StatusDialogState.Installing -> InstallButtonState.BUSY
                                        addonUrlInput.isNotBlank() -> InstallButtonState.READY
                                        else -> InstallButtonState.EMPTY
                                    }

                                    PluginsInstallButton(
                                        state = installButtonState,
                                        label = "Install addon",
                                        onClick = {
                                            val url = addonUrlInput.trim()
                                            if (url.isNotBlank()) {
                                                if (addonsUiState.addons.any { it.manifestUrl.trim().equals(url, ignoreCase = true) }) {
                                                    alreadyInstalledAlert = "Addon Already Installed" to "The addon from \"$url\" is already installed on your device."
                                                } else {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                    statusDialogState = StatusDialogState.Installing
                                                    viewModel.installAddon(url) { success, msg ->
                                                        statusDialogState = if (success) {
                                                            addonUrlInput = ""
                                                            StatusDialogState.Success(msg ?: url)
                                                        } else {
                                                            StatusDialogState.Error(
                                                                message = msg ?: "Failed to install addon",
                                                                onRetry = {
                                                                    statusDialogState = StatusDialogState.Installing
                                                                    viewModel.installAddon(url) { s, m ->
                                                                        statusDialogState = if (s) {
                                                                            addonUrlInput = ""
                                                                            StatusDialogState.Success(m ?: url)
                                                                        } else {
                                                                            StatusDialogState.Error(m ?: "Failed to install addon")
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Section: INSTALLED · X with Update all action
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "INSTALLED · ${addonsUiState.addons.size}",
                                        style = TextStyle(
                                            color = PluginsGlassTokens.TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.1.sp
                                        )
                                    )

                                    if (addonsUiState.addons.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                                .background(PluginsGlassTokens.GlassFill)
                                                .border(
                                                    PluginsGlassTokens.HairlineWidth,
                                                    PluginsGlassTokens.HairlineBorder,
                                                    RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                                )
                                                .pluginsPressable(enabled = !isRefreshing, scaleDown = 0.98f) {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                                    statusDialogState = StatusDialogState.Installing
                                                    viewModel.updateAllAddons { success, msg ->
                                                        statusDialogState = if (success) StatusDialogState.Success(msg ?: "Addons updated") else StatusDialogState.Error(msg ?: "Failed to update addons")
                                                    }
                                                }
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Sync,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Update all",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (addonsUiState.addons.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusProviderIcon)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Widgets,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Text(
                                                text = "No Stremio addons installed",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Add a manifest URL above to install community addons.",
                                                color = PluginsGlassTokens.TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(addonsUiState.addons.distinctBy { it.manifestUrl }, key = { it.manifestUrl }) { addon ->
                                    val isEnabled = addon.enabled
                                    val rowAlpha = if (isEnabled) 1f else 0.55f

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(rowAlpha)
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // 40dp glass tile with outline widgets icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Widgets,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = addon.displayTitle,
                                                    style = TextStyle(
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                val vStr = "v${addon.manifest?.version ?: "1.0"}"
                                                Text(
                                                    text = vStr,
                                                    style = TextStyle(
                                                        color = PluginsGlassTokens.TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            val desc = addon.manifest?.description
                                            if (!desc.isNullOrBlank()) {
                                                Text(
                                                    text = desc,
                                                    style = TextStyle(
                                                        color = PluginsGlassTokens.TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // 44x26dp Toggle Switch (full opacity)
                                        PluginsGlassToggle(
                                            checked = isEnabled,
                                            onCheckedChange = { viewModel.toggleAddon(addon.manifestUrl, it) },
                                            modifier = Modifier.alpha(1f / rowAlpha)
                                        )

                                        // Configuration action if configurable
                                        if (addon.manifest?.behaviorHints?.configurable == true) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .pluginsPressable(scaleDown = 0.98f) {
                                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                        val baseUrl = addonTransportBaseUrl(addon.manifestUrl)
                                                        val query = addon.manifestUrl.substringAfter("?", "")
                                                        val configUrl = if (query.isNotBlank()) "$baseUrl/configure?$query" else "$baseUrl/configure"
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(configUrl)).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            toastMessage = "Could not open configuration: ${e.message}"
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Settings,
                                                    contentDescription = "Configure Addon",
                                                    tint = Color.White.copy(alpha = 0.70f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Outline update / sync
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .pluginsPressable(scaleDown = 0.98f) {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                    viewModel.refreshAddon(addon.manifestUrl)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Sync,
                                                contentDescription = "Update Addon",
                                                tint = Color.White.copy(alpha = 0.70f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Outline trash
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .pluginsPressable(scaleDown = 0.98f) {
                                                    addonToDelete = addon
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Uninstall Addon",
                                                tint = PluginsGlassTokens.TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (addon != addonsUiState.addons.lastOrNull()) {
                                        PluginsHairlineDivider()
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: INSTALLED SCRAPERS (Virtualized LazyColumn for 120 FPS performance)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 44dp Glass Search Field
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(PluginsGlassTokens.RadiusInput))
                                        .background(PluginsGlassTokens.GlassFill)
                                        .border(
                                            PluginsGlassTokens.HairlineWidth,
                                            PluginsGlassTokens.HairlineBorder,
                                            RoundedCornerShape(PluginsGlassTokens.RadiusInput)
                                        )
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = PluginsGlassTokens.TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (scraperSearchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search ${installedPlugins.size} scrapers",
                                                    color = PluginsGlassTokens.TextTertiary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                            BasicTextField(
                                                value = scraperSearchQuery,
                                                onValueChange = { scraperSearchQuery = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                cursorBrush = SolidColor(Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (scraperSearchQuery.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .pluginsPressable(scaleDown = 0.98f) {
                                                        scraperSearchQuery = ""
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Clear search",
                                                    tint = PluginsGlassTokens.TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: INSTALLED · X
                            item {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "INSTALLED · ${filteredScrapers.size}",
                                    style = TextStyle(
                                        color = PluginsGlassTokens.TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.1.sp
                                    )
                                )
                            }

                            if (installedPlugins.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusProviderIcon)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Extension,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Text(
                                                text = "No scrapers installed",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Install community repositories to discover providers.",
                                                color = PluginsGlassTokens.TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            } else if (filteredScrapers.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "No scrapers matching \"$scraperSearchQuery\"",
                                                color = PluginsGlassTokens.TextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                            PluginsGlassPillButton(
                                                text = "Clear search",
                                                onClick = { scraperSearchQuery = "" }
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filteredScrapers.distinctBy { it.id }, key = { it.id }) { manifest ->
                                    val isTesting = testingIds.contains(manifest.id)
                                    val health = healthMap[manifest.id]
                                    val isEnabled = manifest.isEnabled
                                    val rowAlpha = if (isEnabled) 1f else 0.55f

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(rowAlpha)
                                            .padding(vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // 40dp glass tile with outline puzzle icon
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Extension,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = manifest.name,
                                                        style = TextStyle(
                                                            color = Color.White,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val vStr = "v${manifest.versionName.ifBlank { manifest.version.toString() }}"
                                                    Text(
                                                        text = vStr,
                                                        style = TextStyle(
                                                            color = PluginsGlassTokens.TextSecondary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    )
                                                }

                                                Text(
                                                    text = "English · Concurrent Scraper",
                                                    style = TextStyle(
                                                        color = PluginsGlassTokens.TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                )
                                            }

                                            // 44x26dp Toggle Switch (full opacity)
                                            PluginsGlassToggle(
                                                checked = isEnabled,
                                                onCheckedChange = { viewModel.togglePluginEnabled(manifest) },
                                                modifier = Modifier.alpha(1f / rowAlpha)
                                            )

                                            // Outline trash
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .pluginsPressable(scaleDown = 0.98f) {
                                                        pluginToDelete = manifest
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Uninstall",
                                                    tint = PluginsGlassTokens.TextTertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (manifest.description.isNotBlank()) {
                                            Text(
                                                text = manifest.description,
                                                style = TextStyle(
                                                    color = PluginsGlassTokens.TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 54.dp)
                                            )
                                        }

                                        // Latency Diagnostic Probe Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 54.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Test Control Pill (32dp tall, monochrome)
                                            Box(
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                                    .background(PluginsGlassTokens.GlassFill)
                                                    .border(
                                                        PluginsGlassTokens.HairlineWidth,
                                                        PluginsGlassTokens.HairlineBorder,
                                                        RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                                    )
                                                    .pluginsPressable(enabled = !isTesting, scaleDown = 0.98f) {
                                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                        viewModel.testPlugin(manifest)
                                                    }
                                                    .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (isTesting) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 2.dp,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Testing...",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Speed,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.70f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = if (health != null) "Retest" else "Test",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }

                                            // Status indicator (monochrome outline check / error)
                                            if (health != null) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (health.isWorking) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.85f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = health.message,
                                                            color = Color.White.copy(alpha = 0.85f),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ErrorOutline,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.60f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Not responding",
                                                            color = Color.White.copy(alpha = 0.60f),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (manifest != filteredScrapers.lastOrNull()) {
                                        PluginsHairlineDivider()
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                }
            }
        }

        // Glass Toast for in-page notifications
        PluginsGlassToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    // Modal: Install Status In-Place Dialog
    statusDialogState?.let { state ->
        PluginsInstallStatusDialog(
            state = state,
            onDismiss = { statusDialogState = null }
        )
    }

    // Modal: Already Installed Alert Dialog
    alreadyInstalledAlert?.let { (title, msg) ->
        com.euthopiar.ui.components.EuthopiarGlassDialog(
            title = title,
            message = msg,
            confirmText = "Got it",
            dismissText = null,
            icon = Icons.Outlined.CheckCircle,
            onConfirm = { alreadyInstalledAlert = null },
            onDismiss = { alreadyInstalledAlert = null }
        )
    }

    // Modal: Delete Repository Confirmation
    repoToDelete?.let { url ->
        PluginsSharedGlassDialog(
            title = "Delete repository?",
            message = "This will remove repository \"$url\" and its registered scrapers.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.removeRepository(url)
                repoToDelete = null
            },
            onDismiss = { repoToDelete = null },
            itemPreview = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = url,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    // Modal: Uninstall Scraper Confirmation
    pluginToDelete?.let { manifest ->
        PluginsSharedGlassDialog(
            title = "Uninstall ${manifest.name}?",
            message = "This will remove the provider from active resolution.",
            confirmText = "Uninstall",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.uninstallPlugin(manifest) { _ -> }
                pluginToDelete = null
            },
            onDismiss = { pluginToDelete = null },
            itemPreview = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = manifest.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "v${manifest.versionName.ifBlank { manifest.version.toString() }}",
                            color = PluginsGlassTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        )
    }

    // Modal: Uninstall Addon Confirmation
    addonToDelete?.let { addon ->
        PluginsSharedGlassDialog(
            title = "Uninstall ${addon.displayTitle}?",
            message = "This will remove the Stremio addon manifest.",
            confirmText = "Uninstall",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.removeAddon(addon.manifestUrl)
                addonToDelete = null
            },
            onDismiss = { addonToDelete = null },
            itemPreview = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = addon.displayTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "v${addon.manifest?.version ?: "1.0"}",
                            color = PluginsGlassTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        )
    }
}

/**
 * Three text tabs: "Repositories", "Addons", "Scrapers".
 * Left aligned, 22dp spacing between tabs, 13sp medium.
 * Underline dynamically glides and stretches with PagerState touch scroll position.
 */
@Composable
private fun HubTabRow(
    pagerState: PagerState,
    onSelectTab: (PluginsTab) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    // Initialize with calibrated defaults so underline renders with zero glitch on frame 0
    val tabPositions = remember {
        mutableStateMapOf<Int, Pair<Dp, Dp>>(
            0 to (0.dp to 86.dp),
            1 to (108.dp to 50.dp),
            2 to (180.dp to 58.dp)
        )
    }

    val continuousPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (PluginsTab.entries.size - 1).toFloat())

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginsTab.entries.forEachIndexed { index, tab ->
                val tabDistance = kotlin.math.abs(continuousPosition - index)
                val activeRatio = (1f - tabDistance).coerceIn(0f, 1f)
                val textAlpha = 0.45f + (0.55f * activeRatio)
                val textColor = Color.White.copy(alpha = textAlpha)

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val xDp = with(density) { coords.positionInParent().x.toDp() }
                            val wDp = with(density) { coords.size.width.toDp() }
                            if (wDp > 0.dp) {
                                tabPositions[index] = xDp to wDp
                            }
                        }
                        .pluginsPressable(scaleDown = 0.98f) {
                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            onSelectTab(tab)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Underline and Hairline container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            // Full width hairline divider
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                thickness = PluginsGlassTokens.HairlineWidth,
                color = PluginsGlassTokens.HairlineDivider
            )

            // Inner container with the EXACT same 20.dp horizontal padding as the tab row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.BottomStart)
            ) {
                val baseIndex = continuousPosition.toInt().coerceIn(0, PluginsTab.entries.size - 1)
                val nextIndex = (baseIndex + 1).coerceIn(0, PluginsTab.entries.size - 1)
                val fraction = (continuousPosition - baseIndex).coerceIn(0f, 1f)

                val baseTab = tabPositions[baseIndex]
                val nextTab = tabPositions[nextIndex]

                val targetOffset = if (baseTab != null && nextTab != null) {
                    lerp(baseTab.first, nextTab.first, fraction)
                } else baseTab?.first

                val targetWidth = if (baseTab != null && nextTab != null) {
                    lerp(baseTab.second, nextTab.second, fraction)
                } else baseTab?.second

                if (targetOffset != null && targetWidth != null && targetWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = targetOffset)
                            .width(targetWidth)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
