package com.kitsugi.animelist.data.cloudstream

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.extractorApis
import dalvik.system.PathClassLoader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CsPluginLoader {

    private const val TAG = "CsPluginLoader"

    /**
     * Ayrı teşhis tag'i — tüm CS3 eklenti yükleme/hata olaylarını ERROR seviyesinde loglar.
     * Logcat'te izole görmek için: adb logcat -s PLUGIN_DIAG
     * Tüm CS3 logları için: adb logcat | findstr "PLUGIN_DIAG\|CS_ERROR\|CS_LOAD"
     */
    private const val PLUGIN_DIAG = "PLUGIN_DIAG"

    // Matches BasePlugin.Manifest structure from cloudstream-master
    private data class PluginManifest(
        @SerializedName("pluginClassName") val pluginClassName: String?,
        @SerializedName("pluginClass")     val pluginClass: String?,
        @SerializedName("internalName")    val internalName: String?,
        @SerializedName("name")            val name: String?,
        @SerializedName("version")         val version: Int? = null,
        @SerializedName("requiresResources") val requiresResources: Boolean = false
    )

    val loadedPlugins = java.util.concurrent.ConcurrentHashMap<String, BasePlugin>()
    val loadedPluginIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    private val loadMutex = Mutex()

    // ─── Public settings helpers ──────────────────────────────────────────────

    /**
     * Returns the loaded [Plugin] instance for the given scraperId, or null if the
     * plugin is not loaded or does not extend [Plugin] (only BasePlugin).
     * The [Context] is needed to resolve the .cs3 file path.
     */
    fun getPluginInstance(scraperId: String, context: Context): Plugin? {
        val cs3File = File(context.filesDir, "cs_extensions/$scraperId.cs3")
        return loadedPlugins[cs3File.absolutePath] as? Plugin
    }

    /**
     * Returns true if the plugin is loaded AND has registered an [openSettings] callback.
     * Use this to decide whether to show the ⚙️ settings button in the UI.
     */
    fun hasSettings(scraperId: String, context: Context): Boolean {
        return getPluginInstance(scraperId, context)?.openSettings != null
    }

    private fun unloadExtensionInternal(context: Context, scraperId: String) {
        val cs3File = File(context.filesDir, "cs_extensions/$scraperId.cs3")
        val absolutePath = cs3File.absolutePath
        Log.i(TAG, "Unloading extension: $scraperId ($absolutePath)")

        // 1. Call beforeUnload() on the plugin instance
        val plugin = loadedPlugins[absolutePath]
        if (plugin != null) {
            try {
                plugin.beforeUnload()
                Log.d(TAG, "Called beforeUnload() on plugin instance for $scraperId")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to run beforeUnload for $scraperId: ${e.message}", e)
            }
            loadedPlugins.remove(absolutePath)
        }
        loadedPluginIds.value = loadedPluginIds.value - scraperId

        // 2. Remove all registered APIs thread-safely
        try {
            val prevApis = APIHolder.allProviders.filter { it.sourcePlugin == absolutePath }
            prevApis.forEach { api ->
                try {
                    APIHolder.removePluginMapping(api)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove plugin mapping for API: ${api.name}", e)
                }
            }
            APIHolder.allProviders.removeAll { it.sourcePlugin == absolutePath }
            Log.d(TAG, "Cleared API providers for $scraperId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean APIHolder providers for $scraperId: ${e.message}", e)
        }

        // 3. Clear orphaned extractorApis thread-safely
        try {
            extractorApis.removeAll { it.sourcePlugin == absolutePath }
            Log.d(TAG, "Cleared orphaned extractorApis for $scraperId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean extractorApis for $scraperId: ${e.message}")
        }

        // 4. Clear VideoClickActions thread-safely
        try {
            com.lagradost.cloudstream3.actions.VideoClickActionHolder.allVideoClickActions.removeAll { action ->
                action.sourcePlugin == absolutePath
            }
            Log.d(TAG, "Cleared VideoClickActions for $scraperId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean VideoClickActions for $scraperId: ${e.message}")
        }
    }

    /**
     * Unloads a loaded extension fully, clearing its APIs, extractors, and actions
     * to prevent memory leaks and duplicate registrations.
     */
    suspend fun unloadExtension(context: Context, scraperId: String) = loadMutex.withLock {
        unloadExtensionInternal(context, scraperId)
    }

    private fun File.sha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        this.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ─── Download ─────────────────────────────────────────────────────────────

    fun downloadExtension(context: Context, scraperId: String, urlString: String, expectedHash: String? = null): Boolean {
        val normalizedUrl = com.kitsugi.animelist.utils.CloudstreamUrlHelper.normalizeUrl(urlString)
        val extensionDir = File(context.filesDir, "cs_extensions")
        if (!extensionDir.exists()) extensionDir.mkdirs()

        val tempFile = File(extensionDir, "$scraperId.cs3.tmp")
        val targetFile = File(extensionDir, "$scraperId.cs3")

        return try {
            if (tempFile.exists()) tempFile.delete()

            Log.d(TAG, "Downloading plugin $scraperId from $normalizedUrl to temp file ${tempFile.name}")
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "CloudStream/3")
                .build()

            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("Failed to download extension: ${response.code}")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Log.d(TAG, "Downloaded ${tempFile.length()} bytes to temp file")

            // Verify SHA-256 hash if expected
            if (expectedHash != null) {
                val calculatedHash = tempFile.sha256()
                val cleanExpected = expectedHash.trim().removePrefix("sha256-").removePrefix("SHA256-")
                if (!calculatedHash.equals(cleanExpected, ignoreCase = true)) {
                    Log.e(TAG, "Hash verification failed for $scraperId. Expected: $expectedHash, got: $calculatedHash")
                    tempFile.delete()
                    return false
                }
                Log.d(TAG, "Hash verification passed for $scraperId")
            }

            // Verify it is a valid ZIP (cs3 = ZIP-wrapped DEX)
            val isValidZip = try { ZipFile(tempFile).use { true } } catch (_: Exception) { false }
            if (!isValidZip) {
                Log.e(TAG, "Downloaded file is NOT a valid ZIP/CS3: $scraperId — aborting")
                tempFile.delete()
                return false
            }

            // Atomic move to target
            if (targetFile.exists()) targetFile.delete()
            val renameSuccess = tempFile.renameTo(targetFile)
            if (!renameSuccess) {
                Log.e(TAG, "Failed to rename temp file to target file: $scraperId")
                tempFile.delete()
                return false
            }

            // MUST be read-only for Android 10+ DEX security policy
            targetFile.setReadOnly()
            Log.d(TAG, "Plugin $scraperId downloaded, verified, and installed atomicaly OK")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download plugin $scraperId from $normalizedUrl", e)
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    /**
     * Mirrors the exact approach used in Cloudstream's PluginManager.loadPlugin():
     *  1. Use PathClassLoader (not DexClassLoader) with context.classLoader as parent
     *     so that host-app classes (Plugin, BasePlugin, MainAPI, etc.) are resolvable.
     *  2. Read manifest.json via loader.getResourceAsStream() — same as original.
     *  3. Instantiate the plugin class and call load(context) or load().
     */
    suspend fun loadExtension(context: Context, scraperId: String, forceReload: Boolean = false): List<MainAPI> = loadMutex.withLock {
        val cs3File = File(context.filesDir, "cs_extensions/$scraperId.cs3")
        val absolutePath = cs3File.absolutePath

        if (!cs3File.exists()) {
            Log.e(TAG, "Plugin file not found: ${cs3File.absolutePath}")
            Log.e(PLUGIN_DIAG, "❌ CS3 DOSYA YOK: $scraperId → ${cs3File.absolutePath}")
            return@withLock emptyList()
        }

        if (!forceReload) {
            val alreadyLoaded = loadedPlugins.containsKey(absolutePath)
            if (alreadyLoaded) {
                val registeredApis = APIHolder.allProviders.filter {
                    it.sourcePlugin == absolutePath
                }
                if (registeredApis.isNotEmpty()) {
                    Log.d(TAG, "Plugin $scraperId is already loaded. Returning ${registeredApis.size} cached API provider(s).")
                    if (!loadedPluginIds.value.contains(scraperId)) {
                        loadedPluginIds.value = loadedPluginIds.value + scraperId
                    }

                    // ── Re-apply patches even on cache hit ──────────────────
                    // isAllowedVersion and RequestBlocker patches must be re-applied because
                    // the patch targets static/object-level state in the plugin's DEX which
                    // can be reset between app sessions or coroutine contexts.
                    val cachedPlugin = loadedPlugins[absolutePath]
                    val pluginLoader = cachedPlugin?.javaClass?.classLoader
                    if (pluginLoader != null) {
                        applyHelperPatches(pluginLoader, scraperId, cachedPlugin?.javaClass?.name ?: "")
                    }

                    return@withLock registeredApis
                }
            }
        }

        Log.d(TAG, "Loading extension $scraperId (${cs3File.length()} bytes)")
        Log.i(PLUGIN_DIAG, "⏳ CS3 YÜKLENİYOR: $scraperId (${cs3File.length()} bytes)")

        // Quick ZIP sanity-check before handing to classloader
        val isValidZip = try { ZipFile(cs3File).use { true } } catch (zipEx: Exception) {
            Log.e(PLUGIN_DIAG, "❌ CS3 ZIP BOZUK: $scraperId — ${zipEx.message}")
            false
        }
        if (!isValidZip) {
            Log.e(TAG, "CS3 file for $scraperId is not a valid ZIP — corrupt or not a DEX plugin. Re-install it.")
            Log.e(PLUGIN_DIAG, "❌ CS3 GEÇERSİZ ZIP: $scraperId — Yeniden yükleyin!")
            return@withLock emptyList()
        }

        // Android 10+ requirement: file must be read-only before PathClassLoader opens it
        if (cs3File.canWrite()) {
            cs3File.setReadOnly()
        }

        try {
            // ── Step 1: PathClassLoader with host classLoader as parent ──────
            // This is the key: plugin's DEX can reference any class that lives in
            // our app (Plugin, BasePlugin, MainAPI…) because the parent loader
            // has them all.
            val loader = PathClassLoader(cs3File.absolutePath, context.classLoader)

            // ── Step 2: Full cleanup — clear previously registered APIs, extractors, and actions ──
            // Note: unloadExtension already handles extractorApis cleanup internally
            unloadExtensionInternal(context, scraperId)

            // ── Step 3: Read manifest.json from the ZIP ──────────────────────
            val manifest = readManifest(loader, cs3File, scraperId)
                ?: return@withLock emptyList()

            val pluginClassName = manifest.pluginClassName ?: manifest.pluginClass
            if (pluginClassName.isNullOrBlank()) {
                Log.e(TAG, "manifest.json for $scraperId has no pluginClassName or pluginClass field")
                Log.e(PLUGIN_DIAG, "❌ MANIFEST HATASI: $scraperId — pluginClassName/pluginClass alanı eksik!")
                return@withLock emptyList()
            }

            // ── Step 4: Instantiate the plugin class ─────────────────────────
            Log.d(TAG, "Instantiating class: $pluginClassName version=${manifest.version}")
            val pluginClass = loader.loadClass(pluginClassName)
            
            // Override isAllowedVersion + RequestBlocker patches BEFORE instantiating the class.
            // Turkish plugins (like byayzen/kerimmkirac) evaluate version constraints inside
            // static/companion initializer blocks when newInstance() is invoked. If we patch
            // after newInstance(), mainUrl is already cleared/compromised.
            applyHelperPatches(loader, scraperId, pluginClassName)

            val pluginInstance = pluginClass.getDeclaredConstructor().newInstance() as BasePlugin
            pluginInstance.filename = cs3File.absolutePath
            loadedPlugins[cs3File.absolutePath] = pluginInstance

            // ── Step 5: Load dynamic resources if required (requiresResources=true) ──
            // Mirrors PluginManager.loadPlugin() L642-656
            if (manifest.requiresResources) {
                Log.d(TAG, "Loading dynamic resources for $scraperId (requiresResources=true)")
                try {
                    val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
                    val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                    addAssetPath.invoke(assets, cs3File.absolutePath)
                    @Suppress("DEPRECATION")
                    (pluginInstance as? Plugin)?.resources = Resources(
                        assets,
                        context.resources.displayMetrics,
                        context.resources.configuration
                    )
                    Log.d(TAG, "Dynamic resources loaded successfully for $scraperId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load resources for $scraperId: ${e.message}")
                    // Non-fatal: continue loading even if resource injection fails
                }
            }

            // ── Step 6: Call load() — with Context if it's a full Plugin ─────
            // Cloudstream her zaman Activity context geçiyor load()'a.
            // Eklentiler openSettings içinde bu context'i capture ediyor,
            // bu yüzden Activity context şart (Fragment/startActivity için).
            // Timeout + runCatching: plugin'in load() veya asenkron başlattığı
            // kodların (örn. InatBox'ın runBlocking Jackson çağrısı) uygulamayı
            // çökertmesini engeller.
            val loadResult = runCatching {
                kotlinx.coroutines.withTimeout(30_000L) {
                    if (pluginInstance is Plugin) {
                        // ALWAYS use getDynamicContext() — never pass raw Activity.
                        // Raw Activity bypass'es our WindowManagerProxy entirely.
                        // getDynamicContext() wraps WINDOW_SERVICE with our proxy so that
                        // plugin captcha dialogs (e.g. BotKontrol.runCaptchaSolve) get
                        // BadTokenException intercepted instead of crashing the process.
                        val safeCtx = com.kitsugi.animelist.KitsugiApplication.getDynamicContext(
                            com.kitsugi.animelist.KitsugiApplication.activeActivity ?: context
                        )
                        pluginInstance.load(safeCtx)
                    } else {
                        pluginInstance.load()
                    }
                }
            }
            if (loadResult.isFailure) {
                val loadErr = loadResult.exceptionOrNull()
                Log.e(TAG, "Plugin $scraperId load() failed — unloading. Cause: ${loadErr?.javaClass?.simpleName}: ${loadErr?.message}", loadErr)
                Log.e(PLUGIN_DIAG, "❌ CS3 LOAD() HATASI: $scraperId — ${loadErr?.javaClass?.simpleName}: ${loadErr?.message}")
                unloadExtensionInternal(context, scraperId)
                return@withLock emptyList()
            }

            // ── Step 7: Collect registered APIs ─────────────────────────────
            val allRegisteredApis = APIHolder.allProviders.filter {
                it.sourcePlugin == cs3File.absolutePath
            }

            // Fallback Recovery for Anti-Leech / Version Lock compromised mainUrl
            val DEFAULT_PLUGIN_DOMAINS = mapOf(
                "TurkAnime" to "https://www.turkanime.tv",
                "Dizilla" to "https://dizilla.com",
                "DDizi" to "https://www.ddizi.org",
                "DiziPal" to "https://dizipal.co",
                "DiziPalOriginal" to "https://dizipal.co",
                "SezonlukDizi" to "https://sezonlukdizi.org",
                "RecTV" to "https://rectv.co",
                "AnimeciX" to "https://animecix.net",
                "Animeler" to "https://animeler.pw",
                "SinemaCX" to "https://sinemacx.com",
                "SineWix" to "https://sinewix.com",
                "Sinewix" to "https://sinewix.com"
            )
            allRegisteredApis.forEach { api ->
                if (api.mainUrl.isBlank() || api.mainUrl == "/") {
                    val defaultUrl = DEFAULT_PLUGIN_DOMAINS[api.name]
                    if (defaultUrl != null) {
                        api.mainUrl = defaultUrl
                        Log.w(TAG, "[${api.name}] Anti-Leech bypass: Empty mainUrl recovered to: $defaultUrl")
                    }
                }
            }

            Log.d(TAG, "Plugin $scraperId registered ${allRegisteredApis.size} API provider(s): ${allRegisteredApis.map { it.name }}")

            // Log providers that require a WebView/captcha for user/developer awareness,
            // but return all registered providers so they can be queried and verified.
            val KNOWN_WEBVIEW_PLUGINS = setOf(
                "TrAnimeIzle",   // BotKontrol captcha: opens WebView, forgot usesWebView=true
            )
            allRegisteredApis.forEach { api ->
                val blockedByFlag = api.usesWebView
                val blockedByName = KNOWN_WEBVIEW_PLUGINS.any {
                    api.name.contains(it, ignoreCase = true)
                }
                if (blockedByFlag || blockedByName) {
                    val reason = if (blockedByFlag) "usesWebView=true" else "bilinen captcha eklentisi"
                    Log.d(TAG, "  [${api.name}] $reason — WebView doğrulaması gerekebilir")
                }
            }

            if (allRegisteredApis.isEmpty()) {
                Log.w(TAG, "Plugin loaded OK but registered 0 providers. allProviders size=${APIHolder.allProviders.size}")
                Log.e(PLUGIN_DIAG, "⚠️ CS3 SIFIR PROVIDER: $scraperId yüklendi ama 0 API kaydetti! manifest.pluginClass=$pluginClassName")
            } else {
                Log.i(PLUGIN_DIAG, "✅ CS3 BAŞARILI: $scraperId → ${allRegisteredApis.size} provider: ${allRegisteredApis.map { it.name }}")
            }

            loadedPluginIds.value = loadedPluginIds.value + scraperId
            allRegisteredApis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load extension $scraperId: ${e.javaClass.simpleName}: ${e.message}", e)
            Log.e(PLUGIN_DIAG, "❌ CS3 YÜKLEME HATASI: $scraperId — ${e.javaClass.simpleName}: ${e.message}\n${android.util.Log.getStackTraceString(e)}")
            emptyList()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Reads manifest.json and parses it into a PluginManifest object.
     * Tries classloader resource stream first, falls back to direct ZIP reading.
     */
    private fun readManifest(
        loader: PathClassLoader,
        cs3File: File,
        scraperId: String
    ): PluginManifest? {
        // Primary: use classloader resource stream (works for PathClassLoader correctly)
        var manifestJson: String? = null
        try {
            loader.getResourceAsStream("manifest.json")?.use { stream ->
                manifestJson = InputStreamReader(stream).readText()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getResourceAsStream failed for $scraperId, falling back to ZipFile", e)
        }

        // Fallback: read directly from ZIP
        if (manifestJson == null) {
            try {
                manifestJson = ZipFile(cs3File).use { zip ->
                    zip.getEntry("manifest.json")?.let { entry ->
                        zip.getInputStream(entry).reader().use { it.readText() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ZIP fallback also failed for $scraperId", e)
            }
        }

        if (manifestJson == null) {
            Log.e(TAG, "No manifest.json found in $scraperId — cannot determine plugin class")
            return null
        }

        return try {
            Gson().fromJson(manifestJson, PluginManifest::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse manifest.json for $scraperId", e)
            null
        }
    }

    /**
     * Türk anime eklentilerinin `isAllowedVersion` kontrolünü bypass eder ve
     * `RequestBlocker.counter`'ı sıfırlar. Hem ilk yükleme hem de cache-hit
     * yolunda çağrılır — böylece her fetch döngüsünde patch geçerli olur.
     *
     * @param loader  Plugin'in DEX'ini yükleyen ClassLoader
     * @param scraperId Plugin ID (ör. "Animeler", "AnimeciX")
     * @param pluginClassName Tam plugin sınıf adı (ör. "com.kraptor.AnimelerPlugin")
     */
    private fun applyHelperPatches(loader: ClassLoader, scraperId: String, pluginClassName: String) {
        val packageName = pluginClassName.substringBeforeLast('.')
        val baseName    = pluginClassName.substringAfterLast('.').removeSuffix("Plugin")

        // Tüm bilinen Türk CS3 eklenti paket kalıpları:
        // Her paketin kendi Helper sınıfını ve RequestBlocker eşdeğerini dene.
        val potentialHelperClassNames = listOf(
            // Dynamic: plugin kendi paket+base adından türetilmiş
            "$packageName.${baseName}Helper",
            "$packageName.${scraperId}Helper",
            // com.kraptor.* — AnimeciX, Animeler, AnimeElysium vb.
            "com.kraptor.${baseName}Helper",
            "com.kraptor.${scraperId}Helper",
            "com.kraptor.AnimeciXHelper",
            "com.kraptor.AnimeElysiumHelper",
            "com.kraptor.AnimelerHelper",
            // com.keyiflerolsun.* — DiziPal, RecTV, DDizi, Dizilla vb.
            "com.keyiflerolsun.${baseName}Helper",
            "com.keyiflerolsun.${scraperId}Helper",
            // com.nikyokki.* — AnimPow, AsyaAnimeleri vb.
            "com.nikyokki.${baseName}Helper",
            "com.nikyokki.${scraperId}Helper",
            // com.byayzen.* — DiziBox, HDFilmCehennemi vb.
            "com.byayzen.${baseName}Helper",
            "com.byayzen.${scraperId}Helper",
            // com.kerimmkirac.* — TurkAnime, DiziBox vb.
            "com.kerimmkirac.${baseName}Helper",
            "com.kerimmkirac.${scraperId}Helper",
            // recloudstream.* — Xhamster, XNXX vb.
            "recloudstream.${baseName}Helper",
            "recloudstream.${scraperId}Helper",
        ).distinct()

        for (helperClassName in potentialHelperClassNames) {
            try {
                val helperClass = loader.loadClass(helperClassName)

                // 1. Direct static field patch
                try {
                    val field = helperClass.getDeclaredField("isAllowedVersion")
                    field.isAccessible = true
                    field.set(null, true)
                    Log.d(TAG, "[$scraperId] Patched static field $helperClassName.isAllowedVersion = true")
                } catch (e: NoSuchFieldException) {
                    // Field not found — try setter method
                }

                // 2. Kotlin object setter — setAllowedVersion(true)
                try {
                    val method = helperClass.getMethod(
                        "setAllowedVersion",
                        Boolean::class.javaPrimitiveType ?: Boolean::class.java
                    )
                    val instanceField = helperClass.getDeclaredField("INSTANCE")
                    instanceField.isAccessible = true
                    val instanceObj = instanceField.get(null)
                    method.invoke(instanceObj, true)
                    Log.d(TAG, "[$scraperId] Invoked $helperClassName.setAllowedVersion(true)")
                } catch (e: Exception) {
                    // Setter not found — field-only patch is enough
                }
            } catch (e: ClassNotFoundException) {
                // Not this plugin type — expected, ignore
            } catch (e: Exception) {
                Log.e(TAG, "[$scraperId] Failed to patch $helperClassName: ${e.message}", e)
            }
        }

        // Patch RequestBlocker eşdeğerleri — farklı paket adlarında dene
        val blockerClassNames = listOf(
            "com.kraptor.RequestBlocker",
            "com.keyiflerolsun.RequestBlocker",
            "com.nikyokki.RequestBlocker",
            "com.byayzen.RequestBlocker",
            "com.kerimmkirac.RequestBlocker",
            "$packageName.RequestBlocker",
        )
        for (blockerClassName in blockerClassNames) {
            try {
                val blockerClass = loader.loadClass(blockerClassName)
                val counterField = blockerClass.getDeclaredField("counter")
                counterField.isAccessible = true
                val counterObj = counterField.get(null) as? java.util.concurrent.atomic.AtomicInteger
                counterObj?.set(100)
                Log.d(TAG, "[$scraperId] Patched $blockerClassName.counter = 100")
            } catch (e: ClassNotFoundException) {
                // Not this package — ignore
            } catch (e: Exception) {
                Log.e(TAG, "[$scraperId] Failed to patch $blockerClassName: ${e.message}", e)
            }
        }
    }
}
