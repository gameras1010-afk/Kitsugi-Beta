package com.kitsugi.animelist.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiForumReply
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A single forum comment/reply card used inside topic detail sheets.
 *
 * @param comment       The reply data to display.
 * @param displayText   Possibly-translated comment body to render (falls back to [comment.comment]).
 * @param apiClient     Client used to toggle likes.
 * @param coroutineScope Scope for like toggle coroutines.
 */
@Composable
internal fun KitsugiForumCommentCard(
    comment: KitsugiForumReply,
    displayText: String,
    apiClient: JikanApiClient,
    coroutineScope: CoroutineScope
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current

    var isLikedState by remember { mutableStateOf(comment.isLiked) }
    var likeCountState by remember { mutableStateOf(comment.likeCount) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KitsugiColors.SurfaceStrong, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Author row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.SurfaceSoft)
            ) {
                if (!comment.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = comment.avatarUrl,
                        contentDescription = comment.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = comment.username.take(1).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = comment.username,
                color = KitsugiColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (!comment.dateText.isNullOrBlank()) {
                Text(
                    text = comment.dateText,
                    color = KitsugiColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        KitsugiMarkdownText(text = displayText)

        Spacer(modifier = Modifier.height(8.dp))

        // Actions row (Translate, Copy, Like)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { context.openTranslator(comment.comment) },
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
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("comment_text", comment.comment))
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp)) {
                    coroutineScope.launch {
                        val success = apiClient.toggleLike(comment.id, "THREAD_COMMENT")
                        if (success) {
                            isLikedState = !isLikedState
                            likeCountState = if (isLikedState) likeCountState + 1 else likeCountState - 1
                        } else {
                            Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Beğen",
                    tint = if (isLikedState) accentColor else KitsugiColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = likeCountState.toString(),
                    color = KitsugiColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
