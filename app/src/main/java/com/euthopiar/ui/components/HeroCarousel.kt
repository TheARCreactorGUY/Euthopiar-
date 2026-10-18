package com.euthopiar.ui.components

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.euthopiar.R
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.data.local.PlaybackHistoryEntity
import com.euthopiar.ui.details.PlaybackActionState
import com.euthopiar.ui.details.PlaybackTargetHelper
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Background
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.HairlineWidth
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.rememberReduceMotion
import com.euthopiar.ui.theme.Motion
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.pressable
import com.euthopiar.ui.theme.rememberWindowDimensions
import com.euthopiar.ui.util.TrimTransparentTransformation
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HERO_BACKGROUND_PARALLAX = 0.06f
private const val HERO_BACKGROUND_BASE_SCALE = 1.08f
private const val HERO_CONTENT_PARALLAX = 0.18f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val AUTO_ADVANCE_DURATION_MS = 10000L

/**
 * Flagship Cinematic Hero Carousel Overhaul matching Part 1 - Part 7 design specifications.
 *
 * Global Visual Language:
 * - Bold, minimal, modern, cinematic atmosphere.
 * - Deep artwork field with top safe legibility scrim & multi-stop bottom melt to near-black background.
 * - Soft color haze blob behind lower hero subtly derived from the slide's dominant color tone.
 * - Studio ClearLogo with text fallback in 28-30sp medium wide tracking.
 * - Wrapping row of small glass chips (24px tall, 8px radius, 11sp, 6px gaps, outline icons, no colors).
 * - White primary pill button (48px tall, weight 1f, 15sp medium dark text, dark play icon).
 * - Glass secondary pill button (48px tall, 20px padding, outline info icon, 14sp regular text).
 * - Thin 2px progress bar (white 18% track, white 90% fill, 11sp muted minutes-left label).
 * - Centered thin 2px pagination segments (28px active with auto-advance fill, 16px inactive).
 * - 60fps hardware-accelerated stagger motion on slide transitions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onPlayClick: (MediaItem, PlaybackHistoryEntity?) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    onBookmarkClick: (MediaItem) -> Unit,
    playbackMap: Map<String, PlaybackHistoryEntity> = emptyMap(),
    bookmarkedIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(
        initialPage = if (items.size > 1) (1000 / 2) * items.size else 0,
        pageCount = { if (items.size > 1) items.size * 1000 else items.size.coerceAtLeast(1) }
    ),
    isOffScreen: Boolean = false,
    isFeedScrolling: () -> Boolean = { false },
    playFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    stretchPx: () -> Float = { 0f },
    scrollOffsetPx: () -> Float = { 0f },
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val reduceMotion = rememberReduceMotion()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val windowDimensions = rememberWindowDimensions()
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLargeScreen = windowDimensions.isTablet || windowDimensions.isTV

    // Responsive carousel height: adapts between portrait phone, landscape phone, and tablets/TVs
    val carouselHeight = if (isLandscape) {
        if (isLargeScreen) {
            (configuration.screenHeightDp * 0.65f).coerceIn(400f, 600f).dp
        } else {
            (configuration.screenHeightDp * 0.85f).coerceIn(280f, 380f).dp
        }
    } else {
        (configuration.screenHeightDp * 0.60f + 40f).coerceIn(480f, 600f).dp
    }
    val contentBottomPadding = if (isLandscape && !isLargeScreen) Dimens.spacing12 else Dimens.spacing24
    val heroWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val maxStretchPx = with(density) { 180.dp.toPx() }

    val heroHazeState = remember { HazeState() }
    var hasFocus by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-advance progress timer for active pagination segment fill
    val autoAdvanceProgress = remember { Animatable(0f) }
    var isUserTouching by remember { mutableStateOf(false) }
    var lastPage by remember { mutableStateOf(pagerState.currentPage) }

    // Auto-advance loop: 10 seconds vsync-synced, pausing on interaction, feed scrolling, backgrounded, or reduce motion
    LaunchedEffect(items.size, reduceMotion, hasFocus, isOffScreen, pagerState.currentPage, isUserTouching) {
        if (pagerState.currentPage != lastPage) {
            lastPage = pagerState.currentPage
            autoAdvanceProgress.snapTo(0f)
        }
        if (!reduceMotion && items.size > 1 && !isOffScreen && !isUserTouching && !hasFocus) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (pagerState.isScrollInProgress || isFeedScrolling()) {
                    delay(100L)
                }
                val remainingFraction = (1f - autoAdvanceProgress.value).coerceIn(0f, 1f)
                val remainingDuration = (AUTO_ADVANCE_DURATION_MS * remainingFraction).roundToInt().coerceAtLeast(1)

                autoAdvanceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingDuration,
                        easing = LinearEasing
                    )
                )

                while (pagerState.isScrollInProgress || isFeedScrolling() || hasFocus || isUserTouching) {
                    delay(100L)
                }

                if (!hasFocus && !isOffScreen && !isUserTouching) {
                    val nextPage = pagerState.currentPage + 1
                    if (nextPage < pagerState.pageCount) {
                        try {
                            pagerState.animateScrollToPage(
                                nextPage,
                                animationSpec = tween(Motion.DurationHero, easing = Motion.EmphasizedDecelerateEasing)
                            )
                        } finally {
                            if (abs(pagerState.currentPageOffsetFraction) > 0.001f) {
                                withContext(NonCancellable) {
                                    val settleTarget = (pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt())
                                        .coerceIn(0, pagerState.pageCount - 1)
                                    pagerState.scrollToPage(settleTarget)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Snap to virtual middle page on initial setup for bidirectional infinity
    LaunchedEffect(items.size) {
        if (items.size > 1 && pagerState.currentPage == 0 && pagerState.pageCount > items.size) {
            val middlePage = (pagerState.pageCount / (2 * items.size)) * items.size
            pagerState.scrollToPage(middlePage)
        }
    }

    // Multi-plane visible layer calculation
    val currentVirtualPage = pagerState.currentPage
    val visiblePages = listOf(
        currentVirtualPage,
        currentVirtualPage - 1,
        currentVirtualPage + 1,
    ).distinct()
        .mapNotNull { page ->
            if (page !in 0 until pagerState.pageCount) return@mapNotNull null
            val pageOffset = heroPageOffset(pagerState, page)
            val visibility = (1f - abs(pageOffset)).coerceIn(0f, 1f)
            if (visibility <= 0f) {
                null
            } else {
                val actualIndex = if (items.isNotEmpty()) {
                    ((page % items.size) + items.size) % items.size
                } else 0
                HeroPageLayer(
                    page = page,
                    itemIndex = actualIndex,
                    visibility = visibility,
                    offset = pageOffset,
                )
            }
        }
        .sortedBy(HeroPageLayer::page)

    val currentActualIndex = if (items.isNotEmpty()) {
        ((currentVirtualPage % items.size) + items.size) % items.size
    } else 0
    val currentSlideItem = items.getOrNull(currentActualIndex)

    // Soft color haze derived from current slide's identity
    val targetHazeColor = if (currentSlideItem != null) resolveSlideHazeColor(currentSlideItem) else Color(0xFF2A2859)
    val animatedHazeColor by animateColorAsState(
        targetValue = targetHazeColor,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "heroSlideHazeColor"
    )

    val isAmoled = LocalAmoledMode.current
    val baseBg = if (isAmoled) Color.Black else Background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heroStretchHeight(carouselHeight, stretchPx)
            .background(baseBg)
            .heroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
                onUserTouchChange = { isUserTouching = it },
            )
            .clipToBounds()
            .onFocusChanged { hasFocus = it.hasFocus }
    ) {
        // Keep Compose's Pager layout measurements fully active in the background
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.001f },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        // ==========================================
        // Plane 1: Backdrop Artwork Field & Contrast Scrims
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = heroHazeState)
        ) {
            visiblePages.forEach { layer ->
                key(layer.page) {
                    val item = items[layer.itemIndex]
                    val isCurrentItem = layer.page == currentVirtualPage

                    HeroBackdropLayer(
                        layer = layer,
                        item = item,
                        heroWidthPx = heroWidthPx,
                        isCurrentItem = isCurrentItem,
                        reduceMotion = reduceMotion,
                        scrollOffsetPx = scrollOffsetPx
                    )
                }
            }
            HeroScrimsOverlay(
                baseBg = baseBg,
                animatedHazeColor = animatedHazeColor,
                isAmoled = isAmoled,
                carouselHeight = carouselHeight
            )
        }

        // ==========================================
        // Planes 2 & 3: Floating HUD (Logo, Chips, Actions, Progress, Pagination)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heroStretchContentCompress(stretchPx, maxStretchPx)
        ) {
            visiblePages.forEach { layer ->
                key(layer.page) {
                    val item = items[layer.itemIndex]
                    val isInteractive = layer.visibility >= 0.7f
                    val isCurrentItem = layer.page == currentVirtualPage

                    HeroHudContent(
                        layer = layer,
                        item = item,
                        isInteractive = isInteractive,
                        isCurrentItem = isCurrentItem,
                        playbackHistory = playbackMap[item.id],
                        itemsCount = items.size,
                        heroHazeState = heroHazeState,
                        heroWidthPx = heroWidthPx,
                        scrollOffsetPx = scrollOffsetPx,
                        contentBottomPadding = contentBottomPadding,
                        playFocusRequester = playFocusRequester,
                        reduceMotion = reduceMotion,
                        onPlayClick = onPlayClick,
                        onDetailsClick = onDetailsClick
                    )
                }
            }

            // ==========================================
            // Plane 4: Centered Unified Pagination Bar (Part 3 & Part 7)
            // ==========================================
            if (items.size > 1) {
                HeroPagination(
                    itemsCount = items.size,
                    currentVirtualPage = currentVirtualPage,
                    progressProvider = { autoAdvanceProgress.value },
                    reduceMotion = reduceMotion,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = contentBottomPadding)
                )
            }
        }
    }
}

/**
 * Floating HUD Content block for a carousel slide with staggered element entrance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.HeroHudContent(
    layer: HeroPageLayer,
    item: MediaItem,
    isInteractive: Boolean,
    isCurrentItem: Boolean,
    playbackHistory: PlaybackHistoryEntity?,
    itemsCount: Int,
    heroHazeState: HazeState,
    heroWidthPx: Float,
    scrollOffsetPx: () -> Float,
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    playFocusRequester: androidx.compose.ui.focus.FocusRequester?,
    reduceMotion: Boolean,
    onPlayClick: (MediaItem, PlaybackHistoryEntity?) -> Unit,
    onDetailsClick: (MediaItem) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val tenDpPx = with(density) { 10.dp.toPx() }

    val absOffset = abs(layer.offset)
    val enterFraction = (1f - absOffset).coerceIn(0f, 1f)
    // Outgoing elements fade out quickly (~150ms equivalent in transition)
    val outgoingAlpha = (1f - absOffset * 2.5f).coerceIn(0f, 1f)

    // Stagger calculations for incoming elements: logo -> chips (+40ms) -> actions (+80ms) -> progress (+120ms)
    val logoStagger = if (reduceMotion) enterFraction else ((enterFraction - 0.15f) / 0.65f).coerceIn(0f, 1f)
    val logoAlpha = if (isCurrentItem) outgoingAlpha.coerceAtLeast(logoStagger) else logoStagger
    val logoRise = if (reduceMotion) 0f else (1f - logoStagger) * tenDpPx

    val chipsStagger = if (reduceMotion) enterFraction else ((enterFraction - 0.25f) / 0.65f).coerceIn(0f, 1f)
    val chipsAlpha = if (isCurrentItem) outgoingAlpha.coerceAtLeast(chipsStagger) else chipsStagger
    val chipsRise = if (reduceMotion) 0f else (1f - chipsStagger) * tenDpPx

    val actionsStagger = if (reduceMotion) enterFraction else ((enterFraction - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val actionsAlpha = if (isCurrentItem) outgoingAlpha.coerceAtLeast(actionsStagger) else actionsStagger
    val actionsRise = if (reduceMotion) 0f else (1f - actionsStagger) * tenDpPx

    val progressStagger = if (reduceMotion) enterFraction else ((enterFraction - 0.45f) / 0.55f).coerceIn(0f, 1f)
    val progressAlpha = if (isCurrentItem) outgoingAlpha.coerceAtLeast(progressStagger) else progressStagger
    val progressRise = if (reduceMotion) 0f else (1f - progressStagger) * tenDpPx

    val actionState = remember(item.id, playbackHistory) {
        PlaybackTargetHelper.resolveActionState(playbackHistory, item.type)
    }

    // Clearance above centered pagination: 20dp gap + 2dp segment height
    val hudBottomPadding = if (itemsCount > 1) contentBottomPadding + 22.dp else contentBottomPadding

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomStart)
            .padding(horizontal = 16.dp)
            .padding(bottom = hudBottomPadding)
            .graphicsLayer {
                val scrollExitAlpha = (1f - (scrollOffsetPx() / 320.dp.toPx())).coerceIn(0f, 1f)
                alpha = scrollExitAlpha
                translationX = -layer.offset * heroWidthPx * HERO_CONTENT_PARALLAX
                translationY = abs(layer.offset) * 12.dp.toPx()
            },
        horizontalAlignment = Alignment.Start
    ) {
        // 1. Studio ClearLogo (or Text Fallback, max 70% screen width)
        var isLogoError by remember(item.logoUrl) { mutableStateOf(false) }
        val maxLogoWidth = (configuration.screenWidthDp * 0.70f).dp
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = logoAlpha
                    translationY = logoRise
                }
                .padding(bottom = 12.dp)
        ) {
            if (!item.logoUrl.isNullOrBlank() && !isLogoError) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.logoUrl)
                        .crossfade(true)
                        .transformations(TrimTransparentTransformation())
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .heightIn(min = 40.dp, max = 64.dp)
                        .widthIn(max = maxLogoWidth),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    onError = { isLogoError = true }
                )
            } else {
                Text(
                    text = item.title.uppercase(),
                    style = AppTypography.displayHero.copy(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 28.sp,
                        letterSpacing = 0.16.em
                    ),
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.semantics {
                        contentDescription = item.title
                    }
                )
            }
        }

        // 2. Metadata Chips (wrapping row of small glass chips, missing fields hidden)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = chipsAlpha
                    translationY = chipsRise
                }
                .padding(bottom = 16.dp)
        ) {
            HeroMetadataChips(
                year = item.year,
                type = item.type,
                quality = item.quality,
                contentRating = item.contentRating,
                imdbRating = item.rating,
                rottenTomatoesRating = item.rottenTomatoesRating,
                hazeState = heroHazeState
            )
        }

        // 3. Action Row: White Primary Pill (flexes) + Glass Details Pill (hugs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = actionsAlpha
                    translationY = actionsRise
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary Button: Solid White Pill filling remaining width (48px tall, 15sp medium dark text, play icon)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(ShapeFull)
                    .background(Color.White)
                    .pressable(
                        scaleDown = 0.98f,
                        pressedAlpha = 0.92f,
                        onClickLabel = "${actionState.label}, ${item.title}",
                        enabled = isInteractive,
                        onClick = { if (isInteractive) onPlayClick(item, playbackHistory) }
                    )
                    .then(
                        if (isInteractive && isCurrentItem && playFocusRequester != null) {
                            Modifier.focusRequester(playFocusRequester)
                        } else Modifier
                    )
                    .tvFocusable(shape = ShapeFull)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF0B0B0F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionState.label,
                        fontFamily = AppFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0B0B0F),
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Secondary Button: Frosted Glass Pill (48px tall, 20px padding, outline info icon, 14sp medium text)
            val isAmoled = LocalAmoledMode.current
            val detailsAnchorColor = if (isAmoled) Color.Black else Color(0xFF000000)
            val detailsFallbackColor = if (isAmoled) Color.Black else Color(0xFF14151F).copy(alpha = 0.94f)
            val detailsAnchorAlpha = 0.34f
            val detailsWhiteLiftAlpha = 0.06f

            val detailsBorderBrush = remember {
                Brush.verticalGradient(
                    0.00f to Color.White.copy(alpha = 0.32f),
                    0.35f to Color.White.copy(alpha = 0.10f),
                    0.65f to Color.White.copy(alpha = 0.06f),
                    1.00f to Color.White.copy(alpha = 0.16f)
                )
            }

            val detailsBackgroundModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.hazeEffect(state = heroHazeState) {
                    blurRadius = 18.dp
                    tints = listOf(
                        HazeTint(detailsAnchorColor.copy(alpha = detailsAnchorAlpha)),
                        HazeTint(Color.White.copy(alpha = detailsWhiteLiftAlpha))
                    )
                    fallbackTint = HazeTint(detailsFallbackColor)
                }
            } else {
                Modifier
                    .background(detailsAnchorColor.copy(alpha = detailsAnchorAlpha), ShapeFull)
                    .background(Color.White.copy(alpha = detailsWhiteLiftAlpha), ShapeFull)
            }

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(ShapeFull)
                    .then(detailsBackgroundModifier)
                    .border(
                        width = 1.dp,
                        brush = detailsBorderBrush,
                        shape = ShapeFull
                    )
                    .drawBehind {
                        // Subtle 1dp top inner highlight white at 10%
                        drawLine(
                            color = Color.White.copy(alpha = 0.10f),
                            start = Offset(16.dp.toPx(), 1.dp.toPx()),
                            end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .pressable(
                        scaleDown = 0.98f,
                        onClickLabel = "Details for ${item.title}",
                        enabled = isInteractive,
                        onClick = { if (isInteractive) onDetailsClick(item) }
                    )
                    .tvFocusable(shape = ShapeFull)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Details",
                        fontFamily = AppFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Progress Row Slot: Thin 2px Progress Bar + Muted Minutes Left (only for Resume items)
        // Fixed slot height ensures zero vertical jump when swiping between titles with/without progress (Part 5)
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (actionState is PlaybackActionState.Resume) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = progressAlpha
                            translationY = progressRise
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        val clampedProgress = actionState.progress.coerceIn(0f, 1f)
                        if (clampedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(clampedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Color.White.copy(alpha = 0.90f))
                            )
                        }
                    }
                    Text(
                        text = "${actionState.minutesLeft} min left",
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.75f),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.40f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Centered carousel pagination segments matching Part 3 & Part 7 specifications:
 * - 2px tall, fully rounded.
 * - Active segment is 28px and 90% white (fills left-to-right over auto-advance).
 * - Inactive segments are 16px and 22% white.
 * - 5px gap.
 * - Smooth 250ms ease-out width and alpha transitions.
 * - Compact sliding window for >8 slides.
 * - Accurate 1-based accessibility descriptions ("Slide X of Y").
 */
@Composable
private fun HeroPagination(
    itemsCount: Int,
    currentVirtualPage: Int,
    progressProvider: () -> Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (itemsCount <= 1) return

    val activeIndex = if (itemsCount > 0) {
        ((currentVirtualPage % itemsCount) + itemsCount) % itemsCount
    } else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (itemsCount <= 8) {
            // Standard full pagination for up to 8 slides
            repeat(itemsCount) { index ->
                if (index > 0) Spacer(modifier = Modifier.width(5.dp))
                val isActive = index == activeIndex
                HeroPaginationSegment(
                    isActive = isActive,
                    slideIndex = index,
                    totalSlides = itemsCount,
                    progressProvider = progressProvider,
                    reduceMotion = reduceMotion
                )
            }
        } else {
            // Compact sliding window for >8 slides
            val maxVisible = 7
            val windowStart = (activeIndex - maxVisible / 2).coerceIn(0, itemsCount - maxVisible)
            repeat(maxVisible) { slot ->
                if (slot > 0) Spacer(modifier = Modifier.width(5.dp))
                val slideIndex = windowStart + slot
                val isActive = slideIndex == activeIndex
                val isEdge = (slot == 0 && windowStart > 0) || (slot == maxVisible - 1 && windowStart + maxVisible < itemsCount)
                HeroPaginationSegment(
                    isActive = isActive,
                    slideIndex = slideIndex,
                    totalSlides = itemsCount,
                    progressProvider = progressProvider,
                    reduceMotion = reduceMotion,
                    isCompactEdge = isEdge
                )
            }
        }
    }
}

@Composable
private fun HeroPaginationSegment(
    isActive: Boolean,
    slideIndex: Int,
    totalSlides: Int,
    progressProvider: () -> Float,
    reduceMotion: Boolean,
    isCompactEdge: Boolean = false
) {
    val targetWidth = when {
        isActive -> 28.dp
        isCompactEdge -> 8.dp
        else -> 16.dp
    }
    val targetAlpha = if (isActive) 0.90f else 0.35f

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing),
        label = "paginationWidth_$slideIndex"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing),
        label = "paginationAlpha_$slideIndex"
    )

    Box(
        modifier = Modifier
            .height(2.dp)
            .width(animatedWidth)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White.copy(alpha = if (isActive) 0.35f else animatedAlpha))
            .semantics {
                contentDescription = "Slide ${slideIndex + 1} of $totalSlides"
            }
            .then(
                if (isActive) {
                    Modifier.drawWithContent {
                        drawContent()
                        val fraction = if (!reduceMotion) progressProvider().coerceIn(0f, 1f) else 1f
                        if (fraction > 0f) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = animatedAlpha),
                                size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                    }
                } else Modifier
            )
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HeroScrimsOverlay(
    baseBg: Color,
    animatedHazeColor: Color,
    isAmoled: Boolean,
    carouselHeight: androidx.compose.ui.unit.Dp
) {
    // 2. Multi-stop vertical melt scrim (Smootherstep 11 stops)
    // Sized to the full hero rectangle, extending 2dp past bottom edge to prevent fractional rounding seam
    val meltStops = remember(baseBg, animatedHazeColor, isAmoled) {
        calculateHeroMeltColorStops(
            baseBg = baseBg,
            dominantColor = animatedHazeColor,
            isAmoled = isAmoled
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = meltStops,
                        startY = 0f,
                        endY = size.height
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height + 2.dp.toPx())
                )
            }
    )

    // 3. HUD Contrast Scrim: Runs from 48dp above top of logo down to hero bottom edge
    // Alpha rises over first 60% with smootherstep from 0 to 0.40, then holds at 0.40
    val hudScrimStops = remember(baseBg) {
        calculateHudScrimColorStops(baseBg)
    }
    val hudScrimHeight = (carouselHeight * 0.58f).coerceAtLeast(300.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(hudScrimHeight)
            .align(Alignment.BottomCenter)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = hudScrimStops,
                        startY = 0f,
                        endY = size.height
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height + 2.dp.toPx())
                )
            }
    )

    // 4. Side vignette for left-aligned typography contrast
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to baseBg.copy(alpha = 0.80f),
                        0.45f to baseBg.copy(alpha = 0.30f),
                        0.80f to baseBg.copy(alpha = 0.00f),
                    )
                )
            )
    )

    // 5. Top status bar and header protection scrim (75% black down to transparent ~120dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.75f),
                    0.55f to Color.Black.copy(alpha = 0.35f),
                    1.0f to Color.Black.copy(alpha = 0.00f),
                )
            )
    )
}

@Composable
private fun HeroBackdropLayer(
    layer: HeroPageLayer,
    item: MediaItem,
    heroWidthPx: Float,
    isCurrentItem: Boolean,
    reduceMotion: Boolean,
    scrollOffsetPx: () -> Float,
) {
    val configuration = LocalConfiguration.current
    val windowDimensions = rememberWindowDimensions()
    val isLandscapeOrLarge = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        windowDimensions.isTablet || windowDimensions.isTV
    val isAmoled = LocalAmoledMode.current
    val screenBg = if (isAmoled) Color.Black else Background

    val rawArtworkUrl = if (isLandscapeOrLarge) {
        item.backdropUrl ?: item.posterUrl
    } else {
        item.posterUrl ?: item.backdropUrl
    }

    val artworkUrl = remember(rawArtworkUrl, isLandscapeOrLarge) {
        if (rawArtworkUrl == null) null
        else if (isLandscapeOrLarge) {
            rawArtworkUrl.replace("/t/p/w500/", "/t/p/w1280/")
        } else {
            rawArtworkUrl.replace("/t/p/w500/", "/t/p/w780/")
        }
    }

    // Settling scale on slide change: settles from 104% (1.04f) down to 100% (1.00f)
    val settleScaleAnim = remember(layer.page) { Animatable(if (reduceMotion) 1.0f else 1.04f) }
    LaunchedEffect(layer.page, isCurrentItem, reduceMotion) {
        if (isCurrentItem && !reduceMotion) {
            settleScaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
        }
    }

    // Ken Burns subtle cinematic zoom in: starts at 1.0f and gently drifts forward
    val kenBurnsScale = remember(layer.page) { Animatable(1.0f) }
    LaunchedEffect(layer.page, isCurrentItem, reduceMotion) {
        if (isCurrentItem && !reduceMotion) {
            kenBurnsScale.animateTo(
                targetValue = 1.055f,
                animationSpec = tween(
                    durationMillis = 9600,
                    easing = LinearEasing
                )
            )
        }
    }

    var isImageError by remember(artworkUrl) { mutableStateOf(false) }

    if (artworkUrl == null || isImageError) {
        // Dark neutral block with soft glass icon tile when image is missing or fails to load (Part 5)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .graphicsLayer {
                    val scrollY = scrollOffsetPx().coerceAtLeast(0f)
                    alpha = layer.visibility
                    translationX = -layer.offset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                    translationY = scrollY * 0.5f
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(HairlineWidth, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_type_movie_clapperboard),
                    contentDescription = item.title,
                    tint = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        val context = LocalContext.current
        val imageRequest = remember(artworkUrl, context) {
            ImageRequest.Builder(context)
                .data(artworkUrl)
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            alignment = if (isLandscapeOrLarge) Alignment.Center else Alignment.TopCenter,
            onError = { isImageError = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scrollY = scrollOffsetPx().coerceAtLeast(0f)
                    alpha = layer.visibility
                    translationX = -layer.offset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                    // 0.5x Parallax drift: backdrop scrolls slower than the feed for true depth
                    translationY = scrollY * 0.5f
                    // Parallax zoom: poster zooms subtly into background as user scrolls down the page
                    val scrollZoom = 1f + (scrollY * 0.0005f).coerceAtMost(0.12f)
                    val scale = HERO_BACKGROUND_BASE_SCALE * settleScaleAnim.value * kenBurnsScale.value * scrollZoom
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

/**
 * Resolves soft atmospheric background haze color derived from item characteristics.
 */
private fun resolveSlideHazeColor(item: MediaItem): Color {
    val hash = (item.id.hashCode() and 0x7FFFFFFF) % 5
    return when (hash) {
        0 -> Color(0xFF2A2859) // Indigo
        1 -> Color(0xFF5A2542) // Rose
        2 -> Color(0xFF1A454D) // Teal
        3 -> Color(0xFF4A3820) // Muted warm
        else -> Color(0xFF3B2358) // Violet
    }
}

/**
 * Hero Carousel Shimmer Skeleton matching Part 5:
 *
 * Dark image block with shimmer, logo-shaped skeleton bar,
 * row of chip skeleton blocks, two pill skeletons (one wide white-ish, one glass),
 * and centered pagination skeleton segments.
 */
@Composable
fun HeroCarouselSkeleton(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val windowDimensions = rememberWindowDimensions()
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLargeScreen = windowDimensions.isTablet || windowDimensions.isTV

    val carouselHeight = if (isLandscape) {
        if (isLargeScreen) {
            (configuration.screenHeightDp * 0.65f).coerceIn(400f, 600f).dp
        } else {
            (configuration.screenHeightDp * 0.85f).coerceIn(280f, 380f).dp
        }
    } else {
        (configuration.screenHeightDp * 0.60f + 40f).coerceIn(480f, 600f).dp
    }
    val contentBottomPadding = if (isLandscape && !isLargeScreen) Dimens.spacing12 else Dimens.spacing24
    val isAmoled = LocalAmoledMode.current
    val skeletonBg = if (isAmoled) Color.Black else Background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(carouselHeight)
            .background(skeletonBg)
    ) {
        // Shimmering backdrop base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )

        // Bottom dark melt
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to skeletonBg.copy(alpha = 0.45f),
                        0.80f to skeletonBg.copy(alpha = 0.90f),
                        1.0f to skeletonBg,
                    )
                )
        )

        // Content skeleton overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = contentBottomPadding),
            horizontalAlignment = Alignment.Start
        ) {
            // Logo skeleton bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row of chip skeletons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) { i ->
                    val chipWidth = when (i) {
                        0 -> 48.dp
                        1 -> 64.dp
                        2 -> 44.dp
                        else -> 54.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(chipWidth)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row skeletons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wide white-ish primary pill skeleton
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(ShapeFull)
                        .shimmerEffect()
                )

                // Glass details pill skeleton
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(48.dp)
                        .clip(ShapeFull)
                        .shimmerEffect()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Centered pagination skeleton segments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { i ->
                    if (i > 0) Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .width(if (i == 0) 28.dp else 16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = if (i == 0) 0.50f else 0.20f))
                    )
                }
            }
        }
    }
}

private data class HeroPageLayer(
    val page: Int,
    val itemIndex: Int,
    val visibility: Float,
    val offset: Float,
)

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun Modifier.heroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
    onUserTouchChange: (Boolean) -> Unit = {},
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onUserTouchChange(true)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: run {
                onUserTouchChange(false)
                return@awaitEachGesture
            }
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    onUserTouchChange(false)
                    break
                }
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (!change.pressed) {
                    onUserTouchChange(false)
                    if (dragging) {
                        val targetPage = resolveHeroTargetPage(
                            startPage = startPage,
                            pageCount = pagerState.pageCount,
                            totalDx = totalDx,
                            velocityX = velocityTracker.calculateVelocity().x,
                            widthPx = widthPx,
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                targetPage,
                                animationSpec = tween(Motion.DurationEmphasized, easing = Motion.EmphasizedDecelerateEasing)
                            )
                        }
                    } else if (abs(pagerState.currentPageOffsetFraction) > 0.001f) {
                        // Settle cleanly if a quick tap or release interrupted an active animation
                        coroutineScope.launch {
                            val targetPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt())
                                .coerceIn(0, pagerState.pageCount - 1)
                            pagerState.animateScrollToPage(
                                targetPage,
                                animationSpec = tween(Motion.DurationEmphasized, easing = Motion.EmphasizedDecelerateEasing)
                            )
                        }
                    }
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val verticalDrag =
                        abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                    when {
                        verticalDrag -> {
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.changes.none { it.pressed }) break
                            }
                            onUserTouchChange(false)
                            if (abs(pagerState.currentPageOffsetFraction) > 0.001f) {
                                // If vertical drag interrupted an auto-shift in flight, settle cleanly
                                coroutineScope.launch {
                                    val targetPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt())
                                        .coerceIn(0, pagerState.pageCount - 1)
                                    pagerState.animateScrollToPage(
                                        targetPage,
                                        animationSpec = tween(Motion.DurationEmphasized, easing = Motion.EmphasizedDecelerateEasing)
                                    )
                                }
                            }
                            break
                        }
                        horizontalDrag -> dragging = true
                        else -> continue
                    }
                }

                pagerState.dispatchRawDelta(-delta.x)
                change.consume()
            }
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    pageCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, pageCount - 1)
    return when {
        totalDx > 0f -> (currentPage - 1).coerceAtLeast(0)
        totalDx < 0f -> (currentPage + 1).coerceAtMost(pageCount - 1)
        else -> currentPage
    }
}

/**
 * Calculates 11-stop smootherstep vertical melt scrim color stops:
 * - Curve: a(t) = 6t^5 - 15t^4 + 10t^3 starting at 30% of hero height, reaching 1.000 at 95%
 * - Zero RGB gray-fringe: uses exact baseBg RGB at 0 alpha instead of Color.Transparent
 * - LCD mode: drifts toward dominant muted color in middle (k(t) = 0.25 * 4t(1-t)) and returns to pure baseBg
 * - AMOLED mode: pure #000000 with 0 tint for 100% 0-nit OLED shutoff
 */
internal fun calculateHeroMeltColorStops(
    baseBg: Color,
    dominantColor: Color,
    isAmoled: Boolean
): Array<Pair<Float, Color>> {
    val tValues = floatArrayOf(0.0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
    val fractions = floatArrayOf(0.300f, 0.365f, 0.430f, 0.495f, 0.560f, 0.625f, 0.690f, 0.755f, 0.820f, 0.885f, 0.950f)
    val alphas = floatArrayOf(0.000f, 0.009f, 0.058f, 0.163f, 0.317f, 0.500f, 0.683f, 0.837f, 0.942f, 0.991f, 1.000f)

    val stops = mutableListOf<Pair<Float, Color>>()
    val zeroAlphaBg = baseBg.copy(alpha = 0f)
    stops.add(0.0f to zeroAlphaBg)

    for (i in tValues.indices) {
        val t = tValues[i]
        val fraction = fractions[i]
        val alpha = alphas[i]

        val rgbColor = if (isAmoled) {
            baseBg
        } else {
            val k = (0.25f * 4f * t * (1f - t)).coerceIn(0f, 0.25f)
            androidx.compose.ui.graphics.lerp(baseBg, dominantColor, k)
        }
        stops.add(fraction to rgbColor.copy(alpha = alpha))
    }

    stops.add(1.0f to baseBg.copy(alpha = 1.0f))
    return stops.toTypedArray()
}

/**
 * Calculates HUD contrast scrim stops:
 * - Runs from 48dp above top of logo down to hero bottom edge
 * - Alpha rises over the first 60% of its height with smootherstep from 0 to 0.40, then holds at 0.40
 */
internal fun calculateHudScrimColorStops(
    baseBg: Color
): Array<Pair<Float, Color>> {
    val zeroAlphaBg = baseBg.copy(alpha = 0f)
    val maxHudAlpha = 0.40f

    return arrayOf(
        0.00f to zeroAlphaBg,
        0.12f to baseBg.copy(alpha = maxHudAlpha * 0.058f),
        0.24f to baseBg.copy(alpha = maxHudAlpha * 0.317f),
        0.36f to baseBg.copy(alpha = maxHudAlpha * 0.683f),
        0.48f to baseBg.copy(alpha = maxHudAlpha * 0.942f),
        0.60f to baseBg.copy(alpha = maxHudAlpha),
        1.00f to baseBg.copy(alpha = maxHudAlpha)
    )
}
