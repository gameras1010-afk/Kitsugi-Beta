package com.kitsugi.animelist.ui.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.remote.AddonStreamClient
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.data.remote.DebridResolver
import com.kitsugi.animelist.data.repository.CloudstreamRepoRepository
import com.kitsugi.animelist.core.security.RepoVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AddonViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val addonDao = KitsugiDatabase.getDatabase(context).managedAddonDao()
    private val debridResolver = DebridResolver(context)
    private val csRepoRepository = CloudstreamRepoRepository(context)
    private val addonClient = AddonStreamClient()

    val addonsList: Flow<List<ManagedAddonEntity>> = addonDao.getAllAddonsFlow()
    val reposList: Flow<List<CloudstreamRepoEntity>> = csRepoRepository.getReposFlow()
    val csPluginsList: Flow<List<CsPluginEntity>> = csRepoRepository.getAllCsPluginsFlow()

    var debridToken by mutableStateOf(debridResolver.getApiKey() ?: "")
        private set

    var repoPluginsState by mutableStateOf<Map<String, List<CsPlugin>?>>(emptyMap())
    var repoLoadingState by mutableStateOf<Map<String, Boolean>>(emptyMap())

    var bulkInstallRepoUrl by mutableStateOf<String?>(null)
    var bulkInstallRepoName by mutableStateOf<String?>(null)
    var bulkInstallDone by mutableStateOf(0)
    var bulkInstallTotal by mutableStateOf(0)
    var bulkInstallCurrentName by mutableStateOf("")
    var bulkInstallResultMessage by mutableStateOf<String?>(null)
    
    var untrustedRepoToConfirm by mutableStateOf<String?>(null)
        private set

    fun clearUntrustedRepoConfirm() {
        untrustedRepoToConfirm = null
    }

    private val installPluginMutex = Mutex()

    var onShowMessage: ((String) -> Unit)? = null

    init {
        viewModelScope.launch {
            csRepoRepository.syncAndAutoUpdate()
            seedSubtitlePresets()
        }
    }

    private suspend fun seedSubtitlePresets() {
        for (url in com.kitsugi.animelist.data.local.AddonPresets.DEFAULT_SUBTITLE_ADDONS) {
            val normalized = addonClient.normalizeManifestUrl(url)
            val existing = addonDao.getAllAddons().find { it.manifestUrl.trim().equals(normalized.trim(), ignoreCase = true) }
            if (existing == null) {
                val resolved = addonClient.fetchManifest(url)
                if (resolved != null) {
                    addonDao.insertAddon(resolved.copy(isEnabled = true, subtitleTypes = resolved.subtitleTypes ?: "movie,series"))
                } else {
                    addonDao.insertAddon(
                        ManagedAddonEntity(
                            manifestUrl = normalized,
                            name = when {
                                url.contains("opensubtitles-v3") -> "OpenSubtitles v3"
                                url.contains("opensubtitles") -> "OpenSubtitles"
                                url.contains("yts") -> "YTS Subtitles"
                                url.contains("turkcealtyaziorg") -> "TurkceAltyazi.org"
                                else -> "Subtitle Addon"
                            },
                            description = "Sistem Altyazı Eklentisi",
                            icon = null,
                            isEnabled = true,
                            orderIndex = 99,
                            idPrefixes = null,
                            streamTypes = null,
                            subtitleTypes = "movie,series"
                        )
                    )
                }
            } else if (existing.subtitleTypes == null) {
                addonDao.updateAddon(existing.copy(subtitleTypes = "movie,series", isEnabled = true))
            }
        }

        val deprecatedV2 = addonDao.getAllAddons().find {
            it.manifestUrl.contains("opensubtitles.strem.io", ignoreCase = true) &&
            !it.manifestUrl.contains("opensubtitles-v3", ignoreCase = true)
        }
        if (deprecatedV2 != null) {
            addonDao.deleteAddon(deprecatedV2)
        }

        val all = addonDao.getAllAddons()
        for (addon in all) {
            if (addon.subtitleTypes == null && (
                addon.manifestUrl.contains("subtitles", ignoreCase = true) ||
                addon.manifestUrl.contains("opensubtitles", ignoreCase = true) ||
                addon.name.contains("subtitles", ignoreCase = true) ||
                addon.name.contains("OpenSubtitles", ignoreCase = true) ||
                addon.name.contains("TurkceAltyazi", ignoreCase = true)
            )) {
                addonDao.updateAddon(addon.copy(subtitleTypes = "movie,series"))
            }
        }

        autoUpdateStremioAddons()
    }

    private suspend fun autoUpdateStremioAddons() {
        try {
            val allAddons = addonDao.getAllAddons()
            for (addon in allAddons) {
                val freshManifest = addonClient.fetchManifest(addon.manifestUrl)
                if (freshManifest != null) {
                    // Manifest'ten subtitleTypes geldiyse kullan.
                    // Gelmediyse: addon altyazı ile ilgiliyse "movie,series" yaz (eski null kayıtları düzelt).
                    val resolvedSubtitleTypes = freshManifest.subtitleTypes
                        ?: if (addon.subtitleTypes != null) addon.subtitleTypes
                        else if (looksLikeSubtitleAddonUrl(addon.manifestUrl, addon.name)) "movie,series"
                        else null

                    val updated = addon.copy(
                        name = freshManifest.name,
                        description = freshManifest.description ?: addon.description,
                        icon = freshManifest.icon ?: addon.icon,
                        idPrefixes = freshManifest.idPrefixes ?: addon.idPrefixes,
                        streamTypes = freshManifest.streamTypes ?: addon.streamTypes,
                        subtitleTypes = resolvedSubtitleTypes
                    )
                    if (updated != addon) {
                        addonDao.updateAddon(updated)
                        android.util.Log.d("AddonViewModel", "Addon güncellendi: ${addon.name} subtitleTypes=${resolvedSubtitleTypes}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddonViewModel", "Auto update stremio addons error: ${e.message}")
        }
    }

    private fun looksLikeSubtitleAddonUrl(manifestUrl: String, name: String): Boolean {
        val combined = (manifestUrl + " " + name).lowercase()
        return combined.contains("subtitle") ||
            combined.contains("altyaz") ||
            combined.contains("opensubtitle") ||
            combined.contains("turkcealtyazi") ||
            combined.contains("caption") ||
            combined.contains("yts-sub")
    }

    fun syncRepos(repos: List<CloudstreamRepoEntity>) {
        viewModelScope.launch {
            repos.forEach { repo ->
                if (!repoPluginsState.containsKey(repo.repoUrl) && repoLoadingState[repo.repoUrl] != true) {
                    fetchRepoPlugins(repo.repoUrl)
                }
            }
        }
    }

    fun addAddon(urlsInput: String) {
        viewModelScope.launch {
            val urls = urlsInput.split(Regex("[,\\n\\r\\s]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (urls.isEmpty()) {
                onShowMessage?.invoke("Geçersiz URL veya boş girdi")
                return@launch
            }
            var successCount = 0
            var failCount = 0
            for (url in urls) {
                var resolved = addonClient.fetchManifest(url)
                if (resolved == null) {
                    // Fallback to static manifest for popular addons if Cloudflare/network blocks direct JSON fetch
                    resolved = when {
                        url.contains("torrentio.strem.fun", ignoreCase = true) -> {
                            val normalized = addonClient.normalizeManifestUrl(url)
                            ManagedAddonEntity(
                                manifestUrl = normalized,
                                name = "Torrentio",
                                description = "Ana torrent kaynağı (YTS, EZTV, RARBG, 1337x, TPB vb.). RealDebrid desteği sunar.",
                                icon = "https://torrentio.strem.fun/images/logo.png",
                                isEnabled = true,
                                orderIndex = 0,
                                idPrefixes = "[\"tt\",\"kitsu\"]",
                                streamTypes = "movie,series",
                                subtitleTypes = null
                            )
                        }
                        url.contains("anime-kitsu.strem.fun", ignoreCase = true) -> {
                            val normalized = addonClient.normalizeManifestUrl(url)
                            ManagedAddonEntity(
                                manifestUrl = normalized,
                                name = "Anime Kitsu",
                                description = "Kitsu üzerinden anime listelerini ve verileri yükler.",
                                icon = null,
                                isEnabled = true,
                                orderIndex = 1,
                                idPrefixes = "[\"kitsu\"]",
                                streamTypes = "series",
                                subtitleTypes = null
                            )
                        }
                        else -> null
                    }
                }
                if (resolved != null) {
                    addonDao.insertAddon(resolved)
                    successCount++
                } else {
                    failCount++
                }
            }
            val message = when {
                successCount > 0 && failCount == 0 -> {
                    if (successCount == 1) "Eklenti başarıyla yüklendi!" else "$successCount eklenti başarıyla yüklendi!"
                }
                successCount > 0 && failCount > 0 -> {
                    "$successCount eklenti yüklendi, $failCount eklenti yüklenemedi."
                }
                else -> "Eklenti manifestosu yüklenemedi. Bağlantıyı kontrol edin."
            }
            onShowMessage?.invoke(message)
        }
    }

    fun toggleAddon(addon: ManagedAddonEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            val isPreset = com.kitsugi.animelist.data.local.AddonPresets.DEFAULT_SUBTITLE_ADDONS.any {
                val normalizedPreset = addonClient.normalizeManifestUrl(it)
                normalizedPreset.trim().equals(addon.manifestUrl.trim(), ignoreCase = true)
            }
            if (isPreset) {
                addonDao.updateAddon(addon.copy(isEnabled = true))
                onShowMessage?.invoke("Sistem altyazı eklentileri devre dışı bırakılamaz.")
            } else {
                addonDao.updateAddon(addon.copy(isEnabled = isEnabled))
            }
        }
    }

    fun deleteAddon(addon: ManagedAddonEntity) {
        viewModelScope.launch {
            val isPreset = com.kitsugi.animelist.data.local.AddonPresets.DEFAULT_SUBTITLE_ADDONS.any {
                val normalizedPreset = addonClient.normalizeManifestUrl(it)
                normalizedPreset.trim().equals(addon.manifestUrl.trim(), ignoreCase = true)
            }
            if (isPreset) {
                onShowMessage?.invoke("Sistem altyazı eklentileri silinemez.")
            } else {
                addonDao.deleteAddon(addon)
                onShowMessage?.invoke("Eklenti silindi: ${addon.name}")
            }
        }
    }

    fun saveDebridToken(token: String) {
        debridToken = token
        debridResolver.setApiKey(token)
    }

    fun addRepo(urlsInput: String, force: Boolean = false) {
        viewModelScope.launch {
            val urls = urlsInput.split(Regex("[,\\n\\r\\s]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (urls.isEmpty()) {
                onShowMessage?.invoke("Geçersiz URL veya boş girdi")
                return@launch
            }
            if (!force) {
                val untrusted = urls.firstOrNull { !RepoVerifier.isUrlTrusted(context, it) }
                if (untrusted != null) {
                    untrustedRepoToConfirm = urlsInput
                    return@launch
                }
            }
            
            untrustedRepoToConfirm = null
            var successCount = 0
            var failCount = 0
            var lastErrorMessage = ""
            for (url in urls) {
                RepoVerifier.trustRepo(context, url)
                val result = csRepoRepository.addRepo(url)
                result.fold(
                    onSuccess = { successCount++ },
                    onFailure = { e ->
                        failCount++
                        lastErrorMessage = e.message ?: "Bilinmeyen hata"
                    }
                )
            }
            val message = when {
                successCount > 0 && failCount == 0 -> {
                    if (successCount == 1) "Repo başarıyla eklendi!" else "$successCount repo başarıyla eklendi!"
                }
                successCount > 0 && failCount > 0 -> {
                    "$successCount repo eklendi, $failCount repo eklenemedi."
                }
                else -> {
                    if (urls.size == 1) "Repo eklenemedi: $lastErrorMessage" else "Hiçbir repo eklenemedi. Son hata: $lastErrorMessage"
                }
            }
            onShowMessage?.invoke(message)
        }
    }

    fun deleteRepo(repo: CloudstreamRepoEntity) {
        viewModelScope.launch {
            csRepoRepository.deleteRepo(repo)
            onShowMessage?.invoke("Repo silindi: ${repo.name}")
        }
    }

    fun fetchRepoPlugins(repoUrl: String) {
        viewModelScope.launch {
            repoLoadingState = repoLoadingState + (repoUrl to true)
            val plugins = csRepoRepository.fetchPluginsForRepo(repoUrl)
            repoPluginsState = repoPluginsState + (repoUrl to plugins)
            repoLoadingState = repoLoadingState + (repoUrl to false)
        }
    }

    fun installPlugin(plugin: CsPlugin, onResult: ((Boolean) -> Unit)?) {
        viewModelScope.launch {
            installPluginMutex.withLock {
                val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                val installed = if (isStremio) {
                    csRepoRepository.installPluginAsAddon(plugin)
                } else {
                    csRepoRepository.installCsPlugin(plugin)
                }
                if (installed && !isStremio) {
                    try {
                        com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, plugin.internalName)
                    } catch (e: Exception) {
                        android.util.Log.e("InstallPlugin", "Failed to load extension ${plugin.internalName}", e)
                    }
                }
                onResult?.invoke(installed)
                onShowMessage?.invoke(
                    if (installed) "✅ Kuruldu: ${plugin.name}"
                    else "⚠️ Zaten kurulu ya da indirilemedi: ${plugin.name}"
                )
            }
        }
    }

    fun installAllPlugins(
        repoUrl: String,
        repoName: String,
        plugins: List<CsPlugin>,
        addons: List<ManagedAddonEntity>,
        csPlugins: List<CsPluginEntity>
    ) {
        viewModelScope.launch {
            val toInstall = plugins.filter { plugin ->
                val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                if (isStremio) {
                    addons.none { it.manifestUrl.trim().equals(plugin.url.trim(), ignoreCase = true) }
                } else {
                    csPlugins.none { it.id == plugin.internalName }
                }
            }
            if (toInstall.isEmpty()) {
                bulkInstallResultMessage = "ℹ️ Bu repodaki tüm eklentiler zaten kurulu"
                return@launch
            }

            bulkInstallRepoUrl = repoUrl
            bulkInstallRepoName = repoName
            bulkInstallTotal = toInstall.size
            bulkInstallDone = 0
            bulkInstallCurrentName = toInstall.firstOrNull()?.name ?: ""
            bulkInstallResultMessage = null

            var successCount = 0
            val failedNames = mutableListOf<String>()
            for ((index, plugin) in toInstall.withIndex()) {
                bulkInstallCurrentName = plugin.name
                bulkInstallDone = index
                installPluginMutex.withLock {
                    val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                    val installed = if (isStremio) {
                        csRepoRepository.installPluginAsAddon(plugin)
                    } else {
                        csRepoRepository.installCsPlugin(plugin)
                    }
                    if (installed) {
                        successCount++
                        if (!isStremio) {
                            try {
                                com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, plugin.internalName)
                            } catch (e: Exception) {
                                android.util.Log.e("BulkInstall", "Failed to load extension ${plugin.internalName}", e)
                            }
                        }
                    } else {
                        failedNames.add(plugin.name)
                    }
                }
                bulkInstallDone = index + 1
                kotlinx.coroutines.delay(150L)
            }
            val failCount = failedNames.size
            bulkInstallResultMessage = when {
                failCount == 0 -> "✅ $successCount eklenti başarıyla kuruldu!"
                successCount > 0 -> "⚠️ $successCount kuruldu, $failCount kurulamadı (sunucu dosyaları silinmiş olabilir)"
                else -> "❌ $failCount eklenti kurulamadı — sunucu dosyaları kaldırmış olabilir"
            }

            bulkInstallRepoUrl = null
            bulkInstallRepoName = null
            bulkInstallDone = 0
            bulkInstallTotal = 0
            bulkInstallCurrentName = ""
        }
    }

    fun updateAllPlugins(repoUrl: String, repoName: String, plugins: List<CsPlugin>, csPlugins: List<CsPluginEntity>) {
        viewModelScope.launch {
            val toUpdate = plugins.filter { plugin ->
                val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                if (isStremio) {
                    false
                } else {
                    val installed = csPlugins.find { it.id == plugin.internalName }
                    installed != null && installed.version < plugin.version
                }
            }
            if (toUpdate.isEmpty()) {
                bulkInstallResultMessage = "ℹ️ Güncellenecek eklenti bulunamadı"
                return@launch
            }

            bulkInstallRepoUrl = repoUrl
            bulkInstallRepoName = repoName
            bulkInstallTotal = toUpdate.size
            bulkInstallDone = 0
            bulkInstallCurrentName = toUpdate.firstOrNull()?.name ?: ""
            bulkInstallResultMessage = null

            var successCount = 0
            val failedNames = mutableListOf<String>()
            for ((index, plugin) in toUpdate.withIndex()) {
                bulkInstallCurrentName = plugin.name
                bulkInstallDone = index
                installPluginMutex.withLock {
                    val installed = csRepoRepository.installCsPlugin(plugin)
                    if (installed) {
                        successCount++
                        try {
                            com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, plugin.internalName, forceReload = true)
                        } catch (e: Exception) {
                            android.util.Log.e("BulkUpdate", "Failed to load extension ${plugin.internalName}", e)
                        }
                    } else {
                        failedNames.add(plugin.name)
                    }
                }
                bulkInstallDone = index + 1
                kotlinx.coroutines.delay(150L)
            }
            val failCount = failedNames.size
            bulkInstallResultMessage = when {
                failCount == 0 -> "✅ $successCount eklenti başarıyla güncellendi!"
                successCount > 0 -> "⚠️ $successCount güncellendi, $failCount güncellenemedi"
                else -> "❌ $failCount eklenti güncellenemedi"
            }

            bulkInstallRepoUrl = null
            bulkInstallRepoName = null
            bulkInstallDone = 0
            bulkInstallTotal = 0
            bulkInstallCurrentName = ""
        }
    }

    fun toggleCsPlugin(plugin: CsPluginEntity, enabled: Boolean) {
        viewModelScope.launch {
            csRepoRepository.toggleCsPlugin(plugin.id, enabled)
            if (enabled) {
                try {
                    com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, plugin.id)
                } catch (e: Exception) {
                    android.util.Log.e("ToggleCsPlugin", "Failed to load extension ${plugin.id} on toggle", e)
                }
            }
        }
    }

    fun uninstallCsPlugin(plugin: CsPluginEntity) {
        viewModelScope.launch {
            val csPlugin = CsPlugin(
                name = plugin.name,
                internalName = plugin.id,
                url = plugin.downloadUrl,
                description = "",
                version = plugin.version,
                language = null,
                tvTypes = null,
                iconUrl = plugin.iconUrl,
                authors = emptyList()
            )
            csRepoRepository.uninstallCsPlugin(csPlugin)
            onShowMessage?.invoke("Eklenti kaldırıldı: ${plugin.name}")
        }
    }

    fun clearBulkInstallResult() {
        bulkInstallResultMessage = null
    }
}
