package com.euthopiar.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.theme.pressable

/**
 * Restyled monochrome glass choice card (kept for backward compatibility).
 * Pure monochrome, zero red.
 */
@Composable
fun OnboardingChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else OnboardingTokens.GlassFill,
        animationSpec = tween(150),
        label = "choiceCardBg",
    )

    val borderColor = if (isSelected) Color.White.copy(alpha = 0.40f) else OnboardingTokens.GlassHairlineBorder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(animatedBgColor)
            .border(width = OnboardingTokens.HairlineWidth, color = borderColor, shape = shape)
            .pressable(
                scaleDown = 0.98f,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OnboardingTokens.TextDarkButton else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = OnboardingTokens.TextDarkButton,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
