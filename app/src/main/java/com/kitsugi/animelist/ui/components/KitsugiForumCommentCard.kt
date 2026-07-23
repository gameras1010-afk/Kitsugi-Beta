package com.kitsugi.animelist.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiForumReply
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A single forum comment/reply card used inside topic detail sheets,
 * supporting recursive rendering of child comments.
 */
@Composable
internal fun KitsugiForumCommentCard(
    comment: KitsugiForumReply,
    displayText: String,
    apiClient: JikanApiClient,
    coroutineScope: CoroutineScope,
    selectedLanguage: String = "original",
    translatedComments: Map<Int, String> = emptyMap(),
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)? = null,
    onReplyClick: ((KitsugiForumReply) -> Unit)? = null
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current

    var isLikedState by remember { mutableStateOf(comment.isLiked) }
    var likeCountState by remember { mutableStateOf(comment.likeCount) }
    var isRepliesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KitsugiColors.SurfaceStrong, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Author row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onUserProfileClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUserProfileClick(comment.userId, comment.username, comment.avatarUrl) }
                        .padding(4.dp)
                } else Modifier
            ) {
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
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = getRelativeTime(comment.createdAt, comment.dateText),
                color = KitsugiColors.TextSecondary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var activeGalleryIndex by remember { mutableStateOf(0) }

        KitsugiMarkdownText(
            text = displayText,
            onImageGalleryRequest = { urls, index ->
                activeGalleryImages = urls
                activeGalleryIndex = index
            }
        )

        if (activeGalleryImages.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                imageUrls = activeGalleryImages,
                initialIndex = activeGalleryIndex,
                title = comment.username,
                onDismiss = { activeGalleryImages = emptyList() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions row (Translate, Copy, Reply, Like, Replies Toggle)
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
                if (onReplyClick != null) {
                    IconButton(
                        onClick = { onReplyClick(comment) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = "Yanıtla",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (comment.childComments.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isRepliesExpanded = !isRepliesExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isRepliesExpanded) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Yanıtlar",
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.childComments.size.toString(),
                            color = KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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

        // Child comments recursive rendering
        if (isRepliesExpanded && comment.childComments.isNotEmpty()) {
            comment.childComments.forEach { child ->
                KitsugiForumChildCommentRow(
                    comment = child,
                    level = 1,
                    apiClient = apiClient,
                    coroutineScope = coroutineScope,
                    selectedLanguage = selectedLanguage,
                    translatedComments = translatedComments,
                    onUserProfileClick = onUserProfileClick,
                    onReplyClick = onReplyClick
                )
            }
        }
    }
}

@Composable
private fun KitsugiForumChildCommentRow(
    comment: KitsugiForumReply,
    level: Int,
    apiClient: JikanApiClient,
    coroutineScope: CoroutineScope,
    selectedLanguage: String,
    translatedComments: Map<Int, String>,
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)?,
    onReplyClick: ((KitsugiForumReply) -> Unit)?
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current
    var isLikedState by remember { mutableStateOf(comment.isLiked) }
    var likeCountState by remember { mutableStateOf(comment.likeCount) }
    var isRepliesExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = 10.dp)
    ) {
        // Indentation line
        for (i in 0 until level) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(KitsugiColors.SurfaceSoft)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onUserProfileClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUserProfileClick(comment.userId, comment.username, comment.avatarUrl) }
                            .padding(vertical = 2.dp)
                    } else Modifier
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
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
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = comment.username,
                        color = KitsugiColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = getRelativeTime(comment.createdAt, comment.dateText),
                    color = KitsugiColors.TextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body
            val displayComment = if (selectedLanguage == "turkish") {
                translatedComments[comment.id] ?: comment.comment
            } else {
                comment.comment
            }

            var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
            var activeGalleryIndex by remember { mutableStateOf(0) }

            KitsugiMarkdownText(
                text = displayComment,
                onImageGalleryRequest = { urls, index ->
                    activeGalleryImages = urls
                    activeGalleryIndex = index
                }
            )

            if (activeGalleryImages.isNotEmpty()) {
                KitsugiImageGalleryDialog(
                    imageUrls = activeGalleryImages,
                    initialIndex = activeGalleryIndex,
                    title = comment.username,
                    onDismiss = { activeGalleryImages = emptyList() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Actions (Translate, Copy, Reply, Like, Nested Replies)
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
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = "Çevir",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("comment_text", comment.comment))
                            Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (onReplyClick != null) {
                        IconButton(
                            onClick = { onReplyClick(comment) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Reply,
                                contentDescription = "Yanıtla",
                                tint = KitsugiColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comment.childComments.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isRepliesExpanded = !isRepliesExpanded }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isRepliesExpanded) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                                contentDescription = "Yanıtlar",
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.childComments.size.toString(),
                                color = KitsugiColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
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
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (isLikedState) accentColor else KitsugiColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = likeCountState.toString(),
                            color = KitsugiColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isRepliesExpanded && comment.childComments.isNotEmpty()) {
                comment.childComments.forEach { child ->
                    KitsugiForumChildCommentRow(
                        comment = child,
                        level = level + 1,
                        apiClient = apiClient,
                        coroutineScope = coroutineScope,
                        selectedLanguage = selectedLanguage,
                        translatedComments = translatedComments,
                        onUserProfileClick = onUserProfileClick,
                        onReplyClick = onReplyClick
                    )
                }
            }
        }
    }
}

private fun getRelativeTime(createdAt: Int?, fallback: String?): String {
    if (createdAt == null || createdAt <= 0) return fallback.orEmpty()
    val diffMillis = System.currentTimeMillis() - (createdAt * 1000L)
    val diffSeconds = diffMillis / 1000L
    return when {
        diffSeconds < 0 -> "Şimdi"
        diffSeconds < 60 -> "Şimdi"
        diffSeconds < 3600 -> "${diffSeconds / 60}d önce"
        diffSeconds < 86400 -> "${diffSeconds / 3600}sa önce"
        diffSeconds < 604800 -> "${diffSeconds / 86400}g önce"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(createdAt * 1000L))
        }
    }
}
