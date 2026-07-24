package com.kitsugi.animelist.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
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
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiForumReply
import com.kitsugi.animelist.data.remote.KitsugiForumTopic
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.launch

import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiTopicDetailBottomSheet(
    topic: KitsugiForumTopic,
    source: String,
    apiClient: JikanApiClient,
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)? = null,
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
    var showReplyEditor by remember { mutableStateOf(false) }
    var replyTargetComment by remember { mutableStateOf<KitsugiForumReply?>(null) }
    var isTopicLikedState by remember(topic.id) { mutableStateOf(topic.isLiked) }
    var topicLikeCountState by remember(topic.id) { mutableStateOf(topic.likeCount) }

    // Translation states
    var selectedLanguage by remember { mutableStateOf("original") }
    var translatedTitle by remember { mutableStateOf<String?>(null) }
    val translatedComments = remember { mutableStateMapOf<Int, String>() }

    val listState = rememberLazyListState()

    // Load initial comments
    LaunchedEffect(topic.id) {
        isLoading = true
        page = 1
        hasMore = true
        commentsList = apiClient.fetchForumTopicReplies(topic.id, page = 1)
        isLoading = false
    }

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 8 && totalItems > 0
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
                // T3-05: printStackTrace → Log.e
                android.util.Log.e("TopicDetailSheet", "loadNextPage failed: ${e.message}", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            loadNextPage()
        }
    }

    // Translation orchestration
    LaunchedEffect(selectedLanguage, commentsList) {
        if (selectedLanguage == "turkish") {
            // Translate topic title
            if (translatedTitle == null) {
                coroutineScope.launch {
                    val tr = translationManager.translateToTurkish(topic.title)
                    if (tr.isNotBlank()) translatedTitle = tr
                }
            }
            // Recursive translation helper
            fun translateRecursive(comment: KitsugiForumReply) {
                if (!translatedComments.containsKey(comment.id)) {
                    coroutineScope.launch {
                        val tr = translationManager.translateToTurkish(comment.comment)
                        if (tr.isNotBlank()) {
                            translatedComments[comment.id] = tr
                        }
                    }
                }
                comment.childComments.forEach { child ->
                    translateRecursive(child)
                }
            }
            // Translate comments recursively
            commentsList.forEach { comment ->
                translateRecursive(comment)
            }
        }
    }

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        innerScrollState = listState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = KitsugiColors.SurfaceStrong
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextPrimary
                    )
                }

                Text(
                    text = "Konu Detayı",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = apiClient.toggleThreadSubscription(topic.id)
                                if (success) {
                                    isSubscribed = !isSubscribed
                                    val msg = if (isSubscribed) "Abone olundu" else "Abonelik iptal edildi"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "İşlem başarısız, lütfen giriş durumunuzu kontrol edin", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.SurfaceStrong
                        )
                    ) {
                        Icon(
                            imageVector = if (isSubscribed) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                            contentDescription = "Abone Ol",
                            tint = if (isSubscribed) accentColor else KitsugiColors.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Switcher Pills + Translate / Copy Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isOrigSelected = selectedLanguage == "original"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOrigSelected) accentColor else KitsugiColors.SurfaceStrong)
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { selectedLanguage = "original" }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Orijinal",
                            color = if (isOrigSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isTrSelected = selectedLanguage == "turkish"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTrSelected) accentColor else KitsugiColors.SurfaceStrong)
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { selectedLanguage = "turkish" }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Türkçe",
                            color = if (isTrSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { context.openTranslator(topic.title) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = "Çevir",
                            tint = accentColor
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("topic_title", topic.title))
                            Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = KitsugiColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item 1: Topic title & Author Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        val displayTitle = if (selectedLanguage == "turkish") translatedTitle ?: topic.title else topic.title
                        Text(
                            text = displayTitle,
                            color = KitsugiColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                        if (!topic.dateText.isNullOrBlank()) {
                            Text(
                                text = topic.dateText,
                                color = KitsugiColors.TextSecondary,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = KitsugiColors.Border.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(KitsugiColors.SurfaceSoft)
                                        .then(
                                            if (onUserProfileClick != null) {
                                                Modifier.clickable { onUserProfileClick(topic.userId, topic.username, topic.avatarUrl) }
                                            } else Modifier
                                        )
                                ) {
                                    if (!topic.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = topic.avatarUrl,
                                            contentDescription = topic.username,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = topic.username.take(1).uppercase(),
                                                color = KitsugiColors.TextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = topic.username,
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = if (onUserProfileClick != null) {
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onUserProfileClick(topic.userId, topic.username, topic.avatarUrl) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    } else Modifier
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Like/Favorite
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .tvClickable {
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
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTopicLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Beğen",
                                        tint = if (isTopicLikedState) accentColor else KitsugiColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = topicLikeCountState.toString(),
                                        color = KitsugiColors.TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }

                                // Reply
                                if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                                    IconButton(
                                        onClick = { showReplyEditor = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                                            contentDescription = "Yanıtla",
                                            tint = KitsugiColors.TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = KitsugiColors.Border.copy(alpha = 0.3f))
                    }
                }

                // Section Header
                item {
                    Text(
                        text = "Yorumlar",
                        color = KitsugiColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                // Loading State
                if (isLoading && commentsList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                } else if (commentsList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Henüz yanıt bulunmuyor veya bu kaynak çevrimdışı görüntülüyor.",
                                color = KitsugiColors.TextMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    // Comments list items
                    items(commentsList, key = { it.id }) { comment ->
                        val displayComment = if (selectedLanguage == "turkish") {
                            translatedComments[comment.id] ?: comment.comment
                        } else {
                            comment.comment
                        }
                        KitsugiForumCommentCard(
                            comment = comment,
                            displayText = displayComment,
                            apiClient = apiClient,
                            coroutineScope = coroutineScope,
                            selectedLanguage = selectedLanguage,
                            translatedComments = translatedComments,
                            onUserProfileClick = onUserProfileClick,
                            onReplyClick = { target ->
                                replyTargetComment = target
                                showReplyEditor = true
                            }
                        )
                    }

                    // Load More Indicator
                    if (isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                Button(
                    onClick = { showReplyEditor = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Reply,
                        contentDescription = "Yanıtla",
                        tint = KitsugiColors.Background
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yanıtla", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showReplyEditor) {
        val editorTitle = if (replyTargetComment != null) {
            "${replyTargetComment?.username} Kullanıcısına Yanıt Ver"
        } else {
            "Konuyu Yanıtla"
        }
        val editorPlaceholder = if (replyTargetComment != null) {
            "Yanıtınızı buraya yazın..."
        } else {
            "Mesajınızı buraya yazın..."
        }
        KitsugiReplyEditorBottomSheet(
            title = editorTitle,
            placeholder = editorPlaceholder,
            onPublish = { text ->
                apiClient.postReply(
                    targetId = topic.id,
                    isActivity = false,
                    text = text,
                    parentCommentId = replyTargetComment?.id
                )
            },
            onDismiss = {
                showReplyEditor = false
                replyTargetComment = null
                // Reload comments after posting
                coroutineScope.launch {
                    isLoading = true
                    commentsList = apiClient.fetchForumTopicReplies(topic.id, page = 1)
                    isLoading = false
                }
            }
        )
    }
}
