package com.nuvio.app.features.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchHistoryRepositoryTest {
    @Test
    fun `applySearchHistoryEntry moves existing query to front`() {
        val updated = applySearchHistoryEntry(
            current = listOf("batman", "dune", "silo"),
            query = "dune",
            limit = 10,
        )

        assertEquals(listOf("dune", "batman", "silo"), updated)
    }

    @Test
    fun `applySearchHistoryEntry respects max size`() {
        val updated = applySearchHistoryEntry(
            current = listOf("a", "b", "c"),
            query = "d",
            limit = 3,
        )

        assertEquals(listOf("d", "a", "b"), updated)
    }
}
