package com.kitsugi.animelist

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.kitsugi.animelist.core.deeplink.DeepLinkHandler
import com.kitsugi.animelist.core.recommendations.TvChannelSyncService
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme
import com.kitsugi.animelist.ui.tv.TvRootScreen
import com.kitsugi.animelist.ui.tv.design.KitsugiTvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tvChannelSyncService: TvChannelSyncService

    private val formFactor by lazy { DeviceProfile.detect(this) }

    override fun attachBaseContext(newBase: Context) {
        val tag = LocaleCache.localeTag.takeIf { it != LocaleCache.UNSET }

        if (!tag.isNullOrEmpty()) {
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KitsugiAnimeList)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsDataStore = remember { com.kitsugi.animelist.data.settings.SettingsDataStore(applicationContext) }
            val appSettings by settingsDataStore.settingsFlow.collectAsState(initial = com.kitsugi.animelist.data.settings.AppSettings())

            val darkTheme = when (appSettings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            KitsugiAnimeListTheme(
                darkTheme = darkTheme,
                amoledBlack = appSettings.amoledBlack,
                selectedThemeId = appSettings.selectedThemeId,
                customAccentColor = appSettings.customAccentColor,
                isTv = formFactor == DeviceFormFactor.TV
            ) {
                when (formFactor) {
                    DeviceFormFactor.TV -> {
                        KitsugiTvTheme {
                            TvRootScreen()
                        }
                    }
                    DeviceFormFactor.TABLET,
                    DeviceFormFactor.PHONE -> AppRoot()
                }
            }
        }

        handleAuthIntent(intent)
        // B1.1: Non-auth deep links (Channels / typed links) park as pending.
        // TvRootScreen drains them once navState and session are ready.
        handleDeepLinkIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (formFactor == DeviceFormFactor.TV) {
            // B1.11: Servisi baslatir (ilk cagirida periyodik job planlanir).
            // Sonraki cagirilar no-op'tur (job zaten planlanmis).
            tvChannelSyncService.start()
            tvChannelSyncService.onForegroundChanged(foreground = true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (formFactor == DeviceFormFactor.TV) {
            com.kitsugi.animelist.core.player.TvTrailerPlayerPoolHolder.get(this).yield()
            // B1.11: Arka plana geciste launcher icin son reconcile yapilir
            tvChannelSyncService.onForegroundChanged(foreground = false)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return try {
            super.dispatchKeyEvent(event)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("FocusRequester is not initialized", ignoreCase = true) == true) {
                android.util.Log.w("MainActivity", "Bypassed uninitialized FocusRequester crash during key event dispatch", e)
                true
            } else {
                throw e
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
        // B1.1: Warm-start channel/deep link
        handleDeepLinkIntent(intent)
    }

    // B1.1: Routes versioned deep links (Kitsugianimelist://v1/*) to DeepLinkHandler.
    // Auth links are handled by handleAuthIntent; this returns true for them (ignored).
    // NavState not available at Activity level; parked as pending; TvRootScreen drains.
    private fun handleDeepLinkIntent(intent: Intent?) {
        DeepLinkHandler.handle(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        ExternalAuthManager.handleAuthIntent(
            context = this,
            intent = intent,
            onSuccess = { serviceName ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        when (serviceName) {
                            "anilist" -> "AniList bağlantısı başarılı."
                            "simkl"   -> "Simkl bağlantısı başarılı."
                            else      -> "MyAnimeList bağlantısı başarılı."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}