package com.euthopiar.ui.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.library.LibraryColors
import com.euthopiar.ui.library.LibraryDimens
import com.euthopiar.ui.theme.LocalReduceMotion

/**
 * 22dp circle selection indicator:
 * - Unselected: hairline circle (10% white, 0.5dp)
 * - Selected: fills white with dark check, animated with quick spring
 */
@Composable
fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = if (reduceMotion) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "selectSpring"
    )

    Box(
        modifier = modifier
            .size(LibraryDimens.selectCircleSize)
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(LibraryColors.TextWhite)
                } else {
                    Modifier
                        .background(LibraryColors.Background.copy(alpha = 0.6f))
                        .border(1.dp, LibraryColors.Hairline.copy(alpha = 0.4f), CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = LibraryColors.Background,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
