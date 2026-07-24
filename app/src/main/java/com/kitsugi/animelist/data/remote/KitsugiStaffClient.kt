package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.KitsugiApplication
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.kitsugi.animelist.utils.*

class KitsugiStaffClient {

    suspend fun fetchStaff(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        tmdbId: Int? = null,
        realMalId: Int? = null
    ): List<KitsugiStaff> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) {
                return@withContext emptyList()
            }

            when (source.lowercase()) {
                "shikimori" -> {
                    return@withContext KitsugiShikimoriClient.fetchStaff(mediaType, externalId)
                }
                "simkl" -> {
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            val malList = fetchStaff("jikan", malId, mediaType, null, null)
                            if (malList.isNotEmpty()) return@withContext malList
                        }
                    }
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }
                    if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val (_, tmdbStaff) = TmdbApiClient().fetchCredits(resolvedTmdb, isMovie)
                        if (tmdbStaff.isNotEmpty()) return@withContext tmdbStaff
                    }
                    emptyList()
                }

                "tmdb" -> {
                    val effectiveTmdbId = tmdbId ?: externalId
                    if (effectiveTmdbId > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val (_, tmdbStaff) = TmdbApiClient().fetchCredits(effectiveTmdbId, isMovie)
                        tmdbStaff
                    } else emptyList()
                }
                "jikan", "mal" -> {
                    val jikanId = realMalId?.takeIf { it > 0 } ?: externalId
                    val endpoint = when (mediaType) {
                        MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
                        MediaType.Manga -> "manga"
                    }
                    val url = URL("https://api.jikan.moe/v4/$endpoint/$jikanId/staff")
                    val jikanList = runCatching {
                        KitsugiApiBase.runWithRateLimit {
                            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                            val root = JSONObject(response)
                            val data = root.optJSONArray("data") ?: return@runWithRateLimit emptyList()
                            val list = mutableListOf<KitsugiStaff>()
                            for (i in 0 until data.length()) {
                                val item = data.optJSONObject(i) ?: continue
                                val personObj = item.optJSONObject("person") ?: continue
                                val id = personObj.optInt("mal_id")
                                val name = (personObj.optNullableString("name") ?: "Bilinmeyen").toFriendlyName()
                                val positions = item.optJSONArray("positions")
                                val role = if (positions != null && positions.length() > 0) {
                                    val posList = mutableListOf<String>()
                                    for (j in 0 until positions.length()) {
                                        val pos = positions.optString(j)
                                        if (pos.isNotBlank() && pos != "null") posList.add(pos.toTurkishStaffRole())
                                    }
                                    posList.joinToString(", ")
                                } else "Ekip Üyesi"
                                val imageUrl = personObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                list.add(KitsugiStaff(id, name, role, imageUrl, source = "jikan"))
                            }
                            list
                        }
                    }.getOrElse { emptyList() }

                    if (jikanList.isNotEmpty()) {
                        jikanList
                    } else {
                        android.util.Log.w("KitsugiStaffClient", "Jikan ekip listesi boş veya başarısız oldu. Shikimori fallback devreye giriyor...")
                        KitsugiShikimoriClient.fetchStaff(mediaType, jikanId)
                    }
                }

                "anilist" -> {
                    val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else null
                    val idParam = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
                    val idFilter = if (aniListId != null) "id: \$id" else "idMal: \$idMal"
                    val query = """
                        query (${'$'}type: MediaType, $idParam) {
                            Media($idFilter, type: ${'$'}type) {
                                staff(page: 1, perPage: 24) {
                                    edges {
                                        role
                                        node {
                                            id
                                            name { userPreferred }
                                            image { medium }
                                        }
                                    }
                                }
                            }
                        }
                    """.trimIndent()
                    val variables = JSONObject().put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
                    if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

                    runCatching {
                        val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching emptyList()
                        val root = JSONObject(response)
                        val edges = root.optJSONObject("data")?.optJSONObject("Media")?.optJSONObject("staff")?.optJSONArray("edges") ?: return@runCatching emptyList()
                        val list = mutableListOf<KitsugiStaff>()
                        for (i in 0 until edges.length()) {
                            val edge = edges.optJSONObject(i) ?: continue
                            val role = (edge.optNullableString("role") ?: "Ekip Üyesi").toTurkishStaffRole()
                            val node = edge.optJSONObject("node") ?: continue
                            val id = node.optInt("id")
                            val nameObj = node.optJSONObject("name")
                            val name = nameObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                            val imageUrl = node.optJSONObject("image")?.optNullableString("medium")
                            list.add(KitsugiStaff(id, name, role, imageUrl, source = "anilist"))
                        }
                        list
                    }.getOrElse { emptyList() }
                }

                else -> emptyList()
            }
        }
    }

    suspend fun fetchStaffDetail(
        source: String,
        staffId: Int,
        name: String? = null
    ): KitsugiStaffDetail? {
        return withContext(Dispatchers.IO) {
            if (staffId <= 0) return@withContext null
            when (source.lowercase()) {
                "shikimori" -> {
                    KitsugiShikimoriClient.fetchStaffDetail(staffId)
                }
                "jikan", "mal" -> {
                    var detail: KitsugiStaffDetail? = null
                    var networkError: Throwable? = null

                    // 1. MAL / Jikan API
                    val url = URL("https://api.jikan.moe/v4/people/$staffId/full")
                    val jikanRes = runCatching {
                        KitsugiApiBase.runWithRateLimit {
                            val response = KitsugiApiBase.executeGetRequestOrThrow(url)
                            val root = JSONObject(response)
                            val data = root.optJSONObject("data") ?: return@runWithRateLimit null

                            val staffName = (data.optNullableString("name") ?: "Bilinmeyen").toFriendlyName()
                            val nativeName = data.optNullableString("given_name") ?: data.optNullableString("family_name")

                            val alternativeNames = mutableListOf<String>()
                            val altArray = data.optJSONArray("alternate_names")
                            if (altArray != null) {
                                for (i in 0 until altArray.length()) {
                                    val alt = altArray.optString(i)
                                    if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                                }
                            }

                            val imageUrl = data.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                            val biography = data.optNullableString("about")?.cleanApiText()?.takeIf { it.isNotBlank() }

                            val birthday = data.optNullableString("birthday")

                            val characterRoles = mutableListOf<KitsugiStaffCharacterRole>()
                            val voicesArray = data.optJSONArray("voices")
                            if (voicesArray != null) {
                                for (i in 0 until voicesArray.length()) {
                                    val item = voicesArray.optJSONObject(i) ?: continue
                                    val role = (item.optNullableString("role") ?: "Bilinmeyen").toTurkishCharacterRole()

                                    val animeObj = item.optJSONObject("anime")
                                    val animeId = animeObj?.optInt("mal_id") ?: 0
                                    val animeTitle = animeObj?.optNullableString("title") ?: "Bilinmeyen"
                                    val animeImg = animeObj?.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")

                                    val charObj = item.optJSONObject("character") ?: continue
                                    val charId = charObj.optInt("mal_id")
                                    val charName = (charObj.optNullableString("name") ?: "Bilinmeyen").toFriendlyName()
                                    val charImg = charObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")

                                    if (charId <= 0 && charName.isBlank()) continue

                                    characterRoles.add(KitsugiStaffCharacterRole(
                                        characterId = charId,
                                        characterName = charName.ifBlank { "Bilinmeyen" },
                                        characterImageUrl = charImg,
                                        characterSource = "jikan",
                                        mediaId = animeId,
                                        mediaTitle = animeTitle,
                                        mediaImageUrl = animeImg,
                                        mediaType = "anime".toTurkishMediaTypeString(),
                                        characterRole = role,
                                        mediaSource = "jikan"
                                    ))
                                }
                            }

                            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
                            val animeWorks = data.optJSONArray("anime")
                            if (animeWorks != null) {
                                for (i in 0 until animeWorks.length()) {
                                    val item = animeWorks.optJSONObject(i) ?: continue
                                    val position = (item.optNullableString("position") ?: "Ekip Üyesi").toTurkishStaffRole()
                                    val animeObj = item.optJSONObject("anime")
                                    val animeId = animeObj?.optInt("mal_id") ?: 0
                                    val animeTitle = animeObj?.optNullableString("title") ?: "Bilinmeyen"
                                    val animeImg = animeObj?.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                    mediaWorks.add(KitsugiStaffMediaWork(
                                        mediaId = animeId,
                                        mediaTitle = animeTitle,
                                        mediaImageUrl = animeImg,
                                        mediaType = "anime".toTurkishMediaTypeString(),
                                        staffRole = position,
                                        source = "jikan"
                                    ))
                                }
                            }

                            KitsugiStaffDetail(
                                id = staffId,
                                name = staffName,
                                nativeName = nativeName,
                                alternativeNames = alternativeNames,
                                imageUrl = imageUrl,
                                biography = biography,
                                occupation = null,
                                birthday = birthday,
                                age = null,
                                gender = null,
                                homeTown = null,
                                characterRoles = characterRoles,
                                mediaWorks = mediaWorks
                            )
                        }
                    }
                    jikanRes.onSuccess {
                        detail = it
                    }.onFailure { err ->
                        if (err !is ResourceNotFoundException) {
                            networkError = err
                        }
                        android.util.Log.e("KitsugiStaffClient", "Jikan fetch failed: ${err.message}", err)
                    }

                    // 2. Shikimori Fallback / Merge
                    runCatching {
                        KitsugiShikimoriClient.fetchStaffDetail(staffId)
                    }.onSuccess { shikiDetail ->
                        if (shikiDetail != null) {
                            detail = if (detail == null) shikiDetail else detail!!.mergeWith(shikiDetail)
                        }
                    }.onFailure { err ->
                        android.util.Log.e("KitsugiStaffClient", "Shikimori staff detail fallback failed: ${err.message}", err)
                    }

                    // 3. AniList Fallback / Merge
                    val targetName = detail?.name ?: name
                    if (!targetName.isNullOrBlank()) {
                        runCatching {
                            fetchAniListStaffByName(targetName)
                        }.onSuccess { aniListDetail ->
                            if (aniListDetail != null) {
                                detail = if (detail == null) aniListDetail else detail!!.mergeWith(aniListDetail)
                            }
                        }.onFailure { err ->
                            android.util.Log.e("KitsugiStaffClient", "AniList staff detail fallback failed: ${err.message}", err)
                        }
                    }

                    if (detail == null && networkError != null) {
                        throw networkError!!
                    }
                    detail
                }

                "anilist" -> {
                    val query = """
                        query (${'$'}id: Int) {
                            Staff(id: ${'$'}id) {
                                id
                                isFavourite
                                name {
                                    userPreferred
                                    native
                                    alternative
                                }
                                image { large }
                                description
                                primaryOccupations
                                gender
                                dateOfBirth { year month day }
                                age
                                homeTown
                                characterMedia(page: 1, perPage: 24) {
                                    edges {
                                        characterRole
                                        node {
                                            id
                                            idMal
                                            title { userPreferred english romaji native }
                                            coverImage { large }
                                            type
                                        }
                                        characters {
                                            id
                                            name { userPreferred }
                                            image { medium }
                                        }
                                    }
                                }
                                staffMedia(page: 1, perPage: 24) {
                                    edges {
                                        staffRole
                                        node {
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
                    val variables = JSONObject().put("id", staffId)
                    runCatching {
                        val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                        val root = JSONObject(response)
                        val data = root.optJSONObject("data")?.optJSONObject("Staff") ?: return@runCatching null

                        val nameObj = data.optJSONObject("name")
                        val name = nameObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                        val nativeName = nameObj?.optNullableString("native")

                        val alternativeNames = mutableListOf<String>()
                        val altArray = nameObj?.optJSONArray("alternative")
                        if (altArray != null) {
                            for (i in 0 until altArray.length()) {
                                val alt = altArray.optString(i)
                                if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                            }
                        }

                        val imageUrl = data.optJSONObject("image")?.optNullableString("large")
                        val biography = data.optNullableString("description")?.cleanApiText()?.takeIf { it.isNotBlank() }

                        val gender = data.optNullableString("gender")?.toTurkishGender()
                        val age = data.optNullableString("age")
                        val homeTown = data.optNullableString("homeTown")

                        val occupArray = data.optJSONArray("primaryOccupations")
                        val occupation = if (occupArray != null && occupArray.length() > 0) {
                            val list = mutableListOf<String>()
                            for (i in 0 until occupArray.length()) {
                                val occ = occupArray.optString(i)
                                if (occ.isNotBlank() && occ != "null") list.add(occ.toTurkishStaffRole())
                            }
                            list.joinToString(", ")
                        } else null

                        val dobObj = data.optJSONObject("dateOfBirth")
                        val birthday = if (dobObj != null) {
                            val d = dobObj.optInt("day", 0)
                            val m = dobObj.optInt("month", 0)
                            val y = dobObj.optInt("year", 0)
                            if (d > 0 && m > 0) {
                                if (y > 0) "$d/$m/$y" else "$d/$m"
                            } else null
                        } else null

                        val characterRoles = mutableListOf<KitsugiStaffCharacterRole>()
                        val charMediaEdges = data.optJSONObject("characterMedia")?.optJSONArray("edges")
                        if (charMediaEdges != null) {
                            for (i in 0 until charMediaEdges.length()) {
                                val edge = charMediaEdges.optJSONObject(i) ?: continue
                                val charRole = (edge.optNullableString("characterRole") ?: "Bilinmeyen").toTurkishCharacterRole()
                                val mediaNode = edge.optJSONObject("node") ?: continue
                                val mediaId = mediaNode.optInt("idMal").takeIf { it > 0 } ?: (100_000_000 + mediaNode.optInt("id"))
                                val mediaTitleObj = mediaNode.optJSONObject("title")
                                val mediaTitle = mediaTitleObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                                val mediaTitleEnglish = mediaTitleObj?.optNullableString("english")
                                val mediaTitleNative = mediaTitleObj?.optNullableString("native")
                                val mediaTitleRomaji = mediaTitleObj?.optNullableString("romaji")
                                val mediaImg = mediaNode.optJSONObject("coverImage")?.optNullableString("large")
                                val mediaType = mediaNode.optNullableString("type").orEmpty().toTurkishMediaTypeString()

                                val chars = edge.optJSONArray("characters")
                                if (chars != null && chars.length() > 0) {
                                    val charObj = chars.optJSONObject(0) ?: continue
                                    val charId = charObj.optInt("id")
                                    val charName = charObj.optJSONObject("name")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                                    val charImg = charObj.optJSONObject("image")?.optNullableString("medium")

                                    characterRoles.add(KitsugiStaffCharacterRole(
                                        characterId = charId,
                                        characterName = charName,
                                        characterImageUrl = charImg,
                                        characterSource = "anilist",
                                        mediaId = mediaId,
                                        mediaTitle = mediaTitle,
                                        mediaImageUrl = mediaImg,
                                        mediaType = mediaType,
                                        characterRole = charRole,
                                        mediaSource = "anilist"
                                    ))
                                }
                            }
                        }

                        val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
                        val staffMediaEdges = data.optJSONObject("staffMedia")?.optJSONArray("edges")
                        if (staffMediaEdges != null) {
                            for (i in 0 until staffMediaEdges.length()) {
                                val edge = staffMediaEdges.optJSONObject(i) ?: continue
                                val staffRole = (edge.optNullableString("staffRole") ?: "Ekip Üyesi").toTurkishStaffRole()
                                val mediaNode = edge.optJSONObject("node") ?: continue
                                val mediaId = mediaNode.optInt("idMal").takeIf { it > 0 } ?: (100_000_000 + mediaNode.optInt("id"))
                                val staffTitleObj = mediaNode.optJSONObject("title")
                                val mediaTitle = staffTitleObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                                val mediaTitleEnglish = staffTitleObj?.optNullableString("english")
                                val mediaTitleNative = staffTitleObj?.optNullableString("native")
                                val mediaTitleRomaji = staffTitleObj?.optNullableString("romaji")
                                val mediaImg = mediaNode.optJSONObject("coverImage")?.optNullableString("large")
                                val mediaType = mediaNode.optNullableString("type").orEmpty().toTurkishMediaTypeString()

                                mediaWorks.add(KitsugiStaffMediaWork(
                                    mediaId = mediaId,
                                    mediaTitle = mediaTitle,
                                    mediaImageUrl = mediaImg,
                                    mediaType = mediaType,
                                    staffRole = staffRole,
                                    source = "anilist",
                                    titleEnglish = mediaTitleEnglish,
                                    titleJapanese = mediaTitleNative,
                                    titleRomaji = mediaTitleRomaji
                                ))
                            }
                        }

                        val isFavourite = data.optBoolean("isFavourite", false)
                        KitsugiStaffDetail(
                            id = staffId,
                            name = name,
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
                            isFavourite = isFavourite,
                            aniListId = staffId
                        )
                    }.getOrNull()
                }
                "tmdb" -> {
                    val tmdbRes = TmdbApiClient().fetchPersonStaffDetail(staffId)
                    if (tmdbRes != null && KitsugiApplication.getInstance()?.let { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(it) } != null) {
                        val aniListDetail = fetchAniListStaffByName(tmdbRes.name)
                        if (aniListDetail != null) {
                            tmdbRes.copy(isFavourite = aniListDetail.isFavourite, aniListId = aniListDetail.id)
                        } else {
                            tmdbRes
                        }
                    } else {
                        tmdbRes
                    }
                }
                else -> null
            }
        }
    }

    private suspend fun fetchAniListStaffByName(name: String): KitsugiStaffDetail? {
        val query = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 1) {
                    staff(search: ${'$'}search) {
                        id
                        isFavourite
                        name {
                            userPreferred
                            native
                            alternative
                        }
                        image { large }
                        description
                        primaryOccupations
                        gender
                        dateOfBirth { year month day }
                        age
                        homeTown
                        characterMedia(page: 1, perPage: 24) {
                            edges {
                                characterRole
                                node {
                                    id
                                    idMal
                                    title { userPreferred english romaji native }
                                    coverImage { large }
                                    type
                                }
                                characters {
                                    id
                                    name { userPreferred }
                                    image { medium }
                                }
                            }
                        }
                        staffMedia(page: 1, perPage: 24) {
                            edges {
                                staffRole
                                node {
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
            }
        """.trimIndent()
        val variables = JSONObject().put("search", name)
        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
            val root = JSONObject(response)
            val staffArr = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("staff") ?: return@runCatching null
            if (staffArr.length() == 0) return@runCatching null
            val data = staffArr.getJSONObject(0)

            val nameObj = data.optJSONObject("name")
            val staffName = nameObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
            val nativeName = nameObj?.optNullableString("native")

            val alternativeNames = mutableListOf<String>()
            val altArray = nameObj?.optJSONArray("alternative")
            if (altArray != null) {
                for (i in 0 until altArray.length()) {
                    val alt = altArray.optString(i)
                    if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                }
            }

            val imageUrl = data.optJSONObject("image")?.optNullableString("large")
            val biography = data.optNullableString("description")?.cleanApiText()

            val primaryOccupations = mutableListOf<String>()
            data.optJSONArray("primaryOccupations")?.let { arr ->
                for (i in 0 until arr.length()) primaryOccupations.add(arr.getString(i))
            }
            val occupation = primaryOccupations.joinToString(", ").takeIf { it.isNotBlank() }

            val gender = data.optNullableString("gender")?.toTurkishGender()
            val birthday = buildString {
                val dob = data.optJSONObject("dateOfBirth")
                if (dob != null) {
                    val d = dob.optionalPositiveInt("day")
                    val m = dob.optionalPositiveInt("month")
                    val y = dob.optionalPositiveInt("year")
                    if (d != null && m != null) {
                        append("$d.$m")
                        if (y != null) append(".$y")
                    } else if (y != null) {
                        append(y)
                    }
                }
            }.takeIf { it.isNotBlank() }

            val age = data.optNullableString("age")
            val homeTown = data.optNullableString("homeTown")

            val characterRoles = mutableListOf<KitsugiStaffCharacterRole>()
            val charMedia = data.optJSONObject("characterMedia")
            val charEdges = charMedia?.optJSONArray("edges")
            if (charEdges != null) {
                for (i in 0 until charEdges.length()) {
                    val edge = charEdges.optJSONObject(i) ?: continue
                    val node = edge.optJSONObject("node") ?: continue
                    val charRole = (edge.optNullableString("characterRole") ?: "SUPPORTING").toTurkishCharacterRole()
                    val mediaId = node.optInt("id")
                    val idMal = node.optionalPositiveInt("idMal")
                    val mediaTitle = node.optJSONObject("title")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                    val mediaImg = node.optJSONObject("coverImage")?.optNullableString("large")
                    val type = node.optNullableString("type").orEmpty().lowercase()

                    val stableMediaId = idMal ?: (100_000_000 + mediaId)

                    val charArr = edge.optJSONArray("characters")
                    if (charArr != null && charArr.length() > 0) {
                        val charObj = charArr.getJSONObject(0)
                        val charId = charObj.optInt("id")
                        val charName = charObj.optJSONObject("name")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                        val charImg = charObj.optJSONObject("image")?.optNullableString("medium")

                        characterRoles.add(
                            KitsugiStaffCharacterRole(
                                characterId = charId,
                                characterName = charName,
                                characterImageUrl = charImg,
                                characterSource = "anilist",
                                mediaId = stableMediaId,
                                mediaTitle = mediaTitle,
                                mediaImageUrl = mediaImg,
                                mediaType = type.toTurkishMediaTypeString(),
                                characterRole = charRole
                            )
                        )
                    }
                }
            }

            val mediaWorks = mutableListOf<KitsugiStaffMediaWork>()
            val staffMedia = data.optJSONObject("staffMedia")
            val staffEdges = staffMedia?.optJSONArray("edges")
            if (staffEdges != null) {
                for (i in 0 until staffEdges.length()) {
                    val edge = staffEdges.optJSONObject(i) ?: continue
                    val node = edge.optJSONObject("node") ?: continue
                    val staffRole = (edge.optNullableString("staffRole") ?: "Staff").toTurkishStaffRole()
                    val mediaId = node.optInt("id")
                    val idMal = node.optionalPositiveInt("idMal")
                    val staffTitleObj = node.optJSONObject("title")
                    val mediaTitle = staffTitleObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                    val mediaTitleEnglish = staffTitleObj?.optNullableString("english")
                    val mediaTitleNative = staffTitleObj?.optNullableString("native")
                    val mediaTitleRomaji = staffTitleObj?.optNullableString("romaji")
                    val mediaImg = node.optJSONObject("coverImage")?.optNullableString("large")
                    val type = node.optNullableString("type").orEmpty().lowercase()

                    val stableMediaId = idMal ?: (100_000_000 + mediaId)

                    mediaWorks.add(
                        KitsugiStaffMediaWork(
                            mediaId = stableMediaId,
                            mediaTitle = mediaTitle,
                            mediaImageUrl = mediaImg,
                            mediaType = type.toTurkishMediaTypeString(),
                            staffRole = staffRole,
                            source = if (idMal != null) "jikan" else "anilist",
                            titleEnglish = mediaTitleEnglish,
                            titleJapanese = mediaTitleNative,
                            titleRomaji = mediaTitleRomaji
                        )
                    )
                }
            }

            val isFavourite = data.optBoolean("isFavourite", false)
            KitsugiStaffDetail(
                id = data.optInt("id"),
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
                isFavourite = isFavourite,
                aniListId = data.optInt("id")
            )
        }.getOrNull()
    }
}
