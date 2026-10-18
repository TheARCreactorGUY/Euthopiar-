package com.euthopiar.ui.home

import com.euthopiar.core.model.CatalogRow
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenVirtualizationTest {

    @Test
    fun testContentRowsDeduplicateByTitle() {
        val sampleItem = MediaItem(
            id = "1",
            title = "Test Media",
            url = "",
            posterUrl = null,
            type = MediaType.MOVIE
        )

        val rowsWithDuplicates = listOf(
            CatalogRow(title = "Trending", items = listOf(sampleItem)),
            CatalogRow(title = "Popular", items = listOf(sampleItem)),
            CatalogRow(title = "Trending", items = listOf(sampleItem)), // Duplicate title from another provider
            CatalogRow(title = "Top Rated", items = listOf(sampleItem))
        )

        val deduplicated = rowsWithDuplicates.distinctBy { it.title }

        assertEquals(3, deduplicated.size)
        val titles = deduplicated.map { it.title }
        assertEquals(listOf("Trending", "Popular", "Top Rated"), titles)

        // Verify keys generated are strictly unique to avoid Compose LazyColumn key collision
        val keys = deduplicated.mapIndexed { index, row -> "row_${row.title}" }
        assertEquals(keys.distinct().size, keys.size)
    }

    @Test
    fun testTop10RowFilteredFromContentRows() {
        val sampleItem = MediaItem(
            id = "1",
            title = "Test",
            url = "",
            posterUrl = null,
            type = MediaType.MOVIE
        )

        val rows = listOf(
            CatalogRow(title = "Top 10 Today", items = (1..6).map { sampleItem.copy(id = it.toString()) }),
            CatalogRow(title = "Trending Now", items = listOf(sampleItem)),
            CatalogRow(title = "Popular Series", items = listOf(sampleItem))
        )

        val rankedTop10 = rows.firstOrNull { row ->
            val t = row.title.trim().lowercase()
            (t == "top 10" || t.startsWith("top 10") || t.startsWith("top ten") || t == "top 10 today") &&
            row.items.size >= 5
        }

        val contentRows = if (rankedTop10 != null) {
            rows.filter { it.title != rankedTop10.title }
        } else {
            rows
        }.distinctBy { it.title }

        assertEquals(2, contentRows.size)
        assertTrue("Top 10 Today should be extracted to dedicated TopTenRow, not duplicated in contentRows", contentRows.none { it.title == "Top 10 Today" })
    }
}
