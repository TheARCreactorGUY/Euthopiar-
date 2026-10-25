package com.euthopiar.ui.settings.pages

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.core.js.computeCanonicalRepoId
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.core.model.PluginRepositoryIndex
import com.euthopiar.ui.settings.PluginManagerViewModel
import com.euthopiar.ui.settings.plugins.*
import com.euthopiar.ui.theme.LocalAmoledMode
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val CATEGORY_FILTERS = listOf(
    "All",
    "Movies",
    "TV Series",
    "Anime",
    "Asian Dramas"
)

/**
 * Screen 2: Repository Detail Screen.
 * Displays repository contents, category filter pills with right fade,
 * scraper rows with outline puzzle icons, download circles, and ProviderDetailSheet.
 */
@Composable
fun RepoPluginsScreen(
    repoUrl: String,
    onBack: () -> Unit,
    viewModel: PluginManagerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAmoled = LocalAmoledMode.current

    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val providerRepos by viewModel.providerRepos.collectAsState()
    val installingIds by viewModel.installingPluginIds.collectAsState()
    val testingIds by viewModel.testingPluginIds.collectAsState()
    val healthMap by viewModel.pluginHealthMap.collectAsState()

    var repoIndex by remember { mutableStateOf<PluginRepositoryIndex?>(null) }
    var isLoadingRepo by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPluginForSheet by remember { mutableStateOf<PluginManifest?>(null) }
    var pluginToUninstall by remember { mutableStateOf<PluginManifest?>(null) }
    var alreadyInstalledAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isInstallingAll by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val repoHazeState = rememberHazeState()
    val scope = rememberCoroutineScope()

    // Fetch repository manifest
    fun loadRepo() {
        scope.launch {
            isLoadingRepo = true
            loadError = false
            val cached = providerRepos.find { it.url == repoUrl }
            if (cached != null) {
                repoIndex = PluginRepositoryIndex(
                    name = cached.name,
                    description = cached.description,
                    manifestVersion = 1,
                    plugins = cached.plugins
                )
                isLoadingRepo = false
            } else {
                val fetched = viewModel.fetchRepoDetails(repoUrl)
                if (fetched != null) {
                    repoIndex = fetched
                } else {
                    loadError = true
                }
                isLoadingRepo = false
            }
        }
    }

    LaunchedEffect(repoUrl) {
        loadRepo()
    }

    val plugins = repoIndex?.plugins ?: emptyList()
    val filteredPlugins = remember(plugins, selectedCategory) {
        when (selectedCategory) {
            "All" -> plugins
            "Movies" -> plugins.filter { p ->
                p.supportedTypes.any { it.contains("movie", ignoreCase = true) }
            }
            "TV Series" -> plugins.filter { p ->
                p.supportedTypes.any { it.contains("series", ignoreCase = true) || it.contains("tv", ignoreCase = true) }
            }
            "Anime" -> plugins.filter { p ->
                p.supportedTypes.any { it.contains("anime", ignoreCase = true) }
            }
            "Asian Dramas" -> plugins.filter { p ->
                p.supportedTypes.any { it.contains("drama", ignoreCase = true) || it.contains("asian", ignoreCase = true) }
            }
            else -> plugins
        }
    }

    val pageBg = if (isAmoled) PluginsGlassTokens.AmoledBackground else PluginsGlassTokens.BaseDarkBackground
    val repoTitle = repoIndex?.name ?: repoUrl.substringAfterLast("/").substringBefore(".json").ifBlank { "Repository" }
    val sanitizedUrl = remember(repoUrl) {
        repoUrl.removePrefix("https://").removePrefix("http://")
    }
    val canonicalRepoId = remember(repoUrl) { computeCanonicalRepoId(repoUrl) }

    fun isPluginInstalled(plugin: PluginManifest): Boolean {
        val cleanId = plugin.id.substringAfter(":")
        val namespacedId = "$canonicalRepoId:$cleanId"
        return installedPlugins.any { installed ->
            installed.id == namespacedId ||
            (installed.id.startsWith("$canonicalRepoId:") && installed.id.substringAfter(":") == cleanId) ||
            (installed.siteUrl.isNotBlank() && computeCanonicalRepoId(installed.siteUrl) == canonicalRepoId && installed.id.substringAfter(":") == cleanId) ||
            (installed.id == plugin.id && !installed.id.contains(":") && (installed.siteUrl == repoUrl || installed.siteUrl.isBlank()))
        }
    }

    fun findInstalledPlugin(plugin: PluginManifest): PluginManifest? {
        val cleanId = plugin.id.substringAfter(":")
        val namespacedId = "$canonicalRepoId:$cleanId"
        return installedPlugins.find { installed ->
            installed.id == namespacedId ||
            (installed.id.startsWith("$canonicalRepoId:") && installed.id.substringAfter(":") == cleanId) ||
            (installed.siteUrl.isNotBlank() && computeCanonicalRepoId(installed.siteUrl) == canonicalRepoId && installed.id.substringAfter(":") == cleanId) ||
            (installed.id == plugin.id && !installed.id.contains(":") && (installed.siteUrl == repoUrl || installed.siteUrl.isBlank()))
        }
    }

    val uninstalledCount = remember(plugins, installedPlugins, canonicalRepoId) {
        plugins.count { p -> !isPluginInstalled(p) }
    }
    val installedCount = plugins.size - uninstalledCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Back button, title block, bulk action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PluginsGlassCircleButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = repoTitle,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sanitizedUrl,
                        style = TextStyle(
                            color = PluginsGlassTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bulk Action
                if (isInstallingAll) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                            .background(PluginsGlassTokens.GlassFill)
                            .border(
                                PluginsGlassTokens.HairlineWidth,
                                PluginsGlassTokens.HairlineBorder,
                                RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = "Installing ${plugins.size - uninstalledCount} of ${plugins.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (uninstalledCount > 0) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                            .background(Color.White)
                            .pluginsPressable(scaleDown = 0.98f) {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                isInstallingAll = true
                                viewModel.installAllInRepo(repoUrl) { count ->
                                    isInstallingAll = false
                                    toastMessage = "Installed $count scrapers"
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Install all · $uninstalledCount",
                            color = PluginsGlassTokens.TextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (plugins.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                            .background(PluginsGlassTokens.GlassFill)
                            .border(
                                PluginsGlassTokens.HairlineWidth,
                                PluginsGlassTokens.HairlineBorder,
                                RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                            )
                            .pluginsPressable(scaleDown = 0.98f) {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                isInstallingAll = true
                                viewModel.installAllInRepo(repoUrl) { count ->
                                    isInstallingAll = false
                                    toastMessage = "Updated & reinstalled $count scrapers"
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Update all",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Category Filter Pills with 24dp soft right fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_FILTERS.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val pillBg = if (isSelected) Color.White else PluginsGlassTokens.GlassFill
                        val textColor = if (isSelected) PluginsGlassTokens.TextDark else Color.White.copy(alpha = 0.75f)

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(pillBg)
                                .then(
                                    if (!isSelected) Modifier.border(
                                        PluginsGlassTokens.HairlineWidth,
                                        PluginsGlassTokens.HairlineBorder,
                                        RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                    ) else Modifier
                                )
                                .pluginsPressable(scaleDown = 0.98f) {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Soft 24dp fade on right edge
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(24.dp)
                        .height(34.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, pageBg)
                            )
                        )
                )
            }

            // Muted count subtitle
            if (!isLoadingRepo && !loadError && plugins.isNotEmpty()) {
                Text(
                    text = "${plugins.size} scrapers · $installedCount installed",
                    style = TextStyle(
                        color = PluginsGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Body Area
            when {
                isLoadingRepo -> {
                    // 6 Skeleton shimmer rows with 1.6s animation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(6) {
                            RepoScraperSkeletonRow()
                        }
                    }
                }

                loadError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Failed to load repository",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            PluginsWhitePillButton(
                                text = "Retry",
                                onClick = { loadRepo() }
                            )
                        }
                    }
                }

                filteredPlugins.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scrapers available for $selectedCategory",
                            color = PluginsGlassTokens.TextTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                else -> {
                    // Hairline-divided scraper rows sitting directly on background
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(filteredPlugins.distinctBy { it.id }, key = { it.id }) { plugin ->
                            val isInstalled = isPluginInstalled(plugin)
                            val cleanId = plugin.id.substringAfter(":")
                            val namespacedId = "$canonicalRepoId:$cleanId"
                            val isInstalling = installingIds.contains(plugin.id) || installingIds.contains(namespacedId)

                            RepoScraperRow(
                                plugin = plugin,
                                isInstalled = isInstalled,
                                isInstalling = isInstalling,
                                onRowClick = {
                                    if (isInstalled) {
                                        alreadyInstalledAlert = "Scraper Already Installed" to "\"${plugin.name}\" (v${plugin.versionName.ifBlank { plugin.version.toString() }}) is already installed on your device."
                                    } else {
                                        selectedPluginForSheet = plugin
                                    }
                                },
                                onInstallClick = {
                                    if (isInstalled) {
                                        alreadyInstalledAlert = "Scraper Already Installed" to "\"${plugin.name}\" (v${plugin.versionName.ifBlank { plugin.version.toString() }}) is already installed on your device."
                                    } else {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                        viewModel.installPlugin(plugin) { success ->
                                            toastMessage = if (success) "Installed ${plugin.name}" else "Failed to install ${plugin.name}"
                                        }
                                    }
                                },
                                onUninstallClick = { pluginToUninstall = findInstalledPlugin(plugin) ?: plugin }
                            )

                            PluginsHairlineDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }

        // Glass Toast for feedback notifications
        PluginsGlassToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    // Provider Details Modal Bottom Sheet
    selectedPluginForSheet?.let { plugin ->
        val isInstalled = isPluginInstalled(plugin)
        val cleanId = plugin.id.substringAfter(":")
        val namespacedId = "$canonicalRepoId:$cleanId"
        val isInstalling = installingIds.contains(plugin.id) || installingIds.contains(namespacedId)
        val isTesting = testingIds.contains(plugin.id) || testingIds.contains(namespacedId)
        val health = healthMap[plugin.id] ?: healthMap[namespacedId]

        ProviderDetailSheet(
            plugin = plugin,
            isInstalled = isInstalled,
            isInstalling = isInstalling,
            isTesting = isTesting,
            health = health,
            onTest = { viewModel.testPlugin(plugin) },
            onInstall = {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                viewModel.installPlugin(plugin) { success ->
                    toastMessage = if (success) "Installed ${plugin.name}" else "Failed to install"
                }
            },
            onUninstall = {
                val p = findInstalledPlugin(plugin) ?: plugin
                viewModel.uninstallPlugin(p) {
                    toastMessage = "Uninstalled ${p.name}"
                }
            },
            onDismiss = { selectedPluginForSheet = null },
            hazeState = repoHazeState
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

    // Modal: Uninstall Confirmation Dialog
    pluginToUninstall?.let { plugin ->
        PluginsSharedGlassDialog(
            title = "Uninstall ${plugin.name}?",
            message = "This provider will be removed from your stream aggregator and will no longer query for links.",
            confirmText = "Uninstall",
            dismissText = "Cancel",
            onConfirm = {
                val p = findInstalledPlugin(plugin) ?: plugin
                pluginToUninstall = null
                viewModel.uninstallPlugin(p) {
                    toastMessage = "Uninstalled ${p.name}"
                }
            },
            onDismiss = { pluginToUninstall = null },
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
                            text = plugin.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "v${plugin.versionName.ifBlank { plugin.version.toString() }}",
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
 * Hairline-divided row sitting directly on background (no boxed card).
 */
@Composable
private fun RepoScraperRow(
    plugin: PluginManifest,
    isInstalled: Boolean,
    isInstalling: Boolean,
    onRowClick: () -> Unit,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pluginsPressable(scaleDown = 0.98f, onClick = onRowClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 40dp glass tile (outline puzzle icon: 100% white when installed, 50% when not)
        Box(
            modifier = Modifier
                .size(40.dp)
                .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Extension,
                contentDescription = null,
                tint = if (isInstalled) Color.White else Color.White.copy(alpha = 0.50f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = plugin.name,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Meta line: "English · v1.0.0" + glass "Installed" pill with check icon if installed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "English · v${plugin.versionName.ifBlank { plugin.version.toString() }}",
                    style = TextStyle(
                        color = PluginsGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                )

                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(PluginsGlassTokens.RadiusTiny))
                            .background(PluginsGlassTokens.GlassFill)
                            .border(
                                PluginsGlassTokens.HairlineWidth,
                                PluginsGlassTokens.HairlineBorder,
                                RoundedCornerShape(PluginsGlassTokens.RadiusTiny)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Installed",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (plugin.description.isNotBlank()) {
                Text(
                    text = plugin.description,
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

        // Trailing Action Button
        Crossfade(
            targetState = isInstalled to isInstalling,
            animationSpec = tween(durationMillis = 160),
            label = "scraperTrailingAction"
        ) { (installed, installing) ->
            if (installed) {
                // Installed: outline trash (44dp touch target)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .pluginsPressable(scaleDown = 0.98f, onClick = onUninstallClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Uninstall",
                        tint = PluginsGlassTokens.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (installing) {
                // Installing: glass circle with 18dp thin spinner
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pluginsRowGlass(shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            } else {
                // Not installed: 40dp solid white circle with outline download arrow
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .pluginsPressable(scaleDown = 0.98f, onClick = onInstallClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Install",
                        tint = PluginsGlassTokens.TextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Shimmer skeleton row for repository loading state.
 */
@Composable
private fun RepoScraperSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusIcon))
                .pluginsShimmer()
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .pluginsShimmer()
            )
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .pluginsShimmer()
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .pluginsShimmer()
        )
    }
}
