package com.euthopiar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.TextTertiary

/**
 * Standard bullet separator composable ("·") with normalized UTF-8 encoding.
 */
@Composable
fun Dot(
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.labelSmall
) {
    Text(
        text = "·",
        style = style,
        color = TextTertiary,
        modifier = modifier.padding(horizontal = Dimens.spacing4)
    )
}

/**
 * Shared metadata line joining elements (e.g. Year, Rating, Runtime, Genre) with clean dot separators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetaLine(
    items: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.labelSmall
) {
    val nonBlankItems = items.filter { it.isNotBlank() }
    if (nonBlankItems.isEmpty()) return

    FlowRow(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        nonBlankItems.forEachIndexed { index, item ->
            if (index > 0) {
                Dot(style = style)
            }
            Text(
                text = item,
                style = style,
                color = TextTertiary
            )
        }
    }
}
