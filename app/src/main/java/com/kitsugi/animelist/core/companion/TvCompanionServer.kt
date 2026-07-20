package com.kitsugi.animelist.core.companion

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import org.json.JSONArray
import org.json.JSONObject

/**
 * NanoHTTPD-based local HTTP server that exposes the TV Companion REST API.
 *
 * ## Binding
 * The server binds to the device's LAN IP (see [DeviceIpAddress]) on the first
 * available port in range [PORT_RANGE_START]..[PORT_RANGE_END]. If no port is
 * available the server will not start and [boundPort] returns -1.
 *
 * ## Authentication
 * Every request (except `/health`) MUST include a `token` query parameter that
 * matches the current [TvCompanionSessionManager] token. Mismatched tokens
 * return HTTP 401.
 *
 * ## Endpoints
 * | Method | Path                          | Auth | Description                          |
 * |--------|-------------------------------|------|--------------------------------------|
 * | GET    | /health                       | –    | Always 200; used to test reachability|
 * | GET    | /companion                    | ✔   | Returns session info JSON             |
 * | POST   | /companion/auth               | –    | Authenticates with token             |
 * | GET    | /companion/addons             | ✔   | Lists installed Stremio addons        |
 * | POST   | /companion/addons             | ✔   | Request addon install approval        |
 * | DELETE | /companion/addons/{id}        | ✔   | Delete an addon by ID                 |
 * | PATCH  | /companion/addons/{id}        | ✔   | Toggle enable/disable an addon        |
 * | GET    | /companion/repos              | ✔   | Lists installed Cloudstream repos     |
 * | POST   | /companion/repos              | ✔   | Request repository add approval       |
 * | DELETE | /companion/repos/{id}         | ✔   | Delete a repo by ID                   |
 * | GET    | /companion/csplugins          | ✔   | Lists installed Cloudstream plugins   |
 * | PATCH  | /companion/csplugins/{id}     | ✔   | Toggle CS plugin enabled state        |
 * | DELETE | /companion/csplugins/{id}     | ✔   | Uninstall a CS plugin                 |
 * | GET    | /companion/manga/repos        | ✔   | Lists manga repository URLs           |
 * | POST   | /companion/manga/repos        | ✔   | Request manga repo add approval       |
 * | DELETE | /companion/manga/repos/{url}  | ✔   | Delete a manga repository             |
 * | GET    | /companion/manga/sources      | ✔   | Lists installed manga sources         |
 * | PATCH  | /companion/manga/sources/{k}  | ✔   | Toggle manga source enabled state     |
 * | DELETE | /companion/manga/sources/{k}  | ✔   | Uninstall a manga source              |
 * | GET    | /companion/settings/apikeys   | ✔   | Returns masked API key status         |
 * | PATCH  | /companion/settings/apikeys   | ✔   | Set/clear an API key (write-only)     |
 * | GET    | /companion/settings/player    | ✔   | Returns current player settings       |
 * | PATCH  | /companion/settings/player    | ✔   | Update a player setting               |
 * | POST   | /companion/disconnect         | ✔   | Graceful disconnect                   |
 */
class TvCompanionServer(
    private val context: android.content.Context,
    private val sessionManager: TvCompanionSessionManager,
    /** Returns the current list of installed Stremio addons as JSON-serializable maps. */
    private val addonProvider: () -> List<Map<String, Any>>,
    /** Returns the current list of installed Cloudstream repos as JSON-serializable maps. */
    private val repoProvider: () -> List<Map<String, Any>>,
    /** Returns the current list of installed Cloudstream plugins (extensions) as JSON-serializable maps. */
    private val csPluginsProvider: () -> List<Map<String, Any>>,
    /** Returns the current list of manga repository URLs. */
    private val mangaReposProvider: () -> List<String>,
    /** Returns the current list of manga sources as JSON-serializable maps. */
    private val mangaSourcesProvider: () -> List<Map<String, Any>>,
    /** Called when the companion requests deleting an addon by its manifestUrl (URL-encoded). */
    private val onDeleteAddon: (String) -> Unit,
    /** Called when the companion requests toggling an addon enable state by manifestUrl. */
    private val onToggleAddon: (String, Boolean) -> Unit,
    /** Called when the companion requests deleting a repo by its repoUrl (URL-encoded). */
    private val onDeleteRepo: (String) -> Unit,
    /** Called when the companion requests toggling a Cloudstream plugin enabled state. */
    private val onToggleCsPlugin: (String, Boolean) -> Unit,
    /** Called when the companion requests uninstalling a Cloudstream plugin. */
    private val onUninstallCsPlugin: (String) -> Unit,
    /** Called when the companion requests deleting a manga repo by its URL. */
    private val onDeleteMangaRepo: (String) -> Unit,
    /** Called when the companion requests toggling a manga source active state. */
    private val onToggleMangaSource: (String, Boolean) -> Unit,
    /** Called when the companion requests uninstalling/deleting a manga source extension. */
    private val onDeleteMangaSource: (String) -> Unit,
    /** Returns current API key masked state as a JSON-serializable map (keys are never returned in plaintext). */
    private val apiKeysProvider: () -> Map<String, Any>,
    /** Called when companion requests saving an API key. Key = field name (tmdb/mdblist/aniskip/debrid), value = new value (empty string clears). */
    private val onSaveApiKey: (String, String) -> Unit,
    /** Returns current player settings as a JSON-serializable map. */
    private val playerSettingsProvider: () -> Map<String, Any>,
    /** Called when companion requests updating a player setting. Key = field name, value = new string value. */
    private val onSavePlayerSetting: (String, String) -> Unit,
    /** D5: Export backup data as JSON string. */
    private val backupProvider: () -> String,
    /** D5: Import backup data. Returns true on success, false on parsing failure. */
    private val onImportBackup: (String) -> Boolean,
    /** Called on the main thread when an approval-requiring event arrives. */
    private val onApprovalRequested: (TvCompanionSessionManager.PendingRequest) -> Unit
) : NanoHTTPD(DeviceIpAddress.get() ?: "0.0.0.0", findAvailablePort()) {

    // ── Routing ───────────────────────────────────────────────────────────────

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trimEnd('/')
        val method = session.method

        return when {
            // ── Health (no auth) ──────────────────────────────────────────────
            uri == "/health" && method == Method.GET -> healthResponse()

            // ── Static Web Client (no auth) ───────────────────────────────────
            (uri == "" || uri == "/index.html") && method == Method.GET -> serveWebClient()

            // ── Auth (no auth guard, token in body) ──────────────────────────
            uri == "/companion/auth" && method == Method.POST ->
                handleAuth(session)

            // ── Guarded endpoints ────────────────────────────────────────────
            else -> withAuth(session) { handleGuarded(uri, method, session) }
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private fun healthResponse(): Response =
        newJsonResponse(Status.OK, json("status" to "ok", "service" to "Kitsugi-companion"))

    private fun handleAuth(session: IHTTPSession): Response {
        val body = session.parseBody()
        val token = body["token"] ?: queryParam(session, "token") ?: ""
        return if (sessionManager.authenticate(token)) {
            newJsonResponse(Status.OK, json("authenticated" to true))
        } else {
            newJsonResponse(Status.UNAUTHORIZED, json("error" to "invalid_token"))
        }
    }

    private fun handleGuarded(uri: String, method: Method, session: IHTTPSession): Response {
        // Path parameters
        val addonIdFromPath: String? = if (uri.startsWith("/companion/addons/"))
            java.net.URLDecoder.decode(uri.removePrefix("/companion/addons/"), "UTF-8").takeIf { it.isNotBlank() }
        else null
        val repoIdFromPath: String? = if (uri.startsWith("/companion/repos/"))
            java.net.URLDecoder.decode(uri.removePrefix("/companion/repos/"), "UTF-8").takeIf { it.isNotBlank() }
        else null
        val csPluginIdFromPath: String? = if (uri.startsWith("/companion/csplugins/"))
            java.net.URLDecoder.decode(uri.removePrefix("/companion/csplugins/"), "UTF-8").takeIf { it.isNotBlank() }
        else null
        val mangaRepoFromPath: String? = if (uri.startsWith("/companion/manga/repos/"))
            java.net.URLDecoder.decode(uri.removePrefix("/companion/manga/repos/"), "UTF-8").takeIf { it.isNotBlank() }
        else null
        val mangaSourceFromPath: String? = if (uri.startsWith("/companion/manga/sources/"))
            java.net.URLDecoder.decode(uri.removePrefix("/companion/manga/sources/"), "UTF-8").takeIf { it.isNotBlank() }
        else null

        return when {
            uri == "/companion" && method == Method.GET ->
                handleSessionInfo()

            // ── Addons ────────────────────────────────────────────────────────
            uri == "/companion/addons" && method == Method.GET ->
                handleListAddons()

            uri == "/companion/addons" && method == Method.POST ->
                handleApprovalRequest(session, action = "INSTALL_ADDON")

            // Backward-compat alias
            uri == "/companion/addon" && method == Method.POST ->
                handleApprovalRequest(session, action = "INSTALL_ADDON")

            addonIdFromPath != null && method == Method.DELETE -> {
                onDeleteAddon(addonIdFromPath)
                newJsonResponse(Status.OK, json("deleted" to true, "id" to addonIdFromPath))
            }

            addonIdFromPath != null && method == Method.PATCH -> {
                val body = session.parseBody()
                val enabled = body["enabled"]?.toBooleanStrictOrNull() ?: true
                onToggleAddon(addonIdFromPath, enabled)
                newJsonResponse(Status.OK, json("toggled" to true, "id" to addonIdFromPath, "enabled" to enabled))
            }

            // ── Repos ─────────────────────────────────────────────────────────
            uri == "/companion/repos" && method == Method.GET ->
                handleListRepos()

            uri == "/companion/repos" && method == Method.POST ->
                handleApprovalRequest(session, action = "ADD_REPO")

            // Backward-compat alias
            uri == "/companion/repo" && method == Method.POST ->
                handleApprovalRequest(session, action = "ADD_REPO")

            repoIdFromPath != null && method == Method.DELETE -> {
                onDeleteRepo(repoIdFromPath)
                newJsonResponse(Status.OK, json("deleted" to true, "id" to repoIdFromPath))
            }

            // ── Cloudstream Plugins ───────────────────────────────────────────
            uri == "/companion/csplugins" && method == Method.GET ->
                handleListCsPlugins()

            csPluginIdFromPath != null && method == Method.PATCH -> {
                val body = session.parseBody()
                val enabled = body["enabled"]?.toBooleanStrictOrNull() ?: true
                onToggleCsPlugin(csPluginIdFromPath, enabled)
                newJsonResponse(Status.OK, json("toggled" to true, "id" to csPluginIdFromPath, "enabled" to enabled))
            }

            csPluginIdFromPath != null && method == Method.DELETE -> {
                onUninstallCsPlugin(csPluginIdFromPath)
                newJsonResponse(Status.OK, json("uninstalled" to true, "id" to csPluginIdFromPath))
            }

            // ── Manga Repos ───────────────────────────────────────────────────
            uri == "/companion/manga/repos" && method == Method.GET ->
                handleListMangaRepos()

            uri == "/companion/manga/repos" && method == Method.POST ->
                handleApprovalRequest(session, action = "ADD_REPO")

            mangaRepoFromPath != null && method == Method.DELETE -> {
                onDeleteMangaRepo(mangaRepoFromPath)
                newJsonResponse(Status.OK, json("deleted" to true, "id" to mangaRepoFromPath))
            }

            // ── Manga Sources ─────────────────────────────────────────────────
            uri == "/companion/manga/sources" && method == Method.GET ->
                handleListMangaSources()

            mangaSourceFromPath != null && method == Method.PATCH -> {
                val body = session.parseBody()
                val enabled = body["enabled"]?.toBooleanStrictOrNull() ?: true
                onToggleMangaSource(mangaSourceFromPath, enabled)
                newJsonResponse(Status.OK, json("toggled" to true, "id" to mangaSourceFromPath, "enabled" to enabled))
            }

            mangaSourceFromPath != null && method == Method.DELETE -> {
                onDeleteMangaSource(mangaSourceFromPath)
                newJsonResponse(Status.OK, json("deleted" to true, "id" to mangaSourceFromPath))
            }

            // ── API Keys (D4) ─────────────────────────────────────────────────
            uri == "/companion/settings/apikeys" && method == Method.GET ->
                handleGetApiKeys()

            uri == "/companion/settings/apikeys" && method == Method.PATCH -> {
                val body = session.parseBody()
                val key = body["key"] ?: ""
                val value = body["value"] ?: ""
                if (key.isBlank()) {
                    newJsonResponse(Status.BAD_REQUEST, json("error" to "missing_key"))
                } else {
                    onSaveApiKey(key, value)
                    newJsonResponse(Status.OK, json("saved" to true, "key" to key, "hasValue" to value.isNotBlank()))
                }
            }

            // ── Player Settings (D6) ──────────────────────────────────────────
            uri == "/companion/settings/player" && method == Method.GET ->
                handleGetPlayerSettings()

            uri == "/companion/settings/player" && method == Method.PATCH -> {
                val body = session.parseBody()
                val key = body["key"] ?: ""
                val value = body["value"] ?: ""
                if (key.isBlank()) {
                    newJsonResponse(Status.BAD_REQUEST, json("error" to "missing_key"))
                } else {
                    onSavePlayerSetting(key, value)
                    newJsonResponse(Status.OK, json("saved" to true, "key" to key))
                }
            }

            // ── Backup (D5) ───────────────────────────────────────────────────
            uri == "/companion/backup/export" && method == Method.GET ->
                handleExportBackup()

            uri == "/companion/backup/import" && method == Method.POST ->
                handleImportBackup(session)

            // ── Misc ──────────────────────────────────────────────────────────
            uri == "/companion/disconnect" && method == Method.POST -> {
                sessionManager.disconnect()
                newJsonResponse(Status.OK, json("disconnected" to true))
            }

            else -> newJsonResponse(
                Status.NOT_FOUND,
                json("error" to "not_found", "uri" to uri)
            )
        }
    }

    private fun handleListAddons(): Response {
        val arr = JSONArray()
        addonProvider().forEach { addon ->
            arr.put(JSONObject(addon))
        }
        return newJsonResponse(Status.OK, json("addons" to arr))
    }

    private fun handleListRepos(): Response {
        val arr = JSONArray()
        repoProvider().forEach { repo ->
            arr.put(JSONObject(repo))
        }
        return newJsonResponse(Status.OK, json("repos" to arr))
    }

    private fun handleListCsPlugins(): Response {
        val arr = JSONArray()
        csPluginsProvider().forEach { plugin ->
            arr.put(JSONObject(plugin))
        }
        return newJsonResponse(Status.OK, json("plugins" to arr))
    }

    private fun handleListMangaRepos(): Response {
        val arr = JSONArray()
        mangaReposProvider().forEach { repoUrl ->
            arr.put(repoUrl)
        }
        return newJsonResponse(Status.OK, json("repos" to arr))
    }

    private fun handleListMangaSources(): Response {
        val arr = JSONArray()
        mangaSourcesProvider().forEach { source ->
            arr.put(JSONObject(source))
        }
        return newJsonResponse(Status.OK, json("sources" to arr))
    }

    private fun handleGetApiKeys(): Response {
        val masked = apiKeysProvider()
        return newJsonResponse(Status.OK, JSONObject(masked))
    }

    private fun handleGetPlayerSettings(): Response {
        val settings = playerSettingsProvider()
        return newJsonResponse(Status.OK, JSONObject(settings))
    }

    private fun handleExportBackup(): Response {
        val backupJson = backupProvider()
        val response = newFixedLengthResponse(Status.OK, "application/json", backupJson)
        response.addHeader("Content-Disposition", "attachment; filename=\"Kitsugi-backup.json\"")
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Cache-Control", "no-cache, no-store")
        response.addHeader("X-Content-Type-Options", "nosniff")
        return response
    }

    private fun handleImportBackup(session: IHTTPSession): Response {
        val body = session.parseBody()
        val payload = body["payload"] ?: ""
        
        // Size validation (Max 10MB) to protect against massive payloads/memory exhaustion
        if (payload.length > 10_485_760) {
            return newJsonResponse(Status.BAD_REQUEST, json("error" to "payload_too_large", "message" to "Yedek dosyası boyutu 10MB limitini aşamaz."))
        }
        
        if (payload.isBlank()) {
            return newJsonResponse(Status.BAD_REQUEST, json("error" to "empty_payload", "message" to "Yedek verisi boş olamaz."))
        }
        
        val success = try {
            val rootObj = JSONObject(payload)
            if (!rootObj.has("entries")) {
                false
            } else {
                onImportBackup(payload)
            }
        } catch (e: Exception) {
            false
        }
        
        return if (success) {
            newJsonResponse(Status.OK, json("imported" to true, "message" to "Yedek başarıyla içe aktarıldı."))
        } else {
            newJsonResponse(Status.BAD_REQUEST, json("error" to "invalid_format", "message" to "Geçersiz yedek dosyası formatı."))
        }
    }

    private fun handleSessionInfo(): Response {
        val state = sessionManager.sessionState.value
        return newJsonResponse(
            Status.OK,
            json(
                "state" to state.connectionState.name,
                "connectedAt" to state.connectedAt,
                "hasPendingRequest" to (state.pendingRequest != null)
            )
        )
    }

    private fun handleApprovalRequest(session: IHTTPSession, action: String): Response {
        val body = session.parseBody()
        val payload = body["payload"] ?: "{}"
        val requestId = body["id"] ?: System.currentTimeMillis().toString()

        val accepted = sessionManager.requestApproval(
            id = requestId,
            action = action,
            payload = payload
        )
        return if (accepted) {
            sessionManager.sessionState.value.pendingRequest?.let { onApprovalRequested(it) }
            newJsonResponse(Status.ACCEPTED, json("status" to "pending_approval", "id" to requestId))
        } else {
            newJsonResponse(
                Status.CONFLICT,
                json("error" to "approval_already_pending")
            )
        }
    }

    // ── Auth Guard ────────────────────────────────────────────────────────────

    private inline fun withAuth(
        session: IHTTPSession,
        block: () -> Response
    ): Response {
        val token = queryParam(session, "token")
            ?: return newJsonResponse(Status.UNAUTHORIZED, json("error" to "missing_token"))
        return if (sessionManager.isAuthenticated(token)) {
            block()
        } else {
            newJsonResponse(Status.UNAUTHORIZED, json("error" to "unauthenticated"))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun IHTTPSession.parseBody(): Map<String, String> {
        val files = mutableMapOf<String, String>()
        return try {
            parseBody(files)
            files
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun queryParam(session: IHTTPSession, key: String): String? =
        session.parameters[key]?.firstOrNull()

    private fun newJsonResponse(status: Status, body: JSONObject): Response =
        newFixedLengthResponse(status, "application/json", body.toString()).also {
            it.addHeader("Access-Control-Allow-Origin", "*")
            it.addHeader("Cache-Control", "no-cache, no-store")
            it.addHeader("X-Content-Type-Options", "nosniff")
        }

    private fun json(vararg pairs: Pair<String, Any?>): JSONObject =
        JSONObject().apply { pairs.forEach { (k, v) -> put(k, v) } }

    private fun serveWebClient(): Response {
        return try {
            val inputStream = context.assets.open("companion/index.html")
            val html = inputStream.bufferedReader().use { it.readText() }
            newFixedLengthResponse(Status.OK, "text/html", html).apply {
                addHeader("Cache-Control", "no-cache, no-store")
                addHeader("X-Content-Type-Options", "nosniff")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(
                Status.INTERNAL_ERROR,
                "text/plain",
                "Error loading companion client: ${e.message}"
            )
        }
    }

    // ── Companion Object ──────────────────────────────────────────────────────

    companion object {
        const val PORT_RANGE_START = 8080
        const val PORT_RANGE_END = 8090

        /**
         * Scans ports [PORT_RANGE_START]..[PORT_RANGE_END] and returns the first
         * one that is not already in use.
         */
        fun findAvailablePort(): Int {
            for (port in PORT_RANGE_START..PORT_RANGE_END) {
                try {
                    java.net.ServerSocket(port).use { return port }
                } catch (_: java.io.IOException) { /* port in use, try next */ }
            }
            return PORT_RANGE_START // fallback
        }
    }
}
