package com.kitsugi.animelist.ui.screens.home.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * FP-23 – Data pipeline manager for loading content catalog pages.
 */
class CatalogPipeline {
    private val _catalogItems = MutableStateFlow<List<String>>(emptyList())
    val catalogItems: StateFlow<List<String>> = _catalogItems

    fun loadPage(page: Int) {
        val newItems = listOf("Anime $page-A", "Anime $page-B", "Manga $page-A")
        _catalogItems.value = _catalogItems.value + newItems
    }

    fun clearCatalog() {
        _catalogItems.value = emptyList()
    }
}
