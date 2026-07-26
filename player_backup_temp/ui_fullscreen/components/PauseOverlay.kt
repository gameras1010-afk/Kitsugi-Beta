package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PauseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    title: String,
    posterUrl: String?,
    episodeTitle: String?,
    season: Int?,
    episode: Int?,
    year: String?,
    description: String?,
    cast: List<MetaCastMember>,
    modifier: Modifier = Modifier
) {
    var selectedCastMember by remember { mutableStateOf<MetaCastMember?>(null) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                }
        ) {
            // Content container
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 64.dp, end = 24.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left metadata section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .tvClickable(enabled = false) {} // block click propagation
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "Şu Anda İzliyorsunuz",
                        color = KitsugiColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val infoText = buildString {
                        if (year != null) append(year)
                        if (season != null && episode != null) {
                            if (isNotEmpty()) append(" • ")
                            append("Sezon $season, Bölüm $episode")
                        }
                    }
                    if (infoText.isNotEmpty()) {
                        Text(
                            text = infoText,
                            color = KitsugiColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (!episodeTitle.isNullOrBlank()) {
                        Text(
                            text = episodeTitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            color = KitsugiColors.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Oyuncular",
                            color = KitsugiColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cast.take(8)) { member ->
                                CastChip(member = member, onClick = { selectedCastMember = member })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right Poster or clock section
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PauseOverlayClock()

                    if (!posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = title,
                            modifier = Modifier
                                .height(180.dp)
                                .width(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Cast Member Detail Dialog
            if (selectedCastMember != null) {
                val accentColor = LocalKitsugiAccent.current
                AlertDialog(
                    onDismissRequest = { selectedCastMember = null },
                    containerColor = KitsugiColors.Surface,
                    titleContentColor = KitsugiColors.TextPrimary,
                    textContentColor = KitsugiColors.TextSecondary,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = selectedCastMember!!.name,
                            color = KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!selectedCastMember!!.photo.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedCastMember!!.photo,
                                    contentDescription = selectedCastMember!!.name,
                                    modifier = Modifier
                                        .size(100.dp, 140.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column {
                                if (!selectedCastMember!!.character.isNullOrBlank()) {
                                    Text(
                                        text = "Karakter:",
                                        color = KitsugiColors.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = selectedCastMember!!.character!!,
                                        color = KitsugiColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedCastMember = null }) {
                            Text("Kapat", color = accentColor)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CastChip(
    member: MetaCastMember,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(KitsugiColors.SurfaceSoft)
            .tvClickable(shape = RoundedCornerShape(8.dp), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!member.photo.isNullOrBlank()) {
            AsyncImage(
                model = member.photo,
                contentDescription = member.name,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = member.name,
            color = KitsugiColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PauseOverlayClock() {
    var nowStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            nowStr = sdf.format(Date())
            delay(1000)
        }
    }
    Text(
        text = nowStr,
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Light
    )
}
