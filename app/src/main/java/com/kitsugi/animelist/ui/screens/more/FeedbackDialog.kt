package com.kitsugi.animelist.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, type: String, description: String) -> Unit
) {
    val KitsugiColors = LocalKitsugiColors.current
    val accentColor = LocalKitsugiAccent.current

    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackDescription by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableStateOf(0) }
    val feedbackTypes = listOf("Hata Bildirimi", "Özellik Önerisi", "Genel")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KitsugiColors.background.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = KitsugiColors.surfaceStrong
                ),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geri Bildirim Gönder",
                            color = KitsugiColors.textPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                tint = KitsugiColors.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Type Selector Label
                    Text(
                        text = "Geri Bildirim Türü",
                        color = KitsugiColors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        feedbackTypes.forEachIndexed { index, type ->
                            val isSelected = selectedTypeIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) accentColor else KitsugiColors.surface
                                    )
                                    .tvClickable(shape = RoundedCornerShape(20.dp), onClick = { selectedTypeIndex = index })
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) KitsugiColors.background else KitsugiColors.textPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title TextField
                    Text(
                        text = "Konu Başlığı",
                        color = KitsugiColors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = feedbackTitle,
                        onValueChange = { feedbackTitle = it },
                        placeholder = { Text("Kısaca konuyu özetleyin...", color = KitsugiColors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = KitsugiColors.surface,
                            focusedTextColor = KitsugiColors.textPrimary,
                            unfocusedTextColor = KitsugiColors.textPrimary,
                            focusedContainerColor = KitsugiColors.surface,
                            unfocusedContainerColor = KitsugiColors.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Description TextField
                    Text(
                        text = "Detaylar",
                        color = KitsugiColors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = feedbackDescription,
                        onValueChange = { feedbackDescription = it },
                        placeholder = { Text("Detayları ve varsa adımları buraya yazabilirsiniz...", color = KitsugiColors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = KitsugiColors.surface,
                            focusedTextColor = KitsugiColors.textPrimary,
                            unfocusedTextColor = KitsugiColors.textPrimary,
                            focusedContainerColor = KitsugiColors.surface,
                            unfocusedContainerColor = KitsugiColors.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Button(
                        onClick = {
                            if (feedbackTitle.isNotBlank() && feedbackDescription.isNotBlank()) {
                                onSubmit(feedbackTitle, feedbackTypes[selectedTypeIndex], feedbackDescription)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = accentColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = feedbackTitle.isNotBlank() && feedbackDescription.isNotBlank()
                    ) {
                        Text(
                            text = "Gönder",
                            color = KitsugiColors.background,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
