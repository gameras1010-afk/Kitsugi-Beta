package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiErrorState
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

/** Shows a pulsing spinner while the IMDb ID is being resolved. */
@Composable
fun ResolvingIdState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = LocalKitsugiAccent.current, strokeWidth = 3.dp)
        Text("IMDb ID çözümleniyor...", color = KitsugiColors.TextSecondary)
    }
}

/** Shows a generic error icon + message with an optional retry button. */
@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?) {
    KitsugiErrorState(
        message = message,
        modifier = Modifier.padding(top = 40.dp),
        onRetryClick = onRetry
    )
}

/** Prompt shown when no compatible addons are installed. */
@Composable
fun NoAddonsState(onOpenSettings: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        KitsugiEmptyState(
            title = "Uyumlu eklenti bulunamadı",
            subtitle = "Ayarlar > Eklentiler bölümünden Torrentio, Dizilla veya başka bir eklenti ekleyin.",
            icon = Icons.Rounded.Extension,
            modifier = Modifier.padding(top = 30.dp)
        )
        if (onOpenSettings != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKitsugiAccent.current,
                    contentColor = KitsugiColors.Background
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Eklentileri Yönet (Ayarlar)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Prompt shown when all addons finished loading but returned no streams. */
@Composable
fun EmptyStreamsState(onOpenSettings: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        KitsugiEmptyState(
            title = "Akış bulunamadı",
            subtitle = "Tüm eklentiler denendi. Farklı bir bölüm veya daha fazla eklenti eklemeyi deneyin.",
            icon = Icons.Rounded.SearchOff,
            modifier = Modifier.padding(top = 30.dp)
        )
        if (onOpenSettings != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKitsugiAccent.current,
                    contentColor = KitsugiColors.Background
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Daha Fazla Eklenti Ekle", fontWeight = FontWeight.Bold)
            }
        }
    }
}
