package com.euthopiar.ui.home

import com.euthopiar.core.model.CatalogRow
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogRowDeduplicationTest {

    private fun deduplicateCatalogRows(rows: List<CatalogRow>): List<CatalogRow> {
        val seen = mutableSetOf<String>()
        return rows.filter { row ->
            val key = "${row.title.trim().lowercase()}|${row.subtitle?.trim()?.lowercase().orEmpty()}"
            seen.add(key)
        }
    }

    @Test
    fun testDeduplicateCatalogRowsRemovesDuplicateTitlesAndSubtitles() {
        val row1 = CatalogRow("Popular Movies", emptyList(), subtitle = "Top Rated")
        val row2 = CatalogRow("popular movies ", emptyList(), subtitle = " top rated ")
        val row3 = CatalogRow("Popular Movies", emptyList(), subtitle = "Trending Today")
        val row4 = CatalogRow("Trending Series", emptyList(), subtitle = null)
        val row5 = CatalogRow("trending series", emptyList(), subtitle = "")

        val deduped = deduplicateCatalogRows(listOf(row1, row2, row3, row4, row5))
        assertEquals(3, deduped.size)
        assertEquals(row1, deduped[0])
        assertEquals(row3, deduped[1])
        assertEquals(row4, deduped[2])
    }
}
