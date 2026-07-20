package com.kitsugi.animelist.ui.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.ui.screens.detail.extractYouTubeId
import kotlinx.coroutines.delay

sealed interface TvHeroState {
    data class Static(val backdropUrl: String?) : TvHeroState
    data class LoadingTrailer(val backdropUrl: String?, val videoId: String) : TvHeroState
    data class PlayingTrailer(val backdropUrl: String?, val videoId: String) : TvHeroState
}

class TvHeroStateHolder(
    initialState: TvHeroState,
    private val onStateChange: (TvHeroState) -> Unit
) {
    val state = initialState

    fun setPlaying(videoId: String, backdropUrl: String?) {
        onStateChange(TvHeroState.PlayingTrailer(backdropUrl, videoId))
    }

    fun setStatic(backdropUrl: String?) {
        onStateChange(TvHeroState.Static(backdropUrl))
    }
}

@Composable
fun rememberTvHeroState(
    backdropUrl: String?,
    trailerUrl: String?,
    settleDelayMs: Long = 2000L
): TvHeroStateHolder {
    var state by remember(backdropUrl, trailerUrl) {
        mutableStateOf<TvHeroState>(TvHeroState.Static(backdropUrl))
    }

    LaunchedEffect(backdropUrl, trailerUrl) {
        if (trailerUrl.isNullOrBlank()) {
            state = TvHeroState.Static(backdropUrl)
            return@LaunchedEffect
        }
        val videoId = extractYouTubeId(trailerUrl)
        if (videoId.isNullOrBlank()) {
            state = TvHeroState.Static(backdropUrl)
            return@LaunchedEffect
        }

        // Wait to settle before showing loading/trailer
        state = TvHeroState.Static(backdropUrl)
        delay(settleDelayMs)
        
        state = TvHeroState.LoadingTrailer(backdropUrl, videoId)
    }

    return remember(state) {
        TvHeroStateHolder(state) { newState ->
            state = newState
        }
    }
}
