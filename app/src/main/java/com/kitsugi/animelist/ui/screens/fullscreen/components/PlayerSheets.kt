package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

// ─────────────────────────────────────────────────────────────
//  PlayerSheets.kt
//  Houses all slide-in overlay panels for the player.
//  Each composable follows AnimatedVisibility + slide-in pattern
//  consistent with SubtitleSelectionOverlay.
// ─────────────────────────────────────────────────────────────

/**
 * PlayerSheetsHost — bir arada tüm overlay panellerini render eden sarmalayıcı.
 * Hangi panel aktifse onu gösterir.
 */
@Composable
fun PlayerSheetsHost(
    activePanel: PlayerPanel,
    // Subtitle
    subtitleTracks: List<TrackOption>,
    isSubtitleDisabled: Boolean,
    subtitleStyle: SubtitleStyleSettings,
    subtitleDelayMs: Long,
    onSubtitleTrackSelected: (TrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSubtitleStyleChange: (SubtitleStyleSettings) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    // Audio
    audioTracks: List<TrackOption>,
    selectedAudioTrack: TrackOption?,
    onAudioTrackSelected: (TrackOption) -> Unit,
    // Speed
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    // Sleep Timer
    sleepTimerSecondsLeft: Int,
    onStartSleepTimer: (minutes: Int) -> Unit,
    onStopSleepTimer: () -> Unit,
    // Dismiss
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        SubtitleSelectionOverlay(
            visible = activePanel == PlayerPanel.SUBTITLES,
            onClose = onDismiss,
            trackOptions = subtitleTracks,
            isSubtitleDisabled = isSubtitleDisabled,
            onDisableSubtitles = onDisableSubtitles,
            onSelectTrack = onSubtitleTrackSelected,
            styleSettings = subtitleStyle,
            onStyleChange = onSubtitleStyleChange,
            subtitleDelayMs = subtitleDelayMs,
            onSubtitleDelayChange = onSubtitleDelayChange,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        AudioSelectionOverlay(
            visible = activePanel == PlayerPanel.AUDIO,
            onClose = onDismiss,
            tracks = audioTracks,
            selectedTrack = selectedAudioTrack,
            onSelectTrack = onAudioTrackSelected,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        SpeedSelectionOverlay(
            visible = activePanel == PlayerPanel.SPEED,
            currentSpeed = currentSpeed,
            onSpeedChange = { onSpeedChange(it); onDismiss() },
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        SleepTimerOverlay(
            visible = activePanel == PlayerPanel.SLEEP_TIMER,
            secondsLeft = sleepTimerSecondsLeft,
            onStart = { mins -> onStartSleepTimer(mins); onDismiss() },
            onStop = { onStopSleepTimer(); onDismiss() },
            onClose = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  AudioSelectionOverlay
// ─────────────────────────────────────────────────────────────

@Composable
fun AudioSelectionOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    tracks: List<TrackOption>,
    selectedTrack: TrackOption?,
    onSelectTrack: (TrackOption) -> Unit,
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
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF080814).copy(alpha = 0.88f),
                            Color(0xFF0D0D20).copy(alpha = 0.78f)
                        )
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
                    Text("Ses Parçası", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (tracks.isEmpty()) {
                    Text(
                        "Ses parçası bulunamadı.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tracks) { track ->
                            val isSelected = track.label == selectedTrack?.label
                            AudioTrackRow(
                                label = track.label,
                                isSelected = isSelected,
                                onClick = { onSelectTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) Modifier
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))))
                    .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Rounded.Check, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  SpeedSelectionOverlay
// ─────────────────────────────────────────────────────────────

private val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
fun SpeedSelectionOverlay(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(280.dp)
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
                    Text("Oynatma Hızı", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(speedOptions) { speed ->
                        val isSelected = currentSpeed == speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isSelected) Modifier
                                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .tvClickable(shape = RoundedCornerShape(10.dp)) { onSpeedChange(speed) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x",
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
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
//  SleepTimerOverlay — Uyku Zamanlayıcı
// ─────────────────────────────────────────────────────────────

private val sleepTimerOptions = listOf(
    "Kapalı" to 0,
    "10 dakika" to 10,
    "20 dakika" to 20,
    "30 dakika" to 30,
    "45 dakika" to 45,
    "60 dakika" to 60,
    "90 dakika" to 90
)

@Composable
fun SleepTimerOverlay(
    visible: Boolean,
    secondsLeft: Int,
    onStart: (minutes: Int) -> Unit,
    onStop: () -> Unit,
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
                        Icon(Icons.Rounded.Bedtime, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(22.dp))
                        Text("Uyku Zamanlayıcı", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }

                // Active timer display
                if (secondsLeft > 0) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.AccentBlue.copy(alpha = 0.15f))
                            .border(1.dp, KitsugiColors.AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val minutes = secondsLeft / 60
                            val seconds = secondsLeft % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("kalan süre", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Stop, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zamanlayıcıyı Durdur", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = KitsugiColors.Border)
                }

                Spacer(Modifier.height(if (secondsLeft > 0) 4.dp else 16.dp))
                Text("Süre Seç", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sleepTimerOptions) { (label, minutes) ->
                        val isActive = secondsLeft > 0 && minutes > 0 && (secondsLeft / 60) == minutes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isActive) Modifier
                                        .background(Brush.linearGradient(listOf(KitsugiColors.AccentBlue.copy(alpha = 0.2f), KitsugiColors.AccentBlue.copy(alpha = 0.05f))))
                                        .border(0.8.dp, KitsugiColors.AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                    if (minutes == 0) onStop() else onStart(minutes)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (minutes == 0) Icons.Rounded.BedtimeOff else Icons.Rounded.Bedtime,
                                    contentDescription = null,
                                    tint = if (isActive) KitsugiColors.AccentBlue else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                            if (isActive) {
                                Icon(Icons.Rounded.Check, null, tint = KitsugiColors.AccentBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
