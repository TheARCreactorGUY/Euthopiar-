package com.euthopiar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.MediaItem
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.rememberWindowDimensions

/**
 * Modern Netflix-style Content Row with tight 8dp poster gaps and SectionHeader.
 */
@Composable
fun ContentRow(
    row: CatalogRow,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    sectionLabelStyle: com.euthopiar.data.preferences.SectionLabelStyle = com.euthopiar.data.preferences.SectionLabelStyle.AccentBar,
    onItemLongClick: ((MediaItem) -> Unit)? = null,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (row.items.isEmpty()) return

    val windowDimensions = rememberWindowDimensions()
    val rowListState = androidx.compose.runtime.saveable.rememberSaveable(
        row.title,
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacing8)
    ) {
        SectionHeader(
            title = row.title,
            style = sectionLabelStyle,
            subtitle = row.subtitle,
            eyebrow = row.eyebrow,
            onSeeAll = onSeeAllClick
        )

        LazyRow(
            state = rowListState,
            contentPadding = PaddingValues(horizontal = windowDimensions.horizontalMargin),
            horizontalArrangement = Arrangement.spacedBy(Dimens.posterGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = row.items,
                key = { index, item -> "${item.id}_${item.provider ?: ""}_$index" },
                contentType = { _, _ -> "media_card" }
            ) { _, item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongClick = onItemLongClick?.let { { it(item) } }
                )
            }
        }
    }
}
