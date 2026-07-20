package com.kitsugi.animelist.core.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the TV Companion Core logic:
 *  - [TvCompanionToken] generation and validation
 *  - [TvCompanionSessionManager] lifecycle and approval flow
 *
 * No Android framework dependencies; pure JVM tests.
 */
class TvCompanionCoreTest {

    private lateinit var manager: TvCompanionSessionManager

    @Before
    fun setUp() {
        manager = TvCompanionSessionManager()
    }

    // ── TvCompanionToken ───────────────────────────────────────────────────────

    @Test
    fun `generate returns 64 character hex string`() {
        val token = TvCompanionToken.generate()
        assertEquals(64, token.length)
        assertTrue(token.all { it in "0123456789abcdef" })
    }

    @Test
    fun `generate produces unique tokens on each call`() {
        val t1 = TvCompanionToken.generate()
        val t2 = TvCompanionToken.generate()
        assertNotEquals(t1, t2)
    }

    @Test
    fun `isValid returns true for valid 64-char hex token`() {
        val token = TvCompanionToken.generate()
        assertTrue(TvCompanionToken.isValid(token))
    }

    @Test
    fun `isValid returns false for too-short string`() {
        assertFalse(TvCompanionToken.isValid("abc123"))
    }

    @Test
    fun `isValid returns false for non-hex characters`() {
        val bad = "g".repeat(64) // 'g' is not hex
        assertFalse(TvCompanionToken.isValid(bad))
    }

    @Test
    fun `safeEquals returns true for identical tokens`() {
        val token = TvCompanionToken.generate()
        assertTrue(TvCompanionToken.safeEquals(token, token))
    }

    @Test
    fun `safeEquals returns false for different length strings`() {
        assertFalse(TvCompanionToken.safeEquals("abc", "abcd"))
    }

    @Test
    fun `safeEquals returns false for differing tokens`() {
        val t1 = TvCompanionToken.generate()
        val t2 = TvCompanionToken.generate()
        // Very low probability they match; if so the test is meaningless but won't fail production
        if (t1 != t2) assertFalse(TvCompanionToken.safeEquals(t1, t2))
    }

    // ── TvCompanionSessionManager – initial state ──────────────────────────────

    @Test
    fun `initial state is IDLE`() {
        assertEquals(
            TvCompanionSessionManager.ConnectionState.IDLE,
            manager.sessionState.value.connectionState
        )
    }

    @Test
    fun `currentToken returns non-empty token on init`() {
        assertTrue(manager.currentToken().isNotBlank())
        assertEquals(64, manager.currentToken().length)
    }

    // ── rotateToken ─────────────────────────────────────────────────────────────

    @Test
    fun `rotateToken returns new token each call`() {
        val first = manager.rotateToken()
        val second = manager.rotateToken()
        assertNotEquals(first, second)
    }

    @Test
    fun `rotateToken resets session state to IDLE`() {
        // Authenticate first
        manager.authenticate(manager.currentToken())
        assertEquals(TvCompanionSessionManager.ConnectionState.CONNECTED,
            manager.sessionState.value.connectionState)

        manager.rotateToken()
        assertEquals(TvCompanionSessionManager.ConnectionState.IDLE,
            manager.sessionState.value.connectionState)
    }

    // ── authenticate ────────────────────────────────────────────────────────────

    @Test
    fun `authenticate with correct token transitions to CONNECTED`() {
        val token = manager.currentToken()
        assertTrue(manager.authenticate(token))
        assertEquals(TvCompanionSessionManager.ConnectionState.CONNECTED,
            manager.sessionState.value.connectionState)
    }

    @Test
    fun `authenticate with wrong token returns false`() {
        manager.rotateToken() // ensure a fresh token
        val wrongToken = TvCompanionToken.generate() // will differ from current
        // Minimal safeguard: only test if truly different
        val current = manager.currentToken()
        if (current != wrongToken) {
            assertFalse(manager.authenticate(wrongToken))
            assertEquals(TvCompanionSessionManager.ConnectionState.IDLE,
                manager.sessionState.value.connectionState)
        }
    }

    @Test
    fun `authenticate with invalid-format token returns false`() {
        assertFalse(manager.authenticate("tooshort"))
    }

    // ── isAuthenticated ──────────────────────────────────────────────────────────

    @Test
    fun `isAuthenticated returns true after successful authenticate`() {
        val token = manager.currentToken()
        manager.authenticate(token)
        assertTrue(manager.isAuthenticated(token))
    }

    @Test
    fun `isAuthenticated returns false before authenticate`() {
        assertFalse(manager.isAuthenticated(manager.currentToken()))
    }

    @Test
    fun `isAuthenticated returns false after rotateToken`() {
        val oldToken = manager.currentToken()
        manager.authenticate(oldToken)
        manager.rotateToken()
        assertFalse(manager.isAuthenticated(oldToken))
    }

    // ── Approval flow ──────────────────────────────────────────────────────────

    @Test
    fun `requestApproval transitions to PENDING_APPROVAL`() {
        val token = manager.currentToken()
        manager.authenticate(token)
        val accepted = manager.requestApproval("id-1", "INSTALL_ADDON", """{"url":"https://example.com"}""")
        assertTrue(accepted)
        assertEquals(TvCompanionSessionManager.ConnectionState.PENDING_APPROVAL,
            manager.sessionState.value.connectionState)
        assertNotNull(manager.sessionState.value.pendingRequest)
    }

    @Test
    fun `requestApproval fails when one is already pending`() {
        manager.authenticate(manager.currentToken())
        manager.requestApproval("id-1", "INSTALL_ADDON", "{}")
        val second = manager.requestApproval("id-2", "ADD_REPO", "{}")
        assertFalse(second)
    }

    @Test
    fun `approvePending returns request and clears pending state`() {
        manager.authenticate(manager.currentToken())
        manager.requestApproval("id-1", "ADD_REPO", """{"name":"test"}""")
        val approved = manager.approvePending()
        assertNotNull(approved)
        assertEquals("id-1", approved!!.id)
        assertEquals("ADD_REPO", approved.action)
        assertNull(manager.sessionState.value.pendingRequest)
        assertEquals(TvCompanionSessionManager.ConnectionState.CONNECTED,
            manager.sessionState.value.connectionState)
    }

    @Test
    fun `rejectPending clears pending state and stays CONNECTED`() {
        manager.authenticate(manager.currentToken())
        manager.requestApproval("id-1", "INSTALL_ADDON", "{}")
        manager.rejectPending()
        assertNull(manager.sessionState.value.pendingRequest)
        assertEquals(TvCompanionSessionManager.ConnectionState.CONNECTED,
            manager.sessionState.value.connectionState)
    }

    // ── expire & disconnect ────────────────────────────────────────────────────

    @Test
    fun `expire transitions to EXPIRED`() {
        manager.authenticate(manager.currentToken())
        manager.expire()
        assertEquals(TvCompanionSessionManager.ConnectionState.EXPIRED,
            manager.sessionState.value.connectionState)
    }

    @Test
    fun `disconnect rotates token and resets to IDLE`() {
        val oldToken = manager.currentToken()
        manager.authenticate(oldToken)
        manager.disconnect()
        assertNotEquals(oldToken, manager.currentToken())
        assertEquals(TvCompanionSessionManager.ConnectionState.IDLE,
            manager.sessionState.value.connectionState)
    }

    // ── tickExpiry ─────────────────────────────────────────────────────────────

    @Test
    fun `tickExpiry does not expire a freshly authenticated session`() {
        manager.authenticate(manager.currentToken())
        manager.tickExpiry()
        assertEquals(TvCompanionSessionManager.ConnectionState.CONNECTED,
            manager.sessionState.value.connectionState)
    }
}
