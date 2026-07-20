package com.kitsugi.animelist.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.components.KitsugiExploreMediaCard
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * V2-D03 – GridHomeContent
 *
 * Ana ekran grid görünümü — 2-3 sütunlu lazy vertical grid.
 * Her kart için KitsugiExploreMediaCard kullanılır.
 *
 * @param title         Bölüm başlığı
 * @param results       Gösterilecek Jikan/TMDB medya listesi
 * @param columns       Sütun sayısı (2 = telefon, 3 = tablet/yatay)
 * @param isLoading     Yükleme durumu
 * @param onItemClick   Kart tıklama geri bildirimi
 * @param alreadyInList Kartın listede işaretli gösterilip gösterilmeyeceğini belirler
 * @param modifier      Dışarıdan gelen modifier
 */
@Composable
fun GridHomeContent(
    title: String,
    results: List<JikanSearchResult>,
    isLoading: Boolean = false,
    columns: Int = 2,
    alreadyInList: (JikanSearchResult) -> Boolean = { false },
    onItemClick: (JikanSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        when {
            isLoading && results.isEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.heightIn(max = 600.dp)
                ) {
                    items(List(columns * 3) { it.toString() }) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = KitsugiColors.SurfaceSoft,
                            modifier = Modifier
                                .aspectRatio(0.67f)
                                .fillMaxWidth()
                        ) {}
                    }
                }
            }

            results.isEmpty() -> {
                Text(
                    text = "Gösterilecek içerik yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KitsugiColors.TextMuted,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    items(results, key = { it.malId }) { item ->
                        KitsugiExploreMediaCard(
                            result = item,
                            alreadyInList = alreadyInList(item),
                            onClick = { onItemClick(item) },
                            forceVertical = true
                        )
                    }
                }
            }
        }
    }
}

// Geriye dönük uyumluluk — String tabanlı eski çağrı imzası
@Composable
fun GridHomeContent(
    items: List<String>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Popüler Keşifler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KitsugiColors.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Card(
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(text = item, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
