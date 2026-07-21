package com.kitsugi.animelist.ui.screens.detail

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiActivity
import com.kitsugi.animelist.data.remote.KitsugiForumTopic
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.components.KitsugiActivityDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiAllActivitiesBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiAllReviewsBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiAllTopicsBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.components.KitsugiReviewDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiTopicDetailBottomSheet
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.components.KitsugiShimmerSearchResultList
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.launch

@Composable
fun ReviewsTabContent(
    state: DetailTabState<List<KitsugiReview>>,
    source: String,
    externalId: Int,
    mediaType: MediaType,
    apiClient: JikanApiClient,
    titleLanguage: String = "ROMAJI",
    preferredTranslator: String = "DEFAULT"
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var activeReviewForDetail by remember { mutableStateOf<KitsugiReview?>(null) }
    var activeTopicForDetail by remember { mutableStateOf<KitsugiForumTopic?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }
    
    var showAllTopicsSheet by remember { mutableStateOf(false) }
    var showAllActivitiesSheet by remember { mutableStateOf(false) }
    var showAllReviewsSheet by remember { mutableStateOf(false) }

    val isTmdbOrSimkl = source.equals("tmdb", ignoreCase = true) || source.equals("simkl", ignoreCase = true)

    var forumTopics by remember { mutableStateOf<List<KitsugiForumTopic>>(emptyList()) }
    var activitiesList by remember { mutableStateOf<List<KitsugiActivity>>(emptyList()) }

    // TMDB / Simkl kaynakları için forum/aktivite desteği yok — API çağrısını atla
    LaunchedEffect(source, externalId, mediaType) {
        if (!isTmdbOrSimkl) {
            coroutineScope.launch {
                runCatching {
                    forumTopics = apiClient.fetchForumTopics(source, externalId, mediaType)
                }
            }
            coroutineScope.launch {
                runCatching {
                    activitiesList = apiClient.fetchActivities(source, externalId, mediaType = mediaType)
                }
            }
        }
    }

    var reviewsList by remember(state) {
        mutableStateOf(if (state is DetailTabState.Success) state.data else emptyList())
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- 1. FORUM TOPICS SECTION ---
        if (forumTopics.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tartışma Konuları",
                        color = KitsugiColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(
                        onClick = { showAllTopicsSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.SurfaceStrong
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.List,
                            contentDescription = "Tümünü Gör",
                            tint = KitsugiColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(forumTopics) { topic ->
                        TopicCard(
                            topic = topic,
                            onClick = {
                                Toast.makeText(context, "Yükleniyor...", Toast.LENGTH_SHORT).show()
                                activeTopicForDetail = topic
                            },
                            onLikeClick = if (source.lowercase() != "jikan" && source.lowercase() != "mal") {
                                {
                                    coroutineScope.launch {
                                        val success = apiClient.toggleLike(topic.id, "THREAD")
                                        if (success) {
                                            forumTopics = forumTopics.map {
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
                            } else null
                        )
                    }
                }
            }
        }

        // --- 2. ACTIVITIES SECTION ---
        if (activitiesList.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktiviteler",
                        color = KitsugiColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(
                        onClick = { showAllActivitiesSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.SurfaceStrong
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.List,
                            contentDescription = "Tümünü Gör",
                            tint = KitsugiColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activitiesList) { activity ->
                        ActivityCard(
                            activity = activity,
                            titleLanguage = titleLanguage,
                            onClick = {
                                Toast.makeText(context, "Yükleniyor...", Toast.LENGTH_SHORT).show()
                                activeActivityIdForDetail = activity.id
                            },
                            onLikeClick = {
                                if (source.lowercase() == "jikan" || source.lowercase() == "mal") {
                                    Toast.makeText(context, "Beğeni özelliği MAL kaynağı için desteklenmemektedir.", Toast.LENGTH_SHORT).show()
                                    return@ActivityCard
                                }
                                coroutineScope.launch {
                                    val success = apiClient.toggleLike(activity.id, "ACTIVITY")
                                    if (success) {
                                        activitiesList = activitiesList.map {
                                            if (it.id == activity.id) {
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
                        )
                    }
                }
            }
        }

        // --- 3. REVIEWS SECTION ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İncelemeler",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = { showAllReviewsSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = KitsugiColors.SurfaceStrong
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.List,
                        contentDescription = "Tümünü Gör",
                        tint = KitsugiColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            when (state) {
                is DetailTabState.Loading -> {
                    KitsugiShimmerSearchResultList(itemCount = 2)
                }
                is DetailTabState.Error -> {
                    Text(
                        text = "İncelemeler yüklenirken hata oluştu.",
                        color = KitsugiColors.AccentRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                    )
                }
                is DetailTabState.Success -> {
                    if (reviewsList.isEmpty()) {
                        Text(
                            text = "Henüz inceleme yazılmamış.",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reviewsList) { rev ->
                                KitsugiReviewCard(
                                    rev = rev,
                                    modifier = Modifier.width(280.dp),
                                    preferredTranslator = preferredTranslator,
                                    onClick = { activeReviewForDetail = rev },
                                    onHelpfulClick = {
                                        if (rev.id == null) {
                                            Toast.makeText(context, "Beğeni özelliği MAL kaynağı için desteklenmemektedir.", Toast.LENGTH_SHORT).show()
                                            return@KitsugiReviewCard
                                        }
                                        coroutineScope.launch {
                                            val currentRating = rev.userRating
                                            val newRating = if (currentRating == "UP_VOTE") "NO_RATING" else "UP_VOTE"
                                            val success = apiClient.rateReview(rev.id, newRating)
                                            if (success) {
                                                reviewsList = reviewsList.map {
                                                    if (it.id == rev.id) {
                                                        val diff = if (newRating == "UP_VOTE") 1 else -1
                                                        val oldCount = it.helpfulCount ?: 0
                                                        it.copy(
                                                            userRating = newRating,
                                                            helpfulCount = (oldCount + diff).coerceAtLeast(0)
                                                        )
                                                    } else {
                                                        it
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEETS TRIGGERS ---
    if (showAllTopicsSheet) {
        KitsugiAllTopicsBottomSheet(
            source = source,
            externalId = externalId,
            mediaType = mediaType,
            apiClient = apiClient,
            onDismiss = { showAllTopicsSheet = false }
        )
    }

    if (showAllActivitiesSheet) {
        KitsugiAllActivitiesBottomSheet(
            source = source,
            externalId = externalId,
            mediaType = mediaType,
            apiClient = apiClient,
            titleLanguage = titleLanguage,
            onDismiss = { showAllActivitiesSheet = false }
        )
    }

    if (showAllReviewsSheet) {
        KitsugiAllReviewsBottomSheet(
            source = source,
            externalId = externalId,
            mediaType = mediaType,
            apiClient = apiClient,
            onDismiss = { showAllReviewsSheet = false }
        )
    }

    if (activeReviewForDetail != null) {
        KitsugiReviewDetailBottomSheet(
            review = activeReviewForDetail!!,
            apiClient = apiClient,
            onDismiss = { activeReviewForDetail = null }
        )
    }

    if (activeTopicForDetail != null) {
        KitsugiTopicDetailBottomSheet(
            topic = activeTopicForDetail!!,
            source = source,
            apiClient = apiClient,
            onDismiss = { activeTopicForDetail = null }
        )
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

@Composable
private fun TopicCard(
    topic: KitsugiForumTopic,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = topic.title,
            color = KitsugiColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 36.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
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
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = topic.username,
                color = KitsugiColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onLikeClick != null) {
                val accentColor = LocalKitsugiAccent.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp), onClick = onLikeClick)
                ) {
                    Icon(
                        imageVector = if (topic.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (topic.isLiked) accentColor else KitsugiColors.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = topic.likeCount.toString(),
                        color = if (topic.isLiked) accentColor else KitsugiColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = KitsugiColors.TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = topic.commentCount.toString(),
                    color = KitsugiColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            if (topic.viewCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = topic.viewCount.toString(),
                        color = KitsugiColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!topic.dateText.isNullOrBlank()) {
                Text(
                    text = topic.dateText,
                    color = KitsugiColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: KitsugiActivity,
    titleLanguage: String = "ROMAJI",
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(12.dp)
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
                if (!activity.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = activity.avatarUrl,
                        contentDescription = activity.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = activity.username.take(1).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activity.username,
                color = KitsugiColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 70.dp)
            ) {
                // Resolve localized title for ListActivity entries
                val localizedTitle = when (titleLanguage) {
                    "ENGLISH" -> activity.mediaTitleEnglish?.takeIf { it.isNotBlank() }
                        ?: activity.mediaTitleRomaji
                        ?: activity.mediaTitleNative
                        ?: activity.mediaTitle
                    "NATIVE", "JAPANESE_STAFF" -> activity.mediaTitleNative?.takeIf { it.isNotBlank() }
                        ?: activity.mediaTitleRomaji
                        ?: activity.mediaTitleEnglish
                        ?: activity.mediaTitle
                    else -> activity.mediaTitleRomaji
                        ?: activity.mediaTitleEnglish
                        ?: activity.mediaTitleNative
                        ?: activity.mediaTitle
                }
                val displayText = if (activity.mediaTitle != null && localizedTitle != null && localizedTitle != activity.mediaTitle) {
                    activity.text.replace("**${activity.mediaTitle}**", "**$localizedTitle**")
                } else activity.text
                KitsugiMarkdownText(
                    text = displayText,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!activity.mediaCoverUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = activity.mediaCoverUrl,
                    contentDescription = activity.mediaTitle,
                    modifier = Modifier
                        .size(width = 36.dp, height = 50.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp), onClick = onLikeClick)
            ) {
                Icon(
                    imageVector = if (activity.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (activity.isLiked) accentColor else KitsugiColors.TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = activity.likeCount.toString(),
                    color = if (activity.isLiked) accentColor else KitsugiColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            if (!activity.dateText.isNullOrBlank()) {
                Text(
                    text = activity.dateText.take(11),
                    color = KitsugiColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
