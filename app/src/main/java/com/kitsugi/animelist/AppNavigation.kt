package com.kitsugi.animelist

import com.kitsugi.animelist.data.manga.MangaChapter
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.app.FullScreenMediaGridState
import com.kitsugi.animelist.ui.navigation.MainTab

sealed interface DetailScreen {
    data class MediaDetail(val entryId: Int) : DetailScreen
    data class ApiResultDetail(val result: JikanSearchResult) : DetailScreen
    data class CharacterDetail(val characterId: Int, val source: String, val name: String? = null, val imageUrl: String? = null) : DetailScreen
    data class StaffDetail(val staffId: Int, val source: String, val name: String? = null, val imageUrl: String? = null) : DetailScreen
    data class StudioDetail(val studioId: Int, val source: String, val name: String? = null, val imageUrl: String? = null) : DetailScreen
    data object AiringCalendar : DetailScreen
    data object Stats : DetailScreen
    data object Favourites : DetailScreen
    data object About : DetailScreen
    data class UserProfile(val userId: Int, val username: String? = null, val avatarUrl: String? = null) : DetailScreen
}

sealed interface AppStateKey {
    val depth: Int

    data class Tab(val tab: MainTab) : AppStateKey {
        override val depth: Int = 0
    }
    data class StudioDetail(val studioId: Int, val source: String, override val depth: Int, val name: String? = null, val imageUrl: String? = null) : AppStateKey
    data class StaffDetail(val staffId: Int, val source: String, override val depth: Int, val name: String? = null, val imageUrl: String? = null) : AppStateKey
    data class CharacterDetail(val characterId: Int, val source: String, override val depth: Int, val name: String? = null, val imageUrl: String? = null) : AppStateKey
    data class ApiResultDetail(val result: JikanSearchResult, override val depth: Int) : AppStateKey
    data class MediaDetail(val entryId: Int, override val depth: Int) : AppStateKey
    data class FullScreenGrid(val state: FullScreenMediaGridState, override val depth: Int) : AppStateKey
    data class MangaBrowse(override val depth: Int) : AppStateKey
    data class MangaDetail(override val depth: Int) : AppStateKey
    data class MangaReader(override val depth: Int) : AppStateKey
    data class MangaSourceHealth(override val depth: Int) : AppStateKey
    data class AiringCalendar(override val depth: Int) : AppStateKey
    data class Stats(override val depth: Int) : AppStateKey
    data class Favourites(override val depth: Int) : AppStateKey
    data class About(override val depth: Int) : AppStateKey
    data class UserProfile(val userId: Int, override val depth: Int, val username: String? = null, val avatarUrl: String? = null) : AppStateKey
}

/**
 * Manga detay/okuma sayfasını açmak için gereken geçici navigasyon verisi.
 * MangaSource bir interface olduğu için AppStateKey içinde saklanamaz;
 * bunun yerine AppRoot'ta ayrı bir remember değişkeninde tutulur.
 *
 * Bu ekran; kapak, açıklama, bölüm listesi ve "Oku / Devam Et" butonunu gösterir.
 * Bir bölüm seçilince buradan [MangaReaderNavState] kurulur.
 */
data class MangaDetailNavState(
    val source: MangaSource,
    val mangaDetails: MangaDetails
)

/**
 * Manga okuyucusunu açmak için gereken geçici navigasyon verisi.
 * MangaSource bir interface olduğu için AppStateKey içinde saklanamaz;
 * bunun yerine AppRoot'ta ayrı bir remember değişkeninde tutulur.
 */
data class MangaReaderNavState(
    val source: MangaSource,
    val mangaDetails: MangaDetails,
    val chapter: MangaChapter
)
