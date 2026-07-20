package com.kitsugi.animelist.data.repository

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.local.CloudstreamRepoDao
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.ManagedAddonDao
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.remote.CloudstreamRepoClient
import com.kitsugi.animelist.data.remote.CsPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Business logic layer for Cloudstream-style repository management.
 *
 * Repos use the same JSON format as Cloudstream (GitHub-hosted manifest),
 * but instead of loading .cs3 plugin binaries, we extract the Stremio
 * manifest URL from each plugin's "url" field and save it as a
 * [ManagedAddonEntity] — the same entity used for regular Stremio addons.
 */
class CloudstreamRepoRepository(private val context: Context) {

    private val db = KitsugiDatabase.getDatabase(context.applicationContext)
    private val repoDao: CloudstreamRepoDao = db.cloudstreamRepoDao()
    private val addonDao: ManagedAddonDao = db.managedAddonDao()
    private val csPluginDao = db.csPluginDao()
    private val client = CloudstreamRepoClient()

    companion object {
        private const val TAG = "CloudstreamRepoRepository"
        /** App process seviyesinde tek bir sync çalışması yeterlidir.
         *  30 dakika geçmeden ikinci bir syncAndAutoUpdate başlatılmaz.
         */
        @Volatile private var lastSyncAtMs: Long = 0L
        private const val SYNC_THROTTLE_MS = 30 * 60 * 1000L // 30 dakika
    }

    /** Flow of all stored repos, ordered by insertion time. */
    fun getReposFlow(): Flow<List<CloudstreamRepoEntity>> = repoDao.getAllReposFlow()

    /**
     * Normalizes a Cloudstream repository URL, handling custom redirector wrappers
     * and stripping the cloudstreamrepo:// custom protocol scheme.
     */
    fun normalizeRepoUrl(rawUrl: String): String {
        return com.kitsugi.animelist.utils.CloudstreamUrlHelper.normalizeUrl(rawUrl)
    }

    /**
     * Validates and adds a new repository.
     * @return A [Result] containing the repo on success, or an exception describing the failure.
     */
    suspend fun addRepo(repoUrl: String): Result<CloudstreamRepoEntity> = withContext(Dispatchers.IO) {
        try {
            var inputUrl = repoUrl.trim()

            // Kısa kod mu? (örn. "kekikdevam") → cutt.ly üzerinden gerçek URL'ye çöz
            if (com.kitsugi.animelist.utils.CloudstreamUrlHelper.isShortCode(inputUrl)) {
                Log.d(TAG, "Kısa kod tespit edildi: '$inputUrl' → cutt.ly çözümü deneniyor...")
                val resolved = com.kitsugi.animelist.utils.CloudstreamUrlHelper.resolveShortCode(inputUrl)
                    ?: return@withContext Result.failure(
                        Exception("'$inputUrl' kısa kodu geçersiz veya bulunamadı. Lütfen tam URL'yi deneyin.")
                    )
                Log.d(TAG, "Kısa kod çözüldü: '$inputUrl' → '$resolved'")
                inputUrl = resolved
            }

            val normalizedUrl = normalizeRepoUrl(inputUrl)
            val fetched = client.fetchRepo(normalizedUrl)
                ?: return@withContext Result.failure(Exception("Repo manifest alınamadı. URL'yi kontrol edin."))

            val entity = CloudstreamRepoEntity(
                repoUrl = normalizedUrl,
                name = fetched.name,
                description = fetched.description
            )
            repoDao.insertRepo(entity)
            Log.d(TAG, "Added repo: ${entity.name} (${entity.repoUrl})")
            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "addRepo failed for $repoUrl", e)
            Result.failure(e)
        }
    }

    /** Removes a repo from the database. Does NOT remove its installed addons. */
    suspend fun deleteRepo(repo: CloudstreamRepoEntity) = withContext(Dispatchers.IO) {
        repoDao.deleteRepo(repo)
        Log.d(TAG, "Deleted repo: ${repo.name}")
    }

    suspend fun fetchPluginsForRepo(repoUrl: String): List<CsPlugin>? = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeRepoUrl(repoUrl)
        client.fetchAllPlugins(normalizedUrl)
    }

    /**
     * Installs a [CsPlugin] from a Cloudstream repo into the managed addons
     * table as if it were a regular Stremio addon.
     *
     * The plugin's "url" field must be a Stremio manifest URL (ending in /manifest.json
     * or pointing to a Stremio-compatible addon).
     *
     * @return true if installed successfully, false if the URL is already installed.
     */
    suspend fun installPluginAsAddon(plugin: CsPlugin): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = addonDao.getAllAddons()
            val alreadyInstalled = existing.any {
                it.manifestUrl.trim().equals(plugin.url.trim(), ignoreCase = true)
            }
            if (alreadyInstalled) {
                Log.d(TAG, "Plugin already installed: ${plugin.name}")
                return@withContext false
            }

            val addonClient = com.kitsugi.animelist.data.remote.AddonStreamClient()
            val fetched = addonClient.fetchManifest(plugin.url.trim())

            val entity = if (fetched != null) {
                fetched.copy(
                    orderIndex = existing.size + 1,
                    isEnabled = true
                )
            } else {
                ManagedAddonEntity(
                    manifestUrl = plugin.url.trim(),
                    name = plugin.name,
                    description = plugin.description,
                    icon = plugin.iconUrl,
                    isEnabled = true,
                    orderIndex = existing.size + 1,
                    idPrefixes = null,
                    streamTypes = plugin.tvTypes?.joinToString(",")?.lowercase()
                )
            }
            addonDao.insertAddon(entity)
            Log.d(TAG, "Installed plugin as addon: ${entity.name} -> ${entity.manifestUrl} (resolved: ${fetched != null})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "installPluginAsAddon failed for ${plugin.name}", e)
            false
        }
    }

    /**
     * Checks whether a plugin from a repo is already installed as an addon.
     */
    suspend fun isPluginInstalled(pluginUrl: String): Boolean = withContext(Dispatchers.IO) {
        addonDao.getAllAddons().any {
            it.manifestUrl.trim().equals(pluginUrl.trim(), ignoreCase = true)
        }
    }

    fun getEnabledCsPluginsFlow(): Flow<List<com.kitsugi.animelist.data.local.CsPluginEntity>> = csPluginDao.getEnabledPluginsFlow()

    fun getAllCsPluginsFlow(): Flow<List<com.kitsugi.animelist.data.local.CsPluginEntity>> = csPluginDao.getAllPluginsFlow()

    suspend fun toggleCsPlugin(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        csPluginDao.getPluginById(id)?.let {
            csPluginDao.upsert(it.copy(enabled = enabled))
            if (!enabled) {
                com.kitsugi.animelist.data.cloudstream.CsPluginLoader.unloadExtension(context, id)
            }
        }
    }

    suspend fun isCsPluginInstalled(id: String): Boolean = withContext(Dispatchers.IO) {
        csPluginDao.getPluginById(id) != null
    }

    suspend fun installCsPlugin(plugin: CsPlugin): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadSuccess = com.kitsugi.animelist.data.cloudstream.CsPluginLoader.downloadExtension(
                context,
                plugin.internalName,
                plugin.url,
                plugin.fileHash
            )
            if (!downloadSuccess) {
                Log.e(TAG, "Failed to download CS plugin: ${plugin.name}")
                return@withContext false
            }

            val entity = com.kitsugi.animelist.data.local.CsPluginEntity(
                id = plugin.internalName,
                name = plugin.name,
                downloadUrl = plugin.url,
                tvTypes = com.google.gson.Gson().toJson(plugin.tvTypes ?: emptyList<String>()),
                iconUrl = plugin.iconUrl,
                version = plugin.version,
                enabled = true,
                installedAt = System.currentTimeMillis()
            )
            csPluginDao.upsert(entity)
            Log.d(TAG, "Successfully installed CS plugin: ${plugin.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "installCsPlugin failed for ${plugin.name}", e)
            false
        }
    }

    suspend fun uninstallCsPlugin(plugin: CsPlugin) = withContext(Dispatchers.IO) {
        try {
            val entity = csPluginDao.getPluginById(plugin.internalName)
            if (entity != null) {
                csPluginDao.delete(entity)
            }
            // Unload the extension from memory
            com.kitsugi.animelist.data.cloudstream.CsPluginLoader.unloadExtension(context, plugin.internalName)

            val extensionFile = java.io.File(context.filesDir, "cs_extensions/${plugin.internalName}.cs3")
            if (extensionFile.exists()) {
                extensionFile.delete()
            }
            Log.d(TAG, "Successfully uninstalled CS plugin: ${plugin.name}")
        } catch (e: Exception) {
            Log.e(TAG, "uninstallCsPlugin failed for ${plugin.name}", e)
        }
    }

    suspend fun syncAndAutoUpdate() = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastSyncAtMs < SYNC_THROTTLE_MS) {
                Log.d(TAG, "syncAndAutoUpdate throttled — son sync'ten ${(now - lastSyncAtMs) / 1000}s geçti, 30dk dolmadı.")
                return@withContext
            }
            lastSyncAtMs = now
            Log.d(TAG, "Starting syncAndAutoUpdate...")
            val currentRepos = repoDao.getAllRepos()



            // 1. Automatically migrate legacy repository URLs (e.g. keyiflerolsun -> maarrem)
            val refreshedRepos = repoDao.getAllRepos()
            for (repo in refreshedRepos) {
                val normalized = com.kitsugi.animelist.utils.CloudstreamUrlHelper.normalizeUrl(repo.repoUrl)
                if (!normalized.equals(repo.repoUrl, ignoreCase = true)) {
                    Log.d(TAG, "Migrating repository in database: ${repo.repoUrl} -> $normalized")
                    repoDao.deleteRepo(repo)
                    
                    // Fetch metadata of the new repository
                    val fetched = client.fetchRepo(normalized)
                    val newEntity = if (fetched != null) {
                        CloudstreamRepoEntity(
                            repoUrl = normalized,
                            name = fetched.name,
                            description = fetched.description,
                            addedAt = repo.addedAt
                        )
                    } else {
                        CloudstreamRepoEntity(
                            repoUrl = normalized,
                            name = if (repo.name.contains("keyiflerolsun", ignoreCase = true) || repo.name.contains("KekikAkademi", ignoreCase = true)) {
                                "Turkish Providers Repository | @maarrem"
                            } else {
                                repo.name
                            },
                            description = repo.description,
                            addedAt = repo.addedAt
                        )
                    }
                    repoDao.insertRepo(newEntity)
                    Log.d(TAG, "Successfully migrated repository to $normalized")
                }
            }

            // 2. Fetch all latest plugins from all active repos
            val updatedRepos = repoDao.getAllRepos()
            val allLatestPluginsMap = mutableMapOf<String, CsPlugin>()
            for (repo in updatedRepos) {
                try {
                    val repoPlugins = fetchPluginsForRepo(repo.repoUrl)
                    if (repoPlugins != null) {
                        for (plugin in repoPlugins) {
                            val existing = allLatestPluginsMap[plugin.internalName]
                            if (existing == null || plugin.version > existing.version) {
                                allLatestPluginsMap[plugin.internalName] = plugin
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch plugins for repo: ${repo.repoUrl}", e)
                }
            }

            // 3. Auto-update installed CS plugins if newer versions are available
            val installedCsPlugins = csPluginDao.getAllPlugins()
            for (installedPlugin in installedCsPlugins) {
                val latestPlugin = allLatestPluginsMap[installedPlugin.id]
                if (latestPlugin != null && latestPlugin.version > installedPlugin.version) {
                    Log.d(TAG, "Auto-updating plugin: ${installedPlugin.name} from version ${installedPlugin.version} to ${latestPlugin.version}")
                    
                    val downloadSuccess = com.kitsugi.animelist.data.cloudstream.CsPluginLoader.downloadExtension(
                        context,
                        latestPlugin.internalName,
                        latestPlugin.url,
                        latestPlugin.fileHash
                    )
                    if (downloadSuccess) {
                        val updatedEntity = installedPlugin.copy(
                            version = latestPlugin.version,
                            downloadUrl = latestPlugin.url,
                            iconUrl = latestPlugin.iconUrl ?: installedPlugin.iconUrl,
                            tvTypes = com.google.gson.Gson().toJson(latestPlugin.tvTypes ?: emptyList<String>())
                        )
                        csPluginDao.upsert(updatedEntity)
                        
                        if (installedPlugin.enabled) {
                            Log.d(TAG, "Reloading updated plugin in memory: ${installedPlugin.name}")
                            com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, installedPlugin.id)
                        }
                        Log.d(TAG, "Successfully auto-updated plugin: ${installedPlugin.name}")
                    } else {
                        Log.e(TAG, "Failed to download update for plugin: ${installedPlugin.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncAndAutoUpdate failed", e)
        }
    }
}
