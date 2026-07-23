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
import androidx.compose.ui.text.style.TextOverflow
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
    val displaySummary = rev.summary ?: ""

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .tvClickable(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(16.dp)
            .height(144.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Center-aligned italic summary text
        Text(
            text = displaySummary.replace(Regex("[*_`~]"), ""),
            color = KitsugiColors.TextPrimary,
            fontSize = 13.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        // Bottom stats & author row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Stats (Rating, Helpful)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rev.score != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Puan",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = rev.score.toString(),
                            color = KitsugiColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if ((rev.helpfulCount != null && rev.helpfulCount > 0) || rev.id != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onHelpfulClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = "Faydalı",
                            tint = if (rev.userRating == "UP_VOTE") accentColor else KitsugiColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${rev.helpfulCount ?: 0}",
                            color = if (rev.userRating == "UP_VOTE") accentColor else KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Author Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onUserProfileClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUserProfileClick(rev.userId, rev.username, rev.avatarUrl) }
                        .padding(4.dp)
                } else Modifier
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
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
                            Text(
                                text = rev.username.take(1).uppercase(),
                                color = KitsugiColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = rev.username,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
            }
        }
    }
}
