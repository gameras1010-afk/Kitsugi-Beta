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
import androidx.compose.material.icons.automirrored.rounded.Comment
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
import com.kitsugi.animelist.data.remote.KitsugiForumTopic
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.launch

import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiAllTopicsBottomSheet(
    source: String,
    externalId: Int,
    mediaType: MediaType,
    apiClient: JikanApiClient,
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var topicsList by remember { mutableStateOf<List<KitsugiForumTopic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }

    // Translation states
    var selectedLanguage by remember { mutableStateOf("original") }
    val translatedTitles = remember { mutableStateMapOf<Int, String>() }

    var activeTopicForDetail by remember { mutableStateOf<KitsugiForumTopic?>(null) }

    val listState = rememberLazyListState()

    // Initial Load
    LaunchedEffect(externalId) {
        isLoading = true
        page = 1
        hasMore = true
        topicsList = apiClient.fetchForumTopics(source, externalId, mediaType, page = 1)
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
                val newTopics = apiClient.fetchForumTopics(source, externalId, mediaType, page = nextPage)
                if (newTopics.isEmpty()) {
                    hasMore = false
                } else {
                    topicsList = (topicsList + newTopics).distinctBy { it.id }
                    page = nextPage
                }
            } catch (e: Exception) {
                // T3-05: printStackTrace → Log.e
                android.util.Log.e("AllTopicsSheet", "loadNextPage failed: ${e.message}", e)
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
    LaunchedEffect(selectedLanguage, topicsList) {
        if (selectedLanguage == "turkish") {
            topicsList.forEach { topic ->
                if (!translatedTitles.containsKey(topic.id)) {
                    coroutineScope.launch {
                        val tr = translationManager.translateToTurkish(topic.title)
                        if (tr.isNotBlank()) {
                            translatedTitles[topic.id] = tr
                        }
                    }
                }
            }
        }
    }

    KitsugiSheetOrDialog(
        onDismiss = onDismiss
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
                    text = "Tüm Tartışma Konuları",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && topicsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (topicsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "Tartışma konusu bulunamadı.", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(topicsList, key = { it.id }) { topic ->
                        val displayTitle = if (selectedLanguage == "turkish") translatedTitles[topic.id] ?: topic.title else topic.title
                        
                        // Theme-aligned Topic Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(KitsugiColors.SurfaceStrong)
                                .tvClickable(shape = RoundedCornerShape(16.dp)) {
                                    Toast.makeText(context, "Yükleniyor...", Toast.LENGTH_SHORT).show()
                                    activeTopicForDetail = topic
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayTitle,
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { context.openTranslator(topic.title) },
                                        modifier = Modifier.size(28.dp)
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
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("topic_title", topic.title))
                                            Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Kopyala",
                                            tint = KitsugiColors.TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = if (onUserProfileClick != null) {
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUserProfileClick(topic.userId, topic.username, topic.avatarUrl) }
                                            .padding(4.dp)
                                    } else Modifier
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(KitsugiColors.SurfaceSoft)
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
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = topic.username,
                                        color = KitsugiColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp)) {
                                                coroutineScope.launch {
                                                    val success = apiClient.toggleLike(topic.id, "THREAD")
                                                    if (success) {
                                                        topicsList = topicsList.map {
                                                            if (it.id == topic.id) {
                                                                val newLiked = !it.isLiked
                                                                val newCount = if (newLiked) it.likeCount + 1 else it.likeCount - 1
                                                                it.copy(isLiked = newLiked, likeCount = newCount)
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (topic.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Beğen",
                                                tint = if (topic.isLiked) accentColor else KitsugiColors.TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = topic.likeCount.toString(),
                                                color = KitsugiColors.TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Comment,
                                            contentDescription = "Yorumlar",
                                            tint = KitsugiColors.TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = topic.commentCount.toString(),
                                            color = KitsugiColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (topic.viewCount > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.RemoveRedEye,
                                                contentDescription = "Görüntülenme",
                                                tint = KitsugiColors.TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = topic.viewCount.toString(),
                                                color = KitsugiColors.TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeTopicForDetail != null) {
        KitsugiTopicDetailBottomSheet(
            topic = activeTopicForDetail!!,
            source = source,
            apiClient = apiClient,
            onUserProfileClick = onUserProfileClick,
            onDismiss = { activeTopicForDetail = null }
        )
    }
}
