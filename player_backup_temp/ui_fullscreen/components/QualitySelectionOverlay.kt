package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable

/**
 * Bir kalite değeriyle etiketlenmiş kaynak grubu.
 *
 * @param label "Otomatik", "4K (2160p)", "1080p", "720p", "480p", "Düşük Kalite" vb.
 * @param qualityValue Sıralama için sayısal değer (-1 = Otomatik/bilinmiyor)
 * @param sources Bu kalite grubuna ait akış kaynakları
 */
data class QualityGroup(
    val label: String,
    val qualityValue: Int,
    val sources: List<StreamSource>
)

/** Kaynak listesinden kalite grupları oluştur, en yüksekten en düşüğe sırala. */
fun buildQualityGroups(sources: List<StreamSource>): List<QualityGroup> {
    // Önce bilinen kalite değerine sahip kaynakları grupla
    val withQuality = sources
        .filter { (it.qualityValue ?: 0) > 0 }
        .groupBy { it.qualityValue!! }
        .map { (qv, srcs) ->
            QualityGroup(
                label = when {
                    qv >= 2160 -> "4K (2160p)"
                    qv >= 1440 -> "2K (1440p)"
                    qv >= 1080 -> "1080p (Full HD)"
                    qv >= 720  -> "720p (HD)"
                    qv >= 480  -> "480p (SD)"
                    qv >= 360  -> "360p"
                    else       -> "${qv}p"
                },
                qualityValue = qv,
                sources = srcs
            )
        }
        .sortedByDescending { it.qualityValue }

    // Kalite değeri olmayan kaynakları "Otomatik" grubuna koy
    val withoutQuality = sources.filter { (it.qualityValue ?: 0) <= 0 }
    val autoGroup = if (withoutQuality.isNotEmpty()) {
        listOf(
            QualityGroup(
                label = "Otomatik",
                qualityValue = -1,
                sources = withoutQuality
            )
        )
    } else emptyList()

    return withQuality + autoGroup
}

@Composable
fun QualitySelectionOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    sources: List<StreamSource>,
    currentSourceIndex: Int,
    onQualitySelected: (StreamSource, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val qualityGroups = remember(sources) { buildQualityGroups(sources) }

    // Aktif kalite grubunu hesapla
    val currentSource = sources.getOrNull(currentSourceIndex)
    val currentQualityValue = currentSource?.qualityValue ?: -1

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
                // ── Başlık ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Kalite Seçimi",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Kalite Grupları ──────────────────────────────────────────
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    qualityGroups.forEach { group ->
                        val isCurrentGroup = when {
                            group.qualityValue == -1 -> currentQualityValue <= 0
                            else -> group.qualityValue == currentQualityValue
                        }

                        // Grup başlığı
                        Text(
                            text = group.label,
                            color = if (isCurrentGroup) accentColor else Color.White.copy(alpha = 0.85f),
                            fontWeight = if (isCurrentGroup) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        // Gruptaki her kaynak
                        group.sources.forEachIndexed { _, source ->
                            val sourceIndex = sources.indexOf(source)
                            val isCurrentSource = sourceIndex == currentSourceIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCurrentSource) accentColor.copy(alpha = 0.18f)
                                        else if (isCurrentGroup) Color.White.copy(alpha = 0.05f)
                                        else Color.Transparent
                                    )
                                    .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                        onQualitySelected(source, sourceIndex)
                                        onClose()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val addonLabel = source.addonName
                                        .ifBlank { source.name }
                                        .take(32)
                                    Text(
                                        text = addonLabel,
                                        color = if (isCurrentSource) accentColor else Color.White,
                                        fontWeight = if (isCurrentSource) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    if (source.name.isNotBlank() && source.name != source.addonName) {
                                        Text(
                                            text = source.name.take(40),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isCurrentSource) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
