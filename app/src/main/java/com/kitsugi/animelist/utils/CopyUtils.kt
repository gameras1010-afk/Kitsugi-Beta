package com.kitsugi.animelist.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Çift tıklandığında [text] değerini panoya kopyalayan Modifier extension'ı.
 * Tüm uygulama genelinde kullanılır.
 */
fun Modifier.copyOnDoubleTap(context: Context, text: String): Modifier =
    this.pointerInput(text) {
        detectTapGestures(
            onDoubleTap = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Kitsugi_copy", text))
                Toast.makeText(context, "Kopyalandı: $text", Toast.LENGTH_SHORT).show()
            }
        )
    }
