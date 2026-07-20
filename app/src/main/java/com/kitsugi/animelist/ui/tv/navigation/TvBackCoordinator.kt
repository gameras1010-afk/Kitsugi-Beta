package com.kitsugi.animelist.ui.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

/**
 * WP-03 — TV Back Stack ve Focus Koordinatörü
 *
 * TvRootScreen içindeki Back-öncelik mantığını kapsülleyen state coordinator.
 *
 * Back öncelik sırası (KitsugiTV sözleşmesi):
 *   1. Stream player overlay
 *   2. Manga reader
 *   3. Detail/character/staff/studio nested detail back-stack
 *   4. Manga detail
 *   5. Manga extension / source health overlay
 *   6. Addons overlay
 *   7. Sidebar focus → exit confirm dialog
 *
 * @param navigationState  TV back-stack ve overlay durumu
 * @param sidebarState     Sidebar focus sahipliği
 * @param onRequestExit    Çıkış onay dialogunu açma isteği
 */
@Stable
class TvBackCoordinator(
    private val navigationState: TvNavigationState,
    private val sidebarState: TvSidebarState,
    private val onRequestExit: () -> Unit
) {
    /**
     * Back tuşu basıldığında çağrılır.
     *
     * @return true eğer bu coordinator back event'i tüketti, false ise sistem'e bırakıldı.
     */
    fun handleBack(): Boolean {
        // 1. TvNavigationState'in iç back yönetimini çalıştır
        // (stream, mangaReader, mangaDetail, mangaExtension, addons, detailBackStack sırasında)
        if (navigationState.pop()) return true

        // 2. Sidebar focus varsa önce içeriğe döndür
        if (sidebarState.hasFocus) {
            sidebarState.collapse()
            return true
        }

        // 3. Hiçbir overlay yoksa çıkış onay dialogu iste
        onRequestExit()
        return true
    }

    /**
     * Back için hangi bağlamın aktif olduğunu döndürür (debug/diagnostics).
     */
    fun activeBackContext(): String = when {
        navigationState.streamTarget != null -> "stream"
        navigationState.mangaReaderTarget != null -> "mangaReader"
        navigationState.detailBackStack.isNotEmpty() -> "detail[${navigationState.detailBackStack.size}]"
        navigationState.mangaDetailTarget != null -> "mangaDetail"
        navigationState.isMangaSourceHealthActive -> "mangaSourceHealth"
        navigationState.isMangaExtensionActive -> "mangaExtension"
        navigationState.isAddonsActive -> "addons"
        sidebarState.hasFocus -> "sidebar"
        else -> "root"
    }
}

/**
 * [TvBackCoordinator]'u remember ile oluşturur.
 *
 * Bağımlılıklar dışarıdan enjekte edildiği için coordinator stabil kalır,
 * ancak [TvNavigationState] ve [TvSidebarState] kendi içlerinde reaktif.
 */
@Composable
fun rememberTvBackCoordinator(
    navigationState: TvNavigationState,
    sidebarState: TvSidebarState,
    onRequestExit: () -> Unit
): TvBackCoordinator = remember(navigationState, sidebarState) {
    TvBackCoordinator(navigationState, sidebarState, onRequestExit)
}

/**
 * Sidebar/Drawer item focus requester'larını temiz bir kapsayıcıda tutar.
 * TvRootScreen tarafından remember { } ile oluşturulur.
 */
@Stable
class TvDrawerFocusRequesters(
    val destinations: List<TvDestination>
) {
    private val requesters: Map<TvDestination, FocusRequester> =
        destinations.associateWith { FocusRequester() }

    operator fun get(dest: TvDestination): FocusRequester? = requesters[dest]

    fun toMap(): Map<TvDestination, FocusRequester> = requesters
}

/**
 * [TvDrawerFocusRequesters]'ı remember ile oluşturur.
 */
@Composable
fun rememberTvDrawerFocusRequesters(
    destinations: List<TvDestination> = TvDestination.entries
): TvDrawerFocusRequesters = remember(destinations) {
    TvDrawerFocusRequesters(destinations)
}
