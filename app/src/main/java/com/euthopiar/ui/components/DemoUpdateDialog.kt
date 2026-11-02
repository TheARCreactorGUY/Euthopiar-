package com.euthopiar.ui.components

import android.os.Build
import android.widget.Toast
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
import androidx.compose.runtime.*
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
import com.euthopiar.ui.settings.components.SettingsTokens
import com.euthopiar.ui.settings.components.settingsPressable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standalone Showcase Demo for the Software Update Dialog.
 *
 * Strictly isolated:
 * - Uses self-contained mock changelog data with 2 showcase photos.
 * - Simulates download progress locally (0% to 100%) without network or APK downloads.
 * - Completely unlinked from production [AppUpdateManager] or release preferences.
 */
@Composable
fun DemoUpdateDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val isAmoled = LocalAmoledMode.current

    val dialogProgress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    // Simulated download state for testing
    var isDownloading by remember { mutableStateOf(false) }
    var simulatedProgress by remember { mutableStateOf(0f) }
    var isReadyToInstall by remember { mutableStateOf(false) }

    // Demo showcase photos
    val demoPhotos = remember {
        listOf(
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80"
        )
    }

    // Demo structured changelog nodes
    val demoNodes = remember {
        listOf(
            ChangelogNode.Header("Top Features"),
            ChangelogNode.FeatureBullet(
                title = "Bit-perfect Output",
                description = "Lossless audio and high-bitrate video reach your screen and DAC unchanged, matching native frame rate and sample depth."
            ),
            ChangelogNode.FeatureBullet(
                title = "Aspect Ratio HUD",
                description = "Tactile aspect ratio pill indicator with non-intrusive spring physics and rapid gesture dismissal."
            ),
            ChangelogNode.FeatureBullet(
                title = "Dynamic Media Logos",
                description = "High-definition content logos smoothly transition from center loading stage into the details header."
            ),
            ChangelogNode.Header("What's New"),
            ChangelogNode.FeatureBullet(
                title = "Quality Indicator",
                description = "Auto-fading playback resolution pill showing real-time stream bitrate, resolution, and format."
            ),
            ChangelogNode.FeatureBullet(
                title = "Cloud Stream Seeking",
                description = "Lightning-fast HTTP range seeks outside buffer limits without blocking sequential streaming."
            ),
            ChangelogNode.Header("Fixes & Polish"),
            ChangelogNode.FeatureBullet(
                title = "Layout Stability",
                description = "Fixed layout overlapping between player control dock and system navigation bars."
            ),
            ChangelogNode.FeatureBullet(
                title = "Buffer Efficiency",
                description = "Resolved memory retention in background chunk caching."
            )
        )
    }

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

                    Text(
                        text = "Euthopiar 1.0.8 (Showcase Demo) · 46.8 MB",
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
                    // Demo 2-Photo Horizontal Pager Carousel
                    val pagerState = rememberPagerState(pageCount = { demoPhotos.size })
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
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
                                    model = demoPhotos[page],
                                    contentDescription = "Demo Preview ${page + 1}",
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
                            demoPhotos.indices.forEach { index ->
                                val isSelected = pagerState.currentPage == index
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 5.dp,
                                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 650f),
                                    label = "demoDotWidth"
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

                    // Structured Changelog Items
                    demoNodes.forEach { node ->
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
                                        .padding(vertical = 4.5.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = TextStyle(
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp,
                                            color = Color.White
                                        )
                                    )
                                    val annotated = buildAnnotatedString {
                                        if (node.title != null) {
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
                                cardFill.copy(alpha = 0.85f),
                                cardFill
                            ),
                            startY = 0f,
                            endY = 40f
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live simulated progress indicator when download is active
                    AnimatedVisibility(
                        visible = isDownloading,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(140))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Simulated Download...",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontSize = 11.5.sp,
                                        color = SettingsTokens.TextWhite72
                                    )
                                )
                                Text(
                                    text = "${(simulatedProgress * 100).toInt()}%",
                                    style = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { simulatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.20f)
                            )
                        }
                    }

                    // Primary Action Pill Button
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
                                        Toast.makeText(context, "Demo installation simulated!", Toast.LENGTH_SHORT).show()
                                        dismissWithAnim()
                                    } else {
                                        // Start simulated download progress
                                        coroutineScope.launch {
                                            isDownloading = true
                                            simulatedProgress = 0f
                                            for (i in 1..20) {
                                                delay(80L)
                                                simulatedProgress = (i * 5f) / 100f
                                            }
                                            isDownloading = false
                                            isReadyToInstall = true
                                        }
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
                                val primaryText = if (isReadyToInstall) "Install Update (Demo)" else "Download Now"
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

                    Spacer(modifier = Modifier.height(2.dp))

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
                                text = "Remind Me Later",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = SettingsTokens.TextWhite72
                                )
                            )
                        }

                        Text(
                            text = "·",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 13.5.sp,
                                color = SettingsTokens.TextWhite42
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Skip Version quiet link
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .settingsPressable(
                                    onClick = {
                                        Toast.makeText(context, "Demo update skipped", Toast.LENGTH_SHORT).show()
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
                                    fontSize = 12.sp,
                                    color = SettingsTokens.TextWhite42
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
