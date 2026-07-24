package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.KitsugiApplication
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.kitsugi.animelist.utils.*

class KitsugiCharacterClient {

    companion object {
        private const val TAG = "KitsugiCharacterClient"
    }

    /**
     * [realMalId] — AniList kaynaklı entrylerde ARM veya detailState'ten gelen gerçek MAL ID.
     * Bu değer varsa Jikan fetch'i için [externalId] yerine kullanılır.
     */
    suspend fun fetchCharacters(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        realMalId: Int? = null,
        tmdbId: Int? = null
    ): List<KitsugiCharacter> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) {
                Log.w(TAG, "fetchCharacters: externalId geçersiz ($externalId), source=$source")
                return@withContext emptyList()
            }

            val srcLower = source.lowercase()
            Log.d(TAG, "fetchCharacters başladı: source=$source, externalId=$externalId, realMalId=$realMalId, mediaType=$mediaType, tmdbId=$tmdbId")

            when (srcLower) {
                "shikimori" -> {
                    return@withContext KitsugiShikimoriClient.fetchCharacters(mediaType, externalId)
                }
                "simkl" -> {
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            val malList = fetchCharacters("jikan", malId, mediaType, null, null)
                            if (malList.isNotEmpty()) return@withContext malList
                        }
                    }
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }
                    if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val (tmdbChars, _) = TmdbApiClient().fetchCredits(resolvedTmdb, isMovie)
                        if (tmdbChars.isNotEmpty()) return@withContext tmdbChars
                    }
                    emptyList()
                }

                "tmdb" -> {
                    val effectiveTmdbId = tmdbId ?: externalId
                    if (effectiveTmdbId > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val (tmdbChars, _) = TmdbApiClient().fetchCredits(effectiveTmdbId, isMovie)
                        tmdbChars
                    } else emptyList()
                }
                "jikan", "mal" -> {
                    val jikanId = realMalId?.takeIf { it > 0 } ?: externalId
                    val endpoint = when (mediaType) {
                        MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
                        MediaType.Manga -> "manga"
                    }
                    val url = URL("https://api.jikan.moe/v4/$endpoint/$jikanId/characters")
                    Log.d(TAG, "Jikan isteği: $url")
                    val jikanList = runCatching {
                        KitsugiApiBase.runWithRateLimit {
                            val response = KitsugiApiBase.executeGetRequest(url)
                            if (response == null) {
                                Log.w(TAG, "Jikan yanıt null: $url")
                                return@runWithRateLimit emptyList()
                            }
                            if (!response.trimStart().startsWith('{')) {
                                Log.e(TAG, "Jikan HTML/CF yanıtı (ilk 200 char): ${response.take(200)}")
                                return@runWithRateLimit emptyList()
                            }
                            val root = JSONObject(response)
                            val data = root.optJSONArray("data")
                            if (data == null) {
                                Log.w(TAG, "Jikan 'data' alanı yok. Root keys: ${root.keys().asSequence().toList()}")
                                return@runWithRateLimit emptyList()
                            }
                            Log.d(TAG, "Jikan karakter sayısı: ${data.length()}")
                            val list = mutableListOf<KitsugiCharacter>()
                            for (i in 0 until data.length()) {
                                val item = data.optJSONObject(i) ?: continue
                                val charObj = item.optJSONObject("character") ?: continue
                                val id = charObj.optInt("mal_id")
                                val name = charObj.optNullableString("name") ?: "Bilinmeyen"
                                val role = (item.optNullableString("role") ?: "Bilinmeyen").toTurkishCharacterRole()
                                val imageUrl = charObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")

                                val vaList = mutableListOf<KitsugiVoiceActor>()
                                val vaArray = item.optJSONArray("voice_actors")
                                if (vaArray != null) {
                                    for (j in 0 until vaArray.length()) {
                                        val vaItem = vaArray.optJSONObject(j) ?: continue
                                        val vaPerson = vaItem.optJSONObject("person") ?: continue
                                        val vaId = vaPerson.optInt("mal_id")
                                        val vaName = vaPerson.optNullableString("name") ?: "Bilinmeyen"
                                        val vaLang = (vaItem.optNullableString("language") ?: "Bilinmeyen").toTurkishLanguage()
                                        val vaImageUrl = vaPerson.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                        vaList.add(KitsugiVoiceActor(vaId, vaName, vaLang, vaImageUrl, source = "jikan"))
                                    }
                                }
                                list.add(KitsugiCharacter(id, name, role, imageUrl, vaList, source = "jikan"))
                            }
                            list
                        }
                    }.getOrElse { err ->
                        Log.e(TAG, "Jikan fetch exception: ${err.javaClass.simpleName}: ${err.message}", err)
                        emptyList()
                    }

                    if (jikanList.isNotEmpty()) {
                        jikanList
                    } else {
                        Log.w(TAG, "Jikan karakter listesi boş veya başarısız oldu. Shikimori fallback devreye giriyor...")
                        KitsugiShikimoriClient.fetchCharacters(mediaType, jikanId)
                    }
                }

                "anilist" -> {
                    // externalId >= 100_000_000 ise AniList internal ID encode edilmiş demek
                    val aniListId = when {
                        externalId >= 100_000_000 -> externalId - 100_000_000
                        else -> null  // idMal kullan
                    }
                    Log.d(TAG, "AniList fetch: externalId=$externalId, aniListId=$aniListId")
                    val idParam = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
                    val idFilter = if (aniListId != null) "id: \$id" else "idMal: \$idMal"
                    val query = """
                        query (${'$'}type: MediaType, $idParam) {
                            Media($idFilter, type: ${'$'}type) {
                                characters(page: 1, perPage: 24, sort: [RELEVANCE, ROLE, FAVOURITES_DESC]) {
                                    edges {
                                        role
                                        node {
                                            id
                                            name { userPreferred }
                                            image { medium }
                                        }
                                        voiceActors(sort: [RELEVANCE, LANGUAGE]) {
                                            id
                                            name { userPreferred }
                                            image { medium }
                                            languageV2
                                        }
                                    }
                                }
                            }
                        }
                    """.trimIndent()
                    val variables = JSONObject().put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
                    if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

                    runCatching {
                        val response = KitsugiApiBase.executeAniListQuery(query, variables)
                        if (response == null) {
                            Log.w(TAG, "AniList yanıt null: idParam=$idParam, value=${if (aniListId != null) aniListId else externalId}")
                            return@runCatching emptyList<KitsugiCharacter>()
                        }
                        // GraphQL hata kontrolu
                        if (response.contains("\"errors\"")) {
                            Log.e(TAG, "AniList GraphQL hata yanıtı: ${response.take(300)}")
                        }
                        val root = JSONObject(response)
                        val edges = root.optJSONObject("data")
                            ?.optJSONObject("Media")
                            ?.optJSONObject("characters")
                            ?.optJSONArray("edges")
                        if (edges == null) {
                            Log.w(TAG, "AniList edges null. data.Media null mu: ${root.optJSONObject("data")?.optJSONObject("Media") == null}")
                            return@runCatching emptyList<KitsugiCharacter>()
                        }
                        Log.d(TAG, "AniList karakter sayısı: ${edges.length()}")
                        val list = mutableListOf<KitsugiCharacter>()
                        for (i in 0 until edges.length()) {
                            val edge = edges.optJSONObject(i) ?: continue
                            val role = (edge.optNullableString("role") ?: "Bilinmeyen").toTurkishCharacterRole()
                            val node = edge.optJSONObject("node") ?: continue
                            val id = node.optInt("id")
                            val nameObj = node.optJSONObject("name")
                            val name = nameObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
                            val imageUrl = node.optJSONObject("image")?.optNullableString("medium")

                            val vaList = mutableListOf<KitsugiVoiceActor>()
                            val vaArray = edge.optJSONArray("voiceActors")
                            if (vaArray != null) {
                                for (j in 0 until vaArray.length()) {
                                    val vaItem = vaArray.optJSONObject(j) ?: continue
                                    val vaId = vaItem.optInt("id")
                                    val vaName = vaItem.optJSONObject("name")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                                    val vaLang = (vaItem.optNullableString("languageV2")
                                        ?: vaItem.optNullableString("language")
                                        ?: "Japanese").toTurkishLanguage()
                                    val vaImageUrl = vaItem.optJSONObject("image")?.optNullableString("medium")
                                    vaList.add(KitsugiVoiceActor(vaId, vaName, vaLang, vaImageUrl, source = "anilist"))
                                }
                            }
                            list.add(KitsugiCharacter(id, name, role, imageUrl, vaList, source = "anilist"))
                        }
                        list
                    }.getOrElse { err ->
                        Log.e(TAG, "AniList fetch exception: ${err.javaClass.simpleName}: ${err.message}", err)
                        emptyList()
                    }
                }

                else -> {
                    // Bilinmeyen source — Jikan ile dene (MAL ID varsa)
                    val jikanId = realMalId?.takeIf { it > 0 } ?: externalId.takeIf { it > 0 && it < 100_000_000 }
                    if (jikanId != null) {
                        Log.w(TAG, "Bilinmeyen source '$source', Jikan fallback ile deneniyor: id=$jikanId")
                        fetchCharacters("jikan", jikanId, mediaType, null)
                    } else {
                        Log.e(TAG, "Bilinmeyen source '$source' ve geçerli MAL ID yok. Boş liste döndürülüyor.")
                        emptyList()
                    }
                }
            }
        }
    }

    suspend fun fetchCharacterDetail(
        source: String,
        characterId: Int,
        name: String? = null
    ): KitsugiCharacterDetail? {
        return withContext(Dispatchers.IO) {
            if (characterId <= 0) return@withContext null
            when (source.lowercase()) {
                "shikimori" -> {
                    KitsugiShikimoriClient.fetchCharacterDetail(characterId)
                }
                "jikan", "mal" -> {
                    var detail: KitsugiCharacterDetail? = null
                    var networkError: Throwable? = null

                    // 1. MAL / Jikan API
                    val url = URL("https://api.jikan.moe/v4/characters/$characterId/full")
                    val jikanRes = runCatching {
                        KitsugiApiBase.runWithRateLimit {
                            val response = KitsugiApiBase.executeGetRequestOrThrow(url)
                            val root = JSONObject(response)
                            val data = root.optJSONObject("data") ?: return@runWithRateLimit null

                            val charName = data.optNullableString("name") ?: "Bilinmeyen"
                            val nativeName = data.optNullableString("name_kanji")

                            val nicknamesArray = data.optJSONArray("nicknames")
                            val alternativeNames = mutableListOf<String>()
                            if (nicknamesArray != null) {
                                for (i in 0 until nicknamesArray.length()) {
                                    val nickname = nicknamesArray.optString(i)
                                    if (nickname.isNotBlank() && nickname != "null") {
                                        alternativeNames.add(nickname)
                                    }
                                }
                            }

                            val imageUrl = data.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                            val biography = data.optNullableString("about")?.cleanApiText()?.takeIf { it.isNotBlank() }

                            // Parse voice actors
                            val voiceActors = mutableListOf<KitsugiVoiceActor>()
                            val voicesArray = data.optJSONArray("voices")
                            if (voicesArray != null) {
                                for (i in 0 until voicesArray.length()) {
                                    val item = voicesArray.optJSONObject(i) ?: continue
                                    val personObj = item.optJSONObject("person") ?: continue
                                    val vaId = personObj.optInt("mal_id")
                                    val vaName = personObj.optNullableString("name") ?: "Bilinmeyen"
                                    val language = (item.optNullableString("language") ?: "Bilinmeyen").toTurkishLanguage()
                                    val vaImg = personObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                    voiceActors.add(KitsugiVoiceActor(vaId, vaName, language, vaImg))
                                }
                            }

                            // Parse media appearances
                            val mediaAppearances = mutableListOf<KitsugiCharacterMediaAppearance>()
                            val animeArray = data.optJSONArray("anime")
                            if (animeArray != null) {
                                for (i in 0 until animeArray.length()) {
                                    val item = animeArray.optJSONObject(i) ?: continue
                                    val role = (item.optNullableString("role") ?: "Bilinmeyen").toTurkishCharacterRole()
                                    val animeObj = item.optJSONObject("anime") ?: continue
                                    val mediaId = animeObj.optInt("mal_id")
                                    val title = animeObj.optNullableString("title") ?: "Bilinmeyen"
                                    val mediaImg = animeObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                    mediaAppearances.add(KitsugiCharacterMediaAppearance(
                                        mediaId = mediaId,
                                        title = title,
                                        imageUrl = mediaImg,
                                        mediaType = "anime".toTurkishMediaTypeString(),
                                        characterRole = role,
                                        source = "jikan"
                                    ))
                                }
                            }
                            val mangaArray = data.optJSONArray("manga")
                            if (mangaArray != null) {
                                for (i in 0 until mangaArray.length()) {
                                    val item = mangaArray.optJSONObject(i) ?: continue
                                    val role = (item.optNullableString("role") ?: "Bilinmeyen").toTurkishCharacterRole()
                                    val mangaObj = item.optJSONObject("manga") ?: continue
                                    val mediaId = mangaObj.optInt("mal_id")
                                    val title = mangaObj.optNullableString("title") ?: "Bilinmeyen"
                                    val mediaImg = mangaObj.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                                    mediaAppearances.add(KitsugiCharacterMediaAppearance(
                                        mediaId = mediaId,
                                        title = title,
                                        imageUrl = mediaImg,
                                        mediaType = "manga".toTurkishMediaTypeString(),
                                        characterRole = role,
                                        source = "jikan"
                                    ))
                                }
                            }

                            KitsugiCharacterDetail(
                                id = characterId,
                                name = charName,
                                nativeName = nativeName,
                                alternativeNames = alternativeNames,
                                imageUrl = imageUrl,
                                gender = null,
                                age = null,
                                birthday = null,
                                bloodType = null,
                                biography = biography,
                                voiceActors = voiceActors,
                                mediaAppearances = mediaAppearances
                            )
                        }
                    }
                    jikanRes.onSuccess {
                        detail = it
                    }.onFailure { err ->
                        if (err !is ResourceNotFoundException) {
                            networkError = err
                        }
                        Log.e(TAG, "Jikan fetch failed: ${err.message}", err)
                    }

                    // 2. Shikimori Fallback / Merge
                    runCatching {
                        KitsugiShikimoriClient.fetchCharacterDetail(characterId)
                    }.onSuccess { shikiDetail ->
                        if (shikiDetail != null) {
                            detail = if (detail == null) shikiDetail else detail!!.mergeWith(shikiDetail)
                        }
                    }.onFailure { err ->
                        Log.e(TAG, "Shikimori character detail fallback failed: ${err.message}", err)
                    }

                    // 3. AniList Fallback / Merge
                    val targetName = detail?.name ?: name
                    if (!targetName.isNullOrBlank()) {
                        runCatching {
                            fetchAniListCharacterByName(targetName)
                        }.onSuccess { aniListDetail ->
                            if (aniListDetail != null) {
                                detail = if (detail == null) aniListDetail else detail!!.mergeWith(aniListDetail)
                            }
                        }.onFailure { err ->
                            Log.e(TAG, "AniList character detail fallback failed: ${err.message}", err)
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
                            Character(id: ${'$'}id) {
                                id
                                isFavourite
                                name {
                                    userPreferred
                                    native
                                    alternative
                                    alternativeSpoiler
                                }
                                image {
                                    large
                                }
                                description
                                gender
                                dateOfBirth {
                                    year
                                    month
                                    day
                                }
                                age
                                bloodType
                                media(page: 1, perPage: 25) {
                                    edges {
                                        characterRole
                                        node {
                                            id
                                            idMal
                                            title { userPreferred english romaji native }
                                            coverImage { large }
                                            type
                                        }
                                        voiceActors {
                                            id
                                            name { userPreferred }
                                            image { medium }
                                            language
                                        }
                                    }
                                }
                            }
                        }
                    """.trimIndent()
                    val variables = JSONObject().put("id", characterId)
                    runCatching {
                        val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                        val root = JSONObject(response)
                        val data = root.optJSONObject("data")?.optJSONObject("Character") ?: return@runCatching null

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
                        val altSpoilerArray = nameObj?.optJSONArray("alternativeSpoiler")
                        if (altSpoilerArray != null) {
                            for (i in 0 until altSpoilerArray.length()) {
                                val alt = altSpoilerArray.optString(i)
                                if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                            }
                        }

                        val imageUrl = data.optJSONObject("image")?.optNullableString("large")
                        val biography = data.optNullableString("description")?.cleanApiText()?.takeIf { it.isNotBlank() }
                        val gender = data.optNullableString("gender")?.toTurkishGender()
                        val age = data.optNullableString("age")
                        val bloodType = data.optNullableString("bloodType")

                        val dobObj = data.optJSONObject("dateOfBirth")
                        val birthday = if (dobObj != null) {
                            val d = dobObj.optInt("day", 0)
                            val m = dobObj.optInt("month", 0)
                            val y = dobObj.optInt("year", 0)
                            if (d > 0 && m > 0) {
                                if (y > 0) "$d/$m/$y" else "$d/$m"
                            } else null
                        } else null

                        // Parse media appearances & collect unique voice actors
                        val mediaAppearances = mutableListOf<KitsugiCharacterMediaAppearance>()
                        val vaMap = mutableMapOf<Int, KitsugiVoiceActor>()

                        val mediaEdges = data.optJSONObject("media")?.optJSONArray("edges")
                        if (mediaEdges != null) {
                            for (i in 0 until mediaEdges.length()) {
                                val edge = mediaEdges.optJSONObject(i) ?: continue
                                val characterRole = (edge.optNullableString("characterRole") ?: "Bilinmeyen").toTurkishCharacterRole()
                                val node = edge.optJSONObject("node") ?: continue

                                val rawId = node.optInt("id")
                                val idMal = node.optInt("idMal")
                                val mediaId = if (idMal > 0) idMal else (100_000_000 + rawId)
                                val titleObj = node.optJSONObject("title")
                                val titleRomaji = titleObj?.optNullableString("romaji")
                                val titleEnglish = titleObj?.optNullableString("english")
                                val titleNative = titleObj?.optNullableString("native")
                                val title = titleObj?.optNullableString("userPreferred")
                                    ?: titleRomaji ?: titleEnglish ?: titleNative ?: "Bilinmeyen"
                                val mediaImg = node.optJSONObject("coverImage")?.optNullableString("large")
                                val mediaType = node.optNullableString("type").orEmpty().toTurkishMediaTypeString()

                                mediaAppearances.add(KitsugiCharacterMediaAppearance(
                                    mediaId = mediaId,
                                    title = title,
                                    imageUrl = mediaImg,
                                    mediaType = mediaType,
                                    characterRole = characterRole,
                                    source = "anilist",
                                    titleEnglish = titleEnglish,
                                    titleJapanese = titleNative,
                                    titleRomaji = titleRomaji
                                ))

                                val vaArray = edge.optJSONArray("voiceActors")
                                if (vaArray != null) {
                                    for (j in 0 until vaArray.length()) {
                                        val vaItem = vaArray.optJSONObject(j) ?: continue
                                        val vaId = vaItem.optInt("id")
                                        val vaName = vaItem.optJSONObject("name")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                                        val vaLang = (vaItem.optNullableString("languageV2")
                                            ?: vaItem.optNullableString("language")
                                            ?: "Japanese").toTurkishLanguage()
                                        val vaImageUrl = vaItem.optJSONObject("image")?.optNullableString("medium")
                                        vaMap[vaId] = KitsugiVoiceActor(vaId, vaName, vaLang, vaImageUrl, source = "anilist")
                                    }
                                }
                            }
                        }

                        val isFavourite = data.optBoolean("isFavourite", false)
                        KitsugiCharacterDetail(
                            id = characterId,
                            name = name,
                            nativeName = nativeName,
                            alternativeNames = alternativeNames,
                            imageUrl = imageUrl,
                            gender = gender,
                            age = age,
                            birthday = birthday,
                            bloodType = bloodType,
                            biography = biography,
                            voiceActors = vaMap.values.toList(),
                            mediaAppearances = mediaAppearances,
                            isFavourite = isFavourite,
                            aniListId = characterId
                        )
                    }.getOrNull()
                }
                "tmdb" -> {
                    val tmdbRes = TmdbApiClient().fetchPersonCharacterDetail(characterId)
                    if (tmdbRes != null && KitsugiApplication.getInstance()?.let { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(it) } != null) {
                        val aniListDetail = fetchAniListCharacterByName(tmdbRes.name)
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

    private suspend fun fetchAniListCharacterByName(name: String): KitsugiCharacterDetail? {
        val query = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 1) {
                    characters(search: ${'$'}search) {
                        id
                        isFavourite
                        name {
                            userPreferred
                            native
                            alternative
                            alternativeSpoiler
                        }
                        image {
                            large
                        }
                        description
                        gender
                        dateOfBirth {
                            year
                            month
                            day
                        }
                        age
                        bloodType
                        media(page: 1, perPage: 25) {
                            edges {
                                characterRole
                                node {
                                    id
                                    idMal
                                    title { userPreferred english romaji native }
                                    coverImage { large }
                                    type
                                }
                                voiceActors {
                                    id
                                    name { userPreferred }
                                    image { medium }
                                    language
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
            val charactersArr = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("characters") ?: return@runCatching null
            if (charactersArr.length() == 0) return@runCatching null
            val data = charactersArr.getJSONObject(0)

            val nameObj = data.optJSONObject("name")
            val charName = nameObj?.optNullableString("userPreferred") ?: "Bilinmeyen"
            val nativeName = nameObj?.optNullableString("native")

            val alternativeNames = mutableListOf<String>()
            val altArray = nameObj?.optJSONArray("alternative")
            if (altArray != null) {
                for (i in 0 until altArray.length()) {
                    val alt = altArray.optString(i)
                    if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                }
            }
            val altSpoilerArray = nameObj?.optJSONArray("alternativeSpoiler")
            if (altSpoilerArray != null) {
                for (i in 0 until altSpoilerArray.length()) {
                    val alt = altSpoilerArray.optString(i)
                    if (alt.isNotBlank() && alt != "null") alternativeNames.add(alt)
                }
            }

            val imageUrl = data.optJSONObject("image")?.optNullableString("large")
            val biography = data.optNullableString("description")?.cleanApiText()

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
            val bloodType = data.optNullableString("bloodType")

            val mediaAppearances = mutableListOf<KitsugiCharacterMediaAppearance>()
            val voiceActors = mutableListOf<KitsugiVoiceActor>()

            val mediaObj = data.optJSONObject("media")
            val edges = mediaObj?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val node = edge.optJSONObject("node") ?: continue
                    val role = (edge.optNullableString("characterRole") ?: "SUPPORTING").toTurkishCharacterRole()
                    val mediaId = node.optInt("id")
                    val idMal = node.optionalPositiveInt("idMal")
                    val title = node.optJSONObject("title")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                    val titleEnglish = node.optJSONObject("title")?.optNullableString("english")
                    val titleNative = node.optJSONObject("title")?.optNullableString("native")
                    val titleRomaji = node.optJSONObject("title")?.optNullableString("romaji")
                    val imgUrl = node.optJSONObject("coverImage")?.optNullableString("large")
                    val type = node.optNullableString("type").orEmpty().lowercase()

                    val stableId = idMal ?: (100_000_000 + mediaId)

                    mediaAppearances.add(
                        KitsugiCharacterMediaAppearance(
                            mediaId = stableId,
                            title = title,
                            imageUrl = imgUrl,
                            mediaType = type.toTurkishMediaTypeString(),
                            characterRole = role,
                            source = if (idMal != null) "jikan" else "anilist",
                            titleEnglish = titleEnglish,
                            titleJapanese = titleNative,
                            titleRomaji = titleRomaji
                        )
                    )

                    val vaArr = edge.optJSONArray("voiceActors")
                    if (vaArr != null) {
                        for (j in 0 until vaArr.length()) {
                            val va = vaArr.optJSONObject(j) ?: continue
                            val vaId = va.optInt("id")
                            val vaName = va.optJSONObject("name")?.optNullableString("userPreferred") ?: "Bilinmeyen"
                            val vaImg = va.optJSONObject("image")?.optNullableString("medium")
                            val vaLang = (va.optNullableString("language") ?: "Japanese").toTurkishLanguage()
                            voiceActors.add(
                                KitsugiVoiceActor(
                                    id = vaId,
                                    name = vaName,
                                    language = vaLang,
                                    imageUrl = vaImg,
                                    source = "anilist"
                                )
                            )
                        }
                    }
                }
            }

            val isFavourite = data.optBoolean("isFavourite", false)
            KitsugiCharacterDetail(
                id = data.optInt("id"),
                name = charName,
                nativeName = nativeName,
                alternativeNames = alternativeNames,
                imageUrl = imageUrl,
                biography = biography,
                gender = gender,
                birthday = birthday,
                age = age,
                bloodType = bloodType,
                mediaAppearances = mediaAppearances,
                voiceActors = voiceActors,
                isFavourite = isFavourite,
                aniListId = data.optInt("id")
            )
        }.getOrNull()
    }
}
