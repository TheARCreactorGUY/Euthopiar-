package com.euthopiar.ui.settings.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.data.update.AppUpdateConfig
import com.euthopiar.ui.settings.components.*
import com.euthopiar.ui.theme.AppFontFamily

/**
 * Redesigned "About Euthopiar" Settings Screen.
 *
 * Strict High-Fidelity Design Specifications:
 * - Editorial Top Bar with 40x40dp glass back button & crossfading collapsed title.
 * - Bold 32sp Medium headline ("About Euthopiar").
 * - Hero Brand Block: 52x52dp glass emblem with 24dp pure white outline heart,
 *   EUTHOPIAR brand typography, and dynamic version glass chip.
 * - Open hairline-divided sections (COMMUNITY & SOURCE, ENGINE & BUILD STACK) with zero boxed cards.
 * - Non-colliding right-side glass chips ("Media3", "Compose", "Multi-device") with min 16dp spacing.
 * - Pure monochrome aesthetics, 98% (0.98x) tactile press scale, and accessibility font scaling.
 */
@Composable
fun AboutSettingsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToUpdates: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val versionName = remember(context) {
        try {
            val name = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            "v$name · Production"
        } catch (_: Exception) {
            "v1.0.0 · Production"
        }
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    SettingsScaffold(
        title = "About Euthopiar",
        onBack = onBack,
        modifier = modifier
    ) { hazeState ->

        // Hero Brand Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 22.dp)
                .drawBehind {
                    // 0.5dp white 14% hairline divider along the bottom edge
                    drawLine(
                        color = SettingsTokens.HairlineDivider,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = SettingsTokens.HairlineWidth.toPx()
                    )
                }
                .padding(bottom = 20.dp)
        ) {
            // Emblem Tile + Brand Title Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 52x52dp Glass Emblem Tile
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusTileEmblem),
                            hazeState = hazeState,
                            blurRadius = 18.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Brand Title Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "EUTHOPIAR",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            letterSpacing = (20 * 0.08).sp,
                            color = Color.White
                        )
                    )

                    Text(
                        text = "The modern fluid streaming experience",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SettingsTokens.TextWhite54
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsMetadataChip(
                    text = versionName,
                    hazeState = hazeState
                )
                SettingsMetadataChip(
                    text = "Build 2026.10",
                    hazeState = hazeState
                )
            }
        }

        // Section 1: Community & Source
        OpenSettingsSectionHeader(title = "Community & Source")

        OpenSettingsRow(
            title = "Application Updates",
            subtitle = "Check for newer releases and view changelog",
            onClick = { onNavigateToUpdates?.invoke() },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SettingsTokens.TextWhite42,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        OpenSettingsRow(
            title = "GitHub Repository",
            subtitle = "Source code, issue tracking & release notes",
            onClick = { openUrl(AppUpdateConfig.GITHUB_REPO_URL) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SettingsTokens.TextWhite42,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        OpenSettingsRow(
            title = "Open-Source Licenses",
            subtitle = "ExoPlayer, Jetpack Compose, Coil, Haze, Material3",
            onClick = { showLicensesDialog = true },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SettingsTokens.TextWhite42,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        OpenSettingsRow(
            title = "Privacy Architecture",
            subtitle = "Zero-telemetry, on-device encrypted keys, no central logs",
            onClick = { showPrivacyDialog = true },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SettingsTokens.TextWhite42,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Section 2: Engine & Build Stack
        OpenSettingsSectionHeader(title = "Engine & Build Stack")

        OpenSettingsRow(
            title = "Media Engine",
            subtitle = "Media3 ExoPlayer v1.5 with hardware GPU surface tunneling",
            trailingContent = {
                SettingsMetadataChip(
                    text = "Media3",
                    hazeState = hazeState
                )
            }
        )

        OpenSettingsRow(
            title = "UI Toolkit",
            subtitle = "100% Kotlin Jetpack Compose Material3 with fluid springs",
            trailingContent = {
                SettingsMetadataChip(
                    text = "Compose",
                    hazeState = hazeState
                )
            }
        )

        OpenSettingsRow(
            title = "Target Platform",
            subtitle = "Universal responsive support for Phone, Tablet, Foldable & Android TV",
            trailingContent = {
                SettingsMetadataChip(
                    text = "Multi-device",
                    hazeState = hazeState
                )
            }
        )

        // In-App Open-Source Licenses Glass Dialog
        if (showLicensesDialog) {
            Dialog(
                onDismissRequest = { showLicensesDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusDialog),
                            hazeState = hazeState,
                            blurRadius = 22.dp
                        )
                        .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open-Source Licenses",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = SettingsTokens.TextWhite96
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Third-Party Libraries & Notices",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = SettingsTokens.TextWhite54
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Euthopiar is crafted with modern open-source software, licensed under permissive Apache 2.0 and MIT terms:",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = SettingsTokens.TextWhite72
                            )
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Jetpack Compose & Material 3" to "Google LLC • Apache License 2.0",
                                "Media3 ExoPlayer v1.5" to "Google LLC • Apache License 2.0",
                                "Haze Glass Blur Library" to "Chris Banes • Apache License 2.0",
                                "Coil 3 Async Image Loader" to "Coil Contributors • Apache License 2.0",
                                "KotlinX Coroutines & Serialization" to "JetBrains s.r.o. • Apache License 2.0",
                                "OkHttp HTTP/3 Client" to "Square, Inc. • Apache License 2.0"
                            ).forEach { (lib, license) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = lib,
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = SettingsTokens.TextWhite96
                                            )
                                        )
                                        Text(
                                            text = license,
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = SettingsTokens.TextWhite45
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsPillButton(
                                text = "View on GitHub",
                                isSolidWhite = false,
                                onClick = {
                                    try {
                                        openUrl("${AppUpdateConfig.GITHUB_REPO_URL}/blob/main/LICENSE")
                                    } catch (_: Exception) {}
                                },
                                hazeState = hazeState
                            )

                            SettingsPillButton(
                                text = "Close",
                                isSolidWhite = true,
                                onClick = { showLicensesDialog = false }
                            )
                        }
                    }
                }
            }
        }

        // In-App Privacy Architecture Glass Dialog
        if (showPrivacyDialog) {
            Dialog(
                onDismissRequest = { showPrivacyDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .settingsGlass(
                            shape = RoundedCornerShape(SettingsTokens.RadiusDialog),
                            hazeState = hazeState,
                            blurRadius = 22.dp
                        )
                        .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusDialog))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(SettingsTokens.HairlineWidth, SettingsTokens.HairlineBorder, RoundedCornerShape(SettingsTokens.RadiusControl)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Privacy Architecture",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = SettingsTokens.TextWhite96
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Zero Telemetry & Local Sovereign",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = SettingsTokens.TextWhite54
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Euthopiar is built around strict data isolation and user privacy principles:",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = SettingsTokens.TextWhite72
                            )
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Zero Telemetry" to "No tracking SDKs, no behavioral events, and no remote crash loggers.",
                                "Local-Only Database" to "Watch history, bookmarks, and playlists live strictly on your device.",
                                "Direct Cloud Auth" to "Credentials connect point-to-point to Trakt, Simkl, and Debrid over HTTPS.",
                                "On-Device Isolated Storage" to "Sensitive API keys and preferences live strictly in sandboxed on-device DataStore storage."
                            ).forEach { (heading, desc) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(SettingsTokens.RadiusControl))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = heading,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = SettingsTokens.TextWhite96
                                        )
                                    )
                                    Text(
                                        text = desc,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = SettingsTokens.TextWhite54
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsPillButton(
                                text = "Read PRIVACY.md",
                                isSolidWhite = false,
                                onClick = {
                                    try {
                                        openUrl("${AppUpdateConfig.GITHUB_REPO_URL}/blob/main/PRIVACY.md")
                                    } catch (_: Exception) {}
                                },
                                hazeState = hazeState
                            )

                            SettingsPillButton(
                                text = "Got it",
                                isSolidWhite = true,
                                onClick = { showPrivacyDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
