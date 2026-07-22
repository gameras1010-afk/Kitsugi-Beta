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

    suspend fun fetchWeeklySchedule(accessToken: String? = null): Map<Int, List<AiringEntry>> {
        return withContext(Dispatchers.IO) {
            val (weekStart, weekEnd) = currentWeekRange()
            val rawEntries = fetchAiringSchedule(weekStart, weekEnd, accessToken)
            rawEntries
                .filter { it.airingAt in weekStart..weekEnd }
                .groupBy { it.dayOfWeek }
                .mapValues { (_, list) -> list.sortedBy { it.airingAt } }
        }
    }

    suspend fun fetchUpcomingSchedule(limit: Int = 30, accessToken: String? = null): List<AiringEntry> {
        return withContext(Dispatchers.IO) {
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
