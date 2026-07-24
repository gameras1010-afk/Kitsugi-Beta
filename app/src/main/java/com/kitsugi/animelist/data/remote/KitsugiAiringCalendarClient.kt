package com.kitsugi.animelist.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

/**
 * AniList `airingSchedules` GraphQL endpoint'inden haftalık yayın takvimini çeker.
 * Sonuç Map<Int, List<AiringEntry>>: key = Calendar.DAY_OF_WEEK
 */
class KitsugiAiringCalendarClient {

    suspend fun fetchWeeklySchedule(accessToken: String? = null, preferredSource: String? = null): Map<Int, List<AiringEntry>> {
        return withContext(Dispatchers.IO) {
            if (preferredSource == "tmdb") {
                fetchTmdbWeeklySchedule()
            } else {
                val (weekStart, weekEnd) = currentWeekRange()
                val rawEntries = fetchAiringSchedule(weekStart, weekEnd, accessToken)
                rawEntries
                    .filter { it.airingAt in weekStart..weekEnd }
                    .groupBy { it.dayOfWeek }
                    .mapValues { (_, list) -> list.sortedBy { it.airingAt } }
            }
        }
    }

    private suspend fun fetchTmdbWeeklySchedule(): Map<Int, List<AiringEntry>> {
        val apiKey = TmdbApiClient.getActiveApiKey()
        val language = TmdbApiClient.getActiveLanguage()
        val scheduleMap = mutableMapOf<Int, MutableList<AiringEntry>>()
        for (day in 1..7) {
            scheduleMap[day] = mutableListOf()
        }

        // Fetch TV Shows on the air
        val tvUrl1 = "https://api.themoviedb.org/3/tv/on_the_air?api_key=$apiKey&language=$language&page=1"
        val tvUrl2 = "https://api.themoviedb.org/3/tv/on_the_air?api_key=$apiKey&language=$language&page=2"
        
        // Fetch Upcoming Movies
        val movieUrl = "https://api.themoviedb.org/3/movie/upcoming?api_key=$apiKey&language=$language&page=1"

        parseTmdbList(tvUrl1, isMovie = false, scheduleMap = scheduleMap)
        parseTmdbList(tvUrl2, isMovie = false, scheduleMap = scheduleMap)
        parseTmdbList(movieUrl, isMovie = true, scheduleMap = scheduleMap)

        return scheduleMap.mapValues { (_, list) -> list.toList() }
    }

    private fun parseTmdbList(
        urlStr: String,
        isMovie: Boolean,
        scheduleMap: MutableMap<Int, MutableList<AiringEntry>>? = null,
        outList: MutableList<AiringEntry>? = null
    ) {
        try {
            val responseText = KitsugiApiBase.executeGetRequest(java.net.URL(urlStr)) ?: return
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val tmdbId = item.optInt("id", 0).takeIf { it > 0 } ?: continue
                val title = if (isMovie) item.optString("title", "") else item.optString("name", "")
                if (title.isBlank()) continue
                val posterPath = item.optNullableString("poster_path") ?: ""
                val releaseDate = if (isMovie) item.optString("release_date", "") else item.optString("first_air_date", "")
                
                val (airingAt, dayOfWeek) = parseDateToAiringAtAndDayOfWeek(releaseDate)
                val coverUrl = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w500$posterPath" else null
                val rating = item.optDouble("vote_average", 0.0)
                val score = if (rating > 0.0) (rating * 10).toInt().coerceIn(0, 100) else null

                val entry = AiringEntry(
                    aniListId = tmdbId,
                    malId = null,
                    title = title,
                    titleEnglish = title,
                    titleNative = null,
                    coverUrl = coverUrl,
                    episode = if (isMovie) 0 else 1,
                    airingAt = airingAt,
                    dayOfWeek = dayOfWeek,
                    averageScore = score
                )

                if (scheduleMap != null) {
                    val existingList = scheduleMap[dayOfWeek] ?: continue
                    if (existingList.none { it.aniListId == tmdbId }) {
                        existingList.add(entry)
                    }
                }
                if (outList != null) {
                    if (outList.none { it.aniListId == tmdbId }) {
                        outList.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiringCalendarClient", "parseTmdbList error: ${e.message}", e)
        }
    }

    private fun parseDateToAiringAtAndDayOfWeek(dateStr: String, shiftToCurrentWeek: Boolean = false): Pair<Long, Int> {
        if (dateStr.isBlank()) {
            val now = System.currentTimeMillis()
            val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return Pair(now / 1000L, day)
        }
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val day = parts[2].toInt()
                
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day, 20, 0, 0)
                }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                
                if (shiftToCurrentWeek) {
                    val thisWeekCal = Calendar.getInstance()
                    val today = thisWeekCal.get(Calendar.DAY_OF_WEEK)
                    val diff = dayOfWeek - today
                    thisWeekCal.add(Calendar.DAY_OF_YEAR, diff)
                    thisWeekCal.set(Calendar.HOUR_OF_DAY, 20)
                    thisWeekCal.set(Calendar.MINUTE, 0)
                    thisWeekCal.set(Calendar.SECOND, 0)
                    thisWeekCal.set(Calendar.MILLISECOND, 0)
                    Pair(thisWeekCal.timeInMillis / 1000L, dayOfWeek)
                } else {
                    Pair(cal.timeInMillis / 1000L, dayOfWeek)
                }
            } else {
                val now = System.currentTimeMillis()
                val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                Pair(now / 1000L, day)
            }
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            Pair(now / 1000L, day)
        }
    }

    suspend fun fetchUpcomingSchedule(limit: Int = 30, accessToken: String? = null, preferredSource: String? = null): List<AiringEntry> {
        return withContext(Dispatchers.IO) {
            if (preferredSource == "tmdb") {
                fetchTmdbUpcomingSchedule(limit)
            } else {
                val nowSeconds = System.currentTimeMillis() / 1000L
                val fourteenDaysLater = nowSeconds + 14 * 24 * 3600L
                val variables = JSONObject()
                    .put("page", 1)
                    .put("perPage", 50)
                    .put("airingAt_greater", nowSeconds)
                    .put("airingAt_lesser", fourteenDaysLater)
                val responseText = KitsugiApiBase.executeAniListQuery(
                    query = QUERY,
                    variables = variables,
                    accessToken = accessToken
                ) ?: return@withContext emptyList<AiringEntry>()
                val (entries, _) = parseResponse(responseText)
                entries
                    .filter { it.airingAt > nowSeconds }
                    .sortedBy { it.airingAt }
                    .take(limit)
            }
        }
    }

    private suspend fun fetchTmdbUpcomingSchedule(limit: Int): List<AiringEntry> {
        val apiKey = TmdbApiClient.getActiveApiKey()
        val language = TmdbApiClient.getActiveLanguage()
        val list = mutableListOf<AiringEntry>()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val tvUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&first_air_date.gte=$today&sort_by=first_air_date.asc&page=1"
        val movieUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&primary_release_date.gte=$today&sort_by=primary_release_date.asc&page=1"

        parseTmdbList(tvUrl, isMovie = false, outList = list)
        parseTmdbList(movieUrl, isMovie = true, outList = list)

        val nowSeconds = System.currentTimeMillis() / 1000L
        return list
            .filter { it.airingAt > nowSeconds }
            .sortedBy { it.airingAt }
            .take(limit)
    }

    private suspend fun fetchAiringSchedule(
        airingAtGreater: Long,
        airingAtLesser: Long,
        accessToken: String?
    ): List<AiringEntry> {
        val allEntries = mutableListOf<AiringEntry>()
        var page = 1
        var hasNextPage = true
        while (hasNextPage && page <= 10) {
            val variables = JSONObject()
                .put("page", page)
                .put("perPage", 50)
                .put("airingAt_greater", airingAtGreater)
                .put("airingAt_lesser", airingAtLesser)
            val responseText = KitsugiApiBase.executeAniListQuery(
                query = QUERY,
                variables = variables,
                accessToken = accessToken
            ) ?: break
            val (entries, nextPage) = parseResponse(responseText)
            allEntries.addAll(entries)
            hasNextPage = nextPage && entries.isNotEmpty()
            page++
        }
        return allEntries
    }

    private fun parseResponse(jsonText: String): Pair<List<AiringEntry>, Boolean> {
        return try {
            val root = JSONObject(jsonText)
            val pageObj = root.optJSONObject("data")?.optJSONObject("Page")
                ?: return Pair(emptyList(), false)
            val hasNextPage = pageObj.optJSONObject("pageInfo")
                ?.optBoolean("hasNextPage", false) ?: false
            val schedules = pageObj.optJSONArray("airingSchedules")
                ?: return Pair(emptyList(), false)

            val entries = mutableListOf<AiringEntry>()
            for (i in 0 until schedules.length()) {
                val item = schedules.optJSONObject(i) ?: continue
                val media = item.optJSONObject("media") ?: continue
                val airingAt = item.optLong("airingAt", 0L)
                if (airingAt <= 0L) continue
                val episode = item.optInt("episode", 0)
                val aniListId = media.optInt("id", 0)
                if (aniListId <= 0) continue
                val malId = if (media.has("idMal") && !media.isNull("idMal"))
                    media.optInt("idMal", 0).takeIf { it > 0 } else null
                val averageScore = if (media.has("averageScore") && !media.isNull("averageScore"))
                    media.optInt("averageScore", 0).takeIf { it > 0 } else null
                val titleObj = media.optJSONObject("title")
                val romaji = titleObj?.optNullableString("romaji")
                val english = titleObj?.optNullableString("english")
                val native = titleObj?.optNullableString("native")
                val title = romaji ?: english ?: native ?: continue
                val coverUrl = media.optJSONObject("coverImage")?.optNullableString("large")
                val dayOfWeek = Calendar.getInstance().apply {
                    timeInMillis = airingAt * 1000L
                }.get(Calendar.DAY_OF_WEEK)
                entries.add(
                    AiringEntry(
                        aniListId = aniListId,
                        malId = malId,
                        title = title,
                        titleEnglish = english,
                        titleNative = native,
                        coverUrl = coverUrl,
                        episode = episode,
                        airingAt = airingAt,
                        dayOfWeek = dayOfWeek,
                        averageScore = averageScore
                    )
                )
            }
            Pair(entries, hasNextPage)
        } catch (e: Exception) {
            android.util.Log.e("AiringCalendarClient", "Parse hatası: ${e.message}", e)
            Pair(emptyList(), false)
        }
    }

    private fun currentWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis / 1000L
        cal.add(Calendar.DAY_OF_MONTH, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val weekEnd = cal.timeInMillis / 1000L
        return Pair(weekStart, weekEnd)
    }

    companion object {
        private val QUERY = """
            query(${'$'}page:Int,${'$'}perPage:Int,${'$'}airingAt_greater:Int,${'$'}airingAt_lesser:Int){
              Page(page:${'$'}page,perPage:${'$'}perPage){
                pageInfo{hasNextPage}
                airingSchedules(airingAt_greater:${'$'}airingAt_greater,airingAt_lesser:${'$'}airingAt_lesser,sort:TIME){
                  airingAt episode
                  media{
                    id idMal
                    title{romaji english native}
                    coverImage{large}
                    averageScore
                  }
                }
              }
            }
        """.trimIndent()
    }
}
