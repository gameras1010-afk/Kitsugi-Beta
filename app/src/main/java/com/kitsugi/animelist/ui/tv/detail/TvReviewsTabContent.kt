package com.kitsugi.animelist.ui.tv.detail

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiActivity
import com.kitsugi.animelist.data.remote.KitsugiForumReply
import com.kitsugi.animelist.data.remote.KitsugiForumTopic
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.screens.detail.DetailTabState
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

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
    val accentColor = LocalKitsugiAccent.current

    var forumTopics by remember { mutableStateOf<List<KitsugiForumTopic>>(emptyList()) }
    var activitiesList by remember { mutableStateOf<List<KitsugiActivity>>(emptyList()) }

    var activeReviewForDetail by remember { mutableStateOf<KitsugiReview?>(null) }
    var activeTopicForDetail by remember { mutableStateOf<KitsugiForumTopic?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }

    val isTmdbOrSimkl = source.equals("tmdb", ignoreCase = true) || source.equals("simkl", ignoreCase = true)

    // Forum ve Aktiviteleri fetch et
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
//  TV Cards (Forum Topic, Activity, Review)
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

        // Author info
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // Stats & Like
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
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // Body: Text + Media Thumbnail
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

                KitsugiMarkdownText(
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

        // Like & Date
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
        // Header
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

        // Summary
        Text(
            text = review.summary,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        // Date + helpful
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

// ---------------------------------------------------------------------------
//  TV Detail Dialogs (Review, Topic, Activity)
// ---------------------------------------------------------------------------

@Composable
private fun TvReviewDetailDialog(
    review: KitsugiReview,
    apiClient: JikanApiClient,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var selectedLanguage by remember { mutableStateOf("original") }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    var userRatingState by remember(review.id) { mutableStateOf(review.userRating) }
    var helpfulCountState by remember(review.id) { mutableStateOf(review.helpfulCount ?: 0) }

    val scrollState = rememberScrollState()
    var isScrollFocused by remember { mutableStateOf(false) }

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage == "turkish" && translatedText == null) {
            isTranslating = true
            val tr = translationManager.translateToTurkish(review.fullText)
            if (tr.isNotBlank()) {
                translatedText = tr
            }
            isTranslating = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(KitsugiColors.Background)
                .border(1.dp, KitsugiColors.SurfaceSoft, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Sol Panel: Metadata & Butonlar ---
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Yazar Bilgisi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(KitsugiColors.SurfaceSoft),
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = review.username,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (review.score != null) {
                                Text(
                                    text = "Skor: ${review.score}/10",
                                    color = accentColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = KitsugiColors.SurfaceSoft)

                    // Beğeni butonu (Helpful)
                    var isHelpfulFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHelpfulFocused) Color.White.copy(alpha = 0.1f) else KitsugiColors.Surface)
                            .border(
                                width = if (isHelpfulFocused) 1.5.dp else 0.dp,
                                color = if (isHelpfulFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isHelpfulFocused = it.isFocused }
                            .tvClickable(
                                enabled = review.id != null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                coroutineScope.launch {
                                    val newRating = if (userRatingState == "UP_VOTE") "NO_RATING" else "UP_VOTE"
                                    val success = apiClient.rateReview(review.id ?: 0, newRating)
                                    if (success) {
                                        val diff = if (newRating == "UP_VOTE") 1 else -1
                                        userRatingState = newRating
                                        helpfulCountState = (helpfulCountState + diff).coerceAtLeast(0)
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
                                imageVector = Icons.Rounded.ThumbUp,
                                contentDescription = "Faydalı",
                                tint = if (userRatingState == "UP_VOTE") accentColor else KitsugiColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (helpfulCountState > 0) "$helpfulCountState Faydalı" else "Faydalı Bul",
                                color = if (userRatingState == "UP_VOTE") accentColor else KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dil Seçici Butonlar
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

                    // Kapat Butonu
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

                // --- Sağ Panel: Scrollable İçerik ---
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KitsugiColors.Surface)
                        .border(
                            width = 1.dp,
                            color = if (isScrollFocused) Color.White else KitsugiColors.SurfaceSoft,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .onFocusChanged { isScrollFocused = it.isFocused }
                        .focusTarget()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    if (isTranslating) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else {
                        val displayText = if (selectedLanguage == "turkish") (translatedText ?: review.fullText) else review.fullText
                        com.kitsugi.animelist.ui.components.KitsugiHtmlWebView(
                            html = displayText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvTopicDetailDialog(
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
                // T3-05: printStackTrace → Log.e
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

@Composable
private fun TvActivityDetailDialog(
    activityId: Int,
    apiClient: JikanApiClient,
    titleLanguage: String = "ROMAJI",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var activityDetails by remember { mutableStateOf<KitsugiActivity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedLanguage by remember { mutableStateOf("original") }
    var translatedText by remember { mutableStateOf<String?>(null) }
    val translatedReplies = remember { mutableStateMapOf<Int, String>() }

    // Aktivite detaylarını yükle
    LaunchedEffect(activityId) {
        isLoading = true
        runCatching {
            activityDetails = apiClient.fetchActivityReplies(activityId)
        }
        isLoading = false
    }

    // Çeviri işlemi
    LaunchedEffect(selectedLanguage, activityDetails) {
        if (selectedLanguage == "turkish" && activityDetails != null) {
            val act = activityDetails!!
            if (act.mediaTitle == null && translatedText == null) {
                coroutineScope.launch {
                    val tr = translationManager.translateToTurkish(act.text)
                    if (tr.isNotBlank()) translatedText = tr
                }
            }
            act.replies.forEach { reply ->
                if (!translatedReplies.containsKey(reply.id)) {
                    coroutineScope.launch {
                        val tr = translationManager.translateToTurkish(reply.text)
                        if (tr.isNotBlank()) {
                            translatedReplies[reply.id] = tr
                        }
                    }
                }
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
                    if (activityDetails != null) {
                        val act = activityDetails!!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(KitsugiColors.SurfaceSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!act.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = act.avatarUrl,
                                        contentDescription = act.username,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = act.username.take(1).uppercase(),
                                        color = accentColor,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = act.username,
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!act.dateText.isNullOrBlank()) {
                                    Text(
                                        text = act.dateText,
                                        color = KitsugiColors.TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Divider(color = KitsugiColors.SurfaceSoft)

                        // Beğeni Butonu
                        var isActLikedState by remember(act.id) { mutableStateOf(act.isLiked) }
                        var actLikesState by remember(act.id) { mutableStateOf(act.likeCount) }
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
                                        val success = apiClient.toggleLike(act.id, "ACTIVITY")
                                        if (success) {
                                            isActLikedState = !isActLikedState
                                            actLikesState = if (isActLikedState) actLikesState + 1 else actLikesState - 1
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
                                    imageVector = if (isActLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Beğen",
                                    tint = if (isActLikedState) accentColor else KitsugiColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$actLikesState Beğeni",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Yükleniyor...",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.titleMedium
                        )
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

                // --- Sağ Panel: Aktivite Metni & Yanıtlar ---
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KitsugiColors.Surface)
                        .padding(12.dp)
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else if (activityDetails == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aktivite detayları yüklenemedi.",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        val act = activityDetails!!
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // 1. Aktivite Ana Kartı
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(KitsugiColors.Background, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        val localizedTitle = when (titleLanguage) {
                                            "ENGLISH" -> act.mediaTitleEnglish?.takeIf { it.isNotBlank() }
                                                ?: act.mediaTitleRomaji ?: act.mediaTitleNative ?: act.mediaTitle
                                            "NATIVE", "JAPANESE_STAFF" -> act.mediaTitleNative?.takeIf { it.isNotBlank() }
                                                ?: act.mediaTitleRomaji ?: act.mediaTitleEnglish ?: act.mediaTitle
                                            else -> act.mediaTitleRomaji ?: act.mediaTitleEnglish ?: act.mediaTitleNative ?: act.mediaTitle
                                        }
                                        val localizedDisplayText = if (act.mediaTitle != null && localizedTitle != null && localizedTitle != act.mediaTitle) {
                                            act.text.replace("**${act.mediaTitle}**", "**$localizedTitle**")
                                        } else act.text
                                        val displayText = if (selectedLanguage == "turkish") translatedText ?: localizedDisplayText else localizedDisplayText

                                        var actGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
                                        var actGalleryIndex by remember { mutableStateOf(0) }
                                        KitsugiMarkdownText(
                                            text = displayText,
                                            onImageGalleryRequest = { urls, index ->
                                                actGalleryImages = urls
                                                actGalleryIndex = index
                                            }
                                        )
                                        if (actGalleryImages.isNotEmpty()) {
                                            KitsugiImageGalleryDialog(
                                                imageUrls = actGalleryImages,
                                                initialIndex = actGalleryIndex,
                                                title = act.username,
                                                onDismiss = { actGalleryImages = emptyList() }
                                            )
                                        }
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
                            }

                            // 2. Yanıtlar Bölümü Başlığı
                            item {
                                Text(
                                    text = "Yanıtlar (${act.replies.size})",
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }

                            // 3. Yanıt Kartları
                            if (act.replies.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Henüz yanıt yazılmamış.", color = KitsugiColors.TextMuted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                items(act.replies, key = { reply -> reply.id }) { reply ->
                                    val displayReplyText = if (selectedLanguage == "turkish") {
                                        translatedReplies[reply.id] ?: reply.text
                                    } else {
                                        reply.text
                                    }

                                    TvActivityReplyCard(
                                        reply = reply,
                                        displayText = displayReplyText,
                                        apiClient = apiClient
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

@Composable
private fun TvActivityReplyCard(
    reply: com.kitsugi.animelist.data.remote.KitsugiActivityReply,
    displayText: String,
    apiClient: JikanApiClient
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var likedState by remember(reply.id) { mutableStateOf(reply.isLiked) }
    var likeCountState by remember(reply.id) { mutableStateOf(reply.likeCount) }

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
                    if (!reply.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = reply.avatarUrl,
                            contentDescription = reply.username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = reply.username.take(1).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = reply.username,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (!reply.dateText.isNullOrBlank()) {
                Text(
                    text = reply.dateText,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        var replyGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var replyGalleryIndex by remember { mutableStateOf(0) }
        KitsugiMarkdownText(
            text = displayText,
            onImageGalleryRequest = { urls, index ->
                replyGalleryImages = urls
                replyGalleryIndex = index
            }
        )
        if (replyGalleryImages.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                imageUrls = replyGalleryImages,
                initialIndex = replyGalleryIndex,
                title = reply.username,
                onDismiss = { replyGalleryImages = emptyList() }
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
                            val success = apiClient.toggleLike(reply.id, "ACTIVITY_REPLY")
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
