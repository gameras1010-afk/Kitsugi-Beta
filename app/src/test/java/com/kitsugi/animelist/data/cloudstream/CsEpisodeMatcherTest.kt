package com.kitsugi.animelist.data.cloudstream

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import org.junit.Assert.*
import org.junit.Test

/**
 * CsEpisodeMatcher için kapsamlı JVM unit testleri.
 *
 * Her Türk Cloudstream plugin'inin farklı LoadResponse yapısı:
 *
 *  TurkAnime  → AnimeLoadResponse, tüm bölümler DubStatus.None + season=null/1 (flat)
 *  AnimeciX   → AnimeLoadResponse, sezon buketi explicit, episode numaralı
 *  DiziBox    → TvSeriesLoadResponse, episode field null, sadece season var (index-based)
 *  Dizilla    → TvSeriesLoadResponse, episode numaralı, season null (flat)
 *  DiziPal    → TvSeriesLoadResponse, season + episode explicit
 *  Film       → MovieLoadResponse, dataUrl fallback to url
 *  OVA/Tek   → Herhangi bir Response, episodes.size == 1 → ep=1 fallback
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
class CsEpisodeMatcherTest {

    // ─── MovieLoadResponse Tests ──────────────────────────────────────────────

    @Test
    fun testMovieLoadResponseWithDataUrl() {
        val resp = buildMovieResponse(url = "https://hdfilm.com/film/inception", dataUrl = "https://hdfilm.com/play/inception-hd")
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("dataUrl should be returned when non-blank", "https://hdfilm.com/play/inception-hd", data)
    }

    @Test
    fun testMovieLoadResponseDataUrlNullFallsBackToUrl() {
        val resp = buildMovieResponse(url = "https://hdfilm.com/film/interstellar", dataUrl = null)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("url should be returned when dataUrl is null", "https://hdfilm.com/film/interstellar", data)
    }

    @Test
    fun testMovieLoadResponseDataUrlBlankFallsBackToUrl() {
        val resp = buildMovieResponse(url = "https://sinema.cx/film/blade-runner", dataUrl = "   ")
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("url should be returned when dataUrl is blank", "https://sinema.cx/film/blade-runner", data)
    }

    // ─── AnimeLoadResponse: TurkAnime Flat List ───────────────────────────────

    @Test
    fun testAnimeResponseTurkAnimeFlatList_Episode3() {
        // TurkAnime: all episodes under DubStatus.None, season=null, ep=1-based index
        val episodes = (1..24).map { ep -> buildAnimeEpisode(data = "ep$ep", episode = ep, season = null) }
        val resp = buildAnimeResponse(url = "https://turkanime.tv/anime/bleach", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 3)
        assertEquals("ep3", data)
    }

    @Test
    fun testAnimeResponseTurkAnimeFlatList_LastEpisode() {
        val episodes = (1..12).map { ep -> buildAnimeEpisode(data = "ep$ep", episode = ep, season = null) }
        val resp = buildAnimeResponse(url = "https://turkanime.tv/anime/frieren", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 12)
        assertEquals("ep12", data)
    }

    @Test
    fun testAnimeResponseFlatListNoEpisodeMeta_IndexBased() {
        // When ALL episodes have no episode number and no season (raw data list)
        val episodes = listOf(
            buildAnimeEpisode(data = "data_1", episode = null, season = null),
            buildAnimeEpisode(data = "data_2", episode = null, season = null),
            buildAnimeEpisode(data = "data_3", episode = null, season = null),
            buildAnimeEpisode(data = "data_4", episode = null, season = null),
            buildAnimeEpisode(data = "data_5", episode = null, season = null)
        )
        val resp = buildAnimeResponse(url = "https://animeler.pw/anime/some-anime", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 3)
        // Index-based fallback: episode 3 → index 2 (0-based)
        assertEquals("data_3", data)
    }

    @Test
    fun testAnimeResponseSingleEpisodeOVA() {
        // Single episode OVA — should always return the one episode for ep=1
        val episodes = listOf(buildAnimeEpisode(data = "ova_data", episode = null, season = null))
        val resp = buildAnimeResponse(url = "https://animeler.pw/anime/ova", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("ova_data", data)
    }

    // ─── AnimeLoadResponse: AnimeciX Multi-Season ────────────────────────────

    @Test
    fun testAnimeResponseMultiSeasonExact_S2E5() {
        // AnimeciX: explicit season and episode numbers per episode
        val s1eps = (1..12).map { ep -> buildAnimeEpisode(data = "s1e$ep", episode = ep, season = 1) }
        val s2eps = (1..12).map { ep -> buildAnimeEpisode(data = "s2e$ep", episode = ep, season = 2) }
        val resp = buildAnimeResponse(url = "https://anm.cx/anime/jjk", dubbed = s1eps + s2eps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 5)
        assertEquals("s2e5", data)
    }

    @Test
    fun testAnimeResponseMultiSeasonExact_S1E1() {
        val s1eps = (1..13).map { ep -> buildAnimeEpisode(data = "s1e$ep", episode = ep, season = 1) }
        val s2eps = (1..13).map { ep -> buildAnimeEpisode(data = "s2e$ep", episode = ep, season = 2) }
        val resp = buildAnimeResponse(url = "https://anm.cx/anime/aot", dubbed = s1eps + s2eps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("s1e1", data)
    }

    @Test
    fun testAnimeResponseSubPreferredOverDub() {
        // When both Sub and Dub exist, Sub is preferred
        val subEps = listOf(buildAnimeEpisode(data = "sub_ep1", episode = 1, season = 1))
        val dubEps = listOf(buildAnimeEpisode(data = "dub_ep1", episode = 1, season = 1))
        val resp = buildAnimeResponse(url = "https://anm.cx/anime/boruto", subbed = subEps, dubbed = dubEps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("Sub should be preferred over Dub", "sub_ep1", data)
    }

    // ─── AnimeLoadResponse: TreatSeason1AsTarget (Flat with foundSeason) ─────

    @Test
    fun testAnimeResponseTreatSeason1AsTarget() {
        // The response represents season 2, but all episodes are stored under season=1
        // TrAnimeci / AsyaAnimeleri flat-bucket pattern
        val episodes = (1..12).map { ep -> buildAnimeEpisode(data = "s2e$ep", episode = ep, season = 1) }
        // Response URL has "2-sezon" slug → foundSeason=2
        val resp = buildAnimeResponse(url = "https://asyaanimeleri.top/anime/aot-2-sezon", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 7)
        // treatSeason1AsTarget should activate, episodes.season(1) treated as season 2
        assertEquals("s2e7", data)
    }

    // ─── AnimeLoadResponse: Name-Based Fallback ───────────────────────────────

    @Test
    fun testAnimeResponseNameBasedFallback_S2E5Format() {
        // Episode has no season/episode numbers but the name contains "s2e5"
        val ep = buildAnimeEpisodeWithName(data = "target_data", name = "S2E5 - Frieren vs Aura", episode = null, season = null)
        val other = buildAnimeEpisode(data = "other_data", episode = null, season = null)
        val resp = buildAnimeResponse(url = "https://animeler.pw/anime/frieren", dubNone = listOf(other, ep))
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 5)
        assertEquals("target_data", data)
    }

    @Test
    fun testAnimeResponseNameBasedFallback_TurkishSezonBolumFormat() {
        // "2. Sezon 3. Bölüm" pattern
        val ep = buildAnimeEpisodeWithName(data = "turkish_ep_data", name = "2. sezon 3. bölüm - Şafak Vakti", episode = null, season = null)
        val resp = buildAnimeResponse(url = "https://animeler.pw/anime/test", dubNone = listOf(ep))
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 3)
        assertEquals("turkish_ep_data", data)
    }

    // ─── TvSeriesLoadResponse: DiziPal (Explicit Season + Episode) ───────────

    @Test
    fun testTvSeriesExplicitSeasonEpisode_S3E7() {
        val episodes = (1..13).map { ep -> buildTvEpisode(data = "s3e$ep", episode = ep, season = 3) }
        val resp = buildTvSeriesResponse(url = "https://dizipal950.com/dizi/cukur", episodes = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 3, episode = 7)
        assertEquals("s3e7", data)
    }

    @Test
    fun testTvSeriesMultipleSeasonsExact_S2E1() {
        val s1eps = (1..8).map { ep -> buildTvEpisode(data = "s1e$ep", episode = ep, season = 1) }
        val s2eps = (1..8).map { ep -> buildTvEpisode(data = "s2e$ep", episode = ep, season = 2) }
        val resp = buildTvSeriesResponse(url = "https://dizipal950.com/dizi/stranger-things", episodes = s1eps + s2eps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 1)
        assertEquals("s2e1", data)
    }

    // ─── TvSeriesLoadResponse: DiziBox (episode=null, season-based index) ─────

    @Test
    fun testTvSeriesSeasonBasedIndexFallback_DiziBox() {
        // DiziBox: episodes have season set, but episode number is null → index-based
        val s2eps = listOf(
            buildTvEpisode(data = "s2idx0", episode = null, season = 2),
            buildTvEpisode(data = "s2idx1", episode = null, season = 2),
            buildTvEpisode(data = "s2idx2", episode = null, season = 2),
            buildTvEpisode(data = "s2idx3", episode = null, season = 2),
            buildTvEpisode(data = "s2idx4", episode = null, season = 2)
        )
        val s1eps = listOf(buildTvEpisode(data = "s1idx0", episode = null, season = 1))
        val resp = buildTvSeriesResponse(url = "https://dizibox.live/dizi/cukur-2-sezon", episodes = s1eps + s2eps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 2, episode = 3)
        // episode 3 → index 2 within season 2 episodes
        assertEquals("s2idx2", data)
    }

    @Test
    fun testTvSeriesSeasonBasedIndexFallback_FirstEpisode() {
        val s3eps = listOf(
            buildTvEpisode(data = "s3ep1", episode = null, season = 3),
            buildTvEpisode(data = "s3ep2", episode = null, season = 3)
        )
        val resp = buildTvSeriesResponse(url = "https://dizibox.live/dizi/test-3-sezon", episodes = s3eps)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 3, episode = 1)
        assertEquals("s3ep1", data)
    }

    // ─── TvSeriesLoadResponse: Dizilla (flat, no season, episode numbered) ────

    @Test
    fun testTvSeriesFlatNoSeason_EpisodeNumbered() {
        // Dizilla: all episodes listed without season, episode number present
        val episodes = (1..26).map { ep -> buildTvEpisode(data = "ep$ep", episode = ep, season = null) }
        val resp = buildTvSeriesResponse(url = "https://dizilla.nl/dizi/son", episodes = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 15)
        assertEquals("ep15", data)
    }

    @Test
    fun testTvSeriesFlatAllNoMeta_IndexBased() {
        // All episodes have no season and no episode number — full index-based fallback
        val episodes = listOf(
            buildTvEpisode(data = "flat1", episode = null, season = null),
            buildTvEpisode(data = "flat2", episode = null, season = null),
            buildTvEpisode(data = "flat3", episode = null, season = null)
        )
        val resp = buildTvSeriesResponse(url = "https://dizilla.nl/dizi/mini", episodes = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 2)
        assertEquals("flat2", data)
    }

    // ─── TvSeriesLoadResponse: Single Episode (Mini-Series) ──────────────────

    @Test
    fun testTvSeriesSingleEpisodeFallback() {
        val episodes = listOf(buildTvEpisode(data = "mini_ep", episode = null, season = null))
        val resp = buildTvSeriesResponse(url = "https://dizilla.nl/dizi/mini", episodes = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertEquals("mini_ep", data)
    }

    // ─── TvSeriesLoadResponse: TreatSeason1AsTarget ──────────────────────────

    @Test
    fun testTvSeriesTreatSeason1AsTarget_Slug() {
        // URL slug: "cukur-3-sezon" → foundSeason=3
        // Episodes stored under season=1 (site uses season 1 internally for all)
        val episodes = (1..10).map { ep -> buildTvEpisode(data = "s1e$ep", episode = ep, season = 1) }
        val resp = buildTvSeriesResponse(url = "https://sezonlukdizi.org/dizi/cukur-3-sezon", episodes = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 3, episode = 5)
        assertEquals("s1e5", data)
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────────

    @Test
    fun testReturnsNullForOutOfBoundsEpisode() {
        val episodes = (1..5).map { ep -> buildAnimeEpisode(data = "ep$ep", episode = ep, season = 1) }
        val resp = buildAnimeResponse(url = "https://turkanime.tv/anime/frieren", dubNone = episodes)
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 999)
        // No episode 999 exists in 5-episode list → should be null
        assertNull(data)
    }

    @Test
    fun testEmptyEpisodeListReturnsNull() {
        val resp = buildAnimeResponse(url = "https://example.com/anime/empty", dubNone = emptyList())
        val data = CsEpisodeMatcher.findEpisodeData(resp, season = 1, episode = 1)
        assertNull(data)
    }

    // ─── Reflection Helpers (tested indirectly through findEpisodeData) ────────

    @Test
    fun testGetEpisodeNumber() {
        val ep = buildAnimeEpisode(data = "d", episode = 7, season = 2)
        assertEquals(7, CsEpisodeMatcher.getEpisodeNumber(ep))
    }

    @Test
    fun testGetEpisodeSeason() {
        val ep = buildAnimeEpisode(data = "d", episode = 1, season = 3)
        assertEquals(3, CsEpisodeMatcher.getEpisodeSeason(ep))
    }

    @Test
    fun testGetEpisodeData() {
        val ep = buildAnimeEpisode(data = "the_data_string", episode = 1, season = 1)
        assertEquals("the_data_string", CsEpisodeMatcher.getEpisodeData(ep))
    }

    // ─── Builder Helpers ──────────────────────────────────────────────────────

    private fun buildAnimeEpisode(data: String, episode: Int?, season: Int?): Any {
        return buildAnimeEpisodeWithName(data = data, name = null, episode = episode, season = season)
    }

    private fun buildAnimeEpisodeWithName(data: String, name: String?, episode: Int?, season: Int?): Any {
        // Use Cloudstream3's EpisodeObject via reflection — mirrors what real plugins create
        val cls = try {
            Class.forName("com.lagradost.cloudstream3.Episode")
        } catch (_: ClassNotFoundException) {
            // Older cloudstream may use "EpisodeData" class name
            Class.forName("com.lagradost.cloudstream3.EpisodeData")
        }
        val ctors = cls.constructors.sortedByDescending { it.parameterCount }
        for (ctor in ctors) {
            try {
                val args = Array<Any?>(ctor.parameterCount) { null }
                // Assign data (first String param)
                val paramTypes = ctor.parameterTypes
                for (i in paramTypes.indices) {
                    when (paramTypes[i]) {
                        String::class.java -> if (args[i] == null) args[i] = data
                        Int::class.javaPrimitiveType, Integer::class.java -> {
                            // skip — keep null unless explicitly assigned below
                        }
                    }
                }
                val instance = ctor.newInstance(*args)
                // Set fields via reflection
                setField(instance, "data", data)
                if (name != null) trySetField(instance, "name", name)
                if (episode != null) setField(instance, "episode", episode)
                if (season != null) setField(instance, "season", season)
                return instance
            } catch (_: Exception) { continue }
        }
        throw IllegalStateException("Could not create Episode object via reflection")
    }

    private fun buildAnimeResponse(
        url: String,
        name: String = "TestAnime",
        subbed: List<Any> = emptyList(),
        dubbed: List<Any> = emptyList(),
        dubNone: List<Any> = emptyList()
    ): AnimeLoadResponse {
        val cls = AnimeLoadResponse::class.java
        val ctor = cls.constructors.firstOrNull()
            ?: throw IllegalStateException("No AnimeLoadResponse constructor")

        val args = Array<Any?>(ctor.parameterCount) { null }
        val paramTypes = ctor.parameterTypes
        for (i in paramTypes.indices) {
            when {
                paramTypes[i] == String::class.java && args[i] == null -> args[i] = ""
                paramTypes[i] == TvType::class.java -> args[i] = TvType.Anime
                paramTypes[i] == Map::class.java || paramTypes[i] == java.util.Map::class.java -> args[i] = emptyMap<Any, Any>()
                paramTypes[i] == List::class.java || paramTypes[i] == java.util.List::class.java -> args[i] = emptyList<Any>()
                paramTypes[i] == Boolean::class.javaPrimitiveType -> args[i] = false
                paramTypes[i] == Int::class.javaPrimitiveType || paramTypes[i] == Integer::class.java -> args[i] = 0
            }
        }

        @Suppress("UNCHECKED_CAST")
        val instance = ctor.newInstance(*args) as AnimeLoadResponse
        setField(instance, "name", name)
        setField(instance, "url", url)

        // Set episodes map
        val episodesMap = mutableMapOf<DubStatus, List<Any>>()
        if (subbed.isNotEmpty()) episodesMap[DubStatus.Subbed] = subbed
        if (dubbed.isNotEmpty()) episodesMap[DubStatus.Dubbed] = dubbed
        if (dubNone.isNotEmpty()) episodesMap[DubStatus.None] = dubNone
        setField(instance, "episodes", episodesMap)

        return instance
    }

    private fun buildTvEpisode(data: String, episode: Int?, season: Int?): Any {
        // TvSeriesLoadResponse uses Episode objects same as AnimeLoadResponse
        return buildAnimeEpisode(data = data, episode = episode, season = season)
    }

    private fun buildTvSeriesResponse(url: String, episodes: List<Any>, name: String = "TestSeries"): TvSeriesLoadResponse {
        val cls = TvSeriesLoadResponse::class.java
        val ctor = cls.constructors.firstOrNull()
            ?: throw IllegalStateException("No TvSeriesLoadResponse constructor")

        val args = Array<Any?>(ctor.parameterCount) { null }
        val paramTypes = ctor.parameterTypes
        for (i in paramTypes.indices) {
            when {
                paramTypes[i] == String::class.java && args[i] == null -> args[i] = ""
                paramTypes[i] == TvType::class.java -> args[i] = TvType.TvSeries
                paramTypes[i] == Map::class.java || paramTypes[i] == java.util.Map::class.java -> args[i] = emptyMap<Any, Any>()
                paramTypes[i] == List::class.java || paramTypes[i] == java.util.List::class.java -> args[i] = emptyList<Any>()
                paramTypes[i] == Boolean::class.javaPrimitiveType -> args[i] = false
                paramTypes[i] == Int::class.javaPrimitiveType || paramTypes[i] == Integer::class.java -> args[i] = 0
            }
        }

        @Suppress("UNCHECKED_CAST")
        val instance = ctor.newInstance(*args) as TvSeriesLoadResponse
        setField(instance, "name", name)
        setField(instance, "url", url)
        setField(instance, "episodes", episodes)

        return instance
    }

    private fun buildMovieResponse(url: String, dataUrl: String?, name: String = "TestFilm"): MovieLoadResponse {
        val cls = MovieLoadResponse::class.java
        val ctor = cls.constructors.firstOrNull()
            ?: throw IllegalStateException("No MovieLoadResponse constructor")

        val args = Array<Any?>(ctor.parameterCount) { null }
        val paramTypes = ctor.parameterTypes
        for (i in paramTypes.indices) {
            when {
                paramTypes[i] == String::class.java && args[i] == null -> args[i] = ""
                paramTypes[i] == TvType::class.java -> args[i] = TvType.Movie
                paramTypes[i] == Map::class.java || paramTypes[i] == java.util.Map::class.java -> args[i] = emptyMap<Any, Any>()
                paramTypes[i] == List::class.java || paramTypes[i] == java.util.List::class.java -> args[i] = emptyList<Any>()
                paramTypes[i] == Boolean::class.javaPrimitiveType -> args[i] = false
                paramTypes[i] == Int::class.javaPrimitiveType || paramTypes[i] == Integer::class.java -> args[i] = 0
            }
        }

        @Suppress("UNCHECKED_CAST")
        val instance = ctor.newInstance(*args) as MovieLoadResponse
        setField(instance, "name", name)
        setField(instance, "url", url)
        trySetField(instance, "dataUrl", dataUrl)

        return instance
    }

    // ─── Reflection Utilities ─────────────────────────────────────────────────

    private fun setField(obj: Any, name: String, value: Any?) {
        var cls: Class<*>? = obj.javaClass
        while (cls != null) {
            try {
                val f = cls.getDeclaredField(name)
                f.isAccessible = true
                f.set(obj, value)
                return
            } catch (_: NoSuchFieldException) {
                cls = cls.superclass
            }
        }
    }

    private fun trySetField(obj: Any, name: String, value: Any?) {
        try { setField(obj, name, value) } catch (_: Exception) { }
    }
}
