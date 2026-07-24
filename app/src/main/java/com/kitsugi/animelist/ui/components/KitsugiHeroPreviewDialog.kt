package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun KitsugiHeroPreviewDialog(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onAddClick: (synopsis: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    val apiClient = remember {
        JikanApiClient()
    }

    var synopsisState by remember(result.source, result.malId, result.type) {
        mutableStateOf<HeroSynopsisState>(HeroSynopsisState.Loading)
    }

    LaunchedEffect(result.source, result.malId, result.type) {
        synopsisState = HeroSynopsisState.Loading

        val synopsis = apiClient.fetchSynopsis(
            source = result.source,
            externalId = result.malId,
            mediaType = result.type
        )

        synopsisState = if (synopsis.isNullOrBlank()) {
            HeroSynopsisState.Error
        } else {
            HeroSynopsisState.Success(synopsis)
        }
    }

    val synopsisForSave = when (val state = synopsisState) {
        is HeroSynopsisState.Success -> state.text
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Öne Çıkan Detay",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = result.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = result.title.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = result.title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = result.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroPill(
                        text = if (result.type == MediaType.Anime) "ANIME" else "MANGA"
                    )

                    val sourceLabel = result.source.toFriendlySourceLabel()
                    HeroPill(
                        text = sourceLabel
                    )

                    if (result.year != null) {
                        HeroPill(
                            text = result.year.toString()
                        )
                    }

                    val displayScore = if (result.score == null || result.score == 0) "unrated" else "${result.score}/10"
                    HeroPill(
                        text = displayScore
                    )

                    if (result.isAdult) {
                        HeroPill(
                            text = "+18",
                            danger = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val sourceIdLabel = "${result.source.toFriendlySourceLabel()} ID"
                HeroInfoRow(
                    label = sourceIdLabel,
                    value = result.malId.toString()
                )

                HeroInfoRow(
                    label = "Toplam",
                    value = result.total?.toString() ?: "-"
                )

                Spacer(modifier = Modifier.height(12.dp))

                SynopsisBox(
                    synopsisState = synopsisState
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !alreadyInList,
                onClick = {
                    onAddClick(synopsisForSave)
                }
            ) {
                Text(
                    text = if (alreadyInList) "Listede" else "Listeye Ekle",
                    color = if (alreadyInList) {
                        KitsugiColors.AccentGreen
                    } else {
                        accentColor
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
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
    )
}

@Composable
private fun HeroPill(
    text: String,
    danger: Boolean = false
) {
    val accentColor = if (danger) {
        KitsugiColors.AccentRed
    } else {
        LocalKitsugiAccent.current
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeroInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SynopsisBox(
    synopsisState: HeroSynopsisState
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
            HeroSynopsisState.Loading -> "Açıklama yükleniyor..."
            HeroSynopsisState.Error -> "Açıklama alınamadı."
            is HeroSynopsisState.Success -> synopsisState.text
        }

        Text(
            text = text,
            color = when (synopsisState) {
                HeroSynopsisState.Error -> KitsugiColors.AccentRed
                else -> KitsugiColors.TextSecondary
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private sealed class HeroSynopsisState {
    data object Loading : HeroSynopsisState()

    data object Error : HeroSynopsisState()

    data class Success(
        val text: String
    ) : HeroSynopsisState()
}