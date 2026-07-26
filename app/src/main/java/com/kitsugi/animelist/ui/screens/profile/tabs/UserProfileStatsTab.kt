package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.app.RankedStatItem
import com.kitsugi.animelist.ui.screens.profile.FilterChipItem
import com.kitsugi.animelist.ui.screens.profile.HorizontalStatsBar
import com.kitsugi.animelist.ui.screens.profile.KitsugiUserProfileViewModel
import com.kitsugi.animelist.ui.screens.profile.OtherUserProfileState
import com.kitsugi.animelist.ui.screens.profile.PositionalStatItemCard
import com.kitsugi.animelist.ui.screens.profile.ProfileFilterChip
import com.kitsugi.animelist.ui.screens.profile.StatCard
import com.kitsugi.animelist.ui.screens.profile.VerticalStatsBar
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.toEnglishGenreForSearch
import com.kitsugi.animelist.utils.toTurkishGenre

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserProfileStatsTab(
    state: OtherUserProfileState,
    accentColor: Color,
    viewModel: KitsugiUserProfileViewModel,
    onGenreClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onFavoriteStaffClick: (Int, String, String?, String?) -> Unit,
    onFavoriteStudioClick: ((Int, String, String?, String?) -> Unit)?
) {
    val statsMediaType = viewModel.statsMediaType
    val statsSubTab = viewModel.statsSubTab
    var scoreDistType by remember { mutableStateOf(0) }
    var lengthDistType by remember { mutableStateOf(0) }
    var releaseYearDistType by remember { mutableStateOf(0) }
    var startYearDistType by remember { mutableStateOf(0) }

    val overview = if (statsMediaType == 0) state.animeOverviewStats else state.mangaOverviewStats
    if (statsSubTab == 0) {
        if (overview != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(KitsugiColors.Surface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Media Switcher (Anime / Manga)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .padding(4.dp)
                ) {
                    listOf("Anime", "Manga").forEachIndexed { idx, label ->
                        val isSel = statsMediaType == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accentColor else Color.Transparent)
                                .clickable { viewModel.statsMediaType = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) KitsugiColors.Background else KitsugiColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                overview.let { ov ->
                    // 1. Key Stats Grid (3x2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard("Toplam", ov.count.toString())
                            StatCard(if (statsMediaType == 0) "İzlenen bölüm" else "Okunan bölüm", ov.episodesWatched.toString())
                            StatCard(if (statsMediaType == 0) "İzlenen gün" else "Okunan cilt", "%.1f".format(ov.daysWatched))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(if (statsMediaType == 0) "Planlanan gün" else "Planlanan bölüm", "%.1f".format(ov.plannedDaysOrCount))
                            StatCard("Ortalama Puan", "%.2f".format(ov.meanScore))
                            StatCard("Standart sapma", "%.1f".format(ov.standardDeviation))
                        }
                    }

                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                    // 2. Score Distribution
                    if (ov.scoreList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Puan",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val scoreDistListState = rememberLazyListState()
                            LazyRow(
                                state = scoreDistListState,
                                flingBehavior = rememberSnapFlingBehavior(
                                    lazyListState = scoreDistListState,
                                    snapPosition = SnapPosition.Start
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChipItem(
                                        selected = scoreDistType == 0,
                                        text = "Başlık sayısı",
                                        onClick = { scoreDistType = 0 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = scoreDistType == 1,
                                        text = "Harcanan süre",
                                        onClick = { scoreDistType = 1 },
                                        accentColor = accentColor
                                    )
                                }
                            }
                            val mappedStats = ov.scoreList.map { item ->
                                val valFloat = if (scoreDistType == 0) item.count.toFloat() else (item.minutesWatched / 60.0f)
                                item.score.toString() to valFloat
                            }
                            VerticalStatsBar(
                                stats = mappedStats,
                                accentColor = accentColor,
                                mapColorTo = { scoreStr ->
                                    val scoreNum = scoreStr.toIntOrNull() ?: 0
                                    when (scoreNum) {
                                        in 1..3 -> Color(0xFFE57373)
                                        in 4..5 -> Color(0xFFFFB74D)
                                        in 6..7 -> Color(0xFFFFD54F)
                                        in 8..9 -> Color(0xFF81C784)
                                        10 -> Color(0xFF4FC3F7)
                                        else -> accentColor
                                    }
                                }
                            )
                        }
                    }

                    // 3. Episode / Chapter Length Distribution
                    if (ov.lengthList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (statsMediaType == 0) "Bölüm Sayısı" else "Cilt/Bölüm Sayısı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val lengthDistListState = rememberLazyListState()
                            LazyRow(
                                state = lengthDistListState,
                                flingBehavior = rememberSnapFlingBehavior(
                                    lazyListState = lengthDistListState,
                                    snapPosition = SnapPosition.Start
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChipItem(
                                        selected = lengthDistType == 0,
                                        text = "Başlık sayısı",
                                        onClick = { lengthDistType = 0 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = lengthDistType == 1,
                                        text = "Harcanan süre",
                                        onClick = { lengthDistType = 1 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = lengthDistType == 2,
                                        text = "Ortalama Puan",
                                        onClick = { lengthDistType = 2 },
                                        accentColor = accentColor
                                    )
                                }
                            }
                            val mappedLength = ov.lengthList.map { item ->
                                val valFloat = when (lengthDistType) {
                                    0 -> item.count.toFloat()
                                    1 -> (item.minutesWatched / 60.0f)
                                    else -> item.meanScore.toFloat()
                                }
                                item.length to valFloat
                            }
                            VerticalStatsBar(
                                stats = mappedLength,
                                accentColor = accentColor
                            )
                        }
                    }

                    // 4. Status Distribution
                    if (ov.statusList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Durum Dağılımı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusItems = ov.statusList.map { item ->
                                val (label, color) = when (item.status.uppercase()) {
                                    "CURRENT" -> (if (statsMediaType == 0) "Şimdiki" else "Okunuyor") to Color(0xFF81C784)
                                    "COMPLETED" -> "Tamamlandı" to Color(0xFF64B5F6)
                                    "PLANNING" -> "Planlanan" to Color(0xFFA1887F)
                                    "PAUSED" -> "Durduruldu" to Color(0xFFFFB74D)
                                    "DROPPED" -> "Bırakıldı" to Color(0xFFE57373)
                                    "REPEATING" -> (if (statsMediaType == 0) "Tekrar İzleniyor" else "Tekrar Okunuyor") to Color(0xFFBA68C8)
                                    else -> item.status to accentColor
                                }
                                Triple(label, item.count, color)
                            }
                            HorizontalStatsBar(stats = statusItems)
                        }
                    }

                    // 5. Format Distribution
                    if (ov.formatList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tür Dağılımı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val formatItems = ov.formatList.map { item ->
                                val (label, color) = when (item.format.uppercase()) {
                                    "TV" -> "TV" to Color(0xFF5C6BC0)
                                    "TV_SHORT" -> "TV Kısa" to Color(0xFF7E57C2)
                                    "MOVIE" -> "Film" to Color(0xFF26A69A)
                                    "SPECIAL" -> "Özel" to Color(0xFFFFA726)
                                    "OVA" -> "OVA" to Color(0xFFFF7043)
                                    "ONA" -> "ONA" to Color(0xFFEC407A)
                                    "MUSIC" -> "Müzik Klip" to Color(0xFFAB47BC)
                                    "MANGA" -> "Manga" to Color(0xFF42A5F5)
                                    "NOVEL" -> "LN" to Color(0xFF8D6E63)
                                    "ONE_SHOT" -> "One-Shot" to Color(0xFF78909C)
                                    else -> item.format to accentColor
                                }
                                Triple(label, item.count, color)
                            }
                            HorizontalStatsBar(stats = formatItems)
                        }
                    }

                    // 6. Country Distribution
                    if (ov.countryList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Ülke Dağılımı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val countryItems = ov.countryList.map { item ->
                                val (label, color) = when (item.country.uppercase()) {
                                    "JP" -> "Japonya" to Color(0xFF5C6BC0)
                                    "KR" -> "Güney Kore" to Color(0xFF26A69A)
                                    "CN" -> "Çin" to Color(0xFFFF7043)
                                    "TW" -> "Tayvan" to Color(0xFFAB47BC)
                                    else -> item.country to accentColor
                                }
                                Triple(label, item.count, color)
                            }
                            HorizontalStatsBar(stats = countryItems)
                        }
                    }

                    // 7. Release Year Distribution
                    if (ov.releaseYearList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Yayın Yılı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val releaseYearDistListState = rememberLazyListState()
                            LazyRow(
                                state = releaseYearDistListState,
                                flingBehavior = rememberSnapFlingBehavior(
                                    lazyListState = releaseYearDistListState,
                                    snapPosition = SnapPosition.Start
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChipItem(
                                        selected = releaseYearDistType == 0,
                                        text = "Başlık sayısı",
                                        onClick = { releaseYearDistType = 0 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = releaseYearDistType == 1,
                                        text = "Harcanan süre",
                                        onClick = { releaseYearDistType = 1 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = releaseYearDistType == 2,
                                        text = "Ortalama Puan",
                                        onClick = { releaseYearDistType = 2 },
                                        accentColor = accentColor
                                    )
                                }
                            }
                            val mappedYears = ov.releaseYearList
                                .filter { it.releaseYear > 0 }
                                .sortedBy { it.releaseYear }
                                .map { item ->
                                    val valFloat = when (releaseYearDistType) {
                                        0 -> item.count.toFloat()
                                        1 -> (item.minutesWatched / 60.0f)
                                        else -> item.meanScore.toFloat()
                                    }
                                    item.releaseYear.toString() to valFloat
                                }
                            VerticalStatsBar(
                                stats = mappedYears,
                                accentColor = accentColor
                            )
                        }
                    }

                    // 8. Watch / Read Year Distribution
                    if (ov.startYearList.isNotEmpty()) {
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (statsMediaType == 0) "İzleme Yılı" else "Okuma Yılı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val startYearDistListState = rememberLazyListState()
                            LazyRow(
                                state = startYearDistListState,
                                flingBehavior = rememberSnapFlingBehavior(
                                    lazyListState = startYearDistListState,
                                    snapPosition = SnapPosition.Start
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChipItem(
                                        selected = startYearDistType == 0,
                                        text = "Başlık sayısı",
                                        onClick = { startYearDistType = 0 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = startYearDistType == 1,
                                        text = "Harcanan süre",
                                        onClick = { startYearDistType = 1 },
                                        accentColor = accentColor
                                    )
                                }
                                item {
                                    FilterChipItem(
                                        selected = startYearDistType == 2,
                                        text = "Ortalama Puan",
                                        onClick = { startYearDistType = 2 },
                                        accentColor = accentColor
                                    )
                                }
                            }
                            val mappedStartYears = ov.startYearList
                                .filter { it.startYear > 0 }
                                .sortedBy { it.startYear }
                                .map { item ->
                                    val valFloat = when (startYearDistType) {
                                        0 -> item.count.toFloat()
                                        1 -> (item.minutesWatched / 60.0f)
                                        else -> item.meanScore.toFloat()
                                    }
                                    item.startYear.toString() to valFloat
                                }
                            VerticalStatsBar(
                                stats = mappedStartYears,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "İstatistik yüklenemedi.", color = KitsugiColors.TextMuted)
            }
        }
    } else {
        val currentList: List<RankedStatItem> = when (statsSubTab) {
            1 -> overview?.genreList.orEmpty()
            2 -> overview?.tagList.orEmpty()
            3 -> overview?.staffList.orEmpty()
            4 -> overview?.voiceActorList.orEmpty()
            5 -> overview?.studioList.orEmpty()
            else -> emptyList()
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(KitsugiColors.Surface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (statsSubTab in 1..3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KitsugiColors.SurfaceStrong)
                            .padding(4.dp)
                    ) {
                        listOf("Anime", "Manga").forEachIndexed { idx, label ->
                            val isSel = statsMediaType == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) accentColor else Color.Transparent)
                                    .clickable { viewModel.statsMediaType = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) KitsugiColors.Background else KitsugiColors.TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                val sortTypeState = rememberLazyListState()
                LazyRow(
                    state = sortTypeState,
                    flingBehavior = rememberSnapFlingBehavior(
                        lazyListState = sortTypeState,
                        snapPosition = SnapPosition.Start
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ProfileFilterChip(
                            isSelected = viewModel.statsSortType == 0,
                            text = "Başlık sayısı",
                            onClick = { viewModel.statsSortType = 0 },
                            accentColor = accentColor
                        )
                    }
                    item {
                        ProfileFilterChip(
                            isSelected = viewModel.statsSortType == 1,
                            text = "Harcanan süre",
                            onClick = { viewModel.statsSortType = 1 },
                            accentColor = accentColor
                        )
                    }
                    item {
                        ProfileFilterChip(
                            isSelected = viewModel.statsSortType == 2,
                            text = "Ortalama Puan",
                            onClick = { viewModel.statsSortType = 2 },
                            accentColor = accentColor
                        )
                    }
                }
            }

            val sortedList = when (viewModel.statsSortType) {
                0 -> currentList.sortedByDescending { it.count }
                1 -> currentList.sortedByDescending { it.timeSpentMinutes }
                else -> currentList.sortedByDescending { it.meanScore }
            }

            if (sortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "İstatistik verisi bulunamadı.", color = KitsugiColors.TextMuted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedList.forEachIndexed { idx, item ->
                        PositionalStatItemCard(
                            rank = idx + 1,
                            title = if (statsSubTab == 1) item.name.toTurkishGenre() else item.name,
                            count = item.count,
                            meanScore = item.meanScore,
                            timeSpentMinutes = item.timeSpentMinutes,
                            chaptersRead = item.chaptersRead,
                            imageUrl = item.imageUrl,
                            accentColor = accentColor,
                            onClick = {
                                when (statsSubTab) {
                                    1 -> onGenreClick(item.name.toEnglishGenreForSearch())
                                    2 -> onTagClick(item.name)
                                    3, 4 -> if (item.id != null) onFavoriteStaffClick(item.id, "anilist", item.name, item.imageUrl)
                                    5 -> if (item.id != null) onFavoriteStudioClick?.invoke(item.id, "anilist", item.name, item.imageUrl) else onTagClick(item.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
