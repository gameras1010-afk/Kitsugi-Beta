package com.kitsugi.animelist.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

// ─── Motion Token'ları ─────────────────────────────────────────────────────
// KitsugiMobile Tokens.kt'deki motion.normalMillis vb. değerlerden uyarlama.
// Merkezi yönetimle tüm tween sürelerini tek yerden güncelleme imkânı sağlar.

object KitsugiMotion {
    /** Hızlı mikro-etkileşimler: buton basımı, chip toggle */
    const val fastMillis = 150

    /** Normal navigasyon geçişleri: sağdan/soldan slayt */
    const val normalMillis = 280

    /** Yavaş modal / bottom sheet açılma */
    const val slowMillis = 380

    /** Tab geçiş crossfade */
    const val tabFadeMillis = 200

    /** Sheet enter — FloatingPrompt gibi sürüklenebilir paneller */
    const val sheetEnterMillis = 420
}

// ─── Detay Sayfasına Giriş Geçişi ─────────────────────────────────────────
// Sağdan sola süzülerek giren detay sayfası (Android default NavHost hareketi).

fun enterDetailTransition(): EnterTransition =
    slideInHorizontally(tween(KitsugiMotion.normalMillis)) { it } +
        fadeIn(tween(KitsugiMotion.normalMillis))

fun exitForDetailTransition(): ExitTransition =
    slideOutHorizontally(tween(KitsugiMotion.normalMillis)) { -it / 4 } +
        fadeOut(tween(KitsugiMotion.fastMillis + 50))

// ─── Detay Sayfasından Çıkış Geçişi ───────────────────────────────────────
// Detay kapanırken soldan geri döner, önceki içerik sağa çekilir.

fun enterFromDetailTransition(): EnterTransition =
    slideInHorizontally(tween(KitsugiMotion.normalMillis)) { -it / 4 } +
        fadeIn(tween(KitsugiMotion.normalMillis))

fun exitDetailTransition(): ExitTransition =
    slideOutHorizontally(tween(KitsugiMotion.normalMillis)) { it } +
        fadeOut(tween(KitsugiMotion.fastMillis + 50))

// ─── Tab Geçiş Animasyonu ─────────────────────────────────────────────────
// Tab değişimlerinde sade crossfade.

fun tabEnterTransition(): EnterTransition =
    fadeIn(tween(KitsugiMotion.tabFadeMillis))

fun tabExitTransition(): ExitTransition =
    fadeOut(tween(KitsugiMotion.tabFadeMillis - 20))

// ─── ContentTransform Yardımcıları ────────────────────────────────────────
// AppRoot.kt'nin AnimatedContent transitionSpec'inde kullanım kolaylığı sağlar.

fun detailEnterContentTransform(): ContentTransform =
    enterDetailTransition() togetherWith exitForDetailTransition()

fun detailExitContentTransform(): ContentTransform =
    enterFromDetailTransition() togetherWith exitDetailTransition()

fun tabContentTransform(): ContentTransform =
    tabEnterTransition() togetherWith tabExitTransition()

// ─── Sayfa İçi Fade-In Geçiş ──────────────────────────────────────────────
// KitsugiMobile PersonDetailContent'teki AnimatedVisibility(visible=true, enter=fadeIn())
// uyarlaması — detay sayfası içeriği ilk kez render edildiğinde yumuşak belirme.

fun pageContentEnterTransition(): EnterTransition =
    fadeIn(tween(KitsugiMotion.slowMillis)) +
        slideInVertically(tween(KitsugiMotion.slowMillis)) { it / 8 }
