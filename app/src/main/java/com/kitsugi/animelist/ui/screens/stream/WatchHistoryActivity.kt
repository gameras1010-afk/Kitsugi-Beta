package com.kitsugi.animelist.ui.screens.stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kitsugi.animelist.ui.screens.history.WatchHistoryScreen
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme
import com.kitsugi.animelist.DeviceFormFactor
import com.kitsugi.animelist.DeviceProfile

/**
 * Standalone activity for the Watch History screen.
 * Launched from KitsugiStreamActivity so the user can view history without
 * leaving the stream picker flow.
 */
class WatchHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isTv = DeviceProfile.detect(this) == DeviceFormFactor.TV
        setContent {
            KitsugiAnimeListTheme(isTv = isTv) {
                WatchHistoryScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
