package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun KitsugiConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "İptal",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current

    if (isTvDevice) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = KitsugiColors.Surface
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KitsugiColors.Border),
                    modifier = androidx.compose.ui.Modifier
                        .width(420.dp)
                        .padding(24.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = message,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss
                            ) {
                                Text(
                                    text = dismissText,
                                    color = KitsugiColors.TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            androidx.compose.foundation.layout.Spacer(
                                modifier = androidx.compose.ui.Modifier.width(12.dp)
                            )

                            TextButton(
                                onClick = onConfirm
                            ) {
                                Text(
                                    text = confirmText,
                                    color = if (isDestructive) {
                                        KitsugiColors.AccentRed
                                    } else {
                                        accentColor
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            textContentColor = KitsugiColors.TextSecondary,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text(
                        text = confirmText,
                        color = if (isDestructive) {
                            KitsugiColors.AccentRed
                        } else {
                            accentColor
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = dismissText,
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}