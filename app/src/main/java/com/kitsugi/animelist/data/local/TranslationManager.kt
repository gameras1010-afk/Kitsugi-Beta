package com.kitsugi.animelist.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import okhttp3.Request
import java.security.MessageDigest

class TranslationManager(context: Context) {
    private val db = KitsugiDatabase.getDatabase(context.applicationContext)
    private val dao = db.translationCacheDao()

    /**
     * Verilen metnin büyük olasılıkla Türkçe olup olmadığını kontrol eder.
     * Türkçe'ye özgü karakterler (ğ, ş, ı, ü, ö, ç) veya yaygın Türkçe kelimeler içeriyorsa true döner.
     */
    private fun isLikelyTurkish(text: String): Boolean {
        val turkishChars = setOf('ğ', 'Ğ', 'ş', 'Ş', 'ı', 'İ', 'ü', 'Ü', 'ö', 'Ö', 'ç', 'Ç')
        // Türkçe'ye özgü karakter varsa kesinlikle Türkçe
        if (text.any { it in turkishChars }) return true
        // Yaygın Türkçe kelimeler varsa büyük ihtimalle Türkçe
        val turkishWords = setOf(
            "bir", "ve", "bu", "da", "de", "için", "ile", "olan", "sonra",
            "ama", "çok", "daha", "gibi", "kadar", "ya", "ne", "her",
            "olarak", "ancak", "zaman", "tarafından"
        )
        val words = text.lowercase().split(" ", "\n", "\t")
            .map { it.trim('.', ',', '!', '?', ';', ':') }
        return words.count { it in turkishWords } >= 2
    }

    suspend fun translateToTurkish(text: String?): String = withContext(Dispatchers.IO) {
        if (text.isNullOrBlank()) return@withContext ""
        
        val trimmed = text.trim()

        // Metin zaten Türkçeyse çevirme — gereksiz API çağrısını önle
        if (isLikelyTurkish(trimmed)) return@withContext trimmed

        val hash = md5(trimmed)

        // 1. Check local Room database cache
        val cached = runCatching { dao.getTranslation(hash) }.getOrNull()
        if (cached != null) {
            return@withContext cached
        }

        // 2. If not cached, fetch translation from Google Translate
        val translated = fetchFromGoogle(trimmed)
        
        // 3. Cache the success response in DB
        if (translated.isNotBlank() && translated != trimmed) {
            runCatching {
                dao.insertTranslation(TranslationCacheEntity(hash, translated))
            }
            return@withContext translated
        }

        return@withContext trimmed
    }

    private fun fetchFromGoogle(text: String): String {
        return try {
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=tr&dt=t&q=" + 
                    URLEncoder.encode(text, "UTF-8")
            val request = Request.Builder()
                .url(urlStr)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: return text
                    val jsonArray = JSONArray(responseText)
                    val sentences = jsonArray.optJSONArray(0) ?: return text
                    val result = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val sentence = sentences.optJSONArray(i)
                        val translatedPart = sentence?.optString(0)
                        if (translatedPart != null) {
                            result.append(translatedPart)
                        }
                    }
                    result.toString()
                } else {
                    text
                }
            }
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("TranslationManager", "fetchFromGoogle failed: ${e.message}", e)
            text
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
