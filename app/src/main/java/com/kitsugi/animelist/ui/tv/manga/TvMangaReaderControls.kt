package com.kitsugi.animelist.ui.tv.manga

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.manga.ColorFilterType
import com.kitsugi.animelist.data.manga.MangaFitMode
import com.kitsugi.animelist.data.manga.ReadingMode
import com.kitsugi.animelist.ui.screens.manga.MangaReaderViewModel
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.tvClickable

/**
 * TV-Native D-pad uyumlu Manga Okuyucu Ayarlar Overlay Paneli.
 *
 * — Ekranın sağ tarafında 360dp genişliğinde şık bir panel olarak açılır.
 * — Seçenekler dikey olarak hizalanmıştır ve D-pad ile gezinilebilir.
 * — Her satırda D-pad Sol/Sağ basılarak ayarlar anında döngüsel olarak değiştirilir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMangaReaderControls(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Panel dışına tıklanınca kapatmak için arka plana tıklama veya D-pad geri tuşunu dinleme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .tvClickable(enabled = true, onClick = onDismiss)
    ) {
        // Sağ panel gövdesi
        Column(
            modifier = Modifier
                .width(380.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .background(KitsugiColors.BackgroundElevated)
                .border(1.dp, Color.White.copy(alpha = 0.1f))
                .tvClickable(enabled = true, onClick = {}) // Tıklamayı arka plana geçirmesin
                .padding(24.dp)
        ) {
            Text(
                text = "Okuyucu Ayarları",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 1. Okuma Yönü
            val readingModes = listOf(
                ReadingMode.RightToLeft to "Sağdan Sola (Manga)",
                ReadingMode.LeftToRight to "Soldan Sağa",
                ReadingMode.Vertical to "Dikey",
                ReadingMode.Webtoon to "Webtoon"
            )
            val currentModeIdx = readingModes.indexOfFirst { it.first == uiState.readingMode }.coerceAtLeast(0)
            TvControlCycleRow(
                title = "Okuma Yönü",
                description = "Manga veya Webtoon okuma akışını değiştirin.",
                currentValueText = readingModes[currentModeIdx].second,
                onPrevious = {
                    val prevIdx = (currentModeIdx - 1 + readingModes.size) % readingModes.size
                    viewModel.setReadingMode(readingModes[prevIdx].first)
                },
                onNext = {
                    val nextIdx = (currentModeIdx + 1) % readingModes.size
                    viewModel.setReadingMode(readingModes[nextIdx].first)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Görsel Sığdırma Modu
            val fitModes = listOf(
                MangaFitMode.FitScreen to "Ekrana Sığdır",
                MangaFitMode.FitWidth to "Genişliğe Sığdır",
                MangaFitMode.FitHeight to "Yükseğe Sığdır"
            )
            val currentFitIdx = fitModes.indexOfFirst { it.first == uiState.fitMode }.coerceAtLeast(0)
            TvControlCycleRow(
                title = "Görsel Sığdırma",
                description = "Sayfaların ekrana sığdırılma kuralını belirleyin.",
                currentValueText = fitModes[currentFitIdx].second,
                onPrevious = {
                    val prevIdx = (currentFitIdx - 1 + fitModes.size) % fitModes.size
                    viewModel.setFitMode(fitModes[prevIdx].first)
                },
                onNext = {
                    val nextIdx = (currentFitIdx + 1) % fitModes.size
                    viewModel.setFitMode(fitModes[nextIdx].first)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Renk Filtresi
            val colorFilters = listOf(
                ColorFilterType.Normal to "Normal",
                ColorFilterType.Grayscale to "Siyah-Beyaz",
                ColorFilterType.Sepia to "Sepya",
                ColorFilterType.Invert to "Ters Negatif"
            )
            val currentFilterIdx = colorFilters.indexOfFirst { it.first == uiState.colorFilterType }.coerceAtLeast(0)
            TvControlCycleRow(
                title = "Renk Filtresi",
                description = "Sayfa renklerini göz sağlığına göre filtreleyin.",
                currentValueText = colorFilters[currentFilterIdx].second,
                onPrevious = {
                    val prevIdx = (currentFilterIdx - 1 + colorFilters.size) % colorFilters.size
                    viewModel.setColorFilter(colorFilters[prevIdx].first)
                },
                onNext = {
                    val nextIdx = (currentFilterIdx + 1) % colorFilters.size
                    viewModel.setColorFilter(colorFilters[nextIdx].first)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Parlaklık Ayarı
            val brightnessValues = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
            val currentBrightnessIdx = brightnessValues.indexOfFirst { it >= uiState.customBrightness }.coerceAtLeast(0)
            val brightnessPercent = "${(uiState.customBrightness * 100).toInt()}%"
            TvControlCycleRow(
                title = "Okuyucu Parlaklığı",
                description = "TV ekranı üzerinde yapay karartma uygulayın.",
                currentValueText = brightnessPercent,
                onPrevious = {
                    if (currentBrightnessIdx > 0) {
                        viewModel.setCustomBrightness(brightnessValues[currentBrightnessIdx - 1])
                    }
                },
                onNext = {
                    if (currentBrightnessIdx < brightnessValues.size - 1) {
                        viewModel.setCustomBrightness(brightnessValues[currentBrightnessIdx + 1])
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Kapatma butonu
            var isCloseFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCloseFocused) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.1f)
                    )
                    .tvClickable(shape = RoundedCornerShape(8.dp)) { onDismiss() }
                    .onFocusChanged { isCloseFocused = it.isFocused },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kapat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Segment/Döngü kontrollü D-pad satırı. Sol/Sağ tuşları ile değeri kaydırır.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun TvControlCycleRow(
    title: String,
    description: String,
    currentValueText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) Color.White.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onNext()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Değer Değiştirici Bölge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = currentValueText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
