package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors

private val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60)

/**
 * SleepTimerPanel — yan panel olarak açılır, kullanıcı zamanlayıcı süresi seçebilir
 * veya aktif bir zamanlayıcıyı iptal edebilir.
 */
@Composable
fun SleepTimerPanel(
    visible: Boolean,
    secondsLeft: Int,
    onStartTimer: (minutes: Int) -> Unit,
    onStopTimer: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(280),
            initialOffsetX = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutHorizontally(
            animationSpec = tween(220),
            targetOffsetX = { it }
        ) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(
                    KitsugiColors.Surface.copy(alpha = 0.97f),
                    RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = null,
                            tint = KitsugiColors.Accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Uyku Zamanlayıcısı",
                            color = KitsugiColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = KitsugiColors.Border)

                // Active timer display
                if (secondsLeft > 0) {
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                KitsugiColors.Accent.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 20.dp)
                    ) {
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            color = KitsugiColors.Accent,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kalan süre",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onStopTimer,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF6B6B)
                            ),
                             border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFFF6B6B).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("İptal Et", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = KitsugiColors.Border)
                }

                // Duration presets
                Text(
                    text = if (secondsLeft > 0) "Süreyi Değiştir" else "Süre Seç",
                    color = KitsugiColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_MINUTES.forEach { minutes ->
                        val isActive = secondsLeft > 0 && secondsLeft in ((minutes * 60) - 30)..(minutes * 60)
                        Surface(
                            onClick = { onStartTimer(minutes) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) KitsugiColors.Accent.copy(alpha = 0.2f)
                                    else KitsugiColors.SurfaceStrong,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "$minutes dakika",
                                    color = if (isActive) KitsugiColors.Accent
                                            else KitsugiColors.TextPrimary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = KitsugiColors.Accent,
                                        modifier = Modifier.size(16.dp)
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
