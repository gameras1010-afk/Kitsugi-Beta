package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.toFriendlySourceLabel

@Composable
fun KitsugiMediaEntryDetailDialog(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onIncrementProgressClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onSynopsisLoaded: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val statusColor = statusColor(entry.status)
    val uriHandler = LocalUriHandler.current

    val apiClient = remember {
        JikanApiClient()
    }

    var synopsisState by remember(entry.id, entry.source, entry.malId, entry.synopsis) {
        mutableStateOf<SynopsisState>(
            if (!entry.synopsis.isNullOrBlank()) {
                SynopsisState.Success(entry.synopsis)
            } else {
                SynopsisState.Loading
            }
        )
    }

    val externalUrl = buildExternalUrl(entry)

    LaunchedEffect(entry.id, entry.source, entry.malId, entry.synopsis) {
        if (!entry.synopsis.isNullOrBlank()) {
            synopsisState = SynopsisState.Success(entry.synopsis)
            return@LaunchedEffect
        }

        synopsisState = SynopsisState.Loading

        val synopsis = apiClient.fetchSynopsis(
            source = entry.source,
            externalId = entry.malId,
            mediaType = entry.type
        )

        synopsisState = if (synopsis.isNullOrBlank()) {
            SynopsisState.Error
        } else {
            onSynopsisLoaded(synopsis)
            SynopsisState.Success(synopsis)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Kayıt Detayı",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Sol Sütun: Poster ve Hızlı Butonlar
                    Column(
                        modifier = Modifier
                            .width(180.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailPoster(
                            entry = entry,
                            statusColor = statusColor,
                            modifier = Modifier
                                .width(180.dp)
                                .height(250.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = onIncrementProgressClick,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "+1 ${progressUnit(entry)}",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                TextButton(
                                    onClick = onToggleFavoriteClick,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (entry.isFavorite) "Favoriden Çıkar" else "Favori Yap",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            if (externalUrl != null) {
                                TextButton(
                                    onClick = {
                                        uriHandler.openUri(externalUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Kaynakta Aç",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Sağ Sütun: Başlık, Bilgi Izgarası ve Açıklama (Scrollable)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = entry.title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = entry.subtitle,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailPill(
                                text = mediaTypeLabel(entry),
                                color = accentColor
                            )

                            DetailPill(
                                text = entry.status.label,
                                color = statusColor
                            )

                            if (entry.isFavorite) {
                                DetailPill(
                                    text = "★ Favori",
                                    color = accentColor
                                )
                            }

                            if (entry.isAdult) {
                                DetailPill(
                                    text = "+18",
                                    color = KitsugiColors.AccentRed
                                )
                            }
                        }

                        DetailInfoGrid(
                            entry = entry,
                            externalUrl = externalUrl
                        )

                        SynopsisSection(
                            synopsisState = synopsisState
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailPoster(
                        entry = entry,
                        statusColor = statusColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = entry.title,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = entry.subtitle,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailPill(
                            text = mediaTypeLabel(entry),
                            color = accentColor
                        )

                        DetailPill(
                            text = entry.status.label,
                            color = statusColor
                        )

                        if (entry.isFavorite) {
                            DetailPill(
                                text = "★ Favori",
                                color = accentColor
                            )
                        }

                        if (entry.isAdult) {
                            DetailPill(
                                text = "+18",
                                color = KitsugiColors.AccentRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onIncrementProgressClick
                        ) {
                            Text(
                                text = "+1 ${progressUnit(entry)}",
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onToggleFavoriteClick
                        ) {
                            Text(
                                text = if (entry.isFavorite) {
                                    "Favoriden Çıkar"
                                } else {
                                    "Favori Yap"
                                },
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (externalUrl != null) {
                        TextButton(
                            onClick = {
                                uriHandler.openUri(externalUrl)
                            }
                        ) {
                            Text(
                                text = "Kaynakta Aç",
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailInfoGrid(
                        entry = entry,
                        externalUrl = externalUrl
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SynopsisSection(
                        synopsisState = synopsisState
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onEditClick
            ) {
                Text(
                    text = "Düzenle",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDeleteClick
                ) {
                    Text(
                        text = "Sil",
                        color = KitsugiColors.AccentRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Kapat",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

@Composable
private fun SynopsisSection(
    synopsisState: SynopsisState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(14.dp)
    ) {
        Text(
            text = "Açıklama",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val text = when (synopsisState) {
            SynopsisState.Loading -> "Açıklama yükleniyor..."
            SynopsisState.Error -> "Açıklama alınamadı."
            is SynopsisState.Success -> synopsisState.text
        }

        Text(
            text = text,
            color = when (synopsisState) {
                SynopsisState.Error -> KitsugiColors.AccentRed
                else -> KitsugiColors.TextSecondary
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DetailPoster(
    entry: MediaEntry,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(statusColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        if (!entry.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = entry.title.take(2).uppercase(),
                color = statusColor,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun DetailInfoGrid(
    entry: MediaEntry,
    externalUrl: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailInfoRow(
            label = "İlerleme",
            value = entryProgressText(entry)
        )

        if ((entry.source == "mal" || entry.source == "jikan") && entry.type == MediaType.Manga && entry.volumeProgress > 0) {
            DetailInfoRow(
                label = "Okunan Cilt",
                value = entry.volumeProgress.toString()
            )
        }

        DetailInfoRow(
            label = "Puan",
            value = scoreText(entry)
        )

        DetailInfoRow(
            label = "Kaynak",
            value = entry.source.toFriendlySourceLabel().uppercase()
        )

        DetailInfoRow(
            label = "ID",
            value = entry.malId?.toString() ?: "-"
        )

        DetailInfoRow(
            label = "Yıl",
            value = entry.year?.toString() ?: "-"
        )

        if (entry.source == "mal" || entry.source == "jikan") {
            DetailInfoRow(
                label = "Öncelik",
                value = when (entry.priority) {
                    1 -> "Orta"
                    2 -> "Yüksek"
                    else -> "Düşük"
                }
            )
        }

        if (entry.repeatCount > 0) {
            DetailInfoRow(
                label = "Tekrar Sayısı",
                value = entry.repeatCount.toString()
            )
        }

        if (!entry.tags.isNullOrBlank()) {
            DetailInfoRow(
                label = "Etiketler",
                value = entry.tags ?: ""
            )
        }

        if (!entry.notes.isNullOrBlank()) {
            DetailInfoRow(
                label = if (entry.source == "anilist") "Notlar" else "Yorumlar",
                value = entry.notes ?: ""
            )
        }

        DetailInfoRow(
            label = "Favori",
            value = if (entry.isFavorite) "Evet" else "Hayır"
        )

        DetailInfoRow(
            label = "+18",
            value = if (entry.isAdult) "Evet" else "Hayır"
        )

        if (externalUrl != null) {
            DetailInfoRow(
                label = "Dış bağlantı",
                value = externalUrl
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = value,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun DetailPill(
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

private fun buildExternalUrl(entry: MediaEntry): String? {
    val id = entry.malId ?: return null
    return com.kitsugi.animelist.utils.ShareUtils.buildExternalMediaUrl(entry.source, id, type = entry.type)
}

private fun statusColor(status: WatchStatus): Color {
    return when (status) {
        WatchStatus.Watching   -> KitsugiColors.AccentBlue
        WatchStatus.Completed  -> KitsugiColors.AccentGreen
        WatchStatus.Planned    -> KitsugiColors.AccentOrange
        WatchStatus.Dropped    -> KitsugiColors.AccentRed
        WatchStatus.Paused     -> KitsugiColors.AccentPurple
        WatchStatus.Repeating  -> KitsugiColors.AccentBlue
    }
}

private fun mediaTypeLabel(entry: MediaEntry): String {
    return when (entry.type) {
        MediaType.Anime -> "ANIME"
        MediaType.Manga -> "MANGA"
        MediaType.Movie -> "MOVIE"
        MediaType.TvShow -> "TV SHOW"
    }
}

private fun progressUnit(entry: MediaEntry): String {
    return when (entry.type) {
        MediaType.Anime -> "bölüm"
        MediaType.Manga -> "chapter"
        else -> "bölüm"
    }
}

private fun entryProgressText(entry: MediaEntry): String {
    val totalText = entry.total?.toString() ?: "?"
    return "${entry.progress}/$totalText ${progressUnit(entry)}"
}

private fun scoreText(entry: MediaEntry): String {
    return if (entry.score == null) {
        "Puan yok"
    } else {
        "${entry.score}/10"
    }
}

private sealed class SynopsisState {
    data object Loading : SynopsisState()

    data object Error : SynopsisState()

    data class Success(
        val text: String
    ) : SynopsisState()
}