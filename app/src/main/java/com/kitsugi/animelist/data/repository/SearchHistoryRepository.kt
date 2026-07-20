package com.kitsugi.animelist.data.repository

import com.kitsugi.animelist.data.local.SearchHistoryDao
import com.kitsugi.animelist.data.local.SearchHistoryMapper
import com.kitsugi.animelist.ui.screens.search.SearchHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepository(private val searchHistoryDao: SearchHistoryDao) {

    fun getRecentSearchHistory(): Flow<List<SearchHistoryItem>> {
        return searchHistoryDao.getRecentSearchHistory().map { list ->
            list.map { SearchHistoryMapper.toDomain(it) }
        }
    }

    suspend fun insertSearchQuery(item: SearchHistoryItem) {
        if (item.query.isBlank()) return
        val entity = SearchHistoryMapper.toEntity(item)
        searchHistoryDao.insertSearchQuery(entity)
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }
}
