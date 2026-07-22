package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.kitsugi.animelist.utils.*

class KitsugiStudioClient {

    suspend fun fetchStudioDetail(
        source: String,
        studioId: Int,
        name: String? = null
    ): KitsugiStudioDetail? {
        return withContext(Dispatchers.IO) {
            if (studioId <= 0) return@withContext null
            when (source.lowercase()) {
                "jikan", "mal" -> {
                    val jikanRes = fetchJikanStudioDetail(studioId)
                    val detail = if (jikanRes != null) {
                        jikanRes
                    } else if (!name.isNullOrBlank()) {
                        fetchAniListStudioByName(name)
                    } else {
                        null
                    }
                    if (detail != null && !detail.name.isNullOrBlank() &&
                        com.kitsugi.animelist.KitsugiApplication.getInstance()?.let { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(it) } != null) {
                        val aniListDetail = fetchAniListStudioByName(detail.name)
                        if (aniListDetail != null) {
                            detail.copy(isFavourite = aniListDetail.isFavourite, aniListId = aniListDetail.id)
                        } else detail
                    } else detail
                }
                "anilist" -> fetchAniListStudioDetail(studioId)
                "tmdb" -> {
                    val tmdbRes = fetchTmdbStudioDetail(studioId)
                    if (tmdbRes != null && !tmdbRes.name.isNullOrBlank() &&
                        com.kitsugi.animelist.KitsugiApplication.getInstance()?.let { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(it) } != null) {
                        val aniListDetail = fetchAniListStudioByName(tmdbRes.name)
                        if (aniListDetail != null) {
                            tmdbRes.copy(isFavourite = aniListDetail.isFavourite, aniListId = aniListDetail.id)
                        } else tmdbRes
                    } else tmdbRes
                }
                else -> null
            }
        }
    }

    private suspend fun fetchTmdbStudioDetail(studioId: Int): KitsugiStudioDetail? {
        val apiKey = TmdbApiClient.getActiveApiKey()
        val infoUrl = URL("https://api.themoviedb.org/3/company/$studioId?api_key=$apiKey")
        val moviesUrl = URL("https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_companies=$studioId&language=tr-TR&sort_by=popularity.desc")
        val tvUrl = URL("https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&with_companies=$studioId&language=tr-TR&sort_by=popularity.desc")

        return runCatching {
            val detailResponse = KitsugiApiBase.executeGetRequest(infoUrl) ?: return@runCatching null
            val detailData = JSONObject(detailResponse)

            val name = detailData.optString("name")
            val hq = detailData.optNullableString("headquarters")
            val country = detailData.optNullableString("origin_country")
            val desc = detailData.optNullableString("description")

            val about = buildString {
                if (!hq.isNullOrBlank()) append("Merkez: $hq\n")
                if (!country.isNullOrBlank()) append("Ülke: $country\n")
                if (!desc.isNullOrBlank()) append(desc)
            }.trim().cleanApiText().takeIf { it.isNotBlank() }

            val logoPath = detailData.optNullableString("logo_path")
            val imageUrl = if (!logoPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w300$logoPath" else null

            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()

            // Movie discover
            val movieResponse = KitsugiApiBase.executeGetRequest(moviesUrl)
            if (movieResponse != null) {
                val root = JSONObject(movieResponse)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val id = item.optInt("id")
                        val title = item.optNullableString("title") ?: item.optNullableString("original_title") ?: "Başlıksız"
                        val posterPath = item.optNullableString("poster_path")
                        val imgUrl = if (!posterPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w185$posterPath" else null
                        mediaWorks.add(
                            KitsugiStaffMediaWork(
                                mediaId = id,
                                mediaTitle = title,
                                mediaImageUrl = imgUrl,
                                mediaType = "movie".toTurkishMediaTypeString(),
                                staffRole = "Yapım Şirketi",
                                source = "tmdb"
                            )
                        )
                    }
                }
            }

            // TV discover
            val tvResponse = KitsugiApiBase.executeGetRequest(tvUrl)
            if (tvResponse != null) {
                val root = JSONObject(tvResponse)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val id = item.optInt("id")
                        val title = item.optNullableString("name") ?: item.optNullableString("original_name") ?: "Başlıksız"
                        val posterPath = item.optNullableString("poster_path")
                        val imgUrl = if (!posterPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w185$posterPath" else null
                        mediaWorks.add(
                            KitsugiStaffMediaWork(
                                mediaId = id,
                                mediaTitle = title,
                                mediaImageUrl = imgUrl,
                                mediaType = "tv".toTurkishMediaTypeString(),
                                staffRole = "Yapım Şirketi",
                                source = "tmdb"
                            )
                        )
                    }
                }
            }

            KitsugiStudioDetail(
                id = studioId,
                name = name,
                isMain = true,
                imageUrl = imageUrl,
                favorites = null,
                established = null,
                about = about,
                mediaWorks = mediaWorks.distinctBy { it.mediaId }
            )
        }.getOrNull()
    }

    private suspend fun fetchJikanStudioDetail(studioId: Int): KitsugiStudioDetail? {
        val infoUrl = URL("https://api.jikan.moe/v4/producers/$studioId")
        val mediaUrl = URL("https://api.jikan.moe/v4/anime?producers=$studioId&order_by=start_date&sort=desc&limit=80&sfw=false")

        return runCatching {
            // Jikan rate limit: maks 3 istek/saniye — her çağrı runWithRateLimit ile korunuyor
            val infoResponse = KitsugiApiBase.runWithRateLimit {
                KitsugiApiBase.executeGetRequest(infoUrl)
            } ?: return@runCatching null
            val infoRoot = JSONObject(infoResponse)
            val infoData = infoRoot.optJSONObject("data") ?: return@runCatching null

            // Jikan /producers/{id}: isim data.titles[].title (type=="Default") altında,
            // üst seviye data.name alanı bu endpoint'te mevcut değil.
            val titlesArr = infoData.optJSONArray("titles")
            val name: String = if (titlesArr != null) {
                var defaultTitle: String? = null
                var anyTitle: String? = null
                for (i in 0 until titlesArr.length()) {
                    val t = titlesArr.optJSONObject(i) ?: continue
                    val type = t.optNullableString("type")
                    val title = t.optNullableString("title")
                    if (!title.isNullOrBlank()) {
                        if (anyTitle == null) anyTitle = title
                        if (type.equals("Default", ignoreCase = true) ||
                            type.equals("English", ignoreCase = true)
                        ) {
                            defaultTitle = title
                        }
                    }
                }
                defaultTitle ?: anyTitle ?: infoData.optNullableString("name") ?: "Bilinmeyen"
            } else {
                infoData.optNullableString("name") ?: "Bilinmeyen"
            }

            val favorites = infoData.optionalPositiveInt("favorites")
            val established = infoData.optNullableString("established")
            val about = infoData.optNullableString("about")?.cleanApiText()

            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
            val mediaResponse = KitsugiApiBase.runWithRateLimit {
                KitsugiApiBase.executeGetRequest(mediaUrl)
            }
            if (mediaResponse != null) {
                val mediaRoot = JSONObject(mediaResponse)
                val mediaData = mediaRoot.optJSONArray("data")
                if (mediaData != null) {
                    for (i in 0 until mediaData.length()) {
                        val item = mediaData.optJSONObject(i) ?: continue
                        val id = item.optInt("mal_id")
                        val title = item.optNullableString("title") ?: "Bilinmeyen"
                        val imageUrl = item.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                        val type = item.optNullableString("type").orEmpty().lowercase()

                        mediaWorks.add(
                            KitsugiStaffMediaWork(
                                mediaId = id,
                                mediaTitle = title,
                                mediaImageUrl = imageUrl,
                                mediaType = if (type.contains("manga")) "manga".toTurkishMediaTypeString() else "anime".toTurkishMediaTypeString(),
                                staffRole = "Ana Stüdyo",
                                source = "jikan"
                            )
                        )
                    }
                }
            }

            val imagesObj = infoData.optJSONObject("images")
            val imageUrl = imagesObj?.optJSONObject("jpg")?.optNullableString("image_url")

            KitsugiStudioDetail(
                id = studioId,
                name = name,
                isMain = true,
                imageUrl = imageUrl,
                favorites = favorites,
                established = established,
                about = about,
                mediaWorks = mediaWorks.distinctBy { it.mediaId }
            )
        }.getOrNull()
    }

    private suspend fun fetchAniListStudioDetail(studioId: Int): KitsugiStudioDetail? {
        val query = """
            query (${'$'}id: Int) {
                Studio(id: ${'$'}id) {
                    id
                    name
                    isAnimationStudio
                    isFavourite
                    favourites
                    media(page: 1, perPage: 80, sort: [START_DATE_DESC]) {
                        nodes {
                            id
                            idMal
                            title { userPreferred english romaji native }
                            coverImage { large }
                            type
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject().put("id", studioId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
            val root = JSONObject(response)
            val data = root.optJSONObject("data")?.optJSONObject("Studio") ?: return@runCatching null

            val name = data.optString("name")
            val favorites = data.optionalPositiveInt("favourites")
            val isAnimationStudio = data.optBoolean("isAnimationStudio", true)

            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
            val mediaObj = data.optJSONObject("media")
            val nodes = mediaObj?.optJSONArray("nodes")
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val node = nodes.optJSONObject(i) ?: continue
                    val id = node.optInt("id")
                    val idMal = node.optionalPositiveInt("idMal")
                    val titleObj = node.optJSONObject("title")
                    val title = titleObj?.optNullableString("userPreferred") ?: "Başlıksız"
                    val titleEnglish = titleObj?.optNullableString("english")
                    val titleNative = titleObj?.optNullableString("native")
                    val titleRomaji = titleObj?.optNullableString("romaji")
                    val imgUrl = node.optJSONObject("coverImage")?.optNullableString("large")
                    val type = node.optNullableString("type").orEmpty().lowercase()

                    val stableId = idMal ?: (100_000_000 + id)

                    mediaWorks.add(
                        KitsugiStaffMediaWork(
                            mediaId = stableId,
                            mediaTitle = title,
                            mediaImageUrl = imgUrl,
                            mediaType = type.toTurkishMediaTypeString(),
                            staffRole = "Ana Stüdyo",
                            source = if (idMal != null) "jikan" else "anilist",
                            titleEnglish = titleEnglish,
                            titleJapanese = titleNative,
                            titleRomaji = titleRomaji
                        )
                    )
                }
            }

            val isFavourite = data.optBoolean("isFavourite", false)
            KitsugiStudioDetail(
                id = studioId,
                name = name,
                isMain = isAnimationStudio,
                imageUrl = null,
                favorites = favorites,
                established = null,
                about = null,
                mediaWorks = mediaWorks.distinctBy { it.mediaId },
                isFavourite = isFavourite,
                aniListId = studioId
            )
        }.getOrNull()
    }

    private suspend fun fetchAniListStudioByName(name: String): KitsugiStudioDetail? {
        val query = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 1) {
                    studios(search: ${'$'}search) {
                        id
                        name
                        isAnimationStudio
                        isFavourite
                        favourites
                        media(page: 1, perPage: 80, sort: [START_DATE_DESC]) {
                            nodes {
                                id
                                idMal
                                title { userPreferred english romaji native }
                                coverImage { large }
                                type
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject().put("search", name)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
            val root = JSONObject(response)
            val studiosArr = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("studios") ?: return@runCatching null
            if (studiosArr.length() == 0) return@runCatching null
            val studioObj = studiosArr.getJSONObject(0)

            val studioId = studioObj.optInt("id")
            val studioName = studioObj.optString("name")
            val favorites = studioObj.optionalPositiveInt("favourites")
            val isAnimationStudio = studioObj.optBoolean("isAnimationStudio", true)

            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
            val mediaObj = studioObj.optJSONObject("media")
            val nodes = mediaObj?.optJSONArray("nodes")
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val node = nodes.optJSONObject(i) ?: continue
                    val id = node.optInt("id")
                    val idMal = node.optionalPositiveInt("idMal")
                    val titleObj = node.optJSONObject("title")
                    val title = titleObj?.optNullableString("userPreferred") ?: "Başlıksız"
                    val titleEnglish = titleObj?.optNullableString("english")
                    val titleNative = titleObj?.optNullableString("native")
                    val titleRomaji = titleObj?.optNullableString("romaji")
                    val imgUrl = node.optJSONObject("coverImage")?.optNullableString("large")
                    val type = node.optNullableString("type").orEmpty().lowercase()

                    val stableId = idMal ?: (100_000_000 + id)

                    mediaWorks.add(
                        KitsugiStaffMediaWork(
                            mediaId = stableId,
                            mediaTitle = title,
                            mediaImageUrl = imgUrl,
                            mediaType = type.toTurkishMediaTypeString(),
                            staffRole = "Ana Stüdyo",
                            source = if (idMal != null) "jikan" else "anilist",
                            titleEnglish = titleEnglish,
                            titleJapanese = titleNative,
                            titleRomaji = titleRomaji
                        )
                    )
                }
            }

            val isFavourite = studioObj.optBoolean("isFavourite", false)
            KitsugiStudioDetail(
                id = studioId,
                name = studioName,
                isMain = isAnimationStudio,
                imageUrl = null,
                favorites = favorites,
                established = null,
                about = null,
                mediaWorks = mediaWorks.distinctBy { it.mediaId },
                isFavourite = isFavourite,
                aniListId = studioId
            )
        }.getOrNull()
    }
}
