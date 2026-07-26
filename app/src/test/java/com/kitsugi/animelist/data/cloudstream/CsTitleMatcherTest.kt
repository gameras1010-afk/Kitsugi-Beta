package com.kitsugi.animelist.data.cloudstream

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.SearchResponse
import org.junit.Assert.*
import org.junit.Test

class CsTitleMatcherTest {

    @Test
    fun testBuildTitleVariantsWithSeason() {
        val main = "Jujutsu Kaisen"
        val alts = listOf("Sorcery Fight")

        // Season 2 variants should have season-specific queries prepended
        val variants = CsTitleMatcher.buildTitleVariants(main, alts, season = 2)

        assertTrue(variants.contains("Jujutsu Kaisen 2. Sezon"))
        assertTrue(variants.contains("Jujutsu Kaisen Season 2"))
        assertTrue(variants.contains("Jujutsu Kaisen S2"))
        assertTrue(variants.contains("Sorcery Fight 2. Sezon"))
        assertTrue(variants.contains("Sorcery Fight Season 2"))
    }

    @Test
    fun testFindBestMatchWithSeason() {
        val results = listOf(
            createSearchResponse("Jujutsu Kaisen", 2020),
            createSearchResponse("Jujutsu Kaisen 2. Sezon", 2023),
            createSearchResponse("Jujutsu Kaisen 3. Sezon", 2025)
        )

        // Searching for Season 2 should match "Jujutsu Kaisen 2. Sezon"
        val matchS2 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Jujutsu Kaisen",
            altTitles = emptyList(),
            targetYear = 2023,
            targetSeason = 2
        )
        assertNotNull(matchS2)
        assertEquals("Jujutsu Kaisen 2. Sezon", matchS2?.name)

        // Searching for Season 1 (implicitly or explicitly targetSeason = 1) should match "Jujutsu Kaisen"
        val matchS1 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Jujutsu Kaisen",
            altTitles = emptyList(),
            targetYear = 2020,
            targetSeason = 1
        )
        assertNotNull(matchS1)
        assertEquals("Jujutsu Kaisen", matchS1?.name)
    }

    @Test
    fun testFindBestMatchSequelStandalone() {
        val results = listOf(
            createSearchResponse("Tsue to Tsurugi no Wistoria", 2024),
            createSearchResponse("Tsue to Tsurugi no Wistoria Season 2", 2026)
        )

        // Searching for Season 1 of "Tsue to Tsurugi no Wistoria Season 2" (sequel entry)
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Tsue to Tsurugi no Wistoria Season 2",
            altTitles = emptyList(),
            targetYear = 2026,
            targetSeason = 1
        )
        assertNotNull(match)
        assertEquals("Tsue to Tsurugi no Wistoria Season 2", match?.name)
    }

    private fun createSearchResponse(name: String, year: Int?): SearchResponse {
        val cls = Class.forName("com.lagradost.cloudstream3.AnimeSearchResponse")
        val ctor = cls.constructors.firstOrNull { it.parameterCount >= 3 }
            ?: throw IllegalStateException("Could not find AnimeSearchResponse constructor")
        val args = Array<Any?>(ctor.parameterCount) { null }
        args[0] = name
        args[1] = "http://example.com"
        args[2] = "DummyProvider"
        if (ctor.parameterCount > 3) args[3] = TvType.Anime
        
        // Fill non-null Kotlin collection parameters to avoid NullPointerException
        val paramTypes = ctor.parameterTypes
        for (i in 4 until ctor.parameterCount) {
            val pType = paramTypes[i]
            if (pType == java.util.Set::class.java) {
                args[i] = emptySet<Any>()
            } else if (pType == java.util.Map::class.java) {
                args[i] = emptyMap<Any, Any>()
            }
        }
        
        val instance = ctor.newInstance(*args) as SearchResponse
        
        // Try to set year field via reflection if it exists
        if (year != null) {
            try {
                val field = cls.getDeclaredField("year")
                field.isAccessible = true
                field.set(instance, year)
            } catch (_: Exception) {}
        }
        return instance
    }
}
