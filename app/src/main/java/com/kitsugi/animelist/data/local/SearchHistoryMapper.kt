package com.kitsugi.animelist.data.local

import com.kitsugi.animelist.ui.screens.search.SearchHistoryItem
import com.kitsugi.animelist.ui.screens.search.SearchPlatform
import com.kitsugi.animelist.model.MediaType

object SearchHistoryMapper {
    fun toDomain(entity: SearchHistoryEntity): SearchHistoryItem {
        val parts = entity.type.split(":")
        val platform = try {
            SearchPlatform.valueOf(parts.getOrNull(0) ?: "All")
        } catch (e: Exception) {
            SearchPlatform.All
        }
        val mediaType = try {
            MediaType.valueOf(parts.getOrNull(1) ?: "Anime")
        } catch (e: Exception) {
            MediaType.Anime
        }
        return SearchHistoryItem(
            query = entity.query,
            platform = platform,
            mediaType = mediaType
        )
    }

    fun toEntity(item: SearchHistoryItem): SearchHistoryEntity {
        return SearchHistoryEntity(
            query = item.query,
            timestamp = System.currentTimeMillis(),
            type = "${item.platform.name}:${item.mediaType.name}"
        )
    }
}
