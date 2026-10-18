package com.euthopiar.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.euthopiar.ui.library.LibraryColors

/**
 * 2x2 mosaic of four posters:
 * - Edge to edge inside the given shape/bounds
 * - Flat translucent #0B0B0F overlay (about 45% - 55%) for legibility
 * - Handles 0, 1, 2, 3, or 4 posters gracefully
 */
@Composable
fun PlaylistMosaic(
    posters: List<String?>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    overlayAlpha: Float = 0.50f
) {
    val shape: Shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else androidx.compose.ui.graphics.RectangleShape
    val validPosters = posters.filter { !it.isNullOrBlank() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(LibraryColors.Surface)
    ) {
        if (validPosters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LibraryColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = LibraryColors.TextMutedDecorative,
                    modifier = Modifier.fillMaxSize(0.35f)
                )
            }
        } else if (validPosters.size == 1) {
            AsyncImage(
                model = validPosters[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (validPosters.size == 2) {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = validPosters[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                AsyncImage(
                    model = validPosters[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            // 3 or 4 posters -> 2x2 mosaic
            val p0 = validPosters.getOrNull(0)
            val p1 = validPosters.getOrNull(1)
            val p2 = validPosters.getOrNull(2)
            val p3 = validPosters.getOrNull(3) ?: validPosters.getOrNull(0)

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (p0 != null) {
                            AsyncImage(
                                model = p0,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (p1 != null) {
                            AsyncImage(
                                model = p1,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (p2 != null) {
                            AsyncImage(
                                model = p2,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (p3 != null) {
                            AsyncImage(
                                model = p3,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Flat translucent #0B0B0F overlay across the image (no gradients)
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LibraryColors.Background.copy(alpha = overlayAlpha))
            )
        }
    }
}
