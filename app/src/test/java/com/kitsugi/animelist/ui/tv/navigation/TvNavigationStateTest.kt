package com.kitsugi.animelist.ui.tv.navigation

import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.tv.stream.TvStreamArgs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvNavigationStateTest {

    @Test
    fun testDefaultState() {
        val state = TvNavigationState()
        assertEquals(TvDestination.HOME, state.currentTab)
        assertTrue(state.detailBackStack.isEmpty())
        assertNull(state.streamTarget)
    }

    @Test
    fun testTabNavigation() {
        val state = TvNavigationState()
        state.currentTab = TvDestination.SEARCH
        assertEquals(TvDestination.SEARCH, state.currentTab)
    }

    @Test
    fun testDetailNavigation() {
        val state = TvNavigationState()
        val result1 = JikanSearchResult(
            malId = 1,
            title = "Anime 1",
            subtitle = "Sub 1",
            type = MediaType.Anime,
            total = 12,
            score = 8,
            isAdult = false,
            imageUrl = null,
            year = 2024,
            source = "mal"
        )
        val result2 = JikanSearchResult(
            malId = 2,
            title = "Anime 2",
            subtitle = "Sub 2",
            type = MediaType.Movie,
            total = 1,
            score = 9,
            isAdult = false,
            imageUrl = null,
            year = 2025,
            source = "mal"
        )

        state.navigateToDetail(result1)
        assertEquals(1, state.detailBackStack.size)
        assertEquals(TvDetailTarget.Media(result1), state.detailBackStack.lastOrNull())

        state.navigateToDetail(result2)
        assertEquals(2, state.detailBackStack.size)
        assertEquals(TvDetailTarget.Media(result2), state.detailBackStack.lastOrNull())

        // Pop last detail
        val popped = state.pop()
        assertTrue(popped)
        assertEquals(1, state.detailBackStack.size)
        assertEquals(TvDetailTarget.Media(result1), state.detailBackStack.lastOrNull())

        // Pop final detail
        val poppedAgain = state.pop()
        assertTrue(poppedAgain)
        assertTrue(state.detailBackStack.isEmpty())

        // Pop empty
        val poppedEmpty = state.pop()
        assertFalse(poppedEmpty)
    }

    @Test
    fun testStreamNavigation() {
        val state = TvNavigationState()
        val streamArgs = TvStreamArgs(
            malId = 1,
            aniListId = null,
            tmdbId = null,
            episode = 1,
            season = 1,
            isMovie = false,
            title = "Anime Stream",
            posterUrl = null,
            titleEnglish = "Anime Stream",
            titleRomaji = "Anime Stream",
            titleNative = "Anime Stream",
            startYear = 2024,
            description = "Desc"
        )

        state.navigateToStream(streamArgs)
        assertEquals(streamArgs, state.streamTarget)

        // Pop should close stream target first
        val popped = state.pop()
        assertTrue(popped)
        assertNull(state.streamTarget)
    }

    @Test
    fun testClearDetails() {
        val state = TvNavigationState()
        val result = JikanSearchResult(
            malId = 1,
            title = "Anime 1",
            subtitle = "Sub 1",
            type = MediaType.Anime,
            total = 12,
            score = 8,
            isAdult = false,
            imageUrl = null,
            year = 2024,
            source = "mal"
        )
        val streamArgs = TvStreamArgs(
            malId = 1,
            aniListId = null,
            tmdbId = null,
            episode = 1,
            season = 1,
            isMovie = false,
            title = "Anime Stream",
            posterUrl = null,
            titleEnglish = "Anime Stream",
            titleRomaji = "Anime Stream",
            titleNative = "Anime Stream",
            startYear = 2024,
            description = "Desc"
        )

        state.navigateToDetail(result)
        state.navigateToStream(streamArgs)

        state.clearDetails()
        assertTrue(state.detailBackStack.isEmpty())
        assertNull(state.streamTarget)
    }
}
