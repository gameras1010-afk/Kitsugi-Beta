package com.kitsugi.animelist

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel

/**
 * KitsugiSplashActivity — WebView tabanlı açılış ekranı
 *
 * assets/kitsugi_splash.html içindeki neon torii animasyonunu
 * tam ekran WebView'da oynatır. Animasyon bitince MainActivity'ye
 * crossfade ile geçer.
 *
 * Ayarlar:
 *  - splashAnimationEnabled: false ise animasyon atlanır, direkt MainActivity açılır
 *  - splashSoundEnabled:     true ise raw/kitsugi_splash_sound.wav çalınır
 *
 * Güvenlik: WebView sadece local assets yükler, internet erişimi kapalı.
 */
@SuppressLint("CustomSplashScreen")
class KitsugiSplashActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var transitionStarted = false
    private var mediaPlayer: MediaPlayer? = null

    /** JavaScript → Kotlin köprüsü */
    inner class SplashInterface {
        @JavascriptInterface
        fun onAnimationComplete() {
            runOnUiThread { navigateToMain() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Prefetch Discovery data in parallel immediately at startup
        ExploreViewModel.prefetch(applicationContext)

        super.onCreate(savedInstanceState)

        // Tam ekran — status bar ve navigation bar gizli
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        // Ayarları DataStore'dan oku, sonra UI'yı başlat
        // lifecycleScope.launch zaten Main thread'de çalışır — withContext gereksiz
        lifecycleScope.launch {
            val dataStore = SettingsDataStore(applicationContext)
            val settings = dataStore.settingsFlow.firstOrDefault()

            if (settings.splashAnimationEnabled) {
                startAnimatedSplash(settings)
            } else {
                // Animasyon kapalı → direkt geç
                navigateToMain()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Splash başlatma
    // ─────────────────────────────────────────────────────────────────────────

    private fun startAnimatedSplash(settings: AppSettings) {
        // WebView oluştur
        webView = WebView(this).apply {
            setBackgroundColor(0xFF050510.toInt())
        }
        setContentView(webView)
        setupWebView()
        loadSplash()

        // Açılış sesini çal (eğer aktifse)
        if (settings.splashSoundEnabled) {
            playSplashSound()
        }

        // Fallback: HTML sinyali gelmezse 5 saniye sonra geç
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMain()
        }, 5000L)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ses
    // ─────────────────────────────────────────────────────────────────────────

    private fun playSplashSound() {
        try {
            val resId = resources.getIdentifier("kitsugi_splash_sound", "raw", packageName)
            if (resId == 0) return  // ses dosyası yoksa sessiz devam et
            mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                setVolume(0.85f, 0.85f)
                isLooping = false
                setOnCompletionListener { release() }
                start()
            }
        } catch (_: Exception) {
            // Ses hatası kritik değil — yoksay
        }
    }

    private fun releaseSoundPlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WebView
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = false
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
        }
        webView.addJavascriptInterface(SplashInterface(), "KitsugiSplash")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {}

            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                runOnUiThread { navigateToMain() }
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                runOnUiThread { navigateToMain() }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                runOnUiThread { navigateToMain() }
            }
        }
    }

    private fun loadSplash() {
        webView.loadUrl("file:///android_asset/kitsugi_splash.html")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Geçiş
    // ─────────────────────────────────────────────────────────────────────────

    private fun navigateToMain() {
        if (transitionStarted) return
        transitionStarted = true

        releaseSoundPlayer()

        // Prefetch arka planda devam eder — bitmesini BEKLEME.
        // ExploreViewModel init'i cache'den okur; cache dolmadıysa kendi fetch'ini başlatır.
        val intent = Intent(this@KitsugiSplashActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!transitionStarted) {
            mediaPlayer?.takeIf { !it.isPlaying }?.start()
        }
    }

    override fun onDestroy() {
        releaseSoundPlayer()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Splash sırasında geri tuşu devre dışı
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Coroutine içinden Flow'un ilk değerini al.
 * Maksimum 2 saniye bekler; zaman aşımında varsayılan AppSettings döner.
 */
private suspend fun Flow<AppSettings>.firstOrDefault(): AppSettings {
    return try {
        withTimeout(2_000L) {
            var result: AppSettings? = null
            collect { value ->
                result = value
                throw CancellationException("got first value")
            }
            result ?: AppSettings()
        }
    } catch (_: Exception) {
        AppSettings()
    }
}
