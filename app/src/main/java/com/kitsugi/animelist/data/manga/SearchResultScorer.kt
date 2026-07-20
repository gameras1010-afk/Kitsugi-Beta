package com.kitsugi.animelist.data.manga

import com.kitsugi.animelist.data.manga.model.CanonicalTitleSet
import com.kitsugi.animelist.data.manga.model.ScoredSearchResult
import kotlin.math.max

object SearchResultScorer {

    fun score(source: MangaSource, query: CanonicalTitleSet, manga: MangaDetails): Double {
        val policy = TurkishSourceRegistry.policyFor(source)
        val candidate = CanonicalMangaResolver.resolve(manga.title)

        val tokenOverlap = tokenOverlap(query.tokens, candidate.tokens)
        val hasTokenOverlap = tokenOverlap > 0.0
        val hasCompactMatch = query.compact.isNotBlank() && query.compact == candidate.compact
        val hasAliasOverlap = candidate.aliases.any { it in query.aliases } || query.aliases.any { it in candidate.aliases }

        val similarity = max(
            MangaTitleMatcher.getSimilarityScore(query.raw, manga.title),
            max(
                MangaTitleMatcher.getSimilarityScore(query.cleaned, candidate.cleaned),
                MangaTitleMatcher.getSimilarityScore(query.ascii, candidate.ascii),
            ),
        )

        // Reject candidates with no word overlap, compact match, alias overlap, or high similarity
        val isMatchEligible = hasTokenOverlap || hasCompactMatch || hasAliasOverlap || similarity >= 0.65
        if (!isMatchEligible) {
            return 0.0
        }

        var score = 0.0

        if (query.cleaned == candidate.cleaned) score += 1.00
        if (query.ascii == candidate.ascii) score += 0.95
        if (query.compact.isNotBlank() && query.compact == candidate.compact) score += 0.90
        if (query.core.isNotBlank() && query.core == candidate.core) score += 0.85

        if (candidate.aliases.any { it in query.aliases } || query.aliases.any { it in candidate.aliases }) {
            score += 0.30
        }

        score += similarity.coerceAtMost(1.35)
        score += tokenOverlap * 0.35

        if (query.core.isNotBlank() && candidate.ascii.contains(query.core)) {
            score += 0.15
        }
        if (query.tokens.isNotEmpty() && candidate.tokens.containsAll(query.tokens)) {
            score += 0.10
        }

        score += (policy.priority.coerceIn(-100, 200) / 220.0)
        if (policy.isTurkishPreferred) score += 0.18
        if (policy.isTrusted) score += 0.10
        if (!policy.allowGlobalSearch) score -= 1.50

        return score
    }

    fun minAcceptedScore(source: MangaSource, query: CanonicalTitleSet): Double {
        val policy = TurkishSourceRegistry.policyFor(source)
        val base = when {
            query.compact.length <= 3 -> 0.20
            query.compact.length <= 6 -> 0.45
            else -> 0.60
        }
        val fallbackPenalty = if (policy.isFallbackOnly) 0.18 else 0.0
        val trustDiscount = if (policy.isTrusted) 0.08 else 0.0
        return (base + fallbackPenalty - trustDiscount).coerceAtLeast(0.15)
    }

    fun asScored(source: MangaSource, query: CanonicalTitleSet, manga: MangaDetails): ScoredSearchResult {
        return ScoredSearchResult(
            source = source,
            manga = manga,
            score = score(source, query, manga),
        )
    }

    private fun tokenOverlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = (a + b).size.toDouble().coerceAtLeast(1.0)
        return intersection / union
    }
}
