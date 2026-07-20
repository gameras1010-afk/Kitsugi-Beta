package com.kitsugi.animelist.ui.tv.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WP-03 — TV Root Shell: Sidebar Durum Yönetimi
 *
 * Sidebar'ın daraltılmış/genişletilmiş geçişini, animasyon değerlerini
 * ve focus sahipliğini tutan Stable state holder.
 *
 * TvRootScreen bu sınıfı `rememberTvSidebarState()` ile remember eder;
 * bu sayede TvRootScreen recomposition'larından bağımsız sidebar durumu tutulur.
 *
 * @param compactWidth   Daraltılmış sidebar genişliği (sadece ikonlar görünür)
 * @param expandedWidth  Genişletilmiş sidebar genişliği (ikon + etiket)
 * @param animDurationMs Genişleme/daralma animasyon süresi (ms)
 */
@Stable
class TvSidebarState(
    private val compactWidth: Dp = 72.dp,
    private val expandedWidth: Dp = 220.dp,
    private val animDurationMs: Int = 300
) {
    /** Sidebar'ın şu an odaklanılmış olup olmadığı (focus sahipliği) */
    var hasFocus by mutableStateOf(false)
        internal set

    /** Sidebar genişletilmiş mi? Focus varsa genişletilmiş sayılır. */
    val isExpanded: Boolean get() = hasFocus

    /**
     * Sidebar focus sahipliğini günceller.
     * Genellikle `Modifier.onFocusChanged { sidebarState.onFocusChange(it.hasFocus) }` ile kullanılır.
     */
    fun onFocusChange(newHasFocus: Boolean) {
        hasFocus = newHasFocus
    }

    /**
     * Sidebar'ı programatik olarak daralt — örneğin sidebar dışına bir yönelme sonrasında.
     */
    fun collapse() {
        hasFocus = false
    }
}

// ── Animasyon değerleri (Compose animasyon API'si içeren @Composable scope gerektirir) ──

/**
 * Sidebar'ın animasyonlu genişlik değerini döndürür.
 * Composable context içinde çağrılmalıdır.
 */
@Composable
fun TvSidebarState.animatedWidth(): Dp {
    val target = if (isExpanded) 220.dp else 72.dp
    val value by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 300),
        label = "sidebarWidth"
    )
    return value
}

/**
 * Sidebar etiket alpha animasyonu (0f → 1f).
 * Composable context içinde çağrılmalıdır.
 */
@Composable
fun TvSidebarState.animatedLabelAlpha(): Float {
    val value by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "sidebarLabelAlpha"
    )
    return value
}

/**
 * Sidebar genişleme progress (0f → 1f).
 * Composable context içinde çağrılmalıdır.
 */
@Composable
fun TvSidebarState.animatedExpandProgress(): Float {
    val value by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "sidebarExpandProgress"
    )
    return value
}

/**
 * [TvSidebarState]'i remember ile oluşturur.
 */
@Composable
fun rememberTvSidebarState(
    compactWidth: Dp = 72.dp,
    expandedWidth: Dp = 220.dp,
    animDurationMs: Int = 300
): TvSidebarState = remember {
    TvSidebarState(compactWidth, expandedWidth, animDurationMs)
}
