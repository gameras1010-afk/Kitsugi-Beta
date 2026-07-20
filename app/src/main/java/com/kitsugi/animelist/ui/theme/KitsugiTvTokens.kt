package com.kitsugi.animelist.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * KitsugiTV Tasarım Token'ları
 *
 * KitsugiTV-dev projesindeki SizeTokens, SpacingTokens, ShapeTokens, ve MotionFocusTokens
 * kaynak alınarak KitsugiAnimeList'e uyarlanmıştır.
 *
 * Kullanım: sadece `LocalIsTv.current == true` bloklarında kullanılır.
 * Mobil/tablet koduna dokunmaz.
 */
object KitsugiTvTokens {

    // ─── Motion & Animasyon Token'ları ─────────────────────────────────────────
    object Motion {
        val instant: Int = 0
        val quick: Int = 125
        val fast: Int = 180
        val medium: Int = 350
        val slow: Int = 450
        val overlay: Int = 400
        val hero: Int = 450
        val shimmer: Int = 1200

        val standardEasing: Easing = FastOutSlowInEasing
        val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val decelerateEasing: Easing = LinearOutSlowInEasing
        val accelerateEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

        fun <T> quickTween(): TweenSpec<T> = tween(quick, easing = standardEasing)
        fun <T> focusTween(): TweenSpec<T> = tween(fast, easing = standardEasing)
        fun <T> mediumTween(): TweenSpec<T> = tween(medium, easing = standardEasing)
        fun <T> slowTween(): TweenSpec<T> = tween(slow, easing = decelerateEasing)

        fun <T> focusSpring(): SpringSpec<T> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    // ─── Odak (Focus) Token'ları ──────────────────────────────────────────────
    object Focus {
        const val scale: Float = 1.02f
        const val subtleScale: Float = 1.01f
        const val pressedScale: Float = 0.98f
        const val disabledScale: Float = 1.0f
        const val scrollViewportTarget: Float = 0.42f
        const val longPressInitialDelayMillis: Long = 360L
        const val longPressRepeatMillis: Long = 72L
    }

    // ─── Kart Boyutları ──────────────────────────────────────────────────────
    // Referans: KitsugiTV-dev / SizeTokens.kt → KitsugiCardSizes
    object Cards {
        /** 2:3 dikey poster genişliği (yatay listeler ve keşfet grid) */
        val posterWidth: Dp = 126.dp

        /** 2:3 dikey poster yüksekliği */
        val posterHeight: Dp = 189.dp

        /** Kompakt poster */
        val posterCompactWidth: Dp = 112.dp
        val posterCompactHeight: Dp = 168.dp

        /** Yatay arkaplan/afiş kartı */
        val backdropWidth: Dp = 320.dp
        val backdropHeight: Dp = 180.dp

        /** Devam et izle kartı */
        val continueWatchingWidth: Dp = 260.dp
        val continueWatchingHeight: Dp = 146.dp

        /** Bölüm (episode) kartı */
        val episodeWidth: Dp = 320.dp
        val episodeHeight: Dp = 207.dp

        /** Keşfet kategori kartı yüksekliği */
        val categoryHeight: Dp = 64.dp

        /** Arama sonucu thumbnail */
        val searchThumbWidth: Dp = 56.dp
        val searchThumbHeight: Dp = 80.dp

        /** D-pad focus scale faktörü */
        const val focusedScale: Float = 1.02f

        /** D-pad focus border kalınlığı */
        val focusedBorderWidth: Dp = 2.dp
    }

    // ─── Medya Oynatıcı (Player) Token'ları ───────────────────────────────────
    object Player {
        val controlSize: Dp = 44.dp
        val compactControlSize: Dp = 40.dp
        val sidePanelWidth: Dp = 360.dp
        val railWidth: Dp = 280.dp
        val progressHeight: Dp = 4.dp
        val overlayHorizontalPadding: Dp = 52.dp
        val overlayVerticalPadding: Dp = 36.dp
    }

    // ─── Ayarlar (Settings) Token'ları ───────────────────────────────────────
    object Settings {
        val railWidth: Dp = 260.dp
        val railItemHeight: Dp = 56.dp
        val workspacePadding: Dp = 20.dp
        val rowMinHeight: Dp = 64.dp
        val rowGap: Dp = 16.dp
    }

    // ─── Boşluk Değerleri ────────────────────────────────────────────────────
    // Referans: KitsugiTV-dev / SpacingTokens.kt → KitsugiSpacingTokens
    object Spacing {
        val none: Dp = 0.dp
        val hairline: Dp = 1.dp
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
        val xxxl: Dp = 48.dp
        val huge: Dp = 56.dp

        /** Yatay listedeki kartlar arası boşluk (rail.itemGap) */
        val itemGap: Dp = 12.dp

        /** Satırlar arası dikey boşluk (rail.rowGap) */
        val rowGap: Dp = 24.dp

        /** Grid satırları arası boşluk */
        val gridRowGap: Dp = 10.dp

        /** Ekran yatay kenar boşluğu (screen.horizontal) */
        val screenHorizontal: Dp = 48.dp

        /** Ekran dikey kenar boşluğu (screen.vertical) */
        val screenVertical: Dp = 24.dp

        /** TV overscan yatay boşluk (screen.overscanHorizontal) */
        val overscanHorizontal: Dp = 56.dp
        val overscanVertical: Dp = 36.dp

        /** Liste sonundaki ekstra kaydırma boşluğu (rail.tailPadding) */
        val railTailPadding: Dp = 200.dp
        val railHeaderBottom: Dp = 14.dp

        /** Genel iç içerik dolgusu */
        val contentPadding: Dp = 16.dp
    }

    // ─── Şekil Token'ları ─────────────────────────────────────────────────────
    // Referans: KitsugiTV-dev / ShapeTokens.kt → KitsugiRadii (md=12, xl=16)
    object Shapes {
        /** Poster kartı köşe yarıçapı */
        val posterCard: Shape = RoundedCornerShape(12.dp)

        /** Yatay afiş/bölüm kartı köşe yarıçapı */
        val backdropCard: Shape = RoundedCornerShape(16.dp)

        /** Buton köşe yarıçapı */
        val button: Shape = RoundedCornerShape(12.dp)

        /** Chip / pill köşe yarıçapı */
        val chip: Shape = RoundedCornerShape(999.dp)

        /** Dialog köşe yarıçapı */
        val dialog: Shape = RoundedCornerShape(16.dp)

        /** Ayarlar ana konteyner köşe yarıçapı */
        val settingsContainer: Shape = RoundedCornerShape(28.dp)
        val settingsSecondaryCard: Shape = RoundedCornerShape(18.dp)

        /** Navigation item köşe yarıçapı */
        val navItem: Shape = RoundedCornerShape(999.dp)

        /** Kategori kartı köşe yarıçapı */
        val categoryCard: Shape = RoundedCornerShape(14.dp)
    }

    // ─── Düzen Parametreleri ──────────────────────────────────────────────────
    // Referans: KitsugiTV-dev / SizeTokens.kt → KitsugiSidebarSizes
    object Layout {
        /** Keşfet kategori grid sütun sayısı (TV) */
        const val exploreCategoryColumns: Int = 4

        /** Shimmer satırı kart sayısı */
        const val shimmerCardCount: Int = 6

        /** Genişlemiş sidebar genişliği (sidebar.expandedWidth) */
        val sidebarExpandedWidth: Dp = 262.dp

        /** Kompakt sidebar genişliği (sidebar.compactWidth) */
        val sidebarCompactWidth: Dp = 72.dp

        /** Navigation item yüksekliği (sidebar.railItemHeight) */
        val navItemHeight: Dp = 52.dp

        /** Sidebar icon boyutu (sidebar.leadingVisual) */
        val sidebarIconSize: Dp = 34.dp
    }
}
