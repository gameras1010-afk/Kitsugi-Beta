package com.kitsugi.animelist.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

object KitsugiTranslateUtils {

    /**
     * Tries to open the text in available translator apps in priority order:
     * 1. DeepL (mini)
     * 2. DeepL (full app)
     * 3. Google Translate (mini / tap-to-translate)
     * 4. Google Translate (full activity)
     * 5. TranslateYou (open source)
     * Shows a toast if no translator app is found.
     */
    fun Context.openTranslator(text: String) {
        if (!openInDeepLMini(text)
            && !openInDeepL(text)
            && !openInGoogleTranslateMini(text)
            && !openInGoogleTranslate(text)
            && !openInTranslateYou(text)
        ) {
            Toast.makeText(this, "Çeviri uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Context.openInGoogleTranslate(text: String): Boolean {
        return try {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra("key_text_input", text)
                putExtra("key_text_output", "")
                putExtra("key_language_from", "en")
                putExtra("key_language_to", "tr")
                putExtra("key_suggest_translation", "")
                putExtra("key_from_floating_window", false)
                component = ComponentName(
                    "com.google.android.apps.translate",
                    "com.google.android.apps.translate.TranslateActivity"
                )
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun Context.openInGoogleTranslateMini(text: String): Boolean {
        return try {
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                component = ComponentName(
                    "com.google.android.apps.translate",
                    "com.google.android.apps.translate.copydrop.gm3.TapToTranslateActivity"
                )
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun Context.openInDeepL(text: String): Boolean {
        return try {
            // DeepL doesn't support direct text injection, so we copy to clipboard first
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("synopsis", text))
            Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(
                    "com.deepl.mobiletranslator",
                    "com.deepl.mobiletranslator.MainActivity"
                )
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun Context.openInDeepLMini(text: String): Boolean {
        return try {
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                component = ComponentName(
                    "com.deepl.mobiletranslator",
                    "com.deepl.mobiletranslator.MiniTranslatorActivity"
                )
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun Context.openInTranslateYou(text: String): Boolean {
        return try {
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                component = ComponentName(
                    "com.bnyro.translate",
                    "com.bnyro.translate.ui.ShareActivity"
                )
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                startActivity(this)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
