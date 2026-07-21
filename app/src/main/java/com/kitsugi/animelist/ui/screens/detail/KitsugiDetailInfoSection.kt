package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.data.local.MangaMappingEntity
import coil3.compose.AsyncImage
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore
import com.kitsugi.animelist.utils.toTurkishGenre
import com.kitsugi.animelist.utils.toEnglishGenreForSearch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickActions(
    entry: MediaEntry,
    externalUrl: String?,
    onIncrementProgressClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenExternalClick: () -> Unit,
    onWatchClick: (() -> Unit)? = null,
    onReadClick: (() -> Unit)? = null,
    mangaMapping: MangaMappingEntity? = null,
    onLinkMangaClick: (() -> Unit)? = null,
    onUnlinkMangaClick: (() -> Unit)? = null,
    primaryFocusRequester: FocusRequester? = null,
    tabBarFocusRequester: FocusRequester? = null
) {
    val isWatchable = entry.type == MediaType.Anime || entry.type == MediaType.TvShow || entry.type == MediaType.Movie

    // Anime, Dizi ve Film ise İzle butonunu öne çıkar
    if (isWatchable && onWatchClick != null) {
        val accentColor = LocalKitsugiAccent.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(accentColor)
                .then(if (primaryFocusRequester != null) Modifier.focusRequester(primaryFocusRequester) else Modifier)
                .then(if (tabBarFocusRequester != null) Modifier.focusProperties { right = tabBarFocusRequester } else Modifier)
                .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onWatchClick)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = KitsugiColors.Background,
                    modifier = Modifier.padding(0.dp)
                )
                Text(
                    text = "İzle",
                    color = KitsugiColors.Background,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    // Manga ise Oku butonunu öne çıkar
    if (entry.type == MediaType.Manga && onReadClick != null) {
        val accentColor = LocalKitsugiAccent.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(accentColor)
                .then(if (primaryFocusRequester != null) Modifier.focusRequester(primaryFocusRequester) else Modifier)
                .then(if (tabBarFocusRequester != null) Modifier.focusProperties { right = tabBarFocusRequester } else Modifier)
                .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onReadClick)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = KitsugiColors.Background
                )
                Text(
                    text = if (mangaMapping != null) "Oku (${mangaMapping.mangaTitle})" else "Oku",
                    color = KitsugiColors.Background,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (mangaMapping != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(KitsugiColors.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!mangaMapping.mangaThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = mangaMapping.mangaThumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .width(40.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EŞLEŞEN MANGA",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mangaMapping.mangaTitle,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kaynak: ${mangaMapping.mangaSource}",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KitsugiColors.AccentRed.copy(alpha = 0.16f))
                            .tvClickable(shape = RoundedCornerShape(14.dp)) { onUnlinkMangaClick?.invoke() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bağlantıyı Kopar",
                            color = KitsugiColors.AccentRed,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KitsugiColors.SurfaceSoft)
                            .tvClickable(shape = RoundedCornerShape(14.dp)) { onLinkMangaClick?.invoke() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Yeniden Eşleştir",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(KitsugiColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Manga eşleşmesi bulunamadı. Okumaya başlamak için Oku butonuna tıklayarak bir kaynakla eşleştirin.",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    val btnModifier = if (tabBarFocusRequester != null) Modifier.focusProperties { right = tabBarFocusRequester } else Modifier
    val firstBtnModifier = btnModifier.then(
        if (primaryFocusRequester != null && !isWatchable && entry.type != MediaType.Manga) {
            Modifier.focusRequester(primaryFocusRequester)
        } else Modifier
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            text = "+1 ${progressUnit(entry)}",
            primary = true,
            modifier = firstBtnModifier,
            onClick = onIncrementProgressClick
        )

        ActionButton(
            text = if (entry.isFavorite) "Favoriden Çıkar" else "Favori Yap",
            primary = false,
            modifier = btnModifier,
            onClick = onToggleFavoriteClick
        )

        ActionButton(
            text = "Düzenle",
            primary = false,
            modifier = btnModifier,
            onClick = onEditClick
        )

        if (externalUrl != null) {
            ActionButton(
                text = "Kaynakta Aç",
                primary = false,
                modifier = btnModifier,
                onClick = onOpenExternalClick
            )
        }

        ActionButton(
            text = "Sil",
            primary = false,
            danger = true,
            modifier = btnModifier,
            onClick = onDeleteClick
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    primary: Boolean,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    val backgroundColor = when {
        danger -> KitsugiColors.AccentRed.copy(alpha = 0.16f)
        primary -> accentColor
        else -> KitsugiColors.Surface
    }

    val textColor = when {
        danger -> KitsugiColors.AccentRed
        primary -> KitsugiColors.Background
        else -> accentColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainStatsGrid(
    entry: MediaEntry,
    scoreFormat: String,
    hideScores: Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3
    ) {
        MiniStatCard(
            label = "Puan",
            value = entry.getDisplayScore(scoreFormat, hideScores),
            modifier = Modifier.weight(1f)
        )

        MiniStatCard(
            label = "İlerleme",
            value = entryProgressText(entry),
            modifier = Modifier.weight(1f)
        )

        if (entry.malId != null) {
            MiniStatCard(
                label = "ID",
                value = entry.malId.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KitsugiColors.Surface)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun DetailOverviewStatsCard(
    detail: KitsugiMediaDetail
) {
    val rank = detail.rank
    val scoredBy = detail.scoredBy
    val members = detail.members ?: detail.popularity
    val popularityRank = detail.popularityRank

    if (rank == null && scoredBy == null && members == null && popularityRank == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "İstatistikler",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatOverviewItem(
                label = "Puan Sırası",
                value = rank?.let { "#$it" } ?: "-",
                accentColor = LocalKitsugiAccent.current,
                modifier = Modifier.weight(1f)
            )

            StatOverviewItem(
                label = "Oy Sayısı",
                value = scoredBy?.let { formatCompactNumber(it) } ?: "-",
                accentColor = KitsugiColors.AccentGreen,
                modifier = Modifier.weight(1f)
            )

            StatOverviewItem(
                label = "Takip Edenler",
                value = members?.let { formatCompactNumber(it) } ?: "-",
                accentColor = KitsugiColors.AccentBlue,
                modifier = Modifier.weight(1f)
            )

            StatOverviewItem(
                label = "Popülerlik Sırası",
                value = popularityRank?.let { "#$it" } ?: "-",
                accentColor = KitsugiColors.AccentOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatOverviewItem(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatCompactNumber(number: Int): String {
    val javaLocale = java.util.Locale("tr", "TR")
    val formatter = java.text.NumberFormat.getInstance(javaLocale)
    return formatter.format(number)
}

@Composable
internal fun DateInfoCard(
    entry: MediaEntry
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Tarihler",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DateMiniBox(
                label = "Başlangıç",
                value = entry.startDate ?: "-",
                modifier = Modifier.weight(1f)
            )

            DateMiniBox(
                label = "Bitiş",
                value = entry.endDate ?: "-",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun DetailSynopsisCard(
    synopsisState: SynopsisState,
    originalText: String? = null,
    onTranslateClick: ((String) -> Unit)? = null,
    onCopyClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Açıklama",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            val accent = LocalKitsugiAccent.current
            val displayedText = (synopsisState as? SynopsisState.Success)?.text

            // Translate button
            if (onTranslateClick != null && !originalText.isNullOrBlank()) {
                IconButton(onClick = { onTranslateClick(originalText) }) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = "Çevir",
                        tint = accent
                    )
                }
            }

            // Copy button
            if (onCopyClick != null && !displayedText.isNullOrBlank()) {
                IconButton(onClick = { onCopyClick(displayedText) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = KitsugiColors.TextSecondary
                    )
                }
            }
        }

        if (synopsisState is SynopsisState.Success) {
            KitsugiMarkdownText(text = synopsisState.text)
        } else {
            val text = when (synopsisState) {
                SynopsisState.Loading -> "Açıklama yükleniyor..."
                SynopsisState.Error -> "Açıklama alınamadı."
                else -> ""
            }
            Text(
                text = text,
                color = if (synopsisState is SynopsisState.Error) KitsugiColors.AccentRed else KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun DetailExternalLinkCard(
    externalUrl: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Dış Bağlantı",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = externalUrl,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryGenresChipRow(
    genres: List<String>,
    onGenreClick: (String) -> Unit = {}
) {
    val accentColor = LocalKitsugiAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Türler",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { genre ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .tvClickable(shape = RoundedCornerShape(999.dp)) { onGenreClick(genre.toEnglishGenreForSearch()) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = genre.toTurkishGenre(),
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
internal fun EntryInfoSection(
    detail: KitsugiMediaDetail,
    mediaType: MediaType,
    onSearchQuery: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
    ) {
        Text(
            text = "Bilgiler",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        val rows = buildList {
            if (!detail.status.isNullOrBlank()) add("Durum" to detail.status)
            if (!detail.season.isNullOrBlank()) add("Sezon" to detail.season)
            if (!detail.startDate.isNullOrBlank()) add("Başlangıç" to detail.startDate)
            if (!detail.endDate.isNullOrBlank()) add("Bitiş" to detail.endDate)
            if (!detail.sourceMaterial.isNullOrBlank()) add("Kaynak" to detail.sourceMaterial)
            if (mediaType == MediaType.Anime) {
                if (detail.studios.isNotEmpty()) add("Stüdyo" to detail.studios.joinToString(", "))
                if (!detail.episodeDuration.isNullOrBlank()) add("Süre" to detail.episodeDuration)
                if (!detail.broadcast.isNullOrBlank()) add("Yayın" to detail.broadcast)
                if (!detail.rating.isNullOrBlank()) add("Yaş Sınırı" to detail.rating)
            }
            if (!detail.titleEnglish.isNullOrBlank()) add("İngilizce" to detail.titleEnglish)
            if (!detail.titleJapanese.isNullOrBlank()) add("Japonca" to detail.titleJapanese)
            if (detail.synonyms.isNotEmpty()) add("Diğer Adlar" to detail.synonyms.joinToString(", "))
        }
        rows.forEachIndexed { index, (label, value) ->
            val onValueClick: (() -> Unit)? = if (label == "Sezon") {
                { onSearchQuery(value) }
            } else {
                null
            }
            EntryInfoRow(label = label, value = value, onValueClick = onValueClick)
            if (index < rows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = KitsugiColors.Background.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun EntryInfoRow(label: String, value: String, onValueClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f)
        )
        val valueColor = if (onValueClick != null) LocalKitsugiAccent.current else KitsugiColors.TextPrimary
        val valueModifier = Modifier
            .weight(0.6f)
            .let {
                if (onValueClick != null) {
                    it.clip(RoundedCornerShape(4.dp)).tvClickable(shape = RoundedCornerShape(4.dp), onClick = onValueClick)
                } else {
                    it
                }
            }
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (onValueClick != null) FontWeight.Bold else FontWeight.Normal,
            modifier = valueModifier
        )
    }
}
