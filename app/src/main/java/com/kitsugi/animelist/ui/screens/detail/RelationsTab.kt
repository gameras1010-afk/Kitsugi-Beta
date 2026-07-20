package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
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
import com.kitsugi.animelist.ui.components.KitsugiShimmerSearchResultList
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

@Composable
fun RelationsTabContent(
    state: DetailTabState<List<KitsugiRelation>>,
    titleLanguage: String = "ROMAJI",
    onRelationClick: (KitsugiRelation) -> Unit
) {
    when (state) {
        is DetailTabState.Loading -> {
            KitsugiShimmerSearchResultList(itemCount = 3)
        }
        is DetailTabState.Error -> {
            Text(
                text = "İlişkili yapımlar yüklenirken hata oluştu.",
                color = KitsugiColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Text(
                    text = "İlişkili yapım bulunamadı.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    list.forEach { rel ->
                        RelationCard(rel, titleLanguage, onRelationClick)
                    }
                }
            }
        }
    }
}

@Composable
fun RelationCard(
    rel: KitsugiRelation,
    titleLanguage: String = "ROMAJI",
    onClick: (KitsugiRelation) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = rel.getDisplayTitle(titleLanguage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onClick(rel) })
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 70.dp)
                .clip(RoundedCornerShape(8.dp))
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
                    Text(displayTitle.take(2).uppercase(), color = KitsugiColors.TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayTitle,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val typeLabel = when (rel.mediaType) {
                MediaType.Anime -> "Anime"
                MediaType.Manga -> "Manga"
                MediaType.Movie -> "Film"
                MediaType.TvShow -> "Dizi"
            }
            Text(
                text = "${rel.relationType} • $typeLabel",
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
