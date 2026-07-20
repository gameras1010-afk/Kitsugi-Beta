package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.components.KitsugiShimmerMediaRow
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

@Composable
fun RecommendationsTabContent(
    state: DetailTabState<List<KitsugiRelation>>,
    titleLanguage: String = "ROMAJI",
    onRecommendationClick: (KitsugiRelation) -> Unit
) {
    when (state) {
        is DetailTabState.Loading -> {
            KitsugiShimmerMediaRow(cardCount = 4)
        }
        is DetailTabState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Öneriler yüklenirken hata oluştu.",
                    color = KitsugiColors.AccentRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        is DetailTabState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Benzer yapım önerisi bulunamadı.",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Buna Benzer Yapımlar",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(list) { rel ->
                            RecommendationCard(rel, titleLanguage, onRecommendationClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    rel: KitsugiRelation,
    titleLanguage: String = "ROMAJI",
    onClick: (KitsugiRelation) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = rel.getDisplayTitle(titleLanguage)
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = { onClick(rel) })
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!rel.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = rel.imageUrl,
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = displayTitle.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayTitle,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        val typeLabel = when (rel.mediaType) {
            MediaType.Anime -> "Anime"
            MediaType.Manga -> "Manga"
            MediaType.Movie -> "Film"
            MediaType.TvShow -> "Dizi"
        }
        Text(
            text = typeLabel,
            color = accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
