package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun AudioSelectionOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    trackOptions: List<TrackOption>,
    onSelectTrack: (TrackOption) -> Unit,
    audioDelayMs: Long,
    onAudioDelayChange: (Long) -> Unit,
    audioBoostLevel: Float, // 0.0f to 1.0f (representing boost/amplification)
    onAudioBoostChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Seçim, 1: Ses Ayarları

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
                    Text(
                        text = "Ses Ayarları",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Tabs — glass style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { activeTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) Color.White.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Seçim", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { activeTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) Color.White.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Zamanlama & Güçlendirme", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0) {
                        if (trackOptions.size <= 1) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Alternatif ses kanalı bulunamadı.", color = KitsugiColors.TextMuted, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(trackOptions) { opt ->
                                    SubOptionRow(
                                        label = opt.label,
                                        isSelected = opt.isSelected,
                                        onClick = { onSelectTrack(opt) }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Audio Delay (Sync Offset)
                            Column {
                                Text("Ses Senkronizasyonu", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { onAudioDelayChange(audioDelayMs - 100) },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Remove, "-100ms", tint = Color.White)
                                    }
                                    Text(
                                        text = "${audioDelayMs} ms",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { onAudioDelayChange(audioDelayMs + 100) },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Add, "+100ms", tint = Color.White)
                                    }
                                }
                                TextButton(
                                    onClick = { onAudioDelayChange(0) },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Gecikmeyi Sıfırla", color = KitsugiColors.AccentBlue, fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = KitsugiColors.Border)

                            // Audio Amplification (Software volume boost)
                            Column {
                                val boostPct = (audioBoostLevel * 100).toInt()
                                Text("Ses Güçlendirme (Boost) (+$boostPct%)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Slider(
                                    value = audioBoostLevel,
                                    onValueChange = onAudioBoostChange,
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = KitsugiColors.AccentBlue
                                    )
                                )
                                Text(
                                    text = "Düşük sesli kaynaklarda yazılımsal ses artışı sağlar.",
                                    color = KitsugiColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))))
                        .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
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
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Seçili",
                tint = KitsugiColors.AccentBlue,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
