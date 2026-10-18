package com.euthopiar.ui.library.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.library.LibraryTypography
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * Editorial storage meter:
 * - 3dp tall, track at 10% white, fully rounded ends.
 * - White segment = downloaded size.
 * - Crimson segment (2dp gap after white) = in-progress size.
 * - Remainder = free space.
 * - Captions below:
 *   Left: "3 titles ready · 1 downloading" (11sp muted)
 *   Right: "64 GB free" (11sp muted)
 */
@Composable
fun StorageMeter(
    downloadedFraction: Float,
    inProgressFraction: Float,
    readyCount: Int,
    downloadingCount: Int,
    freeSpaceText: String,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val whiteAnim = remember { Animatable(if (reduceMotion) downloadedFraction else 0f) }
    val crimsonAnim = remember { Animatable(if (reduceMotion) inProgressFraction else 0f) }

    LaunchedEffect(downloadedFraction, inProgressFraction, reduceMotion) {
        if (reduceMotion) {
            whiteAnim.snapTo(downloadedFraction)
            crimsonAnim.snapTo(inProgressFraction)
        } else {
            whiteAnim.animateTo(
                targetValue = downloadedFraction.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            delay(80)
            crimsonAnim.animateTo(
                targetValue = inProgressFraction.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(LibraryDimens.storageMeterHeight)
                .clip(CircleShape)
                .background(LibraryColors.Hairline)
        ) {
            val totalWidth = maxWidth
            val whiteWidth = totalWidth * whiteAnim.value.coerceIn(0f, 1f)
            val crimsonWidth = totalWidth * crimsonAnim.value.coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (whiteWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .width(whiteWidth)
                            .height(LibraryDimens.storageMeterHeight)
                            .clip(CircleShape)
                            .background(LibraryColors.TextWhite)
                    )
                }

                if (crimsonWidth > 0.dp) {
                    if (whiteWidth > 0.dp) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(crimsonWidth)
                            .height(LibraryDimens.storageMeterHeight)
                            .clip(CircleShape)
                            .background(LibraryColors.Crimson)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Captions under the meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val captionLeft = buildString {
                append("$readyCount title${if (readyCount != 1) "s" else ""} ready")
                if (downloadingCount > 0) {
                    append(" · $downloadingCount downloading")
                }
            }
            Text(
                text = captionLeft,
                style = LibraryTypography.sectionLabel,
                color = LibraryColors.TextMutedReadable
            )

            Text(
                text = freeSpaceText,
                style = LibraryTypography.sectionLabel,
                color = LibraryColors.TextMutedReadable
            )
        }
    }
}
