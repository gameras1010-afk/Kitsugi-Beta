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
        val poppedWithIndices = previousBackStack.mapIndexed { index, screen -> index to screen }
            .filter { (_, screen) -> screen !in detailBackStack }

        val baseOffset = (if (fullScreenGridState != null) 1 else 0) +
                         (if (mangaBrowseOpen) 1 else 0) +
                         (if (mangaDetailNavState != null) 1 else 0) +
                         (if (mangaReaderNavState != null) 1 else 0) +
                         (if (mangaSourceHealthOpen) 1 else 0)

        poppedWithIndices.forEach { (idx, screen) ->
            val depth = baseOffset + (idx + 1)
            val key = when (screen) {
                is DetailScreen.MediaDetail -> "media_${depth}_${screen.entryId}"
                is DetailScreen.ApiResultDetail -> "api_${depth}_${screen.result.source}_${screen.result.malId}"
                is DetailScreen.CharacterDetail -> "char_${depth}_${screen.source}_${screen.characterId}"
                is DetailScreen.StaffDetail -> "staff_${depth}_${screen.source}_${screen.staffId}"
                is DetailScreen.StudioDetail -> "studio_${depth}_${screen.source}_${screen.studioId}"
                is DetailScreen.AiringCalendar -> "airing_calendar_${depth}"
                DetailScreen.Stats -> "stats_${depth}"
                DetailScreen.Favourites -> "favourites_${depth}"
                DetailScreen.About -> "about_${depth}"
                is DetailScreen.UserProfile -> "user_profile_${depth}_${screen.userId}"
                is DetailScreen.UserMediaList -> "user_media_list_${depth}_${screen.userId}_${screen.initialMediaType.name}"
                DetailScreen.Notifications -> "notifications_${depth}"
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
