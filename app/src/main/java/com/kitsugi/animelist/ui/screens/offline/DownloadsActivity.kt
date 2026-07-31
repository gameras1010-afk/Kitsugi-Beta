package com.kitsugi.animelist.ui.screens.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kitsugi.animelist.ui.screens.offline.DownloadsScreen
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme
import com.kitsugi.animelist.DeviceFormFactor
import com.kitsugi.animelist.DeviceProfile

/**
 * Standalone activity for the Downloads screen.
 * Launched from KitsugiStreamActivity/Screen when a download starts so the user
 * can view progress without leaving the stream picker flow, and go back to it.
 */
class DownloadsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isTv = DeviceProfile.detect(this) == DeviceFormFactor.TV
        setContent {
            KitsugiAnimeListTheme(isTv = isTv) {
                DownloadsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
