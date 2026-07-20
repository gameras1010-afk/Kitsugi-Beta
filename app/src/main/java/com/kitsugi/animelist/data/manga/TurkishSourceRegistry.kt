package com.kitsugi.animelist.data.manga

import com.kitsugi.animelist.data.manga.model.SourceQueryMode
import com.kitsugi.animelist.data.manga.model.SourceSearchPolicy

object TurkishSourceRegistry {

    private val trustedTurkishTokens = mapOf(
        "manga-tr" to 60,
        "mangatr" to 60,
        "sadscans" to 55,
        "webtoonhatti" to 50,
        "tempestfansub" to 45,
        "trmanga" to 40,
        "manga denizi" to 35,
        "mangadenizi" to 35,
        "turkce manga" to 30,
        "türkçe manga" to 30,
    )

    private val trustedGlobalFallbackTokens = mapOf(
        "mangadex" to 22,
        "comick" to 20,
        "comick live" to 20,
        "weebcentral" to 14,
        "weeb central" to 14,
    )

    private val adultTokens = listOf(
        "hentai", "porn", "doujin", "adult", "18+", "xxx"
    )

    fun policyFor(source: MangaSource): SourceSearchPolicy {
        val signature = source.searchSignature()
        val isTurkish = source.lang.equals("tr", ignoreCase = true)
        val isAdult = adultTokens.any { it in signature }

        var priority = when {
            isTurkish -> 100
            source.lang.equals("en", ignoreCase = true) -> 20
            else -> 10
        }

        var isTrusted = false
        var reason: String? = null

        trustedTurkishTokens.forEach { (token, bonus) ->
            if (token in signature) {
                priority += bonus
                isTrusted = true
                reason = "trusted_tr:$token"
            }
        }
        trustedGlobalFallbackTokens.forEach { (token, bonus) ->
            if (token in signature) {
                priority += bonus
                isTrusted = true
                reason = reason ?: "trusted_global:$token"
            }
        }

        if (source.lang.equals("all", ignoreCase = true) && isTrusted) {
            priority += 8
        }

        if (isAdult) {
            priority -= 300
        }

        val isFallbackOnly = !isTurkish
        val allowGlobalSearch = !isAdult
        val queryMode = when {
            isTurkish -> SourceQueryMode.TurkishTitleFirst
            isFallbackOnly -> SourceQueryMode.FallbackOnly
            else -> SourceQueryMode.Default
        }

        return SourceSearchPolicy(
            sourceKey = source.stableSourceKey(),
            priority = priority,
            isTurkishPreferred = isTurkish,
            isTrusted = isTrusted || isTurkish,
            isFallbackOnly = isFallbackOnly,
            allowGlobalSearch = allowGlobalSearch,
            queryMode = queryMode,
            reason = reason,
        )
    }
}
