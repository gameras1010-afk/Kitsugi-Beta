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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.utils.toTurkishGenre
import com.kitsugi.animelist.utils.toEnglishGenreForSearch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.utils.copyOnDoubleTap

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ApiHero(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onBackClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    logoUrl: String? = null
) {
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
    ) {
        if (!result.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = result.getDisplayTitle(titleLanguage),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = result.getDisplayTitle(titleLanguage).take(2).uppercase(),
                    color = accentColor,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KitsugiColors.Background.copy(alpha = 0.05f),
                            KitsugiColors.Background.copy(alpha = 0.30f),
                            KitsugiColors.Background.copy(alpha = 0.72f),
                            KitsugiColors.Background
                        )
                    )
                )
        )

        TextButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 24.dp)
        ) {
            Text(
                text = "Geri",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Logo veya metin başlık
            val context = LocalContext.current
            val titleText = result.getDisplayTitle(titleLanguage)
            val copyTitleGesture = Modifier.copyOnDoubleTap(context, titleText)

            if (!logoUrl.isNullOrBlank()) {
                var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                if (!logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = titleText,
                        modifier = Modifier
                            .height(75.dp)
                            .fillMaxWidth(0.85f)
                            .then(copyTitleGesture),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        onError = { logoFailed = true }
                    )
                } else {
                    Text(
                        text = titleText,
                        modifier = copyTitleGesture,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = titleText,
                    modifier = copyTitleGesture,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApiDetailPill(
                    text = when (result.type) {
                        MediaType.Anime -> "ANIME"
                        MediaType.Manga -> "MANGA"
                        MediaType.Movie -> "FİLM"
                        MediaType.TvShow -> "DİZİ"
                    },
                    color = accentColor
                )

                ApiDetailPill(
                    text = result.source.uppercase(),
                    color = accentColor
                )

                if (result.year != null) {
                    ApiDetailPill(
                        text = result.year.toString(),
                        color = KitsugiColors.TextSecondary
                    )
                }

                if (!hideScores) {
                    ApiDetailPill(
                        text = result.getDisplayScore(scoreFormat, hideScores),
                        color = accentColor
                    )
                }

                if (alreadyInList) {
                    ApiDetailPill(
                        text = "Listede",
                        color = KitsugiColors.AccentGreen
                    )
                }

                if (result.isAdult) {
                    ApiDetailPill(
                        text = "+18",
                        color = KitsugiColors.AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = result.subtitle,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ApiStatsGrid(
    result: JikanSearchResult,
    scoreFormat: String,
    hideScores: Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3
    ) {
        ApiMiniStatCard(
            label = "Tür",
            value = when (result.type) {
                MediaType.Anime -> "ANİME"
                MediaType.Manga -> "MANGA"
                MediaType.Movie -> "FİLM"
                MediaType.TvShow -> "DİZİ"
            },
            modifier = Modifier.weight(1f)
        )

        ApiMiniStatCard(
            label = "Kaynak",
            value = result.source.uppercase(),
            modifier = Modifier.weight(1f)
        )

        ApiMiniStatCard(
            label = "ID",
            value = result.malId.toString(),
            modifier = Modifier.weight(1f)
        )

        ApiMiniStatCard(
            label = "Yıl",
            value = result.year?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )

        ApiMiniStatCard(
            label = "Toplam",
            value = result.total?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )

        ApiMiniStatCard(
            label = "Skor",
            value = if (hideScores) "-" else result.getDisplayScore(scoreFormat, hideScores),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ApiMiniStatCard(
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
internal fun ApiSynopsisCard(
    synopsis: String?,
    isLoading: Boolean,
    onTranslateClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
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

            // Translate button
            if (onTranslateClick != null && !synopsis.isNullOrBlank()) {
                IconButton(onClick = onTranslateClick) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = "Çevir",
                        tint = LocalKitsugiAccent.current
                    )
                }
            }

            // Copy button
            if (onCopyClick != null && !synopsis.isNullOrBlank()) {
                IconButton(onClick = onCopyClick) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = KitsugiColors.TextSecondary
                    )
                }
            }
        }

        if (isLoading || synopsis.isNullOrBlank()) {
            val text = when {
                isLoading -> "Açıklama yükleniyor..."
                else -> "Açıklama bulunamadı."
            }
            Text(
                text = text,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            KitsugiMarkdownText(text = synopsis)
        }
    }
}

@Composable
internal fun ApiTrailerCard(
    trailerUrl: String
) {
    val uriHandler = LocalUriHandler.current
    val accentColor = LocalKitsugiAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(22.dp), onClick = {
                runCatching { uriHandler.openUri(trailerUrl) }
            })
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fragman",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fragmanı izlemek için dokunun",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(10.dp)
            ) {
                Text(
                    text = "▶",
                    color = accentColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ApiGenresChipRow(
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
                        .tvClickable(shape = RoundedCornerShape(999.dp), onClick = { onGenreClick(genre.toEnglishGenreForSearch()) })
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
internal fun ApiInfoSection(
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
            val onValueClick = if (label == "Sezon") {
                { onSearchQuery(value) }
            } else {
                null
            }
            ApiInfoRow(label = label, value = value, onValueClick = onValueClick)
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
private fun ApiInfoRow(label: String, value: String, onValueClick: (() -> Unit)? = null) {
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

@Composable
internal fun ApiThemesList(
    openings: List<com.kitsugi.animelist.data.remote.KitsugiTheme>,
    endings: List<com.kitsugi.animelist.data.remote.KitsugiTheme>
) {
    val accentColor = LocalKitsugiAccent.current
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (openings.isNotEmpty()) {
            Text(
                text = "Açılış Müzikleri",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            openings.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .tvClickable(shape = RoundedCornerShape(8.dp)) {
                            if (!theme.videoUrl.isNullOrBlank()) {
                                runCatching { uriHandler.openUri(theme.videoUrl) }
                            } else {
                                val query = java.net.URLEncoder.encode(theme.label, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$query") }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (!theme.videoUrl.isNullOrBlank()) "▶" else "🔍",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = theme.label,
                        color = accentColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (endings.isNotEmpty()) {
            if (openings.isNotEmpty()) {
                HorizontalDivider(color = KitsugiColors.Background.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
            Text(
                text = "Kapanış Müzikleri",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            endings.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .tvClickable(shape = RoundedCornerShape(8.dp)) {
                            if (!theme.videoUrl.isNullOrBlank()) {
                                runCatching { uriHandler.openUri(theme.videoUrl) }
                            } else {
                                val query = java.net.URLEncoder.encode(theme.label, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$query") }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (!theme.videoUrl.isNullOrBlank()) "▶" else "🔍",
                        color = accentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = theme.label,
                        color = accentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ApiExternalLinkCard(
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

@Composable
internal fun ApiActionButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    val backgroundColor = when {
        primary && enabled -> accentColor
        primary && !enabled -> KitsugiColors.AccentGreen
        else -> KitsugiColors.Surface
    }

    val textColor = when {
        primary -> KitsugiColors.Background
        else -> accentColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .tvClickable(
                enabled = enabled,
                shape = RoundedCornerShape(999.dp),
                onClick = onClick
            )
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

@Composable
internal fun ApiDetailPill(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── MDBList Rating Card ─────────────────────────────────────────────────────

/**
 * Displays external ratings (IMDb, Rotten Tomatoes, Metacritic, Letterboxd, TMDB, Trakt)
 * fetched via the MDBList API. Renders as a horizontal scrolling row of colored badge chips.
 */
@Composable
internal fun ApiMdbListRatingCard(
    ratings: MdbListRatings?,
    isLoading: Boolean,
    showImdb: Boolean = true,
    showTomatoes: Boolean = true,
    showMetacritic: Boolean = true,
    showAudience: Boolean = false,
    showLetterboxd: Boolean = false,
    showTmdb: Boolean = false,
    showTrakt: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    mediaTitle: String = ""
) {
    if (!isLoading && (ratings == null || ratings.isEmpty)) return

    val accentColor = LocalKitsugiAccent.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MDBList",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Harici Puanlar",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ayarlar",
                        tint = KitsugiColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Body ─────────────────────────────────────────────────────────────
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = accentColor,
                    strokeWidth = 1.5.dp
                )
                Text(
                    text = "Puanlar yükleniyor...",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (ratings != null) {
            val isTv = LocalIsTv.current
            val badgeList = buildList<@Composable () -> Unit> {
                val imdbId = ratings.imdbId
                if (showImdb && ratings.imdb != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "⭐",
                            label = "IMDb",
                            value = String.format("%.1f", ratings.imdb),
                            badgeColor = Color(0xFFF5C518),
                            textColor = Color(0xFF1A1A1A),
                            onClick = {
                                if (!imdbId.isNullOrBlank()) {
                                    runCatching { uriHandler.openUri("https://www.imdb.com/title/$imdbId") }
                                } else {
                                    val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                    runCatching { uriHandler.openUri("https://www.imdb.com/find?q=$query") }
                                }
                            }
                        )
                    }
                }
                if (showTomatoes && ratings.tomatoes != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "🍅",
                            label = "Tomatometer",
                            value = "${ratings.tomatoes}%",
                            badgeColor = Color(0xFFFA320A),
                            textColor = Color.White,
                            onClick = {
                                val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.rottentomatoes.com/search?search=$query") }
                            }
                        )
                    }
                }
                if (showAudience && ratings.tomatoesAudience != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "🍿",
                            label = "Audience",
                            value = "${ratings.tomatoesAudience}%",
                            badgeColor = Color(0xFFFA6D0A),
                            textColor = Color.White,
                            onClick = {
                                val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.rottentomatoes.com/search?search=$query") }
                            }
                        )
                    }
                }
                if (showMetacritic && ratings.metacritic != null) {
                    val metaColor = when {
                        ratings.metacritic >= 61 -> Color(0xFF6AC045)
                        ratings.metacritic >= 40 -> Color(0xFFFFCC34)
                        else -> Color(0xFFFF4444)
                    }
                    val metaTextColor = if (ratings.metacritic >= 40) Color(0xFF1A1A1A) else Color.White
                    add {
                        MdbRatingBadge(
                            emoji = "🎖️",
                            label = "Metacritic",
                            value = "${ratings.metacritic}",
                            badgeColor = metaColor,
                            textColor = metaTextColor,
                            onClick = {
                                val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.metacritic.com/search/$query/") }
                            }
                        )
                    }
                }
                if (showLetterboxd && ratings.letterboxd != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "📽️",
                            label = "Letterboxd",
                            value = String.format("%.1f", ratings.letterboxd),
                            badgeColor = Color(0xFF2C3440),
                            textColor = Color(0xFF40BCF4),
                            onClick = {
                                if (!imdbId.isNullOrBlank()) {
                                    runCatching { uriHandler.openUri("https://letterboxd.com/imdb/$imdbId") }
                                } else {
                                    val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                    runCatching { uriHandler.openUri("https://letterboxd.com/search/$query") }
                                }
                            }
                        )
                    }
                }
                if (showTmdb && ratings.tmdb != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "🎬",
                            label = "TMDB",
                            value = String.format("%.1f", ratings.tmdb),
                            badgeColor = Color(0xFF0D253F),
                            textColor = Color(0xFF01B4E4),
                            onClick = {
                                val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.themoviedb.org/search?query=$query") }
                            }
                        )
                    }
                }
                if (showTrakt && ratings.trakt != null) {
                    add {
                        MdbRatingBadge(
                            emoji = "📺",
                            label = "Trakt",
                            value = String.format("%.0f%%", ratings.trakt),
                            badgeColor = Color(0xFFED1C24),
                            textColor = Color.White,
                            onClick = {
                                if (!imdbId.isNullOrBlank()) {
                                    runCatching { uriHandler.openUri("https://trakt.tv/search/imdb/$imdbId") }
                                } else {
                                    val query = java.net.URLEncoder.encode(mediaTitle, "UTF-8")
                                    runCatching { uriHandler.openUri("https://trakt.tv/search?query=$query") }
                                }
                            }
                        )
                    }
                }
            }

            if (isTv) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(badgeList) { badgeComposable ->
                        badgeComposable()
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    badgeList.forEach { badgeComposable ->
                        badgeComposable()
                    }
                }
            }
        }
    }
}

@Composable
private fun MdbRatingBadge(
    emoji: String,
    label: String,
    value: String,
    badgeColor: Color,
    textColor: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(badgeColor)
            .let {
                if (onClick != null) {
                    it.tvClickable(shape = RoundedCornerShape(14.dp), onClick = onClick)
                } else {
                    it
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Büyük skor değeri
        Text(
            text = value,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        // Emoji + Label satırı
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
            Text(
                text = label,
                color = textColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    }
}
