package com.kitsugi.animelist.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.components.KitsugiShimmerMediaRow
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * V2-D03 – ModernHomeRows
 *
 * Ana ekran için TMDB / Jikan içeriklerini gösteren modern yatay satır bileşeni.
 * - Shimmer yükleme desteği
 * - Poster + başlık + puan overlay içeren kart
 * - "Tümünü Gör" butonu
 * - LazyRow ile performanslı scroll
 */
@Composable
fun ModernHomeRows(
    title: String,
    results: List<JikanSearchResult>,
    isLoading: Boolean = false,
    onItemClick: (JikanSearchResult) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    cardWidth: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val accent = LocalKitsugiAccent.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Başlık + Tümünü Gör
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onSeeAllClick != null && (results.isNotEmpty() || isLoading)) {
                TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(
                        text = "Tümünü Gör",
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading && results.isEmpty() -> {
                KitsugiShimmerMediaRow(cardCount = 5)
            }
            results.isEmpty() -> {
                Text(
                    text = "İçerik bulunamadı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KitsugiColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            else -> {
                val rowState = androidx.compose.runtime.remember(title) { androidx.compose.foundation.lazy.LazyListState() }
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                        lazyListState = rowState,
                        snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                    )
                ) {
                    items(results, key = { "${it.source}_${it.malId}" }) { item ->
                        ModernHomeMediaCard(
                            item = item,
                            cardWidth = cardWidth,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tek bir medya kartı — poster + gradient overlay + başlık + puan.
 */
@Composable
private fun ModernHomeMediaCard(
    item: JikanSearchResult,
    cardWidth: Dp,
    onClick: () -> Unit
) {
    val cardHeight = cardWidth * 1.5f
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = cardWidth, height = cardHeight),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster resmi
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Alt gradient + metin overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.score != null && item.score > 0.0) {
                        Text(
                            text = "⭐ ${"%.1f".format(item.score)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// Geriye dönük uyumluluk için String tabanlı eski imza
@Composable
fun ModernHomeRows(
    title: String,
    items: List<String>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KitsugiColors.TextPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .size(width = 140.dp, height = 210.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(text = item, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
