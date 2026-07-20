package com.kitsugi.animelist.data.remote

import org.json.JSONObject
import java.net.URL

/**
 * AnimeThemes.moe API'sinden açılış/kapanış müziklerini çeker.
 * [KitsugiJikanDetailClient] ve [KitsugiAniListDetailClient] tarafından paylaşılır.
 */
internal object KitsugiAnimeThemesClient {

    suspend fun fetchAnimeThemes(externalId: Int, site: String): Pair<List<KitsugiTheme>, List<KitsugiTheme>> {
        val urlString = "https://api.animethemes.moe/anime?" +
            "filter[has]=resources&filter[site]=$site&filter[external_id]=$externalId" +
            "&include=animethemes.song.artists,animethemes.animethemeentries.videos"
        val url = runCatching { URL(urlString) }.getOrNull() ?: return Pair(emptyList(), emptyList())

        val response = KitsugiApiBase.executeGetRequest(url) ?: return Pair(emptyList(), emptyList())

        return try {
            val root = JSONObject(response)
            val animeArr = root.optJSONArray("anime") ?: return Pair(emptyList(), emptyList())
            if (animeArr.length() == 0) return Pair(emptyList(), emptyList())

            val animeObj = animeArr.getJSONObject(0)
            val themesArr = animeObj.optJSONArray("animethemes") ?: return Pair(emptyList(), emptyList())

            val openings = mutableListOf<KitsugiTheme>()
            val endings = mutableListOf<KitsugiTheme>()

            for (i in 0 until themesArr.length()) {
                val themeObj = themesArr.getJSONObject(i)
                val type = themeObj.optString("type")
                val slug = themeObj.optNullableString("slug") ?: ""

                val songObj = themeObj.optJSONObject("song")
                val title = songObj?.optNullableString("title") ?: continue

                val artists = mutableListOf<String>()
                songObj.optJSONArray("artists")?.let { artistsArr ->
                    for (j in 0 until artistsArr.length()) {
                        val artistObj = artistsArr.getJSONObject(j)
                        val name = artistObj.optString("name")
                        if (name.isNotBlank()) artists.add(name)
                    }
                }

                // En iyi video URL'ini çek: nc=false (sublu) + en yüksek çözünürlük
                var videoUrl: String? = null
                themeObj.optJSONArray("animethemeentries")?.let { entriesArr ->
                    if (entriesArr.length() > 0) {
                        val entry = entriesArr.getJSONObject(0)
                        val videos = entry.optJSONArray("videos")
                        if (videos != null && videos.length() > 0) {
                            // Önce sublu olmayan 720p WEB versiyonu ara
                            for (v in 0 until videos.length()) {
                                val vid = videos.getJSONObject(v)
                                val link = vid.optNullableString("link")
                                val source = vid.optString("source")
                                val res = vid.optInt("resolution")
                                if (link != null && !vid.optBoolean("subbed") && source == "WEB" && res >= 720) {
                                    videoUrl = link
                                    break
                                }
                            }
                            // Bulamazsa ilk mevcut linki al
                            if (videoUrl == null) {
                                videoUrl = videos.getJSONObject(0).optNullableString("link")
                            }
                        }
                    }
                }

                val label = buildString {
                    append("\"$title\"")
                    if (artists.isNotEmpty()) {
                        append(" by ")
                        append(artists.joinToString(", "))
                    }
                    if (slug.isNotBlank()) append(" ($slug)")
                }

                val theme = KitsugiTheme(label = label, videoUrl = videoUrl)
                if (type == "OP") openings.add(theme)
                else if (type == "ED") endings.add(theme)
            }

            Pair(openings, endings)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }

    /**
     * AniList streaming episode başlıklarından bölüm numarasını çıkarmaya çalışır.
     * Örnek: "Episode 5", "Ep. 12", "#7 – Title", "S1E4", "Bölüm 3"
     */
    fun parseEpisodeNumberFromTitle(title: String): Int? {
        val patterns = listOf(
            Regex("""[Ss]\d+[Ee](\d+)"""),                                    // S1E5
            Regex("""[Ee]p(?:isode)?\.?\s*(\d+)""", RegexOption.IGNORE_CASE), // Episode 5, Ep. 5
            Regex("""#(\d+)"""),                                               // #7 – Title
            Regex("""B[öo]l[üu]m\s+(\d+)""", RegexOption.IGNORE_CASE),        // Bölüm 3
            Regex("""^(\d+)\s*[-–—.]"""),                                      // "5 - Title"
            Regex("""\b(\d+)\b""")                                             // son çare: herhangi sayı
        )
        for (pattern in patterns) {
            val num = pattern.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (num != null && num > 0 && num < 5000) return num
        }
        return null
    }
}
