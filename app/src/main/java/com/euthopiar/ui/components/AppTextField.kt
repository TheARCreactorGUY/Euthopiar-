package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*

/**
 * Standard text input field styled with SurfaceMid, ShapeM, hairline Outline border,
 * focus/error states, and optional paste & clear actions.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    showPasteButton: Boolean = false,
    showClearButton: Boolean = true,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val borderColor = when {
        isError -> Error
        isFocused -> TextSecondary
        else -> Outline
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight)
                .clip(ShapeM)
                .background(SurfaceMid)
                .border(HairlineWidth, borderColor, ShapeM)
                .tvFocusable(shape = ShapeM)
                .padding(horizontal = Dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null, // decorative - input field purpose conveyed by placeholder/label
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(Dimens.iconM)
                        .padding(end = Dimens.spacing8)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppTypography.bodyMedium,
                        color = TextTertiary
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    textStyle = AppTypography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }

            if (showClearButton && value.isNotEmpty()) {
                AppIconButton(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Clear text",
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(Dimens.buttonHeight),
                    tint = TextSecondary,
                    iconSize = Dimens.iconS
                )
            }

            if (showPasteButton) {
                AppIconButton(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = "Paste from clipboard",
                    onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrBlank()) {
                            onValueChange(clipText)
                        }
                    },
                    modifier = Modifier.size(Dimens.buttonHeight),
                    tint = TextSecondary,
                    iconSize = Dimens.iconS
                )
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.spacing4))
            Text(
                text = errorMessage,
                style = AppTypography.labelSmall,
                color = Error,
                modifier = Modifier.padding(start = Dimens.spacing4)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppTextFieldPreview() {
    AppTextField(
        value = "https://example.com/repo.json",
        onValueChange = {},
        placeholder = "Enter URL...",
        showPasteButton = true
    )
}
