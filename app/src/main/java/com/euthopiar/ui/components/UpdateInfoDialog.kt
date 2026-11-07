package com.euthopiar.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.euthopiar.data.update.ApkDownloadState
import com.euthopiar.data.update.AppUpdateManager
import com.euthopiar.data.update.ReleaseInfo
import com.euthopiar.data.update.isRemoteVersionNewer
import com.euthopiar.ui.navigation.LocalNavbarVisible
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Parsed node representation of a release changelog entry.
 */
sealed interface ChangelogNode {
    data class Header(val title: String) : ChangelogNode
    data class FeatureBullet(val title: String?, val description: String) : ChangelogNode
    data class Paragraph(val text: String) : ChangelogNode
}

/**
 * Parsed changelog containing extracted release photos (1-2 images) and structured text nodes.
 */
data class ParsedChangelog(
    val photos: List<String>,
    val nodes: List<ChangelogNode>
)

/**
 * Intelligently parses raw GitHub release markdown:
 * 1. Extracts photos (markdown images, HTML img tags, standalone image URLs).
 * 2. Structures headers (## Top Features, ## What's New).
 * 3. Highlights bullet headlines (**Title:** Description) in bold with clean spacing.
 */
fun parseReleaseChangelog(rawText: String): ParsedChangelog {
    if (rawText.isBlank()) {
        return ParsedChangelog(
            photos = emptyList(),
            nodes = listOf(
                ChangelogNode.FeatureBullet(
                    title = "Bug Fixes & Improvements",
                    description = "General stability enhancements, playback optimization and performance updates."
                )
            )
        )
    }

    val imgMdRegex = Regex("""!\[.*?\]\((https?://[^\s\)]+)\)""")
    val imgHtmlRegex = Regex("""<img[^>]*src=["'](https?://[^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    val standaloneImgRegex = Regex("""^https?://[^\s]+\.(?:png|jpg|jpeg|webp|gif)(?:\?[^\s]*)?$""", RegexOption.IGNORE_CASE)

    val extractedPhotos = mutableListOf<String>()

    imgMdRegex.findAll(rawText).forEach { m ->
        extractedPhotos.add(m.groupValues[1])
    }
    imgHtmlRegex.findAll(rawText).forEach { m ->
        extractedPhotos.add(m.groupValues[1])
    }

    var cleanText = rawText
    cleanText = imgMdRegex.replace(cleanText, "")
    cleanText = imgHtmlRegex.replace(cleanText, "")

    val nodes = mutableListOf<ChangelogNode>()
    val lines = cleanText.lines()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue

        // Check if standalone image line
        if (standaloneImgRegex.matches(trimmed)) {
            extractedPhotos.add(trimmed)
            continue
        }

        // Section header (## Top Features, ### What's New)
        if (trimmed.startsWith("#")) {
            val headerText = trimmed.trimStart('#').trim().removePrefix("**").removeSuffix("**").trim()
            if (headerText.isNotBlank()) {
                nodes.add(ChangelogNode.Header(headerText))
            }
            continue
        }

        // Bullet item
        var content = trimmed
        var isBullet = false
        val bulletPrefixes = listOf("* ", "- ", "• ", "+ ")
        for (prefix in bulletPrefixes) {
            if (content.startsWith(prefix)) {
                content = content.removePrefix(prefix).trim()
                isBullet = true
                break
            }
        }

        // Pattern 1: **Title:** Description
        val boldMatch = Regex("""^\*\*(.*?)\*\*(.*)$""").find(content)
        if (boldMatch != null) {
            val title = boldMatch.groupValues[1].trim().removeSuffix(":").trim()
            val desc = boldMatch.groupValues[2].trim().removePrefix(":").removePrefix("-").trim()
            nodes.add(ChangelogNode.FeatureBullet(title.ifBlank { null }, desc.ifBlank { content }))
            continue
        }

        // Pattern 2: Title: Description
        val colonIdx = content.indexOf(':')
        if (colonIdx in 2..45) {
            val potentialTitle = content.substring(0, colonIdx).trim().removePrefix("**").removeSuffix("**").trim()
            val potentialDesc = content.substring(colonIdx + 1).trim()
            if (!potentialTitle.startsWith("http", ignoreCase = true) && potentialDesc.isNotBlank()) {
                nodes.add(ChangelogNode.FeatureBullet(potentialTitle, potentialDesc))
                continue
            }
        }

        // Pattern 3: Title - Description
        val dashIdx = content.indexOf(" - ")
        if (dashIdx in 2..45) {
            val potentialTitle = content.substring(0, dashIdx).trim().removePrefix("**").removeSuffix("**").trim()
            val potentialDesc = content.substring(dashIdx + 3).trim()
            if (potentialDesc.isNotBlank()) {
                nodes.add(ChangelogNode.FeatureBullet(potentialTitle, potentialDesc))
                continue
            }
        }

        // Fallback: bullet or paragraph
        if (isBullet) {
            nodes.add(ChangelogNode.FeatureBullet(null, content))
        } else {
            nodes.add(ChangelogNode.Paragraph(content))
        }
    }

    if (nodes.isEmpty()) {
        nodes.add(
            ChangelogNode.FeatureBullet(
                title = "Bug Fixes & Improvements",
                description = "General stability enhancements, playback optimization and performance updates."
            )
        )
    }

    return ParsedChangelog(
        photos = extractedPhotos.distinct().take(2),
        nodes = nodes
    )
}

/**
 * Release Media Showcase for 1 or 2 photos:
 * - 1 Photo: Premium 16:9 hero preview card with frosted glass hairline border.
 * - 2 Photos: Emil Kowalski swipeable pager with fluid animated pill dot indicators.
 */
@Composable
private fun ReleasePhotoShowcase(
    photos: List<String>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    if (photos.size == 1) {
        // Single Hero Image Card
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(SettingsTokens.HairlineWidth, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
        ) {
            AppAsyncImage(
                model = photos.first(),
                contentDescription = "Release Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        // 2-Photo Horizontal Pager Carousel
        val pagerState = rememberPagerState(pageCount = { photos.size })
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(SettingsTokens.HairlineWidth, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                ) {
                    AppAsyncImage(
                        model = photos[page],
                        contentDescription = "Release Preview ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated dot indicator pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                photos.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 5.dp,
                        animationSpec = spring(dampingRatio = 0.76f, stiffness = 650f),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.28f))
                    )
                }
            }
        }
    }
}

/**
 * Modern Software Update Popup Dialog.
 *
 * Designed in deep accordance with the 3 core principles:
 * - Taste: Luxury frosted glassmorphism card, subtle hairline border, no nested inner boxes, clean typography.
 * - Emil Kowalski: Fluid scrollable changelog with pinned bottom action dock over an upward gradient fade,
 *   organic spring physics on entry/exit, tactile press responses.
 * - Impeccable: Bold feature lead-ins, distinct section hierarchy, seamless 1-2 photo showcase, and high legibility.
 */
@Composable
fun UpdateInfoDialog(
    release: ReleaseInfo,
    currentVersion: String,
    downloadState: ApkDownloadState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: (ReleaseInfo) -> Unit,
    onTriggerInstall: (File, String) -> Unit,
    onSkipVersion: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val updateManager = remember(context) { AppUpdateManager.getInstance(context) }
    val scrollState = rememberScrollState()
    val isNewerUpdate = remember(currentVersion, release.tagName) {
        isRemoteVersionNewer(currentVersion, release.tagName)
    }

    val formattedSize = remember(release.apkSizeBytes) {
        if (release.apkSizeBytes > 0L) {
            String.format(Locale.US, "%.1f MB", release.apkSizeBytes / (1024.0 * 1024.0))
        } else null
    }

    // Parsed changelog structure with photos and formatted bullet nodes
    val parsedChangelog = remember(release.changelog) {
        parseReleaseChangelog(release.changelog)
    }

    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current
    val navbarVisible = LocalNavbarVisible.current

    DisposableEffect(Unit) {
        navbarVisible.value = false
        onDispose {
            navbarVisible.value = true
        }
    }

    val dialogProgress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            dialogProgress.snapTo(1f)
        } else {
            dialogProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 550f)
            )
        }
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    dialogProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180)
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler { dismissWithAnim() }

    val animValue = dialogProgress.value
    val scrimAlpha = if (reduceMotion) 0.65f else (0.65f * animValue).coerceIn(0f, 0.65f)
    val cardScale = if (reduceMotion) 1f else 0.94f + 0.06f * animValue
    val cardTranslationY = if (reduceMotion) 0f else (1f - animValue) * 30f
    val cardAlpha = if (reduceMotion) 1f else animValue.coerceIn(0f, 1f)

    val cardShape = RoundedCornerShape(28.dp)
    val cardFill = if (isAmoled) Color.Black.copy(alpha = 0.92f) else Color(0xFF0F1219).copy(alpha = 0.85f)
    val fallbackColor = if (isAmoled) Color.Black.copy(alpha = 0.96f) else Color(0xFF0F1219).copy(alpha = 0.96f)

    val isDownloading = downloadState is ApkDownloadState.Downloading
    val isReadyToInstall = downloadState is ApkDownloadState.ReadyToInstall

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            ),
        contentAlignment = Alignment.Center
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Modal Dialog Card
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    translationY = cardTranslationY
                    alpha = cardAlpha
                }
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.80f)
                .padding(horizontal = 20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent modal dismiss on card click */ }
                )
                .clip(cardShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 24.dp
                            tints = listOf(HazeTint(cardFill))
                            fallbackTint = HazeTint(fallbackColor)
                        }
                    } else {
                        Modifier.background(fallbackColor)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = cardShape
                )
        ) {
            // Main Content Area: Header + Scrollable Changelog
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Software Update",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            color = SettingsTokens.TextWhite96
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val subtitleText = buildString {
                        if (isNewerUpdate) {
                            append("Euthopiar ${release.versionName} is available to download.")
                        } else {
                            append("Euthopiar ${release.tagName}")
                        }
                        if (formattedSize != null) {
                            append(" · $formattedSize")
                        }
                    }

                    Text(
                        text = subtitleText,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = SettingsTokens.TextWhite54
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Subtle Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SettingsTokens.HairlineWidth)
                        .background(Color.White.copy(alpha = 0.10f))
                )

                // Scrollable Changelog Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Release Photos Showcase (1 or 2 images)
                    if (parsedChangelog.photos.isNotEmpty()) {
                        ReleasePhotoShowcase(
                            photos = parsedChangelog.photos,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Structured Changelog Items
                    parsedChangelog.nodes.forEach { node ->
                        when (node) {
                            is ChangelogNode.Header -> {
                                Text(
                                    text = node.title,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp,
                                        lineHeight = 21.sp,
                                        color = SettingsTokens.TextWhite96
                                    ),
                                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                )
                            }
                            is ChangelogNode.FeatureBullet -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                                    )
                                    val annotated = buildAnnotatedString {
                                        if (!node.title.isNullOrBlank()) {
                                            withStyle(
                                                SpanStyle(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SettingsTokens.TextWhite96
                                                )
                                            ) {
                                                append(node.title)
                                                append(": ")
                                            }
                                        }
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Normal,
                                                color = SettingsTokens.TextWhite72
                                            )
                                        ) {
                                            append(node.description)
                                        }
                                    }
                                    Text(
                                        text = annotated,
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        )
                                    )
                                }
                            }
                            is ChangelogNode.Paragraph -> {
                                Text(
                                    text = node.text,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        color = SettingsTokens.TextWhite72
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Bottom padding spacer so content scrolls completely clear of the floating pinned action dock
                    Spacer(modifier = Modifier.height(115.dp))
                }
            }

            // Pinned Bottom Action Dock over Upward Gradient Mask
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                cardFill.copy(alpha = 0.75f),
                                cardFill.copy(alpha = 0.98f),
                                cardFill
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Download Progress View (when Downloading)
                    if (downloadState is ApkDownloadState.Downloading) {
                        val percent = (downloadState.progressFraction * 100).toInt()
                        val curMb = downloadState.downloadedBytes / (1024.0 * 1024.0)
                        val totMb = downloadState.totalBytes / (1024.0 * 1024.0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading APK: $percent%",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = SettingsTokens.TextWhite82
                                )
                            )
                            if (downloadState.totalBytes > 0L) {
                                Text(
                                    text = String.format(Locale.US, "%.1f / %.1f MB", curMb, totMb),
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = SettingsTokens.TextWhite54
                                    )
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { downloadState.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Download Error Notice (when Error)
                    if (downloadState is ApkDownloadState.Error) {
                        Text(
                            text = "Download issue: ${downloadState.message}",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.5.sp,
                                color = Color(0xFFFF6B6B)
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Primary Action Button: 48dp Solid White Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(if (isDownloading) Color.White.copy(alpha = 0.50f) else Color.White)
                            .settingsPressable(
                                enabled = !isDownloading,
                                onClick = {
                                    if (isReadyToInstall) {
                                        onTriggerInstall((downloadState as ApkDownloadState.ReadyToInstall).apkFile, release.htmlUrl)
                                    } else {
                                        onDownloadAndInstall(release)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SettingsTokens.TextDark,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Downloading Update...",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = SettingsTokens.TextDark
                                    )
                                )
                            } else {
                                val primaryText = when {
                                    isReadyToInstall -> "Install Update"
                                    release.apkDownloadUrl != null && isNewerUpdate -> "Download Now"
                                    release.apkDownloadUrl != null -> "Download APK"
                                    else -> "View on GitHub"
                                }
                                Text(
                                    text = primaryText,
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = SettingsTokens.TextDark
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Quiet Text Button: Remind Me Later
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .settingsPressable(onClick = dismissWithAnim)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isNewerUpdate) "Remind Me Later" else "Close",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = SettingsTokens.TextWhite72
                                )
                            )
                        }

                        if (isNewerUpdate) {
                            Text(
                                text = "·",
                                style = TextStyle(color = SettingsTokens.TextWhite42, fontSize = 13.sp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .settingsPressable(
                                        onClick = {
                                            onSkipVersion(release.versionName)
                                            dismissWithAnim()
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Skip Version",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.5.sp,
                                        color = SettingsTokens.TextWhite45
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
