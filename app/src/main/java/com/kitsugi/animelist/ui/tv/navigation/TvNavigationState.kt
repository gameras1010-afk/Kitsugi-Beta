package com.kitsugi.animelist.ui.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.kitsugi.animelist.core.deeplink.PendingDetailLink
import com.kitsugi.animelist.core.deeplink.PendingMangaLink
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.tv.stream.TvStreamArgs

enum class TvDestination { HOME, SEARCH, LIBRARY, MANGA, SETTINGS, ACCOUNT }

data class TvNavItem(
    val destination: TvDestination,
    val icon: ImageVector,
    val label: String
)

/** Manga browse → detail geçiş verisi */
data class TvMangaDetailArgs(
    val source: MangaSource,
    val manga: MangaDetails
)

/** Manga okuyucu geçiş verisi */
data class TvMangaReaderArgs(
    val chapter: com.kitsugi.animelist.data.manga.MangaChapter,
    val manga: com.kitsugi.animelist.data.manga.MangaDetails,
    val source: com.kitsugi.animelist.data.manga.MangaSource
)

sealed interface TvDetailTarget {
    data class Media(val result: JikanSearchResult) : TvDetailTarget
    data class Character(val characterId: Int, val source: String, val name: String?, val imageUrl: String?) : TvDetailTarget
    data class Staff(val staffId: Int, val source: String, val name: String?, val imageUrl: String?) : TvDetailTarget
    data class Studio(val studioId: Int, val source: String, val name: String?, val imageUrl: String?) : TvDetailTarget
}

@Stable
class TvNavigationState {
    var currentTab by mutableStateOf(TvDestination.HOME)
    val detailBackStack = mutableStateListOf<TvDetailTarget>()
    var streamTarget by mutableStateOf<TvStreamArgs?>(null)

    // ── Manga navigation ────────────────────────────────────────────────────────
    var mangaDetailTarget by mutableStateOf<TvMangaDetailArgs?>(null)
    var mangaReaderTarget by mutableStateOf<TvMangaReaderArgs?>(null)

    // ── Addon/Extension navigation ──────────────────────────────────────────────
    var isAddonsActive by mutableStateOf(false)

    // ── Manga Extension & Source Health navigation ──────────────────────────────
    var isMangaExtensionActive by mutableStateOf(false)
    var isMangaSourceHealthActive by mutableStateOf(false)

    // ── TV Companion navigation ────────────────────────────────────────────────
    var isCompanionActive by mutableStateOf(false)

    // ── Pending deep link state (B1.1) ───────────────────────────────────────
    // Channels / cold-start deep link'ten gelen navigasyon isteği.
    // ViewModel bu alanı tüketince null'a çeker.
    var pendingDeepLinkDetail by mutableStateOf<PendingDetailLink?>(null)
    var pendingDeepLinkManga  by mutableStateOf<PendingMangaLink?>(null)

    fun navigateToDetail(result: JikanSearchResult) {
        detailBackStack.add(TvDetailTarget.Media(result))
    }

    fun navigateToStream(args: TvStreamArgs) {
        streamTarget = args
    }

    fun navigateToCharacterDetail(characterId: Int, source: String, name: String?, imageUrl: String?) {
        detailBackStack.add(TvDetailTarget.Character(characterId, source, name, imageUrl))
    }

    fun navigateToStaffDetail(staffId: Int, source: String, name: String?, imageUrl: String?) {
        detailBackStack.add(TvDetailTarget.Staff(staffId, source, name, imageUrl))
    }

    fun navigateToStudioDetail(studioId: Int, source: String, name: String?, imageUrl: String?) {
        detailBackStack.add(TvDetailTarget.Studio(studioId, source, name, imageUrl))
    }

    fun navigateToMangaDetail(source: MangaSource, manga: MangaDetails) {
        mangaDetailTarget = TvMangaDetailArgs(source, manga)
    }

    fun navigateToMangaReader(chapter: com.kitsugi.animelist.data.manga.MangaChapter, manga: MangaDetails, source: MangaSource) {
        mangaReaderTarget = TvMangaReaderArgs(chapter, manga, source)
    }

    fun navigateToAddons() {
        isAddonsActive = true
    }

    fun navigateToMangaExtension() {
        isMangaExtensionActive = true
    }

    fun navigateToMangaSourceHealth() {
        isMangaSourceHealthActive = true
    }

    fun navigateToCompanion() {
        isCompanionActive = true
    }

    fun pop(): Boolean {
        if (isCompanionActive) {
            isCompanionActive = false
            return true
        }
        if (isMangaSourceHealthActive) {
            isMangaSourceHealthActive = false
            return true
        }
        if (mangaReaderTarget != null) {
            mangaReaderTarget = null
            return true
        }
        if (streamTarget != null) {
            streamTarget = null
            return true
        }
        if (mangaDetailTarget != null) {
            mangaDetailTarget = null
            return true
        }
        if (isMangaExtensionActive) {
            isMangaExtensionActive = false
            return true
        }
        if (isAddonsActive) {
            isAddonsActive = false
            return true
        }
        if (detailBackStack.isNotEmpty()) {
            detailBackStack.removeAt(detailBackStack.lastIndex)
            return true
        }
        return false
    }

    fun clearDetails() {
        detailBackStack.clear()
        streamTarget = null
        mangaDetailTarget = null
        mangaReaderTarget = null
        isAddonsActive = false
        isMangaExtensionActive = false
        isMangaSourceHealthActive = false
        isCompanionActive = false
        // Pending deep links de temizlenir (tab switch, logout)
        pendingDeepLinkDetail = null
        pendingDeepLinkManga  = null
    }
}

@Composable
fun rememberTvNavigationState(): TvNavigationState {
    return remember { TvNavigationState() }
}

// B1.1 - Pending deep link'i DeepLinkHandler'dan drain edip TvNavigationState'e uygular.
// TvRootScreen'de LaunchedEffect ile cagrilir; session hazir olduktan sonra.
// Drain tek seferlik; ikinci cagirida pending yoksa no-op.
fun TvNavigationState.drainDeepLink() {
    val link = com.kitsugi.animelist.core.deeplink.DeepLinkHandler.drainPending()
        ?: return

    when (link) {
        is com.kitsugi.animelist.core.deeplink.TvDeepLink.Detail -> {
            currentTab = TvDestination.HOME
            pendingDeepLinkDetail = PendingDetailLink(
                source   = link.source,
                mediaId  = link.mediaId,
                autoPlay = false
            )
        }
        is com.kitsugi.animelist.core.deeplink.TvDeepLink.Play -> {
            currentTab = TvDestination.HOME
            pendingDeepLinkDetail = PendingDetailLink(
                source   = link.source,
                mediaId  = link.mediaId,
                autoPlay = true,
                season   = link.season,
                episode  = link.episode
            )
        }
        is com.kitsugi.animelist.core.deeplink.TvDeepLink.Manga -> {
            currentTab = TvDestination.MANGA
            pendingDeepLinkManga = PendingMangaLink(
                sourceKey = link.sourceKey,
                mangaId   = link.mangaId,
                chapterId = link.chapterId
            )
        }
        com.kitsugi.animelist.core.deeplink.TvDeepLink.None,
        com.kitsugi.animelist.core.deeplink.TvDeepLink.Auth -> Unit
    }
}
