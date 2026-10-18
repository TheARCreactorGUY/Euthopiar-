package com.euthopiar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.euthopiar.ui.theme.ShapeS
import com.euthopiar.ui.theme.TextPrimary

/**
 * Flagship TV focus modifier providing:
 * - Proper modifier ordering: zIndex, onFocusChanged, optional D-pad click, focusable, scale, and focus styling
 * - Spring-animated scale with zIndex elevation so scaled cards never clip under siblings
 * - Soft shadow glow and crisp 2dp white focus ring when focused
 * - Full D-pad Center / Enter click event forwarding
 */
fun Modifier.tvFocusable(
    shape: Shape = ShapeS,
    focusedScale: Float = 1.05f,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tvFocusScale"
    )

    this
        .zIndex(if (isFocused) 1f else 0f)
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (onClick != null) {
                Modifier.onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        onClick()
                        true
                    } else false
                }
            } else Modifier
        )
        .focusable(enabled = enabled)
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = shape,
                        spotColor = Color.White.copy(alpha = 0.35f),
                        ambientColor = Color.White.copy(alpha = 0.20f)
                    )
                    .border(2.dp, TextPrimary, shape)
            } else {
                Modifier
            }
        )
}
