package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

// ─────────────────────────────────────────────────────────────
//  PlayerPanels.kt
//  Houses side panels for Episodes list, Stream Info,
//  and Skip Settings — each extracted from the monolithic
//  KitsugiFullscreenPlayerScreen composable.
// ─────────────────────────────────────────────────────────────

/**
 * PlayerPanelsHost — aktif panele göre ilgili side panel'i gösterir.
 */
@Composable
fun PlayerPanelsHost(
    activePanel: PlayerPanel,
    // Episodes
    episodesList: List<KitsugiStreamingEpisode>,
    currentEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    // Sources
    streamSources: List<StreamSource>,
    currentSourceIndex: Int,
    onSourceSelected: (Int, StreamSource) -> Unit,
    // Stream Info
    streamInfo: StreamInfoData?,
    // Skip Settings
    aniSkipEnabled: Boolean,
    aniSkipAutoSkip: Boolean,
    animeSkipClientId: String,
    onSkipSettingsUpdate: (enabled: Boolean, autoSkip: Boolean, clientId: String) -> Unit,
    // Dismiss
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        EpisodesSidePanelWrapper(
            visible = activePanel == PlayerPanel.EPISODES,
            episodesList = episodesList,
            currentEpisode = currentEpisode,
            onEpisodeSelected = { onEpisodeSelected(it); onDismiss() },
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        SourcesSidePanelWrapper(
            visible = activePanel == PlayerPanel.SOURCES,
            sources = streamSources,
            currentIndex = currentSourceIndex,
            onSourceSelected = { idx, src -> onSourceSelected(idx, src); onDismiss() },
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        StreamInfoPanelWrapper(
            visible = activePanel == PlayerPanel.STREAM_INFO,
            info = streamInfo,
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        SkipSettingsPanelWrapper(
            visible = activePanel == PlayerPanel.SKIP_SETTINGS,
            aniSkipEnabled = aniSkipEnabled,
            aniSkipAutoSkip = aniSkipAutoSkip,
            animeSkipClientId = animeSkipClientId,
            onUpdate = onSkipSettingsUpdate,
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  EpisodesSidePanelWrapper
// ─────────────────────────────────────────────────────────────

@Composable
private fun EpisodesSidePanelWrapper(
    visible: Boolean,
    episodesList: List<KitsugiStreamingEpisode>,
    currentEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF080814).copy(alpha = 0.92f), Color(0xFF0D0D20).copy(alpha = 0.82f))
                    )
                )
                .leftBorder(1.dp, Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.VideoLibrary, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(20.dp))
                        Text("Bölümler", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (episodesList.isEmpty()) {
                    Text("Bölüm bulunamadı.", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(episodesList) { ep ->
                            val isActive = ep.episodeNumber == currentEpisode
                            EpisodeRowItem(
                                episode = ep,
                                isActive = isActive,
                                onClick = { ep.episodeNumber?.let { onEpisodeSelected(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: KitsugiStreamingEpisode,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier
                    .background(Brush.linearGradient(listOf(KitsugiColors.AccentBlue.copy(alpha = 0.22f), KitsugiColors.AccentBlue.copy(alpha = 0.06f))))
                    .border(0.8.dp, KitsugiColors.AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier.background(Color.White.copy(alpha = 0.04f))
            )
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bölüm ${episode.episodeNumber}",
                color = if (isActive) KitsugiColors.AccentBlue else Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
            if (!episode.title.isNullOrBlank()) {
                Text(
                    text = episode.title,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        if (isActive) {
            Icon(Icons.Rounded.PlayArrow, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  SourcesSidePanelWrapper
// ─────────────────────────────────────────────────────────────

@Composable
private fun SourcesSidePanelWrapper(
    visible: Boolean,
    sources: List<StreamSource>,
    currentIndex: Int,
    onSourceSelected: (Int, StreamSource) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF080814).copy(alpha = 0.92f), Color(0xFF0D0D20).copy(alpha = 0.82f))
                    )
                )
                .leftBorder(1.dp, Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Source, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(20.dp))
                        Text("Kaynaklar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sources.size) { idx ->
                        val src = sources[idx]
                        val isSelected = idx == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isSelected) Modifier
                                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                    else Modifier.background(Color.White.copy(alpha = 0.04f))
                                )
                                .tvClickable(shape = RoundedCornerShape(10.dp)) { onSourceSelected(idx, src) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = src.addonName ?: "Kaynak ${idx + 1}",
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                src.title?.let {
                                    Text(it, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  StreamInfoPanelWrapper
// ─────────────────────────────────────────────────────────────

@Composable
private fun StreamInfoPanelWrapper(
    visible: Boolean,
    info: StreamInfoData?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color(0xFF080814).copy(alpha = 0.92f), Color(0xFF0D0D20).copy(alpha = 0.82f))))
                .leftBorder(1.dp, Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(20.dp))
                        Text("Akış Bilgisi", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (info == null) {
                        Text("Bilgi yükleniyor...", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                    } else {
                        InfoRow("Eklenti", info.addonName)
                        InfoRow("Oynatıcı", info.playerEngine)
                        InfoRow("Video", info.videoWidth?.let { "${it}x${info.videoHeight}" })
                        InfoRow("Video Kodek", info.videoCodec)
                        InfoRow("Ses Kodek", info.audioCodec)
                        InfoRow("FPS", info.videoFrameRate?.let { "%.2f".format(it) })
                        InfoRow("Dosya", info.filename)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────
//  SkipSettingsPanelWrapper
// ─────────────────────────────────────────────────────────────

@Composable
private fun SkipSettingsPanelWrapper(
    visible: Boolean,
    aniSkipEnabled: Boolean,
    aniSkipAutoSkip: Boolean,
    animeSkipClientId: String,
    onUpdate: (enabled: Boolean, autoSkip: Boolean, clientId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by remember(aniSkipEnabled) { mutableStateOf(aniSkipEnabled) }
    var autoSkip by remember(aniSkipAutoSkip) { mutableStateOf(aniSkipAutoSkip) }
    var clientId by remember(animeSkipClientId) { mutableStateOf(animeSkipClientId) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color(0xFF080814).copy(alpha = 0.92f), Color(0xFF0D0D20).copy(alpha = 0.82f))))
                .leftBorder(1.dp, Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(20.dp))
                        Text("Atlama Ayarları", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AniSkip Etkin", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it; onUpdate(it, autoSkip, clientId) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KitsugiColors.AccentBlue)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Otomatik Atla", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = autoSkip,
                            onCheckedChange = { autoSkip = it; onUpdate(enabled, it, clientId) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KitsugiColors.AccentBlue)
                        )
                    }
                    HorizontalDivider(color = KitsugiColors.Border)
                    Text("AnimeSkip Client ID", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it; onUpdate(enabled, autoSkip, it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        placeholder = { Text("Client ID girin...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KitsugiColors.AccentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true
                    )
                }
            }
        }
    }
}
