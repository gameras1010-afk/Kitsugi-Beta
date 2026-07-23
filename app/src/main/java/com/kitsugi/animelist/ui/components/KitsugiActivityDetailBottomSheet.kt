package com.kitsugi.animelist.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiActivityDetailBottomSheet(
    activityId: Int,
    apiClient: JikanApiClient,
    titleLanguage: String = "ROMAJI",
    blurAdultMedia: Boolean = false,
    onUserProfileClick: ((userId: Int?, username: String, avatarUrl: String?) -> Unit)? = null,
    onMediaClick: ((mediaId: Int, mediaType: com.kitsugi.animelist.model.MediaType, source: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val translationManager = remember { TranslationManager(context) }

    var activityDetails by remember { mutableStateOf<KitsugiActivity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showReplyEditor by remember { mutableStateOf(false) }

    // Translation states
    var selectedLanguage by remember { mutableStateOf("original") }
    var translatedText by remember { mutableStateOf<String?>(null) }
    val translatedReplies = remember { mutableStateMapOf<Int, String>() }



    var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }

    // Load activity details & replies
    LaunchedEffect(activityId) {
        isLoading = true
        activityDetails = apiClient.fetchActivityReplies(activityId)
        isLoading = false
    }

    // Translation orchestration
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
                    text = "Aktivite",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

                if (activityDetails != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { context.openTranslator(activityDetails!!.text) },
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
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("activity_text", activityDetails!!.text))
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (activityDetails == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aktivite detayları yüklenemedi.", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                }
            } else {
                val act = activityDetails!!
                
                // Content area with unified LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: User Header & Text Content Card
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(KitsugiColors.SurfaceSoft)
                                        .then(
                                            if (onUserProfileClick != null) {
                                                Modifier.clickable { onUserProfileClick(act.userId, act.username, act.avatarUrl) }
                                            } else Modifier
                                        )
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
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = act.username,
                                        color = KitsugiColors.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = if (onUserProfileClick != null) {
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onUserProfileClick(act.userId, act.username, act.avatarUrl) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        } else Modifier
                                    )
                                    if (!act.dateText.isNullOrBlank()) {
                                        Text(
                                            text = act.dateText,
                                            color = KitsugiColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val isMediaClickable = act.mediaId != null && onMediaClick != null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KitsugiColors.SurfaceStrong)
                                    .then(
                                        if (isMediaClickable) {
                                            Modifier.clickable {
                                                val mType = when (act.mediaType?.uppercase()) {
                                                    "MANGA" -> com.kitsugi.animelist.model.MediaType.Manga
                                                    "MOVIE" -> com.kitsugi.animelist.model.MediaType.Movie
                                                    "TV", "TV_SHOW" -> com.kitsugi.animelist.model.MediaType.TvShow
                                                    else -> com.kitsugi.animelist.model.MediaType.Anime
                                                }
                                                onMediaClick?.invoke(act.mediaId!!, mType, "anilist")
                                                onDismiss()
                                            }
                                        } else Modifier
                                    )
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    // Resolve localized title for ListActivity entries
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
                                    val localizedDisplayText = if (act.mediaTitle != null && localizedTitle != null && localizedTitle != act.mediaTitle) {
                                        act.text.replace("**${act.mediaTitle}**", "**$localizedTitle**")
                                    } else act.text
                                    val displayText = if (selectedLanguage == "turkish") translatedText ?: localizedDisplayText else localizedDisplayText
                                    KitsugiMarkdownText(
                                        text = displayText,
                                        onImageGalleryRequest = { urls, index ->
                                            activeGalleryImages = urls
                                            activeGalleryIndex = index
                                        }
                                    )
                                }
                                if (!act.mediaCoverUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AsyncImage(
                                        model = act.mediaCoverUrl,
                                        contentDescription = act.mediaTitle,
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .then(if (blurAdultMedia && act.isAdult) Modifier.blur(24.dp) else Modifier),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Item 2: Replies Header
                    item {
                        Text(
                            text = "Yanıtlar (${act.replies.size})",
                            color = KitsugiColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    // Item 3: Replies Empty State or List
                    if (act.replies.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Henüz yanıt yazılmamış.", color = KitsugiColors.TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(act.replies, key = { reply -> reply.id }) { reply ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(KitsugiColors.SurfaceStrong, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(KitsugiColors.SurfaceSoft)
                                            .then(
                                                if (onUserProfileClick != null) {
                                                    Modifier.clickable { onUserProfileClick(reply.userId, reply.username, reply.avatarUrl) }
                                                } else Modifier
                                            )
                                    ) {
                                        if (!reply.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = reply.avatarUrl,
                                                contentDescription = reply.username,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = reply.username.take(1).uppercase(),
                                                    color = KitsugiColors.TextMuted,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = reply.username,
                                        color = KitsugiColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = if (onUserProfileClick != null) {
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onUserProfileClick(reply.userId, reply.username, reply.avatarUrl) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        } else Modifier
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (!reply.dateText.isNullOrBlank()) {
                                        Text(
                                            text = reply.dateText,
                                            color = KitsugiColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val displayReplyText = if (selectedLanguage == "turkish") translatedReplies[reply.id] ?: reply.text else reply.text
                                KitsugiMarkdownText(
                                    text = displayReplyText,
                                    onImageGalleryRequest = { urls, index ->
                                        activeGalleryImages = urls
                                        activeGalleryIndex = index
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

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
                                            onClick = { context.openTranslator(reply.text) },
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
                                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("reply_text", reply.text))
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

                                    var repLikedState by remember { mutableStateOf(reply.isLiked) }
                                    var repLikesState by remember { mutableStateOf(reply.likeCount) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.tvClickable(shape = RoundedCornerShape(8.dp)) {
                                            coroutineScope.launch {
                                                val success = apiClient.toggleLike(reply.id, "ACTIVITY_REPLY")
                                                if (success) {
                                                    repLikedState = !repLikedState
                                                    repLikesState = if (repLikedState) repLikesState + 1 else repLikesState - 1
                                                } else {
                                                    Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (repLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Beğen",
                                            tint = if (repLikedState) accentColor else KitsugiColors.TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = repLikesState.toString(),
                                            color = KitsugiColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Bar Actions (Like and Reply)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isActLikedState by remember { mutableStateOf(act.isLiked) }
                    var actLikesState by remember { mutableStateOf(act.likeCount) }

                    // Activity Like
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = apiClient.toggleLike(act.id, "ACTIVITY")
                                if (success) {
                                    isActLikedState = !isActLikedState
                                    actLikesState = if (isActLikedState) actLikesState + 1 else actLikesState - 1
                                } else {
                                    Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActLikedState) accentColor.copy(alpha = 0.2f) else KitsugiColors.SurfaceStrong
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Icon(
                            imageVector = if (isActLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (isActLikedState) accentColor else KitsugiColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = actLikesState.toString(),
                            color = if (isActLikedState) accentColor else KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Activity Reply
                    Button(
                        onClick = { showReplyEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.6f)
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
    }

    if (showReplyEditor && activityDetails != null) {
        KitsugiReplyEditorBottomSheet(
            title = "Aktiviteyi Yanıtla",
            placeholder = "Mesajınızı buraya yazın...",
            onPublish = { text ->
                apiClient.postReply(activityId, isActivity = true, text = text)
            },
            onDismiss = {
                showReplyEditor = false
                // Reload activity and replies after posting
                coroutineScope.launch {
                    activityDetails = apiClient.fetchActivityReplies(activityId)
                }
            }
        )
    }

    if (activeGalleryImages.isNotEmpty()) {
        KitsugiImageGalleryDialog(
            imageUrls = activeGalleryImages,
            initialIndex = activeGalleryIndex,
            title = activityDetails?.username ?: "Aktivite",
            onDismiss = { activeGalleryImages = emptyList() }
        )
    }
}
