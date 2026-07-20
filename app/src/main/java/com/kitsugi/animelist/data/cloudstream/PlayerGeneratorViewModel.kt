package com.kitsugi.animelist.data.cloudstream

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.core.player.LinkGenerator
import com.kitsugi.animelist.data.repository.StreamSource
import com.lagradost.cloudstream3.MainAPI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * FP-29 – PlayerGeneratorViewModel
 * Stream linklerini önbelleğe alır, sonraki bölümleri preload eder
 * ve eklenti bazlı oynatma listesini yönetir.
 */
class PlayerGeneratorViewModel(application: Application) : AndroidViewModel(application), LinkGenerator {
    private val context = application.applicationContext

    var currentEpisode = 1
    var maxEpisodes = 12
    var malId: Int? = null
    var aniListId: Int? = null
    var animeTitle: String = ""
    var seasonNum: Int = 1
    var alternativeTitles: List<String> = emptyList()
    var year: Int? = null

    // Sıralanmış linklerin önbelleği
    private val linksCache = mutableMapOf<Int, List<StreamSource>>()
    private val preloadedNextEpisodeLinks = mutableMapOf<Int, Deferred<List<StreamSource>>>()

    override val hasNext: Boolean
        get() = currentEpisode < maxEpisodes

    override val hasPrev: Boolean
        get() = currentEpisode > 1

    fun initialize(
        episode: Int,
        maxEp: Int,
        mId: Int?,
        aId: Int?,
        title: String,
        season: Int,
        alts: List<String>,
        prodYear: Int?
    ) {
        currentEpisode = episode
        maxEpisodes = maxEp
        malId = mId
        aniListId = aId
        animeTitle = title
        seasonNum = season
        alternativeTitles = alts
        year = prodYear

        linksCache.clear()
        preloadedNextEpisodeLinks.clear()
    }

    /**
     * Belirli bir bölüm için stream kaynaklarını alır ve sıralar.
     */
    suspend fun getStreamsForEpisode(episode: Int, apis: List<MainAPI>): List<StreamSource> {
        val cached = linksCache[episode]
        if (cached != null) return cached

        val results = mutableListOf<StreamSource>()
        for (api in apis) {
            try {
                val streams = CsStreamRunner.getStreams(
                    api = api,
                    title = animeTitle,
                    alternativeTitles = alternativeTitles,
                    year = year,
                    season = seasonNum,
                    episode = episode,
                    malId = malId,
                    aniListId = aniListId
                )
                results.addAll(streams)
            } catch (e: Exception) {
                Log.e("PlayerGeneratorVM", "Failed to get streams from ${api.name}", e)
            }
        }

        // Kaliteye göre öncelikli sıralama
        val sorted = results.sortedWith(
            compareByDescending<StreamSource> { it.qualityValue ?: 0 }
                .thenBy { it.name }
        )
        linksCache[episode] = sorted
        return sorted
    }

    /**
     * Sonraki bölümün linklerini arka planda önbelleğe yükler (Preload).
     */
    fun preloadNextEpisode(apis: List<MainAPI>) {
        if (!hasNext) return
        val nextEp = currentEpisode + 1
        if (preloadedNextEpisodeLinks.containsKey(nextEp)) return

        Log.d("PlayerGeneratorVM", "Preloading next episode $nextEp streams...")
        val deferred = viewModelScope.async {
            getStreamsForEpisode(nextEp, apis)
        }
        preloadedNextEpisodeLinks[nextEp] = deferred
    }

    override suspend fun getNextEpisodeLink(): StreamSource? {
        if (!hasNext) return null
        val nextEp = currentEpisode + 1

        val deferred = preloadedNextEpisodeLinks[nextEp]
        val streams = if (deferred != null) {
            deferred.await()
        } else {
            emptyList()
        }

        currentEpisode = nextEp
        return streams.firstOrNull()
    }

    override suspend fun getPrevEpisodeLink(): StreamSource? {
        if (!hasPrev) return null
        val prevEp = currentEpisode - 1
        currentEpisode = prevEp
        val cached = linksCache[prevEp]
        return cached?.firstOrNull()
    }
}
