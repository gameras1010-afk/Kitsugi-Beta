package com.kitsugi.animelist.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.DetailScreen
import com.kitsugi.animelist.MangaDetailNavState
import com.kitsugi.animelist.MangaReaderNavState

@Stable
class AppNavigationState(
    val stateHolder: SaveableStateHolder
) {
    var detailBackStack by mutableStateOf<List<DetailScreen>>(emptyList())
    var mangaBrowseOpen by mutableStateOf(false)
        private set
    var mangaBrowseQuery by mutableStateOf<String?>(null)
        private set
    var mangaBrowseMediaId by mutableStateOf<Int?>(null)
        private set

    fun openMangaBrowse(query: String? = null, mediaId: Int? = null) {
        mangaBrowseQuery = query
        mangaBrowseMediaId = mediaId
        mangaBrowseOpen = true
    }

    fun closeMangaBrowse() {
        mangaBrowseOpen = false
        mangaBrowseQuery = null
        mangaBrowseMediaId = null
    }

    var mangaSourceHealthOpen by mutableStateOf(false)
        private set

    fun openMangaSourceHealth() {
        mangaSourceHealthOpen = true
    }

    fun closeMangaSourceHealth() {
        mangaSourceHealthOpen = false
    }

    var mangaDetailNavState by mutableStateOf<MangaDetailNavState?>(null)
    var mangaReaderNavState by mutableStateOf<MangaReaderNavState?>(null)
    var fullScreenGridState by mutableStateOf<FullScreenMediaGridState?>(null)
    var previousBackStack by mutableStateOf<List<DetailScreen>>(emptyList())

    fun popDetailStack() {
        detailBackStack = detailBackStack.dropLast(1)
    }

    fun navigateToDetail(screen: DetailScreen) {
        detailBackStack = detailBackStack + screen
    }

    fun clearPreviousScreens() {
        val popped = previousBackStack.filter { it !in detailBackStack }
        popped.forEach { screen ->
            val key = when (screen) {
                is DetailScreen.MediaDetail -> "media_${screen.entryId}"
                is DetailScreen.ApiResultDetail -> "api_${screen.result.source}_${screen.result.malId}"
                is DetailScreen.CharacterDetail -> "character_${screen.source}_${screen.characterId}"
                is DetailScreen.StaffDetail -> "staff_${screen.source}_${screen.staffId}"
                is DetailScreen.StudioDetail -> "studio_${screen.source}_${screen.studioId}"
                DetailScreen.AiringCalendar -> "airing_calendar"
                DetailScreen.Stats -> "stats"
                DetailScreen.Favourites -> "favourites"
                DetailScreen.About -> "about"
                is DetailScreen.UserProfile -> "user_profile_${screen.userId}"
                is DetailScreen.UserMediaList -> "user_media_list_${screen.userId}_${screen.initialMediaType.name}"
            }
            stateHolder.removeState(key)
        }
        previousBackStack = detailBackStack
    }
}

@Composable
fun rememberAppNavigationState(
    stateHolder: SaveableStateHolder = rememberSaveableStateHolder()
): AppNavigationState {
    return remember(stateHolder) {
        AppNavigationState(stateHolder)
    }
}
