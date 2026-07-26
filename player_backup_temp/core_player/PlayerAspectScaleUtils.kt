package com.kitsugi.animelist.core.player

import androidx.media3.ui.AspectRatioFrameLayout

object PlayerAspectScaleUtils {
    fun getMedia3ResizeMode(mode: PlayerAspectMode): Int {
        return when (mode) {
            PlayerAspectMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlayerAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlayerAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            PlayerAspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            PlayerAspectMode.CROP_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            PlayerAspectMode.CROP_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    fun getMedia3AspectRatioOverride(mode: PlayerAspectMode): Float {
        return when (mode) {
            PlayerAspectMode.CROP_16_9 -> 16f / 9f
            PlayerAspectMode.CROP_4_3 -> 4f / 3f
            else -> -1f
        }
    }

    fun getMpvAspectProperty(mode: PlayerAspectMode): String {
        return when (mode) {
            PlayerAspectMode.ORIGINAL -> "no"
            PlayerAspectMode.FIT -> "no"
            PlayerAspectMode.FILL -> "yes"
            PlayerAspectMode.ZOOM -> "no"
            PlayerAspectMode.CROP_16_9 -> "16:9"
            PlayerAspectMode.CROP_4_3 -> "4:3"
        }
    }
}
