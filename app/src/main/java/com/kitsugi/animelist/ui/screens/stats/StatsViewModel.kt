package com.kitsugi.animelist.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.MediaEntryDao
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatusStat(
    val status: WatchStatus,
    val count: Int,
    val percentage: Float
)

data class ScoreStat(
    val score: Int,
    val count: Int,
    val percentage: Float
)

data class StatsUiState(
    val isLoading: Boolean = true,
    // Anime
    val totalAnime: Int = 0,
    val totalEpisodesWatched: Int = 0,
    val daysWatched: Float = 0f,
    val meanScore: Float = 0f,
    val animeStatusStats: List<StatusStat> = emptyList(),
    val animeScoreStats: List<ScoreStat> = emptyList(),
    // Manga
    val totalManga: Int = 0,
    val totalChaptersRead: Int = 0,
    val mangaStatusStats: List<StatusStat> = emptyList(),
    val mangaScoreStats: List<ScoreStat> = emptyList(),
    val totalFavorites: Int = 0
)

class StatsViewModel(
    private val dao: MediaEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            dao.observeAll().collect { allEntities ->
                processEntities(allEntities)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val allEntities = dao.getAll()
                processEntities(allEntities)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun processEntities(allEntities: List<com.kitsugi.animelist.data.local.MediaEntryEntity>) {
        try {
            val all = allEntities.map { it.toDomain() }

            val anime = all.filter { it.type == MediaType.Anime || it.type == MediaType.Movie }
            val manga = all.filter { it.type == MediaType.Manga }

            // Anime stats
            val totalEpisodes = anime.sumOf { it.progress }
            val daysWatched = (totalEpisodes * 24f) / 60f / 60f // Assume 24 min/ep

            val animeWithScore = anime.filter { (it.score ?: 0) > 0 }
            val meanScore = if (animeWithScore.isNotEmpty())
                animeWithScore.mapNotNull { it.score }.average().toFloat()
            else 0f

            val animeStatusStats = buildStatusStats(anime)
            val animeScoreStats = buildScoreStats(animeWithScore)

            // Manga stats
            val totalChapters = manga.sumOf { it.progress }
            val mangaWithScore = manga.filter { (it.score ?: 0) > 0 }
            val mangaStatusStats = buildStatusStats(manga)
            val mangaScoreStats = buildScoreStats(mangaWithScore)

            _uiState.value = StatsUiState(
                isLoading = false,
                totalAnime = anime.size,
                totalEpisodesWatched = totalEpisodes,
                daysWatched = daysWatched,
                meanScore = meanScore,
                animeStatusStats = animeStatusStats,
                animeScoreStats = animeScoreStats,
                totalManga = manga.size,
                totalChaptersRead = totalChapters,
                mangaStatusStats = mangaStatusStats,
                mangaScoreStats = mangaScoreStats,
                totalFavorites = all.count { it.isFavorite }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun buildStatusStats(entries: List<com.kitsugi.animelist.model.MediaEntry>): List<StatusStat> {
        val total = entries.size.coerceAtLeast(1)
        return WatchStatus.entries.mapNotNull { status ->
            val count = entries.count { it.status == status }
            if (count == 0) return@mapNotNull null
            StatusStat(
                status = status,
                count = count,
                percentage = count.toFloat() / total
            )
        }
    }

    private fun buildScoreStats(entries: List<com.kitsugi.animelist.model.MediaEntry>): List<ScoreStat> {
        val total = entries.size.coerceAtLeast(1)
        return (1..10).mapNotNull { score ->
            val count = entries.count { it.score == score }
            if (count == 0) return@mapNotNull null
            ScoreStat(
                score = score,
                count = count,
                percentage = count.toFloat() / total
            )
        }
    }
}
