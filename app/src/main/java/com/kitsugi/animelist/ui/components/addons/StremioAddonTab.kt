package com.kitsugi.animelist.ui.components.addons

import android.content.Context
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.components.KitsugiDropdownMenu
import com.kitsugi.animelist.ui.components.KitsugiDropdownItem

@Composable
internal fun StremioTab(
    addons: List<ManagedAddonEntity>,
    initialDebridToken: String,
    accentColor: Color,
    onAddAddon: (String) -> Unit,
    onToggleAddon: (ManagedAddonEntity, Boolean) -> Unit,
    onDeleteAddon: (ManagedAddonEntity) -> Unit,
    onSaveDebridToken: (String) -> Unit
) {
    var newManifestUrl by rememberSaveable { mutableStateOf("") }
    var debridToken by rememberSaveable(initialDebridToken) { mutableStateOf(initialDebridToken) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RealDebrid
        item {
            Text("RealDebrid Ayarları", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = debridToken,
                onValueChange = { debridToken = it; onSaveDebridToken(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text("RealDebrid API Key (Token)") },
                placeholder = { Text("Tokenınızı girin...") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = KitsugiColors.TextPrimary,
                    unfocusedTextColor = KitsugiColors.TextPrimary,
                    focusedContainerColor = KitsugiColors.SurfaceSoft,
                    unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = KitsugiColors.Border,
                    cursorColor = accentColor
                )
            )
            Text(
                "Kitsugi akışlarında donma yaşamadan, yüksek hızlı torrent izlemek için debrid hesabınızı bağlayın.",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Oynatma Ayarları
        item {
            HorizontalDivider(color = KitsugiColors.Border)
            Spacer(Modifier.height(8.dp))
            Text("Oynatma Ayarları", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE) }
            var autoplayEnabled by remember { mutableStateOf(prefs.getBoolean("autoplay_enabled", false)) }
            var defaultPlayer by remember { mutableStateOf(prefs.getString("default_player_engine", "exoplayer") ?: "exoplayer") }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Otomatik Oynat (Auto-Play)", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("İlk önbellekli akışı otomatik başlatır.", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = autoplayEnabled,
                    onCheckedChange = { autoplayEnabled = it; prefs.edit().putBoolean("autoplay_enabled", it).apply() },
                    colors = SwitchDefaults.colors(checkedThumbColor = KitsugiColors.Surface, checkedTrackColor = accentColor)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Varsayılan Oynatıcı Motoru", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            var dropdownExpanded by remember { mutableStateOf(false) }
            val playerOptions = listOf("exoplayer" to "Dahili Oynatıcı (ExoPlayer)", "mpv" to "Harici Oynatıcı (MPV)", "ask" to "Her Zaman Sor")
            val currentPlayerName = playerOptions.find { it.first == defaultPlayer }?.second ?: "Dahili Oynatıcı (ExoPlayer)"
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.SurfaceSoft)
                        .tvClickable(shape = RoundedCornerShape(18.dp)) { dropdownExpanded = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = currentPlayerName, color = KitsugiColors.TextPrimary)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Seç", tint = KitsugiColors.TextSecondary)
                }
                KitsugiDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    playerOptions.forEach { option ->
                        KitsugiDropdownItem(
                            text = option.second,
                            selected = option.first == defaultPlayer,
                            onClick = { defaultPlayer = option.first; prefs.edit().putString("default_player_engine", option.first).apply(); dropdownExpanded = false }
                        )
                    }
                }
            }
        }

        // Popüler Eklentiler
        item {
            HorizontalDivider(color = KitsugiColors.Border)
            Spacer(Modifier.height(8.dp))
            Text("Popüler Akış Eklentileri", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tek tıkla hızlıca kurabilirsiniz.", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                val torrentioUrl = if (debridToken.isNotBlank()) {
                    "https://torrentio.strem.fun/realdebrid=$debridToken/providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentproject,torrent9,yts,horriblesubs/manifest.json"
                } else {
                    "https://torrentio.strem.fun/manifest.json"
                }
                PresetAddonRow("Torrentio (Tavsiye Edilen)", "Ana torrent kaynağı. RealDebrid token girildiyse otomatik bağlanır.", torrentioUrl, addons.any { it.manifestUrl.contains("torrentio") }, accentColor, KitsugiColors.AccentBlue, Icons.Rounded.Download, onAddAddon)
                PresetAddonRow("Anime Kitsu Kataloğu", "Kitsu üzerinden anime listelerini ve verileri yükler.", "https://anime-kitsu.strem.fun/manifest.json", addons.any { it.manifestUrl.contains("kitsu") }, accentColor, KitsugiColors.AccentPurple, Icons.Rounded.AutoAwesome, onAddAddon)
                PresetAddonRow("Official Cinemeta", "Film ve dizi meta verilerini çeken resmi katalog eklentisi.", "https://v3-cinemeta.strem.io/manifest.json", addons.any { it.manifestUrl.contains("cinemeta") }, accentColor, KitsugiColors.AccentGreen, Icons.Rounded.Movie, onAddAddon)
                PresetAddonRow("YouTube Trailers", "Fragmanları ve YouTube videolarını oynatan resmi eklenti.", "https://youtube.strem.io/manifest.json", addons.any { it.manifestUrl.contains("youtube.strem.io") }, accentColor, KitsugiColors.AccentOrange, Icons.Rounded.PlayCircle, onAddAddon)
            }
        }

        // Manuel Ekle
        item {
            HorizontalDivider(color = KitsugiColors.Border)
            Spacer(Modifier.height(8.dp))
            Text("Özel Akış Eklentisi Ekle", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = newManifestUrl,
                    onValueChange = { newManifestUrl = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Manifest URL (JSON)") },
                    placeholder = { Text("https://...") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary,
                        focusedContainerColor = KitsugiColors.SurfaceSoft, unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                        focusedIndicatorColor = accentColor, unfocusedIndicatorColor = KitsugiColors.Border, cursorColor = accentColor
                    )
                )
                Button(
                    onClick = { if (newManifestUrl.isNotBlank()) { onAddAddon(newManifestUrl.trim()); newManifestUrl = "" } },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(16.dp)
                ) { Icon(Icons.Rounded.Add, contentDescription = "Ekle", tint = KitsugiColors.Surface) }
            }
        }

        // Kurulu eklentiler
        item {
            HorizontalDivider(color = KitsugiColors.Border)
            Spacer(Modifier.height(8.dp))
            Text("Yüklü Akış Eklentileri (${addons.size})", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (addons.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Henüz akış eklentisi yüklenmemiş.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(addons) { addon ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.SurfaceSoft).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Extension, contentDescription = null, tint = accentColor)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(addon.name, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(addon.description ?: "Açıklama yok", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = addon.isEnabled,
                        onCheckedChange = { onToggleAddon(addon, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = KitsugiColors.Surface, checkedTrackColor = accentColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onDeleteAddon(addon) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = KitsugiColors.AccentRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetAddonRow(
    title: String,
    subtitle: String,
    manifestUrl: String,
    isInstalled: Boolean,
    accentColor: Color,
    iconColor: Color,
    icon: ImageVector,
    onAddAddon: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.SurfaceSoft)
            .tvClickable(enabled = !isInstalled, shape = RoundedCornerShape(16.dp)) { onAddAddon(manifestUrl) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = if (isInstalled) "Kuruldu" else "Kur",
            color = if (isInstalled) KitsugiColors.TextMuted else accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
