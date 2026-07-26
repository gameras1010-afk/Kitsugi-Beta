package com.kitsugi.animelist.ui.tv.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusTarget
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiForumReply
import com.kitsugi.animelist.data.remote.KitsugiForumTopic
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

@Composable
fun TvTopicDetailDialog(
    topic: KitsugiForumTopic,
    source: String,
    apiClient: JikanApiClient,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var commentsList by remember { mutableStateOf<List<KitsugiForumReply>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isSubscribed by remember { mutableStateOf(false) }
    var isTopicLikedState by remember(topic.id) { mutableStateOf(topic.isLiked) }
    var topicLikeCountState by remember(topic.id) { mutableStateOf(topic.likeCount) }

    var selectedLanguage by remember { mutableStateOf("original") }
    var translatedTitle by remember { mutableStateOf<String?>(null) }
    val translatedComments = remember { mutableStateMapOf<Int, String>() }

    // Yorumları yükle
    LaunchedEffect(topic.id) {
        isLoading = true
        page = 1
        hasMore = true
        runCatching {
            commentsList = apiClient.fetchForumTopicReplies(topic.id, page = 1)
        }
        isLoading = false
    }

    // Çeviri işleme
    LaunchedEffect(selectedLanguage, commentsList) {
        if (selectedLanguage == "turkish") {
            if (translatedTitle == null) {
                coroutineScope.launch {
                    val tr = translationManager.translateToTurkish(topic.title)
                    if (tr.isNotBlank()) translatedTitle = tr
                }
            }
            commentsList.forEach { comment ->
                if (!translatedComments.containsKey(comment.id)) {
                    coroutineScope.launch {
                        val tr = translationManager.translateToTurkish(comment.comment)
                        if (tr.isNotBlank()) {
                            translatedComments[comment.id] = tr
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        if (isLoading || isLoadingMore || !hasMore) return
        isLoadingMore = true
        coroutineScope.launch {
            try {
                val nextPage = page + 1
                val newComments = apiClient.fetchForumTopicReplies(topic.id, page = nextPage)
                if (newComments.isEmpty()) {
                    hasMore = false
                } else {
                    commentsList = (commentsList + newComments).distinctBy { it.id }
                    page = nextPage
                }
            } catch (e: Exception) {
                android.util.Log.e("TvReviewsTabContent", "loadNextPage failed: ${e.message}", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(KitsugiColors.Background)
                .border(1.dp, KitsugiColors.SurfaceSoft, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Sol Panel: Metadata & Aksiyonlar ---
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val displayTitle = if (selectedLanguage == "turkish") translatedTitle ?: topic.title else topic.title
                    Text(
                        text = displayTitle,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(KitsugiColors.SurfaceSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!topic.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = topic.avatarUrl,
                                    contentDescription = topic.username,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = topic.username.take(1).uppercase(),
                                    color = KitsugiColors.TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Column {
                            Text(
                                text = topic.username,
                                color = KitsugiColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!topic.dateText.isNullOrBlank()) {
                                Text(
                                    text = topic.dateText,
                                    color = KitsugiColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Divider(color = KitsugiColors.SurfaceSoft)

                    // Beğeni Butonu
                    var isLikeFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLikeFocused) Color.White.copy(alpha = 0.1f) else KitsugiColors.Surface)
                            .border(
                                width = if (isLikeFocused) 1.5.dp else 0.dp,
                                color = if (isLikeFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isLikeFocused = it.isFocused }
                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                coroutineScope.launch {
                                    val success = apiClient.toggleLike(topic.id, "THREAD")
                                    if (success) {
                                        isTopicLikedState = !isTopicLikedState
                                        topicLikeCountState = if (isTopicLikedState) topicLikeCountState + 1 else topicLikeCountState - 1
                                    } else {
                                        Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isTopicLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Beğen",
                                tint = if (isTopicLikedState) accentColor else KitsugiColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$topicLikeCountState Beğeni",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Abone Ol Butonu (Jikan/MAL hariç)
                    if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                        var isSubFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSubFocused) Color.White.copy(alpha = 0.1f) else KitsugiColors.Surface)
                                .border(
                                    width = if (isSubFocused) 1.5.dp else 0.dp,
                                    color = if (isSubFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onFocusChanged { isSubFocused = it.isFocused }
                                .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                    coroutineScope.launch {
                                        val success = apiClient.toggleThreadSubscription(topic.id)
                                        if (success) {
                                            isSubscribed = !isSubscribed
                                            val msg = if (isSubscribed) "Abone olundu" else "Abonelik iptal edildi"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "İşlem başarısız, lütfen giriş yapın", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                    contentDescription = "Abone Ol",
                                    tint = if (isSubscribed) accentColor else KitsugiColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSubscribed) "Abone Olundu" else "Abone Ol",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dil Seçici
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var isOrigFocused by remember { mutableStateOf(false) }
                        val isOrigSelected = selectedLanguage == "original"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOrigSelected) accentColor else KitsugiColors.Surface)
                                .border(
                                    width = if (isOrigFocused) 1.5.dp else 0.dp,
                                    color = if (isOrigFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { isOrigFocused = it.isFocused }
                                .tvClickable(shape = RoundedCornerShape(10.dp)) { selectedLanguage = "original" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Orijinal",
                                color = if (isOrigSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        var isTrFocused by remember { mutableStateOf(false) }
                        val isTrSelected = selectedLanguage == "turkish"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTrSelected) accentColor else KitsugiColors.Surface)
                                .border(
                                    width = if (isTrFocused) 1.5.dp else 0.dp,
                                    color = if (isTrFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { isTrFocused = it.isFocused }
                                .tvClickable(shape = RoundedCornerShape(10.dp)) { selectedLanguage = "turkish" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Türkçe",
                                color = if (isTrSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Kapat
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = KitsugiColors.SurfaceStrong),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .border(
                                width = if (isCloseFocused) 1.5.dp else 0.dp,
                                color = if (isCloseFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kapat", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // --- Sağ Panel: Scrollable Yorum Listesi ---
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KitsugiColors.Surface)
                        .padding(12.dp)
                ) {
                    if (isLoading && commentsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else if (commentsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Yorum bulunmuyor veya bu kaynak çevrimdışı.",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(commentsList, key = { it.id }) { comment ->
                                val displayCommentText = if (selectedLanguage == "turkish") {
                                    translatedComments[comment.id] ?: comment.comment
                                } else {
                                    comment.comment
                                }

                                TvForumCommentCard(
                                    comment = comment,
                                    displayText = displayCommentText,
                                    apiClient = apiClient
                                )
                            }

                            if (hasMore) {
                                item {
                                    var isLoadMoreFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isLoadMoreFocused) Color.White.copy(alpha = 0.1f) else KitsugiColors.SurfaceStrong)
                                            .border(
                                                width = if (isLoadMoreFocused) 1.5.dp else 0.dp,
                                                color = if (isLoadMoreFocused) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .onFocusChanged { isLoadMoreFocused = it.isFocused }
                                            .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                                loadNextPage()
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text(
                                                text = "Daha Fazla Yükle",
                                                color = KitsugiColors.TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvForumCommentCard(
    comment: KitsugiForumReply,
    displayText: String,
    apiClient: JikanApiClient
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var likedState by remember(comment.id) { mutableStateOf(comment.isLiked) }
    var likeCountState by remember(comment.id) { mutableStateOf(comment.likeCount) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KitsugiColors.Background)
            .border(
                width = 1.dp,
                color = if (isFocused) Color.White else KitsugiColors.SurfaceSoft,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusTarget()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!comment.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = comment.avatarUrl,
                            contentDescription = comment.username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = comment.username.take(1).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = comment.username,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (!comment.dateText.isNullOrBlank()) {
                Text(
                    text = comment.dateText,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        var commentGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var commentGalleryIndex by remember { mutableStateOf(0) }
        KitsugiMarkdownText(
            text = displayText,
            onImageGalleryRequest = { urls, index ->
                commentGalleryImages = urls
                commentGalleryIndex = index
            }
        )
        if (commentGalleryImages.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                imageUrls = commentGalleryImages,
                initialIndex = commentGalleryIndex,
                title = comment.username,
                onDismiss = { commentGalleryImages = emptyList() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isLikeBtnFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isLikeBtnFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                    .onFocusChanged { isLikeBtnFocused = it.isFocused }
                    .tvClickable(shape = RoundedCornerShape(6.dp)) {
                        coroutineScope.launch {
                            val success = apiClient.toggleLike(comment.id, "THREAD_REPLY")
                            if (success) {
                                likedState = !likedState
                                likeCountState = if (likedState) likeCountState + 1 else likeCountState - 1
                            } else {
                                Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (likedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Beğen",
                    tint = if (likedState) accentColor else KitsugiColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = likeCountState.toString(),
                    color = if (likedState) accentColor else KitsugiColors.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
