package com.kitsugi.animelist.data.auth

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import okhttp3.Request
import com.kitsugi.animelist.data.remote.optNullableString

object MalImportManager {
    private const val ANIME_LIST_URL =
        "https://api.myanimelist.net/v2/users/@me/animelist" +
        "?fields=id,title,alternative_titles,main_picture,start_date,end_date,media_type,status,num_episodes,start_season,nsfw,genres,rating," +
        "list_status{status,score,num_episodes_watched,start_date,finish_date,is_favorited,priority,is_repeating,num_times_rewatched,rewatch_value,tags,comments,updated_at}" +
        "&limit=100&nsfw=true"

    private const val MANGA_LIST_URL =
        "https://api.myanimelist.net/v2/users/@me/mangalist" +
        "?fields=id,title,alternative_titles,main_picture,start_date,end_date,media_type,status,num_chapters,start_season,num_volumes,nsfw,genres," +
        "list_status{status,score,num_chapters_read,num_volumes_read,start_date,finish_date,is_favorited,priority,is_repeating,num_times_reread,reread_value,tags,comments,updated_at}" +
        "&limit=100&nsfw=true"

    suspend fun fetchAnimeList(
        accessToken: String,
        showAdultContent: Boolean = false
    ): List<MediaEntry> {
        val url = ANIME_LIST_URL.replace("&nsfw=true", "&nsfw=$showAdultContent")
        return withContext(Dispatchers.IO) {
            fetchPaginatedList(accessToken, url)
        }
    }

    suspend fun fetchMangaList(
        accessToken: String,
        showAdultContent: Boolean = false
    ): List<MediaEntry> {
        val url = MANGA_LIST_URL.replace("&nsfw=true", "&nsfw=$showAdultContent")
        return withContext(Dispatchers.IO) {
            fetchPaginatedList(accessToken, url)
        }
    }

    suspend fun fetchAllLists(
        accessToken: String,
        showAdultContent: Boolean = false
    ): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            val anime = fetchAnimeList(accessToken, showAdultContent)
            val manga = fetchMangaList(accessToken, showAdultContent)
            anime + manga
        }
    }

    private fun fetchPaginatedList(
        accessToken: String,
        startUrl: String
    ): List<MediaEntry> {
        val allEntries = mutableListOf<MediaEntry>()
        var nextUrl: String? = startUrl

        while (nextUrl != null) {
            val response = getJson(accessToken = accessToken, urlString = nextUrl)
            val root = JSONObject(response)
            val dataArray = root.optJSONArray("data")

            if (dataArray != null) {
                for (index in 0 until dataArray.length()) {
                    val wrapper = dataArray.optJSONObject(index) ?: continue
                    val node = wrapper.optJSONObject("node") ?: continue
                    val listStatus = wrapper.optJSONObject("list_status") ?: JSONObject()
                    allEntries.add(nodeToMediaEntry(node = node, listStatus = listStatus))
                }
            }

            nextUrl = root.optJSONObject("paging")?.optNullableString("next")
        }

        return allEntries
    }

    private fun nodeToMediaEntry(
        node: JSONObject,
        listStatus: JSONObject
    ): MediaEntry {
        val malId = node.optInt("id", 0)

        val title = node.optNullableString("title") ?: "Başlıksız"

        val rawMediaType = node.optNullableString("media_type").orEmpty()
        val mediaType = if (rawMediaType == "manga" || rawMediaType == "novel" || rawMediaType == "one_shot" || rawMediaType == "manhwa" || rawMediaType == "manhua") {
            MediaType.Manga
        } else {
            MediaType.Anime
        }

        val startSeason = node.optJSONObject("start_season")
        val year = startSeason?.optInt("year", 0)?.takeIf { it > 0 }
            ?: node.optNullableString("start_date")
                .takeIf { it != null && it.length >= 4 }
                ?.take(4)
                ?.toIntOrNull()

        val format = convertMalFormat(rawMediaType)

        val subtitleParts = buildList {
            if (format.isNotBlank()) add(format)
            if (year != null) add(year.toString())
        }

        val picture = node.optJSONObject("main_picture")
        val imageUrl = picture?.optNullableString("large")
            ?: picture?.optNullableString("medium")

        val status = convertMalListStatus(
            listStatus.optNullableString("status").orEmpty()
        )

        val score = listStatus.optInt("score", 0).takeIf { it > 0 }

        val progress = if (mediaType == MediaType.Manga) {
            listStatus.optInt("num_chapters_read", 0)
        } else {
            listStatus.optInt("num_episodes_watched", 0)
        }.coerceAtLeast(0)

        val total = if (mediaType == MediaType.Manga) {
            node.optInt("num_chapters", 0).takeIf { it > 0 }
        } else {
            node.optInt("num_episodes", 0).takeIf { it > 0 }
        }

        val volumeProgress = if (mediaType == MediaType.Manga) {
            listStatus.optInt("num_volumes_read", 0)
        } else {
            0
        }.coerceAtLeast(0)

        val priority = listStatus.optInt("priority", 0)
        val isRepeating = listStatus.optBoolean("is_repeating", false)
        val repeatCount = if (mediaType == MediaType.Manga) {
            listStatus.optInt("num_times_reread", 0)
        } else {
            listStatus.optInt("num_times_rewatched", 0)
        }.coerceAtLeast(0)

        val repeatValue = if (mediaType == MediaType.Manga) {
            listStatus.optInt("reread_value", 0)
        } else {
            listStatus.optInt("rewatch_value", 0)
        }.coerceAtLeast(0)

        val notes = listStatus.optNullableString("comments")

        val tagsJsonArray = listStatus.optJSONArray("tags")
        val tags = if (tagsJsonArray != null) {
            val list = mutableListOf<String>()
            for (i in 0 until tagsJsonArray.length()) {
                list.add(tagsJsonArray.optString(i))
            }
            list.joinToString(", ")
        } else null

        val isFavorite = listStatus.optBoolean("is_favorited", false)

        val startDate = listStatus.optNullableString("start_date")
            .takeIf { it?.isValidDateString() == true }

        val endDate = listStatus.optNullableString("finish_date")
            .takeIf { it?.isValidDateString() == true }

        val updatedAtString = listStatus.optNullableString("updated_at")
        val altTitles = node.optJSONObject("alternative_titles")
        val titleEnglish = altTitles?.optNullableString("en")
        val titleJapanese = altTitles?.optNullableString("ja")

        val updatedAt = if (!updatedAtString.isNullOrBlank()) {
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.parse(updatedAtString).epochSecond
                } else {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    (sdf.parse(updatedAtString)?.time ?: 0L) / 1000L
                }
            }.getOrDefault(0L)
        } else {
            0L
        }

        val rating = node.optNullableString("rating").orEmpty().lowercase()
        val isAdultRating = rating.contains("rx") || rating.contains("hentai")

        val genresArray = node.optJSONArray("genres")
        val hasAdultGenre = if (genresArray != null) {
            var found = false
            for (i in 0 until genresArray.length()) {
                val genreObj = genresArray.optJSONObject(i)
                if (genreObj != null) {
                    val genreName = genreObj.optNullableString("name").orEmpty().lowercase()
                    if (genreName == "hentai") {
                        found = true
                        break
                    }
                }
            }
            found
        } else false

        val isNsfwBlack = node.optNullableString("nsfw") == "black"
        val isAdult = isNsfwBlack || isAdultRating || hasAdultGenre

        return MediaEntry(
            id = 0,
            title = title,
            subtitle = if (subtitleParts.isEmpty()) {
                "MyAnimeList'ten içe aktarıldı"
            } else {
                subtitleParts.joinToString(", ")
            },
            type = mediaType,
            status = status,
            score = score,
            progress = progress,
            total = total,
            isFavorite = isFavorite,
            isAdult = isAdult,
            source = "mal",
            malId = malId,
            imageUrl = imageUrl,
            year = year,
            synopsis = null,
            startDate = startDate,
            endDate = endDate,
            notes = notes,
            tags = tags,
            priority = priority,
            isRepeating = isRepeating,
            repeatCount = repeatCount,
            repeatValue = repeatValue,
            volumeProgress = volumeProgress,
            updatedAt = updatedAt,
            titleEnglish = titleEnglish,
            titleJapanese = titleJapanese
        )
    }

    private fun getJson(
        accessToken: String,
        urlString: String
    ): String {
        val request = Request.Builder()
            .url(urlString)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorText = response.body?.string().orEmpty()
                    throw IllegalStateException(
                        "MAL API hatası: ${response.code} $errorText"
                    )
                }

                return response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw IllegalStateException("MAL bağlantı hatası: ${e.message}", e)
        }
    }

    private fun convertMalListStatus(
        malStatus: String
    ): WatchStatus {
        return when (malStatus.lowercase()) {
            "watching", "reading", "rewatching", "rereading" -> WatchStatus.Watching
            "completed" -> WatchStatus.Completed
            "dropped" -> WatchStatus.Dropped
            "on_hold", "hold", "paused" -> WatchStatus.Paused
            "plan_to_watch", "plan_to_read", "planning" -> WatchStatus.Planned
            else -> WatchStatus.Planned
        }
    }

    private fun convertMalFormat(
        malFormat: String
    ): String {
        return when (malFormat) {
            "tv" -> "TV"
            "ova" -> "OVA"
            "movie" -> "Movie"
            "special" -> "Special"
            "ona" -> "ONA"
            "music" -> "Music"
            "manga" -> "Manga"
            "novel" -> "Novel"
            "one_shot" -> "One Shot"
            "manhwa" -> "Manhwa"
            "manhua" -> "Manhua"
            else -> malFormat.replaceFirstChar { it.uppercase() }
        }
    }

    private fun String?.isValidDateString(): Boolean {
        if (this.isNullOrBlank()) return false

        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        return matches(regex)
    }

    data class MalUserProfile(
        val name: String,
        val pictureUrl: String?
    )

    suspend fun fetchUserProfile(
        accessToken: String
    ): MalUserProfile {
        return withContext(Dispatchers.IO) {
            val response = getJson(accessToken, "https://api.myanimelist.net/v2/users/@me")
            val json = JSONObject(response)
            MalUserProfile(
                name = json.optNullableString("name") ?: "MAL Kullanıcısı",
                pictureUrl = json.optNullableString("picture")
            )
        }
    }
}