package com.kitsugi.animelist.core.player.engine

import com.kitsugi.animelist.data.settings.AppSettings

object PlayerEngineSelector {
    fun selectEngine(
        settings: AppSettings,
        videoUrl: String,
        isCS: Boolean = false
    ): PlayerEngineType {
        return when (settings.playerPreference.uppercase()) {
            "MPV" -> PlayerEngineType.MPV
            "EXTERNAL" -> PlayerEngineType.EXTERNAL
            else -> PlayerEngineType.MEDIA3
        }
    }
}
