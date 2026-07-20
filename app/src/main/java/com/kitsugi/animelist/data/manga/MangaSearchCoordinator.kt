package com.kitsugi.animelist.data.manga

import com.kitsugi.animelist.data.manga.model.ScoredSearchResult
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus

class MangaSearchCoordinator(
    private val stateStore: MangaSourceStateStore,
) {

    fun getAvailableSources(sources: List<MangaSource>): List<MangaSource> {
        return sources
            .filter { stateStore.getHealthStatus(it) != SourceHealthStatus.Disabled }
            .sortedWith(
                compareByDescending<MangaSource> { getSourcePriority(it) }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun getSearchCandidateSources(
        sources: List<MangaSource>,
        includeTrustedFallbacks: Boolean = true,
    ): List<MangaSource> {
        val available = getAvailableSources(sources)
            .filter { stateStore.isSearchEligible(it) }
            .filter { TurkishSourceRegistry.policyFor(it).allowGlobalSearch }

        val turkish = available.filter { TurkishSourceRegistry.policyFor(it).isTurkishPreferred }
        if (turkish.isNotEmpty()) {
            val extras = if (includeTrustedFallbacks) {
                available
                    .filter {
                        val policy = TurkishSourceRegistry.policyFor(it)
                        !policy.isTurkishPreferred && policy.isTrusted && !policy.isFallbackOnly
                    }
                    .take(2)
            } else {
                emptyList()
            }
            return (turkish + extras).distinctBy { it.stableSourceKey() }
        }
        return available
    }

    fun postProcess(source: MangaSource, query: String, mangas: List<MangaDetails>, relaxScoring: Boolean = false): List<MangaDetails> {
        val canonicalQuery = CanonicalMangaResolver.resolve(query)
        val minScore = if (relaxScoring) 0.05 else SearchResultScorer.minAcceptedScore(source, canonicalQuery)
        return mangas
            .map { SearchResultScorer.asScored(source, canonicalQuery, it) }
            .filter { it.score >= minScore }
            .sortedWith(
                compareByDescending<ScoredSearchResult> { it.score }
                    .thenBy { it.manga.title.lowercase() }
            )
            .distinctBy { scored ->
                val titleKey = CanonicalMangaResolver.resolve(scored.manga.title).compact.ifBlank { scored.manga.title.lowercase() }
                "${source.stableSourceKey()}|${scored.manga.url}|$titleKey"
            }
            .map { it.manga.copy(source = source.name) }
    }

    fun getSourcePriority(source: MangaSource): Int {
        val policy = TurkishSourceRegistry.policyFor(source)
        val healthDelta = when (stateStore.getHealthStatus(source)) {
            SourceHealthStatus.Healthy -> 16
            SourceHealthStatus.Degraded -> -15
            SourceHealthStatus.Broken -> -120
            SourceHealthStatus.CaptchaRequired -> -35
            SourceHealthStatus.RateLimited -> -25
            SourceHealthStatus.Disabled -> -1_000
            SourceHealthStatus.Unknown -> 0
        }
        val cooldownPenalty = if (stateStore.isCoolingDown(source)) -180 else 0
        val streakPenalty = (stateStore.getFailureStreak(source).coerceAtMost(5)) * -12
        return policy.priority + healthDelta + cooldownPenalty + streakPenalty
    }
}
