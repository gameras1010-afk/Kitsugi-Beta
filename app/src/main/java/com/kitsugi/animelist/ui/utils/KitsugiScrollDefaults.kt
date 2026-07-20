package com.kitsugi.animelist.ui.utils

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * KitsugiScrollDefaults — TV-native Scroll Davranışı
 *
 * KitsugiTV-dev referansı: BringIntoViewSpec / ScrollBehavior.
 *
 * Android TV D-pad navigasyonunda fokus alan öğe, liste sınırında
 * kısmi görünürken kesilmeden tam olarak ekrana getirilir.
 *
 * Kullanım — LazyRow/LazyColumn'da CompositionLocalProvider ile:
 * ```kotlin
 * val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
 * CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
 *     LazyRow(...) { ... }
 * }
 * ```
 *
 * ⚠️ Bu API @ExperimentalFoundationApi kapsamındadır.
 */
object KitsugiScrollDefaults {

    /**
     * Fokus alan öğeyi viewport'un yatay merkezine getirir.
     * TV D-pad sola/sağa gezinimde doğal, yumuşak scroll sağlar.
     *
     * @param animationSpec Scroll geçiş animasyonu — varsayılan spring ile TV-native hissiyat.
     * @param centeringFraction 0f=sol kenar, 0.5f=merkez, 1f=sağ kenar. TV için 0.5f.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun rememberTvCenteredSpec(
        animationSpec: AnimationSpec<Float> = spring(),
        centeringFraction: Float = 0.5f
    ): BringIntoViewSpec {
        return remember(centeringFraction, animationSpec) {
            object : BringIntoViewSpec {
                override val scrollAnimationSpec: AnimationSpec<Float> = animationSpec

                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float
                ): Float {
                    // Öğe merkezini hesapla
                    val itemCenter = offset + size / 2f
                    // Viewport'un hedef merkezi
                    val viewportCenter = containerSize * centeringFraction
                    val scrollNeeded = itemCenter - viewportCenter

                    return when {
                        offset < 0f -> scrollNeeded                    // Sol kenardan taşıyor
                        offset + size > containerSize -> scrollNeeded  // Sağ kenardan taşıyor
                        else -> 0f                                     // Görünür, scroll gereksiz
                    }
                }
            }
        }
    }

    /**
     * Fokus alan satırın (row) ekranın dikey koordinatında belirli bir inset'te (örneğin 280.dp)
     * kalmasını sağlayan dikey scroll davranışı.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun rememberTvVerticalInsetSpec(
        insetDp: androidx.compose.ui.unit.Dp,
        canScrollBackwardProvider: () -> Boolean,
        animationSpec: AnimationSpec<Float> = spring()
    ): BringIntoViewSpec {
        val density = androidx.compose.ui.platform.LocalDensity.current
        return remember(insetDp, density, animationSpec) {
            val insetPx = with(density) { insetDp.toPx() }
            object : BringIntoViewSpec {
                override val scrollAnimationSpec: AnimationSpec<Float> = animationSpec

                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float
                ): Float {
                    val currentLeadingEdge = offset
                    if (kotlin.math.abs(currentLeadingEdge - insetPx) < 1f) return 0f
                    val distance = currentLeadingEdge - insetPx
                    if (distance < 0f && !canScrollBackwardProvider()) return 0f
                    return distance
                }
            }
        }
    }
}
