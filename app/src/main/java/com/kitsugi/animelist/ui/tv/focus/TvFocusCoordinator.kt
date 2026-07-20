package com.kitsugi.animelist.ui.tv.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.kitsugi.animelist.ui.tv.navigation.TvDestination

internal fun <T> syncRouteMap(
    existing: MutableMap<TvDestination, T>,
    routes: List<TvDestination>,
    create: () -> T
) {
    val validRoutes = routes.toSet()
    existing.keys.retainAll(validRoutes)
    routes.forEach { route ->
        existing.getOrPut(route, create)
    }
}

@Composable
internal fun rememberDrawerItemFocusRequesters(
    drawerItems: List<TvDestination>
): Map<TvDestination, FocusRequester> {
    val focusRequesters = remember { linkedMapOf<TvDestination, FocusRequester>() }
    syncRouteMap(
        existing = focusRequesters,
        routes = drawerItems,
        create = ::FocusRequester
    )
    return focusRequesters
}
