package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*

/**
 * Standard grouped card container for settings items.
 * Optional category header title and leading icon.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacing4, vertical = Dimens.spacing8)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null, // decorative - section title conveys category
                        tint = TextTertiary,
                        modifier = Modifier.size(Dimens.iconS)
                    )
                }
                Text(
                    text = title.uppercase(),
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeM)
                .background(SurfaceLow)
                .border(HairlineWidth, Outline, ShapeM),
            content = content
        )
    }
}

/**
 * Standard 56dp settings row with title, optional subtitle,
 * and trailing switch, chevron, value label, or custom trailing content.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    showChevron: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val effectiveOnClick = onClick ?: if (checked != null && onCheckedChange != null) {
        { onCheckedChange(!checked) }
    } else null
    val isClickable = effectiveOnClick != null && enabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(
                if (isClickable) {
                    Modifier
                        .tvFocusable(shape = ShapeM)
                        .pressable(
                            enabled = enabled,
                            role = if (checked != null) Role.Switch else Role.Button,
                            onClickLabel = title,
                            onClick = effectiveOnClick!!
                        )
                } else Modifier
            )
            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyLarge,
                color = if (enabled) TextPrimary else TextTertiary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = AppTypography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
            Spacer(modifier = Modifier.width(Dimens.spacing8))
        }

        if (value != null) {
            Text(
                text = value,
                style = AppTypography.labelMedium,
                color = TextTertiary
            )
            if (showChevron || onClick != null) {
                Spacer(modifier = Modifier.width(Dimens.spacing8))
            }
        }

        if (checked != null) {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        } else if (showChevron || (onClick != null && trailingContent == null && value == null)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null, // decorative - chevron navigation indicator
                tint = TextTertiary,
                modifier = Modifier.size(Dimens.spacing12)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun SettingsComponentsPreview() {
    SettingsGroup(title = "General Settings") {
        SettingsRow(
            title = "Display Language",
            subtitle = "Select system audio and subtitle preferences",
            value = "English",
            onClick = {}
        )
        SettingsRow(
            title = "Auto-Play Next Episode",
            subtitle = "Automatically queue next chapter when credits roll",
            checked = true,
            onCheckedChange = {}
        )
    }
}
