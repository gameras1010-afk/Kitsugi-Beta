package com.kitsugi.animelist.data.manga

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kitsugi.animelist.data.remote.MangaExtensionInfo
import com.kitsugi.animelist.data.remote.MangaRepoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MangaExtensionAutoUpdater
 *
 * Keiyoushi repo index'ini periyodik olarak kontrol ederek yüklü manga eklentilerini
 * otomatik günceller. Uygulama başlangıcında arka planda çalışır.
 *
 * Mantık:
 *   1) Keiyoushi index.min.json çek
 *   2) Yüklü her APK'nın paket adını → repo'daki versionCode ile karşılaştır
 *   3) Repo'daki versionCode daha yüksekse → indir ve bellekten yeniden yükle
 *   4) Sonuçları SharedPreferences'a kaydet (son kontrol zamanı + kaç eklenti güncellendi)
 *
 * Güvenlik mekanizmaları:
 *   - Günde en fazla 1 kontrol yapar (ağ tasarrufu için)
 *   - Tek bir eklenti hata verirse diğerlerine devam eder
 *   - İndirme başarısız olursa mevcut sürüm korunur
 */
object MangaExtensionAutoUpdater {

    private const val TAG = "MangaExtAutoUpdater"
    private const val PREFS_NAME = "manga_auto_updater"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val KEY_LAST_UPDATED_COUNT = "last_updated_count"
    private const val KEY_LAST_CHECK_RESULT = "last_check_result"

    /** Günde en fazla bir kontrol (24 saat) */
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    /**
     * Otomatik güncelleme akışını çalıştırır.
     * [forceCheck] = true ile 24 saatlik kısıtlamayı aşabilirsiniz (ayarlar ekranındaki "Şimdi Güncelle" butonu).
     *
     * @return Güncellenen eklenti sayısı
     */
    suspend fun runIfNeeded(
        context: Context,
        forceCheck: Boolean = false
    ): UpdateResult = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        val now = System.currentTimeMillis()

        if (!forceCheck && (now - lastCheck) < CHECK_INTERVAL_MS) {
            val since = (now - lastCheck) / 1000 / 60
            Log.d(TAG, "Son kontrolden bu yana $since dk geçmiş, atlanıyor (24 saat dolmadı).")
            return@withContext UpdateResult.Skipped(reason = "Sonraki kontrol ${((CHECK_INTERVAL_MS - (now - lastCheck)) / 1000 / 60)} dk sonra.")
        }

        Log.i(TAG, "Keiyoushi repo kontrolü başlıyor...")
        prefs.edit().putLong(KEY_LAST_CHECK_MS, now).apply()

        // Yüklü eklentileri al — loadedSources ve fiziksel klasör üzerinden paket adı → versionCode
        val loadedSources = MangaExtensionLoader.getLoadedSources()
        val installedPackages = buildInstalledPackageMap(context, loadedSources)
        if (installedPackages.isEmpty()) {
            Log.i(TAG, "Yüklü manga eklentisi veya APK paketi bulunamadı, güncelleme atlanıyor.")
            return@withContext UpdateResult.Success(updated = 0, checked = 0)
        }

        Log.i(TAG, "${installedPackages.size} yüklü APK paketi bulundu.")

        // Keiyoushi index'ini çek
        val client = MangaRepoClient(context)
        val repoExtensions = client.fetchExtensionsAutoDetect(MangaRepoClient.KEIYOUSHI_INDEX_URL)

        if (repoExtensions == null) {
            Log.e(TAG, "Keiyoushi repo çekilemedi.")
            prefs.edit().putString(KEY_LAST_CHECK_RESULT, "REPO_FETCH_FAILED").apply()
            return@withContext UpdateResult.Failed(reason = "Repo index çekilemedi")
        }

        Log.i(TAG, "Repo'da ${repoExtensions.size} eklenti var.")

        // Paket adına göre hızlı lookup için map oluştur
        val repoByPkg = repoExtensions.associateBy { it.pkg }

        var updatedCount = 0
        var checkedCount = 0
        val updatedNames = mutableListOf<String>()

        for ((pkg, installedVersionCode) in installedPackages) {
            val repoEntry = repoByPkg[pkg] ?: continue
            checkedCount++

            val repoVersionCode = repoEntry.versionCode
            if (repoVersionCode > installedVersionCode) {
                Log.i(TAG, "${repoEntry.name}: güncelleme var! ($installedVersionCode → $repoVersionCode)")
                val success = tryUpdateExtension(context, repoEntry)
                if (success) {
                    updatedCount++
                    updatedNames.add(repoEntry.name)
                    Log.i(TAG, "${repoEntry.name}: güncellendi ✅")
                } else {
                    Log.w(TAG, "${repoEntry.name}: güncelleme başarısız ❌")
                }
            } else {
                Log.d(TAG, "${repoEntry.name}: güncel (v$installedVersionCode)")
            }
        }

        val summary = if (updatedCount > 0)
            "✅ $updatedCount eklenti güncellendi: ${updatedNames.joinToString(", ")}"
        else
            "✅ Tüm eklentiler güncel ($checkedCount kontrol edildi)"

        Log.i(TAG, summary)
        prefs.edit()
            .putInt(KEY_LAST_UPDATED_COUNT, updatedCount)
            .putString(KEY_LAST_CHECK_RESULT, summary)
            .apply()

        UpdateResult.Success(updated = updatedCount, checked = checkedCount, updatedNames = updatedNames)
    }

    /**
     * Tek bir eklentiyi indir ve yeniden yükle.
     * Başarısız olursa mevcut APK dokunulmaz.
     */
    private fun tryUpdateExtension(context: Context, info: MangaExtensionInfo): Boolean {
        return try {
            val extensionId = info.pkg.replace(".", "_")
            val downloaded = MangaExtensionLoader.downloadExtension(context, extensionId, info.apkUrl)
            if (!downloaded) return false

            // Bellekten kaldır ve yeniden yükle
            MangaExtensionLoader.unloadExtension(context, extensionId)
            val loaded = MangaExtensionLoader.loadExtension(context, extensionId)
            loaded != null
        } catch (e: Exception) {
            Log.e(TAG, "tryUpdateExtension hata (${info.name}): ${e.message}", e)
            false
        }
    }

    /**
     * Yüklü kaynaklardan paket adı → kurulu versionCode haritası oluşturur.
     * PackageManager aracılığıyla APK'nın gerçek versionCode'unu okur.
     */
    private fun buildInstalledPackageMap(
        context: Context,
        sources: List<MangaSource>
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        
        // 1) loadedSources içindeki eklentileri al
        for (source in sources) {
            val pkg = source.pkgName.trim()
            if (pkg.isNotBlank()) {
                val vc = MangaExtensionLoader.getInstalledVersionCode(context, pkg)
                if (vc >= 0) {
                    result[pkg] = vc
                }
            }
        }
        
        // 2) manga_extensions dizinindeki tüm APK dosyalarını tarayarak yüklü olanları da al
        try {
            val extDir = MangaExtensionLoader.extensionDir(context)
            if (extDir.exists() && extDir.isDirectory) {
                val apkFiles = extDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) }
                apkFiles?.forEach { file ->
                    val filename = file.nameWithoutExtension
                    val pkg = filename.replace("_", ".")
                    val vc = MangaExtensionLoader.getInstalledVersionCode(context, pkg)
                    if (vc >= 0) {
                        result[pkg] = vc
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "buildInstalledPackageMap dizin tarama hatası: ${e.message}")
        }
        
        return result
    }

    // ─── Durum sorgulama (Settings UI için) ──────────────────────────────────

    fun getLastCheckTimeMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_CHECK_MS, 0L)

    fun getLastUpdatedCount(context: Context): Int =
        prefs(context).getInt(KEY_LAST_UPDATED_COUNT, 0)

    fun getLastCheckResult(context: Context): String? =
        prefs(context).getString(KEY_LAST_CHECK_RESULT, null)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Result sınıfları ────────────────────────────────────────────────────

    sealed class UpdateResult {
        data class Success(
            val updated: Int,
            val checked: Int,
            val updatedNames: List<String> = emptyList()
        ) : UpdateResult()

        data class Skipped(val reason: String) : UpdateResult()
        data class Failed(val reason: String) : UpdateResult()
    }
}
