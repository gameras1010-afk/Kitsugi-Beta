package com.kitsugi.animelist.data.cloudstream

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks per-plugin failure state during a single search session.
 *
 * Blocking policy:
 * - NotImplementedError → NOT an immediate block. CsStreamRunner tries
 *   search(q,1) → search(q) → quickSearch(q) in sequence and calls
 *   recordFailure() on each. We count how many *distinct search methods*
 *   returned NotImplementedError for this plugin. Only after all 3 methods
 *   have been tried (notImplCount >= 3) do we block permanently for this session.
 *
 * - Any other error (IOException, HTTP 4xx/5xx, parse error…) → standard
 *   threshold of 3 cumulative failures before blocking, same as before.
 *
 * This prevents plugins that only implement quickSearch() from being
 * permanently blocked just because search(q,1) threw NotImplementedError.
 */
object CsPluginStatusTracker {

    private const val TAG = "CsPluginStatusTracker"

    /** CsPluginLoader ile ortak izleme tag'i */
    private const val PLUGIN_DIAG = "PLUGIN_DIAG"

    // Count of real (non-NotImpl) failures per plugin
    private val failures = ConcurrentHashMap<String, Int>()

    // Count of NotImplementedError hits per plugin (one per search method tried)
    private val notImplCount = ConcurrentHashMap<String, Int>()

    // Count of repeated parse/NPE errors per plugin — too many = plugin is structurally broken
    private val parseFailures = ConcurrentHashMap<String, Int>()
    private const val PARSE_BLOCK_THRESHOLD = 3

    private val blocklist = ConcurrentHashMap.newKeySet<String>()
    private val errorMessages = ConcurrentHashMap<String, String>()

    /**
     * Records a failure for the given plugin.
     *
     * @param pluginId  Typically api.name — used as the tracking key.
     * @param error     The throwable that was caught.
     */
    private fun isParseException(error: Throwable): Boolean {
        val name = error.javaClass.name
        return name.contains("MissingKotlinParameterException", ignoreCase = true) ||
               name.contains("MismatchedInputException", ignoreCase = true) ||
               name.contains("JsonMappingException", ignoreCase = true) ||
               name.contains("JsonParseException", ignoreCase = true) ||
               name.contains("NullPointerException", ignoreCase = true)
    }

    /**
     * Records a failure for the given plugin.
     *
     * @param pluginId  Typically api.name — used as the tracking key.
     * @param error     The throwable that was caught.
     */
    fun recordFailure(pluginId: String, error: Throwable) {
        val message = error.localizedMessage ?: error.message ?: error.javaClass.simpleName
        errorMessages[pluginId] = message

        if (error is kotlin.NotImplementedError) {
            // One more search *method* returned "not implemented".
            // CsStreamRunner tries up to 3 methods, so block only when all 3 failed.
            val count = (notImplCount[pluginId] ?: 0) + 1
            notImplCount[pluginId] = count
            Log.d(TAG, "[$pluginId] NotImplementedError #$count/3")
            if (count >= 3) {
                Log.w(TAG, "[$pluginId] Tüm 3 search yöntemi NotImplementedError verdi — bloklanıyor.")
                Log.e(PLUGIN_DIAG, "🚫 BLOK [$pluginId] — 3/3 NotImplementedError — Hiçbir search metodu desteklenmiyor!")
                blocklist.add(pluginId)
            }
        } else if (isParseException(error)) {
            // Parse/NPE errors may be query-specific, but if they fire on EVERY call
            // (e.g. DiziKorea NPE on search+quickSearch each round) we block after the threshold.
            val count = (parseFailures[pluginId] ?: 0) + 1
            parseFailures[pluginId] = count
            Log.d(TAG, "[$pluginId] Yapısal/Ayrıştırma hatası tespit edildi (${error.javaClass.simpleName}) — #$count/$PARSE_BLOCK_THRESHOLD (Bloklanmıyor, Sorguya özel olabilir).")
            if (count >= PARSE_BLOCK_THRESHOLD) {
                Log.w(TAG, "[$pluginId] $PARSE_BLOCK_THRESHOLD tekrarlı yapısal hata — bloklanıyor (plugin JSON yapısı bozuk).")
                Log.e(PLUGIN_DIAG, "🚫 BLOK [$pluginId] — $count/$PARSE_BLOCK_THRESHOLD PARSE/NPE hatası — ${error.javaClass.simpleName}: ${error.message}")
                blocklist.add(pluginId)
            }
        } else {
            // Real error (network, HTTP 5xx, etc.) — use cumulative threshold
            val count = (failures[pluginId] ?: 0) + 1
            failures[pluginId] = count
            Log.d(TAG, "[$pluginId] Gerçek hata #$count/3: ${error.javaClass.simpleName}")
            if (count >= 3) {
                Log.w(TAG, "[$pluginId] 3 gerçek hata — bloklanıyor.")
                Log.e(PLUGIN_DIAG, "🚫 BLOK [$pluginId] — 3/3 Ağ/HTTP hatası — Son hata: ${error.javaClass.simpleName}: ${error.message}")
                blocklist.add(pluginId)
            }
        }
    }

    fun isBlocked(pluginId: String): Boolean = blocklist.contains(pluginId)

    fun getErrorMessage(pluginId: String): String? = errorMessages[pluginId]

    fun clearPluginStatus(pluginId: String) {
        failures.remove(pluginId)
        notImplCount.remove(pluginId)
        parseFailures.remove(pluginId)
        blocklist.remove(pluginId)
        errorMessages.remove(pluginId)
        Log.d(TAG, "[$pluginId] durum takibi sıfırlandı.")
    }

    fun clear() {
        failures.clear()
        notImplCount.clear()
        parseFailures.clear()
        blocklist.clear()
        errorMessages.clear()
        Log.d(TAG, "Plugin durum takibi sıfırlandı.")
    }
}
