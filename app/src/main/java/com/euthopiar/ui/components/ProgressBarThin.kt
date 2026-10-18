package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.SurfaceHigh

/**
 * Standard 3dp slim progress bar used by Continue Watching, Details resume, and Downloads.
 * Styled with Accent color and SurfaceHigh track.
 */
@Composable
fun ProgressBarThin(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    trackColor: Color = SurfaceHigh
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(ShapeFull)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            }
    ) {
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(ShapeFull)
                    .background(color)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun ProgressBarThinPreview() {
    ProgressBarThin(progress = 0.65f)
}
