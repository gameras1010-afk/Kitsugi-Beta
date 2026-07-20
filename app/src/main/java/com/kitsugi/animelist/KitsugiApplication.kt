package com.kitsugi.animelist

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.size.Precision
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okio.Path.Companion.toOkioPath
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KitsugiApplication : Application(), SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        @Volatile
        var activeActivity: android.app.Activity? = null

        @Volatile
        private var instance: KitsugiApplication? = null

        fun getInstance(): KitsugiApplication? = instance

        private var dynamicContextWrapper: KitsugiDynamicContextWrapper? = null

        fun getDynamicContext(context: Context): Context {
            var wrapper = dynamicContextWrapper
            if (wrapper == null) {
                wrapper = KitsugiDynamicContextWrapper(context.applicationContext)
                dynamicContextWrapper = wrapper
            }
            return wrapper
        }
    }

    override fun onCreate() {
        instance = this
        super.onCreate()

        // Initialize custom FileLoggingTree
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.init(this)

        // Catch and log uncaught exceptions so they are recorded in crash_log.txt and logcat
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTraceStr = android.util.Log.getStackTraceString(throwable)

            // ── Plugin pencere hataları: process'i öldürme ───────────────────────────
            // BotKontrol ve diğer Kraptor/Byayzen plugin'leri captcha dialog açarken
            // geçersiz token ile Dialog.show() çağırabilir → BadTokenException.
            // Kotlin coroutine Dispatchers.Main üzerinde exception, DispatchedTask.run()
            // içinde yakalanarak bu handler'a iletilir. Handler RETURN ederse (killProcess
            // çağırmadan) main Looper normal çalışmaya devam eder — uygulama ölmez.
            val isBadToken = generateSequence(throwable as Throwable?) { it.cause }
                .any {
                    it is android.view.WindowManager.BadTokenException ||
                    it.javaClass.name.contains("BadTokenException")
                }
            val isProxyCast = throwable is ClassCastException &&
                throwable.message?.contains("Proxy") == true
            val isFromPlugin = stackTraceStr.contains("BotKontrol") ||
                stackTraceStr.contains("com.kraptor.") ||
                stackTraceStr.contains("com.byayzen.") ||
                stackTraceStr.contains("com.kerimmkirac.") ||
                stackTraceStr.contains("com.nikyokki.") ||
                stackTraceStr.contains("com.lagradost.cloudstream3.")

            if ((isBadToken || isProxyCast) && isFromPlugin) {
                android.util.Log.e("KitsugiApplication",
                    "Plugin pencere hatası bastırıldı — process devam ediyor: " +
                    "${throwable.javaClass.simpleName}: ${throwable.message}")
                return@setDefaultUncaughtExceptionHandler   // main Looper continues
            }
            // ─────────────────────────────────────────────────────────────────────────

            val crashReport = buildString {
                append("Thread: ${thread.name}\n")
                append("Zaman: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                append("Cihaz Markası: ${android.os.Build.BRAND}\n")
                append("Cihaz Modeli: ${android.os.Build.MODEL}\n")
                append("Android Sürümü: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
                append("Hata: ${throwable.javaClass.name}: ${throwable.message}\n")
                append("Stacktrace:\n$stackTraceStr")
            }

            try {
                val file = java.io.File(filesDir, "crash_log.txt")
                file.writeText(crashReport)
            } catch (_: Exception) {}

            android.util.Log.e("KitsugiApplication", "KRİTİK HATA - Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)

            try {
                val intent = android.content.Intent(this@KitsugiApplication, com.kitsugi.animelist.ui.screens.crash.KitsugiCrashActivity::class.java).apply {
                    putExtra("crash_report", crashReport)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Hata ekranı başlatılamadı", e)
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(10)
        }

        // Start background logcat redirection to app_logs.txt only in debug mode to save IO performance
        if (BuildConfig.DEBUG) {
            try {
                val logFile = java.io.File(filesDir, "app_logs.txt")
                // Run logcat redirection: max 2MB per file, keep 2 rotated backups
                Runtime.getRuntime().exec(arrayOf(
                    "logcat",
                    "-f", logFile.absolutePath,
                    "-r", "2048",
                    "-n", "2",
                    "-v", "time",
                    "*:D"
                ))
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Failed to start logcat file redirection: ${e.message}")
            }
        }

        // Initialize ratings repository room cache context
        KitsugiEpisodeRatingsRepository.init(this)

        // T4-11: Initialize DNS Over HTTPS resolver without blocking main thread.
        // DnsManager starts with default=0 immediately; the real persisted value is applied
        // asynchronously once the DataStore flow emits its first value.
        com.kitsugi.animelist.core.network.DnsManager.init(this, 0)
        applicationScope.launch {
            try {
                val dataStore = com.kitsugi.animelist.data.settings.SettingsDataStore(this@KitsugiApplication)
                val savedDns = dataStore.settingsFlow.first().dnsChoice
                if (savedDns != 0) {
                    com.kitsugi.animelist.core.network.DnsManager.init(this@KitsugiApplication, savedDns)
                }
            } catch (e: Exception) {
                android.util.Log.w("KitsugiApplication", "DNS init async failed: ${e.message}")
            }
        }

        // T3.3: Yayın takvimi bildirim kanalını oluştur (API 26+ için gerekli)
        com.kitsugi.animelist.core.notifications.KitsugiAiringNotificationScheduler.createNotificationChannel(this)
        // T3.3: Ayarlar açıksa yayın alarmlarını planla (AlarmManager tabanlı)
        com.kitsugi.animelist.core.notifications.AiringNotificationWorker.scheduleIfEnabled(this)

        // Initialize Cloudstream runtime singleton context and client
        com.kitsugi.animelist.data.cloudstream.CsRuntimeInit.init(this)

        try {
            uy.kohesive.injekt.Injekt.addSingleton(eu.kanade.tachiyomi.network.NetworkHelper::class.java, eu.kanade.tachiyomi.network.NetworkHelper(this))
            uy.kohesive.injekt.Injekt.addSingleton(kotlinx.serialization.json.Json::class.java, kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
            android.util.Log.d("KitsugiApplication", "NetworkHelper and Json registered in Injekt.")
        } catch (e: Exception) {
            android.util.Log.w("KitsugiApplication", "Injekt registration skipped: ${e.message}")
        }

        // Track active activity to provide correct Window Token context for plugin dialogs (e.g. Captchas)
        // Handler to delay-null activeActivity (gives plugin dialogs time to close)
        val activityNullHandler = android.os.Handler(android.os.Looper.getMainLooper())
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                // Cancel any pending null — new activity is becoming visible
                activityNullHandler.removeCallbacksAndMessages(null)
                com.lagradost.api.setContext(java.lang.ref.WeakReference(getDynamicContext(activity)))
            }
            override fun onActivityResumed(activity: android.app.Activity) {
                activityNullHandler.removeCallbacksAndMessages(null)
                activeActivity = activity
                com.lagradost.api.setContext(java.lang.ref.WeakReference(getDynamicContext(activity)))
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                // DO NOT null activeActivity here! A paused activity still has a valid window
                // token. Nulling here causes BotKontrol (and any plugin dialog) to crash with
                // BadTokenException: "Unable to add window -- token null is not valid".
                // We keep the reference alive so plugin dialogs (captcha, etc.) can still show.
            }
            override fun onActivityStopped(activity: android.app.Activity) {
                // Delay null by 2s — gives async plugin captcha dialogs time to finish
                if (activeActivity === activity) {
                    activityNullHandler.postDelayed({
                        if (activeActivity === activity) {
                            activeActivity = null
                        }
                    }, 2000L)
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {
                activityNullHandler.removeCallbacksAndMessages(null)
                if (activeActivity === activity) {
                    activeActivity = null
                }
            }
        })

        // Seed default Stremio addons if database is empty
        applicationScope.launch {
            AddonStreamRepository(this@KitsugiApplication).seedPresetsIfEmpty()
        }

        // Pre-load all enabled Cloudstream plugins sequentially on startup.
        // Delayed 5s so app UI is fully ready and the main thread is not blocked
        // (fixes the 527ms BIND_APPLICATION latency seen in PerfMonitor logs).
        applicationScope.launch {
            kotlinx.coroutines.delay(5_000L)
            try {
                val db = KitsugiDatabase.getDatabase(this@KitsugiApplication)
                val enabledPlugins = db.csPluginDao().getEnabledPlugins()
                android.util.Log.d("KitsugiApplication", "Pre-loading ${enabledPlugins.size} enabled CS plugin(s) sequentially...")
                for (plugin in enabledPlugins) {
                    try {
                        CsPluginLoader.loadExtension(this@KitsugiApplication, plugin.id)
                        android.util.Log.d("KitsugiApplication", "Pre-loaded plugin: ${plugin.id}")
                    } catch (e: Exception) {
                        android.util.Log.w("KitsugiApplication", "Pre-load failed for ${plugin.id}: ${e.message}")
                    }
                }
                android.util.Log.d("KitsugiApplication", "Plugin pre-loading complete.")
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Plugin pre-loading error: ${e.message}", e)
            }
        }

        // Pre-load manga extensions from manga_extensions/ on startup (7s delay like CS plugins)
        applicationScope.launch {
            kotlinx.coroutines.delay(7_000L)
            try {
                com.kitsugi.animelist.data.manga.MangaExtensionLoader.loadAllExtensions(this@KitsugiApplication)
                android.util.Log.d("KitsugiApplication", "Manga eklentileri tarama tamamlandı.")

                // Kotatsu-Redo: tüm 1300+ built-in kaynağı yükle (dil filtresi: TR varsayılan)
                try {
                    com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.initialize(this@KitsugiApplication)
                    android.util.Log.i("KitsugiApplication",
                        "Kotatsu init: ${com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getSourceCount()} kaynak yüklendi.")
                } catch (e: Exception) {
                    android.util.Log.e("KitsugiApplication", "Kotatsu init hatası: ${e.message}", e)
                }

                // Dinamik manga domain kataloğunu internetten çek ve yerel ayarları güncelle
                try {
                    com.kitsugi.animelist.data.manga.MangaCatalogManager.syncCatalog(this@KitsugiApplication)
                } catch (e: Exception) {
                    android.util.Log.e("KitsugiApplication", "Manga katalog güncelleme hatası: ${e.message}", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Manga eklenti tarama hatası: ${e.message}", e)
            }

            // Eklentiler yüklendikten 5 saniye sonra Keiyoushi repo'sundan otomatik
            // güncelleme kontrolü yap (günde en fazla 1 kez çalışır).
            kotlinx.coroutines.delay(5_000L)
            try {
                val result = com.kitsugi.animelist.data.manga.MangaExtensionAutoUpdater.runIfNeeded(
                    context = this@KitsugiApplication,
                    forceCheck = false
                )
                when (result) {
                    is com.kitsugi.animelist.data.manga.MangaExtensionAutoUpdater.UpdateResult.Success ->
                        android.util.Log.i("KitsugiApplication",
                            "Manga oto-güncelleme: ${result.updated} güncellendi, ${result.checked} kontrol edildi.")
                    is com.kitsugi.animelist.data.manga.MangaExtensionAutoUpdater.UpdateResult.Skipped ->
                        android.util.Log.d("KitsugiApplication", "Manga oto-güncelleme atlandı: ${result.reason}")
                    is com.kitsugi.animelist.data.manga.MangaExtensionAutoUpdater.UpdateResult.Failed ->
                        android.util.Log.w("KitsugiApplication", "Manga oto-güncelleme başarısız: ${result.reason}")
                }
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Manga oto-güncelleme hatası: ${e.message}", e)
            }

            // Kotatsu-Redo kaynaklarını arka planda sessizce güncelle (12 saatte bir)
            kotlinx.coroutines.delay(4_000L)
            try {
                val kotatsuCount = com.kitsugi.animelist.data.manga.MangaCatalogManager.syncKotatsuSources(
                    context = this@KitsugiApplication,
                    forceCheck = false
                )
                android.util.Log.i("KitsugiApplication", "Kotatsu-Redo kaynakları güncellendi: $kotatsuCount kaynak yüklendi.")
            } catch (e: Exception) {
                android.util.Log.e("KitsugiApplication", "Kotatsu sync hatası: ${e.message}", e)
            }
        }

        // Load locale synchronously so it's available before Activity.attachBaseContext.
        val tag = getSharedPreferences("app_locale", Context.MODE_PRIVATE)
            .getString("locale_tag", null)
        LocaleCache.localeTag = tag ?: ""
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                }
                add(OkHttpNetworkFetcherFactory(callFactory = { com.kitsugi.animelist.core.network.KitsugiHttpClient.client }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)  // R2: %33 → %25 (manga RGB_565 için yeterli)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("nuvio_images").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)  // R2: 200MB → 512MB
                    .build()
            }
            .crossfade(false)
            .allowHardware(true)
            .allowRgb565(true)           // manga için %50 bellek tasarrufu
            .precision(Precision.INEXACT)
            .build()
    }
}

class KitsugiDynamicContextWrapper(appContext: Context) : android.content.ContextWrapper(appContext) {
    override fun getPackageName(): String {
        return "com.lagradost.cloudstream3"
    }

    override fun getSystemService(name: String): Any? {
        // Delegate to live Activity's service where possible so dialogs get valid window tokens.
        // NO proxy wrapping — Android internals cast WindowManager to WindowManagerImpl (concrete
        // class) inside Window.setWindowManager(), an interface proxy causes ClassCastException.
        val activity = KitsugiApplication.activeActivity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            try {
                val service = activity.getSystemService(name)
                if (service != null) return service
            } catch (_: Exception) {}
        }
        return super.getSystemService(name)
    }

    override fun getTheme(): android.content.res.Resources.Theme {
        return KitsugiApplication.activeActivity?.theme ?: super.getTheme()
    }

    override fun getResources(): android.content.res.Resources {
        return KitsugiApplication.activeActivity?.resources ?: super.getResources()
    }

    override fun getAssets(): android.content.res.AssetManager {
        return KitsugiApplication.activeActivity?.assets ?: super.getAssets()
    }
}

object LocaleCache {
    const val UNSET = "__UNSET__"

    @Volatile
    var localeTag: String = UNSET

    fun updateLocale(context: Context, tag: String) {
        val prefsTag = if (tag == "system") "" else tag
        context.getSharedPreferences("app_locale", Context.MODE_PRIVATE)
            .edit()
            .putString("locale_tag", prefsTag)
            .apply()
        localeTag = prefsTag

        // Apply immediately to resources
        val locale = if (tag == "system") java.util.Locale.getDefault() else java.util.Locale.forLanguageTag(tag)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Find activity and recreate
        findActivity(context)?.recreate()
    }

    private fun findActivity(context: Context): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }
}

