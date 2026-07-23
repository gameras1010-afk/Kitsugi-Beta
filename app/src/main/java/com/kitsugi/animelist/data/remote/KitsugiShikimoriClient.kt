package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import com.kitsugi.animelist.utils.*

object KitsugiShikimoriClient {
    private const val TAG = "KitsugiShikimoriClient"
    private const val BASE_URL = "https://shikimori.one/api"

    suspend fun fetchCharacters(
        mediaType: MediaType,
        externalId: Int
    ): List<KitsugiCharacter> {
        return withContext(Dispatchers.IO) {
            val endpoint = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "animes"
                MediaType.Manga -> "mangas"
            }
            val url = URL("$BASE_URL/$endpoint/$externalId/roles")
            Log.d(TAG, "Shikimori fetchCharacters: $url")
            runCatching {
                KitsugiApiBase.runWithRateLimit {
                    val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                    val array = JSONArray(response)
                    val charactersMap = mutableMapOf<Int, KitsugiCharacter>()

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val charObj = item.optJSONObject("character") ?: continue
                        val charId = charObj.optInt("id")
                        if (charId <= 0) continue

                        val rolesArr = item.optJSONArray("roles")
                        val roleStr = if (rolesArr != null && rolesArr.length() > 0) {
                            rolesArr.optString(0)
                        } else "Supporting"

                        val charName = charObj.optString("name", "Bilinmeyen")
                        val relativeImg = charObj.optJSONObject("image")?.optString("original")
                        val imageUrl = relativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                        val vaList = mutableListOf<KitsugiVoiceActor>()
                        val personObj = item.optJSONObject("person")
                        if (personObj != null) {
                            val vaId = personObj.optInt("id")
                            if (vaId > 0) {
                                val vaName = personObj.optString("name", "Bilinmeyen")
                                val vaRelativeImg = personObj.optJSONObject("image")?.optString("original")
                                val vaImageUrl = vaRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }
                                vaList.add(
                                    KitsugiVoiceActor(
                                        id = vaId,
                                        name = vaName,
                                        language = "Japonca",
                                        imageUrl = vaImageUrl,
                                        source = "shikimori"
                                    )
                                )
                            }
                        }

                        val existing = charactersMap[charId]
                        if (existing != null) {
                            val updatedVas = (existing.voiceActors + vaList).distinctBy { it.id }
                            charactersMap[charId] = existing.copy(voiceActors = updatedVas)
                        } else {
                            charactersMap[charId] = KitsugiCharacter(
                                id = charId,
                                name = charName,
                                role = roleStr.toTurkishCharacterRole(),
                                imageUrl = imageUrl,
                                voiceActors = vaList,
                                source = "shikimori"
                            )
                        }
                    }
                    charactersMap.values.toList()
                }
            }.getOrElse { err ->
                Log.e(TAG, "Shikimori fetchCharacters exception: ${err.message}", err)
                emptyList()
            }
        }
    }

    suspend fun fetchStaff(
        mediaType: MediaType,
        externalId: Int
    ): List<KitsugiStaff> {
        return withContext(Dispatchers.IO) {
            val endpoint = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "animes"
                MediaType.Manga -> "mangas"
            }
            val url = URL("$BASE_URL/$endpoint/$externalId/roles")
            Log.d(TAG, "Shikimori fetchStaff: $url")
            runCatching {
                KitsugiApiBase.runWithRateLimit {
                    val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                    val array = JSONArray(response)
                    val staffList = mutableListOf<KitsugiStaff>()

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val charObj = item.optJSONObject("character")
                        if (charObj == null) {
                            val personObj = item.optJSONObject("person") ?: continue
                            val staffId = personObj.optInt("id")
                            if (staffId <= 0) continue

                            val rolesArr = item.optJSONArray("roles")
                            val roleStr = if (rolesArr != null && rolesArr.length() > 0) {
                                rolesArr.optString(0)
                            } else "Staff"

                            val staffName = personObj.optString("name", "Bilinmeyen")
                            val relativeImg = personObj.optJSONObject("image")?.optString("original")
                            val imageUrl = relativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                            staffList.add(
                                KitsugiStaff(
                                    id = staffId,
                                    name = staffName,
                                    role = roleStr.toTurkishStaffRole(),
                                    imageUrl = imageUrl,
                                    source = "shikimori"
                                )
                            )
                        }
                    }
                    staffList
                }
            }.getOrElse { err ->
                Log.e(TAG, "Shikimori fetchStaff exception: ${err.message}", err)
                emptyList()
            }
        }
    }

    suspend fun fetchCharacterDetail(characterId: Int): KitsugiCharacterDetail? {
        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/characters/$characterId")
            Log.d(TAG, "Shikimori fetchCharacterDetail: $url")
            runCatching {
                KitsugiApiBase.runWithRateLimit {
                    val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit null
                    val data = JSONObject(response)

                    val charName = data.optString("name", "Bilinmeyen")
                    val nativeName = data.optNullableString("japanese")
                    val alternativeNames = mutableListOf<String>()
                    val altname = data.optNullableString("altname")
                    if (!altname.isNullOrBlank()) {
                        alternativeNames.add(altname)
                    }

                    val relativeImg = data.optJSONObject("image")?.optString("original")
                    val imageUrl = relativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }
                    val biography = data.optNullableString("description")?.cleanApiText()

                    val gender = null
                    val age = null
                    val birthday = null
                    val bloodType = null

                    val voiceActors = mutableListOf<KitsugiVoiceActor>()
                    val seyuArray = data.optJSONArray("seyu")
                    if (seyuArray != null) {
                        for (i in 0 until seyuArray.length()) {
                            val seyuItem = seyuArray.optJSONObject(i) ?: continue
                            val seyuId = seyuItem.optInt("id")
                            if (seyuId <= 0) continue

                            val seyuName = seyuItem.optString("name", "Bilinmeyen")
                            val seyuRelativeImg = seyuItem.optJSONObject("image")?.optString("original")
                            val seyuImageUrl = seyuRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                            voiceActors.add(
                                KitsugiVoiceActor(
                                    id = seyuId,
                                    name = seyuName,
                                    language = "Japonca",
                                    imageUrl = seyuImageUrl,
                                    source = "shikimori"
                                )
                            )
                        }
                    }

                    val mediaAppearances = mutableListOf<KitsugiCharacterMediaAppearance>()
                    val animesArray = data.optJSONArray("animes")
                    if (animesArray != null) {
                        for (i in 0 until animesArray.length()) {
                            val animeItem = animesArray.optJSONObject(i) ?: continue
                            val animeId = animeItem.optInt("id")
                            if (animeId <= 0) continue

                            val animeTitle = animeItem.optString("name", "Bilinmeyen")
                            val animeRelativeImg = animeItem.optJSONObject("image")?.optString("original")
                            val animeImageUrl = animeRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }
                            val kind = animeItem.optString("kind", "tv")

                            val rolesArr = animeItem.optJSONArray("roles")
                            val roleStr = if (rolesArr != null && rolesArr.length() > 0) {
                                rolesArr.optString(0)
                            } else "Supporting"

                            mediaAppearances.add(
                                KitsugiCharacterMediaAppearance(
                                    mediaId = animeId,
                                    title = animeTitle,
                                    imageUrl = animeImageUrl,
                                    mediaType = kind.toTurkishMediaTypeString(),
                                    characterRole = roleStr.toTurkishCharacterRole(),
                                    source = "shikimori"
                                )
                            )
                        }
                    }

                    val mangasArray = data.optJSONArray("mangas")
                    if (mangasArray != null) {
                        for (i in 0 until mangasArray.length()) {
                            val mangaItem = mangasArray.optJSONObject(i) ?: continue
                            val mangaId = mangaItem.optInt("id")
                            if (mangaId <= 0) continue

                            val mangaTitle = mangaItem.optString("name", "Bilinmeyen")
                            val mangaRelativeImg = mangaItem.optJSONObject("image")?.optString("original")
                            val mangaImageUrl = mangaRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                            val rolesArr = mangaItem.optJSONArray("roles")
                            val roleStr = if (rolesArr != null && rolesArr.length() > 0) {
                                rolesArr.optString(0)
                            } else "Supporting"

                            mediaAppearances.add(
                                KitsugiCharacterMediaAppearance(
                                    mediaId = mangaId,
                                    title = mangaTitle,
                                    imageUrl = mangaImageUrl,
                                    mediaType = "manga".toTurkishMediaTypeString(),
                                    characterRole = roleStr.toTurkishCharacterRole(),
                                    source = "shikimori"
                                )
                            )
                        }
                    }

                    KitsugiCharacterDetail(
                        id = characterId,
                        name = charName,
                        nativeName = nativeName,
                        alternativeNames = alternativeNames,
                        imageUrl = imageUrl,
                        gender = gender,
                        age = age,
                        birthday = birthday,
                        bloodType = bloodType,
                        biography = biography,
                        voiceActors = voiceActors,
                        mediaAppearances = mediaAppearances,
                        isFavourite = false
                    )
                }
            }.getOrNull()
        }
    }

    suspend fun fetchStaffDetail(staffId: Int): KitsugiStaffDetail? {
        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/people/$staffId")
            Log.d(TAG, "Shikimori fetchStaffDetail: $url")
            runCatching {
                KitsugiApiBase.runWithRateLimit {
                    val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit null
                    val data = JSONObject(response)

                    val staffName = data.optString("name", "Bilinmeyen")
                    val nativeName = data.optNullableString("japanese")
                    val alternativeNames = mutableListOf<String>()
                    val biography = data.optNullableString("biography")?.cleanApiText()

                    val birthday = data.optNullableString("birth_on")
                    val homeTown = data.optNullableString("birth_place")
                    val gender = null
                    val age = null
                    val occupation = data.optNullableString("job_title")

                    val relativeImg = data.optJSONObject("image")?.optString("original")
                    val imageUrl = relativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                    val characterRoles = mutableListOf<KitsugiStaffCharacterRole>()
                    val rolesArray = data.optJSONArray("roles")
                    if (rolesArray != null) {
                        for (i in 0 until rolesArray.length()) {
                            val roleObj = rolesArray.optJSONObject(i) ?: continue
                            val charObj = roleObj.optJSONObject("character") ?: continue
                            val animeObj = roleObj.optJSONObject("anime") ?: roleObj.optJSONObject("manga")

                            val charId = charObj.optInt("id")
                            val charName = charObj.optString("name", "Bilinmeyen") ?: "Bilinmeyen"
                            val charRelativeImg = charObj.optJSONObject("image")?.optString("original")
                            val charImg = charRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }

                            val mediaId = animeObj?.optInt("id") ?: 0
                            val mediaTitle = animeObj?.optString("name", "Bilinmeyen") ?: "Bilinmeyen"
                            val mediaRelativeImg = animeObj?.optJSONObject("image")?.optString("original")
                            val mediaImg = mediaRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }
                            val mediaTypeStr = animeObj?.optString("kind", "tv") ?: "manga"

                            val roleStr = roleObj.optString("role", "Seyu") ?: "Seyu"

                            characterRoles.add(
                                KitsugiStaffCharacterRole(
                                    characterId = charId,
                                    characterName = charName,
                                    characterImageUrl = charImg,
                                    characterSource = "shikimori",
                                    mediaId = mediaId,
                                    mediaTitle = mediaTitle,
                                    mediaImageUrl = mediaImg,
                                    mediaType = mediaTypeStr.toTurkishMediaTypeString(),
                                    characterRole = roleStr.toTurkishCharacterRole(),
                                    mediaSource = "shikimori"
                                )
                            )
                        }
                    }

                    val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
                    val worksArray = data.optJSONArray("works")
                    if (worksArray != null) {
                        for (i in 0 until worksArray.length()) {
                            val workObj = worksArray.optJSONObject(i) ?: continue
                            val animeObj = workObj.optJSONObject("anime") ?: workObj.optJSONObject("manga") ?: continue
                            val mediaId = animeObj.optInt("id")
                            if (mediaId <= 0) continue

                            val mediaTitle = animeObj.optString("name", "Bilinmeyen") ?: "Bilinmeyen"
                            val mediaRelativeImg = animeObj.optJSONObject("image")?.optString("original")
                            val mediaImg = mediaRelativeImg?.let { if (it.startsWith("/")) "https://shikimori.one$it" else it }
                            val mediaTypeStr = animeObj.optString("kind", "tv") ?: "tv"

                            val roleStr = workObj.optString("role", "Staff") ?: "Staff"

                            mediaWorks.add(
                                KitsugiStaffMediaWork(
                                    mediaId = mediaId,
                                    mediaTitle = mediaTitle,
                                    mediaImageUrl = mediaImg,
                                    mediaType = mediaTypeStr.toTurkishMediaTypeString(),
                                    staffRole = roleStr.toTurkishStaffRole(),
                                    source = "shikimori"
                                )
                            )
                        }
                    }

                    KitsugiStaffDetail(
                        id = staffId,
                        name = staffName,
                        nativeName = nativeName,
                        alternativeNames = alternativeNames,
                        imageUrl = imageUrl,
                        biography = biography,
                        occupation = occupation,
                        birthday = birthday,
                        age = age,
                        gender = gender,
                        homeTown = homeTown,
                        characterRoles = characterRoles,
                        mediaWorks = mediaWorks,
                        isFavourite = false
                    )
                }
            }.getOrNull()
        }
    }
}
