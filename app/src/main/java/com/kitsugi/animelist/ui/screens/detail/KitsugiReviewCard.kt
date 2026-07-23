package com.kitsugi.animelist.ui.screens.detail

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator

import androidx.compose.foundation.clickable

/**
 * İnceleme (review) kart bileşeni.
 * ReviewsTab.kt dosyasından çıkarılmıştır.
 */
@Composable
internal fun KitsugiReviewCard(
    rev: KitsugiReview,
    modifier: Modifier = Modifier.fillMaxWidth(),
    preferredTranslator: String = "DEFAULT",
    backgroundColor: androidx.compose.ui.graphics.Color = KitsugiColors.Surface,
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)? = null,
    onClick: () -> Unit,
    onHelpfulClick: () -> Unit,
    onImageGalleryRequest: ((urls: List<String>, index: Int) -> Unit)? = null
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current
    // İnceleme özeti: kullanıcı 🌐 butonuna basana kadar orijinal metin gösterilir
    val displaySummary = rev.summary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .tvClickable(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onUserProfileClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUserProfileClick(rev.userId, rev.username, rev.avatarUrl) }
                        .padding(4.dp)
                } else Modifier,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.SurfaceSoft)
                ) {
                    if (!rev.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = rev.avatarUrl,
                            contentDescription = rev.username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(rev.username.take(1).uppercase(), color = KitsugiColors.TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = rev.username,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (rev.score != null) {
                Text(
                    text = "★ ${rev.score}",
                    color = accentColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black
                )
            }

            if (!rev.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { context.openTranslator(rev.summary, preferredTranslator) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = "Çevir",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("review", displaySummary))
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
        Spacer(modifier = Modifier.height(10.dp))
        KitsugiMarkdownText(
            text = displaySummary,
            onImageGalleryRequest = onImageGalleryRequest
        )

        if ((rev.helpfulCount != null && rev.helpfulCount > 0) || rev.id != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp), onClick = onHelpfulClick)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = "Faydalı",
                        tint = if (rev.userRating == "UP_VOTE") accentColor else KitsugiColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${rev.helpfulCount ?: 0} oylama",
                        color = if (rev.userRating == "UP_VOTE") accentColor else KitsugiColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
