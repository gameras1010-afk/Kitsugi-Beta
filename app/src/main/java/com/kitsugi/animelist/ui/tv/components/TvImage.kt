package com.kitsugi.animelist.ui.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * Stateless TV image component — KitsugiTV pattern.
 *
 * SIFIR recomposition: No onLoading/onSuccess/onError callbacks.
 * Placeholder ve error state'leri Painter ile yönetilir (callback yok = state yok).
 * Coil3 ImageRequest ile boyut bazlı cache key, hardware bitmap, RGB565 desteği.
 */
@Composable
fun TvImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shimmerOffsetState: State<Float>? = null,   // Geriye dönük uyumluluk için kalır, artık kullanılmaz
    backgroundColor: Color = KitsugiColors.Surface,
    onError: (() -> Unit)? = null,              // Opsiyonel, sadece özel durumlar
    requestWidthPx: Int? = null,                 // YENİ: Decode boyutu
    requestHeightPx: Int? = null                 // YENİ: Decode boyutu
) {
    val context = LocalContext.current
    val placeholderPainter = remember(backgroundColor) { ColorPainter(backgroundColor) }

    val imageRequest = remember(model, requestWidthPx, requestHeightPx) {
        val builder = ImageRequest.Builder(context)
            .data(model)
            .crossfade(false)                    // TV'de kapalı (performans)
            .allowHardware(true)                 // GPU bitmap → UI thread bloklamaz
            .allowRgb565(true)                   // %50 bellek tasarrufu
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)

        if (requestWidthPx != null && requestHeightPx != null) {
            builder.size(width = requestWidthPx, height = requestHeightPx)
        }

        // Boyut bazlı cache key — aynı resim farklı boyutta tekrar decode olmaz
        if (model is String && requestWidthPx != null) {
            builder.memoryCacheKey("${model}_${requestWidthPx}x${requestHeightPx}")
        }

        builder.build()
    }

    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            placeholder = placeholderPainter,     // STATELESS! Recomposition yok
            error = placeholderPainter,           // STATELESS!
            fallback = placeholderPainter,        // STATELESS!
            onError = { onError?.invoke() }       // Sadece dış callback, iç state yok
        )
    }
}
