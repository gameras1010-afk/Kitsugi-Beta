package com.kitsugi.animelist.ui.utils

import com.kitsugi.animelist.ui.tv.navigation.TvDestination
import com.kitsugi.animelist.ui.tv.focus.syncRouteMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class DrawerFocusRequesterStateTest {

    @Test
    fun `syncRouteMap preserves existing identities when destination is inserted`() {
        val home = Any()
        val search = Any()
        val library = Any()
        val settings = Any()
        val requesters = linkedMapOf(
            TvDestination.HOME to home,
            TvDestination.SEARCH to search,
            TvDestination.LIBRARY to library,
            TvDestination.SETTINGS to settings
        )

        syncRouteMap(
            existing = requesters,
            routes = listOf(TvDestination.HOME, TvDestination.ACCOUNT, TvDestination.SEARCH, TvDestination.LIBRARY, TvDestination.SETTINGS),
            create = { Any() }
        )

        assertSame(home, requesters.getValue(TvDestination.HOME))
        assertSame(search, requesters.getValue(TvDestination.SEARCH))
        assertSame(library, requesters.getValue(TvDestination.LIBRARY))
        assertSame(settings, requesters.getValue(TvDestination.SETTINGS))
        assertNotNull(requesters[TvDestination.ACCOUNT])
    }

    @Test
    fun `syncRouteMap drops stale routes when a route is removed`() {
        val home = Any()
        val account = Any()
        val search = Any()
        val library = Any()
        val settings = Any()
        val requesters = linkedMapOf(
            TvDestination.HOME to home,
            TvDestination.ACCOUNT to account,
            TvDestination.SEARCH to search,
            TvDestination.LIBRARY to library,
            TvDestination.SETTINGS to settings
        )

        syncRouteMap(
            existing = requesters,
            routes = listOf(TvDestination.HOME, TvDestination.SEARCH, TvDestination.LIBRARY, TvDestination.SETTINGS),
            create = { Any() }
        )

        assertFalse(requesters.containsKey(TvDestination.ACCOUNT))
        assertSame(home, requesters.getValue(TvDestination.HOME))
        assertSame(search, requesters.getValue(TvDestination.SEARCH))
        assertSame(library, requesters.getValue(TvDestination.LIBRARY))
        assertSame(settings, requesters.getValue(TvDestination.SETTINGS))
    }

    @Test
    fun `syncRouteMap is a no-op when routes match existing entries`() {
        val home = Any()
        val account = Any()
        val search = Any()
        val library = Any()
        val settings = Any()
        val requesters = linkedMapOf(
            TvDestination.HOME to home,
            TvDestination.ACCOUNT to account,
            TvDestination.SEARCH to search,
            TvDestination.LIBRARY to library,
            TvDestination.SETTINGS to settings
        )

        syncRouteMap(
            existing = requesters,
            routes = listOf(TvDestination.HOME, TvDestination.ACCOUNT, TvDestination.SEARCH, TvDestination.LIBRARY, TvDestination.SETTINGS),
            create = { Any() }
        )

        assertEquals(5, requesters.size)
        assertSame(home, requesters.getValue(TvDestination.HOME))
        assertSame(account, requesters.getValue(TvDestination.ACCOUNT))
        assertSame(search, requesters.getValue(TvDestination.SEARCH))
        assertSame(library, requesters.getValue(TvDestination.LIBRARY))
        assertSame(settings, requesters.getValue(TvDestination.SETTINGS))
    }
}
