package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.ui.app.RankedStatItem
import com.kitsugi.animelist.ui.screens.profile.*
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.toEnglishGenreForSearch
import com.kitsugi.animelist.utils.toTurkishGenre

@Composable
fun AniListStatsTab(
    state: AniListProfileState,
    accentColor: Color,
    statsMediaType: Int,
    statsSubTab: Int,
    scoreDistType: Int,
    lengthDistType: Int,
    releaseYearDistType: Int,
    startYearDistType: Int,
    statSortType: Int,
    onStatsMediaTypeChange: (Int) -> Unit,
    onScoreDistTypeChange: (Int) -> Unit,
    onLengthDistTypeChange: (Int) -> Unit,
    onReleaseYearDistTypeChange: (Int) -> Unit,
    onStartYearDistTypeChange: (Int) -> Unit,
    onStatSortTypeChange: (Int) -> Unit,
    onGenreClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null
) {
    val overview = if (statsMediaType == 0) state.animeOverviewStats else state.mangaOverviewStats

    if (statsSubTab == 0) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(KitsugiColors.Surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Media Switcher
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
                            .clickable { onStatsMediaTypeChange(idx) }
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

            if (overview != null) {
                // 1. Key Stats Grid (3x2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard("Toplam", overview.count.toString())
                        StatCard(if (statsMediaType == 0) "İzlenen bölüm" else "Okunan bölüm", overview.episodesWatched.toString())
                        StatCard(if (statsMediaType == 0) "İzlenen gün" else "Okunan cilt", "%.1f".format(overview.daysWatched))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(if (statsMediaType == 0) "Planlanan gün" else "Planlanan bölüm", "%.1f".format(overview.plannedDaysOrCount))
                        StatCard("Ortalama Puan", "%.2f".format(overview.meanScore))
                        StatCard("Standart sapma", "%.1f".format(overview.standardDeviation))
                    }
                }

                HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                // 2. Score Distribution
                if (overview.scoreList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Puan", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChipItem(selected = scoreDistType == 0, text = "Başlık sayısı", onClick = { onScoreDistTypeChange(0) }, accentColor = accentColor)
                            FilterChipItem(selected = scoreDistType == 1, text = "Harcanan süre", onClick = { onScoreDistTypeChange(1) }, accentColor = accentColor)
                        }
                        val mappedStats = overview.scoreList.map { item ->
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
                                    10      -> Color(0xFF4FC3F7)
                                    else    -> accentColor
                                }
                            }
                        )
                    }
                }

                // 3. Episode / Chapter Length Distribution
                if (overview.lengthList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (statsMediaType == 0) "Bölüm Sayısı" else "Cilt/Bölüm Sayısı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChipItem(selected = lengthDistType == 0, text = "Başlık sayısı", onClick = { onLengthDistTypeChange(0) }, accentColor = accentColor)
                            FilterChipItem(selected = lengthDistType == 1, text = "Harcanan süre", onClick = { onLengthDistTypeChange(1) }, accentColor = accentColor)
                            FilterChipItem(selected = lengthDistType == 2, text = "Ortalama Puan", onClick = { onLengthDistTypeChange(2) }, accentColor = accentColor)
                        }
                        val mappedLength = overview.lengthList.map { item ->
                            val valFloat = when (lengthDistType) { 0 -> item.count.toFloat(); 1 -> (item.minutesWatched / 60.0f); else -> item.meanScore.toFloat() }
                            item.length to valFloat
                        }
                        VerticalStatsBar(stats = mappedLength, accentColor = accentColor)
                    }
                }

                // 4. Status Distribution
                if (overview.statusList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Durum Dağılımı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val statusItems = overview.statusList.map { item ->
                            val (label, color) = when (item.status.uppercase()) {
                                "CURRENT"   -> (if (statsMediaType == 0) "Şimdiki" else "Okunuyor") to Color(0xFF81C784)
                                "COMPLETED" -> "Tamamlandı" to Color(0xFF64B5F6)
                                "PLANNING"  -> "Planlanan" to Color(0xFFA1887F)
                                "PAUSED"    -> "Durduruldu" to Color(0xFFFFB74D)
                                "DROPPED"   -> "Bırakıldı" to Color(0xFFE57373)
                                "REPEATING" -> (if (statsMediaType == 0) "Tekrar İzleniyor" else "Tekrar Okunuyor") to Color(0xFFBA68C8)
                                else        -> item.status to accentColor
                            }
                            Triple(label, item.count, color)
                        }
                        HorizontalStatsBar(stats = statusItems)
                    }
                }

                // 5. Format Distribution
                if (overview.formatList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tür Dağılımı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val formatItems = overview.formatList.map { item ->
                            val (label, color) = when (item.format.uppercase()) {
                                "TV"       -> "TV"         to Color(0xFF5C6BC0)
                                "TV_SHORT" -> "TV Kısa"   to Color(0xFF7E57C2)
                                "MOVIE"    -> "Film"       to Color(0xFF26A69A)
                                "SPECIAL"  -> "Özel"       to Color(0xFFFFA726)
                                "OVA"      -> "OVA"        to Color(0xFFFF7043)
                                "ONA"      -> "ONA"        to Color(0xFFEC407A)
                                "MUSIC"    -> "Müzik Klip" to Color(0xFFAB47BC)
                                "MANGA"    -> "Manga"      to Color(0xFF42A5F5)
                                "NOVEL"    -> "LN"         to Color(0xFF8D6E63)
                                "ONE_SHOT" -> "One-Shot"   to Color(0xFF78909C)
                                else       -> item.format  to accentColor
                            }
                            Triple(label, item.count, color)
                        }
                        HorizontalStatsBar(stats = formatItems)
                    }
                }

                // 6. Country Distribution
                if (overview.countryList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ülke Dağılımı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val countryItems = overview.countryList.map { item ->
                            val (label, color) = when (item.country.uppercase()) {
                                "JP" -> "Japonya"     to Color(0xFF5C6BC0)
                                "KR" -> "Güney Kore"  to Color(0xFF26A69A)
                                "CN" -> "Çin"         to Color(0xFFFF7043)
                                "TW" -> "Tayvan"      to Color(0xFFAB47BC)
                                else -> item.country  to accentColor
                            }
                            Triple(label, item.count, color)
                        }
                        HorizontalStatsBar(stats = countryItems)
                    }
                }

                // 7. Release Year Distribution
                if (overview.releaseYearList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Yayın Yılı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChipItem(selected = releaseYearDistType == 0, text = "Başlık sayısı", onClick = { onReleaseYearDistTypeChange(0) }, accentColor = accentColor)
                            FilterChipItem(selected = releaseYearDistType == 1, text = "Harcanan süre", onClick = { onReleaseYearDistTypeChange(1) }, accentColor = accentColor)
                            FilterChipItem(selected = releaseYearDistType == 2, text = "Ortalama Puan", onClick = { onReleaseYearDistTypeChange(2) }, accentColor = accentColor)
                        }
                        val mappedYears = overview.releaseYearList
                            .filter { it.releaseYear > 0 }
                            .sortedBy { it.releaseYear }
                            .map { item ->
                                val valFloat = when (releaseYearDistType) { 0 -> item.count.toFloat(); 1 -> (item.minutesWatched / 60.0f); else -> item.meanScore.toFloat() }
                                item.releaseYear.toString() to valFloat
                            }
                        VerticalStatsBar(stats = mappedYears, accentColor = accentColor)
                    }
                }

                // 8. Watch / Read Year Distribution
                if (overview.startYearList.isNotEmpty()) {
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (statsMediaType == 0) "İzleme Yılı" else "Okuma Yılı", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChipItem(selected = startYearDistType == 0, text = "Başlık sayısı", onClick = { onStartYearDistTypeChange(0) }, accentColor = accentColor)
                            FilterChipItem(selected = startYearDistType == 1, text = "Harcanan süre", onClick = { onStartYearDistTypeChange(1) }, accentColor = accentColor)
                            FilterChipItem(selected = startYearDistType == 2, text = "Ortalama Puan", onClick = { onStartYearDistTypeChange(2) }, accentColor = accentColor)
                        }
                        val mappedStartYears = overview.startYearList
                            .filter { it.startYear > 0 }
                            .sortedBy { it.startYear }
                            .map { item ->
                                val valFloat = when (startYearDistType) { 0 -> item.count.toFloat(); 1 -> (item.minutesWatched / 60.0f); else -> item.meanScore.toFloat() }
                                item.startYear.toString() to valFloat
                            }
                        VerticalStatsBar(stats = mappedStartYears, accentColor = accentColor)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "İstatistik verisi hazırlanamadı.", color = KitsugiColors.TextMuted)
                }
            }
        }
    } else {
        // Ranked sub-tabs: Genres, Tags, Staff, VAs, Studios
        val currentList: List<RankedStatItem> = when (statsSubTab) {
            1 -> overview?.genreList.orEmpty()
            2 -> overview?.tagList.orEmpty()
            3 -> overview?.staffList.orEmpty()
            4 -> overview?.voiceActorList.orEmpty()
            5 -> overview?.studioList.orEmpty()
            else -> emptyList()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(KitsugiColors.Surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Media Switcher for tabs 1..3
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
                                .clickable { onStatsMediaTypeChange(idx) }
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

            // Sort Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ProfileFilterChip(isSelected = statSortType == 0, text = "Başlık sayısı", onClick = { onStatSortTypeChange(0) }, accentColor = accentColor)
                ProfileFilterChip(isSelected = statSortType == 1, text = "Harcanan süre", onClick = { onStatSortTypeChange(1) }, accentColor = accentColor)
                ProfileFilterChip(isSelected = statSortType == 2, text = "Ortalama Puan", onClick = { onStatSortTypeChange(2) }, accentColor = accentColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val sortedList = when (statSortType) {
            0    -> currentList.sortedByDescending { it.count }
            1    -> currentList.sortedByDescending { it.timeSpentMinutes ?: it.chaptersRead ?: 0 }
            2    -> currentList.sortedByDescending { it.meanScore }
            else -> currentList
        }

        if (sortedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                                1    -> onGenreClick(item.name.toEnglishGenreForSearch())
                                2    -> onTagClick(item.name)
                                3, 4 -> if (item.id != null) onFavoriteStaffClick(item.id, "anilist", item.name, item.imageUrl)
                                5    -> if (item.id != null) onFavoriteStudioClick?.invoke(item.id, "anilist", item.name, item.imageUrl) else onTagClick(item.name)
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }
}
