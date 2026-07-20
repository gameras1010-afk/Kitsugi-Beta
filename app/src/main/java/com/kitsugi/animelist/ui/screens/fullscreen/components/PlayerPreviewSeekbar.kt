package com.kitsugi.animelist.ui.screens.fullscreen.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import kotlinx.coroutines.delay

/**
 * T2.2: PlayerPreviewSeekbar
 *
 * Seek barı sürüklenirken thumbnail önizleme gösterir.
 * PreviewGenerator'dan alınan bitmap'i popup halinde gösterir.
 *
 * @param positionMs Mevcut oynatma pozisyonu (ms)
 * @param durationMs Toplam süre (ms)
 * @param onSeek Seek çağrıldığında pozisyon (ms) iletilir
 * @param previewEnabled Thumbnail önizleme açık mı?
 * @param getThumbnail Verilen saniye için bitmap döndüren fonksiyon (suspend)
 */
@Composable
fun PlayerPreviewSeekbar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    previewEnabled: Boolean = true,
    getThumbnail: (suspend (Long) -> Bitmap?)? = null,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0L) return

    val accentColor = LocalKitsugiAccent.current

    // Sürükleme durumu
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var thumbnailVisible by remember { mutableStateOf(false) }
    var thumbOffsetFraction by remember { mutableFloatStateOf(0f) }

    // Dışarıdan gelen pozisyon değişimini takip et (sürükleme yokken)
    LaunchedEffect(positionMs, isDragging) {
        if (!isDragging) {
            sliderValue = positionMs.toFloat()
        }
    }

    // Thumbnail yükleme (debounce 300ms)
    LaunchedEffect(sliderValue, isDragging) {
        if (isDragging && previewEnabled && getThumbnail != null) {
            delay(300L)
            val sec = (sliderValue / 1000L).toLong()
            thumbnailBitmap = getThumbnail(sec)
            thumbnailVisible = thumbnailBitmap != null
        } else if (!isDragging) {
            thumbnailVisible = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val widthPx = constraints.maxWidth
            // Thumbnail popup
            androidx.compose.animation.AnimatedVisibility(
                visible = thumbnailVisible && isDragging,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val thumbX = (thumbOffsetFraction * widthPx).toInt() - 72.dp.roundToPx()
                        IntOffset(thumbX.coerceIn(0, widthPx - 144.dp.roundToPx()), 0)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bmp = thumbnailBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Önizleme",
                            modifier = Modifier
                                .size(width = 144.dp, height = 81.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 144.dp, height = 81.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(KitsugiColors.SurfaceSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatPreviewMs(sliderValue.toLong()),
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatPreviewMs(sliderValue.toLong()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Slider
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                isDragging = true
                sliderValue = newValue
                // Thumb fraction hesapla (0..1)
                thumbOffsetFraction = if (durationMs > 0) newValue / durationMs.toFloat() else 0f
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(sliderValue.toLong())
                thumbnailVisible = false
            },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Zaman etiketleri
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPreviewMs(sliderValue.toLong()),
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatPreviewMs(durationMs),
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** ms → MM:SS veya HH:MM:SS formatı */
private fun formatPreviewMs(ms: Long): String {
    val totalSec = ms / 1000L
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
