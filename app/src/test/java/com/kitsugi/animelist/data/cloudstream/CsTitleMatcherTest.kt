package com.kitsugi.animelist.data.cloudstream

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.SearchResponse
import org.junit.Assert.*
import org.junit.Test

/**
 * CsTitleMatcher için kapsamlı JVM unit testleri.
 *
 * Kapsam:
 *  - Anime (sezon 1, sezon 2+, Roman rakam sezonlar)
 *  - Dizi (DiziBox, Dizilla, DiziPal, RecTV naming patterns)
 *  - Film (yıl baskın eşleştirme)
 *  - Slug tabanlı sezon çıkarma (tüm bilinen Türk site URL formatları)
 *  - Alternatif başlık (romaji, Türkçe, İngilizce)
 *  - Kısmi eşleştirme ve fallback senaryoları
 *  - getBestTitleSimilarity fonksiyonu
 */
class CsTitleMatcherTest {

    // ─── Season-Specific Anime Variant Tests ─────────────────────────────────

    @Test
    fun testBuildTitleVariantsWithSeason() {
        val main = "Jujutsu Kaisen"
        val alts = listOf("Sorcery Fight")

        val variants = CsTitleMatcher.buildTitleVariants(main, alts, season = 2)

        assertTrue("Should contain '2. Sezon' variant", variants.contains("Jujutsu Kaisen 2. Sezon"))
        assertTrue("Should contain 'Season 2' variant", variants.contains("Jujutsu Kaisen Season 2"))
        assertTrue("Should contain 'S2' variant", variants.contains("Jujutsu Kaisen S2"))
        assertTrue("Should contain alt '2. Sezon' variant", variants.contains("Sorcery Fight 2. Sezon"))
        assertTrue("Should contain alt 'Season 2' variant", variants.contains("Sorcery Fight Season 2"))
        // Base title must still be included
        assertTrue("Should still contain base title", variants.contains("Jujutsu Kaisen"))
    }

    @Test
    fun testBuildTitleVariantsSeason1NoPrependedSeasonSuffixes() {
        val variants = CsTitleMatcher.buildTitleVariants("Demon Slayer", emptyList(), season = 1)
        // For season 1, season-specific queries should NOT be prepended
        assertFalse("Season-1 should not prepend '1. Sezon' queries", variants.contains("Demon Slayer 1. Sezon"))
        // But base title should be there
        assertTrue(variants.contains("Demon Slayer"))
    }

    @Test
    fun testBuildTitleVariantsFilmNoSeason() {
        val main = "Avengers: Endgame"
        val variants = CsTitleMatcher.buildTitleVariants(main, emptyList(), season = 1)
        // Base title must be present
        assertTrue(variants.contains("Avengers: Endgame"))
        // ASCII normalized version
        assertTrue("ASCII normalized version should exist", variants.any { it.contains("Avengers") && it.contains("Endgame") })
    }

    @Test
    fun testBuildTitleVariantsWithTurkishChars() {
        val main = "Şahane Aile"
        val variants = CsTitleMatcher.buildTitleVariants(main, emptyList(), season = 1)
        // ASCII version: Ş→S, should appear
        assertTrue("ASCII version of Turkish title should appear",
            variants.any { it.contains("Sahane") || it.contains("sahane") })
    }

    // ─── FindBestMatch: Anime Season Matching ────────────────────────────────

    @Test
    fun testFindBestMatchWithSeason() {
        val results = listOf(
            createSearchResponse("Jujutsu Kaisen", 2020),
            createSearchResponse("Jujutsu Kaisen 2. Sezon", 2023),
            createSearchResponse("Jujutsu Kaisen 3. Sezon", 2025)
        )

        val matchS2 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Jujutsu Kaisen",
            altTitles = emptyList(),
            targetYear = 2023,
            targetSeason = 2
        )
        assertNotNull("Season 2 match should not be null", matchS2)
        assertEquals("Jujutsu Kaisen 2. Sezon", matchS2?.name)

        val matchS1 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Jujutsu Kaisen",
            altTitles = emptyList(),
            targetYear = 2020,
            targetSeason = 1
        )
        assertNotNull("Season 1 match should not be null", matchS1)
        assertEquals("Jujutsu Kaisen", matchS1?.name)
    }

    @Test
    fun testFindBestMatchSequelStandalone() {
        val results = listOf(
            createSearchResponse("Tsue to Tsurugi no Wistoria", 2024),
            createSearchResponse("Tsue to Tsurugi no Wistoria Season 2", 2026)
        )

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

    @Test
    fun testFindBestMatchImplicitSeasonPenalty() {
        // When we search for season 3, a result with no season marker should be penalised
        val results = listOf(
            createSearchResponse("Attack on Titan", 2013),               // implicit S1
            createSearchResponse("Attack on Titan 3. Sezon", 2018)        // explicit S3
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Attack on Titan",
            altTitles = listOf("Shingeki no Kyojin"),
            targetYear = 2018,
            targetSeason = 3
        )
        assertNotNull(match)
        assertEquals("Attack on Titan 3. Sezon", match?.name)
    }

    @Test
    fun testFindBestMatchRomanNumeralSeason() {
        // Roman numeral seasons are common on some Turkish sites
        val results = listOf(
            createSearchResponse("Vinland Saga", 2019),
            createSearchResponse("Vinland Saga II", 2023)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Vinland Saga Season 2",
            altTitles = listOf("Vinland Saga"),
            targetYear = 2023,
            targetSeason = 2
        )
        assertNotNull(match)
        assertEquals("Vinland Saga II", match?.name)
    }

    @Test
    fun testFindBestMatchRomanNumeralIII() {
        val results = listOf(
            createSearchResponse("Overlord I", 2015),
            createSearchResponse("Overlord II", 2018),
            createSearchResponse("Overlord III", 2018),
            createSearchResponse("Overlord IV", 2022)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Overlord",
            altTitles = emptyList(),
            targetYear = 2018,
            targetSeason = 3
        )
        assertNotNull(match)
        assertEquals("Overlord III", match?.name)
    }

    // ─── FindBestMatch: Film Scenarios ───────────────────────────────────────

    @Test
    fun testFindBestMatchFilmYearBoosted() {
        val results = listOf(
            createSearchResponse("Inception", 2010),
            createSearchResponse("Inception 2 Fan Film", 2020)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Inception",
            altTitles = emptyList(),
            targetYear = 2010,
            targetSeason = 1
        )
        assertNotNull(match)
        assertEquals("Inception", match?.name)
    }

    @Test
    fun testFindBestMatchFilmNoYear() {
        // No year available — should still match on title similarity alone
        val results = listOf(
            createSearchResponse("Interstellar", null),
            createSearchResponse("Interstella 5555", null)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Interstellar",
            altTitles = emptyList(),
            targetYear = null,
            targetSeason = 1
        )
        assertNotNull(match)
        assertEquals("Interstellar", match?.name)
    }

    @Test
    fun testFindBestMatchFilmAltTitle() {
        // Plugin returns the Turkish name, we search by the English title
        val results = listOf(
            createSearchResponse("Uzay Yolculuğu 2001", 1968),
            createSearchResponse("2001: A Space Odyssey", 1968)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "2001: A Space Odyssey",
            altTitles = listOf("Uzay Yolculuğu 2001"),
            targetYear = 1968,
            targetSeason = 1
        )
        assertNotNull(match)
        // Either match is acceptable — just verify we found something
        assertNotNull(match?.name)
    }

    // ─── FindBestMatch: Turkish TV Series Scenarios ──────────────────────────

    @Test
    fun testFindBestMatchDiziBoxPattern() {
        // DiziBox names episodes as "Serie Title 3. Sezon 5. Bölüm" inside episode objects,
        // but the search result itself is just the series title.
        // At the findBestMatch level, we should match the series by title.
        val results = listOf(
            createSearchResponse("Kuzey Yıldızı İlk Aşk", null),
            createSearchResponse("Kuzey Yarım Küre", null)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Kuzey Yıldızı İlk Aşk",
            altTitles = emptyList(),
            targetYear = null,
            targetSeason = 1
        )
        assertNotNull(match)
        assertEquals("Kuzey Yıldızı İlk Aşk", match?.name)
    }

    @Test
    fun testFindBestMatchDizillaPattern() {
        // Dizilla typically lists TV series without season in the title.
        val results = listOf(
            createSearchResponse("Çukur", 2017),
            createSearchResponse("Çukur 2. Sezon", 2018),
            createSearchResponse("Çukurova Masalı", 2021)
        )
        val matchS2 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Çukur",
            altTitles = emptyList(),
            targetYear = 2018,
            targetSeason = 2
        )
        assertNotNull(matchS2)
        assertEquals("Çukur 2. Sezon", matchS2?.name)
    }

    @Test
    fun testFindBestMatchKoreanDramaPattern() {
        // KoreanTurk & AsyaWatch: Korean dramas with Korean title in parentheses
        val results = listOf(
            createSearchResponse("İtme Beni (밀어내기)", 2023),
            createSearchResponse("Kimse Bilmez", 2023)
        )
        val match = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "İtme Beni",
            altTitles = listOf("밀어내기", "Push Me"),
            targetYear = 2023,
            targetSeason = 1
        )
        assertNotNull(match)
        assertEquals("İtme Beni (밀어내기)", match?.name)
    }

    // ─── ParseSeasonFromSlug Tests ────────────────────────────────────────────

    @Test
    fun testParseSeasonFromSlug() {
        // Standard formats
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/naruto-shippuden-2-sezon"))
        assertEquals(3, CsTitleMatcher.parseSeasonFromSlug("https://example.com/sezon-3/naruto"))
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/s2/naruto"))
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/naruto-2/"))
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/naruto"))
    }

    @Test
    fun testParseSeasonFromSlugTurkAnimeSiteFormats() {
        // TurkAnime: "naruto-shippuden-2-sezon-izle"
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://www.turkanime.tv/anime/naruto-shippuden-2-sezon-izle"))
        // AnimeciX: "anime/attack-on-titan-season-3"
        assertEquals(3, CsTitleMatcher.parseSeasonFromSlug("https://anm.cx/anime/attack-on-titan-season-3"))
        // Animeler: "anime/black-clover-3/"
        assertEquals(3, CsTitleMatcher.parseSeasonFromSlug("https://animeler.pw/anime/black-clover-3/"))
        // AnimPow: "anime/jujutsu-kaisen-2-sezon"
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://animpow.com/anime/jujutsu-kaisen-2-sezon"))
    }

    @Test
    fun testParseSeasonFromSlugAsyaWatchFormats() {
        // AsyaWatch: K-drama slug often doesn't have season marker — should return null
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://asyawatch.com/drama/itme-beni"))
        // But if it does have a season
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://asyawatch.com/drama/itme-beni-2"))
    }

    @Test
    fun testParseSeasonFromSlugDiziSiteFormats() {
        // DiziBox: "dizi/dizinin-adi-2-sezon"
        assertEquals(2, CsTitleMatcher.parseSeasonFromSlug("https://www.dizibox.live/dizi/gece-yarisi-2-sezon"))
        // DiziPal: "dizi/cukur/sezon-3"
        assertEquals(3, CsTitleMatcher.parseSeasonFromSlug("https://dizipal950.com/dizi/cukur/sezon-3"))
        // Dizilla: sometimes no season
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://dizilla.nl/dizi/cukur"))
    }

    @Test
    fun testParseSeasonFromSlugEdgeCases() {
        // URL contains year that looks like it could be mistaken for season
        // "2001" should not be parsed as season (only 2-10 for trailing digit)
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://example.com/film/2001-uzay-macerasi"))
        // Trailing digit = 1 should not be parsed (only 2-10 range for trailing)
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/naruto-1"))
        // Trailing digit = 11 should not be parsed (>10 limit)
        assertNull(CsTitleMatcher.parseSeasonFromSlug("https://example.com/anime/naruto-11"))
    }

    // ─── FindBestMatch: URL Slug Season Tests ─────────────────────────────────

    @Test
    fun testFindBestMatchWithSlugSeason() {
        val results = listOf(
            createSearchResponse("Jujutsu Kaisen", "https://example.com/jujutsu-kaisen-1", 2020),
            createSearchResponse("Jujutsu Kaisen", "https://example.com/jujutsu-kaisen-2-sezon", 2023)
        )

        val matchS2 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Jujutsu Kaisen",
            altTitles = emptyList(),
            targetYear = 2023,
            targetSeason = 2
        )
        assertNotNull(matchS2)
        assertEquals("https://example.com/jujutsu-kaisen-2-sezon", matchS2?.url)
    }

    @Test
    fun testFindBestMatchSlugSeasonOverridesImplicit() {
        // Both results have the same name "Sword Art Online"
        // One has slug season-2, other has slug season-1
        val results = listOf(
            createSearchResponse("Sword Art Online", "https://example.com/sao-1", 2012),
            createSearchResponse("Sword Art Online", "https://example.com/sao-season-2", 2014)
        )
        val matchS2 = CsTitleMatcher.findBestMatch(
            results = results,
            mainTitle = "Sword Art Online",
            altTitles = emptyList(),
            targetYear = 2014,
            targetSeason = 2
        )
        assertNotNull(matchS2)
        assertEquals("https://example.com/sao-season-2", matchS2?.url)
    }

    // ─── ParseSeasonFromTitle Tests ───────────────────────────────────────────

    @Test
    fun testParseSeasonFromTitle() {
        assertEquals(2, CsTitleMatcher.parseSeasonFromTitle("Jujutsu Kaisen 2. Sezon"))
        assertEquals(3, CsTitleMatcher.parseSeasonFromTitle("Attack on Titan Season 3"))
        assertEquals(2, CsTitleMatcher.parseSeasonFromTitle("Sword Art Online S2"))
        assertEquals(4, CsTitleMatcher.parseSeasonFromTitle("Overlord 4"))
        assertEquals(2, CsTitleMatcher.parseSeasonFromTitle("Vinland Saga II"))
        assertEquals(3, CsTitleMatcher.parseSeasonFromTitle("Overlord III"))
        assertEquals(4, CsTitleMatcher.parseSeasonFromTitle("Overlord IV"))
        assertEquals(5, CsTitleMatcher.parseSeasonFromTitle("Overlord V"))
        assertNull(CsTitleMatcher.parseSeasonFromTitle("Naruto"))
        assertNull(CsTitleMatcher.parseSeasonFromTitle("Death Note"))
    }

    @Test
    fun testParseSeasonFromTitleTurkishFormats() {
        assertEquals(2, CsTitleMatcher.parseSeasonFromTitle("Boku no Hero Academia 2. Sezon"))
        assertEquals(3, CsTitleMatcher.parseSeasonFromTitle("Kimetsu no Yaiba 3. sezon"))
        assertEquals(4, CsTitleMatcher.parseSeasonFromTitle("One Punch Man 4.Sezon"))
    }

    // ─── GetBestTitleSimilarity Tests ─────────────────────────────────────────

    @Test
    fun testGetBestTitleSimilarityExactMatch() {
        val sim = CsTitleMatcher.getBestTitleSimilarity(
            "Jujutsu Kaisen",
            "Jujutsu Kaisen",
            emptyList()
        )
        assertEquals(1.0, sim, 0.001)
    }

    @Test
    fun testGetBestTitleSimilarityAltMatch() {
        // Candidate matches an alt title
        val sim = CsTitleMatcher.getBestTitleSimilarity(
            "Sorcery Fight",
            "Jujutsu Kaisen",
            listOf("Sorcery Fight", "呪術廻戦")
        )
        assertTrue("Alt title should give very high similarity", sim > 0.90)
    }

    @Test
    fun testGetBestTitleSimilarityTurkishAscii() {
        // Turkish characters should be normalized for comparison
        val sim = CsTitleMatcher.getBestTitleSimilarity(
            "Sahane Aile",   // ASCII version of "Şahane Aile"
            "Şahane Aile",
            emptyList()
        )
        assertTrue("Turkish char normalization should give high similarity", sim > 0.85)
    }

    @Test
    fun testGetBestTitleSimilarityUnrelated() {
        val sim = CsTitleMatcher.getBestTitleSimilarity(
            "Boruto",
            "Attack on Titan",
            emptyList()
        )
        assertTrue("Unrelated titles should have low similarity", sim < 0.30)
    }

    // ─── SimplifyTitle & ToAsciiTitle ────────────────────────────────────────

    @Test
    fun testSimplifyTitle() {
        val simplified = CsTitleMatcher.simplifyTitle("Kimetsu no Yaiba: Mugen Ressha-hen!")
        assertFalse("Simplified title should not contain !", simplified.contains("!"))
        assertFalse("Simplified title should not contain :", simplified.contains(":"))
    }

    @Test
    fun testToAsciiTitle() {
        val ascii = CsTitleMatcher.toAsciiTitle("Şahane Aile — Çok İyi")
        assertEquals("Sahane Aile   Cok Iyi", ascii)
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private fun createSearchResponse(name: String, url: String = "http://example.com", year: Int?): SearchResponse {
        val cls = Class.forName("com.lagradost.cloudstream3.AnimeSearchResponse")
        val ctor = cls.constructors.firstOrNull { it.parameterCount >= 3 }
            ?: throw IllegalStateException("Could not find AnimeSearchResponse constructor")
        val args = Array<Any?>(ctor.parameterCount) { null }
        args[0] = name
        args[1] = url
        args[2] = "DummyProvider"
        if (ctor.parameterCount > 3) args[3] = TvType.Anime

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

        if (year != null) {
            try {
                val field = cls.getDeclaredField("year")
                field.isAccessible = true
                field.set(instance, year)
            } catch (_: Exception) {}
        }
        return instance
    }

    private fun createSearchResponse(name: String, year: Int?): SearchResponse {
        return createSearchResponse(name, "http://example.com", year)
    }
}
