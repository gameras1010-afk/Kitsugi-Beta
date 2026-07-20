package com.kitsugi.animelist.data.manga

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.kitsugi.animelist.core.security.TrustManager
import com.kitsugi.animelist.data.manga.loader.ChildFirstPathClassLoader
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import uy.kohesive.injekt.Injekt
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/**
 * MangaExtensionLoader
 *
 * Manga eklentilerini dinamik olarak yükleyen motor.
 *
 * İKİ FORMAT DESTEKLENİR:
 *
 * 1) Mihon/Tachiyomi APK (.apk)
 *    - AndroidManifest.xml içindeki meta-data "tachiyomi.extension.class" alanından
 *      sınıf ismi okunur.
 *    - Yüklenen sınıf eu.kanade.tachiyomi.source.Source interface'ini implement eder.
 *    - MihonSourceWrapper aracılığıyla Kitsugi MangaSource'a dönüştürülür.
 *
 * 2) Kitsugi Native (.mex)
 *    - ZIP içindeki manifest.json dosyasındaki "sourceClassName" alanından
 *      sınıf ismi okunur.
 *    - Yüklenen sınıf doğrudan MangaSource interface'ini implement eder.
 *
 * Keiyoushi gibi repolardan indirilen APK'lar Format 1 ile desteklenir.
 */
object MangaExtensionLoader {

    private const val TAG = "MangaExtensionLoader"

    // Mihon APK'larında AndroidManifest.xml meta-data anahtarı
    private const val MIHON_EXTENSION_META_KEY = "tachiyomi.extension.class"

    /** Yüklü kaynak örneklerini tutar: filePath -> List<MangaSource> (birden fazla source içerebilir) */
    private val loadedSources = ConcurrentHashMap<String, List<MangaSource>>()

    // ─── Yüklü Kaynakları Sorgulama ─────────────────────────────────────────

    /**
     * Tüm yüklü kaynakları döner:
     * 1) APK/MEX formatında yüklenmiş Keiyoushi/Mihon kaynakları
     * 2) KotatsuExtensionAdapter üzerinden yüklü Kotatsu built-in kaynakları
     *
     * Kaynak listesi, tüm sisteme tek noktadan erişim sağlar.
     */
    fun getLoadedSources(): List<MangaSource> {
        val apkMexSources = loadedSources.values.flatten()
        // Sadece aktif dillerdeki Kotatsu kaynakları — Futon mantığıyla aynı
        val kotatsuSources = KotatsuExtensionAdapter.getActiveSources()
        return if (kotatsuSources.isEmpty()) apkMexSources
        else apkMexSources + kotatsuSources
    }

    fun getSource(extensionId: String, context: Context): MangaSource? {
        val apkFile = apkFile(context, extensionId)
        val mexFile = mexFile(context, extensionId)
        return loadedSources[apkFile.absolutePath]?.firstOrNull()
            ?: loadedSources[mexFile.absolutePath]?.firstOrNull()
    }

    fun getExtensionIdForSource(source: MangaSource): String? {
        val entry = loadedSources.entries.find { list -> list.value.any { it === source } } ?: return null
        return File(entry.key).nameWithoutExtension
    }

    // ─── İndirme ─────────────────────────────────────────────────────────────

    /**
     * Verilen URL'den eklentiyi indirir.
     * Dosya uzantısı .apk ise APK olarak, değilse .mex olarak kaydedilir.
     */
    fun downloadExtension(
        context: Context,
        extensionId: String,
        urlString: String
    ): Boolean {
        val dir = extensionDir(context).also { if (!it.exists()) it.mkdirs() }
        val isApk = urlString.substringBefore("?").endsWith(".apk", ignoreCase = true)
            || urlString.contains(".apk", ignoreCase = true)
            || extensionId.startsWith("eu_kanade_tachiyomi")
        val targetExt = if (isApk) "apk" else "mex"
        val temp = File(dir, "$extensionId.$targetExt.tmp")
        val target = File(dir, "$extensionId.$targetExt")

        return try {
            if (temp.exists()) temp.delete()

            val mirroredUrl = com.kitsugi.animelist.data.remote.MangaRepoClient.applyMirror(urlString)
            Log.d(TAG, "İndiriliyor [$targetExt]: $extensionId <- $mirroredUrl (original: $urlString)")
            
            val networkHelper = Injekt.get(NetworkHelper::class.java)
            val client = networkHelper.client

            var success = false
            var attempt = 1
            val maxAttempts = 4
            var delayMs = 1500L

            while (attempt <= maxAttempts && !success) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url(mirroredUrl)
                        .header("User-Agent", networkHelper.defaultUserAgentProvider())
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.code == 429) {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: (delayMs / 1000L)
                            val sleepTime = (retryAfter * 1000L) + (Math.random() * 500).toLong()
                            Log.w(TAG, "$extensionId: HTTP 429 (Too Many Requests). $sleepTime ms bekleniyor... (Deneme $attempt/$maxAttempts)")
                            Thread.sleep(sleepTime)
                            attempt++
                            delayMs *= 2
                        } else if (!response.isSuccessful) {
                            Log.e(TAG, "$extensionId indirme hatası: HTTP ${response.code} (Deneme $attempt/$maxAttempts)")
                            Thread.sleep(delayMs)
                            attempt++
                            delayMs *= 2
                        } else {
                            response.body?.byteStream()?.use { inp ->
                                FileOutputStream(temp).use { out -> inp.copyTo(out) }
                            }
                            success = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "$extensionId bağlantı hatası (Deneme $attempt/$maxAttempts): ${e.message}")
                    if (attempt == maxAttempts) throw e
                    Thread.sleep(delayMs)
                    attempt++
                    delayMs *= 2
                }
            }

            if (!success) {
                Log.e(TAG, "$extensionId: Tüm indirme denemeleri başarısız oldu.")
                return false
            }

            // ZIP/APK geçerliliği kontrolü
            val valid = try { ZipFile(temp).use { true } } catch (_: Exception) { false }
            if (!valid) {
                Log.e(TAG, "$extensionId: indirilen dosya geçerli bir ZIP/APK değil")
                temp.delete()
                return false
            }

            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                Log.e(TAG, "$extensionId: geçici dosya taşınamadı")
                temp.delete()
                return false
            }
            // NOT: setReadOnly() KALDIRILDI — Android 10+ PathClassLoader read-only dosyayı yükleyemez
            Log.i(TAG, "$extensionId başarıyla indirildi (${target.length()} bytes) [$targetExt]")
            true
        } catch (e: Exception) {
            Log.e(TAG, "$extensionId indirme hatası: ${e.message}", e)
            if (temp.exists()) temp.delete()
            false
        }
    }

    // ─── Yükleme ─────────────────────────────────────────────────────────────

    /**
     * İndirilen eklentiyi belleğe yükler.
     * Önce .apk uzantılı Mihon formatını dener, bulamazsa .mex formatını dener.
     */
    fun loadExtension(context: Context, extensionId: String): MangaSource? {
        val apk = apkFile(context, extensionId)
        val mex = mexFile(context, extensionId)

        return when {
            apk.exists() -> loadMihonApk(context, extensionId, apk)
            mex.exists() -> loadNativeMex(context, extensionId, mex)
            else -> {
                Log.e(TAG, "$extensionId: ne .apk ne de .mex dosyası bulunamadı")
                null
            }
        }
    }

    /**
     * Mihon/Tachiyomi APK yükleyici.
     *
     * Adımlar:
     * 1) PackageManager ile APK içindeki AndroidManifest.xml meta-data'dan
     *    eu.kanade.tachiyomi.source.Source sınıf adını okur.
     * 2) PathClassLoader ile sınıfı yükler.
     * 3) NetworkHelper'ı Injekt'e kaydeder (HttpSource bu bağımlılığa ihtiyaç duyar).
     * 4) Source örneğini MihonSourceWrapper ile sarar ve döndürür.
     */
    private fun loadMihonApk(context: Context, extensionId: String, file: File): MangaSource? {
        // Önbellekten döndür (ilk source'u ver)
        loadedSources[file.absolutePath]?.let { return it.firstOrNull() }

        return try {
            // NOT: setReadOnly() kaldırıldı — Android 10+ PathClassLoader için dosya readable olmalı

            // AndroidManifest.xml meta-data'dan paket adı ve sınıf adını oku
            val (packageName, className) = readMihonClassNameFromManifest(context, file, extensionId)
                ?: return null

            // ─── T1.01 – Signature Check ──────────────────────────────────────────
            val signatureHash = TrustManager.getApkSignatureHash(context, file)
            if (signatureHash != null) {
                if (!TrustManager.isSignatureTrusted(context, signatureHash)) {
                    Log.e(TAG, "SecurityException: extension signature is not trusted: $signatureHash")
                    throw SecurityException("Signature untrusted for package $packageName: $signatureHash")
                }
            } else {
                Log.w(TAG, "Could not extract signature for file: ${file.absolutePath}")
            }

            Log.d(TAG, "$extensionId [APK]: paket=$packageName, sınıf yükleniyor -> $className")

            // ChildFirstPathClassLoader
            val loader = ChildFirstPathClassLoader(file.absolutePath, null, context.classLoader)

            // NetworkHelper'ı Injekt'e kaydet (HttpSource lazy inject eder)
            try {
                Injekt.import<android.app.Application>(context.applicationContext as android.app.Application)
                Injekt.import<eu.kanade.tachiyomi.network.NetworkHelper>(
                    Injekt.get(eu.kanade.tachiyomi.network.NetworkHelper::class.java)
                )
                Injekt.import<kotlinx.serialization.json.Json>(
                    Injekt.get(kotlinx.serialization.json.Json::class.java)
                )
            } catch (_: Exception) { /* Zaten kayıtlıysa hata verir, önemli değil */ }

            // Sınıfları yükle (Mihon'da sınıflar ';' ile ayrılabilir)
            val classNames = className.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            val sources = classNames.flatMap { name ->
                try {
                    val clazz = loader.loadClass(name)
                    when {
                        SourceFactory::class.java.isAssignableFrom(clazz) -> {
                            val factory = clazz.getDeclaredConstructor().newInstance() as SourceFactory
                            factory.createSources()
                        }
                        else -> {
                            val instance = try {
                                clazz.getDeclaredConstructor(Context::class.java).newInstance(context) as Source
                            } catch (_: NoSuchMethodException) {
                                clazz.getDeclaredConstructor().newInstance() as Source
                            }
                            listOf(instance)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sınıf yükleme hatası ($name): ${e.message}", e)
                    emptyList()
                }
            }

            if (sources.isEmpty()) {
                Log.e(TAG, "$extensionId: kaynak listesi boş veya yüklenemedi")
                return null
            }

            Log.d(TAG, "$extensionId [APK]: ${sources.size} kaynak bulundu")

            // Engine tipini APK DEX'inden tespit et ve cache'le
            val engine = detectEngineFromApk(context, extensionId, file)
            Log.d(TAG, "$extensionId engine tespit: $engine")

            // TÜM source'ları wrap et — artık sadece 1 değil hepsi kaydediliyor
            val wrappers = sources.map { source ->
                applyDefaultPreferences(context, source)
                MihonSourceWrapper(source, context, packageName, engine)
            }

            loadedSources[file.absolutePath] = wrappers
            Log.i(TAG, "$extensionId [APK] yüklendi: ${wrappers.size} kaynak (pkg=$packageName): ${wrappers.joinToString { "${it.name}[${it.lang}]" }}")

            // YENİ YÜKLENEN KAYNAKLARIN ESKİ COOLDOWN/BROKEN STATE'İNİ TEMİZLE
            // Önceki broken build'den kalan "Broken" durumu yeni kaynakları arama dışı bırakır.
            // Her fresh load'da state sıfırlanır — bir sonraki başarılı aramada Healthy'ye döner.
            try {
                val stateStore = MangaSourceStateStore(context)
                wrappers.forEach { wrapper ->
                    val health = stateStore.getHealthStatus(wrapper)
                    if (health == com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Broken ||
                        health == com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Degraded ||
                        stateStore.isCoolingDown(wrapper)
                    ) {
                        Log.d(TAG, "$extensionId [${wrapper.name}]: eski ${health.name} durumu sıfırlandı")
                        stateStore.resetSource(wrapper)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "$extensionId: state sıfırlama hatası (kritik değil): ${e.message}")
            }

            wrappers.firstOrNull()

        } catch (e: SecurityException) {
            Log.e(TAG, "$extensionId [APK] signature verification failed: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$extensionId [APK] yükleme hatası: ${e.message}", e)
            null
        }
    }

    /**
     * Kitsugi native .mex yükleyici (eski format, manifest.json tabanlı).
     */
    private fun loadNativeMex(context: Context, extensionId: String, file: File): MangaSource? {
        loadedSources[file.absolutePath]?.let { return it.firstOrNull() }

        return try {
            val valid = try { ZipFile(file).use { true } } catch (_: Exception) { false }
            if (!valid) { Log.e(TAG, "$extensionId [MEX]: bozuk ZIP"); return null }
            // NOT: setReadOnly() kaldırıldı

            val loader = PathClassLoader(file.absolutePath, context.classLoader)
            val className = readSourceClassNameFromJson(loader, file, extensionId) ?: return null

            Log.d(TAG, "$extensionId [MEX]: sınıf yükleniyor -> $className")
            val clazz = loader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance() as MangaSource

            loadedSources[file.absolutePath] = listOf(instance)
            Log.i(TAG, "$extensionId [MEX] yüklendi: ${instance.name} [${instance.lang}]")
            instance
        } catch (e: Exception) {
            Log.e(TAG, "$extensionId [MEX] yükleme hatası: ${e.message}", e)
            null
        }
    }

    // ─── Kaldırma / Silme ────────────────────────────────────────────────────

    fun unloadExtension(context: Context, extensionId: String) {
        loadedSources.remove(apkFile(context, extensionId).absolutePath)
        loadedSources.remove(mexFile(context, extensionId).absolutePath)
        Log.i(TAG, "$extensionId bellekten kaldırıldı")
    }

    fun deleteExtension(context: Context, extensionId: String): Boolean {
        unloadExtension(context, extensionId)
        var deleted = false
        listOf(apkFile(context, extensionId), mexFile(context, extensionId)).forEach { file ->
            if (file.exists()) deleted = file.delete().also {
                Log.i(TAG, "${file.name} silindi: $it")
            }
        }
        return deleted
    }

    /** Yüklü source sayısını döndürür (debug amaçlı) */
    fun getLoadedSourceCount(): Int = loadedSources.values.sumOf { it.size }

    /**
     * Tüm .apk ve .mex eklentilerini tarayarak yükler.
     * isLoadedAll flag kaldırıldı — her dosya için cache kontrolü yapılır,
     * böylece yeni kurulan extensionlar her zaman doğru şekilde yüklenir.
     */
    @Synchronized
    fun loadAllExtensions(context: Context) {
        val dir = extensionDir(context)
        if (!dir.exists()) { dir.mkdirs(); return }

        val files = dir.listFiles { _, name ->
            name.endsWith(".mex", ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)
        } ?: return

        val newFiles = files.filter { !loadedSources.containsKey(it.absolutePath) }
        if (newFiles.isEmpty()) {
            Log.d(TAG, "Tüm eklentiler zaten yüklü (${loadedSources.size} APK/MEX, ${getLoadedSourceCount()} kaynak)")
            return
        }

        Log.i(TAG, "${newFiles.size} yeni eklenti yükleniyor: ${dir.absolutePath}")
        for (file in newFiles) {
            val extensionId = file.nameWithoutExtension
            try { loadExtension(context, extensionId) }
            catch (e: Exception) { Log.e(TAG, "Eklenti yükleme hatası ($extensionId): ${e.message}", e) }
        }
        Log.i(TAG, "Toplam yüklü kaynak: ${getLoadedSourceCount()}")
    }

    // ─── Preference Varsayılanları ────────────────────────────────────────────

    /**
     * Bir kaynak [ConfigurableSource] ise, `setupPreferenceScreen` çağrılarak
     * androidx.preference'ın varsayılan değerleri kalıcı hale getirilmesi sağlanır.
     *
     * Nasıl çalışır:
     * - Kaynağın kendi SharedPreferences'ı (`getSourcePreferences()`) bir PreferenceManager'a
     *   bağlanır (reflection ile `preferenceDataStore` yerine SharedPreferences adı ayarlanır).
     * - Boş bir PreferenceScreen oluşturulur ve `setupPreferenceScreen(screen)` çağrılır.
     * - Kaynak, EditTextPreference vb. eklerken `setDefaultValue(...)` verir; ekleme sırasında
     *   androidx bu default'u ilgili SharedPreferences'a yazar.
     *
     * Hata olursa sessizce geçilir (kaynak yine yüklü kalır, sadece default yazılmamış olur).
     */
    private fun applyDefaultPreferences(context: Context, source: eu.kanade.tachiyomi.source.Source) {
        val configurable = source as? eu.kanade.tachiyomi.source.ConfigurableSource ?: return
        try {
            // Kaynağın kendi preference dosyası (source_<id>)
            val prefKey = "source_${source.id}"

            // androidx.preference.PreferenceManager'ın public constructor'ı yoktur;
            // Mihon/Aniyomi gibi reflection ile oluşturuyoruz.
            val pmClass = androidx.preference.PreferenceManager::class.java
            val ctor = pmClass.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            val prefManager = ctor.newInstance(context)

            // Hangi SharedPreferences dosyasını kullanacağını ayarla (source_<id>)
            pmClass.getMethod("setSharedPreferencesName", String::class.java)
                .invoke(prefManager, prefKey)
            pmClass.getMethod("setSharedPreferencesMode", Int::class.javaPrimitiveType)
                .invoke(prefManager, Context.MODE_PRIVATE)

            val screen = pmClass.getMethod("createPreferenceScreen", Context::class.java)
                .invoke(prefManager, context) as androidx.preference.PreferenceScreen

            // Kaynak, screen'e kendi preference'larını (default değerleriyle) ekler;
            // androidx ekleme sırasında default'ları SharedPreferences'a yazar.
            configurable.setupPreferenceScreen(screen)

            Log.d(TAG, "Default preferences uygulandı: ${source.name} ($prefKey)")
        } catch (e: Exception) {
            Log.w(TAG, "Default preference uygulanamadı (${source.name}): ${e.message}")
        }
    }

    // ─── Engine Detection ─────────────────────────────────────────────────────

    /**
     * APK dosyasının DEX'indeki ASCII string tablosunu okuyarak
     * eklentinin hangi scraping motorunu kullandığını tespit eder.
     *
     * Bu yaklaşım jadx/dex2jar gibi araç gerektirmez; sadece ByteArray
     * üzerinde regex ile pattern matching yapar.
     *
     * Fingerprint öncelik sırası (en özgül → en genel):
     *   INERTIA > SVELTE > MADARA > THEMESIA > CUSTOM_HTML
     */
    private fun detectEngineFromApk(context: Context, extensionId: String, apkFile: File): ExtensionEngine {
        val cacheKey = "engine_$extensionId"
        val timeKey = "engine_time_$extensionId"
        val prefs = context.getSharedPreferences("manga_extension_engines", Context.MODE_PRIVATE)

        val lastModified = apkFile.lastModified()
        if (prefs.getLong(timeKey, 0L) == lastModified) {
            val cached = prefs.getString(cacheKey, null)
            if (cached != null) {
                val parsed = runCatching { ExtensionEngine.valueOf(cached) }.getOrNull()
                if (parsed != null) {
                    return parsed
                }
            }
        }

        val engine = try {
            ZipFile(apkFile).use { zip ->
                // classes.dex yoksa UNKNOWN
                val dexEntry = zip.getEntry("classes.dex") ?: return ExtensionEngine.UNKNOWN
                val bytes = zip.getInputStream(dexEntry).use { it.readBytes() }

                // ISO-8859-1 is a 1-to-1 byte-to-char mapping. This performs direct conversion in native C++
                // and avoids multi-MB StringBuilder loops in Kotlin, using native String.contains (Boyer-Moore/SIMD).
                val dexStr = String(bytes, Charsets.ISO_8859_1)

                when {
                    // Inertia.js — en spesifik, önce kontrol et
                    "InertiaDto" in dexStr || "X-Inertia" in dexStr ||
                    ("inertia" in dexStr && "mangadenizi" in dexStr) ->
                        ExtensionEngine.INERTIA

                    // SvelteKit — __data.json veya SvelteNode varlığı
                    "SvelteNode" in dexStr || "SvelteResponse" in dexStr ||
                    "__data.json" in dexStr || ("__data" in dexStr && "cdn-u.efsaneler" in dexStr) ->
                        ExtensionEngine.SVELTE

                    // Madara — WP-Manga Plugin spesifik string'ler
                    "wpmangaprotectornonce" in dexStr || "madara_load_more" in dexStr ||
                    "wp_manga_chapter_type" in dexStr ->
                        ExtensionEngine.MADARA

                    // Themesia — WP Manga Themesia spesifik
                    "themesia" in dexStr || "ts_reader" in dexStr ||
                    "MangaThemesia" in dexStr ->
                        ExtensionEngine.THEMESIA

                    // Diğer tüm HTML tabanlı extensionlar
                    else -> ExtensionEngine.CUSTOM_HTML
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Engine detection başarısız (${apkFile.name}): ${e.message}")
            ExtensionEngine.UNKNOWN
        }

        if (engine != ExtensionEngine.UNKNOWN) {
            prefs.edit()
                .putString(cacheKey, engine.name)
                .putLong(timeKey, lastModified)
                .apply()
        }

        return engine
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    fun extensionDir(context: Context) = File(context.filesDir, "manga_extensions")
    fun apkFile(context: Context, id: String) = File(extensionDir(context), "$id.apk")
    private fun mexFile(context: Context, id: String) = File(extensionDir(context), "$id.mex")

    /**
     * Kurulu APK dosyasından versionCode değerini okur.
     *
     * Mihon'daki PackageInfoCompat.getLongVersionCode() ile eşdeğer:
     *   API 28+ → longVersionCode (yüksek 32 bit versionCodeMajor + düşük 32 bit versionCode)
     *   API <28  → versionCode (int)
     *
     * @param pkg APK paket adı (noktalı). Örn: "eu.kanade.tachiyomi.extension.tr.mangatr"
     * @return Kurulu versionCode, dosya yoksa veya okunamazsa -1L
     */
    fun getInstalledVersionCode(context: Context, pkg: String): Long {
        val extensionId = pkg.replace(".", "_")
        val file = apkFile(context, extensionId)
        if (!file.exists()) return -1L
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, flags)
            } ?: return -1L
            // API 28+: longVersionCode; API <28: versionCode (int → long)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getInstalledVersionCode hata ($pkg): ${e.message}")
            -1L
        }
    }

    /**
     * PackageManager kullanarak APK içindeki AndroidManifest.xml'den
     * "tachiyomi.extension.class" meta-data değerini ve paket adını okur.
     *
     * @return Pair(packageName, resolvedClassName) veya hata durumunda null.
     *   - first  → APK'nın paket adı (örn. "eu.kanade.tachiyomi.extension.tr.mangadenizi")
     *   - second → Tam nitelikli sınıf adı (veya ';' ile ayrılmış liste)
     */
    private fun readMihonClassNameFromManifest(
        context: Context,
        file: File,
        extensionId: String
    ): Pair<String, String>? {
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA or PackageManager.GET_CONFIGURATIONS
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, flags)
            }
            if (packageInfo == null) {
                Log.e(TAG, "$extensionId: getPackageArchiveInfo null döndü. Dosya bozuk veya uyumsuz olabilir: ${file.absolutePath}")
                return null
            }
            val pkgName = packageInfo.packageName
            if (pkgName.isNullOrBlank()) {
                Log.e(TAG, "$extensionId: packageInfo.packageName null veya boş.")
                return null
            }
            val appInfo = packageInfo.applicationInfo
            if (appInfo == null) {
                Log.e(TAG, "$extensionId: packageInfo.applicationInfo null döndü.")
                return null
            }
            appInfo.let { info ->
                if (info.sourceDir == null) info.sourceDir = file.absolutePath
                if (info.publicSourceDir == null) info.publicSourceDir = file.absolutePath
            }
            val meta = appInfo.metaData
            if (meta == null) {
                Log.e(TAG, "$extensionId: applicationInfo.metaData null döndü. Manifest meta-data bulunamadı.")
                return null
            }
            val rawClassName = meta.getString(MIHON_EXTENSION_META_KEY)
            if (rawClassName.isNullOrBlank()) {
                Log.e(TAG, "$extensionId: '$MIHON_EXTENSION_META_KEY' meta-data bulunamadı veya boş.")
                return null
            }
            // "." ile başlıyorsa her parçanın önüne paket adı ekle (ör. ".MangaSource;.MangaSource2")
            val resolvedClass = rawClassName.split(";").joinToString(";") { part ->
                val trimmed = part.trim()
                if (trimmed.startsWith(".")) "$pkgName$trimmed" else trimmed
            }
            Log.d(TAG, "$extensionId manifest okundu -> pkg=$pkgName, class=$resolvedClass")
            Pair(pkgName, resolvedClass)
        } catch (e: Exception) {
            Log.e(TAG, "$extensionId: manifest okuma hatası: ${e.message}", e)
            null
        }
    }

    /**
     * manifest.json dosyasından "sourceClassName" alanını okur (MEX formatı).
     */
    private fun readSourceClassNameFromJson(
        loader: PathClassLoader,
        file: File,
        extensionId: String
    ): String? {
        var json: String? = null

        try {
            loader.getResourceAsStream("manifest.json")?.use {
                json = it.bufferedReader().readText()
            }
        } catch (_: Exception) {}

        if (json == null) {
            try {
                ZipFile(file).use { zip ->
                    zip.getEntry("manifest.json")?.let { entry ->
                        json = zip.getInputStream(entry).bufferedReader().readText()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$extensionId: manifest.json okunamadı", e)
            }
        }

        if (json == null) { Log.e(TAG, "$extensionId: manifest.json bulunamadı"); return null }

        val raw = json!!
        val key = "\"sourceClassName\""
        val idx = raw.indexOf(key)
        if (idx < 0) { Log.e(TAG, "$extensionId: 'sourceClassName' alanı yok"); return null }
        val colon = raw.indexOf(':', idx + key.length)
        val start = raw.indexOf('"', colon + 1) + 1
        val end = raw.indexOf('"', start)
        if (start <= 0 || end <= start) { Log.e(TAG, "$extensionId: sourceClassName ayrıştırılamadı"); return null }
        return raw.substring(start, end).trim()
    }
}
