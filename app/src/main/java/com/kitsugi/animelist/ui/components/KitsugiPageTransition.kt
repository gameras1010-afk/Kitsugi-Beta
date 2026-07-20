package com.kitsugi.animelist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ─── Sayfa İçeriği Giriş Animasyonu ───────────────────────────────────────
// KitsugiMobile'daki PersonDetailContent'in şu yapısından uyarlama:
//
//   AnimatedVisibility(
//       visible = true,
//       enter = fadeIn(),
//   ) { ... }
//
// Her detay sayfasının içeriği ilk render'da bu wrapper içinde belirir.
// "visible = true" ile başlatıldığında bir kez çalışır ve biter — sürekli
// animasyon döngüsüne girmez.

import androidx.compose.foundation.layout.Box

@Composable
fun KitsugiPageEnter(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = pageContentEnterTransition()
    ) {
        content()
    }
}

// ─── Hata Durumu Giriş Animasyonu ─────────────────────────────────────────
// Hata mesajı veya boş durum bileşenleri için daha yavaş fade-in.

@Composable
fun KitsugiErrorEnter(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = KitsugiMotion.slowMillis),
        ),
    ) {
        content()
    }
}
