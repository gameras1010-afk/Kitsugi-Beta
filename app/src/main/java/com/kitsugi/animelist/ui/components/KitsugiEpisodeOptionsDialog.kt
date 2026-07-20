package com.kitsugi.animelist.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import java.net.URLEncoder
import androidx.compose.material.icons.rounded.PlayArrow
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity

@Composable
fun KitsugiEpisodeOptionsDialog(
    animeTitle: String,
    episodeNumber: Int?,
    episodeTitle: String?,
    originalUrl: String?,
    siteName: String?,
    malId: Int? = null,
    aniListId: Int? = null,
    tmdbId: Int? = null,
    posterUrl: String? = null,
    titleEnglish: String? = null,
    titleRomaji: String? = null,
    titleNative: String? = null,
    startYear: Int? = null,
    isMovie: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val epNumText = if (episodeNumber != null) "$episodeNumber. Bölüm" else "Bölüm"
    val fullTitle = buildString {
        append(animeTitle)
        if (episodeNumber != null) append(" $episodeNumber. Bölüm")
    }

    val cleanEpTitle = episodeTitle?.let {
        it.replace(Regex("""^#\d+\s*[-–—]\s*"""), "")
          .replace(Regex("""^Bölüm\s+\d+\s*[-–—]\s*"""), "")
    }

    val displayEpTitle = if (!cleanEpTitle.isNullOrBlank() && cleanEpTitle != epNumText) {
        "$epNumText – $cleanEpTitle"
    } else {
        epNumText
    }

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        title = {
            Column {
                Text(
                    text = animeTitle,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayEpTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 0. Akış Eklentileri ile İzle
                if (episodeNumber != null) {
                    EpisodeOptionRow(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Akış Eklentileri ile İzle",
                        description = "Torrent ve RealDebrid akışlarını çözümler",
                        onClick = {
                        KitsugiStreamActivity.start(
                                context = context,
                                malId = malId,
                                aniListId = aniListId,
                                tmdbId = tmdbId,
                                episode = episodeNumber,
                                season = 1,
                                isMovie = isMovie,
                                title = animeTitle,
                                posterUrl = posterUrl,
                                titleEnglish = titleEnglish,
                                titleRomaji = titleRomaji,
                                titleNative = titleNative,
                                startYear = startYear
                            )
                            onDismiss()
                        }
                    )
                }

                // 1. Resmi Linke Git (eğer varsa)
                if (!originalUrl.isNullOrBlank()) {
                    val label = if (!siteName.isNullOrBlank()) "Resmi İzleme Adresi ($siteName)" else "Resmi Sayfaya Git"
                    EpisodeOptionRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = label,
                        description = "Lisans sahibi platformda izle",
                        onClick = { openUrl(originalUrl) }
                    )
                }

                // 2. YouTube'da Ara
                val ytQuery = URLEncoder.encode("$fullTitle English Sub", "UTF-8")
                val ytUrl = "https://www.youtube.com/results?search_query=$ytQuery"
                EpisodeOptionRow(
                    icon = Icons.Rounded.Search,
                    title = "YouTube'da Ara",
                    description = "Klipler, incelemeler veya fragmanlar için arat",
                    onClick = { openUrl(ytUrl) }
                )

                // 3. Google'da Ara (İngilizce)
                val googleQuery = URLEncoder.encode("Watch $fullTitle English Sub", "UTF-8")
                val googleUrl = "https://www.google.com/search?q=$googleQuery"
                EpisodeOptionRow(
                    icon = Icons.Rounded.Language,
                    title = "Google'da Ara (İngilizce)",
                    description = "Yabancı dizi/anime sitelerinde arat",
                    onClick = { openUrl(googleUrl) }
                )

                // 4. Türkçe Altyazılı Ara
                val trQuery = URLEncoder.encode("$fullTitle Türkçe altyazılı izle", "UTF-8")
                val trUrl = "https://www.google.com/search?q=$trQuery"
                EpisodeOptionRow(
                    icon = Icons.Rounded.Language,
                    title = "Türkçe Altyazılı Ara",
                    description = "Türkçe anime izleme sitelerinde arat",
                    onClick = { openUrl(trUrl) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
private fun EpisodeOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.SurfaceSoft)
            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
