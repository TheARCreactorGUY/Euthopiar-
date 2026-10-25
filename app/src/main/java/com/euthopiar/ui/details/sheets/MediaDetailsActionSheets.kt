package com.euthopiar.ui.details.sheets

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.euthopiar.data.repository.Playlist
import com.euthopiar.data.repository.PlaylistItem
import com.euthopiar.data.repository.PlaylistRepository
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.sanitizeTitle
import com.euthopiar.ui.navigation.LocalNavbarVisible
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SheetScreen {
    WATCHLIST_CATEGORY,
    ADD_TO_PLAYLIST,
    NEW_PLAYLIST_NAMING
}

/**
 * Unified Frosted Glass Bottom Sheet for Media Details:
 * Handles:
 * 1. Watchlist Category selection
 * 2. Add to Playlist (Empty, Populated)
 * 3. In-sheet "New playlist" creation flow
 *
 * Implements smooth 60fps in-sheet transitions with back stack preservation.
 */
@Composable
fun MediaDetailsActionSheet(
    mediaId: String,
    title: String,
    posterUrl: String? = null,
    backdropUrl: String? = null,
    isTvSeries: Boolean = false,
    year: Int? = null,
    provider: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    episodeData: String? = null,
    initialScreen: SheetScreen = SheetScreen.WATCHLIST_CATEGORY,
    watchlistStatus: String? = null,
    onSelectWatchlistCategory: ((categoryKey: String, categoryLabel: String) -> Unit)? = null,
    onRemoveFromWatchlist: (() -> Unit)? = null,
    onPlaylistAction: ((message: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val playlistRepository = remember { PlaylistRepository.getInstance(context) }
    val userPlaylists by playlistRepository.userPlaylists.collectAsState()
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val navbarVisible = LocalNavbarVisible.current

    DisposableEffect(Unit) {
        navbarVisible.value = false
        onDispose {
            navbarVisible.value = true
        }
    }

    val sheetProgress = remember { Animatable(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            sheetProgress.snapTo(1f)
        } else {
            sheetProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                if (reduceMotion) {
                    onDismiss()
                } else {
                    sheetProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
                    )
                    onDismiss()
                }
            }
        }
    }

    val cleanTitle = remember(title) { sanitizeTitle(title) }

    val playlistItem = remember(mediaId, title, posterUrl, backdropUrl, isTvSeries, seasonNumber, episodeNumber, episodeTitle, episodeData) {
        PlaylistItem(
            mediaId = mediaId,
            title = cleanTitle,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            isTvSeries = isTvSeries,
            year = year,
            provider = provider,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle?.let { sanitizeTitle(it) },
            episodeData = episodeData
        )
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }
    // Track if navigated into playlist from watchlist
    var navigatedFromWatchlist by remember { mutableStateOf(initialScreen == SheetScreen.WATCHLIST_CATEGORY) }

    // Intercept back key: Naming -> Playlist -> Watchlist (if opened from watchlist) -> Close
    BackHandler {
        when (currentScreen) {
            SheetScreen.NEW_PLAYLIST_NAMING -> currentScreen = SheetScreen.ADD_TO_PLAYLIST
            SheetScreen.ADD_TO_PLAYLIST -> {
                if (navigatedFromWatchlist && initialScreen == SheetScreen.WATCHLIST_CATEGORY) {
                    currentScreen = SheetScreen.WATCHLIST_CATEGORY
                } else {
                    dismissWithAnim()
                }
            }
            SheetScreen.WATCHLIST_CATEGORY -> dismissWithAnim()
        }
    }

    val isAmoled = LocalAmoledMode.current
    val animValue = sheetProgress.value
    val maxSheetHeight = (configuration.screenHeightDp * 0.85f).dp
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheetFill = if (isAmoled) Color.Black.copy(alpha = 0.88f) else Color(0xFF0F101A).copy(alpha = 0.70f)
    val fallbackFill = if (isAmoled) Color.Black.copy(alpha = 0.94f) else Color(0xFF0F101A).copy(alpha = 0.94f)
    val scrimAlpha = if (reduceMotion) 0.60f else (0.60f * (animValue / (250f / 340f))).coerceIn(0f, 0.60f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissWithAnim
            )
    ) {
        // Scrim: 60% black fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Sheet Container: slides up from bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .onGloballyPositioned { coordinates ->
                    sheetHeightPx = coordinates.size.height.toFloat()
                }
                .offset {
                    val slideDistance = if (sheetHeightPx > 0f) sheetHeightPx else with(density) { maxSheetHeight.toPx() }
                    val currentY = (1f - animValue) * slideDistance + dragOffsetY
                    IntOffset(0, currentY.roundToInt())
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through */ }
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || dragOffsetY > 0) {
                            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragOffsetY > 120f) {
                            dismissWithAnim()
                        } else {
                            scope.launch {
                                Animatable(dragOffsetY).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffsetY = value
                                }
                            }
                        }
                    }
                )
                .clip(sheetShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 24.dp
                            tints = listOf(HazeTint(sheetFill))
                            fallbackTint = HazeTint(fallbackFill)
                        }
                    } else {
                        Modifier.background(fallbackFill)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = sheetShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp)
            ) {
                // Drag handle: 36 by 4 dp, white 28%
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
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                        } else {
                            val isMovingForward = when {
                                initialState == SheetScreen.WATCHLIST_CATEGORY && targetState == SheetScreen.ADD_TO_PLAYLIST -> true
                                initialState == SheetScreen.ADD_TO_PLAYLIST && targetState == SheetScreen.NEW_PLAYLIST_NAMING -> true
                                else -> false
                            }
                            if (isMovingForward) {
                                (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { 24 } + fadeIn(tween(200)))
                                    .togetherWith(slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { -24 } + fadeOut(tween(150)))
                            } else {
                                (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -24 } + fadeIn(tween(200)))
                                    .togetherWith(slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { 24 } + fadeOut(tween(150)))
                            }
                        }
                    },
                    label = "sheetContentTransition"
                ) { screen ->
                    when (screen) {
                        SheetScreen.WATCHLIST_CATEGORY -> {
                            WatchlistCategoryContent(
                                title = cleanTitle,
                                currentStatus = watchlistStatus,
                                onSelectCategory = { statusKey, label ->
                                    onSelectWatchlistCategory?.invoke(statusKey, label)
                                    scope.launch {
                                        delay(250)
                                        dismissWithAnim()
                                    }
                                },
                                onNavigateToPlaylist = {
                                    navigatedFromWatchlist = true
                                    currentScreen = SheetScreen.ADD_TO_PLAYLIST
                                },
                                onRemoveFromWatchlist = {
                                    onRemoveFromWatchlist?.invoke()
                                    scope.launch {
                                        delay(150)
                                        dismissWithAnim()
                                    }
                                }
                            )
                        }

                        SheetScreen.ADD_TO_PLAYLIST -> {
                            AddToPlaylistContent(
                                title = cleanTitle,
                                playlists = userPlaylists,
                                playlistItem = playlistItem,
                                hasBackButton = navigatedFromWatchlist && initialScreen == SheetScreen.WATCHLIST_CATEGORY,
                                onBack = { currentScreen = SheetScreen.WATCHLIST_CATEGORY },
                                onNewPlaylistClick = { currentScreen = SheetScreen.NEW_PLAYLIST_NAMING },
                                onTogglePlaylist = { pl, isAlreadyIn ->
                                    if (isAlreadyIn) {
                                        playlistRepository.removeItemFromPlaylist(pl.id, playlistItem.uniqueKey)
                                        onPlaylistAction?.invoke("Removed from ${pl.title}")
                                    } else {
                                        playlistRepository.addItemToPlaylist(pl.id, playlistItem)
                                        onPlaylistAction?.invoke("Added to ${pl.title}")
                                    }
                                }
                            )
                        }

                        SheetScreen.NEW_PLAYLIST_NAMING -> {
                            NewPlaylistNamingContent(
                                existingPlaylists = userPlaylists,
                                onBack = { currentScreen = SheetScreen.ADD_TO_PLAYLIST },
                                onConfirm = { newName ->
                                    val created = playlistRepository.createPlaylist(newName)
                                    playlistRepository.addItemToPlaylist(created.id, playlistItem)
                                    onPlaylistAction?.invoke("Added to ${created.title}")
                                    dismissWithAnim()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sheet 1: Watchlist Category Content
 */
@Composable
private fun WatchlistCategoryContent(
    title: String,
    currentStatus: String?,
    onSelectCategory: (statusKey: String, label: String) -> Unit,
    onNavigateToPlaylist: () -> Unit,
    onRemoveFromWatchlist: () -> Unit
) {
    var selectedCategoryKey by remember(currentStatus) { mutableStateOf(currentStatus) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        // Header block
        Text(
            text = "Add to watchlist",
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                lineHeight = (26 * 1.05).sp,
                letterSpacing = (-0.02).sp,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.55f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))

        // Category rows
        val categories = listOf(
            Triple("TO_BE_WATCHED", "To be watched", "Save it for later"),
            Triple("WATCHING", "Watching", "Pick up where you left off"),
            Triple("COMPLETED", "Completed", "Finished it")
        )

        categories.forEachIndexed { index, (key, label, hint) ->
            val icon = when (key) {
                "TO_BE_WATCHED" -> Icons.Outlined.BookmarkBorder
                "WATCHING" -> Icons.Outlined.PlayArrow
                else -> Icons.Outlined.CheckCircle
            }
            val isSelected = selectedCategoryKey == key

            SheetActionRow(
                icon = icon,
                title = label,
                hint = hint,
                isSelected = isSelected,
                showRadio = true,
                onClick = {
                    selectedCategoryKey = key
                    onSelectCategory(key, label)
                }
            )

            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))
        }

        // "Add to playlist" row
        SheetActionRow(
            icon = Icons.Outlined.PlaylistAdd,
            title = "Add to playlist",
            hint = null,
            trailingIcon = Icons.Outlined.ChevronRight,
            onClick = onNavigateToPlaylist
        )

        // "Remove from watchlist" action (only if already on watchlist)
        if (!currentStatus.isNullOrBlank()) {
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = onRemoveFromWatchlist
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Remove from watchlist",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                )
            }
        }
    }
}

/**
 * Sheet 2: Add to Playlist Content
 */
@Composable
private fun AddToPlaylistContent(
    title: String,
    playlists: List<Playlist>,
    playlistItem: PlaylistItem,
    hasBackButton: Boolean,
    onBack: () -> Unit,
    onNewPlaylistClick: () -> Unit,
    onTogglePlaylist: (Playlist, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        // Header block
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasBackButton) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to categories",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add to playlist",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 26.sp,
                        lineHeight = (26 * 1.05).sp,
                        letterSpacing = (-0.02).sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))

        // "New playlist" row
        SheetActionRow(
            icon = Icons.Outlined.Add,
            title = "New playlist",
            hint = "Name it and add this title",
            trailingIcon = Icons.Outlined.ChevronRight,
            onClick = onNewPlaylistClick
        )

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))

        if (playlists.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistAdd,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "No playlists yet",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Create one to group titles you love.",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))
        } else {
            // Populated State (scrollable up to 75% screen height)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(
                    items = playlists,
                    key = { it.id }
                ) { playlist ->
                    val isAlreadyIn = remember(playlist.items, playlistItem.uniqueKey) {
                        playlist.items.any { it.uniqueKey == playlistItem.uniqueKey }
                    }

                    val firstPoster = remember(playlist.items) {
                        playlist.items.firstNotNullOfOrNull { it.posterUrl ?: it.backdropUrl }
                    }

                    SheetPlaylistRow(
                        playlist = playlist,
                        posterUrl = firstPoster,
                        isSelected = isAlreadyIn,
                        onClick = { onTogglePlaylist(playlist, isAlreadyIn) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))
                }
            }
        }
    }
}

/**
 * Sheet 2 - Subpage: New Playlist Naming Step
 */
@Composable
private fun NewPlaylistNamingContent(
    existingPlaylists: List<Playlist>,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val reduceMotion = LocalReduceMotion.current
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val trimmed = name.trim()
    val isDuplicate = remember(trimmed, existingPlaylists) {
        trimmed.isNotEmpty() && existingPlaylists.any { it.title.equals(trimmed, ignoreCase = true) }
    }

    fun triggerShake() {
        if (!reduceMotion) {
            scope.launch {
                shakeOffset.animateTo(4f, tween(40))
                shakeOffset.animateTo(-4f, tween(40))
                shakeOffset.animateTo(3f, tween(40))
                shakeOffset.animateTo(-3f, tween(40))
                shakeOffset.animateTo(0f, tween(40))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        // Header with Back Chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "New playlist",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.sp,
                    lineHeight = (26 * 1.05).sp,
                    letterSpacing = (-0.02).sp,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glass Text Field (46dp tall, 0.5px hairline, 14-16dp radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(
                    width = 0.5.dp,
                    color = if (errorMessage != null) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (name.isEmpty()) {
                Text(
                    text = "Playlist name",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.40f)
                    )
                )
            }

            BasicTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 40) {
                        name = it
                        errorMessage = null
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = Color.White
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        // Live counter shown near limit (length >= 30) or inline validation error
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (name.length >= 30) {
                Text(
                    text = "${name.length}/40",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // White Pill Button "Create and add"
        val isButtonActive = trimmed.isNotEmpty() && !isDuplicate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isButtonActive) Color.White else Color.White.copy(alpha = 0.30f))
                .clickable(
                    role = Role.Button,
                    onClick = {
                        if (trimmed.isEmpty()) {
                            errorMessage = "Enter a name"
                            triggerShake()
                        } else if (isDuplicate) {
                            errorMessage = "You already have a playlist with that name"
                            triggerShake()
                        } else {
                            onConfirm(trimmed)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create and add",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isButtonActive) Color(0xFF0B0B0F) else Color(0xFF0B0B0F).copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * Standard Action Row inside sheets:
 * - >=48dp touch target
 * - 36x36 glass icon tile
 * - Title 16sp, hint 12sp
 * - Selection indicator or chevron
 */
@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    hint: String?,
    isSelected: Boolean = false,
    showRadio: Boolean = false,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "rowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = if (showRadio) Role.RadioButton else Role.Button,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glass Icon Tile: 36x36 px, 12 px radius
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hint,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showRadio) {
            Spacer(modifier = Modifier.width(12.dp))
            RoundSelectionIndicator(isSelected = isSelected)
        } else if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Playlist row with thumbnail or icon tile and round selection indicator.
 */
@Composable
private fun SheetPlaylistRow(
    playlist: Playlist,
    posterUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "plRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or glass tile
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sanitizeTitle(playlist.title),
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${playlist.items.size} titles",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        RoundSelectionIndicator(isSelected = isSelected)
    }
}

/**
 * 22px round selection indicator:
 * - Unselected: hollow circle with 30% white outline
 * - Selected: solid white (92%) circle with dark check mark
 */
@Composable
fun RoundSelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "checkScale"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(Color.White.copy(alpha = 0.92f))
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = Color(0xFF0B0B0F),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
            )
        }
    }
}
