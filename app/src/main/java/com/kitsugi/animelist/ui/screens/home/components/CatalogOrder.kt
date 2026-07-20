package com.kitsugi.animelist.ui.screens.home.components

/**
 * FP-27 – Sorting/Ordering criteria options for catalogs.
 */
enum class CatalogOrder(val label: String) {
    TRENDING("Trendler"),
    POPULARITY("Popülerlik"),
    RELEASE_DATE("Yayın Tarihi"),
    ALPHABETICAL("A-Z")
}
