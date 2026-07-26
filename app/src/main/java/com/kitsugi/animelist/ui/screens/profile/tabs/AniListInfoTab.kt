package com.kitsugi.animelist.ui.screens.profile.tabs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import com.kitsugi.animelist.ui.screens.profile.StatCard

@Composable
fun AniListInfoTab(
    state: AniListProfileState,
    accentColor: Color,
    onImageClick: ((urls: List<String>, initialIndex: Int, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val displayAbout = state.about

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (displayAbout.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(KitsugiColors.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hakkında",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { context.openTranslator(displayAbout) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = "Çevir",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("about", displayAbout))
                                Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = KitsugiColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                com.kitsugi.animelist.ui.components.KitsugiHtmlWebView(
                    html = displayAbout,
                    modifier = Modifier.fillMaxWidth(),
                    onImageClick = { urls, idx ->
                        onImageClick?.invoke(urls, idx, "${state.name} Biyografisi")
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(KitsugiColors.Surface)
                .padding(18.dp)
        ) {
            Text(
                text = "Profil Özeti",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard("Anime", state.animeStats?.count?.toString() ?: "0")
                StatCard("Manga", state.mangaStats?.count?.toString() ?: "0")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard("Takipçi", state.socialState.followers.size.toString())
                StatCard("Takip Edilen", state.socialState.following.size.toString())
            }
        }
    }
}
