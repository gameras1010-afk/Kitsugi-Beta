package com.kitsugi.animelist.ui.components

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
import com.kitsugi.animelist.data.remote.KitsugiActivity
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiAllActivitiesBottomSheet(
    source: String,
    externalId: Int,
    mediaType: MediaType = MediaType.Anime,
    apiClient: JikanApiClient,
    titleLanguage: String = "ROMAJI",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var activitiesList by remember { mutableStateOf<List<KitsugiActivity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }

    // Translation states
    var selectedLanguage by remember { mutableStateOf("original") }
    val translatedTexts = remember { mutableStateMapOf<Int, String>() }

    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }


    val listState = rememberLazyListState()

    // Initial Load
    LaunchedEffect(externalId) {
        isLoading = true
        page = 1
        hasMore = true
        activitiesList = apiClient.fetchActivities(source, externalId, page = 1, mediaType = mediaType)
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
                val newActs = apiClient.fetchActivities(source, externalId, page = nextPage, mediaType = mediaType)
                if (newActs.isEmpty()) {
                    hasMore = false
                } else {
                    activitiesList = (activitiesList + newActs).distinctBy { it.id }
                    page = nextPage
                }
            } catch (e: Exception) {
                // T3-05: printStackTrace → Log.e
                android.util.Log.e("AllActivitiesSheet", "loadNextPage failed: ${e.message}", e)
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
    LaunchedEffect(selectedLanguage, activitiesList) {
        if (selectedLanguage == "turkish") {
            activitiesList.forEach { act ->
                if (act.mediaTitle == null && !translatedTexts.containsKey(act.id)) {
                    coroutineScope.launch {
                        val tr = translationManager.translateToTurkish(act.text)
                        if (tr.isNotBlank()) {
                            translatedTexts[act.id] = tr
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
                    text = "Tüm Aktiviteler",
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

            if (isLoading && activitiesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (activitiesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "Aktivite bulunamadı.", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activitiesList, key = { it.id }) { act ->
                        // Resolve title according to user's titleLanguage preference
                        val localizedTitle = when (titleLanguage) {
                            "ENGLISH" -> act.mediaTitleEnglish?.takeIf { it.isNotBlank() }
                                ?: act.mediaTitleRomaji
                                ?: act.mediaTitleNative
                                ?: act.mediaTitle
                            "NATIVE", "JAPANESE_STAFF" -> act.mediaTitleNative?.takeIf { it.isNotBlank() }
                                ?: act.mediaTitleRomaji
                                ?: act.mediaTitleEnglish
                                ?: act.mediaTitle
                            else -> act.mediaTitleRomaji
                                ?: act.mediaTitleEnglish
                                ?: act.mediaTitleNative
                                ?: act.mediaTitle
                        }
                        // For ListActivities: regenerate display text with localized title
                        val localizedDisplayText = if (act.mediaTitle != null && localizedTitle != null && localizedTitle != act.mediaTitle) {
                            act.text.replace("**${act.mediaTitle}**", "**$localizedTitle**")
                        } else act.text
                        val displayText = if (selectedLanguage == "turkish") translatedTexts[act.id] ?: localizedDisplayText else localizedDisplayText
                        
                        // Theme-aligned Activity Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(KitsugiColors.SurfaceStrong)
                                .tvClickable(shape = RoundedCornerShape(16.dp)) {
                                    Toast.makeText(context, "Yükleniyor...", Toast.LENGTH_SHORT).show()
                                    activeActivityIdForDetail = act.id
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(KitsugiColors.SurfaceSoft)
                                ) {
                                    if (!act.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = act.avatarUrl,
                                            contentDescription = act.username,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = act.username.take(1).uppercase(),
                                                color = KitsugiColors.TextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = act.username,
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                if (!act.dateText.isNullOrBlank()) {
                                    Text(
                                        text = act.dateText,
                                        color = KitsugiColors.TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    KitsugiMarkdownText(
                                        text = displayText,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (!act.mediaCoverUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AsyncImage(
                                        model = act.mediaCoverUrl,
                                        contentDescription = act.mediaTitle,
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp)) {
                                        if (source.lowercase() == "jikan" || source.lowercase() == "mal") {
                                            Toast.makeText(context, "Beğeni özelliği MAL kaynağı için desteklenmemektedir.", Toast.LENGTH_SHORT).show()
                                            return@tvClickable
                                        }
                                        coroutineScope.launch {
                                            val success = apiClient.toggleLike(act.id, "ACTIVITY")
                                            if (success) {
                                                activitiesList = activitiesList.map {
                                                    if (it.id == act.id) {
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
                                        imageVector = if (act.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Beğeni",
                                        tint = if (act.isLiked) accentColor else KitsugiColors.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = act.likeCount.toString(),
                                        color = KitsugiColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
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

    if (activeActivityIdForDetail != null) {
        KitsugiActivityDetailBottomSheet(
            activityId = activeActivityIdForDetail!!,
            apiClient = apiClient,
            titleLanguage = titleLanguage,
            onDismiss = { activeActivityIdForDetail = null }
        )
    }
}
