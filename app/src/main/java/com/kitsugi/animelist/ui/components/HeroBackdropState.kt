package com.kitsugi.animelist.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * V2-D02 – HeroBackdropState
 *
 * Hero banner backdrop için durum makinesi.
 * Crossfade, blur fade ve parallax scroll yönetimi.
 * NuvioTV HeroBackdropState.kt referans alındı.
 */

enum class HeroBackdropPhase {
    LOADING,   // İskelet gösteriliyor
    FADEIN,    // İçerik yüklendi, fade-in animasyonu
    VISIBLE,   // Tam görünür
    TRANSITION // Bir içerikten diğerine geçiş
}

@Stable
class HeroBackdropState {
    var phase by mutableStateOf(HeroBackdropPhase.LOADING)
        private set

    var currentImageUrl by mutableStateOf<String?>(null)
        private set

    var previousImageUrl by mutableStateOf<String?>(null)
        private set

    var alpha by mutableStateOf(0f)
        private set

    var blurRadius by mutableStateOf(0f)
        private set

    fun onImageLoaded(url: String) {
        previousImageUrl = currentImageUrl
        currentImageUrl = url
        phase = HeroBackdropPhase.FADEIN
    }

    fun onFadeInComplete() {
        phase = HeroBackdropPhase.VISIBLE
        previousImageUrl = null
        alpha = 1f
        blurRadius = 0f
    }

    fun onFocusGained() {
        phase = HeroBackdropPhase.VISIBLE
        blurRadius = 0f
    }

    fun onFocusLost() {
        blurRadius = 16f
    }

    fun onTransitionStart(newUrl: String) {
        previousImageUrl = currentImageUrl
        currentImageUrl = newUrl
        phase = HeroBackdropPhase.TRANSITION
    }

    fun reset() {
        phase = HeroBackdropPhase.LOADING
        currentImageUrl = null
        previousImageUrl = null
        alpha = 0f
        blurRadius = 0f
    }
}

@Composable
fun rememberHeroBackdropState(): HeroBackdropState {
    return remember { HeroBackdropState() }
}

/**
 * AlwaysCrossfadeTransitionFactory yerine kullanılan yardımcı –
 * her içerik değişiminde crossfade animasyonu uygular.
 */
@Composable
fun <T> CrossfadeContent(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 400, easing = FastOutSlowInEasing),
    content: @Composable (T) -> Unit
) {
    val transition = updateTransition(targetState = targetState, label = "crossfade")
    val alpha by transition.animateFloat(
        transitionSpec = { animationSpec },
        label = "contentAlpha"
    ) { 1f }

    Box(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        content(targetState)
    }
}
