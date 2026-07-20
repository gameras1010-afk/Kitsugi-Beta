package com.kitsugi.animelist.ui.screens.manga.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.ReadingMode
import com.kitsugi.animelist.data.manga.ColorFilterType
import com.kitsugi.animelist.data.manga.MangaFitMode
import com.kitsugi.animelist.data.manga.loader.MangaPageLoaderV2

/**
 * Yatay sayfa çevirmeli okuma modu (LTR / RTL / Dikey Pager).
 *
 * — LTR (Soldan Sağa): Manhua, Batı çizgi romanları için
 * — RTL (Sağdan Sola): Japonca Manga için
 *
 * RTL modu, sayfaları tersine çevirerek HorizontalPager'ı
 * sağdan sola kaydıracak şekilde yapılandırır.
 *
 * Her sayfa MangaPageItem bileşeni ile çizilir:
 *   • Telephoto ZoomableAsyncImage → pinch-to-zoom, double-tap
 *   • MangaPageLoaderV2 ile 3 ileri prefetch + Coil cache
 */
@Composable
fun MangaPagerReader(
    pages: List<MangaPage>,
    pagerState: PagerState,
    loader: MangaPageLoaderV2,
    readingMode: ReadingMode = ReadingMode.RightToLeft,
    colorFilterType: ColorFilterType = ColorFilterType.Normal,
    fitMode: MangaFitMode = MangaFitMode.FitScreen,
    onCenterTap: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRtl       = readingMode == ReadingMode.RightToLeft
    val direction   = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        HorizontalPager(
            state    = pagerState,
            modifier = modifier.fillMaxSize(),
            key      = { index -> pages.getOrNull(index)?.index ?: index },
            beyondViewportPageCount = 2   // Yan sayfaları belleğe al
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MangaPageItem(
                    page        = page,
                    loader      = loader,
                    readingMode = readingMode,
                    colorFilterType = colorFilterType,
                    fitMode     = fitMode,
                    onCenterTap = onCenterTap,
                    onNextPage   = onNextPage,
                    onPrevPage   = onPrevPage,
                    modifier    = Modifier.fillMaxSize()
                )
            }
        }
    }
}
