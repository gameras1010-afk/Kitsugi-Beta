package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

internal data class StatusFilter(
    val id: String,
    val title: String,
    val status: WatchStatus?
)

internal data class TypeFilter(
    val id: String,
    val title: String,
    val type: MediaType?
)

internal data class FavoriteFilter(
    val id: String,
    val title: String
)

internal data class SortOption(
    val id: String,
    val title: String
)

internal data class ScoreFilter(
    val id: String,
    val title: String
)

internal data class YearFilter(
    val id: String,
    val title: String
)

internal data class ExtraFilter(
    val id: String,
    val title: String
)

internal val statusFilters = listOf(
    StatusFilter("all", "Tümü", null),
    StatusFilter("watching", "İzleniyor", WatchStatus.Watching),
    StatusFilter("completed", "Tamamlandı", WatchStatus.Completed),
    StatusFilter("planned", "Planlandı", WatchStatus.Planned),
    StatusFilter("dropped", "Bırakıldı", WatchStatus.Dropped),
    StatusFilter("paused", "Durduruldu", WatchStatus.Paused)
)

internal val typeFilters = listOf(
    TypeFilter("all", "Tümü", null),
    TypeFilter("anime", "Anime", MediaType.Anime),
    TypeFilter("manga", "Manga", MediaType.Manga),
    TypeFilter("movie", "Film", MediaType.Movie),
    TypeFilter("tvshow", "Dizi", MediaType.TvShow)
)

internal val favoriteFilters = listOf(
    FavoriteFilter("all", "Tümü"),
    FavoriteFilter("favorites", "Favoriler")
)

internal val scoreFilters = listOf(
    ScoreFilter("all", "Tümü"),
    ScoreFilter("high", "Çok İyi (8-10)"),
    ScoreFilter("mid", "İyi (5-7)"),
    ScoreFilter("low", "Düşük (1-4)"),
    ScoreFilter("unrated", "Puansız")
)

internal val yearFilters = listOf(
    YearFilter("all", "Tümü"),
    YearFilter("new", "2025 ve Sonrası"),
    YearFilter("2020s", "2020 - 2024"),
    YearFilter("2010s", "2010'lar"),
    YearFilter("2000s", "2000'ler"),
    YearFilter("classic", "90'lar ve Öncesi")
)

internal val extraFilters = listOf(
    ExtraFilter("all", "Tümü"),
    ExtraFilter("repeating", "Tekrar İzlenenler"),
    ExtraFilter("private", "Özel / Gizli"),
    ExtraFilter("ongoing", "Tamamlanmamış")
)

internal val sortOptions = listOf(
    SortOption("newest", "Son eklenen"),
    SortOption("oldest", "İlk eklenen"),
    SortOption("title", "Ada göre"),
    SortOption("score", "Puan"),
    SortOption("progress", "İlerleme"),
    SortOption("favorites", "Favoriler"),
    SortOption("start_date_desc", "Başlangıç (Yeni)"),
    SortOption("start_date_asc", "Başlangıç (Eski)"),
    SortOption("end_date_desc", "Bitiş (Yeni)"),
    SortOption("end_date_asc", "Bitiş (Eski)"),
    SortOption("year_desc", "Yayın Yılı (Yeni)"),
    SortOption("year_asc", "Yayın Yılı (Eski)"),
    SortOption("updated_desc", "Son Güncelleme"),
    SortOption("priority_desc", "Öncelik")
)

@Composable
internal fun AddActionButton(
    title: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                color = if (primary) {
                    accentColor
                } else {
                    KitsugiColors.Surface
                }
            )
            .tvClickable(shape = RoundedCornerShape(22.dp), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (primary) {
                KitsugiColors.Background
            } else {
                accentColor
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
internal fun FilterLabel(
    text: String
) {
    Text(
        text = text.uppercase(),
        color = KitsugiColors.TextMuted,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
internal fun StatusFilterRow(
    selectedStatusFilterId: String,
    onStatusSelected: (String) -> Unit
) {
    FilterChipRow(
        items = statusFilters.map { it.id to it.title },
        selectedId = selectedStatusFilterId,
        onSelected = onStatusSelected
    )
}

@Composable
internal fun TypeFilterRow(
    selectedTypeFilterId: String,
    onTypeSelected: (String) -> Unit
) {
    FilterChipRow(
        items = typeFilters.map { it.id to it.title },
        selectedId = selectedTypeFilterId,
        onSelected = onTypeSelected
    )
}

@Composable
internal fun FavoriteFilterRow(
    selectedFavoriteFilterId: String,
    onFavoriteFilterSelected: (String) -> Unit
) {
    FilterChipRow(
        items = favoriteFilters.map { it.id to it.title },
        selectedId = selectedFavoriteFilterId,
        onSelected = onFavoriteFilterSelected
    )
}

@Composable
internal fun ScoreFilterRow(
    selectedScoreFilterId: String,
    onScoreSelected: (String) -> Unit
) {
    FilterChipRow(
        items = scoreFilters.map { it.id to it.title },
        selectedId = selectedScoreFilterId,
        onSelected = onScoreSelected
    )
}

@Composable
internal fun YearFilterRow(
    selectedYearFilterId: String,
    onYearSelected: (String) -> Unit
) {
    FilterChipRow(
        items = yearFilters.map { it.id to it.title },
        selectedId = selectedYearFilterId,
        onSelected = onYearSelected
    )
}

@Composable
internal fun ExtraFilterRow(
    selectedExtraFilterId: String,
    onExtraSelected: (String) -> Unit
) {
    FilterChipRow(
        items = extraFilters.map { it.id to it.title },
        selectedId = selectedExtraFilterId,
        onSelected = onExtraSelected
    )
}

@Composable
internal fun SortFilterRow(
    selectedSortId: String,
    onSortSelected: (String) -> Unit
) {
    FilterChipRow(
        items = sortOptions.map { it.id to it.title },
        selectedId = selectedSortId,
        onSelected = onSortSelected
    )
}

@Composable
internal fun RichMyListFilterPanel(
    selectedStatusFilterId: String,
    selectedTypeFilterId: String,
    selectedFavoriteFilterId: String,
    selectedScoreFilterId: String,
    selectedYearFilterId: String,
    selectedExtraFilterId: String,
    selectedSortId: String,
    onStatusSelected: (String) -> Unit,
    onTypeSelected: (String) -> Unit,
    onFavoriteSelected: (String) -> Unit,
    onScoreSelected: (String) -> Unit,
    onYearSelected: (String) -> Unit,
    onExtraSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    onResetFilters: () -> Unit,
    onHideFilters: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KitsugiColors.Surface.copy(alpha = 0.65f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Content Types
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeItems = listOf(
                "anime" to "✓ Anime",
                "manga" to "Manga",
                "characters" to "Karakterler",
                "staff" to "Ekip",
                "studios" to "Stüdyo"
            )
            typeItems.forEach { (id, label) ->
                val isSelected = selectedTypeFilterId == id || (id == "anime" && selectedTypeFilterId == "all")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor else KitsugiColors.SurfaceSoft)
                        .tvClickable(shape = RoundedCornerShape(12.dp), onClick = {
                            if (id == "anime" || id == "manga") onTypeSelected(id)
                        })
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section: Status Filters
        Column {
            FilterLabel(text = "Durum")
            Spacer(modifier = Modifier.height(6.dp))
            StatusFilterRow(
                selectedStatusFilterId = selectedStatusFilterId,
                onStatusSelected = onStatusSelected
            )
        }

        // Section: Media Type
        Column {
            FilterLabel(text = "Medya Türü")
            Spacer(modifier = Modifier.height(6.dp))
            TypeFilterRow(
                selectedTypeFilterId = selectedTypeFilterId,
                onTypeSelected = onTypeSelected
            )
        }

        // Section: Score Range
        Column {
            FilterLabel(text = "Puan Aralığı")
            Spacer(modifier = Modifier.height(6.dp))
            ScoreFilterRow(
                selectedScoreFilterId = selectedScoreFilterId,
                onScoreSelected = onScoreSelected
            )
        }

        // Section: Release Year
        Column {
            FilterLabel(text = "Yayın Yılı")
            Spacer(modifier = Modifier.height(6.dp))
            YearFilterRow(
                selectedYearFilterId = selectedYearFilterId,
                onYearSelected = onYearSelected
            )
        }

        // Section: Extra Options
        Column {
            FilterLabel(text = "Ekstra / Özel")
            Spacer(modifier = Modifier.height(6.dp))
            ExtraFilterRow(
                selectedExtraFilterId = selectedExtraFilterId,
                onExtraSelected = onExtraSelected
            )
        }

        // Action Buttons: Hide & Reset Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onHideFilters)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Filtreleri gizle",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onResetFilters)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Temizle",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    items: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            ListFilterChip(
                title = item.second,
                selected = selectedId == item.first,
                onClick = {
                    onSelected(item.first)
                }
            )
        }
    }
}

@Composable
private fun ListFilterChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                color = if (selected) {
                    accentColor
                } else {
                    KitsugiColors.Surface
                }
            )
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) {
                KitsugiColors.Background
            } else {
                KitsugiColors.TextSecondary
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
internal fun EmptyListResultCard(
    searchQuery: String,
    selectedStatusFilterId: String,
    selectedTypeFilterId: String,
    selectedFavoriteFilterId: String,
    selectedScoreFilterId: String,
    selectedYearFilterId: String,
    selectedExtraFilterId: String,
    selectedSortId: String
) {
    val activeStatusTitle = statusFilters.firstOrNull { it.id == selectedStatusFilterId }?.title ?: "Tümü"
    val activeTypeTitle = typeFilters.firstOrNull { it.id == selectedTypeFilterId }?.title ?: "Tümü"
    val activeFavoriteTitle = favoriteFilters.firstOrNull { it.id == selectedFavoriteFilterId }?.title ?: "Tümü"
    val activeScoreTitle = scoreFilters.firstOrNull { it.id == selectedScoreFilterId }?.title ?: "Tümü"
    val activeYearTitle = yearFilters.firstOrNull { it.id == selectedYearFilterId }?.title ?: "Tümü"
    val activeExtraTitle = extraFilters.firstOrNull { it.id == selectedExtraFilterId }?.title ?: "Tümü"
    val activeSortTitle = sortOptions.firstOrNull { it.id == selectedSortId }?.title ?: "Son eklenen"

    KitsugiEmptyState(
        title = "Sonuç bulunamadı",
        subtitle = if (searchQuery.isBlank()) {
            "Seçili filtrelerle içerik bulunamadı."
        } else {
            "\"$searchQuery\" için içerik bulunamadı."
        },
        icon = Icons.Rounded.FilterList
    )
}
