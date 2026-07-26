package com.kitsugi.animelist.ui.app

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// KitsugiProfileViewModel extension — istatistik yardımcı fonksiyonları
// ─────────────────────────────────────────────────────────────────────────────
    fun parseDetailedOverviewStats(json: JSONObject?, isAnime: Boolean): DetailedUserOverviewStats {
        if (json == null) return DetailedUserOverviewStats()
        val count = json.optInt("count", 0)
        val epOrChap = if (isAnime) json.optInt("episodesWatched", 0) else json.optInt("chaptersRead", 0)
        val minutesOrVol = if (isAnime) json.optInt("minutesWatched", 0) else json.optInt("volumesRead", 0)
        val daysWatched = if (isAnime) (minutesOrVol / 60.0 / 24.0) else minutesOrVol.toDouble()
        val meanScore = json.optDouble("meanScore", 0.0)
        val stdDev = json.optDouble("standardDeviation", 0.0)

        val scoreList = mutableListOf<ScoreStatItem>()
        json.optJSONArray("scores")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                scoreList.add(
                    ScoreStatItem(
                        score = item.optInt("score", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val lengthList = mutableListOf<LengthStatItem>()
        json.optJSONArray("lengths")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                lengthList.add(
                    LengthStatItem(
                        length = item.optString("length", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val formatList = mutableListOf<FormatStatItem>()
        json.optJSONArray("formats")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                formatList.add(
                    FormatStatItem(
                        format = item.optString("format", "DiÄŸer"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val statusList = mutableListOf<StatusStatItem>()
        json.optJSONArray("statuses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                statusList.add(
                    StatusStatItem(
                        status = item.optString("status", "Bilinmeyen"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val countryList = mutableListOf<CountryStatItem>()
        json.optJSONArray("countries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                countryList.add(
                    CountryStatItem(
                        country = item.optString("country", "DiÄŸer"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val releaseYearList = mutableListOf<ReleaseYearStatItem>()
        json.optJSONArray("releaseYears")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                releaseYearList.add(
                    ReleaseYearStatItem(
                        releaseYear = item.optInt("releaseYear", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val startYearList = mutableListOf<StartYearStatItem>()
        json.optJSONArray("startYears")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                startYearList.add(
                    StartYearStatItem(
                        startYear = item.optInt("startYear", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val genreList = mutableListOf<RankedStatItem>()
        json.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                genreList.add(
                    RankedStatItem(
                        name = item.optString("genre", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        val tagList = mutableListOf<RankedStatItem>()
        json.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val tagObj = item.optJSONObject("tag")
                tagList.add(
                    RankedStatItem(
                        name = tagObj?.optString("name") ?: item.optString("tag", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        val staffList = mutableListOf<RankedStatItem>()
        json.optJSONArray("staff")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val staffObj = item.optJSONObject("staff")
                staffList.add(
                    RankedStatItem(
                        id = staffObj?.optInt("id"),
                        name = staffObj?.optJSONObject("name")?.optString("full") ?: "Bilinmeyen Ekip",
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null,
                        imageUrl = staffObj?.optJSONObject("image")?.optString("large")
                    )
                )
            }
        }

        val voiceActorList = mutableListOf<RankedStatItem>()
        json.optJSONArray("voiceActors")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val vaObj = item.optJSONObject("voiceActor")
                voiceActorList.add(
                    RankedStatItem(
                        id = vaObj?.optInt("id"),
                        name = vaObj?.optJSONObject("name")?.optString("full") ?: "Bilinmeyen Seslendirici",
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null,
                        imageUrl = vaObj?.optJSONObject("image")?.optString("large")
                    )
                )
            }
        }

        val studioList = mutableListOf<RankedStatItem>()
        json.optJSONArray("studios")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val studioObj = item.optJSONObject("studio")
                studioList.add(
                    RankedStatItem(
                        id = studioObj?.optInt("id"),
                        name = studioObj?.optString("name") ?: item.optString("studio", "Bilinmeyen StÃ¼dyo"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        var plannedDays = 0.0
        statusList.find { it.status == "PLANNING" || it.status == "PlanlandÄ±" }?.let {
            plannedDays = if (isAnime) (it.count * 12.0 * 24.0 / 60.0 / 24.0) else it.count.toDouble()
        }

        return DetailedUserOverviewStats(
            count = count,
            episodesWatched = epOrChap,
            daysWatched = daysWatched,
            plannedDaysOrCount = plannedDays,
            meanScore = meanScore,
            standardDeviation = stdDev,
            scoreList = scoreList,
            lengthList = lengthList,
            formatList = formatList,
            statusList = statusList,
            countryList = countryList,
            releaseYearList = releaseYearList,
            startYearList = startYearList,
            genreList = genreList,
            tagList = tagList,
            staffList = staffList,
            voiceActorList = voiceActorList,
            studioList = studioList
        )
    }

    fun computeOverviewStatsFromEntries(entries: List<MediaEntry>, isAnime: Boolean): DetailedUserOverviewStats {
        if (entries.isEmpty()) return DetailedUserOverviewStats()
        val count = entries.size
        val totalProgress = entries.sumOf { it.progress }
        val daysWatched = if (isAnime) (totalProgress * 24.0 / 60.0 / 24.0) else 0.0
        val scores = entries.mapNotNull { it.score }
        val meanScore = if (scores.isNotEmpty()) scores.average() else 0.0
        val variance = if (scores.size > 1) scores.sumOf { Math.pow(it - meanScore, 2.0) } / scores.size else 0.0
        val stdDev = Math.sqrt(variance)

        // Scores 1..10
        val scoreCounts = (1..10).map { s ->
            val matching = entries.filter { (it.score ?: 0) == s }
            ScoreStatItem(score = s, count = matching.size, meanScore = s.toDouble())
        }

        // Statuses
        val statusGroup = entries.groupBy { it.status }.map { (st, list) ->
            val label = when (st) {
                WatchStatus.Watching -> "CURRENT"
                WatchStatus.Completed -> "COMPLETED"
                WatchStatus.Planned -> "PLANNING"
                WatchStatus.Paused -> "PAUSED"
                WatchStatus.Dropped -> "DROPPED"
                else -> "PLANNING"
            }
            StatusStatItem(status = label, count = list.size)
        }

        // Formats
        val formatGroup = entries.groupBy { it.type }.map { (tp, list) ->
            val label = when (tp) {
                MediaType.Anime -> "TV"
                MediaType.Movie -> "MOVIE"
                MediaType.TvShow -> "TV"
                MediaType.Manga -> "MANGA"
            }
            FormatStatItem(format = label, count = list.size)
        }

        val plannedCount = entries.count { it.status == WatchStatus.Planned }.toDouble()

        val tagList = entries.flatMap { entry ->
            val tagString = entry.tags
            if (!tagString.isNullOrBlank()) {
                tagString.split(",").map { t -> t.trim() to entry }
            } else {
                emptyList()
            }
        }.filter { it.first.isNotBlank() }
            .groupBy { it.first }
            .map { (tag, pairs) ->
                val itemEntries = pairs.map { it.second }
                val sc = itemEntries.mapNotNull { it.score }
                val avg = if (sc.isNotEmpty()) sc.average() else 0.0
                val mins = itemEntries.sumOf { e -> e.progress * 24 }
                val chaps = itemEntries.sumOf { e -> e.progress }
                RankedStatItem(
                    name = tag,
                    count = itemEntries.size,
                    meanScore = avg,
                    timeSpentMinutes = if (isAnime) mins else null,
                    chaptersRead = if (!isAnime) chaps else null
                )
            }.sortedByDescending { it.count }

        val genreList = emptyList<RankedStatItem>()
        val studioList = emptyList<RankedStatItem>()

        return DetailedUserOverviewStats(
            count = count,
            episodesWatched = totalProgress,
            daysWatched = daysWatched,
            plannedDaysOrCount = plannedCount,
            meanScore = meanScore,
            standardDeviation = stdDev,
            scoreList = scoreCounts,
            statusList = statusGroup,
            formatList = formatGroup,
            genreList = genreList,
            tagList = tagList,
            studioList = studioList
        )
    }

    // --- MYANIMELIST API FETCHING (via Official API & Jikan Fallback) ---
