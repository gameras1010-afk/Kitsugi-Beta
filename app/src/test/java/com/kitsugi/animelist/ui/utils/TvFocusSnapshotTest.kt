package com.kitsugi.animelist.ui.utils

import com.kitsugi.animelist.ui.tv.focus.TvFocusSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvFocusSnapshotTest {

    @Test
    fun `TvFocusSnapshot default values are correct`() {
        val snapshot = TvFocusSnapshot()
        assertNull(snapshot.focusedItemKey)
        assertEquals(0, snapshot.scrollIndex)
        assertEquals(0, snapshot.scrollOffset)
        assert(snapshot.metadata.isEmpty())
    }

    @Test
    fun `TvFocusSnapshot custom values are correctly stored`() {
        val snapshot = TvFocusSnapshot(
            focusedItemKey = "item_123",
            scrollIndex = 5,
            scrollOffset = 100,
            metadata = mapOf("rowKey" to "row_abc")
        )
        assertEquals("item_123", snapshot.focusedItemKey)
        assertEquals(5, snapshot.scrollIndex)
        assertEquals(100, snapshot.scrollOffset)
        assertEquals("row_abc", snapshot.metadata["rowKey"])
    }
}
