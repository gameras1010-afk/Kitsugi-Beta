package com.kitsugi.animelist.ui.tv.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.kitsugi.animelist.ui.screens.detail.DetailTabState
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
//  TvReviewsTabContent — Main Entry Point
// ---------------------------------------------------------------------------

@Composable
fun TvReviewsTabContent(
    state: DetailTabState<List<KitsugiReview>>,
    source: String,
    externalId: Int,
    mediaType: MediaType,
    apiClient: JikanApiClient,
    titleLanguage: String = "ROMAJI",
    focusRequester: FocusRequester,
    focusUp: FocusRequester,
    focusDown: FocusRequester
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var forumTopics by remember { mutableStateOf<List<KitsugiForumTopic>>(emptyList()) }
    var activitiesList by remember { mutableStateOf<List<KitsugiActivity>>(emptyList()) }

    var activeReviewForDetail by remember { mutableStateOf<KitsugiReview?>(null) }
    var activeTopicForDetail by remember { mutableStateOf<KitsugiForumTopic?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }

    val isTmdbOrSimkl = source.equals("tmdb", ignoreCase = true) || source.equals("simkl", ignoreCase = true)

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

    val reviewsList = remember(state) {
        if (state is DetailTabState.Success) state.data else emptyList()
    }

    val hasTopics = forumTopics.isNotEmpty()
    val hasActivities = activitiesList.isNotEmpty()
    val hasReviews = reviewsList.isNotEmpty()

    if (hasTopics || hasActivities || hasReviews) {
        val topicsRowFocusRequester = remember { FocusRequester() }
        val activitiesRowFocusRequester = remember { FocusRequester() }
        val reviewsRowFocusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap)
        ) {
            // --- 1. FORUM TOPICS ROW ---
            if (hasTopics) {
                Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                    Text(
                        text = "Tartışma Konuları",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KitsugiColors.TextPrimary
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(if (hasTopics) focusRequester else topicsRowFocusRequester)
                            .focusGroup()
                            .focusProperties {
                                up = focusUp
                                down = when {
                                    hasActivities -> activitiesRowFocusRequester
                                    hasReviews    -> reviewsRowFocusRequester
                                    else          -> focusDown
                                }
                            },
                        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(forumTopics) { topic ->
                            TvTopicCard(
                                topic = topic,
                                onClick = { activeTopicForDetail = topic },
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
                                                    } else it
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

            // --- 2. ACTIVITIES ROW ---
            if (hasActivities) {
                Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                    Text(
                        text = "Aktiviteler",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KitsugiColors.TextPrimary
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(if (!hasTopics) focusRequester else activitiesRowFocusRequester)
                            .focusGroup()
                            .focusProperties {
                                up = if (hasTopics) topicsRowFocusRequester else focusUp
                                down = if (hasReviews) reviewsRowFocusRequester else focusDown
                            },
                        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(activitiesList) { activity ->
                            TvActivityCard(
                                activity = activity,
                                titleLanguage = titleLanguage,
                                onClick = { activeActivityIdForDetail = activity.id },
                                onLikeClick = {
                                    if (source.lowercase() == "jikan" || source.lowercase() == "mal") {
                                        Toast.makeText(context, "Beğeni özelliği MAL kaynağı için desteklenmemektedir.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        coroutineScope.launch {
                                            val success = apiClient.toggleLike(activity.id, "ACTIVITY")
                                            if (success) {
                                                activitiesList = activitiesList.map {
                                                    if (it.id == activity.id) {
                                                        val newLiked = !it.isLiked
                                                        val newCount = if (newLiked) it.likeCount + 1 else it.likeCount - 1
                                                        it.copy(isLiked = newLiked, likeCount = newCount)
                                                    } else it
                                                }
                                            } else {
                                                Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // --- 3. REVIEWS ROW ---
            if (hasReviews) {
                Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                    Text(
                        text = "İncelemeler",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KitsugiColors.TextPrimary
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(if (!hasTopics && !hasActivities) focusRequester else reviewsRowFocusRequester)
                            .focusGroup()
                            .focusProperties {
                                up = when {
                                    hasActivities -> activitiesRowFocusRequester
                                    hasTopics     -> topicsRowFocusRequester
                                    else          -> focusUp
                                }
                                down = focusDown
                            },
                        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(reviewsList.take(10)) { review ->
                            TvReviewCard(
                                review = review,
                                onClick = { activeReviewForDetail = review }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DETAY DIALOG TETİKLEYİCİLERİ ---
    if (activeReviewForDetail != null) {
        TvReviewDetailDialog(
            review = activeReviewForDetail!!,
            apiClient = apiClient,
            onDismiss = { activeReviewForDetail = null }
        )
    }

    if (activeTopicForDetail != null) {
        TvTopicDetailDialog(
            topic = activeTopicForDetail!!,
            source = source,
            apiClient = apiClient,
            onDismiss = { activeTopicForDetail = null }
        )
    }

    if (activeActivityIdForDetail != null) {
        TvActivityDetailDialog(
            activityId = activeActivityIdForDetail!!,
            apiClient = apiClient,
            titleLanguage = titleLanguage,
            onDismiss = { activeActivityIdForDetail = null }
        )
    }
}

// ---------------------------------------------------------------------------
//  TV List Cards (Forum Topic, Activity, Review)
// ---------------------------------------------------------------------------

@Composable
private fun TvTopicCard(
    topic: KitsugiForumTopic,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
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
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = topic.username,
                color = KitsugiColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onLikeClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .tvClickable(shape = RoundedCornerShape(6.dp), onClick = onLikeClick)
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
private fun TvActivityCard(
    activity: KitsugiActivity,
    titleLanguage: String = "ROMAJI",
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.SurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                if (!activity.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = activity.avatarUrl,
                        contentDescription = activity.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = activity.username.take(1).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 50.dp)
            ) {
                val localizedTitle = when (titleLanguage) {
                    "ENGLISH" -> activity.mediaTitleEnglish?.takeIf { it.isNotBlank() }
                        ?: activity.mediaTitleRomaji ?: activity.mediaTitleNative ?: activity.mediaTitle
                    "NATIVE", "JAPANESE_STAFF" -> activity.mediaTitleNative?.takeIf { it.isNotBlank() }
                        ?: activity.mediaTitleRomaji ?: activity.mediaTitleEnglish ?: activity.mediaTitle
                    else -> activity.mediaTitleRomaji ?: activity.mediaTitleEnglish ?: activity.mediaTitleNative ?: activity.mediaTitle
                }
                val displayText = if (activity.mediaTitle != null && localizedTitle != null && localizedTitle != activity.mediaTitle) {
                    activity.text.replace("**${activity.mediaTitle}**", "**$localizedTitle**")
                } else activity.text

                com.kitsugi.animelist.ui.components.KitsugiMarkdownText(
                    text = displayText,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!activity.mediaCoverUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = activity.mediaCoverUrl,
                    contentDescription = activity.mediaTitle,
                    modifier = Modifier
                        .size(width = 30.dp, height = 42.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .tvClickable(shape = RoundedCornerShape(6.dp), onClick = onLikeClick)
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

@Composable
private fun TvReviewCard(
    review: KitsugiReview,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.SurfaceStrong),
                contentAlignment = Alignment.Center
            ) {
                if (!review.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.avatarUrl,
                        contentDescription = review.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = review.username.take(1).uppercase(),
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = review.username,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (review.score != null && review.score > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${review.score}/10",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Text(
            text = review.summary,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!review.dateText.isNullOrBlank()) {
                Text(
                    text = review.dateText,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (review.helpfulCount != null && review.helpfulCount > 0) {
                Text(
                    text = "👍 ${review.helpfulCount}",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
