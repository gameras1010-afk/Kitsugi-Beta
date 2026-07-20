package com.kitsugi.animelist.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.utils.*

/**
 * TMDB API'sinden kişi (oyuncu/ekip) detayları, medya jenerik/öneri listeleri,
 * yorumlar ve istatistik verilerini çeken istemci.
 *
 * [TmdbApiClient] tarafından delegate olarak kullanılır; doğrudan çağrılmamalıdır.
 */
internal object TmdbCreditsClient {

    private const val TAG = "TmdbCreditsClient"
    private const val IMG_W185 = "https://image.tmdb.org/t/p/w185"
    private const val IMG_W300 = "https://image.tmdb.org/t/p/w300"

    suspend fun fetchCredits(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): Pair<List<KitsugiCharacter>, List<KitsugiStaff>> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId/credits?api_key=$apiKey&language=tr-TR"
        try {
            val responseText = executeGet(url) ?: return@withContext Pair(emptyList(), emptyList())
            val root = JSONObject(responseText)

            val castArray = root.optJSONArray("cast")
            val charList = mutableListOf<KitsugiCharacter>()
            if (castArray != null) {
                for (i in 0 until castArray.length()) {
                    val item = castArray.getJSONObject(i)
                    val id = item.optInt("id")
                    val actorName = item.optString("name", "Bilinmeyen")
                    val characterName = item.optString("character", "Bilinmeyen")
                    val profilePath = item.optNullableString("profile_path")
                    val order = item.optInt("order", 999)
                    val imageUrl = if (!profilePath.isNullOrEmpty()) "$IMG_W185$profilePath" else null
                    val va = KitsugiVoiceActor(
                        id = id, name = actorName, language = "oyuncu",
                        imageUrl = imageUrl, source = "tmdb"
                    )
                    charList.add(
                        KitsugiCharacter(
                            id = id,
                            name = characterName,
                            role = if (order < 5) "main".toTurkishCharacterRole() else "supporting".toTurkishCharacterRole(),
                            imageUrl = imageUrl,
                            voiceActors = listOf(va),
                            source = "tmdb"
                        )
                    )
                }
            }

            val crewArray = root.optJSONArray("crew")
            val staffList = mutableListOf<KitsugiStaff>()
            if (crewArray != null) {
                for (i in 0 until crewArray.length()) {
                    val item = crewArray.getJSONObject(i)
                    val id = item.optInt("id")
                    val name = item.optString("name", "Bilinmeyen")
                    val job = item.optString("job", "Ekip Üyesi")
                    val profilePath = item.optNullableString("profile_path")
                    val imageUrl = if (!profilePath.isNullOrEmpty()) "$IMG_W185$profilePath" else null
                    staffList.add(
                        KitsugiStaff(id = id, name = name, role = job.toTurkishStaffRole(),
                            imageUrl = imageUrl, source = "tmdb")
                    )
                }
            }
            Pair(charList, staffList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching credits: ${e.message}", e)
            Pair(emptyList(), emptyList())
        }
    }

    suspend fun fetchRecommendations(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<KitsugiRelation> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId/recommendations?api_key=$apiKey&language=tr-TR"
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<KitsugiRelation>()
            for (i in 0 until minOf(results.length(), 20)) {
                val item = results.getJSONObject(i)
                val id = item.optInt("id")
                val title = if (isMovie) item.optString("title", "Bilinmeyen") else item.optString("name", "Bilinmeyen")
                val posterPath = item.optNullableString("poster_path") ?: ""
                val imageUrl = if (posterPath.isNotEmpty()) "$IMG_W185$posterPath" else null
                list.add(
                    KitsugiRelation(
                        malId = id, title = title, relationType = "Tavsiye",
                        imageUrl = imageUrl,
                        mediaType = if (isMovie) MediaType.Movie else MediaType.TvShow,
                        source = "simkl"
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB recommendations: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchReviews(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<KitsugiReview> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId/reviews?api_key=$apiKey"
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<KitsugiReview>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val author = item.optString("author", "Kullanıcı")
                val content = item.optString("content", "").cleanApiText()
                val summary = if (content.length > 280) content.take(280) + "..." else content
                val authorDetails = item.optJSONObject("author_details")
                val rating = authorDetails?.optDouble("rating", 0.0) ?: 0.0
                val score = if (rating > 0.0) (rating * 10).toInt() else null
                val avatarPath = authorDetails?.optNullableString("avatar_path") ?: ""
                val avatarUrl = if (avatarPath.isNotEmpty()) {
                    if (avatarPath.startsWith("/http")) avatarPath.substring(1)
                    else "https://image.tmdb.org/t/p/w185$avatarPath"
                } else null
                val rawDate = item.optString("created_at", "")
                val dateText = if (rawDate.isNotEmpty()) {
                    try {
                        val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        val sdfOut = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        sdfOut.format(sdfIn.parse(rawDate)!!)
                    } catch (_: Exception) { rawDate.take(10) }
                } else null
                list.add(
                    KitsugiReview(
                        id = null, username = author, avatarUrl = avatarUrl,
                        score = score, summary = summary, fullText = content,
                        dateText = dateText, helpfulCount = null, ratingAmount = null, userRating = null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB reviews: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchPersonCharacterDetail(
        personId: Int,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): KitsugiCharacterDetail? = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/person/$personId?api_key=$apiKey&language=tr-TR"
        try {
            val responseText = executeGet(url) ?: return@withContext null
            val root = JSONObject(responseText)
            val name = root.optString("name", "Bilinmeyen")
            val biography = root.optString("biography", "").cleanApiText().takeIf { it.isNotBlank() }
            val profilePath = root.optNullableString("profile_path") ?: ""
            val imageUrl = if (profilePath.isNotEmpty()) "$IMG_W300$profilePath" else null
            val birthday = root.optString("birthday", "").takeIf { it.isNotBlank() }
            val placeOfBirth = root.optString("place_of_birth", "").takeIf { it.isNotBlank() }
            val genderInt = root.optInt("gender", 0)
            val gender = when (genderInt) { 1 -> "Dişi"; 2 -> "Erkek"; 3 -> "Non-binary"; else -> null }
            val alternativeNames = mutableListOf<String>()
            val aka = root.optJSONArray("also_known_as")
            if (aka != null) {
                for (i in 0 until aka.length()) {
                    val nameStr = aka.optString(i, "")
                    if (nameStr.isNotBlank()) alternativeNames.add(nameStr)
                }
            }
            val appearances = fetchPersonMediaAppearances(personId, apiKey, executeGet)
            KitsugiCharacterDetail(
                id = personId, name = name, nativeName = null,
                alternativeNames = alternativeNames, imageUrl = imageUrl,
                gender = gender, age = null, birthday = birthday, bloodType = null,
                biography = biography, voiceActors = emptyList(), mediaAppearances = appearances
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB person details: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchPersonMediaAppearances(
        personId: Int,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<KitsugiCharacterMediaAppearance> {
        val url = "https://api.themoviedb.org/3/person/$personId/combined_credits?api_key=$apiKey&language=tr-TR"
        return try {
            val responseText = executeGet(url) ?: return emptyList()
            val root = JSONObject(responseText)
            val castArray = root.optJSONArray("cast") ?: return emptyList()
            val list = mutableListOf<KitsugiCharacterMediaAppearance>()
            for (i in 0 until minOf(castArray.length(), 20)) {
                val item = castArray.getJSONObject(i)
                val id = item.optInt("id")
                val isMovie = item.optString("media_type") == "movie"
                val title = if (isMovie) item.optString("title", "Bilinmeyen") else item.optString("name", "Bilinmeyen")
                val character = item.optString("character", "Bilinmeyen")
                val posterPath = item.optNullableString("poster_path") ?: ""
                val imageUrl = if (posterPath.isNotEmpty()) "$IMG_W185$posterPath" else null
                list.add(
                    KitsugiCharacterMediaAppearance(
                        mediaId = id, title = title, imageUrl = imageUrl,
                        mediaType = if (isMovie) "movie".toTurkishMediaTypeString() else "tv".toTurkishMediaTypeString(),
                        characterRole = character, source = "tmdb"
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB combined credits: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchPersonStaffDetail(
        personId: Int,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): KitsugiStaffDetail? = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/person/$personId?api_key=$apiKey&language=tr-TR"
        try {
            val responseText = executeGet(url) ?: return@withContext null
            val root = JSONObject(responseText)
            val name = root.optString("name", "Bilinmeyen")
            val biography = root.optString("biography", "").cleanApiText().takeIf { it.isNotBlank() }
            val profilePath = root.optNullableString("profile_path") ?: ""
            val imageUrl = if (profilePath.isNotEmpty()) "$IMG_W300$profilePath" else null
            val birthday = root.optString("birthday", "").takeIf { it.isNotBlank() }
            val placeOfBirth = root.optString("place_of_birth", "").takeIf { it.isNotBlank() }
            val genderInt = root.optInt("gender", 0)
            val gender = when (genderInt) { 1 -> "Dişi"; 2 -> "Erkek"; 3 -> "Non-binary"; else -> null }
            val alternativeNames = mutableListOf<String>()
            val aka = root.optJSONArray("also_known_as")
            if (aka != null) {
                for (i in 0 until aka.length()) {
                    val nameStr = aka.optString(i, "")
                    if (nameStr.isNotBlank()) alternativeNames.add(nameStr)
                }
            }
            val department = root.optString("known_for_department", "Ekip Üyesi")
                .takeIf { it.isNotBlank() }?.toTurkishStaffRole()
            val works = fetchPersonMediaWorks(personId, apiKey, executeGet)
            KitsugiStaffDetail(
                id = personId, name = name, nativeName = null,
                alternativeNames = alternativeNames, imageUrl = imageUrl,
                biography = biography, occupation = department, birthday = birthday,
                age = null, gender = gender, homeTown = placeOfBirth,
                characterRoles = emptyList(), mediaWorks = works
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB staff details: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchPersonMediaWorks(
        personId: Int,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<KitsugiStaffMediaWork> {
        val url = "https://api.themoviedb.org/3/person/$personId/combined_credits?api_key=$apiKey&language=tr-TR"
        return try {
            val responseText = executeGet(url) ?: return emptyList()
            val root = JSONObject(responseText)
            val crewArray = root.optJSONArray("crew") ?: return emptyList()
            val list = mutableListOf<KitsugiStaffMediaWork>()
            for (i in 0 until minOf(crewArray.length(), 20)) {
                val item = crewArray.getJSONObject(i)
                val id = item.optInt("id")
                val isMovie = item.optString("media_type") == "movie"
                val title = if (isMovie) item.optString("title", "Bilinmeyen") else item.optString("name", "Bilinmeyen")
                val job = item.optString("job", "Ekip Üyesi")
                val posterPath = item.optNullableString("poster_path") ?: ""
                val imageUrl = if (posterPath.isNotEmpty()) "$IMG_W185$posterPath" else null
                list.add(
                    KitsugiStaffMediaWork(
                        mediaId = id, mediaTitle = title, mediaImageUrl = imageUrl,
                        mediaType = if (isMovie) "movie".toTurkishMediaTypeString() else "tv".toTurkishMediaTypeString(),
                        staffRole = job.toTurkishStaffRole(), source = "tmdb"
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB media works: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchStats(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): KitsugiStats? = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId?api_key=$apiKey&language=tr-TR"
        try {
            val responseText = executeGet(url) ?: return@withContext null
            val root = JSONObject(responseText)
            val voteCount = root.optInt("vote_count", 0)
            val voteAverage = root.optDouble("vote_average", 0.0)
            val popularity = root.optDouble("popularity", 0.0)
            if (voteCount <= 0) return@withContext null

            val avgScore = voteAverage.coerceIn(1.0, 10.0)
            val scoreList = mutableListOf<KitsugiScoreStat>()
            for (score in 1..10) {
                val dist = score - avgScore
                val sigma = 1.8
                val weight = Math.exp(-(dist * dist) / (2 * sigma * sigma))
                val amount = (voteCount * weight * 0.20).toInt().coerceAtLeast(if (score == avgScore.toInt()) voteCount / 5 else 0)
                if (amount > 0) scoreList.add(KitsugiScoreStat(score, amount))
            }

            KitsugiStats(
                watching = (popularity * 10).toInt().coerceAtLeast(0),
                completed = voteCount,
                planned = null,
                dropped = null,
                scoreDistribution = scoreList
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchStats TMDB error: ${e.message}", e)
            null
        }
    }
}
