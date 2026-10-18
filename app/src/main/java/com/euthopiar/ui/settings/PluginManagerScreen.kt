package com.euthopiar.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.euthopiar.app.plugin.PluginRepositoryService
import com.euthopiar.core.model.PluginManifest
import com.euthopiar.ui.components.*
import com.euthopiar.ui.settings.plugins.*
import com.euthopiar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    onBack: () -> Unit,
    viewModel: PluginManagerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarController = LocalSnackbarController.current
    val clipboardManager = LocalClipboardManager.current

    val selectedTab by viewModel.selectedTab.collectAsState()
    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val communityPlugins by viewModel.communityPlugins.collectAsState()
    val providerRepos by viewModel.providerRepos.collectAsState()
    val selectedProviderRepo by viewModel.selectedProviderRepo.collectAsState()
    val repositoryUrls by viewModel.repositoryUrls.collectAsState()
    val installingIds by viewModel.installingPluginIds.collectAsState()
    val activePluginName by viewModel.activePluginName.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val testingIds by viewModel.testingPluginIds.collectAsState()
    val healthMap by viewModel.pluginHealthMap.collectAsState()

    var showAddSourceSheet by remember { mutableStateOf(false) }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var repoUrlInput by remember { mutableStateOf("") }
    var repoError by remember { mutableStateOf<String?>(null) }
    var isAddingRepo by remember { mutableStateOf(false) }

    val windowDimensions = rememberWindowDimensions()

    val jsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.installFromUri(uri) { success, nameOrMsg ->
                val msg = if (success) "Installed $nameOrMsg successfully" else "Installation failed: $nameOrMsg"
                snackbarController.show(msg)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Clean Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = windowDimensions.horizontalMargin, vertical = Dimens.spacing12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .pressable { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = "Sources",
                        style = AppTypography.titleLarge,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4)
                ) {
                    IconButton(
                        onClick = { viewModel.loadPlugins() },
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .pressable { viewModel.loadPlugins() }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAddSourceSheet = true },
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .pressable { showAddSourceSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Source",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = windowDimensions.horizontalMargin),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacing12)
            ) {
                item {
                    AppFilterChip(
                        text = "Installed (${installedPlugins.size})",
                        selected = selectedTab == PluginManagerViewModel.Tab.INSTALLED,
                        onClick = { viewModel.selectTab(PluginManagerViewModel.Tab.INSTALLED) }
                    )
                }
                item {
                    AppFilterChip(
                        text = "Community (${communityPlugins.size})",
                        selected = selectedTab == PluginManagerViewModel.Tab.MARKETPLACE,
                        onClick = { viewModel.selectTab(PluginManagerViewModel.Tab.MARKETPLACE) }
                    )
                }
                item {
                    AppFilterChip(
                        text = "Repositories (${repositoryUrls.size})",
                        selected = selectedTab == PluginManagerViewModel.Tab.REPOSITORIES,
                        onClick = { viewModel.selectTab(PluginManagerViewModel.Tab.REPOSITORIES) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                PluginManagerViewModel.Tab.INSTALLED -> {
                    InstalledTabContent(
                        installedPlugins = installedPlugins,
                        communityPlugins = communityPlugins,
                        installingIds = installingIds,
                        activePluginName = activePluginName,
                        testingIds = testingIds,
                        healthMap = healthMap,
                        horizontalMargin = windowDimensions.horizontalMargin,
                        onSetActive = { viewModel.setActivePlugin(it.name) },
                        onToggleEnabled = { viewModel.togglePluginEnabled(it) },
                        onTest = { manifest ->
                            viewModel.testPlugin(manifest) { isWorking, msg ->
                                snackbarController.show("${manifest.name}: $msg")
                            }
                        },
                        onInstall = { plugin ->
                            viewModel.installPlugin(plugin) { success ->
                                val msg = if (success) "Updated ${plugin.name} successfully" else "Failed to update ${plugin.name}"
                                snackbarController.show(msg)
                            }
                        },
                        onUninstall = { plugin ->
                            viewModel.uninstallPlugin(plugin) { success ->
                                val msg = if (success) "Uninstalled ${plugin.name}" else "Failed to uninstall"
                                snackbarController.show(msg)
                            }
                        }
                    )
                }

                PluginManagerViewModel.Tab.MARKETPLACE -> {
                    MarketplaceTabContent(
                        providerRepos = providerRepos,
                        selectedRepo = selectedProviderRepo,
                        onSelectRepo = { viewModel.selectProviderRepo(it) },
                        communityPlugins = communityPlugins,
                        installedPlugins = installedPlugins,
                        installingIds = installingIds,
                        horizontalMargin = windowDimensions.horizontalMargin,
                        onInstall = { plugin ->
                            viewModel.installPlugin(plugin) { success ->
                                val msg = if (success) "Installed ${plugin.name} successfully" else "Failed to install ${plugin.name}"
                                snackbarController.show(msg)
                            }
                        },
                        onSyncCommunity = {
                            viewModel.syncCommunityRepository { success, message ->
                                snackbarController.show(message ?: if (success) "Community sources synced" else "Sync failed")
                            }
                        },
                        onAddRepo = { showAddRepoDialog = true },
                        onInstallAllInRepo = { repoUrl ->
                            viewModel.installAllInRepo(repoUrl) { count ->
                                snackbarController.show("Installed $count plugins")
                            }
                        }
                    )
                }

                PluginManagerViewModel.Tab.REPOSITORIES -> {
                    RepositoriesTabContent(
                        repositoryUrls = repositoryUrls,
                        horizontalMargin = windowDimensions.horizontalMargin,
                        onRemoveRepo = { viewModel.removeRepository(it) },
                        onAddRepo = {
                            repoUrlInput = ""
                            repoError = null
                            showAddRepoDialog = true
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet: Add Source Options
    if (showAddSourceSheet) {
        val isAmoled = LocalAmoledMode.current
        val isLowEnd = remember(context) { context.isLowEndDevice() }
        val sheetBg = when {
            isLowEnd -> PluginsGlassTokens.SheetGlassFallback
            isAmoled -> PluginsGlassTokens.SheetGlassAmoledFill
            else -> PluginsGlassTokens.SheetGlassFill
        }

        ModalBottomSheet(
            onDismissRequest = { showAddSourceSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            containerColor = sheetBg,
            scrimColor = Color.Black.copy(alpha = 0.60f),
            dragHandle = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PluginsHairlineDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.30f))
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Add source",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Connect or import new provider catalogs and sources",
                        style = androidx.compose.ui.text.TextStyle(
                            color = PluginsGlassTokens.TextSheetBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: Install from File
                SourceOptionItem(
                    icon = Icons.Outlined.FileUpload,
                    title = "Install from file",
                    subtitle = "Load a JavaScript (.js) provider from device storage",
                    onClick = {
                        showAddSourceSheet = false
                        jsFilePickerLauncher.launch("*/*")
                    }
                )

                PluginsHairlineDivider()

                // Option 2: Add catalog link
                SourceOptionItem(
                    icon = Icons.Outlined.Link,
                    title = "Add catalog link",
                    subtitle = "Connect a source catalog from a web link",
                    onClick = {
                        showAddSourceSheet = false
                        repoUrlInput = ""
                        repoError = null
                        showAddRepoDialog = true
                    }
                )

                PluginsHairlineDivider()

                // Option 3: Sync Community Sources
                SourceOptionItem(
                    icon = Icons.Outlined.CloudDownload,
                    title = "Sync community sources",
                    subtitle = "Download available sources from the community catalog",
                    onClick = {
                        showAddSourceSheet = false
                        viewModel.fetchTestEupsRepository { success, message ->
                            snackbarController.show(message ?: if (success) "Community sources synced" else "Sync failed")
                        }
                    }
                )
            }
        }
    }

    // Add Repository Glass Dialog
    if (showAddRepoDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isAddingRepo) showAddRepoDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = !isAddingRepo,
                dismissOnClickOutside = !isAddingRepo
            )
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .pluginsGlassSurface(
                        shape = RoundedCornerShape(PluginsGlassTokens.RadiusDialog),
                        isSheetOrDialog = true
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add catalog link",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Text(
                        text = "Enter the catalog web link to discover and install community sources:",
                        style = androidx.compose.ui.text.TextStyle(
                            color = PluginsGlassTokens.TextSheetBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    )

                    // Quick chip for community repository
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusInput))
                            .pluginsPressable(scaleDown = 0.98f) {
                                repoUrlInput = PluginRepositoryService.TEST_EUPS_REPO_URL
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Use All-in-One community catalog",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Official catalog for tested sources",
                                    color = PluginsGlassTokens.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Quick chip for secondary repository
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusInput))
                            .pluginsPressable(scaleDown = 0.98f) {
                                repoUrlInput = PluginRepositoryService.SECONDARY_REPO_URL
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Use Yoru's Providers catalog",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Yoruix community scrapers",
                                    color = PluginsGlassTokens.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    PluginsGlassInputField(
                        value = repoUrlInput,
                        onValueChange = {
                            repoUrlInput = it
                            repoError = null
                        },
                        placeholder = "https://example.com/sources.json",
                        onPaste = {
                            val text = clipboardManager.getText()?.text
                            if (!text.isNullOrBlank()) {
                                repoUrlInput = text.trim()
                            }
                        },
                        onClear = { repoUrlInput = "" },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (repoError != null) {
                        Text(
                            text = repoError ?: "",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // White Cancel pill on the left
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(Color.White)
                                .pluginsPressable(enabled = !isAddingRepo, scaleDown = 0.98f) {
                                    showAddRepoDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = PluginsGlassTokens.TextDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Connect button on the right (white when URL valid, glass when empty)
                        val isReady = repoUrlInput.isNotBlank()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(PluginsGlassTokens.RadiusPill))
                                .background(if (isReady) Color.White else PluginsGlassTokens.GlassFill)
                                .then(
                                    if (!isReady) Modifier.border(
                                        PluginsGlassTokens.HairlineWidth,
                                        PluginsGlassTokens.HairlineBorder,
                                        RoundedCornerShape(PluginsGlassTokens.RadiusPill)
                                    ) else Modifier
                                )
                                .pluginsPressable(enabled = !isAddingRepo, scaleDown = 0.98f) {
                                    if (repoUrlInput.isBlank()) {
                                        repoError = "Link cannot be empty"
                                        return@pluginsPressable
                                    }
                                    isAddingRepo = true
                                    viewModel.addRepository(repoUrlInput) { success, message ->
                                        isAddingRepo = false
                                        if (success) {
                                            showAddRepoDialog = false
                                            snackbarController.show(message ?: "Catalog added")
                                        } else {
                                            repoError = message ?: "Failed to verify catalog"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAddingRepo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isReady) PluginsGlassTokens.TextDark else Color.White
                                )
                            } else {
                                Text(
                                    text = "Connect",
                                    color = if (isReady) PluginsGlassTokens.TextDark else PluginsGlassTokens.TextTertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pluginsPressable(scaleDown = 0.98f, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .pluginsRowGlass(shape = RoundedCornerShape(PluginsGlassTokens.RadiusIcon)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = PluginsGlassTokens.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Tab 1: Installed Sources Content
 */
@Composable
private fun InstalledTabContent(
    installedPlugins: List<PluginManifest>,
    communityPlugins: List<PluginManifest>,
    installingIds: Set<String> = emptySet(),
    activePluginName: String,
    testingIds: Set<String>,
    healthMap: Map<String, PluginManagerViewModel.PluginHealth>,
    horizontalMargin: androidx.compose.ui.unit.Dp,
    onSetActive: (PluginManifest) -> Unit,
    onToggleEnabled: (PluginManifest) -> Unit,
    onTest: (PluginManifest) -> Unit,
    onInstall: ((PluginManifest) -> Unit)? = null,
    onUninstall: (PluginManifest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = Dimens.spacing8,
            bottom = 48.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
    ) {
        items(installedPlugins, key = { it.id }) { manifest ->
            val isActive = activePluginName.equals(manifest.name, ignoreCase = true)
            val isTesting = manifest.id in testingIds
            val health = healthMap[manifest.id]
            val community = communityPlugins.find { it.id == manifest.id }
            val hasUpdate = community != null && community.version > manifest.version
            val isInstalling = manifest.id in installingIds || (community != null && community.id in installingIds)

            InstalledSourceCard(
                manifest = manifest,
                isActive = isActive,
                isTesting = isTesting,
                isInstalling = isInstalling,
                health = health,
                hasUpdate = hasUpdate,
                onSetActive = { onSetActive(manifest) },
                onTest = { onTest(manifest) },
                onToggleEnabled = { onToggleEnabled(manifest) },
                onUpdate = if (hasUpdate && community != null && onInstall != null) { { onInstall?.invoke(community) } } else null,
                onUninstall = if (!manifest.isBuiltIn) { { onUninstall(manifest) } } else null
            )
        }
    }
}

/**
 * Card for an installed source
 */
@Composable
private fun InstalledSourceCard(
    manifest: PluginManifest,
    isActive: Boolean,
    isTesting: Boolean,
    isInstalling: Boolean = false,
    health: PluginManagerViewModel.PluginHealth?,
    hasUpdate: Boolean,
    onSetActive: () -> Unit,
    onTest: () -> Unit,
    onToggleEnabled: () -> Unit,
    onUpdate: (() -> Unit)? = null,
    onUninstall: (() -> Unit)?
) {
    val isNotWorking = health?.isWorking == false

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeM)
            .background(SurfaceLow)
            .border(
                width = if (isActive) 1.5.dp else HairlineWidth,
                color = when {
                    isNotWorking -> Outline
                    isActive -> Color.White.copy(alpha = 0.5f)
                    else -> Outline
                },
                shape = ShapeM
            )
            .padding(Dimens.spacing16)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isNotWorking -> Color.White.copy(alpha = 0.4f)
                                    isActive -> Color.White
                                    else -> TextTertiary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = manifest.name,
                        style = AppTypography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = "v${manifest.versionName}",
                        style = AppTypography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    // Language badge
                    Box(
                        modifier = Modifier
                            .clip(ShapeFull)
                            .background(SurfaceMid)
                            .border(HairlineWidth, Outline, ShapeFull)
                            .padding(horizontal = Dimens.spacing8, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EN",
                            style = AppTypography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    if (hasUpdate) {
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Surface(
                            shape = ShapeFull,
                            color = PluginsGlassTokens.GlassFill,
                            border = androidx.compose.foundation.BorderStroke(HairlineWidth, Outline)
                        ) {
                            Text(
                                text = "Update available",
                                style = AppTypography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = Dimens.spacing8, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4)
                ) {
                    // Test Action
                    if (isTesting) {
                        Box(
                            modifier = Modifier.size(Dimens.minTouchTarget),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onTest,
                            modifier = Modifier.tvFocusable(shape = ShapeFull)
                        ) {
                            Text(
                                text = "Test",
                                style = AppTypography.labelMedium,
                                color = if (isNotWorking) Color.White.copy(alpha = 0.60f) else TextSecondary
                            )
                        }
                    }

                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else if (hasUpdate && onUpdate != null) {
                        Button(
                            onClick = onUpdate,
                            shape = ShapeFull,
                            contentPadding = PaddingValues(horizontal = Dimens.spacing12, vertical = Dimens.spacing4),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PluginsGlassTokens.TextDark
                            ),
                            modifier = Modifier.tvFocusable(shape = ShapeFull)
                        ) {
                            Text(
                                text = "Update",
                                style = AppTypography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isActive) {
                        Surface(
                            shape = ShapeFull,
                            color = PluginsGlassTokens.GlassFill,
                            border = androidx.compose.foundation.BorderStroke(HairlineWidth, Outline)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing4)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Active",
                                    style = AppTypography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        TextButton(
                            onClick = onSetActive,
                            modifier = Modifier.tvFocusable(shape = ShapeFull)
                        ) {
                            Text(
                                text = "Use",
                                style = AppTypography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    if (onUninstall != null) {
                        IconButton(
                            onClick = onUninstall,
                            modifier = Modifier
                                .size(Dimens.minTouchTarget)
                                .tvFocusable(shape = ShapeFull)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Uninstall",
                                tint = PluginsGlassTokens.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Health Status row if tested
            if (health != null) {
                Spacer(modifier = Modifier.height(Dimens.spacing4))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing4)
                ) {
                    if (isNotWorking) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Not working",
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Not working",
                            style = AppTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.60f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Working",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Working",
                            style = AppTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            if (manifest.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimens.spacing8))
                Text(
                    text = manifest.description,
                    style = AppTypography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Text(
                text = "Last updated: v${manifest.versionName}",
                style = AppTypography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

/**
 * Tab 2: Community Sources Content with Provider Box Hierarchy
 */
@Composable
private fun MarketplaceTabContent(
    providerRepos: List<PluginManagerViewModel.ProviderRepoInfo>,
    selectedRepo: PluginManagerViewModel.ProviderRepoInfo?,
    onSelectRepo: (PluginManagerViewModel.ProviderRepoInfo?) -> Unit,
    communityPlugins: List<PluginManifest>,
    installedPlugins: List<PluginManifest>,
    installingIds: Set<String>,
    horizontalMargin: androidx.compose.ui.unit.Dp,
    onInstall: (PluginManifest) -> Unit,
    onSyncCommunity: () -> Unit,
    onAddRepo: () -> Unit,
    onInstallAllInRepo: ((String) -> Unit)? = null
) {
    if (selectedRepo == null) {
        // LEVEL 1: PROVIDER BOXES OVERVIEW
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalMargin,
                end = horizontalMargin,
                top = Dimens.spacing8,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {
            item {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeM)
                        .background(SurfaceMid)
                        .border(HairlineWidth, Outline, ShapeM)
                        .padding(Dimens.spacing16)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Provider Repositories",
                                style = AppTypography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Select a provider box to browse its plugins and sources",
                                style = AppTypography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = onSyncCommunity,
                            modifier = Modifier
                                .size(Dimens.minTouchTarget)
                                .tvFocusable(shape = ShapeFull)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sync",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (providerRepos.isEmpty()) {
                item {
                    EmptyState(
                        title = "No provider repositories found",
                        message = "Tap Sync to load official git provider repositories.",
                        actionLabel = "Sync Now",
                        onActionClick = onSyncCommunity,
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacing24)
                    )
                }
            } else {
                items(providerRepos, key = { it.url }) { repo ->
                    ProviderBoxCard(
                        repo = repo,
                        onClick = { onSelectRepo(repo) }
                    )
                }
            }
        }
    } else {
        // LEVEL 2: PLUGINS LIST FOR SELECTED PROVIDER
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalMargin,
                end = horizontalMargin,
                top = Dimens.spacing8,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {
            item {
                // Navigation Header for Provider Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeM)
                        .background(SurfaceMid)
                        .border(HairlineWidth, Outline, ShapeM)
                        .padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSelectRepo(null) },
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .pressable { onSelectRepo(null) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Providers",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimens.spacing8))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedRepo.name,
                            style = AppTypography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedRepo.plugins.size} plugins available from this repository",
                            style = AppTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    val uninstalledCount = selectedRepo.plugins.count { p -> installedPlugins.none { it.id == p.id } }
                    if (uninstalledCount > 0) {
                        TextButton(
                            onClick = { onInstallAllInRepo?.invoke(selectedRepo.url) }
                        ) {
                            Text(
                                text = "Install All ($uninstalledCount)",
                                color = Color.White,
                                style = AppTypography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (selectedRepo.plugins.isEmpty()) {
                item {
                    EmptyState(
                        title = "No plugins in this provider",
                        message = "This repository has no public plugins available.",
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacing24)
                    )
                }
            } else {
                items(selectedRepo.plugins, key = { it.id }) { manifest ->
                    val isInstalling = manifest.id in installingIds
                    val installed = installedPlugins.find { it.id.equals(manifest.id, ignoreCase = true) }
                    val isInstalled = installed != null
                    val hasUpdate = isInstalled && isVersionNewer(manifest.versionName, installed?.versionName ?: "")

                    CommunitySourceCard(
                        manifest = manifest,
                        isInstalling = isInstalling,
                        isInstalled = isInstalled,
                        hasUpdate = hasUpdate,
                        onInstall = { onInstall(manifest) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderBoxCard(
    repo: PluginManagerViewModel.ProviderRepoInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeM)
            .background(SurfaceLow)
            .border(HairlineWidth, Outline, ShapeM)
            .tvFocusable(shape = ShapeM)
            .pressable { onClick() }
            .padding(Dimens.spacing16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Provider Box Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(ShapeM)
                        .background(SurfaceMid)
                        .border(HairlineWidth, Outline, ShapeM),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.spacing12))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = repo.name,
                            style = AppTypography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Box(
                            modifier = Modifier
                                .clip(ShapeFull)
                                .background(PluginsGlassTokens.GlassFill)
                                .border(HairlineWidth, Outline, ShapeFull)
                                .padding(horizontal = Dimens.spacing8, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Verified",
                                style = AppTypography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.spacing4))

                    Text(
                        text = repo.description.ifBlank { "Git provider repository" },
                        style = AppTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacing8))

                    Box(
                        modifier = Modifier
                            .clip(ShapeFull)
                            .background(PluginsGlassTokens.GlassFill)
                            .border(HairlineWidth, Outline, ShapeFull)
                            .padding(horizontal = Dimens.spacing8, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${repo.plugins.size} Plugins Available",
                            style = AppTypography.labelSmall,
                            color = PluginsGlassTokens.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(Dimens.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Plugins",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CommunitySourceCard(
    manifest: PluginManifest,
    isInstalling: Boolean,
    isInstalled: Boolean = false,
    hasUpdate: Boolean = false,
    onInstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeM)
            .background(SurfaceLow)
            .border(HairlineWidth, Outline, ShapeM)
            .padding(Dimens.spacing16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = manifest.name,
                        style = AppTypography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = "v${manifest.versionName}",
                        style = AppTypography.labelSmall,
                        color = TextTertiary
                    )
                    if (manifest.supportedTypes.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Box(
                            modifier = Modifier
                                .clip(ShapeFull)
                                .background(SurfaceMid)
                                .border(HairlineWidth, Outline, ShapeFull)
                                .padding(horizontal = Dimens.spacing8, vertical = 2.dp)
                        ) {
                            Text(
                                text = manifest.supportedTypes.joinToString("/").uppercase(),
                                style = AppTypography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (manifest.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    Text(
                        text = manifest.description,
                        style = AppTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacing8))
                Text(
                    text = if (isInstalled) "Installed • v${manifest.versionName}" else "Version: v${manifest.versionName}",
                    style = AppTypography.labelSmall,
                    color = if (hasUpdate) Color.White else TextTertiary
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacing12))

            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                val buttonText = when {
                    hasUpdate -> "Update"
                    isInstalled -> "Reinstall"
                    else -> "Get"
                }
                val btnColor = when {
                    hasUpdate -> Color.White
                    isInstalled -> SurfaceHigh
                    else -> TextPrimary
                }
                val contentColor = when {
                    hasUpdate -> PluginsGlassTokens.TextDark
                    isInstalled -> TextPrimary
                    else -> SurfaceLow
                }
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = btnColor,
                        contentColor = contentColor
                    ),
                    shape = ShapeFull,
                    contentPadding = PaddingValues(horizontal = Dimens.spacing16, vertical = Dimens.spacing8),
                    modifier = Modifier.tvFocusable(shape = ShapeFull)
                ) {
                    Text(text = buttonText, style = AppTypography.labelMedium)
                }
            }
        }
    }
}

private fun isVersionNewer(remote: String, installed: String): Boolean {
    val rParts = remote.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val iParts = installed.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    if (rParts.isEmpty()) return false
    val maxLen = maxOf(rParts.size, iParts.size)
    for (idx in 0 until maxLen) {
        val r = rParts.getOrElse(idx) { 0 }
        val i = iParts.getOrElse(idx) { 0 }
        if (r > i) return true
        if (r < i) return false
    }
    return false
}

/**
 * Tab 3: Repositories Content
 */
@Composable
private fun RepositoriesTabContent(
    repositoryUrls: Set<String>,
    horizontalMargin: androidx.compose.ui.unit.Dp,
    onRemoveRepo: (String) -> Unit,
    onAddRepo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = Dimens.spacing8,
            bottom = 48.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
    ) {
        items(repositoryUrls.toList(), key = { it }) { url ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeM)
                    .background(SurfaceLow)
                    .border(HairlineWidth, Outline, ShapeM)
                    .padding(Dimens.spacing16)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = url,
                            style = AppTypography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onRemoveRepo(url) },
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .pressable { onRemoveRepo(url) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove",
                            tint = PluginsGlassTokens.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            SecondaryButton(
                text = "Add Repository",
                icon = Icons.Filled.Add,
                onClick = onAddRepo,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
