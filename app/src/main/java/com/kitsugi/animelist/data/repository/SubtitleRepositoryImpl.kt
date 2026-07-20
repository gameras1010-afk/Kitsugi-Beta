package com.kitsugi.animelist.data.repository

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.core.player.OpenSubtitlesHasher
import com.kitsugi.animelist.core.player.model.Subtitle
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.remote.AddonStreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

/**
 * S01 – SubtitleRepositoryImpl
 *
 * NuvioTV SubtitleRepositoryImpl.kt mantığı referans alınarak yeniden yazıldı.
 * Enabled subtitle addon'larını paralel olarak sorgular.
 *
 * Düzeltmeler (NuvioTV karşılaştırması):
 * 1. idPrefixes filtreleme: Addon yalnızca belirli ID prefix'lerini destekliyorsa
 *    (örn: OpenSubtitles → "tt"), queryId ile eşleştir.
 * 2. subtitleTypes tip kontrolü: Addon hangi içerik tipini (movie/series) destekliyorsa ona sor.
 * 3. Detaylı logging: Hangi addon'dan kaç altyazı geldi, hangi URL'e istek gitti.
 */
class SubtitleRepositoryImpl(private val context: Context) {

    companion object {
        private const val TAG = "SubtitleRepository"
        private const val PER_ADDON_TIMEOUT_MS = 20_000L
    }

    private val database = KitsugiDatabase.getDatabase(context.applicationContext)
    private val addonDao = database.managedAddonDao()
    private val addonClient = AddonStreamClient()

    /**
     * Tüm aktif altyazı addon'larından paralel olarak altyazı listesi çeker.
     *
     * @param type        İçerik tipi: "series" veya "movie"
     * @param id          Stremio ID (örn: "tt1234567:1:5" veya "kitsu:12345:5")
     * @param videoUrl    Video URL'si (OpenSubtitles hash hesabı için; null ise hash atlanır)
     * @param videoHeaders Video HTTP header'ları (hash isteği için)
     * @param filename    Dosya adı (addon'lara ipucu olarak verilir)
     */
    suspend fun getSubtitles(
        type: String,
        id: String,
        videoUrl: String? = null,
        videoHeaders: Map<String, String>? = null,
        filename: String? = null
    ): List<Subtitle> = coroutineScope {

        val normalizedType = if (type.equals("tv", ignoreCase = true)) "series" else type.lowercase()

        // 1. Altyazı destekleyen enabled addon'ları al + filtrele
        val allAddons = addonDao.getEnabledAddons()
        val subtitleAddons = allAddons.filter { addon ->
            // subtitleTypes kontrolü:
            // null = "bilinmiyor" (eski DB kaydı veya manifest parse edilemedi)
            //   → Addon adı/URL'si altyazı ile ilgiliyse dahil et, aksi halde DENE (atma)
            // Not-null = manifest'ten kesin olarak parse edildi
            if (addon.subtitleTypes != null) {
                // subtitleTypes tip kontrolü: "movie,series" içinde requestedType var mı?
                val supportedTypes = addon.subtitleTypes.split(",").map { it.trim().lowercase() }
                if (supportedTypes.isNotEmpty() && normalizedType !in supportedTypes) {
                    Log.d(TAG, "Addon '${addon.name}' atlandı: type=$normalizedType desteklenmiyor (desteklenenler: ${addon.subtitleTypes})")
                    return@filter false
                }
            } else {
                // subtitleTypes = null → Stream addon'u veya bilinmeyen. İsim/URL kontrolü yap:
                // Eğer kesinlikle stream-only addon ise (stream türü var ama altyazı ilgisi yok), atla
                val looksLikeSubtitleAddon = looksLikeSubtitleAddon(addon)
                if (!looksLikeSubtitleAddon && addon.streamTypes != null) {
                    Log.d(TAG, "Addon '${addon.name}' atlandı: subtitleTypes=null ve stream-only addon gibi görünüyor")
                    return@filter false
                }
                Log.d(TAG, "Addon '${addon.name}' dahil edildi: subtitleTypes=null ama altyazı addon'u olabilir")
            }

            // idPrefixes kontrolü (NuvioTV supportsType() mantığı):
            // Addon belirli prefix'leri tanımlıyorsa, ID bu prefix ile başlamalı
            val idPrefixes = parseIdPrefixes(addon)
            if (!idPrefixes.isNullOrEmpty()) {
                val matches = idPrefixes.any { prefix -> id.startsWith(prefix) }
                if (!matches) {
                    Log.d(TAG, "Addon '${addon.name}' atlandı: id='$id' prefix listesiyle eşleşmiyor (prefixes=$idPrefixes)")
                    return@filter false
                }
            }
            true
        }

        if (subtitleAddons.isEmpty()) {
            Log.w(TAG, "Hiç aktif altyazı addon'u bulunamadı — type=$normalizedType, id=$id")
            return@coroutineScope emptyList()
        }

        Log.d(TAG, "Altyazı fetch başlatılıyor: ${subtitleAddons.size} addon, type=$normalizedType, id=$id, filename=$filename")

        // 2. OpenSubtitles hash hesapla (arka planda, addon fetch'iyle eş zamanlı)
        val hashJob = async(Dispatchers.IO) {
            if (videoUrl.isNullOrBlank()) return@async null
            try {
                val result = OpenSubtitlesHasher.compute(videoUrl, videoHeaders)
                if (result != null) {
                    Log.d(TAG, "OpenSubtitles hash: hash=${result.hash}, size=${result.size}")
                }
                result
            } catch (e: Exception) {
                Log.w(TAG, "Hash hesaplanamadı: ${e.message}")
                null
            }
        }

        // 3. Her addon için paralel fetch
        val deferredResults = subtitleAddons.map { addon ->
            async(Dispatchers.IO) {
                val hashResult = try { hashJob.await() } catch (_: Exception) { null }

                val extraParams = buildMap<String, String> {
                    if (hashResult != null) {
                        put("videoHash", hashResult.hash)
                        put("videoSize", hashResult.size.toString())
                    }
                    if (!filename.isNullOrBlank()) put("filename", filename)
                }

                val subtitles = withTimeoutOrNull(PER_ADDON_TIMEOUT_MS) {
                    try {
                        val items = addonClient.fetchSubtitles(
                            manifestUrl = addon.manifestUrl,
                            type = normalizedType,
                            id = id,
                            extraParams = extraParams.ifEmpty { null }
                        )
                        Log.d(TAG, "Addon '${addon.name}': ${items.size} altyazı döndü")
                        items.mapNotNull { item ->
                            val url = item.url ?: return@mapNotNull null
                            val lang = item.lang ?: "unknown"
                            Subtitle(
                                id = item.id ?: url,
                                url = url,
                                lang = lang,
                                addonName = addon.name,
                                addonLogo = addon.icon,
                                format = guessFormat(url)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Addon '${addon.name}' fetch hatası: ${e.message}")
                        emptyList()
                    }
                } ?: run {
                    Log.w(TAG, "Addon '${addon.name}' ${PER_ADDON_TIMEOUT_MS}ms timeout")
                    emptyList()
                }
                subtitles
            }
        }

        val allSubtitles = deferredResults.awaitAll().flatten()
        Log.d(TAG, "Toplam ${allSubtitles.size} altyazı bulundu (${subtitleAddons.size} addon sorgulandı)")

        // 4. ID'ye göre dedup
        val seen = LinkedHashSet<String>()
        val deduped = mutableListOf<Subtitle>()
        for (sub in allSubtitles) {
            if (seen.add(sub.id)) deduped += sub
        }

        deduped
    }

    /**
     * Addon'un isim veya URL'sine bakarak altyazı addon'u olup olmadığını tahmin eder.
     * subtitleTypes = null olan eski kayıtlar için fallback filtre.
     */
    private fun looksLikeSubtitleAddon(addon: ManagedAddonEntity): Boolean {
        val combined = (addon.name + " " + addon.manifestUrl).lowercase()
        return combined.contains("subtitle") ||
            combined.contains("altyaz") ||
            combined.contains("opensubtitle") ||
            combined.contains("yts-subtitle") ||
            combined.contains("turkcealtyazi") ||
            combined.contains("caption") ||
            combined.contains("sub.strem")
    }

    /**
     * Addon'un idPrefixes JSON array'ini parse eder.
     * Null → prefix filtresi yok (her ID ile eşleşir).
     */
    private fun parseIdPrefixes(addon: ManagedAddonEntity): List<String>? {
        val json = addon.idPrefixes ?: return null
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "idPrefixes parse hatası (addon='${addon.name}'): ${e.message}")
            null
        }
    }

    /**
     * URL uzantısından format tahmin eder.
     */
    private fun guessFormat(url: String): String {
        val path = url.substringBefore("?").lowercase()
        return when {
            path.endsWith(".ass") || path.endsWith(".ssa") -> "ass"
            path.endsWith(".vtt") -> "vtt"
            path.endsWith(".ttml") || path.endsWith(".dfxp") -> "ttml"
            else -> "srt"
        }
    }
}
