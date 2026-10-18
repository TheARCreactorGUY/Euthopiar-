package com.euthopiar.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.core.repository.ProviderRepository
import com.euthopiar.ui.components.EmptyState
import com.euthopiar.ui.components.LocalSnackbarController
import com.euthopiar.ui.components.PosterCard
import com.euthopiar.ui.components.SearchGridSkeleton
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.*
import com.euthopiar.ui.util.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InPluginSearchOverlay(
    onDismiss: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    registry: PluginRegistry = PluginRegistry.Default,
    repository: ProviderRepository = ProviderRepository.Default,
    modifier: Modifier = Modifier
) {
    val activePlugin by registry.activePlugin.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarController = LocalSnackbarController.current
    val preferencesRepository = remember { com.euthopiar.data.preferences.PreferencesRepository.getInstance(context) }
    val profileManager = remember { com.euthopiar.data.profile.ProfileManager.getInstance(context) }
    val activeProfile by profileManager.activeProfile.collectAsState()
    val extraContentEnabled by preferencesRepository.extraContentEnabled.collectAsState(initial = false)

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val windowDimensions = rememberWindowDimensions()
    val gridColumns = when (windowDimensions.widthClass) {
        WindowWidthClass.Expanded -> 6
        WindowWidthClass.Medium -> 5
        WindowWidthClass.Compact -> 3
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun triggerSearch(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            results = emptyList()
            isLoading = false
            return
        }
        searchJob = scope.launch {
            delay(300)
            isLoading = true
            try {
                val searchResults = repository.search(q.trim(), isGlobal = false)
                results = searchResults
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                results = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = windowDimensions.horizontalMargin)
        ) {
            // Search Input Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spacing12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (48dp touch target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ShapeFull)
                        .tvFocusable(shape = ShapeFull)
                        .pressable(role = Role.Button, onClickLabel = "Close search") { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.spacing8))

                // Search Box Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(ShapeFull)
                        .background(SurfaceMid)
                        .border(HairlineWidth, Outline, ShapeFull)
                        .padding(horizontal = Dimens.spacing16),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null, // decorative - search field is labeled by placeholder and input semantics
                            tint = if (query.isNotEmpty()) TextPrimary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(Dimens.spacing8))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search in ${activePlugin.name}...",
                                    style = AppTypography.bodyLarge,
                                    color = TextTertiary
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    triggerSearch(it)
                                },
                                singleLine = true,
                                textStyle = AppTypography.bodyLarge.copy(color = TextPrimary),
                                cursorBrush = SolidColor(Accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                    triggerSearch(query)
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        if (query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .tvFocusable(shape = CircleShape)
                                    .pressable(role = Role.Button, onClickLabel = "Clear search") {
                                        query = ""
                                        results = emptyList()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(ShapeFull)
                                        .background(SurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scoped Provider Subtitle Indicator (no developer URLs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Text(
                    text = "Searching in ${activePlugin.name}",
                    style = AppTypography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing12))

            // Body Content: Loading / Empty / Responsive Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    SearchGridSkeleton()
                } else if (query.isNotEmpty() && results.isEmpty()) {
                    EmptyState(
                        title = "No results found",
                        message = "No matches found for \"$query\" in ${activePlugin.name}."
                    )
                } else if (query.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type to search movies and series",
                            style = AppTypography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                } else {
                    val distinctResults = remember(results) { results.distinctBy { it.id } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.posterGap),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacing12),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(distinctResults, key = { it.id }) { item ->
                            PosterCard(
                                id = item.id,
                                posterUrl = item.posterUrl ?: item.backdropUrl,
                                title = item.title,
                                year = item.year,
                                type = when (item.type) {
                                    com.euthopiar.core.model.MediaType.MOVIE -> "Movie"
                                    com.euthopiar.core.model.MediaType.TV_SERIES -> "Series"
                                    com.euthopiar.core.model.MediaType.ANIME -> "Anime"
                                },
                                onClick = { onMediaClick(item) },
                                width = Dp.Unspecified
                            )
                        }
                    }
                }
            }
        }

    }
}
