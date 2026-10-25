package com.euthopiar.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.components.tvFocusable
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState

/**
 * 1. STEP PROGRESS BAR
 * Three equal segments across the content width, 2 dp tall, 4 dp gap, fully rounded.
 * A segment for current step and completed steps is white 90%. Upcoming steps are white 18%.
 * Announced to screen readers as "Step N of 3".
 */
@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier,
) {
    val stepDescription = "Step $currentStep of $totalSteps"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .semantics {
                this.contentDescription = stepDescription
                this.stateDescription = stepDescription
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..totalSteps) {
            val isCompletedOrCurrent = i <= currentStep
            val targetColor = if (isCompletedOrCurrent) OnboardingTokens.TextWhite90 else OnboardingTokens.TextWhite18
            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "progressSegment_$i",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(animatedColor),
            )
        }
    }
}

/**
 * 2. TOP ROW (40 dp tall, 14 dp below the progress bar):
 * - Page 1 (Welcome): NOTHING on left. Skip chip on right. No logo, icon, or brand mark.
 * - Pages 2 & 3: Glass back button on left (40 dp circle, outline left arrow 20 dp, mirrored in RTL)
 *   and Skip chip on right.
 * - Skip chip: glass pill, 32 dp tall, 14 dp horizontal padding, label "Skip" 12 sp Medium white,
 *   invisible 48 dp touch target.
 */
@Composable
fun OnboardingTopRow(
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            OnboardingBackButton(
                onClick = onBack,
                hazeState = hazeState,
                isAmoled = isAmoled,
            )
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        OnboardingSkipChip(
            onClick = onSkip,
            hazeState = hazeState,
            isAmoled = isAmoled,
        )
    }
}

/**
 * Glass back button: 40 dp circle, 0.5 dp hairline border, outline arrow 20 dp (mirrored in RTL).
 * Minimum 48 dp touch target.
 */
@Composable
fun OnboardingBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(120),
        label = "backPressScale",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Back",
                onClick = onClick,
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .onboardingGlassSurface(
                    shape = CircleShape,
                    hazeState = hazeState,
                    isAmoled = isAmoled,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Skip chip: glass pill, 32 dp tall, 14 dp horizontal padding, label "Skip" 12 sp Medium white.
 * Minimum 48 dp touch target.
 */
@Composable
fun OnboardingSkipChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(120),
        label = "skipPressScale",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Skip onboarding",
                onClick = onClick,
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .onboardingGlassSurface(
                    shape = CircleShape,
                    hazeState = hazeState,
                    isAmoled = isAmoled,
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Skip",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White,
                ),
            )
        }
    }
}

/**
 * WHITE PILL (Primary action button on every page).
 * - Solid white fill, near-black label (#0B0B0F)
 * - Full content width, 52 dp tall, fully rounded
 * - Label 15 sp Medium with 18 dp outline right-arrow icon after label (8 dp gap; mirrored in RTL)
 * - No border, no shadow
 * - Pressed state: scale to 98% (NOT 94), white fill drops to ~88%, 100-150 ms
 * - Loading state: 16 dp thin near-black spinner
 */
@Composable
fun OnboardingWhitePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    showArrow: Boolean = true,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "onboardingPillScale",
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.88f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "onboardingPillAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = backgroundAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled && !isLoading,
                onClick = onClick,
            )
            .tvFocusable(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = OnboardingTokens.TextDarkButton,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = OnboardingTokens.TextDarkButton,
                    ),
                )

                if (showArrow) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon ?: Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = OnboardingTokens.TextDarkButton,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * SEGMENTED CONTROL for Choice Blocks on Page 3.
 * - Glass container: 14 dp radius, 3 dp inner padding, 2 dp gap
 * - Segments: 38 dp tall, 11 dp radius, label 13 sp Medium
 * - Unselected: transparent with white 70% label
 * - Selected: solid white fill with #0B0B0F label
 * - Smooth 200 ms selection transition
 */
@Composable
fun <T> OnboardingSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isAmoled: Boolean = false,
) {
    val reduceMotion = LocalReduceMotion.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onboardingGlassSurface(
                shape = RoundedCornerShape(14.dp),
                hazeState = hazeState,
                isAmoled = isAmoled,
            )
            .padding(3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = labelProvider(option)

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = if (reduceMotion) tween(0) else tween(200, easing = FastOutSlowInEasing),
                    label = "segmentBg_${label}",
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) OnboardingTokens.TextDarkButton else OnboardingTokens.TextMuted70,
                    animationSpec = if (reduceMotion) tween(100) else tween(120),
                    label = "segmentText_${label}",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(backgroundColor)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onOptionSelected(option) },
                        )
                        .tvFocusable(shape = RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = textColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
