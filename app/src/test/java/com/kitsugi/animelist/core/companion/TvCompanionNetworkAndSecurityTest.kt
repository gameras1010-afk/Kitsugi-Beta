package com.kitsugi.animelist.core.companion

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.net.ServerSocket

class TvCompanionNetworkAndSecurityTest {

    @Test
    fun testPortRangeScanningAndFallback() {
        // C1.13: Test port race scenario. If the primary port is in use,
        // the server must correctly scan and bind to the next available port.
        val basePort = TvCompanionServer.PORT_RANGE_START
        var socket: ServerSocket? = null
        try {
            socket = ServerSocket(basePort)
            val scannedPort = TvCompanionServer.findAvailablePort()
            assertNotEquals(basePort, scannedPort)
            assertTrue(scannedPort in (basePort + 1)..TvCompanionServer.PORT_RANGE_END)
        } finally {
            socket?.close()
        }
    }

    @Test
    fun testAuthGuardSecurityAdversarial() {
        // C1.14: Test adversarial auth inputs.
        // A request without a token or with an invalid token must be rejected.
        val mockContext = mock<Context>()
        val sessionManager = TvCompanionSessionManager()
        val server = TvCompanionServer(
            context = mockContext,
            sessionManager = sessionManager,
            addonProvider = { emptyList() },
            repoProvider = { emptyList() },
            csPluginsProvider = { emptyList() },
            mangaReposProvider = { emptyList() },
            mangaSourcesProvider = { emptyList() },
            onDeleteAddon = {},
            onToggleAddon = { _, _ -> },
            onDeleteRepo = {},
            onToggleCsPlugin = { _, _ -> },
            onUninstallCsPlugin = {},
            onDeleteMangaRepo = {},
            onToggleMangaSource = { _, _ -> },
            onDeleteMangaSource = {},
            apiKeysProvider = { emptyMap() },
            onSaveApiKey = { _, _ -> },
            playerSettingsProvider = { emptyMap() },
            onSavePlayerSetting = { _, _ -> },
            backupProvider = { "{}" },
            onImportBackup = { _ -> true },
            onApprovalRequested = {}
        )

        // 1. Missing token
        val sessionMissingToken = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/addons"
            on { method } doReturn NanoHTTPD.Method.GET
            on { parameters } doReturn emptyMap()
        }
        val resMissing = server.serve(sessionMissingToken)
        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED.requestStatus, resMissing.status.requestStatus)

        // 2. Invalid token format or incorrect token
        val sessionWrongToken = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/addons"
            on { method } doReturn NanoHTTPD.Method.GET
            on { parameters } doReturn mapOf("token" to listOf("invalid_token_12345"))
        }
        val resWrong = server.serve(sessionWrongToken)
        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED.requestStatus, resWrong.status.requestStatus)
    }

    @Test
    fun testBackupImportPayloadSizeLimit() {
        // C1.14: Test size constraint (zip-bomb/DoS protection). Reject payloads > 10MB.
        val mockContext = mock<Context>()
        val sessionManager = TvCompanionSessionManager()
        val server = TvCompanionServer(
            context = mockContext,
            sessionManager = sessionManager,
            addonProvider = { emptyList() },
            repoProvider = { emptyList() },
            csPluginsProvider = { emptyList() },
            mangaReposProvider = { emptyList() },
            mangaSourcesProvider = { emptyList() },
            onDeleteAddon = {},
            onToggleAddon = { _, _ -> },
            onDeleteRepo = {},
            onToggleCsPlugin = { _, _ -> },
            onUninstallCsPlugin = {},
            onDeleteMangaRepo = {},
            onToggleMangaSource = { _, _ -> },
            onDeleteMangaSource = {},
            apiKeysProvider = { emptyMap() },
            onSaveApiKey = { _, _ -> },
            playerSettingsProvider = { emptyMap() },
            onSavePlayerSetting = { _, _ -> },
            backupProvider = { "{}" },
            onImportBackup = { _ -> true },
            onApprovalRequested = {}
        )

        val token = sessionManager.currentToken()
        sessionManager.authenticate(token)

        // Create a massive payload: 10.5 MB of data
        val hugePayload = "a".repeat(10_500_000)

        val session = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/backup/import"
            on { method } doReturn NanoHTTPD.Method.POST
            on { parameters } doReturn mapOf("token" to listOf(token))
        }
        doAnswer { invocation ->
            val files = invocation.getArgument<MutableMap<String, String>>(0)
            files["payload"] = hugePayload
            null
        }.whenever(session).parseBody(any())

        val response = server.serve(session)
        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST.requestStatus, response.status.requestStatus)
    }

    @Test
    fun testBackupImportInvalidFormatRejection() {
        // C1.14: Test invalid format rejection (missing "entries" in root object).
        val mockContext = mock<Context>()
        val sessionManager = TvCompanionSessionManager()
        val server = TvCompanionServer(
            context = mockContext,
            sessionManager = sessionManager,
            addonProvider = { emptyList() },
            repoProvider = { emptyList() },
            csPluginsProvider = { emptyList() },
            mangaReposProvider = { emptyList() },
            mangaSourcesProvider = { emptyList() },
            onDeleteAddon = {},
            onToggleAddon = { _, _ -> },
            onDeleteRepo = {},
            onToggleCsPlugin = { _, _ -> },
            onUninstallCsPlugin = {},
            onDeleteMangaRepo = {},
            onToggleMangaSource = { _, _ -> },
            onDeleteMangaSource = {},
            apiKeysProvider = { emptyMap() },
            onSaveApiKey = { _, _ -> },
            playerSettingsProvider = { emptyMap() },
            onSavePlayerSetting = { _, _ -> },
            backupProvider = { "{}" },
            onImportBackup = { _ -> true },
            onApprovalRequested = {}
        )

        val token = sessionManager.currentToken()
        sessionManager.authenticate(token)

        // 1. Completely invalid JSON
        val session1 = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/backup/import"
            on { method } doReturn NanoHTTPD.Method.POST
            on { parameters } doReturn mapOf("token" to listOf(token))
        }
        doAnswer { invocation ->
            val files = invocation.getArgument<MutableMap<String, String>>(0)
            files["payload"] = "Not a JSON content"
            null
        }.whenever(session1).parseBody(any())

        val response1 = server.serve(session1)
        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST.requestStatus, response1.status.requestStatus)

        // 2. JSON without "entries" field
        val session2 = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/backup/import"
            on { method } doReturn NanoHTTPD.Method.POST
            on { parameters } doReturn mapOf("token" to listOf(token))
        }
        doAnswer { invocation ->
            val files = invocation.getArgument<MutableMap<String, String>>(0)
            files["payload"] = """{"schemaVersion": 1, "app": "Kitsugi"}"""
            null
        }.whenever(session2).parseBody(any())

        val response2 = server.serve(session2)
        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST.requestStatus, response2.status.requestStatus)
    }

    @Test
    fun testBackupImportSuccessFlow() {
        val mockContext = mock<Context>()
        val sessionManager = TvCompanionSessionManager()
        var importCalled = false
        val server = TvCompanionServer(
            context = mockContext,
            sessionManager = sessionManager,
            addonProvider = { emptyList() },
            repoProvider = { emptyList() },
            csPluginsProvider = { emptyList() },
            mangaReposProvider = { emptyList() },
            mangaSourcesProvider = { emptyList() },
            onDeleteAddon = {},
            onToggleAddon = { _, _ -> },
            onDeleteRepo = {},
            onToggleCsPlugin = { _, _ -> },
            onUninstallCsPlugin = {},
            onDeleteMangaRepo = {},
            onToggleMangaSource = { _, _ -> },
            onDeleteMangaSource = {},
            apiKeysProvider = { emptyMap() },
            onSaveApiKey = { _, _ -> },
            playerSettingsProvider = { emptyMap() },
            onSavePlayerSetting = { _, _ -> },
            backupProvider = { "{}" },
            onImportBackup = { payload ->
                importCalled = true
                payload.contains("Kitsugi")
            },
            onApprovalRequested = {}
        )

        val token = sessionManager.currentToken()
        sessionManager.authenticate(token)

        val session = mock<NanoHTTPD.IHTTPSession> {
            on { uri } doReturn "/companion/backup/import"
            on { method } doReturn NanoHTTPD.Method.POST
            on { parameters } doReturn mapOf("token" to listOf(token))
        }
        doAnswer { invocation ->
            val files = invocation.getArgument<MutableMap<String, String>>(0)
            files["payload"] = """{"schemaVersion": 1, "app": "Kitsugi", "entries": []}"""
            null
        }.whenever(session).parseBody(any())

        val response = server.serve(session)
        assertEquals(NanoHTTPD.Response.Status.OK.requestStatus, response.status.requestStatus)
        assertTrue(importCalled)
    }
}
