package com.euthopiar.ui.library.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.repository.Playlist
import com.euthopiar.ui.components.VelvetAlertDialog
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.library.components.LibraryGlassIconTile
import com.euthopiar.ui.library.components.PlaylistMosaic
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import com.euthopiar.ui.theme.pressable
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    gridState: LazyGridState,
    onDeletePlaylist: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (playlistToDelete != null) {
        val target = playlistToDelete!!
        VelvetAlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = "Delete playlist?",
            message = "Are you sure you want to delete \"${com.euthopiar.ui.library.sanitizeTitle(target.title)}\"? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                val id = target.id
                playlistToDelete = null
                onDeletePlaylist?.invoke(id)
            }
        )
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Uniform 2-column square grid for all playlists (no featured banner)
        itemsIndexed(
            items = playlists,
            key = { _, pl -> "playlist_${pl.id}" }
        ) { index, playlist ->
            StaggeredPlaylistTile(
                index = index,
                playlist = playlist,
                onClick = { onSelectPlaylist(playlist) },
                onLongClick = if (!playlist.isSystem && onDeletePlaylist != null) {
                    { playlistToDelete = playlist }
                } else null,
                hazeState = hazeState
            )
        }

        // Trailing tile completing the grid: "+ New playlist"
        item(key = "new_playlist_tile") {
            NewPlaylistTile(
                onClick = onCreatePlaylistClick,
                hazeState = hazeState
            )
        }
    }
}

/**
 * Uniform Square Playlist tile:
 * - Square cover with 14dp radius and 0.5px hairline border
 * - 2x2 collage of first 4 posters
 * - Small glass count pill in bottom-right corner ("4 titles", 11sp, 10dp radius, NO RED)
 * - Title 15sp Medium white
 * - Subline 12sp muted 55% white
 */
@Composable
private fun StaggeredPlaylistTile(
    index: Int,
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    hazeState: HazeState? = null
) {
    val reduceMotion = LocalReduceMotion.current
    val animProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion && index < 8) {
            delay((index * 35L).coerceAtMost(300L))
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val posters = remember(playlist.items) {
        playlist.items.mapNotNull { it.posterUrl ?: it.backdropUrl }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = (1f - animProgress.value) * 12.dp.value
            }
            .pressable(
                scaleDown = 0.98f,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(LibraryDimens.hairline, LibraryColors.GlassBorder, RoundedCornerShape(14.dp))
                .background(LibraryColors.Surface)
        ) {
            PlaylistMosaic(
                posters = posters,
                cornerRadius = 14.dp,
                overlayAlpha = 0.35f,
                modifier = Modifier.fillMaxSize()
            )

            // Small glass count pill in bottom-right corner ("4 titles", 11sp, 10dp radius, NO RED)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(LibraryDimens.hairline, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                val countText = "${playlist.items.size} ${if (playlist.items.size == 1) "title" else "titles"}"
                Text(
                    text = countText,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title: 15sp Medium, white, one line, ellipsis
        Text(
            text = com.euthopiar.ui.library.sanitizeTitle(playlist.title),
            style = LibraryTypography.title15,
            color = LibraryColors.TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subline: 12sp muted 55% white
        val countLabel = "${playlist.items.size} ${if (playlist.items.size == 1) "title" else "titles"}"
        Text(
            text = countLabel,
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = LibraryColors.TextMuted55
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Trailing "New playlist" tile:
 * - Square tile with 14dp radius
 * - Glass fill (5% white)
 * - DASHED 0.5px hairline border (25% white)
 * - Centered 40x40dp glass tile with plus icon
 * - Title "New playlist" 15sp Medium white
 * - Subline "Group titles you love" 12sp muted 55% white
 */
@Composable
private fun NewPlaylistTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(
                scaleDown = 0.98f,
                role = Role.Button,
                onClickLabel = "Create new playlist",
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .drawBehind {
                    val strokeWidth = 0.5.dp.toPx()
                    val cornerRadiusPx = 14.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        ),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            LibraryGlassIconTile(
                icon = Icons.Default.Add,
                contentDescription = null,
                size = 40.dp,
                radius = 14.dp,
                iconSize = 20.dp,
                hazeState = hazeState
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "New playlist",
            style = LibraryTypography.title15,
            color = LibraryColors.TextWhite,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Group titles you love",
            style = TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = LibraryColors.TextMuted55
            ),
            maxLines = 1
        )
    }
}
