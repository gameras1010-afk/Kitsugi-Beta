package com.kitsugi.animelist.ui.tv.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

@Composable
fun TvReviewDetailDialog(
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

    var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }

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
                            html = displayText,
                            onImageClick = { urls, index ->
                                activeGalleryImages = urls
                                activeGalleryIndex = index
                            }
                        )
                    }
                }
            }
        }
        if (activeGalleryImages.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                imageUrls = activeGalleryImages,
                initialIndex = activeGalleryIndex,
                title = review.username,
                onDismiss = { activeGalleryImages = emptyList() }
            )
        }
    }
}
