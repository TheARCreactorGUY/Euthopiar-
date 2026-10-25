package com.euthopiar.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.profile.AvatarWithThinRing
import com.euthopiar.ui.profile.ProfileGlassChip
import com.euthopiar.ui.profile.ProfileGlassPill
import com.euthopiar.ui.profile.ProfileGlassTokens
import com.euthopiar.ui.profile.ProfileManagementSheet
import com.euthopiar.ui.profile.SettingsHairlineRow
import com.euthopiar.ui.settings.models.SettingsCategory
import com.euthopiar.ui.settings.models.SettingsPage
import com.euthopiar.ui.settings.models.isVisibleFor
import com.euthopiar.ui.settings.search.SettingsSearchBar
import com.euthopiar.ui.settings.search.getAllSettingsSearchEntries
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

@Composable
fun SettingsRootScreen(
    onNavigateToPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    val activeProfile by profileManager.activeProfile.collectAsState()

    val isAmoled = LocalAmoledMode.current
    val reduceMotion = LocalReduceMotion.current

    var showProfileSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allSearchEntries = remember { getAllSettingsSearchEntries() }
    val filteredSearchEntries = remember(searchQuery, activeProfile) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            allSearchEntries.filter { entry ->
                entry.page.isVisibleFor(activeProfile) && entry.searchableText.contains(q)
            }
        }
    }

    // Motion stagger on first appearance only
    var hasAnimatedEnter by rememberSaveable { mutableStateOf(false) }
    var headerTriggered by remember { mutableStateOf(hasAnimatedEnter || reduceMotion) }

    LaunchedEffect(Unit) {
        if (!hasAnimatedEnter && !reduceMotion) {
            headerTriggered = true
            delay(240L + (7 * 40L) + 240L)
            hasAnimatedEnter = true
        }
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (headerTriggered || hasAnimatedEnter || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (headerTriggered || hasAnimatedEnter || reduceMotion) 0f else 8f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "headerTranslationY"
    )

    val backgroundColor = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground

    // Measured FloatingNavbar spacing:
    // FloatingNavbar pill height = 70.dp + vertical padding = 24.dp (94.dp total container height).
    // Plus system navigation bars inset + 24.dp breathing clearance.
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = 94.dp + 24.dp + navBarBottomInset

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (!isAmoled) {
                    Modifier.drawBehind {
                        val maxDim = maxOf(size.width, size.height)
                        // Static Indigo wash top-right (0 GPU draw passes during idle)
                        drawRect(
                            brush = Brush.radialGradient(
                                0.00f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.22f),
                                0.35f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.12f),
                                0.65f to ProfileGlassTokens.HazeIndigoBlob.copy(alpha = 0.03f),
                                1.00f to Color.Transparent,
                                center = Offset(size.width * 0.90f, size.height * 0.15f),
                                radius = maxDim * 0.90f
                            )
                        )
                    }
                } else Modifier
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = listBottomPadding
            )
        ) {
            // 1. Headline "Settings" (38 sp, medium, lineHeight 1.0, letterSpacing slightly negative, 22 dp side margin)
            item(key = "header_title") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp)
                        .padding(top = 28.dp, bottom = 18.dp)
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = headerTranslationY * density
                        }
                ) {
                    Text(
                        text = "Settings",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.03).sp,
                            color = Color.White
                        )
                    )
                }
            }

            // 2. Search Field (Full width, 46 dp tall, 14 dp radius, thin outline search icon)
            item(key = "header_search") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = headerTranslationY * density
                        }
                ) {
                    SettingsSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }
            }

            // Search Results Mode
            if (searchQuery.isNotBlank()) {
                if (filteredSearchEntries.isEmpty()) {
                    // Calm empty state
                    item(key = "search_empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No matching settings",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Try searching for playback, card scale, or plugins",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = ProfileGlassTokens.TextMuted50
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Hairline-divided search results matching row style
                    items(filteredSearchEntries, key = { "search_${it.id}" }) { entry ->
                        SettingsHairlineRow(
                            title = entry.title,
                            subtitle = "${entry.page.title} — ${entry.description}",
                            icon = entry.icon,
                            onClick = {
                                searchQuery = ""
                                onNavigateToPage(entry.page.route)
                            }
                        )
                        Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                            HorizontalDivider(
                                thickness = ProfileGlassTokens.HairlineWidthDp,
                                color = ProfileGlassTokens.HairlineDivider
                            )
                        }
                    }
                }
            } else {
                // Normal Hub Categories Mode

                // 3. Account Row (Hairline above and below, 50dp avatar in 0.5px white ring, 8dp radius glass chip, switch pill)
                item(key = "account_row") {
                    StaggeredEnterRow(
                        index = 0,
                        isAnimated = hasAnimatedEnter,
                        reduceMotion = reduceMotion
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hairline above
                            Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                                HorizontalDivider(
                                    thickness = ProfileGlassTokens.HairlineWidthDp,
                                    color = ProfileGlassTokens.HairlineDivider
                                )
                            }

                            // Account Row Content
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Button,
                                        onClickLabel = "Switch or manage accounts",
                                        onClick = { showProfileSheet = true }
                                    )
                                    .padding(horizontal = 22.dp, vertical = 16.dp)
                                    .tvFocusable(shape = RoundedCornerShape(12.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Profile avatar 50 dp surrounded by a thin ring of 0.5 px white at 30% with 4 dp gap (outer ~60 dp)
                                AvatarWithThinRing(
                                    avatarId = activeProfile.avatarId,
                                    avatarSize = 50.dp,
                                    ringGap = 4.dp,
                                    ringBorderWidth = 0.5.dp,
                                    ringColor = Color.White.copy(alpha = 0.30f)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Middle: Name at 18 sp medium + glass role chip + "Switch or manage accounts" line
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = activeProfile.name,
                                            style = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        ProfileGlassChip(
                                            text = if (activeProfile.isKids) "Kids" else activeProfile.type.displayName,
                                            showLockIcon = activeProfile.isPinLocked
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Switch or manage accounts",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = ProfileGlassTokens.TextMuted50
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Right: Glass pill button "Switch" (32 dp visual height, >= 44 dp touch area)
                                ProfileGlassPill(
                                    text = "Switch",
                                    onClick = { showProfileSheet = true },
                                    visualHeight = 32.dp,
                                    horizontalPadding = 14.dp,
                                    fontSize = 13
                                )
                            }

                            // Hairline below
                            Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                                HorizontalDivider(
                                    thickness = ProfileGlassTokens.HairlineWidthDp,
                                    color = ProfileGlassTokens.HairlineDivider
                                )
                            }

                            Spacer(modifier = Modifier.height(26.dp))
                        }
                    }
                }

                // 4. Settings Groups

                // Group 1: GENERAL
                item(key = "group_general_header") {
                    SettingsSectionHeader(title = SettingsCategory.General.title)
                }

                item(key = "row_playback") {
                    StaggeredEnterRow(index = 1, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                        SettingsHairlineRow(
                            title = SettingsPage.Playback.title,
                            subtitle = SettingsPage.Playback.subtitle,
                            icon = SettingsPage.Playback.icon,
                            onClick = { onNavigateToPage(SettingsPage.Playback.route) }
                        )
                    }
                    SettingsRowHairline()
                }

                item(key = "row_appearance") {
                    StaggeredEnterRow(index = 2, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                        SettingsHairlineRow(
                            title = SettingsPage.Appearance.title,
                            subtitle = SettingsPage.Appearance.subtitle,
                            icon = SettingsPage.Appearance.icon,
                            onClick = { onNavigateToPage(SettingsPage.Appearance.route) }
                        )
                    }
                    Spacer(modifier = Modifier.height(26.dp))
                }

                // Group 2: ECOSYSTEM AND PLUGINS (Hidden for Kids)
                if (SettingsPage.Plugins.isVisibleFor(activeProfile)) {
                    item(key = "group_ecosystem_header") {
                        SettingsSectionHeader(title = SettingsCategory.Ecosystem.title)
                    }

                    item(key = "row_integrations") {
                        StaggeredEnterRow(index = 3, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                            SettingsHairlineRow(
                                title = SettingsPage.Integrations.title,
                                subtitle = SettingsPage.Integrations.subtitle,
                                icon = SettingsPage.Integrations.icon,
                                onClick = { onNavigateToPage(SettingsPage.Integrations.route) }
                            )
                        }
                        SettingsRowHairline()
                    }

                    item(key = "row_plugins") {
                        StaggeredEnterRow(index = 4, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                            SettingsHairlineRow(
                                title = SettingsPage.Plugins.title,
                                subtitle = SettingsPage.Plugins.subtitle,
                                icon = SettingsPage.Plugins.icon,
                                onClick = { onNavigateToPage(SettingsPage.Plugins.route) }
                            )
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }

                // Group 3: ADVANCED
                item(key = "group_advanced_header") {
                    SettingsSectionHeader(title = SettingsCategory.Advanced.title)
                }

                item(key = "row_storage") {
                    StaggeredEnterRow(index = 5, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                        SettingsHairlineRow(
                            title = SettingsPage.Storage.title,
                            subtitle = SettingsPage.Storage.subtitle,
                            icon = SettingsPage.Storage.icon,
                            onClick = { onNavigateToPage(SettingsPage.Storage.route) }
                        )
                    }
                    Spacer(modifier = Modifier.height(26.dp))
                }

                // Group 4: ABOUT
                item(key = "group_about_header") {
                    SettingsSectionHeader(title = SettingsCategory.About.title)
                }

                item(key = "row_updates") {
                    StaggeredEnterRow(index = 6, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                        SettingsHairlineRow(
                            title = SettingsPage.Updates.title,
                            subtitle = SettingsPage.Updates.subtitle,
                            icon = SettingsPage.Updates.icon,
                            onClick = { onNavigateToPage(SettingsPage.Updates.route) }
                        )
                    }
                    SettingsRowHairline()
                }

                item(key = "row_about") {
                    StaggeredEnterRow(index = 7, isAnimated = hasAnimatedEnter, reduceMotion = reduceMotion) {
                        SettingsHairlineRow(
                            title = SettingsPage.About.title,
                            subtitle = SettingsPage.About.subtitle,
                            icon = SettingsPage.About.icon,
                            onClick = { onNavigateToPage(SettingsPage.About.route) }
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // App version caption
                item(key = "app_version_caption") {
                    val appVersionCaption = remember(context) {
                        try {
                            val ver = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                            "Euthopiar v$ver (Production)"
                        } catch (_: Exception) {
                            "Euthopiar (Production)"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appVersionCaption,
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = ProfileGlassTokens.TextMuted50.copy(alpha = 0.40f)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ProfileManagementSheet(
            onDismiss = { showProfileSheet = false }
        )
    }
}

/**
 * Section label: 11 sp, uppercase, wide letter spacing, white 45%, followed by hairline under it (~10 dp padding).
 * Filled icons next to section labels are removed.
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.08.sp,
                color = ProfileGlassTokens.TextLabel45
            ),
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            thickness = ProfileGlassTokens.HairlineWidthDp,
            color = ProfileGlassTokens.HairlineDivider
        )
    }
}

/**
 * Hairline separator between setting rows.
 */
@Composable
private fun SettingsRowHairline() {
    Box(modifier = Modifier.padding(horizontal = 22.dp)) {
        HorizontalDivider(
            thickness = ProfileGlassTokens.HairlineWidthDp,
            color = ProfileGlassTokens.HairlineDivider
        )
    }
}

/**
 * Staggered entrance animation container for setting rows.
 * Account row and first 6-8 rows stagger in with 40 ms delay between each (opacity 0 to 1, 8 dp up to 0, 240 ms, ease-out).
 * Does not replay when returning from subpages.
 */
@Composable
private fun StaggeredEnterRow(
    index: Int,
    isAnimated: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (reduceMotion || isAnimated) {
        Box(modifier = modifier) { content() }
    } else {
        var startAnim by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            startAnim = true
        }
        val animAlpha by animateFloatAsState(
            targetValue = if (startAnim) 1f else 0f,
            animationSpec = tween(
                durationMillis = 160,
                delayMillis = 30 + (index * 20),
                easing = FastOutSlowInEasing
            ),
            label = "staggerAlpha_$index"
        )
        val animTranslationY by animateFloatAsState(
            targetValue = if (startAnim) 0f else 8f,
            animationSpec = tween(
                durationMillis = 160,
                delayMillis = 30 + (index * 20),
                easing = FastOutSlowInEasing
            ),
            label = "staggerTrans_$index"
        )
        Box(
            modifier = modifier.graphicsLayer {
                alpha = animAlpha
                translationY = animTranslationY * density
            }
        ) {
            content()
        }
    }
}
