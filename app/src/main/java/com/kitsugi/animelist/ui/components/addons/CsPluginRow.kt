package com.kitsugi.animelist.ui.components.addons

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CsPluginRow(
    plugin: CsPlugin,
    installState: PluginInstallState,
    isInstalled: Boolean,
    installedVersion: Int?,
    installedPlugin: CsPluginEntity?,
    accentColor: Color,
    onInstallPlugin: (CsPlugin, ((Boolean) -> Unit)?) -> Unit,
    onStartInstall: () -> Unit,
    onInstallResult: (Boolean) -> Unit
) {
    val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            var isError by remember { mutableStateOf(false) }
            if (!plugin.iconUrl.isNullOrBlank() && !isError) {
                AsyncImage(
                    model = plugin.iconUrl.replace("%size%", "128").replace("%exact_size%", "128"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { isError = true }
                )
            } else {
                Text("⚡", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = plugin.name,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                when (plugin.status) {
                    0 -> {
                        Text(
                            text = "🔴 Down",
                            color = KitsugiColors.AccentRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KitsugiColors.AccentRed.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    2 -> {
                        Text(
                            text = "🟡 Beta",
                            color = KitsugiColors.AccentOrange,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KitsugiColors.AccentOrange.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                val verText = if (installedVersion != null) {
                    if (installedVersion < plugin.version) {
                        "v$installedVersion → v${plugin.version}"
                    } else {
                        "v${plugin.version}"
                    }
                } else {
                    "v${plugin.version}"
                }

                Text(
                    text = verText,
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (plugin.fileSize != null) {
                    Text(
                        text = formatFileSize(plugin.fileSize),
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(KitsugiColors.SurfaceSoft)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (!plugin.description.isNullOrBlank()) {
                Text(
                    plugin.description,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (plugin.status == 2) {
                Text(
                    text = "⚠️ Bu eklenti test aşamasındadır (Beta/Yavaş). Hatalar barındırabilir.",
                    color = KitsugiColors.AccentOrange,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!plugin.language.isNullOrBlank()) {
                Text(
                    "🌐 ${plugin.language?.uppercase()}",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.width(8.dp))

        when (installState) {
            PluginInstallState.LOADING -> {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = accentColor, strokeWidth = 2.5.dp)
            }
            PluginInstallState.SUCCESS -> {
                Text(
                    text = "✓ Başarılı",
                    color = KitsugiColors.AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KitsugiColors.AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            PluginInstallState.FAILURE -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "✗ Hata",
                        color = KitsugiColors.AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KitsugiColors.AccentRed.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Tekrar",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                onStartInstall()
                                onInstallPlugin(plugin) { success ->
                                    onInstallResult(success)
                                }
                            }
                            .background(accentColor.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            PluginInstallState.IDLE -> {
                val hasUpdate = !isStremio && isInstalled && installedVersion != null && installedVersion < plugin.version
                if (hasUpdate) {
                    Text(
                        text = "⬆️ Güncelle",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                onStartInstall()
                                onInstallPlugin(plugin) { success ->
                                    onInstallResult(success)
                                }
                            }
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else if (isInstalled && !isStremio) {
                    val localContext = LocalContext.current
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "✓ Kuruldu",
                            color = KitsugiColors.AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(KitsugiColors.AccentGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        val isEnabled = installedPlugin?.enabled == true
                        val loadedPluginIds by com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadedPluginIds.collectAsState()
                        val pluginHasSettings = remember(loadedPluginIds, plugin.internalName, isInstalled, isEnabled) {
                            isInstalled && isEnabled && com.kitsugi.animelist.data.cloudstream.CsPluginLoader.hasSettings(plugin.internalName, localContext)
                        }
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        var isSettingsOpening by remember { mutableStateOf(false) }
                        if (pluginHasSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Eklenti Ayarları",
                                tint = accentColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .tvClickable(shape = RoundedCornerShape(6.dp)) {
                                        if (isSettingsOpening) return@tvClickable
                                        isSettingsOpening = true
                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            try {
                                                var loadedPlugin = com.kitsugi.animelist.data.cloudstream.CsPluginLoader
                                                    .getPluginInstance(plugin.internalName, localContext)
                                                if (loadedPlugin == null && isEnabled) {
                                                    com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(localContext, plugin.internalName)
                                                    loadedPlugin = com.kitsugi.animelist.data.cloudstream.CsPluginLoader
                                                        .getPluginInstance(plugin.internalName, localContext)
                                                }
                                                val baseCtx = com.kitsugi.animelist.KitsugiApplication.activeActivity as? androidx.appcompat.app.AppCompatActivity
                                                if (loadedPlugin != null && baseCtx != null) {
                                                    val themedCtx = com.kitsugi.animelist.data.cloudstream.CsPluginContextWrapper(
                                                        baseCtx,
                                                        com.kitsugi.animelist.R.style.Theme_KitsugiAnimeList
                                                    )
                                                    loadedPlugin.openSettings?.invoke(themedCtx)
                                                } else {
                                                    android.widget.Toast.makeText(localContext, "Eklenti veya ekran hazır değil.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Throwable) {
                                                android.util.Log.e("KitsugiAddons", "Ayarlar açılamadı: ${e.message}", e)
                                            } finally {
                                                kotlinx.coroutines.delay(1000)
                                                isSettingsOpening = false
                                            }
                                        }
                                    }
                                    .padding(2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Yeniden Kur",
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .tvClickable(shape = RoundedCornerShape(6.dp)) {
                                    onStartInstall()
                                    onInstallPlugin(plugin) { success ->
                                        onInstallResult(success)
                                    }
                                }
                                .padding(2.dp)
                        )
                    }
                } else if (isInstalled) {
                    Text(
                        text = "✓ Kuruldu",
                        color = KitsugiColors.AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KitsugiColors.AccentGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    if (plugin.status == 0) {
                        Text(
                            text = "Kur",
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(KitsugiColors.SurfaceSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Kur",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                    onStartInstall()
                                    onInstallPlugin(plugin) { success ->
                                        onInstallResult(success)
                                    }
                                }
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
